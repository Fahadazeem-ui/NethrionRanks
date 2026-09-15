package com.nethrion.ranks.pvp;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Combat gate for the per-player PvP preference.
 * Both sides must have PvP enabled.
 */
public final class PvPToggleListener implements Listener {
    private final PvPManager manager;
    public PvPToggleListener(PvPManager manager) { this.manager = manager; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolvePlayer(event);
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;

        if (!manager.isEnabled(attacker.getUniqueId()) ||
                !manager.isEnabled(victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private Player resolvePlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }
}
