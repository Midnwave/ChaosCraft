package com.blockforge.chaoscraft.services.performance;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * /ccperformance command — manage the MythicMobs performance limiter at runtime.
 * <p>
 * Subcommands: enable, disable, toggle, reload, status, set, cleanup, debug
 */
public class PerformanceCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;
    private final PerformanceService service;

    private static final List<String> SUBCOMMANDS = List.of(
            "enable", "disable", "toggle", "reload", "status", "set", "cleanup", "debug");

    private static final List<String> SET_OPTIONS = List.of(
            "radius.max", "radius.blocks", "radius.enabled",
            "server.max", "server.enabled",
            "player.max", "player.radius", "player.enabled",
            "ping.threshold", "ping.radius", "ping.maxremove", "ping.cooldown", "ping.enabled");

    public PerformanceCommand(ChaosCraftPlugin plugin, PerformanceService service) {
        this.plugin = plugin;
        this.service = service;
    }

    // ---------------------------------------------------------------- command

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("chaoscraft.performance.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        if (!service.isMythicMobsAvailable()) {
            sender.sendMessage(Component.text(
                    "MythicMobs is not installed. Performance limiter is unavailable.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            showHelp(sender, label);
            return true;
        }

        var config = service.getConfig();
        var prefix = config.getMessagePrefix();

        switch (args[0].toLowerCase()) {
            case "enable" -> {
                service.setEnabled(true);
                sender.sendMessage(prefix + config.getMsgStatusEnabled());
            }
            case "disable" -> {
                service.setEnabled(false);
                sender.sendMessage(prefix + config.getMsgStatusDisabled());
            }
            case "toggle" -> {
                boolean nowEnabled = !service.isEnabled();
                service.setEnabled(nowEnabled);
                sender.sendMessage(prefix + (nowEnabled
                        ? config.getMsgStatusEnabled()
                        : config.getMsgStatusDisabled()));
            }
            case "reload" -> {
                service.reload();
                sender.sendMessage(prefix + config.getMsgConfigReloaded());
            }
            case "status" -> showStatus(sender, config);
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text(
                            "Usage: /" + label + " set <option> <value>", NamedTextColor.RED));
                    sender.sendMessage(Component.text(
                            "Options: " + String.join(", ", SET_OPTIONS), NamedTextColor.GRAY));
                    return true;
                }
                handleSet(sender, args[1], args[2], config, prefix);
            }
            case "cleanup" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text(
                            "This command can only be used by players.", NamedTextColor.RED));
                    return true;
                }
                int removed = service.cleanupAroundPlayer(player, config.getPingMaxRemovePerCycle());
                sender.sendMessage(Component.text(prefix)
                        .append(Component.text("Cleaned up " + removed + " mobs around you.", NamedTextColor.GREEN)));
            }
            case "debug" -> {
                boolean newDebug = !config.isDebug();
                config.setDebug(newDebug);
                sender.sendMessage(Component.text(prefix)
                        .append(Component.text("Debug mode: ", NamedTextColor.YELLOW))
                        .append(newDebug
                                ? Component.text("ON", NamedTextColor.GREEN)
                                : Component.text("OFF", NamedTextColor.RED)));
            }
            default -> showHelp(sender, label);
        }
        return true;
    }

    // ---------------------------------------------------------------- help

    private void showHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("=== ChaosCraft Performance Commands ===", NamedTextColor.GOLD));
        helpLine(sender, label, "enable", "Enable performance limiter");
        helpLine(sender, label, "disable", "Disable performance limiter");
        helpLine(sender, label, "toggle", "Toggle on/off");
        helpLine(sender, label, "reload", "Reload configuration");
        helpLine(sender, label, "status", "Show current status");
        helpLine(sender, label, "set <option> <value>", "Change a setting");
        helpLine(sender, label, "cleanup", "Force cleanup around you");
        helpLine(sender, label, "debug", "Toggle debug mode");
    }

    private void helpLine(CommandSender sender, String label, String sub, String desc) {
        sender.sendMessage(Component.text("/" + label + " " + sub, NamedTextColor.YELLOW)
                .append(Component.text(" - " + desc, NamedTextColor.GRAY)));
    }

    // ---------------------------------------------------------------- status

    private void showStatus(CommandSender sender, PerformanceConfig config) {
        sender.sendMessage(Component.text("=== Performance Limiter Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Enabled: ", NamedTextColor.YELLOW)
                .append(config.isEnabled()
                        ? Component.text("YES", NamedTextColor.GREEN)
                        : Component.text("NO", NamedTextColor.RED)));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Server Stats:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  Total MythicMobs: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(service.getServerMobCount()), NamedTextColor.WHITE)));

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Limits:", NamedTextColor.GOLD));

        // Radius
        sender.sendMessage(Component.text("  Radius Limit: ", NamedTextColor.YELLOW)
                .append(config.isRadiusLimitEnabled()
                        ? Component.text("ON", NamedTextColor.GREEN)
                        : Component.text("OFF", NamedTextColor.RED)));
        if (config.isRadiusLimitEnabled()) {
            sender.sendMessage(Component.text("    Max: " + config.getRadiusMaxMobs()
                    + " mobs in " + config.getRadiusBlocks() + " blocks", NamedTextColor.GRAY));
        }

        // Server
        sender.sendMessage(Component.text("  Server Limit: ", NamedTextColor.YELLOW)
                .append(config.isServerLimitEnabled()
                        ? Component.text("ON", NamedTextColor.GREEN)
                        : Component.text("OFF", NamedTextColor.RED)));
        if (config.isServerLimitEnabled()) {
            sender.sendMessage(Component.text("    Max: " + config.getServerMaxMobs() + " mobs", NamedTextColor.GRAY));
        }

        // Per-player
        sender.sendMessage(Component.text("  Per-Player Limit: ", NamedTextColor.YELLOW)
                .append(config.isPerPlayerLimitEnabled()
                        ? Component.text("ON", NamedTextColor.GREEN)
                        : Component.text("OFF", NamedTextColor.RED)));
        if (config.isPerPlayerLimitEnabled()) {
            sender.sendMessage(Component.text("    Max: " + config.getPerPlayerMaxMobs()
                    + " mobs in " + config.getPerPlayerRadiusBlocks() + " blocks", NamedTextColor.GRAY));
        }

        // Ping cleanup
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Ping Cleanup:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  Enabled: ", NamedTextColor.YELLOW)
                .append(config.isPingCleanupEnabled()
                        ? Component.text("ON", NamedTextColor.GREEN)
                        : Component.text("OFF", NamedTextColor.RED)));
        if (config.isPingCleanupEnabled()) {
            sender.sendMessage(Component.text("    Threshold: " + config.getPingThresholdMs() + "ms", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("    Radius: " + config.getPingCleanupRadius() + " blocks", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("    Max remove: " + config.getPingMaxRemovePerCycle() + " per cycle", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("    Cooldown: " + config.getPingCooldownSeconds() + "s", NamedTextColor.GRAY));
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Bypass mobs: ", NamedTextColor.YELLOW)
                .append(Component.text(String.valueOf(config.getBypassMobs().size()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Debug: ", NamedTextColor.YELLOW)
                .append(config.isDebug()
                        ? Component.text("ON", NamedTextColor.GREEN)
                        : Component.text("OFF", NamedTextColor.RED)));
    }

    // ---------------------------------------------------------------- set

    private void handleSet(CommandSender sender, String option, String value,
                           PerformanceConfig config, String prefix) {
        try {
            switch (option.toLowerCase()) {
                case "radius.max" -> {
                    config.setRadiusMaxMobs(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "radius.max", value, config);
                }
                case "radius.blocks" -> {
                    config.setRadiusBlocks(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "radius.blocks", value, config);
                }
                case "radius.enabled" -> {
                    config.setRadiusLimitEnabled(parseBoolean(value));
                    sendUpdated(sender, prefix, "radius.enabled", String.valueOf(parseBoolean(value)), config);
                }
                case "server.max" -> {
                    config.setServerMaxMobs(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "server.max", value, config);
                }
                case "server.enabled" -> {
                    config.setServerLimitEnabled(parseBoolean(value));
                    sendUpdated(sender, prefix, "server.enabled", String.valueOf(parseBoolean(value)), config);
                }
                case "player.max" -> {
                    config.setPerPlayerMaxMobs(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "player.max", value, config);
                }
                case "player.radius" -> {
                    config.setPerPlayerRadiusBlocks(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "player.radius", value, config);
                }
                case "player.enabled" -> {
                    config.setPerPlayerLimitEnabled(parseBoolean(value));
                    sendUpdated(sender, prefix, "player.enabled", String.valueOf(parseBoolean(value)), config);
                }
                case "ping.threshold" -> {
                    config.setPingThresholdMs(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "ping.threshold", value + "ms", config);
                }
                case "ping.radius" -> {
                    config.setPingCleanupRadius(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "ping.radius", value, config);
                }
                case "ping.maxremove" -> {
                    config.setPingMaxRemovePerCycle(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "ping.maxremove", value, config);
                }
                case "ping.cooldown" -> {
                    config.setPingCooldownSeconds(Integer.parseInt(value));
                    sendUpdated(sender, prefix, "ping.cooldown", value + "s", config);
                }
                case "ping.enabled" -> {
                    config.setPingCleanupEnabled(parseBoolean(value));
                    sendUpdated(sender, prefix, "ping.enabled", String.valueOf(parseBoolean(value)), config);
                }
                default -> {
                    sender.sendMessage(Component.text("Unknown option: " + option, NamedTextColor.RED));
                    sender.sendMessage(Component.text("Options: " + String.join(", ", SET_OPTIONS), NamedTextColor.GRAY));
                }
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + config.getMsgInvalidValue());
        }
    }

    private void sendUpdated(CommandSender sender, String prefix, String setting,
                             String value, PerformanceConfig config) {
        var msg = config.getMsgSettingUpdated()
                .replace("{setting}", setting)
                .replace("{value}", value);
        sender.sendMessage(prefix + msg);
    }

    private boolean parseBoolean(String value) {
        return value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("on")
                || value.equals("1");
    }

    // ---------------------------------------------------------------- tab completion

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.performance.admin")) return List.of();

        if (args.length == 1) {
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filterStartsWith(SET_OPTIONS, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            var opt = args[1].toLowerCase();
            if (opt.endsWith(".enabled")) {
                return List.of("true", "false");
            }
            return List.of("10", "25", "50", "100", "200", "500");
        }
        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        var lower = prefix.toLowerCase();
        var result = new ArrayList<String>();
        for (var opt : options) {
            if (opt.startsWith(lower)) result.add(opt);
        }
        return result;
    }
}
