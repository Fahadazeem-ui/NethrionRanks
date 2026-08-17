package com.nethrion.ranks;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("NethrionRanks is starting up...");

        // Managers, commands, aur listeners yahan register honge
        // jaise jaise hum modules banate jayenge

        getLogger().info("NethrionRanks enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("NethrionRanks disabled.");
    }
}
