package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.Skill;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class SkillsCommand
        implements CommandExecutor, TabCompleter {

    private final RankLadderManager ladder;

    public SkillsCommand(
            RankLadderManager ladder) {
        this.ladder = ladder;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        ladder.enforceNationalInactivity();

        if (
                args.length > 0 &&
                        args[0].equalsIgnoreCase(
                                "board"
                        )
        ) {
            showBoard(sender);
        } else if (
                sender instanceof Player player
        ) {
            showSkills(player);
        } else {
            sender.sendMessage(
                    "Use /skills board."
            );
        }

        return true;
    }

    private void showSkills(
            Player player) {

        PlayerRankProfile profile =
                ladder.getProfile(
                        player.getUniqueId()
                );

        player.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════ " +
                        ChatColor.GOLD +
                        " YOUR COMBAT SKILL " +
                        ChatColor.DARK_GRAY +
                        "════════"
        );

        if (
                profile.getSkill() == null
        ) {
            player.sendMessage(
                    ChatColor.YELLOW +
                            "No combat skill yet."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "Your first successful formal " +
                            "/rankduel will permanently assign your skill."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "The plugin decides it from clear weapon dominance."
            );
        } else {
            Skill skill =
                    profile.getSkill();

            player.sendMessage(
                    ChatColor.WHITE +
                            "Skill: " +
                            ChatColor.AQUA +
                            skill.getMasterTitle()
            );

            player.sendMessage(
                    ChatColor.WHITE +
                            "Level: " +
                            ChatColor.GREEN +
                            profile.getSkillLevel(skill)
            );

            player.sendMessage(
                    ChatColor.WHITE +
                            "XP: " +
                            ChatColor.GREEN +
                            profile.getSkillLevelProgressXp(skill) +
                            ChatColor.GRAY +
                            "/" +
                            profile.getSkillXpToNextLevel(skill)
            );
        }

        player.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════════════════════════════"
        );
    }

    private void showBoard(
            CommandSender sender) {

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════ " +
                        ChatColor.GOLD +
                        " SKILL LEADERS " +
                        ChatColor.DARK_GRAY +
                        "════════"
        );

        for (Skill skill : Skill.values()) {

            PlayerRankProfile top =
                    ladder.getTopPlayerForSkill(
                            skill
                    );

            if (top == null) {
                sender.sendMessage(
                        ChatColor.AQUA +
                                skill.getMasterTitle() +
                                ChatColor.GRAY +
                                " — Unclaimed"
                );
                continue;
            }

            Player online =
                    Bukkit.getPlayer(
                            top.getUuid()
                    );

            String name =
                    online != null
                            ? online.getName()
                            : Bukkit.getOfflinePlayer(top.getUuid()).getName();

            if (name == null || name.isBlank()) {
                name = top.getUuid().toString().substring(0, 8);
            }

            sender.sendMessage(
                    ChatColor.AQUA +
                            skill.getMasterTitle() +
                            ChatColor.GRAY +
                            " — " +
                            ChatColor.WHITE +
                            name +
                            ChatColor.DARK_GRAY +
                            " | " +
                            ChatColor.GREEN +
                            top.getTier().getDisplayName() +
                            ChatColor.DARK_GRAY +
                            " | " +
                            ChatColor.YELLOW +
                            top.getKills() +
                            " kills"
            );
        }

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════════════════════════════"
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {

        if (args.length == 1) {
            String token =
                    args[0].toLowerCase(
                            Locale.ROOT
                    );

            if ("board".startsWith(token)) {
                return List.of("board");
            }
        }

        return List.of();
    }
}
