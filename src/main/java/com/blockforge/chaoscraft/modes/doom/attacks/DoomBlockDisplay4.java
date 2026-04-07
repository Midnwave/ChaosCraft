package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Doom Mode -- BLOCK DISPLAY ATTACKS (Part 4, attacks 31-40)
 * 10 advanced orbital/geometry BlockDisplay attacks.
 *
 * Categories:
 *   31. InfernalArmillarySphere -- 3 perpendicular rotating rings + glowing core
 *   32. MagmaHelixTower         -- Double helix tower with center axis
 *   33. DoomPentagram           -- 5-pointed star with sequential glow
 *   34. HellfireDNA             -- Double helix with pulsing rungs
 *   35. InfernalTorusRing       -- Torus (donut) with tumble rotation
 *   36. MagmaMobiusStrip        -- Twisted parametric loop
 *   37. DoomTesseract           -- 4D hypercube rotation illusion
 *   38. InfernalGear            -- Interlocking gear system
 *   39. MagmaAstrolabe          -- 3 nested tilted rings + pointer
 *   40. HellfireSundial         -- Base circle, gnomon, hour markers, shadow
 *
 * ALL attacks use Transformation-based animation (no teleport rotation).
 * Scale reshaping, 25+ blocks each, 5-8 block radii, no status effects.
 *
 * Doom palette:
 * - Lava orange: RGB(255, 100, 20)
 * - Hellfire red: RGB(200, 50, 10)
 * - Charred black: RGB(20, 10, 5)
 * - Ember glow: RGB(240, 80, 30)
 * - Brimstone yellow: RGB(220, 180, 30)
 * - Nether purple: RGB(120, 20, 80)
 * - Void black: RGB(10, 5, 15)
 * - Blood red: RGB(180, 20, 20)
 *
 * Materials: MAGMA_BLOCK, NETHERRACK, NETHER_BRICKS, RED_NETHER_BRICKS,
 *            BLACKSTONE, POLISHED_BLACKSTONE, CRYING_OBSIDIAN, SHROOMLIGHT,
 *            BASALT, DEEPSLATE, COAL_BLOCK, OBSIDIAN, RED_CONCRETE,
 *            BLACK_CONCRETE, ORANGE_CONCRETE, GRAY_CONCRETE, SCULK,
 *            SCULK_CATALYST, AMETHYST_BLOCK, CHAIN, IRON_BLOCK, LIGHTNING_ROD
 */
public final class DoomBlockDisplay4 {
    private DoomBlockDisplay4() {}

    private static final String MODE_PATH = "modes/doom/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Orbital / Geometry (31-40)
        registry.register(new InfernalArmillarySphere(plugin));
        registry.register(new MagmaHelixTower(plugin));
        registry.register(new DoomPentagram(plugin));
        registry.register(new HellfireDNA(plugin));
        registry.register(new InfernalTorusRing(plugin));
        registry.register(new MagmaMobiusStrip(plugin));
        registry.register(new DoomTesseract(plugin));
        registry.register(new InfernalGear(plugin));
        registry.register(new MagmaAstrolabe(plugin));
        registry.register(new HellfireSundial(plugin));
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
    // 31. INFERNAL ARMILLARY SPHERE (30 blocks)
    // 3 rings on perpendicular axes (10 blocks each, flat plates
    // scale(0.5,0.1,0.5) arranged in circle). Each ring rotates on
    // its own axis at a different speed via Transformation. 3 glowing
    // core blocks. Rings never synchronize.
    // damage=20, radius=8, ticks-between-damage=15, duration=500
    // ================================================================
    public static class InfernalArmillarySphere extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringX = new ArrayList<>();   // 10 blocks, rotate around X
        private final List<BlockDisplayHandle> ringY = new ArrayList<>();   // 10 blocks, rotate around Y
        private final List<BlockDisplayHandle> ringZ = new ArrayList<>();   // 10 blocks, rotate around Z
        private final List<BlockDisplayHandle> core = new ArrayList<>();    // 3 glowing center
        private static final double RADIUS = 3.5;
        private static final int BLOCKS_PER_RING = 10;

        public InfernalArmillarySphere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_infernal_armillary_sphere", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ring X (rotates around X axis) -- lies in Y-Z plane
            for (int i = 0; i < BLOCKS_PER_RING; i++) {
                double angle = (2.0 * Math.PI * i) / BLOCKS_PER_RING;
                double py = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                Location loc = center.clone().add(0, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(0.5f, 0.1f, 0.5f).glow(200, 50, 10).interpolation(3, 0);
                ringX.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring Y (rotates around Y axis) -- lies in X-Z plane
            for (int i = 0; i < BLOCKS_PER_RING; i++) {
                double angle = (2.0 * Math.PI * i) / BLOCKS_PER_RING;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                Location loc = center.clone().add(px, 0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                h.scale(0.5f, 0.1f, 0.5f).glow(255, 100, 20).interpolation(3, 0);
                ringY.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring Z (rotates around Z axis) -- lies in X-Y plane
            for (int i = 0; i < BLOCKS_PER_RING; i++) {
                double angle = (2.0 * Math.PI * i) / BLOCKS_PER_RING;
                double px = Math.cos(angle) * RADIUS;
                double py = Math.sin(angle) * RADIUS;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.5f, 0.1f, 0.5f).glow(240, 80, 30).interpolation(3, 0);
                ringZ.add(h);
                spawnedEntities.add(h.entity());
            }

            // Core: 3 glowing blocks at center
            for (int i = 0; i < 3; i++) {
                double offset = (i - 1) * 0.3;
                Location loc = center.clone().add(offset, offset, -offset);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 200, 50).interpolation(3, 0);
                core.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ring X rotates around X axis at 4 deg/tick
            double angleX = Math.toRadians(ticksAlive * 4.0);
            for (int i = 0; i < ringX.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / BLOCKS_PER_RING + angleX;
                double py = Math.cos(baseAngle) * RADIUS;
                double pz = Math.sin(baseAngle) * RADIUS;
                float tx = -0.25f;
                float ty = (float) py - 0.05f;
                float tz = (float) pz - 0.25f;
                ringX.get(i).animateTo(
                        new Vector3f(tx, ty, tz),
                        new AxisAngle4f((float) angleX, 1, 0, 0),
                        new Vector3f(0.5f, 0.1f, 0.5f), 3);
            }

            // Ring Y rotates around Y axis at 3 deg/tick (never syncs with X's 4)
            double angleY = Math.toRadians(ticksAlive * 3.0);
            for (int i = 0; i < ringY.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / BLOCKS_PER_RING + angleY;
                double px = Math.cos(baseAngle) * RADIUS;
                double pz = Math.sin(baseAngle) * RADIUS;
                float tx = (float) px - 0.25f;
                float ty = -0.05f;
                float tz = (float) pz - 0.25f;
                ringY.get(i).animateTo(
                        new Vector3f(tx, ty, tz),
                        new AxisAngle4f((float) angleY, 0, 1, 0),
                        new Vector3f(0.5f, 0.1f, 0.5f), 3);
            }

            // Ring Z rotates around Z axis at 5 deg/tick
            double angleZ = Math.toRadians(ticksAlive * 5.0);
            for (int i = 0; i < ringZ.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / BLOCKS_PER_RING + angleZ;
                double px = Math.cos(baseAngle) * RADIUS;
                double py = Math.sin(baseAngle) * RADIUS;
                float tx = (float) px - 0.25f;
                float ty = (float) py - 0.05f;
                float tz = -0.25f;
                ringZ.get(i).animateTo(
                        new Vector3f(tx, ty, tz),
                        new AxisAngle4f((float) angleZ, 0, 0, 1),
                        new Vector3f(0.5f, 0.1f, 0.5f), 3);
            }

            // Core pulses glow brightness
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : core) {
                    float pulse = 0.35f + 0.15f * (float) Math.sin(ticksAlive * 0.15);
                    h.animateTo(
                            new Vector3f(-pulse / 2, -pulse / 2, -pulse / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, pulse, pulse), 10);
                }
            }

            // Particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.crimsonDust(center, 3, 1.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalArmillarySphere(plugin); }
    }

    // ================================================================
    // 32. MAGMA HELIX TOWER (28 blocks)
    // Double helix: 2 strands (10 each, spiral Y=0->8, 4 revolutions,
    // scale(0.5,0.5,0.5)). Center axis: 4 blocks scale(0.2,2.0,0.2).
    // 4 orbit accent blocks. Y-axis Transform rotation.
    // damage=25, radius=5, ticks-between-damage=20, duration=500
    // ================================================================
    public static class MagmaHelixTower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> strandA = new ArrayList<>();  // 10
        private final List<BlockDisplayHandle> strandB = new ArrayList<>();  // 10
        private final List<BlockDisplayHandle> axis = new ArrayList<>();     // 4
        private final List<BlockDisplayHandle> accents = new ArrayList<>();  // 4
        private static final double HELIX_RADIUS = 2.0;
        private static final double HELIX_HEIGHT = 8.0;
        private static final int STRAND_COUNT = 10;
        private static final int REVOLUTIONS = 4;

        public MagmaHelixTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_magma_helix_tower", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Strand A: helix from Y=0 to Y=8
            for (int i = 0; i < STRAND_COUNT; i++) {
                double t = (double) i / STRAND_COUNT;
                double angle = t * REVOLUTIONS * 2.0 * Math.PI;
                double px = Math.cos(angle) * HELIX_RADIUS;
                double pz = Math.sin(angle) * HELIX_RADIUS;
                double py = t * HELIX_HEIGHT;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 20).interpolation(4, 0);
                strandA.add(h);
                spawnedEntities.add(h.entity());
            }

            // Strand B: offset by PI
            for (int i = 0; i < STRAND_COUNT; i++) {
                double t = (double) i / STRAND_COUNT;
                double angle = t * REVOLUTIONS * 2.0 * Math.PI + Math.PI;
                double px = Math.cos(angle) * HELIX_RADIUS;
                double pz = Math.sin(angle) * HELIX_RADIUS;
                double py = t * HELIX_HEIGHT;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 50, 10).interpolation(4, 0);
                strandB.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center axis: 4 tall thin blocks
            for (int i = 0; i < 4; i++) {
                double py = i * 2.0 + 0.5;
                Location loc = center.clone().add(0, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.2f, 2.0f, 0.2f).glow(20, 10, 5).interpolation(4, 0);
                axis.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 orbit accents
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4;
                double px = Math.cos(angle) * (HELIX_RADIUS + 1.0);
                double pz = Math.sin(angle) * (HELIX_RADIUS + 1.0);
                Location loc = center.clone().add(px, 4.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.3f, 0.3f, 0.3f).glow(240, 80, 30).interpolation(4, 0);
                accents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Y-axis rotation via Transform -- entire helix spins
            float yRot = (float) Math.toRadians(ticksAlive * 3.0);

            // Strand A
            for (int i = 0; i < strandA.size(); i++) {
                double t = (double) i / STRAND_COUNT;
                double baseAngle = t * REVOLUTIONS * 2.0 * Math.PI;
                double rotAngle = baseAngle + yRot;
                float px = (float) (Math.cos(rotAngle) * HELIX_RADIUS) - 0.25f;
                float pz = (float) (Math.sin(rotAngle) * HELIX_RADIUS) - 0.25f;
                float py = (float) (t * HELIX_HEIGHT) - 0.25f;
                strandA.get(i).animateTo(
                        new Vector3f(px, py, pz),
                        new AxisAngle4f(yRot, 0, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), 4);
            }

            // Strand B
            for (int i = 0; i < strandB.size(); i++) {
                double t = (double) i / STRAND_COUNT;
                double baseAngle = t * REVOLUTIONS * 2.0 * Math.PI + Math.PI;
                double rotAngle = baseAngle + yRot;
                float px = (float) (Math.cos(rotAngle) * HELIX_RADIUS) - 0.25f;
                float pz = (float) (Math.sin(rotAngle) * HELIX_RADIUS) - 0.25f;
                float py = (float) (t * HELIX_HEIGHT) - 0.25f;
                strandB.get(i).animateTo(
                        new Vector3f(px, py, pz),
                        new AxisAngle4f(yRot, 0, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), 4);
            }

            // Axis spins with helix
            for (int i = 0; i < axis.size(); i++) {
                float py = i * 2.0f + 0.5f - 0.1f;
                axis.get(i).animateTo(
                        new Vector3f(-0.1f, py, -0.1f),
                        new AxisAngle4f(yRot, 0, 1, 0),
                        new Vector3f(0.2f, 2.0f, 0.2f), 4);
            }

            // Accent orbits at double speed
            float accentRot = (float) Math.toRadians(ticksAlive * 6.0);
            for (int i = 0; i < accents.size(); i++) {
                double angle = (2.0 * Math.PI * i) / 4 + accentRot;
                float px = (float) (Math.cos(angle) * (HELIX_RADIUS + 1.0)) - 0.15f;
                float pz = (float) (Math.sin(angle) * (HELIX_RADIUS + 1.0)) - 0.15f;
                accents.get(i).animateTo(
                        new Vector3f(px, 3.85f, pz),
                        new AxisAngle4f(accentRot, 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 0.3f), 4);
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 4, 0), 4, 2.0, 255, 100, 20, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaHelixTower(plugin); }
    }

    // ================================================================
    // 33. DOOM PENTAGRAM (30 blocks)
    // 5 star points (3 blocks each tapered outward), 5 inner lines
    // (2 blocks each), center disc (5 flat blocks scale(0.8,0.05,0.8)).
    // Y=0.2 off ground. Y-rotation. Points pulse glow sequentially
    // clockwise.
    // damage=25, radius=7, ticks-between-damage=20, duration=500
    // ================================================================
    public static class DoomPentagram extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> starPoints = new ArrayList<>();  // 5 groups of 3
        private final List<List<BlockDisplayHandle>> innerLines = new ArrayList<>();  // 5 groups of 2
        private final List<BlockDisplayHandle> centerDisc = new ArrayList<>();        // 5
        private static final double OUTER_RADIUS = 5.0;
        private static final double INNER_RADIUS = 2.0;

        public DoomPentagram(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_pentagram", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pentagram: 5 points, star shape
            // Star point angles: 0, 72, 144, 216, 288 degrees (offset -90 to point up)
            for (int p = 0; p < 5; p++) {
                double pointAngle = Math.toRadians(p * 72.0 - 90.0);
                double tipX = Math.cos(pointAngle) * OUTER_RADIUS;
                double tipZ = Math.sin(pointAngle) * OUTER_RADIUS;

                List<BlockDisplayHandle> pointBlocks = new ArrayList<>();
                // 3 blocks tapered outward from inner toward tip
                for (int j = 0; j < 3; j++) {
                    double t = (j + 1) / 3.0;
                    double bx = tipX * t;
                    double bz = tipZ * t;
                    float scaleW = 0.4f - j * 0.1f; // Taper: 0.4, 0.3, 0.2
                    Location loc = center.clone().add(bx, 0.2, bz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                    h.scale(scaleW, 0.1f, scaleW).glow(200, 50, 10).interpolation(5, 0);
                    pointBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
                starPoints.add(pointBlocks);
            }

            // Inner connecting lines (pentagram inner star lines)
            // Connect point i to point (i+2)%5 for star pattern
            for (int i = 0; i < 5; i++) {
                double angleA = Math.toRadians(i * 72.0 - 90.0);
                double angleB = Math.toRadians(((i + 2) % 5) * 72.0 - 90.0);
                double ax = Math.cos(angleA) * INNER_RADIUS;
                double az = Math.sin(angleA) * INNER_RADIUS;
                double bx = Math.cos(angleB) * INNER_RADIUS;
                double bz = Math.sin(angleB) * INNER_RADIUS;

                List<BlockDisplayHandle> lineBlocks = new ArrayList<>();
                for (int j = 0; j < 2; j++) {
                    double t = (j + 1) / 3.0;
                    double lx = ax + (bx - ax) * t;
                    double lz = az + (bz - az) * t;
                    Location loc = center.clone().add(lx, 0.2, lz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                    h.scale(0.25f, 0.08f, 0.25f).glow(120, 20, 80).interpolation(5, 0);
                    lineBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
                innerLines.add(lineBlocks);
            }

            // Center disc: 5 flat blocks
            for (int i = 0; i < 5; i++) {
                double angle = (2.0 * Math.PI * i) / 5;
                double dx = Math.cos(angle) * 0.5;
                double dz = Math.sin(angle) * 0.5;
                Location loc = center.clone().add(dx, 0.2, dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 0.05f, 0.8f).glow(180, 20, 20).interpolation(5, 0);
                centerDisc.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Y-rotation for the whole structure
            float yRot = (float) Math.toRadians(ticksAlive * 2.0);

            // Rotate star points
            for (int p = 0; p < 5; p++) {
                double pointAngle = Math.toRadians(p * 72.0 - 90.0);
                double tipX = Math.cos(pointAngle) * OUTER_RADIUS;
                double tipZ = Math.sin(pointAngle) * OUTER_RADIUS;

                // Sequential clockwise pulse: each point glows for 20 ticks in sequence
                int pulsePhase = (ticksAlive / 20) % 5;
                boolean glowing = (pulsePhase == p);

                for (int j = 0; j < starPoints.get(p).size(); j++) {
                    double t = (j + 1) / 3.0;
                    // Apply Y-rotation to the position
                    double bx = tipX * t;
                    double bz = tipZ * t;
                    double rx = bx * Math.cos(yRot) - bz * Math.sin(yRot);
                    double rz = bx * Math.sin(yRot) + bz * Math.cos(yRot);
                    float scaleW = 0.4f - j * 0.1f;
                    float scaleH = glowing ? 0.2f : 0.1f; // Pulse height when glowing

                    starPoints.get(p).get(j).animateTo(
                            new Vector3f((float) rx - scaleW / 2, 0.2f, (float) rz - scaleW / 2),
                            new AxisAngle4f(yRot, 0, 1, 0),
                            new Vector3f(scaleW, scaleH, scaleW), 5);

                    if (glowing) {
                        starPoints.get(p).get(j).glow(255, 200, 50);
                    } else {
                        starPoints.get(p).get(j).glow(200, 50, 10);
                    }
                }
            }

            // Rotate inner lines
            for (int i = 0; i < 5; i++) {
                double angleA = Math.toRadians(i * 72.0 - 90.0);
                double angleB = Math.toRadians(((i + 2) % 5) * 72.0 - 90.0);
                double ax = Math.cos(angleA) * INNER_RADIUS;
                double az = Math.sin(angleA) * INNER_RADIUS;
                double bx = Math.cos(angleB) * INNER_RADIUS;
                double bz = Math.sin(angleB) * INNER_RADIUS;

                for (int j = 0; j < innerLines.get(i).size(); j++) {
                    double t = (j + 1) / 3.0;
                    double lx = ax + (bx - ax) * t;
                    double lz = az + (bz - az) * t;
                    double rx = lx * Math.cos(yRot) - lz * Math.sin(yRot);
                    double rz = lx * Math.sin(yRot) + lz * Math.cos(yRot);

                    innerLines.get(i).get(j).animateTo(
                            new Vector3f((float) rx - 0.125f, 0.2f, (float) rz - 0.125f),
                            new AxisAngle4f(yRot, 0, 1, 0),
                            new Vector3f(0.25f, 0.08f, 0.25f), 5);
                }
            }

            // Center disc rotates
            for (int i = 0; i < centerDisc.size(); i++) {
                double angle = (2.0 * Math.PI * i) / 5;
                double dx = Math.cos(angle + yRot) * 0.5;
                double dz = Math.sin(angle + yRot) * 0.5;
                centerDisc.get(i).animateTo(
                        new Vector3f((float) dx - 0.4f, 0.2f, (float) dz - 0.4f),
                        new AxisAngle4f(yRot, 0, 1, 0),
                        new Vector3f(0.8f, 0.05f, 0.8f), 5);
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.5, 0), 5, 3.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoomPentagram(plugin); }
    }

    // ================================================================
    // 34. HELLFIRE DNA (30 blocks)
    // Double helix with rungs: 2 backbones (10 each, helix shape),
    // 5 rungs (2 blocks each scale(0.15,0.15,0.8)) connecting the
    // strands. Y-rotation. Rungs pulse scale periodically.
    // damage=20, radius=6, ticks-between-damage=15, duration=500
    // ================================================================
    public static class HellfireDNA extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> backboneA = new ArrayList<>(); // 10
        private final List<BlockDisplayHandle> backboneB = new ArrayList<>(); // 10
        private final List<BlockDisplayHandle> rungs = new ArrayList<>();     // 10 (5 rungs x 2)
        private static final double HELIX_RADIUS = 2.0;
        private static final double HELIX_HEIGHT = 7.0;
        private static final int BACKBONE_COUNT = 10;
        private static final int REVOLUTIONS = 3;

        public HellfireDNA(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_hellfire_dna", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Backbone A
            for (int i = 0; i < BACKBONE_COUNT; i++) {
                double t = (double) i / BACKBONE_COUNT;
                double angle = t * REVOLUTIONS * 2.0 * Math.PI;
                double px = Math.cos(angle) * HELIX_RADIUS;
                double pz = Math.sin(angle) * HELIX_RADIUS;
                double py = t * HELIX_HEIGHT;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 100, 20).interpolation(4, 0);
                backboneA.add(h);
                spawnedEntities.add(h.entity());
            }

            // Backbone B (offset by PI)
            for (int i = 0; i < BACKBONE_COUNT; i++) {
                double t = (double) i / BACKBONE_COUNT;
                double angle = t * REVOLUTIONS * 2.0 * Math.PI + Math.PI;
                double px = Math.cos(angle) * HELIX_RADIUS;
                double pz = Math.sin(angle) * HELIX_RADIUS;
                double py = t * HELIX_HEIGHT;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 50, 10).interpolation(4, 0);
                backboneB.add(h);
                spawnedEntities.add(h.entity());
            }

            // 5 rungs connecting the two backbones (at indices 1,3,5,7,9)
            for (int r = 0; r < 5; r++) {
                int idx = r * 2 + 1; // indices 1,3,5,7,9
                double t = (double) idx / BACKBONE_COUNT;
                double angleA = t * REVOLUTIONS * 2.0 * Math.PI;
                double angleB = angleA + Math.PI;
                double axA = Math.cos(angleA) * HELIX_RADIUS;
                double azA = Math.sin(angleA) * HELIX_RADIUS;
                double axB = Math.cos(angleB) * HELIX_RADIUS;
                double azB = Math.sin(angleB) * HELIX_RADIUS;
                double py = t * HELIX_HEIGHT;

                // Two blocks per rung (one from each side toward center)
                for (int j = 0; j < 2; j++) {
                    double lerp = (j == 0) ? 0.25 : 0.75;
                    double rx = axA + (axB - axA) * lerp;
                    double rz = azA + (azB - azA) * lerp;
                    Location loc = center.clone().add(rx, py, rz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                    h.scale(0.15f, 0.15f, 0.8f).glow(240, 180, 30).interpolation(4, 0);
                    rungs.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            float yRot = (float) Math.toRadians(ticksAlive * 3.0);

            // Backbone A
            for (int i = 0; i < backboneA.size(); i++) {
                double t = (double) i / BACKBONE_COUNT;
                double baseAngle = t * REVOLUTIONS * 2.0 * Math.PI + yRot;
                float px = (float) (Math.cos(baseAngle) * HELIX_RADIUS) - 0.2f;
                float pz = (float) (Math.sin(baseAngle) * HELIX_RADIUS) - 0.2f;
                float py = (float) (t * HELIX_HEIGHT) - 0.2f;
                backboneA.get(i).animateTo(
                        new Vector3f(px, py, pz),
                        new AxisAngle4f(yRot, 0, 1, 0),
                        new Vector3f(0.4f, 0.4f, 0.4f), 4);
            }

            // Backbone B
            for (int i = 0; i < backboneB.size(); i++) {
                double t = (double) i / BACKBONE_COUNT;
                double baseAngle = t * REVOLUTIONS * 2.0 * Math.PI + Math.PI + yRot;
                float px = (float) (Math.cos(baseAngle) * HELIX_RADIUS) - 0.2f;
                float pz = (float) (Math.sin(baseAngle) * HELIX_RADIUS) - 0.2f;
                float py = (float) (t * HELIX_HEIGHT) - 0.2f;
                backboneB.get(i).animateTo(
                        new Vector3f(px, py, pz),
                        new AxisAngle4f(yRot, 0, 1, 0),
                        new Vector3f(0.4f, 0.4f, 0.4f), 4);
            }

            // Rungs: pulse scale and rotate with structure
            float rungPulse = 0.8f + 0.3f * (float) Math.sin(ticksAlive * 0.2);
            for (int r = 0; r < 5; r++) {
                int idx = r * 2 + 1;
                double t = (double) idx / BACKBONE_COUNT;
                double angleA = t * REVOLUTIONS * 2.0 * Math.PI + yRot;
                double angleB = angleA + Math.PI;
                double axA = Math.cos(angleA) * HELIX_RADIUS;
                double azA = Math.sin(angleA) * HELIX_RADIUS;
                double axB = Math.cos(angleB) * HELIX_RADIUS;
                double azB = Math.sin(angleB) * HELIX_RADIUS;
                float py = (float) (t * HELIX_HEIGHT) - 0.075f;

                for (int j = 0; j < 2; j++) {
                    double lerp = (j == 0) ? 0.25 : 0.75;
                    float rx = (float) (axA + (axB - axA) * lerp) - 0.075f;
                    float rz = (float) (azA + (azB - azA) * lerp) - 0.075f;
                    int rungIdx = r * 2 + j;
                    if (rungIdx < rungs.size()) {
                        rungs.get(rungIdx).animateTo(
                                new Vector3f(rx, py, rz),
                                new AxisAngle4f(yRot, 0, 1, 0),
                                new Vector3f(0.15f, 0.15f, rungPulse), 4);
                    }
                }
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 3.5, 0), 3, 2.0, 255, 150, 30, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireDNA(plugin); }
    }

    // ================================================================
    // 35. INFERNAL TORUS RING (28 blocks)
    // Torus (donut): 20 blocks positioned via torus math
    // x=(R+r*cos(v))*cos(u), z=(R+r*cos(v))*sin(u), y=r*sin(v)
    // R=3, r=1. 8 inner accent blocks. Y-rotation + X-tumble via
    // Transform creating a tumbling donut effect.
    // damage=25, radius=6, ticks-between-damage=20, duration=500
    // ================================================================
    public static class InfernalTorusRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> torusBlocks = new ArrayList<>();  // 20
        private final List<BlockDisplayHandle> innerAccents = new ArrayList<>(); // 8
        private static final double MAJOR_R = 3.0;
        private static final double MINOR_R = 1.0;
        private static final int TORUS_COUNT = 20;
        // Pre-computed torus u,v angles for each block
        private final double[] torusU = new double[TORUS_COUNT];
        private final double[] torusV = new double[TORUS_COUNT];

        public InfernalTorusRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_infernal_torus_ring", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Distribute 20 blocks across torus surface
            // Use a grid: 5 steps in u (around the tube), 4 steps in v (around the ring)
            int uSteps = 5;
            int vSteps = 4;
            int idx = 0;
            for (int ui = 0; ui < uSteps; ui++) {
                for (int vi = 0; vi < vSteps; vi++) {
                    if (idx >= TORUS_COUNT) break;
                    double u = (2.0 * Math.PI * ui) / uSteps;
                    double v = (2.0 * Math.PI * vi) / vSteps;
                    torusU[idx] = u;
                    torusV[idx] = v;

                    double px = (MAJOR_R + MINOR_R * Math.cos(v)) * Math.cos(u);
                    double pz = (MAJOR_R + MINOR_R * Math.cos(v)) * Math.sin(u);
                    double py = MINOR_R * Math.sin(v);

                    Location loc = center.clone().add(px, py + 3.0, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 20).interpolation(4, 0);
                    torusBlocks.add(h);
                    spawnedEntities.add(h.entity());
                    idx++;
                }
            }

            // 8 inner accent blocks at the center ring (v=0, evenly around u)
            for (int i = 0; i < 8; i++) {
                double u = (2.0 * Math.PI * i) / 8;
                double px = MAJOR_R * Math.cos(u);
                double pz = MAJOR_R * Math.sin(u);
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.3f, 0.3f, 0.3f).glow(240, 80, 30).interpolation(4, 0);
                innerAccents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Y-rotation at 3 deg/tick
            float yRot = (float) Math.toRadians(ticksAlive * 3.0);
            // X-tumble at 1.5 deg/tick for donut tumbling effect
            float xTumble = (float) Math.toRadians(ticksAlive * 1.5);

            for (int i = 0; i < torusBlocks.size(); i++) {
                double u = torusU[i] + yRot;
                double v = torusV[i] + xTumble;

                double px = (MAJOR_R + MINOR_R * Math.cos(v)) * Math.cos(u);
                double pz = (MAJOR_R + MINOR_R * Math.cos(v)) * Math.sin(u);
                double py = MINOR_R * Math.sin(v);

                torusBlocks.get(i).animateTo(
                        new Vector3f((float) px - 0.25f, (float) py + 2.75f, (float) pz - 0.25f),
                        new AxisAngle4f(xTumble, 1, 0, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), 4);
            }

            // Inner accents orbit with Y rotation
            for (int i = 0; i < innerAccents.size(); i++) {
                double u = (2.0 * Math.PI * i) / 8 + yRot;
                float px = (float) (MAJOR_R * Math.cos(u)) - 0.15f;
                float pz = (float) (MAJOR_R * Math.sin(u)) - 0.15f;
                innerAccents.get(i).animateTo(
                        new Vector3f(px, 2.85f, pz),
                        new AxisAngle4f(yRot, 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 0.3f), 4);
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 4, 3.0, 255, 80, 20, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalTorusRing(plugin); }
    }

    // ================================================================
    // 36. MAGMA MOBIUS STRIP (26 blocks)
    // 20 flat plates scale(0.6,0.08,0.4) along a Mobius strip
    // parametric path (180-degree twist over one loop). 6 edge accent
    // blocks. Continuous rotation appearing to flow along surface.
    // x=(R+s*cos(u/2))*cos(u), z=(R+s*cos(u/2))*sin(u), y=s*sin(u/2)
    // damage=20, radius=5, ticks-between-damage=20, duration=500
    // ================================================================
    public static class MagmaMobiusStrip extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> plates = new ArrayList<>();   // 20
        private final List<BlockDisplayHandle> edgeAccents = new ArrayList<>(); // 6
        private static final double R = 3.0;
        private static final double STRIP_WIDTH = 0.6;
        private static final int PLATE_COUNT = 20;

        public MagmaMobiusStrip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_magma_mobius_strip", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 plates along Mobius strip (s=0, center of strip)
            for (int i = 0; i < PLATE_COUNT; i++) {
                double u = (2.0 * Math.PI * i) / PLATE_COUNT;
                double s = 0; // center of strip width
                double px = (R + s * Math.cos(u / 2.0)) * Math.cos(u);
                double pz = (R + s * Math.cos(u / 2.0)) * Math.sin(u);
                double py = s * Math.sin(u / 2.0);

                Location loc = center.clone().add(px, py + 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                // Flat plates with twist -- the rotation handles the twist
                h.scale(0.6f, 0.08f, 0.4f);
                // Twist angle is u/2 for Mobius half-twist
                float twist = (float) (u / 2.0);
                h.rotate(twist, (float) Math.cos(u), 0, (float) Math.sin(u));
                h.glow(255, 100, 20).interpolation(4, 0);
                plates.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 edge accent blocks distributed along strip at s=+STRIP_WIDTH
            for (int i = 0; i < 6; i++) {
                double u = (2.0 * Math.PI * i) / 6;
                double s = STRIP_WIDTH;
                double px = (R + s * Math.cos(u / 2.0)) * Math.cos(u);
                double pz = (R + s * Math.cos(u / 2.0)) * Math.sin(u);
                double py = s * Math.sin(u / 2.0);

                Location loc = center.clone().add(px, py + 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.25f, 0.25f, 0.25f).glow(240, 180, 30).interpolation(4, 0);
                edgeAccents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Flow rotation: shift u parameter over time to create flowing surface illusion
            float flow = (float) Math.toRadians(ticksAlive * 4.0);

            for (int i = 0; i < plates.size(); i++) {
                double u = (2.0 * Math.PI * i) / PLATE_COUNT + flow;
                double s = 0;
                double px = (R + s * Math.cos(u / 2.0)) * Math.cos(u);
                double pz = (R + s * Math.cos(u / 2.0)) * Math.sin(u);
                double py = s * Math.sin(u / 2.0);

                float twist = (float) (u / 2.0);

                plates.get(i).animateTo(
                        new Vector3f((float) px - 0.3f, (float) py + 2.96f, (float) pz - 0.2f),
                        new AxisAngle4f(twist, (float) Math.cos(u), 0, (float) Math.sin(u)),
                        new Vector3f(0.6f, 0.08f, 0.4f), 4);
            }

            // Edge accents follow with same flow
            for (int i = 0; i < edgeAccents.size(); i++) {
                double u = (2.0 * Math.PI * i) / 6 + flow;
                double s = STRIP_WIDTH;
                double px = (R + s * Math.cos(u / 2.0)) * Math.cos(u);
                double pz = (R + s * Math.cos(u / 2.0)) * Math.sin(u);
                double py = s * Math.sin(u / 2.0);

                edgeAccents.get(i).animateTo(
                        new Vector3f((float) px - 0.125f, (float) py + 2.875f, (float) pz - 0.125f),
                        new AxisAngle4f((float) (u / 2.0), (float) Math.cos(u), 0, (float) Math.sin(u)),
                        new Vector3f(0.25f, 0.25f, 0.25f), 4);
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 3, 2.5, 255, 120, 30, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaMobiusStrip(plugin); }
    }

    // ================================================================
    // 37. DOOM TESSERACT (32 blocks)
    // Hypercube: inner cube 8 corners, outer cube 8 corners (larger),
    // 16 connecting beams scale(0.1,0.1,0.5). Inner rotates around Y,
    // outer rotates around Z -- creating a 4D rotation illusion.
    // damage=25, radius=7, ticks-between-damage=20, duration=500
    // ================================================================
    public static class DoomTesseract extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> innerCube = new ArrayList<>();     // 8
        private final List<BlockDisplayHandle> outerCube = new ArrayList<>();     // 8
        private final List<BlockDisplayHandle> beams = new ArrayList<>();         // 16
        private static final float INNER_SIZE = 1.5f;
        private static final float OUTER_SIZE = 3.0f;

        // Unit cube corners: all combinations of -1,+1
        private static final float[][] CORNERS = {
                {-1, -1, -1}, {-1, -1, 1}, {-1, 1, -1}, {-1, 1, 1},
                {1, -1, -1}, {1, -1, 1}, {1, 1, -1}, {1, 1, 1}
        };

        public DoomTesseract(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_tesseract", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Inner cube: 8 corners
            for (float[] corner : CORNERS) {
                Location loc = center.clone().add(
                        corner[0] * INNER_SIZE, corner[1] * INNER_SIZE + 3.0, corner[2] * INNER_SIZE);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.35f, 0.35f, 0.35f).glow(120, 20, 80).interpolation(4, 0);
                innerCube.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer cube: 8 corners
            for (float[] corner : CORNERS) {
                Location loc = center.clone().add(
                        corner[0] * OUTER_SIZE, corner[1] * OUTER_SIZE + 3.0, corner[2] * OUTER_SIZE);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.45f, 0.45f, 0.45f).glow(180, 20, 20).interpolation(4, 0);
                outerCube.add(h);
                spawnedEntities.add(h.entity());
            }

            // 16 connecting beams: each inner corner connects to 2 outer corners
            // Connect inner[i] to outer[i] (direct), and inner[i] to outer[i^1] (adjacent)
            for (int i = 0; i < 8; i++) {
                // Beam from inner[i] to outer[i]
                float mx = (CORNERS[i][0] * INNER_SIZE + CORNERS[i][0] * OUTER_SIZE) / 2;
                float my = (CORNERS[i][1] * INNER_SIZE + CORNERS[i][1] * OUTER_SIZE) / 2 + 3.0f;
                float mz = (CORNERS[i][2] * INNER_SIZE + CORNERS[i][2] * OUTER_SIZE) / 2;
                Location loc1 = center.clone().add(mx, my, mz);
                BlockDisplayHandle b1 = displayBuilder.spawnBlock(loc1, Material.NETHER_BRICKS);
                b1.scale(0.1f, 0.1f, 0.5f).glow(200, 50, 10).interpolation(4, 0);
                beams.add(b1);
                spawnedEntities.add(b1.entity());

                // Beam from inner[i] to outer[i^1] (flip last bit for adjacent)
                int adj = i ^ 1;
                float mx2 = (CORNERS[i][0] * INNER_SIZE + CORNERS[adj][0] * OUTER_SIZE) / 2;
                float my2 = (CORNERS[i][1] * INNER_SIZE + CORNERS[adj][1] * OUTER_SIZE) / 2 + 3.0f;
                float mz2 = (CORNERS[i][2] * INNER_SIZE + CORNERS[adj][2] * OUTER_SIZE) / 2;
                Location loc2 = center.clone().add(mx2, my2, mz2);
                BlockDisplayHandle b2 = displayBuilder.spawnBlock(loc2, Material.NETHER_BRICKS);
                b2.scale(0.1f, 0.1f, 0.5f).glow(200, 50, 10).interpolation(4, 0);
                beams.add(b2);
                spawnedEntities.add(b2.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            float yRot = (float) Math.toRadians(ticksAlive * 2.0); // Inner: Y rotation
            float zRot = (float) Math.toRadians(ticksAlive * 1.5); // Outer: Z rotation

            // Inner cube rotates around Y
            for (int i = 0; i < innerCube.size(); i++) {
                float cx = CORNERS[i][0] * INNER_SIZE;
                float cy = CORNERS[i][1] * INNER_SIZE;
                float cz = CORNERS[i][2] * INNER_SIZE;
                // Rotate around Y
                float rx = (float) (cx * Math.cos(yRot) - cz * Math.sin(yRot));
                float rz = (float) (cx * Math.sin(yRot) + cz * Math.cos(yRot));
                innerCube.get(i).animateTo(
                        new Vector3f(rx - 0.175f, cy + 2.825f, rz - 0.175f),
                        new AxisAngle4f(yRot, 0, 1, 0),
                        new Vector3f(0.35f, 0.35f, 0.35f), 4);
            }

            // Outer cube rotates around Z
            for (int i = 0; i < outerCube.size(); i++) {
                float cx = CORNERS[i][0] * OUTER_SIZE;
                float cy = CORNERS[i][1] * OUTER_SIZE;
                float cz = CORNERS[i][2] * OUTER_SIZE;
                // Rotate around Z
                float rx = (float) (cx * Math.cos(zRot) - cy * Math.sin(zRot));
                float ry = (float) (cx * Math.sin(zRot) + cy * Math.cos(zRot));
                outerCube.get(i).animateTo(
                        new Vector3f(rx - 0.225f, ry + 2.775f, cz - 0.225f),
                        new AxisAngle4f(zRot, 0, 0, 1),
                        new Vector3f(0.45f, 0.45f, 0.45f), 4);
            }

            // Beams follow -- stretch between the rotated positions
            for (int i = 0; i < 8; i++) {
                // Inner corner position (Y-rotated)
                float icx = CORNERS[i][0] * INNER_SIZE;
                float icy = CORNERS[i][1] * INNER_SIZE;
                float icz = CORNERS[i][2] * INNER_SIZE;
                float irx = (float) (icx * Math.cos(yRot) - icz * Math.sin(yRot));
                float irz = (float) (icx * Math.sin(yRot) + icz * Math.cos(yRot));

                // Outer corner position (Z-rotated) for direct beam
                float ocx = CORNERS[i][0] * OUTER_SIZE;
                float ocy = CORNERS[i][1] * OUTER_SIZE;
                float ocz = CORNERS[i][2] * OUTER_SIZE;
                float orx = (float) (ocx * Math.cos(zRot) - ocy * Math.sin(zRot));
                float ory = (float) (ocx * Math.sin(zRot) + ocy * Math.cos(zRot));

                // Beam 1: midpoint of inner[i] -> outer[i]
                float mx1 = (irx + orx) / 2;
                float my1 = (icy + ory) / 2 + 3.0f;
                float mz1 = (irz + ocz) / 2;
                int b1Idx = i * 2;
                if (b1Idx < beams.size()) {
                    beams.get(b1Idx).animateTo(
                            new Vector3f(mx1 - 0.05f, my1 - 0.05f, mz1 - 0.25f),
                            new AxisAngle4f((yRot + zRot) / 2, 0.5f, 0.5f, 0),
                            new Vector3f(0.1f, 0.1f, 0.5f), 4);
                }

                // Beam 2: midpoint of inner[i] -> outer[i^1]
                int adj = i ^ 1;
                float aocx = CORNERS[adj][0] * OUTER_SIZE;
                float aocy = CORNERS[adj][1] * OUTER_SIZE;
                float aocz = CORNERS[adj][2] * OUTER_SIZE;
                float aorx = (float) (aocx * Math.cos(zRot) - aocy * Math.sin(zRot));
                float aory = (float) (aocx * Math.sin(zRot) + aocy * Math.cos(zRot));

                float mx2 = (irx + aorx) / 2;
                float my2 = (icy + aory) / 2 + 3.0f;
                float mz2 = (irz + aocz) / 2;
                int b2Idx = i * 2 + 1;
                if (b2Idx < beams.size()) {
                    beams.get(b2Idx).animateTo(
                            new Vector3f(mx2 - 0.05f, my2 - 0.05f, mz2 - 0.25f),
                            new AxisAngle4f((yRot + zRot) / 2, 0.5f, 0.5f, 0),
                            new Vector3f(0.1f, 0.1f, 0.5f), 4);
                }
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.darkPurpleDust(center.clone().add(0, 3, 0), 5, 3.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoomTesseract(plugin); }
    }

    // ================================================================
    // 38. INFERNAL GEAR (30 blocks)
    // Main gear: 12 outer teeth (rectangular protrusions), 12 inner
    // ring blocks, 4 axle center blocks, 2 small meshing gears (1 block
    // each serving as center). Main rotates clockwise, small gears
    // counter-rotate proportionally via Transform.
    // damage=30, radius=6, ticks-between-damage=25, duration=500
    // ================================================================
    public static class InfernalGear extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> teeth = new ArrayList<>();       // 12
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();   // 12
        private final List<BlockDisplayHandle> axle = new ArrayList<>();        // 4
        private final List<BlockDisplayHandle> smallGearA = new ArrayList<>();  // ~6 blocks
        private final List<BlockDisplayHandle> smallGearB = new ArrayList<>();  // ~6 blocks (total small: includes teeth)
        private static final double MAIN_RADIUS = 3.0;
        private static final double TOOTH_RADIUS = 3.8;
        private static final double SMALL_RADIUS = 1.2;
        private static final double SMALL_OFFSET_A = 4.5; // distance from center for small gear A
        private static final double SMALL_OFFSET_B_ANGLE = Math.PI; // opposite side

        public InfernalGear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_infernal_gear", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main gear inner ring: 12 blocks
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * MAIN_RADIUS;
                double pz = Math.sin(angle) * MAIN_RADIUS;
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.4f, 0.3f, 0.4f).glow(20, 10, 5).interpolation(4, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Main gear teeth: 12 rectangular protrusions
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * TOOTH_RADIUS;
                double pz = Math.sin(angle) * TOOTH_RADIUS;
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                // Rectangular tooth pointing outward
                h.scale(0.5f, 0.4f, 0.2f).glow(200, 200, 200).interpolation(4, 0);
                teeth.add(h);
                spawnedEntities.add(h.entity());
            }

            // Axle: 4 center blocks
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4;
                double px = Math.cos(angle) * 0.3;
                double pz = Math.sin(angle) * 0.3;
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.3f, 0.5f, 0.3f).glow(240, 80, 30).interpolation(4, 0);
                axle.add(h);
                spawnedEntities.add(h.entity());
            }

            // Small meshing gear A (offset to the right): 1 center + ~5 teeth-like blocks
            // Positioned at (SMALL_OFFSET_A, 0, 0)
            // 6 blocks total for this small gear
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6;
                double r = (i % 2 == 0) ? SMALL_RADIUS : SMALL_RADIUS * 0.5;
                double px = SMALL_OFFSET_A + Math.cos(angle) * r;
                double pz = Math.sin(angle) * r;
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.3f, 0.3f, 0.3f).glow(200, 50, 10).interpolation(4, 0);
                smallGearA.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Main gear rotates clockwise at 2 deg/tick
            float mainRot = (float) Math.toRadians(-ticksAlive * 2.0);
            // Small gears counter-rotate at proportional speed (main_radius/small_radius ratio)
            float smallRot = (float) Math.toRadians(ticksAlive * 2.0 * (MAIN_RADIUS / SMALL_RADIUS));

            // Inner ring
            for (int i = 0; i < innerRing.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / 12 + mainRot;
                float px = (float) (Math.cos(baseAngle) * MAIN_RADIUS) - 0.2f;
                float pz = (float) (Math.sin(baseAngle) * MAIN_RADIUS) - 0.2f;
                innerRing.get(i).animateTo(
                        new Vector3f(px, 2.85f, pz),
                        new AxisAngle4f(mainRot, 0, 1, 0),
                        new Vector3f(0.4f, 0.3f, 0.4f), 4);
            }

            // Teeth
            for (int i = 0; i < teeth.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / 12 + mainRot;
                float px = (float) (Math.cos(baseAngle) * TOOTH_RADIUS) - 0.25f;
                float pz = (float) (Math.sin(baseAngle) * TOOTH_RADIUS) - 0.1f;
                teeth.get(i).animateTo(
                        new Vector3f(px, 2.8f, pz),
                        new AxisAngle4f(mainRot + (float) baseAngle, 0, 1, 0),
                        new Vector3f(0.5f, 0.4f, 0.2f), 4);
            }

            // Axle
            for (int i = 0; i < axle.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / 4 + mainRot;
                float px = (float) (Math.cos(baseAngle) * 0.3) - 0.15f;
                float pz = (float) (Math.sin(baseAngle) * 0.3) - 0.15f;
                axle.get(i).animateTo(
                        new Vector3f(px, 2.75f, pz),
                        new AxisAngle4f(mainRot, 0, 1, 0),
                        new Vector3f(0.3f, 0.5f, 0.3f), 4);
            }

            // Small gear A: counter-rotates around its own center
            for (int i = 0; i < smallGearA.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / 6 + smallRot;
                double r = (i % 2 == 0) ? SMALL_RADIUS : SMALL_RADIUS * 0.5;
                float px = (float) (SMALL_OFFSET_A + Math.cos(baseAngle) * r) - 0.15f;
                float pz = (float) (Math.sin(baseAngle) * r) - 0.15f;
                smallGearA.get(i).animateTo(
                        new Vector3f(px, 2.85f, pz),
                        new AxisAngle4f(smallRot, 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 0.3f), 4);
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 3, 2.5, 200, 200, 200, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalGear(plugin); }
    }

    // ================================================================
    // 39. MAGMA ASTROLABE (28 blocks)
    // 3 nested rings at different tilts (0, 30, 60 degrees) with
    // 10, 8, 6 blocks respectively. Each rotates on its tilted axis
    // via Transform. Central pointer (4 blocks) tracks nearest player
    // direction. 4 accent blocks.
    // Total: 10 + 8 + 6 + 4 = 28 blocks (pointer counts as accents)
    // damage=20, radius=7, ticks-between-damage=15, duration=500
    // ================================================================
    public static class MagmaAstrolabe extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringOuter = new ArrayList<>();   // 10 blocks, tilt 0
        private final List<BlockDisplayHandle> ringMiddle = new ArrayList<>();  // 8 blocks, tilt 30
        private final List<BlockDisplayHandle> ringInner = new ArrayList<>();   // 6 blocks, tilt 60
        private final List<BlockDisplayHandle> pointer = new ArrayList<>();     // 4 blocks
        private static final double R_OUTER = 3.5;
        private static final double R_MIDDLE = 2.5;
        private static final double R_INNER = 1.5;

        public MagmaAstrolabe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_magma_astrolabe", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring (tilt 0 deg) -- 10 blocks in horizontal circle
            for (int i = 0; i < 10; i++) {
                double angle = (2.0 * Math.PI * i) / 10;
                double px = Math.cos(angle) * R_OUTER;
                double pz = Math.sin(angle) * R_OUTER;
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.35f, 0.15f, 0.35f).glow(200, 50, 10).interpolation(4, 0);
                ringOuter.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring (tilt 30 deg) -- 8 blocks
            double tiltM = Math.toRadians(30);
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * R_MIDDLE;
                double py0 = Math.sin(angle) * R_MIDDLE * Math.sin(tiltM);
                double pz = Math.sin(angle) * R_MIDDLE * Math.cos(tiltM);
                Location loc = center.clone().add(px, py0 + 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(0.3f, 0.12f, 0.3f).glow(255, 100, 20).interpolation(4, 0);
                ringMiddle.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner ring (tilt 60 deg) -- 6 blocks
            double tiltI = Math.toRadians(60);
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6;
                double px = Math.cos(angle) * R_INNER;
                double py0 = Math.sin(angle) * R_INNER * Math.sin(tiltI);
                double pz = Math.sin(angle) * R_INNER * Math.cos(tiltI);
                Location loc = center.clone().add(px, py0 + 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.25f, 0.1f, 0.25f).glow(240, 80, 30).interpolation(4, 0);
                ringInner.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central pointer: 4 elongated blocks pointing in a direction
            for (int i = 0; i < 4; i++) {
                double dist = 0.3 * (i + 1);
                Location loc = center.clone().add(dist, 3.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                float scaleX = 0.1f + 0.05f * (3 - i); // Taper: thicker at base
                h.scale(scaleX, 0.1f, 0.1f).glow(220, 180, 30).interpolation(4, 0);
                pointer.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Each ring rotates on its tilted axis at different speeds
            float rotOuter = (float) Math.toRadians(ticksAlive * 2.0);
            float rotMiddle = (float) Math.toRadians(ticksAlive * 3.0);
            float rotInner = (float) Math.toRadians(ticksAlive * 5.0);

            // Outer ring (tilt 0 = horizontal, rotate around Y)
            for (int i = 0; i < ringOuter.size(); i++) {
                double angle = (2.0 * Math.PI * i) / 10 + rotOuter;
                float px = (float) (Math.cos(angle) * R_OUTER) - 0.175f;
                float pz = (float) (Math.sin(angle) * R_OUTER) - 0.175f;
                ringOuter.get(i).animateTo(
                        new Vector3f(px, 2.925f, pz),
                        new AxisAngle4f(rotOuter, 0, 1, 0),
                        new Vector3f(0.35f, 0.15f, 0.35f), 4);
            }

            // Middle ring (tilt 30 deg, rotate around tilted axis)
            double tiltM = Math.toRadians(30);
            for (int i = 0; i < ringMiddle.size(); i++) {
                double angle = (2.0 * Math.PI * i) / 8 + rotMiddle;
                float px = (float) (Math.cos(angle) * R_MIDDLE) - 0.15f;
                float py0 = (float) (Math.sin(angle) * R_MIDDLE * Math.sin(tiltM));
                float pz = (float) (Math.sin(angle) * R_MIDDLE * Math.cos(tiltM)) - 0.15f;
                ringMiddle.get(i).animateTo(
                        new Vector3f(px, py0 + 2.94f, pz),
                        new AxisAngle4f(rotMiddle, 0, (float) Math.cos(tiltM), (float) Math.sin(tiltM)),
                        new Vector3f(0.3f, 0.12f, 0.3f), 4);
            }

            // Inner ring (tilt 60 deg)
            double tiltI = Math.toRadians(60);
            for (int i = 0; i < ringInner.size(); i++) {
                double angle = (2.0 * Math.PI * i) / 6 + rotInner;
                float px = (float) (Math.cos(angle) * R_INNER) - 0.125f;
                float py0 = (float) (Math.sin(angle) * R_INNER * Math.sin(tiltI));
                float pz = (float) (Math.sin(angle) * R_INNER * Math.cos(tiltI)) - 0.125f;
                ringInner.get(i).animateTo(
                        new Vector3f(px, py0 + 2.95f, pz),
                        new AxisAngle4f(rotInner, 0, (float) Math.cos(tiltI), (float) Math.sin(tiltI)),
                        new Vector3f(0.25f, 0.1f, 0.25f), 4);
            }

            // Pointer tracks nearest player
            Player nearest = findNearestPlayer(center, 20.0);
            float pointerAngle;
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                pointerAngle = (float) Math.atan2(dz, dx);
            } else {
                pointerAngle = (float) Math.toRadians(ticksAlive * 1.0);
            }

            for (int i = 0; i < pointer.size(); i++) {
                double dist = 0.3 * (i + 1);
                float px = (float) (Math.cos(pointerAngle) * dist) - 0.05f;
                float pz = (float) (Math.sin(pointerAngle) * dist) - 0.05f;
                float scaleX = 0.1f + 0.05f * (3 - i);
                pointer.get(i).animateTo(
                        new Vector3f(px, 2.95f, pz),
                        new AxisAngle4f(pointerAngle, 0, 1, 0),
                        new Vector3f(scaleX, 0.1f, 0.1f), 4);
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 3, 2.5, 220, 180, 30, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaAstrolabe(plugin); }
    }

    // ================================================================
    // 40. HELLFIRE SUNDIAL (26 blocks)
    // Base circle: 10 flat blocks. Gnomon: 4 elongated blocks tilted
    // 45 degrees. 8 hour markers evenly spaced around the base.
    // 4 shadow blocks that follow gnomon rotation (cast shadow effect).
    // Y-rotation for the gnomon, markers stay fixed.
    // damage=25, radius=6, ticks-between-damage=25, duration=500
    // ================================================================
    public static class HellfireSundial extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> base = new ArrayList<>();       // 10
        private final List<BlockDisplayHandle> gnomon = new ArrayList<>();     // 4
        private final List<BlockDisplayHandle> markers = new ArrayList<>();    // 8
        private final List<BlockDisplayHandle> shadow = new ArrayList<>();     // 4
        private static final double BASE_RADIUS = 3.5;
        private static final double MARKER_RADIUS = 4.0;

        public HellfireSundial(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_hellfire_sundial", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base circle: 10 flat blocks
            for (int i = 0; i < 10; i++) {
                double angle = (2.0 * Math.PI * i) / 10;
                double px = Math.cos(angle) * BASE_RADIUS;
                double pz = Math.sin(angle) * BASE_RADIUS;
                Location loc = center.clone().add(px, 0.1, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.7f, 0.06f, 0.7f).glow(20, 10, 5).interpolation(5, 0);
                base.add(h);
                spawnedEntities.add(h.entity());
            }

            // Gnomon: 4 elongated blocks tilted at 45 degrees
            for (int i = 0; i < 4; i++) {
                double height = 0.5 + i * 0.8;
                Location loc = center.clone().add(0, height, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.15f, 0.8f, 0.15f);
                // Tilt 45 degrees on Z axis
                h.rotate((float) Math.toRadians(45), 0, 0, 1);
                h.glow(255, 100, 20).interpolation(5, 0);
                gnomon.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 hour markers evenly spaced around base edge
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * MARKER_RADIUS;
                double pz = Math.sin(angle) * MARKER_RADIUS;
                Location loc = center.clone().add(px, 0.15, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.2f, 0.2f, 0.2f).glow(240, 180, 30).interpolation(5, 0);
                markers.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 shadow blocks (dark, flat, opposite gnomon direction)
            for (int i = 0; i < 4; i++) {
                double dist = 1.0 + i * 0.8;
                Location loc = center.clone().add(dist, 0.05, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.3f, 0.03f, 0.2f).glow(10, 5, 15).interpolation(5, 0);
                shadow.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Gnomon rotates around Y slowly (simulating sun movement)
            float yRot = (float) Math.toRadians(ticksAlive * 1.5);
            float tilt45 = (float) Math.toRadians(45);

            // Gnomon blocks rotate with Y rotation, maintaining 45-degree tilt
            for (int i = 0; i < gnomon.size(); i++) {
                double height = 0.5 + i * 0.8;
                // Position along the tilted gnomon arm, rotated by yRot
                double armOffset = i * 0.3;
                float px = (float) (Math.cos(yRot) * armOffset) - 0.075f;
                float pz = (float) (Math.sin(yRot) * armOffset) - 0.075f;

                // Combine Y rotation with 45-degree tilt
                // The tilt axis rotates with the gnomon
                float tiltAxisX = (float) -Math.sin(yRot);
                float tiltAxisZ = (float) Math.cos(yRot);

                gnomon.get(i).animateTo(
                        new Vector3f(px, (float) height, pz),
                        new AxisAngle4f(tilt45, tiltAxisX, 0, tiltAxisZ),
                        new Vector3f(0.15f, 0.8f, 0.15f), 5);
            }

            // Shadow blocks follow gnomon rotation, cast on ground opposite side
            float shadowAngle = yRot + (float) Math.PI; // Opposite direction
            for (int i = 0; i < shadow.size(); i++) {
                double dist = 1.0 + i * 0.8;
                float sx = (float) (Math.cos(shadowAngle) * dist) - 0.15f;
                float sz = (float) (Math.sin(shadowAngle) * dist) - 0.1f;
                // Shadow gets wider and thinner farther from gnomon
                float width = 0.3f + i * 0.1f;
                float alpha = 0.03f - i * 0.005f;
                if (alpha < 0.01f) alpha = 0.01f;

                shadow.get(i).animateTo(
                        new Vector3f(sx, 0.05f, sz),
                        new AxisAngle4f(shadowAngle, 0, 1, 0),
                        new Vector3f(width, alpha, 0.2f), 5);
            }

            // Base and markers stay fixed (they are the sundial face)
            // But pulse the marker closest to where the gnomon shadow points
            int activeMarker = (int) ((yRot / (2 * Math.PI)) * 8) % 8;
            if (activeMarker < 0) activeMarker += 8;
            for (int i = 0; i < markers.size(); i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                float px = (float) (Math.cos(angle) * MARKER_RADIUS) - 0.1f;
                float pz = (float) (Math.sin(angle) * MARKER_RADIUS) - 0.1f;
                float sz = (i == activeMarker) ? 0.35f : 0.2f;
                markers.get(i).animateTo(
                        new Vector3f(px, 0.15f, pz),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sz, sz, sz), 5);
                if (i == activeMarker) {
                    markers.get(i).glow(255, 255, 100);
                } else {
                    markers.get(i).glow(240, 180, 30);
                }
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 3, 2.0, 240, 180, 30, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireSundial(plugin); }
    }
}
