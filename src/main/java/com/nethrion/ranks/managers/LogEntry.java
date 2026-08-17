package com.nethrion.ranks.managers;

/**
 * Ek single audit log entry — base ke andar hui activity ka record.
 * Har entry mein: kaun, kya, kahan, kab.
 */
public class LogEntry {

    private final String playerName;
    private final String actionType; // "BREAK", "PLACE", "CONTAINER"
    private final String itemOrBlockName;
    private final int x, y, z;
    private final long timestamp;

    public LogEntry(String playerName, String actionType, String itemOrBlockName, int x, int y, int z) {
        this.playerName = playerName;
        this.actionType = actionType;
        this.itemOrBlockName = itemOrBlockName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = System.currentTimeMillis();
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getItemOrBlockName() {
        return itemOrBlockName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Chat mein dikhane ke liye ek line format
    public String toDisplayString() {
        return "[" + actionType + "] " + playerName + " -> " + itemOrBlockName +
                " at (" + x + ", " + y + ", " + z + ")";
    }
}
