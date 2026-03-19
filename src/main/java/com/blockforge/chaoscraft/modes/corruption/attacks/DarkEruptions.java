package com.blockforge.chaoscraft.modes.corruption.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Corruption Mode — DARK ERUPTIONS (Corruption Waves & Pulses)
 * 15 corruption-themed BlockDisplay attacks featuring shockwaves, pulses,
 * waves, and eruptions of dark energy.
 *
 * Color palette:
 * - Dark purple: RGB(45, 0, 64)
 * - Crimson: RGB(74, 0, 0)
 * - Void blue: RGB(10, 0, 48)
 *
 * Materials: SCULK, DEEPSLATE, BLACKSTONE, CRYING_OBSIDIAN, OBSIDIAN, COAL_BLOCK, NETHERRACK
 * Sounds: BLOCK_SCULK_SPREAD, BLOCK_SCULK_BREAK, BLOCK_DEEPSLATE_BREAK,
 *         ENTITY_WARDEN_HEARTBEAT, BLOCK_RESPAWN_ANCHOR_DEPLETE
 */
public final class DarkEruptions {

    private DarkEruptions() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CorruptionShockwave(plugin));
        registry.register(new DarkPulse(plugin));
        registry.register(new VoidWave(plugin));
        registry.register(new CorruptionTsunami(plugin));
        registry.register(new ShadowRipple(plugin));
        registry.register(new DarkEnergyBurst(plugin));
        registry.register(new CorruptionHeartbeat(plugin));
        registry.register(new VoidTremor(plugin));
        registry.register(new ShadowCascade(plugin));
        registry.register(new DarkRadiation(plugin));
        registry.register(new CorruptionGeyser(plugin));
        registry.register(new VoidDischarge(plugin));
        registry.register(new ShadowStorm(plugin));
        registry.register(new CorruptionNova(plugin));
        registry.register(new DarkTide(plugin));
    }

    // ================================================================
    // 46. CORRUPTION SHOCKWAVE — 16 blocks expand outward in flat ring,
    //     damage on pass. Sculk blocks blast outward from center.
    // ================================================================
    public static class CorruptionShockwave extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 16;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> angles = new ArrayList<>();
        private float currentRadius = 0;
        private static final float MAX_RADIUS = 12.0f;
        private static final int EXPAND_TICKS = 60;

        public CorruptionShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_shockwave", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLOCK_COUNT;
                angles.add(angle);
                Location spawnLoc = center.clone();
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, Material.SCULK);
                handle.scale(1.2f, 0.4f, 1.2f)
                      .glow(45, 0, 64)
                      .interpolation(3, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive <= EXPAND_TICKS) {
                float progress = (float) ticksAlive / EXPAND_TICKS;
                currentRadius = progress * MAX_RADIUS;

                for (int i = 0; i < BLOCK_COUNT; i++) {
                    double angle = angles.get(i);
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    Location blockLoc = c.clone().add(x, 0, z);
                    blocks.get(i).entity().teleport(blockLoc);

                    // Rotate as they expand
                    float rotAngle = ticksAlive * 0.1f + (float)(angle);
                    blocks.get(i).rotate(rotAngle, 0, 1, 0);
                }

                // Damage at the expanding ring edge
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.particleRing(c.clone(), currentRadius, Particle.DUST, 32,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.8f));
                }
            }

            // Shockwave trail particles
            if (ticksAlive % 3 == 0 && ticksAlive <= EXPAND_TICKS) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location dustLoc = c.clone().add(
                            Math.cos(angle) * currentRadius, 0.3,
                            Math.sin(angle) * currentRadius);
                    DisplayBuilder.dustParticles(dustLoc, 3, 0.4, 10, 0, 48, 1.5f);
                }
            }

            // Ambient sculk sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionShockwave(plugin); }
    }

    // ================================================================
    // 47. DARK PULSE — 12 blocks pulse outward/inward rhythmically,
    //     synchronized with warden heartbeat sound.
    // ================================================================
    public static class DarkPulse extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 12;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> angles = new ArrayList<>();
        private static final float MIN_RADIUS = 1.5f;
        private static final float MAX_RADIUS = 7.0f;
        private static final int PULSE_PERIOD = 40; // ticks per pulse cycle

        public DarkPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_pulse", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(320);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLOCK_COUNT;
                angles.add(angle);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(center.clone(), Material.DEEPSLATE);
                handle.scale(0.8f, 1.5f, 0.8f)
                      .glow(74, 0, 0)
                      .interpolation(4, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Sinusoidal pulse: expand then contract
            float pulsePhase = (float)(ticksAlive % PULSE_PERIOD) / PULSE_PERIOD;
            float radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * (float)(0.5 + 0.5 * Math.sin(pulsePhase * Math.PI * 2));

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = angles.get(i);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location blockLoc = c.clone().add(x, 0.5, z);
                blocks.get(i).entity().teleport(blockLoc);

                // Scale pulse: blocks grow at max radius, shrink at min
                float scaleFactor = 0.6f + 0.6f * (radius - MIN_RADIUS) / (MAX_RADIUS - MIN_RADIUS);
                blocks.get(i).scale(scaleFactor, 1.5f * scaleFactor, scaleFactor);

                // Face outward
                float faceAngle = (float) angle;
                blocks.get(i).rotate(faceAngle, 0, 1, 0);
            }

            // Heartbeat sound on each pulse peak
            if (ticksAlive % PULSE_PERIOD == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.5f);
            }

            // Crimson dust at expanding edge
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location dustLoc = c.clone().add(Math.cos(angle) * radius, 1.0, Math.sin(angle) * radius);
                    DisplayBuilder.dustParticles(dustLoc, 4, 0.3, 74, 0, 0, 1.3f);
                }
            }

            // Sculk ambience
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkPulse(plugin); }
    }

    // ================================================================
    // 48. VOID WAVE — 20 blocks tall wave wall, sweeps forward 10 blocks.
    //     A towering wall of obsidian and sculk crashes across the arena.
    // ================================================================
    public static class VoidWave extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 20;
        private final List<BlockDisplayHandle> waveBlocks = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> yOffsets = new ArrayList<>();
        private float sweepProgress = 0;
        private static final float WAVE_WIDTH = 10.0f;
        private static final float WAVE_HEIGHT = 8.0f;
        private static final float SWEEP_DISTANCE = 10.0f;
        private static final int SWEEP_TICKS = 80;

        public VoidWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_wave", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double xOff = -WAVE_WIDTH / 2 + (WAVE_WIDTH / (BLOCK_COUNT - 1.0)) * i;
                // Wave shape: taller in middle, shorter at edges
                double heightFactor = 1.0 - Math.abs((double) i / (BLOCK_COUNT - 1) - 0.5) * 1.6;
                double yOff = WAVE_HEIGHT * Math.max(0.2, heightFactor);
                xOffsets.add(xOff);
                yOffsets.add(yOff);

                Material mat = (i % 3 == 0) ? Material.OBSIDIAN : (i % 3 == 1) ? Material.SCULK : Material.BLACKSTONE;
                Location spawnLoc = center.clone().add(xOff, yOff * 0.5, -SWEEP_DISTANCE / 2);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, mat);
                handle.scale(0.8f, (float) yOff, 1.5f)
                      .glow(10, 0, 48)
                      .interpolation(3, 0);
                waveBlocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive > SWEEP_TICKS) return;

            sweepProgress = (float) ticksAlive / SWEEP_TICKS;
            float zOffset = -SWEEP_DISTANCE / 2 + sweepProgress * SWEEP_DISTANCE;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double xOff = xOffsets.get(i);
                double yOff = yOffsets.get(i);

                // Wave crest motion: blocks bob up and down as they sweep
                double waveMotion = Math.sin(ticksAlive * 0.12 + i * 0.4) * 0.8;
                Location blockLoc = c.clone().add(xOff, yOff * 0.5 + waveMotion, zOffset);
                waveBlocks.get(i).entity().teleport(blockLoc);

                // Tilt forward like a cresting wave
                float tiltAngle = 0.2f + sweepProgress * 0.4f;
                waveBlocks.get(i).rotate(tiltAngle, 1, 0, 0);
            }

            // Dark spray particles at wave crest
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double rx = c.getX() + (-WAVE_WIDTH / 2 + Math.random() * WAVE_WIDTH);
                    Location sprayLoc = new Location(w, rx, c.getY() + WAVE_HEIGHT * 0.8, c.getZ() + zOffset);
                    DisplayBuilder.dustParticles(sprayLoc, 4, 0.5, 45, 0, 64, 1.6f);
                }
            }

            // Crashing sound as wave advances
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 3, zOffset), Sound.BLOCK_SCULK_BREAK, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidWave(plugin); }
    }

    // ================================================================
    // 49. CORRUPTION TSUNAMI — 18 blocks massive wave, crashes forward
    //     with debris trailing behind. Two-phase: rise then crash.
    // ================================================================
    public static class CorruptionTsunami extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 18;
        private final List<BlockDisplayHandle> waveBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> debrisBlocks = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private static final float WAVE_WIDTH = 12.0f;
        private static final float PEAK_HEIGHT = 10.0f;
        private static final int RISE_TICKS = 40;
        private static final int CRASH_TICKS = 30;
        private static final float CRASH_DISTANCE = 8.0f;

        public CorruptionTsunami(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_tsunami", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(52.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(52.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double xOff = -WAVE_WIDTH / 2 + (WAVE_WIDTH / (BLOCK_COUNT - 1.0)) * i;
                xOffsets.add(xOff);

                Material mat = (i % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.DEEPSLATE;
                Location spawnLoc = center.clone().add(xOff, 0, -4);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, mat);
                handle.scale(1.0f, 1.5f, 2.0f)
                      .glow(45, 0, 64)
                      .interpolation(3, 0);
                waveBlocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            // Debris blocks trailing behind
            for (int i = 0; i < 6; i++) {
                Location debrisLoc = center.clone().add(
                        Math.random() * WAVE_WIDTH - WAVE_WIDTH / 2, Math.random() * 2, -5 - Math.random() * 3);
                Material debrisMat = (i % 2 == 0) ? Material.COAL_BLOCK : Material.NETHERRACK;
                BlockDisplayHandle debris = displayBuilder.spawnBlock(debrisLoc, debrisMat);
                debris.scale(0.5f, 0.5f, 0.5f)
                      .glow(74, 0, 0)
                      .interpolation(2, 0);
                debrisBlocks.add(debris);
                spawnedEntities.add(debris.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive <= RISE_TICKS) {
                // Phase 1: Rise up
                float riseProgress = (float) ticksAlive / RISE_TICKS;
                float height = riseProgress * PEAK_HEIGHT;

                for (int i = 0; i < BLOCK_COUNT; i++) {
                    double xOff = xOffsets.get(i);
                    float stagger = (float) Math.sin(i * 0.5) * 0.5f;
                    Location blockLoc = c.clone().add(xOff, height + stagger, -4);
                    waveBlocks.get(i).entity().teleport(blockLoc);
                    waveBlocks.get(i).scale(1.0f, 1.5f + riseProgress * 2.0f, 2.0f);

                    // Lean back as it rises
                    float leanAngle = -0.3f * riseProgress;
                    waveBlocks.get(i).rotate(leanAngle, 1, 0, 0);
                }

                // Rising particles
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, height, -4), 8, 2.0, 10, 0, 48, 1.8f);
                }

            } else if (ticksAlive <= RISE_TICKS + CRASH_TICKS) {
                // Phase 2: Crash forward
                float crashProgress = (float)(ticksAlive - RISE_TICKS) / CRASH_TICKS;
                float zForward = -4 + crashProgress * CRASH_DISTANCE;
                float height = PEAK_HEIGHT * (1.0f - crashProgress * crashProgress); // Parabolic crash

                for (int i = 0; i < BLOCK_COUNT; i++) {
                    double xOff = xOffsets.get(i);
                    Location blockLoc = c.clone().add(xOff, Math.max(0, height), zForward);
                    waveBlocks.get(i).entity().teleport(blockLoc);

                    // Curl over as it crashes
                    float curlAngle = crashProgress * 1.2f;
                    waveBlocks.get(i).rotate(curlAngle, 1, 0, 0);
                }

                // Debris follows behind
                for (int i = 0; i < debrisBlocks.size(); i++) {
                    float debrisDelay = 0.3f + i * 0.08f;
                    float debrisZ = -5 + Math.max(0, crashProgress - debrisDelay) * CRASH_DISTANCE;
                    float debrisY = Math.max(0, height * 0.5f);
                    Location debrisLoc = c.clone().add(
                            Math.random() * WAVE_WIDTH - WAVE_WIDTH / 2, debrisY, debrisZ);
                    debrisBlocks.get(i).entity().teleport(debrisLoc);
                    debrisBlocks.get(i).rotate(ticksAlive * 0.15f, 1, 0.5f, 0.3f);
                }

                // Ground impact at crash completion
                if (crashProgress >= 0.95f && ticksAlive == RISE_TICKS + CRASH_TICKS) {
                    Location impactLoc = c.clone().add(0, 0, zForward);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.particleRing(impactLoc, 5.0, Particle.DUST, 40,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.0f));
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
                    w.spawnParticle(Particle.BLOCK, impactLoc, 40, 3.0, 0.5, 3.0, 0.3,
                            Material.DEEPSLATE.createBlockData());
                }
            }

            // Ambient rumble
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionTsunami(plugin); }
    }

    // ================================================================
    // 50. SHADOW RIPPLE — 14 blocks ground level concentric circles
    //     expanding outward, creating dark water-like ripple effect.
    // ================================================================
    public static class ShadowRipple extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 14;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Integer> ringAssignment = new ArrayList<>(); // which ring (0-2) each block is in
        private final List<Double> angles = new ArrayList<>();
        private static final int RING_COUNT = 3;
        private static final float[] RING_SPEEDS = {0.15f, 0.10f, 0.07f};
        private final float[] ringRadii = {0, 0, 0};

        public ShadowRipple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_ripple", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Distribute blocks across 3 rings: 4, 5, 5
            int[] blocksPerRing = {4, 5, 5};
            int blockIndex = 0;

            for (int ring = 0; ring < RING_COUNT; ring++) {
                for (int i = 0; i < blocksPerRing[ring]; i++) {
                    double angle = (2 * Math.PI * i) / blocksPerRing[ring];
                    angles.add(angle);
                    ringAssignment.add(ring);

                    Material mat = (ring == 0) ? Material.SCULK : (ring == 1) ? Material.BLACKSTONE : Material.DEEPSLATE;
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(center.clone(), mat);
                    handle.scale(1.0f, 0.2f, 1.0f)
                          .glow(45, 0, 64)
                          .interpolation(3, 0);
                    blocks.add(handle);
                    spawnedEntities.add(handle.entity());
                    blockIndex++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Expand each ring at staggered speeds
            for (int ring = 0; ring < RING_COUNT; ring++) {
                int delay = ring * 15; // stagger ring spawns
                if (ticksAlive > delay) {
                    ringRadii[ring] += RING_SPEEDS[ring];
                    if (ringRadii[ring] > 10.0f) ringRadii[ring] = 0; // reset for ripple loop
                }
            }

            for (int i = 0; i < BLOCK_COUNT; i++) {
                int ring = ringAssignment.get(i);
                double angle = angles.get(i);
                float radius = ringRadii[ring];

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                // Subtle vertical bob
                double yBob = Math.sin(ticksAlive * 0.08 + angle) * 0.15;
                Location blockLoc = c.clone().add(x, yBob, z);
                blocks.get(i).entity().teleport(blockLoc);

                // Scale diminishes as ring expands
                float scaleFade = Math.max(0.3f, 1.0f - radius / 10.0f);
                blocks.get(i).scale(1.0f * scaleFade, 0.2f, 1.0f * scaleFade);

                // Subtle wobble rotation
                float wobble = (float) Math.sin(ticksAlive * 0.05 + i) * 0.1f;
                blocks.get(i).rotate(wobble, 0, 1, 0);
            }

            // Dark ripple particles
            if (ticksAlive % 6 == 0) {
                for (int ring = 0; ring < RING_COUNT; ring++) {
                    if (ringRadii[ring] > 0.5f) {
                        DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), ringRadii[ring],
                                Particle.DUST, 16,
                                new Particle.DustOptions(Color.fromRGB(10, 0, 48), 1.0f));
                    }
                }
            }

            // Water-like ambient
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowRipple(plugin); }
    }

    // ================================================================
    // 51. DARK ENERGY BURST — 16 blocks explode outward from center
    //     in all directions (3D sphere expansion).
    // ================================================================
    public static class DarkEnergyBurst extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 16;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Vector3f> directions = new ArrayList<>();
        private float burstRadius = 0;
        private static final float MAX_BURST_RADIUS = 10.0f;
        private static final float BURST_SPEED = 0.3f;
        private boolean peaked = false;

        public DarkEnergyBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_energy_burst", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Distribute points on a sphere using golden spiral
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double y = 1.0 - (2.0 * i / (BLOCK_COUNT - 1));
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                float dx = (float)(Math.cos(theta) * radiusAtY);
                float dy = (float) y;
                float dz = (float)(Math.sin(theta) * radiusAtY);
                directions.add(new Vector3f(dx, dy, dz));

                Material mat = (i % 4 == 0) ? Material.CRYING_OBSIDIAN :
                               (i % 4 == 1) ? Material.SCULK :
                               (i % 4 == 2) ? Material.COAL_BLOCK : Material.OBSIDIAN;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(center.clone(), mat);
                handle.scale(0.8f, 0.8f, 0.8f)
                      .glow(74, 0, 0)
                      .interpolation(2, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!peaked) {
                burstRadius += BURST_SPEED;
                if (burstRadius >= MAX_BURST_RADIUS) peaked = true;
            }

            for (int i = 0; i < BLOCK_COUNT; i++) {
                Vector3f dir = directions.get(i);
                double x = dir.x * burstRadius;
                double y = dir.y * burstRadius;
                double z = dir.z * burstRadius;
                Location blockLoc = c.clone().add(x, y + 3, z); // offset Y so sphere is above ground
                blocks.get(i).entity().teleport(blockLoc);

                // Spin while flying outward
                float spinAngle = ticksAlive * 0.12f + i * 0.5f;
                blocks.get(i).rotate(spinAngle, dir.x, dir.y, dir.z);

                // Trails
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(blockLoc, 2, 0.2, 45, 0, 64, 1.0f);
                }
            }

            // Energy core particles at center
            if (ticksAlive % 5 == 0 && !peaked) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 10, 0.8, 74, 0, 0, 2.0f);
            }

            // Burst sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkEnergyBurst(plugin); }
    }

    // ================================================================
    // 52. CORRUPTION HEARTBEAT — 10 blocks pulse scale in sync with
    //     warden heartbeat. Blocks swell and shrink like a beating heart.
    // ================================================================
    public static class CorruptionHeartbeat extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 10;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> baseAngles = new ArrayList<>();
        private static final float BASE_RADIUS = 3.0f;
        private static final int BEAT_INTERVAL = 30; // ticks per heartbeat

        public CorruptionHeartbeat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_heartbeat", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLOCK_COUNT;
                baseAngles.add(angle);

                double x = Math.cos(angle) * BASE_RADIUS;
                double z = Math.sin(angle) * BASE_RADIUS;
                Location spawnLoc = center.clone().add(x, 1.0, z);

                Material mat = (i % 2 == 0) ? Material.SCULK : Material.DEEPSLATE;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, mat);
                handle.scale(1.0f, 1.5f, 1.0f)
                      .glow(74, 0, 0)
                      .interpolation(4, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Double-beat pattern: two quick beats then pause (like real heartbeat)
            int beatPhase = ticksAlive % BEAT_INTERVAL;
            float beatScale;
            if (beatPhase < 4) {
                // First beat (swell)
                beatScale = 1.0f + 0.8f * (float) Math.sin((beatPhase / 4.0f) * Math.PI);
            } else if (beatPhase < 8) {
                // Brief rest
                beatScale = 1.0f;
            } else if (beatPhase < 12) {
                // Second beat (smaller swell)
                beatScale = 1.0f + 0.5f * (float) Math.sin(((beatPhase - 8) / 4.0f) * Math.PI);
            } else {
                // Rest until next cycle
                beatScale = 1.0f;
            }

            float radiusScale = BASE_RADIUS * (0.8f + 0.4f * beatScale);

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = baseAngles.get(i);
                double x = Math.cos(angle) * radiusScale;
                double z = Math.sin(angle) * radiusScale;
                float yBob = (beatScale - 1.0f) * 0.5f;
                Location blockLoc = c.clone().add(x, 1.0 + yBob, z);
                blocks.get(i).entity().teleport(blockLoc);
                blocks.get(i).scale(beatScale, 1.5f * beatScale, beatScale);

                // Face center
                blocks.get(i).rotate((float) angle + (float) Math.PI, 0, 1, 0);
            }

            // Heartbeat sound
            if (beatPhase == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.3f, 0.5f);
            }

            // Crimson pulse particles on beat
            if (beatPhase < 4) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 6, radiusScale * 0.4, 74, 0, 0, 1.5f);
            }

            // Dark ambient
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionHeartbeat(plugin); }
    }

    // ================================================================
    // 53. VOID TREMOR — 15 blocks shake violently on ground, dark
    //     crack particles. Earthquake-like ground disturbance.
    // ================================================================
    public static class VoidTremor extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 15;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> baseX = new ArrayList<>();
        private final List<Double> baseZ = new ArrayList<>();
        private final List<Float> baseY = new ArrayList<>();

        public VoidTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tremor", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.0 + Math.random() * 6.0;
                double bx = Math.cos(angle) * dist;
                double bz = Math.sin(angle) * dist;
                float by = (float)(Math.random() * 0.3);
                baseX.add(bx);
                baseZ.add(bz);
                baseY.add(by);

                Material mat = (i % 3 == 0) ? Material.BLACKSTONE :
                               (i % 3 == 1) ? Material.DEEPSLATE : Material.NETHERRACK;
                Location spawnLoc = center.clone().add(bx, by, bz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, mat);
                handle.scale(1.2f, 0.6f, 1.2f)
                      .glow(10, 0, 48)
                      .interpolation(1, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Intensity ramps up then down
            float intensity = Math.min(1.0f, ticksAlive / 30.0f);
            if (ticksAlive > 180) intensity = Math.max(0, 1.0f - (ticksAlive - 180) / 60.0f);

            for (int i = 0; i < BLOCK_COUNT; i++) {
                // Violent shaking: random jitter scaled by intensity
                double shakeX = (Math.random() - 0.5) * 0.8 * intensity;
                double shakeZ = (Math.random() - 0.5) * 0.8 * intensity;
                float shakeY = (float)((Math.random() - 0.5) * 1.2 * intensity);

                Location blockLoc = c.clone().add(
                        baseX.get(i) + shakeX,
                        baseY.get(i) + Math.max(0, shakeY),
                        baseZ.get(i) + shakeZ);
                blocks.get(i).entity().teleport(blockLoc);

                // Jittery rotation
                float jitterAngle = (float)((Math.random() - 0.5) * 0.4 * intensity);
                blocks.get(i).rotate(jitterAngle, (float)(Math.random()), 0, (float)(Math.random()));
            }

            // Dark crack particles along the ground
            if (ticksAlive % 3 == 0 && intensity > 0.3f) {
                for (int i = 0; i < (int)(8 * intensity); i++) {
                    double crackAngle = Math.random() * Math.PI * 2;
                    double crackDist = Math.random() * 7.0;
                    Location crackLoc = c.clone().add(
                            Math.cos(crackAngle) * crackDist, 0.1,
                            Math.sin(crackAngle) * crackDist);
                    DisplayBuilder.dustParticles(crackLoc, 3, 0.2, 10, 0, 48, 1.2f);
                }
            }

            // Rumbling sounds intensify
            if (ticksAlive % 12 == 0) {
                float volume = 0.4f + 0.8f * intensity;
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, volume, 0.3f + 0.3f * intensity);
            }

            // Warden heartbeat at peak intensity
            if (ticksAlive % 30 == 0 && intensity > 0.7f) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTremor(plugin); }
    }

    // ================================================================
    // 54. SHADOW CASCADE — 12 blocks fall in domino sequence, each
    //     triggers a mini shockwave on impact.
    // ================================================================
    public static class ShadowCascade extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 12;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Boolean> fallen = new ArrayList<>();
        private final List<Double> lineX = new ArrayList<>();
        private final List<Double> lineZ = new ArrayList<>();
        private static final float START_Y = 15.0f;
        private static final float FALL_SPEED = 0.5f;
        private static final int DELAY_PER_BLOCK = 8; // ticks between each domino

        public ShadowCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_cascade", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(42.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Domino line: arc from one side to other
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double t = (double) i / (BLOCK_COUNT - 1);
                double angle = -Math.PI / 3 + t * (2 * Math.PI / 3); // 120 degree arc
                double radius = 6.0;
                double lx = Math.cos(angle) * radius;
                double lz = Math.sin(angle) * radius;
                lineX.add(lx);
                lineZ.add(lz);
                yPositions.add(START_Y);
                fallen.add(false);

                Material mat = (i % 2 == 0) ? Material.OBSIDIAN : Material.SCULK;
                Location spawnLoc = center.clone().add(lx, START_Y, lz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, mat);
                handle.scale(1.0f, 2.0f, 1.0f)
                      .glow(45, 0, 64)
                      .interpolation(2, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                int triggerTick = i * DELAY_PER_BLOCK;
                if (ticksAlive < triggerTick) continue;
                if (fallen.get(i)) continue;

                // Fall
                float y = yPositions.get(i) - FALL_SPEED;
                yPositions.set(i, y);

                Location blockLoc = c.clone().add(lineX.get(i), y, lineZ.get(i));
                blocks.get(i).entity().teleport(blockLoc);

                // Tilt forward as it falls (domino lean)
                float leanProgress = 1.0f - (y / START_Y);
                float leanAngle = leanProgress * 1.5f;
                blocks.get(i).rotate(leanAngle, 0, 0, 1);

                // Trail particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(blockLoc.clone().add(0, 1, 0), 2, 0.3, 74, 0, 0, 1.0f);
                }

                // Ground impact
                if (y <= 0) {
                    yPositions.set(i, 0f);
                    fallen.set(i, true);

                    Location impactLoc = c.clone().add(lineX.get(i), 0, lineZ.get(i));
                    triggerImpactDamage(impactLoc);

                    // Mini shockwave ring
                    DisplayBuilder.particleRing(impactLoc, 3.5, Particle.DUST, 20,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.5f));
                    w.spawnParticle(Particle.BLOCK, impactLoc, 20, 1.5, 0.3, 1.5, 0.2,
                            Material.DEEPSLATE.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.9f, 0.5f + i * 0.05f);
                }
            }

            // Ambient tension
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowCascade(plugin); }
    }

    // ================================================================
    // 55. DARK RADIATION — 20 small blocks orbit at varying distances
    //     like electrons around a nucleus. Multiple orbital shells.
    // ================================================================
    public static class DarkRadiation extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 20;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Float> orbitRadii = new ArrayList<>();
        private final List<Float> orbitSpeeds = new ArrayList<>();
        private final List<Float> orbitAngles = new ArrayList<>();
        private final List<Float> orbitTilts = new ArrayList<>(); // Y offset for 3D orbits

        public DarkRadiation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_radiation", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3 orbital shells: inner(6), middle(7), outer(7)
            float[] shellRadii = {2.5f, 4.5f, 7.0f};
            int[] blocksPerShell = {6, 7, 7};
            float[] shellSpeeds = {0.12f, 0.08f, 0.05f};
            int blockIndex = 0;

            for (int shell = 0; shell < 3; shell++) {
                for (int i = 0; i < blocksPerShell[shell]; i++) {
                    float startAngle = (float)(2 * Math.PI * i / blocksPerShell[shell]);
                    float tilt = (float)((Math.random() - 0.5) * 2.0); // random Y tilt for each electron
                    orbitRadii.add(shellRadii[shell]);
                    orbitSpeeds.add(shellSpeeds[shell] + (float)(Math.random() * 0.02 - 0.01));
                    orbitAngles.add(startAngle);
                    orbitTilts.add(tilt);

                    Material mat = (shell == 0) ? Material.SCULK :
                                   (shell == 1) ? Material.COAL_BLOCK : Material.BLACKSTONE;
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(center.clone(), mat);
                    handle.scale(0.4f, 0.4f, 0.4f)
                          .glow(10, 0, 48)
                          .interpolation(2, 0);
                    blocks.add(handle);
                    spawnedEntities.add(handle.entity());
                    blockIndex++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float angle = orbitAngles.get(i) + orbitSpeeds.get(i);
                orbitAngles.set(i, angle);

                float radius = orbitRadii.get(i);
                float tilt = orbitTilts.get(i);

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                // Tilted orbital plane
                double y = Math.sin(angle + tilt) * (radius * 0.3) + 3.0;

                Location blockLoc = c.clone().add(x, y, z);
                blocks.get(i).entity().teleport(blockLoc);

                // Spin each electron
                float spinAngle = ticksAlive * 0.15f + i * 0.8f;
                blocks.get(i).rotate(spinAngle, 0, 1, 0);
            }

            // Radiation trail particles
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 4; i++) {
                    int idx = (int)(Math.random() * BLOCK_COUNT);
                    Location trailLoc = blocks.get(idx).entity().getLocation();
                    DisplayBuilder.dustParticles(trailLoc, 2, 0.15, 45, 0, 64, 0.8f);
                }
            }

            // Nucleus glow at center
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 5, 0.5, 74, 0, 0, 1.5f);
            }

            // Geiger-counter-like clicks
            if (ticksAlive % 18 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.4f, 1.5f + (float)(Math.random() * 0.5));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkRadiation(plugin); }
    }

    // ================================================================
    // 56. CORRUPTION GEYSER — 14 blocks erupt upward, spray dark
    //     particles at peak. Column eruption with scatter at top.
    // ================================================================
    public static class CorruptionGeyser extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 14;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> ejectionSpeeds = new ArrayList<>();
        private final List<Double> scatterX = new ArrayList<>();
        private final List<Double> scatterZ = new ArrayList<>();
        private final List<Boolean> peaked = new ArrayList<>();
        private static final float PEAK_HEIGHT = 14.0f;
        private static final float GRAVITY = 0.015f;

        public CorruptionGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_geyser", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(340);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(46.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                yPositions.add(0f);
                ejectionSpeeds.add(0.5f + (float)(Math.random() * 0.4));
                // Slight initial scatter
                scatterX.add((Math.random() - 0.5) * 1.5);
                scatterZ.add((Math.random() - 0.5) * 1.5);
                peaked.add(false);

                Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN :
                               (i % 3 == 1) ? Material.SCULK : Material.NETHERRACK;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(center.clone(), mat);
                handle.scale(0.7f, 1.0f, 0.7f)
                      .glow(45, 0, 64)
                      .interpolation(2, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                int launchDelay = i * 3; // staggered launch
                if (ticksAlive < launchDelay) continue;

                int localTick = ticksAlive - launchDelay;
                float speed = ejectionSpeeds.get(i);

                // Physics: y = v*t - 0.5*g*t^2
                float y = speed * localTick - 0.5f * GRAVITY * localTick * localTick;

                if (y < 0 && localTick > 10) {
                    y = 0; // landed
                    if (!peaked.get(i)) {
                        peaked.set(i, true);
                        Location impactLoc = c.clone().add(scatterX.get(i) * 3, 0, scatterZ.get(i) * 3);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
                        w.spawnParticle(Particle.BLOCK, impactLoc, 15, 1.0, 0.2, 1.0, 0.2,
                                Material.SCULK.createBlockData());
                    }
                }

                // After peak, scatter outward
                boolean pastPeak = speed * localTick < 0.5f * GRAVITY * localTick * localTick && localTick > 10;
                double xOff = scatterX.get(i) * (pastPeak ? 3.0 : 1.0);
                double zOff = scatterZ.get(i) * (pastPeak ? 3.0 : 1.0);

                Location blockLoc = c.clone().add(xOff, y, zOff);
                blocks.get(i).entity().teleport(blockLoc);

                // Tumble rotation
                float tumble = localTick * 0.1f * (i % 2 == 0 ? 1 : -1);
                blocks.get(i).rotate(tumble, 1, 0.3f, 0);
            }

            // Geyser spray particles at base
            if (ticksAlive % 2 == 0 && ticksAlive < 60) {
                for (int i = 0; i < 5; i++) {
                    Location sprayLoc = c.clone().add(
                            (Math.random() - 0.5) * 1.5,
                            Math.random() * 3,
                            (Math.random() - 0.5) * 1.5);
                    DisplayBuilder.dustParticles(sprayLoc, 3, 0.3, 10, 0, 48, 1.5f);
                }
            }

            // Peak spray particles
            if (ticksAlive % 4 == 0 && ticksAlive > 20 && ticksAlive < 80) {
                DisplayBuilder.dustParticles(c.clone().add(0, PEAK_HEIGHT * 0.8, 0), 8, 2.0, 45, 0, 64, 2.0f);
            }

            // Eruption sounds
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.7f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionGeyser(plugin); }
    }

    // ================================================================
    // 57. VOID DISCHARGE — 16 blocks form lightning bolt shape, flash
    //     bright then fade. Jagged vertical lightning strike.
    // ================================================================
    public static class VoidDischarge extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 16;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Location> boltPositions = new ArrayList<>();
        private boolean struck = false;
        private int strikeTickOffset = 0;

        public VoidDischarge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_discharge", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build jagged lightning bolt path from top to bottom
            double currentX = 0;
            double currentZ = 0;
            float segmentHeight = 16.0f / BLOCK_COUNT;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float y = 16.0f - i * segmentHeight;
                // Jagged: random horizontal offset at each segment
                currentX += (Math.random() - 0.5) * 2.0;
                currentZ += (Math.random() - 0.5) * 2.0;
                // Clamp to prevent wandering too far
                currentX = Math.max(-4, Math.min(4, currentX));
                currentZ = Math.max(-4, Math.min(4, currentZ));

                Location boltLoc = center.clone().add(currentX, y, currentZ);
                boltPositions.add(boltLoc);

                Material mat = (i % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.SCULK;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(boltLoc, mat);
                handle.scale(0.5f, segmentHeight * 0.9f, 0.5f)
                      .glow(10, 0, 48)
                      .brightness(15, 15)
                      .interpolation(1, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            // Pre-strike: blocks start invisible (tiny scale)
            for (BlockDisplayHandle handle : blocks) {
                handle.scale(0.05f, 0.05f, 0.05f);
            }

            // Charge-up sound
            DisplayBuilder.playSound(center.clone().add(0, 16, 0), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Charge-up phase (0-30 ticks): warning particles
            if (ticksAlive < 30) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 16, 0), 8, 2.0, 10, 0, 48, 1.5f);
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 5, 1.0, 74, 0, 0, 1.2f);
                }
                // Warden heartbeat as charge
                if (ticksAlive == 10 || ticksAlive == 20) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.8f);
                }
                return;
            }

            // Strike phase (tick 30): instant full-size flash
            if (!struck) {
                struck = true;
                strikeTickOffset = ticksAlive;

                float segmentHeight = 16.0f / BLOCK_COUNT;
                for (int i = 0; i < BLOCK_COUNT; i++) {
                    blocks.get(i).scale(0.5f, segmentHeight * 0.9f, 0.5f);
                    // Maximum brightness flash
                    blocks.get(i).glow(200, 180, 255); // bright void flash
                }

                // Lightning crack sound
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0f, 1.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);

                // Impact at base
                triggerImpactDamage(c.clone());

                // Flash particles along entire bolt
                for (Location boltLoc : boltPositions) {
                    DisplayBuilder.dustParticles(boltLoc, 5, 0.5, 45, 0, 64, 2.0f);
                }
            }

            // Fade phase: blocks shrink and dim after strike
            int fadeTick = ticksAlive - strikeTickOffset;
            if (fadeTick > 0 && fadeTick < 40) {
                float fadeProgress = fadeTick / 40.0f;
                float fadeScale = Math.max(0.05f, 1.0f - fadeProgress);
                float segmentHeight = 16.0f / BLOCK_COUNT;

                for (int i = 0; i < BLOCK_COUNT; i++) {
                    blocks.get(i).scale(0.5f * fadeScale, segmentHeight * 0.9f * fadeScale, 0.5f * fadeScale);

                    // Jitter during fade (residual energy)
                    if (fadeTick < 20) {
                        float jitter = (float)((Math.random() - 0.5) * 0.3 * (1.0 - fadeProgress));
                        Location jitterLoc = boltPositions.get(i).clone().add(jitter, 0, jitter);
                        blocks.get(i).entity().teleport(jitterLoc);
                    }
                }

                // Return to void blue glow
                if (fadeTick == 5) {
                    for (BlockDisplayHandle handle : blocks) {
                        handle.glow(10, 0, 48);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidDischarge(plugin); }
    }

    // ================================================================
    // 58. SHADOW STORM — 18 blocks swirl in tornado formation.
    //     Rising spiral that grows wider at the top.
    // ================================================================
    public static class ShadowStorm extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 18;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Float> heightSlots = new ArrayList<>();
        private final List<Float> angleOffsets = new ArrayList<>();
        private static final float TORNADO_HEIGHT = 12.0f;
        private static final float BASE_RADIUS = 1.5f;
        private static final float TOP_RADIUS = 6.0f;
        private static final float SPIN_SPEED = 0.08f;

        public ShadowStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_storm", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float heightSlot = (TORNADO_HEIGHT * i) / (BLOCK_COUNT - 1);
                float angleOffset = (float)(2 * Math.PI * i) / BLOCK_COUNT;
                heightSlots.add(heightSlot);
                angleOffsets.add(angleOffset);

                Material mat = (i % 4 == 0) ? Material.OBSIDIAN :
                               (i % 4 == 1) ? Material.SCULK :
                               (i % 4 == 2) ? Material.DEEPSLATE : Material.BLACKSTONE;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(center.clone(), mat);
                // Lower blocks smaller, upper blocks bigger
                float sizeFactor = 0.5f + (heightSlot / TORNADO_HEIGHT) * 0.8f;
                handle.scale(sizeFactor, sizeFactor, sizeFactor)
                      .glow(45, 0, 64)
                      .interpolation(3, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float height = heightSlots.get(i);
                float angleBase = angleOffsets.get(i) + ticksAlive * SPIN_SPEED;

                // Radius increases with height (cone shape)
                float heightRatio = height / TORNADO_HEIGHT;
                float radius = BASE_RADIUS + (TOP_RADIUS - BASE_RADIUS) * heightRatio;

                // Wobble: slight radius oscillation
                float wobble = (float) Math.sin(ticksAlive * 0.05 + i * 0.3) * 0.5f;
                radius += wobble;

                double x = Math.cos(angleBase) * radius;
                double z = Math.sin(angleBase) * radius;

                // Slight vertical oscillation
                float yOsc = (float) Math.sin(ticksAlive * 0.06 + i * 0.5) * 0.3f;
                Location blockLoc = c.clone().add(x, height + yOsc, z);
                blocks.get(i).entity().teleport(blockLoc);

                // Blocks spin on their own axis too
                float selfSpin = ticksAlive * 0.15f + i;
                blocks.get(i).rotate(selfSpin, 0, 1, 0);
            }

            // Tornado debris particles
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    float randomHeight = (float)(Math.random() * TORNADO_HEIGHT);
                    float heightRatio = randomHeight / TORNADO_HEIGHT;
                    float radius = BASE_RADIUS + (TOP_RADIUS - BASE_RADIUS) * heightRatio;
                    double angle = Math.random() * Math.PI * 2;
                    Location dustLoc = c.clone().add(
                            Math.cos(angle) * radius, randomHeight,
                            Math.sin(angle) * radius);
                    DisplayBuilder.dustParticles(dustLoc, 3, 0.4, 10, 0, 48, 1.2f);
                }
            }

            // Wind howl sounds
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, TORNADO_HEIGHT / 2, 0),
                        Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.3f + (float)(Math.random() * 0.3));
            }

            // Ground rumble
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowStorm(plugin); }
    }

    // ================================================================
    // 59. CORRUPTION NOVA — 12 blocks expand as sphere then collapse
    //     with massive impact. Two-phase: expand then implode.
    // ================================================================
    public static class CorruptionNova extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 12;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Vector3f> directions = new ArrayList<>();
        private float currentRadius = 0;
        private static final float MAX_RADIUS = 8.0f;
        private static final int EXPAND_TICKS = 50;
        private static final int HOLD_TICKS = 20;
        private static final int COLLAPSE_TICKS = 15;
        private boolean collapsed = false;

        public CorruptionNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_nova", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(54.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(380);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(54.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Distribute on sphere
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double y = 1.0 - (2.0 * i / (BLOCK_COUNT - 1));
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                float dx = (float)(Math.cos(theta) * radiusAtY);
                float dy = (float) y;
                float dz = (float)(Math.sin(theta) * radiusAtY);
                directions.add(new Vector3f(dx, dy, dz));

                Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN :
                               (i % 3 == 1) ? Material.SCULK : Material.OBSIDIAN;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(center.clone().add(0, 4, 0), mat);
                handle.scale(1.0f, 1.0f, 1.0f)
                      .glow(74, 0, 0)
                      .interpolation(3, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float radius;

            if (ticksAlive <= EXPAND_TICKS) {
                // Phase 1: Expand
                float progress = (float) ticksAlive / EXPAND_TICKS;
                radius = progress * MAX_RADIUS;
                // Blocks grow as they expand
                float blockScale = 0.5f + progress * 1.0f;
                for (BlockDisplayHandle handle : blocks) {
                    handle.scale(blockScale, blockScale, blockScale);
                }
            } else if (ticksAlive <= EXPAND_TICKS + HOLD_TICKS) {
                // Phase 2: Hold at max — ominous pause
                radius = MAX_RADIUS;
                // Pulsing glow during hold
                int holdTick = ticksAlive - EXPAND_TICKS;
                if (holdTick % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);
                }
            } else if (ticksAlive <= EXPAND_TICKS + HOLD_TICKS + COLLAPSE_TICKS) {
                // Phase 3: Collapse inward rapidly
                float collapseProgress = (float)(ticksAlive - EXPAND_TICKS - HOLD_TICKS) / COLLAPSE_TICKS;
                radius = MAX_RADIUS * (1.0f - collapseProgress * collapseProgress); // accelerating collapse
                // Blocks shrink as they collapse
                float blockScale = Math.max(0.3f, 1.5f * (1.0f - collapseProgress));
                for (BlockDisplayHandle handle : blocks) {
                    handle.scale(blockScale, blockScale, blockScale);
                }

                // Switch glow to dark purple during collapse
                if (ticksAlive == EXPAND_TICKS + HOLD_TICKS + 1) {
                    for (BlockDisplayHandle handle : blocks) {
                        handle.glow(45, 0, 64);
                    }
                }
            } else {
                radius = 0;
                // Collapse impact — only trigger once
                if (!collapsed) {
                    collapsed = true;
                    Location impactLoc = c.clone().add(0, 4, 0);
                    triggerImpactDamage(impactLoc);

                    // Massive impact particles
                    DisplayBuilder.dustParticles(impactLoc, 30, 3.0, 74, 0, 0, 2.5f);
                    DisplayBuilder.dustParticles(impactLoc, 20, 2.0, 45, 0, 64, 2.0f);
                    DisplayBuilder.particleRing(c.clone(), 6.0, Particle.DUST, 40,
                            new Particle.DustOptions(Color.fromRGB(10, 0, 48), 2.0f));

                    DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0f, 0.2f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
                    w.spawnParticle(Particle.BLOCK, impactLoc, 50, 2.0, 2.0, 2.0, 0.3,
                            Material.SCULK.createBlockData());
                }
                return;
            }

            currentRadius = radius;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                Vector3f dir = directions.get(i);
                double x = dir.x * currentRadius;
                double y = dir.y * currentRadius + 4; // elevated center
                double z = dir.z * currentRadius;
                Location blockLoc = c.clone().add(x, y, z);
                blocks.get(i).entity().teleport(blockLoc);

                // Orbit rotation
                float spinAngle = ticksAlive * 0.1f + i * 0.5f;
                blocks.get(i).rotate(spinAngle, dir.x, dir.y, dir.z);
            }

            // Energy trails
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 4; i++) {
                    int idx = (int)(Math.random() * BLOCK_COUNT);
                    Location trailLoc = blocks.get(idx).entity().getLocation();
                    DisplayBuilder.dustParticles(trailLoc, 3, 0.3, 74, 0, 0, 1.3f);
                }
            }

            // Buildup sounds
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionNova(plugin); }
    }

    // ================================================================
    // 60. DARK TIDE — 15 blocks low wave sweeping across ground left
    //     to right. Undulating ground-level dark energy flow.
    // ================================================================
    public static class DarkTide extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 15;
        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private float sweepX = 0;
        private static final float SWEEP_WIDTH = 16.0f;
        private static final float TIDE_DEPTH = 8.0f;
        private static final int SWEEP_TICKS = 100;

        public DarkTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_tide", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double zOff = -TIDE_DEPTH / 2 + (TIDE_DEPTH / (BLOCK_COUNT - 1.0)) * i;
                zOffsets.add(zOff);

                Location spawnLoc = center.clone().add(-SWEEP_WIDTH / 2, 0.3, zOff);
                Material mat = (i % 3 == 0) ? Material.SCULK :
                               (i % 3 == 1) ? Material.DEEPSLATE : Material.COAL_BLOCK;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, mat);
                handle.scale(1.5f, 0.6f, 1.2f)
                      .glow(45, 0, 64)
                      .interpolation(3, 0);
                blocks.add(handle);
                spawnedEntities.add(handle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive > SWEEP_TICKS) return;

            float sweepProgress = (float) ticksAlive / SWEEP_TICKS;
            sweepX = -SWEEP_WIDTH / 2 + sweepProgress * SWEEP_WIDTH;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double zOff = zOffsets.get(i);

                // Wave undulation: each block has a phase offset based on its Z position
                double wavePhase = ticksAlive * 0.12 + i * 0.6;
                double waveY = 0.3 + Math.sin(wavePhase) * 0.8;
                double waveXOffset = Math.cos(wavePhase) * 0.3;

                Location blockLoc = c.clone().add(sweepX + waveXOffset, waveY, zOff);
                blocks.get(i).entity().teleport(blockLoc);

                // Rolling rotation (like water tumbling)
                float rollAngle = (float)(wavePhase * 0.5);
                blocks.get(i).rotate(rollAngle, 0, 0, 1);

                // Scale pulse with wave
                float scaleY = 0.4f + 0.4f * (float)(0.5 + 0.5 * Math.sin(wavePhase));
                blocks.get(i).scale(1.5f, scaleY, 1.2f);
            }

            // Dark tide particles along the wave front
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 5; i++) {
                    double rz = c.getZ() + (-TIDE_DEPTH / 2 + Math.random() * TIDE_DEPTH);
                    Location foamLoc = new Location(w, c.getX() + sweepX, c.getY() + 0.5, rz);
                    DisplayBuilder.dustParticles(foamLoc, 3, 0.4, 10, 0, 48, 1.3f);
                }
            }

            // Ground shadow particles behind the wave
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 3; i++) {
                    double trailX = sweepX - 1.0 - Math.random() * 3.0;
                    double trailZ = c.getZ() + (-TIDE_DEPTH / 2 + Math.random() * TIDE_DEPTH);
                    Location trailLoc = new Location(w, c.getX() + trailX, c.getY() + 0.1, trailZ);
                    DisplayBuilder.dustParticles(trailLoc, 2, 0.3, 45, 0, 64, 0.8f);
                }
            }

            // Flowing sound
            if (ticksAlive % 18 == 0) {
                DisplayBuilder.playSound(c.clone().add(sweepX, 0, 0), Sound.BLOCK_SCULK_BREAK, 0.6f, 0.5f);
            }

            // Occasional deep pulse
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkTide(plugin); }
    }
}
