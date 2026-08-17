package com.nethrion.ranks.rank;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Weapon detection: pehle custom NBT tag check karta hai (jaise Spear —
 * jo dikhta Iron Hoe/Stick jaisa hai lekin tagged "SPEAR" hai), warna
 * normal vanilla Material se skill nikalta hai (sword/axe/mace/bow).
 */
public class WeaponUtil {

    // Yeh key custom weapons (Phase 4) pe lagayi jayegi taake unki asal
    // skill pehchani ja sake, chahe unka Minecraft item type kuch bhi ho.
    public static final NamespacedKey WEAPON_TYPE_KEY = new NamespacedKey("nethrionranks", "weapon_type");

    // ItemStack se skill detect karna — pehle custom tag check, phir vanilla material
    public static Skill fromItemStack(ItemStack item) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String tag = meta.getPersistentDataContainer().get(WEAPON_TYPE_KEY, PersistentDataType.STRING);
            if (tag != null) {
                try {
                    return Skill.valueOf(tag);
                } catch (IllegalArgumentException ignored) {
                    // Ghalat/corrupt tag ho to vanilla detection pe fallback karo
                }
            }
        }

        return fromMaterial(item.getType());
    }

    // Sirf vanilla Minecraft materials ke liye (Spear iska hissa nahi — woh sirf custom-tagged item se aati hai)
    public static Skill fromMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();

        if (name.endsWith("_SWORD")) return Skill.SWORD;
        if (name.endsWith("_AXE")) return Skill.AXE;
        if (material == Material.MACE) return Skill.MACE;
        if (material == Material.BOW || material == Material.CROSSBOW) return Skill.BOW;

        return null; // koi tracked weapon nahi
    }

    public static long getMinDurationMillis(Skill skill) {
        if (skill == null) return 60_000L;
        switch (skill) {
            case SWORD: return 60_000L;      // 1 min
            case AXE: return 90_000L;        // 1.5 min
            case SPEARMACE: return 120_000L; // 2 min
            case MACE: return 150_000L;      // 2.5 min
            case BOW: return 180_000L;       // 3 min
            default: return 60_000L;
        }
    }
}
