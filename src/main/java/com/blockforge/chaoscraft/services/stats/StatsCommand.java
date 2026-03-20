package com.blockforge.chaoscraft.services.stats;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles all stats-related subcommands routed from ChaosCraftCommand.
 * Commands: kills, setkills, addkills, setskills, addskills, setsurvivals, addsurvivals
 */
public class StatsCommand {

    private final ChaosCraftPlugin plugin;
    private final PlayerStatsService statsService;

    public StatsCommand(ChaosCraftPlugin plugin, PlayerStatsService statsService) {
        this.plugin = plugin;
        this.statsService = statsService;
    }

    /**
     * Handle a stats subcommand.
     * @param sender Command sender
     * @param subCommand The subcommand name (kills, setkills, etc.)
     * @param args Remaining arguments after the subcommand
     */
    public boolean handle(CommandSender sender, String subCommand, String[] args) {
        return switch (subCommand.toLowerCase()) {
            case "kills" -> handleKills(sender, args);
            case "setkills" -> handleSetKills(sender, args);
            case "addkills" -> handleAddKills(sender, args);
            case "setskills" -> handleSetSKills(sender, args);
            case "addskills" -> handleAddSKills(sender, args);
            case "setsurvivals" -> handleSetSurvivals(sender, args);
            case "addsurvivals" -> handleAddSurvivals(sender, args);
            default -> false;
        };
    }

    private boolean handleKills(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Usage: /cc kills <player>", NamedTextColor.RED));
            return true;
        }

        PlayerStats stats = statsService.getStats(target.getUniqueId());
        sender.sendMessage(Component.empty()
                .append(Component.text("--- ", NamedTextColor.DARK_GRAY))
                .append(Component.text(target.getName() + "'s Stats", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" ---", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.text("  Kills: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.kills()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  S-Kills: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.sKills()), NamedTextColor.LIGHT_PURPLE)));
        sender.sendMessage(Component.text("  Mode Survivals: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.modeSurvivals()), NamedTextColor.GREEN)));
        return true;
    }

    private boolean handleSetKills(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc setkills <player> <amount>", NamedTextColor.RED));
            return true;
        }
        UUID uuid = resolvePlayer(sender, args[0]);
        if (uuid == null) return true;
        int amount = parseAmount(sender, args[1]);
        if (amount < 0) return true;

        statsService.setKills(uuid, amount);
        sender.sendMessage(Component.text("Set kills for " + args[0] + " to " + amount, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAddKills(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc addkills <player> <amount>", NamedTextColor.RED));
            return true;
        }
        UUID uuid = resolvePlayer(sender, args[0]);
        if (uuid == null) return true;
        int amount = parseAmount(sender, args[1]);
        if (amount < 0) return true;

        statsService.addKills(uuid, amount);
        sender.sendMessage(Component.text("Added " + amount + " kills to " + args[0], NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetSKills(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc setskills <player> <amount>", NamedTextColor.RED));
            return true;
        }
        UUID uuid = resolvePlayer(sender, args[0]);
        if (uuid == null) return true;
        int amount = parseAmount(sender, args[1]);
        if (amount < 0) return true;

        statsService.setSKills(uuid, amount);
        sender.sendMessage(Component.text("Set s-kills for " + args[0] + " to " + amount, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAddSKills(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc addskills <player> <amount>", NamedTextColor.RED));
            return true;
        }
        UUID uuid = resolvePlayer(sender, args[0]);
        if (uuid == null) return true;
        int amount = parseAmount(sender, args[1]);
        if (amount < 0) return true;

        statsService.addSKills(uuid, amount);
        sender.sendMessage(Component.text("Added " + amount + " s-kills to " + args[0], NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetSurvivals(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc setsurvivals <player> <amount>", NamedTextColor.RED));
            return true;
        }
        UUID uuid = resolvePlayer(sender, args[0]);
        if (uuid == null) return true;
        int amount = parseAmount(sender, args[1]);
        if (amount < 0) return true;

        statsService.setModeSurvivals(uuid, amount);
        sender.sendMessage(Component.text("Set mode survivals for " + args[0] + " to " + amount, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAddSurvivals(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc addsurvivals <player> <amount>", NamedTextColor.RED));
            return true;
        }
        UUID uuid = resolvePlayer(sender, args[0]);
        if (uuid == null) return true;
        int amount = parseAmount(sender, args[1]);
        if (amount < 0) return true;

        statsService.addModeSurvivals(uuid, amount);
        sender.sendMessage(Component.text("Added " + amount + " mode survivals to " + args[0], NamedTextColor.GREEN));
        return true;
    }

    private UUID resolvePlayer(CommandSender sender, String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) return online.getUniqueId();

        // Try offline player
        @SuppressWarnings("deprecation")
        var offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline.getUniqueId();
        }

        sender.sendMessage(Component.text("Player not found: " + name, NamedTextColor.RED));
        return null;
    }

    private int parseAmount(CommandSender sender, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            if (amount < 0) {
                sender.sendMessage(Component.text("Amount must be non-negative.", NamedTextColor.RED));
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number: " + amountStr, NamedTextColor.RED));
            return -1;
        }
    }
}
