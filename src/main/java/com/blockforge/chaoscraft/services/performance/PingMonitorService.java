package com.blockforge.chaoscraft.services.performance;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically checks every online player's ping and triggers automatic
 * MythicMob cleanup when a player exceeds the configured threshold.
 */
public class PingMonitorService {

    private final ChaosCraftPlugin plugin;
    private PerformanceConfig config;
    private final MythicMobsLimiter mobsLimiter;

    private BukkitTask monitorTask;
    private boolean running = false;

    /** UUID -> epoch-millis of last cleanup for that player. */
    private final Map<UUID, Long> cleanupCooldowns = new ConcurrentHashMap<>();

    public PingMonitorService(ChaosCraftPlugin plugin, PerformanceConfig config, MythicMobsLimiter mobsLimiter) {
        this.plugin = plugin;
        this.config = config;
        this.mobsLimiter = mobsLimiter;
    }

    // ---------------------------------------------------------------- lifecycle

    public void start() {
        if (running || !config.isPingCleanupEnabled()) return;

        running = true;
        long interval = config.getPingCheckIntervalTicks();

        monitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (config.isEnabled() && config.isPingCleanupEnabled()) {
                    checkAllPlayers();
                }
            }
        }.runTaskTimer(plugin, interval, interval);

        if (config.isDebug()) {
            plugin.getLogger().info("[Performance] Ping monitor started (interval: "
                    + interval + " ticks, threshold: " + config.getPingThresholdMs() + "ms)");
        }
    }

    public void stop() {
        if (!running) return;

        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        running = false;
        cleanupCooldowns.clear();

        if (config.isDebug()) {
            plugin.getLogger().info("[Performance] Ping monitor stopped.");
        }
    }

    public void updateConfig(PerformanceConfig config) {
        this.config = config;
        if (running) {
            stop();
            start();
        }
    }

    // ---------------------------------------------------------------- checking

    private void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("chaoscraft.performance.bypass")) continue;

            int ping = player.getPing();
            if (ping < config.getPingThresholdMs()) continue;

            if (isOnCooldown(player.getUniqueId())) {
                if (config.isDebug()) {
                    plugin.getLogger().info("[Performance] Player " + player.getName()
                            + " has high ping (" + ping + "ms) but is on cleanup cooldown.");
                }
            } else {
                triggerCleanup(player, ping);
            }
        }
    }

    // ---------------------------------------------------------------- cooldown

    private boolean isOnCooldown(UUID playerId) {
        var lastCleanup = cleanupCooldowns.get(playerId);
        if (lastCleanup == null) return false;

        long cooldownMs = (long) config.getPingCooldownSeconds() * 1000L;
        return System.currentTimeMillis() - lastCleanup < cooldownMs;
    }

    /** Returns remaining cooldown in seconds, or 0 if none. */
    public int getRemainingCooldown(UUID playerId) {
        var lastCleanup = cleanupCooldowns.get(playerId);
        if (lastCleanup == null) return 0;

        long cooldownMs = (long) config.getPingCooldownSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - lastCleanup;
        return elapsed >= cooldownMs ? 0 : (int) ((cooldownMs - elapsed) / 1000L);
    }

    // ---------------------------------------------------------------- cleanup

    private void triggerCleanup(Player player, int ping) {
        if (config.isDebug()) {
            plugin.getLogger().info("[Performance] Triggering cleanup for " + player.getName()
                    + " (ping: " + ping + "ms, threshold: " + config.getPingThresholdMs() + "ms)");
        }

        // Send warning
        var warningMsg = config.getMessagePrefix()
                + config.getMsgPingCleanupWarning().replace("{ping}", String.valueOf(ping));
        player.sendMessage(warningMsg);

        // Remove mobs
        int removed = mobsLimiter.cleanupMobsAroundPlayer(
                player, config.getPingCleanupRadius(), config.getPingMaxRemovePerCycle());

        // Set cooldown
        cleanupCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        if (removed > 0) {
            var completeMsg = config.getMessagePrefix()
                    + config.getMsgPingCleanupComplete().replace("{count}", String.valueOf(removed));
            player.sendMessage(completeMsg);

            if (config.isDebug()) {
                plugin.getLogger().info("[Performance] Removed " + removed + " mobs around " + player.getName());
            }
        }
    }

    /**
     * Force-cleanup around a player regardless of ping, then set cooldown.
     *
     * @return number of mobs removed
     */
    public int forceCleanup(Player player) {
        int removed = mobsLimiter.cleanupMobsAroundPlayer(
                player, config.getPingCleanupRadius(), config.getPingMaxRemovePerCycle());
        cleanupCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        return removed;
    }

    public void clearCooldown(UUID playerId) {
        cleanupCooldowns.remove(playerId);
    }

    public void clearAllCooldowns() {
        cleanupCooldowns.clear();
    }

    public boolean isRunning() {
        return running;
    }
}
