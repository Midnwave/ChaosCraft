package com.blockforge.chaoscraft.services.claims;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks and awards claim blocks to players.
 * Supports: playtime, voting, economy, admin grants, mode survival bonuses.
 */
public class ClaimBlockTracker {

    private final ChaosCraftPlugin plugin;
    private final ClaimStorage storage;

    private int playtimeBlocksPerHour;
    private int voteBlocks;
    private int modeSurvivalBonus;
    private int initialBlocks;
    private boolean economyEnabled;
    private double pricePerBlock;

    private BukkitTask playtimeTask;
    // Track online time per player (in minutes since last award)
    private final Map<UUID, Integer> playtimeMinutes = new HashMap<>();

    public ClaimBlockTracker(ChaosCraftPlugin plugin, ClaimStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void initialize() {
        loadConfig();
        startPlaytimeTracking();
    }

    public void loadConfig() {
        var config = plugin.getConfig();
        playtimeBlocksPerHour = config.getInt("claims.earning.playtime-blocks-per-hour", 100);
        voteBlocks = config.getInt("claims.earning.vote-blocks", 50);
        modeSurvivalBonus = config.getInt("claims.earning.mode-survival-bonus", 200);
        initialBlocks = config.getInt("claims.initial-blocks", 100);
        economyEnabled = config.getBoolean("claims.earning.economy.enabled", true);
        pricePerBlock = config.getDouble("claims.earning.economy.price-per-block", 1.0);
    }

    public void shutdown() {
        if (playtimeTask != null) {
            playtimeTask.cancel();
            playtimeTask = null;
        }
    }

    /**
     * Track playtime and award blocks every minute check (awards proportionally per hour).
     */
    private void startPlaytimeTracking() {
        if (playtimeBlocksPerHour <= 0) return;

        // Run every 60 seconds (1200 ticks)
        playtimeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                int minutes = playtimeMinutes.getOrDefault(uuid, 0) + 1;
                playtimeMinutes.put(uuid, minutes);

                // Award every 60 minutes
                if (minutes >= 60) {
                    playtimeMinutes.put(uuid, 0);
                    storage.addClaimBlocks(uuid, playtimeBlocksPerHour);
                    plugin.debug("[Claims] Awarded " + playtimeBlocksPerHour + " claim blocks to " + player.getName() + " for 1 hour playtime.");
                }
            }
        }, 1200L, 1200L);
    }

    /**
     * Ensure a player has at least the initial claim block amount.
     * Called on first join.
     */
    public void ensureInitialBlocks(UUID player) {
        int current = storage.getClaimBlocks(player);
        if (current <= 0) {
            storage.setClaimBlocks(player, initialBlocks);
            plugin.debug("[Claims] Granted initial " + initialBlocks + " claim blocks to " + player);
        }
    }

    /**
     * Award blocks for voting (called by vote listener/plugin hook).
     */
    public void awardVoteBlocks(UUID player) {
        if (voteBlocks > 0) {
            storage.addClaimBlocks(player, voteBlocks);
        }
    }

    /**
     * Award blocks for surviving a mode.
     */
    public void awardModeSurvivalBonus(UUID player) {
        if (modeSurvivalBonus > 0) {
            storage.addClaimBlocks(player, modeSurvivalBonus);
        }
    }

    /**
     * Buy claim blocks with economy (Vault).
     * Returns true if purchase successful.
     */
    public boolean buyClaimBlocks(Player player, int amount) {
        if (!economyEnabled || pricePerBlock <= 0) return false;

        // Vault economy integration
        var economy = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economy == null) return false;

        double cost = amount * pricePerBlock;
        var eco = economy.getProvider();
        if (!eco.has(player, cost)) return false;

        eco.withdrawPlayer(player, cost);
        storage.addClaimBlocks(player.getUniqueId(), amount);
        return true;
    }

    // ---- Accessors ----

    public int getPlaytimeBlocksPerHour() { return playtimeBlocksPerHour; }
    public int getVoteBlocks() { return voteBlocks; }
    public int getModeSurvivalBonus() { return modeSurvivalBonus; }
    public int getInitialBlocks() { return initialBlocks; }
    public boolean isEconomyEnabled() { return economyEnabled; }
    public double getPricePerBlock() { return pricePerBlock; }
}
