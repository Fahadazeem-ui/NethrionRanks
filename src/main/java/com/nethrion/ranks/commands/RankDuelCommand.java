package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.DuelManager;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.RankTier;
import com.nethrion.ranks.rank.Skill;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankDuelCommand implements CommandExecutor {

    private final DuelManager duelManager;
    private final RankLadderManager rankLadderManager;

    public RankDuelCommand(DuelManager duelManager, RankLadderManager rankLadderManager) {
        this.duelManager = duelManager;
        this.rankLadderManager = rankLadderManager;
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

    private void handleChallenge(Player challenger, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            challenger.sendMessage(ChatColor.RED + "Player online nahi hai.");
            return;
        }
        if (target.getUniqueId().equals(challenger.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "Khud ko challenge nahi kar sakte!");
            return;
        }
        if (duelManager.isInActiveDuel(challenger.getUniqueId()) || duelManager.isInActiveDuel(target.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "Tum ya woh player pehle se ek duel mein hain.");
            return;
        }

        // Cross-skill eligibility: sirf same-tier ya National ke against allowed
        if (rankLadderManager.hasSkill(challenger.getUniqueId()) && rankLadderManager.hasSkill(target.getUniqueId())) {
            Skill challengerSkill = rankLadderManager.getSkill(challenger.getUniqueId());
            Skill targetSkill = rankLadderManager.getSkill(target.getUniqueId());

            if (challengerSkill != targetSkill) {
                RankTier challengerTier = rankLadderManager.getTier(challenger.getUniqueId());
                RankTier targetTier = rankLadderManager.getTier(target.getUniqueId());
                boolean nationalInvolved = challengerTier == RankTier.NATIONAL || targetTier == RankTier.NATIONAL;

                if (challengerTier != targetTier && !nationalInvolved) {
                    challenger.sendMessage(ChatColor.RED + "Cross-skill duel sirf same rank-tier ke beech allowed hai (ya National ke khilaf).");
                    return;
                }
            }
        }

        duelManager.sendInvite(challenger, target);

        challenger.sendMessage(ChatColor.GREEN + "Duel invite bhej diya " + target.getName() + " ko! (60 sec mein expire hoga)");
        target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + challenger.getName() +
                ChatColor.YELLOW + " ne tumhe duel ke liye challenge kiya hai!");
        target.sendMessage(ChatColor.GRAY + "Accept karne ke liye: " + ChatColor.WHITE + "/rankduel accept " + challenger.getName());
        target.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "prove it in the arena, not the chat. (60s to respond)");
    }

    private void handleAccept(Player acceptor, String[] args) {
        if (args.length < 2) {
            acceptor.sendMessage(ChatColor.YELLOW + "Usage: /rankduel accept <player>");
            return;
        }

        String challengerName = args[1];
        Player challenger = Bukkit.getPlayerExact(challengerName);

        if (challenger == null || !challenger.isOnline()) {
            acceptor.sendMessage(ChatColor.RED + "Player online nahi hai.");
            return;
        }

        if (!duelManager.hasPendingInviteFrom(acceptor.getUniqueId(), challenger.getUniqueId())) {
            acceptor.sendMessage(ChatColor.RED + challengerName + " se koi pending invite nahi hai.");
            return;
        }

        duelManager.clearInvite(acceptor.getUniqueId());
        duelManager.startSession(challenger.getUniqueId(), acceptor.getUniqueId());

        String title = ChatColor.RED + "" + ChatColor.BOLD + "Duel Started!";
        challenger.sendTitle(title, ChatColor.GRAY + "Fight fair. Good luck.", 10, 40, 10);
        acceptor.sendTitle(title, ChatColor.GRAY + "Fight fair. Good luck.", 10, 40, 10);

        challenger.sendMessage(ChatColor.GREEN + acceptor.getName() + " ne duel accept kar liya! Fight shuru.");
        acceptor.sendMessage(ChatColor.GREEN + "Tumne " + challenger.getName() + " ka duel accept kar liya! Fight shuru.");
    }
}
