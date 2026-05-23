package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — ENVIRONMENTAL ATTACKS 16-30 (Category 2 — Particle Atmospheric Effects)
 *
 * Pure particle-driven atmosphere — most are pure ambient (damage=0). Each
 * attack layers 3+ particle types per active tick to build a cohesive heavy
 * industrial mood: iron dust, smoke columns, forge embers, rust flakes,
 * electric arcs, ground fog, chain wisps, shockwave rings, metallic rain.
 *
 * Choreography per attack:
 *   PHASE 1 — SPAWN     : buildup particles + atmospheric sound
 *   PHASE 2 — ACTIVE    : continuous layered particle loop
 *   PHASE 3 — DISSIPATE : tail-off particles, soft close-out sound
 *
 * 5 of 15 are damaging (timing-dodge style aggressive atmospheres):
 *   21 Electric Arc Flickers, 24 Distant Impact Rumbles, 25 Metallic Rain,
 *   28 Shockwave Ground Rings, 29 Forge Ember Glow.
 *
 * 10 of 15 are pure ambient (no damage): 16, 17, 18, 19, 20, 22, 23, 26, 27, 30.
 *
 * ItemDisplays only (0-3 per attack as anchors). NO BlockDisplays. NO potion
 * effects. Modern Particle enum names only (SMOKE / LARGE_SMOKE / EXPLOSION /
 * DUST / FALLING_DUST / BLOCK).
 */
public final class ChainEnvironmental2 {
    private ChainEnvironmental2() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IronDustMist(plugin));
        registry.register(new RisingSmokeColumns(plugin));
        registry.register(new MetalSparkDrifts(plugin));
        registry.register(new ChainEnergyWisps(plugin));
        registry.register(new RustFlakeFall(plugin));
        registry.register(new ElectricArcFlickers(plugin));
        registry.register(new GroundIronFog(plugin));
        registry.register(new ChainGroundTraces(plugin));
        registry.register(new DistantImpactRumbles(plugin));
        registry.register(new MetallicRain(plugin));
        registry.register(new ShadowTendrilSnakes(plugin));
        registry.register(new IronAurora(plugin));
        registry.register(new ShockwaveGroundRings(plugin));
        registry.register(new ForgeEmberGlow(plugin));
        registry.register(new ChainFragmentShower(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    /** Configure an ambient (no damage) attack with standard defaults. */
    private static void applyAmbientDefaults(AttackConfig config) {
        config.setDamage(0.0);
        config.setDamageRadius(0.0);
        config.setTicksBetweenDamage(20);
        config.setDamageDelayTicks(0);
        config.setDamageOnImpactOnly(false);
        config.setImpactDamage(0.0);
        config.setImpactRadius(0.0);
        config.setTracksPlayer(false);
        config.setFollowAiEnabled(false);
        config.setEnabled(true);
        config.setChance(10.0);
    }

    /** Configure an aggressive (damaging) atmosphere attack. */
    private static void applyDamagingDefaults(AttackConfig config, double damage, double radius) {
        config.setDamage(damage);
        config.setDamageRadius(radius);
        config.setTicksBetweenDamage(20);
        config.setDamageDelayTicks(20);
        config.setDamageOnImpactOnly(false);
        config.setImpactDamage(0.0);
        config.setImpactRadius(0.0);
        config.setTracksPlayer(false);
        config.setFollowAiEnabled(false);
        config.setEnabled(true);
        config.setChance(10.0);
    }

    // ================================================================
    // 16. IRON DUST MIST — "The Haze" (ambient)
    //     Constant low-density iron dust mist filling arena air Y=0.5-3.5.
    // ================================================================
    public static class IronDustMist extends EnvironmentalAttack {
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 40;

        public IronDustMist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_dust_mist", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(120);
            config.setDesignType("Ambient atmosphere — iron dust mist (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.5f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            int density = Math.max(0, (int) (2 * intensity));
            // Layer 1: gray iron dust (DUST) at random arena positions
            for (int i = 0; i < density; i++) {
                double rx = (Math.random() - 0.5) * 18.0;
                double rz = (Math.random() - 0.5) * 18.0;
                double ry = 0.5 + Math.random() * 3.0;
                Location p = c.clone().add(rx, ry, rz);
                DisplayBuilder.dustParticles(p, 1, 0.0, 140, 140, 140, 1.8f);
            }
            // Layer 2: occasional white sparkle dust accent
            if (tick % 8 == 0) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                Location p = c.clone().add(rx, 1.5 + Math.random() * 1.5, rz);
                DisplayBuilder.dustParticles(p, 1, 0.0, 200, 200, 210, 0.6f);
            }
            // Layer 3: faint smoke drift
            if (tick % 6 == 0) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                w.spawnParticle(Particle.SMOKE, c.clone().add(rx, 1.2, rz), 1, 0.1, 0.1, 0.1, 0.0);
            }
            if (tick == SPAWN_END) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.4f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.3f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new IronDustMist(plugin); }
    }

    // ================================================================
    // 17. RISING SMOKE COLUMNS — "The Forges" (ambient)
    //     4 smoke columns at arena corners rising Y=0..6.
    // ================================================================
    public static class RisingSmokeColumns extends EnvironmentalAttack {
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 40;
        private static final double[][] CORNERS = {
                {  8.0,  8.0 }, {  8.0, -8.0 }, { -8.0,  8.0 }, { -8.0, -8.0 }
        };

        public RisingSmokeColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rising_smoke_columns", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(800);
            config.setCooldownTicks(140);
            config.setDesignType("Ambient atmosphere — corner forge smoke columns (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.4f, 0.5f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            for (double[] corner : CORNERS) {
                for (double y = 0; y <= 6.0; y += 0.5) {
                    if (Math.random() > 0.35 * intensity) continue;
                    Location p = c.clone().add(corner[0] + (Math.random() - 0.5) * 0.4, y, corner[1] + (Math.random() - 0.5) * 0.4);
                    // Layer 1: campfire cosy smoke (rising)
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
                // Layer 2: dark dust for soot accents
                if (tick % 5 == 0) {
                    Location p = c.clone().add(corner[0], 0.3 + Math.random() * 1.2, corner[1]);
                    DisplayBuilder.dustParticles(p, 1, 0.15, 70, 70, 70, 1.4f);
                }
                // Layer 3: flame accent at base implies a forge
                if (tick % 9 == 0) {
                    Location p = c.clone().add(corner[0], 0.2, corner[1]);
                    w.spawnParticle(Particle.FLAME, p, 1, 0.1, 0.05, 0.1, 0.01);
                }
            }
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.3f, 0.7f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.3f, 0.4f);
        }

        @Override public AbstractAttack newInstance() { return new RisingSmokeColumns(plugin); }
    }

    // ================================================================
    // 18. METAL SPARK DRIFTS — "The Embers" (ambient)
    //     Sparks drifting across arena mid-air every 5 ticks.
    // ================================================================
    public static class MetalSparkDrifts extends EnvironmentalAttack {
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 30;

        public MetalSparkDrifts(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("metal_spark_drifts", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(110);
            config.setDesignType("Ambient atmosphere — drifting metal sparks (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.3f, 1.4f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            if (tick % 5 == 0 && intensity > 0) {
                int spawns = 1 + (int) (2 * intensity);
                for (int s = 0; s < spawns; s++) {
                    double rx = (Math.random() - 0.5) * 14.0;
                    double rz = (Math.random() - 0.5) * 14.0;
                    double ry = 1.5 + Math.random() * 1.5;
                    Location p = c.clone().add(rx, ry, rz);
                    // Layer 1: CRIT sparks with upward drift
                    w.spawnParticle(Particle.CRIT, p, 3, 0.3, 0.2, 0.3, 0.05);
                    // Layer 2: ELECTRIC_SPARK accent
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.2, 0.15, 0.2, 0.04);
                    // Layer 3: warm forge-orange dust
                    DisplayBuilder.dustParticles(p, 1, 0.2, 255, 160, 60, 1.0f);
                }
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 1.1f);
        }

        @Override public AbstractAttack newInstance() { return new MetalSparkDrifts(plugin); }
    }

    // ================================================================
    // 19. CHAIN ENERGY WISPS — "The Static" (ambient)
    //     Small colored wisps implying latent chain energy.
    // ================================================================
    public static class ChainEnergyWisps extends EnvironmentalAttack {
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 35;

        public ChainEnergyWisps(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_energy_wisps", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(120);
            config.setDesignType("Ambient atmosphere — chain energy wisps (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.5f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Layer 1: dim cool-blue wisp dust (slow drift)
            int drifts = Math.max(0, (int) (1 * intensity));
            for (int i = 0; i < drifts; i++) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                double ry = 1.0 + Math.random() * 3.0;
                Location p = c.clone().add(rx, ry, rz);
                DisplayBuilder.dustParticles(p, 1, 0.0, 80, 90, 110, 1.5f);
            }
            // Layer 2: bright sparkles
            if (tick % 4 == 0) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                double ry = 1.5 + Math.random() * 2.5;
                Location p = c.clone().add(rx, ry, rz);
                DisplayBuilder.dustParticles(p, 1, 0.0, 200, 200, 200, 0.8f);
            }
            // Layer 3: electric spark very occasionally
            if (tick % 13 == 0) {
                double rx = (Math.random() - 0.5) * 12.0;
                double rz = (Math.random() - 0.5) * 12.0;
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(rx, 2.0, rz), 1, 0.15, 0.15, 0.15, 0.03);
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainEnergyWisps(plugin); }
    }

    // ================================================================
    // 20. RUST FLAKE FALL — "The Decay" (ambient)
    //     RED_TERRACOTTA falling dust from ceiling at Y=5.
    // ================================================================
    public static class RustFlakeFall extends EnvironmentalAttack {
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 30;
        private static BlockData rustData;

        public RustFlakeFall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rust_flake_fall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(120);
            config.setDesignType("Ambient atmosphere — rust flake ceiling fall (no damage)");
        }

        private BlockData rustData() {
            if (rustData == null) rustData = Material.RED_TERRACOTTA.createBlockData();
            return rustData;
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRAVEL_FALL, 0.4f, 0.7f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            int count = Math.max(0, (int) (4 * intensity));
            // Layer 1: FALLING_DUST(RED_TERRACOTTA) from ceiling
            for (int i = 0; i < count; i++) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                Location p = c.clone().add(rx, 5.0, rz);
                w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.1, 0.0, 0.1, 0.0, rustData());
            }
            // Layer 2: rust-tinted dust drift at mid height
            if (tick % 4 == 0) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 2.5, rz);
                DisplayBuilder.dustParticles(p, 1, 0.2, 160, 70, 40, 1.0f);
            }
            // Layer 3: ground settle dust
            if (tick % 6 == 0) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 0.15, rz);
                DisplayBuilder.dustParticles(p, 1, 0.25, 120, 60, 30, 1.2f);
            }
            if (tick % 100 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRAVEL_FALL, 0.3f, 0.7f);
        }

        @Override public AbstractAttack newInstance() { return new RustFlakeFall(plugin); }
    }

    // ================================================================
    // 21. ELECTRIC ARC FLICKERS — "The Charge" (DAMAGING)
    //     Brief burst-events of electric arcs at random arena points.
    //     Player caught in the arc-radius during a flicker takes damage.
    // ================================================================
    public static class ElectricArcFlickers extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        // Tracking the current arc-burst position so isInCylinderRange damage maps to a real visible arc.
        private double arcX = 0.0, arcZ = 0.0;
        private int nextBurst = 30;
        private int burstUntil = -1;

        public ElectricArcFlickers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("electric_arc_flickers", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyDamagingDefaults(config, 100.0, 4.5);
            config.setDurationTicks(700);
            config.setCooldownTicks(140);
            config.setDesignType("Aggressive atmosphere — electric arc bursts (timing dodge)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.3f);
            DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 1.6f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;

            // Idle ambient sparks while between bursts (purely decorative)
            if (tick % 12 == 0) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(rx, 1.8, rz), 1, 0.1, 0.1, 0.1, 0.02);
            }

            // Trigger a burst
            if (tick >= nextBurst && tick < dissipateStart) {
                arcX = (Math.random() - 0.5) * 14.0;
                arcZ = (Math.random() - 0.5) * 14.0;
                burstUntil = tick + 3;
                nextBurst = tick + 40 + (int) (Math.random() * 50); // 40-90t
                Location p = c.clone().add(arcX, 1.5, arcZ);
                // Pre-burst chime
                DisplayBuilder.playSound(p, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.7f);
                DisplayBuilder.playSound(p, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 1.4f);
            }

            // Active burst — emit dense layered particles + drag center for damage proximity
            if (tick <= burstUntil) {
                Location p = c.clone().add(arcX, 1.5, arcZ);
                // Layer 1: ELECTRIC_SPARK dense cluster
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 15, 0.3, 0.3, 0.3, 0.15);
                // Layer 2: CRIT impact sparks
                w.spawnParticle(Particle.CRIT, p, 8, 0.3, 0.3, 0.3, 0.20);
                // Layer 3: white-blue dust shimmer
                DisplayBuilder.dustParticles(p, 4, 0.4, 180, 200, 255, 1.3f);
                // Vertical arc column
                for (int y = 0; y < 4; y++) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p.clone().add(0, y * 0.4, 0), 1, 0.06, 0.06, 0.06, 0.04);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ElectricArcFlickers(plugin); }
    }

    // ================================================================
    // 22. GROUND IRON FOG — "The Creep" (ambient)
    //     Dense low-lying fog at Y=0.2 across arena floor.
    // ================================================================
    public static class GroundIronFog extends EnvironmentalAttack {
        private static final int SPAWN_END = 30;
        private static final int DISSIPATE_LEN = 40;

        public GroundIronFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_iron_fog", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(800);
            config.setCooldownTicks(150);
            config.setDesignType("Ambient atmosphere — low ground iron fog (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.3f, 0.5f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            int count = Math.max(0, (int) (8 * intensity));
            // Layer 1: SMOKE near floor (slow drift)
            for (int i = 0; i < count; i++) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                Location p = c.clone().add(rx, 0.2, rz);
                w.spawnParticle(Particle.SMOKE, p, 1, 2.0, 0.0, 2.0, 0.0);
            }
            // Layer 2: occasional LARGE_SMOKE for dense patches
            if (tick % 5 == 0) {
                double rx = (Math.random() - 0.5) * 12.0;
                double rz = (Math.random() - 0.5) * 12.0;
                Location p = c.clone().add(rx, 0.3, rz);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.4, 0.0, 0.4, 0.0);
            }
            // Layer 3: muted gray dust accents
            if (tick % 4 == 0) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 0.15, rz);
                DisplayBuilder.dustParticles(p, 1, 0.2, 90, 90, 95, 1.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new GroundIronFog(plugin); }
    }

    // ================================================================
    // 23. CHAIN GROUND TRACES — "The Marks" (ambient)
    //     4 ghost-echo lines on the ground (DUST), each 1.5 blocks long,
    //     fading over 80 ticks. New trace cycles continuously.
    // ================================================================
    public static class ChainGroundTraces extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private static final int LINE_COUNT = 4;
        private static final int LINE_LIFE = 80;
        private final double[] lineX = new double[LINE_COUNT];
        private final double[] lineZ = new double[LINE_COUNT];
        private final double[] lineAng = new double[LINE_COUNT];
        private final int[] lineStart = new int[LINE_COUNT];

        public ChainGroundTraces(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_ground_traces", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(110);
            config.setDesignType("Ambient atmosphere — chain ground trace echoes (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.3f, 0.5f);
            for (int i = 0; i < LINE_COUNT; i++) {
                lineX[i] = (Math.random() - 0.5) * 10.0;
                lineZ[i] = (Math.random() - 0.5) * 10.0;
                lineAng[i] = Math.random() * Math.PI * 2;
                lineStart[i] = -i * 20;
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float spawnIntensity = 1.0f;
            if (tick < SPAWN_END) spawnIntensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) spawnIntensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            for (int i = 0; i < LINE_COUNT; i++) {
                int age = tick - lineStart[i];
                if (age < 0) continue;
                if (age >= LINE_LIFE) {
                    // respawn line at new position
                    lineX[i] = (Math.random() - 0.5) * 10.0;
                    lineZ[i] = (Math.random() - 0.5) * 10.0;
                    lineAng[i] = Math.random() * Math.PI * 2;
                    lineStart[i] = tick;
                    continue;
                }
                float fade = 1.0f - (age / (float) LINE_LIFE);
                fade *= spawnIntensity;
                if (fade <= 0) continue;
                double dx = Math.cos(lineAng[i]);
                double dz = Math.sin(lineAng[i]);
                // Layer 1: dark trace dust along 1.5 block line
                for (int step = 0; step < 6; step++) {
                    double t = step / 5.0 * 1.5;
                    Location p = c.clone().add(lineX[i] + dx * t, 0.05, lineZ[i] + dz * t);
                    if (tick % 2 == 0) {
                        DisplayBuilder.dustParticles(p, 1, 0.05, 60, 60, 60, 2.0f * fade);
                    }
                }
                // Layer 2: gray smoke at line midpoint
                if (tick % 8 == 0) {
                    Location mid = c.clone().add(lineX[i] + dx * 0.75, 0.15, lineZ[i] + dz * 0.75);
                    w.spawnParticle(Particle.SMOKE, mid, 1, 0.05, 0.02, 0.05, 0.0);
                }
                // Layer 3: subtle electric spark hint
                if (tick % 18 == i % 18) {
                    Location end = c.clone().add(lineX[i] + dx * 1.5, 0.1, lineZ[i] + dz * 1.5);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, end, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainGroundTraces(plugin); }
    }

    // ================================================================
    // 24. DISTANT IMPACT RUMBLES — "The Echo" (DAMAGING)
    //     Random distant explosion clusters at arena walls every 30-60t.
    //     Damage radius covers the burst spot.
    // ================================================================
    public static class DistantImpactRumbles extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private int nextBurst = 25;
        private int burstUntil = -1;

        public DistantImpactRumbles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("distant_impact_rumbles", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyDamagingDefaults(config, 80.0, 4.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(150);
            config.setDesignType("Aggressive atmosphere — distant wall rumbles (timing dodge)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.3f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.3f, 0.4f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;

            // Ambient idle: occasional low rumble dust
            if (tick % 25 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 14.0, 0.1, (Math.random() - 0.5) * 14.0);
                DisplayBuilder.dustParticles(p, 1, 0.5, 100, 100, 100, 1.2f);
            }

            if (tick >= nextBurst && tick < dissipateStart) {
                // Random wall edge position
                double ang = Math.random() * Math.PI * 2;
                double r = 7.0 + Math.random() * 1.5;
                double bx = Math.cos(ang) * r;
                double bz = Math.sin(ang) * r;
                Location p = c.clone().add(bx, 1.0, bz);
                // Layer 1: EXPLOSION cluster (distant-feeling — count 4)
                w.spawnParticle(Particle.EXPLOSION, p, 4, 0.2, 0.2, 0.2, 0.0);
                // Layer 2: LARGE_SMOKE bloom
                w.spawnParticle(Particle.LARGE_SMOKE, p, 8, 0.4, 0.3, 0.4, 0.02);
                // Layer 3: gray dust dust ring
                DisplayBuilder.dustParticles(p, 6, 0.6, 90, 90, 90, 1.6f);
                DisplayBuilder.playSound(p, Sound.ENTITY_IRON_GOLEM_STEP, 0.5f, 0.4f);
                DisplayBuilder.playSound(p, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 0.4f);
                burstUntil = tick + 3;
                nextBurst = tick + 30 + (int) (Math.random() * 30); // 30-60t

                // shift damage center toward the burst so isInCylinderRange catches players near it.
                // Note: we don't actually move the center — radius covers a wide span instead.
            }

            if (tick <= burstUntil) {
                // Continue spewing burst particles for 3t after spawn for visual carry-through
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 0.5, rz);
                w.spawnParticle(Particle.SMOKE, p, 2, 0.3, 0.2, 0.3, 0.01);
            }
        }

        @Override public AbstractAttack newInstance() { return new DistantImpactRumbles(plugin); }
    }

    // ================================================================
    // 25. METALLIC RAIN — "The Downpour" (DAMAGING)
    //     Dense FALLING_DUST(IRON_BLOCK) rain from ceiling.
    // ================================================================
    public static class MetallicRain extends EnvironmentalAttack {
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 40;
        private static BlockData ironData;

        public MetallicRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("metallic_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyDamagingDefaults(config, 120.0, 6.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(180);
            config.setDesignType("Aggressive atmosphere — dense metallic shaving rain (timing dodge)");
        }

        private BlockData ironData() {
            if (ironData == null) ironData = Material.IRON_BLOCK.createBlockData();
            return ironData;
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.6f, 0.7f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.5f, 0.8f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            int count = Math.max(0, (int) (12 * intensity));
            // Layer 1: FALLING_DUST(IRON_BLOCK) from ceiling
            for (int i = 0; i < count; i++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 5.5, rz);
                w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.05, 0.0, 0.05, 0.4, ironData());
            }
            // Layer 2: shaving spark accents mid-fall
            int sparks = Math.max(0, (int) (4 * intensity));
            for (int i = 0; i < sparks; i++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                double ry = 1.0 + Math.random() * 3.0;
                Location p = c.clone().add(rx, ry, rz);
                w.spawnParticle(Particle.CRIT, p, 1, 0.1, 0.2, 0.1, 0.1);
            }
            // Layer 3: ground splash dust
            if (tick % 3 == 0) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 0.1, rz);
                DisplayBuilder.dustParticles(p, 1, 0.3, 180, 180, 200, 1.0f);
            }
            if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.4f, 1.0f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.4f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new MetallicRain(plugin); }
    }

    // ================================================================
    // 26. SHADOW TENDRIL SNAKES — "The Crawl" (ambient)
    //     2 LARGE_SMOKE tendrils that snake along the ground (random walk).
    // ================================================================
    public static class ShadowTendrilSnakes extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private static final int TENDRIL_COUNT = 2;
        private static final int TRAIL_LEN = 14;
        private final double[] headX = new double[TENDRIL_COUNT];
        private final double[] headZ = new double[TENDRIL_COUNT];
        // Trail of past positions: trail[t][step] -> {x, z}
        private final double[][] trailX = new double[TENDRIL_COUNT][TRAIL_LEN];
        private final double[][] trailZ = new double[TENDRIL_COUNT][TRAIL_LEN];
        private final int[] trailHead = new int[TENDRIL_COUNT];

        public ShadowTendrilSnakes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_tendril_snakes", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(140);
            config.setDesignType("Ambient atmosphere — shadow tendril snakes (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.4f, 0.4f);
            for (int t = 0; t < TENDRIL_COUNT; t++) {
                double ang = Math.random() * Math.PI * 2;
                headX[t] = Math.cos(ang) * 7.0;
                headZ[t] = Math.sin(ang) * 7.0;
                for (int i = 0; i < TRAIL_LEN; i++) {
                    trailX[t][i] = headX[t];
                    trailZ[t][i] = headZ[t];
                }
                trailHead[t] = 0;
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int t = 0; t < TENDRIL_COUNT; t++) {
                // Update head every 3 ticks via random walk
                if (tick % 3 == 0) {
                    headX[t] += (Math.random() - 0.5) * 0.4;
                    headZ[t] += (Math.random() - 0.5) * 0.4;
                    // Bounce inside an 8-block radius
                    double d = Math.sqrt(headX[t] * headX[t] + headZ[t] * headZ[t]);
                    if (d > 8.0) {
                        // respawn at random edge
                        double ang = Math.random() * Math.PI * 2;
                        headX[t] = Math.cos(ang) * 7.5;
                        headZ[t] = Math.sin(ang) * 7.5;
                    }
                    // Push into trail buffer
                    trailHead[t] = (trailHead[t] + 1) % TRAIL_LEN;
                    trailX[t][trailHead[t]] = headX[t];
                    trailZ[t][trailHead[t]] = headZ[t];
                }
                // Draw trail with falloff
                for (int i = 0; i < TRAIL_LEN; i++) {
                    int idx = (trailHead[t] - i + TRAIL_LEN) % TRAIL_LEN;
                    float fade = 1.0f - (i / (float) TRAIL_LEN);
                    if (fade <= 0.05f) continue;
                    Location p = c.clone().add(trailX[t][idx], 0.3, trailZ[t][idx]);
                    // Layer 1: LARGE_SMOKE body
                    if (i % 2 == 0) {
                        w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.15 * fade, 0.05, 0.15 * fade, 0.0);
                    }
                    // Layer 2: SMOKE wisp
                    if (i % 3 == 0) {
                        w.spawnParticle(Particle.SMOKE, p.clone().add(0, 0.25, 0), 1, 0.1, 0.1, 0.1, 0.0);
                    }
                    // Layer 3: dark dust shimmer at head
                    if (i == 0) {
                        DisplayBuilder.dustParticles(p, 2, 0.15, 30, 30, 35, 1.5f);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ShadowTendrilSnakes(plugin); }
    }

    // ================================================================
    // 27. IRON AURORA — "The Sky" (ambient)
    //     Horizontal streams of dust drifting high in the arena Y=4-6.
    // ================================================================
    public static class IronAurora extends EnvironmentalAttack {
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 40;

        public IronAurora(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_aurora", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(800);
            config.setCooldownTicks(150);
            config.setDesignType("Ambient atmosphere — iron-grey sky aurora (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.3f, 0.6f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            int count = Math.max(0, (int) (4 * intensity));
            // Layer 1: top stream Y=5, lighter color
            for (int i = 0; i < count; i++) {
                double driftPhase = tick * 0.04 + i * 1.7;
                double rx = (Math.random() - 0.5) * 14.0 + Math.sin(driftPhase) * 0.5;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 5.0, rz);
                DisplayBuilder.dustParticles(p, 1, 0.05, 160, 170, 190, 1.2f);
            }
            // Layer 2: bottom stream Y=4.2, darker color
            for (int i = 0; i < count; i++) {
                double driftPhase = tick * 0.05 - i * 1.3;
                double rx = (Math.random() - 0.5) * 14.0 + Math.cos(driftPhase) * 0.5;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 4.2, rz);
                DisplayBuilder.dustParticles(p, 1, 0.05, 100, 110, 130, 1.0f);
            }
            // Layer 3: shimmer accents
            if (tick % 7 == 0) {
                double rx = (Math.random() - 0.5) * 12.0;
                double rz = (Math.random() - 0.5) * 12.0;
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(rx, 4.6, rz), 1, 0.2, 0.1, 0.2, 0.01);
            }
        }

        @Override public AbstractAttack newInstance() { return new IronAurora(plugin); }
    }

    // ================================================================
    // 28. SHOCKWAVE GROUND RINGS — "The Tremor" (DAMAGING)
    //     Every 40t: expanding particle ring from center r=0.5→4.0 over 10t.
    //     Damage hits when player is inside the active ring.
    // ================================================================
    public static class ShockwaveGroundRings extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private static BlockData ironData;
        private int nextRing = 30;
        private int ringStart = -1;

        public ShockwaveGroundRings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shockwave_ground_rings", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyDamagingDefaults(config, 90.0, 4.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(140);
            config.setDesignType("Aggressive atmosphere — expanding shockwave rings (timing dodge)");
        }

        private BlockData ironData() {
            if (ironData == null) ironData = Material.IRON_BLOCK.createBlockData();
            return ironData;
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.5f, 0.6f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;

            // Trigger a new ring
            if (tick >= nextRing && tick < dissipateStart) {
                ringStart = tick;
                nextRing = tick + 40;
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.6f);
            }

            // Animate active ring (0..10t lifespan)
            if (ringStart >= 0 && tick - ringStart <= 10) {
                int age = tick - ringStart;
                double r = 0.5 + (4.0 - 0.5) * (age / 10.0);
                Location ringCenter = c.clone().add(0, 0.1, 0);
                // Layer 1: BLOCK(IRON_BLOCK) ring outline
                DisplayBuilder.particleRing(ringCenter, r, Particle.BLOCK, 16, ironData());
                // Layer 2: gray dust accent ring
                for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16;
                    Location p = ringCenter.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    if (i % 2 == 0) {
                        DisplayBuilder.dustParticles(p, 1, 0.05, 120, 120, 130, 1.4f);
                    }
                }
                // Layer 3: CRIT impact sparks scattered along ring
                if (age % 2 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8 + age * 0.1;
                        Location p = ringCenter.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                        w.spawnParticle(Particle.CRIT, p, 1, 0.05, 0.05, 0.05, 0.05);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ShockwaveGroundRings(plugin); }
    }

    // ================================================================
    // 29. FORGE EMBER GLOW — "The Heat" (DAMAGING)
    //     4 floor-corner vents with rising FLAME/LAVA hot embers.
    //     Light contact damage if you stand at a vent.
    //     Adds 4 IRON_TRAPDOOR ItemDisplay vent caps as the only solid anchors.
    // ================================================================
    public static class ForgeEmberGlow extends EnvironmentalAttack {
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 40;
        private static final double[][] CORNERS = {
                {  6.0,  6.0 }, {  6.0, -6.0 }, { -6.0,  6.0 }, { -6.0, -6.0 }
        };
        private final List<ItemDisplayHandle> vents = new ArrayList<>();

        public ForgeEmberGlow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("forge_ember_glow", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyDamagingDefaults(config, 85.0, 4.5);
            config.setDurationTicks(800);
            config.setCooldownTicks(150);
            config.setDesignType("Aggressive atmosphere — corner forge ember vents (timing dodge)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.5f, 0.5f);
            for (double[] corner : CORNERS) {
                Location vp = c.clone().add(corner[0], 0.05, corner[1]);
                ItemDisplayHandle h = displayBuilder.spawnItem(vp, new ItemStack(Material.IRON_TRAPDOOR));
                h.scale(0.001f, 0.001f, 0.001f).glow(255, 140, 40).interpolation(2, 0);
                vents.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float intensity = 1.0f;
            if (tick < SPAWN_END) intensity = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) intensity = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Animate vent caps to full scale
            float ventScale = 0.7f * intensity;
            for (int i = 0; i < vents.size(); i++) {
                double[] corner = CORNERS[i];
                vents.get(i).animateTo(
                        new Vector3f((float) corner[0] - ventScale / 2, 0.05f, (float) corner[1] - ventScale / 2),
                        new AxisAngle4f((float) (tick * 0.02), 0, 1, 0),
                        new Vector3f(ventScale, 0.05f, ventScale), 4);

                Location p = c.clone().add(corner[0], 0.3, corner[1]);
                // Layer 1: FLAME rising
                int flameCount = Math.max(0, (int) (6 * intensity));
                w.spawnParticle(Particle.FLAME, p, flameCount, 0.2, 0.1, 0.2, 0.04);
                // Layer 2: LAVA droplet accent
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.LAVA, p, 1, 0.1, 0.05, 0.1, 0.0);
                }
                // Layer 3: SMALL_FLAME / SOUL_FIRE_FLAME accent + orange dust
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.SMALL_FLAME, p.clone().add(0, 0.3, 0), 2, 0.15, 0.2, 0.15, 0.03);
                }
                if (tick % 5 == 0) {
                    DisplayBuilder.dustParticles(p.clone().add(0, 0.5, 0), 1, 0.25, 255, 160, 50, 1.4f);
                }
                // Bonus: hot smoke top
                if (tick % 6 == 0) {
                    w.spawnParticle(Particle.LARGE_SMOKE, p.clone().add(0, 1.0, 0), 1, 0.2, 0.2, 0.2, 0.02);
                }
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 0.8f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.4f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new ForgeEmberGlow(plugin); }
    }

    // ================================================================
    // 30. CHAIN FRAGMENT SHOWER — "The Light" (ambient)
    //     Every 25t: 6 ITEM(CHAIN) particles burst from random mid-air position.
    // ================================================================
    public static class ChainFragmentShower extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private int nextBurst = 25;
        private static ItemStack chainStack;

        public ChainFragmentShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_fragment_shower", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(110);
            config.setDesignType("Ambient atmosphere — random chain fragment bursts (no damage)");
        }

        private ItemStack chainStack() {
            if (chainStack == null) chainStack = new ItemStack(Material.CHAIN);
            return chainStack;
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.3f, 1.4f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;

            if (tick >= nextBurst && tick < dissipateStart) {
                double rx = (Math.random() - 0.5) * 12.0;
                double rz = (Math.random() - 0.5) * 12.0;
                Location p = c.clone().add(rx, 2.5, rz);
                // Layer 1: ITEM(CHAIN) bursts — 6 fragments
                w.spawnParticle(Particle.ITEM, p, 6, 0.4, 0.4, 0.4, 0.2, chainStack());
                // Layer 2: ELECTRIC_SPARK at burst origin
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.3, 0.3, 0.3, 0.05);
                // Layer 3: gray dust shimmer
                DisplayBuilder.dustParticles(p, 3, 0.4, 130, 130, 140, 1.3f);
                DisplayBuilder.playSound(p, Sound.BLOCK_CHAIN_HIT, 0.5f, 1.1f + (float) Math.random() * 0.3f);
                nextBurst = tick + 25;
            }
            // Ambient idle: occasional faint chain-link wisp
            if (tick % 18 == 0) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 2.0 + Math.random() * 1.5, rz);
                DisplayBuilder.dustParticles(p, 1, 0.0, 100, 100, 110, 1.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainFragmentShower(plugin); }
    }
}
