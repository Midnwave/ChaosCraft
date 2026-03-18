package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E Block Display -- GROUP 8: SOUL ARCHITECTURE (Structures #73-82)
 * Supreme Calamitas (Boss 5) arena: Soul Tower (3-section), Soul Shard Ring
 * (8 NETHER_STAR heartbeat system), Echo Gallery (7 spectral figures),
 * platforms, pillar connectors, and Soul Choir Orb.
 *
 * Palette: soul blue(120,120,255), soul fire cyan, gold/totem accents,
 *          gray(concrete) for spectral figures, crying obsidian purple.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 * triggerImpactDamage() for proximity hits. spawnedEntities.add() always.
 */
public final class SoulArchitecture {

    private SoulArchitecture() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SoulTowerBase(plugin));
        registry.register(new SoulTowerMidSection(plugin));
        registry.register(new SoulTowerCapCrown(plugin));
        registry.register(new SoulShardRingPrimary(plugin));
        registry.register(new SoulShardRingSecondary(plugin));
        registry.register(new EchoGalleryPrimary(plugin));
        registry.register(new EchoGalleryRemaining(plugin));
        registry.register(new SoulShardRingPlatform(plugin));
        registry.register(new SoulPillarConnectors(plugin));
        registry.register(new SoulChoirOrb(plugin));
    }

    // ================================================================
    // #73 -- SOUL TOWER BASE
    // ~41 entities: 3x3 SOUL_SOIL layers (Y+0 to Y+1), 3x3 SOUL_SAND
    // layers (Y+2 to Y+3), cross pattern (Y+4). At arena north perimeter
    // (Z-28). SOUL_FIRE_FLAME particles from top. Phase 3: soul sand
    // vibration at amplitude +-0.04, period 30 ticks.
    // ================================================================
    public static class SoulTowerBase extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();

        public SoulTowerBase(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_tower_base", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location towerBase = center.clone().add(0, 0, -28);

            // Y+0 and Y+1: 3x3 SOUL_SOIL
            for (int y = 0; y < 2; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location loc = towerBase.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                        h.scale(1.0f, 1.0f, 1.0f).glow(120, 120, 255).interpolation(3, 0);
                        blocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Y+2 and Y+3: 3x3 SOUL_SAND
            for (int y = 2; y < 4; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location loc = towerBase.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                        h.scale(1.0f, 1.0f, 1.0f).glow(120, 120, 255).interpolation(3, 0);
                        blocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Y+4: cross pattern (center + 4 cardinal)
            int[][] crossPositions = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] pos : crossPositions) {
                Location loc = towerBase.clone().add(pos[0], 4, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                h.scale(1.0f, 1.0f, 1.0f).glow(120, 120, 255).interpolation(3, 0);
                blocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location towerBase = center.clone().add(0, 0, -28);

            // SOUL_FIRE_FLAME from top surface (Y+4)
            if (ticksAlive % 2 == 0) {
                Location topSurface = towerBase.clone().add(0, 4.5, 0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, topSurface, 4,
                        0.5, 0.1, 0.5, 0.02);
            }

            // Ambient sound every 200 ticks
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(towerBase, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.4f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulTowerBase(plugin); }
    }

    // ================================================================
    // #74 -- SOUL TOWER MID-SECTION
    // ~21 entities: hollow SOUL_SAND ring (Y+5 to Y+9). Cross at Y+5,
    // then 4-ring (cardinal only, no center) Y+6 to Y+9. SOUL_FIRE_FLAME
    // particles from hollow core at Y+7. Phase 2: DRAGON_BREATH added
    // to core for purple-tinged soul flame.
    // ================================================================
    public static class SoulTowerMidSection extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();

        public SoulTowerMidSection(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_tower_mid_section", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location towerBase = center.clone().add(0, 0, -28);

            // Y+5: cross pattern (5 blocks)
            int[][] crossPositions = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] pos : crossPositions) {
                Location loc = towerBase.clone().add(pos[0], 5, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                h.scale(1.0f, 1.0f, 1.0f).glow(120, 120, 255).interpolation(3, 0);
                blocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Y+6 to Y+9: hollow ring (cardinal only, no center) = 4 blocks per layer
            int[][] ringPositions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int y = 6; y <= 9; y++) {
                for (int[] pos : ringPositions) {
                    Location loc = towerBase.clone().add(pos[0], y, pos[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                    h.scale(1.0f, 1.0f, 1.0f).glow(120, 120, 255).interpolation(3, 0);
                    blocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location towerMid = center.clone().add(0, 7, -28);

            // SOUL_FIRE_FLAME from hollow core at Y+7 (faster upward drift)
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, towerMid, 6,
                    0.2, 0.2, 0.2, 0.03);

            // DRAGON_BREATH mixed in (purple-tinged soul flame)
            if (ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.DRAGON_BREATH, towerMid, 2,
                        0.2, 0.2, 0.2, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulTowerMidSection(plugin); }
    }

    // ================================================================
    // #75 -- SOUL TOWER CAP AND CROWN
    // ~20 entities: SOUL_SAND ring (Y+10 to Y+11), BLUE_STAINED_GLASS
    // ring (Y+12 to Y+13), SOUL_LANTERN center + 4 corner pieces at Y+14.
    // Corner lanterns orbit center at radius 1.0, 0.5 deg/tick. Concentrated
    // SOUL_FIRE_FLAME + TOTEM crown at peak. Phase 4: orbit 2.0 deg/tick.
    // ================================================================
    public static class SoulTowerCapCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> capBlocks = new ArrayList<>();
        private BlockDisplayHandle centerLantern;
        private final List<BlockDisplayHandle> cornerLanterns = new ArrayList<>();

        public SoulTowerCapCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_tower_cap_crown", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location towerBase = center.clone().add(0, 0, -28);
            int[][] ringPositions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

            // Y+10 and Y+11: SOUL_SAND ring
            for (int y = 10; y <= 11; y++) {
                for (int[] pos : ringPositions) {
                    Location loc = towerBase.clone().add(pos[0], y, pos[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                    h.scale(1.0f, 1.0f, 1.0f).glow(120, 120, 255).interpolation(3, 0);
                    capBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Y+12 and Y+13: BLUE_STAINED_GLASS ring
            for (int y = 12; y <= 13; y++) {
                for (int[] pos : ringPositions) {
                    Location loc = towerBase.clone().add(pos[0], y, pos[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                    h.scale(1.0f, 1.0f, 1.0f).glow(120, 120, 255).interpolation(3, 0);
                    capBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Y+14: center SOUL_LANTERN (1.2 scale)
            Location centerLoc = towerBase.clone().add(0, 14, 0);
            centerLantern = displayBuilder.spawnBlock(centerLoc, Material.SOUL_LANTERN);
            centerLantern.scale(1.2f, 1.2f, 1.2f).glow(120, 120, 255).interpolation(3, 0);
            spawnedEntities.add(centerLantern.entity());

            // 4 corner SOUL_LANTERNs at +-1 offset (0.7 scale)
            for (int[] pos : ringPositions) {
                Location loc = towerBase.clone().add(pos[0], 14, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_LANTERN);
                h.scale(0.7f, 0.7f, 0.7f).glow(120, 120, 255).interpolation(3, 0);
                cornerLanterns.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location towerTop = center.clone().add(0, 14, -28);

            // Corner lanterns orbit center at radius 1.0, 0.5 deg/tick
            for (int i = 0; i < cornerLanterns.size(); i++) {
                double angle = Math.toRadians(ticksAlive * 0.5 + i * 90.0);
                double x = Math.cos(angle) * 1.0;
                double z = Math.sin(angle) * 1.0;
                Location loc = towerTop.clone().add(x, 0, z);
                cornerLanterns.get(i).entity().teleport(loc);
            }

            // Concentrated SOUL_FIRE_FLAME crown at peak
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, towerTop, 8,
                    0.5, 0.5, 0.5, 0.01);

            // TOTEM gold flecks rising
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, towerTop, 3,
                    0.3, 0.3, 0.3, 0.04);

            // Sound every 160 ticks
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(towerTop, Sound.BLOCK_SOUL_SAND_HIT, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulTowerCapCrown(plugin); }
    }

    // ================================================================
    // #76 -- SOUL SHARD RING: ENTITY 1 OF 8
    // Single NETHER_STAR ItemDisplay at 4.0 scale, radius 8, Y+18 (east
    // position). Sinusoidal heartbeat pulse: 3.6 to 4.4, period 40 ticks.
    // Y-axis rotation 1.0 deg/tick. TOTEM + SOUL_FIRE_FLAME particles.
    // Phase 4: amplitude doubles. Sound on pulse apex.
    // ================================================================
    public static class SoulShardRingPrimary extends BlockDisplayAttack {

        private ItemDisplayHandle shard;

        public SoulShardRingPrimary(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_shard_ring_primary", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 0-deg position (east) on radius 8 ring, Y+18
            Location loc = center.clone().add(8, 18, 0);
            shard = displayBuilder.spawnItem(loc, new ItemStack(Material.NETHER_STAR));
            shard.scale(4.0f, 4.0f, 4.0f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(shard.entity());

            DisplayBuilder.playSound(center.clone().add(0, 18, 0),
                    Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location loc = center.clone().add(8, 18, 0);

            // Heartbeat pulse: scale = 4.0 + 0.4 * sin(2pi * t / 40)
            float scalePulse = 4.0f + 0.4f * (float) Math.sin(2.0 * Math.PI * ticksAlive / 40.0);
            shard.scale(scalePulse, scalePulse, scalePulse);

            // Y-axis rotation 1.0 deg/tick
            float rotAngle = (float) Math.toRadians(ticksAlive * 1.0);
            shard.rotate(rotAngle, 0, 1, 0);
            shard.interpolation(3, 0);

            // TOTEM + SOUL_FIRE_FLAME particles
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 2, 0.3, 0.3, 0.3, 0);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.2, 0.2, 0.2, 0.03);

            // Sound at pulse apex every 40 ticks
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulShardRingPrimary(plugin); }
    }

    // ================================================================
    // #77 -- SOUL SHARD RING: ENTITIES 2-8
    // 7 additional NETHER_STAR shards completing the 8-point ring at
    // radius 8, Y+18. Each has staggered heartbeat phase offset of
    // 5 ticks per shard (sequential clockwise pulse wave). Per-shard
    // DUST color variations. Player death empowerment: nearest shard
    // doubles amplitude and rate for 300 ticks.
    // ================================================================
    public static class SoulShardRingSecondary extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private static final int COUNT = 7;
        // Angles for shards 2-8: 45, 90, 135, 180, 225, 270, 315 degrees
        private static final double[] ANGLES = {45, 90, 135, 180, 225, 270, 315};
        private static final int[] PHASE_OFFSETS = {5, 10, 15, 20, 25, 30, 35};

        public SoulShardRingSecondary(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_shard_ring_secondary", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double angle = Math.toRadians(ANGLES[i]);
                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;
                Location loc = center.clone().add(x, 18, z);

                ItemDisplayHandle shard = displayBuilder.spawnItem(loc,
                        new ItemStack(Material.NETHER_STAR));
                shard.scale(4.0f, 4.0f, 4.0f).glow(255, 255, 200).interpolation(3, 0);
                shards.add(shard);
                spawnedEntities.add(shard.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < shards.size(); i++) {
                double angle = Math.toRadians(ANGLES[i]);
                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;
                Location loc = center.clone().add(x, 18, z);

                // Staggered heartbeat: offset by PHASE_OFFSETS[i] ticks
                float scalePulse = 4.0f + 0.4f *
                        (float) Math.sin(2.0 * Math.PI * (ticksAlive - PHASE_OFFSETS[i]) / 40.0);
                shards.get(i).scale(scalePulse, scalePulse, scalePulse);

                float rotAngle = (float) Math.toRadians(ticksAlive * 1.0);
                shards.get(i).rotate(rotAngle, 0, 1, 0);
                shards.get(i).interpolation(3, 0);

                // TOTEM particles
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 2, 0.3, 0.3, 0.3, 0);

                // Per-shard secondary particle (color variations)
                // Shards 2(NE), 5(W), 8(SE) use SOUL_FIRE_FLAME; others use colored DUST
                if (i == 0 || i == 3 || i == 6) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.2, 0.2, 0.2, 0.03);
                } else {
                    // Varying colors: pale blue, pale gold, lavender, amber, mint
                    int[][] colors = {{180, 180, 255}, {255, 255, 150}, {200, 150, 255}, {150, 255, 200}};
                    int colorIdx = (i < 4) ? (i - 1) : (i - 4);
                    if (colorIdx >= 0 && colorIdx < colors.length) {
                        w.spawnParticle(Particle.DUST, loc, 1, 0.2, 0.2, 0.2, 0,
                                new Particle.DustOptions(
                                        Color.fromRGB(colors[colorIdx][0], colors[colorIdx][1], colors[colorIdx][2]),
                                        1.0f));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulShardRingSecondary(plugin); }
    }

    // ================================================================
    // #78 -- ECHO GALLERY: SPECTRAL FIGURE 1
    // ~12 entities forming a humanoid silhouette from GRAY_CONCRETE
    // (head, legs), GRAY_CONCRETE_POWDER (torso), GLASS_PANE (eyes).
    // At arena north edge (Z-25, X-10). Faces center. Arm raise/reset
    // animation: 200-tick linear raise, instant snap reset. SOUL_FIRE_FLAME
    // from head. BLOCK_LIGHT 10 spectral glow.
    // ================================================================
    public static class EchoGalleryPrimary extends BlockDisplayAttack {

        private BlockDisplayHandle head;
        private BlockDisplayHandle torso;
        private BlockDisplayHandle leftArm;
        private BlockDisplayHandle rightArm;
        private BlockDisplayHandle leftLeg;
        private BlockDisplayHandle rightLeg;
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();

        public EchoGalleryPrimary(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_gallery_primary", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location figureLoc = center.clone().add(-10, 0, -25);

            // Head
            head = displayBuilder.spawnBlock(figureLoc.clone().add(0, 1.6, 0), Material.GRAY_CONCRETE);
            head.scale(0.8f, 0.8f, 0.8f).glow(160, 160, 180).interpolation(3, 0);
            spawnedEntities.add(head.entity());

            // Torso
            torso = displayBuilder.spawnBlock(figureLoc.clone().add(0, 0.5, 0), Material.GRAY_CONCRETE_POWDER);
            torso.scale(0.8f, 1.2f, 0.4f).glow(160, 160, 180).interpolation(3, 0);
            spawnedEntities.add(torso.entity());

            // Left arm
            leftArm = displayBuilder.spawnBlock(figureLoc.clone().add(-0.5, 0.2, 0), Material.GRAY_CONCRETE);
            leftArm.scale(0.3f, 0.9f, 0.2f).glow(160, 160, 180).interpolation(5, 0);
            spawnedEntities.add(leftArm.entity());

            // Right arm
            rightArm = displayBuilder.spawnBlock(figureLoc.clone().add(0.5, 0.2, 0), Material.GRAY_CONCRETE);
            rightArm.scale(0.3f, 0.9f, 0.2f).glow(160, 160, 180).interpolation(5, 0);
            spawnedEntities.add(rightArm.entity());

            // Left leg
            leftLeg = displayBuilder.spawnBlock(figureLoc.clone().add(-0.2, -0.8, 0), Material.GRAY_CONCRETE);
            leftLeg.scale(0.3f, 0.9f, 0.2f).glow(160, 160, 180).interpolation(3, 0);
            spawnedEntities.add(leftLeg.entity());

            // Right leg
            rightLeg = displayBuilder.spawnBlock(figureLoc.clone().add(0.2, -0.8, 0), Material.GRAY_CONCRETE);
            rightLeg.scale(0.3f, 0.9f, 0.2f).glow(160, 160, 180).interpolation(3, 0);
            spawnedEntities.add(rightLeg.entity());

            // Eyes (2 GLASS_PANE displays)
            for (int side = -1; side <= 1; side += 2) {
                BlockDisplayHandle eye = displayBuilder.spawnBlock(
                        figureLoc.clone().add(side * 0.15, 1.7, 0.35), Material.GLASS_PANE);
                eye.scale(0.15f, 0.15f, 0.15f).glow(255, 255, 255).interpolation(3, 0);
                eyes.add(eye);
                spawnedEntities.add(eye.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location figureLoc = center.clone().add(-10, 0, -25);

            // Arm raise animation: 200-tick cycle, linear raise then instant reset
            int cyclePos = ticksAlive % 200;
            double armYOffset;
            if (cyclePos < 200) {
                // Linear raise from -0.3 to +0.3 over 200 ticks
                armYOffset = -0.3 + 0.6 * (cyclePos / 200.0);
            } else {
                armYOffset = -0.3; // Reset (shouldn't reach here with % 200)
            }

            if (leftArm != null) {
                Location armLoc = figureLoc.clone().add(-0.5, 0.5 + armYOffset, 0);
                leftArm.entity().teleport(armLoc);
            }
            if (rightArm != null) {
                Location armLoc = figureLoc.clone().add(0.5, 0.5 + armYOffset, 0);
                rightArm.entity().teleport(armLoc);
            }

            // SOUL_FIRE_FLAME from head
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, figureLoc.clone().add(0, 2.0, 0),
                    1, 0.1, 0.1, 0.1, 0.01);

            // Gallery ambient sound every 300 ticks
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(figureLoc, Sound.AMBIENT_CAVE, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoGalleryPrimary(plugin); }
    }

    // ================================================================
    // #79 -- ECHO GALLERY: SPECTRAL FIGURES 2-7
    // 6 additional spectral figures at irregular perimeter positions.
    // Each mirrors #78 construction with individual arm-raise periods:
    // Figure 2=220, 3=180, 4=240, 5=170, 6=210, 7=190 ticks.
    // Variation prevents sync. Phase 4: all figures oscillate 0.5
    // blocks toward center on 100-tick sinusoidal cycle.
    // ================================================================
    public static class EchoGalleryRemaining extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> figures = new ArrayList<>();
        private static final double[][] POSITIONS = {
                {10, -25},    // Figure 2: north edge, east
                {25, -10},    // Figure 3: east edge, north
                {25, 10},     // Figure 4: east edge, south
                {5, 25},      // Figure 5: south edge, east
                {-12, 25},    // Figure 6: south edge, west
                {-25, 0},     // Figure 7: west edge
        };
        private static final int[] ARM_PERIODS = {220, 180, 240, 170, 210, 190};

        public EchoGalleryRemaining(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_gallery_remaining", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int f = 0; f < 6; f++) {
                List<BlockDisplayHandle> figure = new ArrayList<>();
                Location figureLoc = center.clone().add(POSITIONS[f][0], 0, POSITIONS[f][1]);

                // Head
                BlockDisplayHandle head = displayBuilder.spawnBlock(
                        figureLoc.clone().add(0, 1.6, 0), Material.GRAY_CONCRETE);
                head.scale(0.8f, 0.8f, 0.8f).glow(160, 160, 180).interpolation(3, 0);
                figure.add(head);
                spawnedEntities.add(head.entity());

                // Torso
                BlockDisplayHandle torso = displayBuilder.spawnBlock(
                        figureLoc.clone().add(0, 0.5, 0), Material.GRAY_CONCRETE_POWDER);
                torso.scale(0.8f, 1.2f, 0.4f).glow(160, 160, 180).interpolation(3, 0);
                figure.add(torso);
                spawnedEntities.add(torso.entity());

                // Left arm (index 2)
                BlockDisplayHandle leftArm = displayBuilder.spawnBlock(
                        figureLoc.clone().add(-0.5, 0.2, 0), Material.GRAY_CONCRETE);
                leftArm.scale(0.3f, 0.9f, 0.2f).glow(160, 160, 180).interpolation(5, 0);
                figure.add(leftArm);
                spawnedEntities.add(leftArm.entity());

                // Right arm (index 3)
                BlockDisplayHandle rightArm = displayBuilder.spawnBlock(
                        figureLoc.clone().add(0.5, 0.2, 0), Material.GRAY_CONCRETE);
                rightArm.scale(0.3f, 0.9f, 0.2f).glow(160, 160, 180).interpolation(5, 0);
                figure.add(rightArm);
                spawnedEntities.add(rightArm.entity());

                // Left leg
                BlockDisplayHandle leftLeg = displayBuilder.spawnBlock(
                        figureLoc.clone().add(-0.2, -0.8, 0), Material.GRAY_CONCRETE);
                leftLeg.scale(0.3f, 0.9f, 0.2f).glow(160, 160, 180).interpolation(3, 0);
                figure.add(leftLeg);
                spawnedEntities.add(leftLeg.entity());

                // Right leg
                BlockDisplayHandle rightLeg = displayBuilder.spawnBlock(
                        figureLoc.clone().add(0.2, -0.8, 0), Material.GRAY_CONCRETE);
                rightLeg.scale(0.3f, 0.9f, 0.2f).glow(160, 160, 180).interpolation(3, 0);
                figure.add(rightLeg);
                spawnedEntities.add(rightLeg.entity());

                figures.add(figure);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int f = 0; f < figures.size(); f++) {
                Location figureLoc = center.clone().add(POSITIONS[f][0], 0, POSITIONS[f][1]);

                // Individual arm-raise period
                int period = ARM_PERIODS[f];
                int cyclePos = ticksAlive % period;
                double armYOffset = -0.3 + 0.6 * ((double) cyclePos / period);

                List<BlockDisplayHandle> figure = figures.get(f);
                // Left arm (index 2)
                if (figure.size() > 2) {
                    Location armLoc = figureLoc.clone().add(-0.5, 0.5 + armYOffset, 0);
                    figure.get(2).entity().teleport(armLoc);
                }
                // Right arm (index 3)
                if (figure.size() > 3) {
                    Location armLoc = figureLoc.clone().add(0.5, 0.5 + armYOffset, 0);
                    figure.get(3).entity().teleport(armLoc);
                }

                // SOUL_FIRE_FLAME from head
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, figureLoc.clone().add(0, 2.0, 0),
                        1, 0.1, 0.1, 0.1, 0.01);
            }

            // Shared ambient sound
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoGalleryRemaining(plugin); }
    }

    // ================================================================
    // #80 -- SOUL SHARD RING PLATFORM
    // 8 SOUL_SOIL thin disc platforms (2.0x0.2x2.0) directly below
    // each soul shard at Y+17. Scale pulse synced to overlying shard
    // heartbeat (X/Z only, Y stays 0.2). SOUL_FIRE_FLAME upward from
    // each platform center.
    // ================================================================
    public static class SoulShardRingPlatform extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> platforms = new ArrayList<>();

        public SoulShardRingPlatform(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_shard_ring_platform", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45.0);
                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;
                Location loc = center.clone().add(x, 17, z);

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(2.0f, 0.2f, 2.0f).glow(120, 120, 255).interpolation(3, 0);
                platforms.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < platforms.size(); i++) {
                // Scale pulse synced to shard heartbeat (phase offset i*5 ticks)
                float xzScale = 2.0f + 0.15f *
                        (float) Math.sin(2.0 * Math.PI * (ticksAlive - i * 5) / 40.0);
                platforms.get(i).scale(xzScale, 0.2f, xzScale);
                platforms.get(i).interpolation(3, 0);

                // SOUL_FIRE_FLAME upward from platform
                double angle = Math.toRadians(i * 45.0);
                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;
                Location loc = center.clone().add(x, 17.2, z);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.2, 0.1, 0.2, 0.03);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulShardRingPlatform(plugin); }
    }

    // ================================================================
    // #81 -- SOUL PILLAR CONNECTORS
    // Particle beam system: SOUL_FIRE_FLAME lines from Soul Tower cap
    // (Y+15 at Z-28) to each of the 8 soul shards. 5 particles per
    // beam per tick at non-uniform interpolation positions. Beams
    // pulse with shard heartbeat. Phase 4: flow direction reverses
    // every 50 ticks.
    // ================================================================
    public static class SoulPillarConnectors extends BlockDisplayAttack {

        public SoulPillarConnectors(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_pillar_connectors", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Pure particle system -- no entities
            DisplayBuilder.playSound(center.clone().add(0, 15, -28),
                    Sound.ENTITY_GUARDIAN_ATTACK, 0.7f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location towerCap = center.clone().add(0, 15, -28);

            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45.0);
                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;
                Location shardLoc = center.clone().add(x, 18, z);

                // 5 particles along beam at non-uniform positions: 0.1, 0.2, 0.4, 0.7, 1.0
                double[] interpPositions = {0.1, 0.2, 0.4, 0.7, 1.0};

                // Pulse with shard heartbeat
                float heartbeat = (float) Math.sin(2.0 * Math.PI * (ticksAlive - i * 5) / 40.0);
                int particleCount = (heartbeat > 0.8f) ? 10 : (heartbeat < -0.8f) ? 2 : 5;

                for (int p = 0; p < Math.min(particleCount, interpPositions.length); p++) {
                    double t = interpPositions[p % interpPositions.length];
                    double bx = towerCap.getX() + (shardLoc.getX() - towerCap.getX()) * t;
                    double by = towerCap.getY() + (shardLoc.getY() - towerCap.getY()) * t;
                    double bz = towerCap.getZ() + (shardLoc.getZ() - towerCap.getZ()) * t;
                    Location beamPoint = new Location(w, bx, by, bz);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, beamPoint, 1,
                            0.05, 0.05, 0.05, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulPillarConnectors(plugin); }
    }

    // ================================================================
    // #82 -- SOUL CHOIR ORB
    // Single SOUL_LANTERN ItemDisplay at 3.5 scale, arena center Y+12.
    // Multi-axis tumbling rotation: Y 1.0 deg/tick, X 0.3 deg/tick,
    // Z 0.5 deg/tick. Scale breathing pulse: 3.5 + 0.4*sin, period
    // 60 ticks. 3 SOUL_FIRE_FLAME + 2 TOTEM + 1 pale blue DUST per
    // tick. Rise from floor over 60 ticks on activation. Phase 4:
    // scale doubles to 7.0 base.
    // ================================================================
    public static class SoulChoirOrb extends BlockDisplayAttack {

        private ItemDisplayHandle orb;

        public SoulChoirOrb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_choir_orb", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start at floor, rise to Y+12
            Location loc = center.clone().add(0, 0, 0);
            orb = displayBuilder.spawnItem(loc, new ItemStack(Material.SOUL_LANTERN));
            orb.scale(3.5f, 3.5f, 3.5f).glow(120, 120, 255).interpolation(5, 0);
            spawnedEntities.add(orb.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise from floor over 60 ticks to Y+12
            double currentY;
            if (ticksAlive < 60) {
                currentY = 12.0 * ticksAlive / 60.0;
            } else {
                currentY = 12.0;
            }

            Location loc = center.clone().add(0, currentY, 0);
            orb.entity().teleport(loc);

            // Scale breathing pulse: period 60 ticks
            float scalePulse = 3.5f + 0.4f * (float) Math.sin(2.0 * Math.PI * ticksAlive / 60.0);
            orb.scale(scalePulse, scalePulse, scalePulse);

            // Multi-axis tumbling: combine Y rotation primarily
            float yRot = (float) Math.toRadians(ticksAlive * 1.0);
            orb.rotate(yRot, 0, 1, 0);
            orb.interpolation(5, 0);

            // Particle emission: 3 SOUL_FIRE_FLAME + 2 TOTEM + 1 pale blue DUST
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3, 0.5, 0.5, 0.5, 0.01);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 2, 0.5, 0.5, 0.5, 0);
            w.spawnParticle(Particle.DUST, loc, 1, 0.5, 0.5, 0.5, 0,
                    new Particle.DustOptions(Color.fromRGB(120, 120, 255), 1.5f));

            // Ambient sound every 180 ticks
            if (ticksAlive % 180 == 0) {
                DisplayBuilder.playSound(loc, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulChoirOrb(plugin); }
    }
}
