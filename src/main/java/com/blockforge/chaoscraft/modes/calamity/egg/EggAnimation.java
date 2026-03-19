package com.blockforge.chaoscraft.modes.calamity.egg;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityConfig;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the animated dragon egg BlockDisplay during Calamity mode.
 *
 * When Calamity starts:
 * 1. The physical dragon egg block is removed from the portal
 * 2. A purple glowing BlockDisplay replaces it
 * 3. The display shakes, rotates, and pulses with particles
 * 4. Crying obsidian ItemDisplay cubes orbit around it
 *    (not a flat circle — varying X/Y axes for a 3D orbit)
 * 5. The egg persists throughout the entire fight, orbiting the arena
 * 6. Between bosses: egg "charges up" with particle explosions
 * 7. Boss spawn: egg explodes with particles/sounds, stretches, then boss appears
 */
public class EggAnimation {

    private final ChaosCraftPlugin plugin;
    private final CalamityConfig config;

    // The main egg BlockDisplay
    private BlockDisplay eggDisplay;
    // Orbiting crying obsidian cubes
    private final List<ItemDisplay> orbitCubes = new ArrayList<>();

    // State
    private boolean active = false;
    private Location center;
    private BukkitTask animationTask;
    private long tickCounter = 0;

    // Animation parameters
    private boolean charging = false;
    private boolean exploding = false;

    public EggAnimation(ChaosCraftPlugin plugin, CalamityConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ========================
    // Lifecycle
    // ========================

    /**
     * Spawn the animated egg at the given location (where the dragon egg was).
     */
    public void start(Location eggLocation) {
        this.center = eggLocation.clone();
        this.active = true;
        this.tickCounter = 0;

        World world = center.getWorld();
        if (world == null) return;

        // Spawn the main egg BlockDisplay (dragon egg block, glowing purple)
        eggDisplay = world.spawn(center, BlockDisplay.class, display -> {
            display.setBlock(Material.DRAGON_EGG.createBlockData());
            display.setGlowColorOverride(Color.fromRGB(128, 0, 255));
            display.setGlowing(true);
            display.setBrightness(new Display.Brightness(15, 15));

            // Initial transformation: centered, slightly scaled up
            display.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f), // translation (center the block)
                    new AxisAngle4f(0, 0, 1, 0),        // left rotation
                    new Vector3f(1.0f, 1.0f, 1.0f),     // scale
                    new AxisAngle4f(0, 0, 1, 0)          // right rotation
            ));
            display.setInterpolationDuration(2);
            display.setInterpolationDelay(0);
        });

        // Spawn orbiting crying obsidian cubes
        int cubeCount = config.getEggOrbitCubeCount();
        for (int i = 0; i < cubeCount; i++) {
            final int index = i;
            ItemDisplay cube = world.spawn(center, ItemDisplay.class, display -> {
                display.setItemStack(new org.bukkit.inventory.ItemStack(Material.CRYING_OBSIDIAN));
                display.setGlowColorOverride(Color.fromRGB(200, 0, 255));
                display.setGlowing(true);
                display.setBrightness(new Display.Brightness(15, 15));

                // Small scale
                display.setTransformation(new Transformation(
                        new Vector3f(-0.15f, -0.15f, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                display.setInterpolationDuration(2);
                display.setInterpolationDelay(0);
            });
            orbitCubes.add(cube);
        }

        // Start animation loop
        startAnimation();
    }

    public void stop() {
        active = false;

        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }

        // Remove all display entities
        if (eggDisplay != null && eggDisplay.isValid()) {
            eggDisplay.remove();
            eggDisplay = null;
        }
        for (ItemDisplay cube : orbitCubes) {
            if (cube != null && cube.isValid()) {
                cube.remove();
            }
        }
        orbitCubes.clear();
    }

    // ========================
    // Animation loop
    // ========================

    private void startAnimation() {
        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || center == null || center.getWorld() == null) {
                    cancel();
                    return;
                }

                tickCounter++;
                animateEgg();
                animateOrbitCubes();
                spawnAmbientParticles();

                if (charging) {
                    animateCharging();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Animate the main egg BlockDisplay: rotation, subtle shaking, breathing scale.
     * Also teleports the egg to the current center so it orbits the arena.
     */
    private void animateEgg() {
        if (eggDisplay == null || !eggDisplay.isValid()) return;

        // Teleport the egg to the current center (so it orbits the arena, not stays at spawn)
        eggDisplay.teleport(center);

        // Rotation: slow continuous Y rotation
        float yRot = (float) (tickCounter * 0.02);
        // Shake: small random offset that intensifies when charging
        float shakeIntensity = charging ? 0.08f : 0.02f;
        float shakeX = (float) (Math.sin(tickCounter * 0.3) * shakeIntensity);
        float shakeZ = (float) (Math.cos(tickCounter * 0.4) * shakeIntensity);

        // Breathing scale: subtly pulses between 0.95 and 1.05
        float breathe = 1.0f + (float) (Math.sin(tickCounter * 0.05) * 0.05);
        if (charging) {
            breathe = 1.0f + (float) (Math.sin(tickCounter * 0.15) * 0.15); // More intense when charging
        }

        eggDisplay.setTransformation(new Transformation(
                new Vector3f(-0.5f + shakeX, -0.5f, -0.5f + shakeZ),
                new AxisAngle4f(yRot, 0, 1, 0),
                new Vector3f(breathe, breathe, breathe),
                new AxisAngle4f(0, 0, 1, 0)
        ));
        eggDisplay.setInterpolationDuration(3);
        eggDisplay.setInterpolationDelay(0);
    }

    /**
     * Animate orbiting crying obsidian cubes in 3D orbital paths.
     * Each cube has a unique orbit plane (varying X/Y axis tilt) so
     * they don't all orbit in the same flat circle.
     */
    private void animateOrbitCubes() {
        if (orbitCubes.isEmpty()) return;

        double orbitRadius = config.getEggOrbitRadius();
        double orbitSpeed = config.getEggOrbitSpeed();
        int cubeCount = orbitCubes.size();

        for (int i = 0; i < cubeCount; i++) {
            ItemDisplay cube = orbitCubes.get(i);
            if (cube == null || !cube.isValid()) continue;

            // Each cube gets a unique phase offset and axis tilt
            double phaseOffset = (2.0 * Math.PI * i) / cubeCount;
            double angle = tickCounter * orbitSpeed + phaseOffset;

            // Axis tilt: each cube tilts its orbit plane differently
            // This creates the "not just a straight circle" effect
            double tiltAngle = (Math.PI / 6) * (i % 3 + 1); // 30°, 60°, 90° tilts
            double wobble = Math.sin(tickCounter * 0.03 + i) * 0.3; // Slow wobble

            // Calculate 3D position on tilted orbit
            double x = orbitRadius * Math.cos(angle);
            double y = orbitRadius * Math.sin(angle) * Math.sin(tiltAngle + wobble);
            double z = orbitRadius * Math.sin(angle) * Math.cos(tiltAngle + wobble);

            // Apply vertical bob
            y += Math.sin(tickCounter * 0.08 + i * 0.5) * 0.5;

            Location cubeLoc = center.clone().add(x, y, z);
            cube.teleport(cubeLoc);

            // Small self-rotation on the cubes
            float cubeRot = (float) (tickCounter * 0.1 + i);
            cube.setTransformation(new Transformation(
                    new Vector3f(-0.15f, -0.15f, -0.15f),
                    new AxisAngle4f(cubeRot, 0.5f, 1.0f, 0.3f),
                    new Vector3f(0.3f, 0.3f, 0.3f),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            cube.setInterpolationDuration(2);
            cube.setInterpolationDelay(0);
        }
    }

    /**
     * Ambient particles around the egg.
     */
    private void spawnAmbientParticles() {
        if (tickCounter % 3 != 0) return; // Every 3 ticks

        World world = center.getWorld();
        if (world == null) return;

        // Purple dust swirl around egg
        world.spawnParticle(Particle.DUST, center, 3,
                0.8, 0.8, 0.8, 0,
                new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f));

        // Occasional end rod particles rising
        if (tickCounter % 10 == 0) {
            world.spawnParticle(Particle.END_ROD, center, 2,
                    0.3, 0.3, 0.3, 0.02);
        }

        // Enchantment particles spiral
        if (tickCounter % 5 == 0) {
            double spiralAngle = tickCounter * 0.2;
            double spiralX = Math.cos(spiralAngle) * 1.5;
            double spiralZ = Math.sin(spiralAngle) * 1.5;
            double spiralY = (tickCounter % 40) / 40.0 * 2.0 - 1.0;
            world.spawnParticle(Particle.ENCHANT, center.clone().add(spiralX, spiralY, spiralZ),
                    5, 0.1, 0.1, 0.1, 0.5);
        }
    }

    // ========================
    // Charging & Boss Spawn
    // ========================

    /**
     * Start the charging animation between bosses.
     * The egg glows brighter, orbits faster, particles intensify.
     */
    public void startCharging() {
        charging = true;
    }

    public void stopCharging() {
        charging = false;
    }

    /**
     * Extra particles and effects during charging.
     */
    private void animateCharging() {
        World world = center.getWorld();
        if (world == null) return;

        // Intense purple particle burst
        if (tickCounter % 2 == 0) {
            world.spawnParticle(Particle.DUST, center, 8,
                    1.5, 1.5, 1.5, 0,
                    new Particle.DustOptions(Color.fromRGB(200, 0, 255), 2.0f));
        }

        // Lightning-like particles shooting out
        if (tickCounter % 10 == 0) {
            double angle = Math.random() * Math.PI * 2;
            double len = 2 + Math.random() * 3;
            Location end = center.clone().add(
                    Math.cos(angle) * len,
                    (Math.random() - 0.5) * 4,
                    Math.sin(angle) * len
            );
            // Line of particles from egg to end point
            int steps = 10;
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                Location point = center.clone().add(
                        (end.getX() - center.getX()) * t,
                        (end.getY() - center.getY()) * t,
                        (end.getZ() - center.getZ()) * t
                );
                world.spawnParticle(Particle.DUST, point, 1,
                        0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 0, 200), 0.8f));
            }
        }

        // Sound pulses
        if (tickCounter % 20 == 0) {
            world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f,
                    0.5f + (float) (Math.random() * 0.5));
        }
    }

    /**
     * Play the boss spawn explosion animation.
     * The egg rapidly stretches/distorts, explodes with particles and sound,
     * then returns to normal after the boss has spawned.
     *
     * @param callback Called after the explosion finishes (use to spawn boss)
     */
    public void playBossSpawnExplosion(Runnable callback) {
        if (!active || center == null) {
            if (callback != null) callback.run();
            return;
        }

        World world = center.getWorld();
        if (world == null) {
            if (callback != null) callback.run();
            return;
        }

        charging = false;

        // Phase 1: Rapid shrink + intense glow (20 ticks)
        new BukkitRunnable() {
            int phase = 0;

            @Override
            public void run() {
                if (!active || eggDisplay == null || !eggDisplay.isValid()) {
                    cancel();
                    if (callback != null) callback.run();
                    return;
                }

                phase++;

                if (phase <= 20) {
                    // Shrink down while vibrating intensely
                    float scale = 1.0f - (phase / 20.0f) * 0.5f;
                    float shake = (float) (Math.random() * 0.15);
                    eggDisplay.setTransformation(new Transformation(
                            new Vector3f(-0.5f + shake, -0.5f, -0.5f + shake),
                            new AxisAngle4f(phase * 0.5f, 0, 1, 0),
                            new Vector3f(scale, scale, scale),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    eggDisplay.setInterpolationDuration(1);
                    eggDisplay.setInterpolationDelay(0);

                    // Suction particles (moving toward egg)
                    world.spawnParticle(Particle.DUST, center, 15,
                            3, 3, 3, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 0, 255), 1.5f));

                    world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.3f,
                            0.5f + phase * 0.05f);

                } else if (phase == 21) {
                    // EXPLOSION!
                    // Rapidly scale up
                    eggDisplay.setTransformation(new Transformation(
                            new Vector3f(-1.0f, -1.0f, -1.0f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(2.0f, 2.0f, 2.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    eggDisplay.setInterpolationDuration(5);
                    eggDisplay.setInterpolationDelay(0);

                    // Massive particle explosion
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 0, 0, 0, 0);
                    world.spawnParticle(Particle.DUST, center, 100,
                            5, 5, 5, 0,
                            new Particle.DustOptions(Color.fromRGB(128, 0, 255), 3.0f));
                    world.spawnParticle(Particle.DUST, center, 80,
                            4, 4, 4, 0,
                            new Particle.DustOptions(Color.fromRGB(0, 200, 255), 2.5f));
                    world.spawnParticle(Particle.REVERSE_PORTAL, center, 60,
                            3, 3, 3, 0.5);
                    world.spawnParticle(Particle.FLASH, center, 5, 0, 0, 0, 0);

                    // Explosion sounds
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                    world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
                    world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.6f);

                } else if (phase <= 30) {
                    // Cool-down: scale back to normal, particles dissipate
                    float t = (phase - 21) / 9.0f;
                    float scale = 2.0f - t * 1.0f;
                    eggDisplay.setTransformation(new Transformation(
                            new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scale, scale, scale),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    eggDisplay.setInterpolationDuration(3);
                    eggDisplay.setInterpolationDelay(0);

                    world.spawnParticle(Particle.DUST, center, 20,
                            3 - t * 2, 3 - t * 2, 3 - t * 2, 0,
                            new Particle.DustOptions(Color.fromRGB(128, 0, 255), 2.0f - t));

                } else {
                    // Done — return to normal
                    cancel();
                    if (callback != null) callback.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ========================
    // Movement (orbit around arena)
    // ========================

    /**
     * Move the egg's center position. Called from CalamityMode tick
     * to make the egg orbit around the arena during the fight.
     */
    public void updateCenter(Location newCenter) {
        this.center = newCenter.clone();
    }

    /**
     * Get the current center of the egg for reference.
     */
    public Location getCenter() {
        return center;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCharging() {
        return charging;
    }
}
