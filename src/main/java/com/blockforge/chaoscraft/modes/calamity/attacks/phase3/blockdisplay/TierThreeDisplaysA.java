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
 * Phase 4C Block Display -- TIER 3: "IT NOTICES YOU" (60% HP)
 * Structures 51-60. The Dweller's silhouette and eye motifs bleed into
 * the environment. Soul blue counterpoints the brimstone heat.
 *
 * Dweller palette: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255)
 * Materials: NETHERRACK, MAGMA_BLOCK, BASALT, SMOOTH_BASALT, BLACKSTONE,
 *   POLISHED_BLACKSTONE, CRIMSON_NYLIUM, NETHER_WART_BLOCK, SOUL_SOIL,
 *   CRACKED_STONE_BRICKS, OBSIDIAN, CRYING_OBSIDIAN, ORANGE_STAINED_GLASS,
 *   RED_STAINED_GLASS, BLACK_CONCRETE
 *
 * Rules:
 * - NO status effects
 * - AxisAngle4f ONLY for rotations
 * - Damage 4.0-14.0 HP (escalating with tier)
 * - triggerImpactDamage() for impact hits
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Static particle/sound helpers
 */
public final class TierThreeDisplaysA {

    private TierThreeDisplaysA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WatchersSilhouette(plugin));
        registry.register(new BrimstoneEyeBloom(plugin));
        registry.register(new VeinedPillarCluster(plugin));
        registry.register(new SoulDrainRing(plugin));
        registry.register(new FractureWall(plugin));
        registry.register(new LavaVeinSpine(plugin));
        registry.register(new DwellerEyeTrio(plugin));
        registry.register(new BrimstoneAltar(plugin));
        registry.register(new MagmaTideFormation(plugin));
        registry.register(new CharredEyeObelisk(plugin));
    }

    // ================================================================
    // 51. THE WATCHER'S SILHOUETTE — Flat relief of the Dweller's upper body
    //     against a polished blackstone wall with glowing eye motifs
    // ================================================================
    public static class WatchersSilhouette extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> torsoBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> armLeftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> armRightBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ribBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> plinthBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shadowBlocks = new ArrayList<>();

        public WatchersSilhouette(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("watchers_silhouette", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Backing wall: 3 wide x 6 tall polished blackstone
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y < 6; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, -0.5), Material.POLISHED_BLACKSTONE);
                    h.scale(1.0f, 1.0f, 0.4f).glow(40, 40, 50).interpolation(3, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Torso: 4 wide x 2 tall black concrete
            for (int x = -2; x <= 1; x++) {
                for (int y = 2; y <= 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x * 0.9, y, 0), Material.BLACK_CONCRETE);
                    h.scale(0.9f, 1.0f, 0.3f).glow(20, 20, 25).interpolation(3, 0);
                    torsoBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Left arm: 5 blocks descending
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-2.5, y, 0), Material.BLACK_CONCRETE);
                h.scale(0.6f, 1.0f, 0.3f).glow(20, 20, 25).interpolation(3, 0);
                armLeftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right arm: 5 blocks descending
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(2.2, y, 0), Material.BLACK_CONCRETE);
                h.scale(0.6f, 1.0f, 0.3f).glow(20, 20, 25).interpolation(3, 0);
                armRightBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Head: 3 wide x 2 tall, corners cut (oval-ish)
            double[][] headOffsets = {{-0.8, 4, 0}, {0, 4, 0}, {0.8, 4, 0},
                    {-0.4, 5, 0}, {0, 5, 0}, {0.4, 5, 0}};
            for (double[] off : headOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.BLACK_CONCRETE);
                h.scale(0.8f, 0.9f, 0.3f).glow(15, 15, 20).interpolation(3, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Eyes: 2 magma cream → using magma blocks with cyan glow for the eye effect
            BlockDisplayHandle eyeL = displayBuilder.spawnBlock(
                    center.clone().add(-0.4, 4.5, 0.2), Material.MAGMA_BLOCK);
            eyeL.scale(0.35f, 0.35f, 0.2f).glow(0, 150, 255).interpolation(3, 0);
            eyeBlocks.add(eyeL);
            spawnedEntities.add(eyeL.entity());

            BlockDisplayHandle eyeR = displayBuilder.spawnBlock(
                    center.clone().add(0.4, 4.5, 0.2), Material.MAGMA_BLOCK);
            eyeR.scale(0.35f, 0.35f, 0.2f).glow(0, 150, 255).interpolation(3, 0);
            eyeBlocks.add(eyeR);
            spawnedEntities.add(eyeR.entity());

            // Rib accents: 4 horizontal bars across torso
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-1.2 + i * 0.8, 2.5, 0.3), Material.NETHERRACK);
                h.scale(0.8f, 0.12f, 0.12f).glow(200, 0, 50)
                        .rotate((float)(Math.PI / 2), 0, 0, 1).interpolation(3, 0);
                ribBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Shadow blocks behind silhouette
            double[][] shadowOff = {{-1.5, 1, -0.8}, {1.5, 1, -0.8},
                    {-1.0, 3, -0.8}, {1.0, 3, -0.8}};
            for (double[] off : shadowOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.CRACKED_STONE_BRICKS);
                h.scale(1.0f, 1.0f, 0.3f).glow(30, 30, 35).interpolation(3, 0);
                shadowBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Plinth: 5-wide basalt and soul soil
            for (int x = -2; x <= 2; x++) {
                Material mat = (x == -2 || x == 2) ? Material.SOUL_SOIL : Material.BASALT;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, -1, 0), mat);
                h.scale(1.0f, 1.0f, 1.0f).glow(60, 60, 70).interpolation(2, 0);
                plinthBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);
            DisplayBuilder.crimsonDust(center.clone().add(0, 3, 0), 30, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Arm pendulum: left arm 8° over 80 ticks, right arm 12° over 65 ticks
            float leftSwing = (float) Math.sin(ticksAlive * Math.PI / 40) * 0.14f;
            for (BlockDisplayHandle h : armLeftBlocks) {
                h.rotate(leftSwing, 1, 0, 0);
                h.interpolation(3, 0);
            }
            float rightSwing = (float) Math.sin(ticksAlive * Math.PI / 32.5) * 0.21f;
            for (BlockDisplayHandle h : armRightBlocks) {
                h.rotate(rightSwing, 1, 0, 0);
                h.interpolation(3, 0);
            }

            // Eye pulse: 30-tick cycle, scale from 0.35 to 0.45
            float eyeScale = 0.35f + 0.1f * (float) Math.sin(ticksAlive * Math.PI / 15);
            for (BlockDisplayHandle h : eyeBlocks) {
                h.scale(eyeScale, eyeScale, 0.2f);
                h.interpolation(3, 0);
            }

            // Wall breathing: 0.05 blocks backward every 120 ticks
            float wallZ = -0.5f - 0.05f * (float) Math.sin(ticksAlive * Math.PI / 60);
            // Just animate via interpolation, don't teleport the whole wall

            // Rib slow rotation
            for (int i = 0; i < ribBlocks.size(); i++) {
                float ribRot = (float)(Math.PI / 2) + ticksAlive * 0.007f;
                ribBlocks.get(i).rotate(ribRot, 0, 0, 1);
                ribBlocks.get(i).interpolation(3, 0);
            }

            // Soul fire flame from eyes
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle eye : eyeBlocks) {
                    Location eLoc = eye.entity().getLocation().add(0, 0.2, 0.5);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc, 3, 0.05, 0.05, 0.3, 0.02);
                }
            }

            // Crimson spore from arms
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle arm : armLeftBlocks) {
                    w.spawnParticle(Particle.CRIMSON_SPORE, arm.entity().getLocation(), 2,
                            0.1, 0.3, 0.1, 0);
                }
                for (BlockDisplayHandle arm : armRightBlocks) {
                    w.spawnParticle(Particle.CRIMSON_SPORE, arm.entity().getLocation(), 2,
                            0.1, 0.3, 0.1, 0);
                }
            }

            // Ash from plinth
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle p : plinthBlocks) {
                    w.spawnParticle(Particle.ASH, p.entity().getLocation().add(0, 0.5, 0), 2,
                            0.5, 0.1, 0.5, 0);
                }
            }

            // Ambient sound loop
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WatchersSilhouette(plugin); }
    }

    // ================================================================
    // 52. BRIMSTONE EYE BLOOM — Massive concentric oval eye motif
    //     with counter-rotating iris and pulsing magma ring
    // ================================================================
    public static class BrimstoneEyeBloom extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> magmaRing = new ArrayList<>();
        private final List<BlockDisplayHandle> irisRing = new ArrayList<>();
        private BlockDisplayHandle pupil;
        private final List<BlockDisplayHandle> backingBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> stalkBlocks = new ArrayList<>();

        public BrimstoneEyeBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_eye_bloom", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 16 netherrack in oval
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double rx = 4.5, ry = 2.5;
                Location loc = center.clone().add(Math.cos(angle) * rx, Math.sin(angle) * ry, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.7f, 0.7f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Magma ring: 12 magma blocks inset
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double rx = 3.3, ry = 1.8;
                Location loc = center.clone().add(Math.cos(angle) * rx, Math.sin(angle) * ry, -0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.65f, 0.65f, 0.4f).glow(255, 100, 0).interpolation(3, 0);
                magmaRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Iris: 8 orange stained glass
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double rx = 2.0, ry = 1.1;
                Location loc = center.clone().add(Math.cos(angle) * rx, Math.sin(angle) * ry, -0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.3f).glow(255, 150, 0).interpolation(3, 0);
                irisRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Pupil: single bright block (nether star effect via glowstone)
            pupil = displayBuilder.spawnBlock(center.clone().add(0, 0, -0.3), Material.GLOWSTONE);
            pupil.scale(0.8f, 0.8f, 0.4f).glow(255, 255, 255).interpolation(3, 0);
            spawnedEntities.add(pupil.entity());

            // Backing: 6 red stained glass behind
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, Math.sin(angle) * 0.8, -0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_STAINED_GLASS);
                h.scale(0.8f, 0.8f, 0.2f).glow(200, 0, 50).interpolation(2, 0);
                backingBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Basalt stalks at base
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(side * 4.0, -2.5 + y, 0), Material.BASALT);
                    h.scale(0.6f, 1.0f, 0.6f).glow(60, 60, 70).interpolation(2, 0);
                    stalkBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Tilt entire assembly 5 degrees backward
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.8f);
            DisplayBuilder.crimsonDust(center, 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Pupil Z-axis rotation: 1.2°/tick
            float pupilRot = ticksAlive * 0.021f;
            if (pupil != null) {
                pupil.rotate(pupilRot, 0, 0, 1);
                pupil.interpolation(3, 0);
            }

            // Iris counter-rotation: 0.6°/tick opposite
            float irisRot = -ticksAlive * 0.0105f;
            for (BlockDisplayHandle h : irisRing) {
                h.rotate(irisRot, 0, 0, 1);
                h.interpolation(3, 0);
            }

            // Magma ring pulse: 50-tick sine wave
            float magmaScale = 1.0f + 0.06f * (float) Math.sin(ticksAlive * Math.PI / 25);
            for (BlockDisplayHandle h : magmaRing) {
                h.scale(0.65f * magmaScale, 0.65f * magmaScale, 0.4f);
                h.interpolation(3, 0);
            }

            // Eye tilting: X-axis between -3 and +3 degrees over 200 ticks
            float tiltX = (float) Math.sin(ticksAlive * Math.PI / 100) * 0.052f;

            // Lava burst from magma ring every 40 ticks
            if (ticksAlive % 40 == 0) {
                for (BlockDisplayHandle h : magmaRing) {
                    Location bLoc = h.entity().getLocation();
                    w.spawnParticle(Particle.LAVA, bLoc, 2, 0.3, 0.3, 0.3, 0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.4f);
            }

            // Flame helix from pupil
            if (ticksAlive % 3 == 0) {
                double angle = ticksAlive * 0.3;
                double hx = Math.cos(angle) * (0.5 + (ticksAlive % 40) * 0.05);
                double hy = Math.sin(angle) * (0.5 + (ticksAlive % 40) * 0.05);
                Location pLoc = center.clone().add(hx, hy, 0.5);
                w.spawnParticle(Particle.FLAME, pLoc, 1, 0, 0, 0, 0.01);
            }

            // Dripping lava from outer ring bottom
            if (ticksAlive % 7 == 0) {
                int idx = ticksAlive / 7 % outerRing.size();
                if (outerRing.get(idx).entity().getLocation().getY() < center.getY()) {
                    w.spawnParticle(Particle.DRIPPING_LAVA,
                            outerRing.get(idx).entity().getLocation(), 1, 0.1, 0, 0.1, 0);
                }
            }

            // Ambient fire sound
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.8f);
            }

            // Flash pulse every 100 ticks (full rotation) — impact damage
            if (ticksAlive % 100 == 0 && ticksAlive > 0) {
                triggerImpactDamage(center);
                w.spawnParticle(Particle.FLAME, center, 20, 1.5, 1.5, 1.5, 0.1);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneEyeBloom(plugin); }
    }

    // ================================================================
    // 53. VEINED PILLAR CLUSTER — Three basalt columns of differing heights
    //     with lava veins and crimson nylium floor
    // ================================================================
    public static class VeinedPillarCluster extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shortPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> midPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> tallPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> veinBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> floorBlocks = new ArrayList<>();
        private BlockDisplayHandle capBlock;

        public VeinedPillarCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("veined_pillar_cluster", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Short column (7 blocks) at offset (-1.2, 0, -1.2)
            Location shortBase = center.clone().add(-1.2, 0, -1.2);
            for (int y = 0; y < 7; y++) {
                Material mat = y < 2 ? Material.NETHERRACK : (y < 5 ? Material.MAGMA_BLOCK : Material.CRACKED_STONE_BRICKS);
                BlockDisplayHandle h = displayBuilder.spawnBlock(shortBase.clone().add(0, y, 0), Material.SMOOTH_BASALT);
                h.scale(1.2f, 1.0f, 1.2f).glow(80, 80, 90).interpolation(3, 0);
                shortPillar.add(h);
                spawnedEntities.add(h.entity());
                // Vein on face
                if (y % 2 == 1) {
                    BlockDisplayHandle v = displayBuilder.spawnBlock(shortBase.clone().add(0.5, y, 0), Material.RED_STAINED_GLASS);
                    v.scale(0.15f, 0.8f, 0.15f).glow(200, 0, 50).interpolation(2, 0);
                    veinBlocks.add(v);
                    spawnedEntities.add(v.entity());
                }
            }

            // Mid column (9 blocks) at offset (1.2, 0, -0.5)
            Location midBase = center.clone().add(1.2, 0, -0.5);
            for (int y = 0; y < 9; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(midBase.clone().add(0, y, 0), Material.SMOOTH_BASALT);
                h.scale(1.2f, 1.0f, 1.2f).glow(80, 80, 90).interpolation(3, 0);
                midPillar.add(h);
                spawnedEntities.add(h.entity());
                if (y % 2 == 1) {
                    BlockDisplayHandle v = displayBuilder.spawnBlock(midBase.clone().add(-0.5, y, 0), Material.RED_STAINED_GLASS);
                    v.scale(0.15f, 0.8f, 0.15f).glow(200, 0, 50).interpolation(2, 0);
                    veinBlocks.add(v);
                    spawnedEntities.add(v.entity());
                }
            }

            // Tall column (11 blocks) at offset (0, 0, 1.2)
            Location tallBase = center.clone().add(0, 0, 1.2);
            for (int y = 0; y < 11; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(tallBase.clone().add(0, y, 0), Material.SMOOTH_BASALT);
                h.scale(1.2f, 1.0f, 1.2f).glow(80, 80, 90).interpolation(3, 0);
                tallPillar.add(h);
                spawnedEntities.add(h.entity());
                if (y % 2 == 1) {
                    BlockDisplayHandle v = displayBuilder.spawnBlock(tallBase.clone().add(0, y, 0.5), Material.RED_STAINED_GLASS);
                    v.scale(0.15f, 0.8f, 0.15f).glow(200, 0, 50).interpolation(2, 0);
                    veinBlocks.add(v);
                    spawnedEntities.add(v.entity());
                }
            }

            // Cap on tallest
            capBlock = displayBuilder.spawnBlock(tallBase.clone().add(0, 11, 0), Material.POLISHED_BLACKSTONE);
            capBlock.scale(1.4f, 0.5f, 1.4f).glow(40, 40, 50).interpolation(2, 0);
            spawnedEntities.add(capBlock.entity());

            // Floor triangle: crimson nylium and nether wart blocks
            double[][] floorOff = {{-0.5, -0.5, 0}, {0.5, -0.5, 0}, {0, -0.5, 0.5},
                    {-0.3, -0.5, -0.3}, {0.3, -0.5, 0.3}};
            for (int i = 0; i < floorOff.length; i++) {
                Material mat = i < 3 ? Material.CRIMSON_NYLIUM : Material.NETHER_WART_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(floorOff[i][0], floorOff[i][1], floorOff[i][2]), mat);
                h.scale(1.0f, 0.3f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                floorBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Y-axis rotation for each column at different rates
            float shortRot = ticksAlive * 0.00524f;  // 0.3°/tick
            float midRot = ticksAlive * 0.00873f;    // 0.5°/tick
            float tallRot = ticksAlive * 0.01396f;   // 0.8°/tick

            // Vertical bobbing
            float shortBob = 0.4f * (float) Math.sin(ticksAlive * Math.PI / 45);
            float midBob = 0.3f * (float) Math.sin(ticksAlive * Math.PI / 55);
            float tallBob = 0.2f * (float) Math.sin(ticksAlive * Math.PI / 35);

            // Apply rotation via interpolation
            for (BlockDisplayHandle h : shortPillar) {
                h.rotate(shortRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : midPillar) {
                h.rotate(midRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : tallPillar) {
                h.rotate(tallRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Flame from vein surfaces
            if (ticksAlive % 7 == 0) {
                for (BlockDisplayHandle v : veinBlocks) {
                    Location vLoc = v.entity().getLocation().add(0, 0.5, 0);
                    w.spawnParticle(Particle.FLAME, vLoc, 1, 0.05, 0.2, 0.05, 0.01);
                }
            }

            // Crimson spore from floor
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle f : floorBlocks) {
                    w.spawnParticle(Particle.CRIMSON_SPORE, f.entity().getLocation().add(0, 0.3, 0),
                            2, 0.3, 0.1, 0.3, 0);
                }
            }

            // Smoke from column tops
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(-1.2, 7, -1.2), 2, 0.2, 0.3, 0.2, 0.02);
                w.spawnParticle(Particle.SMOKE, center.clone().add(1.2, 9, -0.5), 2, 0.2, 0.3, 0.2, 0.02);
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 11, 1.2), 3, 0.2, 0.3, 0.2, 0.02);
            }

            // Ambient sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VeinedPillarCluster(plugin); }
    }

    // ================================================================
    // 54. SOUL DRAIN RING — Floating horizontal ring of soul soil segments
    //     with orbiting magma satellites around a central totem display
    // ================================================================
    public static class SoulDrainRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> shadowWeights = new ArrayList<>();
        private BlockDisplayHandle totemCore;
        private final List<BlockDisplayHandle> satellites = new ArrayList<>();

        public SoulDrainRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_drain_ring", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(700);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 soul soil ring segments at radius 2.5, floating 1.5 above ground
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 1.5, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(0.7f, 0.4f, 0.7f).glow(0, 150, 255).interpolation(3, 0);
                ringSegments.add(h);
                spawnedEntities.add(h.entity());

                // Shadow weight below each segment
                BlockDisplayHandle s = displayBuilder.spawnBlock(loc.clone().add(0, -0.3, 0), Material.BASALT);
                s.scale(0.5f, 0.2f, 0.5f).glow(40, 40, 50).interpolation(2, 0);
                shadowWeights.add(s);
                spawnedEntities.add(s.entity());
            }

            // Central totem: bright glowing block
            totemCore = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 0), Material.GLOWSTONE);
            totemCore.scale(0.6f, 0.8f, 0.6f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(totemCore.entity());

            // 4 magma cream satellites at cardinal positions
            double[][] satOff = {{1, 2.5, 0}, {-1, 2.5, 0}, {0, 2.5, 1}, {0, 2.5, -1}};
            for (double[] off : satOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.MAGMA_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
                satellites.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Vertical bob: 0.25 blocks on 100-tick sine wave
            float bob = 0.25f * (float) Math.sin(ticksAlive * Math.PI / 50);

            // Ring rotation: 0.4°/tick counter-clockwise
            float ringAngleOffset = ticksAlive * 0.00698f;

            for (int i = 0; i < ringSegments.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 12 + ringAngleOffset;
                Location loc = center.clone().add(
                        Math.cos(baseAngle) * 2.5, 1.5 + bob, Math.sin(baseAngle) * 2.5);
                ringSegments.get(i).entity().teleport(loc);
                shadowWeights.get(i).entity().teleport(loc.clone().add(0, -0.3, 0));
            }

            // Totem counter-rotation: 0.8°/tick clockwise
            float totemRot = ticksAlive * 0.01396f;
            if (totemCore != null) {
                totemCore.entity().teleport(center.clone().add(0, 2.5 + bob, 0));
                totemCore.rotate(totemRot, 0, 1, 0);
                totemCore.interpolation(3, 0);
            }

            // Satellite orbit: 1 orbit per 150 ticks
            float satAngle = ticksAlive * (float)(Math.PI * 2) / 150;
            for (int i = 0; i < satellites.size(); i++) {
                double baseA = (Math.PI * 2 * i) / 4 + satAngle;
                Location sLoc = center.clone().add(Math.cos(baseA) * 1.0, 2.5 + bob, Math.sin(baseA) * 1.0);
                satellites.get(i).entity().teleport(sLoc);
            }

            // Soul fire flame from ring segments upward
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle h : ringSegments) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, h.entity().getLocation().add(0, 0.3, 0),
                            2, 0.05, 0.2, 0.05, 0.01);
                }
            }

            // Dripping lava from satellites
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle s : satellites) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, s.entity().getLocation(), 1, 0.05, 0, 0.05, 0);
                }
            }

            // Ash halo outward
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), 3.0, Particle.ASH, 6, null);
            }

            // Sound loop
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulDrainRing(plugin); }
    }

    // ================================================================
    // 55. FRACTURE WALL — Vertical slab of island surface heaved upright
    //     with a magma fault line and grinding tectonic animation
    // ================================================================
    public static class FractureWall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> faultBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> soulPatches = new ArrayList<>();
        private final List<BlockDisplayHandle> crimsonPatches = new ArrayList<>();

        public FractureWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fracture_wall", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
            config.setImpactDamage(4.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wall: 11 wide x 8 tall, leaning 12 degrees
            for (int x = -5; x <= 5; x++) {
                for (int y = 0; y < 8; y++) {
                    Material mat;
                    if (y < 5) {
                        mat = (x + y) % 3 == 0 ? Material.BLACKSTONE : Material.NETHERRACK;
                    } else {
                        mat = (x + y) % 2 == 0 ? Material.CRACKED_STONE_BRICKS : Material.BASALT;
                    }
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, 0), mat);
                    h.scale(1.0f, 1.0f, 0.5f).glow(80, 60, 50)
                            .rotate(0.21f, 0, 0, 1) // 12 degrees lean
                            .interpolation(3, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Diagonal fault line: 9 magma blocks from lower-left to upper-right
            for (int i = 0; i < 9; i++) {
                double fx = -4 + i;
                double fy = i * 0.9;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(fx, fy, 0.1), Material.MAGMA_BLOCK);
                h.scale(0.8f, 0.8f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
                faultBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Soul soil patches on left side
            double[][] soulOff = {{-4, 2, 0.05}, {-3, 3, 0.05}, {-4.5, 4, 0.05},
                    {-3.5, 1, 0.05}, {-2.5, 4.5, 0.05}, {-4, 5, 0.05}};
            for (double[] off : soulOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.SOUL_SOIL);
                h.scale(0.7f, 0.7f, 0.2f).glow(0, 150, 255).interpolation(2, 0);
                soulPatches.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crimson nylium patches on right side
            double[][] crimOff = {{3, 1, 0.05}, {4, 2, 0.05}, {3.5, 3, 0.05}, {4.5, 1.5, 0.05}};
            for (double[] off : crimOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.CRIMSON_NYLIUM);
                h.scale(0.7f, 0.7f, 0.2f).glow(200, 0, 50).interpolation(2, 0);
                crimsonPatches.add(h);
                spawnedEntities.add(h.entity());
            }

            // Polished blackstone caps
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-3 + i * 3, 8, 0), Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.5f, 0.4f).glow(50, 50, 60).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Grinding tilt: 10 to 14 degrees over 180-tick cycle
            float tilt = 0.175f + 0.035f * (float) Math.sin(ticksAlive * Math.PI / 90);
            for (BlockDisplayHandle h : wallBlocks) {
                h.rotate(tilt, 0, 0, 1);
                h.interpolation(4, 0);
            }

            // Fault line magma pulse: 40-tick cycle
            float faultScale = 1.0f + 0.1f * (float) Math.sin(ticksAlive * Math.PI / 20);
            for (BlockDisplayHandle h : faultBlocks) {
                h.scale(0.8f * faultScale, 0.8f * faultScale, 0.3f);
                h.interpolation(3, 0);
            }

            // Micro-lurch every 60 ticks
            if (ticksAlive % 60 == 0 && ticksAlive > 0) {
                triggerImpactDamage(center);
                // Shockwave particles
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 0.5, 1), 15, 1.5, 0.3, 0.5, 0.05);
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 0.5, 1), 10, 1.5, 0.3, 0.5, 0.03);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.3f);
            }

            // Flame from fault line
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle h : faultBlocks) {
                    w.spawnParticle(Particle.FLAME, h.entity().getLocation().add(0, 0.3, 0),
                            1, 0.05, 0.15, 0.05, 0.01);
                }
            }

            // Smoke from base
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 0.2, 0.5), 3, 2.0, 0.1, 0.3, 0.01);
            }

            // Dripping lava on right face
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle h : crimsonPatches) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, h.entity().getLocation(), 1, 0.1, 0, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FractureWall(plugin); }
    }

    // ================================================================
    // 56. LAVA VEIN SPINE — Freestanding S-curve spinal column
    //     with magma vertebral processes and lava vein internals
    // ================================================================
    public static class LavaVeinSpine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spineCore = new ArrayList<>();
        private final List<BlockDisplayHandle> vertebralPairs = new ArrayList<>();
        private final List<BlockDisplayHandle> veinBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crownBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();

        public LavaVeinSpine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_vein_spine", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(760);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spine core: 7 smooth basalt blocks with S-curve offsets
            float[] sOffsets = {0, 0.1f, 0.2f, 0.15f, 0, -0.1f, -0.15f};
            for (int y = 0; y < 7; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(sOffsets[y], y + 1, 0), Material.SMOOTH_BASALT);
                h.scale(0.8f, 1.0f, 0.8f).glow(80, 80, 90).interpolation(3, 0);
                spineCore.add(h);
                spawnedEntities.add(h.entity());
            }

            // Vertebral processes: 4 pairs of magma blocks at positions 1, 3, 5, 7
            int[] vertebralY = {1, 3, 5, 7};
            for (int vy : vertebralY) {
                for (int side = -1; side <= 1; side += 2) {
                    double offset = side * 0.7;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(offset, vy, 0), Material.MAGMA_BLOCK);
                    h.scale(0.5f, 0.4f, 0.5f).glow(255, 100, 0)
                            .rotate(0.785f * side, 0, 0, 1) // 45° angle
                            .interpolation(3, 0);
                    vertebralPairs.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Red stained glass vein blocks between vertebrae
            int[] veinY = {2, 4, 6};
            for (int vy : veinY) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(sOffsets[vy - 1], vy + 0.5, 0), Material.RED_STAINED_GLASS);
                h.scale(0.3f, 0.6f, 0.3f).glow(200, 0, 50).interpolation(2, 0);
                veinBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown: 3 blackstone blocks flared outward
            double[][] crownOff = {{-0.5, 8.5, 0}, {0, 9, 0}, {0.5, 8.5, 0}};
            for (double[] off : crownOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.BLACKSTONE);
                h.scale(0.6f, 0.6f, 0.6f).glow(50, 50, 60).interpolation(2, 0);
                crownBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Base: 3x3 netherrack spread
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0, z), Material.NETHERRACK);
                    h.scale(1.0f, 0.5f, 1.0f).glow(120, 60, 40).interpolation(2, 0);
                    baseBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // S-curve flex: middle translates left/right over 200 ticks
            float flex = 0.2f * (float) Math.sin(ticksAlive * Math.PI / 100);
            // Apply to mid-spine blocks (indices 2-4)
            for (int i = 2; i <= 4 && i < spineCore.size(); i++) {
                Location baseLoc = center.clone().add(flex, i + 1, 0);
                spineCore.get(i).entity().teleport(baseLoc);
            }

            // Vertebral pendulum: 15° forward/back on X-axis, 60-tick cycle
            float vertebralSwing = 0.262f * (float) Math.sin(ticksAlive * Math.PI / 30);
            for (BlockDisplayHandle h : vertebralPairs) {
                h.rotate(vertebralSwing, 1, 0, 0);
                h.interpolation(3, 0);
            }

            // Crown Y-axis rotation: 0.6°/tick
            float crownRot = ticksAlive * 0.01047f;
            for (BlockDisplayHandle h : crownBlocks) {
                h.rotate(crownRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Lava burst from veins
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle v : veinBlocks) {
                    w.spawnParticle(Particle.LAVA, v.entity().getLocation(), 1, 0.3, 0.1, 0.3, 0);
                }
            }

            // Flame from vertebrae
            if (ticksAlive % 7 == 0) {
                for (BlockDisplayHandle vp : vertebralPairs) {
                    w.spawnParticle(Particle.FLAME, vp.entity().getLocation().add(0, 0.3, 0),
                            1, 0.05, 0.1, 0.05, 0.01);
                }
            }

            // Dripping lava along crown
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle c : crownBlocks) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, c.entity().getLocation(), 1, 0.1, 0, 0.1, 0);
                }
            }

            // Ambient sound
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaVeinSpine(plugin); }
    }

    // ================================================================
    // 57. DWELLER EYE TRIO — Three stacked eye structures on a basalt mast
    //     with slow arena-scanning Y-rotation
    // ================================================================
    public static class DwellerEyeTrio extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mastBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeFrames = new ArrayList<>();
        private final List<BlockDisplayHandle> irisBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> lidBlocks = new ArrayList<>();
        private BlockDisplayHandle collarBlock;

        public DwellerEyeTrio(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_eye_trio", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Mast: 10 basalt blocks
            for (int y = 0; y < 10; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.BASALT);
                h.scale(0.5f, 1.0f, 0.5f).glow(70, 70, 80).interpolation(2, 0);
                mastBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Collar at base
            collarBlock = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.CRACKED_STONE_BRICKS);
            collarBlock.scale(1.2f, 0.5f, 1.2f).glow(60, 60, 70).interpolation(2, 0);
            spawnedEntities.add(collarBlock.entity());

            // Three eyes at heights 2, 5, 8
            int[] eyeHeights = {2, 5, 8};
            for (int eh : eyeHeights) {
                // Iris (fire charge equivalent): bright orange block
                BlockDisplayHandle iris = displayBuilder.spawnBlock(
                        center.clone().add(0, eh, 0.5), Material.ORANGE_STAINED_GLASS);
                iris.scale(0.5f, 0.5f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
                irisBlocks.add(iris);
                spawnedEntities.add(iris.entity());

                // Netherrack eyelid quarter blocks (4 around iris)
                double[][] lidOff = {{-0.4, 0.3, 0.5}, {0.4, 0.3, 0.5},
                        {-0.4, -0.3, 0.5}, {0.4, -0.3, 0.5}};
                for (double[] off : lidOff) {
                    BlockDisplayHandle lid = displayBuilder.spawnBlock(
                            center.clone().add(off[0], eh + off[1], off[2]), Material.NETHERRACK);
                    lid.scale(0.3f, 0.3f, 0.2f).glow(150, 60, 40).interpolation(2, 0);
                    lidBlocks.add(lid);
                    spawnedEntities.add(lid.entity());
                }

                // Black concrete lid-lines above and below
                BlockDisplayHandle topLid = displayBuilder.spawnBlock(
                        center.clone().add(0, eh + 0.5, 0.5), Material.BLACK_CONCRETE);
                topLid.scale(0.8f, 0.15f, 0.2f).glow(20, 20, 25).interpolation(2, 0);
                eyeFrames.add(topLid);
                spawnedEntities.add(topLid.entity());

                BlockDisplayHandle botLid = displayBuilder.spawnBlock(
                        center.clone().add(0, eh - 0.5, 0.5), Material.BLACK_CONCRETE);
                botLid.scale(0.8f, 0.15f, 0.2f).glow(20, 20, 25).interpolation(2, 0);
                eyeFrames.add(botLid);
                spawnedEntities.add(botLid.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Mast Y-rotation: 0.2°/tick (full revolution ~30s)
            float mastRot = ticksAlive * 0.00349f;
            for (BlockDisplayHandle h : mastBlocks) {
                h.rotate(mastRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : eyeFrames) {
                h.rotate(mastRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : lidBlocks) {
                h.rotate(mastRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Iris pulse: staggered 30-tick cycles (scale 0.5 to 0.65)
            for (int i = 0; i < irisBlocks.size(); i++) {
                int offset = i * 10;
                float iScale = 0.5f + 0.15f * (float) Math.sin((ticksAlive + offset) * Math.PI / 15);
                irisBlocks.get(i).scale(iScale, iScale, 0.3f);
                irisBlocks.get(i).rotate(mastRot, 0, 1, 0);
                irisBlocks.get(i).interpolation(3, 0);
            }

            // Eye lean-forward: 50-tick subtle oscillation
            // Implemented as part of the mast rotation animation

            // Soul fire flame cones from each iris
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle iris : irisBlocks) {
                    Location iLoc = iris.entity().getLocation().add(0, 0.1, 0.5);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, iLoc, 2, 0.05, 0.05, 0.3, 0.03);
                }
            }

            // Crimson spore from eyelids
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle lid : lidBlocks) {
                    w.spawnParticle(Particle.CRIMSON_SPORE, lid.entity().getLocation(), 1,
                            0.1, 0.15, 0.1, 0);
                }
            }

            // Smoke from mast top
            if (ticksAlive % 6 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 10, 0), 2, 0.1, 0.2, 0.1, 0.01);
            }

            // Sound loop
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellerEyeTrio(plugin); }
    }

    // ================================================================
    // 58. BRIMSTONE ALTAR — Squat altar with tumbling nether star core
    //     and rotating blaze rod corner posts
    // ================================================================
    public static class BrimstoneAltar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseLayer = new ArrayList<>();
        private final List<BlockDisplayHandle> midLayer = new ArrayList<>();
        private BlockDisplayHandle magmaCenter;
        private BlockDisplayHandle starCore;
        private final List<BlockDisplayHandle> cornerPosts = new ArrayList<>();
        private final List<BlockDisplayHandle> cardinalBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> frontPanel = new ArrayList<>();

        public BrimstoneAltar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_altar", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
            config.setImpactDamage(8.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base layer: 5x5 blackstone platform
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0, z), Material.BLACKSTONE);
                    h.scale(1.0f, 0.5f, 1.0f).glow(40, 40, 50).interpolation(2, 0);
                    baseLayer.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Mid layer: 3x3 netherrack with polished blackstone ring
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.5, z), Material.NETHERRACK);
                    h.scale(1.0f, 0.5f, 1.0f).glow(120, 60, 40).interpolation(2, 0);
                    midLayer.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Polished blackstone perimeter ring on mid layer
            double[][] perimOff = {{-2, 0.5, -1}, {-2, 0.5, 0}, {-2, 0.5, 1},
                    {2, 0.5, -1}, {2, 0.5, 0}, {2, 0.5, 1},
                    {-1, 0.5, -2}, {0, 0.5, -2}, {1, 0.5, -2}};
            for (double[] off : perimOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.5f, 1.0f).glow(50, 50, 60).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Top layer: magma center
            magmaCenter = displayBuilder.spawnBlock(center.clone().add(0, 1.0, 0), Material.MAGMA_BLOCK);
            magmaCenter.scale(1.0f, 0.5f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(magmaCenter.entity());

            // Cardinal cracked stone bricks on top
            double[][] cardOff = {{1, 1.0, 0}, {-1, 1.0, 0}, {0, 1.0, 1}, {0, 1.0, -1}};
            for (double[] off : cardOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.CRACKED_STONE_BRICKS);
                h.scale(0.8f, 0.5f, 0.8f).glow(70, 70, 80).interpolation(2, 0);
                cardinalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Nether star core (using glowstone for bright visual)
            starCore = displayBuilder.spawnBlock(center.clone().add(0, 1.6, 0), Material.GLOWSTONE);
            starCore.scale(0.4f, 0.4f, 0.4f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(starCore.entity());

            // Corner blaze rod posts (using netherrack pillars with glow)
            double[][] cornerOff = {{1.5, 1.5, 1.5}, {-1.5, 1.5, 1.5},
                    {1.5, 1.5, -1.5}, {-1.5, 1.5, -1.5}};
            for (double[] off : cornerOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.NETHERRACK);
                h.scale(0.15f, 0.8f, 0.15f).glow(255, 100, 0).interpolation(3, 0);
                cornerPosts.add(h);
                spawnedEntities.add(h.entity());
            }

            // Front panel: 2x2 orange stained glass
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(-0.5 + x, 0.5 + y * 0.5, -2.2), Material.ORANGE_STAINED_GLASS);
                    h.scale(0.5f, 0.5f, 0.2f).glow(255, 150, 0).interpolation(2, 0);
                    frontPanel.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Nether star tumble: Y at 0.8°/tick, X at 0.3°/tick, Z at 0.15°/tick
            // Using combined rotation approximation
            float starRot = ticksAlive * 0.01396f;
            if (starCore != null) {
                starCore.rotate(starRot, 0.3f, 0.8f, 0.15f);
                starCore.interpolation(3, 0);
            }

            // Corner post Y-rotation: 1.0°/tick
            float postRot = ticksAlive * 0.01745f;
            for (BlockDisplayHandle h : cornerPosts) {
                h.rotate(postRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Front panel pulse: 35-tick sine wave
            float panelScale = 1.0f + 0.05f * (float) Math.sin(ticksAlive * Math.PI / 17.5);
            for (BlockDisplayHandle h : frontPanel) {
                h.scale(0.5f * panelScale, 0.5f * panelScale, 0.2f);
                h.interpolation(3, 0);
            }

            // Altar settling: sink 0.05 blocks every 20 ticks, reset
            // (visual micro-settle via interpolation)

            // Flame from corner posts
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle p : cornerPosts) {
                    w.spawnParticle(Particle.FLAME, p.entity().getLocation().add(0, 0.5, 0),
                            2, 0.03, 0.2, 0.03, 0.01);
                }
            }

            // Lava ring from magma center
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 1.2, 0), 1, 0.5, 0, 0.5, 0);
            }

            // Dripping lava from front panel
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle fp : frontPanel) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, fp.entity().getLocation(), 1, 0.1, 0, 0.1, 0);
                }
            }

            // Nether star burst every 200 ticks (10 seconds) — impact damage
            if (ticksAlive % 200 == 0 && ticksAlive > 0) {
                triggerImpactDamage(center);
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 1.5, 0), 40, 2.5, 1.5, 2.5, 0.1);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneAltar(plugin); }
    }

    // ================================================================
    // 59. MAGMA TIDE FORMATION — Horizontal wave structure that surges
    //     across the ground with frozen lava-spray crest
    // ================================================================
    public static class MagmaTideFormation extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> waveBody = new ArrayList<>();
        private final List<BlockDisplayHandle> crestSpray = new ArrayList<>();
        private final List<BlockDisplayHandle> troughBlocks = new ArrayList<>();
        private float waveX = 0;

        public MagmaTideFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_tide_formation", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wave profile: rising from 1 to 4 blocks height across 12 blocks
            int[] waveHeights = {1, 1, 1, 2, 2, 2, 3, 4, 4, 3, 2, 1};
            Material[] waveMats = {Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK,
                    Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK,
                    Material.NETHERRACK, Material.MAGMA_BLOCK, Material.NETHERRACK, Material.NETHERRACK,
                    Material.BLACKSTONE};

            for (int x = 0; x < 12; x++) {
                for (int y = 0; y < waveHeights[x]; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x - 6, y, 0), waveMats[x]);
                    h.scale(1.0f, 1.0f, 1.0f).glow(200, 80, 30).interpolation(3, 0);
                    waveBody.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Crest spray: 6 orange stained glass at staggered heights
            int[] sprayHeights = {3, 4, 3, 4, 3, 4};
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(i - 3 + 1, sprayHeights[i], 0), Material.ORANGE_STAINED_GLASS);
                h.scale(0.6f, 0.8f, 0.6f).glow(255, 150, 0)
                        .rotate(0.35f, 1, 0, 0) // Leaning 20° in wave direction
                        .interpolation(3, 0);
                crestSpray.add(h);
                spawnedEntities.add(h.entity());
            }

            // Trough: soul sand bed below wave
            for (int x = 0; x < 12; x++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x - 6, -1, 0), Material.SOUL_SOIL);
                h.scale(1.0f, 0.5f, 1.0f).glow(0, 150, 255).interpolation(2, 0);
                troughBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wave translation: 0.08 blocks/tick for 10 blocks, then reset
            waveX += 0.08f;
            if (waveX > 10.0f) {
                waveX = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.4f);
            }

            // Move all wave blocks forward
            for (BlockDisplayHandle h : waveBody) {
                Location loc = h.entity().getLocation();
                loc.add(0.08, 0, 0);
                h.entity().teleport(loc);
            }
            for (BlockDisplayHandle h : crestSpray) {
                Location loc = h.entity().getLocation();
                loc.add(0.08, 0, 0);
                h.entity().teleport(loc);
            }

            // Crest curl: X-axis rotation at 0.4°/tick
            float curlRot = 0.35f + ticksAlive * 0.00698f;
            for (BlockDisplayHandle h : crestSpray) {
                // Staggered oscillation for spray
                float bob = 0.2f * (float) Math.sin(ticksAlive * Math.PI / 10);
                h.rotate(curlRot, 1, 0, 0);
                h.interpolation(3, 0);
            }

            // Lava splash from leading edge
            if (ticksAlive % 2 == 0) {
                Location leadEdge = center.clone().add(waveX - 6, 1, 0);
                w.spawnParticle(Particle.LAVA, leadEdge, 3, 0.3, 0.5, 0.3, 0);
            }

            // Dripping lava from crest
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle cs : crestSpray) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, cs.entity().getLocation(), 1, 0.1, 0, 0.1, 0);
                }
            }

            // Smoke from trough
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < troughBlocks.size(); i += 3) {
                    w.spawnParticle(Particle.SMOKE, troughBlocks.get(i).entity().getLocation().add(0, 0.3, 0),
                            2, 0.5, 0.1, 0.3, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaTideFormation(plugin); }
    }

    // ================================================================
    // 60. CHARRED EYE OBELISK — 12-block tapered obelisk with rotating
    //     magma band and four directional eye ports
    // ================================================================
    public static class CharredEyeObelisk extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> obeliskBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> magmaBand = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private BlockDisplayHandle capBlock;
        private final List<BlockDisplayHandle> leaningBase = new ArrayList<>();

        public CharredEyeObelisk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("charred_eye_obelisk", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(380);
            config.setImpactDamage(4.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base: 3x3 blackstone, 2 blocks tall
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, y, z), Material.BLACKSTONE);
                        h.scale(1.0f, 1.0f, 1.0f).glow(40, 40, 50).interpolation(2, 0);
                        obeliskBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Mid-shaft: 2x2 cracked stone bricks, 6 blocks tall
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    for (int y = 2; y < 8; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x - 0.5, y, z - 0.5), Material.CRACKED_STONE_BRICKS);
                        h.scale(0.8f, 1.0f, 0.8f).glow(70, 70, 80).interpolation(2, 0);
                        obeliskBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Taper: 1x1 polished blackstone, 2 blocks
            for (int y = 8; y < 10; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.POLISHED_BLACKSTONE);
                h.scale(0.6f, 1.0f, 0.6f).glow(50, 50, 60).interpolation(2, 0);
                obeliskBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cap: fire charge equivalent — bright glowing block
            capBlock = displayBuilder.spawnBlock(center.clone().add(0, 10, 0), Material.MAGMA_BLOCK);
            capBlock.scale(0.7f, 0.7f, 0.7f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(capBlock.entity());

            // Magma band at 4th block height: 12 blocks wrapping perimeter
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 4, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.4f, 0.5f, 0.4f).glow(255, 100, 0).interpolation(3, 0);
                magmaBand.add(h);
                spawnedEntities.add(h.entity());
            }

            // Four orange stained glass eyes at 6th block, one per face
            double[][] eyeOff = {{0.8, 6, 0}, {-0.8, 6, 0}, {0, 6, 0.8}, {0, 6, -0.8}};
            for (double[] off : eyeOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.ORANGE_STAINED_GLASS);
                h.scale(0.4f, 0.4f, 0.15f).glow(255, 150, 0).interpolation(3, 0);
                eyeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Leaning basalt blocks at base
            BlockDisplayHandle lean1 = displayBuilder.spawnBlock(
                    center.clone().add(1.5, 0.5, 0), Material.BASALT);
            lean1.scale(0.6f, 1.2f, 0.6f).glow(70, 70, 80)
                    .rotate(0.52f, 0, 0, 1) // 30° outward
                    .interpolation(2, 0);
            leaningBase.add(lean1);
            spawnedEntities.add(lean1.entity());

            BlockDisplayHandle lean2 = displayBuilder.spawnBlock(
                    center.clone().add(-1.5, 0.5, 0), Material.BASALT);
            lean2.scale(0.6f, 1.2f, 0.6f).glow(70, 70, 80)
                    .rotate(-0.52f, 0, 0, 1)
                    .interpolation(2, 0);
            leaningBase.add(lean2);
            spawnedEntities.add(lean2.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Obelisk Y-rotation: 0.5°/tick
            float obeliskRot = ticksAlive * 0.00873f;
            for (BlockDisplayHandle h : obeliskBlocks) {
                h.rotate(obeliskRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Cap counter-rotation: 1.5°/tick + scale pulse 60-tick sine
            float capRot = -ticksAlive * 0.02618f;
            float capScale = 0.7f + 0.15f * (float) Math.sin(ticksAlive * Math.PI / 30);
            if (capBlock != null) {
                capBlock.rotate(capRot, 0, 1, 0);
                capBlock.scale(capScale, capScale, capScale);
                capBlock.interpolation(3, 0);
            }

            // Magma band rotation: 1.0°/tick counter to main shaft
            float bandRot = -ticksAlive * 0.01745f;
            for (int i = 0; i < magmaBand.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 12 + bandRot;
                Location loc = center.clone().add(Math.cos(baseAngle) * 1.2, 4, Math.sin(baseAngle) * 1.2);
                magmaBand.get(i).entity().teleport(loc);
            }

            // Eye outward pulse: 40-tick sine wave, 0.08 blocks
            float eyePulse = 0.08f * (float) Math.sin(ticksAlive * Math.PI / 20);
            for (int i = 0; i < eyeBlocks.size(); i++) {
                double[][] baseOff = {{0.8, 6, 0}, {-0.8, 6, 0}, {0, 6, 0.8}, {0, 6, -0.8}};
                double[] off = baseOff[i];
                double dx = off[0] != 0 ? off[0] + Math.signum(off[0]) * eyePulse : 0;
                double dz = off[2] != 0 ? off[2] + Math.signum(off[2]) * eyePulse : 0;
                Location eLoc = center.clone().add(dx, off[1], dz);
                eyeBlocks.get(i).entity().teleport(eLoc);
            }

            // Flame from cap
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 10.5, 0), 4, 0.1, 0.3, 0.1, 0.02);
            }

            // Lava burst from magma band
            if (ticksAlive % 5 == 0) {
                int idx = (ticksAlive / 5) % magmaBand.size();
                w.spawnParticle(Particle.LAVA, magmaBand.get(idx).entity().getLocation(), 2,
                        0.3, 0.1, 0.3, 0);
            }

            // Soul fire flame from eyes
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle eye : eyeBlocks) {
                    Location eLoc = eye.entity().getLocation();
                    double dx = eLoc.getX() - center.getX();
                    double dz = eLoc.getZ() - center.getZ();
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc, 2,
                            dx * 0.3, 0.05, dz * 0.3, 0.02);
                }
            }

            // Sound loop
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.4f);
            }
            if (ticksAlive % 80 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CharredEyeObelisk(plugin); }
    }
}
