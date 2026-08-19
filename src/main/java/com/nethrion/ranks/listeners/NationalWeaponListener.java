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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Locks National weapons to their holder: they can be freely moved
 * around within the holder's own inventory (any slot, offhand
 * included), but can never leave it - no dropping, no placing into
 * a chest/ender chest/shulker/other container, and no dropping on
 * death. A National weapon only ever leaves a player's hands when
 * a ranked duel takes their National rank away, in which case it
 * simply vanishes (see onRespawn) and the new National is granted
 * a brand new copy via RankLadderManager.
 */
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
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack current =
                event.getCurrentItem();

        ItemStack cursor =
                event.getCursor();

        boolean involvesLocked =
                WeaponUtil.isLockedWeapon(current) ||
                        WeaponUtil.isLockedWeapon(cursor);

        if (!involvesLocked) {
            return;
        }

        InventoryView view =
                event.getView();

        if (!isExternalContainerOpen(view)) {
            /*
             * Only the holder's own inventory/crafting screen is
             * open - freely allow rearranging slots, hotbar swaps,
             * armor/offhand placement, all of it.
             */
            return;
        }

        Inventory clicked =
                event.getClickedInventory();

        boolean targetingContainer =
                clicked != null &&
                        clicked.equals(
                                view.getTopInventory()
                        );

        boolean shiftingIntoContainer =
                event.isShiftClick() &&
                        clicked != null &&
                        clicked.equals(
                                view.getBottomInventory()
                        );

        if (targetingContainer || shiftingIntoContainer) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (
                !WeaponUtil.isLockedWeapon(
                        event.getOldCursor()
                )
        ) {
            return;
        }

        InventoryView view =
                event.getView();

        if (!isExternalContainerOpen(view)) {
            return;
        }

        int topSize =
                view.getTopInventory().getSize();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * True once any inventory other than the player's own
     * inventory/crafting screen is open (a chest, ender chest,
     * shulker box, barrel, etc.) - the only situation a National
     * weapon needs to be kept out of.
     */
    private boolean isExternalContainerOpen(
            InventoryView view) {

        InventoryType type =
                view.getTopInventory().getType();

        return type != InventoryType.CRAFTING &&
                type != InventoryType.PLAYER &&
                type != InventoryType.CREATIVE;
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
                        RankTier currentTier =
                                ladder.getTier(uuid);

                        Skill currentSkill =
                                ladder.getSkill(uuid);

                        for (ItemStack item : restore) {
                            /*
                             * A resolved ranked duel loss takes the
                             * National rank (and skill) away before
                             * this ever runs. Only restore the item
                             * if the player is still National in the
                             * exact skill that weapon belongs to -
                             * otherwise it stays gone for good, and
                             * the new National already received a
                             * brand new copy.
                             */
                            Skill itemSkill =
                                    WeaponUtil.getTaggedSkill(item);

                            if (
                                    currentTier == RankTier.NATIONAL &&
                                            itemSkill != null &&
                                            itemSkill == currentSkill
                            ) {
                                player.getInventory().addItem(
                                        item
                                );
                            }
                        }
                    }

                    ladder.refreshOnlineDisplay(uuid);
                }
        );
    }
}
