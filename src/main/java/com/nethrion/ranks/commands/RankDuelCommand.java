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
    private final RankLadderManager ladder;

    public RankDuelCommand(DuelManager duelManager, RankLadderManager ladder) {
        this.duelManager = duelManager;
        this.ladder = ladder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player-only command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW +
                    "Usage: /rankduel <player> | /rankduel accept <player>");
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
            challenger.sendMessage(ChatColor.RED + "Khud ko challenge nahi kar sakte.");
            return;
        }

        if (duelManager.isInActiveDuel(challenger.getUniqueId()) ||
                duelManager.isInActiveDuel(target.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "Tum ya target pehle se duel mein hai.");
            return;
        }

        if (!checkEligibility(challenger, target)) return;

        duelManager.sendInvite(challenger, target);

        challenger.sendMessage(ChatColor.GREEN +
                "Ranked duel invite " + target.getName() + " ko bhej diya. 60 seconds.");
        target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD +
                challenger.getName() + ChatColor.YELLOW +
                " ne tumhe ranked duel challenge kiya hai.");
        target.sendMessage(ChatColor.WHITE +
                "/rankduel accept " + challenger.getName());
    }

    private boolean checkEligibility(Player a, Player b) {
        Skill aSkill = ladder.getSkill(a.getUniqueId());
        Skill bSkill = ladder.getSkill(b.getUniqueId());
        RankTier aTier = ladder.getTier(a.getUniqueId());
        RankTier bTier = ladder.getTier(b.getUniqueId());

        boolean aNational = aTier == RankTier.NATIONAL;
        boolean bNational = bTier == RankTier.NATIONAL;

        if (aNational || bNational) {
            if (!ladder.isNationalEligible(a.getUniqueId())) {
                a.sendMessage(ChatColor.RED +
                        "National duel cooldown: " +
                        ladder.formatCooldown(
                                ladder.getRemainingNationalCooldownMillis(a.getUniqueId())));
                return false;
            }
            if (!ladder.isNationalEligible(b.getUniqueId())) {
                a.sendMessage(ChatColor.RED +
                        b.getName() + " abhi National cooldown par hai.");
                return false;
            }
            return true;
        }

        // Different skills can duel each other only inside the same tier.
        if (aSkill != null && bSkill != null &&
                aSkill != bSkill && aTier != bTier) {
            a.sendMessage(ChatColor.RED +
                    "Cross-skill duel sirf same tier par allowed hai, ya National ke against.");
            return false;
        }

        return true;
    }

    private void handleAccept(Player acceptor, String[] args) {
        if (args.length < 2) {
            acceptor.sendMessage(ChatColor.YELLOW +
                    "Usage: /rankduel accept <player>");
            return;
        }

        Player challenger = Bukkit.getPlayerExact(args[1]);
        if (challenger == null || !challenger.isOnline()) {
            acceptor.sendMessage(ChatColor.RED + "Challenger online nahi hai.");
            return;
        }

        if (!duelManager.hasPendingInviteFrom(
                acceptor.getUniqueId(),
                challenger.getUniqueId())) {
            acceptor.sendMessage(ChatColor.RED +
                    "Is player ka koi active invite nahi hai.");
            return;
        }

        if (duelManager.isInActiveDuel(challenger.getUniqueId()) ||
                duelManager.isInActiveDuel(acceptor.getUniqueId())) {
            acceptor.sendMessage(ChatColor.RED + "Duel start nahi ho sakti; koi already active hai.");
            return;
        }

        if (!checkEligibility(challenger, acceptor)) return;

        duelManager.clearInvite(acceptor.getUniqueId());
        duelManager.startSession(
                challenger.getUniqueId(),
                acceptor.getUniqueId()
        );

        challenger.sendTitle(
                ChatColor.RED + "" + ChatColor.BOLD + "RANKED DUEL",
                ChatColor.GRAY + "Fight has begun.",
                10, 40, 10
        );
        acceptor.sendTitle(
                ChatColor.RED + "" + ChatColor.BOLD + "RANKED DUEL",
                ChatColor.GRAY + "Fight has begun.",
                10, 40, 10
        );

        challenger.sendMessage(ChatColor.GREEN +
                "Duel accepted. Weapon-dominance aur minimum-time rules active hain.");
        acceptor.sendMessage(ChatColor.GREEN +
                "Duel accepted. Weapon-dominance aur minimum-time rules active hain.");
    }
}
