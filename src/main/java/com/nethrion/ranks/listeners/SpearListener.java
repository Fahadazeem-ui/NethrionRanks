package com.nethrion.ranks.listeners;

import com.nethrion.ranks.rank.WeaponUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpearListener implements Listener {

    private static final double REACH = 4.5;
    private static final double DAMAGE = 7.0;
    private static final long SWING_COOLDOWN_MILLIS = 350L;

    private final Set<UUID> customDamage = new HashSet<>();
    private final java.util.Map<UUID, Long> cooldowns = new java.util.HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!WeaponUtil.isSpear(item)) return;

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < SWING_COOLDOWN_MILLIS) return;

        cooldowns.put(player.getUniqueId(), now);

        Location eye = player.getEyeLocation();

        RayTraceResult result =
                player.getWorld().rayTraceEntities(
                        eye,
                        eye.getDirection(),
                        REACH,
                        0.35,
                        entity ->
                                entity instanceof LivingEntity &&
                                        entity.getUniqueId() != player.getUniqueId() &&
                                        !(entity instanceof org.bukkit.entity.ArmorStand)
                );

        if (result == null || result.getHitEntity() == null) return;

        Entity hit = result.getHitEntity();

        customDamage.add(hit.getUniqueId());
        try {
            ((LivingEntity) hit).damage(DAMAGE, player);
        } finally {
            customDamage.remove(hit.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpearVanillaDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        if (!WeaponUtil.isSpear(
                player.getInventory().getItemInMainHand())) {
            return;
        }

        if (customDamage.contains(event.getEntity().getUniqueId())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (WeaponUtil.isSpear(
                event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (WeaponUtil.isSpear(
                event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }
}
