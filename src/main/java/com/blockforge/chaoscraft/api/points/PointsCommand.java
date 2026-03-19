package com.blockforge.chaoscraft.api.points;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /points command — check Mode Points balance, leaderboard, and debug toggle.
 *
 * /points           — Show your current session points
 * /points top       — Show top 10 players
 * /points debug     — Toggle debug mode (admin)
 * /points reload    — Reload points config (admin)
 */
public class PointsCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public PointsCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ModePointsService service = plugin.getModePointsService();
        if (service == null) {
            sender.sendMessage(Component.text("Mode Points system not initialized.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            return handleBalance(sender, service);
        }

        return switch (args[0].toLowerCase()) {
            case "top" -> handleTop(sender, service);
            case "debug" -> handleDebug(sender, service);
            case "reload" -> handleReload(sender, service);
            default -> {
                sender.sendMessage(Component.text("Usage: /points [top|debug|reload]", NamedTextColor.GRAY));
                yield true;
            }
        };
    }

    private boolean handleBalance(CommandSender sender, ModePointsService service) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }

        if (!service.isSessionActive()) {
            sender.sendMessage(Component.text("No mode is currently active. Points are per-session.", NamedTextColor.GRAY));
            return true;
        }

        int points = service.getPoints(player);
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("=== Mode Points ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Your Points: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(points), NamedTextColor.GREEN, TextDecoration.BOLD)));

        // Show next reward threshold
        var thresholds = service.getConfig().getRewardThresholds();
        Integer nextThreshold = thresholds.higherKey(points);
        if (nextThreshold != null) {
            int needed = nextThreshold - points;
            sender.sendMessage(Component.text("Next Reward: ", NamedTextColor.GRAY)
                    .append(Component.text(nextThreshold + " points", NamedTextColor.YELLOW))
                    .append(Component.text(" (" + needed + " more)", NamedTextColor.DARK_GRAY)));
        } else if (!thresholds.isEmpty()) {
            sender.sendMessage(Component.text("All rewards earned!", NamedTextColor.GREEN));
        }

        sender.sendMessage(Component.empty());
        return true;
    }

    private boolean handleTop(CommandSender sender, ModePointsService service) {
        if (!service.isSessionActive()) {
            sender.sendMessage(Component.text("No active session.", NamedTextColor.GRAY));
            return true;
        }

        var top = service.getTopPlayers(10);
        sender.sendMessage(Component.text("=== Mode Points Leaderboard ===", NamedTextColor.GOLD, TextDecoration.BOLD));

        if (top.isEmpty()) {
            sender.sendMessage(Component.text("No points earned yet.", NamedTextColor.GRAY));
            return true;
        }

        for (int i = 0; i < top.size(); i++) {
            var entry = top.get(i);
            var player = plugin.getServer().getPlayer(entry.getKey());
            String name = player != null ? player.getName() : entry.getKey().toString().substring(0, 8);
            NamedTextColor rankColor = i == 0 ? NamedTextColor.GOLD : i == 1 ? NamedTextColor.WHITE : i == 2 ? NamedTextColor.DARK_GRAY : NamedTextColor.GRAY;

            sender.sendMessage(Component.text("#" + (i + 1) + " ", rankColor, TextDecoration.BOLD)
                    .append(Component.text(name, NamedTextColor.AQUA))
                    .append(Component.text(" — " + entry.getValue() + " pts", NamedTextColor.GREEN)));
        }
        return true;
    }

    private boolean handleDebug(CommandSender sender, ModePointsService service) {
        if (!sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        boolean current = service.getConfig().isDebug();
        service.getConfig().setDebug(!current);
        String state = !current ? "ON" : "OFF";
        sender.sendMessage(Component.text("Mode Points debug: " + state,
                !current ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (!current) {
            sender.sendMessage(Component.text("  You will see point awards in chat as they happen.", NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender, ModePointsService service) {
        if (!sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        service.reload();
        sender.sendMessage(Component.text("Mode Points config reloaded.", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("top"));
            if (sender.hasPermission("chaoscraft.admin")) {
                subs.add("debug");
                subs.add("reload");
            }
            String prefix = args[0].toLowerCase();
            return subs.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
