package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.Skill;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
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
                args.length > 0 &&
                        args[0].equalsIgnoreCase("list")
        ) {
            showSkillNameList(sender);
        } else if (
                args.length > 0 &&
                        args[0].equalsIgnoreCase("top")
        ) {
            showTop(sender);
        } else if (
                sender instanceof Player player
        ) {
            showSkills(player);
        } else {
            sender.sendMessage(
                    "Use /skills board, /skills list, or /skills top."
            );
        }

        return true;
    }

    /**
     * /skills list — just the skill names themselves, bold + a neon
     * color, no rank-holder names attached (per spec: "sirf skills
     * name show honge skil holder names nhi").
     */
    private void showSkillNameList(
            CommandSender sender) {

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════ " +
                        ChatColor.GOLD +
                        " SKILLS " +
                        ChatColor.DARK_GRAY +
                        "════════"
        );

        for (Skill skill : Skill.values()) {
            sender.sendMessage(
                    ChatColor.of("#39FF14") +
                            "" +
                            ChatColor.BOLD +
                            skill.getMasterTitle()
            );
        }

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════════════════════════════"
        );
    }

    /**
     * /skills top — the highest ranker(s) of each skill. A skill with
     * no rank holder at all is skipped entirely (per spec: "agar kisi
     * skill ka koi ranker he hi nhi to vo /skills top men show hi
     * nhi hogi"). If several players are tied for #1 (same tier AND
     * same kill count), every tied name is listed together.
     */
    private void showTop(
            CommandSender sender) {

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════ " +
                        ChatColor.GOLD +
                        " SKILLS TOP " +
                        ChatColor.DARK_GRAY +
                        "════════"
        );

        boolean anyShown = false;

        for (Skill skill : Skill.values()) {
            List<PlayerRankProfile> top =
                    ladder.getTopPlayersForSkill(skill);

            if (top.isEmpty()) {
                // Deliberately not shown at all - no ranker exists yet.
                continue;
            }

            anyShown = true;

            StringBuilder names = new StringBuilder();
            for (int i = 0; i < top.size(); i++) {
                PlayerRankProfile profile = top.get(i);

                String name =
                        Bukkit.getOfflinePlayer(profile.getUuid()).getName();
                if (name == null) name = "Unknown";

                if (i > 0) names.append(ChatColor.GRAY).append(", ");

                names.append(ChatColor.WHITE)
                        .append(name)
                        .append(ChatColor.GRAY)
                        .append(" (")
                        .append(profile.getTier().getDisplayName())
                        .append(" rank)");
            }

            sender.sendMessage(
                    ChatColor.AQUA +
                            skill.getMasterTitle() +
                            ChatColor.DARK_GRAY +
                            " - " +
                            names
            );
        }

        if (!anyShown) {
            sender.sendMessage(
                    ChatColor.GRAY +
                            "No skill has a ranker yet."
            );
        }

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════════════════════════════"
        );
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
                            : "Offline";

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

            List<String> options =
                    java.util.List.of("board", "list", "top");

            List<String> matches = new java.util.ArrayList<>();
            for (String option : options) {
                if (option.startsWith(token)) {
                    matches.add(option);
                }
            }
            return matches;
        }

        return List.of();
    }
}
