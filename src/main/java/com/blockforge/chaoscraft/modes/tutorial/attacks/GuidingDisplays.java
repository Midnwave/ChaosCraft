package com.blockforge.chaoscraft.modes.tutorial.attacks;

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
 * Tutorial Mode — GUIDING DISPLAY ATTACKS
 * 10 block display attacks that serve as visual guides and waypoints.
 * Zero damage — purely informational visual cues for new players.
 *
 * Color palette:
 * - Guide green: RGB(50, 220, 100)
 * - Beacon gold: RGB(255, 215, 0)
 * - Arrow cyan: RGB(0, 200, 240)
 * - Compass blue: RGB(60, 120, 255)
 * - Marker white: RGB(240, 240, 255)
 */
public final class GuidingDisplays {

    private GuidingDisplays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FloatingArrow(plugin));
        registry.register(new SpinningCompass(plugin));
        registry.register(new WaypointBeacon(plugin));
        registry.register(new DirectionalChevron(plugin));
        registry.register(new OrbitalGuideOrbs(plugin));
        registry.register(new QuestionMark(plugin));
        registry.register(new PathTrail(plugin));
        registry.register(new PulsingTarget(plugin));
        registry.register(new RotatingDiamond(plugin));
        registry.register(new ExclamationMark(plugin));
    }

    // ================================================================
    // 1. FLOATING ARROW — Large arrow shape pointing downward,
    //    bobs up and down with a gentle golden glow
    // ================================================================
    public static class FloatingArrow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arrowBlocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 13;
        private static final double[][] ARROW_OFFSETS = {
            {0, 4, 0},       // shaft top
            {0, 3.5, 0},     // shaft
            {0, 3, 0},       // shaft
            {0, 2.5, 0},     // shaft center
            {0, 2, 0},       // tip center
            {-0.5, 2.5, 0},  // left wing inner
            {-1, 3, 0},      // left wing mid
            {-1.5, 3.5, 0},  // left wing outer
            {-2, 4, 0},      // left wing tip
            {0.5, 2.5, 0},   // right wing inner
            {1, 3, 0},       // right wing mid
            {1.5, 3.5, 0},   // right wing outer
            {2, 4, 0},       // right wing tip
        };

        public FloatingArrow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floating_arrow", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (double[] offset : ARROW_OFFSETS) {
                Location loc = center.clone().add(offset[0], offset[1], offset[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                block.scale(0.45f, 0.45f, 0.45f)
                     .glow(255, 215, 0)
                     .interpolation(3, 0);
                arrowBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Bob up and down
            float bobOffset = (float) Math.sin(ticksAlive * 0.1) * 0.4f;

            for (int i = 0; i < arrowBlocks.size() && i < ARROW_OFFSETS.length; i++) {
                Location newLoc = c.clone().add(
                        ARROW_OFFSETS[i][0], ARROW_OFFSETS[i][1] + bobOffset, ARROW_OFFSETS[i][2]);
                arrowBlocks.get(i).entity().teleport(newLoc);
            }

            // Sparkle particles
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3 + bobOffset, 0), 5, 0.8, 255, 215, 0, 1.0f);
            }

            // Gentle chime every 2 seconds
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloatingArrow(plugin); }
    }

    // ================================================================
    // 2. SPINNING COMPASS — 4 cardinal direction arms rotating
    //    horizontally, with a diamond center block
    // ================================================================
    public static class SpinningCompass extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private BlockDisplayHandle centerBlock;
        private static final int ARM_COUNT = 4;
        private static final int BLOCKS_PER_ARM = 3;

        public SpinningCompass(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spinning_compass", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center diamond
            centerBlock = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 0), Material.DIAMOND_BLOCK);
            centerBlock.scale(0.6f, 0.6f, 0.6f)
                       .glow(60, 120, 255)
                       .interpolation(2, 0);
            spawnedEntities.add(centerBlock.entity());

            // 4 compass arms: N (red), E (cyan), S (green), W (gold)
            Material[] armMaterials = {
                Material.RED_CONCRETE, Material.CYAN_CONCRETE,
                Material.LIME_CONCRETE, Material.YELLOW_CONCRETE
            };
            for (int i = 0; i < ARM_COUNT; i++) {
                double angle = (Math.PI / 2) * i;
                for (int j = 1; j <= BLOCKS_PER_ARM; j++) {
                    double x = Math.cos(angle) * (j * 0.7);
                    double z = Math.sin(angle) * (j * 0.7);
                    BlockDisplayHandle arm = displayBuilder.spawnBlock(
                            center.clone().add(x, 2.5, z), armMaterials[i]);
                    arm.scale(0.4f, 0.3f, 0.4f)
                       .glow(0, 200, 240)
                       .interpolation(2, 0);
                    arms.add(arm);
                    spawnedEntities.add(arm.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float rotAngle = ticksAlive * 0.06f;

            // Rotate all arm blocks around center
            int idx = 0;
            for (int i = 0; i < ARM_COUNT; i++) {
                double baseAngle = (Math.PI / 2) * i + rotAngle;
                for (int j = 1; j <= BLOCKS_PER_ARM; j++) {
                    if (idx >= arms.size()) break;
                    double x = Math.cos(baseAngle) * (j * 0.7);
                    double z = Math.sin(baseAngle) * (j * 0.7);
                    arms.get(idx).entity().teleport(c.clone().add(x, 2.5, z));
                    idx++;
                }
            }

            // Rotate center block visually
            centerBlock.rotate(rotAngle, 0, 1, 0);

            // Trail particles at the north (red) arm tip
            if (ticksAlive % 4 == 0) {
                double trailAngle = rotAngle;
                Location trailLoc = c.clone().add(
                        Math.cos(trailAngle) * 2.1, 2.5, Math.sin(trailAngle) * 2.1);
                DisplayBuilder.dustParticles(trailLoc, 3, 0.2, 60, 120, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpinningCompass(plugin); }
    }

    // ================================================================
    // 3. WAYPOINT BEACON — Vertical pillar of glowing blocks with
    //    ascending particles, like a quest marker
    // ================================================================
    public static class WaypointBeacon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private static final int PILLAR_HEIGHT = 10;

        public WaypointBeacon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("waypoint_beacon", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Vertical pillar alternating emerald/sea lantern, tapering
            for (int y = 0; y < PILLAR_HEIGHT; y++) {
                Material mat = (y % 2 == 0) ? Material.EMERALD_BLOCK : Material.SEA_LANTERN;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(0, y + 1, 0), mat);
                float scaleXZ = 0.5f - (y * 0.03f);
                block.scale(scaleXZ, 0.8f, scaleXZ)
                     .glow(50, 220, 100)
                     .interpolation(3, 0);
                pillarBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Crown block at top
            BlockDisplayHandle crown = displayBuilder.spawnBlock(
                    center.clone().add(0, PILLAR_HEIGHT + 1, 0), Material.GOLD_BLOCK);
            crown.scale(0.7f, 0.7f, 0.7f)
                 .glow(255, 215, 0)
                 .interpolation(2, 0);
            pillarBlocks.add(crown);
            spawnedEntities.add(crown.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pulse scale on pillar blocks
            float pulse = 1.0f + (float) Math.sin(ticksAlive * 0.15) * 0.1f;
            for (int i = 0; i < pillarBlocks.size() - 1; i++) {
                float baseXZ = 0.5f - (i * 0.03f);
                pillarBlocks.get(i).scale(baseXZ * pulse, 0.8f, baseXZ * pulse);
            }

            // Crown rotation
            if (!pillarBlocks.isEmpty()) {
                BlockDisplayHandle crown = pillarBlocks.get(pillarBlocks.size() - 1);
                crown.rotate(ticksAlive * 0.08f, 0, 1, 0);
            }

            // Ascending particles along the pillar
            if (ticksAlive % 3 == 0) {
                double particleY = (ticksAlive * 0.3) % (PILLAR_HEIGHT + 2);
                DisplayBuilder.dustParticles(c.clone().add(0, particleY, 0), 4, 0.3, 50, 220, 100, 1.2f);
            }

            // Ring particles at base
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), 1.5, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(50, 220, 100), 1.0f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WaypointBeacon(plugin); }
    }

    // ================================================================
    // 4. DIRECTIONAL CHEVRON — V-shaped chevrons pointing forward,
    //    pulse outward rhythmically in staggered waves
    // ================================================================
    public static class DirectionalChevron extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chevronBlocks = new ArrayList<>();
        private static final int ROWS = 3;
        private static final int BLOCKS_PER_ROW = 9; // -4 to +4

        public DirectionalChevron(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("directional_chevron", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.CYAN_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.WHITE_CONCRETE};
            for (int row = 0; row < ROWS; row++) {
                float zOff = row * 1.2f;
                for (int i = -4; i <= 4; i++) {
                    double x = i * 0.5;
                    double z = -Math.abs(i) * 0.4 + zOff;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(x, 2.5, z), mats[row]);
                    block.scale(0.4f, 0.4f, 0.4f)
                         .glow(0, 200, 240)
                         .interpolation(3, 0);
                    chevronBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            int idx = 0;
            for (int row = 0; row < ROWS; row++) {
                float rowDelay = row * 0.3f;
                float rowPulse = (float) Math.sin(ticksAlive * 0.12 - rowDelay) * 0.6f;
                float zOff = row * 1.2f;
                for (int i = -4; i <= 4; i++) {
                    if (idx >= chevronBlocks.size()) break;
                    double x = i * 0.5;
                    double z = -Math.abs(i) * 0.4 + zOff + rowPulse;
                    chevronBlocks.get(idx).entity().teleport(c.clone().add(x, 2.5, z));
                    idx++;
                }
            }

            // Arrow trail particles
            if (ticksAlive % 6 == 0) {
                float pulseZ = (float) Math.sin(ticksAlive * 0.12) * 0.6f;
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, pulseZ + 2), 4, 0.3, 0, 200, 240, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DirectionalChevron(plugin); }
    }

    // ================================================================
    // 5. ORBITAL GUIDE ORBS — 8 small orbs orbiting in a tilted ring
    //    around a center emerald, different colors
    // ================================================================
    public static class OrbitalGuideOrbs extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> orbs = new ArrayList<>();
        private BlockDisplayHandle centerOrb;
        private static final int ORB_COUNT = 8;
        private static final double ORBIT_RADIUS = 2.0;
        private static final Material[] ORB_MATS = {
            Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.LAPIS_BLOCK,
            Material.REDSTONE_BLOCK, Material.PRISMARINE, Material.AMETHYST_BLOCK,
            Material.COPPER_BLOCK, Material.CYAN_CONCRETE
        };

        public OrbitalGuideOrbs(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_guide_orbs", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center emerald
            centerOrb = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.EMERALD_BLOCK);
            centerOrb.scale(0.6f, 0.6f, 0.6f)
                     .glow(50, 220, 100)
                     .interpolation(2, 0);
            spawnedEntities.add(centerOrb.entity());

            for (int i = 0; i < ORB_COUNT; i++) {
                double angle = (2 * Math.PI * i) / ORB_COUNT;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;
                BlockDisplayHandle orb = displayBuilder.spawnBlock(
                        center.clone().add(x, 3, z), ORB_MATS[i]);
                orb.scale(0.35f, 0.35f, 0.35f)
                   .glow(240, 240, 255)
                   .interpolation(2, 0);
                orbs.add(orb);
                spawnedEntities.add(orb.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float speed = ticksAlive * 0.08f;
            float tilt = 0.3f;

            for (int i = 0; i < orbs.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / ORB_COUNT + speed;
                double x = Math.cos(baseAngle) * ORBIT_RADIUS;
                double z = Math.sin(baseAngle) * ORBIT_RADIUS;
                double y = Math.sin(baseAngle) * tilt * ORBIT_RADIUS;
                orbs.get(i).entity().teleport(c.clone().add(x, 3 + y, z));
            }

            // Pulse center
            float centerPulse = 0.6f + (float) Math.sin(ticksAlive * 0.15) * 0.15f;
            centerOrb.scale(centerPulse, centerPulse, centerPulse);

            // Trailing dust
            if (ticksAlive % 4 == 0) {
                double trailAngle = speed;
                Location trailLoc = c.clone().add(
                        Math.cos(trailAngle) * ORBIT_RADIUS, 3,
                        Math.sin(trailAngle) * ORBIT_RADIUS);
                DisplayBuilder.dustParticles(trailLoc, 3, 0.15, 240, 240, 255, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalGuideOrbs(plugin); }
    }

    // ================================================================
    // 6. QUESTION MARK — Large ? shape made of lapis blocks,
    //    slowly rotates on Y axis with a glowing dot
    // ================================================================
    public static class QuestionMark extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> questionBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();
        private static final double[][] Q_OFFSETS = {
            {0, 1.5, 0},       // dot
            {0, 3.0, 0},       // stem bottom
            {0, 3.5, 0},       // stem
            {0.3, 4.0, 0},     // curve start
            {0.6, 4.5, 0},     // curve right
            {0.6, 5.0, 0},     // curve right top
            {0.3, 5.5, 0},     // top curve
            {0, 5.8, 0},       // top center
            {-0.3, 5.5, 0},    // top left
            {-0.6, 5.0, 0},    // left descend
            {-0.6, 4.5, 0},    // left bottom
        };

        public QuestionMark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("question_mark", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < Q_OFFSETS.length; i++) {
                double[] offset = Q_OFFSETS[i];
                Location loc = center.clone().add(offset[0], offset[1], offset[2]);
                Material mat = (i == 0) ? Material.GOLD_BLOCK : Material.LAPIS_BLOCK;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                float size = (i == 0) ? 0.5f : 0.45f;
                int glowR = (i == 0) ? 255 : 60;
                int glowG = (i == 0) ? 215 : 120;
                int glowB = (i == 0) ? 0 : 255;
                block.scale(size, size, size)
                     .glow(glowR, glowG, glowB)
                     .interpolation(3, 0);
                questionBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(offset);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate entire ? around Y axis
            float angle = ticksAlive * 0.05f;

            for (int i = 0; i < questionBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                questionBlocks.get(i).entity().teleport(c.clone().add(rotX, base[1], rotZ));
            }

            // Sparkle at the dot
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 3, 0.2, 255, 215, 0, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new QuestionMark(plugin); }
    }

    // ================================================================
    // 7. PATH TRAIL — Line of 12 blocks on the ground forming a
    //    dotted path, blocks light up sequentially
    // ================================================================
    public static class PathTrail extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pathBlocks = new ArrayList<>();
        private static final int PATH_LENGTH = 12;
        private static final double SPACING = 1.0;

        public PathTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("path_trail", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < PATH_LENGTH; i++) {
                Location loc = center.clone().add(0, 0.1, i * SPACING);
                Material mat = (i % 2 == 0) ? Material.SEA_LANTERN : Material.PRISMARINE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.6f, 0.15f, 0.6f)
                     .glow(50, 220, 100)
                     .interpolation(3, 0);
                pathBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sequential lighting: one block "active" at a time
            int activeIndex = (ticksAlive / 4) % PATH_LENGTH;

            for (int i = 0; i < pathBlocks.size(); i++) {
                if (i == activeIndex) {
                    pathBlocks.get(i).scale(0.7f, 0.35f, 0.7f);
                    pathBlocks.get(i).glow(255, 255, 200);
                } else {
                    pathBlocks.get(i).scale(0.6f, 0.15f, 0.6f);
                    pathBlocks.get(i).glow(50, 220, 100);
                }
            }

            // Particle at active block
            if (ticksAlive % 4 == 0) {
                Location activeLoc = c.clone().add(0, 0.3, activeIndex * SPACING);
                DisplayBuilder.dustParticles(activeLoc, 5, 0.2, 255, 255, 200, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PathTrail(plugin); }
    }

    // ================================================================
    // 8. PULSING TARGET — Concentric rings that expand and contract,
    //    bullseye pattern using red/white concrete
    // ================================================================
    public static class PulsingTarget extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ring1 = new ArrayList<>();
        private final List<BlockDisplayHandle> ring2 = new ArrayList<>();
        private final List<BlockDisplayHandle> ring3 = new ArrayList<>();
        private BlockDisplayHandle bullseye;
        private static final int RING1_COUNT = 8;
        private static final int RING2_COUNT = 12;
        private static final int RING3_COUNT = 16;

        public PulsingTarget(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pulsing_target", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location flat = center.clone().add(0, 0.2, 0);

            // Center bullseye
            bullseye = displayBuilder.spawnBlock(flat, Material.RED_CONCRETE);
            bullseye.scale(0.5f, 0.1f, 0.5f)
                    .glow(255, 50, 50)
                    .interpolation(3, 0);
            spawnedEntities.add(bullseye.entity());

            // Ring 1: white at radius 1
            for (int i = 0; i < RING1_COUNT; i++) {
                double angle = (2 * Math.PI * i) / RING1_COUNT;
                Location loc = flat.clone().add(Math.cos(angle) * 1.0, 0, Math.sin(angle) * 1.0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                block.scale(0.4f, 0.1f, 0.4f).glow(240, 240, 255).interpolation(3, 0);
                ring1.add(block);
                spawnedEntities.add(block.entity());
            }

            // Ring 2: red at radius 2
            for (int i = 0; i < RING2_COUNT; i++) {
                double angle = (2 * Math.PI * i) / RING2_COUNT;
                Location loc = flat.clone().add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                block.scale(0.4f, 0.1f, 0.4f).glow(255, 50, 50).interpolation(3, 0);
                ring2.add(block);
                spawnedEntities.add(block.entity());
            }

            // Ring 3: white at radius 3
            for (int i = 0; i < RING3_COUNT; i++) {
                double angle = (2 * Math.PI * i) / RING3_COUNT;
                Location loc = flat.clone().add(Math.cos(angle) * 3.0, 0, Math.sin(angle) * 3.0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                block.scale(0.35f, 0.1f, 0.35f).glow(240, 240, 255).interpolation(3, 0);
                ring3.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location flat = c.clone().add(0, 0.2, 0);

            float pulse1 = 1.0f + (float) Math.sin(ticksAlive * 0.12) * 0.3f;
            float pulse2 = 1.0f + (float) Math.sin(ticksAlive * 0.12 - 0.5) * 0.3f;
            float pulse3 = 1.0f + (float) Math.sin(ticksAlive * 0.12 - 1.0) * 0.3f;

            moveRing(flat, ring1, RING1_COUNT, 1.0 * pulse1);
            moveRing(flat, ring2, RING2_COUNT, 2.0 * pulse2);
            moveRing(flat, ring3, RING3_COUNT, 3.0 * pulse3);

            // Bullseye pulse
            float bPulse = 0.5f + (float) Math.sin(ticksAlive * 0.2) * 0.15f;
            bullseye.scale(bPulse, 0.1f, bPulse);
        }

        private void moveRing(Location center, List<BlockDisplayHandle> ring, int count, double radius) {
            for (int i = 0; i < ring.size(); i++) {
                double angle = (2 * Math.PI * i) / count;
                Location loc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                ring.get(i).entity().teleport(loc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PulsingTarget(plugin); }
    }

    // ================================================================
    // 9. ROTATING DIAMOND — Large octahedron shape made of diamond
    //    and prismarine blocks, slowly spinning on Y axis
    // ================================================================
    public static class RotatingDiamond extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> diamondBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();
        private static final double[][] DIAMOND_OFFSETS = {
            // Top pyramid
            {0, 3, 0},           // apex
            {0.6, 2.2, 0},      // mid ring
            {-0.6, 2.2, 0},
            {0, 2.2, 0.6},
            {0, 2.2, -0.6},
            {1.0, 1.5, 0},      // widest ring
            {-1.0, 1.5, 0},
            {0, 1.5, 1.0},
            {0, 1.5, -1.0},
            {0.7, 1.5, 0.7},
            {-0.7, 1.5, 0.7},
            {0.7, 1.5, -0.7},
            {-0.7, 1.5, -0.7},
            // Bottom pyramid
            {0.6, 0.8, 0},
            {-0.6, 0.8, 0},
            {0, 0.8, 0.6},
            {0, 0.8, -0.6},
            {0, 0, 0},          // bottom apex
        };

        public RotatingDiamond(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rotating_diamond", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.DIAMOND_BLOCK, Material.PRISMARINE};
            for (int i = 0; i < DIAMOND_OFFSETS.length; i++) {
                Location loc = center.clone().add(
                        DIAMOND_OFFSETS[i][0], DIAMOND_OFFSETS[i][1] + 2, DIAMOND_OFFSETS[i][2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % 2]);
                block.scale(0.4f, 0.4f, 0.4f)
                     .glow(0, 200, 240)
                     .interpolation(3, 0);
                diamondBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(DIAMOND_OFFSETS[i]);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.06f;
            float bob = (float) Math.sin(ticksAlive * 0.08) * 0.3f;

            for (int i = 0; i < diamondBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                diamondBlocks.get(i).entity().teleport(
                        c.clone().add(rotX, base[1] + 2 + bob, rotZ));
            }

            // Sparkle particles at the top
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 5 + bob, 0), 4, 0.5, 0, 200, 240, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RotatingDiamond(plugin); }
    }

    // ================================================================
    // 10. EXCLAMATION MARK — Large ! shape made of red/gold blocks,
    //     pulses scale to draw attention, side accents
    // ================================================================
    public static class ExclamationMark extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> barBlocks = new ArrayList<>();
        private BlockDisplayHandle dotBlock;
        private final List<BlockDisplayHandle> accentBlocks = new ArrayList<>();

        public ExclamationMark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("exclamation_mark", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Vertical bar (6 blocks)
            for (int y = 0; y < 6; y++) {
                Location loc = center.clone().add(0, 3 + y * 0.6, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                block.scale(0.5f, 0.5f, 0.5f)
                     .glow(255, 60, 60)
                     .interpolation(3, 0);
                barBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Dot (gold)
            dotBlock = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.GOLD_BLOCK);
            dotBlock.scale(0.55f, 0.55f, 0.55f)
                    .glow(255, 215, 0)
                    .interpolation(3, 0);
            spawnedEntities.add(dotBlock.entity());

            // Side accents (orange)
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(side * 0.5, 4 + y * 0.6, 0);
                    BlockDisplayHandle accent = displayBuilder.spawnBlock(loc, Material.ORANGE_CONCRETE);
                    accent.scale(0.25f, 0.35f, 0.25f)
                          .glow(255, 150, 30)
                          .interpolation(3, 0);
                    accentBlocks.add(accent);
                    spawnedEntities.add(accent.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pulse all blocks
            float pulse = 1.0f + (float) Math.sin(ticksAlive * 0.18) * 0.2f;
            for (BlockDisplayHandle bar : barBlocks) {
                bar.scale(0.5f * pulse, 0.5f * pulse, 0.5f * pulse);
            }
            dotBlock.scale(0.55f * pulse, 0.55f * pulse, 0.55f * pulse);
            for (BlockDisplayHandle accent : accentBlocks) {
                accent.scale(0.25f * pulse, 0.35f * pulse, 0.25f * pulse);
            }

            // Alert particles
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 5, 0), 5, 0.5, 255, 60, 60, 1.2f);
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 3, 0.3, 255, 215, 0, 1.0f);
            }

            // Alert sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ExclamationMark(plugin); }
    }
}
