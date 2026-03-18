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
 * /cc play &lt;player&gt; -- Trigger the play script for a player (admin command).
 */
public class PlayCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public PlayCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.play.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (plugin.getPlayService() == null) {
            sender.sendMessage(Component.text("Play service is not enabled!", NamedTextColor.RED));
            return true;
        }
        if (plugin.getUserAgreementService() == null) {
            sender.sendMessage(Component.text("User Agreement service is not enabled!", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /cc play <player>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        boolean hasAccepted = plugin.getUserAgreementService().hasAccepted(target.getUniqueId());
        plugin.getPlayService().executePlayScript(target, hasAccepted);

        if (hasAccepted) {
            plugin.getPlayService().setPlaying(target.getUniqueId());
            sender.sendMessage(Component.text("Executed play script for " + target.getName() + " (agreement accepted)", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Executed play script for " + target.getName() + " (agreement NOT accepted)", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Use: /cc useragreement accept " + target.getName(), NamedTextColor.GRAY));
        }

        plugin.getLogger().info(sender.getName() + " executed play command for " + target.getName() + " (accepted=" + hasAccepted + ")");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return null; // online player names
        return List.of();
    }
}
