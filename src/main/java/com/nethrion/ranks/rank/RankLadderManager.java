package com.nethrion.ranks.rank;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RankLadderManager {

    public static final long NATIONAL_COOLDOWN_MILLIS = 3L * 24L * 60L * 60L * 1000L;

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final Map<UUID, PlayerRankProfile> profiles = new HashMap<>();

    public RankLadderManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        plugin.saveDefaultConfig();
        loadAllProfiles();
    }

    private void loadAllProfiles() {
        ConfigurationSection section = config.getConfigurationSection("rankladder");
        if (section == null) return;

        for (String uuidString : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                PlayerRankProfile profile = new PlayerRankProfile(uuid);

                String skillName = section.getString(uuidString + ".skill", "NONE");
                if (!"NONE".equalsIgnoreCase(skillName)) {
                    profile.setSkill(Skill.valueOf(skillName));
                }

                profile.setTier(RankTier.valueOf(
                        section.getString(uuidString + ".tier", "CIVILLIAN")
                ));
                profile.setKills(section.getInt(uuidString + ".kills", 0));
                profile.setLastNationalDuelTimestamp(
                        section.getLong(uuidString + ".lastNationalDuel", 0L)
                );
                profile.setOutlawState(
                        section.getInt(uuidString + ".outlaw.level", 0),
                        section.getLong(uuidString + ".outlaw.until", 0L)
                );

                for (Skill skill : Skill.values()) {
                    profile.setSkillXp(
                            skill,
                            section.getLong(
                                    uuidString + ".skillXp." + skill.name(),
                                    0L
                            )
                    );
                }

                profiles.put(uuid, profile);
            } catch (Exception ignored) {
                plugin.getLogger().warning("Skipped invalid rank profile: " + uuidString);
            }
        }
    }

    private void persistProfile(PlayerRankProfile profile) {
        profile.clearExpiredOutlaw();

        String path = "rankladder." + profile.getUuid();
        config.set(path + ".skill",
                profile.hasSkill() ? profile.getSkill().name() : "NONE");
        config.set(path + ".tier", profile.getTier().name());
        config.set(path + ".kills", profile.getKills());
        config.set(path + ".lastNationalDuel",
                profile.getLastNationalDuelTimestamp());
        config.set(path + ".outlaw.level", profile.getOutlawLevel());
        config.set(path + ".outlaw.until", profile.getOutlawUntilMillis());

        for (Skill skill : Skill.values()) {
            config.set(
                    path + ".skillXp." + skill.name(),
                    profile.getSkillXp(skill)
            );
        }

        plugin.saveConfig();
    }

    public PlayerRankProfile getProfile(UUID uuid) {
        PlayerRankProfile profile =
                profiles.computeIfAbsent(uuid, PlayerRankProfile::new);
        profile.clearExpiredOutlaw();
        return profile;
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

    public boolean assignSkillIfAbsent(UUID uuid, Skill skill) {
        if (skill == null) return false;
        PlayerRankProfile profile = getProfile(uuid);
        if (profile.hasSkill()) return false;
        profile.setSkill(skill);
        persistProfile(profile);
        return true;
    }

    public void addKill(UUID uuid) {
        PlayerRankProfile profile = getProfile(uuid);
        profile.addKill();
        persistProfile(profile);
    }

    public int getKills(UUID uuid) {
        return getProfile(uuid).getKills();
    }

    public void addSkillXp(UUID uuid, Skill skill, long xp) {
        PlayerRankProfile profile = getProfile(uuid);
        profile.addSkillXp(skill, xp);
        persistProfile(profile);
    }

    public int getSkillLevel(UUID uuid, Skill skill) {
        return getProfile(uuid).getSkillLevel(skill);
    }

    public List<PlayerRankProfile> getTopKillers(int n) {
        List<PlayerRankProfile> sorted = new ArrayList<>(profiles.values());
        sorted.sort(
                Comparator.comparingInt(PlayerRankProfile::getKills)
                        .reversed()
                        .thenComparing(p -> p.getUuid().toString())
        );
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    public List<PlayerRankProfile> getOccupants(Skill skill, RankTier tier) {
        List<PlayerRankProfile> result = new ArrayList<>();
        for (PlayerRankProfile profile : profiles.values()) {
            if (profile.getSkill() == skill && profile.getTier() == tier) {
                result.add(profile);
            }
        }
        result.sort(
                Comparator.comparingInt(PlayerRankProfile::getKills)
                        .reversed()
        );
        return result;
    }

    public boolean isNationalEligible(UUID uuid) {
        PlayerRankProfile profile = getProfile(uuid);
        return System.currentTimeMillis() - profile.getLastNationalDuelTimestamp()
                >= NATIONAL_COOLDOWN_MILLIS;
    }

    public long getRemainingNationalCooldownMillis(UUID uuid) {
        long remaining =
                NATIONAL_COOLDOWN_MILLIS -
                        (System.currentTimeMillis() -
                                getProfile(uuid).getLastNationalDuelTimestamp());
        return Math.max(0L, remaining);
    }

    public void markNationalDuel(UUID... uuids) {
        long now = System.currentTimeMillis();
        for (UUID uuid : uuids) {
            PlayerRankProfile profile = getProfile(uuid);
            profile.setLastNationalDuelTimestamp(now);
            persistProfile(profile);
        }
    }

    public String formatCooldown(long millis) {
        long totalSeconds = Math.max(0L, (millis + 999L) / 1000L);
        long days = totalSeconds / 86400L;
        totalSeconds %= 86400L;
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    public UUID findLowestKillOccupant(Skill skill, RankTier tier, UUID... excluded) {
        PlayerRankProfile lowest = null;
        for (PlayerRankProfile profile : profiles.values()) {
            if (profile.getSkill() != skill || profile.getTier() != tier) continue;

            boolean skip = false;
            for (UUID uuid : excluded) {
                if (profile.getUuid().equals(uuid)) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;

            if (lowest == null || profile.getKills() < lowest.getKills()) {
                lowest = profile;
            }
        }
        return lowest == null ? null : lowest.getUuid();
    }

    private int countOccupants(Skill skill, RankTier tier, UUID... excluded) {
        int count = 0;
        for (PlayerRankProfile profile : profiles.values()) {
            if (profile.getSkill() != skill || profile.getTier() != tier) continue;

            boolean skip = false;
            for (UUID uuid : excluded) {
                if (profile.getUuid().equals(uuid)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) count++;
        }
        return count;
    }

    private UUID occupy(UUID uuid, Skill skill, RankTier tier, UUID... excluded) {
        PlayerRankProfile profile = getProfile(uuid);

        if (tier != RankTier.CIVILLIAN) {
            int capacity = tier.getSlotsPerSkill();
            if (countOccupants(skill, tier, uuid, excluded) >= capacity) {
                UUID bumped = findLowestKillOccupant(skill, tier, uuid, excluded);
                if (bumped != null) {
                    PlayerRankProfile bumpProfile = getProfile(bumped);
                    bumpProfile.setTier(RankTier.CIVILLIAN);
                    persistProfile(bumpProfile);
                    profile.setSkill(skill);
                    profile.setTier(tier);
                    persistProfile(profile);
                    return bumped;
                }
            }
        }

        profile.setSkill(skill);
        profile.setTier(tier);
        persistProfile(profile);
        return null;
    }

    public DuelResult resolveDuel(
            UUID winnerUUID,
            Skill winnerSkill,
            UUID loserUUID,
            Skill loserSkill) {

        PlayerRankProfile winner = getProfile(winnerUUID);
        PlayerRankProfile loser = getProfile(loserUUID);

        RankTier winnerOld = winner.getTier();
        RankTier loserOld = loser.getTier();

        List<UUID> bumped = new ArrayList<>();

        if (winnerOld == RankTier.NATIONAL || loserOld == RankTier.NATIONAL) {
            return resolveNational(winnerUUID, winnerSkill, loserUUID, loserSkill, bumped);
        }

        if (winnerOld == loserOld) {
            RankTier next = winnerOld.next();

            UUID bump = occupy(winnerUUID, winnerSkill, next);
            if (bump != null) bumped.add(bump);

            PlayerRankProfile loserProfile = getProfile(loserUUID);
            loserProfile.setTier(RankTier.CIVILLIAN);
            loserProfile.setSkill(loserSkill);
            persistProfile(loserProfile);

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

        UUID bumpWinnerSide = occupy(winnerUUID, winnerSkill, loserOld, loserUUID);
        if (bumpWinnerSide != null) bumped.add(bumpWinnerSide);

        UUID bumpLoserSide = occupy(loserUUID, loserSkill, winnerOld, winnerUUID);
        if (bumpLoserSide != null) bumped.add(bumpLoserSide);

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

    private DuelResult resolveNational(
            UUID winnerUUID,
            Skill winnerSkill,
            UUID loserUUID,
            Skill loserSkill,
            List<UUID> bumped) {

        PlayerRankProfile winner = getProfile(winnerUUID);
        PlayerRankProfile loser = getProfile(loserUUID);

        RankTier winnerOld = winner.getTier();
        RankTier loserOld = loser.getTier();

        if (winnerOld == RankTier.NATIONAL && loserOld != RankTier.NATIONAL) {
            RankTier replacementTier = loserOld;
            UUID bump = occupy(winnerUUID, winnerSkill, replacementTier, loserUUID);
            if (bump != null) bumped.add(bump);

            loser.setSkill(loserSkill);
            loser.setTier(RankTier.NATIONAL);
            persistProfile(loser);

            markNationalDuel(winnerUUID, loserUUID);

            return new DuelResult(
                    DuelResult.Type.NATIONAL_DEFENSE,
                    winnerUUID,
                    loserUUID,
                    RankTier.NATIONAL,
                    replacementTier,
                    loserOld,
                    RankTier.NATIONAL,
                    bumped
            );
        }

        if (loserOld == RankTier.NATIONAL && winnerOld != RankTier.NATIONAL) {
            RankTier replacementTier = winnerOld;
            UUID bump = occupy(loserUUID, loserSkill, replacementTier, winnerUUID);
            if (bump != null) bumped.add(bump);

            winner.setSkill(winnerSkill);
            winner.setTier(RankTier.NATIONAL);
            persistProfile(winner);

            markNationalDuel(winnerUUID, loserUUID);

            return new DuelResult(
                    DuelResult.Type.NATIONAL_DEFENSE,
                    winnerUUID,
                    loserUUID,
                    winnerOld,
                    RankTier.NATIONAL,
                    RankTier.NATIONAL,
                    replacementTier,
                    bumped
            );
        }

        // Same-rank National duel: winner keeps the title, loser falls to A.
        winner.setSkill(winnerSkill);
        winner.setTier(RankTier.NATIONAL);
        persistProfile(winner);

        loser.setSkill(loserSkill);
        loser.setTier(RankTier.A);
        persistProfile(loser);

        markNationalDuel(winnerUUID, loserUUID);

        return new DuelResult(
                DuelResult.Type.NATIONAL_DEFENSE,
                winnerUUID,
                loserUUID,
                RankTier.NATIONAL,
                RankTier.NATIONAL,
                RankTier.NATIONAL,
                RankTier.A,
                bumped
        );
    }

    public void refreshOnlineDisplay(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        PlayerRankProfile profile = getProfile(uuid);
        String prefix = profile.getDisplayPrefix();

        String visibleName =
                ChatColor.GRAY + "[" +
                        ChatColor.GOLD + prefix +
                        ChatColor.GRAY + "] " +
                        ChatColor.RESET +
                        player.getName();

        player.setPlayerListName(visibleName);
        player.setDisplayName(visibleName);
    }
}
