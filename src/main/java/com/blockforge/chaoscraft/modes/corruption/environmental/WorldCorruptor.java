package com.blockforge.chaoscraft.modes.corruption.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.corruption.engine.BlockRestorer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles world block replacement with corruption blocks.
 * Replaces natural blocks with corruption variants (vanilla, ItemsAdder, or CraftEngine).
 * All original blocks are stored for full restoration on mode end.
 */
public class WorldCorruptor {

    private final ChaosCraftPlugin plugin;
    private final BlockRestorer restorer;

    private World activeWorld;
    private int centerX, centerZ;
    private int maxRadiusChunks;
    private int blocksPerTick;
    private List<Material> vanillaCorruptionBlocks;
    private List<String> itemsadderBlocks;
    private List<String> craftengineBlocks;

    private BukkitTask corruptionTask;
    private int currentRadius = 1; // Starts at 1 chunk radius, expands over time
    private int totalCorrupted = 0;

    // Track which blocks have been corrupted (to avoid double-corrupting)
    private final Set<Long> corruptedPositions = new HashSet<>();

    public WorldCorruptor(ChaosCraftPlugin plugin, BlockRestorer restorer) {
        this.plugin = plugin;
        this.restorer = restorer;
    }

    public void start(World world, int centerX, int centerZ, int maxRadiusChunks,
                      int blocksPerTick, List<Material> vanillaBlocks,
                      List<String> iaBlocks, List<String> ceBlocks) {
        this.activeWorld = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.maxRadiusChunks = maxRadiusChunks;
        this.blocksPerTick = blocksPerTick;
        this.vanillaCorruptionBlocks = vanillaBlocks != null && !vanillaBlocks.isEmpty()
                ? vanillaBlocks
                : List.of(Material.CRYING_OBSIDIAN, Material.BLACKSTONE, Material.DEEPSLATE,
                        Material.SCULK, Material.COAL_BLOCK);
        this.itemsadderBlocks = iaBlocks != null ? iaBlocks : List.of();
        this.craftengineBlocks = ceBlocks != null ? ceBlocks : List.of();

        currentRadius = 1;
        totalCorrupted = 0;

        // Run every tick, corrupt blocksPerTick blocks
        corruptionTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCorruption();
            }
        }.runTaskTimer(plugin, 20L, 1L); // Start after 1 second

        plugin.getLogger().info("[Corruption] World corruptor started. Max radius: "
                + maxRadiusChunks + " chunks, rate: " + blocksPerTick + " blocks/tick.");
    }

    public void stop() {
        if (corruptionTask != null) {
            corruptionTask.cancel();
            corruptionTask = null;
        }
    }

    private void tickCorruption() {
        if (activeWorld == null) return;

        for (int i = 0; i < blocksPerTick; i++) {
            corruptRandomBlock();
        }

        // Slowly expand radius over time (every 200 ticks = 10 seconds)
        if (totalCorrupted % 200 == 0 && currentRadius < maxRadiusChunks) {
            currentRadius++;
            plugin.debug("[Corruption] Corruption radius expanded to " + currentRadius + " chunks.");
        }
    }

    private void corruptRandomBlock() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // Center corruption on a random player, not world spawn
        List<org.bukkit.entity.Player> players = activeWorld.getPlayers();
        int cx = centerX, cz = centerZ;
        if (!players.isEmpty()) {
            org.bukkit.entity.Player target = players.get(rand.nextInt(players.size()));
            cx = target.getLocation().getBlockX();
            cz = target.getLocation().getBlockZ();
        }

        // Pick random position within current radius around the player
        int radiusBlocks = currentRadius * 16;
        int x = cx + rand.nextInt(-radiusBlocks, radiusBlocks + 1);
        int z = cz + rand.nextInt(-radiusBlocks, radiusBlocks + 1);

        // Get highest block at this position — prioritize surface
        int surfaceY = activeWorld.getHighestBlockYAt(x, z);
        if (surfaceY <= activeWorld.getMinHeight()) return;

        // 90% surface (top 3 blocks), 10% underground (down to -5)
        int y;
        if (rand.nextDouble() < 0.9) {
            y = surfaceY - rand.nextInt(3); // Surface: grass, dirt, paths, roofs
        } else {
            int minY = Math.max(activeWorld.getMinHeight() + 1, surfaceY - 5);
            y = rand.nextInt(minY, surfaceY + 1);
        }

        // Check if already corrupted
        long posKey = blockKey(x, y, z);
        if (corruptedPositions.contains(posKey)) return;

        Block block = activeWorld.getBlockAt(x, y, z);
        Material original = block.getType();

        // Skip air, liquids, bedrock, and already-corruption blocks
        if (original.isAir() || original == Material.WATER || original == Material.LAVA
                || original == Material.BEDROCK || original == Material.BARRIER
                || vanillaCorruptionBlocks.contains(original)) {
            return;
        }

        // Check claims
        var claimsService = plugin.getClaimsService();
        if (claimsService != null && claimsService.isEnabled()) {
            if (!claimsService.shouldCorruptionSpreadAt(block.getLocation())) return;
        }

        // Store original for restoration
        BlockData originalData = block.getBlockData().clone();
        restorer.store(block.getLocation(), originalData);

        // Replace with corruption block
        Material corruptionBlock = selectCorruptionBlock();
        block.setType(corruptionBlock, false); // false = no physics update

        corruptedPositions.add(posKey);
        totalCorrupted++;

        // Corruption spread particles at the block
        Location particleLoc = block.getLocation().add(0.5, 1.0, 0.5);
        activeWorld.spawnParticle(Particle.DUST, particleLoc,
                3, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.0f));
    }

    /**
     * Select a random corruption block material.
     * Prefers vanilla blocks. ItemsAdder/CraftEngine blocks handled via string IDs
     * (would need their API to place — using vanilla fallback for now).
     */
    private Material selectCorruptionBlock() {
        // For now, use vanilla blocks. IA/CE integration will use their APIs when available.
        return vanillaCorruptionBlocks.get(
                ThreadLocalRandom.current().nextInt(vanillaCorruptionBlocks.size()));
    }

    public int getTotalCorrupted() { return totalCorrupted; }
    public int getCurrentRadius() { return currentRadius; }

    public void reset() {
        corruptedPositions.clear();
        totalCorrupted = 0;
        currentRadius = 1;
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
