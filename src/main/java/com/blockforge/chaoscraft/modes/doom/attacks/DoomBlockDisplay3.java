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
 * Doom Mode — BLOCK DISPLAY ATTACKS (Part 3, attacks 21-30)
 * 10 advanced creature / animated block display attacks.
 *
 * Each creature is built from 25-35 BlockDisplay entities shaped into anatomy
 * via Transformation scale (limbs = rods, wings = plates, bodies = wide blocks).
 * ALL animation uses setTransformation() with setInterpolationDuration(2-5) for
 * smooth motion -- NO teleport for rotation. Teleport is only used for whole-body
 * movement (walking/flying).
 *
 * Creature attacks:
 *  #21  LavaGolemTitan         — 35 blocks, humanoid lava golem, arm/leg walk cycle
 *  #22  InfernalPhoenixRising  — 30 blocks, phoenix with flapping wing plates
 *  #23  NetherDragonSerpent    — 32 blocks, serpentine dragon, sine-wave undulation
 *  #24  MagmaSpiderQueen       — 30 blocks, spider with 8-leg walking gait
 *  #25  HellfireWyvernDive     — 28 blocks, diving wyvern, wing fold on descent
 *  #26  DoomCentipede          — 30 blocks, caterpillar-motion centipede
 *  #27  InfernalStagBeetle     — 28 blocks, beetle with charging horn
 *  #28  MagmaJellyfishSwarm    — 30 blocks, 5 jellyfish orbiting with tentacle sway
 *  #29  HellfireMinotaurCharge — 32 blocks, minotaur charge attack
 *  #30  DoomSkeletonKing       — 30 blocks, skeleton king with sword swing
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
 *            BLACK_CONCRETE, ORANGE_CONCRETE, GLOWSTONE, BONE_BLOCK, END_ROD
 *
 * Rules:
 * - NO status effects
 * - AxisAngle4f ONLY (never Quaternionf)
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Location center = getCenter(); if (center == null) return; EVERY onTick
 * - All rotation via Transformation + interpolation, NOT teleport
 * - Teleport only for whole-body positional movement
 */
public final class DoomBlockDisplay3 {
    private DoomBlockDisplay3() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new LavaGolemTitan(plugin));
        registry.register(new InfernalPhoenixRising(plugin));
        registry.register(new NetherDragonSerpent(plugin));
        registry.register(new MagmaSpiderQueen(plugin));
        registry.register(new HellfireWyvernDive(plugin));
        registry.register(new DoomCentipede(plugin));
        registry.register(new InfernalStagBeetle(plugin));
        registry.register(new MagmaJellyfishSwarm(plugin));
        registry.register(new HellfireMinotaurCharge(plugin));
        registry.register(new DoomSkeletonKing(plugin));
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

    /**
     * Helper: compute yaw angle from one location toward another (radians).
     */
    private static double yawToward(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.atan2(dz, dx);
    }

    // ================================================================
    // #21 — LAVA GOLEM TITAN (35 blocks)
    // Humanoid golem: chest 4, arms 4+4, legs 3+3, head 2, pauldrons 4,
    // fists 2, spine 3, eyes 2, kneecaps 2, belt 2.
    // Arms swing ±30° via Transform rotation. Legs alternate Y-translation
    // for walking gait. Tracks nearest player. 7r, 40dmg/30t.
    // ================================================================
    public static class LavaGolemTitan extends BlockDisplayAttack {
        // Body part handles
        private final List<BlockDisplayHandle> chest = new ArrayList<>();
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private final List<BlockDisplayHandle> leftLeg = new ArrayList<>();
        private final List<BlockDisplayHandle> rightLeg = new ArrayList<>();
        private final List<BlockDisplayHandle> headParts = new ArrayList<>();
        private final List<BlockDisplayHandle> pauldrons = new ArrayList<>();
        private final List<BlockDisplayHandle> fists = new ArrayList<>();
        private final List<BlockDisplayHandle> spine = new ArrayList<>();
        private final List<BlockDisplayHandle> extras = new ArrayList<>(); // eyes, kneecaps, belt
        private double facing = 0;

        public LavaGolemTitan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_golem_titan", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // CHEST — 4 magma blocks, wide torso scale(1.2, 1.0, 0.8)
            double[][] chestOffsets = {{0, 2.5, 0}, {0, 3.5, 0}, {0.3, 3.0, 0}, {-0.3, 3.0, 0}};
            for (double[] off : chestOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.MAGMA_BLOCK);
                h.scale(1.2f, 1.0f, 0.8f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                chest.add(h);
            }

            // LEFT ARM — 4 segments, rod shape scale(0.4, 1.2, 0.4)
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(-1.3, 3.5 - i * 0.8, 0), Material.NETHERRACK);
                h.scale(0.4f, 1.2f, 0.4f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                leftArm.add(h);
            }

            // RIGHT ARM — 4 segments, rod shape
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(1.3, 3.5 - i * 0.8, 0), Material.NETHERRACK);
                h.scale(0.4f, 1.2f, 0.4f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                rightArm.add(h);
            }

            // LEFT LEG — 3 segments, scale(0.5, 1.0, 0.5)
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(-0.4, 1.5 - i * 0.8, 0), Material.BLACKSTONE);
                h.scale(0.5f, 1.0f, 0.5f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                leftLeg.add(h);
            }

            // RIGHT LEG — 3 segments
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0.4, 1.5 - i * 0.8, 0), Material.BLACKSTONE);
                h.scale(0.5f, 1.0f, 0.5f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                rightLeg.add(h);
            }

            // HEAD — 2 blocks
            BlockDisplayHandle head1 = displayBuilder.spawnBlock(center.clone().add(0, 4.5, 0), Material.POLISHED_BLACKSTONE);
            head1.scale(0.8f, 0.8f, 0.8f).glow(240, 80, 30).interpolation(3, 0);
            spawnedEntities.add(head1.entity());
            headParts.add(head1);
            BlockDisplayHandle head2 = displayBuilder.spawnBlock(center.clone().add(0, 5.0, 0), Material.RED_NETHER_BRICKS);
            head2.scale(0.7f, 0.5f, 0.7f).glow(200, 50, 10).interpolation(3, 0);
            spawnedEntities.add(head2.entity());
            headParts.add(head2);

            // PAULDRONS — 4 wide shoulder plates scale(0.8, 0.3, 0.8)
            double[][] pauldronOffsets = {{-1.1, 4.0, -0.2}, {-1.1, 4.0, 0.2}, {1.1, 4.0, -0.2}, {1.1, 4.0, 0.2}};
            for (double[] off : pauldronOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHER_BRICKS);
                h.scale(0.8f, 0.3f, 0.8f).glow(120, 20, 80).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                pauldrons.add(h);
            }

            // FISTS — 2 glowing blocks
            BlockDisplayHandle fistL = displayBuilder.spawnBlock(center.clone().add(-1.3, 0.5, 0), Material.SHROOMLIGHT);
            fistL.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 20).interpolation(3, 0);
            spawnedEntities.add(fistL.entity());
            fists.add(fistL);
            BlockDisplayHandle fistR = displayBuilder.spawnBlock(center.clone().add(1.3, 0.5, 0), Material.SHROOMLIGHT);
            fistR.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 20).interpolation(3, 0);
            spawnedEntities.add(fistR.entity());
            fists.add(fistR);

            // SPINE — 3 segments behind torso
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 2.2 + i * 0.7, -0.5), Material.BASALT);
                h.scale(0.3f, 0.6f, 0.3f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                spine.add(h);
            }

            // EYES — 2 shroomlight
            BlockDisplayHandle eyeL = displayBuilder.spawnBlock(center.clone().add(-0.2, 4.7, 0.4), Material.SHROOMLIGHT);
            eyeL.scale(0.15f, 0.15f, 0.15f).glow(255, 200, 50).interpolation(2, 0);
            spawnedEntities.add(eyeL.entity());
            extras.add(eyeL);
            BlockDisplayHandle eyeR = displayBuilder.spawnBlock(center.clone().add(0.2, 4.7, 0.4), Material.SHROOMLIGHT);
            eyeR.scale(0.15f, 0.15f, 0.15f).glow(255, 200, 50).interpolation(2, 0);
            spawnedEntities.add(eyeR.entity());
            extras.add(eyeR);

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.4f);
            w.spawnParticle(Particle.LAVA, center.clone().add(0, 3, 0), 30, 1.5, 2, 1.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Track nearest player
            Player target = findNearestPlayer(center, 30.0);
            if (target != null) {
                facing = yawToward(center, target.getLocation());
            }

            // Walk cycle: swing arms ±30° on X-axis, alternating legs via Y-translation
            float armSwing = (float) (Math.toRadians(30) * Math.sin(tick * 0.15));
            float legShift = (float) (0.3 * Math.sin(tick * 0.15));

            // Left arm swing forward/backward
            for (int i = 0; i < leftArm.size(); i++) {
                Transformation t = leftArm.get(i).entity().getTransformation();
                leftArm.get(i).entity().setInterpolationDuration(3);
                leftArm.get(i).entity().setInterpolationDelay(0);
                leftArm.get(i).entity().setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(armSwing, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Right arm opposite swing
            for (int i = 0; i < rightArm.size(); i++) {
                Transformation t = rightArm.get(i).entity().getTransformation();
                rightArm.get(i).entity().setInterpolationDuration(3);
                rightArm.get(i).entity().setInterpolationDelay(0);
                rightArm.get(i).entity().setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(-armSwing, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Left leg Y-translation shift (walk bob)
            for (BlockDisplayHandle leg : leftLeg) {
                Transformation t = leg.entity().getTransformation();
                Vector3f trans = new Vector3f(t.getTranslation());
                trans.y = -0.5f + legShift;
                leg.entity().setInterpolationDuration(3);
                leg.entity().setInterpolationDelay(0);
                leg.entity().setTransformation(new Transformation(
                        trans, new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // Right leg opposite shift
            for (BlockDisplayHandle leg : rightLeg) {
                Transformation t = leg.entity().getTransformation();
                Vector3f trans = new Vector3f(t.getTranslation());
                trans.y = -0.5f - legShift;
                leg.entity().setInterpolationDuration(3);
                leg.entity().setInterpolationDelay(0);
                leg.entity().setTransformation(new Transformation(
                        trans, new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // Slowly walk toward player using teleport for position only
            if (target != null && tick % 5 == 0) {
                double dx = Math.cos(facing) * 0.15;
                double dz = Math.sin(facing) * 0.15;
                Location newCenter = center.clone().add(dx, 0, dz);
                setCenter(newCenter);

                // Teleport all body parts to new position (maintaining relative offsets)
                // We move entire golem by shifting center; individual blocks animate via Transform
            }

            // Ember particles from fists every 4 ticks
            if (tick % 4 == 0) {
                for (BlockDisplayHandle fist : fists) {
                    DisplayBuilder.dustParticles(fist.entity().getLocation(), 3, 0.3, 255, 100, 20, 1.2f);
                }
            }

            // Footstep sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 0.8f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new LavaGolemTitan(plugin); }
    }

    // ================================================================
    // #22 — INFERNAL PHOENIX RISING (30 blocks)
    // Wings 12 (6/side, scale(0.8,0.1,0.6) plates), body 5, tail 6
    // scale(0.2,0.1,1.0), head 3, beak 1 scale(0.15,0.15,0.4), eyes 3.
    // Wings flap Z-axis ±25° at 10-tick period via Transform.
    // Circular flight Y=6. 8r, 25dmg/20t. FLAME wingtip trail.
    // ================================================================
    public static class InfernalPhoenixRising extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private final List<BlockDisplayHandle> headParts = new ArrayList<>();
        private BlockDisplayHandle beak;
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private static final double FLIGHT_RADIUS = 5.0;
        private static final double FLIGHT_Y = 6.0;

        public InfernalPhoenixRising(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_phoenix_rising", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location spawnBase = center.clone().add(FLIGHT_RADIUS, FLIGHT_Y, 0);

            // BODY — 5 magma blocks forming elongated torso
            for (int i = 0; i < 5; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0, -1.0 + i * 0.5), Material.MAGMA_BLOCK);
                float bodyScale = (i == 2) ? 1.0f : 0.7f;
                h.scale(0.6f * bodyScale, 0.5f * bodyScale, 0.8f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }

            // LEFT WING — 6 thin plates scale(0.8, 0.1, 0.6)
            for (int i = 0; i < 6; i++) {
                double xOff = -0.6 - i * 0.4;
                double zOff = -0.3 + (i % 3) * 0.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(xOff, 0.2, zOff), Material.ORANGE_CONCRETE);
                h.scale(0.8f, 0.1f, 0.6f).glow(240, 80, 30).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                leftWing.add(h);
            }

            // RIGHT WING — 6 thin plates (mirrored)
            for (int i = 0; i < 6; i++) {
                double xOff = 0.6 + i * 0.4;
                double zOff = -0.3 + (i % 3) * 0.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(xOff, 0.2, zOff), Material.ORANGE_CONCRETE);
                h.scale(0.8f, 0.1f, 0.6f).glow(240, 80, 30).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                rightWing.add(h);
            }

            // TAIL — 6 long thin segments scale(0.2, 0.1, 1.0)
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(0, -0.1 - i * 0.1, 1.5 + i * 0.6), Material.RED_CONCRETE);
                h.scale(0.2f, 0.1f, 1.0f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                tail.add(h);
            }

            // HEAD — 3 blocks
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0.3 + i * 0.2, -1.5), Material.SHROOMLIGHT);
                float s = 0.5f - i * 0.1f;
                h.scale(s, s, s).glow(255, 200, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headParts.add(h);
            }

            // BEAK — scale(0.15, 0.15, 0.4)
            beak = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0.3, -2.0), Material.GLOWSTONE);
            beak.scale(0.15f, 0.15f, 0.4f).glow(220, 180, 30).interpolation(3, 0);
            spawnedEntities.add(beak.entity());

            // EYES — 3 small glowing
            for (int i = 0; i < 3; i++) {
                double xOff = (i == 0) ? -0.15 : (i == 1) ? 0.15 : 0.0;
                double yOff = (i == 2) ? 0.55 : 0.45;
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(xOff, yOff, -1.6), Material.SHROOMLIGHT);
                h.scale(0.12f, 0.12f, 0.12f).glow(255, 255, 200).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                eyes.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.6f);
            w.spawnParticle(Particle.FLAME, spawnBase, 40, 2, 1, 2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Circular flight path
            double flightAngle = tick * 0.04;
            double fx = Math.cos(flightAngle) * FLIGHT_RADIUS;
            double fz = Math.sin(flightAngle) * FLIGHT_RADIUS;
            double yBob = Math.sin(tick * 0.08) * 0.5;
            Location flightPos = center.clone().add(fx, FLIGHT_Y + yBob, fz);

            // Teleport body to flight position
            for (int i = 0; i < body.size(); i++) {
                Location bLoc = flightPos.clone().add(0, 0, -1.0 + i * 0.5);
                bLoc.setYaw(0); bLoc.setPitch(0);
                body.get(i).entity().teleport(bLoc);
            }

            // Wing flap: Z-axis ±25° at 10-tick period via Transform
            float flapAngle = (float) (Math.toRadians(25) * Math.sin(tick * (2 * Math.PI / 10.0)));

            // Left wing flap (negative Z = left tilts up)
            for (int i = 0; i < leftWing.size(); i++) {
                float intensity = 1.0f + i * 0.15f; // outer feathers flap more
                BlockDisplay bd = leftWing.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(flapAngle * intensity, 0, 0, 1),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                Location wLoc = flightPos.clone().add(-0.6 - i * 0.4, 0.2, -0.3 + (i % 3) * 0.2);
                wLoc.setYaw(0); wLoc.setPitch(0);
                bd.teleport(wLoc);
            }

            // Right wing flap (mirrored)
            for (int i = 0; i < rightWing.size(); i++) {
                float intensity = 1.0f + i * 0.15f;
                BlockDisplay bd = rightWing.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(-flapAngle * intensity, 0, 0, 1),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                Location wLoc = flightPos.clone().add(0.6 + i * 0.4, 0.2, -0.3 + (i % 3) * 0.2);
                wLoc.setYaw(0); wLoc.setPitch(0);
                bd.teleport(wLoc);
            }

            // Tail follows
            for (int i = 0; i < tail.size(); i++) {
                float tailSway = (float) (Math.toRadians(10) * Math.sin(tick * 0.1 + i * 0.5));
                BlockDisplay bd = tail.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(4);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(tailSway, 0, 1, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                Location tLoc = flightPos.clone().add(0, -0.1 - i * 0.1, 1.5 + i * 0.6);
                tLoc.setYaw(0); tLoc.setPitch(0);
                bd.teleport(tLoc);
            }

            // Head, beak, eyes follow flight position
            for (int i = 0; i < headParts.size(); i++) {
                Location hLoc = flightPos.clone().add(0, 0.3 + i * 0.2, -1.5);
                hLoc.setYaw(0); hLoc.setPitch(0);
                headParts.get(i).entity().teleport(hLoc);
            }
            Location bkLoc = flightPos.clone().add(0, 0.3, -2.0);
            bkLoc.setYaw(0); bkLoc.setPitch(0);
            beak.entity().teleport(bkLoc);

            for (int i = 0; i < eyes.size(); i++) {
                double xOff = (i == 0) ? -0.15 : (i == 1) ? 0.15 : 0.0;
                double yOff = (i == 2) ? 0.55 : 0.45;
                Location eLoc = flightPos.clone().add(xOff, yOff, -1.6);
                eLoc.setYaw(0); eLoc.setPitch(0);
                eyes.get(i).entity().teleport(eLoc);
            }

            // FLAME wingtip trail
            if (tick % 2 == 0) {
                if (!leftWing.isEmpty()) {
                    Location tipL = leftWing.get(leftWing.size() - 1).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.FLAME, tipL, 3, 0.1, 0.1, 0.1, 0.02);
                }
                if (!rightWing.isEmpty()) {
                    Location tipR = rightWing.get(rightWing.size() - 1).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.FLAME, tipR, 3, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // Ambient fire sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(flightPos, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.2f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new InfernalPhoenixRising(plugin); }
    }

    // ================================================================
    // #23 — NETHER DRAGON SERPENT (32 blocks)
    // 20 body segments S-curve (scale(0.8,0.6,0.8)→tail smaller),
    // head 4 scale(1.0,0.8,1.2), jaw 2 (open/close), horns 2
    // scale(0.2,0.6,0.2), wings 4. Body undulates sine-wave Y+lateral
    // via Transform. Head tracks player. 5r along body, 35dmg/20t.
    // ================================================================
    public static class NetherDragonSerpent extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodySegments = new ArrayList<>();
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> jaws = new ArrayList<>();
        private final List<BlockDisplayHandle> horns = new ArrayList<>();
        private final List<BlockDisplayHandle> wings = new ArrayList<>();
        private static final int SEGMENT_COUNT = 20;

        public NetherDragonSerpent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_dragon_serpent", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 BODY SEGMENTS — S-curve, gradually smaller toward tail
            for (int i = 0; i < SEGMENT_COUNT; i++) {
                double progress = (double) i / SEGMENT_COUNT;
                double sineX = Math.sin(i * 0.6) * 2.0;
                double zPos = -5.0 + i * 0.6;
                Location segLoc = center.clone().add(sineX, 2.0, zPos);

                Material mat = (i % 3 == 0) ? Material.RED_NETHER_BRICKS : (i % 3 == 1) ? Material.NETHER_BRICKS : Material.NETHERRACK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                float scale = (float) (0.8 - progress * 0.4);
                h.scale(scale, scale * 0.75f, scale).glow(200, 50, 10).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                bodySegments.add(h);
            }

            // HEAD — 4 blocks scale(1.0, 0.8, 1.2)
            Location headBase = center.clone().add(0, 2.5, -5.5);
            Material[] headMats = {Material.POLISHED_BLACKSTONE, Material.RED_NETHER_BRICKS, Material.MAGMA_BLOCK, Material.NETHER_BRICKS};
            double[][] headOff = {{0, 0, 0}, {0, 0.5, 0}, {0.3, 0.2, -0.3}, {-0.3, 0.2, -0.3}};
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(headBase.clone().add(headOff[i][0], headOff[i][1], headOff[i][2]), headMats[i]);
                h.scale(1.0f, 0.8f, 1.2f).glow(240, 80, 30).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headBlocks.add(h);
            }

            // JAWS — 2 blocks (upper and lower, open/close)
            BlockDisplayHandle jawUpper = displayBuilder.spawnBlock(headBase.clone().add(0, 0.3, -0.8), Material.BLACKSTONE);
            jawUpper.scale(0.6f, 0.2f, 0.8f).glow(20, 10, 5).interpolation(3, 0);
            spawnedEntities.add(jawUpper.entity());
            jaws.add(jawUpper);
            BlockDisplayHandle jawLower = displayBuilder.spawnBlock(headBase.clone().add(0, -0.2, -0.8), Material.BLACKSTONE);
            jawLower.scale(0.6f, 0.2f, 0.8f).glow(20, 10, 5).interpolation(3, 0);
            spawnedEntities.add(jawLower.entity());
            jaws.add(jawLower);

            // HORNS — 2 scale(0.2, 0.6, 0.2)
            BlockDisplayHandle hornL = displayBuilder.spawnBlock(headBase.clone().add(-0.4, 0.8, -0.2), Material.BASALT);
            hornL.scale(0.2f, 0.6f, 0.2f).glow(120, 20, 80).interpolation(3, 0);
            spawnedEntities.add(hornL.entity());
            horns.add(hornL);
            BlockDisplayHandle hornR = displayBuilder.spawnBlock(headBase.clone().add(0.4, 0.8, -0.2), Material.BASALT);
            hornR.scale(0.2f, 0.6f, 0.2f).glow(120, 20, 80).interpolation(3, 0);
            spawnedEntities.add(hornR.entity());
            horns.add(hornR);

            // WINGS — 4 plates near segment 5-6
            double[][] wingOff = {{-1.5, 0.5, -2.0}, {-2.0, 0.8, -1.5}, {1.5, 0.5, -2.0}, {2.0, 0.8, -1.5}};
            for (double[] off : wingOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], 2.0 + off[1], off[2]), Material.RED_CONCRETE);
                h.scale(0.8f, 0.1f, 0.6f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                wings.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Sine-wave undulation: each segment has phase offset
            for (int i = 0; i < bodySegments.size(); i++) {
                double progress = (double) i / SEGMENT_COUNT;
                double phase = tick * 0.12 + i * 0.4;
                double lateralWave = Math.sin(phase) * 1.8;
                double yWave = Math.cos(phase * 0.7) * 0.6;
                double zPos = -5.0 + i * 0.6;

                // Animate via Transform translation (smooth)
                BlockDisplay bd = bodySegments.get(i).entity();
                float scale = (float) (0.8 - progress * 0.4);
                bd.setInterpolationDuration(4);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        new Vector3f((float) lateralWave - 0.5f, (float) yWave - 0.5f, -0.5f),
                        new AxisAngle4f((float) (Math.sin(phase) * Math.toRadians(10)), 0, 0, 1),
                        new Vector3f(scale, scale * 0.75f, scale),
                        new AxisAngle4f(0, 0, 1, 0)
                ));

                // Teleport for forward movement along path
                Location segLoc = center.clone().add(0, 2.0, zPos);
                segLoc.setYaw(0); segLoc.setPitch(0);
                bd.teleport(segLoc);
            }

            // Head tracks nearest player
            Player target = findNearestPlayer(center, 25.0);
            Location headBase = center.clone().add(0, 2.5, -5.5);

            if (target != null) {
                double yaw = yawToward(headBase, target.getLocation());
                float headRotY = (float) yaw;
                for (int i = 0; i < headBlocks.size(); i++) {
                    BlockDisplay bd = headBlocks.get(i).entity();
                    Transformation t = bd.getTransformation();
                    bd.setInterpolationDuration(3);
                    bd.setInterpolationDelay(0);
                    bd.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(headRotY, 0, 1, 0),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Jaw open/close cycle
            float jawAngle = (float) (Math.toRadians(15) * Math.abs(Math.sin(tick * 0.1)));
            if (jaws.size() >= 2) {
                BlockDisplay upper = jaws.get(0).entity();
                upper.setInterpolationDuration(3);
                upper.setInterpolationDelay(0);
                Transformation ut = upper.getTransformation();
                upper.setTransformation(new Transformation(
                        ut.getTranslation(),
                        new AxisAngle4f(jawAngle, 1, 0, 0),
                        ut.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                BlockDisplay lower = jaws.get(1).entity();
                lower.setInterpolationDuration(3);
                lower.setInterpolationDelay(0);
                Transformation lt = lower.getTransformation();
                lower.setTransformation(new Transformation(
                        lt.getTranslation(),
                        new AxisAngle4f(-jawAngle, 1, 0, 0),
                        lt.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Fire particles along body
            if (tick % 4 == 0 && bodySegments.size() > 5) {
                Location segLoc = bodySegments.get(5).entity().getLocation();
                center.getWorld().spawnParticle(Particle.FLAME, segLoc, 4, 0.3, 0.2, 0.3, 0.02);
            }

            // Growl sound periodically
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.6f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NetherDragonSerpent(plugin); }
    }

    // ================================================================
    // #24 — MAGMA SPIDER QUEEN (30 blocks)
    // Abdomen 4 scale(1.2,0.8,1.4), thorax 3, head 2, 8 legs (2 segments
    // each = 16 total, upper scale(0.2,0.8,0.2) + lower scale(0.15,0.7,0.15)),
    // pedipalps 2, eyes 3 shroomlight scale(0.15,0.15,0.15).
    // Legs animate walking gait via Transform rotation. 6r, 30dmg/25t.
    // ================================================================
    public static class MagmaSpiderQueen extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> abdomen = new ArrayList<>();
        private final List<BlockDisplayHandle> thorax = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> upperLegs = new ArrayList<>(); // 8 upper segments
        private final List<BlockDisplayHandle> lowerLegs = new ArrayList<>(); // 8 lower segments
        private final List<BlockDisplayHandle> pedipalps = new ArrayList<>();
        private final List<BlockDisplayHandle> eyesList = new ArrayList<>();
        private double moveFacing = 0;

        public MagmaSpiderQueen(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_spider_queen", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // ABDOMEN — 4 blocks scale(1.2, 0.8, 1.4)
            double[][] abdOff = {{0, 1.5, 1.0}, {0, 1.5, 1.8}, {0.3, 1.7, 1.4}, {-0.3, 1.7, 1.4}};
            for (double[] off : abdOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.MAGMA_BLOCK);
                h.scale(1.2f, 0.8f, 1.4f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                abdomen.add(h);
            }

            // THORAX — 3 blocks
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 1.6, -0.3 + i * 0.4), Material.RED_NETHER_BRICKS);
                h.scale(0.8f, 0.6f, 0.6f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                thorax.add(h);
            }

            // HEAD — 2 blocks
            BlockDisplayHandle h1 = displayBuilder.spawnBlock(center.clone().add(0, 1.6, -0.8), Material.POLISHED_BLACKSTONE);
            h1.scale(0.7f, 0.6f, 0.7f).glow(20, 10, 5).interpolation(3, 0);
            spawnedEntities.add(h1.entity());
            head.add(h1);
            BlockDisplayHandle h2 = displayBuilder.spawnBlock(center.clone().add(0, 1.9, -0.9), Material.NETHER_BRICKS);
            h2.scale(0.5f, 0.4f, 0.5f).glow(20, 10, 5).interpolation(3, 0);
            spawnedEntities.add(h2.entity());
            head.add(h2);

            // 8 LEGS — each has upper (scale(0.2,0.8,0.2)) + lower (scale(0.15,0.7,0.15))
            // 4 legs per side, spread around the body
            double[] legAngles = {-60, -30, 30, 60, -150, -120, 120, 150}; // degrees from front
            for (int i = 0; i < 8; i++) {
                double rad = Math.toRadians(legAngles[i]);
                double legX = Math.cos(rad) * 1.0;
                double legZ = Math.sin(rad) * 1.0;

                // Upper leg segment
                BlockDisplayHandle upper = displayBuilder.spawnBlock(
                        center.clone().add(legX, 1.3, legZ), Material.BLACKSTONE);
                upper.scale(0.2f, 0.8f, 0.2f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(upper.entity());
                upperLegs.add(upper);

                // Lower leg segment (further out and lower)
                double lowerX = Math.cos(rad) * 1.8;
                double lowerZ = Math.sin(rad) * 1.8;
                BlockDisplayHandle lower = displayBuilder.spawnBlock(
                        center.clone().add(lowerX, 0.5, lowerZ), Material.DEEPSLATE);
                lower.scale(0.15f, 0.7f, 0.15f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(lower.entity());
                lowerLegs.add(lower);
            }

            // PEDIPALPS — 2 small forward appendages
            BlockDisplayHandle pp1 = displayBuilder.spawnBlock(center.clone().add(-0.3, 1.4, -1.2), Material.NETHERRACK);
            pp1.scale(0.15f, 0.15f, 0.4f).glow(200, 50, 10).interpolation(3, 0);
            spawnedEntities.add(pp1.entity());
            pedipalps.add(pp1);
            BlockDisplayHandle pp2 = displayBuilder.spawnBlock(center.clone().add(0.3, 1.4, -1.2), Material.NETHERRACK);
            pp2.scale(0.15f, 0.15f, 0.4f).glow(200, 50, 10).interpolation(3, 0);
            spawnedEntities.add(pp2.entity());
            pedipalps.add(pp2);

            // EYES — 3 shroomlight scale(0.15, 0.15, 0.15)
            double[][] eyeOff = {{-0.15, 2.0, -1.0}, {0.15, 2.0, -1.0}, {0, 2.1, -0.95}};
            for (double[] off : eyeOff) {
                BlockDisplayHandle eye = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.SHROOMLIGHT);
                eye.scale(0.15f, 0.15f, 0.15f).glow(255, 200, 50).interpolation(2, 0);
                spawnedEntities.add(eye.entity());
                eyesList.add(eye);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Walking gait: alternating leg groups rotate via Transform
            // Group A (legs 0,2,5,7) and Group B (legs 1,3,4,6) alternate
            for (int i = 0; i < 8; i++) {
                boolean groupA = (i == 0 || i == 2 || i == 5 || i == 7);
                float phase = groupA ? 1.0f : -1.0f;
                float legRotation = (float) (Math.toRadians(20) * Math.sin(tick * 0.2) * phase);

                // Upper leg rotation
                if (i < upperLegs.size()) {
                    BlockDisplay bd = upperLegs.get(i).entity();
                    Transformation t = bd.getTransformation();
                    bd.setInterpolationDuration(3);
                    bd.setInterpolationDelay(0);
                    bd.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(legRotation, 0, 0, 1),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }

                // Lower leg rotation (slightly more swing)
                if (i < lowerLegs.size()) {
                    BlockDisplay bd = lowerLegs.get(i).entity();
                    Transformation t = bd.getTransformation();
                    bd.setInterpolationDuration(3);
                    bd.setInterpolationDelay(0);
                    bd.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(legRotation * 1.3f, 0, 0, 1),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Pedipalps twitch
            for (int i = 0; i < pedipalps.size(); i++) {
                float twitch = (float) (Math.toRadians(8) * Math.sin(tick * 0.3 + i * Math.PI));
                BlockDisplay bd = pedipalps.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(2);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(twitch, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Scuttling sound
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_SPIDER_STEP, 0.8f, 0.6f);
            }

            // Venom drip particles from pedipalps
            if (tick % 6 == 0) {
                for (BlockDisplayHandle pp : pedipalps) {
                    DisplayBuilder.dustParticles(pp.entity().getLocation(), 2, 0.15, 100, 200, 50, 0.8f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MagmaSpiderQueen(plugin); }
    }

    // ================================================================
    // #25 — HELLFIRE WYVERN DIVE (28 blocks)
    // Body 4, wings 10 (5/side fan scale(0.6,0.1,0.8)), tail 4,
    // head 2, claws 4, horn 1, neck 2, eye 1.
    // Starts Y=15, dives (wings fold inward via rotation during descent).
    // Impact-only 8r, 55dmg. ENTITY_ENDER_DRAGON_FLAP.
    // ================================================================
    public static class HellfireWyvernDive extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private final List<BlockDisplayHandle> tailBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> claws = new ArrayList<>();
        private final List<BlockDisplayHandle> miscParts = new ArrayList<>(); // horn, neck, eye
        private boolean impactDone = false;
        private static final double START_Y = 15.0;

        public HellfireWyvernDive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_wyvern_dive", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(55.0);
            config.setImpactRadius(8.0);
            config.setDamage(0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location spawnBase = center.clone().add(0, START_Y, 0);

            // BODY — 4 blocks
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0, -0.5 + i * 0.5), Material.MAGMA_BLOCK);
                h.scale(0.7f, 0.6f, 0.8f).glow(255, 100, 20).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                bodyBlocks.add(h);
            }

            // LEFT WING — 5 fan plates scale(0.6, 0.1, 0.8)
            for (int i = 0; i < 5; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(-0.8 - i * 0.5, 0.1, -0.2 + i * 0.15), Material.RED_CONCRETE);
                h.scale(0.6f, 0.1f, 0.8f).glow(200, 50, 10).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                leftWing.add(h);
            }

            // RIGHT WING — 5 fan plates (mirrored)
            for (int i = 0; i < 5; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(0.8 + i * 0.5, 0.1, -0.2 + i * 0.15), Material.RED_CONCRETE);
                h.scale(0.6f, 0.1f, 0.8f).glow(200, 50, 10).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                rightWing.add(h);
            }

            // TAIL — 4 segments
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(0, -0.1 * i, 1.2 + i * 0.5), Material.NETHER_BRICKS);
                float s = 0.5f - i * 0.08f;
                h.scale(s, s * 0.6f, 0.6f).glow(120, 20, 80).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                tailBlocks.add(h);
            }

            // HEAD — 2 blocks
            BlockDisplayHandle hd1 = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0.2, -1.2), Material.POLISHED_BLACKSTONE);
            hd1.scale(0.6f, 0.5f, 0.8f).glow(20, 10, 5).interpolation(4, 0);
            spawnedEntities.add(hd1.entity());
            headBlocks.add(hd1);
            BlockDisplayHandle hd2 = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0.4, -1.5), Material.BLACKSTONE);
            hd2.scale(0.4f, 0.35f, 0.6f).glow(20, 10, 5).interpolation(4, 0);
            spawnedEntities.add(hd2.entity());
            headBlocks.add(hd2);

            // CLAWS — 4 small blocks
            double[][] clawOff = {{-0.5, -0.3, -0.8}, {0.5, -0.3, -0.8}, {-0.5, -0.3, 0.3}, {0.5, -0.3, 0.3}};
            for (double[] off : clawOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(off[0], off[1], off[2]), Material.DEEPSLATE);
                h.scale(0.2f, 0.3f, 0.2f).glow(20, 10, 5).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                claws.add(h);
            }

            // HORN — 1
            BlockDisplayHandle horn = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0.6, -1.6), Material.BASALT);
            horn.scale(0.15f, 0.4f, 0.15f).glow(120, 20, 80).interpolation(4, 0);
            spawnedEntities.add(horn.entity());
            miscParts.add(horn);

            // NECK — 2
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0.1, -0.7 - i * 0.3), Material.RED_NETHER_BRICKS);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 50, 10).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                miscParts.add(h);
            }

            // EYE — 1
            BlockDisplayHandle eye = displayBuilder.spawnBlock(spawnBase.clone().add(0, 0.35, -1.7), Material.SHROOMLIGHT);
            eye.scale(0.12f, 0.12f, 0.12f).glow(255, 200, 50).interpolation(2, 0);
            spawnedEntities.add(eye.entity());
            miscParts.add(eye);

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.7f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (impactDone) return;

            // Descent rate: accelerate over time
            double descentProgress = Math.min(1.0, tick / 80.0);
            double currentY = START_Y * (1.0 - descentProgress * descentProgress);

            if (currentY <= 0.5) {
                // IMPACT
                impactDone = true;
                triggerImpactDamage(center);
                center.getWorld().spawnParticle(Particle.EXPLOSION, center, 5, 2, 1, 2, 0);
                center.getWorld().spawnParticle(Particle.FLAME, center, 50, 3, 1, 3, 0.2);
                center.getWorld().spawnParticle(Particle.LAVA, center, 30, 2, 1, 2, 0.1);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                return;
            }

            Location flightPos = center.clone().add(0, currentY, 0);

            // Wings fold inward during dive: rotation increases as descent progresses
            float foldAngle = (float) (Math.toRadians(60) * descentProgress);

            for (int i = 0; i < leftWing.size(); i++) {
                BlockDisplay bd = leftWing.get(i).entity();
                bd.setInterpolationDuration(5);
                bd.setInterpolationDelay(0);
                Transformation t = bd.getTransformation();
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(foldAngle * (1 + i * 0.1f), 0, 0, 1),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                Location wLoc = flightPos.clone().add(-0.8 - i * 0.5, 0.1, -0.2 + i * 0.15);
                wLoc.setYaw(0); wLoc.setPitch(0);
                bd.teleport(wLoc);
            }

            for (int i = 0; i < rightWing.size(); i++) {
                BlockDisplay bd = rightWing.get(i).entity();
                bd.setInterpolationDuration(5);
                bd.setInterpolationDelay(0);
                Transformation t = bd.getTransformation();
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(-foldAngle * (1 + i * 0.1f), 0, 0, 1),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                Location wLoc = flightPos.clone().add(0.8 + i * 0.5, 0.1, -0.2 + i * 0.15);
                wLoc.setYaw(0); wLoc.setPitch(0);
                bd.teleport(wLoc);
            }

            // Teleport all other parts to follow descent
            for (int i = 0; i < bodyBlocks.size(); i++) {
                Location bLoc = flightPos.clone().add(0, 0, -0.5 + i * 0.5);
                bLoc.setYaw(0); bLoc.setPitch(0);
                bodyBlocks.get(i).entity().teleport(bLoc);
            }
            for (int i = 0; i < tailBlocks.size(); i++) {
                Location tLoc = flightPos.clone().add(0, -0.1 * i, 1.2 + i * 0.5);
                tLoc.setYaw(0); tLoc.setPitch(0);
                tailBlocks.get(i).entity().teleport(tLoc);
            }
            for (int i = 0; i < headBlocks.size(); i++) {
                double zOff = (i == 0) ? -1.2 : -1.5;
                double yOff = (i == 0) ? 0.2 : 0.4;
                Location hLoc = flightPos.clone().add(0, yOff, zOff);
                hLoc.setYaw(0); hLoc.setPitch(0);
                headBlocks.get(i).entity().teleport(hLoc);
            }
            for (int i = 0; i < claws.size(); i++) {
                double[][] clawOff = {{-0.5, -0.3, -0.8}, {0.5, -0.3, -0.8}, {-0.5, -0.3, 0.3}, {0.5, -0.3, 0.3}};
                Location cLoc = flightPos.clone().add(clawOff[i][0], clawOff[i][1], clawOff[i][2]);
                cLoc.setYaw(0); cLoc.setPitch(0);
                claws.get(i).entity().teleport(cLoc);
            }
            // Misc parts follow
            double[][] miscOff = {{0, 0.6, -1.6}, {0, 0.1, -0.7}, {0, 0.1, -1.0}, {0, 0.35, -1.7}};
            for (int i = 0; i < miscParts.size() && i < miscOff.length; i++) {
                Location mLoc = flightPos.clone().add(miscOff[i][0], miscOff[i][1], miscOff[i][2]);
                mLoc.setYaw(0); mLoc.setPitch(0);
                miscParts.get(i).entity().teleport(mLoc);
            }

            // Dive wind particles
            if (tick % 3 == 0) {
                center.getWorld().spawnParticle(Particle.CLOUD, flightPos, 3, 0.5, 0.2, 0.5, 0.05);
            }

            // Wing flap sound during soar
            if (tick % 20 == 0 && descentProgress < 0.3) {
                DisplayBuilder.playSound(flightPos, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HellfireWyvernDive(plugin); }
    }

    // ================================================================
    // #26 — DOOM CENTIPEDE (30 blocks)
    // 25 segments scale(0.5,0.4,0.5) winding, head 3 with mandibles,
    // stinger 2 scale(0.2,0.5,0.2). Caterpillar motion (each segment
    // follows prev with 2-tick delay). Z-axis rock ±10° with phase
    // offset via Transform. 4r along body, 25dmg/15t.
    // ================================================================
    public static class DoomCentipede extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private final List<BlockDisplayHandle> headParts = new ArrayList<>();
        private final List<BlockDisplayHandle> stinger = new ArrayList<>();
        private static final int SEG_COUNT = 25;
        private double pathAngle = 0;

        public DoomCentipede(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_centipede", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(25.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 25 BODY SEGMENTS — caterpillar chain
            for (int i = 0; i < SEG_COUNT; i++) {
                double zOff = i * 0.45;
                Location segLoc = center.clone().add(0, 1.0, zOff);
                Material mat = (i % 4 == 0) ? Material.MAGMA_BLOCK : (i % 2 == 0) ? Material.RED_NETHER_BRICKS : Material.NETHERRACK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                h.scale(0.5f, 0.4f, 0.5f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                segments.add(h);
            }

            // HEAD — 3 blocks (2 mandibles + 1 cranium)
            BlockDisplayHandle cranium = displayBuilder.spawnBlock(center.clone().add(0, 1.2, -0.5), Material.POLISHED_BLACKSTONE);
            cranium.scale(0.6f, 0.5f, 0.6f).glow(20, 10, 5).interpolation(3, 0);
            spawnedEntities.add(cranium.entity());
            headParts.add(cranium);

            // Mandibles
            BlockDisplayHandle mandL = displayBuilder.spawnBlock(center.clone().add(-0.3, 1.0, -0.9), Material.BLACKSTONE);
            mandL.scale(0.15f, 0.15f, 0.35f).glow(240, 80, 30).interpolation(2, 0);
            spawnedEntities.add(mandL.entity());
            headParts.add(mandL);
            BlockDisplayHandle mandR = displayBuilder.spawnBlock(center.clone().add(0.3, 1.0, -0.9), Material.BLACKSTONE);
            mandR.scale(0.15f, 0.15f, 0.35f).glow(240, 80, 30).interpolation(2, 0);
            spawnedEntities.add(mandR.entity());
            headParts.add(mandR);

            // STINGER — 2 blocks at tail end scale(0.2, 0.5, 0.2)
            double tailZ = SEG_COUNT * 0.45;
            BlockDisplayHandle st1 = displayBuilder.spawnBlock(center.clone().add(0, 1.3, tailZ + 0.3), Material.RED_CONCRETE);
            st1.scale(0.2f, 0.5f, 0.2f).glow(255, 100, 20).interpolation(3, 0);
            spawnedEntities.add(st1.entity());
            stinger.add(st1);
            BlockDisplayHandle st2 = displayBuilder.spawnBlock(center.clone().add(0, 1.7, tailZ + 0.5), Material.SHROOMLIGHT);
            st2.scale(0.15f, 0.3f, 0.15f).glow(255, 200, 50).interpolation(3, 0);
            spawnedEntities.add(st2.entity());
            stinger.add(st2);

            DisplayBuilder.playSound(center, Sound.ENTITY_SILVERFISH_AMBIENT, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Caterpillar motion: each segment undulates with phase delay
            // Z-axis rock ±10° with phase offset
            pathAngle += 0.03;

            for (int i = 0; i < segments.size(); i++) {
                // Phase-delayed wave: each segment 2 ticks behind the previous
                double phase = tick * 0.15 - i * 0.3;
                double lateralWave = Math.sin(phase) * 1.2;
                double yWave = Math.abs(Math.sin(phase * 0.5)) * 0.4; // caterpillar hump

                // Z-axis rock rotation
                float rockAngle = (float) (Math.toRadians(10) * Math.sin(phase));

                BlockDisplay bd = segments.get(i).entity();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        new Vector3f((float) lateralWave - 0.25f, (float) yWave - 0.2f, -0.25f),
                        new AxisAngle4f(rockAngle, 0, 0, 1),
                        new Vector3f(0.5f, 0.4f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));

                // Teleport along path for forward movement
                double pathX = Math.cos(pathAngle) * 3.0;
                double pathZ = Math.sin(pathAngle) * 3.0 + i * 0.45;
                Location segLoc = center.clone().add(pathX, 1.0, pathZ - SEG_COUNT * 0.225);
                segLoc.setYaw(0); segLoc.setPitch(0);
                bd.teleport(segLoc);
            }

            // Mandible click animation
            if (headParts.size() >= 3) {
                float mandibleOpen = (float) (Math.toRadians(10) * Math.abs(Math.sin(tick * 0.2)));
                for (int i = 1; i <= 2; i++) {
                    BlockDisplay bd = headParts.get(i).entity();
                    float sign = (i == 1) ? 1 : -1;
                    bd.setInterpolationDuration(2);
                    bd.setInterpolationDelay(0);
                    Transformation t = bd.getTransformation();
                    bd.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(mandibleOpen * sign, 0, 1, 0),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Stinger bob
            for (int i = 0; i < stinger.size(); i++) {
                BlockDisplay bd = stinger.get(i).entity();
                float bob = (float) (Math.sin(tick * 0.2 + i) * 0.15);
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        new Vector3f(t.getTranslation().x, t.getTranslation().y + bob, t.getTranslation().z),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // Scuttling particles
            if (tick % 5 == 0) {
                DisplayBuilder.dustParticles(center, 3, 1.5, 200, 50, 10, 0.8f);
            }

            // Sound
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_SILVERFISH_STEP, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DoomCentipede(plugin); }
    }

    // ================================================================
    // #27 — INFERNAL STAG BEETLE (28 blocks)
    // Shell 6 scale(0.9,0.3,1.0), horn 4 scale(0.2,0.15,0.8) forked,
    // legs 6×2 scale(0.15,0.5,0.15), head 2, underbelly 4, wing-hint 4.
    // Horn charge every 50t (tilt down + translate forward 3 blocks).
    // Charge 5r, 50dmg. ENTITY_IRON_GOLEM_ATTACK.
    // ================================================================
    public static class InfernalStagBeetle extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shell = new ArrayList<>();
        private final List<BlockDisplayHandle> hornParts = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>(); // 12 total
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> underbelly = new ArrayList<>();
        private final List<BlockDisplayHandle> wingHints = new ArrayList<>();
        private boolean charging = false;
        private int chargeTickStart = 0;

        public InfernalStagBeetle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_stag_beetle", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(5.0);
            config.setDamage(0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // SHELL — 6 blocks scale(0.9, 0.3, 1.0) — wide flat carapace
            double[][] shellOff = {{0, 1.8, 0}, {0, 1.8, 0.8}, {0, 1.8, -0.8},
                    {0.3, 1.85, 0.4}, {-0.3, 1.85, 0.4}, {0, 1.85, -0.3}};
            for (double[] off : shellOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.RED_NETHER_BRICKS);
                h.scale(0.9f, 0.3f, 1.0f).glow(200, 50, 10).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                shell.add(h);
            }

            // HORN — 4 blocks scale(0.2, 0.15, 0.8) forked pair
            double[][] hornOff = {{-0.15, 1.7, -1.5}, {0.15, 1.7, -1.5}, {-0.25, 1.8, -2.0}, {0.25, 1.8, -2.0}};
            for (double[] off : hornOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BLACKSTONE);
                h.scale(0.2f, 0.15f, 0.8f).glow(20, 10, 5).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                hornParts.add(h);
            }

            // LEGS — 6 pairs (12 total) scale(0.15, 0.5, 0.15)
            double[] legZ = {-0.6, 0, 0.6, -0.6, 0, 0.6};
            double[] legX = {-0.8, -0.9, -0.85, 0.8, 0.9, 0.85};
            for (int i = 0; i < 6; i++) {
                // Upper
                BlockDisplayHandle upper = displayBuilder.spawnBlock(center.clone().add(legX[i], 1.2, legZ[i]), Material.DEEPSLATE);
                upper.scale(0.15f, 0.5f, 0.15f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(upper.entity());
                legs.add(upper);
                // Lower
                BlockDisplayHandle lower = displayBuilder.spawnBlock(center.clone().add(legX[i] * 1.3, 0.6, legZ[i]), Material.DEEPSLATE);
                lower.scale(0.15f, 0.5f, 0.15f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(lower.entity());
                legs.add(lower);
            }

            // HEAD — 2
            BlockDisplayHandle hd1 = displayBuilder.spawnBlock(center.clone().add(0, 1.5, -1.0), Material.POLISHED_BLACKSTONE);
            hd1.scale(0.6f, 0.5f, 0.6f).glow(240, 80, 30).interpolation(3, 0);
            spawnedEntities.add(hd1.entity());
            headBlocks.add(hd1);
            BlockDisplayHandle hd2 = displayBuilder.spawnBlock(center.clone().add(0, 1.65, -1.2), Material.NETHER_BRICKS);
            hd2.scale(0.4f, 0.35f, 0.4f).glow(240, 80, 30).interpolation(3, 0);
            spawnedEntities.add(hd2.entity());
            headBlocks.add(hd2);

            // UNDERBELLY — 4
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 1.3, -0.6 + i * 0.4), Material.MAGMA_BLOCK);
                h.scale(0.7f, 0.25f, 0.6f).glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                underbelly.add(h);
            }

            // WING HINTS — 4 thin plates peeking from shell edges
            double[][] wingOff = {{-0.6, 1.9, -0.2}, {-0.6, 1.9, 0.5}, {0.6, 1.9, -0.2}, {0.6, 1.9, 0.5}};
            for (double[] off : wingOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.ORANGE_CONCRETE);
                h.scale(0.5f, 0.05f, 0.5f).glow(240, 80, 30).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                wingHints.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Leg walking gait
            for (int i = 0; i < legs.size(); i++) {
                boolean groupA = (i % 4 < 2);
                float phase = groupA ? 1.0f : -1.0f;
                float legSwing = (float) (Math.toRadians(15) * Math.sin(tick * 0.15) * phase);

                BlockDisplay bd = legs.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(legSwing, 0, 0, 1),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Charge every 50 ticks: horn tilts down and beetle lunges forward
            if (tick % 50 == 0 && tick > 0) {
                charging = true;
                chargeTickStart = tick;
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
            }

            if (charging) {
                int chargeTick = tick - chargeTickStart;
                if (chargeTick <= 15) {
                    // Horn tilt down animation
                    float tiltProgress = Math.min(1.0f, chargeTick / 8.0f);
                    float hornTilt = (float) (Math.toRadians(-30) * tiltProgress);
                    for (BlockDisplayHandle h : hornParts) {
                        BlockDisplay bd = h.entity();
                        Transformation t = bd.getTransformation();
                        bd.setInterpolationDuration(4);
                        bd.setInterpolationDelay(0);
                        bd.setTransformation(new Transformation(
                                new Vector3f(t.getTranslation().x, t.getTranslation().y, t.getTranslation().z - tiltProgress * 0.2f),
                                new AxisAngle4f(hornTilt, 1, 0, 0),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }

                    // Charge impact at peak
                    if (chargeTick == 12) {
                        Location impactLoc = center.clone().add(0, 1.0, -3.0);
                        triggerImpactDamage(impactLoc);
                        center.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 2, 1, 0.5, 1, 0);
                    }
                } else {
                    // Reset horn
                    for (BlockDisplayHandle h : hornParts) {
                        BlockDisplay bd = h.entity();
                        Transformation t = bd.getTransformation();
                        bd.setInterpolationDuration(5);
                        bd.setInterpolationDelay(0);
                        bd.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(0, 1, 0, 0),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                    charging = false;
                }
            }

            // Ambient beetle hiss
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_SILVERFISH_AMBIENT, 0.6f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new InfernalStagBeetle(plugin); }
    }

    // ================================================================
    // #28 — MAGMA JELLYFISH SWARM (30 blocks)
    // 5 jellyfish (6 each: cap 3 scale(0.8,0.4,0.8), tentacles 3
    // scale(0.1,1.2,0.1)). Float Y=3-8, bob independently. Tentacles
    // sway X-rotation ±15° via Transform. Orbit center at different
    // radii. 5r per jellyfish, 15dmg/10t.
    // ================================================================
    public static class MagmaJellyfishSwarm extends BlockDisplayAttack {
        private static final int JELLY_COUNT = 5;
        private final List<List<BlockDisplayHandle>> jellyCaps = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> jellyTentacles = new ArrayList<>();
        private final double[] orbitRadii = {3.0, 4.5, 5.5, 3.8, 6.0};
        private final double[] orbitSpeeds = {0.025, -0.03, 0.02, -0.035, 0.015};
        private final double[] floatHeights = {4.0, 6.0, 3.5, 7.0, 5.0};
        private final double[] bobPhases = {0, 1.2, 2.5, 0.8, 3.5};

        public MagmaJellyfishSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_jellyfish_swarm", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(15.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] capMats = {Material.MAGMA_BLOCK, Material.SHROOMLIGHT, Material.GLOWSTONE, Material.ORANGE_CONCRETE, Material.RED_CONCRETE};
            Material[] tentMats = {Material.NETHERRACK, Material.RED_NETHER_BRICKS, Material.NETHER_BRICKS, Material.BASALT, Material.DEEPSLATE};

            for (int j = 0; j < JELLY_COUNT; j++) {
                double angle = (2 * Math.PI * j) / JELLY_COUNT;
                double jx = Math.cos(angle) * orbitRadii[j];
                double jz = Math.sin(angle) * orbitRadii[j];
                Location jellyLoc = center.clone().add(jx, floatHeights[j], jz);

                List<BlockDisplayHandle> caps = new ArrayList<>();
                List<BlockDisplayHandle> tents = new ArrayList<>();

                // CAP — 3 blocks scale(0.8, 0.4, 0.8)
                double[][] capOff = {{0, 0.3, 0}, {-0.2, 0, 0.2}, {0.2, 0, -0.2}};
                for (double[] off : capOff) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(jellyLoc.clone().add(off[0], off[1], off[2]), capMats[j]);
                    h.scale(0.8f, 0.4f, 0.8f).glow(255, 100, 20).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    caps.add(h);
                }

                // TENTACLES — 3 long rods scale(0.1, 1.2, 0.1)
                for (int t = 0; t < 3; t++) {
                    double tx = (t - 1) * 0.25;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(jellyLoc.clone().add(tx, -1.0, 0), tentMats[j]);
                    h.scale(0.1f, 1.2f, 0.1f).glow(200, 50, 10).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    tents.add(h);
                }

                jellyCaps.add(caps);
                jellyTentacles.add(tents);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            for (int j = 0; j < JELLY_COUNT; j++) {
                // Orbit position
                double angle = bobPhases[j] + tick * orbitSpeeds[j];
                double jx = Math.cos(angle) * orbitRadii[j];
                double jz = Math.sin(angle) * orbitRadii[j];
                // Independent bobbing
                double yBob = Math.sin(tick * 0.06 + bobPhases[j]) * 0.8;
                Location jellyLoc = center.clone().add(jx, floatHeights[j] + yBob, jz);

                // Move caps
                List<BlockDisplayHandle> caps = jellyCaps.get(j);
                double[][] capOff = {{0, 0.3, 0}, {-0.2, 0, 0.2}, {0.2, 0, -0.2}};
                for (int c = 0; c < caps.size() && c < capOff.length; c++) {
                    Location cLoc = jellyLoc.clone().add(capOff[c][0], capOff[c][1], capOff[c][2]);
                    cLoc.setYaw(0); cLoc.setPitch(0);
                    caps.get(c).entity().teleport(cLoc);
                }

                // Tentacle sway: X-rotation ±15° with phase offset
                List<BlockDisplayHandle> tents = jellyTentacles.get(j);
                for (int t = 0; t < tents.size(); t++) {
                    float swayAngle = (float) (Math.toRadians(15) * Math.sin(tick * 0.12 + j * 1.5 + t * 0.8));
                    BlockDisplay bd = tents.get(t).entity();
                    Transformation tr = bd.getTransformation();
                    bd.setInterpolationDuration(3);
                    bd.setInterpolationDelay(0);
                    bd.setTransformation(new Transformation(
                            tr.getTranslation(),
                            new AxisAngle4f(swayAngle, 1, 0, 0),
                            tr.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));

                    // Teleport tentacle to follow cap
                    double tx = (t - 1) * 0.25;
                    Location tLoc = jellyLoc.clone().add(tx, -1.0, 0);
                    tLoc.setYaw(0); tLoc.setPitch(0);
                    bd.teleport(tLoc);
                }

                // Glow trail particles per jellyfish
                if (tick % 4 == j % 4) {
                    DisplayBuilder.dustParticles(jellyLoc, 2, 0.5, 255, 100, 20, 1.0f);
                }
            }

            // Ambient bubble sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.5f, 0.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MagmaJellyfishSwarm(plugin); }
    }

    // ================================================================
    // #29 — HELLFIRE MINOTAUR CHARGE (32 blocks)
    // Torso 4, legs 4 scale(0.5,1.2,0.5), arms 4 scale(0.4,1.0,0.4),
    // head 2, horns 4 scale(0.15,0.1,0.6), hooves 2, shoulder-armor 4
    // scale(0.7,0.2,0.5), axe 6 (blade + shaft), eyes 2.
    // Charges at player (translation). Arms swing during charge.
    // Impact 6r, 60dmg. ENTITY_RAVAGER_ROAR.
    // ================================================================
    public static class HellfireMinotaurCharge extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> torso = new ArrayList<>();
        private final List<BlockDisplayHandle> legBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> armBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> hornBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> hooves = new ArrayList<>();
        private final List<BlockDisplayHandle> shoulderArmor = new ArrayList<>();
        private final List<BlockDisplayHandle> axeParts = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private Player chargeTarget = null;
        private boolean isCharging = false;
        private int chargeStartTick = 0;
        private double chargeDirX = 0;
        private double chargeDirZ = 0;

        public HellfireMinotaurCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_minotaur_charge", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(6.0);
            config.setDamage(0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // TORSO — 4 wide blocks
            double[][] torsoOff = {{0, 3.0, 0}, {0, 3.8, 0}, {0.3, 3.4, 0}, {-0.3, 3.4, 0}};
            for (double[] off : torsoOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.RED_NETHER_BRICKS);
                h.scale(1.0f, 0.8f, 0.7f).glow(200, 50, 10).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                torso.add(h);
            }

            // LEGS — 4 rod shapes scale(0.5, 1.2, 0.5)
            double[][] legOff = {{-0.4, 1.5, 0}, {0.4, 1.5, 0}, {-0.4, 0.3, 0}, {0.4, 0.3, 0}};
            for (double[] off : legOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BLACKSTONE);
                h.scale(0.5f, 1.2f, 0.5f).glow(20, 10, 5).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                legBlocks.add(h);
            }

            // ARMS — 4 rod shapes scale(0.4, 1.0, 0.4)
            double[][] armOff = {{-1.2, 3.8, 0}, {-1.2, 2.8, 0}, {1.2, 3.8, 0}, {1.2, 2.8, 0}};
            for (double[] off : armOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHERRACK);
                h.scale(0.4f, 1.0f, 0.4f).glow(200, 50, 10).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                armBlocks.add(h);
            }

            // HEAD — 2
            BlockDisplayHandle hd1 = displayBuilder.spawnBlock(center.clone().add(0, 4.8, 0), Material.POLISHED_BLACKSTONE);
            hd1.scale(0.8f, 0.7f, 0.8f).glow(20, 10, 5).interpolation(4, 0);
            spawnedEntities.add(hd1.entity());
            headBlocks.add(hd1);
            BlockDisplayHandle hd2 = displayBuilder.spawnBlock(center.clone().add(0, 4.5, 0.3), Material.NETHER_BRICKS);
            hd2.scale(0.5f, 0.5f, 0.4f).glow(200, 50, 10).interpolation(4, 0);
            spawnedEntities.add(hd2.entity());
            headBlocks.add(hd2);

            // HORNS — 4 scale(0.15, 0.1, 0.6)
            double[][] hornOff = {{-0.4, 5.2, -0.1}, {-0.5, 5.4, -0.3}, {0.4, 5.2, -0.1}, {0.5, 5.4, -0.3}};
            for (double[] off : hornOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BASALT);
                h.scale(0.15f, 0.1f, 0.6f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                hornBlocks.add(h);
            }

            // HOOVES — 2
            BlockDisplayHandle hoofL = displayBuilder.spawnBlock(center.clone().add(-0.4, 0, 0), Material.COAL_BLOCK);
            hoofL.scale(0.4f, 0.3f, 0.5f).glow(20, 10, 5).interpolation(3, 0);
            spawnedEntities.add(hoofL.entity());
            hooves.add(hoofL);
            BlockDisplayHandle hoofR = displayBuilder.spawnBlock(center.clone().add(0.4, 0, 0), Material.COAL_BLOCK);
            hoofR.scale(0.4f, 0.3f, 0.5f).glow(20, 10, 5).interpolation(3, 0);
            spawnedEntities.add(hoofR.entity());
            hooves.add(hoofR);

            // SHOULDER ARMOR — 4 scale(0.7, 0.2, 0.5)
            double[][] shoulderOff = {{-1.0, 4.2, -0.2}, {-1.0, 4.2, 0.2}, {1.0, 4.2, -0.2}, {1.0, 4.2, 0.2}};
            for (double[] off : shoulderOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHER_BRICKS);
                h.scale(0.7f, 0.2f, 0.5f).glow(120, 20, 80).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                shoulderArmor.add(h);
            }

            // AXE — 6 blocks (4 blade + 2 shaft)
            // Shaft (held in right hand area)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(1.6, 3.0 + i * 1.0, 0), Material.BASALT);
                h.scale(0.15f, 1.0f, 0.15f).glow(120, 20, 80).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                axeParts.add(h);
            }
            // Blade
            double[][] bladeOff = {{1.8, 5.0, 0}, {2.0, 5.0, 0}, {1.9, 5.3, 0}, {2.1, 4.8, 0}};
            for (double[] off : bladeOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.RED_CONCRETE);
                h.scale(0.3f, 0.4f, 0.1f).glow(240, 80, 30).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                axeParts.add(h);
            }

            // EYES — 2
            BlockDisplayHandle eyeL = displayBuilder.spawnBlock(center.clone().add(-0.2, 4.9, 0.4), Material.SHROOMLIGHT);
            eyeL.scale(0.12f, 0.12f, 0.12f).glow(255, 50, 20).interpolation(2, 0);
            spawnedEntities.add(eyeL.entity());
            eyeBlocks.add(eyeL);
            BlockDisplayHandle eyeR = displayBuilder.spawnBlock(center.clone().add(0.2, 4.9, 0.4), Material.SHROOMLIGHT);
            eyeR.scale(0.12f, 0.12f, 0.12f).glow(255, 50, 20).interpolation(2, 0);
            spawnedEntities.add(eyeR.entity());
            eyeBlocks.add(eyeR);

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
            w.spawnParticle(Particle.FLAME, center.clone().add(0, 3, 0), 30, 1.5, 2, 1.5, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Find target and initiate charge every 60 ticks
            if (!isCharging && tick % 60 == 0 && tick > 10) {
                chargeTarget = findNearestPlayer(center, 25.0);
                if (chargeTarget != null) {
                    isCharging = true;
                    chargeStartTick = tick;
                    double yaw = yawToward(center, chargeTarget.getLocation());
                    chargeDirX = Math.cos(yaw);
                    chargeDirZ = Math.sin(yaw);
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.6f);
                }
            }

            // Arm swing animation (always active, more intense during charge)
            float swingIntensity = isCharging ? 2.0f : 1.0f;
            float armSwing = (float) (Math.toRadians(25) * Math.sin(tick * 0.2 * swingIntensity));

            // Left arm (indices 0-1)
            for (int i = 0; i < 2 && i < armBlocks.size(); i++) {
                BlockDisplay bd = armBlocks.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(armSwing, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
            // Right arm (indices 2-3) — opposite
            for (int i = 2; i < 4 && i < armBlocks.size(); i++) {
                BlockDisplay bd = armBlocks.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(-armSwing, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Leg pump during charge
            float legPump = (float) (Math.toRadians(20) * Math.sin(tick * 0.25));
            for (int i = 0; i < legBlocks.size(); i++) {
                float phase = (i % 2 == 0) ? 1 : -1;
                BlockDisplay bd = legBlocks.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(legPump * phase, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Charge movement via teleport (translation)
            if (isCharging) {
                int chargeTick = tick - chargeStartTick;
                double speed = 0.4;

                if (chargeTick <= 25) {
                    // Move all entities forward
                    Location newCenter = center.clone().add(chargeDirX * speed, 0, chargeDirZ * speed);
                    setCenter(newCenter);

                    // Ground dust
                    if (chargeTick % 3 == 0) {
                        center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 3, 0.5, 0.1, 0.5, 0.01);
                    }
                }

                // Impact at end of charge
                if (chargeTick == 20) {
                    triggerImpactDamage(center.clone().add(chargeDirX * 2, 1, chargeDirZ * 2));
                    center.getWorld().spawnParticle(Particle.EXPLOSION, center, 3, 1, 1, 1, 0);
                }

                if (chargeTick > 25) {
                    isCharging = false;
                }
            }

            // Hoof stomp sound while moving
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_HORSE_STEP, 0.8f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HellfireMinotaurCharge(plugin); }
    }

    // ================================================================
    // #30 — DOOM SKELETON KING (30 blocks)
    // Ribcage 6 bone scale(0.15,0.8,0.4), spine 4 scale(0.2,0.5,0.2),
    // arms 4 each segmented (8 total), skull 2, crown 4 shroomlight
    // scale(0.4,0.15,0.4), sword 4 scale(0.15,0.1,1.5), pelvis 2.
    // Sword swing animation. Crown Y-bobs separately. 7r, 30dmg/25t.
    // ================================================================
    public static class DoomSkeletonKing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ribcage = new ArrayList<>();
        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> leftArmBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArmBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> skull = new ArrayList<>();
        private final List<BlockDisplayHandle> crown = new ArrayList<>();
        private final List<BlockDisplayHandle> sword = new ArrayList<>();
        private final List<BlockDisplayHandle> pelvis = new ArrayList<>();

        public DoomSkeletonKing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_skeleton_king", AttackType.BLOCK_DISPLAY, 1, "modes/doom/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // RIBCAGE — 6 bone blocks scale(0.15, 0.8, 0.4) — rib-like rods
            for (int i = 0; i < 6; i++) {
                double xOff = (i < 3) ? -0.2 - (i % 3) * 0.1 : 0.2 + (i % 3) * 0.1;
                double yOff = 3.0 + (i % 3) * 0.3;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(xOff, yOff, 0), Material.BONE_BLOCK);
                h.scale(0.15f, 0.8f, 0.4f).glow(220, 200, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                ribcage.add(h);
            }

            // SPINE — 4 scale(0.2, 0.5, 0.2) — vertebrae
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 2.0 + i * 0.6, 0), Material.BONE_BLOCK);
                h.scale(0.2f, 0.5f, 0.2f).glow(220, 200, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                spineBlocks.add(h);
            }

            // LEFT ARM — 4 segmented bones
            double[][] lArmOff = {{-0.8, 4.0, 0}, {-1.2, 3.5, 0}, {-1.5, 3.0, 0}, {-1.7, 2.5, 0}};
            for (double[] off : lArmOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BONE_BLOCK);
                h.scale(0.15f, 0.6f, 0.15f).glow(220, 200, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                leftArmBlocks.add(h);
            }

            // RIGHT ARM — 4 segmented bones (sword hand)
            double[][] rArmOff = {{0.8, 4.0, 0}, {1.2, 3.5, 0}, {1.5, 3.0, 0}, {1.7, 2.5, 0}};
            for (double[] off : rArmOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BONE_BLOCK);
                h.scale(0.15f, 0.6f, 0.15f).glow(220, 200, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                rightArmBlocks.add(h);
            }

            // SKULL — 2
            BlockDisplayHandle sk1 = displayBuilder.spawnBlock(center.clone().add(0, 4.8, 0), Material.BONE_BLOCK);
            sk1.scale(0.6f, 0.6f, 0.6f).glow(220, 200, 180).interpolation(3, 0);
            spawnedEntities.add(sk1.entity());
            skull.add(sk1);
            BlockDisplayHandle sk2 = displayBuilder.spawnBlock(center.clone().add(0, 5.2, 0), Material.BONE_BLOCK);
            sk2.scale(0.5f, 0.4f, 0.5f).glow(220, 200, 180).interpolation(3, 0);
            spawnedEntities.add(sk2.entity());
            skull.add(sk2);

            // CROWN — 4 shroomlight scale(0.4, 0.15, 0.4)
            double[][] crownOff = {{-0.25, 5.6, -0.25}, {0.25, 5.6, -0.25}, {-0.25, 5.6, 0.25}, {0.25, 5.6, 0.25}};
            for (double[] off : crownOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.SHROOMLIGHT);
                h.scale(0.4f, 0.15f, 0.4f).glow(255, 200, 50).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                crown.add(h);
            }

            // SWORD — 4 blocks scale(0.15, 0.1, 1.5) — long blade held by right arm
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(1.8, 2.0 + i * 0.8, 0.3), Material.DEEPSLATE);
                h.scale(0.15f, 0.1f, 1.5f).glow(120, 20, 80).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                sword.add(h);
            }

            // PELVIS — 2
            BlockDisplayHandle p1 = displayBuilder.spawnBlock(center.clone().add(-0.2, 2.0, 0), Material.BONE_BLOCK);
            p1.scale(0.4f, 0.3f, 0.3f).glow(220, 200, 180).interpolation(3, 0);
            spawnedEntities.add(p1.entity());
            pelvis.add(p1);
            BlockDisplayHandle p2 = displayBuilder.spawnBlock(center.clone().add(0.2, 2.0, 0), Material.BONE_BLOCK);
            p2.scale(0.4f, 0.3f, 0.3f).glow(220, 200, 180).interpolation(3, 0);
            spawnedEntities.add(p2.entity());
            pelvis.add(p2);

            DisplayBuilder.playSound(center, Sound.ENTITY_SKELETON_AMBIENT, 1.5f, 0.3f);
            w.spawnParticle(Particle.SOUL, center.clone().add(0, 3, 0), 20, 1, 2, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // SWORD SWING — continuous pendulum via Transform rotation
            float swordSwing = (float) (Math.toRadians(40) * Math.sin(tick * 0.12));
            for (int i = 0; i < sword.size(); i++) {
                BlockDisplay bd = sword.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(4);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(swordSwing, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Right arm follows sword motion (limited swing)
            float armFollow = swordSwing * 0.6f;
            for (int i = 0; i < rightArmBlocks.size(); i++) {
                BlockDisplay bd = rightArmBlocks.get(i).entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(armFollow, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Left arm idle sway
            float leftSway = (float) (Math.toRadians(10) * Math.sin(tick * 0.08));
            for (BlockDisplayHandle h : leftArmBlocks) {
                BlockDisplay bd = h.entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(3);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(leftSway, 1, 0, 0),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // CROWN — separate Y-bob via Transform translation
            float crownBob = (float) (Math.sin(tick * 0.1) * 0.15);
            for (BlockDisplayHandle h : crown) {
                BlockDisplay bd = h.entity();
                Transformation t = bd.getTransformation();
                bd.setInterpolationDuration(2);
                bd.setInterpolationDelay(0);
                bd.setTransformation(new Transformation(
                        new Vector3f(t.getTranslation().x, -0.5f + crownBob, t.getTranslation().z),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // Skull jaw click effect — slight Y oscillation
            if (!skull.isEmpty()) {
                float jawMove = (float) (Math.sin(tick * 0.15) * 0.05);
                BlockDisplay skBd = skull.get(0).entity();
                Transformation st = skBd.getTransformation();
                skBd.setInterpolationDuration(2);
                skBd.setInterpolationDelay(0);
                skBd.setTransformation(new Transformation(
                        new Vector3f(st.getTranslation().x, st.getTranslation().y + jawMove, st.getTranslation().z),
                        new AxisAngle4f().set(st.getLeftRotation()),
                        st.getScale(),
                        new AxisAngle4f().set(st.getRightRotation())
                ));
            }

            // Soul particles from ribcage
            if (tick % 6 == 0) {
                center.getWorld().spawnParticle(Particle.SOUL, center.clone().add(0, 3.5, 0), 2, 0.3, 0.3, 0.3, 0.02);
            }

            // Sword whoosh sound on swing peaks
            if (tick % 26 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
            }

            // Bone rattle ambient
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_SKELETON_STEP, 0.7f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DoomSkeletonKing(plugin); }
    }
}
