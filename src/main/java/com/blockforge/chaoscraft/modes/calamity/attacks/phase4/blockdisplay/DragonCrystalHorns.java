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
 * Phase 4D Block Display -- GROUP 7: DRAGON CRYSTAL HORNS (Structures #61-70)
 * Void Emperor (Boss 4) arena: twelve massive crystal spires around the island
 * perimeter, their particle columns, post-collapse debris, and convergence funnel.
 *
 * Palette: amethyst_block, purpur_block, purpur_pillar, amethyst_cluster, calcite.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 * NO LARGE_AMETHYST_CLUSTER (uses amethyst_cluster only).
 */
public final class DragonCrystalHorns {

    private DragonCrystalHorns() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrystalHornAlpha(plugin));
        registry.register(new CrystalHornBeta(plugin));
        registry.register(new CrystalHornGamma(plugin));
        registry.register(new CrystalHornDeltaKappa(plugin));
        registry.register(new CrystalHornLambda(plugin));
        registry.register(new CrystalHornMuNu(plugin));
        registry.register(new HornTipParticleColumns(plugin));
        registry.register(new ShatteredHornDebris(plugin));
        registry.register(new PerimeterVoidCracks(plugin));
        registry.register(new DragonBreathConvergenceFunnel(plugin));
    }

    // ================================================================
    // #61 -- CRYSTAL HORN ALPHA: Reference horn at 0 deg (north).
    //        Base purpur, mid amethyst shaft, tip amethyst_cluster.
    //        Y+0 to Y+22. 8 deg lean toward center. 18 BlockDisplays.
    // ================================================================
    public static class CrystalHornAlpha extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseSection = new ArrayList<>();
        private final List<BlockDisplayHandle> midSection = new ArrayList<>();
        private final List<BlockDisplayHandle> tipSection = new ArrayList<>();
        private final List<BlockDisplayHandle> accentClusters = new ArrayList<>();

        public CrystalHornAlpha(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_horn_alpha", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Northern perimeter, 0 deg
            Location hornBase = center.clone().add(0, 0, -30);

            // Base section Y+0 to Y+6
            BlockDisplayHandle base1 = displayBuilder.spawnBlock(hornBase, Material.PURPUR_BLOCK);
            base1.scale(4.0f, 4.0f, 4.0f).glow(128, 0, 255).interpolation(3, 0);
            baseSection.add(base1);
            spawnedEntities.add(base1.entity());

            BlockDisplayHandle base2 = displayBuilder.spawnBlock(hornBase.clone().add(0, 3, 0), Material.PURPUR_PILLAR);
            base2.scale(3.2f, 5.0f, 3.2f).glow(128, 0, 255).interpolation(3, 0);
            baseSection.add(base2);
            spawnedEntities.add(base2.entity());

            BlockDisplayHandle base3 = displayBuilder.spawnBlock(hornBase.clone().add(0, 6, 0), Material.AMETHYST_BLOCK);
            base3.scale(2.8f, 2.8f, 2.8f).glow(180, 0, 255).interpolation(3, 0);
            baseSection.add(base3);
            spawnedEntities.add(base3.entity());

            // Mid section Y+7 to Y+14
            BlockDisplayHandle mid1 = displayBuilder.spawnBlock(hornBase.clone().add(0, 9, 0), Material.AMETHYST_BLOCK);
            mid1.scale(2.4f, 4.0f, 2.4f).glow(180, 0, 255).interpolation(3, 0);
            midSection.add(mid1);
            spawnedEntities.add(mid1.entity());

            BlockDisplayHandle mid2 = displayBuilder.spawnBlock(hornBase.clone().add(0, 12, 0), Material.PURPUR_PILLAR);
            mid2.scale(1.6f, 3.5f, 1.6f).glow(128, 0, 255).interpolation(3, 0);
            midSection.add(mid2);
            spawnedEntities.add(mid2.entity());

            // Side buds (amethyst_cluster, not large_amethyst_bud)
            BlockDisplayHandle bud1 = displayBuilder.spawnBlock(hornBase.clone().add(0.8, 11, 0), Material.AMETHYST_CLUSTER);
            bud1.scale(2.0f, 2.0f, 2.0f).rotate((float) Math.toRadians(35), 0, 1, 0)
                .glow(180, 0, 255).interpolation(3, 0);
            midSection.add(bud1);
            spawnedEntities.add(bud1.entity());

            BlockDisplayHandle bud2 = displayBuilder.spawnBlock(hornBase.clone().add(0, 13, -0.7), Material.AMETHYST_CLUSTER);
            bud2.scale(1.6f, 1.6f, 1.6f).glow(180, 0, 255).interpolation(3, 0);
            midSection.add(bud2);
            spawnedEntities.add(bud2.entity());

            // Upper spire Y+15 to Y+22
            BlockDisplayHandle spire1 = displayBuilder.spawnBlock(hornBase.clone().add(0, 17, 0), Material.AMETHYST_BLOCK);
            spire1.scale(1.8f, 4.5f, 1.8f).glow(180, 0, 255).interpolation(3, 0);
            tipSection.add(spire1);
            spawnedEntities.add(spire1.entity());

            BlockDisplayHandle tipCluster = displayBuilder.spawnBlock(hornBase.clone().add(0, 20, 0), Material.AMETHYST_CLUSTER);
            tipCluster.scale(1.4f, 2.2f, 1.4f).glow(180, 0, 255).interpolation(3, 0);
            tipSection.add(tipCluster);
            spawnedEntities.add(tipCluster.entity());

            BlockDisplayHandle tipLean = displayBuilder.spawnBlock(hornBase.clone().add(0, 21.5, 0), Material.AMETHYST_CLUSTER);
            tipLean.scale(1.0f, 1.8f, 1.0f).rotate((float) Math.toRadians(-25), 1, 0, 0)
                   .glow(180, 0, 255).interpolation(3, 0);
            tipSection.add(tipLean);
            spawnedEntities.add(tipLean.entity());

            // 8 surface accent clusters distributed along height
            float[] accentHeights = {2, 4, 7, 10, 13, 15, 18, 20};
            float[] accentAngles = {15, -30, 25, -40, 35, -20, 45, -15};
            for (int i = 0; i < 8; i++) {
                double xOff = Math.sin(Math.toRadians(accentAngles[i])) * 1.2;
                double zOff = Math.cos(Math.toRadians(accentAngles[i])) * 1.2;
                float accScale = 0.6f + (float) (Math.random() * 0.6);
                BlockDisplayHandle accent = displayBuilder.spawnBlock(
                        hornBase.clone().add(xOff, accentHeights[i], zOff), Material.AMETHYST_CLUSTER);
                accent.scale(accScale, accScale, accScale).glow(180, 0, 255).interpolation(3, 0);
                accentClusters.add(accent);
                spawnedEntities.add(accent.entity());
            }

            DisplayBuilder.playSound(hornBase, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Tip scale oscillation: 1.4 + 0.18 * sin(tick * 0.09)
            float tipOsc = 1.4f + 0.18f * (float) Math.sin(ticksAlive * 0.09);
            for (BlockDisplayHandle h : tipSection) {
                h.scale(tipOsc, tipOsc * 1.5f, tipOsc);
                h.interpolation(3, 0);
            }

            // DRAGON_BREATH from tip
            if (ticksAlive % 4 == 0) {
                Location hornBase = center.clone().add(0, 0, -30);
                Location tipPos = hornBase.clone().add(0, 22, 0);
                w.spawnParticle(Particle.DRAGON_BREATH, tipPos, 2, 0.5, 0.5, 0.5, 0.04);
            }

            // Ambient sound every 110 ticks
            if (ticksAlive % 110 == 0) {
                Location hornBase = center.clone().add(0, 0, -30);
                DisplayBuilder.playSound(hornBase.clone().add(0, 10, 0),
                        Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalHornAlpha(plugin); }
    }

    // ================================================================
    // #62 -- CRYSTAL HORN BETA: 30 deg position, Y+19, lean 12 deg.
    //        Same architecture as Alpha but shorter, more aggressive lean.
    //        Extra buds + purpur slab foot.
    // ================================================================
    public static class CrystalHornBeta extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public CrystalHornBeta(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_horn_beta", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.toRadians(30);
            Location hornBase = center.clone().add(Math.sin(angle) * 30, 0, -Math.cos(angle) * 30);

            // Wide purpur foot slab at Y+1
            BlockDisplayHandle foot = displayBuilder.spawnBlock(hornBase.clone().add(0, 1, 0), Material.PURPUR_BLOCK);
            foot.scale(3.6f, 0.5f, 3.6f).glow(128, 0, 255).interpolation(3, 0);
            allBlocks.add(foot);
            spawnedEntities.add(foot.entity());

            // Base purpur root
            BlockDisplayHandle base = displayBuilder.spawnBlock(hornBase, Material.PURPUR_BLOCK);
            base.scale(3.5f, 3.5f, 3.5f).glow(128, 0, 255).interpolation(3, 0);
            allBlocks.add(base);
            spawnedEntities.add(base.entity());

            // Lower shaft
            BlockDisplayHandle shaft = displayBuilder.spawnBlock(hornBase.clone().add(0, 3, 0), Material.PURPUR_PILLAR);
            shaft.scale(2.8f, 4.0f, 2.8f).glow(128, 0, 255).interpolation(3, 0);
            allBlocks.add(shaft);
            spawnedEntities.add(shaft.entity());

            // Transition
            BlockDisplayHandle trans = displayBuilder.spawnBlock(hornBase.clone().add(0, 5.5, 0), Material.AMETHYST_BLOCK);
            trans.scale(2.4f, 2.4f, 2.4f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(trans);
            spawnedEntities.add(trans.entity());

            // Mid shaft
            BlockDisplayHandle midShaft = displayBuilder.spawnBlock(hornBase.clone().add(0, 8, 0), Material.AMETHYST_BLOCK);
            midShaft.scale(2.0f, 3.5f, 2.0f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(midShaft);
            spawnedEntities.add(midShaft.entity());

            // Extra outward-angling buds
            BlockDisplayHandle budA = displayBuilder.spawnBlock(hornBase.clone().add(1.0, 8, 0), Material.AMETHYST_CLUSTER);
            budA.scale(1.4f, 1.4f, 1.4f).rotate((float) Math.toRadians(25), 0, 0, 1)
                .glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(budA);
            spawnedEntities.add(budA.entity());

            BlockDisplayHandle budB = displayBuilder.spawnBlock(hornBase.clone().add(-1.0, 10, 0), Material.AMETHYST_CLUSTER);
            budB.scale(1.4f, 1.4f, 1.4f).rotate((float) Math.toRadians(-25), 0, 0, 1)
                .glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(budB);
            spawnedEntities.add(budB.entity());

            // Upper spire
            BlockDisplayHandle spire = displayBuilder.spawnBlock(hornBase.clone().add(0, 14, 0), Material.AMETHYST_BLOCK);
            spire.scale(1.5f, 4.0f, 1.5f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(spire);
            spawnedEntities.add(spire.entity());

            // Tip cluster
            BlockDisplayHandle tip = displayBuilder.spawnBlock(hornBase.clone().add(0, 18, 0), Material.AMETHYST_CLUSTER);
            tip.scale(1.2f, 2.0f, 1.2f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(tip);
            spawnedEntities.add(tip.entity());

            DisplayBuilder.playSound(hornBase, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Tip oscillation
            if (!allBlocks.isEmpty()) {
                BlockDisplayHandle tip = allBlocks.get(allBlocks.size() - 1);
                float osc = 1.2f + 0.18f * (float) Math.sin(ticksAlive * 0.09);
                tip.scale(osc, osc * 1.7f, osc);
                tip.interpolation(3, 0);
            }

            // Dragon breath from tip at slightly higher rate
            if (ticksAlive % 3 == 0) {
                double angle = Math.toRadians(30);
                Location tipPos = center.clone().add(Math.sin(angle) * 30, 19, -Math.cos(angle) * 30);
                w.spawnParticle(Particle.DRAGON_BREATH, tipPos, 3, 0.5, 0.5, 0.5, 0.04);
            }

            if (ticksAlive % 110 == 0) {
                double angle = Math.toRadians(30);
                Location hornLoc = center.clone().add(Math.sin(angle) * 30, 10, -Math.cos(angle) * 30);
                DisplayBuilder.playSound(hornLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalHornBeta(plugin); }
    }

    // ================================================================
    // #63 -- CRYSTAL HORN GAMMA: 60 deg, Y+25 (tallest horn).
    //        Nearly vertical (5 deg lean). Extra buds at Y+17, Y+19.
    //        Phase 2 brightness increase on tips.
    // ================================================================
    public static class CrystalHornGamma extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tipBlocks = new ArrayList<>();

        public CrystalHornGamma(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_horn_gamma", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.toRadians(60);
            Location hornBase = center.clone().add(Math.sin(angle) * 30, 0, -Math.cos(angle) * 30);

            // Base section
            BlockDisplayHandle base = displayBuilder.spawnBlock(hornBase, Material.PURPUR_BLOCK);
            base.scale(4.0f, 4.0f, 4.0f).glow(128, 0, 255).interpolation(3, 0);
            allBlocks.add(base);
            spawnedEntities.add(base.entity());

            BlockDisplayHandle lowerShaft = displayBuilder.spawnBlock(hornBase.clone().add(0, 3, 0), Material.PURPUR_PILLAR);
            lowerShaft.scale(3.2f, 5.0f, 3.2f).glow(128, 0, 255).interpolation(3, 0);
            allBlocks.add(lowerShaft);
            spawnedEntities.add(lowerShaft.entity());

            BlockDisplayHandle trans = displayBuilder.spawnBlock(hornBase.clone().add(0, 7, 0), Material.AMETHYST_BLOCK);
            trans.scale(2.8f, 2.8f, 2.8f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(trans);
            spawnedEntities.add(trans.entity());

            // Tall mid shaft (Y-scale 6.0)
            BlockDisplayHandle midShaft = displayBuilder.spawnBlock(hornBase.clone().add(0, 11, 0), Material.AMETHYST_BLOCK);
            midShaft.scale(2.4f, 6.0f, 2.4f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(midShaft);
            spawnedEntities.add(midShaft.entity());

            BlockDisplayHandle midPillar = displayBuilder.spawnBlock(hornBase.clone().add(0, 14, 0), Material.PURPUR_PILLAR);
            midPillar.scale(1.6f, 3.5f, 1.6f).glow(128, 0, 255).interpolation(3, 0);
            allBlocks.add(midPillar);
            spawnedEntities.add(midPillar.entity());

            // Extra buds at Y+17 and Y+19
            BlockDisplayHandle bud17 = displayBuilder.spawnBlock(hornBase.clone().add(0.8, 17, 0), Material.AMETHYST_CLUSTER);
            bud17.scale(1.8f, 1.8f, 1.8f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(bud17);
            spawnedEntities.add(bud17.entity());

            BlockDisplayHandle bud19 = displayBuilder.spawnBlock(hornBase.clone().add(-0.7, 19, 0), Material.AMETHYST_CLUSTER);
            bud19.scale(1.6f, 1.6f, 1.6f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(bud19);
            spawnedEntities.add(bud19.entity());

            // Upper spire
            BlockDisplayHandle spire = displayBuilder.spawnBlock(hornBase.clone().add(0, 19, 0), Material.AMETHYST_BLOCK);
            spire.scale(1.8f, 5.0f, 1.8f).glow(180, 0, 255).interpolation(3, 0);
            allBlocks.add(spire);
            spawnedEntities.add(spire.entity());

            // Internal spine protruding above main tip
            BlockDisplayHandle spine = displayBuilder.spawnBlock(hornBase.clone().add(0, 22, 0), Material.PURPUR_PILLAR);
            spine.scale(0.8f, 3.5f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            allBlocks.add(spine);
            spawnedEntities.add(spine.entity());

            // Tip cluster
            BlockDisplayHandle tipCluster = displayBuilder.spawnBlock(hornBase.clone().add(0, 23, 0), Material.AMETHYST_CLUSTER);
            tipCluster.scale(1.4f, 2.2f, 1.4f).brightness(8, 8).glow(180, 0, 255).interpolation(3, 0);
            tipBlocks.add(tipCluster);
            allBlocks.add(tipCluster);
            spawnedEntities.add(tipCluster.entity());

            DisplayBuilder.playSound(hornBase, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.65f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Tip oscillation
            for (BlockDisplayHandle tip : tipBlocks) {
                float osc = 1.4f + 0.18f * (float) Math.sin(ticksAlive * 0.09);
                tip.scale(osc, osc * 1.6f, osc);
                tip.interpolation(3, 0);
            }

            // High rate dragon breath + totem from tallest horn tip
            if (ticksAlive % 3 == 0) {
                double angle = Math.toRadians(60);
                Location tipPos = center.clone().add(Math.sin(angle) * 30, 25, -Math.cos(angle) * 30);
                w.spawnParticle(Particle.DRAGON_BREATH, tipPos, 4, 0.5, 0.5, 0.5, 0.05);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, tipPos, 2, 0.5, 0.5, 0.5, 0);
            }

            if (ticksAlive % 110 == 0) {
                double angle = Math.toRadians(60);
                Location hornLoc = center.clone().add(Math.sin(angle) * 30, 12, -Math.cos(angle) * 30);
                DisplayBuilder.playSound(hornLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.65f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalHornGamma(plugin); }
    }

    // ================================================================
    // #64 -- CRYSTAL HORNS DELTA-KAPPA: 6 horns at 90-240 deg.
    //        Heights: 20, 18, 21, 23, 17, 22. Varying lean angles.
    //        Sequential collapse pitch: 0.7 to 0.95.
    // ================================================================
    public static class CrystalHornDeltaKappa extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> hornSets = new ArrayList<>();

        public CrystalHornDeltaKappa(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_horns_delta_kappa", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double[] angles = {90, 120, 150, 180, 210, 240};
            int[] heights = {20, 18, 21, 23, 17, 22};
            float[] leans = {10, 14, 7, 11, 16, 9};

            for (int h = 0; h < 6; h++) {
                List<BlockDisplayHandle> hornBlocks = new ArrayList<>();
                double angle = Math.toRadians(angles[h]);
                Location hornBase = center.clone().add(Math.sin(angle) * 30, 0, -Math.cos(angle) * 30);
                int maxHeight = heights[h];

                // Simplified horn following Alpha template, proportionally scaled
                float heightFactor = maxHeight / 22.0f;

                // Base
                BlockDisplayHandle base = displayBuilder.spawnBlock(hornBase, Material.PURPUR_BLOCK);
                base.scale(4.0f * heightFactor, 4.0f * heightFactor, 4.0f * heightFactor)
                    .glow(128, 0, 255).interpolation(3, 0);
                hornBlocks.add(base);
                spawnedEntities.add(base.entity());

                // Lower shaft
                BlockDisplayHandle lower = displayBuilder.spawnBlock(
                        hornBase.clone().add(0, 3 * heightFactor, 0), Material.PURPUR_PILLAR);
                lower.scale(3.0f * heightFactor, 5.0f * heightFactor, 3.0f * heightFactor)
                     .glow(128, 0, 255).interpolation(3, 0);
                hornBlocks.add(lower);
                spawnedEntities.add(lower.entity());

                // Amethyst transition
                BlockDisplayHandle trans = displayBuilder.spawnBlock(
                        hornBase.clone().add(0, 6 * heightFactor, 0), Material.AMETHYST_BLOCK);
                trans.scale(2.6f * heightFactor, 2.6f * heightFactor, 2.6f * heightFactor)
                     .glow(180, 0, 255).interpolation(3, 0);
                hornBlocks.add(trans);
                spawnedEntities.add(trans.entity());

                // Mid shaft
                BlockDisplayHandle mid = displayBuilder.spawnBlock(
                        hornBase.clone().add(0, 10 * heightFactor, 0), Material.AMETHYST_BLOCK);
                mid.scale(2.2f * heightFactor, 4.0f * heightFactor, 2.2f * heightFactor)
                   .glow(180, 0, 255).interpolation(3, 0);
                hornBlocks.add(mid);
                spawnedEntities.add(mid.entity());

                // Upper spire
                BlockDisplayHandle spire = displayBuilder.spawnBlock(
                        hornBase.clone().add(0, 16 * heightFactor, 0), Material.AMETHYST_BLOCK);
                spire.scale(1.6f * heightFactor, 4.5f * heightFactor, 1.6f * heightFactor)
                     .glow(180, 0, 255).interpolation(3, 0);
                hornBlocks.add(spire);
                spawnedEntities.add(spire.entity());

                // Tip cluster
                BlockDisplayHandle tip = displayBuilder.spawnBlock(
                        hornBase.clone().add(0, maxHeight - 2, 0), Material.AMETHYST_CLUSTER);
                tip.scale(1.2f, 2.0f, 1.2f).glow(180, 0, 255).interpolation(3, 0);
                hornBlocks.add(tip);
                spawnedEntities.add(tip.entity());

                hornSets.add(hornBlocks);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Tip oscillation for each horn set
            for (List<BlockDisplayHandle> hornBlocks : hornSets) {
                if (!hornBlocks.isEmpty()) {
                    BlockDisplayHandle tip = hornBlocks.get(hornBlocks.size() - 1);
                    float osc = 1.2f + 0.18f * (float) Math.sin(ticksAlive * 0.09);
                    tip.scale(osc, osc * 1.7f, osc);
                    tip.interpolation(3, 0);
                }
            }

            // Dragon breath from each horn tip
            if (ticksAlive % 4 == 0) {
                double[] angles = {90, 120, 150, 180, 210, 240};
                int[] heights = {20, 18, 21, 23, 17, 22};
                for (int h = 0; h < 6; h++) {
                    double angle = Math.toRadians(angles[h]);
                    Location tipPos = center.clone().add(
                            Math.sin(angle) * 30, heights[h], -Math.cos(angle) * 30);
                    int rate = heights[h] > 20 ? 4 : 2;
                    w.spawnParticle(Particle.DRAGON_BREATH, tipPos, rate, 0.5, 0.5, 0.5, 0.05);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalHornDeltaKappa(plugin); }
    }

    // ================================================================
    // #65 -- CRYSTAL HORN LAMBDA: 270 deg (due west), Y+20, lean 18 deg.
    //        Forked top -- main spire splits into two sub-spires at Y+17.
    // ================================================================
    public static class CrystalHornLambda extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mainBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> forkABlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> forkBBlocks = new ArrayList<>();

        public CrystalHornLambda(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_horn_lambda", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location hornBase = center.clone().add(-30, 0, 0); // 270 deg = due west

            // Base and shaft (same template)
            BlockDisplayHandle base = displayBuilder.spawnBlock(hornBase, Material.PURPUR_BLOCK);
            base.scale(4.0f, 4.0f, 4.0f).glow(128, 0, 255).interpolation(3, 0);
            mainBlocks.add(base);
            spawnedEntities.add(base.entity());

            BlockDisplayHandle shaft = displayBuilder.spawnBlock(hornBase.clone().add(0, 3, 0), Material.PURPUR_PILLAR);
            shaft.scale(3.2f, 5.0f, 3.2f).glow(128, 0, 255).interpolation(3, 0);
            mainBlocks.add(shaft);
            spawnedEntities.add(shaft.entity());

            BlockDisplayHandle trans = displayBuilder.spawnBlock(hornBase.clone().add(0, 6, 0), Material.AMETHYST_BLOCK);
            trans.scale(2.8f, 2.8f, 2.8f).glow(180, 0, 255).interpolation(3, 0);
            mainBlocks.add(trans);
            spawnedEntities.add(trans.entity());

            BlockDisplayHandle midShaft = displayBuilder.spawnBlock(hornBase.clone().add(0, 9, 0), Material.AMETHYST_BLOCK);
            midShaft.scale(2.4f, 4.0f, 2.4f).glow(180, 0, 255).interpolation(3, 0);
            mainBlocks.add(midShaft);
            spawnedEntities.add(midShaft.entity());

            BlockDisplayHandle midPillar = displayBuilder.spawnBlock(hornBase.clone().add(0, 12, 0), Material.PURPUR_PILLAR);
            midPillar.scale(1.6f, 3.5f, 1.6f).glow(128, 0, 255).interpolation(3, 0);
            mainBlocks.add(midPillar);
            spawnedEntities.add(midPillar.entity());

            // Main shaft terminates at Y+17
            BlockDisplayHandle terminatorShaft = displayBuilder.spawnBlock(hornBase.clone().add(0, 15, 0), Material.AMETHYST_BLOCK);
            terminatorShaft.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 255).interpolation(3, 0);
            mainBlocks.add(terminatorShaft);
            spawnedEntities.add(terminatorShaft.entity());

            // Fork A: leans 15 deg outward (away from arena)
            BlockDisplayHandle forkA = displayBuilder.spawnBlock(hornBase.clone().add(-0.8, 17, 0), Material.AMETHYST_BLOCK);
            forkA.scale(1.2f, 3.5f, 1.2f).rotate((float) Math.toRadians(15), 0, 0, 1)
                 .glow(180, 0, 255).interpolation(3, 0);
            forkABlocks.add(forkA);
            spawnedEntities.add(forkA.entity());

            BlockDisplayHandle forkATip = displayBuilder.spawnBlock(hornBase.clone().add(-1.6, 20, 0), Material.AMETHYST_CLUSTER);
            forkATip.scale(0.9f, 1.6f, 0.9f).glow(180, 0, 255).interpolation(3, 0);
            forkABlocks.add(forkATip);
            spawnedEntities.add(forkATip.entity());

            // Fork B: leans 10 deg inward (toward arena)
            BlockDisplayHandle forkB = displayBuilder.spawnBlock(hornBase.clone().add(0.6, 17, 0), Material.AMETHYST_BLOCK);
            forkB.scale(1.2f, 3.5f, 1.2f).rotate((float) Math.toRadians(-10), 0, 0, 1)
                 .glow(180, 0, 255).interpolation(3, 0);
            forkBBlocks.add(forkB);
            spawnedEntities.add(forkB.entity());

            BlockDisplayHandle forkBTip = displayBuilder.spawnBlock(hornBase.clone().add(1.2, 20, 0), Material.AMETHYST_CLUSTER);
            forkBTip.scale(0.9f, 1.6f, 0.9f).glow(180, 0, 255).interpolation(3, 0);
            forkBBlocks.add(forkBTip);
            spawnedEntities.add(forkBTip.entity());

            DisplayBuilder.playSound(hornBase, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.85f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Each fork tip oscillates independently
            if (!forkABlocks.isEmpty()) {
                BlockDisplayHandle tipA = forkABlocks.get(forkABlocks.size() - 1);
                float oscA = 0.9f + 0.15f * (float) Math.sin(ticksAlive * 0.09);
                tipA.scale(oscA, oscA * 1.8f, oscA);
                tipA.interpolation(3, 0);
            }
            if (!forkBBlocks.isEmpty()) {
                BlockDisplayHandle tipB = forkBBlocks.get(forkBBlocks.size() - 1);
                float oscB = 0.9f + 0.15f * (float) Math.sin(ticksAlive * 0.09 + 1.5);
                tipB.scale(oscB, oscB * 1.8f, oscB);
                tipB.interpolation(3, 0);
            }

            // Dragon breath from both fork tips
            if (ticksAlive % 4 == 0) {
                Location tipPosA = center.clone().add(-31.6, 20, 0);
                Location tipPosB = center.clone().add(-28.8, 20, 0);
                w.spawnParticle(Particle.DRAGON_BREATH, tipPosA, 2, 0.3, 0.3, 0.3, 0.04);
                w.spawnParticle(Particle.DRAGON_BREATH, tipPosB, 2, 0.3, 0.3, 0.3, 0.04);
            }

            if (ticksAlive % 110 == 0) {
                DisplayBuilder.playSound(center.clone().add(-30, 10, 0),
                        Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.85f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalHornLambda(plugin); }
    }

    // ================================================================
    // #66 -- CRYSTAL HORNS MU + NU: 300 + 330 deg, mirrored pair.
    //        Lean toward each other, forming a gateway arch.
    //        Collapse simultaneously (not sequentially).
    // ================================================================
    public static class CrystalHornMuNu extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> muBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> nuBlocks = new ArrayList<>();

        public CrystalHornMuNu(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_horn_mu_nu", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Mu at 300 deg, height Y+21
            double angleMu = Math.toRadians(300);
            Location muBase = center.clone().add(Math.sin(angleMu) * 30, 0, -Math.cos(angleMu) * 30);
            spawnHorn(muBase, 21, muBlocks);

            // Nu at 330 deg, height Y+18
            double angleNu = Math.toRadians(330);
            Location nuBase = center.clone().add(Math.sin(angleNu) * 30, 0, -Math.cos(angleNu) * 30);
            spawnHorn(nuBase, 18, nuBlocks);

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.75f);
        }

        private void spawnHorn(Location hornBase, int height, List<BlockDisplayHandle> blocks) {
            float factor = height / 22.0f;

            BlockDisplayHandle base = displayBuilder.spawnBlock(hornBase, Material.PURPUR_BLOCK);
            base.scale(4.0f * factor, 4.0f * factor, 4.0f * factor).glow(128, 0, 255).interpolation(3, 0);
            blocks.add(base);
            spawnedEntities.add(base.entity());

            BlockDisplayHandle shaft = displayBuilder.spawnBlock(hornBase.clone().add(0, 3 * factor, 0), Material.PURPUR_PILLAR);
            shaft.scale(3.0f * factor, 5.0f * factor, 3.0f * factor).glow(128, 0, 255).interpolation(3, 0);
            blocks.add(shaft);
            spawnedEntities.add(shaft.entity());

            BlockDisplayHandle trans = displayBuilder.spawnBlock(hornBase.clone().add(0, 6 * factor, 0), Material.AMETHYST_BLOCK);
            trans.scale(2.6f * factor, 2.6f * factor, 2.6f * factor).glow(180, 0, 255).interpolation(3, 0);
            blocks.add(trans);
            spawnedEntities.add(trans.entity());

            BlockDisplayHandle mid = displayBuilder.spawnBlock(hornBase.clone().add(0, 10 * factor, 0), Material.AMETHYST_BLOCK);
            mid.scale(2.2f * factor, 4.5f * factor, 2.2f * factor).glow(180, 0, 255).interpolation(3, 0);
            blocks.add(mid);
            spawnedEntities.add(mid.entity());

            BlockDisplayHandle spire = displayBuilder.spawnBlock(hornBase.clone().add(0, 16 * factor, 0), Material.AMETHYST_BLOCK);
            spire.scale(1.6f * factor, 4.5f * factor, 1.6f * factor).glow(180, 0, 255).interpolation(3, 0);
            blocks.add(spire);
            spawnedEntities.add(spire.entity());

            BlockDisplayHandle tip = displayBuilder.spawnBlock(hornBase.clone().add(0, height - 2, 0), Material.AMETHYST_CLUSTER);
            tip.scale(1.2f, 2.0f, 1.2f).glow(180, 0, 255).interpolation(3, 0);
            blocks.add(tip);
            spawnedEntities.add(tip.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Tip oscillation for both horns
            for (List<BlockDisplayHandle> blocks : List.of(muBlocks, nuBlocks)) {
                if (!blocks.isEmpty()) {
                    BlockDisplayHandle tip = blocks.get(blocks.size() - 1);
                    float osc = 1.2f + 0.18f * (float) Math.sin(ticksAlive * 0.09);
                    tip.scale(osc, osc * 1.7f, osc);
                    tip.interpolation(3, 0);
                }
            }

            // Dragon breath from both tips, streams toward each other
            if (ticksAlive % 3 == 0) {
                double angleMu = Math.toRadians(300);
                double angleNu = Math.toRadians(330);
                Location muTip = center.clone().add(Math.sin(angleMu) * 30, 19, -Math.cos(angleMu) * 30);
                Location nuTip = center.clone().add(Math.sin(angleNu) * 30, 16, -Math.cos(angleNu) * 30);
                w.spawnParticle(Particle.DRAGON_BREATH, muTip, 3, 0.5, 0.5, 0.5, 0.04);
                w.spawnParticle(Particle.DRAGON_BREATH, nuTip, 3, 0.5, 0.5, 0.5, 0.04);
            }

            if (ticksAlive % 110 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.12f, 0.75f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalHornMuNu(plugin); }
    }

    // ================================================================
    // #67 -- HORN TIP PARTICLE COLUMNS: One emitter per horn tip.
    //        DRAGON_BREATH 5/tick upward + SPELL_WITCH 2/tick.
    //        Rates ramp with phase.
    // ================================================================
    public static class HornTipParticleColumns extends BlockDisplayAttack {

        private static final double[] HORN_ANGLES = {0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330};
        private static final int[] HORN_HEIGHTS = {22, 19, 25, 20, 18, 21, 23, 17, 22, 20, 21, 18};

        public HornTipParticleColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("horn_tip_particle_columns", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only emitter system -- no blocks to spawn
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Emit from all 12 horn tips each tick
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(HORN_ANGLES[i]);
                double x = Math.sin(angle) * 30;
                double z = -Math.cos(angle) * 30;
                Location tipPos = center.clone().add(x, HORN_HEIGHTS[i] + 2, z);

                // DRAGON_BREATH upward column
                w.spawnParticle(Particle.DRAGON_BREATH, tipPos, 5, 0.5, 0.5, 0.5, 0.12);
                // SPELL_WITCH accent
                w.spawnParticle(Particle.WITCH, tipPos, 2, 0.3, 0.3, 0.3, 0.08);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HornTipParticleColumns(plugin); }
    }

    // ================================================================
    // #68 -- SHATTERED HORN DEBRIS: Per-horn ground debris spawning
    //        on collapse. 8 debris BlockDisplays per horn base.
    //        4 amethyst_block + 3 purpur_block + 1 amethyst_cluster.
    // ================================================================
    public static class ShatteredHornDebris extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> debrisBlocks = new ArrayList<>();

        public ShatteredHornDebris(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shattered_horn_debris", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn debris at a single representative horn base (0 deg north)
            Location hornBase = center.clone().add(0, 0, -30);

            // 4 amethyst_block debris
            for (int i = 0; i < 4; i++) {
                double dx = (Math.random() - 0.5) * 6.0;
                double dz = (Math.random() - 0.5) * 6.0;
                float scale = 0.3f + (float) (Math.random() * 0.3);
                BlockDisplayHandle debris = displayBuilder.spawnBlock(
                        hornBase.clone().add(dx, Math.random() * 0.5, dz), Material.AMETHYST_BLOCK);
                debris.scale(scale, scale, scale).glow(180, 0, 255).interpolation(3, 0);
                debrisBlocks.add(debris);
                spawnedEntities.add(debris.entity());
            }

            // 3 purpur_block debris
            for (int i = 0; i < 3; i++) {
                double dx = (Math.random() - 0.5) * 6.0;
                double dz = (Math.random() - 0.5) * 6.0;
                float scale = 0.2f + (float) (Math.random() * 0.3);
                BlockDisplayHandle debris = displayBuilder.spawnBlock(
                        hornBase.clone().add(dx, Math.random() * 0.5, dz), Material.PURPUR_BLOCK);
                debris.scale(scale, scale, scale).glow(128, 0, 255).interpolation(3, 0);
                debrisBlocks.add(debris);
                spawnedEntities.add(debris.entity());
            }

            // 1 amethyst_cluster bud at debris edge
            BlockDisplayHandle bud = displayBuilder.spawnBlock(
                    hornBase.clone().add(2.5, 0.3, 1.5), Material.AMETHYST_CLUSTER);
            float budScale = 0.7f + (float) (Math.random() * 0.3);
            bud.scale(budScale, budScale, budScale).glow(180, 0, 255).interpolation(3, 0);
            debrisBlocks.add(bud);
            spawnedEntities.add(bud.entity());

            DisplayBuilder.playSound(hornBase, Sound.BLOCK_AMETHYST_BLOCK_FALL, 0.4f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Residual CRIT particles that decay over 200 ticks
            if (ticksAlive < 200 && ticksAlive % 5 == 0) {
                Location hornBase = center.clone().add(0, 0.5, -30);
                int count = Math.max(1, (int) ((1.0 - ticksAlive / 200.0) * 4));
                w.spawnParticle(Particle.CRIT, hornBase, count, 1.5, 0.5, 1.5, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShatteredHornDebris(plugin); }
    }

    // ================================================================
    // #69 -- PERIMETER VOID CRACKS: Ground-level crack network between
    //        horn bases. 12 crack segments, 3 BlockDisplays each.
    //        Obsidian + black_concrete thin shards with PORTAL fill.
    // ================================================================
    public static class PerimeterVoidCracks extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crackBlocks = new ArrayList<>();

        public PerimeterVoidCracks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("perimeter_void_cracks", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 crack segments between adjacent horn pairs (every 30 deg)
            for (int i = 0; i < 12; i++) {
                double angle1 = Math.toRadians(i * 30.0);
                double angle2 = Math.toRadians((i + 1) * 30.0);
                double midAngle = (angle1 + angle2) / 2.0;
                double mx = Math.sin(midAngle) * 30;
                double mz = -Math.cos(midAngle) * 30;
                Location crackMid = center.clone().add(mx, 0, mz);

                float crackAngle = (float) Math.toRadians(15 + Math.random() * 10);

                // Primary obsidian crack shard
                BlockDisplayHandle shard1 = displayBuilder.spawnBlock(crackMid, Material.OBSIDIAN);
                shard1.scale(0.3f, 0.1f, 2.5f).rotate(crackAngle, 0, 1, 0)
                      .glow(30, 0, 60).interpolation(3, 0);
                crackBlocks.add(shard1);
                spawnedEntities.add(shard1.entity());

                // Secondary black_concrete overlap
                BlockDisplayHandle shard2 = displayBuilder.spawnBlock(crackMid.clone().add(0.2, 0, 0.3), Material.BLACK_CONCRETE);
                shard2.scale(0.2f, 0.05f, 1.8f).rotate(crackAngle + 0.15f, 0, 1, 0)
                      .glow(20, 0, 40).interpolation(3, 0);
                crackBlocks.add(shard2);
                spawnedEntities.add(shard2.entity());

                // Tertiary obsidian at opposite end
                BlockDisplayHandle shard3 = displayBuilder.spawnBlock(crackMid.clone().add(-0.3, 0, -0.2), Material.OBSIDIAN);
                shard3.scale(0.15f, 0.08f, 1.2f).rotate(crackAngle - 0.2f, 0, 1, 0)
                      .glow(30, 0, 60).interpolation(3, 0);
                crackBlocks.add(shard3);
                spawnedEntities.add(shard3.entity());

                // Spawn burst
                w.spawnParticle(Particle.PORTAL, crackMid, 20, 1.5, 0.5, 1.5, 0);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.3f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Permanent PORTAL emission from each crack segment
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 12; i++) {
                    double midAngle = Math.toRadians(i * 30.0 + 15);
                    double mx = Math.sin(midAngle) * 30;
                    double mz = -Math.cos(midAngle) * 30;
                    Location crackPos = center.clone().add(mx, 0.2, mz);
                    w.spawnParticle(Particle.PORTAL, crackPos, 1, 0.5, 0.2, 0.5, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PerimeterVoidCracks(plugin); }
    }

    // ================================================================
    // #70 -- DRAGON BREATH CONVERGENCE FUNNEL: Particle emitter at
    //        Y+45 above arena center. Downward DRAGON_BREATH cloud
    //        receiving rising horn streams. Increases at Phase 3.
    // ================================================================
    public static class DragonBreathConvergenceFunnel extends BlockDisplayAttack {

        public DragonBreathConvergenceFunnel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_breath_convergence_funnel", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only emitter, no blocks
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location funnelPos = center.clone().add(0, 45, 0);

            // DRAGON_BREATH downward cloud: velocity Y-0.06
            w.spawnParticle(Particle.DRAGON_BREATH, funnelPos, 15, 3.0, 0.5, 3.0, 0.06);
            // TOTEM golden mist
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, funnelPos, 6, 2.0, 0.5, 2.0, 0);

            // Ambient sound every 160 ticks
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(funnelPos, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DragonBreathConvergenceFunnel(plugin); }
    }
}
