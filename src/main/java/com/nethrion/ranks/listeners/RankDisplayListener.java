package com.nethrion.ranks.listeners;

import com.nethrion.ranks.rank.RankLadderManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Refreshes a player's rank display (tab list name, chat display name,
 * and scoreboard team prefix/suffix) whenever they join.
 *
 * All of the actual prefix-building - gradients, badges, tier text - lives
 * in exactly one place: {@link RankLadderManager#refreshOnlineDisplay}.
 * This class used to keep its OWN second copy of that logic (a separate
 * buildPrefix/rankBadge/skillIcon/tierColor set, styled with flat colors
 * and the old badge-icon-skill layout) and applied it to the *same*
 * scoreboard team right after refreshOnlineDisplay ran. Because both
 * writes targeted the identical team name ("nr_" + uuid prefix), the
 * second write silently clobbered the first on every single join -
 * meaning any styling change made in RankLadderManager (like the tier
 * gradients) would visibly "revert" the moment a player joined. That
 * duplicate logic has been removed entirely; this listener now only
 * triggers the single source of truth.
 */
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
    }
}
