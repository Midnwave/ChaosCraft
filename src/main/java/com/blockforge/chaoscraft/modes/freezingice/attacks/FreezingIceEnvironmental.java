package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * FreezingIce Mode — ENVIRONMENTAL FX BATCH 1 (entries 1-10).
 * Frost Projectile theme. Pure ItemDisplay + particle attacks. NO BlockDisplays.
 *
 * Each attack adheres to the choreography contract:
 *   PHASE 1 — SPAWN        : dramatic charge/materialize with rising layered particles
 *   PHASE 2 — ACTIVE/IMPACT: high-action descent, sweep or burst with continuous animateTo motion
 *   PHASE 3 — DISSIPATE    : satisfying aftermath - lingering particles fade and ambient settle
 *
 * Three or more distinct particle types are layered every active tick.
 * Three phase-distinct sounds mark each phase transition.
 *
 * Damage / radius / IDs / configurable defaults are UNCHANGED. Class signatures UNCHANGED.
 */
public final class FreezingIceEnvironmental {
    private FreezingIceEnvironmental() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IcicleVolley(plugin));
        registry.register(new HailstoneShower(plugin));
        registry.register(new FrozenJavelinRain(plugin));
        registry.register(new SubzeroArrowSwarm(plugin));
        registry.register(new GlassShardRain(plugin));
        registry.register(new CryoMortar(plugin));
        registry.register(new PolarSpear(plugin));
        registry.register(new SleetVolley(plugin));
        registry.register(new FrostbiteDarts(plugin));
        registry.register(new CometStrike(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    // ================================================================
    // 1. ICICLE VOLLEY
    //    SPAWN  (0-30t)  : 18 PACKED_ICE icicles materialize at Y+22, scale 0→1, point-down, spinning
    //    ACTIVE          : each icicle accelerates downward, SNOWFLAKE trail, on-impact burst
    //    DISSIPATE       : lingering frost dust + falling snowflakes for 40t
    // ================================================================
    public static class IcicleVolley extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> icicles = new ArrayList<>();
        private final double[] iX = new double[18];
        private final double[] iZ = new double[18];
        private final double[] iY = new double[18];      // current Y offset above center
        private final double[] iVy = new double[18];     // accumulated falling-physics velocity
        private final double[] iSpawnY = new double[18]; // initial Y (for animateTo from-pose)
        private final int[] iStartTick = new int[18];
        private final boolean[] iMaterialized = new boolean[18];
        private final boolean[] iImpacted = new boolean[18];
        private final List<double[]> impactSites = new ArrayList<>(); // {x, z, deathTick}
        private static final double START_Y = 22.0;
        private static final int SPAWN_END = 30;
        private static final int DISSIPATE_LEN = 40;

        public IcicleVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_volley", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(50400.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(200);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50400.0);
            config.setImpactRadius(3.75);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.4f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 1.5f);

            for (int i = 0; i < 18; i++) {
                double a = Math.random() * Math.PI * 2;
                double rr = Math.random() * 3.5;
                iX[i] = Math.cos(a) * rr;
                iZ[i] = Math.sin(a) * rr;
                iSpawnY[i] = START_Y + Math.random() * 2.5;
                iY[i] = iSpawnY[i];
                iVy[i] = 0.05; // initial downward seed
                iStartTick[i] = (int)(Math.random() * 14);
                iMaterialized[i] = false;
                iImpacted[i] = false;
                Location p = c.clone().add(iX[i], iY[i], iZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                // SPAWN: start at scale 0, will animate to full 0.45 x 1.6 x 0.45 icicle shape
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 230, 255).interpolation(2, 0);
                icicles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Spawn-cloud dressing: 6 ICE shards + 4 BLUE_ICE chunks floating in the sky cluster
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.2;
                double rr = 2.0 + Math.random();
                double yy = START_Y + 2.5 + Math.random() * 2;
                Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 3.0, START_Y + 4.0, Math.sin(a) * 3.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(160, 210, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ===== SPAWN PHASE (0..30t): materialize + sky charge =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                // Materialize each icicle when its start tick fires — scale 0→1 with high spin
                for (int i = 0; i < icicles.size(); i++) {
                    if (iImpacted[i] || tick < iStartTick[i]) continue;
                    if (!iMaterialized[i]) {
                        iMaterialized[i] = true;
                    }
                    // Scale ramp during spawn: invisible → full icicle proportion
                    float sScale = (float)(0.45 * progress);
                    float ySc = (float)(1.6 * progress);
                    icicles.get(i).animateTo(
                            new Vector3f((float)iX[i] - sScale / 2, (float)iY[i], (float)iZ[i] - sScale / 2),
                            // High spin during materialize
                            new AxisAngle4f((float)(tick * 0.6 + i * 0.7), 0, 1, 0),
                            new Vector3f(sScale, ySc, sScale), 3);
                    // Sky-charge particles: dense build-up around each icicle tip
                    Location pos = c.clone().add(iX[i], iY[i] - ySc / 2, iZ[i]);
                    w.spawnParticle(Particle.SNOWFLAKE, pos, 2, 0.2, 0.2, 0.2, 0.01);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 1, 0.15, 0.15, 0.15, 0.02);
                    if ((tick + i) % 3 == 0) {
                        DisplayBuilder.dustParticles(pos, 1, 0.1, 200, 230, 255, 1.0f);
                    }
                }
                // Rising-charge column above arena
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f + (float) progress * 1.3f);
                }
                for (int y = 4; y <= 20; y += 2) {
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, y, 0), 1, 1.2, 0.2, 1.2, 0.01);
                }
                return;
            }

            // ===== ACTIVE PHASE: falling-physics descent =====
            for (int i = 0; i < icicles.size(); i++) {
                if (iImpacted[i]) continue;
                // Falling-physics acceleration: velocity grows ~0.06/tick (gravity-like)
                iVy[i] = Math.min(1.4, iVy[i] + 0.06);
                iY[i] -= iVy[i];
                Location pos = c.clone().add(iX[i], iY[i], iZ[i]);

                // Continuous motion via animateTo — point-down icicle with slight wobble around X
                icicles.get(i).animateTo(
                        new Vector3f((float)iX[i] - 0.225f, (float)iY[i], (float)iZ[i] - 0.225f),
                        new AxisAngle4f((float)(Math.sin(tick * 0.4 + i) * 0.15), 0, 0, 1),
                        new Vector3f(0.45f, 1.6f, 0.45f), 2);

                // Layered trail: SNOWFLAKE (4/tick) + ELECTRIC_SPARK + frost DUST
                w.spawnParticle(Particle.SNOWFLAKE, pos, 4, 0.08, 0.4, 0.08, 0.01);
                w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 1, 0.06, 0.2, 0.06, 0.02);
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(pos, 1, 0.05, 200, 230, 255, 1.0f);
                }

                if (iY[i] <= 0.6) {
                    Location impact = c.clone().add(iX[i], 0.4, iZ[i]);

                    // === IMPACT SOUNDS ===
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.2f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_HIT, 1.2f, 1.0f);

                    // ITEM_SNOWBALL burst (24)
                    w.spawnParticle(Particle.ITEM_SNOWBALL, impact, 24, 0.5, 0.3, 0.5, 0.25);
                    // ELECTRIC_SPARK ring at radius 2.5
                    DisplayBuilder.particleRing(impact, 2.5, Particle.ELECTRIC_SPARK, 36, null);
                    // END_ROD vertical streak from impact upward
                    for (int y = 0; y < 8; y++) {
                        w.spawnParticle(Particle.END_ROD, impact.clone().add(0, y * 0.6, 0), 1, 0.05, 0.05, 0.05, 0.02);
                    }
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 12, 0.4, 0.3, 0.4, 0.1);
                    DisplayBuilder.dustParticles(impact, 12, 0.5, 180, 230, 255, 1.4f);

                    triggerImpactDamage(impact);
                    iImpacted[i] = true;
                    impactSites.add(new double[] { iX[i], iZ[i], tick });
                }
            }

            // ===== DISSIPATE PHASE: lingering FROSTED_ICE-colored DUST + falling SNOWFLAKE for 40t =====
            // Active per impact site for DISSIPATE_LEN ticks after each impact.
            for (int idx = impactSites.size() - 1; idx >= 0; idx--) {
                double[] site = impactSites.get(idx);
                int age = tick - (int) site[2];
                if (age <= 0) continue;
                if (age > DISSIPATE_LEN) {
                    impactSites.remove(idx);
                    continue;
                }
                Location p = c.clone().add(site[0], 0.4, site[1]);
                // Layered: FROSTED_ICE-tinted DUST + falling SNOWFLAKE drift + low ELECTRIC_SPARK shimmer
                if (age % 2 == 0) {
                    DisplayBuilder.dustParticles(p, 2, 1.0, 170, 220, 255, 1.2f);
                }
                w.spawnParticle(Particle.SNOWFLAKE, p.clone().add(0, 2.5, 0), 2, 0.8, 0.5, 0.8, 0.02);
                if (age % 4 == 0) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.4, 0.05, 0.4, 0.01);
                }
                // Soft dissipate close-out chime
                if (age == DISSIPATE_LEN - 2) {
                    DisplayBuilder.playSound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.7f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IcicleVolley(plugin); }
    }

    // ================================================================
    // 2. HAILSTONE SHOWER
    //    SPAWN  (0-30t)  : 30 SNOWBALL gather at Y+18 in cloud cluster, scale 0→0.4, swirling
    //    ACTIVE          : 200t fall scatter, individual rotation, per-impact bursts
    //    DISSIPATE       : Y+18 cloud dissipates with WHITE dust, lingering ground snow
    // ================================================================
    public static class HailstoneShower extends EnvironmentalAttack {
        private static final int TOTAL_STONES = 30;
        private static final int SPAWN_END = 30;
        private static final int FALL_DURATION = 200;
        private static final int DISSIPATE_START = SPAWN_END + FALL_DURATION; // 230
        private static final int DISSIPATE_LEN = 30;
        private static final double CLOUD_Y = 18.0;

        private final List<ItemDisplayHandle> stones = new ArrayList<>();
        private final List<ItemDisplayHandle> cloudFrags = new ArrayList<>();
        private final double[] sStartX = new double[TOTAL_STONES];
        private final double[] sStartZ = new double[TOTAL_STONES];
        private final double[] sTargetX = new double[TOTAL_STONES]; // scattered XZ landing
        private final double[] sTargetZ = new double[TOTAL_STONES];
        private final double[] sX = new double[TOTAL_STONES];
        private final double[] sZ = new double[TOTAL_STONES];
        private final double[] sY = new double[TOTAL_STONES];
        private final double[] sVy = new double[TOTAL_STONES];
        private final double[] sSpin = new double[TOTAL_STONES];
        private final int[] sStartTick = new int[TOTAL_STONES];
        private final boolean[] sImpacted = new boolean[TOTAL_STONES];

        public HailstoneShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hailstone_shower", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(26400.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(260);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(26400.0);
            config.setImpactRadius(2.7);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.6f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.4f, 1.0f);

            for (int i = 0; i < TOTAL_STONES; i++) {
                // Swirling cloud cluster origin at Y+18 within r=3 cluster
                double cAng = Math.PI * 2 * i / TOTAL_STONES + Math.random() * 0.4;
                double cR = Math.random() * 3.0;
                sStartX[i] = Math.cos(cAng) * cR;
                sStartZ[i] = Math.sin(cAng) * cR;
                // Scattered ±5b landing XZ for full-arena coverage
                sTargetX[i] = sStartX[i] + (Math.random() - 0.5) * 10.0;
                sTargetZ[i] = sStartZ[i] + (Math.random() - 0.5) * 10.0;
                sX[i] = sStartX[i];
                sZ[i] = sStartZ[i];
                sY[i] = CLOUD_Y + (Math.random() - 0.5) * 1.2;
                sVy[i] = 0.0;
                sSpin[i] = (Math.random() - 0.5) * 0.8;
                // Falls stagger across 200-tick window after spawn
                sStartTick[i] = SPAWN_END + (int)(Math.random() * FALL_DURATION * 0.85);
                sImpacted[i] = false;
                Location p = c.clone().add(sX[i], sY[i], sZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                // SPAWN: start invisible, will animate to 0.4 during spawn phase
                h.scale(0.001f, 0.001f, 0.001f).glow(220, 240, 255).interpolation(2, 0);
                stones.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cloud-fragment dressing pieces that dissipate during phase 3
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 3 + Math.random() * 2;
                Location p = c.clone().add(Math.cos(a) * rr, CLOUD_Y + Math.random() * 1.0, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOW_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(240, 250, 255).interpolation(60, 0);
                cloudFrags.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.4;
                double rr = 2.5 + Math.random() * 1.5;
                Location p = c.clone().add(Math.cos(a) * rr, CLOUD_Y + 0.5 + Math.random() * 1.5, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.POWDER_SNOW_BUCKET));
                h.scale(0.001f, 0.001f, 0.001f).glow(230, 245, 255).interpolation(60, 0);
                cloudFrags.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ===== SPAWN PHASE (0..30t): swirling cloud gathers at Y+18 =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                float scale = (float)(0.4 * progress);
                for (int i = 0; i < stones.size(); i++) {
                    // Swirl: rotate the home positions slowly around center
                    double swirl = tick * 0.06;
                    double newX = sStartX[i] * Math.cos(swirl) - sStartZ[i] * Math.sin(swirl);
                    double newZ = sStartX[i] * Math.sin(swirl) + sStartZ[i] * Math.cos(swirl);
                    sX[i] = newX;
                    sZ[i] = newZ;
                    stones.get(i).animateTo(
                            new Vector3f((float)sX[i] - scale / 2, (float)sY[i], (float)sZ[i] - scale / 2),
                            new AxisAngle4f((float)(tick * 0.5 + i), 0, 1, 0),
                            new Vector3f(scale), 3);
                }
                // Cloud fragments grow in too
                float cfScale = (float)(0.35 + 0.05 * progress);
                for (int i = 0; i < cloudFrags.size(); i++) {
                    if (i % 4 != tick % 4) continue;
                    cloudFrags.get(i).animateTo(
                            new Vector3f(-cfScale / 2, 0, -cfScale / 2),
                            new AxisAngle4f((float)(tick * 0.05), 0, 1, 0),
                            new Vector3f(cfScale), 6);
                }
                // Layered cloud-cluster particles: CLOUD body + WHITE DUST swirl + SNOWFLAKE drift
                int dots = 30;
                for (int k = 0; k < dots; k++) {
                    double ang = Math.PI * 2 * k / dots + tick * 0.08;
                    double rr = 3.0 + Math.sin(tick * 0.15 + k) * 0.7;
                    Location rp = c.clone().add(Math.cos(ang) * rr, CLOUD_Y + Math.sin(tick * 0.1 + k) * 0.4, Math.sin(ang) * rr);
                    if (k % 2 == 0) w.spawnParticle(Particle.CLOUD, rp, 1, 0, 0, 0, 0);
                    if (k % 3 == 0) w.spawnParticle(Particle.SNOWFLAKE, rp, 1, 0, 0, 0, 0.01);
                    if (k % 4 == 0) DisplayBuilder.dustParticles(rp, 1, 0.1, 240, 250, 255, 1.0f);
                }
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.5f + (float) progress * 1.2f);
                }
                return;
            }

            // ===== ACTIVE PHASE: stones scatter-fall over 200t =====
            for (int i = 0; i < stones.size(); i++) {
                if (sImpacted[i] || tick < sStartTick[i]) continue;

                // Falling-physics with random scatter from start to target XZ
                sVy[i] = Math.min(1.0, sVy[i] + 0.05);
                sY[i] -= sVy[i];
                // Lerp XZ toward scattered target as stone falls
                double fallProgress = Math.min(1.0, (CLOUD_Y - sY[i]) / CLOUD_Y);
                sX[i] = sStartX[i] + (sTargetX[i] - sStartX[i]) * fallProgress;
                sZ[i] = sStartZ[i] + (sTargetZ[i] - sStartZ[i]) * fallProgress;

                Location pos = c.clone().add(sX[i], sY[i], sZ[i]);
                stones.get(i).animateTo(
                        new Vector3f((float)sX[i] - 0.2f, (float)sY[i], (float)sZ[i] - 0.2f),
                        new AxisAngle4f((float)(tick * sSpin[i] + i), 0, 1, 0),
                        new Vector3f(0.4f), 2);

                // Layered trail: ITEM_SNOWBALL streak + CLOUD wisp + occasional WHITE dust
                w.spawnParticle(Particle.ITEM_SNOWBALL, pos, 2, 0.05, 0.2, 0.05, 0.02);
                w.spawnParticle(Particle.CLOUD, pos, 1, 0.06, 0.06, 0.06, 0.01);
                if ((tick + i) % 4 == 0) {
                    DisplayBuilder.dustParticles(pos, 1, 0.05, 240, 250, 255, 1.0f);
                }

                if (sY[i] <= 0.5) {
                    Location impact = c.clone().add(sX[i], 0.3, sZ[i]);
                    float pitch = 0.7f + (float)Math.random() * 0.4f;
                    // === IMPACT SOUNDS ===
                    DisplayBuilder.playSound(impact, Sound.BLOCK_POWDER_SNOW_BREAK, 1.2f, pitch);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_HIT, 0.6f, pitch);

                    // Layered impact: ITEM_SNOWBALL streak + FALLING_DUST(WHITE_CONCRETE) burst + GLASS_HIT pop
                    w.spawnParticle(Particle.ITEM_SNOWBALL, impact, 10, 0.3, 0.2, 0.3, 0.1);
                    w.spawnParticle(Particle.FALLING_DUST, impact, 12, 0.4, 0.2, 0.4, 0.05,
                            Material.WHITE_CONCRETE.createBlockData());
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 8, 0.3, 0.3, 0.3, 0.05);
                    DisplayBuilder.dustParticles(impact, 6, 0.3, 240, 250, 255, 1.1f);

                    triggerImpactDamage(impact);
                    sImpacted[i] = true;
                }
            }

            // Active ambient: WEATHER_RAIN looped during fall
            if (tick % 24 == 0 && tick < DISSIPATE_START) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.7f, 1.6f);
            }

            // ===== DISSIPATE PHASE: cloud dissipates with WHITE dust + lingering ground snow =====
            if (tick == DISSIPATE_START) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_BREAK, 1.2f, 0.6f);
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.0f, 1.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
                // Shrink-out remaining airborne stones + cloud fragments
                for (int i = 0; i < stones.size(); i++) {
                    if (sImpacted[i]) continue;
                    stones.get(i).animateTo(
                            new Vector3f((float)sX[i] - 0.2f, (float)sY[i], (float)sZ[i] - 0.2f),
                            new AxisAngle4f((float)(tick * 0.6 + i), 0, 1, 0),
                            new Vector3f(0.001f), 20);
                }
                for (int i = 0; i < cloudFrags.size(); i++) {
                    cloudFrags.get(i).animateTo(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(0.001f), DISSIPATE_LEN);
                }
            }
            if (tick >= DISSIPATE_START) {
                int age = tick - DISSIPATE_START;
                if (age > DISSIPATE_LEN) return;
                // Dissipating cloud at Y+18: expanding sparse WHITE dust + snowflake drift
                int dots = 18;
                for (int k = 0; k < dots; k++) {
                    double ang = Math.PI * 2 * k / dots + age * 0.1;
                    double rr = 3.0 + age * 0.15;
                    Location rp = c.clone().add(Math.cos(ang) * rr, CLOUD_Y + Math.sin(age * 0.1 + k) * 0.6, Math.sin(ang) * rr);
                    if (k % 2 == 0) DisplayBuilder.dustParticles(rp, 1, 0.2, 240, 250, 255, 1.2f);
                    if (k % 3 == 0) w.spawnParticle(Particle.SNOWFLAKE, rp, 1, 0.1, 0.5, 0.1, 0.02);
                    if (k % 4 == 0) w.spawnParticle(Particle.CLOUD, rp, 1, 0.1, 0.1, 0.1, 0);
                }
                // Lingering ground snow at random arena positions
                if (age % 2 == 0) {
                    for (int j = 0; j < 6; j++) {
                        double rx = (Math.random() - 0.5) * 14;
                        double rz = (Math.random() - 0.5) * 14;
                        Location p = c.clone().add(rx, 0.3, rz);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.2, 0.05, 0.2, 0.01);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new HailstoneShower(plugin); }
    }

    // ================================================================
    // 3. FROZEN JAVELIN RAIN
    //    SPAWN (0-25t)   : 8 AMETHYST_SHARD javelins materialize at Y+18 at 45°, scale 0→1, rotating
    //    ACTIVE          : tips charge with ELECTRIC_SPARK, then thrust downward at 45°
    //    DISSIPATE       : stick at impact angle briefly, then shatter
    // ================================================================
    public static class FrozenJavelinRain extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> javelins = new ArrayList<>();
        private final double[] jX = new double[8];
        private final double[] jZ = new double[8];
        private final double[] jDx = new double[8];
        private final double[] jDz = new double[8];
        private final double[] jY = new double[8];
        private final double[] jStartX = new double[8];
        private final double[] jStartZ = new double[8];
        private final double[] jStartY = new double[8];
        private final double[] jYaw = new double[8];
        private final int[] jStartTick = new int[8];
        private final boolean[] jMaterialized = new boolean[8];
        private final boolean[] jImpacted = new boolean[8];
        private final int[] jShatterTick = new int[8];      // tick to shatter (after stick)
        private final double[] jImpactX = new double[8];
        private final double[] jImpactZ = new double[8];
        private static final double START_Y = 18.0;
        private static final int SPAWN_END = 25;
        private static final int CHARGE_TICKS = 12;          // post-spawn tip-charge before thrust
        private static final double V = 0.7;

        public FrozenJavelinRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_javelin_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(60000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60000.0);
            config.setImpactRadius(4.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.9f);

            for (int i = 0; i < 8; i++) {
                jYaw[i] = Math.PI * 2 * i / 8 + Math.random() * 0.3;
                jDx[i] = -Math.cos(jYaw[i]); // moves inward at 45°
                jDz[i] = -Math.sin(jYaw[i]);
                jStartX[i] = Math.cos(jYaw[i]) * 7.0;
                jStartZ[i] = Math.sin(jYaw[i]) * 7.0;
                jStartY[i] = START_Y;
                jX[i] = jStartX[i];
                jZ[i] = jStartZ[i];
                jY[i] = jStartY[i];
                jStartTick[i] = i * 3;
                jMaterialized[i] = false;
                jImpacted[i] = false;
                Location p = c.clone().add(jX[i], jY[i], jZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                // SPAWN: start invisible, will materialize at full elongated shape
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 220, 255).interpolation(2, 0);
                javelins.add(h);
                spawnedEntities.add(h.entity());
            }

            // Frost decoration
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 8.0, START_Y + 1.5, Math.sin(a) * 8.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_CLUSTER));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 200, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 8;
                Location p = c.clone().add(Math.cos(a) * 6.0, START_Y + 3, Math.sin(a) * 6.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PRISMARINE_CRYSTALS));
                h.scale(0.55f, 0.55f, 0.55f).glow(150, 220, 240).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < javelins.size(); i++) {
                if (jImpacted[i] || tick < jStartTick[i]) continue;
                int localTick = tick - jStartTick[i];

                // ===== SPAWN PHASE (0..25t local): materialize scale 0→1, spin to point-down at 45° =====
                if (localTick < SPAWN_END) {
                    double progress = localTick / (double) SPAWN_END;
                    float scaleXY = (float)(0.3 * progress);
                    float scaleZ = (float)(2.2 * progress);
                    float yaw = (float) jYaw[i];
                    // Spawn-spin: extra Z-axis spin layered with the final yaw orientation
                    float spin = (float)(localTick * 0.5);
                    javelins.get(i).animateTo(
                            new Vector3f((float)jX[i] - scaleXY / 2, (float)jY[i], (float)jZ[i] - scaleXY / 2),
                            new AxisAngle4f(yaw + spin, 0, 1, 0),
                            new Vector3f(scaleXY, scaleXY, scaleZ), 2);
                    jMaterialized[i] = true;
                    continue;
                }

                // ===== CHARGE PHASE (25..37t local): tip charges with ELECTRIC_SPARK build-up =====
                if (localTick < SPAWN_END + CHARGE_TICKS) {
                    Location tipPos = c.clone().add(jX[i] + jDx[i] * 1.0, jY[i] - 1.0, jZ[i] + jDz[i] * 1.0);
                    int density = 6 + (localTick - SPAWN_END);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, tipPos, density, 0.3, 0.3, 0.3, 0.05);
                    w.spawnParticle(Particle.END_ROD, tipPos, 1, 0.2, 0.2, 0.2, 0.01);
                    DisplayBuilder.dustParticles(tipPos, 1, 0.2, 200, 180, 255, 1.1f);
                    if (localTick == SPAWN_END) {
                        DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
                    }
                    if (localTick == SPAWN_END + CHARGE_TICKS - 1) {
                        DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.4f, 0.7f);
                    }
                    continue;
                }

                // ===== ACTIVE THRUST: descend at 45° toward target =====
                jY[i] -= V;
                jX[i] += jDx[i] * V;
                jZ[i] += jDz[i] * V;
                Location pos = c.clone().add(jX[i], jY[i], jZ[i]);

                float yaw = (float) jYaw[i];
                javelins.get(i).animateTo(
                        new Vector3f((float)jX[i] - 0.15f, (float)jY[i], (float)jZ[i] - 0.15f),
                        new AxisAngle4f(yaw, 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 2.2f), 2);

                // Mid-flight layered trail
                w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 3, 0.1, 0.1, 0.1, 0.05);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 2, 0.1, 0.1, 0.1, 0.02);
                DisplayBuilder.dustParticles(pos, 1, 0.1, 200, 180, 255, 1.0f);

                if (jY[i] <= 0.5) {
                    Location impact = c.clone().add(jX[i], 0.4, jZ[i]);
                    jImpactX[i] = jX[i];
                    jImpactZ[i] = jZ[i];

                    // === IMPACT SOUNDS ===
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_HIT, 1.4f, 0.8f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.6f, 0.7f);

                    // Layered impact: END_ROD (16) + violet DUST shimmer + SNOWFLAKE
                    w.spawnParticle(Particle.END_ROD, impact, 16, 0.5, 0.3, 0.5, 0.3);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 20, 0.6, 0.4, 0.6, 0.5);
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 12, 0.5, 0.3, 0.5, 0.1);
                    DisplayBuilder.dustParticles(impact, 16, 0.6, 200, 150, 255, 1.5f);

                    // Stick at impact angle briefly (8t), then shatter
                    javelins.get(i).animateTo(
                            new Vector3f((float)jX[i] - 0.15f, 0.4f, (float)jZ[i] - 0.15f),
                            new AxisAngle4f(yaw, 0, 1, 0),
                            new Vector3f(0.3f, 0.3f, 2.2f), 1);

                    triggerImpactDamage(impact);
                    jImpacted[i] = true;
                    jShatterTick[i] = tick + 12;
                }
            }

            // ===== DISSIPATE PHASE: stuck javelins shatter after 12t =====
            for (int i = 0; i < javelins.size(); i++) {
                if (!jImpacted[i]) continue;
                if (tick == jShatterTick[i]) {
                    Location pos = c.clone().add(jImpactX[i], 0.4, jImpactZ[i]);
                    DisplayBuilder.playSound(pos, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.2f, 1.1f);
                    // Shatter: spin-shrink + violet dust burst + lingering END_ROD
                    javelins.get(i).animateTo(
                            new Vector3f((float)jImpactX[i] - 0.15f, 0.4f, (float)jImpactZ[i] - 0.15f),
                            new AxisAngle4f((float)(tick * 0.8 + i), 1, 0.5f, 0),
                            new Vector3f(0.001f), 14);
                    w.spawnParticle(Particle.END_ROD, pos, 12, 0.4, 0.4, 0.4, 0.15);
                    DisplayBuilder.dustParticles(pos, 12, 0.5, 200, 150, 255, 1.4f);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 8, 0.3, 0.3, 0.3, 0.1);
                }
                // Linger dust trickle for 12t after shatter
                if (tick > jShatterTick[i] && tick <= jShatterTick[i] + 14 && (tick + i) % 3 == 0) {
                    Location pos = c.clone().add(jImpactX[i], 0.6, jImpactZ[i]);
                    DisplayBuilder.dustParticles(pos, 1, 0.3, 200, 150, 255, 1.0f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrozenJavelinRain(plugin); }
    }

    // ================================================================
    // 4. SUBZERO ARROW SWARM
    //    SPAWN (0-40t)   : 24 SNOWBALL items materialize at r=12 ring, scale 0→0.25, elongated x-axis (arrow-like)
    //    ACTIVE          : streak inward each on own arc over 80t, CLOUD trail, SNOWFLAKE puffs at center
    //    DISSIPATE (40t) : final convergent burst at center — large SNOWFLAKE explosion + GLASS_BREAK
    // ================================================================
    public static class SubzeroArrowSwarm extends EnvironmentalAttack {
        private static final int SPAWN_END = 40;
        private static final int ACTIVE_LEN = 80;
        private static final int DISSIPATE_START = SPAWN_END + ACTIVE_LEN; // 120
        private static final int DURATION = DISSIPATE_START + 40;          // 160
        private final List<ItemDisplayHandle> arrows = new ArrayList<>();
        private final double[] aStartX = new double[24];
        private final double[] aStartZ = new double[24];
        private final double[] aTargetX = new double[24]; // random center XZ (not all 0)
        private final double[] aTargetZ = new double[24];
        private final double[] aY = new double[24];
        private final double[] aAng = new double[24];     // for arrow orientation
        private final int[] aStartTick = new int[24];
        private final boolean[] aDone = new boolean[24];

        public SubzeroArrowSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subzero_arrow_swarm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(28800.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(DURATION);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            DisplayBuilder.playSound(c, Sound.ENTITY_ARROW_SHOOT, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);

            for (int i = 0; i < 24; i++) {
                aAng[i] = Math.PI * 2 * i / 24 + Math.random() * 0.1;
                aStartX[i] = Math.cos(aAng[i]) * 12.0;
                aStartZ[i] = Math.sin(aAng[i]) * 12.0;
                // Random center XZ target ±1.5b for own-arc variation
                aTargetX[i] = (Math.random() - 0.5) * 3.0;
                aTargetZ[i] = (Math.random() - 0.5) * 3.0;
                aY[i] = 1.0 + (i % 4) * 0.4;
                aStartTick[i] = SPAWN_END + (i % 8) * 2;
                aDone[i] = false;
                Location p = c.clone().add(aStartX[i], aY[i], aStartZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                // SPAWN: start invisible, will animate to 0.25 elongated-x
                h.scale(0.001f, 0.001f, 0.001f).glow(210, 235, 255).interpolation(2, 0);
                arrows.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring backdrop
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 13.0, 1.5, Math.sin(a) * 13.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ARROW));
                h.scale(0.45f, 0.45f, 0.45f).glow(200, 220, 240).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.3;
                Location p = c.clone().add(Math.cos(a) * 11.5, 2.5, Math.sin(a) * 11.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SPECTRAL_ARROW));
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 220, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ===== SPAWN PHASE (0..40t): arrows materialize at perimeter =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                float xLen = (float)(0.7 * progress);  // elongated along arrow's travel axis
                float xyShort = (float)(0.25 * progress);
                for (int i = 0; i < arrows.size(); i++) {
                    arrows.get(i).animateTo(
                            new Vector3f((float)aStartX[i] - xyShort / 2, (float)aY[i], (float)aStartZ[i] - xyShort / 2),
                            new AxisAngle4f((float) aAng[i], 0, 1, 0),
                            // Elongate the Z-axis so it looks arrow-like (long forward axis)
                            new Vector3f(xyShort, xyShort, xLen), 3);
                    // Spawn shimmer at each arrow
                    if ((tick + i) % 4 == 0) {
                        Location p = c.clone().add(aStartX[i], aY[i], aStartZ[i]);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.02);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                // Perimeter ring shimmer build-up
                int dots = 36;
                for (int k = 0; k < dots; k++) {
                    double ang = Math.PI * 2 * k / dots + tick * 0.04;
                    Location rp = c.clone().add(Math.cos(ang) * 12.0, 1.5, Math.sin(ang) * 12.0);
                    if (k % 4 == 0) DisplayBuilder.dustParticles(rp, 1, 0.1, 210, 235, 255, 1.0f);
                }
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
                }
                return;
            }

            // ===== ACTIVE PHASE (40..120t): arrows streak inward on own arcs =====
            if (tick == SPAWN_END) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ARROW_SHOOT, 1.6f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.4f, 1.3f);
            }
            for (int i = 0; i < arrows.size(); i++) {
                if (aDone[i] || tick < aStartTick[i]) continue;
                int active = tick - aStartTick[i];
                // 80-tick arc from start to target (own arc per arrow)
                double t = Math.min(1.0, active / 80.0);
                double ease = t * t * (3 - 2 * t);
                double cx = aStartX[i] + (aTargetX[i] - aStartX[i]) * ease;
                double cz = aStartZ[i] + (aTargetZ[i] - aStartZ[i]) * ease;
                // Slight arc Y bow
                double cy = aY[i] + Math.sin(t * Math.PI) * 0.6;

                arrows.get(i).animateTo(
                        new Vector3f((float)cx - 0.125f, (float)cy, (float)cz - 0.125f),
                        new AxisAngle4f((float) aAng[i], 0, 1, 0),
                        new Vector3f(0.25f, 0.25f, 0.7f), 2);

                Location pos = c.clone().add(cx, cy, cz);
                // CLOUD trail per arrow (1/tick) + ELECTRIC_SPARK + SNOWFLAKE wisps
                w.spawnParticle(Particle.CLOUD, pos, 1, 0.05, 0.05, 0.05, 0.01);
                w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 1, 0.05, 0.05, 0.05, 0.04);
                if ((active + i) % 3 == 0) {
                    w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.05, 0.05, 0.05, 0.02);
                }

                if (t >= 1.0) aDone[i] = true;
            }

            // At convergence point: 6 SNOWFLAKE puff every 5t
            if (tick % 5 == 0 && tick < DISSIPATE_START) {
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 6, 0.4, 0.4, 0.4, 0.05);
            }

            // Active ambient: BLOCK_NOTE_BLOCK_CHIME ambient per 8t
            if (tick % 8 == 0 && tick > SPAWN_END && tick < DISSIPATE_START) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 2.0f);
            }

            // ===== DISSIPATE PHASE (120..160t): final convergent burst =====
            if (tick == DISSIPATE_START) {
                Location burst = c.clone().add(0, 1.5, 0);
                DisplayBuilder.playSound(burst, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.1f);
                DisplayBuilder.playSound(burst, Sound.ENTITY_GENERIC_EXPLODE, 1.1f, 1.4f);
                DisplayBuilder.playSound(burst, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.9f);
                // Large SNOWFLAKE explosion (40) + END_ROD + CLOUD
                w.spawnParticle(Particle.SNOWFLAKE, burst, 40, 2.0, 1.2, 2.0, 0.25);
                w.spawnParticle(Particle.END_ROD, burst, 24, 1.5, 1.0, 1.5, 0.25);
                w.spawnParticle(Particle.CLOUD, burst, 24, 1.5, 0.8, 1.5, 0.1);
                w.spawnParticle(Particle.FLASH, burst, 1, 0, 0, 0, 0);
                // Shrink-out all arrows
                for (int i = 0; i < arrows.size(); i++) {
                    double cx = aTargetX[i];
                    double cz = aTargetZ[i];
                    arrows.get(i).animateTo(
                            new Vector3f((float)cx - 0.125f, (float)aY[i], (float)cz - 0.125f),
                            new AxisAngle4f((float)(tick * 0.6 + i), 0, 1, 0),
                            new Vector3f(0.001f), 20);
                }
            }
            if (tick >= DISSIPATE_START) {
                int age = tick - DISSIPATE_START;
                if (age > 40) return;
                // Lingering dust + snowflake fade at convergence
                if (age % 2 == 0) {
                    Location p = c.clone().add(0, 1.5, 0);
                    DisplayBuilder.dustParticles(p, 2, 1.0, 210, 235, 255, 1.1f);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 2, 0.8, 0.5, 0.8, 0.03);
                }
            }

            // Constant damage in center radius 9
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SubzeroArrowSwarm(plugin); }
    }

    // ================================================================
    // 5. GLASS SHARD RAIN
    //    SPAWN (0-30t)   : 40 GLASS_BOTTLE items materialize at Y+12 in 14b square grid, scale 0→0.4, slow drift
    //    ACTIVE (180t)   : shards drift downward slowly with shimmer rotation, GLOW trail, END_ROD on landing
    //    DISSIPATE (40t) : remaining shards shatter mid-air cascade + scale shrink
    // ================================================================
    public static class GlassShardRain extends EnvironmentalAttack {
        private static final int SHARDS = 40;
        private static final int SPAWN_END = 30;
        private static final int ACTIVE_LEN = 180;
        private static final int DISSIPATE_START = SPAWN_END + ACTIVE_LEN; // 210
        private static final int DURATION = DISSIPATE_START + 40;          // 250
        private static final double START_Y = 12.0;
        private static final double GRID_HALF = 7.0; // 14b square (-7..7)

        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] gX = new double[SHARDS];
        private final double[] gZ = new double[SHARDS];
        private final double[] gY = new double[SHARDS];
        private final double[] gFallSpeed = new double[SHARDS];
        private final double[] gDriftX = new double[SHARDS];
        private final double[] gDriftZ = new double[SHARDS];
        private final boolean[] gLanded = new boolean[SHARDS];

        public GlassShardRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glass_shard_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(33600.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(DURATION);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 1.3f);

            for (int i = 0; i < SHARDS; i++) {
                // Square grid layout — 40 positions across -7..7
                gX[i] = (Math.random() - 0.5) * (GRID_HALF * 2);
                gZ[i] = (Math.random() - 0.5) * (GRID_HALF * 2);
                gY[i] = START_Y + (Math.random() - 0.5) * 1.5;
                gFallSpeed[i] = 0.06 + Math.random() * 0.04; // slow drift
                gDriftX[i] = (Math.random() - 0.5) * 0.04;
                gDriftZ[i] = (Math.random() - 0.5) * 0.04;
                gLanded[i] = false;
                Location p = c.clone().add(gX[i], gY[i], gZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                // SPAWN: start invisible, animate to 0.4 with slow drift
                h.scale(0.001f, 0.001f, 0.001f).glow(200, 230, 255).interpolation(2, 0);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cloud-level backdrop
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 4 + Math.random() * 3;
                Location p = c.clone().add(Math.cos(a) * rr, 10 + Math.random() * 2, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_PANE));
                h.scale(0.45f, 0.45f, 0.05f).glow(220, 240, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.3;
                Location p = c.clone().add(Math.cos(a) * 5.5, 11 + Math.random() * 2, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 240, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ===== SPAWN PHASE (0..30t): scale 0→0.4 ramp with slow drift =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                float scale = (float)(0.4 * progress);
                for (int i = 0; i < shards.size(); i++) {
                    shards.get(i).animateTo(
                            new Vector3f((float)gX[i] - scale / 2, (float)gY[i], (float)gZ[i] - scale / 2),
                            new AxisAngle4f((float)(tick * 0.2 + i * 0.3), 0, 1, 0),
                            new Vector3f(scale), 3);
                }
                // Layered cloud-grid particles
                if (tick % 2 == 0) {
                    int dots = 24;
                    for (int k = 0; k < dots; k++) {
                        double ang = Math.PI * 2 * k / dots + tick * 0.05;
                        double rr = 5 + Math.sin(tick * 0.1 + k) * 1;
                        Location rp = c.clone().add(Math.cos(ang) * rr, START_Y, Math.sin(ang) * rr);
                        if (k % 3 == 0) w.spawnParticle(Particle.END_ROD, rp, 1, 0, 0, 0, 0);
                        if (k % 4 == 0) w.spawnParticle(Particle.SNOWFLAKE, rp, 1, 0, 0, 0, 0.01);
                        if (k % 5 == 0) DisplayBuilder.dustParticles(rp, 1, 0.1, 220, 240, 255, 1.0f);
                    }
                }
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.5f + (float) progress * 1.0f);
                }
                // Random tinkle sounds during spawn phase too
                if (Math.random() < 0.15) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.4f + (float) Math.random() * 0.4f);
                }
                return;
            }

            // ===== ACTIVE PHASE: shards drift downward slowly =====
            if (tick == SPAWN_END) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.8f, 1.6f);
            }
            for (int i = 0; i < shards.size(); i++) {
                if (gLanded[i]) continue;
                gY[i] -= gFallSpeed[i];
                gX[i] += gDriftX[i];
                gZ[i] += gDriftZ[i];
                Location pos = c.clone().add(gX[i], gY[i], gZ[i]);

                // Shimmer rotation continuous
                shards.get(i).animateTo(
                        new Vector3f((float)gX[i] - 0.2f, (float)gY[i], (float)gZ[i] - 0.2f),
                        new AxisAngle4f((float)(tick * 0.25 + i * 0.4), 1, 1, 0),
                        new Vector3f(0.4f), 2);

                // Layered trail: GLOW per shard (1/tick) + occasional SNOWFLAKE + FALLING_DUST(GLASS)
                w.spawnParticle(Particle.GLOW, pos, 1, 0.05, 0.05, 0.05, 0.01);
                if ((tick + i) % 3 == 0) {
                    w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.05, 0.05, 0.05, 0.02);
                }
                if ((tick + i) % 5 == 0) {
                    w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.05, 0.05, 0.05, 0,
                            Material.GLASS.createBlockData());
                }

                if (gY[i] <= 0.4) {
                    Location impact = c.clone().add(gX[i], 0.3, gZ[i]);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_HIT, 0.8f,
                            1.1f + (float) Math.random() * 0.4f);
                    // END_ROD on each ground touch + GLASS shards + SNOWFLAKE
                    w.spawnParticle(Particle.END_ROD, impact, 10, 0.3, 0.2, 0.3, 0.1);
                    w.spawnParticle(Particle.FALLING_DUST, impact, 8, 0.3, 0.1, 0.3, 0.05,
                            Material.GLASS.createBlockData());
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 6, 0.3, 0.2, 0.3, 0.05);
                    DisplayBuilder.dustParticles(impact, 4, 0.2, 220, 240, 255, 1.0f);
                    shards.get(i).animateTo(
                            new Vector3f((float)gX[i] - 0.2f, 0.3f, (float)gZ[i] - 0.2f),
                            new AxisAngle4f((float)(tick * 0.4), 0, 1, 0),
                            new Vector3f(0.001f), 8);
                    gLanded[i] = true;
                }
            }

            // Active ambient: random tinkles every 4-8t at random positions
            if (tick % (4 + (tick / 7) % 5) == 0 && tick < DISSIPATE_START) {
                Location rp = c.clone().add((Math.random() - 0.5) * 12, 5 + Math.random() * 6, (Math.random() - 0.5) * 12);
                DisplayBuilder.playSound(rp, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.2f + (float) Math.random() * 0.6f);
            }

            // ===== DISSIPATE PHASE (210..250t): remaining shards shatter mid-air + scale shrink =====
            if (tick == DISSIPATE_START) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.7f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 1.0f);
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.5f);
                for (int i = 0; i < shards.size(); i++) {
                    if (gLanded[i]) continue;
                    // Cascade-shatter — random delay per shard
                    int delay = (int)(Math.random() * 30);
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (gLanded[idx]) return;
                        Location p = getCenter();
                        if (p == null || p.getWorld() == null) return;
                        Location pos = p.clone().add(gX[idx], gY[idx], gZ[idx]);
                        DisplayBuilder.playSound(pos, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.9f + (float) Math.random() * 0.5f);
                        p.getWorld().spawnParticle(Particle.FALLING_DUST, pos, 8, 0.3, 0.3, 0.3, 0,
                                Material.GLASS.createBlockData());
                        p.getWorld().spawnParticle(Particle.SNOWFLAKE, pos, 6, 0.3, 0.3, 0.3, 0.05);
                        p.getWorld().spawnParticle(Particle.END_ROD, pos, 4, 0.2, 0.2, 0.2, 0.05);
                        DisplayBuilder.dustParticles(pos, 4, 0.3, 220, 240, 255, 1.1f);
                        shards.get(idx).animateTo(
                                new Vector3f((float)gX[idx] - 0.2f, (float)gY[idx], (float)gZ[idx] - 0.2f),
                                new AxisAngle4f((float) Math.random() * 6, 1, 1, 0),
                                new Vector3f(0.001f), 12);
                        gLanded[idx] = true;
                    }, delay);
                }
            }

            // Constant damage radius 12
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GlassShardRain(plugin); }
    }

    // ================================================================
    // 6. CRYO MORTAR
    //    SPAWN (0-40t)   : 1 NETHER_STAR mortar charges at arena edge, scale 0→1.4, dense SNOWFLAKE trail,
    //                      SOUL_FIRE_FLAME core, 6 BLUE_ICE casing-shrapnel orbits r=1.5
    //    ACTIVE          : lobs in parabolic arc toward center, shrapnel maintains orbit during flight
    //    DISSIPATE       : massive impact + smoke column rises, AMBIENT_CAVE
    // ================================================================
    public static class CryoMortar extends EnvironmentalAttack {
        private ItemDisplayHandle mortar;
        private final List<ItemDisplayHandle> shrapnel = new ArrayList<>();
        private double startX, startZ;
        private boolean impacted = false;
        private int impactTick = -1;
        private static final double APEX_Y = 16.0;
        private static final int SPAWN_END = 40;
        private static final int FLIGHT_TICKS = 60;
        private static final int DISSIPATE_LEN = 40;

        public CryoMortar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_mortar", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(69600.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(140);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(69600.0);
            config.setImpactRadius(13.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);

            double startAng = Math.random() * Math.PI * 2;
            startX = Math.cos(startAng) * 14.0;
            startZ = Math.sin(startAng) * 14.0;
            Location p = c.clone().add(startX, 1.0, startZ);
            mortar = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_STAR));
            // SPAWN: start invisible, animate to 1.4 during spawn phase
            mortar.scale(0.001f, 0.001f, 0.001f).glow(180, 230, 255).interpolation(2, 0);
            spawnedEntities.add(mortar.entity());

            // 6 BLUE_ICE casing-shrapnel orbiting at r=1.5
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location pp = c.clone().add(startX + Math.cos(a) * 1.5, 1.0, startZ + Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.BLUE_ICE));
                h.scale(0.001f, 0.001f, 0.001f).glow(160, 220, 255).interpolation(2, 0);
                shrapnel.add(h);
                spawnedEntities.add(h.entity());
            }

            // Frosty trail dressing
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 2.5 + Math.random() * 0.8;
                Location pp = c.clone().add(startX + Math.cos(a) * rr, 0.5 + Math.random() * 0.5, startZ + Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.SNOWBALL));
                h.scale(0.4f, 0.4f, 0.4f).glow(220, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (mortar == null) return;

            // ===== SPAWN PHASE (0..40t): mortar charges at edge, scale 0→1.4 with dense trail =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                float scale = (float)(1.4 * progress);
                mortar.animateTo(
                        new Vector3f((float)startX - scale / 2, 1.0f, (float)startZ - scale / 2),
                        new AxisAngle4f((float)(tick * 0.4), 0, 1, 0),
                        new Vector3f(scale), 3);
                // Shrapnel orbit grows in
                float orbitScale = (float)(0.5 * progress);
                for (int i = 0; i < shrapnel.size(); i++) {
                    double a = Math.PI * 2 * i / shrapnel.size() + tick * 0.15;
                    double sx = startX + Math.cos(a) * 1.5;
                    double sz = startZ + Math.sin(a) * 1.5;
                    double sy = 1.0 + Math.sin(tick * 0.2 + i) * 0.4;
                    shrapnel.get(i).animateTo(
                            new Vector3f((float)sx - orbitScale / 2, (float)sy, (float)sz - orbitScale / 2),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(orbitScale), 2);
                }
                // Layered charge particles: SNOWFLAKE dense (10/tick) + SOUL_FIRE_FLAME core flicker + ELECTRIC_SPARK
                Location pos = c.clone().add(startX, 1.0, startZ);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 10, 0.6, 0.4, 0.6, 0.03);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 3, 0.3, 0.3, 0.3, 0.02);
                w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 4, 0.4, 0.3, 0.4, 0.04);
                DisplayBuilder.dustParticles(pos, 2, 0.4, 180, 230, 255, 1.3f);
                if (tick % 6 == 0) {
                    DisplayBuilder.playSound(pos, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 0.5f + (float) progress * 0.4f);
                }
                return;
            }

            // ===== ACTIVE PHASE: parabolic flight via animateTo + shrapnel orbit during flight =====
            if (!impacted) {
                int flightTick = tick - SPAWN_END;
                double t = Math.min(1.0, flightTick / (double) FLIGHT_TICKS);
                double x = startX * (1.0 - t);
                double z = startZ * (1.0 - t);
                double y = 1.0 + 4 * APEX_Y * t * (1.0 - t);
                Location pos = c.clone().add(x, y, z);

                mortar.animateTo(
                        new Vector3f((float)x - 0.7f, (float)y, (float)z - 0.7f),
                        new AxisAngle4f((float)(tick * 0.25), 0, 1, 0),
                        new Vector3f(1.4f), 2);

                // Shrapnel maintains orbit during flight
                for (int i = 0; i < shrapnel.size(); i++) {
                    double a = Math.PI * 2 * i / shrapnel.size() + tick * 0.3;
                    double sx = x + Math.cos(a) * 1.5;
                    double sz = z + Math.sin(a) * 1.5;
                    double sy = y + Math.sin(tick * 0.3 + i) * 0.5;
                    shrapnel.get(i).animateTo(
                            new Vector3f((float)sx - 0.25f, (float)sy, (float)sz - 0.25f),
                            new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                            new Vector3f(0.5f), 2);
                }

                // Layered flight trail
                w.spawnParticle(Particle.SNOWFLAKE, pos, 6, 0.25, 0.25, 0.25, 0.04);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 4, 0.12, 0.12, 0.12, 0.02);
                w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 2, 0.15, 0.15, 0.15, 0.04);
                DisplayBuilder.dustParticles(pos, 2, 0.15, 180, 230, 255, 1.3f);

                if (tick % 10 == 0) {
                    DisplayBuilder.playSound(pos, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.7f, 0.7f);
                }

                if (t >= 1.0) {
                    Location impact = c.clone().add(0, 0.4, 0);
                    impacted = true;
                    impactTick = tick;

                    // === IMPACT SOUNDS ===
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.6f);
                    DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 0.5f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.7f);

                    // Layered impact: ITEM_SNOWBALL (60) + END_ROD burst (30) + ELECTRIC_SPARK ring r=9
                    w.spawnParticle(Particle.ITEM_SNOWBALL, impact, 60, 1.2, 0.8, 1.2, 0.6);
                    w.spawnParticle(Particle.END_ROD, impact, 30, 1.0, 0.5, 1.0, 0.3);
                    DisplayBuilder.particleRing(impact, 9.0, Particle.ELECTRIC_SPARK, 60, null);
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 40, 1.0, 0.5, 1.0, 0.2);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, impact, 20, 0.8, 0.4, 0.8, 0.15);
                    DisplayBuilder.dustParticles(impact, 40, 1.5, 180, 230, 255, 1.8f);

                    // Shrapnel scatters outward
                    for (int i = 0; i < shrapnel.size(); i++) {
                        double a = Math.PI * 2 * i / shrapnel.size();
                        double sx = Math.cos(a) * 6.0;
                        double sz = Math.sin(a) * 6.0;
                        shrapnel.get(i).animateTo(
                                new Vector3f((float)sx - 0.25f, 0.3f, (float)sz - 0.25f),
                                new AxisAngle4f((float)(tick * 0.8 + i), 0, 1, 0),
                                new Vector3f(0.001f), 20);
                    }

                    triggerImpactDamage(impact);
                }
                return;
            }

            // ===== DISSIPATE PHASE: smoke column rises + AMBIENT_CAVE =====
            if (impacted) {
                int age = tick - impactTick;
                if (age > DISSIPATE_LEN) return;
                Location impact = c.clone().add(0, 0.4, 0);
                // Rising smoke column
                int height = Math.min(12, age / 2);
                for (int y = 0; y <= height; y++) {
                    double r = 0.6 + y * 0.15;
                    int dots = 8;
                    for (int k = 0; k < dots; k++) {
                        double ang = Math.PI * 2 * k / dots + age * 0.1 + y * 0.3;
                        Location rp = impact.clone().add(Math.cos(ang) * r, y * 0.6, Math.sin(ang) * r);
                        if (k % 2 == 0) DisplayBuilder.dustParticles(rp, 1, 0.1, 180, 220, 255, 1.0f);
                        if (k % 3 == 0) w.spawnParticle(Particle.CLOUD, rp, 1, 0.05, 0.05, 0.05, 0.01);
                        if (k % 4 == 0) w.spawnParticle(Particle.SNOWFLAKE, rp, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }
                if (age == 4 || age == 20 || age == 36) {
                    DisplayBuilder.playSound(impact, Sound.AMBIENT_CAVE, 1.2f, 0.5f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CryoMortar(plugin); }
    }

    // ================================================================
    // 7. POLAR SPEAR
    //    SPAWN (0-40t)   : 1 huge ICE spear scale 0→1, hovers at Y+15. 4 BLUE_ICE rings + ELECTRIC_SPARK 8/tick build-up
    //    ACTIVE          : 40t charge with rising chime 0.5→1.8, then fast descent Y+15→0 over 8t
    //    DISSIPATE       : massive impact + spear shatters into 4 sub-pieces flying outward
    // ================================================================
    public static class PolarSpear extends EnvironmentalAttack {
        private ItemDisplayHandle spear;
        private final List<ItemDisplayHandle> energyRings = new ArrayList<>();
        private ItemDisplayHandle[] subPieces;
        private double sY = 15.0;
        private boolean descending = false;
        private boolean impacted = false;
        private int impactTick = -1;
        private static final int SPAWN_END = 40;        // materialize + ring formation
        private static final int CHARGE_TICKS = 40;     // hover charge
        private static final int DISSIPATE_LEN = 40;

        public PolarSpear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("polar_spear", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(66000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(160);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(66000.0);
            config.setImpactRadius(7.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);

            Location p = c.clone().add(0, sY, 0);
            spear = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
            // SPAWN: start invisible, will materialize to spear shape
            spear.scale(0.001f, 0.001f, 0.001f).glow(180, 230, 255).interpolation(4, 0);
            spawnedEntities.add(spear.entity());

            // 4 BLUE_ICE energy rings around spear gather energy
            for (int i = 0; i < 4; i++) {
                double y = sY - 2.0 + i * 1.3;
                Location pp = c.clone().add(0, y, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.BLUE_ICE));
                h.scale(0.001f, 0.001f, 0.001f).glow(160, 220, 255).interpolation(2, 0);
                energyRings.add(h);
                spawnedEntities.add(h.entity());
            }

            // Sigil decoration
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location pp = c.clone().add(Math.cos(a) * 2.5, sY + Math.sin(a) * 0.6, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.PACKED_ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (spear == null) return;

            // ===== SPAWN PHASE (0..40t): materialize spear + energy rings build up =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                float spearXY = (float)(0.6 * progress);
                float spearZ = (float)(3.5 * progress);
                // Rotate so long axis points down
                float pitch = (float)(progress * Math.PI / 2);
                spear.animateTo(
                        new Vector3f(-spearXY / 2, (float)sY, -spearZ / 2),
                        new AxisAngle4f(pitch, 1, 0, 0),
                        new Vector3f(spearXY, spearXY, spearZ), 4);
                // Rings ramp in radius/scale
                for (int i = 0; i < energyRings.size(); i++) {
                    double ringY = sY - 2.0 + i * 1.3;
                    double rad = 1.5 + i * 0.4;
                    int rings = 8;
                    for (int k = 0; k < rings; k++) {
                        double ang = Math.PI * 2 * k / rings + tick * 0.1 + i;
                        Location rp = c.clone().add(Math.cos(ang) * rad * progress, ringY, Math.sin(ang) * rad * progress);
                        if (k % 2 == 0) w.spawnParticle(Particle.END_ROD, rp, 1, 0, 0, 0, 0);
                    }
                    float rScale = (float)(0.55 * progress);
                    energyRings.get(i).animateTo(
                            new Vector3f(-rScale / 2, (float) ringY, -rScale / 2),
                            new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                            new Vector3f(rScale), 2);
                }
                // ELECTRIC_SPARK 8/tick building up around spear
                Location pos = c.clone().add(0, sY, 0);
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 0.8 + Math.random() * 0.6;
                    Location sp = pos.clone().add(Math.cos(a) * r, (Math.random() - 0.5) * 2.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, sp, 1, 0.05, 0.05, 0.05, 0.05);
                }
                DisplayBuilder.dustParticles(pos, 2, 0.4, 180, 230, 255, 1.2f);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 3, 0.5, 0.5, 0.5, 0.02);
                return;
            }

            // ===== ACTIVE PHASE: 40t charge with rising chime, then fast descent =====
            if (!descending) {
                int chargeTick = tick - SPAWN_END;
                if (chargeTick < CHARGE_TICKS) {
                    double phase = chargeTick / (double) CHARGE_TICKS;
                    // Rising BLOCK_AMETHYST_BLOCK_CHIME 0.5→1.8 ladder
                    if (chargeTick % 4 == 0) {
                        float chime = 0.5f + (float) phase * 1.3f;
                        DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, chime);
                    }
                    // Keep spear oriented + intensifying spark build-up
                    spear.animateTo(
                            new Vector3f(-0.3f, (float)sY, -1.75f),
                            new AxisAngle4f((float)(Math.PI / 2), 1, 0, 0),
                            new Vector3f(0.6f, 0.6f, 3.5f), 4);

                    Location pos = c.clone().add(0, sY, 0);
                    int dense = 8 + (int)(phase * 8);
                    for (int i = 0; i < dense; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = 0.6 + Math.random() * 1.0;
                        Location sp = pos.clone().add(Math.cos(a) * r, (Math.random() - 0.5) * 2.4, Math.sin(a) * r);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, sp, 1, 0.05, 0.05, 0.05, 0.05);
                    }
                    DisplayBuilder.dustParticles(pos, 3, 0.5, 180, 230, 255, 1.3f);
                    w.spawnParticle(Particle.END_ROD, pos, 2, 0.3, 0.4, 0.3, 0.03);
                    return;
                }
                descending = true;
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.8f);
            }

            // Fast descent: Y+15→0 over 8t (so dY ≈ 1.875/tick)
            if (!impacted) {
                sY -= 1.9;
                spear.animateTo(
                        new Vector3f(-0.3f, (float)sY, -1.75f),
                        new AxisAngle4f((float)(Math.PI / 2), 1, 0, 0),
                        new Vector3f(0.6f, 0.6f, 3.5f), 1);

                Location pos = c.clone().add(0, sY, 0);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 12, 0.3, 0.6, 0.3, 0.05);
                w.spawnParticle(Particle.CLOUD, pos, 6, 0.3, 0.3, 0.3, 0.03);
                w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 8, 0.3, 0.4, 0.3, 0.05);
                DisplayBuilder.dustParticles(pos, 4, 0.4, 180, 230, 255, 1.4f);

                if (sY <= 1.0) {
                    Location impact = c.clone().add(0, 0.4, 0);
                    impacted = true;
                    impactTick = tick;

                    // === IMPACT SOUNDS ===
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.6f);
                    DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 0.4f);

                    // Massive ELECTRIC_SPARK burst (50) + FALLING_DUST(WHITE_CONCRETE) ring + radial cracks
                    w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 50, 1.2, 0.6, 1.2, 0.4);
                    w.spawnParticle(Particle.FALLING_DUST, impact, 40, 1.0, 0.5, 1.0, 0.1,
                            Material.WHITE_CONCRETE.createBlockData());
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 40, 1.0, 0.4, 1.0, 0.2);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(impact, 30, 1.0, 180, 230, 255, 1.6f);
                    // Radial cracks: 4 spears of particles outward
                    for (int axis = 0; axis < 4; axis++) {
                        double ang = Math.PI * 2 * axis / 4;
                        for (int r = 1; r <= 7; r++) {
                            Location rp = impact.clone().add(Math.cos(ang) * r, 0.2, Math.sin(ang) * r);
                            w.spawnParticle(Particle.SNOWFLAKE, rp, 3, 0.2, 0.1, 0.2, 0.05);
                            w.spawnParticle(Particle.ELECTRIC_SPARK, rp, 2, 0.15, 0.1, 0.15, 0.04);
                        }
                    }

                    // ===== DISSIPATE: spear shatters into 4 sub-pieces flying outward =====
                    subPieces = new ItemDisplayHandle[4];
                    for (int i = 0; i < 4; i++) {
                        double ang = Math.PI * 2 * i / 4 + Math.PI / 8;
                        Location sp = c.clone().add(0, 2.0, 0);
                        ItemDisplayHandle sub = displayBuilder.spawnItem(sp, new ItemStack(Material.ICE));
                        sub.scale(0.4f, 0.4f, 1.0f).glow(180, 230, 255).interpolation(2, 0);
                        subPieces[i] = sub;
                        spawnedEntities.add(sub.entity());
                        // Fly outward + upward over DISSIPATE_LEN
                        double tx = Math.cos(ang) * 8.0;
                        double tz = Math.sin(ang) * 8.0;
                        sub.animateTo(
                                new Vector3f((float)tx - 0.2f, 4.0f, (float)tz - 0.2f),
                                new AxisAngle4f((float)(tick * 0.6 + i), 1, 0.4f, 0),
                                new Vector3f(0.001f), DISSIPATE_LEN);
                    }

                    // Hide main spear
                    spear.animateTo(
                            new Vector3f(-0.3f, 0.4f, -1.75f),
                            new AxisAngle4f((float)(Math.PI / 2), 1, 0, 0),
                            new Vector3f(0.001f), 6);

                    triggerImpactDamage(impact);
                }
                return;
            }

            // ===== DISSIPATE PHASE: sub-pieces trails + fading sparks =====
            if (impacted) {
                int age = tick - impactTick;
                if (age > DISSIPATE_LEN) return;
                if (subPieces == null) return;
                Location impact = c.clone().add(0, 0.4, 0);
                for (int i = 0; i < subPieces.length; i++) {
                    double ang = Math.PI * 2 * i / 4 + Math.PI / 8;
                    double progress = age / (double) DISSIPATE_LEN;
                    double px = Math.cos(ang) * 8.0 * progress;
                    double pz = Math.sin(ang) * 8.0 * progress;
                    double py = 2.0 + Math.sin(progress * Math.PI) * 2.0;
                    Location pp = c.clone().add(px, py, pz);
                    if (age % 2 == 0) {
                        w.spawnParticle(Particle.SNOWFLAKE, pp, 2, 0.2, 0.2, 0.2, 0.02);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, pp, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                // Lingering ground sparks
                if (age % 3 == 0) {
                    DisplayBuilder.dustParticles(impact, 4, 1.0, 180, 230, 255, 1.2f);
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 4, 1.5, 0.3, 1.5, 0.02);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PolarSpear(plugin); }
    }

    // ================================================================
    // 8. SLEET VOLLEY
    //    SPAWN (0-40t)   : 50 small SNOWBALL items materialize in fan at arena edge, scale 0→0.2, 8 PHANTOM_MEMBRANE accents
    //    ACTIVE (100t)   : fan sweeps 90° across zone, staggered animateTo per item, CLOUD streak, DRIPPING_WATER
    //    DISSIPATE (40t) : remaining items burst upward and fade
    // ================================================================
    public static class SleetVolley extends EnvironmentalAttack {
        private static final int SHARDS = 50;
        private static final int SPAWN_END = 40;
        private static final int SWEEP_LEN = 100;
        private static final int DISSIPATE_START = SPAWN_END + SWEEP_LEN; // 140
        private static final int DURATION = DISSIPATE_START + 40;        // 180

        private final List<ItemDisplayHandle> sleets = new ArrayList<>();
        private final List<ItemDisplayHandle> phantomAccents = new ArrayList<>();
        // Per-shard fan slot: each item has a per-shard target ring position + sweep stagger
        private final double[] slRingAng = new double[SHARDS];      // base angle within fan
        private final double[] slStartX = new double[SHARDS];
        private final double[] slStartZ = new double[SHARDS];
        private final double[] slCurX = new double[SHARDS];
        private final double[] slCurY = new double[SHARDS];
        private final double[] slCurZ = new double[SHARDS];
        private final int[] slStaggerTick = new int[SHARDS];        // staggered active start
        private final double[] slBurstVx = new double[SHARDS];
        private final double[] slBurstVy = new double[SHARDS];
        private final double[] slBurstVz = new double[SHARDS];
        private double baseAng;
        private static final double FAN_R = 8.0;
        private static final double FAN_Y = 6.0;

        public SleetVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sleet_volley", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(21600.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(DURATION);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.4f, 1.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_SWOOP, 0.9f, 1.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);

            Player target = findNearestPlayer(c, 30.0);
            if (target != null) {
                baseAng = Math.atan2(target.getLocation().getZ() - c.getZ(), target.getLocation().getX() - c.getX());
            } else {
                baseAng = Math.random() * Math.PI * 2;
            }

            // 50 shards in fan along arena edge (90° fan around baseAng+PI — opposite side, sweep into target)
            double launchSide = baseAng + Math.PI;
            for (int i = 0; i < SHARDS; i++) {
                double t = (i + 0.5) / SHARDS;
                double fanOffset = (t - 0.5) * Math.PI / 2; // -45..+45
                slRingAng[i] = launchSide + fanOffset;
                slStartX[i] = Math.cos(slRingAng[i]) * FAN_R;
                slStartZ[i] = Math.sin(slRingAng[i]) * FAN_R;
                slCurX[i] = slStartX[i];
                slCurY[i] = FAN_Y;
                slCurZ[i] = slStartZ[i];
                slStaggerTick[i] = (int)(i * (SWEEP_LEN * 0.7 / SHARDS)); // staggered start
                double burstSpeed = 0.35 + Math.random() * 0.25;
                slBurstVx[i] = Math.cos(slRingAng[i]) * burstSpeed;
                slBurstVz[i] = Math.sin(slRingAng[i]) * burstSpeed;
                slBurstVy[i] = 0.25 + Math.random() * 0.15;

                Location p = c.clone().add(slStartX[i], FAN_Y, slStartZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                // SPAWN: invisible, animate to 0.2 small shards
                h.scale(0.001f, 0.001f, 0.001f).glow(220, 240, 255).interpolation(2, 0);
                sleets.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 PHANTOM_MEMBRANE flutter accents
            for (int i = 0; i < 8; i++) {
                double ang = launchSide + (Math.random() - 0.5) * Math.PI / 2;
                double rr = FAN_R - 1 + Math.random() * 2;
                Location p = c.clone().add(Math.cos(ang) * rr, FAN_Y + (Math.random() - 0.5) * 2, Math.sin(ang) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 220, 240).interpolation(2, 0);
                phantomAccents.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ===== SPAWN PHASE (0..40t): fan materializes at arena edge =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                float scale = (float)(0.2 * progress);
                for (int i = 0; i < sleets.size(); i++) {
                    sleets.get(i).animateTo(
                            new Vector3f((float)slStartX[i] - scale / 2, (float)slCurY[i], (float)slStartZ[i] - scale / 2),
                            new AxisAngle4f((float)(tick * 0.5 + i * 0.4), 0, 1, 0),
                            new Vector3f(scale), 3);
                }
                // PHANTOM_MEMBRANE accents flutter in
                float accScale = (float)(0.5 * progress);
                for (int i = 0; i < phantomAccents.size(); i++) {
                    phantomAccents.get(i).animateTo(
                            new Vector3f(-accScale / 2, 0, -accScale / 2),
                            new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                            new Vector3f(accScale), 4);
                }
                // Layered particles: fan-arc shimmer + DRIPPING_WATER + CLOUD
                int dots = 24;
                for (int k = 0; k < dots; k++) {
                    double t2 = k / (double) dots;
                    double fanOffset = (t2 - 0.5) * Math.PI / 2;
                    double ang = baseAng + Math.PI + fanOffset;
                    Location rp = c.clone().add(Math.cos(ang) * FAN_R, FAN_Y, Math.sin(ang) * FAN_R);
                    if (k % 2 == 0) w.spawnParticle(Particle.CLOUD, rp, 1, 0.05, 0.05, 0.05, 0.01);
                    if (k % 3 == 0) DisplayBuilder.dustParticles(rp, 1, 0.1, 220, 240, 255, 1.0f);
                    if (k % 4 == 0) w.spawnParticle(Particle.DRIPPING_WATER, rp, 1, 0.05, 0.05, 0.05, 0);
                    if (k % 5 == 0) w.spawnParticle(Particle.SNOWFLAKE, rp, 1, 0.05, 0.05, 0.05, 0.01);
                }
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f + (float) progress * 1.0f);
                }
                return;
            }

            // ===== ACTIVE SWEEP PHASE (40..140t): fan sweeps 90° across zone over 100t =====
            if (tick == SPAWN_END) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_SWOOP, 1.4f, 1.2f);
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 1.5f);
            }
            if (tick < DISSIPATE_START) {
                int sweepTick = tick - SPAWN_END;
                for (int i = 0; i < sleets.size(); i++) {
                    int localT = sweepTick - slStaggerTick[i];
                    if (localT < 0) continue;
                    double t = Math.min(1.0, localT / 30.0);  // each shard travels in ~30t
                    double ease = t * t * (3 - 2 * t);
                    // Target across zone — opposite of starting fan position
                    double targetX = -slStartX[i] * 1.2;
                    double targetZ = -slStartZ[i] * 1.2;
                    slCurX[i] = slStartX[i] + (targetX - slStartX[i]) * ease;
                    slCurZ[i] = slStartZ[i] + (targetZ - slStartZ[i]) * ease;
                    slCurY[i] = FAN_Y - ease * 4.5 + Math.sin(localT * 0.3) * 0.3;

                    sleets.get(i).animateTo(
                            new Vector3f((float)slCurX[i] - 0.1f, (float)slCurY[i], (float)slCurZ[i] - 0.1f),
                            new AxisAngle4f((float)(sweepTick * 0.4 + i * 0.3), 0, 1, 0),
                            new Vector3f(0.2f), 2);

                    // Layered streak: CLOUD per item (1/tick) + DRIPPING_WATER occasional + SNOWFLAKE
                    Location pos = c.clone().add(slCurX[i], slCurY[i], slCurZ[i]);
                    w.spawnParticle(Particle.CLOUD, pos, 1, 0.05, 0.05, 0.05, 0.01);
                    if ((sweepTick + i) % 4 == 0) {
                        w.spawnParticle(Particle.DRIPPING_WATER, pos, 1, 0.08, 0.08, 0.08, 0);
                    }
                    if ((sweepTick + i) % 3 == 0) {
                        w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.05, 0.05, 0.05, 0.02);
                    }

                    // On landing — staggered landings produce powder-snow break per shard
                    if (t >= 1.0 && localT == 30) {
                        DisplayBuilder.playSound(pos, Sound.BLOCK_POWDER_SNOW_BREAK, 0.5f, 0.8f + (float) Math.random() * 0.5f);
                        w.spawnParticle(Particle.SNOWFLAKE, pos, 4, 0.2, 0.2, 0.2, 0.05);
                        DisplayBuilder.dustParticles(pos, 3, 0.2, 220, 240, 255, 1.1f);
                    }
                }
                // Active ambient: WEATHER_RAIN looped pitch 1.6
                if (sweepTick % 14 == 0) {
                    DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.6f, 1.6f);
                }
                if (sweepTick % 22 == 11) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_SWOOP, 0.6f, 1.4f);
                }
                return;
            }

            // ===== DISSIPATE PHASE (140..180t): remaining items burst upward + fade =====
            if (tick == DISSIPATE_START) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_BREAK, 1.2f, 1.0f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.9f);
                for (int i = 0; i < sleets.size(); i++) {
                    // Burst upward + outward (override toward sky)
                    sleets.get(i).animateTo(
                            new Vector3f((float)slCurX[i] - 0.1f, (float)slCurY[i] + 6.0f, (float)slCurZ[i] - 0.1f),
                            new AxisAngle4f((float)(tick * 0.7 + i), 0, 1, 0),
                            new Vector3f(0.001f), 40);
                }
                // Cloud-burst at center
                Location burst = c.clone().add(0, FAN_Y, 0);
                w.spawnParticle(Particle.CLOUD, burst, 60, 4.0, 1.5, 4.0, 0.2);
                w.spawnParticle(Particle.SNOWFLAKE, burst, 40, 3.5, 1.2, 3.5, 0.2);
                w.spawnParticle(Particle.FLASH, burst, 1, 0, 0, 0, 0);
            }
            if (tick >= DISSIPATE_START) {
                int age = tick - DISSIPATE_START;
                if (age > 40) return;
                for (int i = 0; i < sleets.size(); i++) {
                    if ((tick + i) % 3 == 0) {
                        slCurX[i] += slBurstVx[i] * 0.3;
                        slCurY[i] += slBurstVy[i] - age * 0.01;
                        slCurZ[i] += slBurstVz[i] * 0.3;
                        Location pos = c.clone().add(slCurX[i], slCurY[i], slCurZ[i]);
                        w.spawnParticle(Particle.CLOUD, pos, 1, 0.1, 0.1, 0.1, 0.01);
                        if ((tick + i) % 5 == 0) {
                            w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }
            }

            // Constant damage radius 15
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SleetVolley(plugin); }
    }

    // ================================================================
    // 9. FROSTBITE DARTS
    //    SPAWN (0-30t)   : 12 AMETHYST_SHARD darts materialize in r=8 circle at Y+3, scale 0→0.35,
    //                      3 ECHO_SHARD targeting cores at center
    //    ACTIVE          : cores pulse 15t then fire darts inward staggered with ELECTRIC_SPARK trail
    //    DISSIPATE (40t) : cores dim, lingering DUST particles fade
    // ================================================================
    public static class FrostbiteDarts extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> darts = new ArrayList<>();
        private final List<ItemDisplayHandle> cores = new ArrayList<>();
        private final double[] dAng = new double[12];
        private final double[] dR = new double[12];
        private final int[] dStartTick = new int[12];
        private final boolean[] dMaterialized = new boolean[12];
        private final boolean[] dFired = new boolean[12];
        private final boolean[] dImpacted = new boolean[12];
        private int lastImpactTick = -1;
        private static final double START_R = 8.0;
        private static final int SPAWN_END = 30;
        private static final int CORE_PULSE_TICKS = 15;
        private static final int DISSIPATE_LEN = 40;

        public FrostbiteDarts(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frostbite_darts", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(36000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(200);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(36000.0);
            config.setImpactRadius(3.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1.0f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.4f);

            for (int i = 0; i < 12; i++) {
                dAng[i] = Math.PI * 2 * i / 12;
                dR[i] = START_R;
                dStartTick[i] = SPAWN_END + i * 5; // staggered fire
                dMaterialized[i] = false;
                dFired[i] = false;
                dImpacted[i] = false;
                Location p = c.clone().add(Math.cos(dAng[i]) * dR[i], 3.0, Math.sin(dAng[i]) * dR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                // SPAWN: start invisible, materialize at dart shape
                h.scale(0.001f, 0.001f, 0.001f).glow(220, 180, 255).interpolation(2, 0);
                darts.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 ECHO_SHARD targeting cores at center
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 * i / 3;
                Location pp = c.clone().add(Math.cos(a) * 0.8, 3.0, Math.sin(a) * 0.8);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.001f, 0.001f, 0.001f).glow(200, 160, 255).interpolation(2, 0);
                cores.add(h);
                spawnedEntities.add(h.entity());
            }

            // Anchor decoration
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * START_R, 4.5, Math.sin(a) * START_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_CLUSTER));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 180, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ===== SPAWN PHASE (0..30t): darts + cores materialize =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                float dScale = (float)(0.35 * progress);
                for (int i = 0; i < darts.size(); i++) {
                    double dx = Math.cos(dAng[i]) * START_R;
                    double dz = Math.sin(dAng[i]) * START_R;
                    darts.get(i).animateTo(
                            new Vector3f((float)dx - dScale / 2, 3.0f, (float)dz - dScale / 2),
                            new AxisAngle4f((float)dAng[i] + (float)Math.PI + (float)(tick * 0.3), 0, 1, 0),
                            new Vector3f(dScale, dScale, dScale * 2), 3);
                }
                // Cores materialize
                float cScale = (float)(0.6 * progress);
                for (int i = 0; i < cores.size(); i++) {
                    double a = Math.PI * 2 * i / 3 + tick * 0.05;
                    double cx = Math.cos(a) * 0.8;
                    double cz = Math.sin(a) * 0.8;
                    cores.get(i).animateTo(
                            new Vector3f((float)cx - cScale / 2, 3.0f, (float)cz - cScale / 2),
                            new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                            new Vector3f(cScale), 2);
                }
                // Rising chime
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.6f + (float) progress * 1.0f);
                }
                // Layered particles: GLOW core + violet DUST + ELECTRIC_SPARK
                Location coreCtr = c.clone().add(0, 3.0, 0);
                w.spawnParticle(Particle.GLOW, coreCtr, 4, 0.6, 0.4, 0.6, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, coreCtr, 4, 0.4, 0.4, 0.4, 0.05);
                DisplayBuilder.dustParticles(coreCtr, 4, 0.5, 200, 160, 255, 1.2f);
                return;
            }

            // ===== ACTIVE PHASE: cores pulse, then fire darts staggered =====
            // Cores keep pulsing every tick
            for (int i = 0; i < cores.size(); i++) {
                double a = Math.PI * 2 * i / 3 + tick * 0.15;
                double cx = Math.cos(a) * 1.0;
                double cz = Math.sin(a) * 1.0;
                float pulse = (float)(0.5 + Math.sin(tick * 0.3 + i) * 0.15);
                cores.get(i).animateTo(
                        new Vector3f((float)cx - pulse / 2, 3.0f, (float)cz - pulse / 2),
                        new AxisAngle4f((float)(tick * 0.5 + i), 0, 1, 0),
                        new Vector3f(pulse), 2);
                Location cp = c.clone().add(cx, 3.0, cz);
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, cp, 2, 0.15, 0.15, 0.15, 0.03);
                }
            }

            for (int i = 0; i < darts.size(); i++) {
                if (dImpacted[i] || tick < dStartTick[i]) continue;
                int localTick = tick - dStartTick[i];

                if (!dFired[i]) {
                    // CORE pulse phase 15t: rising chime, dart locks on
                    if (localTick % 3 == 0 && localTick < CORE_PULSE_TICKS) {
                        float chime = 1.0f + localTick * 0.06f;
                        DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, Math.min(2.0f, chime));
                    }
                    if (localTick >= CORE_PULSE_TICKS) {
                        dFired[i] = true;
                        Location pos = c.clone().add(Math.cos(dAng[i]) * dR[i], 3.0, Math.sin(dAng[i]) * dR[i]);
                        // === FIRE SOUND ===
                        DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.2f, 1.5f);
                    }
                    // Hover wiggle with charging-up energy
                    Location pos = c.clone().add(Math.cos(dAng[i]) * dR[i],
                            3.0 + Math.sin(tick * 0.2 + i) * 0.2,
                            Math.sin(dAng[i]) * dR[i]);
                    darts.get(i).animateTo(
                            new Vector3f((float)(Math.cos(dAng[i]) * dR[i]) - 0.175f, (float)(pos.getY() - c.getY()), (float)(Math.sin(dAng[i]) * dR[i]) - 0.175f),
                            new AxisAngle4f((float)dAng[i] + (float)Math.PI, 0, 1, 0),
                            new Vector3f(0.35f, 0.35f, 0.7f), 2);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 2, 0.1, 0.1, 0.1, 0.03);
                    DisplayBuilder.dustParticles(pos, 1, 0.1, 200, 160, 255, 1.0f);
                } else {
                    // Fired — fly inward
                    dR[i] -= 0.55;
                    Location pos = c.clone().add(Math.cos(dAng[i]) * dR[i], 3.0, Math.sin(dAng[i]) * dR[i]);
                    darts.get(i).animateTo(
                            new Vector3f((float)(Math.cos(dAng[i]) * dR[i]) - 0.175f, 3.0f, (float)(Math.sin(dAng[i]) * dR[i]) - 0.175f),
                            new AxisAngle4f((float)dAng[i] + (float)Math.PI, 0, 1, 0),
                            new Vector3f(0.35f, 0.35f, 0.7f), 2);

                    // Cold-blue ELECTRIC_SPARK trail + SNOWFLAKE
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 3, 0.05, 0.05, 0.05, 0.05);
                    w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.05, 0.05, 0.05, 0.02);
                    DisplayBuilder.dustParticles(pos, 1, 0.08, 200, 220, 255, 1.0f);

                    if (dR[i] <= 0.4) {
                        Location impact = c.clone().add(Math.cos(dAng[i]) * 0.2, 2.5, Math.sin(dAng[i]) * 0.2);
                        // === IMPACT SOUNDS ===
                        DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.4f, 1.3f);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_HIT, 0.8f, 1.4f);

                        // END_ROD burst + AMETHYST_BLOCK_BREAK + violet DUST shimmer
                        w.spawnParticle(Particle.END_ROD, impact, 16, 0.4, 0.3, 0.4, 0.2);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 18, 0.4, 0.3, 0.4, 0.4);
                        w.spawnParticle(Particle.SNOWFLAKE, impact, 10, 0.3, 0.3, 0.3, 0.1);
                        DisplayBuilder.dustParticles(impact, 12, 0.5, 200, 150, 255, 1.4f);

                        triggerImpactDamage(impact);
                        dImpacted[i] = true;
                        lastImpactTick = tick;
                    }
                }
            }

            // ===== DISSIPATE PHASE: cores dim, lingering dust fades =====
            // Triggers after last dart has impacted
            boolean allImpacted = true;
            for (int i = 0; i < 12; i++) if (!dImpacted[i]) { allImpacted = false; break; }
            if (allImpacted && lastImpactTick > 0) {
                int age = tick - lastImpactTick;
                if (age >= 0 && age <= DISSIPATE_LEN) {
                    if (age == 0) {
                        DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.4f);
                        DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
                    }
                    // Cores shrink toward 0 over DISSIPATE_LEN
                    float fade = (float) Math.max(0.001, 0.5 * (1.0 - age / (double) DISSIPATE_LEN));
                    for (int i = 0; i < cores.size(); i++) {
                        double a = Math.PI * 2 * i / 3 + tick * 0.05;
                        double cx = Math.cos(a) * 1.0;
                        double cz = Math.sin(a) * 1.0;
                        cores.get(i).animateTo(
                                new Vector3f((float)cx - fade / 2, 3.0f, (float)cz - fade / 2),
                                new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                                new Vector3f(fade), 2);
                    }
                    // Lingering violet dust trickle at center
                    if (age % 2 == 0) {
                        Location coreCtr = c.clone().add(0, 3.0, 0);
                        DisplayBuilder.dustParticles(coreCtr, 2, 1.0, 200, 150, 255, 1.1f);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, coreCtr, 1, 0.5, 0.5, 0.5, 0.02);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostbiteDarts(plugin); }
    }

    // ================================================================
    // 10. COMET STRIKE
    //     SPAWN (0-50t)  : 1 DIAMOND comet head materializes at arena edge Y+25, scale 0→1.8,
    //                      10 BLUE_ICE tail fragments form behind, SOUL_FIRE_FLAME core, SNOWFLAKE dense (15/tick)
    //     ACTIVE         : sweeps down at steep diagonal toward center over 60t, tail fragments lag
    //     DISSIPATE      : massive impact, smoke column rises + tail fragments scatter outward
    // ================================================================
    public static class CometStrike extends EnvironmentalAttack {
        private ItemDisplayHandle comet;
        private final List<ItemDisplayHandle> tailFragments = new ArrayList<>();
        private double cX, cY, cZ;
        private double startX, startZ, startY;
        private double dx, dy, dz;
        private boolean impacted = false;
        private int impactTick = -1;
        private static final int SPAWN_END = 50;
        private static final int FLIGHT_TICKS = 60;
        private static final int DISSIPATE_LEN = 40;
        private static final int TAIL_LAG = 4;          // ticks each tail fragment lags behind

        public CometStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("comet_strike", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(72000.0);
            config.setDamageRadius(16.5);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(180);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(72000.0);
            config.setImpactRadius(16.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // === SPAWN PHASE SOUNDS ===
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.6f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);

            double ang = Math.random() * Math.PI * 2;
            startX = Math.cos(ang) * 15.0;
            startZ = Math.sin(ang) * 15.0;
            startY = 25.0;
            cX = startX;
            cY = startY;
            cZ = startZ;

            // Target center over FLIGHT_TICKS — steep diagonal
            double targetX = 0, targetY = 0.4, targetZ = 0;
            dx = (targetX - cX) / FLIGHT_TICKS;
            dy = (targetY - cY) / FLIGHT_TICKS;
            dz = (targetZ - cZ) / FLIGHT_TICKS;

            Location p = c.clone().add(cX, cY, cZ);
            comet = displayBuilder.spawnItem(p, new ItemStack(Material.DIAMOND));
            // SPAWN: start invisible, animate to 1.8 during spawn phase
            comet.scale(0.001f, 0.001f, 0.001f).glow(160, 230, 255).interpolation(2, 0);
            spawnedEntities.add(comet.entity());

            // 10 BLUE_ICE tail fragments form behind during spawn
            for (int i = 0; i < 10; i++) {
                Location pp = c.clone().add(cX, cY, cZ);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.BLUE_ICE));
                h.scale(0.001f, 0.001f, 0.001f).glow(160, 220, 255).interpolation(2, 0);
                tailFragments.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (comet == null) return;

            // ===== SPAWN PHASE (0..50t): comet materializes + tail fragments form =====
            if (tick < SPAWN_END) {
                double progress = tick / (double) SPAWN_END;
                float scale = (float)(1.8 * progress);
                comet.animateTo(
                        new Vector3f((float)cX - scale / 2, (float)cY, (float)cZ - scale / 2),
                        new AxisAngle4f((float)(tick * 0.4), 1, 1, 0),
                        new Vector3f(scale), 3);

                // Tail fragments form behind: each lined up along the backward travel direction
                for (int i = 0; i < tailFragments.size(); i++) {
                    double back = (i + 1) * 1.2;
                    double tx = cX - dx * back * 3;
                    double ty = cY - dy * back * 3;
                    double tz = cZ - dz * back * 3;
                    float tScale = (float)((0.5 - i * 0.025) * progress);
                    if (tScale < 0.05) tScale = 0.05f;
                    tailFragments.get(i).animateTo(
                            new Vector3f((float)tx - tScale / 2, (float)ty, (float)tz - tScale / 2),
                            new AxisAngle4f((float)(tick * 0.3 + i), 1, 0.5f, 0),
                            new Vector3f(tScale), 3);
                }

                // Layered charge particles: SOUL_FIRE_FLAME core + SNOWFLAKE dense (15/tick) + ELECTRIC_SPARK + DUST
                Location pos = c.clone().add(cX, cY, cZ);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 6, 0.5, 0.5, 0.5, 0.04);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 15, 0.6, 0.6, 0.6, 0.04);
                w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 4, 0.4, 0.4, 0.4, 0.04);
                DisplayBuilder.dustParticles(pos, 3, 0.4, 180, 230, 255, 1.4f);

                // Long pull-in howl looped 3x during spawn
                if (tick == 0 || tick == 16 || tick == 32) {
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 0.5f);
                }
                return;
            }

            // ===== ACTIVE PHASE: sweep down at steep diagonal =====
            if (!impacted) {
                int flight = tick - SPAWN_END;
                if (flight == 0) {
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.4f, 0.7f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
                }
                cX += dx;
                cY += dy;
                cZ += dz;
                Location pos = c.clone().add(cX, cY, cZ);

                comet.animateTo(
                        new Vector3f((float)cX - 0.9f, (float)cY, (float)cZ - 0.9f),
                        new AxisAngle4f((float)(tick * 0.4), 1, 1, 0),
                        new Vector3f(1.8f), 2);

                // Tail fragments lag behind — each with own delayed animateTo
                for (int i = 0; i < tailFragments.size(); i++) {
                    int lag = (i + 1) * TAIL_LAG;
                    // Past lag-ticks back, the head was at this location:
                    double bx = cX - dx * lag;
                    double by = cY - dy * lag;
                    double bz = cZ - dz * lag;
                    float tScale = Math.max(0.05f, 0.5f - i * 0.025f);
                    tailFragments.get(i).animateTo(
                            new Vector3f((float)bx - tScale / 2, (float)by, (float)bz - tScale / 2),
                            new AxisAngle4f((float)(tick * 0.3 + i * 0.5), 1, 0.5f, 0),
                            new Vector3f(tScale), TAIL_LAG);
                }

                // Layered flight trail: SOUL_FIRE_FLAME core + SNOWFLAKE dense (15/tick) + FLAME + DUST
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 6, 0.25, 0.25, 0.25, 0.04);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 15, 0.4, 0.4, 0.4, 0.06);
                w.spawnParticle(Particle.FLAME, pos, 4, 0.2, 0.2, 0.2, 0.04);
                DisplayBuilder.dustParticles(pos, 4, 0.3, 180, 230, 255, 1.5f);

                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.8f, 0.5f);
                }

                if (cY <= 0.6) {
                    Location impact = c.clone().add(0, 0.4, 0);
                    impacted = true;
                    impactTick = tick;

                    // === IMPACT SOUNDS ===
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.6f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_POWDER_SNOW_BREAK, 1.6f, 0.4f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);

                    // Massive ELECTRIC_SPARK burst (50) + END_ROD vertical column + radial SNOWFLAKE ring
                    w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 50, 1.5, 1.0, 1.5, 0.6);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, impact, 40, 1.2, 0.8, 1.2, 0.3);
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 60, 1.5, 0.6, 1.5, 0.2);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 2, 0.5, 0.2, 0.5, 0);
                    w.spawnParticle(Particle.FLASH, impact, 2, 0.1, 0.1, 0.1, 0);
                    DisplayBuilder.dustParticles(impact, 60, 2.0, 180, 230, 255, 2.0f);
                    // END_ROD vertical column from impact
                    for (int y = 0; y < 14; y++) {
                        w.spawnParticle(Particle.END_ROD, impact.clone().add(0, y * 0.6, 0), 2, 0.2, 0.2, 0.2, 0.02);
                    }
                    // Radial SNOWFLAKE ring r=11
                    DisplayBuilder.particleRing(impact, 11.0, Particle.SNOWFLAKE, 60, null);

                    // Tail fragments scatter outward
                    for (int i = 0; i < tailFragments.size(); i++) {
                        double scatterAng = Math.PI * 2 * i / tailFragments.size() + Math.random() * 0.4;
                        double sx = Math.cos(scatterAng) * 10.0;
                        double sz = Math.sin(scatterAng) * 10.0;
                        tailFragments.get(i).animateTo(
                                new Vector3f((float)sx - 0.2f, 3.0f, (float)sz - 0.2f),
                                new AxisAngle4f((float)(tick * 0.6 + i), 1, 0.4f, 0),
                                new Vector3f(0.001f), DISSIPATE_LEN);
                    }

                    // Hide comet head
                    comet.animateTo(
                            new Vector3f(-0.9f, 0.4f, -0.9f),
                            new AxisAngle4f((float)(tick * 0.4), 1, 1, 0),
                            new Vector3f(0.001f), 8);

                    triggerImpactDamage(impact);
                }
                return;
            }

            // ===== DISSIPATE PHASE: smoke column + lingering glowing crater =====
            if (impacted) {
                int age = tick - impactTick;
                if (age > DISSIPATE_LEN) return;
                Location impact = c.clone().add(0, 0.4, 0);
                // Rising smoke column
                int height = Math.min(14, age / 2);
                for (int y = 0; y <= height; y++) {
                    double r = 0.7 + y * 0.15;
                    int dots = 8;
                    for (int k = 0; k < dots; k++) {
                        double ang = Math.PI * 2 * k / dots + age * 0.08 + y * 0.25;
                        Location rp = impact.clone().add(Math.cos(ang) * r, y * 0.7, Math.sin(ang) * r);
                        if (k % 2 == 0) DisplayBuilder.dustParticles(rp, 1, 0.1, 180, 220, 255, 1.0f);
                        if (k % 3 == 0) w.spawnParticle(Particle.CLOUD, rp, 1, 0.05, 0.05, 0.05, 0.01);
                        if (k % 4 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, rp, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }
                // Lingering glowing crater END_ROD
                if (age % 2 == 0) {
                    int craterDots = 16;
                    for (int k = 0; k < craterDots; k++) {
                        double ang = Math.PI * 2 * k / craterDots + age * 0.1;
                        double rr = 2.0 + Math.sin(age * 0.15 + k) * 0.5;
                        Location rp = impact.clone().add(Math.cos(ang) * rr, 0.3, Math.sin(ang) * rr);
                        if (k % 2 == 0) w.spawnParticle(Particle.END_ROD, rp, 1, 0, 0, 0, 0);
                    }
                }
                if (age == 4 || age == 24) {
                    DisplayBuilder.playSound(impact, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CometStrike(plugin); }
    }
}
