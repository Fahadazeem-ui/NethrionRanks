package com.nethrion.ranks;

import com.nethrion.ranks.commands.BaseCommand;
import com.nethrion.ranks.commands.RankCommand;
import com.nethrion.ranks.commands.SkillsCommand;
import com.nethrion.ranks.listeners.BaseListener;
import com.nethrion.ranks.managers.BaseManager;
import com.nethrion.ranks.managers.RankManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private RankManager rankManager;
    private BaseManager baseManager;

    @Override
    public void onEnable() {
        getLogger().info("NethrionRanks is starting up...");

        // Managers create karna
        this.rankManager = new RankManager(this);
        this.baseManager = new BaseManager(this);

        // Commands register karna
        getCommand("rank").setExecutor(new RankCommand(rankManager));
        getCommand("skills").setExecutor(new SkillsCommand(rankManager));
        getCommand("base").setExecutor(new BaseCommand(baseManager));

        // Listeners register karna
        getServer().getPluginManager().registerEvents(new BaseListener(baseManager), this);

        getLogger().info("NethrionRanks enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("NethrionRanks disabled.");
    }
}
