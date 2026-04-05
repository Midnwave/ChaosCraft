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
 * 10 hellfire and doom-themed BlockDisplay attacks.
 *
 * Categories:
 *   31-32: Projectile / Falling
 *   33-34: Ground / Area Denial
 *   35-36: Descending / Overhead
 *   37-38: Persistent / Tracking
 *   39-40: Complex Structures
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
        // Projectile / Falling (31-32)
        registry.register(new DoomComet(plugin));
        registry.register(new LavaBombCluster(plugin));
        // Ground / Area Denial (33-34)
        registry.register(new VoidAnchor(plugin));
        registry.register(new CorruptionSpire(plugin));
        // Descending / Overhead (35-36)
        registry.register(new DemonicFist(plugin));
        registry.register(new BloodmoonCrescent(plugin));
        // Persistent / Tracking (37-38)
        registry.register(new HellLightningRod(plugin));
        registry.register(new ShadowStalker(plugin));
        // Complex Structures (39-40)
        registry.register(new InfernalWeb(plugin));
        registry.register(new DemonicWings(plugin));
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
    // 31. DOOM COMET -- "The Streaker" (14 displays)
    //     A blazing comet that streaks in a straight line across the arena.
    //     Head: 3 MAGMA_BLOCK (1 large, 2 smaller offset)
    //     Tail: 8 SHROOMLIGHT cubes decreasing size
    //     Tail glow: 3 ORANGE_CONCRETE flat slabs at tail positions
    //     Streaks 40 blocks in a straight line. Tumbling head, trailing tail.
    //     damage=10.0, radius=1.8, ticks-between-damage=5, duration=80
    // ================================================================
    public static class DoomComet extends BlockDisplayAttack {
        private Location center;
        private Location origin;
        private double dirX, dirZ;
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tailBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tailGlow = new ArrayList<>();
        private boolean dissipating = false;

        public DoomComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_comet", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(1.8);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(80);
            config.setCooldownTicks(160);
            config.setChance(9.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Random direction to streak
            double angle = Math.random() * 2 * Math.PI;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // Start 40 blocks away, streak toward and through center
            Location spawnLoc = center.clone().add(-dirX * 20, 4, -dirZ * 20);

            // Head: 1 large magma block + 2 smaller offset
            BlockDisplayHandle headMain = displayBuilder.spawnBlock(spawnLoc.clone(), Material.MAGMA_BLOCK);
            headMain.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 20).interpolation(3, 0);
            headMain.entity().setTeleportDuration(3);
            spawnedEntities.add(headMain.entity());
            headBlocks.add(headMain);

            BlockDisplayHandle headL = displayBuilder.spawnBlock(spawnLoc.clone().add(-dirZ * 0.3, 0, dirX * 0.3), Material.MAGMA_BLOCK);
            headL.scale(0.35f, 0.35f, 0.35f).glow(240, 80, 30).interpolation(3, 0);
            headL.entity().setTeleportDuration(3);
            spawnedEntities.add(headL.entity());
            headBlocks.add(headL);

            BlockDisplayHandle headR = displayBuilder.spawnBlock(spawnLoc.clone().add(dirZ * 0.3, 0, -dirX * 0.3), Material.MAGMA_BLOCK);
            headR.scale(0.35f, 0.35f, 0.35f).glow(240, 80, 30).interpolation(3, 0);
            headR.entity().setTeleportDuration(3);
            spawnedEntities.add(headR.entity());
            headBlocks.add(headR);

            // Tail: 8 SHROOMLIGHT cubes decreasing size at increasing offsets behind head
            float[] tailSizes = {0.5f, 0.42f, 0.34f, 0.26f, 0.20f, 0.15f, 0.10f, 0.07f};
            double[] tailOffsets = {0.5, 1.1, 1.8, 2.7, 3.6, 4.5, 5.3, 6.1};
            for (int i = 0; i < 8; i++) {
                Location tailLoc = spawnLoc.clone().add(-dirX * tailOffsets[i], 0, -dirZ * tailOffsets[i]);
                BlockDisplayHandle tail = displayBuilder.spawnBlock(tailLoc, Material.SHROOMLIGHT);
                tail.scale(tailSizes[i], tailSizes[i], tailSizes[i]).glow(255, 100, 20).interpolation(3, 0);
                tail.entity().setTeleportDuration(3);
                spawnedEntities.add(tail.entity());
                tailBlocks.add(tail);
            }

            // Tail glow: 3 flat slabs at tail positions 2, 4, 6
            int[] glowIndices = {1, 3, 5};
            for (int idx : glowIndices) {
                Location glowLoc = spawnLoc.clone().add(-dirX * tailOffsets[idx], -0.1, -dirZ * tailOffsets[idx]);
                BlockDisplayHandle glow = displayBuilder.spawnBlock(glowLoc, Material.ORANGE_CONCRETE);
                glow.scale(0.3f, 0.05f, 0.3f).glow(240, 80, 30).interpolation(3, 0);
                glow.entity().setTeleportDuration(3);
                spawnedEntities.add(glow.entity());
                tailGlow.add(glow);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Move all blocks forward by delta each tick
            double speed = 1.0; // blocks per 3 ticks (teleportDuration=3)
            double dx = dirX * speed;
            double dz = dirZ * speed;

            // Calculate current head position
            double headX = origin.getX() + (-dirX * 20) + dx * tick;
            double headZ = origin.getZ() + (-dirZ * 20) + dz * tick;
            double headY = origin.getY() + 4;
            Location headLoc = new Location(w, headX, headY, headZ);

            // Update damage center to head position
            setCenter(headLoc.clone());

            // Check if past 40 blocks from origin
            double distTraveled = tick * speed;
            if (distTraveled > 40) {
                dissipating = true;
            }

            // Move head blocks
            headBlocks.get(0).entity().teleport(headLoc.clone());
            headBlocks.get(1).entity().teleport(headLoc.clone().add(-dirZ * 0.3, 0, dirX * 0.3));
            headBlocks.get(2).entity().teleport(headLoc.clone().add(dirZ * 0.3, 0, -dirX * 0.3));

            // Head tumble on X axis
            float tumbleAngle = (float) Math.toRadians(tick * 15);
            for (BlockDisplayHandle head : headBlocks) {
                Transformation t = head.entity().getTransformation();
                head.entity().setInterpolationDuration(3);
                head.entity().setInterpolationDelay(0);
                head.entity().setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(tumbleAngle, 1f, 0f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Move tail blocks (trail behind head)
            double[] tailOffsets = {0.5, 1.1, 1.8, 2.7, 3.6, 4.5, 5.3, 6.1};
            for (int i = 0; i < tailBlocks.size(); i++) {
                Location tailLoc = headLoc.clone().add(-dirX * tailOffsets[i], 0, -dirZ * tailOffsets[i]);
                tailBlocks.get(i).entity().teleport(tailLoc);
            }

            // Move tail glow
            int[] glowIndices = {1, 3, 5};
            for (int i = 0; i < tailGlow.size(); i++) {
                Location glowLoc = headLoc.clone().add(-dirX * tailOffsets[glowIndices[i]], -0.1, -dirZ * tailOffsets[glowIndices[i]]);
                tailGlow.get(i).entity().teleport(glowLoc);
            }

            // Dissipate: scale everything to 0
            if (dissipating) {
                for (BlockDisplayHandle h : headBlocks) {
                    h.entity().setInterpolationDuration(10);
                    h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new Transformation(
                            h.entity().getTransformation().getTranslation(),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.01f, 0.01f, 0.01f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                for (BlockDisplayHandle t : tailBlocks) {
                    t.entity().setInterpolationDuration(10);
                    t.entity().setInterpolationDelay(0);
                    t.entity().setTransformation(new Transformation(
                            t.entity().getTransformation().getTranslation(),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.01f, 0.01f, 0.01f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                for (BlockDisplayHandle g : tailGlow) {
                    g.entity().setInterpolationDuration(10);
                    g.entity().setInterpolationDelay(0);
                    g.entity().setTransformation(new Transformation(
                            g.entity().getTransformation().getTranslation(),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.01f, 0.01f, 0.01f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Flame trail particles behind head
            if (tick % 2 == 0) {
                Location trailLoc = headLoc.clone().add(-dirX * 0.8, 0, -dirZ * 0.8);
                w.spawnParticle(Particle.FLAME, trailLoc, 4, 0.15, 0.15, 0.15, 0.02);
                DisplayBuilder.dustParticles(trailLoc, 3, 0.3, 255, 100, 20, 1.2f);
            }

            // Whoosh sound periodically
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(headLoc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomComet(plugin); }
    }

    // ================================================================
    // 32. LAVA BOMB CLUSTER -- "The Barrage" (10 displays: 2 per bomb x 5)
    //     5 bombs staggered 8 ticks apart, follow parabolic arcs to
    //     scattered targets around player. Each bomb: MAGMA_BLOCK + SHROOMLIGHT.
    //     Impact-only damage with explosion effect on landing.
    //     impact-damage=10.0, impact-radius=2.2, duration=120
    // ================================================================
    public static class LavaBombCluster extends BlockDisplayAttack {
        private Location center;
        private final BlockDisplayHandle[] bombOuter = new BlockDisplayHandle[5];
        private final BlockDisplayHandle[] bombInner = new BlockDisplayHandle[5];
        private final double[][] targetOffsets = new double[5][2];
        private final int[] spawnTick = new int[5];
        private final boolean[] launched = new boolean[5];
        private final boolean[] impacted = new boolean[5];
        private double playerX, playerZ;

        public LavaBombCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_bomb_cluster", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(2.2);
            config.setDurationTicks(120);
            config.setCooldownTicks(180);
            config.setChance(9.0);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Record player target position
            Player target = getTargetPlayer();
            if (target != null) {
                playerX = target.getLocation().getX();
                playerZ = target.getLocation().getZ();
            } else {
                playerX = center.getX();
                playerZ = center.getZ();
            }

            // Stagger bombs 8 ticks apart, scatter targets +-2 blocks around player
            for (int i = 0; i < 5; i++) {
                spawnTick[i] = i * 8;
                launched[i] = false;
                impacted[i] = false;
                targetOffsets[i][0] = (Math.random() - 0.5) * 4.0; // +-2 blocks X
                targetOffsets[i][1] = (Math.random() - 0.5) * 4.0; // +-2 blocks Z
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int i = 0; i < 5; i++) {
                if (impacted[i]) continue;

                // Launch bomb at its stagger time
                if (!launched[i] && tick >= spawnTick[i]) {
                    launched[i] = true;

                    // Spawn at Y+12 above center with slight X/Z scatter
                    double startX = center.getX() + (Math.random() - 0.5) * 3.0;
                    double startZ = center.getZ() + (Math.random() - 0.5) * 3.0;
                    Location bombLoc = new Location(w, startX, center.getY() + 12, startZ);

                    BlockDisplayHandle outer = displayBuilder.spawnBlock(bombLoc.clone(), Material.MAGMA_BLOCK);
                    outer.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 20).interpolation(3, 0);
                    outer.entity().setTeleportDuration(2);
                    spawnedEntities.add(outer.entity());
                    bombOuter[i] = outer;

                    BlockDisplayHandle inner = displayBuilder.spawnBlock(bombLoc.clone(), Material.SHROOMLIGHT);
                    inner.scale(0.3f, 0.3f, 0.3f).glow(240, 80, 30).interpolation(3, 0);
                    inner.entity().setTeleportDuration(2);
                    spawnedEntities.add(inner.entity());
                    bombInner[i] = inner;

                    DisplayBuilder.playSound(bombLoc, Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.7f);
                }

                // Animate launched bombs along parabolic arc
                if (launched[i] && bombOuter[i] != null) {
                    int bombTick = tick - spawnTick[i];
                    int flightDuration = 30; // 1.5 seconds to impact

                    if (bombTick <= flightDuration) {
                        double t = (double) bombTick / flightDuration;
                        double startX = bombOuter[i].entity().getLocation().getX();
                        double startZ = bombOuter[i].entity().getLocation().getZ();

                        // Use stored initial position for interpolation
                        if (bombTick == 1) {
                            startX = center.getX() + (i - 2) * 0.6;
                            startZ = center.getZ();
                        }

                        double targetX = playerX + targetOffsets[i][0];
                        double targetZ = playerZ + targetOffsets[i][1];

                        // Parabolic Y: starts at +12, peaks at +14, lands at 0
                        double y = center.getY() + 12 * (1 - t) + 4 * t * (1 - t) * 4;
                        // Simplified parabola
                        y = center.getY() + 12.0 - 12.0 * t + 2.0 * Math.sin(t * Math.PI);

                        double x = center.getX() + (i - 2) * 0.6 + (targetX - center.getX()) * t;
                        double z = center.getZ() + (targetZ - center.getZ()) * t;

                        Location newLoc = new Location(w, x, y, z);
                        newLoc.setYaw(0);
                        newLoc.setPitch(0);
                        bombOuter[i].entity().teleport(newLoc);
                        bombInner[i].entity().teleport(newLoc);

                        // Spin on Y axis
                        float spinAngle = (float) Math.toRadians(bombTick * 20);
                        Transformation bt = bombOuter[i].entity().getTransformation();
                        bombOuter[i].entity().setInterpolationDuration(2);
                        bombOuter[i].entity().setInterpolationDelay(0);
                        bombOuter[i].entity().setTransformation(new Transformation(
                                bt.getTranslation(),
                                new AxisAngle4f(spinAngle, 0f, 1f, 0f),
                                bt.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));

                        // Flame trail
                        if (bombTick % 2 == 0) {
                            w.spawnParticle(Particle.FLAME, newLoc, 2, 0.1, 0.1, 0.1, 0.02);
                        }
                    }

                    // Impact at end of flight
                    if (bombTick == flightDuration) {
                        impacted[i] = true;
                        double targetX = playerX + targetOffsets[i][0];
                        double targetZ = playerZ + targetOffsets[i][1];
                        Location impactLoc = new Location(w, targetX, center.getY(), targetZ);

                        // Expand to 0.8 then collapse
                        bombOuter[i].entity().setInterpolationDuration(5);
                        bombOuter[i].entity().setInterpolationDelay(0);
                        bombOuter[i].entity().setTransformation(new Transformation(
                                new Vector3f(-0.4f, -0.4f, -0.4f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.8f, 0.8f, 0.8f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bombInner[i].entity().setInterpolationDuration(5);
                        bombInner[i].entity().setInterpolationDelay(0);
                        bombInner[i].entity().setTransformation(new Transformation(
                                new Vector3f(-0.15f, -0.15f, -0.15f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.01f, 0.01f, 0.01f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));

                        // Explosion effects
                        w.spawnParticle(Particle.LAVA, impactLoc.clone().add(0, 0.5, 0), 25, 1.5, 0.5, 1.5, 0.05);
                        w.spawnParticle(Particle.FLAME, impactLoc.clone().add(0, 0.3, 0), 15, 1.0, 0.3, 1.0, 0.04);
                        w.spawnParticle(Particle.LARGE_SMOKE, impactLoc.clone().add(0, 1, 0), 10, 0.8, 0.5, 0.8, 0.02);
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

                        triggerImpactDamage(impactLoc);
                    }
                }
            }

            // Ambient rumble
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaBombCluster(plugin); }
    }

    // ================================================================
    // 33. VOID ANCHOR -- "The Weight" (12 displays)
    //     A heavy cross-shaped anchor falls from the sky with acceleration,
    //     impacts the ground for massive damage, then stays embedded as
    //     area denial with constant damage radius.
    //     Hybrid: impact damage on landing + constant radius while embedded.
    //     impact-damage=16.0, impact-radius=3.0, damage=4.0, radius=2.0
    // ================================================================
    public static class VoidAnchor extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> allParts = new ArrayList<>();
        private double currentY;
        private double fallSpeed;
        private boolean embedded = false;
        private int embedTick = 0;

        public VoidAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_anchor", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(0);
            config.setDamageOnImpactOnly(false); // Hybrid -- we handle impact manually
            config.setImpactDamage(16.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setChance(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            currentY = 15.0;
            fallSpeed = 0.1;

            Location spawnBase = center.clone().add(0, currentY, 0);

            // Cross beam: 2 horizontal BLACKSTONE bars
            BlockDisplayHandle beamX = displayBuilder.spawnBlock(spawnBase.clone(), Material.BLACKSTONE);
            beamX.scale(1.2f, 0.3f, 0.3f).glow(20, 10, 5).interpolation(3, 0);
            beamX.entity().setTeleportDuration(2);
            spawnedEntities.add(beamX.entity());
            allParts.add(beamX);

            BlockDisplayHandle beamZ = displayBuilder.spawnBlock(spawnBase.clone(), Material.BLACKSTONE);
            beamZ.scale(0.3f, 0.3f, 1.2f).glow(20, 10, 5).interpolation(3, 0);
            beamZ.entity().setTeleportDuration(2);
            spawnedEntities.add(beamZ.entity());
            allParts.add(beamZ);

            // Vertical shaft: tall BLACK_CONCRETE
            BlockDisplayHandle shaft = displayBuilder.spawnBlock(spawnBase.clone().add(0, -1.25, 0), Material.BLACK_CONCRETE);
            shaft.scale(0.2f, 2.5f, 0.2f).glow(10, 5, 15).interpolation(3, 0);
            shaft.entity().setTeleportDuration(2);
            spawnedEntities.add(shaft.entity());
            allParts.add(shaft);

            // Curved arms: 2 OBSIDIAN at +-45 deg Z at bottom of shaft
            BlockDisplayHandle armL = displayBuilder.spawnBlock(spawnBase.clone().add(-0.4, -2.3, 0), Material.OBSIDIAN);
            armL.scale(0.5f, 0.2f, 0.2f).rotate((float) Math.toRadians(45), 0f, 0f, 1f).glow(20, 10, 5).interpolation(3, 0);
            armL.entity().setTeleportDuration(2);
            spawnedEntities.add(armL.entity());
            allParts.add(armL);

            BlockDisplayHandle armR = displayBuilder.spawnBlock(spawnBase.clone().add(0.4, -2.3, 0), Material.OBSIDIAN);
            armR.scale(0.5f, 0.2f, 0.2f).rotate((float) Math.toRadians(-45), 0f, 0f, 1f).glow(20, 10, 5).interpolation(3, 0);
            armR.entity().setTeleportDuration(2);
            spawnedEntities.add(armR.entity());
            allParts.add(armR);

            // Top ring: 4 CRYING_OBSIDIAN small cubes in ring around cross junction
            double ringRadius = 0.5;
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4.0 + Math.PI / 4;
                double rx = Math.cos(angle) * ringRadius;
                double rz = Math.sin(angle) * ringRadius;
                BlockDisplayHandle ring = displayBuilder.spawnBlock(spawnBase.clone().add(rx, 0.2, rz), Material.CRYING_OBSIDIAN);
                ring.scale(0.2f, 0.2f, 0.2f).glow(120, 20, 80).interpolation(3, 0);
                ring.entity().setTeleportDuration(2);
                spawnedEntities.add(ring.entity());
                allParts.add(ring);
            }

            // 1 sentinel indicator particle
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 8, 1.0, 120, 20, 80, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            if (!embedded) {
                // Accelerating fall
                fallSpeed += 0.08;
                currentY -= fallSpeed;

                if (currentY <= 0) {
                    currentY = 0;
                    embedded = true;
                    embedTick = tick;

                    // Impact effects
                    Location impactLoc = center.clone();
                    w.spawnParticle(Particle.EXPLOSION, impactLoc, 3, 0.5, 0.2, 0.5, 0);
                    w.spawnParticle(Particle.LARGE_SMOKE, impactLoc.clone().add(0, 0.5, 0), 20, 1.5, 0.5, 1.5, 0.03);
                    DisplayBuilder.dustParticles(impactLoc, 30, 2.0, 120, 20, 80, 2.0f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.3f);

                    // Impact damage (manual trigger since we use hybrid model)
                    triggerImpactDamage(impactLoc);
                }

                // Move all parts to current Y
                Location base = center.clone().add(0, currentY, 0);
                double[][] offsets = {
                        {0, 0, 0},           // beamX
                        {0, 0, 0},           // beamZ
                        {0, -1.25, 0},       // shaft
                        {-0.4, -2.3, 0},     // armL
                        {0.4, -2.3, 0},      // armR
                };
                for (int i = 0; i < 5 && i < allParts.size(); i++) {
                    Location loc = base.clone().add(offsets[i][0], offsets[i][1], offsets[i][2]);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    allParts.get(i).entity().teleport(loc);
                }
                // Ring pieces (indices 5-8)
                double ringRadius = 0.5;
                for (int i = 0; i < 4 && (i + 5) < allParts.size(); i++) {
                    double angle = (2 * Math.PI * i) / 4.0 + Math.PI / 4;
                    double rx = Math.cos(angle) * ringRadius;
                    double rz = Math.sin(angle) * ringRadius;
                    Location loc = base.clone().add(rx, 0.2, rz);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    allParts.get(i + 5).entity().teleport(loc);
                }

                // Falling warning shadow particle on ground
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 5, 1.0, 10, 5, 15, 1.5f);
                }
            } else {
                // Embedded: area denial with constant damage
                // Update center to ground level for damage radius
                setCenter(center.clone());

                // Pulse particles around base
                if (tick % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0), 6, 1.5, 120, 20, 80, 1.0f);
                }

                // Ambient void hum
                if (tick % 40 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.3f);
                }

                // Crying obsidian drip particles from ring
                if (tick % 8 == 0) {
                    double ringRad = 0.5;
                    int idx = (tick / 8) % 4;
                    double angle = (2 * Math.PI * idx) / 4.0 + Math.PI / 4;
                    Location dropLoc = center.clone().add(Math.cos(angle) * ringRad, 0.3, Math.sin(angle) * ringRad);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, dropLoc, 3, 0.1, 0.1, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new VoidAnchor(plugin); }
    }

    // ================================================================
    // 34. CORRUPTION SPIRE -- "The Growth" (13 displays)
    //     Grows segment-by-segment from the ground like a twisted spire.
    //     8 SCULK segments decreasing size, each Y-rotated 15 deg.
    //     4 AMETHYST_BLOCK shard protrusions at mid-height.
    //     1 SCULK_CATALYST cap at top.
    //     Pulses in idle, shatters top-down on dissipate.
    //     damage=5.0, radius=1.5, ticks-between-damage=14, delay=30, duration=180
    // ================================================================
    public static class CorruptionSpire extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private BlockDisplayHandle cap;
        private int segmentsRevealed = 0;
        private boolean shattering = false;
        private int shatterTick = 0;

        public CorruptionSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_spire", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(14);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(180);
            config.setCooldownTicks(160);
            config.setChance(9.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 8 SCULK segments, decreasing size, each Y-rotated 15 deg from previous
            float[] segSizes = {0.55f, 0.50f, 0.45f, 0.40f, 0.35f, 0.28f, 0.20f, 0.12f};
            double yPos = 0;
            for (int i = 0; i < 8; i++) {
                Location segLoc = center.clone().add(0, yPos, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.SCULK);
                float s = segSizes[i];
                seg.scale(s, s, s);
                seg.rotate((float) Math.toRadians(15 * i), 0f, 1f, 0f);
                seg.glow(10, 80, 80).interpolation(5, 0);
                spawnedEntities.add(seg.entity());
                segments.add(seg);

                // Start invisible (scale 0), will grow in onTick
                seg.entity().setTransformation(new Transformation(
                        seg.entity().getTransformation().getTranslation(),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.01f, 0.01f, 0.01f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));

                yPos += s * 0.8; // Stack segments
            }

            // 4 AMETHYST_BLOCK shard protrusions at mid-height (segments 3-4 area)
            double shardY = 0;
            for (int i = 0; i < 4; i++) shardY += segSizes[i] * 0.8;
            shardY -= segSizes[3] * 0.4; // mid of segment 3-4

            for (int i = 0; i < 4; i++) {
                double shardAngle = (2 * Math.PI * i) / 4.0;
                double ox = Math.cos(shardAngle) * 0.35;
                double oz = Math.sin(shardAngle) * 0.35;
                Location shardLoc = center.clone().add(ox, shardY, oz);
                BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.AMETHYST_BLOCK);
                shard.scale(0.01f, 0.01f, 0.01f); // Start invisible
                shard.rotate((float) shardAngle, 0f, 1f, 0f);
                shard.glow(160, 80, 200).interpolation(5, 0);
                spawnedEntities.add(shard.entity());
                shards.add(shard);
            }

            // SCULK_CATALYST cap
            double capY = 0;
            for (float sz : segSizes) capY += sz * 0.8;
            Location capLoc = center.clone().add(0, capY, 0);
            cap = displayBuilder.spawnBlock(capLoc, Material.SCULK_CATALYST);
            cap.scale(0.01f, 0.01f, 0.01f); // Start invisible
            cap.glow(10, 120, 100).interpolation(5, 0);
            spawnedEntities.add(cap.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            float[] segSizes = {0.55f, 0.50f, 0.45f, 0.40f, 0.35f, 0.28f, 0.20f, 0.12f};

            // Growth phase: reveal segments one by one (every ~3 ticks for 25 total)
            if (tick <= 25 && !shattering) {
                int targetRevealed = Math.min(8, (tick * 8) / 25 + 1);
                while (segmentsRevealed < targetRevealed && segmentsRevealed < segments.size()) {
                    int idx = segmentsRevealed;
                    float s = segSizes[idx];
                    segments.get(idx).entity().setInterpolationDuration(5);
                    segments.get(idx).entity().setInterpolationDelay(0);
                    segments.get(idx).entity().setTransformation(new Transformation(
                            segments.get(idx).entity().getTransformation().getTranslation(),
                            new AxisAngle4f((float) Math.toRadians(15 * idx), 0f, 1f, 0f),
                            new Vector3f(s, s, s),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    segmentsRevealed++;

                    DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_BREAK, 0.6f, 0.8f + idx * 0.1f);
                }

                // Reveal shards at segment 4-5
                if (segmentsRevealed >= 5 && tick > 15) {
                    for (BlockDisplayHandle shard : shards) {
                        Transformation st = shard.entity().getTransformation();
                        if (st.getScale().x < 0.05f) {
                            shard.entity().setInterpolationDuration(8);
                            shard.entity().setInterpolationDelay(0);
                            shard.entity().setTransformation(new Transformation(
                                    st.getTranslation(),
                                    new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(0.1f, 0.35f, 0.1f),
                                    new AxisAngle4f(0, 0, 1, 0)
                            ));
                        }
                    }
                }

                // Reveal cap at end
                if (segmentsRevealed >= 8) {
                    Transformation ct = cap.entity().getTransformation();
                    if (ct.getScale().x < 0.05f) {
                        cap.entity().setInterpolationDuration(8);
                        cap.entity().setInterpolationDelay(0);
                        cap.entity().setTransformation(new Transformation(
                                ct.getTranslation(),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.25f, 0.25f, 0.25f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Idle phase: pulse/throb (scale oscillation)
            if (tick > 25 && tick < config.getDurationTicks() - 20 && !shattering) {
                if (tick % 15 == 0) {
                    float pulse = 1.0f + 0.08f * (float) Math.sin(tick * 0.1);
                    for (int i = 0; i < segments.size(); i++) {
                        float s = segSizes[i] * pulse;
                        segments.get(i).entity().setInterpolationDuration(10);
                        segments.get(i).entity().setInterpolationDelay(0);
                        segments.get(i).entity().setTransformation(new Transformation(
                                segments.get(i).entity().getTransformation().getTranslation(),
                                new AxisAngle4f((float) Math.toRadians(15 * i), 0f, 1f, 0f),
                                new Vector3f(s, s, s),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }

                // Sculk particles
                if (tick % 8 == 0) {
                    w.spawnParticle(Particle.SCULK_CHARGE_POP, center.clone().add(0, 1.2, 0), 3, 0.3, 0.5, 0.3, 0.01);
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.8, 0), 2, 0.4, 10, 80, 80, 1.0f);
                }

                // Ambient sound
                if (tick % 50 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.4f);
                }
            }

            // Shatter phase: collapse top-down in final 20 ticks
            if (tick >= config.getDurationTicks() - 20) {
                if (!shattering) {
                    shattering = true;
                    shatterTick = tick;
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.5f);
                }

                int sTick = tick - shatterTick;
                // Shatter from top to bottom: cap first, then segments 7 down to 0
                int shatterIndex = sTick / 2; // one piece every 2 ticks

                // Cap shatters first
                if (shatterIndex == 0) {
                    cap.entity().setInterpolationDuration(3);
                    cap.entity().setInterpolationDelay(0);
                    cap.entity().setTransformation(new Transformation(
                            cap.entity().getTransformation().getTranslation(),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.01f, 0.01f, 0.01f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Shards shatter at index 1
                if (shatterIndex == 1) {
                    for (BlockDisplayHandle shard : shards) {
                        shard.entity().setInterpolationDuration(3);
                        shard.entity().setInterpolationDelay(0);
                        shard.entity().setTransformation(new Transformation(
                                shard.entity().getTransformation().getTranslation(),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.01f, 0.01f, 0.01f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }

                // Segments shatter top-down (7, 6, 5, ... 0)
                int segIdx = 7 - (shatterIndex - 2);
                if (shatterIndex >= 2 && segIdx >= 0 && segIdx < segments.size()) {
                    segments.get(segIdx).entity().setInterpolationDuration(3);
                    segments.get(segIdx).entity().setInterpolationDelay(0);
                    segments.get(segIdx).entity().setTransformation(new Transformation(
                            segments.get(segIdx).entity().getTransformation().getTranslation(),
                            segments.get(segIdx).entity().getTransformation().getLeftRotation(),
                            new Vector3f(0.01f, 0.01f, 0.01f),
                            segments.get(segIdx).entity().getTransformation().getRightRotation()
                    ));

                    // Shatter particle
                    double shatterY = 0;
                    for (int j = 0; j < segIdx; j++) shatterY += segSizes[j] * 0.8;
                    DisplayBuilder.dustParticles(center.clone().add(0, shatterY, 0), 5, 0.3, 10, 80, 80, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSpire(plugin); }
    }

    // ================================================================
    // 35. DEMONIC FIST -- "The Smite" (15 displays)
    //     A massive clenched fist descends from above and punches through
    //     the ground plane. Impact-only, massive radius, very rare.
    //     Palm: 3 BLACKSTONE. Knuckles: 4 NETHERRACK rows.
    //     Thumb: 1 angled NETHERRACK. Wrist: 1 BLACKSTONE tall.
    //     Plus 5 knuckle detail blocks.
    //     impact-damage=28.0, impact-radius=5.0, duration=60, chance=2
    // ================================================================
    public static class DemonicFist extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> allParts = new ArrayList<>();
        private double currentY;
        private double fallSpeed;
        private boolean impactDone = false;
        private boolean lifting = false;
        private int liftTick = 0;

        public DemonicFist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demonic_fist", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
            config.setChance(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            currentY = 12.0;
            fallSpeed = 0.15;

            Location base = center.clone().add(0, currentY, 0);

            // Palm: 3 BLACKSTONE cubes in a row (wide palm)
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle palm = displayBuilder.spawnBlock(base.clone().add(i * 0.65, 0, 0), Material.BLACKSTONE);
                palm.scale(0.7f, 0.3f, 0.6f).glow(20, 10, 5).interpolation(3, 0);
                palm.entity().setTeleportDuration(1);
                spawnedEntities.add(palm.entity());
                allParts.add(palm);
            }

            // 4 knuckle rows of NETHERRACK across the top
            for (int i = 0; i < 4; i++) {
                double kx = -0.75 + i * 0.5;
                BlockDisplayHandle knuckle = displayBuilder.spawnBlock(base.clone().add(kx, 0.35, -0.05), Material.NETHERRACK);
                knuckle.scale(0.3f, 0.4f, 0.5f).glow(200, 50, 10).interpolation(3, 0);
                knuckle.entity().setTeleportDuration(1);
                spawnedEntities.add(knuckle.entity());
                allParts.add(knuckle);
            }

            // 5 knuckle detail blocks (smaller bumps on top of knuckles)
            for (int i = 0; i < 4; i++) {
                double kx = -0.75 + i * 0.5;
                BlockDisplayHandle detail = displayBuilder.spawnBlock(base.clone().add(kx, 0.6, 0), Material.NETHERRACK);
                detail.scale(0.2f, 0.15f, 0.35f).glow(240, 80, 30).interpolation(3, 0);
                detail.entity().setTeleportDuration(1);
                spawnedEntities.add(detail.entity());
                allParts.add(detail);
            }

            // Extra knuckle detail
            BlockDisplayHandle knuckleTop = displayBuilder.spawnBlock(base.clone().add(0, 0.7, -0.1), Material.NETHERRACK);
            knuckleTop.scale(1.2f, 0.1f, 0.3f).glow(200, 50, 10).interpolation(3, 0);
            knuckleTop.entity().setTeleportDuration(1);
            spawnedEntities.add(knuckleTop.entity());
            allParts.add(knuckleTop);

            // Thumb: 1 angled NETHERRACK
            BlockDisplayHandle thumb = displayBuilder.spawnBlock(base.clone().add(-1.0, 0.1, 0.2), Material.NETHERRACK);
            thumb.scale(0.25f, 0.3f, 0.3f).rotate((float) Math.toRadians(30), 0f, 0f, 1f)
                    .glow(200, 50, 10).interpolation(3, 0);
            thumb.entity().setTeleportDuration(1);
            spawnedEntities.add(thumb.entity());
            allParts.add(thumb);

            // Wrist: 1 BLACKSTONE tall block
            BlockDisplayHandle wrist = displayBuilder.spawnBlock(base.clone().add(0, 1.0, 0.1), Material.BLACKSTONE);
            wrist.scale(0.5f, 0.8f, 0.4f).glow(20, 10, 5).interpolation(3, 0);
            wrist.entity().setTeleportDuration(1);
            spawnedEntities.add(wrist.entity());
            allParts.add(wrist);

            // Warning shadow on ground
            DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 20, 3.0, 200, 50, 10, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            if (!impactDone) {
                // Accelerating descent
                fallSpeed += 0.25;
                currentY -= fallSpeed;

                if (currentY <= -1.5) { // Punches through ground plane
                    currentY = -1.5;
                    impactDone = true;

                    // Massive impact effects
                    Location impactLoc = center.clone();
                    w.spawnParticle(Particle.EXPLOSION, impactLoc, 5, 2, 0.5, 2, 0);
                    w.spawnParticle(Particle.LARGE_SMOKE, impactLoc.clone().add(0, 0.5, 0), 40, 3, 1, 3, 0.04);
                    w.spawnParticle(Particle.LAVA, impactLoc.clone().add(0, 0.3, 0), 30, 3, 0.3, 3, 0.1);
                    DisplayBuilder.dustParticles(impactLoc, 50, 4.0, 200, 50, 10, 2.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.2f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);

                    triggerImpactDamage(impactLoc);
                }

                // Move all parts to current Y
                moveAllParts(currentY);

                // Warning particles on ground
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 8, 2.5, 200, 50, 10, 1.5f);
                }
            } else if (!lifting) {
                // Brief pause embedded in ground (10 ticks)
                if (tick > 30 || (impactDone && tick - liftTick > 10)) {
                    lifting = true;
                    liftTick = tick;
                }
                if (!lifting && liftTick == 0) {
                    liftTick = tick;
                }
            } else {
                // Lift back up and scale to 0
                int lt = tick - liftTick;
                currentY += 0.4;
                moveAllParts(currentY);

                // Scale down as it lifts
                if (lt > 5) {
                    float scale = Math.max(0.01f, 1.0f - (lt - 5) * 0.07f);
                    for (BlockDisplayHandle part : allParts) {
                        Transformation t = part.entity().getTransformation();
                        Vector3f origScale = t.getScale();
                        part.entity().setInterpolationDuration(3);
                        part.entity().setInterpolationDelay(0);
                        part.entity().setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(origScale.x * scale, origScale.y * scale, origScale.z * scale),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }
        }

        private void moveAllParts(double y) {
            if (allParts.isEmpty()) return;
            Location base = center.clone().add(0, y, 0);

            // Part positions relative to base (same order as spawn)
            double[][] offsets = {
                    {-0.65, 0, 0}, {0, 0, 0}, {0.65, 0, 0},         // palm (3)
                    {-0.75, 0.35, -0.05}, {-0.25, 0.35, -0.05},     // knuckles (4)
                    {0.25, 0.35, -0.05}, {0.75, 0.35, -0.05},
                    {-0.75, 0.6, 0}, {-0.25, 0.6, 0},               // knuckle details (4)
                    {0.25, 0.6, 0}, {0.75, 0.6, 0},
                    {0, 0.7, -0.1},                                   // knuckle top
                    {-1.0, 0.1, 0.2},                                 // thumb
                    {0, 1.0, 0.1},                                    // wrist
            };

            for (int i = 0; i < allParts.size() && i < offsets.length; i++) {
                Location loc = base.clone().add(offsets[i][0], offsets[i][1], offsets[i][2]);
                loc.setYaw(0);
                loc.setPitch(0);
                allParts.get(i).entity().teleport(loc);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DemonicFist(plugin); }
    }

    // ================================================================
    // 36. BLOODMOON CRESCENT -- "The Lunar Curse" (14 displays)
    //     A massive crescent moon hangs overhead at Y+8, slowly rotating.
    //     8 RED_CONCRETE outer cubes, 5 DARK_RED inner (crescent cutout),
    //     1 glow core. Rains particles downward. Very long duration,
    //     whole arena is hazard. Rotates on Z axis.
    //     damage=2.0, radius=8.0, ticks-between-damage=20, duration=400
    // ================================================================
    public static class BloodmoonCrescent extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerCutout = new ArrayList<>();

        public BloodmoonCrescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bloodmoon_crescent", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setChance(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Location moonCenter = center.clone().add(0, 8, 0);

            // 8 RED_CONCRETE cubes forming an outer circle (radius 2.0)
            double outerRadius = 2.0;
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8.0;
                double x = Math.cos(angle) * outerRadius;
                double z = Math.sin(angle) * outerRadius;
                BlockDisplayHandle block = displayBuilder.spawnBlock(moonCenter.clone().add(x, z, 0), Material.RED_CONCRETE);
                block.scale(0.9f, 0.9f, 0.4f).glow(180, 20, 20).interpolation(5, 0);
                spawnedEntities.add(block.entity());
                outerRing.add(block);
            }

            // 5 DARK_RED inner blocks offset to one side to create crescent shape
            // These overlap with the right side of the circle to "cut out" a portion
            double innerRadius = 1.3;
            double cutoutOffsetX = 0.8; // Shift the cutout circle to the right
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5.0 - Math.PI * 0.3;
                double x = Math.cos(angle) * innerRadius + cutoutOffsetX;
                double z = Math.sin(angle) * innerRadius;
                BlockDisplayHandle block = displayBuilder.spawnBlock(moonCenter.clone().add(x, z, 0), Material.RED_TERRACOTTA);
                block.scale(0.8f, 0.8f, 0.5f).glow(120, 10, 10).interpolation(5, 0);
                spawnedEntities.add(block.entity());
                innerCutout.add(block);
            }

            // Core glow at center
            BlockDisplayHandle glow = displayBuilder.spawnBlock(moonCenter.clone(), Material.SHROOMLIGHT);
            glow.scale(0.5f, 0.5f, 0.3f).glow(200, 40, 40).interpolation(5, 0);
            spawnedEntities.add(glow.entity());
            outerRing.add(glow); // Track with outer ring for rotation

            DisplayBuilder.playSound(center, Sound.AMBIENT_CRIMSON_FOREST_MOOD, 1.0f, 0.3f);
            w.spawnParticle(Particle.DUST, moonCenter, 20, 2, 2, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 20, 20), 2.0f));
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            Location moonCenter = center.clone().add(0, 8, 0);

            // Slow rotation: update all block positions rotating around the Z axis of moonCenter
            double rotAngle = tick * 0.008; // Very slow rotation

            // Outer ring rotation
            double outerRadius = 2.0;
            for (int i = 0; i < 8 && i < outerRing.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 8.0 + rotAngle;
                double x = Math.cos(baseAngle) * outerRadius;
                double y = Math.sin(baseAngle) * outerRadius;
                Location loc = moonCenter.clone().add(x, y, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                outerRing.get(i).entity().teleport(loc);
            }

            // Core glow (last in outerRing) stays at center
            if (outerRing.size() > 8) {
                Location coreLoc = moonCenter.clone();
                coreLoc.setYaw(0);
                coreLoc.setPitch(0);
                outerRing.get(8).entity().teleport(coreLoc);
            }

            // Inner cutout rotation (same angle)
            double innerRadius = 1.3;
            double cutoutOffsetX = 0.8;
            for (int i = 0; i < innerCutout.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 5.0 - Math.PI * 0.3 + rotAngle;
                double rawX = Math.cos(baseAngle) * innerRadius + cutoutOffsetX;
                double rawY = Math.sin(baseAngle) * innerRadius;
                // Rotate the cutout offset too
                double cosR = Math.cos(rotAngle);
                double sinR = Math.sin(rotAngle);
                double finalX = rawX * cosR - rawY * sinR;
                double finalY = rawX * sinR + rawY * cosR;

                // Simplified: just offset and rotate the whole position
                double cutBaseAngle = (2 * Math.PI * i) / 5.0 - Math.PI * 0.3 + rotAngle;
                double cx = Math.cos(cutBaseAngle) * innerRadius;
                double cy = Math.sin(cutBaseAngle) * innerRadius;
                // Apply cutout shift in rotated frame
                cx += Math.cos(rotAngle) * cutoutOffsetX;
                cy += Math.sin(rotAngle) * cutoutOffsetX;

                Location loc = moonCenter.clone().add(cx, cy, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                innerCutout.get(i).entity().teleport(loc);
            }

            // Particle rain downward from moon to ground
            if (tick % 3 == 0) {
                double rx = (Math.random() - 0.5) * 5.0;
                double rz = (Math.random() - 0.5) * 5.0;
                Location rainStart = moonCenter.clone().add(rx, -1, rz);
                w.spawnParticle(Particle.DUST, rainStart, 1, 0, 3, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.5f));
                w.spawnParticle(Particle.DRIPPING_LAVA, rainStart, 1, 0.5, 0, 0.5, 0);
            }

            // Ground-level blood particles
            if (tick % 6 == 0) {
                double gx = (Math.random() - 0.5) * 8.0;
                double gz = (Math.random() - 0.5) * 8.0;
                DisplayBuilder.dustParticles(center.clone().add(gx, 0.2, gz), 2, 0.3, 180, 20, 20, 1.0f);
            }

            // Ambient sound
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CRIMSON_FOREST_MOOD, 0.6f, 0.2f);
            }

            // Eerie whisper
            if (tick % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BloodmoonCrescent(plugin); }
    }

    // ================================================================
    // 37. HELL LIGHTNING ROD -- "The Conductor" (12 displays)
    //     A tall iron rod with crossbars that periodically summons
    //     lightning strikes at its base. Spark cubes burst outward
    //     and a ground magma ring pulses from base.
    //     Impact-only (lightning strikes every 20 ticks).
    //     impact-damage=12.0, impact-radius=3.0, duration=160
    // ================================================================
    public static class HellLightningRod extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle rod;
        private BlockDisplayHandle tip;
        private final List<BlockDisplayHandle> crossbars = new ArrayList<>();
        private BlockDisplayHandle lightningRodBlock;
        private final List<BlockDisplayHandle> sparks = new ArrayList<>();

        public HellLightningRod(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hell_lightning_rod", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(200);
            config.setChance(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Main shaft: CHAIN block (tall thin rod)
            rod = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.CHAIN);
            rod.scale(0.1f, 3.0f, 0.1f).glow(180, 180, 200).interpolation(3, 0);
            spawnedEntities.add(rod.entity());

            // Iron tip at top
            tip = displayBuilder.spawnBlock(center.clone().add(0, 3.0, 0), Material.IRON_BLOCK);
            tip.scale(0.3f, 0.3f, 0.3f).glow(200, 200, 220).interpolation(3, 0);
            spawnedEntities.add(tip.entity());

            // 2 crossbars at 1/3 and 2/3 height
            for (int i = 1; i <= 2; i++) {
                double barY = i;
                BlockDisplayHandle barX = displayBuilder.spawnBlock(center.clone().add(0, barY, 0), Material.IRON_BLOCK);
                barX.scale(0.5f, 0.05f, 0.05f).glow(180, 180, 200).interpolation(3, 0);
                spawnedEntities.add(barX.entity());
                crossbars.add(barX);
            }

            // Lightning rod block at apex
            lightningRodBlock = displayBuilder.spawnBlock(center.clone().add(0, 3.3, 0), Material.LIGHTNING_ROD);
            lightningRodBlock.scale(0.3f, 0.4f, 0.3f).glow(220, 220, 240).interpolation(3, 0);
            spawnedEntities.add(lightningRodBlock.entity());

            // 6 spark cubes around tip (start small, burst outward on lightning)
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6.0;
                double sx = Math.cos(angle) * 0.3;
                double sz = Math.sin(angle) * 0.3;
                BlockDisplayHandle spark = displayBuilder.spawnBlock(center.clone().add(sx, 3.2, sz), Material.SHROOMLIGHT);
                spark.scale(0.08f, 0.08f, 0.08f).glow(255, 255, 200).interpolation(3, 0);
                spark.entity().setTeleportDuration(3);
                spawnedEntities.add(spark.entity());
                sparks.add(spark);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Every 20 ticks: lightning strike cycle
            int cycleTick = tick % 20;

            if (cycleTick == 0 && tick > 0) {
                // Lightning strike at base
                w.strikeLightningEffect(center.clone());
                triggerImpactDamage(center.clone());

                // Spark cubes burst outward
                for (int i = 0; i < sparks.size(); i++) {
                    double angle = (2 * Math.PI * i) / 6.0;
                    double burstX = Math.cos(angle) * 2.0;
                    double burstZ = Math.sin(angle) * 2.0;
                    Location burstLoc = center.clone().add(burstX, 3.2 + Math.random(), burstZ);
                    burstLoc.setYaw(0);
                    burstLoc.setPitch(0);
                    sparks.get(i).entity().teleport(burstLoc);

                    // Scale up spark during burst
                    sparks.get(i).entity().setInterpolationDuration(5);
                    sparks.get(i).entity().setInterpolationDelay(0);
                    sparks.get(i).entity().setTransformation(new Transformation(
                            sparks.get(i).entity().getTransformation().getTranslation(),
                            sparks.get(i).entity().getTransformation().getLeftRotation(),
                            new Vector3f(0.2f, 0.2f, 0.2f),
                            sparks.get(i).entity().getTransformation().getRightRotation()
                    ));
                }

                // Ground ring of magma particles pulsing outward
                for (int r = 0; r < 3; r++) {
                    double ringRad = 0.5 + r * 1.0;
                    int points = 8 + r * 4;
                    for (int p = 0; p < points; p++) {
                        double a = (2 * Math.PI * p) / points;
                        Location ringLoc = center.clone().add(Math.cos(a) * ringRad, 0.1, Math.sin(a) * ringRad);
                        DisplayBuilder.dustParticles(ringLoc, 1, 0.1, 255, 100, 20, 1.5f);
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.6f);
            }

            // Return sparks to default position between strikes
            if (cycleTick == 10) {
                for (int i = 0; i < sparks.size(); i++) {
                    double angle = (2 * Math.PI * i) / 6.0;
                    double sx = Math.cos(angle) * 0.3;
                    double sz = Math.sin(angle) * 0.3;
                    Location sparkLoc = center.clone().add(sx, 3.2, sz);
                    sparkLoc.setYaw(0);
                    sparkLoc.setPitch(0);
                    sparks.get(i).entity().teleport(sparkLoc);

                    // Scale back down
                    sparks.get(i).entity().setInterpolationDuration(5);
                    sparks.get(i).entity().setInterpolationDelay(0);
                    sparks.get(i).entity().setTransformation(new Transformation(
                            sparks.get(i).entity().getTransformation().getTranslation(),
                            sparks.get(i).entity().getTransformation().getLeftRotation(),
                            new Vector3f(0.08f, 0.08f, 0.08f),
                            sparks.get(i).entity().getTransformation().getRightRotation()
                    ));
                }
            }

            // Ambient electrical crackle
            if (tick % 7 == 0) {
                Location tipLoc = center.clone().add(0, 3.3, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, tipLoc, 3, 0.2, 0.2, 0.2, 0.05);
            }

            // Rod glow pulse
            if (tick % 15 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 3, 0.15, 200, 200, 240, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HellLightningRod(plugin); }
    }

    // ================================================================
    // 38. SHADOW STALKER -- "The Pursuer" (8 displays)
    //     A flat shadow on the ground that tracks the player. When close,
    //     it rears up (Y scale increases). Constant damage while stood on.
    //     Body: 1 GRAY_CONCRETE flat. Arms: 2 BLACK_CONCRETE thin.
    //     Head: 1 BLACK_CONCRETE blob. Extra tendrils: 4 small wisps.
    //     damage=4.0, radius=1.5, ticks-between-damage=10, tracks-player=true
    // ================================================================
    public static class ShadowStalker extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle body;
        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private BlockDisplayHandle head;
        private final List<BlockDisplayHandle> tendrils = new ArrayList<>();
        private boolean rearing = false;
        private int rearTick = 0;
        private Location lastTrackLoc;

        public ShadowStalker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_stalker", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(200);
            config.setCooldownTicks(150);
            config.setChance(10.0);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.lastTrackLoc = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Body: flat GRAY_CONCRETE
            body = displayBuilder.spawnBlock(center.clone(), Material.GRAY_CONCRETE);
            body.scale(1.8f, 0.05f, 1.2f).glow(30, 30, 30).interpolation(5, 0);
            body.entity().setTeleportDuration(3);
            spawnedEntities.add(body.entity());

            // Arms: 2 BLACK_CONCRETE thin slabs extending from sides
            BlockDisplayHandle armL = displayBuilder.spawnBlock(center.clone().add(-1.2, 0, 0), Material.BLACK_CONCRETE);
            armL.scale(0.8f, 0.04f, 0.4f).glow(10, 10, 10).interpolation(5, 0);
            armL.entity().setTeleportDuration(3);
            spawnedEntities.add(armL.entity());
            arms.add(armL);

            BlockDisplayHandle armR = displayBuilder.spawnBlock(center.clone().add(1.2, 0, 0), Material.BLACK_CONCRETE);
            armR.scale(0.8f, 0.04f, 0.4f).glow(10, 10, 10).interpolation(5, 0);
            armR.entity().setTeleportDuration(3);
            spawnedEntities.add(armR.entity());
            arms.add(armR);

            // Head: BLACK_CONCRETE blob at front
            head = displayBuilder.spawnBlock(center.clone().add(0, 0, -0.7), Material.BLACK_CONCRETE);
            head.scale(0.6f, 0.04f, 0.6f).glow(5, 5, 5).interpolation(5, 0);
            head.entity().setTeleportDuration(3);
            spawnedEntities.add(head.entity());

            // 4 tendrils (small wisps trailing behind)
            for (int i = 0; i < 4; i++) {
                double tx = (i - 1.5) * 0.4;
                BlockDisplayHandle tendril = displayBuilder.spawnBlock(center.clone().add(tx, 0, 0.8 + i * 0.15), Material.BLACK_CONCRETE);
                tendril.scale(0.2f, 0.03f, 0.3f).glow(15, 10, 20).interpolation(5, 0);
                tendril.entity().setTeleportDuration(3);
                spawnedEntities.add(tendril.entity());
                tendrils.add(tendril);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Track player position (update every 15 ticks)
            Player target = getTargetPlayer();
            if (target != null && target.isOnline() && tick % 15 == 0) {
                Location targetLoc = target.getLocation().clone();
                targetLoc.setY(c.getY());
                lastTrackLoc = targetLoc;
            }

            // Move shadow toward last tracked position
            if (lastTrackLoc != null) {
                double dx = lastTrackLoc.getX() - c.getX();
                double dz = lastTrackLoc.getZ() - c.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > 0.3) {
                    double speed = 0.15;
                    double mx = (dx / dist) * speed;
                    double mz = (dz / dist) * speed;
                    Location newCenter = c.clone().add(mx, 0, mz);
                    newCenter.setYaw(0);
                    newCenter.setPitch(0);
                    setCenter(newCenter);
                    c = newCenter;
                }

                // Check proximity for rearing
                if (target != null && target.isOnline()) {
                    double playerDist = target.getLocation().distanceSquared(c);
                    if (playerDist < 9.0) { // < 3 blocks
                        if (!rearing) {
                            rearing = true;
                            rearTick = tick;
                            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_HURT, 0.8f, 0.3f);
                        }
                    } else {
                        if (rearing) {
                            rearing = false;
                        }
                    }
                }
            }

            // Move all parts to current center
            body.entity().teleport(makeFlat(c));
            arms.get(0).entity().teleport(makeFlat(c.clone().add(-1.2, 0, 0)));
            arms.get(1).entity().teleport(makeFlat(c.clone().add(1.2, 0, 0)));
            head.entity().teleport(makeFlat(c.clone().add(0, 0, -0.7)));
            for (int i = 0; i < tendrils.size(); i++) {
                double tx = (i - 1.5) * 0.4;
                // Tendrils wave slightly
                double wave = Math.sin(tick * 0.1 + i) * 0.15;
                tendrils.get(i).entity().teleport(makeFlat(c.clone().add(tx + wave, 0, 0.8 + i * 0.15)));
            }

            // Rearing animation: increase Y scale over 5 ticks
            if (rearing) {
                int rearProgress = Math.min(5, tick - rearTick);
                float yScale = 0.05f + rearProgress * 0.18f; // 0.05 -> ~0.95 over 5 ticks
                body.entity().setInterpolationDuration(3);
                body.entity().setInterpolationDelay(0);
                body.entity().setTransformation(new Transformation(
                        body.entity().getTransformation().getTranslation(),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.8f, yScale, 1.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                head.entity().setInterpolationDuration(3);
                head.entity().setInterpolationDelay(0);
                head.entity().setTransformation(new Transformation(
                        head.entity().getTransformation().getTranslation(),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.6f, yScale * 0.6f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            } else {
                // Flatten back down
                body.entity().setInterpolationDuration(5);
                body.entity().setInterpolationDelay(0);
                body.entity().setTransformation(new Transformation(
                        body.entity().getTransformation().getTranslation(),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.8f, 0.05f, 1.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                head.entity().setInterpolationDuration(5);
                head.entity().setInterpolationDelay(0);
                head.entity().setTransformation(new Transformation(
                        head.entity().getTransformation().getTranslation(),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.6f, 0.04f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Shadow smoke particles
            if (tick % 5 == 0) {
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.1, 0), 2, 0.6, 0.05, 0.4, 0.005);
            }

            // Ambient whisper
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.2f);
            }
        }

        private Location makeFlat(Location loc) {
            loc.setYaw(0);
            loc.setPitch(0);
            return loc;
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowStalker(plugin); }
    }

    // ================================================================
    // 39. INFERNAL WEB -- "The Trap" (29 displays)
    //     A web-shaped structure that draws outward from center over 60
    //     ticks. Center hub, 8 radial lines, inner ring (8), outer ring (12).
    //     Damage radius grows as web completes.
    //     Center: 1 SHROOMLIGHT. Radials: 8 RED_NETHER_BRICKS flat slabs.
    //     Inner ring: 8 MAGMA_BLOCK. Outer ring: 12 MAGMA_BLOCK.
    //     damage=4.0, radius=0.5 (grows to 2.5), ticks-between-damage=12
    // ================================================================
    public static class InfernalWeb extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle hub;
        private final List<BlockDisplayHandle> radials = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private double currentRadius = 0.5;

        public InfernalWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_web", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(0.5); // Starts small, grows
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(200);
            config.setCooldownTicks(180);
            config.setChance(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Center hub: SHROOMLIGHT
            hub = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.SHROOMLIGHT);
            hub.scale(0.3f, 0.15f, 0.3f).glow(255, 100, 20).interpolation(5, 0);
            spawnedEntities.add(hub.entity());

            // 4 cardinal radial lines (N/S/E/W) -- RED_NETHER_BRICKS flat slabs
            double[][] cardinalDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (double[] dir : cardinalDirs) {
                Location radLoc = center.clone().add(dir[0] * 0.1, 0.02, dir[1] * 0.1);
                BlockDisplayHandle rad = displayBuilder.spawnBlock(radLoc, Material.RED_NETHER_BRICKS);
                // Start tiny, will grow
                rad.scale(0.01f, 0.02f, 0.01f).glow(200, 50, 10).interpolation(8, 0);
                if (dir[1] != 0) {
                    rad.rotate((float) Math.toRadians(90), 0f, 1f, 0f);
                }
                spawnedEntities.add(rad.entity());
                radials.add(rad);
            }

            // 4 diagonal radial lines (rotated 45 deg)
            double[][] diagDirs = {{0.707, 0.707}, {-0.707, 0.707}, {0.707, -0.707}, {-0.707, -0.707}};
            for (double[] dir : diagDirs) {
                Location radLoc = center.clone().add(dir[0] * 0.1, 0.02, dir[1] * 0.1);
                BlockDisplayHandle rad = displayBuilder.spawnBlock(radLoc, Material.RED_NETHER_BRICKS);
                rad.scale(0.01f, 0.02f, 0.01f).glow(200, 50, 10).interpolation(8, 0);
                float rotAngle = (float) Math.atan2(dir[1], dir[0]);
                rad.rotate(rotAngle, 0f, 1f, 0f);
                spawnedEntities.add(rad.entity());
                radials.add(rad);
            }

            // Inner ring: 8 MAGMA_BLOCK cubes (start at center, expand to radius 1.2)
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle ring = displayBuilder.spawnBlock(center.clone().add(0, 0.03, 0), Material.MAGMA_BLOCK);
                ring.scale(0.01f, 0.01f, 0.01f).glow(240, 80, 30).interpolation(5, 0);
                ring.entity().setTeleportDuration(3);
                spawnedEntities.add(ring.entity());
                innerRing.add(ring);
            }

            // Outer ring: 12 MAGMA_BLOCK cubes (start at center, expand to radius 2.4)
            for (int i = 0; i < 12; i++) {
                BlockDisplayHandle ring = displayBuilder.spawnBlock(center.clone().add(0, 0.03, 0), Material.MAGMA_BLOCK);
                ring.scale(0.01f, 0.01f, 0.01f).glow(255, 100, 20).interpolation(5, 0);
                ring.entity().setTeleportDuration(3);
                spawnedEntities.add(ring.entity());
                outerRing.add(ring);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NETHER_BRICKS_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Drawing phase: 0 to 60 ticks (3 seconds)
            double progress = Math.min(1.0, tick / 60.0);

            // Grow radial lines from center outward
            if (tick <= 60) {
                float radLen = (float) (2.5 * progress);
                for (int i = 0; i < radials.size(); i++) {
                    Transformation rt = radials.get(i).entity().getTransformation();
                    radials.get(i).entity().setInterpolationDuration(8);
                    radials.get(i).entity().setInterpolationDelay(0);
                    radials.get(i).entity().setTransformation(new Transformation(
                            rt.getTranslation(),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(radLen, 0.02f, 0.1f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Inner ring appears at 30% progress
                if (progress >= 0.3) {
                    double innerProg = (progress - 0.3) / 0.3; // 0 to 1 during 30-60%
                    double innerRadius = 1.2 * Math.min(1.0, innerProg);
                    for (int i = 0; i < innerRing.size(); i++) {
                        double angle = (2 * Math.PI * i) / 8.0;
                        double rx = Math.cos(angle) * innerRadius;
                        double rz = Math.sin(angle) * innerRadius;
                        Location ringLoc = center.clone().add(rx, 0.03, rz);
                        ringLoc.setYaw(0);
                        ringLoc.setPitch(0);
                        innerRing.get(i).entity().teleport(ringLoc);

                        if (innerProg > 0.1) {
                            float s = 0.15f * (float) Math.min(1.0, innerProg);
                            innerRing.get(i).entity().setInterpolationDuration(5);
                            innerRing.get(i).entity().setInterpolationDelay(0);
                            innerRing.get(i).entity().setTransformation(new Transformation(
                                    innerRing.get(i).entity().getTransformation().getTranslation(),
                                    innerRing.get(i).entity().getTransformation().getLeftRotation(),
                                    new Vector3f(s, s, s),
                                    innerRing.get(i).entity().getTransformation().getRightRotation()
                            ));
                        }
                    }
                }

                // Outer ring appears at 60% progress
                if (progress >= 0.6) {
                    double outerProg = (progress - 0.6) / 0.4; // 0 to 1 during 60-100%
                    double outerRadius = 2.4 * Math.min(1.0, outerProg);
                    for (int i = 0; i < outerRing.size(); i++) {
                        double angle = (2 * Math.PI * i) / 12.0;
                        double rx = Math.cos(angle) * outerRadius;
                        double rz = Math.sin(angle) * outerRadius;
                        Location ringLoc = center.clone().add(rx, 0.03, rz);
                        ringLoc.setYaw(0);
                        ringLoc.setPitch(0);
                        outerRing.get(i).entity().teleport(ringLoc);

                        if (outerProg > 0.1) {
                            float s = 0.12f * (float) Math.min(1.0, outerProg);
                            outerRing.get(i).entity().setInterpolationDuration(5);
                            outerRing.get(i).entity().setInterpolationDelay(0);
                            outerRing.get(i).entity().setTransformation(new Transformation(
                                    outerRing.get(i).entity().getTransformation().getTranslation(),
                                    outerRing.get(i).entity().getTransformation().getLeftRotation(),
                                    new Vector3f(s, s, s),
                                    outerRing.get(i).entity().getTransformation().getRightRotation()
                            ));
                        }
                    }
                }
            }

            // Update damage radius to match web expansion
            currentRadius = 0.5 + 2.0 * progress;
            config.setDamageRadius(currentRadius);

            // Pulse effect in idle phase
            if (tick > 60 && tick % 20 == 0) {
                // Hub pulse
                float pulse = 0.3f + 0.1f * (float) Math.sin(tick * 0.15);
                hub.entity().setInterpolationDuration(10);
                hub.entity().setInterpolationDelay(0);
                hub.entity().setTransformation(new Transformation(
                        hub.entity().getTransformation().getTranslation(),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, 0.15f, pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Fire particles along web strands
            if (tick % 4 == 0 && tick > 20) {
                int strandIdx = tick % 8;
                double[] angles;
                if (strandIdx < 4) {
                    double[][] cardinalAngles = {{0}, {Math.PI}, {Math.PI / 2}, {-Math.PI / 2}};
                    angles = new double[]{cardinalAngles[strandIdx][0]};
                } else {
                    double[][] diagAngles = {{Math.PI / 4}, {3 * Math.PI / 4}, {-Math.PI / 4}, {-3 * Math.PI / 4}};
                    angles = new double[]{diagAngles[strandIdx - 4][0]};
                }
                double pDist = Math.random() * 2.5 * progress;
                Location pLoc = center.clone().add(Math.cos(angles[0]) * pDist, 0.1, Math.sin(angles[0]) * pDist);
                w.spawnParticle(Particle.FLAME, pLoc, 1, 0.05, 0.05, 0.05, 0.005);
            }

            // Ambient crackle
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalWeb(plugin); }
    }

    // ================================================================
    // 40. DEMONIC WINGS -- "The Descent" (22 displays)
    //     Wings that fold closed on spawn then dramatically unfurl.
    //     Spine: 1 OBSIDIAN. Each wing (mirrored): 3 BLACKSTONE primary
    //     feathers, 5 GRAY_CONCRETE secondary, 2 BLACK_CONCRETE wisps.
    //     Wings slowly flap +-10 deg X with 60-tick period. Hovering.
    //     Constant radius under wing area.
    //     damage=5.0, radius=3.0, ticks-between-damage=14, duration=200
    // ================================================================
    public static class DemonicWings extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle spine;
        private final List<BlockDisplayHandle> leftPrimary = new ArrayList<>();
        private final List<BlockDisplayHandle> leftSecondary = new ArrayList<>();
        private final List<BlockDisplayHandle> leftWisps = new ArrayList<>();
        private final List<BlockDisplayHandle> rightPrimary = new ArrayList<>();
        private final List<BlockDisplayHandle> rightSecondary = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWisps = new ArrayList<>();
        private boolean unfurled = false;

        public DemonicWings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demonic_wings", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(14);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(200);
            config.setCooldownTicks(190);
            config.setChance(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Location spawnBase = center.clone().add(0, 3.5, 0); // Hovering height

            // Spine: 1 OBSIDIAN cube
            spine = displayBuilder.spawnBlock(spawnBase.clone(), Material.OBSIDIAN);
            spine.scale(0.3f, 0.8f, 0.3f).glow(20, 10, 5).interpolation(5, 0);
            spawnedEntities.add(spine.entity());

            // --- LEFT WING ---
            // 3 primary feathers (BLACKSTONE, large, angled outward)
            for (int i = 0; i < 3; i++) {
                double xOff = -(0.6 + i * 0.7);
                double yOff = 0.1 - i * 0.15;
                Location fLoc = spawnBase.clone().add(xOff, yOff, 0);
                BlockDisplayHandle feather = displayBuilder.spawnBlock(fLoc, Material.BLACKSTONE);
                // Start folded (rotated closed on X axis)
                float foldAngle = (float) Math.toRadians(80); // nearly closed
                feather.scale(0.8f, 0.05f, 0.3f).rotate(foldAngle, 0f, 0f, 1f)
                        .glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(feather.entity());
                leftPrimary.add(feather);
            }

            // 5 secondary feathers (GRAY_CONCRETE, smaller)
            for (int i = 0; i < 5; i++) {
                double xOff = -(0.4 + i * 0.45);
                double yOff = -0.2 - i * 0.12;
                Location fLoc = spawnBase.clone().add(xOff, yOff, 0.1);
                BlockDisplayHandle feather = displayBuilder.spawnBlock(fLoc, Material.GRAY_CONCRETE);
                float foldAngle = (float) Math.toRadians(80);
                feather.scale(0.5f, 0.04f, 0.2f).rotate(foldAngle, 0f, 0f, 1f)
                        .glow(50, 45, 45).interpolation(10, 0);
                spawnedEntities.add(feather.entity());
                leftSecondary.add(feather);
            }

            // 2 trailing wisps (BLACK_CONCRETE)
            for (int i = 0; i < 2; i++) {
                double xOff = -(1.8 + i * 0.4);
                double yOff = -0.5 - i * 0.2;
                Location fLoc = spawnBase.clone().add(xOff, yOff, 0.15);
                BlockDisplayHandle wisp = displayBuilder.spawnBlock(fLoc, Material.BLACK_CONCRETE);
                float foldAngle = (float) Math.toRadians(80);
                wisp.scale(0.3f, 0.03f, 0.15f).rotate(foldAngle, 0f, 0f, 1f)
                        .glow(10, 5, 10).interpolation(10, 0);
                spawnedEntities.add(wisp.entity());
                leftWisps.add(wisp);
            }

            // --- RIGHT WING (mirrored) ---
            for (int i = 0; i < 3; i++) {
                double xOff = 0.6 + i * 0.7;
                double yOff = 0.1 - i * 0.15;
                Location fLoc = spawnBase.clone().add(xOff, yOff, 0);
                BlockDisplayHandle feather = displayBuilder.spawnBlock(fLoc, Material.BLACKSTONE);
                float foldAngle = (float) Math.toRadians(-80);
                feather.scale(0.8f, 0.05f, 0.3f).rotate(foldAngle, 0f, 0f, 1f)
                        .glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(feather.entity());
                rightPrimary.add(feather);
            }

            for (int i = 0; i < 5; i++) {
                double xOff = 0.4 + i * 0.45;
                double yOff = -0.2 - i * 0.12;
                Location fLoc = spawnBase.clone().add(xOff, yOff, 0.1);
                BlockDisplayHandle feather = displayBuilder.spawnBlock(fLoc, Material.GRAY_CONCRETE);
                float foldAngle = (float) Math.toRadians(-80);
                feather.scale(0.5f, 0.04f, 0.2f).rotate(foldAngle, 0f, 0f, 1f)
                        .glow(50, 45, 45).interpolation(10, 0);
                spawnedEntities.add(feather.entity());
                rightSecondary.add(feather);
            }

            for (int i = 0; i < 2; i++) {
                double xOff = 1.8 + i * 0.4;
                double yOff = -0.5 - i * 0.2;
                Location fLoc = spawnBase.clone().add(xOff, yOff, 0.15);
                BlockDisplayHandle wisp = displayBuilder.spawnBlock(fLoc, Material.BLACK_CONCRETE);
                float foldAngle = (float) Math.toRadians(-80);
                wisp.scale(0.3f, 0.03f, 0.15f).rotate(foldAngle, 0f, 0f, 1f)
                        .glow(10, 5, 10).interpolation(10, 0);
                spawnedEntities.add(wisp.entity());
                rightWisps.add(wisp);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            Location spawnBase = center.clone().add(0, 3.5, 0);

            // Hovering bob
            double bob = Math.sin(tick * 0.05) * 0.3;
            Location hovering = spawnBase.clone().add(0, bob, 0);

            // Move spine
            Location spineLoc = hovering.clone();
            spineLoc.setYaw(0);
            spineLoc.setPitch(0);
            spine.entity().teleport(spineLoc);

            // Unfurl phase: first 20 ticks, rotate from 80 deg to 0 deg (open)
            float wingAngle;
            if (tick <= 20) {
                float unfurlProgress = tick / 20.0f;
                wingAngle = (float) Math.toRadians(80 * (1.0f - unfurlProgress));
                if (tick == 20) unfurled = true;
            } else {
                // Flapping: oscillate +-10 degrees with 60-tick period
                wingAngle = (float) Math.toRadians(10 * Math.sin(tick * 2 * Math.PI / 60.0));
            }

            // Update left wing positions and rotations
            updateWingSide(hovering, leftPrimary, leftSecondary, leftWisps, wingAngle, true, tick);
            updateWingSide(hovering, rightPrimary, rightSecondary, rightWisps, -wingAngle, false, tick);

            // Shadow/damage particles below
            if (tick % 6 == 0 && tick > 20) {
                double px = (Math.random() - 0.5) * 4.0;
                double pz = (Math.random() - 0.5) * 3.0;
                DisplayBuilder.dustParticles(center.clone().add(px, 0.2, pz), 2, 0.2, 20, 10, 5, 1.0f);
                w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(px, 0.1, pz), 1, 0.1, 0.05, 0.1, 0.005);
            }

            // Wing trail particles
            if (tick > 20 && tick % 4 == 0) {
                if (!leftPrimary.isEmpty()) {
                    Location wingTip = leftPrimary.get(leftPrimary.size() - 1).entity().getLocation();
                    w.spawnParticle(Particle.SMOKE, wingTip, 2, 0.2, 0.1, 0.2, 0.005);
                }
                if (!rightPrimary.isEmpty()) {
                    Location wingTip = rightPrimary.get(rightPrimary.size() - 1).entity().getLocation();
                    w.spawnParticle(Particle.SMOKE, wingTip, 2, 0.2, 0.1, 0.2, 0.005);
                }
            }

            // Flap sound
            if (tick > 20 && tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.5f);
            }

            // Ambient low drone
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.3f, 0.3f);
            }
        }

        private void updateWingSide(Location base, List<BlockDisplayHandle> primary,
                                     List<BlockDisplayHandle> secondary, List<BlockDisplayHandle> wisps,
                                     float wingAngle, boolean leftSide, int tick) {
            int sign = leftSide ? -1 : 1;

            // Primary feathers (3)
            for (int i = 0; i < primary.size(); i++) {
                double xOff = sign * (0.6 + i * 0.7);
                double yOff = 0.1 - i * 0.15;
                Location fLoc = base.clone().add(xOff, yOff, 0);
                fLoc.setYaw(0);
                fLoc.setPitch(0);
                primary.get(i).entity().teleport(fLoc);

                float featherAngle = wingAngle + (float) Math.toRadians(sign * (-5 - i * 8));
                primary.get(i).entity().setInterpolationDuration(5);
                primary.get(i).entity().setInterpolationDelay(0);
                primary.get(i).entity().setTransformation(new Transformation(
                        primary.get(i).entity().getTransformation().getTranslation(),
                        new AxisAngle4f(featherAngle, 0f, 0f, 1f),
                        new Vector3f(0.8f, 0.05f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Secondary feathers (5)
            for (int i = 0; i < secondary.size(); i++) {
                double xOff = sign * (0.4 + i * 0.45);
                double yOff = -0.2 - i * 0.12;
                Location fLoc = base.clone().add(xOff, yOff, 0.1);
                fLoc.setYaw(0);
                fLoc.setPitch(0);
                secondary.get(i).entity().teleport(fLoc);

                // Slightly more angle for secondaries (droop effect)
                float featherAngle = wingAngle * 1.2f + (float) Math.toRadians(sign * (-3 - i * 5));
                secondary.get(i).entity().setInterpolationDuration(5);
                secondary.get(i).entity().setInterpolationDelay(0);
                secondary.get(i).entity().setTransformation(new Transformation(
                        secondary.get(i).entity().getTransformation().getTranslation(),
                        new AxisAngle4f(featherAngle, 0f, 0f, 1f),
                        new Vector3f(0.5f, 0.04f, 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Trailing wisps (2)
            for (int i = 0; i < wisps.size(); i++) {
                double xOff = sign * (1.8 + i * 0.4);
                double yOff = -0.5 - i * 0.2;
                // Wisps wave slightly
                double wave = Math.sin(tick * 0.08 + i * 1.5) * 0.15;
                Location fLoc = base.clone().add(xOff, yOff + wave, 0.15);
                fLoc.setYaw(0);
                fLoc.setPitch(0);
                wisps.get(i).entity().teleport(fLoc);

                float wispAngle = wingAngle * 1.5f + (float) Math.toRadians(sign * (-10 - i * 8));
                wisps.get(i).entity().setInterpolationDuration(5);
                wisps.get(i).entity().setInterpolationDelay(0);
                wisps.get(i).entity().setTransformation(new Transformation(
                        wisps.get(i).entity().getTransformation().getTranslation(),
                        new AxisAngle4f(wispAngle, 0f, 0f, 1f),
                        new Vector3f(0.3f, 0.03f, 0.15f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DemonicWings(plugin); }
    }
}
