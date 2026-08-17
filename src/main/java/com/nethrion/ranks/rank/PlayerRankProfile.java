package com.nethrion.ranks.rank;

import java.util.UUID;

/**
 * Ek player ka pura rank-related data — uski skill, tier, kills, aur National
 * activity-tracking timestamp.
 */
public class PlayerRankProfile {

    private final UUID uuid;
    private Skill skill;              // null jab tak pehli ranked duel na ho
    private RankTier tier = RankTier.CIVILLIAN;
    private int kills = 0;
    private long lastNationalDuelTimestamp = 0L; // National ke 3-din wale rule ke liye

    public PlayerRankProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public boolean hasSkill() {
        return skill != null;
    }

    public RankTier getTier() {
        return tier;
    }

    public void setTier(RankTier tier) {
        this.tier = tier;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    // Sirf loading ke waqt config se value restore karne ke liye
    public void setKills(int kills) {
        this.kills = kills;
    }

    public long getLastNationalDuelTimestamp() {
        return lastNationalDuelTimestamp;
    }

    public void setLastNationalDuelTimestamp(long timestamp) {
        this.lastNationalDuelTimestamp = timestamp;
    }

    // Nametag/tablist display ke liye — "Civillian" ya "TIER SkillMaster"
    public String getDisplayPrefix() {
        if (tier == RankTier.CIVILLIAN || skill == null) {
            return RankTier.CIVILLIAN.getDisplayName();
        }
        return tier.getDisplayName() + " " + skill.getMasterTitle();
    }
}
