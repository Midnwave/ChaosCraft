package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EndModeCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public EndModeCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) {
            sender.sendMessage(Component.text("No mode is currently active.", NamedTextColor.RED));
            return true;
        }

        String modeName = manager.getActiveModeName();
        manager.endActiveMode();
        sender.sendMessage(Component.text("Mode ended: " + modeName, NamedTextColor.GREEN));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
