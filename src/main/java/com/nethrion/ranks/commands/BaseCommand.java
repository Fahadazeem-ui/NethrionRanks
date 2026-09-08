package com.nethrion.ranks.commands;

import com.nethrion.ranks.managers.BaseManager;
import com.nethrion.ranks.managers.BaseManager.BaseDefinition;
import com.nethrion.ranks.managers.LogEntry;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BaseCommand
        implements CommandExecutor, TabCompleter {

    private final BaseManager baseManager;

    public BaseCommand(
            BaseManager baseManager) {
        this.baseManager = baseManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    "Player-only command."
            );
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub =
                args[0].toLowerCase(
                        Locale.ROOT
                );

        switch (sub) {
            case "set" ->
                    handleSet(player, args);

            case "delete" ->
                    handleDelete(player, args);

            case "list" ->
                    handleList(player);

            case "logs" ->
                    handleLogs(player, args);

            default ->
                    sendHelp(player);
        }

        return true;
    }

    private void handleSet(
            Player player,
            String[] args) {

        int width;
        int length;
        Integer requestedSlot = null;

        if (args.length == 2) {
            int[] dimensions =
                    parseDimensions(
                            args[1]
                    );

            if (dimensions == null) {
                player.sendMessage(
                        ChatColor.RED +
                                "Use: /base set 30x40"
                );
                return;
            }

            width = dimensions[0];
            length = dimensions[1];

        } else if (args.length >= 3) {

            try {
                requestedSlot =
                        Integer.parseInt(
                                args[1]
                        );
            } catch (NumberFormatException exception) {
                player.sendMessage(
                        ChatColor.RED +
                                "Slot must be 1, 2 or 3."
                );
                return;
            }

            int[] dimensions =
                    parseDimensions(
                            args[2]
                    );

            if (dimensions == null) {
                player.sendMessage(
                        ChatColor.RED +
                                "Use: /base set <slot> 30x40"
                );
                return;
            }

            width = dimensions[0];
            length = dimensions[1];

        } else {
            player.sendMessage(
                    ChatColor.YELLOW +
                            "Use: /base set 30x40"
            );
            return;
        }

        BaseDefinition base;

        if (requestedSlot == null) {
            base =
                    baseManager.setBase(
                            player,
                            width,
                            length
                    );
        } else {
            base =
                    baseManager.setBase(
                            player,
                            requestedSlot,
                            width,
                            length
                    );
        }

        if (base == null) {
            player.sendMessage(
                    ChatColor.RED +
                            "No free base slot. " +
                            "Delete one first with /base delete <1|2|3>."
            );
            return;
        }

        player.sendMessage(
                ChatColor.GREEN +
                        "Base slot " +
                        base.slot() +
                        " set at " +
                        base.width() +
                        "x" +
                        base.length() +
                        "."
        );

        player.sendMessage(
                ChatColor.GRAY +
                        "Protection/audit area spans full world height."
        );
    }

    private void handleDelete(
            Player player,
            String[] args) {

        if (args.length < 2) {
            player.sendMessage(
                    ChatColor.YELLOW +
                            "Use: /base delete <1|2|3>"
            );
            return;
        }

        int slot;

        try {
            slot =
                    Integer.parseInt(
                            args[1]
                    );
        } catch (NumberFormatException exception) {
            player.sendMessage(
                    ChatColor.RED +
                            "Slot must be 1, 2 or 3."
            );
            return;
        }

        if (
                baseManager.deleteBase(
                        player.getUniqueId(),
                        slot
                )
        ) {
            player.sendMessage(
                    ChatColor.GREEN +
                            "Base slot " +
                            slot +
                            " deleted."
            );
        } else {
            player.sendMessage(
                    ChatColor.RED +
                            "That base slot is empty."
            );
        }
    }

    private void handleList(
            Player player) {

        List<BaseDefinition> bases =
                baseManager.getBases(
                        player.getUniqueId()
                );

        player.sendMessage(
                ChatColor.GOLD +
                        "===== YOUR BASE SLOTS ====="
        );

        if (bases.isEmpty()) {
            player.sendMessage(
                    ChatColor.GRAY +
                            "No bases configured."
            );
        }

        for (BaseDefinition base :
                bases) {
            player.sendMessage(
                    ChatColor.YELLOW +
                            "#" +
                            base.slot() +
                            ChatColor.WHITE +
                            " " +
                            base.width() +
                            "x" +
                            base.length() +
                            ChatColor.GRAY +
                            " @ " +
                            base.center().getBlockX() +
                            ", " +
                            base.center().getBlockZ()
            );
        }

        player.sendMessage(
                ChatColor.GOLD +
                        "==========================="
        );
    }

    private void handleLogs(
            Player player,
            String[] args) {

        int slot = 1;
        Long sinceMillis = null;
        StringBuilder keywordBuilder = new StringBuilder();

        if (args.length >= 2) {
            try {
                slot =
                        Integer.parseInt(
                                args[1]
                        );
            } catch (NumberFormatException exception) {
                // Not a slot number - treat it as either a time token
                // or the start of a keyword (resolved below).
                Long parsedTime = parseTimeToMillis(args[1]);
                if (parsedTime != null) {
                    sinceMillis = parsedTime;
                } else {
                    keywordBuilder.append(args[1]);
                }
            }
        }

        // Every remaining token (from index 2 onward) is either the
        // time filter (30m/2h/1d) or part of the keyword - a keyword
        // does NOT need to be one exact word, so multiple trailing
        // tokens are joined back together with spaces.
        for (int i = 2; i < args.length; i++) {
            Long parsedTime = parseTimeToMillis(args[i]);
            if (parsedTime != null && sinceMillis == null) {
                sinceMillis = parsedTime;
                continue;
            }

            if (keywordBuilder.length() > 0) {
                keywordBuilder.append(' ');
            }
            keywordBuilder.append(args[i]);
        }

        String keyword =
                keywordBuilder.length() > 0
                        ? keywordBuilder.toString()
                        : null;

        if (
                !baseManager.hasBase(
                        player.getUniqueId(),
                        slot
                )
        ) {
            player.sendMessage(
                    ChatColor.RED +
                            "That base slot is empty."
            );
            return;
        }

        List<LogEntry> logs =
                baseManager.getLogs(
                        player.getUniqueId(),
                        slot,
                        sinceMillis,
                        keyword
                );

        if (logs.isEmpty()) {
            player.sendMessage(
                    ChatColor.GRAY +
                            "No activity recorded in that range."
            );
            return;
        }

        player.sendMessage(
                ChatColor.GOLD +
                        "===== BASE " +
                        slot +
                        " AUDIT" +
                        (
                                keyword == null
                                        ? ""
                                        : ChatColor.GRAY + " (keyword: " +
                                        ChatColor.WHITE + keyword +
                                        ChatColor.GOLD + ")"
                        ) +
                        " ====="
        );

        for (LogEntry entry :
                logs) {
            player.sendMessage(
                    ChatColor.GRAY +
                            entry.toDisplayString()
            );
        }

        player.sendMessage(
                ChatColor.GOLD +
                        "========================="
        );
    }

    private void sendHelp(
            Player player) {

        player.sendMessage(
                ChatColor.DARK_GRAY +
                        "──── " +
                        ChatColor.GOLD +
                        "Base" +
                        ChatColor.DARK_GRAY +
                        " ────"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                        "/base set 30x40" +
                        ChatColor.GRAY +
                        " — first free slot"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                        "/base set 2 30x40" +
                        ChatColor.GRAY +
                        " — exact slot"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                        "/base list" +
                        ChatColor.GRAY +
                        " — your three slots"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                        "/base logs 1 2h" +
                        ChatColor.GRAY +
                        " — activity audit"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                        "/base logs 1 iron" +
                        ChatColor.GRAY +
                        " — audit filtered by keyword"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                        "/base delete 1" +
                        ChatColor.GRAY +
                        " — free a slot"
        );
    }

    private int[] parseDimensions(
            String input) {

        String normalized =
                input.toLowerCase(
                        Locale.ROOT
                ).replace(
                        " ",
                        ""
                );

        String[] parts =
                normalized.split(
                        "x"
                );

        if (parts.length != 2) {
            return null;
        }

        try {
            int width =
                    Integer.parseInt(
                            parts[0]
                    );

            int length =
                    Integer.parseInt(
                            parts[1]
                    );

            if (
                    !BaseManager.validDimension(
                            width
                    ) ||
                    !BaseManager.validDimension(
                            length
                    )
            ) {
                return null;
            }

            return new int[] {
                    width,
                    length
            };

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long parseTimeToMillis(
            String input) {

        try {
            if (input == null || input.isEmpty()) {
                return null;
            }

            char unit =
                    input.charAt(
                            input.length() - 1
                    );

            long value =
                    Long.parseLong(
                            input.substring(
                                    0,
                                    input.length() - 1
                            )
                    );

            if (value <= 0) {
                return null;
            }

            return switch (unit) {
                case 'm' -> value * 60_000L;
                case 'h' -> value * 3_600_000L;
                case 'd' -> value * 86_400_000L;
                default -> null;
            };

        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {

        if (args.length == 1) {
            return filter(
                    List.of(
                            "set",
                            "list",
                            "logs",
                            "delete"
                    ),
                    args[0]
            );
        }

        if (
                args.length == 2 &&
                        (
                                args[0]
                                        .equalsIgnoreCase("logs") ||
                                args[0]
                                        .equalsIgnoreCase("delete")
                        )
        ) {
            return filter(
                    List.of(
                            "1",
                            "2",
                            "3"
                    ),
                    args[1]
            );
        }

        if (
                args.length == 3 &&
                        args[0].equalsIgnoreCase(
                                "logs"
                        )
        ) {
            return filter(
                    List.of(
                            "30m",
                            "2h",
                            "1d"
                    ),
                    args[2]
            );
        }

        if (
                args.length == 3 &&
                        args[0].equalsIgnoreCase(
                                "set"
                        )
        ) {
            return filter(
                    List.of(
                            "10x10",
                            "20x20",
                            "30x30",
                            "40x40",
                            "50x50"
                    ),
                    args[2]
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

        List<String> result =
                new ArrayList<>();

        for (String value :
                values) {
            if (
                    value.toLowerCase(
                            Locale.ROOT
                    ).startsWith(lower)
            ) {
                result.add(value);
            }
        }

        return result;
    }
}
