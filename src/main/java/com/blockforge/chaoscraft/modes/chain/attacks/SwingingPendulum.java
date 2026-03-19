package com.blockforge.chaoscraft.modes.chain.attacks;

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
 * Chain Mode Block Display -- SWINGING PENDULUM GROUP
 * 15 chain-themed pendulum/swinging/spinning attacks.
 *
 * All attacks use the chain color palette:
 * - Iron gray: RGB(180, 180, 190)
 * - Dark iron: RGB(100, 100, 110)
 * - Rust orange: RGB(180, 100, 40)
 * - Chain glow: RGB(200, 200, 220)
 * - Netherite dark: RGB(60, 50, 50)
 *
 * Rules:
 * - Minimum 10 BlockDisplays per structure
 * - Minimum 40 HP damage (20 hearts)
 * - NO status effects -- damage only
 * - Always spawn straight (yaw=0, pitch=0)
 * - Phase = 1 for all
 * - Full animations with interpolation and teleport
 * - Metallic sounds (BLOCK_CHAIN_PLACE, BLOCK_CHAIN_BREAK, BLOCK_ANVIL_LAND, ENTITY_IRON_GOLEM_HURT)
 */
public final class SwingingPendulum {

    private SwingingPendulum() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new NetheriteWreckingBall(plugin));
        registry.register(new DoublePendulum(plugin));
        registry.register(new ChainFlail(plugin));
        registry.register(new PendulumBlade(plugin));
        registry.register(new TripleChainCradle(plugin));
        registry.register(new OrbitChains(plugin));
        registry.register(new ChainHammer(plugin));
        registry.register(new Centrifuge(plugin));
        registry.register(new ChandelierSwing(plugin));
        registry.register(new GrapplingHook(plugin));
        registry.register(new ChainBolas(plugin));
        registry.register(new SiegeFlail(plugin));
        registry.register(new ClockPendulum(plugin));
        registry.register(new ChainWhip(plugin));
        registry.register(new Gyroscope(plugin));
    }

    // ================================================================
    // 1. NETHERITE WRECKING BALL
    // A 4x4x4 netherite cube on a 10-block chain from overhead anchor.
    // Swings in a wide 120-degree arc. Heavy thud on impact.
    // ================================================================
    public static class NetheriteWreckingBall extends BlockDisplayAttack {

        private BlockDisplayHandle anchor;
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> ballBlocks = new ArrayList<>();
        private double swingAngle = 0;
        private double swingVelocity = 0.06;
        private static final double CHAIN_LENGTH = 10.0;
        private static final double MAX_SWING = Math.toRadians(60);

        public NetheriteWreckingBall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("netherite_wrecking_ball", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location anchorLoc = center.clone().add(0, 12, 0);
            anchor = displayBuilder.spawnBlock(anchorLoc, Material.IRON_BLOCK);
            anchor.scale(3.0f, 1.5f, 3.0f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(anchor.entity());

            for (int i = 0; i < 10; i++) {
                Location linkLoc = anchorLoc.clone().add(0, -(i + 1), 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                link.scale(0.9f, 1.5f, 0.9f).glow(100, 100, 110).interpolation(2, 0);
                chainLinks.add(link);
                spawnedEntities.add(link.entity());
            }

            Location ballCenter = anchorLoc.clone().add(0, -11.5, 0);
            double[][] ballOffsets = {
                {0,0,0}, {1,0,0}, {0,1,0}, {0,0,1},
                {1,1,0}, {1,0,1}, {0,1,1}, {1,1,1},
                {-0.5,0.5,0.5}, {0.5,0.5,-0.5}, {0.5,-0.5,0.5}, {0.5,0.5,1.5}
            };
            for (double[] off : ballOffsets) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                    ballCenter.clone().add(off[0] - 0.5, off[1] - 0.5, off[2] - 0.5),
                    Material.NETHERITE_BLOCK
                );
                b.scale(1.5f, 1.5f, 1.5f).glow(60, 50, 50).interpolation(2, 0);
                ballBlocks.add(b);
                spawnedEntities.add(b.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.5f);
            DisplayBuilder.dustParticles(center, 40, 2.0, 180, 180, 190, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            swingAngle = MAX_SWING * Math.sin(ticksAlive * swingVelocity);
            Location anchorPoint = c.clone().add(0, 12, 0);

            for (int i = 0; i < chainLinks.size(); i++) {
                double fraction = (double)(i + 1) / (CHAIN_LENGTH + 1);
                double segAngle = swingAngle * fraction;
                double xOff = Math.sin(segAngle) * (i + 1);
                double yOff = -(i + 1) * Math.cos(segAngle);
                Location linkTarget = anchorPoint.clone().add(xOff, yOff, 0);
                chainLinks.get(i).entity().teleport(linkTarget);
                chainLinks.get(i).rotate((float) segAngle, 0, 0, 1);
                chainLinks.get(i).interpolation(2, 0);
            }

            double ballXOff = Math.sin(swingAngle) * (CHAIN_LENGTH + 0.5);
            double ballYOff = -(CHAIN_LENGTH + 0.5) * Math.cos(swingAngle);
            Location ballBaseTarget = anchorPoint.clone().add(ballXOff, ballYOff, 0);

            double[][] ballOffsets = {
                {0,0,0}, {1,0,0}, {0,1,0}, {0,0,1},
                {1,1,0}, {1,0,1}, {0,1,1}, {1,1,1},
                {-0.5,0.5,0.5}, {0.5,0.5,-0.5}, {0.5,-0.5,0.5}, {0.5,0.5,1.5}
            };
            for (int i = 0; i < ballBlocks.size(); i++) {
                Location bTarget = ballBaseTarget.clone().add(
                    ballOffsets[i][0] - 0.5, ballOffsets[i][1] - 0.5, ballOffsets[i][2] - 0.5
                );
                ballBlocks.get(i).entity().teleport(bTarget);
            }

            setCenter(ballBaseTarget);

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(ballBaseTarget, 8, 1.5, 60, 50, 50, 1.2f);
            }

            if (Math.abs(swingAngle) < 0.05 && ticksAlive > 10) {
                DisplayBuilder.playSound(ballBaseTarget, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.6f);
                DisplayBuilder.dustParticles(ballBaseTarget.clone().add(0, -1, 0), 25, 2.0, 180, 100, 40, 2.0f);
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(anchorPoint, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetheriteWreckingBall(plugin); }
    }

    // ================================================================
    // 2. DOUBLE PENDULUM
    // Two chains connected end-to-end with iron block weights.
    // Chaotic double pendulum physics. Mesmerizing motion.
    // ================================================================
    public static class DoublePendulum extends BlockDisplayAttack {

        private BlockDisplayHandle pivot;
        private final List<BlockDisplayHandle> upperChain = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerChain = new ArrayList<>();
        private BlockDisplayHandle weight1;
        private BlockDisplayHandle weight2;
        private double theta1 = Math.PI / 3;
        private double theta2 = Math.PI / 2;
        private double omega1 = 0;
        private double omega2 = 0;
        private static final double L1 = 6.0;
        private static final double L2 = 4.0;
        private static final double G = 0.04;
        private static final double M1 = 2.0;
        private static final double M2 = 1.5;

        public DoublePendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("double_pendulum", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location pivotLoc = center.clone().add(0, 12, 0);
            pivot = displayBuilder.spawnBlock(pivotLoc, Material.IRON_BLOCK);
            pivot.scale(2.25f, 2.25f, 2.25f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(pivot.entity());

            for (int i = 0; i < 6; i++) {
                Location linkLoc = pivotLoc.clone().add(0, -(i + 1), 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                link.scale(0.75f, 1.5f, 0.75f).glow(100, 100, 110).interpolation(2, 0);
                upperChain.add(link);
                spawnedEntities.add(link.entity());
            }

            Location w1Loc = pivotLoc.clone().add(0, -7, 0);
            weight1 = displayBuilder.spawnBlock(w1Loc, Material.IRON_BLOCK);
            weight1.scale(1.8f, 1.8f, 1.8f).glow(200, 200, 220).interpolation(2, 0);
            spawnedEntities.add(weight1.entity());

            for (int i = 0; i < 4; i++) {
                Location linkLoc = w1Loc.clone().add(0, -(i + 1), 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                link.scale(0.75f, 1.5f, 0.75f).glow(100, 100, 110).interpolation(2, 0);
                lowerChain.add(link);
                spawnedEntities.add(link.entity());
            }

            Location w2Loc = w1Loc.clone().add(0, -5, 0);
            weight2 = displayBuilder.spawnBlock(w2Loc, Material.IRON_BLOCK);
            weight2.scale(1.5f, 1.5f, 1.5f).glow(200, 200, 220).interpolation(2, 0);
            spawnedEntities.add(weight2.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
            DisplayBuilder.dustParticles(center, 30, 2.0, 200, 200, 220, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location pivotPoint = c.clone().add(0, 12, 0);

            double dt = 1.0;
            double dTheta = theta1 - theta2;
            double denom1 = (M1 + M2) * L1 - M2 * L1 * Math.cos(dTheta) * Math.cos(dTheta);
            double alpha1 = (M2 * L1 * omega1 * omega1 * Math.sin(dTheta) * Math.cos(dTheta)
                    + M2 * G * Math.sin(theta2) * Math.cos(dTheta)
                    + M2 * L2 * omega2 * omega2 * Math.sin(dTheta)
                    - (M1 + M2) * G * Math.sin(theta1)) / denom1;

            double denom2 = (L2 / L1) * denom1;
            double alpha2 = (-M2 * L2 * omega2 * omega2 * Math.sin(dTheta) * Math.cos(dTheta)
                    + (M1 + M2) * G * Math.sin(theta1) * Math.cos(dTheta)
                    - (M1 + M2) * L1 * omega1 * omega1 * Math.sin(dTheta)
                    - (M1 + M2) * G * Math.sin(theta2)) / denom2;

            omega1 += alpha1 * dt;
            omega2 += alpha2 * dt;
            omega1 *= 0.999;
            omega2 *= 0.999;
            theta1 += omega1 * dt;
            theta2 += omega2 * dt;

            double x1 = Math.sin(theta1) * L1;
            double y1 = -Math.cos(theta1) * L1;
            Location joint = pivotPoint.clone().add(x1, y1, 0);

            double x2 = x1 + Math.sin(theta2) * L2;
            double y2 = y1 - Math.cos(theta2) * L2;
            Location tip = pivotPoint.clone().add(x2, y2, 0);

            for (int i = 0; i < upperChain.size(); i++) {
                double frac = (double)(i + 1) / (L1 + 1);
                double lx = Math.sin(theta1) * (i + 1);
                double ly = -Math.cos(theta1) * (i + 1);
                Location linkTarget = pivotPoint.clone().add(lx, ly, 0);
                upperChain.get(i).entity().teleport(linkTarget);
                upperChain.get(i).rotate((float) theta1, 0, 0, 1);
                upperChain.get(i).interpolation(2, 0);
            }
            weight1.entity().teleport(joint);

            for (int i = 0; i < lowerChain.size(); i++) {
                double lx = x1 + Math.sin(theta2) * (i + 1);
                double ly = y1 - Math.cos(theta2) * (i + 1);
                Location linkTarget = pivotPoint.clone().add(lx, ly, 0);
                lowerChain.get(i).entity().teleport(linkTarget);
                lowerChain.get(i).rotate((float) theta2, 0, 0, 1);
                lowerChain.get(i).interpolation(2, 0);
            }
            weight2.entity().teleport(tip);

            setCenter(tip);

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(tip, 6, 0.8, 200, 200, 220, 1.0f);
                DisplayBuilder.dustParticles(joint, 4, 0.5, 180, 180, 190, 0.8f);
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(pivotPoint, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.0f);
            }

            if (Math.abs(omega1) > 0.15 || Math.abs(omega2) > 0.15) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.playSound(tip, Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoublePendulum(plugin); }
    }

    // ================================================================
    // 3. CHAIN FLAIL
    // Spiked ball (netherite core + 6 iron bars) on 5-block chain.
    // Spins horizontally, gaining speed. Sparks at spike tips.
    // ================================================================
    public static class ChainFlail extends BlockDisplayAttack {

        private BlockDisplayHandle hub;
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private BlockDisplayHandle core;
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private double spinAngle = 0;
        private double spinSpeed = 0.03;

        public ChainFlail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_flail", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location hubLoc = center.clone().add(0, 6, 0);
            hub = displayBuilder.spawnBlock(hubLoc, Material.IRON_BLOCK);
            hub.scale(1.5f, 1.5f, 1.5f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(hub.entity());

            for (int i = 0; i < 5; i++) {
                double angle = spinAngle;
                double dist = (i + 1) * 1.2;
                Location linkLoc = hubLoc.clone().add(Math.cos(angle) * dist, -0.5, Math.sin(angle) * dist);
                BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                link.scale(0.75f, 1.2f, 0.75f).glow(100, 100, 110).interpolation(2, 0);
                chainLinks.add(link);
                spawnedEntities.add(link.entity());
            }

            double chainRadius = 6.0;
            Location coreLoc = hubLoc.clone().add(chainRadius, -0.5, 0);
            core = displayBuilder.spawnBlock(coreLoc, Material.NETHERITE_BLOCK);
            core.scale(1.8f, 1.8f, 1.8f).glow(60, 50, 50).interpolation(2, 0);
            spawnedEntities.add(core.entity());

            double[][] spikeDirections = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            for (double[] dir : spikeDirections) {
                Location spikeLoc = coreLoc.clone().add(dir[0] * 0.8, dir[1] * 0.8, dir[2] * 0.8);
                BlockDisplayHandle spike = displayBuilder.spawnBlock(spikeLoc, Material.IRON_BARS);
                spike.scale(0.45f, 1.2f, 0.45f).glow(200, 200, 220).interpolation(2, 0);
                spikes.add(spike);
                spawnedEntities.add(spike.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.7f);
            DisplayBuilder.dustParticles(center, 30, 2.0, 100, 100, 110, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (spinSpeed < 0.2) {
                spinSpeed += 0.0005;
            }
            spinAngle += spinSpeed;

            Location hubLoc = c.clone().add(0, 6, 0);
            double chainRadius = 5.0 + Math.min(spinSpeed * 10, 2.0);

            for (int i = 0; i < chainLinks.size(); i++) {
                double dist = (i + 1) * (chainRadius / 5.0);
                double lx = Math.cos(spinAngle) * dist;
                double lz = Math.sin(spinAngle) * dist;
                double ly = -0.5 - (1.0 - Math.min(spinSpeed * 8, 1.0)) * (5 - i) * 0.3;
                Location linkTarget = hubLoc.clone().add(lx, ly, lz);
                chainLinks.get(i).entity().teleport(linkTarget);
                chainLinks.get(i).rotate((float) spinAngle, 0, 1, 0);
                chainLinks.get(i).interpolation(2, 0);
            }

            double coreX = Math.cos(spinAngle) * chainRadius;
            double coreZ = Math.sin(spinAngle) * chainRadius;
            double coreY = -0.5 - (1.0 - Math.min(spinSpeed * 8, 1.0)) * 1.5;
            Location coreTarget = hubLoc.clone().add(coreX, coreY, coreZ);
            core.entity().teleport(coreTarget);

            double[][] spikeDirections = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
            for (int i = 0; i < spikes.size(); i++) {
                Location spikeTarget = coreTarget.clone().add(
                    spikeDirections[i][0] * 0.8, spikeDirections[i][1] * 0.8, spikeDirections[i][2] * 0.8
                );
                spikes.get(i).entity().teleport(spikeTarget);
            }

            setCenter(coreTarget);

            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle spike : spikes) {
                    Location spikeLoc = spike.entity().getLocation();
                    DisplayBuilder.dustParticles(spikeLoc, 3, 0.2, 255, 200, 50, 0.6f);
                    c.getWorld().spawnParticle(Particle.LAVA, spikeLoc, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(hubLoc, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.9f + (float)(spinSpeed * 3));
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(coreTarget, Sound.ENTITY_IRON_GOLEM_HURT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainFlail(plugin); }
    }

    // ================================================================
    // 4. PENDULUM BLADE
    // Large flat blade (5x1x3 iron) on 8-block chain.
    // Swings in single plane. Blade rotates at extremes.
    // ================================================================
    public static class PendulumBlade extends BlockDisplayAttack {

        private BlockDisplayHandle anchorBlock;
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private double swingAngle = 0;
        private double swingVelocity = 0.05;
        private static final double CHAIN_LENGTH = 8.0;
        private static final double MAX_SWING = Math.toRadians(55);

        public PendulumBlade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pendulum_blade", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(72.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location anchorLoc = center.clone().add(0, 11, 0);
            anchorBlock = displayBuilder.spawnBlock(anchorLoc, Material.IRON_BLOCK);
            anchorBlock.scale(2.25f, 0.75f, 2.25f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(anchorBlock.entity());

            for (int i = 0; i < 8; i++) {
                Location linkLoc = anchorLoc.clone().add(0, -(i + 1), 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                link.scale(0.75f, 1.5f, 0.75f).glow(100, 100, 110).interpolation(2, 0);
                chainLinks.add(link);
                spawnedEntities.add(link.entity());
            }

            Location bladeCenter = anchorLoc.clone().add(0, -9.5, 0);
            double[][] bladeOffsets = {
                {-2, 0, 0}, {-1, 0, 0}, {0, 0, 0}, {1, 0, 0}, {2, 0, 0},
                {-1, 0, -1}, {0, 0, -1}, {1, 0, -1},
                {-1, 0, 1}, {0, 0, 1}, {1, 0, 1},
                {0, -1, 0}, {0, -1, -1}, {0, -1, 1}
            };
            for (double[] off : bladeOffsets) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                    bladeCenter.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK
                );
                b.scale(1.5f, 0.3f, 1.5f).glow(200, 200, 220).interpolation(2, 0);
                bladeBlocks.add(b);
                spawnedEntities.add(b.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.5f);
            DisplayBuilder.dustParticles(center, 35, 2.5, 200, 200, 220, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            swingAngle = MAX_SWING * Math.sin(ticksAlive * swingVelocity);
            Location anchorPoint = c.clone().add(0, 11, 0);

            for (int i = 0; i < chainLinks.size(); i++) {
                double fraction = (double)(i + 1) / (CHAIN_LENGTH + 1);
                double segAngle = swingAngle * fraction;
                double xOff = Math.sin(segAngle) * (i + 1);
                double yOff = -(i + 1) * Math.cos(segAngle);
                Location linkTarget = anchorPoint.clone().add(xOff, yOff, 0);
                chainLinks.get(i).entity().teleport(linkTarget);
                chainLinks.get(i).rotate((float) segAngle, 0, 0, 1);
                chainLinks.get(i).interpolation(2, 0);
            }

            double bladeX = Math.sin(swingAngle) * (CHAIN_LENGTH + 0.5);
            double bladeY = -(CHAIN_LENGTH + 0.5) * Math.cos(swingAngle);
            Location bladeCenter = anchorPoint.clone().add(bladeX, bladeY, 0);

            float bladeTwist = (float)(swingAngle * 0.3);

            double[][] bladeOffsets = {
                {-2, 0, 0}, {-1, 0, 0}, {0, 0, 0}, {1, 0, 0}, {2, 0, 0},
                {-1, 0, -1}, {0, 0, -1}, {1, 0, -1},
                {-1, 0, 1}, {0, 0, 1}, {1, 0, 1},
                {0, -1, 0}, {0, -1, -1}, {0, -1, 1}
            };
            for (int i = 0; i < bladeBlocks.size(); i++) {
                Location bTarget = bladeCenter.clone().add(bladeOffsets[i][0], bladeOffsets[i][1], bladeOffsets[i][2]);
                bladeBlocks.get(i).entity().teleport(bTarget);
                bladeBlocks.get(i).rotate((float) swingAngle + bladeTwist, 0, 0, 1);
                bladeBlocks.get(i).interpolation(2, 0);
            }

            setCenter(bladeCenter);

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleLine(
                    bladeCenter.clone().add(-2, 0, 0), bladeCenter.clone().add(2, 0, 0),
                    Particle.DUST, 4,
                    new Particle.DustOptions(Color.fromRGB(200, 200, 220), 1.0f)
                );
            }

            if (Math.abs(Math.cos(ticksAlive * swingVelocity)) > 0.98) {
                DisplayBuilder.playSound(bladeCenter, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 1.5f);
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(anchorPoint, Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PendulumBlade(plugin); }
    }

    // ================================================================
    // 5. TRIPLE CHAIN CRADLE
    // Newton's cradle: 5 iron block weights on 3 parallel chains.
    // Momentum transfer animation. Click-clack sounds.
    // ================================================================
    public static class TripleChainCradle extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<BlockDisplayHandle> weights = new ArrayList<>();
        private final List<BlockDisplayHandle> frameParts = new ArrayList<>();
        private final double[] weightAngles = new double[5];
        private int transferState = 0;
        private int transferTimer = 0;
        private static final double PENDULUM_LENGTH = 5.0;
        private static final double SPACING = 1.5;

        public TripleChainCradle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("triple_chain_cradle", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location frameTop = center.clone().add(0, 8, 0);
            for (int i = 0; i < 4; i++) {
                double xOff = (i < 2) ? -4 : 4;
                double zOff = (i % 2 == 0) ? -1 : 1;
                BlockDisplayHandle post = displayBuilder.spawnBlock(
                    center.clone().add(xOff, 4, zOff), Material.IRON_BLOCK
                );
                post.scale(0.45f, 6.0f, 0.45f).glow(180, 180, 190).interpolation(2, 0);
                frameParts.add(post);
                spawnedEntities.add(post.entity());
            }
            BlockDisplayHandle crossbar1 = displayBuilder.spawnBlock(frameTop.clone().add(0, 0, -1), Material.IRON_BLOCK);
            crossbar1.scale(12.0f, 0.45f, 0.45f).glow(180, 180, 190).interpolation(2, 0);
            frameParts.add(crossbar1);
            spawnedEntities.add(crossbar1.entity());
            BlockDisplayHandle crossbar2 = displayBuilder.spawnBlock(frameTop.clone().add(0, 0, 1), Material.IRON_BLOCK);
            crossbar2.scale(12.0f, 0.45f, 0.45f).glow(180, 180, 190).interpolation(2, 0);
            frameParts.add(crossbar2);
            spawnedEntities.add(crossbar2.entity());

            for (int i = 0; i < 5; i++) {
                double xOff = (i - 2) * SPACING;
                for (int j = 0; j < 2; j++) {
                    Location chainLoc = frameTop.clone().add(xOff, -(j + 1) * 2, 0);
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                    chain.scale(0.45f, 3.0f, 0.45f).glow(100, 100, 110).interpolation(2, 0);
                    chains.add(chain);
                    spawnedEntities.add(chain.entity());
                }

                Location weightLoc = frameTop.clone().add(xOff, -PENDULUM_LENGTH - 0.5, 0);
                BlockDisplayHandle weight = displayBuilder.spawnBlock(weightLoc, Material.IRON_BLOCK);
                weight.scale(1.8f, 1.8f, 1.8f).glow(200, 200, 220).interpolation(2, 0);
                weights.add(weight);
                spawnedEntities.add(weight.entity());
                weightAngles[i] = 0;
            }

            weightAngles[0] = Math.toRadians(40);

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.8f);
            DisplayBuilder.dustParticles(center, 30, 3.0, 200, 200, 220, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location frameTop = c.clone().add(0, 8, 0);

            transferTimer++;
            double swingSpeed = 0.08;

            switch (transferState) {
                case 0:
                    weightAngles[0] = Math.toRadians(40) * Math.cos(transferTimer * swingSpeed);
                    if (transferTimer > (int)(Math.PI / swingSpeed) && Math.abs(weightAngles[0]) < 0.02) {
                        weightAngles[0] = 0;
                        weightAngles[4] = Math.toRadians(-40);
                        transferState = 1;
                        transferTimer = 0;
                        DisplayBuilder.playSound(frameTop, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.8f);
                    }
                    break;
                case 1:
                    weightAngles[4] = Math.toRadians(-40) * Math.cos(transferTimer * swingSpeed);
                    if (transferTimer > (int)(Math.PI / swingSpeed) && Math.abs(weightAngles[4]) < 0.02) {
                        weightAngles[4] = 0;
                        weightAngles[0] = Math.toRadians(40);
                        transferState = 0;
                        transferTimer = 0;
                        DisplayBuilder.playSound(frameTop, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.8f);
                    }
                    break;
            }

            for (int i = 0; i < 5; i++) {
                double xBase = (i - 2) * SPACING;
                double angle = weightAngles[i];

                double wx = xBase + Math.sin(angle) * PENDULUM_LENGTH;
                double wy = -PENDULUM_LENGTH * Math.cos(angle) - 0.5;
                Location weightTarget = frameTop.clone().add(wx, wy, 0);
                weights.get(i).entity().teleport(weightTarget);

                int chainIdx = i * 2;
                for (int j = 0; j < 2; j++) {
                    double frac = (double)(j + 1) / 3.0;
                    double cx = xBase + Math.sin(angle) * PENDULUM_LENGTH * frac;
                    double cy = -PENDULUM_LENGTH * frac * Math.cos(angle);
                    Location chainTarget = frameTop.clone().add(cx, cy, 0);
                    chains.get(chainIdx + j).entity().teleport(chainTarget);
                    chains.get(chainIdx + j).rotate((float) angle, 0, 0, 1);
                    chains.get(chainIdx + j).interpolation(2, 0);
                }
            }

            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle w : weights) {
                    DisplayBuilder.dustParticles(w.entity().getLocation(), 2, 0.3, 200, 200, 220, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TripleChainCradle(plugin); }
    }

    // ================================================================
    // 6. ORBIT CHAINS
    // 8 chains of varying lengths (3-7 blocks) orbiting a central anchor.
    // Different speeds and heights. Glowing tips.
    // ================================================================
    public static class OrbitChains extends BlockDisplayAttack {

        private BlockDisplayHandle centralAnchor;
        private final List<List<BlockDisplayHandle>> orbitArms = new ArrayList<>();
        private final List<BlockDisplayHandle> tips = new ArrayList<>();
        private final double[] armLengths = {3, 4, 5, 6, 7, 4, 5, 6};
        private final double[] orbitSpeeds = {0.06, 0.08, 0.04, 0.07, 0.05, 0.09, 0.03, 0.065};
        private final double[] heightOffsets = {0, 1.5, -1, 0.5, -0.5, 2.0, -1.5, 1.0};
        private final double[] startAngles = new double[8];

        public OrbitChains(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbit_chains", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(14.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location hubLoc = center.clone().add(0, 6, 0);
            centralAnchor = displayBuilder.spawnBlock(hubLoc, Material.IRON_BLOCK);
            centralAnchor.scale(2.25f, 2.25f, 2.25f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(centralAnchor.entity());

            for (int arm = 0; arm < 8; arm++) {
                startAngles[arm] = (Math.PI * 2 * arm) / 8.0;
                List<BlockDisplayHandle> armLinks = new ArrayList<>();
                int length = (int) armLengths[arm];

                for (int i = 0; i < length; i++) {
                    double angle = startAngles[arm];
                    double dist = (i + 1);
                    Location linkLoc = hubLoc.clone().add(
                        Math.cos(angle) * dist, heightOffsets[arm] - i * 0.3, Math.sin(angle) * dist
                    );
                    BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                    link.scale(0.6f, 1.2f, 0.6f).glow(100, 100, 110).interpolation(2, 0);
                    armLinks.add(link);
                    spawnedEntities.add(link.entity());
                }

                double tipAngle = startAngles[arm];
                Location tipLoc = hubLoc.clone().add(
                    Math.cos(tipAngle) * (length + 0.5),
                    heightOffsets[arm] - length * 0.3,
                    Math.sin(tipAngle) * (length + 0.5)
                );
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.DEEPSLATE);
                tip.scale(1.2f, 1.2f, 1.2f).glow(200, 200, 220).interpolation(2, 0);
                tips.add(tip);
                spawnedEntities.add(tip.entity());

                orbitArms.add(armLinks);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
            DisplayBuilder.dustParticles(center, 40, 3.0, 200, 200, 220, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location hubLoc = c.clone().add(0, 6, 0);

            for (int arm = 0; arm < 8; arm++) {
                double angle = startAngles[arm] + ticksAlive * orbitSpeeds[arm];
                int length = (int) armLengths[arm];
                List<BlockDisplayHandle> armLinks = orbitArms.get(arm);

                for (int i = 0; i < armLinks.size(); i++) {
                    double dist = (i + 1);
                    double wobble = Math.sin(ticksAlive * 0.1 + arm) * 0.2;
                    Location linkTarget = hubLoc.clone().add(
                        Math.cos(angle) * dist,
                        heightOffsets[arm] - i * 0.3 + wobble,
                        Math.sin(angle) * dist
                    );
                    armLinks.get(i).entity().teleport(linkTarget);
                }

                Location tipTarget = hubLoc.clone().add(
                    Math.cos(angle) * (length + 0.5),
                    heightOffsets[arm] - length * 0.3 + Math.sin(ticksAlive * 0.1 + arm) * 0.2,
                    Math.sin(angle) * (length + 0.5)
                );
                tips.get(arm).entity().teleport(tipTarget);

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(tipTarget, 3, 0.3, 200, 200, 220, 0.8f);
                }
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(hubLoc, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.9f);
            }

            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(hubLoc, Sound.ENTITY_IRON_GOLEM_HURT, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitChains(plugin); }
    }

    // ================================================================
    // 7. CHAIN HAMMER
    // Massive T-shaped hammer (6 iron blocks) on 6-block chain.
    // Rises slowly, slams with acceleration. 3 slam cycles.
    // ================================================================
    public static class ChainHammer extends BlockDisplayAttack {

        private BlockDisplayHandle anchorPoint;
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> hammerHead = new ArrayList<>();
        private final List<BlockDisplayHandle> hammerHandle = new ArrayList<>();
        private int slamCycle = 0;
        private int cycleTimer = 0;
        private double hammerY = 0;
        private boolean rising = true;
        private static final double RISE_HEIGHT = 8.0;
        private static final double SLAM_SPEED_BASE = 0.15;

        public ChainHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_hammer", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(78.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(78.0);
            config.setImpactRadius(10.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location anchorLoc = center.clone().add(0, 12, 0);
            anchorPoint = displayBuilder.spawnBlock(anchorLoc, Material.IRON_BLOCK);
            anchorPoint.scale(1.5f, 0.75f, 1.5f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(anchorPoint.entity());

            for (int i = 0; i < 6; i++) {
                Location linkLoc = anchorLoc.clone().add(0, -(i + 1), 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                link.scale(0.9f, 1.5f, 0.9f).glow(100, 100, 110).interpolation(2, 0);
                chainLinks.add(link);
                spawnedEntities.add(link.entity());
            }

            Location handleBase = anchorLoc.clone().add(0, -8, 0);
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle handleBlock = displayBuilder.spawnBlock(
                    handleBase.clone().add(0, -i, 0), Material.DEEPSLATE
                );
                handleBlock.scale(0.9f, 1.5f, 0.9f).glow(60, 50, 50).interpolation(2, 0);
                hammerHandle.add(handleBlock);
                spawnedEntities.add(handleBlock.entity());
            }

            Location headCenter = handleBase.clone().add(0, -3.5, 0);
            double[][] headOffsets = {
                {-2, 0, 0}, {-1, 0, 0}, {0, 0, 0}, {1, 0, 0}, {2, 0, 0},
                {-2, 0, -1}, {-1, 0, -1}, {0, 0, -1}, {1, 0, -1}, {2, 0, -1}
            };
            for (double[] off : headOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    headCenter.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK
                );
                h.scale(1.5f, 2.25f, 1.5f).glow(200, 200, 220).interpolation(2, 0);
                hammerHead.add(h);
                spawnedEntities.add(h.entity());
            }

            hammerY = 0;
            rising = true;
            slamCycle = 0;
            cycleTimer = 0;

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.5f);
            DisplayBuilder.dustParticles(center, 40, 2.0, 180, 180, 190, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (slamCycle >= 3) return;

            cycleTimer++;

            if (rising) {
                hammerY += 0.1;
                if (hammerY >= RISE_HEIGHT) {
                    rising = false;
                    cycleTimer = 0;
                    DisplayBuilder.playSound(c.clone().add(0, 12 + hammerY, 0), Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.5f);
                }
            } else {
                double acceleration = SLAM_SPEED_BASE + cycleTimer * 0.02;
                hammerY -= acceleration;
                if (hammerY <= 0) {
                    hammerY = 0;
                    rising = true;
                    cycleTimer = 0;
                    slamCycle++;

                    Location impactLoc = c.clone().add(0, 0.5, 0);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.4f);
                    DisplayBuilder.dustParticles(impactLoc, 50, 3.0, 180, 100, 40, 2.0f);
                    DisplayBuilder.particleRing(impactLoc, 4.0, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(180, 100, 40), 2.0f));
                    c.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 3, 1, 0.5, 1, 0);
                }
            }

            Location anchorLoc = c.clone().add(0, 12, 0);

            for (int i = 0; i < chainLinks.size(); i++) {
                double frac = (double)(i + 1) / 7.0;
                double linkY = 12 - (i + 1) - (hammerY * (1.0 - frac));
                Location linkTarget = c.clone().add(0, linkY, 0);
                chainLinks.get(i).entity().teleport(linkTarget);
            }

            double handleBaseY = 12 - 8 + hammerY;
            for (int i = 0; i < hammerHandle.size(); i++) {
                Location hTarget = c.clone().add(0, handleBaseY - i, 0);
                hammerHandle.get(i).entity().teleport(hTarget);
            }

            double headY = handleBaseY - 3.5;
            double[][] headOffsets = {
                {-2, 0, 0}, {-1, 0, 0}, {0, 0, 0}, {1, 0, 0}, {2, 0, 0},
                {-2, 0, -1}, {-1, 0, -1}, {0, 0, -1}, {1, 0, -1}, {2, 0, -1}
            };
            for (int i = 0; i < hammerHead.size(); i++) {
                Location hTarget = c.clone().add(headOffsets[i][0], headY + headOffsets[i][1], headOffsets[i][2]);
                hammerHead.get(i).entity().teleport(hTarget);
            }

            if (ticksAlive % 6 == 0 && rising) {
                DisplayBuilder.playSound(anchorLoc, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainHammer(plugin); }
    }

    // ================================================================
    // 8. CENTRIFUGE
    // 4 chains from spinning hub. Start hanging, extend horizontally
    // as speed increases (centrifugal effect). Growing danger radius.
    // ================================================================
    public static class Centrifuge extends BlockDisplayAttack {

        private BlockDisplayHandle hub;
        private final List<List<BlockDisplayHandle>> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> armWeights = new ArrayList<>();
        private double spinAngle = 0;
        private double spinSpeed = 0.02;
        private static final int ARM_LENGTH = 6;

        public Centrifuge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("centrifuge", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location hubLoc = center.clone().add(0, 8, 0);
            hub = displayBuilder.spawnBlock(hubLoc, Material.IRON_BLOCK);
            hub.scale(2.25f, 2.25f, 2.25f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(hub.entity());

            BlockDisplayHandle hubTop = displayBuilder.spawnBlock(hubLoc.clone().add(0, 1, 0), Material.HEAVY_CORE);
            hubTop.scale(1.5f, 0.75f, 1.5f).glow(100, 100, 110).interpolation(2, 0);
            spawnedEntities.add(hubTop.entity());

            for (int arm = 0; arm < 4; arm++) {
                double baseAngle = (Math.PI * 2 * arm) / 4.0;
                List<BlockDisplayHandle> armLinks = new ArrayList<>();

                for (int i = 0; i < ARM_LENGTH; i++) {
                    Location linkLoc = hubLoc.clone().add(0, -(i + 1), 0);
                    BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                    link.scale(0.6f, 1.2f, 0.6f).glow(100, 100, 110).interpolation(2, 0);
                    armLinks.add(link);
                    spawnedEntities.add(link.entity());
                }

                Location weightLoc = hubLoc.clone().add(0, -(ARM_LENGTH + 1), 0);
                BlockDisplayHandle weight = displayBuilder.spawnBlock(weightLoc, Material.ANVIL);
                weight.scale(1.2f, 1.2f, 1.2f).glow(60, 50, 50).interpolation(2, 0);
                armWeights.add(weight);
                spawnedEntities.add(weight.entity());

                arms.add(armLinks);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
            DisplayBuilder.dustParticles(center, 35, 2.0, 180, 180, 190, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (spinSpeed < 0.18) {
                spinSpeed += 0.0004;
            }
            spinAngle += spinSpeed;

            double extensionFactor = Math.min(spinSpeed / 0.18, 1.0);
            Location hubLoc = c.clone().add(0, 8, 0);

            for (int arm = 0; arm < 4; arm++) {
                double baseAngle = (Math.PI * 2 * arm) / 4.0 + spinAngle;
                List<BlockDisplayHandle> armLinks = arms.get(arm);

                for (int i = 0; i < armLinks.size(); i++) {
                    double hangComponent = (1.0 - extensionFactor) * (i + 1);
                    double extendComponent = extensionFactor * (i + 1);
                    double lx = Math.cos(baseAngle) * extendComponent;
                    double lz = Math.sin(baseAngle) * extendComponent;
                    double ly = -hangComponent;
                    Location linkTarget = hubLoc.clone().add(lx, ly, lz);
                    armLinks.get(i).entity().teleport(linkTarget);
                }

                double wHang = (1.0 - extensionFactor) * (ARM_LENGTH + 1);
                double wExtend = extensionFactor * (ARM_LENGTH + 1);
                double wx = Math.cos(baseAngle) * wExtend;
                double wz = Math.sin(baseAngle) * wExtend;
                double wy = -wHang;
                Location weightTarget = hubLoc.clone().add(wx, wy, wz);
                armWeights.get(arm).entity().teleport(weightTarget);

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(weightTarget, 4, 0.4, 180, 100, 40, 0.8f);
                }
            }

            if (ticksAlive % 6 == 0) {
                float pitch = 0.6f + (float)(spinSpeed * 5);
                DisplayBuilder.playSound(hubLoc, Sound.BLOCK_CHAIN_PLACE, 0.8f, pitch);
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(hubLoc, Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.0f);
            }

            if (extensionFactor > 0.5 && ticksAlive % 10 == 0) {
                double radius = extensionFactor * (ARM_LENGTH + 1);
                DisplayBuilder.particleRing(hubLoc, radius, Particle.DUST, 20,
                    new Particle.DustOptions(Color.fromRGB(200, 200, 220), 1.0f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Centrifuge(plugin); }
    }

    // ================================================================
    // 9. CHANDELIER SWING
    // Ornate chandelier (central pillar + 6 hanging chains with tips).
    // Whole structure sways. Individual chains dangle with delay.
    // ================================================================
    public static class ChandelierSwing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> centralPillar = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> hangingChains = new ArrayList<>();
        private final List<BlockDisplayHandle> chainTips = new ArrayList<>();
        private BlockDisplayHandle crownTop;
        private double swayAngle = 0;
        private static final double SWAY_SPEED = 0.04;
        private static final double MAX_SWAY = Math.toRadians(20);

        public ChandelierSwing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chandelier_swing", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location topLoc = center.clone().add(0, 10, 0);
            crownTop = displayBuilder.spawnBlock(topLoc, Material.IRON_BLOCK);
            crownTop.scale(3.0f, 0.75f, 3.0f).glow(200, 200, 220).interpolation(2, 0);
            spawnedEntities.add(crownTop.entity());

            for (int i = 0; i < 4; i++) {
                Location pillarLoc = topLoc.clone().add(0, -(i + 1), 0);
                Material mat = (i % 2 == 0) ? Material.CHAIN : Material.IRON_BLOCK;
                BlockDisplayHandle pillarBlock = displayBuilder.spawnBlock(pillarLoc, mat);
                float pScale = 0.9f - i * 0.075f;
                pillarBlock.scale(pScale, 1.5f, pScale).glow(180, 180, 190).interpolation(2, 0);
                centralPillar.add(pillarBlock);
                spawnedEntities.add(pillarBlock.entity());
            }

            for (int arm = 0; arm < 6; arm++) {
                double angle = (Math.PI * 2 * arm) / 6.0;
                double radius = 2.0;
                Location armBase = topLoc.clone().add(
                    Math.cos(angle) * radius, -2, Math.sin(angle) * radius
                );

                List<BlockDisplayHandle> chainSegments = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    Location chainLoc = armBase.clone().add(0, -(i + 1), 0);
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                    chain.scale(0.45f, 1.2f, 0.45f).glow(100, 100, 110).interpolation(2, 0);
                    chainSegments.add(chain);
                    spawnedEntities.add(chain.entity());
                }
                hangingChains.add(chainSegments);

                Location tipLoc = armBase.clone().add(0, -4, 0);
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.IRON_BARS);
                tip.scale(0.75f, 0.9f, 0.75f).glow(200, 200, 220).interpolation(2, 0);
                chainTips.add(tip);
                spawnedEntities.add(tip.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 1.0f);
            DisplayBuilder.dustParticles(center.clone().add(0, 8, 0), 30, 3.0, 200, 200, 220, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            swayAngle = MAX_SWAY * Math.sin(ticksAlive * SWAY_SPEED);
            Location topLoc = c.clone().add(0, 10, 0);

            double swayX = Math.sin(swayAngle) * 0.5;
            Location swayedTop = topLoc.clone().add(swayX, 0, 0);
            crownTop.entity().teleport(swayedTop);

            for (int i = 0; i < centralPillar.size(); i++) {
                double pillarSway = swayX * (1.0 + i * 0.15);
                Location pillarTarget = topLoc.clone().add(pillarSway, -(i + 1), 0);
                centralPillar.get(i).entity().teleport(pillarTarget);
            }

            for (int arm = 0; arm < 6; arm++) {
                double armAngle = (Math.PI * 2 * arm) / 6.0;
                double radius = 2.0;

                double delayedSway = MAX_SWAY * Math.sin((ticksAlive - arm * 3) * SWAY_SPEED);
                double armSwayX = Math.sin(delayedSway) * 0.8;

                Location armBase = topLoc.clone().add(
                    Math.cos(armAngle) * radius + armSwayX, -2, Math.sin(armAngle) * radius
                );

                List<BlockDisplayHandle> chainSegments = hangingChains.get(arm);
                for (int i = 0; i < chainSegments.size(); i++) {
                    double chainSway = armSwayX * (1.0 + i * 0.2);
                    Location chainTarget = armBase.clone().add(chainSway - armSwayX, -(i + 1), 0);
                    chainSegments.get(i).entity().teleport(chainTarget);
                }

                double tipSway = armSwayX * 1.6;
                Location tipTarget = armBase.clone().add(tipSway - armSwayX, -4, 0);
                chainTips.get(arm).entity().teleport(tipTarget);

                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(tipTarget, 2, 0.2, 200, 200, 220, 0.6f);
                }
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(swayedTop, Sound.BLOCK_CHAIN_PLACE, 0.4f, 1.2f);
            }

            if (ticksAlive % 20 == 0) {
                Location centerGlow = topLoc.clone().add(swayX, -3, 0);
                DisplayBuilder.dustParticles(centerGlow, 10, 1.5, 200, 200, 220, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChandelierSwing(plugin); }
    }

    // ================================================================
    // 10. GRAPPLING HOOK
    // 3 curved iron bar hooks on 10-block chain. Launches outward,
    // arcs through air, retracts. 3 directions sequentially.
    // ================================================================
    public static class GrapplingHook extends BlockDisplayAttack {

        private BlockDisplayHandle launcher;
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> hooks = new ArrayList<>();
        private int launchPhase = 0;
        private int phaseTimer = 0;
        private double launchProgress = 0;
        private final double[] launchDirections = {0, Math.toRadians(120), Math.toRadians(240)};
        private double currentDirection = 0;
        private boolean retracting = false;

        public GrapplingHook(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("grappling_hook", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location launcherLoc = center.clone().add(0, 3, 0);
            launcher = displayBuilder.spawnBlock(launcherLoc, Material.IRON_BLOCK);
            launcher.scale(2.25f, 2.25f, 2.25f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(launcher.entity());

            for (int i = 0; i < 10; i++) {
                BlockDisplayHandle link = displayBuilder.spawnBlock(launcherLoc.clone(), Material.CHAIN);
                link.scale(0.6f, 0.9f, 0.6f).glow(100, 100, 110).interpolation(2, 0);
                chainLinks.add(link);
                spawnedEntities.add(link.entity());
            }

            double[][] hookOffsets = {{0, 0, 0}, {0.6, -0.5, 0.3}, {-0.6, -0.5, -0.3}};
            for (double[] off : hookOffsets) {
                BlockDisplayHandle hook = displayBuilder.spawnBlock(
                    launcherLoc.clone().add(off[0], off[1], off[2]), Material.IRON_BARS
                );
                hook.scale(0.6f, 1.2f, 0.6f).glow(200, 200, 220)
                    .rotate(0.5f, 1, 0, 0).interpolation(2, 0);
                hooks.add(hook);
                spawnedEntities.add(hook.entity());
            }

            launchPhase = 0;
            phaseTimer = 0;
            launchProgress = 0;
            retracting = false;
            currentDirection = launchDirections[0];

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.8f);
            DisplayBuilder.dustParticles(center, 25, 1.5, 180, 180, 190, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (launchPhase >= 3) return;

            phaseTimer++;
            Location launcherLoc = c.clone().add(0, 3, 0);

            if (!retracting) {
                launchProgress += 0.08;
                if (launchProgress >= 1.0) {
                    launchProgress = 1.0;
                    retracting = true;
                    phaseTimer = 0;
                }
            } else {
                launchProgress -= 0.12;
                if (launchProgress <= 0) {
                    launchProgress = 0;
                    retracting = false;
                    launchPhase++;
                    phaseTimer = 0;
                    if (launchPhase < 3) {
                        currentDirection = launchDirections[launchPhase];
                        DisplayBuilder.playSound(launcherLoc, Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.2f);
                    }
                }
            }

            double maxDist = 12.0;
            double dist = launchProgress * maxDist;
            double arcHeight = Math.sin(launchProgress * Math.PI) * 4.0;

            double hookX = Math.cos(currentDirection) * dist;
            double hookZ = Math.sin(currentDirection) * dist;
            double hookY = 3 + arcHeight;
            Location hookTarget = c.clone().add(hookX, hookY, hookZ);

            for (int i = 0; i < chainLinks.size(); i++) {
                double frac = (double)(i + 1) / (chainLinks.size() + 1);
                double lx = Math.cos(currentDirection) * dist * frac;
                double lz = Math.sin(currentDirection) * dist * frac;
                double ly = 3 + Math.sin(frac * Math.PI) * arcHeight * frac;
                Location linkTarget = c.clone().add(lx, ly, lz);
                chainLinks.get(i).entity().teleport(linkTarget);
            }

            double[][] hookOffsets = {{0, 0, 0}, {0.6, -0.5, 0.3}, {-0.6, -0.5, -0.3}};
            for (int i = 0; i < hooks.size(); i++) {
                Location hTarget = hookTarget.clone().add(hookOffsets[i][0], hookOffsets[i][1], hookOffsets[i][2]);
                hooks.get(i).entity().teleport(hTarget);
            }

            setCenter(hookTarget);

            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(hookTarget, 5, 0.5, 200, 200, 220, 0.8f);
            }

            if (!retracting && launchProgress > 0.1 && ticksAlive % 4 == 0) {
                DisplayBuilder.playSound(hookTarget, Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.5f);
            }

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.playSound(launcherLoc, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GrapplingHook(plugin); }
    }

    // ================================================================
    // 11. CHAIN BOLAS
    // 2 iron weights connected by 6-block chain. Spins horizontally
    // like thrown bolas. Rises higher as it spins. Spinning barrier.
    // ================================================================
    public static class ChainBolas extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private BlockDisplayHandle weight1;
        private BlockDisplayHandle weight2;
        private BlockDisplayHandle weightDecor1;
        private BlockDisplayHandle weightDecor2;
        private double spinAngle = 0;
        private double spinSpeed = 0.05;
        private double height = 1.0;
        private static final double CHAIN_HALF_LENGTH = 3.0;

        public ChainBolas(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_bolas", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location baseLoc = center.clone().add(0, height, 0);

            for (int i = 0; i < 6; i++) {
                double offset = (i - 2.5) * 1.0;
                Location linkLoc = baseLoc.clone().add(offset, 0, 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.CHAIN);
                link.scale(0.75f, 0.75f, 1.2f).glow(100, 100, 110).interpolation(2, 0);
                chainLinks.add(link);
                spawnedEntities.add(link.entity());
            }

            Location w1Loc = baseLoc.clone().add(-CHAIN_HALF_LENGTH, 0, 0);
            weight1 = displayBuilder.spawnBlock(w1Loc, Material.IRON_BLOCK);
            weight1.scale(1.95f, 1.95f, 1.95f).glow(200, 200, 220).interpolation(2, 0);
            spawnedEntities.add(weight1.entity());

            weightDecor1 = displayBuilder.spawnBlock(w1Loc.clone().add(0, -0.5, 0), Material.DEEPSLATE);
            weightDecor1.scale(1.2f, 0.75f, 1.2f).glow(60, 50, 50).interpolation(2, 0);
            spawnedEntities.add(weightDecor1.entity());

            Location w2Loc = baseLoc.clone().add(CHAIN_HALF_LENGTH, 0, 0);
            weight2 = displayBuilder.spawnBlock(w2Loc, Material.IRON_BLOCK);
            weight2.scale(1.95f, 1.95f, 1.95f).glow(200, 200, 220).interpolation(2, 0);
            spawnedEntities.add(weight2.entity());

            weightDecor2 = displayBuilder.spawnBlock(w2Loc.clone().add(0, -0.5, 0), Material.DEEPSLATE);
            weightDecor2.scale(1.2f, 0.75f, 1.2f).glow(60, 50, 50).interpolation(2, 0);
            spawnedEntities.add(weightDecor2.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.8f);
            DisplayBuilder.dustParticles(center, 30, 2.0, 200, 200, 220, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (spinSpeed < 0.2) {
                spinSpeed += 0.0003;
            }
            spinAngle += spinSpeed;

            if (height < 8.0) {
                height += 0.015;
            }

            Location baseLoc = c.clone().add(0, height, 0);

            double w1x = Math.cos(spinAngle) * CHAIN_HALF_LENGTH;
            double w1z = Math.sin(spinAngle) * CHAIN_HALF_LENGTH;
            double w2x = Math.cos(spinAngle + Math.PI) * CHAIN_HALF_LENGTH;
            double w2z = Math.sin(spinAngle + Math.PI) * CHAIN_HALF_LENGTH;

            Location w1Target = baseLoc.clone().add(w1x, 0, w1z);
            Location w2Target = baseLoc.clone().add(w2x, 0, w2z);
            weight1.entity().teleport(w1Target);
            weight2.entity().teleport(w2Target);
            weightDecor1.entity().teleport(w1Target.clone().add(0, -0.5, 0));
            weightDecor2.entity().teleport(w2Target.clone().add(0, -0.5, 0));

            for (int i = 0; i < chainLinks.size(); i++) {
                double frac = (double)(i + 1) / (chainLinks.size() + 1);
                double lx = w2x + (w1x - w2x) * frac;
                double lz = w2z + (w1z - w2z) * frac;
                Location linkTarget = baseLoc.clone().add(lx, 0, lz);
                chainLinks.get(i).entity().teleport(linkTarget);
                chainLinks.get(i).rotate((float) spinAngle, 0, 1, 0);
                chainLinks.get(i).interpolation(2, 0);
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(w1Target, 3, 0.3, 200, 200, 220, 0.8f);
                DisplayBuilder.dustParticles(w2Target, 3, 0.3, 200, 200, 220, 0.8f);
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(baseLoc, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.8f + (float)(spinSpeed * 3));
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.particleRing(baseLoc, CHAIN_HALF_LENGTH, Particle.DUST, 16,
                    new Particle.DustOptions(Color.fromRGB(180, 180, 190), 0.8f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainBolas(plugin); }
    }

    // ================================================================
    // 12. SIEGE FLAIL
    // Massive 5x5 netherite ball on thick 4-block chain (2x2 scale).
    // Spins overhead, slams into ground. 3-second wind-up. Devastating.
    // ================================================================
    public static class SiegeFlail extends BlockDisplayAttack {

        private BlockDisplayHandle mountPoint;
        private final List<BlockDisplayHandle> thickChain = new ArrayList<>();
        private final List<BlockDisplayHandle> ballBlocks = new ArrayList<>();
        private double windUpAngle = 0;
        private double windUpSpeed = 0.04;
        private int phase = 0; // 0=wind-up, 1=slam, 2=impact
        private int phaseTimer = 0;
        private double slamProgress = 0;

        public SiegeFlail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("siege_flail", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(84.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(84.0);
            config.setImpactRadius(12.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location mountLoc = center.clone().add(0, 10, 0);
            mountPoint = displayBuilder.spawnBlock(mountLoc, Material.IRON_BLOCK);
            mountPoint.scale(3.0f, 3.0f, 3.0f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(mountPoint.entity());

            for (int i = 0; i < 4; i++) {
                Location chainLoc = mountLoc.clone().add(0, -(i + 1) * 1.5, 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                link.scale(2.25f, 2.25f, 2.25f).glow(100, 100, 110).interpolation(2, 0);
                thickChain.add(link);
                spawnedEntities.add(link.entity());
            }

            Location ballCenter = mountLoc.clone().add(0, -8, 0);
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        double dist = Math.sqrt(x*x + y*y + z*z);
                        if (dist <= 2.5) {
                            BlockDisplayHandle b = displayBuilder.spawnBlock(
                                ballCenter.clone().add(x * 0.8, y * 0.8, z * 0.8), Material.NETHERITE_BLOCK
                            );
                            b.scale(1.2f, 1.2f, 1.2f).glow(60, 50, 50).interpolation(2, 0);
                            ballBlocks.add(b);
                            spawnedEntities.add(b.entity());
                        }
                    }
                }
            }

            this.phase = 0;
            phaseTimer = 0;
            windUpAngle = 0;

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 2.0f, 0.4f);
            DisplayBuilder.dustParticles(center, 50, 3.0, 60, 50, 50, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            phaseTimer++;
            Location mountLoc = c.clone().add(0, 10, 0);

            if (phase == 0) {
                if (windUpSpeed < 0.2) {
                    windUpSpeed += 0.002;
                }
                windUpAngle += windUpSpeed;

                double orbitRadius = 5.0;
                double ballX = Math.cos(windUpAngle) * orbitRadius;
                double ballZ = Math.sin(windUpAngle) * orbitRadius;
                double ballY = 10 - 3;
                Location ballCenter = c.clone().add(ballX, ballY, ballZ);

                updateBallPosition(ballCenter);
                updateChainToTarget(mountLoc, ballCenter);

                if (phaseTimer % 4 == 0) {
                    DisplayBuilder.dustParticles(ballCenter, 6, 1.0, 60, 50, 50, 1.0f);
                }

                if (phaseTimer % 6 == 0) {
                    float pitch = 0.5f + (float)(windUpSpeed * 3);
                    DisplayBuilder.playSound(mountLoc, Sound.BLOCK_CHAIN_PLACE, 1.0f, pitch);
                }

                if (phaseTimer >= 60) {
                    phase = 1;
                    phaseTimer = 0;
                    slamProgress = 0;
                    DisplayBuilder.playSound(mountLoc, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.4f);
                }
            } else if (phase == 1) {
                slamProgress += 0.06;
                if (slamProgress > 1.0) slamProgress = 1.0;

                double startY = 7;
                double endY = 0.5;
                double currentY = startY - (startY - endY) * slamProgress * slamProgress;
                double dirX = Math.cos(windUpAngle) * 5.0 * (1.0 - slamProgress);
                double dirZ = Math.sin(windUpAngle) * 5.0 * (1.0 - slamProgress);

                Location ballCenter = c.clone().add(dirX, currentY, dirZ);
                updateBallPosition(ballCenter);
                updateChainToTarget(mountLoc, ballCenter);

                if (slamProgress >= 1.0) {
                    phase = 2;
                    phaseTimer = 0;

                    Location impactLoc = c.clone().add(0, 0.5, 0);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
                    DisplayBuilder.dustParticles(impactLoc, 80, 4.0, 180, 100, 40, 2.5f);
                    c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impactLoc, 2, 1, 0.5, 1, 0);

                    for (double radius = 1; radius <= 6; radius += 1.5) {
                        DisplayBuilder.particleRing(impactLoc, radius, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(180, 100, 40), 2.0f));
                    }
                }
            }
        }

        private void updateBallPosition(Location ballCenter) {
            int idx = 0;
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        double dist = Math.sqrt(x*x + y*y + z*z);
                        if (dist <= 2.5 && idx < ballBlocks.size()) {
                            ballBlocks.get(idx).entity().teleport(
                                ballCenter.clone().add(x * 0.8, y * 0.8, z * 0.8)
                            );
                            idx++;
                        }
                    }
                }
            }
        }

        private void updateChainToTarget(Location mount, Location target) {
            for (int i = 0; i < thickChain.size(); i++) {
                double frac = (double)(i + 1) / (thickChain.size() + 1);
                double lx = mount.getX() + (target.getX() - mount.getX()) * frac;
                double ly = mount.getY() + (target.getY() - mount.getY()) * frac;
                double lz = mount.getZ() + (target.getZ() - mount.getZ()) * frac;
                thickChain.get(i).entity().teleport(new Location(mount.getWorld(), lx, ly, lz));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SiegeFlail(plugin); }
    }

    // ================================================================
    // 13. CLOCK PENDULUM
    // Clock-like structure with 2 vertical chain "hands" and iron face.
    // Pendulum swings with mechanical rhythm. Tick-tock. Face rotates.
    // ================================================================
    public static class ClockPendulum extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> faceBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> pendulumChain = new ArrayList<>();
        private BlockDisplayHandle pendulumWeight;
        private BlockDisplayHandle minuteHand;
        private BlockDisplayHandle hourHand;
        private double pendulumAngle = 0;
        private double faceRotation = 0;
        private static final double PENDULUM_LENGTH = 6.0;
        private static final double SWING_SPEED = 0.06;
        private static final double MAX_ANGLE = Math.toRadians(35);

        public ClockPendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("clock_pendulum", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location faceCenterLoc = center.clone().add(0, 8, 0);

            double[][] faceOffsets = {
                {0,0,0}, {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0},
                {1,1,0}, {1,-1,0}, {-1,1,0}, {-1,-1,0},
                {2,0,0}, {-2,0,0}, {0,2,0}, {0,-2,0}
            };
            for (double[] off : faceOffsets) {
                BlockDisplayHandle fb = displayBuilder.spawnBlock(
                    faceCenterLoc.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK
                );
                fb.scale(1.5f, 1.5f, 0.45f).glow(200, 200, 220).interpolation(2, 0);
                faceBlocks.add(fb);
                spawnedEntities.add(fb.entity());
            }

            minuteHand = displayBuilder.spawnBlock(faceCenterLoc.clone().add(0, 0.5, 0.2), Material.DEEPSLATE);
            minuteHand.scale(0.23f, 2.25f, 0.23f).glow(60, 50, 50).interpolation(2, 0);
            spawnedEntities.add(minuteHand.entity());

            hourHand = displayBuilder.spawnBlock(faceCenterLoc.clone().add(0.3, 0, 0.2), Material.DEEPSLATE);
            hourHand.scale(1.5f, 0.23f, 0.23f).glow(60, 50, 50).interpolation(2, 0);
            spawnedEntities.add(hourHand.entity());

            Location pendulumTop = faceCenterLoc.clone().add(0, -2.5, 0);
            for (int i = 0; i < 6; i++) {
                Location chainLoc = pendulumTop.clone().add(0, -(i + 1), 0);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                chain.scale(0.75f, 1.5f, 0.75f).glow(100, 100, 110).interpolation(2, 0);
                pendulumChain.add(chain);
                spawnedEntities.add(chain.entity());
            }

            Location weightLoc = pendulumTop.clone().add(0, -7, 0);
            pendulumWeight = displayBuilder.spawnBlock(weightLoc, Material.HEAVY_CORE);
            pendulumWeight.scale(2.25f, 0.75f, 2.25f).glow(180, 100, 40).interpolation(2, 0);
            spawnedEntities.add(pendulumWeight.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.5f);
            DisplayBuilder.dustParticles(faceCenterLoc, 30, 2.5, 200, 200, 220, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            pendulumAngle = MAX_ANGLE * Math.sin(ticksAlive * SWING_SPEED);
            faceRotation += 0.005;

            Location faceCenterLoc = c.clone().add(0, 8, 0);

            double[][] faceOffsets = {
                {0,0,0}, {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0},
                {1,1,0}, {1,-1,0}, {-1,1,0}, {-1,-1,0},
                {2,0,0}, {-2,0,0}, {0,2,0}, {0,-2,0}
            };
            for (int i = 0; i < faceBlocks.size(); i++) {
                double ox = faceOffsets[i][0];
                double oy = faceOffsets[i][1];
                double rotX = ox * Math.cos(faceRotation) - oy * Math.sin(faceRotation);
                double rotY = ox * Math.sin(faceRotation) + oy * Math.cos(faceRotation);
                Location fTarget = faceCenterLoc.clone().add(rotX, rotY, 0);
                faceBlocks.get(i).entity().teleport(fTarget);
            }

            double minAngle = ticksAlive * 0.02;
            minuteHand.entity().teleport(faceCenterLoc.clone().add(
                Math.sin(minAngle) * 0.7, Math.cos(minAngle) * 0.7, 0.2
            ));
            minuteHand.rotate((float) -minAngle, 0, 0, 1);
            minuteHand.interpolation(2, 0);

            double hrAngle = ticksAlive * 0.003;
            hourHand.entity().teleport(faceCenterLoc.clone().add(
                Math.sin(hrAngle) * 0.5, Math.cos(hrAngle) * 0.5, 0.2
            ));
            hourHand.rotate((float) -hrAngle, 0, 0, 1);
            hourHand.interpolation(2, 0);

            Location pendulumTop = faceCenterLoc.clone().add(0, -2.5, 0);
            for (int i = 0; i < pendulumChain.size(); i++) {
                double frac = (double)(i + 1) / PENDULUM_LENGTH;
                double segAngle = pendulumAngle * frac;
                double px = Math.sin(segAngle) * (i + 1);
                double py = -(i + 1) * Math.cos(segAngle);
                Location chainTarget = pendulumTop.clone().add(px, py, 0);
                pendulumChain.get(i).entity().teleport(chainTarget);
                pendulumChain.get(i).rotate((float) segAngle, 0, 0, 1);
                pendulumChain.get(i).interpolation(2, 0);
            }

            double wx = Math.sin(pendulumAngle) * (PENDULUM_LENGTH + 1);
            double wy = -(PENDULUM_LENGTH + 1) * Math.cos(pendulumAngle);
            Location weightTarget = pendulumTop.clone().add(wx, wy, 0);
            pendulumWeight.entity().teleport(weightTarget);

            setCenter(weightTarget);

            boolean atExtreme = Math.abs(Math.cos(ticksAlive * SWING_SPEED)) > 0.98;
            if (atExtreme) {
                DisplayBuilder.playSound(faceCenterLoc, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.8f);
            }

            int swingHalf = (int)(ticksAlive * SWING_SPEED / Math.PI);
            if (ticksAlive % (int)(Math.PI / SWING_SPEED / 2) < 2) {
                DisplayBuilder.playSound(faceCenterLoc, Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.5f);
            }

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(weightTarget, 4, 0.4, 180, 100, 40, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ClockPendulum(plugin); }
    }

    // ================================================================
    // 14. CHAIN WHIP
    // 12 chain links forming a segmented whip. Cracks outward in
    // sine wave. Snapping sound at crack. Quick forward-and-back.
    // ================================================================
    public static class ChainWhip extends BlockDisplayAttack {

        private BlockDisplayHandle handle;
        private final List<BlockDisplayHandle> whipSegments = new ArrayList<>();
        private BlockDisplayHandle crackTip;
        private int strikeTimer = 0;
        private int strikeCount = 0;
        private double strikeDirection = 0;
        private static final int MAX_STRIKES = 8;
        private static final int STRIKE_DURATION = 30;
        private static final int REST_DURATION = 15;

        public ChainWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_whip", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location handleLoc = center.clone().add(0, 3, 0);
            handle = displayBuilder.spawnBlock(handleLoc, Material.IRON_BLOCK);
            handle.scale(0.9f, 2.25f, 0.9f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(handle.entity());

            for (int i = 0; i < 12; i++) {
                Location segLoc = handleLoc.clone().add(i + 1, 0, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.CHAIN);
                float segScale = 0.75f - i * 0.038f;
                seg.scale(segScale, 0.9f, segScale).glow(100, 100, 110).interpolation(2, 0);
                whipSegments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            crackTip = displayBuilder.spawnBlock(handleLoc.clone().add(13, 0, 0), Material.IRON_BARS);
            crackTip.scale(0.45f, 0.6f, 0.45f).glow(200, 200, 220).interpolation(2, 0);
            spawnedEntities.add(crackTip.entity());

            strikeTimer = 0;
            strikeCount = 0;
            strikeDirection = 0;

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.8f);
            DisplayBuilder.dustParticles(center, 25, 2.0, 100, 100, 110, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (strikeCount >= MAX_STRIKES) return;

            strikeTimer++;
            Location handleLoc = c.clone().add(0, 3, 0);

            boolean inStrike = strikeTimer <= STRIKE_DURATION;

            if (!inStrike) {
                if (strikeTimer >= STRIKE_DURATION + REST_DURATION) {
                    strikeTimer = 0;
                    strikeCount++;
                    strikeDirection = Math.random() * Math.PI * 2;
                }

                for (int i = 0; i < whipSegments.size(); i++) {
                    double dist = (i + 1) * 0.5;
                    double dx = Math.cos(strikeDirection) * dist;
                    double dz = Math.sin(strikeDirection) * dist;
                    double hang = -i * 0.15;
                    Location segTarget = handleLoc.clone().add(dx, hang, dz);
                    whipSegments.get(i).entity().teleport(segTarget);
                }
                crackTip.entity().teleport(handleLoc.clone().add(
                    Math.cos(strikeDirection) * 6.5, -12 * 0.15, Math.sin(strikeDirection) * 6.5
                ));
                return;
            }

            double strikeProgress = (double) strikeTimer / STRIKE_DURATION;

            for (int i = 0; i < whipSegments.size(); i++) {
                double segFrac = (double)(i + 1) / 13.0;
                double waveProgress = strikeProgress * 3.0 - segFrac * 2.0;
                waveProgress = Math.max(0, Math.min(1, waveProgress));

                double maxDist = (i + 1) * 1.0;
                double dist = maxDist * waveProgress;
                double sineWave = Math.sin(segFrac * Math.PI * 2 + strikeProgress * Math.PI * 3) * (1.0 - segFrac) * 1.5;

                double dx = Math.cos(strikeDirection) * dist;
                double dz = Math.sin(strikeDirection) * dist;

                double perpX = -Math.sin(strikeDirection) * sineWave;
                double perpZ = Math.cos(strikeDirection) * sineWave;

                double yOff = Math.sin(waveProgress * Math.PI) * 0.5;

                Location segTarget = handleLoc.clone().add(dx + perpX, yOff, dz + perpZ);
                whipSegments.get(i).entity().teleport(segTarget);
            }

            double tipDist = 12.0 * Math.min(strikeProgress * 1.5, 1.0);
            double tipDx = Math.cos(strikeDirection) * tipDist;
            double tipDz = Math.sin(strikeDirection) * tipDist;
            Location tipTarget = handleLoc.clone().add(tipDx, 0, tipDz);
            crackTip.entity().teleport(tipTarget);

            setCenter(tipTarget);

            if (strikeProgress > 0.7 && strikeProgress < 0.8) {
                DisplayBuilder.playSound(tipTarget, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 1.8f);
                DisplayBuilder.dustParticles(tipTarget, 15, 0.5, 255, 200, 50, 1.0f);
                c.getWorld().spawnParticle(Particle.CRIT, tipTarget, 10, 0.3, 0.3, 0.3, 0.1);
            }

            if (ticksAlive % 3 == 0 && inStrike) {
                DisplayBuilder.playSound(handleLoc, Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.2f);
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(tipTarget, 3, 0.3, 100, 100, 110, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainWhip(plugin); }
    }

    // ================================================================
    // 15. GYROSCOPE
    // 3 concentric rings of chains (radii 2, 4, 6) spinning on
    // different axes (X, Y, Z). Stabilizes then destabilizes cyclically.
    // ================================================================
    public static class Gyroscope extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> middleRing = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private BlockDisplayHandle coreBlock;
        private BlockDisplayHandle coreDecor1;
        private BlockDisplayHandle coreDecor2;
        private double xAngle = 0;
        private double yAngle = 0;
        private double zAngle = 0;
        private double xSpeed = 0.04;
        private double ySpeed = 0.06;
        private double zSpeed = 0.08;
        private boolean stable = true;
        private int stabilityTimer = 0;

        public Gyroscope(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gyroscope", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(13.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location coreLoc = center.clone().add(0, 6, 0);
            coreBlock = displayBuilder.spawnBlock(coreLoc, Material.IRON_BLOCK);
            coreBlock.scale(1.5f, 1.5f, 1.5f).glow(200, 200, 220).interpolation(2, 0);
            spawnedEntities.add(coreBlock.entity());

            coreDecor1 = displayBuilder.spawnBlock(coreLoc.clone().add(0, 0.5, 0), Material.HEAVY_CORE);
            coreDecor1.scale(0.9f, 0.45f, 0.9f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(coreDecor1.entity());

            coreDecor2 = displayBuilder.spawnBlock(coreLoc.clone().add(0, -0.5, 0), Material.HEAVY_CORE);
            coreDecor2.scale(0.9f, 0.45f, 0.9f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(coreDecor2.entity());

            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8.0;
                double x = Math.cos(angle) * 2.0;
                double y = Math.sin(angle) * 2.0;
                BlockDisplayHandle link = displayBuilder.spawnBlock(
                    coreLoc.clone().add(x, y, 0), Material.CHAIN
                );
                link.scale(0.75f, 0.75f, 0.75f).glow(100, 100, 110).interpolation(2, 0);
                innerRing.add(link);
                spawnedEntities.add(link.entity());
            }

            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8.0;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                BlockDisplayHandle link = displayBuilder.spawnBlock(
                    coreLoc.clone().add(x, 0, z), Material.CHAIN
                );
                link.scale(0.9f, 0.9f, 0.9f).glow(180, 180, 190).interpolation(2, 0);
                middleRing.add(link);
                spawnedEntities.add(link.entity());
            }

            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8.0;
                double z = Math.cos(angle) * 6.0;
                double y = Math.sin(angle) * 6.0;
                BlockDisplayHandle link = displayBuilder.spawnBlock(
                    coreLoc.clone().add(0, y, z), Material.CHAIN
                );
                link.scale(1.05f, 1.05f, 1.05f).glow(200, 200, 220).interpolation(2, 0);
                outerRing.add(link);
                spawnedEntities.add(link.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
            DisplayBuilder.dustParticles(coreLoc, 40, 3.0, 200, 200, 220, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            stabilityTimer++;
            if (stabilityTimer > 100) {
                stable = !stable;
                stabilityTimer = 0;
                if (!stable) {
                    DisplayBuilder.playSound(c.clone().add(0, 6, 0), Sound.BLOCK_CHAIN_BREAK, 1.2f, 0.5f);
                } else {
                    DisplayBuilder.playSound(c.clone().add(0, 6, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.2f);
                }
            }

            double wobble = stable ? 0 : Math.sin(ticksAlive * 0.15) * 0.3;
            double speedMult = stable ? 1.0 : 1.5 + Math.sin(ticksAlive * 0.1) * 0.5;

            xAngle += xSpeed * speedMult;
            yAngle += ySpeed * speedMult;
            zAngle += zSpeed * speedMult;

            Location coreLoc = c.clone().add(wobble, 6, wobble * 0.7);

            for (int i = 0; i < 8; i++) {
                double baseAngle = (Math.PI * 2 * i) / 8.0 + xAngle;
                double rx = Math.cos(baseAngle) * 2.0;
                double ry = Math.sin(baseAngle) * 2.0;

                double finalX = rx;
                double finalY = ry * Math.cos(yAngle * 0.3) - 0 * Math.sin(yAngle * 0.3);
                double finalZ = ry * Math.sin(yAngle * 0.3);

                Location linkTarget = coreLoc.clone().add(finalX, finalY, finalZ);
                innerRing.get(i).entity().teleport(linkTarget);
            }

            for (int i = 0; i < 8; i++) {
                double baseAngle = (Math.PI * 2 * i) / 8.0 + yAngle;
                double rx = Math.cos(baseAngle) * 4.0;
                double rz = Math.sin(baseAngle) * 4.0;

                double finalX = rx * Math.cos(zAngle * 0.2);
                double finalY = rx * Math.sin(zAngle * 0.2);
                double finalZ = rz;

                Location linkTarget = coreLoc.clone().add(finalX, finalY, finalZ);
                middleRing.get(i).entity().teleport(linkTarget);
            }

            for (int i = 0; i < 8; i++) {
                double baseAngle = (Math.PI * 2 * i) / 8.0 + zAngle;
                double rz = Math.cos(baseAngle) * 6.0;
                double ry = Math.sin(baseAngle) * 6.0;

                double finalZ = rz * Math.cos(xAngle * 0.15) - ry * Math.sin(xAngle * 0.15);
                double finalY = rz * Math.sin(xAngle * 0.15) + ry * Math.cos(xAngle * 0.15);
                double finalX = Math.sin(baseAngle + yAngle * 0.1) * 0.5;

                Location linkTarget = coreLoc.clone().add(finalX, finalY, finalZ);
                outerRing.get(i).entity().teleport(linkTarget);
            }

            if (ticksAlive % 5 == 0) {
                if (!stable) {
                    DisplayBuilder.dustParticles(coreLoc, 8, 2.0, 180, 100, 40, 1.2f);
                } else {
                    DisplayBuilder.dustParticles(coreLoc, 4, 1.0, 200, 200, 220, 0.8f);
                }
            }

            if (ticksAlive % 8 == 0) {
                float pitch = stable ? 1.0f : 0.6f;
                DisplayBuilder.playSound(coreLoc, Sound.BLOCK_CHAIN_PLACE, 0.6f, pitch);
            }

            if (!stable && ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(coreLoc, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.8f);
                for (int ring = 0; ring < 3; ring++) {
                    double radius = (ring + 1) * 2.0;
                    DisplayBuilder.particleRing(coreLoc, radius, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(180, 100, 40), 1.0f));
                }
            }

            if (ticksAlive % 3 == 0 && !stable) {
                for (BlockDisplayHandle link : outerRing) {
                    Location linkLoc = link.entity().getLocation();
                    c.getWorld().spawnParticle(Particle.LAVA, linkLoc, 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Gyroscope(plugin); }
    }
}
