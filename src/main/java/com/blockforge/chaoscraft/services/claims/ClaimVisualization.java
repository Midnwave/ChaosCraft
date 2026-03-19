package com.blockforge.chaoscraft.services.claims;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles temporary block-based visualization of claim boundaries.
 * Places corner and edge marker blocks at the surface level,
 * then restores the original blocks after a configurable duration.
 */
public class ClaimVisualization {

    private final ChaosCraftPlugin plugin;

    // Config values
    private int durationSeconds;
    private Material cornerMaterial;
    private Material edgeMaterial;

    // Active visualizations per player (UUID -> cleanup runnable)
    private final Map<UUID, VisualizationSession> activeSessions = new ConcurrentHashMap<>();

    public ClaimVisualization(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load visualization settings from the plugin config.
     */
    public void loadConfig() {
        var config = plugin.getConfig();
        durationSeconds = config.getInt("claims.visualization.duration-seconds", 15);
        cornerMaterial = parseMaterial(config.getString("claims.visualization.corner-material", "GOLD_BLOCK"), Material.GOLD_BLOCK);
        edgeMaterial = parseMaterial(config.getString("claims.visualization.edge-material", "GLOWSTONE"), Material.GLOWSTONE);
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            Material mat = Material.valueOf(name.toUpperCase());
            if (mat.isBlock()) return mat;
        } catch (IllegalArgumentException ignored) {}
        return fallback;
    }

    // ========================
    // Show Claim
    // ========================

    /**
     * Show claim boundaries to a player using temporary blocks.
     * Cancels any existing visualization for that player before starting a new one.
     *
     * @param player The player to show the visualization to
     * @param claim  The claim to visualize
     */
    public void showClaim(Player player, Claim claim) {
        UUID uuid = player.getUniqueId();

        // Cancel existing visualization for this player
        cancelVisualization(uuid);

        World world = claim.getWorld();
        if (world == null) return;

        int minX = claim.getMinX();
        int maxX = claim.getMaxX();
        int minZ = claim.getMinZ();
        int maxZ = claim.getMaxZ();

        // Collect all visualization positions and their original block data
        Map<Location, BlockData> originalBlocks = new LinkedHashMap<>();

        // Place corner blocks
        placeVisualizationBlock(world, minX, minZ, cornerMaterial, originalBlocks);
        placeVisualizationBlock(world, minX, maxZ, cornerMaterial, originalBlocks);
        placeVisualizationBlock(world, maxX, minZ, cornerMaterial, originalBlocks);
        placeVisualizationBlock(world, maxX, maxZ, cornerMaterial, originalBlocks);

        // Place edge blocks every 5 blocks along each edge (skip corners)
        // North edge (minZ): from minX to maxX
        for (int x = minX + 5; x < maxX; x += 5) {
            placeVisualizationBlock(world, x, minZ, edgeMaterial, originalBlocks);
        }

        // South edge (maxZ): from minX to maxX
        for (int x = minX + 5; x < maxX; x += 5) {
            placeVisualizationBlock(world, x, maxZ, edgeMaterial, originalBlocks);
        }

        // West edge (minX): from minZ to maxZ
        for (int z = minZ + 5; z < maxZ; z += 5) {
            placeVisualizationBlock(world, minX, z, edgeMaterial, originalBlocks);
        }

        // East edge (maxX): from minZ to maxZ
        for (int z = minZ + 5; z < maxZ; z += 5) {
            placeVisualizationBlock(world, maxX, z, edgeMaterial, originalBlocks);
        }

        if (originalBlocks.isEmpty()) return;

        // Schedule cleanup
        BukkitRunnable cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                restoreBlocks(originalBlocks);
                activeSessions.remove(uuid);
            }
        };
        cleanupTask.runTaskLater(plugin, durationSeconds * 20L);

        // Store session
        activeSessions.put(uuid, new VisualizationSession(originalBlocks, cleanupTask));
    }

    /**
     * Place a single visualization block at the surface Y level of the given X/Z.
     * Saves the original block data before replacing it.
     */
    private void placeVisualizationBlock(World world, int x, int z, Material material, Map<Location, BlockData> originalBlocks) {
        int surfaceY = world.getHighestBlockYAt(x, z);
        Location loc = new Location(world, x, surfaceY, z);

        // Save original block data
        BlockData originalData = world.getBlockAt(loc).getBlockData().clone();
        originalBlocks.put(loc, originalData);

        // Place visualization block
        world.getBlockAt(loc).setType(material, false);
    }

    /**
     * Restore all original blocks from a visualization session.
     */
    private void restoreBlocks(Map<Location, BlockData> originalBlocks) {
        for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();

            // Validate the world is still loaded
            if (loc.getWorld() == null || !loc.isWorldLoaded()) continue;

            // Only restore if the block hasn't been changed by something else
            // (a player may have broken/placed blocks during visualization)
            loc.getBlock().setBlockData(data, false);
        }
    }

    // ========================
    // Cancel / Cleanup
    // ========================

    /**
     * Cancel an active visualization for a player, restoring blocks immediately.
     */
    public void cancelVisualization(UUID uuid) {
        VisualizationSession session = activeSessions.remove(uuid);
        if (session != null) {
            // Cancel the scheduled cleanup
            try {
                session.cleanupTask.cancel();
            } catch (IllegalStateException ignored) {
                // Task may have already run
            }
            // Restore blocks immediately
            restoreBlocks(session.originalBlocks);
        }
    }

    /**
     * Check if a player has an active visualization.
     */
    public boolean hasActiveVisualization(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    /**
     * Cancel all active visualizations (e.g., on plugin disable).
     */
    public void cancelAll() {
        for (UUID uuid : new ArrayList<>(activeSessions.keySet())) {
            cancelVisualization(uuid);
        }
    }

    // ========================
    // Inner session class
    // ========================

    /**
     * Tracks an active visualization session for cleanup.
     */
    private static class VisualizationSession {
        final Map<Location, BlockData> originalBlocks;
        final BukkitRunnable cleanupTask;

        VisualizationSession(Map<Location, BlockData> originalBlocks, BukkitRunnable cleanupTask) {
            this.originalBlocks = originalBlocks;
            this.cleanupTask = cleanupTask;
        }
    }
}
