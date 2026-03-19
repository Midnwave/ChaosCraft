package com.blockforge.chaoscraft.modes.corruption.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Ambient environmental effects for the Corrupted Corruption mode.
 * Handles: dark particles, corruption fog, ambient sounds, spread edge visuals.
 */
public class AmbientEffects {

    private final ChaosCraftPlugin plugin;
    private BukkitTask ambientTask;
    private World activeWorld;
    private int centerX, centerZ;
    private int radiusBlocks;

    private boolean darkParticles;
    private boolean corruptionFog;
    private boolean ambientSounds;
    private int soundIntervalTicks;
    private int tickCounter;

    public AmbientEffects(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(World world, int centerX, int centerZ, int radiusChunks,
                      boolean darkParticles, boolean corruptionFog,
                      boolean ambientSounds, int soundIntervalTicks) {
        this.activeWorld = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radiusBlocks = radiusChunks * 16;
        this.darkParticles = darkParticles;
        this.corruptionFog = corruptionFog;
        this.ambientSounds = ambientSounds;
        this.soundIntervalTicks = soundIntervalTicks;
        this.tickCounter = 0;

        // Run every 5 ticks for particles, sound check on interval
        ambientTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickAmbient();
            }
        }.runTaskTimer(plugin, 20L, 5L);

        plugin.getLogger().info("[Corruption] Ambient effects started.");
    }

    public void stop() {
        if (ambientTask != null) {
            ambientTask.cancel();
            ambientTask = null;
        }
    }

    private void tickAmbient() {
        if (activeWorld == null) return;
        tickCounter += 5;

        for (Player player : activeWorld.getPlayers()) {
            Location loc = player.getLocation();

            // Check if player is within corruption radius
            double dx = loc.getBlockX() - centerX;
            double dz = loc.getBlockZ() - centerZ;
            if (dx * dx + dz * dz > (long) radiusBlocks * radiusBlocks) continue;

            if (darkParticles) {
                spawnDarkParticles(loc);
            }

            if (corruptionFog) {
                spawnCorruptionFog(loc);
            }
        }

        // Ambient sounds on interval
        if (ambientSounds && tickCounter % soundIntervalTicks == 0) {
            playAmbientSounds();
        }
    }

    /**
     * Dark particles drifting in the air around the player.
     */
    private void spawnDarkParticles(Location playerLoc) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int i = 0; i < 5; i++) {
            double offsetX = rand.nextDouble(-8, 8);
            double offsetY = rand.nextDouble(-2, 6);
            double offsetZ = rand.nextDouble(-8, 8);
            Location particleLoc = playerLoc.clone().add(offsetX, offsetY, offsetZ);

            // Black/dark purple dust particles
            playerLoc.getWorld().spawnParticle(Particle.DUST, particleLoc,
                    1, 0.2, 0.2, 0.2, 0,
                    new Particle.DustOptions(Color.fromRGB(20, 10, 20), 1.5f));
        }

        // Occasional soul fire flame
        if (ThreadLocalRandom.current().nextFloat() < 0.2f) {
            double offsetX = rand.nextDouble(-6, 6);
            double offsetY = rand.nextDouble(0, 4);
            double offsetZ = rand.nextDouble(-6, 6);
            playerLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                    playerLoc.clone().add(offsetX, offsetY, offsetZ),
                    1, 0.1, 0.1, 0.1, 0.005);
        }
    }

    /**
     * Dense low-lying fog at ground level.
     */
    private void spawnCorruptionFog(Location playerLoc) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int i = 0; i < 8; i++) {
            double offsetX = rand.nextDouble(-10, 10);
            double offsetZ = rand.nextDouble(-10, 10);
            double fogY = rand.nextDouble(-0.5, 1.5);
            Location fogLoc = playerLoc.clone().add(offsetX, fogY, offsetZ);

            // Use campfire signal smoke for dense fog look
            playerLoc.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, fogLoc,
                    1, 0.5, 0.1, 0.5, 0.001);
        }

        // Dark dust particles at ankle height
        for (int i = 0; i < 3; i++) {
            double offsetX = rand.nextDouble(-6, 6);
            double offsetZ = rand.nextDouble(-6, 6);
            Location dustLoc = playerLoc.clone().add(offsetX, 0.2, offsetZ);

            playerLoc.getWorld().spawnParticle(Particle.DUST, dustLoc,
                    2, 0.3, 0.05, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(30, 15, 30), 2.0f));
        }
    }

    /**
     * Play ambient corruption sounds at random positions near players.
     */
    private void playAmbientSounds() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (Player player : activeWorld.getPlayers()) {
            Location loc = player.getLocation();
            double dx = loc.getBlockX() - centerX;
            double dz = loc.getBlockZ() - centerZ;
            if (dx * dx + dz * dz > (long) radiusBlocks * radiusBlocks) continue;

            // Random offset for directional sound
            double soundX = rand.nextDouble(-15, 15);
            double soundZ = rand.nextDouble(-15, 15);
            Location soundLoc = loc.clone().add(soundX, rand.nextDouble(-3, 5), soundZ);

            float roll = rand.nextFloat();
            if (roll < 0.25f) {
                // Sculk sensor click
                player.playSound(soundLoc, Sound.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.AMBIENT,
                        0.4f, 0.5f + rand.nextFloat() * 0.5f);
            } else if (roll < 0.5f) {
                // Cave ambience
                player.playSound(soundLoc, Sound.AMBIENT_CAVE, SoundCategory.AMBIENT,
                        0.3f, 0.6f + rand.nextFloat() * 0.4f);
            } else if (roll < 0.7f) {
                // Deep rumble (respawn anchor deplete)
                player.playSound(soundLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.AMBIENT,
                        0.2f, 0.3f + rand.nextFloat() * 0.3f);
            } else if (roll < 0.85f) {
                // Warden heartbeat
                player.playSound(soundLoc, Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.AMBIENT,
                        0.3f, 0.8f + rand.nextFloat() * 0.4f);
            } else {
                // Sculk spread
                player.playSound(soundLoc, Sound.BLOCK_SCULK_SPREAD, SoundCategory.AMBIENT,
                        0.5f, 0.4f + rand.nextFloat() * 0.6f);
            }
        }
    }
}
