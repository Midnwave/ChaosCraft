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
 * FreezingIce Mode — ENVIRONMENTAL FX BATCH 5 (entries 41-50).
 * Surreal + Mobile Chaser attacks. Pure ItemDisplay + particle attacks. NO BlockDisplays.
 * Themes: echoing bells, drifting specters, hunting zones, prowling orbs,
 * roaming beasts, shivering chainpetals, frostfire orbitals, cryostasis domes,
 * subzero vortexes, and the climactic absolute-zero implosion.
 *
 * Follow-AI attacks (3): DriftingFrostSpecter (0.09), HuntingColdZone (0.08),
 * RoamingHoarfrostBeast (0.11).
 *
 * Attacks 41-48 use explicit spawn (0-40t) -> active -> dissipate (final 40t)
 * phase choreography with 3+ particle types and phase-distinct sounds.
 */
public final class FreezingIceEnvironmental5 {
    private FreezingIceEnvironmental5() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";
    private static final int SPAWN_PHASE_TICKS = 40;
    private static final int DISSIPATE_PHASE_TICKS = 40;

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EchoingFrostBell(plugin));
        registry.register(new DriftingFrostSpecter(plugin));
        registry.register(new HuntingColdZone(plugin));
        registry.register(new ProwlingBlizzardOrb(plugin));
        registry.register(new RoamingHoarfrostBeast(plugin));
        registry.register(new ShiveringChainpetals(plugin));
        registry.register(new FrostfireOrbital(plugin));
        registry.register(new CryostasisDome(plugin));
        registry.register(new SubzeroVortex(plugin));
        registry.register(new AbsoluteZeroPoint(plugin));
    }

    // ================================================================
    // 41. ECHOING FROST BELL — Phased: spawn bell growing -> breathing
    //     pulse with expanding concentric SCULK_SOUL rings -> shrink to
    //     nothing with crescendo. Damage 48000hp, 15.0r, 4-tick interval.
    // ================================================================
    public static class EchoingFrostBell extends EnvironmentalAttack {
        private ItemDisplayHandle bell;
        private final List<ItemDisplayHandle> rim = new ArrayList<>();
        private final List<ItemDisplayHandle> resonators = new ArrayList<>();

        public EchoingFrostBell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echoing_frost_bell", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(48000.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(400);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // SPAWN sound (low, ominous bell strike)
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_USE, 1.8f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.7f);

            Location bellPos = c.clone().add(0, 3.0, 0);
            bell = displayBuilder.spawnItem(bellPos, new ItemStack(Material.NAUTILUS_SHELL));
            bell.scale(0.01f, 0.01f, 0.01f).glow(180, 220, 255).interpolation(SPAWN_PHASE_TICKS - 4, 0);
            spawnedEntities.add(bell.entity());

            // Rim accents start tiny — grow in during spawn phase.
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 1.6, 2.2, Math.sin(a) * 1.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAUTILUS_SHELL));
                h.scale(0.01f, 0.01f, 0.01f).glow(160, 200, 240).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                rim.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.2;
                Location p = c.clone().add(Math.cos(a) * 0.9, 4.4 + Math.sin(a) * 0.4, Math.sin(a) * 0.9);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.01f, 0.01f, 0.01f).glow(140, 200, 255).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                resonators.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (bell == null) return;
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_PHASE_TICKS;

            // ===== SPAWN PHASE (0-40t): bell grows scale 0 -> 1.2 with SCULK_SOUL shimmer =====
            if (tick < SPAWN_PHASE_TICKS) {
                double t = tick / (double)SPAWN_PHASE_TICKS;
                float s = (float)(0.01 + 1.19 * t);
                bell.animateTo(
                        new Vector3f(-s / 2, 3.0f - s / 2, -s / 2),
                        new AxisAngle4f((float)(tick * 0.04), 0, 1, 0),
                        new Vector3f(s), 4);
                // Rim items grow alongside
                for (int i = 0; i < rim.size(); i++) {
                    double a = Math.PI * 2 * i / 8;
                    float rs = (float)(0.01 + 0.44 * t);
                    rim.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * 1.6) - rs / 2, 2.2f - rs / 2, (float)(Math.sin(a) * 1.6) - rs / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(rs), 4);
                }
                for (int i = 0; i < resonators.size(); i++) {
                    double a = Math.PI * 2 * i / 6 + 0.2;
                    float rs = (float)(0.01 + 0.49 * t);
                    resonators.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * 0.9) - rs / 2, (float)(4.4 + Math.sin(a) * 0.4) - rs / 2, (float)(Math.sin(a) * 0.9) - rs / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(rs), 4);
                }
                // SCULK_SOUL shimmer during spawn
                for (int i = 0; i < 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double rr = Math.random() * (1.5 + t * 1.5);
                    w.spawnParticle(Particle.SCULK_SOUL, c.clone().add(Math.cos(a) * rr, 3.0 + (Math.random() - 0.5) * 2.0, Math.sin(a) * rr),
                            1, 0.1, 0.1, 0.1, 0.02);
                }
                if (tick == 10) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.0f, 1.4f);
                if (tick == 25) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.0f, 1.6f);
            }
            // ===== ACTIVE PHASE (40 .. duration-40): breathing pulse + expanding rings =====
            else if (tick < dissipateStart) {
                int aT = tick - SPAWN_PHASE_TICKS;
                // Breathing pulse: scale 1.2 <-> 1.0 every 30t (sin wave)
                double pulse = 0.5 + 0.5 * Math.sin(aT * (Math.PI / 30.0));
                float scale = (float)(1.0 + pulse * 0.2);
                bell.animateTo(
                        new Vector3f(-scale / 2, 3.0f - scale / 2, -scale / 2),
                        new AxisAngle4f((float)(tick * 0.02), 0, 1, 0),
                        new Vector3f(scale), 4);

                // Rim sway
                for (int i = 0; i < rim.size(); i++) {
                    double a = Math.PI * 2 * i / 8 + aT * 0.005;
                    float by = (float)(2.2 + Math.sin(aT * 0.05 + i) * 0.18);
                    rim.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * 1.6) - 0.225f, by - 0.225f, (float)(Math.sin(a) * 1.6) - 0.225f),
                            new AxisAngle4f((float)(tick * 0.06 + i), 0, 1, 0),
                            new Vector3f(0.45f), 4);
                }
                // Resonator orbit
                for (int i = 0; i < resonators.size(); i++) {
                    double a = Math.PI * 2 * i / 6 + aT * 0.03;
                    float ry = (float)(4.4 + Math.sin(aT * 0.06 + i) * 0.25);
                    resonators.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * 0.9) - 0.25f, ry - 0.25f, (float)(Math.sin(a) * 0.9) - 0.25f),
                            new AxisAngle4f((float)(tick * 0.08 + i), 0, 1, 0),
                            new Vector3f(0.5f), 4);
                }

                // Expanding SCULK_SOUL rings every 20t (3 overlap, expand 1->9 over 30t)
                if (aT % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 1.4f, 0.9f);
                    for (int r = 1; r <= 9; r++) {
                        final int rr = r;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            Location ringC = c.clone().add(0, 0.4, 0);
                            if (ringC.getWorld() != null) {
                                DisplayBuilder.particleRing(ringC, rr, Particle.SCULK_SOUL, 18 + rr * 2, null);
                                DisplayBuilder.particleRing(ringC, rr * 0.85, Particle.SNOWFLAKE, 12, null);
                            }
                        }, r * 3L);
                    }
                }
                // Continuous bell shimmer (3rd particle layer)
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.END_ROD, c.clone().add(0, 3.0, 0), 2, 0.6, 0.4, 0.6, 0.02);
                    DisplayBuilder.dustParticles(c.clone().add(0, 3.0, 0), 2, 0.5, 180, 220, 255, 1.0f);
                }
                if (aT % 60 == 30) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.8f);
            }
            // ===== DISSIPATE PHASE (final 40t): rings shrink in, bell shrinks to 0, crescendo =====
            else {
                int dT = tick - dissipateStart;
                if (dT == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.6f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.4f, 0.5f);
                }
                double t = dT / (double)DISSIPATE_PHASE_TICKS;
                float scale = (float)(1.2 - 1.2 * t);
                if (scale < 0.01f) scale = 0.01f;
                bell.animateTo(
                        new Vector3f(-scale / 2, 3.0f - scale / 2, -scale / 2),
                        new AxisAngle4f((float)(tick * 0.15), 0, 1, 0),
                        new Vector3f(scale), 4);
                for (ItemDisplayHandle h : rim) {
                    float rs = (float)(0.45 * (1 - t));
                    if (rs < 0.01f) rs = 0.01f;
                    h.animateTo(
                            new Vector3f(-rs / 2, 2.2f - rs / 2, -rs / 2),
                            new AxisAngle4f((float)(tick * 0.2), 0, 1, 0),
                            new Vector3f(rs), 4);
                }
                for (ItemDisplayHandle h : resonators) {
                    float rs = (float)(0.5 * (1 - t));
                    if (rs < 0.01f) rs = 0.01f;
                    h.animateTo(
                            new Vector3f(-rs / 2, 3.0f - rs / 2, -rs / 2),
                            new AxisAngle4f((float)(tick * 0.25), 0, 1, 0),
                            new Vector3f(rs), 4);
                }
                // Ring-shrink particle pulses (inward)
                if (dT % 8 == 0) {
                    double maxR = 9.0 * (1 - t);
                    for (double r = maxR; r > 0; r -= 1.5) {
                        DisplayBuilder.particleRing(c.clone().add(0, 0.4, 0), r, Particle.SCULK_SOUL, 16, null);
                    }
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.5f + (float)t * 0.5f);
                }
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 3.0, 0), 3, 0.4, 0.3, 0.4, 0.01);
                if (dT == DISSIPATE_PHASE_TICKS - 5) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 2.0f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.6f, 0.4f);
                }
            }

            // Damage applies throughout
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

        @Override public AbstractAttack newInstance() { return new EchoingFrostBell(plugin); }
    }

    // ================================================================
    // 42. DRIFTING FROST SPECTER [FOLLOW-AI 0.09] — Phased humanoid that
    //     materializes, drifts toward player with layered particle aura,
    //     then evaporates upward.
    // ================================================================
    public static class DriftingFrostSpecter extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> parts = new ArrayList<>();
        // Layout indices: 0-2 head, 3-5 body, 6-8 limbs
        private final double[] partOX = new double[9];
        private final double[] partOY = new double[9];
        private final double[] partOZ = new double[9];
        private final List<ItemDisplayHandle> aura = new ArrayList<>();

        public DriftingFrostSpecter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("drifting_frost_specter", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(43200.0);
            config.setDamageRadius(6.75);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
            // FOLLOW-AI: drift toward player at sub-walk pace.
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.09);
        }

        private float baseScale(int i) { return (i < 3) ? 0.6f : (i < 6) ? 0.7f : 0.5f; }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // SPAWN sound (materialization)
            DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 1.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.8f, 0.6f);

            // Humanoid silhouette skeleton
            partOX[0] = 0;     partOY[0] = 3.8; partOZ[0] = 0;     // head center
            partOX[1] = 0.25;  partOY[1] = 3.9; partOZ[1] = 0.1;   // head
            partOX[2] = -0.25; partOY[2] = 3.7; partOZ[2] = -0.1;  // head
            partOX[3] = 0;     partOY[3] = 2.5; partOZ[3] = 0;     // torso
            partOX[4] = 0.2;   partOY[4] = 2.0; partOZ[4] = 0;     // torso
            partOX[5] = -0.2;  partOY[5] = 1.6; partOZ[5] = 0;     // torso
            partOX[6] = 0.55;  partOY[6] = 2.2; partOZ[6] = 0;     // arm L
            partOX[7] = -0.55; partOY[7] = 2.2; partOZ[7] = 0;     // arm R
            partOX[8] = 0;     partOY[8] = 0.8; partOZ[8] = 0;     // legs

            for (int i = 0; i < 9; i++) {
                Location p = c.clone().add(partOX[i], partOY[i], partOZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 200, 240).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                parts.add(h);
                spawnedEntities.add(h.entity());
            }

            // Aura items: 6 ECHO_SHARD orbiting + 4 trail
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 1.4, 2.0 + Math.sin(a) * 0.6, Math.sin(a) * 1.4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.01f, 0.01f, 0.01f).glow(140, 180, 230).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                aura.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + 0.3;
                Location p = c.clone().add(Math.cos(a) * 0.8, 1.0 + i * 0.7, Math.sin(a) * 0.8);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.01f, 0.01f, 0.01f).glow(170, 210, 250).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                aura.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_PHASE_TICKS;

            // ===== SPAWN PHASE (0-40t): silhouette materializes scale 0 -> baseScale, SOUL_FIRE flicker =====
            if (tick < SPAWN_PHASE_TICKS) {
                double t = tick / (double)SPAWN_PHASE_TICKS;
                for (int i = 0; i < parts.size(); i++) {
                    float s = (float)(0.01 + (baseScale(i) - 0.01) * t);
                    parts.get(i).animateTo(
                            new Vector3f((float)partOX[i] - s / 2, (float)partOY[i] - s / 2, (float)partOZ[i] - s / 2),
                            new AxisAngle4f((float)(tick * 0.03 + i), 0, 1, 0),
                            new Vector3f(s), 4);
                }
                for (int i = 0; i < aura.size(); i++) {
                    float as = (float)(0.01 + 0.4 * t);
                    int auraIdx = i;
                    double aOff = (auraIdx < 6) ?
                            Math.PI * 2 * auraIdx / 6 :
                            Math.PI * 2 * (auraIdx - 6) / 4 + 0.3;
                    double rr = (auraIdx < 6) ? 1.4 : 0.8;
                    double yy = (auraIdx < 6) ? 2.0 + Math.sin(aOff) * 0.6 : 1.0 + (auraIdx - 6) * 0.7;
                    aura.get(i).animateTo(
                            new Vector3f((float)(Math.cos(aOff) * rr) - as / 2, (float)yy - as / 2, (float)(Math.sin(aOff) * rr) - as / 2),
                            new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0),
                            new Vector3f(as), 4);
                }
                // SOUL_FIRE_FLAME materialization flicker
                if (tick % 2 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 1.0;
                        double y = 0.5 + Math.random() * 3.5;
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(Math.cos(a) * rr, y, Math.sin(a) * rr),
                                1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
            // ===== ACTIVE PHASE: drift toward player, layered visuals =====
            else if (tick < dissipateStart) {
                int aT = tick - SPAWN_PHASE_TICKS;
                // Torso wobble (X-axis ±10° over 60t)
                double torsoWobble = Math.sin(aT * (Math.PI / 30.0)) * (10.0 * Math.PI / 180.0);

                for (int i = 0; i < parts.size(); i++) {
                    double bobY = partOY[i] + Math.sin(aT * 0.08 + i * 0.5) * 0.18;
                    double sway = Math.sin(aT * 0.05 + i) * 0.08;
                    float s = baseScale(i);
                    // Torso parts (3-5) get extra X wobble
                    boolean isTorso = (i >= 3 && i <= 5);
                    parts.get(i).animateTo(
                            new Vector3f((float)(partOX[i] + sway) - s / 2, (float)bobY - s / 2, (float)partOZ[i] - s / 2),
                            new AxisAngle4f((float)(isTorso ? torsoWobble : tick * 0.04 + i), isTorso ? 1 : 0, isTorso ? 0 : 1, 0),
                            new Vector3f(s), 4);
                }

                // SCULK_SOUL spiral around torso (8 particles every 4t)
                if (aT % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = (aT * 0.1) + (Math.PI * 2 * i / 8);
                        double rr = 0.9;
                        Location sp = c.clone().add(Math.cos(a) * rr, 2.0 + Math.sin(aT * 0.05 + i) * 0.3, Math.sin(a) * rr);
                        w.spawnParticle(Particle.SCULK_SOUL, sp, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }
                // SOUL_FIRE_FLAME flicker at head every 6t
                if (aT % 6 == 0) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 3.8, 0), 3, 0.25, 0.2, 0.25, 0.02);
                }
                // ELECTRIC_SPARK trail
                if (aT % 5 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 0.8;
                        w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(Math.cos(a) * rr, 1.5 + Math.random() * 2.0, Math.sin(a) * rr),
                                1, 0.1, 0.1, 0.1, 0.03);
                    }
                }
                if (aT % 40 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.8f, 0.4f);
                if (aT % 80 == 40) DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.5f, 0.6f);
            }
            // ===== DISSIPATE PHASE (final 40t): rapid flicker + rise upward + fade =====
            else {
                int dT = tick - dissipateStart;
                if (dT == 0) {
                    DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.4f, 0.5f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_HURT, 1.0f, 0.4f);
                }
                double t = dT / (double)DISSIPATE_PHASE_TICKS;
                // Flicker scale rapidly between 0.6 and 0.8 (early), then fade to 0
                boolean flickerHigh = (dT % 6) < 3;
                for (int i = 0; i < parts.size(); i++) {
                    float s = baseScale(i);
                    float ds;
                    if (t < 0.25) {
                        ds = flickerHigh ? s * 1.3f : s * 0.8f;
                    } else {
                        ds = (float)(s * (1 - (t - 0.25) / 0.75));
                    }
                    if (ds < 0.01f) ds = 0.01f;
                    // Rise upward
                    double riseY = partOY[i] + t * 5.0;
                    parts.get(i).animateTo(
                            new Vector3f((float)partOX[i] - ds / 2, (float)riseY - ds / 2, (float)partOZ[i] - ds / 2),
                            new AxisAngle4f((float)(tick * 0.2), 0, 1, 0),
                            new Vector3f(ds), 3);
                }
                for (int i = 0; i < aura.size(); i++) {
                    float ds = (float)(0.4 * (1 - t));
                    if (ds < 0.01f) ds = 0.01f;
                    aura.get(i).animateTo(
                            new Vector3f(-ds / 2, (float)(2.0 + t * 5.0) - ds / 2, -ds / 2),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(ds), 3);
                }
                // SCULK_SOUL evaporation burst
                if (dT % 3 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 1.5;
                        w.spawnParticle(Particle.SCULK_SOUL, c.clone().add(Math.cos(a) * rr, 1.5 + Math.random() * (3.0 + t * 4.0), Math.sin(a) * rr),
                                1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                if (dT == DISSIPATE_PHASE_TICKS - 5) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.3f);
                }
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

        @Override public AbstractAttack newInstance() { return new DriftingFrostSpecter(plugin); }
    }

    // ================================================================
    // 43. HUNTING COLD ZONE [FOLLOW-AI 0.08] — Phased: ring contracts from
    //     r=12 to r=8 -> hunts player with SONIC_BOOM pulses + SNOWFLAKE
    //     rain + ELECTRIC_SPARK -> collapses inward.
    // ================================================================
    public static class HuntingColdZone extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ring = new ArrayList<>();
        private final List<ItemDisplayHandle> interior = new ArrayList<>();
        private final double[] rAng = new double[24];
        private static final double RING_RADIUS = 8.0;
        private static final double SPAWN_START_R = 12.0;

        public HuntingColdZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunting_cold_zone", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(38400.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(420);
            config.setCooldownTicks(60);
            // FOLLOW-AI: zone hunts the player.
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.08);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // SPAWN sound (ominous chime)
            DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_AMBIENT, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.7f);

            // 24 BLUE_ICE perimeter ring, start at SPAWN_START_R
            for (int i = 0; i < 24; i++) {
                rAng[i] = Math.PI * 2 * i / 24;
                Location p = c.clone().add(Math.cos(rAng[i]) * SPAWN_START_R, 0.4, Math.sin(rAng[i]) * SPAWN_START_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(120, 180, 255).interpolation(8, 0);
                ring.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior accents grow with the ring
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 4.0;
                Location p = c.clone().add(Math.cos(a) * rr, 1.6 + Math.sin(a) * 0.4, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.01f, 0.01f, 0.01f).glow(140, 180, 230).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                interior.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 1.0, 1.0 + Math.sin(a) * 0.3, Math.sin(a) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.01f, 0.01f, 0.01f).glow(160, 200, 250).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                interior.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_PHASE_TICKS;

            // ===== SPAWN PHASE (0-60t): ring contracts r=12 -> r=8, scale 0 -> 0.5 =====
            if (tick < 60) {
                double t = Math.min(1.0, tick / 60.0);
                double currentR = SPAWN_START_R + (RING_RADIUS - SPAWN_START_R) * t;
                float scale = (float)(0.01 + 0.49 * t);
                for (int i = 0; i < ring.size(); i++) {
                    rAng[i] += 0.005;
                    float bx = (float)(Math.cos(rAng[i]) * currentR);
                    float bz = (float)(Math.sin(rAng[i]) * currentR);
                    ring.get(i).animateTo(
                            new Vector3f(bx - scale / 2, 0.4f - scale / 2, bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.03 + i * 0.2), 0, 1, 0),
                            new Vector3f(scale), 6);
                }
                // Interior grows
                if (tick < SPAWN_PHASE_TICKS) {
                    double it2 = tick / (double)SPAWN_PHASE_TICKS;
                    for (int i = 0; i < interior.size(); i++) {
                        float is = (float)(0.01 + 0.54 * it2);
                        if (i < 8) {
                            double a = Math.PI * 2 * i / 8;
                            interior.get(i).animateTo(
                                    new Vector3f((float)(Math.cos(a) * 4.0) - is / 2, (float)(1.6 + Math.sin(a) * 0.4) - is / 2, (float)(Math.sin(a) * 4.0) - is / 2),
                                    new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                                    new Vector3f(is), 4);
                        } else {
                            int idx = i - 8;
                            double a = Math.PI * 2 * idx / 6;
                            interior.get(i).animateTo(
                                    new Vector3f((float)(Math.cos(a) * 1.0) - is / 2, (float)(1.0 + Math.sin(a) * 0.3) - is / 2, (float)(Math.sin(a) * 1.0) - is / 2),
                                    new AxisAngle4f((float)(tick * 0.05 + idx), 0, 1, 0),
                                    new Vector3f(is), 4);
                        }
                    }
                }
                // Contracting frost mist
                if (tick % 3 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(Math.cos(a) * currentR, 0.4 + Math.random() * 0.6, Math.sin(a) * currentR),
                                1, 0.2, 0.2, 0.2, 0.02);
                    }
                }
                if (tick == 30) DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 0.7f);
            }
            // ===== ACTIVE PHASE: ring slides toward player, layered FX =====
            else if (tick < dissipateStart) {
                int aT = tick - 60;
                // Ring rotation + bob
                for (int i = 0; i < ring.size(); i++) {
                    rAng[i] += 0.012;
                    float bx = (float)(Math.cos(rAng[i]) * RING_RADIUS);
                    float bz = (float)(Math.sin(rAng[i]) * RING_RADIUS);
                    float by = (float)(0.4 + Math.sin(aT * 0.05 + i) * 0.2);
                    ring.get(i).animateTo(
                            new Vector3f(bx - 0.25f, by - 0.25f, bz - 0.25f),
                            new AxisAngle4f((float)(tick * 0.04 + i * 0.2), 0, 1, 0),
                            new Vector3f(0.5f), 6);
                }

                // 3 layered visuals:
                // Layer 1: SONIC_BOOM pulse every 30t (inward wave)
                if (aT % 30 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 0.8f);
                    for (int r = 8; r >= 1; r--) {
                        final int rr = r;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            Location ringC = getCenter();
                            if (ringC != null && ringC.getWorld() != null) {
                                DisplayBuilder.particleRing(ringC.clone().add(0, 0.5, 0), rr, Particle.SONIC_BOOM, 12, null);
                            }
                        }, (8 - r) * 2L);
                    }
                }
                // Layer 2: SNOWFLAKE constant rain (8 per tick inside ring)
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double rr = Math.random() * RING_RADIUS;
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(Math.cos(a) * rr, 0.5 + Math.random() * 1.5, Math.sin(a) * rr),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
                // Layer 3: ELECTRIC_SPARK at ring items every 8t
                if (aT % 8 == 0) {
                    for (int i = 0; i < ring.size(); i += 3) {
                        Location p = c.clone().add(Math.cos(rAng[i]) * RING_RADIUS, 0.4, Math.sin(rAng[i]) * RING_RADIUS);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                if (aT % 50 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_AMBIENT, 0.9f, 0.5f);
            }
            // ===== DISSIPATE PHASE: ring collapses inward + finale =====
            else {
                int dT = tick - dissipateStart;
                if (dT == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.7f);
                }
                double t = Math.min(1.0, dT / (double)DISSIPATE_PHASE_TICKS);
                double currentR = RING_RADIUS * (1 - t);
                if (currentR < 0.1) currentR = 0.1;
                float scale = (float)(0.5 * (1 - t));
                if (scale < 0.01f) scale = 0.01f;
                for (int i = 0; i < ring.size(); i++) {
                    rAng[i] += 0.05; // faster spin during collapse
                    float bx = (float)(Math.cos(rAng[i]) * currentR);
                    float bz = (float)(Math.sin(rAng[i]) * currentR);
                    ring.get(i).animateTo(
                            new Vector3f(bx - scale / 2, 0.4f - scale / 2, bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.2 + i), 0, 1, 0),
                            new Vector3f(scale), 3);
                }
                for (ItemDisplayHandle h : interior) {
                    float is = (float)(0.55 * (1 - t));
                    if (is < 0.01f) is = 0.01f;
                    h.animateTo(
                            new Vector3f(-is / 2, 0.4f - is / 2, -is / 2),
                            new AxisAngle4f((float)(tick * 0.25), 0, 1, 0),
                            new Vector3f(is), 3);
                }
                if (dT == DISSIPATE_PHASE_TICKS - 5) {
                    // Final cascade
                    Location ic = c.clone().add(0, 0.4, 0);
                    DisplayBuilder.playSound(ic, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.5f);
                    DisplayBuilder.playSound(ic, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, ic, 1, 0, 0, 0, 0);
                    // END_ROD column upward
                    for (double y = 0; y < 10; y += 0.5) {
                        w.spawnParticle(Particle.END_ROD, c.clone().add(0, y, 0), 2, 0.2, 0.1, 0.2, 0.05);
                    }
                }
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

        @Override public AbstractAttack newInstance() { return new HuntingColdZone(plugin); }
    }

    // ================================================================
    // 44. PROWLING BLIZZARD ORB — Phased: 3-axis orbital sphere with
    //     slow orbit -> speedup -> implode finale.
    // ================================================================
    public static class ProwlingBlizzardOrb extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> orbs = new ArrayList<>();
        private final List<ItemDisplayHandle> core = new ArrayList<>();
        private final double[] oAng = new double[16];
        private final int[] oAxis = new int[16]; // 0=X-axis orbit, 1=Y-axis orbit, 2=Z-axis orbit
        private double offX = 0, offZ = 0;
        private double driftAng = Math.random() * Math.PI * 2;
        private static final double ORB_R = 2.6;
        private static final double ORB_CENTER_Y = 2.2;

        public ProwlingBlizzardOrb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prowling_blizzard_orb", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(40800.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // SPAWN sound (low rumble + reverse-pitched rain)
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.2f, 0.8f);

            for (int i = 0; i < 16; i++) {
                oAxis[i] = i % 3;
                oAng[i] = Math.PI * 2 * (i / 3.0) / 5.5;
                Location p = computeOrbPos(c, i);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.01f, 0.01f, 0.01f).glow(220, 240, 255).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                orbs.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner core (also grow in)
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 0.6, ORB_CENTER_Y + Math.sin(a) * 0.4, Math.sin(a) * 0.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 230, 255).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                core.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * ORB_R, ORB_CENTER_Y, Math.sin(a) * ORB_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_STAR));
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 220, 255).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                core.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        private Location computeOrbPos(Location c, int i) {
            double a = oAng[i];
            double bx, by, bz;
            switch (oAxis[i]) {
                case 0: bx = 0; by = ORB_CENTER_Y + Math.cos(a) * ORB_R; bz = Math.sin(a) * ORB_R; break;
                case 1: bx = Math.cos(a) * ORB_R; by = ORB_CENTER_Y; bz = Math.sin(a) * ORB_R; break;
                default: bx = Math.cos(a) * ORB_R; by = ORB_CENTER_Y + Math.sin(a) * ORB_R; bz = 0; break;
            }
            return c.clone().add(offX + bx, by, offZ + bz);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_PHASE_TICKS;

            // Random walk drift (apply only during active/slow phases)
            if (tick > SPAWN_PHASE_TICKS && tick < dissipateStart) {
                if (tick % 60 == 0) driftAng += (Math.random() - 0.5) * Math.PI;
                offX += Math.cos(driftAng) * 0.04;
                offZ += Math.sin(driftAng) * 0.04;
                double dd = Math.sqrt(offX * offX + offZ * offZ);
                if (dd > 8.0) {
                    offX *= 8.0 / dd;
                    offZ *= 8.0 / dd;
                    driftAng = Math.atan2(-offZ, -offX);
                }
            }

            // ===== SPAWN PHASE (0-40t): materialize scale 0 -> 0.5 =====
            if (tick < SPAWN_PHASE_TICKS) {
                double t = tick / (double)SPAWN_PHASE_TICKS;
                float scale = (float)(0.01 + 0.49 * t);
                for (int i = 0; i < orbs.size(); i++) {
                    oAng[i] += 0.02; // slow initial spin
                    double a = oAng[i];
                    double bx, by, bz;
                    switch (oAxis[i]) {
                        case 0: bx = 0; by = ORB_CENTER_Y + Math.cos(a) * ORB_R; bz = Math.sin(a) * ORB_R; break;
                        case 1: bx = Math.cos(a) * ORB_R; by = ORB_CENTER_Y; bz = Math.sin(a) * ORB_R; break;
                        default: bx = Math.cos(a) * ORB_R; by = ORB_CENTER_Y + Math.sin(a) * ORB_R; bz = 0; break;
                    }
                    orbs.get(i).animateTo(
                            new Vector3f((float)bx - scale / 2, (float)by - scale / 2, (float)bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(scale), 4);
                }
                for (ItemDisplayHandle h : core) {
                    float cs = (float)(0.01 + 0.39 * t);
                    h.animateTo(
                            new Vector3f(-cs / 2, (float)ORB_CENTER_Y - cs / 2, -cs / 2),
                            new AxisAngle4f((float)(tick * 0.08), 0, 1, 0),
                            new Vector3f(cs), 4);
                }
                if (tick % 3 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 2.0;
                        w.spawnParticle(Particle.CLOUD, c.clone().add(Math.cos(a) * rr, ORB_CENTER_Y + (Math.random() - 0.5) * 2, Math.sin(a) * rr),
                                1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }
            // ===== ACTIVE PHASE: 3 sub-phases =====
            else if (tick < dissipateStart) {
                int aT = tick - SPAWN_PHASE_TICKS;
                // Sub-phase 1: slow orbit (0-60), Sub-phase 2: speedup (60-110), Sub-phase 3: implode (110-160)
                double spinSpeed;
                double radiusMul;
                float currentScale = 0.5f;
                int subphase;
                if (aT < 60) {
                    subphase = 0;
                    spinSpeed = 0.04;
                    radiusMul = 1.0;
                } else if (aT < 110) {
                    subphase = 1;
                    double t = (aT - 60) / 50.0;
                    spinSpeed = 0.04 + (0.10 - 0.04) * t;
                    radiusMul = 1.0;
                    currentScale = (float)(0.5 + 0.15 * t);
                } else {
                    subphase = 2;
                    double t = Math.min(1.0, (aT - 110) / 50.0);
                    spinSpeed = 0.10 + 0.10 * t;
                    radiusMul = 1.0 - t; // 3 -> 0
                    currentScale = (float)(0.65 * (1 - t * 0.7));
                }

                for (int i = 0; i < orbs.size(); i++) {
                    oAng[i] += spinSpeed;
                    double a = oAng[i];
                    double r = ORB_R * radiusMul;
                    double bx, by, bz;
                    switch (oAxis[i]) {
                        case 0: bx = 0; by = ORB_CENTER_Y + Math.cos(a) * r; bz = Math.sin(a) * r; break;
                        case 1: bx = Math.cos(a) * r; by = ORB_CENTER_Y; bz = Math.sin(a) * r; break;
                        default: bx = Math.cos(a) * r; by = ORB_CENTER_Y + Math.sin(a) * r; bz = 0; break;
                    }
                    orbs.get(i).animateTo(
                            new Vector3f((float)(offX + bx) - currentScale / 2, (float)by - currentScale / 2, (float)(offZ + bz) - currentScale / 2),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(currentScale), 2);
                }
                // Core anchor pulses with subphase
                for (int j = 0; j < core.size(); j++) {
                    double a = Math.PI * 2 * j / core.size() + tick * 0.04;
                    double cr = (j < 4) ? 0.6 : ORB_R * Math.max(0.2, radiusMul);
                    float cs = (j < 4) ? 0.4f : 0.4f;
                    core.get(j).animateTo(
                            new Vector3f((float)(offX + Math.cos(a) * cr) - cs / 2, (float)(ORB_CENTER_Y) - cs / 2, (float)(offZ + Math.sin(a) * cr) - cs / 2),
                            new AxisAngle4f((float)(tick * 0.1), 0, 1, 0),
                            new Vector3f(cs), 4);
                }

                // Particles by subphase
                if (subphase == 0) {
                    if (aT % 2 == 0) {
                        for (int i = 0; i < 4; i++) {
                            double a = Math.random() * Math.PI * 2;
                            double rr = Math.random() * ORB_R;
                            w.spawnParticle(Particle.CLOUD, c.clone().add(offX + Math.cos(a) * rr, ORB_CENTER_Y + (Math.random() - 0.5) * 2, offZ + Math.sin(a) * rr),
                                    1, 0.1, 0.1, 0.1, 0.02);
                            if (Math.random() < 0.4) w.spawnParticle(Particle.SNOWFLAKE,
                                    c.clone().add(offX + Math.cos(a) * rr, ORB_CENTER_Y, offZ + Math.sin(a) * rr), 1, 0.1, 0.1, 0.1, 0.01);
                        }
                    }
                    if (aT % 40 == 0) DisplayBuilder.playSound(c.clone().add(offX, ORB_CENTER_Y, offZ), Sound.WEATHER_RAIN, 0.8f, 0.5f);
                } else if (subphase == 1) {
                    // Speedup: ELECTRIC_SPARK at intersection points + brighter glow
                    if (aT % 2 == 0) {
                        for (int i = 0; i < orbs.size(); i += 2) {
                            double a = oAng[i];
                            double bx, by, bz;
                            switch (oAxis[i]) {
                                case 0: bx = 0; by = ORB_CENTER_Y + Math.cos(a) * ORB_R; bz = Math.sin(a) * ORB_R; break;
                                case 1: bx = Math.cos(a) * ORB_R; by = ORB_CENTER_Y; bz = Math.sin(a) * ORB_R; break;
                                default: bx = Math.cos(a) * ORB_R; by = ORB_CENTER_Y + Math.sin(a) * ORB_R; bz = 0; break;
                            }
                            w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(offX + bx, by, offZ + bz), 2, 0.1, 0.1, 0.1, 0.05);
                        }
                    }
                    if (aT % 20 == 0) {
                        float pitch = 0.7f + ((aT - 60) / 50f) * 0.7f;
                        DisplayBuilder.playSound(c.clone().add(offX, ORB_CENTER_Y, offZ), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, pitch);
                    }
                } else {
                    // Implode: pull particles inward
                    if (aT % 1 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double a = Math.random() * Math.PI * 2;
                            double rr = Math.random() * ORB_R * 1.5;
                            w.spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(offX + Math.cos(a) * rr, ORB_CENTER_Y + (Math.random() - 0.5) * 3, offZ + Math.sin(a) * rr),
                                    1, 0.05, 0.05, 0.05, 0.06);
                        }
                    }
                    if (aT % 10 == 0) {
                        DisplayBuilder.playSound(c.clone().add(offX, ORB_CENTER_Y, offZ), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.6f);
                    }
                }
            }
            // ===== DISSIPATE PHASE (final 40t): central burst then fade =====
            else {
                int dT = tick - dissipateStart;
                if (dT == 0) {
                    Location burst = c.clone().add(offX, ORB_CENTER_Y, offZ);
                    DisplayBuilder.playSound(burst, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.7f);
                    DisplayBuilder.playSound(burst, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.8f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, burst, 2, 0.3, 0.3, 0.3, 0);
                    for (int r = 1; r <= 6; r++) {
                        final int rr = r;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            Location bc = getCenter();
                            if (bc != null && bc.getWorld() != null) {
                                DisplayBuilder.particleRing(bc.clone().add(offX, ORB_CENTER_Y, offZ), rr, Particle.END_ROD, 16, null);
                                DisplayBuilder.particleRing(bc.clone().add(offX, ORB_CENTER_Y, offZ), rr * 0.9, Particle.ELECTRIC_SPARK, 14, null);
                            }
                        }, r * 3L);
                    }
                }
                double t = dT / (double)DISSIPATE_PHASE_TICKS;
                float scale = (float)(0.5 * (1 - t));
                if (scale < 0.01f) scale = 0.01f;
                for (int i = 0; i < orbs.size(); i++) {
                    orbs.get(i).animateTo(
                            new Vector3f((float)offX - scale / 2, (float)ORB_CENTER_Y - scale / 2, (float)offZ - scale / 2),
                            new AxisAngle4f((float)(tick * 0.4 + i), 1, 0.5f, 0),
                            new Vector3f(scale), 4);
                }
                for (ItemDisplayHandle h : core) {
                    h.animateTo(
                            new Vector3f((float)offX - scale / 2, (float)ORB_CENTER_Y - scale / 2, (float)offZ - scale / 2),
                            new AxisAngle4f((float)(tick * 0.3), 0, 1, 0),
                            new Vector3f(scale), 4);
                }
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                Location dmgC = c.clone().add(offX, 0, offZ);
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(dmgC) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ProwlingBlizzardOrb(plugin); }
    }

    // ================================================================
    // 45. ROAMING HOARFROST BEAST [FOLLOW-AI 0.11] — Phased quadruped:
    //     materialize -> stalk with trot gait + tail whip + halo aura
    //     -> slow + dissipate.
    // ================================================================
    public static class RoamingHoarfrostBeast extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> parts = new ArrayList<>();
        private final List<ItemDisplayHandle> accents = new ArrayList<>();
        // 12-part layout: 0 head, 1 body, 2-5 legs (FL, FR, BL, BR), 6-7 horns, 8 tail, 9 jaw, 10-11 eyes
        private final double[] partOX = new double[12];
        private final double[] partOY = new double[12];
        private final double[] partOZ = new double[12];
        private final float[] partSX = new float[12];
        private final float[] partSY = new float[12];
        private final float[] partSZ = new float[12];

        public RoamingHoarfrostBeast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("roaming_hoarfrost_beast", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(45600.0);
            config.setDamageRadius(8.25);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(380);
            config.setCooldownTicks(60);
            // FOLLOW-AI: quadruped predator stalks player.
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.11);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // SPAWN sound (deep roar)
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.3f);
            DisplayBuilder.playSound(c, Sound.ENTITY_POLAR_BEAR_WARNING, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 0.6f);

            // Head
            partOX[0] = 1.6;  partOY[0] = 2.0; partOZ[0] = 0; partSX[0] = 1.0f; partSY[0] = 1.0f; partSZ[0] = 1.0f;
            // Body
            partOX[1] = 0;    partOY[1] = 1.8; partOZ[1] = 0; partSX[1] = 1.8f; partSY[1] = 1.2f; partSZ[1] = 1.1f;
            // Legs (FL FR BL BR)
            partOX[2] = 0.8;  partOY[2] = 0.7; partOZ[2] = 0.5;  partSX[2] = 0.4f; partSY[2] = 1.2f; partSZ[2] = 0.4f;
            partOX[3] = 0.8;  partOY[3] = 0.7; partOZ[3] = -0.5; partSX[3] = 0.4f; partSY[3] = 1.2f; partSZ[3] = 0.4f;
            partOX[4] = -0.8; partOY[4] = 0.7; partOZ[4] = 0.5;  partSX[4] = 0.4f; partSY[4] = 1.2f; partSZ[4] = 0.4f;
            partOX[5] = -0.8; partOY[5] = 0.7; partOZ[5] = -0.5; partSX[5] = 0.4f; partSY[5] = 1.2f; partSZ[5] = 0.4f;
            // Horns
            partOX[6] = 1.7;  partOY[6] = 2.7; partOZ[6] = 0.35; partSX[6] = 0.25f; partSY[6] = 0.7f; partSZ[6] = 0.25f;
            partOX[7] = 1.7;  partOY[7] = 2.7; partOZ[7] = -0.35; partSX[7] = 0.25f; partSY[7] = 0.7f; partSZ[7] = 0.25f;
            // Tail (rear)
            partOX[8] = -1.5; partOY[8] = 1.9; partOZ[8] = 0;   partSX[8] = 0.8f; partSY[8] = 0.3f; partSZ[8] = 0.3f;
            // Jaw
            partOX[9] = 2.0;  partOY[9] = 1.7; partOZ[9] = 0;   partSX[9] = 0.6f; partSY[9] = 0.3f; partSZ[9] = 0.8f;
            // Eyes
            partOX[10] = 1.85; partOY[10] = 2.2; partOZ[10] = 0.25; partSX[10] = 0.2f; partSY[10] = 0.2f; partSZ[10] = 0.2f;
            partOX[11] = 1.85; partOY[11] = 2.2; partOZ[11] = -0.25; partSX[11] = 0.2f; partSY[11] = 0.2f; partSZ[11] = 0.2f;

            for (int i = 0; i < 12; i++) {
                Location p = c.clone().add(partOX[i], partOY[i], partOZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(190, 220, 255).interpolation(SPAWN_PHASE_TICKS - 10, 0);
                parts.add(h);
                spawnedEntities.add(h.entity());
            }

            // Accents (humps + claws)
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 0.5, 2.4, Math.sin(a) * 0.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 220, 240).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                accents.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double xx = (i < 2 ? 0.8 : -0.8);
                double zz = (i % 2 == 0 ? 0.55 : -0.55);
                Location p = c.clone().add(xx, 0.2, zz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(150, 200, 255).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                accents.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_PHASE_TICKS;
            int spawnEnd = 30;

            // ===== SPAWN PHASE (0-30t): materialize scale 0 -> base, ice shards rising =====
            if (tick < spawnEnd) {
                double t = tick / (double)spawnEnd;
                for (int i = 0; i < parts.size(); i++) {
                    float sx = (float)(0.01 + (partSX[i] - 0.01) * t);
                    float sy = (float)(0.01 + (partSY[i] - 0.01) * t);
                    float sz = (float)(0.01 + (partSZ[i] - 0.01) * t);
                    parts.get(i).animateTo(
                            new Vector3f((float)partOX[i] - sx / 2, (float)partOY[i] - sy / 2, (float)partOZ[i] - sz / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(sx, sy, sz), 4);
                }
                for (int i = 0; i < accents.size(); i++) {
                    float as = (i < 6) ? (float)(0.01 + 0.39 * t) : (float)(0.01 + 0.24 * t);
                    accents.get(i).animateTo(
                            new Vector3f(-as / 2, ((i < 6) ? 2.4f : 0.2f) - as / 2, -as / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(as, (i < 6) ? as * 0.75f : as * 0.8f, as), 4);
                }
                if (tick % 2 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 2.0;
                        w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(Math.cos(a) * rr, Math.random() * 3.0, Math.sin(a) * rr),
                                1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }
            // ===== ACTIVE PHASE: stalk with quadruped trot gait + aura =====
            else if (tick < dissipateStart) {
                int aT = tick - spawnEnd;
                // Trot gait: diagonal pairs (FL+BR vs FR+BL) over 16t cycle
                double phase = (aT % 16) / 16.0;
                double swing = Math.sin(phase * Math.PI * 2);
                double swingAlt = Math.sin(phase * Math.PI * 2 + Math.PI);
                double[] legBob = {
                        0, 0,                                     // head, body
                        swing * 0.35, swingAlt * 0.35,            // FL, FR
                        swingAlt * 0.35, swing * 0.35,            // BL, BR  (diagonal pairs FL+BR, FR+BL)
                        0, 0,                                     // horns
                        0, 0, 0, 0                                // tail, jaw, eyes
                };
                // Tail whip every 30t (rapid 90° swing)
                double tailWhip = ((aT % 30) < 8) ? Math.sin(((aT % 30) / 8.0) * Math.PI) * 1.4 : 0;
                // Head sway + body wave
                double headSway = Math.sin(aT * 0.08) * 0.1;
                double bodyWave = Math.sin(aT * 0.1) * 0.05;

                for (int i = 0; i < parts.size(); i++) {
                    double bobY = partOY[i] + legBob[i] + bodyWave;
                    double swayZ = (i == 0 || i == 9 || i == 10 || i == 11) ? headSway : 0;
                    // Forward stride: legs swing X-axis slightly with gait
                    double strideX = 0;
                    if (i >= 2 && i <= 5) {
                        // Diagonal pairs: FL(i=2) + BR(i=5) swing together; FR(i=3) + BL(i=4) swing opposite
                        boolean pairA = (i == 2 || i == 5);
                        strideX = pairA ? swing * 0.35 : swingAlt * 0.35;
                    }
                    double tailX = (i == 8) ? Math.sin(tailWhip) * 0.6 : 0;
                    double tailZ = (i == 8) ? tailWhip * 0.4 : 0;

                    parts.get(i).animateTo(
                            new Vector3f((float)(partOX[i] + strideX + tailX) - partSX[i] / 2,
                                    (float)bobY - partSY[i] / 2,
                                    (float)(partOZ[i] + swayZ + tailZ) - partSZ[i] / 2),
                            new AxisAngle4f((float)(Math.sin(aT * 0.06 + i) * 0.06), 0, 1, 0),
                            new Vector3f(partSX[i], partSY[i], partSZ[i]), 4);
                }

                // 3 layered particles:
                // Layer 1: SCULK_SOUL halo (8 per tick around body)
                for (int i = 0; i < 8; i++) {
                    double a = (aT * 0.05) + (Math.PI * 2 * i / 8);
                    w.spawnParticle(Particle.SCULK_SOUL, c.clone().add(Math.cos(a) * 1.4, 1.8 + Math.sin(aT * 0.03 + i) * 0.4, Math.sin(a) * 1.4),
                            1, 0.05, 0.05, 0.05, 0.005);
                }
                // Layer 2: ELECTRIC_SPARK eyes (2 per tick at head)
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(1.85, 2.2, 0.25), 1, 0.05, 0.05, 0.05, 0.02);
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(1.85, 2.2, -0.25), 1, 0.05, 0.05, 0.05, 0.02);
                // Layer 3: DUST cold-blue trail at feet
                if (aT % 3 == 0) {
                    for (int i = 2; i <= 5; i++) {
                        Location foot = c.clone().add(partOX[i], 0.3, partOZ[i]);
                        DisplayBuilder.dustParticles(foot, 2, 0.2, 150, 200, 255, 1.2f);
                    }
                }
                // Bonus mouth breath
                if (aT % 4 == 0) {
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(2.2, 1.9, 0), 3, 0.25, 0.2, 0.25, 0.04);
                }
                // Step sounds with gait
                if (aT % 16 == 0 || aT % 16 == 8) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 1.4f, 0.4f);
                }
                if (aT % 50 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.9f, 0.3f);
            }
            // ===== DISSIPATE PHASE (final 40t): slow then rapid fade + ground pulse =====
            else {
                int dT = tick - dissipateStart;
                if (dT == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_DEATH, 1.4f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_HURT, 1.2f, 0.3f);
                }
                double t = dT / (double)DISSIPATE_PHASE_TICKS;
                for (int i = 0; i < parts.size(); i++) {
                    float sx = (float)(partSX[i] * (1 - t));
                    float sy = (float)(partSY[i] * (1 - t));
                    float sz = (float)(partSZ[i] * (1 - t));
                    if (sx < 0.01f) sx = 0.01f;
                    if (sy < 0.01f) sy = 0.01f;
                    if (sz < 0.01f) sz = 0.01f;
                    parts.get(i).animateTo(
                            new Vector3f((float)partOX[i] - sx / 2, (float)partOY[i] - sy / 2, (float)partOZ[i] - sz / 2),
                            new AxisAngle4f((float)(tick * 0.1), 0, 1, 0),
                            new Vector3f(sx, sy, sz), 4);
                }
                for (int i = 0; i < accents.size(); i++) {
                    float as = (float)((i < 6 ? 0.4 : 0.25) * (1 - t));
                    if (as < 0.01f) as = 0.01f;
                    accents.get(i).animateTo(
                            new Vector3f(-as / 2, ((i < 6) ? 2.4f : 0.2f) - as / 2, -as / 2),
                            new AxisAngle4f((float)(tick * 0.1), 0, 1, 0),
                            new Vector3f(as), 4);
                }
                // SCULK_SOUL ground pulse
                if (dT % 5 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 1 + dT * 0.1, Particle.SCULK_SOUL, 20, null);
                }
                if (dT == DISSIPATE_PHASE_TICKS - 5) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
                }
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

        @Override public AbstractAttack newInstance() { return new RoamingHoarfrostBeast(plugin); }
    }

    // ================================================================
    // 46. SHIVERING CHAINPETALS — Phased: chain materializes staggered ->
    //     undulating snake motion with electric current -> compress to
    //     center.
    // ================================================================
    public static class ShiveringChainpetals extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> petals = new ArrayList<>();
        private final List<ItemDisplayHandle> accents = new ArrayList<>();
        private double chainAng = 0;
        private static final int LINK_COUNT = 20;
        private static final double CHAIN_LEN = 12.0;

        public ShiveringChainpetals(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shivering_chainpetals", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(31200.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(340);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // SPAWN sound (chain ignition)
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SWEET_BERRY_BUSH_PLACE, 1.2f, 1.2f);
            DisplayBuilder.playSound(c, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.8f, 1.0f);

            chainAng = Math.random() * Math.PI * 2;
            double cosA = Math.cos(chainAng);
            double sinA = Math.sin(chainAng);
            double step = CHAIN_LEN / (LINK_COUNT - 1);
            double startOff = -CHAIN_LEN / 2;
            for (int i = 0; i < LINK_COUNT; i++) {
                double t = startOff + step * i;
                Location p = c.clone().add(cosA * t, 1.2, sinA * t);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 240, 180).interpolation(4, 0);
                petals.add(h);
                spawnedEntities.add(h.entity());
            }

            // Accents grow with chain
            for (int i = 0; i < 6; i++) {
                double t = startOff + step * (i * 3.5);
                Location p = c.clone().add(cosA * t, 1.6, sinA * t);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 240, 200).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                accents.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double sign = (i < 2 ? -1 : 1);
                double off = sign * (CHAIN_LEN / 2 + 0.6 + (i % 2) * 0.4);
                Location p = c.clone().add(cosA * off, 1.2, sinA * off);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.01f, 0.01f, 0.01f).glow(220, 250, 200).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                accents.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_PHASE_TICKS;

            double cosA = Math.cos(chainAng);
            double sinA = Math.sin(chainAng);
            double pCosA = -sinA;
            double pSinA = cosA;
            double step = CHAIN_LEN / (LINK_COUNT - 1);
            double startOff = -CHAIN_LEN / 2;

            // ===== SPAWN PHASE (0-40t): staggered materialize from one end =====
            if (tick < SPAWN_PHASE_TICKS) {
                double phase = tick / (double)SPAWN_PHASE_TICKS;
                for (int i = 0; i < LINK_COUNT; i++) {
                    // Each link spawns when phase exceeds (i / LINK_COUNT)
                    double linkPhase = (double)i / LINK_COUNT;
                    double t = Math.max(0, Math.min(1.0, (phase - linkPhase) * 3.0));
                    float s = (float)(0.01 + 0.59 * t);
                    double off = startOff + step * i;
                    petals.get(i).animateTo(
                            new Vector3f((float)(cosA * off) - s / 2, 1.2f - s / 2, (float)(sinA * off) - s / 2),
                            new AxisAngle4f((float)(tick * 0.06 + i), 0, 1, 0),
                            new Vector3f(s), 3);
                }
                for (int i = 0; i < accents.size(); i++) {
                    float as = (float)(0.01 + 0.39 * phase);
                    accents.get(i).animateTo(
                            new Vector3f(-as / 2, 1.2f - as / 2, -as / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(as), 4);
                }
                // Chime as each link spawns
                if (tick % 2 == 0) {
                    int idx = (int)(phase * LINK_COUNT);
                    if (idx < LINK_COUNT) {
                        double off = startOff + step * idx;
                        DisplayBuilder.playSound(c.clone().add(cosA * off, 1.2, sinA * off), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f + idx * 0.04f);
                    }
                }
                if (tick % 3 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double a = Math.random() * Math.PI * 2;
                        w.spawnParticle(Particle.ENCHANT, c.clone().add(Math.cos(a) * 2.0, 1.2, Math.sin(a) * 2.0),
                                1, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }
            // ===== ACTIVE PHASE: serpentine snake undulation with current =====
            else if (tick < dissipateStart) {
                int aT = tick - SPAWN_PHASE_TICKS;
                // Each item's Y position follows sin(tick*0.1 + index*0.5)*0.6
                for (int i = 0; i < LINK_COUNT; i++) {
                    double t = startOff + step * i;
                    double snake = Math.sin(i * 0.5 - aT * 0.15) * 1.5;
                    double bobY = Math.sin(aT * 0.1 + i * 0.5) * 0.6;
                    double bx = cosA * t + pCosA * snake;
                    double bz = sinA * t + pSinA * snake;
                    petals.get(i).animateTo(
                            new Vector3f((float)bx - 0.3f, (float)(1.2 + bobY) - 0.3f, (float)bz - 0.3f),
                            new AxisAngle4f((float)(aT * 0.04 + i), 0, 1, 0),
                            new Vector3f(0.6f), 4);
                }
                // 3 layered particles:
                // Layer 1: SCULK_SOUL trail per link (1 per 4t)
                if (aT % 4 == 0) {
                    for (int i = 0; i < LINK_COUNT; i++) {
                        double t = startOff + step * i;
                        double snake = Math.sin(i * 0.5 - aT * 0.15) * 1.5;
                        double bobY = Math.sin(aT * 0.1 + i * 0.5) * 0.6;
                        w.spawnParticle(Particle.SCULK_SOUL, c.clone().add(cosA * t + pCosA * snake, 1.2 + bobY, sinA * t + pSinA * snake),
                                1, 0.05, 0.05, 0.05, 0.005);
                    }
                }
                // Layer 2: GLOW halos
                if (aT % 6 == 0) {
                    for (int i = 0; i < LINK_COUNT; i += 2) {
                        double t = startOff + step * i;
                        double snake = Math.sin(i * 0.5 - aT * 0.15) * 1.5;
                        double bobY = Math.sin(aT * 0.1 + i * 0.5) * 0.6;
                        w.spawnParticle(Particle.GLOW, c.clone().add(cosA * t + pCosA * snake, 1.2 + bobY, sinA * t + pSinA * snake),
                                2, 0.15, 0.15, 0.15, 0.01);
                    }
                }
                // Layer 3: ELECTRIC_SPARK between adjacent links
                if (aT % 3 == 0) {
                    for (int i = 0; i < LINK_COUNT - 1; i++) {
                        double t1 = startOff + step * i;
                        double t2 = startOff + step * (i + 1);
                        double snake1 = Math.sin(i * 0.5 - aT * 0.15) * 1.5;
                        double snake2 = Math.sin((i + 1) * 0.5 - aT * 0.15) * 1.5;
                        double bob1 = Math.sin(aT * 0.1 + i * 0.5) * 0.6;
                        double bob2 = Math.sin(aT * 0.1 + (i + 1) * 0.5) * 0.6;
                        double midX = (cosA * t1 + pCosA * snake1 + cosA * t2 + pCosA * snake2) * 0.5;
                        double midZ = (sinA * t1 + pSinA * snake1 + sinA * t2 + pSinA * snake2) * 0.5;
                        double midY = (bob1 + bob2) * 0.5 + 1.2;
                        if (i % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(midX, midY, midZ), 1, 0.05, 0.05, 0.05, 0.03);
                    }
                }
                // Periodic chime
                if (aT % 8 == 0) {
                    int idx = (aT / 8) % LINK_COUNT;
                    double t = startOff + step * idx;
                    double snake = Math.sin(idx * 0.5 - aT * 0.15) * 1.5;
                    Location chimeLoc = c.clone().add(cosA * t + pCosA * snake, 1.2, sinA * t + pSinA * snake);
                    float pitch = 0.6f + (idx / (float)LINK_COUNT) * 1.6f;
                    DisplayBuilder.playSound(chimeLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, pitch);
                }
                if (aT % 80 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.6f, 1.0f);
            }
            // ===== DISSIPATE PHASE (final 40t): compress to center =====
            else {
                int dT = tick - dissipateStart;
                if (dT == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.8f);
                }
                double t = dT / (double)DISSIPATE_PHASE_TICKS;
                float scale = (float)(0.6 * (1 - t));
                if (scale < 0.01f) scale = 0.01f;
                for (int i = 0; i < petals.size(); i++) {
                    double srcOff = startOff + step * i;
                    double bx = cosA * srcOff * (1 - t);
                    double bz = sinA * srcOff * (1 - t);
                    petals.get(i).animateTo(
                            new Vector3f((float)bx - scale / 2, 1.2f - scale / 2, (float)bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(scale), 3);
                }
                for (int i = 0; i < accents.size(); i++) {
                    float as = (float)(0.4 * (1 - t));
                    if (as < 0.01f) as = 0.01f;
                    accents.get(i).animateTo(
                            new Vector3f(-as / 2, 1.2f - as / 2, -as / 2),
                            new AxisAngle4f((float)(tick * 0.3), 0, 1, 0),
                            new Vector3f(as), 3);
                }
                if (dT % 4 == 0) {
                    w.spawnParticle(Particle.ENCHANT, c.clone().add(0, 1.2, 0), 8, 1.0, 0.5, 1.0, 0.1);
                }
                if (dT == DISSIPATE_PHASE_TICKS - 5) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.6f, 0.7f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 1.2, 0), 1, 0, 0, 0, 0);
                }
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (int i = 0; i < LINK_COUNT; i++) {
                    double t = startOff + step * i;
                    double snake = Math.sin(i * 0.5 - tick * 0.15) * 1.5;
                    Location link = c.clone().add(cosA * t + pCosA * snake, 1.2, sinA * t + pSinA * snake);
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(link) <= r2) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                            break;
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ShiveringChainpetals(plugin); }
    }

    // ================================================================
    // 47. FROSTFIRE ORBITAL — Phased: helix materializes ascending ->
    //     varied spiral -> speedup -> radial burst.
    // ================================================================
    public static class FrostfireOrbital extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stars = new ArrayList<>();
        private final List<ItemDisplayHandle> supports = new ArrayList<>();
        private final double[] sAng = new double[6];
        private final double[] sY = new double[6];
        // Final burst directions (per-star)
        private final double[] burstX = new double[6];
        private final double[] burstZ = new double[6];
        private static final double HELIX_R = 2.5;

        public FrostfireOrbital(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frostfire_orbital", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(38400.0);
            config.setDamageRadius(11.25);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2f, 0.8f);

            for (int i = 0; i < 6; i++) {
                sAng[i] = Math.PI * 2 * i / 6;
                sY[i] = 1.0 + i * (8.0 / 5);
                Location p = c.clone().add(Math.cos(sAng[i]) * HELIX_R, sY[i], Math.sin(sAng[i]) * HELIX_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_STAR));
                h.scale(0.01f, 0.01f, 0.01f).glow(160, 220, 255).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                stars.add(h);
                spawnedEntities.add(h.entity());
            }

            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + 0.3;
                double yy = 1.0 + i * (8.0 / 7);
                Location p = c.clone().add(Math.cos(a) * (HELIX_R - 0.5), yy, Math.sin(a) * (HELIX_R - 0.5));
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.01f, 0.01f, 0.01f).glow(140, 200, 240).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                supports.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * (HELIX_R + 0.6), 0.5, Math.sin(a) * (HELIX_R + 0.6));
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(140, 200, 255).interpolation(SPAWN_PHASE_TICKS - 4, 0);
                supports.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_PHASE_TICKS;

            // ===== SPAWN PHASE (0-40t): helix materializes ascending order, scale 0 -> 0.6 =====
            if (tick < SPAWN_PHASE_TICKS) {
                double phase = tick / (double)SPAWN_PHASE_TICKS;
                for (int i = 0; i < stars.size(); i++) {
                    // Staggered: star i appears when phase > i/6
                    double t = Math.max(0, Math.min(1.0, (phase - i / 6.0) * 4.0));
                    float s = (float)(0.01 + 0.59 * t);
                    stars.get(i).animateTo(
                            new Vector3f((float)(Math.cos(sAng[i]) * HELIX_R) - s / 2, (float)sY[i] - s / 2, (float)(Math.sin(sAng[i]) * HELIX_R) - s / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(s), 4);
                }
                for (int i = 0; i < supports.size(); i++) {
                    float ss = (float)(0.01 + 0.39 * phase);
                    supports.get(i).animateTo(
                            new Vector3f(-ss / 2, 1.0f - ss / 2, -ss / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(ss), 4);
                }
                // Chime ascending
                if (tick % 6 == 0) {
                    int idx = (int)(phase * 6);
                    if (idx < 6) DisplayBuilder.playSound(c.clone().add(0, sY[idx], 0), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.8f + idx * 0.15f);
                }
                if (tick % 2 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double a = Math.random() * Math.PI * 2;
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(Math.cos(a) * HELIX_R, Math.random() * 9.0, Math.sin(a) * HELIX_R),
                                1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }
            // ===== ACTIVE PHASE: 3 sub-phases =====
            else if (tick < dissipateStart) {
                int aT = tick - SPAWN_PHASE_TICKS;
                int subphase;
                if (aT < 60) subphase = 0;
                else if (aT < 90) subphase = 1;
                else subphase = 2;

                if (subphase == 0) {
                    // Spiral: varying speeds by height 0.04 .. 0.10 rad/tick
                    for (int i = 0; i < stars.size(); i++) {
                        double spinSpeed = 0.04 + (i / 5.0) * 0.06;
                        sAng[i] += spinSpeed;
                        double yy = sY[i] + Math.sin(aT * 0.06 + i) * 0.4;
                        float bx = (float)(Math.cos(sAng[i]) * HELIX_R);
                        float bz = (float)(Math.sin(sAng[i]) * HELIX_R);
                        stars.get(i).animateTo(
                                new Vector3f(bx - 0.3f, (float)yy - 0.3f, bz - 0.3f),
                                new AxisAngle4f((float)(tick * 0.18 + i), 0, 1, 0),
                                new Vector3f(0.6f), 4);
                        // NETHER_STAR glow trail
                        w.spawnParticle(Particle.END_ROD, c.clone().add(bx, yy, bz), 1, 0.05, 0.05, 0.05, 0.005);
                    }
                    // Beacon base ring
                    if (aT % 3 == 0) DisplayBuilder.particleRing(c.clone().add(0, 0.4, 0), HELIX_R + 0.6, Particle.END_ROD, 12, null);
                    if (aT % 40 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.9f, 0.9f);
                } else if (subphase == 1) {
                    // Speed up: all to 0.2 rad/tick + SOUL_FIRE column intensifies
                    for (int i = 0; i < stars.size(); i++) {
                        double t = (aT - 60) / 30.0;
                        double spinSpeed = (0.04 + (i / 5.0) * 0.06) + (0.2 - (0.04 + (i / 5.0) * 0.06)) * t;
                        sAng[i] += spinSpeed;
                        double yy = sY[i] + Math.sin(aT * 0.06 + i) * 0.3;
                        float bx = (float)(Math.cos(sAng[i]) * HELIX_R);
                        float bz = (float)(Math.sin(sAng[i]) * HELIX_R);
                        stars.get(i).animateTo(
                                new Vector3f(bx - 0.35f, (float)yy - 0.35f, bz - 0.35f),
                                new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                                new Vector3f(0.7f), 3);
                    }
                    // SOUL_FIRE column
                    for (double y = 1.0; y < 9.0; y += 0.5) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, y, 0), 2, 0.2, 0.1, 0.2, 0.03);
                    }
                    if (aT % 10 == 0) {
                        float pitch = 0.8f + ((aT - 60) / 30f) * 0.8f;
                        DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, pitch);
                    }
                } else {
                    // Burst: items fly outward radially over 30t from current positions
                    int bT = aT - 90;
                    if (bT == 0) {
                        for (int i = 0; i < stars.size(); i++) {
                            burstX[i] = Math.cos(sAng[i]);
                            burstZ[i] = Math.sin(sAng[i]);
                        }
                        DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);
                    }
                    double t = Math.min(1.0, bT / 30.0);
                    for (int i = 0; i < stars.size(); i++) {
                        double outwardR = HELIX_R + 4.0 * t;
                        double bx = burstX[i] * outwardR;
                        double bz = burstZ[i] * outwardR;
                        double yy = sY[i] + Math.sin(aT * 0.1 + i) * 0.3;
                        stars.get(i).animateTo(
                                new Vector3f((float)bx - 0.35f, (float)yy - 0.35f, (float)bz - 0.35f),
                                new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                                new Vector3f(0.7f), 3);
                        // Falling dust trails after burst
                        w.spawnParticle(Particle.FALLING_DUST, c.clone().add(bx, yy, bz), 2, 0.1, 0.1, 0.1, 0.02,
                                Material.BLUE_ICE.createBlockData());
                    }
                }
            }
            // ===== DISSIPATE PHASE (final 40t): items fall + fade =====
            else {
                int dT = tick - dissipateStart;
                if (dT == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.8f);
                }
                double t = dT / (double)DISSIPATE_PHASE_TICKS;
                float scale = (float)(0.7 * (1 - t));
                if (scale < 0.01f) scale = 0.01f;
                for (int i = 0; i < stars.size(); i++) {
                    double outwardR = HELIX_R + 4.0;
                    double fallY = sY[i] - t * sY[i];
                    double bx = burstX[i] * outwardR;
                    double bz = burstZ[i] * outwardR;
                    stars.get(i).animateTo(
                            new Vector3f((float)bx - scale / 2, (float)fallY - scale / 2, (float)bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.4 + i), 1, 0, 0),
                            new Vector3f(scale), 3);
                    w.spawnParticle(Particle.FALLING_DUST, c.clone().add(bx, fallY + 1, bz), 2, 0.1, 0.1, 0.1, 0.02,
                            Material.BLUE_ICE.createBlockData());
                    w.spawnParticle(Particle.END_ROD, c.clone().add(bx, fallY, bz), 1, 0.05, 0.05, 0.05, 0.005);
                }
                for (int i = 0; i < supports.size(); i++) {
                    float ss = (float)(0.4 * (1 - t));
                    if (ss < 0.01f) ss = 0.01f;
                    supports.get(i).animateTo(
                            new Vector3f(-ss / 2, 0.5f - ss / 2, -ss / 2),
                            new AxisAngle4f((float)(tick * 0.3), 0, 1, 0),
                            new Vector3f(ss), 3);
                }
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

        @Override public AbstractAttack newInstance() { return new FrostfireOrbital(plugin); }
    }

    // ================================================================
    // 48. CRYOSTASIS DOME — Phased: hemisphere grows from ground up ->
    //     swaying hold with layered FX -> dome collapses inward to center.
    // ================================================================
    public static class CryostasisDome extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shells = new ArrayList<>();
        private final List<ItemDisplayHandle> interior = new ArrayList<>();
        private final double[] shAng = new double[32];
        private final double[] shTheta = new double[32]; // polar angle
        private static final double DOME_R = 8.0;
        private static final int SPAWN_BUILD_TICKS = 60;

        public CryostasisDome(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryostasis_dome", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(33600.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(320);
            config.setCooldownTicks(60);
            config.setImpactDamage(60000.0);
            config.setImpactRadius(12.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.4f, 0.6f);

            // 32 GLASS_BOTTLE distributed across hemisphere using fibonacci spread
            for (int i = 0; i < 32; i++) {
                double phi = Math.acos(1.0 - (i / 31.0)); // 0..pi/2 polar
                double theta = i * 2.39996; // golden angle
                shAng[i] = theta;
                shTheta[i] = phi;
                double bx = DOME_R * Math.sin(phi) * Math.cos(theta);
                double by = DOME_R * Math.cos(phi);
                double bz = DOME_R * Math.sin(phi) * Math.sin(theta);
                Location p = c.clone().add(bx, 0.4 + by, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 220, 255).interpolation(8, 0);
                shells.add(h);
                spawnedEntities.add(h.entity());
            }

            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 1.6, 0.6 + (i % 4) * 0.8, Math.sin(a) * 1.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.01f, 0.01f, 0.01f).glow(160, 200, 240).interpolation(SPAWN_BUILD_TICKS - 4, 0);
                interior.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 3.5, 2.0 + Math.sin(a) * 0.5, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 210, 240).interpolation(SPAWN_BUILD_TICKS - 4, 0);
                interior.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_PHASE_TICKS;

            // ===== SPAWN PHASE (0-60t): dome grows ground-up, scale 0 -> 0.7 =====
            if (tick < SPAWN_BUILD_TICKS) {
                double phase = tick / (double)SPAWN_BUILD_TICKS;
                // Each shell spawns when phase > (1 - cos(phi))/(1 - cos(pi/2)) ~= (1 - cos(phi)) since cos(pi/2)=0
                // i.e. ground-level shells (low phi value) spawn first... actually phi=0 means by=DOME_R (top), so we invert
                for (int i = 0; i < shells.size(); i++) {
                    // by = DOME_R * cos(phi); ground = cos(phi)=0 => phi=pi/2
                    // We want ground first: ratio = cos(phi) inverted -> use (1 - cos(phi))
                    double byNorm = Math.cos(shTheta[i]); // 1 at top, 0 at ground
                    double appearAt = byNorm; // top appears latest
                    double t = Math.max(0, Math.min(1.0, (phase - appearAt) * 3.0));
                    float s = (float)(0.01 + 0.69 * t);
                    double bx = DOME_R * Math.sin(shTheta[i]) * Math.cos(shAng[i]);
                    double by = DOME_R * Math.cos(shTheta[i]);
                    double bz = DOME_R * Math.sin(shTheta[i]) * Math.sin(shAng[i]);
                    shells.get(i).animateTo(
                            new Vector3f((float)bx - s / 2, (float)(0.4 + by) - s / 2, (float)bz - s / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(s), 6);
                }
                for (int i = 0; i < interior.size(); i++) {
                    float is = (float)(0.01 + 0.49 * phase);
                    interior.get(i).animateTo(
                            new Vector3f(-is / 2, 0.4f - is / 2, -is / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(is), 4);
                }
                // ELECTRIC_SPARK rising through items
                if (tick % 2 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * DOME_R;
                        w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(Math.cos(a) * rr, Math.random() * 8.0, Math.sin(a) * rr),
                                1, 0.05, 0.05, 0.05, 0.03);
                    }
                }
                // Chime per spawning shell
                if (tick % 3 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f + (float)(phase * 0.6));
                }
            }
            // ===== ACTIVE PHASE: hold with 3 layered visuals =====
            else if (tick < dissipateStart) {
                int aT = tick - SPAWN_BUILD_TICKS;
                // Subtle sway: Y±0.05 + 0.5° wobble
                for (int i = 0; i < shells.size(); i++) {
                    double phi = shTheta[i] + Math.sin(aT * 0.04 + i) * 0.01;
                    double theta = shAng[i] + Math.cos(aT * 0.03 + i) * 0.008;
                    double bx = DOME_R * Math.sin(phi) * Math.cos(theta);
                    double by = DOME_R * Math.cos(phi) + Math.sin(aT * 0.05 + i) * 0.05;
                    double bz = DOME_R * Math.sin(phi) * Math.sin(theta);
                    shells.get(i).animateTo(
                            new Vector3f((float)bx - 0.35f, (float)(0.4 + by) - 0.35f, (float)bz - 0.35f),
                            new AxisAngle4f((float)(tick * 0.025 + i), 0, 1, 0),
                            new Vector3f(0.7f), 8);
                }
                // Layer 1: SCULK_SOUL pulse every 30t expanding outward
                if (aT % 30 == 0) {
                    for (int r = 1; r <= 8; r++) {
                        final int rr = r;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            Location pc = getCenter();
                            if (pc != null && pc.getWorld() != null) {
                                DisplayBuilder.particleRing(pc.clone().add(0, 0.5, 0), rr, Particle.SCULK_SOUL, 14, null);
                            }
                        }, r * 2L);
                    }
                }
                // Layer 2: GLOW halo over dome perimeter
                if (aT % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double phi = Math.random() * Math.PI / 2;
                        double theta = Math.random() * Math.PI * 2;
                        double bx = DOME_R * Math.sin(phi) * Math.cos(theta);
                        double by = DOME_R * Math.cos(phi);
                        double bz = DOME_R * Math.sin(phi) * Math.sin(theta);
                        w.spawnParticle(Particle.GLOW, c.clone().add(bx, 0.4 + by, bz), 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                // Layer 3: ICICLE-like FALLING_DUST trickle inside dome
                if (aT % 2 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 6.0;
                        w.spawnParticle(Particle.FALLING_DUST, c.clone().add(Math.cos(a) * rr, 5.0 + Math.random() * 3.0, Math.sin(a) * rr),
                                1, 0.05, 0.05, 0.05, 0.01, Material.BLUE_ICE.createBlockData());
                    }
                }
                // GLASS_HIT ambient
                if (aT % 25 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.8f, 0.8f + (float)Math.random() * 0.4f);
                if (aT % 50 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);

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
            // ===== DISSIPATE PHASE (final 40t): collapse inward to center =====
            else {
                int dT = tick - dissipateStart;
                if (dT == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.6f, 0.7f);
                }
                double t = Math.min(1.0, dT / (double)DISSIPATE_PHASE_TICKS);
                float scale = (float)(0.7 * (1 - t));
                if (scale < 0.01f) scale = 0.01f;
                // Items animate toward origin point at center over 40t
                for (int i = 0; i < shells.size(); i++) {
                    double bx0 = DOME_R * Math.sin(shTheta[i]) * Math.cos(shAng[i]);
                    double by0 = DOME_R * Math.cos(shTheta[i]);
                    double bz0 = DOME_R * Math.sin(shTheta[i]) * Math.sin(shAng[i]);
                    double bx = bx0 * (1 - t);
                    double by = by0 * (1 - t);
                    double bz = bz0 * (1 - t);
                    shells.get(i).animateTo(
                            new Vector3f((float)bx - scale / 2, (float)(0.4 + by) - scale / 2, (float)bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.3 + i), 1, 0.5f, 0),
                            new Vector3f(scale), 3);
                    // Cascading break trails
                    if (dT % 4 == 0 && i % 4 == 0) {
                        w.spawnParticle(Particle.BLOCK, c.clone().add(bx, 0.4 + by, bz), 4, 0.2, 0.2, 0.2, 0.05, Material.GLASS.createBlockData());
                    }
                }
                for (int i = 0; i < interior.size(); i++) {
                    float is = (float)(0.5 * (1 - t));
                    if (is < 0.01f) is = 0.01f;
                    interior.get(i).animateTo(
                            new Vector3f(-is / 2, 0.4f - is / 2, -is / 2),
                            new AxisAngle4f((float)(tick * 0.3), 0, 1, 0),
                            new Vector3f(is), 3);
                }
                // Cascading glass break sounds during collapse
                if (dT % 6 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.9f, 0.7f + (float)Math.random() * 0.5f);
                }
                if (dT == DISSIPATE_PHASE_TICKS - 5) {
                    Location impact = c.clone().add(0, 0.4, 0);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.7f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.SONIC_BOOM, impact, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(impact, 36, 1.5, 200, 220, 255, 1.6f);
                    // END_ROD burst
                    for (int r = 1; r <= 5; r++) {
                        DisplayBuilder.particleRing(impact, r, Particle.END_ROD, 16, null);
                    }
                    triggerImpactDamage(impact);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CryostasisDome(plugin); }
    }

    // ================================================================
    // 49. SUBZERO VORTEX — 36 SNOWBALL tornado spiral Y+0 to Y+12, 0.1
    //     rad/tick around vertical axis. Constant 7.0r, 3000hp, 10-tick.
    // ================================================================
    public static class SubzeroVortex extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flakes = new ArrayList<>();
        private final double[] fAng = new double[36];
        private final double[] fY = new double[36];
        private final double[] fR = new double[36];

        public SubzeroVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subzero_vortex", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(36000.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(380);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.5f);

            for (int i = 0; i < 36; i++) {
                fAng[i] = Math.PI * 2 * (i / 6.0); // 6 per layer
                fY[i] = (i / 6) * 2.0; // 6 layers Y=0..10
                // Tornado profile: tight at base, wider at top
                fR[i] = 1.5 + (fY[i] / 12.0) * 3.0;
                Location p = c.clone().add(Math.cos(fAng[i]) * fR[i], fY[i], Math.sin(fAng[i]) * fR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 240, 255).interpolation(2, 0);
                flakes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer wind veil: 10 FEATHER ascending spiral + 8 PHANTOM_MEMBRANE wraith trails
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 5 + 0.2;
                double yy = 0.5 + i * 1.1;
                double rr = 2.0 + (yy / 12.0) * 3.0;
                Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 230, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double yy = 2.0 + (i % 4) * 2.5;
                double rr = 2.5 + (yy / 12.0) * 2.5;
                Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 230, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Vortex spin — 0.1 rad/tick
            for (int i = 0; i < flakes.size(); i++) {
                fAng[i] += 0.10;
                double localR = fR[i] + Math.sin(tick * 0.06 + i) * 0.25;
                double bobY = fY[i] + Math.sin(tick * 0.08 + i) * 0.3;
                float bx = (float)(Math.cos(fAng[i]) * localR);
                float bz = (float)(Math.sin(fAng[i]) * localR);
                flakes.get(i).animateTo(
                        new Vector3f(bx - 0.25f, (float)bobY, bz - 0.25f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                        new Vector3f(0.5f), 2);
            }

            // CLOUD vortex column + SNOWFLAKE wisps
            if (tick % 1 == 0) {
                for (double y = 0.3; y < 12.5; y += 0.6) {
                    double a = (y * 1.4) + tick * 0.3;
                    double localR = 1.5 + (y / 12.0) * 3.0;
                    Location p = c.clone().add(Math.cos(a) * localR, y, Math.sin(a) * localR);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.05, 0.05, 0.05, 0.02);
                    if (Math.random() < 0.4) w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.03);
                }
            }
            // Outer dust ring at top
            if (tick % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 11.0, 0), 4.5, Particle.SNOWFLAKE, 20, null);
            }
            if (tick % 30 == 0) DisplayBuilder.playSound(c.clone().add(0, 6, 0), Sound.ENTITY_HORSE_BREATHE, 0.9f, 0.6f);

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

        @Override public AbstractAttack newInstance() { return new SubzeroVortex(plugin); }
    }

    // ================================================================
    // 50. ABSOLUTE ZERO POINT — 1 NETHER_STAR core + 8 BLUE_ICE orbiting
    //     pull inward over 100t then IMPLODE. Pull: 8.0r, 1800hp, 12-tick.
    //     Implosion: 12.0r, 6000hp impact. SHOWCASE FINALE.
    // ================================================================
    public static class AbsoluteZeroPoint extends EnvironmentalAttack {
        private ItemDisplayHandle core;
        private final List<ItemDisplayHandle> orbs = new ArrayList<>();
        private final double[] oAng = new double[8];
        private double currentR = 6.0;
        private boolean imploded = false;
        private static final int PULL_DURATION = 100;
        private static final double START_R = 6.0;
        private static final double END_R = 0.4;

        public AbsoluteZeroPoint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("absolute_zero_point", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(21600.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
            config.setImpactDamage(72000.0);
            config.setImpactRadius(18.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.8f);

            // Central core
            Location corePos = c.clone().add(0, 3.0, 0);
            core = displayBuilder.spawnItem(corePos, new ItemStack(Material.NETHER_STAR));
            core.scale(1.0f, 1.0f, 1.0f).glow(180, 220, 255).interpolation(6, 0);
            spawnedEntities.add(core.entity());

            // 8 BLUE_ICE orbiters at START_R
            for (int i = 0; i < 8; i++) {
                oAng[i] = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(oAng[i]) * START_R, 3.0 + Math.sin(oAng[i]) * 0.6, Math.sin(oAng[i]) * START_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.8f, 0.8f, 0.8f).glow(140, 200, 255).interpolation(4, 0);
                orbs.add(h);
                spawnedEntities.add(h.entity());
            }

            // Anchor ring beneath: 8 ECHO_SHARD ground + 6 PACKED_ICE pillars
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + Math.PI / 8;
                Location p = c.clone().add(Math.cos(a) * 5.0, 0.4, Math.sin(a) * 5.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.5f, 0.5f, 0.5f).glow(140, 200, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 6.0, 1.0 + (i % 3) * 0.6, Math.sin(a) * 6.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.45f, 1.2f, 0.45f).glow(180, 220, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!imploded && tick < PULL_DURATION) {
                // Pull-in phase: orbs spiral inward from START_R -> END_R
                double t = tick / (double)PULL_DURATION;
                currentR = START_R + (END_R - START_R) * t;
                double spinSpeed = 0.08 + t * 0.25; // accelerate as we close in

                // Core pulse — scale up slightly
                double coreScale = 1.0 + t * 0.6;
                core.animateTo(
                        new Vector3f(-(float)coreScale / 2, 3.0f - (float)coreScale / 2, -(float)coreScale / 2),
                        new AxisAngle4f((float)(tick * 0.2), 0, 1, 0),
                        new Vector3f((float)coreScale), 4);

                for (int i = 0; i < orbs.size(); i++) {
                    oAng[i] += spinSpeed;
                    double yy = 3.0 + Math.sin(oAng[i] + tick * 0.05) * (0.6 * (1 - t)); // bob flattens as we close in
                    float bx = (float)(Math.cos(oAng[i]) * currentR);
                    float bz = (float)(Math.sin(oAng[i]) * currentR);
                    orbs.get(i).animateTo(
                            new Vector3f(bx - 0.4f, (float)yy, bz - 0.4f),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(0.8f), 4);
                }

                // REVERSE_PORTAL pull from periphery toward core
                if (tick % 1 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = 1.0 + Math.random() * 6.0;
                        Location p = c.clone().add(Math.cos(a) * rr, 1.0 + Math.random() * 4.0, Math.sin(a) * rr);
                        w.spawnParticle(Particle.REVERSE_PORTAL, p, 1, 0.1, 0.1, 0.1, 0.04);
                    }
                }
                // SCULK_SOUL accent
                if (tick % 3 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 4.0;
                        Location p = c.clone().add(Math.cos(a) * rr, 1.5 + Math.random() * 3.5, Math.sin(a) * rr);
                        w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                // Rising pitch chime as we approach implosion
                if (tick % 10 == 0) {
                    float pitch = 0.6f + (float)(t * 1.6f);
                    DisplayBuilder.playSound(c.clone().add(0, 3, 0), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, pitch);
                }

                // Pull-phase damage
                if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                    double r2 = config.getDamageRadius() * config.getDamageRadius();
                    Location dmgC = c.clone().add(0, 3.0, 0);
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(dmgC) <= r2) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            } else if (!imploded) {
                // IMPLOSION
                imploded = true;
                Location impact = c.clone().add(0, 3.0, 0);
                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.6f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.8f, 0.8f);
                DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_THUNDER, 1.6f, 0.5f);

                // Burst particles — layered
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 3, 1.0, 1.0, 1.0, 0);
                w.spawnParticle(Particle.SONIC_BOOM, impact, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.FLASH, impact, 2, 0.5, 0.5, 0.5, 0);

                // Outward burst rings of SOUL_FIRE_FLAME + ELECTRIC_SPARK + END_ROD
                for (int r = 1; r <= 12; r++) {
                    final int rr = r;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.particleRing(impact, rr, Particle.SOUL_FIRE_FLAME, 28, null);
                        DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), rr, Particle.ELECTRIC_SPARK, 24, null);
                        DisplayBuilder.particleRing(impact, rr * 0.8, Particle.END_ROD, 20, null);
                        DisplayBuilder.dustParticles(impact, 8, rr * 0.5, 200, 230, 255, 1.6f);
                    }, r * 2L);
                }

                // Collapse orbs into core
                core.animateTo(
                        new Vector3f(-1.5f, 1.5f, -1.5f),
                        new AxisAngle4f((float)(tick * 0.5), 0, 1, 0),
                        new Vector3f(3.0f), 8);
                for (int i = 0; i < orbs.size(); i++) {
                    orbs.get(i).animateTo(
                            new Vector3f(-0.4f, 3.0f, -0.4f),
                            new AxisAngle4f((float)(tick * 0.4 + i), 1, 1, 0),
                            new Vector3f(0.1f), 6);
                }

                triggerImpactDamage(impact);
            } else {
                // Post-implosion echo — fade
                int postT = tick - PULL_DURATION;
                if (postT % 4 == 0 && postT < 60) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 8.0;
                        Location p = c.clone().add(Math.cos(a) * rr, 0.5 + Math.random() * 4.0, Math.sin(a) * rr);
                        w.spawnParticle(Particle.END_ROD, p, 1, 0.1, 0.1, 0.1, 0.02);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                if (postT == 20) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new AbsoluteZeroPoint(plugin); }
    }
}
