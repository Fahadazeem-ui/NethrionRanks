package com.nethrion.ranks.listeners;

import com.nethrion.ranks.managers.BaseManager;
import com.nethrion.ranks.managers.LogEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BaseListener implements Listener {

    private final BaseManager baseManager;

    // Yeh container types "storage caution alert" trigger karte hain
    private static final Material[] STORAGE_BLOCKS = {
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
            Material.SHULKER_BOX
    };

    public BaseListener(BaseManager baseManager) {
        this.baseManager = baseManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        UUID ownerUUID = baseManager.getBaseOwnerAt(loc);
        if (ownerUUID == null) return; // base ke bahar hai, ignore karo

        LogEntry entry = new LogEntry(
                event.getPlayer().getName(),
                "BREAK",
                event.getBlock().getType().name(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
        );
        baseManager.addLog(ownerUUID, entry);
        // Rule 1 ke mutabiq: koi chat message ya broadcast NAHI bhejna
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Location loc = event.getBlock().getLocation();
        UUID ownerUUID = baseManager.getBaseOwnerAt(loc);
        if (ownerUUID == null) return;

        LogEntry entry = new LogEntry(
                event.getPlayer().getName(),
                "PLACE",
                event.getBlock().getType().name(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
        );
        baseManager.addLog(ownerUUID, entry);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Sirf real storage container clicks pe react karna hai (player's own inventory pe nahi)
        if (event.getInventory().getLocation() == null) return;

        Location loc = event.getInventory().getLocation();
        boolean isStorageBlock = false;
        for (Material mat : STORAGE_BLOCKS) {
            if (loc.getBlock().getType() == mat) {
                isStorageBlock = true;
                break;
            }
        }
        if (!isStorageBlock) return;

        UUID ownerUUID = baseManager.getBaseOwnerAt(loc);
        if (ownerUUID == null) return; // base ke bahar wala chest, ignore

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();

        ItemStack item = event.getCurrentItem();
        String itemName = (item != null && item.getType() != Material.AIR)
                ? item.getType().name() : "Unknown Item";

        // Silent log — hamesha record hota hai
        LogEntry entry = new LogEntry(
                clicker.getName(), "CONTAINER", itemName,
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
        );
        baseManager.addLog(ownerUUID, entry);

        // Real-time caution alert — sirf tab jab koi AUR player (owner nahi) base ke storage mein ghuse
        if (!clicker.getUniqueId().equals(ownerUUID)) {
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null && owner.isOnline()) {
                owner.sendActionBar("§c⚠ Caution: §f" + clicker.getName() +
                        " §7took/placed §f" + itemName + "§7!");
            }
        }
    }
}
