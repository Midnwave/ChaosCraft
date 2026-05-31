package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — ENVIRONMENTAL ATTACKS 61-75 (Category 5: Combat Feedback & Event Effects)
 *
 * === VISIBILITY OVERHAUL ===
 * Player feedback: "too weak — sparse particles, tiny / missing ItemDisplays."
 * This batch is rebuilt to the SAME dense+visible standard as
 * ChainEnvironmental (1-15), ChainEnvironmental2 (16-30), ChainEnvironmental3
 * (31-45) and ChainEnvironmental4 (46-60).
 *
 * These are REACTIVE EVENT EFFECTS — telegraphs, impact flashes, clash sparks,
 * swing trails, air displacement, snap bursts, ground cracks, warning pulses,
 * near-miss sparks, tangle remnants, reversal shockwaves, metal echoes,
 * explosion debris, sequence finales, ambient heartbeat. Each one is now a
 * PUNCHY, VISIBLE burst:
 *
 *   - Real ItemDisplay debris flies on every burst — thick CHAIN shards,
 *     IRON_BLOCK chunks and ANVIL fragments scaled 0.4 x 1.2 x 0.4 MINIMUM for
 *     chains, chunkier for blocks/anvils. No more 0.06-0.1 specks, no more
 *     pure-particle events. ItemDisplays ONLY — never BlockDisplays.
 *   - Heavy multi-layer particles: 3+ layers, 8-20 particles per tick DURING the
 *     burst (denser than ambient — these are punchy events). CRIT /
 *     ELECTRIC_SPARK / EXPLOSION / SMOKE / LARGE_SMOKE / SOUL_FIRE_FLAME / DUST /
 *     END_ROD / FALLING_DUST.
 *   - Every display glows from the SHARED palette (RUST / AMETHYST / IRON /
 *     LANTERN_GLOW / DARK_IRON) — same constants as the sibling files.
 *   - Spawn pattern matches the siblings exactly:
 *         spawnItem -> scale(0.001) -> glow -> interpolation(2,0)
 *     then a per-tick animateTo. Debris flies outward with gravity, shockwave
 *     rings expand, swing trails follow — real motion, never static.
 *   - spawnedEntities.add(h.entity()) tracking + onCleanup() override.
 *
 * Critical contract preserved: IDs, durations, signatures, newInstance(),
 * spawn-straight (yaw=0, pitch=0), damage values. Ambient/feedback effects keep
 * damage=0; the five reactive shockwaves keep their damage values.
 */
public final class ChainEnvironmental5 {
    private ChainEnvironmental5() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    // Glow palettes (visibility) — shared with ChainEnvironmental (1-15)..(46-60)
    private static final int[] RUST = {200, 110, 40};       // industrial rust-orange
    private static final int[] IRON = {210, 210, 230};      // bright iron
    private static final int[] AMETHYST = {170, 80, 230};   // energy purple
    private static final int[] LANTERN_GLOW = {255, 180, 80};
    private static final int[] DARK_IRON = {120, 120, 150};

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new AttackTelegraphFloorIndicator(plugin));
        registry.register(new ImpactCraterRing(plugin));
        registry.register(new ChainClashSparks(plugin));
        registry.register(new WreckingBallSwingTrail(plugin));
        registry.register(new PendulumAirDisplacement(plugin));
        registry.register(new ChainSnapBurst(plugin));
        registry.register(new IronSmashGroundCrack(plugin));
        registry.register(new FallingWeightWarningPulse(plugin));
        registry.register(new NearMissSparks(plugin));
        registry.register(new ChainTangleVisual(plugin));
        registry.register(new SwingReversalShockwave(plugin));
        registry.register(new MetalImpactEcho(plugin));
        registry.register(new IronExplosionDebris(plugin));
        registry.register(new AttackSequenceFinale(plugin));
        registry.register(new ChainModeAmbientHeartbeat(plugin));
    }

    // ================================================================
    // Helpers — punchy EVENT-BURST particle layers (denser than ambient)
    // ================================================================

    private static void redDust(Location l, int count, double spread) {
        DisplayBuilder.dustParticles(l, count, spread, 220, 40, 40, 1.6f);
    }

    private static void orangeDust(Location l, int count, double spread) {
        DisplayBuilder.dustParticles(l, count, spread, 240, 140, 40, 1.4f);
    }

    private static void grayDust(Location l, int count, double spread) {
        DisplayBuilder.dustParticles(l, count, spread, 90, 90, 100, 1.4f);
    }

    private static void ironDust(Location l, int count, double spread) {
        DisplayBuilder.dustParticles(l, count, spread, IRON[0], IRON[1], IRON[2], 1.4f);
    }

    /**
     * HEAVY multi-layer IMPACT burst at a point — bright sparks + crit shimmer +
     * thick smoke + colored dust + iron debris dust. 8-20 particles across 5
     * layers. {@code col} sets the dust hue. Used at the heart of every event.
     */
    private static void burstHeavy(World w, Location p, int[] col, double spread) {
        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 14, spread, spread, spread, 0.35);
        w.spawnParticle(Particle.CRIT, p, 10, spread * 0.8, spread * 0.8, spread * 0.8, 0.25);
        w.spawnParticle(Particle.LARGE_SMOKE, p, 6, spread * 0.7, spread * 0.5, spread * 0.7, 0.05);
        w.spawnParticle(Particle.SMOKE, p, 8, spread * 0.6, spread * 0.6, spread * 0.6, 0.08);
        DisplayBuilder.dustParticles(p, 10, spread, col[0], col[1], col[2], 1.6f);
        w.spawnParticle(Particle.FALLING_DUST, p, 6, spread * 0.5, spread * 0.5, spread * 0.5, 0.0,
                Material.IRON_BLOCK.createBlockData());
    }

    /** Heavy debris-trail pass following a flying ItemDisplay chunk. */
    private static void debrisTrail(World w, Location pos, int tick, Material mat) {
        w.spawnParticle(Particle.FALLING_DUST, pos, 2, 0.1, 0.1, 0.1, 0.0, mat.createBlockData());
        if (tick % 2 == 0) w.spawnParticle(Particle.SMOKE, pos, 2, 0.08, 0.08, 0.08, 0.01);
        if (tick % 2 == 0) w.spawnParticle(Particle.CRIT, pos, 2, 0.08, 0.08, 0.08, 0.04);
        if (tick % 3 == 0) ironDust(pos, 2, 0.1);
    }

    // ================================================================
    // 61. ATTACK TELEGRAPH FLOOR INDICATOR — "The Warning"
    //     Combat feedback — incoming-impact telegraph (no damage, atmosphere)
    //     SPAWN  : big RED_CONCRETE flat disc + 4 CHAIN marker shards at the rim
    //     ACTIVE : pulsing disc, heavy ELECTRIC_SPARK rim ring, rising warning fog
    //     FADE   : disc + shards shrink, pre-impact spark crack
    // ================================================================
    public static class AttackTelegraphFloorIndicator extends EnvironmentalAttack {
        private ItemDisplayHandle indicator;
        private final List<ItemDisplayHandle> rimShards = new ArrayList<>();
        private static final int SHARDS = 4;
        private static final double R = 4.0; // matches an "impact_radius" telegraph

        public AttackTelegraphFloorIndicator(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("attack_telegraph_floor_indicator", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(60);
            config.setCooldownTicks(80);
            config.setChance(10);
            config.setDesignType("Combat feedback — incoming-impact telegraph (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.9f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.8f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.5f, 1.5f);

            Location ground = c.clone();
            ground.setY(c.getY() + 0.05);
            indicator = displayBuilder.spawnItem(ground, new ItemStack(Material.RED_CONCRETE));
            indicator.scale(0.001f, 0.001f, 0.001f).glow(220, 30, 30).interpolation(2, 0);
            spawnedEntities.add(indicator.entity());
            float wide = (float)(R * 2.0);
            indicator.animateTo(
                    new Vector3f(-wide / 2f, -0.45f, -wide / 2f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(wide, 0.04f, wide), 6);

            // 4 thick CHAIN marker shards standing at the rim
            for (int k = 0; k < SHARDS; k++) {
                double a = Math.PI * 2 * k / SHARDS;
                Location p = c.clone().add(Math.cos(a) * R, 0.6, Math.sin(a) * R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                rimShards.add(h); spawnedEntities.add(h.entity());
                h.animateTo(
                        new Vector3f((float)(Math.cos(a) * R) - 0.2f, 0.6f, (float)(Math.sin(a) * R) - 0.2f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.4f, 1.2f, 0.4f), 6);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float wide = (float)(R * 2.0);
            float pulse = 1.0f + (float)Math.sin(tick * Math.PI / 5.0) * 0.04f;
            if (tick % 5 == 0 && indicator != null) {
                indicator.animateTo(
                        new Vector3f(-wide * pulse / 2f, -0.45f, -wide * pulse / 2f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(wide * pulse, 0.04f, wide * pulse), 5);
            }
            // Bob the rim shards with the pulse
            for (int k = 0; k < rimShards.size(); k++) {
                double a = Math.PI * 2 * k / SHARDS;
                double by = 0.6 + Math.sin(tick * 0.2 + k) * 0.12;
                rimShards.get(k).animateTo(
                        new Vector3f((float)(Math.cos(a) * R) - 0.2f, (float) by, (float)(Math.sin(a) * R) - 0.2f),
                        new AxisAngle4f((float)(a + tick * 0.04), 0, 1, 0),
                        new Vector3f(0.4f, 1.2f, 0.4f), 5);
            }
            // HEAVY spark ring along the edge every 3 ticks (16 points)
            if (tick % 3 == 0) {
                for (int k = 0; k < 16; k++) {
                    double a = Math.PI * 2 * k / 16 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * R, 0.15, Math.sin(a) * R);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.1, 0.1, 0.1, 0.04);
                    redDust(p, 2, 0.15);
                    if (k % 2 == 0) w.spawnParticle(Particle.SMOKE, p, 1, 0.08, 0.1, 0.08, 0.02);
                }
            }
            // Rising warning fog at center
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.4, 0), 4, R * 0.4, 0.2, R * 0.4, 0.02);
                redDust(c.clone().add(0, 0.6, 0), 5, R * 0.4);
            }
            // Pre-impact crack in last 6 ticks
            if (tick == config.getDurationTicks() - 6) {
                if (indicator != null) indicator.animateTo(
                        new Vector3f(-wide / 2f, -0.45f, -wide / 2f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.001f, 0.001f, 0.001f), 6);
                for (ItemDisplayHandle h : rimShards) {
                    org.bukkit.util.Transformation t = h.entity().getTransformation();
                    h.entity().setInterpolationDuration(6); h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new org.bukkit.util.Transformation(
                            t.getTranslation(), t.getLeftRotation(),
                            new Vector3f(0.001f, 0.001f, 0.001f), t.getRightRotation()));
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.2f, 1.0f);
                burstHeavy(w, c.clone().add(0, 0.5, 0), RUST, 0.8);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AttackTelegraphFloorIndicator(plugin); }
    }

    // ================================================================
    // 62. IMPACT CRATER RING — "The Aftermath"
    //     Reactive shockwave (sidestep wide AoE) — IMPACT DAMAGE
    //     SPAWN  : 8 big IRON_BLOCK debris chunks blasted out + heavy slam burst
    //     ACTIVE : expanding BLOCK(IRON_BLOCK) ring, debris arcs out + falls w/ gravity
    //     FADE   : debris shrink, lingering crater dust
    // ================================================================
    public static class ImpactCraterRing extends EnvironmentalAttack {
        private static final int PIECES = 8;
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private final double[] dAx = new double[PIECES];
        private final double[] dAz = new double[PIECES];
        private final double[] dY = new double[PIECES];
        private final double[] dVy = new double[PIECES];
        private static final double IMPACT_R = 4.0;

        public ImpactCraterRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("impact_crater_ring", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(60);
            config.setCooldownTicks(90);
            config.setChance(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(200.0);
            config.setImpactRadius(4.0);
            config.setDesignType("Reactive shockwave (sidestep wide AoE)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;

            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.8f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.6f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);

            triggerImpactDamage(c.clone());

            // Heavy central impact burst
            w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 2, 0.4, 0.2, 0.4, 0);
            burstHeavy(w, c.clone().add(0, 0.6, 0), IRON, 1.2);

            // 8 thick IRON_BLOCK debris chunks blasted out at compass points
            for (int i = 0; i < PIECES; i++) {
                double a = Math.PI * 2 * i / PIECES + Math.random() * 0.25;
                dAx[i] = Math.cos(a);
                dAz[i] = Math.sin(a);
                dY[i] = 0.6;
                dVy[i] = 0.30 + Math.random() * 0.12;
                Location p = c.clone().add(0.0, dY[i], 0.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                debris.add(h);
                spawnedEntities.add(h.entity());
                h.animateTo(
                        new Vector3f(-0.25f, (float)dY[i], -0.25f),
                        new AxisAngle4f((float)(i * 0.7), 0, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Expanding BLOCK(IRON_BLOCK) shockwave ring over 8 ticks (24 points)
            if (tick <= 8) {
                double r = (IMPACT_R + 1.5) * (tick / 8.0);
                for (int k = 0; k < 24; k++) {
                    double a = Math.PI * 2 * k / 24;
                    Location p = c.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                    w.spawnParticle(Particle.BLOCK, p, 3, 0.12, 0.12, 0.12, 0.03, Material.IRON_BLOCK.createBlockData());
                    if (k % 2 == 0) w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.12, 0.12, 0.12, 0.02);
                    if (k % 3 == 0) grayDust(p, 2, 0.12);
                    if (k % 4 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }

            // Debris physics — arc outward then fall under gravity
            float chunk = (tick > config.getDurationTicks() - 12) ? 0.001f : 0.5f;
            for (int i = 0; i < debris.size(); i++) {
                dVy[i] -= 0.04;
                dY[i] = Math.max(0.25, dY[i] + dVy[i]);
                double radial = Math.min(IMPACT_R + 1.5, tick * 0.5 + 0.5);
                float px = (float)(dAx[i] * radial);
                float pz = (float)(dAz[i] * radial);
                debris.get(i).animateTo(
                        new Vector3f(px - chunk / 2f, (float)dY[i] - chunk / 2f, pz - chunk / 2f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 0.4f, 1, 0.2f),
                        new Vector3f(chunk, chunk, chunk), 2);
                Location pos = c.clone().add(px, dY[i], pz);
                debrisTrail(w, pos, tick, Material.IRON_BLOCK);
            }

            // FADE — lingering crater dust
            if (tick > 40 && tick % 3 == 0) {
                for (int k = 0; k < 6; k++) {
                    double a = Math.random() * Math.PI * 2;
                    double rr = Math.random() * (IMPACT_R + 0.5);
                    Location p = c.clone().add(Math.cos(a) * rr, 0.15, Math.sin(a) * rr);
                    grayDust(p, 2, 0.12);
                    if (k % 2 == 0) w.spawnParticle(Particle.SMOKE, p, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            if (tick == config.getDurationTicks() - 12) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 0.6f, 1.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ImpactCraterRing(plugin); }
    }

    // ================================================================
    // 63. CHAIN CLASH SPARKS — "The Contact"
    //     Combat feedback — pendulum reversal-point spark burst (no damage)
    //     SPAWN  : big clash burst + 3 thick CHAIN shards thrown out from contact
    //     ACTIVE : shards tumble + fall, 3 secondary clash bursts at offsets
    //     FADE   : shards shrink, dwindling embers
    // ================================================================
    public static class ChainClashSparks extends EnvironmentalAttack {
        private static final int SHARDS = 3;
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] sAx = new double[SHARDS];
        private final double[] sAz = new double[SHARDS];
        private final double[] sY = new double[SHARDS];
        private final double[] sVy = new double[SHARDS];

        public ChainClashSparks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_clash_sparks", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(40);
            config.setCooldownTicks(60);
            config.setChance(10);
            config.setDesignType("Combat feedback — pendulum reversal-point spark burst (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.6f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.2f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.8f);

            Location burst = c.clone().add(0, 1.4, 0);
            burstHeavy(w, burst, LANTERN_GLOW, 0.7);
            orangeDust(burst, 12, 0.6);

            // 3 thick CHAIN shards thrown from the contact point
            for (int i = 0; i < SHARDS; i++) {
                double a = Math.PI * 2 * i / SHARDS + Math.random() * 0.6;
                sAx[i] = Math.cos(a);
                sAz[i] = Math.sin(a);
                sY[i] = 1.4;
                sVy[i] = 0.25 + Math.random() * 0.1;
                Location p = c.clone().add(0, sY[i], 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(LANTERN_GLOW[0], LANTERN_GLOW[1], LANTERN_GLOW[2]).interpolation(2, 0);
                shards.add(h); spawnedEntities.add(h.entity());
                h.animateTo(
                        new Vector3f(-0.2f, (float) sY[i], -0.2f),
                        new AxisAngle4f((float)(i * 0.9), 0, 1, 0),
                        new Vector3f(0.4f, 1.2f, 0.4f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Shard physics
            boolean fading = tick > config.getDurationTicks() - 10;
            float sc = fading ? 0.001f : 0.4f;
            for (int i = 0; i < shards.size(); i++) {
                sVy[i] -= 0.035;
                sY[i] = Math.max(0.2, sY[i] + sVy[i]);
                double radial = Math.min(2.6, tick * 0.22);
                float px = (float)(sAx[i] * radial);
                float pz = (float)(sAz[i] * radial);
                shards.get(i).animateTo(
                        new Vector3f(px - sc / 2f, (float) sY[i] - (fading ? 0.0006f : 0.6f), pz - sc / 2f),
                        new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                        new Vector3f(sc, fading ? 0.001f : 1.2f, sc), 2);
                debrisTrail(w, c.clone().add(px, sY[i], pz), tick, Material.CHAIN);
            }

            // 3 secondary clash bursts at random offsets
            if (tick == 8 || tick == 16 || tick == 24) {
                double rx = (Math.random() - 0.5) * 3;
                double rz = (Math.random() - 0.5) * 3;
                Location p = c.clone().add(rx, 1.0 + Math.random() * 0.8, rz);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 12, 0.4, 0.4, 0.4, 0.3);
                w.spawnParticle(Particle.CRIT, p, 8, 0.3, 0.3, 0.3, 0.2);
                orangeDust(p, 8, 0.4);
                w.spawnParticle(Particle.SMOKE, p, 4, 0.3, 0.3, 0.3, 0.05);
                DisplayBuilder.playSound(p, Sound.BLOCK_CHAIN_HIT, 1.0f, 1.6f);
            }
            // FADE: dwindling embers
            if (tick > 28 && tick % 2 == 0) {
                for (int k = 0; k < 4; k++) {
                    double rx = (Math.random() - 0.5) * 2.4;
                    double rz = (Math.random() - 0.5) * 2.4;
                    Location p = c.clone().add(rx, 0.6 + Math.random() * 1.0, rz);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.1, 0.05, 0.02);
                    orangeDust(p, 1, 0.1);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainClashSparks(plugin); }
    }

    // ================================================================
    // 64. WRECKING BALL SWING TRAIL — "The Arc"
    //     Combat feedback — swing-arc trace behind pendulum (no damage)
    //     SPAWN  : big IRON_BLOCK wrecking ball + thick CHAIN tether links
    //     ACTIVE : ball swings the full arc, CHAIN trail follows, heavy trailing
    //              dust + sparks, fading "stale" arc samples
    //     FADE   : ball + chain shrink at the bottom of the arc
    // ================================================================
    public static class WreckingBallSwingTrail extends EnvironmentalAttack {
        private ItemDisplayHandle ball;
        private final List<ItemDisplayHandle> tether = new ArrayList<>();
        private static final int TETHER_LINKS = 5;
        private double trailAngle = 0;

        public WreckingBallSwingTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_swing_trail", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(140);
            config.setCooldownTicks(100);
            config.setChance(10);
            config.setDesignType("Combat feedback — swing-arc trace behind pendulum (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.8f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 0.6f, 0.7f);

            ball = displayBuilder.spawnItem(c.clone().add(0, 4.0, 0), new ItemStack(Material.IRON_BLOCK));
            ball.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
            spawnedEntities.add(ball.entity());
            ball.animateTo(new Vector3f(-0.55f, 4.0f, -0.55f), new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.1f, 1.1f, 1.1f), 4);

            for (int i = 0; i < TETHER_LINKS; i++) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(0, 5.0 + i * 0.6, 0), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                tether.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            boolean fading = tick > config.getDurationTicks() - 30;
            float fscale = fading ? Math.max(0f, 1f - (tick - (config.getDurationTicks() - 30)) / 30f) : 1f;

            // Pendulum arc, 4-block radius, period 60 ticks, pivot at Y=7.5
            trailAngle = Math.sin(tick * Math.PI / 30.0) * (Math.PI / 2.2);
            double pivotY = 7.5;
            double bx = Math.sin(trailAngle) * 4.0;
            double by = pivotY - Math.cos(trailAngle) * 3.5;
            if (ball != null) {
                float s = 1.1f * fscale;
                ball.animateTo(
                        new Vector3f((float) bx - s / 2, (float) by - s / 2, -s / 2),
                        new AxisAngle4f((float)(tick * 0.3), 0, 1, 0),
                        new Vector3f(s, s, s), 2);
            }
            // Tether links interpolate from pivot down to the ball
            for (int i = 0; i < tether.size(); i++) {
                double t = (i + 1) / (double)(TETHER_LINKS + 1);
                double lx = bx * t;
                double ly = pivotY - (pivotY - by) * t;
                float sxz = 0.4f * fscale;
                float sy = 1.2f * fscale;
                tether.get(i).animateTo(
                        new Vector3f((float) lx - sxz / 2, (float) ly - sy / 2, -sxz / 2),
                        new AxisAngle4f((float) trailAngle, 0, 0, 1),
                        new Vector3f(sxz, sy, sxz), 2);
            }

            // HEAVY trail at the ball's current position
            Location pos = c.clone().add(bx, by, 0);
            grayDust(pos, 6, 0.25);
            w.spawnParticle(Particle.SMOKE, pos, 4, 0.2, 0.2, 0.2, 0.01);
            w.spawnParticle(Particle.CRIT, pos, 3, 0.2, 0.2, 0.2, 0.04);
            if (tick % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 3, 0.2, 0.2, 0.2, 0.04);

            // Layered stale-trail of the recent arc, fading with age
            if (tick % 2 == 0) {
                for (int back = 1; back <= 10; back++) {
                    double pastAngle = Math.sin((tick - back * 2) * Math.PI / 30.0) * (Math.PI / 2.2);
                    double px = Math.sin(pastAngle) * 4.0;
                    double py = pivotY - Math.cos(pastAngle) * 3.5;
                    Location p = c.clone().add(px, py, 0);
                    int alpha = Math.max(70, 150 - back * 8);
                    DisplayBuilder.dustParticles(p, 2, 0.08, alpha, alpha, alpha + 10, 1.2f);
                }
            }
            // Whoosh at the ends of the swing
            if (tick % 30 == 15 && tick > 5) DisplayBuilder.playSound(pos, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.7f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new WreckingBallSwingTrail(plugin); }
    }

    // ================================================================
    // 65. PENDULUM AIR DISPLACEMENT — "The Rush"
    //     Combat feedback — air-displacement whoosh on swing mid-arc (no damage)
    //     SPAWN  : big leading-edge SMOKE wall + 2 spinning CHAIN draft markers
    //     ACTIVE : heavy SMOKE sheets perpendicular to motion at peak-speed moments
    //     FADE   : draft markers shrink, trailing wisps settle
    // ================================================================
    public static class PendulumAirDisplacement extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> drafts = new ArrayList<>();
        private static final int DRAFTS = 2;

        public PendulumAirDisplacement(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pendulum_air_displacement", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(120);
            config.setCooldownTicks(80);
            config.setChance(10);
            config.setDesignType("Combat feedback — air-displacement whoosh on swing mid-arc (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 1.1f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.7f, 1.6f);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.8f, 1.6f);

            // Big leading-edge displacement wall
            for (int k = 0; k < 18; k++) {
                Location p = c.clone().add((Math.random() - 0.5) * 4, 1.4 + Math.random() * 1.2, (Math.random() - 0.5) * 4);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.15, 0.15, 0.15, 0.18);
            }

            // 2 spinning CHAIN draft markers at mid-arc height
            for (int i = 0; i < DRAFTS; i++) {
                double off = (i == 0 ? -1.6 : 1.6);
                Location p = c.clone().add(off, 1.7, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                drafts.add(h); spawnedEntities.add(h.entity());
                h.animateTo(new Vector3f((float) off - 0.2f, 1.7f, -0.2f), new AxisAngle4f((float)(i * 1.5), 0, 1, 0),
                        new Vector3f(0.4f, 1.2f, 0.4f), 4);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            boolean fading = tick > config.getDurationTicks() - 25;
            float fscale = fading ? Math.max(0f, 1f - (tick - (config.getDurationTicks() - 25)) / 25f) : 1f;

            double dir = (tick % 60 < 30) ? 1.0 : -1.0;
            // Draft markers slide side-to-side with the rush and spin fast
            for (int i = 0; i < drafts.size(); i++) {
                double baseOff = (i == 0 ? -1.6 : 1.6);
                double slide = Math.sin(tick * Math.PI / 30.0) * 1.4;
                float sxz = 0.4f * fscale;
                float sy = 1.2f * fscale;
                drafts.get(i).animateTo(
                        new Vector3f((float)(baseOff + slide) - sxz / 2, 1.7f - sy / 2, -sxz / 2),
                        new AxisAngle4f((float)(tick * 0.35 + i), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 3);
            }

            // Mid-arc = max speed — heavy perpendicular SMOKE sheet
            double phase = (tick % 30) / 30.0;
            boolean inMidArc = phase < 0.12 || phase > 0.88;
            if (inMidArc) {
                for (int k = 0; k < 10; k++) {
                    double offX = (k - 4.5) * 0.4;
                    Location p = c.clone().add(offX, 1.6 + (Math.random() - 0.5) * 0.6, dir * 0.6);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.06, 0.12, 0.06, 0.16);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.06, 0.1, 0.06, 0.12);
                    grayDust(p, 2, 0.08);
                }
                if (tick % 30 == 0 && tick > 5) DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 0.9f);
            }
            // Background ambient wisps (denser than before)
            if (tick % 3 == 0) {
                for (int k = 0; k < 3; k++) {
                    Location p = c.clone().add((Math.random() - 0.5) * 4, 1.0 + Math.random() * 1.6, (Math.random() - 0.5) * 4);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.06, 0.06, 0.06, 0.01);
                }
            }
            // FADE
            if (fading && tick % 3 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 2.5, 1.0, (Math.random() - 0.5) * 2.5);
                grayDust(p, 2, 0.3);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PendulumAirDisplacement(plugin); }
    }

    // ================================================================
    // 66. CHAIN SNAP BURST — "The Break"
    //     Combat feedback — chain dissipation snap (no damage, atmosphere)
    //     SPAWN  : violent BLOCK(CHAIN) + spark burst + 5 CHAIN shards blasted out
    //     ACTIVE : shards fly + fall w/ gravity, 2 micro echo bursts
    //     FADE   : shards shrink, drifting embers
    // ================================================================
    public static class ChainSnapBurst extends EnvironmentalAttack {
        private static final int SHARDS = 5;
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] sAx = new double[SHARDS];
        private final double[] sAz = new double[SHARDS];
        private final double[] sY = new double[SHARDS];
        private final double[] sVy = new double[SHARDS];

        public ChainSnapBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_snap_burst", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(30);
            config.setCooldownTicks(60);
            config.setChance(10);
            config.setDesignType("Combat feedback — chain dissipation snap (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.8f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.7f, 1.8f);

            Location burst = c.clone().add(0, 1.4, 0);
            w.spawnParticle(Particle.BLOCK, burst, 30, 0.6, 0.6, 0.6, 0.5, Material.CHAIN.createBlockData());
            burstHeavy(w, burst, IRON, 0.7);

            // 5 thick CHAIN shards blasted from the break
            for (int i = 0; i < SHARDS; i++) {
                double a = Math.PI * 2 * i / SHARDS + Math.random() * 0.4;
                sAx[i] = Math.cos(a);
                sAz[i] = Math.sin(a);
                sY[i] = 1.4;
                sVy[i] = 0.28 + Math.random() * 0.12;
                Location p = c.clone().add(0, sY[i], 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                shards.add(h); spawnedEntities.add(h.entity());
                h.animateTo(new Vector3f(-0.2f, (float) sY[i], -0.2f), new AxisAngle4f((float)(i * 0.8), 0, 1, 0),
                        new Vector3f(0.4f, 1.2f, 0.4f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            boolean fading = tick > config.getDurationTicks() - 8;
            float sc = fading ? 0.001f : 0.4f;
            for (int i = 0; i < shards.size(); i++) {
                sVy[i] -= 0.04;
                sY[i] = Math.max(0.2, sY[i] + sVy[i]);
                double radial = Math.min(2.8, tick * 0.3);
                float px = (float)(sAx[i] * radial);
                float pz = (float)(sAz[i] * radial);
                shards.get(i).animateTo(
                        new Vector3f(px - sc / 2f, (float) sY[i] - (fading ? 0.0006f : 0.6f), pz - sc / 2f),
                        new AxisAngle4f((float)(tick * 0.45 + i), 0, 1, 0),
                        new Vector3f(sc, fading ? 0.001f : 1.2f, sc), 2);
                debrisTrail(w, c.clone().add(px, sY[i], pz), tick, Material.CHAIN);
            }

            // Micro echo bursts
            if (tick == 6 || tick == 12) {
                Location p = c.clone().add((Math.random() - 0.5) * 1.8, 1.4, (Math.random() - 0.5) * 1.8);
                w.spawnParticle(Particle.BLOCK, p, 10, 0.25, 0.25, 0.25, 0.25, Material.CHAIN.createBlockData());
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 8, 0.25, 0.25, 0.25, 0.2);
                grayDust(p, 5, 0.25);
            }
            // FADE drifting embers
            if (tick > 18 && tick % 2 == 0) {
                for (int k = 0; k < 3; k++) {
                    Location p = c.clone().add((Math.random() - 0.5) * 2, 0.5 + Math.random() * 1.2, (Math.random() - 0.5) * 2);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.05, 0.1, 0.05, 0.01);
                    grayDust(p, 1, 0.1);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainSnapBurst(plugin); }
    }

    // ================================================================
    // 67. IRON SMASH GROUND CRACK — "The Fracture"
    //     Reactive shockwave (sidestep wide AoE) — IMPACT DAMAGE
    //     SPAWN  : heavy slam burst + 4 thick IRON_BLOCK fracture-spine displays
    //     ACTIVE : spines rip outward, FALLING_DUST + BLOCK fracture lines, smoke
    //     FADE   : spines shrink, line dust settles
    // ================================================================
    public static class IronSmashGroundCrack extends EnvironmentalAttack {
        private static final int LINES = 4;
        private static final int POINTS_PER_LINE = 10;
        private static final int SPAWN_TICKS = 10;
        private final List<ItemDisplayHandle> spines = new ArrayList<>();

        public IronSmashGroundCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_smash_ground_crack", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(80);
            config.setCooldownTicks(120);
            config.setChance(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(180.0);
            config.setImpactRadius(3.0);
            config.setDesignType("Reactive shockwave (sidestep wide AoE)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.7f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.2f, 0.6f);

            triggerImpactDamage(c.clone());

            w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.4, 0), 1, 0, 0, 0, 0);
            burstHeavy(w, c.clone().add(0, 0.5, 0), IRON, 1.0);

            // 4 thick IRON_BLOCK fracture spines, one per crack line
            for (int line = 0; line < LINES; line++) {
                double ang = Math.PI * 2 * line / LINES + Math.random() * 0.2;
                Location p = c.clone().add(Math.cos(ang) * 1.3, 0.12, Math.sin(ang) * 1.3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                spines.add(h);
                h.animateTo(
                        new Vector3f((float)(Math.cos(ang) * 1.3 - 0.6), 0.0f, (float)(Math.sin(ang) * 1.3 - 0.2)),
                        new AxisAngle4f((float) ang, 0, 1, 0),
                        new Vector3f(1.2f, 0.25f, 0.4f), 6);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // First 10 ticks: BLOCK(IRON) fracture lines rip outward
            if (tick < SPAWN_TICKS) {
                for (int line = 0; line < LINES; line++) {
                    double ang = Math.PI * 2 * line / LINES;
                    for (int k = 0; k < POINTS_PER_LINE; k++) {
                        double dist = (k + 1) * 0.35;
                        if (dist > tick * 0.4 + 0.4) continue;
                        Location p = c.clone().add(Math.cos(ang) * dist, 0.08, Math.sin(ang) * dist);
                        w.spawnParticle(Particle.BLOCK, p, 2, 0.08, 0.08, 0.08, 0.02, Material.IRON_BLOCK.createBlockData());
                        if (k % 2 == 0) w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.06, 0.1, 0.06, 0.0, Material.IRON_BLOCK.createBlockData());
                        if (k % 3 == 0) grayDust(p, 2, 0.08);
                    }
                }
            }

            // Spines settle outward slightly + ridge dust
            float fade = (tick > config.getDurationTicks() - 15) ? 0.001f
                    : Math.min(1f, tick / (float) SPAWN_TICKS);
            for (int line = 0; line < spines.size(); line++) {
                double ang = Math.PI * 2 * line / LINES;
                float sx = 1.2f * fade;
                float sy = 0.25f * fade;
                float sz = 0.4f * fade;
                spines.get(line).animateTo(
                        new Vector3f((float)(Math.cos(ang) * 1.3) - sx / 2, 0.0f, (float)(Math.sin(ang) * 1.3) - sz / 2),
                        new AxisAngle4f((float) ang, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }

            // Lingering smoke + sparks along the cracks
            if (tick % 4 == 0) {
                for (int line = 0; line < LINES; line++) {
                    double ang = Math.PI * 2 * line / LINES;
                    for (int k = 1; k <= POINTS_PER_LINE; k += 3) {
                        double dist = k * 0.35;
                        Location p = c.clone().add(Math.cos(ang) * dist, 0.1, Math.sin(ang) * dist);
                        w.spawnParticle(Particle.SMOKE, p, 1, 0.04, 0.08, 0.04, 0.01);
                        if (k % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.03);
                    }
                }
            }
            if (tick == config.getDurationTicks() - 15) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 0.5f, 1.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronSmashGroundCrack(plugin); }
    }

    // ================================================================
    // 68. FALLING WEIGHT WARNING PULSE — "The Omen"
    //     Combat feedback — accelerating drop-warning pulse (no damage)
    //     SPAWN  : big RED_CONCRETE disc + a CHAIN-tethered IRON_BLOCK weight
    //              looming overhead
    //     ACTIVE : disc pulse + spark ramp, the weight descends + grows as the
    //              countdown quickens
    //     FADE   : EXPLOSION burst-and-vanish at impact, weight slams + vanishes
    // ================================================================
    public static class FallingWeightWarningPulse extends EnvironmentalAttack {
        private ItemDisplayHandle indicator;
        private ItemDisplayHandle weight;
        private final List<ItemDisplayHandle> tether = new ArrayList<>();
        private static final int TETHER_LINKS = 4;
        private static final int COUNTDOWN = 60;          // total approach
        private static final int RAMP_START = COUNTDOWN - 20;
        private static final double START_Y = 8.0;

        public FallingWeightWarningPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_weight_warning_pulse", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(COUNTDOWN + 10);
            config.setCooldownTicks(100);
            config.setChance(10);
            config.setDesignType("Combat feedback — accelerating drop-warning pulse (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.7f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.7f);

            Location ground = c.clone();
            ground.setY(c.getY() + 0.05);
            indicator = displayBuilder.spawnItem(ground, new ItemStack(Material.RED_CONCRETE));
            indicator.scale(0.001f, 0.001f, 0.001f).glow(220, 30, 30).interpolation(2, 0);
            spawnedEntities.add(indicator.entity());
            indicator.animateTo(new Vector3f(-1.6f, -0.45f, -1.6f), new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(3.2f, 0.04f, 3.2f), 6);

            // Looming IRON_BLOCK weight overhead
            weight = displayBuilder.spawnItem(c.clone().add(0, START_Y, 0), new ItemStack(Material.IRON_BLOCK));
            weight.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
            spawnedEntities.add(weight.entity());
            weight.animateTo(new Vector3f(-0.6f, (float) START_Y, -0.6f), new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.2f, 1.2f, 1.2f), 6);

            // CHAIN tether up from the weight
            for (int i = 0; i < TETHER_LINKS; i++) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(0, START_Y + 1.0 + i * 0.6, 0), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                tether.add(h); spawnedEntities.add(h.entity());
                h.animateTo(new Vector3f(-0.2f, (float)(START_Y + 1.0 + i * 0.6), -0.2f), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 1.2f, 0.4f), 6);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float scaleMul = 1.0f;
            int sparkCount = 6;
            float baseW = 3.2f;
            if (tick >= RAMP_START && tick < COUNTDOWN) {
                float prog = (tick - RAMP_START) / 20f;
                scaleMul = 1.0f + 0.3f * prog;
                sparkCount = (int)(6 + 18 * prog);
                if (tick % 2 == 0 && indicator != null) {
                    float sz = baseW * scaleMul;
                    indicator.animateTo(new Vector3f(-sz / 2f, -0.45f, -sz / 2f), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sz, 0.04f, sz), 2);
                }
                if (tick % Math.max(2, 10 - (int)(8 * prog)) == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.6f + prog * 0.6f, 1.4f + prog * 0.6f);
                }
            }

            // Weight descends along an ease-in over the countdown (still high until ramp)
            double t = Math.min(1.0, tick / (double) COUNTDOWN);
            double wy = START_Y - (START_Y - 1.2) * (t * t); // accelerating drop
            float ws = (float)(1.2 + scaleMul * 0.3);
            if (weight != null) {
                weight.animateTo(new Vector3f(-ws / 2, (float) wy - ws / 2, -ws / 2),
                        new AxisAngle4f((float)(tick * 0.05), 0, 1, 0), new Vector3f(ws, ws, ws), 3);
            }
            for (int i = 0; i < tether.size(); i++) {
                double ly = wy + 1.0 + i * 0.9;
                tether.get(i).animateTo(new Vector3f(-0.2f, (float) ly - 0.6f, -0.2f), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 1.2f, 0.4f), 3);
            }

            // Spark ring along the disc edge (ramping count)
            if (tick % 3 == 0) {
                float sz = baseW * scaleMul;
                double rr = sz / 2.0;
                for (int k = 0; k < sparkCount; k++) {
                    double a = Math.PI * 2 * k / sparkCount + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * rr, 0.12, Math.sin(a) * rr);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.08, 0.08, 0.08, 0.03);
                    redDust(p, 2, 0.12);
                }
            }
            // Trailing dust from the descending weight
            if (tick % 2 == 0 && weight != null) {
                Location wp = c.clone().add(0, wy, 0);
                w.spawnParticle(Particle.SMOKE, wp, 2, 0.2, 0.2, 0.2, 0.01);
                grayDust(wp, 3, 0.25);
            }

            // FADE — slam-and-vanish at impact
            if (tick == COUNTDOWN) {
                w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 2, 0.3, 0.1, 0.3, 0);
                burstHeavy(w, c.clone().add(0, 0.6, 0), RUST, 1.5);
                redDust(c, 20, 1.6);
                if (indicator != null) indicator.animateTo(new Vector3f(-1.6f, -0.45f, -1.6f), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.001f, 0.001f, 0.001f), 6);
                if (weight != null) weight.animateTo(new Vector3f(-0.6f, 0.4f, -0.6f), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.001f, 0.001f, 0.001f), 6);
                for (ItemDisplayHandle h : tether) {
                    org.bukkit.util.Transformation tr = h.entity().getTransformation();
                    h.entity().setInterpolationDuration(6); h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new org.bukkit.util.Transformation(
                            tr.getTranslation(), tr.getLeftRotation(),
                            new Vector3f(0.001f, 0.001f, 0.001f), tr.getRightRotation()));
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.4f, 0.6f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FallingWeightWarningPulse(plugin); }
    }

    // ================================================================
    // 69. NEAR-MISS SPARKS — "The Dodge"
    //     Combat feedback — near-miss reward burst (no damage, atmosphere)
    //     SPAWN  : bright golden CRIT/END_ROD burst + 2 spinning CHAIN slash shards
    //     ACTIVE : shards arc past + fade, three golden CRIT echoes
    //     FADE   : satisfying END_ROD ember settle
    // ================================================================
    public static class NearMissSparks extends EnvironmentalAttack {
        private static final int SHARDS = 2;
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] sAx = new double[SHARDS];
        private final double[] sAz = new double[SHARDS];

        public NearMissSparks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("near_miss_sparks", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(30);
            config.setCooldownTicks(60);
            config.setChance(10);
            config.setDesignType("Combat feedback — near-miss reward burst (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.0f, 1.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 2.0f);

            Location burst = c.clone().add(0, 1.2, 0);
            w.spawnParticle(Particle.CRIT, burst, 16, 0.4, 0.4, 0.4, 0.35);
            w.spawnParticle(Particle.ELECTRIC_SPARK, burst, 12, 0.4, 0.4, 0.4, 0.3);
            w.spawnParticle(Particle.END_ROD, burst, 8, 0.3, 0.3, 0.3, 0.08);
            DisplayBuilder.dustParticles(burst, 12, 0.45, 255, 215, 80, 1.5f);

            // 2 spinning CHAIN "slash" shards that whoosh past
            for (int i = 0; i < SHARDS; i++) {
                double a = (i == 0 ? 0.6 : Math.PI + 0.6);
                sAx[i] = Math.cos(a);
                sAz[i] = Math.sin(a);
                Location p = c.clone().add(-sAx[i] * 1.2, 1.2, -sAz[i] * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(255, 215, 80).interpolation(2, 0);
                shards.add(h); spawnedEntities.add(h.entity());
                h.animateTo(new Vector3f((float)(-sAx[i] * 1.2) - 0.2f, 1.2f, (float)(-sAz[i] * 1.2) - 0.2f),
                        new AxisAngle4f((float) a, 0, 1, 0), new Vector3f(0.4f, 1.2f, 0.4f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Shards sweep across past the player and fade
            boolean fading = tick > config.getDurationTicks() - 10;
            float sc = fading ? 0.001f : 0.4f;
            for (int i = 0; i < shards.size(); i++) {
                double along = -1.2 + tick * 0.18; // sweeps from -1.2 outward through center
                float px = (float)(sAx[i] * along);
                float pz = (float)(sAz[i] * along);
                shards.get(i).animateTo(new Vector3f(px - sc / 2f, 1.2f, pz - sc / 2f),
                        new AxisAngle4f((float)(tick * 0.5 + i), 0, 1, 0),
                        new Vector3f(sc, fading ? 0.001f : 1.2f, sc), 2);
                Location pos = c.clone().add(px, 1.2, pz);
                DisplayBuilder.dustParticles(pos, 3, 0.12, 255, 215, 80, 1.3f);
                w.spawnParticle(Particle.CRIT, pos, 2, 0.1, 0.1, 0.1, 0.05);
            }

            // Three golden CRIT echoes
            if (tick == 5 || tick == 10 || tick == 15) {
                Location p = c.clone().add((Math.random() - 0.5) * 1.4, 1.2 + Math.random() * 0.5, (Math.random() - 0.5) * 1.4);
                w.spawnParticle(Particle.CRIT, p, 8, 0.25, 0.25, 0.25, 0.2);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 5, 0.2, 0.2, 0.2, 0.15);
                w.spawnParticle(Particle.END_ROD, p, 3, 0.15, 0.15, 0.15, 0.05);
                DisplayBuilder.dustParticles(p, 5, 0.25, 255, 215, 80, 1.4f);
            }
            // FADE
            if (tick > 18 && tick % 2 == 0) {
                for (int k = 0; k < 3; k++) {
                    Location p = c.clone().add((Math.random() - 0.5) * 1.2, 1.4 + Math.random() * 0.4, (Math.random() - 0.5) * 1.2);
                    w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NearMissSparks(plugin); }
    }

    // ================================================================
    // 70. CHAIN TANGLE VISUAL — "The Web"
    //     Combat feedback — post-cage tangle remnant (no damage, atmosphere)
    //     SPAWN  : 8 big tangled CHAIN ItemDisplays at random angles + heavy
    //              BLOCK(CHAIN) burst
    //     ACTIVE : tangle writhes (rotation drift) + sheds debris/sparks
    //     FADE   : whole knot collapses scale → 0
    // ================================================================
    public static class ChainTangleVisual extends EnvironmentalAttack {
        private static final int KNOTS = 8;
        private final List<ItemDisplayHandle> tangles = new ArrayList<>();
        private final float[] tYaw = new float[KNOTS];
        private final float[] tPitch = new float[KNOTS];
        private final double[] tOffX = new double[KNOTS];
        private final double[] tOffY = new double[KNOTS];
        private final double[] tOffZ = new double[KNOTS];

        public ChainTangleVisual(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_tangle_visual", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(40);
            config.setCooldownTicks(80);
            config.setChance(10);
            config.setDesignType("Combat feedback — post-cage tangle remnant (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.9f, 1.0f);
            w.spawnParticle(Particle.BLOCK, c.clone().add(0, 1.4, 0), 24, 0.9, 0.7, 0.9, 0.08, Material.CHAIN.createBlockData());
            burstHeavy(w, c.clone().add(0, 1.4, 0), IRON, 0.9);

            for (int i = 0; i < KNOTS; i++) {
                tYaw[i] = (float)(Math.random() * Math.PI * 2);
                tPitch[i] = (float)((Math.random() - 0.5) * Math.PI);
                tOffX[i] = (Math.random() - 0.5) * 2.0;
                tOffY[i] = 0.6 + Math.random() * 1.8;
                tOffZ[i] = (Math.random() - 0.5) * 2.0;
                Location p = c.clone().add(tOffX[i], tOffY[i], tOffZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                tangles.add(h);
                h.animateTo(
                        new Vector3f((float) tOffX[i] - 0.2f, (float) tOffY[i] - 0.6f, (float) tOffZ[i] - 0.2f),
                        new AxisAngle4f(tYaw[i], (float)Math.cos(tPitch[i]), 0.6f, (float)Math.sin(tPitch[i])),
                        new Vector3f(0.4f, 1.2f, 0.4f), 4);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Tangle writhes — rotation drift + faint position jitter
            int fadeStart = config.getDurationTicks() - 15;
            boolean fading = tick >= fadeStart;
            for (int i = 0; i < tangles.size(); i++) {
                if (fading) continue;
                float wr = tYaw[i] + tick * 0.03f;
                tangles.get(i).animateTo(
                        new Vector3f((float) tOffX[i] - 0.2f, (float) tOffY[i] - 0.6f, (float) tOffZ[i] - 0.2f),
                        new AxisAngle4f(wr, (float)Math.cos(tPitch[i]), 0.6f, (float)Math.sin(tPitch[i])),
                        new Vector3f(0.4f, 1.2f, 0.4f), 4);
            }

            // Sheds debris + sparks
            if (tick % 2 == 0) {
                for (int i = 0; i < KNOTS; i += 2) {
                    Location p = c.clone().add(tOffX[i], tOffY[i], tOffZ[i]);
                    w.spawnParticle(Particle.BLOCK, p, 2, 0.1, 0.25, 0.1, 0.0, Material.CHAIN.createBlockData());
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.08, 0.1, 0.08, 0.02);
                    ironDust(p, 2, 0.12);
                }
            }

            // FADE: whole knot collapses to nothing
            if (tick == fadeStart) {
                for (int i = 0; i < tangles.size(); i++) {
                    tangles.get(i).animateTo(
                            new Vector3f((float) tOffX[i] - 0.0005f, (float) tOffY[i], (float) tOffZ[i] - 0.0005f),
                            new AxisAngle4f(tYaw[i], (float)Math.cos(tPitch[i]), 0.6f, (float)Math.sin(tPitch[i])),
                            new Vector3f(0.001f, 0.001f, 0.001f), 15);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 1.4f);
                w.spawnParticle(Particle.BLOCK, c.clone().add(0, 1.4, 0), 16, 0.8, 0.7, 0.8, 0.06, Material.CHAIN.createBlockData());
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainTangleVisual(plugin); }
    }

    // ================================================================
    // 71. SWING REVERSAL SHOCKWAVE — "The Slam"
    //     Reactive shockwave (sidestep wide AoE) — CONTINUOUS DAMAGE
    //     SPAWN  : heavy slam burst + 6 IRON_BLOCK debris chunks blasted out
    //     ACTIVE : CRIT/ELECTRIC_SPARK ring expands to damage radius, debris arcs +
    //              falls, dust hold ring
    //     FADE   : debris shrink, dust settle
    // ================================================================
    public static class SwingReversalShockwave extends EnvironmentalAttack {
        private static final int PIECES = 6;
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private final double[] dAx = new double[PIECES];
        private final double[] dAz = new double[PIECES];
        private final double[] dY = new double[PIECES];
        private final double[] dVy = new double[PIECES];

        public SwingReversalShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("swing_reversal_shockwave", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(180.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(50);
            config.setCooldownTicks(120);
            config.setChance(10);
            config.setDesignType("Reactive shockwave (sidestep wide AoE)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.2f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 1.0f);

            burstHeavy(w, c.clone().add(0, 0.6, 0), IRON, 1.0);

            // 6 IRON_BLOCK debris chunks blasted out
            for (int i = 0; i < PIECES; i++) {
                double a = Math.PI * 2 * i / PIECES + Math.random() * 0.3;
                dAx[i] = Math.cos(a);
                dAz[i] = Math.sin(a);
                dY[i] = 0.6;
                dVy[i] = 0.25 + Math.random() * 0.12;
                Location p = c.clone().add(0, dY[i], 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                debris.add(h); spawnedEntities.add(h.entity());
                h.animateTo(new Vector3f(-0.22f, (float) dY[i], -0.22f), new AxisAngle4f((float)(i * 0.7), 0, 1, 0),
                        new Vector3f(0.45f, 0.45f, 0.45f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Ring expansion 0 -> damage radius, then hold
            if (tick <= 5) {
                double r = 1.5 * (tick / 5.0);
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.CRIT, 18, null);
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.ELECTRIC_SPARK, 14, null);
            } else if (tick <= 20) {
                double prog = (tick - 5) / 15.0;
                double r = 1.5 + (config.getDamageRadius() - 1.5) * prog;
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.CRIT, 24, null);
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.ELECTRIC_SPARK, 16, null);
                if (tick % 2 == 0) DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.LARGE_SMOKE, 12, null);
            } else if (tick % 3 == 0) {
                double r = config.getDamageRadius();
                for (int k = 0; k < 16; k++) {
                    double a = Math.PI * 2 * k / 16 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                    grayDust(p, 2, 0.12);
                    if (k % 2 == 0) w.spawnParticle(Particle.SMOKE, p, 1, 0.08, 0.08, 0.08, 0.01);
                }
            }

            // Debris physics
            boolean fading = tick > config.getDurationTicks() - 12;
            float chunk = fading ? 0.001f : 0.45f;
            for (int i = 0; i < debris.size(); i++) {
                dVy[i] -= 0.04;
                dY[i] = Math.max(0.2, dY[i] + dVy[i]);
                double radial = Math.min(config.getDamageRadius(), tick * 0.4 + 0.5);
                float px = (float)(dAx[i] * radial);
                float pz = (float)(dAz[i] * radial);
                debris.get(i).animateTo(new Vector3f(px - chunk / 2f, (float) dY[i] - chunk / 2f, pz - chunk / 2f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 0.3f, 1, 0.2f), new Vector3f(chunk, chunk, chunk), 2);
                debrisTrail(w, c.clone().add(px, dY[i], pz), tick, Material.IRON_BLOCK);
            }

            // FADE
            if (fading && tick % 2 == 0) {
                for (int k = 0; k < 6; k++) {
                    Location p = c.clone().add((Math.random() - 0.5) * config.getDamageRadius() * 2, 0.2, (Math.random() - 0.5) * config.getDamageRadius() * 2);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.1, 0.05, 0.1, 0.01);
                    grayDust(p, 1, 0.1);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SwingReversalShockwave(plugin); }
    }

    // ================================================================
    // 72. METAL IMPACT ECHO — "The Ring"
    //     Combat feedback — fading metal echo ring (no damage, atmosphere)
    //     SPAWN  : chain-hit burst + a CHAIN ring of 6 thick shards that pulses out
    //     ACTIVE : FALLING_DUST(IRON) ring expands, shards ripple outward, 3
    //              decreasing-volume echoes
    //     FADE   : shards shrink, last echo decays
    // ================================================================
    public static class MetalImpactEcho extends EnvironmentalAttack {
        private static final int RING = 6;
        private final List<ItemDisplayHandle> shards = new ArrayList<>();

        public MetalImpactEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("metal_impact_echo", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(45);
            config.setCooldownTicks(80);
            config.setChance(10);
            config.setDesignType("Combat feedback — fading metal echo ring (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.0f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.7f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.4f);
            burstHeavy(w, c.clone().add(0, 0.5, 0), IRON, 0.7);

            // 6 thick CHAIN shards in a ring that ripple outward
            for (int i = 0; i < RING; i++) {
                double a = Math.PI * 2 * i / RING;
                Location p = c.clone().add(Math.cos(a) * 0.5, 0.5, Math.sin(a) * 0.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                shards.add(h); spawnedEntities.add(h.entity());
                h.animateTo(new Vector3f((float)(Math.cos(a) * 0.5) - 0.2f, 0.5f, (float)(Math.sin(a) * 0.5) - 0.2f),
                        new AxisAngle4f((float) a, 0, 1, 0), new Vector3f(0.4f, 0.8f, 0.4f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Shards ripple from r=0.5 to r=2.5 then hold; shrink on fade
            boolean fading = tick > config.getDurationTicks() - 10;
            double r = 0.5 + Math.min(2.0, tick * 0.2);
            float sc = fading ? 0.001f : 0.4f;
            for (int i = 0; i < shards.size(); i++) {
                double a = Math.PI * 2 * i / RING + tick * 0.03;
                double by = 0.5 + Math.sin(tick * 0.2 + i) * 0.15;
                shards.get(i).animateTo(
                        new Vector3f((float)(Math.cos(a) * r) - sc / 2f, (float) by - 0.4f, (float)(Math.sin(a) * r) - sc / 2f),
                        new AxisAngle4f((float)(a + tick * 0.05), 0, 1, 0),
                        new Vector3f(sc, fading ? 0.001f : 0.8f, sc), 3);
            }

            // FALLING_DUST(IRON) echo ring r=0.5 -> 2.5 over 10t
            if (tick <= 10) {
                double rr = 0.5 + 2.0 * (tick / 10.0);
                for (int k = 0; k < 20; k++) {
                    double a = Math.PI * 2 * k / 20;
                    Location p = c.clone().add(Math.cos(a) * rr, 0.08, Math.sin(a) * rr);
                    w.spawnParticle(Particle.FALLING_DUST, p, 2, 0.05, 0.08, 0.05, 0, Material.IRON_BLOCK.createBlockData());
                    if (k % 3 == 0) grayDust(p, 2, 0.08);
                    if (k % 4 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.06, 0.06, 0.06, 0.03);
                }
            }

            // 3 decreasing-volume echoes
            if (tick == 8) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.6f, 0.8f);
            if (tick == 16) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.35f, 0.7f);

            // Sustained echo ring
            if (tick > 10 && tick % 4 == 0) {
                for (int k = 0; k < 12; k++) {
                    double a = Math.PI * 2 * k / 12 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * 2.5, 0.08, Math.sin(a) * 2.5);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.04, 0.04, 0.04, 0.01);
                    DisplayBuilder.dustParticles(p, 1, 0.06, 130, 130, 140, 1.2f);
                }
            }
            // FADE — last echo
            if (tick == 36) DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.4f, 1.3f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MetalImpactEcho(plugin); }
    }

    // ================================================================
    // 73. IRON EXPLOSION DEBRIS — "The Scatter"
    //     Reactive shockwave (sidestep wide AoE) — IMPACT DAMAGE
    //     SPAWN  : EXPLOSION + heavy burst + 10 mixed IRON_BLOCK / ANVIL debris
    //              chunks blasted out
    //     ACTIVE : debris arcs out at compass points + falls w/ gravity, smoke +
    //              spark trails
    //     FADE   : pieces lie + shrink
    // ================================================================
    public static class IronExplosionDebris extends EnvironmentalAttack {
        private static final int PIECES = 10;
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private final Material[] mats = new Material[PIECES];
        private final double[] dAx = new double[PIECES];
        private final double[] dAz = new double[PIECES];
        private final double[] dY = new double[PIECES];
        private final double[] dVy = new double[PIECES];

        public IronExplosionDebris(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_explosion_debris", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(70);
            config.setCooldownTicks(120);
            config.setChance(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(250.0);
            config.setImpactRadius(4.5);
            config.setDesignType("Reactive shockwave (sidestep wide AoE)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;

            DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.3f, 0.8f);

            triggerImpactDamage(c.clone());
            w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 3, 0.5, 0.3, 0.5, 0);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            burstHeavy(w, c.clone().add(0, 0.6, 0), IRON, 1.3);

            for (int i = 0; i < PIECES; i++) {
                double a = Math.PI * 2 * i / PIECES + Math.random() * 0.2;
                dAx[i] = Math.cos(a);
                dAz[i] = Math.sin(a);
                dY[i] = 0.6;
                dVy[i] = 0.32 + Math.random() * 0.12;
                mats[i] = (i % 3 == 0) ? Material.ANVIL : Material.IRON_BLOCK;
                Location p = c.clone().add(0, dY[i], 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mats[i]));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                debris.add(h);
                h.animateTo(new Vector3f(-0.25f, (float) dY[i], -0.25f), new AxisAngle4f((float)(i * 0.7), 0, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            boolean fading = tick > config.getDurationTicks() - 15;
            for (int i = 0; i < debris.size(); i++) {
                if (dY[i] > 0.28) {
                    dVy[i] -= 0.045;
                    dY[i] = Math.max(0.28, dY[i] + dVy[i]);
                }
                double radial = Math.min(4.5, 0.42 * tick + 0.5);
                float px = (float)(dAx[i] * radial);
                float pz = (float)(dAz[i] * radial);
                float chunk = fading ? 0.001f : 0.5f;
                debris.get(i).animateTo(new Vector3f(px - chunk / 2f, (float) dY[i] - chunk / 2f, pz - chunk / 2f),
                        new AxisAngle4f((float)(tick * 0.35 + i * 0.6), 0.4f, 1, 0.2f), new Vector3f(chunk, chunk, chunk), 2);
                debrisTrail(w, c.clone().add(px, dY[i], pz), tick, mats[i]);
            }

            // Lingering smoke/dust over the scatter field
            if (tick % 3 == 0) {
                for (int k = 0; k < 5; k++) {
                    double a = Math.random() * Math.PI * 2;
                    double rr = Math.random() * 4.5;
                    Location p = c.clone().add(Math.cos(a) * rr, 0.2, Math.sin(a) * rr);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.1, 0.1, 0.1, 0.01);
                    grayDust(p, 1, 0.1);
                }
            }

            if (tick == config.getDurationTicks() - 15) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 0.6f, 1.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronExplosionDebris(plugin); }
    }

    // ================================================================
    // 74. ATTACK SEQUENCE FINALE — "The Crescendo"
    //     Reactive shockwave (sidestep wide AoE) — CONTINUOUS DAMAGE
    //     SPAWN  : huge multi-layer central burst + 8 ring bursts + 3 big golden
    //              NETHERITE_BLOCK monoliths rising + 6 CHAIN streamers
    //     ACTIVE : monoliths pulse + spin, streamers orbit, sustained golden
    //              damage ring, bell echoes
    //     FADE   : monoliths slam down, golden END_ROD close-out rain
    // ================================================================
    public static class AttackSequenceFinale extends EnvironmentalAttack {
        private static final int MONOLITHS = 3;
        private static final int STREAMERS = 6;
        private final List<ItemDisplayHandle> monoliths = new ArrayList<>();
        private final List<ItemDisplayHandle> streamers = new ArrayList<>();

        public AttackSequenceFinale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("attack_sequence_finale", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(220.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(60);
            config.setCooldownTicks(180);
            config.setChance(10);
            config.setDesignType("Reactive shockwave (sidestep wide AoE)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            World w = c.getWorld(); if (w == null) return;

            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.5f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 1.5f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.3f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.1f, 0.9f);

            // Huge central burst
            w.spawnParticle(Particle.CRIT, c.clone().add(0, 1.0, 0), 36, 1.0, 0.6, 1.0, 0.9);
            w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.0, 0), 28, 0.8, 0.5, 0.8, 0.6);
            w.spawnParticle(Particle.END_ROD, c.clone().add(0, 1.0, 0), 14, 0.6, 0.5, 0.6, 0.1);
            DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 16, 0.8, 255, 215, 80, 1.6f);

            // 8 ring bursts at arena edge
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * 2 * k / 8;
                Location p = c.clone().add(Math.cos(a) * 4.5, 1.0, Math.sin(a) * 4.5);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 12, 0.4, 0.4, 0.4, 0.35);
                w.spawnParticle(Particle.CRIT, p, 8, 0.3, 0.3, 0.3, 0.25);
                DisplayBuilder.dustParticles(p, 8, 0.4, 255, 215, 80, 1.5f);
            }

            // 3 big golden NETHERITE_BLOCK monoliths rising at center
            for (int i = 0; i < MONOLITHS; i++) {
                double a = Math.PI * 2 * i / MONOLITHS;
                Location p = c.clone().add(Math.cos(a) * 0.9, 0.3, Math.sin(a) * 0.9);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHERITE_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(255, 215, 80).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                monoliths.add(h);
                h.animateTo(new Vector3f((float)(Math.cos(a) * 0.9) - 0.45f, 1.0f, (float)(Math.sin(a) * 0.9) - 0.45f),
                        new AxisAngle4f((float) a, 0, 1, 0), new Vector3f(0.9f, 2.0f, 0.9f), 4);
            }
            // 6 CHAIN streamers orbiting
            for (int i = 0; i < STREAMERS; i++) {
                double a = Math.PI * 2 * i / STREAMERS;
                Location p = c.clone().add(Math.cos(a) * 2.0, 1.6, Math.sin(a) * 2.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(255, 200, 60).interpolation(2, 0);
                streamers.add(h); spawnedEntities.add(h.entity());
                h.animateTo(new Vector3f((float)(Math.cos(a) * 2.0) - 0.2f, 1.6f, (float)(Math.sin(a) * 2.0) - 0.2f),
                        new AxisAngle4f((float) a, 0, 1, 0), new Vector3f(0.4f, 1.2f, 0.4f), 4);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            boolean fading = tick > config.getDurationTicks() - 10;

            // Monoliths pulse + spin; slam down on fade
            for (int i = 0; i < monoliths.size(); i++) {
                double a = Math.PI * 2 * i / MONOLITHS;
                if (fading) {
                    monoliths.get(i).animateTo(new Vector3f((float)(Math.cos(a) * 0.9) - 0.45f, 0.2f, (float)(Math.sin(a) * 0.9) - 0.45f),
                            new AxisAngle4f((float)(a + tick * 0.05), 0, 1, 0), new Vector3f(0.001f, 0.001f, 0.001f), 8);
                } else {
                    float pulse = (float)(2.0 + Math.sin(tick * 0.2 + i) * 0.25);
                    monoliths.get(i).animateTo(new Vector3f((float)(Math.cos(a) * 0.9) - 0.45f, 1.0f - pulse / 2 + 1.0f, (float)(Math.sin(a) * 0.9) - 0.45f),
                            new AxisAngle4f((float)(a + tick * 0.04), 0, 1, 0), new Vector3f(0.9f, pulse, 0.9f), 4);
                }
            }
            // Streamers orbit the center
            for (int i = 0; i < streamers.size(); i++) {
                double a = Math.PI * 2 * i / STREAMERS + tick * 0.06;
                double rr = 2.0 + Math.sin(tick * 0.1 + i) * 0.4;
                double by = 1.6 + Math.sin(tick * 0.15 + i) * 0.5;
                float sc = fading ? 0.001f : 0.4f;
                streamers.get(i).animateTo(new Vector3f((float)(Math.cos(a) * rr) - sc / 2f, (float) by - 0.6f, (float)(Math.sin(a) * rr) - sc / 2f),
                        new AxisAngle4f((float) a, 0, 1, 0), new Vector3f(sc, fading ? 0.001f : 1.2f, sc), 3);
            }

            // Sustained golden damage ring
            if (tick % 2 == 0) {
                for (int k = 0; k < 28; k++) {
                    double a = Math.PI * 2 * k / 28 + tick * 0.04;
                    double r = 4.7 + Math.sin(tick * 0.2 + k) * 0.3;
                    Location p = c.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                    if (k % 2 == 0) w.spawnParticle(Particle.CRIT, p, 1, 0.06, 0.1, 0.06, 0.05);
                    if (k % 3 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.06, 0.06, 0.06, 0.05);
                    if (k % 4 == 0) DisplayBuilder.dustParticles(p, 1, 0.1, 255, 200, 60, 1.3f);
                }
            }
            // Bell echoes
            if (tick == 16 || tick == 32 || tick == 48) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.7f, 1.4f + tick * 0.01f);
            }
            // FADE — golden close-out rain
            if (fading && tick % 2 == 0) {
                for (int k = 0; k < 8; k++) {
                    Location p = c.clone().add((Math.random() - 0.5) * 9, 0.3 + Math.random() * 2.0, (Math.random() - 0.5) * 9);
                    w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0.01);
                    if (k % 2 == 0) DisplayBuilder.dustParticles(p, 1, 0.1, 255, 215, 80, 1.2f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AttackSequenceFinale(plugin); }
    }

    // ================================================================
    // 75. CHAIN MODE AMBIENT HEARTBEAT — "The Pulse"
    //     Combat feedback — long ambient heartbeat (no damage, atmosphere)
    //     SPAWN  : low golem-step boom + a central ANVIL "heart" + 4 CHAIN
    //              tether links anchoring it
    //     ACTIVE : the heart THUMPS (scale pulse) every 80t, heavy inward
    //              FALLING_DUST from all 4 walls, expanding pulse halo
    //     FADE   : heart shrinks, trailing low rumble fog
    // ================================================================
    public static class ChainModeAmbientHeartbeat extends EnvironmentalAttack {
        private static final int PULSE_PERIOD = 80;
        private static final double WALL_R = 14.0;
        private ItemDisplayHandle heart;
        private final List<ItemDisplayHandle> anchors = new ArrayList<>();
        private static final int ANCHORS = 4;
        private static final double HEART_Y = 3.0;

        public ChainModeAmbientHeartbeat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_mode_ambient_heartbeat", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(120);
            config.setChance(10);
            config.setDesignType("Combat feedback — long ambient heartbeat (no damage, atmosphere)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.3f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.6f, 0.7f);

            // Central ANVIL "heart"
            heart = displayBuilder.spawnItem(c.clone().add(0, HEART_Y, 0), new ItemStack(Material.ANVIL));
            heart.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
            spawnedEntities.add(heart.entity());
            heart.animateTo(new Vector3f(-0.6f, (float) HEART_Y - 0.6f, -0.6f), new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.2f, 1.2f, 1.2f), 6);

            // 4 CHAIN tether links radiating out (one toward each wall)
            double[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int i = 0; i < ANCHORS; i++) {
                Location p = c.clone().add(dirs[i][0] * 1.4, HEART_Y, dirs[i][1] * 1.4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                anchors.add(h); spawnedEntities.add(h.entity());
                double yaw = Math.atan2(dirs[i][1], dirs[i][0]);
                h.animateTo(new Vector3f((float)(dirs[i][0] * 1.4) - 0.2f, (float) HEART_Y - 0.2f, (float)(dirs[i][1] * 1.4) - 0.2f),
                        new AxisAngle4f((float)(Math.PI / 2), (float) Math.cos(yaw), 0, (float) Math.sin(yaw)),
                        new Vector3f(0.4f, 1.2f, 0.4f), 6);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            boolean fading = tick > config.getDurationTicks() - 20;

            // Heart THUMP — a sharp scale spike right after each pulse, easing back
            int sincePulse = tick % PULSE_PERIOD;
            double thump = 1.2 + (sincePulse < 8 ? (1.0 - sincePulse / 8.0) * 0.5 : 0.0);
            if (heart != null) {
                float s = (float)(fading ? 0.001 : thump);
                heart.animateTo(new Vector3f(-s / 2, (float) HEART_Y - s / 2, -s / 2),
                        new AxisAngle4f((float)(tick * 0.01), 0, 1, 0), new Vector3f(s, s, s), 4);
            }
            double[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int i = 0; i < anchors.size(); i++) {
                double yaw = Math.atan2(dirs[i][1], dirs[i][0]);
                float sc = fading ? 0.001f : 0.4f;
                anchors.get(i).animateTo(new Vector3f((float)(dirs[i][0] * 1.4) - sc / 2f, (float) HEART_Y - sc / 2f, (float)(dirs[i][1] * 1.4) - sc / 2f),
                        new AxisAngle4f((float)(Math.PI / 2), (float) Math.cos(yaw), 0, (float) Math.sin(yaw)),
                        new Vector3f(sc, fading ? 0.001f : 1.2f, sc), 4);
            }

            // 80-tick heartbeat pulses — at tick=0, 80, 160, 240
            if (tick % PULSE_PERIOD == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.3f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.35f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.5f);
                // Heavy FALLING_DUST(IRON) inward from all 4 walls
                double[][] walls = {{-WALL_R, 0}, {WALL_R, 0}, {0, -WALL_R}, {0, WALL_R}};
                for (double[] wall : walls) {
                    for (int k = 0; k < 8; k++) {
                        double ox = wall[0] + (k - 3.5) * (wall[0] == 0 ? 1.0 : 0);
                        double oz = wall[1] + (k - 3.5) * (wall[1] == 0 ? 1.0 : 0);
                        Location p = c.clone().add(ox, 1.5 + Math.random() * 2.5, oz);
                        w.spawnParticle(Particle.FALLING_DUST, p, 2, 0.12, 0.25, 0.12, 0, Material.IRON_BLOCK.createBlockData());
                        grayDust(p, 2, 0.25);
                    }
                }
                // Burst at the heart
                burstHeavy(w, c.clone().add(0, HEART_Y, 0), RUST, 0.8);
            }

            // Expanding pulse halo at the heart for 12 ticks after each pulse
            if (sincePulse < 12) {
                double r = sincePulse * 0.5;
                for (int k = 0; k < 16; k++) {
                    double a = Math.PI * 2 * k / 16 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * r, HEART_Y - 0.5, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.06, RUST[0], RUST[1], RUST[2], 1.3f);
                    if (k % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.06, 0.06, 0.06, 0.03);
                }
            }

            // Per-tick ambient drift across the arena
            if (tick % 8 == 0) {
                for (int k = 0; k < 2; k++) {
                    double rx = (Math.random() - 0.5) * WALL_R * 1.6;
                    double rz = (Math.random() - 0.5) * WALL_R * 1.6;
                    Location p = c.clone().add(rx, 1.0 + Math.random() * 2.5, rz);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.1, 0.1, 0.1, 0.005);
                    grayDust(p, 1, 0.3);
                }
            }
            // FADE
            if (fading && tick % 4 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 6, 0.5, (Math.random() - 0.5) * 6);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.2, 0.1, 0.2, 0.005);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainModeAmbientHeartbeat(plugin); }
    }
}
