package com.blockforge.chaoscraft.services.claims;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * Represents a land claim. 2D column from bedrock to sky.
 * Defined by two corner coordinates (x1,z1) and (x2,z2) in a specific world.
 */
public class Claim {

    private final int id;
    private final UUID owner;
    private final String worldName;
    private int minX, minZ, maxX, maxZ;
    private final Map<UUID, TrustLevel> trusts = new HashMap<>();
    private final Map<ClaimFlag, Boolean> flags = new EnumMap<>(ClaimFlag.class);
    private final long createdAt;

    public Claim(int id, UUID owner, String worldName, int x1, int z1, int x2, int z2, long createdAt) {
        this.id = id;
        this.owner = owner;
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
        this.createdAt = createdAt;

        // Initialize flags with defaults
        for (ClaimFlag flag : ClaimFlag.values()) {
            flags.put(flag, flag.getDefaultValue());
        }
    }

    // ---- Spatial checks ----

    /**
     * Check if a location (x, z) is inside this claim.
     */
    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * Check if a location is inside this claim (checks world too).
     */
    public boolean contains(Location loc) {
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        return contains(loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Check if this claim overlaps with another claim.
     */
    public boolean overlaps(Claim other) {
        if (!this.worldName.equals(other.worldName)) return false;
        return this.minX <= other.maxX && this.maxX >= other.minX
                && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    /**
     * Check if this claim overlaps with a rectangle defined by two corners.
     */
    public boolean overlaps(String world, int x1, int z1, int x2, int z2) {
        if (!this.worldName.equals(world)) return false;
        int oMinX = Math.min(x1, x2), oMaxX = Math.max(x1, x2);
        int oMinZ = Math.min(z1, z2), oMaxZ = Math.max(z1, z2);
        return this.minX <= oMaxX && this.maxX >= oMinX
                && this.minZ <= oMaxZ && this.maxZ >= oMinZ;
    }

    /**
     * Get the area (number of blocks) this claim covers.
     */
    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    public int getWidth() { return maxX - minX + 1; }
    public int getLength() { return maxZ - minZ + 1; }

    // ---- Trust management ----

    public void setTrust(UUID player, TrustLevel level) {
        trusts.put(player, level);
    }

    public void removeTrust(UUID player) {
        trusts.remove(player);
    }

    public TrustLevel getTrust(UUID player) {
        return trusts.get(player);
    }

    public boolean hasTrust(UUID player, TrustLevel required) {
        if (player.equals(owner)) return true;
        TrustLevel level = trusts.get(player);
        return level != null && level.includes(required);
    }

    public Map<UUID, TrustLevel> getTrusts() {
        return Collections.unmodifiableMap(trusts);
    }

    // ---- Flag management ----

    public boolean getFlag(ClaimFlag flag) {
        return flags.getOrDefault(flag, flag.getDefaultValue());
    }

    public void setFlag(ClaimFlag flag, boolean value) {
        flags.put(flag, value);
    }

    public Map<ClaimFlag, Boolean> getFlags() {
        return Collections.unmodifiableMap(flags);
    }

    // ---- Resize ----

    public void resize(int x1, int z1, int x2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
    }

    // ---- Accessors ----

    public int getId() { return id; }
    public UUID getOwner() { return owner; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxZ() { return maxZ; }
    public long getCreatedAt() { return createdAt; }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Claim other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Claim{id=" + id + ", owner=" + owner + ", world=" + worldName
                + ", bounds=[" + minX + "," + minZ + " -> " + maxX + "," + maxZ + "]"
                + ", area=" + getArea() + "}";
    }
}
