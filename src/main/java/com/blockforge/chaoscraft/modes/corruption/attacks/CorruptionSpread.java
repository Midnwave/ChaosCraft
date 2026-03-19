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
 * Corruption Mode — CORRUPTION PILLARS & SPIRES
 * 15 corruption-themed BlockDisplay attacks featuring dark monoliths,
 * twisted spires, and decaying structures that erupt from the ground.
 *
 * Color palette:
 * - Dark purple:      RGB(45, 0, 64)
 * - Deep crimson:     RGB(74, 0, 0)
 * - Sickly green:     RGB(26, 58, 0)
 * - Void blue:        RGB(10, 0, 48)
 * - Corruption black: RGB(20, 10, 20)
 *
 * Materials: SCULK, DEEPSLATE, BLACKSTONE, CRYING_OBSIDIAN, OBSIDIAN,
 *            COAL_BLOCK, NETHERRACK, SOUL_SOIL, TINTED_GLASS
 */
public final class CorruptionSpread {

    private CorruptionSpread() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CorruptionPillar(plugin));
        registry.register(new DarkObelisk(plugin));
        registry.register(new TwistedSpire(plugin));
        registry.register(new CorruptionNeedle(plugin));
        registry.register(new VoidSpikeCluster(plugin));
        registry.register(new PulsatingTower(plugin));
        registry.register(new DecayingColumn(plugin));
        registry.register(new ShadowMonolith(plugin));
        registry.register(new CorruptionStalagmite(plugin));
        registry.register(new WarpedGrowth(plugin));
        registry.register(new DarkCrystalFormation(plugin));
        registry.register(new CorruptionAntenna(plugin));
        registry.register(new RottingTreeStump(plugin));
        registry.register(new VoidBeacon(plugin));
        registry.register(new CorruptionFountain(plugin));
    }

    // ================================================================
    // 1. CORRUPTION PILLAR — 12 sculk blocks spiral upward 15 blocks
    //    with slow rotation, trailing dark dust particles
    // ================================================================
    public static class CorruptionPillar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Float> targetYPositions = new ArrayList<>();
        private final List<Float> currentYPositions = new ArrayList<>();
        private int spawned = 0;
        private static final int BLOCK_COUNT = 12;
        private static final float MAX_HEIGHT = 15.0f;

        public CorruptionPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_pillar", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                targetYPositions.add((MAX_HEIGHT / BLOCK_COUNT) * (i + 1));
                currentYPositions.add(0.0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.4f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Stagger block spawning — one every 3 ticks
            if (spawned < BLOCK_COUNT && ticksAlive % 3 == 0) {
                double spiralAngle = (2 * Math.PI * spawned) / BLOCK_COUNT;
                double radius = 0.4;
                double ox = Math.cos(spiralAngle) * radius;
                double oz = Math.sin(spiralAngle) * radius;

                Location spawnLoc = c.clone().add(ox, 0, oz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, Material.SCULK);
                handle.scale(0.8f, 1.2f, 0.8f)
                      .glow(45, 0, 64)
                      .interpolation(8, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);

                DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.7f + (float)(Math.random() * 0.3));
                spawned++;
            }

            // Animate blocks rising to their target heights
            for (int i = 0; i < blocks.size(); i++) {
                float target = targetYPositions.get(i);
                float current = currentYPositions.get(i);
                if (current < target) {
                    float newY = Math.min(current + 0.35f, target);
                    currentYPositions.set(i, newY);

                    BlockDisplayHandle handle = blocks.get(i);
                    double spiralAngle = (2 * Math.PI * i) / BLOCK_COUNT;
                    double radius = 0.4;
                    double ox = Math.cos(spiralAngle) * radius;
                    double oz = Math.sin(spiralAngle) * radius;

                    Location newLoc = c.clone().add(ox, newY, oz);
                    handle.entity().teleport(newLoc);

                    // Apply slow rotation as it rises
                    float rotAngle = (float)(ticksAlive * 0.02 + i * 0.5);
                    handle.animateTo(
                        new Vector3f(-0.4f, -0.6f, -0.4f),
                        new AxisAngle4f(rotAngle, 0, 1, 0),
                        new Vector3f(0.8f, 1.2f, 0.8f),
                        8
                    );
                }
            }

            // Dark dust trailing particles
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < blocks.size(); i++) {
                    double spiralAngle = (2 * Math.PI * i) / BLOCK_COUNT;
                    double ox = Math.cos(spiralAngle) * 0.4;
                    double oz = Math.sin(spiralAngle) * 0.4;
                    Location pLoc = c.clone().add(ox, currentYPositions.get(i), oz);
                    DisplayBuilder.dustParticles(pLoc, 3, 0.3, 45, 0, 64, 1.2f);
                }
            }

            // Ambient sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionPillar(plugin); }
    }

    // ================================================================
    // 2. DARK OBELISK — 16-block rectangular monolith with pulsing
    //    glow (scale oscillation) and deep hum sound
    // ================================================================
    public static class DarkObelisk extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 16;
        private static final float HEIGHT = 12.0f;
        private boolean built = false;

        public DarkObelisk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_obelisk", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build 4-wide, 4-tall rectangular monolith (2x1 columns, 4 blocks each = 16)
            Material[] mats = { Material.DEEPSLATE, Material.BLACKSTONE, Material.DEEPSLATE, Material.OBSIDIAN };
            int idx = 0;
            for (int col = 0; col < 4; col++) {
                double ox = (col < 2) ? -0.5 : 0.5;
                double oz = (col % 2 == 0) ? -0.5 : 0.5;
                for (int row = 0; row < 4; row++) {
                    Location loc = center.clone().add(ox, row * 1.0, oz);
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mats[idx % mats.length]);
                    handle.scale(1.0f, 1.0f, 1.0f)
                          .glow(20, 10, 20)
                          .interpolation(10, 0);
                    spawnedEntities.add(handle.entity());
                    blocks.add(handle);
                    idx++;
                }
            }
            built = true;

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || !built) return;

            // Pulsing scale oscillation — breathe effect
            float pulse = 1.0f + 0.12f * (float) Math.sin(ticksAlive * 0.08);
            float yPulse = 1.0f + 0.06f * (float) Math.sin(ticksAlive * 0.08 + 0.5);

            for (BlockDisplayHandle handle : blocks) {
                handle.animateTo(
                    new Vector3f(-pulse / 2, -0.5f, -pulse / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(pulse, yPulse, pulse),
                    10
                );
            }

            // Dark purple ambient particles around the obelisk
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = 1.0 + Math.random() * 1.5;
                    Location pLoc = c.clone().add(
                        Math.cos(angle) * dist,
                        Math.random() * HEIGHT,
                        Math.sin(angle) * dist
                    );
                    DisplayBuilder.dustParticles(pLoc, 2, 0.2, 45, 0, 64, 1.5f);
                }
            }

            // Deep heartbeat hum every 30 ticks
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.4f);
            }

            // Occasional deep rumble
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkObelisk(plugin); }
    }

    // ================================================================
    // 3. TWISTED SPIRE — 14 blocks helical twist, leans 15 degrees,
    //    creaking sounds as it twists
    // ================================================================
    public static class TwistedSpire extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private int spawned = 0;
        private static final int BLOCK_COUNT = 14;
        private static final float MAX_HEIGHT = 14.0f;
        private static final float LEAN_ANGLE = (float) Math.toRadians(15);

        public TwistedSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("twisted_spire", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(420);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.3f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spawn blocks sequentially every 2 ticks
            if (spawned < BLOCK_COUNT && ticksAlive % 2 == 0) {
                float y = (MAX_HEIGHT / BLOCK_COUNT) * spawned;
                double helixAngle = (2 * Math.PI * spawned) / 7.0; // full helix every 7 blocks
                double helixRadius = 0.6;
                double ox = Math.cos(helixAngle) * helixRadius;
                double oz = Math.sin(helixAngle) * helixRadius;

                // Apply lean offset — lean along X axis
                double leanOffset = Math.sin(LEAN_ANGLE) * y;

                Material mat = (spawned % 3 == 0) ? Material.BLACKSTONE : Material.DEEPSLATE;
                Location loc = c.clone().add(ox + leanOffset * 0.1, y, oz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);

                // Taper scale toward top
                float taper = 1.0f - (spawned * 0.04f);
                handle.scale(taper, 1.1f, taper)
                      .rotate(LEAN_ANGLE, 0, 0, 1)
                      .glow(74, 0, 0)
                      .interpolation(6, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);

                DisplayBuilder.playSound(loc, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.8f + spawned * 0.05f);
                spawned++;
            }

            // Twist animation — slow rotation around Y
            float twistRate = ticksAlive * 0.015f;
            for (int i = 0; i < blocks.size(); i++) {
                float individualTwist = twistRate + (i * 0.3f);
                float taper = 1.0f - (i * 0.04f);
                blocks.get(i).animateTo(
                    new Vector3f(-taper / 2, -0.55f, -taper / 2),
                    new AxisAngle4f(individualTwist, 0, 1, 0),
                    new Vector3f(taper, 1.1f, taper),
                    6
                );
            }

            // Creaking particles
            if (ticksAlive % 6 == 0 && !blocks.isEmpty()) {
                int randIdx = (int)(Math.random() * blocks.size());
                Location pLoc = blocks.get(randIdx).entity().getLocation();
                DisplayBuilder.dustParticles(pLoc, 4, 0.4, 74, 0, 0, 1.0f);
            }

            // Creaking sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TwistedSpire(plugin); }
    }

    // ================================================================
    // 4. CORRUPTION NEEDLE — 18-block ultra-thin needle, shoots up
    //    rapidly 15 blocks, holds at apex, then retracts
    // ================================================================
    public static class CorruptionNeedle extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private int phase = 0; // 0=rising, 1=holding, 2=retracting
        private float currentHeight = 0;
        private int holdTimer = 0;
        private static final int BLOCK_COUNT = 18;
        private static final float APEX_HEIGHT = 15.0f;
        private static final float RISE_SPEED = 0.6f;
        private static final float RETRACT_SPEED = 0.4f;
        private static final int HOLD_DURATION = 60; // 3 seconds

        public CorruptionNeedle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_needle", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn all needle blocks at ground level, stacked tightly
            for (int i = 0; i < BLOCK_COUNT; i++) {
                Material mat = (i % 2 == 0) ? Material.SCULK : Material.COAL_BLOCK;
                Location loc = center.clone().add(0, i * 0.5, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);
                handle.scale(0.3f, 0.5f, 0.3f)
                      .glow(10, 0, 48)
                      .interpolation(3, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            switch (phase) {
                case 0: // Rising
                    currentHeight += RISE_SPEED;
                    if (currentHeight >= APEX_HEIGHT) {
                        currentHeight = APEX_HEIGHT;
                        phase = 1;
                        DisplayBuilder.playSound(c.clone().add(0, APEX_HEIGHT, 0), Sound.BLOCK_SCULK_BREAK, 1.0f, 1.8f);
                    }
                    break;
                case 1: // Holding at apex
                    holdTimer++;
                    if (holdTimer >= HOLD_DURATION) {
                        phase = 2;
                        DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
                    }
                    break;
                case 2: // Retracting
                    currentHeight -= RETRACT_SPEED;
                    if (currentHeight < 0) currentHeight = 0;
                    break;
            }

            // Update all block positions
            for (int i = 0; i < blocks.size(); i++) {
                float blockY = currentHeight + (i * 0.5f);
                // During retraction, collapse blocks downward
                if (phase == 2) {
                    blockY = currentHeight * ((float) i / BLOCK_COUNT);
                }
                Location newLoc = c.clone().add(0, blockY, 0);
                blocks.get(i).entity().teleport(newLoc);

                // Taper toward tip
                float taper = 0.3f - (i * 0.012f);
                if (taper < 0.08f) taper = 0.08f;
                blocks.get(i).animateTo(
                    new Vector3f(-taper / 2, -0.25f, -taper / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(taper, 0.5f, taper),
                    3
                );
            }

            // Dark void particles streaming off the needle
            if (ticksAlive % 3 == 0) {
                Location tip = c.clone().add(0, currentHeight + BLOCK_COUNT * 0.5f, 0);
                DisplayBuilder.dustParticles(tip, 5, 0.15, 10, 0, 48, 1.3f);
                DisplayBuilder.dustParticles(c.clone().add(0, currentHeight, 0), 3, 0.5, 20, 10, 20, 0.8f);
            }

            // Ominous hum while holding
            if (phase == 1 && ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, APEX_HEIGHT / 2, 0), Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionNeedle(plugin); }
    }

    // ================================================================
    // 5. VOID SPIKE CLUSTER — 5 spikes (15 blocks total) burst from
    //    ground at different angles like a corrupted crystal formation
    // ================================================================
    public static class VoidSpikeCluster extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> spikes = new ArrayList<>();
        private final List<Float> spikeAnglesX = new ArrayList<>();
        private final List<Float> spikeAnglesZ = new ArrayList<>();
        private final List<Float> spikeHeights = new ArrayList<>();
        private int spikesSpawned = 0;
        private static final int SPIKE_COUNT = 5;
        private static final int BLOCKS_PER_SPIKE = 3;
        private static final float MAX_SPIKE_HEIGHT = 8.0f;

        public VoidSpikeCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_spike_cluster", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pre-calculate spike angles
            for (int s = 0; s < SPIKE_COUNT; s++) {
                float angleX = (float)((Math.random() - 0.5) * Math.toRadians(40));
                float angleZ = (float)((Math.random() - 0.5) * Math.toRadians(40));
                spikeAnglesX.add(angleX);
                spikeAnglesZ.add(angleZ);
                spikeHeights.add(0.0f);
                spikes.add(new ArrayList<>());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Spawn spikes one at a time, every 8 ticks
            if (spikesSpawned < SPIKE_COUNT && ticksAlive % 8 == 0) {
                double spikeAngle = (2 * Math.PI * spikesSpawned) / SPIKE_COUNT;
                double baseRadius = 1.5;
                double baseX = Math.cos(spikeAngle) * baseRadius;
                double baseZ = Math.sin(spikeAngle) * baseRadius;

                Material[] spikeMats = { Material.OBSIDIAN, Material.DEEPSLATE, Material.BLACKSTONE };
                List<BlockDisplayHandle> spikeBlocks = spikes.get(spikesSpawned);

                for (int b = 0; b < BLOCKS_PER_SPIKE; b++) {
                    Location loc = c.clone().add(baseX, b * 0.8, baseZ);
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, spikeMats[b % spikeMats.length]);

                    float taper = 0.7f - (b * 0.15f);
                    handle.scale(taper, 1.0f, taper)
                          .rotate(spikeAnglesX.get(spikesSpawned), 1, 0, 0)
                          .glow(10, 0, 48)
                          .interpolation(5, 0);
                    spawnedEntities.add(handle.entity());
                    spikeBlocks.add(handle);
                }

                DisplayBuilder.playSound(c.clone().add(baseX, 0, baseZ), Sound.BLOCK_SCULK_BREAK, 0.8f, 0.6f + spikesSpawned * 0.1f);
                spikesSpawned++;
            }

            // Animate spikes rising
            for (int s = 0; s < spikesSpawned; s++) {
                float height = spikeHeights.get(s);
                if (height < MAX_SPIKE_HEIGHT) {
                    height += 0.4f;
                    spikeHeights.set(s, Math.min(height, MAX_SPIKE_HEIGHT));
                }

                double spikeAngle = (2 * Math.PI * s) / SPIKE_COUNT;
                double baseX = Math.cos(spikeAngle) * 1.5;
                double baseZ = Math.sin(spikeAngle) * 1.5;
                List<BlockDisplayHandle> spikeBlocks = spikes.get(s);

                for (int b = 0; b < spikeBlocks.size(); b++) {
                    // Lean spike outward using its angle
                    float leanX = spikeAnglesX.get(s);
                    float leanZ = spikeAnglesZ.get(s);
                    double yPos = height * ((float)(b + 1) / BLOCKS_PER_SPIKE);
                    double xOff = Math.sin(leanX) * yPos * 0.3;
                    double zOff = Math.sin(leanZ) * yPos * 0.3;

                    Location newLoc = c.clone().add(baseX + xOff, yPos, baseZ + zOff);
                    spikeBlocks.get(b).entity().teleport(newLoc);
                }
            }

            // Void particles at spike tips
            if (ticksAlive % 4 == 0) {
                for (int s = 0; s < spikesSpawned; s++) {
                    List<BlockDisplayHandle> spikeBlocks = spikes.get(s);
                    if (!spikeBlocks.isEmpty()) {
                        Location tipLoc = spikeBlocks.get(spikeBlocks.size() - 1).entity().getLocation();
                        DisplayBuilder.dustParticles(tipLoc.clone().add(0, 0.5, 0), 4, 0.2, 10, 0, 48, 1.4f);
                    }
                }
            }

            // Ground burst particles
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.particleRing(c, 2.0, Particle.DUST, 16,
                    new Particle.DustOptions(Color.fromRGB(20, 10, 20), 1.2f));
            }

            // Ambient cave sound
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.7f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSpikeCluster(plugin); }
    }

    // ================================================================
    // 6. PULSATING TOWER — 12 blocks stack up, then expand and
    //    contract rhythmically like a breathing corruption mass
    // ================================================================
    public static class PulsatingTower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private int spawned = 0;
        private boolean fullyBuilt = false;
        private static final int BLOCK_COUNT = 12;

        public PulsatingTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pulsating_tower", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Build up — spawn one block per tick
            if (spawned < BLOCK_COUNT && !fullyBuilt) {
                Material mat;
                if (spawned < 3) mat = Material.DEEPSLATE;
                else if (spawned < 8) mat = Material.SCULK;
                else mat = Material.SOUL_SOIL;

                Location loc = c.clone().add(0, spawned, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);
                handle.scale(1.2f, 1.0f, 1.2f)
                      .glow(26, 58, 0)
                      .interpolation(8, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
                spawned++;

                DisplayBuilder.playSound(loc, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.6f + spawned * 0.06f);

                if (spawned >= BLOCK_COUNT) {
                    fullyBuilt = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
                }
            }

            // Pulsing breathe effect once fully built
            if (fullyBuilt) {
                float breathCycle = ticksAlive * 0.06f;
                for (int i = 0; i < blocks.size(); i++) {
                    // Each block pulses with a phase offset
                    float phaseOffset = i * 0.4f;
                    float pulseX = 1.2f + 0.35f * (float) Math.sin(breathCycle + phaseOffset);
                    float pulseZ = 1.2f + 0.35f * (float) Math.sin(breathCycle + phaseOffset);
                    float pulseY = 1.0f + 0.08f * (float) Math.sin(breathCycle + phaseOffset + 1.0f);

                    blocks.get(i).animateTo(
                        new Vector3f(-pulseX / 2, -0.5f, -pulseZ / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulseX, pulseY, pulseZ),
                        8
                    );
                }

                // Sickly green particles oozing out during expansion
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 0.8 + Math.random() * 0.6;
                        double y = Math.random() * BLOCK_COUNT;
                        Location pLoc = c.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(pLoc, 3, 0.2, 26, 58, 0, 1.3f);
                    }
                }

                // Heartbeat sound synced with pulse
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PulsatingTower(plugin); }
    }

    // ================================================================
    // 7. DECAYING COLUMN — 14-block classic column that crumbles
    //    top-down, blocks fall with gravity simulation
    // ================================================================
    public static class DecayingColumn extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Boolean> fallen = new ArrayList<>();
        private final List<Float> fallVelocities = new ArrayList<>();
        private final List<Float> blockYPositions = new ArrayList<>();
        private boolean fullyBuilt = false;
        private int buildTick = 0;
        private int decayIndex;
        private static final int BLOCK_COUNT = 14;

        public DecayingColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("decaying_column", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build the column instantly
            for (int i = 0; i < BLOCK_COUNT; i++) {
                Material mat;
                if (i < 2 || i >= BLOCK_COUNT - 2) mat = Material.BLACKSTONE;
                else if (i % 3 == 0) mat = Material.CRYING_OBSIDIAN;
                else mat = Material.DEEPSLATE;

                Location loc = center.clone().add(0, i, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);
                handle.scale(1.0f, 1.0f, 1.0f)
                      .glow(74, 0, 0)
                      .interpolation(4, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
                fallen.add(false);
                fallVelocities.add(0.0f);
                blockYPositions.add((float) i);
            }
            decayIndex = BLOCK_COUNT - 1;
            fullyBuilt = true;

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.4f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!fullyBuilt) return;

            // Start crumbling after 60 ticks, one block every 8 ticks from top
            if (ticksAlive > 60 && ticksAlive % 8 == 0 && decayIndex >= 0) {
                fallen.set(decayIndex, true);
                fallVelocities.set(decayIndex, 0.02f);

                // Random horizontal offset for tumbling
                DisplayBuilder.playSound(
                    c.clone().add(0, blockYPositions.get(decayIndex), 0),
                    Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.7f + (float)(Math.random() * 0.4)
                );
                decayIndex--;
            }

            // Animate falling blocks with gravity
            for (int i = 0; i < BLOCK_COUNT; i++) {
                if (!fallen.get(i)) continue;

                float vel = fallVelocities.get(i) + 0.06f; // gravity acceleration
                fallVelocities.set(i, vel);

                float y = blockYPositions.get(i) - vel;
                if (y < 0) {
                    y = 0;
                    fallVelocities.set(i, 0.0f);

                    // Impact particles on ground hit
                    if (blockYPositions.get(i) > 0.1f) {
                        Location impactLoc = c.clone().add((Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2);
                        DisplayBuilder.dustParticles(impactLoc, 8, 0.5, 74, 0, 0, 1.0f);
                        DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.4f);
                    }
                }
                blockYPositions.set(i, y);

                // Tumble rotation as it falls
                float tumble = (BLOCK_COUNT - i) * 0.15f + ticksAlive * 0.1f;
                Location newLoc = c.clone().add((Math.random() - 0.5) * 0.05, y, (Math.random() - 0.5) * 0.05);
                blocks.get(i).entity().teleport(newLoc);
                blocks.get(i).animateTo(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new AxisAngle4f(tumble, 1, 0.5f, 0),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    4
                );
            }

            // Dust cloud around base during crumbling
            if (ticksAlive > 60 && ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 6, 1.0, 20, 10, 20, 1.5f);
            }

            // Ambient crumble sound
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DecayingColumn(plugin); }
    }

    // ================================================================
    // 8. SHADOW MONOLITH — 16 obsidian blocks form a doorway/arch,
    //    interior glows dark purple with swirling void particles
    // ================================================================
    public static class ShadowMonolith extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private boolean built = false;
        private static final int BLOCK_COUNT = 16;

        public ShadowMonolith(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_monolith", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(480);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left pillar: 6 blocks
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(-1.5, i, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                handle.scale(1.0f, 1.0f, 1.0f)
                      .glow(20, 10, 20)
                      .interpolation(6, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
            }

            // Right pillar: 6 blocks
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(1.5, i, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                handle.scale(1.0f, 1.0f, 1.0f)
                      .glow(20, 10, 20)
                      .interpolation(6, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
            }

            // Top arch: 4 blocks spanning across
            for (int i = 0; i < 4; i++) {
                double x = -1.5 + (3.0 / 3) * i;
                Location loc = center.clone().add(x, 6, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                handle.scale(1.0f, 1.0f, 1.0f)
                      .glow(45, 0, 64)
                      .interpolation(6, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
            }

            built = true;
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || !built) return;
            World w = c.getWorld();

            // Interior dark purple glow — swirling void particles inside doorway
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double x = (Math.random() - 0.5) * 2.0;
                    double y = Math.random() * 5.5 + 0.5;
                    double z = (Math.random() - 0.5) * 0.6;
                    Location pLoc = c.clone().add(x, y, z);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.15, 45, 0, 64, 1.8f);
                }
            }

            // Swirling particle vortex inside the doorway
            if (ticksAlive % 2 == 0) {
                double vortexAngle = ticksAlive * 0.12;
                double vortexRadius = 0.6 + 0.3 * Math.sin(ticksAlive * 0.05);
                Location vortexLoc = c.clone().add(
                    Math.cos(vortexAngle) * vortexRadius,
                    3.0 + Math.sin(ticksAlive * 0.04) * 1.5,
                    Math.sin(vortexAngle) * 0.2
                );
                DisplayBuilder.dustParticles(vortexLoc, 3, 0.1, 10, 0, 48, 1.5f);
            }

            // Subtle pulsing glow on arch blocks (top 4)
            float glowPulse = 0.9f + 0.15f * (float) Math.sin(ticksAlive * 0.1);
            for (int i = 12; i < blocks.size(); i++) {
                blocks.get(i).animateTo(
                    new Vector3f(-glowPulse / 2, -0.5f, -0.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(glowPulse, 1.0f, 1.0f),
                    6
                );
            }

            // Ambient void hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 3, 0), Sound.AMBIENT_CAVE, 0.5f, 0.3f);
            }

            // Heartbeat from within
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 3, 0), Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowMonolith(plugin); }
    }

    // ================================================================
    // 9. CORRUPTION STALAGMITE — 12 pointed blocks grow upward
    //    from a base, dripping dark particles like corrupted cave growth
    // ================================================================
    public static class CorruptionStalagmite extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blocks = new ArrayList<>();
        private final List<Float> growthTargets = new ArrayList<>();
        private final List<Float> currentGrowths = new ArrayList<>();
        private int spawned = 0;
        private static final int BLOCK_COUNT = 12;
        private static final float MAX_HEIGHT = 10.0f;

        public CorruptionStalagmite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_stalagmite", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(380);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Arrange blocks in a pointed cone formation
            // Base ring (6 blocks), middle ring (4 blocks), tip (2 blocks)
            // Base ring
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                growthTargets.add(MAX_HEIGHT * 0.4f);
                currentGrowths.add(0.0f);
            }
            // Middle ring
            for (int i = 0; i < 4; i++) {
                growthTargets.add(MAX_HEIGHT * 0.7f);
                currentGrowths.add(0.0f);
            }
            // Tip
            for (int i = 0; i < 2; i++) {
                growthTargets.add(MAX_HEIGHT);
                currentGrowths.add(0.0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.3f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spawn blocks progressively
            if (spawned < BLOCK_COUNT && ticksAlive % 3 == 0) {
                double ox, oz;
                Material mat;
                float scaleX;

                if (spawned < 6) {
                    // Base ring
                    double angle = (2 * Math.PI * spawned) / 6;
                    ox = Math.cos(angle) * 1.2;
                    oz = Math.sin(angle) * 1.2;
                    mat = Material.NETHERRACK;
                    scaleX = 0.9f;
                } else if (spawned < 10) {
                    // Middle ring
                    double angle = (2 * Math.PI * (spawned - 6)) / 4;
                    ox = Math.cos(angle) * 0.5;
                    oz = Math.sin(angle) * 0.5;
                    mat = Material.SCULK;
                    scaleX = 0.6f;
                } else {
                    // Tip
                    ox = (spawned == 10) ? 0.1 : -0.1;
                    oz = 0;
                    mat = Material.COAL_BLOCK;
                    scaleX = 0.3f;
                }

                Location loc = c.clone().add(ox, 0, oz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);
                handle.scale(scaleX, 0.5f, scaleX)
                      .glow(26, 58, 0)
                      .interpolation(6, 0);
                spawnedEntities.add(handle.entity());
                blocks.add(handle);
                spawned++;

                DisplayBuilder.playSound(loc, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.9f);
            }

            // Grow blocks upward toward their targets
            for (int i = 0; i < blocks.size(); i++) {
                float target = growthTargets.get(i);
                float current = currentGrowths.get(i);
                if (current < target) {
                    float newGrowth = Math.min(current + 0.2f, target);
                    currentGrowths.set(i, newGrowth);

                    // Recalculate position
                    double ox, oz;
                    float scaleX;
                    if (i < 6) {
                        double angle = (2 * Math.PI * i) / 6;
                        ox = Math.cos(angle) * 1.2;
                        oz = Math.sin(angle) * 1.2;
                        scaleX = 0.9f;
                    } else if (i < 10) {
                        double angle = (2 * Math.PI * (i - 6)) / 4;
                        ox = Math.cos(angle) * 0.5;
                        oz = Math.sin(angle) * 0.5;
                        scaleX = 0.6f;
                    } else {
                        ox = (i == 10) ? 0.1 : -0.1;
                        oz = 0;
                        scaleX = 0.3f;
                    }

                    Location newLoc = c.clone().add(ox, newGrowth, oz);
                    blocks.get(i).entity().teleport(newLoc);

                    // Taper as growth increases
                    float heightRatio = newGrowth / MAX_HEIGHT;
                    float dynamicTaper = scaleX * (1.0f - heightRatio * 0.5f);
                    blocks.get(i).animateTo(
                        new Vector3f(-dynamicTaper / 2, -0.25f, -dynamicTaper / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(dynamicTaper, 0.5f, dynamicTaper),
                        6
                    );
                }
            }

            // Dripping dark particles from tips
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < blocks.size(); i++) {
                    if (Math.random() < 0.3) {
                        Location dripLoc = blocks.get(i).entity().getLocation().clone();
                        dripLoc.add(0, -0.2, 0);
                        DisplayBuilder.dustParticles(dripLoc, 2, 0.1, 20, 10, 20, 0.8f);
                    }
                }
            }

            // Ambient sculk sound
            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionStalagmite(plugin); }
    }

    // ================================================================
    // 10. WARPED GROWTH — 15 blocks form organic corruption tree,
    //     trunk rises first, then branches spread outward over time
    // ================================================================
    public static class WarpedGrowth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> trunkBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> branchBlocks = new ArrayList<>();
        private int trunkBuilt = 0;
        private int branchesBuilt = 0;
        private boolean trunkComplete = false;
        private static final int TRUNK_COUNT = 7;
        private static final int BRANCH_COUNT = 8; // 15 total
        private static final float TRUNK_HEIGHT = 8.0f;

        public WarpedGrowth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("warped_growth", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(460);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.4f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Build trunk — one block every 4 ticks
            if (trunkBuilt < TRUNK_COUNT && !trunkComplete && ticksAlive % 4 == 0) {
                float y = (TRUNK_HEIGHT / TRUNK_COUNT) * trunkBuilt;
                // Slight S-curve for organic look
                double wobbleX = Math.sin(trunkBuilt * 0.8) * 0.3;
                double wobbleZ = Math.cos(trunkBuilt * 0.6) * 0.2;

                Material mat = (trunkBuilt % 2 == 0) ? Material.SCULK : Material.SOUL_SOIL;
                Location loc = c.clone().add(wobbleX, y, wobbleZ);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);

                float trunkTaper = 1.0f - (trunkBuilt * 0.06f);
                handle.scale(trunkTaper, 1.2f, trunkTaper)
                      .glow(26, 58, 0)
                      .interpolation(8, 0);
                spawnedEntities.add(handle.entity());
                trunkBlocks.add(handle);
                trunkBuilt++;

                DisplayBuilder.playSound(loc, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.5f + trunkBuilt * 0.08f);

                if (trunkBuilt >= TRUNK_COUNT) {
                    trunkComplete = true;
                    DisplayBuilder.playSound(c.clone().add(0, TRUNK_HEIGHT, 0), Sound.BLOCK_SCULK_BREAK, 0.8f, 0.6f);
                }
            }

            // Build branches — spread outward from trunk top
            if (trunkComplete && branchesBuilt < BRANCH_COUNT && ticksAlive % 6 == 0) {
                double branchAngle = (2 * Math.PI * branchesBuilt) / BRANCH_COUNT;
                double branchLen = 2.0 + Math.random() * 1.5;
                double bx = Math.cos(branchAngle) * branchLen;
                double bz = Math.sin(branchAngle) * branchLen;
                double by = TRUNK_HEIGHT - 1.0 + Math.random() * 2.0;

                // Droopy branches — angle downward slightly
                double droop = -Math.random() * 0.8;

                Material mat = (branchesBuilt % 3 == 0) ? Material.NETHERRACK : Material.SCULK;
                Location loc = c.clone().add(bx, by + droop, bz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);

                float branchAngleRad = (float)(branchAngle);
                handle.scale(0.5f, 0.5f, 1.5f)
                      .rotate(branchAngleRad, 0, 1, 0)
                      .glow(26, 58, 0)
                      .interpolation(10, 0);
                spawnedEntities.add(handle.entity());
                branchBlocks.add(handle);
                branchesBuilt++;

                DisplayBuilder.playSound(loc, Sound.BLOCK_SCULK_SPREAD, 0.3f, 1.0f);
            }

            // Organic growth animation — branches slowly extend
            if (trunkComplete) {
                for (int i = 0; i < branchBlocks.size(); i++) {
                    float growthPulse = 1.0f + 0.1f * (float) Math.sin(ticksAlive * 0.04 + i * 0.7);
                    float branchAngle = (float)((2 * Math.PI * i) / BRANCH_COUNT);
                    branchBlocks.get(i).animateTo(
                        new Vector3f(-0.25f, -0.25f, -0.75f * growthPulse),
                        new AxisAngle4f(branchAngle, 0, 1, 0),
                        new Vector3f(0.5f * growthPulse, 0.5f, 1.5f * growthPulse),
                        10
                    );
                }
            }

            // Sickly green corruption particles dripping from branches
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle branch : branchBlocks) {
                    if (Math.random() < 0.4) {
                        Location bLoc = branch.entity().getLocation().clone().add(0, -0.3, 0);
                        DisplayBuilder.dustParticles(bLoc, 3, 0.3, 26, 58, 0, 1.0f);
                    }
                }
                // Trunk ooze
                if (!trunkBlocks.isEmpty()) {
                    int randTrunk = (int)(Math.random() * trunkBlocks.size());
                    Location tLoc = trunkBlocks.get(randTrunk).entity().getLocation();
                    DisplayBuilder.dustParticles(tLoc, 2, 0.4, 45, 0, 64, 0.9f);
                }
            }

            // Ambient corruption sound
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WarpedGrowth(plugin); }
    }

    // ================================================================
    // 11. DARK CRYSTAL FORMATION — 10 tinted glass + deepslate blocks
    //     at angled orientations like crystals, with shimmer effect
    // ================================================================
    public static class DarkCrystalFormation extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crystals = new ArrayList<>();
        private final List<Float> crystalAngles = new ArrayList<>();
        private boolean built = false;
        private static final int CRYSTAL_COUNT = 10;

        public DarkCrystalFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_crystal_formation", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CRYSTAL_COUNT; i++) {
                double angle = (2 * Math.PI * i) / CRYSTAL_COUNT;
                double dist = 0.8 + Math.random() * 1.2;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                double oy = Math.random() * 3.0;

                Material mat = (i % 2 == 0) ? Material.TINTED_GLASS : Material.DEEPSLATE;
                Location loc = center.clone().add(ox, oy, oz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);

                // Random tilt angle for crystal-like appearance
                float tiltAngle = (float)(Math.random() * Math.PI * 0.6 - Math.PI * 0.3);
                float tiltAxis = (float)(Math.random() * Math.PI * 2);
                crystalAngles.add(tiltAngle);

                handle.scale(0.4f, 1.8f, 0.4f)
                      .rotate(tiltAngle, (float) Math.cos(tiltAxis), 0, (float) Math.sin(tiltAxis))
                      .glow(45, 0, 64)
                      .interpolation(8, 0);
                spawnedEntities.add(handle.entity());
                crystals.add(handle);
            }

            built = true;
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_BREAK, 1.3f, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || !built) return;

            // Shimmer effect — oscillate scale and brightness on each crystal
            for (int i = 0; i < crystals.size(); i++) {
                float shimmer = 1.0f + 0.15f * (float) Math.sin(ticksAlive * 0.12 + i * 1.2);
                float yShimmer = 1.8f + 0.2f * (float) Math.sin(ticksAlive * 0.08 + i * 0.9);

                float tiltAngle = crystalAngles.get(i);
                crystals.get(i).animateTo(
                    new Vector3f(-0.2f * shimmer, -0.9f, -0.2f * shimmer),
                    new AxisAngle4f(tiltAngle + ticksAlive * 0.003f, 0.5f, 1, 0),
                    new Vector3f(0.4f * shimmer, yShimmer, 0.4f * shimmer),
                    8
                );
            }

            // Shimmering light particles between crystals
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    int idx = (int)(Math.random() * crystals.size());
                    Location pLoc = crystals.get(idx).entity().getLocation().clone();
                    pLoc.add((Math.random() - 0.5) * 0.5, Math.random() * 1.5, (Math.random() - 0.5) * 0.5);

                    // Alternate between void blue and dark purple
                    if (Math.random() < 0.5) {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.1, 10, 0, 48, 1.0f);
                    } else {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.1, 45, 0, 64, 1.0f);
                    }
                }
            }

            // Connecting light beams between adjacent crystals
            if (ticksAlive % 10 == 0 && crystals.size() >= 2) {
                int a = (int)(Math.random() * crystals.size());
                int b = (a + 1) % crystals.size();
                Location locA = crystals.get(a).entity().getLocation();
                Location locB = crystals.get(b).entity().getLocation();
                DisplayBuilder.particleLine(locA, locB, Particle.DUST, 3,
                    new Particle.DustOptions(Color.fromRGB(45, 0, 64), 0.8f));
            }

            // Crystal resonance sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.4f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkCrystalFormation(plugin); }
    }

    // ================================================================
    // 12. CORRUPTION ANTENNA — 14-block tall pole, top sparks with
    //     dark lightning particles and electric crackle sounds
    // ================================================================
    public static class CorruptionAntenna extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> poleBlocks = new ArrayList<>();
        private BlockDisplayHandle topCap = null;
        private int poleBuilt = 0;
        private boolean fullyBuilt = false;
        private static final int POLE_COUNT = 13; // +1 top cap = 14 total
        private static final float POLE_HEIGHT = 13.0f;

        public CorruptionAntenna(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_antenna", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(420);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.4f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Build pole rapidly — one block every 2 ticks
            if (poleBuilt < POLE_COUNT && !fullyBuilt && ticksAlive % 2 == 0) {
                float y = (POLE_HEIGHT / POLE_COUNT) * poleBuilt;
                Material mat = (poleBuilt % 4 == 0) ? Material.COAL_BLOCK : Material.BLACKSTONE;
                Location loc = c.clone().add(0, y, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);

                float taper = 0.5f - (poleBuilt * 0.02f);
                handle.scale(taper, 1.0f, taper)
                      .glow(20, 10, 20)
                      .interpolation(4, 0);
                spawnedEntities.add(handle.entity());
                poleBlocks.add(handle);
                poleBuilt++;

                DisplayBuilder.playSound(loc, Sound.BLOCK_DEEPSLATE_BREAK, 0.3f, 0.8f + poleBuilt * 0.04f);
            }

            // Spawn glowing top cap
            if (poleBuilt >= POLE_COUNT && topCap == null) {
                Location topLoc = c.clone().add(0, POLE_HEIGHT, 0);
                topCap = displayBuilder.spawnBlock(topLoc, Material.SCULK);
                topCap.scale(0.6f, 0.6f, 0.6f)
                      .glow(45, 0, 64)
                      .interpolation(6, 0);
                spawnedEntities.add(topCap.entity());
                fullyBuilt = true;

                DisplayBuilder.playSound(topLoc, Sound.BLOCK_SCULK_SPREAD, 1.2f, 1.8f);
            }

            // Dark lightning sparks from the top
            if (fullyBuilt) {
                Location topLoc = c.clone().add(0, POLE_HEIGHT + 0.5, 0);

                // Spark particles every 2 ticks
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double sparkAngle = Math.random() * Math.PI * 2;
                        double sparkDist = Math.random() * 2.5;
                        double sparkY = (Math.random() - 0.5) * 3.0;
                        Location sparkLoc = topLoc.clone().add(
                            Math.cos(sparkAngle) * sparkDist,
                            sparkY,
                            Math.sin(sparkAngle) * sparkDist
                        );
                        DisplayBuilder.dustParticles(sparkLoc, 2, 0.05, 45, 0, 64, 0.6f);
                    }
                }

                // Jagged lightning bolt lines every 15 ticks
                if (ticksAlive % 15 == 0) {
                    double boltAngle = Math.random() * Math.PI * 2;
                    double boltDist = 2.0 + Math.random() * 3.0;
                    Location boltEnd = topLoc.clone().add(
                        Math.cos(boltAngle) * boltDist,
                        -3.0 - Math.random() * 4.0,
                        Math.sin(boltAngle) * boltDist
                    );
                    DisplayBuilder.particleLine(topLoc, boltEnd, Particle.DUST, 4,
                        new Particle.DustOptions(Color.fromRGB(10, 0, 48), 1.2f));

                    DisplayBuilder.playSound(topLoc, Sound.BLOCK_SCULK_BREAK, 0.8f, 1.5f);
                }

                // Pulsing cap glow
                float capPulse = 0.6f + 0.2f * (float) Math.sin(ticksAlive * 0.15);
                topCap.animateTo(
                    new Vector3f(-capPulse / 2, -capPulse / 2, -capPulse / 2),
                    new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                    new Vector3f(capPulse, capPulse, capPulse),
                    6
                );

                // Electric crackle sound
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(topLoc, Sound.BLOCK_SCULK_BREAK, 0.6f, 1.8f);
                }
            }

            // Ambient hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionAntenna(plugin); }
    }

    // ================================================================
    // 13. ROTTING TREE STUMP — 12 blocks form hollow dead tree with
    //     corruption oozing from its core, slow organic pulse
    // ================================================================
    public static class RottingTreeStump extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private boolean built = false;
        private static final int OUTER_COUNT = 8;
        private static final int CORE_COUNT = 4; // 12 total

        public RottingTreeStump(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rotting_tree_stump", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer hollow ring (stump walls) — 8 blocks in ring, 2 layers high
            for (int layer = 0; layer < 2; layer++) {
                for (int i = 0; i < 4; i++) {
                    double angle = (2 * Math.PI * i) / 4 + (layer * Math.PI / 4);
                    double ox = Math.cos(angle) * 1.3;
                    double oz = Math.sin(angle) * 1.3;

                    Material mat = (layer == 0) ? Material.NETHERRACK : Material.SOUL_SOIL;
                    Location loc = center.clone().add(ox, layer * 1.5, oz);
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);

                    // Irregular shapes for organic look
                    float scaleY = 1.5f + (float)(Math.random() * 0.5);
                    handle.scale(1.0f, scaleY, 1.0f)
                          .glow(74, 0, 0)
                          .interpolation(8, 0);
                    spawnedEntities.add(handle.entity());
                    outerBlocks.add(handle);
                }
            }

            // Core blocks — corruption mass inside the hollow
            for (int i = 0; i < CORE_COUNT; i++) {
                double ox = (Math.random() - 0.5) * 0.8;
                double oz = (Math.random() - 0.5) * 0.8;
                double oy = i * 0.6;

                Material mat = (i % 2 == 0) ? Material.SCULK : Material.CRYING_OBSIDIAN;
                Location loc = center.clone().add(ox, oy, oz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);
                handle.scale(0.7f, 0.7f, 0.7f)
                      .glow(45, 0, 64)
                      .interpolation(10, 0);
                spawnedEntities.add(handle.entity());
                coreBlocks.add(handle);
            }

            built = true;
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.3f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || !built) return;

            // Organic pulse on outer blocks — slight sway
            for (int i = 0; i < outerBlocks.size(); i++) {
                float sway = 0.05f * (float) Math.sin(ticksAlive * 0.03 + i * 0.8);
                float scaleY = 1.5f + 0.1f * (float) Math.sin(ticksAlive * 0.05 + i);
                outerBlocks.get(i).animateTo(
                    new Vector3f(-0.5f + sway, -0.5f, -0.5f),
                    new AxisAngle4f(sway * 2, 0, 1, 0),
                    new Vector3f(1.0f, scaleY, 1.0f),
                    8
                );
            }

            // Core corruption blocks pulse and glow
            for (int i = 0; i < coreBlocks.size(); i++) {
                float corePulse = 0.7f + 0.2f * (float) Math.sin(ticksAlive * 0.1 + i * 1.5);
                coreBlocks.get(i).animateTo(
                    new Vector3f(-corePulse / 2, -corePulse / 2, -corePulse / 2),
                    new AxisAngle4f(ticksAlive * 0.02f, 0, 1, 0),
                    new Vector3f(corePulse, corePulse, corePulse),
                    10
                );
            }

            // Corruption ooze particles from core — drip and spread
            if (ticksAlive % 3 == 0) {
                // Upward ooze from core
                Location coreLoc = c.clone().add(0, 1.0, 0);
                DisplayBuilder.dustParticles(coreLoc, 4, 0.5, 45, 0, 64, 1.4f);

                // Spreading ground corruption particles
                double spreadAngle = Math.random() * Math.PI * 2;
                double spreadDist = 1.5 + Math.random() * 1.0;
                Location spreadLoc = c.clone().add(Math.cos(spreadAngle) * spreadDist, 0.1, Math.sin(spreadAngle) * spreadDist);
                DisplayBuilder.dustParticles(spreadLoc, 2, 0.2, 26, 58, 0, 0.8f);
            }

            // Deep crimson drip particles from outer walls
            if (ticksAlive % 6 == 0) {
                int wallIdx = (int)(Math.random() * outerBlocks.size());
                Location wallLoc = outerBlocks.get(wallIdx).entity().getLocation().clone();
                wallLoc.add(0, -0.5, 0);
                DisplayBuilder.dustParticles(wallLoc, 3, 0.2, 74, 0, 0, 1.0f);
            }

            // Ambient sounds
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.3f);
            }
            if (ticksAlive % 55 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RottingTreeStump(plugin); }
    }

    // ================================================================
    // 14. VOID BEACON — 16 blocks form lighthouse tower, top emits
    //     rotating beam of dark particles sweeping 360 degrees
    // ================================================================
    public static class VoidBeacon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();
        private BlockDisplayHandle beaconTop = null;
        private int towerBuilt = 0;
        private boolean fullyBuilt = false;
        private float beamAngle = 0;
        private static final int TOWER_COUNT = 14; // +2 beacon blocks = 16
        private static final float TOWER_HEIGHT = 12.0f;
        private static final float BEAM_LENGTH = 8.0f;

        public VoidBeacon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_beacon", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Build tower — rapid construction, one block per tick
            if (towerBuilt < TOWER_COUNT && !fullyBuilt) {
                float y = (TOWER_HEIGHT / TOWER_COUNT) * towerBuilt;
                Material mat;
                if (towerBuilt < 3) mat = Material.BLACKSTONE;
                else if (towerBuilt < 10) mat = Material.DEEPSLATE;
                else mat = Material.OBSIDIAN;

                Location loc = c.clone().add(0, y, 0);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);

                // Slight taper toward top
                float taper = 0.9f - (towerBuilt * 0.03f);
                handle.scale(taper, TOWER_HEIGHT / TOWER_COUNT, taper)
                      .glow(20, 10, 20)
                      .interpolation(3, 0);
                spawnedEntities.add(handle.entity());
                towerBlocks.add(handle);
                towerBuilt++;

                if (towerBuilt % 3 == 0) {
                    DisplayBuilder.playSound(loc, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.5f + towerBuilt * 0.04f);
                }
            }

            // Spawn beacon top (2 blocks: platform + light)
            if (towerBuilt >= TOWER_COUNT && beaconTop == null) {
                // Platform block
                Location platLoc = c.clone().add(0, TOWER_HEIGHT, 0);
                BlockDisplayHandle platform = displayBuilder.spawnBlock(platLoc, Material.CRYING_OBSIDIAN);
                platform.scale(1.2f, 0.4f, 1.2f)
                        .glow(45, 0, 64)
                        .interpolation(6, 0);
                spawnedEntities.add(platform.entity());
                towerBlocks.add(platform);

                // Beacon light block
                Location lightLoc = c.clone().add(0, TOWER_HEIGHT + 0.5, 0);
                beaconTop = displayBuilder.spawnBlock(lightLoc, Material.SCULK);
                beaconTop.scale(0.5f, 0.8f, 0.5f)
                         .glow(10, 0, 48)
                         .interpolation(4, 0);
                spawnedEntities.add(beaconTop.entity());

                fullyBuilt = true;
                DisplayBuilder.playSound(c.clone().add(0, TOWER_HEIGHT, 0), Sound.BLOCK_SCULK_SPREAD, 1.2f, 1.5f);
            }

            // Rotating dark particle beam
            if (fullyBuilt) {
                beamAngle += 0.04f; // Smooth 360 rotation
                if (beamAngle > Math.PI * 2) beamAngle -= (float)(Math.PI * 2);

                Location beamOrigin = c.clone().add(0, TOWER_HEIGHT + 0.8, 0);

                // Draw beam as line of particles
                if (ticksAlive % 2 == 0) {
                    double beamEndX = Math.cos(beamAngle) * BEAM_LENGTH;
                    double beamEndZ = Math.sin(beamAngle) * BEAM_LENGTH;
                    Location beamEnd = beamOrigin.clone().add(beamEndX, -1.0, beamEndZ);
                    DisplayBuilder.particleLine(beamOrigin, beamEnd, Particle.DUST, 5,
                        new Particle.DustOptions(Color.fromRGB(10, 0, 48), 1.5f));

                    // Secondary dimmer beam offset
                    Location beamEnd2 = beamOrigin.clone().add(
                        Math.cos(beamAngle + 0.15) * BEAM_LENGTH * 0.8,
                        -0.5,
                        Math.sin(beamAngle + 0.15) * BEAM_LENGTH * 0.8
                    );
                    DisplayBuilder.particleLine(beamOrigin, beamEnd2, Particle.DUST, 3,
                        new Particle.DustOptions(Color.fromRGB(45, 0, 64), 0.9f));
                }

                // Rotate the beacon light block
                beaconTop.animateTo(
                    new Vector3f(-0.25f, -0.4f, -0.25f),
                    new AxisAngle4f(beamAngle * 2, 0, 1, 0),
                    new Vector3f(0.5f, 0.8f, 0.5f),
                    4
                );

                // Ambient glow particles at beacon top
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(beamOrigin, 5, 0.3, 10, 0, 48, 1.2f);
                }

                // Beacon sweep sound
                if (ticksAlive % 25 == 0) {
                    DisplayBuilder.playSound(beamOrigin, Sound.BLOCK_SCULK_SPREAD, 0.7f, 1.2f);
                }
            }

            // Tower base ambient sound
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBeacon(plugin); }
    }

    // ================================================================
    // 15. CORRUPTION FOUNTAIN — 12 blocks form a basin, dark particles
    //     spray upward in a fountain pattern and cascade down
    // ================================================================
    public static class CorruptionFountain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> basinBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spoutBlocks = new ArrayList<>();
        private boolean built = false;
        private static final int BASIN_COUNT = 8;
        private static final int SPOUT_COUNT = 4; // 12 total

        public CorruptionFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_fountain", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(420);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build basin — octagonal ring
            for (int i = 0; i < BASIN_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BASIN_COUNT;
                double ox = Math.cos(angle) * 2.0;
                double oz = Math.sin(angle) * 2.0;

                Material mat = (i % 2 == 0) ? Material.DEEPSLATE : Material.BLACKSTONE;
                Location loc = center.clone().add(ox, 0, oz);
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);
                handle.scale(1.2f, 0.8f, 1.2f)
                      .glow(20, 10, 20)
                      .interpolation(6, 0);
                spawnedEntities.add(handle.entity());
                basinBlocks.add(handle);
            }

            // Central spout column
            for (int i = 0; i < SPOUT_COUNT; i++) {
                Location loc = center.clone().add(0, i * 0.8, 0);
                Material mat = (i < 2) ? Material.CRYING_OBSIDIAN : Material.SCULK;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, mat);
                handle.scale(0.6f, 0.8f, 0.6f)
                      .glow(45, 0, 64)
                      .interpolation(8, 0);
                spawnedEntities.add(handle.entity());
                spoutBlocks.add(handle);
            }

            built = true;
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.4f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || !built) return;
            World w = c.getWorld();

            float spoutTop = SPOUT_COUNT * 0.8f;
            Location fountainTop = c.clone().add(0, spoutTop + 0.5, 0);

            // Upward spray particles — main fountain jet
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double sprayAngle = Math.random() * Math.PI * 2;
                    double spraySpread = Math.random() * 0.4;
                    double sprayHeight = 3.0 + Math.random() * 4.0;
                    Location sprayLoc = fountainTop.clone().add(
                        Math.cos(sprayAngle) * spraySpread,
                        sprayHeight,
                        Math.sin(sprayAngle) * spraySpread
                    );
                    DisplayBuilder.dustParticles(sprayLoc, 2, 0.1, 45, 0, 64, 1.3f);
                }
            }

            // Cascading particles falling outward from apex
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double cascadeAngle = Math.random() * Math.PI * 2;
                    double cascadeDist = 0.5 + Math.random() * 2.0;
                    double cascadeHeight = spoutTop + 5.0 - (cascadeDist * 1.5); // Arc down as distance increases

                    if (cascadeHeight < 0) cascadeHeight = 0;

                    Location cascadeLoc = c.clone().add(
                        Math.cos(cascadeAngle) * cascadeDist,
                        cascadeHeight,
                        Math.sin(cascadeAngle) * cascadeDist
                    );

                    // Alternate corruption colors
                    if (i % 3 == 0) {
                        DisplayBuilder.dustParticles(cascadeLoc, 2, 0.15, 10, 0, 48, 1.0f);
                    } else if (i % 3 == 1) {
                        DisplayBuilder.dustParticles(cascadeLoc, 2, 0.15, 45, 0, 64, 1.0f);
                    } else {
                        DisplayBuilder.dustParticles(cascadeLoc, 2, 0.15, 20, 10, 20, 0.8f);
                    }
                }
            }

            // Pooling particles at basin rim
            if (ticksAlive % 5 == 0) {
                double poolAngle = Math.random() * Math.PI * 2;
                Location poolLoc = c.clone().add(Math.cos(poolAngle) * 2.0, 0.3, Math.sin(poolAngle) * 2.0);
                DisplayBuilder.dustParticles(poolLoc, 3, 0.3, 26, 58, 0, 1.0f);
            }

            // Pulsing spout animation
            for (int i = 0; i < spoutBlocks.size(); i++) {
                float pulse = 0.6f + 0.1f * (float) Math.sin(ticksAlive * 0.1 + i * 0.8);
                spoutBlocks.get(i).animateTo(
                    new Vector3f(-pulse / 2, -0.4f, -pulse / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(pulse, 0.8f, pulse),
                    8
                );
            }

            // Basin blocks subtle pulse
            if (ticksAlive % 10 == 0) {
                for (int i = 0; i < basinBlocks.size(); i++) {
                    float basinPulse = 1.2f + 0.08f * (float) Math.sin(ticksAlive * 0.05 + i);
                    basinBlocks.get(i).animateTo(
                        new Vector3f(-basinPulse / 2, -0.4f, -basinPulse / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(basinPulse, 0.8f, basinPulse),
                        10
                    );
                }
            }

            // Fountain bubbling sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(fountainTop, Sound.BLOCK_SCULK_SPREAD, 0.6f, 1.2f);
            }

            // Deep ambient rumble
            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.5f, 0.4f);
            }

            // Heartbeat from the core
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionFountain(plugin); }
    }
}
