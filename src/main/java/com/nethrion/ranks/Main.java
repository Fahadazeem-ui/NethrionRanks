package com.nethrion.ranks;

import com.nethrion.ranks.commands.BaseCommand;
import com.nethrion.ranks.commands.KillCountCommand;
import com.nethrion.ranks.commands.KillTopCommand;
import com.nethrion.ranks.commands.RankCommand;
import com.nethrion.ranks.commands.RankDuelCommand;
import com.nethrion.ranks.commands.SkillsCommand;
import com.nethrion.ranks.listeners.BaseListener;
import com.nethrion.ranks.listeners.DuelListener;
import com.nethrion.ranks.listeners.RankDisplayListener;
import com.nethrion.ranks.listeners.SpearListener;
import com.nethrion.ranks.managers.BaseManager;
import com.nethrion.ranks.rank.DuelManager;
import com.nethrion.ranks.rank.RankLadderManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private RankLadderManager rankLadderManager;
    private DuelManager duelManager;
    private BaseManager baseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        rankLadderManager = new RankLadderManager(this);
        duelManager = new DuelManager(this, rankLadderManager);
        baseManager = new BaseManager(this);

        getCommand("rank").setExecutor(
                new RankCommand(rankLadderManager)
        );
        getCommand("skills").setExecutor(
                new SkillsCommand(rankLadderManager)
        );
        getCommand("base").setExecutor(
                new BaseCommand(baseManager)
        );
        getCommand("rankduel").setExecutor(
                new RankDuelCommand(
                        duelManager,
                        rankLadderManager
                )
        );
        getCommand("killcount").setExecutor(
                new KillCountCommand(rankLadderManager)
        );
        getCommand("killtop").setExecutor(
                new KillTopCommand(rankLadderManager)
        );

        getServer().getPluginManager().registerEvents(
                new BaseListener(baseManager),
                this
        );
        getServer().getPluginManager().registerEvents(
                new DuelListener(
                        duelManager,
                        rankLadderManager
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new SpearListener(),
                this
        );
        getServer().getPluginManager().registerEvents(
                new RankDisplayListener(
                        rankLadderManager
                ),
                this
        );

        getLogger().info(
                "NethrionRanks enabled. " +
                        "Rank ladder, ranked duels, skill locking, " +
                        "Spear, kill tracking and base audit are active."
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("NethrionRanks disabled.");
    }
}
