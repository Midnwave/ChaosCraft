package com.blockforge.chaoscraft.services.performance;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * Top-level orchestrator for the performance sub-system.
 * Manages the {@link PerformanceConfig}, {@link MythicMobsLimiter},
 * and {@link PingMonitorService}.
 */
public class PerformanceService {

    private final ChaosCraftPlugin plugin;
    private PerformanceConfig config;
    private MythicMobsLimiter mobsLimiter;
    private PingMonitorService pingMonitor;
    private boolean mythicMobsAvailable = false;

    public PerformanceService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- lifecycle

    public void initialize() {
        loadConfig();

        mythicMobsAvailable = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
        if (!mythicMobsAvailable) {
            plugin.getLogger().warning("MythicMobs not found! Performance limiter will be disabled.");
            return;
        }

        mobsLimiter = new MythicMobsLimiter(plugin, config);
        mobsLimiter.register();

        pingMonitor = new PingMonitorService(plugin, config, mobsLimiter);
        pingMonitor.start();

        plugin.getLogger().info("Performance service initialized.");
        if (config.isEnabled()) {
            plugin.getLogger().info("  - MythicMobs limiter: ACTIVE");
            plugin.getLogger().info("  - Radius limit: " + (config.isRadiusLimitEnabled()
                    ? config.getRadiusMaxMobs() + " mobs in " + config.getRadiusBlocks() + " blocks"
                    : "DISABLED"));
            plugin.getLogger().info("  - Server limit: " + (config.isServerLimitEnabled()
                    ? config.getServerMaxMobs() + " mobs"
                    : "DISABLED"));
            plugin.getLogger().info("  - Per-player limit: " + (config.isPerPlayerLimitEnabled()
                    ? config.getPerPlayerMaxMobs() + " mobs in " + config.getPerPlayerRadiusBlocks() + " blocks"
                    : "DISABLED"));
            plugin.getLogger().info("  - Ping cleanup: " + (config.isPingCleanupEnabled()
                    ? ">" + config.getPingThresholdMs() + "ms threshold"
                    : "DISABLED"));
        } else {
            plugin.getLogger().info("  - Performance limiter is DISABLED in config");
        }
    }

    public void shutdown() {
        if (pingMonitor != null) {
            pingMonitor.stop();
        }
        if (mobsLimiter != null) {
            mobsLimiter.unregister();
        }
    }

    // ---------------------------------------------------------------- config

    public void loadConfig() {
        var file = new File(plugin.getDataFolder(), "performance.yml");
        if (!file.exists()) {
            plugin.saveResource("performance.yml", false);
        }
        var yaml = YamlConfiguration.loadConfiguration(file);

        if (config == null) {
            config = new PerformanceConfig(yaml);
        } else {
            config.load(yaml);
        }
    }

    public void reload() {
        loadConfig();
        if (mobsLimiter != null) {
            mobsLimiter.updateConfig(config);
        }
        if (pingMonitor != null) {
            pingMonitor.updateConfig(config);
        }
        plugin.getLogger().info("Performance configuration reloaded.");
    }

    // ---------------------------------------------------------------- enable / disable

    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        if (enabled) {
            if (mobsLimiter != null) mobsLimiter.register();
            if (pingMonitor != null) pingMonitor.start();
        } else {
            if (mobsLimiter != null) mobsLimiter.unregister();
            if (pingMonitor != null) pingMonitor.stop();
        }
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    // ---------------------------------------------------------------- delegates

    public boolean isMythicMobsAvailable() {
        return mythicMobsAvailable;
    }

    public PerformanceConfig getConfig() {
        return config;
    }

    public MythicMobsLimiter getMobsLimiter() {
        return mobsLimiter;
    }

    public PingMonitorService getPingMonitor() {
        return pingMonitor;
    }

    public int getServerMobCount() {
        return mobsLimiter != null ? mobsLimiter.getServerMobCount() : 0;
    }

    public int getMobCountInRadius(Location location, int radius) {
        return mobsLimiter != null ? mobsLimiter.getMobCountInRadius(location, radius) : 0;
    }

    public int cleanupAroundPlayer(Player player, int maxRemove) {
        return mobsLimiter != null
                ? mobsLimiter.cleanupMobsAroundPlayer(player, config.getPingCleanupRadius(), maxRemove)
                : 0;
    }
}
