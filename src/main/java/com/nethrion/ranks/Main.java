package com.nethrion.ranks;

import com.nethrion.ranks.commands.BaseCommand;
import com.nethrion.ranks.commands.RankCommand;
import com.nethrion.ranks.commands.RankDuelCommand;
import com.nethrion.ranks.commands.SkillsCommand;
import com.nethrion.ranks.listeners.BaseListener;
import com.nethrion.ranks.listeners.DuelListener;
import com.nethrion.ranks.managers.BaseManager;
import com.nethrion.ranks.managers.RankManager;
import com.nethrion.ranks.rank.DuelManager;
import com.nethrion.ranks.rank.RankLadderManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private RankManager rankManager;
    private BaseManager baseManager;
    private RankLadderManager rankLadderManager; // Phase 1: rank-ladder data engine
    private DuelManager duelManager;             // Phase 2: duel invites + active sessions

    @Override
    public void onEnable() {
        getLogger().info("NethrionRanks is starting up...");

        // Managers create karna
        this.rankManager = new RankManager(this);
        this.baseManager = new BaseManager(this);
        this.rankLadderManager = new RankLadderManager(this);
        this.duelManager = new DuelManager(this, rankLadderManager);

        // Commands register karna
        getCommand("rank").setExecutor(new RankCommand(rankManager));
        getCommand("skills").setExecutor(new SkillsCommand(rankManager));
        getCommand("base").setExecutor(new BaseCommand(baseManager));
        getCommand("rankduel").setExecutor(new RankDuelCommand(duelManager, rankLadderManager));

        // Listeners register karna
        getServer().getPluginManager().registerEvents(new BaseListener(baseManager), this);
        getServer().getPluginManager().registerEvents(new DuelListener(duelManager, rankLadderManager), this);

        getLogger().info("NethrionRanks enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("NethrionRanks disabled.");
    }
}
