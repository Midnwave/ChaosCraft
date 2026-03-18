package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class GeometricShapes {
    private GeometricShapes() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HollowCube(plugin));
        registry.register(new VoidTorus(plugin));
        registry.register(new DoubleHelixSpire(plugin));
        registry.register(new InfiniteTesseractFrame(plugin));
        registry.register(new MobiusRibbon(plugin));
        registry.register(new OctahedronBurst(plugin));
        registry.register(new ClockworkOrreryRing(plugin));
        registry.register(new FractalSierpinskiFrame(plugin));
        registry.register(new GoldenRatioSpiral(plugin));
        registry.register(new VoidIcosahedron(plugin));
    }

    // ================================================================
    // 51. HOLLOW CUBE
    // ================================================================
    public static class HollowCube extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> corners = new ArrayList<>();
        private final List<BlockDisplayHandle> edges = new ArrayList<>();
        private final List<BlockDisplayHandle> faces = new ArrayList<>();
        private BlockDisplayHandle centerBlock;
        private final List<BlockDisplayHandle> slabs = new ArrayList<>();

        public HollowCube(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hollow_cube", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 obsidian corners at ±1.5 on each axis
            float[] signs = {-1.5f, 1.5f};
            for (float cx : signs) {
                for (float cy : signs) {
                    for (float cz : signs) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(cx, cy, cz), Material.OBSIDIAN);
                        h.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(2, 0);
                        corners.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // 12 black_concrete edge midpoints
            // Edges along X axis (y fixed, z fixed) — 4 edges
            double[][] edgeMidpointsX = {{0, -1.5, -1.5},{0, -1.5, 1.5},{0, 1.5, -1.5},{0, 1.5, 1.5}};
            // Edges along Y axis (x fixed, z fixed) — 4 edges
            double[][] edgeMidpointsY = {{-1.5, 0, -1.5},{-1.5, 0, 1.5},{1.5, 0, -1.5},{1.5, 0, 1.5}};
            // Edges along Z axis (x fixed, y fixed) — 4 edges
            double[][] edgeMidpointsZ = {{-1.5, -1.5, 0},{-1.5, 1.5, 0},{1.5, -1.5, 0},{1.5, 1.5, 0}};

            for (double[][] group : new double[][][]{edgeMidpointsX, edgeMidpointsY, edgeMidpointsZ}) {
                for (double[] off : group) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(off[0], off[1], off[2]), Material.BLACK_CONCRETE);
                    h.scale(0.8f, 0.8f, 0.8f).glow(60, 0, 120).interpolation(2, 0);
                    edges.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 6 crying_obsidian face centers
            double[][] faceCenters = {{0,1.5,0},{0,-1.5,0},{1.5,0,0},{-1.5,0,0},{0,0,1.5},{0,0,-1.5}};
            for (double[] off : faceCenters) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.CRYING_OBSIDIAN);
                h.scale(1.1f, 1.1f, 1.1f).glow(100, 0, 200).interpolation(2, 0);
                faces.add(h);
                spawnedEntities.add(h.entity());
            }

            // 1 amethyst at center
            centerBlock = displayBuilder.spawnBlock(center.clone(), Material.AMETHYST_BLOCK);
            centerBlock.scale(0.7f, 0.7f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(centerBlock.entity());

            // 4 polished_blackstone on diagonal orbiting plane (XZ plane)
            double slabRadius = 2.1;
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI / 4 + i * (Math.PI / 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * slabRadius, 0, Math.sin(angle) * slabRadius),
                        Material.POLISHED_BLACKSTONE);
                h.scale(0.6f, 0.6f, 0.6f).glow(80, 0, 160).interpolation(2, 0);
                slabs.add(h);
                spawnedEntities.add(h.entity());
                
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Frame rotates Y-axis at 1.5°/tick
            float frameAngle = ticksAlive * 0.026f;
            // Face plates counter-rotate X-axis at 0.8°/tick
            float faceAngle = ticksAlive * 0.01396f;

            float[] signs = {-1.5f, 1.5f};
            int ci = 0;
            for (float cx : signs) {
                for (float cy : signs) {
                    for (float cz : signs) {
                        double rx = Math.cos(frameAngle) * cx - Math.sin(frameAngle) * cz;
                        double rz = Math.sin(frameAngle) * cx + Math.cos(frameAngle) * cz;
                        corners.get(ci).entity().teleport(center.clone().add(rx, cy, rz));
                        ci++;
                    }
                }
            }

            // Edge midpoints rotate with frame
            double[][] edgeMidpoints = {
                {0,-1.5,-1.5},{0,-1.5,1.5},{0,1.5,-1.5},{0,1.5,1.5},
                {-1.5,0,-1.5},{-1.5,0,1.5},{1.5,0,-1.5},{1.5,0,1.5},
                {-1.5,-1.5,0},{-1.5,1.5,0},{1.5,-1.5,0},{1.5,1.5,0}
            };
            for (int i = 0; i < edges.size(); i++) {
                double ox = edgeMidpoints[i][0], oy = edgeMidpoints[i][1], oz = edgeMidpoints[i][2];
                double rx = Math.cos(frameAngle) * ox - Math.sin(frameAngle) * oz;
                double rz = Math.sin(frameAngle) * ox + Math.cos(frameAngle) * oz;
                edges.get(i).entity().teleport(center.clone().add(rx, oy, rz));
            }

            // Face centers counter-rotate on X-axis
            double[][] faceOffsets = {{0,1.5,0},{0,-1.5,0},{1.5,0,0},{-1.5,0,0},{0,0,1.5},{0,0,-1.5}};
            for (int i = 0; i < faces.size(); i++) {
                double fx = faceOffsets[i][0], fy = faceOffsets[i][1], fz = faceOffsets[i][2];
                double ry = Math.cos(faceAngle) * fy - Math.sin(faceAngle) * fz;
                double rz = Math.sin(faceAngle) * fy + Math.cos(faceAngle) * fz;
                // also apply frame Y rotation
                double rx = Math.cos(frameAngle) * fx - Math.sin(frameAngle) * rz;
                double finalZ = Math.sin(frameAngle) * fx + Math.cos(frameAngle) * rz;
                faces.get(i).entity().teleport(center.clone().add(rx, ry, finalZ));
            }

            // Center oscillates Y ±0.4 over 60-tick sine period
            double centerY = Math.sin(ticksAlive * (2 * Math.PI / 60.0)) * 0.4;
            centerBlock.entity().teleport(center.clone().add(0, centerY, 0));

            // 4 slabs orbit diagonal axis at 2°/tick
            float slabAngle = ticksAlive * 0.03491f;
            double slabRadius = 2.1;
            for (int i = 0; i < 4; i++) {
                double baseAngle = Math.PI / 4 + i * (Math.PI / 2) + slabAngle;
                slabs.get(i).entity().teleport(
                        center.clone().add(Math.cos(baseAngle) * slabRadius, 0, Math.sin(baseAngle) * slabRadius));
            }

            // Particles: purple dust from corners every tick
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle h : corners) {
                    DisplayBuilder.purpleDust(h.entity().getLocation(), 1, 0.1);
                }
            }

            // Dripping obsidian tear from edge midpoints
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : edges) {
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, h.entity().getLocation(), 1, 0, 0, 0, 0);
                }
            }

            // End rod bursts from corners every 20 ticks
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle h : corners) {
                    w.spawnParticle(Particle.END_ROD, h.entity().getLocation(), 4, 0.2, 0.2, 0.2, 0.05);
                }
            }

            // Sound every 40 ticks
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new HollowCube(plugin);
        }
    }

    // ================================================================
    // 52. VOID TORUS
    // ================================================================
    public static class VoidTorus extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> midline = new ArrayList<>();
        private final List<BlockDisplayHandle> glass = new ArrayList<>();
        private BlockDisplayHandle torusCenter;

        public VoidTorus(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_torus", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 24 black_concrete outer ring (radius 2.5, y=0)
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24;
                double x = Math.cos(angle) * 2.5;
                double z = Math.sin(angle) * 2.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.BLACK_CONCRETE);
                h.scale(0.9f, 0.9f, 0.9f).glow(30, 0, 80).interpolation(2, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 24 obsidian inner ring (radius 1.5, y=0)
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.OBSIDIAN);
                h.scale(0.8f, 0.8f, 0.8f).glow(80, 0, 160).interpolation(2, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 crying_obsidian at midline (radius 2.0) at 30° intervals
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(i * 30.0);
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.CRYING_OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(100, 0, 200).interpolation(2, 0);
                midline.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 purple_stained_glass at 60° intervals
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60.0);
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0.3, z), Material.PURPLE_STAINED_GLASS);
                h.scale(0.7f, 0.7f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
                glass.add(h);
                spawnedEntities.add(h.entity());
            }

            // 1 netherite at center
            torusCenter = displayBuilder.spawnBlock(center.clone(), Material.NETHERITE_BLOCK);
            torusCenter.scale(0.8f, 0.8f, 0.8f).glow(60, 0, 120).interpolation(2, 0);
            spawnedEntities.add(torusCenter.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Entire torus rotates Y-axis 1°/tick
            float yRot = (float) Math.toRadians(ticksAlive);
            // Nutation: structure tilts 30° off vertical, that tilt rotates at 0.3°/tick
            float nutationAngle = (float) Math.toRadians(ticksAlive * 0.3);
            float tiltRad = (float) Math.toRadians(30.0);

            // Outer ring
            for (int i = 0; i < outerRing.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 24 + yRot;
                double x = Math.cos(baseAngle) * 2.5;
                double y = 0;
                double z = Math.sin(baseAngle) * 2.5;
                // Apply nutation tilt around nutating axis
                double[] tilted = applyNutation(x, y, z, tiltRad, nutationAngle);
                outerRing.get(i).entity().teleport(center.clone().add(tilted[0], tilted[1], tilted[2]));
            }

            // Inner ring
            for (int i = 0; i < innerRing.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 24 + yRot;
                double x = Math.cos(baseAngle) * 1.5;
                double z = Math.sin(baseAngle) * 1.5;
                double[] tilted = applyNutation(x, 0, z, tiltRad, nutationAngle);
                innerRing.get(i).entity().teleport(center.clone().add(tilted[0], tilted[1], tilted[2]));
            }

            // Midline
            for (int i = 0; i < midline.size(); i++) {
                double baseAngle = Math.toRadians(i * 30.0) + yRot;
                double x = Math.cos(baseAngle) * 2.0;
                double z = Math.sin(baseAngle) * 2.0;
                double[] tilted = applyNutation(x, 0, z, tiltRad, nutationAngle);
                midline.get(i).entity().teleport(center.clone().add(tilted[0], tilted[1], tilted[2]));
            }

            // Glass
            for (int i = 0; i < glass.size(); i++) {
                double baseAngle = Math.toRadians(i * 60.0) + yRot;
                double x = Math.cos(baseAngle) * 2.0;
                double z = Math.sin(baseAngle) * 2.0;
                double[] tilted = applyNutation(x, 0.3, z, tiltRad, nutationAngle);
                glass.get(i).entity().teleport(center.clone().add(tilted[0], tilted[1], tilted[2]));
            }

            // Netherite center pulses scale 1.0 to 1.6 over 80-tick sine cycle
            float pulseScale = 1.0f + 0.3f * (float)(Math.sin(ticksAlive * (2 * Math.PI / 80.0)) * 0.5 + 0.5);
            torusCenter.scale(pulseScale, pulseScale, pulseScale);

            // Particles
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.darkPurpleDust(center, 3, 2.5);
            }
            if (ticksAlive % 6 == 0) {
                w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, center, 3, 2.0, 0.1, 2.0, 0);
            }
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL, torusCenter.entity().getLocation(), 4, 0.1, 0.1, 0.1, 0.05);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 0.3f);
            }
        }

        private double[] applyNutation(double x, double y, double z, float tiltRad, float nutationAngle) {
            // Tilt the structure: rotate around an axis in the XZ plane
            double tiltAxisX = Math.cos(nutationAngle);
            double tiltAxisZ = Math.sin(nutationAngle);
            // Rodrigues' rotation formula for rotating (x,y,z) around tilt axis by tiltRad
            double cosT = Math.cos(tiltRad), sinT = Math.sin(tiltRad);
            double dot = tiltAxisX * x + tiltAxisZ * z; // tiltAxis.dot(v) — Y component of axis is 0
            double rx = x * cosT + (tiltAxisZ * y - 0 * z) * sinT + tiltAxisX * dot * (1 - cosT);
            double ry = y * cosT + (0 * z - tiltAxisX * x - tiltAxisZ * z) * sinT
                    + 0 * dot * (1 - cosT);
            // simplified: axis is (tiltAxisX, 0, tiltAxisZ), so cross product:
            // axis × v = (0*z - tiltAxisZ*y, tiltAxisZ*x - tiltAxisX*z, tiltAxisX*y - 0*x)
            double crossX = -tiltAxisZ * y;
            double crossY = tiltAxisZ * x - tiltAxisX * z;
            double crossZ = tiltAxisX * y;
            double axDotV = tiltAxisX * x + 0 * y + tiltAxisZ * z;
            double outX = x * cosT + crossX * sinT + tiltAxisX * axDotV * (1 - cosT);
            double outY = y * cosT + crossY * sinT + 0 * axDotV * (1 - cosT);
            double outZ = z * cosT + crossZ * sinT + tiltAxisZ * axDotV * (1 - cosT);
            return new double[]{outX, outY, outZ};
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidTorus(plugin);
        }
    }

    // ================================================================
    // 53. DOUBLE HELIX SPIRE
    // ================================================================
    public static class DoubleHelixSpire extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> strandA = new ArrayList<>();
        private final List<BlockDisplayHandle> strandB = new ArrayList<>();
        private final List<BlockDisplayHandle> rungs = new ArrayList<>();
        private final List<BlockDisplayHandle> strandTips = new ArrayList<>();
        private final List<BlockDisplayHandle> apexBlocks = new ArrayList<>();

        // Base angles stored for strand positions
        private final double[] strandAAngles = new double[20];
        private final double[] strandAHeights = new double[20];
        private final double[] strandBAngles = new double[20];
        private final double[] strandBHeights = new double[20];

        public DoubleHelixSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("double_helix_spire", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Strand A: 20 blackstone, rising helix, 8 blocks tall, 2 full rotations
            for (int i = 0; i < 20; i++) {
                double height = i * 0.4;
                double angle = Math.toRadians(i * 36.0);
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                strandAAngles[i] = angle;
                strandAHeights[i] = height;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, height, z), Material.BLACKSTONE);
                h.scale(0.8f, 0.8f, 0.8f).glow(80, 0, 160).interpolation(2, 0);
                strandA.add(h);
                spawnedEntities.add(h.entity());
            }

            // Strand B: 20 polished_blackstone, offset 180°
            for (int i = 0; i < 20; i++) {
                double height = i * 0.4;
                double angle = Math.toRadians(i * 36.0 + 180.0);
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                strandBAngles[i] = angle;
                strandBHeights[i] = height;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, height, z), Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.8f, 0.8f).glow(60, 0, 120).interpolation(2, 0);
                strandB.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 cobbled_deepslate rungs at each half-rotation (every 180° = 10 blocks apart)
            // Rungs connect strand A to strand B at index 0,2,4,6,8,10,12,14
            for (int i = 0; i < 8; i++) {
                int idx = i * 2; // half-rotation
                double aAngle = strandAAngles[idx];
                double bAngle = strandBAngles[idx];
                double height = strandAHeights[idx];
                double midX = (Math.cos(aAngle) + Math.cos(bAngle)) * 1.5 / 2.0;
                double midZ = (Math.sin(aAngle) + Math.sin(bAngle)) * 1.5 / 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(midX, height, midZ), Material.COBBLED_DEEPSLATE);
                h.scale(0.4f, 0.4f, 2.5f).glow(50, 0, 100).interpolation(2, 0);
                rungs.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 crying_obsidian at top of each strand (indices 18,19 for A, 18,19 for B)
            for (int i = 18; i < 20; i++) {
                BlockDisplayHandle ha = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(strandAAngles[i]) * 1.5, strandAHeights[i] + 0.5, Math.sin(strandAAngles[i]) * 1.5),
                        Material.CRYING_OBSIDIAN);
                ha.scale(0.9f, 0.9f, 0.9f).glow(128, 0, 255).interpolation(2, 0);
                strandTips.add(ha);
                spawnedEntities.add(ha.entity());

                BlockDisplayHandle hb = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(strandBAngles[i]) * 1.5, strandBHeights[i] + 0.5, Math.sin(strandBAngles[i]) * 1.5),
                        Material.CRYING_OBSIDIAN);
                hb.scale(0.9f, 0.9f, 0.9f).glow(128, 0, 255).interpolation(2, 0);
                strandTips.add(hb);
                spawnedEntities.add(hb.entity());
            }

            // 2 amethyst hovering above
            double topHeight = 19 * 0.4 + 1.5;
            for (int i = 0; i < 2; i++) {
                double ax = (i == 0) ? -0.4 : 0.4;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(ax, topHeight, 0), Material.AMETHYST_BLOCK);
                h.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                apexBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Both strands rotate Y-axis 1.2°/tick
            float rotAngle = (float) Math.toRadians(ticksAlive * 1.2);
            // Structure translates upward 0.015 blocks/tick, snapping back every 120 ticks
            double liftOffset = (ticksAlive % 120) * 0.015;

            // Strand A
            for (int i = 0; i < strandA.size(); i++) {
                double baseAngle = strandAAngles[i] + rotAngle;
                double x = Math.cos(baseAngle) * 1.5;
                double z = Math.sin(baseAngle) * 1.5;
                double y = strandAHeights[i] + liftOffset;
                strandA.get(i).entity().teleport(center.clone().add(x, y, z));
            }

            // Strand B
            for (int i = 0; i < strandB.size(); i++) {
                double baseAngle = strandBAngles[i] + rotAngle;
                double x = Math.cos(baseAngle) * 1.5;
                double z = Math.sin(baseAngle) * 1.5;
                double y = strandBHeights[i] + liftOffset;
                strandB.get(i).entity().teleport(center.clone().add(x, y, z));
            }

            // Rungs counter-rotate 0.4°/tick
            float rungAngle = (float) Math.toRadians(ticksAlive * -0.4);
            for (int i = 0; i < rungs.size(); i++) {
                int idx = i * 2;
                double aAngle = strandAAngles[idx] + rungAngle;
                double bAngle = strandBAngles[idx] + rungAngle;
                double height = strandAHeights[idx] + liftOffset;
                double midX = (Math.cos(aAngle) + Math.cos(bAngle)) * 1.5 / 2.0;
                double midZ = (Math.sin(aAngle) + Math.sin(bAngle)) * 1.5 / 2.0;
                rungs.get(i).entity().teleport(center.clone().add(midX, height, midZ));
            }

            // Tips
            for (int i = 0; i < strandTips.size(); i++) {
                int srcIdx = 18 + (i % 2);
                double angle = (i < 2 ? strandAAngles[srcIdx] : strandBAngles[srcIdx - 2]) + rotAngle;
                double heights = (i < 2 ? strandAHeights[srcIdx] : strandBHeights[srcIdx - 2]) + 0.5 + liftOffset;
                strandTips.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 1.5, heights, Math.sin(angle) * 1.5));
            }

            // Apex blocks
            double topHeight = 19 * 0.4 + 1.5 + liftOffset;
            for (int i = 0; i < apexBlocks.size(); i++) {
                double ax = (i == 0) ? -0.4 : 0.4;
                apexBlocks.get(i).entity().teleport(center.clone().add(ax, topHeight, 0));
            }

            // Particles: enchant trailing along strands
            if (ticksAlive % 3 == 0) {
                int idx = (ticksAlive / 3) % strandA.size();
                w.spawnParticle(Particle.ENCHANT, strandA.get(idx).entity().getLocation(), 3, 0.1, 0.1, 0.1, 0.5);
                w.spawnParticle(Particle.ENCHANT, strandB.get(idx).entity().getLocation(), 3, 0.1, 0.1, 0.1, 0.5);
            }

            // Magenta dust at mid-height
            if (ticksAlive % 5 == 0) {
                double midH = 4.0 * 0.4 + liftOffset;
                DisplayBuilder.dustParticles(center.clone().add(0, midH, 0), 3, 1.5f, 180, 0, 180, 1.2f);
            }

            // Purple spore particles from top
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, center.clone().add(0, topHeight, 0),
                        3, 0.5, 0.3, 0.5, 0);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.7f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new DoubleHelixSpire(plugin);
        }
    }

    // ================================================================
    // 54. INFINITE TESSERACT FRAME
    // ================================================================
    public static class InfiniteTesseractFrame extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerCorners = new ArrayList<>();
        private final List<BlockDisplayHandle> outerEdges = new ArrayList<>();
        private final List<BlockDisplayHandle> innerCorners = new ArrayList<>();
        private final List<BlockDisplayHandle> struts = new ArrayList<>();

        // Outer cube corners (half-edge = 2.0): all ±2.0 combos
        private static final float[][] OUTER_CORNERS = {
            {-2f,-2f,-2f},{-2f,-2f,2f},{-2f,2f,-2f},{-2f,2f,2f},
            {2f,-2f,-2f},{2f,-2f,2f},{2f,2f,-2f},{2f,2f,2f}
        };
        // Outer cube edges: 12 edges, 2 segments each (at 1/3 and 2/3 along edge)
        // Each edge defined by two corner indices; segments at t=1/3 and t=2/3
        private static final int[][] EDGE_PAIRS = {
            {0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}
        };

        public InfiniteTesseractFrame(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infinite_tesseract_frame", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer cube: 8 obsidian corner pairs (scale 1.0)
            for (float[] c : OUTER_CORNERS) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(c[0], c[1], c[2]), Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(80, 0, 160).interpolation(2, 0);
                outerCorners.add(h);
                spawnedEntities.add(h.entity());
            }

            // 24 netherite edge segments (2 per edge)
            for (int[] edge : EDGE_PAIRS) {
                float[] a = OUTER_CORNERS[edge[0]], b = OUTER_CORNERS[edge[1]];
                for (int seg = 1; seg <= 2; seg++) {
                    float t = seg / 3.0f;
                    double x = a[0] + (b[0] - a[0]) * t;
                    double y = a[1] + (b[1] - a[1]) * t;
                    double z = a[2] + (b[2] - a[2]) * t;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, z), Material.NETHERITE_BLOCK);
                    h.scale(0.5f, 0.5f, 0.5f).glow(60, 0, 120).interpolation(2, 0);
                    outerEdges.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Inner cube: 8 black_glazed_terracotta corners at ±1.0, scale 0.5
            float[] iSigns = {-1f, 1f};
            for (float ix : iSigns) {
                for (float iy : iSigns) {
                    for (float iz : iSigns) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(ix, iy, iz), Material.BLACK_GLAZED_TERRACOTTA);
                        h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                        innerCorners.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // 8 purple_stained_glass panes as struts from inner corner toward outer corner
            for (int i = 0; i < 8; i++) {
                float[] oc = OUTER_CORNERS[i];
                // midpoint between inner (±1) and outer (±2) = ±1.5
                double mx = oc[0] * 0.75, my = oc[1] * 0.75, mz = oc[2] * 0.75;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(mx, my, mz), Material.PURPLE_STAINED_GLASS);
                h.scale(0.4f, 0.4f, 1.5f).glow(128, 0, 255).interpolation(5, 0);
                struts.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Outer cube rotates Y 0.8°/tick
            float outerY = (float) Math.toRadians(ticksAlive * 0.8);
            // Inner cube rotates Y -0.8°/tick AND X 0.5°/tick
            float innerY = (float) Math.toRadians(ticksAlive * -0.8);
            float innerX = (float) Math.toRadians(ticksAlive * 0.5);
            // Inner cube oscillates Y position ±0.3 over 100 ticks
            double innerYOff = Math.sin(ticksAlive * (2 * Math.PI / 100.0)) * 0.3;
            // Struts scale 1.0 to 1.4 over 100-tick sine
            float strutScale = 1.0f + 0.2f * (float)(Math.sin(ticksAlive * (2 * Math.PI / 100.0)) * 0.5 + 0.5);

            // Outer corners
            for (int i = 0; i < outerCorners.size(); i++) {
                float[] c = OUTER_CORNERS[i];
                double rx = Math.cos(outerY) * c[0] - Math.sin(outerY) * c[2];
                double rz = Math.sin(outerY) * c[0] + Math.cos(outerY) * c[2];
                outerCorners.get(i).entity().teleport(center.clone().add(rx, c[1], rz));
            }

            // Outer edges
            int edgeIdx = 0;
            for (int[] edge : EDGE_PAIRS) {
                float[] a = OUTER_CORNERS[edge[0]], b = OUTER_CORNERS[edge[1]];
                for (int seg = 1; seg <= 2; seg++) {
                    float t = seg / 3.0f;
                    double ox = a[0] + (b[0] - a[0]) * t;
                    double oy = a[1] + (b[1] - a[1]) * t;
                    double oz = a[2] + (b[2] - a[2]) * t;
                    double rx = Math.cos(outerY) * ox - Math.sin(outerY) * oz;
                    double rz = Math.sin(outerY) * ox + Math.cos(outerY) * oz;
                    outerEdges.get(edgeIdx++).entity().teleport(center.clone().add(rx, oy, rz));
                }
            }

            // Inner corners: rotate Y then X
            float[] iSigns = {-1f, 1f};
            int ii = 0;
            for (float ix : iSigns) {
                for (float iy : iSigns) {
                    for (float iz : iSigns) {
                        // Rotate Y
                        double ry1x = Math.cos(innerY) * ix - Math.sin(innerY) * iz;
                        double ry1z = Math.sin(innerY) * ix + Math.cos(innerY) * iz;
                        // Rotate X
                        double rx2y = Math.cos(innerX) * iy - Math.sin(innerX) * ry1z;
                        double rx2z = Math.sin(innerX) * iy + Math.cos(innerX) * ry1z;
                        innerCorners.get(ii++).entity().teleport(
                                center.clone().add(ry1x, rx2y + innerYOff, rx2z));
                    }
                }
            }

            // Struts: scale pulse
            for (int i = 0; i < struts.size(); i++) {
                struts.get(i).scale(0.4f * strutScale, 0.4f * strutScale, 1.5f * strutScale);
            }

            // Particles: portal from struts
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : struts) {
                    w.spawnParticle(Particle.PORTAL, h.entity().getLocation(), 3, 0.2, 0.2, 0.2, 0.5);
                }
            }

            // Deep indigo dust between cubes
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, innerYOff, 0), 4, 1.5, 30, 0, 80, 1.5f);
            }

            // End rod from outer corners every 40 ticks
            if (ticksAlive % 40 == 0) {
                for (BlockDisplayHandle h : outerCorners) {
                    w.spawnParticle(Particle.END_ROD, h.entity().getLocation(), 3, 0.1, 0.1, 0.1, 0.05);
                }
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new InfiniteTesseractFrame(plugin);
        }
    }

    // ================================================================
    // 55. MOBIUS RIBBON
    // ================================================================
    public static class MobiusRibbon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ribbon = new ArrayList<>();
        private final List<BlockDisplayHandle> inlay = new ArrayList<>();
        private final List<BlockDisplayHandle> amethysts = new ArrayList<>();
        private final List<BlockDisplayHandle> cryingObs = new ArrayList<>();
        private BlockDisplayHandle anchor;

        // Base angles for each ring block (30 blocks, 0..2pi)
        private static final int RING_COUNT = 30;
        private static final double RING_RADIUS = 3.0;

        public MobiusRibbon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mobius_ribbon", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 30 dark_prismarine forming ribbon ring
            // Half-twist: each block rotates by (i/RING_COUNT)*PI around the tangential axis
            for (int i = 0; i < RING_COUNT; i++) {
                double angle = (2 * Math.PI * i) / RING_COUNT;
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;
                // Simulate half-twist: slight Y tilt proportional to angle
                double twistY = Math.sin(angle * 0.5) * 0.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, twistY, z), Material.DARK_PRISMARINE);
                // Rotate the block to simulate the Möbius twist
                float twistAngle = (float)(angle * 0.5); // half revolution = half twist
                h.rotate(twistAngle, 0, 1, 0).scale(0.9f, 0.9f, 0.9f).glow(0, 80, 100).interpolation(2, 0);
                ribbon.add(h);
                spawnedEntities.add(h.entity());
            }

            // 15 black_concrete inlaid — same positions, offset 0.3 toward center
            for (int i = 0; i < 15; i++) {
                double angle = (2 * Math.PI * i * 2) / RING_COUNT; // every other ribbon slot
                double inR = RING_RADIUS - 0.3;
                double x = Math.cos(angle) * inR;
                double z = Math.sin(angle) * inR;
                double twistY = Math.sin(angle * 0.5) * 0.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, twistY, z), Material.BLACK_CONCRETE);
                h.scale(0.6f, 0.6f, 0.6f).glow(30, 0, 60).interpolation(2, 0);
                inlay.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 amethyst at 0°, 60°, 120°, 180°, 240°, 300°
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60.0);
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;
                double twistY = Math.sin(angle * 0.5) * 0.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, twistY + 0.1, z), Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(5, 0);
                amethysts.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 crying_obsidian at 0°, 90°, 180°, 270°
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(i * 90.0);
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;
                double twistY = Math.sin(angle * 0.5) * 0.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, twistY - 0.1, z), Material.CRYING_OBSIDIAN);
                h.scale(0.7f, 0.7f, 0.7f).glow(80, 0, 160).interpolation(2, 0);
                cryingObs.add(h);
                spawnedEntities.add(h.entity());
            }

            // 1 obsidian at 180° twist midpoint as anchor
            double anchorAngle = Math.PI;
            anchor = displayBuilder.spawnBlock(
                    center.clone().add(Math.cos(anchorAngle) * RING_RADIUS, 0, Math.sin(anchorAngle) * RING_RADIUS),
                    Material.OBSIDIAN);
            anchor.scale(1.0f, 1.0f, 1.0f).glow(50, 0, 100).interpolation(2, 0);
            spawnedEntities.add(anchor.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring rotates Y-axis 1°/tick
            float yRot = (float) Math.toRadians(ticksAlive);
            // Ring oscillates tilt ±15° on X-axis over 120-tick sine cycle
            float tiltAngle = (float)(Math.sin(ticksAlive * (2 * Math.PI / 120.0)) * Math.toRadians(15.0));

            // Ribbon
            for (int i = 0; i < ribbon.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / RING_COUNT + yRot;
                double x = Math.cos(baseAngle) * RING_RADIUS;
                double z = Math.sin(baseAngle) * RING_RADIUS;
                double twistY = Math.sin(baseAngle * 0.5) * 0.2;
                // Apply X tilt
                double ty = twistY * Math.cos(tiltAngle) - z * Math.sin(tiltAngle);
                double tz = twistY * Math.sin(tiltAngle) + z * Math.cos(tiltAngle);
                ribbon.get(i).entity().teleport(center.clone().add(x, ty, tz));
            }

            // Inlay
            for (int i = 0; i < inlay.size(); i++) {
                double baseAngle = (2 * Math.PI * i * 2) / RING_COUNT + yRot;
                double inR = RING_RADIUS - 0.3;
                double x = Math.cos(baseAngle) * inR;
                double z = Math.sin(baseAngle) * inR;
                double twistY = Math.sin(baseAngle * 0.5) * 0.2;
                double ty = twistY * Math.cos(tiltAngle) - z * Math.sin(tiltAngle);
                double tz = twistY * Math.sin(tiltAngle) + z * Math.cos(tiltAngle);
                inlay.get(i).entity().teleport(center.clone().add(x, ty, tz));
            }

            // 6 amethyst pulse scale 1.0 to 1.3 with 10-tick phase offsets
            for (int i = 0; i < amethysts.size(); i++) {
                double baseAngle = Math.toRadians(i * 60.0) + yRot;
                double x = Math.cos(baseAngle) * RING_RADIUS;
                double z = Math.sin(baseAngle) * RING_RADIUS;
                double twistY = Math.sin(baseAngle * 0.5) * 0.2 + 0.1;
                double ty = twistY * Math.cos(tiltAngle) - z * Math.sin(tiltAngle);
                double tz = twistY * Math.sin(tiltAngle) + z * Math.cos(tiltAngle);
                amethysts.get(i).entity().teleport(center.clone().add(x, ty, tz));
                // Phase offset of 10 ticks between each
                float pulse = 1.0f + 0.15f * (float)(Math.sin((ticksAlive - i * 10) * (2 * Math.PI / 40.0)) * 0.5 + 0.5);
                amethysts.get(i).scale(pulse, pulse, pulse);
            }

            // Crying obsidian
            for (int i = 0; i < cryingObs.size(); i++) {
                double baseAngle = Math.toRadians(i * 90.0) + yRot;
                double x = Math.cos(baseAngle) * RING_RADIUS;
                double z = Math.sin(baseAngle) * RING_RADIUS;
                double twistY = Math.sin(baseAngle * 0.5) * 0.2 - 0.1;
                double ty = twistY * Math.cos(tiltAngle) - z * Math.sin(tiltAngle);
                double tz = twistY * Math.sin(tiltAngle) + z * Math.cos(tiltAngle);
                cryingObs.get(i).entity().teleport(center.clone().add(x, ty, tz));
            }

            // Anchor at 180°
            double anchorAngle = Math.PI + yRot;
            double ax = Math.cos(anchorAngle) * RING_RADIUS;
            double az = Math.sin(anchorAngle) * RING_RADIUS;
            double aty = (-az) * Math.sin(tiltAngle);
            double atz = (-az) * Math.cos(tiltAngle);
            anchor.entity().teleport(center.clone().add(ax, aty, atz));

            // Particles: teal dust flowing along ring
            if (ticksAlive % 3 == 0) {
                int idx = (ticksAlive / 3) % RING_COUNT;
                DisplayBuilder.dustParticles(ribbon.get(idx).entity().getLocation(), 2, 0.2, 0, 60, 80, 1.2f);
            }

            // Falling obsidian tear from lower edge
            if (ticksAlive % 6 == 0) {
                for (BlockDisplayHandle h : cryingObs) {
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, h.entity().getLocation(), 2, 0.1, 0, 0.1, 0);
                }
            }

            // Bubble pop at 180° twist point
            if (ticksAlive % 8 == 0) {
                w.spawnParticle(Particle.BUBBLE_POP, anchor.entity().getLocation(), 3, 0.2, 0.2, 0.2, 0.05);
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_HIT, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new MobiusRibbon(plugin);
        }
    }

    // ================================================================
    // 56. OCTAHEDRON BURST — Dual octahedron that explosively scales out
    // ================================================================
    public static class OctahedronBurst extends BlockDisplayAttack {
        private static final double[][] APEXES = {
            {0,2,0},{0,-2,0},{2,0,0},{-2,0,0},{0,0,2},{0,0,-2}
        };
        private static final int[][] EDGE_PAIRS = {
            {0,2},{0,3},{0,4},{0,5},{1,2},{1,3},{1,4},{1,5},{2,4},{2,5},{3,4},{3,5}
        };
        private static final double[][] FACE_LOCAL = {
            {1,1,1},{-1,1,1},{1,1,-1},{-1,1,-1},{1,-1,1},{-1,-1,1},{1,-1,-1},{-1,-1,-1}
        };
        private static final double[][] INNER_PTS = {
            {0,0.8,0},{0,-0.8,0},{0.8,0,0},{-0.8,0,0},{0,0,0.8},{0,0,-0.8}
        };
        private final List<BlockDisplayHandle> outerApexes = new ArrayList<>();
        private final List<BlockDisplayHandle> outerEdges = new ArrayList<>();
        private final List<BlockDisplayHandle> outerFaces = new ArrayList<>();
        private final List<BlockDisplayHandle> innerApexes = new ArrayList<>();
        private float outerYaw = 0, innerPitch = 0, innerYaw = 0, burstScale = 1f;
        private boolean prevInBurst = false;

        public OctahedronBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("octahedron_burst", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(16.0); config.setDamageRadius(9.0);
            config.setDurationTicks(400); config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            for (int i = 0; i < APEXES.length; i++) {
                double[] a = APEXES[i];
                Material mat = (i < 2) ? Material.NETHERITE_BLOCK : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(a[0],a[1],a[2]), mat);
                h.scale(1f,1f,1f).glow(128,0,255).interpolation(2,0);
                outerApexes.add(h); spawnedEntities.add(h.entity());
            }
            for (int[] p : EDGE_PAIRS) {
                double[] a=APEXES[p[0]], b=APEXES[p[1]];
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add((a[0]+b[0])/2,(a[1]+b[1])/2,(a[2]+b[2])/2), Material.OBSIDIAN);
                h.scale(0.7f,0.7f,0.7f).glow(100,0,200).interpolation(2,0);
                outerEdges.add(h); spawnedEntities.add(h.entity());
            }
            for (double[] fc : FACE_LOCAL) {
                double mag = Math.sqrt(fc[0]*fc[0]+fc[1]*fc[1]+fc[2]*fc[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add(fc[0]/mag*1.2,fc[1]/mag*1.2,fc[2]/mag*1.2), Material.BLACK_CONCRETE);
                h.scale(0.6f,0.6f,0.6f).glow(60,0,120).interpolation(2,0);
                outerFaces.add(h); spawnedEntities.add(h.entity());
            }
            for (double[] a : INNER_PTS) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add(a[0],a[1],a[2]), Material.POLISHED_BLACKSTONE);
                h.scale(0.4f,0.4f,0.4f).glow(180,50,255).interpolation(2,0);
                innerApexes.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            outerYaw += 1.5f; innerPitch += 2f; innerYaw -= 1f;
            int phase = ticksAlive % 80;
            boolean inBurst = phase < 20;
            burstScale = inBurst ? 1f + (phase/20f)*1.5f : 1f;
            if (!inBurst && prevInBurst) {
                center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.4f);
                DisplayBuilder.purpleDust(center, 10, 1.5);
            }
            prevInBurst = inBurst;
            double yR = Math.toRadians(outerYaw);
            for (int i = 0; i < APEXES.length; i++) {
                double[] a=APEXES[i];
                double rx=a[0]*Math.cos(yR)-a[2]*Math.sin(yR), rz=a[0]*Math.sin(yR)+a[2]*Math.cos(yR);
                outerApexes.get(i).entity().teleport(center.clone().add(rx*burstScale,a[1]*burstScale,rz*burstScale));
            }
            for (int i = 0; i < EDGE_PAIRS.length; i++) {
                double[] a=APEXES[EDGE_PAIRS[i][0]], b=APEXES[EDGE_PAIRS[i][1]];
                double mx=(a[0]+b[0])/2, my=(a[1]+b[1])/2, mz=(a[2]+b[2])/2;
                double rx=mx*Math.cos(yR)-mz*Math.sin(yR), rz=mx*Math.sin(yR)+mz*Math.cos(yR);
                outerEdges.get(i).entity().teleport(center.clone().add(rx*burstScale,my*burstScale,rz*burstScale));
            }
            for (int i = 0; i < FACE_LOCAL.length; i++) {
                double[] fc=FACE_LOCAL[i]; double mag=Math.sqrt(fc[0]*fc[0]+fc[1]*fc[1]+fc[2]*fc[2]);
                double nx=fc[0]/mag*1.2, ny=fc[1]/mag*1.2, nz=fc[2]/mag*1.2;
                double rx=nx*Math.cos(yR)-nz*Math.sin(yR), rz=nx*Math.sin(yR)+nz*Math.cos(yR);
                outerFaces.get(i).entity().teleport(center.clone().add(rx*burstScale,ny*burstScale,rz*burstScale));
            }
            double ipR=Math.toRadians(innerPitch), iyR=Math.toRadians(innerYaw);
            for (int i = 0; i < INNER_PTS.length; i++) {
                double[] a=INNER_PTS[i];
                double ry1=a[0]*Math.cos(iyR)-a[2]*Math.sin(iyR), rz1=a[0]*Math.sin(iyR)+a[2]*Math.cos(iyR);
                double fy=a[1]*Math.cos(ipR)-rz1*Math.sin(ipR), fz=a[1]*Math.sin(ipR)+rz1*Math.cos(ipR);
                innerApexes.get(i).entity().teleport(center.clone().add(ry1,fy,fz));
            }
            if (ticksAlive%5==0) DisplayBuilder.purpleDust(center.clone().add(0,-1.5,0), 2, 0.8);
            if (ticksAlive%60==0) center.getWorld().playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1f);
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new OctahedronBurst(plugin); }
    }

    // ================================================================
    // 57. CLOCKWORK ORRERY RING — Three independently spinning armillary rings
    // ================================================================
    public static class ClockworkOrreryRing extends BlockDisplayAttack {
        private static final int OUTER_N = 18; private static final double OUTER_R = 5.0;
        private static final int EQUAT_N = 12; private static final double EQUAT_R = 3.0;
        private static final int TILT_N  = 8;  private static final double TILT_R  = 4.0;
        private final List<BlockDisplayHandle> outerRing  = new ArrayList<>();
        private final List<BlockDisplayHandle> equatBand  = new ArrayList<>();
        private final List<BlockDisplayHandle> tiltBand   = new ArrayList<>();
        private final List<BlockDisplayHandle> polarNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> decorTiles = new ArrayList<>();
        private BlockDisplayHandle centerBlock;
        private float outerAngle = 0, equatAngle = 0, tiltAngle = 0;

        public ClockworkOrreryRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("clockwork_orrery_ring", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0); config.setDamageRadius(5.0);
            config.setDurationTicks(440); config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            for (int i = 0; i < OUTER_N; i++) {
                double a = 2*Math.PI*i/OUTER_N;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add(Math.cos(a)*OUTER_R, 0, Math.sin(a)*OUTER_R), Material.BLACKSTONE);
                h.scale(0.8f,0.8f,0.8f).glow(80,0,160).interpolation(2,0);
                outerRing.add(h); spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < EQUAT_N; i++) {
                double a = 2*Math.PI*i/EQUAT_N;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add(Math.cos(a)*EQUAT_R, Math.sin(a)*EQUAT_R, 0), Material.COBBLED_DEEPSLATE);
                h.scale(0.7f,0.7f,0.7f).glow(60,0,120).interpolation(2,0);
                equatBand.add(h); spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < TILT_N; i++) {
                double a = 2*Math.PI*i/TILT_N;
                double x=Math.cos(a)*TILT_R*0.7071, y=Math.sin(a)*TILT_R, z=Math.cos(a)*TILT_R*0.7071;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x,y,z), Material.DARK_PRISMARINE);
                h.scale(0.75f,0.75f,0.75f).glow(0,140,180).interpolation(2,0);
                tiltBand.add(h); spawnedEntities.add(h.entity());
            }
            double[][] polars = {{0,OUTER_R,0},{0,-OUTER_R,0},{EQUAT_R,0,0}};
            for (double[] p : polars) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(p[0],p[1],p[2]), Material.AMETHYST_BLOCK);
                h.scale(0.9f,0.9f,0.9f).glow(160,80,255).interpolation(2,0);
                polarNodes.add(h); spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i*60.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add(Math.cos(a)*OUTER_R, 0.1, Math.sin(a)*OUTER_R), Material.BLACK_GLAZED_TERRACOTTA);
                h.scale(0.5f,0.5f,0.5f).glow(100,0,200).interpolation(2,0);
                decorTiles.add(h); spawnedEntities.add(h.entity());
            }
            centerBlock = displayBuilder.spawnBlock(center.clone(), Material.CRYING_OBSIDIAN);
            centerBlock.scale(1f,1f,1f).glow(130,0,200).interpolation(2,0);
            spawnedEntities.add(centerBlock.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            outerAngle += 0.6f; equatAngle += 0.9f; tiltAngle += 1.2f;
            double yR = Math.toRadians(outerAngle);
            for (int i = 0; i < OUTER_N; i++) {
                double a = 2*Math.PI*i/OUTER_N + yR;
                outerRing.get(i).entity().teleport(center.clone().add(Math.cos(a)*OUTER_R, 0, Math.sin(a)*OUTER_R));
            }
            double zR = Math.toRadians(equatAngle);
            for (int i = 0; i < EQUAT_N; i++) {
                double a = 2*Math.PI*i/EQUAT_N + zR;
                equatBand.get(i).entity().teleport(center.clone().add(Math.cos(a)*EQUAT_R, Math.sin(a)*EQUAT_R, 0));
            }
            double tR = Math.toRadians(tiltAngle);
            for (int i = 0; i < TILT_N; i++) {
                double a = 2*Math.PI*i/TILT_N + tR;
                double x=Math.cos(a)*TILT_R*0.7071, y=Math.sin(a)*TILT_R, z=Math.cos(a)*TILT_R*0.7071;
                tiltBand.get(i).entity().teleport(center.clone().add(x,y,z));
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i*60.0) + yR;
                decorTiles.get(i).entity().teleport(center.clone().add(Math.cos(a)*OUTER_R, 0.1, Math.sin(a)*OUTER_R));
            }
            float pulse = 1f + 0.8f*(float)((Math.sin(ticksAlive*Math.PI*2/100.0)+1)/2.0);
            Transformation t = centerBlock.entity().getTransformation();
            centerBlock.entity().setTransformation(new Transformation(
                t.getTranslation(), t.getLeftRotation(), new Vector3f(pulse,pulse,pulse), t.getRightRotation()));
            if (ticksAlive%3==0) {
                center.getWorld().spawnParticle(Particle.END_ROD, center, 2, OUTER_R*0.5, 0.5, OUTER_R*0.5, 0.02);
                DisplayBuilder.purpleDust(center.clone().add(Math.cos(ticksAlive*0.2)*2, 0, Math.sin(ticksAlive*0.2)*2), 2, 0.5);
            }
            if (ticksAlive%10==0) center.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, center, 1, 0.1, 0.1, 0.1, 0);
            if (ticksAlive%80==0) center.getWorld().playSound(center, Sound.BLOCK_BELL_USE, 0.6f, 0.3f);
            if (ticksAlive%20==0) center.getWorld().playSound(center, Sound.BLOCK_CHAIN_HIT, 0.4f, 0.5f);
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ClockworkOrreryRing(plugin); }
    }

    // ================================================================
    // 58. FRACTAL SIERPINSKI FRAME — Outer tetrahedron with 4 half-scale sub-tetras
    // ================================================================
    public static class FractalSierpinskiFrame extends BlockDisplayAttack {
        private static final double[][] OUTER_V = {{2,0,2},{2,0,-2},{-2,0,0},{0,3,0}};
        private static final int[][] OUTER_E = {{0,1},{0,2},{0,3},{1,2},{1,3},{2,3}};
        private static final int[][] FACE_VERTS = {{0,1,2},{0,1,3},{0,2,3},{1,2,3}};
        private final List<BlockDisplayHandle> outerCorners = new ArrayList<>();
        private final List<BlockDisplayHandle> outerEdges   = new ArrayList<>();
        private final List<BlockDisplayHandle> subTetraBlks = new ArrayList<>();
        private final List<BlockDisplayHandle> voidCenters  = new ArrayList<>();
        private BlockDisplayHandle centroidBlock;
        private float outerRotY = 0;
        private float[] subRotY = {0,0,0,0};

        public FractalSierpinskiFrame(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fractal_sierpinski_frame", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0); config.setDamageRadius(7.0);
            config.setDurationTicks(320); config.setCooldownTicks(400);
        }

        private static double[][] subTetraCorners(int fi) {
            int[] fv = FACE_VERTS[fi];
            double cx=0,cy=0,cz=0;
            for (int v : fv) { cx+=OUTER_V[v][0]; cy+=OUTER_V[v][1]; cz+=OUTER_V[v][2]; }
            cx/=3; cy/=3; cz/=3;
            int apex=-1;
            outer: for (int k=0;k<4;k++) { for (int v:fv) if(v==k) continue outer; apex=k; break; }
            double[][] r = new double[4][3];
            for (int i=0;i<3;i++) { double[] v=OUTER_V[fv[i]]; r[i]=new double[]{cx+(v[0]-cx)*0.5,cy+(v[1]-cy)*0.5,cz+(v[2]-cz)*0.5}; }
            double[] av=OUTER_V[apex]; r[3]=new double[]{cx+(av[0]-cx)*0.5,cy+(av[1]-cy)*0.5,cz+(av[2]-cz)*0.5};
            return r;
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            for (double[] v : OUTER_V) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(v[0],v[1],v[2]), Material.OBSIDIAN);
                h.scale(1f,1f,1f).glow(128,0,255).interpolation(2,0);
                outerCorners.add(h); spawnedEntities.add(h.entity());
            }
            for (int[] ep : OUTER_E) {
                double[] a=OUTER_V[ep[0]], b=OUTER_V[ep[1]];
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add((a[0]+b[0])/2,(a[1]+b[1])/2,(a[2]+b[2])/2), Material.BLACK_CONCRETE);
                h.scale(0.65f,0.65f,0.65f).glow(80,0,180).interpolation(2,0);
                outerEdges.add(h); spawnedEntities.add(h.entity());
            }
            for (int fi=0;fi<4;fi++) {
                for (double[] c : subTetraCorners(fi)) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(c[0],c[1],c[2]), Material.POLISHED_BLACKSTONE);
                    h.scale(0.45f,0.45f,0.45f).glow(160,40,255).interpolation(2,0);
                    subTetraBlks.add(h); spawnedEntities.add(h.entity());
                }
            }
            for (int[] fv : FACE_VERTS) {
                double cx=0,cy=0,cz=0;
                for (int v:fv) { cx+=OUTER_V[v][0]; cy+=OUTER_V[v][1]; cz+=OUTER_V[v][2]; }
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(cx/3,cy/3,cz/3), Material.CRYING_OBSIDIAN);
                h.scale(0.5f,0.5f,0.5f).glow(100,0,200).interpolation(2,0);
                voidCenters.add(h); spawnedEntities.add(h.entity());
            }
            centroidBlock = displayBuilder.spawnBlock(center.clone().add(0,1,0), Material.AMETHYST_BLOCK);
            centroidBlock.scale(0.8f,0.8f,0.8f).glow(200,100,255).interpolation(2,0);
            spawnedEntities.add(centroidBlock.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            outerRotY += 0.7f;
            for (int fi=0;fi<4;fi++) subRotY[fi] += (fi%2==0) ? 1.4f : -1.4f;
            float breathe = 1f+0.2f*(float)((Math.sin(ticksAlive*Math.PI*2/120.0)+1)/2.0);
            double yR = Math.toRadians(outerRotY);
            double[][] rotV = new double[4][3];
            for (int ci=0;ci<OUTER_V.length;ci++) {
                double[] v=OUTER_V[ci];
                rotV[ci][0]=(v[0]*Math.cos(yR)-v[2]*Math.sin(yR))*breathe;
                rotV[ci][1]=v[1]*breathe;
                rotV[ci][2]=(v[0]*Math.sin(yR)+v[2]*Math.cos(yR))*breathe;
                outerCorners.get(ci).entity().teleport(center.clone().add(rotV[ci][0],rotV[ci][1],rotV[ci][2]));
            }
            for (int i=0;i<OUTER_E.length;i++) {
                double[] a=rotV[OUTER_E[i][0]], b=rotV[OUTER_E[i][1]];
                outerEdges.get(i).entity().teleport(center.clone().add((a[0]+b[0])/2,(a[1]+b[1])/2,(a[2]+b[2])/2));
            }
            int subIdx=0;
            for (int fi=0;fi<4;fi++) {
                double srY = Math.toRadians(subRotY[fi]+outerRotY);
                double[][] sc = subTetraCorners(fi);
                int[] fv = FACE_VERTS[fi];
                double fcx=0,fcy=0,fcz=0;
                for (int v:fv) { fcx+=OUTER_V[v][0]; fcy+=OUTER_V[v][1]; fcz+=OUTER_V[v][2]; }
                fcx/=3; fcy/=3; fcz/=3;
                for (double[] c : sc) {
                    double dx=c[0]-fcx, dz=c[2]-fcz;
                    double rx=dx*Math.cos(srY)-dz*Math.sin(srY)+fcx;
                    double rz=dx*Math.sin(srY)+dz*Math.cos(srY)+fcz;
                    subTetraBlks.get(subIdx++).entity().teleport(center.clone().add(rx*breathe,c[1]*breathe,rz*breathe));
                }
            }
            for (int fi=0;fi<4;fi++) {
                int[] fv=FACE_VERTS[fi]; double cx=0,cy=0,cz=0;
                for (int v:fv) { cx+=OUTER_V[v][0]; cy+=OUTER_V[v][1]; cz+=OUTER_V[v][2]; }
                double rx=(cx/3*Math.cos(yR)-cz/3*Math.sin(yR))*breathe;
                double rz=(cx/3*Math.sin(yR)+cz/3*Math.cos(yR))*breathe;
                voidCenters.get(fi).entity().teleport(center.clone().add(rx,cy/3*breathe,rz));
            }
            centroidBlock.entity().teleport(center.clone().add(0,breathe,0));
            if (ticksAlive%4==0) {
                for (int[] ep : OUTER_E) {
                    double[] a=rotV[ep[0]], b=rotV[ep[1]]; double t2=(ticksAlive%20)/20.0;
                    center.getWorld().spawnParticle(Particle.DUST,
                        center.clone().add(a[0]+(b[0]-a[0])*t2, a[1]+(b[1]-a[1])*t2, a[2]+(b[2]-a[2])*t2),
                        1, 0.05, 0.05, 0.05, new Particle.DustOptions(Color.fromRGB(80,0,120),1f));
                }
            }
            if (ticksAlive%8==0) center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0,1,0), 4,0.2,0.2,0.2,0.05);
            if (ticksAlive%120==0) center.getWorld().playSound(center, Sound.AMBIENT_CAVE, 0.15f, 0.2f);
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FractalSierpinskiFrame(plugin); }
    }

    // ================================================================
    // 59. GOLDEN RATIO SPIRAL — Fibonacci golden-angle phyllotaxis spiral
    // ================================================================
    public static class GoldenRatioSpiral extends BlockDisplayAttack {
        private static final double GOLDEN_ANGLE = Math.toRadians(137.507764);
        private static final int COUNT = 21;
        private static final double[][] SPIRAL_BASE = new double[COUNT][3];
        private static final java.util.Set<Integer> FIB_IDX = new java.util.HashSet<>(
            java.util.Arrays.asList(0,1,2,4,7,12,20));
        static {
            for (int i=0;i<COUNT;i++) {
                SPIRAL_BASE[i][0] = Math.cos(i*GOLDEN_ANGLE)*Math.sqrt(i+1)*0.8;
                SPIRAL_BASE[i][2] = Math.sin(i*GOLDEN_ANGLE)*Math.sqrt(i+1)*0.8;
            }
        }
        private final List<BlockDisplayHandle> spiralBlocks = new ArrayList<>();
        private BlockDisplayHandle centerBlock;
        private float globalAngle = 0;
        private final float[] blockAngles = new float[COUNT];

        private float diffSpeed(int i) {
            if (i < 7) return 2f;
            if (i > 14) return 0.5f;
            return 2f - 1.5f*(i-7)/7f;
        }

        public GoldenRatioSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("golden_ratio_spiral", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0); config.setDamageRadius(6.0);
            config.setDurationTicks(360); config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            for (int i=0;i<COUNT;i++) {
                double[] pos=SPIRAL_BASE[i];
                Material mat = FIB_IDX.contains(i) ? Material.AMETHYST_BLOCK :
                               (i<5) ? Material.CRYING_OBSIDIAN :
                               (i<8) ? Material.BLACKSTONE : Material.OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0],0,pos[2]), mat);
                h.scale(0.75f,0.75f,0.75f).glow(128,0,255).interpolation(2,0);
                spiralBlocks.add(h); spawnedEntities.add(h.entity());
            }
            centerBlock = displayBuilder.spawnBlock(center.clone(), Material.NETHERITE_BLOCK);
            centerBlock.scale(1f,1f,1f).glow(200,120,0).interpolation(2,0);
            spawnedEntities.add(centerBlock.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            globalAngle += 0.5f;
            for (int i=0;i<COUNT;i++) blockAngles[i] += diffSpeed(i);
            double gR = Math.toRadians(globalAngle);
            for (int i=0;i<COUNT;i++) {
                double[] pos=SPIRAL_BASE[i];
                double lr=Math.toRadians(blockAngles[i]);
                double rx=pos[0]*Math.cos(lr)-pos[2]*Math.sin(lr);
                double rz=pos[0]*Math.sin(lr)+pos[2]*Math.cos(lr);
                double fx=rx*Math.cos(gR)-rz*Math.sin(gR), fz=rx*Math.sin(gR)+rz*Math.cos(gR);
                spiralBlocks.get(i).entity().teleport(center.clone().add(fx,0,fz));
            }
            float ang = (float)Math.toRadians(ticksAlive*3.0);
            Transformation t = centerBlock.entity().getTransformation();
            centerBlock.entity().setTransformation(new Transformation(t.getTranslation(),
                new AxisAngle4f(ang, 0.577f, 0.577f, 0.577f), t.getScale(), new AxisAngle4f(0,0,1,0)));
            if (ticksAlive%5==0) {
                double inR=3.0-(ticksAlive%30)*0.1;
                double inA=Math.toRadians(ticksAlive*15.0);
                center.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                    center.clone().add(Math.cos(inA)*inR, 0.5, Math.sin(inA)*inR), 1,0.05,0.1,0.05,0);
            }
            if (ticksAlive%3==0) {
                int ti=ticksAlive%COUNT; double[] pos=SPIRAL_BASE[ti];
                double lr=Math.toRadians(blockAngles[ti]);
                double rx=pos[0]*Math.cos(lr)-pos[2]*Math.sin(lr), rz=pos[0]*Math.sin(lr)+pos[2]*Math.cos(lr);
                double gRad=Math.toRadians(globalAngle);
                double fx=rx*Math.cos(gRad)-rz*Math.sin(gRad), fz=rx*Math.sin(gRad)+rz*Math.cos(gRad);
                center.getWorld().spawnParticle(Particle.DUST, center.clone().add(fx,0,fz),
                    1,0.1,0.1,0.1, new Particle.DustOptions(Color.fromRGB(180,120,0),1.3f));
            }
            if (ticksAlive%8==0) center.getWorld().playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 1f);
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GoldenRatioSpiral(plugin); }
    }

    // ================================================================
    // 60. VOID ICOSAHEDRON — 20-faced polyhedron tumbling on diagonal axis
    // ================================================================
    public static class VoidIcosahedron extends BlockDisplayAttack {
        private static final double PHI = 1.6180339887, S = 1.5;
        private static final double[][] VERTS = {
            {0,S,PHI*S},{0,S,-PHI*S},{0,-S,PHI*S},{0,-S,-PHI*S},
            {S,PHI*S,0},{S,-PHI*S,0},{-S,PHI*S,0},{-S,-PHI*S,0},
            {PHI*S,0,S},{PHI*S,0,-S},{-PHI*S,0,S},{-PHI*S,0,-S}
        };
        private static final double EDGE_THRESH_SQ = 9.0 * 1.05; // edge² = 4*S²=9
        private static final double AX, AY, AZ;
        static { double len=Math.sqrt(1.0+0.25+0.09); AX=1.0/len; AY=0.5/len; AZ=0.3/len; }

        private final List<BlockDisplayHandle> vertBlocks    = new ArrayList<>();
        private final List<BlockDisplayHandle> edgeBlocks    = new ArrayList<>();
        private final List<BlockDisplayHandle> faceBlocks    = new ArrayList<>();
        private final List<BlockDisplayHandle> cryingBlocks  = new ArrayList<>();
        private final List<BlockDisplayHandle> floatAmethyst = new ArrayList<>();
        private BlockDisplayHandle centerBlock;
        private int[][] edgePairs;
        private double[][] faceCenters;
        private float rotAngle = 0, faceNodeAngle = 0, centerPulse = 0;
        private static final int[][] OP_PAIRS = {{0,3},{1,2},{4,7},{5,6},{8,11},{9,10}};
        private static final int[] FLOAT_FACE = {0,5,10,15};

        public VoidIcosahedron(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_icosahedron", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(14.0); config.setDamageRadius(8.0);
            config.setDurationTicks(400); config.setCooldownTicks(500);
        }

        private static double[] rodrigues(double[] p, double angle) {
            double cos=Math.cos(angle), sin=Math.sin(angle), dot=p[0]*AX+p[1]*AY+p[2]*AZ;
            return new double[]{p[0]*cos+(AY*p[2]-AZ*p[1])*sin+AX*dot*(1-cos),
                                p[1]*cos+(AZ*p[0]-AX*p[2])*sin+AY*dot*(1-cos),
                                p[2]*cos+(AX*p[1]-AY*p[0])*sin+AZ*dot*(1-cos)};
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            java.util.List<int[]> ep = new java.util.ArrayList<>();
            for (int i=0;i<12;i++) for (int j=i+1;j<12;j++) {
                double dx=VERTS[i][0]-VERTS[j][0], dy=VERTS[i][1]-VERTS[j][1], dz=VERTS[i][2]-VERTS[j][2];
                if (dx*dx+dy*dy+dz*dz < EDGE_THRESH_SQ) ep.add(new int[]{i,j});
            }
            edgePairs = ep.toArray(new int[0][]);
            boolean[][] adj = new boolean[12][12];
            for (int[] e : edgePairs) adj[e[0]][e[1]] = adj[e[1]][e[0]] = true;
            java.util.List<double[]> fc = new java.util.ArrayList<>();
            for (int i=0;i<12;i++) for (int j=i+1;j<12;j++) if (adj[i][j])
                for (int k=j+1;k<12;k++) if (adj[i][k]&&adj[j][k])
                    fc.add(new double[]{(VERTS[i][0]+VERTS[j][0]+VERTS[k][0])/3,
                                        (VERTS[i][1]+VERTS[j][1]+VERTS[k][1])/3,
                                        (VERTS[i][2]+VERTS[j][2]+VERTS[k][2])/3});
            faceCenters = fc.toArray(new double[0][]);
            for (double[] v : VERTS) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(v[0],v[1],v[2]), Material.OBSIDIAN);
                h.scale(0.9f,0.9f,0.9f).glow(128,0,255).interpolation(2,0);
                vertBlocks.add(h); spawnedEntities.add(h.entity());
            }
            for (int[] e : edgePairs) {
                double[] a=VERTS[e[0]], b=VERTS[e[1]];
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add((a[0]+b[0])/2,(a[1]+b[1])/2,(a[2]+b[2])/2), Material.BLACK_CONCRETE);
                h.scale(0.6f,0.6f,0.6f).glow(80,0,160).interpolation(2,0);
                edgeBlocks.add(h); spawnedEntities.add(h.entity());
            }
            for (double[] f : faceCenters) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(f[0],f[1],f[2]), Material.BLACK_GLAZED_TERRACOTTA);
                h.scale(0.55f,0.55f,0.55f).glow(40,0,80).interpolation(2,0);
                faceBlocks.add(h); spawnedEntities.add(h.entity());
            }
            for (int[] p : OP_PAIRS) for (int vi : p) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add(VERTS[vi][0],VERTS[vi][1],VERTS[vi][2]), Material.CRYING_OBSIDIAN);
                h.scale(0.7f,0.7f,0.7f).glow(160,0,200).interpolation(2,0);
                cryingBlocks.add(h); spawnedEntities.add(h.entity());
            }
            for (int fi : FLOAT_FACE) {
                if (fi < faceCenters.length) {
                    double[] f=faceCenters[fi]; double mag=Math.sqrt(f[0]*f[0]+f[1]*f[1]+f[2]*f[2]);
                    double ext=mag+1.2;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(f[0]/mag*ext,f[1]/mag*ext,f[2]/mag*ext), Material.AMETHYST_BLOCK);
                    h.scale(0.8f,0.8f,0.8f).glow(200,160,255).interpolation(2,0);
                    floatAmethyst.add(h); spawnedEntities.add(h.entity());
                }
            }
            centerBlock = displayBuilder.spawnBlock(center.clone(), Material.NETHERITE_BLOCK);
            centerBlock.scale(1f,1f,1f).glow(50,0,100).interpolation(2,0);
            spawnedEntities.add(centerBlock.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            rotAngle += (float)Math.toRadians(0.6);
            faceNodeAngle += (float)Math.toRadians(1.0);
            centerPulse += (float)Math.toRadians(6.0);
            double[][] rotV = new double[12][3];
            for (int i=0;i<12;i++) {
                rotV[i] = rodrigues(VERTS[i], rotAngle);
                vertBlocks.get(i).entity().teleport(center.clone().add(rotV[i][0],rotV[i][1],rotV[i][2]));
            }
            for (int i=0;i<edgePairs.length && i<edgeBlocks.size();i++) {
                double[] a=rotV[edgePairs[i][0]], b=rotV[edgePairs[i][1]];
                edgeBlocks.get(i).entity().teleport(center.clone().add((a[0]+b[0])/2,(a[1]+b[1])/2,(a[2]+b[2])/2));
            }
            for (int i=0;i<faceCenters.length && i<faceBlocks.size();i++) {
                double[] rf = rodrigues(faceCenters[i], rotAngle);
                faceBlocks.get(i).entity().teleport(center.clone().add(rf[0],rf[1],rf[2]));
            }
            int ci=0;
            for (int[] p : OP_PAIRS) for (int vi : p)
                if (ci<cryingBlocks.size())
                    cryingBlocks.get(ci++).entity().teleport(center.clone().add(rotV[vi][0],rotV[vi][1],rotV[vi][2]));
            for (int i=0;i<floatAmethyst.size() && i<FLOAT_FACE.length;i++) {
                int fi=FLOAT_FACE[i];
                if (fi<faceCenters.length) {
                    double[] rf=rodrigues(faceCenters[fi],rotAngle);
                    double mag=Math.sqrt(rf[0]*rf[0]+rf[1]*rf[1]+rf[2]*rf[2]);
                    double ext=mag+1.2+0.3*Math.sin(faceNodeAngle+i);
                    floatAmethyst.get(i).entity().teleport(center.clone().add(rf[0]/mag*ext,rf[1]/mag*ext,rf[2]/mag*ext));
                }
            }
            float pulse = 1f+(float)((Math.sin(centerPulse)+1)/2.0);
            Transformation t = centerBlock.entity().getTransformation();
            centerBlock.entity().setTransformation(new Transformation(
                t.getTranslation(), t.getLeftRotation(), new Vector3f(pulse,pulse,pulse), t.getRightRotation()));
            if (ticksAlive%20==0) {
                for (double[] rv : rotV)
                    center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(rv[0],rv[1],rv[2]), 2,0.1,0.1,0.1,0.01);
            }
            if (ticksAlive%4==0 && faceCenters.length>0) {
                double[] rf=rodrigues(faceCenters[ticksAlive/4%faceCenters.length],rotAngle);
                center.getWorld().spawnParticle(Particle.DUST, center.clone().add(rf[0],rf[1],rf[2]),
                    1,0.1,0.1,0.1, new Particle.DustOptions(Color.fromRGB(10,0,20),1.1f));
            }
            if (ticksAlive%3==0) {
                double[] rv=rodrigues(VERTS[ticksAlive/3%12],rotAngle);
                center.getWorld().spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, center.clone().add(rv[0],rv[1],rv[2]), 1,0.05,0.05,0.05,0);
            }
            if (ticksAlive%30==0) center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1f);
            if (ticksAlive%25==0) center.getWorld().playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.8f);
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VoidIcosahedron(plugin); }
    }
}
