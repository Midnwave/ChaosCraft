package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * /cc entertitlescreen &lt;player&gt; -- Force a player into the title screen.
 */
public class EnterTitleScreenCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public EnterTitleScreenCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.titlescreen")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /cc entertitlescreen <player>", NamedTextColor.RED));
            return true;
        }
        if (plugin.getTitleScreenService() == null) {
            sender.sendMessage(Component.text("Title screen service is not enabled!", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }
        if (plugin.getTitleScreenService().isInTitleScreen(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is already in title screen.", NamedTextColor.YELLOW));
            return true;
        }
        plugin.getTitleScreenService().startSession(target);
        sender.sendMessage(Component.text("Forced " + target.getName() + " to enter title screen.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return null; // online player names
        return List.of();
    }
}
