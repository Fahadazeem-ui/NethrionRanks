package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class KillTopCommand implements CommandExecutor {

    private final RankLadderManager ladder;

    public KillTopCommand(RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<PlayerRankProfile> top = ladder.getTopKillers(3);

        sender.sendMessage(ChatColor.GOLD + "========== TOP KILLERS ==========");

        if (top.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No ranked kills yet.");
        }

        int place = 1;
        for (PlayerRankProfile profile : top) {
            var player = Bukkit.getPlayer(profile.getUuid());
            String name = player != null
                    ? player.getName()
                    : profile.getUuid().toString().substring(0, 8);

            sender.sendMessage(
                    ChatColor.YELLOW + "#" + place +
                            ChatColor.WHITE + " " + name +
                            ChatColor.GRAY + " — " +
                            ChatColor.GREEN + profile.getKills()
            );
            place++;
        }

        sender.sendMessage(ChatColor.GOLD + "=================================");
        return true;
    }
}
