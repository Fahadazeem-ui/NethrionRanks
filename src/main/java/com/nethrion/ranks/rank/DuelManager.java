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
    private final RankLadderManager ladder;

    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final List<DuelSession> activeSessions = new ArrayList<>();

    public DuelManager(JavaPlugin plugin, RankLadderManager ladder) {
        this.plugin = plugin;
        this.ladder = ladder;
    }

    public RankLadderManager getRankLadderManager() {
        return ladder;
    }

    public void sendInvite(Player challenger, Player target) {
        UUID challengerUUID = challenger.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        pendingInvites.put(targetUUID, challengerUUID);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            UUID current = pendingInvites.get(targetUUID);
            if (challengerUUID.equals(current)) {
                pendingInvites.remove(targetUUID);

                Player c = Bukkit.getPlayer(challengerUUID);
                Player t = Bukkit.getPlayer(targetUUID);

                if (c != null) {
                    c.sendMessage("§cRanked duel invite expire ho gaya.");
                }
                if (t != null) {
                    t.sendMessage("§7Ranked duel invite expire ho gaya.");
                }
            }
        }, 20L * 60L);
    }

    public boolean hasAnyPendingInvite(UUID target) {
        return pendingInvites.containsKey(target);
    }

    public boolean hasPendingInviteFrom(UUID target, UUID challenger) {
        return challenger.equals(pendingInvites.get(target));
    }

    public void clearInvite(UUID target) {
        pendingInvites.remove(target);
    }

    public boolean isInActiveDuel(UUID uuid) {
        return getActiveSession(uuid) != null;
    }

    public DuelSession getActiveSession(UUID uuid) {
        for (DuelSession session : activeSessions) {
            if (session.involves(uuid)) return session;
        }
        return null;
    }

    public DuelSession startSession(UUID a, UUID b) {
        DuelSession session = new DuelSession(a, b);
        activeSessions.add(session);
        return session;
    }

    public void endSession(DuelSession session) {
        activeSessions.remove(session);
    }

    public void cancelSessionFor(UUID uuid) {
        DuelSession session = getActiveSession(uuid);
        if (session != null) {
            activeSessions.remove(session);
        }
    }
}
