package com.nethrion.ranks.gui;

import com.nethrion.ranks.Main;
import com.nethrion.ranks.rank.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * NethrionRanks GUI — Bold gradient aesthetic, fully functional,
 * symmetrical, no junk. Every button serves a purpose.
 *
 * Layout philosophy:
 *  - Black glass border (slots 0-8 top row, 45-53 bottom row, edges)
 *  - Center content area clean and breathable
 *  - Each screen is its own method, opened fresh
 */
public class RankGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // ─────────────────────────────────────────
    //  COLOUR TOKENS
    // ─────────────────────────────────────────
    private static final String GOLD_START   = "#FFD700";
    private static final String GOLD_END     = "#FF8C00";
    private static final String AQUA_START   = "#00F5FF";
    private static final String AQUA_END     = "#0080FF";
    private static final String RED_START    = "#FF4444";
    private static final String RED_END      = "#CC0000";
    private static final String GREEN_START  = "#44FF88";
    private static final String GREEN_END    = "#00AA44";
    private static final String PURPLE_START = "#CC44FF";
    private static final String PURPLE_END   = "#7700CC";
    private static final String GREY         = "#888888";
    private static final String WHITE        = "#FFFFFF";
    private static final String DARK         = "#AAAAAA";

    // ─────────────────────────────────────────
    //  MAIN DASHBOARD  (54 slots)
    // ─────────────────────────────────────────
    /**
     * The primary screen — shown to any player via /rankgui.
     * Adapts its buttons based on the viewer's rank/skill/state.
     */
    public static void openDashboard(Player viewer) {
        RankLadderManager ladder = Main.getInstance().getRankLadderManager();
        PlayerRankProfile profile = ladder.getProfile(viewer.getUniqueId());

        String title = gradient("  ⚔  NETHRION  RANKS  ⚔  ", GOLD_START, GOLD_END, true);
        Inventory inv = Bukkit.createInventory(null, 54, MM.deserialize(title));

        // Black glass border
        fillBorder(inv, borderGlass());

        // ── Row 2 centre: Player Head + Info ──────────────────────────
        inv.setItem(22, headItem(viewer, profile, ladder));

        // ── Row 3 action buttons (slots 28–34) ────────────────────────
        //  [28] My Profile   [30] Rank Ladder   [32] Skills Board   [34] PvP Toggle
        inv.setItem(28, actionButton(
                Material.BOOK,
                gradient("My Profile", AQUA_START, AQUA_END, true),
                List.of(dim("View your full rank, skill XP"), dim("and combat statistics.")),
                "GUI_PROFILE"
        ));

        inv.setItem(30, actionButton(
                Material.GOLDEN_SWORD,
                gradient("Rank Ladder", GOLD_START, GOLD_END, true),
                List.of(dim("See who holds every"), dim("National rank seat.")),
                "GUI_LADDER"
        ));

        inv.setItem(32, actionButton(
                Material.EXPERIENCE_BOTTLE,
                gradient("Skills Board", GREEN_START, GREEN_END, true),
                List.of(dim("Top players ranked"), dim("by skill mastery.")),
                "GUI_SKILLS"
        ));

        boolean pvpOn = Main.getInstance().getPvPManager().isEnabled(viewer.getUniqueId());
        inv.setItem(34, pvpToggleButton(pvpOn));

        // ── Row 4 context buttons (slots 37–43) ───────────────────────
        //  [37] Ranked Duel   [39] Kill Count   [41] Base Audit   [43] Apply / Resign
        inv.setItem(37, actionButton(
                Material.DIAMOND_SWORD,
                gradient("Ranked Duel", PURPLE_START, PURPLE_END, true),
                List.of(dim("Challenge a player to a"), dim("formal 1v1 rank duel.")),
                "GUI_DUEL"
        ));

        inv.setItem(39, actionButton(
                Material.PLAYER_HEAD,
                gradient("Kill Count", RED_START, RED_END, true),
                List.of(dim("Your duel kills:"),
                        plain(WHITE, "  " + profile.getKills() + " kills")),
                "GUI_KILLS"
        ));

        inv.setItem(41, actionButton(
                Material.CHEST,
                gradient("Base Audit", "#8899AA", "#556677", true),
                List.of(dim("View activity logs"), dim("for your registered bases.")),
                "GUI_BASE"
        ));

        // Apply/Resign button — context-sensitive
        inv.setItem(43, applyButton(profile));

        // Admin row — slot 49 only if OP
        if (viewer.isOp()) {
            inv.setItem(49, actionButton(
                    Material.COMMAND_BLOCK,
                    gradient("Admin Panel", RED_START, RED_END, true),
                    List.of(dim("Manage ranks, weapons,"), dim("and requests.")),
                    "GUI_ADMIN"
            ));
        }

        viewer.openInventory(inv);
    }

    // ─────────────────────────────────────────
    //  PROFILE SCREEN  (54 slots)
    // ─────────────────────────────────────────
    public static void openProfile(Player viewer) {
        RankLadderManager ladder = Main.getInstance().getRankLadderManager();
        PlayerRankProfile p = ladder.getProfile(viewer.getUniqueId());

        String title = gradient("  ◈  " + viewer.getName() + "  ◈  ", AQUA_START, AQUA_END, true);
        Inventory inv = Bukkit.createInventory(null, 54, MM.deserialize(title));
        fillBorder(inv, borderGlass());

        // Head centred top
        inv.setItem(22, headItem(viewer, p, ladder));

        // Tier + Skill summary - row 3
        RankTier tier = p.getTier();
        Skill skill = p.getSkill();

        String tierGradStart = tier == RankTier.NATIONAL ? GOLD_START : GREY;
        String tierGradEnd   = tier == RankTier.NATIONAL ? GOLD_END   : "#666666";

        inv.setItem(28, infoPane(
                Material.NETHER_STAR,
                gradient("Rank Tier", tierGradStart, tierGradEnd, true),
                List.of(
                        plain(WHITE, tier == null ? "Civilian" : tier.getDisplayName()),
                        dim(tier == RankTier.NATIONAL ? "★ National Ranked" : "Unranked")
                )
        ));

        inv.setItem(30, infoPane(
                skillMaterial(skill),
                gradient("Combat Skill", AQUA_START, AQUA_END, true),
                skillLore(p, ladder)
        ));

        inv.setItem(32, infoPane(
                Material.IRON_SWORD,
                gradient("Duel Record", PURPLE_START, PURPLE_END, true),
                List.of(
                        plain(WHITE, p.getKills() + " ranked kills"),
                        dim(p.isOutlaw() ? "⚠ Outlaw Penalty Active" : "Clean record")
                )
        ));

        // Secondary skill if exists
        Skill sec = p.getSecondarySkill();
        if (sec != null) {
            inv.setItem(34, infoPane(
                    skillMaterial(sec),
                    gradient("Secondary Skill", GREEN_START, GREEN_END, true),
                    List.of(
                            plain(WHITE, sec.getDisplayName()),
                            dim("Level " + p.getSkillLevel(sec))
                    )
            ));
        } else {
            inv.setItem(34, glassPane(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Outlaw warning if active
        if (p.isOutlaw()) {
            long remaining = p.getOutlawUntilMillis() - System.currentTimeMillis();
            String timeStr = formatTime(remaining);
            inv.setItem(40, infoPane(
                    Material.REDSTONE,
                    gradient("⚠ Outlaw Status", RED_START, RED_END, true),
                    List.of(
                            plain("#FF6666", "Level " + p.getOutlawLevel()),
                            plain("#FF6666", "Expires in: " + timeStr),
                            dim("Stay out of unsanctioned combat.")
                    )
            ));
        }

        // Back button
        inv.setItem(49, backButton("GUI_DASHBOARD"));

        viewer.openInventory(inv);
    }

    // ─────────────────────────────────────────
    //  RANK LADDER SCREEN  (54 slots)
    // ─────────────────────────────────────────
    public static void openLadder(Player viewer) {
        RankLadderManager ladder = Main.getInstance().getRankLadderManager();

        String title = gradient("  ◈  RANK  LADDER  ◈  ", GOLD_START, GOLD_END, true);
        Inventory inv = Bukkit.createInventory(null, 54, MM.deserialize(title));
        fillBorder(inv, borderGlass());

        // Column headers — slots 10, 19, 28, 37 (left col)
        // Show each skill's National seat holder
        Skill[] skills = Skill.values();
        int[] displaySlots = {10, 12, 14, 16, 28, 30};

        for (int i = 0; i < skills.length && i < displaySlots.length; i++) {
            Skill skill = skills[i];
            PlayerRankProfile occupant = ladder.getNationalOccupant(skill);

            if (occupant != null) {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(occupant.getUuid());
                String name = op.getName() != null ? op.getName() : "Unknown";
                inv.setItem(displaySlots[i], nationalSeatItem(skill, name, occupant.getKills(), true));
            } else {
                inv.setItem(displaySlots[i], nationalSeatItem(skill, "— Vacant —", 0, false));
            }
        }

        // Civilian count summary
        long civilianCount = ladder.getAllProfiles().stream()
                .filter(pr -> pr.getTier() == RankTier.CIVILLIAN || pr.getTier() == null)
                .count();
        inv.setItem(40, infoPane(
                Material.LEATHER_CHESTPLATE,
                gradient("Civilian Pool", GREY, "#666666", true),
                List.of(dim("Players at Civilian tier:"), plain(WHITE, String.valueOf(civilianCount)))
        ));

        // Top killers quick-view — slot 22
        List<PlayerRankProfile> topKillers = ladder.getTopKillers(3);
        if (!topKillers.isEmpty()) {
            List<Component> killerLore = new ArrayList<>();
            killerLore.add(dim("Top Duel Killers:"));
            for (int i = 0; i < topKillers.size(); i++) {
                PlayerRankProfile kp = topKillers.get(i);
                org.bukkit.OfflinePlayer ko = Bukkit.getOfflinePlayer(kp.getUuid());
                String kname = ko.getName() != null ? ko.getName() : "Unknown";
                String medal = i == 0 ? "§6#1 " : i == 1 ? "§7#2 " : "§c#3 ";
                killerLore.add(plain(WHITE, medal + kname + " — " + kp.getKills() + "k"));
            }
            inv.setItem(22, infoPane(Material.GOLDEN_SWORD, gradient("Kill Leaders", GOLD_START, GOLD_END, true), killerLore));
        }

        inv.setItem(49, backButton("GUI_DASHBOARD"));
        viewer.openInventory(inv);
    }

    // ─────────────────────────────────────────
    //  SKILLS BOARD SCREEN  (54 slots)
    // ─────────────────────────────────────────
    public static void openSkillsBoard(Player viewer) {
        RankLadderManager ladder = Main.getInstance().getRankLadderManager();

        String title = gradient("  ◈  SKILLS  BOARD  ◈  ", GREEN_START, GREEN_END, true);
        Inventory inv = Bukkit.createInventory(null, 54, MM.deserialize(title));
        fillBorder(inv, borderGlass());

        Skill[] skills = Skill.values();
        int[] slots = {10, 12, 14, 16, 28, 30};

        for (int i = 0; i < skills.length && i < slots.length; i++) {
            Skill skill = skills[i];
            List<PlayerRankProfile> top = ladder.getTopPlayersForSkill(skill);

            List<Component> lore = new ArrayList<>();
            lore.add(dim("Top Masters:"));
            if (top.isEmpty()) {
                lore.add(plain(GREY, "No data yet."));
            } else {
                for (int j = 0; j < Math.min(top.size(), 3); j++) {
                    PlayerRankProfile sp = top.get(j);
                    org.bukkit.OfflinePlayer sop = Bukkit.getOfflinePlayer(sp.getUuid());
                    String sname = sop.getName() != null ? sop.getName() : "Unknown";
                    int lvl = sp.getSkillLevel(skill);
                    lore.add(plain(WHITE, (j + 1) + ". " + sname + "  Lv." + lvl));
                }
            }

            String[] gc = skillGradient(skill);
            inv.setItem(slots[i], infoPane(skillMaterial(skill), gradient(skill.getDisplayName() + " Master", gc[0], gc[1], true), lore));
        }

        // My skill quick view
        PlayerRankProfile myP = ladder.getProfile(viewer.getUniqueId());
        Skill mySkill = myP.getSkill();
        if (mySkill != null) {
            int myLvl = myP.getSkillLevel(mySkill);
            long myXp = myP.getSkillXp(mySkill);
            long nextXp = myP.getSkillXpToNextLevel(mySkill);
            inv.setItem(40, infoPane(
                    skillMaterial(mySkill),
                    gradient("Your Progress", AQUA_START, AQUA_END, true),
                    List.of(
                            plain(WHITE, mySkill.getDisplayName() + "  Level " + myLvl),
                            plain(GREEN_START, "XP: " + myXp + " / " + nextXp),
                            dim(progressBar(myXp, nextXp))
                    )
            ));
        }

        inv.setItem(49, backButton("GUI_DASHBOARD"));
        viewer.openInventory(inv);
    }

    // ─────────────────────────────────────────
    //  ADMIN PANEL SCREEN  (54 slots)
    // ─────────────────────────────────────────
    public static void openAdminPanel(Player viewer) {
        if (!viewer.isOp()) return;

        String title = gradient("  ⚙  ADMIN  PANEL  ⚙  ", RED_START, RED_END, true);
        Inventory inv = Bukkit.createInventory(null, 54, MM.deserialize(title));
        fillBorder(inv, Material.RED_STAINED_GLASS_PANE);

        // Core admin actions - symmetrical 2 x 3 grid centred
        inv.setItem(20, actionButton(
                Material.PLAYER_HEAD,
                gradient("Set Player Rank", RED_START, RED_END, true),
                List.of(dim("Use: /rank set <player> <tier> [skill]")),
                "ADMIN_SETRANK"
        ));
        inv.setItem(22, actionButton(
                Material.DIAMOND_SWORD,
                gradient("Give Weapon", GOLD_START, GOLD_END, true),
                List.of(dim("Use: /rank giveweapon <player> <skill> <tier>")),
                "ADMIN_GIVEWEAPON"
        ));
        inv.setItem(24, actionButton(
                Material.BARRIER,
                gradient("Remove Weapon", "#FF4444", "#CC0000", true),
                List.of(dim("Use: /rank removeweapon <player> [skill]")),
                "ADMIN_REMOVEWEAPON"
        ));
        inv.setItem(29, actionButton(
                Material.PAPER,
                gradient("Pending Requests", PURPLE_START, PURPLE_END, true),
                List.of(dim("Use: /requests list")),
                "ADMIN_REQUESTS"
        ));
        inv.setItem(31, actionButton(
                Material.EXPERIENCE_BOTTLE,
                gradient("Kill Admin", "#FFAA00", "#FF6600", true),
                List.of(dim("Use: /kill set <player> <count>")),
                "ADMIN_KILLS"
        ));
        inv.setItem(33, actionButton(
                Material.REDSTONE_BLOCK,
                gradient("Reset to Civilian", "#FF4444", "#990000", true),
                List.of(dim("Use: /rank remove <player>")),
                "ADMIN_RESET"
        ));

        inv.setItem(49, backButton("GUI_DASHBOARD"));
        viewer.openInventory(inv);
    }

    // ─────────────────────────────────────────
    //  ITEM BUILDERS
    // ─────────────────────────────────────────

    private static ItemStack headItem(Player player, PlayerRankProfile profile, RankLadderManager ladder) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        meta.setOwningPlayer(player);

        RankTier tier = profile.getTier();
        Skill skill = profile.getSkill();
        String tierName = (tier != null) ? tier.getDisplayName() : "Civilian";
        String skillName = (skill != null) ? skill.getDisplayName() : "None";

        String gs = tier == RankTier.NATIONAL ? GOLD_START : GREY;
        String ge = tier == RankTier.NATIONAL ? GOLD_END   : "#666666";

        meta.displayName(MM.deserialize(gradient(player.getName(), gs, ge, true)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<color:" + GREY + ">Rank: </color><color:" + WHITE + ">" + tierName + "</color>"));
        lore.add(MM.deserialize("<color:" + GREY + ">Skill: </color><color:" + WHITE + ">" + skillName + "</color>"));
        lore.add(MM.deserialize("<color:" + GREY + ">Kills: </color><color:" + WHITE + ">" + profile.getKills() + "</color>"));
        if (profile.isOutlaw()) {
            lore.add(Component.empty());
            lore.add(MM.deserialize("<color:#FF4444>⚠ Outlaw Penalty Active</color>"));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack nationalSeatItem(Skill skill, String holderName, int kills, boolean occupied) {
        String[] gc = skillGradient(skill);
        Material mat = occupied ? skillMaterial(skill) : Material.GRAY_STAINED_GLASS_PANE;

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize("<color:" + (occupied ? WHITE : GREY) + ">" + holderName + "</color>"));
        if (occupied) {
            lore.add(MM.deserialize("<color:" + GREY + ">Kills: " + kills + "</color>"));
        }
        lore.add(Component.empty());
        lore.add(MM.deserialize("<color:" + GREY + ">National " + skill.getDisplayName() + " seat</color>"));

        return buildItem(mat, gradient("National " + skill.getDisplayName(), gc[0], gc[1], true), lore, null);
    }

    /** Generic action button with a PDC action tag in lore (invisible to player). */
    private static ItemStack actionButton(Material mat, String displayGradient, List<Component> loreLines, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize(displayGradient));
        List<Component> lore = new ArrayList<>(loreLines);
        lore.add(Component.empty());
        lore.add(MM.deserialize("<color:#333333>" + action + "</color>")); // hidden tag line
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack infoPane(Material mat, String displayGradient, List<Component> loreLines) {
        return buildItem(mat, displayGradient, loreLines, null);
    }

    private static ItemStack pvpToggleButton(boolean pvpEnabled) {
        String grad = pvpEnabled ? GREEN_START + "," + GREEN_END : RED_START + "," + RED_END;
        String label = pvpEnabled ? "PvP  ON" : "PvP  OFF";
        String gradStart = pvpEnabled ? GREEN_START : RED_START;
        String gradEnd   = pvpEnabled ? GREEN_END   : RED_END;
        Material mat = pvpEnabled ? Material.LIME_DYE : Material.RED_DYE;

        return actionButton(mat,
                gradient(label, gradStart, gradEnd, true),
                List.of(dim(pvpEnabled ? "You can fight other players." : "You will not deal or take PvP damage."),
                        dim("Click to toggle.")),
                "GUI_PVP_TOGGLE"
        );
    }

    private static ItemStack applyButton(PlayerRankProfile profile) {
        if (profile.getTier() != RankTier.NATIONAL) {
            return actionButton(Material.PAPER,
                    gradient("Apply / Resign", GREY, "#555555", true),
                    List.of(dim("Apply for Miner rank"), dim("or resign your current rank.")),
                    "GUI_APPLY"
            );
        } else {
            return actionButton(Material.PAPER,
                    gradient("Resign Rank", RED_START, RED_END, true),
                    List.of(dim("Request to step down"), dim("from National to Civilian.")),
                    "GUI_RESIGN"
            );
        }
    }

    private static ItemStack backButton(String target) {
        return actionButton(
                Material.ARROW,
                gradient("← Back", GREY, "#555555", false),
                List.of(dim("Return to previous screen.")),
                target
        );
    }

    private static ItemStack borderGlass() {
        return glassPane(Material.BLACK_STAINED_GLASS_PANE);
    }

    private static ItemStack glassPane(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack buildItem(Material mat, String displayGradient, List<Component> lore, Enchantment glow) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(MM.deserialize(displayGradient));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (glow != null) {
            meta.addEnchant(glow, 1, true);
        }
        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────
    //  LAYOUT HELPERS
    // ─────────────────────────────────────────

    /** Fill outer ring of a 6-row (54-slot) inventory. */
    private static void fillBorder(Inventory inv, ItemStack pane) {
        // Top row 0–8
        for (int i = 0; i <= 8; i++) inv.setItem(i, pane);
        // Bottom row 45–53
        for (int i = 45; i <= 53; i++) inv.setItem(i, pane);
        // Left column
        for (int i = 9; i <= 36; i += 9) inv.setItem(i, pane);
        // Right column
        for (int i = 17; i <= 44; i += 9) inv.setItem(i, pane);
    }

    private static void fillBorder(Inventory inv, Material mat) {
        fillBorder(inv, glassPane(mat));
    }

    // ─────────────────────────────────────────
    //  TEXT HELPERS
    // ─────────────────────────────────────────

    /**
     * Build a MiniMessage gradient tag.
     * @param text  display text
     * @param from  hex start colour e.g. "#FFD700"
     * @param to    hex end colour
     * @param bold  whether to apply bold
     */
    public static String gradient(String text, String from, String to, boolean bold) {
        String inner = "<gradient:" + from + ":" + to + ">" + text + "</gradient>";
        return bold ? "<b>" + inner + "</b>" : inner;
    }

    private static Component dim(String text) {
        return MM.deserialize("<color:" + GREY + ">" + text + "</color>");
    }

    private static Component plain(String color, String text) {
        return MM.deserialize("<color:" + color + ">" + text + "</color>");
    }

    private static List<Component> skillLore(PlayerRankProfile p, RankLadderManager ladder) {
        Skill skill = p.getSkill();
        if (skill == null) return List.of(dim("No skill chosen yet."));

        int level = p.getSkillLevel(skill);
        long xp = p.getSkillXp(skill);
        long nextXp = p.getSkillXpToNextLevel(skill);

        return List.of(
                plain(WHITE, skill.getDisplayName() + "  Level " + level),
                plain(GREEN_START, "XP " + xp + " / " + nextXp),
                dim(progressBar(xp, nextXp)),
                dim(skill.getMasterTitle())
        );
    }

    private static String progressBar(long current, long max) {
        if (max <= 0) return "[▓▓▓▓▓▓▓▓▓▓] MAX";
        int filled = (int) Math.min(10, (current * 10) / max);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) bar.append(i < filled ? "▓" : "░");
        bar.append("]");
        return bar.toString();
    }

    private static String formatTime(long millis) {
        if (millis <= 0) return "Expired";
        long secs = millis / 1000;
        long mins = secs / 60;
        long hrs  = mins / 60;
        if (hrs > 0) return hrs + "h " + (mins % 60) + "m";
        if (mins > 0) return mins + "m " + (secs % 60) + "s";
        return secs + "s";
    }

    // ─────────────────────────────────────────
    //  SKILL → MATERIAL / GRADIENT MAPS
    // ─────────────────────────────────────────

    private static Material skillMaterial(Skill skill) {
        if (skill == null) return Material.BARRIER;
        return switch (skill) {
            case SWORD     -> Material.NETHERITE_SWORD;
            case AXE       -> Material.NETHERITE_AXE;
            case MACE      -> Material.MACE;
            case SPEARMACE -> Material.IRON_HOE;
            case BOW       -> Material.BOW;
        };
    }

    private static String[] skillGradient(Skill skill) {
        if (skill == null) return new String[]{GREY, "#444444"};
        return switch (skill) {
            case SWORD     -> new String[]{"#FFFFFF", "#AAAAAA"};
            case AXE       -> new String[]{"#FF6666", "#CC0000"};
            case MACE      -> new String[]{"#CC44FF", "#7700CC"};
            case SPEARMACE -> new String[]{"#44FFAA", "#008844"};
            case BOW       -> new String[]{"#44CCFF", "#0066CC"};
        };
    }

    // ─────────────────────────────────────────
    //  GUI ACTION TAGS (read by RankGUIListener)
    // ─────────────────────────────────────────
    /** Extract the action tag from the last lore line of a clicked item. */
    public static String extractAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return null;
        // Last visible lore line contains the action tag as plain text
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(lore.get(lore.size() - 1)).trim();
        if (plain.startsWith("GUI_") || plain.startsWith("ADMIN_")) return plain;
        return null;
    }
}
