package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.blockdisplay;

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
 * Phase 4D Block Display -- GROUP 3: AMETHYST CRYSTAL ARRAYS (#19-28)
 * 10 attacks featuring massive crystal formations erupting from the island
 * surface -- the Devourer of Gods' crystalline residue, amplified and
 * redirected by the Void Emperor.
 *
 * Structures:
 *  #19 PrimarySpireClusterAlpha  -- NW 5-spire cluster, Y+0 to Y+18
 *  #20 PrimarySpireClusterBeta   -- NE 4-spire cluster with calcite needle
 *  #21 SecondaryRidgeNorth       -- North wall linear ridge
 *  #22 SecondaryRidgeSouth       -- South wall ridge with purpur accent
 *  #23 CrystalGeodeVentSW        -- SW radial burst geode
 *  #24 CrystalGeodeVentSE        -- SE mixed amethyst/calcite geode
 *  #25 MegaSpireVoidFang         -- East mega-spire, 24 blocks tall
 *  #26 MegaSpireSanctumTooth     -- West mega-spire with gold crown
 *  #27 CrystalBridgeArch         -- NE-to-north arch bridge
 *  #28 SunkenCrystalPerimeter    -- 16 perimeter crystal accents
 *
 * Rules applied:
 * - NO status effects
 * - Void Emperor palette: magenta(180,0,200), cyan(0,200,255), purple(128,0,255)
 * - AxisAngle4f ONLY, static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Damage HP 4.0-12.0
 */
public final class AmethystCrystalArrays {

    private AmethystCrystalArrays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PrimarySpireClusterAlpha(plugin));
        registry.register(new PrimarySpireClusterBeta(plugin));
        registry.register(new SecondaryRidgeNorth(plugin));
        registry.register(new SecondaryRidgeSouth(plugin));
        registry.register(new CrystalGeodeVentSW(plugin));
        registry.register(new CrystalGeodeVentSE(plugin));
        registry.register(new MegaSpireVoidFang(plugin));
        registry.register(new MegaSpireSanctumTooth(plugin));
        registry.register(new CrystalBridgeArch(plugin));
        registry.register(new SunkenCrystalPerimeter(plugin));
    }

    // ================================================================
    // #19 -- PRIMARY SPIRE CLUSTER ALPHA (Northwest)
    // 5 amethyst spires erupting from a shared budding amethyst base.
    // Non-uniform heights and tilts for natural irregular cluster.
    // ================================================================
    public static class PrimarySpireClusterAlpha extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spires = new ArrayList<>();
        // Random periods for each spire oscillation (6-9 seconds = 120-180 ticks)
        private final int[] spirePeriods = {130, 155, 170, 140, 160};

        public PrimarySpireClusterAlpha(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("primary_spire_alpha", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location clusterCenter = center.clone().add(-17, 0, -17);

            // Oval base: 6x6 area of budding amethyst
            for (int x = -3; x <= 2; x++) {
                for (int z = -3; z <= 2; z++) {
                    if (Math.random() < 0.6) {
                        Location baseLoc = clusterCenter.clone().add(x, 0, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(baseLoc, Material.AMETHYST_BLOCK);
                        float randRot = (float) (Math.random() * Math.PI * 2);
                        h.scale(1.0f, 0.5f, 1.0f).rotate(randRot, 0, 1, 0).glow(128, 0, 255);
                        baseBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // 5 spires with varying heights, scales, and tilts
            float[][] spireData = {
                    // {offsetX, offsetZ, scaleX, scaleY, scaleZ, tiltDeg, tiltDirX, tiltDirZ}
                    {0, 0, 1.5f, 9.0f, 1.5f, 0, 0, 1},          // Center, no tilt
                    {0, 2, 1.0f, 7.0f, 1.0f, 8, 0, 1},           // North offset, tilt 8 deg N
                    {0, -2, 0.8f, 6.0f, 0.8f, 12, 0, -1},        // South offset, tilt 12 deg S
                    {2, 0, 0.9f, 8.0f, 0.9f, 5, 1, 0},           // East offset, tilt 5 deg E
                    {-2, 0, 1.1f, 5.0f, 1.1f, 15, -1, 0}         // West offset, tilt 15 deg W
            };

            for (float[] data : spireData) {
                Location spireLoc = clusterCenter.clone().add(data[0], 0, data[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(spireLoc, Material.AMETHYST_BLOCK);
                float tiltRad = (float) Math.toRadians(data[5]);
                h.scale(data[2], data[3], data[4])
                        .rotate(tiltRad, data[6], 0, data[7])
                        .glow(128, 0, 255).interpolation(5, 0);
                spires.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(clusterCenter, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location clusterCenter = center.clone().add(-17, 0, -17);

            // Each spire tip oscillates in a small ellipse with individual period
            // (visual effect only - we don't move the actual entity, just hint via particles)

            // DUST particles from each spire tip
            if (ticksAlive % 3 == 0) {
                float[][] heights = {{0, 9}, {0, 7}, {0, 6}, {2, 8}, {-2, 5}};
                for (int i = 0; i < spires.size() && i < heights.length; i++) {
                    Location tipLoc = clusterCenter.clone().add(heights[i][0], heights[i][1], 0);
                    DisplayBuilder.dustParticles(tipLoc, 4, 0.5, 155, 89, 182, 1.5f);
                }
            }

            // SPELL_WITCH from central spire
            if (ticksAlive % 5 == 0) {
                Location centralTip = clusterCenter.clone().add(0, 9, 0);
                center.getWorld().spawnParticle(Particle.WITCH, centralTip, 2, 0.3, 0.3, 0.3, 0.01);
            }

            // Cracking sound every 400 ticks (20 sec)
            if (ticksAlive % 400 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(clusterCenter, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrimarySpireClusterAlpha(plugin); }
    }

    // ================================================================
    // #20 -- PRIMARY SPIRE CLUSTER BETA (Northeast)
    // 4-spire cluster with one distinct calcite white needle that
    // tilts 20 deg toward arena center and breathes in height.
    // ================================================================
    public static class PrimarySpireClusterBeta extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spires = new ArrayList<>();
        private BlockDisplayHandle calciteSpire;

        public PrimarySpireClusterBeta(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("primary_spire_beta", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location clusterCenter = center.clone().add(17, 0, -17);

            // Base amethyst
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.random() < 0.55) {
                        Location baseLoc = clusterCenter.clone().add(x, 0, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(baseLoc, Material.AMETHYST_BLOCK);
                        float randRot = (float) (Math.random() * Math.PI * 2);
                        h.scale(1.0f, 0.5f, 1.0f).rotate(randRot, 0, 1, 0).glow(128, 0, 255);
                        baseBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // 3 amethyst spires
            float[][] spireData = {
                    {0, 0, 1.3f, 8.0f, 1.3f, 0, 0, 1},
                    {0, 2, 0.9f, 6.0f, 0.9f, 10, 0, 1},
                    {-2, 0, 0.8f, 5.0f, 0.8f, 8, -1, 0}
            };
            for (float[] data : spireData) {
                Location spireLoc = clusterCenter.clone().add(data[0], 0, data[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(spireLoc, Material.AMETHYST_BLOCK);
                float tiltRad = (float) Math.toRadians(data[5]);
                h.scale(data[2], data[3], data[4])
                        .rotate(tiltRad, data[6], 0, data[7])
                        .glow(128, 0, 255).interpolation(5, 0);
                spires.add(h);
                spawnedEntities.add(h.entity());
            }

            // Calcite needle: tall, white, tilted 20 deg toward arena center
            Location calciteLoc = clusterCenter.clone().add(1, 0, -1);
            calciteSpire = displayBuilder.spawnBlock(calciteLoc, Material.CALCITE);
            float tilt = (float) Math.toRadians(20);
            calciteSpire.scale(0.6f, 11.0f, 0.6f)
                    .rotate(tilt, -0.707f, 0, 0.707f) // tilt toward arena center (SW direction)
                    .glow(0, 200, 255).interpolation(8, 0);
            spawnedEntities.add(calciteSpire.entity());

            DisplayBuilder.playSound(clusterCenter, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location clusterCenter = center.clone().add(17, 0, -17);

            // Calcite needle height breathing: +/-0.5 on 4-sec (80-tick) cycle
            if (calciteSpire != null) {
                float yScale = 11.0f + 0.5f * (float) Math.sin(ticksAlive * Math.PI / 40);
                float tilt = (float) Math.toRadians(20);
                calciteSpire.scale(0.6f, yScale, 0.6f);
                calciteSpire.interpolation(8, 0);
            }

            // END_ROD particles from calcite tip
            if (ticksAlive % 2 == 0) {
                Location tipLoc = clusterCenter.clone().add(1, 11, -1);
                center.getWorld().spawnParticle(Particle.END_ROD, tipLoc, 5, 0.2, 0.2, 0.2, 0.01);
            }

            // Amethyst dust from spire tips
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(clusterCenter.clone().add(0, 8, 0), 4, 0.8,
                        155, 89, 182, 1.5f);
            }

            // Cracking sound
            if (ticksAlive % 400 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(clusterCenter, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrimarySpireClusterBeta(plugin); }
    }

    // ================================================================
    // #21 -- SECONDARY CRYSTAL RIDGE (North Wall)
    // Linear ridge of 11 amethyst blocks running east-west along
    // the north arena boundary. Static with particle emission.
    // ================================================================
    public static class SecondaryRidgeNorth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ridgeBlocks = new ArrayList<>();

        public SecondaryRidgeNorth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_ridge_north", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ridge along north edge Z-26, X from -8 to +8
            for (int i = 0; i < 11; i++) {
                double xPos = -8.0 + (16.0 / 10) * i;
                Location loc = center.clone().add(xPos, 0, -26);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                // Randomized dimensions for irregular profile
                float scaleX = 0.6f + (float) (Math.random() * 0.6);
                float scaleY = 2.0f + (float) (Math.random() * 3.0);
                float scaleZ = 0.6f + (float) (Math.random() * 0.6);
                float yRot = (float) Math.toRadians(Math.random() * 30);
                float pitch = (float) Math.toRadians(-5 + Math.random() * 10);
                h.scale(scaleX, scaleY, scaleZ).rotate(yRot, 0, 1, 0)
                        .glow(128, 0, 255);
                ridgeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fill gaps with budding amethyst at ground level
            for (int i = 0; i < 6; i++) {
                double xPos = -6.0 + (12.0 / 5) * i;
                Location loc = center.clone().add(xPos, 0, -26);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.3f, 0.8f).glow(128, 0, 255);
                ridgeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Continuous amethyst particle strip along ridge top
            if (ticksAlive % 2 == 0) {
                double xPos = -8.0 + (Math.random() * 16.0);
                Location particleLoc = center.clone().add(xPos, 4, -26);
                DisplayBuilder.dustParticles(particleLoc, 2, 0.3, 155, 89, 182, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SecondaryRidgeNorth(plugin); }
    }

    // ================================================================
    // #22 -- SECONDARY CRYSTAL RIDGE (South Wall)
    // Mirror of North ridge at Z+26 with one purpur pillar accent
    // at center that pulses height.
    // ================================================================
    public static class SecondaryRidgeSouth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ridgeBlocks = new ArrayList<>();
        private BlockDisplayHandle purpurAccent;

        public SecondaryRidgeSouth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_ridge_south", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 11; i++) {
                double xPos = -8.0 + (16.0 / 10) * i;
                Location loc = center.clone().add(xPos, 0, 26);

                // Central block is purpur pillar accent (index 5)
                if (i == 5) {
                    purpurAccent = displayBuilder.spawnBlock(loc, Material.PURPUR_PILLAR);
                    purpurAccent.scale(1.8f, 6.0f, 1.8f).glow(180, 0, 200).interpolation(8, 0);
                    spawnedEntities.add(purpurAccent.entity());
                } else {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                    float scaleX = 0.6f + (float) (Math.random() * 0.6);
                    float scaleY = 2.0f + (float) (Math.random() * 3.0);
                    float scaleZ = 0.6f + (float) (Math.random() * 0.6);
                    float yRot = (float) Math.toRadians(Math.random() * 30);
                    h.scale(scaleX, scaleY, scaleZ).rotate(yRot, 0, 1, 0).glow(128, 0, 255);
                    ridgeBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Fill gaps
            for (int i = 0; i < 6; i++) {
                double xPos = -6.0 + (12.0 / 5) * i;
                Location loc = center.clone().add(xPos, 0, 26);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.3f, 0.8f).glow(128, 0, 255);
                ridgeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Purpur pillar height pulse: +/-0.3 on 5-sec (100-tick) cycle
            if (purpurAccent != null) {
                float yScale = 6.0f + 0.3f * (float) Math.sin(ticksAlive * Math.PI / 50);
                purpurAccent.scale(1.8f, yScale, 1.8f);
                purpurAccent.interpolation(8, 0);
            }

            // Particle strip
            if (ticksAlive % 2 == 0) {
                double xPos = -8.0 + (Math.random() * 16.0);
                Location particleLoc = center.clone().add(xPos, 4, 26);
                DisplayBuilder.dustParticles(particleLoc, 2, 0.3, 155, 89, 182, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SecondaryRidgeSouth(plugin); }
    }

    // ================================================================
    // #23 -- CRYSTAL GEODE VENT (Southwest, Ground Level)
    // 12 amethyst arms in a radial starburst pattern erupting outward
    // from a budding amethyst cap. Rapid pulsing breath.
    // ================================================================
    public static class CrystalGeodeVentSW extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crystalArms = new ArrayList<>();
        private BlockDisplayHandle capPiece;
        private final float[] armLengths = new float[12];

        public CrystalGeodeVentSW(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_geode_vent_sw", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ventCenter = center.clone().add(-12, 0, 12);

            // Central budding amethyst cap
            capPiece = displayBuilder.spawnBlock(ventCenter, Material.AMETHYST_BLOCK);
            capPiece.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(capPiece.entity());

            // 12 radial arms
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                float length = 1.5f + (float) (Math.random() * 2.0); // 1.5-3.5
                armLengths[i] = length;
                float tiltDeg = 15 + (float) (Math.random() * 20); // 15-35 deg above horizontal
                float tiltRad = (float) Math.toRadians(tiltDeg);

                Location armLoc = ventCenter.clone().add(
                        Math.cos(angle) * 0.5, 0.3, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(armLoc, Material.AMETHYST_BLOCK);
                // Orient horizontally outward at the arm angle, tilted up
                h.scale(0.4f, 0.4f, length)
                        .rotate((float) angle, 0, 1, 0)
                        .glow(128, 0, 255).interpolation(3, 0);
                crystalArms.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(ventCenter, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.3f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location ventCenter = center.clone().add(-12, 0, 12);

            // All arms pulse length +/-0.2 on 1.5-sec (30-tick) fast sin cycle
            float pulse = (float) Math.sin(ticksAlive * Math.PI / 15);
            for (int i = 0; i < crystalArms.size(); i++) {
                float newLength = armLengths[i] + 0.2f * pulse;
                crystalArms.get(i).scale(0.4f, 0.4f, newLength);
                crystalArms.get(i).interpolation(3, 0);
            }

            // Amethyst particles from cap
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(ventCenter.clone().add(0, 1, 0), 10, 0.5,
                        155, 89, 182, 1.2f);
            }

            // Pulse sound every 60 ticks (3 sec)
            if (ticksAlive % 60 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(ventCenter, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.3f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalGeodeVentSW(plugin); }
    }

    // ================================================================
    // #24 -- CRYSTAL GEODE VENT (Southeast, Ground Level)
    // Mirror of SW with mixed amethyst/calcite arms. Opposite phase
    // pulse from SW vent.
    // ================================================================
    public static class CrystalGeodeVentSE extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crystalArms = new ArrayList<>();
        private BlockDisplayHandle capPiece;
        private final float[] armLengths = new float[12];

        public CrystalGeodeVentSE(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_geode_vent_se", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ventCenter = center.clone().add(12, 0, 12);

            capPiece = displayBuilder.spawnBlock(ventCenter, Material.AMETHYST_BLOCK);
            capPiece.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(capPiece.entity());

            // 12 arms: 4 are calcite (indices 1, 4, 7, 10), rest are amethyst
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                boolean isCalcite = (i == 1 || i == 4 || i == 7 || i == 10);
                Material mat = isCalcite ? Material.CALCITE : Material.AMETHYST_BLOCK;
                float length = isCalcite ? (3.0f + (float) (Math.random() * 1.0)) :
                        (1.5f + (float) (Math.random() * 2.0));
                armLengths[i] = length;

                Location armLoc = ventCenter.clone().add(
                        Math.cos(angle) * 0.5, 0.3, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(armLoc, mat);
                int glowR = isCalcite ? 0 : 128;
                int glowG = isCalcite ? 200 : 0;
                int glowB = 255;
                h.scale(0.4f, 0.4f, length)
                        .rotate((float) angle, 0, 1, 0)
                        .glow(glowR, glowG, glowB).interpolation(3, 0);
                crystalArms.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(ventCenter, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.3f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location ventCenter = center.clone().add(12, 0, 12);

            // Opposite phase from SW vent (+ PI)
            float pulse = (float) Math.sin(ticksAlive * Math.PI / 15 + Math.PI);
            for (int i = 0; i < crystalArms.size(); i++) {
                float newLength = armLengths[i] + 0.2f * pulse;
                crystalArms.get(i).scale(0.4f, 0.4f, newLength);
                crystalArms.get(i).interpolation(3, 0);
            }

            // Mixed particles
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(ventCenter.clone().add(0, 1, 0), 6, 0.5,
                        155, 89, 182, 1.2f);
                center.getWorld().spawnParticle(Particle.END_ROD,
                        ventCenter.clone().add(0, 1.2, 0), 2, 0.3, 0.2, 0.3, 0.01);
            }

            // Pulse sound
            if (ticksAlive % 60 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(ventCenter, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.3f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalGeodeVentSE(plugin); }
    }

    // ================================================================
    // #25 -- MEGA SPIRE "THE VOID FANG" (Island Center-East)
    // Single massive amethyst spire 3.0x24.0x3.0, tilted 5 deg toward
    // arena center. Eruption animation, very slow rotation.
    // ================================================================
    public static class MegaSpireVoidFang extends BlockDisplayAttack {

        private BlockDisplayHandle megaSpire;
        private boolean eruptionComplete = false;

        public MegaSpireVoidFang(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mega_spire_void_fang", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location spireBase = center.clone().add(25, 0, 0);

            // Start at 0 height, will grow over 40 ticks
            megaSpire = displayBuilder.spawnBlock(spireBase, Material.AMETHYST_BLOCK);
            float tilt = (float) Math.toRadians(5);
            megaSpire.scale(3.0f, 0.1f, 3.0f).rotate(tilt, 0, 0, -1) // tilt west toward center
                    .glow(128, 0, 255).interpolation(40, 0);
            spawnedEntities.add(megaSpire.entity());

            // Trigger eruption animation on next tick
            DisplayBuilder.playSound(spireBase, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location spireBase = center.clone().add(25, 0, 0);

            if (!eruptionComplete && ticksAlive == 1) {
                // Eruption: grow to full height over 40 ticks
                megaSpire.scale(3.0f, 24.0f, 3.0f);
                megaSpire.interpolation(40, 0);
                eruptionComplete = true;
            }

            // Post-eruption: very slow Y rotation at 0.1 deg/tick
            if (eruptionComplete && megaSpire != null) {
                float rotAngle = (float) Math.toRadians(ticksAlive * 0.1);
                float tilt = (float) Math.toRadians(5);
                // Combined tilt + rotation
                megaSpire.rotate(rotAngle + tilt, 0, 1, 0);
            }

            // DRAGON_BREATH cascade from tip (Y+24)
            if (eruptionComplete && ticksAlive % 2 == 0) {
                Location tip = spireBase.clone().add(0, 24, 0);
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, tip, 10,
                        0.5, 2.0, 0.5, 0);
            }

            // Amethyst ring at midpoint Y+12
            if (eruptionComplete && ticksAlive % 2 == 0) {
                Location mid = spireBase.clone().add(0, 12, 0);
                DisplayBuilder.dustParticles(mid, 8, 1.5, 155, 89, 182, 1.5f);
            }

            // Ambient dragon sound every 600 ticks (30 sec)
            if (ticksAlive % 600 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(spireBase.clone().add(0, 24, 0),
                        Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.2f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MegaSpireVoidFang(plugin); }
    }

    // ================================================================
    // #26 -- MEGA SPIRE "THE SANCTUM TOOTH" (Island Center-West)
    // Polished blackstone mega-spire with gold crown block at tip.
    // Static post-eruption, gold crown rotates.
    // ================================================================
    public static class MegaSpireSanctumTooth extends BlockDisplayAttack {

        private BlockDisplayHandle megaSpire;
        private BlockDisplayHandle goldCrown;
        private boolean eruptionComplete = false;

        public MegaSpireSanctumTooth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mega_spire_sanctum_tooth", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location spireBase = center.clone().add(-25, 0, 0);

            megaSpire = displayBuilder.spawnBlock(spireBase, Material.POLISHED_BLACKSTONE);
            float tilt = (float) Math.toRadians(7);
            megaSpire.scale(2.5f, 0.1f, 2.5f).rotate(tilt, 0, 0, 1) // tilt east toward center
                    .glow(180, 0, 200).interpolation(40, 0);
            spawnedEntities.add(megaSpire.entity());

            // Gold crown at tip (initially at ground, will teleport after eruption)
            goldCrown = displayBuilder.spawnBlock(spireBase.clone().add(0, 1, 0), Material.GOLD_BLOCK);
            goldCrown.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200).interpolation(3, 0);
            spawnedEntities.add(goldCrown.entity());

            DisplayBuilder.playSound(spireBase, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location spireBase = center.clone().add(-25, 0, 0);

            if (!eruptionComplete && ticksAlive == 1) {
                megaSpire.scale(2.5f, 20.0f, 2.5f);
                megaSpire.interpolation(40, 0);
                eruptionComplete = true;
            }

            // Move gold crown to tip after eruption settles
            if (eruptionComplete && goldCrown != null) {
                Location crownLoc = spireBase.clone().add(0, 20, 0);
                goldCrown.entity().teleport(crownLoc);

                // Gold crown continuous Y rotation at 2.0 deg/tick
                float rotAngle = (float) Math.toRadians(ticksAlive * 2.0);
                goldCrown.rotate(rotAngle, 0, 1, 0);
                goldCrown.interpolation(3, 0);
            }

            // TOTEM particles from gold crown
            if (eruptionComplete && ticksAlive % 2 == 0) {
                Location crownPos = spireBase.clone().add(0, 20.5, 0);
                center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, crownPos, 8,
                        1.0, 0.5, 1.0, 0.01);
            }

            // Blackstone sound every 500 ticks (25 sec)
            if (ticksAlive % 500 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(spireBase.clone().add(0, 20, 0),
                        Sound.BLOCK_GILDED_BLACKSTONE_HIT, 0.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MegaSpireSanctumTooth(plugin); }
    }

    // ================================================================
    // #27 -- CRYSTAL BRIDGE ARCH (Northeast to North)
    // 14 amethyst blocks in an arc from NE cluster to north ridge.
    // Wave scale-pulse propagates along the arch.
    // ================================================================
    public static class CrystalBridgeArch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private static final int ARCH_COUNT = 14;

        public CrystalBridgeArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_bridge_arch", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Arc from NE cluster (X+14, Z-14) to north ridge (X+0, Z-22)
            // Apex at X+7, Z-18, Y+14
            Location startPos = center.clone().add(14, 8, -14);
            Location endPos = center.clone().add(0, 8, -22);
            double apexY = 14.0;

            for (int i = 0; i < ARCH_COUNT; i++) {
                double t = (double) i / (ARCH_COUNT - 1);
                double x = startPos.getX() + (endPos.getX() - startPos.getX()) * t;
                double z = startPos.getZ() + (endPos.getZ() - startPos.getZ()) * t;
                // Parabolic arch: y = base + apex * sin(pi * t)
                double y = 8.0 + (apexY - 8.0) * Math.sin(Math.PI * t);

                Location loc = new Location(center.getWorld(), x, center.getY() + y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);

                // Tangent-aligned rotation
                double tangentAngle = Math.atan2(
                        endPos.getZ() - startPos.getZ(),
                        endPos.getX() - startPos.getX());
                h.scale(0.7f, 0.7f, 1.5f)
                        .rotate((float) tangentAngle, 0, 1, 0)
                        .glow(128, 0, 255).interpolation(5, 0);
                archBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wave scale-pulse: each block phase-shifted by 8 ticks (0.4 sec)
            for (int i = 0; i < archBlocks.size(); i++) {
                int phaseShift = i * 8;
                float scaleY = 0.7f + 0.15f * (float) Math.sin((ticksAlive - phaseShift) * Math.PI / 56);
                archBlocks.get(i).scale(0.7f, scaleY, 1.5f);
                archBlocks.get(i).interpolation(5, 0);
            }

            // Particles at apex
            if (ticksAlive % 4 == 0) {
                Location apex = center.clone().add(7, 14, -18);
                DisplayBuilder.dustParticles(apex, 5, 0.5, 155, 89, 182, 1.2f);
            }

            // Resonate sound every 160 ticks (8 sec)
            if (ticksAlive % 160 == 0 && ticksAlive > 0) {
                Location apex = center.clone().add(7, 14, -18);
                DisplayBuilder.playSound(apex, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.2f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalBridgeArch(plugin); }
    }

    // ================================================================
    // #28 -- SUNKEN CRYSTAL FORMATION (Arena Platform Perimeter)
    // 16 small 3-block crystal clusters at equal intervals around the
    // platform outer edge. Synchronized pulse, perimeter heartbeat.
    // ================================================================
    public static class SunkenCrystalPerimeter extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> centerPieces = new ArrayList<>();
        private final List<BlockDisplayHandle> sidePieces = new ArrayList<>();
        private static final int CLUSTER_COUNT = 16;
        private static final double PERIMETER_RADIUS = 22.0;

        public SunkenCrystalPerimeter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sunken_crystal_perimeter", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CLUSTER_COUNT; i++) {
                double angle = (2 * Math.PI * i) / CLUSTER_COUNT;
                double x = Math.cos(angle) * PERIMETER_RADIUS;
                double z = Math.sin(angle) * PERIMETER_RADIUS;
                Location clusterLoc = center.clone().add(x, -0.5, z);

                // Center piece: taller
                BlockDisplayHandle cp = displayBuilder.spawnBlock(clusterLoc, Material.AMETHYST_BLOCK);
                float outwardTilt = (float) Math.toRadians(10);
                cp.scale(0.4f, 1.2f, 0.4f)
                        .rotate(outwardTilt, (float) Math.cos(angle), 0, (float) Math.sin(angle))
                        .glow(128, 0, 255).interpolation(3, 0);
                centerPieces.add(cp);
                spawnedEntities.add(cp.entity());

                // Two smaller side pieces
                for (int s = -1; s <= 1; s += 2) {
                    double sideAngle = angle + s * 0.1;
                    Location sideLoc = clusterLoc.clone().add(
                            Math.cos(sideAngle) * 0.3, 0, Math.sin(sideAngle) * 0.3);
                    BlockDisplayHandle sp = displayBuilder.spawnBlock(sideLoc, Material.AMETHYST_BLOCK);
                    sp.scale(0.3f, 0.8f, 0.3f)
                            .rotate((float) Math.toRadians(12),
                                    (float) Math.cos(sideAngle), 0, (float) Math.sin(sideAngle))
                            .glow(128, 0, 255);
                    sidePieces.add(sp);
                    spawnedEntities.add(sp.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Synchronized pulse on all center pieces: +/-0.15 on 4-sec (80-tick) cycle
            float pulse = (float) Math.sin(ticksAlive * Math.PI / 40);
            for (BlockDisplayHandle cp : centerPieces) {
                float scale = 1.2f + 0.15f * pulse;
                cp.scale(0.4f, scale, 0.4f);
                cp.interpolation(3, 0);
            }

            // Amethyst particles from all clusters simultaneously
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle cp : centerPieces) {
                    DisplayBuilder.dustParticles(cp.entity().getLocation().clone().add(0, 1, 0),
                            2, 0.2, 155, 89, 182, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SunkenCrystalPerimeter(plugin); }
    }
}
