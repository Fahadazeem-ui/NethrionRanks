package com.nethrion.ranks.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class RankManager {

    private final JavaPlugin plugin;
    private final FileConfiguration config;

    // Ladder ka order — is list mein jitni age wala rank, utna bara
    private final String[] rankLadder = {
            "Newcomer", "Member", "Trusted", "Veteran", "Elite", "Legend"
    };

    public RankManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    // Player ka current rank nikalna (agar naya player hai to "Newcomer" milega)
    public String getRank(UUID uuid) {
        return config.getString("players." + uuid + ".rank", "Newcomer");
    }

    // Player ka rank set/update karna
    public void setRank(UUID uuid, String rank) {
        config.set("players." + uuid + ".rank", rank);
        plugin.saveConfig();
    }

    // Player ko ladder mein agle rank pe promote karna
    public String promoteRank(UUID uuid) {
        String current = getRank(uuid);
        int index = -1;
        for (int i = 0; i < rankLadder.length; i++) {
            if (rankLadder[i].equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }
        if (index == -1 || index == rankLadder.length - 1) {
            return current; // already max rank ya rank mila hi nahi
        }
        String next = rankLadder[index + 1];
        setRank(uuid, next);
        return next;
    }

    public String[] getRankLadder() {
        return rankLadder;
    }

    // Player ka skill level nikalna (default 1)
    public int getSkillLevel(UUID uuid, String skillName) {
        return config.getInt("players." + uuid + ".skills." + skillName + ".level", 1);
    }

    // Player ka skill XP nikalna
    public int getSkillXP(UUID uuid, String skillName) {
        return config.getInt("players." + uuid + ".skills." + skillName + ".xp", 0);
    }

    // Skill mein XP add karna, aur agar level-up ke liye kaafi ho to level barhana
    public void addSkillXP(UUID uuid, String skillName, int amount) {
        int currentXP = getSkillXP(uuid, skillName) + amount;
        int currentLevel = getSkillLevel(uuid, skillName);

        int xpNeeded = currentLevel * 100; // simple formula: level * 100 XP chahiye agle level ke liye
        while (currentXP >= xpNeeded) {
            currentXP -= xpNeeded;
            currentLevel++;
            xpNeeded = currentLevel * 100;
        }

        config.set("players." + uuid + ".skills." + skillName + ".level", currentLevel);
        config.set("players." + uuid + ".skills." + skillName + ".xp", currentXP);
        plugin.saveConfig();
    }
}
