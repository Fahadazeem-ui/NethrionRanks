package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.Skill;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillsCommand implements CommandExecutor {

    private final RankLadderManager ladder;

    public SkillsCommand(RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player-only command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("board")) {
            showBoard(player);
        } else {
            showSkills(player);
        }

        return true;
    }

    private void showSkills(Player player) {
        PlayerRankProfile profile = ladder.getProfile(player.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "========== SKILLS ==========");

        for (Skill skill : Skill.values()) {
            int level = profile.getSkillLevel(skill);
            long progress = profile.getSkillLevelProgressXp(skill);
            long required = profile.getSkillXpToNextLevel(skill);

            String marker =
                    profile.getSkill() == skill
                            ? ChatColor.GREEN + " ★"
                            : "";

            player.sendMessage(
                    ChatColor.AQUA + skill.getDisplayName() +
                            marker +
                            ChatColor.GRAY + " | Lv." +
                            ChatColor.GREEN + level +
                            ChatColor.GRAY + " | " +
                            progress + "/" + required
            );
        }

        player.sendMessage(ChatColor.GOLD + "============================");
    }

    private void showBoard(Player player) {
        PlayerRankProfile own = ladder.getProfile(player.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "======= YOUR SKILL BOARD =======");

        if (own.getSkill() == null) {
            player.sendMessage(ChatColor.YELLOW + "Permanent skill abhi first successful ranked duel se assign hogi.");
        } else {
            player.sendMessage(ChatColor.WHITE + "Permanent skill: " +
                    ChatColor.AQUA + own.getSkill().getDisplayName());
            player.sendMessage(ChatColor.WHITE + "Master level: " +
                    ChatColor.GREEN + own.getSkillLevel(own.getSkill()));
        }

        player.sendMessage(ChatColor.GRAY +
                "Skill rule: har ranked duel mein dominant weapon usage >=60% hona chahiye.");
        player.sendMessage(ChatColor.GRAY +
                "Locked skill se hat kar dominant skill aaye to match void hota hai.");

        player.sendMessage(ChatColor.GOLD + "================================");
    }
}
