package com.blockforge.chaoscraft.api.points;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main service for Mode Points. Manages config, tracker, and provides the public API.
 * Initialized once in ChaosCraftPlugin, shared across all modes.
 */
public class ModePointsService {

    private final ChaosCraftPlugin plugin;
    private final ModePointsConfig config;
    private final ModePointsTracker tracker;

    public ModePointsService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.config = new ModePointsConfig(plugin);
        this.tracker = new ModePointsTracker(plugin, config);
    }

    public void initialize() {
        config.load();
        plugin.getLogger().info("[ModePoints] Mode Points system initialized.");
    }

    public void reload() {
        config.load();
    }

    // ========================
    // Session management (called by ModeManager)
    // ========================

    public void startSession() {
        tracker.startSession();
        plugin.getLogger().info("[ModePoints] Session started.");
    }

    public void endSession() {
        // Log final scores
        if (tracker.isActive()) {
            plugin.getLogger().info("[ModePoints] Session ended. Total points awarded: " + tracker.getTotalPointsAwarded());
            List<Map.Entry<UUID, Integer>> top = tracker.getTopPlayers(5);
            for (int i = 0; i < top.size(); i++) {
                var entry = top.get(i);
                var player = plugin.getServer().getPlayer(entry.getKey());
                String name = player != null ? player.getName() : entry.getKey().toString();
                plugin.getLogger().info("[ModePoints] #" + (i + 1) + " " + name + ": " + entry.getValue() + " points");
            }
        }
        tracker.endSession();
    }

    // ========================
    // Point awarding API
    // ========================

    /**
     * Award points for a specific action.
     */
    public void award(Player player, PointAction action) {
        tracker.awardPoints(player, action);
    }

    /**
     * Award points with a custom amount.
     */
    public void award(Player player, PointAction action, int customPoints) {
        tracker.awardPoints(player, action, customPoints);
    }

    // ========================
    // Query API
    // ========================

    public int getPoints(Player player) {
        return tracker.getPoints(player);
    }

    public int getPoints(UUID player) {
        return tracker.getPoints(player);
    }

    public List<Map.Entry<UUID, Integer>> getTopPlayers(int limit) {
        return tracker.getTopPlayers(limit);
    }

    public boolean isSessionActive() {
        return tracker.isActive();
    }

    // ========================
    // Accessors
    // ========================

    public ModePointsConfig getConfig() { return config; }
    public ModePointsTracker getTracker() { return tracker; }
}
