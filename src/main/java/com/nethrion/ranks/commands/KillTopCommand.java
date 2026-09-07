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
            var online = Bukkit.getPlayer(profile.getUuid());

            String name;
            if (online != null) {
                name = online.getName();
            } else {
                // Fall back to the offline player's actual last-known
                // username instead of a chunk of their UUID - that's
                // what was showing up as things like "00000000" /
                // "108ccb30" before.
                var offlineName =
                        Bukkit.getOfflinePlayer(profile.getUuid()).getName();
                name = offlineName != null
                        ? offlineName
                        : "Unknown";
            }

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
