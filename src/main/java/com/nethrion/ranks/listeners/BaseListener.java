package com.nethrion.ranks.listeners;

import com.nethrion.ranks.managers.BaseManager;
import com.nethrion.ranks.managers.BaseManager.BaseDefinition;
import com.nethrion.ranks.managers.LogEntry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BaseListener
        implements Listener {

    private final BaseManager baseManager;

    private static final Material[] STORAGE_BLOCKS = {
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.BARREL,
            Material.SHULKER_BOX
    };

    public BaseListener(
            BaseManager baseManager) {
        this.baseManager = baseManager;
    }

    @EventHandler
    public void onBlockBreak(
            BlockBreakEvent event) {

        Location location =
                event.getBlock()
                        .getLocation();

        BaseDefinition base =
                baseManager.findBaseAt(
                        location
                );

        if (base == null) {
            return;
        }

        baseManager.addLog(
                baseOwner(base),
                base.slot(),
                new LogEntry(
                        event.getPlayer().getName(),
                        "BREAK",
                        event.getBlock()
                                .getType()
                                .name(),
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                )
        );
    }

    @EventHandler
    public void onBlockPlace(
            BlockPlaceEvent event) {

        Location location =
                event.getBlock()
                        .getLocation();

        BaseDefinition base =
                baseManager.findBaseAt(
                        location
                );

        if (base == null) {
            return;
        }

        baseManager.addLog(
                baseOwner(base),
                base.slot(),
                new LogEntry(
                        event.getPlayer().getName(),
                        "PLACE",
                        event.getBlock()
                                .getType()
                                .name(),
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                )
        );
    }

    @EventHandler
    public void onInventoryOpen(
            InventoryOpenEvent event) {

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory =
                event.getInventory();

        Location location =
                inventory.getLocation();

        if (location == null) {
            return;
        }

        if (!isStorageBlock(
                location.getBlock()
                        .getType()
        )) {
            return;
        }

        BaseDefinition base =
                baseManager.findBaseAt(
                        location
                );

        if (base == null) {
            return;
        }

        ItemStack held =
                player.getInventory()
                        .getItemInMainHand();

        String itemName =
                held.getType() ==
                        Material.AIR
                        ? "open"
                        : held.getType().name();

        baseManager.addLog(
                baseOwner(base),
                base.slot(),
                new LogEntry(
                        player.getName(),
                        "OPEN_CONTAINER",
                        itemName,
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                )
        );

        UUID owner =
                baseOwner(base);

        if (
                owner != null &&
                        !owner.equals(
                                player.getUniqueId()
                        )
        ) {
            Player baseOwner =
                    player.getServer()
                            .getPlayer(owner);

            if (
                    baseOwner != null &&
                            baseOwner.isOnline()
            ) {
                baseOwner.sendActionBar(
                        "§c⚠ " +
                                player.getName() +
                                " opened storage in Base " +
                                base.slot()
                );
            }
        }
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inventory =
                event.getView()
                        .getTopInventory();

        Location location =
                inventory.getLocation();

        if (location == null) {
            return;
        }

        if (!isStorageBlock(
                location.getBlock()
                        .getType()
        )) {
            return;
        }

        BaseDefinition base =
                baseManager.findBaseAt(
                        location
                );

        if (base == null) {
            return;
        }

        ItemStack current =
                event.getCurrentItem();

        String itemName =
                current != null &&
                        current.getType() !=
                                Material.AIR
                        ? current.getType().name()
                        : "Unknown";

        baseManager.addLog(
                baseOwner(base),
                base.slot(),
                new LogEntry(
                        player.getName(),
                        "CONTAINER_CLICK",
                        itemName,
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                )
        );
    }

    private boolean isStorageBlock(
            Material material) {

        for (Material storage :
                STORAGE_BLOCKS) {
            if (storage == material) {
                return true;
            }
        }

        return false;
    }

    /*
     * BaseDefinition keeps the owner internally; expose it through
     * the manager so the listener never needs to duplicate storage.
     */
    private UUID baseOwner(
            BaseDefinition base) {
        return base.owner();
    }
}
