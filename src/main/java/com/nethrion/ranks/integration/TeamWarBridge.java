package com.nethrion.ranks.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * Optional, reflection-based integration with NethrionTeams.
 *
 * NethrionRanks does not compile against or embed NethrionTeams. When the
 * plugin is present, the TeamManager is queried for the exact current
 * active war of each player's team. The exemption is valid only when both
 * players are in the same ACTIVE war and belong to different participating
 * teams. If the teams plugin is absent or its API is unavailable, the
 * integration safely fails closed (no war exemption).
 */
public final class TeamWarBridge {

    private static final String PLUGIN_NAME = "NethrionTeams";

    private Object teamManager;
    private Method findByPlayer;
    private Method activeWar;
    private Method teamId;
    private Method warId;
    private Method warStatus;
    private Method warTeams;
    private boolean initialized;

    public TeamWarBridge() {
        initialize();
    }

    public boolean isOpposingActiveWar(Player a, Player b) {
        if (a == null || b == null ||
                a.getUniqueId().equals(b.getUniqueId())) {
            return false;
        }

        if (!initialized) {
            initialize();
        }

        if (!initialized) {
            return false;
        }

        try {
            Object teamA = findByPlayer.invoke(
                    teamManager,
                    a.getUniqueId()
            );
            Object teamB = findByPlayer.invoke(
                    teamManager,
                    b.getUniqueId()
            );

            if (teamA == null || teamB == null) {
                return false;
            }

            UUID teamAId = (UUID) teamId.invoke(teamA);
            UUID teamBId = (UUID) teamId.invoke(teamB);

            if (teamAId == null || teamBId == null ||
                    teamAId.equals(teamBId)) {
                return false;
            }

            Object warA = activeWar.invoke(teamManager, teamAId);
            Object warB = activeWar.invoke(teamManager, teamBId);

            if (warA == null || warB == null) {
                return false;
            }

            UUID warAId = (UUID) warId.invoke(warA);
            UUID warBId = (UUID) warId.invoke(warB);

            if (warAId == null || !warAId.equals(warBId)) {
                return false;
            }

            Object status = warStatus.invoke(warA);
            if (status == null || !"ACTIVE".equals(status.toString())) {
                return false;
            }

            Object teamsObject = warTeams.invoke(warA);
            if (!(teamsObject instanceof Set<?> teams)) {
                return false;
            }

            return teams.contains(teamAId) && teams.contains(teamBId);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            // Never grant a war exemption when the external plugin's API
            // cannot be read reliably.
            return false;
        }
    }

    private void initialize() {
        initialized = false;

        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (plugin == null || !plugin.isEnabled()) {
                return;
            }

            Field managerField =
                    plugin.getClass().getDeclaredField("manager");
            managerField.setAccessible(true);

            Object manager = managerField.get(plugin);
            if (manager == null) {
                return;
            }

            Class<?> managerClass = manager.getClass();
            Class<?> teamClass =
                    Class.forName("com.nethrion.teams.Team");
            Class<?> warClass =
                    Class.forName("com.nethrion.teams.TeamWar");

            findByPlayer =
                    managerClass.getMethod("findByPlayer", UUID.class);
            activeWar =
                    managerClass.getMethod("activeWar", UUID.class);
            teamId =
                    teamClass.getMethod("getId");
            warId =
                    warClass.getMethod("id");
            warStatus =
                    warClass.getMethod("status");
            warTeams =
                    warClass.getMethod("teams");

            teamManager = manager;
            initialized = true;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            teamManager = null;
        }
    }
}
