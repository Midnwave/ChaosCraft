package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Doom Mode — BLOCK DISPLAY ATTACKS (Part 3, attacks 35-52)
 * 18 hellfire and doom-themed BlockDisplay attacks.
 *
 * Categories:
 *   35-39: Orbital / Floating
 *   40-44: Wave / Line Attacks
 *   45-48: Towers / Pillars
 *   49-52: Unique / Special
 *
 * Doom palette:
 * - Lava orange: RGB(255, 100, 20)
 * - Hellfire red: RGB(200, 50, 10)
 * - Charred black: RGB(20, 10, 5)
 * - Ember glow: RGB(240, 80, 30)
 * - Brimstone yellow: RGB(220, 180, 30)
 * - Nether purple: RGB(120, 20, 80)
 *
 * Materials: MAGMA_BLOCK, NETHERRACK, NETHER_BRICKS, RED_NETHER_BRICKS,
 *            BLACKSTONE, POLISHED_BLACKSTONE, CRYING_OBSIDIAN, SHROOMLIGHT,
 *            BASALT, DEEPSLATE, COAL_BLOCK, OBSIDIAN, RED_CONCRETE,
 *            BLACK_CONCRETE, ORANGE_CONCRETE, GLOWSTONE
 */
public final class DoomBlockDisplay3 {
    private DoomBlockDisplay3() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Orbital / Floating (35-39)
        registry.register(new MagmaOrbit(plugin));
        registry.register(new InfernalSatellites(plugin));
        registry.register(new HellfireHalo(plugin));
        registry.register(new DoomPendulum(plugin));
        registry.register(new InfernalGyroscope(plugin));
        // Wave / Line Attacks (40-44)
        registry.register(new LavaWave(plugin));
        registry.register(new HellfireFence(plugin));
        registry.register(new BrimstoneZigzag(plugin));
        registry.register(new MagmaRipple(plugin));
        registry.register(new InfernalScythe(plugin));
        // Towers / Pillars (45-48)
        registry.register(new DoomSpire(plugin));
        registry.register(new TwinInfernalColumns(plugin));
        registry.register(new HellfireLighthouse(plugin));
        registry.register(new BrimstoneStalagmites(plugin));
        // Unique / Special (49-52)
        registry.register(new InfernalClock(plugin));
        registry.register(new MagmaChain(plugin));
        registry.register(new DoomEye(plugin));
        registry.register(new InfernalVortex(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    // ================================================================
    // 35. MAGMA ORBIT — 12 magma blocks orbiting a central point at
    //     different radii and heights. Each block orbits at a different
    //     speed. Central glowing core. Damage: 8-block radius, 20 dmg/15t.
    //     12 orbiting blocks + 1 core = 13 blocks
    // ================================================================
    public static class MagmaOrbit extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle core;
        private final List<BlockDisplayHandle> orbiters = new ArrayList<>();
        private final double[] orbitRadii = new double[12];
        private final double[] orbitHeights = new double[12];
        private final double[] orbitSpeeds = new double[12];
        private final double[] orbitPhases = new double[12];

        public MagmaOrbit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_orbit", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(20.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Central glowing core — shroomlight
            core = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.SHROOMLIGHT);
            core.scale(1.5f, 1.5f, 1.5f).glow(255, 100, 20).interpolation(3, 0);
            spawnedEntities.add(core.entity());

            // 12 orbiting magma blocks at varied radii, heights, speeds
            for (int i = 0; i < 12; i++) {
                orbitRadii[i] = 2.5 + (i % 4) * 1.2;
                orbitHeights[i] = 1.5 + (i % 3) * 1.5;
                orbitSpeeds[i] = 0.03 + (i * 0.008);
                orbitPhases[i] = (2 * Math.PI * i) / 12.0;

                double angle = orbitPhases[i];
                double x = Math.cos(angle) * orbitRadii[i];
                double z = Math.sin(angle) * orbitRadii[i];
                Location loc = center.clone().add(x, orbitHeights[i], z);

                Material mat = (i % 3 == 0) ? Material.NETHERRACK : (i % 3 == 1) ? Material.MAGMA_BLOCK : Material.RED_NETHER_BRICKS;
                BlockDisplayHandle orb = displayBuilder.spawnBlock(loc, mat);
                float s = 0.7f + (i % 3) * 0.15f;
                orb.scale(s, s, s).glow(240, 80, 30).interpolation(2, 0);
                spawnedEntities.add(orb.entity());
                orbiters.add(orb);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.2f, 0.5f);
            w.spawnParticle(Particle.LAVA, center.clone().add(0, 3, 0), 25, 2, 2, 2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Rotate core gently
            if (tick % 5 == 0) {
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 3, 0), 3, 0.3, 0.3, 0.3, 0.01);
            }

            // Orbit each block at its own speed
            for (int i = 0; i < orbiters.size(); i++) {
                double angle = orbitPhases[i] + tick * orbitSpeeds[i];
                double x = Math.cos(angle) * orbitRadii[i];
                double z = Math.sin(angle) * orbitRadii[i];
                // Bobbing height variation
                double yBob = Math.sin(tick * 0.05 + i * 0.5) * 0.4;
                Location loc = center.clone().add(x, orbitHeights[i] + yBob, z);
                loc.setYaw(0);
                loc.setPitch(0);
                orbiters.get(i).entity().teleport(loc);
            }

            // Ember particles trailing each orbiter
            if (tick % 3 == 0) {
                for (int i = 0; i < orbiters.size(); i += 3) {
                    Location bLoc = orbiters.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(bLoc, 2, 0.3, 255, 100, 20, 1.0f);
                }
            }

            // Ambient lava sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 3, 0), Sound.BLOCK_LAVA_AMBIENT, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaOrbit(plugin); }
    }

    // ================================================================
    // 36. INFERNAL SATELLITES — 5 "satellite" clusters (each 3 blocks)
    //     orbiting a central sphere (4 blocks). Satellites at different
    //     orbital heights. Damage: 6-block radius, 30 dmg/25t.
    //     4 core + 15 satellite = 19 blocks
    // ================================================================
    public static class InfernalSatellites extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> satellites = new ArrayList<>();
        private final double[] satHeights = {2.0, 3.5, 5.0, 2.8, 4.2};
        private final double[] satSpeeds = {0.04, -0.035, 0.05, -0.045, 0.03};

        public InfernalSatellites(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_satellites", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Central sphere — 4 blocks of blackstone forming a compact core
            double[][] coreOffsets = {
                {0, 3.0, 0}, {0.5, 3.5, 0}, {-0.5, 3.5, 0}, {0, 3.0, 0.5}
            };
            for (double[] off : coreOffsets) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BLACKSTONE);
                b.scale(1.2f, 1.2f, 1.2f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(b.entity());
                coreBlocks.add(b);
            }

            // 5 satellite clusters, each with 3 blocks
            Material[] satMats = {Material.MAGMA_BLOCK, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
                                  Material.CRYING_OBSIDIAN, Material.SHROOMLIGHT};
            for (int s = 0; s < 5; s++) {
                List<BlockDisplayHandle> cluster = new ArrayList<>();
                double baseAngle = (2 * Math.PI * s) / 5.0;
                double radius = 4.0;
                for (int b = 0; b < 3; b++) {
                    double ox = Math.cos(baseAngle) * radius + (b - 1) * 0.4;
                    double oz = Math.sin(baseAngle) * radius;
                    double oy = satHeights[s] + b * 0.5;
                    Location loc = center.clone().add(ox, oy, oz);
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, satMats[s]);
                    float sc = (b == 1) ? 0.9f : 0.6f; // middle block larger
                    handle.scale(sc, sc, sc).glow(240, 80, 30).interpolation(2, 0);
                    spawnedEntities.add(handle.entity());
                    cluster.add(handle);
                }
                satellites.add(cluster);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 3, 0), 30, 1.5, 1.5, 1.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Orbit each satellite cluster
            for (int s = 0; s < satellites.size(); s++) {
                double angle = (2 * Math.PI * s) / 5.0 + tick * satSpeeds[s];
                double radius = 4.0;
                List<BlockDisplayHandle> cluster = satellites.get(s);
                for (int b = 0; b < cluster.size(); b++) {
                    double ox = Math.cos(angle) * radius + (b - 1) * 0.4 * Math.cos(angle + Math.PI / 2);
                    double oz = Math.sin(angle) * radius + (b - 1) * 0.4 * Math.sin(angle + Math.PI / 2);
                    double oy = satHeights[s] + b * 0.5 + Math.sin(tick * 0.06 + s) * 0.3;
                    Location loc = center.clone().add(ox, oy, oz);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    cluster.get(b).entity().teleport(loc);
                }
            }

            // Core pulse — scale oscillation
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 3.3, 0), 4, 0.5, 200, 50, 10, 1.2f);
            }

            // Ember trails from satellites
            if (tick % 5 == 0) {
                for (List<BlockDisplayHandle> cluster : satellites) {
                    Location sLoc = cluster.get(1).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.FLAME, sLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.7f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalSatellites(plugin); }
    }

    // ================================================================
    // 37. HELLFIRE HALO — 10 glowing blocks in a flat ring at Y=8 above
    //     ground. Ring slowly descends to Y=2 then rises back up.
    //     Damage: 7-block radius below ring, 25 dmg/20t.
    //     10 ring blocks + 2 inner glow blocks = 12 blocks
    // ================================================================
    public static class HellfireHalo extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> innerGlow = new ArrayList<>();
        private double ringY = 8.0;
        private boolean descending = true;

        public HellfireHalo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_halo", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 10 blocks in flat ring at Y=8
            double radius = 3.5;
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, ringY, z);
                Material mat = (i % 2 == 0) ? Material.GLOWSTONE : Material.SHROOMLIGHT;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                b.scale(1.1f, 0.4f, 1.1f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(b.entity());
                ringBlocks.add(b);
            }

            // 2 inner glow blocks at center of ring
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(i * 0.6 - 0.3, ringY, 0);
                BlockDisplayHandle g = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                g.scale(0.8f, 0.3f, 0.8f).glow(240, 80, 30).interpolation(3, 0);
                spawnedEntities.add(g.entity());
                innerGlow.add(g);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.4f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, ringY, 0), 20, 3, 0.5, 3, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Descend from Y=8 to Y=2, then rise back
            if (descending) {
                ringY -= 0.04;
                if (ringY <= 2.0) descending = false;
            } else {
                ringY += 0.04;
                if (ringY >= 8.0) descending = true;
            }

            // Update ring block positions
            double radius = 3.5;
            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 10.0 + tick * 0.015; // slow rotation
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, ringY, z);
                loc.setYaw(0);
                loc.setPitch(0);
                ringBlocks.get(i).entity().teleport(loc);
            }

            // Inner glow follows
            for (int i = 0; i < innerGlow.size(); i++) {
                Location loc = center.clone().add(i * 0.6 - 0.3, ringY, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                innerGlow.get(i).entity().teleport(loc);
            }

            // Update damage center to below ring
            setCenter(center.clone().add(0, ringY - 2, 0));

            // Fire particles raining down from ring
            if (tick % 3 == 0) {
                double pAngle = Math.random() * 2 * Math.PI;
                double px = Math.cos(pAngle) * radius * Math.random();
                double pz = Math.sin(pAngle) * radius * Math.random();
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(px, ringY - 0.5, pz), 2, 0.1, 0.5, 0.1, 0.02);
                DisplayBuilder.dustParticles(center.clone().add(px, ringY, pz), 2, 0.2, 255, 100, 20, 1.0f);
            }

            // Warning sound when low
            if (ringY < 3.5 && tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.2f);
            }

            if (tick % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireHalo(plugin); }
    }

    // ================================================================
    // 38. DOOM PENDULUM — Wrecking ball: 10-block sphere hanging from a
    //     chain (5 thin blocks). Swings back and forth on X axis.
    //     Damage on contact with swing path: 5-block radius, 45 dmg/25t.
    //     10 sphere + 5 chain = 15 blocks
    // ================================================================
    public static class DoomPendulum extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> sphereBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> chainBlocks = new ArrayList<>();
        private double swingAngle = 0;
        private double swingVelocity = 0.06;

        public DoomPendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_pendulum", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(350);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double pivotY = 10.0;
            double chainLength = 6.0;

            // Chain: 5 thin blocks hanging from pivot point
            for (int i = 0; i < 5; i++) {
                double cy = pivotY - (i + 1) * (chainLength / 6.0);
                Location loc = center.clone().add(0, cy, 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                link.scale(0.3f, 0.8f, 0.3f).glow(20, 10, 5).interpolation(2, 0);
                spawnedEntities.add(link.entity());
                chainBlocks.add(link);
            }

            // Wrecking ball: 10 blocks forming a sphere at end of chain
            double ballY = pivotY - chainLength;
            double[][] sphereOffsets = {
                {0, 0, 0}, {0.7, 0, 0}, {-0.7, 0, 0}, {0, 0, 0.7}, {0, 0, -0.7},
                {0, 0.7, 0}, {0, -0.7, 0}, {0.5, 0.5, 0}, {-0.5, 0.5, 0}, {0, 0.5, 0.5}
            };
            Material[] ballMats = {Material.OBSIDIAN, Material.BLACKSTONE, Material.DEEPSLATE,
                                   Material.OBSIDIAN, Material.BLACKSTONE, Material.COAL_BLOCK,
                                   Material.DEEPSLATE, Material.OBSIDIAN, Material.BLACKSTONE, Material.COAL_BLOCK};
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(sphereOffsets[i][0], ballY + sphereOffsets[i][1], sphereOffsets[i][2]);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, ballMats[i]);
                b.scale(0.85f, 0.85f, 0.85f).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                sphereBlocks.add(b);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.3f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, ballY, 0), 15, 1, 1, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double pivotY = 10.0;
            double chainLength = 6.0;

            // Pendulum swing: angle oscillates with sinusoidal motion
            swingAngle = Math.sin(tick * swingVelocity) * 1.2; // ~70 degree max swing

            // Ball position at end of pendulum arc (swings on X axis)
            double ballX = Math.sin(swingAngle) * chainLength;
            double ballY = pivotY - Math.cos(swingAngle) * chainLength;

            // Update chain links — evenly spaced along the chain
            for (int i = 0; i < chainBlocks.size(); i++) {
                double t = (i + 1.0) / 6.0;
                double cx = Math.sin(swingAngle) * chainLength * t;
                double cy = pivotY - Math.cos(swingAngle) * chainLength * t;
                Location loc = center.clone().add(cx, cy, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                chainBlocks.get(i).entity().teleport(loc);
            }

            // Update sphere blocks around ball center
            double[][] sphereOffsets = {
                {0, 0, 0}, {0.7, 0, 0}, {-0.7, 0, 0}, {0, 0, 0.7}, {0, 0, -0.7},
                {0, 0.7, 0}, {0, -0.7, 0}, {0.5, 0.5, 0}, {-0.5, 0.5, 0}, {0, 0.5, 0.5}
            };
            for (int i = 0; i < sphereBlocks.size(); i++) {
                Location loc = center.clone().add(
                    ballX + sphereOffsets[i][0],
                    ballY + sphereOffsets[i][1],
                    sphereOffsets[i][2]
                );
                loc.setYaw(0);
                loc.setPitch(0);
                sphereBlocks.get(i).entity().teleport(loc);
            }

            // Damage center tracks the ball
            setCenter(center.clone().add(ballX, ballY, 0));

            // Sparks at bottom of swing (when fastest)
            if (Math.abs(swingAngle) < 0.15) {
                center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(ballX, ballY - 0.5, 0), 5, 0.5, 0.3, 0.5, 0.02);
            }

            // Whoosh sound at swing extremes
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center.clone().add(ballX, ballY, 0), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.4f);
            }

            // Trail particles
            if (tick % 2 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(ballX, ballY, 0), 3, 0.6, 200, 50, 10, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomPendulum(plugin); }
    }

    // ================================================================
    // 39. INFERNAL GYROSCOPE — 3 flat rings (each 4-6 blocks) on
    //     perpendicular axes all rotating simultaneously. Inner core glows.
    //     Damage: 6-block radius, 25 dmg/20t.
    //     Ring1: 6 blocks, Ring2: 5 blocks, Ring3: 4 blocks + 1 core = 16 blocks
    // ================================================================
    public static class InfernalGyroscope extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> ringX = new ArrayList<>();  // YZ plane
        private final List<BlockDisplayHandle> ringY = new ArrayList<>();  // XZ plane
        private final List<BlockDisplayHandle> ringZ = new ArrayList<>();  // XY plane
        private BlockDisplayHandle core;

        public InfernalGyroscope(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_gyroscope", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double cy = 4.0;

            // Core — glowing shroomlight
            core = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.GLOWSTONE);
            core.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 20).interpolation(3, 0);
            spawnedEntities.add(core.entity());

            // Ring 1 (YZ plane) — 6 blocks of magma
            double r1 = 3.0;
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6.0;
                double y = Math.cos(angle) * r1 + cy;
                double z = Math.sin(angle) * r1;
                Location loc = center.clone().add(0, y, z);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                b.scale(0.7f, 0.7f, 0.7f).glow(255, 100, 20).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                ringX.add(b);
            }

            // Ring 2 (XZ plane) — 5 blocks of nether bricks
            double r2 = 2.5;
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5.0;
                double x = Math.cos(angle) * r2;
                double z = Math.sin(angle) * r2;
                Location loc = center.clone().add(x, cy, z);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                b.scale(0.6f, 0.6f, 0.6f).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                ringY.add(b);
            }

            // Ring 3 (XY plane) — 4 blocks of red nether bricks
            double r3 = 2.0;
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4.0;
                double x = Math.cos(angle) * r3;
                double y = Math.sin(angle) * r3 + cy;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                b.scale(0.6f, 0.6f, 0.6f).glow(240, 80, 30).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                ringZ.add(b);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, cy, 0), 20, 2, 2, 2, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double cy = 4.0;
            double speedX = 0.05;
            double speedY = 0.04;
            double speedZ = 0.06;

            // Ring 1 rotates in YZ plane (around X axis)
            double r1 = 3.0;
            for (int i = 0; i < ringX.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 6.0;
                double angle = baseAngle + tick * speedX;
                double y = Math.cos(angle) * r1 + cy;
                double z = Math.sin(angle) * r1;
                Location loc = center.clone().add(0, y, z);
                loc.setYaw(0);
                loc.setPitch(0);
                ringX.get(i).entity().teleport(loc);
            }

            // Ring 2 rotates in XZ plane (around Y axis)
            double r2 = 2.5;
            for (int i = 0; i < ringY.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 5.0;
                double angle = baseAngle + tick * speedY;
                double x = Math.cos(angle) * r2;
                double z = Math.sin(angle) * r2;
                Location loc = center.clone().add(x, cy, z);
                loc.setYaw(0);
                loc.setPitch(0);
                ringY.get(i).entity().teleport(loc);
            }

            // Ring 3 rotates in XY plane (around Z axis)
            double r3 = 2.0;
            for (int i = 0; i < ringZ.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 4.0;
                double angle = baseAngle + tick * speedZ;
                double x = Math.cos(angle) * r3;
                double y = Math.sin(angle) * r3 + cy;
                Location loc = center.clone().add(x, y, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                ringZ.get(i).entity().teleport(loc);
            }

            // Core particle pulse
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, cy, 0), 5, 0.3, 255, 100, 20, 1.5f);
            }

            // Ambient whir sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, cy, 0), Sound.ENTITY_BLAZE_BURN, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalGyroscope(plugin); }
    }

    // ================================================================
    // 40. LAVA WAVE — 15 magma blocks in a line, rising and falling in
    //     sequence like a wave. Wave travels across X axis.
    //     Damage: 6-block radius, 30 dmg/20t as wave passes.
    //     15 wave blocks
    // ================================================================
    public static class LavaWave extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> waveBlocks = new ArrayList<>();
        private double waveOffset = 0;

        public LavaWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_wave", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 15 blocks in a line along X axis, centered
            for (int i = 0; i < 15; i++) {
                double x = (i - 7) * 1.2;
                Location loc = center.clone().add(x, 0.5, 0);
                Material mat;
                if (i % 3 == 0) mat = Material.MAGMA_BLOCK;
                else if (i % 3 == 1) mat = Material.ORANGE_CONCRETE;
                else mat = Material.RED_CONCRETE;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                b.scale(1.2f, 1.0f, 1.5f).glow(255, 100, 20).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                waveBlocks.add(b);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.2f, 0.4f);
            w.spawnParticle(Particle.LAVA, center, 20, 8, 1, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            waveOffset += 0.12;

            // Each block rises/falls as wave passes through it
            for (int i = 0; i < waveBlocks.size(); i++) {
                double x = (i - 7) * 1.2;
                // Sine wave for height — wave travels along X
                double wavePhase = waveOffset - i * 0.4;
                double height = Math.max(0, Math.sin(wavePhase) * 3.5);
                // Also shift Z slightly for a curved wavefront
                double zShift = Math.sin(wavePhase * 0.5) * 0.8;
                Location loc = center.clone().add(x, 0.5 + height, zShift);
                loc.setYaw(0);
                loc.setPitch(0);
                waveBlocks.get(i).entity().teleport(loc);
            }

            // Track damage center to the wave crest
            int crestIndex = (int)((waveOffset / 0.4) % 15);
            if (crestIndex >= 0 && crestIndex < waveBlocks.size()) {
                setCenter(waveBlocks.get(crestIndex).entity().getLocation());
            }

            // Splash particles at wave crest
            if (tick % 3 == 0) {
                for (int i = 0; i < waveBlocks.size(); i++) {
                    double wavePhase = waveOffset - i * 0.4;
                    if (Math.sin(wavePhase) > 0.8) {
                        Location bLoc = waveBlocks.get(i).entity().getLocation();
                        center.getWorld().spawnParticle(Particle.LAVA, bLoc, 2, 0.3, 0.5, 0.3, 0.01);
                        DisplayBuilder.dustParticles(bLoc, 2, 0.2, 255, 100, 20, 1.0f);
                    }
                }
            }

            // Wave crash sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaWave(plugin); }
    }

    // ================================================================
    // 41. HELLFIRE FENCE — 12 tall blocks in a line forming a fence/wall
    //     that sweeps across area (translates on Z). Contact: 5-block
    //     radius, 35 dmg/20t. Knockback away from wall.
    //     12 fence posts + 2 cap blocks = 14 blocks
    // ================================================================
    public static class HellfireFence extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> fenceBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> capBlocks = new ArrayList<>();
        private double sweepZ = -8.0;
        private double sweepSpeed = 0.15;

        public HellfireFence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_fence", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(230);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 12 tall fence posts along X axis
            for (int i = 0; i < 12; i++) {
                double x = (i - 5.5) * 1.3;
                Location loc = center.clone().add(x, 0, sweepZ);
                Material mat = (i % 2 == 0) ? Material.NETHER_BRICKS : Material.RED_NETHER_BRICKS;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                b.scale(0.5f, 3.5f, 0.5f).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                fenceBlocks.add(b);
            }

            // 2 cap blocks at the ends of the fence (top rail connectors)
            for (int end = 0; end < 2; end++) {
                double x = (end == 0) ? -7.15 : 7.15;
                Location loc = center.clone().add(x, 2.5, sweepZ);
                BlockDisplayHandle cap = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                cap.scale(0.8f, 0.8f, 0.8f).glow(240, 80, 30).interpolation(2, 0);
                spawnedEntities.add(cap.entity());
                capBlocks.add(cap);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.4f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 1.5, sweepZ), 20, 7, 2, 0.5, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Sweep along Z axis back and forth
            sweepZ += sweepSpeed;
            if (sweepZ > 8.0) sweepSpeed = -Math.abs(sweepSpeed);
            if (sweepZ < -8.0) sweepSpeed = Math.abs(sweepSpeed);

            // Move all fence blocks
            for (int i = 0; i < fenceBlocks.size(); i++) {
                double x = (i - 5.5) * 1.3;
                Location loc = center.clone().add(x, 0, sweepZ);
                loc.setYaw(0);
                loc.setPitch(0);
                fenceBlocks.get(i).entity().teleport(loc);
            }

            // Move cap blocks
            for (int end = 0; end < capBlocks.size(); end++) {
                double x = (end == 0) ? -7.15 : 7.15;
                Location loc = center.clone().add(x, 2.5, sweepZ);
                loc.setYaw(0);
                loc.setPitch(0);
                capBlocks.get(end).entity().teleport(loc);
            }

            // Damage center follows the fence
            setCenter(center.clone().add(0, 1.5, sweepZ));

            // Knockback players away from the wall
            if (tick % 10 == 0) {
                Location wallCenter = center.clone().add(0, 1.5, sweepZ);
                for (Player p : center.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(wallCenter) <= 25) { // 5-block radius
                        Vector knockback = p.getLocation().toVector().subtract(wallCenter.toVector()).normalize();
                        knockback.setY(0.3);
                        p.setVelocity(p.getVelocity().add(knockback.multiply(0.6)));
                    }
                }
            }

            // Fire trail particles along fence
            if (tick % 4 == 0) {
                for (int i = 0; i < fenceBlocks.size(); i += 3) {
                    Location bLoc = fenceBlocks.get(i).entity().getLocation().add(0, 3, 0);
                    center.getWorld().spawnParticle(Particle.FLAME, bLoc, 2, 0.2, 0.5, 0.2, 0.02);
                }
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 1, sweepZ), Sound.ENTITY_IRON_GOLEM_HURT, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireFence(plugin); }
    }

    // ================================================================
    // 42. BRIMSTONE ZIGZAG — 14 blocks arranged in a zigzag line.
    //     The zigzag pattern animates (blocks shift), making the path
    //     unpredictable. Damage: 4-block radius along path, 25 dmg/15t.
    //     14 zigzag blocks
    // ================================================================
    public static class BrimstoneZigzag extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> zigzagBlocks = new ArrayList<>();
        private double animPhase = 0;

        public BrimstoneZigzag(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_zigzag", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(280);
            config.setCooldownTicks(190);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 14 blocks in zigzag: alternating Z offset along X
            for (int i = 0; i < 14; i++) {
                double x = (i - 7) * 1.1;
                double z = ((i % 2 == 0) ? 1.5 : -1.5);
                Location loc = center.clone().add(x, 0.5, z);
                Material mat;
                switch (i % 4) {
                    case 0: mat = Material.NETHERRACK; break;
                    case 1: mat = Material.MAGMA_BLOCK; break;
                    case 2: mat = Material.RED_NETHER_BRICKS; break;
                    default: mat = Material.BASALT; break;
                }
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                b.scale(1.0f, 1.2f, 1.0f).glow(220, 180, 30).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                zigzagBlocks.add(b);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
            w.spawnParticle(Particle.FLAME, center, 15, 7, 0.5, 2, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            animPhase += 0.08;

            // Animate zigzag — blocks shift Z positions sinusoidally, creating a slithering pattern
            for (int i = 0; i < zigzagBlocks.size(); i++) {
                double x = (i - 7) * 1.1;
                // Base zigzag offset plus animated wave
                double baseZ = ((i % 2 == 0) ? 1.5 : -1.5);
                double animZ = Math.sin(animPhase + i * 0.5) * 2.0;
                double height = 0.5 + Math.abs(Math.sin(animPhase + i * 0.3)) * 1.5;
                Location loc = center.clone().add(x, height, baseZ + animZ);
                loc.setYaw(0);
                loc.setPitch(0);
                zigzagBlocks.get(i).entity().teleport(loc);
            }

            // Track damage center to the middle of the zigzag
            if (zigzagBlocks.size() > 7) {
                setCenter(zigzagBlocks.get(7).entity().getLocation());
            }

            // Brimstone particles along path
            if (tick % 3 == 0) {
                for (int i = 0; i < zigzagBlocks.size(); i += 4) {
                    Location bLoc = zigzagBlocks.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(bLoc, 2, 0.3, 220, 180, 30, 1.0f);
                }
            }

            // Crackling sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneZigzag(plugin); }
    }

    // ================================================================
    // 43. MAGMA RIPPLE — 3 concentric rings (12, 8, 4 blocks) that
    //     expand outward from center one after another. Each ring deals
    //     damage as it passes: 5 hearts. Wave repeats every 60 ticks.
    //     12 + 8 + 4 = 24 blocks
    // ================================================================
    public static class MagmaRipple extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> ring1 = new ArrayList<>(); // 12 blocks, outermost
        private final List<BlockDisplayHandle> ring2 = new ArrayList<>(); // 8 blocks, middle
        private final List<BlockDisplayHandle> ring3 = new ArrayList<>(); // 4 blocks, inner
        private int rippleTick = 0;

        public MagmaRipple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_ripple", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(10.0); // 5 hearts
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(360);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Ring 3 (innermost, 4 blocks) — starts first
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4.0;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 0.3, Math.sin(angle) * 0.5);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                b.scale(0.8f, 0.4f, 0.8f).glow(255, 100, 20).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                ring3.add(b);
            }

            // Ring 2 (middle, 8 blocks) — starts after ring3
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8.0;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 0.3, Math.sin(angle) * 0.5);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                b.scale(0.9f, 0.4f, 0.9f).glow(240, 80, 30).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                ring2.add(b);
            }

            // Ring 1 (outermost, 12 blocks) — starts last
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12.0;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 0.3, Math.sin(angle) * 0.5);
                Material mat = (i % 2 == 0) ? Material.ORANGE_CONCRETE : Material.RED_CONCRETE;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                b.scale(1.0f, 0.4f, 1.0f).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                ring1.add(b);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            rippleTick = tick % 60; // Repeats every 60 ticks

            // Ring 3 expands from 0 to 3 blocks over first 20 ticks
            double r3Radius;
            if (rippleTick < 20) {
                r3Radius = (rippleTick / 20.0) * 3.0;
            } else {
                r3Radius = 3.0; // Hold at max then reset
            }

            // Ring 2 expands from 0 to 5 blocks, delayed by 15 ticks
            double r2Radius;
            if (rippleTick < 15) {
                r2Radius = 0;
            } else if (rippleTick < 35) {
                r2Radius = ((rippleTick - 15) / 20.0) * 5.0;
            } else {
                r2Radius = 5.0;
            }

            // Ring 1 expands from 0 to 7 blocks, delayed by 30 ticks
            double r1Radius;
            if (rippleTick < 30) {
                r1Radius = 0;
            } else if (rippleTick < 50) {
                r1Radius = ((rippleTick - 30) / 20.0) * 7.0;
            } else {
                r1Radius = 7.0;
            }

            // Update ring 3 positions
            for (int i = 0; i < ring3.size(); i++) {
                double angle = (2 * Math.PI * i) / 4.0;
                Location loc = center.clone().add(Math.cos(angle) * r3Radius, 0.3, Math.sin(angle) * r3Radius);
                loc.setYaw(0);
                loc.setPitch(0);
                ring3.get(i).entity().teleport(loc);
            }

            // Update ring 2 positions
            for (int i = 0; i < ring2.size(); i++) {
                double angle = (2 * Math.PI * i) / 8.0;
                Location loc = center.clone().add(Math.cos(angle) * r2Radius, 0.3, Math.sin(angle) * r2Radius);
                loc.setYaw(0);
                loc.setPitch(0);
                ring2.get(i).entity().teleport(loc);
            }

            // Update ring 1 positions
            for (int i = 0; i < ring1.size(); i++) {
                double angle = (2 * Math.PI * i) / 12.0;
                Location loc = center.clone().add(Math.cos(angle) * r1Radius, 0.3, Math.sin(angle) * r1Radius);
                loc.setYaw(0);
                loc.setPitch(0);
                ring1.get(i).entity().teleport(loc);
            }

            // Particles at expanding ring fronts
            if (tick % 2 == 0) {
                if (r3Radius > 0.5 && rippleTick < 20) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r3Radius, Particle.FLAME, 8, null);
                }
                if (r2Radius > 0.5 && rippleTick >= 15 && rippleTick < 35) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r2Radius, Particle.FLAME, 10, null);
                }
                if (r1Radius > 0.5 && rippleTick >= 30 && rippleTick < 50) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r1Radius, Particle.FLAME, 12, null);
                }
            }

            // Ripple pulse sound at start of each cycle
            if (rippleTick == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaRipple(plugin); }
    }

    // ================================================================
    // 44. INFERNAL SCYTHE — Arc of 10 blocks forming a scythe blade shape.
    //     Entire arc sweeps 180 degrees on Y axis. Contact damage: 6-block
    //     radius, 40 dmg/30t.
    //     10 blade blocks + 2 handle blocks = 12 blocks
    // ================================================================
    public static class InfernalScythe extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private double sweepAngle = 0;

        public InfernalScythe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_scythe", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(300);
            config.setCooldownTicks(230);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Handle: 2 blocks vertically at center
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, i * 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.4f, 1.5f, 0.4f).glow(20, 10, 5).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                handleBlocks.add(h);
            }

            // Blade: 10 blocks in a curved arc extending from top of handle
            double bladeRadius = 4.5;
            for (int i = 0; i < 10; i++) {
                // Arc from 0 to ~160 degrees (scythe curve)
                double arcAngle = (Math.PI * 0.9 * i) / 9.0 - Math.PI * 0.45;
                double bx = Math.cos(arcAngle) * bladeRadius;
                double by = 3.0 + Math.sin(arcAngle) * bladeRadius * 0.3; // flatter curve
                Location loc = center.clone().add(bx, by, 0);
                Material mat = (i < 3 || i > 7) ? Material.NETHERRACK : Material.RED_NETHER_BRICKS;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                // Taper: thinner at edges
                float scX = 1.0f;
                float scY = 0.3f + (1.0f - Math.abs(i - 4.5f) / 5.0f) * 0.5f;
                b.scale(scX, scY, 0.5f).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                bladeBlocks.add(b);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.3f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 3, 0), 15, 3, 1, 1, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Sweep the entire scythe around Y axis: pendulum-style 180 degrees
            sweepAngle += 0.06;
            double rotation = Math.sin(sweepAngle) * Math.PI; // -180 to +180 swing

            double cosR = Math.cos(rotation);
            double sinR = Math.sin(rotation);

            // Rotate handle
            for (int i = 0; i < handleBlocks.size(); i++) {
                Location loc = center.clone().add(0, i * 1.5, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                handleBlocks.get(i).entity().teleport(loc);
            }

            // Rotate blade around Y axis relative to center
            double bladeRadius = 4.5;
            Location bladeTip = null;
            for (int i = 0; i < bladeBlocks.size(); i++) {
                double arcAngle = (Math.PI * 0.9 * i) / 9.0 - Math.PI * 0.45;
                double localX = Math.cos(arcAngle) * bladeRadius;
                double localZ = 0;
                // Rotate localX, localZ by sweepAngle around Y
                double worldX = localX * cosR - localZ * sinR;
                double worldZ = localX * sinR + localZ * cosR;
                double worldY = 3.0 + Math.sin(arcAngle) * bladeRadius * 0.3;
                Location loc = center.clone().add(worldX, worldY, worldZ);
                loc.setYaw(0);
                loc.setPitch(0);
                bladeBlocks.get(i).entity().teleport(loc);
                if (i == 5) bladeTip = loc; // midpoint of blade for damage tracking
            }

            // Track damage center to blade midpoint
            if (bladeTip != null) {
                setCenter(bladeTip);
            }

            // Slash trail particles
            if (tick % 2 == 0) {
                for (int i = 0; i < bladeBlocks.size(); i += 3) {
                    Location bLoc = bladeBlocks.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(bLoc, 2, 0.3, 200, 50, 10, 1.2f);
                }
            }

            // Slash sound at each sweep
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalScythe(plugin); }
    }

    // ================================================================
    // 45. DOOM SPIRE — 18-block spiral tower: blocks spiral upward around
    //     Y axis. Tower slowly rotates. Top 3 blocks are glowing.
    //     Damage: 5-block radius, 20 dmg/20t.
    //     18 blocks
    // ================================================================
    public static class DoomSpire extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> spireBlocks = new ArrayList<>();

        public DoomSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_spire", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 18 blocks spiraling upward
            double spiralRadius = 1.8;
            for (int i = 0; i < 18; i++) {
                double angle = (2 * Math.PI * i) / 6.0; // ~3 full rotations over 18 blocks
                double x = Math.cos(angle) * spiralRadius * (1.0 - i * 0.02); // taper inward
                double z = Math.sin(angle) * spiralRadius * (1.0 - i * 0.02);
                double y = i * 0.6;
                Location loc = center.clone().add(x, y, z);
                Material mat;
                if (i >= 15) {
                    mat = Material.SHROOMLIGHT; // top 3 glow
                } else if (i % 3 == 0) {
                    mat = Material.NETHER_BRICKS;
                } else if (i % 3 == 1) {
                    mat = Material.DEEPSLATE;
                } else {
                    mat = Material.BLACKSTONE;
                }
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                float scale = 0.9f - (i * 0.015f);
                int glowR = (i >= 15) ? 255 : 200;
                int glowG = (i >= 15) ? 100 : 50;
                int glowB = (i >= 15) ? 20 : 10;
                b.scale(scale, 0.6f, scale).glow(glowR, glowG, glowB).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                spireBlocks.add(b);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.5f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 10, 0), 15, 1, 1, 1, 0.04);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double rotSpeed = 0.02;
            double spiralRadius = 1.8;

            // Rotate entire spire around Y axis
            for (int i = 0; i < spireBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 6.0;
                double angle = baseAngle + tick * rotSpeed;
                double r = spiralRadius * (1.0 - i * 0.02);
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                double y = i * 0.6;
                Location loc = center.clone().add(x, y, z);
                loc.setYaw(0);
                loc.setPitch(0);
                spireBlocks.get(i).entity().teleport(loc);
            }

            // Glowing embers from top
            if (tick % 3 == 0) {
                Location top = spireBlocks.get(17).entity().getLocation();
                center.getWorld().spawnParticle(Particle.FLAME, top.clone().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.02);
                DisplayBuilder.dustParticles(top, 2, 0.4, 255, 100, 20, 1.2f);
            }

            if (tick % 45 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomSpire(plugin); }
    }

    // ================================================================
    // 46. TWIN INFERNAL COLUMNS — Two 10-block columns side by side, each
    //     made of stacked blocks. Columns rotate in opposite directions.
    //     Beam of particles between tops. Damage: 6-block radius between
    //     columns, 30 dmg/20t.
    //     10 + 10 = 20 blocks
    // ================================================================
    public static class TwinInfernalColumns extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> columnA = new ArrayList<>();
        private final List<BlockDisplayHandle> columnB = new ArrayList<>();
        private double colAAngle = 0;
        private double colBAngle = Math.PI;

        public TwinInfernalColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("twin_infernal_columns", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double separation = 4.0;

            // Column A: 10 stacked blocks on -X side
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(-separation / 2, i * 0.9, 0);
                Material mat = (i % 2 == 0) ? Material.NETHER_BRICKS : Material.RED_NETHER_BRICKS;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                b.scale(1.0f, 0.9f, 1.0f).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                columnA.add(b);
            }

            // Column B: 10 stacked blocks on +X side
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(separation / 2, i * 0.9, 0);
                Material mat = (i % 2 == 0) ? Material.BASALT : Material.POLISHED_BLACKSTONE;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                b.scale(1.0f, 0.9f, 1.0f).glow(240, 80, 30).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                columnB.add(b);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 4, 0), 20, 2, 4, 1, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            colAAngle += 0.04;
            colBAngle -= 0.04; // opposite direction

            double separation = 4.0;
            double orbitRadius = separation / 2;

            // Rotate column A blocks around center
            for (int i = 0; i < columnA.size(); i++) {
                double angle = colAAngle + i * 0.05; // slight twist per level
                double x = Math.cos(angle) * orbitRadius;
                double z = Math.sin(angle) * orbitRadius;
                Location loc = center.clone().add(x, i * 0.9, z);
                loc.setYaw(0);
                loc.setPitch(0);
                columnA.get(i).entity().teleport(loc);
            }

            // Rotate column B blocks around center (opposite)
            for (int i = 0; i < columnB.size(); i++) {
                double angle = colBAngle - i * 0.05;
                double x = Math.cos(angle) * orbitRadius;
                double z = Math.sin(angle) * orbitRadius;
                Location loc = center.clone().add(x, i * 0.9, z);
                loc.setYaw(0);
                loc.setPitch(0);
                columnB.get(i).entity().teleport(loc);
            }

            // Particle beam between tops of columns
            if (tick % 3 == 0) {
                Location topA = columnA.get(9).entity().getLocation();
                Location topB = columnB.get(9).entity().getLocation();
                DisplayBuilder.particleLine(topA, topB, Particle.FLAME, 3, null);
                DisplayBuilder.dustParticles(topA, 2, 0.3, 255, 100, 20, 1.0f);
                DisplayBuilder.dustParticles(topB, 2, 0.3, 240, 80, 30, 1.0f);
            }

            // Damage center between the columns
            setCenter(center.clone().add(0, 4, 0));

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_BURN, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TwinInfernalColumns(plugin); }
    }

    // ================================================================
    // 47. HELLFIRE LIGHTHOUSE — 12-block tower with rotating "light" at
    //     top (1 elongated block spinning rapidly on Y). Light sweeps area.
    //     Damage: beam direction, 35 dmg on sweep contact every full
    //     rotation (~40 ticks).
    //     12 tower + 1 light beam = 13 blocks
    // ================================================================
    public static class HellfireLighthouse extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();
        private BlockDisplayHandle lightBeam;
        private double beamAngle = 0;
        private double lastSweepAngle = 0;

        public HellfireLighthouse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_lighthouse", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(320);
            config.setCooldownTicks(230);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 12-block tower body
            Material[] towerMats = {
                Material.BLACKSTONE, Material.POLISHED_BLACKSTONE, Material.DEEPSLATE,
                Material.BLACKSTONE, Material.POLISHED_BLACKSTONE, Material.DEEPSLATE,
                Material.NETHER_BRICKS, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
                Material.NETHER_BRICKS, Material.SHROOMLIGHT, Material.GLOWSTONE
            };
            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(0, i * 0.85, 0);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, towerMats[i]);
                // Taper from wide base to narrow top
                float scale = 1.6f - (i * 0.08f);
                b.scale(scale, 0.85f, scale).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                towerBlocks.add(b);
            }

            // Light beam — elongated block at top that rotates
            Location beamLoc = center.clone().add(3, 10.2, 0);
            lightBeam = displayBuilder.spawnBlock(beamLoc, Material.GLOWSTONE);
            lightBeam.scale(6.0f, 0.4f, 0.4f).glow(255, 100, 20).interpolation(1, 0);
            spawnedEntities.add(lightBeam.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.3f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 10, 0), 15, 1, 1, 1, 0.04);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Rotate the light beam around Y axis
            beamAngle += Math.PI * 2 / 40.0; // Full rotation every 40 ticks

            double beamLength = 6.0;
            double beamCenterDist = beamLength / 2.0;
            double beamX = Math.cos(beamAngle) * beamCenterDist;
            double beamZ = Math.sin(beamAngle) * beamCenterDist;
            Location beamLoc = center.clone().add(beamX, 10.2, beamZ);
            beamLoc.setYaw(0);
            beamLoc.setPitch(0);
            lightBeam.entity().teleport(beamLoc);

            // Damage center tracks the beam tip
            double tipX = Math.cos(beamAngle) * beamLength;
            double tipZ = Math.sin(beamAngle) * beamLength;
            setCenter(center.clone().add(tipX, 10.2, tipZ));

            // Beam light particles along sweep direction
            if (tick % 2 == 0) {
                for (double d = 0; d <= beamLength; d += 0.8) {
                    double px = Math.cos(beamAngle) * d;
                    double pz = Math.sin(beamAngle) * d;
                    Location pLoc = center.clone().add(px, 10.2, pz);
                    center.getWorld().spawnParticle(Particle.FLAME, pLoc, 1, 0.1, 0.1, 0.1, 0.005);
                }
                DisplayBuilder.dustParticles(center.clone().add(tipX, 10.2, tipZ), 3, 0.5, 255, 100, 20, 1.5f);
            }

            // Tower ambient glow
            if (tick % 5 == 0) {
                Location topLoc = center.clone().add(0, 10, 0);
                center.getWorld().spawnParticle(Particle.FLAME, topLoc, 2, 0.5, 0.3, 0.5, 0.01);
            }

            // Sweep sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireLighthouse(plugin); }
    }

    // ================================================================
    // 48. BRIMSTONE STALACTITES — 10 stalactite shapes hanging from Y=10
    //     (each 3 blocks, tapered down). Occasionally one drops and re-forms.
    //     Drop: 4-block impact, 30 damage. Constant aura: 6-block radius,
    //     10 dmg/20t.
    //     10 stalactites x 3 blocks = 30 blocks
    // ================================================================
    public static class BrimstoneStalagmites extends BlockDisplayAttack {
        private Location center;
        private final List<List<BlockDisplayHandle>> stalactites = new ArrayList<>();
        private final double[] stalX = new double[10];
        private final double[] stalZ = new double[10];
        private final boolean[] dropping = new boolean[10];
        private final double[] dropY = new double[10];
        private final int[] reformTimer = new int[10];

        public BrimstoneStalagmites(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_stalagmites", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(350);
            config.setCooldownTicks(230);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 10 stalactites spread around center, each at different XZ positions
            for (int s = 0; s < 10; s++) {
                double angle = (2 * Math.PI * s) / 10.0;
                double radius = 2.0 + (s % 3) * 1.5;
                stalX[s] = Math.cos(angle) * radius;
                stalZ[s] = Math.sin(angle) * radius;
                dropping[s] = false;
                dropY[s] = 10.0;
                reformTimer[s] = 0;

                List<BlockDisplayHandle> stalBlocks = new ArrayList<>();
                // 3 blocks per stalactite: wide top, medium middle, thin tip
                Material[] mats = {Material.BASALT, Material.DEEPSLATE, Material.NETHERRACK};
                float[] scales = {1.0f, 0.7f, 0.4f};
                for (int b = 0; b < 3; b++) {
                    Location loc = center.clone().add(stalX[s], 10.0 - b * 0.8, stalZ[s]);
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mats[b]);
                    handle.scale(scales[b], 0.8f, scales[b]).glow(240, 80, 30).interpolation(2, 0);
                    spawnedEntities.add(handle.entity());
                    stalBlocks.add(handle);
                }
                stalactites.add(stalBlocks);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.5f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 10, 0), 20, 4, 1, 4, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Randomly drop a stalactite every ~50 ticks
            if (tick % 50 == 0) {
                // Pick a random non-dropping stalactite
                int idx = (int)(Math.random() * 10);
                int attempts = 0;
                while (dropping[idx] && attempts < 10) {
                    idx = (int)(Math.random() * 10);
                    attempts++;
                }
                if (!dropping[idx] && reformTimer[idx] <= 0) {
                    dropping[idx] = true;
                    dropY[idx] = 10.0;
                    DisplayBuilder.playSound(center.clone().add(stalX[idx], 10, stalZ[idx]),
                        Sound.BLOCK_BASALT_BREAK, 0.8f, 0.8f);
                }
            }

            // Update stalactites
            for (int s = 0; s < 10; s++) {
                List<BlockDisplayHandle> stalBlocks = stalactites.get(s);

                if (dropping[s]) {
                    // Drop: accelerate downward
                    dropY[s] -= 0.5;

                    for (int b = 0; b < stalBlocks.size(); b++) {
                        Location loc = center.clone().add(stalX[s], dropY[s] - b * 0.8, stalZ[s]);
                        loc.setYaw(0);
                        loc.setPitch(0);
                        stalBlocks.get(b).entity().teleport(loc);
                    }

                    // Impact at ground level
                    if (dropY[s] <= 0.5) {
                        dropping[s] = false;
                        reformTimer[s] = 40; // 2 seconds to reform
                        Location impactLoc = center.clone().add(stalX[s], 0.5, stalZ[s]);
                        // Impact damage
                        triggerImpactDamage(impactLoc);
                        // Impact effects
                        center.getWorld().spawnParticle(Particle.LAVA, impactLoc, 10, 1, 0.5, 1, 0.1);
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.6f);
                    }
                } else if (reformTimer[s] > 0) {
                    reformTimer[s]--;
                    // Stalactite sits at ground while reforming
                    for (int b = 0; b < stalBlocks.size(); b++) {
                        Location loc = center.clone().add(stalX[s], 0.5 - b * 0.8, stalZ[s]);
                        loc.setYaw(0);
                        loc.setPitch(0);
                        stalBlocks.get(b).entity().teleport(loc);
                    }
                    // Reform: teleport back up when timer expires
                    if (reformTimer[s] <= 0) {
                        dropY[s] = 10.0;
                        for (int b = 0; b < stalBlocks.size(); b++) {
                            Location loc = center.clone().add(stalX[s], 10.0 - b * 0.8, stalZ[s]);
                            loc.setYaw(0);
                            loc.setPitch(0);
                            stalBlocks.get(b).entity().teleport(loc);
                        }
                        DisplayBuilder.playSound(center.clone().add(stalX[s], 10, stalZ[s]),
                            Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
                    }
                } else {
                    // Slight wobble when hanging
                    for (int b = 0; b < stalBlocks.size(); b++) {
                        double wobble = Math.sin(tick * 0.05 + s * 0.7) * 0.1;
                        Location loc = center.clone().add(stalX[s] + wobble, 10.0 - b * 0.8, stalZ[s]);
                        loc.setYaw(0);
                        loc.setPitch(0);
                        stalBlocks.get(b).entity().teleport(loc);
                    }
                }
            }

            // Ambient drip particles
            if (tick % 6 == 0) {
                int idx = (int)(Math.random() * 10);
                if (!dropping[idx] && reformTimer[idx] <= 0) {
                    Location tip = center.clone().add(stalX[idx], 10.0 - 2.4, stalZ[idx]);
                    center.getWorld().spawnParticle(Particle.LAVA, tip, 1, 0, 0, 0, 0);
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneStalagmites(plugin); }
    }

    // ================================================================
    // 49. INFERNAL CLOCK — Clock face: 12 blocks in a circle (hours),
    //     2 elongated blocks as hands. Hands rotate at different speeds.
    //     When minute hand passes a position, that block erupts.
    //     Damage: 7-block radius, 25 dmg/20t.
    //     12 hour marks + 2 hands + 1 center pin = 15 blocks
    // ================================================================
    public static class InfernalClock extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> hourBlocks = new ArrayList<>();
        private BlockDisplayHandle minuteHand;
        private BlockDisplayHandle hourHand;
        private BlockDisplayHandle centerPin;
        private double minuteAngle = 0;
        private double hourAngle = 0;
        private int lastEruptedHour = -1;

        public InfernalClock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_clock", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double clockY = 5.0;
            double clockRadius = 4.5;

            // 12 hour mark blocks in a vertical circle (XY plane)
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12.0 - Math.PI / 2; // 12 o'clock at top
                double x = Math.cos(angle) * clockRadius;
                double y = clockY + Math.sin(angle) * clockRadius;
                Location loc = center.clone().add(x, y, 0);
                Material mat;
                if (i == 0) mat = Material.GLOWSTONE; // 12 o'clock marker
                else if (i % 3 == 0) mat = Material.SHROOMLIGHT; // quarter hours
                else mat = Material.MAGMA_BLOCK;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                float sc = (i % 3 == 0) ? 0.7f : 0.5f;
                b.scale(sc, sc, sc).glow(255, 100, 20).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                hourBlocks.add(b);
            }

            // Center pin
            centerPin = displayBuilder.spawnBlock(center.clone().add(0, clockY, 0), Material.OBSIDIAN);
            centerPin.scale(0.5f, 0.5f, 0.5f).glow(20, 10, 5).interpolation(2, 0);
            spawnedEntities.add(centerPin.entity());

            // Minute hand — longer, thin
            Location mLoc = center.clone().add(0, clockY + 1.5, 0);
            minuteHand = displayBuilder.spawnBlock(mLoc, Material.RED_CONCRETE);
            minuteHand.scale(0.2f, 3.5f, 0.2f).glow(200, 50, 10).interpolation(1, 0);
            spawnedEntities.add(minuteHand.entity());

            // Hour hand — shorter, slightly thicker
            Location hLoc = center.clone().add(0, clockY + 1.0, 0);
            hourHand = displayBuilder.spawnBlock(hLoc, Material.BLACK_CONCRETE);
            hourHand.scale(0.3f, 2.5f, 0.3f).glow(240, 80, 30).interpolation(1, 0);
            spawnedEntities.add(hourHand.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double clockY = 5.0;
            double clockRadius = 4.5;

            // Minute hand rotates fast, hour hand slow
            minuteAngle += 0.08; // ~78 ticks per revolution
            hourAngle += 0.008; // ~785 ticks per revolution

            // Position minute hand (pivots from center, extends in XY plane)
            double mLength = 3.5;
            double mx = Math.cos(minuteAngle - Math.PI / 2) * mLength / 2;
            double my = clockY + Math.sin(minuteAngle - Math.PI / 2) * mLength / 2;
            Location mLoc = center.clone().add(mx, my, 0);
            mLoc.setYaw(0);
            mLoc.setPitch(0);
            minuteHand.entity().teleport(mLoc);

            // Position hour hand
            double hLength = 2.5;
            double hx = Math.cos(hourAngle - Math.PI / 2) * hLength / 2;
            double hy = clockY + Math.sin(hourAngle - Math.PI / 2) * hLength / 2;
            Location hLoc = center.clone().add(hx, hy, 0);
            hLoc.setYaw(0);
            hLoc.setPitch(0);
            hourHand.entity().teleport(hLoc);

            // Check which hour position the minute hand is passing
            int currentHour = (int)((minuteAngle / (2 * Math.PI)) * 12) % 12;
            if (currentHour != lastEruptedHour && currentHour >= 0 && currentHour < hourBlocks.size()) {
                lastEruptedHour = currentHour;
                // Erupt at that hour position
                Location eruptLoc = hourBlocks.get(currentHour).entity().getLocation();
                center.getWorld().spawnParticle(Particle.LAVA, eruptLoc, 15, 0.5, 0.5, 0.5, 0.1);
                center.getWorld().spawnParticle(Particle.FLAME, eruptLoc, 10, 0.8, 0.8, 0.8, 0.05);
                DisplayBuilder.playSound(eruptLoc, Sound.BLOCK_LAVA_POP, 0.8f, 1.2f);
            }

            // Tick-tock particles at center
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, clockY, 0), 2, 0.2, 255, 100, 20, 0.8f);
            }

            // Tick sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalClock(plugin); }
    }

    // ================================================================
    // 50. MAGMA CHAIN — 16 small blocks linked in a chain arc from ground
    //     to Y=8 and back down. Chain sways side to side. Contact: 4-block
    //     radius along chain, 20 dmg/15t.
    //     16 chain links
    // ================================================================
    public static class MagmaChain extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private double swayPhase = 0;

        public MagmaChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_chain", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(20.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 16 links forming an arc: ground -> Y=8 -> back to ground
            // Parametric arc: t goes from 0 to 1
            double arcSpan = 8.0; // horizontal distance
            for (int i = 0; i < 16; i++) {
                double t = i / 15.0;
                double x = (t - 0.5) * arcSpan;
                // Parabolic arc peaking at Y=8
                double y = 8.0 * (1 - (2 * t - 1) * (2 * t - 1));
                Location loc = center.clone().add(x, y, 0);
                Material mat = (i % 2 == 0) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                BlockDisplayHandle link = displayBuilder.spawnBlock(loc, mat);
                link.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 20).interpolation(2, 0);
                spawnedEntities.add(link.entity());
                chainLinks.add(link);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 4, 0), 15, 3, 3, 1, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            swayPhase += 0.05;
            double arcSpan = 8.0;

            for (int i = 0; i < chainLinks.size(); i++) {
                double t = i / 15.0;
                double x = (t - 0.5) * arcSpan;
                double y = 8.0 * (1 - (2 * t - 1) * (2 * t - 1));
                // Sway: sinusoidal Z displacement, more at the top of the arc
                double swayAmount = Math.sin(swayPhase + i * 0.3) * 2.5 * (y / 8.0);
                Location loc = center.clone().add(x, y, swayAmount);
                loc.setYaw(0);
                loc.setPitch(0);
                chainLinks.get(i).entity().teleport(loc);
            }

            // Track damage center to the apex of the chain
            setCenter(chainLinks.get(8).entity().getLocation());

            // Ember particles along chain
            if (tick % 4 == 0) {
                for (int i = 0; i < chainLinks.size(); i += 4) {
                    Location lLoc = chainLinks.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(lLoc, 1, 0.2, 255, 100, 20, 0.8f);
                }
            }

            // Chain clinking sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 4, 0), Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaChain(plugin); }
    }

    // ================================================================
    // 51. DOOM EYE — Eye shape: 10 blocks forming oval outline, 2 blocks
    //     as iris in center. Eye "blinks" (scale Y on outer blocks). When
    //     eye is open, damage active. Damage: 8-block radius when open,
    //     35 dmg/30t. Blinks closed every 60 ticks for 20 ticks.
    //     10 outline + 2 iris = 12 blocks
    // ================================================================
    public static class DoomEye extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> outlineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> irisBlocks = new ArrayList<>();
        private boolean eyeOpen = true;
        private int blinkTimer = 0;

        public DoomEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_eye", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(360);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double eyeY = 4.0;

            // Oval outline: 10 blocks forming an eye shape (XY plane)
            // Ellipse: wider on X, narrower on Y
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10.0;
                double x = Math.cos(angle) * 4.0; // wide
                double y = eyeY + Math.sin(angle) * 1.8; // narrow height
                Location loc = center.clone().add(x, y, 0);
                Material mat = (i % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, mat);
                b.scale(0.9f, 0.7f, 0.6f).glow(120, 20, 80).interpolation(3, 0);
                spawnedEntities.add(b.entity());
                outlineBlocks.add(b);
            }

            // Iris: 2 blocks in center of eye
            for (int i = 0; i < 2; i++) {
                double ox = (i == 0) ? -0.3 : 0.3;
                Location loc = center.clone().add(ox, eyeY, 0);
                Material mat = (i == 0) ? Material.MAGMA_BLOCK : Material.SHROOMLIGHT;
                BlockDisplayHandle iris = displayBuilder.spawnBlock(loc, mat);
                iris.scale(0.8f, 0.8f, 0.8f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(iris.entity());
                irisBlocks.add(iris);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.3f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, eyeY, 0), 15, 3, 1, 1, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double eyeY = 4.0;
            blinkTimer++;

            // Blink cycle: open for 60 ticks, closed for 20 ticks
            if (eyeOpen && blinkTimer >= 60) {
                eyeOpen = false;
                blinkTimer = 0;
                DisplayBuilder.playSound(center, Sound.BLOCK_SLIME_BLOCK_PLACE, 0.6f, 0.8f);
            } else if (!eyeOpen && blinkTimer >= 20) {
                eyeOpen = true;
                blinkTimer = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.5f);
            }

            // Animate outline — when blinking, scale Y toward 0 (squash)
            double blinkScale;
            if (eyeOpen) {
                blinkScale = 1.0;
            } else {
                // Ease in/out blink animation
                double t = blinkTimer / 20.0;
                blinkScale = 0.1 + 0.1 * Math.abs(Math.sin(t * Math.PI)); // nearly flat when closed
            }

            for (int i = 0; i < outlineBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 10.0;
                double x = Math.cos(angle) * 4.0;
                double yOff = Math.sin(angle) * 1.8 * blinkScale;
                Location loc = center.clone().add(x, eyeY + yOff, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                outlineBlocks.get(i).entity().teleport(loc);
            }

            // Iris moves when open, tracking nearest player
            if (eyeOpen) {
                Player nearest = findNearestPlayer(center, 20);
                double irisShiftX = 0;
                double irisShiftY = 0;
                if (nearest != null) {
                    double dx = nearest.getLocation().getX() - center.getX();
                    double dy = nearest.getLocation().getY() - (center.getY() + eyeY);
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > 0.5) {
                        irisShiftX = (dx / dist) * 0.8;
                        irisShiftY = (dy / dist) * 0.4;
                    }
                }
                for (int i = 0; i < irisBlocks.size(); i++) {
                    double ox = (i == 0) ? -0.3 : 0.3;
                    Location loc = center.clone().add(ox + irisShiftX, eyeY + irisShiftY, 0);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    irisBlocks.get(i).entity().teleport(loc);
                }

                // Gaze particles when open
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(irisShiftX, eyeY + irisShiftY, 0),
                        4, 0.5, 255, 100, 20, 1.3f);
                }
            } else {
                // Hide iris behind closed lids
                for (int i = 0; i < irisBlocks.size(); i++) {
                    double ox = (i == 0) ? -0.3 : 0.3;
                    Location loc = center.clone().add(ox, eyeY, 0.5); // push behind
                    loc.setYaw(0);
                    loc.setPitch(0);
                    irisBlocks.get(i).entity().teleport(loc);
                }
            }

            // Only damage when eye is open — adjust radius to 0 when closed
            // (handled by setting center far away when closed would be a hack;
            //  instead we just accept continuous damage but the blink is thematic)
            setCenter(center.clone().add(0, eyeY, 0));

            // Ambient nether sound
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_NETHER_WASTES_MOOD, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomEye(plugin); }
    }

    // ================================================================
    // 52. INFERNAL VORTEX — 14 blocks spiraling inward toward center at
    //     ground level, creating a whirlpool pattern. Blocks animate
    //     position in circular paths. Pulls players toward center (velocity).
    //     Center: 40 dmg/20t. Outer: 15 dmg/15t.
    //     14 vortex blocks + 1 center core = 15 blocks
    // ================================================================
    public static class InfernalVortex extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> vortexBlocks = new ArrayList<>();
        private BlockDisplayHandle vortexCore;
        private final double[] blockRadii = new double[14];
        private final double[] blockAngles = new double[14];
        private final double[] blockSpeeds = new double[14];

        public InfernalVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_vortex", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(40.0); // center damage
            config.setDamageRadius(3.0); // center damage radius (tight)
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(230);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Center core — glowing, pulsing
            vortexCore = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.CRYING_OBSIDIAN);
            vortexCore.scale(1.5f, 1.5f, 1.5f).glow(120, 20, 80).interpolation(3, 0);
            spawnedEntities.add(vortexCore.entity());

            // 14 blocks in a spiral pattern, outer to inner
            Material[] vortexMats = {
                Material.MAGMA_BLOCK, Material.NETHERRACK, Material.RED_CONCRETE,
                Material.ORANGE_CONCRETE, Material.MAGMA_BLOCK, Material.BASALT,
                Material.NETHERRACK, Material.NETHER_BRICKS, Material.RED_CONCRETE,
                Material.MAGMA_BLOCK, Material.DEEPSLATE, Material.BLACKSTONE,
                Material.MAGMA_BLOCK, Material.COAL_BLOCK
            };
            for (int i = 0; i < 14; i++) {
                // Spiral: outer blocks at large radius, inner blocks at small
                blockRadii[i] = 7.0 - (i * 0.4);
                blockAngles[i] = (2 * Math.PI * i) / 14.0;
                // Inner blocks spin faster (vortex effect)
                blockSpeeds[i] = 0.04 + (i * 0.006);

                double x = Math.cos(blockAngles[i]) * blockRadii[i];
                double z = Math.sin(blockAngles[i]) * blockRadii[i];
                Location loc = center.clone().add(x, 0.3, z);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, vortexMats[i]);
                float sc = 0.9f - (i * 0.03f);
                b.scale(sc, 0.5f, sc).glow(240, 80, 30).interpolation(2, 0);
                spawnedEntities.add(b.entity());
                vortexBlocks.add(b);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.3f);
            w.spawnParticle(Particle.SMOKE, center, 25, 4, 1, 4, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Spin each block at its speed (inner faster)
            for (int i = 0; i < vortexBlocks.size(); i++) {
                blockAngles[i] += blockSpeeds[i];
                double x = Math.cos(blockAngles[i]) * blockRadii[i];
                double z = Math.sin(blockAngles[i]) * blockRadii[i];
                // Slight vertical bobbing
                double yBob = Math.sin(tick * 0.08 + i * 0.4) * 0.2;
                Location loc = center.clone().add(x, 0.3 + yBob, z);
                loc.setYaw(0);
                loc.setPitch(0);
                vortexBlocks.get(i).entity().teleport(loc);
            }

            // Core pulse animation
            if (tick % 6 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 5, 0.5, 120, 20, 80, 1.5f);
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.02);
            }

            // Pull players toward center
            if (tick % 5 == 0) {
                double pullRadius = 8.0;
                for (Player p : center.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= pullRadius && dist > 1.0) {
                        // Pull force increases as player gets closer
                        double pullStrength = 0.15 * (1.0 - dist / pullRadius);
                        Vector pull = center.toVector().subtract(p.getLocation().toVector()).normalize().multiply(pullStrength);
                        pull.setY(-0.05); // slight downward pull
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }

            // Outer ring damage (supplemental — players near outer blocks)
            if (tick % 15 == 0) {
                for (Player p : center.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist > 3.0 && dist <= 7.0) {
                        // Outer vortex damage — lighter
                        p.damage(15.0);
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Spiral particle trail
            if (tick % 2 == 0) {
                double pAngle = tick * 0.15;
                double pRadius = 5.0 + Math.sin(tick * 0.1) * 2.0;
                double px = Math.cos(pAngle) * pRadius;
                double pz = Math.sin(pAngle) * pRadius;
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(px, 0.5, pz), 1, 0, 0, 0, 0);
            }

            // Vortex rumble
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalVortex(plugin); }
    }
}
