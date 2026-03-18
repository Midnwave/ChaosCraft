package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * /cc useragreement accept &lt;player&gt; -- Mark a player as having accepted the user agreement.
 */
public class UserAgreementCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public UserAgreementCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.useragreement.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (plugin.getUserAgreementService() == null) {
            sender.sendMessage(Component.text("User Agreement service is not enabled!", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            return handleAccept(sender, args);
        }

        sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
        sendHelp(sender);
        return true;
    }

    private boolean handleAccept(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc useragreement accept <player>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        plugin.getUserAgreementService().acceptAgreement(target.getUniqueId());
        sender.sendMessage(Component.text("Marked " + target.getName() + " as having accepted the user agreement.", NamedTextColor.GREEN));
        plugin.getLogger().info(sender.getName() + " marked " + target.getName() + " as accepted user agreement");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== User Agreement Commands ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("/cc useragreement accept <player>", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("accept").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("accept")) return null; // online player names
        return List.of();
    }
}
