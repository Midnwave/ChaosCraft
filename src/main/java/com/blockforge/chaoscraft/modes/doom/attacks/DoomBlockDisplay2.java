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
import java.util.Random;

/**
 * Doom Mode -- BLOCK DISPLAY ATTACKS (Part 2)
 * Attacks 11-20: Advanced weapon/siege/bombardment block display structures.
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
 * Rules:
 * - ALL animation via Transformation + setInterpolationDuration (no teleport rotation)
 * - 25+ blocks per attack
 * - 5-8 block damage radii
 * - No status effects
 * - Lava/fire materials
 * - AxisAngle4f ONLY (never Quaternionf)
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Location center = getCenter(); if (center == null) return; EVERY onTick
 */
public final class DoomBlockDisplay2 {
    private DoomBlockDisplay2() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MeteorSwarm(plugin));
        registry.register(new ObsidianWarhammer(plugin));
        registry.register(new InfernalTrident(plugin));
        registry.register(new LavaHydraHeads(plugin));
        registry.register(new DoomExecutionerAxe(plugin));
        registry.register(new BrimstoneBallista(plugin));
        registry.register(new MoltenBoulderBarrage(plugin));
        registry.register(new InfernalChainWhip(plugin));
        registry.register(new DoomCatapultPayload(plugin));
        registry.register(new HellfireRain(plugin));
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
    // #11 -- METEOR SWARM
    // 6 meteors (5 blocks each: 3 core + 2 tail scale(0.4,0.4,0.4)).
    // Fall from Y+25 staggered 10t, compound rotation during fall.
    // Impact-only 5r, 30 per meteor. FLAME+LAVA trail.
    // Total blocks: 30
    // ================================================================
    public static class MeteorSwarm extends BlockDisplayAttack {

        private static final int METEOR_COUNT = 6;
        private static final int BLOCKS_PER_METEOR = 5;
        private static final int STAGGER_TICKS = 10;
        private static final double FALL_HEIGHT = 25.0;
        private static final int FALL_DURATION = 30; // ticks to fall

        private final List<List<BlockDisplayHandle>> meteors = new ArrayList<>();
        private final double[] meteorOffsetX = new double[METEOR_COUNT];
        private final double[] meteorOffsetZ = new double[METEOR_COUNT];
        private final boolean[] impacted = new boolean[METEOR_COUNT];
        private Location spawnCenter;

        public MeteorSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_meteor_swarm", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            spawnCenter = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Random rng = new Random();
            for (int m = 0; m < METEOR_COUNT; m++) {
                meteorOffsetX[m] = (rng.nextDouble() - 0.5) * 12.0;
                meteorOffsetZ[m] = (rng.nextDouble() - 0.5) * 12.0;
                impacted[m] = false;

                List<BlockDisplayHandle> meteorBlocks = new ArrayList<>();
                Location meteorLoc = center.clone().add(meteorOffsetX[m], FALL_HEIGHT, meteorOffsetZ[m]);

                // 3 core blocks — magma, netherrack, glowstone
                Material[] coreMats = {Material.MAGMA_BLOCK, Material.NETHERRACK, Material.GLOWSTONE};
                double[][] coreOffsets = {{0, 0, 0}, {0.4, 0.2, 0.3}, {-0.3, 0.3, -0.2}};
                for (int i = 0; i < 3; i++) {
                    BlockDisplayHandle core = displayBuilder.spawnBlock(
                            meteorLoc.clone().add(coreOffsets[i][0], coreOffsets[i][1], coreOffsets[i][2]),
                            coreMats[i]);
                    core.scale(0.9f, 0.9f, 0.9f)
                        .glow(255, 100, 20)
                        .interpolation(FALL_DURATION, m * STAGGER_TICKS);
                    spawnedEntities.add(core.entity());
                    meteorBlocks.add(core);
                }

                // 2 tail blocks — smaller, trailing
                for (int i = 0; i < 2; i++) {
                    BlockDisplayHandle tail = displayBuilder.spawnBlock(
                            meteorLoc.clone().add((rng.nextDouble() - 0.5) * 0.5, 0.8 + i * 0.5, (rng.nextDouble() - 0.5) * 0.5),
                            Material.NETHERRACK);
                    tail.scale(0.4f, 0.4f, 0.4f)
                        .glow(240, 80, 30)
                        .interpolation(FALL_DURATION, m * STAGGER_TICKS);
                    spawnedEntities.add(tail.entity());
                    meteorBlocks.add(tail);
                }

                meteors.add(meteorBlocks);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int m = 0; m < METEOR_COUNT; m++) {
                int meteorStart = m * STAGGER_TICKS;
                if (tick < meteorStart) continue;
                if (impacted[m]) continue;

                int meteorTick = tick - meteorStart;
                double progress = Math.min(1.0, (double) meteorTick / FALL_DURATION);

                // Current Y offset from ground
                double currentY = FALL_HEIGHT * (1.0 - progress);

                // Compound rotation angle
                float rotAngle = (float) (progress * Math.PI * 4.0);

                // Apply transformation to all blocks in this meteor
                List<BlockDisplayHandle> mBlocks = meteors.get(m);
                for (int i = 0; i < mBlocks.size(); i++) {
                    BlockDisplayHandle h = mBlocks.get(i);
                    float baseScale = i < 3 ? 0.9f : 0.4f;

                    // Translation: fall toward ground, centered on spawn offset
                    Vector3f translation = new Vector3f(
                            (float) meteorOffsetX[m] - 0.5f,
                            (float) currentY - 0.5f,
                            (float) meteorOffsetZ[m] - 0.5f
                    );

                    // Compound rotation on X and Z axes
                    float axisX = (float) Math.sin(rotAngle * 0.3);
                    float axisZ = (float) Math.cos(rotAngle * 0.7);
                    float norm = (float) Math.sqrt(axisX * axisX + axisZ * axisZ);
                    if (norm < 0.01f) norm = 1.0f;

                    h.entity().setInterpolationDuration(3);
                    h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new Transformation(
                            translation,
                            new AxisAngle4f(rotAngle, axisX / norm, 0.3f, axisZ / norm),
                            new Vector3f(baseScale, baseScale, baseScale),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Particle trail
                if (meteorTick % 2 == 0) {
                    Location trailLoc = spawnCenter.clone().add(meteorOffsetX[m], currentY, meteorOffsetZ[m]);
                    w.spawnParticle(Particle.FLAME, trailLoc, 5, 0.3, 0.3, 0.3, 0.02);
                    w.spawnParticle(Particle.LAVA, trailLoc, 3, 0.2, 0.2, 0.2, 0.01);
                }

                // Impact check
                if (progress >= 1.0) {
                    impacted[m] = true;
                    Location impactLoc = spawnCenter.clone().add(meteorOffsetX[m], 0, meteorOffsetZ[m]);
                    triggerImpactDamage(impactLoc);
                    w.spawnParticle(Particle.LAVA, impactLoc, 30, 2.0, 0.5, 2.0, 0.1);
                    w.spawnParticle(Particle.FLAME, impactLoc, 20, 2.0, 1.0, 2.0, 0.05);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MeteorSwarm(plugin); }
    }

    // ================================================================
    // #12 -- OBSIDIAN WARHAMMER
    // Hammer head 12 obsidian (4x2x2 grid), handle 6 scale(0.25,1.0,0.25),
    // pommel 4, crying obsidian accents 6. Falls Y+18, Z-rotation spin.
    // Impact 9r, 65 damage. BLOCK_ANVIL_LAND.
    // Total blocks: 28
    // ================================================================
    public static class ObsidianWarhammer extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private static final double START_Y = 18.0;
        private static final int FALL_TICKS = 40;
        private boolean impactDone = false;

        public ObsidianWarhammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_obsidian_warhammer", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(65.0);
            config.setImpactRadius(9.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, START_Y, 0);

            // Hammer head: 12 obsidian in a 4x2x2 grid (wide flat head, offset forward)
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 2; y++) {
                    for (int z = 0; z < 2; z++) {
                        // Skip 4 corner blocks to stay at 12 (4x2x2 = 16 minus 4 corners)
                        if ((x == 0 || x == 3) && (z == 0 || z == 1) && y == 1) continue;
                        Location loc = base.clone().add(x * 0.6 - 0.9, y * 0.6 + 3.0, z * 0.6 - 0.3);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(0.6f, 0.6f, 0.6f)
                         .glow(20, 10, 5)
                         .interpolation(3, 0);
                        spawnedEntities.add(h.entity());
                        allBlocks.add(h);
                    }
                }
            }

            // Handle: 6 blocks vertically, thin
            for (int i = 0; i < 6; i++) {
                Location loc = base.clone().add(0.3, i * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.25f, 1.0f, 0.25f)
                 .glow(60, 40, 30)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Pommel: 4 blocks at bottom of handle
            double[][] pommelOffsets = {{0.1, -0.3, 0.1}, {0.5, -0.3, 0.1}, {0.1, -0.3, -0.3}, {0.5, -0.3, -0.3}};
            for (double[] off : pommelOffsets) {
                Location loc = base.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 0.4f, 0.4f)
                 .glow(20, 10, 5)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Crying obsidian accents: 6 blocks along hammer head edges
            double[][] accentOffsets = {
                {-1.2, 3.0, -0.3}, {-1.2, 3.0, 0.3}, {1.5, 3.0, -0.3},
                {1.5, 3.0, 0.3}, {-0.9, 3.6, 0.0}, {1.2, 3.6, 0.0}
            };
            for (double[] off : accentOffsets) {
                Location loc = base.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.35f, 0.35f, 0.35f)
                 .glow(140, 50, 200)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (impactDone) return;

            double progress = Math.min(1.0, (double) tick / FALL_TICKS);
            double currentY = START_Y * (1.0 - progress);

            // Z-axis spin during fall — full rotations
            float spinAngle = (float) (progress * Math.PI * 6.0);

            // Apply transformation to ALL blocks as a unified structure
            for (int i = 0; i < allBlocks.size(); i++) {
                BlockDisplayHandle h = allBlocks.get(i);
                Transformation oldT = h.entity().getTransformation();
                Vector3f oldTranslation = oldT.getTranslation();

                // Shift Y component down
                Vector3f newTranslation = new Vector3f(
                        oldTranslation.x,
                        oldTranslation.y - (float) (START_Y - currentY) + (float) START_Y * (float) ((double) (tick - 1) / FALL_TICKS > 1.0 ? 0 : (1.0 - (double) (tick - 1) / FALL_TICKS)),
                        oldTranslation.z
                );

                // Simpler: just set absolute Y based on progress
                float yOffset = (float) currentY - 0.5f;

                h.entity().setInterpolationDuration(3);
                h.entity().setInterpolationDelay(0);
                h.entity().setTransformation(new Transformation(
                        new Vector3f(oldTranslation.x, yOffset, oldTranslation.z),
                        new AxisAngle4f(spinAngle, 0, 0, 1),
                        oldT.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Particles during fall
            if (tick % 3 == 0) {
                Location trailLoc = center.clone().add(0, currentY + 2, 0);
                center.getWorld().spawnParticle(Particle.SMOKE, trailLoc, 6, 0.5, 0.5, 0.5, 0.02);
            }

            // Impact
            if (progress >= 1.0) {
                impactDone = true;
                triggerImpactDamage(center);
                center.getWorld().spawnParticle(Particle.LAVA, center, 40, 3.0, 0.5, 3.0, 0.1);
                center.getWorld().spawnParticle(Particle.SMOKE, center, 25, 2.0, 1.0, 2.0, 0.05);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianWarhammer(plugin); }
    }

    // ================================================================
    // #13 -- INFERNAL TRIDENT
    // 3 prongs (each 3 blocks scale(0.15,1.2,0.15)), shaft 5 scale(0.2,0.8,0.2),
    // cross-guard 4 scale(0.6,0.15,0.15), 6 deco, 2 wake blocks.
    // Tracks player, shaft-axis rotation. Impact 6r, 50.
    // Total blocks: 26
    // ================================================================
    public static class InfernalTrident extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private double approachAngle = 0;
        private double distance = 20.0;
        private boolean impactDone = false;
        private static final int APPROACH_TICKS = 50;

        public InfernalTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_infernal_trident", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(280);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Random rng = new Random();
            approachAngle = rng.nextDouble() * Math.PI * 2;
            distance = 20.0;

            Location spawnLoc = center.clone().add(
                    Math.cos(approachAngle) * distance,
                    8,
                    Math.sin(approachAngle) * distance
            );

            // 3 prongs: each 3 blocks, elongated thin rods
            Material[] prongMats = {Material.GLOWSTONE, Material.SHROOMLIGHT, Material.GLOWSTONE};
            double[] prongXOff = {-0.3, 0.0, 0.3};
            for (int p = 0; p < 3; p++) {
                for (int i = 0; i < 3; i++) {
                    Location loc = spawnLoc.clone().add(prongXOff[p], 3.0 + i * 0.8, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, prongMats[p]);
                    h.scale(0.15f, 1.2f, 0.15f)
                     .glow(255, 100, 20)
                     .interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                }
            }

            // Shaft: 5 blocks, thicker rod
            for (int i = 0; i < 5; i++) {
                Location loc = spawnLoc.clone().add(0, -i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(0.2f, 0.8f, 0.2f)
                 .glow(200, 50, 10)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Cross-guard: 4 blocks, wide thin bars
            double[][] guardOffsets = {{-0.8, 2.8, 0}, {0.8, 2.8, 0}, {0, 2.8, -0.5}, {0, 2.8, 0.5}};
            for (double[] off : guardOffsets) {
                Location loc = spawnLoc.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                h.scale(0.6f, 0.15f, 0.15f)
                 .glow(200, 50, 10)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Deco: 6 accent blocks around shaft/guard junction
            double[][] decoOffsets = {
                {-0.2, 2.5, 0.2}, {0.2, 2.5, -0.2}, {0, 2.2, 0.2},
                {0, 2.2, -0.2}, {-0.15, 1.8, 0}, {0.15, 1.8, 0}
            };
            for (double[] off : decoOffsets) {
                Location loc = spawnLoc.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.2f, 0.2f, 0.2f)
                 .glow(240, 80, 30)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Wake: 2 trailing particle-like blocks
            for (int i = 0; i < 2; i++) {
                Location loc = spawnLoc.clone().add(0, -3.5 - i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_CONCRETE);
                h.scale(0.15f, 0.4f, 0.15f)
                 .glow(255, 100, 20)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THROW, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (impactDone) return;

            double progress = Math.min(1.0, (double) tick / APPROACH_TICKS);
            double currentDist = 20.0 * (1.0 - progress);
            double currentY = 8.0 * (1.0 - progress);

            // Shaft-axis spin
            float spinAngle = (float) (tick * 0.3);

            // Position relative to target
            float offX = (float) (Math.cos(approachAngle) * currentDist);
            float offZ = (float) (Math.sin(approachAngle) * currentDist);

            for (int i = 0; i < allBlocks.size(); i++) {
                BlockDisplayHandle h = allBlocks.get(i);
                Transformation oldT = h.entity().getTransformation();

                // Translate entire trident toward player
                Vector3f translation = new Vector3f(
                        offX + oldT.getTranslation().x * 0.0f,
                        (float) currentY - 0.5f,
                        offZ + oldT.getTranslation().z * 0.0f
                );

                h.entity().setInterpolationDuration(3);
                h.entity().setInterpolationDelay(0);
                h.entity().setTransformation(new Transformation(
                        translation,
                        new AxisAngle4f(spinAngle, 0, 1, 0),
                        oldT.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Trail particles
            if (tick % 2 == 0) {
                Location trailLoc = center.clone().add(offX, currentY, offZ);
                center.getWorld().spawnParticle(Particle.FLAME, trailLoc, 4, 0.2, 0.3, 0.2, 0.01);
            }

            // Impact
            if (progress >= 1.0) {
                impactDone = true;
                triggerImpactDamage(center);
                center.getWorld().spawnParticle(Particle.LAVA, center, 25, 2.0, 0.5, 2.0, 0.08);
                DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_HIT, 2.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalTrident(plugin); }
    }

    // ================================================================
    // #14 -- LAVA HYDRA HEADS
    // 3 heads (4 head + 3 neck each = 21), base 6, 8 fire mane blocks.
    // Heads sway independently on sine. Strike every 60t (neck extends
    // via scale interpolation). Impact 4r, 40 per strike.
    // Total blocks: 35
    // ================================================================
    public static class LavaHydraHeads extends BlockDisplayAttack {

        private static final int HEAD_COUNT = 3;
        private final List<List<BlockDisplayHandle>> heads = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> necks = new ArrayList<>();
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> maneBlocks = new ArrayList<>();
        private final int[] lastStrikeTick = new int[HEAD_COUNT];
        private Location spawnCenter;

        public LavaHydraHeads(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_lava_hydra_heads", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(15.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            spawnCenter = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Base: 6 magma blocks forming a mound
            double[][] baseOffsets = {
                {-0.5, 0, -0.5}, {0.5, 0, -0.5}, {-0.5, 0, 0.5},
                {0.5, 0, 0.5}, {0, 0.5, 0}, {0, -0.3, 0}
            };
            for (double[] off : baseOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.MAGMA_BLOCK);
                h.scale(1.2f, 0.8f, 1.2f)
                 .glow(200, 50, 10)
                 .interpolation(5, 0);
                spawnedEntities.add(h.entity());
                baseBlocks.add(h);
            }

            // 3 heads with necks
            double[] headAngles = {-0.8, 0.0, 0.8}; // X offset for each head
            for (int h = 0; h < HEAD_COUNT; h++) {
                lastStrikeTick[h] = -60;
                List<BlockDisplayHandle> neckList = new ArrayList<>();
                List<BlockDisplayHandle> headList = new ArrayList<>();

                // Neck: 3 blocks extending upward
                for (int n = 0; n < 3; n++) {
                    Location neckLoc = center.clone().add(headAngles[h], 1.0 + n * 1.2, 0);
                    BlockDisplayHandle neckBlock = displayBuilder.spawnBlock(neckLoc, Material.NETHER_BRICKS);
                    neckBlock.scale(0.5f, 1.0f, 0.5f)
                             .glow(200, 50, 10)
                             .interpolation(5, 0);
                    spawnedEntities.add(neckBlock.entity());
                    neckList.add(neckBlock);
                }

                // Head: 4 blocks forming a jaw-like shape
                double headY = 1.0 + 3 * 1.2;
                double[][] headOffsets = {
                    {headAngles[h], headY, 0},
                    {headAngles[h] + 0.3, headY + 0.3, 0.3},
                    {headAngles[h] - 0.3, headY + 0.3, 0.3},
                    {headAngles[h], headY + 0.5, 0}
                };
                Material[] headMats = {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.GLOWSTONE};
                for (int i = 0; i < 4; i++) {
                    BlockDisplayHandle headBlock = displayBuilder.spawnBlock(
                            center.clone().add(headOffsets[i][0], headOffsets[i][1], headOffsets[i][2]),
                            headMats[i]);
                    headBlock.scale(0.7f, 0.6f, 0.7f)
                             .glow(255, 100, 20)
                             .interpolation(5, 0);
                    spawnedEntities.add(headBlock.entity());
                    headList.add(headBlock);
                }

                necks.add(neckList);
                heads.add(headList);
            }

            // Fire mane: 8 shroomlight blocks around the heads
            double[][] maneOffsets = {
                {-1.2, 4.5, 0.3}, {1.2, 4.5, 0.3}, {0, 5.0, -0.3}, {0, 5.0, 0.5},
                {-0.6, 4.8, -0.4}, {0.6, 4.8, -0.4}, {-0.9, 4.2, 0.5}, {0.9, 4.2, 0.5}
            };
            for (double[] off : maneOffsets) {
                BlockDisplayHandle m = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.SHROOMLIGHT);
                m.scale(0.4f, 0.4f, 0.4f)
                 .glow(255, 100, 20)
                 .interpolation(5, 0);
                spawnedEntities.add(m.entity());
                maneBlocks.add(m);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Sway each head independently on sine
            for (int h = 0; h < HEAD_COUNT; h++) {
                double swayX = Math.sin(tick * 0.05 + h * 2.1) * 0.8;
                double swayZ = Math.cos(tick * 0.07 + h * 1.7) * 0.5;
                float swayAngle = (float) (Math.sin(tick * 0.06 + h * 2.0) * 0.3);

                // Animate neck blocks — interpolate sway via transformation
                List<BlockDisplayHandle> neckList = necks.get(h);
                for (int n = 0; n < neckList.size(); n++) {
                    double neckFactor = (n + 1.0) / 3.0;
                    float tx = (float) (swayX * neckFactor);
                    float tz = (float) (swayZ * neckFactor);

                    BlockDisplayHandle nb = neckList.get(n);
                    Transformation oldT = nb.entity().getTransformation();
                    nb.entity().setInterpolationDuration(8);
                    nb.entity().setInterpolationDelay(0);
                    nb.entity().setTransformation(new Transformation(
                            new Vector3f(tx - 0.25f, oldT.getTranslation().y, tz - 0.25f),
                            new AxisAngle4f(swayAngle * (float) neckFactor, 0, 0, 1),
                            oldT.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Animate head blocks
                List<BlockDisplayHandle> headList = heads.get(h);
                for (BlockDisplayHandle hb : headList) {
                    Transformation oldT = hb.entity().getTransformation();
                    hb.entity().setInterpolationDuration(8);
                    hb.entity().setInterpolationDelay(0);
                    hb.entity().setTransformation(new Transformation(
                            new Vector3f((float) swayX - 0.35f, oldT.getTranslation().y, (float) swayZ - 0.35f),
                            new AxisAngle4f(swayAngle, 0, 0, 1),
                            oldT.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Strike every 60 ticks — extend neck via scale interpolation
                if (tick - lastStrikeTick[h] >= 60) {
                    lastStrikeTick[h] = tick;

                    // Extend neck blocks vertically
                    for (BlockDisplayHandle nb : neckList) {
                        nb.entity().setInterpolationDuration(10);
                        nb.entity().setInterpolationDelay(0);
                        Transformation t = nb.entity().getTransformation();
                        nb.entity().setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(0.5f, 1.8f, 0.5f), // Extended scale
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }

                    // Strike damage at head position
                    Player nearest = findNearestPlayer(center, 8);
                    if (nearest != null) {
                        Location strikeLoc = nearest.getLocation();
                        triggerImpactDamage(strikeLoc);
                        w.spawnParticle(Particle.FLAME, strikeLoc, 15, 1.0, 0.5, 1.0, 0.05);
                        DisplayBuilder.playSound(strikeLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
                    }
                }

                // Retract neck after 15 ticks of striking
                if (tick - lastStrikeTick[h] == 15) {
                    for (BlockDisplayHandle nb : neckList) {
                        nb.entity().setInterpolationDuration(10);
                        nb.entity().setInterpolationDelay(0);
                        Transformation t = nb.entity().getTransformation();
                        nb.entity().setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(0.5f, 1.0f, 0.5f), // Normal scale
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Mane flicker particles
            if (tick % 5 == 0) {
                for (BlockDisplayHandle m : maneBlocks) {
                    Location mLoc = m.entity().getLocation();
                    w.spawnParticle(Particle.FLAME, mLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaHydraHeads(plugin); }
    }

    // ================================================================
    // #15 -- DOOM EXECUTIONER AXE
    // Crescent blade 10 blocks (arc, scale(0.8,0.15,0.5) thin), handle 6,
    // pommel 3, glowing edge 8 shroomlight. 180-deg Y-arc sweep.
    // Contact 7r, 55 per sweep. ENTITY_PLAYER_ATTACK_SWEEP.
    // Total blocks: 27
    // ================================================================
    public static class DoomExecutionerAxe extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private int sweepCount = 0;
        private static final int SWEEP_TICKS = 30;

        public DoomExecutionerAxe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_executioner_axe", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(SWEEP_TICKS);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Crescent blade: 10 blocks in an arc shape, thin profile
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI * 0.3 + (Math.PI * 0.4 * i / 9.0); // Arc from ~55 to ~125 degrees
                double bx = Math.cos(angle) * 2.5;
                double by = Math.sin(angle) * 2.5 + 4.0;
                Location loc = center.clone().add(bx, by, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.8f, 0.15f, 0.5f)
                 .glow(20, 10, 5)
                 .interpolation(5, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Handle: 6 blocks descending
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 3.5 - i * 0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.3f, 0.7f, 0.3f)
                 .glow(60, 40, 30)
                 .interpolation(5, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Pommel: 3 blocks at bottom
            double[][] pommelOffsets = {{-0.2, -0.5, 0}, {0.2, -0.5, 0}, {0, -0.8, 0}};
            for (double[] off : pommelOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 0.4f, 0.4f)
                 .glow(20, 10, 5)
                 .interpolation(5, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Glowing edge: 8 shroomlight blocks along the blade edge
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 0.32 + (Math.PI * 0.36 * i / 7.0);
                double ex = Math.cos(angle) * 2.8;
                double ey = Math.sin(angle) * 2.8 + 4.0;
                Location loc = center.clone().add(ex, ey, 0.15);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.3f, 0.1f, 0.2f)
                 .glow(255, 100, 20)
                 .interpolation(5, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // 180-degree Y-axis arc sweep, alternating direction each sweep
            int sweepPhase = tick % (SWEEP_TICKS * 2);
            double sweepProgress;
            if (sweepPhase < SWEEP_TICKS) {
                sweepProgress = (double) sweepPhase / SWEEP_TICKS;
            } else {
                sweepProgress = 1.0 - ((double) (sweepPhase - SWEEP_TICKS) / SWEEP_TICKS);
            }

            float yRotation = (float) (sweepProgress * Math.PI); // 0 to 180 degrees

            for (BlockDisplayHandle h : allBlocks) {
                Transformation oldT = h.entity().getTransformation();
                h.entity().setInterpolationDuration(5);
                h.entity().setInterpolationDelay(0);
                h.entity().setTransformation(new Transformation(
                        oldT.getTranslation(),
                        new AxisAngle4f(yRotation, 0, 1, 0),
                        oldT.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Sweep sound at each direction change
            if (sweepPhase == 0 || sweepPhase == SWEEP_TICKS) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
                center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 3, 0), 8, 2.0, 1.0, 2.0, 0.1);
            }

            // Trail particles along blade arc
            if (tick % 3 == 0) {
                double trailAngle = Math.PI * 0.5;
                double trailX = Math.cos(trailAngle + yRotation) * 2.5;
                double trailZ = Math.sin(trailAngle + yRotation) * 2.5;
                Location trailLoc = center.clone().add(trailX, 4.0, trailZ);
                center.getWorld().spawnParticle(Particle.FLAME, trailLoc, 3, 0.3, 0.2, 0.3, 0.02);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomExecutionerAxe(plugin); }
    }

    // ================================================================
    // #16 -- BRIMSTONE BALLISTA
    // A-frame 10, arms 8 angled, bolt 6 scale(0.15,0.1,2.0),
    // wheels 8 (circle). Fires bolt every 80t -- detaches, flies 15 blocks.
    // Impact 5r, 45. ENTITY_CROSSBOW_SHOOT.
    // Total blocks: 30 (excluding re-fired bolts which reuse entities)
    // ================================================================
    public static class BrimstoneBallista extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> boltBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> wheelBlocks = new ArrayList<>();
        private int lastFireTick = -80;
        private boolean boltFlying = false;
        private double boltDistance = 0;
        private double boltAngle = 0;

        public BrimstoneBallista(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_brimstone_ballista", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(45.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // A-frame: 10 blocks forming triangular support structure
            double[][] frameOffsets = {
                // Left leg
                {-1.5, 0, 0}, {-1.2, 1.0, 0}, {-0.8, 2.0, 0},
                // Right leg
                {1.5, 0, 0}, {1.2, 1.0, 0}, {0.8, 2.0, 0},
                // Top cross-beam
                {-0.4, 2.5, 0}, {0.4, 2.5, 0},
                // Rear braces
                {-0.8, 0.5, -1.0}, {0.8, 0.5, -1.0}
            };
            for (double[] off : frameOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHER_BRICKS);
                h.scale(0.4f, 0.5f, 0.4f)
                 .glow(200, 50, 10)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                frameBlocks.add(h);
            }

            // Arms: 8 blocks angled from top toward front
            for (int i = 0; i < 8; i++) {
                double armAngle = (i < 4) ? -0.3 : 0.3;
                int idx = i % 4;
                Location loc = center.clone().add(armAngle * (idx + 1), 2.5 - idx * 0.2, 0.5 + idx * 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                h.scale(0.3f, 0.25f, 0.6f)
                 .glow(200, 50, 10)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                frameBlocks.add(h);
            }

            // Bolt: 6 blocks in a line, long and thin
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 2.3, 0.5 + i * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.15f, 0.1f, 2.0f)
                 .glow(255, 100, 20)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                boltBlocks.add(h);
            }

            // Wheels: 8 blocks in circle pattern on sides
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8.0;
                double wx = Math.cos(angle) * 0.8;
                double wy = Math.sin(angle) * 0.8;
                double side = (i < 4) ? -1.8 : 1.8;
                Location loc = center.clone().add(side, 0.8 + wy, wx);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(0.3f, 0.3f, 0.3f)
                 .glow(60, 40, 30)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                wheelBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Aim at nearest player
            Player nearest = findNearestPlayer(center, 25);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                boltAngle = Math.atan2(dz, dx);
            }

            // Fire bolt every 80 ticks
            if (tick - lastFireTick >= 80 && !boltFlying) {
                lastFireTick = tick;
                boltFlying = true;
                boltDistance = 0;
                DisplayBuilder.playSound(center, Sound.ITEM_CROSSBOW_SHOOT, 1.5f, 0.4f);
            }

            // Animate bolt flight
            if (boltFlying) {
                boltDistance += 0.8; // ~0.8 blocks per tick
                if (boltDistance >= 15.0) {
                    boltFlying = false;
                    // Impact at bolt destination
                    Location impactLoc = center.clone().add(
                            Math.cos(boltAngle) * 15.0, 0, Math.sin(boltAngle) * 15.0);
                    triggerImpactDamage(impactLoc);
                    w.spawnParticle(Particle.LAVA, impactLoc, 20, 1.5, 0.5, 1.5, 0.05);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                }

                // Move bolt blocks forward
                for (int i = 0; i < boltBlocks.size(); i++) {
                    BlockDisplayHandle h = boltBlocks.get(i);
                    float bx = (float) (Math.cos(boltAngle) * boltDistance);
                    float bz = (float) (Math.sin(boltAngle) * boltDistance);
                    h.entity().setInterpolationDuration(3);
                    h.entity().setInterpolationDelay(0);
                    Transformation oldT = h.entity().getTransformation();
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(bx - 0.5f, 1.8f, bz + i * 0.3f - 0.5f),
                            new AxisAngle4f((float) boltAngle, 0, 1, 0),
                            oldT.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Bolt trail particles
                if (tick % 2 == 0) {
                    Location trailLoc = center.clone().add(
                            Math.cos(boltAngle) * boltDistance, 2.3, Math.sin(boltAngle) * boltDistance);
                    w.spawnParticle(Particle.FLAME, trailLoc, 3, 0.1, 0.1, 0.1, 0.01);
                }
            } else {
                // Reset bolt to loaded position
                for (int i = 0; i < boltBlocks.size(); i++) {
                    BlockDisplayHandle h = boltBlocks.get(i);
                    h.entity().setInterpolationDuration(10);
                    h.entity().setInterpolationDelay(0);
                    Transformation oldT = h.entity().getTransformation();
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.5f, 1.8f, i * 0.3f),
                            new AxisAngle4f(0, 0, 1, 0),
                            oldT.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Wheel rotation animation
            float wheelSpin = (float) (tick * 0.1);
            for (BlockDisplayHandle wh : wheelBlocks) {
                Transformation oldT = wh.entity().getTransformation();
                wh.entity().setInterpolationDuration(5);
                wh.entity().setInterpolationDelay(0);
                wh.entity().setTransformation(new Transformation(
                        oldT.getTranslation(),
                        new AxisAngle4f(wheelSpin, 1, 0, 0),
                        oldT.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneBallista(plugin); }
    }

    // ================================================================
    // #17 -- MOLTEN BOULDER BARRAGE
    // 5 boulders (5 each irregular sphere). Roll outward from center,
    // X/Z compound rotation, accelerate. Contact 4r, 35 per boulder.
    // BLOCK_STONE_BREAK + BLOCK_CRACK particles.
    // Total blocks: 25
    // ================================================================
    public static class MoltenBoulderBarrage extends BlockDisplayAttack {

        private static final int BOULDER_COUNT = 5;
        private static final int BLOCKS_PER_BOULDER = 5;
        private final List<List<BlockDisplayHandle>> boulders = new ArrayList<>();
        private final double[] boulderAngles = new double[BOULDER_COUNT];
        private final double[] boulderDist = new double[BOULDER_COUNT];
        private final double[] boulderSpeed = new double[BOULDER_COUNT];

        public MoltenBoulderBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_molten_boulder_barrage", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Random rng = new Random();
            for (int b = 0; b < BOULDER_COUNT; b++) {
                boulderAngles[b] = (2.0 * Math.PI * b) / BOULDER_COUNT + (rng.nextDouble() - 0.5) * 0.5;
                boulderDist[b] = 0.5;
                boulderSpeed[b] = 0.1 + rng.nextDouble() * 0.05;

                List<BlockDisplayHandle> bBlocks = new ArrayList<>();

                // Irregular sphere: 5 blocks at slightly random offsets
                Material[] mats = {Material.MAGMA_BLOCK, Material.NETHERRACK, Material.MAGMA_BLOCK,
                                   Material.BASALT, Material.NETHERRACK};
                for (int i = 0; i < BLOCKS_PER_BOULDER; i++) {
                    double ox = (rng.nextDouble() - 0.5) * 0.6;
                    double oy = (rng.nextDouble() - 0.5) * 0.6;
                    double oz = (rng.nextDouble() - 0.5) * 0.6;
                    Location loc = center.clone().add(ox, 0.5 + oy, oz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                    h.scale(0.7f + rng.nextFloat() * 0.3f, 0.7f + rng.nextFloat() * 0.3f, 0.7f + rng.nextFloat() * 0.3f)
                     .glow(255, 100, 20)
                     .interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    bBlocks.add(h);
                }

                boulders.add(bBlocks);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int b = 0; b < BOULDER_COUNT; b++) {
                // Accelerate outward
                boulderSpeed[b] += 0.008;
                boulderDist[b] += boulderSpeed[b];

                double bx = Math.cos(boulderAngles[b]) * boulderDist[b];
                double bz = Math.sin(boulderAngles[b]) * boulderDist[b];

                // Compound X/Z rotation for rolling effect
                float rollAngle = (float) (boulderDist[b] * 2.0);
                float axisX = (float) Math.cos(boulderAngles[b]);
                float axisZ = (float) Math.sin(boulderAngles[b]);

                List<BlockDisplayHandle> bBlocks = boulders.get(b);
                for (int i = 0; i < bBlocks.size(); i++) {
                    BlockDisplayHandle h = bBlocks.get(i);
                    Transformation oldT = h.entity().getTransformation();

                    // Translate outward from center
                    Vector3f translation = new Vector3f(
                            (float) bx - 0.5f + (i % 2 == 0 ? 0.15f : -0.15f),
                            0.0f + (float) Math.abs(Math.sin(rollAngle + i)) * 0.3f,
                            (float) bz - 0.5f + (i % 3 == 0 ? 0.1f : -0.1f)
                    );

                    h.entity().setInterpolationDuration(3);
                    h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new Transformation(
                            translation,
                            new AxisAngle4f(rollAngle, axisX, 0.2f, axisZ),
                            oldT.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Trail particles
                if (tick % 4 == b % 4) {
                    Location trailLoc = center.clone().add(bx, 0.5, bz);
                    w.spawnParticle(Particle.BLOCK, trailLoc, 5, 0.3, 0.2, 0.3, 0.01,
                            Material.MAGMA_BLOCK.createBlockData());
                    w.spawnParticle(Particle.FLAME, trailLoc, 2, 0.2, 0.1, 0.2, 0.01);
                }
            }

            // Rumble sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MoltenBoulderBarrage(plugin); }
    }

    // ================================================================
    // #18 -- INFERNAL CHAIN WHIP
    // 16 chain links scale(0.3,0.3,0.3) in catenary, spiked ball 5,
    // handle 3, 4 accents. Pendulum X-axis sine swing.
    // Ball contact 5r, 45. BLOCK_CHAIN_BREAK.
    // Total blocks: 28
    // ================================================================
    public static class InfernalChainWhip extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> spikedBall = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> accentBlocks = new ArrayList<>();

        public InfernalChainWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_infernal_chain_whip", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location anchorPoint = center.clone().add(0, 8, 0);

            // Handle: 3 blocks at anchor point
            Material[] handleMats = {Material.POLISHED_BLACKSTONE, Material.BLACKSTONE, Material.POLISHED_BLACKSTONE};
            for (int i = 0; i < 3; i++) {
                Location loc = anchorPoint.clone().add(0, -i * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, handleMats[i]);
                h.scale(0.4f, 0.5f, 0.4f)
                 .glow(60, 40, 30)
                 .interpolation(5, 0);
                spawnedEntities.add(h.entity());
                handleBlocks.add(h);
            }

            // Chain links: 16 blocks descending in catenary curve
            for (int i = 0; i < 16; i++) {
                double t = (double) i / 15.0;
                // Catenary curve: y = cosh(t) shape
                double chainY = anchorPoint.getY() - 1.5 - i * 0.35 - Math.pow(t, 2) * 2.0;
                double chainX = Math.sin(t * Math.PI * 0.5) * 1.5;
                Location loc = new Location(w, center.getX() + chainX, chainY, center.getZ());

                Material chainMat = (i % 3 == 0) ? Material.COAL_BLOCK : Material.DEEPSLATE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, chainMat);
                h.scale(0.3f, 0.3f, 0.3f)
                 .glow(100, 60, 20)
                 .interpolation(4, 0);
                spawnedEntities.add(h.entity());
                chainLinks.add(h);
            }

            // Spiked ball: 5 blocks at the end of the chain
            Location ballCenter = new Location(w, center.getX() + 1.5, anchorPoint.getY() - 8.5, center.getZ());
            double[][] spikeOffsets = {{0, 0, 0}, {0.4, 0.3, 0}, {-0.4, 0.3, 0}, {0, 0.3, 0.4}, {0, 0.3, -0.4}};
            Material[] spikeMats = {Material.OBSIDIAN, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK};
            for (int i = 0; i < 5; i++) {
                Location loc = ballCenter.clone().add(spikeOffsets[i][0], spikeOffsets[i][1], spikeOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, spikeMats[i]);
                float sc = (i == 0) ? 0.9f : 0.4f;
                h.scale(sc, sc, sc)
                 .glow(255, 100, 20)
                 .interpolation(4, 0);
                spawnedEntities.add(h.entity());
                spikedBall.add(h);
            }

            // Accents: 4 glowing blocks along chain
            int[] accentIndices = {3, 7, 11, 14};
            for (int idx : accentIndices) {
                Location loc = chainLinks.get(idx).entity().getLocation().clone().add(0.2, 0, 0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.15f, 0.15f, 0.15f)
                 .glow(255, 100, 20)
                 .interpolation(4, 0);
                spawnedEntities.add(h.entity());
                accentBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Pendulum swing on X-axis using sine
            double swingAngle = Math.sin(tick * 0.08) * 1.2; // ~69 deg amplitude

            // Update chain links with pendulum catenary
            for (int i = 0; i < chainLinks.size(); i++) {
                double t = (double) i / 15.0;
                double linkSwing = swingAngle * t; // More swing toward the end

                // Catenary positions affected by swing
                double chainX = Math.sin(t * Math.PI * 0.5 + linkSwing) * (1.5 + t * 2.0);
                double chainY = 8.0 - 1.5 - i * 0.35 - Math.pow(t, 2) * 2.0;

                BlockDisplayHandle h = chainLinks.get(i);
                h.entity().setInterpolationDuration(4);
                h.entity().setInterpolationDelay(0);
                Transformation oldT = h.entity().getTransformation();
                h.entity().setTransformation(new Transformation(
                        new Vector3f((float) chainX - 0.15f, (float) chainY - 0.15f, -0.15f),
                        new AxisAngle4f((float) (linkSwing * 0.3), 0, 0, 1),
                        oldT.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Update spiked ball — at the end of the chain, maximum swing
            double ballX = Math.sin(Math.PI * 0.5 + swingAngle) * 3.5;
            double ballY = 8.0 - 8.5 + Math.cos(swingAngle) * 0.5;
            for (int i = 0; i < spikedBall.size(); i++) {
                BlockDisplayHandle h = spikedBall.get(i);
                Transformation oldT = h.entity().getTransformation();
                double[] sOff = new double[][]{{0, 0, 0}, {0.4, 0.3, 0}, {-0.4, 0.3, 0}, {0, 0.3, 0.4}, {0, 0.3, -0.4}}[i];
                h.entity().setInterpolationDuration(4);
                h.entity().setInterpolationDelay(0);
                h.entity().setTransformation(new Transformation(
                        new Vector3f((float) (ballX + sOff[0]) - 0.45f, (float) (ballY + sOff[1]) - 0.45f, (float) sOff[2] - 0.15f),
                        new AxisAngle4f((float) (swingAngle * 2.0), 1, 0, 0),
                        oldT.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Update damage center to ball position
            setCenter(center.clone().add(ballX, ballY - 8, 0));

            // Particles at ball
            if (tick % 3 == 0) {
                Location ballLoc = center.clone().add(ballX, ballY - 8, 0);
                w.spawnParticle(Particle.FLAME, ballLoc, 3, 0.3, 0.3, 0.3, 0.02);
                w.spawnParticle(Particle.SMOKE, ballLoc, 2, 0.2, 0.2, 0.2, 0.01);
            }

            // Chain rattle sound
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.5f + (float) Math.abs(Math.sin(tick * 0.08)) * 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalChainWhip(plugin); }
    }

    // ================================================================
    // #19 -- DOOM CATAPULT PAYLOAD
    // Arm 8 elongated, counterweight 6, frame 8, payload 4.
    // Arm Z-rotation 0 to 90 deg launches payload on parabolic arc.
    // Impact-only 8r, 50. BLOCK_PISTON_EXTEND.
    // Total blocks: 26
    // ================================================================
    public static class DoomCatapultPayload extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> armBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> counterweightBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> payloadBlocks = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;
        private double payloadX = 0, payloadY = 0;
        private double launchAngle = 0;
        private boolean impactDone = false;

        public DoomCatapultPayload(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_catapult_payload", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Determine launch direction toward nearest player
            Player nearest = findNearestPlayer(center, 30);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                launchAngle = Math.atan2(dz, dx);
            }

            // Frame: 8 blocks forming the base structure
            double[][] frameOffsets = {
                {-1.5, 0, -0.5}, {-1.5, 0, 0.5}, {1.5, 0, -0.5}, {1.5, 0, 0.5},
                {-1.5, 1.5, 0}, {1.5, 1.5, 0},
                {-1.0, 2.0, 0}, {1.0, 2.0, 0}
            };
            for (double[] off : frameOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHER_BRICKS);
                h.scale(0.5f, 0.6f, 0.5f)
                 .glow(200, 50, 10)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                frameBlocks.add(h);
            }

            // Arm: 8 blocks elongated, starts horizontal
            for (int i = 0; i < 8; i++) {
                double armPos = -3.0 + i * 0.9;
                Location loc = center.clone().add(armPos, 2.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                h.scale(0.3f, 0.3f, 0.8f)
                 .glow(200, 50, 10)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                armBlocks.add(h);
            }

            // Counterweight: 6 blocks at the short end of arm
            double[][] cwOffsets = {
                {-3.0, 2.2, -0.3}, {-3.0, 2.2, 0.3}, {-3.3, 2.0, 0},
                {-2.7, 2.0, 0}, {-3.0, 1.8, -0.2}, {-3.0, 1.8, 0.2}
            };
            for (double[] off : cwOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f)
                 .glow(20, 10, 5)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                counterweightBlocks.add(h);
            }

            // Payload: 4 blocks at the long end of arm
            double[][] plOffsets = {{4.0, 2.8, 0}, {4.3, 3.0, 0.2}, {4.3, 3.0, -0.2}, {4.0, 3.2, 0}};
            for (double[] off : plOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.MAGMA_BLOCK);
                h.scale(0.6f, 0.6f, 0.6f)
                 .glow(255, 100, 20)
                 .interpolation(3, 0);
                spawnedEntities.add(h.entity());
                payloadBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Phase 1: Arm rotates from 0 to 90 degrees over 30 ticks
            if (tick <= 30 && !launched) {
                double armProgress = (double) tick / 30.0;
                float armAngle = (float) (armProgress * Math.PI * 0.5); // 0 to 90 degrees

                // Rotate arm blocks via Z-axis rotation
                for (BlockDisplayHandle h : armBlocks) {
                    Transformation oldT = h.entity().getTransformation();
                    h.entity().setInterpolationDuration(3);
                    h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new Transformation(
                            oldT.getTranslation(),
                            new AxisAngle4f(armAngle, 0, 0, 1),
                            oldT.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Counterweight follows rotation
                for (BlockDisplayHandle h : counterweightBlocks) {
                    Transformation oldT = h.entity().getTransformation();
                    h.entity().setInterpolationDuration(3);
                    h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new Transformation(
                            oldT.getTranslation(),
                            new AxisAngle4f(armAngle, 0, 0, 1),
                            oldT.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                if (tick == 30) {
                    launched = true;
                    launchTick = tick;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                }
            }

            // Phase 2: Payload flies on parabolic arc
            if (launched && !impactDone) {
                int flightTick = tick - launchTick;
                // Parabolic arc: x = v*t, y = v*t - 0.5*g*t^2
                double t = flightTick * 0.05;
                double launchVelocity = 12.0;
                double gravity = 6.0;
                payloadX = launchVelocity * t;
                payloadY = launchVelocity * t * 0.6 - 0.5 * gravity * t * t;

                // Move payload blocks along arc in launch direction
                for (int i = 0; i < payloadBlocks.size(); i++) {
                    BlockDisplayHandle h = payloadBlocks.get(i);
                    float px = (float) (Math.cos(launchAngle) * payloadX);
                    float pz = (float) (Math.sin(launchAngle) * payloadX);
                    float py = (float) payloadY + 3.0f;

                    h.entity().setInterpolationDuration(3);
                    h.entity().setInterpolationDelay(0);
                    Transformation oldT = h.entity().getTransformation();
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(px - 0.3f + i * 0.15f, py, pz - 0.3f),
                            new AxisAngle4f((float) (flightTick * 0.3), 1, 0, 1),
                            oldT.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Trail particles
                if (flightTick % 2 == 0) {
                    double trailPx = Math.cos(launchAngle) * payloadX;
                    double trailPz = Math.sin(launchAngle) * payloadX;
                    Location trailLoc = center.clone().add(trailPx, payloadY + 3, trailPz);
                    w.spawnParticle(Particle.FLAME, trailLoc, 4, 0.3, 0.3, 0.3, 0.02);
                    w.spawnParticle(Particle.SMOKE, trailLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }

                // Impact when payload hits ground
                if (payloadY < -3.0 && flightTick > 5) {
                    impactDone = true;
                    Location impactLoc = center.clone().add(
                            Math.cos(launchAngle) * payloadX, 0, Math.sin(launchAngle) * payloadX);
                    triggerImpactDamage(impactLoc);
                    w.spawnParticle(Particle.LAVA, impactLoc, 35, 3.0, 0.5, 3.0, 0.1);
                    w.spawnParticle(Particle.SMOKE, impactLoc, 20, 2.0, 1.5, 2.0, 0.05);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomCatapultPayload(plugin); }
    }

    // ================================================================
    // #20 -- HELLFIRE RAIN
    // 30 blocks scale(0.5,0.5,0.5) random rotation, rain from Y+20
    // across 10x10. Staggered 3t. Each impact 3r, 20.
    // 90 ticks bombardment. ENTITY_BLAZE_SHOOT.
    // Total blocks: 30
    // ================================================================
    public static class HellfireRain extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 30;
        private static final int STAGGER_TICKS = 3;
        private static final double FALL_HEIGHT = 20.0;
        private static final int FALL_DURATION = 25;
        private static final double SPREAD = 10.0;

        private final List<BlockDisplayHandle> rainBlocks = new ArrayList<>();
        private final double[] blockOffsetX = new double[BLOCK_COUNT];
        private final double[] blockOffsetZ = new double[BLOCK_COUNT];
        private final float[] blockRotX = new float[BLOCK_COUNT];
        private final float[] blockRotY = new float[BLOCK_COUNT];
        private final float[] blockRotZ = new float[BLOCK_COUNT];
        private final boolean[] impacted = new boolean[BLOCK_COUNT];

        public HellfireRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_hellfire_rain", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Random rng = new Random();
            Material[] rainMats = {Material.MAGMA_BLOCK, Material.NETHERRACK, Material.GLOWSTONE,
                                    Material.SHROOMLIGHT, Material.RED_NETHER_BRICKS, Material.NETHER_BRICKS};

            for (int i = 0; i < BLOCK_COUNT; i++) {
                blockOffsetX[i] = (rng.nextDouble() - 0.5) * SPREAD;
                blockOffsetZ[i] = (rng.nextDouble() - 0.5) * SPREAD;
                blockRotX[i] = rng.nextFloat() * 2.0f - 1.0f;
                blockRotY[i] = rng.nextFloat() * 2.0f - 1.0f;
                blockRotZ[i] = rng.nextFloat() * 2.0f - 1.0f;
                // Normalize rotation axis
                float norm = (float) Math.sqrt(blockRotX[i] * blockRotX[i] + blockRotY[i] * blockRotY[i] + blockRotZ[i] * blockRotZ[i]);
                if (norm < 0.01f) norm = 1.0f;
                blockRotX[i] /= norm;
                blockRotY[i] /= norm;
                blockRotZ[i] /= norm;
                impacted[i] = false;

                Location loc = center.clone().add(blockOffsetX[i], FALL_HEIGHT, blockOffsetZ[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, rainMats[rng.nextInt(rainMats.length)]);
                h.scale(0.5f, 0.5f, 0.5f)
                 .glow(255, 100, 20)
                 .interpolation(3, i * STAGGER_TICKS);
                spawnedEntities.add(h.entity());
                rainBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                int blockStart = i * STAGGER_TICKS;
                if (tick < blockStart) continue;
                if (impacted[i]) continue;

                int blockTick = tick - blockStart;
                double progress = Math.min(1.0, (double) blockTick / FALL_DURATION);
                double currentY = FALL_HEIGHT * (1.0 - progress);

                // Random rotation during fall
                float rotAngle = (float) (progress * Math.PI * 3.0 + i * 0.5);

                BlockDisplayHandle h = rainBlocks.get(i);
                h.entity().setInterpolationDuration(3);
                h.entity().setInterpolationDelay(0);
                h.entity().setTransformation(new Transformation(
                        new Vector3f((float) blockOffsetX[i] - 0.25f, (float) currentY - 0.25f, (float) blockOffsetZ[i] - 0.25f),
                        new AxisAngle4f(rotAngle, blockRotX[i], blockRotY[i], blockRotZ[i]),
                        new Vector3f(0.5f, 0.5f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));

                // Trail
                if (blockTick % 3 == 0) {
                    Location trailLoc = center.clone().add(blockOffsetX[i], currentY, blockOffsetZ[i]);
                    w.spawnParticle(Particle.FLAME, trailLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }

                // Impact
                if (progress >= 1.0) {
                    impacted[i] = true;
                    Location impactLoc = center.clone().add(blockOffsetX[i], 0, blockOffsetZ[i]);
                    triggerImpactDamage(impactLoc);
                    w.spawnParticle(Particle.LAVA, impactLoc, 8, 0.8, 0.3, 0.8, 0.05);
                    w.spawnParticle(Particle.FLAME, impactLoc, 5, 0.5, 0.3, 0.5, 0.02);
                }
            }

            // Ongoing blaze sounds during bombardment
            if (tick % 15 == 0 && tick < BLOCK_COUNT * STAGGER_TICKS + FALL_DURATION) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.5f + (float) (tick % 30) * 0.02f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireRain(plugin); }
    }
}
