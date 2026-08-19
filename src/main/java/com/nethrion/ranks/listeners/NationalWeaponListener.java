package com.nethrion.ranks.listeners;

import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.RankTier;
import com.nethrion.ranks.rank.Skill;
import com.nethrion.ranks.rank.WeaponUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NationalWeaponListener implements Listener {

    private final RankLadderManager ladder;
    private final Map<UUID, List<ItemStack>> restoreAfterDeath =
            new HashMap<>();

    public NationalWeaponListener(
            RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (
                WeaponUtil.isLockedWeapon(
                        event.getItemDrop().getItemStack()
                )
        ) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    "§cNational weapons are locked to you."
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (
                WeaponUtil.isLockedWeapon(
                        event.getMainHandItem()
                ) ||
                WeaponUtil.isLockedWeapon(
                        event.getOffHandItem()
                )
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack current =
                event.getCurrentItem();

        ItemStack cursor =
                event.getCursor();

        if (
                WeaponUtil.isLockedWeapon(current) ||
                        WeaponUtil.isLockedWeapon(cursor)
        ) {
            /*
             * A locked National weapon may be used from its existing
             * inventory slot, but it cannot be picked up, moved,
             * shifted, swapped or placed into another inventory.
             */
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (
                WeaponUtil.isLockedWeapon(
                        event.getOldCursor()
                )
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player =
                event.getEntity();

        List<ItemStack> locked =
                new ArrayList<>();

        for (ItemStack item :
                event.getDrops()) {

            if (
                    WeaponUtil.isLockedWeapon(item)
            ) {
                locked.add(
                        item.clone()
                );
            }
        }

        if (!locked.isEmpty()) {
            event.getDrops().removeIf(
                    WeaponUtil::isLockedWeapon
            );

            restoreAfterDeath.put(
                    player.getUniqueId(),
                    locked
            );
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID uuid =
                event.getPlayer().getUniqueId();

        List<ItemStack> restore =
                restoreAfterDeath.remove(uuid);

        Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager()
                        .getPlugin("NethrionRanks"),
                () -> {

                    Player player =
                            event.getPlayer();

                    if (
                            restore != null &&
                                    !restore.isEmpty()
                    ) {
                        for (ItemStack item : restore) {
                            player.getInventory().addItem(
                                    item
                            );
                        }
                    }

                    ladder.refreshOnlineDisplay(uuid);
                }
        );
    }
}
