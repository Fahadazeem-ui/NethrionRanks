package com.nethrion.ranks.commands;

import com.nethrion.ranks.managers.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankDuelCommand implements CommandExecutor {

    private final RankManager rankManager;

    // Target UUID -> Challenger UUID. Non-persistent hai — server restart hote hi khaali ho jayega.
    private final Map<UUID, UUID> pendingDuels = new HashMap<>();

    public RankDuelCommand(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Yeh command sirf player use kar sakta hai.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /rankduel <player> | /rankduel accept <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            handleAccept(player, args);
        } else {
            handleChallenge(player, args[0]);
        }

        return true;
    }

    // /rankduel <playername> — challenge bhejna
    private void handleChallenge(Player challenger, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            challenger.sendMessage(ChatColor.RED + "Player '" + targetName + "' online nahi hai.");
            return;
        }

        if (target.getUniqueId().equals(challenger.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "Tum khud ko duel challenge nahi kar sakte!");
            return;
        }

        pendingDuels.put(target.getUniqueId(), challenger.getUniqueId());

        String challengerRank = rankManager.getRank(challenger.getUniqueId());

        challenger.sendMessage(ChatColor.GREEN + "Duel challenge bhej diya " + target.getName() + " ko!");
        target.sendMessage(ChatColor.GOLD + challenger.getName() + " (" + challengerRank + ") " +
                ChatColor.YELLOW + "ne tumhe duel ke liye challenge kiya hai!");
        target.sendMessage(ChatColor.GRAY + "Accept karne ke liye: " + ChatColor.WHITE +
                "/rankduel accept " + challenger.getName());
    }

    // /rankduel accept <playername> — challenge accept karna
    private void handleAccept(Player acceptor, String[] args) {
        if (args.length < 2) {
            acceptor.sendMessage(ChatColor.YELLOW + "Usage: /rankduel accept <player>");
            return;
        }

        String challengerName = args[1];
        Player challenger = Bukkit.getPlayerExact(challengerName);

        if (challenger == null || !challenger.isOnline()) {
            acceptor.sendMessage(ChatColor.RED + "Player '" + challengerName + "' online nahi hai.");
            return;
        }

        UUID storedChallengerUUID = pendingDuels.get(acceptor.getUniqueId());

        if (storedChallengerUUID == null || !storedChallengerUUID.equals(challenger.getUniqueId())) {
            acceptor.sendMessage(ChatColor.RED + challengerName + " se koi pending invite nahi hai.");
            return;
        }

        // Valid hai — entry clear karo aur duel start karo
        pendingDuels.remove(acceptor.getUniqueId());
        startDuel(challenger, acceptor);
    }

    // Duel initialization — dono players ko ready karna aur announce karna
    private void startDuel(Player challenger, Player acceptor) {
        challenger.sendMessage(ChatColor.GREEN + acceptor.getName() + " ne tumhara duel accept kar liya!");
        acceptor.sendMessage(ChatColor.GREEN + "Tumne " + challenger.getName() + " ka duel accept kar liya!");

        // Dono players ko fresh state dena
        challenger.setHealth(20.0);
        challenger.setFoodLevel(20);
        acceptor.setHealth(20.0);
        acceptor.setFoodLevel(20);

        // Challenger ko acceptor ke pass teleport karna taake duel start ho sake
        challenger.teleport(acceptor.getLocation());

        challenger.sendTitle(ChatColor.RED + "Duel Started!", ChatColor.GRAY + "Good luck!", 10, 40, 10);
        acceptor.sendTitle(ChatColor.RED + "Duel Started!", ChatColor.GRAY + "Good luck!", 10, 40, 10);
    }
}
