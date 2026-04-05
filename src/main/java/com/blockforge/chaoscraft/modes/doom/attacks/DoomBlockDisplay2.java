package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Doom Mode — BLOCK DISPLAY ATTACKS (Part 2)
 * Attacks 18-34: Creatures, Area Denial, Complex Geometry, and Explosive/Burst.
 *
 * Doom palette:
 * - Lava orange: RGB(255, 100, 20)
 * - Hellfire red: RGB(200, 50, 10)
 * - Charred black: RGB(20, 10, 5)
 * - Ember glow: RGB(240, 80, 30)
 *
 * Materials: MAGMA_BLOCK, NETHERRACK, NETHER_BRICKS, RED_NETHER_BRICKS,
 *            BLACKSTONE, POLISHED_BLACKSTONE, CRYING_OBSIDIAN, SHROOMLIGHT,
 *            BASALT, DEEPSLATE, COAL_BLOCK, OBSIDIAN, RED_CONCRETE,
 *            BLACK_CONCRETE, ORANGE_CONCRETE, GLOWSTONE
 *
 * Particles: LAVA, FLAME, SMOKE, LARGE_SMOKE, LANDING_LAVA
 * Sounds: BLOCK_LAVA_POP, ENTITY_BLAZE_SHOOT, BLOCK_LAVA_EXTINGUISH,
 *         ENTITY_GENERIC_EXPLODE, ENTITY_WITHER_SHOOT
 */
public final class DoomBlockDisplay2 {
    private DoomBlockDisplay2() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Creature-like (18-22)
        registry.register(new LavaWorm(plugin));
        registry.register(new InfernalSpider(plugin));
        registry.register(new MagmaGolem(plugin));
        registry.register(new HellfirePhoenix(plugin));
        registry.register(new NetherSerpent(plugin));
        // Area Denial (23-27)
        registry.register(new LavaGeyserField(plugin));
        registry.register(new BrimstoneMinefield(plugin));
        registry.register(new HellfireBarricade(plugin));
        registry.register(new MagmaFlowerPatch(plugin));
        registry.register(new InfernalChessboard(plugin));
        // Complex Geometry (28-31)
        registry.register(new DoomTriangle(plugin));
        registry.register(new InfernalHelix(plugin));
        registry.register(new MagmaStargate(plugin));
        registry.register(new HellfireCrown(plugin));
        // Explosive/Burst (32-34)
        registry.register(new InfernalSupernova(plugin));
        registry.register(new LavaGrenadeCluster(plugin));
        registry.register(new BrimstoneBarrage(plugin));
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
    // CREATURE-LIKE ATTACKS (18-22)
    // ================================================================

    // ================================================================
    // 18. LAVA WORM — Segmented worm: 14 magma blocks in sine wave,
    //     head block larger. Slithers forward animating sine wave positions.
    //     14 body segments + 1 head = 15 blocks (MAGMA_BLOCK)
    // ================================================================
    public static class LavaWorm extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private BlockDisplayHandle head;
        private double pathProgress = 0;

        public LavaWorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_worm", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Head — larger magma block at front
            Location headLoc = center.clone().add(0, 0.5, 0);
            head = displayBuilder.spawnBlock(headLoc, Material.MAGMA_BLOCK);
            head.scale(1.6f, 1.2f, 1.6f)
                .glow(255, 100, 20)
                .interpolation(3, 0);
            spawnedEntities.add(head.entity());

            // 14 body segments — decreasing size toward tail
            for (int i = 0; i < 14; i++) {
                double offset = -(i + 1) * 0.9;
                Location segLoc = center.clone().add(0, 0.3, offset);
                float segScale = 1.3f - (i * 0.06f);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.MAGMA_BLOCK);
                seg.scale(segScale, 0.8f, 0.9f)
                   .glow(240, 80, 30)
                   .interpolation(3, 0);
                spawnedEntities.add(seg.entity());
                segments.add(seg);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            pathProgress += 0.07;

            // Head position — circling motion, pulls toward nearest player
            double headX = Math.cos(pathProgress) * 5.0;
            double headZ = Math.sin(pathProgress) * 5.0;

            Player nearest = findNearestPlayer(c, 20);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - (center.getX() + headX);
                double dz = nearest.getLocation().getZ() - (center.getZ() + headZ);
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.5) {
                    headX += (dx / dist) * 0.4;
                    headZ += (dz / dist) * 0.4;
                }
            }

            Location headLoc = center.clone().add(headX, 0.5, headZ);
            head.entity().teleport(headLoc);
            setCenter(headLoc);

            // Body segments follow with sine-wave slither
            for (int i = 0; i < segments.size(); i++) {
                double delay = (i + 1) * 0.35;
                double segAngle = pathProgress - delay;
                double segX = Math.cos(segAngle) * 5.0;
                double segZ = Math.sin(segAngle) * 5.0;
                // Lateral sine wave for slithering
                double sineOffset = Math.sin(tick * 0.18 + i * 0.7) * 0.6;
                double perpX = -Math.sin(segAngle) * sineOffset;
                double perpZ = Math.cos(segAngle) * sineOffset;
                // Vertical sine wave
                double yBob = Math.sin(tick * 0.12 + i * 0.5) * 0.3;

                Location segLoc = center.clone().add(segX + perpX, 0.3 + yBob, segZ + perpZ);
                segments.get(i).entity().teleport(segLoc);
            }

            // Lava trail particles
            if (tick % 3 == 0) {
                headLoc.getWorld().spawnParticle(Particle.LAVA, headLoc, 4, 0.3, 0.2, 0.3, 0.01);
                DisplayBuilder.dustParticles(headLoc, 3, 0.4, 255, 100, 20, 1.2f);
            }

            // Rumbling sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(headLoc, Sound.BLOCK_LAVA_POP, 0.8f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaWorm(plugin); }
    }

    // ================================================================
    // 19. INFERNAL SPIDER — Spider shape: 10 body blocks (central cluster),
    //     8 leg blocks extending outward-downward. Legs animate walking.
    //     10 body (NETHER_BRICKS) + 8 legs (BLACKSTONE) = 18 blocks
    // ================================================================
    public static class InfernalSpider extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private double posX, posZ;
        private double targetX, targetZ;
        private boolean lunging = false;
        private int lungeTick = 0;

        public InfernalSpider(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_spider", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            this.posX = center.getX();
            this.posZ = center.getZ();
            this.targetX = posX;
            this.targetZ = posZ;
            World w = center.getWorld();
            if (w == null) return;

            // Body: 10 blocks forming a central cluster (abdomen + thorax + head)
            double[][] bodyOffsets = {
                // Rear abdomen (3 blocks)
                {-0.4, 0.8, -0.8}, {0.0, 1.0, -0.8}, {0.4, 0.8, -0.8},
                // Mid thorax (4 blocks, 2x2)
                {-0.4, 0.8, -0.1}, {0.4, 0.8, -0.1},
                {-0.4, 0.8, 0.3},  {0.4, 0.8, 0.3},
                // Upper thorax
                {0.0, 1.2, 0.1},
                // Head (2 blocks, slightly raised)
                {-0.2, 1.1, 0.7},  {0.2, 1.1, 0.7}
            };
            for (double[] off : bodyOffsets) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                    center.clone().add(off[0], off[1], off[2]), Material.NETHER_BRICKS);
                float sc = (off[1] > 1.0) ? 0.65f : 0.85f;
                b.scale(sc, 0.55f, sc).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(b.entity());
                bodyBlocks.add(b);
            }

            // Legs: 8 legs (4 pairs), angled outward-downward (BLACKSTONE, thin)
            double[][] legBases = {
                {-1.3, 0.2, -0.7}, {1.3, 0.2, -0.7},   // rear pair
                {-1.4, 0.2, -0.2}, {1.4, 0.2, -0.2},   // mid-rear pair
                {-1.4, 0.2, 0.2},  {1.4, 0.2, 0.2},    // mid-front pair
                {-1.2, 0.2, 0.6},  {1.2, 0.2, 0.6}     // front pair
            };
            for (double[] off : legBases) {
                BlockDisplayHandle leg = displayBuilder.spawnBlock(
                    center.clone().add(off[0], off[1], off[2]), Material.BLACKSTONE);
                leg.scale(0.25f, 0.9f, 0.25f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(leg.entity());
                legs.add(leg);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Track nearest player
            Player nearest = findNearestPlayer(c, 25);
            if (nearest != null) {
                targetX = nearest.getLocation().getX();
                targetZ = nearest.getLocation().getZ();
            }

            double dx = targetX - posX;
            double dz = targetZ - posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);

            // Lunge when close
            if (dist < 5.0 && !lunging && tick % 50 < 2) {
                lunging = true;
                lungeTick = 0;
            }

            double speed = lunging ? 0.4 : 0.14;
            if (lunging) {
                lungeTick++;
                if (lungeTick > 12) lunging = false;
            }

            if (dist > 0.3) {
                posX += (dx / dist) * speed;
                posZ += (dz / dist) * speed;
            }

            Location spiderCenter = new Location(c.getWorld(), posX, c.getY(), posZ);
            spiderCenter.setYaw(0);
            spiderCenter.setPitch(0);
            setCenter(spiderCenter);

            // Move body blocks
            double[][] bodyOffsets = {
                {-0.4, 0.8, -0.8}, {0.0, 1.0, -0.8}, {0.4, 0.8, -0.8},
                {-0.4, 0.8, -0.1}, {0.4, 0.8, -0.1},
                {-0.4, 0.8, 0.3},  {0.4, 0.8, 0.3},
                {0.0, 1.2, 0.1},
                {-0.2, 1.1, 0.7},  {0.2, 1.1, 0.7}
            };
            for (int i = 0; i < bodyBlocks.size(); i++) {
                Location bLoc = spiderCenter.clone().add(
                    bodyOffsets[i][0], bodyOffsets[i][1], bodyOffsets[i][2]);
                bodyBlocks.get(i).entity().teleport(bLoc);
            }

            // Animate legs — alternating gait
            double[][] legBases = {
                {-1.3, 0.2, -0.7}, {1.3, 0.2, -0.7},
                {-1.4, 0.2, -0.2}, {1.4, 0.2, -0.2},
                {-1.4, 0.2, 0.2},  {1.4, 0.2, 0.2},
                {-1.2, 0.2, 0.6},  {1.2, 0.2, 0.6}
            };
            for (int i = 0; i < legs.size(); i++) {
                double legLift = Math.sin(tick * 0.35 + i * Math.PI / 4) * 0.3;
                if (legLift < 0) legLift = 0;
                double forwardStep = Math.sin(tick * 0.35 + i * Math.PI / 4) * 0.25;
                Location legLoc = spiderCenter.clone().add(
                    legBases[i][0], legBases[i][1] + legLift, legBases[i][2] + forwardStep);
                legs.get(i).entity().teleport(legLoc);
            }

            // Fire particles from body
            if (tick % 4 == 0) {
                spiderCenter.getWorld().spawnParticle(Particle.FLAME,
                    spiderCenter.clone().add(0, 1.0, 0), 5, 0.5, 0.2, 0.5, 0.01);
            }

            // Lunge impact sound
            if (lunging && lungeTick == 6) {
                DisplayBuilder.playSound(spiderCenter, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
            }

            // Ambient
            if (tick % 45 == 0) {
                DisplayBuilder.playSound(spiderCenter, Sound.BLOCK_LAVA_POP, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalSpider(plugin); }
    }

    // ================================================================
    // 20. MAGMA GOLEM — Humanoid: 12 blocks — chest (2x2), 2 arms
    //     (2 blocks each, angled), 2 legs (2 blocks each), head (1 block).
    //     Arms swing in idle animation.
    //     4 chest (MAGMA_BLOCK) + 4 arm (NETHERRACK) + 4 leg (BLACKSTONE)
    //     + 1 head (SHROOMLIGHT) = 13 blocks
    // ================================================================
    public static class MagmaGolem extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allParts = new ArrayList<>();
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private double posX, posZ;

        // Part offset definitions for teleporting
        private static final double[][] PART_OFFSETS = {
            // Legs: left lower, left upper, right lower, right upper (4 blocks, BLACKSTONE)
            {-0.4, 0.0, 0.0},  {-0.4, 1.0, 0.0},
            {0.4, 0.0, 0.0},   {0.4, 1.0, 0.0},
            // Chest: 2x2 (4 blocks, MAGMA_BLOCK)
            {-0.4, 2.0, -0.2}, {0.4, 2.0, -0.2},
            {-0.4, 2.0, 0.2},  {0.4, 2.0, 0.2},
            // Head (1 block, SHROOMLIGHT)
            {0.0, 3.2, 0.0},
            // Left arm: shoulder, forearm (2 blocks, NETHERRACK)
            {-1.2, 2.8, 0.0},  {-1.2, 1.8, 0.0},
            // Right arm: shoulder, forearm (2 blocks, NETHERRACK)
            {1.2, 2.8, 0.0},   {1.2, 1.8, 0.0}
        };

        public MagmaGolem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_golem", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            this.posX = center.getX();
            this.posZ = center.getZ();
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {
                // 4 legs
                Material.BLACKSTONE, Material.BLACKSTONE, Material.BLACKSTONE, Material.BLACKSTONE,
                // 4 chest
                Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK,
                // 1 head
                Material.SHROOMLIGHT,
                // 2 left arm
                Material.NETHERRACK, Material.NETHERRACK,
                // 2 right arm
                Material.NETHERRACK, Material.NETHERRACK
            };
            float[][] scales = {
                // Legs — thick
                {0.6f, 1.0f, 0.5f}, {0.6f, 1.0f, 0.5f}, {0.6f, 1.0f, 0.5f}, {0.6f, 1.0f, 0.5f},
                // Chest — wide
                {0.9f, 1.0f, 0.7f}, {0.9f, 1.0f, 0.7f}, {0.9f, 1.0f, 0.7f}, {0.9f, 1.0f, 0.7f},
                // Head — large
                {1.0f, 1.0f, 1.0f},
                // Arms — thin
                {0.4f, 0.9f, 0.4f}, {0.4f, 0.9f, 0.4f},
                {0.4f, 0.9f, 0.4f}, {0.4f, 0.9f, 0.4f}
            };

            for (int i = 0; i < PART_OFFSETS.length; i++) {
                Location loc = center.clone().add(
                    PART_OFFSETS[i][0], PART_OFFSETS[i][1], PART_OFFSETS[i][2]);
                BlockDisplayHandle part = displayBuilder.spawnBlock(loc, mats[i]);
                part.scale(scales[i][0], scales[i][1], scales[i][2])
                    .glow(255, 100, 20)
                    .interpolation(3, 0);
                spawnedEntities.add(part.entity());
                allParts.add(part);
            }

            // Track arm references (indices 9-10 = left arm, 11-12 = right arm)
            leftArm.add(allParts.get(9));
            leftArm.add(allParts.get(10));
            rightArm.add(allParts.get(11));
            rightArm.add(allParts.get(12));

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly walk toward nearest player
            Player nearest = findNearestPlayer(c, 25);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - posX;
                double dz = nearest.getLocation().getZ() - posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 1.0) {
                    posX += (dx / dist) * 0.08;
                    posZ += (dz / dist) * 0.08;
                }
            }

            Location golemBase = new Location(c.getWorld(), posX, c.getY(), posZ);
            golemBase.setYaw(0);
            golemBase.setPitch(0);
            setCenter(golemBase.clone().add(0, 2, 0)); // Center at chest height

            // Move all body parts
            for (int i = 0; i < allParts.size(); i++) {
                double ox = PART_OFFSETS[i][0];
                double oy = PART_OFFSETS[i][1];
                double oz = PART_OFFSETS[i][2];

                // Arm swing animation
                if (i >= 9 && i <= 10) {
                    // Left arm — swings forward/back
                    double swing = Math.sin(tick * 0.12) * 0.8;
                    oz += swing;
                    oy += Math.abs(Math.sin(tick * 0.12)) * 0.3;
                } else if (i >= 11 && i <= 12) {
                    // Right arm — opposite phase
                    double swing = Math.sin(tick * 0.12 + Math.PI) * 0.8;
                    oz += swing;
                    oy += Math.abs(Math.sin(tick * 0.12 + Math.PI)) * 0.3;
                }

                // Leg walking bob
                if (i < 4) {
                    double legBob = Math.sin(tick * 0.15 + (i < 2 ? 0 : Math.PI)) * 0.15;
                    oy += Math.max(0, legBob);
                }

                Location partLoc = golemBase.clone().add(ox, oy, oz);
                allParts.get(i).entity().teleport(partLoc);
            }

            // Body bob
            if (tick % 4 == 0) {
                golemBase.getWorld().spawnParticle(Particle.LAVA,
                    golemBase.clone().add(0, 2.5, 0), 3, 0.4, 0.3, 0.4, 0.01);
            }

            // Stomp sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(golemBase, Sound.ENTITY_IRON_GOLEM_STEP, 0.8f, 0.4f);
            }

            // Roar
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(golemBase, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaGolem(plugin); }
    }

    // ================================================================
    // 21. HELLFIRE PHOENIX — Bird shape: 10 wing blocks (5 per side),
    //     3 body blocks, 2 tail blocks. Wings flap (rotate up/down).
    //     10 wing (SHROOMLIGHT) + 3 body (MAGMA_BLOCK) + 2 tail (NETHERRACK)
    //     = 15 blocks
    // ================================================================
    public static class HellfirePhoenix extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private double flyAngle = 0;

        // Wing offsets (relative to body center) — 5 per side, spreading outward
        private static final double[][] LEFT_WING = {
            {-1.0, 0.3, 0.0}, {-2.0, 0.5, -0.1}, {-3.0, 0.8, -0.2},
            {-4.0, 1.1, -0.3}, {-4.8, 1.5, -0.4}
        };
        private static final double[][] RIGHT_WING = {
            {1.0, 0.3, 0.0}, {2.0, 0.5, -0.1}, {3.0, 0.8, -0.2},
            {4.0, 1.1, -0.3}, {4.8, 1.5, -0.4}
        };

        public HellfirePhoenix(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_phoenix", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(230);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone().add(0, 5, 0); // Spawn in air
            World w = center.getWorld();
            if (w == null) return;

            Location bodyCenter = this.center.clone();

            // Body — 3 magma blocks (central fuselage)
            for (int i = 0; i < 3; i++) {
                Location loc = bodyCenter.clone().add(0, 0, -i * 0.8);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                float sc = i == 0 ? 0.9f : (i == 1 ? 1.1f : 0.8f);
                b.scale(sc, 0.6f, 0.8f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(b.entity());
                body.add(b);
            }

            // Left wing — 5 shroomlight blocks
            for (double[] off : LEFT_WING) {
                Location loc = bodyCenter.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle wb = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                wb.scale(0.9f, 0.3f, 0.7f).glow(240, 80, 30).interpolation(4, 0);
                spawnedEntities.add(wb.entity());
                leftWing.add(wb);
            }

            // Right wing — 5 shroomlight blocks
            for (double[] off : RIGHT_WING) {
                Location loc = bodyCenter.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle wb = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                wb.scale(0.9f, 0.3f, 0.7f).glow(240, 80, 30).interpolation(4, 0);
                spawnedEntities.add(wb.entity());
                rightWing.add(wb);
            }

            // Tail — 2 netherrack blocks
            for (int i = 0; i < 2; i++) {
                Location loc = bodyCenter.clone().add(0, 0.1 * i, -(2.4 + i * 0.9));
                BlockDisplayHandle t = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                t.scale(0.5f - i * 0.1f, 0.3f, 0.7f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(t.entity());
                tail.add(t);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            flyAngle += 0.05;

            // Circular flight path
            double flyX = center.getX() + Math.cos(flyAngle) * 8.0;
            double flyZ = center.getZ() + Math.sin(flyAngle) * 8.0;
            double flyY = center.getY() + Math.sin(flyAngle * 2) * 1.5;

            Location bodyCenter = new Location(c.getWorld(), flyX, flyY, flyZ);
            bodyCenter.setYaw(0);
            bodyCenter.setPitch(0);
            setCenter(bodyCenter);

            // Move body
            for (int i = 0; i < body.size(); i++) {
                Location bLoc = bodyCenter.clone().add(0, 0, -i * 0.8);
                body.get(i).entity().teleport(bLoc);
            }

            // Wing flap animation — sinusoidal Y offset
            double flapAngle = Math.sin(tick * 0.2) * 1.5; // Flap amplitude

            for (int i = 0; i < leftWing.size(); i++) {
                double baseY = LEFT_WING[i][1];
                double flapOffset = flapAngle * (i + 1) * 0.3; // More flap at wingtip
                Location wLoc = bodyCenter.clone().add(
                    LEFT_WING[i][0], baseY + flapOffset, LEFT_WING[i][2]);
                leftWing.get(i).entity().teleport(wLoc);
            }

            for (int i = 0; i < rightWing.size(); i++) {
                double baseY = RIGHT_WING[i][1];
                double flapOffset = flapAngle * (i + 1) * 0.3;
                Location wLoc = bodyCenter.clone().add(
                    RIGHT_WING[i][0], baseY + flapOffset, RIGHT_WING[i][2]);
                rightWing.get(i).entity().teleport(wLoc);
            }

            // Tail sway
            for (int i = 0; i < tail.size(); i++) {
                double sway = Math.sin(tick * 0.1 + i * 0.5) * 0.4;
                Location tLoc = bodyCenter.clone().add(sway, 0.1 * i, -(2.4 + i * 0.9));
                tail.get(i).entity().teleport(tLoc);
            }

            // Fire trail particles
            if (tick % 2 == 0) {
                bodyCenter.getWorld().spawnParticle(Particle.FLAME,
                    bodyCenter.clone().add(0, -0.5, -1.5), 8, 0.6, 0.2, 0.6, 0.02);
                DisplayBuilder.dustParticles(bodyCenter, 3, 0.5, 255, 100, 20, 1.5f);
            }

            // Screech
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(bodyCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfirePhoenix(plugin); }
    }

    // ================================================================
    // 22. NETHER SERPENT — 16 netherrack blocks in a long snaking S-curve.
    //     Head tracks nearest player. Body undulates with phase-offset sine.
    //     16 body (NETHERRACK) + head implicit in first segment = 16 blocks
    // ================================================================
    public static class NetherSerpent extends BlockDisplayAttack {
        private Location origin;
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private double pathProgress = 0;
        private double headX, headZ;

        public NetherSerpent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_serpent", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            this.headX = center.getX();
            this.headZ = center.getZ();
            World w = center.getWorld();
            if (w == null) return;

            // 16 segments forming an S-curve
            for (int i = 0; i < 16; i++) {
                double t = i * 0.8;
                double sx = Math.sin(t * 0.5) * 3.0;
                double sz = -i * 0.7;
                Location segLoc = center.clone().add(sx, 0.3, sz);

                Material mat = (i == 0) ? Material.RED_NETHER_BRICKS : Material.NETHERRACK;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, mat);
                float scale = (i == 0) ? 1.5f : (1.2f - i * 0.04f);
                seg.scale(scale, 0.7f, 0.8f)
                   .glow(i == 0 ? 255 : 200, i == 0 ? 100 : 50, i == 0 ? 20 : 10)
                   .interpolation(3, 0);
                spawnedEntities.add(seg.entity());
                segments.add(seg);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            pathProgress += 0.06;

            // Head tracks nearest player
            Player nearest = findNearestPlayer(origin, 25);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - headX;
                double dz = nearest.getLocation().getZ() - headZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.5) {
                    headX += (dx / dist) * 0.2;
                    headZ += (dz / dist) * 0.2;
                }
            } else {
                // Default wander
                headX = origin.getX() + Math.cos(pathProgress) * 4.0;
                headZ = origin.getZ() + Math.sin(pathProgress) * 4.0;
            }

            // Update head
            Location headLoc = new Location(c.getWorld(), headX, origin.getY() + 0.4, headZ);
            headLoc.setYaw(0);
            headLoc.setPitch(0);
            segments.get(0).entity().teleport(headLoc);
            setCenter(headLoc);

            // Body undulates with phase-offset sine waves
            double prevX = headX;
            double prevZ = headZ;
            for (int i = 1; i < segments.size(); i++) {
                double phase = pathProgress - i * 0.4;
                // S-curve undulation with lateral sine
                double lateralWave = Math.sin(tick * 0.12 + i * 0.8) * 1.2;
                double forwardWave = Math.cos(tick * 0.08 + i * 0.5) * 0.5;
                double yBob = Math.sin(tick * 0.1 + i * 0.6) * 0.2;

                // Each segment trails behind the previous
                double segX = prevX - Math.cos(phase) * 0.7 + lateralWave * 0.3;
                double segZ = prevZ - 0.7 + forwardWave * 0.2;

                Location segLoc = new Location(c.getWorld(), segX, origin.getY() + 0.3 + yBob, segZ);
                segLoc.setYaw(0);
                segLoc.setPitch(0);
                segments.get(i).entity().teleport(segLoc);

                prevX = segX;
                prevZ = segZ;
            }

            // Lava drip particles along body
            if (tick % 5 == 0) {
                for (int i = 0; i < segments.size(); i += 4) {
                    Location segLoc = segments.get(i).entity().getLocation();
                    segLoc.getWorld().spawnParticle(Particle.LAVA, segLoc, 2, 0.3, 0.1, 0.3, 0.01);
                }
            }

            // Hiss
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(headLoc, Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new NetherSerpent(plugin); }
    }

    // ================================================================
    // AREA DENIAL ATTACKS (23-27)
    // ================================================================

    // ================================================================
    // 23. LAVA GEYSER FIELD — 10 small magma columns (2 blocks each)
    //     scattered in 10x10 area. Each erupts upward periodically
    //     (scale Y pulse). 10 columns x 2 blocks = 20 blocks total.
    // ================================================================
    public static class LavaGeyserField extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bases = new ArrayList<>();
        private final List<BlockDisplayHandle> tops = new ArrayList<>();
        private final double[][] columnPositions = new double[10][2];
        private final int[] eruptTimers = new int[10];

        public LavaGeyserField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_geyser_field", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(15.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Scatter 10 columns in a 10x10 area
            for (int i = 0; i < 10; i++) {
                double ox = (Math.random() - 0.5) * 10.0;
                double oz = (Math.random() - 0.5) * 10.0;
                columnPositions[i][0] = ox;
                columnPositions[i][1] = oz;
                eruptTimers[i] = (int) (Math.random() * 60); // Staggered eruption start

                // Base block
                Location baseLoc = center.clone().add(ox, 0, oz);
                BlockDisplayHandle base = displayBuilder.spawnBlock(baseLoc, Material.MAGMA_BLOCK);
                base.scale(0.8f, 0.8f, 0.8f).glow(240, 80, 30).interpolation(5, 0);
                spawnedEntities.add(base.entity());
                bases.add(base);

                // Top block (erupts upward)
                Location topLoc = center.clone().add(ox, 0.8, oz);
                BlockDisplayHandle top = displayBuilder.spawnBlock(topLoc, Material.SHROOMLIGHT);
                top.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 20).interpolation(5, 0);
                spawnedEntities.add(top.entity());
                tops.add(top);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 10; i++) {
                eruptTimers[i]++;
                int cycle = eruptTimers[i] % 80; // 80-tick eruption cycle

                // Eruption: scale Y pulse from 0.6 to 3.0 and back
                double eruptHeight;
                if (cycle < 15) {
                    // Rising
                    eruptHeight = 0.6 + (cycle / 15.0) * 2.4;
                } else if (cycle < 30) {
                    // Peak
                    eruptHeight = 3.0;
                } else if (cycle < 45) {
                    // Falling
                    eruptHeight = 3.0 - ((cycle - 30) / 15.0) * 2.4;
                } else {
                    // Dormant
                    eruptHeight = 0.6;
                }

                // Animate top block scale
                Transformation t = tops.get(i).entity().getTransformation();
                tops.get(i).entity().setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.6f, (float) eruptHeight, 0.6f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));

                // Particles during eruption
                if (cycle < 45 && tick % 3 == 0) {
                    Location colLoc = c.clone().add(
                        columnPositions[i][0], eruptHeight, columnPositions[i][1]);
                    c.getWorld().spawnParticle(Particle.LAVA, colLoc, 3, 0.2, 0.5, 0.2, 0.02);
                    c.getWorld().spawnParticle(Particle.FLAME, colLoc, 2, 0.1, 0.3, 0.1, 0.01);
                }

                // Sound on eruption start
                if (cycle == 1) {
                    Location colLoc = c.clone().add(
                        columnPositions[i][0], 0, columnPositions[i][1]);
                    DisplayBuilder.playSound(colLoc, Sound.BLOCK_LAVA_POP, 0.6f, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaGeyserField(plugin); }
    }

    // ================================================================
    // 24. BRIMSTONE MINEFIELD — 12 flat magma blocks at ground level,
    //     slightly buried. When player within 3 blocks, mine "detonates"
    //     (scale burst + particles). Impact damage per mine.
    //     12 mines (MAGMA_BLOCK) = 12 blocks
    // ================================================================
    public static class BrimstoneMinefield extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mines = new ArrayList<>();
        private final double[][] minePositions = new double[12][2];
        private final boolean[] detonated = new boolean[12];

        public BrimstoneMinefield(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_minefield", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0); // No continuous damage — impact only
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Scatter 12 mines in a 12x12 area
            for (int i = 0; i < 12; i++) {
                double ox = (Math.random() - 0.5) * 12.0;
                double oz = (Math.random() - 0.5) * 12.0;
                minePositions[i][0] = ox;
                minePositions[i][1] = oz;
                detonated[i] = false;

                // Flat mine block, slightly embedded
                Location mineLoc = center.clone().add(ox, -0.3, oz);
                BlockDisplayHandle mine = displayBuilder.spawnBlock(mineLoc, Material.MAGMA_BLOCK);
                mine.scale(0.9f, 0.15f, 0.9f).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(mine.entity());
                mines.add(mine);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Check each mine for nearby players
            for (int i = 0; i < 12; i++) {
                if (detonated[i]) continue;

                Location mineLoc = c.clone().add(minePositions[i][0], 0, minePositions[i][1]);

                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(mineLoc) <= 9.0) { // 3-block trigger radius
                        // DETONATE
                        detonated[i] = true;

                        // Scale burst animation
                        Transformation t = mines.get(i).entity().getTransformation();
                        mines.get(i).entity().setInterpolationDuration(5);
                        mines.get(i).entity().setInterpolationDelay(0);
                        mines.get(i).entity().setTransformation(new Transformation(
                            new Vector3f(-1.5f, -0.5f, -1.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(3.0f, 2.0f, 3.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));

                        // Explosion particles
                        w.spawnParticle(Particle.LAVA, mineLoc.clone().add(0, 1, 0),
                            30, 1.5, 1.0, 1.5, 0.1);
                        w.spawnParticle(Particle.FLAME, mineLoc.clone().add(0, 0.5, 0),
                            20, 1.0, 0.5, 1.0, 0.05);
                        w.spawnParticle(Particle.LARGE_SMOKE, mineLoc.clone().add(0, 1.5, 0),
                            15, 1.0, 1.0, 1.0, 0.03);

                        // Impact damage
                        triggerImpactDamage(mineLoc);

                        // Explosion sound
                        DisplayBuilder.playSound(mineLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                        break;
                    }
                }

                // Subtle glow pulse on armed mines
                if (!detonated[i] && tick % 20 < 10) {
                    mines.get(i).glow(255, 60, 10);
                } else if (!detonated[i]) {
                    mines.get(i).glow(200, 50, 10);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneMinefield(plugin); }
    }

    // ================================================================
    // 25. HELLFIRE BARRICADE — Two parallel walls of 12 blocks each
    //     (24 total) forming a corridor. Walls slowly close inward.
    //     Players caught between = crushed.
    //     24 blocks: 12 NETHER_BRICKS (left wall) + 12 RED_NETHER_BRICKS (right wall)
    // ================================================================
    public static class HellfireBarricade extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftWall = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWall = new ArrayList<>();
        private double wallSeparation = 8.0; // Starting gap between walls

        public HellfireBarricade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_barricade", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(3.0); // Narrow kill zone between walls
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left wall: 12 blocks (4 wide x 3 tall)
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(-wallSeparation / 2, y * 1.3, (x - 1.5) * 1.3);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                    block.scale(1.2f, 1.3f, 1.3f).glow(200, 50, 10).interpolation(5, 0);
                    spawnedEntities.add(block.entity());
                    leftWall.add(block);
                }
            }

            // Right wall: 12 blocks (4 wide x 3 tall)
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(wallSeparation / 2, y * 1.3, (x - 1.5) * 1.3);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                    block.scale(1.2f, 1.3f, 1.3f).glow(240, 80, 30).interpolation(5, 0);
                    spawnedEntities.add(block.entity());
                    rightWall.add(block);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Walls close inward over time
            if (wallSeparation > 1.0) {
                wallSeparation -= 0.03; // Slow close
            }

            // Update left wall positions
            int idx = 0;
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 3; y++) {
                    Location loc = c.clone().add(-wallSeparation / 2, y * 1.3, (x - 1.5) * 1.3);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    leftWall.get(idx).entity().teleport(loc);
                    idx++;
                }
            }

            // Update right wall positions
            idx = 0;
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 3; y++) {
                    Location loc = c.clone().add(wallSeparation / 2, y * 1.3, (x - 1.5) * 1.3);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    rightWall.get(idx).entity().teleport(loc);
                    idx++;
                }
            }

            // Grinding particles between walls
            if (tick % 5 == 0 && wallSeparation < 4.0) {
                c.getWorld().spawnParticle(Particle.FLAME,
                    c.clone().add(0, 1.5, 0), 8, wallSeparation / 3, 1.0, 1.5, 0.02);
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 5, 1.0, 255, 100, 20, 1.5f);
            }

            // Grinding sound as walls close
            if (tick % 25 == 0 && wallSeparation < 5.0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 0.3f);
            }

            // Slam sound when nearly closed
            if (wallSeparation <= 1.1 && wallSeparation > 0.95) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireBarricade(plugin); }
    }

    // ================================================================
    // 26. MAGMA FLOWER PATCH — 10 "flowers" each made of 3 blocks
    //     (1 stem + 2 petal blocks). Petals slowly open/close.
    //     When open, damage aura active.
    //     10 flowers x 3 blocks = 30 blocks total
    //     Stems: BASALT, Petals: MAGMA_BLOCK
    // ================================================================
    public static class MagmaFlowerPatch extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stems = new ArrayList<>();
        private final List<BlockDisplayHandle> petalsLeft = new ArrayList<>();
        private final List<BlockDisplayHandle> petalsRight = new ArrayList<>();
        private final double[][] flowerPositions = new double[10][2];

        public MagmaFlowerPatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_flower_patch", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                double ox = (Math.random() - 0.5) * 12.0;
                double oz = (Math.random() - 0.5) * 12.0;
                flowerPositions[i][0] = ox;
                flowerPositions[i][1] = oz;

                // Stem — tall basalt
                Location stemLoc = center.clone().add(ox, 0, oz);
                BlockDisplayHandle stem = displayBuilder.spawnBlock(stemLoc, Material.BASALT);
                stem.scale(0.3f, 1.5f, 0.3f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(stem.entity());
                stems.add(stem);

                // Left petal
                Location leftLoc = center.clone().add(ox - 0.5, 1.5, oz);
                BlockDisplayHandle leftPetal = displayBuilder.spawnBlock(leftLoc, Material.MAGMA_BLOCK);
                leftPetal.scale(0.6f, 0.3f, 0.6f).glow(255, 100, 20).interpolation(8, 0);
                spawnedEntities.add(leftPetal.entity());
                petalsLeft.add(leftPetal);

                // Right petal
                Location rightLoc = center.clone().add(ox + 0.5, 1.5, oz);
                BlockDisplayHandle rightPetal = displayBuilder.spawnBlock(rightLoc, Material.MAGMA_BLOCK);
                rightPetal.scale(0.6f, 0.3f, 0.6f).glow(255, 100, 20).interpolation(8, 0);
                spawnedEntities.add(rightPetal.entity());
                petalsRight.add(rightPetal);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 10; i++) {
                // Each flower has its own phase for open/close cycle
                double phase = tick * 0.04 + i * 0.6;
                double openAmount = (Math.sin(phase) + 1.0) / 2.0; // 0 (closed) to 1 (open)

                // Petal spread — when open, petals move outward
                double spreadX = openAmount * 0.8;
                double petalY = 1.5 + openAmount * 0.3; // Slight rise when open

                Location leftLoc = c.clone().add(
                    flowerPositions[i][0] - 0.2 - spreadX, petalY, flowerPositions[i][1]);
                leftLoc.setYaw(0);
                leftLoc.setPitch(0);
                petalsLeft.get(i).entity().teleport(leftLoc);

                Location rightLoc = c.clone().add(
                    flowerPositions[i][0] + 0.2 + spreadX, petalY, flowerPositions[i][1]);
                rightLoc.setYaw(0);
                rightLoc.setPitch(0);
                petalsRight.get(i).entity().teleport(rightLoc);

                // Petal scale animation — wider when open
                float petalScaleX = 0.4f + (float) openAmount * 0.5f;
                Transformation tl = petalsLeft.get(i).entity().getTransformation();
                petalsLeft.get(i).entity().setTransformation(new Transformation(
                    tl.getTranslation(), new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(petalScaleX, 0.3f, 0.6f), new AxisAngle4f(0, 0, 1, 0)
                ));
                Transformation tr = petalsRight.get(i).entity().getTransformation();
                petalsRight.get(i).entity().setTransformation(new Transformation(
                    tr.getTranslation(), new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(petalScaleX, 0.3f, 0.6f), new AxisAngle4f(0, 0, 1, 0)
                ));

                // Glow brighter when open
                if (openAmount > 0.7) {
                    petalsLeft.get(i).glow(255, 150, 50);
                    petalsRight.get(i).glow(255, 150, 50);
                } else {
                    petalsLeft.get(i).glow(200, 50, 10);
                    petalsRight.get(i).glow(200, 50, 10);
                }

                // Fire particles when open
                if (openAmount > 0.6 && tick % 6 == 0) {
                    Location flowerTop = c.clone().add(
                        flowerPositions[i][0], 2.0, flowerPositions[i][1]);
                    c.getWorld().spawnParticle(Particle.FLAME, flowerTop, 3, 0.3, 0.2, 0.3, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaFlowerPatch(plugin); }
    }

    // ================================================================
    // 27. INFERNAL CHESSBOARD — 16 alternating magma/blackstone blocks
    //     in a 4x4 grid at ground level. Random squares "activate" every
    //     40 ticks (scale up + glow). Active squares deal damage.
    //     8 MAGMA_BLOCK + 8 BLACKSTONE = 16 blocks
    // ================================================================
    public static class InfernalChessboard extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tiles = new ArrayList<>();
        private final boolean[] active = new boolean[16];
        private final int[] activeCooldowns = new int[16];

        public InfernalChessboard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_chessboard", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4x4 grid — alternating materials
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    int idx = row * 4 + col;
                    boolean isMagma = (row + col) % 2 == 0;
                    Material mat = isMagma ? Material.MAGMA_BLOCK : Material.BLACKSTONE;

                    double ox = (col - 1.5) * 1.5;
                    double oz = (row - 1.5) * 1.5;

                    Location tileLoc = center.clone().add(ox, -0.2, oz);
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, mat);
                    tile.scale(1.4f, 0.2f, 1.4f)
                        .glow(isMagma ? 200 : 20, isMagma ? 50 : 10, isMagma ? 10 : 5)
                        .interpolation(5, 0);
                    spawnedEntities.add(tile.entity());
                    tiles.add(tile);

                    active[idx] = false;
                    activeCooldowns[idx] = (int) (Math.random() * 40);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 16; i++) {
                activeCooldowns[i]--;

                if (activeCooldowns[i] <= 0 && !active[i]) {
                    // Activate this tile
                    active[i] = true;
                    activeCooldowns[i] = 30; // Active for 30 ticks

                    // Scale up animation
                    Transformation t = tiles.get(i).entity().getTransformation();
                    tiles.get(i).entity().setInterpolationDuration(5);
                    tiles.get(i).entity().setInterpolationDelay(0);
                    tiles.get(i).entity().setTransformation(new Transformation(
                        t.getTranslation(), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.4f, 2.0f, 1.4f), new AxisAngle4f(0, 0, 1, 0)
                    ));
                    tiles.get(i).glow(255, 150, 30);

                    // Activation sound
                    int row = i / 4;
                    int col = i % 4;
                    Location tileLoc = c.clone().add((col - 1.5) * 1.5, 0, (row - 1.5) * 1.5);
                    DisplayBuilder.playSound(tileLoc, Sound.BLOCK_LAVA_POP, 0.6f, 0.8f);

                } else if (active[i]) {
                    if (activeCooldowns[i] <= 0) {
                        // Deactivate
                        active[i] = false;
                        activeCooldowns[i] = 20 + (int) (Math.random() * 40); // Random next activation

                        // Scale back down
                        Transformation t = tiles.get(i).entity().getTransformation();
                        tiles.get(i).entity().setInterpolationDuration(5);
                        tiles.get(i).entity().setInterpolationDelay(0);
                        tiles.get(i).entity().setTransformation(new Transformation(
                            t.getTranslation(), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.4f, 0.2f, 1.4f), new AxisAngle4f(0, 0, 1, 0)
                        ));

                        boolean isMagma = ((i / 4) + (i % 4)) % 2 == 0;
                        tiles.get(i).glow(isMagma ? 200 : 20, isMagma ? 50 : 10, isMagma ? 10 : 5);
                    }

                    // Damage players on active tiles
                    int row = i / 4;
                    int col = i % 4;
                    Location tileLoc = c.clone().add((col - 1.5) * 1.5, 0.5, (row - 1.5) * 1.5);

                    // Fire particles on active tiles
                    if (tick % 4 == 0) {
                        c.getWorld().spawnParticle(Particle.FLAME, tileLoc, 5, 0.5, 0.5, 0.5, 0.02);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalChessboard(plugin); }
    }

    // ================================================================
    // COMPLEX GEOMETRY ATTACKS (28-31)
    // ================================================================

    // ================================================================
    // 28. DOOM TRIANGLE — 12 blocks in a perfect equilateral triangle
    //     (4 per side), standing upright. Triangle rotates on Y axis.
    //     12 blocks: 4 MAGMA_BLOCK + 4 NETHER_BRICKS + 4 RED_NETHER_BRICKS
    // ================================================================
    public static class DoomTriangle extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> triangleBlocks = new ArrayList<>();
        private final double[][] blockPositions = new double[12][3];

        public DoomTriangle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_triangle", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Equilateral triangle vertices (standing upright in XY plane)
            // Side length ~6 blocks. 4 blocks per side.
            double sideLen = 6.0;
            double height = sideLen * Math.sqrt(3) / 2;

            // Vertex positions (XY plane, centered)
            double[][] vertices = {
                {-sideLen / 2, 0, 0},               // Bottom left
                {sideLen / 2, 0, 0},                 // Bottom right
                {0, height, 0}                        // Top
            };

            Material[] sideMats = {Material.MAGMA_BLOCK, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS};
            int blockIdx = 0;

            // Generate 4 blocks per side (interpolating between vertices)
            for (int side = 0; side < 3; side++) {
                double[] start = vertices[side];
                double[] end = vertices[(side + 1) % 3];
                for (int seg = 0; seg < 4; seg++) {
                    double t = seg / 4.0;
                    double bx = start[0] + (end[0] - start[0]) * t;
                    double by = start[1] + (end[1] - start[1]) * t;
                    double bz = start[2] + (end[2] - start[2]) * t;

                    blockPositions[blockIdx][0] = bx;
                    blockPositions[blockIdx][1] = by + 1.0; // Raise above ground
                    blockPositions[blockIdx][2] = bz;

                    Location loc = center.clone().add(bx, by + 1.0, bz);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, sideMats[side]);
                    block.scale(0.8f, 0.8f, 0.8f).glow(255, 100, 20).interpolation(3, 0);
                    spawnedEntities.add(block.entity());
                    triangleBlocks.add(block);
                    blockIdx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double rotAngle = tick * 0.04; // Y-axis rotation speed

            for (int i = 0; i < triangleBlocks.size(); i++) {
                double origX = blockPositions[i][0];
                double origY = blockPositions[i][1];
                double origZ = blockPositions[i][2];

                // Rotate around Y axis
                double cosA = Math.cos(rotAngle);
                double sinA = Math.sin(rotAngle);
                double newX = origX * cosA - origZ * sinA;
                double newZ = origX * sinA + origZ * cosA;

                Location newLoc = c.clone().add(newX, origY, newZ);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                triangleBlocks.get(i).entity().teleport(newLoc);
            }

            // Inner glow particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME,
                    c.clone().add(0, 2.5, 0), 6, 1.5, 1.5, 1.5, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 4, 1.0, 255, 100, 20, 1.2f);
            }

            // Hum
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomTriangle(plugin); }
    }

    // ================================================================
    // 29. INFERNAL HELIX — 10 blocks in a double helix (DNA-like),
    //     two strands spiraling around Y axis. Entire helix rotates.
    //     5 CRYING_OBSIDIAN (strand 1) + 5 GLOWSTONE (strand 2) = 10 blocks
    // ================================================================
    public static class InfernalHelix extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> strand1 = new ArrayList<>();
        private final List<BlockDisplayHandle> strand2 = new ArrayList<>();
        private final double[][] strand1Pos = new double[5][3];
        private final double[][] strand2Pos = new double[5][3];

        public InfernalHelix(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_helix", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(230);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double helixRadius = 2.0;
            double helixHeight = 8.0;

            for (int i = 0; i < 5; i++) {
                double t = (double) i / 5;
                double angle = t * Math.PI * 2;
                double y = t * helixHeight;

                // Strand 1
                double s1x = Math.cos(angle) * helixRadius;
                double s1z = Math.sin(angle) * helixRadius;
                strand1Pos[i] = new double[]{s1x, y + 0.5, s1z};

                Location loc1 = center.clone().add(s1x, y + 0.5, s1z);
                BlockDisplayHandle b1 = displayBuilder.spawnBlock(loc1, Material.CRYING_OBSIDIAN);
                b1.scale(0.7f, 0.7f, 0.7f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(b1.entity());
                strand1.add(b1);

                // Strand 2 — offset by PI (opposite side)
                double s2x = Math.cos(angle + Math.PI) * helixRadius;
                double s2z = Math.sin(angle + Math.PI) * helixRadius;
                strand2Pos[i] = new double[]{s2x, y + 0.5, s2z};

                Location loc2 = center.clone().add(s2x, y + 0.5, s2z);
                BlockDisplayHandle b2 = displayBuilder.spawnBlock(loc2, Material.GLOWSTONE);
                b2.scale(0.7f, 0.7f, 0.7f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(b2.entity());
                strand2.add(b2);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double rotAngle = tick * 0.06; // Rotation speed

            for (int i = 0; i < 5; i++) {
                // Strand 1 — rotate around Y
                double origX1 = strand1Pos[i][0];
                double origY1 = strand1Pos[i][1];
                double origZ1 = strand1Pos[i][2];
                double cosA = Math.cos(rotAngle);
                double sinA = Math.sin(rotAngle);
                double newX1 = origX1 * cosA - origZ1 * sinA;
                double newZ1 = origX1 * sinA + origZ1 * cosA;
                // Slight vertical oscillation
                double yOff1 = Math.sin(tick * 0.08 + i * 0.8) * 0.3;

                Location loc1 = c.clone().add(newX1, origY1 + yOff1, newZ1);
                loc1.setYaw(0);
                loc1.setPitch(0);
                strand1.get(i).entity().teleport(loc1);

                // Strand 2 — same rotation
                double origX2 = strand2Pos[i][0];
                double origY2 = strand2Pos[i][1];
                double origZ2 = strand2Pos[i][2];
                double newX2 = origX2 * cosA - origZ2 * sinA;
                double newZ2 = origX2 * sinA + origZ2 * cosA;
                double yOff2 = Math.sin(tick * 0.08 + i * 0.8 + Math.PI) * 0.3;

                Location loc2 = c.clone().add(newX2, origY2 + yOff2, newZ2);
                loc2.setYaw(0);
                loc2.setPitch(0);
                strand2.get(i).entity().teleport(loc2);
            }

            // Spiral particles between strands
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME,
                    c.clone().add(0, 4, 0), 6, 1.5, 3.0, 1.5, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 3, 1.5, 240, 80, 30, 1.0f);
            }

            // Hum
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalHelix(plugin); }
    }

    // ================================================================
    // 30. MAGMA STARGATE — 10 blocks in a flat circle + 5 blocks forming
    //     a star pattern inside. Star rotates independently of circle.
    //     10 ring (OBSIDIAN) + 5 star (MAGMA_BLOCK) = 15 blocks
    // ================================================================
    public static class MagmaStargate extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();
        private final double[][] ringPositions = new double[10][3];
        private final double[][] starPositions = new double[5][3];

        public MagmaStargate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_stargate", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(20.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location gateCenter = center.clone().add(0, 3, 0); // Floating

            // Ring — 10 obsidian blocks in a circle (radius 3)
            double ringRadius = 3.0;
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double rx = Math.cos(angle) * ringRadius;
                double ry = Math.sin(angle) * ringRadius;
                ringPositions[i] = new double[]{rx, ry, 0};

                Location loc = gateCenter.clone().add(rx, ry, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                block.scale(0.9f, 0.9f, 0.5f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(block.entity());
                ringBlocks.add(block);
            }

            // Star — 5 magma blocks forming a pentagram inside (radius 1.8)
            double starRadius = 1.8;
            for (int i = 0; i < 5; i++) {
                // Pentagram: connect every other vertex
                double angle = (2 * Math.PI * i) / 5 - Math.PI / 2;
                double sx = Math.cos(angle) * starRadius;
                double sy = Math.sin(angle) * starRadius;
                starPositions[i] = new double[]{sx, sy, 0};

                Location loc = gateCenter.clone().add(sx, sy, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                block.scale(0.7f, 0.7f, 0.7f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(block.entity());
                starBlocks.add(block);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location gateCenter = c.clone().add(0, 3, 0);

            // Ring rotates slowly on Z axis (gate face spins)
            double ringRot = tick * 0.02;

            for (int i = 0; i < ringBlocks.size(); i++) {
                double origX = ringPositions[i][0];
                double origY = ringPositions[i][1];
                double cosR = Math.cos(ringRot);
                double sinR = Math.sin(ringRot);
                double newX = origX * cosR - origY * sinR;
                double newY = origX * sinR + origY * cosR;

                Location loc = gateCenter.clone().add(newX, newY, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                ringBlocks.get(i).entity().teleport(loc);
            }

            // Star rotates faster in the opposite direction
            double starRot = -tick * 0.05;

            for (int i = 0; i < starBlocks.size(); i++) {
                double origX = starPositions[i][0];
                double origY = starPositions[i][1];
                double cosR = Math.cos(starRot);
                double sinR = Math.sin(starRot);
                double newX = origX * cosR - origY * sinR;
                double newY = origX * sinR + origY * cosR;

                Location loc = gateCenter.clone().add(newX, newY, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                starBlocks.get(i).entity().teleport(loc);
            }

            // Portal particles in center
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME,
                    gateCenter, 5, 0.8, 0.8, 0.2, 0.01);
                DisplayBuilder.dustParticles(gateCenter, 4, 1.0, 240, 80, 30, 1.5f);
            }

            // Portal hum
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaStargate(plugin); }
    }

    // ================================================================
    // 31. HELLFIRE CROWN — 10 spikes of varying heights in a crown shape.
    //     Hovers at Y+5, rotating slowly. Each spike wobbles independently.
    //     10 spike blocks: 5 MAGMA_BLOCK + 5 SHROOMLIGHT
    // ================================================================
    public static class HellfireCrown extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final double[] spikeHeights = new double[10];
        private final double[] spikePhases = new double[10];

        public HellfireCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_crown", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double crownRadius = 2.5;
            Location crownCenter = center.clone().add(0, 5, 0);

            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double sx = Math.cos(angle) * crownRadius;
                double sz = Math.sin(angle) * crownRadius;

                // Alternating spike heights for crown shape
                spikeHeights[i] = (i % 2 == 0) ? 2.5 : 1.5;
                spikePhases[i] = Math.random() * Math.PI * 2; // Random wobble phase

                Material mat = (i % 2 == 0) ? Material.MAGMA_BLOCK : Material.SHROOMLIGHT;
                Location loc = crownCenter.clone().add(sx, 0, sz);
                BlockDisplayHandle spike = displayBuilder.spawnBlock(loc, mat);
                spike.scale(0.6f, (float) spikeHeights[i], 0.6f)
                     .glow(255, 100, 20)
                     .interpolation(4, 0);
                spawnedEntities.add(spike.entity());
                spikes.add(spike);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location crownCenter = c.clone().add(0, 5, 0);
            double crownRadius = 2.5;
            double rotAngle = tick * 0.03; // Slow rotation

            for (int i = 0; i < spikes.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 10 + rotAngle;
                double sx = Math.cos(baseAngle) * crownRadius;
                double sz = Math.sin(baseAngle) * crownRadius;

                // Independent wobble per spike
                double wobbleX = Math.sin(tick * 0.1 + spikePhases[i]) * 0.15;
                double wobbleZ = Math.cos(tick * 0.12 + spikePhases[i]) * 0.15;

                // Slight height pulse
                double heightPulse = Math.sin(tick * 0.08 + spikePhases[i]) * 0.3;

                Location spikeLoc = crownCenter.clone().add(sx + wobbleX, heightPulse, sz + wobbleZ);
                spikeLoc.setYaw(0);
                spikeLoc.setPitch(0);
                spikes.get(i).entity().teleport(spikeLoc);

                // Animate spike scale (height pulse)
                float currentHeight = (float) (spikeHeights[i] + heightPulse * 0.5);
                Transformation t = spikes.get(i).entity().getTransformation();
                spikes.get(i).entity().setTransformation(new Transformation(
                    t.getTranslation(), new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.6f, currentHeight, 0.6f), new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Particles below the crown (damage zone indicator)
            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(
                    crownCenter.clone().add(0, -3, 0), 8, 2.5, 255, 100, 20, 1.5f);
                c.getWorld().spawnParticle(Particle.FLAME,
                    crownCenter.clone().add(0, -1, 0), 4, 2.0, 1.5, 2.0, 0.01);
            }

            // Menacing hum
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireCrown(plugin); }
    }

    // ================================================================
    // EXPLOSIVE/BURST ATTACKS (32-34)
    // ================================================================

    // ================================================================
    // 32. INFERNAL SUPERNOVA — 16 blocks start at center, then blast
    //     outward in all directions. Each block travels 5 blocks out.
    //     Impact damage on expansion.
    //     8 MAGMA_BLOCK + 4 SHROOMLIGHT + 4 GLOWSTONE = 16 blocks
    // ================================================================
    public static class InfernalSupernova extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private final double[][] directions = new double[16][3];
        private boolean exploded = false;
        private int chargeTime = 40; // Ticks before explosion

        public InfernalSupernova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_supernova", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0); // No continuous — impact only
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(45.0);
            config.setImpactRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location core = center.clone().add(0, 2, 0);

            // Distribute 16 blocks in a sphere at center (initially clustered)
            Material[] mats = new Material[16];
            for (int i = 0; i < 8; i++) mats[i] = Material.MAGMA_BLOCK;
            for (int i = 8; i < 12; i++) mats[i] = Material.SHROOMLIGHT;
            for (int i = 12; i < 16; i++) mats[i] = Material.GLOWSTONE;

            // Pre-calculate outward directions (fibonacci sphere distribution)
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 16; i++) {
                double y = 1 - (2.0 * i / 15.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                directions[i][0] = Math.cos(theta) * radiusAtY;
                directions[i][1] = y;
                directions[i][2] = Math.sin(theta) * radiusAtY;

                // Spawn clustered at core
                Location loc = core.clone().add(
                    directions[i][0] * 0.3, directions[i][1] * 0.3, directions[i][2] * 0.3);
                BlockDisplayHandle frag = displayBuilder.spawnBlock(loc, mats[i]);
                frag.scale(0.8f, 0.8f, 0.8f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(frag.entity());
                fragments.add(frag);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location core = c.clone().add(0, 2, 0);

            if (tick < chargeTime) {
                // Charging phase — blocks orbit tightly, scale pulses
                double orbSpeed = tick * 0.15;
                for (int i = 0; i < fragments.size(); i++) {
                    double angle = orbSpeed + (2 * Math.PI * i) / 16;
                    double orbitR = 0.5 + Math.sin(tick * 0.2) * 0.3;
                    double ox = Math.cos(angle) * orbitR * Math.abs(directions[i][0] + 0.5);
                    double oy = directions[i][1] * orbitR;
                    double oz = Math.sin(angle) * orbitR * Math.abs(directions[i][2] + 0.5);

                    Location loc = core.clone().add(ox, oy, oz);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    fragments.get(i).entity().teleport(loc);
                }

                // Charging particles
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(core, 10, 0.5, 255, 150, 30, 1.5f);
                    core.getWorld().spawnParticle(Particle.FLAME, core, 8, 0.3, 0.3, 0.3, 0.02);
                }

                // Charging sound escalation
                if (tick % 10 == 0) {
                    float pitch = 0.5f + (tick / (float) chargeTime) * 1.0f;
                    DisplayBuilder.playSound(core, Sound.BLOCK_BEACON_AMBIENT, 0.8f, pitch);
                }

            } else if (!exploded) {
                // EXPLODE — trigger impact damage
                exploded = true;
                triggerImpactDamage(core);

                // Explosion effects
                core.getWorld().spawnParticle(Particle.LAVA, core, 50, 2, 2, 2, 0.2);
                core.getWorld().spawnParticle(Particle.FLAME, core, 40, 3, 3, 3, 0.1);
                core.getWorld().spawnParticle(Particle.LARGE_SMOKE, core, 30, 2, 2, 2, 0.05);
                DisplayBuilder.playSound(core, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.3f);
                DisplayBuilder.playSound(core, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.4f);
            }

            if (exploded) {
                // Fragments fly outward
                int ticksSinceExplosion = tick - chargeTime;
                double expandDist = Math.min(ticksSinceExplosion * 0.3, 5.0);

                for (int i = 0; i < fragments.size(); i++) {
                    double ox = directions[i][0] * expandDist;
                    double oy = directions[i][1] * expandDist;
                    double oz = directions[i][2] * expandDist;

                    Location loc = core.clone().add(ox, oy, oz);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    fragments.get(i).entity().teleport(loc);

                    // Fade out — shrink over time
                    float fadeScale = Math.max(0.1f, 0.8f - ticksSinceExplosion * 0.01f);
                    Transformation t = fragments.get(i).entity().getTransformation();
                    fragments.get(i).entity().setTransformation(new Transformation(
                        t.getTranslation(), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(fadeScale, fadeScale, fadeScale), new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Trail particles
                if (ticksSinceExplosion % 3 == 0 && ticksSinceExplosion < 30) {
                    core.getWorld().spawnParticle(Particle.FLAME, core,
                        10, expandDist, expandDist, expandDist, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalSupernova(plugin); }
    }

    // ================================================================
    // 33. LAVA GRENADE CLUSTER — 10 small magma sphere clusters
    //     (3 blocks each) arc from spawn point and land in spread pattern.
    //     Each landing: 4-block impact radius, 25 damage.
    //     10 grenades x 3 blocks = 30 blocks
    //     Material: MAGMA_BLOCK, RED_CONCRETE, ORANGE_CONCRETE
    // ================================================================
    public static class LavaGrenadeCluster extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> grenades = new ArrayList<>();
        private final double[][] targetPositions = new double[10][2];
        private final double[] launchAngles = new double[10];
        private final boolean[] landed = new boolean[10];
        private final int[] launchDelays = new int[10]; // Staggered launches

        public LavaGrenadeCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_grenade_cluster", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(25.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location launchPoint = center.clone().add(0, 3, 0);

            for (int g = 0; g < 10; g++) {
                List<BlockDisplayHandle> cluster = new ArrayList<>();

                // Target position — spread around center
                double angle = (2 * Math.PI * g) / 10 + Math.random() * 0.5;
                double dist = 4.0 + Math.random() * 6.0;
                targetPositions[g][0] = Math.cos(angle) * dist;
                targetPositions[g][1] = Math.sin(angle) * dist;
                launchAngles[g] = angle;
                landed[g] = false;
                launchDelays[g] = g * 5; // Staggered: 5 ticks between each

                Material[] clusterMats = {Material.MAGMA_BLOCK, Material.RED_CONCRETE, Material.ORANGE_CONCRETE};

                // 3 blocks per grenade — tight cluster
                for (int b = 0; b < 3; b++) {
                    double bx = (Math.random() - 0.5) * 0.4;
                    double by = (Math.random() - 0.5) * 0.4;
                    double bz = (Math.random() - 0.5) * 0.4;
                    Location loc = launchPoint.clone().add(bx, by, bz);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, clusterMats[b]);
                    block.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 20).interpolation(2, 0);
                    spawnedEntities.add(block.entity());
                    cluster.add(block);
                }

                grenades.add(cluster);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int g = 0; g < 10; g++) {
                if (landed[g]) continue;
                if (tick < launchDelays[g]) continue; // Not yet launched

                int flightTick = tick - launchDelays[g];
                int flightDuration = 30; // Ticks to reach target

                if (flightTick >= flightDuration) {
                    // LAND — impact
                    landed[g] = true;

                    Location landLoc = c.clone().add(targetPositions[g][0], 0, targetPositions[g][1]);
                    triggerImpactDamage(landLoc);

                    // Move blocks to ground
                    for (int b = 0; b < grenades.get(g).size(); b++) {
                        double bx = (Math.random() - 0.5) * 1.5;
                        double bz = (Math.random() - 0.5) * 1.5;
                        Location loc = landLoc.clone().add(bx, 0.1, bz);
                        loc.setYaw(0);
                        loc.setPitch(0);
                        grenades.get(g).get(b).entity().teleport(loc);
                    }

                    // Impact effects
                    landLoc.getWorld().spawnParticle(Particle.LAVA, landLoc.clone().add(0, 0.5, 0),
                        20, 1.5, 0.5, 1.5, 0.1);
                    landLoc.getWorld().spawnParticle(Particle.FLAME, landLoc,
                        15, 1.0, 0.3, 1.0, 0.05);
                    DisplayBuilder.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);

                } else {
                    // In flight — parabolic arc
                    double t = (double) flightTick / flightDuration;
                    double arcX = targetPositions[g][0] * t;
                    double arcZ = targetPositions[g][1] * t;
                    // Parabolic Y: peaks at halfway
                    double arcY = 3.0 + 6.0 * (4 * t * (1 - t)); // Max height 9

                    for (int b = 0; b < grenades.get(g).size(); b++) {
                        double bx = (Math.random() - 0.5) * 0.2;
                        double by = (Math.random() - 0.5) * 0.2;
                        double bz = (Math.random() - 0.5) * 0.2;
                        Location loc = c.clone().add(arcX + bx, arcY + by, arcZ + bz);
                        loc.setYaw(0);
                        loc.setPitch(0);
                        grenades.get(g).get(b).entity().teleport(loc);
                    }

                    // Trail particles
                    if (flightTick % 3 == 0) {
                        Location trailLoc = c.clone().add(arcX, arcY, arcZ);
                        c.getWorld().spawnParticle(Particle.FLAME, trailLoc, 3, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }

            // Launch sounds for each grenade
            for (int g = 0; g < 10; g++) {
                if (tick == launchDelays[g]) {
                    DisplayBuilder.playSound(c.clone().add(0, 3, 0), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaGrenadeCluster(plugin); }
    }

    // ================================================================
    // 34. BRIMSTONE BARRAGE — 12 blocks rain down from Y+20 in rapid
    //     succession (2-tick interval between each). Each has its own
    //     landing spot. Each impact: 3-block radius, 20 damage.
    //     Combined area covered: massive.
    //     4 DEEPSLATE + 4 COAL_BLOCK + 4 BLACK_CONCRETE = 12 blocks
    // ================================================================
    public static class BrimstoneBarrage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> projectiles = new ArrayList<>();
        private final double[][] targetXZ = new double[12][2];
        private final boolean[] landed = new boolean[12];
        private final int[] launchTick = new int[12]; // When each starts falling

        public BrimstoneBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_barrage", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(180);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = new Material[12];
            for (int i = 0; i < 4; i++) mats[i] = Material.DEEPSLATE;
            for (int i = 4; i < 8; i++) mats[i] = Material.COAL_BLOCK;
            for (int i = 8; i < 12; i++) mats[i] = Material.BLACK_CONCRETE;

            for (int i = 0; i < 12; i++) {
                // Scatter landing targets across a wide area
                targetXZ[i][0] = (Math.random() - 0.5) * 16.0;
                targetXZ[i][1] = (Math.random() - 0.5) * 16.0;
                landed[i] = false;
                launchTick[i] = i * 2; // 2-tick interval between each

                // Spawn high up
                Location spawnLoc = center.clone().add(targetXZ[i][0], 20, targetXZ[i][1]);
                BlockDisplayHandle proj = displayBuilder.spawnBlock(spawnLoc, mats[i]);
                proj.scale(1.0f, 1.0f, 1.0f).glow(200, 50, 10).interpolation(2, 0);
                spawnedEntities.add(proj.entity());
                projectiles.add(proj);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 12; i++) {
                if (landed[i]) continue;
                if (tick < launchTick[i]) continue; // Not yet falling

                int fallTick = tick - launchTick[i];
                double fallSpeed = 0.8; // Blocks per tick
                double currentY = 20.0 - fallTick * fallSpeed;

                if (currentY <= 0) {
                    // IMPACT
                    landed[i] = true;
                    currentY = 0;

                    Location impactLoc = c.clone().add(targetXZ[i][0], 0, targetXZ[i][1]);
                    triggerImpactDamage(impactLoc);

                    // Impact effects
                    impactLoc.getWorld().spawnParticle(Particle.LAVA,
                        impactLoc.clone().add(0, 0.5, 0), 15, 1.0, 0.3, 1.0, 0.08);
                    impactLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        impactLoc.clone().add(0, 1, 0), 10, 0.8, 0.5, 0.8, 0.03);
                    impactLoc.getWorld().spawnParticle(Particle.FLAME,
                        impactLoc, 12, 0.8, 0.2, 0.8, 0.04);

                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);

                    // Flatten the block on impact
                    Transformation t = projectiles.get(i).entity().getTransformation();
                    projectiles.get(i).entity().setInterpolationDuration(3);
                    projectiles.get(i).entity().setInterpolationDelay(0);
                    projectiles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.75f, -0.1f, -0.75f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.5f, 0.2f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Update position
                Location projLoc = c.clone().add(targetXZ[i][0], currentY, targetXZ[i][1]);
                projLoc.setYaw(0);
                projLoc.setPitch(0);
                projectiles.get(i).entity().teleport(projLoc);

                // Falling trail
                if (!landed[i] && fallTick % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.FLAME,
                        projLoc, 3, 0.2, 0.2, 0.2, 0.01);
                    DisplayBuilder.dustParticles(projLoc, 2, 0.3, 200, 50, 10, 1.0f);
                }

                // Warning particles on ground before impact
                if (!landed[i] && currentY < 10 && currentY > 2) {
                    Location groundWarning = c.clone().add(targetXZ[i][0], 0.1, targetXZ[i][1]);
                    DisplayBuilder.dustParticles(groundWarning, 4, 0.5, 255, 50, 10, 1.5f);
                }
            }

            // Whooshing sounds as blocks fall
            if (tick % 4 == 0 && tick < 30) {
                DisplayBuilder.playSound(c.clone().add(0, 10, 0), Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneBarrage(plugin); }
    }
}
