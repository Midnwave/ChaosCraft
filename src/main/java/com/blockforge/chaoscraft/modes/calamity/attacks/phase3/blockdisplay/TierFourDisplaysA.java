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
 * Phase 4C Block Display -- TIER 5: "THE BURN" (20% HP)
 * Structures 71-80. The island is almost gone. Every structure is minimum
 * 30 blocks. Maximum intensity brimstone apocalypse.
 *
 * Dweller palette: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255)
 * Rules: NO status effects, AxisAngle4f ONLY, damage 6.0-10.0 HP
 */
public final class TierFourDisplaysA {

    private TierFourDisplaysA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TheConsumingCrown(plugin));
        registry.register(new MagmaRiverDelta(plugin));
        registry.register(new TheDwellersStride(plugin));
        registry.register(new ObsidianCageDescent(plugin));
        registry.register(new BrimstoneEruptionMantle(plugin));
        registry.register(new SoulFlameCathedral(plugin));
        registry.register(new PetrifiedTitanHand(plugin));
        registry.register(new SpiralInferno(plugin));
        registry.register(new NetherSkyFragment(plugin));
        registry.register(new LastPillarOfTheEnd(plugin));
    }

    // ================================================================
    // 71. THE CONSUMING CROWN — Massive 22-diameter crown with 8 primary
    //     prongs, secondary prongs, interior chandelier, nether star center
    // ================================================================
    public static class TheConsumingCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseRing = new ArrayList<>();
        private final List<BlockDisplayHandle> primaryProngs = new ArrayList<>();
        private final List<BlockDisplayHandle> secondaryProngs = new ArrayList<>();
        private final List<BlockDisplayHandle> chandelierBlocks = new ArrayList<>();
        private BlockDisplayHandle starCenter;

        public TheConsumingCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("consuming_crown", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(11.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
            config.setImpactDamage(8.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base ring: 36 netherrack at radius 11, 2 blocks tall
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2 * i) / 36;
                for (int y = 0; y < 2; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 11, y, Math.sin(angle) * 11), Material.NETHERRACK);
                    h.scale(1.0f, 1.0f, 1.0f).glow(120, 60, 40).interpolation(3, 0);
                    baseRing.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 8 primary prongs at intervals around the ring
            for (int p = 0; p < 8; p++) {
                double angle = (Math.PI * 2 * p) / 8;
                // Each prong: magma base (2), basalt mid (3), cracked stone upper (2), polished cap (1)
                Material[] prongMats = {Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.BASALT,
                        Material.BASALT, Material.BASALT, Material.CRACKED_STONE_BRICKS,
                        Material.CRACKED_STONE_BRICKS, Material.POLISHED_BLACKSTONE};
                for (int y = 0; y < 8; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 11, 2 + y, Math.sin(angle) * 11), prongMats[y]);
                    h.scale(0.8f, 1.0f, 0.8f).glow(y < 2 ? 255 : 80, y < 2 ? 100 : 80, y < 2 ? 0 : 90)
                            .interpolation(3, 0);
                    primaryProngs.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Fire charge cap (magma glow block)
                BlockDisplayHandle cap = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 11, 10, Math.sin(angle) * 11), Material.MAGMA_BLOCK);
                cap.scale(0.7f, 0.7f, 0.7f).glow(255, 100, 0).interpolation(3, 0);
                primaryProngs.add(cap);
                spawnedEntities.add(cap.entity());
            }

            // 8 secondary prongs between primary ones (shorter, 5 blocks)
            for (int p = 0; p < 8; p++) {
                double angle = (Math.PI * 2 * p) / 8 + Math.PI / 8;
                Material[] secMats = {Material.NETHERRACK, Material.NETHERRACK, Material.BLACKSTONE,
                        Material.BLACKSTONE, Material.NETHERRACK};
                for (int y = 0; y < 5; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 11, 2 + y, Math.sin(angle) * 11), secMats[y]);
                    h.scale(0.6f, 1.0f, 0.6f).glow(100, 50, 30).interpolation(2, 0);
                    secondaryProngs.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Interior chandelier: 16 hanging elements
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double r = 8.0 + (i % 3) * 0.5;
                int hangLen = 3 + (i % 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * r, 10 - hangLen, Math.sin(angle) * r), Material.NETHERRACK);
                h.scale(0.15f, (float) hangLen * 0.4f, 0.15f).glow(255, 100, 0).interpolation(3, 0);
                chandelierBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Nether star center (glowstone stand-in)
            starCenter = displayBuilder.spawnBlock(center.clone().add(0, 7, 0), Material.GLOWSTONE);
            starCenter.scale(1.0f, 1.0f, 1.0f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(starCenter.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crown Y-rotation: 0.4°/tick
            float crownRot = ticksAlive * 0.00698f;
            for (BlockDisplayHandle h : baseRing) {
                h.rotate(crownRot, 0, 1, 0);
                h.interpolation(4, 0);
            }
            for (BlockDisplayHandle h : primaryProngs) {
                h.rotate(crownRot, 0, 1, 0);
                h.interpolation(4, 0);
            }
            for (BlockDisplayHandle h : secondaryProngs) {
                h.rotate(crownRot, 0, 1, 0);
                h.interpolation(4, 0);
            }

            // Nether star tumble: combined axis rotation
            float starRot = ticksAlive * 0.01745f;
            if (starCenter != null) {
                starCenter.rotate(starRot, 0.5f, 1.0f, 0.3f);
                starCenter.interpolation(3, 0);
            }

            // Base ring vertical bob: 0.4 blocks on 90-tick cycle
            float baseBob = 0.4f * (float) Math.sin(ticksAlive * Math.PI / 45);

            // Chandelier sway: 12° pendulum, 60-tick cycle
            float chandelierSwing = 0.21f * (float) Math.sin(ticksAlive * Math.PI / 30);
            for (BlockDisplayHandle h : chandelierBlocks) {
                h.rotate(chandelierSwing + crownRot, 1, 0, 0);
                h.interpolation(3, 0);
            }

            // Lava burst from prong tips every 60 ticks
            if (ticksAlive % 60 == 0) {
                for (int p = 0; p < 8; p++) {
                    double angle = (Math.PI * 2 * p) / 8;
                    Location tipLoc = center.clone().add(Math.cos(angle) * 11, 10, Math.sin(angle) * 11);
                    w.spawnParticle(Particle.LAVA, tipLoc, 4, 0.5, 1, 0.5, 0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.6f);
            }

            // Flame from interior
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 7, 0), 4, 3, 1, 3, 0.02);
            }

            // Soul fire column from nether star down
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 5, 0), 3,
                        0.5, 1, 0.5, 0.02);
            }

            // Smoke from base ring
            if (ticksAlive % 6 == 0) {
                double sAngle = Math.random() * Math.PI * 2;
                w.spawnParticle(Particle.SMOKE,
                        center.clone().add(Math.cos(sAngle) * 11, 1, Math.sin(sAngle) * 11),
                        3, 0.3, 0.3, 0.3, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheConsumingCrown(plugin); }
    }

    // ================================================================
    // 72. MAGMA RIVER DELTA — Branching river delta at ground level
    //     with flowing lava pulse wave and basalt origin mound
    // ================================================================
    public static class MagmaRiverDelta extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mainChannel = new ArrayList<>();
        private final List<BlockDisplayHandle> tributaries = new ArrayList<>();
        private final List<BlockDisplayHandle> lavaSurface = new ArrayList<>();
        private final List<BlockDisplayHandle> originMound = new ArrayList<>();
        private final List<BlockDisplayHandle> obsidianPosts = new ArrayList<>();

        public MagmaRiverDelta(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_river_delta", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(500);
            config.setImpactDamage(6.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main channel: 30 blocks long, 3 wide
            for (int x = 0; x < 30; x++) {
                for (int z = -1; z <= 1; z++) {
                    Material mat = (x + z) % 2 == 0 ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x - 15, 0, z), mat);
                    h.scale(1.0f, 0.5f, 1.0f).glow(200, 80, 30).interpolation(2, 0);
                    mainChannel.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Lava surface: orange stained glass along channel
            for (int x = 0; x < 30; x += 2) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x - 15, 0.5, 0), Material.ORANGE_STAINED_GLASS);
                h.scale(1.8f, 0.15f, 2.5f).glow(255, 150, 0).interpolation(3, 0);
                lavaSurface.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 tributaries branching at 30° angles from midpoint
            double[] branchAngles = {0.52, -0.52, 0.26, -0.26};
            for (int t = 0; t < 4; t++) {
                double branchAngle = branchAngles[t];
                for (int seg = 0; seg < 8; seg++) {
                    double bx = Math.cos(branchAngle) * seg;
                    double bz = Math.sin(branchAngle) * seg;
                    Material mat = seg % 2 == 0 ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(bx, 0, bz + (t < 2 ? 2 : -2)), mat);
                    h.scale(0.8f, 0.5f, 0.8f).glow(200, 80, 30).interpolation(2, 0);
                    tributaries.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Fan at tributary tip
                for (int fx = -2; fx <= 2; fx++) {
                    double fanX = Math.cos(branchAngle) * 8 + fx;
                    double fanZ = Math.sin(branchAngle) * 8 + (t < 2 ? 2 : -2);
                    Material mat = fx % 2 == 0 ? Material.BLACKSTONE : Material.NETHERRACK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(fanX, 0, fanZ), mat);
                    h.scale(1.0f, 0.5f, 1.0f).glow(60, 60, 70).interpolation(2, 0);
                    tributaries.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Origin mound: 3x3x2 basalt
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, y, z), Material.BASALT);
                        h.scale(1.0f, 1.0f, 1.0f).glow(70, 70, 80).interpolation(2, 0);
                        originMound.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // 3 obsidian posts at origin
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 0.8, y + 2, Math.sin(angle) * 0.8), Material.OBSIDIAN);
                    h.scale(0.4f, 1.0f, 0.4f).glow(30, 0, 40).interpolation(2, 0);
                    obsidianPosts.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Channel walls: blackstone banks
            for (int x = 0; x < 30; x += 3) {
                for (int side = -1; side <= 1; side += 2) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x - 15, 0.5, side * 2), Material.BLACKSTONE);
                    h.scale(1.0f, 0.8f, 0.5f).glow(40, 40, 50).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Flowing wave: lava surface blocks pulse traveling from origin to tips
            for (int i = 0; i < lavaSurface.size(); i++) {
                float wavePhase = (ticksAlive - i * 10) % 150;
                float waveScale = 1.0f + 0.06f * (float) Math.sin(wavePhase * Math.PI / 75);
                lavaSurface.get(i).scale(1.8f * waveScale, 0.15f, 2.5f * waveScale);
                lavaSurface.get(i).interpolation(3, 0);
            }

            // Y-rotation: 0.1°/tick
            float deltaRot = ticksAlive * 0.00175f;

            // Obsidian post rotation: 0.6°/tick
            float postRot = ticksAlive * 0.01047f;
            for (BlockDisplayHandle h : obsidianPosts) {
                h.rotate(postRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Channel wall breathing: 0.05 blocks
            // (visual via scale interpolation)

            // Lava particles flowing along channels
            if (ticksAlive % 3 == 0) {
                int pos = (ticksAlive / 3) % 30;
                w.spawnParticle(Particle.LAVA, center.clone().add(pos - 15, 0.7, 0), 1, 0.3, 0.1, 0.3, 0);
            }

            // Dripping lava from channel edges
            if (ticksAlive % 5 == 0) {
                int idx = (ticksAlive / 5) % mainChannel.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, mainChannel.get(idx).entity().getLocation(), 1,
                        0.1, 0, 0.1, 0);
            }

            // Flame from origin
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 2.5, 0), 3, 0.3, 0.5, 0.3, 0.02);
            }

            // Smoke from tributary fans
            if (ticksAlive % 8 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(8, 0.5, 2), 2, 1, 0.2, 1, 0.01);
                w.spawnParticle(Particle.SMOKE, center.clone().add(8, 0.5, -2), 2, 1, 0.2, 1, 0.01);
            }

            // Tributary tip lava burst every 30 ticks
            if (ticksAlive % 30 == 0) {
                triggerImpactDamage(center);
            }

            // Sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaRiverDelta(plugin); }
    }

    // ================================================================
    // 73. THE DWELLER'S STRIDE — Pair of massive sunken footprints
    //     with soul fire columns and stride trail
    // ================================================================
    public static class TheDwellersStride extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftFoot = new ArrayList<>();
        private final List<BlockDisplayHandle> rightFoot = new ArrayList<>();
        private final List<BlockDisplayHandle> trailBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ballDisplays = new ArrayList<>();
        private final List<BlockDisplayHandle> heelDisplays = new ArrayList<>();

        public TheDwellersStride(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dwellers_stride", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left footprint: 8x5 oval of cracked stone brick border + soul soil fill
            buildFootprint(center.clone().add(-3, 0, 0), leftFoot, ballDisplays, heelDisplays);
            buildFootprint(center.clone().add(3, 0, 8), rightFoot, ballDisplays, heelDisplays);

            // Trail: 12 magma blocks between footprints
            for (int i = 0; i < 12; i++) {
                double tx = -3 + (6.0 * i / 12);
                double tz = i * 0.67;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(tx, 0, tz), Material.MAGMA_BLOCK);
                h.scale(0.8f, 0.3f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                trailBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.7f, 0.3f);
        }

        private void buildFootprint(Location footCenter, List<BlockDisplayHandle> footBlocks,
                                     List<BlockDisplayHandle> ballDisps, List<BlockDisplayHandle> heelDisps) {
            // Outer border: cracked stone brick oval
            for (int i = 0; i < 22; i++) {
                double angle = (Math.PI * 2 * i) / 22;
                double rx = 4.0, rz = 2.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        footCenter.clone().add(Math.cos(angle) * rx, -0.3, Math.sin(angle) * rz),
                        Material.CRACKED_STONE_BRICKS);
                h.scale(0.6f, 0.4f, 0.6f).glow(70, 70, 80).interpolation(2, 0);
                footBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior fill: soul soil
            for (int x = -3; x <= 3; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (x * x / 16.0 + z * z / 6.25 > 1) continue; // Inside ellipse
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            footCenter.clone().add(x, -0.3, z), Material.SOUL_SOIL);
                    h.scale(0.9f, 0.3f, 0.9f).glow(0, 150, 255).interpolation(2, 0);
                    footBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Ball of foot: magma cream (magma block) at front
            BlockDisplayHandle ball = displayBuilder.spawnBlock(
                    footCenter.clone().add(2.5, -0.1, 0), Material.MAGMA_BLOCK);
            ball.scale(0.8f, 0.5f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
            ballDisps.add(ball);
            spawnedEntities.add(ball.entity());

            // Heel: fire charge (magma block) at back
            BlockDisplayHandle heel = displayBuilder.spawnBlock(
                    footCenter.clone().add(-3, -0.1, 0), Material.MAGMA_BLOCK);
            heel.scale(0.6f, 0.5f, 0.6f).glow(255, 100, 0).interpolation(3, 0);
            heelDisps.add(heel);
            spawnedEntities.add(heel.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Footprints deepen: 0.008 blocks/tick for 100 ticks
            if (ticksAlive <= 100) {
                float sink = ticksAlive * 0.008f;
                for (BlockDisplayHandle h : leftFoot) {
                    Location loc = h.entity().getLocation();
                    loc.add(0, -0.008, 0);
                    h.entity().teleport(loc);
                }
                for (BlockDisplayHandle h : rightFoot) {
                    Location loc = h.entity().getLocation();
                    loc.add(0, -0.008, 0);
                    h.entity().teleport(loc);
                }
            }

            // Ball pulse: 80-tick sine, scale 0.8 to 1.0
            float ballScale = 0.8f + 0.2f * (float) Math.sin(ticksAlive * Math.PI / 40);
            for (BlockDisplayHandle ball : ballDisplays) {
                ball.scale(ballScale, 0.5f, ballScale);
                ball.interpolation(3, 0);
            }

            // Heel Y-rotation: 1.0°/tick
            float heelRot = ticksAlive * 0.01745f;
            for (BlockDisplayHandle heel : heelDisplays) {
                heel.rotate(heelRot, 0, 1, 0);
                heel.interpolation(3, 0);
            }

            // Trail illumination: staggered 10-tick pulse
            for (int i = 0; i < trailBlocks.size(); i++) {
                int phase = (ticksAlive - i * 10) % 120;
                float tScale = phase < 20 ? 0.8f + 0.4f * (phase / 20.0f) : 0.8f;
                trailBlocks.get(i).scale(tScale, 0.3f, tScale);
                trailBlocks.get(i).interpolation(3, 0);
            }

            // Soul fire flame from soul soil
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < leftFoot.size(); i += 4) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, leftFoot.get(i).entity().getLocation().add(0, 0.3, 0),
                            1, 0.05, 0.2, 0.05, 0.01);
                }
                for (int i = 0; i < rightFoot.size(); i += 4) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, rightFoot.get(i).entity().getLocation().add(0, 0.3, 0),
                            1, 0.05, 0.2, 0.05, 0.01);
                }
            }

            // Dripping lava from borders
            if (ticksAlive % 8 == 0) {
                int idx = (ticksAlive / 8) % 22;
                if (idx < leftFoot.size()) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, leftFoot.get(idx).entity().getLocation(),
                            1, 0.1, 0, 0.1, 0);
                }
            }

            // Lava bursts from trail
            if (ticksAlive % 6 == 0) {
                int tidx = (ticksAlive / 6) % trailBlocks.size();
                w.spawnParticle(Particle.LAVA, trailBlocks.get(tidx).entity().getLocation(),
                        2, 0.2, 0.3, 0.2, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheDwellersStride(plugin); }
    }

    // ================================================================
    // 74. OBSIDIAN CAGE DESCENT — 9x9x10 cage that descends from above
    //     with independently rotating bar rings and crying obsidian drip
    // ================================================================
    public static class ObsidianCageDescent extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cornerColumns = new ArrayList<>();
        private final List<BlockDisplayHandle> barRingBot = new ArrayList<>();
        private final List<BlockDisplayHandle> barRingMid = new ArrayList<>();
        private final List<BlockDisplayHandle> barRingTop = new ArrayList<>();
        private final List<BlockDisplayHandle> wallFill = new ArrayList<>();
        private float cageY = 15.0f;

        public ObsidianCageDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_cage_descent", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.5);
            config.setDamageOnImpactOnly(false);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 corner columns of obsidian, each 10 blocks tall (3-wide profile)
            double[][] corners = {{-4, 0, -4}, {4, 0, -4}, {-4, 0, 4}, {4, 0, 4}};
            for (double[] corner : corners) {
                for (int y = 0; y < 10; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(corner[0], y + 15, corner[2]), Material.OBSIDIAN);
                    h.scale(1.2f, 1.0f, 1.2f).glow(30, 0, 40).interpolation(3, 0);
                    cornerColumns.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Horizontal bar rings at heights 3, 6, 10 (crying obsidian)
            int[] barHeights = {3, 6, 10};
            List<List<BlockDisplayHandle>> barLists = List.of(barRingBot, barRingMid, barRingTop);
            for (int b = 0; b < 3; b++) {
                int bh = barHeights[b];
                // Connect corners with bars
                double[][] barPositions = {
                        {-3, 0, -4}, {-1, 0, -4}, {1, 0, -4}, {3, 0, -4},
                        {-3, 0, 4}, {-1, 0, 4}, {1, 0, 4}, {3, 0, 4},
                        {-4, 0, -3}, {-4, 0, -1}, {-4, 0, 1}, {-4, 0, 3},
                        {4, 0, -3}, {4, 0, -1}, {4, 0, 1}, {4, 0, 3}
                };
                for (double[] bp : barPositions) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(bp[0], bh + 15, bp[2]), Material.CRYING_OBSIDIAN);
                    h.scale(0.8f, 0.5f, 0.8f).glow(80, 0, 120).interpolation(3, 0);
                    barLists.get(b).add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Lower wall fill: netherrack at bottom 2 rows of each face
            for (int y = 0; y < 2; y++) {
                for (int i = -3; i <= 3; i++) {
                    // North and south faces
                    for (int face : new int[]{-4, 4}) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(i, y + 15, face), Material.NETHERRACK);
                        h.scale(0.9f, 1.0f, 0.3f).glow(120, 60, 40).interpolation(2, 0);
                        wallFill.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Corner caps
            for (double[] corner : corners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(corner[0], 10 + 15, corner[2]), Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.5f, 1.0f).glow(50, 50, 60).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Flanking basalt
            double[][] flanks = {{-5, 0, 0}, {5, 0, 0}, {0, 0, -5}, {0, 0, 5}};
            for (double[] flank : flanks) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(flank[0], 15, flank[2]), Material.BASALT);
                h.scale(0.8f, 1.0f, 0.8f).glow(70, 70, 80)
                        .rotate(0.785f, 0, 1, 0).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent: 0.06 blocks/tick from 15 to 0.5, then slam
            if (cageY > 0.5f) {
                cageY -= 0.06f;
            } else if (cageY > -0.5f) {
                // Slam
                cageY = 0;
                if (ticksAlive > 50) {
                    triggerImpactDamage(center);
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    w.spawnParticle(Particle.SMOKE, center, 30, 4, 1, 4, 0.1);
                }
            }

            // Move all cage parts down
            float yDelta = -0.06f;
            for (BlockDisplayHandle h : cornerColumns) {
                Location loc = h.entity().getLocation();
                loc.add(0, yDelta, 0);
                h.entity().teleport(loc);
            }
            for (BlockDisplayHandle h : barRingBot) {
                Location loc = h.entity().getLocation();
                loc.add(0, yDelta, 0);
                h.entity().teleport(loc);
            }
            for (BlockDisplayHandle h : barRingMid) {
                Location loc = h.entity().getLocation();
                loc.add(0, yDelta, 0);
                h.entity().teleport(loc);
            }
            for (BlockDisplayHandle h : barRingTop) {
                Location loc = h.entity().getLocation();
                loc.add(0, yDelta, 0);
                h.entity().teleport(loc);
            }
            for (BlockDisplayHandle h : wallFill) {
                Location loc = h.entity().getLocation();
                loc.add(0, yDelta, 0);
                h.entity().teleport(loc);
            }

            // Y-rotation: 0.3°/tick while descending
            float cageRot = ticksAlive * 0.00524f;

            // Independent bar ring rotations
            float botRot = ticksAlive * 0.01396f;  // 0.8°/tick
            float midRot = ticksAlive * 0.00873f;  // 0.5°/tick
            float topRot = ticksAlive * 0.00524f;  // 0.3°/tick
            for (BlockDisplayHandle h : barRingBot) {
                h.rotate(botRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : barRingMid) {
                h.rotate(midRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : barRingTop) {
                h.rotate(topRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Crying obsidian drip
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < barRingBot.size(); i += 4) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, barRingBot.get(i).entity().getLocation(),
                            1, 0.1, 0, 0.1, 0);
                }
            }

            // Smoke in cage interior when landed
            if (cageY <= 0.5f && ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 3, 0), 5, 3, 3, 3, 0.01);
            }

            // Ash trailing as cage falls
            if (cageY > 1 && ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.ASH, center.clone().add(0, cageY + 10, 0), 3, 2, 1, 2, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianCageDescent(plugin); }
    }

    // ================================================================
    // 75. BRIMSTONE ERUPTION MANTLE — 15x15 floor-covering eruption
    //     with magma clusters and geyser spikes
    // ================================================================
    public static class BrimstoneEruptionMantle extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> floorBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> magmaClusters = new ArrayList<>();
        private final List<BlockDisplayHandle> eruptionSpikes = new ArrayList<>();
        private final List<BlockDisplayHandle> wallSegments = new ArrayList<>();

        public BrimstoneEruptionMantle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_eruption_mantle", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(1100);
            config.setCooldownTicks(500);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base floor: netherrack covering 15x15 (irregular border, sample subset)
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    // Skip some edges for irregular border
                    if (Math.abs(x) == 7 && Math.abs(z) == 7) continue;
                    if (Math.abs(x) == 7 && Math.abs(z) > 5) continue;
                    float yOff = (float) (Math.random() * 0.5 - 0.2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, yOff, z), Material.NETHERRACK);
                    h.scale(1.0f, 0.5f, 1.0f).glow(120, 60, 40).interpolation(2, 0);
                    floorBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Magma clusters: 6 groupings of 5-7 blocks each
            double[][] clusterCenters = {{-3, 0, -3}, {3, 0, 2}, {-2, 0, 4}, {4, 0, -4}, {0, 0, 0}, {-5, 0, 1}};
            for (double[] cc : clusterCenters) {
                int clusterSize = 5 + (int) (Math.random() * 3);
                for (int i = 0; i < clusterSize; i++) {
                    double ox = cc[0] + (Math.random() - 0.5) * 2;
                    double oz = cc[2] + (Math.random() - 0.5) * 2;
                    float yOff = 0.5f + (float) (Math.random() * 1.0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(ox, yOff, oz), Material.MAGMA_BLOCK);
                    h.scale(0.8f, 0.6f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                    magmaClusters.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Eruption spikes: 8 blackstone spikes of 3-5 blocks each
            double[][] spikePositions = {{-5, 0, -5}, {5, 0, 5}, {-4, 0, 3}, {4, 0, -3},
                    {0, 0, -6}, {0, 0, 6}, {-6, 0, 0}, {6, 0, 0}};
            for (double[] sp : spikePositions) {
                int spikeH = 3 + (int) (Math.random() * 3);
                for (int y = 0; y < spikeH; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(sp[0], y, sp[2]), Material.BLACKSTONE);
                    h.scale(0.5f, 1.0f, 0.5f).glow(40, 40, 50).interpolation(3, 0);
                    eruptionSpikes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Perimeter wall segments
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + Math.PI / 4;
                for (int seg = 0; seg < 3; seg++) {
                    double wx = Math.cos(angle) * 7 + seg - 1;
                    double wz = Math.sin(angle) * 7;
                    for (int y = 0; y < 2 + (int) (Math.random() * 2); y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(wx, y, wz), Material.BASALT);
                        h.scale(1.0f, 1.0f, 0.6f).glow(70, 70, 80).interpolation(2, 0);
                        wallSegments.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // Cracked stone brick caps
                BlockDisplayHandle cap = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 7, 3, Math.sin(angle) * 7),
                        Material.CRACKED_STONE_BRICKS);
                cap.scale(1.5f, 0.4f, 0.6f).glow(70, 70, 80).interpolation(2, 0);
                spawnedEntities.add(cap.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Mantle breathing: 120-tick sine, 0.3 blocks amplitude
            float breathe = 0.3f * (float) Math.sin(ticksAlive * Math.PI / 60);

            // Geyser eruption: random spike every 60 ticks
            if (ticksAlive % 60 == 0 && !eruptionSpikes.isEmpty()) {
                int spikeIdx = (ticksAlive / 60) % (eruptionSpikes.size() / 3);
                int startIdx = spikeIdx * 3;
                if (startIdx < eruptionSpikes.size()) {
                    Location spikeLoc = eruptionSpikes.get(startIdx).entity().getLocation();
                    // Scale burst
                    for (int i = startIdx; i < Math.min(startIdx + 3, eruptionSpikes.size()); i++) {
                        eruptionSpikes.get(i).scale(0.7f, 1.5f, 0.7f);
                        eruptionSpikes.get(i).interpolation(5, 0);
                    }
                    triggerImpactDamage(spikeLoc);
                    w.spawnParticle(Particle.LAVA, spikeLoc.add(0, 3, 0), 15, 1, 2, 1, 0.1);
                    DisplayBuilder.playSound(spikeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.4f);
                }
            }

            // Lava from magma clusters
            if (ticksAlive % 4 == 0) {
                int idx = (ticksAlive / 4) % magmaClusters.size();
                w.spawnParticle(Particle.LAVA, magmaClusters.get(idx).entity().getLocation().add(0, 0.5, 0),
                        2, 0.3, 0.5, 0.3, 0);
            }

            // Flame across mantle surface
            if (ticksAlive % 3 == 0) {
                double fx = (Math.random() - 0.5) * 14;
                double fz = (Math.random() - 0.5) * 14;
                w.spawnParticle(Particle.FLAME, center.clone().add(fx, 0.5, fz), 2, 0.3, 0.2, 0.3, 0.01);
            }

            // Smoke from wall segments
            if (ticksAlive % 8 == 0 && !wallSegments.isEmpty()) {
                int wIdx = (ticksAlive / 8) % wallSegments.size();
                w.spawnParticle(Particle.SMOKE, wallSegments.get(wIdx).entity().getLocation().add(0, 1, 0),
                        2, 0.2, 0.3, 0.2, 0.01);
            }

            // Wall tilt outward: 5° over 150 ticks
            float wallTilt = 0.087f * (float)(ticksAlive / 150.0);
            if (wallTilt > 0.087f) wallTilt = 0.087f;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneEruptionMantle(plugin); }
    }

    // ================================================================
    // 76. SOUL FLAME CATHEDRAL — Arched ruined nave with soul soil
    //     runner and blaze rod stained-glass analog windows
    // ================================================================
    public static class SoulFlameCathedral extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftRib = new ArrayList<>();
        private final List<BlockDisplayHandle> rightRib = new ArrayList<>();
        private final List<BlockDisplayHandle> keystoneBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> soulRunner = new ArrayList<>();
        private final List<BlockDisplayHandle> windowBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> obsidianPosts = new ArrayList<>();
        private final List<BlockDisplayHandle> buttresses = new ArrayList<>();

        public SoulFlameCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_flame_cathedral", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
            config.setImpactDamage(8.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left rib: 14 basalt blocks following arch curve
            for (int i = 0; i < 14; i++) {
                double t = (double) i / 13;
                double x = -5 + t * 5; // From -5 to 0
                double y = 14 * Math.sin(t * Math.PI); // Arch curve
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, 0), Material.BASALT);
                h.scale(0.8f, 0.8f, 0.8f).glow(70, 70, 80).interpolation(3, 0);
                leftRib.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right rib: 14 smooth basalt mirrored
            for (int i = 0; i < 14; i++) {
                double t = (double) i / 13;
                double x = 5 - t * 5;
                double y = 14 * Math.sin(t * Math.PI);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, 0), Material.SMOOTH_BASALT);
                h.scale(0.8f, 0.8f, 0.8f).glow(70, 70, 80).interpolation(3, 0);
                rightRib.add(h);
                spawnedEntities.add(h.entity());
            }

            // Keystone: 3 polished blackstone at apex
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(i * 0.5, 15, 0), Material.POLISHED_BLACKSTONE);
                h.scale(0.6f, 0.6f, 0.6f).glow(50, 50, 60).interpolation(2, 0);
                keystoneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Soul soil runner: 12 blocks along the nave floor
            for (int z = -6; z <= 5; z++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, 0, z), Material.SOUL_SOIL);
                h.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 255).interpolation(2, 0);
                soulRunner.add(h);
                spawnedEntities.add(h.entity());
            }

            // Obsidian column posts along runner
            for (int z = -5; z <= 5; z += 2) {
                for (int y = 0; y < 2; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(0, y, z), Material.OBSIDIAN);
                    h.scale(0.3f, 1.0f, 0.3f).glow(30, 0, 40).interpolation(2, 0);
                    obsidianPosts.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Window displays: blaze rod analogs along inner rib faces
            Material[] windowBacking = {Material.ORANGE_STAINED_GLASS, Material.RED_STAINED_GLASS};
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 4; i++) {
                    double t = 0.2 + i * 0.2;
                    double x = side * (5 - t * 5);
                    double y = 14 * Math.sin(t * Math.PI);
                    // Blaze rod analog
                    BlockDisplayHandle rod = displayBuilder.spawnBlock(
                            center.clone().add(x * 0.8, y, 0), Material.NETHERRACK);
                    rod.scale(0.15f, 0.6f, 0.15f).glow(255, 100, 0).interpolation(3, 0);
                    windowBlocks.add(rod);
                    spawnedEntities.add(rod.entity());
                    // Backing glass
                    BlockDisplayHandle glass = displayBuilder.spawnBlock(
                            center.clone().add(x * 0.8, y, -0.2 * side), windowBacking[i % 2]);
                    glass.scale(0.4f, 0.5f, 0.15f).glow(255, 150, 0).interpolation(2, 0);
                    spawnedEntities.add(glass.entity());
                }
            }

            // Buttresses at rib bases
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 4; i++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(side * 5.5, i * 0.5, (i - 2) * 0.3), Material.NETHERRACK);
                    h.scale(0.7f, 0.5f, 0.5f).glow(120, 60, 40)
                            .rotate(0.785f * side, 0, 0, 1).interpolation(2, 0);
                    buttresses.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rib sway: apex translates 0.3 blocks inward over 150 ticks
            float sway = 0.3f * (float) Math.sin(ticksAlive * Math.PI / 75);

            // Window blaze rods X-rotation: 0.4°/tick
            float windowRot = ticksAlive * 0.00698f;
            for (BlockDisplayHandle wd : windowBlocks) {
                wd.rotate(windowRot, 1, 0, 0);
                wd.interpolation(3, 0);
            }

            // Soul runner traveling pulse: 1 block per 15 ticks
            for (int i = 0; i < soulRunner.size(); i++) {
                float pulse = (ticksAlive - i * 15) % (soulRunner.size() * 15);
                float sScale = pulse < 20 ? 1.0f + 0.08f * (pulse / 20.0f) : 1.0f;
                soulRunner.get(i).scale(sScale, 0.3f, sScale);
                soulRunner.get(i).interpolation(3, 0);
            }

            // Obsidian post rotation: 0.5°/tick
            float postRot = ticksAlive * 0.00873f;
            for (BlockDisplayHandle h : obsidianPosts) {
                h.rotate(postRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Soul fire flame from runner
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < soulRunner.size(); i += 2) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, soulRunner.get(i).entity().getLocation().add(0, 0.5, 0),
                            2, 0.05, 0.3, 0.05, 0.01);
                }
            }

            // Flame from windows into nave
            if (ticksAlive % 6 == 0) {
                for (BlockDisplayHandle wd : windowBlocks) {
                    w.spawnParticle(Particle.FLAME, wd.entity().getLocation(), 1, 0.1, 0.1, 0.3, 0.02);
                }
            }

            // Smoke at apex
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 14, 0), 2, 0.5, 0.5, 0.5, 0.01);
            }

            // Apex convergent flame burst on max sway
            if (ticksAlive % 150 == 75) {
                triggerImpactDamage(center.clone().add(0, 14, 0));
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 14, 0), 25, 2, 1, 2, 0.1);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.5f);
            }

            // Sound
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFlameCathedral(plugin); }
    }

    // ================================================================
    // 77. THE PETRIFIED TITAN HAND — 20-block-tall severed hand rising
    //     from the ground with finger oscillation and grasp cycles
    // ================================================================
    public static class PetrifiedTitanHand extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> palmBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> fingers = new ArrayList<>();
        private final List<BlockDisplayHandle> wristRing = new ArrayList<>();
        private boolean risen = false;

        public PetrifiedTitanHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("petrified_titan_hand", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(500);
            config.setImpactDamage(10.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Palm: 7x7 slab (simplified) of basalt and blackstone, underground
            for (int x = -3; x <= 3; x++) {
                for (int z = -1; z <= 1; z++) {
                    Material mat = (x + z) % 2 == 0 ? Material.BASALT : Material.BLACKSTONE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, -20, z), mat);
                    h.scale(1.0f, 1.5f, 1.0f).glow(70, 70, 80).interpolation(3, 0);
                    palmBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 5 fingers: thumb(8), index(14), middle(18), ring(14), pinky(10)
            int[] fingerHeights = {8, 14, 18, 14, 10};
            double[] fingerX = {-3.5, -1.5, 0, 1.5, 3};
            for (int f = 0; f < 5; f++) {
                List<BlockDisplayHandle> finger = new ArrayList<>();
                int fh = fingerHeights[f];
                for (int y = 0; y < fh; y++) {
                    Material mat;
                    if (y % 3 == 0) mat = Material.MAGMA_BLOCK; // Knuckle joints
                    else if (y % 2 == 0) mat = Material.NETHERRACK;
                    else mat = Material.CRACKED_STONE_BRICKS;

                    float xOff = (float) fingerX[f];
                    if (f == 0) xOff -= y * 0.15f; // Thumb angles outward
                    if (f == 4) xOff += y * 0.1f; // Pinky angles outward

                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(xOff, -20 + 4 + y, 0), mat);
                    float fScale = f == 0 || f == 4 ? 0.6f : 0.8f;
                    h.scale(fScale, 1.0f, fScale).glow(y % 3 == 0 ? 255 : 100, y % 3 == 0 ? 100 : 60, y % 3 == 0 ? 0 : 40)
                            .interpolation(3, 0);
                    finger.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Fingertip: polished blackstone
                BlockDisplayHandle tip = displayBuilder.spawnBlock(
                        center.clone().add(fingerX[f], -20 + 4 + fh, 0), Material.POLISHED_BLACKSTONE);
                tip.scale(0.5f, 0.5f, 0.5f).glow(50, 50, 60).interpolation(2, 0);
                finger.add(tip);
                spawnedEntities.add(tip.entity());
                fingers.add(finger);
            }

            // Wrist ring: 12 soul soil at ground level
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 4, -0.5, Math.sin(angle) * 1.5), Material.SOUL_SOIL);
                h.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 255).interpolation(2, 0);
                wristRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Torn tendons below palm
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 2, -21, Math.sin(angle) * 0.8),
                        Material.CRACKED_STONE_BRICKS);
                h.scale(0.5f, 0.8f, 0.5f).glow(70, 70, 80)
                        .rotate(-0.35f, 1, 0, 0).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Eruption (first 60 ticks) — rise 20 blocks
            if (ticksAlive <= 60) {
                float rise = (ticksAlive / 60.0f) * 20.0f;
                float yDelta = 20.0f / 60.0f;
                for (BlockDisplayHandle h : palmBlocks) {
                    Location loc = h.entity().getLocation();
                    loc.add(0, yDelta, 0);
                    h.entity().teleport(loc);
                }
                for (List<BlockDisplayHandle> finger : fingers) {
                    for (BlockDisplayHandle h : finger) {
                        Location loc = h.entity().getLocation();
                        loc.add(0, yDelta, 0);
                        h.entity().teleport(loc);
                    }
                }
                if (ticksAlive == 1) {
                    triggerImpactDamage(center);
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.4f);
                }
                return;
            }

            // Phase 2: Animate fingers
            // Middle finger sway: 3° on Z-axis, 70-tick cycle
            float midSway = 0.052f * (float) Math.sin(ticksAlive * Math.PI / 35);
            // Index leads by 10 ticks, ring lags by 10
            float indexSway = 0.052f * (float) Math.sin((ticksAlive + 10) * Math.PI / 35);
            float ringSway = 0.052f * (float) Math.sin((ticksAlive - 10) * Math.PI / 35);

            if (fingers.size() >= 5) {
                for (BlockDisplayHandle h : fingers.get(1)) { // index
                    h.rotate(indexSway, 0, 0, 1);
                    h.interpolation(3, 0);
                }
                for (BlockDisplayHandle h : fingers.get(2)) { // middle
                    h.rotate(midSway, 0, 0, 1);
                    h.interpolation(3, 0);
                }
                for (BlockDisplayHandle h : fingers.get(3)) { // ring
                    h.rotate(ringSway, 0, 0, 1);
                    h.interpolation(3, 0);
                }
            }

            // Thumb rotation: 8° in/out, 90-tick cycle
            float thumbSwing = 0.14f * (float) Math.sin(ticksAlive * Math.PI / 45);
            if (!fingers.isEmpty()) {
                for (BlockDisplayHandle h : fingers.get(0)) {
                    h.rotate(thumbSwing, 0, 0, 1);
                    h.interpolation(3, 0);
                }
            }

            // Whole hand Y-rotation: 0.1°/tick
            float handRot = ticksAlive * 0.00175f;

            // Grasp every 120 ticks
            // (visual via scale/transform — simplified to particle burst)
            if (ticksAlive % 120 == 0 && ticksAlive > 60) {
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 8, 0), 15, 3, 5, 2, 0.05);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.5f);
            }

            // Lava from knuckle magma blocks
            if (ticksAlive % 5 == 0) {
                for (List<BlockDisplayHandle> finger : fingers) {
                    for (int i = 0; i < finger.size(); i += 3) {
                        w.spawnParticle(Particle.LAVA, finger.get(i).entity().getLocation(),
                                1, 0.3, 0.1, 0.3, 0);
                    }
                }
            }

            // Soul fire flame from wrist ring
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < wristRing.size(); i += 3) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, wristRing.get(i).entity().getLocation().add(0, 0.3, 0),
                            2, 0.3, 0.2, 0.3, 0.01);
                }
            }

            // Crimson spore from tendon underside
            if (ticksAlive % 8 == 0) {
                w.spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(0, 1, 0), 3, 1, 0.5, 1, 0);
            }

            // Smoke from palm
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 5, 0), 4, 2, 1, 1, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PetrifiedTitanHand(plugin); }
    }

    // ================================================================
    // 78. SPIRAL INFERNO — 20-block-tall helical spiral with fast Y-rotation,
    //     fire charge spine, and centrifugal flame spray
    // ================================================================
    public static class SpiralInferno extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiralBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> containmentRing = new ArrayList<>();
        private final List<BlockDisplayHandle> crownBlocks = new ArrayList<>();

        public SpiralInferno(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spiral_inferno", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Helical spiral: 60 blocks, 4 rotations over 20 blocks
            Material[] spiralMats = {Material.MAGMA_BLOCK, Material.NETHERRACK, Material.BASALT};
            for (int i = 0; i < 60; i++) {
                double angle = (Math.PI * 2 * i) / 15; // 4 full rotations
                double y = (i / 60.0) * 20.0;
                double r = 3.0;
                Location loc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, spiralMats[i / 20]);
                h.scale(1.5f, 0.8f, 1.5f).glow(200, 80, 30).interpolation(3, 0);
                spiralBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fire charge spine: 4 displays at heights 5, 10, 15, 20
            int[] spineHeights = {5, 10, 15, 20};
            for (int sh : spineHeights) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, sh, 0), Material.MAGMA_BLOCK);
                h.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                spineBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Containment ring: 9-block-diameter blackstone, 2 blocks tall
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                for (int y = 0; y < 2; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 4.5, y, Math.sin(angle) * 4.5), Material.BLACKSTONE);
                    h.scale(1.0f, 1.0f, 1.0f).glow(40, 40, 50).interpolation(2, 0);
                    containmentRing.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Crown blaze rods at spiral top
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 2, 20, Math.sin(angle) * 2), Material.NETHERRACK);
                h.scale(0.15f, 0.8f, 0.15f).glow(255, 100, 0).interpolation(3, 0);
                crownBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spiral Y-rotation: 1.5°/tick (fast!)
            float spiralRot = ticksAlive * 0.02618f;

            // Breathing: radius 3.0 +/- 0.1 over 60-tick cycle
            float breathe = 0.1f * (float) Math.sin(ticksAlive * Math.PI / 30);
            float radius = 3.0f + breathe;

            for (int i = 0; i < spiralBlocks.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 15 + spiralRot;
                double y = (i / 60.0) * 20.0;
                Location loc = center.clone().add(Math.cos(baseAngle) * radius, y, Math.sin(baseAngle) * radius);
                spiralBlocks.get(i).entity().teleport(loc);
            }

            // Spine counter-rotation: 2.0°/tick + scale pulse
            float spineRot = -ticksAlive * 0.0349f;
            for (int i = 0; i < spineBlocks.size(); i++) {
                int offset = i * 20;
                float sScale = 1.0f + 0.3f * (float) Math.sin((ticksAlive + offset) * Math.PI / 10);
                spineBlocks.get(i).rotate(spineRot, 0, 1, 0);
                spineBlocks.get(i).scale(sScale, sScale, sScale);
                spineBlocks.get(i).interpolation(3, 0);
            }

            // Crown rotation: 3.0°/tick on Z-axis
            float crownRot = ticksAlive * 0.05236f;
            for (BlockDisplayHandle h : crownBlocks) {
                h.rotate(crownRot, 0, 0, 1);
                h.interpolation(3, 0);
            }

            // Flame flung outward from spiral blocks
            if (ticksAlive % 2 == 0) {
                int idx = (ticksAlive / 2) % spiralBlocks.size();
                Location bLoc = spiralBlocks.get(idx).entity().getLocation();
                double dx = bLoc.getX() - center.getX();
                double dz = bLoc.getZ() - center.getZ();
                w.spawnParticle(Particle.FLAME, bLoc, 2, dx * 0.1, 0.05, dz * 0.1, 0.03);
            }

            // Lava from spine
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle sp : spineBlocks) {
                    w.spawnParticle(Particle.LAVA, sp.entity().getLocation(), 2, 0.5, 0.1, 0.5, 0);
                }
            }

            // Smoke from containment ring
            if (ticksAlive % 6 == 0) {
                int rIdx = (ticksAlive / 6) % containmentRing.size();
                w.spawnParticle(Particle.SMOKE, containmentRing.get(rIdx).entity().getLocation().add(0, 1, 0),
                        2, 0.2, 0.3, 0.2, 0.01);
            }

            // Sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpiralInferno(plugin); }
    }

    // ================================================================
    // 79. NETHER SKY FRAGMENT — Massive overhead ceiling fragment
    //     descending slowly then slamming down
    // ================================================================
    public static class NetherSkyFragment extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fragmentBlocks = new ArrayList<>();
        private float fragmentY = 12.0f;
        private boolean slammed = false;

        public NetherSkyFragment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_sky_fragment", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(1300);
            config.setCooldownTicks(600);
            config.setImpactDamage(14.0);
            config.setImpactRadius(12.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 25x18 fragment with irregular edges (simplified representative set)
            Material[] fragMats = {Material.NETHERRACK, Material.BLACKSTONE, Material.BASALT,
                    Material.MAGMA_BLOCK, Material.CRACKED_STONE_BRICKS, Material.OBSIDIAN};
            for (int x = -12; x <= 12; x++) {
                for (int z = -9; z <= 8; z++) {
                    // Irregular edge: skip outer corners
                    if (Math.abs(x) + Math.abs(z) > 18) continue;
                    if (Math.abs(x) == 12 && Math.abs(z) > 5) continue;
                    // Select material based on position pattern
                    Material mat;
                    if (Math.abs(x) > 9 || Math.abs(z) > 6) mat = Material.CRACKED_STONE_BRICKS;
                    else if (Math.abs(x) < 3 && Math.abs(z) < 3) mat = Material.MAGMA_BLOCK;
                    else if ((x + z) % 5 == 0) mat = Material.BASALT;
                    else if ((x + z) % 7 == 0) mat = Material.BLACKSTONE;
                    else mat = Material.NETHERRACK;

                    // Thickness: 3 blocks
                    for (int y = 0; y < 3; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, 12 + y, z), mat);
                        h.scale(1.0f, 1.0f, 1.0f).glow(mat == Material.MAGMA_BLOCK ? 255 : 80,
                                mat == Material.MAGMA_BLOCK ? 100 : 60, mat == Material.MAGMA_BLOCK ? 0 : 50)
                                .interpolation(3, 0);
                        fragmentBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent: 0.02 blocks/tick until 4-block height, then pause, then slam
            if (!slammed) {
                if (fragmentY > 4.0f) {
                    fragmentY -= 0.02f;
                    float yDelta = -0.02f;
                    for (BlockDisplayHandle h : fragmentBlocks) {
                        Location loc = h.entity().getLocation();
                        loc.add(0, yDelta, 0);
                        h.entity().teleport(loc);
                    }
                } else if (ticksAlive > 400 + 200) { // Pause 200 ticks at 4-block height, then slam
                    slammed = true;
                    // Slam: drop 4 blocks in 5 ticks
                    for (BlockDisplayHandle h : fragmentBlocks) {
                        Location loc = h.entity().getLocation();
                        loc.add(0, -4, 0);
                        h.entity().teleport(loc);
                    }
                    fragmentY = 0;
                    triggerImpactDamage(center);
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
                    // Shockwave ring
                    DisplayBuilder.particleRing(center, 15, Particle.LAVA, 60, null);
                    w.spawnParticle(Particle.SMOKE, center, 50, 12, 1, 9, 0.1);
                }
            }

            // Slight X-axis tilt: 2° over descent
            float tilt = ticksAlive * 0.000262f;

            // Lava drips from magma inclusions
            if (ticksAlive % 3 == 0 && !slammed) {
                double dx = (Math.random() - 0.5) * 24;
                double dz = (Math.random() - 0.5) * 16;
                w.spawnParticle(Particle.LAVA, center.clone().add(dx, fragmentY - 1, dz), 1,
                        0.1, 0.1, 0.1, 0);
            }

            // Smoke from torn edges
            if (ticksAlive % 5 == 0) {
                double sx = (Math.random() > 0.5 ? 12 : -12) * (0.5 + Math.random() * 0.5);
                double sz = (Math.random() - 0.5) * 16;
                w.spawnParticle(Particle.SMOKE, center.clone().add(sx, fragmentY + 1, sz), 3,
                        0.3, 0.3, 0.3, 0.01);
            }

            // Dripping lava from obsidian corners
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.DRIPPING_LAVA, center.clone().add(10, fragmentY, 7), 1,
                        0.1, 0, 0.1, 0);
                w.spawnParticle(Particle.DRIPPING_LAVA, center.clone().add(-10, fragmentY, -7), 1,
                        0.1, 0, 0.1, 0);
            }

            // Grinding sound during descent
            if (ticksAlive % 80 == 0 && !slammed) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetherSkyFragment(plugin); }
    }

    // ================================================================
    // 80. THE LAST PILLAR OF THE END — 30-block obsidian pillar being
    //     consumed by brimstone with ascending conversion line
    // ================================================================
    public static class LastPillarOfTheEnd extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> obsidianSection = new ArrayList<>();
        private final List<BlockDisplayHandle> transitionSection = new ArrayList<>();
        private final List<BlockDisplayHandle> netherSection = new ArrayList<>();
        private final List<BlockDisplayHandle> consumptionLine = new ArrayList<>();
        private final List<BlockDisplayHandle> tearColumns = new ArrayList<>();
        private final List<BlockDisplayHandle> endStoneBase = new ArrayList<>();
        private BlockDisplayHandle crystalCore;

        public LastPillarOfTheEnd(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("last_pillar_of_the_end", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(1400);
            config.setCooldownTicks(600);
            config.setImpactDamage(4.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Bottom 18 blocks: pure obsidian, 3x3 profile
            for (int y = 0; y < 18; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, y, z), Material.OBSIDIAN);
                        h.scale(1.0f, 1.0f, 1.0f).glow(30, 0, 40).interpolation(2, 0);
                        obsidianSection.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Consumption line at block 18: magma ring wrapping perimeter
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 1.5, 18, Math.sin(angle) * 1.5), Material.MAGMA_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                consumptionLine.add(h);
                spawnedEntities.add(h.entity());
            }

            // Transition zone: blocks 19-24, mixed obsidian/netherrack
            for (int y = 19; y <= 24; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Material mat = (x + z + y) % 2 == 0 ? Material.OBSIDIAN : Material.NETHERRACK;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, y, z), mat);
                        h.scale(1.0f, 1.0f, 1.0f).glow(80, 40, 30).interpolation(3, 0);
                        transitionSection.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Upper netherrack: blocks 25-30
            for (int y = 25; y <= 30; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Material mat = y >= 29 ? Material.NETHERRACK : ((x + z + y) % 3 == 0 ? Material.OBSIDIAN : Material.NETHERRACK);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, y, z), mat);
                        h.scale(1.0f, 1.0f, 1.0f).glow(120, 60, 40).interpolation(2, 0);
                        netherSection.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Crystal core at top (nether star via glowstone)
            crystalCore = displayBuilder.spawnBlock(center.clone().add(0, 31, 0), Material.GLOWSTONE);
            crystalCore.scale(0.7f, 0.7f, 0.7f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(crystalCore.entity());

            // Crying obsidian tear columns weeping down from consumption line
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                for (int y = 8; y <= 18; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 1.3, y, Math.sin(angle) * 1.3),
                            Material.CRYING_OBSIDIAN);
                    h.scale(0.3f, 1.0f, 0.3f).glow(80, 0, 120).interpolation(2, 0);
                    tearColumns.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // End stone base ring: 7x7 (center 3x3 end stone look = netherrack outer)
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(x) == 3 && Math.abs(z) == 3) continue;
                    Material mat = (Math.abs(x) <= 1 && Math.abs(z) <= 1) ?
                            Material.SMOOTH_BASALT : Material.NETHERRACK; // End stone approximation
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, -1, z), mat);
                    h.scale(1.0f, 0.5f, 1.0f).glow(mat == Material.SMOOTH_BASALT ? 200 : 120,
                            mat == Material.SMOOTH_BASALT ? 200 : 60, mat == Material.SMOOTH_BASALT ? 180 : 40)
                            .interpolation(2, 0);
                    endStoneBase.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.7f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crystal tumble: all 3 axes
            float crystalRot = ticksAlive * 0.02094f;
            if (crystalCore != null) {
                crystalCore.rotate(crystalRot, 0.4f, 1.2f, 0.2f);
                crystalCore.interpolation(3, 0);
            }

            // Consumption line rotation: 2.0°/tick
            float lineRot = ticksAlive * 0.0349f;
            for (int i = 0; i < consumptionLine.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12 + lineRot;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 18, Math.sin(angle) * 1.5);
                consumptionLine.get(i).entity().teleport(loc);
            }

            // Pillar sway: 1° on Z-axis, 200-tick cycle
            float sway = 0.0175f * (float) Math.sin(ticksAlive * Math.PI / 100);

            // Tear column slow outward drift: 0.02 blocks per 50 ticks
            // (minimal, visual accent)

            // Dripping lava from tear columns
            if (ticksAlive % 4 == 0) {
                int tIdx = (ticksAlive / 4) % tearColumns.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, tearColumns.get(tIdx).entity().getLocation(),
                        1, 0.05, 0, 0.05, 0);
            }

            // Flame from consumption line
            if (ticksAlive % 3 == 0) {
                int cIdx = (ticksAlive / 3) % consumptionLine.size();
                w.spawnParticle(Particle.FLAME, consumptionLine.get(cIdx).entity().getLocation().add(0, 0.5, 0),
                        3, 0.1, 0.3, 0.1, 0.02);
            }

            // Soul fire flame from base seam
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, -0.5, 0), 2.0,
                        Particle.SOUL_FIRE_FLAME, 4, null);
            }

            // Lava burst from crystal every 100 ticks
            if (ticksAlive % 100 == 0 && ticksAlive > 0) {
                triggerImpactDamage(center.clone().add(0, 31, 0));
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 31, 0), 10, 1.5, 0.5, 1.5, 0);
            }

            // Sound
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.4f);
            }
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LastPillarOfTheEnd(plugin); }
    }
}
