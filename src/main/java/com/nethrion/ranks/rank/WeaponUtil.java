package com.nethrion.ranks.rank;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class WeaponUtil {

    public static final NamespacedKey WEAPON_TYPE_KEY =
            new NamespacedKey("nethrionranks", "weapon_type");

    private WeaponUtil() {}

    public static Skill fromItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String tag = meta.getPersistentDataContainer()
                    .get(WEAPON_TYPE_KEY, PersistentDataType.STRING);
            if (tag != null) {
                try {
                    return Skill.valueOf(tag);
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (isNamedSpear(item)) return Skill.SPEARMACE;
        }

        return fromMaterial(item.getType());
    }

    public static Skill fromMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();

        if (name.endsWith("_SWORD")) return Skill.SWORD;
        if (name.endsWith("_AXE")) return Skill.AXE;
        if (material == Material.MACE) return Skill.MACE;
        if (material == Material.BOW || material == Material.CROSSBOW) return Skill.BOW;

        return null;
    }

    public static boolean isSpear(ItemStack item) {
        return fromItemStack(item) == Skill.SPEARMACE;
    }

    public static boolean isNamedSpear(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_HOE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String display = meta.getDisplayName();
        String tag = meta.getPersistentDataContainer()
                .get(WEAPON_TYPE_KEY, PersistentDataType.STRING);

        return (tag != null && "SPEARMACE".equalsIgnoreCase(tag))
                || (display != null && ChatColor.stripColor(display).equalsIgnoreCase("Spear"));
    }

    public static ItemStack createSpear() {
        ItemStack spear = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = spear.getItemMeta();
        if (meta == null) return spear;

        meta.setDisplayName(ChatColor.WHITE + "Spear");
        meta.getPersistentDataContainer().set(
                WEAPON_TYPE_KEY,
                PersistentDataType.STRING,
                Skill.SPEARMACE.name()
        );
        spear.setItemMeta(meta);
        return spear;
    }

    public static long getMinDurationMillis(Skill skill) {
        if (skill == null) return 60_000L;
        return switch (skill) {
            case SWORD -> 60_000L;
            case AXE -> 90_000L;
            case SPEARMACE -> 120_000L;
            case MACE -> 150_000L;
            case BOW -> 180_000L;
        };
    }

    public static String describe(Skill skill) {
        return skill == null ? "Unknown" : skill.getDisplayName();
    }
}
