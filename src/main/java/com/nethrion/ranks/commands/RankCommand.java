package com.nethrion.ranks.commands;

import com.nethrion.ranks.managers.RankManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {

    private final RankManager rankManager;

    public RankCommand(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Yeh command sirf player use kar sakta hai.");
            return true;
        }

        Player player = (Player) sender;
        String currentRank = rankManager.getRank(player.getUniqueId());
        String[] ladder = rankManager.getRankLadder();

        player.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Tumhara Rank" + ChatColor.GOLD + " =====");
        player.sendMessage(ChatColor.GRAY + "Current Rank: " + ChatColor.GREEN + currentRank);

        // Poora ladder dikhana, current rank ko highlight karke
        StringBuilder ladderDisplay = new StringBuilder();
        for (int i = 0; i < ladder.length; i++) {
            if (ladder[i].equalsIgnoreCase(currentRank)) {
                ladderDisplay.append(ChatColor.GREEN).append("[").append(ladder[i]).append("]").append(ChatColor.GRAY);
            } else {
                ladderDisplay.append(ladder[i]);
            }
            if (i < ladder.length - 1) {
                ladderDisplay.append(" -> ");
            }
        }
        player.sendMessage(ChatColor.GRAY + "Ladder: " + ladderDisplay);

        return true;
    }
}
