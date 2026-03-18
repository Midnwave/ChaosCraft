package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay;

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
 * Phase 3 Block Display — GROUP 1: BRIMSTONE SPINE (Structures 1-8)
 * The Calamity Dweller's central arena skeleton.
 * Eight structures converge at the island center, forming the
 * summoning anchor and initial arena framework.
 *
 * Dweller theme: Brimstone Inversion — Hell consuming The End.
 * Palette: netherrack, magma, blackstone, obsidian, soul soil/sand.
 * NO status effects. Damage in HP (not hearts).
 */
public final class BrimstoneSpine {

    private BrimstoneSpine() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BrimstoneThrone(plugin));
        registry.register(new MagmaRibArrayLeft(plugin));
        registry.register(new MagmaRibArrayRight(plugin));
        registry.register(new CentralLavaEye(plugin));
        registry.register(new BrimstonePillarNorth(plugin));
        registry.register(new BrimstonePillarSouth(plugin));
        registry.register(new BrimstoneArch(plugin));
        registry.register(new SoulFireRing(plugin));
    }

    // ================================================================
    // 1. BRIMSTONE THRONE — Asymmetric throne at island center, Y+0 to Y+16
    //    Pre-spawned. Sinusoidal Y oscillation. 7 spines of varying blocks.
    // ================================================================
    public static class BrimstoneThrone extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> seatBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private BlockDisplayHandle leftArm;
        private BlockDisplayHandle rightArm;

        public BrimstoneThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_throne", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base platform: netherrack 3x3 at Y+0
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0, z), Material.NETHERRACK);
                    h.glow(200, 0, 50).interpolation(3, 0);
                    seatBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Vertical column: magma block + cracked stone bricks up to seat
            for (int y = 1; y <= 7; y++) {
                Material mat = (y % 2 == 0) ? Material.MAGMA_BLOCK : Material.CRACKED_STONE_BRICKS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), mat);
                h.glow(255, 100, 0).interpolation(3, 0);
                seatBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Seat at Y+8
            BlockDisplayHandle seat = displayBuilder.spawnBlock(
                    center.clone().add(0, 8, 0), Material.BLACKSTONE);
            seat.scale(2.0f, 0.5f, 1.5f).glow(200, 0, 50).interpolation(3, 0);
            seatBlocks.add(seat);
            spawnedEntities.add(seat.entity());

            // Left arm: 3 blocks wide, ends in blackstone spike
            leftArm = displayBuilder.spawnBlock(
                    center.clone().add(-2, 8, 0), Material.BLACKSTONE);
            leftArm.scale(3.0f, 0.8f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(leftArm.entity());

            BlockDisplayHandle leftSpike = displayBuilder.spawnBlock(
                    center.clone().add(-4, 9, 0), Material.BLACKSTONE);
            leftSpike.scale(0.5f, 2.0f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
            seatBlocks.add(leftSpike);
            spawnedEntities.add(leftSpike.entity());

            // Right arm: 1 block wide, crying obsidian terminal
            rightArm = displayBuilder.spawnBlock(
                    center.clone().add(1.5, 8, 0), Material.CRYING_OBSIDIAN);
            rightArm.scale(1.0f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(rightArm.entity());

            // 7 spines at the back: Y+9 to Y+16, each a different material
            Material[] spineMats = {
                Material.NETHERRACK, Material.MAGMA_BLOCK, Material.BLACKSTONE,
                Material.CRACKED_STONE_BRICKS, Material.SOUL_SOIL, Material.OBSIDIAN,
                Material.BASALT
            };
            int[] spineHeights = {12, 14, 16, 13, 11, 15, 10};
            for (int i = 0; i < 7; i++) {
                float xOff = -3.0f + i;
                for (int y = 9; y <= spineHeights[i]; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(xOff, y, -1), spineMats[i]);
                    h.scale(0.6f, 1.0f, 0.6f).glow(200, 0, 50).interpolation(3, 0);
                    spineBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.5f);
            DisplayBuilder.crimsonDust(center.clone().add(0, 8, 0), 40, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sinusoidal Y oscillation: 0.3 blocks up/down, 80-tick period
            float yOsc = (float) Math.sin(ticksAlive * Math.PI / 40.0) * 0.3f;
            for (BlockDisplayHandle h : seatBlocks) {
                BlockDisplay bd = h.entity();
                Location loc = bd.getLocation();
                bd.teleport(loc.clone().add(0, yOsc - (float) Math.sin((ticksAlive - 1) * Math.PI / 40.0) * 0.3f, 0));
            }
            for (BlockDisplayHandle h : spineBlocks) {
                BlockDisplay bd = h.entity();
                Location loc = bd.getLocation();
                bd.teleport(loc.clone().add(0, yOsc - (float) Math.sin((ticksAlive - 1) * Math.PI / 40.0) * 0.3f, 0));
            }
            if (leftArm != null) {
                BlockDisplay bd = leftArm.entity();
                bd.teleport(bd.getLocation().add(0, yOsc - (float) Math.sin((ticksAlive - 1) * Math.PI / 40.0) * 0.3f, 0));
            }
            if (rightArm != null) {
                BlockDisplay bd = rightArm.entity();
                bd.teleport(bd.getLocation().add(0, yOsc - (float) Math.sin((ticksAlive - 1) * Math.PI / 40.0) * 0.3f, 0));
            }

            // Crying obsidian dripping from right arm
            if (ticksAlive % 10 == 0 && rightArm != null) {
                Location drip = rightArm.entity().getLocation().add(0.5, -0.5, 0);
                w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, drip, 2, 0.1, 0.3, 0.1, 0);
            }

            // Lava particles from netherrack spine (index 0) at rate ~5/sec
            if (ticksAlive % 4 == 0 && !spineBlocks.isEmpty()) {
                Location spineTop = spineBlocks.get(0).entity().getLocation().add(0, 1, 0);
                w.spawnParticle(Particle.LAVA, spineTop, 1, 0.1, 0.2, 0.1, 0);
            }

            // Soul fire from soul soil spine (index 4 group)
            if (ticksAlive % 7 == 0 && spineBlocks.size() > 10) {
                Location soulLoc = spineBlocks.get(10).entity().getLocation().add(0, 1, 0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, soulLoc, 3, 0.1, 0.5, 0.1, 0);
            }

            // Ambient sound every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 8, 0),
                        Sound.ENTITY_BLAZE_AMBIENT, 0.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneThrone(plugin); }
    }

    // ================================================================
    // 2. MAGMA RIB ARRAY (LEFT) — 8 curved ribs with glass panels, west side
    // ================================================================
    public static class MagmaRibArrayLeft extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ribs = new ArrayList<>();
        private final List<BlockDisplayHandle> glassPanels = new ArrayList<>();
        private final int RIB_COUNT = 8;

        public MagmaRibArrayLeft(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_rib_left", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ribOrigin = center.clone().add(-12, 0, 0);

            // 8 ribs: curved arc from Y+0 to Y+18
            for (int i = 0; i < RIB_COUNT; i++) {
                double zOff = -3.5 + i;
                for (int seg = 0; seg < 6; seg++) {
                    double arcT = seg / 5.0;
                    double yOff = arcT * 18.0;
                    double xCurve = Math.sin(arcT * Math.PI) * 3.0;
                    float taper = 1.0f - (seg * 0.12f);

                    Material mat = (seg % 2 == 0) ? Material.NETHERRACK : Material.MAGMA_BLOCK;
                    Location loc = ribOrigin.clone().add(xCurve, -3 + yOff, zOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(taper * 1.0f, 1.0f, taper * 0.8f)
                     .glow(255, 100, 0).interpolation(3, 0);
                    ribs.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Glass panel between rib pairs
                if (i < RIB_COUNT - 1) {
                    Location glassLoc = ribOrigin.clone().add(1.5, 6, zOff + 0.5);
                    BlockDisplayHandle gp = displayBuilder.spawnBlock(glassLoc,
                            Material.ORANGE_STAINED_GLASS);
                    gp.scale(0.1f, 4.0f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                    glassPanels.add(gp);
                    spawnedEntities.add(gp.entity());
                }
            }

            DisplayBuilder.playSound(ribOrigin, Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise animation: staggered eruption over first 60 ticks
            if (ticksAlive <= 60) {
                for (int i = 0; i < ribs.size(); i++) {
                    int ribIndex = i / 6;
                    int startTick = ribIndex * 7;
                    if (ticksAlive < startTick) continue;
                    int elapsed = ticksAlive - startTick;
                    if (elapsed > 60) elapsed = 60;
                    // Cubic easing: fast start, slow finish
                    float t = Math.min(1.0f, elapsed / 60.0f);
                    float eased = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
                    float yShift = (1.0f - eased) * -3.0f;
                    BlockDisplay bd = ribs.get(i).entity();
                    bd.teleport(bd.getLocation().add(0, yShift * 0.05f, 0));
                }
            }

            // Breathing: rotate on Z-axis +-2 degrees, 200-tick period
            float breathAngle = (float) Math.sin(ticksAlive * Math.PI / 100.0) * 0.035f;
            for (BlockDisplayHandle h : ribs) {
                h.rotate(breathAngle, 0, 0, 1);
                h.interpolation(5, 0);
            }

            // Glass panel color swap: alternate orange/red every 30 ticks
            if (ticksAlive % 30 == 0) {
                boolean useRed = (ticksAlive / 30) % 2 == 0;
                for (BlockDisplayHandle gp : glassPanels) {
                    Material mat = useRed ? Material.RED_STAINED_GLASS : Material.ORANGE_STAINED_GLASS;
                    gp.entity().setBlock(mat.createBlockData());
                }
            }

            // Flame particles from rib tips
            if (ticksAlive % 20 == 0) {
                for (int i = 5; i < ribs.size(); i += 6) {
                    Location tip = ribs.get(i).entity().getLocation().add(0, 0.5, 0);
                    w.spawnParticle(Particle.FLAME, tip, 2, 0.1, 0.2, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaRibArrayLeft(plugin); }
    }

    // ================================================================
    // 3. MAGMA RIB ARRAY (RIGHT) — Mirror of left, east side, blackstone
    //    Offset breathing cycle creates alternating bellows effect.
    // ================================================================
    public static class MagmaRibArrayRight extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ribs = new ArrayList<>();
        private final List<BlockDisplayHandle> glassPanels = new ArrayList<>();
        private final int RIB_COUNT = 8;

        public MagmaRibArrayRight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_rib_right", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ribOrigin = center.clone().add(12, 0, 0);

            for (int i = 0; i < RIB_COUNT; i++) {
                double zOff = -3.5 + i;
                for (int seg = 0; seg < 6; seg++) {
                    double arcT = seg / 5.0;
                    double yOff = arcT * 18.0;
                    double xCurve = -Math.sin(arcT * Math.PI) * 3.0; // Mirror
                    float taper = 1.0f - (seg * 0.12f);

                    // Blackstone instead of magma for darker look
                    Material mat = (seg % 2 == 0) ? Material.NETHERRACK : Material.BLACKSTONE;
                    Location loc = ribOrigin.clone().add(xCurve, -3 + yOff, zOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(taper * 1.0f, 1.0f, taper * 0.8f)
                     .glow(200, 0, 50).interpolation(3, 0);
                    ribs.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Deep red glass panels
                if (i < RIB_COUNT - 1) {
                    Location glassLoc = ribOrigin.clone().add(-1.5, 6, zOff + 0.5);
                    BlockDisplayHandle gp = displayBuilder.spawnBlock(glassLoc,
                            Material.RED_STAINED_GLASS);
                    gp.scale(0.1f, 4.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                    glassPanels.add(gp);
                    spawnedEntities.add(gp.entity());
                }
            }

            DisplayBuilder.playSound(ribOrigin, Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise animation: stagger eruption
            if (ticksAlive <= 60) {
                for (int i = 0; i < ribs.size(); i++) {
                    int ribIndex = i / 6;
                    int startTick = ribIndex * 7;
                    if (ticksAlive < startTick) continue;
                    int elapsed = ticksAlive - startTick;
                    if (elapsed > 60) elapsed = 60;
                    float t = Math.min(1.0f, elapsed / 60.0f);
                    float eased = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
                    float yShift = (1.0f - eased) * -3.0f;
                    BlockDisplay bd = ribs.get(i).entity();
                    bd.teleport(bd.getLocation().add(0, yShift * 0.05f, 0));
                }
            }

            // Breathing offset by 50 ticks from left rib array
            float breathAngle = (float) Math.sin((ticksAlive + 50) * Math.PI / 100.0) * 0.035f;
            for (BlockDisplayHandle h : ribs) {
                h.rotate(breathAngle, 0, 0, 1);
                h.interpolation(5, 0);
            }

            // Flame particles from rib tips
            if (ticksAlive % 20 == 0) {
                for (int i = 5; i < ribs.size(); i += 6) {
                    Location tip = ribs.get(i).entity().getLocation().add(0, 0.5, 0);
                    w.spawnParticle(Particle.FLAME, tip, 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaRibArrayRight(plugin); }
    }

    // ================================================================
    // 4. CENTRAL LAVA EYE — Rotating magma ring with spinning items
    //    at island center Y+2 to Y+6
    // ================================================================
    public static class CentralLavaEye extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private BlockDisplayHandle centerBlock;

        public CentralLavaEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("central_lava_eye", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(7.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 magma blocks in a ring, radius 2, at Y+2
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 2, z), Material.MAGMA_BLOCK);
                h.scale(0.8f, 0.8f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Netherrack center at Y+3
            centerBlock = displayBuilder.spawnBlock(
                    center.clone().add(0, 3, 0), Material.NETHERRACK);
            centerBlock.scale(0.6f, 0.6f, 0.6f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(centerBlock.entity());

            // Obsidian core above — visual anchor
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                    center.clone().add(0, 4, 0), Material.OBSIDIAN);
            core.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(core.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring rotation: 4.5 degrees per tick around center
            float anglePerTick = (float) Math.toRadians(4.5);
            for (int i = 0; i < ringBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 8 + ticksAlive * anglePerTick;
                double x = Math.cos(baseAngle) * 2.0;
                double z = Math.sin(baseAngle) * 2.0;
                Location target = center.clone().add(x, 2, z);
                ringBlocks.get(i).entity().teleport(target);
            }

            // Center block scale pulse: 0.6 -> 0.9 over 15 ticks, back
            float pulse = 0.6f + (float) Math.sin(ticksAlive * Math.PI / 15.0) * 0.15f;
            if (centerBlock != null) {
                centerBlock.scale(pulse, pulse, pulse);
                centerBlock.interpolation(2, 0);
            }

            // Lava particle burst from center every 2-3 ticks
            if (ticksAlive % 3 == 0) {
                Location eyeCenter = center.clone().add(0, 3, 0);
                w.spawnParticle(Particle.LAVA, eyeCenter, 3, 1.5, 0.2, 1.5, 0);
            }

            // Ambient sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 3, 0),
                        Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CentralLavaEye(plugin); }
    }

    // ================================================================
    // 5. BRIMSTONE PILLAR (NORTH) — 2x2 alternating netherrack/blackstone
    //    column, Y+0 to Y+20, slow Y-axis rotation, crying obsidian cap
    // ================================================================
    public static class BrimstonePillarNorth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> capBlocks = new ArrayList<>();
        private BlockDisplayHandle crossbar;

        public BrimstonePillarNorth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_pillar_north", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, -18);

            // 2x2 column: alternating netherrack/blackstone, 20 blocks tall
            for (int y = 0; y < 20; y++) {
                Material mat = (y % 2 == 0) ? Material.NETHERRACK : Material.BLACKSTONE;
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                base.clone().add(dx, y - 20, dz), mat);
                        h.glow(200, 0, 50).interpolation(3, 0);
                        pillarBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Crying obsidian cap triangle at Y+20
            double[][] capOffsets = {{0, 20, 0}, {1, 20, 0}, {0.5, 20, 1}};
            for (double[] off : capOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        base.clone().add(off[0], off[1] - 20, off[2]),
                        Material.CRYING_OBSIDIAN);
                h.glow(128, 0, 255).interpolation(3, 0);
                capBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crossbar at Y+10: basalt, 5 blocks wide
            crossbar = displayBuilder.spawnBlock(
                    base.clone().add(-2, 10 - 20, 0.5), Material.BASALT);
            crossbar.scale(5.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(crossbar.entity());

            DisplayBuilder.playSound(base, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise animation: linear over 80 ticks
            if (ticksAlive <= 80) {
                float rise = ticksAlive / 80.0f;
                float yOffset = rise * 20.0f;
                for (BlockDisplayHandle h : pillarBlocks) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().add(0, yOffset / 80.0f, 0));
                }
                for (BlockDisplayHandle h : capBlocks) {
                    h.entity().teleport(h.entity().getLocation().add(0, yOffset / 80.0f, 0));
                }
                if (crossbar != null) {
                    crossbar.entity().teleport(crossbar.entity().getLocation().add(0, yOffset / 80.0f, 0));
                }
            }

            // Y-axis rotation: 0.5 deg/tick clockwise
            if (ticksAlive > 80) {
                float rot = (float) Math.toRadians((ticksAlive - 80) * 0.5);
                for (BlockDisplayHandle h : pillarBlocks) {
                    h.rotate(rot, 0, 1, 0);
                    h.interpolation(3, 0);
                }
                for (BlockDisplayHandle h : capBlocks) {
                    h.rotate(rot, 0, 1, 0);
                    h.interpolation(3, 0);
                }
                if (crossbar != null) {
                    crossbar.rotate(rot, 0, 1, 0);
                    crossbar.interpolation(3, 0);
                }
            }

            // Crying obsidian drip from cap
            if (ticksAlive % 6 == 0 && ticksAlive > 80) {
                for (BlockDisplayHandle h : capBlocks) {
                    Location drip = h.entity().getLocation().add(0, -0.5, 0);
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, drip, 2, 0.1, 0.3, 0.1, 0);
                }
            }

            // Soul fire from crossbar ends
            if (ticksAlive % 8 == 0 && crossbar != null && ticksAlive > 80) {
                Location barLoc = crossbar.entity().getLocation();
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, barLoc.clone().add(-2, 0.5, 0), 3, 0.1, 0.2, 0.1, 0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, barLoc.clone().add(5, 0.5, 0), 3, 0.1, 0.2, 0.1, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstonePillarNorth(plugin); }
    }

    // ================================================================
    // 6. BRIMSTONE PILLAR (SOUTH) — Polished blackstone + soul soil,
    //    counter-clockwise rotation, soul fire columns on crossbar
    // ================================================================
    public static class BrimstonePillarSouth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> capBlocks = new ArrayList<>();
        private BlockDisplayHandle crossbar;

        public BrimstonePillarSouth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_pillar_south", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, 18);

            // Polished blackstone + soul soil alternating
            for (int y = 0; y < 20; y++) {
                Material mat = (y % 2 == 0) ? Material.SOUL_SOIL : Material.POLISHED_BLACKSTONE;
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                base.clone().add(dx, y - 20, dz), mat);
                        h.glow(0, 150, 255).interpolation(3, 0);
                        pillarBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Crying obsidian cap
            double[][] capOffsets = {{0, 20, 0}, {1, 20, 0}, {0.5, 20, 1}};
            for (double[] off : capOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        base.clone().add(off[0], off[1] - 20, off[2]),
                        Material.CRYING_OBSIDIAN);
                h.glow(0, 150, 255).interpolation(3, 0);
                capBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Soul sand crossbar at Y+10
            crossbar = displayBuilder.spawnBlock(
                    base.clone().add(-2, 10 - 20, 0.5), Material.SOUL_SAND);
            crossbar.scale(5.0f, 1.0f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
            spawnedEntities.add(crossbar.entity());

            DisplayBuilder.playSound(base, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise animation: linear over 80 ticks (2-second delay simulated by later spawn)
            if (ticksAlive <= 80) {
                float rise = ticksAlive / 80.0f;
                float yOffset = rise * 20.0f;
                for (BlockDisplayHandle h : pillarBlocks) {
                    h.entity().teleport(h.entity().getLocation().add(0, yOffset / 80.0f, 0));
                }
                for (BlockDisplayHandle h : capBlocks) {
                    h.entity().teleport(h.entity().getLocation().add(0, yOffset / 80.0f, 0));
                }
                if (crossbar != null) {
                    crossbar.entity().teleport(crossbar.entity().getLocation().add(0, yOffset / 80.0f, 0));
                }
            }

            // Counter-clockwise Y-axis rotation: -0.5 deg/tick
            if (ticksAlive > 80) {
                float rot = (float) Math.toRadians((ticksAlive - 80) * -0.5);
                for (BlockDisplayHandle h : pillarBlocks) {
                    h.rotate(rot, 0, 1, 0);
                    h.interpolation(3, 0);
                }
                for (BlockDisplayHandle h : capBlocks) {
                    h.rotate(rot, 0, 1, 0);
                    h.interpolation(3, 0);
                }
                if (crossbar != null) {
                    crossbar.rotate(rot, 0, 1, 0);
                    crossbar.interpolation(3, 0);
                }
            }

            // Soul fire columns from crossbar ends — scale grows over time
            if (ticksAlive % 4 == 0 && crossbar != null && ticksAlive > 80) {
                Location barLoc = crossbar.entity().getLocation();
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, barLoc.clone().add(-2, 0.5, 0), 4, 0.1, 0.5, 0.1, 0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, barLoc.clone().add(5, 0.5, 0), 4, 0.1, 0.5, 0.1, 0);
            }

            // Obsidian tears from cap
            if (ticksAlive % 8 == 0 && ticksAlive > 80) {
                for (BlockDisplayHandle h : capBlocks) {
                    Location drip = h.entity().getLocation().add(0, -0.5, 0);
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, drip, 2, 0.1, 0.3, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstonePillarSouth(plugin); }
    }

    // ================================================================
    // 7. BRIMSTONE ARCH — Spanning north-south pillars at Y+20 to Y+28,
    //    rough organic curve with crying obsidian apex
    // ================================================================
    public static class BrimstoneArch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private BlockDisplayHandle apexBlock;

        public BrimstoneArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_arch", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Arch spanning from Z-18 (north pillar) to Z+18 (south pillar)
            // Parabolic arch peaking at Y+28 at center (z=0)
            int blockCount = 32;
            for (int i = 0; i < blockCount; i++) {
                double t = (double) i / (blockCount - 1);
                double z = -18.0 + t * 36.0;
                double archHeight = 20.0 + 8.0 * Math.sin(t * Math.PI);
                // Random offset for organic feel
                double xJitter = (Math.random() - 0.5) * 0.6;
                double yJitter = (Math.random() - 0.5) * 0.6;

                Location loc = center.clone().add(xJitter, 40 + yJitter, z); // Start at Y+40 for drop
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.glow(200, 0, 50).interpolation(3, 0);
                archBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Apex block: crying obsidian flanked by crying obsidian
            apexBlock = displayBuilder.spawnBlock(
                    center.clone().add(0, 40, 0), Material.CRYING_OBSIDIAN);
            apexBlock.scale(1.2f, 1.2f, 1.2f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(apexBlock.entity());

            BlockDisplayHandle apexLeft = displayBuilder.spawnBlock(
                    center.clone().add(-1, 40, 0), Material.CRYING_OBSIDIAN);
            apexLeft.glow(128, 0, 255).interpolation(3, 0);
            archBlocks.add(apexLeft);
            spawnedEntities.add(apexLeft.entity());

            BlockDisplayHandle apexRight = displayBuilder.spawnBlock(
                    center.clone().add(1, 40, 0), Material.CRYING_OBSIDIAN);
            apexRight.glow(128, 0, 255).interpolation(3, 0);
            archBlocks.add(apexRight);
            spawnedEntities.add(apexRight.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Drop animation: blocks fall from Y+40 to arch position over 100 ticks
            // Each block staggered by 3 ticks, outermost last
            if (ticksAlive <= 130) {
                for (int i = 0; i < Math.min(archBlocks.size(), 32); i++) {
                    // Stagger: outer blocks last
                    int distFromCenter = Math.abs(i - 16);
                    int startTick = distFromCenter * 3;
                    if (ticksAlive < startTick) continue;
                    int elapsed = ticksAlive - startTick;
                    float t = Math.min(1.0f, elapsed / 100.0f);
                    // Cubic ease-in for gravity feel
                    float eased = t * t * t;

                    double paramT = (double) i / 31.0;
                    double z = -18.0 + paramT * 36.0;
                    double targetY = 20.0 + 8.0 * Math.sin(paramT * Math.PI);
                    double currentY = 40.0 + (targetY - 40.0) * eased;

                    Location target = center.clone().add(
                            (Math.random() - 0.5) * 0.3, currentY, z);
                    archBlocks.get(i).entity().teleport(target);
                }

                // Move apex blocks similarly
                if (ticksAlive > 10) {
                    float apexT = Math.min(1.0f, (ticksAlive - 10) / 100.0f);
                    float apexEased = apexT * apexT * apexT;
                    double apexY = 40.0 + (28.0 - 40.0) * apexEased;
                    if (apexBlock != null) {
                        apexBlock.entity().teleport(center.clone().add(0, apexY, 0));
                    }
                }

                // Impact damage when arch settles
                if (ticksAlive == 130) {
                    triggerImpactDamage(center.clone().add(0, 20, 0));
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 50, 3.0);
                }
            }

            // Post-settlement: gentle Z-axis rocking (+-1 degree, 60-tick period)
            if (ticksAlive > 130) {
                float rock = (float) Math.sin(ticksAlive * Math.PI / 30.0) * 0.017f;
                for (BlockDisplayHandle h : archBlocks) {
                    h.rotate(rock, 0, 0, 1);
                    h.interpolation(5, 0);
                }
                if (apexBlock != null) {
                    apexBlock.rotate(rock, 0, 0, 1);
                    apexBlock.interpolation(5, 0);
                }
            }

            // Dripping lava from every third block
            if (ticksAlive % 10 == 0 && ticksAlive > 130) {
                for (int i = 0; i < Math.min(archBlocks.size(), 32); i += 3) {
                    Location drip = archBlocks.get(i).entity().getLocation().add(0, -0.5, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, drip, 1, 0.1, 0, 0.1, 0);
                }
            }

            // Apex sound every 160 ticks
            if (ticksAlive % 160 == 0 && ticksAlive > 130) {
                DisplayBuilder.playSound(center.clone().add(0, 28, 0),
                        Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneArch(plugin); }
    }

    // ================================================================
    // 8. SOUL FIRE RING — 20 soul soil blocks at ground level, radius 15,
    //    orbiting soul sand displays, synchronized flame bursts
    // ================================================================
    public static class SoulFireRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> soilBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> sandBlocks = new ArrayList<>();
        private final int RING_COUNT = 20;

        public SoulFireRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_ring", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < RING_COUNT; i++) {
                double angle = (2 * Math.PI * i) / RING_COUNT;
                double x = Math.cos(angle) * 15.0;
                double z = Math.sin(angle) * 15.0;

                // Soul soil base, embedded slightly
                BlockDisplayHandle soil = displayBuilder.spawnBlock(
                        center.clone().add(x, -0.3, z), Material.SOUL_SOIL);
                soil.glow(0, 150, 255).interpolation(3, 0);
                soilBlocks.add(soil);
                spawnedEntities.add(soil.entity());

                // Suspended soul sand above, smaller and tilted
                BlockDisplayHandle sand = displayBuilder.spawnBlock(
                        center.clone().add(x, 1.0, z), Material.SOUL_SAND);
                // Random tilt for each
                float tiltX = (float) (Math.random() - 0.5) * 0.52f; // ~15 deg
                float tiltZ = (float) (Math.random() - 0.5) * 0.52f;
                sand.scale(0.6f, 0.6f, 0.6f)
                    .rotate(tiltX, 1, 0, 0)
                    .glow(0, 150, 255).interpolation(3, 0);
                sandBlocks.add(sand);
                spawnedEntities.add(sand.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Soul sand orbit: small circles radius 0.3, 120-tick period per block
            for (int i = 0; i < sandBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / RING_COUNT;
                double baseX = Math.cos(baseAngle) * 15.0;
                double baseZ = Math.sin(baseAngle) * 15.0;

                double orbitAngle = (2 * Math.PI * ticksAlive) / 120.0;
                double orbX = Math.cos(orbitAngle) * 0.3;
                double orbZ = Math.sin(orbitAngle) * 0.3;

                Location target = center.clone().add(baseX + orbX, 1.0, baseZ + orbZ);
                sandBlocks.get(i).entity().teleport(target);
            }

            // Soul fire particles from each soil block (3/sec -> every 7 ticks)
            if (ticksAlive % 7 == 0) {
                for (BlockDisplayHandle soil : soilBlocks) {
                    Location flameLoc = soil.entity().getLocation().add(0.5, 0.8, 0.5);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, flameLoc, 3, 0.2, 0.4, 0.2, 0);
                }
            }

            // Synchronized flash burst every 60 ticks: all 20 positions, 20 particles each
            if (ticksAlive % 60 == 0) {
                for (BlockDisplayHandle soil : soilBlocks) {
                    Location burstLoc = soil.entity().getLocation().add(0.5, 0.5, 0.5);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, burstLoc, 20, 0.5, 0.3, 0.5, 0.02);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFireRing(plugin); }
    }
}
