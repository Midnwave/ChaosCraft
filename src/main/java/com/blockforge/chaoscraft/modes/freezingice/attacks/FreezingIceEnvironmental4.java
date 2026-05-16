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
 * FreezingIce Mode — ENVIRONMENTAL FX BATCH 4 (entries 31-40).
 * Aerial Spectacle + Sound theme — sound-pattern playback, sky-spanning
 * vertical elements, surreal echoes that evoke unease.
 *
 * Pure ItemDisplay + particle attacks. NO BlockDisplays.
 *
 * 31. CelestialIcicleCanopy  — 30 PACKED_ICE canopy descends, drops at once
 * 32. IceCrackingShockwave   — 24 ICE expanding ring r=1->12
 * 33. GlassShatterResonance  — 12 GLASS_BOTTLE vibrate then explode
 * 34. ResonantFrostNote      — 1 ECHO_SHARD emits concentric ring pulses
 * 35. SubsonicHum            — 6 NAUTILUS_SHELL hexagon resonators
 * 36. FrozenPlayerEcho       — 4 humanoid GRAY_STAINED_GLASS silhouettes thaw
 * 37. TimeStoppedObject      — 8 mixed items frozen mid-fall then released
 * 38. FrostMirage            — 12 GLASS_BOTTLE humanoid teleports each 30t
 * 39. InvertedSnowfall       — 60 SNOWBALL rise from ground
 * 40. CrystallineDouble      — 16 BLUE_ICE counter-rotating rings
 */
public final class FreezingIceEnvironmental4 {
    private FreezingIceEnvironmental4() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CelestialIcicleCanopy(plugin));
        registry.register(new IceCrackingShockwave(plugin));
        registry.register(new GlassShatterResonance(plugin));
        registry.register(new ResonantFrostNote(plugin));
        registry.register(new SubsonicHum(plugin));
        registry.register(new FrozenPlayerEcho(plugin));
        registry.register(new TimeStoppedObject(plugin));
        registry.register(new FrostMirage(plugin));
        registry.register(new InvertedSnowfall(plugin));
        registry.register(new CrystallineDouble(plugin));
    }

    // ================================================================
    // 31. CELESTIAL ICICLE CANOPY — 30 PACKED_ICE ItemDisplays form a
    //     dome canopy at Y+12 that descends slowly (60t), then drops
    //     all at once. Impact radius 10.0, 4400hp.
    // ================================================================
    public static class CelestialIcicleCanopy extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> icicles = new ArrayList<>();
        private final double[] iX = new double[30];
        private final double[] iZ = new double[30];
        private final double[] iY = new double[30];
        private boolean dropped = false;
        private static final int DESCEND_END = 60;
        private static final int DROP_TICK = 80;

        public CelestialIcicleCanopy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_icicle_canopy", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(52800.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(52800.0);
            config.setImpactRadius(15.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.6f);

            // Dome layout — 30 icicles arranged in 3 rings: 12 outer (r=8), 12 mid (r=5), 6 inner (r=2)
            int idx = 0;
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                iX[idx] = Math.cos(a) * 8.0;
                iZ[idx] = Math.sin(a) * 8.0;
                iY[idx] = 12.0;
                Location p = c.clone().add(iX[idx], iY[idx], iZ[idx]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.7f, 1.6f, 0.7f).glow(200, 240, 255).interpolation(20, 0);
                icicles.add(h);
                spawnedEntities.add(h.entity());
                idx++;
            }
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12 + Math.PI / 12;
                iX[idx] = Math.cos(a) * 5.0;
                iZ[idx] = Math.sin(a) * 5.0;
                iY[idx] = 13.5;
                Location p = c.clone().add(iX[idx], iY[idx], iZ[idx]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.65f, 1.5f, 0.65f).glow(210, 245, 255).interpolation(20, 0);
                icicles.add(h);
                spawnedEntities.add(h.entity());
                idx++;
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                iX[idx] = Math.cos(a) * 2.0;
                iZ[idx] = Math.sin(a) * 2.0;
                iY[idx] = 14.5;
                Location p = c.clone().add(iX[idx], iY[idx], iZ[idx]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.7f, 1.7f, 0.7f).glow(220, 250, 255).interpolation(20, 0);
                icicles.add(h);
                spawnedEntities.add(h.entity());
                idx++;
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Phase 1: 0-60 descend slowly to about Y+7
            // Phase 2: 60-80 hover threateningly with chime building
            // Phase 3: 80 drop all at once, impact
            if (!dropped && tick <= DESCEND_END) {
                double t = tick / (double)DESCEND_END;
                for (int i = 0; i < icicles.size(); i++) {
                    double targetY = iY[i] - 5.0; // descend 5 blocks
                    double curY = iY[i] + (targetY - iY[i]) * t;
                    icicles.get(i).animateTo(
                            new Vector3f((float)iX[i] - 0.35f, (float)curY, (float)iZ[i] - 0.35f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 1.6f, 0.7f), 4);
                }
                // Continuous SNOWFLAKE
                if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 9.0;
                    double yy = 4 + Math.random() * 10;
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
                // Chime builds in pitch
                if (tick % 12 == 0) {
                    float pitch = 0.7f + (float)t * 0.8f;
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, pitch);
                }
            } else if (!dropped && tick < DROP_TICK) {
                // Hover phase — chimes intensify
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
                }
                if (tick % 2 == 0) for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 9.0;
                    double yy = 5 + Math.random() * 4;
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
            } else if (!dropped) {
                dropped = true;
                // Drop all icicles to ground at once
                for (int i = 0; i < icicles.size(); i++) {
                    icicles.get(i).animateTo(
                            new Vector3f((float)iX[i] - 0.35f, 0.5f, (float)iZ[i] - 0.35f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 1.6f, 0.7f), 4);
                }
                Location impact = c.clone();
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.8f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.2f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.9f);
                // ITEM_SNOWBALL burst across canopy footprint
                for (int i = 0; i < icicles.size(); i++) {
                    Location ip = c.clone().add(iX[i], 0.4, iZ[i]);
                    w.spawnParticle(Particle.ITEM_SNOWBALL, ip, 18, 0.6, 0.4, 0.6, 0.2);
                    w.spawnParticle(Particle.SNOWFLAKE, ip, 12, 0.5, 0.3, 0.5, 0.05);
                    DisplayBuilder.dustParticles(ip, 12, 0.5, 200, 240, 255, 1.4f);
                }
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                triggerImpactDamage(impact);
            } else {
                // Settled — ice block sparkle
                if (tick % 6 == 0) for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 8;
                    w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r),
                            1, 0.05, 0.05, 0.05, 0);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CelestialIcicleCanopy(plugin); }
    }

    // ================================================================
    // 32. ICE CRACKING SHOCKWAVE — 24 ICE ItemDisplays in expanding ring
    //     r=1 -> r=12 over duration. Ring-edge damage 3000hp ticksBetween=10.
    //     FALLING_DUST(ICE) + ELECTRIC_SPARK at ring edge.
    // ================================================================
    public static class IceCrackingShockwave extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] sAng = new double[24];
        private double ringR = 1.0;

        public IceCrackingShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_cracking_shockwave", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(36000.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.4f, 0.8f);

            for (int i = 0; i < 24; i++) {
                sAng[i] = Math.PI * 2 * i / 24;
                Location p = c.clone().add(Math.cos(sAng[i]) * ringR, 0.4, Math.sin(sAng[i]) * ringR);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.55f, 0.4f, 0.55f).glow(180, 230, 255).interpolation(4, 0);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Expand r=1 -> r=12 over 200 ticks then stay at 12
            ringR = Math.min(12.0, 1.0 + tick * 0.055);

            // Position shards on the ring edge with slight rotation
            for (int i = 0; i < shards.size(); i++) {
                sAng[i] += 0.02;
                float bx = (float)(Math.cos(sAng[i]) * ringR);
                float bz = (float)(Math.sin(sAng[i]) * ringR);
                float by = 0.4f + (float)(Math.sin(tick * 0.1 + i) * 0.15);
                shards.get(i).animateTo(
                        new Vector3f(bx - 0.275f, by, bz - 0.275f),
                        new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                        new Vector3f(0.55f, 0.4f, 0.55f), 2);
            }

            // FALLING_DUST(ICE) + ELECTRIC_SPARK at ring edge
            if (tick % 1 == 0) {
                int sparkCount = 24;
                for (int i = 0; i < sparkCount; i++) {
                    double a = Math.PI * 2 * i / sparkCount + tick * 0.03;
                    Location p = c.clone().add(Math.cos(a) * ringR, 0.5, Math.sin(a) * ringR);
                    w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.1, 0.1, 0.1, 0, Material.ICE.createBlockData());
                    if (i % 3 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }

            // Expanding crack sound
            if (tick % 14 == 0) {
                float pitch = 0.6f + (float)(ringR / 12.0) * 0.8f;
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, pitch);
            }

            // Damage on the ring edge band
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double inner = Math.max(0, ringR - 1.2);
                double outer = ringR + 1.2;
                double inner2 = inner * inner;
                double outer2 = outer * outer;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double d2 = p.getLocation().distanceSquared(c);
                    if (d2 >= inner2 && d2 <= outer2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IceCrackingShockwave(plugin); }
    }

    // ================================================================
    // 33. GLASS SHATTER RESONANCE — 12 GLASS_BOTTLE ItemDisplays vibrate
    //     (scale pulse) for 60t while NOTE_BLOCK_HAT pitch rises, then
    //     explode. Impact radius 10.0, 5000hp.
    // ================================================================
    public static class GlassShatterResonance extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bottles = new ArrayList<>();
        private final double[] bAng = new double[12];
        private boolean exploded = false;
        private static final int CHARGE_END = 60;

        public GlassShatterResonance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glass_shatter_resonance", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(60000.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(160);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60000.0);
            config.setImpactRadius(15.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HAT, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.8f);

            for (int i = 0; i < 12; i++) {
                bAng[i] = Math.PI * 2 * i / 12;
                double r = 4.0;
                Location p = c.clone().add(Math.cos(bAng[i]) * r, 1.4, Math.sin(bAng[i]) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.7f, 0.7f, 0.7f).glow(200, 240, 255).interpolation(2, 0);
                bottles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!exploded && tick < CHARGE_END) {
                // Vibration intensifies — scale pulses and rapid jitter
                double t = tick / (double)CHARGE_END;
                double pulse = Math.sin(tick * (0.4 + t * 0.6)) * (0.1 + t * 0.2);
                float scale = (float)(0.7 + pulse);
                for (int i = 0; i < bottles.size(); i++) {
                    double r = 4.0 + Math.sin(tick * 0.5 + i) * 0.15;
                    float jitterX = (float)((Math.random() - 0.5) * t * 0.4);
                    float jitterZ = (float)((Math.random() - 0.5) * t * 0.4);
                    float bx = (float)(Math.cos(bAng[i]) * r) + jitterX;
                    float bz = (float)(Math.sin(bAng[i]) * r) + jitterZ;
                    bottles.get(i).animateTo(
                            new Vector3f(bx - 0.35f, 1.4f, bz - 0.35f),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                }

                // ELECTRIC_SPARK building
                if (tick % 2 == 0) for (int i = 0; i < bottles.size(); i++) {
                    Location p = c.clone().add(Math.cos(bAng[i]) * 4.0, 1.4, Math.sin(bAng[i]) * 4.0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1 + (int)(t * 3), 0.1, 0.1, 0.1, 0.05);
                }

                // Rising pitch hat — clearly building tension
                if (tick % 4 == 0) {
                    float pitch = 0.5f + (float)t * 1.5f;
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, pitch);
                }
            } else if (!exploded) {
                exploded = true;
                Location impact = c.clone();
                // END_ROD burst
                for (int i = 0; i < 60; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double rr = Math.random() * 10;
                    double yy = 0.4 + Math.random() * 4;
                    Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                    w.spawnParticle(Particle.END_ROD, p, 4, 0.3, 0.3, 0.3, 0.15);
                }
                // GLASS_BREAK barrage
                for (int i = 0; i < 8; i++) {
                    float pitch = 0.7f + (float)Math.random() * 1.0f;
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.6f, pitch);
                }
                // Per-bottle shatter VFX
                for (int i = 0; i < bottles.size(); i++) {
                    Location bp = c.clone().add(Math.cos(bAng[i]) * 4.0, 1.4, Math.sin(bAng[i]) * 4.0);
                    w.spawnParticle(Particle.EXPLOSION, bp, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.END_ROD, bp, 16, 0.4, 0.4, 0.4, 0.25);
                    DisplayBuilder.dustParticles(bp, 14, 0.4, 200, 240, 255, 1.4f);
                }
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                triggerImpactDamage(impact);
            } else {
                // Settled fog
                if (tick % 4 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 10;
                    w.spawnParticle(Particle.END_ROD, c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 2.5, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GlassShatterResonance(plugin); }
    }

    // ================================================================
    // 34. RESONANT FROST NOTE — 1 ECHO_SHARD core with strobing color
    //     cycle, expanding concentric rings during active phase, then
    //     spin-shrink dissipate with a final ring explosion.
    //     3 phases: spawn (0-30t) -> active (30 -> dur-40) -> dissipate (last 40t)
    // ================================================================
    public static class ResonantFrostNote extends EnvironmentalAttack {
        private ItemDisplayHandle core;
        // strobe color triplet: cyan / purple / white
        private static final int[][] STROBE = {
                {80, 220, 255},   // cyan
                {180, 80, 240},   // purple
                {245, 245, 255}   // white
        };

        public ResonantFrostNote(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("resonant_frost_note", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(43200.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(320);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.6f, 1.3f);

            // Spawn at scale 0 — will grow during spawn phase
            core = displayBuilder.spawnItem(c.clone().add(0, 1.8, 0), new ItemStack(Material.ECHO_SHARD));
            core.scale(0.01f, 0.01f, 0.01f).glow(80, 220, 255).interpolation(6, 0);
            spawnedEntities.add(core.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (core == null) return;

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;

            // ----- PHASE 1: SPAWN (0-30t) -----
            if (tick < 30) {
                double t = tick / 30.0;
                float scale = (float)(0.01 + t * 1.39); // grows 0 -> 1.4
                float by = 1.8f + (float)Math.sin(tick * 0.15) * 0.1f;
                core.animateTo(
                        new Vector3f(-0.7f, by, -0.7f),
                        new AxisAngle4f((float)(tick * 0.1), 0, 1, 0),
                        new Vector3f(scale), 4);
                // SCULK_SOUL shimmer during materialization
                if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 2.0;
                    w.spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(Math.cos(a) * r, 1.4 + Math.random() * 0.9, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.02);
                }
                // pitch ladder ascending
                if (tick % 5 == 0) {
                    float pitch = 0.6f + (float)t * 1.2f;
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, pitch);
                }
                if (tick == 0) DisplayBuilder.playSound(c, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 1.5f);
            }
            // ----- PHASE 2: ACTIVE (30 -> dur-40) -----
            else if (tick < dissipateStart) {
                int activeTick = tick - 30;
                // Strobe color every 15t — cycle cyan/purple/white
                if (activeTick % 15 == 0) {
                    int idx = (activeTick / 15) % STROBE.length;
                    int[] col = STROBE[idx];
                    core.glow(col[0], col[1], col[2]);
                }
                // Continuous Y-axis rotation 0.06 rad/tick + bob
                float by = 1.8f + (float)Math.sin(tick * 0.08) * 0.25f;
                core.animateTo(
                        new Vector3f(-0.7f, by, -0.7f),
                        new AxisAngle4f((float)(activeTick * 0.06), 0, 1, 0),
                        new Vector3f(1.4f), 2);

                // Concentric ring pulses — every 20t spawn a ring expanding r=1..9 over 60t
                if (activeTick % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.3f, 0.6f);
                    final Location cFinal = c.clone();
                    for (int step = 0; step <= 8; step++) {
                        final int stepF = step;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (cFinal.getWorld() == null) return;
                            double rr = 1.0 + stepF; // expand 1 -> 9
                            int pts = 16 + stepF * 3;
                            for (int i = 0; i < pts; i++) {
                                double a = Math.PI * 2 * i / pts;
                                Location p = cFinal.clone().add(Math.cos(a) * rr, 1.0, Math.sin(a) * rr);
                                // 3 layered particle types: NOTE / CRIT / BUBBLE_POP
                                if (i % 3 == 0) cFinal.getWorld().spawnParticle(Particle.NOTE, p, 1, 0.05, 0.05, 0.05, 0.5);
                                else if (i % 3 == 1) cFinal.getWorld().spawnParticle(Particle.CRIT, p, 1, 0.1, 0.1, 0.1, 0.02);
                                else cFinal.getWorld().spawnParticle(Particle.BUBBLE_POP, p, 1, 0.05, 0.05, 0.05, 0);
                            }
                        }, stepF * 7L);
                    }
                }

                // Constant SCULK_SOUL halo around core
                if (tick % 3 == 0) for (int i = 0; i < 2; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.5;
                    w.spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(Math.cos(a) * r, 1.5 + Math.random() * 1.0, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // ----- PHASE 3: DISSIPATE (last 40t) -----
            else {
                int dt = tick - dissipateStart;
                double t = dt / 40.0;
                float scale = (float)Math.max(0.01, 1.4 * (1.0 - t)); // 1.4 -> 0
                float by = 1.8f + (float)Math.sin(tick * 0.08) * 0.25f;
                // accelerate spin: 0.5 rad/tick at end
                core.animateTo(
                        new Vector3f(-scale * 0.5f, by, -scale * 0.5f),
                        new AxisAngle4f((float)(dt * 0.5), 0, 1, 0),
                        new Vector3f(scale), 2);
                if (dt == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.6f, 0.8f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.3f, 1.6f);
                    // Final ring explosion at max radius — CRIT + END_ROD burst
                    for (int i = 0; i < 36; i++) {
                        double a = Math.PI * 2 * i / 36;
                        Location p = c.clone().add(Math.cos(a) * 9.0, 1.0, Math.sin(a) * 9.0);
                        w.spawnParticle(Particle.CRIT, p, 4, 0.3, 0.3, 0.3, 0.2);
                        w.spawnParticle(Particle.END_ROD, p, 3, 0.2, 0.2, 0.2, 0.15);
                    }
                }
                // shimmer fading out
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(0, 1.6, 0), 1, 0.4, 0.6, 0.4, 0.03);
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

        @Override public AbstractAttack newInstance() { return new ResonantFrostNote(plugin); }
    }

    // ================================================================
    // 35. SUBSONIC HUM — 6 NAUTILUS_SHELL hexagon, 3 active sub-phases:
    //     quiet idle -> buildup -> climax (all 6 rings overlap +
    //     central heartbeat thump). Final dissipate shrink with ambient fade.
    // ================================================================
    public static class SubsonicHum extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shells = new ArrayList<>();
        private final double[] sAng = new double[6];
        private static final int SPAWN_END = 60;
        // active sub-phases:
        // 60-150 quiet idle (90t)
        // 150-200 buildup (50t)
        // 200-220 climax (20t)
        private static final int QUIET_END = 150;
        private static final int BUILDUP_END = 200;
        private static final int CLIMAX_END = 220;
        private boolean climaxFired = false;

        public SubsonicHum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subsonic_hum", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(33600.0);
            config.setDamageRadius(16.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(400);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);

            // Hexagon — staggered scale 0 -> 0.8 over 60t (sub-spawned with delays)
            for (int i = 0; i < 6; i++) {
                sAng[i] = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(sAng[i]) * 5.5, 0.5, Math.sin(sAng[i]) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAUTILUS_SHELL));
                // start invisible (scale 0) — grow during spawn phase
                h.scale(0.01f, 0.01f, 0.01f).glow(50, 120, 200).interpolation(10, 0);
                shells.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;

            // ----- PHASE 1: SPAWN (0-60t) — staggered materialize -----
            if (tick < SPAWN_END) {
                for (int i = 0; i < shells.size(); i++) {
                    int startAt = i * 6;   // stagger each shell by 6t
                    int local = tick - startAt;
                    if (local < 0) continue;
                    double t = Math.min(1.0, local / 30.0);
                    float scale = (float)(0.01 + t * 0.79); // 0 -> 0.8
                    float bx = (float)(Math.cos(sAng[i]) * 5.5);
                    float bz = (float)(Math.sin(sAng[i]) * 5.5);
                    shells.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, 0.5f, bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.03 + i), 0, 1, 0),
                            new Vector3f(scale), 4);
                }
                if (tick % 4 == 0) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.5;
                    w.spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 1.0, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
                if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.6f, 0.5f);
            }
            // ----- PHASE 2A: QUIET IDLE (60-150) -----
            else if (tick < QUIET_END) {
                // gentle pulse ±0.05
                float pulse = (float)(0.8 + Math.sin(tick * 0.08) * 0.05);
                for (int i = 0; i < shells.size(); i++) {
                    float bx = (float)(Math.cos(sAng[i]) * 5.5);
                    float bz = (float)(Math.sin(sAng[i]) * 5.5);
                    shells.get(i).animateTo(
                            new Vector3f(bx - pulse * 0.5f, 0.5f, bz - pulse * 0.5f),
                            new AxisAngle4f((float)(tick * 0.03 + i), 0, 1, 0),
                            new Vector3f(pulse), 4);
                }
                // soft SCULK_SOUL between shells
                if (tick % 4 == 0) for (int i = 0; i < 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.5;
                    w.spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
                // slow heartbeat
                if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.4f, 0.5f);
            }
            // ----- PHASE 2B: BUILDUP (150-200) -----
            else if (tick < BUILDUP_END) {
                int bt = tick - QUIET_END;
                double t = bt / 50.0;
                float pulse = (float)(0.8 + Math.sin(tick * (0.15 + t * 0.2)) * (0.1 + t * 0.1));
                for (int i = 0; i < shells.size(); i++) {
                    float bx = (float)(Math.cos(sAng[i]) * 5.5);
                    float bz = (float)(Math.sin(sAng[i]) * 5.5);
                    // intense glow ramp
                    if (bt % 10 == 0) {
                        int gb = (int)(120 + t * 80);
                        shells.get(i).glow(50 + (int)(t * 50), gb, 200 + (int)(t * 55));
                    }
                    shells.get(i).animateTo(
                            new Vector3f(bx - pulse * 0.5f, 0.5f, bz - pulse * 0.5f),
                            new AxisAngle4f((float)(tick * 0.08 + i), 0, 1, 0),
                            new Vector3f(pulse), 2);
                }
                // each shell emits its own small expanding ring (SCULK)
                int ringInterval = (int)Math.max(8, 30 - t * 22); // 30 -> 8t
                if (bt % ringInterval == 0) {
                    for (int i = 0; i < shells.size(); i++) {
                        final double cx = Math.cos(sAng[i]) * 5.5;
                        final double cz = Math.sin(sAng[i]) * 5.5;
                        for (int step = 1; step <= 3; step++) {
                            final int sF = step;
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                if (c.getWorld() == null) return;
                                for (int j = 0; j < 10; j++) {
                                    double a = Math.PI * 2 * j / 10;
                                    Location p = c.clone().add(cx + Math.cos(a) * sF * 0.8, 0.6, cz + Math.sin(a) * sF * 0.8);
                                    c.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, p, 1, 0.05, 0.05, 0.05, 0);
                                }
                            }, sF * 3L);
                        }
                    }
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.3f, 0.6f + (float)t * 0.4f);
                }
                // ELECTRIC_SPARK accent between shells
                if (bt % 6 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.5;
                    w.spawnParticle(Particle.ELECTRIC_SPARK,
                            c.clone().add(Math.cos(a) * r, 0.4 + Math.random() * 0.6, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.05);
                }
            }
            // ----- PHASE 2C: CLIMAX (200-220) — all rings fire at once + thump -----
            else if (tick < CLIMAX_END) {
                if (!climaxFired) {
                    climaxFired = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.6f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 1.0f);
                    // central WARDEN heartbeat thump — ELECTRIC_SPARK explosion at center
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                    for (int i = 0; i < 60; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 3.0;
                        Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 1.0, Math.sin(a) * r);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.2, 0.2, 0.2, 0.3);
                    }
                    // All 6 rings fire simultaneously — expanding overlap
                    for (int i = 0; i < shells.size(); i++) {
                        final double cx = Math.cos(sAng[i]) * 5.5;
                        final double cz = Math.sin(sAng[i]) * 5.5;
                        for (int step = 1; step <= 6; step++) {
                            final int sF = step;
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                if (c.getWorld() == null) return;
                                for (int j = 0; j < 14; j++) {
                                    double a = Math.PI * 2 * j / 14;
                                    Location p = c.clone().add(cx + Math.cos(a) * sF * 1.1, 0.6, cz + Math.sin(a) * sF * 1.1);
                                    c.getWorld().spawnParticle(Particle.SONIC_BOOM, p, 1, 0, 0, 0, 0);
                                    c.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, p, 1, 0.1, 0.1, 0.1, 0);
                                }
                            }, sF * 2L);
                        }
                    }
                }
                // shells spike in scale at climax
                float bigPulse = (float)(1.0 + Math.sin(tick * 0.4) * 0.2);
                for (int i = 0; i < shells.size(); i++) {
                    float bx = (float)(Math.cos(sAng[i]) * 5.5);
                    float bz = (float)(Math.sin(sAng[i]) * 5.5);
                    shells.get(i).animateTo(
                            new Vector3f(bx - bigPulse * 0.5f, 0.5f, bz - bigPulse * 0.5f),
                            new AxisAngle4f((float)(tick * 0.2 + i), 0, 1, 0),
                            new Vector3f(bigPulse), 1);
                }
            }
            // ----- AFTERMATH (220 -> dissipateStart) -----
            else if (tick < dissipateStart) {
                // shells settle, residual hum
                float idle = (float)(0.8 + Math.sin(tick * 0.05) * 0.05);
                for (int i = 0; i < shells.size(); i++) {
                    float bx = (float)(Math.cos(sAng[i]) * 5.5);
                    float bz = (float)(Math.sin(sAng[i]) * 5.5);
                    shells.get(i).animateTo(
                            new Vector3f(bx - idle * 0.5f, 0.5f, bz - idle * 0.5f),
                            new AxisAngle4f((float)(tick * 0.02 + i), 0, 1, 0),
                            new Vector3f(idle), 4);
                }
                if (tick % 6 == 0) for (int i = 0; i < 2; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.5;
                    w.spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
                if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.6f, 0.5f);
            }
            // ----- PHASE 3: DISSIPATE (last 40t) -----
            else {
                int dt = tick - dissipateStart;
                double t = dt / 40.0;
                float scale = (float)Math.max(0.01, 0.8 * (1.0 - t)); // 0.8 -> 0
                for (int i = 0; i < shells.size(); i++) {
                    float bx = (float)(Math.cos(sAng[i]) * 5.5);
                    float bz = (float)(Math.sin(sAng[i]) * 5.5);
                    shells.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, 0.5f, bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                }
                if (dt == 0) {
                    DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.0f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SHRIEKER_FALL, 1.0f, 0.5f);
                }
                // smoke rising fade
                if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5.5;
                    w.spawnParticle(Particle.LARGE_SMOKE,
                            c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 2.0, Math.sin(a) * r),
                            1, 0.05, 0.1, 0.05, 0.02);
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

        @Override public AbstractAttack newInstance() { return new SubsonicHum(plugin); }
    }

    // ================================================================
    // 36. FROZEN PLAYER ECHO — 4 ghost-player silhouettes (humanoid form
    //     of 8 GRAY_STAINED_GLASS each = 32 total ItemDisplays in 4
    //     clusters) fade in over 40t then thaw (impact). Impact radius
    //     6.0, 4000hp each.
    // ================================================================
    public static class FrozenPlayerEcho extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> echoes = new ArrayList<>();
        // 4 clusters, each silhouette has 8 parts -> 32 total
        private final double[] clusterX = new double[4];
        private final double[] clusterZ = new double[4];
        private final boolean[] thawed = new boolean[4];
        private static final int FADE_END = 40;
        private static final int THAW_TICK = 100;

        public FrozenPlayerEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_player_echo", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(48000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48000.0);
            config.setImpactRadius(9.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.PARTICLE_SOUL_ESCAPE, 1.2f, 0.7f);

            // 4 silhouettes arranged at cardinal directions, 7 blocks out
            for (int s = 0; s < 4; s++) {
                double a = Math.PI * 2 * s / 4 + Math.PI / 4;
                clusterX[s] = Math.cos(a) * 7.0;
                clusterZ[s] = Math.sin(a) * 7.0;

                // Humanoid form — 8 body parts (head, torso top, torso mid, torso low,
                // arm-L, arm-R, leg-L, leg-R)
                double bx = clusterX[s];
                double bz = clusterZ[s];

                spawnPart(c, bx, 1.95, bz, 0.45f, 0.45f, 0.45f);  // head
                spawnPart(c, bx, 1.55, bz, 0.5f,  0.4f,  0.3f);   // torso upper
                spawnPart(c, bx, 1.25, bz, 0.5f,  0.4f,  0.3f);   // torso mid
                spawnPart(c, bx, 0.95, bz, 0.5f,  0.35f, 0.3f);   // torso low
                spawnPart(c, bx - 0.4, 1.4, bz, 0.25f, 0.6f, 0.25f); // left arm
                spawnPart(c, bx + 0.4, 1.4, bz, 0.25f, 0.6f, 0.25f); // right arm
                spawnPart(c, bx - 0.15, 0.5, bz, 0.25f, 0.7f, 0.25f); // left leg
                spawnPart(c, bx + 0.15, 0.5, bz, 0.25f, 0.7f, 0.25f); // right leg
            }
        }

        private void spawnPart(Location c, double dx, double dy, double dz, float sx, float sy, float sz) {
            Location p = c.clone().add(dx, dy, dz);
            ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GRAY_STAINED_GLASS));
            h.scale(sx, sy, sz).glow(180, 200, 220).interpolation(20, 0);
            echoes.add(h);
            spawnedEntities.add(h.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < FADE_END) {
                // Fade-in via gradual scale interp (already 20-tick interp on spawn).
                // Continuous SCULK_SOUL + SOUL_FIRE_FLAME from each silhouette
                if (tick % 2 == 0) for (int s = 0; s < 4; s++) {
                    Location sp = c.clone().add(clusterX[s], 1.0, clusterZ[s]);
                    w.spawnParticle(Particle.SCULK_SOUL, sp, 2, 0.4, 0.8, 0.4, 0.02);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, sp, 1, 0.3, 0.7, 0.3, 0.01);
                }
                if (tick % 14 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.9f, 0.7f);
                }
            } else if (tick < THAW_TICK) {
                // Hover phase — silhouettes glow steadily, eerie sound
                if (tick % 3 == 0) for (int s = 0; s < 4; s++) {
                    if (thawed[s]) continue;
                    Location sp = c.clone().add(clusterX[s], 1.0, clusterZ[s]);
                    w.spawnParticle(Particle.SCULK_SOUL, sp, 1, 0.35, 0.7, 0.35, 0.01);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, sp, 1, 0.3, 0.6, 0.3, 0.01);
                }
                if (tick % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 1.0f, 0.6f);
                }
            } else {
                // Thaw phase — each silhouette pops independently every 8 ticks
                int idx = (tick - THAW_TICK) / 8;
                if (idx < 4 && !thawed[idx]) {
                    thawed[idx] = true;
                    Location impact = c.clone().add(clusterX[idx], 0.8, clusterZ[idx]);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.9f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.3f);
                    DisplayBuilder.playSound(impact, Sound.PARTICLE_SOUL_ESCAPE, 1.4f, 0.9f);
                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.SCULK_SOUL, impact, 24, 0.6, 0.9, 0.6, 0.1);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, impact, 18, 0.5, 0.8, 0.5, 0.1);
                    DisplayBuilder.dustParticles(impact, 20, 0.6, 180, 200, 220, 1.5f);
                    triggerImpactDamage(impact);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrozenPlayerEcho(plugin); }
    }

    // ================================================================
    // 37. TIME STOPPED OBJECT — 8 mixed items (sword, axe, apple, book,
    //     etc.) frozen mid-fall in air, then released. Impact each,
    //     radius 2.5, 3200hp.
    // ================================================================
    public static class TimeStoppedObject extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> items = new ArrayList<>();
        private final double[] iAng = new double[8];
        private final double[] iR = new double[8];
        private final double[] iY = new double[8];
        private final boolean[] iImpacted = new boolean[8];
        private static final int FREEZE_END = 60;

        public TimeStoppedObject(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("time_stopped_object", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(38400.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(200);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(38400.0);
            config.setImpactRadius(3.75);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 1.0f);

            Material[] mixed = {
                    Material.IRON_SWORD, Material.IRON_AXE, Material.APPLE, Material.BOOK,
                    Material.BREAD, Material.CLOCK, Material.ENDER_PEARL, Material.PAPER
            };
            for (int i = 0; i < 8; i++) {
                iAng[i] = Math.PI * 2 * i / 8 + (Math.random() - 0.5) * 0.3;
                iR[i] = 2.5 + Math.random() * 3.5;
                iY[i] = 4.5 + Math.random() * 3.0;
                Location p = c.clone().add(Math.cos(iAng[i]) * iR[i], iY[i], Math.sin(iAng[i]) * iR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mixed[i]));
                h.scale(0.85f, 0.85f, 0.85f).glow(180, 100, 220).interpolation(4, 0);
                items.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < FREEZE_END) {
                // Freeze phase — items lock in place, REVERSE_PORTAL aura
                for (int i = 0; i < items.size(); i++) {
                    if (iImpacted[i]) continue;
                    float bx = (float)(Math.cos(iAng[i]) * iR[i]);
                    float bz = (float)(Math.sin(iAng[i]) * iR[i]);
                    items.get(i).animateTo(
                            new Vector3f(bx - 0.425f, (float)iY[i], bz - 0.425f),
                            new AxisAngle4f((float)(tick * 0.02 + i), 1, 0.4f, 0),
                            new Vector3f(0.85f), 8);
                }
                if (tick % 2 == 0) for (int i = 0; i < items.size(); i++) {
                    Location p = c.clone().add(Math.cos(iAng[i]) * iR[i], iY[i], Math.sin(iAng[i]) * iR[i]);
                    w.spawnParticle(Particle.REVERSE_PORTAL, p, 2, 0.25, 0.25, 0.25, 0.05);
                    if (Math.random() < 0.3) w.spawnParticle(Particle.ENCHANT, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
                if (tick % 18 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
                }
            } else {
                // Release phase — items fall, FALLING_DUST trail
                for (int i = 0; i < items.size(); i++) {
                    if (iImpacted[i]) continue;
                    iY[i] -= 0.4;
                    if (iY[i] <= 0.5) {
                        Location impact = c.clone().add(Math.cos(iAng[i]) * iR[i], 0.4, Math.sin(iAng[i]) * iR[i]);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.4f, 1.0f + (float)Math.random() * 0.6f);
                        w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.FALLING_DUST, impact, 16, 0.4, 0.3, 0.4, 0.1, Material.AMETHYST_BLOCK.createBlockData());
                        DisplayBuilder.dustParticles(impact, 12, 0.4, 180, 100, 220, 1.4f);
                        triggerImpactDamage(impact);
                        iImpacted[i] = true;
                    } else {
                        Location pos = c.clone().add(Math.cos(iAng[i]) * iR[i], iY[i], Math.sin(iAng[i]) * iR[i]);
                        float bx = (float)(Math.cos(iAng[i]) * iR[i]);
                        float bz = (float)(Math.sin(iAng[i]) * iR[i]);
                        items.get(i).animateTo(
                                new Vector3f(bx - 0.425f, (float)iY[i], bz - 0.425f),
                                new AxisAngle4f((float)(tick * 0.3 + i), 1, 0.7f, 0),
                                new Vector3f(0.85f), 2);
                        w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.05, 0.05, 0.05, 0, Material.AMETHYST_BLOCK.createBlockData());
                        DisplayBuilder.dustParticles(pos, 1, 0.05, 180, 100, 220, 0.9f);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new TimeStoppedObject(plugin); }
    }

    // ================================================================
    // 38. FROST MIRAGE — 12 GLASS_BOTTLE humanoid silhouette cycling
    //     through 4 distinct sub-phases of 60t each:
    //       1) Stable (breathing pulse + halo)
    //       2) Pre-teleport (flicker)
    //       3) Teleport (END_ROD burst + reassemble at new spot)
    //       4) Stabilize (outline ELECTRIC_SPARK)
    //     Spawn (0-30t): materialize. Dissipate (last 40t): fade out.
    // ================================================================
    public static class FrostMirage extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> parts = new ArrayList<>();
        // Local body offsets relative to the mirage's current center
        private final double[] partDX = new double[12];
        private final double[] partDY = new double[12];
        private final double[] partDZ = new double[12];
        private final float[] partSX = new float[12];
        private final float[] partSY = new float[12];
        private final float[] partSZ = new float[12];
        private double mirageX = 0;
        private double mirageZ = 0;
        private static final int SPAWN_END = 30;
        private static final int SUB_PHASE_LEN = 60;
        private static final int CYCLE_LEN = SUB_PHASE_LEN * 4;
        private int lastVisibleTick = -1;

        public FrostMirage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_mirage", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(40800.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.2f);
            DisplayBuilder.playSound(c, Sound.PARTICLE_SOUL_ESCAPE, 0.9f, 1.4f);

            double[][] layout = {
                    { 0.0, 2.0, 0.0, 0.5, 0.5, 0.5 },
                    { 0.0, 1.55, 0.0, 0.55, 0.4, 0.35 },
                    { 0.0, 1.2, 0.0, 0.55, 0.4, 0.35 },
                    { 0.0, 0.85, 0.0, 0.55, 0.4, 0.35 },
                    { -0.4, 1.55, 0.0, 0.25, 0.45, 0.25 },
                    { 0.4, 1.55, 0.0, 0.25, 0.45, 0.25 },
                    { -0.4, 1.05, 0.0, 0.22, 0.5, 0.22 },
                    { 0.4, 1.05, 0.0, 0.22, 0.5, 0.22 },
                    { -0.18, 0.55, 0.0, 0.25, 0.55, 0.25 },
                    { 0.18, 0.55, 0.0, 0.25, 0.55, 0.25 },
                    { -0.18, 0.15, 0.0, 0.22, 0.4, 0.22 },
                    { 0.18, 0.15, 0.0, 0.22, 0.4, 0.22 }
            };
            for (int i = 0; i < 12; i++) {
                partDX[i] = layout[i][0];
                partDY[i] = layout[i][1];
                partDZ[i] = layout[i][2];
                partSX[i] = (float)layout[i][3];
                partSY[i] = (float)layout[i][4];
                partSZ[i] = (float)layout[i][5];
                Location p = c.clone().add(mirageX + partDX[i], partDY[i], mirageZ + partDZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                // start at scale 0 — grows during spawn phase
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 220, 255).interpolation(6, 0);
                parts.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        private void setPartScales(float mul, int interp) {
            for (int i = 0; i < parts.size(); i++) {
                float sx = partSX[i] * mul;
                float sy = partSY[i] * mul;
                float sz = partSZ[i] * mul;
                float bx = (float)(mirageX + partDX[i]);
                float bz = (float)(mirageZ + partDZ[i]);
                parts.get(i).animateTo(
                        new Vector3f(bx - sx / 2.0f, (float)partDY[i], bz - sz / 2.0f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), interp);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;

            // ----- PHASE 1: SPAWN (0-30t) -----
            if (tick < SPAWN_END) {
                double t = tick / (double)SPAWN_END;
                float mul = (float)(0.01 + t * 0.59); // -> 0.6
                setPartScales(mul, 4);
                if (tick % 2 == 0) for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.5;
                    w.spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(mirageX + Math.cos(a) * r, 0.5 + Math.random() * 1.8, mirageZ + Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.02);
                }
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.7f, 1.0f + (float)t * 0.4f);
            }
            // ----- PHASE 3: DISSIPATE (last 40t) -----
            else if (tick >= dissipateStart) {
                int dt = tick - dissipateStart;
                double t = dt / 40.0;
                float mul = (float)Math.max(0.01, 0.6 * (1.0 - t));
                setPartScales(mul, 2);
                if (dt == 0) {
                    DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.2f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_DEATH, 1.1f, 1.0f);
                }
                if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.5;
                    w.spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(mirageX + Math.cos(a) * r, 0.5 + Math.random() * 1.8, mirageZ + Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.02);
                }
                if (dt == 35) {
                    // final SCULK_SOUL pulse
                    Location pulse = c.clone().add(mirageX, 1.2, mirageZ);
                    w.spawnParticle(Particle.SCULK_SOUL, pulse, 24, 0.6, 0.8, 0.6, 0.05);
                }
            }
            // ----- PHASE 2: ACTIVE (cycling sub-phases) -----
            else {
                int activeTick = tick - SPAWN_END;
                int cyclePos = activeTick % CYCLE_LEN;
                int subPhase = cyclePos / SUB_PHASE_LEN;       // 0..3
                int subTick = cyclePos % SUB_PHASE_LEN;        // 0..59

                switch (subPhase) {
                    case 0: { // STABLE — breathing pulse + SCULK_SOUL halo
                        float breath = (float)(0.6 + Math.sin(subTick * 0.12) * 0.04);
                        for (int i = 0; i < parts.size(); i++) {
                            float sx = partSX[i] * breath;
                            float sy = partSY[i] * breath;
                            float sz = partSZ[i] * breath;
                            float bx = (float)(mirageX + partDX[i]);
                            float bz = (float)(mirageZ + partDZ[i]);
                            parts.get(i).animateTo(
                                    new Vector3f(bx - sx / 2.0f, (float)partDY[i], bz - sz / 2.0f),
                                    new AxisAngle4f((float)(activeTick * 0.02), 0, 1, 0),
                                    new Vector3f(sx, sy, sz), 4);
                        }
                        if (subTick % 3 == 0) for (int i = 0; i < 3; i++) {
                            double a = Math.random() * Math.PI * 2;
                            double r = 1.0 + Math.random() * 0.6;
                            w.spawnParticle(Particle.SCULK_SOUL,
                                    c.clone().add(mirageX + Math.cos(a) * r, 1.0 + Math.random() * 1.2, mirageZ + Math.sin(a) * r),
                                    1, 0.1, 0.1, 0.1, 0.02);
                        }
                        if (subTick % 25 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.9f, 0.8f);
                        break;
                    }
                    case 1: { // PRE-TELEPORT — last 10t flicker; first 50t glow up
                        if (subTick < SUB_PHASE_LEN - 10) {
                            // steady glow build
                            float breath = (float)(0.6 + Math.sin(subTick * 0.15) * 0.05);
                            setPartScales(breath, 2);
                            if (subTick % 4 == 0) for (int i = 0; i < parts.size(); i += 2) {
                                Location p = c.clone().add(mirageX + partDX[i], partDY[i], mirageZ + partDZ[i]);
                                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                            }
                        } else {
                            // RAPID FLICKER — alternate visible/invisible every 2t
                            int flickerTick = subTick - (SUB_PHASE_LEN - 10);
                            boolean visible = (flickerTick / 2) % 2 == 0;
                            float mul = visible ? 0.6f : 0.01f;
                            setPartScales(mul, 1);
                            if (flickerTick % 2 == 0) {
                                for (int i = 0; i < parts.size(); i++) {
                                    Location p = c.clone().add(mirageX + partDX[i], partDY[i], mirageZ + partDZ[i]);
                                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.15, 0.15, 0.15, 0.1);
                                }
                            }
                            if (flickerTick == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.6f);
                        }
                        break;
                    }
                    case 2: { // TELEPORT — first tick: burst at old spot + pick new spot + reassemble
                        if (subTick == 0) {
                            // END_ROD burst at OLD position
                            Location oldPos = c.clone().add(mirageX, 1.0, mirageZ);
                            for (int i = 0; i < 30; i++) {
                                double a = Math.random() * Math.PI * 2;
                                double r = Math.random() * 1.5;
                                Location p = oldPos.clone().add(Math.cos(a) * r, Math.random() * 1.8 - 0.5, Math.sin(a) * r);
                                w.spawnParticle(Particle.END_ROD, p, 2, 0.2, 0.2, 0.2, 0.15);
                                w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.05);
                            }
                            DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.5f);
                            DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 1.4f, 1.2f);
                            // pick new arena point
                            double a = Math.random() * Math.PI * 2;
                            double r = 2.0 + Math.random() * 4.0;
                            mirageX = Math.cos(a) * r;
                            mirageZ = Math.sin(a) * r;
                            // reassemble — each part animateTo new position with 5t interpolation
                            for (int i = 0; i < parts.size(); i++) {
                                float bx = (float)(mirageX + partDX[i]);
                                float bz = (float)(mirageZ + partDZ[i]);
                                parts.get(i).animateTo(
                                        new Vector3f(bx - partSX[i] * 0.6f / 2.0f, (float)partDY[i], bz - partSZ[i] * 0.6f / 2.0f),
                                        new AxisAngle4f(0, 0, 1, 0),
                                        new Vector3f(partSX[i] * 0.6f, partSY[i] * 0.6f, partSZ[i] * 0.6f), 5);
                            }
                        } else {
                            // hold position, shimmer at new spot
                            if (subTick % 2 == 0) for (int i = 0; i < 4; i++) {
                                double a = Math.random() * Math.PI * 2;
                                double r = Math.random() * 1.5;
                                w.spawnParticle(Particle.END_ROD,
                                        c.clone().add(mirageX + Math.cos(a) * r, 0.5 + Math.random() * 1.8, mirageZ + Math.sin(a) * r),
                                        1, 0.1, 0.1, 0.1, 0.02);
                            }
                        }
                        break;
                    }
                    case 3: { // STABILIZE — outline glow with ELECTRIC_SPARK every 4t
                        float breath = (float)(0.6 + Math.sin(subTick * 0.1) * 0.03);
                        for (int i = 0; i < parts.size(); i++) {
                            float sx = partSX[i] * breath;
                            float sy = partSY[i] * breath;
                            float sz = partSZ[i] * breath;
                            float bx = (float)(mirageX + partDX[i]);
                            float bz = (float)(mirageZ + partDZ[i]);
                            parts.get(i).animateTo(
                                    new Vector3f(bx - sx / 2.0f, (float)partDY[i], bz - sz / 2.0f),
                                    new AxisAngle4f((float)(activeTick * 0.03), 0, 1, 0),
                                    new Vector3f(sx, sy, sz), 3);
                        }
                        if (subTick % 4 == 0) for (int i = 0; i < parts.size(); i++) {
                            Location p = c.clone().add(mirageX + partDX[i], partDY[i], mirageZ + partDZ[i]);
                            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                        }
                        if (subTick == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
                        break;
                    }
                }
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                Location dmgCenter = c.clone().add(mirageX, 0, mirageZ);
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(dmgCenter) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostMirage(plugin); }
    }

    // ================================================================
    // 39. INVERTED SNOWFALL — 60 SNOWBALL ItemDisplays.
    //     Spawn (0-40t): spiral pattern at ground, scale 0 -> 0.4
    //     Active phases:
    //       Gather (40-60t): converge to tight cluster at center
    //       Rise  (60-180t): helical ascent +Y8 over 120t, layered particles
    //       Convergence (180-220t): converge to single point Y+10
    //     Dissipate (last 20t): radial burst outward, scale -> 0
    // ================================================================
    public static class InvertedSnowfall extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flakes = new ArrayList<>();
        // initial spiral spawn parameters
        private final double[] spAng = new double[60];
        private final double[] spR = new double[60];
        // burst directions (set at dissipate)
        private final double[] burstAng = new double[60];
        private final double[] burstR = new double[60];
        private static final int SPAWN_END = 40;
        private static final int GATHER_END = 60;
        private static final int RISE_END = 180;
        private static final int CONVERGE_END = 220;

        public InvertedSnowfall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inverted_snowfall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(28800.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(380);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_PLACE, 1.0f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.3f, 0.6f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.6f, 0.5f);

            // spiral pattern — angle grows with index (Archimedean-ish)
            for (int i = 0; i < 60; i++) {
                spAng[i] = i * 0.5 + Math.random() * 0.1;
                spR[i] = 0.5 + (i / 60.0) * 6.5; // 0.5 -> 7
                Location p = c.clone().add(Math.cos(spAng[i]) * spR[i], 0.3, Math.sin(spAng[i]) * spR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.01f, 0.01f, 0.01f).glow(230, 245, 255).interpolation(4, 0);
                flakes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 20;

            // ----- PHASE 1: SPAWN (0-40t) — spiral materialize -----
            if (tick < SPAWN_END) {
                double t = tick / (double)SPAWN_END;
                float scale = (float)(0.01 + t * 0.39); // -> 0.4
                for (int i = 0; i < flakes.size(); i++) {
                    float bx = (float)(Math.cos(spAng[i]) * spR[i]);
                    float bz = (float)(Math.sin(spAng[i]) * spR[i]);
                    flakes.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, 0.3f, bz - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(scale), 3);
                }
                if (tick % 2 == 0) for (int i = 0; i < 5; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 7.0;
                    w.spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 0.4, Math.sin(a) * r),
                            1, 0.1, 0.05, 0.1, 0.01);
                }
                if (tick % 10 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_PLACE, 0.5f, 0.4f + (float)t * 0.4f);
            }
            // ----- PHASE 2A: GATHER (40-60t) — converge to tighter cluster at center -----
            else if (tick < GATHER_END) {
                int gt = tick - SPAWN_END;
                double t = gt / 20.0;
                // shrink radius from initial spiral down to 1.0
                for (int i = 0; i < flakes.size(); i++) {
                    double targetR = 1.0;
                    double curR = spR[i] + (targetR - spR[i]) * t;
                    float bx = (float)(Math.cos(spAng[i]) * curR);
                    float bz = (float)(Math.sin(spAng[i]) * curR);
                    flakes.get(i).animateTo(
                            new Vector3f(bx - 0.2f, 0.4f + (float)t * 0.4f, bz - 0.2f),
                            new AxisAngle4f((float)(tick * 0.15 + i), 0, 1, 0),
                            new Vector3f(0.4f), 2);
                }
                if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 3.0;
                    w.spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r),
                            1, 0.1, 0.05, 0.1, 0.03);
                }
                if (gt == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.0f, 1.2f);
            }
            // ----- PHASE 2B: RISE (60-180t) — helical ascent -----
            else if (tick < RISE_END) {
                int rt = tick - GATHER_END;
                double t = rt / 120.0;
                double yBase = 0.8 + t * 8.0; // 0.8 -> 8.8
                for (int i = 0; i < flakes.size(); i++) {
                    // helical: rotate angle with time, radius oscillates
                    double helixAng = spAng[i] + rt * 0.08;
                    double helixR = 1.0 + Math.sin(rt * 0.06 + i * 0.5) * 0.5;
                    double bob = Math.sin(rt * 0.12 + i) * 0.1;
                    float bx = (float)(Math.cos(helixAng) * helixR);
                    float bz = (float)(Math.sin(helixAng) * helixR);
                    float by = (float)(yBase + bob + (i % 6) * 0.15); // stagger heights
                    flakes.get(i).animateTo(
                            new Vector3f(bx - 0.2f, by, bz - 0.2f),
                            new AxisAngle4f((float)(rt * 0.15 + i), 0, 1, 0),
                            new Vector3f(0.4f), 2);
                }
                // 3 layered particle types:
                //  - CLOUD trail per item (sampled)
                if (rt % 2 == 0) for (int i = 0; i < 6; i++) {
                    int idx = (int)(Math.random() * flakes.size());
                    double helixAng = spAng[idx] + rt * 0.08;
                    double helixR = 1.0 + Math.sin(rt * 0.06 + idx * 0.5) * 0.5;
                    Location p = c.clone().add(Math.cos(helixAng) * helixR, yBase - 0.4, Math.sin(helixAng) * helixR);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.01);
                }
                //  - ELECTRIC_SPARK at center column
                if (rt % 3 == 0) {
                    Location col = c.clone().add(0, 1 + Math.random() * (yBase + 1.0), 0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, col, 1, 0.1, 0.2, 0.1, 0.02);
                }
                //  - SNOWFLAKE rising upward (reverse gravity look) — positive Y velocity
                if (rt % 1 == 0) for (int i = 0; i < 5; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 2.5;
                    Location p = c.clone().add(Math.cos(a) * r, Math.random() * (yBase + 2.0), Math.sin(a) * r);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.05, 0.05, 0.05, 0.15);
                }
                // WEATHER_RAIN reverse-pitched loop
                if (rt % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.8f, 0.5f);
                }
                if (rt % 35 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 0.9f, 0.5f);
                }
            }
            // ----- PHASE 2C: CONVERGENCE (180-220t) — converge to single point at Y+10 -----
            else if (tick < CONVERGE_END) {
                int ct = tick - RISE_END;
                double t = ct / 40.0;
                for (int i = 0; i < flakes.size(); i++) {
                    // current position interp -> (0, 10, 0)
                    double helixAng = spAng[i] + RISE_END * 0.08;
                    double helixR = 1.0;
                    double startX = Math.cos(helixAng) * helixR;
                    double startZ = Math.sin(helixAng) * helixR;
                    double startY = 8.8;
                    double curX = startX + (0 - startX) * t;
                    double curZ = startZ + (0 - startZ) * t;
                    double curY = startY + (10.0 - startY) * t;
                    flakes.get(i).animateTo(
                            new Vector3f((float)curX - 0.2f, (float)curY, (float)curZ - 0.2f),
                            new AxisAngle4f((float)(ct * 0.3 + i), 0, 1, 0),
                            new Vector3f(0.4f), 2);
                }
                // BUBBLE_POP + END_ROD swirl at convergence
                if (ct % 2 == 0) {
                    Location p = c.clone().add((Math.random() - 0.5) * 1.0, 9 + Math.random() * 1.5, (Math.random() - 0.5) * 1.0);
                    w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
                if (ct == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.4f);
                if (ct == 35) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.0f);
            }
            // ----- HOLD (220 -> dissipateStart) — gentle hover at Y+10 -----
            else if (tick < dissipateStart) {
                for (int i = 0; i < flakes.size(); i++) {
                    double a = i * 0.4 + tick * 0.05;
                    double r = 0.4 + Math.sin(tick * 0.1 + i) * 0.15;
                    float bx = (float)(Math.cos(a) * r);
                    float bz = (float)(Math.sin(a) * r);
                    float by = 10.0f + (float)Math.sin(tick * 0.05 + i) * 0.15f;
                    flakes.get(i).animateTo(
                            new Vector3f(bx - 0.2f, by, bz - 0.2f),
                            new AxisAngle4f((float)(tick * 0.2 + i), 0, 1, 0),
                            new Vector3f(0.4f), 2);
                }
                if (tick % 3 == 0) {
                    Location p = c.clone().add((Math.random() - 0.5) * 1.5, 9.5 + Math.random() * 1.0, (Math.random() - 0.5) * 1.5);
                    w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0.02);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // ----- PHASE 3: DISSIPATE (last 20t) — radial burst outward -----
            else {
                int dt = tick - dissipateStart;
                if (dt == 0) {
                    // pick burst directions
                    for (int i = 0; i < 60; i++) {
                        burstAng[i] = Math.random() * Math.PI * 2;
                        burstR[i] = 4.0 + Math.random() * 4.0;
                    }
                    DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.4f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                    // END_ROD burst at convergence
                    for (int i = 0; i < 40; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 2.0;
                        Location p = c.clone().add(Math.cos(a) * r, 10 + Math.random() * 1.0, Math.sin(a) * r);
                        w.spawnParticle(Particle.END_ROD, p, 3, 0.3, 0.3, 0.3, 0.2);
                    }
                }
                double t = dt / 20.0;
                float scale = (float)Math.max(0.01, 0.4 * (1.0 - t));
                for (int i = 0; i < flakes.size(); i++) {
                    double curR = burstR[i] * t;
                    double bx = Math.cos(burstAng[i]) * curR;
                    double bz = Math.sin(burstAng[i]) * curR;
                    double by = 10.0 + (Math.random() - 0.5) * 2.0 * t;
                    flakes.get(i).animateTo(
                            new Vector3f((float)bx - scale * 0.5f, (float)by, (float)bz - scale * 0.5f),
                            new AxisAngle4f((float)(dt * 0.5 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                }
                if (tick % 2 == 0) for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 8.0;
                    w.spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(Math.cos(a) * r, 8 + Math.random() * 4, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.03);
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

        @Override public AbstractAttack newInstance() { return new InvertedSnowfall(plugin); }
    }

    // ================================================================
    // 40. CRYSTALLINE DOUBLE — 16 BLUE_ICE in 2 counter-rotating rings
    //     (8 each, Y=2 and Y=4). Spawn (0-30t): scale 0->0.7.
    //     Active: counter-rotation + intersection sparks + glow column +
    //     cold-blue dust shimmer. Dissipate (last 40t): rings accelerate,
    //     spin inward to (0,3,0), final central explosion cascade.
    // ================================================================
    public static class CrystallineDouble extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ringA = new ArrayList<>();
        private final List<ItemDisplayHandle> ringB = new ArrayList<>();
        private final double[] aAng = new double[8];
        private final double[] bAng = new double[8];
        private final double[] aAng0 = new double[8];
        private final double[] bAng0 = new double[8];
        private static final double RING_R = 4.0;
        private static final int SPAWN_END = 30;

        public CrystallineDouble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_double", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(36000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.5f);

            // Ring A — Y=2.0
            for (int i = 0; i < 8; i++) {
                aAng[i] = Math.PI * 2 * i / 8;
                aAng0[i] = aAng[i];
                Location p = c.clone().add(Math.cos(aAng[i]) * RING_R, 2.0, Math.sin(aAng[i]) * RING_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(120, 200, 255).interpolation(4, 0);
                ringA.add(h);
                spawnedEntities.add(h.entity());
            }
            // Ring B — Y=4.0 (offset)
            for (int i = 0; i < 8; i++) {
                bAng[i] = Math.PI * 2 * i / 8 + Math.PI / 8;
                bAng0[i] = bAng[i];
                Location p = c.clone().add(Math.cos(bAng[i]) * RING_R, 4.0, Math.sin(bAng[i]) * RING_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(80, 180, 255).interpolation(4, 0);
                ringB.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int duration = config.getDurationTicks();
            int dissipateStart = duration - 40;

            // ----- PHASE 1: SPAWN (0-30t) — scale 0 -> 0.7 -----
            if (tick < SPAWN_END) {
                double t = tick / (double)SPAWN_END;
                float scale = (float)(0.01 + t * 0.69); // -> 0.7
                for (int i = 0; i < 8; i++) {
                    float ax = (float)(Math.cos(aAng[i]) * RING_R);
                    float az = (float)(Math.sin(aAng[i]) * RING_R);
                    ringA.get(i).animateTo(
                            new Vector3f(ax - scale * 0.5f, 2.0f, az - scale * 0.5f),
                            new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(scale), 3);
                    float bx = (float)(Math.cos(bAng[i]) * RING_R);
                    float bz = (float)(Math.sin(bAng[i]) * RING_R);
                    ringB.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, 4.0f, bz - scale * 0.5f),
                            new AxisAngle4f((float)(-tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(scale), 3);
                }
                if (tick % 3 == 0) for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * RING_R;
                    w.spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(Math.cos(a) * r, 2 + Math.random() * 2.0, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
                if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.8f + (float)t * 0.6f);
            }
            // ----- PHASE 2: ACTIVE (30 -> dissipateStart) — counter-rotation -----
            else if (tick < dissipateStart) {
                int at = tick - SPAWN_END;
                // Ring A CW at 0.05 rad/tick, Ring B CCW
                double aSpeed = 0.05;
                double bSpeed = -0.05;
                for (int i = 0; i < 8; i++) {
                    aAng[i] += aSpeed;
                    float ax = (float)(Math.cos(aAng[i]) * RING_R);
                    float az = (float)(Math.sin(aAng[i]) * RING_R);
                    ringA.get(i).animateTo(
                            new Vector3f(ax - 0.35f, 2.0f, az - 0.35f),
                            new AxisAngle4f((float)(at * 0.08 + i), 0, 1, 0),
                            new Vector3f(0.7f), 2);

                    bAng[i] += bSpeed;
                    float bx = (float)(Math.cos(bAng[i]) * RING_R);
                    float bz = (float)(Math.sin(bAng[i]) * RING_R);
                    ringB.get(i).animateTo(
                            new Vector3f(bx - 0.35f, 4.0f, bz - 0.35f),
                            new AxisAngle4f((float)(-at * 0.08 + i), 0, 1, 0),
                            new Vector3f(0.7f), 2);
                }

                // 1) ELECTRIC_SPARK at vertical alignment intersections every 4t
                if (at % 4 == 0) {
                    boolean anyAligned = false;
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 8; j++) {
                            double dAng = ((aAng[i] - bAng[j]) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
                            if (dAng < 0.15 || dAng > (Math.PI * 2 - 0.15)) {
                                // Vertical sparks from Ring A item up to Ring B item
                                double mx = Math.cos(aAng[i]) * RING_R;
                                double mz = Math.sin(aAng[i]) * RING_R;
                                for (double yy = 2.0; yy <= 4.0; yy += 0.25) {
                                    w.spawnParticle(Particle.ELECTRIC_SPARK,
                                            c.clone().add(mx, yy, mz), 1, 0.05, 0.05, 0.05, 0.05);
                                }
                                anyAligned = true;
                            }
                        }
                    }
                    if (anyAligned) {
                        float pitch = 1.2f + (float)Math.random() * 0.6f;
                        DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, pitch);
                    }
                }

                // 2) GLOW core column between the rings
                if (at % 2 == 0) for (int i = 0; i < 4; i++) {
                    double offX = (Math.random() - 0.5) * 0.6;
                    double offZ = (Math.random() - 0.5) * 0.6;
                    double yy = 2.0 + Math.random() * 2.0;
                    w.spawnParticle(Particle.GLOW, c.clone().add(offX, yy, offZ), 1, 0.05, 0.1, 0.05, 0.01);
                }

                // 3) cold-blue DUST shimmer around each item (0xA8D8FF -> 168, 216, 255)
                if (at % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        Location ap = c.clone().add(Math.cos(aAng[i]) * RING_R, 2.0, Math.sin(aAng[i]) * RING_R);
                        Location bp = c.clone().add(Math.cos(bAng[i]) * RING_R, 4.0, Math.sin(bAng[i]) * RING_R);
                        DisplayBuilder.dustParticles(ap, 1, 0.2, 168, 216, 255, 1.2f);
                        DisplayBuilder.dustParticles(bp, 1, 0.2, 168, 216, 255, 1.2f);
                    }
                }

                // Random pitch BLOCK_AMETHYST_BLOCK_HIT during rotation
                if (at % 30 == 0) {
                    float pitch = 0.7f + (float)Math.random() * 1.2f;
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, pitch);
                }
            }
            // ----- PHASE 3: DISSIPATE (last 40t) — accelerate then collapse to (0,3,0) -----
            else {
                int dt = tick - dissipateStart;
                double t = dt / 40.0;
                // accelerate rotation to 0.2 rad/tick at end
                double accel = 0.05 + t * 0.15;
                float scale = (float)Math.max(0.01, 0.7 * (1.0 - t));
                for (int i = 0; i < 8; i++) {
                    aAng[i] += accel;
                    bAng[i] -= accel;
                    // spin inward — radius shrinks, Y converges to 3
                    double curR = RING_R * (1.0 - t);
                    double aY = 2.0 + (3.0 - 2.0) * t;
                    double bY = 4.0 + (3.0 - 4.0) * t;
                    float ax = (float)(Math.cos(aAng[i]) * curR);
                    float az = (float)(Math.sin(aAng[i]) * curR);
                    ringA.get(i).animateTo(
                            new Vector3f(ax - scale * 0.5f, (float)aY, az - scale * 0.5f),
                            new AxisAngle4f((float)(dt * 0.3 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                    float bx = (float)(Math.cos(bAng[i]) * curR);
                    float bz = (float)(Math.sin(bAng[i]) * curR);
                    ringB.get(i).animateTo(
                            new Vector3f(bx - scale * 0.5f, (float)bY, bz - scale * 0.5f),
                            new AxisAngle4f((float)(-dt * 0.3 + i), 0, 1, 0),
                            new Vector3f(scale), 2);
                }
                if (dt == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.2f, 0.9f);
                }
                // accelerating dust trail
                if (dt % 2 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double curR = RING_R * (1.0 - t);
                        double aY = 2.0 + (3.0 - 2.0) * t;
                        Location ap = c.clone().add(Math.cos(aAng[i]) * curR, aY, Math.sin(aAng[i]) * curR);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, ap, 1, 0.05, 0.05, 0.05, 0.1);
                        DisplayBuilder.dustParticles(ap, 1, 0.1, 168, 216, 255, 1.4f);
                    }
                }
                if (dt == 38) {
                    // Final central explosion: ELECTRIC_SPARK burst + EXPLODE + GLASS_BREAK cascade
                    Location center = c.clone().add(0, 3, 0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.9f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.8f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.2f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.0f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
                    for (int i = 0; i < 80; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 5.0;
                        double yy = 1.5 + Math.random() * 3.0;
                        Location p = c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.3, 0.3, 0.3, 0.25);
                        if (i % 3 == 0) DisplayBuilder.dustParticles(p, 2, 0.3, 168, 216, 255, 1.4f);
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

        @Override public AbstractAttack newInstance() { return new CrystallineDouble(plugin); }
    }
}
