package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.RankLadderManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KillAdminCommand implements CommandExecutor, TabCompleter {

    private final RankLadderManager ladder;

    public KillAdminCommand(RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (args.length != 3 || !args[0].equalsIgnoreCase("set")) {
            // Preserve vanilla /kill behavior for every non-admin form.
            StringBuilder vanilla = new StringBuilder("minecraft:kill");
            for (String arg : args) {
                vanilla.append(' ').append(arg);
            }
            Bukkit.dispatchCommand(sender, vanilla.toString());
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only server operators can use /kill set.");
            return true;
        }

        int count;
        try {
            count = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Count must be a whole number.");
            return true;
        }

        if (count < 0) {
            sender.sendMessage(ChatColor.RED + "Count cannot be negative.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            ladder.setKills(target.getUniqueId(), count);
            sendSuccess(sender, target.getName(), count);
            target.sendMessage(
                    ChatColor.YELLOW + "Your kill count was set to " +
                            ChatColor.WHITE + count +
                            ChatColor.YELLOW + " by an operator."
            );
            return true;
        }

        var offline = Bukkit.getOfflinePlayer(args[1]);
        if (!offline.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        ladder.setKills(offline.getUniqueId(), count);
        sendSuccess(sender, args[1], count);
        return true;
    }

    private void sendSuccess(CommandSender sender, String name, int count) {
        sender.sendMessage(
                ChatColor.GREEN + "Set " +
                        ChatColor.WHITE + name +
                        ChatColor.GREEN + "'s kills to " +
                        ChatColor.WHITE + count + ChatColor.GREEN + "."
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {

        if (!sender.isOp()) return Collections.emptyList();

        if (args.length == 1) {
            return List.of("set");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String prefix = args[1].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    names.add(player.getName());
                }
            }
            return names;
        }

        return Collections.emptyList();
    }
}
