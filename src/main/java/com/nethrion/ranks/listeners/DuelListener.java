package com.nethrion.ranks.listeners;

import com.nethrion.ranks.rank.DuelManager;
import com.nethrion.ranks.rank.DuelResult;
import com.nethrion.ranks.rank.DuelSession;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.Skill;
import com.nethrion.ranks.rank.WeaponUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class DuelListener implements Listener {

    private final DuelManager duelManager;
    private final RankLadderManager rankLadderManager;

    public DuelListener(DuelManager duelManager, RankLadderManager rankLadderManager) {
        this.duelManager = duelManager;
        this.rankLadderManager = rankLadderManager;
    }

    // ---------- Live weapon-usage tracking ----------

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        DuelSession session = duelManager.getActiveSession(attacker.getUniqueId());
        if (session == null || !session.involves(victim.getUniqueId())) return; // yeh fight kisi duel ka hissa nahi

        Skill usedSkill = resolveSkillUsed(event, attacker);
        session.recordDamage(attacker.getUniqueId(), usedSkill, event.getFinalDamage());
    }

    // Projectile (arrow/trident) ho ya direct hit, asal attacker player nikalna
    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) return (Player) event.getDamager();
        if (event.getDamager() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    // Kaunsi skill use hui — projectile ho to Bow, warna haath mein pakri item se
    // (Spear yahan automatically detect ho jayegi, kyunki fromItemStack pehle
    // custom NBT tag check karta hai, chahe item dikhta Iron Hoe/Stick jaisa ho)
    private Skill resolveSkillUsed(EntityDamageByEntityEvent event, Player attacker) {
        if (event.getDamager() instanceof Arrow || event.getDamager() instanceof SpectralArrow) return Skill.BOW;
        if (event.getDamager() instanceof Player) {
            return WeaponUtil.fromItemStack(attacker.getInventory().getItemInMainHand());
        }
        return null;
    }

    // ---------- Match resolution on death ----------

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        DuelSession session = duelManager.getActiveSession(killer.getUniqueId());
        if (session == null || !session.involves(victim.getUniqueId())) {
            // Yeh formal /rankduel wali fight nahi thi.
            // Innocent-Kill debuff Phase 5 mein yahan add hoga.
            return;
        }

        duelManager.endSession(session);

        Skill winnerDominant = session.getDominantSkill(killer.getUniqueId());
        Skill loserDominant = session.getDominantSkill(victim.getUniqueId());

        if (winnerDominant == null || loserDominant == null) {
            voidMatch(killer, victim, "Weapon usage mein clear majority nahi thi.");
            return;
        }

        long minRequired = Math.min(
                WeaponUtil.getMinDurationMillis(winnerDominant),
                WeaponUtil.getMinDurationMillis(loserDominant)
        );

        if (session.getElapsedMillis() < minRequired) {
            voidMatch(killer, victim, "Fight minimum required time se pehle khatam ho gayi.");
            return;
        }

        // Agar player ki skill pehle se locked hai to wahi use hogi, warna abhi detect hui wali lock ho jayegi
        Skill winnerSkill = rankLadderManager.hasSkill(killer.getUniqueId())
                ? rankLadderManager.getSkill(killer.getUniqueId()) : winnerDominant;
        Skill loserSkill = rankLadderManager.hasSkill(victim.getUniqueId())
                ? rankLadderManager.getSkill(victim.getUniqueId()) : loserDominant;

        DuelResult result = rankLadderManager.resolveDuel(
                killer.getUniqueId(), winnerSkill, victim.getUniqueId(), loserSkill);
        rankLadderManager.addKill(killer.getUniqueId());

        announceResult(killer, victim, result);
    }

    private void voidMatch(Player a, Player b, String reason) {
        String title = ChatColor.GRAY + "Match Voided";
        a.sendTitle(title, ChatColor.DARK_GRAY + reason, 10, 60, 10);
        b.sendTitle(title, ChatColor.DARK_GRAY + reason, 10, 60, 10);
        a.sendMessage(ChatColor.GRAY + "Duel void ho gayi: " + reason);
        b.sendMessage(ChatColor.GRAY + "Duel void ho gayi: " + reason);
    }

    private void announceResult(Player winner, Player loser, DuelResult result) {
        String winnerLine = ChatColor.GREEN + "" + result.getWinnerOldTier().getDisplayName() +
                ChatColor.WHITE + " -> " + ChatColor.GREEN + result.getWinnerNewTier().getDisplayName();
        String loserLine = ChatColor.RED + "" + result.getLoserOldTier().getDisplayName() +
                ChatColor.WHITE + " -> " + ChatColor.RED + result.getLoserNewTier().getDisplayName();

        winner.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "Duel Won!", winnerLine, 10, 60, 10);
        loser.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "Duel Lost", loserLine, 10, 60, 10);

        winner.sendMessage(ChatColor.GREEN + "Tumne " + loser.getName() + " ko haraya! " + winnerLine);
        loser.sendMessage(ChatColor.RED + "Tum " + winner.getName() + " se haar gaye. " + loserLine);

        for (UUID bumpedUUID : result.getBumpedPlayers()) {
            Player bumped = Bukkit.getPlayer(bumpedUUID);
            if (bumped != null) {
                bumped.sendMessage(ChatColor.YELLOW + "Tumhari rank-slot kisi zyada active player ko mil gayi (kam kills ki wajah se). Ab tum Civillian ho.");
            }
        }
    }
}
