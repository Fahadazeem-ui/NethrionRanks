package com.nethrion.ranks.commands;

import com.nethrion.ranks.pvp.PvPManager;
import com.nethrion.ranks.rank.DuelManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class PvPCommand implements CommandExecutor, TabCompleter {
    private final PvPManager manager;
    private final DuelManager duelManager;

    public PvPCommand(PvPManager manager, DuelManager duelManager) {
        this.manager = manager;
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player-only command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "PvP: " +
                    (manager.isEnabled(player.getUniqueId())
                            ? ChatColor.GREEN + "ON"
                            : ChatColor.RED + "OFF"));
            return true;
        }

        String value = args[0].toLowerCase(Locale.ROOT);
        if (!value.equals("on") && !value.equals("off")) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /pvp [on|off]");
            return true;
        }

        boolean enabled = value.equals("on");

        if (!enabled && duelManager.isInActiveDuel(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED +
                    "Active Rank Duel ke dauran PvP OFF nahi kar sakte.");
            return true;
        }

        if (manager.isEnabled(player.getUniqueId()) == enabled) {
            player.sendMessage(ChatColor.YELLOW + "PvP already " +
                    (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF") +
                    ChatColor.YELLOW + ".");
            return true;
        }

        if (!manager.setEnabled(player.getUniqueId(), enabled)) {
            player.sendMessage(ChatColor.RED + "PvP preference save nahi ho saki.");
            return true;
        }

        player.sendMessage(enabled
                ? ChatColor.GREEN + "PvP ON — tum PvP fights kar sakte ho."
                : ChatColor.RED + "PvP OFF — tum PvP damage nahi doge aur PvP damage receive nahi karoge.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !(sender instanceof Player)) return List.of();
        String token = args[0].toLowerCase(Locale.ROOT);
        return List.of("on", "off").stream().filter(v -> v.startsWith(token)).toList();
    }
}
