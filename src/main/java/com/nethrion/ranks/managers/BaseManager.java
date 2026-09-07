package com.nethrion.ranks.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaseManager {

    public static final int MAX_DIMENSION = 50;
    public static final int MAX_SLOTS = 3;

    private final JavaPlugin plugin;
    private final FileConfiguration config;

    private final Map<UUID, Map<Integer, BaseDefinition>> bases =
            new HashMap<>();

    public BaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadBasesFromConfig();
    }

    private void loadBasesFromConfig() {
        ConfigurationSection section =
                config.getConfigurationSection(
                        "bases"
                );

        if (section == null) return;

        for (String ownerString :
                section.getKeys(false)) {

            try {
                UUID owner =
                        UUID.fromString(
                                ownerString
                        );

                Map<Integer, BaseDefinition> slots =
                        new HashMap<>();

                ConfigurationSection slotsSection =
                        section.getConfigurationSection(
                                ownerString +
                                        ".slots"
                        );

                if (slotsSection != null) {
                    for (
                            String slotString :
                            slotsSection.getKeys(false)
                    ) {
                        try {
                            int slot =
                                    Integer.parseInt(
                                            slotString
                                    );

                            String worldName =
                                    slotsSection.getString(
                                            slotString +
                                                    ".world"
                                    );

                            World world =
                                    Bukkit.getWorld(
                                            worldName
                                    );

                            if (world == null) continue;

                            Location center =
                                    new Location(
                                            world,
                                            slotsSection.getDouble(
                                                    slotString + ".x"
                                            ),
                                            slotsSection.getDouble(
                                                    slotString + ".y"
                                            ),
                                            slotsSection.getDouble(
                                                    slotString + ".z"
                                            )
                                    );

                            int width =
                                    Math.max(
                                            1,
                                            Math.min(
                                                    MAX_DIMENSION,
                                                    slotsSection.getInt(
                                                            slotString +
                                                                    ".width",
                                                            MAX_DIMENSION
                                                    )
                                            )
                                    );

                            int length =
                                    Math.max(
                                            1,
                                            Math.min(
                                                    MAX_DIMENSION,
                                                    slotsSection.getInt(
                                                            slotString +
                                                                    ".length",
                                                            MAX_DIMENSION
                                                    )
                                            )
                                    );

                            BaseDefinition definition =
                                    new BaseDefinition(
                                            owner,
                                            slot,
                                            center,
                                            width,
                                            length
                                    );

                            List<?> rawLogs =
                                    slotsSection.getList(
                                            slotString +
                                                    ".logs"
                                    );

                            if (rawLogs != null) {
                                for (Object raw :
                                        rawLogs) {
                                    if (raw instanceof Map<?, ?> map) {
                                        LogEntry log =
                                                LogEntry.fromMap(
                                                        map
                                                );

                                        if (log != null) {
                                            definition.logs.add(
                                                    log
                                            );
                                        }
                                    }
                                }
                            }

                            slots.put(
                                    slot,
                                    definition
                            );

                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                /*
                 * Migrate the old single-base format into slot 1
                 * so previous data is not silently lost.
                 */
                if (
                        slots.isEmpty() &&
                                section.isSet(
                                        ownerString + ".world"
                                )
                ) {
                    String worldName =
                            section.getString(
                                    ownerString + ".world"
                            );

                    World world =
                            Bukkit.getWorld(
                                    worldName
                            );

                    if (world != null) {
                        BaseDefinition migrated =
                                new BaseDefinition(
                                        owner,
                                        1,
                                        new Location(
                                                world,
                                                section.getDouble(
                                                        ownerString + ".x"
                                                ),
                                                section.getDouble(
                                                        ownerString + ".y"
                                                ),
                                                section.getDouble(
                                                        ownerString + ".z"
                                                )
                                        ),
                                        MAX_DIMENSION,
                                        MAX_DIMENSION
                                );

                        slots.put(
                                1,
                                migrated
                        );
                    }
                }

                bases.put(
                        owner,
                        slots
                );

            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public BaseDefinition setBase(
            Player player,
            int width,
            int length) {

        Map<Integer, BaseDefinition> slots =
                bases.computeIfAbsent(
                        player.getUniqueId(),
                        ignored -> new HashMap<>()
                );

        int slot = firstFreeSlot(slots);

        if (slot == -1) {
            return null;
        }

        BaseDefinition definition =
                new BaseDefinition(
                        player.getUniqueId(),
                        slot,
                        player.getLocation(),
                        width,
                        length
                );

        slots.put(
                slot,
                definition
        );

        persistOwner(
                player.getUniqueId()
        );

        return definition;
    }

    public BaseDefinition setBase(
            Player player,
            int slot,
            int width,
            int length) {

        if (
                slot < 1 ||
                        slot > MAX_SLOTS
        ) {
            return null;
        }

        Map<Integer, BaseDefinition> slots =
                bases.computeIfAbsent(
                        player.getUniqueId(),
                        ignored -> new HashMap<>()
                );

        if (
                slots.containsKey(slot)
        ) {
            return null;
        }

        BaseDefinition definition =
                new BaseDefinition(
                        player.getUniqueId(),
                        slot,
                        player.getLocation(),
                        width,
                        length
                );

        slots.put(
                slot,
                definition
        );

        persistOwner(
                player.getUniqueId()
        );

        return definition;
    }

    public boolean deleteBase(
            UUID owner,
            int slot) {

        Map<Integer, BaseDefinition> slots =
                bases.get(owner);

        if (slots == null) {
            return false;
        }

        if (slots.remove(slot) == null) {
            return false;
        }

        if (slots.isEmpty()) {
            bases.remove(owner);
        }

        persistOwner(owner);
        return true;
    }

    public boolean hasBase(
            UUID owner,
            int slot) {

        return bases.containsKey(owner) &&
                bases.get(owner)
                        .containsKey(slot);
    }

    public List<BaseDefinition> getBases(
            UUID owner) {

        Map<Integer, BaseDefinition> slots =
                bases.getOrDefault(
                        owner,
                        new HashMap<>()
                );

        List<BaseDefinition> list =
                new ArrayList<>(
                        slots.values()
                );

        list.sort(
                java.util.Comparator
                        .comparingInt(
                                BaseDefinition::slot
                        )
        );

        return list;
    }

    public BaseDefinition getBaseAt(
            UUID owner,
            int slot) {

        Map<Integer, BaseDefinition> slots =
                bases.get(owner);

        return slots == null
                ? null
                : slots.get(slot);
    }

    public BaseDefinition findBaseAt(
            Location location) {

        if (location == null) {
            return null;
        }

        for (
                Map<Integer, BaseDefinition> slots :
                bases.values()
        ) {
            for (
                    BaseDefinition base :
                    slots.values()
            ) {
                if (
                        base.contains(
                                location
                        )
                ) {
                    return base;
                }
            }
        }

        return null;
    }

    public UUID getBaseOwnerAt(
            Location location) {

        BaseDefinition base =
                findBaseAt(location);

        return base == null
                ? null
                : base.owner;
    }

    public void addLog(
            UUID owner,
            int slot,
            LogEntry entry) {

        BaseDefinition base =
                getBaseAt(
                        owner,
                        slot
                );

        if (base == null) {
            return;
        }

        base.logs.add(entry);
        persistOwner(owner);
    }

    public List<LogEntry> getLogs(
            UUID owner,
            Integer slot,
            Long sinceMillis) {

        BaseDefinition base =
                getBaseAt(
                        owner,
                        slot
                );

        if (base == null) {
            return List.of();
        }

        List<LogEntry> result =
                new ArrayList<>();

        long now =
                System.currentTimeMillis();

        for (LogEntry log :
                base.logs) {

            if (
                    sinceMillis == null ||
                            now - log.getTimestamp()
                                    <= sinceMillis
            ) {
                result.add(log);
            }
        }

        return result;
    }

    public static boolean validDimension(
            int value) {

        return value >= 1 &&
                value <= MAX_DIMENSION;
    }

    private int firstFreeSlot(
            Map<Integer, BaseDefinition> slots) {

        for (int slot = 1; slot <= MAX_SLOTS; slot++) {
            if (!slots.containsKey(slot)) {
                return slot;
            }
        }

        return -1;
    }

    private void persistOwner(
            UUID owner) {

        String root =
                "bases." +
                        owner;

        config.set(
                root,
                null
        );

        Map<Integer, BaseDefinition> slots =
                bases.get(owner);

        if (slots == null || slots.isEmpty()) {
            plugin.saveConfig();
            return;
        }

        for (
                BaseDefinition base :
                slots.values()
        ) {

            String path =
                    root +
                            ".slots." +
                            base.slot;

            config.set(
                    path + ".world",
                    base.center.getWorld()
                            .getName()
            );

            config.set(
                    path + ".x",
                    base.center.getX()
            );

            config.set(
                    path + ".y",
                    base.center.getY()
            );

            config.set(
                    path + ".z",
                    base.center.getZ()
            );

            config.set(
                    path + ".width",
                    base.width
            );

            config.set(
                    path + ".length",
                    base.length
            );

            List<Map<String, Object>> logs =
                    new ArrayList<>();

            for (LogEntry log :
                    base.logs) {
                logs.add(
                        log.toMap()
                );
            }

            config.set(
                    path + ".logs",
                    logs
            );
        }

        plugin.saveConfig();
    }

    public static final class BaseDefinition {

        private final int slot;
        private final Location center;
        private final int width;
        private final int length;
        private final UUID owner;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private final List<LogEntry> logs =
                new ArrayList<>();

        private BaseDefinition(
                UUID owner,
                int slot,
                Location center,
                int width,
                int length) {

            this.owner = owner;
            this.slot = slot;
            this.center =
                    center.clone();

            this.width =
                    Math.max(
                            1,
                            Math.min(
                                    MAX_DIMENSION,
                                    width
                            )
                    );

            this.length =
                    Math.max(
                            1,
                            Math.min(
                                    MAX_DIMENSION,
                                    length
                            )
                    );

            /*
             * Anchor the protected/audited area to the block the
             * player was standing in, not their exact fractional
             * coordinate (e.g. x=241.73). The old contains() check
             * compared that fractional coordinate straight against
             * whole-block locations, which silently shifted or
             * clipped up to a full block off the intended area.
             * That error is barely visible on a big square base
             * (10x10, 50x50...) but breaks small or rectangular
             * ones (8x8, 8x9, 5x9 — different width/length per
             * axis) right at the edges, which matches what was
             * being reported: base sets fine, edge blocks don't
             * detect. Computing an explicit, deterministic
             * min/max per axis guarantees exactly `width` blocks
             * on X and exactly `length` blocks on Z every time,
             * regardless of where inside the block the player was
             * standing when they ran /base set.
             */
            int centerBlockX =
                    this.center.getBlockX();

            int centerBlockZ =
                    this.center.getBlockZ();

            this.minX =
                    centerBlockX -
                            (this.width - 1) / 2;

            this.maxX =
                    this.minX +
                            this.width -
                            1;

            this.minZ =
                    centerBlockZ -
                            (this.length - 1) / 2;

            this.maxZ =
                    this.minZ +
                            this.length -
                            1;
        }

        public UUID owner() {
            return owner;
        }

        public int slot() {
            return slot;
        }

        public Location center() {
            return center.clone();
        }

        public int width() {
            return width;
        }

        public int length() {
            return length;
        }

        private boolean contains(
                Location location) {

            if (
                    location.getWorld() == null ||
                            center.getWorld() == null ||
                            !center.getWorld().equals(
                                    location.getWorld()
                            )
            ) {
                return false;
            }

            int blockX =
                    location.getBlockX();

            int blockZ =
                    location.getBlockZ();

            return blockX >= minX &&
                    blockX <= maxX &&
                    blockZ >= minZ &&
                    blockZ <= maxZ;
        }
    }
}
