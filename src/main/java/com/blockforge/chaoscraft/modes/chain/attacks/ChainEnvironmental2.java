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
 * Chain Mode — ENVIRONMENTAL ATTACKS 16-30 (Category 2 — Atmospheric Particles)
 *
 * === VISIBILITY OVERHAUL ===
 * Player feedback: "too weak / invisible — sparse particles (~1/tick), tiny or
 * missing ItemDisplays." This batch is rebuilt to the SAME dense+visible
 * standard as ChainEnvironmental (attacks 1-15):
 *
 *   - Every attack now carries solid ItemDisplay focal points (floating CHAIN
 *     segments scaled 0.4 x 1.2 x 0.4 thick, IRON_BARS, LANTERN, IRON_BLOCK /
 *     NETHERITE_BLOCK cores, ANVIL, IRON_TRAPDOOR vents) so there is always
 *     something solid on screen — NO BlockDisplays, ItemDisplays only.
 *   - Heavy multi-layer particles: 3+ layers, each 5-15 particles per tick
 *     (the old ~1/tick passes are gone). CRIT / ELECTRIC_SPARK / SMOKE /
 *     LARGE_SMOKE / SOUL_FIRE_FLAME / DUST / END_ROD / FLAME / WITCH.
 *   - Every ItemDisplay glows: industrial rust-orange (200,110,40) for iron
 *     themes, amethyst-purple (170,80,230) for energy themes.
 *   - Nothing is static — every display drifts / bobs / rotates / sways
 *     continuously via animateTo.
 *
 * chain_energy_wisps (the standout offender) is now a dense crackling energy
 * field: 10 floating CHAIN ItemDisplays drifting + slowly rotating with an
 * amethyst glow, plus 12+ ELECTRIC_SPARK + END_ROD + WITCH particles per tick
 * swirling between them.
 *
 * Choreography per attack:
 *   PHASE 1 — SPAWN     : scale 0 -> full + buildup particles + atmospheric sound
 *   PHASE 2 — ACTIVE    : continuous animateTo motion + heavy layered particle loop
 *   PHASE 3 — DISSIPATE : tail-off particles, soft close-out sound
 *
 * 5 of 15 are damaging (timing-dodge style aggressive atmospheres):
 *   21 Electric Arc Flickers, 24 Distant Impact Rumbles, 25 Metallic Rain,
 *   28 Shockwave Ground Rings, 29 Forge Ember Glow.
 *
 * 10 of 15 are pure ambient (no damage): 16, 17, 18, 19, 20, 22, 23, 26, 27, 30.
 *
 * ItemDisplays only. NO BlockDisplays. NO potion effects. Modern Particle enum
 * names only (SMOKE / LARGE_SMOKE / EXPLOSION / DUST / FALLING_DUST / BLOCK).
 */
public final class ChainEnvironmental2 {
    private ChainEnvironmental2() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    // Glow palettes (visibility) — shared with ChainEnvironmental (1-15)
    private static final int[] RUST = {200, 110, 40};       // industrial rust-orange
    private static final int[] IRON = {210, 210, 230};      // bright iron
    private static final int[] AMETHYST = {170, 80, 230};   // energy purple
    private static final int[] LANTERN_GLOW = {255, 180, 80};
    private static final int[] DARK_IRON = {120, 120, 150};

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

    /**
     * HEAVY layered industrial particle pass at a setpiece anchor — rust dust
     * drip + industrial smoke + electric sparks + crit shimmer. 4 layers,
     * several particles each (not ~1/tick).
     */
    private static void ambientParticles(World w, Location p, int tick) {
        DisplayBuilder.dustParticles(p.clone().add(0, -0.2, 0), 5, 0.3, 200, 110, 40, 1.5f);
        w.spawnParticle(Particle.SMOKE, p, 4, 0.3, 0.25, 0.3, 0.01);
        if (tick % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 5, 0.25, 0.2, 0.25, 0.02);
        if (tick % 5 == 0) w.spawnParticle(Particle.CRIT, p, 4, 0.25, 0.2, 0.25, 0.05);
    }

    /** Rust drip beneath a chain/iron segment — the "everything is rusting" look. */
    private static void rustDrip(World w, Location p, int tick) {
        DisplayBuilder.dustParticles(p.clone().add(0, -0.4, 0), 3, 0.12, 190, 100, 35, 1.3f);
        if (tick % 3 == 0) w.spawnParticle(Particle.FALLING_DUST, p.clone().add(0, -0.5, 0), 1, 0.08, 0.1, 0.08, 0.0,
                Material.IRON_BLOCK.createBlockData());
        if (tick % 6 == 0) w.spawnParticle(Particle.CRIT, p, 3, 0.1, 0.15, 0.1, 0.03);
    }

    // ================================================================
    // 16. IRON DUST MIST — "The Haze" (ambient)
    //     Dense iron dust mist filling arena air Y=0.5-3.5 PLUS 8 slowly
    //     drifting IRON_BARS haze panels glowing rust-orange so the haze has
    //     solid focal points instead of being faint dots.
    // ================================================================
    public static class IronDustMist extends EnvironmentalAttack {
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 40;
        private static final int PANELS = 8;
        private final List<ItemDisplayHandle> panels = new ArrayList<>();
        private final double[] px = new double[PANELS];
        private final double[] py = new double[PANELS];
        private final double[] pz = new double[PANELS];
        private final double[] period = new double[PANELS];
        private final double[] phase = new double[PANELS];

        public IronDustMist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_dust_mist", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(120);
            config.setDesignType("Ambient atmosphere — iron dust mist (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.5f);
            for (int i = 0; i < PANELS; i++) {
                double ang = Math.PI * 2 * i / PANELS + Math.random() * 0.3;
                double r = 5.0 + Math.random() * 6.0;
                px[i] = Math.cos(ang) * r;
                py[i] = 1.5 + Math.random() * 2.0;
                pz[i] = Math.sin(ang) * r;
                period[i] = 90 + Math.random() * 50;
                phase[i] = Math.random() * Math.PI * 2;
                Location lp = c.clone().add(px[i], py[i], pz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.IRON_BARS));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                panels.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Drifting IRON_BARS haze panels (sway + bob + slow yaw)
            float ps = 1.4f * fade, psy = 1.0f * fade;
            for (int i = 0; i < PANELS; i++) {
                double drift = Math.sin(tick / period[i] * Math.PI * 2 + phase[i]) * 1.2;
                double bob = Math.cos(tick / (period[i] * 0.7) * Math.PI * 2 + phase[i]) * 0.4;
                panels.get(i).animateTo(
                        new Vector3f((float)(px[i] + drift) - ps / 2, (float)(py[i] + bob) - psy / 2, (float)pz[i] - ps / 2),
                        new AxisAngle4f((float)(tick * 0.015 + i), 0, 1, 0),
                        new Vector3f(ps, psy, ps), 6);
                if (i % 2 == tick % 2) {
                    Location ap = c.clone().add(px[i] + drift, py[i] + bob, pz[i]);
                    DisplayBuilder.dustParticles(ap, 6, 0.6, 150, 150, 155, 1.8f);
                    w.spawnParticle(Particle.SMOKE, ap, 4, 0.5, 0.4, 0.5, 0.01);
                }
            }

            int density = Math.max(0, (int) (14 * fade));
            // Layer 1: gray iron dust filling the arena air
            for (int i = 0; i < density; i++) {
                double rx = (Math.random() - 0.5) * 18.0;
                double rz = (Math.random() - 0.5) * 18.0;
                double ry = 0.5 + Math.random() * 3.0;
                DisplayBuilder.dustParticles(c.clone().add(rx, ry, rz), 1, 0.0, 140, 140, 140, 2.0f);
            }
            // Layer 2: bright iron sparkle accents
            for (int i = 0; i < 6; i++) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                DisplayBuilder.dustParticles(c.clone().add(rx, 1.5 + Math.random() * 1.5, rz), 1, 0.0, 200, 200, 210, 0.9f);
            }
            // Layer 3: industrial smoke haze
            for (int i = 0; i < 6; i++) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                w.spawnParticle(Particle.SMOKE, c.clone().add(rx, 1.2 + Math.random() * 1.5, rz), 1, 0.15, 0.15, 0.15, 0.0);
            }
            if (tick == SPAWN_END) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.4f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.4f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronDustMist(plugin); }
    }

    // ================================================================
    // 17. RISING SMOKE COLUMNS — "The Forges" (ambient)
    //     4 corner forges, each = ANVIL base + 2 CHAIN uprights, with thick
    //     rising smoke columns (Y=0..6) and forge flame/ember at the base.
    // ================================================================
    public static class RisingSmokeColumns extends EnvironmentalAttack {
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 40;
        private static final double[][] CORNERS = {
                {  8.0,  8.0 }, {  8.0, -8.0 }, { -8.0,  8.0 }, { -8.0, -8.0 }
        };
        private final List<ItemDisplayHandle> anvils = new ArrayList<>();
        private final List<ItemDisplayHandle> uprights = new ArrayList<>(); // 2 per corner

        public RisingSmokeColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rising_smoke_columns", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(800);
            config.setCooldownTicks(140);
            config.setDesignType("Ambient atmosphere — corner forge smoke columns (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.6f, 0.5f);
            for (double[] corner : CORNERS) {
                Location bp = c.clone().add(corner[0], 0.3, corner[1]);
                ItemDisplayHandle anvil = displayBuilder.spawnItem(bp, new ItemStack(Material.ANVIL));
                anvil.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                anvils.add(anvil);
                spawnedEntities.add(anvil.entity());
                for (int k = 0; k < 2; k++) {
                    Location up = c.clone().add(corner[0], 1.2 + k * 1.1, corner[1]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(up, new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    uprights.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Animate forge bases + chain uprights
            float anvilScale = 0.7f * fade;
            float chXZ = 0.42f * fade, chY = 1.0f * fade;
            for (int ci = 0; ci < CORNERS.length; ci++) {
                double[] corner = CORNERS[ci];
                anvils.get(ci).animateTo(
                        new Vector3f((float)corner[0] - anvilScale / 2, 0.3f - anvilScale / 2, (float)corner[1] - anvilScale / 2),
                        new AxisAngle4f((float)(tick * 0.01), 0, 1, 0),
                        new Vector3f(anvilScale), 5);
                for (int k = 0; k < 2; k++) {
                    double cy = 1.2 + k * 1.1;
                    double sway = Math.toRadians(4.0 * Math.sin(tick / 70.0 * Math.PI * 2 + ci + k));
                    uprights.get(ci * 2 + k).animateTo(
                            new Vector3f((float)corner[0] - chXZ / 2, (float)cy - chY / 2, (float)corner[1] - chXZ / 2),
                            new AxisAngle4f((float) sway, 0, 0, 1),
                            new Vector3f(chXZ, chY, chXZ), 5);
                }

                // Thick rising smoke column
                for (double y = 0; y <= 6.0; y += 0.45) {
                    if (Math.random() > 0.65 * fade) continue;
                    Location p = c.clone().add(corner[0] + (Math.random() - 0.5) * 0.5, y, corner[1] + (Math.random() - 0.5) * 0.5);
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p, 2, 0.08, 0.08, 0.08, 0.02);
                }
                // Soot dust accents
                Location soot = c.clone().add(corner[0], 0.5 + Math.random() * 1.5, corner[1]);
                DisplayBuilder.dustParticles(soot, 5, 0.25, 70, 70, 70, 1.6f);
                // Forge flame + ember at base
                Location base = c.clone().add(corner[0], 0.4, corner[1]);
                w.spawnParticle(Particle.FLAME, base, 6, 0.25, 0.1, 0.25, 0.02);
                if (tick % 3 == 0) w.spawnParticle(Particle.LAVA, base, 1, 0.15, 0.05, 0.15, 0.0);
                if (tick % 4 == 0) DisplayBuilder.dustParticles(base.clone().add(0, 0.4, 0), 4, 0.2, 255, 150, 50, 1.4f);
            }
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 0.7f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.4f, 0.4f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new RisingSmokeColumns(plugin); }
    }

    // ================================================================
    // 18. METAL SPARK DRIFTS — "The Embers" (ambient)
    //     6 drifting LANTERN ember-nodes (warm glow) shedding dense forge
    //     sparks every tick — CRIT + ELECTRIC_SPARK + warm orange dust.
    // ================================================================
    public static class MetalSparkDrifts extends EnvironmentalAttack {
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 30;
        private static final int NODES = 6;
        private final List<ItemDisplayHandle> nodes = new ArrayList<>();
        private final double[] nx = new double[NODES];
        private final double[] ny = new double[NODES];
        private final double[] nz = new double[NODES];
        private final double[] period = new double[NODES];
        private final double[] phase = new double[NODES];

        public MetalSparkDrifts(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("metal_spark_drifts", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(110);
            config.setDesignType("Ambient atmosphere — drifting metal sparks (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.4f, 1.4f);
            for (int i = 0; i < NODES; i++) {
                double ang = Math.PI * 2 * i / NODES + Math.random() * 0.4;
                double r = 4.0 + Math.random() * 5.0;
                nx[i] = Math.cos(ang) * r;
                ny[i] = 1.8 + Math.random() * 1.5;
                nz[i] = Math.sin(ang) * r;
                period[i] = 70 + Math.random() * 40;
                phase[i] = Math.random() * Math.PI * 2;
                Location lp = c.clone().add(nx[i], ny[i], nz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.LANTERN));
                h.scale(0.001f, 0.001f, 0.001f).glow(LANTERN_GLOW[0], LANTERN_GLOW[1], LANTERN_GLOW[2]).interpolation(2, 0);
                nodes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float ns = 0.55f * fade;
            for (int i = 0; i < NODES; i++) {
                double driftX = Math.sin(tick / period[i] * Math.PI * 2 + phase[i]) * 1.5;
                double driftZ = Math.cos(tick / (period[i] * 0.85) * Math.PI * 2 + phase[i]) * 1.5;
                double bob = Math.sin(tick / 40.0 * Math.PI * 2 + i) * 0.3;
                double cx = nx[i] + driftX, cy = ny[i] + bob, cz = nz[i] + driftZ;
                nodes.get(i).animateTo(
                        new Vector3f((float)cx - ns / 2, (float)cy - ns / 2, (float)cz - ns / 2),
                        new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0),
                        new Vector3f(ns), 4);
                Location p = c.clone().add(cx, cy, cz);
                // Dense forge sparks shedding off each node
                w.spawnParticle(Particle.CRIT, p, 6, 0.3, 0.25, 0.3, 0.06);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 5, 0.3, 0.25, 0.3, 0.05);
                DisplayBuilder.dustParticles(p, 5, 0.3, 255, 160, 60, 1.3f);
                if (tick % 4 == 0) w.spawnParticle(Particle.FLAME, p, 2, 0.15, 0.15, 0.15, 0.01);
            }
            // Loose drifting sparks across the arena between nodes
            for (int s = 0; s < 6; s++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                double ry = 1.5 + Math.random() * 1.5;
                Location p = c.clone().add(rx, ry, rz);
                w.spawnParticle(Particle.CRIT, p, 1, 0.2, 0.2, 0.2, 0.05);
                if (s % 2 == 0) DisplayBuilder.dustParticles(p, 1, 0.2, 255, 150, 55, 1.0f);
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 1.1f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MetalSparkDrifts(plugin); }
    }

    // ================================================================
    // 19. CHAIN ENERGY WISPS — "The Static" (ambient)
    //     === FLAGSHIP REBUILD ===  Dense crackling chain-energy field:
    //     10 floating CHAIN ItemDisplays drift + slowly rotate with an
    //     amethyst-purple glow, and 12+ ELECTRIC_SPARK + END_ROD + WITCH
    //     particles swirl between them every tick. Fills the area with
    //     visible energy instead of faint dots.
    // ================================================================
    public static class ChainEnergyWisps extends EnvironmentalAttack {
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 35;
        private static final int WISPS = 10;
        private final List<ItemDisplayHandle> wisps = new ArrayList<>();
        private final double[] wx = new double[WISPS];
        private final double[] wy = new double[WISPS];
        private final double[] wz = new double[WISPS];
        private final double[] orbitR = new double[WISPS];
        private final double[] orbitSpeed = new double[WISPS];
        private final double[] phase = new double[WISPS];

        public ChainEnergyWisps(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_energy_wisps", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(120);
            config.setDesignType("Ambient atmosphere — dense chain energy field (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.6f);
            for (int i = 0; i < WISPS; i++) {
                double ang = Math.PI * 2 * i / WISPS;
                orbitR[i] = 3.0 + Math.random() * 5.0;
                orbitSpeed[i] = (0.012 + Math.random() * 0.02) * (i % 2 == 0 ? 1 : -1);
                phase[i] = ang;
                wy[i] = 1.5 + Math.random() * 2.5;
                wx[i] = Math.cos(ang) * orbitR[i];
                wz[i] = Math.sin(ang) * orbitR[i];
                Location lp = c.clone().add(wx[i], wy[i], wz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(AMETHYST[0], AMETHYST[1], AMETHYST[2]).interpolation(2, 0);
                wisps.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // THICK floating chains drifting + slowly rotating around the field
            float sX = 0.42f * fade, sY = 1.2f * fade, sZ = 0.42f * fade;
            double[] cx = new double[WISPS], cy = new double[WISPS], cz = new double[WISPS];
            for (int i = 0; i < WISPS; i++) {
                double a = phase[i] + tick * orbitSpeed[i];
                cx[i] = Math.cos(a) * orbitR[i];
                cz[i] = Math.sin(a) * orbitR[i];
                cy[i] = wy[i] + Math.sin(tick / 35.0 * Math.PI * 2 + i) * 0.5;
                wisps.get(i).animateTo(
                        new Vector3f((float)cx[i] - sX / 2, (float)cy[i] - sY / 2, (float)cz[i] - sZ / 2),
                        new AxisAngle4f((float)(tick * 0.05 + i * 0.6), 0.4f, 1, 0.2f),
                        new Vector3f(sX, sY, sZ), 4);
                Location p = c.clone().add(cx[i], cy[i], cz[i]);
                // Energy crackle hugging each chain
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 8, 0.25, 0.4, 0.25, 0.04);
                w.spawnParticle(Particle.END_ROD, p, 2, 0.15, 0.3, 0.15, 0.01);
                DisplayBuilder.dustParticles(p, 5, 0.25, AMETHYST[0], AMETHYST[1], AMETHYST[2], 1.5f);
                if (tick % 3 == 0) w.spawnParticle(Particle.WITCH, p, 3, 0.25, 0.3, 0.25, 0.02);
            }

            // Arcs of energy between adjacent chains (END_ROD/spark line)
            for (int i = 0; i < WISPS; i++) {
                int j = (i + 1) % WISPS;
                for (int s = 1; s < 4; s++) {
                    double t = s / 4.0;
                    Location lp = c.clone().add(
                            cx[i] + (cx[j] - cx[i]) * t,
                            cy[i] + (cy[j] - cy[i]) * t,
                            cz[i] + (cz[j] - cz[i]) * t);
                    if ((tick + i) % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, lp, 2, 0.1, 0.1, 0.1, 0.03);
                    if ((tick + i) % 5 == 0) DisplayBuilder.dustParticles(lp, 1, 0.05, 200, 120, 255, 1.0f);
                }
            }

            // Ambient swirling energy filling the whole field
            for (int s = 0; s < 10; s++) {
                double ra = Math.random() * Math.PI * 2;
                double rr = Math.random() * 8.0;
                double ry = 1.0 + Math.random() * 3.0;
                Location p = c.clone().add(Math.cos(ra) * rr, ry, Math.sin(ra) * rr);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.03);
                if (s % 2 == 0) w.spawnParticle(Particle.WITCH, p, 1, 0.1, 0.1, 0.1, 0.01);
                if (s % 3 == 0) DisplayBuilder.dustParticles(p, 1, 0.0, 150, 90, 220, 1.2f);
            }
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.7f);
            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.7f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainEnergyWisps(plugin); }
    }

    // ================================================================
    // 20. RUST FLAKE FALL — "The Decay" (ambient)
    //     Dense RED_TERRACOTTA falling flakes from ceiling, PLUS 6 corroded
    //     IRON_BARS panels high up that the flakes appear to shed from.
    // ================================================================
    public static class RustFlakeFall extends EnvironmentalAttack {
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 30;
        private static final int PANELS = 6;
        private static BlockData rustData;
        private final List<ItemDisplayHandle> panels = new ArrayList<>();
        private final double[] px = new double[PANELS];
        private final double[] pz = new double[PANELS];
        private final double[] phase = new double[PANELS];

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
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRAVEL_FALL, 0.6f, 0.7f);
            for (int i = 0; i < PANELS; i++) {
                double ang = Math.PI * 2 * i / PANELS + Math.random() * 0.4;
                double r = 4.0 + Math.random() * 5.0;
                px[i] = Math.cos(ang) * r;
                pz[i] = Math.sin(ang) * r;
                phase[i] = Math.random() * Math.PI * 2;
                Location lp = c.clone().add(px[i], 5.2, pz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.IRON_BARS));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                panels.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Corroded ceiling panels — slight sway/bob, shed rust
            float ps = 1.2f * fade, psy = 0.9f * fade;
            for (int i = 0; i < PANELS; i++) {
                double bob = Math.sin(tick / 60.0 * Math.PI * 2 + phase[i]) * 0.2;
                panels.get(i).animateTo(
                        new Vector3f((float)px[i] - ps / 2, (float)(5.2 + bob) - psy / 2, (float)pz[i] - ps / 2),
                        new AxisAngle4f((float)(tick * 0.01 + i), 0, 1, 0),
                        new Vector3f(ps, psy, ps), 5);
                Location p = c.clone().add(px[i], 5.0 + bob, pz[i]);
                if (i % 2 == tick % 2) rustDrip(w, p, tick);
            }

            int count = Math.max(0, (int) (16 * fade));
            // Layer 1: FALLING_DUST(RED_TERRACOTTA) from ceiling
            for (int i = 0; i < count; i++) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                w.spawnParticle(Particle.FALLING_DUST, c.clone().add(rx, 5.0, rz), 1, 0.1, 0.0, 0.1, 0.0, rustData());
            }
            // Layer 2: rust-tinted dust drift at mid height
            for (int i = 0; i < 6; i++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                DisplayBuilder.dustParticles(c.clone().add(rx, 2.5, rz), 1, 0.2, 160, 70, 40, 1.2f);
            }
            // Layer 3: ground settle dust
            for (int i = 0; i < 5; i++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                DisplayBuilder.dustParticles(c.clone().add(rx, 0.15, rz), 1, 0.25, 120, 60, 30, 1.4f);
            }
            if (tick % 100 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRAVEL_FALL, 0.4f, 0.7f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new RustFlakeFall(plugin); }
    }

    // ================================================================
    // 21. ELECTRIC ARC FLICKERS — "The Charge" (DAMAGING)
    //     Burst-events of electric arcs at random arena points. Now anchored
    //     by 5 floating IRON_BARS conductor pylons (amethyst glow) that the
    //     arcs jump between; a brilliant arc column erupts on each burst.
    //     Player caught in the arc-radius during a flicker takes damage.
    // ================================================================
    public static class ElectricArcFlickers extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private static final int PYLONS = 5;
        private double arcX = 0.0, arcZ = 0.0;
        private int nextBurst = 30;
        private int burstUntil = -1;
        private final List<ItemDisplayHandle> pylons = new ArrayList<>();
        private final double[] px = new double[PYLONS];
        private final double[] pz = new double[PYLONS];

        public ElectricArcFlickers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("electric_arc_flickers", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyDamagingDefaults(config, 100.0, 4.5);
            config.setDurationTicks(700);
            config.setCooldownTicks(140);
            config.setDesignType("Aggressive atmosphere — electric arc bursts (timing dodge)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.3f);
            DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.6f);
            for (int i = 0; i < PYLONS; i++) {
                double ang = Math.PI * 2 * i / PYLONS;
                double r = 6.0;
                px[i] = Math.cos(ang) * r;
                pz[i] = Math.sin(ang) * r;
                Location lp = c.clone().add(px[i], 2.0, pz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.IRON_BARS));
                h.scale(0.001f, 0.001f, 0.001f).glow(AMETHYST[0], AMETHYST[1], AMETHYST[2]).interpolation(2, 0);
                pylons.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Conductor pylons — tall thin glowing bars, gentle hum-bob, constant crackle
            float ps = 0.5f * fade, psy = 2.2f * fade;
            for (int i = 0; i < PYLONS; i++) {
                double bob = Math.sin(tick / 30.0 * Math.PI * 2 + i) * 0.15;
                pylons.get(i).animateTo(
                        new Vector3f((float)px[i] - ps / 2, (float)(2.0 + bob) - psy / 2, (float)pz[i] - ps / 2),
                        new AxisAngle4f((float)(tick * 0.03 + i), 0, 1, 0),
                        new Vector3f(ps, psy, ps), 4);
                Location p = c.clone().add(px[i], 2.0 + bob, pz[i]);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 6, 0.2, 0.6, 0.2, 0.03);
                DisplayBuilder.dustParticles(p, 3, 0.25, AMETHYST[0], AMETHYST[1], AMETHYST[2], 1.3f);
                if (tick % 4 == 0) w.spawnParticle(Particle.END_ROD, p, 1, 0.1, 0.4, 0.1, 0.01);
            }
            // Idle arcs between adjacent pylons (visible energy lattice)
            for (int i = 0; i < PYLONS; i++) {
                int j = (i + 1) % PYLONS;
                for (int s = 1; s < 5; s++) {
                    double t = s / 5.0;
                    Location lp = c.clone().add(px[i] + (px[j] - px[i]) * t, 2.0, pz[i] + (pz[j] - pz[i]) * t);
                    if ((tick + i) % 3 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, lp, 1, 0.1, 0.2, 0.1, 0.02);
                }
            }

            // Trigger a burst
            if (tick >= nextBurst && tick < dissipateStart) {
                arcX = (Math.random() - 0.5) * 14.0;
                arcZ = (Math.random() - 0.5) * 14.0;
                burstUntil = tick + 4;
                nextBurst = tick + 40 + (int) (Math.random() * 50); // 40-90t
                Location p = c.clone().add(arcX, 1.5, arcZ);
                DisplayBuilder.playSound(p, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.7f);
                DisplayBuilder.playSound(p, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.4f);
            }

            // Active burst — brilliant dense arc + tall column
            if (tick <= burstUntil) {
                Location p = c.clone().add(arcX, 1.5, arcZ);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 30, 0.4, 0.4, 0.4, 0.18);
                w.spawnParticle(Particle.CRIT, p, 14, 0.4, 0.4, 0.4, 0.22);
                w.spawnParticle(Particle.END_ROD, p, 6, 0.3, 0.3, 0.3, 0.05);
                DisplayBuilder.dustParticles(p, 10, 0.5, 180, 200, 255, 1.5f);
                for (int y = 0; y < 6; y++) {
                    Location col = p.clone().add(0, y * 0.5, 0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, col, 3, 0.08, 0.08, 0.08, 0.05);
                    if (y % 2 == 0) DisplayBuilder.dustParticles(col, 2, 0.1, 200, 120, 255, 1.2f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ElectricArcFlickers(plugin); }
    }

    // ================================================================
    // 22. GROUND IRON FOG — "The Creep" (ambient)
    //     Dense low-lying fog across the arena floor PLUS 8 half-buried
    //     IRON_TRAPDOOR fog vents that the fog appears to seep from.
    // ================================================================
    public static class GroundIronFog extends EnvironmentalAttack {
        private static final int SPAWN_END = 30;
        private static final int DISSIPATE_LEN = 40;
        private static final int VENTS = 8;
        private final List<ItemDisplayHandle> vents = new ArrayList<>();
        private final double[] vx = new double[VENTS];
        private final double[] vz = new double[VENTS];

        public GroundIronFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_iron_fog", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(800);
            config.setCooldownTicks(150);
            config.setDesignType("Ambient atmosphere — low ground iron fog (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.6f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.5f, 0.5f);
            for (int i = 0; i < VENTS; i++) {
                double ang = Math.PI * 2 * i / VENTS + Math.random() * 0.4;
                double r = 3.0 + Math.random() * 6.0;
                vx[i] = Math.cos(ang) * r;
                vz[i] = Math.sin(ang) * r;
                Location lp = c.clone().add(vx[i], 0.05, vz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.IRON_TRAPDOOR));
                h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                vents.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Slowly rotating fog vents seeping dense smoke
            float vs = 0.85f * fade;
            for (int i = 0; i < VENTS; i++) {
                vents.get(i).animateTo(
                        new Vector3f((float)vx[i] - vs / 2, 0.05f, (float)vz[i] - vs / 2),
                        new AxisAngle4f((float)(tick * 0.02 + i), 0, 1, 0),
                        new Vector3f(vs, 0.1f, vs), 5);
                Location p = c.clone().add(vx[i], 0.25, vz[i]);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 3, 0.4, 0.05, 0.4, 0.005);
                w.spawnParticle(Particle.SMOKE, p, 4, 0.5, 0.05, 0.5, 0.0);
                if (tick % 4 == 0) DisplayBuilder.dustParticles(p, 3, 0.4, 90, 90, 95, 1.5f);
            }

            int count = Math.max(0, (int) (24 * fade));
            // Layer 1: SMOKE blanket near floor (wide spread)
            for (int i = 0; i < count; i++) {
                double rx = (Math.random() - 0.5) * 16.0;
                double rz = (Math.random() - 0.5) * 16.0;
                w.spawnParticle(Particle.SMOKE, c.clone().add(rx, 0.2, rz), 1, 2.0, 0.05, 2.0, 0.0);
            }
            // Layer 2: LARGE_SMOKE dense patches
            for (int i = 0; i < 8; i++) {
                double rx = (Math.random() - 0.5) * 12.0;
                double rz = (Math.random() - 0.5) * 12.0;
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(rx, 0.3, rz), 1, 0.5, 0.05, 0.5, 0.0);
            }
            // Layer 3: muted gray dust accents
            for (int i = 0; i < 6; i++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                DisplayBuilder.dustParticles(c.clone().add(rx, 0.15, rz), 1, 0.2, 90, 90, 95, 1.5f);
            }
            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.4f, 0.4f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GroundIronFog(plugin); }
    }

    // ================================================================
    // 23. CHAIN GROUND TRACES — "The Marks" (ambient)
    //     Ghost-echo chain lines on the ground. Each trace is now reinforced
    //     by a small dragging CHAIN ItemDisplay laid flat at its head, glowing
    //     amethyst, with dense trace dust + soul-fire shimmer along the line.
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
        private final List<ItemDisplayHandle> markers = new ArrayList<>();

        public ChainGroundTraces(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_ground_traces", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(110);
            config.setDesignType("Ambient atmosphere — chain ground trace echoes (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.7f);
            for (int i = 0; i < LINE_COUNT; i++) {
                lineX[i] = (Math.random() - 0.5) * 10.0;
                lineZ[i] = (Math.random() - 0.5) * 10.0;
                lineAng[i] = Math.random() * Math.PI * 2;
                lineStart[i] = -i * 20;
                Location lp = c.clone().add(lineX[i], 0.1, lineZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(AMETHYST[0], AMETHYST[1], AMETHYST[2]).interpolation(2, 0);
                markers.add(h);
                spawnedEntities.add(h.entity());
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
                    lineX[i] = (Math.random() - 0.5) * 10.0;
                    lineZ[i] = (Math.random() - 0.5) * 10.0;
                    lineAng[i] = Math.random() * Math.PI * 2;
                    lineStart[i] = tick;
                    DisplayBuilder.playSound(c.clone().add(lineX[i], 0.1, lineZ[i]), Sound.BLOCK_CHAIN_STEP, 0.4f, 0.6f);
                    continue;
                }
                float lifeFade = 1.0f - (age / (float) LINE_LIFE);
                lifeFade *= spawnIntensity;
                if (lifeFade <= 0) continue;
                double dx = Math.cos(lineAng[i]);
                double dz = Math.sin(lineAng[i]);

                // Dragging CHAIN marker laid flat (rotates around vertical to align to the line)
                float ms = 0.4f * lifeFade, msy = 1.0f * lifeFade;
                Location head = c.clone().add(lineX[i] + dx * 1.5, 0.1, lineZ[i] + dz * 1.5);
                markers.get(i).animateTo(
                        new Vector3f((float)(lineX[i] + dx * 1.5) - ms / 2, 0.1f, (float)(lineZ[i] + dz * 1.5) - ms / 2),
                        new AxisAngle4f((float)(lineAng[i] + Math.PI / 2), 1, 0, 0),
                        new Vector3f(ms, msy, ms), 4);

                // Layer 1: dense trace dust along the 1.5-block line
                for (int step = 0; step < 8; step++) {
                    double t = step / 7.0 * 1.5;
                    Location p = c.clone().add(lineX[i] + dx * t, 0.06, lineZ[i] + dz * t);
                    DisplayBuilder.dustParticles(p, 2, 0.05, 70, 60, 90, 2.0f * lifeFade);
                }
                // Layer 2: soul-fire shimmer along the trace
                if (tick % 3 == 0) {
                    Location mid = c.clone().add(lineX[i] + dx * 0.75, 0.15, lineZ[i] + dz * 0.75);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, mid, 3, 0.1, 0.05, 0.1, 0.005);
                    w.spawnParticle(Particle.SMOKE, mid, 2, 0.1, 0.05, 0.1, 0.0);
                }
                // Layer 3: electric spark crackle at the head
                w.spawnParticle(Particle.ELECTRIC_SPARK, head, 4, 0.1, 0.1, 0.1, 0.02);
                DisplayBuilder.dustParticles(head, 3, 0.12, AMETHYST[0], AMETHYST[1], AMETHYST[2], 1.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainGroundTraces(plugin); }
    }

    // ================================================================
    // 24. DISTANT IMPACT RUMBLES — "The Echo" (DAMAGING)
    //     Random explosion clusters at arena walls. Now ringed by 6 floating
    //     IRON_BLOCK "impact slabs" that jolt (scale-punch) on each rumble,
    //     with a heavy explosion + smoke + ember bloom at the burst point.
    // ================================================================
    public static class DistantImpactRumbles extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private static final int SLABS = 6;
        private int nextBurst = 25;
        private int burstUntil = -1;
        private double burstX = 0, burstZ = 0;
        private final List<ItemDisplayHandle> slabs = new ArrayList<>();
        private final double[] sx = new double[SLABS];
        private final double[] sz = new double[SLABS];
        private final int[] jolt = new int[SLABS]; // tick the slab was last jolted

        public DistantImpactRumbles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("distant_impact_rumbles", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyDamagingDefaults(config, 80.0, 4.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(150);
            config.setDesignType("Aggressive atmosphere — distant wall rumbles (timing dodge)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.4f);
            for (int i = 0; i < SLABS; i++) {
                double ang = Math.PI * 2 * i / SLABS;
                double r = 7.5;
                sx[i] = Math.cos(ang) * r;
                sz[i] = Math.sin(ang) * r;
                jolt[i] = -100;
                Location lp = c.clone().add(sx[i], 1.5, sz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                slabs.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Slabs hum + jolt-punch on recent rumbles
            for (int i = 0; i < SLABS; i++) {
                double bob = Math.sin(tick / 50.0 * Math.PI * 2 + i) * 0.1;
                float punch = 0f;
                int since = tick - jolt[i];
                if (since >= 0 && since < 6) punch = 0.4f * (1f - since / 6f);
                float ss = (0.8f + punch) * fade;
                slabs.get(i).animateTo(
                        new Vector3f((float)sx[i] - ss / 2, (float)(1.5 + bob) - ss / 2, (float)sz[i] - ss / 2),
                        new AxisAngle4f((float)(tick * 0.01 + i), 0, 1, 0),
                        new Vector3f(ss), 3);
                if (i % 2 == tick % 2) {
                    Location p = c.clone().add(sx[i], 1.5 + bob, sz[i]);
                    DisplayBuilder.dustParticles(p, 3, 0.3, 110, 110, 115, 1.4f);
                    w.spawnParticle(Particle.SMOKE, p, 2, 0.2, 0.2, 0.2, 0.0);
                }
            }

            // Ambient idle: low rumble dust
            if (tick % 12 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 14.0, 0.1, (Math.random() - 0.5) * 14.0);
                DisplayBuilder.dustParticles(p, 4, 0.5, 100, 100, 100, 1.4f);
            }

            if (tick >= nextBurst && tick < dissipateStart) {
                double ang = Math.random() * Math.PI * 2;
                double r = 7.0 + Math.random() * 1.5;
                burstX = Math.cos(ang) * r;
                burstZ = Math.sin(ang) * r;
                Location p = c.clone().add(burstX, 1.0, burstZ);
                // Heavy explosion bloom
                w.spawnParticle(Particle.EXPLOSION, p, 8, 0.3, 0.3, 0.3, 0.0);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 16, 0.6, 0.4, 0.6, 0.03);
                w.spawnParticle(Particle.FLAME, p, 10, 0.4, 0.3, 0.4, 0.05);
                DisplayBuilder.dustParticles(p, 12, 0.7, 90, 90, 90, 1.8f);
                DisplayBuilder.dustParticles(p, 8, 0.6, 255, 150, 60, 1.4f);
                DisplayBuilder.playSound(p, Sound.ENTITY_IRON_GOLEM_STEP, 0.7f, 0.4f);
                DisplayBuilder.playSound(p, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.4f);
                burstUntil = tick + 4;
                nextBurst = tick + 30 + (int) (Math.random() * 30); // 30-60t

                // Jolt the two nearest slabs
                int best = 0; double bestD = Double.MAX_VALUE;
                for (int i = 0; i < SLABS; i++) {
                    double d = (sx[i] - burstX) * (sx[i] - burstX) + (sz[i] - burstZ) * (sz[i] - burstZ);
                    if (d < bestD) { bestD = d; best = i; }
                }
                jolt[best] = tick;
                jolt[(best + 1) % SLABS] = tick;
            }

            if (tick <= burstUntil) {
                Location p = c.clone().add(burstX, 0.8, burstZ);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 4, 0.5, 0.3, 0.5, 0.02);
                w.spawnParticle(Particle.SMOKE, p, 4, 0.4, 0.3, 0.4, 0.01);
                DisplayBuilder.dustParticles(p, 4, 0.4, 100, 100, 100, 1.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DistantImpactRumbles(plugin); }
    }

    // ================================================================
    // 25. METALLIC RAIN — "The Downpour" (DAMAGING)
    //     Dense FALLING_DUST(IRON_BLOCK) rain from ceiling. Now fed by 6
    //     spinning IRON_BARS grate panels overhead that the rain pours
    //     through, with heavy shaving sparks + ground splash.
    // ================================================================
    public static class MetallicRain extends EnvironmentalAttack {
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 40;
        private static final int GRATES = 6;
        private static BlockData ironData;
        private final List<ItemDisplayHandle> grates = new ArrayList<>();
        private final double[] gx = new double[GRATES];
        private final double[] gz = new double[GRATES];

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
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 0.7f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.7f, 0.8f);
            for (int i = 0; i < GRATES; i++) {
                double ang = Math.PI * 2 * i / GRATES + Math.random() * 0.3;
                double r = 3.0 + Math.random() * 5.0;
                gx[i] = Math.cos(ang) * r;
                gz[i] = Math.sin(ang) * r;
                Location lp = c.clone().add(gx[i], 5.5, gz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.IRON_BARS));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                grates.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Spinning overhead grates the rain pours through
            float gs = 1.4f * fade;
            for (int i = 0; i < GRATES; i++) {
                grates.get(i).animateTo(
                        new Vector3f((float)gx[i] - gs / 2, 5.5f - 0.05f, (float)gz[i] - gs / 2),
                        new AxisAngle4f((float)(tick * 0.06 + i), 0, 1, 0),
                        new Vector3f(gs, 0.1f, gs), 3);
                Location p = c.clone().add(gx[i], 5.3, gz[i]);
                w.spawnParticle(Particle.CRIT, p, 4, 0.4, 0.1, 0.4, 0.05);
                if (tick % 3 == 0) DisplayBuilder.dustParticles(p, 3, 0.4, 180, 180, 200, 1.3f);
            }

            int count = Math.max(0, (int) (28 * fade));
            // Layer 1: FALLING_DUST(IRON_BLOCK) heavy rain from ceiling
            for (int i = 0; i < count; i++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                w.spawnParticle(Particle.FALLING_DUST, c.clone().add(rx, 5.5, rz), 1, 0.05, 0.0, 0.05, 0.4, ironData());
            }
            // Layer 2: shaving spark streaks mid-fall
            int sparks = Math.max(0, (int) (10 * fade));
            for (int i = 0; i < sparks; i++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                double ry = 1.0 + Math.random() * 3.0;
                w.spawnParticle(Particle.CRIT, c.clone().add(rx, ry, rz), 2, 0.1, 0.3, 0.1, 0.1);
            }
            // Layer 3: ground splash dust
            for (int i = 0; i < 8; i++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                DisplayBuilder.dustParticles(c.clone().add(rx, 0.1, rz), 1, 0.3, 180, 180, 200, 1.2f);
            }
            if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.0f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MetallicRain(plugin); }
    }

    // ================================================================
    // 26. SHADOW TENDRIL SNAKES — "The Crawl" (ambient)
    //     2 snaking shadow tendrils that random-walk along the ground. Each
    //     tendril now leads with a CHAIN ItemDisplay "head" (dark glow) and
    //     trails a thick LARGE_SMOKE + soul-fire body with dense dark dust.
    // ================================================================
    public static class ShadowTendrilSnakes extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private static final int TENDRIL_COUNT = 2;
        private static final int TRAIL_LEN = 14;
        private final double[] headX = new double[TENDRIL_COUNT];
        private final double[] headZ = new double[TENDRIL_COUNT];
        private final double[][] trailX = new double[TENDRIL_COUNT][TRAIL_LEN];
        private final double[][] trailZ = new double[TENDRIL_COUNT][TRAIL_LEN];
        private final int[] trailHead = new int[TENDRIL_COUNT];
        private final List<ItemDisplayHandle> heads = new ArrayList<>();

        public ShadowTendrilSnakes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_tendril_snakes", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(700);
            config.setCooldownTicks(140);
            config.setDesignType("Ambient atmosphere — shadow tendril snakes (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.6f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.7f);
            for (int t = 0; t < TENDRIL_COUNT; t++) {
                double ang = Math.random() * Math.PI * 2;
                headX[t] = Math.cos(ang) * 7.0;
                headZ[t] = Math.sin(ang) * 7.0;
                for (int i = 0; i < TRAIL_LEN; i++) {
                    trailX[t][i] = headX[t];
                    trailZ[t][i] = headZ[t];
                }
                trailHead[t] = 0;
                Location lp = c.clone().add(headX[t], 0.4, headZ[t]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(60, 30, 90).interpolation(2, 0);
                heads.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            for (int t = 0; t < TENDRIL_COUNT; t++) {
                if (tick % 3 == 0) {
                    headX[t] += (Math.random() - 0.5) * 0.5;
                    headZ[t] += (Math.random() - 0.5) * 0.5;
                    double d = Math.sqrt(headX[t] * headX[t] + headZ[t] * headZ[t]);
                    if (d > 8.0) {
                        double ang = Math.random() * Math.PI * 2;
                        headX[t] = Math.cos(ang) * 7.5;
                        headZ[t] = Math.sin(ang) * 7.5;
                    }
                    trailHead[t] = (trailHead[t] + 1) % TRAIL_LEN;
                    trailX[t][trailHead[t]] = headX[t];
                    trailZ[t][trailHead[t]] = headZ[t];
                }

                // CHAIN head bobbing + writhing
                float hs = 0.5f * fade, hsy = 1.1f * fade;
                double bob = Math.sin(tick / 12.0 * Math.PI * 2 + t) * 0.2;
                heads.get(t).animateTo(
                        new Vector3f((float)headX[t] - hs / 2, (float)(0.5 + bob) - hsy / 2, (float)headZ[t] - hs / 2),
                        new AxisAngle4f((float)(tick * 0.12 + t), 0.5f, 1, 0),
                        new Vector3f(hs, hsy, hs), 3);
                Location headLoc = c.clone().add(headX[t], 0.5 + bob, headZ[t]);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, headLoc, 5, 0.2, 0.2, 0.2, 0.01);
                DisplayBuilder.dustParticles(headLoc, 5, 0.2, 40, 20, 60, 1.6f);

                // Thick smoke body trailing the head
                for (int i = 0; i < TRAIL_LEN; i++) {
                    int idx = (trailHead[t] - i + TRAIL_LEN) % TRAIL_LEN;
                    float tf = 1.0f - (i / (float) TRAIL_LEN);
                    if (tf <= 0.05f) continue;
                    Location p = c.clone().add(trailX[t][idx], 0.3, trailZ[t][idx]);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 2, 0.18 * tf, 0.1, 0.18 * tf, 0.0);
                    if (i % 2 == 0) w.spawnParticle(Particle.SMOKE, p.clone().add(0, 0.25, 0), 2, 0.12, 0.12, 0.12, 0.0);
                    if (i % 3 == 0) DisplayBuilder.dustParticles(p, 2, 0.15, 30, 30, 35, 1.5f);
                }
            }
            if (tick % 70 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.3f, 0.6f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ShadowTendrilSnakes(plugin); }
    }

    // ================================================================
    // 27. IRON AURORA — "The Sky" (ambient)
    //     Horizontal iron-grey aurora streams high in the arena (Y=4-6). Now
    //     anchored by 7 slowly drifting NETHERITE_BLOCK sky-shards (iron glow)
    //     riding the streams, with dense dual-layer dust ribbons + shimmer.
    // ================================================================
    public static class IronAurora extends EnvironmentalAttack {
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 40;
        private static final int SHARDS = 7;
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] baseX = new double[SHARDS];
        private final double[] baseY = new double[SHARDS];
        private final double[] baseZ = new double[SHARDS];
        private final double[] speed = new double[SHARDS];
        private final double[] phase = new double[SHARDS];

        public IronAurora(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_aurora", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(800);
            config.setCooldownTicks(150);
            config.setDesignType("Ambient atmosphere — iron-grey sky aurora (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.5f, 0.6f);
            for (int i = 0; i < SHARDS; i++) {
                baseX[i] = (Math.random() - 0.5) * 12.0;
                baseY[i] = 4.4 + Math.random() * 1.4;
                baseZ[i] = (Math.random() - 0.5) * 12.0;
                speed[i] = (0.5 + Math.random() * 0.6) * (i % 2 == 0 ? 1 : -1);
                phase[i] = Math.random() * Math.PI * 2;
                Location lp = c.clone().add(baseX[i], baseY[i], baseZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.NETHERITE_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Drifting sky-shards riding the aurora
            float ss = 0.6f * fade;
            for (int i = 0; i < SHARDS; i++) {
                double driftX = Math.sin(tick * 0.01 * speed[i] + phase[i]) * 6.0;
                double driftZ = Math.cos(tick * 0.008 * speed[i] + phase[i]) * 4.0;
                double bob = Math.sin(tick / 50.0 * Math.PI * 2 + i) * 0.3;
                double cx = baseX[i] + driftX, cy = baseY[i] + bob, cz = baseZ[i] + driftZ;
                shards.get(i).animateTo(
                        new Vector3f((float)cx - ss / 2, (float)cy - ss / 2, (float)cz - ss / 2),
                        new AxisAngle4f((float)(tick * 0.03 + i), 0.3f, 1, 0.3f),
                        new Vector3f(ss), 5);
                Location p = c.clone().add(cx, cy, cz);
                DisplayBuilder.dustParticles(p, 5, 0.4, 170, 180, 200, 1.5f);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.3, 0.2, 0.3, 0.01);
                if (tick % 5 == 0) w.spawnParticle(Particle.END_ROD, p, 1, 0.2, 0.1, 0.2, 0.005);
            }

            int count = Math.max(0, (int) (10 * fade));
            // Layer 1: top ribbon Y=5 (lighter)
            for (int i = 0; i < count; i++) {
                double driftPhase = tick * 0.04 + i * 1.7;
                double rx = (Math.random() - 0.5) * 14.0 + Math.sin(driftPhase) * 0.8;
                double rz = (Math.random() - 0.5) * 14.0;
                DisplayBuilder.dustParticles(c.clone().add(rx, 5.0, rz), 1, 0.05, 160, 170, 190, 1.4f);
            }
            // Layer 2: bottom ribbon Y=4.2 (darker)
            for (int i = 0; i < count; i++) {
                double driftPhase = tick * 0.05 - i * 1.3;
                double rx = (Math.random() - 0.5) * 14.0 + Math.cos(driftPhase) * 0.8;
                double rz = (Math.random() - 0.5) * 14.0;
                DisplayBuilder.dustParticles(c.clone().add(rx, 4.2, rz), 1, 0.05, 100, 110, 130, 1.2f);
            }
            // Layer 3: shimmer accents
            for (int i = 0; i < 5; i++) {
                double rx = (Math.random() - 0.5) * 12.0;
                double rz = (Math.random() - 0.5) * 12.0;
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(rx, 4.6, rz), 1, 0.2, 0.1, 0.2, 0.01);
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronAurora(plugin); }
    }

    // ================================================================
    // 28. SHOCKWAVE GROUND RINGS — "The Tremor" (DAMAGING)
    //     Expanding shockwave rings from center. Now anchored by a central
    //     ANVIL impact core (rust glow) that scale-punches on each ring, and
    //     the ring carries BLOCK(IRON) + dust + CRIT + soul-fire chevrons.
    // ================================================================
    public static class ShockwaveGroundRings extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private static BlockData ironData;
        private int nextRing = 30;
        private int ringStart = -1;
        private ItemDisplayHandle core;

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
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.7f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.7f, 0.6f);
            Location lp = c.clone().add(0, 0.4, 0);
            core = displayBuilder.spawnItem(lp, new ItemStack(Material.ANVIL));
            core.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
            spawnedEntities.add(core.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Central core — punch on ring spawn, otherwise idle hum
            float punch = 0f;
            if (ringStart >= 0) {
                int since = tick - ringStart;
                if (since >= 0 && since < 6) punch = 0.5f * (1f - since / 6f);
            }
            float cs = (0.9f + punch) * fade;
            if (core != null) {
                core.animateTo(
                        new Vector3f(-cs / 2, 0.4f - cs / 2, -cs / 2),
                        new AxisAngle4f((float)(tick * 0.05), 0, 1, 0),
                        new Vector3f(cs), 3);
            }
            Location coreLoc = c.clone().add(0, 0.5, 0);
            w.spawnParticle(Particle.SMOKE, coreLoc, 3, 0.3, 0.2, 0.3, 0.0);
            DisplayBuilder.dustParticles(coreLoc, 3, 0.3, 200, 110, 40, 1.3f);

            // Trigger a new ring
            if (tick >= nextRing && tick < dissipateStart) {
                ringStart = tick;
                nextRing = tick + 40;
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.6f);
                w.spawnParticle(Particle.EXPLOSION, coreLoc, 2, 0.2, 0.1, 0.2, 0.0);
            }

            // Animate active ring (0..10t lifespan)
            if (ringStart >= 0 && tick - ringStart <= 10) {
                int age = tick - ringStart;
                double r = 0.5 + (4.0 - 0.5) * (age / 10.0);
                Location ringCenter = c.clone().add(0, 0.12, 0);
                // Layer 1: BLOCK(IRON_BLOCK) ring outline (dense)
                DisplayBuilder.particleRing(ringCenter, r, Particle.BLOCK, 28, ironData());
                // Layer 2: gray dust + rust accent ring
                for (int i = 0; i < 28; i++) {
                    double a = Math.PI * 2 * i / 28;
                    Location p = ringCenter.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 120, 120, 130, 1.5f);
                    if (i % 4 == 0) DisplayBuilder.dustParticles(p.clone().add(0, 0.1, 0), 1, 0.05, 200, 110, 40, 1.3f);
                }
                // Layer 3: CRIT + soul-fire chevrons sweeping the ring
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12 + age * 0.12;
                    Location p = ringCenter.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                    w.spawnParticle(Particle.CRIT, p, 2, 0.05, 0.1, 0.05, 0.05);
                    if (i % 3 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ShockwaveGroundRings(plugin); }
    }

    // ================================================================
    // 29. FORGE EMBER GLOW — "The Heat" (DAMAGING)
    //     4 floor-corner forge vents (IRON_TRAPDOOR caps) belching rising
    //     FLAME / LAVA hot embers + soul-fire columns + orange dust. Light
    //     contact damage if you stand at a vent.
    // ================================================================
    public static class ForgeEmberGlow extends EnvironmentalAttack {
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 40;
        private static final double[][] CORNERS = {
                {  6.0,  6.0 }, {  6.0, -6.0 }, { -6.0,  6.0 }, { -6.0, -6.0 }
        };
        private final List<ItemDisplayHandle> vents = new ArrayList<>();
        private final List<ItemDisplayHandle> cores = new ArrayList<>(); // NETHERITE_BLOCK ember cores

        public ForgeEmberGlow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("forge_ember_glow", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyDamagingDefaults(config, 85.0, 4.5);
            config.setDurationTicks(800);
            config.setCooldownTicks(150);
            config.setDesignType("Aggressive atmosphere — corner forge ember vents (timing dodge)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.7f, 0.5f);
            for (double[] corner : CORNERS) {
                Location vp = c.clone().add(corner[0], 0.05, corner[1]);
                ItemDisplayHandle h = displayBuilder.spawnItem(vp, new ItemStack(Material.IRON_TRAPDOOR));
                h.scale(0.001f, 0.001f, 0.001f).glow(255, 140, 40).interpolation(2, 0);
                vents.add(h);
                spawnedEntities.add(h.entity());
                Location cp = c.clone().add(corner[0], 0.6, corner[1]);
                ItemDisplayHandle core = displayBuilder.spawnItem(cp, new ItemStack(Material.NETHERITE_BLOCK));
                core.scale(0.001f, 0.001f, 0.001f).glow(255, 120, 30).interpolation(2, 0);
                cores.add(core);
                spawnedEntities.add(core.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float ventScale = 0.9f * fade;
            float coreScale = 0.45f * fade;
            for (int i = 0; i < vents.size(); i++) {
                double[] corner = CORNERS[i];
                vents.get(i).animateTo(
                        new Vector3f((float) corner[0] - ventScale / 2, 0.05f, (float) corner[1] - ventScale / 2),
                        new AxisAngle4f((float) (tick * 0.02), 0, 1, 0),
                        new Vector3f(ventScale, 0.1f, ventScale), 4);
                double coreBob = Math.sin(tick / 18.0 * Math.PI * 2 + i) * 0.15;
                cores.get(i).animateTo(
                        new Vector3f((float) corner[0] - coreScale / 2, (float)(0.6 + coreBob) - coreScale / 2, (float) corner[1] - coreScale / 2),
                        new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                        new Vector3f(coreScale), 4);

                Location p = c.clone().add(corner[0], 0.3, corner[1]);
                // Layer 1: FLAME rising column
                w.spawnParticle(Particle.FLAME, p, Math.max(0, (int)(10 * fade)), 0.25, 0.15, 0.25, 0.05);
                // Layer 2: LAVA droplets + soul-fire column
                if (tick % 3 == 0) w.spawnParticle(Particle.LAVA, p, 2, 0.15, 0.1, 0.15, 0.0);
                for (int y = 0; y < 4; y++) {
                    Location col = p.clone().add(0, 0.3 + y * 0.4, 0);
                    if ((tick + y) % 2 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, col, 2, 0.12, 0.1, 0.12, 0.01);
                }
                // Layer 3: orange ember dust + hot smoke cap
                DisplayBuilder.dustParticles(p.clone().add(0, 0.5, 0), 5, 0.25, 255, 150, 50, 1.5f);
                if (tick % 4 == 0) w.spawnParticle(Particle.LARGE_SMOKE, p.clone().add(0, 1.2, 0), 2, 0.25, 0.2, 0.25, 0.02);
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.8f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ForgeEmberGlow(plugin); }
    }

    // ================================================================
    // 30. CHAIN FRAGMENT SHOWER — "The Light" (ambient)
    //     Periodic chain-fragment bursts from mid-air. Now framed by 6
    //     slowly-orbiting CHAIN ItemDisplays (rust glow) that the fragments
    //     scatter from, with dense ITEM(CHAIN) + spark + dust bursts.
    // ================================================================
    public static class ChainFragmentShower extends EnvironmentalAttack {
        private static final int SPAWN_END = 15;
        private static final int DISSIPATE_LEN = 30;
        private static final int ORBITERS = 6;
        private int nextBurst = 25;
        private static ItemStack chainStack;
        private final List<ItemDisplayHandle> orbiters = new ArrayList<>();
        private final double[] orbitR = new double[ORBITERS];
        private final double[] orbitY = new double[ORBITERS];
        private final double[] orbitSpeed = new double[ORBITERS];
        private final double[] phase = new double[ORBITERS];

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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.5f, 1.4f);
            for (int i = 0; i < ORBITERS; i++) {
                double ang = Math.PI * 2 * i / ORBITERS;
                orbitR[i] = 3.5 + Math.random() * 3.0;
                orbitY[i] = 2.0 + Math.random() * 1.5;
                orbitSpeed[i] = (0.015 + Math.random() * 0.015) * (i % 2 == 0 ? 1 : -1);
                phase[i] = ang;
                Location lp = c.clone().add(Math.cos(ang) * orbitR[i], orbitY[i], Math.sin(ang) * orbitR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                orbiters.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Orbiting chain fragments — thick, slowly tumbling
            float sX = 0.42f * fade, sY = 1.2f * fade, sZ = 0.42f * fade;
            for (int i = 0; i < ORBITERS; i++) {
                double a = phase[i] + tick * orbitSpeed[i];
                double cx = Math.cos(a) * orbitR[i];
                double cz = Math.sin(a) * orbitR[i];
                double cy = orbitY[i] + Math.sin(tick / 30.0 * Math.PI * 2 + i) * 0.4;
                orbiters.get(i).animateTo(
                        new Vector3f((float)cx - sX / 2, (float)cy - sY / 2, (float)cz - sZ / 2),
                        new AxisAngle4f((float)(tick * 0.06 + i * 0.5), 0.5f, 1, 0.2f),
                        new Vector3f(sX, sY, sZ), 4);
                Location p = c.clone().add(cx, cy, cz);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.2, 0.2, 0.2, 0.02);
                DisplayBuilder.dustParticles(p, 4, 0.2, 130, 130, 140, 1.4f);
                if (tick % 4 == 0) w.spawnParticle(Particle.CRIT, p, 2, 0.15, 0.15, 0.15, 0.03);
            }

            if (tick >= nextBurst && tick < dissipateStart) {
                double rx = (Math.random() - 0.5) * 12.0;
                double rz = (Math.random() - 0.5) * 12.0;
                Location p = c.clone().add(rx, 2.5, rz);
                // Dense ITEM(CHAIN) burst
                w.spawnParticle(Particle.ITEM, p, 14, 0.5, 0.5, 0.5, 0.25, chainStack());
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 10, 0.4, 0.4, 0.4, 0.06);
                w.spawnParticle(Particle.CRIT, p, 8, 0.4, 0.4, 0.4, 0.08);
                DisplayBuilder.dustParticles(p, 8, 0.5, 130, 130, 140, 1.5f);
                DisplayBuilder.playSound(p, Sound.BLOCK_CHAIN_HIT, 0.7f, 1.1f + (float) Math.random() * 0.3f);
                nextBurst = tick + 22;
            }
            // Ambient idle: faint chain-link wisps scattered around
            for (int s = 0; s < 4; s++) {
                double rx = (Math.random() - 0.5) * 14.0;
                double rz = (Math.random() - 0.5) * 14.0;
                Location p = c.clone().add(rx, 2.0 + Math.random() * 1.5, rz);
                DisplayBuilder.dustParticles(p, 1, 0.0, 130, 130, 140, 1.1f);
                if (s % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainFragmentShower(plugin); }
    }
}
