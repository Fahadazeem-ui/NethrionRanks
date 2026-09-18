package com.nethrion.ranks.managers;

import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.miner.NationalMinerManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Persistent, OP-approved requests for a player to leave their current rank.
 * A request never stores a rank to restore; approval always resets whatever
 * rank is current at approval time.
 */
public final class RankExitRequestManager {
    private final JavaPlugin plugin;
    private final RankLadderManager ladder;
    private final NationalMinerManager miner;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, Long> pending = new LinkedHashMap<>();

    public RankExitRequestManager(JavaPlugin plugin, RankLadderManager ladder, NationalMinerManager miner) {
        this.plugin = plugin;
        this.ladder = ladder;
        this.miner = miner;
        this.file = new File(plugin.getDataFolder(), "rank_requests.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        pending.clear();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long created = config.getLong(key + ".created", 0L);
                if (created > 0L) pending.put(uuid, created);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void save() {
        for (String key : new HashSet<>(config.getKeys(false))) {
            config.set(key, null);
        }
        for (Map.Entry<UUID, Long> e : pending.entrySet()) {
            String path = e.getKey().toString();
            config.set(path + ".created", e.getValue());
        }
        try { config.save(file); } catch (IOException ex) {
            plugin.getLogger().warning("Could not save rank_requests.yml: " + ex.getMessage());
        }
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public boolean request(UUID uuid) {
        if (uuid == null || hasPending(uuid)) return false;
        PlayerRankProfile profile = ladder.getProfile(uuid);
        if (!miner.isNationalMiner(uuid) &&
                (!profile.getTier().isRanked() || profile.getSkill() == null)) return false;
        pending.put(uuid, System.currentTimeMillis());
        save();

        Player player = Bukkit.getPlayer(uuid);
        String name = player != null ? player.getName() : Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString());
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (op.isOp()) {
                op.sendMessage("§6[NethrionRanks] §eRank exit request: §f" + name
                        + " §7(currently §f" + profile.getDisplayPrefix() + "§7). Use §e/requests§7.");
            }
        }
        return true;
    }

    public Set<UUID> getPending() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(pending.keySet()));
    }

    public boolean approve(UUID uuid) {
        if (!pending.containsKey(uuid)) return false;
        // The request is only an intent to leave. Resolve the LIVE current state.
        if (miner.isNationalMiner(uuid)) {
            miner.leave(uuid);
            ladder.adminResetToCivilian(uuid);
        } else {
            ladder.adminResetToCivilian(uuid);
        }
        pending.remove(uuid);
        save();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage("§aYour rank-exit request was approved. You are now §7Civillian§a.");
        }
        return true;
    }

    public boolean cancel(UUID uuid) {
        if (pending.remove(uuid) == null) return false;
        save();
        return true;
    }

    public long createdAt(UUID uuid) {
        return pending.getOrDefault(uuid, 0L);
    }

    private static final class ConfigurationSectionAdapter {
        static void forEachTopLevel(FileConfiguration cfg, java.util.function.Consumer<String> c) {
            for (String key : cfg.getKeys(false)) c.accept(key);
        }
    }
}
