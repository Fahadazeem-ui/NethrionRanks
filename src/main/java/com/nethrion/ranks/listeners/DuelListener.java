package com.nethrion.ranks.listeners;

import com.nethrion.ranks.rank.DuelManager;
import com.nethrion.ranks.rank.DuelResult;
import com.nethrion.ranks.rank.DuelSession;
import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
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

    private static final long OUTLAW_DURATION_MILLIS = 30L * 60L * 1000L;

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
    public void onQuit(PlayerQuitEvent event) {
        DuelSession session =
                duelManager.getActiveSession(event.getPlayer().getUniqueId());

        if (session == null) return;

        UUID opponent = session.getOpponent(event.getPlayer().getUniqueId());
        Player winner = Bukkit.getPlayer(opponent);

        duelManager.endSession(session);

        if (winner != null) {
            winner.sendTitle(
                    ChatColor.GREEN + "DUEL WON",
                    ChatColor.GRAY + "Opponent disconnected.",
                    10, 50, 10
            );
            winner.sendMessage(
                    ChatColor.GREEN + "Ranked duel won because your opponent disconnected."
            );
        }
    }

    private void applyInnocentKillPenalty(Player killer, Player victim) {
        PlayerRankProfile profile =
                ladder.getProfile(killer.getUniqueId());

        profile.addOutlawLevel(
                1,
                OUTLAW_DURATION_MILLIS
        );

        killer.addPotionEffect(
                new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.WEAKNESS,
                        20 * 60 * 15,
                        Math.max(0, profile.getOutlawLevel() - 1)
                )
        );

        killer.addPotionEffect(
                new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS,
                        20 * 60 * 15,
                        Math.max(0, profile.getOutlawLevel() - 1)
                )
        );

        killer.sendMessage(
                ChatColor.RED + "Innocent killing penalty: " +
                        ChatColor.YELLOW +
                        "Outlaw Level " +
                        profile.getOutlawLevel() +
                        ChatColor.RED +
                        "."
        );

        if (victim != null) {
            Bukkit.broadcastMessage(
                    ChatColor.DARK_RED + "⚠ " +
                            ChatColor.RED +
                            killer.getName() +
                            ChatColor.GRAY +
                            " killed " +
                            ChatColor.RED +
                            victim.getName() +
                            ChatColor.GRAY +
                            " outside a ranked duel."
            );
        }
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
                                "Your slot was occupied by a higher-activity contender. " +
                                "You are now Civillian."
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
