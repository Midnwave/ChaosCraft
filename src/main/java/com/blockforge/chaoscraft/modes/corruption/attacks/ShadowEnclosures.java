package com.blockforge.chaoscraft.modes.corruption.attacks;

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
 * Corruption Mode — SHADOW ENCLOSURE ATTACKS (Decay Structures)
 * 15 corruption-themed BlockDisplay attacks featuring crumbling architecture,
 * decaying structures, and shadow-infused ruins that collapse and enclose players.
 *
 * Color palette:
 * - Dark purple: RGB(45, 0, 64)
 * - Crimson: RGB(74, 0, 0)
 * - Void blue: RGB(10, 0, 48)
 *
 * Materials: SCULK, DEEPSLATE, BLACKSTONE, CRYING_OBSIDIAN, OBSIDIAN, COAL_BLOCK, NETHERRACK, SOUL_SOIL
 * Sounds: BLOCK_SCULK_SPREAD, BLOCK_SCULK_BREAK, BLOCK_DEEPSLATE_BREAK, ENTITY_WARDEN_HEARTBEAT
 */
public final class ShadowEnclosures {

    private ShadowEnclosures() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrumblingWall(plugin));
        registry.register(new CorruptedArch(plugin));
        registry.register(new RuinedTower(plugin));
        registry.register(new DecayBridge(plugin));
        registry.register(new CorruptedStaircase(plugin));
        registry.register(new DarkRuins(plugin));
        registry.register(new CorruptionCage(plugin));
        registry.register(new DecayingPlatform(plugin));
        registry.register(new ShadowScaffold(plugin));
        registry.register(new CorruptionThrone(plugin));
        registry.register(new DarkAltar(plugin));
        registry.register(new CorruptedObeliskRing(plugin));
        registry.register(new DecayDome(plugin));
        registry.register(new ShadowBarricade(plugin));
        registry.register(new CorruptionPyramid(plugin));
    }

    // ================================================================
    // Helper: pick a random corruption material
    // ================================================================
    private static Material corruptionMaterial(int index) {
        Material[] mats = {
            Material.SCULK, Material.DEEPSLATE, Material.BLACKSTONE,
            Material.CRYING_OBSIDIAN, Material.OBSIDIAN, Material.COAL_BLOCK,
            Material.NETHERRACK, Material.SOUL_SOIL
        };
        return mats[Math.abs(index) % mats.length];
    }

    // ================================================================
    // 61. CRUMBLING WALL — 16 blocks wall section, blocks fall off
    //     over time as the structure decays from top to bottom
    // ================================================================
    public static class CrumblingWall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<Float> fallDelays = new ArrayList<>();
        private final List<Boolean> fallen = new ArrayList<>();
        private final List<Float> fallY = new ArrayList<>();
        private static final int BLOCK_COUNT = 16;

        public CrumblingWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crumbling_wall", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 wide x 4 tall wall
            int idx = 0;
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    if (idx >= BLOCK_COUNT) break;
                    double x = (col - 1.5) * 1.1;
                    double y = row * 1.1;
                    Location loc = center.clone().add(x, y, 0);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, corruptionMaterial(idx));
                    block.scale(1.0f, 1.0f, 0.8f)
                         .glow(45, 0, 64)
                         .interpolation(3, 0);
                    wallBlocks.add(block);
                    spawnedEntities.add(block.entity());
                    // Top rows fall sooner
                    fallDelays.add(40.0f + (3 - row) * 30.0f + (float)(Math.random() * 40));
                    fallen.add(false);
                    fallY.add((float) y);
                    idx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < wallBlocks.size(); i++) {
                if (fallen.get(i)) continue;

                if (ticksAlive >= fallDelays.get(i)) {
                    // Start falling
                    float y = fallY.get(i) - 0.25f;
                    fallY.set(i, y);

                    int col = i % 4;
                    double x = (col - 1.5) * 1.1;
                    wallBlocks.get(i).entity().teleport(c.clone().add(x, y, 0));

                    // Random tilt as it falls
                    float tiltAngle = (float)(Math.random() * 0.3);
                    wallBlocks.get(i).rotate(tiltAngle, 0, 0, 1);

                    if (y <= -1.0f) {
                        fallen.set(i, true);
                        Location impactLoc = c.clone().add(x, 0, 0);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.dustParticles(impactLoc, 8, 0.5, 45, 0, 64, 1.5f);
                        DisplayBuilder.playSound(impactLoc, Sound.BLOCK_SCULK_BREAK, 0.7f, 0.6f);
                    }
                }
            }

            // Ambient corruption particles
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 5, 1.5, 45, 0, 64, 1.0f);
            }
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrumblingWall(plugin); }
    }

    // ================================================================
    // 62. CORRUPTED ARCH — 14 blocks archway, crumbles from the top
    //     Keystone falls first, then sides collapse inward
    // ================================================================
    public static class CorruptedArch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private final List<double[]> blockOffsets = new ArrayList<>();
        private final List<Integer> crumbleOrder = new ArrayList<>();
        private final List<Boolean> crumbled = new ArrayList<>();
        private final List<Float> currentY = new ArrayList<>();
        private static final int BLOCK_COUNT = 14;

        public CorruptedArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corrupted_arch", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left pillar (5 blocks)
            for (int i = 0; i < 5; i++) {
                double x = -2.5;
                double y = i * 1.1;
                addArchBlock(center, x, y, 0, i);
            }
            // Right pillar (5 blocks)
            for (int i = 0; i < 5; i++) {
                double x = 2.5;
                double y = i * 1.1;
                addArchBlock(center, x, y, 0, 5 + i);
            }
            // Arch top (4 blocks curving over)
            double[] archX = {-1.5, -0.5, 0.5, 1.5};
            double[] archY = {5.2, 5.8, 5.8, 5.2};
            for (int i = 0; i < 4; i++) {
                addArchBlock(center, archX[i], archY[i], 0, 10 + i);
            }

            // Crumble order: top center first, then outward, then pillars top-down
            crumbleOrder.add(11); crumbleOrder.add(12); // center arch
            crumbleOrder.add(10); crumbleOrder.add(13); // outer arch
            crumbleOrder.add(4); crumbleOrder.add(9);   // pillar tops
            crumbleOrder.add(3); crumbleOrder.add(8);
            crumbleOrder.add(2); crumbleOrder.add(7);
            crumbleOrder.add(1); crumbleOrder.add(6);
            crumbleOrder.add(0); crumbleOrder.add(5);

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.6f);
        }

        private void addArchBlock(Location center, double x, double y, double z, int idx) {
            Location loc = center.clone().add(x, y, z);
            BlockDisplayHandle block = displayBuilder.spawnBlock(loc, corruptionMaterial(idx));
            block.scale(1.0f, 1.0f, 1.2f)
                 .glow(74, 0, 0)
                 .interpolation(3, 0);
            archBlocks.add(block);
            spawnedEntities.add(block.entity());
            blockOffsets.add(new double[]{x, y, z});
            crumbled.add(false);
            currentY.add((float) y);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Crumble one block every 15 ticks
            int crumbleIdx = ticksAlive / 15;
            if (crumbleIdx < crumbleOrder.size()) {
                int blockIdx = crumbleOrder.get(crumbleIdx);
                if (!crumbled.get(blockIdx) && ticksAlive % 15 == 0) {
                    crumbled.set(blockIdx, true);
                    DisplayBuilder.playSound(
                        c.clone().add(blockOffsets.get(blockIdx)[0], blockOffsets.get(blockIdx)[1], 0),
                        Sound.BLOCK_SCULK_BREAK, 0.6f, 0.8f
                    );
                }
            }

            // Animate falling crumbled blocks
            for (int i = 0; i < archBlocks.size(); i++) {
                if (!crumbled.get(i)) continue;
                float y = currentY.get(i) - 0.3f;
                currentY.set(i, y);
                double[] off = blockOffsets.get(i);
                archBlocks.get(i).entity().teleport(c.clone().add(off[0], y, off[2]));
                archBlocks.get(i).rotate((float)(Math.random() * 0.2), 1, 0, 0);

                if (y <= -0.5f && y > -1.0f) {
                    Location impactLoc = c.clone().add(off[0], 0, off[2]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 6, 0.4, 74, 0, 0, 1.2f);
                }
            }

            // Dark particle mist around arch
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 8, 2.0, 10, 0, 48, 1.0f);
            }
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptedArch(plugin); }
    }

    // ================================================================
    // 63. RUINED TOWER — 18 blocks broken tower, top section tilts
    //     and falls with ground-shaking impact damage
    // ================================================================
    public static class RuinedTower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> topBlocks = new ArrayList<>();
        private final List<double[]> topOffsets = new ArrayList<>();
        private float tiltAngle = 0;
        private boolean topFalling = false;
        private boolean impactDone = false;
        private static final int BASE_COUNT = 10;
        private static final int TOP_COUNT = 8;
        private static final int TILT_START_TICK = 60;

        public RuinedTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ruined_tower", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base section: 2x5 pillar (stable)
            for (int i = 0; i < BASE_COUNT; i++) {
                double x = (i % 2 == 0) ? -0.5 : 0.5;
                double y = (i / 2) * 1.1;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                block.scale(1.0f, 1.0f, 1.0f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                baseBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Top section: 2x4 (will tilt and fall)
            for (int i = 0; i < TOP_COUNT; i++) {
                double x = (i % 2 == 0) ? -0.5 : 0.5;
                double y = 5.5 + (i / 2) * 1.1;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                block.scale(1.0f, 1.0f, 1.0f)
                     .glow(74, 0, 0)
                     .interpolation(4, 0);
                topBlocks.add(block);
                spawnedEntities.add(block.entity());
                topOffsets.add(new double[]{x, y, 0});
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Creaking sounds before tilt
            if (ticksAlive > 30 && ticksAlive < TILT_START_TICK && ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 6, 0), Sound.BLOCK_SCULK_BREAK, 0.6f, 0.3f);
            }

            // Begin tilting
            if (ticksAlive >= TILT_START_TICK && !impactDone) {
                topFalling = true;
                tiltAngle += 0.03f;

                // Rotate top blocks around base pivot
                for (int i = 0; i < topBlocks.size(); i++) {
                    double[] off = topOffsets.get(i);
                    double pivotY = 5.5;
                    double relY = off[1] - pivotY;
                    double relX = off[0];

                    // Rotate around Z-axis
                    double newX = relX * Math.cos(tiltAngle) - relY * Math.sin(tiltAngle);
                    double newY = relX * Math.sin(tiltAngle) + relY * Math.cos(tiltAngle);

                    Location newLoc = c.clone().add(newX, pivotY + newY, off[2]);
                    topBlocks.get(i).entity().teleport(newLoc);
                    topBlocks.get(i).rotate(tiltAngle, 0, 0, 1);
                }

                // Impact when tilted ~90 degrees
                if (tiltAngle >= 1.4f && !impactDone) {
                    impactDone = true;
                    Location impactLoc = c.clone().add(4, 0, 0);
                    triggerImpactDamage(impactLoc);

                    // Shockwave
                    DisplayBuilder.particleRing(impactLoc, 4.0, Particle.DUST, 32,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.0f));
                    w.spawnParticle(Particle.BLOCK, impactLoc, 40, 2.0, 0.5, 2.0, 0.3,
                            Material.DEEPSLATE.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.2f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);
                }
            }

            // Ambient particles
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 4, 0), 4, 1.0, 10, 0, 48, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RuinedTower(plugin); }
    }

    // ================================================================
    // 64. DECAY BRIDGE — 15 blocks bridge collapses from center outward,
    //     each plank drops sequentially creating a wave of destruction
    // ================================================================
    public static class DecayBridge extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> planks = new ArrayList<>();
        private final List<Float> plankY = new ArrayList<>();
        private final List<Double> plankX = new ArrayList<>();
        private final List<Boolean> dropped = new ArrayList<>();
        private final List<Boolean> impactDone = new ArrayList<>();
        private static final int PLANK_COUNT = 15;

        public DecayBridge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("decay_bridge", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            float bridgeY = 4.0f;
            for (int i = 0; i < PLANK_COUNT; i++) {
                double x = (i - PLANK_COUNT / 2.0) * 1.1;
                Location loc = center.clone().add(x, bridgeY, 0);
                Material mat = (i % 2 == 0) ? Material.DEEPSLATE : Material.BLACKSTONE;
                BlockDisplayHandle plank = displayBuilder.spawnBlock(loc, mat);
                plank.scale(1.0f, 0.3f, 2.0f)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                planks.add(plank);
                spawnedEntities.add(plank.entity());
                plankY.add(bridgeY);
                plankX.add(x);
                dropped.add(false);
                impactDone.add(false);
            }

            // Side rails
            Location leftRail = center.clone().add(-(PLANK_COUNT / 2.0) * 1.1, bridgeY + 1.0, 0);
            Location rightRail = center.clone().add((PLANK_COUNT / 2.0) * 1.1, bridgeY + 1.0, 0);
            BlockDisplayHandle lrail = displayBuilder.spawnBlock(leftRail, Material.SCULK);
            lrail.scale(0.3f, 1.2f, 0.3f).glow(10, 0, 48);
            spawnedEntities.add(lrail.entity());
            BlockDisplayHandle rrail = displayBuilder.spawnBlock(rightRail, Material.SCULK);
            rrail.scale(0.3f, 1.2f, 0.3f).glow(10, 0, 48);
            spawnedEntities.add(rrail.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drop planks from center outward
            int centerIdx = PLANK_COUNT / 2;
            int dropRadius = ticksAlive / 8; // one pair every 8 ticks

            for (int i = 0; i < PLANK_COUNT; i++) {
                int distFromCenter = Math.abs(i - centerIdx);
                if (distFromCenter <= dropRadius && !dropped.get(i) && ticksAlive > 40) {
                    dropped.set(i, true);
                    DisplayBuilder.playSound(
                        c.clone().add(plankX.get(i), plankY.get(i), 0),
                        Sound.BLOCK_SCULK_BREAK, 0.5f, 0.7f + (float)(Math.random() * 0.3)
                    );
                }

                if (dropped.get(i)) {
                    float y = plankY.get(i) - 0.2f;
                    plankY.set(i, y);
                    planks.get(i).entity().teleport(c.clone().add(plankX.get(i), y, 0));
                    planks.get(i).rotate((float)(Math.random() * 0.15), 1, 0, 0);

                    if (y <= 0 && !impactDone.get(i)) {
                        impactDone.set(i, true);
                        Location impactLoc = c.clone().add(plankX.get(i), 0, 0);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.dustParticles(impactLoc, 6, 0.3, 45, 0, 64, 1.0f);
                    }
                }
            }

            // Ambient creaking
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DecayBridge(plugin); }
    }

    // ================================================================
    // 65. CORRUPTED STAIRCASE — 14 blocks spiral stairs, steps break
    //     bottom-up as corruption rises through the structure
    // ================================================================
    public static class CorruptedStaircase extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> steps = new ArrayList<>();
        private final List<double[]> stepOffsets = new ArrayList<>();
        private final List<Boolean> broken = new ArrayList<>();
        private final List<Float> fallY = new ArrayList<>();
        private static final int STEP_COUNT = 14;

        public CorruptedStaircase(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corrupted_staircase", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(380);
            config.setCooldownTicks(310);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spiral staircase around a central column
            for (int i = 0; i < STEP_COUNT; i++) {
                double angle = (Math.PI * 2 * i) / 8.0; // full rotation every 8 steps
                double radius = 2.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = i * 0.7;

                Location loc = center.clone().add(x, y, z);
                Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN : Material.DEEPSLATE;
                BlockDisplayHandle step = displayBuilder.spawnBlock(loc, mat);
                step.scale(1.2f, 0.3f, 1.2f)
                    .glow(74, 0, 0)
                    .interpolation(3, 0);
                // Angle step to face center
                float rotAngle = (float) angle;
                step.rotate(rotAngle, 0, 1, 0);

                steps.add(step);
                spawnedEntities.add(step.entity());
                stepOffsets.add(new double[]{x, y, z});
                broken.add(false);
                fallY.add((float) y);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Break steps bottom-up, one every 12 ticks after initial delay
            if (ticksAlive > 50) {
                int breakIdx = (ticksAlive - 50) / 12;
                if (breakIdx < steps.size() && !broken.get(breakIdx) && (ticksAlive - 50) % 12 == 0) {
                    broken.set(breakIdx, true);
                    double[] off = stepOffsets.get(breakIdx);
                    DisplayBuilder.playSound(
                        c.clone().add(off[0], off[1], off[2]),
                        Sound.BLOCK_SCULK_BREAK, 0.7f, 0.6f
                    );
                }
            }

            // Animate broken steps falling
            for (int i = 0; i < steps.size(); i++) {
                if (!broken.get(i)) continue;

                float y = fallY.get(i) - 0.15f;
                fallY.set(i, y);
                double[] off = stepOffsets.get(i);
                steps.get(i).entity().teleport(c.clone().add(off[0], y, off[2]));

                // Tumble rotation
                float tumble = (float)((stepOffsets.get(i)[1] - y) * 0.15);
                steps.get(i).rotate(tumble, 1, 0, 1);

                if (y <= -0.5f && y > -1.0f) {
                    Location impactLoc = c.clone().add(off[0], 0, off[2]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 5, 0.4, 74, 0, 0, 1.0f);
                }
            }

            // Corruption rising particles at center
            if (ticksAlive % 6 == 0) {
                float particleY = Math.min(STEP_COUNT * 0.7f, (ticksAlive - 50) * 0.06f);
                if (particleY > 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, particleY, 0), 6, 0.5, 10, 0, 48, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptedStaircase(plugin); }
    }

    // ================================================================
    // 66. DARK RUINS — 20 blocks partial building, blocks drift away
    //     slowly as if gravity is failing within the corrupted zone
    // ================================================================
    public static class DarkRuins extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ruinBlocks = new ArrayList<>();
        private final List<double[]> originalOffsets = new ArrayList<>();
        private final List<double[]> driftVelocities = new ArrayList<>();
        private final List<double[]> currentPositions = new ArrayList<>();
        private final List<Integer> driftStartTick = new ArrayList<>();
        private static final int BLOCK_COUNT = 20;

        public DarkRuins(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_ruins", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build a partial L-shaped ruin
            double[][] layout = {
                // Ground floor walls
                {-2,0,0}, {-1,0,0}, {0,0,0}, {1,0,0}, {2,0,0},
                {-2,1,0}, {-1,1,0}, {2,1,0},
                {-2,2,0}, {-1,2,0},
                // Side wall
                {-2,0,2}, {-2,1,2}, {-2,2,2}, {-2,3,2},
                // Corner pillars
                {-2,0,1}, {-2,1,1}, {-2,2,1}, {-2,3,1},
                // Scattered floor
                {0,0,1}, {1,0,1}
            };

            Material[] mats = {Material.DEEPSLATE, Material.BLACKSTONE, Material.OBSIDIAN, Material.SOUL_SOIL};

            for (int i = 0; i < BLOCK_COUNT && i < layout.length; i++) {
                Location loc = center.clone().add(layout[i][0], layout[i][1], layout[i][2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                block.scale(1.0f, 1.0f, 1.0f)
                     .glow(10, 0, 48)
                     .interpolation(5, 0);
                ruinBlocks.add(block);
                spawnedEntities.add(block.entity());
                originalOffsets.add(layout[i].clone());
                currentPositions.add(layout[i].clone());

                // Random drift direction (slow, eerie)
                double vx = (Math.random() - 0.5) * 0.06;
                double vy = 0.02 + Math.random() * 0.04;
                double vz = (Math.random() - 0.5) * 0.06;
                driftVelocities.add(new double[]{vx, vy, vz});

                // Stagger drift start times, higher blocks drift sooner
                driftStartTick.add(60 + (int)(Math.random() * 80) - (int)(layout[i][1] * 10));
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < ruinBlocks.size(); i++) {
                if (ticksAlive < driftStartTick.get(i)) continue;

                double[] pos = currentPositions.get(i);
                double[] vel = driftVelocities.get(i);
                pos[0] += vel[0];
                pos[1] += vel[1];
                pos[2] += vel[2];

                ruinBlocks.get(i).entity().teleport(c.clone().add(pos[0], pos[1], pos[2]));

                // Slow spin as blocks drift
                float spin = (ticksAlive - driftStartTick.get(i)) * 0.008f;
                ruinBlocks.get(i).rotate(spin, (float)(vel[0] > 0 ? 1 : -1), 0.5f, (float)(vel[2] > 0 ? 1 : -1));

                // Trailing void particles
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(
                        c.clone().add(pos[0], pos[1] - 0.5, pos[2]),
                        3, 0.2, 10, 0, 48, 0.8f
                    );
                }
            }

            // Damage in the ruin footprint area
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 1), 10, 2.5, 45, 0, 64, 1.2f);
            }

            // Eerie heartbeat
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkRuins(plugin); }
    }

    // ================================================================
    // 67. CORRUPTION CAGE — 16 blocks cage bars, bars slowly close
    //     inward trapping players in a shrinking prison
    // ================================================================
    public static class CorruptionCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private final List<double[]> barPositions = new ArrayList<>();
        private final List<Double> barAngles = new ArrayList<>();
        private float shrinkFactor = 1.0f;
        private static final int BAR_COUNT = 16;
        private static final float INITIAL_RADIUS = 5.0f;

        public CorruptionCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_cage", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BAR_COUNT; i++) {
                double angle = (Math.PI * 2 * i) / BAR_COUNT;
                double x = Math.cos(angle) * INITIAL_RADIUS;
                double z = Math.sin(angle) * INITIAL_RADIUS;

                Location loc = center.clone().add(x, 0, z);
                Material mat = (i % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle bar = displayBuilder.spawnBlock(loc, mat);
                bar.scale(0.4f, 5.0f, 0.4f)
                   .glow(45, 0, 64)
                   .interpolation(4, 0);

                bars.add(bar);
                spawnedEntities.add(bar.entity());
                barPositions.add(new double[]{x, 0, z});
                barAngles.add(angle);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly shrink the cage radius
            if (ticksAlive > 40 && shrinkFactor > 0.15f) {
                shrinkFactor -= 0.003f;
            }

            float currentRadius = INITIAL_RADIUS * shrinkFactor;

            for (int i = 0; i < bars.size(); i++) {
                double angle = barAngles.get(i);
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                bars.get(i).entity().teleport(c.clone().add(x, 0, z));

                // Bars rotate to face center as they close in
                float faceAngle = (float) angle + (float) Math.PI;
                bars.get(i).rotate(faceAngle, 0, 1, 0);
            }

            // Corruption particles inside the cage
            if (ticksAlive % 6 == 0) {
                double particleRadius = currentRadius * 0.7;
                double pAngle = Math.random() * Math.PI * 2;
                Location pLoc = c.clone().add(
                    Math.cos(pAngle) * particleRadius,
                    Math.random() * 4,
                    Math.sin(pAngle) * particleRadius
                );
                DisplayBuilder.dustParticles(pLoc, 4, 0.3, 74, 0, 0, 1.2f);
            }

            // Warning sound as cage tightens
            if (ticksAlive % 20 == 0 && shrinkFactor < 0.6f) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.3f + shrinkFactor);
            }

            // Scraping sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionCage(plugin); }
    }

    // ================================================================
    // 68. DECAYING PLATFORM — 12 blocks flat platform, blocks fall
    //     one by one in a random sequence leaving gaps
    // ================================================================
    public static class DecayingPlatform extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tiles = new ArrayList<>();
        private final List<double[]> tileOffsets = new ArrayList<>();
        private final List<Float> tileY = new ArrayList<>();
        private final List<Integer> dropOrder = new ArrayList<>();
        private final List<Boolean> dropping = new ArrayList<>();
        private final List<Boolean> impactDone = new ArrayList<>();
        private static final int TILE_COUNT = 12;
        private static final float PLATFORM_Y = 3.5f;

        public DecayingPlatform(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("decaying_platform", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4x3 grid platform
            int idx = 0;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 4; col++) {
                    if (idx >= TILE_COUNT) break;
                    double x = (col - 1.5) * 1.2;
                    double z = (row - 1.0) * 1.2;
                    Location loc = center.clone().add(x, PLATFORM_Y, z);
                    Material mat = corruptionMaterial(idx);
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(loc, mat);
                    tile.scale(1.1f, 0.25f, 1.1f)
                        .glow(10, 0, 48)
                        .interpolation(2, 0);
                    tiles.add(tile);
                    spawnedEntities.add(tile.entity());
                    tileOffsets.add(new double[]{x, z});
                    tileY.add(PLATFORM_Y);
                    dropping.add(false);
                    impactDone.add(false);
                    idx++;
                }
            }

            // Create random drop order
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < TILE_COUNT; i++) indices.add(i);
            java.util.Collections.shuffle(indices);
            dropOrder.addAll(indices);

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drop one tile every 10 ticks after delay
            if (ticksAlive > 50) {
                int dropIdx = (ticksAlive - 50) / 10;
                if (dropIdx < dropOrder.size()) {
                    int tileIdx = dropOrder.get(dropIdx);
                    if (!dropping.get(tileIdx) && (ticksAlive - 50) % 10 == 0) {
                        dropping.set(tileIdx, true);
                        double[] off = tileOffsets.get(tileIdx);
                        DisplayBuilder.playSound(
                            c.clone().add(off[0], PLATFORM_Y, off[1]),
                            Sound.BLOCK_SCULK_BREAK, 0.6f, 0.8f
                        );
                        // Warning flash
                        tiles.get(tileIdx).glow(74, 0, 0);
                    }
                }
            }

            // Animate dropping tiles
            for (int i = 0; i < tiles.size(); i++) {
                if (!dropping.get(i)) continue;

                float y = tileY.get(i) - 0.18f;
                tileY.set(i, y);
                double[] off = tileOffsets.get(i);
                tiles.get(i).entity().teleport(c.clone().add(off[0], y, off[1]));

                // Wobble
                float wobble = (float)(Math.sin(y * 3) * 0.1);
                tiles.get(i).rotate(wobble, 1, 0, 0);

                if (y <= 0 && !impactDone.get(i)) {
                    impactDone.set(i, true);
                    Location impactLoc = c.clone().add(off[0], 0, off[1]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 5, 0.3, 10, 0, 48, 1.0f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.7f);
                }
            }

            // Ambient void sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DecayingPlatform(plugin); }
    }

    // ================================================================
    // 69. SHADOW SCAFFOLD — 14 blocks scaffolding structure, collapses
    //     in sequence from top-left to bottom-right like dominoes
    // ================================================================
    public static class ShadowScaffold extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> beams = new ArrayList<>();
        private final List<double[]> beamOffsets = new ArrayList<>();
        private final List<Float> beamY = new ArrayList<>();
        private final List<Boolean> collapsing = new ArrayList<>();
        private final List<Float> collapseAngle = new ArrayList<>();
        private static final int BEAM_COUNT = 14;

        public ShadowScaffold(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_scaffold", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(380);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Scaffolding: 3 columns of vertical beams + horizontal crossbars
            double[][] layout = {
                // Left column (vertical beams)
                {-2, 0, 0}, {-2, 1.5, 0}, {-2, 3.0, 0}, {-2, 4.5, 0},
                // Center column
                {0, 0, 0}, {0, 1.5, 0}, {0, 3.0, 0}, {0, 4.5, 0},
                // Right column
                {2, 0, 0}, {2, 1.5, 0}, {2, 3.0, 0},
                // Horizontal crossbars
                {-1, 4.5, 0}, {1, 4.5, 0}, {-1, 3.0, 0}
            };

            for (int i = 0; i < BEAM_COUNT && i < layout.length; i++) {
                Location loc = center.clone().add(layout[i][0], layout[i][1], layout[i][2]);
                Material mat = (i < 11) ? Material.COAL_BLOCK : Material.NETHERRACK;
                BlockDisplayHandle beam = displayBuilder.spawnBlock(loc, mat);

                if (i >= 11) {
                    // Horizontal crossbar
                    beam.scale(1.8f, 0.3f, 0.5f)
                        .glow(45, 0, 64);
                } else {
                    // Vertical beam
                    beam.scale(0.4f, 1.4f, 0.4f)
                        .glow(74, 0, 0);
                }
                beam.interpolation(3, 0);

                beams.add(beam);
                spawnedEntities.add(beam.entity());
                beamOffsets.add(layout[i].clone());
                beamY.add((float) layout[i][1]);
                collapsing.add(false);
                collapseAngle.add(0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sequential collapse: top-right to bottom-left
            // Collapse order: highest first, then by x position
            int collapseIdx = (ticksAlive - 60) / 8;
            int[] collapseSequence = {3, 7, 12, 11, 13, 2, 6, 10, 1, 5, 9, 0, 4, 8};

            if (ticksAlive > 60 && collapseIdx >= 0 && collapseIdx < collapseSequence.length) {
                int beamIdx = collapseSequence[collapseIdx];
                if (!collapsing.get(beamIdx) && (ticksAlive - 60) % 8 == 0) {
                    collapsing.set(beamIdx, true);
                    double[] off = beamOffsets.get(beamIdx);
                    DisplayBuilder.playSound(
                        c.clone().add(off[0], off[1], off[2]),
                        Sound.BLOCK_SCULK_BREAK, 0.6f, 0.5f + (float)(off[1] * 0.1)
                    );
                }
            }

            // Animate collapsing beams
            for (int i = 0; i < beams.size(); i++) {
                if (!collapsing.get(i)) continue;

                float angle = collapseAngle.get(i) + 0.04f;
                collapseAngle.set(i, angle);

                float y = beamY.get(i) - 0.15f;
                beamY.set(i, y);

                double[] off = beamOffsets.get(i);
                beams.get(i).entity().teleport(c.clone().add(off[0], y, off[2]));
                beams.get(i).rotate(angle, 0, 0, 1);

                if (y <= -0.5f && y > -1.0f) {
                    Location impactLoc = c.clone().add(off[0], 0, off[2]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 4, 0.3, 45, 0, 64, 1.0f);
                }
            }

            // Shadow wisps around scaffold
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 6, 2.0, 10, 0, 48, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowScaffold(plugin); }
    }

    // ================================================================
    // 70. CORRUPTION THRONE — 16 blocks throne shape with dark particle
    //     aura, armrests extend to grab at players, backrest looms
    // ================================================================
    public static class CorruptionThrone extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> throneBlocks = new ArrayList<>();
        private final List<double[]> blockOffsets = new ArrayList<>();
        private float auraIntensity = 0;
        private float armExtend = 0;
        private static final int BLOCK_COUNT = 16;

        public CorruptionThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_throne", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(420);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Throne structure
            double[][] layout = {
                // Seat (4 blocks)
                {-0.5, 0.8, 0}, {0.5, 0.8, 0}, {-0.5, 0.8, 0.8}, {0.5, 0.8, 0.8},
                // Base/legs (4 blocks)
                {-0.8, 0, -0.2}, {0.8, 0, -0.2}, {-0.8, 0, 1.0}, {0.8, 0, 1.0},
                // Backrest (4 blocks tall)
                {-0.5, 1.6, -0.2}, {0.5, 1.6, -0.2}, {-0.5, 2.6, -0.2}, {0.5, 2.6, -0.2},
                // Left armrest (2 blocks)
                {-1.2, 1.2, 0.3}, {-1.2, 1.2, 0.8},
                // Right armrest (2 blocks)
                {1.2, 1.2, 0.3}, {1.2, 1.2, 0.8}
            };

            Material[] mats = {
                Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN,
                Material.DEEPSLATE, Material.DEEPSLATE, Material.DEEPSLATE, Material.DEEPSLATE,
                Material.BLACKSTONE, Material.BLACKSTONE, Material.CRYING_OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.SCULK, Material.SCULK, Material.SCULK, Material.SCULK
            };

            for (int i = 0; i < BLOCK_COUNT && i < layout.length; i++) {
                Location loc = center.clone().add(layout[i][0], layout[i][1], layout[i][2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i]);

                if (i >= 8 && i < 12) {
                    // Backrest - taller
                    block.scale(1.0f, 1.0f, 0.4f);
                } else if (i >= 12) {
                    // Armrests
                    block.scale(0.4f, 0.4f, 0.6f);
                } else if (i < 4) {
                    // Seat
                    block.scale(1.0f, 0.3f, 0.8f);
                } else {
                    // Legs
                    block.scale(0.4f, 0.8f, 0.4f);
                }

                block.glow(45, 0, 64)
                     .interpolation(4, 0);

                throneBlocks.add(block);
                spawnedEntities.add(block.entity());
                blockOffsets.add(layout[i].clone());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Build aura intensity over time
            auraIntensity = Math.min(1.0f, ticksAlive * 0.005f);

            // Armrests slowly extend outward (menacing grab)
            if (ticksAlive > 60 && armExtend < 1.5f) {
                armExtend += 0.01f;

                // Left armrests (indices 12, 13)
                for (int i = 12; i <= 13; i++) {
                    double[] off = blockOffsets.get(i);
                    double newX = off[0] - armExtend * 0.5;
                    throneBlocks.get(i).entity().teleport(c.clone().add(newX, off[1], off[2]));
                }
                // Right armrests (indices 14, 15)
                for (int i = 14; i <= 15; i++) {
                    double[] off = blockOffsets.get(i);
                    double newX = off[0] + armExtend * 0.5;
                    throneBlocks.get(i).entity().teleport(c.clone().add(newX, off[1], off[2]));
                }
            }

            // Backrest looms taller
            if (ticksAlive > 80) {
                float loom = Math.min(1.0f, (ticksAlive - 80) * 0.005f);
                for (int i = 10; i <= 11; i++) {
                    double[] off = blockOffsets.get(i);
                    throneBlocks.get(i).entity().teleport(c.clone().add(off[0], off[1] + loom * 2, off[2]));
                    throneBlocks.get(i).scale(1.0f, 1.0f + loom, 0.4f);
                }
            }

            // Dark aura particles around throne
            if (ticksAlive % 4 == 0) {
                int particleCount = (int)(6 * auraIntensity) + 2;
                for (int p = 0; p < particleCount; p++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = 1.5 + Math.random() * 2.0;
                    double py = Math.random() * 3.5;
                    Location pLoc = c.clone().add(Math.cos(angle) * dist, py, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.1, 45, 0, 64, 1.0f + auraIntensity);
                }
            }

            // Crimson wisps from seat
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.2, 0.4), 4, 0.5, 74, 0, 0, 1.5f);
            }

            // Heartbeat grows stronger
            if (ticksAlive % (30 - (int)(auraIntensity * 15)) == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f + auraIntensity * 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionThrone(plugin); }
    }

    // ================================================================
    // 71. DARK ALTAR — 14 blocks altar with floating dark orb above,
    //     orb pulses damage waves in expanding rings
    // ================================================================
    public static class DarkAltar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> altarBlocks = new ArrayList<>();
        private BlockDisplayHandle orbBlock;
        private float orbPulse = 0;
        private int pulseCount = 0;
        private static final int ALTAR_BLOCK_COUNT = 13; // +1 orb = 14

        public DarkAltar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_altar", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Altar base (3x3 slab)
            double[][] baseLayout = {
                {-1, 0, -1}, {0, 0, -1}, {1, 0, -1},
                {-1, 0, 0},  {0, 0, 0},  {1, 0, 0},
                {-1, 0, 1},  {0, 0, 1},  {1, 0, 1}
            };
            for (int i = 0; i < 9; i++) {
                Location loc = center.clone().add(baseLayout[i][0], baseLayout[i][1], baseLayout[i][2]);
                Material mat = (i == 4) ? Material.CRYING_OBSIDIAN : Material.DEEPSLATE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(1.0f, 0.5f, 1.0f)
                     .glow(10, 0, 48)
                     .interpolation(2, 0);
                altarBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Corner pillars (4 blocks)
            double[][] pillarLayout = {{-1.2, 0.5, -1.2}, {1.2, 0.5, -1.2}, {-1.2, 0.5, 1.2}, {1.2, 0.5, 1.2}};
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(pillarLayout[i][0], pillarLayout[i][1], pillarLayout[i][2]);
                BlockDisplayHandle pillar = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                pillar.scale(0.4f, 1.5f, 0.4f)
                      .glow(74, 0, 0)
                      .interpolation(2, 0);
                altarBlocks.add(pillar);
                spawnedEntities.add(pillar.entity());
            }

            // Floating dark orb
            Location orbLoc = center.clone().add(0, 3.5, 0);
            orbBlock = displayBuilder.spawnBlock(orbLoc, Material.SCULK);
            orbBlock.scale(0.8f, 0.8f, 0.8f)
                    .glow(45, 0, 64)
                    .interpolation(5, 0);
            spawnedEntities.add(orbBlock.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Orb hover animation
            float orbY = 3.5f + (float) Math.sin(ticksAlive * 0.05) * 0.5f;
            orbBlock.entity().teleport(c.clone().add(0, orbY, 0));

            // Orb scale pulse
            orbPulse = 0.8f + (float) Math.sin(ticksAlive * 0.1) * 0.2f;
            orbBlock.scale(orbPulse, orbPulse, orbPulse);

            // Orb spin
            float spin = ticksAlive * 0.03f;
            orbBlock.rotate(spin, 0, 1, 0);

            // Dark energy beam between altar center and orb
            if (ticksAlive % 5 == 0) {
                Location altarTop = c.clone().add(0, 0.5, 0);
                Location orbPos = c.clone().add(0, orbY, 0);
                DisplayBuilder.particleLine(altarTop, orbPos, Particle.DUST, 3,
                        new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.2f));
            }

            // Damage pulse wave every 40 ticks
            if (ticksAlive > 60 && ticksAlive % 40 == 0) {
                pulseCount++;
                float pulseRadius = 2.0f + pulseCount * 0.5f;
                DisplayBuilder.particleRing(c.clone().add(0, 1, 0), pulseRadius, Particle.DUST, 24,
                        new Particle.DustOptions(Color.fromRGB(74, 0, 0), 1.8f));
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.8f, 0.3f + pulseCount * 0.05f);
            }

            // Pillar glow flicker
            if (ticksAlive % 20 == 0) {
                for (int i = 9; i < altarBlocks.size(); i++) {
                    if (ticksAlive % 40 < 20) {
                        altarBlocks.get(i).glow(74, 0, 0);
                    } else {
                        altarBlocks.get(i).glow(45, 0, 64);
                    }
                }
            }

            // Ambient particles
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, orbY + 0.5, 0), 6, 0.6, 10, 0, 48, 1.0f);
            }

            // Heartbeat
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkAltar(plugin); }
    }

    // ================================================================
    // 72. CORRUPTED OBELISK RING — 12 blocks standing stones in circle,
    //     lean inward over time converging on center point
    // ================================================================
    public static class CorruptedObeliskRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> obelisks = new ArrayList<>();
        private final List<Double> obeliskAngles = new ArrayList<>();
        private float leanProgress = 0;
        private static final int OBELISK_COUNT = 12;
        private static final float RING_RADIUS = 4.5f;

        public CorruptedObeliskRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corrupted_obelisk_ring", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(380);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < OBELISK_COUNT; i++) {
                double angle = (Math.PI * 2 * i) / OBELISK_COUNT;
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;

                Location loc = center.clone().add(x, 0, z);
                Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN
                             : (i % 3 == 1) ? Material.OBSIDIAN : Material.DEEPSLATE;
                BlockDisplayHandle obelisk = displayBuilder.spawnBlock(loc, mat);

                // Varied heights for visual interest
                float height = 2.5f + (float)(Math.sin(i * 1.2) * 0.8);
                obelisk.scale(0.6f, height, 0.6f)
                       .glow(45, 0, 64)
                       .interpolation(5, 0);

                obelisks.add(obelisk);
                spawnedEntities.add(obelisk.entity());
                obeliskAngles.add(angle);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly lean inward
            if (ticksAlive > 50 && leanProgress < 0.6f) {
                leanProgress += 0.002f;
            }

            for (int i = 0; i < obelisks.size(); i++) {
                double angle = obeliskAngles.get(i);
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;

                // Lean toward center
                float leanAngle = leanProgress;
                // Lean axis is perpendicular to the radius direction (tangent)
                float leanAxisX = (float) -Math.sin(angle);
                float leanAxisZ = (float) Math.cos(angle);

                obelisks.get(i).rotate(leanAngle, leanAxisX, 0, leanAxisZ);

                // Shift base slightly inward as they lean
                double inwardShift = leanProgress * 0.8;
                double newX = Math.cos(angle) * (RING_RADIUS - inwardShift);
                double newZ = Math.sin(angle) * (RING_RADIUS - inwardShift);
                obelisks.get(i).entity().teleport(c.clone().add(newX, 0, newZ));
            }

            // Energy lines between obelisks
            if (ticksAlive % 8 == 0 && ticksAlive > 40) {
                for (int i = 0; i < OBELISK_COUNT; i++) {
                    int next = (i + 1) % OBELISK_COUNT;
                    double a1 = obeliskAngles.get(i);
                    double a2 = obeliskAngles.get(next);
                    Location p1 = c.clone().add(Math.cos(a1) * RING_RADIUS, 1.5, Math.sin(a1) * RING_RADIUS);
                    Location p2 = c.clone().add(Math.cos(a2) * RING_RADIUS, 1.5, Math.sin(a2) * RING_RADIUS);
                    DisplayBuilder.particleLine(p1, p2, Particle.DUST, 2,
                            new Particle.DustOptions(Color.fromRGB(74, 0, 0), 0.8f));
                }
            }

            // Central corruption pillar of particles
            if (ticksAlive % 6 == 0) {
                float pillarY = Math.min(5.0f, leanProgress * 8);
                DisplayBuilder.dustParticles(c.clone().add(0, pillarY, 0), 8, 0.3, 10, 0, 48, 1.5f);
            }

            // Warden heartbeat intensifies as obelisks lean
            if (ticksAlive % (int)(30 - leanProgress * 30) == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f + leanProgress, 0.4f);
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptedObeliskRing(plugin); }
    }

    // ================================================================
    // 73. DECAY DOME — 18 blocks dome, cracks appear and pieces fall
    //     inward raining debris on anyone inside
    // ================================================================
    public static class DecayDome extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> domeBlocks = new ArrayList<>();
        private final List<double[]> domeOffsets = new ArrayList<>();
        private final List<Float> domeY = new ArrayList<>();
        private final List<Boolean> cracked = new ArrayList<>();
        private final List<Boolean> impactDone = new ArrayList<>();
        private static final int DOME_COUNT = 18;
        private static final float DOME_RADIUS = 4.0f;

        public DecayDome(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("decay_dome", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(420);
            config.setCooldownTicks(340);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Dome using spherical coordinates (upper hemisphere)
            int idx = 0;
            // Bottom ring (8 blocks)
            for (int i = 0; i < 8 && idx < DOME_COUNT; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double x = Math.cos(angle) * DOME_RADIUS;
                double z = Math.sin(angle) * DOME_RADIUS;
                double y = 1.0;
                addDomeBlock(center, x, y, z, idx++);
            }
            // Middle ring (6 blocks, higher and tighter)
            for (int i = 0; i < 6 && idx < DOME_COUNT; i++) {
                double angle = (Math.PI * 2 * i) / 6 + 0.3;
                double x = Math.cos(angle) * (DOME_RADIUS * 0.7);
                double z = Math.sin(angle) * (DOME_RADIUS * 0.7);
                double y = 3.0;
                addDomeBlock(center, x, y, z, idx++);
            }
            // Top cap (4 blocks)
            for (int i = 0; i < 4 && idx < DOME_COUNT; i++) {
                double angle = (Math.PI * 2 * i) / 4 + 0.6;
                double x = Math.cos(angle) * (DOME_RADIUS * 0.3);
                double z = Math.sin(angle) * (DOME_RADIUS * 0.3);
                double y = 4.5;
                addDomeBlock(center, x, y, z, idx++);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.4f);
        }

        private void addDomeBlock(Location center, double x, double y, double z, int idx) {
            Location loc = center.clone().add(x, y, z);
            Material mat = corruptionMaterial(idx);
            BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
            block.scale(1.2f, 1.0f, 1.2f)
                 .glow(10, 0, 48)
                 .interpolation(3, 0);
            domeBlocks.add(block);
            spawnedEntities.add(block.entity());
            domeOffsets.add(new double[]{x, y, z});
            domeY.add((float) y);
            cracked.add(false);
            impactDone.add(false);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Crack blocks starting from top, working down
            // Top cap first, then middle, then bottom
            int crackIdx = (ticksAlive - 70) / 10;
            int[] crackSequence = {14, 15, 16, 17, 8, 9, 10, 11, 12, 13, 0, 1, 2, 3, 4, 5, 6, 7};

            if (ticksAlive > 70 && crackIdx >= 0 && crackIdx < crackSequence.length) {
                int blockIdx = crackSequence[crackIdx];
                if (blockIdx < domeBlocks.size() && !cracked.get(blockIdx) && (ticksAlive - 70) % 10 == 0) {
                    cracked.set(blockIdx, true);
                    double[] off = domeOffsets.get(blockIdx);
                    DisplayBuilder.playSound(
                        c.clone().add(off[0], off[1], off[2]),
                        Sound.BLOCK_SCULK_BREAK, 0.6f, 0.7f
                    );
                    // Flash crimson on crack
                    domeBlocks.get(blockIdx).glow(74, 0, 0);
                }
            }

            // Cracked blocks fall inward toward center
            for (int i = 0; i < domeBlocks.size(); i++) {
                if (!cracked.get(i)) continue;

                float y = domeY.get(i) - 0.2f;
                domeY.set(i, y);

                double[] off = domeOffsets.get(i);
                // Fall inward (toward center x=0, z=0)
                double inwardFactor = 0.02;
                double newX = off[0] * (1.0 - inwardFactor * (domeOffsets.get(i)[1] - y));
                double newZ = off[2] * (1.0 - inwardFactor * (domeOffsets.get(i)[1] - y));

                domeBlocks.get(i).entity().teleport(c.clone().add(newX, y, newZ));
                domeBlocks.get(i).rotate((float)(Math.random() * 0.2), 1, 0, 1);

                if (y <= 0 && !impactDone.get(i)) {
                    impactDone.set(i, true);
                    Location impactLoc = c.clone().add(newX, 0, newZ);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 8, 0.5, 45, 0, 64, 1.2f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.6f);
                }
            }

            // Interior darkness particles
            if (ticksAlive % 6 == 0) {
                Location innerLoc = c.clone().add(
                    (Math.random() - 0.5) * 3, Math.random() * 3, (Math.random() - 0.5) * 3
                );
                DisplayBuilder.dustParticles(innerLoc, 3, 0.2, 10, 0, 48, 1.5f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DecayDome(plugin); }
    }

    // ================================================================
    // 74. SHADOW BARRICADE — 15 blocks low wall grows taller then
    //     crumbles, blocks topple forward onto players
    // ================================================================
    public static class ShadowBarricade extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<double[]> wallOffsets = new ArrayList<>();
        private final List<Float> blockY = new ArrayList<>();
        private final List<Boolean> toppling = new ArrayList<>();
        private final List<Boolean> impactDone = new ArrayList<>();
        private float growthProgress = 0;
        private boolean crumbling = false;
        private static final int BLOCK_COUNT = 15;

        public ShadowBarricade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_barricade", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(380);
            config.setCooldownTicks(310);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(42.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 wide x 3 tall wall (grows from ground)
            int idx = 0;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 5; col++) {
                    if (idx >= BLOCK_COUNT) break;
                    double x = (col - 2.0) * 1.2;
                    double y = -1.0; // Start below ground
                    double z = 0;
                    Location loc = center.clone().add(x, y, z);
                    Material mat = (row == 2) ? Material.SOUL_SOIL : corruptionMaterial(idx);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                    block.scale(1.1f, 1.0f, 0.8f)
                         .glow(45, 0, 64)
                         .interpolation(4, 0);
                    wallBlocks.add(block);
                    spawnedEntities.add(block.entity());
                    wallOffsets.add(new double[]{x, row * 1.1, z}); // Target y positions
                    blockY.add(-1.0f);
                    toppling.add(false);
                    impactDone.add(false);
                    idx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Wall rises from ground (ticks 0-120)
            if (ticksAlive <= 120 && growthProgress < 1.0f) {
                growthProgress = Math.min(1.0f, ticksAlive / 120.0f);

                for (int i = 0; i < wallBlocks.size(); i++) {
                    double[] off = wallOffsets.get(i);
                    float targetY = (float) off[1];
                    float currentY = -1.0f + (targetY + 1.0f) * growthProgress;
                    blockY.set(i, currentY);
                    wallBlocks.get(i).entity().teleport(c.clone().add(off[0], currentY, off[2]));
                }

                // Rising rumble
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.3f + growthProgress * 0.3f);
                }
            }

            // Phase 2: Wall stands menacingly (ticks 120-200)
            if (ticksAlive > 120 && ticksAlive < 200) {
                // Ominous particles along top
                if (ticksAlive % 6 == 0) {
                    double rx = (Math.random() - 0.5) * 5.0;
                    DisplayBuilder.dustParticles(c.clone().add(rx, 3.5, 0), 4, 0.3, 74, 0, 0, 1.3f);
                }
            }

            // Phase 3: Crumble forward (ticks 200+)
            if (ticksAlive >= 200) {
                crumbling = true;

                // Topple blocks from top row first
                int toppleIdx = (ticksAlive - 200) / 6;
                // Top row first (10-14), then middle (5-9), then bottom (0-4)
                int[] toppleOrder = {12, 10, 14, 11, 13, 7, 5, 9, 6, 8, 2, 0, 4, 1, 3};

                if (toppleIdx < toppleOrder.length) {
                    int blockIdx = toppleOrder[toppleIdx];
                    if (blockIdx < wallBlocks.size() && !toppling.get(blockIdx) && (ticksAlive - 200) % 6 == 0) {
                        toppling.set(blockIdx, true);
                        DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.5f, 0.6f);
                    }
                }
            }

            // Animate toppling blocks (fall forward on Z axis)
            for (int i = 0; i < wallBlocks.size(); i++) {
                if (!toppling.get(i)) continue;

                float y = blockY.get(i) - 0.15f;
                blockY.set(i, y);
                double[] off = wallOffsets.get(i);
                // Fall forward (positive Z)
                float forwardDist = ((float) off[1] - y) * 0.3f;
                wallBlocks.get(i).entity().teleport(c.clone().add(off[0], y, off[2] + forwardDist));

                float tiltAngle = Math.min(1.5f, ((float) off[1] - y) * 0.2f);
                wallBlocks.get(i).rotate(tiltAngle, 1, 0, 0);

                if (y <= -0.5f && !impactDone.get(i)) {
                    impactDone.set(i, true);
                    Location impactLoc = c.clone().add(off[0], 0, off[2] + forwardDist);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 6, 0.4, 45, 0, 64, 1.0f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowBarricade(plugin); }
    }

    // ================================================================
    // 75. CORRUPTION PYRAMID — 16 blocks pyramid, apex block rises
    //     and hovers, pulling other blocks upward in its wake
    // ================================================================
    public static class CorruptionPyramid extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pyramidBlocks = new ArrayList<>();
        private final List<double[]> blockOffsets = new ArrayList<>();
        private final List<Float> blockY = new ArrayList<>();
        private final List<Boolean> ascending = new ArrayList<>();
        private BlockDisplayHandle apexBlock;
        private float apexY;
        private boolean apexRising = false;
        private static final int BASE_COUNT = 15; // + 1 apex = 16

        public CorruptionPyramid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_pyramid", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Bottom layer: 3x3 = 9 blocks
            int idx = 0;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    double x = (col - 1.0) * 1.1;
                    double z = (row - 1.0) * 1.1;
                    addPyramidBlock(center, x, 0, z, idx++);
                }
            }
            // Middle layer: 2x2 = 4 blocks
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    double x = (col - 0.5) * 1.1;
                    double z = (row - 0.5) * 1.1;
                    addPyramidBlock(center, x, 1.1, z, idx++);
                }
            }
            // Upper layer: 2 blocks
            addPyramidBlock(center, -0.3, 2.2, 0, idx++);
            addPyramidBlock(center, 0.3, 2.2, 0, idx++);

            // Apex block
            Location apexLoc = center.clone().add(0, 3.3, 0);
            apexBlock = displayBuilder.spawnBlock(apexLoc, Material.CRYING_OBSIDIAN);
            apexBlock.scale(0.7f, 0.7f, 0.7f)
                     .glow(74, 0, 0)
                     .interpolation(5, 0);
            spawnedEntities.add(apexBlock.entity());
            apexY = 3.3f;

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);
        }

        private void addPyramidBlock(Location center, double x, double y, double z, int idx) {
            Location loc = center.clone().add(x, y, z);
            Material mat = corruptionMaterial(idx);
            BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
            block.scale(1.0f, 1.0f, 1.0f)
                 .glow(45, 0, 64)
                 .interpolation(3, 0);
            pyramidBlocks.add(block);
            spawnedEntities.add(block.entity());
            blockOffsets.add(new double[]{x, y, z});
            blockY.add((float) y);
            ascending.add(false);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Phase 1: Pyramid sits ominously (0-80 ticks)
            if (ticksAlive < 80) {
                // Pulsing dark particles around base
                if (ticksAlive % 8 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    Location pLoc = c.clone().add(Math.cos(angle) * 2, 0.5, Math.sin(angle) * 2);
                    DisplayBuilder.dustParticles(pLoc, 4, 0.3, 10, 0, 48, 1.0f);
                }
            }

            // Phase 2: Apex begins rising (80+)
            if (ticksAlive >= 80) {
                apexRising = true;
                apexY += 0.06f;
                apexBlock.entity().teleport(c.clone().add(0, apexY, 0));

                // Apex spins as it rises
                float spin = (ticksAlive - 80) * 0.04f;
                apexBlock.rotate(spin, 0, 1, 0);

                // Energy beam from pyramid to apex
                if (ticksAlive % 4 == 0) {
                    Location pyramidTop = c.clone().add(0, 3.3, 0);
                    Location apexPos = c.clone().add(0, apexY, 0);
                    DisplayBuilder.particleLine(pyramidTop, apexPos, Particle.DUST, 3,
                            new Particle.DustOptions(Color.fromRGB(74, 0, 0), 1.5f));
                }

                // Pull upper blocks upward after apex (staggered)
                if (ticksAlive > 120) {
                    // Upper blocks first (indices 13, 14), then middle (9-12), then base
                    int pullWave = (ticksAlive - 120) / 20;

                    for (int i = pyramidBlocks.size() - 1; i >= 0; i--) {
                        int blockLayer = (i < 9) ? 0 : (i < 13) ? 1 : 2;
                        if (blockLayer <= pullWave && !ascending.get(i)) {
                            ascending.set(i, true);
                        }
                    }
                }
            }

            // Animate ascending blocks
            for (int i = 0; i < pyramidBlocks.size(); i++) {
                if (!ascending.get(i)) continue;

                float y = blockY.get(i) + 0.03f;
                blockY.set(i, y);
                double[] off = blockOffsets.get(i);

                // Slight outward drift as they rise
                double driftX = off[0] * (1.0 + (y - off[1]) * 0.02);
                double driftZ = off[2] * (1.0 + (y - off[1]) * 0.02);

                pyramidBlocks.get(i).entity().teleport(c.clone().add(driftX, y, driftZ));

                // Gentle rotation
                float rot = (y - (float) off[1]) * 0.05f;
                pyramidBlocks.get(i).rotate(rot, 0, 1, 0);

                // Trailing particles
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(
                        c.clone().add(driftX, y - 0.5, driftZ),
                        2, 0.1, 45, 0, 64, 0.8f
                    );
                }
            }

            // Apex glow pulses
            if (apexRising && ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(
                    c.clone().add(0, apexY, 0), 8, 0.5, 74, 0, 0, 1.5f
                );
            }

            // Heartbeat intensifies
            if (ticksAlive % 25 == 0) {
                float intensity = apexRising ? 1.0f : 0.5f;
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, intensity, 0.4f);
            }

            // Sculk ambient
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionPyramid(plugin); }
    }
}
