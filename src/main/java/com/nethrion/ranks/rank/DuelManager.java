package com.nethrion.ranks.rank;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DuelManager {

    private final JavaPlugin plugin;
    private final RankLadderManager rankLadderManager;

    // Target UUID -> Challenger UUID
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final List<DuelSession> activeSessions = new ArrayList<>();

    public DuelManager(JavaPlugin plugin, RankLadderManager rankLadderManager) {
        this.plugin = plugin;
        this.rankLadderManager = rankLadderManager;
    }

    public RankLadderManager getRankLadderManager() {
        return rankLadderManager;
    }

    // ---------- Invites ----------

    public void sendInvite(Player challenger, Player target) {
        UUID challengerUUID = challenger.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        pendingInvites.put(targetUUID, challengerUUID);

        // 60 seconds = 1200 ticks — expiry ke baad khud clear ho jayega
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            UUID stillPending = pendingInvites.get(targetUUID);
            if (stillPending != null && stillPending.equals(challengerUUID)) {
                pendingInvites.remove(targetUUID);

                Player c = Bukkit.getPlayer(challengerUUID);
                Player t = Bukkit.getPlayer(targetUUID);
                if (c != null) c.sendMessage("§cInvite expire ho gaya — " + (t != null ? t.getName() : "player") + " ne accept nahi kiya.");
                if (t != null) t.sendMessage("§7Duel invite expire ho gaya.");
            }
        }, 1200L);
    }

    public boolean hasPendingInviteFrom(UUID target, UUID challenger) {
        UUID stored = pendingInvites.get(target);
        return stored != null && stored.equals(challenger);
    }

    public void clearInvite(UUID target) {
        pendingInvites.remove(target);
    }

    // ---------- Active Sessions ----------

    public boolean isInActiveDuel(UUID uuid) {
        return getActiveSession(uuid) != null;
    }

    public DuelSession getActiveSession(UUID uuid) {
        for (DuelSession session : activeSessions) {
            if (session.involves(uuid)) return session;
        }
        return null;
    }

    public DuelSession startSession(UUID playerA, UUID playerB) {
        DuelSession session = new DuelSession(playerA, playerB);
        activeSessions.add(session);
        return session;
    }

    public void endSession(DuelSession session) {
        activeSessions.remove(session);
    }
}
