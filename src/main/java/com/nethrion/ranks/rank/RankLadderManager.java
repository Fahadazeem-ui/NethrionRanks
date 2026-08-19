package com.nethrion.ranks.rank;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RankLadderManager {

    public static final long NATIONAL_COOLDOWN_MILLIS =
            3L * 24L * 60L * 60L * 1000L;

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final Map<UUID, PlayerRankProfile> profiles =
            new HashMap<>();

    public RankLadderManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        plugin.saveDefaultConfig();
        loadAllProfiles();
    }

    private void loadAllProfiles() {
        ConfigurationSection section =
                config.getConfigurationSection(
                        "rankladder"
                );

        if (section == null) return;

        for (String uuidString : section.getKeys(false)) {
            try {
                UUID uuid =
                        UUID.fromString(uuidString);

                PlayerRankProfile profile =
                        new PlayerRankProfile(uuid);

                String skillName =
                        section.getString(
                                uuidString + ".skill",
                                "NONE"
                        );

                if (
                        !"NONE".equalsIgnoreCase(
                                skillName
                        )
                ) {
                    profile.setSkill(
                            Skill.valueOf(skillName)
                    );
                }

                profile.setTier(
                        RankTier.valueOf(
                                section.getString(
                                        uuidString + ".tier",
                                        "CIVILLIAN"
                                )
                        )
                );

                profile.setKills(
                        section.getInt(
                                uuidString + ".kills",
                                0
                        )
                );

                profile.setLastNationalDuelTimestamp(
                        section.getLong(
                                uuidString +
                                        ".lastNationalDuel",
                                0L
                        )
                );

                profile.setOutlawState(
                        section.getInt(
                                uuidString +
                                        ".outlaw.level",
                                0
                        ),
                        section.getLong(
                                uuidString +
                                        ".outlaw.until",
                                0L
                        ),
                        section.getInt(
                                uuidString +
                                        ".outlaw.resets",
                                0
                        )
                );

                for (Skill skill : Skill.values()) {
                    profile.setSkillXp(
                            skill,
                            section.getLong(
                                    uuidString +
                                            ".skillXp." +
                                            skill.name(),
                                    0L
                            )
                    );
                }

                profiles.put(uuid, profile);

            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "Skipped invalid rank profile: " +
                                uuidString
                );
            }
        }
    }

    private void persistProfile(
            PlayerRankProfile profile) {

        profile.clearExpiredOutlaw();

        String path =
                "rankladder." +
                        profile.getUuid();

        config.set(
                path + ".skill",
                profile.hasSkill()
                        ? profile.getSkill().name()
                        : "NONE"
        );

        config.set(
                path + ".tier",
                profile.getTier().name()
        );

        config.set(
                path + ".kills",
                profile.getKills()
        );

        config.set(
                path + ".lastNationalDuel",
                profile.getLastNationalDuelTimestamp()
        );

        config.set(
                path + ".outlaw.level",
                profile.getOutlawLevel()
        );

        config.set(
                path + ".outlaw.until",
                profile.getOutlawUntilMillis()
        );

        config.set(
                path + ".outlaw.resets",
                profile.getOutlawResetCount()
        );

        for (Skill skill : Skill.values()) {
            config.set(
                    path + ".skillXp." +
                            skill.name(),
                    profile.getSkillXp(skill)
            );
        }

        plugin.saveConfig();
    }

    public void adminPersistProfile(
            PlayerRankProfile profile) {
        if (profile != null) {
            persistProfile(profile);
        }
    }

    public PlayerRankProfile getProfile(UUID uuid) {
        PlayerRankProfile profile =
                profiles.computeIfAbsent(
                        uuid,
                        PlayerRankProfile::new
                );

        profile.clearExpiredOutlaw();
        persistProfileIfChanged(profile);
        return profile;
    }

    private void persistProfileIfChanged(
            PlayerRankProfile profile) {
        // Intentionally kept lightweight. Configuration persistence
        // is performed by state-changing operations.
    }

    public List<PlayerRankProfile> getAllProfiles() {
        return new ArrayList<>(profiles.values());
    }

    public boolean hasSkill(UUID uuid) {
        return getProfile(uuid).hasSkill();
    }

    public Skill getSkill(UUID uuid) {
        return getProfile(uuid).getSkill();
    }

    public RankTier getTier(UUID uuid) {
        return getProfile(uuid).getTier();
    }

    public boolean assignSkillIfAbsent(
            UUID uuid,
            Skill skill) {

        if (skill == null) return false;

        PlayerRankProfile profile =
                getProfile(uuid);

        if (profile.hasSkill()) {
            return false;
        }

        profile.setSkill(skill);
        persistProfile(profile);
        return true;
    }

    public void startOutlawPenalty(
            UUID uuid,
            long durationMillis) {

        PlayerRankProfile profile =
                getProfile(uuid);

        profile.startOutlawPenalty(
                durationMillis
        );

        persistProfile(profile);
    }

    public boolean refreshOutlawPenalty(
            UUID uuid,
            long durationMillis,
            int maximumResets) {

        PlayerRankProfile profile =
                getProfile(uuid);

        boolean refreshed =
                profile.refreshOutlawPenalty(
                        durationMillis,
                        maximumResets
                );

        if (refreshed) {
            persistProfile(profile);
        }

        return refreshed;
    }

    public void addKill(UUID uuid) {
        PlayerRankProfile profile =
                getProfile(uuid);

        profile.addKill();
        persistProfile(profile);
    }

    public int getKills(UUID uuid) {
        return getProfile(uuid).getKills();
    }

    public void addSkillXp(
            UUID uuid,
            Skill skill,
            long xp) {

        PlayerRankProfile profile =
                getProfile(uuid);

        profile.addSkillXp(skill, xp);
        persistProfile(profile);
    }

    public int getSkillLevel(
            UUID uuid,
            Skill skill) {

        return getProfile(uuid)
                .getSkillLevel(skill);
    }

    public List<PlayerRankProfile> getTopKillers(
            int n) {

        List<PlayerRankProfile> sorted =
                new ArrayList<>(
                        profiles.values()
                );

        sorted.sort(
                Comparator
                        .comparingInt(
                                PlayerRankProfile::getKills
                        )
                        .reversed()
                        .thenComparing(
                                p -> p.getUuid().toString()
                        )
        );

        return sorted.subList(
                0,
                Math.min(
                        n,
                        sorted.size()
                )
        );
    }

    public List<PlayerRankProfile> getOccupants(
            Skill skill,
            RankTier tier) {

        List<PlayerRankProfile> result =
                new ArrayList<>();

        for (
                PlayerRankProfile profile :
                profiles.values()
        ) {
            if (
                    profile.getSkill() == skill &&
                            profile.getTier() == tier
            ) {
                result.add(profile);
            }
        }

        result.sort(
                Comparator
                        .comparingInt(
                                PlayerRankProfile::getKills
                        )
                        .reversed()
                        .thenComparing(
                                p -> p.getUuid().toString()
                        )
        );

        return result;
    }

    public PlayerRankProfile getTopPlayerForSkill(
            Skill skill) {

        PlayerRankProfile best = null;

        for (
                PlayerRankProfile profile :
                profiles.values()
        ) {
            if (
                    profile.getSkill() != skill ||
                            profile.getTier() ==
                                    RankTier.CIVILLIAN
            ) {
                continue;
            }

            if (
                    best == null ||
                            profile.getTier()
                                    .ordinal() >
                                    best.getTier()
                                            .ordinal() ||
                            (
                                    profile.getTier()
                                            == best.getTier() &&
                                    profile.getKills() >
                                            best.getKills()
                            )
            ) {
                best = profile;
            }
        }

        return best;
    }

    public boolean isNationalEligible(
            UUID uuid) {

        PlayerRankProfile profile =
                getProfile(uuid);

        return (
                System.currentTimeMillis() -
                        profile
                                .getLastNationalDuelTimestamp()
        ) >= NATIONAL_COOLDOWN_MILLIS;
    }

    public long getRemainingNationalCooldownMillis(
            UUID uuid) {

        long remaining =
                NATIONAL_COOLDOWN_MILLIS -
                        (
                                System.currentTimeMillis() -
                                        getProfile(uuid)
                                                .getLastNationalDuelTimestamp()
                        );

        return Math.max(0L, remaining);
    }

    /**
     * Marks National participation only for UUIDs explicitly passed.
     * The caller should pass the player who was National at the start
     * of a formal duel, plus any newly crowned National.
     */
    public void markNationalDuel(
            UUID... uuids) {

        long now =
                System.currentTimeMillis();

        for (UUID uuid : uuids) {
            PlayerRankProfile profile =
                    getProfile(uuid);

            profile.setLastNationalDuelTimestamp(now);
            persistProfile(profile);
        }
    }

    public void enforceNationalInactivity() {
        for (Skill skill : Skill.values()) {
            PlayerRankProfile national =
                    getNationalOccupant(skill);

            if (national == null) continue;

            if (!isNationalEligible(national.getUuid())) {
                continue;
            }

            List<PlayerRankProfile> sPlayers =
                    getOccupants(skill, RankTier.S);

            if (sPlayers.isEmpty()) {
                continue;
            }

            PlayerRankProfile promoted =
                    sPlayers.get(0);

            transferNationalWeapons(
                    national.getUuid(),
                    promoted.getUuid()
            );

            national.setTier(RankTier.S);
            persistProfile(national);

            promoted.setTier(RankTier.NATIONAL);
            promoted.setLastNationalDuelTimestamp(
                    System.currentTimeMillis()
            );
            persistProfile(promoted);

            ensureNationalWeapon(
                    promoted.getUuid(),
                    skill
            );

            refreshOnlineDisplay(
                    national.getUuid()
            );
            refreshOnlineDisplay(
                    promoted.getUuid()
            );
        }
    }

    public PlayerRankProfile getNationalOccupant(
            Skill skill) {

        for (
                PlayerRankProfile profile :
                profiles.values()
        ) {
            if (
                    profile.getSkill() == skill &&
                            profile.getTier() ==
                                    RankTier.NATIONAL
            ) {
                return profile;
            }
        }

        return null;
    }

    public String formatCooldown(long millis) {
        long totalSeconds =
                Math.max(
                        0L,
                        (millis + 999L) / 1000L
                );

        long days =
                totalSeconds / 86400L;
        totalSeconds %= 86400L;

        long hours =
                totalSeconds / 3600L;
        totalSeconds %= 3600L;

        long minutes =
                totalSeconds / 60L;
        long seconds =
                totalSeconds % 60L;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }

        return seconds + "s";
    }

    public UUID findLowestKillOccupant(
            Skill skill,
            RankTier tier,
            UUID... excluded) {

        PlayerRankProfile lowest =
                null;

        for (
                PlayerRankProfile profile :
                profiles.values()
        ) {
            if (
                    profile.getSkill() != skill ||
                            profile.getTier() != tier
            ) {
                continue;
            }

            if (isExcluded(
                    profile.getUuid(),
                    excluded
            )) {
                continue;
            }

            if (
                    lowest == null ||
                            profile.getKills() <
                                    lowest.getKills() ||
                            (
                                    profile.getKills() ==
                                            lowest.getKills() &&
                                    profile.getUuid()
                                            .toString()
                                            .compareTo(
                                                    lowest.getUuid()
                                                            .toString()
                                            ) < 0
                            )
            ) {
                lowest = profile;
            }
        }

        return lowest == null
                ? null
                : lowest.getUuid();
    }

    private UUID findHighestKillOccupant(
            Skill skill,
            RankTier tier) {

        PlayerRankProfile highest =
                null;

        for (
                PlayerRankProfile profile :
                profiles.values()
        ) {
            if (
                    profile.getSkill() != skill ||
                            profile.getTier() != tier
            ) {
                continue;
            }

            if (
                    highest == null ||
                            profile.getKills() >
                                    highest.getKills() ||
                            (
                                    profile.getKills() ==
                                            highest.getKills() &&
                                    profile.getUuid()
                                            .toString()
                                            .compareTo(
                                                    highest.getUuid()
                                                            .toString()
                                            ) < 0
                            )
            ) {
                highest = profile;
            }
        }

        return highest == null
                ? null
                : highest.getUuid();
    }

    private int countOccupants(
            Skill skill,
            RankTier tier,
            UUID... excluded) {

        int count = 0;

        for (
                PlayerRankProfile profile :
                profiles.values()
        ) {
            if (
                    profile.getSkill() != skill ||
                            profile.getTier() != tier
            ) {
                continue;
            }

            if (
                    !isExcluded(
                            profile.getUuid(),
                            excluded
                    )
            ) {
                count++;
            }
        }

        return count;
    }

    private boolean isExcluded(
            UUID uuid,
            UUID... excluded) {

        if (excluded == null) return false;

        for (UUID excludedUuid : excluded) {
            if (
                    uuid.equals(excludedUuid)
            ) {
                return true;
            }
        }

        return false;
    }

    private UUID occupy(
            UUID uuid,
            Skill skill,
            RankTier tier,
            UUID... excluded) {

        PlayerRankProfile profile =
                getProfile(uuid);

        if (
                tier == RankTier.CIVILLIAN
        ) {
            profile.setSkill(skill);
            profile.setTier(
                    RankTier.CIVILLIAN
            );
            persistProfile(profile);
            return null;
        }

        int capacity =
                tier.getSlotsPerSkill();

        if (
                countOccupants(
                        skill,
                        tier,
                        excluded
                ) >= capacity
        ) {
            UUID bumped =
                    findLowestKillOccupant(
                            skill,
                            tier,
                            excluded
                    );

            if (bumped != null) {
                PlayerRankProfile bumpProfile =
                        getProfile(bumped);

                bumpProfile.setTier(
                        RankTier.CIVILLIAN
                );

                persistProfile(
                        bumpProfile
                );

                profile.setSkill(skill);
                profile.setTier(tier);
                persistProfile(profile);

                return bumped;
            }
        }

        profile.setSkill(skill);
        profile.setTier(tier);
        persistProfile(profile);
        return null;
    }

    /**
     * Resolves all formal duel outcomes.
     *
     * Rules:
     * - Civillian vs ranked: winner takes the ranked seat.
     * - Civillian vs Civillian: winner receives E.
     * - Same tier: winner promotes one tier; loser becomes Civillian.
     * - Different tier, same skill: direct rank swap.
     * - Different tier, different skill: prevented by RankDuelCommand,
     *   except National cases.
     * - National vs National, different skills: winner remains National;
     *   defeated National's skill cascades upward from lower tiers.
     */
    public DuelResult resolveDuel(
            UUID winnerUUID,
            Skill winnerSkill,
            UUID loserUUID,
            Skill loserSkill) {

        PlayerRankProfile winner =
                getProfile(winnerUUID);

        PlayerRankProfile loser =
                getProfile(loserUUID);

        RankTier winnerOld =
                winner.getTier();

        RankTier loserOld =
                loser.getTier();

        List<UUID> bumped =
                new ArrayList<>();

        boolean winnerWasNational =
                winnerOld == RankTier.NATIONAL;

        boolean loserWasNational =
                loserOld == RankTier.NATIONAL;

        boolean crossSkill =
                winnerSkill != null &&
                        loserSkill != null &&
                        winnerSkill != loserSkill;

        /*
         * National-vs-National cross-skill:
         * winner keeps National in own skill.
         * defeated National's skill uses the explicit
         * low-kill upward cascade:
         * low-kill S -> National,
         * low-kill A -> S,
         * ... until E vacancy.
         */
        if (
                winnerWasNational &&
                        loserWasNational &&
                        crossSkill
        ) {
            List<UUID> cascade =
                    cascadeNationalVacancy(
                            loserSkill
                    );

            bumped.addAll(cascade);

            transferNationalWeapons(
                    loserUUID,
                    winnerUUID
            );

            winner.setSkill(winnerSkill);
            winner.setTier(RankTier.NATIONAL);
            persistProfile(winner);

            loser.setSkill(loserSkill);
            loser.setTier(RankTier.CIVILLIAN);
            persistProfile(loser);

            ensureNationalWeapon(
                    winnerUUID,
                    winnerSkill
            );
            markNationalDuel(winnerUUID);

            return new DuelResult(
                    DuelResult.Type.NATIONAL_DEFENSE,
                    winnerUUID,
                    loserUUID,
                    RankTier.NATIONAL,
                    RankTier.NATIONAL,
                    RankTier.NATIONAL,
                    RankTier.CIVILLIAN,
                    bumped
            );
        }

        /*
         * If the loser is National and the winner is not,
         * winner becomes National in his own skill.
         * The defeated National skill cascades only when this
         * was a cross-skill challenge. Same-skill is a direct swap.
         */
        if (loserWasNational) {
            if (
                    crossSkill
            ) {
                List<UUID> cascade =
                        cascadeNationalVacancy(
                                loserSkill
                        );

                bumped.addAll(cascade);

                transferNationalWeapons(
                        loserUUID,
                        winnerUUID
                );

                winner.setSkill(winnerSkill);
                winner.setTier(
                        RankTier.NATIONAL
                );
                persistProfile(winner);

                loser.setSkill(loserSkill);
                loser.setTier(
                        RankTier.CIVILLIAN
                );
                persistProfile(loser);

                ensureNationalWeapon(
                        winnerUUID,
                        winnerSkill
                );
                markNationalDuel(winnerUUID);

                return new DuelResult(
                        DuelResult.Type.NATIONAL_DEFENSE,
                        winnerUUID,
                        loserUUID,
                        winnerOld,
                        RankTier.NATIONAL,
                        RankTier.NATIONAL,
                        RankTier.CIVILLIAN,
                        bumped
                );
            }

            /*
             * Same-skill National challenge:
             * direct swap of National and challenger's rank.
             */
            winner.setSkill(winnerSkill);
            winner.setTier(
                    RankTier.NATIONAL
            );
            persistProfile(winner);

            loser.setSkill(loserSkill);
            loser.setTier(winnerOld);
            persistProfile(loser);

            transferNationalWeapons(
                    loserUUID,
                    winnerUUID
            );
            ensureNationalWeapon(
                    winnerUUID,
                    winnerSkill
            );
            markNationalDuel(winnerUUID);

            return new DuelResult(
                    DuelResult.Type.NATIONAL_DEFENSE,
                    winnerUUID,
                    loserUUID,
                    winnerOld,
                    RankTier.NATIONAL,
                    RankTier.NATIONAL,
                    winnerOld,
                    bumped
            );
        }

        /*
         * National defending against a lower-tier player.
         * Same-skill is a direct rank swap: the National becomes
         * the challenger's old tier.
         */
        if (winnerWasNational) {
            if (
                    crossSkill
            ) {
                /*
                 * Winner was already National and defeated a
                 * lower-tier cross-skill challenger. National
                 * stays where it is; loser remains in the
                 * challenger's own tier.
                 */
                winner.setSkill(winnerSkill);
                winner.setTier(
                        RankTier.NATIONAL
                );
                persistProfile(winner);

                loser.setSkill(loserSkill);
                loser.setTier(loserOld);
                persistProfile(loser);

                markNationalDuel(winnerUUID);

                return new DuelResult(
                        DuelResult.Type.NATIONAL_DEFENSE,
                        winnerUUID,
                        loserUUID,
                        RankTier.NATIONAL,
                        RankTier.NATIONAL,
                        loserOld,
                        loserOld,
                        bumped
                );
            }

            winner.setSkill(winnerSkill);
            winner.setTier(
                    RankTier.NATIONAL
            );
            persistProfile(winner);

            loser.setSkill(loserSkill);
            loser.setTier(
                    loserOld
            );
            persistProfile(loser);

            markNationalDuel(winnerUUID);

            return new DuelResult(
                    DuelResult.Type.NATIONAL_DEFENSE,
                    winnerUUID,
                    loserUUID,
                    RankTier.NATIONAL,
                    RankTier.NATIONAL,
                    loserOld,
                    loserOld,
                    bumped
            );
        }

        /*
         * Both Civillian:
         * winner receives E; loser stays Civillian.
         */
        if (
                winnerOld == RankTier.CIVILLIAN &&
                        loserOld == RankTier.CIVILLIAN
        ) {
            UUID bump =
                    occupy(
                            winnerUUID,
                            winnerSkill,
                            RankTier.E
                    );

            if (bump != null) {
                bumped.add(bump);
            }

            loser.setTier(
                    RankTier.CIVILLIAN
            );
            loser.setSkill(
                    loser.hasSkill()
                            ? loser.getSkill()
                            : loserSkill
            );
            persistProfile(loser);

            return new DuelResult(
                    DuelResult.Type.PROMOTION,
                    winnerUUID,
                    loserUUID,
                    RankTier.CIVILLIAN,
                    RankTier.E,
                    RankTier.CIVILLIAN,
                    RankTier.CIVILLIAN,
                    bumped
            );
        }

        /*
         * Civillian vs ranked:
         * winner takes the exact defeated player's tier
         * in winner's newly locked skill.
         */
        if (
                winnerOld == RankTier.CIVILLIAN &&
                        loserOld != RankTier.CIVILLIAN
        ) {
            UUID bump =
                    occupy(
                            winnerUUID,
                            winnerSkill,
                            loserOld,
                            loserUUID
                    );

            if (bump != null) {
                bumped.add(bump);
            }

            loser.setTier(
                    RankTier.CIVILLIAN
            );
            loser.setSkill(loserSkill);
            persistProfile(loser);

            return new DuelResult(
                    DuelResult.Type.FULL_SWAP,
                    winnerUUID,
                    loserUUID,
                    RankTier.CIVILLIAN,
                    loserOld,
                    loserOld,
                    RankTier.CIVILLIAN,
                    bumped
            );
        }

        /*
         * Ranked vs Civillian:
         * Winner keeps his current ranked seat.
         * Loser is already Civillian, so no rank is lost.
         */
        if (
                winnerOld != RankTier.CIVILLIAN &&
                        loserOld == RankTier.CIVILLIAN
        ) {
            winner.setSkill(winnerSkill);
            winner.setTier(winnerOld);
            persistProfile(winner);

            loser.setSkill(loserSkill);
            loser.setTier(RankTier.CIVILLIAN);
            persistProfile(loser);

            return new DuelResult(
                    DuelResult.Type.FULL_SWAP,
                    winnerUUID,
                    loserUUID,
                    winnerOld,
                    winnerOld,
                    RankTier.CIVILLIAN,
                    RankTier.CIVILLIAN,
                    bumped
            );
        }

        /*
         * Same tier:
         * winner moves exactly one tier upward.
         * If next tier is full, the lowest-kill occupant is
         * removed to Civillian and the winner takes the seat.
         * Loser becomes Civillian.
         */
        if (winnerOld == loserOld) {
            RankTier next =
                    winnerOld.next();

            if (next == RankTier.NATIONAL) {
                UUID bump =
                        occupy(
                                winnerUUID,
                                winnerSkill,
                                RankTier.NATIONAL
                        );

                if (bump != null) {
                    bumped.add(bump);
                }
            } else {
                UUID bump =
                        occupy(
                                winnerUUID,
                                winnerSkill,
                                next
                        );

                if (bump != null) {
                    bumped.add(bump);
                }
            }

            loser.setTier(
                    RankTier.CIVILLIAN
            );
            loser.setSkill(loserSkill);
            persistProfile(loser);

            if (next == RankTier.NATIONAL) {
                markNationalDuel(winnerUUID);
                ensureNationalWeapon(
                        winnerUUID,
                        winnerSkill
                );
            }

            return new DuelResult(
                    DuelResult.Type.PROMOTION,
                    winnerUUID,
                    loserUUID,
                    winnerOld,
                    next,
                    loserOld,
                    RankTier.CIVILLIAN,
                    bumped
            );
        }

        /*
         * Different tier, same skill:
         *
         * - Lower-ranked winner steals the higher seat.
         * - Higher-ranked winner does not gain anything; both
         *   players keep their existing tiers.
         */
        if (
                winnerSkill == loserSkill
        ) {
            if (
                    winnerOld.ordinal() <
                            loserOld.ordinal()
            ) {
                winner.setSkill(winnerSkill);
                winner.setTier(loserOld);
                persistProfile(winner);

                loser.setSkill(loserSkill);
                loser.setTier(winnerOld);
                persistProfile(loser);

                return new DuelResult(
                        DuelResult.Type.FULL_SWAP,
                        winnerUUID,
                        loserUUID,
                        winnerOld,
                        loserOld,
                        loserOld,
                        winnerOld,
                        bumped
                );
            }

            winner.setSkill(winnerSkill);
            winner.setTier(winnerOld);
            persistProfile(winner);

            loser.setSkill(loserSkill);
            loser.setTier(loserOld);
            persistProfile(loser);

            return new DuelResult(
                    DuelResult.Type.FULL_SWAP,
                    winnerUUID,
                    loserUUID,
                    winnerOld,
                    winnerOld,
                    loserOld,
                    loserOld,
                    bumped
            );
        }

        /*
         * Cross-skill non-National different-tier fights are
         * rejected by the command layer. This defensive branch
         * leaves both players untouched if a malformed call ever
         * reaches this method.
         */
        return new DuelResult(
                DuelResult.Type.VOID,
                winnerUUID,
                loserUUID,
                winnerOld,
                winnerOld,
                loserOld,
                loserOld,
                bumped
        );
    }

    /**
     * National cross-skill vacancy cascade:
     *
     * National -> lowest-kill S
     * S -> lowest-kill A
     * A -> lowest-kill B
     * ...
     * D -> lowest-kill E
     *
     * The final E vacancy remains empty.
     */
    private List<UUID> cascadeNationalVacancy(
            Skill skill) {

        List<UUID> moved =
                new ArrayList<>();

        RankTier vacancy =
                RankTier.NATIONAL;

        while (
                vacancy.ordinal() >
                        RankTier.E.ordinal()
        ) {
            RankTier search =
                    vacancy.previous();

            UUID source = null;
            RankTier sourceTier = null;

            while (
                    search.ordinal() >=
                            RankTier.E.ordinal()
            ) {
                source =
                        findLowestKillOccupant(
                                skill,
                                search
                        );

                if (source != null) {
                    sourceTier = search;
                    break;
                }

                search =
                        search.previous();
            }

            if (
                    source == null ||
                            sourceTier == null
            ) {
                break;
            }

            PlayerRankProfile profile =
                    getProfile(source);

            profile.setTier(vacancy);
            persistProfile(profile);

            moved.add(source);

            vacancy = sourceTier;
        }

        if (!moved.isEmpty()) {
            PlayerRankProfile newNational =
                    getProfile(
                            moved.get(0)
                    );

            if (
                    newNational.getTier() ==
                            RankTier.NATIONAL
            ) {
                newNational.setLastNationalDuelTimestamp(
                        System.currentTimeMillis()
                );
                persistProfile(newNational);

                ensureNationalWeapon(
                        newNational.getUuid(),
                        skill
                );
            }
        }

        return moved;
    }

    /**
     * A defeated National's old weapon(s) never survive the loss:
     * they vanish from the loser entirely instead of dropping or
     * being cloned across. The winner is always granted a brand
     * new, full-durability National weapon separately via
     * {@link #ensureNationalWeapon}, so this only needs to strip
     * the loser clean.
     */
    public void transferNationalWeapons(
            UUID fromUuid,
            UUID toUuid) {

        removeAllNationalWeapons(
                fromUuid
        );
    }

    public void ensureNationalWeapon(
            UUID uuid,
            Skill skill) {

        Player player =
                Bukkit.getPlayer(uuid);

        if (
                player == null ||
                        skill == null
        ) {
            return;
        }

        List<ItemStack> required =
                WeaponUtil.createRankWeaponSet(
                        skill,
                        RankTier.NATIONAL,
                        true
                );

        for (ItemStack template : required) {
            if (
                    hasMatchingNationalWeapon(
                            player,
                            skill,
                            WeaponUtil.getRole(template)
                    )
            ) {
                continue;
            }

            grantItem(player, template);
        }
    }

    private boolean hasMatchingNationalWeapon(
            Player player,
            Skill skill,
            String role) {

        for (ItemStack item :
                player.getInventory().getContents()) {
            if (matchesNationalWeapon(item, skill, role)) {
                return true;
            }
        }

        return matchesNationalWeapon(
                player.getInventory().getItemInOffHand(),
                skill,
                role
        );
    }

    private boolean matchesNationalWeapon(
            ItemStack item,
            Skill skill,
            String role) {

        if (!WeaponUtil.isNationalWeapon(item)) {
            return false;
        }

        if (WeaponUtil.getTaggedSkill(item) != skill) {
            return false;
        }

        if (role == null) {
            return true;
        }

        return role.equals(WeaponUtil.getRole(item));
    }

    private void grantItem(
            Player player,
            ItemStack item) {

        Map<Integer, ItemStack> leftovers =
                player.getInventory().addItem(
                        item
                );

        if (!leftovers.isEmpty()) {
            for (ItemStack leftover :
                    leftovers.values()) {
                player.getEnderChest().addItem(
                        leftover
                );
            }
        }
    }

    public void removeAllNationalWeapons(
            UUID uuid) {

        Player player =
                Bukkit.getPlayer(uuid);

        if (player == null) {
            return;
        }

        removeAllNationalWeapons(player);
    }

    /**
     * Removes a player's National weapon(s) for a specific skill
     * (both pieces, for SPEARMACE), or every National weapon they
     * hold when skill is null. Used by the admin cleanup command.
     */
    public void removeNationalWeapon(
            UUID uuid,
            Skill skill) {

        Player player =
                Bukkit.getPlayer(uuid);

        if (player == null) {
            return;
        }

        ItemStack[] contents =
                player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            if (
                    WeaponUtil.isNationalWeapon(contents[i]) &&
                            (
                                    skill == null ||
                                            WeaponUtil.getTaggedSkill(
                                                    contents[i]
                                            ) == skill
                            )
            ) {
                contents[i] = null;
            }
        }

        player.getInventory().setContents(
                contents
        );

        ItemStack offhand =
                player.getInventory().getItemInOffHand();

        if (
                WeaponUtil.isNationalWeapon(offhand) &&
                        (
                                skill == null ||
                                        WeaponUtil.getTaggedSkill(offhand) ==
                                                skill
                        )
        ) {
            player.getInventory().setItemInOffHand(
                    null
            );
        }
    }

    private void removeAllNationalWeapons(
            Player player) {

        ItemStack[] contents =
                player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            if (
                    WeaponUtil.isNationalWeapon(
                            contents[i]
                    )
            ) {
                contents[i] = null;
            }
        }

        player.getInventory().setContents(
                contents
        );

        if (
                WeaponUtil.isNationalWeapon(
                        player.getInventory()
                                .getItemInOffHand()
                )
        ) {
            player.getInventory().setItemInOffHand(
                    null
            );
        }
    }

    public void refreshOnlineDisplay(
            UUID uuid) {

        Player player =
                Bukkit.getPlayer(uuid);

        if (player == null) return;

        PlayerRankProfile profile =
                getProfile(uuid);

        String prefix =
                profile.getDisplayPrefix();

        ChatColor tierColor =
                getTierColor(
                        profile.getTier()
                );

        String visibleName =
                buildStyledPlayerName(
                        profile,
                        player.getName()
                );

        player.setPlayerListName(
                visibleName
        );

        player.setDisplayName(
                visibleName
        );

        Scoreboard scoreboard =
                Bukkit.getScoreboardManager()
                        .getMainScoreboard();

        String teamName =
                "nr_" +
                        uuid.toString()
                                .replace("-", "")
                                .substring(0, 12);

        Team team =
                scoreboard.getTeam(
                        teamName
                );

        if (team == null) {
            team =
                    scoreboard.registerNewTeam(
                            teamName
                    );
        }

        for (Team existing :
                scoreboard.getTeams()) {
            if (
                    existing != team &&
                            existing.getName().startsWith("nr_") &&
                            existing.hasEntry(player.getName())
            ) {
                existing.removeEntry(
                        player.getName()
                );
            }
        }

        team.setPrefix(
                buildStyledTeamPrefix(
                        profile
                )
        );

        team.setSuffix(
                ChatColor.RESET.toString()
        );

        if (!team.hasEntry(player.getName())) {
            team.addEntry(
                    player.getName()
            );
        }
    }

    private String buildStyledPlayerName(
            PlayerRankProfile profile,
            String playerName) {

        RankTier tier =
                profile.getTier();

        if (
                tier == RankTier.CIVILLIAN ||
                        profile.getSkill() == null
        ) {
            return ChatColor.GRAY +
                    ChatColor.BOLD +
                    "◆ Civillian" +
                    ChatColor.DARK_GRAY +
                    ChatColor.BOLD +
                    " │ " +
                    ChatColor.RESET +
                    ChatColor.GRAY +
                    playerName;
        }

        ChatColor tierColor =
                getTierColor(tier);

        return tierColor +
                ChatColor.BOLD +
                rankBadge(tier) +
                " " +
                tier.getDisplayName() +
                " " +
                skillIcon(profile.getSkill()) +
                " " +
                profile.getSkill().getMasterTitle() +
                ChatColor.DARK_GRAY +
                ChatColor.BOLD +
                " │ " +
                ChatColor.RESET +
                ChatColor.WHITE +
                playerName;
    }

    private String buildStyledTeamPrefix(
            PlayerRankProfile profile) {

        RankTier tier =
                profile.getTier();

        if (
                tier == RankTier.CIVILLIAN ||
                        profile.getSkill() == null
        ) {
            return ChatColor.GRAY +
                    ChatColor.BOLD +
                    "◆ Civillian " +
                    ChatColor.RESET;
        }

        return getTierColor(tier) +
                ChatColor.BOLD +
                rankBadge(tier) +
                " " +
                tier.getDisplayName() +
                " " +
                skillIcon(profile.getSkill()) +
                " " +
                profile.getSkill().getMasterTitle() +
                " " +
                ChatColor.RESET;
    }

    private String rankBadge(
            RankTier tier) {

        return switch (tier) {
            case NATIONAL -> "♛";
            case S -> "✦";
            case A -> "★";
            case B -> "◆";
            case C -> "◇";
            case D -> "•";
            case E -> "◈";
            case CIVILLIAN -> "◆";
        };
    }

    private String skillIcon(
            Skill skill) {

        return switch (skill) {
            case SWORD -> "⚔";
            case AXE -> "⚒";
            case MACE -> "✹";
            case SPEARMACE -> "✦";
            case BOW -> "➳";
        };
    }

    private ChatColor getTierColor(
            RankTier tier) {

        return switch (tier) {
            case NATIONAL -> ChatColor.GOLD;
            case S -> ChatColor.DARK_RED;
            case A -> ChatColor.RED;
            case B -> ChatColor.DARK_PURPLE;
            case C -> ChatColor.LIGHT_PURPLE;
            case D -> ChatColor.BLUE;
            case E -> ChatColor.AQUA;
            case CIVILLIAN -> ChatColor.GRAY;
        };
    }
}
