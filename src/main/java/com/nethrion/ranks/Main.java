package com.nethrion.ranks;

import com.nethrion.ranks.gui.RankGUICommand;
import com.nethrion.ranks.gui.RankGUIListener;
import com.nethrion.ranks.miner.NationalMinerManager;
import com.nethrion.ranks.pvp.PvPManager;
import com.nethrion.ranks.rank.DuelManager;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.managers.RankExitRequestManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NethrionRanks — Main entry point.
 *
 * Extends the original plugin startup to register the new GUI
 * command (/rankgui) and the GUI click listener.
 *
 * All original systems (RankLadderManager, DuelManager, WeaponUtil,
 * NationalMinerManager, BaseManager, PvPManager, etc.) are
 * unchanged and loaded exactly as before.
 */
public class Main extends JavaPlugin {

    private static Main instance;

    // ── Managers (wired in onEnable) ─────────────────────────────
    private RankLadderManager rankLadderManager;
    private DuelManager       duelManager;
    private NationalMinerManager minerManager;
    private PvPManager        pvpManager;
    private RankExitRequestManager requestManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // ── Boot original systems ──────────────────────────────────
        rankLadderManager = new RankLadderManager(this);
        duelManager       = new DuelManager(this, rankLadderManager);

        pvpManager        = new PvPManager(this);
        minerManager      = new NationalMinerManager(this, rankLadderManager);
        requestManager    = new RankExitRequestManager(this, rankLadderManager, minerManager);

        // ── Register original commands ─────────────────────────────
        registerOriginalCommands();

        // ── Register original listeners ────────────────────────────
        registerOriginalListeners();

        // ── Register GUI (new in 1.1.0) ────────────────────────────
        var guiCmd = new RankGUICommand();
        var guiCmdObj = getCommand("rankgui");
        if (guiCmdObj != null) {
            guiCmdObj.setExecutor(guiCmd);
            guiCmdObj.setTabCompleter(guiCmd);
        }
        getServer().getPluginManager().registerEvents(new RankGUIListener(), this);

        getLogger().info("NethrionRanks 1.1.0 enabled — GUI ready (/rankgui).");
    }

    @Override
    public void onDisable() {
        getLogger().info("NethrionRanks disabled.");
    }

    // ─────────────────────────────────────────────────────────────
    //  ORIGINAL COMMAND WIRING
    // ─────────────────────────────────────────────────────────────
    private void registerOriginalCommands() {
        // rank
        var rankCmd = getCommand("rank");
        if (rankCmd != null) {
            var rc = new com.nethrion.ranks.commands.RankCommand(rankLadderManager);
            rankCmd.setExecutor(rc);
            rankCmd.setTabCompleter(rc);
        }
        // skills
        var skillsCmd = getCommand("skills");
        if (skillsCmd != null) {
            var sc = new com.nethrion.ranks.commands.SkillsCommand(rankLadderManager);
            skillsCmd.setExecutor(sc);
            skillsCmd.setTabCompleter(sc);
        }
        // base
        var baseCmd = getCommand("base");
        var baseManager = new com.nethrion.ranks.managers.BaseManager(this);
        if (baseCmd != null) {
            var bc = new com.nethrion.ranks.commands.BaseCommand(baseManager);
            baseCmd.setExecutor(bc);
            baseCmd.setTabCompleter(bc);
        }
        // rankduel
        var duelCmd = getCommand("rankduel");
        if (duelCmd != null) {
            var dc = new com.nethrion.ranks.commands.RankDuelCommand(duelManager, rankLadderManager, pvpManager);
            duelCmd.setExecutor(dc);
            duelCmd.setTabCompleter(dc);
        }
        // killcount
        var killCountCmd = getCommand("killcount");
        if (killCountCmd != null) {
            killCountCmd.setExecutor(new com.nethrion.ranks.commands.KillCountCommand(rankLadderManager));
        }
        // killtop
        var killTopCmd = getCommand("killtop");
        if (killTopCmd != null) {
            killTopCmd.setExecutor(new com.nethrion.ranks.commands.KillTopCommand(rankLadderManager));
        }
        // pvp
        var pvpCmd = getCommand("pvp");
        if (pvpCmd != null) {
            var pc = new com.nethrion.ranks.commands.PvPCommand(pvpManager, duelManager);
            pvpCmd.setExecutor(pc);
            pvpCmd.setTabCompleter(pc);
        }
        // kill (admin)
        var killAdminCmd = getCommand("kill");
        if (killAdminCmd != null) {
            var kac = new com.nethrion.ranks.commands.KillAdminCommand(rankLadderManager);
            killAdminCmd.setExecutor(kac);
            killAdminCmd.setTabCompleter(kac);
        }
        // apply
        var applyCmd = getCommand("apply");
        if (applyCmd != null) {
            var ac = new com.nethrion.ranks.commands.ApplyCommand(minerManager, requestManager);
            applyCmd.setExecutor(ac);
            applyCmd.setTabCompleter(ac);
        }
        // requests
        var requestsCmd = getCommand("requests");
        if (requestsCmd != null) {
            var reqc = new com.nethrion.ranks.commands.RequestsCommand(requestManager, rankLadderManager);
            requestsCmd.setExecutor(reqc);
            requestsCmd.setTabCompleter(reqc);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ORIGINAL LISTENER WIRING
    // ─────────────────────────────────────────────────────────────
    private void registerOriginalListeners() {
        var pm = getServer().getPluginManager();
        var teamWarBridge = new com.nethrion.ranks.integration.TeamWarBridge();

        pm.registerEvents(new com.nethrion.ranks.listeners.BaseListener(
                new com.nethrion.ranks.managers.BaseManager(this)), this);

        pm.registerEvents(new com.nethrion.ranks.listeners.DuelListener(
                duelManager, rankLadderManager, teamWarBridge), this);

        pm.registerEvents(new com.nethrion.ranks.listeners.NationalWeaponListener(
                rankLadderManager, minerManager), this);

        pm.registerEvents(new com.nethrion.ranks.listeners.RankDisplayListener(
                rankLadderManager), this);

        pm.registerEvents(new com.nethrion.ranks.listeners.SpearListener(), this);

        pm.registerEvents(new com.nethrion.ranks.pvp.PvPToggleListener(pvpManager), this);

        // NationalMinerManager implements Listener (block-break /
        // join tracking for mining totals) but was never registered
        // — without this, mining progress would never be recorded.
        pm.registerEvents(minerManager, this);
    }

    // ─────────────────────────────────────────────────────────────
    //  STATIC ACCESSORS (used by GUI and other systems)
    // ─────────────────────────────────────────────────────────────
    public static Main getInstance() {
        return instance;
    }

    public RankLadderManager getRankLadderManager() {
        return rankLadderManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public NationalMinerManager getMinerManager() {
        return minerManager;
    }

    public PvPManager getPvPManager() {
        return pvpManager;
    }

    public RankExitRequestManager getRequestManager() {
        return requestManager;
    }
}
