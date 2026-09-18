package com.nethrion.ranks.gui;

import com.nethrion.ranks.Main;
import com.nethrion.ranks.miner.MinerProgressionManager;
import com.nethrion.ranks.pvp.PvPManager;
import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all click events within NethrionRanks GUI inventories.
 * Each action tag maps to a specific screen transition or server action.
 */
public class RankGUIListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (e.getClickedInventory().getType() == InventoryType.PLAYER) return;

        // Only intercept clicks in our titled GUIs (title contains gradient tags)
        String rawTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(e.getView().title());

        // All our GUI titles contain ◈ or ⚔ or ⚙
        if (!rawTitle.contains("NETHRION") && !rawTitle.contains("◈") && !rawTitle.contains("⚔") && !rawTitle.contains("⚙")) {
            return;
        }

        e.setCancelled(true); // Prevent item pickup in all our GUIs

        ItemStack clicked = e.getCurrentItem();
        String action = RankGUI.extractAction(clicked);
        if (action == null) return;

        handleAction(player, action);
    }

    private void handleAction(Player player, String action) {
        RankLadderManager ladder = Main.getInstance().getRankLadderManager();
        PvPManager pvpManager = Main.getInstance().getPvPManager();
        MinerProgressionManager minerManager = Main.getInstance().getMinerManager();

        switch (action) {

            // ── Navigation ──────────────────────────────────
            case "GUI_DASHBOARD" -> RankGUI.openDashboard(player);
            case "GUI_PROFILE"   -> RankGUI.openProfile(player);
            case "GUI_LADDER"    -> RankGUI.openLadder(player);
            case "GUI_SKILLS"    -> RankGUI.openSkillsBoard(player);
            case "GUI_ADMIN"     -> {
                if (player.isOp()) RankGUI.openAdminPanel(player);
                else player.sendMessage(MM.deserialize(
                        "<color:#FF4444>Access denied.</color>"));
            }

            // ── PvP Toggle ─────────────────────────────────
            case "GUI_PVP_TOGGLE" -> {
                boolean current = pvpManager.isEnabled(player.getUniqueId());
                boolean set = pvpManager.setEnabled(player.getUniqueId(), !current);
                if (set) {
                    String state = !current ? "ON" : "OFF";
                    String col   = !current ? "#44FF88" : "#FF4444";
                    player.sendMessage(MM.deserialize("<color:" + col + ">PvP toggled " + state + ".</color>"));
                    // Reopen dashboard to reflect new state
                    RankGUI.openDashboard(player);
                } else {
                    player.sendMessage(MM.deserialize(
                            "<color:#FF4444>Could not toggle PvP — check if you are in an active duel.</color>"));
                }
            }

            // ── Duel (open chat hint) ───────────────────────
            case "GUI_DUEL" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<gradient:#CC44FF:#7700CC><b>Ranked Duel</b></gradient>" +
                        "<color:#AAAAAA> — use </color>" +
                        "<color:#FFFFFF>/rankduel <player></color>" +
                        "<color:#AAAAAA> to challenge someone.</color>"));
            }

            // ── Kill Count ─────────────────────────────────
            case "GUI_KILLS" -> {
                PlayerRankProfile p = ladder.getProfile(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<gradient:#FF4444:#CC0000><b>Duel Kills</b></gradient>" +
                        "<color:#FFFFFF>: " + p.getKills() + " kills</color>"));
                // Show killtop inline
                player.performCommand("killtop");
            }

            // ── Base Audit (open chat hint) ─────────────────
            case "GUI_BASE" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<color:#8899AA>Base Audit: use </color>" +
                        "<color:#FFFFFF>/base list</color>" +
                        "<color:#8899AA> to see your registered bases.</color>"));
            }

            // ── Apply / Resign ─────────────────────────────
            case "GUI_APPLY" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<color:#AAAAAA>Apply commands: </color>" +
                        "<color:#FFFFFF>/apply miner</color>" +
                        "<color:#AAAAAA> or </color>" +
                        "<color:#FFFFFF>/apply civilian</color>"));
            }
            case "GUI_RESIGN" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<color:#FF6644>To resign from National: </color>" +
                        "<color:#FFFFFF>/apply civilian</color>"));
            }

            // ── Miner Progress ─────────────────────────────
            case "GUI_MINER" -> {
                player.closeInventory();
                minerManager.sendProgress(player);
            }

            // ── Admin panel hints ───────────────────────────
            case "ADMIN_SETRANK" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<color:#FFAAAA>Usage: </color><color:#FFFFFF>/rank set <player> <tier> [skill]</color>"));
            }
            case "ADMIN_GIVEWEAPON" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<color:#FFAAAA>Usage: </color><color:#FFFFFF>/rank giveweapon <player> <skill> <tier></color>"));
            }
            case "ADMIN_REMOVEWEAPON" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<color:#FFAAAA>Usage: </color><color:#FFFFFF>/rank removeweapon <player> [skill]</color>"));
            }
            case "ADMIN_REQUESTS" -> {
                player.closeInventory();
                player.performCommand("requests list");
            }
            case "ADMIN_KILLS" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<color:#FFAAAA>Usage: </color><color:#FFFFFF>/kill set <player> <count></color>"));
            }
            case "ADMIN_RESET" -> {
                player.closeInventory();
                player.sendMessage(MM.deserialize(
                        "<color:#FFAAAA>Usage: </color><color:#FFFFFF>/rank remove <player></color>"));
            }

            default -> { /* unknown action — do nothing */ }
        }
    }
}
