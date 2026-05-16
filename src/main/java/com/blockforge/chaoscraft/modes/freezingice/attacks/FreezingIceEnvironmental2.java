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
 * FreezingIce Mode — ENVIRONMENTAL FX BATCH 2 (entries 11-20).
 * Drifting Hazards — clouds, fog, blizzards, breath, haze, banks,
 * veils, gusts, spike fields, and frozen puddles. Pure ItemDisplay
 * + particle attacks. NO BlockDisplays.
 *
 * Each attack follows a strict 3-phase choreography:
 *   SPAWN (intro 0-30/40t)  →  ACTIVE (mid)  →  DISSIPATE (final 40t).
 * Three attacks use FOLLOW-AI (drift toward nearest player at sub-walk
 * speed): DriftingFrostCloud (0.06), ProwlingColdFog (0.05),
 * HuntingBlizzardPocket (0.10).
 */
public final class FreezingIceEnvironmental2 {
    private FreezingIceEnvironmental2() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DriftingFrostCloud(plugin));
        registry.register(new ProwlingColdFog(plugin));
        registry.register(new HuntingBlizzardPocket(plugin));
        registry.register(new SlowFrostBreath(plugin));
        registry.register(new WanderingHoarfrostHaze(plugin));
        registry.register(new CreepingRimeBank(plugin));
        registry.register(new StalkingChillVeil(plugin));
        registry.register(new FrozenGustFront(plugin));
        registry.register(new FrostSpikeField(plugin));
        registry.register(new ExpandingFrozenPuddle(plugin));
    }

    // ================================================================
    // 11. DRIFTING FROST CLOUD — FOLLOW-AI 0.06.
    //     Spawn: 14 SNOWBALL puffs form above center (scale 0->0.6).
    //     Active: drifts toward player; bobs ±0.2Y; CLOUD drizzle + SNOWFLAKE rain.
    //     Dissipate: cloud dissolves outward, fades to 0.
    // ================================================================
    public static class DriftingFrostCloud extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> puffs = new ArrayList<>();
        private final double[] pOx = new double[14];
        private final double[] pOz = new double[14];
        private final double[] pY = new double[14];
        private final double[] pPhase = new double[14];
        private final double[] pDx = new double[14]; // dissipate scatter dir
        private final double[] pDz = new double[14];

        public DriftingFrostCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("drifting_frost_cloud", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2640.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.06);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // Spawn ambient sounds
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.0f, 0.9f);

            // 14 SNOWBALL puffs — puffy cloud forms above center, random offsets
            for (int i = 0; i < 14; i++) {
                double a = Math.PI * 2 * i / 14 + (Math.random() - 0.5) * 0.4;
                double r = 1.5 + Math.random() * 3.0;
                pOx[i] = Math.cos(a) * r;
                pOz[i] = Math.sin(a) * r;
                pY[i] = 4.0 + (Math.random() - 0.5) * 1.4;
                pPhase[i] = Math.random() * Math.PI * 2;
                // Dissipate scatter direction (+5 random direction)
                double da = Math.random() * Math.PI * 2;
                pDx[i] = Math.cos(da) * 5.0;
                pDz[i] = Math.sin(da) * 5.0;
                Location p = c.clone().add(pOx[i], pY[i], pOz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.01f, 0.01f, 0.01f).glow(220, 235, 255).interpolation(30, 0);
                puffs.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 30;
            boolean dissipatePhase = tick >= dissipateStart;

            // --- ItemDisplay choreography ---
            if (spawnPhase) {
                // scale 0 -> 0.6
                float progress = Math.min(1f, tick / 30f);
                float scale = 0.6f * progress;
                if (tick % 2 == 0) {
                    for (int i = 0; i < puffs.size(); i++) {
                        puffs.get(i).animateTo(
                                new Vector3f((float)pOx[i] - scale * 0.5f, (float)pY[i], (float)pOz[i] - scale * 0.5f),
                                new AxisAngle4f((float)(tick * 0.03 + i), 0, 1, 0),
                                new Vector3f(scale), 4);
                    }
                }
            } else if (dissipatePhase) {
                // Cloud dissolves outward — items animateTo +5 random direction; scale 0.6 -> 0
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = 0.6f * (1f - t01);
                if (tick % 2 == 0) {
                    for (int i = 0; i < puffs.size(); i++) {
                        float bx = (float)(pOx[i] + pDx[i] * t01);
                        float bz = (float)(pOz[i] + pDz[i] * t01);
                        puffs.get(i).animateTo(
                                new Vector3f(bx - scale * 0.5f, (float)pY[i], bz - scale * 0.5f),
                                new AxisAngle4f((float)(tick * 0.06 + i), 0, 1, 0),
                                new Vector3f(scale), 2);
                    }
                }
            } else {
                // Active — bob ±0.2 Y; gentle continuous motion
                if (tick % 2 == 0) {
                    for (int i = 0; i < puffs.size(); i++) {
                        double bobY = pY[i] + Math.sin(tick * 0.05 + pPhase[i]) * 0.2;
                        puffs.get(i).animateTo(
                                new Vector3f((float)pOx[i] - 0.3f, (float)bobY, (float)pOz[i] - 0.3f),
                                new AxisAngle4f((float)(tick * 0.02 + i), 0, 1, 0),
                                new Vector3f(0.6f), 4);
                    }
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // Spawn-flourish: intensifying SNOWFLAKE rain + CLOUD halo
                int intensity = 2 + tick / 10;
                for (int i = 0; i < intensity; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.5;
                    Location p = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 4.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.4, 0.1, 0.02);
                }
                if (tick % 3 == 0) for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * 3.5, 4.0, Math.sin(a) * 3.5);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.3f);
                }
            } else if (dissipatePhase) {
                // Sparse trailing sparkles as cloud dissolves
                if (tick % 2 == 0) for (int i = 0; i < 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 7.0;
                    Location p = c.clone().add(Math.cos(a) * r, 1.5 + Math.random() * 2.5, Math.sin(a) * r);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.05, 0.05, 0.05, 0.0);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.01);
                }
            } else {
                // Active — 3 layered patterns:
                // 1) CLOUD drizzle below (4/tick)
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 4.5;
                    double colY = 3.5 - Math.random() * 4.5;
                    Location p = c.clone().add(Math.cos(a) * r, colY, Math.sin(a) * r);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.15, 0.1, 0.15, 0.01);
                }
                // 2) SNOWFLAKE rain inside footprint (6/tick)
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 4.5;
                    double yy = 0.3 + Math.random() * 3.5;
                    Location p = c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.0, 0.1, 0.02);
                }
                // 3) DUST cold-blue halo ring at cloud edge
                if (tick % 6 == 0) for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12 + tick * 0.02;
                    Location p = c.clone().add(Math.cos(a) * 4.5, 4.0 + Math.sin(tick * 0.04 + i) * 0.3, Math.sin(a) * 4.5);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.2f);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 0.6f, 0.8f + (float)Math.random() * 0.4f);
            } else if (dissipatePhase) {
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.2f, 1.0f);
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.5f, 1.2f + (float)Math.random() * 0.3f);
            } else {
                // Active ambient: WEATHER_RAIN pitch 0.7 + soft BLOCK_POWDER_SNOW_PLACE
                if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.6f, 0.7f);
                if (tick % 18 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 0.4f, 0.9f + (float)Math.random() * 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new DriftingFrostCloud(plugin); }
    }

    // ================================================================
    // 12. PROWLING COLD FOG — FOLLOW-AI 0.05.
    //     Spawn: 20 PHANTOM_MEMBRANE at ground + 4 rising wisps.
    //     Active: creeps toward player; swirling fog blanket; SCULK_SOUL drift.
    //     Dissipate: rises upward and dissipates, items animateTo Y+8 shrinking.
    // ================================================================
    public static class ProwlingColdFog extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> wisps = new ArrayList<>();
        private final List<ItemDisplayHandle> tallWisps = new ArrayList<>();
        private final double[] wAng = new double[20];
        private final double[] wR = new double[20];
        private final double[] wPhase = new double[20];
        private final double[] tWAng = new double[4];
        private final double[] tWR = new double[4];
        private final double[] tWBaseY = new double[4];

        public ProwlingColdFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prowling_cold_fog", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2160.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(420);
            config.setCooldownTicks(60);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.05);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.0f, 0.9f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.4f);

            // 20 PHANTOM_MEMBRANE at ground level in fog blanket pattern
            for (int i = 0; i < 20; i++) {
                wAng[i] = Math.PI * 2 * i / 20 + (Math.random() - 0.5) * 0.2;
                wR[i] = 1.0 + Math.random() * 6.5;
                wPhase[i] = Math.random() * Math.PI * 2;
                Location p = c.clone().add(Math.cos(wAng[i]) * wR[i], 0.35 + Math.random() * 0.3, Math.sin(wAng[i]) * wR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 200, 220).interpolation(40, 0);
                wisps.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 PHANTOM_MEMBRANE rising upward as tall wisps
            for (int i = 0; i < 4; i++) {
                tWAng[i] = Math.PI * 2 * i / 4 + Math.PI / 4;
                tWR[i] = 2.5 + Math.random() * 1.5;
                tWBaseY[i] = 1.0;
                Location p = c.clone().add(Math.cos(tWAng[i]) * tWR[i], tWBaseY[i], Math.sin(tWAng[i]) * tWR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.01f, 0.01f, 0.01f).glow(160, 180, 210).interpolation(40, 0);
                tallWisps.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 40;
            boolean dissipatePhase = tick >= dissipateStart;

            // --- ItemDisplay choreography ---
            if (spawnPhase) {
                float progress = Math.min(1f, tick / 40f);
                float scale = 0.4f * progress;
                if (tick % 2 == 0) {
                    for (int i = 0; i < wisps.size(); i++) {
                        wAng[i] += 0.012;
                        float bx = (float)(Math.cos(wAng[i]) * wR[i]);
                        float bz = (float)(Math.sin(wAng[i]) * wR[i]);
                        wisps.get(i).animateTo(
                                new Vector3f(bx - scale * 1.75f, 0.35f, bz - scale * 1.75f),
                                new AxisAngle4f((float)(tick * 0.015 + i), 0, 1, 0),
                                new Vector3f(scale * 3.5f, scale * 0.375f, scale * 3.5f), 4);
                    }
                    for (int i = 0; i < tallWisps.size(); i++) {
                        float bx = (float)(Math.cos(tWAng[i]) * tWR[i]);
                        float bz = (float)(Math.sin(tWAng[i]) * tWR[i]);
                        tallWisps.get(i).animateTo(
                                new Vector3f(bx - scale * 0.5f, (float)tWBaseY[i], bz - scale * 0.5f),
                                new AxisAngle4f((float)(tick * 0.03 + i), 0, 1, 0),
                                new Vector3f(scale, scale * 4.0f, scale), 4);
                    }
                }
            } else if (dissipatePhase) {
                // Fog rises Y+8 and dissipates (items animateTo Y+8 while shrinking)
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = (1f - t01) * 0.4f;
                float riseY = 0.35f + t01 * 8.0f;
                float tallRiseY = 0.0f;
                if (tick % 2 == 0) {
                    for (int i = 0; i < wisps.size(); i++) {
                        wAng[i] += 0.012;
                        float bx = (float)(Math.cos(wAng[i]) * wR[i]);
                        float bz = (float)(Math.sin(wAng[i]) * wR[i]);
                        wisps.get(i).animateTo(
                                new Vector3f(bx - scale * 1.75f, riseY, bz - scale * 1.75f),
                                new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                                new Vector3f(scale * 3.5f, scale * 0.375f, scale * 3.5f), 2);
                    }
                    for (int i = 0; i < tallWisps.size(); i++) {
                        float bx = (float)(Math.cos(tWAng[i]) * tWR[i]);
                        float bz = (float)(Math.sin(tWAng[i]) * tWR[i]);
                        tallRiseY = (float)tWBaseY[i] + t01 * 8.0f;
                        tallWisps.get(i).animateTo(
                                new Vector3f(bx - scale * 0.5f, tallRiseY, bz - scale * 0.5f),
                                new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                                new Vector3f(scale, scale * 4.0f, scale), 2);
                    }
                }
            } else {
                // Active — items swirl in slow circular pattern at their level
                if (tick % 2 == 0) {
                    for (int i = 0; i < wisps.size(); i++) {
                        wAng[i] += 0.012;
                        double bobR = wR[i] + Math.sin(tick * 0.03 + wPhase[i]) * 0.4;
                        double bobY = 0.35 + Math.sin(tick * 0.06 + wPhase[i]) * 0.1;
                        float bx = (float)(Math.cos(wAng[i]) * bobR);
                        float bz = (float)(Math.sin(wAng[i]) * bobR);
                        wisps.get(i).animateTo(
                                new Vector3f(bx - 0.7f, (float)bobY, bz - 0.7f),
                                new AxisAngle4f((float)(tick * 0.015 + i), 0, 1, 0),
                                new Vector3f(1.4f, 0.15f, 1.4f), 4);
                    }
                    for (int i = 0; i < tallWisps.size(); i++) {
                        double swirlAng = tWAng[i] + tick * 0.02;
                        double bobY = tWBaseY[i] + Math.sin(tick * 0.05 + i) * 0.4;
                        float bx = (float)(Math.cos(swirlAng) * tWR[i]);
                        float bz = (float)(Math.sin(swirlAng) * tWR[i]);
                        tallWisps.get(i).animateTo(
                                new Vector3f(bx - 0.2f, (float)bobY, bz - 0.2f),
                                new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0),
                                new Vector3f(0.4f, 1.6f, 0.4f), 4);
                    }
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // SCULK_SOUL rings rippling outward
                if (tick % 4 == 0) {
                    double r = (tick % 40) * 0.2;
                    for (int i = 0; i < 16; i++) {
                        double a = Math.PI * 2 * i / 16;
                        Location p = c.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                        w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
                if (tick % 3 == 0) for (int i = 0; i < 10; i++) {
                    double a = Math.PI * 2 * i / 10 + tick * 0.05;
                    Location p = c.clone().add(Math.cos(a) * 5.0, 0.5, Math.sin(a) * 5.0);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.05, 0.1, 0.01);
                }
            } else if (dissipatePhase) {
                // Rising fog wisps as it lifts
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 6.0;
                    int dt = tick - dissipateStart;
                    double yy = 0.5 + (dt / 40.0) * 6.0 + Math.random() * 1.5;
                    Location p = c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.2, 0.1, 0.03);
                    w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
                if (tick % 5 == 0) {
                    Location p = c.clone().add((Math.random() - 0.5) * 6, 1.0 + Math.random() * 2.0, (Math.random() - 0.5) * 6);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.05, 0.05, 0.05, 0.0);
                }
            } else {
                // Active — 3 layered:
                // 1) SCULK_SOUL drift (5/tick)
                for (int i = 0; i < 5; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 7.0;
                    double yy = 0.4 + Math.random() * 1.5;
                    Location p = c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                    w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.15, 0.1, 0.15, 0.01);
                }
                // 2) CLOUD ground hugger
                for (int i = 0; i < 4; i++) {
                    double a = tick * 0.08 + Math.PI * 2 * i / 4;
                    double r = 1.5 + Math.sin(tick * 0.04 + i) * 0.6;
                    Location p = c.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.15, 0.05, 0.15, 0.02);
                }
                // 3) SOUL_FIRE_FLAME occasional flicker
                if (tick % 12 == 0) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 6.5;
                    Location p = c.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.1, 0.2, 0.1, 0.01);
                }
                // 4) Cold-blue DUST halo
                if (tick % 6 == 0) for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16 + tick * 0.01;
                    Location p = c.clone().add(Math.cos(a) * 7.8, 0.4, Math.sin(a) * 7.8);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.2f);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.5f, 1.0f);
                if (tick % 12 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.5f, 0.4f);
            } else if (dissipatePhase) {
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.4f);
                if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.3f, 0.5f + (float)Math.random() * 0.2f);
            } else {
                // Active — AMBIENT_CAVE quiet + ENTITY_GLOW_SQUID_AMBIENT pitch 0.4
                if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.4f, 1.0f);
                if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ProwlingColdFog(plugin); }
    }

    // ================================================================
    // 13. HUNTING BLIZZARD POCKET — FOLLOW-AI 0.10.
    //     Spawn: 30 SNOWBALL form swirling sphere (vortex pattern, scale 0->0.5).
    //     Active: chases player aggressively; orbital 3-axis spiral 0.08 rad/tick;
    //             ELECTRIC_SPARK at intersections, CLOUD trails, SNOWFLAKE rain.
    //     Dissipate: spin accelerates to 0.2 rad/tick, items spiral outward fading.
    // ================================================================
    public static class HuntingBlizzardPocket extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flakes = new ArrayList<>();
        private final double[] sAng = new double[30];
        private final double[] sAng2 = new double[30];
        private final double[] sR = new double[30];

        public HuntingBlizzardPocket(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunting_blizzard_pocket", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3120.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.10);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 0.8f);

            for (int i = 0; i < 30; i++) {
                sAng[i] = Math.PI * 2 * i / 30;
                sAng2[i] = Math.acos(1 - 2.0 * ((i + 0.5) / 30.0));
                sR[i] = 2.5 + Math.random() * 1.5;
                double bx = Math.sin(sAng2[i]) * Math.cos(sAng[i]) * sR[i];
                double bz = Math.sin(sAng2[i]) * Math.sin(sAng[i]) * sR[i];
                double by = 3.0 + Math.cos(sAng2[i]) * sR[i];
                Location p = c.clone().add(bx, by, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.01f, 0.01f, 0.01f).glow(255, 255, 255).interpolation(30, 0);
                flakes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 30;
            boolean dissipatePhase = tick >= dissipateStart;

            // --- ItemDisplay choreography ---
            if (spawnPhase) {
                // vortex pattern, scale 0 -> 0.5
                float progress = Math.min(1f, tick / 30f);
                float scale = 0.5f * progress;
                for (int i = 0; i < flakes.size(); i++) {
                    sAng[i] += 0.08; // vortex spin during spawn
                    double bx = Math.sin(sAng2[i]) * Math.cos(sAng[i]) * sR[i];
                    double bz = Math.sin(sAng2[i]) * Math.sin(sAng[i]) * sR[i];
                    double by = 3.0 + Math.cos(sAng2[i]) * sR[i];
                    flakes.get(i).animateTo(
                            new Vector3f((float)bx - scale * 0.5f, (float)by, (float)bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.2 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                }
            } else if (dissipatePhase) {
                // Accelerate spin to 0.2 rad/tick, items spiral outward fading
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = 0.5f * (1f - t01);
                float rExpand = 1f + 4.0f * t01; // outward spiral
                for (int i = 0; i < flakes.size(); i++) {
                    sAng[i] += 0.20;
                    double rr = sR[i] * rExpand;
                    double bx = Math.sin(sAng2[i]) * Math.cos(sAng[i]) * rr;
                    double bz = Math.sin(sAng2[i]) * Math.sin(sAng[i]) * rr;
                    double by = 3.0 + Math.cos(sAng2[i]) * rr;
                    flakes.get(i).animateTo(
                            new Vector3f((float)bx - scale * 0.5f, (float)by, (float)bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.5 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                }
            } else {
                // Active — 3-axis orbital spiral 0.08 rad/tick
                for (int i = 0; i < flakes.size(); i++) {
                    sAng[i] += 0.08;
                    sAng2[i] += 0.02 * Math.sin(tick * 0.03 + i);
                    double bx = Math.sin(sAng2[i]) * Math.cos(sAng[i]) * sR[i];
                    double bz = Math.sin(sAng2[i]) * Math.sin(sAng[i]) * sR[i];
                    double by = 3.0 + Math.cos(sAng2[i]) * sR[i];
                    flakes.get(i).animateTo(
                            new Vector3f((float)bx - 0.25f, (float)by, (float)bz - 0.25f),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(0.5f), 2);
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // Vortex forming — ELECTRIC_SPARK + SNOWFLAKE + CLOUD
                for (int i = 0; i < 5; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 4.0;
                    double yy = 1.5 + Math.random() * 3.0;
                    Location p = c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.05);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
            } else if (dissipatePhase) {
                // Dissipate burst — SNOWFLAKE radial + ELECTRIC_SPARK + CLOUD
                if (tick == dissipateStart) {
                    for (int i = 0; i < 40; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 6.0;
                        Location p = c.clone().add(Math.cos(a) * r, 3.0 + (Math.random() - 0.5) * 4.0, Math.sin(a) * r);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 2, 0.3, 0.3, 0.3, 0.15);
                        w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.2, 0.2, 0.1);
                    }
                }
                if (tick % 2 == 0) for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 2.0 + Math.random() * 5.0;
                    Location p = c.clone().add(Math.cos(a) * r, 2.0 + Math.random() * 3.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
            } else {
                // Active — 3 layered:
                // 1) SNOWFLAKE swirl (orbital)
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI * 2 * i / 6) + tick * 0.25;
                    double radius = 3.0 + Math.sin(tick * 0.1 + i) * 0.6;
                    double yy = 3.0 + Math.sin(tick * 0.15 + i) * 2.0;
                    Location p = c.clone().add(Math.cos(a) * radius, yy, Math.sin(a) * radius);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
                // 2) ELECTRIC_SPARK at orbit intersections
                if (tick % 3 == 0) for (int i = 0; i < flakes.size(); i += 5) {
                    double bx = Math.sin(sAng2[i]) * Math.cos(sAng[i]) * sR[i];
                    double bz = Math.sin(sAng2[i]) * Math.sin(sAng[i]) * sR[i];
                    double by = 3.0 + Math.cos(sAng2[i]) * sR[i];
                    Location p = c.clone().add(bx, by, bz);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
                // 3) CLOUD trails per item
                if (tick % 2 == 0) for (int i = 0; i < flakes.size(); i += 3) {
                    double bx = Math.sin(sAng2[i]) * Math.cos(sAng[i]) * sR[i];
                    double bz = Math.sin(sAng2[i]) * Math.sin(sAng[i]) * sR[i];
                    double by = 3.0 + Math.cos(sAng2[i]) * sR[i];
                    Location p = c.clone().add(bx, by, bz);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
                // 4) Ice blue dust outline
                if (tick % 3 == 0) for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12 + tick * 0.15;
                    double yy = 2.5 + Math.sin(tick * 0.1 + i) * 0.8;
                    Location p = c.clone().add(Math.cos(a) * 3.2, yy, Math.sin(a) * 3.2);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 200, 230, 255, 1.3f);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.7f, 1.2f);
            } else if (dissipatePhase) {
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);
            } else {
                // Active — ITEM_TRIDENT_RIPTIDE_2 looped pitch 1.2 + BLOCK_AMETHYST_BLOCK_HIT during chase
                if (tick % 20 == 0) DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.6f, 1.2f);
                if (tick % 14 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 0.9f + (float)Math.random() * 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new HuntingBlizzardPocket(plugin); }
    }

    // ================================================================
    // 14. SLOW FROST BREATH —
    //     Spawn: 15 GLASS_BOTTLE form cone at one arena edge (scale 0->0.6).
    //     Active: cone "breathes" forward, animateTo +radius over 100t; DUST fill,
    //             SNOWFLAKE shedding, ELECTRIC_SPARK at cone tip.
    //     Dissipate: cone contracts back inward, fades.
    // ================================================================
    public static class SlowFrostBreath extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bottles = new ArrayList<>();
        private double breathAng;
        private final double[] bDist = new double[15];
        private final double[] bSide = new double[15];
        private final double[] bY = new double[15];

        public SlowFrostBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slow_frost_breath", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2880.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(320);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.0f, 0.8f);

            breathAng = Math.random() * Math.PI * 2;
            double cosA = Math.cos(breathAng);
            double sinA = Math.sin(breathAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // 15 GLASS_BOTTLE — initial cone tight at edge, pointing outward
            for (int i = 0; i < 15; i++) {
                bDist[i] = 0.5 + (i / 15.0) * 1.0; // tight near edge initially
                double coneWidth = 0.3 + (i / 15.0) * 0.6;
                bSide[i] = (Math.random() - 0.5) * 2 * coneWidth;
                bY[i] = 1.5 + (Math.random() - 0.5) * 1.0;
                double bx = cosA * bDist[i] + pCosA * bSide[i];
                double bz = sinA * bDist[i] + pSinA * bSide[i];
                Location p = c.clone().add(bx, bY[i], bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 230, 255).interpolation(30, 0);
                bottles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 30;
            boolean dissipatePhase = tick >= dissipateStart;

            double cosA = Math.cos(breathAng);
            double sinA = Math.sin(breathAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // Compute breath progress (0->1 over 100t) during active phase
            // Active phase = [30, dissipateStart)
            float breathProgress = 0f;
            int activeLen = dissipateStart - 30;
            if (!spawnPhase && !dissipatePhase) {
                int activeTick = tick - 30;
                breathProgress = Math.min(1f, activeTick / 100f);
            } else if (dissipatePhase) {
                // During dissipate, contract back
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                breathProgress = Math.max(0f, 1f - t01);
            }

            if (spawnPhase) {
                // Materialize: scale 0 -> 0.6 at tight cone
                float progress = Math.min(1f, tick / 30f);
                float scale = 0.6f * progress;
                for (int i = 0; i < bottles.size(); i++) {
                    double yBob = bY[i] + Math.sin(tick * 0.05 + i) * 0.15;
                    float bx = (float)(cosA * bDist[i] + pCosA * bSide[i]);
                    float bz = (float)(sinA * bDist[i] + pSinA * bSide[i]);
                    bottles.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, (float)yBob, bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(scale), 4);
                }
            } else if (dissipatePhase) {
                // Contract back inward + fade
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = 0.6f * (1f - t01);
                for (int i = 0; i < bottles.size(); i++) {
                    double drift = bDist[i] + breathProgress * 6.5;
                    double sideDrift = bSide[i] + bSide[i] * breathProgress * 2.5;
                    double yBob = bY[i] + Math.sin(tick * 0.06 + i) * 0.2;
                    float bx = (float)(cosA * drift + pCosA * sideDrift);
                    float bz = (float)(sinA * drift + pSinA * sideDrift);
                    bottles.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, (float)yBob, bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.06 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                }
            } else {
                // Active — cone breathes forward: items animateTo +radius over 100t
                for (int i = 0; i < bottles.size(); i++) {
                    double drift = bDist[i] + breathProgress * 6.5;
                    double sideDrift = bSide[i] + bSide[i] * breathProgress * 2.5 + Math.sin(tick * 0.05 + i * 0.7) * 0.2;
                    double yBob = bY[i] + Math.sin(tick * 0.06 + i) * 0.25;
                    float bx = (float)(cosA * drift + pCosA * sideDrift);
                    float bz = (float)(sinA * drift + pSinA * sideDrift);
                    bottles.get(i).animateTo(
                            new Vector3f(bx - 0.3f, (float)yBob, bz - 0.3f),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.6f), 4);
                }
            }

            // Effective cone front based on breathProgress
            double frontDist = 0.5 + breathProgress * 7.0;
            double frontWidth = 0.4 + breathProgress * 2.2;

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // Cone materializing — DUST cyan + CLOUD + SNOWFLAKE flurry near edge
                for (int i = 0; i < 5; i++) {
                    double d = 0.5 + Math.random() * 1.5;
                    double side = (Math.random() - 0.5) * 1.2;
                    double yy = 1.5 + (Math.random() - 0.5) * 1.4;
                    Location p = c.clone().add(cosA * d + pCosA * side, yy, sinA * d + pSinA * side);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.3f);
                    if (i % 2 == 0) w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                    if (i % 3 == 0) w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
            } else if (dissipatePhase) {
                // Receding wisps along cone
                for (int i = 0; i < 4; i++) {
                    double d = Math.random() * Math.max(0.5, frontDist);
                    double curW = 0.4 + (d / Math.max(1.0, frontDist)) * frontWidth;
                    double side = (Math.random() - 0.5) * 2 * curW;
                    double yy = 1.5 + (Math.random() - 0.5) * 1.4;
                    Location p = c.clone().add(cosA * d + pCosA * side, yy, sinA * d + pSinA * side);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.01);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.1f);
                }
            } else {
                // Active — 3 layered patterns:
                // 1) DUST cyan trail (cone fill)
                for (int i = 0; i < 10; i++) {
                    double d = 0.5 + Math.random() * frontDist;
                    double curW = 0.4 + (d / Math.max(1.0, frontDist)) * frontWidth;
                    double side = (Math.random() - 0.5) * 2 * curW;
                    double yy = 1.5 + (Math.random() - 0.5) * 1.4;
                    Location p = c.clone().add(cosA * d + pCosA * side, yy, sinA * d + pSinA * side);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.1f);
                }
                // 2) SNOWFLAKE shedding
                if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                    double d = 0.5 + Math.random() * frontDist;
                    double curW = 0.4 + (d / Math.max(1.0, frontDist)) * frontWidth;
                    double side = (Math.random() - 0.5) * 2 * curW;
                    double yy = 1.5 + (Math.random() - 0.5) * 1.4;
                    Location p = c.clone().add(cosA * d + pCosA * side, yy, sinA * d + pSinA * side);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
                // 3) ELECTRIC_SPARK at cone tip
                if (tick % 3 == 0) for (int i = 0; i < 4; i++) {
                    double side = (Math.random() - 0.5) * 2 * frontWidth;
                    double yy = 1.5 + (Math.random() - 0.5) * 1.2;
                    Location p = c.clone().add(cosA * frontDist + pCosA * side, yy, sinA * frontDist + pSinA * side);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
                // 4) CLOUD fill
                if (tick % 2 == 0) for (int i = 0; i < 3; i++) {
                    double d = 0.5 + Math.random() * frontDist;
                    double curW = 0.4 + (d / Math.max(1.0, frontDist)) * frontWidth;
                    double side = (Math.random() - 0.5) * 2 * curW;
                    double yy = 1.5 + (Math.random() - 0.5) * 1.4;
                    Location p = c.clone().add(cosA * d + pCosA * side, yy, sinA * d + pSinA * side);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.03);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                if (tick % 5 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 0.5f, 0.8f);
            } else if (dissipatePhase) {
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.0f, 0.8f);
                if (tick % 10 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.4f, 0.9f);
            } else {
                // Active — WEATHER_RAIN reverse pitched 0.6 + BLOCK_POWDER_SNOW_PLACE soft
                if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.6f, 0.6f);
                if (tick % 18 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 0.4f, 0.9f);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                // Damage anyone within the cone (wedge check based on breath progress)
                double damageFront = Math.max(0.5, frontDist);
                double damageWidth = Math.max(0.4, frontWidth + 0.2);
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    double axis = dx * cosA + dz * sinA;
                    double perp = dx * pCosA + dz * pSinA;
                    if (axis < 0.3 || axis > damageFront) continue;
                    double curW = 0.4 + (axis / damageFront) * damageWidth;
                    if (Math.abs(perp) <= curW) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SlowFrostBreath(plugin); }
    }

    // ================================================================
    // 15. WANDERING HOARFROST HAZE —
    //     Spawn: 18 QUARTZ in irregular cluster (scale 0->0.5).
    //     Active: each random-walks via small animateTo offsets (±2 every 20t);
    //             DUST cyan haze, SCULK_SOUL between items, occasional GLOW.
    //     Dissipate: scatter outward randomly fading.
    // ================================================================
    public static class WanderingHoarfrostHaze extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] hX = new double[18];
        private final double[] hZ = new double[18];
        private final double[] hY = new double[18];
        private final double[] hTargetX = new double[18];
        private final double[] hTargetZ = new double[18];
        private final double[] hPhase = new double[18];
        private final double[] hScatterDx = new double[18];
        private final double[] hScatterDz = new double[18];

        public WanderingHoarfrostHaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wandering_hoarfrost_haze", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2400.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 0.9f);

            // 18 QUARTZ in irregular cluster
            for (int i = 0; i < 18; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1.5 + Math.random() * 4.5;
                hX[i] = Math.cos(a) * r;
                hZ[i] = Math.sin(a) * r;
                hY[i] = 1.0 + Math.random() * 3.0;
                hTargetX[i] = hX[i];
                hTargetZ[i] = hZ[i];
                hPhase[i] = Math.random() * Math.PI * 2;
                // Dissipate scatter direction
                double da = Math.random() * Math.PI * 2;
                double dmag = 4.0 + Math.random() * 3.0;
                hScatterDx[i] = Math.cos(da) * dmag;
                hScatterDz[i] = Math.sin(da) * dmag;
                Location p = c.clone().add(hX[i], hY[i], hZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.QUARTZ));
                h.scale(0.01f, 0.01f, 0.01f).glow(240, 250, 255).interpolation(30, 0);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 30;
            boolean dissipatePhase = tick >= dissipateStart;

            if (spawnPhase) {
                // Materialize: scale 0 -> 0.5
                float scale = 0.5f * Math.min(1f, tick / 30f);
                for (int i = 0; i < shards.size(); i++) {
                    double yBob = hY[i] + Math.sin(tick * 0.06 + hPhase[i]) * 0.15;
                    shards.get(i).animateTo(
                            new Vector3f((float)hX[i] - scale / 2, (float)yBob, (float)hZ[i] - scale / 2),
                            new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(scale), 4);
                }
            } else if (dissipatePhase) {
                // Scatter randomly outward fading
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = 0.5f * (1f - t01);
                for (int i = 0; i < shards.size(); i++) {
                    double targetX = hX[i] + hScatterDx[i];
                    double targetZ = hZ[i] + hScatterDz[i];
                    double curX = hX[i] + (targetX - hX[i]) * t01;
                    double curZ = hZ[i] + (targetZ - hZ[i]) * t01;
                    double yBob = hY[i] + Math.sin(tick * 0.08 + hPhase[i]) * 0.3 + t01 * 1.0;
                    shards.get(i).animateTo(
                            new Vector3f((float)curX - scale / 2, (float)yBob, (float)curZ - scale / 2),
                            new AxisAngle4f((float)(tick * 0.2 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                }
            } else {
                // Active — random walk via small animateTo offsets (±2 every 20t)
                if (tick % 20 == 0) {
                    for (int i = 0; i < shards.size(); i++) {
                        hTargetX[i] = hX[i] + (Math.random() - 0.5) * 4.0;
                        hTargetZ[i] = hZ[i] + (Math.random() - 0.5) * 4.0;
                        // Keep inside radius
                        double rr = hTargetX[i] * hTargetX[i] + hTargetZ[i] * hTargetZ[i];
                        if (rr > 49) {
                            double s = 7.0 / Math.sqrt(rr);
                            hTargetX[i] *= s;
                            hTargetZ[i] *= s;
                        }
                    }
                }
                // Smooth interpolate toward target
                for (int i = 0; i < shards.size(); i++) {
                    hX[i] += (hTargetX[i] - hX[i]) * 0.05;
                    hZ[i] += (hTargetZ[i] - hZ[i]) * 0.05;
                    double yBob = hY[i] + Math.sin(tick * 0.06 + hPhase[i]) * 0.3;
                    shards.get(i).animateTo(
                            new Vector3f((float)hX[i] - 0.25f, (float)yBob, (float)hZ[i] - 0.25f),
                            new AxisAngle4f((float)(tick * 0.08 + i), 0, 1, 0),
                            new Vector3f(0.5f), 4);
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // Spawn flourish: DUST burst halo + SCULK_SOUL inward
                if (tick % 3 == 0) for (int i = 0; i < 10; i++) {
                    double a = Math.PI * 2 * i / 10 + tick * 0.06;
                    Location p = c.clone().add(Math.cos(a) * 6.0, 2.0, Math.sin(a) * 6.0);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.3f);
                }
                if (tick % 4 == 0) for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.0;
                    Location p = c.clone().add(Math.cos(a) * r, 1.0 + Math.random() * 3.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.0);
                }
            } else if (dissipatePhase) {
                // Scatter trails
                for (int i = 0; i < shards.size(); i += 2) {
                    Location p = c.clone().add(hX[i], hY[i], hZ[i]);
                    w.spawnParticle(Particle.GLOW, p, 1, 0.15, 0.15, 0.15, 0);
                    w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.2, 0.2, 0.2, 0.02);
                }
                if (tick % 3 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 4.0 + Math.random() * 4.0;
                    Location p = c.clone().add(Math.cos(a) * r, 1.0 + Math.random() * 4.0, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.2f);
                }
            } else {
                // Active — 3 layered:
                // 1) DUST cyan haze around cluster
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 6.5;
                    Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 4.0, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.0f);
                }
                // 2) SCULK_SOUL between items
                if (tick % 2 == 0) for (int i = 0; i < 3; i++) {
                    int idx = (tick + i * 5) % shards.size();
                    Location p = c.clone().add(hX[idx], hY[idx] + 0.2, hZ[idx]);
                    w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.2, 0.2, 0.2, 0.02);
                }
                // 3) Occasional GLOW
                if (tick % 8 == 0) {
                    int idx = (int)(Math.random() * shards.size());
                    Location p = c.clone().add(hX[idx], hY[idx] + 0.3, hZ[idx]);
                    w.spawnParticle(Particle.GLOW, p, 2, 0.3, 0.3, 0.3, 0);
                }
                // 4) Snowflake sparse drift
                if (tick % 3 == 0) for (int i = 0; i < 2; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 6.0;
                    Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 4.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 1.2f);
            } else if (dissipatePhase) {
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.2f);
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.0f + (float)Math.random() * 0.4f);
            } else {
                // Active — ENTITY_GLOW_SQUID_AMBIENT distant random + BLOCK_AMETHYST_BLOCK_HIT occasional
                if (tick % 35 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.5f, 0.9f + (float)Math.random() * 0.4f);
                if (tick % 25 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.4f, 1.0f + (float)Math.random() * 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new WanderingHoarfrostHaze(plugin); }
    }

    // ================================================================
    // 16. CREEPING RIME BANK —
    //     Spawn: 24 BLUE_ICE form flat bar wall (4x6 grid) at arena edge (scale 0->0.7).
    //     Active: wall slides perpendicular across arena over duration;
    //             DUST trail bottom, CLOUD wisps off top, ELECTRIC_SPARK at edges.
    //     Dissipate: wall topples forward (rotates X-axis 90° + Y velocity) shatters.
    // ================================================================
    public static class CreepingRimeBank extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tiles = new ArrayList<>();
        private double slideAng;
        // 4 columns wide x 6 rows tall = 24
        private final double[] tSide = new double[24];
        private final double[] tHeight = new double[24];
        private double startOff;
        private double endOff;

        public CreepingRimeBank(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_rime_bank", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3120.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(320);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // SPAWN intro: staggered BLOCK_GLASS_PLACE ambient
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.9f);

            slideAng = Math.random() * Math.PI * 2;
            startOff = -10.0;
            endOff = 10.0;
            double cosA = Math.cos(slideAng);
            double sinA = Math.sin(slideAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // 4 columns wide x 6 rows tall = 24 BLUE_ICE tiles forming wall
            int idx = 0;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 4; col++) {
                    tSide[idx] = -3.0 + col * 2.0; // 4 columns spanning ~6 wide
                    tHeight[idx] = 0.5 + row * 1.0; // 6 rows from y=0.5 to y=5.5
                    double bx = cosA * startOff + pCosA * tSide[idx];
                    double bz = sinA * startOff + pSinA * tSide[idx];
                    Location p = c.clone().add(bx, tHeight[idx], bz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                    h.scale(0.01f, 0.01f, 0.01f).glow(150, 200, 255).interpolation(30, 0);
                    tiles.add(h);
                    spawnedEntities.add(h.entity());
                    idx++;
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 30;
            boolean dissipatePhase = tick >= dissipateStart;

            double cosA = Math.cos(slideAng);
            double sinA = Math.sin(slideAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // Slide progress: linear across active phase
            int activeLen = dissipateStart - 30;
            double slideOff;
            if (spawnPhase) {
                slideOff = startOff;
            } else if (dissipatePhase) {
                slideOff = endOff;
            } else {
                double t = (tick - 30) / (double) Math.max(1, activeLen);
                slideOff = startOff + (endOff - startOff) * t;
            }

            if (spawnPhase) {
                // scale 0 -> 0.7 staggered
                for (int i = 0; i < tiles.size(); i++) {
                    int edgeDist = Math.min(i, tiles.size() - 1 - i);
                    int stagger = edgeDist;
                    int localTick = tick - stagger;
                    float scale = 0f;
                    if (localTick > 0) scale = 0.7f * Math.min(1f, localTick / 20f);
                    float bx = (float)(cosA * slideOff + pCosA * tSide[i]);
                    float bz = (float)(sinA * slideOff + pSinA * tSide[i]);
                    tiles.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, (float)tHeight[i], bz - scale * 0.5f),
                            new AxisAngle4f((float)slideAng, 0, 1, 0),
                            new Vector3f(scale), 4);
                }
            } else if (dissipatePhase) {
                // Topples forward — rotate X-axis 90° + Y velocity (rising then falling)
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = 0.7f * (1f - t01);
                float tipAngle = (float)(Math.PI / 2) * t01;
                // Y arc: rises then falls
                float yArc = (float)(Math.sin(Math.PI * t01) * 1.5);
                // Rotation axis is the cross axis (perpendicular to slide direction in XZ plane)
                float ax = (float)Math.cos(slideAng + Math.PI / 2);
                float az = (float)Math.sin(slideAng + Math.PI / 2);
                // Forward translation as it topples
                float forward = t01 * 1.5f;
                for (int i = 0; i < tiles.size(); i++) {
                    float bx = (float)(cosA * (slideOff + forward) + pCosA * tSide[i]);
                    float bz = (float)(sinA * (slideOff + forward) + pSinA * tSide[i]);
                    float fallY = (float)(tHeight[i] + yArc - t01 * tHeight[i] * 0.7);
                    tiles.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, fallY, bz - scale * 0.5f),
                            new AxisAngle4f(tipAngle, ax, 0, az),
                            new Vector3f(scale), 2);
                }
            } else {
                // Active — wall slides perpendicular smoothly with subtle bob
                for (int i = 0; i < tiles.size(); i++) {
                    double yBob = tHeight[i] + Math.sin(tick * 0.08 + i * 0.4) * 0.08;
                    float bx = (float)(cosA * slideOff + pCosA * tSide[i]);
                    float bz = (float)(sinA * slideOff + pSinA * tSide[i]);
                    tiles.get(i).animateTo(
                            new Vector3f(bx - 0.35f, (float)yBob, bz - 0.35f),
                            new AxisAngle4f((float)slideAng, 0, 1, 0),
                            new Vector3f(0.7f), 4);
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // Wall materializing — DUST + SNOWFLAKE rising flurry
                if (tick % 2 == 0) for (int i = 0; i < 8; i++) {
                    double side = -3.5 + Math.random() * 7.0;
                    double bx = cosA * slideOff + pCosA * side;
                    double bz = sinA * slideOff + pSinA * side;
                    Location p = c.clone().add(bx, 0.5 + Math.random() * 5.0, bz);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.3f);
                    if (i % 2 == 0) w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.05, 0.05, 0.05, 0.0);
                    if (i % 3 == 0) w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.01);
                }
            } else if (dissipatePhase) {
                // Topple shatter cascade — SNOWFLAKE + ELECTRIC_SPARK + CLOUD burst
                if (tick == dissipateStart) {
                    for (int i = 0; i < 40; i++) {
                        double side = -3.5 + Math.random() * 7.0;
                        double bx = cosA * slideOff + pCosA * side;
                        double bz = sinA * slideOff + pSinA * side;
                        Location p = c.clone().add(bx, 0.5 + Math.random() * 5.0, bz);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 3, 0.4, 0.3, 0.4, 0.2);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.3, 0.2, 0.3, 0.1);
                        w.spawnParticle(Particle.CLOUD, p, 2, 0.3, 0.2, 0.3, 0.1);
                    }
                }
                if (tick % 3 == 0) for (int i = 0; i < 5; i++) {
                    double side = -3.5 + Math.random() * 7.0;
                    double bx = cosA * slideOff + pCosA * side;
                    double bz = sinA * slideOff + pSinA * side;
                    Location p = c.clone().add(bx, 0.5 + Math.random() * 4.0, bz);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.05);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.5f);
                }
            } else {
                // Active — 3 layered:
                // 1) DUST trail along bottom
                for (int i = 0; i < 6; i++) {
                    double side = -3.5 + Math.random() * 7.0;
                    double bx = cosA * slideOff + pCosA * side;
                    double bz = sinA * slideOff + pSinA * side;
                    Location p = c.clone().add(bx, 0.3 + Math.random() * 0.4, bz);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.2f);
                }
                // 2) CLOUD wisps off the wall top
                if (tick % 2 == 0) for (int i = 0; i < 5; i++) {
                    double side = -3.5 + Math.random() * 7.0;
                    double bx = cosA * slideOff + pCosA * side;
                    double bz = sinA * slideOff + pSinA * side;
                    Location p = c.clone().add(bx, 5.5 + Math.random() * 1.0, bz);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.1, 0.2, 0.04);
                }
                // 3) ELECTRIC_SPARK at wall edges (column 0 and column 3)
                if (tick % 3 == 0) for (int row = 0; row < 6; row++) {
                    for (int edge = 0; edge < 2; edge++) {
                        double side = edge == 0 ? -3.0 : 3.0;
                        double yy = 0.5 + row * 1.0 + (Math.random() - 0.5) * 0.4;
                        Location p = c.clone().add(cosA * slideOff + pCosA * side, yy, sinA * slideOff + pSinA * side);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.02);
                    }
                }
                // 4) FALLING_DUST(BLUE_ICE) shedding
                if (tick % 4 == 0) for (int i = 0; i < 4; i++) {
                    double side = -3.5 + Math.random() * 7.0;
                    double bx = cosA * slideOff + pCosA * side;
                    double bz = sinA * slideOff + pSinA * side;
                    Location p = c.clone().add(bx, 1.0 + Math.random() * 4.0, bz);
                    w.spawnParticle(Particle.BLOCK, p, 1, 0.1, 0.05, 0.1, 0.02, Material.BLUE_ICE.createBlockData());
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                // BLOCK_GLASS_PLACE staggered ambient
                if (tick % 4 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.6f, 0.7f + (float)Math.random() * 0.4f);
            } else if (dissipatePhase) {
                // BLOCK_GLASS_BREAK cascade on topple
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.7f);
                if (tick % 4 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.8f + (float)Math.random() * 0.4f);
            } else {
                // BLOCK_GLASS_HIT during slide
                if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.5f, 0.8f + (float)Math.random() * 0.3f);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                // Damage anyone within the slide bar (perpendicular band)
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    double axis = dx * cosA + dz * sinA;
                    double perp = dx * pCosA + dz * pSinA;
                    if (Math.abs(axis - slideOff) <= 1.2 && Math.abs(perp) <= 4.0) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CreepingRimeBank(plugin); }
    }

    // ================================================================
    // 17. STALKING CHILL VEIL —
    //     Spawn: 20 PHANTOM_MEMBRANE vertical curtain (4 wide x 5 tall, scale 0->0.5)
    //             with shimmer.
    //     Active: curtain slowly translates forward; items wave-bob; SCULK_SOUL,
    //             CLOUD, GLOW shimmer.
    //     Dissipate: curtain tears apart, items scatter randomly while shrinking.
    // ================================================================
    public static class StalkingChillVeil extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> veil = new ArrayList<>();
        private double slideAng;
        private double startOff;
        private double endOff;
        // 4 wide x 5 tall = 20
        private final double[] vSide = new double[20];
        private final double[] vY = new double[20];
        private final double[] vScatterDx = new double[20];
        private final double[] vScatterDz = new double[20];

        public StalkingChillVeil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stalking_chill_veil", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3360.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(320);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.8f, 0.6f);

            slideAng = Math.random() * Math.PI * 2;
            startOff = -8.0;
            endOff = 8.0;

            double cosA = Math.cos(slideAng);
            double sinA = Math.sin(slideAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // 4 wide x 5 tall = 20 PHANTOM_MEMBRANE
            for (int i = 0; i < 20; i++) {
                int col = i % 4;
                int row = i / 4;
                vSide[i] = -3.0 + col * 2.0;
                vY[i] = 0.6 + row * 1.0;
                // Dissipate scatter direction
                double da = Math.random() * Math.PI * 2;
                double dmag = 3.0 + Math.random() * 2.5;
                vScatterDx[i] = Math.cos(da) * dmag;
                vScatterDz[i] = Math.sin(da) * dmag;
                double bx = cosA * startOff + pCosA * vSide[i];
                double bz = sinA * startOff + pSinA * vSide[i];
                Location p = c.clone().add(bx, vY[i], bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.01f, 0.01f, 0.01f).glow(120, 140, 200).interpolation(30, 0);
                veil.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 30;
            boolean dissipatePhase = tick >= dissipateStart;

            double cosA = Math.cos(slideAng);
            double sinA = Math.sin(slideAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // Translation progress
            int activeLen = dissipateStart - 30;
            double slideOff;
            if (spawnPhase) {
                slideOff = startOff;
            } else if (dissipatePhase) {
                slideOff = endOff;
            } else {
                double t = (tick - 30) / (double) Math.max(1, activeLen);
                slideOff = startOff + (endOff - startOff) * t;
            }

            if (spawnPhase) {
                // scale 0 -> 0.5 with shimmer
                float progress = Math.min(1f, tick / 30f);
                float scale = 0.5f * progress;
                for (int i = 0; i < veil.size(); i++) {
                    float shimmerScale = scale * (0.9f + (float)Math.sin(tick * 0.4 + i) * 0.1f);
                    float bx = (float)(cosA * slideOff + pCosA * vSide[i]);
                    float bz = (float)(sinA * slideOff + pSinA * vSide[i]);
                    veil.get(i).animateTo(
                            new Vector3f(bx - shimmerScale * 1.1f, (float)vY[i], bz - shimmerScale * 0.15f),
                            new AxisAngle4f((float)slideAng, 0, 1, 0),
                            new Vector3f(shimmerScale * 2.2f, shimmerScale * 2.2f, shimmerScale * 0.3f), 4);
                }
            } else if (dissipatePhase) {
                // Curtain tears apart — random scatter while shrinking
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = 0.5f * (1f - t01);
                for (int i = 0; i < veil.size(); i++) {
                    float bx = (float)(cosA * slideOff + pCosA * vSide[i] + vScatterDx[i] * t01);
                    float bz = (float)(sinA * slideOff + pSinA * vSide[i] + vScatterDz[i] * t01);
                    float yy = (float)(vY[i] + (Math.random() - 0.3) * t01 * 2.0);
                    veil.get(i).animateTo(
                            new Vector3f(bx - scale * 1.1f, yy, bz - scale * 0.15f),
                            new AxisAngle4f((float)(slideAng + tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(scale * 2.2f, scale * 2.2f, scale * 0.3f), 2);
                }
            } else {
                // Active — wave-bob Y = sin(index + tick*0.05) * 0.3
                for (int i = 0; i < veil.size(); i++) {
                    double yBob = vY[i] + Math.sin(i + tick * 0.05) * 0.3;
                    double sideBob = vSide[i] + Math.sin(tick * 0.04 + i) * 0.1;
                    float bx = (float)(cosA * slideOff + pCosA * sideBob);
                    float bz = (float)(sinA * slideOff + pSinA * sideBob);
                    veil.get(i).animateTo(
                            new Vector3f(bx - 0.55f, (float)yBob, bz - 0.075f),
                            new AxisAngle4f((float)(slideAng + tick * 0.02), 0, 1, 0),
                            new Vector3f(1.1f, 1.1f, 0.15f), 4);
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // Spawn flourish: column of GLOW + SCULK_SOUL rings
                if (tick % 2 == 0) for (int y = 0; y < 5; y++) {
                    Location p = c.clone().add(cosA * slideOff, 0.6 + y * 1.0, sinA * slideOff);
                    w.spawnParticle(Particle.GLOW, p, 2, 1.5, 0.1, 0.3, 0);
                }
                if (tick % 4 == 0) for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8 + tick * 0.05;
                    Location p = c.clone().add(cosA * slideOff + pCosA * Math.cos(a) * 2.5, 2.5, sinA * slideOff + pSinA * Math.cos(a) * 2.5);
                    w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.0);
                }
            } else if (dissipatePhase) {
                // Tearing apart — SCULK_SOUL + CLOUD + GLOW bursts
                if (tick == dissipateStart) {
                    for (int i = 0; i < 30; i++) {
                        double side = -3.5 + Math.random() * 7.0;
                        double yy = 0.4 + Math.random() * 5.0;
                        Location p = c.clone().add(cosA * slideOff + pCosA * side, yy, sinA * slideOff + pSinA * side);
                        w.spawnParticle(Particle.SCULK_SOUL, p, 2, 0.3, 0.3, 0.3, 0.05);
                        w.spawnParticle(Particle.CLOUD, p, 2, 0.3, 0.3, 0.3, 0.05);
                    }
                }
                if (tick % 3 == 0) for (int i = 0; i < 4; i++) {
                    double side = -3.5 + Math.random() * 7.0;
                    double yy = 0.4 + Math.random() * 5.0;
                    Location p = c.clone().add(cosA * slideOff + pCosA * side, yy, sinA * slideOff + pSinA * side);
                    w.spawnParticle(Particle.GLOW, p, 1, 0.2, 0.2, 0.2, 0);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 168, 216, 255, 1.2f);
                }
            } else {
                // Active — 3 layered:
                // 1) SCULK_SOUL through veil
                for (int i = 0; i < 4; i++) {
                    double side = -3.0 + Math.random() * 6.0;
                    double yy = 0.6 + Math.random() * 4.5;
                    Location p = c.clone().add(cosA * slideOff + pCosA * side, yy, sinA * slideOff + pSinA * side);
                    w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.01);
                }
                // 2) CLOUD wisps
                if (tick % 2 == 0) for (int i = 0; i < 3; i++) {
                    double side = -3.5 + Math.random() * 7.0;
                    double yy = 0.5 + Math.random() * 5.0;
                    Location p = c.clone().add(cosA * (slideOff - 0.6) + pCosA * side, yy, sinA * (slideOff - 0.6) + pSinA * side);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
                // 3) GLOW shimmer
                if (tick % 4 == 0) for (int i = 0; i < 3; i++) {
                    double side = -3.0 + Math.random() * 6.0;
                    double yy = 0.6 + Math.random() * 4.5;
                    Location p = c.clone().add(cosA * slideOff + pCosA * side, yy, sinA * slideOff + pSinA * side);
                    w.spawnParticle(Particle.GLOW, p, 1, 0.2, 0.2, 0.2, 0);
                }
                // 4) Cold-blue DUST halo
                if (tick % 3 == 0) for (int i = 0; i < 6; i++) {
                    double side = -3.5 + Math.random() * 7.0;
                    double yy = 0.4 + Math.random() * 5.0;
                    Location p = c.clone().add(cosA * slideOff + pCosA * side, yy, sinA * slideOff + pSinA * side);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 168, 216, 255, 1.2f);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                if (tick % 7 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 0.5f);
            } else if (dissipatePhase) {
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.0f);
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.2f + (float)Math.random() * 0.3f);
            } else {
                // ENTITY_GLOW_SQUID_AMBIENT pitch 0.5 + ENTITY_VEX_AMBIENT distant
                if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.5f, 0.5f);
                if (tick % 45 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.4f, 0.6f);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    double axis = dx * cosA + dz * sinA;
                    double perp = dx * pCosA + dz * pSinA;
                    if (Math.abs(axis - slideOff) <= 0.8 && Math.abs(perp) <= 3.5) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new StalkingChillVeil(plugin); }
    }

    // ================================================================
    // 18. FROZEN GUST FRONT —
    //     Spawn: 28 FEATHER form linear gust front at edge (scale 0->0.4 horizontal line).
    //     Active: gust sweeps across arena; CLOUD trail per feather (3/tick),
    //             ELECTRIC_SPARK at front edge, SNOWFLAKE pickup behind.
    //     Dissipate: feathers scatter upward and dissolve.
    // ================================================================
    public static class FrozenGustFront extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> feathers = new ArrayList<>();
        private double sweepAng;
        private double startOff;
        private double endOff;
        private final double[] fSide = new double[28];
        private final double[] fY = new double[28];
        private final double[] fPhase = new double[28];

        public FrozenGustFront(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_gust_front", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2640.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(340);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1.2f, 1.3f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.0f, 1.0f);

            sweepAng = Math.random() * Math.PI * 2;
            startOff = -9.0;
            endOff = 11.0;

            double cosA = Math.cos(sweepAng);
            double sinA = Math.sin(sweepAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // 28 FEATHER — horizontal line front
            for (int i = 0; i < 28; i++) {
                fSide[i] = -6.5 + (i / 27.0) * 13.0 + (Math.random() - 0.5) * 0.3;
                fY[i] = 1.0 + Math.random() * 2.5;
                fPhase[i] = Math.random() * Math.PI * 2;
                double bx = cosA * startOff + pCosA * fSide[i];
                double bz = sinA * startOff + pSinA * fSide[i];
                Location p = c.clone().add(bx, fY[i], bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.01f, 0.01f, 0.01f).glow(230, 240, 255).interpolation(30, 0);
                feathers.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 30;
            boolean dissipatePhase = tick >= dissipateStart;

            double cosA = Math.cos(sweepAng);
            double sinA = Math.sin(sweepAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // Sweep progress
            int activeLen = dissipateStart - 30;
            double sweepOff;
            if (spawnPhase) {
                sweepOff = startOff;
            } else if (dissipatePhase) {
                sweepOff = endOff;
            } else {
                double t = (tick - 30) / (double) Math.max(1, activeLen);
                sweepOff = startOff + (endOff - startOff) * t;
            }

            if (spawnPhase) {
                // scale 0 -> 0.4
                float scale = 0.4f * Math.min(1f, tick / 30f);
                for (int i = 0; i < feathers.size(); i++) {
                    double yBob = fY[i] + Math.sin(tick * 0.1 + fPhase[i]) * 0.15;
                    float bx = (float)(cosA * sweepOff + pCosA * fSide[i]);
                    float bz = (float)(sinA * sweepOff + pSinA * fSide[i]);
                    feathers.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, (float)yBob, bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.3 + fPhase[i]), 0, 1, 0),
                            new Vector3f(scale), 4);
                }
            } else if (dissipatePhase) {
                // Feathers scatter upward and dissolve
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = 0.4f * (1f - t01);
                for (int i = 0; i < feathers.size(); i++) {
                    double sideBob = fSide[i] + Math.sin(tick * 0.1 + fPhase[i]) * 1.0;
                    double yRise = fY[i] + t01 * 6.0 + Math.sin(tick * 0.15 + i) * 0.5;
                    float bx = (float)(cosA * sweepOff + pCosA * sideBob);
                    float bz = (float)(sinA * sweepOff + pSinA * sideBob);
                    feathers.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, (float)yRise, bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.5 + fPhase[i]), 0, 1, 0),
                            new Vector3f(scale), 2);
                }
            } else {
                // Active — gust sweeps across arena (each feather animateTo end position)
                for (int i = 0; i < feathers.size(); i++) {
                    double sideBob = fSide[i] + Math.sin(tick * 0.1 + fPhase[i]) * 0.4;
                    double yBob = fY[i] + Math.sin(tick * 0.15 + i) * 0.25;
                    float bx = (float)(cosA * sweepOff + pCosA * sideBob);
                    float bz = (float)(sinA * sweepOff + pSinA * sideBob);
                    feathers.get(i).animateTo(
                            new Vector3f(bx - 0.2f, (float)yBob, bz - 0.2f),
                            new AxisAngle4f((float)(tick * 0.3 + fPhase[i]), 0, 1, 0),
                            new Vector3f(0.4f), 4);
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // Front materializing — CLOUD + DUST + SNOWFLAKE flurry
                if (tick % 2 == 0) for (int i = 0; i < 6; i++) {
                    double side = -6.5 + Math.random() * 13.0;
                    double bx = cosA * sweepOff + pCosA * side;
                    double bz = sinA * sweepOff + pSinA * side;
                    Location p = c.clone().add(bx, 1.0 + Math.random() * 3.0, bz);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 220, 240, 255, 1.2f);
                    if (i % 2 == 0) w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
            } else if (dissipatePhase) {
                // Scatter upward dissolving
                for (int i = 0; i < 5; i++) {
                    double side = -6.5 + Math.random() * 13.0;
                    int dt = tick - dissipateStart;
                    double yy = 1.0 + (dt / 40.0) * 5.0 + Math.random() * 2.0;
                    Location p = c.clone().add(cosA * sweepOff + pCosA * side, yy, sinA * sweepOff + pSinA * side);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.2, 0.2, 0.05);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                    if (i % 2 == 0) DisplayBuilder.dustParticles(p, 1, 0.1, 220, 240, 255, 1.0f);
                }
            } else {
                // Active — 3 layered:
                // 1) CLOUD trail per feather (3/tick across the front)
                for (int i = 0; i < feathers.size(); i += 9) {
                    double sideBob = fSide[i] + Math.sin(tick * 0.1 + fPhase[i]) * 0.4;
                    Location p = c.clone().add(cosA * (sweepOff - 0.6) + pCosA * sideBob, fY[i],
                                               sinA * (sweepOff - 0.6) + pSinA * sideBob);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.15, 0.15, 0.15, 0.04);
                }
                // 2) ELECTRIC_SPARK at front edge
                if (tick % 2 == 0) for (int i = 0; i < 5; i++) {
                    double side = -6.5 + Math.random() * 13.0;
                    double bx = cosA * (sweepOff + 0.8) + pCosA * side;
                    double bz = sinA * (sweepOff + 0.8) + pSinA * side;
                    Location p = c.clone().add(bx, 1.0 + Math.random() * 2.5, bz);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
                // 3) SNOWFLAKE pickup behind
                for (int i = 0; i < 5; i++) {
                    double side = -6.5 + Math.random() * 13.0;
                    double bx = cosA * (sweepOff - 1.0) + pCosA * side;
                    double bz = sinA * (sweepOff - 1.0) + pSinA * side;
                    Location p = c.clone().add(bx, 0.5 + Math.random() * 2.5, bz);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.15, 0.15, 0.15, 0.03);
                }
                // 4) Frost dust outline
                if (tick % 3 == 0) for (int i = 0; i < 4; i++) {
                    double side = -6.5 + Math.random() * 13.0;
                    Location p = c.clone().add(cosA * sweepOff + pCosA * side, 1.5 + Math.random() * 1.5,
                                               sinA * sweepOff + pSinA * side);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 220, 240, 255, 1.1f);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.5f, 1.3f);
            } else if (dissipatePhase) {
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.4f);
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.3f + (float)Math.random() * 0.3f);
            } else {
                // Active — ITEM_ELYTRA_FLYING looped quiet pitch 1.3 + WEATHER_RAIN swept
                if (tick % 22 == 0) DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.5f, 1.3f);
                if (tick % 35 == 0) DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.5f, 1.2f);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    double axis = dx * cosA + dz * sinA;
                    double perp = dx * pCosA + dz * pSinA;
                    if (Math.abs(axis - sweepOff) <= 1.5 && Math.abs(perp) <= 7.5) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrozenGustFront(plugin); }
    }

    // ================================================================
    // 19. FROST SPIKE FIELD —
    //     Spawn: 16 PACKED_ICE erupt from ground 4x4 grid (scale 0->1, Y+0 to Y+2 over 30t).
    //     Active: spikes hold + shimmer; cycle up-down by 0.1 every 30t;
    //             ELECTRIC_SPARK between adjacent, FALLING_DUST(BLUE_ICE) from tips, DUST shimmer.
    //     Dissipate: spikes retract into ground (Y+2 to Y-1 over 30t with shrinking scale).
    // ================================================================
    public static class FrostSpikeField extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> spikes = new ArrayList<>();
        private final double[] kX = new double[16];
        private final double[] kZ = new double[16];

        public FrostSpikeField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_spike_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3600.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(320);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.7f);

            // 4x4 grid
            int idx = 0;
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double offX = -4.5 + gx * 3.0 + (Math.random() - 0.5) * 0.6;
                    double offZ = -4.5 + gz * 3.0 + (Math.random() - 0.5) * 0.6;
                    kX[idx] = offX;
                    kZ[idx] = offZ;
                    Location p = c.clone().add(kX[idx], 0.0, kZ[idx]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                    h.scale(0.01f, 0.01f, 0.01f).glow(180, 220, 255).interpolation(40, 0);
                    spikes.add(h);
                    spawnedEntities.add(h.entity());
                    idx++;
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 40;
            boolean dissipatePhase = tick >= dissipateStart;

            if (spawnPhase) {
                // Erupt from ground — scale 0 -> 1, upward animateTo (Y+0 to Y+2 over 30t)
                float progress = Math.min(1f, tick / 30f);
                float scale = progress;
                float yPos = 0.0f + 2.0f * progress;
                for (int i = 0; i < spikes.size(); i++) {
                    float sx = 0.6f * scale;
                    float sy = 2.0f * scale;
                    spikes.get(i).animateTo(
                            new Vector3f((float)kX[i] - sx / 2, yPos - sy / 2, (float)kZ[i] - sx / 2),
                            new AxisAngle4f((float)(i * 0.2), 0, 1, 0),
                            new Vector3f(sx, sy, sx), 4);
                }
            } else if (dissipatePhase) {
                // Retract into ground — Y+2 to Y-1 over 30t (use first 30t of 40t window)
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 30f);
                float scale = 1f - t01;
                float yPos = 2.0f - 3.0f * t01; // 2 -> -1
                for (int i = 0; i < spikes.size(); i++) {
                    float sx = 0.6f * Math.max(0.01f, scale);
                    float sy = 2.0f * Math.max(0.01f, scale);
                    spikes.get(i).animateTo(
                            new Vector3f((float)kX[i] - sx / 2, yPos - sy / 2, (float)kZ[i] - sx / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i * 0.2), 0, 1, 0),
                            new Vector3f(sx, sy, sx), 2);
                }
            } else {
                // Active — hold + shimmer, cycle up-down by 0.1 every 30t
                float cycle = (float)(Math.sin(tick * (Math.PI / 30.0)) * 0.1);
                float yPos = 2.0f + cycle;
                for (int i = 0; i < spikes.size(); i++) {
                    float sx = 0.6f + (float)Math.sin(tick * 0.04 + i) * 0.02f;
                    float sy = 2.0f + (float)Math.sin(tick * 0.05 + i) * 0.08f;
                    spikes.get(i).animateTo(
                            new Vector3f((float)kX[i] - sx / 2, yPos - sy / 2, (float)kZ[i] - sx / 2),
                            new AxisAngle4f((float)(Math.sin(tick * 0.04 + i) * 0.05), 0, 1, 0),
                            new Vector3f(sx, sy, sx), 8);
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // Eruption — SNOWFLAKE burst + ELECTRIC_SPARK + DUST per spike
                if (tick == 0 || tick == 8 || tick == 16) {
                    for (int i = 0; i < spikes.size(); i++) {
                        Location p = c.clone().add(kX[i], 0.5, kZ[i]);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 6, 0.3, 0.3, 0.3, 0.2);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.2, 0.2, 0.2, 0.1);
                        DisplayBuilder.dustParticles(p, 4, 0.2, 200, 230, 255, 1.3f);
                    }
                }
                if (tick % 3 == 0) for (int i = 0; i < spikes.size(); i++) {
                    Location p = c.clone().add(kX[i], 0.3 + Math.random() * 1.0, kZ[i]);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
            } else if (dissipatePhase) {
                // Retract — DUST + SNOWFLAKE settling
                if (tick == dissipateStart) {
                    for (int i = 0; i < spikes.size(); i++) {
                        Location p = c.clone().add(kX[i], 2.0, kZ[i]);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 5, 0.3, 0.5, 0.3, 0.1);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.2, 0.3, 0.2, 0.05);
                    }
                }
                if (tick % 3 == 0) for (int i = 0; i < spikes.size(); i++) {
                    Location p = c.clone().add(kX[i], 1.0 + Math.random() * 1.5, kZ[i]);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 200, 230, 255, 1.1f);
                }
            } else {
                // Active — 3 layered:
                // 1) ELECTRIC_SPARK between adjacent spikes (line walks)
                if (tick % 4 == 0) for (int i = 0; i < spikes.size() - 1; i++) {
                    // Connect to next spike in same row if exists
                    if ((i + 1) % 4 == 0) continue; // skip row boundary
                    double x1 = kX[i], z1 = kZ[i];
                    double x2 = kX[i + 1], z2 = kZ[i + 1];
                    for (double t = 0; t <= 1.0; t += 0.25) {
                        Location p = c.clone().add(x1 + (x2 - x1) * t, 2.6, z1 + (z2 - z1) * t);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.02);
                    }
                }
                // 2) FALLING_DUST(BLUE_ICE) from spike tips
                if (tick % 3 == 0) for (int i = 0; i < spikes.size(); i++) {
                    Location p = c.clone().add(kX[i], 2.8 + Math.random() * 0.2, kZ[i]);
                    w.spawnParticle(Particle.BLOCK, p, 2, 0.15, 0.05, 0.15, 0.02, Material.BLUE_ICE.createBlockData());
                }
                // 3) DUST cyan shimmer
                if (tick % 2 == 0) for (int i = 0; i < spikes.size(); i++) {
                    if (Math.random() > 0.3) continue;
                    Location p = c.clone().add(kX[i] + (Math.random() - 0.5) * 0.6, 1.0 + Math.random() * 1.5, kZ[i] + (Math.random() - 0.5) * 0.6);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 200, 230, 255, 1.0f);
                }
                // 4) Sparse snowflakes ambient
                if (tick % 4 == 0) for (int i = 0; i < 4; i++) {
                    int idx = (int)(Math.random() * spikes.size());
                    Location p = c.clone().add(kX[idx], 2.0 + Math.random() * 1.0, kZ[idx]);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                // BLOCK_GLASS_PLACE staggered on eruption
                if (tick % 2 == 0 && tick < 30) {
                    int idx = tick / 2;
                    if (idx < spikes.size()) {
                        Location sp = c.clone().add(kX[idx], 0.5, kZ[idx]);
                        DisplayBuilder.playSound(sp, Sound.BLOCK_GLASS_PLACE, 0.8f, 0.7f + (float)Math.random() * 0.3f);
                    }
                }
            } else if (dissipatePhase) {
                // BLOCK_GLASS_BREAK on retract
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.8f);
                if (tick % 4 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.8f + (float)Math.random() * 0.3f);
            } else {
                // BLOCK_AMETHYST_BLOCK_HIT during hold
                if (tick % 15 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 0.9f + (float)Math.random() * 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostSpikeField(plugin); }
    }

    // ================================================================
    // 20. EXPANDING FROZEN PUDDLE —
    //     Spawn: 1 central ICE tile (scale 0->1), ELECTRIC_SPARK ring at perimeter.
    //     Active: ring expands outward — 24 ICE flat tiles materialize at growing r=2->10
    //             over duration (staggered scale 0->1 as radius grows);
    //             ELECTRIC_SPARK at edge, SNOWFLAKE rain inside, GLOW center column.
    //     Dissipate: all tiles shatter — scale shrinks + ELECTRIC_SPARK explosion at center.
    // ================================================================
    public static class ExpandingFrozenPuddle extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ring = new ArrayList<>();
        private ItemDisplayHandle center;
        private final double[] rAng = new double[24];
        private double curRadius;
        private double prevRingTrigger;

        public ExpandingFrozenPuddle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("expanding_frozen_puddle", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2880.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 1.0f);

            curRadius = 2.0;
            prevRingTrigger = 2.0;

            // 1 central ICE tile (scale 0 -> 1)
            Location cp = c.clone().add(0, 0.35, 0);
            center = displayBuilder.spawnItem(cp, new ItemStack(Material.ICE));
            center.scale(0.01f, 0.01f, 0.01f).glow(200, 230, 255).interpolation(20, 0);
            spawnedEntities.add(center.entity());

            // 24 ICE flat tiles — start invisible, materialize as ring grows
            for (int i = 0; i < 24; i++) {
                rAng[i] = Math.PI * 2 * i / 24;
                // Start at center radius but invisible
                Location p = c.clone().add(Math.cos(rAng[i]) * curRadius, 0.35, Math.sin(rAng[i]) * curRadius);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 230, 255).interpolation(14, 0);
                ring.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;
            boolean spawnPhase = tick < 20;
            boolean dissipatePhase = tick >= dissipateStart;

            // Compute current radius — expand 2->10 over active phase
            // Active phase = [20, dissipateStart)
            int activeLen = dissipateStart - 20;
            if (spawnPhase) {
                curRadius = 2.0;
            } else if (dissipatePhase) {
                curRadius = 10.0;
            } else {
                double t = (tick - 20) / (double) Math.max(1, activeLen);
                curRadius = 2.0 + 8.0 * t;
            }

            if (spawnPhase) {
                // Central tile scale 0 -> 1 over 20t
                float progress = Math.min(1f, tick / 20f);
                float scale = progress;
                center.animateTo(
                        new Vector3f(-scale / 2, 0.35f, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, 0.15f, scale), 4);
                // Ring tiles stay at scale 0 during spawn
                for (int i = 0; i < ring.size(); i++) {
                    float bx = (float)(Math.cos(rAng[i]) * 2.0);
                    float bz = (float)(Math.sin(rAng[i]) * 2.0);
                    ring.get(i).animateTo(
                            new Vector3f(bx - 0.005f, 0.35f, bz - 0.005f),
                            new AxisAngle4f((float)rAng[i], 0, 1, 0),
                            new Vector3f(0.01f, 0.01f, 0.01f), 4);
                }
            } else if (dissipatePhase) {
                // All tiles shatter — scale shrinks
                int dt = tick - dissipateStart;
                float t01 = Math.min(1f, dt / 40f);
                float scale = 1f * (1f - t01);
                // Central tile shrinks
                center.animateTo(
                        new Vector3f(-scale / 2, 0.35f, -scale / 2),
                        new AxisAngle4f((float)(tick * 0.1), 0, 1, 0),
                        new Vector3f(scale, 0.15f * scale, scale), 2);
                for (int i = 0; i < ring.size(); i++) {
                    float bx = (float)(Math.cos(rAng[i]) * curRadius);
                    float bz = (float)(Math.sin(rAng[i]) * curRadius);
                    ring.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, 0.35f, bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.15), 0, 1, 0),
                            new Vector3f(scale, 0.15f, scale), 2);
                }
            } else {
                // Active — ring tiles materialize at growing radius; central holds at scale 1
                center.animateTo(
                        new Vector3f(-0.5f, 0.35f + (float)Math.sin(tick * 0.08) * 0.05f, -0.5f),
                        new AxisAngle4f((float)(tick * 0.02), 0, 1, 0),
                        new Vector3f(1.0f, 0.15f, 1.0f), 4);
                // Tiles staggered scale 0 -> 1 as radius grows from 2 to 10
                float overallProgress = (float)((curRadius - 2.0) / 8.0);
                for (int i = 0; i < ring.size(); i++) {
                    // Stagger by index — each tile materializes at slightly different rate
                    float stagger = (i / 24.0f) * 0.3f;
                    float localProgress = Math.max(0f, Math.min(1f, (overallProgress - stagger) / 0.7f));
                    float scale = localProgress;
                    double bobY = 0.35 + Math.sin(tick * 0.08 + i * 0.4) * 0.06;
                    float bx = (float)(Math.cos(rAng[i]) * curRadius);
                    float bz = (float)(Math.sin(rAng[i]) * curRadius);
                    ring.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, (float)bobY, bz - scale * 0.5f),
                            new AxisAngle4f((float)rAng[i], 0, 1, 0),
                            new Vector3f(scale, 0.15f, scale), 4);
                }
            }

            // Trigger sound + particles when ring crosses each block-radius
            if (!spawnPhase && !dissipatePhase && curRadius - prevRingTrigger >= 1.0) {
                prevRingTrigger = curRadius;
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12;
                    Location p = c.clone().add(Math.cos(a) * curRadius, 0.5, Math.sin(a) * curRadius);
                    DisplayBuilder.playSound(p, Sound.BLOCK_GLASS_PLACE, 0.7f, 1.0f);
                }
            }

            // --- Particle layers (3+ types) ---
            if (spawnPhase) {
                // ELECTRIC_SPARK ring at perimeter (around starting radius)
                if (tick % 2 == 0) for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16 + tick * 0.1;
                    Location p = c.clone().add(Math.cos(a) * 2.0, 0.4, Math.sin(a) * 2.0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
                if (tick % 3 == 0) {
                    Location p = c.clone().add(0, 0.5, 0);
                    w.spawnParticle(Particle.GLOW, p, 2, 0.3, 0.3, 0.3, 0);
                    DisplayBuilder.dustParticles(p, 2, 0.3, 180, 220, 255, 1.3f);
                }
            } else if (dissipatePhase) {
                // ELECTRIC_SPARK explosion at center + tile shatter
                if (tick == dissipateStart) {
                    for (int i = 0; i < 60; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * curRadius;
                        Location p = c.clone().add(Math.cos(a) * r, 0.4 + Math.random() * 0.4, Math.sin(a) * r);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.3, 0.2, 0.3, 0.15);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 2, 0.3, 0.2, 0.3, 0.1);
                    }
                    Location cp = c.clone().add(0, 0.5, 0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, cp, 30, 1.0, 0.5, 1.0, 0.3);
                }
                if (tick % 2 == 0) for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * curRadius;
                    Location p = c.clone().add(Math.cos(a) * r, 0.45, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 180, 220, 255, 1.2f);
                    if (i % 2 == 0) w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
            } else {
                // Active — 3 layered:
                // 1) ELECTRIC_SPARK at expanding ring edge
                if (tick % 1 == 0) for (int i = 0; i < 6; i++) {
                    double a = Math.PI * 2 * i / 6 + tick * 0.1;
                    Location p = c.clone().add(Math.cos(a) * curRadius, 0.45, Math.sin(a) * curRadius);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
                // 2) SNOWFLAKE rain inside
                for (int i = 0; i < 5; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * curRadius;
                    Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 0.6, Math.sin(a) * r);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
                // 3) GLOW center column
                if (tick % 3 == 0) for (int y = 0; y < 5; y++) {
                    Location p = c.clone().add(0, 0.5 + y * 0.6, 0);
                    w.spawnParticle(Particle.GLOW, p, 1, 0.15, 0.05, 0.15, 0);
                }
                // 4) Ice blue dust ring at edge
                if (tick % 3 == 0) for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16 + tick * 0.02;
                    Location p = c.clone().add(Math.cos(a) * curRadius, 0.45, Math.sin(a) * curRadius);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 180, 220, 255, 1.1f);
                }
            }

            // --- Phase-distinct sounds ---
            if (spawnPhase) {
                if (tick % 5 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.5f, 1.0f);
            } else if (dissipatePhase) {
                if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.0f);
                if (tick % 3 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.0f + (float)Math.random() * 0.4f);
            } else {
                // BLOCK_AMETHYST_BLOCK_HIT during expansion
                if (tick % 12 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 0.9f + (float)Math.random() * 0.3f);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = curRadius * curRadius;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ExpandingFrozenPuddle(plugin); }
    }
}
