package com.nethrion.ranks.rank;

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
                Skill.SPEARMACE;
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
                                        .equalsIgnoreCase(
                                                "Spear"
                                        )
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

        Material material =
                switch (skill) {
                    case SWORD -> Material.DIAMOND_SWORD;
                    case AXE -> Material.DIAMOND_AXE;
                    case MACE -> Material.MACE;
                    case SPEARMACE -> Material.IRON_HOE;
                    case BOW -> Material.BOW;
                };

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
                        skill.getMasterTitle();

        ChatColor color =
                tierColor(tier);

        meta.setDisplayName(
                color +
                        title
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

        if (locked) {
            pdc.set(
                    NATIONAL_WEAPON_KEY,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        }

        applyBalancedEnchantments(
                item,
                skill,
                tier
        );

        item.setItemMeta(meta);
        return item;
    }

    private static void applyBalancedEnchantments(
            ItemStack item,
            Skill skill,
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
                add(item, Enchantment.SHARPNESS, scale);
                add(item, Enchantment.UNBREAKING, scale);
                add(item, Enchantment.MENDING, 1);
                add(item, Enchantment.FIRE_ASPECT, Math.min(2, scale));
                add(item, Enchantment.KNOCKBACK, Math.min(2, scale));
                add(item, Enchantment.SMITE, scale);
            }

            case SPEARMACE -> {
                add(item, Enchantment.SHARPNESS, scale);
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
