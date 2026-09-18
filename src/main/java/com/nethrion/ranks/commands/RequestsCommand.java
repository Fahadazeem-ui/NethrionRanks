package com.nethrion.ranks.commands;

import com.nethrion.ranks.managers.RankExitRequestManager;
import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class RequestsCommand implements CommandExecutor, TabCompleter {
    private final RankExitRequestManager requests;
    private final RankLadderManager ladder;

    public RequestsCommand(RankExitRequestManager requests, RankLadderManager ladder) {
        this.requests = requests;
        this.ladder = ladder;
    }

    private boolean admin(CommandSender s) {
        if (!s.hasPermission("nethrionranks.admin")) {
            s.sendMessage(ChatColor.RED + "No permission.");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!admin(sender)) return true;

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            if (requests.getPending().isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "No pending rank-exit requests.");
                return true;
            }
            sender.sendMessage(ChatColor.GOLD + "Pending Rank Requests");
            for (UUID uuid : requests.getPending()) {
                PlayerRankProfile p = ladder.getProfile(uuid);
                String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString());
                sender.sendMessage(ChatColor.YELLOW + name + ChatColor.GRAY + " — "
                        + ChatColor.WHITE + p.getDisplayPrefix());
            }
            sender.sendMessage(ChatColor.GRAY + "Use /requests approve <player>.");
            return true;
        }

        if (args[0].equalsIgnoreCase("approve") && args.length >= 2) {
            UUID uuid = resolveKnownPlayer(args[1]);
            if (uuid == null || !requests.hasPending(uuid)) {
                sender.sendMessage(ChatColor.RED + "No pending request found for that player.");
                return true;
            }

            PlayerRankProfile before = ladder.getProfile(uuid);
            String oldDisplay = before.getDisplayPrefix();
            if (!requests.approve(uuid)) {
                sender.sendMessage(ChatColor.RED + "Could not approve that request.");
                return true;
            }

            String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(args[1]);
            sender.sendMessage(ChatColor.GREEN + name + " reset from " + oldDisplay + " to Civillian.");
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel") && args.length >= 2) {
            UUID uuid = resolveKnownPlayer(args[1]);
            if (uuid == null || !requests.cancel(uuid)) {
                sender.sendMessage(ChatColor.RED + "No pending request found for that player.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Request cancelled.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /requests | /requests approve <player> | /requests cancel <player>");
        return true;
    }

    private UUID resolveKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (!offline.hasPlayedBefore()) return null;
        return offline.getUniqueId();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nethrionranks.admin")) return Collections.emptyList();
        if (args.length == 1) return filter(Arrays.asList("list", "approve", "cancel"), args[0]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("approve") || args[0].equalsIgnoreCase("cancel"))) {
            List<String> names = new ArrayList<>();
            for (UUID uuid : requests.getPending()) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name != null) names.add(name);
            }
            return filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String token) {
        List<String> out = new ArrayList<>();
        for (String v : values) if (v.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))) out.add(v);
        return out;
    }
}
