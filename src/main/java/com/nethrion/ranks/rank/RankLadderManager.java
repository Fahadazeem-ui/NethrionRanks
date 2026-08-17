package com.nethrion.ranks.rank;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core Rank Data Engine.
 * Yeh class kisi bhi command se independent hai — sirf pure logic aur
 * config.yml persistence sambhalti hai. Commands (Phase 2/3) isko sirf call karenge.
 */
public class RankLadderManager {

    private final JavaPlugin plugin;
    private final FileConfiguration config;

    // Har player ka profile RAM mein cache — offline players ka bhi (startup pe load hota hai)
    private final Map<UUID, PlayerRankProfile> profiles = new HashMap<>();

    public RankLadderManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadAllProfiles();
    }

    // ---------- Loading & Persistence ----------

    private void loadAllProfiles() {
        ConfigurationSection section = config.getConfigurationSection("rankladder");
        if (section == null) return;

        for (String uuidString : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                PlayerRankProfile profile = new PlayerRankProfile(uuid);

                String skillName = section.getString(uuidString + ".skill", "NONE");
                if (!skillName.equals("NONE")) {
                    profile.setSkill(Skill.valueOf(skillName));
                }

                String tierName = section.getString(uuidString + ".tier", "CIVILLIAN");
                profile.setTier(RankTier.valueOf(tierName));

                profile.setKills(section.getInt(uuidString + ".kills", 0));
                profile.setLastNationalDuelTimestamp(section.getLong(uuidString + ".lastNationalDuel", 0L));

                profiles.put(uuid, profile);
            } catch (IllegalArgumentException ignored) {
                // Corrupt entry ho to skip kar do, crash mat karo
            }
        }
    }

    private void persistProfile(PlayerRankProfile profile) {
        String path = "rankladder." + profile.getUuid();
        config.set(path + ".skill", profile.hasSkill() ? profile.getSkill().name() : "NONE");
        config.set(path + ".tier", profile.getTier().name());
        config.set(path + ".kills", profile.getKills());
        config.set(path + ".lastNationalDuel", profile.getLastNationalDuelTimestamp());
        plugin.saveConfig();
    }

    // ---------- Basic Access ----------

    // Naye player ke liye default Civillian profile ban jata hai (RAM mein), config mein tabhi likhta hai jab kuch change ho
    public PlayerRankProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, PlayerRankProfile::new);
    }

    public boolean hasSkill(UUID uuid) {
        return getProfile(uuid).hasSkill();
    }

    public RankTier getTier(UUID uuid) {
        return getProfile(uuid).getTier();
    }

    public Skill getSkill(UUID uuid) {
        return getProfile(uuid).getSkill();
    }

    // Pehli baar skill assign karna (sirf tab kaam karta hai jab abhi tak koi skill na ho)
    public boolean assignSkillIfAbsent(UUID uuid, Skill skill) {
        PlayerRankProfile profile = getProfile(uuid);
        if (profile.hasSkill()) return false;
        profile.setSkill(skill);
        persistProfile(profile);
        return true;
    }

    // ---------- Kills ----------

    public void addKill(UUID uuid) {
        PlayerRankProfile profile = getProfile(uuid);
        profile.addKill();
        persistProfile(profile);
    }

    public int getKills(UUID uuid) {
        return getProfile(uuid).getKills();
    }

    // Top N killers, sabse zyada kills pehle
    public List<PlayerRankProfile> getTopKillers(int n) {
        List<PlayerRankProfile> sorted = new ArrayList<>(profiles.values());
        sorted.sort((a, b) -> Integer.compare(b.getKills(), a.getKills()));
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    // ---------- Slot & Capacity Logic ----------

    private int countOccupants(Skill skill, RankTier tier, UUID exclude) {
        int count = 0;
        for (PlayerRankProfile p : profiles.values()) {
            if (p.getUuid().equals(exclude)) continue;
            if (p.getSkill() == skill && p.getTier() == tier) count++;
        }
        return count;
    }

    private PlayerRankProfile findLowestKillOccupant(Skill skill, RankTier tier, UUID exclude) {
        PlayerRankProfile lowest = null;
        for (PlayerRankProfile p : profiles.values()) {
            if (p.getUuid().equals(exclude)) continue;
            if (p.getSkill() != skill || p.getTier() != tier) continue;
            if (lowest == null || p.getKills() < lowest.getKills()) {
                lowest = p;
            }
        }
        return lowest;
    }

    /**
     * Player ko diye gaye skill+tier par place karna.
     * Agar tier full hai to sabse kam-kills wale occupant ko Civillian bana kar
     * uski jagah khali karayi jati hai (bumped player ka UUID return hota hai, warna null).
     */
    public UUID placeAtTier(UUID uuid, Skill skill, RankTier tier) {
        PlayerRankProfile profile = getProfile(uuid);
        UUID bumpedUUID = null;

        if (tier != RankTier.CIVILLIAN) {
            int capacity = tier.getSlotsPerSkill();
            int currentOccupants = countOccupants(skill, tier, uuid);

            if (currentOccupants >= capacity) {
                PlayerRankProfile toBump = findLowestKillOccupant(skill, tier, uuid);
                if (toBump != null) {
                    bumpedUUID = toBump.getUuid();
                    toBump.setTier(RankTier.CIVILLIAN);
                    persistProfile(toBump);
                }
            }
        }

        profile.setSkill(skill);
        profile.setTier(tier);
        persistProfile(profile);

        return bumpedUUID;
    }

    // ---------- Duel Resolution ----------

    /**
     * Ek resolved (fair, valid, minimum-time-pass) duel ka result apply karna.
     * winnerSkill/loserSkill caller (duel engine) detect karke bhejta hai.
     */
    public DuelResult resolveDuel(UUID winnerUUID, Skill winnerSkill, UUID loserUUID, Skill loserSkill) {
        PlayerRankProfile winnerProfile = getProfile(winnerUUID);
        PlayerRankProfile loserProfile = getProfile(loserUUID);

        RankTier winnerOldTier = winnerProfile.getTier();
        RankTier loserOldTier = loserProfile.getTier();

        List<UUID> bumpedPlayers = new ArrayList<>();

        if (winnerOldTier == loserOldTier) {
            // Same-tier fight -> winner promote, loser Civillian
            RankTier nextTier = winnerOldTier.next();

            UUID bumped = placeAtTier(winnerUUID, winnerSkill, nextTier);
            if (bumped != null) bumpedPlayers.add(bumped);

            placeAtTier(loserUUID, loserSkill, RankTier.CIVILLIAN);

            return new DuelResult(DuelResult.Type.PROMOTION, winnerUUID, loserUUID,
                    winnerOldTier, nextTier, loserOldTier, RankTier.CIVILLIAN, bumpedPlayers);

        } else {
            // Alag-tier fight -> dono ki ranks aapas mein swap
            UUID bumpedByWinner = placeAtTier(winnerUUID, winnerSkill, loserOldTier);
            UUID bumpedByLoser = placeAtTier(loserUUID, loserSkill, winnerOldTier);

            if (bumpedByWinner != null) bumpedPlayers.add(bumpedByWinner);
            if (bumpedByLoser != null) bumpedPlayers.add(bumpedByLoser);

            return new DuelResult(DuelResult.Type.FULL_SWAP, winnerUUID, loserUUID,
                    winnerOldTier, loserOldTier, loserOldTier, winnerOldTier, bumpedPlayers);
        }
    }
}
