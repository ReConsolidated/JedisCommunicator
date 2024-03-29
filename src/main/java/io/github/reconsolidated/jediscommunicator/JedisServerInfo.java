package io.github.reconsolidated.jediscommunicator;

public class JedisServerInfo {
    public final String serverName;
    public final boolean isOpen;
    public int currentPlayers;
    public final int maxPlayers;
    public final int maxPartySize;
    public final String type;
    public final boolean ranked;

    public JedisServerInfo(String serverName, boolean isOpen, int currentPlayers, int maxPlayers, int maxPartySize, String type, boolean ranked) {
        this.serverName = serverName;
        this.isOpen = isOpen;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.maxPartySize = maxPartySize;
        this.type = type;

        this.ranked = ranked;
    }
}
