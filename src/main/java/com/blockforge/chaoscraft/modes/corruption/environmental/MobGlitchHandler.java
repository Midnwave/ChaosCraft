package com.blockforge.chaoscraft.modes.corruption.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NMS-less mob AI glitching for hostile mobs in the corruption zone.
 * Makes hostile mobs stutter, jerk, and reverse direction periodically.
 * Uses velocity manipulation and teleport nudges for the glitch effect.
 */
public class MobGlitchHandler {

    private final ChaosCraftPlugin plugin;
    private BukkitTask glitchTask;
    private World activeWorld;
    private int centerX, centerZ;
    private int radiusBlocks;
    private double intensity; // 0.0 - 1.0, probability of glitching per check
    private boolean hostileOnly;

    // Track which mobs are currently glitching (cooldown to prevent spam)
    private final Map<UUID, Long> glitchCooldowns = new ConcurrentHashMap<>();
    private static final long GLITCH_COOLDOWN_MS = 2000; // 2 seconds between glitches per mob

    public MobGlitchHandler(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the mob glitch effect in the given world.
     */
    public void start(World world, int centerX, int centerZ, int radiusChunks,
                      double intensity, boolean hostileOnly) {
        this.activeWorld = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radiusBlocks = radiusChunks * 16;
        this.intensity = intensity;
        this.hostileOnly = hostileOnly;

        // Run every 10 ticks (0.5 seconds)
        glitchTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickGlitch();
            }
        }.runTaskTimer(plugin, 10L, 10L);

        plugin.getLogger().info("[Corruption] Mob glitch handler started. Radius: " + radiusBlocks
                + " blocks, intensity: " + intensity);
    }

    public void stop() {
        if (glitchTask != null) {
            glitchTask.cancel();
            glitchTask = null;
        }
        glitchCooldowns.clear();
    }

    private void tickGlitch() {
        if (activeWorld == null) return;

        long now = System.currentTimeMillis();

        for (Entity entity : activeWorld.getEntities()) {
            // Only affect hostile mobs
            if (hostileOnly && !(entity instanceof Monster)) continue;
            if (!entity.isValid() || entity.isDead()) continue;

            Location loc = entity.getLocation();

            // Check if within corruption radius
            double dx = loc.getBlockX() - centerX;
            double dz = loc.getBlockZ() - centerZ;
            if (dx * dx + dz * dz > (long) radiusBlocks * radiusBlocks) continue;

            // Check cooldown
            Long lastGlitch = glitchCooldowns.get(entity.getUniqueId());
            if (lastGlitch != null && now - lastGlitch < GLITCH_COOLDOWN_MS) continue;

            // Random chance based on intensity
            if (Math.random() > intensity) continue;

            // Apply glitch effect
            applyGlitch(entity);
            glitchCooldowns.put(entity.getUniqueId(), now);
        }

        // Clean up cooldowns for dead/removed entities
        if (now % 10000 < 500) { // Every ~10 seconds
            glitchCooldowns.entrySet().removeIf(e -> now - e.getValue() > 30000);
        }
    }

    /**
     * Apply a random glitch effect to a mob.
     */
    private void applyGlitch(Entity entity) {
        Location loc = entity.getLocation();
        double roll = Math.random();

        if (roll < 0.35) {
            // Stutter: push backward then forward
            Vector backward = loc.getDirection().multiply(-0.3);
            entity.setVelocity(backward);

            // Push forward after 5 ticks
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    Vector forward = loc.getDirection().multiply(0.2);
                    entity.setVelocity(forward);
                }
            }, 5L);
        } else if (roll < 0.6) {
            // Jerk: rapid head snap (teleport with random yaw)
            Location jerk = loc.clone();
            jerk.setYaw(loc.getYaw() + (float) (Math.random() * 90 - 45));
            entity.teleport(jerk);

            // Snap back after 3 ticks
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    Location snapBack = entity.getLocation();
                    snapBack.setYaw(loc.getYaw());
                    entity.teleport(snapBack);
                }
            }, 3L);
        } else if (roll < 0.8) {
            // Freeze: stop movement briefly
            entity.setVelocity(new Vector(0, 0, 0));

            // Resume after 10 ticks
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (entity.isValid() && !entity.isDead()) {
                    // Small nudge to restart pathfinding
                    entity.setVelocity(loc.getDirection().multiply(0.15));
                }
            }, 10L);
        } else {
            // Twitch: small random displacement
            double offsetX = (Math.random() - 0.5) * 0.4;
            double offsetZ = (Math.random() - 0.5) * 0.4;
            entity.setVelocity(new Vector(offsetX, 0.05, offsetZ));
        }

        // Soul particles around glitching mob
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0),
                3, 0.3, 0.3, 0.3, 0.01);
    }
}
