package io.github.reconsolidated.jediscommunicator;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;

import java.util.*;

public final class JedisCommunicator extends JavaPlugin {
    private Jedis jedis;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getServicesManager().register(JedisCommunicator.class, this, this, ServicePriority.Normal);

        try {
            jedis = new Jedis("grypciocraft.pl", 6379);
        } catch (Exception e) {
            e.printStackTrace();
            jedis = null;
            return;
        }
        if (jedis == null) {
            Bukkit.getLogger().warning("Couldn't connect to jedis");
            return;
        }
        jedis.auth("kWy681@t");
    }

    public List<String> getNotifications(String playerName) {
        Map<String, String> notifications = jedis.hgetAll("notifications_" + playerName);
        if (notifications == null) {
            notifications = new HashMap<>();
        }
        List<String> result = new ArrayList<>();
        for (String key : notifications.keySet()) {
            result.add(notifications.get(key));
        }
        return result;
    }

    public void clearNotifications(String playerName) {
        jedis.del("notifications_" + playerName);
    }

    public void sendNotification(String playerName, String notification, int expireIn) {
        Map<String, String> notifications = jedis.hgetAll(playerName);
        if (notifications == null) {
            notifications = new HashMap<>();
        }
        notifications.put("" + System.currentTimeMillis(), notification);
        jedis.hset("notifications_" + playerName, notifications);
        if (expireIn != -1) {
            jedis.expire("notifications_" + playerName, expireIn);
        }
    }

    public void sendNotification(String playerName, String notification) {
        sendNotification(playerName, notification, -1);
    }

    /**
     * @param serverName - Name of the server, in format type_port
     * @param isOpen - Whether players can join the server or not.
     * @param currentPlayers
     * @param maxPlayers
     * @param maxPartySize - Maximum size of a party that could join the server
     * @param type - type of the server, e.g. bedwars1, vdIce, survival
     */
    public void setServerInfo(String serverName, boolean isOpen, int currentPlayers, int maxPlayers, int maxPartySize, String type) {
        Map<String, String> serverInfo = new HashMap<String, String>();
        serverInfo.put("name", serverName);
        serverInfo.put("isOpen", "" + isOpen);
        serverInfo.put("currentPlayers", "" + currentPlayers);
        serverInfo.put("maxPlayers", "" + maxPlayers);
        serverInfo.put("maxPartySize", "" + maxPartySize);
        serverInfo.put("type", type);
        jedis.hset("server_info_" + serverName + "", serverInfo);
        jedis.expire(serverName, 1);
    }

    /**
     *
     * @param type - server type, for example bedwars1, bedwars2 ...
     * @return - all servers of that type
     */
    public List<JedisServerInfo> getServers(String type) {
        List<JedisServerInfo> result = new ArrayList<>();
        for (String key : jedis.keys("server_info_*")) {
            Map<String, String> map = jedis.hgetAll(key);
            if (type.equalsIgnoreCase(map.get("type"))) {
                String serverName = map.get("name");
                boolean isOpen = Boolean.parseBoolean(map.get("isOpen"));
                int currentPlayers = Integer.parseInt("currentPlayers");
                int maxPlayers = Integer.parseInt("maxPlayers");
                int maxPartySize = Integer.parseInt("maxPartySize");
                result.add(new JedisServerInfo(serverName, isOpen, currentPlayers, maxPlayers, maxPartySize, type));
            }
        }
        return result;
    }

    /**
     *
     * @param type - server type, for example bedwars1, vdIce, survival
     * @param partySize - minimum available space for a party, set it to 1 if player doesn't have party
     * @return - best suitable server
     */
    public String getServer(String type, int partySize) {
        Set<String> keys = jedis.keys("server_info_" + type + "*");
        Map<String, String> bestServerInfo = null;
        for (String key : keys) {
            Map<String, String> serverInfo = jedis.hgetAll(key);


            if (Integer.parseInt(serverInfo.get("maxPartySize")) >= partySize &&
            serverInfo.get("isOpen").equals("true")) {
                if (bestServerInfo == null) {
                    bestServerInfo = serverInfo;
                }
                // prioritize servers with more players
                if (Integer.parseInt(serverInfo.get("currentPlayers")) > Integer.parseInt(bestServerInfo.get("currentPlayers"))) {
                    bestServerInfo = serverInfo;
                }
            }
        }
        if (bestServerInfo == null) return null;
        return bestServerInfo.get("name");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
