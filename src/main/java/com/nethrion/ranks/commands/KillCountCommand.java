package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.RankLadderManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KillCountCommand implements CommandExecutor {

    private final RankLadderManager ladder;

    public KillCountCommand(RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player-only command.");
            return true;
        }

        player.sendMessage(
                ChatColor.GOLD + "Ranked duel kills: " +
                        ChatColor.GREEN +
                        ladder.getKills(player.getUniqueId())
        );
        return true;
    }
}
