package com.nethrion.ranks.rank;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerRankProfile {

    private final UUID uuid;
    private Skill skill;

    /**
     * The second skill a National holder has won off another National in
     * a cross-skill /rankduel (e.g. a SwordMaster who beats an AxeMaster's
     * National in a cross-skill challenge becomes "National SwordAxeMaster").
     * Only ever set while {@code tier == NATIONAL}; a maximum of ONE
     * secondary skill is allowed (a player can hold at most 2 skills total,
     * and only ever gains them via National-vs-National cross-skill wins -
     * see RankLadderManager#resolveDuel). Cleared automatically the moment
     * the player is no longer National (see setTier), because the combined
     * title has no meaning outside National.
     */
    private Skill secondarySkill;

    private RankTier tier = RankTier.CIVILLIAN;
    private int kills;
    private long lastNationalDuelTimestamp;
    private long outlawUntilMillis;
    private int outlawResetCount;
    private int outlawLevel;
    private final Map<Skill, Long> skillXp = new EnumMap<>(Skill.class);

    public PlayerRankProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }

    public Skill getSkill() { return skill; }

    public void setSkill(Skill skill) { this.skill = skill; }

    public boolean hasSkill() { return skill != null; }

    public Skill getSecondarySkill() { return secondarySkill; }

    /**
     * Only meaningful while National. Silently refuses to set a
     * secondary skill equal to the primary skill (that isn't a second
     * skill at all) - callers should already guard against this, but
     * this is the last line of defense against a corrupt double-title.
     */
    public void setSecondarySkill(Skill secondarySkill) {
        if (secondarySkill != null && secondarySkill == this.skill) {
            return;
        }
        this.secondarySkill = secondarySkill;
    }

    public boolean hasSecondarySkill() { return secondarySkill != null; }

    /** True once this player already holds the maximum of 2 skills. */
    public boolean isAtSkillCap() { return secondarySkill != null; }

    public RankTier getTier() { return tier; }

    public void setTier(RankTier tier) {
        this.tier = tier == null ? RankTier.CIVILLIAN : tier;

        // The combined double-title only exists for an active National.
        // The instant a player leaves National (demoted, swapped out,
        // reset), any second skill they'd won is meaningless - clear it
        // so a future re-promotion never resurrects a stale combination.
        if (this.tier != RankTier.NATIONAL) {
            this.secondarySkill = null;
        }
    }

    public int getKills() { return kills; }

    public void addKill() { kills++; }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public long getLastNationalDuelTimestamp() {
        return lastNationalDuelTimestamp;
    }

    public void setLastNationalDuelTimestamp(long timestamp) {
        this.lastNationalDuelTimestamp = Math.max(0L, timestamp);
    }

    public int getOutlawLevel() { return outlawLevel; }

    public long getOutlawUntilMillis() { return outlawUntilMillis; }

    public int getOutlawResetCount() {
        return outlawResetCount;
    }

    public void startOutlawPenalty(long durationMillis) {
        outlawLevel = Math.max(1, Math.min(5, outlawLevel + 1));
        outlawUntilMillis =
                System.currentTimeMillis() +
                        Math.max(0L, durationMillis);
        outlawResetCount = 0;
    }

    public boolean refreshOutlawPenalty(
            long durationMillis,
            int maximumResets) {

        if (!isOutlaw()) {
            return false;
        }

        if (outlawResetCount >= maximumResets) {
            return false;
        }

        outlawResetCount++;
        outlawUntilMillis =
                System.currentTimeMillis() +
                        Math.max(0L, durationMillis);
        return true;
    }

    public void clearExpiredOutlaw() {
        if (
                outlawUntilMillis > 0L &&
                        System.currentTimeMillis() >= outlawUntilMillis
        ) {
            outlawLevel = 0;
            outlawUntilMillis = 0L;
            outlawResetCount = 0;
        }
    }

    public boolean isOutlaw() {
        clearExpiredOutlaw();
        return outlawLevel > 0;
    }

    public void setOutlawState(
            int level,
            long untilMillis,
            int resetCount) {

        outlawLevel =
                Math.max(
                        0,
                        Math.min(5, level)
                );

        outlawUntilMillis =
                Math.max(0L, untilMillis);

        outlawResetCount =
                Math.max(0, Math.min(3, resetCount));

        clearExpiredOutlaw();
    }

    public void clearOutlawPenalty() {
        outlawLevel = 0;
        outlawUntilMillis = 0L;
        outlawResetCount = 0;
    }

    public long getSkillXp(Skill skill) {
        return skill == null
                ? 0L
                : skillXp.getOrDefault(skill, 0L);
    }

    public void setSkillXp(Skill skill, long xp) {
        if (skill != null) {
            skillXp.put(
                    skill,
                    Math.max(0L, xp)
            );
        }
    }

    public void addSkillXp(
            Skill skill,
            long amount) {

        if (
                skill != null &&
                        amount > 0L
        ) {
            setSkillXp(
                    skill,
                    getSkillXp(skill) + amount
            );
        }
    }

    public int getSkillLevel(Skill skill) {
        if (skill == null) return 0;

        long remaining = getSkillXp(skill);
        int level = 1;

        while (level < 100) {
            long required =
                    xpRequiredForLevel(level);

            if (remaining < required) {
                break;
            }

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
            long required =
                    xpRequiredForLevel(level);

            if (remaining < required) {
                return remaining;
            }

            remaining -= required;
            level++;
        }

        return 0L;
    }

    public long getSkillXpToNextLevel(
            Skill skill) {

        int level =
                getSkillLevel(skill);

        return level >= 100
                ? 0L
                : xpRequiredForLevel(level);
    }

    private static long xpRequiredForLevel(
            int level) {

        return 100L +
                ((long) Math.max(0, level - 1) * 25L);
    }

    public String getDisplayPrefix() {
        if (
                tier == RankTier.CIVILLIAN ||
                        skill == null
        ) {
            return RankTier.CIVILLIAN.getDisplayName();
        }

        return tier.getDisplayName() +
                " " +
                getMasterTitle();
    }

    /**
     * The plain master title, e.g. "SwordMaster", or the combined
     * double title for a National who has won a cross-skill challenge,
     * e.g. "SwordAxeMaster" - skills joined in the order they were won
     * (primary first, secondary second), sharing a single trailing
     * "Master" rather than "SwordMasterAxeMaster".
     */
    public String getMasterTitle() {
        if (skill == null) return "";

        if (secondarySkill == null || tier != RankTier.NATIONAL) {
            return skill.getMasterTitle();
        }

        return skill.getDisplayName() +
                secondarySkill.getDisplayName() +
                "Master";
    }
}
