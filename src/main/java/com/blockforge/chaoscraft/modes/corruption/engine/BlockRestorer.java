package com.blockforge.chaoscraft.modes.corruption.engine;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Stores original block data for all blocks corrupted during the mode,
 * and provides both gradual and immediate restoration on mode end.
 * Restoration plays particles and sounds for visual feedback.
 */
public class BlockRestorer {

    private final ChaosCraftPlugin plugin;

    /**
     * Ordered map of original blocks — insertion order preserved for
     * reverse restoration (outer blocks first).
     */
    private final LinkedHashMap<Location, BlockData> originalBlocks = new LinkedHashMap<>();

    private BukkitRunnable restoreTask;

    public BlockRestorer(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    // Storage
    // ========================

    /**
     * Save the original block data at a location before corruption.
     */
    public void store(Location loc, BlockData data) {
        if (loc == null || data == null) return;
        // Only store the first occurrence — if a block was already corrupted, keep the original
        originalBlocks.putIfAbsent(loc, data);
    }

    /**
     * Get the number of stored original blocks awaiting restoration.
     */
    public int getStoredCount() {
        return originalBlocks.size();
    }

    // ========================
    // Gradual restoration
    // ========================

    /**
     * Start gradual block restoration at the configured rate.
     * Restores in reverse insertion order (last corrupted = first restored,
     * meaning outer/later blocks restore before inner/earlier ones).
     * Spawns green/white particles and plays a soft placement sound at each block.
     *
     * @param blocksPerTick how many blocks to restore each tick
     * @param onComplete    callback to run when all blocks have been restored (nullable)
     */
    public void restoreAll(int blocksPerTick, Runnable onComplete) {
        if (originalBlocks.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // Cancel any existing restore task
        cancelRestoreTask();

        // Build a reversed list for restoration order
        List<Map.Entry<Location, BlockData>> entries = new ArrayList<>(originalBlocks.entrySet());
        Collections.reverse(entries);

        restoreTask = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int restored = 0;

                while (index < entries.size() && restored < blocksPerTick) {
                    Map.Entry<Location, BlockData> entry = entries.get(index);
                    Location loc = entry.getKey();
                    BlockData data = entry.getValue();

                    // Restore the block
                    if (loc.getWorld() != null) {
                        loc.getBlock().setBlockData(data, true);

                        // Visual feedback: green and white particles
                        Location particleLoc = loc.clone().add(0.5, 0.5, 0.5);
                        loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 8, 0.3, 0.3, 0.3, 0,
                                new Particle.DustOptions(Color.fromRGB(100, 255, 100), 0.8f));
                        loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.2, 0.2, 0.2, 0,
                                new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.6f));

                        // Soft placement sound
                        loc.getWorld().playSound(particleLoc, Sound.BLOCK_STONE_PLACE, 0.15f, 1.2f);
                    }

                    index++;
                    restored++;
                }

                // Check if all blocks are restored
                if (index >= entries.size()) {
                    originalBlocks.clear();
                    cancel();
                    restoreTask = null;

                    plugin.getLogger().info("[Corruption] Block restoration complete.");

                    if (onComplete != null) {
                        // Run on next tick to avoid issues from within BukkitRunnable
                        Bukkit.getScheduler().runTask(plugin, onComplete);
                    }
                }
            }
        };

        restoreTask.runTaskTimer(plugin, 1L, 1L);
        plugin.getLogger().info("[Corruption] Starting gradual restoration of " + entries.size()
                + " blocks at " + blocksPerTick + " blocks/tick.");
    }

    // ========================
    // Immediate restoration
    // ========================

    /**
     * Restore all blocks immediately without animation. Used for emergency
     * cleanup (server shutdown, force stop, etc.).
     */
    public void restoreImmediate() {
        cancelRestoreTask();

        int count = 0;
        for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();

            if (loc.getWorld() != null) {
                loc.getBlock().setBlockData(data, true);
                count++;
            }
        }

        originalBlocks.clear();
        plugin.getLogger().info("[Corruption] Immediate restoration complete — " + count + " blocks restored.");
    }

    // ========================
    // Cleanup
    // ========================

    /**
     * Clear all stored data without restoring any blocks.
     * Used for cleanup when restoration is not needed (e.g., world reset).
     */
    public void clear() {
        cancelRestoreTask();
        originalBlocks.clear();
    }

    /**
     * Cancel the running gradual restore task if active.
     */
    private void cancelRestoreTask() {
        if (restoreTask != null) {
            try {
                restoreTask.cancel();
            } catch (IllegalStateException ignored) {
                // Already cancelled or never scheduled
            }
            restoreTask = null;
        }
    }

    /**
     * Check if a gradual restoration is currently running.
     */
    public boolean isRestoring() {
        return restoreTask != null;
    }
}
