package com.blockforge.chaoscraft.modes.corruption.engine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Thread-safe tracker for corruption state per chunk.
 * Tracks floating block counts and corrupted block counts to enforce
 * configurable per-chunk limits during Corrupted Corruption mode.
 */
public class ChunkTracker {

    private final ConcurrentHashMap<Long, ChunkCorruptionData> chunkData = new ConcurrentHashMap<>();
    private int maxFloatingBlocksPerChunk;

    public ChunkTracker(int maxFloatingBlocksPerChunk) {
        this.maxFloatingBlocksPerChunk = maxFloatingBlocksPerChunk;
    }

    // ========================
    // Chunk key encoding
    // ========================

    /**
     * Encode chunk coordinates into a single long key.
     * Uses upper 32 bits for chunkX, lower 32 bits for chunkZ.
     */
    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    // ========================
    // Floating block management
    // ========================

    /**
     * Check if the chunk at the given coordinates can accept another floating block.
     */
    public boolean canAddFloatingBlock(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        ChunkCorruptionData data = chunkData.get(key);
        if (data == null) return true;
        return data.getFloatingBlockCount() < maxFloatingBlocksPerChunk;
    }

    /**
     * Register a new floating block in the given chunk.
     */
    public void addFloatingBlock(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        chunkData.computeIfAbsent(key, k -> new ChunkCorruptionData(maxFloatingBlocksPerChunk))
                .incrementFloatingBlocks();
    }

    /**
     * Remove a floating block from the given chunk's count.
     */
    public void removeFloatingBlock(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        ChunkCorruptionData data = chunkData.get(key);
        if (data != null) {
            data.decrementFloatingBlocks();
            // Clean up empty entries to prevent memory leaks
            if (data.isEmpty()) {
                chunkData.remove(key);
            }
        }
    }

    // ========================
    // Corrupted block tracking
    // ========================

    /**
     * Increment the corrupted block count for a chunk.
     */
    public void addCorruptedBlock(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        chunkData.computeIfAbsent(key, k -> new ChunkCorruptionData(maxFloatingBlocksPerChunk))
                .incrementCorruptedBlocks();
    }

    /**
     * Decrement the corrupted block count for a chunk.
     */
    public void removeCorruptedBlock(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        ChunkCorruptionData data = chunkData.get(key);
        if (data != null) {
            data.decrementCorruptedBlocks();
            if (data.isEmpty()) {
                chunkData.remove(key);
            }
        }
    }

    // ========================
    // Stats & config
    // ========================

    /**
     * Get the number of chunks with any corruption data.
     */
    public int getAffectedChunkCount() {
        return chunkData.size();
    }

    /**
     * Get the total floating block count across all chunks.
     */
    public int getTotalFloatingBlocks() {
        int total = 0;
        for (ChunkCorruptionData data : chunkData.values()) {
            total += data.getFloatingBlockCount();
        }
        return total;
    }

    /**
     * Get the total corrupted block count across all chunks.
     */
    public int getTotalCorruptedBlocks() {
        int total = 0;
        for (ChunkCorruptionData data : chunkData.values()) {
            total += data.getCorruptedBlockCount();
        }
        return total;
    }

    /**
     * Update the max floating blocks per chunk (e.g., on config reload).
     */
    public void setMaxFloatingBlocksPerChunk(int max) {
        this.maxFloatingBlocksPerChunk = max;
        // Update existing entries
        for (ChunkCorruptionData data : chunkData.values()) {
            data.setMaxFloatingBlocks(max);
        }
    }

    public int getMaxFloatingBlocksPerChunk() {
        return maxFloatingBlocksPerChunk;
    }

    /**
     * Clear all tracking data. Called on mode stop/reset.
     */
    public void reset() {
        chunkData.clear();
    }

    // ========================
    // Inner data class
    // ========================

    /**
     * Per-chunk corruption state. Access is thread-safe via ConcurrentHashMap
     * guarantees — individual field mutations use volatile for visibility.
     */
    public static class ChunkCorruptionData {
        private volatile int floatingBlockCount;
        private volatile int corruptedBlockCount;
        private volatile int maxFloatingBlocks;

        public ChunkCorruptionData(int maxFloatingBlocks) {
            this.maxFloatingBlocks = maxFloatingBlocks;
            this.floatingBlockCount = 0;
            this.corruptedBlockCount = 0;
        }

        public int getFloatingBlockCount() { return floatingBlockCount; }
        public int getCorruptedBlockCount() { return corruptedBlockCount; }
        public int getMaxFloatingBlocks() { return maxFloatingBlocks; }

        public void setMaxFloatingBlocks(int max) { this.maxFloatingBlocks = max; }

        public synchronized void incrementFloatingBlocks() { floatingBlockCount++; }
        public synchronized void decrementFloatingBlocks() {
            if (floatingBlockCount > 0) floatingBlockCount--;
        }

        public synchronized void incrementCorruptedBlocks() { corruptedBlockCount++; }
        public synchronized void decrementCorruptedBlocks() {
            if (corruptedBlockCount > 0) corruptedBlockCount--;
        }

        /**
         * Returns true if this chunk has no corruption data and can be cleaned up.
         */
        public boolean isEmpty() {
            return floatingBlockCount == 0 && corruptedBlockCount == 0;
        }
    }
}
