package com.blockforge.chaoscraft.modes.corruption.engine;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds open spaces (caves or above-ground clearings) that corrupted floating
 * blocks can drift toward. Results are cached per chunk with a configurable TTL
 * to avoid repeated world lookups every tick.
 */
public class OpenSpaceFinder {

    private static final long CACHE_TTL_MS = 60_000; // 60 seconds
    private static final int MIN_CAVE_AIR_BLOCKS = 3;

    private final ConcurrentHashMap<Long, CachedSpace> cache = new ConcurrentHashMap<>();

    // ========================
    // Public API
    // ========================

    /**
     * Find the nearest open space to drift toward from a given location.
     * Searches in an expanding spiral pattern up to maxSearchRadius blocks.
     *
     * @param from            the starting location
     * @param maxSearchRadius maximum horizontal search radius in blocks
     * @return a suitable open space Location, or null if none found
     */
    public Location findNearestOpenSpace(Location from, int maxSearchRadius) {
        World world = from.getWorld();
        if (world == null) return null;

        int originX = from.getBlockX();
        int originZ = from.getBlockZ();
        int chunkX = originX >> 4;
        int chunkZ = originZ >> 4;

        // Check cache first for the origin chunk
        Location cached = getCachedSpace(chunkX, chunkZ);
        if (cached != null && cached.getWorld() == world) return cached;

        // Search in expanding rings of chunks around the origin
        int chunkRadius = (maxSearchRadius >> 4) + 1;

        for (int ring = 0; ring <= chunkRadius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    // Only check the perimeter of each ring (skip interior, already checked)
                    if (ring > 0 && Math.abs(dx) != ring && Math.abs(dz) != ring) continue;

                    int cx = chunkX + dx;
                    int cz = chunkZ + dz;

                    // Check cache for this chunk
                    Location cachedResult = getCachedSpace(cx, cz);
                    if (cachedResult != null && cachedResult.getWorld() == world) {
                        return cachedResult;
                    }

                    // Search this chunk for open space
                    Location found = searchChunkForOpenSpace(world, cx, cz);
                    if (found != null) {
                        cacheSpace(cx, cz, found);
                        return found;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Clear all cached results. Call on mode stop or world change.
     */
    public void clearCache() {
        cache.clear();
    }

    // ========================
    // Search logic
    // ========================

    /**
     * Search a single chunk for a suitable open space.
     * Checks above-ground first, then underground caves.
     */
    private Location searchChunkForOpenSpace(World world, int chunkX, int chunkZ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        // Sample 4 random positions within the chunk (center + 3 offsets)
        int[][] samples = {
                {baseX + 8, baseZ + 8},
                {baseX + 3, baseZ + 3},
                {baseX + 12, baseZ + 5},
                {baseX + 5, baseZ + 12}
        };

        for (int[] sample : samples) {
            int x = sample[0];
            int z = sample[1];

            // Strategy 1: Above grass level — simple and common
            Location aboveGround = checkAboveGround(world, x, z);
            if (aboveGround != null) return aboveGround;

            // Strategy 2: Underground cave detection
            Location cave = checkForCave(world, x, z);
            if (cave != null) return cave;
        }

        return null;
    }

    /**
     * Check for open space above the highest block (2 blocks above grass level).
     */
    private Location checkAboveGround(World world, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z);
        int targetY = highestY + 2;

        // Ensure it's within world bounds and has air
        if (targetY >= world.getMaxHeight()) return null;
        if (targetY <= world.getMinHeight()) return null;

        Block target = world.getBlockAt(x, targetY, z);
        Block above = world.getBlockAt(x, targetY + 1, z);

        if (target.getType().isAir() && above.getType().isAir()) {
            return new Location(world, x + 0.5, targetY + 0.5, z + 0.5);
        }
        return null;
    }

    /**
     * Check for underground cave spaces (3+ adjacent air blocks).
     * Scans downward from highest block to find air pockets.
     */
    private Location checkForCave(World world, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z);
        int minY = Math.max(world.getMinHeight() + 5, 10); // Avoid bedrock layer

        for (int y = highestY - 5; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!block.getType().isAir()) continue;

            // Count adjacent air blocks (including diagonals on same Y)
            int airCount = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (world.getBlockAt(x + dx, y, z + dz).getType().isAir()) {
                        airCount++;
                    }
                }
            }

            // Also check above and below
            if (world.getBlockAt(x, y + 1, z).getType().isAir()) airCount++;
            if (world.getBlockAt(x, y - 1, z).getType().isAir()) airCount++;

            if (airCount >= MIN_CAVE_AIR_BLOCKS) {
                return new Location(world, x + 0.5, y + 0.5, z + 0.5);
            }
        }

        return null;
    }

    // ========================
    // Cache management
    // ========================

    private static long cacheKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private Location getCachedSpace(int chunkX, int chunkZ) {
        long key = cacheKey(chunkX, chunkZ);
        CachedSpace entry = cache.get(key);
        if (entry == null) return null;

        // Check TTL
        if (System.currentTimeMillis() - entry.timestamp > CACHE_TTL_MS) {
            cache.remove(key);
            return null;
        }

        return entry.location;
    }

    private void cacheSpace(int chunkX, int chunkZ, Location location) {
        long key = cacheKey(chunkX, chunkZ);
        cache.put(key, new CachedSpace(location, System.currentTimeMillis()));
    }

    // ========================
    // Cache entry
    // ========================

    private static class CachedSpace {
        final Location location;
        final long timestamp;

        CachedSpace(Location location, long timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
    }
}
