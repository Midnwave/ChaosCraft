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
    // 34. RESONANT FROST NOTE — 1 large ECHO_SHARD ItemDisplay at center
    //     emits concentric SONIC_BOOM ring pulses every 20t. Constant
    //     radius 9.0, 3600hp ticksBetween=20.
    // ================================================================
    public static class ResonantFrostNote extends EnvironmentalAttack {
        private ItemDisplayHandle core;

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
            DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 1.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.2f);

            core = displayBuilder.spawnItem(c.clone().add(0, 1.8, 0), new ItemStack(Material.ECHO_SHARD));
            core.scale(1.4f, 1.4f, 1.4f).glow(80, 200, 240).interpolation(8, 0);
            spawnedEntities.add(core.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (core == null) return;

            // Spin and gentle bob
            float by = 1.8f + (float)Math.sin(tick * 0.08) * 0.25f;
            core.animateTo(
                    new Vector3f(-0.7f, by, -0.7f),
                    new AxisAngle4f((float)(tick * 0.15), 0, 1, 0),
                    new Vector3f(1.4f), 2);

            // Concentric ring pulses every 20t
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.3f, 1.5f);
                for (int r = 1; r <= 9; r++) {
                    final int rr = r;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (c.getWorld() == null) return;
                        // SONIC_BOOM ring at radius rr
                        for (int i = 0; i < 18; i++) {
                            double a = Math.PI * 2 * i / 18;
                            Location p = c.clone().add(Math.cos(a) * rr, 1.0, Math.sin(a) * rr);
                            c.getWorld().spawnParticle(Particle.SONIC_BOOM, p, 1, 0, 0, 0, 0);
                        }
                        DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 1, 0.05, 80, 200, 240, 1.0f);
                    }, rr * 2L);
                }
            }

            // Ambient ECHO_SHARD sparkle
            if (tick % 3 == 0) for (int i = 0; i < 2; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 1.5;
                w.spawnParticle(Particle.SCULK_SOUL, c.clone().add(Math.cos(a) * r, 1.5 + Math.random() * 1.0, Math.sin(a) * r),
                        1, 0.1, 0.1, 0.1, 0.02);
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
    // 35. SUBSONIC HUM — 6 NAUTILUS_SHELL ItemDisplays form a hexagon on
    //     the ground. Low-frequency SONIC_BOOM rings + WARDEN_HEARTBEAT
    //     at pitch 0.5. Constant radius 11.0, 2800hp ticksBetween=16.
    // ================================================================
    public static class SubsonicHum extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shells = new ArrayList<>();
        private final double[] sAng = new double[6];

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
            DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.7f, 0.6f);

            for (int i = 0; i < 6; i++) {
                sAng[i] = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(sAng[i]) * 5.5, 0.5, Math.sin(sAng[i]) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAUTILUS_SHELL));
                h.scale(1.1f, 1.1f, 1.1f).glow(50, 120, 200).interpolation(8, 0);
                shells.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Shells subtly pulse on heartbeat
            double phase = (tick % 40) / 40.0;
            float pulse = (float)(1.1 + Math.sin(phase * Math.PI * 2) * 0.15);
            for (int i = 0; i < shells.size(); i++) {
                float bx = (float)(Math.cos(sAng[i]) * 5.5);
                float bz = (float)(Math.sin(sAng[i]) * 5.5);
                shells.get(i).animateTo(
                        new Vector3f(bx - 0.55f, 0.5f, bz - 0.55f),
                        new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0),
                        new Vector3f(pulse), 4);
            }

            // Low-frequency SONIC_BOOM rings expand from each shell every 40t
            if (tick % 40 == 0) {
                for (int i = 0; i < shells.size(); i++) {
                    final double cx = Math.cos(sAng[i]) * 5.5;
                    final double cz = Math.sin(sAng[i]) * 5.5;
                    for (int r = 1; r <= 5; r++) {
                        final int rr = r;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (c.getWorld() == null) return;
                            for (int j = 0; j < 12; j++) {
                                double a = Math.PI * 2 * j / 12;
                                Location p = c.clone().add(cx + Math.cos(a) * rr * 0.9, 0.6, cz + Math.sin(a) * rr * 0.9);
                                c.getWorld().spawnParticle(Particle.SONIC_BOOM, p, 1, 0, 0, 0, 0);
                            }
                        }, rr * 3L);
                    }
                }
            }

            // Heartbeat pulse every 40 ticks
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.6f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            }
            // SCULK_SOUL ambient ground fog
            if (tick % 3 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 11.0;
                w.spawnParticle(Particle.SCULK_SOUL, c.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r),
                        1, 0.1, 0.1, 0.1, 0.01);
            }
            // Dark dust band on the hexagon
            if (tick % 6 == 0) for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24 + tick * 0.01;
                Location p = c.clone().add(Math.cos(a) * 5.5, 0.4, Math.sin(a) * 5.5);
                DisplayBuilder.dustParticles(p, 1, 0.1, 50, 120, 200, 1.4f);
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
    // 38. FROST MIRAGE — 12 GLASS_BOTTLE ItemDisplays form a humanoid
    //     silhouette that shifts position every 30 ticks (teleports as
    //     a group). Constant radius 5.0, 3400hp ticksBetween=12.
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
        private static final int TELEPORT_INTERVAL = 30;

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
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 1.3f, 1.3f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.2f);

            // 12 parts in a humanoid layout (head, 3 torso segs, 2 arms, 2 forearms, 2 legs, 2 shins)
            double[][] layout = {
                    { 0.0, 2.0, 0.0, 0.5, 0.5, 0.5 },     // head
                    { 0.0, 1.55, 0.0, 0.55, 0.4, 0.35 },  // upper torso
                    { 0.0, 1.2, 0.0, 0.55, 0.4, 0.35 },   // mid torso
                    { 0.0, 0.85, 0.0, 0.55, 0.4, 0.35 },  // lower torso
                    { -0.4, 1.55, 0.0, 0.25, 0.45, 0.25 }, // upper L arm
                    { 0.4, 1.55, 0.0, 0.25, 0.45, 0.25 },  // upper R arm
                    { -0.4, 1.05, 0.0, 0.22, 0.5, 0.22 },  // forearm L
                    { 0.4, 1.05, 0.0, 0.22, 0.5, 0.22 },   // forearm R
                    { -0.18, 0.55, 0.0, 0.25, 0.55, 0.25 }, // thigh L
                    { 0.18, 0.55, 0.0, 0.25, 0.55, 0.25 },  // thigh R
                    { -0.18, 0.15, 0.0, 0.22, 0.4, 0.22 },  // shin L
                    { 0.18, 0.15, 0.0, 0.22, 0.4, 0.22 }    // shin R
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
                h.scale(partSX[i], partSY[i], partSZ[i]).glow(180, 220, 255).interpolation(8, 0);
                parts.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Teleport every 30 ticks
            if (tick > 0 && tick % TELEPORT_INTERVAL == 0) {
                double a = Math.random() * Math.PI * 2;
                double r = 2.0 + Math.random() * 4.0;
                mirageX = Math.cos(a) * r;
                mirageZ = Math.sin(a) * r;
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 1.3f, 1.3f);

                for (int i = 0; i < parts.size(); i++) {
                    Location p = c.clone().add(mirageX + partDX[i], partDY[i], mirageZ + partDZ[i]);
                    w.spawnParticle(Particle.END_ROD, p, 4, 0.2, 0.2, 0.2, 0.1);
                    w.spawnParticle(Particle.SCULK_SOUL, p, 2, 0.2, 0.2, 0.2, 0.05);
                }
            }

            // Smooth fly to current mirage position
            for (int i = 0; i < parts.size(); i++) {
                float bx = (float)(mirageX + partDX[i]);
                float bz = (float)(mirageZ + partDZ[i]);
                parts.get(i).animateTo(
                        new Vector3f(bx - partSX[i] / 2.0f, (float)partDY[i], bz - partSZ[i] / 2.0f),
                        new AxisAngle4f((float)(tick * 0.04), 0, 1, 0),
                        new Vector3f(partSX[i], partSY[i], partSZ[i]), 4);
            }

            // Continuous shimmer
            if (tick % 2 == 0) for (int i = 0; i < parts.size(); i++) {
                Location p = c.clone().add(mirageX + partDX[i], partDY[i], mirageZ + partDZ[i]);
                if (Math.random() < 0.4) w.spawnParticle(Particle.END_ROD, p, 1, 0.1, 0.1, 0.1, 0.02);
                if (Math.random() < 0.25) w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.01);
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
    // 39. INVERTED SNOWFALL — 60 SNOWBALL ItemDisplays rise upward from
    //     ground (inverse of normal snowfall). Constant radius 7.0,
    //     2400hp ticksBetween=12.
    // ================================================================
    public static class InvertedSnowfall extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flakes = new ArrayList<>();
        private final double[] fAng = new double[60];
        private final double[] fR = new double[60];
        private final double[] fY = new double[60];
        private final double[] fSpd = new double[60];

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
            DisplayBuilder.playSound(c, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_PLACE, 1.0f, 0.6f);

            for (int i = 0; i < 60; i++) {
                fAng[i] = Math.random() * Math.PI * 2;
                fR[i] = Math.random() * 7.0;
                fY[i] = 0.3 + Math.random() * 1.5;
                fSpd[i] = 0.06 + Math.random() * 0.08;
                Location p = c.clone().add(Math.cos(fAng[i]) * fR[i], fY[i], Math.sin(fAng[i]) * fR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.4f, 0.4f, 0.4f).glow(230, 245, 255).interpolation(2, 0);
                flakes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Rise + drift
            for (int i = 0; i < flakes.size(); i++) {
                fY[i] += fSpd[i];
                if (fY[i] > 14.0) {
                    // Recycle to ground
                    fY[i] = 0.3 + Math.random() * 0.5;
                    fAng[i] = Math.random() * Math.PI * 2;
                    fR[i] = Math.random() * 7.0;
                }
                double driftA = fAng[i] + Math.sin(tick * 0.03 + i) * 0.2;
                float bx = (float)(Math.cos(driftA) * fR[i]);
                float bz = (float)(Math.sin(driftA) * fR[i]);
                flakes.get(i).animateTo(
                        new Vector3f(bx - 0.2f, (float)fY[i], bz - 0.2f),
                        new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0),
                        new Vector3f(0.4f), 2);
            }

            // Rising SNOWFLAKE particles
            if (tick % 1 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 7.0;
                double yy = Math.random() * 14.0;
                Location p = c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                // Positive Y velocity gives an upward-drifting look on small particle counts
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.05, 0.1, 0.08);
            }

            // Ethereal ALLAY hum periodically
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 0.5f);
            }
            if (tick % 70 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.8f, 0.5f);
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
    // 40. CRYSTALLINE DOUBLE — 16 BLUE_ICE ItemDisplays in two counter-
    //     rotating rings (8 each). Constant radius 6.0, 3000hp
    //     ticksBetween=14. ELECTRIC_SPARK at ring intersections.
    // ================================================================
    public static class CrystallineDouble extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ringA = new ArrayList<>();
        private final List<ItemDisplayHandle> ringB = new ArrayList<>();
        private final double[] aAng = new double[8];
        private final double[] bAng = new double[8];
        private static final double RING_R = 4.0;
        private static final int CHIME_INTERVAL = 16;

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

            // Ring A — horizontal ring at Y+1.0
            for (int i = 0; i < 8; i++) {
                aAng[i] = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(aAng[i]) * RING_R, 1.0, Math.sin(aAng[i]) * RING_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.55f, 0.55f, 0.55f).glow(120, 200, 255).interpolation(2, 0);
                ringA.add(h);
                spawnedEntities.add(h.entity());
            }
            // Ring B — counter-rotating ring at Y+2.5
            for (int i = 0; i < 8; i++) {
                bAng[i] = Math.PI * 2 * i / 8 + Math.PI / 8;
                Location p = c.clone().add(Math.cos(bAng[i]) * RING_R, 2.5, Math.sin(bAng[i]) * RING_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.55f, 0.55f, 0.55f).glow(80, 180, 255).interpolation(2, 0);
                ringB.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Ring A rotates clockwise, Ring B counter-clockwise
            double aSpeed = 0.08;
            double bSpeed = -0.08;
            for (int i = 0; i < 8; i++) {
                aAng[i] += aSpeed;
                float ax = (float)(Math.cos(aAng[i]) * RING_R);
                float az = (float)(Math.sin(aAng[i]) * RING_R);
                float ay = 1.0f + (float)(Math.sin(tick * 0.05 + i) * 0.15);
                ringA.get(i).animateTo(
                        new Vector3f(ax - 0.275f, ay, az - 0.275f),
                        new AxisAngle4f((float)(tick * 0.12 + i), 0, 1, 0),
                        new Vector3f(0.55f), 2);

                bAng[i] += bSpeed;
                float bx = (float)(Math.cos(bAng[i]) * RING_R);
                float bz = (float)(Math.sin(bAng[i]) * RING_R);
                float by = 2.5f + (float)(Math.sin(tick * 0.05 + i + Math.PI) * 0.15);
                ringB.get(i).animateTo(
                        new Vector3f(bx - 0.275f, by, bz - 0.275f),
                        new AxisAngle4f((float)(-tick * 0.12 + i), 0, 1, 0),
                        new Vector3f(0.55f), 2);
            }

            // ELECTRIC_SPARK at ring intersections (where angles cross)
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        double dAng = ((aAng[i] - bAng[j]) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
                        if (dAng < 0.18 || dAng > (Math.PI * 2 - 0.18)) {
                            Location ap = c.clone().add(Math.cos(aAng[i]) * RING_R, 1.0, Math.sin(aAng[i]) * RING_R);
                            Location bp = c.clone().add(Math.cos(bAng[j]) * RING_R, 2.5, Math.sin(bAng[j]) * RING_R);
                            Location mid = ap.clone().add(bp).multiply(0.5);
                            w.spawnParticle(Particle.ELECTRIC_SPARK, mid, 3, 0.3, 0.3, 0.3, 0.1);
                            DisplayBuilder.dustParticles(mid, 1, 0.1, 120, 200, 255, 1.2f);
                        }
                    }
                }
            }

            // Constant SNOWFLAKE between rings
            if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * RING_R;
                Location p = c.clone().add(Math.cos(a) * r, 1.0 + Math.random() * 2.0, Math.sin(a) * r);
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Chime on each "rotation" pulse
            if (tick % CHIME_INTERVAL == 0) {
                float pitch = 1.0f + (float)Math.sin(tick * 0.02) * 0.4f;
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, pitch);
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
