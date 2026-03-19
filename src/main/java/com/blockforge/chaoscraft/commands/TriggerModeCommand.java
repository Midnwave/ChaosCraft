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

import java.util.ArrayList;
import java.util.List;

public class TriggerModeCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public TriggerModeCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /triggermode <mode>", NamedTextColor.RED));
            return true;
        }

        var manager = plugin.getModeManager();

        if (manager.isAnyModeActive()) {
            sender.sendMessage(Component.text("A mode is already active: " + manager.getActiveModeName(), NamedTextColor.RED));
            return true;
        }

        String modeName = args[0].toLowerCase();
        if (manager.getMode(modeName) == null) {
            sender.sendMessage(Component.text("Unknown mode: " + modeName, NamedTextColor.RED));
            return true;
        }

        boolean started = manager.startMode(modeName);
        if (started) {
            sender.sendMessage(Component.text("Mode triggered: " + modeName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to start mode: " + modeName, NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.mode.trigger")) return List.of();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String name : plugin.getModeManager().getModeNames()) {
                if (name.startsWith(partial)) {
                    matches.add(name);
                }
            }
            return matches;
        }
        return List.of();
    }
}
