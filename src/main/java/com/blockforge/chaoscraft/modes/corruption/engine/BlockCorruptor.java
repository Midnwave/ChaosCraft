package com.blockforge.chaoscraft.modes.corruption.engine;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.claims.ClaimsService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Converts real world blocks into corrupted floating block displays.
 * Handles block selection, removal, BlockDisplay spawning with dark glow
 * and slow rotation, and coordinates with ChunkTracker for per-chunk limits.
 */
public class BlockCorruptor {

    // Dark corruption purple glow
    private static final Color CORRUPTION_GLOW = Color.fromRGB(45, 0, 64);

    // Slow rotation: ~0.03 radians per tick around Y axis
    private static final float ROTATION_ANGLE = 0.03f;

    private final ChaosCraftPlugin plugin;
    private final ChunkTracker tracker;

    public BlockCorruptor(ChaosCraftPlugin plugin, ChunkTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    // ========================
    // Block corruption
    // ========================

    /**
     * Corrupt the block at the given location: remove the real block, replace it
     * with a floating BlockDisplay entity with dark glow and slow rotation.
     *
     * @param loc      the location of the block to corrupt
     * @param restorer the BlockRestorer to save original data for later restoration
     * @return the spawned BlockDisplay entity, or null if the block cannot be corrupted
     */
    public BlockDisplay corruptBlock(Location loc, BlockRestorer restorer) {
        if (loc == null || loc.getWorld() == null) return null;

        Block block = loc.getBlock();
        Material type = block.getType();

        // Skip air, liquids, bedrock, and other non-corruptible blocks
        if (type.isAir()) return null;
        if (type == Material.WATER || type == Material.LAVA) return null;
        if (type == Material.BEDROCK) return null;
        if (type == Material.END_PORTAL || type == Material.END_PORTAL_FRAME) return null;
        if (type == Material.NETHER_PORTAL) return null;
        if (type == Material.BARRIER) return null;

        // Check claims — respect player land
        ClaimsService claims = plugin.getClaimsService();
        if (claims != null && claims.isEnabled()) {
            if (!claims.shouldCorruptionSpreadAt(loc)) {
                return null;
            }
        }

        // Check chunk limits
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        if (!tracker.canAddFloatingBlock(chunkX, chunkZ)) {
            return null;
        }

        // Save original block data for restoration
        BlockData originalData = block.getBlockData().clone();
        restorer.store(loc.clone(), originalData);

        // Replace the real block with air
        block.setType(Material.AIR, false);

        // Spawn a BlockDisplay of the original block type at that location
        Location spawnLoc = loc.clone().add(0.5, 0.5, 0.5);
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        BlockDisplay display = loc.getWorld().spawn(spawnLoc, BlockDisplay.class, d -> {
            d.setBlock(originalData);

            // Center the display on the spawn point
            d.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new AxisAngle4f(ROTATION_ANGLE, 0, 1, 0),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f(0, 0, 1, 0)
            ));

            // Dark corruption glow
            d.setGlowColorOverride(CORRUPTION_GLOW);
            d.setGlowing(true);

            // Eerie dim lighting
            d.setBrightness(new Display.Brightness(8, 4));

            // Visible from moderate distance
            d.setViewRange(0.6f);

            // Smooth interpolation for rotation animation
            d.setInterpolationDuration(10);
            d.setInterpolationDelay(0);
        });

        // Update chunk tracking
        tracker.addFloatingBlock(chunkX, chunkZ);
        tracker.addCorruptedBlock(chunkX, chunkZ);

        // Spawn corruption particles at the corrupted location
        loc.getWorld().spawnParticle(Particle.DUST, spawnLoc, 15, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(80, 0, 128), 1.2f));

        // Play eerie sound
        loc.getWorld().playSound(spawnLoc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.5f, 0.4f);

        return display;
    }

    // ========================
    // Random block selection
    // ========================

    /**
     * Select a random solid block in the world within the given chunk radius
     * from a center point. Avoids bedrock layer, air, and liquids.
     *
     * @param world        the world to search in
     * @param centerX      center X coordinate (block)
     * @param centerZ      center Z coordinate (block)
     * @param radiusChunks radius in chunks to search
     * @return a suitable random Location, or null if nothing found after attempts
     */
    public Location selectRandomBlock(World world, int centerX, int centerZ, int radiusChunks) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int maxAttempts = 15;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Pick a random chunk within radius
            int chunkOffsetX = random.nextInt(-radiusChunks, radiusChunks + 1);
            int chunkOffsetZ = random.nextInt(-radiusChunks, radiusChunks + 1);
            int chunkX = (centerX >> 4) + chunkOffsetX;
            int chunkZ = (centerZ >> 4) + chunkOffsetZ;

            // Check chunk limit before wasting time searching
            if (!tracker.canAddFloatingBlock(chunkX, chunkZ)) continue;

            // Pick a random block within the chunk
            int blockX = (chunkX << 4) + random.nextInt(16);
            int blockZ = (chunkZ << 4) + random.nextInt(16);

            // Pick a random Y between bedrock layer and surface
            int minY = world.getMinHeight() + 5; // Avoid bedrock
            int maxY = world.getHighestBlockYAt(blockX, blockZ);
            if (maxY <= minY) continue;

            int blockY = random.nextInt(minY, maxY + 1);

            Block block = world.getBlockAt(blockX, blockY, blockZ);
            Material type = block.getType();

            // Validate the block is corruptible
            if (type.isAir()) continue;
            if (type == Material.WATER || type == Material.LAVA) continue;
            if (type == Material.BEDROCK) continue;
            if (type == Material.END_PORTAL || type == Material.END_PORTAL_FRAME) continue;
            if (type == Material.BARRIER) continue;
            if (!type.isSolid()) continue;

            // Check claims
            Location loc = block.getLocation();
            ClaimsService claims = plugin.getClaimsService();
            if (claims != null && claims.isEnabled()) {
                if (!claims.shouldCorruptionSpreadAt(loc)) continue;
            }

            return loc;
        }

        return null;
    }
}
