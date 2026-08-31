package com.nethrion.ranks.commands;

import com.nethrion.ranks.rank.PlayerRankProfile;
import com.nethrion.ranks.rank.RankLadderManager;
import com.nethrion.ranks.rank.RankTier;
import com.nethrion.ranks.rank.Skill;
import com.nethrion.ranks.rank.WeaponUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RankCommand
        implements CommandExecutor, TabCompleter {

    private final RankLadderManager ladder;

    public RankCommand(
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

        if (args.length == 0) {
            if (sender instanceof Player player) {
                sendProfile(player);
            } else {
                sender.sendMessage(
                        "Use /rank help."
                );
            }
            return true;
        }

        switch (
                args[0].toLowerCase(Locale.ROOT)
        ) {
            case "help" -> sendHelp(sender);

            case "list", "board" ->
                    sendBoard(sender);

            case "set" ->
                    handleSetRank(sender, args);

            case "giveweapon" ->
                    handleGiveWeapon(sender, args);

            case "removeweapon" ->
                    handleRemoveWeapon(sender, args);

            default ->
                    sendHelp(sender);
        }

        return true;
    }

    private void sendProfile(
            Player player) {

        PlayerRankProfile profile =
                ladder.getProfile(
                        player.getUniqueId()
                );

        ChatColor color =
                tierColor(
                        profile.getTier()
                );

        player.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════ " +
                        ChatColor.GOLD +
                        " NETHRION RANKS " +
                        ChatColor.DARK_GRAY +
                        "════════"
        );

        player.sendMessage(
                ChatColor.WHITE +
                        "Status: " +
                        color +
                        profile.getDisplayPrefix()
        );

        if (
                profile.getSkill() != null
        ) {
            Skill skill =
                    profile.getSkill();

            player.sendMessage(
                    ChatColor.WHITE +
                            "Mastery: " +
                            ChatColor.AQUA +
                            skill.getMasterTitle() +
                            ChatColor.GRAY +
                            " Lv." +
                            ChatColor.GREEN +
                            profile.getSkillLevel(skill)
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "XP: " +
                            profile.getSkillLevelProgressXp(skill) +
                            "/" +
                            profile.getSkillXpToNextLevel(skill)
            );
        } else {
            player.sendMessage(
                    ChatColor.GRAY +
                            "Skill: " +
                            ChatColor.YELLOW +
                            "Unclaimed"
            );
        }

        player.sendMessage(
                ChatColor.WHITE +
                        "Ranked kills: " +
                        ChatColor.GREEN +
                        profile.getKills()
        );

        if (
                profile.isOutlaw()
        ) {
            player.sendMessage(
                    ChatColor.RED +
                            "Outlaw penalty active: " +
                            ChatColor.YELLOW +
                            profile.getOutlawLevel()
            );
        }

        if (
                profile.getTier() ==
                        RankTier.NATIONAL
        ) {
            player.sendMessage(
                    ChatColor.GOLD +
                            "National activity cooldown: " +
                            ChatColor.WHITE +
                            ladder.formatCooldown(
                                    ladder.getRemainingNationalCooldownMillis(
                                            player.getUniqueId()
                                    )
                            )
            );
        }

        player.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════════════════════════════"
        );
    }

    private void sendHelp(
            CommandSender sender) {

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "──── " +
                        ChatColor.GOLD +
                        "NethrionRanks" +
                        ChatColor.DARK_GRAY +
                        " ────"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/rank" +
                        ChatColor.GRAY +
                        " — apni rank aur mastery"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/rank list" +
                        ChatColor.GRAY +
                        " — complete ladder"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/rankduel <player>" +
                        ChatColor.GRAY +
                        " — formal ranked challenge"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/rankduel accept <player>" +
                        ChatColor.GRAY +
                        " — accept challenge"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/skills" +
                        ChatColor.GRAY +
                        " — apni combat mastery"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/skills board" +
                        ChatColor.GRAY +
                        " — har skill ka #1"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/killcount" +
                        ChatColor.GRAY +
                        " — apne ranked kills"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/killtop" +
                        ChatColor.GRAY +
                        " — top 3 ranked killers"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/base set <width>x<length>" +
                        ChatColor.GRAY +
                        " — base slot set karo"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/base logs [30m|2h|1d]" +
                        ChatColor.GRAY +
                        " — activity audit"
        );

        sender.sendMessage(
                ChatColor.YELLOW +
                        "/base delete <1|2|3>" +
                        ChatColor.GRAY +
                        " — base slot free karo"
        );

        if (
                sender.hasPermission(
                        "nethrionranks.admin"
                )
        ) {
            sender.sendMessage(
                    ChatColor.RED +
                            "ADMIN:"
            );

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "/rank set <player> <tier> [skill]"
            );

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "/rank giveweapon <player> <skill> <tier>"
            );

            sender.sendMessage(
                    ChatColor.YELLOW +
                            "/rank removeweapon <player> [skill]"
            );
        }

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "──────────────────────────────"
        );
    }

    private void sendBoard(
            CommandSender sender) {

        ladder.enforceNationalInactivity();

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "════════ " +
                        ChatColor.GOLD +
                        " RANK LADDER " +
                        ChatColor.DARK_GRAY +
                        "════════"
        );

        for (Skill skill : Skill.values()) {
            sender.sendMessage(
                    ChatColor.AQUA +
                            skill.getMasterTitle()
            );

            for (
                    RankTier tier :
                    RankTier.values()
            ) {
                if (
                        tier == RankTier.CIVILLIAN
                ) {
                    continue;
                }

                List<PlayerRankProfile> occupants =
                        ladder.getOccupants(
                                skill,
                                tier
                        );

                StringBuilder line =
                        new StringBuilder();

                for (
                        PlayerRankProfile profile :
                        occupants
                ) {
                    if (
                            !line.isEmpty()
                    ) {
                        line.append(
                                ChatColor.GRAY +
                                        ", "
                        );
                    }

                    Player online =
                            Bukkit.getPlayer(
                                    profile.getUuid()
                            );

                    String name =
                            online != null
                                    ? online.getName()
                                    : Bukkit.getOfflinePlayer(profile.getUuid()).getName();

                    if (name == null || name.isBlank()) {
                        name = profile.getUuid().toString().substring(0, 8);
                    }

                    line.append(
                            tierColor(tier)
                    )
                    .append(name)
                    .append(
                            ChatColor.DARK_GRAY
                    )
                    .append("(")
                    .append(
                            ChatColor.GREEN
                    )
                    .append(
                            profile.getKills()
                    )
                    .append(
                            ChatColor.DARK_GRAY
                    )
                    .append(")");
                }

                if (line.isEmpty()) {
                    line.append(
                            ChatColor.DARK_GRAY +
                                    "—"
                    );
                }

                sender.sendMessage(
                        ChatColor.GRAY +
                                "  " +
                                tierColor(tier) +
                                tier.getDisplayName() +
                                ChatColor.DARK_GRAY +
                                " [" +
                                tier.getSlotsPerSkill() +
                                "] " +
                                ChatColor.WHITE +
                                line
                );
            }
        }

        sender.sendMessage(
                ChatColor.DARK_GRAY +
                        "══════════════════════════════"
        );
    }

    private void handleSetRank(
            CommandSender sender,
            String[] args) {

        if (
                !sender.hasPermission(
                        "nethrionranks.admin"
                )
        ) {
            sender.sendMessage(
                    ChatColor.RED +
                            "No permission."
            );
            return;
        }

        if (
                args.length < 3
        ) {
            sender.sendMessage(
                    ChatColor.YELLOW +
                            "Usage: /rank set <player> <tier> [skill]"
            );
            return;
        }

        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );

        if (target == null) {
            sender.sendMessage(
                    ChatColor.RED +
                            "Player online nahi hai."
            );
            return;
        }

        RankTier tier;

        try {
            tier =
                    RankTier.valueOf(
                            args[2].toUpperCase(
                                    Locale.ROOT
                            )
                    );
        } catch (Exception exception) {
            sender.sendMessage(
                    ChatColor.RED +
                            "Unknown tier."
            );
            return;
        }

        Skill skill =
                ladder.getSkill(
                        target.getUniqueId()
                );

        if (args.length >= 4) {
            try {
                skill =
                        Skill.valueOf(
                                args[3].toUpperCase(
                                        Locale.ROOT
                                )
                        );
            } catch (Exception exception) {
                sender.sendMessage(
                        ChatColor.RED +
                                "Unknown skill."
                );
                return;
            }
        }

        if (
                tier != RankTier.CIVILLIAN &&
                        skill == null
        ) {
            skill = Skill.SWORD;
        }

        PlayerRankProfile profile =
                ladder.getProfile(
                        target.getUniqueId()
                );

        profile.setSkill(skill);
        profile.setTier(tier);

        if (
                tier ==
                        RankTier.NATIONAL
        ) {
            profile.setLastNationalDuelTimestamp(
                    System.currentTimeMillis()
            );
            ladder.ensureNationalWeapon(
                    target.getUniqueId(),
                    skill
            );
        }

        /*
         * We intentionally persist through the public state path by
         * changing a harmless XP value only when required. The ladder
         * manager's persistence is private, so use its admin setter.
         */
        ladder.adminPersistProfile(profile);

        ladder.refreshOnlineDisplay(
                target.getUniqueId()
        );

        sender.sendMessage(
                ChatColor.GREEN +
                        target.getName() +
                        " set to " +
                        tier.getDisplayName() +
                        " " +
                        (
                                skill == null
                                        ? ""
                                        : skill.getMasterTitle()
                        )
        );
    }

    private void handleGiveWeapon(
            CommandSender sender,
            String[] args) {

        if (
                !sender.hasPermission(
                        "nethrionranks.admin"
                )
        ) {
            sender.sendMessage(
                    ChatColor.RED +
                            "No permission."
            );
            return;
        }

        if (
                args.length < 4
        ) {
            sender.sendMessage(
                    ChatColor.YELLOW +
                            "Usage: /rank giveweapon <player> <skill> <tier>"
            );
            return;
        }

        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );

        if (target == null) {
            sender.sendMessage(
                    ChatColor.RED +
                            "Player online nahi hai."
            );
            return;
        }

        Skill skill;

        RankTier tier;

        try {
            skill =
                    Skill.valueOf(
                            args[2].toUpperCase(
                                    Locale.ROOT
                            )
                    );

            tier =
                    RankTier.valueOf(
                            args[3].toUpperCase(
                                    Locale.ROOT
                            )
                    );

        } catch (Exception exception) {
            sender.sendMessage(
                    ChatColor.RED +
                            "Invalid skill or tier."
            );
            return;
        }

        List<ItemStack> weapons =
                WeaponUtil.createRankWeaponSet(
                        skill,
                        tier
                );

        for (ItemStack weapon : weapons) {
            target.getInventory().addItem(
                    weapon
            );
        }

        target.sendMessage(
                ChatColor.GOLD +
                        "Admin test weapon received: " +
                        ChatColor.WHITE +
                        tier.getDisplayName() +
                        " " +
                        skill.getMasterTitle() +
                        (
                                weapons.size() > 1
                                        ? ChatColor.GRAY +
                                        " (Spear + Mace)"
                                        : ""
                        )
        );

        sender.sendMessage(
                ChatColor.GREEN +
                        "Weapon given to " +
                        target.getName() +
                        "."
        );
    }

    private void handleRemoveWeapon(
            CommandSender sender,
            String[] args) {

        if (
                !sender.hasPermission(
                        "nethrionranks.admin"
                )
        ) {
            sender.sendMessage(
                    ChatColor.RED +
                            "No permission."
            );
            return;
        }

        if (
                args.length < 2
        ) {
            sender.sendMessage(
                    ChatColor.YELLOW +
                            "Usage: /rank removeweapon <player> [skill]"
            );
            return;
        }

        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );

        if (target == null) {
            sender.sendMessage(
                    ChatColor.RED +
                            "Player online nahi hai."
            );
            return;
        }

        Skill skill = null;

        if (args.length >= 3) {
            try {
                skill =
                        Skill.valueOf(
                                args[2].toUpperCase(
                                        Locale.ROOT
                                )
                        );
            } catch (Exception exception) {
                sender.sendMessage(
                        ChatColor.RED +
                                "Invalid skill."
                );
                return;
            }
        }

        ladder.removeNationalWeapon(
                target.getUniqueId(),
                skill
        );

        String scope =
                skill == null
                        ? "all National weapons"
                        : "National " +
                        skill.getMasterTitle();

        target.sendMessage(
                ChatColor.GOLD +
                        "Your " +
                        scope +
                        " were removed by an admin."
        );

        sender.sendMessage(
                ChatColor.GREEN +
                        "Removed " +
                        scope +
                        " from " +
                        target.getName() +
                        "."
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {

        if (args.length == 1) {
            List<String> values =
                    new ArrayList<>(
                            Arrays.asList(
                                    "help",
                                    "list"
                            )
                    );

            if (
                    sender.hasPermission(
                            "nethrionranks.admin"
                    )
            ) {
                values.add("set");
                values.add("giveweapon");
                values.add("removeweapon");
            }

            return filter(
                    values,
                    args[0]
            );
        }

        if (
                args.length == 2 &&
                        (
                                args[0]
                                        .equalsIgnoreCase("set") ||
                                        args[0]
                                                .equalsIgnoreCase(
                                                        "giveweapon"
                                                ) ||
                                        args[0]
                                                .equalsIgnoreCase(
                                                        "removeweapon"
                                                )
                        )
        ) {
            List<String> names =
                    new ArrayList<>();

            for (Player player :
                    Bukkit.getOnlinePlayers()) {
                names.add(
                        player.getName()
                );
            }

            return filter(
                    names,
                    args[1]
            );
        }

        if (
                args.length == 3 &&
                        args[0]
                                .equalsIgnoreCase("set")
        ) {
            List<String> tiers =
                    new ArrayList<>();

            for (RankTier tier :
                    RankTier.values()) {
                tiers.add(
                        tier.getDisplayName()
                );
            }

            return filter(
                    tiers,
                    args[2]
            );
        }

        if (
                args.length == 3 &&
                        (
                                args[0]
                                        .equalsIgnoreCase("giveweapon") ||
                                        args[0]
                                                .equalsIgnoreCase("removeweapon")
                        )
        ) {
            List<String> skills =
                    new ArrayList<>();

            for (Skill skill :
                    Skill.values()) {
                skills.add(
                        skill.name()
                );
            }

            return filter(
                    skills,
                    args[2]
            );
        }

        if (
                args.length == 4 &&
                        args[0]
                                .equalsIgnoreCase("giveweapon")
        ) {
            List<String> tiers =
                    new ArrayList<>();

            for (RankTier tier :
                    RankTier.values()) {
                tiers.add(
                        tier.name()
                );
            }

            return filter(
                    tiers,
                    args[3]
            );
        }

        return List.of();
    }

    private List<String> filter(
            List<String> values,
            String token) {

        String lower =
                token.toLowerCase(
                        Locale.ROOT
                );

        return values.stream()
                .filter(
                        value ->
                                value.toLowerCase(
                                        Locale.ROOT
                                ).startsWith(lower)
                )
                .toList();
    }

    private ChatColor tierColor(
            RankTier tier) {

        return switch (tier) {
            case NATIONAL -> ChatColor.GOLD;
            case S -> ChatColor.DARK_RED;
            case A -> ChatColor.RED;
            case B -> ChatColor.DARK_PURPLE;
            case C -> ChatColor.LIGHT_PURPLE;
            case D -> ChatColor.BLUE;
            case E -> ChatColor.AQUA;
            case CIVILLIAN -> ChatColor.GRAY;
        };
    }
}
