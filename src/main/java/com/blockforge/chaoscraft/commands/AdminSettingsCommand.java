package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.settings.SettingsConfig;
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

import java.util.ArrayList;
import java.util.List;

/**
 * /cc settings &lt;setting&gt; &lt;option&gt; &lt;player&gt;
 * OR /cc settings &lt;enter|exit&gt; &lt;player&gt;
 * Administrative command to manage player settings.
 */
public class AdminSettingsCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public AdminSettingsCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.settings.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (plugin.getSettingsService() == null) {
            sender.sendMessage(Component.text("Settings service is not enabled!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            // List available settings
            sender.sendMessage(Component.text("=== Available Settings ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            for (SettingsConfig.Setting setting : plugin.getSettingsService().getConfig().getSettings()) {
                sender.sendMessage(Component.text(setting.getName() + " ", NamedTextColor.YELLOW)
                        .append(Component.text("- Options: " + String.join(", ", setting.getOptionNames()), NamedTextColor.GRAY)));
            }
            sender.sendMessage(Component.text("Usage: /cc settings <setting> <option> <player>", NamedTextColor.GRAY));
            return true;
        }

        String firstArg = args[0].toLowerCase();
        if (firstArg.equals("enter")) return handleEnter(sender, args);
        if (firstArg.equals("exit")) return handleExit(sender, args);

        // setting option player
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /cc settings <setting> <option> <player>", NamedTextColor.RED));
            return true;
        }

        String settingName = args[0];
        String optionName = args[1];
        String playerName = args[2];

        if (!plugin.getSettingsService().getConfig().hasSetting(settingName)) {
            sender.sendMessage(Component.text("Unknown setting: " + settingName, NamedTextColor.RED));
            sender.sendMessage(Component.text("Available settings: " +
                    String.join(", ", plugin.getSettingsService().getConfig().getSettingNames()), NamedTextColor.GRAY));
            return true;
        }

        SettingsConfig.Setting setting = plugin.getSettingsService().getConfig().getSetting(settingName);
        if (!setting.hasOption(optionName)) {
            sender.sendMessage(Component.text("Unknown option: " + optionName, NamedTextColor.RED));
            sender.sendMessage(Component.text("Available options for " + settingName + ": " +
                    String.join(", ", setting.getOptionNames()), NamedTextColor.GRAY));
            return true;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        boolean success = plugin.getSettingsService().setPlayerSetting(target, settingName, optionName);
        if (success) {
            sender.sendMessage(Component.text("Set " + target.getName() + "'s ", NamedTextColor.GREEN)
                    .append(Component.text(settingName, NamedTextColor.YELLOW))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(optionName, NamedTextColor.YELLOW)));
            target.sendMessage(Component.text("Your ", NamedTextColor.YELLOW)
                    .append(Component.text(settingName, NamedTextColor.GOLD))
                    .append(Component.text(" setting has been set to ", NamedTextColor.YELLOW))
                    .append(Component.text(optionName, NamedTextColor.GOLD)));
        } else {
            sender.sendMessage(Component.text("Failed to set setting!", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleEnter(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc settings enter <player>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("Forced " + target.getName() + " into settings menu.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleExit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc settings exit <player>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        target.closeInventory();
        sender.sendMessage(Component.text("Forced " + target.getName() + " out of settings menu.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (plugin.getSettingsService() == null) return List.of();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(plugin.getSettingsService().getConfig().getSettingNames());
            completions.add("enter");
            completions.add("exit");
            return completions.stream().filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2) {
            String first = args[0].toLowerCase();
            if (first.equals("enter") || first.equals("exit")) return null; // online player names

            SettingsConfig.Setting setting = plugin.getSettingsService().getConfig().getSetting(args[0]);
            if (setting != null) {
                return setting.getOptionNames().stream()
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }
        }
        if (args.length == 3) return null; // online player names
        return List.of();
    }
}
