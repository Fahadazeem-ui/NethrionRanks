package com.nethrion.ranks.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class WeaponUtil {

    public static final NamespacedKey WEAPON_TYPE_KEY =
            new NamespacedKey(
                    "nethrionranks",
                    "weapon_type"
            );

    public static final NamespacedKey WEAPON_TIER_KEY =
            new NamespacedKey(
                    "nethrionranks",
                    "weapon_tier"
            );

    public static final NamespacedKey NATIONAL_WEAPON_KEY =
            new NamespacedKey(
                    "nethrionranks",
                    "national_weapon"
            );

    public static final NamespacedKey ADMIN_WEAPON_KEY =
            new NamespacedKey(
                    "nethrionranks",
                    "admin_weapon"
            );

    /**
     * Distinguishes the two physical pieces a SPEARMACE rank holds
     * ("SPEAR" or "MACE"). Null/absent for every other skill, which
     * only ever has a single physical weapon.
     */
    public static final NamespacedKey WEAPON_ROLE_KEY =
            new NamespacedKey(
                    "nethrionranks",
                    "weapon_role"
            );

    /**
     * Custom "Lunge" level. There is no real registered Minecraft
     * enchantment called Lunge, so it is represented as a tagged
     * level plus a lore line, and SpearListener reads this tag to
     * apply an actual forward-dash effect on hit.
     */
    public static final NamespacedKey LUNGE_KEY =
            new NamespacedKey(
                    "nethrionranks",
                    "lunge_level"
            );

    public static final String ROLE_SPEAR = "SPEAR";
    public static final String ROLE_MACE = "MACE";

    private WeaponUtil() {
    }

    public static Skill fromItemStack(ItemStack item) {
        if (
                item == null ||
                        item.getType() == Material.AIR
        ) {
            return null;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {
            String tag =
                    meta.getPersistentDataContainer().get(
                            WEAPON_TYPE_KEY,
                            PersistentDataType.STRING
                    );

            if (tag != null) {
                try {
                    return Skill.valueOf(tag);
                } catch (IllegalArgumentException ignored) {
                    // Fall through to display/material detection.
                }
            }

            if (isNamedSpear(item)) {
                return Skill.SPEARMACE;
            }
        }

        return fromMaterial(item.getType());
    }

    public static Skill fromMaterial(
            Material material) {

        if (material == null) {
            return null;
        }

        String name =
                material.name();

        if (name.endsWith("_SWORD")) {
            return Skill.SWORD;
        }

        if (name.endsWith("_AXE")) {
            return Skill.AXE;
        }

        if (material == Material.MACE) {
            return Skill.MACE;
        }

        if (
                material == Material.BOW ||
                        material == Material.CROSSBOW
        ) {
            return Skill.BOW;
        }

        return null;
    }

    public static boolean isSpear(
            ItemStack item) {

        return fromItemStack(item) ==
                Skill.SPEARMACE &&
                !ROLE_MACE.equals(getRole(item));
    }

    public static boolean isNamedSpear(
            ItemStack item) {

        if (
                item == null ||
                        item.getType() != Material.IRON_HOE
        ) {
            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        String display =
                meta.getDisplayName();

        String tag =
                meta.getPersistentDataContainer().get(
                        WEAPON_TYPE_KEY,
                        PersistentDataType.STRING
                );

        return (
                tag != null &&
                        "SPEARMACE".equalsIgnoreCase(
                                tag
                        )
                ) ||
                (
                        display != null &&
                                ChatColor.stripColor(display)
                                        .toLowerCase()
                                        .contains("spear")
                );
    }

    public static boolean isNationalWeapon(
            ItemStack item) {

        if (item == null) return false;

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) return false;

        Byte value =
                meta.getPersistentDataContainer().get(
                        NATIONAL_WEAPON_KEY,
                        PersistentDataType.BYTE
                );

        return value != null && value == (byte) 1;
    }

    public static boolean isLockedWeapon(
            ItemStack item) {

        return isNationalWeapon(item);
    }

    public static String getRole(
            ItemStack item) {

        if (item == null) return null;

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(
                WEAPON_ROLE_KEY,
                PersistentDataType.STRING
        );
    }

    public static int getLungeLevel(
            ItemStack item) {

        if (item == null) return 0;

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) return 0;

        Integer level =
                meta.getPersistentDataContainer().get(
                        LUNGE_KEY,
                        PersistentDataType.INTEGER
                );

        return level == null ? 0 : level;
    }

    public static Skill getTaggedSkill(
            ItemStack item) {

        if (!isNationalWeapon(item)) {
            return null;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) return null;

        String value =
                meta.getPersistentDataContainer().get(
                        WEAPON_TYPE_KEY,
                        PersistentDataType.STRING
                );

        if (value == null) return null;

        try {
            return Skill.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static RankTier getTaggedTier(
            ItemStack item) {

        if (!isNationalWeapon(item)) {
            return null;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) return null;

        String value =
                meta.getPersistentDataContainer().get(
                        WEAPON_TIER_KEY,
                        PersistentDataType.STRING
                );

        if (value == null) return null;

        try {
            return RankTier.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static ItemStack createSpear() {
        return createRankWeapon(
                Skill.SPEARMACE,
                RankTier.NATIONAL,
                false
        );
    }

    public static ItemStack createRankWeapon(
            Skill skill,
            RankTier tier) {

        return createRankWeapon(
                skill,
                tier,
                tier == RankTier.NATIONAL
        );
    }

    /**
     * Builds the single physical weapon for every skill except
     * SPEARMACE (which is represented by two pieces, see
     * {@link #createRankWeaponSet}). For SPEARMACE this returns
     * only the Spear piece, kept for backward-compatible callers.
     */
    public static ItemStack createRankWeapon(
            Skill skill,
            RankTier tier,
            boolean locked) {

        if (skill == null) {
            skill = Skill.SWORD;
        }

        if (tier == null) {
            tier = RankTier.E;
        }

        if (skill == Skill.SPEARMACE) {
            return createSpearPiece(tier, locked);
        }

        Material material =
                switch (skill) {
                    case SWORD -> Material.NETHERITE_SWORD;
                    case AXE -> Material.NETHERITE_AXE;
                    case MACE -> Material.MACE;
                    case SPEARMACE -> Material.IRON_HOE;
                    case BOW -> Material.BOW;
                };

        ItemStack item =
                buildWeaponPiece(
                        material,
                        skill,
                        tier,
                        locked,
                        null,
                        skill.getMasterTitle()
                );

        applyEnchantments(
                item,
                skill,
                null,
                tier
        );

        return item;
    }

    /**
     * Builds the full physical loadout for a skill/tier combo.
     * Every skill returns exactly one weapon, except SPEARMACE,
     * which returns two independent pieces (a Spear and a Mace),
     * each individually national-locked and enchanted.
     */
    public static List<ItemStack> createRankWeaponSet(
            Skill skill,
            RankTier tier) {

        return createRankWeaponSet(
                skill,
                tier,
                tier == RankTier.NATIONAL
        );
    }

    public static List<ItemStack> createRankWeaponSet(
            Skill skill,
            RankTier tier,
            boolean locked) {

        List<ItemStack> items =
                new ArrayList<>();

        if (skill == Skill.SPEARMACE) {
            items.add(
                    createSpearPiece(
                            tier,
                            locked
                    )
            );
            items.add(
                    createMacePiece(
                            tier,
                            locked
                    )
            );
            return items;
        }

        items.add(
                createRankWeapon(
                        skill,
                        tier,
                        locked
                )
        );

        return items;
    }

    private static ItemStack createSpearPiece(
            RankTier tier,
            boolean locked) {

        ItemStack item =
                buildWeaponPiece(
                        Material.IRON_HOE,
                        Skill.SPEARMACE,
                        tier,
                        locked,
                        ROLE_SPEAR,
                        "SpearMaster"
                );

        applyEnchantments(
                item,
                Skill.SPEARMACE,
                ROLE_SPEAR,
                tier
        );

        return item;
    }

    private static ItemStack createMacePiece(
            RankTier tier,
            boolean locked) {

        ItemStack item =
                buildWeaponPiece(
                        Material.MACE,
                        Skill.SPEARMACE,
                        tier,
                        locked,
                        ROLE_MACE,
                        "MaceMaster"
                );

        applyEnchantments(
                item,
                Skill.SPEARMACE,
                ROLE_MACE,
                tier
        );

        return item;
    }

    private static ItemStack buildWeaponPiece(
            Material material,
            Skill skill,
            RankTier tier,
            boolean locked,
            String role,
            String titleSuffix) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        String title =
                tier.getDisplayName() +
                        " " +
                        titleSuffix;

        ChatColor color =
                weaponNameColor(skill, role);

        meta.displayName(
                weaponNameGradient(skill, role, title)
        );

        List<String> lore =
                new ArrayList<>();

        lore.add(
                ChatColor.DARK_GRAY +
                        "Nethrion Combat Weapon"
        );

        lore.add(
                ChatColor.GRAY +
                        "Skill: " +
                        ChatColor.WHITE +
                        skill.getDisplayName()
        );

        lore.add(
                ChatColor.GRAY +
                        "Tier: " +
                        color +
                        tier.getDisplayName()
        );

        if (locked) {
            lore.add(
                    ChatColor.RED +
                            "National weapon — locked"
            );
        }

        meta.setLore(lore);

        PersistentDataContainer pdc =
                meta.getPersistentDataContainer();

        pdc.set(
                WEAPON_TYPE_KEY,
                PersistentDataType.STRING,
                skill.name()
        );

        pdc.set(
                WEAPON_TIER_KEY,
                PersistentDataType.STRING,
                tier.name()
        );

        if (role != null) {
            pdc.set(
                    WEAPON_ROLE_KEY,
                    PersistentDataType.STRING,
                    role
            );
        }

        if (locked) {
            pdc.set(
                    NATIONAL_WEAPON_KEY,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        }

        item.setItemMeta(meta);
        return item;
    }

    private static ChatColor weaponNameColor(
            Skill skill,
            String role) {

        if (skill == Skill.SPEARMACE && ROLE_MACE.equals(role)) {
            return ChatColor.GOLD;
        }

        if (skill == Skill.SPEARMACE && ROLE_SPEAR.equals(role)) {
            return ChatColor.AQUA;
        }

        return switch (skill) {
            case SWORD -> ChatColor.AQUA;
            case AXE -> ChatColor.RED;
            case MACE -> ChatColor.LIGHT_PURPLE;
            case BOW -> ChatColor.GREEN;
            case SPEARMACE -> ChatColor.AQUA;
        };
    }

    /**
     * Modern Adventure-component name styling:
     * bold, clean block-like Minecraft text with a smooth two-color gradient.
     * This changes only the visual item name; it does not change the item's
     * material, lore, enchantments, tags, rank logic or permissions.
     */
    private static Component weaponNameGradient(
            Skill skill,
            String role,
            String title) {

        TextColor start;
        TextColor end;

        if (skill == Skill.SPEARMACE && ROLE_MACE.equals(role)) {
            start = TextColor.color(255, 214, 76);
            end = TextColor.color(255, 116, 0);
        } else if (skill == Skill.SPEARMACE && ROLE_SPEAR.equals(role)) {
            start = TextColor.color(74, 238, 255);
            end = TextColor.color(35, 126, 255);
        } else {
            switch (skill) {
                case SWORD -> {
                    start = TextColor.color(70, 238, 255);
                    end = TextColor.color(55, 92, 255);
                }
                case AXE -> {
                    start = TextColor.color(255, 102, 102);
                    end = TextColor.color(170, 45, 255);
                }
                case MACE -> {
                    start = TextColor.color(255, 125, 244);
                    end = TextColor.color(122, 85, 255);
                }
                case BOW -> {
                    start = TextColor.color(112, 255, 164);
                    end = TextColor.color(52, 153, 255);
                }
                case SPEARMACE -> {
                    start = TextColor.color(74, 238, 255);
                    end = TextColor.color(35, 126, 255);
                }
                default -> {
                    start = TextColor.color(255, 255, 255);
                    end = TextColor.color(180, 180, 180);
                }
            }
        }

        int length = Math.max(1, title.length());
        Component.Builder builder = Component.text();

        for (int i = 0; i < length; i++) {
            char ch = title.charAt(i);
            double t = length == 1 ? 0.0 : (double) i / (double) (length - 1);

            int r = (int) Math.round(start.red() + (end.red() - start.red()) * t);
            int g = (int) Math.round(start.green() + (end.green() - start.green()) * t);
            int b = (int) Math.round(start.blue() + (end.blue() - start.blue()) * t);

            builder.append(
                    Component.text(ch)
                            .color(TextColor.color(r, g, b))
                            .decorate(TextDecoration.BOLD)
            );
        }

        return builder.build();
    }

    private static void applyEnchantments(
            ItemStack item,
            Skill skill,
            String role,
            RankTier tier) {

        if (tier == RankTier.NATIONAL) {
            applyNationalEnchantments(
                    item,
                    skill,
                    role
            );
            return;
        }

        applyScaledEnchantments(
                item,
                skill,
                role,
                tier
        );
    }

    /**
     * Fixed enchantment table for every NATIONAL-tier weapon, as
     * specified by the rank design (not scaled by tier).
     */
    private static void applyNationalEnchantments(
            ItemStack item,
            Skill skill,
            String role) {

        if (skill == Skill.SPEARMACE && ROLE_MACE.equals(role)) {
            add(item, Enchantment.DENSITY, 6);
            add(item, Enchantment.BREACH, 4);
            add(item, Enchantment.WIND_BURST, 2);
            add(item, Enchantment.UNBREAKING, 4);
            add(item, Enchantment.MENDING, 4);
            return;
        }

        if (skill == Skill.SPEARMACE && ROLE_SPEAR.equals(role)) {
            add(item, Enchantment.FIRE_ASPECT, 2);
            add(item, Enchantment.KNOCKBACK, 3);
            add(item, Enchantment.UNBREAKING, 4);
            add(item, Enchantment.MENDING, 4);
            applyLunge(item, 4);
            return;
        }

        switch (skill) {
            case SWORD -> {
                add(item, Enchantment.SHARPNESS, 6);
                add(item, Enchantment.FIRE_ASPECT, 3);
                add(item, Enchantment.KNOCKBACK, 2);
                add(item, Enchantment.UNBREAKING, 4);
                add(item, Enchantment.MENDING, 4);
                add(item, Enchantment.LOOTING, 4);
            }

            case AXE -> {
                add(item, Enchantment.SHARPNESS, 6);
                add(item, Enchantment.EFFICIENCY, 6);
                add(item, Enchantment.UNBREAKING, 4);
                add(item, Enchantment.MENDING, 4);
            }

            case MACE -> {
                add(item, Enchantment.DENSITY, 6);
                add(item, Enchantment.BREACH, 4);
                add(item, Enchantment.WIND_BURST, 2);
                add(item, Enchantment.UNBREAKING, 4);
                add(item, Enchantment.MENDING, 4);
            }

            case BOW -> {
                add(item, Enchantment.POWER, 6);
                add(item, Enchantment.FLAME, 2);
                add(item, Enchantment.PUNCH, 3);
                add(item, Enchantment.INFINITY, 1);
                add(item, Enchantment.MENDING, 4);
                add(item, Enchantment.UNBREAKING, 4);
            }

            default -> {
                // SPEARMACE handled above via role.
            }
        }
    }

    /**
     * Applies a light "Lunge" tag: a PDC level SpearListener reads
     * to trigger an actual forward-dash on hit, plus a lore line so
     * it visually reads like the item's other enchantments. There is
     * no real registered Minecraft enchantment named Lunge.
     */
    private static void applyLunge(
            ItemStack item,
            int level) {

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) return;

        meta.getPersistentDataContainer().set(
                LUNGE_KEY,
                PersistentDataType.INTEGER,
                level
        );

        List<String> lore =
                meta.hasLore() && meta.getLore() != null
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();

        String numeral =
                switch (level) {
                    case 1 -> "I";
                    case 2 -> "II";
                    case 3 -> "III";
                    case 5 -> "V";
                    case 6 -> "VI";
                    default -> "IV";
                };

        lore.add(
                ChatColor.GRAY +
                        "Lunge " +
                        numeral
        );

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Previous tier-scaled enchantment behaviour, kept for every
     * tier below NATIONAL.
     */
    private static void applyScaledEnchantments(
            ItemStack item,
            Skill skill,
            String role,
            RankTier tier) {

        int scale =
                switch (tier) {
                    case E -> 2;
                    case D -> 3;
                    case C -> 4;
                    case B -> 5;
                    case A, S, NATIONAL -> 6;
                    case CIVILLIAN -> 1;
                };

        if (skill == Skill.SPEARMACE && ROLE_MACE.equals(role)) {
            add(item, Enchantment.DENSITY, scale);
            add(item, Enchantment.UNBREAKING, scale);
            add(item, Enchantment.MENDING, 1);
            add(item, Enchantment.BREACH, Math.min(2, scale));
            return;
        }

        switch (skill) {
            case SWORD -> {
                add(item, Enchantment.SHARPNESS, scale);
                add(item, Enchantment.UNBREAKING, scale);
                add(item, Enchantment.MENDING, 1);
                add(item, Enchantment.FIRE_ASPECT, Math.min(2, scale));
                add(item, Enchantment.KNOCKBACK, Math.min(2, scale));
                add(item, Enchantment.LOOTING, Math.min(3, scale));
            }

            case AXE -> {
                add(item, Enchantment.SHARPNESS, scale);
                add(item, Enchantment.UNBREAKING, scale);
                add(item, Enchantment.MENDING, 1);
                add(item, Enchantment.EFFICIENCY, scale);
                add(item, Enchantment.FIRE_ASPECT, Math.min(2, scale));
                add(item, Enchantment.SMITE, scale);
            }

            case MACE -> {
                add(item, Enchantment.DENSITY, scale);
                add(item, Enchantment.UNBREAKING, scale);
                add(item, Enchantment.MENDING, 1);
                add(item, Enchantment.BREACH, Math.min(2, scale));
            }

            case SPEARMACE -> {
                // Spear piece at sub-National tiers.
                add(item, Enchantment.UNBREAKING, scale);
                add(item, Enchantment.MENDING, 1);
                add(item, Enchantment.FIRE_ASPECT, Math.min(2, scale));
                add(item, Enchantment.KNOCKBACK, Math.min(2, scale));
                add(item, Enchantment.SWEEPING_EDGE, Math.min(3, scale));
            }

            case BOW -> {
                add(item, Enchantment.POWER, scale);
                add(item, Enchantment.UNBREAKING, scale);
                add(item, Enchantment.MENDING, 1);
                add(item, Enchantment.FLAME, 1);
                add(item, Enchantment.PUNCH, Math.min(2, scale));
                add(item, Enchantment.INFINITY, 1);
            }
        }
    }

    private static void add(
            ItemStack item,
            Enchantment enchantment,
            int level) {

        if (enchantment == null) return;

        item.addUnsafeEnchantment(
                enchantment,
                Math.max(1, level)
        );
    }

    public static String describe(
            Skill skill) {

        return skill == null
                ? "Unknown"
                : skill.getDisplayName();
    }

    public static long getMinDurationMillis(
            Skill skill) {

        if (skill == null) {
            return 60_000L;
        }

        return switch (skill) {
            case SWORD -> 60_000L;
            case AXE -> 90_000L;
            case SPEARMACE -> 120_000L;
            case MACE -> 150_000L;
            case BOW -> 180_000L;
        };
    }

    private static ChatColor tierColor(
            RankTier tier) {

        return switch (tier) {
            case NATIONAL -> ChatColor.GOLD;
            case S -> ChatColor.DARK_RED;
            case A -> ChatColor.RED;
            case B -> ChatColor.DARK_PURPLE;
            case C -> ChatColor.LIGHT_PURPLE;
            case D -> ChatColor.BLUE;
            case E -> ChatColor.AQUA;
            case CIVILLIAN -> ChatColor.GRAY;
        };
    }
}
