package io.github.reconsolidated.jediscommunicator;

import redis.clients.jedis.Jedis;

import java.util.*;

public final class JedisCommunicator {
    private Jedis jedis;

    public JedisCommunicator() {
        jedis = new Jedis("grypciocraft.pl", 6379);
        jedis.auth("kWy681@t");
    }

    public void clear() {
        jedis.flushAll();
    }

    public void reconnect() {
        jedis = new Jedis("grypciocraft.pl", 6379);
        jedis.auth("kWy681@t");
    }

    public boolean isClosed() {
        return !jedis.isConnected() || jedis.isBroken();
    }

    /**
     * @param playerName
     * @return players' party (if he doesnt have one, returns a party with him as an owner)
     */
    public Party getParty(String playerName) {
        for (Party party : getAllParties()) {
            if (party.getAllMembers().contains(playerName)) {
                return party;
            }
        }
        return new Party(playerName);
    }

    public boolean isInParty(String playerName) {
        for (Party party : getAllParties()) {
            if (party.getAllMembers().contains(playerName)) {
                return true;
            }
        }
        return false;
    }


    /**
     *
     * @return all existing parties (with more than 1 player)
     */
    public List<Party> getAllParties() {
        List<Party> parties = new ArrayList<>();
        for (String key : jedis.keys("party|*")) {
            String ownerName = key.split("\\|")[1];
            Set<String> members = jedis.smembers(key);
            parties.add(new Party(ownerName, members));
        }
        return parties;
    }

    public void setRejoin() {

    }

    /**
     * Saves given party if it has any members. Otherwise attempts to delete party data for owner.
     * @param party
     */
    public void saveParty(Party party) {
        if (party.getMembers().size() > 0) {
            for (String s : party.getMembers()) {
                jedis.sadd("party|" + party.getOwner(), s);
            }
        } else {
            jedis.del("party|" + party.getOwner());
        }
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
    public void setServerInfo(String serverName, boolean isOpen, int currentPlayers, int maxPlayers, int maxPartySize, String type, boolean ranked) {
        Map<String, String> serverInfo = new HashMap<String, String>();
        serverInfo.put("name", serverName);
        serverInfo.put("isOpen", "" + isOpen);
        serverInfo.put("currentPlayers", "" + currentPlayers);
        serverInfo.put("maxPlayers", "" + maxPlayers);
        serverInfo.put("maxPartySize", "" + maxPartySize);
        serverInfo.put("type", type);
        serverInfo.put("ranked", "" + ranked);
        jedis.hset("server_info_" + serverName + "", serverInfo);
        jedis.expire("server_info_" + serverName + "", 1);
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
                boolean isOpen = map.get("isOpen").equals("true");
                int currentPlayers = Integer.parseInt(map.get("currentPlayers"));
                int maxPlayers = Integer.parseInt(map.get("maxPlayers"));
                int maxPartySize = Integer.parseInt(map.get("maxPartySize"));
                boolean ranked = map.get("ranked").equals("true");

                result.add(new JedisServerInfo(serverName, isOpen, currentPlayers, maxPlayers, maxPartySize, type, ranked));
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

}
