package com.nethrion.ranks.listeners;

import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.RankTier;
import com.nethrion.ranks.rank.Skill;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class RankDisplayListener implements Listener {

    private final RankLadderManager ladder;

    public RankDisplayListener(
            RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    public void refresh(Player player) {
        if (player == null) return;

        ladder.refreshOnlineDisplay(
                player.getUniqueId()
        );

        applyTeamDisplay(player);
    }

    private void applyTeamDisplay(
            Player player) {

        Scoreboard scoreboard =
                Bukkit.getScoreboardManager()
                        .getMainScoreboard();

        String teamName =
                "nr_" +
                        player.getUniqueId()
                                .toString()
                                .replace("-", "")
                                .substring(0, 12);

        Team team =
                scoreboard.getTeam(teamName);

        if (team == null) {
            team =
                    scoreboard.registerNewTeam(
                            teamName
                    );
        }

        String prefix =
                buildPrefix(player);

        team.setPrefix(prefix);
        team.setSuffix(ChatColor.RESET.toString());

        if (!team.hasEntry(player.getName())) {
            team.addEntry(
                    player.getName()
            );
        }
    }

    private String buildPrefix(
            Player player) {

        var profile =
                ladder.getProfile(
                        player.getUniqueId()
                );

        RankTier tier =
                profile.getTier();

        if (
                tier == RankTier.CIVILLIAN ||
                        profile.getSkill() == null
        ) {
            return ChatColor.GRAY +
                    "Civillian ";
        }

        Skill skill =
                profile.getSkill();

        return tierColor(tier) +
                tier.getDisplayName() +
                " " +
                skill.getMasterTitle() +
                ChatColor.GRAY +
                " ";
    }

    private ChatColor tierColor(
            RankTier tier) {

        return switch (tier) {
            case NATIONAL -> ChatColor.GOLD;
            case S -> ChatColor.DARK_RED;
            case A -> ChatColor.RED;
            case B -> ChatColor.DARK_PURPLE;
            case C -> ChatColor.LIGHT_PURPLE;
            case D -> ChatColor.BLUE;
            case E -> ChatColor.AQUA;
            case CIVILLIAN -> ChatColor.GRAY;
        };
    }
}
