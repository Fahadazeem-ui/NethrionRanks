package com.nethrion.ranks.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaseManager {

    public static final int RADIUS = 30; // 30-block radius protection zone

    private final JavaPlugin plugin;
    private final FileConfiguration config;

    // Owner UUID -> unka base location
    private final Map<UUID, Location> bases = new HashMap<>();

    // Owner UUID -> unke base ki activity logs (non-persistent, sirf runtime ke liye)
    private final Map<UUID, List<LogEntry>> baseLogs = new HashMap<>();

    public BaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadBasesFromConfig();
    }

    // Server start hone par saari saved bases config.yml se load karna
    private void loadBasesFromConfig() {
        ConfigurationSection section = config.getConfigurationSection("bases");
        if (section == null) return;

        for (String uuidString : section.getKeys(false)) {
            try {
                UUID ownerUUID = UUID.fromString(uuidString);
                String worldName = section.getString(uuidString + ".world");
                double x = section.getDouble(uuidString + ".x");
                double y = section.getDouble(uuidString + ".y");
                double z = section.getDouble(uuidString + ".z");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    bases.put(ownerUUID, new Location(world, x, y, z));
                    baseLogs.put(ownerUUID, new ArrayList<>());
                }
            } catch (IllegalArgumentException ignored) {
                // Ghalat UUID format ho to skip kar do
            }
        }
    }

    // Player ka current location ko base ke roop mein save karna
    public void setBase(Player player) {
        UUID ownerUUID = player.getUniqueId();
        Location loc = player.getLocation();

        bases.put(ownerUUID, loc);
        baseLogs.putIfAbsent(ownerUUID, new ArrayList<>());

        config.set("bases." + ownerUUID + ".world", loc.getWorld().getName());
        config.set("bases." + ownerUUID + ".x", loc.getX());
        config.set("bases." + ownerUUID + ".y", loc.getY());
        config.set("bases." + ownerUUID + ".z", loc.getZ());
        plugin.saveConfig();
    }

    public boolean hasBase(UUID ownerUUID) {
        return bases.containsKey(ownerUUID);
    }

    public Location getBase(UUID ownerUUID) {
        return bases.get(ownerUUID);
    }

    // Diye gaye location ke base ka owner dhoondna (agar woh kisi ke 30-block radius mein ho)
    public UUID getBaseOwnerAt(Location location) {
        for (Map.Entry<UUID, Location> entry : bases.entrySet()) {
            Location baseLoc = entry.getValue();
            if (baseLoc.getWorld() == null || location.getWorld() == null) continue;
            if (!baseLoc.getWorld().equals(location.getWorld())) continue;

            double dx = Math.abs(baseLoc.getX() - location.getX());
            double dz = Math.abs(baseLoc.getZ() - location.getZ());

            if (dx <= RADIUS && dz <= RADIUS) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Ek nayi log entry add karna (silent — koi broadcast nahi)
    public void addLog(UUID ownerUUID, LogEntry entry) {
        baseLogs.computeIfAbsent(ownerUUID, k -> new ArrayList<>()).add(entry);
    }

    // Logs nikalna, agar sinceMillis diya gaya ho to sirf utne time ke andar wale
    public List<LogEntry> getLogs(UUID ownerUUID, Long sinceMillis) {
        List<LogEntry> logs = baseLogs.getOrDefault(ownerUUID, new ArrayList<>());
        if (sinceMillis == null) {
            return logs;
        }

        List<LogEntry> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (LogEntry entry : logs) {
            if (now - entry.getTimestamp() <= sinceMillis) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
}
