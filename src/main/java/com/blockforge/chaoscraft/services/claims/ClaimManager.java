package com.blockforge.chaoscraft.services.claims;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.claims.events.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core claim management: CRUD, overlap detection, trust/flag operations.
 * All claims are cached in memory and persisted to SQLite.
 */
public class ClaimManager {

    private final ChaosCraftPlugin plugin;
    private final ClaimStorage storage;
    private final Map<Integer, Claim> claimsById = new ConcurrentHashMap<>();
    // Spatial index: chunk key -> claims in that chunk
    private final Map<Long, Set<Integer>> chunkIndex = new ConcurrentHashMap<>();

    private int minClaimSize;
    private int maxClaimsPerPlayer;

    public ClaimManager(ChaosCraftPlugin plugin, ClaimStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void initialize() {
        loadConfig();
        List<Claim> loaded = storage.loadAllClaims();
        for (Claim claim : loaded) {
            claimsById.put(claim.getId(), claim);
            indexClaim(claim);
        }
        plugin.getLogger().info("[Claims] Loaded " + loaded.size() + " claims.");
    }

    public void loadConfig() {
        var config = plugin.getConfig();
        minClaimSize = config.getInt("claims.min-size", 5);
        maxClaimsPerPlayer = config.getInt("claims.max-claims-per-player", 10);
    }

    // ========================
    // Create / Delete / Resize
    // ========================

    /**
     * Create a new claim. Returns null if validation fails.
     */
    public Claim createClaim(Player owner, Location corner1, Location corner2) {
        if (corner1.getWorld() == null || corner2.getWorld() == null) return null;
        if (!corner1.getWorld().equals(corner2.getWorld())) return null;

        String worldName = corner1.getWorld().getName();
        int x1 = corner1.getBlockX(), z1 = corner1.getBlockZ();
        int x2 = corner2.getBlockX(), z2 = corner2.getBlockZ();

        // Size check
        int width = Math.abs(x2 - x1) + 1;
        int length = Math.abs(z2 - z1) + 1;
        if (width < minClaimSize || length < minClaimSize) {
            return null; // Too small
        }

        // Claim count check
        long playerClaimCount = claimsById.values().stream()
                .filter(c -> c.getOwner().equals(owner.getUniqueId()))
                .count();
        if (playerClaimCount >= maxClaimsPerPlayer) {
            return null; // Too many claims
        }

        // Claim blocks check
        int area = width * length;
        int availableBlocks = storage.getClaimBlocks(owner.getUniqueId());
        int usedBlocks = getUsedClaimBlocks(owner.getUniqueId());
        if (availableBlocks - usedBlocks < area) {
            return null; // Not enough claim blocks
        }

        // Overlap check
        for (Claim existing : claimsById.values()) {
            if (existing.overlaps(worldName, x1, z1, x2, z2)) {
                return null; // Overlaps existing claim
            }
        }

        int id = storage.getNextClaimId();
        Claim claim = new Claim(id, owner.getUniqueId(), worldName, x1, z1, x2, z2, System.currentTimeMillis());

        // Fire event
        ClaimCreateEvent event = new ClaimCreateEvent(claim, owner);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        claimsById.put(id, claim);
        indexClaim(claim);
        storage.saveClaim(claim);

        return claim;
    }

    /**
     * Delete a claim. Returns true if successful.
     */
    public boolean deleteClaim(Claim claim, Player actor) {
        ClaimDeleteEvent event = new ClaimDeleteEvent(claim, actor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        deindexClaim(claim);
        claimsById.remove(claim.getId());
        storage.deleteClaim(claim.getId());
        return true;
    }

    /**
     * Resize a claim. Returns true if successful.
     */
    public boolean resizeClaim(Claim claim, Player actor, int newX1, int newZ1, int newX2, int newZ2) {
        int newMinX = Math.min(newX1, newX2), newMaxX = Math.max(newX1, newX2);
        int newMinZ = Math.min(newZ1, newZ2), newMaxZ = Math.max(newZ1, newZ2);

        // Size check
        int width = newMaxX - newMinX + 1;
        int length = newMaxZ - newMinZ + 1;
        if (width < minClaimSize || length < minClaimSize) return false;

        // Claim blocks check (area difference)
        int newArea = width * length;
        int oldArea = claim.getArea();
        if (newArea > oldArea) {
            int extraNeeded = newArea - oldArea;
            int availableBlocks = storage.getClaimBlocks(claim.getOwner());
            int usedBlocks = getUsedClaimBlocks(claim.getOwner());
            if (availableBlocks - usedBlocks < extraNeeded) return false;
        }

        // Overlap check (exclude self)
        for (Claim existing : claimsById.values()) {
            if (existing.getId() == claim.getId()) continue;
            if (existing.overlaps(claim.getWorldName(), newX1, newZ1, newX2, newZ2)) return false;
        }

        // Fire event
        ClaimResizeEvent event = new ClaimResizeEvent(claim, actor, newMinX, newMinZ, newMaxX, newMaxZ);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        deindexClaim(claim);
        claim.resize(newX1, newZ1, newX2, newZ2);
        indexClaim(claim);
        storage.saveClaim(claim);
        return true;
    }

    // ========================
    // Trust & Flags
    // ========================

    public boolean setTrust(Claim claim, Player executor, UUID target, TrustLevel level) {
        ClaimTrustChangeEvent event = new ClaimTrustChangeEvent(claim, executor, target, level);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        claim.setTrust(target, level);
        storage.saveClaim(claim);
        return true;
    }

    public boolean removeTrust(Claim claim, Player executor, UUID target) {
        ClaimTrustChangeEvent event = new ClaimTrustChangeEvent(claim, executor, target, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        claim.removeTrust(target);
        storage.saveClaim(claim);
        return true;
    }

    public boolean setFlag(Claim claim, Player executor, ClaimFlag flag, boolean value) {
        ClaimFlagChangeEvent event = new ClaimFlagChangeEvent(claim, executor, flag, value);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        claim.setFlag(flag, value);
        storage.saveClaim(claim);
        return true;
    }

    // ========================
    // Lookups
    // ========================

    /**
     * Get the claim at a specific location. Returns null if no claim.
     */
    public Claim getClaimAt(Location loc) {
        if (loc.getWorld() == null) return null;

        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        long chunkKey = chunkKey(x >> 4, z >> 4);

        Set<Integer> candidates = chunkIndex.get(chunkKey);
        if (candidates == null || candidates.isEmpty()) return null;

        for (int id : candidates) {
            Claim claim = claimsById.get(id);
            if (claim != null && claim.contains(loc)) {
                return claim;
            }
        }
        return null;
    }

    /**
     * Check if a player can build at a location.
     */
    public boolean canBuild(Player player, Location loc) {
        if (player.hasPermission("chaoscraft.claims.admin")) return true;
        Claim claim = getClaimAt(loc);
        if (claim == null) return true; // No claim = anyone can build
        return claim.hasTrust(player.getUniqueId(), TrustLevel.BUILD);
    }

    /**
     * Check if a player can access (doors, buttons, etc.) at a location.
     */
    public boolean canAccess(Player player, Location loc) {
        if (player.hasPermission("chaoscraft.claims.admin")) return true;
        Claim claim = getClaimAt(loc);
        if (claim == null) return true;
        return claim.hasTrust(player.getUniqueId(), TrustLevel.ACCESS);
    }

    /**
     * Check if a player can open containers at a location.
     */
    public boolean canOpenContainer(Player player, Location loc) {
        if (player.hasPermission("chaoscraft.claims.admin")) return true;
        Claim claim = getClaimAt(loc);
        if (claim == null) return true;
        return claim.hasTrust(player.getUniqueId(), TrustLevel.CONTAINER);
    }

    /**
     * Get all claims owned by a player.
     */
    public List<Claim> getPlayerClaims(UUID owner) {
        return claimsById.values().stream()
                .filter(c -> c.getOwner().equals(owner))
                .collect(Collectors.toList());
    }

    /**
     * Get total claim blocks used by a player.
     */
    public int getUsedClaimBlocks(UUID owner) {
        return claimsById.values().stream()
                .filter(c -> c.getOwner().equals(owner))
                .mapToInt(Claim::getArea)
                .sum();
    }

    public Claim getClaimById(int id) {
        return claimsById.get(id);
    }

    public Collection<Claim> getAllClaims() {
        return Collections.unmodifiableCollection(claimsById.values());
    }

    public int getClaimCount() {
        return claimsById.size();
    }

    // ========================
    // Mode integration checks
    // ========================

    public boolean shouldBlockDisplayDamageAt(Location loc) {
        Claim claim = getClaimAt(loc);
        if (claim == null) return true; // No claim = damage allowed
        return claim.getFlag(ClaimFlag.BLOCK_DISPLAY_DAMAGE);
    }

    public boolean shouldCorruptionSpreadAt(Location loc) {
        Claim claim = getClaimAt(loc);
        if (claim == null) return true;
        return claim.getFlag(ClaimFlag.CORRUPTION_SPREAD);
    }

    public boolean shouldModeEventSpawnAt(Location loc) {
        Claim claim = getClaimAt(loc);
        if (claim == null) return true;
        return claim.getFlag(ClaimFlag.MODE_EVENTS);
    }

    // ========================
    // Chunk spatial index
    // ========================

    private void indexClaim(Claim claim) {
        int minCX = claim.getMinX() >> 4;
        int maxCX = claim.getMaxX() >> 4;
        int minCZ = claim.getMinZ() >> 4;
        int maxCZ = claim.getMaxZ() >> 4;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                long key = chunkKey(cx, cz);
                chunkIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(claim.getId());
            }
        }
    }

    private void deindexClaim(Claim claim) {
        int minCX = claim.getMinX() >> 4;
        int maxCX = claim.getMaxX() >> 4;
        int minCZ = claim.getMinZ() >> 4;
        int maxCZ = claim.getMaxZ() >> 4;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                long key = chunkKey(cx, cz);
                Set<Integer> set = chunkIndex.get(key);
                if (set != null) {
                    set.remove(claim.getId());
                    if (set.isEmpty()) chunkIndex.remove(key);
                }
            }
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
