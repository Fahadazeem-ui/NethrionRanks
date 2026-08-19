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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RankDuelCommand
        implements CommandExecutor, TabCompleter {

    private final DuelManager duelManager;
    private final RankLadderManager ladder;

    public RankDuelCommand(
            DuelManager duelManager,
            RankLadderManager ladder) {
        this.duelManager = duelManager;
        this.ladder = ladder;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    "Player-only command."
            );
            return true;
        }

        ladder.enforceNationalInactivity();

        if (args.length == 0) {
            player.sendMessage(
                    ChatColor.YELLOW +
                            "Usage: /rankduel <player> | " +
                            "/rankduel accept <player>"
            );
            return true;
        }

        if (
                args[0].equalsIgnoreCase(
                        "accept"
                )
        ) {
            handleAccept(
                    player,
                    args
            );
        } else {
            handleChallenge(
                    player,
                    args[0]
            );
        }

        return true;
    }

    private void handleChallenge(
            Player challenger,
            String targetName) {

        Player target =
                Bukkit.getPlayerExact(
                        targetName
                );

        if (
                target == null ||
                        !target.isOnline()
        ) {
            challenger.sendMessage(
                    ChatColor.RED +
                            "Player online nahi hai."
            );
            return;
        }

        if (
                target.getUniqueId().equals(
                        challenger.getUniqueId()
                )
        ) {
            challenger.sendMessage(
                    ChatColor.RED +
                            "Khud ko challenge nahi kar sakte."
            );
            return;
        }

        if (
                duelManager.isInActiveDuel(
                        challenger.getUniqueId()
                ) ||
                        duelManager.isInActiveDuel(
                                target.getUniqueId()
                        )
        ) {
            challenger.sendMessage(
                    ChatColor.RED +
                            "Tum ya target pehle se duel mein hai."
            );
            return;
        }

        if (
                !checkEligibility(
                        challenger,
                        target
                )
        ) {
            return;
        }

        duelManager.sendInvite(
                challenger,
                target
        );

        challenger.sendMessage(
                ChatColor.GREEN +
                        "Challenge sent to " +
                        ChatColor.WHITE +
                        target.getName() +
                        ChatColor.GRAY +
                        " · expires in 60s."
        );

        target.sendMessage(
                ChatColor.GOLD +
                        "⚔ " +
                        ChatColor.WHITE +
                        challenger.getName() +
                        ChatColor.GOLD +
                        " challenged you to a ranked duel."
        );

        target.sendMessage(
                ChatColor.GRAY +
                        "Fair fight only. " +
                        ChatColor.YELLOW +
                        "/rankduel accept " +
                        challenger.getName()
        );
    }

    private boolean checkEligibility(
            Player a,
            Player b) {

        Skill aSkill =
                ladder.getSkill(
                        a.getUniqueId()
                );

        Skill bSkill =
                ladder.getSkill(
                        b.getUniqueId()
                );

        RankTier aTier =
                ladder.getTier(
                        a.getUniqueId()
                );

        RankTier bTier =
                ladder.getTier(
                        b.getUniqueId()
                );

        boolean aNational =
                aTier == RankTier.NATIONAL;

        boolean bNational =
                bTier == RankTier.NATIONAL;

        if (aNational) {
            if (
                    ladder.isNationalEligible(
                            a.getUniqueId()
                    )
            ) {
                a.sendMessage(
                        ChatColor.RED +
                                "National activity is overdue. " +
                                "Complete a formal duel before this restriction is lifted."
                );
                return false;
            }
        }

        if (bNational) {
            if (
                    ladder.isNationalEligible(
                            b.getUniqueId()
                    )
            ) {
                a.sendMessage(
                        ChatColor.RED +
                                b.getName() +
                                " is currently in National inactivity rotation."
                );
                return false;
            }
        }

        /*
         * A Civillian has no locked skill yet and can challenge
         * any ranked opponent.
         */
        if (
                aSkill == null ||
                        bSkill == null
        ) {
            return true;
        }

        /*
         * Cross-skill non-National fights are only allowed
         * at exactly the same tier.
         */
        if (
                aSkill != bSkill &&
                        !aNational &&
                        !bNational &&
                        aTier != bTier
        ) {
            a.sendMessage(
                    ChatColor.RED +
                            "Cross-skill duel sirf same rank tier par allowed hai."
            );
            return false;
        }

        return true;
    }

    private void handleAccept(
            Player acceptor,
            String[] args) {

        if (args.length < 2) {
            acceptor.sendMessage(
                    ChatColor.YELLOW +
                            "Usage: /rankduel accept <player>"
            );
            return;
        }

        Player challenger =
                Bukkit.getPlayerExact(
                        args[1]
                );

        if (
                challenger == null ||
                        !challenger.isOnline()
        ) {
            acceptor.sendMessage(
                    ChatColor.RED +
                            "Challenger online nahi hai."
            );
            return;
        }

        if (
                !duelManager.hasPendingInviteFrom(
                        acceptor.getUniqueId(),
                        challenger.getUniqueId()
                )
        ) {
            acceptor.sendMessage(
                    ChatColor.RED +
                            "Is player ka koi active invite nahi hai."
            );
            return;
        }

        if (
                duelManager.isInActiveDuel(
                        challenger.getUniqueId()
                ) ||
                        duelManager.isInActiveDuel(
                                acceptor.getUniqueId()
                        )
        ) {
            acceptor.sendMessage(
                    ChatColor.RED +
                            "Duel start nahi ho sakti; koi already active hai."
            );
            return;
        }

        if (
                !checkEligibility(
                        challenger,
                        acceptor
                )
        ) {
            return;
        }

        duelManager.clearInvite(
                acceptor.getUniqueId()
        );

        duelManager.startSession(
                challenger.getUniqueId(),
                acceptor.getUniqueId()
        );

        sendDuelTitle(
                challenger
        );

        sendDuelTitle(
                acceptor
        );

        challenger.sendMessage(
                ChatColor.GREEN +
                        "Ranked duel started."
        );

        acceptor.sendMessage(
                ChatColor.GREEN +
                        "Ranked duel started."
        );
    }

    private void sendDuelTitle(
            Player player) {

        player.sendTitle(
                ChatColor.RED +
                        "" +
                        ChatColor.BOLD +
                        "RANKED DUEL",
                ChatColor.GRAY +
                        "Fight fairly · let the system judge",
                10,
                45,
                10
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {

        if (args.length == 1) {
            List<String> values =
                    new ArrayList<>();

            values.add("accept");

            for (Player player :
                    Bukkit.getOnlinePlayers()) {
                if (
                        sender instanceof Player &&
                                player.getUniqueId().equals(
                                        ((Player) sender)
                                                .getUniqueId()
                                )
                ) {
                    continue;
                }

                values.add(
                        player.getName()
                );
            }

            return filter(
                    values,
                    args[0]
            );
        }

        if (
                args.length == 2 &&
                        args[0].equalsIgnoreCase(
                                "accept"
                        )
        ) {
            if (!(sender instanceof Player player)) {
                return List.of();
            }

            List<String> invite =
                    new ArrayList<>();

            for (Player candidate :
                    Bukkit.getOnlinePlayers()) {
                if (
                        duelManager.hasPendingInviteFrom(
                                player.getUniqueId(),
                                candidate.getUniqueId()
                        )
                ) {
                    invite.add(
                            candidate.getName()
                    );
                }
            }

            return filter(
                    invite,
                    args[1]
            );
        }

        return List.of();
    }

    private List<String> filter(
            List<String> values,
            String token) {

        String lower =
                token.toLowerCase(
                        Locale.ROOT
                );

        return values.stream()
                .filter(
                        value ->
                                value.toLowerCase(
                                        Locale.ROOT
                                ).startsWith(lower)
                )
                .toList();
    }
}
