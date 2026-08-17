package com.nethrion.ranks.rank;

import org.bukkit.Material;

/**
 * Weapon Material -> Skill mapping, aur har skill ka minimum duel-duration.
 * NOTE: Minecraft mein "Spear" naam ka koi item nahi hota — Trident ko
 * Spear ka substitute maana gaya hai (SpearMace skill).
 */
public class WeaponUtil {

    public static Skill fromMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();

        if (name.endsWith("_SWORD")) return Skill.SWORD;
        if (name.endsWith("_AXE")) return Skill.AXE;
        if (material == Material.MACE) return Skill.MACE;
        if (material == Material.TRIDENT) return Skill.SPEARMACE;
        if (material == Material.BOW || material == Material.CROSSBOW) return Skill.BOW;

        return null; // koi tracked weapon nahi (khaali haath, ya koi aur item)
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
