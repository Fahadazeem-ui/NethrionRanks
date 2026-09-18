package com.nethrion.ranks.commands;

import com.nethrion.ranks.managers.RankExitRequestManager;
import com.nethrion.ranks.miner.NationalMinerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class ApplyCommand implements CommandExecutor, TabCompleter {
    private final NationalMinerManager miner;
    private final RankExitRequestManager requests;

    public ApplyCommand(NationalMinerManager miner, RankExitRequestManager requests) {
        this.miner = miner;
        this.requests = requests;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /apply <miner|civilian>");
            return true;
        }

        if (args[0].equalsIgnoreCase("miner")) {
            miner.apply(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("civilian")) {
            if (miner.isNationalMiner(player.getUniqueId())) {
                if (requests.request(player.getUniqueId())) {
                    // NationalMiner is a dedicated role rather than a normal
                    // ladder rank; approval below also removes its weapon.
                    player.sendMessage(ChatColor.GREEN + "Your NationalMiner exit request was sent to the operators.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You already have a pending request or are not eligible for a rank exit request.");
                }
                return true;
            }

            if (requests.request(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "Your rank-exit request was sent to the operators.");
            } else {
                player.sendMessage(ChatColor.YELLOW + "You already have a pending request, or you have no active rank.");
            }
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Usage: /apply <miner|civilian>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("miner", "civilian"), args[0]);
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String token) {
        List<String> out = new ArrayList<>();
        for (String v : values)
            if (v.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))) out.add(v);
        return out;
    }
}
