package com.blockforge.chaoscraft.modes.doom;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Lagless rising lava system for Doom Mode.
 *
 * <p>Fills the arena with real lava blocks (Material.LAVA) from the configured
 * start Y upward, one layer at a time, at a configurable interval.
 *
 * <p><b>Performance guarantees:</b>
 * <ul>
 *   <li>Work is split across ticks — only N blocks processed per tick (configurable)</li>
 *   <li>{@code block.setType(Material.LAVA, false)} skips physics/neighbor/lighting updates</li>
 *   <li>Only AIR blocks are replaced — terrain, builds, and structures are preserved</li>
 *   <li>A 100×100 arena fills one Y level in ~20 ticks (1 sec) at 500 blocks/tick</li>
 *   <li>Cleanup on mode end uses the same batched approach</li>
 * </ul>
 */
public class DoomLavaRise {

    private final ChaosCraftPlugin plugin;
    private final DoomConfig config;
    private final DoomArenaManager arena;

    // State
    private boolean active = false;
    private int currentFillY;
    private int riseTickCounter = 0;
    private boolean filling = false;
    private int fillBlockIndex = 0;
    private int xWidth;
    private int zWidth;
    private int totalBlocksPerLevel;

    // Cleanup state
    private boolean cleaning = false;
    private int cleanupY;
    private int cleanupBlockIndex = 0;

    public DoomLavaRise(ChaosCraftPlugin plugin, DoomConfig config, DoomArenaManager arena) {
        this.plugin = plugin;
        this.config = config;
        this.arena = arena;
    }

    /**
     * Start the lava rise system. Called when Doom Mode starts.
     */
    public void start() {
        if (!config.isLavaRiseEnabled()) {
            plugin.debug("[Doom] Lava rise disabled in config.");
            return;
        }
        if (!arena.isConfigured()) {
            plugin.getLogger().warning("[Doom] Cannot start lava rise — arena not configured!");
            return;
        }

        currentFillY = config.getLavaRiseStartY();
        riseTickCounter = 0;
        filling = false;
        fillBlockIndex = 0;
        active = true;

        // Pre-compute arena dimensions
        xWidth = arena.getMaxX() - arena.getMinX() + 1;
        zWidth = arena.getMaxZ() - arena.getMinZ() + 1;
        totalBlocksPerLevel = xWidth * zWidth;

        plugin.getLogger().info("[Doom] Lava rise started at Y=" + currentFillY
                + ", max Y=" + config.getLavaRiseMaxY()
                + ", arena=" + xWidth + "x" + zWidth + " (" + totalBlocksPerLevel + " blocks/level)"
                + ", batch=" + config.getBlocksPerLevelTick() + " blocks/tick"
                + ", interval=" + config.getRiseIntervalTicks() + " ticks");
    }

    /**
     * Called every tick from DoomMode.onTick().
     * Handles both the fill batching and rise timing.
     */
    public void tick() {
        if (!active) return;

        if (filling) {
            // Continue filling the current Y level
            processFillBatch();
        } else {
            // Wait for next rise interval
            riseTickCounter++;
            if (riseTickCounter >= config.getRiseIntervalTicks()) {
                riseTickCounter = 0;
                triggerRise();
            }
        }
    }

    /**
     * Trigger a new lava rise — advances the fill Y and begins filling.
     */
    private void triggerRise() {
        int maxY = Math.min(config.getLavaRiseMaxY(), arena.getMaxY());

        if (currentFillY >= maxY) {
            plugin.debug("[Doom] Lava has reached max Y=" + maxY + ". Rise complete.");
            return;
        }

        // Rise by configured amount
        int riseAmount = config.getRiseAmount();
        for (int i = 0; i < riseAmount && currentFillY < maxY; i++) {
            // Start filling this level
            filling = true;
            fillBlockIndex = 0;

            // Fill synchronously for each Y level in the rise amount
            // (for multi-level rises, each level is processed in subsequent ticks)
            if (i > 0) {
                // Schedule additional levels for later ticks
                final int levelToFill = currentFillY + i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> fillEntireLevel(levelToFill), (long) i * 2);
            }
        }

        currentFillY += riseAmount;
        currentFillY = Math.min(currentFillY, maxY);

        plugin.debug("[Doom] Lava rising to Y=" + currentFillY);
    }

    /**
     * Process a batch of blocks for the current fill level.
     * Called once per tick while filling is active.
     */
    private void processFillBatch() {
        World world = arena.getArenaWorld();
        if (world == null) {
            filling = false;
            return;
        }

        int batchSize = config.getBlocksPerLevelTick();
        int endIndex = Math.min(fillBlockIndex + batchSize, totalBlocksPerLevel);
        int minX = arena.getMinX();
        int minZ = arena.getMinZ();
        int fillY = currentFillY - config.getRiseAmount(); // The Y we're actually filling

        for (int i = fillBlockIndex; i < endIndex; i++) {
            int x = minX + (i % xWidth);
            int z = minZ + (i / xWidth);

            Block block = world.getBlockAt(x, fillY, z);
            if (block.getType().isAir()) {
                // setType with false = skip physics updates (critical for performance)
                block.setType(Material.LAVA, false);
            }
        }

        fillBlockIndex = endIndex;

        if (fillBlockIndex >= totalBlocksPerLevel) {
            filling = false;
            plugin.debug("[Doom] Filled Y=" + fillY + " (" + totalBlocksPerLevel + " blocks checked)");
        }
    }

    /**
     * Fill an entire Y level in one pass (used for multi-level rises scheduled later).
     */
    private void fillEntireLevel(int y) {
        World world = arena.getArenaWorld();
        if (world == null) return;

        int minX = arena.getMinX();
        int minZ = arena.getMinZ();
        int batchSize = config.getBlocksPerLevelTick();

        // Batch this level across multiple ticks too
        final int[] index = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int end = Math.min(index[0] + batchSize, totalBlocksPerLevel);
            for (int i = index[0]; i < end; i++) {
                int x = minX + (i % xWidth);
                int z = minZ + (i / xWidth);
                Block block = world.getBlockAt(x, y, z);
                if (block.getType().isAir()) {
                    block.setType(Material.LAVA, false);
                }
            }
            index[0] = end;
            if (index[0] >= totalBlocksPerLevel) {
                task.cancel();
            }
        }, 0L, 1L);
    }

    /**
     * Stop the lava rise system.
     */
    public void stop() {
        active = false;
        filling = false;
    }

    /**
     * Clean up all lava blocks placed during the mode.
     * Uses the same batched approach to avoid lag.
     */
    public void cleanup() {
        if (!config.isCleanupOnEnd()) {
            plugin.debug("[Doom] Lava cleanup disabled — lava blocks left in place.");
            return;
        }

        World world = arena.getArenaWorld();
        if (world == null) return;

        int startY = config.getLavaRiseStartY();
        int endY = currentFillY;
        int minX = arena.getMinX();
        int minZ = arena.getMinZ();
        int batchSize = config.getBlocksPerLevelTick();

        plugin.getLogger().info("[Doom] Starting lava cleanup from Y=" + startY + " to Y=" + endY);

        // Process cleanup in batched ticks
        final int[] state = {startY, 0}; // [currentY, blockIndex]

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int y = state[0];
            if (y > endY) {
                task.cancel();
                plugin.getLogger().info("[Doom] Lava cleanup complete.");
                return;
            }

            int end = Math.min(state[1] + batchSize, totalBlocksPerLevel);
            for (int i = state[1]; i < end; i++) {
                int x = minX + (i % xWidth);
                int z = minZ + (i / xWidth);
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.LAVA) {
                    block.setType(Material.AIR, false);
                }
            }
            state[1] = end;

            if (state[1] >= totalBlocksPerLevel) {
                state[0]++;
                state[1] = 0;
            }
        }, 0L, 1L);
    }

    /**
     * Get the current lava Y level (for display/placeholders).
     */
    public int getCurrentLavaY() {
        return currentFillY;
    }

    public boolean isActive() {
        return active;
    }
}
