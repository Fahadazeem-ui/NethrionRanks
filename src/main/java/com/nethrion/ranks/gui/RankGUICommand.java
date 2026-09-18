package com.nethrion.ranks.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /rankgui [player]
 *
 * Opens the NethrionRanks GUI dashboard.
 * OP can open it for another player.
 */
public class RankGUICommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            // Self-open
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MM.deserialize("<color:#FF4444>Console cannot open a GUI.</color>"));
                return true;
            }
            RankGUI.openDashboard(player);
            return true;
        }

        // OP can open for another player
        if (!sender.isOp()) {
            sender.sendMessage(MM.deserialize(
                    "<color:#FF4444>Only operators can open the GUI for another player.</color>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(MM.deserialize(
                    "<color:#FF4444>Player </color><color:#FFFFFF>" + args[0] +
                    "</color><color:#FF4444> is not online.</color>"));
            return true;
        }

        RankGUI.openDashboard(target);
        sender.sendMessage(MM.deserialize(
                "<color:#44FF88>Opened GUI for </color><color:#FFFFFF>" + target.getName() + "</color>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1 && sender.isOp()) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
