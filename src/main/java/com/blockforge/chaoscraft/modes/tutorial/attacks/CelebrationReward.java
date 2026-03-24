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
 * Tutorial Mode — CELEBRATION & REWARD ATTACKS
 * 10 block display attacks for celebratory/reward moments.
 * Zero damage — purely joyful visual effects for completing tutorial milestones.
 *
 * Color palette:
 * - Firework red: RGB(255, 60, 60)
 * - Celebration gold: RGB(255, 215, 0)
 * - Sparkle white: RGB(255, 255, 240)
 * - Rainbow (cycles)
 * - Treasure amber: RGB(220, 170, 50)
 */
public final class CelebrationReward {

    private CelebrationReward() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FireworkBurst(plugin));
        registry.register(new SparkleRing(plugin));
        registry.register(new ConfettiRain(plugin));
        registry.register(new GoldenSpiral(plugin));
        registry.register(new RainbowArc(plugin));
        registry.register(new TreasureChest(plugin));
        registry.register(new GemShower(plugin));
        registry.register(new VictoryFireworks(plugin));
        registry.register(new RotatingGift(plugin));
        registry.register(new SparkleFountain(plugin));
    }

    // ================================================================
    // 1. FIREWORK BURST — Central block launches upward then explodes
    //    into a ring of colorful blocks expanding outward
    // ================================================================
    public static class FireworkBurst extends BlockDisplayAttack {

        private BlockDisplayHandle rocketBlock;
        private final List<BlockDisplayHandle> burstBlocks = new ArrayList<>();
        private boolean exploded = false;
        private float rocketY = 0;
        private static final int BURST_COUNT = 16;
        private static final float LAUNCH_SPEED = 0.4f;
        private static final float BURST_HEIGHT = 8.0f;

        public FireworkBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("firework_burst", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Rocket
            rocketBlock = displayBuilder.spawnBlock(center.clone().add(0, 1, 0), Material.REDSTONE_BLOCK);
            rocketBlock.scale(0.4f, 0.6f, 0.4f)
                       .glow(255, 60, 60)
                       .interpolation(2, 0);
            spawnedEntities.add(rocketBlock.entity());
            rocketY = 1;

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!exploded) {
                // Launch phase
                rocketY += LAUNCH_SPEED;
                rocketBlock.entity().teleport(c.clone().add(0, rocketY, 0));

                // Rocket trail
                if (ticksAlive % 2 == 0) {
                    w.spawnParticle(Particle.FLAME, c.clone().add(0, rocketY - 0.5, 0),
                            3, 0.05, 0.1, 0.05, 0.01);
                    DisplayBuilder.dustParticles(c.clone().add(0, rocketY - 0.3, 0),
                            2, 0.1, 255, 200, 50, 0.8f);
                }

                // Explode at height
                if (rocketY >= BURST_HEIGHT) {
                    exploded = true;
                    rocketBlock.scale(0.01f, 0.01f, 0.01f); // Hide rocket

                    // Spawn burst blocks in a sphere
                    Material[] burstMats = {
                        Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE,
                        Material.LIME_CONCRETE, Material.CYAN_CONCRETE, Material.BLUE_CONCRETE,
                        Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE,
                        Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
                        Material.RED_CONCRETE, Material.YELLOW_CONCRETE,
                        Material.LIME_CONCRETE, Material.CYAN_CONCRETE,
                        Material.PURPLE_CONCRETE, Material.GOLD_BLOCK,
                    };

                    Location burstCenter = c.clone().add(0, BURST_HEIGHT, 0);
                    for (int i = 0; i < BURST_COUNT; i++) {
                        BlockDisplayHandle burst = displayBuilder.spawnBlock(burstCenter, burstMats[i]);
                        burst.scale(0.35f, 0.35f, 0.35f)
                             .glow(255, 255, 240)
                             .interpolation(3, 0);
                        burstBlocks.add(burst);
                        spawnedEntities.add(burst.entity());
                    }

                    DisplayBuilder.playSound(burstCenter, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1.0f);
                    DisplayBuilder.playSound(burstCenter, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.2f);
                }
            } else {
                // Burst expansion phase
                int burstAge = ticksAlive - (int) (BURST_HEIGHT / LAUNCH_SPEED);
                float expandRadius = burstAge * 0.15f;
                float fadeScale = Math.max(0.05f, 0.35f - burstAge * 0.005f);

                Location burstCenter = c.clone().add(0, BURST_HEIGHT, 0);
                for (int i = 0; i < burstBlocks.size(); i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / BURST_COUNT);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    double x = Math.sin(phi) * Math.cos(theta) * expandRadius;
                    double y = Math.cos(phi) * expandRadius;
                    double z = Math.sin(phi) * Math.sin(theta) * expandRadius;

                    burstBlocks.get(i).entity().teleport(
                            burstCenter.clone().add(x, y - burstAge * 0.03, z));
                    burstBlocks.get(i).scale(fadeScale, fadeScale, fadeScale);
                }

                // Twinkle particles
                if (burstAge % 3 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    DisplayBuilder.dustParticles(
                            burstCenter.clone().add(
                                    Math.cos(angle) * expandRadius * 0.7,
                                    Math.random() * expandRadius - expandRadius / 2,
                                    Math.sin(angle) * expandRadius * 0.7),
                            2, 0.1, 255, 255, 240, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FireworkBurst(plugin); }
    }

    // ================================================================
    // 2. SPARKLE RING — Horizontal ring of blocks that expand outward
    //    while spinning, leaving sparkle trails
    // ================================================================
    public static class SparkleRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private static final int RING_COUNT = 12;
        private float currentRadius = 0.5f;

        public SparkleRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sparkle_ring", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {
                Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                Material.AMETHYST_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK,
                Material.EMERALD_BLOCK, Material.AMETHYST_BLOCK, Material.GOLD_BLOCK,
                Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.AMETHYST_BLOCK,
            };

            for (int i = 0; i < RING_COUNT; i++) {
                double angle = (2 * Math.PI * i) / RING_COUNT;
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, 2.5, z), mats[i]);
                block.scale(0.35f, 0.35f, 0.35f)
                     .glow(255, 255, 240)
                     .interpolation(3, 0);
                ringBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Expand then contract
            float targetRadius = 0.5f + (float) Math.sin(ticksAlive * 0.06) * 2.0f + 2.0f;
            currentRadius += (targetRadius - currentRadius) * 0.1f;

            float spin = ticksAlive * 0.08f;
            float wave = (float) Math.sin(ticksAlive * 0.15) * 0.5f;

            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / RING_COUNT + spin;
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                double y = 2.5 + Math.sin(angle * 2 + ticksAlive * 0.1) * 0.3;
                ringBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            // Sparkle trails
            if (ticksAlive % 2 == 0) {
                double trailAngle = spin;
                Location trail = c.clone().add(
                        Math.cos(trailAngle) * currentRadius, 2.5,
                        Math.sin(trailAngle) * currentRadius);
                DisplayBuilder.dustParticles(trail, 3, 0.15, 255, 255, 240, 0.8f);
            }

            // Chime
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.5f + (float) Math.random() * 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SparkleRing(plugin); }
    }

    // ================================================================
    // 3. CONFETTI RAIN — Colorful small blocks falling from above
    //    in random positions, fluttering side to side
    // ================================================================
    public static class ConfettiRain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> confetti = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Float> flutter = new ArrayList<>();
        private static final int CONFETTI_COUNT = 25;
        private static final float START_Y = 10.0f;

        public ConfettiRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("confetti_rain", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] confettiMats = {
                Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE,
                Material.LIME_CONCRETE, Material.CYAN_CONCRETE, Material.BLUE_CONCRETE,
                Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE, Material.PINK_CONCRETE,
                Material.WHITE_CONCRETE, Material.GOLD_BLOCK,
            };

            for (int i = 0; i < CONFETTI_COUNT; i++) {
                double ox = (Math.random() - 0.5) * 8;
                double oz = (Math.random() - 0.5) * 8;
                float startY = START_Y + (float) (Math.random() * 5);
                Location loc = center.clone().add(ox, startY, oz);

                BlockDisplayHandle piece = displayBuilder.spawnBlock(
                        loc, confettiMats[i % confettiMats.length]);
                piece.scale(0.2f, 0.05f, 0.2f)
                     .glow(255, 255, 240)
                     .interpolation(2, 0);
                confetti.add(piece);
                spawnedEntities.add(piece.entity());
                yPositions.add(startY);
                fallSpeeds.add(0.05f + (float) (Math.random() * 0.08));
                xOffsets.add(ox);
                zOffsets.add(oz);
                flutter.add((float) (Math.random() * Math.PI * 2));
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < confetti.size(); i++) {
                float y = yPositions.get(i) - fallSpeeds.get(i);
                yPositions.set(i, y);

                // Flutter side to side
                float flutterVal = flutter.get(i);
                double sway = Math.sin(ticksAlive * 0.1 + flutterVal) * 0.5;
                double ox = xOffsets.get(i) + sway;

                confetti.get(i).entity().teleport(c.clone().add(ox, y, zOffsets.get(i)));

                // Rotate the confetti piece for tumbling
                float tumble = ticksAlive * 0.15f + flutterVal;
                confetti.get(i).rotate(tumble, 1, 0, 0);

                // Respawn at top if fallen too low
                if (y < 0) {
                    yPositions.set(i, START_Y + (float) (Math.random() * 3));
                    xOffsets.set(i, (Math.random() - 0.5) * 8);
                    zOffsets.set(i, (Math.random() - 0.5) * 8);
                }
            }

            // Ambient celebration sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConfettiRain(plugin); }
    }

    // ================================================================
    // 4. GOLDEN SPIRAL — Fibonacci/golden spiral pattern ascending,
    //    blocks placed along a spiral curve rising upward
    // ================================================================
    public static class GoldenSpiral extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiralBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();
        private static final int SPIRAL_POINTS = 20;

        public GoldenSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("golden_spiral", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SPIRAL_POINTS; i++) {
                double t = i * 0.5;
                double radius = 0.3 + t * 0.12;
                double angle = t * 1.2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = 1.5 + i * 0.35;
                double[] off = {x, y, z};

                Material mat = (i % 3 == 0) ? Material.GOLD_BLOCK :
                               (i % 3 == 1) ? Material.YELLOW_CONCRETE : Material.ORANGE_CONCRETE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), mat);
                float size = 0.3f + (i * 0.01f);
                block.scale(size, size, size)
                     .glow(255, 215, 0)
                     .interpolation(3, 0);
                spiralBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate the entire spiral around Y axis
            float rotation = ticksAlive * 0.04f;

            for (int i = 0; i < spiralBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(rotation) - base[2] * Math.sin(rotation);
                double rotZ = base[0] * Math.sin(rotation) + base[2] * Math.cos(rotation);
                // Gentle vertical wave
                double yWave = Math.sin(ticksAlive * 0.1 + i * 0.3) * 0.15;
                spiralBlocks.get(i).entity().teleport(
                        c.clone().add(rotX, base[1] + yWave, rotZ));
            }

            // Golden dust trailing from top
            if (ticksAlive % 4 == 0 && !baseOffsets.isEmpty()) {
                double[] topOff = baseOffsets.get(baseOffsets.size() - 1);
                double topRotX = topOff[0] * Math.cos(rotation) - topOff[2] * Math.sin(rotation);
                double topRotZ = topOff[0] * Math.sin(rotation) + topOff[2] * Math.cos(rotation);
                DisplayBuilder.dustParticles(
                        c.clone().add(topRotX, topOff[1], topRotZ),
                        3, 0.2, 255, 215, 0, 1.0f);
            }

            // Musical chime ascending
            if (ticksAlive % 15 == 0) {
                float pitch = 0.8f + (ticksAlive % 60) / 60.0f;
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, pitch);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldenSpiral(plugin); }
    }

    // ================================================================
    // 5. RAINBOW ARC — Semicircular arc of blocks in rainbow colors,
    //    slowly rotates and shimmers
    // ================================================================
    public static class RainbowArc extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arcBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public RainbowArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rainbow_arc", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 7 rainbow bands, each a semicircle arc
            Material[] rainbowMats = {
                Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE,
                Material.LIME_CONCRETE, Material.CYAN_CONCRETE, Material.BLUE_CONCRETE,
                Material.PURPLE_CONCRETE,
            };

            for (int band = 0; band < 7; band++) {
                double radius = 3.0 + band * 0.45;
                int segments = 10;
                for (int i = 0; i < segments; i++) {
                    double angle = Math.PI * i / (segments - 1); // 0 to PI (semicircle)
                    double x = Math.cos(angle) * radius;
                    double y = Math.sin(angle) * radius;
                    double[] off = {x, 1.0 + y, 0};

                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(off[0], off[1], off[2]), rainbowMats[band]);
                    block.scale(0.35f, 0.35f, 0.35f)
                         .glow(255, 255, 240)
                         .interpolation(3, 0);
                    arcBlocks.add(block);
                    spawnedEntities.add(block.entity());
                    baseOffsets.add(off);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.025f;
            float shimmer = (float) Math.sin(ticksAlive * 0.15) * 0.05f;

            for (int i = 0; i < arcBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                float sizeShimmer = 0.35f + shimmer * ((i % 3 == 0) ? 1 : 0);
                arcBlocks.get(i).entity().teleport(c.clone().add(rotX, base[1], rotZ));
                arcBlocks.get(i).scale(sizeShimmer, sizeShimmer, sizeShimmer);
            }

            // Rainbow sparkle
            if (ticksAlive % 4 == 0) {
                double sparkAngle = Math.random() * Math.PI;
                double sparkRadius = 3.0 + Math.random() * 3.0;
                DisplayBuilder.dustParticles(
                        c.clone().add(Math.cos(sparkAngle) * sparkRadius,
                                1.0 + Math.sin(sparkAngle) * sparkRadius, 0),
                        2, 0.2, 255, 255, 240, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RainbowArc(plugin); }
    }

    // ================================================================
    // 6. TREASURE CHEST — Chest shape with lid that opens/closes,
    //    gold blocks spill out when open
    // ================================================================
    public static class TreasureChest extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chestBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> lidBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> treasureBlocks = new ArrayList<>();
        private boolean isOpen = false;

        public TreasureChest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("treasure_chest", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Chest body (dark oak, 3x2x2)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 0; z++) {
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle block = displayBuilder.spawnBlock(
                                center.clone().add(x * 0.6, 1.5 + y * 0.5, z * 0.6),
                                Material.DARK_OAK_PLANKS);
                        block.scale(0.55f, 0.45f, 0.55f)
                             .glow(100, 65, 30)
                             .interpolation(3, 0);
                        chestBlocks.add(block);
                        spawnedEntities.add(block.entity());
                    }
                }
            }

            // Lid (top row, will animate)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 0; z++) {
                    BlockDisplayHandle lid = displayBuilder.spawnBlock(
                            center.clone().add(x * 0.6, 2.5, z * 0.6), Material.OAK_PLANKS);
                    lid.scale(0.55f, 0.3f, 0.55f)
                       .glow(160, 110, 50)
                       .interpolation(4, 0);
                    lidBlocks.add(lid);
                    spawnedEntities.add(lid.entity());
                }
            }

            // Lock (gold, front center)
            BlockDisplayHandle lock = displayBuilder.spawnBlock(
                    center.clone().add(0, 2.2, 0.35), Material.GOLD_BLOCK);
            lock.scale(0.2f, 0.2f, 0.1f)
                .glow(255, 215, 0)
                .interpolation(3, 0);
            chestBlocks.add(lock);
            spawnedEntities.add(lock.entity());

            // Treasure blocks (hidden inside, will pop out)
            Material[] treasureMats = {
                Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                Material.GOLD_BLOCK, Material.AMETHYST_BLOCK, Material.GOLD_BLOCK,
                Material.DIAMOND_BLOCK, Material.LAPIS_BLOCK,
            };
            for (int i = 0; i < treasureMats.length; i++) {
                BlockDisplayHandle treasure = displayBuilder.spawnBlock(
                        center.clone().add(0, 2, 0), treasureMats[i]);
                treasure.scale(0.01f, 0.01f, 0.01f) // Hidden initially
                        .glow(255, 215, 0)
                        .interpolation(4, 0);
                treasureBlocks.add(treasure);
                spawnedEntities.add(treasure.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHEST_LOCKED, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Cycle open/close every 40 ticks
            boolean shouldBeOpen = (ticksAlive / 40) % 2 == 1;

            if (shouldBeOpen && !isOpen) {
                isOpen = true;
                DisplayBuilder.playSound(c, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
            } else if (!shouldBeOpen && isOpen) {
                isOpen = false;
                DisplayBuilder.playSound(c, Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.0f);
            }

            // Animate lid
            float lidAngle = isOpen ? -0.8f : 0;
            for (int i = 0; i < lidBlocks.size(); i++) {
                int x = (i / 2) - 1;
                int z = (i % 2) - 1;
                double lidY = isOpen ? 2.8 : 2.5;
                double lidZ = isOpen ? z * 0.6 - 0.3 : z * 0.6;
                lidBlocks.get(i).entity().teleport(c.clone().add(x * 0.6, lidY, lidZ));
                lidBlocks.get(i).rotate(lidAngle, 1, 0, 0);
            }

            // Treasure pops out when open
            if (isOpen) {
                int openAge = ticksAlive % 40;
                for (int i = 0; i < treasureBlocks.size(); i++) {
                    if (openAge > i * 3) {
                        double angle = (2 * Math.PI * i) / treasureBlocks.size();
                        float popHeight = Math.min(openAge * 0.05f, 1.5f);
                        double tx = Math.cos(angle) * (0.3 + popHeight * 0.3);
                        double tz = Math.sin(angle) * (0.3 + popHeight * 0.3);
                        treasureBlocks.get(i).entity().teleport(
                                c.clone().add(tx, 2.5 + popHeight, tz));
                        treasureBlocks.get(i).scale(0.25f, 0.25f, 0.25f);
                        treasureBlocks.get(i).rotate(ticksAlive * 0.1f + i, 0, 1, 0);
                    }
                }

                // Sparkle from chest
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 4, 0.5, 255, 215, 0, 1.0f);
                }
            } else {
                // Hide treasure when closed
                for (BlockDisplayHandle treasure : treasureBlocks) {
                    treasure.scale(0.01f, 0.01f, 0.01f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TreasureChest(plugin); }
    }

    // ================================================================
    // 7. GEM SHOWER — Various gem-colored blocks rain gently from
    //    above, spinning and twinkling as they descend
    // ================================================================
    public static class GemShower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> gems = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Float> spinSpeeds = new ArrayList<>();
        private static final int GEM_COUNT = 18;

        public GemShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gem_shower", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] gemMats = {
                Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.AMETHYST_BLOCK,
                Material.LAPIS_BLOCK, Material.GOLD_BLOCK, Material.REDSTONE_BLOCK,
            };

            for (int i = 0; i < GEM_COUNT; i++) {
                double ox = (Math.random() - 0.5) * 6;
                double oz = (Math.random() - 0.5) * 6;
                float startY = 8.0f + (float) (Math.random() * 6);

                BlockDisplayHandle gem = displayBuilder.spawnBlock(
                        center.clone().add(ox, startY, oz), gemMats[i % gemMats.length]);
                gem.scale(0.3f, 0.3f, 0.3f)
                   .glow(255, 255, 240)
                   .interpolation(2, 0);
                gems.add(gem);
                spawnedEntities.add(gem.entity());
                yPositions.add(startY);
                fallSpeeds.add(0.04f + (float) (Math.random() * 0.06));
                xOffsets.add(ox);
                zOffsets.add(oz);
                spinSpeeds.add(0.05f + (float) (Math.random() * 0.15));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < gems.size(); i++) {
                float y = yPositions.get(i) - fallSpeeds.get(i);
                yPositions.set(i, y);

                gems.get(i).entity().teleport(c.clone().add(xOffsets.get(i), y, zOffsets.get(i)));
                gems.get(i).rotate(ticksAlive * spinSpeeds.get(i), 0, 1, 0);

                // Respawn at top when fallen
                if (y < 0.5f) {
                    yPositions.set(i, 10.0f + (float) (Math.random() * 4));
                    xOffsets.set(i, (Math.random() - 0.5) * 6);
                    zOffsets.set(i, (Math.random() - 0.5) * 6);
                }
            }

            // Twinkle particles
            if (ticksAlive % 5 == 0) {
                int randomGem = (int) (Math.random() * gems.size());
                if (randomGem < gems.size()) {
                    Location gemLoc = gems.get(randomGem).entity().getLocation();
                    DisplayBuilder.dustParticles(gemLoc, 2, 0.1, 255, 255, 240, 0.6f);
                }
            }

            // Crystal chime
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.0f + (float) Math.random() * 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GemShower(plugin); }
    }

    // ================================================================
    // 8. VICTORY FIREWORKS — Multiple firework bursts at different
    //    positions and times, grand finale celebration
    // ================================================================
    public static class VictoryFireworks extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> bursts = new ArrayList<>();
        private final List<Location> burstCenters = new ArrayList<>();
        private final List<Integer> burstTicks = new ArrayList<>();
        private final List<Float> burstRadii = new ArrayList<>();
        private static final int BURST_TOTAL = 5;
        private static final int BURST_POINTS = 12;

        public VictoryFireworks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("victory_fireworks", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Spawn new bursts at intervals
            if (ticksAlive % 20 == 5 && bursts.size() < BURST_TOTAL) {
                double bx = (Math.random() - 0.5) * 8;
                double by = 6 + Math.random() * 5;
                double bz = (Math.random() - 0.5) * 8;
                Location burstCenter = c.clone().add(bx, by, bz);

                Material[] burstMats = {
                    Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE,
                    Material.LIME_CONCRETE, Material.CYAN_CONCRETE, Material.BLUE_CONCRETE,
                    Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE,
                    Material.WHITE_CONCRETE, Material.GOLD_BLOCK,
                    Material.PINK_CONCRETE, Material.LIGHT_BLUE_CONCRETE,
                };

                List<BlockDisplayHandle> burst = new ArrayList<>();
                for (int i = 0; i < BURST_POINTS; i++) {
                    BlockDisplayHandle block = displayBuilder.spawnBlock(burstCenter, burstMats[i]);
                    block.scale(0.3f, 0.3f, 0.3f)
                         .glow(255, 255, 240)
                         .interpolation(3, 0);
                    burst.add(block);
                    spawnedEntities.add(block.entity());
                }

                bursts.add(burst);
                burstCenters.add(burstCenter);
                burstTicks.add(ticksAlive);
                burstRadii.add(0.0f);

                DisplayBuilder.playSound(burstCenter, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.8f + (float) Math.random() * 0.4f);
                DisplayBuilder.playSound(burstCenter, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.0f);
            }

            // Animate all active bursts
            for (int b = 0; b < bursts.size(); b++) {
                int age = ticksAlive - burstTicks.get(b);
                float radius = age * 0.12f;
                burstRadii.set(b, radius);
                float fade = Math.max(0.05f, 0.3f - age * 0.004f);

                List<BlockDisplayHandle> burst = bursts.get(b);
                Location bc = burstCenters.get(b);

                for (int i = 0; i < burst.size(); i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / BURST_POINTS);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    double x = Math.sin(phi) * Math.cos(theta) * radius;
                    double y = Math.cos(phi) * radius - age * 0.02;
                    double z = Math.sin(phi) * Math.sin(theta) * radius;

                    burst.get(i).entity().teleport(bc.clone().add(x, y, z));
                    burst.get(i).scale(fade, fade, fade);
                }

                // Twinkle particles
                if (age % 4 == 0 && age < 40) {
                    double sparkAngle = Math.random() * Math.PI * 2;
                    DisplayBuilder.dustParticles(
                            bc.clone().add(Math.cos(sparkAngle) * radius * 0.5,
                                    Math.random() * radius - radius / 2,
                                    Math.sin(sparkAngle) * radius * 0.5),
                            2, 0.1, 255, 255, 240, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VictoryFireworks(plugin); }
    }

    // ================================================================
    // 9. ROTATING GIFT — Gift box shape with a bow on top,
    //    rotates to show all sides, gentle bob
    // ================================================================
    public static class RotatingGift extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> giftBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public RotatingGift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rotating_gift", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Box body (red concrete, 3x3x3 shell)
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        // Only shell (skip interior)
                        if (Math.abs(x) < 1 && y > 0 && y < 2 && Math.abs(z) < 1) continue;
                        double[] off = {x * 0.55, 2 + y * 0.55, z * 0.55};
                        BlockDisplayHandle block = displayBuilder.spawnBlock(
                                center.clone().add(off[0], off[1], off[2]), Material.RED_CONCRETE);
                        block.scale(0.5f, 0.5f, 0.5f)
                             .glow(255, 60, 60)
                             .interpolation(3, 0);
                        giftBlocks.add(block);
                        spawnedEntities.add(block.entity());
                        baseOffsets.add(off);
                    }
                }
            }

            // Ribbon cross (gold blocks on top and sides)
            // Horizontal ribbon across top
            double[][] ribbonOffsets = {
                {-1 * 0.55, 2 + 2 * 0.55 + 0.05, 0},
                {0, 2 + 2 * 0.55 + 0.05, 0},
                {1 * 0.55, 2 + 2 * 0.55 + 0.05, 0},
                {0, 2 + 2 * 0.55 + 0.05, -1 * 0.55},
                {0, 2 + 2 * 0.55 + 0.05, 1 * 0.55},
            };
            for (double[] off : ribbonOffsets) {
                BlockDisplayHandle ribbon = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                ribbon.scale(0.5f, 0.12f, 0.18f)
                      .glow(255, 215, 0)
                      .interpolation(3, 0);
                giftBlocks.add(ribbon);
                spawnedEntities.add(ribbon.entity());
                baseOffsets.add(off);
            }

            // Bow on top (2 gold blocks angled)
            double bowY = 2 + 2 * 0.55 + 0.25;
            double[][] bowOffsets = {
                {-0.25, bowY, 0}, {0.25, bowY, 0},
                {0, bowY + 0.15, 0},
            };
            for (double[] off : bowOffsets) {
                BlockDisplayHandle bow = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                bow.scale(0.25f, 0.2f, 0.2f)
                   .glow(255, 215, 0)
                   .interpolation(3, 0);
                giftBlocks.add(bow);
                spawnedEntities.add(bow.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.04f;
            float bob = (float) Math.sin(ticksAlive * 0.1) * 0.2f;

            for (int i = 0; i < giftBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                giftBlocks.get(i).entity().teleport(c.clone().add(rotX, base[1] + bob, rotZ));
            }

            // Gift sparkle
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 4 + bob, 0), 3, 0.4, 255, 215, 0, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RotatingGift(plugin); }
    }

    // ================================================================
    // 10. SPARKLE FOUNTAIN — Central column shoots blocks upward
    //     that arc outward and fall like a fountain, continuous flow
    // ================================================================
    public static class SparkleFountain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> particles = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> yVelocities = new ArrayList<>();
        private final List<Double> xVelocities = new ArrayList<>();
        private final List<Double> zVelocities = new ArrayList<>();
        private final List<Integer> spawnTicks = new ArrayList<>();
        private static final int MAX_PARTICLES = 30;
        private int nextSpawnTick = 0;

        public SparkleFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sparkle_fountain", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base pedestal
            List<BlockDisplayHandle> pedestal = displayBuilder.spawnRing(
                    center.clone().add(0, 1, 0), Material.PRISMARINE, 1.0, 8);
            for (BlockDisplayHandle block : pedestal) {
                block.scale(0.4f, 0.3f, 0.4f).glow(80, 200, 200).interpolation(2, 0);
                spawnedEntities.add(block.entity());
            }

            // Center spout
            BlockDisplayHandle spout = displayBuilder.spawnBlock(
                    center.clone().add(0, 1.3, 0), Material.SEA_LANTERN);
            spout.scale(0.4f, 0.5f, 0.4f).glow(100, 220, 240).interpolation(2, 0);
            spawnedEntities.add(spout.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spawn new fountain particles
            if (ticksAlive >= nextSpawnTick && particles.size() < MAX_PARTICLES) {
                nextSpawnTick = ticksAlive + 2; // Every 2 ticks

                Material[] fountainMats = {
                    Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                    Material.AMETHYST_BLOCK, Material.PRISMARINE,
                };

                BlockDisplayHandle particle = displayBuilder.spawnBlock(
                        c.clone().add(0, 1.8, 0),
                        fountainMats[particles.size() % fountainMats.length]);
                particle.scale(0.2f, 0.2f, 0.2f)
                        .glow(255, 255, 240)
                        .interpolation(2, 0);
                particles.add(particle);
                spawnedEntities.add(particle.entity());
                yPositions.add(1.8f);
                yVelocities.add(0.3f + (float) (Math.random() * 0.15));
                double angle = Math.random() * Math.PI * 2;
                double speed = 0.03 + Math.random() * 0.04;
                xVelocities.add(Math.cos(angle) * speed);
                zVelocities.add(Math.sin(angle) * speed);
                spawnTicks.add(ticksAlive);
            }

            // Update fountain particles
            for (int i = particles.size() - 1; i >= 0; i--) {
                int age = ticksAlive - spawnTicks.get(i);
                float vy = yVelocities.get(i) - (age * 0.015f); // gravity
                float y = yPositions.get(i) + vy * 0.5f;
                yPositions.set(i, y);

                double xOff = xVelocities.get(i) * age;
                double zOff = zVelocities.get(i) * age;

                particles.get(i).entity().teleport(c.clone().add(xOff, y, zOff));
                particles.get(i).rotate(age * 0.1f, 0, 1, 0);

                // Fade out and remove when below ground
                float fade = Math.max(0.05f, 0.2f - age * 0.003f);
                particles.get(i).scale(fade, fade, fade);

                if (y < 0.5f) {
                    particles.get(i).scale(0.01f, 0.01f, 0.01f);
                    // Reset particle to top
                    yPositions.set(i, 1.8f);
                    yVelocities.set(i, 0.3f + (float) (Math.random() * 0.15));
                    double newAngle = Math.random() * Math.PI * 2;
                    double newSpeed = 0.03 + Math.random() * 0.04;
                    xVelocities.set(i, Math.cos(newAngle) * newSpeed);
                    zVelocities.set(i, Math.sin(newAngle) * newSpeed);
                    spawnTicks.set(i, ticksAlive);
                }
            }

            // Sparkle at the spout
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 3, 0.15, 255, 255, 240, 0.8f);
            }

            // Water ambient sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SparkleFountain(plugin); }
    }
}
