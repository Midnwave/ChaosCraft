package com.blockforge.chaoscraft.modes.corruption.engine;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Main corruption engine that coordinates all world corruption mechanics.
 * Called every tick from CorruptionMode.onTick(). Manages the lifecycle of
 * corrupted blocks: selection, conversion to floating displays, movement
 * toward open spaces, proximity damage/effects, and restoration on stop.
 */
public class CorruptionEngine {

    // Proximity damage radius in blocks
    private static final double PROXIMITY_RADIUS = 2.5;

    // Movement: fraction of distance to move per tick (1/20th = smooth drift)
    private static final double MOVE_FRACTION = 1.0 / 20.0;

    // Corruption particle colors
    private static final Particle.DustOptions CORRUPTION_DUST =
            new Particle.DustOptions(Color.fromRGB(80, 0, 128), 1.0f);
    private static final Particle.DustOptions DARK_DUST =
            new Particle.DustOptions(Color.fromRGB(30, 0, 50), 1.5f);

    private final ChaosCraftPlugin plugin;
    private final CorruptionConfig config;

    // Sub-systems
    private final ChunkTracker chunkTracker;
    private final OpenSpaceFinder spaceFinder;
    private final BlockCorruptor corruptor;
    private final BlockRestorer restorer;

    // Active state
    private World activeWorld;
    private boolean running;
    private long tickCounter;

    // Active floating block displays with their movement targets
    private final List<FloatingBlock> floatingBlocks = new ArrayList<>();

    public CorruptionEngine(ChaosCraftPlugin plugin, CorruptionConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.chunkTracker = new ChunkTracker(config.getMaxFloatingBlocksPerChunk());
        this.spaceFinder = new OpenSpaceFinder();
        this.restorer = new BlockRestorer(plugin);
        this.corruptor = new BlockCorruptor(plugin, chunkTracker);
    }

    // ========================
    // Lifecycle
    // ========================

    /**
     * Start corruption in the given world.
     */
    public void start(World world) {
        if (running) {
            plugin.getLogger().warning("[Corruption] Engine already running — ignoring start call.");
            return;
        }

        this.activeWorld = world;
        this.running = true;
        this.tickCounter = 0;

        // Apply config values to tracker
        chunkTracker.setMaxFloatingBlocksPerChunk(config.getMaxFloatingBlocksPerChunk());

        plugin.getLogger().info("[Corruption] Engine started in world: " + world.getName());
    }

    /**
     * Main tick method — called every tick from CorruptionMode.onTick().
     */
    public void tick() {
        if (!running || activeWorld == null) return;
        tickCounter++;

        // 1. Spread corruption at configured rate
        if (tickCounter % config.getSpreadRateTicks() == 0) {
            spreadCorruption();
        }

        // 2. Move all active floating blocks toward their targets
        moveFloatingBlocks();

        // 3. Check player proximity to floating blocks
        checkPlayerProximity();

        // 4. Spawn ambient particles periodically
        if (tickCounter % 5 == 0) {
            spawnAmbientParticles();
        }

        // 5. Update floating block rotations
        if (tickCounter % 10 == 0) {
            updateRotations();
        }
    }

    /**
     * Stop the corruption engine and begin restoration.
     */
    public void stop() {
        if (!running) return;
        running = false;

        plugin.getLogger().info("[Corruption] Engine stopping — removing "
                + floatingBlocks.size() + " floating displays...");

        // Remove all floating block display entities immediately
        for (FloatingBlock fb : floatingBlocks) {
            if (fb.entity != null && fb.entity.isValid()) {
                // Despawn particle burst
                Location loc = fb.entity.getLocation();
                activeWorld.spawnParticle(Particle.DUST, loc, 10, 0.4, 0.4, 0.4, 0, CORRUPTION_DUST);
                fb.entity.remove();
            }
        }
        floatingBlocks.clear();

        // Start gradual block restoration
        restorer.restoreAll(config.getRestoreBlocksPerTick(), () -> {
            // Restoration complete callback
            chunkTracker.reset();
            spaceFinder.clearCache();
            plugin.getLogger().info("[Corruption] Full restoration complete.");
        });
    }

    /**
     * Force immediate full restoration. Used for emergency shutdown.
     */
    public void forceRestore() {
        running = false;

        // Remove all floating displays
        for (FloatingBlock fb : floatingBlocks) {
            if (fb.entity != null && fb.entity.isValid()) {
                fb.entity.remove();
            }
        }
        floatingBlocks.clear();

        // Immediate block restoration
        restorer.restoreImmediate();

        // Clean up tracking
        chunkTracker.reset();
        spaceFinder.clearCache();

        plugin.getLogger().info("[Corruption] Force restoration complete.");
    }

    // ========================
    // Corruption spreading
    // ========================

    /**
     * Select and corrupt a random block in the world.
     */
    private void spreadCorruption() {
        if (activeWorld.getPlayers().isEmpty()) return;

        // Pick a random online player as the corruption center
        List<Player> players = activeWorld.getPlayers();
        Player target = players.get((int) (Math.random() * players.size()));

        // Select blocks near the player (within 15 blocks, not chunk-scale)
        Location center = target.getLocation();
        Location blockLoc = corruptor.selectRandomBlock(
                activeWorld, center.getBlockX(), center.getBlockZ(), 1); // 1 chunk = 16 blocks around player

        if (blockLoc == null) return;

        // Check minimum distance (2.5 blocks) from all existing floating blocks
        double minDistSq = 2.5 * 2.5; // 6.25
        for (FloatingBlock existing : floatingBlocks) {
            if (existing.entity != null && existing.entity.isValid()) {
                if (existing.entity.getLocation().distanceSquared(blockLoc) < minDistSq) {
                    return; // Too close to another floating block — skip
                }
            }
        }

        // Corrupt the block
        BlockDisplay display = corruptor.corruptBlock(blockLoc, restorer);
        if (display == null) return;

        // Float upward 3-6 blocks above where the block was, then drift toward nearest player
        double floatHeight = 3.0 + Math.random() * 3.0;
        Location targetSpace = blockLoc.clone().add(
                (Math.random() - 0.5) * 6, floatHeight, (Math.random() - 0.5) * 6);

        // Create floating block tracking entry
        FloatingBlock fb = new FloatingBlock();
        fb.entity = display;
        fb.targetLocation = targetSpace;
        fb.currentSpeed = config.getFloatSpeed();
        fb.originChunkX = blockLoc.getBlockX() >> 4;
        fb.originChunkZ = blockLoc.getBlockZ() >> 4;

        floatingBlocks.add(fb);

        plugin.debug("[Corruption] Corrupted block at " + formatLoc(blockLoc)
                + " -> drifting to " + formatLoc(targetSpace));
    }

    // ========================
    // Floating block movement
    // ========================

    /**
     * Move all active floating blocks toward their target locations.
     */
    private void moveFloatingBlocks() {
        Iterator<FloatingBlock> iter = floatingBlocks.iterator();

        while (iter.hasNext()) {
            FloatingBlock fb = iter.next();

            // Remove dead entities
            if (fb.entity == null || !fb.entity.isValid()) {
                chunkTracker.removeFloatingBlock(fb.originChunkX, fb.originChunkZ);
                iter.remove();
                continue;
            }

            Location current = fb.entity.getLocation();
            Location target = fb.targetLocation;

            double dx = target.getX() - current.getX();
            double dy = target.getY() - current.getY();
            double dz = target.getZ() - current.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // If close enough to target, retarget toward nearest player + bob
            if (dist < 0.5) {
                // Find nearest player and slowly drift toward them
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player p : activeWorld.getPlayers()) {
                    if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                        double d = p.getLocation().distanceSquared(current);
                        if (d < nearestDist) {
                            nearestDist = d;
                            nearest = p;
                        }
                    }
                }
                if (nearest != null && nearestDist > 4) { // Don't retarget if already on top of player
                    Location playerLoc = nearest.getLocation().add(0, 2.5, 0);
                    fb.targetLocation = playerLoc;
                }
                // Gentle bobbing while drifting
                double bobY = Math.sin(tickCounter * 0.05) * 0.02;
                fb.entity.teleport(current.clone().add(0, bobY, 0));
                continue;
            }

            // Move toward target — Y rises 3x faster than XZ drift
            double moveX = dx * MOVE_FRACTION * fb.currentSpeed;
            double moveY = dy * MOVE_FRACTION * fb.currentSpeed * 3.0; // Rise fast
            double moveZ = dz * MOVE_FRACTION * fb.currentSpeed;

            Location newLoc = current.clone().add(moveX, moveY, moveZ);
            newLoc.setYaw(0);
            newLoc.setPitch(0);
            fb.entity.teleport(newLoc);
        }
    }

    // ========================
    // Rotation updates
    // ========================

    /**
     * Update the rotation of all floating blocks using interpolation
     * for smooth Y-axis spinning.
     */
    private void updateRotations() {
        float rotationIncrement = 0.15f; // Radians per update (~8.6 degrees)

        for (FloatingBlock fb : floatingBlocks) {
            if (fb.entity == null || !fb.entity.isValid()) continue;

            Transformation current = fb.entity.getTransformation();
            fb.rotationAccumulator += rotationIncrement;

            fb.entity.setInterpolationDuration(10);
            fb.entity.setInterpolationDelay(0);
            fb.entity.setTransformation(new Transformation(
                    current.getTranslation(),
                    new AxisAngle4f(fb.rotationAccumulator, 0, 1, 0),
                    current.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }
    }

    // ========================
    // Player proximity effects
    // ========================

    /**
     * Check if any players are too close to floating blocks.
     * Apply damage and Darkness effect on proximity.
     */
    private void checkPlayerProximity() {
        if (activeWorld == null) return;
        List<Player> players = activeWorld.getPlayers();
        if (players.isEmpty()) return;

        for (Player player : players) {
            if (player.isDead()) continue;
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            if (player.getGameMode() == GameMode.CREATIVE) continue;

            Location playerLoc = player.getLocation();

            for (FloatingBlock fb : floatingBlocks) {
                if (fb.entity == null || !fb.entity.isValid()) continue;

                double distance = fb.entity.getLocation().distance(playerLoc);
                if (distance <= PROXIMITY_RADIUS) {
                    // Deal corruption damage
                    player.damage(config.getProximityDamage());

                    // Apply Darkness effect (40 ticks = 2 seconds)
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.DARKNESS, 40, 0, true, false, false));

                    // Corruption proximity particles on the player
                    player.getWorld().spawnParticle(Particle.DUST, playerLoc.add(0, 1, 0),
                            8, 0.5, 0.5, 0.5, 0, DARK_DUST);

                    // Only apply once per tick per player (break inner loop)
                    break;
                }
            }
        }
    }

    // ========================
    // Ambient particles
    // ========================

    /**
     * Spawn ambient corruption particles around floating blocks.
     */
    private void spawnAmbientParticles() {
        for (FloatingBlock fb : floatingBlocks) {
            if (fb.entity == null || !fb.entity.isValid()) continue;

            Location loc = fb.entity.getLocation();

            // Dark purple dust orbiting the block
            activeWorld.spawnParticle(Particle.DUST, loc, 2, 0.6, 0.6, 0.6, 0, CORRUPTION_DUST);

            // Occasional soul fire particle
            if (Math.random() < 0.15) {
                activeWorld.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }

    // ========================
    // Stats
    // ========================

    /**
     * Get a formatted stats string for debug/admin display.
     */
    public String getStats() {
        int totalCorrupted = restorer.getStoredCount();
        int activeFloating = floatingBlocks.size();
        int chunksAffected = chunkTracker.getAffectedChunkCount();

        return String.format(
                "Corrupted blocks: %d | Active floating: %d | Chunks affected: %d | Tick: %d",
                totalCorrupted, activeFloating, chunksAffected, tickCounter
        );
    }

    /**
     * Check if the engine is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get the active world, or null if not started.
     */
    public World getActiveWorld() {
        return activeWorld;
    }

    /**
     * Get the chunk tracker for external inspection.
     */
    public ChunkTracker getChunkTracker() {
        return chunkTracker;
    }

    /**
     * Get the block restorer for external inspection.
     */
    public BlockRestorer getRestorer() {
        return restorer;
    }

    /**
     * Get the count of active floating blocks.
     */
    public int getActiveFloatingCount() {
        return floatingBlocks.size();
    }

    // ========================
    // Helpers
    // ========================

    private String formatLoc(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    // ========================
    // Floating block data
    // ========================

    /**
     * Internal tracking data for a single corrupted floating block display.
     */
    private static class FloatingBlock {
        BlockDisplay entity;
        Location targetLocation;
        double currentSpeed;
        int originChunkX;
        int originChunkZ;
        float rotationAccumulator = 0f;
    }

    // ========================
    // Config interface
    // ========================

    /**
     * Configuration interface for corruption engine parameters.
     * Implemented by the mode's config class.
     */
    public interface CorruptionConfig {
        /** Ticks between each corruption spread attempt. */
        int getSpreadRateTicks();

        /** Radius in chunks to search for blocks to corrupt around players. */
        int getSpreadRadiusChunks();

        /** Max floating block displays allowed per chunk. */
        int getMaxFloatingBlocksPerChunk();

        /** Movement speed multiplier for floating blocks (1.0 = normal). */
        double getFloatSpeed();

        /** Damage dealt when a player is within proximity radius. */
        double getProximityDamage();

        /** How many blocks to restore per tick during gradual restoration. */
        int getRestoreBlocksPerTick();
    }
}
