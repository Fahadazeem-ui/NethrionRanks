package com.nethrion.ranks.pvp;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Persistent per-player PvP preference.
 *
 * Missing entries intentionally default to ON so an existing server does not
 * silently change its PvP behavior when this feature is first installed.
 */
public final class PvPManager {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public PvPManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pvp.yml");

        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null) parent.mkdirs();
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning(
                        "Could not create pvp.yml: " + ex.getMessage()
                );
            }
        }

        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isEnabled(UUID uuid) {
        if (uuid == null) return true;
        return data.getBoolean(
                "players." + uuid + ".enabled",
                true
        );
    }

    public boolean setEnabled(UUID uuid, boolean enabled) {
        if (uuid == null) return false;

        data.set(
                "players." + uuid + ".enabled",
                enabled
        );

        return save();
    }

    private boolean save() {
        try {
            data.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning(
                    "Could not save pvp.yml: " + ex.getMessage()
            );
            return false;
        }
    }
}
