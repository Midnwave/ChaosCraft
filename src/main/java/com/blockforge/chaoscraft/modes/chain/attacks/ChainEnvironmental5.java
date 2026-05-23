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
 * Chain Mode — ENVIRONMENTAL ATTACKS 61-75 (Combat Feedback & Event Effects)
 *
 * Reactive visual feedback for the Chain mode's industrial pendulum/chain combat:
 * telegraph indicators, impact crater rings, clash sparks, swing trails, air
 * displacement, snap bursts, ground cracks, warning pulses, near-miss sparks,
 * tangle visuals, reversal shockwaves, metal echoes, explosion debris, sequence
 * finale, ambient heartbeat.
 *
 * All visuals use ItemDisplay entities only (never BlockDisplays — environmental
 * rule). Most are damage=0 atmospheric responses; five are damaging shockwaves
 * with sidestep-wide AoE.
 *
 * Each attack adheres to the three-phase contract:
 *   PHASE 1 — SPAWN FLASH : sudden reactive materialise + telegraph particles
 *   PHASE 2 — ACTIVE      : layered particle work + animateTo motion
 *   PHASE 3 — FADE        : lingering settle + shrink-to-zero on items
 */
public final class ChainEnvironmental5 {
    private ChainEnvironmental5() {}

    private static final String MODE_PATH = "modes/chain/attacks";

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
    // Helpers
    // ================================================================

    private static void redDust(Location l, int count, double spread) {
        DisplayBuilder.dustParticles(l, count, spread, 220, 40, 40, 1.4f);
    }

    private static void orangeDust(Location l, int count, double spread) {
        DisplayBuilder.dustParticles(l, count, spread, 240, 140, 40, 1.2f);
    }

    private static void grayDust(Location l, int count, double spread) {
        DisplayBuilder.dustParticles(l, count, spread, 90, 90, 100, 1.3f);
    }

    // ================================================================
    // 61. ATTACK TELEGRAPH FLOOR INDICATOR — "The Warning"
    //     Combat feedback — incoming-impact telegraph (no damage, atmosphere)
    //     SPAWN  : RED_CONCRETE flat indicator materialises (scale 0→target)
    //     ACTIVE : pulsing scale + ELECTRIC_SPARK ring along its edge
    //     FADE   : shrink-to-zero pre-impact handoff
    // ================================================================
    public static class AttackTelegraphFloorIndicator extends EnvironmentalAttack {
        private ItemDisplayHandle indicator;
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
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.5f, 1.4f);

            Location ground = c.clone();
            ground.setY(c.getY() + 0.05);
            indicator = displayBuilder.spawnItem(ground, new ItemStack(Material.RED_CONCRETE));
            indicator.scale(0.001f, 0.001f, 0.001f).glow(220, 30, 30).interpolation(2, 0);
            spawnedEntities.add(indicator.entity());
            // Grow flat: scale = impact_radius*2 × 0.01 × impact_radius*2 (flat disk-like square)
            float wide = (float)(R * 2.0);
            indicator.animateTo(
                    new Vector3f(-wide / 2f, -0.45f, -wide / 2f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(wide, 0.01f, wide), 6);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Pulse scale ±0.02 over 10-tick period
            float wide = (float)(R * 2.0);
            float pulse = 1.0f + (float)Math.sin(tick * Math.PI / 5.0) * 0.025f;
            if (tick % 5 == 0 && indicator != null) {
                indicator.animateTo(
                        new Vector3f(-wide * pulse / 2f, -0.45f, -wide * pulse / 2f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(wide * pulse, 0.01f, wide * pulse), 5);
            }
            // 8 ELECTRIC_SPARK around the edge every 5 ticks
            if (tick % 5 == 0) {
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI * 2 * k / 8 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * R, 0.1, Math.sin(a) * R);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.02);
                    redDust(p, 1, 0.1);
                }
            }
            // Pre-impact shrink in last 6 ticks
            if (tick == config.getDurationTicks() - 6 && indicator != null) {
                indicator.animateTo(
                        new Vector3f(-wide / 2f, -0.45f, -wide / 2f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.001f, 0.001f, 0.001f), 6);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.8f, 1.0f);
                w.spawnParticle(Particle.ELECTRIC_SPARK, c, 14, 0.6, 0.1, 0.6, 0.1);
            }
        }

        @Override public AbstractAttack newInstance() { return new AttackTelegraphFloorIndicator(plugin); }
    }

    // ================================================================
    // 62. IMPACT CRATER RING — "The Aftermath"
    //     Reactive shockwave (sidestep wide AoE) — IMPACT DAMAGE
    //     SPAWN  : 4 IRON_BLOCK debris pieces sized + heavy slam
    //     ACTIVE : expanding BLOCK(IRON_BLOCK) ring 0→radius+1 over 8t,
    //              debris flies outward and falls
    //     FADE   : debris settle particles
    // ================================================================
    public static class ImpactCraterRing extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private final double[] dAx = new double[4];
        private final double[] dAz = new double[4];
        private final double[] dY = new double[4];
        private final double[] dVy = new double[4];
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

            // Impact damage on spawn — this IS the aftermath
            triggerImpactDamage(c.clone());

            // 4 IRON_BLOCK debris pieces flying outward at compass points
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.random() * 0.3;
                dAx[i] = Math.cos(a);
                dAz[i] = Math.sin(a);
                dY[i] = 0.5;
                dVy[i] = 0.25 + Math.random() * 0.1;
                Location p = c.clone().add(0.0, dY[i], 0.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(2, 0);
                debris.add(h);
                spawnedEntities.add(h.entity());
                h.animateTo(
                        new Vector3f(-0.03f, (float)dY[i], -0.03f),
                        new AxisAngle4f((float)(i * 0.7), 0, 1, 0),
                        new Vector3f(0.06f, 0.08f, 0.06f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // === ACTIVE: 16-position expanding ring of BLOCK(IRON_BLOCK) particles over 8 ticks ===
            if (tick <= 8) {
                double r = (IMPACT_R + 1.0) * (tick / 8.0);
                for (int k = 0; k < 16; k++) {
                    double a = Math.PI * 2 * k / 16;
                    Location p = c.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                    w.spawnParticle(Particle.BLOCK, p, 2, 0.1, 0.1, 0.1, 0.02, Material.IRON_BLOCK.createBlockData());
                    if (k % 2 == 0) w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                    if (k % 3 == 0) grayDust(p, 1, 0.1);
                }
            }

            // Debris physics — arc outward then fall
            for (int i = 0; i < debris.size(); i++) {
                dVy[i] -= 0.04;
                dY[i] = Math.max(0.1, dY[i] + dVy[i]);
                double radial = Math.min(IMPACT_R + 1.0, tick * 0.45);
                float px = (float)(dAx[i] * radial);
                float pz = (float)(dAz[i] * radial);
                debris.get(i).animateTo(
                        new Vector3f(px - 0.03f, (float)dY[i], pz - 0.03f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                        new Vector3f(0.06f, 0.08f, 0.06f), 2);
                Location pos = c.clone().add(px, dY[i], pz);
                w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.05, 0.05, 0.05, 0, Material.IRON_BLOCK.createBlockData());
                if (tick % 3 == 0) w.spawnParticle(Particle.SMOKE, pos, 1, 0.05, 0.05, 0.05, 0.01);
            }

            // FADE — settle dust
            if (tick > 40 && tick % 4 == 0) {
                for (int k = 0; k < 4; k++) {
                    double a = Math.random() * Math.PI * 2;
                    double rr = Math.random() * (IMPACT_R + 0.5);
                    Location p = c.clone().add(Math.cos(a) * rr, 0.1, Math.sin(a) * rr);
                    grayDust(p, 1, 0.1);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ImpactCraterRing(plugin); }
    }

    // ================================================================
    // 63. CHAIN CLASH SPARKS — "The Contact"
    //     Combat feedback — pendulum reversal-point spark burst (no damage)
    //     SPAWN  : ELECTRIC_SPARK + CRIT burst at center
    //     ACTIVE : 3 secondary clash bursts in random offsets
    //     FADE   : dwindling ember particles
    // ================================================================
    public static class ChainClashSparks extends EnvironmentalAttack {
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.8f);

            // Initial big burst
            Location burst = c.clone().add(0, 1.4, 0);
            w.spawnParticle(Particle.ELECTRIC_SPARK, burst, 12, 0.4, 0.4, 0.4, 0.3);
            w.spawnParticle(Particle.CRIT, burst, 8, 0.3, 0.3, 0.3, 0.2);
            w.spawnParticle(Particle.SMOKE, burst, 6, 0.3, 0.3, 0.3, 0.05);
            orangeDust(burst, 8, 0.4);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // 3 secondary clash bursts at 8, 16, 24 ticks at random offsets
            if (tick == 8 || tick == 16 || tick == 24) {
                double rx = (Math.random() - 0.5) * 3;
                double rz = (Math.random() - 0.5) * 3;
                Location p = c.clone().add(rx, 1.0 + Math.random() * 0.8, rz);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 8, 0.3, 0.3, 0.3, 0.25);
                w.spawnParticle(Particle.CRIT, p, 5, 0.2, 0.2, 0.2, 0.15);
                orangeDust(p, 4, 0.3);
                DisplayBuilder.playSound(p, Sound.BLOCK_CHAIN_HIT, 0.8f, 1.6f);
            }
            // FADE: dwindling embers
            if (tick > 28 && tick % 2 == 0) {
                for (int k = 0; k < 3; k++) {
                    double rx = (Math.random() - 0.5) * 2;
                    double rz = (Math.random() - 0.5) * 2;
                    Location p = c.clone().add(rx, 1.2 + Math.random() * 0.4, rz);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.1, 0.05, 0.02);
                    orangeDust(p, 1, 0.1);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainClashSparks(plugin); }
    }

    // ================================================================
    // 64. WRECKING BALL SWING TRAIL — "The Arc"
    //     Combat feedback — fading arc trace behind a swinging ball (no damage)
    //     SPAWN  : phantom trail-emitter ItemDisplay (NETHERITE_SCRAP) at center
    //     ACTIVE : arc of DUST particles traced through space, decay
    //     FADE   : trail particles thin out
    // ================================================================
    public static class WreckingBallSwingTrail extends EnvironmentalAttack {
        private ItemDisplayHandle emitter;
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 0.4f, 0.7f);

            emitter = displayBuilder.spawnItem(c.clone().add(0, 4.0, 0), new ItemStack(Material.NETHERITE_SCRAP));
            emitter.scale(0.001f, 0.001f, 0.001f).glow(60, 60, 70).interpolation(2, 0);
            spawnedEntities.add(emitter.entity());
            emitter.animateTo(
                    new Vector3f(-0.15f, 4.0f, -0.15f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.3f, 0.3f, 0.3f), 4);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Swing motion — pendulum arc 4 blocks radius, period 60 ticks
            trailAngle = Math.sin(tick * Math.PI / 30.0) * (Math.PI / 2.2);
            double bx = Math.sin(trailAngle) * 4.0;
            double by = 4.0 - Math.cos(trailAngle) * 3.5;
            if (emitter != null) {
                emitter.animateTo(
                        new Vector3f((float)bx - 0.15f, (float)by, -0.15f),
                        new AxisAngle4f((float)(tick * 0.4), 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 0.3f), 2);
            }
            // DUST trail every 2 ticks at current position
            if (tick % 2 == 0) {
                Location pos = c.clone().add(bx, by, 0);
                grayDust(pos, 3, 0.1);
                w.spawnParticle(Particle.SMOKE, pos, 1, 0.05, 0.05, 0.05, 0.01);
            }
            // Layered "stale" trail: scatter remembered points along sin-arc fading
            if (tick % 3 == 0) {
                for (int back = 1; back <= 8; back++) {
                    double pastAngle = Math.sin((tick - back * 2) * Math.PI / 30.0) * (Math.PI / 2.2);
                    double px = Math.sin(pastAngle) * 4.0;
                    double py = 4.0 - Math.cos(pastAngle) * 3.5;
                    Location p = c.clone().add(px, py, 0);
                    int alpha = Math.max(70, 130 - back * 8);
                    DisplayBuilder.dustParticles(p, 1, 0.05, alpha, alpha, alpha + 10, 1.0f);
                }
            }
            // FADE
            if (tick > config.getDurationTicks() - 30 && emitter != null && tick % 6 == 0) {
                emitter.animateTo(
                        new Vector3f((float)bx - 0.15f, (float)by, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.001f, 0.001f, 0.001f), 12);
            }
        }

        @Override public AbstractAttack newInstance() { return new WreckingBallSwingTrail(plugin); }
    }

    // ================================================================
    // 65. PENDULUM AIR DISPLACEMENT — "The Rush"
    //     Combat feedback — high-speed mid-arc rush bursts (no damage)
    //     SPAWN  : whoosh sound + initial leading-edge burst
    //     ACTIVE : SMOKE bursts perpendicular to motion at peak-speed moments
    //     FADE   : trailing wisps settle
    // ================================================================
    public static class PendulumAirDisplacement extends EnvironmentalAttack {
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
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.9f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.5f, 1.6f);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.6f);

            World w = c.getWorld(); if (w == null) return;
            // Initial leading-edge displacement
            for (int k = 0; k < 10; k++) {
                Location p = c.clone().add((Math.random() - 0.5) * 3, 1.5 + Math.random() * 0.8, (Math.random() - 0.5) * 3);
                w.spawnParticle(Particle.SMOKE, p, 1, 0.1, 0.1, 0.1, 0.15);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Approximate "mid-arc = max speed" — every ~30 ticks (when sin(t*π/30)≈0)
            double phase = (tick % 30) / 30.0;
            boolean inMidArc = phase < 0.1 || phase > 0.9;

            if (inMidArc) {
                // Leading SMOKE burst perpendicular to motion (z-axis displacement)
                double dir = (tick % 60 < 30) ? 1.0 : -1.0;
                for (int k = 0; k < 6; k++) {
                    double offX = (k - 2.5) * 0.4;
                    Location p = c.clone().add(offX, 1.6, dir * 0.5);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.05, 0.1, 0.05, 0.15);
                    grayDust(p, 1, 0.05);
                }
                if (tick % 30 == 0 && tick > 5) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.9f);
                }
            }
            // Background ambient wisps
            if (tick % 4 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 3, 1.0 + Math.random() * 1.5, (Math.random() - 0.5) * 3);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.05, 0.05, 0.05, 0.01);
            }
            // FADE
            if (tick > config.getDurationTicks() - 20 && tick % 3 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 2, 1.0, (Math.random() - 0.5) * 2);
                grayDust(p, 1, 0.3);
            }
        }

        @Override public AbstractAttack newInstance() { return new PendulumAirDisplacement(plugin); }
    }

    // ================================================================
    // 66. CHAIN SNAP BURST — "The Break"
    //     Combat feedback — single-tick chain-dissipate snap (no damage)
    //     SPAWN  : immediate BLOCK(CHAIN) + ELECTRIC_SPARK burst, single tick
    //     ACTIVE : two micro echo bursts
    //     FADE   : drifting embers settle
    // ================================================================
    public static class ChainSnapBurst extends EnvironmentalAttack {
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

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.6f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.2f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.4f, 1.8f);

            // Single-tick burst (per spec)
            Location burst = c.clone().add(0, 1.4, 0);
            w.spawnParticle(Particle.BLOCK, burst, 20, 0.5, 0.5, 0.5, 0.4, Material.CHAIN.createBlockData());
            w.spawnParticle(Particle.ELECTRIC_SPARK, burst, 15, 0.4, 0.4, 0.4, 0.3);
            w.spawnParticle(Particle.SMOKE, burst, 10, 0.3, 0.3, 0.3, 0.1);
            grayDust(burst, 12, 0.4);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Micro echo bursts
            if (tick == 6 || tick == 12) {
                Location p = c.clone().add((Math.random() - 0.5) * 1.5, 1.4, (Math.random() - 0.5) * 1.5);
                w.spawnParticle(Particle.BLOCK, p, 6, 0.2, 0.2, 0.2, 0.2, Material.CHAIN.createBlockData());
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.2, 0.2, 0.2, 0.15);
            }
            // FADE drifting embers
            if (tick > 18 && tick % 2 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 1.5, 0.5 + Math.random() * 1.0, (Math.random() - 0.5) * 1.5);
                w.spawnParticle(Particle.SMOKE, p, 1, 0.05, 0.1, 0.05, 0.01);
                grayDust(p, 1, 0.1);
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainSnapBurst(plugin); }
    }

    // ================================================================
    // 67. IRON SMASH GROUND CRACK — "The Fracture"
    //     Reactive shockwave (sidestep wide AoE) — IMPACT DAMAGE
    //     SPAWN  : heavy slam sound + central FALLING_DUST burst
    //     ACTIVE : 4 outward fracture lines extend, then linger 60t
    //     FADE   : line dust settles
    // ================================================================
    public static class IronSmashGroundCrack extends EnvironmentalAttack {
        private static final int LINES = 4;
        private static final int POINTS_PER_LINE = 8;
        private static final int SPAWN_TICKS = 10;
        private final List<ItemDisplayHandle> markers = new ArrayList<>();

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
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.0f, 0.6f);

            triggerImpactDamage(c.clone());

            w.spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 0.5, 0), 14, 0.5, 0.3, 0.5, 0.05, Material.IRON_BLOCK.createBlockData());

            // 4 line-marker ItemDisplays (one per line, mid-length, low scale)
            for (int line = 0; line < LINES; line++) {
                double ang = Math.PI * 2 * line / LINES + Math.random() * 0.2;
                Location p = c.clone().add(Math.cos(ang) * 1.2, 0.06, Math.sin(ang) * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_NUGGET));
                h.scale(0.001f, 0.001f, 0.001f).glow(140, 140, 160).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                markers.add(h);
                float yawRad = (float) ang;
                h.animateTo(
                        new Vector3f((float)(Math.cos(ang) * 1.2 - 0.15), 0.06f, (float)(Math.sin(ang) * 1.2 - 0.04)),
                        new AxisAngle4f(yawRad, 0, 1, 0),
                        new Vector3f(0.6f, 0.06f, 0.1f), 6);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // First 10 ticks: lines extend outward, 1 particle per tick per line
            if (tick < SPAWN_TICKS) {
                for (int line = 0; line < LINES; line++) {
                    double ang = Math.PI * 2 * line / LINES;
                    for (int k = 0; k < POINTS_PER_LINE; k++) {
                        double dist = (k + 1) * 0.3;
                        if (dist > tick * 0.3 + 0.3) continue;
                        Location p = c.clone().add(Math.cos(ang) * dist, 0.05, Math.sin(ang) * dist);
                        w.spawnParticle(Particle.BLOCK, p, 1, 0.05, 0.05, 0.05, 0, Material.IRON_BLOCK.createBlockData());
                        if (k % 2 == 0) grayDust(p, 1, 0.05);
                    }
                }
            }

            // Persist + lingering ambient on lines
            if (tick % 6 == 0) {
                for (int line = 0; line < LINES; line++) {
                    double ang = Math.PI * 2 * line / LINES;
                    for (int k = 0; k < POINTS_PER_LINE; k++) {
                        double dist = (k + 1) * 0.3;
                        Location p = c.clone().add(Math.cos(ang) * dist, 0.05, Math.sin(ang) * dist);
                        if (Math.random() < 0.3) {
                            w.spawnParticle(Particle.SMOKE, p, 1, 0.02, 0.05, 0.02, 0.01);
                        }
                    }
                }
            }

            // FADE
            if (tick > 65) {
                for (ItemDisplayHandle h : markers) {
                    if (tick == 66) {
                        org.bukkit.util.Transformation t = h.entity().getTransformation();
                        h.entity().setInterpolationDuration(14);
                        h.entity().setInterpolationDelay(0);
                        h.entity().setTransformation(new org.bukkit.util.Transformation(
                                t.getTranslation(),
                                t.getLeftRotation(),
                                new Vector3f(0.001f, 0.001f, 0.001f),
                                t.getRightRotation()));
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IronSmashGroundCrack(plugin); }
    }

    // ================================================================
    // 68. FALLING WEIGHT WARNING PULSE — "The Omen"
    //     Combat feedback — accelerating drop-warning indicator (no damage)
    //     SPAWN  : RED_CONCRETE flat indicator (small)
    //     ACTIVE : scale + spark count ramp over the last 20 ticks of approach
    //     FADE   : burst-and-vanish at impact moment
    // ================================================================
    public static class FallingWeightWarningPulse extends EnvironmentalAttack {
        private ItemDisplayHandle indicator;
        private static final int COUNTDOWN = 60;          // total approach
        private static final int RAMP_START = COUNTDOWN - 20;

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
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.6f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.5f, 1.4f);

            Location ground = c.clone();
            ground.setY(c.getY() + 0.05);
            indicator = displayBuilder.spawnItem(ground, new ItemStack(Material.RED_CONCRETE));
            indicator.scale(0.001f, 0.001f, 0.001f).glow(220, 30, 30).interpolation(2, 0);
            spawnedEntities.add(indicator.entity());
            indicator.animateTo(
                    new Vector3f(-1.5f, -0.45f, -1.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(3.0f, 0.01f, 3.0f), 6);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Ramp scale 1.0 → 1.3 + spark count 4 → 20 in last 20 ticks
            float scaleMul = 1.0f;
            int sparkCount = 4;
            float baseW = 3.0f;
            if (tick >= RAMP_START && tick < COUNTDOWN) {
                float prog = (tick - RAMP_START) / 20f;
                scaleMul = 1.0f + 0.3f * prog;
                sparkCount = (int)(4 + 16 * prog);
                // Pulse animation
                if (tick % 2 == 0 && indicator != null) {
                    float sz = baseW * scaleMul;
                    indicator.animateTo(
                            new Vector3f(-sz / 2f, -0.45f, -sz / 2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sz, 0.01f, sz), 2);
                }
                // Quickening pulse sound
                if (tick % Math.max(2, 10 - (int)(8 * prog)) == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.5f + prog * 0.6f, 1.4f + prog * 0.6f);
                }
            }
            // ELECTRIC_SPARK ring along edge
            if (tick % 3 == 0) {
                float sz = baseW * scaleMul;
                double rr = sz / 2.0;
                for (int k = 0; k < sparkCount; k++) {
                    double a = Math.PI * 2 * k / sparkCount + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * rr, 0.1, Math.sin(a) * rr);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.02);
                    if (k % 2 == 0) redDust(p, 1, 0.1);
                }
            }

            // FADE — burst-and-vanish at impact
            if (tick == COUNTDOWN) {
                w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, c, 25, 1.5, 0.2, 1.5, 0.2);
                redDust(c, 15, 1.5);
                if (indicator != null) {
                    indicator.animateTo(
                            new Vector3f(-1.5f, -0.45f, -1.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.001f, 0.001f, 0.001f), 6);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.4f, 0.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FallingWeightWarningPulse(plugin); }
    }

    // ================================================================
    // 69. NEAR-MISS SPARKS — "The Dodge"
    //     Combat feedback — near-miss reward spark burst (no damage)
    //     SPAWN  : immediate CRIT + ELECTRIC_SPARK burst at narrow-escape point
    //     ACTIVE : three secondary CRIT echoes
    //     FADE   : satisfying ember settle
    // ================================================================
    public static class NearMissSparks extends EnvironmentalAttack {
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
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.8f, 1.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 2.0f);

            Location burst = c.clone().add(0, 1.2, 0);
            w.spawnParticle(Particle.CRIT, burst, 10, 0.3, 0.3, 0.3, 0.3);
            w.spawnParticle(Particle.ELECTRIC_SPARK, burst, 8, 0.3, 0.3, 0.3, 0.25);
            w.spawnParticle(Particle.END_ROD, burst, 4, 0.2, 0.2, 0.2, 0.05);
            DisplayBuilder.dustParticles(burst, 6, 0.3, 255, 215, 80, 1.3f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Three CRIT echoes
            if (tick == 5 || tick == 10 || tick == 15) {
                Location p = c.clone().add((Math.random() - 0.5) * 1.0, 1.2 + Math.random() * 0.4, (Math.random() - 0.5) * 1.0);
                w.spawnParticle(Particle.CRIT, p, 5, 0.2, 0.2, 0.2, 0.2);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.15, 0.15, 0.15, 0.15);
                DisplayBuilder.dustParticles(p, 3, 0.2, 255, 215, 80, 1.2f);
            }
            // FADE
            if (tick > 18 && tick % 2 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 1.0, 1.4 + Math.random() * 0.3, (Math.random() - 0.5) * 1.0);
                w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        @Override public AbstractAttack newInstance() { return new NearMissSparks(plugin); }
    }

    // ================================================================
    // 70. CHAIN TANGLE VISUAL — "The Web"
    //     Combat feedback — post-cage tangle remnant (no damage)
    //     SPAWN  : 6 CHAIN ItemDisplays at random angles + BLOCK(CHAIN) burst
    //     ACTIVE : displays hold tangle pose
    //     FADE   : scale → 0 over 15 ticks
    // ================================================================
    public static class ChainTangleVisual extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tangles = new ArrayList<>();
        private final float[] tYaw = new float[6];
        private final float[] tPitch = new float[6];
        private final double[] tOffX = new double[6];
        private final double[] tOffY = new double[6];
        private final double[] tOffZ = new double[6];

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

            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 1.0f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.7f, 1.0f);
            w.spawnParticle(Particle.BLOCK, c.clone().add(0, 1.2, 0), 15, 0.7, 0.5, 0.7, 0.05, Material.CHAIN.createBlockData());

            for (int i = 0; i < 6; i++) {
                tYaw[i] = (float)(Math.random() * Math.PI * 2);
                tPitch[i] = (float)((Math.random() - 0.5) * Math.PI);
                tOffX[i] = (Math.random() - 0.5) * 1.6;
                tOffY[i] = 0.5 + Math.random() * 1.6;
                tOffZ[i] = (Math.random() - 0.5) * 1.6;
                Location p = c.clone().add(tOffX[i], tOffY[i], tOffZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(80, 80, 90).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                tangles.add(h);
                h.animateTo(
                        new Vector3f((float)tOffX[i] - 0.05f, (float)tOffY[i], (float)tOffZ[i] - 0.05f),
                        new AxisAngle4f(tYaw[i], (float)Math.cos(tPitch[i]), 0.6f, (float)Math.sin(tPitch[i])),
                        new Vector3f(0.1f, 0.5f, 0.1f), 4);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ACTIVE: subtle dust drift + occasional BLOCK(CHAIN) wisps
            if (tick % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    Location p = c.clone().add(tOffX[i], tOffY[i], tOffZ[i]);
                    if (i % 2 == 0) w.spawnParticle(Particle.BLOCK, p, 1, 0.05, 0.2, 0.05, 0, Material.CHAIN.createBlockData());
                    if (i % 3 == 0) grayDust(p, 1, 0.1);
                }
            }

            // FADE: shrink scale → 0 over last 15 ticks
            int fadeStart = config.getDurationTicks() - 15;
            if (tick == fadeStart) {
                for (int i = 0; i < tangles.size(); i++) {
                    tangles.get(i).animateTo(
                            new Vector3f((float)tOffX[i] - 0.05f, (float)tOffY[i], (float)tOffZ[i] - 0.05f),
                            new AxisAngle4f(tYaw[i], (float)Math.cos(tPitch[i]), 0.6f, (float)Math.sin(tPitch[i])),
                            new Vector3f(0.001f, 0.001f, 0.001f), 15);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 1.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainTangleVisual(plugin); }
    }

    // ================================================================
    // 71. SWING REVERSAL SHOCKWAVE — "The Slam"
    //     Reactive shockwave (sidestep wide AoE) — CONTINUOUS DAMAGE
    //     SPAWN  : initial slam sound + CRIT burst
    //     ACTIVE : CRIT ring expands 0→1.5 over 5t (per spec), then ambient
    //     FADE   : dust settle
    // ================================================================
    public static class SwingReversalShockwave extends EnvironmentalAttack {
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
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.0f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 1.0f);

            w.spawnParticle(Particle.CRIT, c.clone().add(0, 0.6, 0), 14, 0.3, 0.3, 0.3, 0.4);
            w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.3, 0), 8, 0.4, 0.1, 0.4, 0.03);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // 5-tick ring expansion 0→1.5; then continue out to damage radius
            if (tick <= 5) {
                double r = 1.5 * (tick / 5.0);
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.CRIT, 12, null);
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.ELECTRIC_SPARK, 8, null);
            } else if (tick <= 20) {
                // Wider damaging ring expand
                double prog = (tick - 5) / 15.0;
                double r = 1.5 + (config.getDamageRadius() - 1.5) * prog;
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.CRIT, 18, null);
                if (tick % 2 == 0) DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.SMOKE, 12, null);
            } else if (tick % 3 == 0) {
                // Hold the radius with intermittent dust
                double r = config.getDamageRadius();
                for (int k = 0; k < 12; k++) {
                    double a = Math.PI * 2 * k / 12 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                    grayDust(p, 1, 0.1);
                }
            }
            // FADE
            if (tick > config.getDurationTicks() - 10 && tick % 2 == 0) {
                for (int k = 0; k < 6; k++) {
                    Location p = c.clone().add((Math.random() - 0.5) * config.getDamageRadius() * 2, 0.2, (Math.random() - 0.5) * config.getDamageRadius() * 2);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.1, 0.05, 0.1, 0.01);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SwingReversalShockwave(plugin); }
    }

    // ================================================================
    // 72. METAL IMPACT ECHO — "The Ring"
    //     Combat feedback — fading echo ring + decaying chain hit sound (no damage)
    //     SPAWN  : initial soft chain-hit sound + central FALLING_DUST burst
    //     ACTIVE : circle of FALLING_DUST(IRON) expands r=0.5→2.5 over 10t,
    //              3 decreasing-volume echo sounds at 0/8/16t
    //     FADE   : last echo decays
    // ================================================================
    public static class MetalImpactEcho extends EnvironmentalAttack {
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.8f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.5f, 0.7f);
            w.spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 0.1, 0), 10, 0.3, 0.05, 0.3, 0, Material.IRON_BLOCK.createBlockData());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // r=0.5→2.5 over 10t at Y=0.05
            if (tick <= 10) {
                double r = 0.5 + 2.0 * (tick / 10.0);
                for (int k = 0; k < 16; k++) {
                    double a = Math.PI * 2 * k / 16;
                    Location p = c.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                    w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.03, 0.05, 0.03, 0, Material.IRON_BLOCK.createBlockData());
                    if (k % 3 == 0) grayDust(p, 1, 0.05);
                }
            }

            // 3 decreasing-volume echoes — at 8 and 16
            if (tick == 8) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.4f, 0.8f);
            if (tick == 16) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.2f, 0.7f);

            // Sustained echo: thin ring at max radius
            if (tick > 10 && tick % 4 == 0) {
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI * 2 * k / 8 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * 2.5, 0.05, Math.sin(a) * 2.5);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.03, 0.03, 0.03, 0.01);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 130, 130, 140, 1.0f);
                }
            }

            // FADE — last echo
            if (tick == 36) DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.3f, 1.3f);
        }

        @Override public AbstractAttack newInstance() { return new MetalImpactEcho(plugin); }
    }

    // ================================================================
    // 73. IRON EXPLOSION DEBRIS — "The Scatter"
    //     Reactive shockwave (sidestep wide AoE) — IMPACT DAMAGE
    //     SPAWN  : initial EXPLOSION particle + 8 IRON_BLOCK debris pieces
    //     ACTIVE : debris arcs outward at 8 compass points, then falls
    //     FADE   : pieces lie on ground 20t then shrink
    // ================================================================
    public static class IronExplosionDebris extends EnvironmentalAttack {
        private static final int PIECES = 8;
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
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

            DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.8f);

            triggerImpactDamage(c.clone());
            w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.5, 0), 10, 0.5, 0.3, 0.5, 0.05);

            for (int i = 0; i < PIECES; i++) {
                double a = Math.PI * 2 * i / PIECES;
                dAx[i] = Math.cos(a);
                dAz[i] = Math.sin(a);
                dY[i] = 0.5;
                dVy[i] = 0.30 + Math.random() * 0.1;
                Location p = c.clone().add(0, dY[i], 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(170, 170, 190).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                debris.add(h);
                h.animateTo(
                        new Vector3f(-0.04f, (float)dY[i], -0.04f),
                        new AxisAngle4f((float)(i * 0.7), 0, 1, 0),
                        new Vector3f(0.08f, 0.1f, 0.08f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Debris physics: outward velocity ~0.4/tick until landed, then settle
            for (int i = 0; i < debris.size(); i++) {
                if (dY[i] > 0.12) {
                    dVy[i] -= 0.045;
                    dY[i] = Math.max(0.12, dY[i] + dVy[i]);
                }
                double radial = Math.min(4.5, 0.4 * tick + 0.5);
                float px = (float)(dAx[i] * radial);
                float pz = (float)(dAz[i] * radial);
                debris.get(i).animateTo(
                        new Vector3f(px - 0.04f, (float)dY[i], pz - 0.04f),
                        new AxisAngle4f((float)(tick * 0.35 + i * 0.6), 0, 1, 0),
                        new Vector3f(0.08f, 0.1f, 0.08f), 2);

                Location pos = c.clone().add(px, dY[i], pz);
                w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.05, 0.05, 0.05, 0, Material.IRON_BLOCK.createBlockData());
                if (tick % 3 == 0) w.spawnParticle(Particle.SMOKE, pos, 1, 0.05, 0.05, 0.05, 0.01);
                if (tick % 5 == 0) grayDust(pos, 1, 0.1);
            }

            // FADE: shrink at tick=duration-15
            int fadeStart = config.getDurationTicks() - 15;
            if (tick == fadeStart) {
                for (int i = 0; i < debris.size(); i++) {
                    double radial = Math.min(4.5, 0.4 * tick + 0.5);
                    float px = (float)(dAx[i] * radial);
                    float pz = (float)(dAz[i] * radial);
                    debris.get(i).animateTo(
                            new Vector3f(px - 0.04f, (float)dY[i], pz - 0.04f),
                            new AxisAngle4f((float)(tick * 0.35 + i * 0.6), 0, 1, 0),
                            new Vector3f(0.001f, 0.001f, 0.001f), 14);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 0.5f, 1.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new IronExplosionDebris(plugin); }
    }

    // ================================================================
    // 74. ATTACK SEQUENCE FINALE — "The Crescendo"
    //     Reactive shockwave (sidestep wide AoE) — CONTINUOUS DAMAGE
    //     SPAWN  : 24-CRIT central burst + 8 ELECTRIC_SPARK ring positions
    //     ACTIVE : 3 NETHERITE_INGOT ItemDisplays grow 0→0.5 in 3t then shrink
    //     FADE   : satisfying close-out particles
    // ================================================================
    public static class AttackSequenceFinale extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ingots = new ArrayList<>();

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

            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 1.4f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.2f, 0.8f);

            // 24 CRIT from arena center
            w.spawnParticle(Particle.CRIT, c.clone().add(0, 1.0, 0), 24, 0.8, 0.5, 0.8, 0.8);
            w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.0, 0), 18, 0.6, 0.4, 0.6, 0.5);
            // 8 ELECTRIC_SPARK bursts at 8 arena positions
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * 2 * k / 8;
                Location p = c.clone().add(Math.cos(a) * 4.5, 1.0, Math.sin(a) * 4.5);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 8, 0.3, 0.3, 0.3, 0.3);
                w.spawnParticle(Particle.CRIT, p, 4, 0.2, 0.2, 0.2, 0.2);
                DisplayBuilder.dustParticles(p, 4, 0.3, 255, 215, 80, 1.4f);
            }
            // 3 NETHERITE_INGOT ItemDisplays at center: grow 0→0.5 in 3t
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 * i / 3;
                Location p = c.clone().add(Math.cos(a) * 0.5, 1.6 + i * 0.4, Math.sin(a) * 0.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHERITE_INGOT));
                h.scale(0.001f, 0.001f, 0.001f).glow(255, 215, 80).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                ingots.add(h);
                h.animateTo(
                        new Vector3f((float)(Math.cos(a) * 0.5 - 0.25), (float)(1.6 + i * 0.4), (float)(Math.sin(a) * 0.5 - 0.25)),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), 3);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // After 3t, shrink ingots 0.5 → 0 over 20t
            if (tick == 4) {
                for (int i = 0; i < ingots.size(); i++) {
                    double a = Math.PI * 2 * i / 3;
                    ingots.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * 0.5 - 0.25), (float)(1.6 + i * 0.4), (float)(Math.sin(a) * 0.5 - 0.25)),
                            new AxisAngle4f((float)(i * 0.5), 0, 1, 0),
                            new Vector3f(0.001f, 0.001f, 0.001f), 20);
                }
            }
            // Sustain finale glow / ring damage area
            if (tick % 2 == 0) {
                for (int k = 0; k < 24; k++) {
                    double a = Math.PI * 2 * k / 24 + tick * 0.04;
                    double r = 4.5 + Math.sin(tick * 0.2 + k) * 0.3;
                    Location p = c.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                    if (k % 3 == 0) w.spawnParticle(Particle.CRIT, p, 1, 0.05, 0.1, 0.05, 0.05);
                    if (k % 4 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.05);
                    if (k % 5 == 0) DisplayBuilder.dustParticles(p, 1, 0.1, 255, 200, 60, 1.2f);
                }
            }
            // Periodic bell echoes
            if (tick == 16 || tick == 32 || tick == 48) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.6f, 1.4f + tick * 0.01f);
            }
            // FADE
            if (tick > 50 && tick % 3 == 0) {
                for (int k = 0; k < 6; k++) {
                    Location p = c.clone().add((Math.random() - 0.5) * 8, 0.3 + Math.random() * 1.5, (Math.random() - 0.5) * 8);
                    w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new AttackSequenceFinale(plugin); }
    }

    // ================================================================
    // 75. CHAIN MODE AMBIENT HEARTBEAT — "The Pulse"
    //     Combat feedback — long-duration ambient pulse, "dungeon is alive" (no damage)
    //     SPAWN  : low golem-step sound + central FALLING_DUST inward burst
    //     ACTIVE : subtle 80-tick repeating pulse (3 cycles), wall-inward particles
    //     FADE   : trailing low rumble
    // ================================================================
    public static class ChainModeAmbientHeartbeat extends EnvironmentalAttack {
        private static final int PULSE_PERIOD = 80;
        private static final double WALL_R = 14.0;

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
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.15f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.5f, 0.7f);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // 80-tick pulses — at tick=0, 80, 160, 240
            if (tick % PULSE_PERIOD == 0 && tick > 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.15f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.25f, 0.5f);
                // FALLING_DUST(IRON) from 4 walls, inward
                double[][] walls = {{-WALL_R, 0}, {WALL_R, 0}, {0, -WALL_R}, {0, WALL_R}};
                for (double[] wall : walls) {
                    for (int k = 0; k < 6; k++) {
                        double ox = wall[0] + (k - 2.5) * (wall[0] == 0 ? 1.0 : 0);
                        double oz = wall[1] + (k - 2.5) * (wall[1] == 0 ? 1.0 : 0);
                        Location p = c.clone().add(ox, 1.5 + Math.random() * 2.0, oz);
                        w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.1, 0.2, 0.1, 0, Material.IRON_BLOCK.createBlockData());
                        if (k % 2 == 0) grayDust(p, 1, 0.2);
                    }
                }
            }

            // Per-tick ambient drift — subtle dust at random arena point every 10t
            if (tick % 10 == 0) {
                double rx = (Math.random() - 0.5) * WALL_R * 1.6;
                double rz = (Math.random() - 0.5) * WALL_R * 1.6;
                Location p = c.clone().add(rx, 1.0 + Math.random() * 2.5, rz);
                w.spawnParticle(Particle.SMOKE, p, 1, 0.1, 0.1, 0.1, 0.005);
                grayDust(p, 1, 0.3);
            }
            // Pulse rise-and-fall halo at center each pulse (10t after pulse start)
            int sincePulse = tick % PULSE_PERIOD;
            if (sincePulse < 10 && tick > 0) {
                double r = sincePulse * 0.4;
                for (int k = 0; k < 10; k++) {
                    double a = Math.PI * 2 * k / 10 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 110, 110, 130, 1.0f);
                }
            }
            // FADE
            if (tick > config.getDurationTicks() - 20 && tick % 4 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 6, 0.5, (Math.random() - 0.5) * 6);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.2, 0.1, 0.2, 0.005);
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainModeAmbientHeartbeat(plugin); }
    }
}
