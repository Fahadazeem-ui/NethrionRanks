package com.nethrion.ranks.rank;

public enum Skill {
    SWORD("Sword"),
    AXE("Axe"),
    MACE("Mace"),
    SPEARMACE("SpearMace"),
    BOW("Bow");

    private final String displayName;

    Skill(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public String getMasterTitle() { return displayName + "Master"; }
}
