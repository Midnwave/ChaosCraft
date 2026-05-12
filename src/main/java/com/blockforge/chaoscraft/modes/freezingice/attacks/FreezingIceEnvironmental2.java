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
    // 11. DRIFTING FROST CLOUD — 14 SNOWBALL ItemDisplays forming a
    //     puffy cloud at Y+4. FOLLOW-AI 0.06 — drifts toward nearest
    //     player. Constant radius 6.0, 220 damage, 14-tick.
    //     Particles: SNOWFLAKE rain underneath. Sound: WEATHER_RAIN pitch 1.4.
    // ================================================================
    public static class DriftingFrostCloud extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> puffs = new ArrayList<>();
        private final double[] pAng = new double[14];
        private final double[] pR = new double[14];
        private final double[] pY = new double[14];
        private final double[] pPhase = new double[14];

        public DriftingFrostCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("drifting_frost_cloud", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(220.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(14);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(360);
            config.setCooldownTicks(180);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.06);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.2f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_PLACE, 1.0f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 1.6f);

            for (int i = 0; i < 14; i++) {
                pAng[i] = Math.PI * 2 * i / 14 + (Math.random() - 0.5) * 0.3;
                pR[i] = 1.5 + Math.random() * 3.0;
                pY[i] = 4.0 + (Math.random() - 0.5) * 1.4;
                pPhase[i] = Math.random() * Math.PI * 2;
                Location p = c.clone().add(Math.cos(pAng[i]) * pR[i], pY[i], Math.sin(pAng[i]) * pR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(1.1f, 0.7f, 1.1f).glow(220, 235, 255).interpolation(14, 0);
                puffs.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cloud decoration: 6 WHITE_DYE wisps + 4 SNOW_BLOCK condensation under
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                double rr = 2.5;
                Location p = c.clone().add(Math.cos(a) * rr, 3.4 + Math.random() * 0.6, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_DYE));
                h.scale(0.55f, 0.55f, 0.55f).glow(240, 248, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.0, 3.2, Math.sin(a) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOW_BLOCK));
                h.scale(0.35f, 0.35f, 0.35f).glow(230, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow drifting puff cloud — gentle bobbing
            if (tick % 2 == 0) {
                for (int i = 0; i < puffs.size(); i++) {
                    pAng[i] += 0.01;
                    double bobY = pY[i] + Math.sin(tick * 0.05 + pPhase[i]) * 0.35;
                    double bobR = pR[i] + Math.sin(tick * 0.04 + i) * 0.2;
                    float bx = (float)(Math.cos(pAng[i]) * bobR);
                    float bz = (float)(Math.sin(pAng[i]) * bobR);
                    puffs.get(i).animateTo(
                            new Vector3f(bx - 0.55f, (float)bobY, bz - 0.55f),
                            new AxisAngle4f((float)(tick * 0.02 + i), 0, 1, 0),
                            new Vector3f(1.1f, 0.7f, 1.1f), 4);
                }
            }

            // SNOWFLAKE rain underneath the cloud — 3 per tick
            for (int i = 0; i < 3; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 5.5;
                Location p = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 3.5, Math.sin(a) * r);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.4, 0.1, 0.02);
            }
            // Wispy CLOUD particles inside the cloud body
            if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 4.0;
                Location p = c.clone().add(Math.cos(a) * r, 3.5 + Math.random() * 1.2, Math.sin(a) * r);
                w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.15, 0.2, 0.01);
                DisplayBuilder.dustParticles(p, 1, 0.1, 220, 235, 255, 1.1f);
            }
            // Periodic chill ambient
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.7f, 1.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.7f);
            }

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

        @Override public AbstractAttack newInstance() { return new DriftingFrostCloud(plugin); }
    }

    // ================================================================
    // 12. PROWLING COLD FOG — 20 PHANTOM_MEMBRANE flat ground-fog
    //     creeping along the ground. FOLLOW-AI 0.05 — slower than the
    //     cloud. Constant radius 8.0, 180 damage, 18-tick.
    //     Particles: CLOUD per tick at fog-low level. Sound: ENTITY_GHAST_AMBIENT pitch 0.5.
    // ================================================================
    public static class ProwlingColdFog extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> wisps = new ArrayList<>();
        private final double[] wAng = new double[20];
        private final double[] wR = new double[20];
        private final double[] wPhase = new double[20];

        public ProwlingColdFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prowling_cold_fog", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(180.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(18);
            config.setDamageDelayTicks(12);
            config.setDurationTicks(420);
            config.setCooldownTicks(200);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.05);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_AMBIENT, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.2f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.7f);

            for (int i = 0; i < 20; i++) {
                wAng[i] = Math.PI * 2 * i / 20 + (Math.random() - 0.5) * 0.2;
                wR[i] = 1.0 + Math.random() * 6.5;
                wPhase[i] = Math.random() * Math.PI * 2;
                Location p = c.clone().add(Math.cos(wAng[i]) * wR[i], 0.35 + Math.random() * 0.4, Math.sin(wAng[i]) * wR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(1.4f, 0.15f, 1.4f).glow(180, 200, 220).interpolation(18, 0);
                wisps.add(h);
                spawnedEntities.add(h.entity());
            }

            // Frosty floor scatter: 6 LIGHT_GRAY_DYE + 4 INK_SAC haze pools
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                double rr = 3.5;
                Location p = c.clone().add(Math.cos(a) * rr, 0.25, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.LIGHT_GRAY_DYE));
                h.scale(0.5f, 0.1f, 0.5f).glow(180, 200, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 5.5, 0.2, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.INK_SAC));
                h.scale(0.5f, 0.1f, 0.5f).glow(120, 140, 160).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow swirl — fog wisps creep along ground
            if (tick % 2 == 0) {
                for (int i = 0; i < wisps.size(); i++) {
                    wAng[i] += 0.012;
                    double bobR = wR[i] + Math.sin(tick * 0.03 + wPhase[i]) * 0.4;
                    double bobY = 0.35 + Math.sin(tick * 0.06 + wPhase[i]) * 0.15;
                    float bx = (float)(Math.cos(wAng[i]) * bobR);
                    float bz = (float)(Math.sin(wAng[i]) * bobR);
                    wisps.get(i).animateTo(
                            new Vector3f(bx - 0.7f, (float)bobY, bz - 0.7f),
                            new AxisAngle4f((float)(tick * 0.015 + i), 0, 1, 0),
                            new Vector3f(1.4f, 0.15f, 1.4f), 4);
                }
            }

            // CLOUD at fog-low level — every tick spreading
            for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 7.5;
                Location p = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 0.6, Math.sin(a) * r);
                w.spawnParticle(Particle.CLOUD, p, 1, 0.3, 0.05, 0.3, 0.02);
            }
            // Cold gray dust ring at perimeter
            if (tick % 6 == 0) for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16 + tick * 0.01;
                Location p = c.clone().add(Math.cos(a) * 7.8, 0.4, Math.sin(a) * 7.8);
                DisplayBuilder.dustParticles(p, 1, 0.1, 180, 200, 220, 1.2f);
            }
            // Sculk soul whispers
            if (tick % 8 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 12, 0.5, (Math.random() - 0.5) * 12);
                w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.01);
            }
            // Periodic low ambient drone
            if (tick % 70 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_AMBIENT, 0.7f, 0.5f);
            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.6f, 0.6f);

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

        @Override public AbstractAttack newInstance() { return new ProwlingColdFog(plugin); }
    }

    // ================================================================
    // 13. HUNTING BLIZZARD POCKET — swirling 30 SNOWBALL sphere
    //     actively chases. FOLLOW-AI 0.10 — fastest of the three.
    //     Constant radius 7.0, 260 damage, 12-tick.
    //     Particles: SNOWFLAKE swirl + CLOUD. Sound: ENTITY_HORSE_BREATHE pitch 0.7 wind howl.
    // ================================================================
    public static class HuntingBlizzardPocket extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flakes = new ArrayList<>();
        private final double[] sAng = new double[30];
        private final double[] sAng2 = new double[30];
        private final double[] sR = new double[30];

        public HuntingBlizzardPocket(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunting_blizzard_pocket", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(260.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(8);
            config.setDurationTicks(360);
            config.setCooldownTicks(190);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.10);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.5f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_PLACE, 1.2f, 0.8f);

            for (int i = 0; i < 30; i++) {
                // Sphere distribution via spherical coordinates
                sAng[i] = Math.PI * 2 * i / 30;
                sAng2[i] = Math.acos(1 - 2.0 * ((i + 0.5) / 30.0)); // polar angle
                sR[i] = 2.5 + Math.random() * 1.5;
                double bx = Math.sin(sAng2[i]) * Math.cos(sAng[i]) * sR[i];
                double bz = Math.sin(sAng2[i]) * Math.sin(sAng[i]) * sR[i];
                double by = 3.0 + Math.cos(sAng2[i]) * sR[i];
                Location p = c.clone().add(bx, by, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(12, 0);
                flakes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Blizzard core: 6 PACKED_ICE shards rotating + 4 SNOW_LAYER bands
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 1.4, 3.0, Math.sin(a) * 1.4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.45f, 0.45f, 0.45f).glow(200, 230, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 2.0, 2.0 + i * 0.4, Math.sin(a) * 2.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOW));
                h.scale(0.7f, 0.15f, 0.7f).glow(240, 250, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Swirling sphere — fast spin
            for (int i = 0; i < flakes.size(); i++) {
                sAng[i] += 0.18;
                double bx = Math.sin(sAng2[i]) * Math.cos(sAng[i]) * sR[i];
                double bz = Math.sin(sAng2[i]) * Math.sin(sAng[i]) * sR[i];
                double by = 3.0 + Math.cos(sAng2[i]) * sR[i];
                flakes.get(i).animateTo(
                        new Vector3f((float)bx - 0.25f, (float)by, (float)bz - 0.25f),
                        new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                        new Vector3f(0.5f), 2);
            }

            // SNOWFLAKE swirl particles — orbital
            if (tick % 1 == 0) for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i / 6) + tick * 0.25;
                double radius = 3.0 + Math.sin(tick * 0.1 + i) * 0.6;
                double yy = 3.0 + Math.sin(tick * 0.15 + i) * 2.0;
                Location p = c.clone().add(Math.cos(a) * radius, yy, Math.sin(a) * radius);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.05);
            }
            // CLOUD trail behind
            if (tick % 2 == 0) for (int i = 0; i < 5; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 3.5;
                double yy = 1.5 + Math.random() * 3.0;
                Location p = c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.2, 0.2, 0.03);
            }
            // Ice blue dust outline
            if (tick % 3 == 0) for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12 + tick * 0.15;
                double yy = 2.5 + Math.sin(tick * 0.1 + i) * 0.8;
                Location p = c.clone().add(Math.cos(a) * 3.2, yy, Math.sin(a) * 3.2);
                DisplayBuilder.dustParticles(p, 1, 0.1, 200, 230, 255, 1.3f);
            }
            // Wind howl periodic
            if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 0.9f, 0.7f);
            if (tick % 55 == 0) DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.7f, 0.6f);

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

        @Override public AbstractAttack newInstance() { return new HuntingBlizzardPocket(plugin); }
    }

    // ================================================================
    // 14. SLOW FROST BREATH — directional cone of 15 GLASS_BOTTLE
    //     (fixed direction, no AI). Constant radius 6.0, 240 damage, 14-tick.
    //     Particles: CLOUD streaks. Sound: ENTITY_HORSE_BREATHE.
    // ================================================================
    public static class SlowFrostBreath extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bottles = new ArrayList<>();
        private double breathAng;
        private final double[] bDist = new double[15];
        private final double[] bSide = new double[15];
        private final double[] bY = new double[15];

        public SlowFrostBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slow_frost_breath", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(240.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(14);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(320);
            config.setCooldownTicks(180);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.5f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 0.9f, 1.0f);

            breathAng = Math.random() * Math.PI * 2;
            double cosA = Math.cos(breathAng);
            double sinA = Math.sin(breathAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            for (int i = 0; i < 15; i++) {
                // Cone shape — wider as it goes out
                bDist[i] = 1.0 + (i / 15.0) * 6.5;
                double coneWidth = 0.4 + (bDist[i] / 7.5) * 2.2;
                bSide[i] = (Math.random() - 0.5) * 2 * coneWidth;
                bY[i] = 1.5 + (Math.random() - 0.5) * 1.2;
                double bx = cosA * bDist[i] + pCosA * bSide[i];
                double bz = sinA * bDist[i] + pSinA * bSide[i];
                Location p = c.clone().add(bx, bY[i], bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 230, 255).interpolation(14, 0);
                bottles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cone decoration: 6 SNOWBALL trailing + 4 ICE shards at cone mouth
            for (int i = 0; i < 6; i++) {
                double d = 0.5 + i * 0.4;
                double bx = cosA * d;
                double bz = sinA * d;
                Location p = c.clone().add(bx, 1.4, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.45f, 0.45f, 0.45f).glow(230, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double d = 6.0 + Math.random() * 1.5;
                double side = (i - 1.5) * 1.0;
                double bx = cosA * d + pCosA * side;
                double bz = sinA * d + pSinA * side;
                Location p = c.clone().add(bx, 1.0 + Math.random() * 1.2, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 230, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double cosA = Math.cos(breathAng);
            double sinA = Math.sin(breathAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // Slow outward drift + bob
            for (int i = 0; i < bottles.size(); i++) {
                double drift = bDist[i] + Math.sin(tick * 0.04 + i) * 0.4;
                double sideBob = bSide[i] + Math.sin(tick * 0.05 + i * 0.7) * 0.2;
                double yBob = bY[i] + Math.sin(tick * 0.06 + i) * 0.25;
                float bx = (float)(cosA * drift + pCosA * sideBob);
                float bz = (float)(sinA * drift + pSinA * sideBob);
                bottles.get(i).animateTo(
                        new Vector3f(bx - 0.25f, (float)yBob, bz - 0.25f),
                        new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                        new Vector3f(0.5f), 4);
            }

            // CLOUD streaks along cone axis
            if (tick % 1 == 0) {
                for (double d = 0.5; d < 7.5; d += 0.5) {
                    double coneWidth = 0.4 + (d / 7.5) * 2.0;
                    double side = (Math.random() - 0.5) * 2 * coneWidth;
                    double bx = cosA * d + pCosA * side;
                    double bz = sinA * d + pSinA * side;
                    double yy = 1.5 + (Math.random() - 0.5) * 1.4;
                    Location p = c.clone().add(bx, yy, bz);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.04);
                }
            }
            // Frost dust streaks
            if (tick % 2 == 0) for (int i = 0; i < 8; i++) {
                double d = 0.5 + Math.random() * 7.0;
                double coneWidth = 0.4 + (d / 7.5) * 2.0;
                double side = (Math.random() - 0.5) * 2 * coneWidth;
                double bx = cosA * d + pCosA * side;
                double bz = sinA * d + pSinA * side;
                double yy = 1.5 + (Math.random() - 0.5) * 1.4;
                Location p = c.clone().add(bx, yy, bz);
                DisplayBuilder.dustParticles(p, 1, 0.1, 220, 240, 255, 1.1f);
            }
            // Snowflakes at the cone mouth
            if (tick % 3 == 0) for (int i = 0; i < 3; i++) {
                double d = 5.5 + Math.random() * 2.0;
                double side = (Math.random() - 0.5) * 4.5;
                Location p = c.clone().add(cosA * d + pCosA * side, 1.0 + Math.random() * 1.5, sinA * d + pSinA * side);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
            }
            // Periodic breath
            if (tick % 45 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.1f, 0.9f);

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                // Damage anyone within the cone (wedge check)
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    // Project onto cone axis
                    double axis = dx * cosA + dz * sinA;
                    double perp = dx * pCosA + dz * pSinA;
                    if (axis < 0.5 || axis > 7.5) continue;
                    double coneWidth = 0.4 + (axis / 7.5) * 2.4;
                    if (Math.abs(perp) <= coneWidth) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SlowFrostBreath(plugin); }
    }

    // ================================================================
    // 15. WANDERING HOARFROST HAZE — 18 QUARTZ random walk (no
    //     follow-AI; per-ItemDisplay random translate). Constant
    //     radius 7.0, 200 damage, 16-tick.
    //     Particles: SNOWFLAKE + GLOW. Sound: AMBIENT_CAVE.
    // ================================================================
    public static class WanderingHoarfrostHaze extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] hX = new double[18];
        private final double[] hZ = new double[18];
        private final double[] hY = new double[18];
        private final double[] hVx = new double[18];
        private final double[] hVz = new double[18];
        private final double[] hPhase = new double[18];

        public WanderingHoarfrostHaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wandering_hoarfrost_haze", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(200.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(16);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(360);
            config.setCooldownTicks(180);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.4f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.9f, 1.2f);

            for (int i = 0; i < 18; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 5.5;
                hX[i] = Math.cos(a) * r;
                hZ[i] = Math.sin(a) * r;
                hY[i] = 0.5 + Math.random() * 3.5;
                double va = Math.random() * Math.PI * 2;
                double vmag = 0.04 + Math.random() * 0.06;
                hVx[i] = Math.cos(va) * vmag;
                hVz[i] = Math.sin(va) * vmag;
                hPhase[i] = Math.random() * Math.PI * 2;
                Location p = c.clone().add(hX[i], hY[i], hZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.QUARTZ));
                h.scale(0.55f, 0.55f, 0.55f).glow(240, 250, 255).interpolation(16, 0);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }

            // Hoarfrost crust: 6 QUARTZ_BLOCK clusters + 4 GLOWSTONE_DUST motes
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 1.5, 0.4, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.QUARTZ_BLOCK));
                h.scale(0.4f, 0.4f, 0.4f).glow(230, 240, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 4.0, 2.0, Math.sin(a) * 4.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOWSTONE_DUST));
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 240, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Random walk — each shard drifts independently, bouncing back at radius
            for (int i = 0; i < shards.size(); i++) {
                // Drift
                hX[i] += hVx[i];
                hZ[i] += hVz[i];
                // Bounce back if escaping
                double r2 = hX[i] * hX[i] + hZ[i] * hZ[i];
                if (r2 > 36) { // 6.0 boundary
                    hVx[i] *= -1;
                    hVz[i] *= -1;
                }
                // Random jitter every 30 ticks (path re-randomize)
                if (tick % 30 == i % 30) {
                    double va = Math.random() * Math.PI * 2;
                    double vmag = 0.04 + Math.random() * 0.06;
                    hVx[i] = Math.cos(va) * vmag;
                    hVz[i] = Math.sin(va) * vmag;
                }
                double yBob = hY[i] + Math.sin(tick * 0.06 + hPhase[i]) * 0.35;
                shards.get(i).animateTo(
                        new Vector3f((float)hX[i] - 0.275f, (float)yBob, (float)hZ[i] - 0.275f),
                        new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0),
                        new Vector3f(0.55f), 16);
            }

            // SNOWFLAKE + GLOW particles wandering
            for (int i = 0; i < 3; i++) {
                int idx = (tick + i) % shards.size();
                Location p = c.clone().add(hX[idx], hY[idx] + 0.2, hZ[idx]);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.2, 0.2, 0.2, 0.03);
                w.spawnParticle(Particle.GLOW, p, 1, 0.15, 0.15, 0.15, 0);
            }
            // Soft cavernous shimmer dust
            if (tick % 4 == 0) for (int i = 0; i < 5; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 6.5;
                Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 4.0, Math.sin(a) * r);
                DisplayBuilder.dustParticles(p, 1, 0.1, 240, 250, 255, 1.0f);
            }
            // Cave ambient
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.7f, 1.0f);
            if (tick % 100 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);

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

        @Override public AbstractAttack newInstance() { return new WanderingHoarfrostHaze(plugin); }
    }

    // ================================================================
    // 16. CREEPING RIME BANK — 24 BLUE_ICE flat bar slides
    //     perpendicular across arena. Constant radius 10.0, 260 damage, 12-tick.
    //     Particles: FALLING_DUST(BLUE_ICE). Sound: BLOCK_GLASS_PLACE per step.
    // ================================================================
    public static class CreepingRimeBank extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tiles = new ArrayList<>();
        private double slideAng;
        private double slideOff;
        private final double[] tDist = new double[24];

        public CreepingRimeBank(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_rime_bank", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(260.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(8);
            config.setDurationTicks(320);
            config.setCooldownTicks(190);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.9f);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.8f, 0.7f);

            slideAng = Math.random() * Math.PI * 2;
            slideOff = -10.0; // starts off arena
            double cosA = Math.cos(slideAng);
            double sinA = Math.sin(slideAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            for (int i = 0; i < 24; i++) {
                // Bar perpendicular to slide direction — 24 tiles spread 12 blocks wide
                tDist[i] = -6.0 + i * (12.0 / 23.0);
                double bx = cosA * slideOff + pCosA * tDist[i];
                double bz = sinA * slideOff + pSinA * tDist[i];
                Location p = c.clone().add(bx, 0.35, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(1.0f, 0.18f, 1.0f).glow(150, 200, 255).interpolation(12, 0);
                tiles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Frost frame: 6 PACKED_ICE + 4 ICE chunks marking the bar leading edge
            for (int i = 0; i < 6; i++) {
                double side = -5.5 + i * (11.0 / 5.0);
                double bx = cosA * (-10.5) + pCosA * side;
                double bz = sinA * (-10.5) + pSinA * side;
                Location p = c.clone().add(bx, 0.7, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 220, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double side = -4.5 + i * 3.0;
                double bx = cosA * (-9.0) + pCosA * side;
                double bz = sinA * (-9.0) + pSinA * side;
                Location p = c.clone().add(bx, 0.5, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 230, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slide forward
            slideOff += 0.16;
            // Reset slide once it crosses fully
            if (slideOff > 11.0) slideOff = -10.0;

            double cosA = Math.cos(slideAng);
            double sinA = Math.sin(slideAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            for (int i = 0; i < tiles.size(); i++) {
                double yBob = 0.35 + Math.sin(tick * 0.1 + i * 0.4) * 0.1;
                float bx = (float)(cosA * slideOff + pCosA * tDist[i]);
                float bz = (float)(sinA * slideOff + pSinA * tDist[i]);
                tiles.get(i).animateTo(
                        new Vector3f(bx - 0.5f, (float)yBob, bz - 0.5f),
                        new AxisAngle4f((float)slideAng, 0, 1, 0),
                        new Vector3f(1.0f, 0.18f, 1.0f), 4);
            }

            // FALLING_DUST(BLUE_ICE) trailing the bar
            if (tick % 1 == 0) for (int i = 0; i < 6; i++) {
                double side = -5.5 + Math.random() * 11.0;
                double bx = cosA * (slideOff - 0.5) + pCosA * side;
                double bz = sinA * (slideOff - 0.5) + pSinA * side;
                Location p = c.clone().add(bx, 0.6 + Math.random() * 0.4, bz);
                w.spawnParticle(Particle.BLOCK, p, 1, 0.2, 0.1, 0.2, 0.05, Material.BLUE_ICE.createBlockData());
            }
            // Snowflakes on top
            if (tick % 2 == 0) for (int i = 0; i < 5; i++) {
                double side = -5.5 + Math.random() * 11.0;
                double bx = cosA * slideOff + pCosA * side;
                double bz = sinA * slideOff + pSinA * side;
                Location p = c.clone().add(bx, 0.8 + Math.random() * 0.4, bz);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                DisplayBuilder.dustParticles(p, 1, 0.1, 150, 200, 255, 1.2f);
            }
            // Glass-place each step (every 4 ticks for footfall feel)
            if (tick % 4 == 0) {
                Location stepP = c.clone().add(cosA * slideOff, 0.4, sinA * slideOff);
                DisplayBuilder.playSound(stepP, Sound.BLOCK_GLASS_PLACE, 0.8f, 0.8f + (float)Math.random() * 0.2f);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                // Damage anyone within the slide bar (perpendicular band)
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    double axis = dx * cosA + dz * sinA;
                    double perp = dx * pCosA + dz * pSinA;
                    if (Math.abs(axis - slideOff) <= 1.2 && Math.abs(perp) <= 6.5) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CreepingRimeBank(plugin); }
    }

    // ================================================================
    // 17. STALKING CHILL VEIL — 20 PHANTOM_MEMBRANE vertical curtain
    //     (fixed-direction sliding). Constant radius 5.0, 280 damage, 11-tick.
    //     Particles: SCULK_SOUL. Sound: ENTITY_PHANTOM_AMBIENT.
    // ================================================================
    public static class StalkingChillVeil extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> veil = new ArrayList<>();
        private double slideAng;
        private double slideOff;
        private final double[] vSide = new double[20];
        private final double[] vY = new double[20];

        public StalkingChillVeil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stalking_chill_veil", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(11);
            config.setDamageDelayTicks(8);
            config.setDurationTicks(320);
            config.setCooldownTicks(190);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_PLACE, 1.0f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 1.0f);

            slideAng = Math.random() * Math.PI * 2;
            slideOff = -8.0;
            double cosA = Math.cos(slideAng);
            double sinA = Math.sin(slideAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            // Veil = 5 wide, 4 tall = 20 tiles
            for (int i = 0; i < 20; i++) {
                int col = i % 5;
                int row = i / 5;
                vSide[i] = -2.4 + col * 1.2; // 5 columns spanning 6 blocks wide
                vY[i] = 0.6 + row * 1.4; // 4 rows from y=0.6 to y=4.8
                double bx = cosA * slideOff + pCosA * vSide[i];
                double bz = sinA * slideOff + pSinA * vSide[i];
                Location p = c.clone().add(bx, vY[i], bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(1.1f, 1.3f, 0.15f).glow(120, 80, 160).interpolation(11, 0);
                veil.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ghostly decoration: 6 SCULK_VEIN strips + 4 ECHO_SHARD at corners
            for (int i = 0; i < 6; i++) {
                double side = -2.5 + i * 1.0;
                double bx = cosA * (slideOff - 0.3) + pCosA * side;
                double bz = sinA * (slideOff - 0.3) + pSinA * side;
                Location p = c.clone().add(bx, 0.4, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SCULK_VEIN));
                h.scale(0.5f, 0.1f, 0.5f).glow(80, 60, 120).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double side = i < 2 ? -2.8 : 2.8;
                double yy = (i % 2 == 0) ? 0.4 : 5.4;
                double bx = cosA * slideOff + pCosA * side;
                double bz = sinA * slideOff + pSinA * side;
                Location p = c.clone().add(bx, yy, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.55f, 0.55f, 0.55f).glow(100, 200, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slide curtain perpendicular to its plane
            slideOff += 0.13;
            if (slideOff > 9.0) slideOff = -8.0;

            double cosA = Math.cos(slideAng);
            double sinA = Math.sin(slideAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            for (int i = 0; i < veil.size(); i++) {
                // Subtle wave shimmer
                double yBob = vY[i] + Math.sin(tick * 0.08 + i * 0.3) * 0.2;
                double sideBob = vSide[i] + Math.sin(tick * 0.06 + i) * 0.15;
                float bx = (float)(cosA * slideOff + pCosA * sideBob);
                float bz = (float)(sinA * slideOff + pSinA * sideBob);
                veil.get(i).animateTo(
                        new Vector3f(bx - 0.55f, (float)yBob, bz - 0.075f),
                        new AxisAngle4f((float)slideAng, 0, 1, 0),
                        new Vector3f(1.1f, 1.3f, 0.15f), 4);
            }

            // SCULK_SOUL whispers throughout the curtain
            if (tick % 1 == 0) for (int i = 0; i < 4; i++) {
                double side = -2.5 + Math.random() * 5.0;
                double yy = 0.5 + Math.random() * 4.5;
                double bx = cosA * slideOff + pCosA * side;
                double bz = sinA * slideOff + pSinA * side;
                Location p = c.clone().add(bx, yy, bz);
                w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.2, 0.2, 0.05, 0.02);
            }
            // Purple ghost dust outline
            if (tick % 3 == 0) for (int i = 0; i < 6; i++) {
                double side = -2.8 + Math.random() * 5.6;
                double yy = 0.4 + Math.random() * 4.6;
                Location p = c.clone().add(cosA * slideOff + pCosA * side, yy, sinA * slideOff + pSinA * side);
                DisplayBuilder.dustParticles(p, 1, 0.05, 130, 80, 180, 1.2f);
            }
            // ENCHANT trail behind
            if (tick % 4 == 0) for (int i = 0; i < 3; i++) {
                double side = -2.5 + Math.random() * 5.0;
                double yy = 0.5 + Math.random() * 4.5;
                Location p = c.clone().add(cosA * (slideOff - 0.6) + pCosA * side, yy, sinA * (slideOff - 0.6) + pSinA * side);
                w.spawnParticle(Particle.ENCHANT, p, 1, 0.05, 0.05, 0.05, 0.04);
            }
            // Phantom ambient
            if (tick % 35 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 0.9f, 0.8f);

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    double axis = dx * cosA + dz * sinA;
                    double perp = dx * pCosA + dz * pSinA;
                    if (Math.abs(axis - slideOff) <= 0.8 && Math.abs(perp) <= 3.0) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new StalkingChillVeil(plugin); }
    }

    // ================================================================
    // 18. FROZEN GUST FRONT — 28 FEATHER directional sweep.
    //     Constant radius 8.0, 220 damage, 12-tick.
    //     Particles: CLOUD streak. Sound: ENTITY_HORSE_BREATHE pitch 0.8.
    // ================================================================
    public static class FrozenGustFront extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> feathers = new ArrayList<>();
        private double sweepAng;
        private double sweepOff;
        private final double[] fSide = new double[28];
        private final double[] fY = new double[28];
        private final double[] fPhase = new double[28];

        public FrozenGustFront(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_gust_front", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(220.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(8);
            config.setDurationTicks(340);
            config.setCooldownTicks(180);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.5f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1.2f, 0.9f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.4f);

            sweepAng = Math.random() * Math.PI * 2;
            sweepOff = -9.0;
            double cosA = Math.cos(sweepAng);
            double sinA = Math.sin(sweepAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            for (int i = 0; i < 28; i++) {
                // 28 feathers across a 14-wide front spread over 3 layers tall
                int col = i % 7;
                int layer = i / 7;
                fSide[i] = -6.0 + col * 2.0 + (Math.random() - 0.5) * 0.6;
                fY[i] = 0.5 + layer * 1.2 + Math.random() * 0.8;
                fPhase[i] = Math.random() * Math.PI * 2;
                double bx = cosA * sweepOff + pCosA * fSide[i];
                double bz = sinA * sweepOff + pSinA * fSide[i];
                Location p = c.clone().add(bx, fY[i], bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.6f, 0.6f, 0.6f).glow(230, 240, 255).interpolation(12, 0);
                feathers.add(h);
                spawnedEntities.add(h.entity());
            }

            // Wind framework: 6 STRING streaks + 4 PHANTOM_MEMBRANE banners
            for (int i = 0; i < 6; i++) {
                double side = -5.0 + i * 2.0;
                double bx = cosA * sweepOff + pCosA * side;
                double bz = sinA * sweepOff + pSinA * side;
                Location p = c.clone().add(bx, 1.8, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.2f, 0.2f, 0.9f).glow(220, 230, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double side = -4.5 + i * 3.0;
                double bx = cosA * (sweepOff - 0.4) + pCosA * side;
                double bz = sinA * (sweepOff - 0.4) + pSinA * side;
                Location p = c.clone().add(bx, 2.5, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.7f, 1.0f, 0.1f).glow(200, 220, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Faster sweep than rime bank
            sweepOff += 0.22;
            if (sweepOff > 10.0) sweepOff = -9.0;

            double cosA = Math.cos(sweepAng);
            double sinA = Math.sin(sweepAng);
            double pCosA = -sinA;
            double pSinA = cosA;

            for (int i = 0; i < feathers.size(); i++) {
                double sideBob = fSide[i] + Math.sin(tick * 0.1 + fPhase[i]) * 0.4;
                double yBob = fY[i] + Math.sin(tick * 0.15 + i) * 0.25;
                float bx = (float)(cosA * sweepOff + pCosA * sideBob);
                float bz = (float)(sinA * sweepOff + pSinA * sideBob);
                feathers.get(i).animateTo(
                        new Vector3f(bx - 0.3f, (float)yBob, bz - 0.3f),
                        new AxisAngle4f((float)(tick * 0.3 + fPhase[i]), 0, 1, 0),
                        new Vector3f(0.6f), 4);
            }

            // CLOUD streak parallel to sweep
            if (tick % 1 == 0) for (int i = 0; i < 8; i++) {
                double side = -6.0 + Math.random() * 12.0;
                double yy = 0.5 + Math.random() * 3.5;
                Location p = c.clone().add(cosA * sweepOff + pCosA * side, yy, sinA * sweepOff + pSinA * side);
                w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.2, 0.2, 0.05);
            }
            // Snowflakes trailing
            if (tick % 2 == 0) for (int i = 0; i < 6; i++) {
                double side = -6.0 + Math.random() * 12.0;
                double yy = 0.5 + Math.random() * 3.5;
                Location p = c.clone().add(cosA * (sweepOff - 0.6) + pCosA * side, yy, sinA * (sweepOff - 0.6) + pSinA * side);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.15, 0.15, 0.15, 0.03);
            }
            // White ash streaks
            if (tick % 1 == 0) for (int i = 0; i < 5; i++) {
                double side = -6.0 + Math.random() * 12.0;
                double yy = 0.5 + Math.random() * 3.5;
                Location p = c.clone().add(cosA * sweepOff + pCosA * side, yy, sinA * sweepOff + pSinA * side);
                w.spawnParticle(Particle.WHITE_ASH, p, 1, 0.1, 0.1, 0.1, 0.04);
            }
            // Frost dust outline
            if (tick % 3 == 0) for (int i = 0; i < 6; i++) {
                double side = -6.0 + Math.random() * 12.0;
                Location p = c.clone().add(cosA * sweepOff + pCosA * side, 1.5 + Math.random() * 2.0, sinA * sweepOff + pSinA * side);
                DisplayBuilder.dustParticles(p, 1, 0.1, 220, 240, 255, 1.1f);
            }
            // Wind howl
            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.8f);
            if (tick % 55 == 0) DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.6f, 0.9f);

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
    // 19. FROST SPIKE FIELD — 16 PACKED_ICE spike grid erupts upward.
    //     Constant radius 8.0, 300 damage, 10-tick.
    //     Particles: ITEM_SNOWBALL erupt + CRIT. Sound: BLOCK_GLASS_BREAK per spike.
    // ================================================================
    public static class FrostSpikeField extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> spikes = new ArrayList<>();
        private final double[] kX = new double[16];
        private final double[] kZ = new double[16];
        private final double[] kY = new double[16];
        private final double[] kTargetY = new double[16];
        private final int[] kStart = new int[16];
        private final boolean[] kErupted = new boolean[16];

        public FrostSpikeField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_spike_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(300.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(6);
            config.setDurationTicks(320);
            config.setCooldownTicks(200);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.2f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.6f);

            // 4x4 grid, slightly randomized
            int idx = 0;
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double offX = -4.5 + gx * 3.0 + (Math.random() - 0.5) * 0.6;
                    double offZ = -4.5 + gz * 3.0 + (Math.random() - 0.5) * 0.6;
                    kX[idx] = offX;
                    kZ[idx] = offZ;
                    kY[idx] = -0.5;
                    kTargetY[idx] = 1.5 + Math.random() * 1.5;
                    kStart[idx] = idx * 4; // staggered
                    kErupted[idx] = false;
                    Location p = c.clone().add(kX[idx], kY[idx], kZ[idx]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                    h.scale(0.5f, 0.1f, 0.5f).glow(180, 220, 255).interpolation(8, 0);
                    spikes.add(h);
                    spawnedEntities.add(h.entity());
                    idx++;
                }
            }

            // Spike camouflage: 6 ICE chunks + 4 BLUE_ICE blocks at perimeter
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 6.5, 0.5, Math.sin(a) * 6.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 230, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 7.5, 0.5, Math.sin(a) * 7.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.55f, 0.55f, 0.55f).glow(150, 200, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < spikes.size(); i++) {
                if (tick < kStart[i]) continue;

                if (!kErupted[i]) {
                    // Eruption — rise from -0.5 to target over 8 ticks
                    int eruptTick = tick - kStart[i];
                    if (eruptTick <= 8) {
                        double progress = eruptTick / 8.0;
                        kY[i] = -0.5 + kTargetY[i] * progress;
                        float sx = 0.5f + (float)progress * 0.3f;
                        float sy = 0.1f + (float)progress * 2.0f; // tall spike
                        spikes.get(i).animateTo(
                                new Vector3f((float)kX[i] - sx / 2, (float)kY[i] - 0.5f, (float)kZ[i] - sx / 2),
                                new AxisAngle4f((float)(Math.random() * Math.PI * 0.1), 0, 1, 0),
                                new Vector3f(sx, sy, sx), 2);
                        // Eruption particles
                        if (eruptTick == 1) {
                            Location ep = c.clone().add(kX[i], 0.3, kZ[i]);
                            w.spawnParticle(Particle.ITEM_SNOWBALL, ep, 16, 0.3, 0.2, 0.3, 0.3);
                            w.spawnParticle(Particle.CRIT, ep, 12, 0.3, 0.2, 0.3, 0.3);
                            w.spawnParticle(Particle.EXPLOSION, ep, 1, 0, 0, 0, 0);
                            DisplayBuilder.dustParticles(ep, 12, 0.3, 200, 230, 255, 1.4f);
                            DisplayBuilder.playSound(ep, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.7f + (float)Math.random() * 0.3f);
                            DisplayBuilder.playSound(ep, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.9f, 0.8f);
                        }
                    } else {
                        kErupted[i] = true;
                    }
                } else {
                    // Settled — subtle sway
                    float sy = 2.1f + (float)Math.sin(tick * 0.05 + i) * 0.05f;
                    float sx = 0.8f;
                    spikes.get(i).animateTo(
                            new Vector3f((float)kX[i] - sx / 2, (float)kY[i] - 0.5f, (float)kZ[i] - sx / 2),
                            new AxisAngle4f((float)(Math.sin(tick * 0.04 + i) * 0.05), 0, 1, 0),
                            new Vector3f(sx, sy, sx), 8);
                    if (tick % 8 == i % 8) {
                        Location p = c.clone().add(kX[i], kY[i] + Math.random() * 1.0, kZ[i]);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.2, 0.4, 0.2, 0.03);
                        DisplayBuilder.dustParticles(p, 1, 0.1, 200, 230, 255, 1.0f);
                    }
                }
            }

            // Ambient field shimmer
            if (tick % 4 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 7.0;
                Location p = c.clone().add(Math.cos(a) * r, 0.4 + Math.random() * 2.5, Math.sin(a) * r);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.01);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                // Damage on each erupted spike's small AOE
                for (int i = 0; i < spikes.size(); i++) {
                    if (!kErupted[i]) continue;
                    Location sp = c.clone().add(kX[i], 0.5, kZ[i]);
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(sp) <= 1.5 * 1.5) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                            break;
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostSpikeField(plugin); }
    }

    // ================================================================
    // 20. EXPANDING FROZEN PUDDLE — 24 ICE flat tiles ring grows 2->10
    //     blocks (animated). Constant radius matches ring, 240 damage, 14-tick.
    //     Particles: DRIPPING_WATER + SNOWFLAKE. Sound: BLOCK_GLASS_PLACE on ring expansion.
    // ================================================================
    public static class ExpandingFrozenPuddle extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ring = new ArrayList<>();
        private final double[] rAng = new double[24];
        private double curRadius;
        private double prevRingTrigger;

        public ExpandingFrozenPuddle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("expanding_frozen_puddle", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(240.0);
            config.setDamageRadius(10.0); // max ring radius — actual damage uses curRadius
            config.setTicksBetweenDamage(14);
            config.setDamageDelayTicks(8);
            config.setDurationTicks(360);
            config.setCooldownTicks(190);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 1.2f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.9f);

            curRadius = 2.0;
            prevRingTrigger = 2.0;

            for (int i = 0; i < 24; i++) {
                rAng[i] = Math.PI * 2 * i / 24;
                Location p = c.clone().add(Math.cos(rAng[i]) * curRadius, 0.35, Math.sin(rAng[i]) * curRadius);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.85f, 0.15f, 0.85f).glow(200, 230, 255).interpolation(14, 0);
                ring.add(h);
                spawnedEntities.add(h.entity());
            }

            // Puddle center decoration: 6 WATER_BUCKET drips + 4 SNOW_BLOCK islands
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 0.6, 0.3, Math.sin(a) * 0.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WATER_BUCKET));
                h.scale(0.45f, 0.1f, 0.45f).glow(120, 180, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 1.2, 0.5, Math.sin(a) * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOW_BLOCK));
                h.scale(0.35f, 0.35f, 0.35f).glow(240, 250, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Expand ring from 2 -> 10 over 240 ticks, hold, then pulse
            if (tick < 240) {
                curRadius = 2.0 + (tick / 240.0) * 8.0;
            } else {
                // Gentle pulse oscillation
                curRadius = 10.0 + Math.sin((tick - 240) * 0.12) * 0.4;
            }

            for (int i = 0; i < ring.size(); i++) {
                double bobY = 0.35 + Math.sin(tick * 0.08 + i * 0.4) * 0.08;
                float bx = (float)(Math.cos(rAng[i]) * curRadius);
                float bz = (float)(Math.sin(rAng[i]) * curRadius);
                // Scale tiles up slightly with ring expansion
                float scale = 0.75f + (float)((curRadius - 2.0) / 8.0) * 0.35f;
                ring.get(i).animateTo(
                        new Vector3f(bx - scale / 2, (float)bobY, bz - scale / 2),
                        new AxisAngle4f((float)rAng[i], 0, 1, 0),
                        new Vector3f(scale, 0.15f, scale), 4);
            }

            // Trigger sound + particles when ring crosses each block-radius
            if (curRadius - prevRingTrigger >= 1.0) {
                prevRingTrigger = curRadius;
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12;
                    Location p = c.clone().add(Math.cos(a) * curRadius, 0.5, Math.sin(a) * curRadius);
                    DisplayBuilder.playSound(p, Sound.BLOCK_GLASS_PLACE, 0.7f, 1.0f);
                    w.spawnParticle(Particle.ITEM_SNOWBALL, p, 4, 0.2, 0.1, 0.2, 0.05);
                }
                DisplayBuilder.particleRing(c.clone().add(0, 0.4, 0), curRadius, Particle.SNOWFLAKE, 28, null);
            }

            // DRIPPING_WATER over the puddle
            if (tick % 1 == 0) for (int i = 0; i < 5; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * curRadius;
                Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 0.4, Math.sin(a) * r);
                w.spawnParticle(Particle.DRIPPING_WATER, p, 1, 0.05, 0.05, 0.05, 0);
            }
            // SNOWFLAKE drift across the ring
            if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * curRadius;
                Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 0.6, Math.sin(a) * r);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
            }
            // Ice blue dust ring at edge
            if (tick % 3 == 0) for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16 + tick * 0.02;
                Location p = c.clone().add(Math.cos(a) * curRadius, 0.45, Math.sin(a) * curRadius);
                DisplayBuilder.dustParticles(p, 1, 0.05, 180, 220, 255, 1.1f);
            }
            // Water ambient
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 0.7f, 0.8f);

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
