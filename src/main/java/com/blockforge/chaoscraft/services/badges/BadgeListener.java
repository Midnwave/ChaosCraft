package com.blockforge.chaoscraft.services.badges;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.badges.functions.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

/**
 * Listener for the Badge System.
 * - Intercepts commands to check command-based badge functions.
 * - Runs a periodic task (every 1200 ticks / 60 seconds) to check
 *   PlaceholderAPI-based badge functions for all online players.
 */
public class BadgeListener implements Listener {

    private final ChaosCraftPlugin plugin;
    private final BadgeService badgeService;
    private BukkitTask periodicTask;

    public BadgeListener(ChaosCraftPlugin plugin, BadgeService badgeService) {
        this.plugin = plugin;
        this.badgeService = badgeService;
    }

    /**
     * Start the periodic placeholder check task.
     */
    public void startPeriodicTask() {
        periodicTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkPlaceholderBadges, 1200L, 1200L);
    }

    /**
     * Stop the periodic task.
     */
    public void stopPeriodicTask() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    // ========================
    // Command-Based Badges
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Strip leading slash
        String command = message.startsWith("/") ? message.substring(1) : message;

        // Check all badges with command functions
        for (Map.Entry<String, BadgeDefinition> entry : badgeService.getAllBadges().entrySet()) {
            BadgeDefinition badge = entry.getValue();
            BadgeFunction func = BadgeFunctionParser.parse(badge.getFunction());

            if (func instanceof CommandFunction cmdFunc) {
                if (command.equalsIgnoreCase(cmdFunc.getCommand()) || command.startsWith(cmdFunc.getCommand() + " ")) {
                    // Don't grant if player already has it
                    if (!badgeService.hasBadge(player.getUniqueId(), badge.getId())) {
                        badgeService.grantBadge(player, badge.getId());
                    }
                }
            }
        }
    }

    // ========================
    // Periodic Placeholder Check
    // ========================

    private void checkPlaceholderBadges() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        // Only check if PlaceholderAPI is available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return;

        for (Map.Entry<String, BadgeDefinition> entry : badgeService.getAllBadges().entrySet()) {
            BadgeDefinition badge = entry.getValue();
            BadgeFunction func = BadgeFunctionParser.parse(badge.getFunction());

            // Only check placeholder-based functions
            if (!(func instanceof PlaceholderAboveFunction) && !(func instanceof PlaceholderGTEFunction)) {
                continue;
            }

            // Skip expired limited badges
            if (badge.isExpired()) continue;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (badgeService.hasBadge(player.getUniqueId(), badge.getId())) continue;

                try {
                    if (func.shouldGrant(player, badge)) {
                        badgeService.grantBadge(player, badge.getId());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[Badges] Error checking placeholder badge " + badge.getId()
                            + " for " + player.getName() + ": " + e.getMessage());
                }
            }
        }
    }
}
