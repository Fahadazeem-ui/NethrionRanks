package com.nethrion.ranks.rank;

/**
 * 8-stage ladder: Civillian (unranked) + 7 real tiers.
 * slotsPerSkill = -1 ka matlab hai "unlimited" (sirf Civillian ke liye).
 */
public enum RankTier {
    CIVILLIAN("Civillian", -1),
    E("E Rank", 3),
    D("D Rank", 3),
    C("C Rank", 3),
    B("B Rank", 3),
    A("A Rank", 2),
    S("S Rank", 2),
    NATIONAL("NATIONAL", 1);

    private final String displayName;
    private final int slotsPerSkill;

    RankTier(String displayName, int slotsPerSkill) {
        this.displayName = displayName;
        this.slotsPerSkill = slotsPerSkill;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSlotsPerSkill() {
        return slotsPerSkill;
    }

    // Ladder mein agla tier (agar already NATIONAL hai to wahi rahega)
    public RankTier next() {
        RankTier[] all = values();
        int idx = this.ordinal();
        return (idx >= all.length - 1) ? this : all[idx + 1];
    }

    // Ladder mein pichla tier (agar already CIVILLIAN hai to wahi rahega)
    public RankTier previous() {
        int idx = this.ordinal();
        return (idx <= 0) ? this : values()[idx - 1];
    }

    public boolean isHigherThan(RankTier other) {
        return this.ordinal() > other.ordinal();
    }
}
