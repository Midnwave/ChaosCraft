package com.blockforge.chaoscraft.modes.chain.attacks;

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
 * Chain Mode — ENVIRONMENTAL / AMBIENT ATTACKS (101-115)
 * 15 chain-themed BlockDisplay attacks with atmospheric and environmental themes.
 * Each creates unique chain structures with distinct animations and particle effects.
 *
 * Color palette:
 * - Iron gray: RGB(180, 180, 190)
 * - Dark iron: RGB(100, 100, 110)
 * - Rust orange: RGB(180, 100, 40)
 * - Chain glow: RGB(200, 200, 220)
 * - Netherite dark: RGB(60, 50, 50)
 */
public final class EnvironmentalAmbient {

    private EnvironmentalAmbient() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(ChainAurora.create(plugin));
        registry.register(RustingChainField.create(plugin));
        registry.register(ChainEarthquake.create(plugin));
        registry.register(PhantomChains.create(plugin));
        registry.register(ChainFog.create(plugin));
        registry.register(ChainInfestation.create(plugin));
        registry.register(ChainThunderstorm.create(plugin));
        registry.register(ChainTide.create(plugin));
        registry.register(ChainEclipse.create(plugin));
        registry.register(HauntedChains.create(plugin));
        registry.register(ChainWhisper.create(plugin));
        registry.register(ChainFrostbite.create(plugin));
        registry.register(ChainSandstorm.create(plugin));
        registry.register(ChainGraveyardRise.create(plugin));
        registry.register(ChainApocalypse.create(plugin));
    }

    // ================================================================
    // 101. CHAIN AURORA — 16 chains form a shimmering curtain 20 blocks
    //      wide at sky height, undulating wave motion, descending slowly.
    //      Cyan/white glow particles.
    // ================================================================
    public static class ChainAurora extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 16;
        private static final float CURTAIN_WIDTH = 20.0f;
        private static final float START_HEIGHT = 25.0f;
        private static final float DESCENT_SPEED = 0.06f;
        private final List<BlockDisplayHandle> curtainChains = new ArrayList<>();
        private final List<Float> phaseOffsets = new ArrayList<>();
        private float currentHeight;

        public ChainAurora(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_aurora", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(25);
            currentHeight = START_HEIGHT;
        }

        public static ChainAurora create(ChaosCraftPlugin plugin) { return new ChainAurora(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                double x = -CURTAIN_WIDTH / 2 + (CURTAIN_WIDTH / (CHAIN_COUNT - 1.0)) * i;
                Location loc = center.clone().add(x, START_HEIGHT, 0);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                float height = 4.0f + (float)(Math.sin(i * 0.6) * 1.5);
                chain.scale(0.6f, height * 1.5f, 0.6f)
                     .glow(0, 220, 255)
                     .interpolation(3, 0);
                curtainChains.add(chain);
                spawnedEntities.add(chain.entity());
                phaseOffsets.add((float)(i * 0.4));
            }

            DisplayBuilder.playSound(center.clone().add(0, START_HEIGHT, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.8f);
            DisplayBuilder.playSound(center.clone().add(0, START_HEIGHT, 0), Sound.BLOCK_CHAIN_STEP, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            currentHeight = START_HEIGHT - (ticksAlive * DESCENT_SPEED);
            if (currentHeight < 1.0f) currentHeight = 1.0f;

            for (int i = 0; i < curtainChains.size(); i++) {
                double x = -CURTAIN_WIDTH / 2 + (CURTAIN_WIDTH / (CHAIN_COUNT - 1.0)) * i;
                float wave = (float) Math.sin(ticksAlive * 0.08 + phaseOffsets.get(i)) * 2.5f;
                float zWave = (float) Math.sin(ticksAlive * 0.05 + phaseOffsets.get(i) * 1.3) * 1.0f;
                Location newLoc = c.clone().add(x, currentHeight + wave, zWave);
                curtainChains.get(i).entity().teleport(newLoc);

                // Sway rotation
                float swayAngle = (float) Math.sin(ticksAlive * 0.06 + phaseOffsets.get(i)) * 0.12f;
                curtainChains.get(i).rotate(swayAngle, 0, 0, 1);
            }

            // Cyan/white shimmer particles
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double rx = c.getX() + (-CURTAIN_WIDTH / 2 + Math.random() * CURTAIN_WIDTH);
                    float ry = currentHeight + (float)(Math.random() * 4 - 2);
                    Location pLoc = new Location(w, rx, c.getY() + ry, c.getZ() + (Math.random() * 2 - 1));
                    if (Math.random() > 0.5) {
                        DisplayBuilder.dustParticles(pLoc, 3, 0.5, 0, 220, 255, 1.3f);
                    } else {
                        DisplayBuilder.dustParticles(pLoc, 3, 0.5, 230, 240, 255, 1.0f);
                    }
                }
            }

            // Ambient aurora hum
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, currentHeight, 0), Sound.BLOCK_CHAIN_STEP, 0.5f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainAurora(plugin); }
    }

    // ================================================================
    // 102. RUSTING CHAIN FIELD — 12 chains scattered on the ground,
    //      pulsing rust-orange glow, slowly corroding (scale shrinking).
    //      Continuous damage on contact.
    // ================================================================
    public static class RustingChainField extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 12;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Double> offsetsX = new ArrayList<>();
        private final List<Double> offsetsZ = new ArrayList<>();
        private final List<Float> baseScales = new ArrayList<>();
        private final List<Float> rotations = new ArrayList<>();

        public RustingChainField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rusting_chain_field", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        public static RustingChainField create(ChaosCraftPlugin plugin) { return new RustingChainField(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.0 + Math.random() * 5.0;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                offsetsX.add(ox);
                offsetsZ.add(oz);

                Location loc = center.clone().add(ox, 0.1, oz);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                float baseScale = 1.5f + (float)(Math.random() * 1.0);
                baseScales.add(baseScale);
                rotations.add((float)(Math.random() * Math.PI));

                chain.scale(baseScale * 1.5f, 0.45f, baseScale * 1.5f)
                     .rotate(rotations.get(i), 0, 1, 0)
                     .glow(180, 100, 40)
                     .interpolation(5, 0);
                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float corrosionFactor = 1.0f - (ticksAlive / (float) config.getDurationTicks()) * 0.7f;
            if (corrosionFactor < 0.3f) corrosionFactor = 0.3f;

            for (int i = 0; i < chains.size(); i++) {
                float scale = baseScales.get(i) * corrosionFactor;

                // Pulsing glow intensity
                float pulse = 0.7f + 0.3f * (float) Math.sin(ticksAlive * 0.1 + i * 0.8);
                chains.get(i).scale(scale, 0.3f * pulse, scale);

                // Rust particle emissions
                if (ticksAlive % 8 == (i % 8)) {
                    Location pLoc = c.clone().add(offsetsX.get(i), 0.3, offsetsZ.get(i));
                    DisplayBuilder.dustParticles(pLoc, 4, 0.4, 180, 100, 40, 1.2f);
                    w.spawnParticle(Particle.SMOKE, pLoc, 2, 0.3, 0.1, 0.3, 0.01);
                }
            }

            // Corrosion crackling sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.3f);
            }

            // Ground-level rust dust cloud
            if (ticksAlive % 6 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 5.0;
                Location dustLoc = c.clone().add(Math.cos(angle) * r, 0.2, Math.sin(angle) * r);
                DisplayBuilder.dustParticles(dustLoc, 5, 0.6, 150, 80, 30, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RustingChainField(plugin); }
    }

    // ================================================================
    // 103. CHAIN EARTHQUAKE — 14 chains burst from the ground at random
    //      positions within 8-block radius, shake violently for 2 seconds,
    //      retract. Repeats 3 times.
    // ================================================================
    public static class ChainEarthquake extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 14;
        private static final int BURST_TICKS = 40; // 2 seconds
        private static final int RETRACT_TICKS = 15;
        private static final int PAUSE_TICKS = 10;
        private static final int CYCLE_TICKS = BURST_TICKS + RETRACT_TICKS + PAUSE_TICKS;
        private static final int TOTAL_CYCLES = 3;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Double> offsetsX = new ArrayList<>();
        private final List<Double> offsetsZ = new ArrayList<>();
        private final List<Float> targetHeights = new ArrayList<>();
        private int currentCycle = 0;
        private boolean spawned = false;

        public ChainEarthquake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_earthquake", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(72.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(CYCLE_TICKS * TOTAL_CYCLES + 20);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(15);
        }

        public static ChainEarthquake create(ChaosCraftPlugin plugin) { return new ChainEarthquake(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.5 + Math.random() * 6.5;
                offsetsX.add(Math.cos(angle) * dist);
                offsetsZ.add(Math.sin(angle) * dist);
                targetHeights.add(3.0f + (float)(Math.random() * 4.0));
                chains.add(null);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int cycleIndex = ticksAlive / CYCLE_TICKS;
            if (cycleIndex >= TOTAL_CYCLES) return;

            int tickInCycle = ticksAlive % CYCLE_TICKS;

            // New cycle — regenerate positions
            if (tickInCycle == 0 && cycleIndex != currentCycle) {
                currentCycle = cycleIndex;
                // Remove old chains
                for (int i = 0; i < chains.size(); i++) {
                    if (chains.get(i) != null) {
                        chains.get(i).entity().remove();
                        chains.set(i, null);
                    }
                }
                // Randomize new positions
                offsetsX.clear();
                offsetsZ.clear();
                targetHeights.clear();
                for (int i = 0; i < CHAIN_COUNT; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = 1.5 + Math.random() * 6.5;
                    offsetsX.add(Math.cos(angle) * dist);
                    offsetsZ.add(Math.sin(angle) * dist);
                    targetHeights.add(3.0f + (float)(Math.random() * 4.0));
                }
                spawned = false;
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.4f + cycleIndex * 0.2f);
            }

            // BURST PHASE: chains rise from ground
            if (tickInCycle < BURST_TICKS) {
                float riseProgress = Math.min(1.0f, tickInCycle / 8.0f);

                if (!spawned) {
                    for (int i = 0; i < CHAIN_COUNT; i++) {
                        Location loc = c.clone().add(offsetsX.get(i), -1.0, offsetsZ.get(i));
                        BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                        chain.scale(0.75f, 0.15f, 0.75f)
                             .glow(180, 180, 190)
                             .interpolation(2, 0);
                        chains.set(i, chain);
                        spawnedEntities.add(chain.entity());

                        // Ground burst particles
                        w.spawnParticle(Particle.BLOCK, loc.clone().add(0, 1, 0), 15, 0.5, 0.2, 0.5, 0.1,
                                Material.DEEPSLATE.createBlockData());
                    }
                    spawned = true;
                }

                for (int i = 0; i < CHAIN_COUNT; i++) {
                    if (chains.get(i) == null) continue;
                    float height = targetHeights.get(i) * riseProgress;
                    Location loc = c.clone().add(offsetsX.get(i), 0, offsetsZ.get(i));

                    // Violent shaking
                    float shakeX = (float)(Math.sin(ticksAlive * 1.5 + i * 2.0) * 0.15);
                    float shakeZ = (float)(Math.cos(ticksAlive * 1.8 + i * 1.5) * 0.15);
                    loc.add(shakeX, 0, shakeZ);

                    chains.get(i).entity().teleport(loc);
                    chains.get(i).scale(0.5f, height, 0.5f);
                }

                // Ground rumble particles
                if (tickInCycle % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double px = c.getX() + (Math.random() * 16 - 8);
                        double pz = c.getZ() + (Math.random() * 16 - 8);
                        Location pLoc = new Location(w, px, c.getY(), pz);
                        w.spawnParticle(Particle.BLOCK, pLoc, 8, 0.5, 0.1, 0.5, 0.05,
                                Material.DEEPSLATE.createBlockData());
                    }
                }

                // Rumble sound
                if (tickInCycle % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.3f);
                }
            }

            // RETRACT PHASE: chains sink back
            else if (tickInCycle < BURST_TICKS + RETRACT_TICKS) {
                int retractTick = tickInCycle - BURST_TICKS;
                float retractProgress = retractTick / (float) RETRACT_TICKS;

                for (int i = 0; i < CHAIN_COUNT; i++) {
                    if (chains.get(i) == null) continue;
                    float height = targetHeights.get(i) * (1.0f - retractProgress);
                    Location loc = c.clone().add(offsetsX.get(i), -retractProgress * 1.0, offsetsZ.get(i));
                    chains.get(i).entity().teleport(loc);
                    chains.get(i).scale(0.5f, Math.max(0.1f, height), 0.5f);
                }

                if (retractTick == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.9f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainEarthquake(plugin); }
    }

    // ================================================================
    // 104. PHANTOM CHAINS — 10 translucent chains orbit the player at
    //      varying heights/speeds. Occasionally one solidifies (scales up)
    //      and strikes inward.
    // ================================================================
    public static class PhantomChains extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 10;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Float> orbitRadii = new ArrayList<>();
        private final List<Float> orbitSpeeds = new ArrayList<>();
        private final List<Float> heights = new ArrayList<>();
        private final List<Float> phaseAngles = new ArrayList<>();
        private final List<Boolean> striking = new ArrayList<>();
        private final List<Integer> strikeTimers = new ArrayList<>();
        private final List<Float> strikeProgress = new ArrayList<>();
        private int nextStrikeTick = 40;

        public PhantomChains(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_chains", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(30);
        }

        public static PhantomChains create(ChaosCraftPlugin plugin) { return new PhantomChains(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float radius = 3.5f + (float)(Math.random() * 2.5);
                float speed = 0.04f + (float)(Math.random() * 0.06);
                float height = 0.5f + (float)(Math.random() * 3.0);
                float phase = (float)(Math.random() * Math.PI * 2);

                orbitRadii.add(radius);
                orbitSpeeds.add(speed);
                heights.add(height);
                phaseAngles.add(phase);
                striking.add(false);
                strikeTimers.add(0);
                strikeProgress.add(0.0f);

                double x = Math.cos(phase) * radius;
                double z = Math.sin(phase) * radius;
                Location loc = center.clone().add(x, height, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.38f, 1.8f, 0.38f)
                     .glow(200, 200, 220)
                     .interpolation(2, 0);
                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_STEP, 0.8f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Trigger periodic strikes
            if (ticksAlive >= nextStrikeTick) {
                int target = (int)(Math.random() * CHAIN_COUNT);
                if (!striking.get(target)) {
                    striking.set(target, true);
                    strikeTimers.set(target, 0);
                    strikeProgress.set(target, 0.0f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.9f, 0.5f);
                }
                nextStrikeTick = ticksAlive + 30 + (int)(Math.random() * 25);
            }

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float angle = phaseAngles.get(i) + ticksAlive * orbitSpeeds.get(i);
                float radius = orbitRadii.get(i);
                float h = heights.get(i);

                if (striking.get(i)) {
                    int st = strikeTimers.get(i) + 1;
                    strikeTimers.set(i, st);

                    if (st < 10) {
                        // Solidify — scale up
                        float progress = st / 10.0f;
                        strikeProgress.set(i, progress);
                        chains.get(i).scale(0.25f + progress * 0.75f, 1.2f + progress * 1.5f, 0.25f + progress * 0.75f);
                        chains.get(i).glow(255, (int)(200 * (1.0f - progress)), (int)(220 * (1.0f - progress)));
                    } else if (st < 18) {
                        // Strike inward
                        float strikeT = (st - 10) / 8.0f;
                        radius = orbitRadii.get(i) * (1.0f - strikeT * 0.85f);
                        chains.get(i).scale(1.0f, 2.7f, 1.0f);
                    } else {
                        // Reset
                        striking.set(i, false);
                        strikeTimers.set(i, 0);
                        chains.get(i).scale(0.25f, 1.2f, 0.25f);
                        chains.get(i).glow(200, 200, 220);
                    }
                }

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                float heightBob = (float)(Math.sin(ticksAlive * 0.07 + i) * 0.5);
                Location newLoc = c.clone().add(x, h + heightBob, z);
                chains.get(i).entity().teleport(newLoc);

                // Ghost particles
                if (ticksAlive % 5 == (i % 5)) {
                    DisplayBuilder.dustParticles(newLoc, 2, 0.2, 200, 200, 220, 0.6f);
                }
            }

            // Eerie whisper sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.3f, 1.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomChains(plugin); }
    }

    // ================================================================
    // 105. CHAIN FOG — 20 tiny chain fragments at waist height, slowly
    //      drifting in random directions like fog. Gray dust particles
    //      for low visibility effect.
    // ================================================================
    public static class ChainFog extends BlockDisplayAttack {

        private static final int FRAGMENT_COUNT = 20;
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private final List<Double> posX = new ArrayList<>();
        private final List<Double> posZ = new ArrayList<>();
        private final List<Double> velX = new ArrayList<>();
        private final List<Double> velZ = new ArrayList<>();
        private final List<Float> posY = new ArrayList<>();

        public ChainFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_fog", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(380);
            config.setTicksBetweenDamage(25);
        }

        public static ChainFog create(ChaosCraftPlugin plugin) { return new ChainFog(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < FRAGMENT_COUNT; i++) {
                double ox = (Math.random() - 0.5) * 12.0;
                double oz = (Math.random() - 0.5) * 12.0;
                posX.add(ox);
                posZ.add(oz);
                velX.add((Math.random() - 0.5) * 0.06);
                velZ.add((Math.random() - 0.5) * 0.06);
                posY.add(0.8f + (float)(Math.random() * 0.6));

                Location loc = center.clone().add(ox, posY.get(i), oz);
                BlockDisplayHandle frag = displayBuilder.spawnBlock(loc, Material.CHAIN);
                frag.scale(0.23f + (float)(Math.random() * 0.3), 0.23f, 0.23f + (float)(Math.random() * 0.3))
                    .glow(100, 100, 110)
                    .interpolation(3, 0);
                float rot = (float)(Math.random() * Math.PI);
                frag.rotate(rot, 0, 1, 0);
                fragments.add(frag);
                spawnedEntities.add(frag.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < fragments.size(); i++) {
                // Drift
                double nx = posX.get(i) + velX.get(i);
                double nz = posZ.get(i) + velZ.get(i);

                // Gentle direction changes
                if (ticksAlive % 60 == (i * 3) % 60) {
                    velX.set(i, velX.get(i) + (Math.random() - 0.5) * 0.03);
                    velZ.set(i, velZ.get(i) + (Math.random() - 0.5) * 0.03);
                    // Clamp velocity
                    velX.set(i, Math.max(-0.08, Math.min(0.08, velX.get(i))));
                    velZ.set(i, Math.max(-0.08, Math.min(0.08, velZ.get(i))));
                }

                posX.set(i, nx);
                posZ.set(i, nz);

                float yBob = posY.get(i) + (float)(Math.sin(ticksAlive * 0.03 + i * 0.7) * 0.15);
                Location newLoc = c.clone().add(nx, yBob, nz);
                fragments.get(i).entity().teleport(newLoc);

                // Slow rotation
                float rot = (float)(ticksAlive * 0.02 + i * 0.5);
                fragments.get(i).rotate(rot, 0, 1, 0);
            }

            // Dense fog particles — gray dust cloud at waist height
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double rx = c.getX() + (Math.random() - 0.5) * 12;
                    double rz = c.getZ() + (Math.random() - 0.5) * 12;
                    Location fogLoc = new Location(w, rx, c.getY() + 0.5 + Math.random() * 1.0, rz);
                    DisplayBuilder.dustParticles(fogLoc, 3, 0.8, 130, 130, 140, 1.5f);
                }
            }

            // Muffled chain sounds
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.3f, 0.4f);
            }

            // Occasional deeper fog pulse
            if (ticksAlive % 50 == 0) {
                for (int i = 0; i < 15; i++) {
                    double rx = c.getX() + (Math.random() - 0.5) * 14;
                    double rz = c.getZ() + (Math.random() - 0.5) * 14;
                    Location pulseLoc = new Location(w, rx, c.getY() + 0.3 + Math.random() * 0.8, rz);
                    w.spawnParticle(Particle.SMOKE, pulseLoc, 3, 1.0, 0.3, 1.0, 0.005);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainFog(plugin); }
    }

    // ================================================================
    // 106. CHAIN INFESTATION — 15 chains emerge from walls/ground like
    //      worms, writhing with sine-wave motion along their length.
    //      Organic horror feel.
    // ================================================================
    public static class ChainInfestation extends BlockDisplayAttack {

        private static final int WORM_COUNT = 15;
        private static final int SEGMENTS_PER_WORM = 4;
        private final List<List<BlockDisplayHandle>> worms = new ArrayList<>();
        private final List<Double> baseX = new ArrayList<>();
        private final List<Double> baseZ = new ArrayList<>();
        private final List<Float> baseY = new ArrayList<>();
        private final List<Float> wormPhase = new ArrayList<>();
        private final List<Float> emergeProgress = new ArrayList<>();
        private final List<Float> emergeSpeed = new ArrayList<>();

        public ChainInfestation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_infestation", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(380);
            config.setTicksBetweenDamage(20);
        }

        public static ChainInfestation create(ChaosCraftPlugin plugin) { return new ChainInfestation(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int worm = 0; worm < WORM_COUNT; worm++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.0 + Math.random() * 6.0;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                baseX.add(ox);
                baseZ.add(oz);
                baseY.add(-0.5f);
                wormPhase.add((float)(Math.random() * Math.PI * 2));
                emergeProgress.add(0.0f);
                emergeSpeed.add(0.02f + (float)(Math.random() * 0.03));

                List<BlockDisplayHandle> segments = new ArrayList<>();
                for (int seg = 0; seg < SEGMENTS_PER_WORM; seg++) {
                    Location loc = center.clone().add(ox, -0.5 + seg * 0.3, oz);
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    chain.scale(0.3f, 0.6f, 0.3f)
                         .glow(100, 100, 110)
                         .interpolation(2, 0);
                    segments.add(chain);
                    spawnedEntities.add(chain.entity());
                }
                worms.add(segments);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int worm = 0; worm < WORM_COUNT; worm++) {
                // Slowly emerge
                float emerge = emergeProgress.get(worm);
                if (emerge < 1.0f) {
                    emerge = Math.min(1.0f, emerge + emergeSpeed.get(worm));
                    emergeProgress.set(worm, emerge);
                }

                float phase = wormPhase.get(worm);
                List<BlockDisplayHandle> segments = worms.get(worm);

                for (int seg = 0; seg < SEGMENTS_PER_WORM; seg++) {
                    // Sine wave writhing
                    float segPhase = phase + ticksAlive * 0.12f + seg * 0.8f;
                    float waveX = (float) Math.sin(segPhase) * 0.3f * emerge;
                    float waveZ = (float) Math.cos(segPhase * 1.3f) * 0.3f * emerge;
                    float segHeight = (seg * 0.5f + 0.2f) * emerge;

                    Location segLoc = c.clone().add(
                            baseX.get(worm) + waveX,
                            segHeight - 0.2,
                            baseZ.get(worm) + waveZ
                    );
                    segments.get(seg).entity().teleport(segLoc);

                    // Writhing rotation
                    float rotAngle = (float) Math.sin(segPhase * 0.8f) * 0.4f;
                    segments.get(seg).rotate(rotAngle, (float) Math.cos(seg), 0, (float) Math.sin(seg));
                }

                // Emergence dirt particles
                if (emerge < 0.8f && ticksAlive % 6 == (worm % 6)) {
                    Location dirtLoc = c.clone().add(baseX.get(worm), 0.1, baseZ.get(worm));
                    w.spawnParticle(Particle.BLOCK, dirtLoc, 5, 0.2, 0.1, 0.2, 0.02,
                            Material.DEEPSLATE.createBlockData());
                }
            }

            // Organic squelching chain sounds
            if (ticksAlive % 18 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.3f);
            }

            // Horror ambient dust
            if (ticksAlive % 8 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 5.0;
                Location dustLoc = c.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r);
                DisplayBuilder.dustParticles(dustLoc, 3, 0.4, 60, 50, 50, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainInfestation(plugin); }
    }

    // ================================================================
    // 107. CHAIN THUNDERSTORM — 12 chains arranged as vertical lightning
    //      bolts (zigzag pattern), flash bright then fade, strike down
    //      at random positions.
    // ================================================================
    public static class ChainThunderstorm extends BlockDisplayAttack {

        private static final int BOLT_COUNT = 12;
        private static final int SEGMENTS_PER_BOLT = 5;
        private final List<List<BlockDisplayHandle>> bolts = new ArrayList<>();
        private final List<Double> boltX = new ArrayList<>();
        private final List<Double> boltZ = new ArrayList<>();
        private final List<Integer> strikeDelays = new ArrayList<>();
        private final List<Boolean> struck = new ArrayList<>();
        private final List<Integer> flashTimers = new ArrayList<>();

        public ChainThunderstorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_thunderstorm", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(75.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(380);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(75.0);
            config.setImpactRadius(7.0);
        }

        public static ChainThunderstorm create(ChaosCraftPlugin plugin) { return new ChainThunderstorm(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int bolt = 0; bolt < BOLT_COUNT; bolt++) {
                double ox = (Math.random() - 0.5) * 14;
                double oz = (Math.random() - 0.5) * 14;
                boltX.add(ox);
                boltZ.add(oz);
                strikeDelays.add(10 + bolt * 12 + (int)(Math.random() * 15));
                struck.add(false);
                flashTimers.add(0);

                List<BlockDisplayHandle> segments = new ArrayList<>();
                for (int seg = 0; seg < SEGMENTS_PER_BOLT; seg++) {
                    Location loc = center.clone().add(ox, 25, oz); // Start hidden high
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    chain.scale(0.45f, 1.5f, 0.45f)
                         .glow(200, 200, 220)
                         .interpolation(1, 0);
                    segments.add(chain);
                    spawnedEntities.add(chain.entity());
                }
                bolts.add(segments);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int bolt = 0; bolt < BOLT_COUNT; bolt++) {
                if (ticksAlive < strikeDelays.get(bolt)) continue;

                if (!struck.get(bolt)) {
                    struck.set(bolt, true);
                    flashTimers.set(bolt, 0);

                    // Create zigzag lightning bolt pattern
                    List<BlockDisplayHandle> segments = bolts.get(bolt);
                    double bx = boltX.get(bolt);
                    double bz = boltZ.get(bolt);

                    for (int seg = 0; seg < SEGMENTS_PER_BOLT; seg++) {
                        float y = 20.0f - seg * 4.0f;
                        float zigX = (float)((seg % 2 == 0 ? 1 : -1) * (0.5 + Math.random() * 0.8));
                        float zigZ = (float)((seg % 2 == 0 ? -1 : 1) * (0.3 + Math.random() * 0.5));
                        Location segLoc = c.clone().add(bx + zigX, y, bz + zigZ);
                        segments.get(seg).entity().teleport(segLoc);
                        segments.get(seg).scale(0.4f, 4.2f, 0.4f);
                        segments.get(seg).glow(255, 255, 240);
                    }

                    // Thunder sound and impact
                    Location impactLoc = c.clone().add(bx, 0, bz);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.2f, 1.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.8f);
                    triggerImpactDamage(impactLoc);

                    // Ground impact particles
                    DisplayBuilder.particleRing(impactLoc, 3.5, Particle.DUST, 30,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 200), 1.5f));
                    w.spawnParticle(Particle.FLASH, impactLoc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.CRIT, impactLoc, 20, 1.5, 0.5, 1.5, 0.3);
                }

                // Flash fade effect
                int flash = flashTimers.get(bolt) + 1;
                flashTimers.set(bolt, flash);

                if (flash < 20) {
                    float fade = 1.0f - (flash / 20.0f);
                    List<BlockDisplayHandle> segments = bolts.get(bolt);
                    for (BlockDisplayHandle seg : segments) {
                        int brightness = (int)(255 * fade);
                        seg.glow(brightness, brightness, (int)(240 * fade));
                        seg.scale(0.4f * fade + 0.1f, 4.2f, 0.4f * fade + 0.1f);
                    }
                } else if (flash == 20) {
                    // Hide bolt segments
                    List<BlockDisplayHandle> segments = bolts.get(bolt);
                    for (BlockDisplayHandle seg : segments) {
                        seg.entity().teleport(c.clone().add(0, -50, 0));
                    }
                }
            }

            // Ambient thunder rumble
            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.4f, 0.2f);
            }

            // Sky-high chain particle flickers
            if (ticksAlive % 4 == 0) {
                double rx = c.getX() + (Math.random() - 0.5) * 16;
                double rz = c.getZ() + (Math.random() - 0.5) * 16;
                Location flickerLoc = new Location(w, rx, c.getY() + 18 + Math.random() * 5, rz);
                DisplayBuilder.dustParticles(flickerLoc, 2, 0.5, 200, 200, 220, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainThunderstorm(plugin); }
    }

    // ================================================================
    // 108. CHAIN TIDE — 18 chains form a wave wall 10 blocks wide,
    //      sweeps forward in sine-wave motion, recedes, then comes back
    //      higher. 3 wave cycles.
    // ================================================================
    public static class ChainTide extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 18;
        private static final float WAVE_WIDTH = 10.0f;
        private static final float SWEEP_DISTANCE = 12.0f;
        private static final int TICKS_PER_CYCLE = 80;
        private static final int TOTAL_CYCLES = 3;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Float> xPositions = new ArrayList<>();

        public ChainTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_tide", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(TICKS_PER_CYCLE * TOTAL_CYCLES + 40);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        public static ChainTide create(ChaosCraftPlugin plugin) { return new ChainTide(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float x = -WAVE_WIDTH / 2 + (WAVE_WIDTH / (CHAIN_COUNT - 1)) * i;
                xPositions.add(x);

                Location loc = center.clone().add(x, 0, -SWEEP_DISTANCE / 2);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.6f, 3.75f, 0.6f)
                     .glow(180, 180, 190)
                     .interpolation(2, 0);
                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int cycleIndex = Math.min(ticksAlive / TICKS_PER_CYCLE, TOTAL_CYCLES - 1);
            int tickInCycle = ticksAlive % TICKS_PER_CYCLE;
            float cycleProgress = tickInCycle / (float) TICKS_PER_CYCLE;

            // Wave height increases each cycle
            float waveHeight = 2.5f + cycleIndex * 1.5f;

            // Sweep: forward then back (sin curve for smooth motion)
            float sweepZ = (float) Math.sin(cycleProgress * Math.PI) * SWEEP_DISTANCE - SWEEP_DISTANCE / 2;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float x = xPositions.get(i);

                // Sine wave undulation along the wall width
                float waveOffset = (float) Math.sin(cycleProgress * Math.PI * 2 + i * 0.4) * 1.5f;
                float y = waveHeight * (float) Math.abs(Math.sin(cycleProgress * Math.PI)) + waveOffset;
                if (y < 0.5f) y = 0.5f;

                Location newLoc = c.clone().add(x, y, sweepZ);
                chains.get(i).entity().teleport(newLoc);
                chains.get(i).scale(0.4f, waveHeight * 0.8f, 0.4f);

                // Rotation to simulate wave lean
                float lean = (float) Math.sin(cycleProgress * Math.PI * 2 + i * 0.3) * 0.2f;
                chains.get(i).rotate(lean, 1, 0, 0);
            }

            // Wave crest spray particles
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    float rx = xPositions.get((int)(Math.random() * CHAIN_COUNT));
                    Location sprayLoc = c.clone().add(rx, waveHeight + 1, sweepZ);
                    DisplayBuilder.dustParticles(sprayLoc, 4, 0.6, 180, 180, 190, 1.0f);
                    w.spawnParticle(Particle.CRIT, sprayLoc, 2, 0.3, 0.5, 0.3, 0.05);
                }
            }

            // Wave crash sound at peak
            if (tickInCycle == TICKS_PER_CYCLE / 2) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.5f + cycleIndex * 0.15f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.9f, 0.6f);
            }

            // Ambient tide sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainTide(plugin); }
    }

    // ================================================================
    // 109. CHAIN ECLIPSE — 16 chains form a ring (solar eclipse shape)
    //      high above, slowly descends while contracting.
    //      Shadow particles below.
    // ================================================================
    public static class ChainEclipse extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 16;
        private static final float INITIAL_RADIUS = 8.0f;
        private static final float START_HEIGHT = 22.0f;
        private static final float FINAL_RADIUS = 1.5f;
        private static final float FINAL_HEIGHT = 2.0f;
        private final List<BlockDisplayHandle> ringChains = new ArrayList<>();
        private BlockDisplayHandle coreBlock;

        public ChainEclipse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_eclipse", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(72.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(18);
        }

        public static ChainEclipse create(ChaosCraftPlugin plugin) { return new ChainEclipse(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central dark core
            Location coreLoc = center.clone().add(0, START_HEIGHT, 0);
            coreBlock = displayBuilder.spawnBlock(coreLoc, Material.NETHERITE_BLOCK);
            coreBlock.scale(3.75f, 1.2f, 3.75f)
                     .glow(60, 50, 50)
                     .interpolation(3, 0);
            spawnedEntities.add(coreBlock.entity());

            // Ring of chains around the core
            for (int i = 0; i < CHAIN_COUNT; i++) {
                double angle = (2 * Math.PI * i) / CHAIN_COUNT;
                double x = Math.cos(angle) * INITIAL_RADIUS;
                double z = Math.sin(angle) * INITIAL_RADIUS;
                Location loc = center.clone().add(x, START_HEIGHT, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.75f, 3.0f, 0.75f)
                     .glow(180, 180, 190)
                     .interpolation(3, 0);
                ringChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, START_HEIGHT, 0), Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float progress = Math.min(1.0f, ticksAlive / (float)(config.getDurationTicks() - 30));
            float currentRadius = INITIAL_RADIUS + (FINAL_RADIUS - INITIAL_RADIUS) * progress;
            float currentHeight = START_HEIGHT + (FINAL_HEIGHT - START_HEIGHT) * progress;

            // Move core
            Location coreLoc = c.clone().add(0, currentHeight, 0);
            coreBlock.entity().teleport(coreLoc);

            // Slowly increase core pulse
            float coreScale = 2.5f + (float)(Math.sin(ticksAlive * 0.08) * 0.3) * (1.0f + progress);
            coreBlock.scale(coreScale, 0.8f, coreScale);

            // Rotate and contract ring
            for (int i = 0; i < CHAIN_COUNT; i++) {
                double baseAngle = (2 * Math.PI * i) / CHAIN_COUNT;
                double rotatedAngle = baseAngle + ticksAlive * 0.015;
                double x = Math.cos(rotatedAngle) * currentRadius;
                double z = Math.sin(rotatedAngle) * currentRadius;

                float chainY = currentHeight + (float)(Math.sin(ticksAlive * 0.1 + i * 0.5) * 0.4);
                Location chainLoc = c.clone().add(x, chainY, z);
                ringChains.get(i).entity().teleport(chainLoc);

                // Point chains outward
                float outAngle = (float) rotatedAngle;
                ringChains.get(i).rotate(outAngle, 0, 1, 0);
            }

            // Shadow particles on the ground below the eclipse
            if (ticksAlive % 4 == 0) {
                float shadowRadius = currentRadius * 1.2f;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * shadowRadius;
                    Location shadowLoc = c.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(shadowLoc, 3, 0.5, 30, 25, 30, 1.5f);
                }
            }

            // Dark corona particles around eclipse
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location coronaLoc = c.clone().add(
                            Math.cos(angle) * (currentRadius + 0.5), currentHeight,
                            Math.sin(angle) * (currentRadius + 0.5));
                    DisplayBuilder.dustParticles(coronaLoc, 2, 0.3, 100, 100, 110, 1.0f);
                }
            }

            // Ominous humming sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, currentHeight, 0), Sound.BLOCK_CHAIN_STEP, 0.6f, 0.2f);
            }

            // Intensify sound as it gets closer
            if (progress > 0.8f && ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.4f + progress * 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainEclipse(plugin); }
    }

    // ================================================================
    // 110. HAUNTED CHAINS — 12 chains hang from invisible anchors,
    //      sway erratically as if possessed. Occasionally one drops
    //      and slams the ground.
    // ================================================================
    public static class HauntedChains extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 12;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Double> anchorX = new ArrayList<>();
        private final List<Double> anchorZ = new ArrayList<>();
        private final List<Float> anchorY = new ArrayList<>();
        private final List<Float> chainLengths = new ArrayList<>();
        private final List<Boolean> dropping = new ArrayList<>();
        private final List<Integer> dropTimers = new ArrayList<>();
        private final List<Float> dropY = new ArrayList<>();
        private int nextDropTick = 30;

        public HauntedChains(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("haunted_chains", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(420);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(69.0);
            config.setImpactRadius(7.0);
        }

        public static HauntedChains create(ChaosCraftPlugin plugin) { return new HauntedChains(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                double angle = (2 * Math.PI * i) / CHAIN_COUNT + (Math.random() - 0.5) * 0.5;
                double dist = 2.0 + Math.random() * 4.0;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                float ay = 6.0f + (float)(Math.random() * 3.0);
                float chainLen = 3.0f + (float)(Math.random() * 2.0);

                anchorX.add(ox);
                anchorZ.add(oz);
                anchorY.add(ay);
                chainLengths.add(chainLen);
                dropping.add(false);
                dropTimers.add(0);
                dropY.add(ay);

                Location loc = center.clone().add(ox, ay, oz);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.45f, chainLen * 1.5f, 0.45f)
                     .glow(200, 200, 220)
                     .interpolation(2, 0);
                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Trigger periodic drops
            if (ticksAlive >= nextDropTick) {
                int target = (int)(Math.random() * CHAIN_COUNT);
                if (!dropping.get(target)) {
                    dropping.set(target, true);
                    dropTimers.set(target, 0);
                    dropY.set(target, anchorY.get(target));
                    DisplayBuilder.playSound(c.clone().add(anchorX.get(target), anchorY.get(target), anchorZ.get(target)),
                            Sound.BLOCK_CHAIN_BREAK, 0.9f, 1.2f);
                }
                nextDropTick = ticksAlive + 25 + (int)(Math.random() * 30);
            }

            for (int i = 0; i < CHAIN_COUNT; i++) {
                double ox = anchorX.get(i);
                double oz = anchorZ.get(i);

                if (dropping.get(i)) {
                    int dt = dropTimers.get(i) + 1;
                    dropTimers.set(i, dt);

                    if (dt < 15) {
                        // Falling
                        float fall = dropY.get(i) - 0.6f;
                        dropY.set(i, fall);
                        Location newLoc = c.clone().add(ox, fall, oz);
                        chains.get(i).entity().teleport(newLoc);

                        if (fall <= 0.5f) {
                            // Ground slam
                            dropY.set(i, 0.5f);
                            Location impactLoc = c.clone().add(ox, 0, oz);
                            triggerImpactDamage(impactLoc);

                            DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
                            DisplayBuilder.particleRing(impactLoc, 3.0, Particle.DUST, 20,
                                    new Particle.DustOptions(Color.fromRGB(180, 180, 190), 1.3f));
                            w.spawnParticle(Particle.BLOCK, impactLoc, 20, 1, 0.2, 1, 0.1,
                                    Material.IRON_BLOCK.createBlockData());
                        }
                    } else if (dt < 40) {
                        // Stay on ground
                        chains.get(i).entity().teleport(c.clone().add(ox, 0.5, oz));
                    } else if (dt < 60) {
                        // Rise back up
                        float riseProgress = (dt - 40) / 20.0f;
                        float y = 0.5f + (anchorY.get(i) - 0.5f) * riseProgress;
                        chains.get(i).entity().teleport(c.clone().add(ox, y, oz));
                    } else {
                        dropping.set(i, false);
                        dropTimers.set(i, 0);
                        dropY.set(i, anchorY.get(i));
                    }
                } else {
                    // Erratic possessed swaying
                    float swayX = (float)(Math.sin(ticksAlive * 0.2 + i * 1.7) * 0.5 +
                                          Math.sin(ticksAlive * 0.37 + i * 2.3) * 0.3);
                    float swayZ = (float)(Math.cos(ticksAlive * 0.15 + i * 1.1) * 0.4 +
                                          Math.cos(ticksAlive * 0.42 + i * 3.1) * 0.25);
                    Location newLoc = c.clone().add(ox + swayX, anchorY.get(i), oz + swayZ);
                    chains.get(i).entity().teleport(newLoc);

                    // Erratic rotation
                    float rotAngle = (float)(Math.sin(ticksAlive * 0.25 + i * 1.5) * 0.3);
                    chains.get(i).rotate(rotAngle, 0, 0, 1);
                }
            }

            // Creepy ambient sounds
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.4f + (float)(Math.random() * 0.8));
            }

            // Ghostly particles around swaying chains
            if (ticksAlive % 6 == 0) {
                int idx = ticksAlive / 6 % CHAIN_COUNT;
                Location pLoc = c.clone().add(anchorX.get(idx), anchorY.get(idx) - 1, anchorZ.get(idx));
                DisplayBuilder.dustParticles(pLoc, 3, 0.4, 200, 200, 220, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HauntedChains(plugin); }
    }

    // ================================================================
    // 111. CHAIN WHISPER — 10 very small chains orbit close to player's
    //      head height in tight circle, spinning fast. Eerie ambient
    //      sounds. Small damage radius but high damage.
    // ================================================================
    public static class ChainWhisper extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 10;
        private static final float ORBIT_RADIUS = 1.8f;
        private static final float ORBIT_SPEED = 0.18f;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Float> heightOffsets = new ArrayList<>();
        private final List<Float> phaseOffsets = new ArrayList<>();

        public ChainWhisper(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_whisper", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(78.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(12);
        }

        public static ChainWhisper create(ChaosCraftPlugin plugin) { return new ChainWhisper(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float phase = (float)((2 * Math.PI * i) / CHAIN_COUNT);
                float heightOff = 1.2f + (float)(Math.random() * 0.8);
                phaseOffsets.add(phase);
                heightOffsets.add(heightOff);

                double x = Math.cos(phase) * ORBIT_RADIUS;
                double z = Math.sin(phase) * ORBIT_RADIUS;
                Location loc = center.clone().add(x, heightOff, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.18f, 0.75f, 0.18f)
                     .glow(200, 200, 220)
                     .interpolation(1, 0);
                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_STEP, 0.4f, 1.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Speed increases over time
            float speedMultiplier = 1.0f + (ticksAlive / (float) config.getDurationTicks()) * 0.5f;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float angle = phaseOffsets.get(i) + ticksAlive * ORBIT_SPEED * speedMultiplier;

                // Slight radius variation
                float r = ORBIT_RADIUS + (float)(Math.sin(ticksAlive * 0.1 + i) * 0.3);
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;

                float heightBob = heightOffsets.get(i) + (float)(Math.sin(ticksAlive * 0.15 + i * 0.7) * 0.2);
                Location newLoc = c.clone().add(x, heightBob, z);
                chains.get(i).entity().teleport(newLoc);

                // Spin rotation to face tangent
                chains.get(i).rotate(angle, 0, 1, 0);
            }

            // Whisper trail particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 3; i++) {
                    float angle = (float)(Math.random() * Math.PI * 2);
                    Location trailLoc = c.clone().add(
                            Math.cos(angle) * ORBIT_RADIUS, 1.5 + Math.random() * 0.5,
                            Math.sin(angle) * ORBIT_RADIUS);
                    DisplayBuilder.dustParticles(trailLoc, 2, 0.15, 180, 180, 190, 0.5f);
                }
            }

            // Eerie whisper sounds — high-pitched chain steps
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.25f, 1.8f + (float)(Math.random() * 0.4));
            }

            // Occasional chain clink
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.3f, 1.9f);
            }

            // Inner glow particles at center
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 4, 0.3, 220, 220, 240, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainWhisper(plugin); }
    }

    // ================================================================
    // 112. CHAIN FROSTBITE — 14 chains coated in ice (BLUE_ICE + CHAIN),
    //      spread across ground in star pattern from center.
    //      Snowflake particles.
    // ================================================================
    public static class ChainFrostbite extends BlockDisplayAttack {

        private static final int ARM_COUNT = 7;
        private static final int CHAINS_PER_ARM = 2;
        private static final float ARM_LENGTH = 6.0f;
        private final List<BlockDisplayHandle> chainSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> iceSegments = new ArrayList<>();
        private final List<Float> armAngles = new ArrayList<>();
        private float spreadProgress = 0;

        public ChainFrostbite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_frostbite", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(370);
            config.setTicksBetweenDamage(18);
        }

        public static ChainFrostbite create(ChaosCraftPlugin plugin) { return new ChainFrostbite(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int arm = 0; arm < ARM_COUNT; arm++) {
                float angle = (float)((2 * Math.PI * arm) / ARM_COUNT);
                armAngles.add(angle);

                for (int seg = 0; seg < CHAINS_PER_ARM; seg++) {
                    // Chain segment
                    Location chainLoc = center.clone(); // Will be positioned in onTick
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                    chain.scale(0.45f, 0.15f, 0.45f)
                         .glow(150, 200, 255)
                         .interpolation(2, 0);
                    chainSegments.add(chain);
                    spawnedEntities.add(chain.entity());

                    // Ice segment alongside
                    BlockDisplayHandle ice = displayBuilder.spawnBlock(chainLoc, Material.BLUE_ICE);
                    ice.scale(0.3f, 0.23f, 0.3f)
                       .glow(100, 180, 255)
                       .interpolation(2, 0);
                    iceSegments.add(ice);
                    spawnedEntities.add(ice.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Spread outward over first 60 ticks
            if (ticksAlive < 60) {
                spreadProgress = ticksAlive / 60.0f;
            } else {
                spreadProgress = 1.0f;
            }

            for (int arm = 0; arm < ARM_COUNT; arm++) {
                float angle = armAngles.get(arm);
                float frostPulse = (float)(Math.sin(ticksAlive * 0.08 + arm * 0.9) * 0.3);

                for (int seg = 0; seg < CHAINS_PER_ARM; seg++) {
                    int idx = arm * CHAINS_PER_ARM + seg;
                    float dist = ((seg + 1.0f) / CHAINS_PER_ARM) * ARM_LENGTH * spreadProgress;

                    double x = Math.cos(angle) * dist;
                    double z = Math.sin(angle) * dist;

                    // Chain on ground, lying flat along the arm direction
                    Location chainLoc = c.clone().add(x, 0.15 + frostPulse * 0.1, z);
                    chainSegments.get(idx).entity().teleport(chainLoc);
                    chainSegments.get(idx).scale(0.3f + frostPulse * 0.1f, 0.2f, ARM_LENGTH / CHAINS_PER_ARM * 0.8f);
                    chainSegments.get(idx).rotate(angle, 0, 1, 0);

                    // Ice slightly offset
                    Location iceLoc = c.clone().add(
                            x + Math.cos(angle + Math.PI / 2) * 0.2,
                            0.1,
                            z + Math.sin(angle + Math.PI / 2) * 0.2);
                    iceSegments.get(idx).entity().teleport(iceLoc);
                    iceSegments.get(idx).scale(0.25f + frostPulse * 0.05f, 0.15f, ARM_LENGTH / CHAINS_PER_ARM * 0.5f);
                    iceSegments.get(idx).rotate(angle, 0, 1, 0);
                }
            }

            // Snowflake particles along arms
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    int arm = (int)(Math.random() * ARM_COUNT);
                    float dist = (float)(Math.random() * ARM_LENGTH * spreadProgress);
                    double x = Math.cos(armAngles.get(arm)) * dist;
                    double z = Math.sin(armAngles.get(arm)) * dist;
                    Location snowLoc = c.clone().add(x, 0.5 + Math.random() * 1.5, z);
                    DisplayBuilder.dustParticles(snowLoc, 3, 0.3, 200, 230, 255, 0.8f);
                    w.spawnParticle(Particle.SNOWFLAKE, snowLoc, 2, 0.3, 0.3, 0.3, 0.01);
                }
            }

            // Frost creeping sounds
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 1.6f);
            }

            // Center frost burst particles
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), spreadProgress * ARM_LENGTH,
                        Particle.SNOWFLAKE, 16, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainFrostbite(plugin); }
    }

    // ================================================================
    // 113. CHAIN SANDSTORM — 18 small chains swirling in expanding/
    //      contracting cylinder. Sand-colored dust particles.
    //      Periodic gusts change direction.
    // ================================================================
    public static class ChainSandstorm extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 18;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Float> orbitAngles = new ArrayList<>();
        private final List<Float> orbitHeights = new ArrayList<>();
        private final List<Float> orbitSpeeds = new ArrayList<>();
        private float currentRadius = 3.0f;
        private float targetRadius = 3.0f;
        private float gustAngle = 0;
        private int nextGustTick = 40;

        public ChainSandstorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_sandstorm", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(370);
            config.setTicksBetweenDamage(18);
        }

        public static ChainSandstorm create(ChaosCraftPlugin plugin) { return new ChainSandstorm(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float angle = (float)(Math.random() * Math.PI * 2);
                float height = 0.5f + (float)(Math.random() * 5.0);
                float speed = 0.06f + (float)(Math.random() * 0.08);

                orbitAngles.add(angle);
                orbitHeights.add(height);
                orbitSpeeds.add(speed);

                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                Location loc = center.clone().add(x, height, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.3f, 0.6f, 0.3f)
                     .glow(180, 160, 100)
                     .interpolation(2, 0);
                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Periodic gust changes
            if (ticksAlive >= nextGustTick) {
                targetRadius = 2.0f + (float)(Math.random() * 5.0);
                gustAngle = (float)(Math.random() * Math.PI * 2);
                nextGustTick = ticksAlive + 40 + (int)(Math.random() * 30);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.0f);
            }

            // Smoothly approach target radius
            currentRadius += (targetRadius - currentRadius) * 0.05f;

            // Gust offset
            float gustX = (float)(Math.cos(gustAngle) * 0.03);
            float gustZ = (float)(Math.sin(gustAngle) * 0.03);

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float angle = orbitAngles.get(i) + ticksAlive * orbitSpeeds.get(i);
                orbitAngles.set(i, angle);

                float heightBob = orbitHeights.get(i) + (float)(Math.sin(ticksAlive * 0.07 + i * 0.8) * 0.5);

                double x = Math.cos(angle) * currentRadius + gustX * ticksAlive * 0.1;
                double z = Math.sin(angle) * currentRadius + gustZ * ticksAlive * 0.1;

                // Contain within a max range
                x = Math.max(-8, Math.min(8, x));
                z = Math.max(-8, Math.min(8, z));

                Location newLoc = c.clone().add(x, heightBob, z);
                chains.get(i).entity().teleport(newLoc);

                // Spin each chain
                float spin = ticksAlive * 0.15f + i * 0.5f;
                chains.get(i).rotate(spin, 0, 1, 0);
            }

            // Sand-colored dust particles filling the cylinder
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * currentRadius;
                    float h = (float)(Math.random() * 5.0);
                    Location dustLoc = c.clone().add(Math.cos(angle) * r, h, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(dustLoc, 2, 0.5, 210, 180, 120, 1.0f);
                }
            }

            // Swirling sand effect
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), currentRadius, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(190, 165, 100), 1.2f));
            }

            // Gritty chain sounds
            if (ticksAlive % 22 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSandstorm(plugin); }
    }

    // ================================================================
    // 114. CHAIN GRAVEYARD RISE — 15 chains rise slowly from ground like
    //      undead hands, each at different timing. Reach full height,
    //      pause, then topple outward.
    // ================================================================
    public static class ChainGraveyardRise extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 15;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Double> posX = new ArrayList<>();
        private final List<Double> posZ = new ArrayList<>();
        private final List<Integer> riseDelays = new ArrayList<>();
        private final List<Float> maxHeights = new ArrayList<>();
        private final List<Float> currentHeights = new ArrayList<>();
        private final List<Float> toppleAngles = new ArrayList<>();
        private final List<Float> toppleDirections = new ArrayList<>();
        private final List<Integer> states = new ArrayList<>(); // 0=waiting, 1=rising, 2=paused, 3=toppling
        private final List<Integer> stateTimers = new ArrayList<>();
        private static final int PAUSE_DURATION = 30;

        public ChainGraveyardRise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_graveyard_rise", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(380);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(69.0);
            config.setImpactRadius(7.0);
        }

        public static ChainGraveyardRise create(ChaosCraftPlugin plugin) { return new ChainGraveyardRise(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.5 + Math.random() * 5.5;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                posX.add(ox);
                posZ.add(oz);
                riseDelays.add(i * 8 + (int)(Math.random() * 12));
                maxHeights.add(3.0f + (float)(Math.random() * 3.0));
                currentHeights.add(0.0f);
                toppleAngles.add(0.0f);
                toppleDirections.add((float)(Math.random() * Math.PI * 2));
                states.add(0);
                stateTimers.add(0);

                Location loc = center.clone().add(ox, -0.5, oz);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.6f, 0.15f, 0.6f)
                     .glow(100, 100, 110)
                     .interpolation(2, 0);
                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < CHAIN_COUNT; i++) {
                int state = states.get(i);
                int timer = stateTimers.get(i) + 1;
                stateTimers.set(i, timer);

                switch (state) {
                    case 0: // Waiting
                        if (ticksAlive >= riseDelays.get(i)) {
                            states.set(i, 1);
                            stateTimers.set(i, 0);
                            DisplayBuilder.playSound(c.clone().add(posX.get(i), 0, posZ.get(i)),
                                    Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.4f);
                            // Ground break particles
                            Location groundLoc = c.clone().add(posX.get(i), 0.1, posZ.get(i));
                            w.spawnParticle(Particle.BLOCK, groundLoc, 12, 0.3, 0.1, 0.3, 0.05,
                                    Material.DEEPSLATE.createBlockData());
                        }
                        break;

                    case 1: // Rising
                        float riseSpeed = 0.08f;
                        float h = currentHeights.get(i) + riseSpeed;
                        currentHeights.set(i, h);

                        Location riseLoc = c.clone().add(posX.get(i), h * 0.5, posZ.get(i));
                        chains.get(i).entity().teleport(riseLoc);
                        chains.get(i).scale(0.4f, h, 0.4f);
                        chains.get(i).glow(140, 140, 150);

                        // Shaking while rising
                        float shake = (float)(Math.sin(timer * 0.8) * 0.05);
                        chains.get(i).rotate(shake, 0, 0, 1);

                        // Dirt particles while rising
                        if (timer % 4 == 0) {
                            Location dirtLoc = c.clone().add(posX.get(i), 0.1, posZ.get(i));
                            w.spawnParticle(Particle.BLOCK, dirtLoc, 5, 0.2, 0.1, 0.2, 0.02,
                                    Material.DEEPSLATE.createBlockData());
                        }

                        if (h >= maxHeights.get(i)) {
                            states.set(i, 2);
                            stateTimers.set(i, 0);
                            DisplayBuilder.playSound(c.clone().add(posX.get(i), h, posZ.get(i)),
                                    Sound.BLOCK_CHAIN_STEP, 0.7f, 0.6f);
                        }
                        break;

                    case 2: // Paused at full height
                        // Slight ominous sway
                        float pauseShake = (float)(Math.sin(timer * 0.3) * 0.08);
                        chains.get(i).rotate(pauseShake, 0, 0, 1);

                        if (timer >= PAUSE_DURATION) {
                            states.set(i, 3);
                            stateTimers.set(i, 0);
                        }
                        break;

                    case 3: // Toppling outward
                        float toppleSpeed = 0.06f;
                        float toppleAngle = toppleAngles.get(i) + toppleSpeed;
                        toppleAngles.set(i, toppleAngle);

                        float dir = toppleDirections.get(i);
                        // Lean outward using rotation
                        chains.get(i).rotate(toppleAngle, (float)Math.cos(dir), 0, (float)Math.sin(dir));

                        if (toppleAngle >= Math.PI / 2) {
                            // Slammed flat — impact
                            toppleAngles.set(i, (float)(Math.PI / 2));
                            float fallDist = currentHeights.get(i);
                            Location impactLoc = c.clone().add(
                                    posX.get(i) + Math.cos(dir) * fallDist * 0.5,
                                    0,
                                    posZ.get(i) + Math.sin(dir) * fallDist * 0.5);
                            triggerImpactDamage(impactLoc);

                            DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f);
                            w.spawnParticle(Particle.BLOCK, impactLoc, 15, 1, 0.2, 1, 0.1,
                                    Material.IRON_BLOCK.createBlockData());
                            DisplayBuilder.particleRing(impactLoc, 2.5, Particle.DUST, 16,
                                    new Particle.DustOptions(Color.fromRGB(100, 100, 110), 1.2f));

                            states.set(i, 4); // Done
                        }
                        break;
                }
            }

            // Graveyard ambient particles
            if (ticksAlive % 5 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 6.0;
                Location ambientLoc = c.clone().add(Math.cos(angle) * r, 0.2, Math.sin(angle) * r);
                w.spawnParticle(Particle.SMOKE, ambientLoc, 3, 0.5, 0.1, 0.5, 0.005);
                DisplayBuilder.dustParticles(ambientLoc, 2, 0.4, 60, 50, 50, 0.8f);
            }

            // Creaking metal sounds
            if (ticksAlive % 28 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainGraveyardRise(plugin); }
    }

    // ================================================================
    // 115. CHAIN APOCALYPSE — THE FINALE. 25+ chains forming a massive
    //      structure: 4 pillars + connecting chains + central heavy core.
    //      Entire structure rotates, ascends, then crashes down with
    //      massive shockwave. 60+ HP damage.
    // ================================================================
    public static class ChainApocalypse extends BlockDisplayAttack {

        private static final int PILLAR_COUNT = 4;
        private static final int CHAINS_PER_PILLAR = 4;
        private static final int CONNECTING_CHAINS = 8;
        private static final int CORE_BLOCKS = 5;
        // Total: 4*4 + 8 + 5 = 29 block displays
        private final List<List<BlockDisplayHandle>> pillars = new ArrayList<>();
        private final List<BlockDisplayHandle> connectors = new ArrayList<>();
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private float structureAngle = 0;
        private float structureHeight = 0;
        private int phase = 0; // 0=assemble, 1=rotate+ascend, 2=crash, 3=aftermath
        private int phaseTimer = 0;
        private static final float MAX_HEIGHT = 18.0f;
        private static final int ASSEMBLE_TICKS = 40;
        private static final int ASCEND_TICKS = 80;
        private static final int CRASH_TICKS = 20;
        private boolean crashImpacted = false;

        public ChainApocalypse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_apocalypse", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(93.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(99.0);
            config.setImpactRadius(16.0);
        }

        public static ChainApocalypse create(ChaosCraftPlugin plugin) { return new ChainApocalypse(plugin); }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 pillars, each with 4 chain segments
            for (int p = 0; p < PILLAR_COUNT; p++) {
                double pillarAngle = (2 * Math.PI * p) / PILLAR_COUNT;
                double px = Math.cos(pillarAngle) * 4.0;
                double pz = Math.sin(pillarAngle) * 4.0;

                List<BlockDisplayHandle> pillar = new ArrayList<>();
                for (int seg = 0; seg < CHAINS_PER_PILLAR; seg++) {
                    Location loc = center.clone().add(px, seg * 1.5, pz);
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    chain.scale(0.9f, 2.25f, 0.9f)
                         .glow(180, 180, 190)
                         .interpolation(2, 0);
                    pillar.add(chain);
                    spawnedEntities.add(chain.entity());
                }
                pillars.add(pillar);
            }

            // 8 connecting chains between pillars (horizontal links)
            for (int i = 0; i < CONNECTING_CHAINS; i++) {
                Location loc = center.clone().add(0, 3, 0);
                BlockDisplayHandle connector = displayBuilder.spawnBlock(loc, Material.IRON_BARS);
                connector.scale(0.45f, 0.45f, 6.0f)
                         .glow(100, 100, 110)
                         .interpolation(2, 0);
                connectors.add(connector);
                spawnedEntities.add(connector.entity());
            }

            // Central core: heavy core + netherite + anvil + deepslate
            Material[] coreMats = {Material.HEAVY_CORE, Material.NETHERITE_BLOCK, Material.ANVIL, Material.DEEPSLATE, Material.IRON_BLOCK};
            for (int i = 0; i < CORE_BLOCKS; i++) {
                Location loc = center.clone().add(0, 2 + i * 0.6, 0);
                BlockDisplayHandle core = displayBuilder.spawnBlock(loc, coreMats[i]);
                float s = (i == 0) ? 2.7f : 1.5f + (float)(Math.random() * 0.75);
                core.scale(s, s * 0.8f, s)
                    .glow(60, 50, 50)
                    .interpolation(3, 0);
                coreBlocks.add(core);
                spawnedEntities.add(core.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            phaseTimer++;

            switch (phase) {
                case 0: // ASSEMBLE — structure builds up
                    assemblePhase(c, w, ticksAlive);
                    if (phaseTimer >= ASSEMBLE_TICKS) {
                        phase = 1;
                        phaseTimer = 0;
                        DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.2f, 0.5f);
                    }
                    break;

                case 1: // ROTATE + ASCEND
                    ascendPhase(c, w, ticksAlive);
                    if (phaseTimer >= ASCEND_TICKS) {
                        phase = 2;
                        phaseTimer = 0;
                        DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.2f);
                    }
                    break;

                case 2: // CRASH DOWN
                    crashPhase(c, w, ticksAlive);
                    if (phaseTimer >= CRASH_TICKS) {
                        phase = 3;
                        phaseTimer = 0;
                    }
                    break;

                case 3: // AFTERMATH — debris settling
                    aftermathPhase(c, w, ticksAlive);
                    break;
            }
        }

        private void assemblePhase(Location c, World w, int ticksAlive) {
            float assembleProgress = Math.min(1.0f, phaseTimer / (float) ASSEMBLE_TICKS);

            // Pillars rise from ground
            for (int p = 0; p < PILLAR_COUNT; p++) {
                double pillarAngle = (2 * Math.PI * p) / PILLAR_COUNT;
                double px = Math.cos(pillarAngle) * 4.0;
                double pz = Math.sin(pillarAngle) * 4.0;

                for (int seg = 0; seg < CHAINS_PER_PILLAR; seg++) {
                    float targetY = seg * 1.5f;
                    float currentY = targetY * assembleProgress - 1.0f * (1.0f - assembleProgress);
                    Location loc = c.clone().add(px, currentY, pz);
                    pillars.get(p).get(seg).entity().teleport(loc);
                }
            }

            // Core assembles in center
            for (int i = 0; i < coreBlocks.size(); i++) {
                float targetY = 2.0f + i * 0.6f;
                float scatterX = (1.0f - assembleProgress) * (float)(Math.sin(i * 2.0) * 3);
                float scatterZ = (1.0f - assembleProgress) * (float)(Math.cos(i * 2.0) * 3);
                Location loc = c.clone().add(scatterX, targetY * assembleProgress, scatterZ);
                coreBlocks.get(i).entity().teleport(loc);
            }

            // Connectors form
            positionConnectors(c, 0, 0);

            // Assembly particles
            if (phaseTimer % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.0;
                    Location pLoc = c.clone().add(Math.cos(angle) * r, Math.random() * 4, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(pLoc, 3, 0.4, 180, 180, 190, 1.0f);
                }
            }

            // Assembly sounds
            if (phaseTimer % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.5f + assembleProgress * 0.5f);
            }
        }

        private void ascendPhase(Location c, World w, int ticksAlive) {
            float ascendProgress = phaseTimer / (float) ASCEND_TICKS;
            structureHeight = ascendProgress * MAX_HEIGHT;
            structureAngle += 0.04f + ascendProgress * 0.08f; // Accelerating rotation

            // Rotate and lift all pillars
            for (int p = 0; p < PILLAR_COUNT; p++) {
                double basePillarAngle = (2 * Math.PI * p) / PILLAR_COUNT + structureAngle;
                double px = Math.cos(basePillarAngle) * 4.0;
                double pz = Math.sin(basePillarAngle) * 4.0;

                for (int seg = 0; seg < CHAINS_PER_PILLAR; seg++) {
                    float y = structureHeight + seg * 1.5f;
                    Location loc = c.clone().add(px, y, pz);
                    pillars.get(p).get(seg).entity().teleport(loc);

                    // Lean outward slightly from rotation
                    float lean = ascendProgress * 0.15f;
                    pillars.get(p).get(seg).rotate(lean, (float)Math.cos(basePillarAngle), 0, (float)Math.sin(basePillarAngle));
                }
            }

            // Core rises and spins
            for (int i = 0; i < coreBlocks.size(); i++) {
                float y = structureHeight + 2.0f + i * 0.6f;
                float coreBob = (float)(Math.sin(ticksAlive * 0.1 + i) * 0.3);
                Location loc = c.clone().add(0, y + coreBob, 0);
                coreBlocks.get(i).entity().teleport(loc);
                coreBlocks.get(i).rotate(structureAngle * 2, 0, 1, 0);

                // Core glow intensifies
                int glowIntensity = (int)(60 + ascendProgress * 140);
                coreBlocks.get(i).glow(glowIntensity, (int)(50 + ascendProgress * 50), (int)(50 + ascendProgress * 50));
            }

            // Connectors rotate with structure
            positionConnectors(c, structureHeight, structureAngle);

            // Rising energy particles
            if (phaseTimer % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = 1.0 + Math.random() * 4.0;
                    Location pLoc = c.clone().add(Math.cos(angle) * r, structureHeight + Math.random() * 6, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(pLoc, 3, 0.5, 200, 200, 220, 1.2f);
                }
                // Upward energy beam particles
                for (int i = 0; i < 4; i++) {
                    Location beamLoc = c.clone().add((Math.random() - 0.5) * 2, Math.random() * structureHeight, (Math.random() - 0.5) * 2);
                    DisplayBuilder.dustParticles(beamLoc, 2, 0.2, 255, 200, 100, 0.8f);
                }
            }

            // Accelerating rotation sound
            if (phaseTimer % (int)(15 - ascendProgress * 10) == 0) {
                DisplayBuilder.playSound(c.clone().add(0, structureHeight, 0), Sound.BLOCK_CHAIN_STEP, 0.8f, 0.6f + ascendProgress * 0.8f);
            }

            // Building rumble
            if (phaseTimer % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.6f + ascendProgress * 0.6f, 0.3f);
            }
        }

        private void crashPhase(Location c, World w, int ticksAlive) {
            float crashProgress = phaseTimer / (float) CRASH_TICKS;
            float currentCrashHeight = MAX_HEIGHT * (1.0f - crashProgress * crashProgress); // Accelerating fall
            structureAngle += 0.12f; // Still spinning fast

            // Everything plummets
            for (int p = 0; p < PILLAR_COUNT; p++) {
                double basePillarAngle = (2 * Math.PI * p) / PILLAR_COUNT + structureAngle;
                // Pillars splay outward as they fall
                double splayRadius = 4.0 + crashProgress * 3.0;
                double px = Math.cos(basePillarAngle) * splayRadius;
                double pz = Math.sin(basePillarAngle) * splayRadius;

                for (int seg = 0; seg < CHAINS_PER_PILLAR; seg++) {
                    float y = currentCrashHeight + seg * 1.5f * (1.0f - crashProgress * 0.5f);
                    if (y < 0.2f) y = 0.2f;
                    Location loc = c.clone().add(px, y, pz);
                    pillars.get(p).get(seg).entity().teleport(loc);
                }
            }

            // Core crashes straight down
            for (int i = 0; i < coreBlocks.size(); i++) {
                float y = currentCrashHeight + 2.0f - i * 0.3f * crashProgress;
                if (y < 0.3f) y = 0.3f;
                Location loc = c.clone().add(0, y, 0);
                coreBlocks.get(i).entity().teleport(loc);
                coreBlocks.get(i).rotate(structureAngle * 3, 0, 1, 0);
            }

            positionConnectors(c, currentCrashHeight, structureAngle);

            // Falling debris particles
            for (int i = 0; i < 12; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 6;
                Location debrisLoc = c.clone().add(Math.cos(angle) * r, Math.random() * currentCrashHeight, Math.sin(angle) * r);
                DisplayBuilder.dustParticles(debrisLoc, 2, 0.5, 180, 100, 40, 1.0f);
            }

            // IMPACT when hitting ground
            if (currentCrashHeight <= 1.0f && !crashImpacted) {
                crashImpacted = true;

                // MASSIVE impact
                triggerImpactDamage(c);

                // Epic shockwave
                for (float radius = 2.0f; radius <= 8.0f; radius += 1.5f) {
                    DisplayBuilder.particleRing(c, radius, Particle.DUST, 32,
                            new Particle.DustOptions(Color.fromRGB(180, 180, 190), 2.0f));
                }

                // Explosion of particles
                w.spawnParticle(Particle.BLOCK, c, 80, 4, 2, 4, 0.5,
                        Material.IRON_BLOCK.createBlockData());
                w.spawnParticle(Particle.BLOCK, c, 60, 3, 1.5, 3, 0.4,
                        Material.DEEPSLATE.createBlockData());
                w.spawnParticle(Particle.CRIT, c.clone().add(0, 1, 0), 50, 3, 2, 3, 0.5);
                w.spawnParticle(Particle.FLASH, c.clone().add(0, 2, 0), 3, 0, 0, 0, 0);

                // Ground crater ring of rust particles
                DisplayBuilder.particleRing(c, 6.0, Particle.DUST, 48,
                        new Particle.DustOptions(Color.fromRGB(180, 100, 40), 2.0f));

                // MASSIVE sound
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.2f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
            }

            // Crash screech
            if (phaseTimer % 4 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, currentCrashHeight, 0), Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.4f + crashProgress);
            }
        }

        private void aftermathPhase(Location c, World w, int ticksAlive) {
            // Debris settles — slight sinking and scattered smoke
            if (phaseTimer < 60) {
                float sinkAmount = phaseTimer * 0.005f;

                for (int p = 0; p < PILLAR_COUNT; p++) {
                    for (int seg = 0; seg < CHAINS_PER_PILLAR; seg++) {
                        BlockDisplay entity = pillars.get(p).get(seg).entity();
                        Location current = entity.getLocation();
                        current.setY(Math.max(c.getY() - 0.5, current.getY() - 0.01));
                        entity.teleport(current);
                    }
                }

                // Settling dust and smoke
                if (phaseTimer % 5 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double r = Math.random() * 7.0;
                        Location smokeLoc = c.clone().add(Math.cos(angle) * r, 0.3 + Math.random() * 2, Math.sin(angle) * r);
                        w.spawnParticle(Particle.SMOKE, smokeLoc, 3, 0.5, 0.3, 0.5, 0.02);
                        DisplayBuilder.dustParticles(smokeLoc, 3, 0.6, 100, 100, 110, 1.0f);
                    }
                }

                // Aftermath metal groaning
                if (phaseTimer % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.3f);
                }

                // Rust particles from wreckage
                if (phaseTimer % 8 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double r = Math.random() * 5.0;
                        Location rustLoc = c.clone().add(Math.cos(angle) * r, 0.2, Math.sin(angle) * r);
                        DisplayBuilder.dustParticles(rustLoc, 4, 0.3, 180, 100, 40, 0.8f);
                    }
                }
            }
        }

        /**
         * Position the 8 connecting chains between pillars at the given height offset and rotation.
         */
        private void positionConnectors(Location c, float heightOffset, float rotationAngle) {
            int connIdx = 0;
            for (int p = 0; p < PILLAR_COUNT; p++) {
                int nextP = (p + 1) % PILLAR_COUNT;
                double angle1 = (2 * Math.PI * p) / PILLAR_COUNT + rotationAngle;
                double angle2 = (2 * Math.PI * nextP) / PILLAR_COUNT + rotationAngle;
                double x1 = Math.cos(angle1) * 4.0;
                double z1 = Math.sin(angle1) * 4.0;
                double x2 = Math.cos(angle2) * 4.0;
                double z2 = Math.sin(angle2) * 4.0;

                // Two connectors per pillar pair: bottom and top
                for (int h = 0; h < 2; h++) {
                    if (connIdx >= connectors.size()) break;
                    float connY = heightOffset + 1.0f + h * 3.5f;
                    double midX = (x1 + x2) / 2;
                    double midZ = (z1 + z2) / 2;
                    Location connLoc = c.clone().add(midX, connY, midZ);
                    connectors.get(connIdx).entity().teleport(connLoc);

                    // Rotate to face between the two pillars
                    float faceAngle = (float) Math.atan2(z2 - z1, x2 - x1);
                    connectors.get(connIdx).rotate(faceAngle, 0, 1, 0);

                    // Scale to span the distance
                    float dist = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
                    connectors.get(connIdx).scale(0.25f, 0.25f, dist * 0.9f);

                    connIdx++;
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainApocalypse(plugin); }
    }
}
