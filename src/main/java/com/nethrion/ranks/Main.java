package com.nethrion.ranks;

import com.nethrion.ranks.commands.BaseCommand;
import com.nethrion.ranks.commands.KillCountCommand;
import com.nethrion.ranks.commands.KillTopCommand;
import com.nethrion.ranks.commands.RankCommand;
import com.nethrion.ranks.commands.RankDuelCommand;
import com.nethrion.ranks.commands.SkillsCommand;
import com.nethrion.ranks.listeners.BaseListener;
import com.nethrion.ranks.listeners.NationalWeaponListener;
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

        RankCommand rankCommand =
                new RankCommand(rankLadderManager);

        SkillsCommand skillsCommand =
                new SkillsCommand(rankLadderManager);

        BaseCommand baseCommand =
                new BaseCommand(baseManager);

        RankDuelCommand rankDuelCommand =
                new RankDuelCommand(
                        duelManager,
                        rankLadderManager
                );

        getCommand("rank").setExecutor(rankCommand);
        getCommand("rank").setTabCompleter(rankCommand);

        getCommand("skills").setExecutor(skillsCommand);
        getCommand("skills").setTabCompleter(skillsCommand);

        getCommand("base").setExecutor(baseCommand);
        getCommand("base").setTabCompleter(baseCommand);

        getCommand("rankduel").setExecutor(rankDuelCommand);
        getCommand("rankduel").setTabCompleter(rankDuelCommand);
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
                new NationalWeaponListener(
                        rankLadderManager
                ),
                this
        );
        getServer().getPluginManager().registerEvents(
                new RankDisplayListener(
                        rankLadderManager
                ),
                this
        );

        getServer().getScheduler().runTaskTimer(
                this,
                rankLadderManager::enforceNationalInactivity,
                20L * 60L * 5L,
                20L * 60L * 5L
        );

        getServer().getScheduler().runTaskTimer(
                this,
                rankLadderManager::announceExpiredBounties,
                20L * 5L,
                20L * 5L
        );

        // Base logs auto-expire after 3 days; this periodic sweep keeps
        // that from only happening lazily on read/write, so genuinely
        // stale entries don't linger in memory/config indefinitely on
        // a base nobody queries again.
        getServer().getScheduler().runTaskTimer(
                this,
                baseManager::purgeAllExpiredLogs,
                20L * 60L * 30L,
                20L * 60L * 30L
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
