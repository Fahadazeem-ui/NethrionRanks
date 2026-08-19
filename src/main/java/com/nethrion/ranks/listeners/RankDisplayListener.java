package com.nethrion.ranks.listeners;

import com.nethrion.ranks.rank.RankLadderManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RankDisplayListener implements Listener {

    private final RankLadderManager ladder;

    public RankDisplayListener(RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ladder.refreshOnlineDisplay(event.getPlayer().getUniqueId());
    }
}
