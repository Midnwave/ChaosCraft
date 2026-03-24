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
 * Tutorial Mode — HAZARD & CHALLENGE ATTACKS
 * 13 block display attacks with mild damage to teach hazard avoidance.
 * These are the first "real" threats players encounter in the tutorial,
 * designed to teach dodging, awareness, and positioning.
 *
 * Color palette:
 * - Warning orange: RGB(255, 150, 30)
 * - Danger red: RGB(220, 50, 50)
 * - Stone gray: RGB(140, 140, 140)
 * - Lava glow: RGB(255, 100, 20)
 * - Ice blue: RGB(150, 210, 255)
 * - Nature green: RGB(60, 140, 40)
 */
public final class HazardChallenge {

    private HazardChallenge() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TumblingCobblestone(plugin));
        registry.register(new FallingLogTrap(plugin));
        registry.register(new SpinningBladeSlow(plugin));
        registry.register(new GravelAvalanche(plugin));
        registry.register(new LavaPuddleWarning(plugin));
        registry.register(new SwingingPendulum(plugin));
        registry.register(new CreepingVines(plugin));
        registry.register(new BouncingBoulder(plugin));
        registry.register(new WindGust(plugin));
        registry.register(new ThornBush(plugin));
        registry.register(new IceSpike(plugin));
        registry.register(new SandSinkhole(plugin));
        registry.register(new FireRingHop(plugin));
    }

    // ================================================================
    // 1. TUMBLING COBBLESTONE — 8 cobblestone blocks tumble down a
    //    slope toward the player, bouncing and rolling
    // ================================================================
    public static class TumblingCobblestone extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stones = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> zPositions = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Float> rollAngles = new ArrayList<>();
        private static final int STONE_COUNT = 8;

        public TumblingCobblestone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tumbling_cobblestone", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < STONE_COUNT; i++) {
                float startZ = -6.0f - (float) (Math.random() * 3);
                float startY = 4.0f + (float) (Math.random() * 3);
                double xOff = (Math.random() - 0.5) * 4;

                Location loc = center.clone().add(xOff, startY, startZ);
                Material mat = (i % 3 == 0) ? Material.MOSSY_COBBLESTONE : Material.COBBLESTONE;
                BlockDisplayHandle stone = displayBuilder.spawnBlock(loc, mat);
                stone.scale(0.6f + (float)(Math.random() * 0.4), 0.6f + (float)(Math.random() * 0.4), 0.6f + (float)(Math.random() * 0.4))
                     .glow(140, 140, 140)
                     .interpolation(2, 0);
                stones.add(stone);
                spawnedEntities.add(stone.entity());
                yPositions.add(startY);
                zPositions.add(startZ);
                fallSpeeds.add(0.08f + (float)(Math.random() * 0.06));
                rollAngles.add(0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < stones.size(); i++) {
                // Roll forward (increasing z) and tumble downhill
                float z = zPositions.get(i) + 0.12f;
                zPositions.set(i, z);

                // Bounce on y axis
                float gravity = fallSpeeds.get(i);
                float y = yPositions.get(i) - gravity;
                if (y < 0.3f) {
                    y = 0.3f;
                    fallSpeeds.set(i, -gravity * 0.5f); // Bounce
                    // Impact sound
                    if (gravity > 0.05f) {
                        DisplayBuilder.playSound(
                                c.clone().add(0, y, z), Sound.BLOCK_STONE_FALL, 0.5f, 0.8f);
                    }
                } else {
                    fallSpeeds.set(i, gravity + 0.008f); // gravity acceleration
                }
                yPositions.set(i, y);

                double xOff = (i - STONE_COUNT / 2.0) * 0.6;
                stones.get(i).entity().teleport(c.clone().add(xOff, y, z));

                // Roll rotation
                float roll = rollAngles.get(i) + 0.15f;
                rollAngles.set(i, roll);
                stones.get(i).rotate(roll, 1, 0, 0);
            }

            // Dust particles
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < stones.size(); i += 3) {
                    if (yPositions.get(i) < 0.8f) {
                        double xOff = (i - STONE_COUNT / 2.0) * 0.6;
                        c.getWorld().spawnParticle(Particle.BLOCK,
                                c.clone().add(xOff, 0.3, zPositions.get(i)),
                                4, 0.2, 0.1, 0.2, 0.05, Material.COBBLESTONE.createBlockData());
                    }
                }
            }

            // Rumble sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TumblingCobblestone(plugin); }
    }

    // ================================================================
    // 2. FALLING LOG TRAP — 3 large oak log blocks fall from above
    //    in sequence with warning particles before each drop
    // ================================================================
    public static class FallingLogTrap extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> logs = new ArrayList<>();
        private final List<Float> logYPositions = new ArrayList<>();
        private final List<Boolean> logFalling = new ArrayList<>();
        private final List<Boolean> logLanded = new ArrayList<>();
        private static final int LOG_COUNT = 3;
        private static final float DROP_HEIGHT = 12.0f;

        public FallingLogTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_log_trap", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double[] xPositions = {-2.0, 0, 2.0};
            for (int i = 0; i < LOG_COUNT; i++) {
                Location loc = center.clone().add(xPositions[i], DROP_HEIGHT, 0);
                BlockDisplayHandle log = displayBuilder.spawnBlock(loc, Material.OAK_LOG);
                log.scale(1.0f, 2.5f, 1.0f)
                   .glow(160, 110, 50)
                   .interpolation(2, 0);
                logs.add(log);
                spawnedEntities.add(log.entity());
                logYPositions.add(DROP_HEIGHT);
                logFalling.add(false);
                logLanded.add(false);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WOOD_BREAK, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            double[] xPositions = {-2.0, 0, 2.0};

            for (int i = 0; i < LOG_COUNT; i++) {
                int triggerTick = i * 30; // staggered drops

                // Warning phase
                if (ticksAlive >= triggerTick - 20 && ticksAlive < triggerTick && !logFalling.get(i)) {
                    // Warning particles on ground
                    Location groundWarning = c.clone().add(xPositions[i], 0.5, 0);
                    if (ticksAlive % 4 == 0) {
                        DisplayBuilder.dustParticles(groundWarning, 6, 0.8, 255, 150, 30, 1.2f);
                        DisplayBuilder.playSound(groundWarning, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
                    }
                }

                // Start falling
                if (ticksAlive >= triggerTick && !logFalling.get(i)) {
                    logFalling.set(i, true);
                    DisplayBuilder.playSound(c.clone().add(xPositions[i], DROP_HEIGHT, 0),
                            Sound.BLOCK_WOOD_BREAK, 0.8f, 0.6f);
                }

                // Fall
                if (logFalling.get(i) && !logLanded.get(i)) {
                    float y = logYPositions.get(i) - 0.5f; // fast fall
                    logYPositions.set(i, y);
                    logs.get(i).entity().teleport(c.clone().add(xPositions[i], y, 0));

                    // Whoosh particles
                    if (ticksAlive % 2 == 0) {
                        DisplayBuilder.dustParticles(
                                c.clone().add(xPositions[i], y + 1, 0),
                                3, 0.3, 160, 110, 50, 0.8f);
                    }

                    // Impact
                    if (y <= 0.5f) {
                        logYPositions.set(i, 0.5f);
                        logLanded.set(i, true);

                        Location impactLoc = c.clone().add(xPositions[i], 0, 0);
                        w.spawnParticle(Particle.BLOCK, impactLoc, 20, 1.0, 0.3, 1.0, 0.1,
                                Material.OAK_LOG.createBlockData());
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8f, 0.5f);
                        DisplayBuilder.particleRing(impactLoc, 2.0, Particle.DUST, 16,
                                new Particle.DustOptions(Color.fromRGB(160, 110, 50), 1.2f));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FallingLogTrap(plugin); }
    }

    // ================================================================
    // 3. SPINNING BLADE SLOW — A horizontal ring of iron blocks
    //    spinning slowly at waist height, easy to jump over
    // ================================================================
    public static class SpinningBladeSlow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private static final int BLADE_COUNT = 8;
        private static final double BLADE_RADIUS = 3.0;

        public SpinningBladeSlow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spinning_blade_slow", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Two crossed blade arms
            for (int arm = 0; arm < 2; arm++) {
                double baseAngle = arm * Math.PI / 2;
                for (int i = -2; i <= 2; i++) {
                    if (i == 0 && arm > 0) continue; // skip center duplicate
                    double dist = i * 0.8;
                    double x = Math.cos(baseAngle) * dist;
                    double z = Math.sin(baseAngle) * dist;
                    Location loc = center.clone().add(x, 1.2, z);
                    BlockDisplayHandle blade = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    blade.scale(0.5f, 0.15f, 0.5f)
                         .glow(200, 200, 210)
                         .interpolation(2, 0);
                    bladeBlocks.add(blade);
                    spawnedEntities.add(blade.entity());
                }
            }

            // Center hub
            BlockDisplayHandle hub = displayBuilder.spawnBlock(
                    center.clone().add(0, 1.2, 0), Material.NETHERITE_BLOCK);
            hub.scale(0.5f, 0.3f, 0.5f)
               .glow(60, 50, 50)
               .interpolation(2, 0);
            bladeBlocks.add(hub);
            spawnedEntities.add(hub.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float spinAngle = ticksAlive * 0.05f; // Slow spin

            // Rotate blade blocks around center
            int idx = 0;
            for (int arm = 0; arm < 2; arm++) {
                double baseAngle = arm * Math.PI / 2 + spinAngle;
                for (int i = -2; i <= 2; i++) {
                    if (i == 0 && arm > 0) continue;
                    if (idx >= bladeBlocks.size() - 1) break; // -1 for hub
                    double dist = i * 0.8;
                    double x = Math.cos(baseAngle) * dist;
                    double z = Math.sin(baseAngle) * dist;
                    bladeBlocks.get(idx).entity().teleport(c.clone().add(x, 1.2, z));
                    idx++;
                }
            }

            // Spin the hub
            bladeBlocks.get(bladeBlocks.size() - 1).rotate(spinAngle, 0, 1, 0);

            // Spark particles at blade tips
            if (ticksAlive % 3 == 0) {
                double tipAngle = spinAngle;
                Location tipLoc = c.clone().add(
                        Math.cos(tipAngle) * 1.6, 1.2, Math.sin(tipAngle) * 1.6);
                c.getWorld().spawnParticle(Particle.CRIT, tipLoc, 2, 0.1, 0.05, 0.1, 0.02);
            }

            // Grinding sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpinningBladeSlow(plugin); }
    }

    // ================================================================
    // 4. GRAVEL AVALANCHE — Wall of gravel blocks slides forward
    //    along the ground, pushing the player back
    // ================================================================
    public static class GravelAvalanche extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> gravelBlocks = new ArrayList<>();
        private float slideProgress = 0;
        private static final int WIDTH = 7;
        private static final int HEIGHT = 3;
        private static final float SLIDE_DISTANCE = 10.0f;

        public GravelAvalanche(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravel_avalanche", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wall of gravel blocks
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    double xOff = (x - WIDTH / 2.0) * 0.7;
                    Material mat = (Math.random() > 0.3) ? Material.GRAVEL : Material.COBBLESTONE;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(xOff, 0.5 + y * 0.7, -SLIDE_DISTANCE / 2), mat);
                    block.scale(0.65f, 0.65f, 0.65f)
                         .glow(140, 130, 120)
                         .interpolation(2, 0);
                    gravelBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            slideProgress = Math.min(1.0f, ticksAlive / 120.0f);
            float zPos = -SLIDE_DISTANCE / 2 + slideProgress * SLIDE_DISTANCE;

            int idx = 0;
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (idx >= gravelBlocks.size()) break;
                    double xOff = (x - WIDTH / 2.0) * 0.7;
                    // Jitter for tumbling effect
                    float jitterX = (float) Math.sin(ticksAlive * 0.2 + x + y) * 0.05f;
                    float jitterY = (float) Math.abs(Math.sin(ticksAlive * 0.15 + x * 2)) * 0.08f;
                    gravelBlocks.get(idx).entity().teleport(
                            c.clone().add(xOff + jitterX, 0.5 + y * 0.7 + jitterY, zPos));
                    idx++;
                }
            }

            // Dust cloud particles at front
            if (ticksAlive % 3 == 0) {
                Location frontLoc = c.clone().add(0, 0.5, zPos + 0.5);
                c.getWorld().spawnParticle(Particle.BLOCK, frontLoc, 8, 2.0, 0.3, 0.3, 0.1,
                        Material.GRAVEL.createBlockData());
                DisplayBuilder.dustParticles(frontLoc, 4, 1.5, 140, 130, 120, 1.5f);
            }

            // Rumble
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 0, zPos), Sound.BLOCK_GRAVEL_BREAK, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravelAvalanche(plugin); }
    }

    // ================================================================
    // 5. LAVA PUDDLE WARNING — Circular lava puddle that expands
    //    slowly with clear warning before damage activates
    // ================================================================
    public static class LavaPuddleWarning extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> lavaBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> warningBlocks = new ArrayList<>();
        private static final int LAVA_COUNT = 12;
        private boolean damageActive = false;

        public LavaPuddleWarning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_puddle_warning", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
            config.setDamageDelayTicks(40); // 2 second warning
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning ring first (orange concrete)
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 0.05, Math.sin(angle) * 2.5);
                BlockDisplayHandle warning = displayBuilder.spawnBlock(loc, Material.ORANGE_CONCRETE);
                warning.scale(0.5f, 0.05f, 0.5f)
                       .glow(255, 150, 30)
                       .interpolation(4, 0);
                warningBlocks.add(warning);
                spawnedEntities.add(warning.entity());
            }

            // Lava blocks (start tiny)
            for (int i = 0; i < LAVA_COUNT; i++) {
                double angle = (2 * Math.PI * i) / LAVA_COUNT;
                double dist = Math.random() * 2.0;
                Location loc = center.clone().add(Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                BlockDisplayHandle lava = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                lava.scale(0.01f, 0.01f, 0.01f) // Hidden initially
                    .glow(255, 100, 20)
                    .interpolation(6, 0);
                lavaBlocks.add(lava);
                spawnedEntities.add(lava.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Warning phase: blink warning ring
            if (ticksAlive < 40) {
                boolean blink = (ticksAlive / 5) % 2 == 0;
                for (BlockDisplayHandle warning : warningBlocks) {
                    warning.scale(blink ? 0.5f : 0.3f, 0.05f, blink ? 0.5f : 0.3f);
                }

                // Warning sound
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
                }
            }

            // Lava expansion phase
            if (ticksAlive >= 30) {
                float lavaProgress = Math.min(1.0f, (ticksAlive - 30) / 30.0f);
                for (int i = 0; i < lavaBlocks.size(); i++) {
                    float size = 0.5f * lavaProgress + (float) Math.sin(ticksAlive * 0.1 + i) * 0.05f;
                    lavaBlocks.get(i).scale(size, 0.08f, size);
                }

                // Hide warning when lava is fully expanded
                if (lavaProgress >= 1.0f) {
                    for (BlockDisplayHandle warning : warningBlocks) {
                        warning.scale(0.01f, 0.01f, 0.01f);
                    }
                }
            }

            // Lava bubbles
            if (ticksAlive >= 40 && ticksAlive % 5 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 2.0;
                Location bubbleLoc = c.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);
                c.getWorld().spawnParticle(Particle.LAVA, bubbleLoc, 1, 0.1, 0.05, 0.1, 0);
                DisplayBuilder.dustParticles(bubbleLoc, 2, 0.2, 255, 100, 20, 0.8f);
            }

            // Ambient lava
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.4f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaPuddleWarning(plugin); }
    }

    // ================================================================
    // 6. SWINGING PENDULUM — Large iron pendulum swings back and
    //    forth through the center, predictable timing
    // ================================================================
    public static class SwingingPendulum extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> armBlocks = new ArrayList<>();
        private BlockDisplayHandle weight;
        private static final int ARM_SEGMENTS = 6;
        private static final double ARM_LENGTH = 5.0;

        public SwingingPendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("swinging_pendulum", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pivot mount at top
            BlockDisplayHandle pivot = displayBuilder.spawnBlock(
                    center.clone().add(0, 8, 0), Material.IRON_BLOCK);
            pivot.scale(0.6f, 0.4f, 0.6f)
                 .glow(180, 180, 190)
                 .interpolation(2, 0);
            spawnedEntities.add(pivot.entity());

            // Arm segments (chain)
            for (int i = 0; i < ARM_SEGMENTS; i++) {
                BlockDisplayHandle arm = displayBuilder.spawnBlock(
                        center.clone().add(0, 7 - i, 0), Material.CHAIN);
                arm.scale(0.3f, 0.8f, 0.3f)
                   .glow(200, 200, 220)
                   .interpolation(2, 0);
                armBlocks.add(arm);
                spawnedEntities.add(arm.entity());
            }

            // Heavy weight at bottom
            weight = displayBuilder.spawnBlock(
                    center.clone().add(0, 2, 0), Material.IRON_BLOCK);
            weight.scale(1.2f, 1.2f, 1.2f)
                  .glow(180, 180, 190)
                  .interpolation(2, 0);
            spawnedEntities.add(weight.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pendulum swing: sin wave, slow and predictable
            float swingAngle = (float) Math.sin(ticksAlive * 0.06) * 0.7f;

            // Position arm segments along the pendulum arc
            for (int i = 0; i < armBlocks.size(); i++) {
                double segDist = (i + 1) * (ARM_LENGTH / ARM_SEGMENTS);
                double x = Math.sin(swingAngle) * segDist;
                double y = 8 - Math.cos(swingAngle) * segDist;
                armBlocks.get(i).entity().teleport(c.clone().add(x, y, 0));
                armBlocks.get(i).rotate(swingAngle, 0, 0, 1);
            }

            // Weight position at end of arm
            double weightX = Math.sin(swingAngle) * ARM_LENGTH;
            double weightY = 8 - Math.cos(swingAngle) * ARM_LENGTH;
            weight.entity().teleport(c.clone().add(weightX, weightY, 0));
            weight.rotate(swingAngle, 0, 0, 1);

            // Wind whoosh at center of swing
            if (Math.abs(swingAngle) < 0.1f && ticksAlive % 20 < 2) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.6f);
            }

            // Chain rattle
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.3f, 0.8f);
            }

            // Danger sparks near weight
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT,
                        c.clone().add(weightX, weightY, 0), 2, 0.2, 0.2, 0.2, 0.02);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SwingingPendulum(plugin); }
    }

    // ================================================================
    // 7. CREEPING VINES — Vine blocks slowly spread from center outward
    //    along the ground, mild entangle damage
    // ================================================================
    public static class CreepingVines extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> vineBlocks = new ArrayList<>();
        private final List<double[]> vineTargets = new ArrayList<>();
        private final List<Float> vineProgress = new ArrayList<>();
        private static final int VINE_COUNT = 16;

        public CreepingVines(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_vines", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < VINE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / VINE_COUNT + (Math.random() - 0.5) * 0.3;
                double dist = 2.0 + Math.random() * 3.0;
                double targetX = Math.cos(angle) * dist;
                double targetZ = Math.sin(angle) * dist;

                BlockDisplayHandle vine = displayBuilder.spawnBlock(
                        center.clone().add(0, 0.1, 0), Material.VINE);
                vine.scale(0.01f, 0.01f, 0.01f) // Start invisible
                    .glow(60, 140, 40)
                    .interpolation(4, 0);
                vineBlocks.add(vine);
                spawnedEntities.add(vine.entity());
                vineTargets.add(new double[]{targetX, targetZ});
                vineProgress.add(0f);
            }

            // Center root cluster
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                BlockDisplayHandle root = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 0.3, 0.1, Math.sin(angle) * 0.3),
                        Material.DARK_OAK_LOG);
                root.scale(0.3f, 0.2f, 0.3f)
                    .glow(80, 60, 30)
                    .interpolation(3, 0);
                spawnedEntities.add(root.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRASS_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < vineBlocks.size(); i++) {
                // Each vine starts growing at a staggered time
                int startTick = i * 3;
                if (ticksAlive < startTick) continue;

                float progress = Math.min(1.0f, (ticksAlive - startTick) / 60.0f);
                vineProgress.set(i, progress);

                double[] target = vineTargets.get(i);
                double x = target[0] * progress;
                double z = target[1] * progress;

                vineBlocks.get(i).entity().teleport(c.clone().add(x, 0.1, z));
                float vineSize = 0.4f * progress;
                vineBlocks.get(i).scale(vineSize, 0.1f, vineSize);

                // Growth particles
                if (progress < 1.0f && ticksAlive % 6 == 0 && i % 4 == (ticksAlive / 6) % 4) {
                    DisplayBuilder.dustParticles(c.clone().add(x, 0.3, z), 2, 0.15, 60, 140, 40, 0.6f);
                }
            }

            // Rustling sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRASS_BREAK, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CreepingVines(plugin); }
    }

    // ================================================================
    // 8. BOUNCING BOULDER — Single large stone sphere that bounces
    //    around in a predictable pattern, easy to dodge
    // ================================================================
    public static class BouncingBoulder extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> boulderBlocks = new ArrayList<>();
        private double boulderX = 0, boulderY = 3, boulderZ = 0;
        private double velX = 0.15, velY = 0, velZ = 0.12;
        private float rollAngle = 0;

        public BouncingBoulder(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bouncing_boulder", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Boulder: cluster of stone blocks forming a rough sphere
            double[][] sphereOffsets = {
                {0, 0, 0},       // center
                {0.5, 0, 0}, {-0.5, 0, 0}, {0, 0, 0.5}, {0, 0, -0.5},
                {0, 0.5, 0}, {0, -0.5, 0},
                {0.35, 0.35, 0}, {-0.35, 0.35, 0}, {0, 0.35, 0.35}, {0, 0.35, -0.35},
            };

            Material[] stoneMats = {Material.STONE, Material.COBBLESTONE, Material.ANDESITE};
            for (int i = 0; i < sphereOffsets.length; i++) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(sphereOffsets[i][0], 3 + sphereOffsets[i][1], sphereOffsets[i][2]),
                        stoneMats[i % stoneMats.length]);
                block.scale(0.55f, 0.55f, 0.55f)
                     .glow(140, 140, 140)
                     .interpolation(2, 0);
                boulderBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Physics
            velY -= 0.02; // gravity
            boulderX += velX;
            boulderY += velY;
            boulderZ += velZ;

            // Bounce off ground
            if (boulderY < 0.7) {
                boulderY = 0.7;
                velY = Math.abs(velY) * 0.7; // energy loss
                DisplayBuilder.playSound(c.clone().add(boulderX, 0, boulderZ),
                        Sound.BLOCK_STONE_FALL, 0.6f, 0.7f);
                c.getWorld().spawnParticle(Particle.BLOCK,
                        c.clone().add(boulderX, 0.5, boulderZ),
                        6, 0.3, 0.1, 0.3, 0.05, Material.STONE.createBlockData());
            }

            // Bounce off invisible walls (keep in area)
            if (Math.abs(boulderX) > 5) { velX = -velX; boulderX = Math.signum(boulderX) * 5; }
            if (Math.abs(boulderZ) > 5) { velZ = -velZ; boulderZ = Math.signum(boulderZ) * 5; }

            // Roll angle
            rollAngle += 0.12f;

            // Move all boulder blocks
            double[][] sphereOffsets = {
                {0, 0, 0}, {0.5, 0, 0}, {-0.5, 0, 0}, {0, 0, 0.5}, {0, 0, -0.5},
                {0, 0.5, 0}, {0, -0.5, 0},
                {0.35, 0.35, 0}, {-0.35, 0.35, 0}, {0, 0.35, 0.35}, {0, 0.35, -0.35},
            };
            for (int i = 0; i < boulderBlocks.size() && i < sphereOffsets.length; i++) {
                double[] off = sphereOffsets[i];
                // Rotate offsets for rolling
                double rX = off[0] * Math.cos(rollAngle) - off[1] * Math.sin(rollAngle);
                double rY = off[0] * Math.sin(rollAngle) + off[1] * Math.cos(rollAngle);
                boulderBlocks.get(i).entity().teleport(
                        c.clone().add(boulderX + rX, boulderY + rY, boulderZ + off[2]));
            }

            // Rumble
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(boulderX, boulderY - 0.3, boulderZ),
                        3, 0.3, 140, 140, 140, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BouncingBoulder(plugin); }
    }

    // ================================================================
    // 9. WIND GUST — Line of white/light blue blocks sweeps across
    //    horizontally, brief knockback-like visual
    // ================================================================
    public static class WindGust extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> windBlocks = new ArrayList<>();
        private float sweepProgress = 0;
        private static final int WIND_WIDTH = 10;
        private static final int WIND_HEIGHT = 4;
        private static final float SWEEP_SPEED = 0.15f;

        public WindGust(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wind_gust", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wall of semi-transparent wind blocks
            for (int x = 0; x < WIND_WIDTH; x++) {
                for (int y = 0; y < WIND_HEIGHT; y++) {
                    double xOff = (x - WIND_WIDTH / 2.0) * 0.7;
                    Material mat = (Math.random() > 0.5) ? Material.WHITE_CONCRETE : Material.LIGHT_BLUE_CONCRETE;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(-8, 0.5 + y * 0.7, xOff), mat);
                    block.scale(0.15f, 0.6f, 0.6f)
                         .glow(220, 230, 255)
                         .interpolation(2, 0);
                    windBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            sweepProgress += SWEEP_SPEED;
            float xPos = -8 + sweepProgress;

            int idx = 0;
            for (int x = 0; x < WIND_WIDTH; x++) {
                for (int y = 0; y < WIND_HEIGHT; y++) {
                    if (idx >= windBlocks.size()) break;
                    double zOff = (x - WIND_WIDTH / 2.0) * 0.7;
                    // Wave motion in each wind block
                    float wave = (float) Math.sin(ticksAlive * 0.2 + x * 0.5 + y * 0.3) * 0.3f;
                    windBlocks.get(idx).entity().teleport(
                            c.clone().add(xPos + wave, 0.5 + y * 0.7, zOff));
                    idx++;
                }
            }

            // Wind whoosh particles ahead of the wall
            if (ticksAlive % 2 == 0) {
                Location aheadLoc = c.clone().add(xPos + 1, 1 + Math.random() * 2,
                        (Math.random() - 0.5) * 5);
                c.getWorld().spawnParticle(Particle.CLOUD, aheadLoc, 2, 0.3, 0.3, 0.3, 0.02);
            }

            // Whoosh sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(xPos, 1, 0),
                        Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WindGust(plugin); }
    }

    // ================================================================
    // 10. THORN BUSH — Cluster of pointed blocks that grows from the
    //     ground, deals contact damage, stays in place
    // ================================================================
    public static class ThornBush extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> thornBlocks = new ArrayList<>();
        private final List<Float> thornGrowth = new ArrayList<>();
        private static final int THORN_COUNT = 14;

        public ThornBush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thorn_bush", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < THORN_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 2.0;
                double x = Math.cos(angle) * dist;
                double z = Math.sin(angle) * dist;

                Material mat = (i % 3 == 0) ? Material.DARK_OAK_LOG :
                               (i % 3 == 1) ? Material.SPRUCE_LEAVES : Material.DARK_OAK_PLANKS;
                BlockDisplayHandle thorn = displayBuilder.spawnBlock(
                        center.clone().add(x, 0.1, z), mat);
                thorn.scale(0.01f, 0.01f, 0.01f)
                     .glow(60, 100, 30)
                     .interpolation(6, 0);
                thornBlocks.add(thorn);
                spawnedEntities.add(thorn.entity());
                thornGrowth.add(0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SWEET_BERRY_BUSH_PLACE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < thornBlocks.size(); i++) {
                int startTick = i * 4;
                if (ticksAlive < startTick) continue;

                float growth = Math.min(1.0f, (ticksAlive - startTick) / 20.0f);
                thornGrowth.set(i, growth);

                // Thorns are tall and thin (pointed)
                float width = 0.15f + (float) Math.random() * 0.1f;
                float height = (0.8f + (float)(Math.random() * 0.8)) * growth;
                thornBlocks.get(i).scale(width * growth, height, width * growth);

                // Slight sway
                if (growth >= 1.0f) {
                    float sway = (float) Math.sin(ticksAlive * 0.08 + i) * 0.03f;
                    thornBlocks.get(i).rotate(sway, 0, 0, 1);
                }
            }

            // Prickle particles
            if (ticksAlive % 8 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 2.0;
                DisplayBuilder.dustParticles(
                        c.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist),
                        2, 0.1, 60, 100, 30, 0.5f);
            }

            // Berry bush sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 0.3f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThornBush(plugin); }
    }

    // ================================================================
    // 11. ICE SPIKE — Pointed ice blocks erupt from the ground upward
    //     in a ring pattern with frost particles
    // ================================================================
    public static class IceSpike extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private final List<Float> spikeHeights = new ArrayList<>();
        private final List<Double> spikeX = new ArrayList<>();
        private final List<Double> spikeZ = new ArrayList<>();
        private static final int SPIKE_COUNT = 10;

        public IceSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_spike", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SPIKE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / SPIKE_COUNT;
                double dist = 1.5 + Math.random() * 1.5;
                double x = Math.cos(angle) * dist;
                double z = Math.sin(angle) * dist;

                Material mat = (i % 3 == 0) ? Material.BLUE_ICE :
                               (i % 3 == 1) ? Material.PACKED_ICE : Material.ICE;
                BlockDisplayHandle spike = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), mat);
                spike.scale(0.01f, 0.01f, 0.01f)
                     .glow(150, 210, 255)
                     .interpolation(4, 0);
                spikeBlocks.add(spike);
                spawnedEntities.add(spike.entity());
                spikeHeights.add(1.5f + (float)(Math.random() * 2.0));
                spikeX.add(x);
                spikeZ.add(z);
            }

            // Center ice formation
            BlockDisplayHandle centerIce = displayBuilder.spawnBlock(
                    center.clone().add(0, 0, 0), Material.BLUE_ICE);
            centerIce.scale(0.01f, 0.01f, 0.01f)
                     .glow(150, 210, 255)
                     .interpolation(4, 0);
            spikeBlocks.add(centerIce);
            spawnedEntities.add(centerIce.entity());
            spikeHeights.add(3.0f);
            spikeX.add(0.0);
            spikeZ.add(0.0);

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < spikeBlocks.size(); i++) {
                int startTick = i * 5;
                if (ticksAlive < startTick) continue;

                float eruptProgress = Math.min(1.0f, (ticksAlive - startTick) / 15.0f);
                float targetHeight = spikeHeights.get(i);
                float currentHeight = targetHeight * eruptProgress;

                // Spike: narrow at top, wider at base
                float baseWidth = 0.3f + (targetHeight - 1.5f) * 0.1f;
                spikeBlocks.get(i).scale(
                        baseWidth * eruptProgress,
                        currentHeight,
                        baseWidth * eruptProgress);

                // Eruption particles
                if (eruptProgress < 1.0f && eruptProgress > 0.1f) {
                    DisplayBuilder.dustParticles(
                            c.clone().add(spikeX.get(i), currentHeight * 0.5, spikeZ.get(i)),
                            3, 0.2, 150, 210, 255, 1.0f);
                }

                // Sound on eruption start
                if (ticksAlive == startTick) {
                    DisplayBuilder.playSound(
                            c.clone().add(spikeX.get(i), 0, spikeZ.get(i)),
                            Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f + (float) Math.random() * 0.5f);
                }
            }

            // Frost particles around the formation
            if (ticksAlive % 4 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 3.0;
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(Math.cos(angle) * dist, 0.5 + Math.random() * 2, Math.sin(angle) * dist),
                        2, 0.1, 0.1, 0.1, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceSpike(plugin); }
    }

    // ================================================================
    // 12. SAND SINKHOLE — Circular area of sand blocks that descend,
    //     pulling the visual downward with a whirlpool pattern
    // ================================================================
    public static class SandSinkhole extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sandBlocks = new ArrayList<>();
        private final List<Double> sandAngles = new ArrayList<>();
        private final List<Double> sandDistances = new ArrayList<>();
        private static final int SAND_COUNT = 20;
        private static final double HOLE_RADIUS = 3.0;

        public SandSinkhole(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sand_sinkhole", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SAND_COUNT; i++) {
                double angle = (2 * Math.PI * i) / SAND_COUNT;
                double dist = 0.5 + (i % 4) * 0.7;
                double x = Math.cos(angle) * dist;
                double z = Math.sin(angle) * dist;

                Material mat = (i % 3 == 0) ? Material.SANDSTONE :
                               (i % 3 == 1) ? Material.SAND : Material.RED_SAND;
                BlockDisplayHandle sand = displayBuilder.spawnBlock(
                        center.clone().add(x, 0.1, z), mat);
                sand.scale(0.5f, 0.15f, 0.5f)
                    .glow(220, 190, 130)
                    .interpolation(3, 0);
                sandBlocks.add(sand);
                spawnedEntities.add(sand.entity());
                sandAngles.add(angle);
                sandDistances.add(dist);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SAND_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float sinkProgress = Math.min(1.0f, ticksAlive / 80.0f);
            float spinSpeed = ticksAlive * 0.03f;

            for (int i = 0; i < sandBlocks.size(); i++) {
                double angle = sandAngles.get(i) + spinSpeed; // Whirlpool spin
                double dist = sandDistances.get(i) * (1.0 - sinkProgress * 0.3); // Pull inward
                double x = Math.cos(angle) * dist;
                double z = Math.sin(angle) * dist;
                double y = 0.1 - sinkProgress * (HOLE_RADIUS - dist) * 0.3; // Center sinks more

                sandBlocks.get(i).entity().teleport(c.clone().add(x, y, z));

                // Tilt blocks toward center (sinking effect)
                float tiltAngle = sinkProgress * 0.3f * (float)(1.0 - dist / HOLE_RADIUS);
                sandBlocks.get(i).rotate(tiltAngle, (float) Math.cos(angle), 0, (float) Math.sin(angle));
            }

            // Dust swirl particles
            if (ticksAlive % 3 == 0) {
                double dustAngle = spinSpeed * 2 + Math.random() * 0.5;
                double dustDist = Math.random() * HOLE_RADIUS;
                Location dustLoc = c.clone().add(
                        Math.cos(dustAngle) * dustDist,
                        0.3 - sinkProgress * 0.5,
                        Math.sin(dustAngle) * dustDist);
                DisplayBuilder.dustParticles(dustLoc, 3, 0.2, 220, 190, 130, 0.8f);
            }

            // Sand falling sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SAND_FALL, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SandSinkhole(plugin); }
    }

    // ================================================================
    // 13. FIRE RING HOP — Ring of fire blocks that expand/contract,
    //     teaching players to time their movement through gaps
    // ================================================================
    public static class FireRingHop extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fireBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> gapMarkers = new ArrayList<>();
        private static final int RING_COUNT = 16;
        private static final double BASE_RADIUS = 3.0;
        private int gapIndex = 0;

        public FireRingHop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_ring_hop", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < RING_COUNT; i++) {
                double angle = (2 * Math.PI * i) / RING_COUNT;
                double x = Math.cos(angle) * BASE_RADIUS;
                double z = Math.sin(angle) * BASE_RADIUS;

                BlockDisplayHandle fire = displayBuilder.spawnBlock(
                        center.clone().add(x, 0.3, z), Material.ORANGE_CONCRETE);
                fire.scale(0.5f, 0.8f, 0.5f)
                    .glow(255, 100, 20)
                    .interpolation(3, 0);
                fireBlocks.add(fire);
                spawnedEntities.add(fire.entity());
            }

            // Gap markers (green, show safe passage)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle marker = displayBuilder.spawnBlock(
                        center.clone().add(0, 0.1, 0), Material.LIME_CONCRETE);
                marker.scale(0.4f, 0.05f, 0.4f)
                      .glow(50, 200, 80)
                      .interpolation(3, 0);
                gapMarkers.add(marker);
                spawnedEntities.add(marker.entity());
            }

            gapIndex = 0;
            DisplayBuilder.playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gap rotates around the ring
            gapIndex = (ticksAlive / 20) % RING_COUNT;
            int gapIndex2 = (gapIndex + RING_COUNT / 2) % RING_COUNT; // Opposite gap

            // Radius pulsing
            float radiusPulse = (float) (BASE_RADIUS + Math.sin(ticksAlive * 0.08) * 0.5);

            for (int i = 0; i < fireBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / RING_COUNT;
                double x = Math.cos(angle) * radiusPulse;
                double z = Math.sin(angle) * radiusPulse;

                boolean isGap = (i == gapIndex || i == gapIndex2);
                if (isGap) {
                    // Gap: shrink fire block
                    fireBlocks.get(i).scale(0.1f, 0.1f, 0.1f);
                } else {
                    // Active fire
                    float flicker = 0.8f + (float) Math.sin(ticksAlive * 0.2 + i) * 0.2f;
                    fireBlocks.get(i).scale(0.5f, flicker, 0.5f);
                    fireBlocks.get(i).entity().teleport(c.clone().add(x, 0.3, z));
                }
            }

            // Position gap markers
            double gapAngle1 = (2 * Math.PI * gapIndex) / RING_COUNT;
            double gapAngle2 = (2 * Math.PI * gapIndex2) / RING_COUNT;
            gapMarkers.get(0).entity().teleport(c.clone().add(
                    Math.cos(gapAngle1) * radiusPulse, 0.1, Math.sin(gapAngle1) * radiusPulse));
            gapMarkers.get(1).entity().teleport(c.clone().add(
                    Math.cos(gapAngle2) * radiusPulse, 0.1, Math.sin(gapAngle2) * radiusPulse));

            // Fire particles
            if (ticksAlive % 3 == 0) {
                int randomFire = (int) (Math.random() * RING_COUNT);
                if (randomFire != gapIndex && randomFire != gapIndex2) {
                    double fAngle = (2 * Math.PI * randomFire) / RING_COUNT;
                    Location fireLoc = c.clone().add(
                            Math.cos(fAngle) * radiusPulse, 0.8, Math.sin(fAngle) * radiusPulse);
                    c.getWorld().spawnParticle(Particle.FLAME, fireLoc, 2, 0.1, 0.2, 0.1, 0.01);
                }
            }

            // Fire crackle
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f);
            }

            // Gap change sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FireRingHop(plugin); }
    }
}
