package com.nethrion.ranks.commands;

import com.nethrion.ranks.managers.BaseManager;
import com.nethrion.ranks.managers.LogEntry;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BaseCommand implements CommandExecutor {

    private final BaseManager baseManager;

    public BaseCommand(BaseManager baseManager) {
        this.baseManager = baseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Yeh command sirf player use kar sakta hai.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /base set | /base logs [time]");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            baseManager.setBase(player);
            player.sendMessage(ChatColor.GREEN + "Tumhara base yahan set ho gaya! (" +
                    BaseManager.RADIUS + "-block radius protected hai)");
            return true;
        }

        if (args[0].equalsIgnoreCase("logs")) {
            if (!baseManager.hasBase(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Tumhara koi base set nahi hai. Pehle /base set karo.");
                return true;
            }

            Long sinceMillis = null; // default: sab logs
            if (args.length >= 2) {
                sinceMillis = parseTimeToMillis(args[1]);
                if (sinceMillis == null) {
                    player.sendMessage(ChatColor.RED + "Ghalat time format! Example: 30m, 2h, 1d");
                    return true;
                }
            }

            List<LogEntry> logs = baseManager.getLogs(player.getUniqueId(), sinceMillis);

            if (logs.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "Is time range mein koi activity record nahi hui.");
                return true;
            }

            player.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Base Activity Logs" + ChatColor.GOLD + " =====");
            for (LogEntry entry : logs) {
                player.sendMessage(ChatColor.GRAY + entry.toDisplayString());
            }
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Usage: /base set | /base logs [time]");
        return true;
    }

    // "30m", "2h", "1d" jaisi strings ko milliseconds mein convert karna
    private Long parseTimeToMillis(String input) {
        try {
            char unit = input.charAt(input.length() - 1);
            long value = Long.parseLong(input.substring(0, input.length() - 1));

            switch (unit) {
                case 'm':
                    return value * 60L * 1000L;
                case 'h':
                    return value * 60L * 60L * 1000L;
                case 'd':
                    return value * 24L * 60L * 60L * 1000L;
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
