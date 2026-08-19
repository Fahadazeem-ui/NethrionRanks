package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.RankTier;
import com.nethrion.ranks.rank.Skill;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RankCommand implements CommandExecutor {

    private final RankLadderManager ladder;

    public RankCommand(RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player-only command.");
            return true;
        }

        if (args.length == 0) {
            sendProfile(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "list", "board" -> sendBoard(player);
            case "giveweapon" -> handleGiveWeapon(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendProfile(Player player) {
        PlayerRankProfile profile = ladder.getProfile(player.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "========== Nethrion Ranks ==========");

        if (profile.getTier() == RankTier.CIVILLIAN) {
            player.sendMessage(ChatColor.WHITE + "Rank: " + ChatColor.YELLOW + "Civillian");
        } else {
            player.sendMessage(ChatColor.WHITE + "Rank: " + ChatColor.GREEN +
                    profile.getTier().getDisplayName());
            player.sendMessage(ChatColor.WHITE + "Skill: " + ChatColor.AQUA +
                    profile.getSkill().getDisplayName());
            player.sendMessage(ChatColor.WHITE + "Level: " + ChatColor.AQUA +
                    profile.getSkillLevel(profile.getSkill()));
        }

        player.sendMessage(ChatColor.WHITE + "Kills: " + ChatColor.GREEN + profile.getKills());

        if (profile.isOutlaw()) {
            player.sendMessage(ChatColor.WHITE + "Outlaw penalty: " + ChatColor.RED +
                    "Level " + profile.getOutlawLevel());
        }

        player.sendMessage(ChatColor.GRAY + "Use /rank list for the ladder board.");
        player.sendMessage(ChatColor.GOLD + "====================================");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "===== NethrionRanks Help =====");
        player.sendMessage(ChatColor.YELLOW + "/rank" + ChatColor.GRAY + " - apni rank, skill, level aur kills");
        player.sendMessage(ChatColor.YELLOW + "/rank list" + ChatColor.GRAY + " - complete ranked ladder");
        player.sendMessage(ChatColor.YELLOW + "/rankduel <player>" + ChatColor.GRAY + " - ranked challenge");
        player.sendMessage(ChatColor.YELLOW + "/rankduel accept <player>" + ChatColor.GRAY + " - accept");
        player.sendMessage(ChatColor.YELLOW + "/skills" + ChatColor.GRAY + " - skill levels");
        player.sendMessage(ChatColor.YELLOW + "/skills board" + ChatColor.GRAY + " - skill board");
        player.sendMessage(ChatColor.YELLOW + "/killcount" + ChatColor.GRAY + " - your ranked duel kills");
        player.sendMessage(ChatColor.YELLOW + "/killtop" + ChatColor.GRAY + " - top 3 killers");
        player.sendMessage(ChatColor.YELLOW + "/base set" + ChatColor.GRAY + " - set base");
        player.sendMessage(ChatColor.YELLOW + "/base logs [30m|2h|1d]" + ChatColor.GRAY + " - base audit");
        if (player.hasPermission("nethrionranks.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/rank giveweapon spear [player]" +
                    ChatColor.GRAY + " - give tagged Iron-Hoe Spear");
        }
    }

    private void sendBoard(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== RANK LADDER ==========");

        for (Skill skill : Skill.values()) {
            player.sendMessage(ChatColor.AQUA + "• " + skill.getDisplayName());

            for (RankTier tier : RankTier.values()) {
                if (tier == RankTier.CIVILLIAN) continue;

                var occupants = ladder.getOccupants(skill, tier);
                StringBuilder names = new StringBuilder();

                for (PlayerRankProfile profile : occupants) {
                    if (!names.isEmpty()) names.append(", ");
                    Player online = Bukkit.getPlayer(profile.getUuid());
                    names.append(online != null ? online.getName() : profile.getUuid().toString().substring(0, 8));
                    names.append("[").append(profile.getKills()).append("]");
                }

                player.sendMessage(
                        ChatColor.GRAY + "  " + tier.getDisplayName() +
                                " " + ChatColor.DARK_GRAY + "(" +
                                tier.getSlotsPerSkill() + " slots): " +
                                ChatColor.WHITE +
                                (names.isEmpty() ? "—" : names)
                );
            }
        }

        player.sendMessage(ChatColor.GOLD + "=================================");
    }

    private void handleGiveWeapon(Player sender, String[] args) {
        if (!sender.hasPermission("nethrionranks.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("spear")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /rank giveweapon spear [player]");
            return;
        }

        Player target = sender;
        if (args.length >= 3) {
            Player selected = Bukkit.getPlayerExact(args[2]);
            if (selected == null) {
                sender.sendMessage(ChatColor.RED + "Player online nahi hai.");
                return;
            }
            target = selected;
        }

        ItemStack spear = com.nethrion.ranks.rank.WeaponUtil.createSpear();
        target.getInventory().addItem(spear);
        target.sendMessage(ChatColor.GOLD + "You received a " +
                ChatColor.WHITE + "Spear" + ChatColor.GOLD + ".");
        if (!target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Spear given to " + target.getName() + ".");
        }
    }
}
