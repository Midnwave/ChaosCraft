package com.blockforge.chaoscraft.modes.doom;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Manages the Doom Mode arena cuboid — pos1/pos2 boundary definition
 * and per-tick boundary enforcement that prevents players from escaping.
 *
 * <p>Arena positions are set via:
 * <ul>
 *   <li>/cc function setdoommodepos1 — saves player's current position as corner 1</li>
 *   <li>/cc function setdoommodepos2 — saves player's current position as corner 2</li>
 * </ul>
 *
 * <p>Boundary enforcement uses position clamping + inward velocity push.
 * No barrier blocks are placed — this avoids block placement/removal lag
 * and handles edge cases like ender pearls within 1 tick.
 */
public class DoomArenaManager {

    private final ChaosCraftPlugin plugin;
    private final DoomConfig config;

    // Computed min/max bounds
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;
    private String worldName;
    private boolean configured = false;

    public DoomArenaManager(ChaosCraftPlugin plugin, DoomConfig config) {
        this.plugin = plugin;
        this.config = config;
        loadFromConfig();
    }

    /**
     * Load arena bounds from the config and compute min/max.
     */
    public void loadFromConfig() {
        int x1 = config.getArenaPos1X();
        int y1 = config.getArenaPos1Y();
        int z1 = config.getArenaPos1Z();
        int x2 = config.getArenaPos2X();
        int y2 = config.getArenaPos2Y();
        int z2 = config.getArenaPos2Z();
        worldName = config.getArenaWorldName();

        // Compute min/max
        minX = Math.min(x1, x2);
        minY = Math.min(y1, y2);
        minZ = Math.min(z1, z2);
        maxX = Math.max(x1, x2);
        maxY = Math.max(y1, y2);
        maxZ = Math.max(z1, z2);

        // Check if configured (both positions must be non-zero and world set)
        configured = !worldName.isEmpty()
                && !(x1 == 0 && y1 == 0 && z1 == 0 && x2 == 0 && y2 == 0 && z2 == 0);

        if (configured) {
            plugin.debug("[Doom] Arena loaded: (" + minX + "," + minY + "," + minZ
                    + ") to (" + maxX + "," + maxY + "," + maxZ + ") in world '" + worldName + "'");
        }
    }

    /**
     * Whether both arena positions are set and valid.
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Get the Bukkit World object for the arena.
     */
    public World getArenaWorld() {
        if (worldName == null || worldName.isEmpty()) return null;
        return Bukkit.getWorld(worldName);
    }

    /**
     * Check if a location is inside the arena cuboid.
     */
    public boolean isInsideArena(Location loc) {
        if (!configured) return false;
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Enforce arena boundary for a player. Called every tick.
     * If the player is outside the bounds, clamp their position and push them inward.
     */
    public void enforceArenaBoundary(Player player) {
        if (!configured || !config.isEnforceBoundary()) return;
        if (player.getWorld() == null || !player.getWorld().getName().equals(worldName)) return;

        Location loc = player.getLocation();
        double px = loc.getX();
        double py = loc.getY();
        double pz = loc.getZ();

        // Clamp to arena bounds (with 0.5 margin for block center)
        double cx = clamp(px, minX, maxX + 1);
        double cy = clamp(py, minY, maxY + 1);
        double cz = clamp(pz, minZ, maxZ + 1);

        boolean outsideX = px != cx;
        boolean outsideY = py != cy;
        boolean outsideZ = pz != cz;

        if (outsideX || outsideY || outsideZ) {
            // Teleport to clamped position
            Location clamped = new Location(player.getWorld(), cx, cy, cz, loc.getYaw(), loc.getPitch());
            player.teleport(clamped);

            // Push inward
            double strength = config.getBoundaryPushStrength();
            double vx = outsideX ? (px > cx ? -strength : strength) : 0;
            double vy = outsideY ? (py > cy ? -strength : strength) : 0;
            double vz = outsideZ ? (pz > cz ? -strength : strength) : 0;
            player.setVelocity(new Vector(vx, vy, vz));
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ========================
    // Getters for lava fill system
    // ========================

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    public String getWorldName() { return worldName; }
}
