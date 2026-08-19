package com.nethrion.ranks.rank;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerRankProfile {

    private final UUID uuid;
    private Skill skill;
    private RankTier tier = RankTier.CIVILLIAN;
    private int kills;
    private long lastNationalDuelTimestamp;
    private long outlawUntilMillis;
    private int outlawLevel;
    private final Map<Skill, Long> skillXp = new EnumMap<>(Skill.class);

    public PlayerRankProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public boolean hasSkill() { return skill != null; }

    public RankTier getTier() { return tier; }
    public void setTier(RankTier tier) { this.tier = tier == null ? RankTier.CIVILLIAN : tier; }

    public int getKills() { return kills; }
    public void addKill() { kills++; }
    public void setKills(int kills) { this.kills = Math.max(0, kills); }

    public long getLastNationalDuelTimestamp() { return lastNationalDuelTimestamp; }
    public void setLastNationalDuelTimestamp(long timestamp) { this.lastNationalDuelTimestamp = Math.max(0L, timestamp); }

    public int getOutlawLevel() { return outlawLevel; }
    public long getOutlawUntilMillis() { return outlawUntilMillis; }

    public void addOutlawLevel(int amount, long durationMillis) {
        outlawLevel = Math.min(5, Math.max(0, outlawLevel + amount));
        outlawUntilMillis = Math.max(outlawUntilMillis, System.currentTimeMillis() + Math.max(0L, durationMillis));
    }

    public void clearExpiredOutlaw() {
        if (outlawUntilMillis > 0L && System.currentTimeMillis() >= outlawUntilMillis) {
            outlawLevel = 0;
            outlawUntilMillis = 0L;
        }
    }

    public boolean isOutlaw() {
        clearExpiredOutlaw();
        return outlawLevel > 0;
    }

    public void setOutlawState(int level, long untilMillis) {
        outlawLevel = Math.max(0, Math.min(5, level));
        outlawUntilMillis = Math.max(0L, untilMillis);
        clearExpiredOutlaw();
    }

    public long getSkillXp(Skill skill) {
        return skill == null ? 0L : skillXp.getOrDefault(skill, 0L);
    }

    public void setSkillXp(Skill skill, long xp) {
        if (skill != null) {
            skillXp.put(skill, Math.max(0L, xp));
        }
    }

    public void addSkillXp(Skill skill, long amount) {
        if (skill != null && amount > 0L) {
            setSkillXp(skill, getSkillXp(skill) + amount);
        }
    }

    public int getSkillLevel(Skill skill) {
        if (skill == null) return 0;
        long xp = getSkillXp(skill);
        int level = 1;
        long remaining = xp;
        while (level < 100) {
            long required = xpRequiredForLevel(level);
            if (remaining < required) break;
            remaining -= required;
            level++;
        }
        return level;
    }

    public long getSkillLevelProgressXp(Skill skill) {
        if (skill == null) return 0L;
        long remaining = getSkillXp(skill);
        int level = 1;
        while (level < 100) {
            long required = xpRequiredForLevel(level);
            if (remaining < required) return remaining;
            remaining -= required;
            level++;
        }
        return 0L;
    }

    public long getSkillXpToNextLevel(Skill skill) {
        int level = getSkillLevel(skill);
        return level >= 100 ? 0L : xpRequiredForLevel(level);
    }

    private static long xpRequiredForLevel(int level) {
        return 100L + ((long) Math.max(0, level - 1) * 25L);
    }

    public String getDisplayPrefix() {
        if (tier == RankTier.CIVILLIAN || skill == null) {
            return RankTier.CIVILLIAN.getDisplayName();
        }
        return tier.getDisplayName() + " " + skill.getMasterTitle();
    }
}
