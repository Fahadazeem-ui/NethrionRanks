package com.nethrion.ranks.managers;

import java.util.LinkedHashMap;
import java.util.Map;

public class LogEntry {

    private final String playerName;
    private final String actionType;
    private final String itemOrBlockName;
    private final int x;
    private final int y;
    private final int z;
    private final long timestamp;

    public LogEntry(
            String playerName,
            String actionType,
            String itemOrBlockName,
            int x,
            int y,
            int z) {

        this(
                playerName,
                actionType,
                itemOrBlockName,
                x,
                y,
                z,
                System.currentTimeMillis()
        );
    }

    public LogEntry(
            String playerName,
            String actionType,
            String itemOrBlockName,
            int x,
            int y,
            int z,
            long timestamp) {

        this.playerName = playerName;
        this.actionType = actionType;
        this.itemOrBlockName = itemOrBlockName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map =
                new LinkedHashMap<>();

        map.put("player", playerName);
        map.put("action", actionType);
        map.put("item", itemOrBlockName);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("timestamp", timestamp);

        return map;
    }

    public static LogEntry fromMap(
            Map<?, ?> map) {

        if (map == null) return null;

        Object player =
                map.get("player");

        Object action =
                map.get("action");

        Object item =
                map.get("item");

        if (
                player == null ||
                        action == null ||
                        item == null
        ) {
            return null;
        }

        return new LogEntry(
                String.valueOf(player),
                String.valueOf(action),
                String.valueOf(item),
                number(map.get("x")),
                number(map.get("y")),
                number(map.get("z")),
                longNumber(
                        map.get("timestamp")
                )
        );
    }

    private static int number(
            Object value) {

        return value instanceof Number
                ? ((Number) value).intValue()
                : 0;
    }

    private static long longNumber(
            Object value) {

        return value instanceof Number
                ? ((Number) value).longValue()
                : System.currentTimeMillis();
    }

    public String toDisplayString() {

        return "[" +
                actionType +
                "] " +
                playerName +
                " -> " +
                itemOrBlockName +
                " at (" +
                x +
                ", " +
                y +
                ", " +
                z +
                ")";
    }
}
