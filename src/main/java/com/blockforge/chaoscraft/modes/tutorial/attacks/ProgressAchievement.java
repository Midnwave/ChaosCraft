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
 * Tutorial Mode — PROGRESS & ACHIEVEMENT ATTACKS
 * 10 block display attacks showing progress indicators and achievement symbols.
 * Zero damage — celebratory visual feedback for completing tutorial steps.
 *
 * Color palette:
 * - Achievement gold: RGB(255, 215, 0)
 * - Star white: RGB(255, 255, 220)
 * - Success green: RGB(50, 200, 80)
 * - Badge blue: RGB(70, 130, 255)
 * - Crown purple: RGB(180, 80, 255)
 */
public final class ProgressAchievement {

    private ProgressAchievement() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new Checkmark(plugin));
        registry.register(new NumberOne(plugin));
        registry.register(new StarBadge(plugin));
        registry.register(new TrophyCup(plugin));
        registry.register(new MedalRibbon(plugin));
        registry.register(new ProgressBar(plugin));
        registry.register(new AscendingSteps(plugin));
        registry.register(new CrownDisplay(plugin));
        registry.register(new ThumbsUp(plugin));
        registry.register(new LevelUpRing(plugin));
    }

    // ================================================================
    // 1. CHECKMARK — Large checkmark (tick) shape in emerald blocks,
    //    pops in with a scale animation then pulses
    // ================================================================
    public static class Checkmark extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> checkBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public Checkmark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("checkmark", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Checkmark shape: short left arm going down-left, long right arm going up-right
            double[][] offsets = {
                // Left arm (short, descending)
                {-1.2, 4.2, 0},
                {-0.8, 3.8, 0},
                {-0.4, 3.4, 0},
                // Bottom vertex
                {0, 3.0, 0},
                // Right arm (long, ascending)
                {0.4, 3.5, 0},
                {0.8, 4.0, 0},
                {1.2, 4.5, 0},
                {1.6, 5.0, 0},
                {2.0, 5.5, 0},
                {2.4, 6.0, 0},
            };

            for (double[] off : offsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.EMERALD_BLOCK);
                block.scale(0.01f, 0.01f, 0.01f) // Start tiny for pop-in
                     .glow(50, 200, 80)
                     .interpolation(6, 0);
                checkBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pop-in animation: scale from 0 to full over first 10 ticks
            float scaleFactor;
            if (ticksAlive < 10) {
                scaleFactor = ticksAlive / 10.0f;
            } else {
                scaleFactor = 1.0f + (float) Math.sin(ticksAlive * 0.15) * 0.1f;
            }

            float blockSize = 0.5f * scaleFactor;
            for (BlockDisplayHandle block : checkBlocks) {
                block.scale(blockSize, blockSize, blockSize);
            }

            // Success particles
            if (ticksAlive % 6 == 0 && ticksAlive > 10) {
                DisplayBuilder.dustParticles(c.clone().add(0.6, 4.5, 0), 4, 1.0, 50, 200, 80, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Checkmark(plugin); }
    }

    // ================================================================
    // 2. NUMBER ONE — Large "1" shape in gold blocks,
    //    with a pedestal base, slowly rotating
    // ================================================================
    public static class NumberOne extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> numBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public NumberOne(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("number_one", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Number "1" shape
            // Vertical stroke
            double[][] strokeOffsets = {
                {0, 6, 0}, {0, 5.5, 0}, {0, 5, 0}, {0, 4.5, 0},
                {0, 4, 0}, {0, 3.5, 0}, {0, 3, 0},
            };
            for (double[] off : strokeOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.5f, 0.45f, 0.3f)
                     .glow(255, 215, 0)
                     .interpolation(3, 0);
                numBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Serif at top (diagonal)
            double[][] serifOffsets = {{-0.4, 5.7, 0}, {-0.2, 5.85, 0}};
            for (double[] off : serifOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.35f, 0.35f, 0.3f)
                     .glow(255, 200, 0)
                     .interpolation(3, 0);
                numBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Base (wider)
            double[][] baseBlockOffsets = {{-0.5, 2.8, 0}, {0.5, 2.8, 0}};
            for (double[] off : baseBlockOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.4f, 0.25f, 0.3f)
                     .glow(255, 215, 0)
                     .interpolation(3, 0);
                numBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Pedestal
            BlockDisplayHandle pedestal = displayBuilder.spawnBlock(
                    center.clone().add(0, 2.3, 0), Material.QUARTZ_BLOCK);
            pedestal.scale(1.2f, 0.3f, 0.8f)
                    .glow(240, 240, 255)
                    .interpolation(3, 0);
            numBlocks.add(pedestal);
            spawnedEntities.add(pedestal.entity());
            baseOffsets.add(new double[]{0, 2.3, 0});

            DisplayBuilder.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.04f;

            for (int i = 0; i < numBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                numBlocks.get(i).entity().teleport(c.clone().add(rotX, base[1], rotZ));
            }

            // Golden sparkles
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 5, 0), 3, 0.5, 255, 215, 0, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NumberOne(plugin); }
    }

    // ================================================================
    // 3. STAR BADGE — 5-pointed star shape in gold/yellow blocks,
    //    rotates and pulses with a bright glow
    // ================================================================
    public static class StarBadge extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public StarBadge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_badge", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5-pointed star: 5 outer points + 5 inner vertices + center
            // Generate star points
            double centerY = 4.0;
            double outerRadius = 2.0;
            double innerRadius = 0.8;

            // Center
            BlockDisplayHandle centerBlock = displayBuilder.spawnBlock(
                    center.clone().add(0, centerY, 0), Material.GOLD_BLOCK);
            centerBlock.scale(0.5f, 0.5f, 0.5f)
                       .glow(255, 255, 220)
                       .interpolation(3, 0);
            starBlocks.add(centerBlock);
            spawnedEntities.add(centerBlock.entity());
            baseOffsets.add(new double[]{0, centerY, 0});

            for (int i = 0; i < 5; i++) {
                // Outer point
                double outerAngle = Math.PI / 2 + (2 * Math.PI * i) / 5;
                double ox = Math.cos(outerAngle) * outerRadius;
                double oy = Math.sin(outerAngle) * outerRadius;
                double[] outerOff = {ox, centerY + oy, 0};
                BlockDisplayHandle outer = displayBuilder.spawnBlock(
                        center.clone().add(outerOff[0], outerOff[1], outerOff[2]), Material.GOLD_BLOCK);
                outer.scale(0.4f, 0.4f, 0.3f)
                     .glow(255, 215, 0)
                     .interpolation(3, 0);
                starBlocks.add(outer);
                spawnedEntities.add(outer.entity());
                baseOffsets.add(outerOff);

                // Arm segment between center and outer point
                double midX = ox * 0.55;
                double midY = oy * 0.55;
                double[] midOff = {midX, centerY + midY, 0};
                BlockDisplayHandle mid = displayBuilder.spawnBlock(
                        center.clone().add(midOff[0], midOff[1], midOff[2]), Material.YELLOW_CONCRETE);
                mid.scale(0.35f, 0.35f, 0.3f)
                   .glow(255, 240, 100)
                   .interpolation(3, 0);
                starBlocks.add(mid);
                spawnedEntities.add(mid.entity());
                baseOffsets.add(midOff);

                // Inner vertex (between outer points)
                double innerAngle = outerAngle + Math.PI / 5;
                double ix = Math.cos(innerAngle) * innerRadius;
                double iy = Math.sin(innerAngle) * innerRadius;
                double[] innerOff = {ix, centerY + iy, 0};
                BlockDisplayHandle inner = displayBuilder.spawnBlock(
                        center.clone().add(innerOff[0], innerOff[1], innerOff[2]), Material.YELLOW_CONCRETE);
                inner.scale(0.3f, 0.3f, 0.3f)
                     .glow(255, 240, 100)
                     .interpolation(3, 0);
                starBlocks.add(inner);
                spawnedEntities.add(inner.entity());
                baseOffsets.add(innerOff);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.05f;
            float pulse = 1.0f + (float) Math.sin(ticksAlive * 0.15) * 0.12f;

            for (int i = 0; i < starBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double bx = base[0] * pulse;
                double by = base[1] + (base[1] - 4.0) * (pulse - 1.0);
                // Rotate around Y
                double rotX = bx * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = bx * Math.sin(angle) + base[2] * Math.cos(angle);
                starBlocks.get(i).entity().teleport(c.clone().add(rotX, by, rotZ));
            }

            // Star sparkles
            if (ticksAlive % 4 == 0) {
                double sparkAngle = Math.random() * Math.PI * 2;
                DisplayBuilder.dustParticles(
                        c.clone().add(Math.cos(sparkAngle) * 1.5, 4, Math.sin(sparkAngle) * 1.5),
                        2, 0.2, 255, 255, 220, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarBadge(plugin); }
    }

    // ================================================================
    // 4. TROPHY CUP — Trophy/goblet shape with handles,
    //    gold with emerald gem accents, slow rotation
    // ================================================================
    public static class TrophyCup extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> trophyBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public TrophyCup(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trophy_cup", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base/pedestal
            double[][] baseOffsetArr = {
                {-0.5, 2, 0}, {0, 2, 0}, {0.5, 2, 0},      // wide base
                {-0.3, 2, -0.3}, {0.3, 2, -0.3},
                {-0.3, 2, 0.3}, {0.3, 2, 0.3},
            };
            for (double[] off : baseOffsetArr) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.4f, 0.2f, 0.4f).glow(255, 215, 0).interpolation(3, 0);
                trophyBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Stem
            double[][] stemOffsets = {{0, 2.3, 0}, {0, 2.7, 0}};
            for (double[] off : stemOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.25f, 0.4f, 0.25f).glow(255, 200, 0).interpolation(3, 0);
                trophyBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Cup bowl (wider, hollow-looking with gold rim)
            double[][] cupOffsets = {
                // Bottom of cup (narrow)
                {-0.3, 3.1, 0}, {0, 3.1, 0}, {0.3, 3.1, 0},
                {0, 3.1, -0.3}, {0, 3.1, 0.3},
                // Mid cup (wider)
                {-0.6, 3.5, 0}, {0.6, 3.5, 0},
                {0, 3.5, -0.6}, {0, 3.5, 0.6},
                {-0.4, 3.5, -0.4}, {0.4, 3.5, -0.4},
                {-0.4, 3.5, 0.4}, {0.4, 3.5, 0.4},
                // Rim (widest)
                {-0.7, 3.9, 0}, {0.7, 3.9, 0},
                {0, 3.9, -0.7}, {0, 3.9, 0.7},
                {-0.5, 3.9, -0.5}, {0.5, 3.9, -0.5},
                {-0.5, 3.9, 0.5}, {0.5, 3.9, 0.5},
            };
            for (double[] off : cupOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.3f, 0.35f, 0.3f).glow(255, 215, 0).interpolation(3, 0);
                trophyBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Handles (left and right)
            double[][] handleOffsets = {
                {-1.0, 3.5, 0}, {-1.1, 3.2, 0}, {-0.9, 2.9, 0}, // left handle
                {1.0, 3.5, 0}, {1.1, 3.2, 0}, {0.9, 2.9, 0},    // right handle
            };
            for (double[] off : handleOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.2f, 0.2f, 0.2f).glow(255, 200, 0).interpolation(3, 0);
                trophyBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Emerald gem accent on front
            BlockDisplayHandle gem = displayBuilder.spawnBlock(
                    center.clone().add(0, 3.5, 0.65), Material.EMERALD_BLOCK);
            gem.scale(0.2f, 0.2f, 0.15f).glow(50, 200, 80).interpolation(3, 0);
            trophyBlocks.add(gem);
            spawnedEntities.add(gem.entity());
            baseOffsets.add(new double[]{0, 3.5, 0.65});

            DisplayBuilder.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.035f;

            for (int i = 0; i < trophyBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                trophyBlocks.get(i).entity().teleport(c.clone().add(rotX, base[1], rotZ));
            }

            // Gold sparkle
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 4.2, 0), 4, 0.5, 255, 215, 0, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TrophyCup(plugin); }
    }

    // ================================================================
    // 5. MEDAL RIBBON — Medal on a ribbon: circular medal with a
    //    V-shaped ribbon above it, gentle swaying
    // ================================================================
    public static class MedalRibbon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> medalBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public MedalRibbon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("medal_ribbon", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ribbon (blue concrete, V-shape from top)
            double[][] ribbonOffsets = {
                {-0.8, 5.5, 0}, {-0.4, 5.0, 0}, {0, 4.6, 0},  // left arm down to center
                {0.4, 5.0, 0}, {0.8, 5.5, 0},                    // right arm up
            };
            for (double[] off : ribbonOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.BLUE_CONCRETE);
                block.scale(0.35f, 0.4f, 0.15f).glow(70, 130, 255).interpolation(3, 0);
                medalBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Medal circle (gold blocks in a ring)
            int medalPoints = 8;
            double medalRadius = 0.6;
            for (int i = 0; i < medalPoints; i++) {
                double angle = (2 * Math.PI * i) / medalPoints;
                double x = Math.cos(angle) * medalRadius;
                double y = Math.sin(angle) * medalRadius;
                double[] off = {x, 3.8 + y, 0};
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.3f, 0.3f, 0.15f).glow(255, 215, 0).interpolation(3, 0);
                medalBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Medal center star (diamond)
            BlockDisplayHandle star = displayBuilder.spawnBlock(
                    center.clone().add(0, 3.8, 0.05), Material.DIAMOND_BLOCK);
            star.scale(0.4f, 0.4f, 0.1f).glow(80, 220, 240).interpolation(3, 0);
            medalBlocks.add(star);
            spawnedEntities.add(star.entity());
            baseOffsets.add(new double[]{0, 3.8, 0.05});

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gentle swaying
            float sway = (float) Math.sin(ticksAlive * 0.06) * 0.15f;
            float angle = ticksAlive * 0.03f;

            for (int i = 0; i < medalBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                medalBlocks.get(i).entity().teleport(c.clone().add(rotX + sway, base[1], rotZ));
            }

            // Glint on medal
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(sway, 3.8, 0), 3, 0.3, 255, 255, 220, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MedalRibbon(plugin); }
    }

    // ================================================================
    // 6. PROGRESS BAR — Horizontal bar that fills from left to right
    //    over the duration, green blocks filling gray frame
    // ================================================================
    public static class ProgressBar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fillBlocks = new ArrayList<>();
        private static final int BAR_LENGTH = 10;

        public ProgressBar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("progress_bar", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Frame: top and bottom borders (gray concrete)
            for (int i = -1; i <= BAR_LENGTH; i++) {
                double x = (i - BAR_LENGTH / 2.0) * 0.6;
                // Top border
                BlockDisplayHandle top = displayBuilder.spawnBlock(
                        center.clone().add(x, 4.0, 0), Material.GRAY_CONCRETE);
                top.scale(0.55f, 0.15f, 0.3f).glow(120, 120, 120).interpolation(2, 0);
                frameBlocks.add(top);
                spawnedEntities.add(top.entity());
                // Bottom border
                BlockDisplayHandle bot = displayBuilder.spawnBlock(
                        center.clone().add(x, 3.3, 0), Material.GRAY_CONCRETE);
                bot.scale(0.55f, 0.15f, 0.3f).glow(120, 120, 120).interpolation(2, 0);
                frameBlocks.add(bot);
                spawnedEntities.add(bot.entity());
            }

            // Left/right end caps
            BlockDisplayHandle leftCap = displayBuilder.spawnBlock(
                    center.clone().add((-BAR_LENGTH / 2.0 - 1) * 0.6, 3.65, 0), Material.GRAY_CONCRETE);
            leftCap.scale(0.15f, 0.6f, 0.3f).glow(120, 120, 120).interpolation(2, 0);
            frameBlocks.add(leftCap);
            spawnedEntities.add(leftCap.entity());

            BlockDisplayHandle rightCap = displayBuilder.spawnBlock(
                    center.clone().add((BAR_LENGTH / 2.0 + 0.5) * 0.6, 3.65, 0), Material.GRAY_CONCRETE);
            rightCap.scale(0.15f, 0.6f, 0.3f).glow(120, 120, 120).interpolation(2, 0);
            frameBlocks.add(rightCap);
            spawnedEntities.add(rightCap.entity());

            // Fill blocks (start invisible/tiny, will animate in)
            for (int i = 0; i < BAR_LENGTH; i++) {
                double x = (i - BAR_LENGTH / 2.0 + 0.5) * 0.6;
                BlockDisplayHandle fill = displayBuilder.spawnBlock(
                        center.clone().add(x, 3.65, 0.01), Material.LIME_CONCRETE);
                fill.scale(0.01f, 0.01f, 0.01f)
                    .glow(50, 200, 80)
                    .interpolation(4, 0);
                fillBlocks.add(fill);
                spawnedEntities.add(fill.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Fill progress based on time
            float progress = Math.min(1.0f, ticksAlive / 100.0f);
            int filledCount = (int) (progress * BAR_LENGTH);

            for (int i = 0; i < fillBlocks.size(); i++) {
                if (i < filledCount) {
                    fillBlocks.get(i).scale(0.5f, 0.5f, 0.25f);
                } else if (i == filledCount) {
                    // Currently filling block (partial)
                    float partial = (progress * BAR_LENGTH) - filledCount;
                    fillBlocks.get(i).scale(0.5f * partial, 0.5f, 0.25f);
                } else {
                    fillBlocks.get(i).scale(0.01f, 0.01f, 0.01f);
                }
            }

            // Pling sound as each block fills
            if (ticksAlive > 0 && ticksAlive % 10 == 0 && filledCount <= BAR_LENGTH) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f,
                        0.8f + (filledCount * 0.12f));
            }

            // Completion celebration
            if (filledCount == BAR_LENGTH && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3.65, 0), 6, 2.0, 50, 200, 80, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ProgressBar(plugin); }
    }

    // ================================================================
    // 7. ASCENDING STEPS — Staircase of blocks going up, each step
    //    lights up sequentially with particles
    // ================================================================
    public static class AscendingSteps extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stepBlocks = new ArrayList<>();
        private static final int STEP_COUNT = 8;

        public AscendingSteps(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ascending_steps", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] stepMats = {
                Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE,
                Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
                Material.GREEN_CONCRETE, Material.CYAN_CONCRETE,
                Material.BLUE_CONCRETE, Material.GOLD_BLOCK,
            };

            for (int i = 0; i < STEP_COUNT; i++) {
                double x = (i - STEP_COUNT / 2.0) * 0.8;
                double y = 1.5 + i * 0.5;
                BlockDisplayHandle step = displayBuilder.spawnBlock(
                        center.clone().add(x, y, 0), stepMats[i]);
                step.scale(0.7f, 0.4f, 0.7f)
                    .glow(120, 120, 120) // Starts dim
                    .interpolation(4, 0);
                stepBlocks.add(step);
                spawnedEntities.add(step.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_HARP, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Light up steps sequentially
            int activeStep = (ticksAlive / 12) % STEP_COUNT;

            for (int i = 0; i < stepBlocks.size(); i++) {
                if (i <= activeStep) {
                    // Lit up: bright glow
                    stepBlocks.get(i).glow(255, 255, 220);
                    float lit = 0.7f + (float) Math.sin(ticksAlive * 0.15) * 0.05f;
                    stepBlocks.get(i).scale(lit, 0.45f, lit);
                } else {
                    // Dim
                    stepBlocks.get(i).glow(120, 120, 120);
                    stepBlocks.get(i).scale(0.7f, 0.4f, 0.7f);
                }
            }

            // Particle on active step
            if (ticksAlive % 4 == 0) {
                double x = (activeStep - STEP_COUNT / 2.0) * 0.8;
                double y = 1.5 + activeStep * 0.5;
                DisplayBuilder.dustParticles(c.clone().add(x, y + 0.5, 0), 4, 0.2, 255, 255, 220, 1.0f);
            }

            // Rising note per step
            if (ticksAlive % 12 == 0) {
                float pitch = 0.6f + (activeStep * 0.2f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, pitch);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AscendingSteps(plugin); }
    }

    // ================================================================
    // 8. CROWN DISPLAY — Royal crown shape in gold/purple blocks,
    //    with gem accents, slowly rotating
    // ================================================================
    public static class CrownDisplay extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crownBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public CrownDisplay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crown_display", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Crown base band (gold ring)
            int bandPoints = 12;
            double bandRadius = 1.0;
            for (int i = 0; i < bandPoints; i++) {
                double angle = (2 * Math.PI * i) / bandPoints;
                double x = Math.cos(angle) * bandRadius;
                double z = Math.sin(angle) * bandRadius;
                double[] off = {x, 3.5, z};
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.35f, 0.35f, 0.35f).glow(255, 215, 0).interpolation(3, 0);
                crownBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Crown points (5 tall spires)
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5;
                double x = Math.cos(angle) * bandRadius;
                double z = Math.sin(angle) * bandRadius;
                // Two blocks per spire
                double[] off1 = {x, 4.0, z};
                double[] off2 = {x, 4.5, z};
                BlockDisplayHandle spire1 = displayBuilder.spawnBlock(
                        center.clone().add(off1[0], off1[1], off1[2]), Material.GOLD_BLOCK);
                spire1.scale(0.25f, 0.35f, 0.25f).glow(255, 215, 0).interpolation(3, 0);
                crownBlocks.add(spire1);
                spawnedEntities.add(spire1.entity());
                baseOffsets.add(off1);

                BlockDisplayHandle spire2 = displayBuilder.spawnBlock(
                        center.clone().add(off2[0], off2[1], off2[2]), Material.GOLD_BLOCK);
                spire2.scale(0.2f, 0.3f, 0.2f).glow(255, 215, 0).interpolation(3, 0);
                crownBlocks.add(spire2);
                spawnedEntities.add(spire2.entity());
                baseOffsets.add(off2);

                // Gem on top of each spire
                double[] gemOff = {x, 4.85, z};
                Material gemMat;
                switch (i % 3) {
                    case 0: gemMat = Material.DIAMOND_BLOCK; break;
                    case 1: gemMat = Material.EMERALD_BLOCK; break;
                    default: gemMat = Material.AMETHYST_BLOCK; break;
                }
                BlockDisplayHandle gem = displayBuilder.spawnBlock(
                        center.clone().add(gemOff[0], gemOff[1], gemOff[2]), gemMat);
                gem.scale(0.18f, 0.18f, 0.18f).glow(180, 80, 255).interpolation(3, 0);
                crownBlocks.add(gem);
                spawnedEntities.add(gem.entity());
                baseOffsets.add(gemOff);
            }

            // Purple velvet interior (top of crown)
            BlockDisplayHandle velvet = displayBuilder.spawnBlock(
                    center.clone().add(0, 4.2, 0), Material.PURPLE_CONCRETE);
            velvet.scale(0.8f, 0.15f, 0.8f).glow(180, 80, 255).interpolation(3, 0);
            crownBlocks.add(velvet);
            spawnedEntities.add(velvet.entity());
            baseOffsets.add(new double[]{0, 4.2, 0});

            DisplayBuilder.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.03f;
            float bob = (float) Math.sin(ticksAlive * 0.08) * 0.15f;

            for (int i = 0; i < crownBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                crownBlocks.get(i).entity().teleport(c.clone().add(rotX, base[1] + bob, rotZ));
            }

            // Royal sparkle
            if (ticksAlive % 6 == 0) {
                double sparkAngle = Math.random() * Math.PI * 2;
                DisplayBuilder.dustParticles(
                        c.clone().add(Math.cos(sparkAngle) * 1.0, 4.8 + bob, Math.sin(sparkAngle) * 1.0),
                        2, 0.15, 255, 215, 0, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownDisplay(plugin); }
    }

    // ================================================================
    // 9. THUMBS UP — Large thumbs-up hand shape in yellow/gold blocks,
    //    bobs with a cheerful animation
    // ================================================================
    public static class ThumbsUp extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> handBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public ThumbsUp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thumbs_up", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Thumb (vertical, gold blocks)
            double[][] thumbOffsets = {
                {0, 5.5, 0}, {0, 5, 0}, {0, 4.5, 0}, {0, 4, 0},
            };
            for (double[] off : thumbOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.45f, 0.45f, 0.4f).glow(255, 215, 0).interpolation(3, 0);
                handBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Fist (horizontal block cluster below thumb)
            double[][] fistOffsets = {
                // Top row of fist
                {-0.5, 3.5, 0}, {0, 3.5, 0}, {0.5, 3.5, 0}, {1.0, 3.5, 0},
                // Middle row
                {-0.5, 3.0, 0}, {0, 3.0, 0}, {0.5, 3.0, 0}, {1.0, 3.0, 0},
                // Bottom row
                {-0.5, 2.5, 0}, {0, 2.5, 0}, {0.5, 2.5, 0}, {1.0, 2.5, 0},
            };
            for (double[] off : fistOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.YELLOW_CONCRETE);
                block.scale(0.4f, 0.4f, 0.4f).glow(255, 240, 100).interpolation(3, 0);
                handBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Cheerful bob
            float bob = (float) Math.sin(ticksAlive * 0.12) * 0.3f;
            // Slight tilt side to side
            float tilt = (float) Math.sin(ticksAlive * 0.08) * 0.08f;

            for (int i = 0; i < handBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double pivotY = 3.0;
                double relY = base[1] - pivotY;
                double relX = base[0];
                double rotX = relX * Math.cos(tilt) - relY * Math.sin(tilt);
                double rotY = relX * Math.sin(tilt) + relY * Math.cos(tilt);
                handBlocks.get(i).entity().teleport(
                        c.clone().add(rotX, rotY + pivotY + bob, base[2]));
            }

            // Cheerful particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 5.8 + bob, 0), 3, 0.3, 255, 255, 100, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThumbsUp(plugin); }
    }

    // ================================================================
    // 10. LEVEL UP RING — Expanding ring of blocks that rises upward,
    //     new rings spawn from bottom, creating upward cascade
    // ================================================================
    public static class LevelUpRing extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> rings = new ArrayList<>();
        private final List<Float> ringYPositions = new ArrayList<>();
        private final List<Integer> ringSpawnTicks = new ArrayList<>();
        private static final int RING_POINTS = 10;
        private static final double RING_RADIUS = 1.5;
        private static final int MAX_RINGS = 5;

        public LevelUpRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("level_up_ring", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn first ring
            spawnRingAt(center, 1.0f);

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        private void spawnRingAt(Location center, float y) {
            List<BlockDisplayHandle> ring = new ArrayList<>();
            Material[] mats = {
                Material.LIME_CONCRETE, Material.YELLOW_CONCRETE,
                Material.GOLD_BLOCK, Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK,
            };
            for (int i = 0; i < RING_POINTS; i++) {
                double angle = (2 * Math.PI * i) / RING_POINTS;
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), mats[i % mats.length]);
                block.scale(0.35f, 0.2f, 0.35f)
                     .glow(50, 200, 80)
                     .interpolation(3, 0);
                ring.add(block);
                spawnedEntities.add(block.entity());
            }
            rings.add(ring);
            ringYPositions.add(y);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spawn new rings periodically
            if (ticksAlive % 20 == 0 && rings.size() < MAX_RINGS) {
                spawnRingAt(c, 1.0f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f,
                        1.0f + rings.size() * 0.15f);
            }

            // Move all rings upward, expanding as they rise
            for (int r = 0; r < rings.size(); r++) {
                float y = ringYPositions.get(r) + 0.08f;
                ringYPositions.set(r, y);
                float expansion = 1.0f + (y - 1.0f) * 0.15f; // expand as rising
                float alpha = Math.max(0.1f, 1.0f - (y - 1.0f) / 8.0f); // fade out

                List<BlockDisplayHandle> ring = rings.get(r);
                float ringRotation = ticksAlive * 0.06f + r * 0.5f;

                for (int i = 0; i < ring.size(); i++) {
                    double angle = (2 * Math.PI * i) / RING_POINTS + ringRotation;
                    double x = Math.cos(angle) * RING_RADIUS * expansion;
                    double z = Math.sin(angle) * RING_RADIUS * expansion;
                    ring.get(i).entity().teleport(c.clone().add(x, y, z));
                    ring.get(i).scale(0.35f * alpha, 0.2f * alpha, 0.35f * alpha);
                }
            }

            // Upward particles
            if (ticksAlive % 3 == 0) {
                double angle = Math.random() * Math.PI * 2;
                DisplayBuilder.dustParticles(
                        c.clone().add(Math.cos(angle) * 1.0, 1.5, Math.sin(angle) * 1.0),
                        3, 0.2, 50, 200, 80, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LevelUpRing(plugin); }
    }
}
