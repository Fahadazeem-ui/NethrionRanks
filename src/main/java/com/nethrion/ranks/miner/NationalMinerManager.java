package com.nethrion.ranks.miner;

import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.WeaponUtil;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Independent multi-holder NationalMiner role.
 *
 * Lifetime mining totals are tracked for all players. Applying for the role
 * requires the configured minimum total. The role has no combat rank/tier and
 * is deliberately independent of Rank Duel.
 */
public final class NationalMinerManager implements Listener {
    public static final int MINIMUM_BLOCKS = 10_000;
    private static final NamespacedKey MINER_ROLE_KEY =
            new NamespacedKey("nethrionranks", "national_miner");
    private static final NamespacedKey MINER_TOTAL_KEY =
            new NamespacedKey("nethrionranks", "miner_blocks");

    private final JavaPlugin plugin;
    private final RankLadderManager ladder;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, Long> totals = new HashMap<>();
    private final Set<UUID> miners = new HashSet<>();

    public NationalMinerManager(JavaPlugin plugin, RankLadderManager ladder) {
        this.plugin = plugin;
        this.ladder = ladder;
        this.file = new File(plugin.getDataFolder(), "national_miner.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        totals.clear();
        miners.clear();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                totals.put(uuid, config.getLong(key + ".blocks", 0L));
                if (config.getBoolean(key + ".miner", false)) miners.add(uuid);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public synchronized void save() {
        for (String key : new HashSet<>(config.getKeys(false))) config.set(key, null);
        Set<UUID> all = new HashSet<>(totals.keySet());
        all.addAll(miners);
        for (UUID uuid : all) {
            String p = uuid.toString();
            config.set(p + ".blocks", totals.getOrDefault(uuid, 0L));
            config.set(p + ".miner", miners.contains(uuid));
        }
        try { config.save(file); } catch (IOException ex) {
            plugin.getLogger().warning("Could not save national_miner.yml: " + ex.getMessage());
        }
    }

    public long getBlocks(UUID uuid) { return totals.getOrDefault(uuid, 0L); }
    public boolean isNationalMiner(UUID uuid) { return miners.contains(uuid); }

    public synchronized boolean apply(Player player) {
        UUID uuid = player.getUniqueId();
        if (miners.contains(uuid)) {
            player.sendMessage("§eYou are already a NationalMiner.");
            return false;
        }
        long total = getBlocks(uuid);
        if (total < MINIMUM_BLOCKS) {
            player.sendMessage("§cYou need at least §e" + MINIMUM_BLOCKS +
                    " §cvalid lifetime mined blocks. You currently have §e" + total + "§c.");
            return false;
        }
        // NationalMiner is a dedicated role. A player cannot retain a combat
        // rank while holding it, so the active combat rank is retired here.
        ladder.adminResetToCivilian(uuid);
        miners.add(uuid);
        save();
        ensureWeapon(uuid);
        player.sendMessage("§aYou are now §b§lNationalMiner§a.");
        return true;
    }

    public synchronized boolean leave(UUID uuid) {
        if (!miners.remove(uuid)) return false;
        removeWeapon(uuid);
        save();
        return true;
    }

    public void ensureWeapon(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !isNationalMiner(uuid)) return;
        removeWeapon(uuid);
        player.getInventory().addItem(createWeapon());
    }

    public void removeWeapon(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isMinerWeapon(item)) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    public ItemStack createWeapon() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(gradientName("NationalMiner"));
        meta.setLore(List.of(
                "§8Nethrion National Tool",
                "§7Role: §bNationalMiner",
                "§cLocked to NationalMiner holders"
        ));
        meta.getPersistentDataContainer().set(MINER_ROLE_KEY, PersistentDataType.BYTE, (byte)1);
        meta.getPersistentDataContainer().set(WeaponUtil.NATIONAL_WEAPON_KEY, PersistentDataType.BYTE, (byte)1);
        meta.getPersistentDataContainer().set(MINER_TOTAL_KEY, PersistentDataType.STRING, "NATIONAL_MINER");
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.FORTUNE, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isMinerWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !WeaponUtil.isNationalWeapon(item)) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(MINER_ROLE_KEY, PersistentDataType.BYTE);
    }

    private Component gradientName(String text) {
        TextColor start = TextColor.color(74, 238, 255);
        TextColor end = TextColor.color(122, 85, 255);
        Component result = Component.empty();
        int len = Math.max(1, text.length());
        for (int i = 0; i < len; i++) {
            double t = len == 1 ? 0 : (double)i / (len - 1);
            int r = (int)Math.round(start.red() + (end.red()-start.red())*t);
            int g = (int)Math.round(start.green() + (end.green()-start.green())*t);
            int b = (int)Math.round(start.blue() + (end.blue()-start.blue())*t);
            result = result.append(Component.text(text.charAt(i))
                    .color(TextColor.color(r,g,b))
                    .decorate(TextDecoration.BOLD));
        }
        return result;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long next = totals.getOrDefault(uuid, 0L) + 1L;
        totals.put(uuid, next);

        // Avoid a disk write on every block while still checkpointing often.
        if (next % 50L == 0L) save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (isNationalMiner(uuid)) {
                ensureWeapon(uuid);
            } else {
                removeWeapon(uuid);
            }
        });
    }
}
