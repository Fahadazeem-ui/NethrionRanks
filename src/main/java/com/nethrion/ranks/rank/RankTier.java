package com.nethrion.ranks.rank;

public enum RankTier {
    CIVILLIAN("Civillian", -1),
    E("E", 3),
    D("D", 3),
    C("C", 3),
    B("B", 3),
    A("A", 2),
    S("S", 2),
    NATIONAL("NATIONAL", 1);

    private final String displayName;
    private final int slotsPerSkill;

    RankTier(String displayName, int slotsPerSkill) {
        this.displayName = displayName;
        this.slotsPerSkill = slotsPerSkill;
    }

    public String getDisplayName() { return displayName; }
    public int getSlotsPerSkill() { return slotsPerSkill; }

    public RankTier next() {
        return ordinal() >= values().length - 1 ? this : values()[ordinal() + 1];
    }

    public RankTier previous() {
        return ordinal() <= 0 ? this : values()[ordinal() - 1];
    }

    public boolean isHigherThan(RankTier other) {
        return other != null && ordinal() > other.ordinal();
    }

    public boolean isRanked() {
        return this != CIVILLIAN;
    }
}
