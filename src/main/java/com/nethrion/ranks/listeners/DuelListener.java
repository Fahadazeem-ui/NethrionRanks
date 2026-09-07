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

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class DuelListener implements Listener {

    private static final long OUTLAW_DURATION_MILLIS =
            3L * 60L * 1000L;

    private static final int MAX_OUTLAW_RESETS = 3;

    private final DuelManager duelManager;
    private final RankLadderManager ladder;

    /**
     * Tracks cumulative damage-per-skill dealt to an outlaw victim by
     * each attacker who has hit them, outside of any tracked
     * DuelSession. This lets a bounty claim be judged on the weapon
     * that did the most work overall (per the "sirf last hit se judge
     * nhi karna, overall damage dekhkar decide karo" rule) instead of
     * whatever weapon happened to land the final blow. Keyed by
     * victim -> attacker -> skill -> total damage. Cleared per-victim
     * once the outlaw dies (claimed or not) so it never grows stale.
     */
    private final Map<UUID, Map<UUID, Map<Skill, Double>>> outlawHuntDamage =
            new java.util.HashMap<>();

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

            recordHuntDamage(
                    victim.getUniqueId(),
                    attacker.getUniqueId(),
                    resolveSkillUsed(event, attacker),
                    event.getFinalDamage()
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

    private void recordHuntDamage(
            UUID victimUUID,
            UUID attackerUUID,
            Skill skill,
            double damage) {

        if (skill == null || damage <= 0.0) {
            // Unrecognized weapon (bare hand, off-list tool, etc.) -
            // deliberately not recorded, so it can never accidentally
            // become the "dominant" skill for a bounty claim.
            return;
        }

        outlawHuntDamage
                .computeIfAbsent(victimUUID, k -> new java.util.HashMap<>())
                .computeIfAbsent(attackerUUID, k -> new EnumMap<>(Skill.class))
                .merge(skill, damage, Double::sum);
    }

    /**
     * The skill that contributed the most total damage from this
     * attacker toward this (outlaw) victim, across the whole hunt -
     * not just the killing blow. Returns null if nothing was recorded
     * (e.g. the outlaw died to fall damage, or every hit landed with
     * an unrecognized weapon).
     */
    private Skill resolveOverallHuntSkill(UUID victimUUID, UUID attackerUUID) {
        Map<UUID, Map<Skill, Double>> byAttacker =
                outlawHuntDamage.get(victimUUID);

        if (byAttacker == null) return null;

        Map<Skill, Double> bySkill = byAttacker.get(attackerUUID);
        if (bySkill == null || bySkill.isEmpty()) return null;

        Skill best = null;
        double bestDamage = -1.0;

        for (Map.Entry<Skill, Double> entry : bySkill.entrySet()) {
            if (entry.getValue() > bestDamage) {
                bestDamage = entry.getValue();
                best = entry.getKey();
            }
        }

        return best;
    }

    private void clearHuntDamage(UUID victimUUID) {
        outlawHuntDamage.remove(victimUUID);
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

        // Look the session up by the LOSER, not the killer. Looking it up by
        // killer.getUniqueId() meant a duel never got closed out when the
        // duelling player died from something other than their opponent
        // (fall damage, void, /kill, lava, a third player interfering) —
        // the session just stayed "active" forever. The loser is always the
        // right anchor: if they were duelling, that duel needs to end no
        // matter what killed them.
        DuelSession session =
                duelManager.getActiveSession(loser.getUniqueId());

        if (session != null && !session.involves(loser.getUniqueId())) {
            // Defensive: should be impossible since getActiveSession already
            // filters on involvement, but never trust it silently.
            session = null;
        }

        if (session != null) {
            boolean killedByOpponent =
                    killer != null &&
                            session.involves(killer.getUniqueId()) &&
                            !killer.getUniqueId().equals(loser.getUniqueId());

            if (!killedByOpponent) {
                // The duel got interrupted by something other than a clean
                // kill from the actual opponent: no killer at all (fall,
                // void, environment, self-inflicted), or a third player
                // stepped in and got the kill instead. Either way the duel
                // itself cannot be scored, so close it out as void and make
                // sure the opponent's rank/state can never get stuck
                // waiting on a match that will never finish.
                duelManager.endSession(session);

                UUID opponentId = session.getOpponent(loser.getUniqueId());
                Player opponent = Bukkit.getPlayer(opponentId);

                if (opponent != null) {
                    voidMatch(
                            opponent,
                            loser,
                            "Duel interrupted before it could be scored (opponent died to something other than you)."
                    );
                } else {
                    loser.sendMessage(
                            ChatColor.GRAY +
                                    "Duel void: match ended before it could be scored."
                    );
                }

                // The duel is handled. If there was a genuine third-party
                // killer, their kill is still a separate event (innocent
                // kill / bounty claim) and must go through the normal path.
                if (killer != null) {
                    PlayerRankProfile victimProfile =
                            ladder.getProfile(loser.getUniqueId());

                    if (victimProfile.isOutlaw()) {
                        handleOutlawBountyClaim(killer, loser);
                    } else {
                        applyInnocentKillPenalty(killer, loser);
                    }
                }
                clearHuntDamage(loser.getUniqueId());
                return;
            }

            // killedByOpponent == true: fall through into the normal
            // ranked-duel resolution below, using this session.
        } else {
            // Loser wasn't in any duel at all.
            if (killer == null) return;

            PlayerRankProfile victimProfile =
                    ladder.getProfile(loser.getUniqueId());

            if (victimProfile.isOutlaw()) {
                handleOutlawBountyClaim(killer, loser);
                clearHuntDamage(loser.getUniqueId());
                return;
            }

            applyInnocentKillPenalty(killer, loser);
            clearHuntDamage(loser.getUniqueId());
            return;
        }

        clearHuntDamage(loser.getUniqueId());
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

        UUID leavingUUID = event.getPlayer().getUniqueId();

        // Prevent a slow leak: drop this player's entry as a hunted
        // victim, and their contributions inside every other victim's
        // attacker map.
        clearHuntDamage(leavingUUID);
        for (Map<UUID, Map<Skill, Double>> byAttacker : outlawHuntDamage.values()) {
            byAttacker.remove(leavingUUID);
        }

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

        // Swap only actually happens if the killer outranked the victim -
        // see the Javadoc on resolveInnocentKillRankSwap for why a
        // lower/equal-rank killer must never gain rank this way.
        boolean swapped = ladder.resolveInnocentKillRankSwap(
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

        if (swapped) {
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
        } else {
            killer.sendMessage(
                    ChatColor.RED +
                            "Innocent kill: " +
                            ChatColor.GRAY +
                            "no rank swap — the victim was not ranked below you."
            );
        }

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

                broadcastBountyTitle(
                        ChatColor.of("#ff5c33") + "" + ChatColor.BOLD + "☠ BOUNTY",
                        ChatColor.of("#ffe08a") + "Maaro " + killer.getName() + " — reward: +1 rank"
                );
            }
        }
    }

    /**
     * Center-screen soft-gradient title popup, used for bounty
     * lifecycle events (lagna / claim hona / naturally lift hona) so
     * nobody has to be watching chat to notice.
     */
    private void broadcastBountyTitle(String title, String subtitle) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendTitle(title, subtitle, 10, 60, 20);
        }
    }

    private void handleOutlawBountyClaim(
            Player hunter,
            Player outlaw) {

        PlayerRankProfile outlawProfile =
                ladder.getProfile(outlaw.getUniqueId());

        if (!outlawProfile.isOutlaw()) {
            // The bounty timer ran out in the instant between the killing
            // blow and this death being processed (both fire the same
            // tick, but the outlaw flag is lazily expired on read, so this
            // window is real). The hunter went for a legitimate bounty
            // claim; punishing them here as a fresh "innocent kill" would
            // immediately slap a brand-new bounty on THEM, and whoever
            // finishes them off next would face the exact same edge case -
            // an endless chain of bounties nobody actually earned. So this
            // path is a no-op: no swap, no outlaw penalty, no bounty. The
            // kill is simply not scored either way.
            hunter.sendMessage(
                    ChatColor.GRAY +
                            "That bounty had already expired the instant you landed the kill — no penalty, no reward."
            );
            return;
        }

        // Judge the claim on the weapon that did the most total damage
        // across the whole hunt, not just whatever landed the final
        // blow — a hunter can't sword-tag someone at 1 HP with a bow
        // and have it count as a Bow claim.
        Skill dominantHuntSkill =
                resolveOverallHuntSkill(
                        outlaw.getUniqueId(),
                        hunter.getUniqueId()
                );

        if (dominantHuntSkill == null) {
            // Nothing usable was recorded (unregistered weapon only,
            // e.g. bare hand, or the kill came from something outside
            // melee/bow damage entirely). The outlaw is still dead and
            // the bounty is still consumed - it just earns no reward.
            outlawProfile.clearOutlawPenalty();
            ladder.forgetOutlaw(outlaw.getUniqueId());
            ladder.adminPersistProfile(outlawProfile);

            hunter.sendMessage(
                    ChatColor.YELLOW +
                            "Bounty claimed, but no reward: the kill wasn't landed with a recognized weapon skill."
            );
            Bukkit.broadcastMessage(
                    ChatColor.GOLD + "✦ " + ChatColor.BOLD + "BOUNTY CLAIMED " +
                            ChatColor.YELLOW + hunter.getName() + ChatColor.GRAY +
                            " eliminated outlaw " + ChatColor.RED + outlaw.getName() +
                            ChatColor.GRAY + "."
            );
            broadcastBountyTitle(
                    ChatColor.of("#8fd3a3") + "" + ChatColor.BOLD + "✦ Bounty Lifted",
                    ChatColor.of("#d9d9d9") + outlaw.getName() + " ki bounty khatam — ab na maara jaye"
            );
            return;
        }

        Skill lockedHunterSkill =
                ladder.getSkill(hunter.getUniqueId());

        if (lockedHunterSkill != null && lockedHunterSkill != dominantHuntSkill) {
            // Hunter already has a locked skill and hunted with a
            // different weapon type - claim is invalid, no reward,
            // but the bounty is still consumed so it can't be
            // re-farmed by someone landing the "real" kill next.
            outlawProfile.clearOutlawPenalty();
            ladder.forgetOutlaw(outlaw.getUniqueId());
            ladder.adminPersistProfile(outlawProfile);

            hunter.sendMessage(
                    ChatColor.YELLOW +
                            "Bounty claimed, but no reward: you hunted with " +
                            dominantHuntSkill.getDisplayName() +
                            ", not your locked skill (" +
                            lockedHunterSkill.getDisplayName() +
                            ")."
            );
            Bukkit.broadcastMessage(
                    ChatColor.GOLD + "✦ " + ChatColor.BOLD + "BOUNTY CLAIMED " +
                            ChatColor.YELLOW + hunter.getName() + ChatColor.GRAY +
                            " eliminated outlaw " + ChatColor.RED + outlaw.getName() +
                            ChatColor.GRAY + "."
            );
            broadcastBountyTitle(
                    ChatColor.of("#8fd3a3") + "" + ChatColor.BOLD + "✦ Bounty Lifted",
                    ChatColor.of("#d9d9d9") + outlaw.getName() + " ki bounty khatam — ab na maara jaye"
            );
            return;
        }

        // A bounty claim must lock the hunter's skill from the overall
        // dominant hunt weapon exactly like a normal ranked duel win
        // does (see onDeath, which calls ladder.assignSkillIfAbsent for
        // the winner). Without this, a hunter who never had a locked
        // skill yet would hit claimOutlawBounty() with skill == null,
        // which bails out with no rank change at all.
        if (lockedHunterSkill == null) {
            ladder.assignSkillIfAbsent(
                    hunter.getUniqueId(),
                    dominantHuntSkill
            );
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
            ladder.forgetOutlaw(outlaw.getUniqueId());
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
            broadcastBountyTitle(
                    ChatColor.of("#8fd3a3") + "" + ChatColor.BOLD + "✦ Bounty Lifted",
                    ChatColor.of("#d9d9d9") + outlaw.getName() + " ki bounty khatam — ab na maara jaye"
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

        broadcastBountyTitle(
                ChatColor.of("#8fd3a3") + "" + ChatColor.BOLD + "✦ Bounty Claimed",
                ChatColor.of("#d9d9d9") + hunter.getName() + " promoted to " + newTier.getDisplayName()
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
