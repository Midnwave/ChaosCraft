package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

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
 * Phase 5E Block Display -- GROUP 3: BRIMSTONE SKULL ARRAYS (#19-28)
 * 10 skull-themed structures using magma blocks and nether wart blocks
 * shaped and glowing as spectral skull formations. Supreme Calamitas
 * wields brimstone skulls as her secondary weapon system -- slow,
 * fire-dripping, deeply oppressive.
 *
 * NOTE: Skulls are approximated via NETHER_WART_BLOCK and MAGMA_BLOCK
 * BlockDisplays at various scales and orientations to create skull-like
 * composite shapes. The magma block internal glow reads as burning eyes.
 *
 * Structures:
 *  #19  NorthernSkullFormation    -- 7 skull composites in shallow arc, 20N, Y+10-16
 *  #20  SouthernSkullArc          -- Mirror of #19, 20S, phase-offset breathing
 *  #21  OverheadSkullDiamond      -- 4 tumbling skulls at Y+22, N/E/S/W
 *  #22  SkullOrbitRing            -- 9 skulls orbiting at Y+16, altitude oscillation
 *  #23  SkullColumnTower          -- 12 skulls stacked vertically, 26W, brightness wave
 *  #24  CentralGroundSkulls       -- 3 oversized floor-level skulls in sigil zone
 *  #25  BrimstoneSkullHalo        -- 6 skulls orbiting above her, tilted ring
 *  #26  SkullConstellation        -- 11 skulls in skeletal hand pattern, connectors
 *  #27  PairedSkullSentinels      -- 2 flanking skulls at eye level, Ground Phase only
 *  #28  SkullStormCanopy          -- 30+ drifting small skulls, Phase 4 rain effect
 *
 * Rules applied:
 * - NO status effects
 * - Calamitas palette: crimson(200,0,50), brimstone(255,100,0), soul blue(0,150,255)
 * - Damage HP 4.0-14.0
 * - AxisAngle4f ONLY (never Quaternionf)
 * - Static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Location center = getCenter(); if (center == null) return; EVERY onTick
 */
public final class BrimstoneSkullArrays {

    private BrimstoneSkullArrays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new NorthernSkullFormation(plugin));
        registry.register(new SouthernSkullArc(plugin));
        registry.register(new OverheadSkullDiamond(plugin));
        registry.register(new SkullOrbitRing(plugin));
        registry.register(new SkullColumnTower(plugin));
        registry.register(new CentralGroundSkulls(plugin));
        registry.register(new BrimstoneSkullHalo(plugin));
        registry.register(new SkullConstellation(plugin));
        registry.register(new PairedSkullSentinels(plugin));
        registry.register(new SkullStormCanopy(plugin));
    }

    // ================================================================
    // #19 -- NORTHERN SKULL FORMATION
    // 7 composite skulls in shallow upward arc, 20N, Y+10 to Y+16.
    // Each skull: nether wart block body + magma block eyes.
    // Y-axis rotation at 1.5 deg/tick with staggered phase offsets.
    // Central 3 skulls breathe (Y oscillation, 80-tick period).
    // ================================================================
    public static class NorthernSkullFormation extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skullBodies = new ArrayList<>();
        private final List<BlockDisplayHandle> skullEyes = new ArrayList<>();
        private final double[] lateralPositions = {-6, -4, -2, 0, 2, 4, 6};
        private final double[] arcYOffsets = {0, 0.8, 1.5, 2.0, 1.5, 0.8, 0};

        public NorthernSkullFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_north_skull_arc", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location arcBase = center.clone().add(0, 10, -20);

            for (int i = 0; i < 7; i++) {
                Location skullLoc = arcBase.clone().add(lateralPositions[i], arcYOffsets[i], 0);

                // Skull body: nether wart block
                BlockDisplayHandle body = displayBuilder.spawnBlock(skullLoc, Material.NETHER_WART_BLOCK);
                body.scale(0.9f, 0.9f, 0.9f).glow(200, 0, 50).interpolation(3, 0);
                skullBodies.add(body);
                spawnedEntities.add(body.entity());

                // Eye glow: small magma block inset
                Location eyeLoc = skullLoc.clone().add(0, 0.1, 0.3);
                BlockDisplayHandle eye = displayBuilder.spawnBlock(eyeLoc, Material.MAGMA_BLOCK);
                eye.scale(0.4f, 0.3f, 0.2f).glow(255, 100, 0);
                skullEyes.add(eye);
                spawnedEntities.add(eye.entity());
            }

            DisplayBuilder.playSound(arcBase, Sound.ENTITY_WITHER_AMBIENT, 0.2f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location arcBase = center.clone().add(0, 10, -20);

            for (int i = 0; i < skullBodies.size(); i++) {
                // Y-axis rotation at 1.5 deg/tick with phase offsets
                float rotAngle = (float) Math.toRadians(ticksAlive * 1.5 + i * 20);
                skullBodies.get(i).rotate(rotAngle, 0, 1, 0);
                skullBodies.get(i).interpolation(3, 0);

                // Central 3 skulls (indices 2,3,4) breathe
                if (i >= 2 && i <= 4) {
                    float yDisp = 0.5f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 80.0);
                    Location breathLoc = arcBase.clone().add(lateralPositions[i], arcYOffsets[i] + yDisp, 0);
                    skullBodies.get(i).entity().teleport(breathLoc);
                    skullEyes.get(i).entity().teleport(breathLoc.clone().add(0, 0.1, 0.3));
                }

                // SOUL_FIRE_FLAME rising from skull surface
                if (ticksAlive % 4 == i % 4) {
                    Location flameLoc = arcBase.clone().add(lateralPositions[i], arcYOffsets[i] + 0.5, -20);
                    Location particleLoc = skullBodies.get(i).entity().getLocation().add(0, 0.5, 0);
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.15, 0.1, 0.15, 0.01);
                }

                // LAVA drip downward
                if (ticksAlive % 10 == i % 7) {
                    Location dripLoc = skullBodies.get(i).entity().getLocation().add(0, -0.3, 0);
                    center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, dripLoc, 1, 0.1, 0.05, 0.1, 0);
                }

                // Dark crimson dust outward
                if (ticksAlive % 7 == i % 7) {
                    Location dustLoc = skullBodies.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(dustLoc, 1, 0.3, 80, 0, 0, 1.2f);
                }
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(arcBase, Sound.ENTITY_WITHER_AMBIENT, 0.2f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NorthernSkullFormation(plugin); }
    }

    // ================================================================
    // #20 -- SOUTHERN SKULL ARC
    // Mirror of #19 at 20S, faces north. 40-tick phase offset on
    // breathing so north breathes up as south breathes down.
    // ================================================================
    public static class SouthernSkullArc extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skullBodies = new ArrayList<>();
        private final List<BlockDisplayHandle> skullEyes = new ArrayList<>();
        private final double[] lateralPositions = {-6, -4, -2, 0, 2, 4, 6};
        private final double[] arcYOffsets = {0, 0.8, 1.5, 2.0, 1.5, 0.8, 0};

        public SouthernSkullArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_south_skull_arc", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location arcBase = center.clone().add(0, 10, 20);

            for (int i = 0; i < 7; i++) {
                Location skullLoc = arcBase.clone().add(lateralPositions[i], arcYOffsets[i], 0);

                BlockDisplayHandle body = displayBuilder.spawnBlock(skullLoc, Material.NETHER_WART_BLOCK);
                body.scale(0.9f, 0.9f, 0.9f).glow(200, 0, 50).interpolation(3, 0);
                skullBodies.add(body);
                spawnedEntities.add(body.entity());

                Location eyeLoc = skullLoc.clone().add(0, 0.1, -0.3);
                BlockDisplayHandle eye = displayBuilder.spawnBlock(eyeLoc, Material.MAGMA_BLOCK);
                eye.scale(0.4f, 0.3f, 0.2f).glow(255, 100, 0);
                skullEyes.add(eye);
                spawnedEntities.add(eye.entity());
            }

            DisplayBuilder.playSound(arcBase, Sound.ENTITY_WITHER_AMBIENT, 0.2f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location arcBase = center.clone().add(0, 10, 20);

            for (int i = 0; i < skullBodies.size(); i++) {
                float rotAngle = (float) Math.toRadians(ticksAlive * 1.5 + i * 20);
                skullBodies.get(i).rotate(rotAngle, 0, 1, 0);
                skullBodies.get(i).interpolation(3, 0);

                // Central 3 breathe with 40-tick offset from north
                if (i >= 2 && i <= 4) {
                    float yDisp = 0.5f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 80.0 + Math.PI);
                    Location breathLoc = arcBase.clone().add(lateralPositions[i], arcYOffsets[i] + yDisp, 0);
                    skullBodies.get(i).entity().teleport(breathLoc);
                    skullEyes.get(i).entity().teleport(breathLoc.clone().add(0, 0.1, -0.3));
                }

                if (ticksAlive % 4 == i % 4) {
                    Location particleLoc = skullBodies.get(i).entity().getLocation().add(0, 0.5, 0);
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.15, 0.1, 0.15, 0.01);
                }

                if (ticksAlive % 10 == i % 7) {
                    Location dripLoc = skullBodies.get(i).entity().getLocation().add(0, -0.3, 0);
                    center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, dripLoc, 1, 0.1, 0.05, 0.1, 0);
                }

                if (ticksAlive % 7 == i % 7) {
                    Location dustLoc = skullBodies.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(dustLoc, 1, 0.3, 80, 0, 0, 1.2f);
                }
            }

            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(arcBase, Sound.ENTITY_WITHER_AMBIENT, 0.2f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SouthernSkullArc(plugin); }
    }

    // ================================================================
    // #21 -- OVERHEAD SKULL DIAMOND
    // 4 composite skulls at Y+22 in cardinal diamond, tumbling on
    // all 3 axes simultaneously. SMOKE + LAVA rain downward.
    // Phase 3: group orbits at 0.5 deg/tick.
    // ================================================================
    public static class OverheadSkullDiamond extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skulls = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final double[][] cardinalOffsets = {{0, 0, -8}, {8, 0, 0}, {0, 0, 8}, {-8, 0, 0}};

        public OverheadSkullDiamond(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_overhead_skull_diamond", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (double[] offset : cardinalOffsets) {
                Location loc = center.clone().add(offset[0], 22, offset[2]);

                BlockDisplayHandle skull = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                skull.scale(1.2f, 1.2f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
                skulls.add(skull);
                spawnedEntities.add(skull.entity());

                BlockDisplayHandle eye = displayBuilder.spawnBlock(loc.clone().add(0, 0.15, 0), Material.MAGMA_BLOCK);
                eye.scale(0.5f, 0.4f, 0.3f).glow(255, 100, 0);
                eyes.add(eye);
                spawnedEntities.add(eye.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 22, 0), Sound.ENTITY_WITHER_AMBIENT, 0.15f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Group orbit (starts at tick 1500 as Phase 3 proxy)
            double groupOrbit = ticksAlive > 1500 ? Math.toRadians(ticksAlive * 0.5) : 0;

            for (int i = 0; i < skulls.size(); i++) {
                // Individual tumble: different rates per axis per skull
                float tumbleX = (float) Math.toRadians(ticksAlive * (1.0 + i * 0.3));
                float tumbleY = (float) Math.toRadians(ticksAlive * (2.0 + i * 0.2));
                float tumbleZ = (float) Math.toRadians(ticksAlive * (0.5 + i * 0.15));

                // Combined rotation via Y-axis (primary tumble axis)
                skulls.get(i).rotate(tumbleY, 0, 1, 0);
                skulls.get(i).interpolation(3, 0);

                // Group orbit position
                double baseAngle = Math.atan2(cardinalOffsets[i][2], cardinalOffsets[i][0]);
                double orbAngle = baseAngle + groupOrbit;
                double radius = 8.0;
                double px = Math.cos(orbAngle) * radius;
                double pz = Math.sin(orbAngle) * radius;
                Location newLoc = center.clone().add(px, 22, pz);
                skulls.get(i).entity().teleport(newLoc);
                eyes.get(i).entity().teleport(newLoc.clone().add(0, 0.15, 0));

                // SMOKE drifting down
                if (ticksAlive % 4 == i) {
                    center.getWorld().spawnParticle(Particle.SMOKE, newLoc, 2, 0.3, 0.5, 0.3, 0.01);
                }

                // LAVA dripping
                if (ticksAlive % 5 == i) {
                    center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, newLoc, 1, 0.2, 0.1, 0.2, 0);
                }
            }

            // Sound from each skull every 120 ticks
            if (ticksAlive % 120 == 0) {
                for (BlockDisplayHandle skull : skulls) {
                    DisplayBuilder.playSound(skull.entity().getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.15f, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OverheadSkullDiamond(plugin); }
    }

    // ================================================================
    // #22 -- SKULL ORBIT RING
    // 9 composite skulls orbiting at radius 10, Y+16. Clockwise 3 deg/tick.
    // 6 standard + 3 crimson (darker glow). Altitude oscillates Y+14 to Y+18.
    // Phase 4: oscillation widens to Y+11 to Y+21.
    // ================================================================
    public static class SkullOrbitRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skulls = new ArrayList<>();
        private final List<BlockDisplayHandle> skullEyes = new ArrayList<>();
        private static final int COUNT = 9;
        private static final double RADIUS = 10.0;

        public SkullOrbitRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_skull_orbit_ring", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / COUNT;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                Location loc = center.clone().add(px, 16, pz);

                boolean isCrimson = (i % 3 == 0); // 3 crimson at 120 deg intervals

                BlockDisplayHandle skull = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                skull.scale(0.8f, 0.8f, 0.8f);
                if (isCrimson) {
                    skull.glow(200, 0, 50);
                } else {
                    skull.glow(128, 0, 255);
                }
                skull.interpolation(2, 0);
                skulls.add(skull);
                spawnedEntities.add(skull.entity());

                BlockDisplayHandle eye = displayBuilder.spawnBlock(loc.clone().add(0, 0.1, 0), Material.MAGMA_BLOCK);
                eye.scale(0.35f, 0.25f, 0.2f).glow(255, 100, 0);
                skullEyes.add(eye);
                spawnedEntities.add(eye.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 16, 0), Sound.ENTITY_WITHER_SHOOT, 0.3f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Clockwise orbit at 3 deg/tick
            double orbitAngle = -Math.toRadians(ticksAlive * 3.0);

            // Altitude oscillation: Y+14 to Y+18 (60-tick period)
            double yOsc = 2.0 * Math.sin(ticksAlive * 2.0 * Math.PI / 60.0);
            double currentY = 16.0 + yOsc;

            for (int i = 0; i < skulls.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / COUNT;
                double angle = baseAngle + orbitAngle;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;

                Location newLoc = center.clone().add(px, currentY, pz);
                skulls.get(i).entity().teleport(newLoc);
                skullEyes.get(i).entity().teleport(newLoc.clone().add(0, 0.1, 0));

                // Particle trails based on type
                boolean isCrimson = (i % 3 == 0);
                if (ticksAlive % 5 == i % 5) {
                    if (isCrimson) {
                        DisplayBuilder.crimsonDust(newLoc, 2, 0.2);
                        center.getWorld().spawnParticle(Particle.FLAME, newLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    } else {
                        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, newLoc, 2, 0.1, 0.1, 0.1, 0.01);
                        center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, newLoc, 1, 0.1, 0.05, 0.1, 0);
                    }
                }
            }

            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, currentY, 0), Sound.ENTITY_WITHER_SHOOT, 0.3f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkullOrbitRing(plugin); }
    }

    // ================================================================
    // #23 -- SKULL COLUMN TOWER
    // 12 composite skulls stacked vertically, 26W, Y+0 to Y+22.
    // All face west, slow Y rotation at 1 deg/tick.
    // Brightness wave sweeps upward every 80 ticks.
    // ================================================================
    public static class SkullColumnTower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skulls = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private static final int COUNT = 12;

        public SkullColumnTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_skull_column_tower", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location towerBase = center.clone().add(-26, 0, 0);

            for (int i = 0; i < COUNT; i++) {
                double y = i * 2.0;
                Location loc = towerBase.clone().add(0, y, 0);

                BlockDisplayHandle skull = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                skull.scale(0.7f, 0.7f, 0.7f).glow(200, 0, 50).interpolation(3, 0);
                skulls.add(skull);
                spawnedEntities.add(skull.entity());

                BlockDisplayHandle eye = displayBuilder.spawnBlock(loc.clone().add(-0.2, 0.1, 0), Material.MAGMA_BLOCK);
                eye.scale(0.3f, 0.2f, 0.15f).glow(255, 100, 0);
                eyes.add(eye);
                spawnedEntities.add(eye.entity());
            }

            DisplayBuilder.playSound(towerBase, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.15f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location towerBase = center.clone().add(-26, 0, 0);

            // Brightness wave: sweeps upward every 80 ticks
            int wavePosition = (ticksAlive % 80) * COUNT / 80;

            for (int i = 0; i < skulls.size(); i++) {
                // Slow Y rotation
                float rotAngle = (float) Math.toRadians(ticksAlive * 1.0);
                skulls.get(i).rotate(rotAngle, 0, 1, 0);
                skulls.get(i).interpolation(3, 0);

                // Brightness wave: pulse from 5 to 20 SOUL_FIRE_FLAME per second
                boolean inWave = (i == wavePosition || i == wavePosition - 1);

                // SOUL_FIRE_FLAME emission
                int emitRate = inWave ? 1 : 4; // Higher rate during wave
                if (ticksAlive % emitRate == 0) {
                    Location flameLoc = towerBase.clone().add(0, i * 2.0 + 0.5, 0);
                    int count = inWave ? 4 : 1;
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, flameLoc, count, 0.15, 0.1, 0.15, 0.01);
                }

                // LAVA drip
                if (ticksAlive % 10 == i % 10) {
                    Location dripLoc = towerBase.clone().add(0, i * 2.0 - 0.2, 0);
                    center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, dripLoc, 1, 0.1, 0.05, 0.1, 0);
                }

                // Dark crimson dust
                if (ticksAlive % 10 == (i + 5) % 10) {
                    Location dustLoc = towerBase.clone().add(0, i * 2.0, 0);
                    DisplayBuilder.dustParticles(dustLoc, 1, 0.3, 80, 0, 0, 1.2f);
                }
            }

            // Sound when wave reaches top
            if (ticksAlive % 80 == 78) {
                DisplayBuilder.playSound(towerBase.clone().add(0, 22, 0), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.15f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkullColumnTower(plugin); }
    }

    // ================================================================
    // #24 -- CENTRAL GROUND SKULLS
    // 3 oversized composite skulls at floor level within sigil zone.
    // Scale 2.0. Tumble on all axes. LAVA + SOUL_FIRE_FLAME + SMOKE.
    // 20-tick pulse: Y scale 2.0 -> 2.4, staggered 6-tick offset.
    // ================================================================
    public static class CentralGroundSkulls extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skulls = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeSets = new ArrayList<>();
        private final double[][] positions = {{0, 0.5, 0}, {0, 0.5, -6}, {0, 0.5, 6}};

        public CentralGroundSkulls(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_central_ground_skulls", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(4000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (double[] pos : positions) {
                Location loc = center.clone().add(pos[0], pos[1], pos[2]);

                BlockDisplayHandle skull = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                skull.scale(2.0f, 2.0f, 2.0f).glow(200, 0, 50).interpolation(5, 0);
                skulls.add(skull);
                spawnedEntities.add(skull.entity());

                BlockDisplayHandle eye = displayBuilder.spawnBlock(loc.clone().add(0, 0.3, 0.5), Material.MAGMA_BLOCK);
                eye.scale(0.8f, 0.6f, 0.4f).glow(255, 100, 0);
                eyeSets.add(eye);
                spawnedEntities.add(eye.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            for (int i = 0; i < skulls.size(); i++) {
                // Tumble rotation (different axis weights per skull)
                float tumbleAngle = (float) Math.toRadians(ticksAlive * (1.0 + i * 0.3));
                skulls.get(i).rotate(tumbleAngle, 0, 1, 0);
                skulls.get(i).interpolation(5, 0);

                // Y scale pulse: 2.0 -> 2.4 -> 2.0, staggered
                int pulsePhase = (ticksAlive + i * 6) % 20;
                float yScale;
                if (pulsePhase < 3) {
                    yScale = 2.0f + 0.4f * (pulsePhase / 3.0f);
                } else if (pulsePhase < 8) {
                    yScale = 2.4f - 0.4f * ((pulsePhase - 3) / 5.0f);
                } else {
                    yScale = 2.0f;
                }
                skulls.get(i).scale(2.0f, yScale, 2.0f);

                Location skullLoc = center.clone().add(positions[i][0], positions[i][1], positions[i][2]);

                // LAVA dripping at bases
                if (ticksAlive % 3 == i) {
                    center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, skullLoc.clone().add(0, -0.2, 0), 2, 0.5, 0.1, 0.5, 0);
                }

                // SOUL_FIRE_FLAME rising
                if (ticksAlive % 2 == 0) {
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, skullLoc.clone().add(0, 1.0, 0), 2, 0.3, 0.2, 0.3, 0.02);
                }

                // SMOKE
                if (ticksAlive % 4 == i) {
                    center.getWorld().spawnParticle(Particle.SMOKE, skullLoc.clone().add(0, 0.8, 0), 2, 0.4, 0.3, 0.4, 0.01);
                }

                // Dark crimson dust
                if (ticksAlive % 3 == (i + 1) % 3) {
                    DisplayBuilder.dustParticles(skullLoc, 2, 0.5, 80, 0, 0, 1.5f);
                }
            }

            // Wither spawn sound every 200 ticks
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CentralGroundSkulls(plugin); }
    }

    // ================================================================
    // #25 -- BRIMSTONE SKULL HALO
    // 6 composite skulls orbiting above her at radius 6, Y+29.
    // Tilted ring (15 deg). Orbit at 4 deg/tick. Tracks her XZ
    // with 4-tick position lag. SOUL_FIRE_FLAME up + LAVA down.
    // Phase 4: tilt increases to 35 deg, speed doubles.
    // ================================================================
    public static class BrimstoneSkullHalo extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skulls = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private static final int COUNT = 6;
        private static final double RADIUS = 6.0;
        private static final double Y_ALT = 29.0;
        private static final double TILT_DEG = 15.0;

        public BrimstoneSkullHalo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_skull_halo", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / COUNT;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                // Tilted ring: Y varies based on angle
                double tiltY = 1.5 * Math.sin(angle) * Math.sin(Math.toRadians(TILT_DEG));
                Location loc = center.clone().add(px, Y_ALT + tiltY, pz);

                BlockDisplayHandle skull = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                skull.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                skulls.add(skull);
                spawnedEntities.add(skull.entity());

                BlockDisplayHandle eye = displayBuilder.spawnBlock(loc.clone().add(0, 0.12, 0), Material.MAGMA_BLOCK);
                eye.scale(0.4f, 0.3f, 0.2f).glow(255, 100, 0);
                eyes.add(eye);
                spawnedEntities.add(eye.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbit at 4 deg/tick
            double orbitAngle = Math.toRadians(ticksAlive * 4.0);

            for (int i = 0; i < skulls.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / COUNT;
                double angle = baseAngle + orbitAngle;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                double tiltY = 1.5 * Math.sin(angle) * Math.sin(Math.toRadians(TILT_DEG));

                Location newLoc = center.clone().add(px, Y_ALT + tiltY, pz);
                skulls.get(i).entity().teleport(newLoc);
                eyes.get(i).entity().teleport(newLoc.clone().add(0, 0.12, 0));

                // SOUL_FIRE_FLAME upward
                if (ticksAlive % 3 == i % 3) {
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, newLoc.clone().add(0, 0.5, 0), 2, 0.1, 0.1, 0.1, 0.02);
                }

                // LAVA downward (falls through fight altitude)
                if (ticksAlive % 5 == i % 5) {
                    center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, newLoc.clone().add(0, -0.3, 0), 1, 0.1, 0.05, 0.1, 0);
                }
            }

            // Sound every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSkullHalo(plugin); }
    }

    // ================================================================
    // #26 -- SKULL CONSTELLATION
    // 11 composite skulls at irregular positions Y+17 to Y+23,
    // forming skeletal hand silhouette. Connected by soul soil
    // filament segments. Entire constellation drifts east-west.
    // ================================================================
    public static class SkullConstellation extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> skulls = new ArrayList<>();
        private final List<BlockDisplayHandle> connectors = new ArrayList<>();
        // Hand constellation: palm center + 5 fingers (2 nodes each) + wrist
        private static final double[][] NODES = {
            {0, 19, 0},       // 0: palm center (largest)
            {-2, 20, 1},      // 1: thumb base
            {-3, 21.5, 2},    // 2: thumb tip
            {-1, 21, -1},     // 3: index base
            {-1.5, 23, -1.5}, // 4: index tip
            {0, 21, -0.5},    // 5: middle base
            {0, 23, -1},      // 6: middle tip
            {1, 21, 0},       // 7: ring base
            {1.5, 22.5, 0.5}, // 8: ring tip
            {2, 20, 1},       // 9: pinky base
            {2.5, 21.5, 1.5}  // 10: pinky tip
        };
        private static final float[] SCALES = {1.1f, 0.7f, 0.5f, 0.8f, 0.5f, 0.9f, 0.6f, 0.8f, 0.5f, 0.7f, 0.5f};

        public SkullConstellation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_skull_constellation", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn skulls
            for (int i = 0; i < NODES.length; i++) {
                Location loc = center.clone().add(NODES[i][0], NODES[i][1], NODES[i][2]);
                BlockDisplayHandle skull = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                skull.scale(SCALES[i], SCALES[i], SCALES[i]).glow(200, 0, 50).interpolation(3, 0);
                skulls.add(skull);
                spawnedEntities.add(skull.entity());
            }

            // Connecting filaments: pairs of adjacent nodes
            int[][] connections = {{0,1},{0,3},{0,5},{0,7},{0,9},{1,2},{3,4},{5,6},{7,8},{9,10}};
            for (int[] conn : connections) {
                // Midpoint connector using soul soil
                double mx = (NODES[conn[0]][0] + NODES[conn[1]][0]) / 2.0;
                double my = (NODES[conn[0]][1] + NODES[conn[1]][1]) / 2.0;
                double mz = (NODES[conn[0]][2] + NODES[conn[1]][2]) / 2.0;
                Location midLoc = center.clone().add(mx, my, mz);

                double dx = NODES[conn[1]][0] - NODES[conn[0]][0];
                double dy = NODES[conn[1]][1] - NODES[conn[0]][1];
                double dz = NODES[conn[1]][2] - NODES[conn[0]][2];
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

                BlockDisplayHandle filament = displayBuilder.spawnBlock(midLoc, Material.SOUL_SOIL);
                // Thin stretched connector
                filament.scale(0.05f, (float) dist, 0.05f);
                // Rotate to align with connection direction
                float rotAngle = (float) Math.atan2(dx, dy);
                filament.rotate(rotAngle, 0, 0, 1);
                filament.glow(0, 150, 255).brightness(6, 6);
                connectors.add(filament);
                spawnedEntities.add(filament.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slow east-west drift: 0.02 blocks/tick, reverses every 500 ticks
            double driftCycle = (ticksAlive % 1000);
            double xDrift = driftCycle < 500 ? (driftCycle * 0.02) : ((1000 - driftCycle) * 0.02);
            xDrift -= 5.0; // Center the oscillation

            for (int i = 0; i < skulls.size(); i++) {
                Location baseLoc = center.clone().add(NODES[i][0] + xDrift, NODES[i][1], NODES[i][2]);
                skulls.get(i).entity().teleport(baseLoc);

                // Individual skull Y rotation with unique phase
                float rotAngle = (float) Math.toRadians(ticksAlive * 1.0 + i * 33);
                skulls.get(i).rotate(rotAngle, 0, 1, 0);
                skulls.get(i).interpolation(3, 0);

                // SOUL_FIRE_FLAME + crimson dust
                if (ticksAlive % 4 == i % 4) {
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, baseLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    DisplayBuilder.crimsonDust(baseLoc, 1, 0.2);
                }
            }

            // Drift connectors too
            int[][] connections = {{0,1},{0,3},{0,5},{0,7},{0,9},{1,2},{3,4},{5,6},{7,8},{9,10}};
            for (int c = 0; c < connectors.size(); c++) {
                int[] conn = connections[c];
                double mx = (NODES[conn[0]][0] + NODES[conn[1]][0]) / 2.0 + xDrift;
                double my = (NODES[conn[0]][1] + NODES[conn[1]][1]) / 2.0;
                double mz = (NODES[conn[0]][2] + NODES[conn[1]][2]) / 2.0;
                Location midLoc = center.clone().add(mx, my, mz);
                connectors.get(c).entity().teleport(midLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkullConstellation(plugin); }
    }

    // ================================================================
    // #27 -- PAIRED SKULL SENTINELS
    // 2 large composite skulls at eye level (Y+4), 5E and 5W.
    // Only present during Ground Phase sequences. Scale 1.6.
    // Track her facing direction at 5 deg/tick. Collapse exit anim.
    // ================================================================
    public static class PairedSkullSentinels extends BlockDisplayAttack {

        private BlockDisplayHandle eastSkull;
        private BlockDisplayHandle eastEye;
        private BlockDisplayHandle westSkull;
        private BlockDisplayHandle westEye;

        public PairedSkullSentinels(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_skull_sentinels", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(9.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // East sentinel
            Location eLoc = center.clone().add(5, 4, 0);
            eastSkull = displayBuilder.spawnBlock(eLoc, Material.NETHER_WART_BLOCK);
            eastSkull.scale(1.6f, 1.6f, 1.6f).glow(200, 0, 50).interpolation(5, 0);
            spawnedEntities.add(eastSkull.entity());

            eastEye = displayBuilder.spawnBlock(eLoc.clone().add(-0.3, 0.2, 0), Material.MAGMA_BLOCK);
            eastEye.scale(0.6f, 0.5f, 0.3f).glow(255, 100, 0);
            spawnedEntities.add(eastEye.entity());

            // West sentinel (brimstone variant: crimson nylium instead)
            Location wLoc = center.clone().add(-5, 4, 0);
            westSkull = displayBuilder.spawnBlock(wLoc, Material.CRIMSON_NYLIUM);
            westSkull.scale(1.6f, 1.6f, 1.6f).glow(255, 100, 0).interpolation(5, 0);
            spawnedEntities.add(westSkull.entity());

            westEye = displayBuilder.spawnBlock(wLoc.clone().add(0.3, 0.2, 0), Material.MAGMA_BLOCK);
            westEye.scale(0.6f, 0.5f, 0.3f).glow(255, 100, 0);
            spawnedEntities.add(westEye.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slow rotation tracking toward center (simulating tracking her position)
            float trackAngle = (float) Math.toRadians(ticksAlive * 5.0 % 360);
            float limitedAngle = (float) Math.toRadians(Math.sin(ticksAlive * 0.05) * 30);

            if (eastSkull != null) {
                eastSkull.rotate(limitedAngle, 0, 1, 0);
                eastSkull.interpolation(5, 0);
            }
            if (westSkull != null) {
                westSkull.rotate(-limitedAngle, 0, 1, 0);
                westSkull.interpolation(5, 0);
            }

            // SOUL_FIRE_FLAME + LAVA from both sentinels
            if (ticksAlive % 3 == 0) {
                Location eLoc = center.clone().add(5, 4.5, 0);
                Location wLoc = center.clone().add(-5, 4.5, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc, 2, 0.2, 0.2, 0.2, 0.01);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, wLoc, 2, 0.2, 0.2, 0.2, 0.01);
            }
            if (ticksAlive % 5 == 0) {
                Location eLoc = center.clone().add(5, 3.5, 0);
                Location wLoc = center.clone().add(-5, 3.5, 0);
                center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, eLoc, 1, 0.2, 0.1, 0.2, 0);
                center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, wLoc, 1, 0.2, 0.1, 0.2, 0);
            }

            // Exit animation: compress Y scale in last 60 ticks with fire burst
            int remaining = config.getDurationTicks() - ticksAlive;
            if (remaining <= 60 && remaining > 0) {
                float yScale = 1.6f * (remaining / 60.0f);
                if (eastSkull != null) eastSkull.scale(1.6f, yScale, 1.6f);
                if (westSkull != null) westSkull.scale(1.6f, yScale, 1.6f);

                // Increased soul fire during compression
                if (ticksAlive % 1 == 0) {
                    Location eLoc = center.clone().add(5, 4, 0);
                    Location wLoc = center.clone().add(-5, 4, 0);
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc, 3, 0.3, 0.3, 0.3, 0.03);
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, wLoc, 3, 0.3, 0.3, 0.3, 0.03);
                }
            }

            // Exit sound
            if (remaining == 1) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PairedSkullSentinels(plugin); }
    }

    // ================================================================
    // #28 -- SKULL STORM CANOPY
    // Continuous spawn of small nether wart skulls throughout upper
    // arena volume Y+12 to Y+35. 3 new every 20 ticks, 200-tick lifespan.
    // Slow downward drift + random horizontal wander.
    // Steady state: ~30 skulls in air. Phase 4 atmospheric saturation.
    // ================================================================
    public static class SkullStormCanopy extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> activeStormSkulls = new ArrayList<>();
        private final List<Integer> spawnTicks = new ArrayList<>();
        private final java.util.Random stormRng = new java.util.Random();

        public SkullStormCanopy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_skull_storm_canopy", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(4000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            // No initial spawn -- storm builds up via onTick
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spawn 3 new skulls every 20 ticks
            if (ticksAlive % 20 == 0) {
                for (int s = 0; s < 3; s++) {
                    double angle = stormRng.nextDouble() * 2.0 * Math.PI;
                    double radius = stormRng.nextDouble() * 30.0;
                    double spawnY = 12.0 + stormRng.nextDouble() * 23.0;
                    double sx = Math.cos(angle) * radius;
                    double sz = Math.sin(angle) * radius;

                    Location spawnLoc = center.clone().add(sx, spawnY, sz);
                    BlockDisplayHandle skull = displayBuilder.spawnBlock(spawnLoc, Material.NETHER_WART_BLOCK);
                    skull.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 50);
                    activeStormSkulls.add(skull);
                    spawnedEntities.add(skull.entity());
                    spawnTicks.add(ticksAlive);
                }
            }

            // Update existing skulls: drift downward + random horizontal wander + tumble
            List<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < activeStormSkulls.size(); i++) {
                int age = ticksAlive - spawnTicks.get(i);

                // Despawn after 200 ticks
                if (age >= 200) {
                    BlockDisplayHandle skull = activeStormSkulls.get(i);
                    skull.entity().remove();
                    toRemove.add(i);
                    continue;
                }

                BlockDisplayHandle skull = activeStormSkulls.get(i);
                Location current = skull.entity().getLocation();

                // Drift downward at 0.05 blocks/tick + slight random horizontal
                double newY = current.getY() - 0.05;
                double xWander = (stormRng.nextDouble() - 0.5) * 0.02;
                double zWander = (stormRng.nextDouble() - 0.5) * 0.02;
                Location newLoc = current.clone().add(xWander, -0.05, zWander);
                skull.entity().teleport(newLoc);

                // Slow tumble
                float tumble = (float) Math.toRadians(age * 2.0);
                skull.rotate(tumble, 0.5f, 1.0f, 0.3f);

                // SOUL_FIRE_FLAME + dark crimson dust
                if (age % 5 == 0) {
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, newLoc, 1, 0.1, 0.05, 0.1, 0.005);
                }
                if (age % 10 == 0) {
                    DisplayBuilder.dustParticles(newLoc, 1, 0.15, 80, 0, 0, 1.0f);
                }
            }

            // Clean up despawned skulls (reverse order to preserve indices)
            for (int i = toRemove.size() - 1; i >= 0; i--) {
                int idx = toRemove.get(i);
                activeStormSkulls.remove(idx);
                spawnTicks.remove(idx);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkullStormCanopy(plugin); }
    }
}
