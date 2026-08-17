package com.nethrion.ranks;

import com.nethrion.ranks.commands.RankCommand;
import com.nethrion.ranks.commands.SkillsCommand;
import com.nethrion.ranks.managers.RankManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private RankManager rankManager;

    @Override
    public void onEnable() {
        getLogger().info("NethrionRanks is starting up...");

        // Managers create karna
        this.rankManager = new RankManager(this);

        // Commands register karna
        getCommand("rank").setExecutor(new RankCommand(rankManager));
        getCommand("skills").setExecutor(new SkillsCommand(rankManager));

        getLogger().info("NethrionRanks enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("NethrionRanks disabled.");
    }
}
