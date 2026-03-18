package com.blockforge.chaoscraft.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * /cc itemtag -- Shows help text pointing to the standalone /itemtag command.
 */
public class ItemTagCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(Component.text("ItemTag Utilities:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Use the /itemtag command for item tag operations:", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /itemtag add <tag> - Add a tag to held item", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /itemtag remove <tag> - Remove a tag from held item", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /itemtag list - List all tags on held item", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  /itemtag use - Use held item as a custom item", NamedTextColor.GRAY));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
