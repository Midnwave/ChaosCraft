package com.blockforge.chaoscraft.services.stats;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

import java.util.UUID;

/**
 * Public API for player stats (kills, s-kills, mode survivals).
 * All stats are lifetime counters — never spent or reduced by the system.
 */
public class PlayerStatsService {

    private final ChaosCraftPlugin plugin;
    private PlayerStatsStorage storage;
    private StatsConfig config;

    public PlayerStatsService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        storage = new PlayerStatsStorage(plugin);
        storage.initialize();

        config = new StatsConfig(plugin);
        config.load();

        plugin.getLogger().info("[Stats] Player stats service initialized.");
    }

    public void reload() {
        if (config != null) config.load();
    }

    public void shutdown() {
        if (storage != null) storage.shutdown();
    }

    // ========================
    // Kills
    // ========================

    public int getKills(UUID uuid) {
        return storage.getStats(uuid).kills();
    }

    public void addKills(UUID uuid, int amount) {
        storage.addKills(uuid, amount);
    }

    public void setKills(UUID uuid, int amount) {
        storage.setKills(uuid, amount);
    }

    /**
     * Get the kill value for a given entity type name.
     * Checks MythicMobs overrides, then entity-type config, then defaults.
     */
    public int getKillValue(String entityType) {
        return config.getKillValue(entityType);
    }

    /**
     * Get the kill value for a MythicMobs mob by internal name.
     */
    public int getMythicMobKillValue(String mythicMobId) {
        return config.getMythicMobKillValue(mythicMobId);
    }

    // ========================
    // S-Kills
    // ========================

    public int getSKills(UUID uuid) {
        return storage.getStats(uuid).sKills();
    }

    public void addSKills(UUID uuid, int amount) {
        storage.addSKills(uuid, amount);
    }

    public void setSKills(UUID uuid, int amount) {
        storage.setSKills(uuid, amount);
    }

    // ========================
    // Mode Survivals
    // ========================

    public int getModeSurvivals(UUID uuid) {
        return storage.getStats(uuid).modeSurvivals();
    }

    public void addModeSurvivals(UUID uuid, int amount) {
        storage.addModeSurvivals(uuid, amount);
    }

    public void setModeSurvivals(UUID uuid, int amount) {
        storage.setModeSurvivals(uuid, amount);
    }

    public void incrementModeSurvivals(UUID uuid) {
        storage.addModeSurvivals(uuid, 1);
    }

    // ========================
    // Full stats
    // ========================

    public PlayerStats getStats(UUID uuid) {
        return storage.getStats(uuid);
    }

    /**
     * Check if a player meets all stat requirements.
     */
    public boolean meetsRequirements(UUID uuid, int requiredKills, int requiredSKills, int requiredSurvivals) {
        return storage.getStats(uuid).meetsRequirements(requiredKills, requiredSKills, requiredSurvivals);
    }

    public StatsConfig getConfig() {
        return config;
    }
}
