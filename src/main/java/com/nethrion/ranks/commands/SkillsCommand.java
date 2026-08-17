package com.nethrion.ranks.commands;

import com.nethrion.ranks.managers.RankManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillsCommand implements CommandExecutor {

    private final RankManager rankManager;

    // Abhi ke liye 3 basic skills — aage jaake aur bhi add ho sakti hain
    private final String[] skillList = { "Mining", "Combat", "Farming" };

    public SkillsCommand(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Yeh command sirf player use kar sakta hai.");
            return true;
        }

        Player player = (Player) sender;

        player.sendMessage(ChatColor.GOLD + "===== " + ChatColor.YELLOW + "Tumhare Skills" + ChatColor.GOLD + " =====");

        for (String skill : skillList) {
            int level = rankManager.getSkillLevel(player.getUniqueId(), skill);
            int xp = rankManager.getSkillXP(player.getUniqueId(), skill);
            int xpNeeded = level * 100;

            player.sendMessage(ChatColor.AQUA + skill + ChatColor.GRAY + ": Level " +
                    ChatColor.GREEN + level + ChatColor.GRAY + " (" + xp + "/" + xpNeeded + " XP)");
        }

        return true;
    }
}
