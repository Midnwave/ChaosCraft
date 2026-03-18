package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * /codes -- Enters code-entry mode for the player (standalone command).
 */
public class CodesCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public CodesCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (plugin.getCodesService() == null) {
            sender.sendMessage(Component.text("Codes service is not enabled!", NamedTextColor.RED));
            return true;
        }
        if (plugin.getCodesService().isInSession(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a code entry session!", NamedTextColor.YELLOW));
            return true;
        }

        plugin.getCodesService().startSession(player.getUniqueId());
        player.sendMessage(Component.text("=== Code Entry Mode ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Type a code in chat to redeem it!", NamedTextColor.GRAY));
        plugin.getLogger().info(player.getName() + " entered code entry mode");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
