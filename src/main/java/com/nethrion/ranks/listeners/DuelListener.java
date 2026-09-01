package com.nethrion.ranks.listeners;

import com.nethrion.ranks.rank.DuelManager;
import com.nethrion.ranks.rank.DuelResult;
import com.nethrion.ranks.rank.DuelSession;
import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.RankTier;
import com.nethrion.ranks.rank.Skill;
import com.nethrion.ranks.rank.WeaponUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class DuelListener implements Listener {

    private static final long OUTLAW_DURATION_MILLIS =
            3L * 60L * 1000L;

    private static final int MAX_OUTLAW_RESETS = 3;

    private final DuelManager duelManager;
    private final RankLadderManager ladder;

    public DuelListener(DuelManager duelManager, RankLadderManager ladder) {
        this.duelManager = duelManager;
        this.ladder = ladder;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        if (
                !attacker.getUniqueId().equals(
                        victim.getUniqueId()
                ) &&
                ladder.getProfile(
                        victim.getUniqueId()
                ).isOutlaw()
        ) {
            PlayerRankProfile victimProfile =
                    ladder.getProfile(
                            victim.getUniqueId()
                    );

            if (
                    ladder.refreshOutlawPenalty(
                            victim.getUniqueId(),
                            OUTLAW_DURATION_MILLIS,
                            MAX_OUTLAW_RESETS
                    )
            ) {
                ladder.refreshOnlineDisplay(
                        victim.getUniqueId()
                );

                applyOutlawEffects(
                        victim,
                        victimProfile
                );

                victim.sendActionBar(
                        ChatColor.RED +
                                "Outlaw penalty refreshed: " +
                                ChatColor.WHITE +
                                "3:00"
                );
            }
        }

        DuelSession session =
                duelManager.getActiveSession(attacker.getUniqueId());

        if (session == null || !session.involves(victim.getUniqueId())) return;

        Skill skill = resolveSkillUsed(event, attacker);
        session.recordDamage(
                attacker.getUniqueId(),
                skill,
                event.getFinalDamage()
        );
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }

        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    private Skill resolveSkillUsed(EntityDamageByEntityEvent event, Player attacker) {
        if (event.getDamager() instanceof Arrow) {
            return Skill.BOW;
        }

        if (event.getDamager() instanceof Projectile) {
            if (event.getDamager() instanceof Arrow) {
                return Skill.BOW;
            }
            return WeaponUtil.fromItemStack(attacker.getInventory().getItemInMainHand());
        }

        return WeaponUtil.fromItemStack(
                attacker.getInventory().getItemInMainHand()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        Player killer = loser.getKiller();

        if (killer == null) return;

        DuelSession session =
                duelManager.getActiveSession(killer.getUniqueId());

        if (session == null || !session.involves(loser.getUniqueId())) {
            PlayerRankProfile victimProfile =
                    ladder.getProfile(loser.getUniqueId());

            if (victimProfile.isOutlaw()) {
                handleOutlawBountyClaim(killer, loser);
                return;
            }

            applyInnocentKillPenalty(killer, loser);
            return;
        }

        duelManager.endSession(session);

        Skill winnerDominant =
                session.getDominantSkill(killer.getUniqueId());
        Skill loserDominant =
                session.getDominantSkill(loser.getUniqueId());

        if (winnerDominant == null || loserDominant == null) {
            voidMatch(killer, loser,
                    "Weapon usage 60% clear majority tak nahi pohanchi.");
            return;
        }

        Skill lockedWinner = ladder.getSkill(killer.getUniqueId());
        Skill lockedLoser = ladder.getSkill(loser.getUniqueId());

        if (lockedWinner != null && lockedWinner != winnerDominant) {
            voidMatch(killer, loser,
                    killer.getName() + " ne apni permanent skill se bahar dominant weapon use kiya.");
            return;
        }

        if (lockedLoser != null && lockedLoser != loserDominant) {
            voidMatch(killer, loser,
                    loser.getName() + " ne apni permanent skill se bahar dominant weapon use kiya.");
            return;
        }

        long minimum =
                Math.min(
                        WeaponUtil.getMinDurationMillis(winnerDominant),
                        WeaponUtil.getMinDurationMillis(loserDominant)
                );

        if (session.getElapsedMillis() < minimum) {
            voidMatch(
                    killer,
                    loser,
                    "Duel minimum required time (" +
                            (minimum / 1000L) +
                            "s) se pehle khatam ho gayi."
            );
            return;
        }

        if (lockedWinner == null) {
            ladder.assignSkillIfAbsent(
                    killer.getUniqueId(),
                    winnerDominant
            );
        }

        if (lockedLoser == null) {
            ladder.assignSkillIfAbsent(
                    loser.getUniqueId(),
                    loserDominant
            );
        }

        DuelResult result =
                ladder.resolveDuel(
                        killer.getUniqueId(),
                        winnerDominant,
                        loser.getUniqueId(),
                        loserDominant
                );

        if (
                result.getType() ==
                        DuelResult.Type.VOID
        ) {
            voidMatch(
                    killer,
                    loser,
                    "This duel combination is not allowed."
            );
            return;
        }

        ladder.addKill(killer.getUniqueId());

        long winnerXp =
                Math.round(session.getDamage(killer.getUniqueId())
                        .values()
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .sum() * 2.0);

        long loserXp =
                Math.round(session.getDamage(loser.getUniqueId())
                        .values()
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .sum());

        ladder.addSkillXp(
                killer.getUniqueId(),
                winnerDominant,
                Math.max(10L, winnerXp)
        );

        ladder.addSkillXp(
                loser.getUniqueId(),
                loserDominant,
                Math.max(5L, loserXp)
        );

        announceResult(killer, loser, result, winnerDominant, loserDominant);
    }

    @EventHandler
    public void onQuit(
            PlayerQuitEvent event) {

        DuelSession session =
                duelManager.getActiveSession(
                        event.getPlayer().getUniqueId()
                );

        if (session == null) {
            return;
        }

        UUID leaver =
                event.getPlayer().getUniqueId();

        UUID opponent =
                session.getOpponent(leaver);

        Player winner =
                Bukkit.getPlayer(opponent);

        duelManager.endSession(session);

        if (winner == null) {
            return;
        }

        Skill winnerSkill =
                session.getDominantSkill(
                        winner.getUniqueId()
                );

        Skill loserSkill =
                session.getDominantSkill(leaver);

        if (
                winnerSkill == null ||
                        loserSkill == null
        ) {
            winner.sendMessage(
                    ChatColor.GRAY +
                            "Duel cancelled: no clear weapon dominance."
            );
            return;
        }

        long minimum =
                Math.min(
                        WeaponUtil.getMinDurationMillis(
                                winnerSkill
                        ),
                        WeaponUtil.getMinDurationMillis(
                                loserSkill
                        )
                );

        if (
                session.getElapsedMillis() <
                        minimum
        ) {
            winner.sendMessage(
                    ChatColor.GRAY +
                            "Duel cancelled: minimum time was not reached."
            );
            return;
        }

        Skill lockedWinner =
                ladder.getSkill(
                        winner.getUniqueId()
                );

        Skill lockedLoser =
                ladder.getSkill(
                        leaver
                );

        if (
                lockedWinner != null &&
                        lockedWinner != winnerSkill
        ) {
            winner.sendMessage(
                    ChatColor.GRAY +
                            "Duel cancelled: your locked skill was violated."
            );
            return;
        }

        if (
                lockedLoser != null &&
                        lockedLoser != loserSkill
        ) {
            winner.sendMessage(
                    ChatColor.GRAY +
                            "Duel cancelled: opponent's locked skill was violated."
            );
            return;
        }

        if (lockedWinner == null) {
            ladder.assignSkillIfAbsent(
                    winner.getUniqueId(),
                    winnerSkill
            );
        }

        if (lockedLoser == null) {
            ladder.assignSkillIfAbsent(
                    leaver,
                    loserSkill
            );
        }

        DuelResult result =
                ladder.resolveDuel(
                        winner.getUniqueId(),
                        winnerSkill,
                        leaver,
                        loserSkill
                );

        if (
                result.getType() ==
                        DuelResult.Type.VOID
        ) {
            return;
        }

        ladder.addKill(
                winner.getUniqueId()
        );

        ladder.addSkillXp(
                winner.getUniqueId(),
                winnerSkill,
                100L
        );

        ladder.addSkillXp(
                leaver,
                loserSkill,
                50L
        );

        winner.sendTitle(
                ChatColor.GREEN +
                        "DUEL WON",
                ChatColor.GRAY +
                        "Opponent disconnected.",
                10,
                50,
                10
        );

        winner.sendMessage(
                ChatColor.GREEN +
                        "Ranked duel resolved after opponent disconnected."
        );

        ladder.refreshOnlineDisplay(
                winner.getUniqueId()
        );
    }

    private void applyInnocentKillPenalty(
            Player killer,
            Player victim) {

        PlayerRankProfile profile =
                ladder.getProfile(
                        killer.getUniqueId()
                );

        boolean bountyAlreadyActive = profile.isOutlaw();
        RankTier oldTier = profile.getTier();
        Skill oldSkill = profile.getSkill();

        ladder.resolveInnocentKillRankSwap(
                killer.getUniqueId(),
                victim.getUniqueId()
        );

        ladder.startOutlawPenalty(
                killer.getUniqueId(),
                OUTLAW_DURATION_MILLIS
        );

        profile =
                ladder.getProfile(
                        killer.getUniqueId()
                );

        applyOutlawEffects(
                killer,
                profile
        );

        RankTier newTier = profile.getTier();
        Skill newSkill = profile.getSkill();

        killer.sendMessage(
                ChatColor.RED +
                        "Innocent kill: " +
                        ChatColor.YELLOW +
                        "rank changed " +
                        ChatColor.WHITE +
                        oldTier.getDisplayName() +
                        " " +
                        (oldSkill == null ? "" : oldSkill.getMasterTitle()) +
                        ChatColor.GRAY +
                        " → " +
                        ChatColor.YELLOW +
                        newTier.getDisplayName() +
                        " " +
                        (newSkill == null ? "" : newSkill.getMasterTitle())
        );

        killer.sendMessage(
                ChatColor.RED +
                        "Outlaw Level " +
                        ChatColor.YELLOW +
                        profile.getOutlawLevel() +
                        ChatColor.RED +
                        " · 3 minutes."
        );

        if (victim != null) {
            Bukkit.broadcastMessage(
                    ChatColor.DARK_RED +
                            "⚠ " +
                            ChatColor.RED +
                            killer.getName() +
                            ChatColor.GRAY +
                            " killed " +
                            ChatColor.RED +
                            victim.getName() +
                            ChatColor.GRAY +
                            " outside a ranked duel."
            );

            if (!bountyAlreadyActive) {
                Bukkit.broadcastMessage(
                        ChatColor.GOLD +
                                "✦ " +
                                ChatColor.BOLD +
                                "BOUNTY " +
                                ChatColor.YELLOW +
                                killer.getName() +
                                ChatColor.GRAY +
                                " — kill them during the outlaw period for " +
                                ChatColor.GREEN +
                                "+1 rank " +
                                ChatColor.GRAY +
                                "in your skill."
                );
            }
        }
    }

    private void handleOutlawBountyClaim(
            Player hunter,
            Player outlaw) {

        PlayerRankProfile outlawProfile =
                ladder.getProfile(outlaw.getUniqueId());

        if (!outlawProfile.isOutlaw()) {
            applyInnocentKillPenalty(hunter, outlaw);
            return;
        }

        Skill hunterSkill =
                ladder.getSkill(hunter.getUniqueId());
        RankTier oldTier =
                ladder.getTier(hunter.getUniqueId());

        UUID bumped =
                ladder.claimOutlawBounty(
                        hunter.getUniqueId()
                );

        RankTier newTier =
                ladder.getTier(hunter.getUniqueId());

        // Claim is consumed exactly once so the same outlaw period cannot be
        // farmed for repeated rank rewards after the outlaw respawns.
        outlawProfile.clearOutlawPenalty();
        ladder.adminPersistProfile(outlawProfile);

        if (newTier == oldTier) {
            hunter.sendMessage(
                    ChatColor.YELLOW +
                            "Bounty claimed, but you could not advance because your skill has no higher seat."
            );
            Bukkit.broadcastMessage(
                    ChatColor.GOLD +
                            "✦ " +
                            ChatColor.BOLD +
                            "BOUNTY CLAIMED " +
                            ChatColor.YELLOW +
                            hunter.getName() +
                            ChatColor.GRAY +
                            " eliminated outlaw " +
                            ChatColor.RED +
                            outlaw.getName() +
                            ChatColor.GRAY +
                            "."
            );
            return;
        }

        hunter.sendMessage(
                ChatColor.GREEN +
                        "Bounty claimed: " +
                        ChatColor.WHITE +
                        oldTier.getDisplayName() +
                        " → " +
                        newTier.getDisplayName() +
                        ChatColor.GRAY +
                        " in " +
                        (hunterSkill == null ? "your skill" : hunterSkill.getDisplayName()) +
                        "."
        );

        Bukkit.broadcastMessage(
                ChatColor.GOLD +
                        "✦ " +
                        ChatColor.BOLD +
                        "BOUNTY CLAIMED " +
                        ChatColor.YELLOW +
                        hunter.getName() +
                        ChatColor.GRAY +
                        " eliminated outlaw " +
                        ChatColor.RED +
                        outlaw.getName() +
                        ChatColor.GRAY +
                        " and earned " +
                        ChatColor.GREEN +
                        "+1 rank " +
                        ChatColor.GRAY +
                        "(" + newTier.getDisplayName() + ")."
        );

        if (bumped != null) {
            Player displaced = Bukkit.getPlayer(bumped);
            if (displaced != null) {
                displaced.sendMessage(
                        ChatColor.YELLOW +
                                "Your rank changed because the bounty reward filled your seat."
                );
            }
        }
    }

    private void applyOutlawEffects(
            Player player,
            PlayerRankProfile profile) {

        player.addPotionEffect(
                new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.WEAKNESS,
                        20 * 60 * 3,
                        Math.max(
                                0,
                                profile.getOutlawLevel() - 1
                        ),
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS,
                        20 * 60 * 3,
                        Math.max(
                                0,
                                profile.getOutlawLevel() - 1
                        ),
                        false,
                        true,
                        true
                )
        );
    }

    private void voidMatch(Player a, Player b, String reason) {
        a.sendTitle(
                ChatColor.GRAY + "MATCH VOID",
                ChatColor.DARK_GRAY + reason,
                10, 70, 10
        );
        b.sendTitle(
                ChatColor.GRAY + "MATCH VOID",
                ChatColor.DARK_GRAY + reason,
                10, 70, 10
        );

        a.sendMessage(ChatColor.GRAY + "Duel void: " + reason);
        b.sendMessage(ChatColor.GRAY + "Duel void: " + reason);
    }

    private void announceResult(
            Player winner,
            Player loser,
            DuelResult result,
            Skill winnerSkill,
            Skill loserSkill) {

        String winnerLine =
                ChatColor.GREEN +
                        result.getWinnerOldTier().getDisplayName() +
                        ChatColor.WHITE +
                        " → " +
                        ChatColor.GREEN +
                        result.getWinnerNewTier().getDisplayName();

        String loserLine =
                ChatColor.RED +
                        result.getLoserOldTier().getDisplayName() +
                        ChatColor.WHITE +
                        " → " +
                        ChatColor.RED +
                        result.getLoserNewTier().getDisplayName();

        winner.sendTitle(
                ChatColor.GOLD + "" + ChatColor.BOLD + "VICTORY",
                winnerLine,
                10, 70, 10
        );

        loser.sendTitle(
                ChatColor.RED + "" + ChatColor.BOLD + "DEFEAT",
                loserLine,
                10, 70, 10
        );

        winner.sendMessage(
                ChatColor.GREEN + "Ranked duel won using " +
                        winnerSkill.getDisplayName() + ". " +
                        winnerLine
        );

        loser.sendMessage(
                ChatColor.RED + "Ranked duel lost using " +
                        loserSkill.getDisplayName() + ". " +
                        loserLine
        );

        for (UUID bumped : result.getBumpedPlayers()) {
            Player player = Bukkit.getPlayer(bumped);
            if (player != null) {
                player.sendMessage(
                        ChatColor.YELLOW +
                                "Your rank changed because the ladder was rebalanced."
                );
            }
        }

        ladder.refreshOnlineDisplay(winner.getUniqueId());
        ladder.refreshOnlineDisplay(loser.getUniqueId());

        for (UUID bumped : result.getBumpedPlayers()) {
            ladder.refreshOnlineDisplay(bumped);
        }
    }
}
