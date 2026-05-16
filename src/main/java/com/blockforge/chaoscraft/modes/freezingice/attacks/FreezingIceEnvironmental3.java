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
 * FreezingIce Mode — ENVIRONMENTAL FX BATCH 3 (entries 21-30).
 * Ground Pattern attacks. Pure ItemDisplay + particle attacks. NO BlockDisplays.
 *
 * Every attack follows: SPAWN → ACTIVE → DISSIPATE (final 40t)
 * with 3+ layered particle types, continuous animateTo motion,
 * and phase-distinct sounds.
 *
 * Damage values / IDs / radii / configurable defaults UNCHANGED.
 */
public final class FreezingIceEnvironmental3 {
    private FreezingIceEnvironmental3() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceRuneCircle(plugin));
        registry.register(new SnowDriftMound(plugin));
        registry.register(new CrystallineLattice(plugin));
        registry.register(new FrozenFootprintTrail(plugin));
        registry.register(new ShatteredMirrorFloor(plugin));
        registry.register(new PermafrostBloom(plugin));
        registry.register(new AuroraRibbons(plugin));
        registry.register(new IceMeteorShower(plugin));
        registry.register(new FrozenStorm(plugin));
        registry.register(new SkyShatteringPrism(plugin));
    }

    // ================================================================
    // 21. ICE RUNE CIRCLE — 12 AMETHYST_SHARD hexagram pattern.
    //     6 outer points + 6 inner points; sparking lines connect.
    // ================================================================
    public static class IceRuneCircle extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> runes = new ArrayList<>();
        private final double[] rAng = new double[12];
        private final double[] rR = new double[12];
        private int dissipateStart;
        private boolean dissipateInitialized = false;

        public IceRuneCircle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_rune_circle", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(42000.0);
            config.setDamageRadius(9.75);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(3);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            dissipateStart = config.getDurationTicks() - 40;
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);

            // 6 outer hexagram points
            for (int i = 0; i < 6; i++) {
                rAng[i] = Math.PI * 2 * i / 6;
                rR[i] = 5.8;
                Location p = c.clone().add(Math.cos(rAng[i]) * rR[i], 0.2, Math.sin(rAng[i]) * rR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 220, 255).interpolation(8, 0);
                runes.add(h);
                spawnedEntities.add(h.entity());
            }
            // 6 inner hexagram points (rotated)
            for (int i = 6; i < 12; i++) {
                rAng[i] = Math.PI * 2 * (i - 6) / 6 + Math.PI / 6;
                rR[i] = 3.2;
                Location p = c.clone().add(Math.cos(rAng[i]) * rR[i], 0.2, Math.sin(rAng[i]) * rR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.01f, 0.01f, 0.01f).glow(220, 240, 255).interpolation(8, 0);
                runes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN (0-40t) =============
            if (tick < 40) {
                double t = tick / 40.0;
                float spawnScaleOuter = (float)(0.5 * t);
                float spawnScaleInner = (float)(0.5 * t);
                for (int i = 0; i < runes.size(); i++) {
                    float scale = i < 6 ? spawnScaleOuter : spawnScaleInner;
                    float bx = (float)(Math.cos(rAng[i]) * rR[i]);
                    float bz = (float)(Math.sin(rAng[i]) * rR[i]);
                    runes.get(i).animateTo(
                            new Vector3f(bx - scale / 2, 0.2f, bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale)), 4);
                }
                // ELECTRIC_SPARK lines drawn between connecting hexagram points
                if (tick % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        int next = (i + 2) % 6; // hexagram triangle pattern
                        Location pa = c.clone().add(Math.cos(rAng[i]) * rR[i], 0.3, Math.sin(rAng[i]) * rR[i]);
                        Location pb = c.clone().add(Math.cos(rAng[next]) * rR[next], 0.3, Math.sin(rAng[next]) * rR[next]);
                        DisplayBuilder.particleLine(pa, pb, Particle.ELECTRIC_SPARK, 8, null);
                    }
                }
                // Ladder chime
                if (tick % 6 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f + (float)t * 1.0f);
                }
                // ENCHANT swirl in
                if (tick % 2 == 0) {
                    for (int i = 0; i < runes.size(); i++) {
                        Location at = c.clone().add(Math.cos(rAng[i]) * rR[i], 0.4, Math.sin(rAng[i]) * rR[i]);
                        w.spawnParticle(Particle.ENCHANT, at.clone().add(0, 1.5 - t * 1.4, 0), 1, 0.2, 0.4, 0.2, 0.03);
                    }
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 6, 0.5, 200, 230, 255, 1.3f);
            }
            // ============= PHASE 2: ACTIVE =============
            else if (tick < dissipateStart) {
                int aTick = tick - 40;
                // Runes rotate Y-axis 0.03 rad/tick continuously
                for (int i = 0; i < runes.size(); i++) {
                    rAng[i] += 0.03 * (i < 6 ? 1 : -1);
                    float scale = 0.5f;
                    float bx = (float)(Math.cos(rAng[i]) * rR[i]);
                    float bz = (float)(Math.sin(rAng[i]) * rR[i]);
                    float by = 0.2f + (float)(Math.sin(aTick * 0.08 + i) * 0.1);
                    runes.get(i).animateTo(
                            new Vector3f(bx - scale / 2, by, bz - scale / 2),
                            new AxisAngle4f((float)(aTick * 0.1 + i), 0, 1, 0),
                            new Vector3f(scale), 4);
                }
                // ELECTRIC_SPARK pulses along hexagram lines every 20t
                if (aTick % 20 == 0) {
                    for (int i = 0; i < 6; i++) {
                        int next = (i + 2) % 6;
                        Location pa = c.clone().add(Math.cos(rAng[i]) * rR[i], 0.35, Math.sin(rAng[i]) * rR[i]);
                        Location pb = c.clone().add(Math.cos(rAng[next]) * rR[next], 0.35, Math.sin(rAng[next]) * rR[next]);
                        DisplayBuilder.particleLine(pa, pb, Particle.ELECTRIC_SPARK, 12, null);
                    }
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 0.8f + (float)Math.random() * 0.4f);
                }
                // GLOW core at center
                if (tick % 3 == 0) {
                    Location ctr = c.clone().add(0, 0.6, 0);
                    w.spawnParticle(Particle.GLOW, ctr, 3, 0.4, 0.3, 0.4, 0.02);
                }
                // AMETHYST DUST shimmer at each rune
                if (tick % 2 == 0) {
                    for (int i = 0; i < runes.size(); i++) {
                        Location at = c.clone().add(Math.cos(rAng[i]) * rR[i], 0.5, Math.sin(rAng[i]) * rR[i]);
                        DisplayBuilder.dustParticles(at, 1, 0.2, 200, 180, 240, 1.1f);
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
            // ============= PHASE 3: DISSIPATE (final 40t) =============
            else {
                int dTick = tick - dissipateStart;
                if (!dissipateInitialized) {
                    dissipateInitialized = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.8f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.9f);
                    // Bright flare
                    for (int i = 0; i < runes.size(); i++) {
                        runes.get(i).glow(255, 255, 255);
                    }
                }
                double t = Math.min(1.0, dTick / 40.0);
                // Bright flare in first 10t, then shatter outward
                if (dTick < 10) {
                    // Brighten + small grow burst
                    float bigScale = 0.5f + (float)(dTick / 10.0) * 0.25f;
                    for (int i = 0; i < runes.size(); i++) {
                        float bx = (float)(Math.cos(rAng[i]) * rR[i]);
                        float bz = (float)(Math.sin(rAng[i]) * rR[i]);
                        runes.get(i).animateTo(
                                new Vector3f(bx - bigScale / 2, 0.2f, bz - bigScale / 2),
                                new AxisAngle4f((float)(dTick * 0.3 + i), 0, 1, 0),
                                new Vector3f(bigScale), 3);
                    }
                    if (tick % 2 == 0) {
                        Location ctr = c.clone().add(0, 0.6, 0);
                        w.spawnParticle(Particle.END_ROD, ctr, 8, 0.3, 0.3, 0.3, 0.1);
                    }
                } else {
                    // Shatter outward — scale shrinks, position pushes outward
                    double shatterT = (dTick - 10) / 30.0;
                    float endScale = (float)((1.0 - shatterT) * 0.75);
                    for (int i = 0; i < runes.size(); i++) {
                        rAng[i] += 0.3 * (i < 6 ? 1 : -1);
                        double rOut = rR[i] + shatterT * 4.0;
                        float bx = (float)(Math.cos(rAng[i]) * rOut);
                        float bz = (float)(Math.sin(rAng[i]) * rOut);
                        runes.get(i).animateTo(
                                new Vector3f(bx - endScale / 2, 0.2f, bz - endScale / 2),
                                new AxisAngle4f((float)(dTick * 0.4 + i), 0, 1, 0),
                                new Vector3f(Math.max(0.001f, endScale)), 3);
                    }
                    // AMETHYST_BLOCK_BREAK cascade
                    if (dTick % 6 == 0) {
                        DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.7f + (float)Math.random() * 0.5f);
                    }
                    // END_ROD burst from center
                    if (dTick % 3 == 0) {
                        Location ctr = c.clone().add(0, 0.6, 0);
                        w.spawnParticle(Particle.END_ROD, ctr, 10, 0.6, 0.4, 0.6, 0.3);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, ctr, 6, 0.4, 0.3, 0.4, 0.2);
                    }
                    // Per-rune shatter dust
                    if (dTick % 2 == 0) {
                        for (int i = 0; i < runes.size(); i++) {
                            double rOut = rR[i] + shatterT * 4.0;
                            Location at = c.clone().add(Math.cos(rAng[i]) * rOut, 0.4, Math.sin(rAng[i]) * rOut);
                            DisplayBuilder.dustParticles(at, 2, 0.3, 200, 180, 240, 1.3f);
                        }
                    }
                }
                // Final big explode
                if (dTick == 30) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.7f);
                    w.spawnParticle(Particle.END_ROD, c.clone().add(0, 1.0, 0), 30, 1.5, 1.0, 1.5, 0.4);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IceRuneCircle(plugin); }
    }

    // ================================================================
    // 22. SNOW DRIFT MOUND — 22 SNOWBALL dome (8+8+6 layered hemisphere).
    //     Builds, breathes, then collapses inward and explodes outward.
    // ================================================================
    public static class SnowDriftMound extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flakes = new ArrayList<>();
        private final double[] sX = new double[22];
        private final double[] sY = new double[22];
        private final double[] sZ = new double[22];
        private boolean dissipated = false;
        private int dissipateStart;
        private static final int BUILD_TICKS = 50;

        public SnowDriftMound(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_drift_mound", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(57600.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(57600.0);
            config.setImpactRadius(13.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            dissipateStart = config.getDurationTicks() - 30;
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.0f, 0.6f);

            // Build hemisphere: bottom 8, middle 8, top 6 (22 total)
            int idx = 0;
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                sX[idx] = Math.cos(a) * 6.5;
                sY[idx] = 0.4;
                sZ[idx] = Math.sin(a) * 6.5;
                idx++;
            }
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + Math.PI / 8;
                sX[idx] = Math.cos(a) * 4.5;
                sY[idx] = 2.2;
                sZ[idx] = Math.sin(a) * 4.5;
                idx++;
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                sX[idx] = Math.cos(a) * 2.2;
                sY[idx] = 3.8;
                sZ[idx] = Math.sin(a) * 2.2;
                idx++;
            }

            // Spawn all at low scale at their final XZ, but start with Y at 0
            for (int i = 0; i < 22; i++) {
                Location p = c.clone().add(sX[i], 0.2, sZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.01f, 0.01f, 0.01f).glow(240, 250, 255).interpolation(8, 0);
                flakes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN — ascending layers piling up (0-50t) =============
            if (tick < BUILD_TICKS) {
                // Layer-staggered: bottom (0-8) appears 0-20t, middle (8-16) 15-35t, top (16-22) 30-50t
                for (int i = 0; i < flakes.size(); i++) {
                    int layerStart;
                    if (i < 8) layerStart = 0;
                    else if (i < 16) layerStart = 15;
                    else layerStart = 30;
                    double localT = Math.max(0.0, Math.min(1.0, (tick - layerStart) / 20.0));
                    float scale = (float)(0.7 * localT);
                    flakes.get(i).animateTo(
                            new Vector3f((float)sX[i] - scale / 2, (float)sY[i], (float)sZ[i] - scale / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale)), 4);
                    if (tick == layerStart) {
                        Location at = c.clone().add(sX[i], sY[i], sZ[i]);
                        DisplayBuilder.playSound(at, Sound.BLOCK_POWDER_SNOW_PLACE, 0.7f, 0.9f + (float)Math.random() * 0.3f);
                    }
                }
                // 3 particle layers: SNOWFLAKE spiral converging + CLOUD wisp + FALLING_DUST(SNOW_BLOCK)
                if (tick % 2 == 0) {
                    double t = tick / (double)BUILD_TICKS;
                    double spiralR = 8.0 * (1.0 - t);
                    for (int j = 0; j < 8; j++) {
                        double a = tick * 0.35 + j * (Math.PI / 4);
                        Location sp = c.clone().add(Math.cos(a) * spiralR, 5.0 - t * 4.0, Math.sin(a) * spiralR);
                        w.spawnParticle(Particle.SNOWFLAKE, sp, 1, 0, 0, 0, 0);
                    }
                    Location apex = c.clone().add(0, 4.5, 0);
                    w.spawnParticle(Particle.CLOUD, apex, 2, 1.0, 0.4, 1.0, 0.02);
                }
                if (tick % 3 == 0) {
                    for (int i = 0; i < flakes.size(); i++) {
                        Location p = c.clone().add(sX[i], sY[i] + 0.4, sZ[i]);
                        w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.2, 0.2, 0.2, 0,
                                Material.WHITE_CONCRETE.createBlockData());
                    }
                }
            }
            // ============= PHASE 2: ACTIVE — dome holds + breathes (50t → dissipateStart) =============
            else if (tick < dissipateStart) {
                int aTick = tick - BUILD_TICKS;
                // Breathing scale ±0.05 every 30t
                float breath = (float)(0.7 + Math.sin(aTick * 0.1) * 0.05);
                for (int i = 0; i < flakes.size(); i++) {
                    float bobY = (float)(sY[i] + Math.sin(aTick * 0.08 + i * 0.4) * 0.1);
                    flakes.get(i).animateTo(
                            new Vector3f((float)sX[i] - breath / 2, bobY, (float)sZ[i] - breath / 2),
                            new AxisAngle4f((float)(aTick * 0.03 + i), 0, 1, 0),
                            new Vector3f(breath), 4);
                }
                // CLOUD wisps off top
                Location apex = c.clone().add(0, 4.5, 0);
                for (int k = 0; k < 3; k++) {
                    double ox = (Math.random() - 0.5) * 2.0;
                    double oz = (Math.random() - 0.5) * 2.0;
                    w.spawnParticle(Particle.CLOUD, apex.clone().add(ox, Math.random() * 0.5, oz), 1, 0, 0, 0, 0.03);
                }
                // FALLING_DUST(WHITE_CONCRETE) trickle down sides
                if (tick % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8 + aTick * 0.02;
                        Location side = c.clone().add(Math.cos(a) * 6.0, 1.0 + Math.random() * 2.5, Math.sin(a) * 6.0);
                        w.spawnParticle(Particle.FALLING_DUST, side, 1, 0.1, 0.2, 0.1, 0,
                                Material.WHITE_CONCRETE.createBlockData());
                    }
                }
                // SCULK_SOUL inside dome
                if (tick % 5 == 0) {
                    for (int k = 0; k < 2; k++) {
                        Location inside = c.clone().add(
                                (Math.random() - 0.5) * 4.0,
                                1.0 + Math.random() * 2.5,
                                (Math.random() - 0.5) * 4.0);
                        w.spawnParticle(Particle.SCULK_SOUL, inside, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                // Ambient glass hit
                if (aTick % 30 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.7f, 0.7f);
                }
            }
            // ============= PHASE 3: DISSIPATE — collapse inward then explode out (final 30t) =============
            else {
                int dTick = tick - dissipateStart;
                if (!dissipated) {
                    dissipated = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_BREAK, 1.5f, 0.8f);
                }
                if (dTick < 15) {
                    // Phase 3a: collapse inward — animateTo origin
                    double t = dTick / 15.0;
                    float scale = (float)(0.7 * (1.0 - t * 0.4));
                    for (int i = 0; i < flakes.size(); i++) {
                        float bx = (float)(sX[i] * (1.0 - t));
                        float by = (float)(sY[i] * (1.0 - t * 0.6));
                        float bz = (float)(sZ[i] * (1.0 - t));
                        flakes.get(i).animateTo(
                                new Vector3f(bx - scale / 2, by, bz - scale / 2),
                                new AxisAngle4f((float)(dTick * 0.2 + i), 0, 1, 0),
                                new Vector3f(scale), 3);
                    }
                    // Implosion particles converging on center
                    if (tick % 2 == 0) {
                        for (int j = 0; j < 12; j++) {
                            double a = Math.random() * Math.PI * 2;
                            double r = 6.0 * (1.0 - t);
                            Location p = c.clone().add(Math.cos(a) * r, 2.0 + Math.random() * 2.0, Math.sin(a) * r);
                            w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0, 0, 0, 0);
                            w.spawnParticle(Particle.FALLING_DUST, p, 1, 0, 0, 0, 0,
                                    Material.WHITE_CONCRETE.createBlockData());
                        }
                    }
                } else {
                    // Phase 3b: explode outward radially with scale shrink
                    double t = (dTick - 15) / 15.0;
                    float scale = (float)((1.0 - t) * 0.45);
                    for (int i = 0; i < flakes.size(); i++) {
                        // Radial blast direction based on original position
                        double dirX = sX[i] * 1.4;
                        double dirZ = sZ[i] * 1.4;
                        float bx = (float)(dirX * (0.2 + t * 2.0));
                        float by = (float)(sY[i] * 0.4 + t * 4.0);
                        float bz = (float)(dirZ * (0.2 + t * 2.0));
                        flakes.get(i).animateTo(
                                new Vector3f(bx - scale / 2, by, bz - scale / 2),
                                new AxisAngle4f((float)(dTick * 0.4 + i), 0, 1, 0),
                                new Vector3f(Math.max(0.001f, scale)), 3);
                    }
                    // ITEM_SNOWBALL burst (40 over phase, paced)
                    if (dTick == 15) {
                        Location impact = c.clone().add(0, 0.5, 0);
                        for (int i = 0; i < 40; i++) {
                            double a = Math.random() * Math.PI * 2;
                            double r = Math.random() * 9.0;
                            Location p = impact.clone().add(Math.cos(a) * r, 0.4 + Math.random() * 2.0, Math.sin(a) * r);
                            w.spawnParticle(Particle.ITEM_SNOWBALL, p, 2, 0.2, 0.2, 0.2, 0.15);
                        }
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                        triggerImpactDamage(impact);
                    }
                    if (dTick % 3 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double a = Math.random() * Math.PI * 2;
                            double r = 2.0 + Math.random() * 7.0;
                            Location p = c.clone().add(Math.cos(a) * r, 0.4 + Math.random() * 2.0, Math.sin(a) * r);
                            w.spawnParticle(Particle.ITEM_SNOWBALL, p, 1, 0.1, 0.1, 0.1, 0.1);
                            w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                    if (dTick == 18) {
                        DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_BREAK, 1.2f, 0.7f);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SnowDriftMound(plugin); }
    }

    // ================================================================
    // 23. CRYSTALLINE LATTICE — 5x5 BLUE_ICE grid (25 ItemDisplays).
    //     Center-out radiating spawn, wave pulses, scatter explosion.
    // ================================================================
    public static class CrystallineLattice extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> cells = new ArrayList<>();
        private final int[] cellX = new int[25];
        private final int[] cellZ = new int[25];
        private final double[] scatterAng = new double[25];
        private final double[] scatterR = new double[25];
        private static final double CELL_SPACING = 2.8;
        private int dissipateStart;
        private boolean dissipateInit = false;

        public CrystallineLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_lattice", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(38400.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(240);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            dissipateStart = config.getDurationTicks() - 40;
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.8f);

            int idx = 0;
            for (int gx = -2; gx <= 2; gx++) {
                for (int gz = -2; gz <= 2; gz++) {
                    cellX[idx] = gx;
                    cellZ[idx] = gz;
                    Location p = c.clone().add(gx * CELL_SPACING, 0.2, gz * CELL_SPACING);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                    h.scale(0.01f, 0.01f, 0.01f).glow(140, 200, 255).interpolation(6, 0);
                    cells.add(h);
                    spawnedEntities.add(h.entity());
                    // Pre-compute scatter angle & radius for dissipate
                    scatterAng[idx] = Math.random() * Math.PI * 2;
                    scatterR[idx] = 4.0 + Math.random() * 4.0;
                    idx++;
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN — center first then radiating out (0-40t) =============
            if (tick < 40) {
                for (int i = 0; i < cells.size(); i++) {
                    int manhattan = Math.abs(cellX[i]) + Math.abs(cellZ[i]); // 0..4
                    int cellSpawnTick = manhattan * 6; // 0/6/12/18/24
                    double t = Math.max(0.0, Math.min(1.0, (tick - cellSpawnTick) / 18.0));
                    float scale = (float)(0.8 * t);
                    // Y-axis rotation 0.04 rad/tick per item
                    cells.get(i).animateTo(
                            new Vector3f((float)(cellX[i] * CELL_SPACING) - scale / 2, 0.2f,
                                    (float)(cellZ[i] * CELL_SPACING) - scale / 2),
                            new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale)), 4);
                    if (tick == cellSpawnTick) {
                        Location at = c.clone().add(cellX[i] * CELL_SPACING, 0.4, cellZ[i] * CELL_SPACING);
                        DisplayBuilder.playSound(at, Sound.BLOCK_GLASS_PLACE, 0.5f, 1.2f);
                    }
                }
                // GLOW core column + ELECTRIC_SPARK
                Location col = c.clone().add(0, 0.8 + tick * 0.05, 0);
                w.spawnParticle(Particle.GLOW, col, 2, 0.2, 0.4, 0.2, 0.02);
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, col, 3, 0.4, 0.4, 0.4, 0.04);
                }
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.4, 0), 3, 0.4, 160, 220, 255, 1.0f);
                }
            }
            // ============= PHASE 2: ACTIVE — wave pulses outward + Y rotation (40 → dissipateStart) =============
            else if (tick < dissipateStart) {
                int aTick = tick - 40;
                // Continuous Y-axis rotation 0.04 rad/tick per item, slight pulse bob
                float baseScale = 0.8f;
                int wave = (aTick / 20) % 5; // wave passes from center every 20t
                int wavePhase = aTick % 20;
                for (int i = 0; i < cells.size(); i++) {
                    int manhattan = Math.abs(cellX[i]) + Math.abs(cellZ[i]); // 0..4
                    boolean inWave = manhattan == wave && wavePhase < 8;
                    float scale = inWave ? baseScale + 0.15f : baseScale;
                    float by = inWave ? 0.5f : 0.2f;
                    cells.get(i).animateTo(
                            new Vector3f((float)(cellX[i] * CELL_SPACING) - scale / 2, by,
                                    (float)(cellZ[i] * CELL_SPACING) - scale / 2),
                            new AxisAngle4f((float)(aTick * 0.04 + i), 0, 1, 0),
                            new Vector3f(scale), 4);
                }
                // ELECTRIC_SPARK between adjacent items in grid (along rows)
                if (tick % 4 == 0) {
                    for (int gx = -2; gx <= 2; gx++) {
                        Location pa = c.clone().add(gx * CELL_SPACING, 0.4, -2 * CELL_SPACING);
                        Location pb = c.clone().add(gx * CELL_SPACING, 0.4, 2 * CELL_SPACING);
                        DisplayBuilder.particleLine(pa, pb, Particle.ELECTRIC_SPARK, 4, null);
                    }
                    for (int gz = -2; gz <= 2; gz++) {
                        Location pa = c.clone().add(-2 * CELL_SPACING, 0.4, gz * CELL_SPACING);
                        Location pb = c.clone().add(2 * CELL_SPACING, 0.4, gz * CELL_SPACING);
                        DisplayBuilder.particleLine(pa, pb, Particle.ELECTRIC_SPARK, 4, null);
                    }
                }
                // GLOW core column
                Location col = c.clone().add(0, 1.5 + Math.sin(aTick * 0.1) * 0.3, 0);
                w.spawnParticle(Particle.GLOW, col, 2, 0.1, 0.8, 0.1, 0.03);
                // Wave pulse chime
                if (aTick % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f + wave * 0.12f);
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
            // ============= PHASE 3: DISSIPATE — scatter outward 4-8b (final 40t) =============
            else {
                int dTick = tick - dissipateStart;
                if (!dissipateInit) {
                    dissipateInit = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
                }
                double t = Math.min(1.0, dTick / 40.0);
                float endScale = (float)((1.0 - t) * 0.8);
                for (int i = 0; i < cells.size(); i++) {
                    double baseX = cellX[i] * CELL_SPACING;
                    double baseZ = cellZ[i] * CELL_SPACING;
                    double targetX = baseX + Math.cos(scatterAng[i]) * scatterR[i];
                    double targetZ = baseZ + Math.sin(scatterAng[i]) * scatterR[i];
                    double px = baseX + (targetX - baseX) * t;
                    double pz = baseZ + (targetZ - baseZ) * t;
                    cells.get(i).animateTo(
                            new Vector3f((float)px - endScale / 2, 0.2f + (float)(t * 1.5),
                                    (float)pz - endScale / 2),
                            new AxisAngle4f((float)(dTick * 0.3 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, endScale)), 3);
                }
                // BLOCK_GLASS_BREAK cascade
                if (dTick % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.7f + (float)Math.random() * 0.5f);
                }
                // Scatter shrapnel particles
                if (dTick % 2 == 0) {
                    for (int i = 0; i < cells.size(); i++) {
                        double baseX = cellX[i] * CELL_SPACING;
                        double baseZ = cellZ[i] * CELL_SPACING;
                        double targetX = baseX + Math.cos(scatterAng[i]) * scatterR[i];
                        double targetZ = baseZ + Math.sin(scatterAng[i]) * scatterR[i];
                        double px = baseX + (targetX - baseX) * t;
                        double pz = baseZ + (targetZ - baseZ) * t;
                        Location at = c.clone().add(px, 0.6 + t * 1.5, pz);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, at, 2, 0.2, 0.2, 0.2, 0.1);
                        DisplayBuilder.dustParticles(at, 1, 0.2, 160, 220, 255, 1.1f);
                    }
                }
                // END_ROD column above center
                Location col = c.clone().add(0, 1.5 + t * 3.0, 0);
                w.spawnParticle(Particle.END_ROD, col, 2, 0.3, 0.5, 0.3, 0.05);
            }
        }

        @Override public AbstractAttack newInstance() { return new CrystallineLattice(plugin); }
    }

    // ================================================================
    // 24. FROZEN FOOTPRINT TRAIL — 14 ICE footprints appear sequentially
    //     in a wandering path, each pulsing & slow-rotating, then fade
    //     oldest first.
    // ================================================================
    public static class FrozenFootprintTrail extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> prints = new ArrayList<>();
        private final double[] pX = new double[14];
        private final double[] pZ = new double[14];
        private final int[] pAppearTick = new int[14];
        private final boolean[] pVisible = new boolean[14];
        private int dissipateStart;
        private boolean dissipateInit = false;

        public FrozenFootprintTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_footprint_trail", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(28800.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            dissipateStart = config.getDurationTicks() - 40;
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.7f);

            // First footprint at center; subsequent footprints wander out with 1.5 block spacing
            double curX = 0.0;
            double curZ = 0.0;
            double heading = Math.random() * Math.PI * 2;
            double stride = 1.5;

            // Footprint 0 appears at spawn(0-30t); each subsequent every 8t in active phase
            for (int i = 0; i < 14; i++) {
                pX[i] = curX;
                pZ[i] = curZ;
                pAppearTick[i] = (i == 0) ? 0 : 30 + (i - 1) * 8;
                pVisible[i] = false;

                Location p = c.clone().add(curX, 0.15, curZ);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 220, 255).interpolation(6, 0);
                prints.add(h);
                spawnedEntities.add(h.entity());

                // Random new heading (wandering)
                heading += (Math.random() - 0.5) * 1.2;
                curX += Math.cos(heading) * stride;
                curZ += Math.sin(heading) * stride;
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < dissipateStart) {
                // ============= PHASE 1 (0-30t): spawn first footprint + ELECTRIC_SPARK outline =============
                // Then continuously appear new prints (Phase 2 ACTIVE blends in seamlessly)
                for (int i = 0; i < prints.size(); i++) {
                    if (!pVisible[i] && tick >= pAppearTick[i]) {
                        pVisible[i] = true;
                        Location at = c.clone().add(pX[i], 0.15, pZ[i]);
                        // BLOCK_POWDER_SNOW_STEP staggered
                        DisplayBuilder.playSound(at, Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 0.8f + (float)Math.random() * 0.4f);
                        // ELECTRIC_SPARK outline burst
                        for (int j = 0; j < 12; j++) {
                            double a = j * (Math.PI / 6);
                            Location sp = at.clone().add(Math.cos(a) * 0.5, 0.25, Math.sin(a) * 0.5);
                            w.spawnParticle(Particle.ELECTRIC_SPARK, sp, 1, 0, 0, 0, 0.02);
                        }
                        // Initial scale (0 → 1) animation
                        float scale = 1.0f;
                        prints.get(i).animateTo(
                                new Vector3f((float)pX[i] - 0.25f, 0.15f, (float)pZ[i] - 0.35f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(scale * 0.5f, scale * 0.1f, scale * 0.7f), 12);
                    }
                }
                // Pulse brightness + slow rotate per visible footprint; SNOWFLAKE drift
                for (int i = 0; i < prints.size(); i++) {
                    if (!pVisible[i]) continue;
                    int age = tick - pAppearTick[i];
                    double rotPhase = age * 0.03;
                    float pulseScale = (float)(0.5 + Math.sin(age * 0.15) * 0.05);
                    prints.get(i).animateTo(
                            new Vector3f((float)pX[i] - pulseScale / 2, 0.15f, (float)pZ[i] - pulseScale * 0.7f),
                            new AxisAngle4f((float)rotPhase, 0, 1, 0),
                            new Vector3f(pulseScale, 0.1f, pulseScale * 1.4f), 4);
                }
                // SNOWFLAKE drift over each footprint
                if (tick % 4 == 0) {
                    for (int i = 0; i < prints.size(); i++) {
                        if (!pVisible[i]) continue;
                        Location at = c.clone().add(pX[i], 0.4, pZ[i]);
                        w.spawnParticle(Particle.SNOWFLAKE, at, 1, 0.2, 0.1, 0.2, 0.01);
                    }
                }
                // BLOCK_GLASS_HIT ambient
                if (tick % 24 == 0 && tick > 30) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.6f, 0.9f);
                }

                if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                    double r2 = 1.5 * 1.5;
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        for (int i = 0; i < prints.size(); i++) {
                            if (!pVisible[i]) continue;
                            Location at = c.clone().add(pX[i], 0.15, pZ[i]);
                            if (p.getLocation().distanceSquared(at) <= r2) {
                                p.damage(config.getDamage());
                                p.setNoDamageTicks(0);
                                break;
                            }
                        }
                    }
                }
            }
            // ============= PHASE 3: DISSIPATE — fade oldest first (final 40t) =============
            else {
                int dTick = tick - dissipateStart;
                if (!dissipateInit) {
                    dissipateInit = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.0f);
                }
                // Each footprint fades over 12t, staggered by index (oldest=index 0 first)
                int perStagger = 2; // 14 prints * 2 = 28t (fits in 40t)
                for (int i = 0; i < prints.size(); i++) {
                    if (!pVisible[i]) continue;
                    int fadeStart = i * perStagger;
                    double localT = Math.max(0.0, Math.min(1.0, (dTick - fadeStart) / 12.0));
                    float scale = (float)((1.0 - localT) * 0.5);
                    prints.get(i).animateTo(
                            new Vector3f((float)pX[i] - scale / 2, 0.15f, (float)pZ[i] - scale * 0.7f),
                            new AxisAngle4f((float)(dTick * 0.1 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale), Math.max(0.001f, scale * 0.2f), Math.max(0.001f, scale * 1.4f)), 3);
                    // FALLING_DUST puff at the moment fade starts
                    if (dTick == fadeStart) {
                        Location at = c.clone().add(pX[i], 0.3, pZ[i]);
                        w.spawnParticle(Particle.FALLING_DUST, at, 8, 0.4, 0.3, 0.4, 0,
                                Material.SNOW_BLOCK.createBlockData());
                        DisplayBuilder.playSound(at, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f);
                    }
                    // Trail of SCULK_SOUL after fading begins
                    if (localT > 0 && tick % 3 == 0) {
                        Location at = c.clone().add(pX[i], 0.5 + localT * 0.6, pZ[i]);
                        w.spawnParticle(Particle.SCULK_SOUL, at, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrozenFootprintTrail(plugin); }
    }

    // ================================================================
    // 25. SHATTERED MIRROR FLOOR — 36 GLASS_BOTTLE flat tiles in 6x6 grid,
    //     vibrate, then shatter upward and explode radially.
    // ================================================================
    public static class ShatteredMirrorFloor extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tiles = new ArrayList<>();
        private final double[] tFinalX = new double[36];
        private final double[] tFinalZ = new double[36];
        private final double[] tExplodeAng = new double[36];
        private boolean shattered = false;
        private static final int SPAWN_END = 30;
        private static final int VIBRATE_START = 50;
        private static final int SHATTER_TICK = 160;
        private static final double TILE_SPACING = 1.4;

        public ShatteredMirrorFloor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shattered_mirror_floor", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(62400.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(200);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(62400.0);
            config.setImpactRadius(12.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.8f);

            // 6x6 grid centered
            int idx = 0;
            for (int gx = 0; gx < 6; gx++) {
                for (int gz = 0; gz < 6; gz++) {
                    tFinalX[idx] = (gx - 2.5) * TILE_SPACING;
                    tFinalZ[idx] = (gz - 2.5) * TILE_SPACING;
                    tExplodeAng[idx] = Math.atan2(tFinalZ[idx], tFinalX[idx]);
                    if (Double.isNaN(tExplodeAng[idx])) tExplodeAng[idx] = Math.random() * Math.PI * 2;
                    idx++;
                }
            }
            // Spawn at scale 0 at final positions
            for (int i = 0; i < 36; i++) {
                Location p = c.clone().add(tFinalX[i], 0.15, tFinalZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.01f, 0.01f, 0.01f).glow(220, 240, 255).interpolation(6, 0);
                tiles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN — flat tiles materialize (0-30t) =============
            if (tick < SPAWN_END) {
                double t = tick / (double)SPAWN_END;
                float scale = (float)(0.6 * t);
                for (int i = 0; i < tiles.size(); i++) {
                    tiles.get(i).animateTo(
                            new Vector3f((float)tFinalX[i] - scale / 2, 0.15f, (float)tFinalZ[i] - scale / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale), 0.05f, Math.max(0.001f, scale)), 4);
                    if (tick % 4 == 0 && (i + tick) % 6 == 0) {
                        Location at = c.clone().add(tFinalX[i], 0.3, tFinalZ[i]);
                        DisplayBuilder.playSound(at, Sound.BLOCK_GLASS_PLACE, 0.4f, 1.3f);
                    }
                }
                // DUST + END_ROD column during spawn
                Location col = c.clone().add(0, 0.6 + t * 1.5, 0);
                w.spawnParticle(Particle.END_ROD, col, 2, 0.2, 0.4, 0.2, 0.04);
                if (tick % 3 == 0) {
                    for (int k = 0; k < 6; k++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 4.0;
                        Location p = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 0.8, Math.sin(a) * r);
                        DisplayBuilder.dustParticles(p, 1, 0.1, 220, 240, 255, 1.1f);
                    }
                }
            }
            // ============= PHASE 2a: HOLD intact briefly with shimmer (30-50t) =============
            else if (tick < VIBRATE_START) {
                // DUST + GLOW per tile every 5t
                if (tick % 5 == 0) {
                    for (int i = 0; i < tiles.size(); i++) {
                        Location at = c.clone().add(tFinalX[i], 0.3, tFinalZ[i]);
                        DisplayBuilder.dustParticles(at, 1, 0.1, 220, 240, 255, 1.0f);
                        w.spawnParticle(Particle.GLOW, at, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.5f, 1.4f);
                }
            }
            // ============= PHASE 2b: ACTIVE VIBRATION (50-160t) =============
            else if (tick < SHATTER_TICK) {
                int vTick = tick - VIBRATE_START;
                // Vibrate violently — rapid X/Z scale jitter ±0.1
                for (int i = 0; i < tiles.size(); i++) {
                    float jitterX = (float)((Math.random() - 0.5) * 0.2);
                    float jitterZ = (float)((Math.random() - 0.5) * 0.2);
                    float scale = (float)(0.6 + jitterX);
                    float scaleZ = (float)(0.6 + jitterZ);
                    tiles.get(i).animateTo(
                            new Vector3f((float)tFinalX[i] - scale / 2, 0.15f, (float)tFinalZ[i] - scaleZ / 2),
                            new AxisAngle4f((float)((Math.random() - 0.5) * 0.2), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale), 0.05f, Math.max(0.001f, scaleZ)), 2);
                }
                // Rapid BLOCK_GLASS_HIT
                if (vTick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.6f, 0.9f + (float)Math.random() * 0.6f);
                }
                // ELECTRIC_SPARK between random tile pairs (tension)
                if (tick % 3 == 0) {
                    int a = (int)(Math.random() * 36);
                    int b = (int)(Math.random() * 36);
                    Location pa = c.clone().add(tFinalX[a], 0.3, tFinalZ[a]);
                    Location pb = c.clone().add(tFinalX[b], 0.3, tFinalZ[b]);
                    DisplayBuilder.particleLine(pa, pb, Particle.ELECTRIC_SPARK, 3, null);
                }
                // GLOW + DUST shimmer
                if (tick % 2 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 6.0;
                        Location p = c.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                        DisplayBuilder.dustParticles(p, 1, 0.1, 220, 230, 255, 1.0f);
                    }
                }
            }
            // ============= PHASE 3: DISSIPATE — shatter UPWARD then radial explode (final 40t) =============
            else if (!shattered) {
                shattered = true;
                Location impact = c.clone().add(0, 0.5, 0);
                // BLOCK_GLASS_BREAK x5 + ENTITY_GENERIC_EXPLODE
                for (int i = 0; i < 5; i++) {
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.8f + (float)Math.random() * 0.5f);
                }
                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.9f);

                // All tiles fly UPWARD with rotation chaos
                for (int i = 0; i < tiles.size(); i++) {
                    float launchY = 4.0f + (float)Math.random() * 1.5f;
                    tiles.get(i).animateTo(
                            new Vector3f((float)tFinalX[i] - 0.3f, launchY, (float)tFinalZ[i] - 0.3f),
                            new AxisAngle4f((float)(Math.random() * Math.PI * 2),
                                    (float)Math.random(), (float)Math.random(), (float)Math.random()),
                            new Vector3f(0.6f, 0.1f, 0.6f), 20);
                    Location at = c.clone().add(tFinalX[i], 0.4, tFinalZ[i]);
                    w.spawnParticle(Particle.BLOCK, at, 8, 0.3, 0.3, 0.3, 0,
                            Material.GLASS.createBlockData());
                    w.spawnParticle(Particle.ELECTRIC_SPARK, at, 4, 0.3, 0.3, 0.3, 0.1);
                }

                // END_ROD column from impact center
                Location apex = c.clone().add(0, 3.0, 0);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, apex, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.END_ROD, apex, 30, 0.3, 2.5, 0.3, 0.25);
                triggerImpactDamage(impact);
            } else {
                // After shatter: tiles explode in radial pattern
                int dTick = tick - SHATTER_TICK;
                if (dTick == 22) {
                    // Trigger radial explosion outward at peak
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 1.1f);
                    for (int i = 0; i < tiles.size(); i++) {
                        double dist = 5.0 + Math.random() * 3.0;
                        float targetX = (float)(Math.cos(tExplodeAng[i]) * dist);
                        float targetZ = (float)(Math.sin(tExplodeAng[i]) * dist);
                        tiles.get(i).animateTo(
                                new Vector3f(targetX - 0.05f, 0.2f, targetZ - 0.05f),
                                new AxisAngle4f((float)(Math.random() * Math.PI * 2),
                                        (float)Math.random(), (float)Math.random(), (float)Math.random()),
                                new Vector3f(0.01f), 18);
                        Location burst = c.clone().add(targetX, 0.4, targetZ);
                        w.spawnParticle(Particle.BLOCK, burst, 6, 0.3, 0.3, 0.3, 0,
                                Material.GLASS.createBlockData());
                        w.spawnParticle(Particle.ELECTRIC_SPARK, burst, 4, 0.3, 0.3, 0.3, 0.15);
                    }
                }
                // END_ROD column continues
                Location col = c.clone().add(0, 3.0 + dTick * 0.1, 0);
                w.spawnParticle(Particle.END_ROD, col, 2, 0.2, 0.3, 0.2, 0.04);
                // Shrapnel rain
                if (dTick % 3 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 8.0;
                        Location p = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 4.0, Math.sin(a) * r);
                        w.spawnParticle(Particle.BLOCK, p, 1, 0.1, 0.1, 0.1, 0,
                                Material.GLASS.createBlockData());
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                // BLOCK_GLASS_BREAK cascade trailing
                if (dTick == 24 || dTick == 30 || dTick == 36) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.9f + (float)Math.random() * 0.4f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ShatteredMirrorFloor(plugin); }
    }

    // ================================================================
    // 26. PERMAFROST BLOOM — 6 seed GLOW_BERRIES + 12 petal GLOW_BERRIES.
    //     Bloom opens outward then closes inward.
    // ================================================================
    public static class PermafrostBloom extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> seeds = new ArrayList<>(); // first 6
        private final List<ItemDisplayHandle> petals = new ArrayList<>(); // next 12
        private final double[] sAng = new double[6];
        private final double[] pAng = new double[12];
        private static final double SEED_R = 0.6;
        private static final double PETAL_R = 5.0;
        private static final int SPAWN_END = 30;
        private static final int BLOOM_END = 50; // petals fully open by 50t (30+20)
        private int dissipateStart;
        private boolean dissipateInit = false;

        public PermafrostBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_bloom", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(33600.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            dissipateStart = config.getDurationTicks() - 40;
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.1f);

            // 6 seed items at center
            for (int i = 0; i < 6; i++) {
                sAng[i] = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(sAng[i]) * SEED_R, 0.3, Math.sin(sAng[i]) * SEED_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 255, 220).interpolation(8, 0);
                seeds.add(h);
                spawnedEntities.add(h.entity());
            }
            // 12 petal items starting at center, will animate outward in active
            for (int i = 0; i < 12; i++) {
                pAng[i] = Math.PI * 2 * i / 12 + Math.PI / 12;
                Location p = c.clone().add(0, 0.3, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.01f, 0.01f, 0.01f).glow(160, 240, 200).interpolation(8, 0);
                petals.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN — 6 seeds materialize (0-30t) =============
            if (tick < SPAWN_END) {
                double t = tick / (double)SPAWN_END;
                float scale = (float)(0.4 * t);
                for (int i = 0; i < seeds.size(); i++) {
                    sAng[i] += 0.01;
                    float bx = (float)(Math.cos(sAng[i]) * SEED_R);
                    float bz = (float)(Math.sin(sAng[i]) * SEED_R);
                    seeds.get(i).animateTo(
                            new Vector3f(bx - scale / 2, 0.3f, bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.06 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale)), 4);
                }
                // ENCHANT sparkle + GLOW core
                if (tick % 2 == 0) {
                    Location ctr = c.clone().add(0, 0.4, 0);
                    w.spawnParticle(Particle.ENCHANT, ctr, 3, 0.4, 0.3, 0.4, 0.05);
                    w.spawnParticle(Particle.GLOW, ctr, 2, 0.3, 0.3, 0.3, 0.02);
                }
                if (tick % 6 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f + (float)t * 0.4f);
                }
            }
            // ============= PHASE 2: ACTIVE — bloom opens then sustains =============
            else if (tick < dissipateStart) {
                int aTick = tick - SPAWN_END;
                // 6 seeds keep slow rotating + slight bob
                for (int i = 0; i < seeds.size(); i++) {
                    sAng[i] += 0.015;
                    float bx = (float)(Math.cos(sAng[i]) * SEED_R);
                    float bz = (float)(Math.sin(sAng[i]) * SEED_R);
                    float by = (float)(0.3 + Math.sin(aTick * 0.08 + i) * 0.1);
                    seeds.get(i).animateTo(
                            new Vector3f(bx - 0.2f, by, bz - 0.2f),
                            new AxisAngle4f((float)(aTick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.4f), 4);
                }
                // Petals open outward — animateTo position over 20t each, staggered (1.5t per petal)
                for (int i = 0; i < petals.size(); i++) {
                    int petalStart = i * 1; // very small stagger
                    int localTick = aTick - petalStart;
                    if (localTick < 0) localTick = 0;
                    double openT = Math.min(1.0, localTick / 20.0);
                    pAng[i] += 0.02;
                    double radius = PETAL_R * openT;
                    float bx = (float)(Math.cos(pAng[i]) * radius);
                    float bz = (float)(Math.sin(pAng[i]) * radius);
                    float by = (float)(0.4 + Math.sin(aTick * 0.06 + i) * 0.15);
                    float scale = (float)(0.5 * Math.min(1.0, openT + 0.2));
                    petals.get(i).animateTo(
                            new Vector3f(bx - scale / 2, by, bz - scale / 2),
                            new AxisAngle4f((float)(aTick * 0.04 + i), 0, 1, 0),
                            new Vector3f(scale), 4);
                    // Chime each time petal reaches its target
                    if (aTick == petalStart + 20) {
                        Location at = c.clone().add(bx, by, bz);
                        DisplayBuilder.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.0f + i * 0.04f);
                        w.spawnParticle(Particle.ENCHANT, at, 6, 0.3, 0.3, 0.3, 0.08);
                    }
                }
                // SCULK_SOUL between petals + GLOW core column + ENCHANT sparkle on each petal
                if (tick % 3 == 0) {
                    for (int i = 0; i < petals.size(); i++) {
                        double curR = PETAL_R * Math.min(1.0, Math.max(0.0, (aTick - i) / 20.0));
                        Location at = c.clone().add(Math.cos(pAng[i]) * curR, 0.5, Math.sin(pAng[i]) * curR);
                        if (i % 2 == 0) {
                            Location between = c.clone().add(Math.cos(pAng[i] + Math.PI / 12) * curR * 0.7,
                                    0.5, Math.sin(pAng[i] + Math.PI / 12) * curR * 0.7);
                            w.spawnParticle(Particle.SCULK_SOUL, between, 1, 0.1, 0.1, 0.1, 0.01);
                        }
                        w.spawnParticle(Particle.ENCHANT, at, 1, 0.2, 0.2, 0.2, 0.03);
                    }
                }
                Location col = c.clone().add(0, 1.5 + Math.sin(aTick * 0.08) * 0.3, 0);
                w.spawnParticle(Particle.GLOW, col, 2, 0.1, 0.6, 0.1, 0.02);
                // BLOCK_AMETHYST_BLOCK_HIT during pulse (every 18t after bloom complete)
                if (aTick >= 20 && aTick % 18 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 1.1f);
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
            // ============= PHASE 3: DISSIPATE — close inward + shrink (final 40t) =============
            else {
                int dTick = tick - dissipateStart;
                if (!dissipateInit) {
                    dissipateInit = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 0.7f);
                }
                double t = Math.min(1.0, dTick / 40.0);
                // Petals close inward (animateTo center)
                for (int i = 0; i < petals.size(); i++) {
                    double radius = PETAL_R * (1.0 - t);
                    float bx = (float)(Math.cos(pAng[i]) * radius);
                    float bz = (float)(Math.sin(pAng[i]) * radius);
                    float scale = (float)((1.0 - t) * 0.4);
                    petals.get(i).animateTo(
                            new Vector3f(bx - scale / 2, 0.4f, bz - scale / 2),
                            new AxisAngle4f((float)(dTick * 0.1 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale)), 3);
                }
                // Seeds shrink too
                for (int i = 0; i < seeds.size(); i++) {
                    float scale = (float)((1.0 - t) * 0.4);
                    float bx = (float)(Math.cos(sAng[i]) * SEED_R * (1.0 - t));
                    float bz = (float)(Math.sin(sAng[i]) * SEED_R * (1.0 - t));
                    seeds.get(i).animateTo(
                            new Vector3f(bx - scale / 2, 0.3f, bz - scale / 2),
                            new AxisAngle4f((float)(dTick * 0.15 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale)), 3);
                }
                // ENCHANT spirals inward
                if (dTick % 2 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = PETAL_R * (1.0 - t) + Math.random();
                        Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 0.6, Math.sin(a) * r);
                        w.spawnParticle(Particle.ENCHANT, p, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                // Final ENCHANT explosion + soft generic explode
                if (dTick == 36) {
                    Location ctr = c.clone().add(0, 0.5, 0);
                    DisplayBuilder.playSound(ctr, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                    w.spawnParticle(Particle.ENCHANT, ctr, 40, 1.5, 1.0, 1.5, 0.5);
                    w.spawnParticle(Particle.GLOW, ctr, 12, 0.6, 0.5, 0.6, 0.1);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PermafrostBloom(plugin); }
    }

    // ================================================================
    // 27. AURORA RIBBONS — 30 PRISMARINE_SHARD at Y+10-14 (3 ribbons of 10),
    //     sway in sin curves, then fragment burst upward.
    // ================================================================
    public static class AuroraRibbons extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] rBaseX = new double[30];
        private final double[] rBaseY = new double[30];
        private final double[] rBaseZ = new double[30];
        private static final int[][] AURORA_COLORS = {
                {120, 220, 240}, // cyan
                {180, 120, 240}, // purple
                {120, 240, 160}  // green
        };
        private int dissipateStart;
        private boolean dissipateInit = false;

        public AuroraRibbons(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("aurora_ribbons", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(28800.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(240);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            dissipateStart = config.getDurationTicks() - 40;
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);

            // 30 ribbons arranged in flowing waving pattern at Y+10-14 (3 ribbons of 10)
            for (int arc = 0; arc < 3; arc++) {
                double arcStart = Math.PI * 2 * arc / 3;
                for (int j = 0; j < 10; j++) {
                    int i = arc * 10 + j;
                    double ang = arcStart + (j / 10.0) * (Math.PI / 2);
                    double r = 8.0 + Math.sin(j * 0.4) * 1.0;
                    rBaseX[i] = Math.cos(ang) * r;
                    rBaseY[i] = 10.0 + (j / 9.0) * 4.0;
                    rBaseZ[i] = Math.sin(ang) * r;
                    Location p = c.clone().add(rBaseX[i], rBaseY[i], rBaseZ[i]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PRISMARINE_SHARD));
                    h.scale(0.01f, 0.01f, 0.01f)
                            .glow(AURORA_COLORS[0][0], AURORA_COLORS[0][1], AURORA_COLORS[0][2])
                            .interpolation(6, 0);
                    shards.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN — sequential materialization (0-40t) =============
            if (tick < 40) {
                int[] col = AURORA_COLORS[0];
                for (int i = 0; i < shards.size(); i++) {
                    int j = i % 10;
                    int lightupTick = j * 3;
                    double t = Math.max(0.0, Math.min(1.0, (tick - lightupTick) / 16.0));
                    float scale = (float)(0.6 * t);
                    shards.get(i).animateTo(
                            new Vector3f((float)rBaseX[i] - scale / 2, (float)rBaseY[i],
                                    (float)rBaseZ[i] - scale / 2),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0.3f),
                            new Vector3f(Math.max(0.001f, scale), Math.max(0.001f, scale * 0.4f),
                                    Math.max(0.001f, scale)), 4);
                    if (tick == lightupTick) {
                        shards.get(i).glow(col[0], col[1], col[2]);
                        Location at = c.clone().add(rBaseX[i], rBaseY[i], rBaseZ[i]);
                        DisplayBuilder.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f + (float)Math.random() * 0.6f);
                    }
                }
                // Glow squid ambient buildup
                if (tick % 12 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 0.6f);
                }
                if (tick % 3 == 0) {
                    for (int i = 0; i < shards.size(); i += 4) {
                        Location at = c.clone().add(rBaseX[i], rBaseY[i], rBaseZ[i]);
                        w.spawnParticle(Particle.END_ROD, at, 1, 0.3, 0.3, 0.3, 0.04);
                    }
                }
            }
            // ============= PHASE 2: ACTIVE — sway with sin curve =============
            else if (tick < dissipateStart) {
                int aTick = tick - 40;
                // Sway: position bobs Y+sin(index*0.3 + tick*0.05) * 1.0
                for (int i = 0; i < shards.size(); i++) {
                    double yBob = Math.sin(i * 0.3 + aTick * 0.05) * 1.0;
                    double xSway = Math.sin(aTick * 0.04 + i * 0.2) * 1.4;
                    float bx = (float)(rBaseX[i] + xSway);
                    float by = (float)(rBaseY[i] + yBob);
                    float bz = (float)rBaseZ[i];
                    shards.get(i).animateTo(
                            new Vector3f(bx - 0.3f, by, bz - 0.3f),
                            new AxisAngle4f((float)(aTick * 0.04 + i), 0, 1, 0.3f),
                            new Vector3f(0.6f, 0.24f, 0.6f), 6);
                }
                // Colored DUST cycling per item
                int[] col = AURORA_COLORS[(aTick / 20) % AURORA_COLORS.length];
                if (tick % 2 == 0) {
                    for (int i = 0; i < shards.size(); i += 2) {
                        double yBob = Math.sin(i * 0.3 + aTick * 0.05) * 1.0;
                        double xSway = Math.sin(aTick * 0.04 + i * 0.2) * 1.4;
                        Location at = c.clone().add(rBaseX[i] + xSway, rBaseY[i] + yBob, rBaseZ[i]);
                        DisplayBuilder.dustParticles(at, 1, 0.2, col[0], col[1], col[2], 1.3f);
                    }
                    if (aTick % 20 == 0) {
                        for (ItemDisplayHandle s : shards) s.glow(col[0], col[1], col[2]);
                    }
                }
                // ELECTRIC_SPARK along ribbon path
                if (tick % 3 == 0) {
                    for (int arc = 0; arc < 3; arc++) {
                        for (int j = 0; j < 9; j++) {
                            int i = arc * 10 + j;
                            int next = i + 1;
                            double yb1 = Math.sin(i * 0.3 + aTick * 0.05) * 1.0;
                            double xs1 = Math.sin(aTick * 0.04 + i * 0.2) * 1.4;
                            double yb2 = Math.sin(next * 0.3 + aTick * 0.05) * 1.0;
                            double xs2 = Math.sin(aTick * 0.04 + next * 0.2) * 1.4;
                            Location a = c.clone().add(rBaseX[i] + xs1, rBaseY[i] + yb1, rBaseZ[i]);
                            Location b = c.clone().add(rBaseX[next] + xs2, rBaseY[next] + yb2, rBaseZ[next]);
                            DisplayBuilder.particleLine(a, b, Particle.ELECTRIC_SPARK, 3, null);
                        }
                    }
                }
                // END_ROD random shimmer
                if (tick % 4 == 0) {
                    for (int k = 0; k < 6; k++) {
                        int i = (int)(Math.random() * shards.size());
                        double yBob = Math.sin(i * 0.3 + aTick * 0.05) * 1.0;
                        double xSway = Math.sin(aTick * 0.04 + i * 0.2) * 1.4;
                        Location at = c.clone().add(rBaseX[i] + xSway, rBaseY[i] + yBob, rBaseZ[i]);
                        w.spawnParticle(Particle.END_ROD, at, 1, 0.2, 0.2, 0.2, 0.03);
                    }
                }
                // Glow squid ambient pitch 0.6
                if (aTick % 40 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 0.6f);
                }
                // Random gentle chime
                if (aTick % 18 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                            0.5f, 0.6f + (float)Math.random() * 0.8f);
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
            // ============= PHASE 3: DISSIPATE — fragment burst upward (final 40t) =============
            else {
                int dTick = tick - dissipateStart;
                if (!dissipateInit) {
                    dissipateInit = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
                }
                double t = Math.min(1.0, dTick / 40.0);
                float endScale = (float)((1.0 - t) * 0.6);
                for (int i = 0; i < shards.size(); i++) {
                    float by = (float)(rBaseY[i] + t * 10.0); // burst Y+10 over 40t
                    shards.get(i).animateTo(
                            new Vector3f((float)rBaseX[i] - endScale / 2, by, (float)rBaseZ[i] - endScale / 2),
                            new AxisAngle4f((float)(dTick * 0.2 + i), 0, 1, 0.3f),
                            new Vector3f(Math.max(0.001f, endScale), Math.max(0.001f, endScale * 0.4f),
                                    Math.max(0.001f, endScale)), 3);
                }
                // END_ROD + glass break trails
                if (dTick % 2 == 0) {
                    for (int i = 0; i < shards.size(); i++) {
                        Location at = c.clone().add(rBaseX[i], rBaseY[i] + t * 10.0, rBaseZ[i]);
                        w.spawnParticle(Particle.END_ROD, at, 1, 0.2, 0.3, 0.2, 0.05);
                    }
                }
                if (dTick == 14 || dTick == 28) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.0f + (float)Math.random() * 0.3f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new AuroraRibbons(plugin); }
    }

    // ================================================================
    // 28. ICE METEOR SHOWER — 6 PACKED_ICE meteors materialize at Y+18,
    //     fall in staggered order, impact one at a time.
    // ================================================================
    public static class IceMeteorShower extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> meteors = new ArrayList<>();
        private final double[] mAng = new double[6];
        private final double[] mR = new double[6];
        private final double[] mY = new double[6];
        private final double[] mTargetY = new double[6];
        private final int[] mFallStartTick = new int[6];
        private final boolean[] mImpacted = new boolean[6];
        private int dissipateStart;
        private boolean dissipateInit = false;
        private static final int SPAWN_END = 50;

        public IceMeteorShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_meteor_shower", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(54000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(300);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(54000.0);
            config.setImpactRadius(6.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            dissipateStart = config.getDurationTicks() - 40;
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 0.5f);

            // 6 meteors materialize at Y+18 in clustered formation
            for (int i = 0; i < 6; i++) {
                mAng[i] = Math.PI * 2 * i / 6 + (Math.random() - 0.5) * 0.4;
                mR[i] = 2.0 + Math.random() * 4.0;
                mY[i] = 18.0 + Math.random() * 1.0;
                mTargetY[i] = mY[i];
                // Stagger fall: 40t between each (starting after spawn ends)
                mFallStartTick[i] = SPAWN_END + i * 40;
                mImpacted[i] = false;
                Location p = c.clone().add(Math.cos(mAng[i]) * mR[i], mY[i], Math.sin(mAng[i]) * mR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(150, 200, 255).interpolation(6, 0);
                meteors.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN — scale up at apex with charge effect (0-50t) =============
            if (tick < SPAWN_END) {
                double t = tick / (double)SPAWN_END;
                float scale = (float)(1.2 * t);
                for (int i = 0; i < meteors.size(); i++) {
                    float bx = (float)(Math.cos(mAng[i]) * mR[i]);
                    float bz = (float)(Math.sin(mAng[i]) * mR[i]);
                    float bobY = (float)(mY[i] + Math.sin(tick * 0.1 + i) * 0.3);
                    meteors.get(i).animateTo(
                            new Vector3f(bx - scale / 2, bobY, bz - scale / 2),
                            new AxisAngle4f((float)(tick * 0.15 + i), 1, 0.5f, 0.3f),
                            new Vector3f(Math.max(0.001f, scale)), 4);
                    // SOUL_FIRE_FLAME cores at each meteor
                    Location at = c.clone().add(bx, mY[i], bz);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, at, 1, 0.4, 0.4, 0.4, 0.02);
                }
                // ITEM_TRIDENT_RIPTIDE_2 long charge
                if (tick % 14 == 0) {
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.8f, 0.5f + (float)t * 0.4f);
                }
                // ELECTRIC_SPARK at apex of each meteor
                if (tick % 2 == 0) {
                    for (int i = 0; i < meteors.size(); i++) {
                        float bx = (float)(Math.cos(mAng[i]) * mR[i]);
                        float bz = (float)(Math.sin(mAng[i]) * mR[i]);
                        Location at = c.clone().add(bx, mY[i] + 1.0, bz);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, at, 2, 0.3, 0.2, 0.3, 0.05);
                    }
                }
            }
            // ============= PHASE 2: ACTIVE — staggered fall =============
            else if (tick < dissipateStart) {
                for (int i = 0; i < meteors.size(); i++) {
                    if (mImpacted[i]) continue;
                    if (tick < mFallStartTick[i]) {
                        // Still waiting — hover at apex
                        float bx = (float)(Math.cos(mAng[i]) * mR[i]);
                        float bz = (float)(Math.sin(mAng[i]) * mR[i]);
                        float bobY = (float)(mY[i] + Math.sin(tick * 0.1 + i) * 0.3);
                        meteors.get(i).animateTo(
                                new Vector3f(bx - 0.6f, bobY, bz - 0.6f),
                                new AxisAngle4f((float)(tick * 0.1 + i), 1, 0.5f, 0.3f),
                                new Vector3f(1.2f), 4);
                        // SOUL_FIRE_FLAME continues at apex
                        Location at = c.clone().add(bx, mY[i], bz);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, at, 1, 0.4, 0.4, 0.4, 0.02);
                        continue;
                    }
                    // Falling
                    mY[i] -= 0.7;
                    Location pos = c.clone().add(Math.cos(mAng[i]) * mR[i], mY[i], Math.sin(mAng[i]) * mR[i]);
                    meteors.get(i).animateTo(
                            new Vector3f((float)(Math.cos(mAng[i]) * mR[i]) - 0.6f, (float)mY[i],
                                    (float)(Math.sin(mAng[i]) * mR[i]) - 0.6f),
                            new AxisAngle4f((float)(tick * 0.4 + i), 1, 0.5f, 0.3f),
                            new Vector3f(1.2f), 2);

                    // SNOWFLAKE trail (10/tick), SOUL_FIRE_FLAME core
                    for (int k = 0; k < 10; k++) {
                        Location trail = pos.clone().add(
                                (Math.random() - 0.5) * 0.6,
                                Math.random() * 0.6,
                                (Math.random() - 0.5) * 0.6);
                        w.spawnParticle(Particle.SNOWFLAKE, trail, 1, 0, 0, 0, 0);
                    }
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 4, 0.3, 0.3, 0.3, 0.05);
                    DisplayBuilder.dustParticles(pos, 2, 0.3, 180, 220, 255, 1.3f);

                    if (tick % 8 == 0) {
                        DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.5f, 0.8f);
                    }

                    if (mY[i] < 0.6) {
                        Location impact = c.clone().add(Math.cos(mAng[i]) * mR[i], 0.4, Math.sin(mAng[i]) * mR[i]);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.7f);
                        DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 0.9f);

                        // ELECTRIC_SPARK ring + END_ROD burst + crater of FALLING_DUST
                        int pts = 24;
                        for (int j = 0; j < pts; j++) {
                            double a = Math.PI * 2 * j / pts;
                            Location ring = impact.clone().add(Math.cos(a) * 2.5, 0.3, Math.sin(a) * 2.5);
                            w.spawnParticle(Particle.ELECTRIC_SPARK, ring, 2, 0.1, 0.1, 0.1, 0.05);
                        }
                        w.spawnParticle(Particle.END_ROD, impact, 20, 0.6, 0.6, 0.6, 0.3);
                        for (int j = 0; j < 18; j++) {
                            double a = Math.random() * Math.PI * 2;
                            double r = Math.random() * 3.0;
                            Location crater = impact.clone().add(Math.cos(a) * r, 0.2 + Math.random() * 0.5, Math.sin(a) * r);
                            w.spawnParticle(Particle.FALLING_DUST, crater, 1, 0.1, 0.1, 0.1, 0,
                                    Material.SNOW_BLOCK.createBlockData());
                        }
                        w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);

                        triggerImpactDamage(impact);
                        mImpacted[i] = true;
                    }
                }
            }
            // ============= PHASE 3: DISSIPATE — craters glow & fade, smoke columns (final 40t) =============
            else {
                int dTick = tick - dissipateStart;
                if (!dissipateInit) {
                    dissipateInit = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                }
                // End_rod glow then fade at each impact crater
                double fade = 1.0 - Math.min(1.0, dTick / 40.0);
                if (dTick % 2 == 0) {
                    for (int i = 0; i < meteors.size(); i++) {
                        if (!mImpacted[i]) continue;
                        Location crater = c.clone().add(Math.cos(mAng[i]) * mR[i], 0.4, Math.sin(mAng[i]) * mR[i]);
                        w.spawnParticle(Particle.END_ROD, crater, (int)(2 * fade) + 1, 0.6, 0.3, 0.6, 0.05);
                        // Smoke column rising
                        Location smokeTop = crater.clone().add(0, 1.0 + Math.random() * 2.5, 0);
                        w.spawnParticle(Particle.CLOUD, smokeTop, 1, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IceMeteorShower(plugin); }
    }

    // ================================================================
    // 29. FROZEN STORM — dark cloud of 12 GLASS_BOTTLE at Y+10 rotating
    //     + 6 NETHER_STAR origins. Lightning strikes from stars to ground.
    // ================================================================
    public static class FrozenStorm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stars = new ArrayList<>();
        private final List<ItemDisplayHandle> cloudBits = new ArrayList<>();
        private final double[] sAng = new double[6];
        private final double[] sR = new double[6];
        private final double[] sY = new double[6];
        private final double[] cbAng = new double[12];
        private final double[] cbR = new double[12];
        private final double[] cbY = new double[12];
        private double cloudRotation = 0.0;
        private int dissipateStart;
        private boolean dissipateInit = false;
        private static final int SPAWN_END = 40;

        public FrozenStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(26400.0);
            config.setDamageRadius(18.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(400);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            dissipateStart = config.getDurationTicks() - 40;
            DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.0f, 0.6f);

            // 6 NETHER_STAR origin points around perimeter at Y+10
            for (int i = 0; i < 6; i++) {
                sAng[i] = Math.PI * 2 * i / 6;
                sR[i] = 8.0;
                sY[i] = 10.0;
                Location p = c.clone().add(Math.cos(sAng[i]) * sR[i], sY[i], Math.sin(sAng[i]) * sR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_STAR));
                h.scale(0.01f, 0.01f, 0.01f).glow(220, 240, 255).interpolation(8, 0);
                stars.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 GLASS_BOTTLE cloud bits at Y+10 rotating
            for (int i = 0; i < 12; i++) {
                cbAng[i] = Math.PI * 2 * i / 12;
                cbR[i] = 4.5 + (i % 3) * 1.0;
                cbY[i] = 10.0 + Math.sin(i * 0.7) * 0.8;
                Location p = c.clone().add(Math.cos(cbAng[i]) * cbR[i], cbY[i], Math.sin(cbAng[i]) * cbR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.01f, 0.01f, 0.01f).glow(160, 180, 210).interpolation(8, 0);
                cloudBits.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN — cloud + stars scale up (0-40t) =============
            if (tick < SPAWN_END) {
                double t = tick / (double)SPAWN_END;
                float starScale = (float)(0.6 * t);
                float cloudScale = (float)(1.0 * t);
                for (int i = 0; i < stars.size(); i++) {
                    sAng[i] += 0.02;
                    float bx = (float)(Math.cos(sAng[i]) * sR[i]);
                    float bz = (float)(Math.sin(sAng[i]) * sR[i]);
                    stars.get(i).animateTo(
                            new Vector3f(bx - starScale / 2, (float)sY[i], bz - starScale / 2),
                            new AxisAngle4f((float)(tick * 0.15 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, starScale)), 4);
                }
                cloudRotation += 0.03;
                for (int i = 0; i < cloudBits.size(); i++) {
                    cbAng[i] -= 0.025;
                    float bx = (float)(Math.cos(cbAng[i] + cloudRotation) * cbR[i]);
                    float bz = (float)(Math.sin(cbAng[i] + cloudRotation) * cbR[i]);
                    cloudBits.get(i).animateTo(
                            new Vector3f(bx - cloudScale / 2, (float)cbY[i], bz - cloudScale / 2),
                            new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, cloudScale)), 4);
                }
                // CLOUD + ELECTRIC_SPARK gathering
                if (tick % 2 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = 6.0 + Math.random() * 4.0;
                        Location p = c.clone().add(Math.cos(a) * r, 9.0 + Math.random() * 2.0, Math.sin(a) * r);
                        w.spawnParticle(Particle.CLOUD, p, 1, 0.3, 0.3, 0.3, 0.02);
                        if (Math.random() < 0.3) {
                            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.04);
                        }
                    }
                }
                // SOUL_FIRE_FLAME wind howl wisps
                if (tick % 4 == 0) {
                    Location wind = c.clone().add(
                            (Math.random() - 0.5) * 8.0, 9.0 + Math.random() * 2.0, (Math.random() - 0.5) * 8.0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, wind, 2, 0.4, 0.4, 0.4, 0.05);
                }
                if (tick % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.8f, 0.5f + (float)t * 0.4f);
                }
            }
            // ============= PHASE 2: ACTIVE — cloud rotates, lightning strikes =============
            else if (tick < dissipateStart) {
                int aTick = tick - SPAWN_END;
                cloudRotation += 0.05; // 0.05 rad/tick spec
                // Rotate stars (orbit perimeter, same Y)
                for (int i = 0; i < stars.size(); i++) {
                    sAng[i] += 0.02;
                    float by = (float)(sY[i] + Math.sin(aTick * 0.06 + i) * 0.3);
                    float bx = (float)(Math.cos(sAng[i]) * sR[i]);
                    float bz = (float)(Math.sin(sAng[i]) * sR[i]);
                    stars.get(i).animateTo(
                            new Vector3f(bx - 0.3f, by, bz - 0.3f),
                            new AxisAngle4f((float)(aTick * 0.2 + i), 0, 1, 0),
                            new Vector3f(0.6f), 4);
                }
                // Rotate cloud bits — use cloudRotation
                for (int i = 0; i < cloudBits.size(); i++) {
                    double angle = cbAng[i] + cloudRotation;
                    float bx = (float)(Math.cos(angle) * cbR[i]);
                    float bz = (float)(Math.sin(angle) * cbR[i]);
                    float by = (float)(cbY[i] + Math.sin(aTick * 0.04 + i) * 0.4);
                    cloudBits.get(i).animateTo(
                            new Vector3f(bx - 0.5f, by, bz - 0.5f),
                            new AxisAngle4f((float)(aTick * 0.08 + i), 0, 1, 0),
                            new Vector3f(1.0f), 4);
                }
                // Lightning bolt every 60t from a NETHER_STAR to ground
                if (aTick % 60 == 0 && aTick > 0) {
                    int s = (int)(Math.random() * 6);
                    Location top = c.clone().add(Math.cos(sAng[s]) * sR[s], sY[s], Math.sin(sAng[s]) * sR[s]);
                    Location btm = c.clone().add(Math.cos(sAng[s]) * sR[s], 0.5, Math.sin(sAng[s]) * sR[s]);
                    // ELECTRIC_SPARK line straight path
                    DisplayBuilder.particleLine(top, btm, Particle.ELECTRIC_SPARK, 30, null);
                    DisplayBuilder.particleLine(top, btm, Particle.END_ROD, 8, null);
                    w.spawnParticle(Particle.FLASH, btm, 1, 0, 0, 0, 0);
                    // FALLING_DUST(WHITE_CONCRETE) at strike point
                    for (int j = 0; j < 14; j++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 2.0;
                        Location dust = btm.clone().add(Math.cos(a) * r, 0.2 + Math.random() * 0.5, Math.sin(a) * r);
                        w.spawnParticle(Particle.FALLING_DUST, dust, 1, 0.1, 0.1, 0.1, 0,
                                Material.WHITE_CONCRETE.createBlockData());
                    }
                    DisplayBuilder.playSound(btm, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.7f);
                    DisplayBuilder.playSound(btm, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);
                }
                // Wind howl SOUL_FIRE_FLAME wisps around cloud + CLOUD particles
                if (tick % 2 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double a = (i / 10.0) * Math.PI * 2 + aTick * 0.04;
                        double r = 5.0 + Math.sin(aTick * 0.05 + i) * 1.5;
                        Location p = c.clone().add(Math.cos(a) * r, 9.5 + Math.sin(i * 0.5) * 1.2, Math.sin(a) * r);
                        w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.2, 0.2, 0.02);
                        if (Math.random() < 0.25) {
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.1, 0.1, 0.1, 0.03);
                        }
                    }
                }
                // ELECTRIC_SPARK swirl inside cloud
                if (tick % 4 == 0) {
                    Location swirl = c.clone().add(0, 10.0, 0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, swirl, 2, 2.0, 0.5, 2.0, 0.05);
                }
                // BLOCK_GLASS_BREAK ambient
                if (aTick % 50 == 0 && aTick > 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.0f);
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
            // ============= PHASE 3: DISSIPATE — cloud dissolves outward, stars fade (final 40t) =============
            else {
                int dTick = tick - dissipateStart;
                if (!dissipateInit) {
                    dissipateInit = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
                }
                double t = Math.min(1.0, dTick / 40.0);
                // Stars fade in place (scale down)
                for (int i = 0; i < stars.size(); i++) {
                    float scale = (float)((1.0 - t) * 0.6);
                    float bx = (float)(Math.cos(sAng[i]) * sR[i]);
                    float bz = (float)(Math.sin(sAng[i]) * sR[i]);
                    stars.get(i).animateTo(
                            new Vector3f(bx - scale / 2, (float)sY[i], bz - scale / 2),
                            new AxisAngle4f((float)(dTick * 0.15 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale)), 3);
                }
                // Cloud bits dissolve outward — radius grows, scale shrinks
                cloudRotation += 0.04;
                for (int i = 0; i < cloudBits.size(); i++) {
                    double angle = cbAng[i] + cloudRotation;
                    double rOut = cbR[i] + t * 6.0;
                    float bx = (float)(Math.cos(angle) * rOut);
                    float bz = (float)(Math.sin(angle) * rOut);
                    float by = (float)(cbY[i] + t * 2.0);
                    float scale = (float)((1.0 - t) * 1.0);
                    cloudBits.get(i).animateTo(
                            new Vector3f(bx - scale / 2, by, bz - scale / 2),
                            new AxisAngle4f((float)(dTick * 0.12 + i), 0, 1, 0),
                            new Vector3f(Math.max(0.001f, scale)), 3);
                }
                // CLOUD particles dispersing
                if (dTick % 2 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = 4.0 + t * 8.0 + Math.random() * 2.0;
                        Location p = c.clone().add(Math.cos(a) * r, 9.0 + t * 2.0 + Math.random(), Math.sin(a) * r);
                        w.spawnParticle(Particle.CLOUD, p, 1, 0.3, 0.3, 0.3, 0.02);
                    }
                }
                if (dTick == 14 || dTick == 30) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.0f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrozenStorm(plugin); }
    }

    // ================================================================
    // 30. SKY SHATTERING PRISM — 1 DIAMOND at Y+12 with rainbow refraction,
    //     shatters into 20 fragments scattering outward.
    // ================================================================
    public static class SkyShatteringPrism extends EnvironmentalAttack {
        private ItemDisplayHandle prism;
        private final List<ItemDisplayHandle> fragments = new ArrayList<>();
        private final double[] fAng = new double[20];
        private final double[] fR = new double[20];
        private final double[] fLandX = new double[20];
        private final double[] fLandZ = new double[20];
        private final boolean[] fImpacted = new boolean[20];
        private double prismRotation = 0.0;
        private boolean shattered = false;
        private static final int SPAWN_END = 50;
        private static final int DISSIPATE_LENGTH = 30;
        private static final double PRISM_Y = 12.0;
        private int shatterTick;
        // Rainbow palette
        private static final int[][] RAINBOW = {
                {255, 60, 60},   // red
                {255, 150, 50},  // orange
                {255, 230, 60},  // yellow
                {80, 220, 80},   // green
                {80, 150, 255},  // blue
                {180, 80, 255}   // purple
        };

        public SkyShatteringPrism(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_shattering_prism", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(36000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(36000.0);
            config.setImpactRadius(3.75);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            // Shatter at duration - 30t (dissipate length)
            shatterTick = config.getDurationTicks() - DISSIPATE_LENGTH;
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.8f);

            // The prism — single DIAMOND at Y+12
            Location p = c.clone().add(0, PRISM_Y, 0);
            prism = displayBuilder.spawnItem(p, new ItemStack(Material.DIAMOND));
            prism.scale(0.01f, 0.01f, 0.01f).glow(180, 240, 255).interpolation(8, 0);
            spawnedEntities.add(prism.entity());

            // Pre-create 20 fragments hidden inside the prism
            for (int i = 0; i < 20; i++) {
                fAng[i] = Math.PI * 2 * i / 20 + (Math.random() - 0.5) * 0.3;
                fR[i] = 0.0;
                // Pre-compute random landing position 4-10b out
                double landR = 4.0 + Math.random() * 6.0;
                fLandX[i] = Math.cos(fAng[i]) * landR;
                fLandZ[i] = Math.sin(fAng[i]) * landR;
                fImpacted[i] = false;
                Location fp = c.clone().add(0, PRISM_Y, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(fp, new ItemStack(Material.DIAMOND));
                h.scale(0.001f, 0.001f, 0.001f).glow(200, 240, 255).interpolation(4, 0);
                fragments.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // ============= PHASE 1: SPAWN — scale up + accumulating ELECTRIC_SPARK shell (0-50t) =============
            if (tick < SPAWN_END) {
                double t = tick / (double)SPAWN_END;
                float scale = (float)(1.5 * t);
                prismRotation += 0.04;
                prism.animateTo(
                        new Vector3f(-scale / 2, (float)PRISM_Y, -scale / 2),
                        new AxisAngle4f((float)prismRotation, 0.3f, 1, 0.3f),
                        new Vector3f(Math.max(0.001f, scale)), 4);
                // Accumulating ELECTRIC_SPARK shell + END_ROD
                if (tick % 2 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = tick * 0.2 + i * (Math.PI / 3);
                        double r = 3.0 * (1.0 - t * 0.7);
                        Location sp = c.clone().add(Math.cos(a) * r, PRISM_Y + Math.sin(tick * 0.1 + i) * 0.5, Math.sin(a) * r);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, sp, 1, 0.1, 0.1, 0.1, 0.04);
                        w.spawnParticle(Particle.END_ROD, sp, 1, 0.1, 0.1, 0.1, 0.04);
                    }
                }
                // BLOCK_AMETHYST_BLOCK_CHIME rising ladder during charge
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f + (float)t * 1.0f);
                }
                // DUST cycling colors
                if (tick % 3 == 0) {
                    int[] col = RAINBOW[(tick / 4) % RAINBOW.length];
                    Location at = c.clone().add(0, PRISM_Y, 0);
                    DisplayBuilder.dustParticles(at, 4, 0.8, col[0], col[1], col[2], 1.3f);
                }
            }
            // ============= PHASE 2: ACTIVE — rainbow refraction vibration (50 → shatterTick) =============
            else if (tick < shatterTick) {
                int aTick = tick - SPAWN_END;
                prismRotation += 0.04;
                // Small vibration jitter
                float jitterX = (float)((Math.random() - 0.5) * 0.1);
                float jitterY = (float)((Math.random() - 0.5) * 0.1);
                float jitterZ = (float)((Math.random() - 0.5) * 0.1);
                prism.animateTo(
                        new Vector3f(-0.75f + jitterX, (float)PRISM_Y + jitterY, -0.75f + jitterZ),
                        new AxisAngle4f((float)prismRotation, 0.3f, 1, 0.3f),
                        new Vector3f(1.5f), 2);
                // DUST in cycling rainbow colors per tick
                int[] col = RAINBOW[(aTick / 4) % RAINBOW.length];
                Location at = c.clone().add(0, PRISM_Y, 0);
                DisplayBuilder.dustParticles(at, 3, 1.2, col[0], col[1], col[2], 1.4f);
                // END_ROD shimmer
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, at, 3, 1.0, 0.8, 1.0, 0.05);
                }
                // Internal GLOW intensifies
                w.spawnParticle(Particle.GLOW, at, 2, 0.4, 0.4, 0.4, 0.03);
                // BLOCK_AMETHYST_BLOCK_HIT during vibration
                if (aTick % 12 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 1.1f + (float)Math.random() * 0.3f);
                }
            }
            // ============= PHASE 3: DISSIPATE — shatter & scatter (final 30t) =============
            else if (!shattered) {
                shattered = true;
                Location prismLoc = c.clone().add(0, PRISM_Y, 0);

                // Hide prism
                prism.animateTo(
                        new Vector3f(0, (float)PRISM_Y, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.001f), 2);

                // BLOCK_GLASS_BREAK x6 + ENTITY_GENERIC_EXPLODE on shatter
                for (int i = 0; i < 6; i++) {
                    DisplayBuilder.playSound(prismLoc, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.9f + (float)Math.random() * 0.5f);
                }
                DisplayBuilder.playSound(prismLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.1f);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, prismLoc, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.FLASH, prismLoc, 1, 0, 0, 0, 0);

                // Launch all 20 fragments — animateTo random position with rotation chaos
                for (int i = 0; i < fragments.size(); i++) {
                    fragments.get(i).animateTo(
                            new Vector3f((float)fLandX[i] - 0.25f, 0.4f, (float)fLandZ[i] - 0.25f),
                            new AxisAngle4f((float)(Math.random() * Math.PI * 2),
                                    (float)Math.random(), (float)Math.random(), (float)Math.random()),
                            new Vector3f(0.5f), 18);
                    // Trail particle burst at takeoff
                    w.spawnParticle(Particle.END_ROD, prismLoc, 4, 0.4, 0.4, 0.4, 0.3);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, prismLoc, 4, 0.4, 0.4, 0.4, 0.25);
                }
            } else {
                int dTick = tick - shatterTick;
                // Fragments in transit — trail particles
                if (dTick % 2 == 0) {
                    int[] col = RAINBOW[(dTick / 3) % RAINBOW.length];
                    for (int i = 0; i < fragments.size(); i++) {
                        if (fImpacted[i]) continue;
                        // Interpolated current position estimate
                        double t = Math.min(1.0, dTick / 18.0);
                        double cx = fLandX[i] * t;
                        double cz = fLandZ[i] * t;
                        double cy = PRISM_Y + (0.4 - PRISM_Y) * t;
                        Location at = c.clone().add(cx, cy, cz);
                        w.spawnParticle(Particle.END_ROD, at, 1, 0.1, 0.1, 0.1, 0.03);
                        DisplayBuilder.dustParticles(at, 1, 0.1, col[0], col[1], col[2], 1.2f);
                    }
                }
                // At ~tick 18 each fragment "lands" — small ELECTRIC_SPARK + DUST burst
                if (dTick == 19) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.2f);
                    int[] col = RAINBOW[0];
                    for (int i = 0; i < fragments.size(); i++) {
                        Location impact = c.clone().add(fLandX[i], 0.4, fLandZ[i]);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 6, 0.3, 0.3, 0.3, 0.15);
                        w.spawnParticle(Particle.END_ROD, impact, 4, 0.3, 0.3, 0.3, 0.1);
                        col = RAINBOW[i % RAINBOW.length];
                        DisplayBuilder.dustParticles(impact, 4, 0.3, col[0], col[1], col[2], 1.4f);
                        triggerImpactDamage(impact);
                        fImpacted[i] = true;
                    }
                }
                // Final BLOCK_GLASS_BREAK cascade
                if (dTick == 24 || dTick == 28) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.0f + (float)Math.random() * 0.4f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SkyShatteringPrism(plugin); }
    }
}
