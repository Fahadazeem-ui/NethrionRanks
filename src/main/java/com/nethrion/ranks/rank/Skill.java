package com.nethrion.ranks.rank;

/**
 * Har player ki permanent skill — pehli ranked duel mein weapon-usage detect ho kar assign hoti hai.
 */
public enum Skill {
    SWORD("Sword"),
    AXE("Axe"),
    MACE("Mace"),
    SPEARMACE("SpearMace"), // Spear akela nahi — Spear-heavy fights isi bucket mein aati hain
    BOW("Bow");

    private final String displayName;

    Skill(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Nametag/tablist ke liye: "SwordMaster", "AxeMaster", waghera
    public String getMasterTitle() {
        return displayName + "Master";
    }
}
