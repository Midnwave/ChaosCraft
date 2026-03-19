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
 * Chain Mode — CHAIN RAIN ATTACKS
 * 15 chain-themed BlockDisplay attacks that rain/fall/sweep through the arena.
 * Each creates dramatic chain structures with metallic particles and sounds.
 *
 * Color palette:
 * - Iron gray: RGB(180, 180, 190)
 * - Dark iron: RGB(100, 100, 110)
 * - Rust orange: RGB(180, 100, 40)
 * - Chain glow: RGB(200, 200, 220)
 * - Netherite dark: RGB(60, 50, 50)
 */
public final class ChainRains {

    private ChainRains() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainDownpour(plugin));
        registry.register(new IronCurtain(plugin));
        registry.register(new ChainHailstorm(plugin));
        registry.register(new SpikedChainRain(plugin));
        registry.register(new DiagonalChainSlash(plugin));
        registry.register(new ChainTornadoDownpour(plugin));
        registry.register(new RustStorm(plugin));
        registry.register(new ChainMeteorShower(plugin));
        registry.register(new PendulumRain(plugin));
        registry.register(new ChainNetDrop(plugin));
        registry.register(new ChainCascade(plugin));
        registry.register(new ChainBlizzard(plugin));
        registry.register(new ScatteredShackleRain(plugin));
        registry.register(new WeightedChainFall(plugin));
        registry.register(new ChainLightningRain(plugin));
    }

    // ================================================================
    // 1. CHAIN DOWNPOUR — 15 chain blocks fall from 20 blocks above
    //    in a 5-block radius, staggered spawn, shockwave on impact
    // ================================================================
    public static class ChainDownpour extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Float> currentYPositions = new ArrayList<>();
        private final List<Double> offsetsX = new ArrayList<>();
        private final List<Double> offsetsZ = new ArrayList<>();
        private final List<Integer> spawnDelays = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();
        private static final float START_Y = 20.0f;
        private static final int CHAIN_COUNT = 15;

        public ChainDownpour(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_downpour", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setTracksPlayer(true); // Rain follows the player
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 5.0;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                offsetsX.add(ox);
                offsetsZ.add(oz);
                fallSpeeds.add(0.15f + (float) (Math.random() * 0.25));
                currentYPositions.add(START_Y);
                spawnDelays.add(i * 4);
                impacted.add(false);
                chains.add(null); // placeholder until spawned
            }

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < CHAIN_COUNT; i++) {
                if (ticksAlive < spawnDelays.get(i)) continue;
                if (impacted.get(i)) continue;

                // Spawn chain on its delay tick
                if (chains.get(i) == null) {
                    Location spawnLoc = c.clone().add(offsetsX.get(i), START_Y, offsetsZ.get(i));
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(spawnLoc, Material.CHAIN);
                    handle.scale(1.5f, 4.5f, 1.5f)
                          .glow(200, 200, 220)
                          .interpolation(2, 0);
                    spawnedEntities.add(handle.entity());
                    chains.set(i, handle);
                    DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.8f + (float)(Math.random() * 0.4));
                }

                // Fall
                float y = currentYPositions.get(i) - fallSpeeds.get(i);
                currentYPositions.set(i, y);

                BlockDisplayHandle chain = chains.get(i);
                Location newLoc = c.clone().add(offsetsX.get(i), y, offsetsZ.get(i));
                chain.entity().teleport(newLoc);

                // Trailing particles while falling
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(newLoc.clone().add(0, 1.5, 0), 3, 0.2, 180, 180, 190, 1.0f);
                }

                // Ground impact
                if (y <= 0) {
                    currentYPositions.set(i, 0f);
                    impacted.set(i, true);

                    Location impactLoc = c.clone().add(offsetsX.get(i), 0, offsetsZ.get(i));
                    triggerImpactDamage(impactLoc);

                    // Shockwave ring of iron particles
                    DisplayBuilder.particleRing(impactLoc, 3.0, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(180, 180, 190), 1.5f));
                    w.spawnParticle(Particle.BLOCK, impactLoc, 30, 1.5, 0.3, 1.5, 0.2,
                            Material.IRON_BLOCK.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);
                }
            }

            // Ambient chain rattle
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainDownpour(plugin); }
    }

    // ================================================================
    // 2. IRON CURTAIN — Wall of 12 chains hanging from a horizontal
    //    iron bar, sweeps forward 10 blocks over 3 seconds
    // ================================================================
    public static class IronCurtain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> barSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> hangingChains = new ArrayList<>();
        private float sweepProgress = 0;
        private static final float SWEEP_DISTANCE = 10.0f;
        private static final int SWEEP_TICKS = 60; // 3 seconds
        private static final float BAR_WIDTH = 6.0f;
        private static final int CHAIN_COUNT = 12;

        public IronCurtain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_curtain", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location barStart = center.clone().add(-BAR_WIDTH / 2, 5, -SWEEP_DISTANCE / 2);
            Location barEnd = center.clone().add(BAR_WIDTH / 2, 5, -SWEEP_DISTANCE / 2);

            // Horizontal iron bar (3 segments)
            for (int i = 0; i < 3; i++) {
                double x = -BAR_WIDTH / 2 + (BAR_WIDTH / 2.0) * i;
                Location seg = center.clone().add(x, 5, -SWEEP_DISTANCE / 2);
                BlockDisplayHandle bar = displayBuilder.spawnBlock(seg, Material.IRON_BLOCK);
                bar.scale(3.3f, 0.6f, 0.6f)
                   .glow(180, 180, 190)
                   .interpolation(2, 0);
                barSegments.add(bar);
                spawnedEntities.add(bar.entity());
            }

            // Hanging chains
            for (int i = 0; i < CHAIN_COUNT; i++) {
                double x = -BAR_WIDTH / 2 + (BAR_WIDTH / (CHAIN_COUNT - 1.0)) * i;
                Location chainLoc = center.clone().add(x, 2.5, -SWEEP_DISTANCE / 2);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                float chainLen = 3.0f + (float)(Math.sin(i * 0.8) * 1.0);
                chain.scale(0.45f, chainLen * 1.5f, 0.45f)
                     .glow(200, 200, 220)
                     .interpolation(3, 0);
                hangingChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive > SWEEP_TICKS) return;

            sweepProgress = (float) ticksAlive / SWEEP_TICKS;
            float zOffset = -SWEEP_DISTANCE / 2 + sweepProgress * SWEEP_DISTANCE;

            // Move bar segments
            for (int i = 0; i < barSegments.size(); i++) {
                double x = -BAR_WIDTH / 2 + (BAR_WIDTH / 2.0) * i;
                barSegments.get(i).entity().teleport(c.clone().add(x, 5, zOffset));
            }

            // Move chains with sway
            for (int i = 0; i < hangingChains.size(); i++) {
                double x = -BAR_WIDTH / 2 + (BAR_WIDTH / (CHAIN_COUNT - 1.0)) * i;
                float sway = (float) Math.sin(ticksAlive * 0.15 + i * 0.5) * 0.3f;
                Location chainLoc = c.clone().add(x + sway, 2.5, zOffset);
                hangingChains.get(i).entity().teleport(chainLoc);

                // Chain sway rotation
                float swayAngle = (float) Math.sin(ticksAlive * 0.15 + i * 0.5) * 0.15f;
                hangingChains.get(i).rotate(swayAngle, 0, 0, 1);
            }

            // Damage zone particles along the curtain
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 6; i++) {
                    double x = c.getX() + (-BAR_WIDTH / 2 + Math.random() * BAR_WIDTH);
                    Location dustLoc = new Location(w, x, c.getY() + 1 + Math.random() * 3, c.getZ() + zOffset);
                    DisplayBuilder.dustParticles(dustLoc, 2, 0.3, 100, 100, 110, 1.2f);
                }
            }

            // Metallic scraping sounds
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 3, zOffset), Sound.BLOCK_CHAIN_BREAK, 0.7f, 0.7f);
            }

            // Spark particles at the leading edge
            if (ticksAlive % 2 == 0) {
                Location sparkLoc = c.clone().add(Math.random() * BAR_WIDTH - BAR_WIDTH / 2, 0.5, zOffset);
                w.spawnParticle(Particle.CRIT, sparkLoc, 3, 0.3, 0.3, 0.1, 0.05);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IronCurtain(plugin); }
    }

    // ================================================================
    // 3. CHAIN HAILSTORM — 20 small chain fragments rain down rapidly
    //    in a 6-block radius, spinning as they fall
    // ================================================================
    public static class ChainHailstorm extends BlockDisplayAttack {

        private static final int FRAGMENT_COUNT = 20;
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Float> spinSpeeds = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();

        public ChainHailstorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_hailstorm", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < FRAGMENT_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 6.0;
                xOffsets.add(Math.cos(angle) * dist);
                zOffsets.add(Math.sin(angle) * dist);
                yPositions.add(15.0f + (float)(Math.random() * 8.0));
                fallSpeeds.add(0.4f + (float)(Math.random() * 0.4));
                spinSpeeds.add(0.2f + (float)(Math.random() * 0.3));
                delays.add((int)(Math.random() * 30));
                landed.add(false);
                fragments.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, 15, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < FRAGMENT_COUNT; i++) {
                if (ticksAlive < delays.get(i)) continue;
                if (landed.get(i)) continue;

                // Spawn fragment on delay
                if (fragments.get(i) == null) {
                    Location spawnLoc = c.clone().add(xOffsets.get(i), yPositions.get(i), zOffsets.get(i));
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(spawnLoc, Material.CHAIN);
                    frag.scale(0.45f, 0.45f, 0.45f)
                        .glow(180, 180, 190)
                        .interpolation(1, 0);
                    fragments.set(i, frag);
                    spawnedEntities.add(frag.entity());
                }

                // Fall and spin
                float y = yPositions.get(i) - fallSpeeds.get(i);
                yPositions.set(i, y);

                BlockDisplayHandle frag = fragments.get(i);
                Location newLoc = c.clone().add(xOffsets.get(i), y, zOffsets.get(i));
                frag.entity().teleport(newLoc);

                // Spin animation
                float spinAngle = ticksAlive * spinSpeeds.get(i);
                frag.rotate(spinAngle, 1, 0.5f, 0);

                // Impact
                if (y <= 0) {
                    yPositions.set(i, 0f);
                    landed.set(i, true);

                    Location impactLoc = c.clone().add(xOffsets.get(i), 0, zOffsets.get(i));
                    triggerImpactDamage(impactLoc);

                    // Impact sparks
                    w.spawnParticle(Particle.CRIT, impactLoc, 8, 0.3, 0.1, 0.3, 0.15);
                    DisplayBuilder.dustParticles(impactLoc, 5, 0.5, 180, 180, 190, 0.8f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_CHAIN_BREAK, 0.5f, 1.5f);
                }
            }

            // Chaotic ambient sound
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 8, 0), Sound.BLOCK_CHAIN_PLACE, 0.4f, 1.2f + (float)(Math.random() * 0.6));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainHailstorm(plugin); }
    }

    // ================================================================
    // 4. SPIKED CHAIN RAIN — 10 chains with netherite tips fall at
    //    45-degree angles, embed in ground on impact
    // ================================================================
    public static class SpikedChainRain extends BlockDisplayAttack {

        private static final int SPIKE_COUNT = 10;
        private final List<BlockDisplayHandle> chainBodies = new ArrayList<>();
        private final List<BlockDisplayHandle> netheriteTips = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Double> driftX = new ArrayList<>();
        private final List<Double> driftZ = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> embedded = new ArrayList<>();
        private static final float START_Y = 18.0f;
        private static final float FALL_SPEED = 0.35f;

        public SpikedChainRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spiked_chain_rain", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(68.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SPIKE_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 5.0;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                xOffsets.add(ox);
                zOffsets.add(oz);
                yPositions.add(START_Y + (float)(Math.random() * 5));

                // Alternating diagonal directions (45 degrees)
                double dir = (i % 2 == 0) ? 1.0 : -1.0;
                driftX.add(dir * (0.15 + Math.random() * 0.1));
                driftZ.add((Math.random() - 0.5) * 0.1);

                delays.add(i * 6);
                embedded.add(false);
                chainBodies.add(null);
                netheriteTips.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < SPIKE_COUNT; i++) {
                if (ticksAlive < delays.get(i)) continue;
                if (embedded.get(i)) continue;

                // Spawn on delay
                if (chainBodies.get(i) == null) {
                    Location spawnLoc = c.clone().add(xOffsets.get(i), yPositions.get(i), zOffsets.get(i));
                    BlockDisplayHandle body = displayBuilder.spawnBlock(spawnLoc, Material.CHAIN);
                    body.scale(0.6f, 6.0f, 0.6f)
                        .glow(200, 200, 220)
                        .interpolation(2, 0);
                    // Tilt 45 degrees in drift direction
                    float tiltAngle = (float)(Math.PI / 4) * (i % 2 == 0 ? 1 : -1);
                    body.rotate(tiltAngle, 0, 0, 1);
                    chainBodies.set(i, body);
                    spawnedEntities.add(body.entity());

                    Location tipLoc = spawnLoc.clone().add(0, -2, 0);
                    BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.NETHERITE_BLOCK);
                    tip.scale(0.75f, 1.2f, 0.75f)
                       .glow(60, 50, 50)
                       .interpolation(2, 0);
                    netheriteTips.set(i, tip);
                    spawnedEntities.add(tip.entity());

                    DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.9f);
                }

                // Fall with diagonal drift
                float y = yPositions.get(i) - FALL_SPEED;
                double ox = xOffsets.get(i) + driftX.get(i);
                double oz = zOffsets.get(i) + driftZ.get(i);
                yPositions.set(i, y);
                xOffsets.set(i, ox);
                zOffsets.set(i, oz);

                chainBodies.get(i).entity().teleport(c.clone().add(ox, y, oz));
                netheriteTips.get(i).entity().teleport(c.clone().add(ox, y - 2, oz));

                // Rust-colored dust trail
                if (ticksAlive % 2 == 0) {
                    Location trailLoc = c.clone().add(ox, y + 2, oz);
                    DisplayBuilder.dustParticles(trailLoc, 4, 0.3, 180, 100, 40, 1.0f);
                }

                // Ground impact — embed in ground
                if (y <= 0) {
                    yPositions.set(i, 0f);
                    embedded.set(i, true);

                    Location impactLoc = c.clone().add(ox, 0, oz);
                    triggerImpactDamage(impactLoc);

                    // Embed chain at ground level (stays visible)
                    chainBodies.get(i).entity().teleport(c.clone().add(ox, -1, oz));
                    netheriteTips.get(i).entity().teleport(c.clone().add(ox, -1.5, oz));

                    // Impact effects
                    w.spawnParticle(Particle.BLOCK, impactLoc, 40, 1, 0.5, 1, 0.3,
                            Material.IRON_BLOCK.createBlockData());
                    DisplayBuilder.dustParticles(impactLoc, 10, 1.5, 180, 100, 40, 1.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpikedChainRain(plugin); }
    }

    // ================================================================
    // 5. DIAGONAL CHAIN SLASH — 12 chains arranged in a diagonal line
    //    slash across the area from upper-left to lower-right
    // ================================================================
    public static class DiagonalChainSlash extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 12;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private float slashProgress = 0;
        private static final float SLASH_DURATION = 40f; // 2 seconds
        private static final float SLASH_RANGE = 14.0f;

        public DiagonalChainSlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("diagonal_chain_slash", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Create 12 chains in a diagonal line formation
            for (int i = 0; i < CHAIN_COUNT; i++) {
                float t = (float) i / (CHAIN_COUNT - 1);
                double startX = -SLASH_RANGE / 2 + t * SLASH_RANGE;
                double startY = 12 - t * 10;
                double startZ = -SLASH_RANGE / 2 + t * SLASH_RANGE;

                Location chainLoc = center.clone().add(startX, startY, startZ);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                chain.scale(0.75f, 3.0f, 0.75f)
                     .glow(200, 200, 220)
                     .interpolation(2, 0);

                // Diagonal tilt
                chain.rotate((float)(Math.PI / 4), 0, 0, 1);

                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            // Add deepslate accent blocks at the ends
            Location topEnd = center.clone().add(-SLASH_RANGE / 2, 12, -SLASH_RANGE / 2);
            BlockDisplayHandle topCap = displayBuilder.spawnBlock(topEnd, Material.DEEPSLATE);
            topCap.scale(1.5f, 1.5f, 1.5f).glow(100, 100, 110);
            spawnedEntities.add(topCap.entity());

            Location botEnd = center.clone().add(SLASH_RANGE / 2, 2, SLASH_RANGE / 2);
            BlockDisplayHandle botCap = displayBuilder.spawnBlock(botEnd, Material.DEEPSLATE);
            botCap.scale(1.5f, 1.5f, 1.5f).glow(100, 100, 110);
            spawnedEntities.add(botCap.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Calculate sweep phase — slash repeats
            float cycleTime = ticksAlive % (SLASH_DURATION * 2);
            boolean forward = cycleTime < SLASH_DURATION;
            float progress = forward ? cycleTime / SLASH_DURATION : (SLASH_DURATION * 2 - cycleTime) / SLASH_DURATION;

            float sweepOffset = (progress - 0.5f) * SLASH_RANGE;

            for (int i = 0; i < chains.size(); i++) {
                float t = (float) i / (CHAIN_COUNT - 1);
                double baseX = -SLASH_RANGE / 2 + t * SLASH_RANGE;
                double baseY = 12 - t * 10;
                double baseZ = -SLASH_RANGE / 2 + t * SLASH_RANGE;

                // Sweep offset moves the whole formation
                double sx = baseX + sweepOffset * 0.5;
                double sy = baseY;
                double sz = baseZ + sweepOffset * 0.5;

                chains.get(i).entity().teleport(c.clone().add(sx, sy, sz));

                // Rotation animation during slash
                float rotAngle = (float)(Math.PI / 4) + ticksAlive * 0.08f;
                chains.get(i).rotate(rotAngle, 0, 0, 1);
            }

            // Metallic spark particles at the leading edge
            if (ticksAlive % 2 == 0) {
                int leadIdx = forward ? CHAIN_COUNT - 1 : 0;
                Location sparkLoc = chains.get(leadIdx).entity().getLocation();
                w.spawnParticle(Particle.CRIT, sparkLoc, 5, 0.3, 0.3, 0.3, 0.1);
                DisplayBuilder.dustParticles(sparkLoc, 3, 0.4, 200, 200, 220, 1.0f);
            }

            // Slash sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.9f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DiagonalChainSlash(plugin); }
    }

    // ================================================================
    // 6. CHAIN TORNADO DOWNPOUR — 16 chains orbit in a tightening
    //    spiral as they descend from 15 blocks up
    // ================================================================
    public static class ChainTornadoDownpour extends BlockDisplayAttack {

        private static final int CHAIN_COUNT = 16;
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> angleOffsets = new ArrayList<>();
        private float currentRadius = 8.0f;
        private float currentHeight = 15.0f;
        private static final float MIN_RADIUS = 2.0f;
        private static final float DESCENT_SPEED = 0.06f;
        private static final float SHRINK_SPEED = 0.025f;
        private static final float ORBIT_SPEED = 0.08f;

        public ChainTornadoDownpour(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_tornado_downpour", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(66.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float angle = (float)(2 * Math.PI * i / CHAIN_COUNT);
                angleOffsets.add(angle);
                float heightOffset = (float)(i % 4) * 1.5f;
                yPositions.add(15.0f + heightOffset);

                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;
                Location chainLoc = center.clone().add(x, 15.0 + heightOffset, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                chain.scale(0.75f, 3.75f, 0.75f)
                     .glow(200, 200, 220)
                     .interpolation(2, 0);
                chains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            // Center column accent
            BlockDisplayHandle core = displayBuilder.spawnBlock(center.clone().add(0, 15, 0), Material.IRON_BLOCK);
            core.scale(0.9f, 0.9f, 0.9f).glow(180, 180, 190);
            spawnedEntities.add(core.entity());

            DisplayBuilder.playSound(center.clone().add(0, 15, 0), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Shrink radius and descend
            currentRadius = Math.max(MIN_RADIUS, 8.0f - ticksAlive * SHRINK_SPEED);
            currentHeight = Math.max(0, 15.0f - ticksAlive * DESCENT_SPEED);

            boolean allLanded = currentHeight <= 0;

            for (int i = 0; i < chains.size(); i++) {
                float angle = angleOffsets.get(i) + ticksAlive * ORBIT_SPEED;
                float heightOffset = (float)(i % 4) * 1.5f * (currentHeight / 15.0f);
                float y = currentHeight + heightOffset;

                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;

                Location newLoc = c.clone().add(x, y, z);
                chains.get(i).entity().teleport(newLoc);

                // Rotation as they orbit
                chains.get(i).rotate(angle * 0.5f, 0, 1, 0);

                // Dust trail along spiral path
                if (ticksAlive % 3 == 0 && i % 4 == 0) {
                    DisplayBuilder.dustParticles(newLoc, 3, 0.4, 180, 180, 190, 1.0f);
                }
            }

            // Wind whoosh sounds
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, currentHeight, 0), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 0.5f);
            }

            // Spiral dust particles on the ground
            if (ticksAlive % 4 == 0) {
                double pAngle = ticksAlive * ORBIT_SPEED * 2;
                Location dustLoc = c.clone().add(Math.cos(pAngle) * currentRadius, 0.5, Math.sin(pAngle) * currentRadius);
                DisplayBuilder.dustParticles(dustLoc, 5, 0.5, 100, 100, 110, 1.3f);
            }

            // Impact when reaching ground
            if (allLanded) {
                triggerImpactDamage(c);
                // Massive shockwave
                DisplayBuilder.particleRing(c, 5.0, Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(180, 180, 190), 2.0f));
                w.spawnParticle(Particle.BLOCK, c, 80, 3, 0.5, 3, 0.3,
                        Material.IRON_BLOCK.createBlockData());
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainTornadoDownpour(plugin); }
    }

    // ================================================================
    // 7. RUST STORM — 18 iron/chain blocks swirl in a chaotic cloud
    //    at player height, tumbling and rotating randomly
    // ================================================================
    public static class RustStorm extends BlockDisplayAttack {

        private static final int BLOCK_COUNT = 18;
        private final List<BlockDisplayHandle> stormBlocks = new ArrayList<>();
        private final List<Float> orbitAngles = new ArrayList<>();
        private final List<Float> orbitRadii = new ArrayList<>();
        private final List<Float> orbitSpeeds = new ArrayList<>();
        private final List<Float> yBobPhases = new ArrayList<>();
        private final List<Float> tumbleSpeeds = new ArrayList<>();
        private float stormDriftX = 0;
        private float stormDriftZ = 0;
        private float driftAngle = 0;

        public RustStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rust_storm", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] materials = {
                Material.CHAIN, Material.CHAIN, Material.CHAIN,
                Material.IRON_BLOCK, Material.IRON_BLOCK,
                Material.CHAIN, Material.CHAIN,
                Material.IRON_BLOCK, Material.CHAIN,
                Material.CHAIN, Material.IRON_BLOCK, Material.CHAIN,
                Material.CHAIN, Material.CHAIN, Material.IRON_BLOCK,
                Material.CHAIN, Material.CHAIN, Material.IRON_BLOCK
            };

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float angle = (float)(Math.random() * Math.PI * 2);
                float radius = 1.0f + (float)(Math.random() * 3.0);
                float speed = 0.04f + (float)(Math.random() * 0.06);

                orbitAngles.add(angle);
                orbitRadii.add(radius);
                orbitSpeeds.add(speed);
                yBobPhases.add((float)(Math.random() * Math.PI * 2));
                tumbleSpeeds.add(0.1f + (float)(Math.random() * 0.15));

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                float yOffset = 1.5f + (float)(Math.random() * 2.0);

                Location blockLoc = center.clone().add(x, yOffset, z);
                BlockDisplayHandle block = displayBuilder.spawnBlock(blockLoc, materials[i]);
                float scale = (materials[i] == Material.IRON_BLOCK) ? 0.9f : 0.6f;
                block.scale(scale, scale, scale)
                     .glow(180, 100, 40) // Rust orange
                     .interpolation(2, 0);
                stormBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow storm drift
            driftAngle += 0.005f;
            stormDriftX = (float) Math.cos(driftAngle) * 0.03f;
            stormDriftZ = (float) Math.sin(driftAngle) * 0.03f;

            Location driftCenter = c.clone().add(
                Math.cos(driftAngle) * ticksAlive * 0.015,
                0,
                Math.sin(driftAngle) * ticksAlive * 0.015
            );

            for (int i = 0; i < stormBlocks.size(); i++) {
                float angle = orbitAngles.get(i) + ticksAlive * orbitSpeeds.get(i);
                float radius = orbitRadii.get(i);
                float yBob = (float) Math.sin(ticksAlive * 0.08 + yBobPhases.get(i)) * 0.8f;

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                float y = 2.0f + yBob;

                Location newLoc = driftCenter.clone().add(x, y, z);
                stormBlocks.get(i).entity().teleport(newLoc);

                // Random tumbling rotation
                float tumble = ticksAlive * tumbleSpeeds.get(i);
                stormBlocks.get(i).rotate(tumble, 1, 0.7f, 0.3f);
            }

            // Rust-colored particle cloud
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    Location dustLoc = driftCenter.clone().add(
                        (Math.random() - 0.5) * 6,
                        1.5 + Math.random() * 2,
                        (Math.random() - 0.5) * 6
                    );
                    DisplayBuilder.dustParticles(dustLoc, 3, 0.5, 180, 100, 40, 1.2f);
                }
            }

            // Dark iron dust accents
            if (ticksAlive % 5 == 0) {
                Location darkDust = driftCenter.clone().add(
                    (Math.random() - 0.5) * 4, 2.5, (Math.random() - 0.5) * 4);
                DisplayBuilder.dustParticles(darkDust, 4, 0.6, 100, 100, 110, 1.5f);
            }

            // Storm sounds
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(driftCenter, Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.8f + (float)(Math.random() * 0.4));
            }
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(driftCenter, Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RustStorm(plugin); }
    }

    // ================================================================
    // 8. CHAIN METEOR SHOWER — 8 large chain-ball meteors (3x3x3
    //    cluster) fall from 25 blocks, fire trails, massive impact
    // ================================================================
    public static class ChainMeteorShower extends BlockDisplayAttack {

        private static final int METEOR_COUNT = 8;
        private final List<List<BlockDisplayHandle>> meteors = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();
        private static final float START_Y = 25.0f;
        private static final float FALL_SPEED = 0.5f;
        private static final int SPAWN_INTERVAL = 10;

        public ChainMeteorShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_meteor_shower", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(75.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < METEOR_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 2 + Math.random() * 6;
                xOffsets.add(Math.cos(angle) * dist);
                zOffsets.add(Math.sin(angle) * dist);
                yPositions.add(START_Y);
                impacted.add(false);
                meteors.add(new ArrayList<>());
            }

            DisplayBuilder.playSound(center.clone().add(0, 25, 0), Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < METEOR_COUNT; i++) {
                if (impacted.get(i)) continue;

                int spawnTick = i * SPAWN_INTERVAL;
                if (ticksAlive < spawnTick) continue;

                // Spawn meteor cluster on its tick
                if (meteors.get(i).isEmpty()) {
                    Location meteorCenter = c.clone().add(xOffsets.get(i), START_Y, zOffsets.get(i));

                    // 3x3x3 cluster of chains (27 blocks but we use key positions = 11 blocks for performance)
                    double[][] clusterOffsets = {
                        {0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                        {0, 1, 0}, {0, -1, 0}, {1, 1, 0}, {-1, -1, 0},
                        {0, 1, 1}, {0, -1, -1}
                    };

                    Material[] mats = {
                        Material.CHAIN, Material.IRON_BLOCK, Material.CHAIN,
                        Material.CHAIN, Material.CHAIN, Material.IRON_BLOCK,
                        Material.CHAIN, Material.CHAIN, Material.CHAIN,
                        Material.IRON_BLOCK, Material.CHAIN
                    };

                    List<BlockDisplayHandle> cluster = meteors.get(i);
                    for (int j = 0; j < clusterOffsets.length; j++) {
                        Location blockLoc = meteorCenter.clone().add(
                            clusterOffsets[j][0] * 0.8, clusterOffsets[j][1] * 0.8, clusterOffsets[j][2] * 0.8);
                        BlockDisplayHandle block = displayBuilder.spawnBlock(blockLoc, mats[j]);
                        block.scale(1.35f, 1.35f, 1.35f)
                             .glow(180, 180, 190)
                             .interpolation(2, 0);
                        cluster.add(block);
                        spawnedEntities.add(block.entity());
                    }

                    DisplayBuilder.playSound(meteorCenter, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
                }

                // Fall
                float y = yPositions.get(i) - FALL_SPEED;
                yPositions.set(i, y);

                List<BlockDisplayHandle> cluster = meteors.get(i);
                double[][] clusterOffsets = {
                    {0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                    {0, 1, 0}, {0, -1, 0}, {1, 1, 0}, {-1, -1, 0},
                    {0, 1, 1}, {0, -1, -1}
                };

                for (int j = 0; j < cluster.size(); j++) {
                    Location blockLoc = c.clone().add(
                        xOffsets.get(i) + clusterOffsets[j][0] * 0.8,
                        y + clusterOffsets[j][1] * 0.8,
                        zOffsets.get(i) + clusterOffsets[j][2] * 0.8
                    );
                    cluster.get(j).entity().teleport(blockLoc);
                }

                // Rotation of the whole cluster
                float rot = (ticksAlive - i * SPAWN_INTERVAL) * 0.05f;
                for (BlockDisplayHandle block : cluster) {
                    block.rotate(rot, 1, 0.5f, 0.3f);
                }

                // Fire particle trail
                if (ticksAlive % 2 == 0) {
                    Location trailLoc = c.clone().add(xOffsets.get(i), y + 2, zOffsets.get(i));
                    w.spawnParticle(Particle.FLAME, trailLoc, 6, 0.5, 0.8, 0.5, 0.02);
                    w.spawnParticle(Particle.SMOKE, trailLoc, 4, 0.4, 0.6, 0.4, 0.02);
                }

                // Impact
                if (y <= 0) {
                    yPositions.set(i, 0f);
                    impacted.set(i, true);

                    Location impactLoc = c.clone().add(xOffsets.get(i), 0, zOffsets.get(i));
                    triggerImpactDamage(impactLoc);

                    // Massive explosion of chain particles
                    w.spawnParticle(Particle.BLOCK, impactLoc, 100, 3, 1, 3, 0.5,
                            Material.CHAIN.createBlockData());
                    w.spawnParticle(Particle.BLOCK, impactLoc, 60, 2, 0.5, 2, 0.3,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.EXPLOSION, impactLoc, 3, 1, 0.5, 1, 0);
                    DisplayBuilder.particleRing(impactLoc, 4.0, Particle.DUST, 36,
                            new Particle.DustOptions(Color.fromRGB(180, 180, 190), 2.0f));

                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.4f);

                    // Remove meteor blocks on impact
                    for (BlockDisplayHandle block : cluster) {
                        block.entity().remove();
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainMeteorShower(plugin); }
    }

    // ================================================================
    // 9. PENDULUM RAIN — 10 chains swing like pendulums from invisible
    //    anchor points 12 blocks up, different phase offsets
    // ================================================================
    public static class PendulumRain extends BlockDisplayAttack {

        private static final int PENDULUM_COUNT = 10;
        private final List<BlockDisplayHandle> pendulumChains = new ArrayList<>();
        private final List<BlockDisplayHandle> pendulumWeights = new ArrayList<>();
        private final List<Double> anchorX = new ArrayList<>();
        private final List<Double> anchorZ = new ArrayList<>();
        private final List<Float> phaseOffsets = new ArrayList<>();
        private final List<Float> swingAmplitudes = new ArrayList<>();
        private static final float ANCHOR_HEIGHT = 12.0f;
        private static final float CHAIN_LENGTH = 8.0f;

        public PendulumRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pendulum_rain", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < PENDULUM_COUNT; i++) {
                double ax = (i - PENDULUM_COUNT / 2.0) * 2.5;
                double az = (Math.random() - 0.5) * 6;
                anchorX.add(ax);
                anchorZ.add(az);
                phaseOffsets.add((float)(i * Math.PI / PENDULUM_COUNT * 2));
                swingAmplitudes.add(0.8f + (float)(Math.random() * 0.4));

                // Chain body (vertical initially)
                Location chainLoc = center.clone().add(ax, ANCHOR_HEIGHT - CHAIN_LENGTH / 2, az);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                chain.scale(0.45f, CHAIN_LENGTH * 1.5f, 0.45f)
                     .glow(200, 200, 220)
                     .interpolation(3, 0);
                pendulumChains.add(chain);
                spawnedEntities.add(chain.entity());

                // Weight at the bottom
                Location weightLoc = center.clone().add(ax, ANCHOR_HEIGHT - CHAIN_LENGTH, az);
                Material weightMat = (i % 3 == 0) ? Material.ANVIL : Material.IRON_BLOCK;
                BlockDisplayHandle weight = displayBuilder.spawnBlock(weightLoc, weightMat);
                weight.scale(1.2f, 1.2f, 1.2f)
                      .glow(100, 100, 110)
                      .interpolation(3, 0);
                pendulumWeights.add(weight);
                spawnedEntities.add(weight.entity());
            }

            // Anchor bar accents
            BlockDisplayHandle leftAnchor = displayBuilder.spawnBlock(
                center.clone().add(-PENDULUM_COUNT, ANCHOR_HEIGHT, 0), Material.DEEPSLATE);
            leftAnchor.scale(0.75f, 0.75f, 0.75f).glow(60, 50, 50);
            spawnedEntities.add(leftAnchor.entity());

            BlockDisplayHandle rightAnchor = displayBuilder.spawnBlock(
                center.clone().add(PENDULUM_COUNT, ANCHOR_HEIGHT, 0), Material.DEEPSLATE);
            rightAnchor.scale(0.75f, 0.75f, 0.75f).glow(60, 50, 50);
            spawnedEntities.add(rightAnchor.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < PENDULUM_COUNT; i++) {
                // Pendulum swing angle using sin wave
                float swingAngle = (float) Math.sin(ticksAlive * 0.06 + phaseOffsets.get(i)) * swingAmplitudes.get(i);

                // Calculate weight position based on pendulum physics
                double weightX = anchorX.get(i) + Math.sin(swingAngle) * CHAIN_LENGTH;
                double weightY = ANCHOR_HEIGHT - Math.cos(swingAngle) * CHAIN_LENGTH;
                double weightZ = anchorZ.get(i);

                // Chain midpoint
                double chainMidX = anchorX.get(i) + Math.sin(swingAngle) * (CHAIN_LENGTH / 2);
                double chainMidY = ANCHOR_HEIGHT - Math.cos(swingAngle) * (CHAIN_LENGTH / 2);

                // Update positions
                Location weightLoc = c.clone().add(weightX, weightY, weightZ);
                Location chainLoc = c.clone().add(chainMidX, chainMidY, weightZ);

                pendulumWeights.get(i).entity().teleport(weightLoc);
                pendulumChains.get(i).entity().teleport(chainLoc);

                // Rotate chain to match swing angle
                pendulumChains.get(i).rotate(swingAngle, 0, 0, 1);
            }

            // Creaking metal sounds at swing peaks
            if (ticksAlive % 18 == 0) {
                int randomPendulum = ticksAlive / 18 % PENDULUM_COUNT;
                Location soundLoc = c.clone().add(anchorX.get(randomPendulum), ANCHOR_HEIGHT, anchorZ.get(randomPendulum));
                DisplayBuilder.playSound(soundLoc, Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.4f + (float)(Math.random() * 0.3));
            }

            // Iron dust particles at weight positions
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < PENDULUM_COUNT; i += 3) {
                    float swingAngle = (float) Math.sin(ticksAlive * 0.06 + phaseOffsets.get(i)) * swingAmplitudes.get(i);
                    double wx = anchorX.get(i) + Math.sin(swingAngle) * CHAIN_LENGTH;
                    double wy = ANCHOR_HEIGHT - Math.cos(swingAngle) * CHAIN_LENGTH;
                    Location dustLoc = c.clone().add(wx, wy, anchorZ.get(i));
                    DisplayBuilder.dustParticles(dustLoc, 3, 0.3, 180, 180, 190, 1.0f);
                }
            }

            // Anvil creaking
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, ANCHOR_HEIGHT, 0), Sound.ENTITY_IRON_GOLEM_HURT, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PendulumRain(plugin); }
    }

    // ================================================================
    // 10. CHAIN NET DROP — A 6x6 grid of thin chains (12 blocks)
    //     drops from above, contracting as it falls, snaps tight
    // ================================================================
    public static class ChainNetDrop extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> netBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();
        private float netHeight = 15.0f;
        private float contraction = 1.0f;
        private boolean snapped = false;
        private static final float DESCENT_SPEED = 0.12f;
        private static final float CONTRACT_SPEED = 0.004f;
        private static final int NET_SEGMENTS = 12;

        public ChainNetDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_net_drop", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(66.0);
            config.setImpactRadius(10.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Create a 6x6 grid pattern with 12 chain blocks
            // Horizontal bars (6 blocks across the X-axis)
            for (int i = 0; i < 6; i++) {
                double x = (i - 2.5) * 1.2;
                baseOffsets.add(new double[]{x, 0, 0});
            }
            // Vertical bars (6 blocks across the Z-axis)
            for (int i = 0; i < 6; i++) {
                double z = (i - 2.5) * 1.2;
                baseOffsets.add(new double[]{0, 0, z});
            }

            for (int i = 0; i < NET_SEGMENTS; i++) {
                double[] offset = baseOffsets.get(i);
                Location blockLoc = center.clone().add(offset[0] * 3, netHeight, offset[2] * 3);
                BlockDisplayHandle block = displayBuilder.spawnBlock(blockLoc, Material.CHAIN);

                if (i < 6) {
                    // Horizontal bars — wide and thin
                    block.scale(0.22f, 0.22f, 9.0f);
                } else {
                    // Vertical bars — wide and thin in other direction
                    block.scale(9.0f, 0.22f, 0.22f);
                }
                block.glow(200, 200, 220).interpolation(3, 0);
                netBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Corner anchors
            double[][] corners = {{-3, 0, -3}, {-3, 0, 3}, {3, 0, -3}, {3, 0, 3}};
            for (double[] corner : corners) {
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(
                    center.clone().add(corner[0], netHeight, corner[2]), Material.IRON_BLOCK);
                anchor.scale(0.6f, 0.6f, 0.6f).glow(180, 180, 190);
                netBlocks.add(anchor);
                spawnedEntities.add(anchor.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 15, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (snapped) return;

            // Descend and contract
            netHeight -= DESCENT_SPEED;
            contraction = Math.max(0.3f, contraction - CONTRACT_SPEED);

            if (netHeight <= 1.0f && !snapped) {
                // SNAP — net closes rapidly
                snapped = true;
                contraction = 0.1f;

                triggerImpactDamage(c);

                // Snap effects
                DisplayBuilder.particleRing(c, 5.0, Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(200, 200, 220), 1.5f));
                w.spawnParticle(Particle.CRIT, c.clone().add(0, 1, 0), 30, 3, 0.5, 3, 0.2);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.2f, 1.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.3f);
            }

            // Update positions
            for (int i = 0; i < NET_SEGMENTS; i++) {
                double[] offset = baseOffsets.get(i);
                double x = offset[0] * 3 * contraction;
                double z = offset[2] * 3 * contraction;
                Location newLoc = c.clone().add(x, netHeight, z);
                netBlocks.get(i).entity().teleport(newLoc);

                // Scale chains to match contraction
                if (i < 6) {
                    netBlocks.get(i).scale(0.22f, 0.22f, 9.0f * contraction);
                } else if (i < 12) {
                    netBlocks.get(i).scale(9.0f * contraction, 0.22f, 0.22f);
                }
            }

            // Update corner anchors
            double[][] corners = {{-3, 0, -3}, {-3, 0, 3}, {3, 0, -3}, {3, 0, 3}};
            for (int i = 0; i < corners.length && i + NET_SEGMENTS < netBlocks.size(); i++) {
                double cx = corners[i][0] * contraction;
                double cz = corners[i][2] * contraction;
                netBlocks.get(NET_SEGMENTS + i).entity().teleport(c.clone().add(cx, netHeight, cz));
            }

            // Taut chain sound as it tightens
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, netHeight, 0), Sound.BLOCK_CHAIN_PLACE, 0.5f,
                        0.8f + (1.0f - contraction) * 0.8f);
            }

            // Chain dust as it descends
            if (ticksAlive % 4 == 0) {
                Location dustLoc = c.clone().add(
                    (Math.random() - 0.5) * 6 * contraction,
                    netHeight,
                    (Math.random() - 0.5) * 6 * contraction
                );
                DisplayBuilder.dustParticles(dustLoc, 3, 0.3, 200, 200, 220, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainNetDrop(plugin); }
    }

    // ================================================================
    // 11. CHAIN CASCADE — 5 waves of 4 chains each, falling left to
    //     right like a waterfall cascade
    // ================================================================
    public static class ChainCascade extends BlockDisplayAttack {

        private static final int WAVE_COUNT = 5;
        private static final int CHAINS_PER_WAVE = 4;
        private final List<List<BlockDisplayHandle>> waves = new ArrayList<>();
        private final List<Float> waveYPositions = new ArrayList<>();
        private final List<Boolean> waveImpacted = new ArrayList<>();
        private static final float START_Y = 16.0f;
        private static final float FALL_SPEED = 0.25f;
        private static final int WAVE_DELAY = 15; // ticks between waves
        private static final float LATERAL_OFFSET = 2.5f;

        public ChainCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_cascade", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int wave = 0; wave < WAVE_COUNT; wave++) {
                waves.add(new ArrayList<>());
                waveYPositions.add(START_Y);
                waveImpacted.add(false);
            }

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int wave = 0; wave < WAVE_COUNT; wave++) {
                int waveStartTick = wave * WAVE_DELAY;
                if (ticksAlive < waveStartTick) continue;
                if (waveImpacted.get(wave)) continue;

                // Spawn wave chains
                if (waves.get(wave).isEmpty()) {
                    float lateralX = (-WAVE_COUNT / 2.0f + wave) * LATERAL_OFFSET;
                    for (int c2 = 0; c2 < CHAINS_PER_WAVE; c2++) {
                        double zOffset = (c2 - CHAINS_PER_WAVE / 2.0) * 1.5;
                        Location chainLoc = c.clone().add(lateralX, START_Y, zOffset);
                        BlockDisplayHandle chain = displayBuilder.spawnBlock(chainLoc, Material.CHAIN);
                        chain.scale(0.6f, 4.5f, 0.6f)
                             .glow(200, 200, 220)
                             .interpolation(2, 0);
                        waves.get(wave).add(chain);
                        spawnedEntities.add(chain.entity());
                    }

                    // Iron block connector at top of wave
                    BlockDisplayHandle connector = displayBuilder.spawnBlock(
                        c.clone().add(lateralX, START_Y + 1, 0), Material.IRON_BLOCK);
                    connector.scale(0.75f, 0.45f, (float)(CHAINS_PER_WAVE * 2.25))
                             .glow(180, 180, 190);
                    waves.get(wave).add(connector);
                    spawnedEntities.add(connector.entity());

                    DisplayBuilder.playSound(c.clone().add(lateralX, START_Y, 0), Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.7f + wave * 0.1f);
                }

                // Fall
                float y = waveYPositions.get(wave) - FALL_SPEED;
                waveYPositions.set(wave, y);

                float lateralX = (-WAVE_COUNT / 2.0f + wave) * LATERAL_OFFSET;
                List<BlockDisplayHandle> waveChains = waves.get(wave);

                for (int ci = 0; ci < CHAINS_PER_WAVE && ci < waveChains.size(); ci++) {
                    double zOffset = (ci - CHAINS_PER_WAVE / 2.0) * 1.5;
                    waveChains.get(ci).entity().teleport(c.clone().add(lateralX, y, zOffset));
                }

                // Connector at top
                if (waveChains.size() > CHAINS_PER_WAVE) {
                    waveChains.get(CHAINS_PER_WAVE).entity().teleport(c.clone().add(lateralX, y + 1, 0));
                }

                // Water-like particle streams
                if (ticksAlive % 3 == 0) {
                    Location streamLoc = c.clone().add(lateralX, y + 2, (Math.random() - 0.5) * 4);
                    DisplayBuilder.dustParticles(streamLoc, 4, 0.4, 180, 180, 190, 1.0f);
                    w.spawnParticle(Particle.DRIPPING_WATER, streamLoc, 2, 0.3, 0.5, 0.3, 0);
                }

                // Impact
                if (y <= 0) {
                    waveYPositions.set(wave, 0f);
                    waveImpacted.set(wave, true);

                    Location impactLoc = c.clone().add(lateralX, 0, 0);
                    triggerImpactDamage(impactLoc);

                    w.spawnParticle(Particle.BLOCK, impactLoc, 40, 2, 0.5, 2, 0.3,
                            Material.CHAIN.createBlockData());
                    DisplayBuilder.particleRing(impactLoc, 3.0, Particle.DUST, 20,
                            new Particle.DustOptions(Color.fromRGB(180, 180, 190), 1.5f));
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.9f + wave * 0.15f);
                }
            }

            // Cascading waterfall sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.7f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainCascade(plugin); }
    }

    // ================================================================
    // 12. CHAIN BLIZZARD — 25 tiny chain fragments swirl horizontally
    //     in figure-8 paths at chest height
    // ================================================================
    public static class ChainBlizzard extends BlockDisplayAttack {

        private static final int FRAGMENT_COUNT = 25;
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private final List<Float> figure8Phases = new ArrayList<>();
        private final List<Float> figure8Speeds = new ArrayList<>();
        private final List<Float> figure8Radii = new ArrayList<>();
        private final List<Float> yOffsets = new ArrayList<>();
        private final List<Float> spinRates = new ArrayList<>();

        public ChainBlizzard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_blizzard", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.CHAIN, Material.IRON_BARS, Material.CHAIN};

            for (int i = 0; i < FRAGMENT_COUNT; i++) {
                figure8Phases.add((float)(Math.random() * Math.PI * 2));
                figure8Speeds.add(0.04f + (float)(Math.random() * 0.04));
                figure8Radii.add(2.0f + (float)(Math.random() * 4.0));
                yOffsets.add(1.0f + (float)(Math.random() * 1.5));
                spinRates.add(0.15f + (float)(Math.random() * 0.2));

                Location fragLoc = center.clone().add(
                    (Math.random() - 0.5) * 8,
                    1.5,
                    (Math.random() - 0.5) * 8
                );
                Material mat = mats[i % mats.length];
                BlockDisplayHandle frag = displayBuilder.spawnBlock(fragLoc, mat);
                frag.scale(0.3f, 0.3f, 0.3f)
                    .glow(200, 200, 220)
                    .interpolation(2, 0);
                fragments.add(frag);
                spawnedEntities.add(frag.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < FRAGMENT_COUNT; i++) {
                float phase = figure8Phases.get(i) + ticksAlive * figure8Speeds.get(i);
                float radius = figure8Radii.get(i);

                // Figure-8 path (lemniscate of Bernoulli)
                double t = phase;
                double denom = 1 + Math.sin(t) * Math.sin(t);
                double x = radius * Math.cos(t) / denom;
                double z = radius * Math.sin(t) * Math.cos(t) / denom;
                float y = yOffsets.get(i) + (float) Math.sin(phase * 1.5) * 0.3f;

                Location newLoc = c.clone().add(x, y, z);
                fragments.get(i).entity().teleport(newLoc);

                // Spin fragments
                float spin = ticksAlive * spinRates.get(i);
                fragments.get(i).rotate(spin, 1, 0, 0.5f);
            }

            // White/gray dust particle cloud
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    Location dustLoc = c.clone().add(
                        (Math.random() - 0.5) * 8,
                        1.0 + Math.random() * 2,
                        (Math.random() - 0.5) * 8
                    );
                    DisplayBuilder.dustParticles(dustLoc, 2, 0.4, 210, 210, 220, 0.8f);
                }
            }

            // Dense white particle cloud
            if (ticksAlive % 3 == 0) {
                Location cloudLoc = c.clone().add(
                    (Math.random() - 0.5) * 6, 1.5, (Math.random() - 0.5) * 6);
                DisplayBuilder.dustParticles(cloudLoc, 3, 0.5, 240, 240, 245, 1.0f);
            }

            // Cold metallic sounds
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 1.8f);
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.3f, 1.5f);
            }
            // High-pitched metallic whistle
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainBlizzard(plugin); }
    }

    // ================================================================
    // 13. SCATTERED SHACKLE RAIN — 12 shackle structures (2 chains
    //     + iron block) fall from various heights, spin end-over-end
    // ================================================================
    public static class ScatteredShackleRain extends BlockDisplayAttack {

        private static final int SHACKLE_COUNT = 12;
        private final List<BlockDisplayHandle> leftChains = new ArrayList<>();
        private final List<BlockDisplayHandle> rightChains = new ArrayList<>();
        private final List<BlockDisplayHandle> connectors = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Float> spinSpeeds = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();

        public ScatteredShackleRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scattered_shackle_rain", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(63.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SHACKLE_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 7.0;
                xOffsets.add(Math.cos(angle) * dist);
                zOffsets.add(Math.sin(angle) * dist);
                yPositions.add(14.0f + (float)(Math.random() * 10));
                fallSpeeds.add(0.2f + (float)(Math.random() * 0.2));
                spinSpeeds.add(0.12f + (float)(Math.random() * 0.1));
                delays.add((int)(Math.random() * 40));
                impacted.add(false);
                leftChains.add(null);
                rightChains.add(null);
                connectors.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, 18, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < SHACKLE_COUNT; i++) {
                if (ticksAlive < delays.get(i)) continue;
                if (impacted.get(i)) continue;

                // Spawn shackle on delay
                if (leftChains.get(i) == null) {
                    Location spawnLoc = c.clone().add(xOffsets.get(i), yPositions.get(i), zOffsets.get(i));

                    BlockDisplayHandle left = displayBuilder.spawnBlock(spawnLoc.clone().add(-0.7, 0, 0), Material.CHAIN);
                    left.scale(0.45f, 3.0f, 0.45f).glow(200, 200, 220).interpolation(2, 0);
                    leftChains.set(i, left);
                    spawnedEntities.add(left.entity());

                    BlockDisplayHandle right = displayBuilder.spawnBlock(spawnLoc.clone().add(0.7, 0, 0), Material.CHAIN);
                    right.scale(0.45f, 3.0f, 0.45f).glow(200, 200, 220).interpolation(2, 0);
                    rightChains.set(i, right);
                    spawnedEntities.add(right.entity());

                    BlockDisplayHandle conn = displayBuilder.spawnBlock(spawnLoc.clone().add(0, 1, 0), Material.IRON_BLOCK);
                    conn.scale(2.7f, 0.6f, 0.6f).glow(180, 180, 190).interpolation(2, 0);
                    connectors.set(i, conn);
                    spawnedEntities.add(conn.entity());

                    DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.9f);
                }

                // Fall
                float y = yPositions.get(i) - fallSpeeds.get(i);
                yPositions.set(i, y);

                // End-over-end spin
                float spin = (ticksAlive - delays.get(i)) * spinSpeeds.get(i);

                // Calculate rotated positions for shackle components
                float sinSpin = (float) Math.sin(spin);
                float cosSpin = (float) Math.cos(spin);
                double ox = xOffsets.get(i);
                double oz = zOffsets.get(i);

                Location baseLoc = c.clone().add(ox, y, oz);
                leftChains.get(i).entity().teleport(baseLoc.clone().add(-0.7 * cosSpin, 0.7 * sinSpin, 0));
                rightChains.get(i).entity().teleport(baseLoc.clone().add(0.7 * cosSpin, -0.7 * sinSpin, 0));
                connectors.get(i).entity().teleport(baseLoc.clone().add(0, sinSpin * 0.5, 0));

                // Rotate all components
                leftChains.get(i).rotate(spin, 0, 0, 1);
                rightChains.get(i).rotate(spin, 0, 0, 1);
                connectors.get(i).rotate(spin, 0, 0, 1);

                // Trail particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(baseLoc.clone().add(0, 1.5, 0), 2, 0.3, 180, 180, 190, 0.8f);
                }

                // Impact
                if (y <= 0) {
                    yPositions.set(i, 0f);
                    impacted.set(i, true);

                    Location impactLoc = c.clone().add(ox, 0, oz);
                    triggerImpactDamage(impactLoc);

                    w.spawnParticle(Particle.BLOCK, impactLoc, 30, 1.5, 0.3, 1.5, 0.2,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.CRIT, impactLoc, 10, 0.5, 0.2, 0.5, 0.15);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.9f, 1.0f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_CHAIN_BREAK, 0.7f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScatteredShackleRain(plugin); }
    }

    // ================================================================
    // 14. WEIGHTED CHAIN FALL — 6 heavy chains with iron block weights
    //     fall straight down, accelerating, ground-crack on impact
    // ================================================================
    public static class WeightedChainFall extends BlockDisplayAttack {

        private static final int WEIGHT_COUNT = 6;
        private final List<BlockDisplayHandle> chainBodies = new ArrayList<>();
        private final List<BlockDisplayHandle> weightBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> topCaps = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> velocities = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();
        private static final float START_Y = 20.0f;
        private static final float GRAVITY = 0.015f;

        public WeightedChainFall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("weighted_chain_fall", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(380);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(75.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < WEIGHT_COUNT; i++) {
                double angle = (2 * Math.PI * i) / WEIGHT_COUNT;
                double dist = 2.0 + Math.random() * 3.0;
                xOffsets.add(Math.cos(angle) * dist);
                zOffsets.add(Math.sin(angle) * dist);
                yPositions.add(START_Y);
                velocities.add(0.05f);
                delays.add(i * 8);
                impacted.add(false);
                chainBodies.add(null);
                weightBlocks.add(null);
                topCaps.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < WEIGHT_COUNT; i++) {
                if (ticksAlive < delays.get(i)) continue;
                if (impacted.get(i)) continue;

                // Spawn on delay
                if (chainBodies.get(i) == null) {
                    Location spawnLoc = c.clone().add(xOffsets.get(i), START_Y, zOffsets.get(i));

                    // Heavy chain body
                    BlockDisplayHandle body = displayBuilder.spawnBlock(spawnLoc, Material.CHAIN);
                    body.scale(0.75f, 7.5f, 0.75f)
                        .glow(200, 200, 220)
                        .interpolation(2, 0);
                    chainBodies.set(i, body);
                    spawnedEntities.add(body.entity());

                    // Iron weight at bottom
                    BlockDisplayHandle weight = displayBuilder.spawnBlock(
                        spawnLoc.clone().add(0, -3, 0), Material.IRON_BLOCK);
                    weight.scale(2.25f, 2.25f, 2.25f)
                          .glow(100, 100, 110)
                          .interpolation(2, 0);
                    weightBlocks.set(i, weight);
                    spawnedEntities.add(weight.entity());

                    // Deepslate cap at top
                    BlockDisplayHandle cap = displayBuilder.spawnBlock(
                        spawnLoc.clone().add(0, 2.5, 0), Material.DEEPSLATE);
                    cap.scale(1.05f, 0.75f, 1.05f)
                       .glow(60, 50, 50);
                    topCaps.set(i, cap);
                    spawnedEntities.add(cap.entity());

                    DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.5f);
                }

                // Accelerating fall
                float vel = velocities.get(i) + GRAVITY;
                velocities.set(i, vel);
                float y = yPositions.get(i) - vel;
                yPositions.set(i, y);

                double ox = xOffsets.get(i);
                double oz = zOffsets.get(i);

                chainBodies.get(i).entity().teleport(c.clone().add(ox, y, oz));
                weightBlocks.get(i).entity().teleport(c.clone().add(ox, y - 3, oz));
                topCaps.get(i).entity().teleport(c.clone().add(ox, y + 2.5, oz));

                // Wind whoosh as it accelerates
                if (vel > 0.3f && ticksAlive % 3 == 0) {
                    Location trailLoc = c.clone().add(ox, y + 3, oz);
                    DisplayBuilder.dustParticles(trailLoc, 5, 0.3, 180, 180, 190, 1.2f);
                }

                // Impact — weight hits first
                if (y - 3 <= 0) {
                    yPositions.set(i, 3f);
                    impacted.set(i, true);

                    Location impactLoc = c.clone().add(ox, 0, oz);
                    triggerImpactDamage(impactLoc);

                    // Ground-crack particle effect
                    for (int j = 0; j < 8; j++) {
                        double crackAngle = (2 * Math.PI * j) / 8;
                        Location crackEnd = impactLoc.clone().add(
                            Math.cos(crackAngle) * 3, 0.1, Math.sin(crackAngle) * 3);
                        DisplayBuilder.particleLine(impactLoc, crackEnd, Particle.DUST, 4,
                                new Particle.DustOptions(Color.fromRGB(100, 100, 110), 1.5f));
                    }

                    // Heavy impact effects
                    w.spawnParticle(Particle.BLOCK, impactLoc, 80, 2, 0.5, 2, 0.4,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.EXPLOSION, impactLoc, 2, 0.5, 0.2, 0.5, 0);
                    DisplayBuilder.particleRing(impactLoc, 3.5, Particle.DUST, 30,
                            new Particle.DustOptions(Color.fromRGB(100, 100, 110), 2.0f));

                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.4f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.3f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.5f);

                    // Chain coils on top effect — reposition chain body to ground
                    chainBodies.get(i).entity().teleport(c.clone().add(ox, 0.5, oz));
                    chainBodies.get(i).scale(2.25f, 0.75f, 2.25f); // Flatten to simulate coiling
                    topCaps.get(i).entity().teleport(c.clone().add(ox, 1, oz));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WeightedChainFall(plugin); }
    }

    // ================================================================
    // 15. CHAIN LIGHTNING RAIN — 10 chains descend in zigzag
    //     lightning-bolt paths, electric effects, jarring movement
    // ================================================================
    public static class ChainLightningRain extends BlockDisplayAttack {

        private static final int BOLT_COUNT = 10;
        private final List<BlockDisplayHandle> boltChains = new ArrayList<>();
        private final List<BlockDisplayHandle> boltCores = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Double> xPositions = new ArrayList<>();
        private final List<Double> zPositions = new ArrayList<>();
        private final List<Double> baseX = new ArrayList<>();
        private final List<Double> baseZ = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();
        private final List<Integer> zigzagTimer = new ArrayList<>();
        private static final float START_Y = 18.0f;
        private static final float FALL_SPEED = 0.4f;
        private static final int ZIGZAG_INTERVAL = 4; // ticks between zigzags

        public ChainLightningRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_lightning_rain", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(330);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(69.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BOLT_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1 + Math.random() * 6;
                double bx = Math.cos(angle) * dist;
                double bz = Math.sin(angle) * dist;
                baseX.add(bx);
                baseZ.add(bz);
                xPositions.add(bx);
                zPositions.add(bz);
                yPositions.add(START_Y + (float)(Math.random() * 5));
                delays.add(i * 5);
                impacted.add(false);
                zigzagTimer.add(0);
                boltChains.add(null);
                boltCores.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BOLT_COUNT; i++) {
                if (ticksAlive < delays.get(i)) continue;
                if (impacted.get(i)) continue;

                // Spawn on delay
                if (boltChains.get(i) == null) {
                    Location spawnLoc = c.clone().add(xPositions.get(i), yPositions.get(i), zPositions.get(i));

                    BlockDisplayHandle chain = displayBuilder.spawnBlock(spawnLoc, Material.CHAIN);
                    chain.scale(0.6f, 3.75f, 0.6f)
                         .glow(200, 200, 220)
                         .interpolation(1, 0);
                    boltChains.set(i, chain);
                    spawnedEntities.add(chain.entity());

                    // Glowing core (lightning center)
                    BlockDisplayHandle core = displayBuilder.spawnBlock(spawnLoc, Material.IRON_BLOCK);
                    core.scale(0.38f, 0.38f, 0.38f)
                        .glow(220, 230, 255)
                        .brightness(15, 15);
                    boltCores.set(i, core);
                    spawnedEntities.add(core.entity());

                    DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.5f);
                }

                // Fall
                float y = yPositions.get(i) - FALL_SPEED;
                yPositions.set(i, y);

                // Zigzag — teleport sideways every few ticks
                int zt = zigzagTimer.get(i) + 1;
                zigzagTimer.set(i, zt);

                if (zt >= ZIGZAG_INTERVAL) {
                    zigzagTimer.set(i, 0);

                    // Random lateral jump
                    double jumpX = (Math.random() - 0.5) * 3.0;
                    double jumpZ = (Math.random() - 0.5) * 3.0;
                    xPositions.set(i, xPositions.get(i) + jumpX);
                    zPositions.set(i, zPositions.get(i) + jumpZ);

                    // Electric flash at zigzag point
                    Location flashLoc = c.clone().add(xPositions.get(i), y, zPositions.get(i));
                    w.spawnParticle(Particle.END_ROD, flashLoc, 8, 0.3, 0.3, 0.3, 0.05);
                    DisplayBuilder.dustParticles(flashLoc, 4, 0.4, 220, 230, 255, 1.0f);

                    // Electric crackle sound
                    DisplayBuilder.playSound(flashLoc, Sound.BLOCK_CHAIN_BREAK, 0.4f, 1.8f);
                }

                // Update positions
                Location newLoc = c.clone().add(xPositions.get(i), y, zPositions.get(i));
                boltChains.get(i).entity().teleport(newLoc);
                boltCores.get(i).entity().teleport(newLoc.clone().add(0, -0.5, 0));

                // END_ROD trail particles
                if (ticksAlive % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, newLoc.clone().add(0, 1.5, 0), 3, 0.2, 0.4, 0.2, 0.02);
                }

                // Electric chain glow particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(newLoc, 2, 0.3, 200, 200, 220, 0.8f);
                }

                // Impact
                if (y <= 0) {
                    yPositions.set(i, 0f);
                    impacted.set(i, true);

                    Location impactLoc = c.clone().add(xPositions.get(i), 0, zPositions.get(i));
                    triggerImpactDamage(impactLoc);

                    // Lightning strike effects
                    w.spawnParticle(Particle.END_ROD, impactLoc, 30, 2, 1, 2, 0.1);
                    w.spawnParticle(Particle.FLASH, impactLoc, 1, 0, 0, 0, 0);
                    DisplayBuilder.particleRing(impactLoc, 3.5, Particle.DUST, 28,
                            new Particle.DustOptions(Color.fromRGB(220, 230, 255), 1.8f));
                    w.spawnParticle(Particle.BLOCK, impactLoc, 40, 2, 0.3, 2, 0.3,
                            Material.CHAIN.createBlockData());

                    // Thunder impact sounds
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_CHAIN_BREAK, 1.2f, 0.4f);
                }
            }

            // Ambient electric crackle
            if (ticksAlive % 15 == 0) {
                Location crackle = c.clone().add(
                    (Math.random() - 0.5) * 10,
                    5 + Math.random() * 10,
                    (Math.random() - 0.5) * 10
                );
                w.spawnParticle(Particle.END_ROD, crackle, 5, 0.5, 0.5, 0.5, 0.03);
                DisplayBuilder.playSound(crackle, Sound.BLOCK_CHAIN_PLACE, 0.3f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainLightningRain(plugin); }
    }
}
