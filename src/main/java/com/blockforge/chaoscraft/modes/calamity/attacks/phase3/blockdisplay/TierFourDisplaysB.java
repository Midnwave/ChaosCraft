package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay;

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

/**
 * Phase 4C Block Display -- TIER 5 ESCALATED: "LAST BREATH" (10% HP)
 * Structures 81-90. Multiple structures reactivate simultaneously.
 * Everything burns at 1.8x speed. Damage is the highest in the fight
 * outside the Finale. Visual density should feel overwhelming.
 *
 * Dweller palette: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255)
 * Rules: NO status effects, AxisAngle4f ONLY, damage 8.0-12.0 HP
 */
public final class TierFourDisplaysB {

    private TierFourDisplaysB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TwinBrimstoneColumns(plugin));
        registry.register(new TheMelt(plugin));
        registry.register(new TheCinderWheel(plugin));
        registry.register(new DwellerRibCage(plugin));
        registry.register(new LavaGeyserBattery(plugin));
        registry.register(new TheCollapse(plugin));
        registry.register(new BrimstoneOrrery(plugin));
        registry.register(new TheHungerWeb(plugin));
        registry.register(new VoidHeartDetonation(plugin));
        registry.register(new TheLastLightOfTheEnd(plugin));
    }

    // ================================================================
    // 81. TWIN BRIMSTONE COLUMNS — Two 24-block columns 15 blocks apart,
    //     connected by H-frame bridges with spine ring arrays
    // ================================================================
    public static class TwinBrimstoneColumns extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftColumn = new ArrayList<>();
        private final List<BlockDisplayHandle> rightColumn = new ArrayList<>();
        private final List<BlockDisplayHandle> bridges = new ArrayList<>();
        private final List<BlockDisplayHandle> spineRings = new ArrayList<>();
        private BlockDisplayHandle leftCap, rightCap;

        public TwinBrimstoneColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("twin_brimstone_columns", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
            config.setImpactDamage(8.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left column: 4x4 netherrack base (2), 3x3 basalt shaft (12), 2x2 magma (6), 1x1 polished tip (2)
            buildColumn(center.clone().add(-7.5, 0, 0), leftColumn, true);
            buildColumn(center.clone().add(7.5, 0, 0), rightColumn, false);

            // Fire charge caps
            leftCap = displayBuilder.spawnBlock(center.clone().add(-7.5, 22, 0), Material.MAGMA_BLOCK);
            leftCap.scale(1.2f, 1.2f, 1.2f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(leftCap.entity());

            rightCap = displayBuilder.spawnBlock(center.clone().add(7.5, 22, 0), Material.MAGMA_BLOCK);
            rightCap.scale(1.2f, 1.2f, 1.2f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(rightCap.entity());

            // Bridges at heights 8 and 16: 6 blackstone blocks each
            for (int bh : new int[]{8, 16}) {
                for (int x = -5; x <= 5; x += 2) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, bh, 0), Material.BLACKSTONE);
                    h.scale(1.5f, 0.5f, 1.0f).glow(40, 40, 50).interpolation(3, 0);
                    bridges.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Bridge magma
                for (int x = -3; x <= 3; x += 2) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, bh, 0), Material.MAGMA_BLOCK);
                    h.scale(0.6f, 0.5f, 0.6f).glow(255, 100, 0).interpolation(3, 0);
                    bridges.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Spine rings at every 6-block interval
            for (int side = -1; side <= 1; side += 2) {
                double cx = side * 7.5;
                for (int ringH : new int[]{6, 12, 18}) {
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 * i) / 8;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(cx + Math.cos(angle) * 1.5, ringH, Math.sin(angle) * 1.5),
                                Material.NETHERRACK);
                        h.scale(0.12f, 0.5f, 0.12f).glow(255, 100, 0).interpolation(3, 0);
                        spineRings.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 0.9f);
        }

        private void buildColumn(Location base, List<BlockDisplayHandle> col, boolean left) {
            // Netherrack base: 2 blocks
            for (int y = 0; y < 2; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(0, y, 0), Material.NETHERRACK);
                h.scale(1.5f, 1.0f, 1.5f).glow(120, 60, 40).interpolation(2, 0);
                col.add(h);
                spawnedEntities.add(h.entity());
            }
            // Basalt shaft: 12 blocks
            for (int y = 2; y < 14; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(0, y, 0), Material.BASALT);
                h.scale(1.2f, 1.0f, 1.2f).glow(70, 70, 80).interpolation(2, 0);
                col.add(h);
                spawnedEntities.add(h.entity());
            }
            // Magma shaft: 6 blocks
            for (int y = 14; y < 20; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                h.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                col.add(h);
                spawnedEntities.add(h.entity());
            }
            // Polished tip: 2 blocks
            for (int y = 20; y < 22; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(0, y, 0), Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 1.0f, 0.8f).glow(50, 50, 60).interpolation(2, 0);
                col.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Columns rotate opposite: 0.8°/tick
            float leftRot = ticksAlive * 0.01396f;
            float rightRot = -ticksAlive * 0.01396f;
            for (BlockDisplayHandle h : leftColumn) {
                h.rotate(leftRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : rightColumn) {
                h.rotate(rightRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Cap pulse: 25-tick alternating
            float leftScale = 1.2f + 0.5f * (float) Math.sin(ticksAlive * Math.PI / 12.5);
            float rightScale = 1.2f + 0.5f * (float) Math.sin((ticksAlive + 12) * Math.PI / 12.5);
            if (leftCap != null) {
                leftCap.scale(leftScale, leftScale, leftScale);
                leftCap.interpolation(3, 0);
            }
            if (rightCap != null) {
                rightCap.scale(rightScale, rightScale, rightScale);
                rightCap.interpolation(3, 0);
            }

            // Spine ring rotation: 2.5°/tick
            float spineRot = ticksAlive * 0.04363f;
            for (BlockDisplayHandle h : spineRings) {
                h.rotate(spineRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Bridge bob: 0.2 blocks, 70-tick sine
            float bridgeBob = 0.2f * (float) Math.sin(ticksAlive * Math.PI / 35);

            // Spine array burst every 50 ticks
            if (ticksAlive % 50 == 0 && ticksAlive > 0) {
                triggerImpactDamage(center);
                for (BlockDisplayHandle h : spineRings) {
                    h.scale(0.2f, 1.0f, 0.2f);
                    h.interpolation(5, 0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.9f);
            }

            // Flame from caps
            if (ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(-7.5, 23, 0), 5, 0.2, 0.5, 0.2, 0.03);
                w.spawnParticle(Particle.FLAME, center.clone().add(7.5, 23, 0), 5, 0.2, 0.5, 0.2, 0.03);
            }

            // Lava from bridge magma
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < bridges.size(); i += 3) {
                    w.spawnParticle(Particle.LAVA, bridges.get(i).entity().getLocation(), 2, 0.5, 0.1, 0.5, 0);
                }
            }

            // Soul fire flame from spine arrays
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < spineRings.size(); i += 4) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, spineRings.get(i).entity().getLocation(),
                            2, 0.3, 0.1, 0.3, 0.02);
                }
            }

            // Smoke in H-frame interior
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 12, 0), 4, 3, 2, 1, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TwinBrimstoneColumns(plugin); }
    }

    // ================================================================
    // 82. THE MELT — 20x20 flat surface liquefying with random sink/bubble
    // ================================================================
    public static class TheMelt extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> surfaceBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eruptionPosts = new ArrayList<>();

        public TheMelt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_melt", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] meltMats = {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.BLACKSTONE,
                    Material.CRACKED_STONE_BRICKS, Material.BASALT};
            // Simplified 20x20 grid (sampling every 2 blocks for performance)
            for (int x = -10; x < 10; x += 2) {
                for (int z = -10; z < 10; z += 2) {
                    float yOff = (float) (Math.random() * 1.0 - 0.5);
                    Material mat = meltMats[(Math.abs(x) + Math.abs(z)) % 5];
                    // Bias magma toward center
                    if (Math.abs(x) < 6 && Math.abs(z) < 6 && Math.random() > 0.5) mat = Material.MAGMA_BLOCK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, yOff, z), mat);
                    h.scale(2.0f, 0.5f, 2.0f).glow(mat == Material.MAGMA_BLOCK ? 255 : 120,
                            mat == Material.MAGMA_BLOCK ? 100 : 60, mat == Material.MAGMA_BLOCK ? 0 : 40)
                            .interpolation(3, 0);
                    surfaceBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Perimeter border
            for (int x = -10; x <= 10; x += 2) {
                for (int z : new int[]{-10, 10}) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.5, z), Material.BLACKSTONE);
                    h.scale(2.0f, 1.0f, 0.5f).glow(40, 40, 50).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }
            for (int z = -10; z <= 10; z += 2) {
                for (int x : new int[]{-10, 10}) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.5, z), Material.BLACKSTONE);
                    h.scale(0.5f, 1.0f, 2.0f).glow(40, 40, 50).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // 16 eruption posts
            for (int i = 0; i < 16; i++) {
                double px = (Math.random() - 0.5) * 18;
                double pz = (Math.random() - 0.5) * 18;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(px, 0.5, pz), Material.BASALT);
                h.scale(0.5f, 1.0f, 0.5f).glow(70, 70, 80).interpolation(3, 0);
                eruptionPosts.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Random sink: every 30 ticks, 1 block sinks 0.1
            if (ticksAlive % 30 == 0 && !surfaceBlocks.isEmpty()) {
                int idx = (int) (Math.random() * surfaceBlocks.size());
                Location loc = surfaceBlocks.get(idx).entity().getLocation();
                loc.add(0, -0.1, 0);
                surfaceBlocks.get(idx).entity().teleport(loc);
            }

            // Random bubble: every 15 ticks, 1 block rises 0.1
            if (ticksAlive % 15 == 0 && !surfaceBlocks.isEmpty()) {
                int idx = (int) (Math.random() * surfaceBlocks.size());
                Location loc = surfaceBlocks.get(idx).entity().getLocation();
                loc.add(0, 0.1, 0);
                surfaceBlocks.get(idx).entity().teleport(loc);
            }

            // Eruption post bob: independent cycles
            for (int i = 0; i < eruptionPosts.size(); i++) {
                int cycle = 40 + (i * 5) % 40;
                float bob = 0.3f * (float) Math.sin(ticksAlive * Math.PI / (cycle / 2.0));
                Location loc = eruptionPosts.get(i).entity().getLocation();
                loc.setY(center.getY() + 0.5 + bob);
                eruptionPosts.get(i).entity().teleport(loc);
            }

            // Lava bubbles across surface
            if (ticksAlive % 2 == 0) {
                double lx = (Math.random() - 0.5) * 20;
                double lz = (Math.random() - 0.5) * 20;
                w.spawnParticle(Particle.LAVA, center.clone().add(lx, 0.5, lz), 1, 0.1, 0.3, 0.1, 0);
            }

            // Dripping lava from eruption posts
            if (ticksAlive % 7 == 0) {
                int idx = (ticksAlive / 7) % eruptionPosts.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, eruptionPosts.get(idx).entity().getLocation(),
                        1, 0.1, 0, 0.1, 0);
            }

            // Flame cover
            if (ticksAlive % 3 == 0) {
                double fx = (Math.random() - 0.5) * 20;
                double fz = (Math.random() - 0.5) * 20;
                w.spawnParticle(Particle.FLAME, center.clone().add(fx, 0.5, fz), 1, 0.2, 0.1, 0.2, 0.01);
            }

            // Smoke from perimeter
            if (ticksAlive % 6 == 0) {
                double sAngle = Math.random() * Math.PI * 2;
                w.spawnParticle(Particle.SMOKE, center.clone().add(Math.cos(sAngle) * 10, 0.8, Math.sin(sAngle) * 10),
                        2, 0.2, 0.2, 0.2, 0.01);
            }

            // Sound
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheMelt(plugin); }
    }

    // ================================================================
    // 83. THE CINDER WHEEL — Massive horizontal wheel at 2.5°/tick
    //     with nether star axle and centrifugal fire spray
    // ================================================================
    public static class TheCinderWheel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> midRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> spokes = new ArrayList<>();
        private BlockDisplayHandle axle;
        private final List<BlockDisplayHandle> axleColumn = new ArrayList<>();

        public TheCinderWheel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_cinder_wheel", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 32 netherrack at radius 9
            for (int i = 0; i < 32; i++) {
                double angle = (Math.PI * 2 * i) / 32;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 9, 4, Math.sin(angle) * 9), Material.NETHERRACK);
                h.scale(1.2f, 0.5f, 1.2f).glow(120, 60, 40).interpolation(3, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring: 24 magma at radius 6
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 * i) / 24;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 6, 4, Math.sin(angle) * 6), Material.MAGMA_BLOCK);
                h.scale(1.0f, 0.5f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                midRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner ring: 12 basalt at radius 3
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3, 4, Math.sin(angle) * 3), Material.BASALT);
                h.scale(1.0f, 0.5f, 1.0f).glow(70, 70, 80).interpolation(3, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Spokes: blackstone between outer and middle
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                for (int seg = 0; seg < 3; seg++) {
                    double r = 6.5 + seg * 0.8;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * r, 4, Math.sin(angle) * r), Material.BLACKSTONE);
                    h.scale(0.5f, 0.5f, 0.5f).glow(40, 40, 50).interpolation(2, 0);
                    spokes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Axle: nether star (glowstone)
            axle = displayBuilder.spawnBlock(center.clone().add(0, 4, 0), Material.GLOWSTONE);
            axle.scale(1.3f, 1.3f, 1.3f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(axle.entity());

            // Axle column below
            for (int y = 0; y < 4; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.BASALT);
                h.scale(0.6f, 1.0f, 0.6f).glow(70, 70, 80).interpolation(2, 0);
                axleColumn.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wheel rotation: 2.5°/tick (very fast)
            float wheelRot = ticksAlive * 0.04363f;

            // Outer ring bob: 0.15 blocks, 80-tick sine
            float outerBob = 0.15f * (float) Math.sin(ticksAlive * Math.PI / 40);
            // Middle bob: opposite phase
            float midBob = -outerBob;

            // Rotate all ring blocks around center
            for (int i = 0; i < outerRing.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 32 + wheelRot;
                Location loc = center.clone().add(Math.cos(baseAngle) * 9, 4 + outerBob, Math.sin(baseAngle) * 9);
                outerRing.get(i).entity().teleport(loc);
            }
            for (int i = 0; i < midRing.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 24 + wheelRot;
                Location loc = center.clone().add(Math.cos(baseAngle) * 6, 4 + midBob, Math.sin(baseAngle) * 6);
                midRing.get(i).entity().teleport(loc);
            }
            for (int i = 0; i < innerRing.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 12 + wheelRot;
                Location loc = center.clone().add(Math.cos(baseAngle) * 3, 4, Math.sin(baseAngle) * 3);
                innerRing.get(i).entity().teleport(loc);
            }

            // Axle counter-rotation: 4.0°/tick + tumble
            float axleRot = -ticksAlive * 0.06981f;
            if (axle != null) {
                axle.rotate(axleRot, 0.3f, 1.0f, 0);
                axle.interpolation(3, 0);
            }

            // Centrifugal flame spray from outer ring
            if (ticksAlive % 2 == 0) {
                int idx = (ticksAlive / 2) % outerRing.size();
                Location bLoc = outerRing.get(idx).entity().getLocation();
                double dx = bLoc.getX() - center.getX();
                double dz = bLoc.getZ() - center.getZ();
                w.spawnParticle(Particle.FLAME, bLoc, 2, dx * 0.05, 0.02, dz * 0.05, 0.06);
            }

            // Lava from middle ring
            if (ticksAlive % 4 == 0) {
                int idx = (ticksAlive / 4) % midRing.size();
                w.spawnParticle(Particle.LAVA, midRing.get(idx).entity().getLocation(), 1, 0.3, 0.1, 0.3, 0);
            }

            // Soul fire from axle downward
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 3, 0), 3,
                        0.3, 0.5, 0.3, 0.02);
            }

            // Sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheCinderWheel(plugin); }
    }

    // ================================================================
    // 84. DWELLER RIB CAGE — 16x20x10 freestanding rib cage with
    //     peristaltic breathing wave and crimson spore atmosphere
    // ================================================================
    public static class DwellerRibCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spineColumn = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> ribs = new ArrayList<>();
        private final List<BlockDisplayHandle> baseRing = new ArrayList<>();

        public DwellerRibCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_rib_cage", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central spine: 20 cracked stone brick blocks
            for (int y = 0; y < 20; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, -5), Material.CRACKED_STONE_BRICKS);
                h.scale(0.5f, 1.0f, 0.5f).glow(70, 70, 80).interpolation(3, 0);
                spineColumn.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 rib arches spaced 1.5 blocks apart
            for (int r = 0; r < 12; r++) {
                List<BlockDisplayHandle> rib = new ArrayList<>();
                float ribY = r * 1.5f + 1;
                // Each rib: 8 blocks following curve from spine outward and back
                for (int seg = 0; seg < 8; seg++) {
                    double t = (double) seg / 7;
                    double x = Math.sin(t * Math.PI) * 8; // Width spread
                    double z = -5 + t * 10; // From spine (back) to ventral (front)
                    Material mat;
                    if (seg == 0 || seg == 7) mat = Material.SMOOTH_BASALT; // Dorsal
                    else if (seg == 3 || seg == 4) mat = Material.MAGMA_BLOCK; // Ventral apex
                    else mat = Material.BASALT;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x - 4, ribY, z), mat);
                    h.scale(0.7f, 0.6f, 0.7f).glow(mat == Material.MAGMA_BLOCK ? 255 : 80,
                            mat == Material.MAGMA_BLOCK ? 100 : 80, mat == Material.MAGMA_BLOCK ? 0 : 90)
                            .interpolation(3, 0);
                    rib.add(h);
                    spawnedEntities.add(h.entity());
                }
                ribs.add(rib);
            }

            // Base: pelvis ring of netherrack
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 3), Material.NETHERRACK);
                h.scale(1.0f, 0.5f, 1.0f).glow(120, 60, 40).interpolation(2, 0);
                baseRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cervical top: 3 polished blackstone
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(i * 0.5, 20, -5), Material.POLISHED_BLACKSTONE);
                h.scale(0.5f, 0.5f, 0.5f).glow(50, 50, 60).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spine breathing: 2° X-axis, 200-tick cycle
            float spineBreath = 0.035f * (float) Math.sin(ticksAlive * Math.PI / 100);

            // Peristaltic rib wave: each rib expands 0.4 outward on staggered 25-tick phases
            for (int r = 0; r < ribs.size(); r++) {
                int phase = (ticksAlive - r * 25) % (ribs.size() * 25);
                float expand = phase < 25 ? 0.4f * (float) Math.sin(phase * Math.PI / 25) : 0;

                // Magma ventral apex pulse
                for (int seg = 3; seg <= 4 && seg < ribs.get(r).size(); seg++) {
                    float mScale = 0.7f + 0.14f * (float) Math.sin(ticksAlive * Math.PI / 15);
                    ribs.get(r).get(seg).scale(mScale, 0.6f, mScale);
                    ribs.get(r).get(seg).interpolation(3, 0);
                }
            }

            // Neck search: circular path 0.2 blocks
            float neckX = 0.2f * (float) Math.cos(ticksAlive * 0.03);
            float neckZ = 0.2f * (float) Math.sin(ticksAlive * 0.03);

            // Flame from magma rib apexes
            if (ticksAlive % 4 == 0) {
                for (List<BlockDisplayHandle> rib : ribs) {
                    if (rib.size() >= 5) {
                        w.spawnParticle(Particle.FLAME, rib.get(3).entity().getLocation().add(0, -0.3, 0),
                                1, 0.05, 0.2, 0.05, 0.01);
                    }
                }
            }

            // Crimson spore through interior
            if (ticksAlive % 4 == 0) {
                double ix = (Math.random() - 0.5) * 10;
                double iy = Math.random() * 18 + 1;
                double iz = (Math.random() - 0.5) * 8;
                w.spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(ix, iy, iz), 3, 0.3, 0.3, 0.3, 0);
            }

            // Dripping lava from spine
            if (ticksAlive % 6 == 0) {
                int sIdx = (ticksAlive / 6) % spineColumn.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, spineColumn.get(sIdx).entity().getLocation(),
                        1, 0.05, 0, 0.05, 0);
            }

            // Ash from broken neck
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.ASH, center.clone().add(0, 20, -5), 3, 0.3, 0.5, 0.3, 0);
            }

            // Sound
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.4f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellerRibCage(plugin); }
    }

    // ================================================================
    // 85. LAVA GEYSER BATTERY — 3x3 grid of 9 geyser vents with
    //     staggered sequential firing pattern
    // ================================================================
    public static class LavaGeyserBattery extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> vents = new ArrayList<>();
        private final List<BlockDisplayHandle> channels = new ArrayList<>();

        public LavaGeyserBattery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_geyser_battery", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3x3 grid of vents, 4 blocks apart
            for (int gx = -1; gx <= 1; gx++) {
                for (int gz = -1; gz <= 1; gz++) {
                    List<BlockDisplayHandle> vent = new ArrayList<>();
                    Location ventBase = center.clone().add(gx * 4, 0, gz * 4);

                    // 3x3 netherrack platform
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    ventBase.clone().add(x, 0, z), Material.NETHERRACK);
                            h.scale(1.0f, 0.5f, 1.0f).glow(120, 60, 40).interpolation(2, 0);
                            vent.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }

                    // Central magma vent
                    BlockDisplayHandle magmaVent = displayBuilder.spawnBlock(ventBase, Material.MAGMA_BLOCK);
                    magmaVent.scale(0.8f, 0.5f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                    vent.add(magmaVent);
                    spawnedEntities.add(magmaVent.entity());

                    // Chimney: 3-5 blocks of basalt/blackstone
                    int chimH = 3 + (Math.abs(gx) + Math.abs(gz));
                    for (int y = 1; y <= chimH; y++) {
                        Material mat = y % 2 == 0 ? Material.BASALT : Material.BLACKSTONE;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                ventBase.clone().add(0, y, 0), mat);
                        h.scale(0.6f, 1.0f, 0.6f).glow(60, 60, 70).interpolation(3, 0);
                        vent.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    vents.add(vent);
                }
            }

            // Connecting channels between adjacent vents
            for (int gx = -1; gx <= 1; gx++) {
                for (int gz = -1; gz < 1; gz++) {
                    for (int seg = 1; seg < 4; seg++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(gx * 4, 0, gz * 4 + seg), Material.CRACKED_STONE_BRICKS);
                        h.scale(0.8f, 0.3f, 0.8f).glow(70, 70, 80).interpolation(2, 0);
                        channels.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Perimeter: blackstone wall
            for (int x = -6; x <= 6; x += 2) {
                for (int z : new int[]{-6, 6}) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.5, z), Material.BLACKSTONE);
                    h.scale(1.5f, 1.0f, 0.5f).glow(40, 40, 50).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Staggered firing: vent fires every 80-tick cycle, each 8 ticks apart
            int cyclePos = ticksAlive % 80;
            int firingVent = cyclePos / 8;

            if (cyclePos % 8 == 0 && firingVent < vents.size()) {
                // Fire this vent
                List<BlockDisplayHandle> vent = vents.get(firingVent);
                if (!vent.isEmpty()) {
                    Location ventLoc = vent.get(0).entity().getLocation();

                    // Scale chimney burst
                    for (int i = 10; i < vent.size(); i++) {
                        vent.get(i).scale(0.8f, 1.4f, 0.8f);
                        vent.get(i).interpolation(5, 0);
                    }

                    // Lava eruption
                    w.spawnParticle(Particle.LAVA, ventLoc.clone().add(0, 4, 0), 15, 1.5, 3, 1.5, 0.1);
                    DisplayBuilder.playSound(ventLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
                    triggerImpactDamage(ventLoc);
                }
            }

            // Reset chimney scale after 10 ticks
            if (cyclePos % 8 == 5) {
                int resetVent = (cyclePos - 5) / 8;
                if (resetVent >= 0 && resetVent < vents.size()) {
                    for (int i = 10; i < vents.get(resetVent).size(); i++) {
                        vents.get(resetVent).get(i).scale(0.6f, 1.0f, 0.6f);
                        vents.get(resetVent).get(i).interpolation(10, 0);
                    }
                }
            }

            // Flame from all chimneys
            if (ticksAlive % 4 == 0) {
                for (List<BlockDisplayHandle> vent : vents) {
                    if (vent.size() > 10) {
                        w.spawnParticle(Particle.FLAME, vent.get(10).entity().getLocation().add(0, 1, 0),
                                2, 0.1, 0.3, 0.1, 0.01);
                    }
                }
            }

            // Dripping lava in channels
            if (ticksAlive % 8 == 0 && !channels.isEmpty()) {
                int cIdx = (ticksAlive / 8) % channels.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, channels.get(cIdx).entity().getLocation(),
                        1, 0.1, 0, 0.1, 0);
            }

            // Smoke from perimeter
            if (ticksAlive % 7 == 0) {
                double sAngle = Math.random() * Math.PI * 2;
                w.spawnParticle(Particle.SMOKE, center.clone().add(Math.cos(sAngle) * 6, 1, Math.sin(sAngle) * 6),
                        2, 0.2, 0.2, 0.2, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaGeyserBattery(plugin); }
    }

    // ================================================================
    // 86. THE COLLAPSE — 35-block tower actively falling apart with
    //     swaying sections and falling block debris
    // ================================================================
    public static class TheCollapse extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> sections = new ArrayList<>();
        private final List<BlockDisplayHandle> fallenBlocks = new ArrayList<>();

        public TheCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_collapse", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
            config.setImpactDamage(8.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 sections of 7 blocks each, decreasing profile
            Material[][] sectionMats = {
                    {Material.BLACKSTONE}, {Material.BASALT}, {Material.NETHERRACK},
                    {Material.CRACKED_STONE_BRICKS}, {Material.NETHERRACK, Material.MAGMA_BLOCK}
            };
            float[] sectionWidths = {2.0f, 1.5f, 1.5f, 1.0f, 0.6f};
            float[] sectionTilts = {0, 0, 0, 0.087f, 0}; // Section 4 leans 5°

            for (int s = 0; s < 5; s++) {
                List<BlockDisplayHandle> section = new ArrayList<>();
                for (int y = 0; y < 7; y++) {
                    Material mat = sectionMats[s][y % sectionMats[s].length];
                    // Skip some blocks in upper sections for damaged look
                    if (s >= 2 && y % 3 == 2) continue;
                    if (s >= 3 && y % 2 == 1) continue;

                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(0, s * 7 + y, 0), mat);
                    h.scale(sectionWidths[s], 1.0f, sectionWidths[s])
                            .glow(mat == Material.MAGMA_BLOCK ? 255 : 70,
                                    mat == Material.MAGMA_BLOCK ? 100 : 70,
                                    mat == Material.MAGMA_BLOCK ? 0 : 80)
                            .interpolation(3, 0);
                    if (sectionTilts[s] > 0) {
                        h.rotate(sectionTilts[s], 0, 0, 1);
                    }
                    section.add(h);
                    spawnedEntities.add(h.entity());
                }
                sections.add(section);
            }

            // 12 fallen blocks scattered around
            Material[] fallenMats = {Material.BLACKSTONE, Material.BASALT, Material.NETHERRACK};
            for (int i = 0; i < 12; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = 2 + Math.random() * 6;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r),
                        fallenMats[i % 3]);
                h.scale(0.8f, 0.8f, 0.8f).glow(60, 60, 70)
                        .rotate((float) (Math.random() * 0.5), 0.5f, 0.3f, 0.2f).interpolation(2, 0);
                fallenBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Section sway: increasing magnitude with height
            float[] swayMags = {0.0087f, 0.026f, 0.052f, 0.105f, 0.21f}; // 0.5, 1.5, 3, 6, 12 degrees
            for (int s = 0; s < sections.size(); s++) {
                float sway = swayMags[s] * (float) Math.sin(ticksAlive * Math.PI / (40 + s * 10));
                int direction = s % 2 == 0 ? 1 : -1;
                for (BlockDisplayHandle h : sections.get(s)) {
                    h.rotate(sway * direction, 0, 0, 1);
                    h.interpolation(4, 0);
                }
            }

            // Top section bob: 0.3 blocks vertical
            if (sections.size() >= 5) {
                float topBob = 0.3f * (float) Math.sin(ticksAlive * Math.PI / 20);
            }

            // Falling block relocation: every 60 ticks
            if (ticksAlive % 60 == 0 && !fallenBlocks.isEmpty()) {
                int idx = (ticksAlive / 60) % fallenBlocks.size();
                double angle = Math.random() * Math.PI * 2;
                double r = 2 + Math.random() * 8;
                Location newLoc = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                fallenBlocks.get(idx).entity().teleport(newLoc);
                // Scale pulse: 0.1 to 1.0 over 8 ticks
                fallenBlocks.get(idx).scale(0.1f, 0.1f, 0.1f);
                fallenBlocks.get(idx).interpolation(8, 0);
                // Landing damage
                triggerImpactDamage(newLoc);
                DisplayBuilder.playSound(newLoc, Sound.BLOCK_STONE_PLACE, 0.6f, 0.6f);

                // Reset scale after
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (idx < fallenBlocks.size()) {
                        fallenBlocks.get(idx).scale(0.8f, 0.8f, 0.8f);
                        fallenBlocks.get(idx).interpolation(8, 0);
                    }
                }, 10);
            }

            // Top sections detach at T+900 (45 seconds)
            if (ticksAlive == 900 && sections.size() >= 5) {
                // Sections 4 and 5 slam down
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 2, 0), 30, 2, 1, 2, 0.15);
                triggerImpactDamage(center);
            }

            // Smoke from section junctions
            if (ticksAlive % 5 == 0) {
                for (int s = 1; s < sections.size(); s++) {
                    w.spawnParticle(Particle.SMOKE, center.clone().add(0, s * 7, 0), 2, 0.5, 0.2, 0.5, 0.01);
                }
            }

            // Crimson spore and ash from upper sections
            if (ticksAlive % 6 == 0) {
                w.spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(0, 20, 0), 3, 1, 3, 1, 0);
                w.spawnParticle(Particle.ASH, center.clone().add(0, 25, 0), 3, 0.5, 2, 0.5, 0);
            }

            // Lava from top magma block
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 34, 0), 2, 0.3, 0.5, 0.3, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheCollapse(plugin); }
    }

    // ================================================================
    // 87. BRIMSTONE ORRERY — Multi-ring orrery with 3 orbital rings
    //     at different tilts and a tumbling nether star center
    // ================================================================
    public static class BrimstoneOrrery extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ring1 = new ArrayList<>();
        private final List<BlockDisplayHandle> ring2 = new ArrayList<>();
        private final List<BlockDisplayHandle> ring3 = new ArrayList<>();
        private BlockDisplayHandle centralStar;
        private final List<BlockDisplayHandle> axleColumn = new ArrayList<>();

        public BrimstoneOrrery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_orrery", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ring 1 (radius 4, horizontal): 16 magma blocks
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 4, 6, Math.sin(angle) * 4), Material.MAGMA_BLOCK);
                h.scale(0.8f, 0.5f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                ring1.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring 2 (radius 7, tilted 30°): 20 netherrack
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double y = 6 + Math.sin(angle) * 3.5; // Tilt creates Y variation
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 7, y, Math.sin(angle) * 7 * Math.cos(0.52)),
                        Material.NETHERRACK);
                h.scale(0.8f, 0.5f, 0.8f).glow(120, 60, 40).interpolation(3, 0);
                ring2.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring 3 (radius 10, tilted 60°): 24 blackstone
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 * i) / 24;
                double y = 6 + Math.sin(angle) * 8.66; // Near-vertical
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 10, y, Math.sin(angle) * 5),
                        Material.BLACKSTONE);
                h.scale(0.7f, 0.5f, 0.7f).glow(40, 40, 50).interpolation(3, 0);
                ring3.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central nether star (glowstone)
            centralStar = displayBuilder.spawnBlock(center.clone().add(0, 6, 0), Material.GLOWSTONE);
            centralStar.scale(1.6f, 1.6f, 1.6f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(centralStar.entity());

            // Axle column
            for (int y = 0; y < 6; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.BASALT);
                h.scale(0.4f, 1.0f, 0.4f).glow(70, 70, 80).interpolation(2, 0);
                axleColumn.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring 1 orbit: 1.5°/tick
            float r1Rot = ticksAlive * 0.02618f;
            for (int i = 0; i < ring1.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16 + r1Rot;
                Location loc = center.clone().add(Math.cos(angle) * 4, 6, Math.sin(angle) * 4);
                ring1.get(i).entity().teleport(loc);
            }

            // Ring 2 orbit: 0.8°/tick opposite
            float r2Rot = -ticksAlive * 0.01396f;
            for (int i = 0; i < ring2.size(); i++) {
                double angle = (Math.PI * 2 * i) / 20 + r2Rot;
                double y = 6 + Math.sin(angle) * 3.5;
                Location loc = center.clone().add(Math.cos(angle) * 7, y, Math.sin(angle) * 7 * Math.cos(0.52));
                ring2.get(i).entity().teleport(loc);
            }

            // Ring 3 orbit: 0.4°/tick same as R1
            float r3Rot = ticksAlive * 0.00698f;
            for (int i = 0; i < ring3.size(); i++) {
                double angle = (Math.PI * 2 * i) / 24 + r3Rot;
                double y = 6 + Math.sin(angle) * 8.66;
                Location loc = center.clone().add(Math.cos(angle) * 10, y, Math.sin(angle) * 5);
                ring3.get(i).entity().teleport(loc);
            }

            // Central star tumble
            float starRot = ticksAlive * 0.0349f;
            if (centralStar != null) {
                centralStar.rotate(starRot, 0.7f, 2.0f, 0.3f);
                centralStar.interpolation(3, 0);
            }

            // Fire charge pulse on Ring 2 (every 4th block)
            for (int i = 0; i < ring2.size(); i += 5) {
                int offset = i * 20;
                float pScale = 0.8f + 0.3f * (float) Math.sin((ticksAlive + offset) * Math.PI / 10);
                ring2.get(i).scale(pScale, 0.5f, pScale);
                ring2.get(i).interpolation(3, 0);
            }

            // Lava from Ring 1 magma
            if (ticksAlive % 3 == 0) {
                int idx = (ticksAlive / 3) % ring1.size();
                Location bLoc = ring1.get(idx).entity().getLocation();
                double dx = bLoc.getX() - center.getX();
                double dz = bLoc.getZ() - center.getZ();
                w.spawnParticle(Particle.LAVA, bLoc, 2, dx * 0.05, 0.05, dz * 0.05, 0);
            }

            // Flame from Ring 2 fire charges
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < ring2.size(); i += 5) {
                    w.spawnParticle(Particle.FLAME, ring2.get(i).entity().getLocation().add(0, -0.5, 0),
                            2, 0.1, 0.3, 0.1, 0.02);
                }
            }

            // Soul fire from nether star down to ground
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 4, 0), 3,
                        0.3, 1.0, 0.3, 0.02);
            }

            // Sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneOrrery(plugin); }
    }

    // ================================================================
    // 88. THE HUNGER WEB — 24x24 canopy web with radial spokes,
    //     concentric rings, and swinging pendulum drops
    // ================================================================
    public static class TheHungerWeb extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spokeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> pendulums = new ArrayList<>();
        private BlockDisplayHandle hubStar;
        private final List<BlockDisplayHandle> hubMagma = new ArrayList<>();

        public TheHungerWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_hunger_web", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 radial spokes of 10 blackstone blocks each
            for (int s = 0; s < 12; s++) {
                double angle = (Math.PI * 2 * s) / 12;
                for (int seg = 1; seg <= 10; seg++) {
                    double r = seg * 1.2;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * r, 8, Math.sin(angle) * r), Material.BLACKSTONE);
                    h.scale(0.5f, 0.3f, 0.5f).glow(40, 40, 50).interpolation(2, 0);
                    spokeBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Concentric rings at radii 5, 10
            for (int radius : new int[]{5, 10}) {
                int count = radius == 5 ? 12 : 20;
                for (int i = 0; i < count; i++) {
                    double angle = (Math.PI * 2 * i) / count;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * radius, 8, Math.sin(angle) * radius),
                            Material.NETHERRACK);
                    h.scale(0.6f, 0.3f, 0.6f).glow(120, 60, 40).interpolation(2, 0);
                    ringBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Central hub: 9 magma blocks + star
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 8, z), Material.MAGMA_BLOCK);
                    h.scale(1.0f, 0.4f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                    hubMagma.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            hubStar = displayBuilder.spawnBlock(center.clone().add(0, 8.5, 0), Material.GLOWSTONE);
            hubStar.scale(1.0f, 1.0f, 1.0f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(hubStar.entity());

            // Hanging pendulums at spoke-ring intersections
            for (int s = 0; s < 12; s++) {
                double angle = (Math.PI * 2 * s) / 12;
                for (int radius : new int[]{5, 10}) {
                    int hangLen = radius == 5 ? 3 : 4;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * radius, 8 - hangLen, Math.sin(angle) * radius),
                            Material.NETHERRACK);
                    h.scale(0.12f, (float) hangLen * 0.4f, 0.12f).glow(255, 100, 0).interpolation(3, 0);
                    pendulums.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Corner support columns
            double[][] corners = {{-10, 0, -10}, {10, 0, -10}, {-10, 0, 10}, {10, 0, 10}};
            for (double[] corner : corners) {
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(corner[0], y + 4, corner[2]), Material.BASALT);
                    h.scale(0.6f, 1.0f, 0.6f).glow(70, 70, 80)
                            .rotate(1.047f, 0, 1, 0).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Web rotation: 0.2°/tick
            float webRot = ticksAlive * 0.00349f;

            // Spoke arm flex: 0.3 blocks down on staggered 30-tick phases
            // (visual via rotation of spokes)
            for (int i = 0; i < spokeBlocks.size(); i++) {
                spokeBlocks.get(i).rotate(webRot, 0, 1, 0);
                spokeBlocks.get(i).interpolation(4, 0);
            }

            // Pendulum swing: 8°/tick amplitude, staggered
            for (int i = 0; i < pendulums.size(); i++) {
                float swing = 0.14f * (float) Math.sin((ticksAlive + i * 5) * Math.PI / 4);
                pendulums.get(i).rotate(swing, 1, 0, 0);
                pendulums.get(i).interpolation(3, 0);
            }

            // Hub star tumble
            float starRot = ticksAlive * 0.02f;
            if (hubStar != null) {
                hubStar.rotate(starRot, 0.5f, 1.0f, 0.3f);
                hubStar.interpolation(3, 0);
            }

            // Lava drip rain from canopy (low density)
            if (ticksAlive % 3 == 0) {
                double dx = (Math.random() - 0.5) * 24;
                double dz = (Math.random() - 0.5) * 24;
                w.spawnParticle(Particle.LAVA, center.clone().add(dx, 7.5, dz), 1, 0.1, 0, 0.1, 0);
            }

            // Flame from hub
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 9, 0), 3, 0.3, 0.3, 0.3, 0.02);
            }

            // Dripping lava from pendulum tips
            if (ticksAlive % 5 == 0) {
                int idx = (ticksAlive / 5) % pendulums.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, pendulums.get(idx).entity().getLocation(),
                        1, 0.05, 0, 0.05, 0);
            }

            // Sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheHungerWeb(plugin); }
    }

    // ================================================================
    // 89. VOID-HEART DETONATION — 12-diameter sphere that charges
    //     for 80 ticks then detonates with massive particle burst
    // ================================================================
    public static class VoidHeartDetonation extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerShell = new ArrayList<>();
        private final List<BlockDisplayHandle> innerShell = new ArrayList<>();
        private BlockDisplayHandle core;
        private final List<BlockDisplayHandle> cardinalCharges = new ArrayList<>();
        private boolean detonated = false;

        public VoidHeartDetonation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_heart_detonation", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(100); // Short: 80 ticks charge + detonation
            config.setCooldownTicks(600);
            config.setImpactDamage(12.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer shell: 48 netherrack on sphere surface (radius 6)
            List<BlockDisplayHandle> sphere = displayBuilder.spawnSphere(center.clone().add(0, 5, 0),
                    Material.NETHERRACK, 6, 48);
            for (BlockDisplayHandle h : sphere) {
                h.scale(0.7f, 0.7f, 0.7f).glow(120, 60, 40).interpolation(3, 0);
                outerShell.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner shell: 24 magma blocks at radius 2.5
            List<BlockDisplayHandle> inner = displayBuilder.spawnSphere(center.clone().add(0, 5, 0),
                    Material.MAGMA_BLOCK, 2.5, 24);
            for (BlockDisplayHandle h : inner) {
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                innerShell.add(h);
                spawnedEntities.add(h.entity());
            }

            // Core: nether star (glowstone), scale 2.0
            core = displayBuilder.spawnBlock(center.clone().add(0, 5, 0), Material.GLOWSTONE);
            core.scale(2.0f, 2.0f, 2.0f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(core.entity());

            // 6 cardinal fire charge displays (magma blocks at cardinal positions)
            double[][] cardinals = {{2, 5, 0}, {-2, 5, 0}, {0, 7, 0}, {0, 3, 0}, {0, 5, 2}, {0, 5, -2}};
            for (double[] cd : cardinals) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(cd[0], cd[1], cd[2]), Material.MAGMA_BLOCK);
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 0).interpolation(3, 0);
                cardinalCharges.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (detonated) return;

            // Outer shell rotation: 1.0°/tick
            float outerRot = ticksAlive * 0.01745f;
            // Inner shell counter-rotation: 1.5°/tick
            float innerRot = -ticksAlive * 0.02618f;

            // Core charging: scale from 2.0 to 3.3 over 80 ticks
            float coreScale = 2.0f + (ticksAlive / 80.0f) * 1.3f;
            if (coreScale > 3.3f) coreScale = 3.3f;
            if (core != null) {
                core.scale(coreScale, coreScale, coreScale);
                core.interpolation(3, 0);
            }

            // Cardinal charges orbit
            float cardRot = ticksAlive * 0.0349f;
            for (int i = 0; i < 3; i++) {
                // Y-axis orbit
                double angle = (Math.PI * 2 * i) / 3 + cardRot;
                cardinalCharges.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 2, 5, Math.sin(angle) * 2));
            }
            for (int i = 3; i < 6 && i < cardinalCharges.size(); i++) {
                // X-axis orbit
                double angle = (Math.PI * 2 * (i - 3)) / 3 + cardRot;
                cardinalCharges.get(i).entity().teleport(
                        center.clone().add(0, 5 + Math.sin(angle) * 2, Math.cos(angle) * 2));
            }

            // Inward-spiraling lava
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < outerShell.size(); i += 8) {
                    Location sLoc = outerShell.get(i).entity().getLocation();
                    double dx = center.getX() - sLoc.getX();
                    double dy = center.getY() + 5 - sLoc.getY();
                    double dz = center.getZ() - sLoc.getZ();
                    w.spawnParticle(Particle.LAVA, sLoc, 1, dx * 0.1, dy * 0.1, dz * 0.1, 0);
                }
            }

            // Flame from cardinals
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle cc : cardinalCharges) {
                    w.spawnParticle(Particle.FLAME, cc.entity().getLocation(), 2, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // Charging sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f + ticksAlive * 0.005f, 0.6f);
            }

            // DETONATION at tick 80
            if (ticksAlive >= 80) {
                detonated = true;

                // Collapse all to 0.1x scale
                for (BlockDisplayHandle h : outerShell) {
                    h.scale(0.1f, 0.1f, 0.1f);
                    h.interpolation(3, 0);
                }
                for (BlockDisplayHandle h : innerShell) {
                    h.scale(0.1f, 0.1f, 0.1f);
                    h.interpolation(3, 0);
                }
                if (core != null) {
                    core.scale(0.1f, 0.1f, 0.1f);
                    core.interpolation(3, 0);
                }

                // Massive particle burst
                Location det = center.clone().add(0, 5, 0);
                w.spawnParticle(Particle.LAVA, det, 100, 6, 6, 6, 0.3);
                w.spawnParticle(Particle.FLAME, det, 80, 6, 6, 6, 0.2);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, det, 50, 6, 6, 6, 0.2);
                w.spawnParticle(Particle.SMOKE, det, 30, 6, 6, 6, 0.1);

                triggerImpactDamage(det);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidHeartDetonation(plugin); }
    }

    // ================================================================
    // 90. THE LAST LIGHT OF THE END — Single end crystal replica on
    //     obsidian pillar, being consumed by brimstone. Atmosphere piece.
    // ================================================================
    public static class TheLastLightOfTheEnd extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillar = new ArrayList<>();
        private BlockDisplayHandle crystalCore;
        private final List<BlockDisplayHandle> glassCube = new ArrayList<>();
        private final List<BlockDisplayHandle> magmaFormations = new ArrayList<>();
        private final List<BlockDisplayHandle> netherrackRing = new ArrayList<>();

        public TheLastLightOfTheEnd(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("last_light_of_the_end", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(1400);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Obsidian pillar: 10 blocks, 1x1
            for (int y = 0; y < 10; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(30, 0, 40).interpolation(2, 0);
                pillar.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crystal core: nether star (glowstone with white glow)
            crystalCore = displayBuilder.spawnBlock(center.clone().add(0, 11, 0), Material.GLOWSTONE);
            crystalCore.scale(0.6f, 0.6f, 0.6f).glow(255, 255, 255).interpolation(3, 0);
            spawnedEntities.add(crystalCore.entity());

            // Glass cube (cracked stone brick as "cracked glass")
            double[][] cubeOff = {{0.5, 11, 0}, {-0.5, 11, 0}, {0, 11, 0.5}, {0, 11, -0.5}};
            for (double[] off : cubeOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.CRACKED_STONE_BRICKS);
                h.scale(0.4f, 0.5f, 0.4f).glow(200, 200, 180).interpolation(3, 0);
                glassCube.add(h);
                spawnedEntities.add(h.entity());
            }

            // Magma formations at pillar base (4 faces, 3x2x2 each)
            for (int face = 0; face < 4; face++) {
                double angle = (Math.PI * 2 * face) / 4;
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(Math.cos(angle) * (1.2 + x * 0.3), y, Math.sin(angle) * (1.2 + x * 0.3)),
                                Material.MAGMA_BLOCK);
                        h.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                        magmaFormations.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Ground ring: 8 netherrack
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 2, -0.5, Math.sin(angle) * 2), Material.NETHERRACK);
                h.scale(1.0f, 0.5f, 1.0f).glow(120, 60, 40).interpolation(2, 0);
                netherrackRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crystal Y-rotation: 0.3°/tick (slower than normal, winding down)
            float crystalRot = ticksAlive * 0.00524f;
            if (crystalCore != null) {
                crystalCore.rotate(crystalRot, 0, 1, 0);
                // Heartbeat pulse: 120-tick cycle, slowing down
                float cScale = 0.6f + 0.06f * (float) Math.sin(ticksAlive * Math.PI / 60);
                crystalCore.scale(cScale, cScale, cScale);
                crystalCore.interpolation(3, 0);
            }

            // Glass cube jitter: 0.02-block micro-translations
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : glassCube) {
                    float jx = (float) (Math.random() - 0.5) * 0.04f;
                    float jz = (float) (Math.random() - 0.5) * 0.04f;
                    h.translate(-0.2f + jx, -0.25f + jx, -0.2f + jz);
                    h.interpolation(5, 0);
                }
            }

            // Magma formations slowly growing: scale 1.0 to 1.1 over 300 ticks
            float magmaGrowth = 1.0f + Math.min(ticksAlive / 3000.0f, 0.1f);
            for (BlockDisplayHandle h : magmaFormations) {
                h.scale(0.5f * magmaGrowth, 0.5f * magmaGrowth, 0.5f * magmaGrowth);
                h.interpolation(5, 0);
            }

            // Soul fire flame from magma formations inward toward pillar
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < magmaFormations.size(); i += 3) {
                    Location mLoc = magmaFormations.get(i).entity().getLocation();
                    double dx = center.getX() - mLoc.getX();
                    double dz = center.getZ() - mLoc.getZ();
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, mLoc, 1,
                            dx * 0.1, 0.1, dz * 0.1, 0.01);
                }
            }

            // Lava dripping from cracked glass (crystal bleeding)
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : glassCube) {
                    w.spawnParticle(Particle.LAVA, h.entity().getLocation(), 1, 0.1, 0, 0.1, 0);
                }
            }

            // Dripping lava down pillar from magma
            if (ticksAlive % 7 == 0) {
                int pillarIdx = (ticksAlive / 7) % pillar.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, pillar.get(pillarIdx).entity().getLocation(),
                        1, 0.1, 0, 0.1, 0);
            }

            // Conspicuously NO FLAME — extinguished quality

            // Quiet End sounds
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.3f);
            }
            if (ticksAlive % 400 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastLightOfTheEnd(plugin); }
    }
}
