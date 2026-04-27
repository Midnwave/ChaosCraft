package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * DevilsDream Mode — ENVIRONMENTAL FX BATCH 1 (entries 1-10).
 * Pure particle + ItemDisplay attacks. No BlockDisplays.
 * Each attack is a particle system with item-display accents,
 * unique animation patterns, and layered sound design.
 */
public final class DDEnvFX1 {
    private DDEnvFX1() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ScreamingAbyss(plugin));
        registry.register(new BleedingWall(plugin));
        registry.register(new PhantomBonfire(plugin));
        registry.register(new LocustStorm(plugin));
        registry.register(new MirrorShardsRain(plugin));
        registry.register(new SunkenCathedral(plugin));
        registry.register(new DevilsRain(plugin));
        registry.register(new ShiveringDark(plugin));
        registry.register(new AshenCathedralVaults(plugin));
        registry.register(new BloodTide(plugin));
    }

    // ================================================================
    // 1. THE SCREAMING ABYSS — A bottomless pit illusion.
    //    Soul-lantern rim, descending lantern column, particle inhale.
    // ================================================================
    public static class ScreamingAbyss extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> rim = new ArrayList<>();
        private final List<ItemDisplayHandle> deep = new ArrayList<>();

        public ScreamingAbyss(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("screaming_abyss", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0); config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25); config.setDurationTicks(420); config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.4f, 0.3f);
            DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.4f);
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                Location p = c.clone().add(Math.cos(a) * 5.0, 0.4, Math.sin(a) * 5.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.9f, 0.9f, 0.9f).glow(40, 200, 220).interpolation(12, 0);
                rim.add(h);
            }
            for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 1.8;
                Location p = c.clone().add(Math.cos(a) * r, -1 - i * 0.7, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.6f + (float) Math.random() * 0.4f, 0.6f, 0.6f).glow(40, 200, 220).interpolation(20, 0);
                deep.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Rim lanterns rotate around centre
            if (tick % 6 == 0) {
                for (int i = 0; i < rim.size(); i++) {
                    double a = Math.PI * 2 * i / rim.size() + tick * 0.012;
                    double r = 5.0 + Math.sin(tick * 0.05) * 0.3;
                    rim.get(i).animateTo(new Vector3f((float)(Math.cos(a)*r) - 0.5f, 0.4f, (float)(Math.sin(a)*r) - 0.5f),
                            new AxisAngle4f((float)(tick * 0.04), 0, 1, 0), new Vector3f(0.9f), 6);
                }
            }
            // Descent illusion — lanterns drift in/out of darkness below
            if (tick % 10 == 0) {
                for (int i = 0; i < deep.size(); i++) {
                    double a = tick * 0.02 + i * 0.7;
                    double r = 0.5 + Math.sin(tick * 0.04 + i) * 1.2;
                    float ty = -1f - i * 0.7f + (float) Math.sin(tick * 0.06 + i) * 0.4f;
                    float s = 0.6f + (float) Math.sin(tick * 0.08 + i) * 0.15f;
                    deep.get(i).animateTo(new Vector3f((float)(Math.cos(a)*r) - s/2f, ty, (float)(Math.sin(a)*r) - s/2f),
                            new AxisAngle4f((float)(tick * 0.05), 0, 1, 0), new Vector3f(s), 10);
                }
            }
            // Black ink + smoke column rising from below
            if (tick % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double a = Math.random() * Math.PI * 2, r = Math.random() * 4.5, y = Math.random() * 6;
                    Location p = getCenter().clone().add(Math.cos(a) * r, -2 + y, Math.sin(a) * r);
                    w.spawnParticle(Particle.SQUID_INK, p, 1, 0.3, 0.3, 0.3, 0.02);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.3, 0.4, 0.3, 0.04);
                    if (Math.random() < 0.25) w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.4, 0.2, 0.4, 0.01);
                }
            }
            // Inward spiral of soul fire flame
            if (tick % 2 == 0) {
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2 * i / 24) + tick * 0.08;
                    double r = 5.5 - (tick % 30) * 0.18;
                    if (r < 0.5) r = 5.5;
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(Math.cos(a) * r, 0.6, Math.sin(a) * r), 1, 0.05, 0.05, 0.05, 0);
                }
            }
            // Drip from rim
            if (tick % 3 == 0) for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, getCenter().clone().add(Math.cos(a) * 5, 0.3, Math.sin(a) * 5), 2, 0.2, 0.1, 0.2, 0);
            }
            // Pull players inward
            if (tick % 8 == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double d2 = p.getLocation().distanceSquared(getCenter());
                    if (d2 <= r2 && d2 > 1.0) {
                        Vector pull = getCenter().toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.18);
                        pull.setY(-0.05);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }
            if (tick % 60 == 0 && Math.random() < 0.6)
                DisplayBuilder.playSound(getCenter().clone().add(0, -3, 0), Sound.ENTITY_GHAST_WARN, 0.8f, 0.4f + (float) Math.random() * 0.3f);
            if (tick % 80 == 0) DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 1.1f, 0.3f);
            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ScreamingAbyss(plugin); }
    }

    // ================================================================
    // 2. BLEEDING WALL — Vertical sheet of dripping blood.
    //    REDSTONE_BLOCK ItemDisplays as wall surface, dust rivulets.
    // ================================================================
    public static class BleedingWall extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> wall = new ArrayList<>();

        public BleedingWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bleeding_wall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0); config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(360); config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.BLOCK_HONEY_BLOCK_BREAK, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_HOSTILE_HURT, 0.8f, 0.3f);
            for (int row = 0; row < 6; row++) for (int col = 0; col < 8; col++) {
                Location p = c.clone().add((col - 3.5) * 0.9, 0.5 + row * 0.9, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.REDSTONE_BLOCK));
                h.scale(0.85f, 0.85f, 0.05f).glow(180, 20, 20).interpolation(10, 0);
                wall.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Wall ripples — each tile pulses on a wave traveling down
            if (tick % 4 == 0) {
                for (int row = 0; row < 6; row++) for (int col = 0; col < 8; col++) {
                    int idx = row * 8 + col;
                    if (idx >= wall.size()) continue;
                    float ripple = (float)(Math.sin((tick - col * 3) * 0.15) * 0.05);
                    float s = 0.85f + ripple;
                    wall.get(idx).animateTo(
                            new Vector3f((float)((col - 3.5) * 0.9 - s/2f), 0.5f + row * 0.9f, -0.025f),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(s, s, 0.05f), 4);
                }
            }
            // Blood-red dust rivulets streaming down the wall surface
            if (tick % 2 == 0) for (int i = 0; i < 14; i++) {
                double x = (Math.random() - 0.5) * 7;
                double y = Math.random() * 5.5 + 0.5;
                Location p = c.clone().add(x, y, 0.05);
                DisplayBuilder.dustParticles(p, 1, 0.05, 192, 16, 16, 1.4f);
                if (Math.random() < 0.3) w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, p, 1, 0.05, 0.1, 0, 0);
            }
            // Pooling on the ground in front of the wall
            if (tick % 5 == 0) for (int i = 0; i < 8; i++) {
                double x = (Math.random() - 0.5) * 7;
                double z = Math.random() * 0.6;
                Location p = c.clone().add(x, 0.05, z);
                DisplayBuilder.dustParticles(p, 1, 0.4, 120, 8, 8, 2.2f);
                if (Math.random() < 0.25) w.spawnParticle(Particle.LANDING_OBSIDIAN_TEAR, p, 1, 0.2, 0, 0.2, 0);
            }
            // Periodic gore burst
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT, 0.6f, 0.4f);
                for (int i = 0; i < 24; i++) {
                    double a = Math.random() * Math.PI * 2;
                    Location p = c.clone().add(Math.cos(a) * 1.0, 1.0 + Math.random() * 4, Math.sin(a) * 0.3);
                    w.spawnParticle(Particle.DAMAGE_INDICATOR, p, 1, 0.3, 0.3, 0.3, 0.05);
                }
            }
            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_HONEY_BLOCK_FALL, 0.7f, 0.3f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new BleedingWall(plugin); }
    }

    // ================================================================
    // 3. PHANTOM BONFIRE — Spectral fire where flames are torches.
    //    Soul torches floating at flame positions, ghastly heat shimmer.
    // ================================================================
    public static class PhantomBonfire extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flames = new ArrayList<>();
        private final List<ItemDisplayHandle> sparks = new ArrayList<>();

        public PhantomBonfire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_bonfire", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0); config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(20); config.setDurationTicks(380); config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.ITEM_FIRECHARGE_USE, 1.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 1.2f, 0.5f);
            // 9 soul torches in a clustered fire pile
            for (int i = 0; i < 9; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 1.2;
                Location p = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 1.5, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_TORCH));
                h.scale(0.7f + (float)Math.random() * 0.3f, 0.7f, 0.7f).glow(80, 200, 240).interpolation(12, 0);
                flames.add(h);
            }
            // 8 floating spark items orbiting the fire (BLAZE_POWDER as pseudo-spark)
            for (int i = 0; i < 8; i++) {
                Location p = c.clone().add(0, 1.5, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLAZE_POWDER));
                h.scale(0.25f, 0.25f, 0.25f).glow(255, 200, 80).interpolation(8, 0);
                sparks.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Flames flicker — bob up/down, scale pulse
            if (tick % 4 == 0) {
                for (int i = 0; i < flames.size(); i++) {
                    float ty = 0.3f + (float)Math.sin(tick * 0.12 + i * 0.7) * 0.4f + i * 0.15f;
                    float s = 0.7f + (float)Math.sin(tick * 0.18 + i) * 0.15f;
                    double a = i * 0.7 + tick * 0.005, r = 0.6 + Math.sin(tick * 0.04 + i) * 0.4;
                    flames.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * r) - s/2f, ty, (float)(Math.sin(a) * r) - s/2f),
                            new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0), new Vector3f(s), 4);
                }
            }
            // Sparks orbit upward in a helix
            if (tick % 3 == 0) {
                for (int i = 0; i < sparks.size(); i++) {
                    double a = i * (Math.PI * 2 / 8) + tick * 0.1;
                    double r = 1.2 + Math.sin(tick * 0.05 + i) * 0.3;
                    float ty = 1.0f + ((tick / 3 + i * 8) % 50) * 0.12f;
                    sparks.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * r) - 0.125f, ty, (float)(Math.sin(a) * r) - 0.125f),
                            new AxisAngle4f((float)(tick * 0.2), 0, 1, 0), new Vector3f(0.25f), 3);
                }
            }
            // Soul flame particles + smoke column
            if (tick % 2 == 0) for (int i = 0; i < 12; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 1.8;
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 2.5, Math.sin(a) * r), 1, 0.1, 0.1, 0.1, 0.01);
            }
            if (tick % 4 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 0.8;
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(Math.cos(a) * r, 2.5 + Math.random() * 2, Math.sin(a) * r), 1, 0.2, 0.3, 0.2, 0.03);
            }
            // Heat shimmer ring at ground
            if (tick % 6 == 0) for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16 + tick * 0.05;
                w.spawnParticle(Particle.SCRAPE, c.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3), 1, 0.1, 0.05, 0.1, 0);
            }
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.9f, 0.4f + (float)Math.random() * 0.3f);
            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_AMBIENT, 0.5f, 0.6f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); p.setFireTicks(60); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PhantomBonfire(plugin); }
    }

    // ================================================================
    // 4. LOCUST STORM — Swarm of phantom membranes (locusts) circling.
    //    Each "locust" = phantom membrane ItemDisplay flapping.
    // ================================================================
    public static class LocustStorm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> swarm = new ArrayList<>();
        private final double[] swarmAngle = new double[20];
        private final double[] swarmHeight = new double[20];
        private final double[] swarmRadius = new double[20];

        public LocustStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("locust_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0); config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(360); config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_SILVERFISH_AMBIENT, 1.0f, 0.5f);
            for (int i = 0; i < 20; i++) {
                swarmAngle[i] = Math.random() * Math.PI * 2;
                swarmHeight[i] = 1.0 + Math.random() * 4.0;
                swarmRadius[i] = 2.5 + Math.random() * 3.5;
                Location p = c.clone();
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.45f, 0.05f, 0.45f).interpolation(2, 0);
                swarm.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Swarm motion — each locust orbits with chaotic drift
            for (int i = 0; i < swarm.size(); i++) {
                swarmAngle[i] += 0.08 + (i % 3) * 0.02;
                swarmHeight[i] += Math.sin(tick * 0.1 + i) * 0.05;
                swarmRadius[i] += Math.sin(tick * 0.07 + i * 0.5) * 0.1;
                if (swarmRadius[i] < 1.5) swarmRadius[i] = 1.5;
                if (swarmRadius[i] > 7.0) swarmRadius[i] = 7.0;
                if (swarmHeight[i] < 0.5) swarmHeight[i] = 0.5;
                if (swarmHeight[i] > 6.0) swarmHeight[i] = 6.0;
                float tx = (float)(Math.cos(swarmAngle[i]) * swarmRadius[i]);
                float ty = (float)swarmHeight[i];
                float tz = (float)(Math.sin(swarmAngle[i]) * swarmRadius[i]);
                // Wing flap — scale Y oscillation
                float wing = 0.05f + (float)Math.abs(Math.sin(tick * 0.5 + i * 0.3)) * 0.4f;
                swarm.get(i).animateTo(new Vector3f(tx - 0.225f, ty, tz - 0.225f),
                        new AxisAngle4f((float)swarmAngle[i] + 1.57f, 0, 1, 0),
                        new Vector3f(0.45f, wing, 0.45f), 2);
            }
            // Particle wing-trails
            if (tick % 2 == 0) for (int i = 0; i < swarm.size(); i++) {
                Location p = c.clone().add(Math.cos(swarmAngle[i]) * swarmRadius[i], swarmHeight[i], Math.sin(swarmAngle[i]) * swarmRadius[i]);
                w.spawnParticle(Particle.ASH, p, 1, 0.1, 0.1, 0.1, 0.01);
            }
            if (tick % 3 == 0) {
                for (int i = 0; i < 10; i++) {
                    double a = Math.random() * Math.PI * 2, r = 2 + Math.random() * 5, y = Math.random() * 5;
                    DisplayBuilder.dustParticles(c.clone().add(Math.cos(a) * r, 0.5 + y, Math.sin(a) * r), 1, 0.1, 80, 60, 30, 0.8f);
                }
            }
            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.9f, 0.7f);
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_SILVERFISH_HURT, 0.6f, 0.5f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new LocustStorm(plugin); }
    }

    // ================================================================
    // 5. MIRROR SHARDS RAIN — Falling mirror shards (glass panes as items).
    // ================================================================
    public static class MirrorShardsRain extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] fallSpeed = new double[14];
        private final double[] startY = new double[14];

        public MirrorShardsRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mirror_shards_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0); config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(320); config.setCooldownTicks(280);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 1.6f);
            for (int i = 0; i < 14; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 6.5;
                fallSpeed[i] = 0.18 + Math.random() * 0.12;
                startY[i] = 7 + Math.random() * 4;
                Location p = c.clone().add(Math.cos(a) * r, startY[i], Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_PANE));
                h.scale(0.5f + (float)Math.random() * 0.4f, 0.05f, 0.5f + (float)Math.random() * 0.4f).glow(220, 220, 250).interpolation(2, 0);
                shards.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            for (int i = 0; i < shards.size(); i++) {
                startY[i] -= fallSpeed[i];
                if (startY[i] < 0.2) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.6f + (float)Math.random() * 0.3f);
                    w.spawnParticle(Particle.ITEM_SLIME, shards.get(i).entity().getLocation(), 6, 0.3, 0.1, 0.3, 0.1);
                    double a = Math.random() * Math.PI * 2, r = Math.random() * 6.5;
                    startY[i] = 7 + Math.random() * 4;
                    fallSpeed[i] = 0.18 + Math.random() * 0.12;
                    shards.get(i).entity().teleport(c.clone().add(Math.cos(a) * r, startY[i], Math.sin(a) * r));
                }
                Location p = shards.get(i).entity().getLocation();
                shards.get(i).animateTo(
                        new Vector3f((float)(p.getX() - c.getX()) - 0.3f, (float)(p.getY() - c.getY()) - (float)fallSpeed[i], (float)(p.getZ() - c.getZ()) - 0.3f),
                        new AxisAngle4f((float)(tick * 0.15 + i), 1, 0, 1), new Vector3f(0.6f, 0.05f, 0.6f), 2);
            }
            // Falling glint particles trailing each shard
            if (tick % 2 == 0) for (int i = 0; i < shards.size(); i++) {
                Location p = shards.get(i).entity().getLocation();
                w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0);
                if (Math.random() < 0.3) DisplayBuilder.dustParticles(p, 1, 0.1, 220, 220, 250, 0.6f);
            }
            // Damage on contact
            if (tick % 5 == 0) {
                for (int i = 0; i < shards.size(); i++) {
                    Location sp = shards.get(i).entity().getLocation();
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(sp) < 1.4) {
                            p.damage(config.getDamage()); p.setNoDamageTicks(0);
                        }
                    }
                }
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.5f);
        }

        @Override public AbstractAttack newInstance() { return new MirrorShardsRain(plugin); }
    }

    // ================================================================
    // 6. SUNKEN CATHEDRAL — Faint pillars + drifting underwater haze.
    //    Conduit ItemDisplays as hazy vertical lights. Heavy spore clouds.
    // ================================================================
    public static class SunkenCathedral extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> pillars = new ArrayList<>();

        public SunkenCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sunken_cathedral", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(25); config.setDurationTicks(440); config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE, 1.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CONDUIT_AMBIENT, 1.0f, 0.5f);
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 5, 1, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CONDUIT));
                h.scale(0.6f, 4.0f, 0.6f).glow(80, 140, 200).interpolation(20, 0);
                pillars.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Pillars sway gently
            if (tick % 8 == 0) {
                for (int i = 0; i < pillars.size(); i++) {
                    double a = Math.PI * 2 * i / 6 + Math.sin(tick * 0.02 + i) * 0.1;
                    float tx = (float)(Math.cos(a) * 5);
                    float tz = (float)(Math.sin(a) * 5);
                    float pulse = 4.0f + (float)Math.sin(tick * 0.05 + i * 0.5) * 0.4f;
                    pillars.get(i).animateTo(new Vector3f(tx - 0.3f, 1f, tz - 0.3f),
                            new AxisAngle4f((float)(tick * 0.02), 0, 1, 0),
                            new Vector3f(0.6f, pulse, 0.6f), 8);
                }
            }
            // Drifting blue spores everywhere
            if (tick % 2 == 0) for (int i = 0; i < 18; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 7, y = Math.random() * 6;
                Location p = c.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                w.spawnParticle(Particle.WARPED_SPORE, p, 1, 0.3, 0.3, 0.3, 0.005);
                if (Math.random() < 0.3) DisplayBuilder.dustParticles(p, 1, 0.2, 100, 160, 200, 1.2f);
            }
            // Bubble columns rising at pillar bases
            if (tick % 3 == 0) for (int i = 0; i < pillars.size(); i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 5, 0.2 + Math.random() * 5, Math.sin(a) * 5);
                w.spawnParticle(Particle.BUBBLE_COLUMN_UP, p, 1, 0.1, 0.2, 0.1, 0.05);
            }
            if (tick % 70 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 0.8f, 0.5f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SunkenCathedral(plugin); }
    }

    // ================================================================
    // 7. DEVIL'S RAIN — Falling fire charges as raindrops.
    // ================================================================
    public static class DevilsRain extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> drops = new ArrayList<>();
        private final double[] dropY = new double[16];

        public DevilsRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(360); config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.3f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.7f);
            for (int i = 0; i < 16; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 7.5;
                dropY[i] = 7 + Math.random() * 3;
                Location p = c.clone().add(Math.cos(a) * r, dropY[i], Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FIRE_CHARGE));
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 80, 20).interpolation(2, 0);
                drops.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            for (int i = 0; i < drops.size(); i++) {
                dropY[i] -= 0.3;
                if (dropY[i] < 0.1) {
                    Location impact = drops.get(i).entity().getLocation();
                    w.spawnParticle(Particle.FLAME, impact, 8, 0.4, 0.1, 0.4, 0.05);
                    w.spawnParticle(Particle.LAVA, impact, 2, 0.3, 0.1, 0.3, 0);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.5f);
                    double a = Math.random() * Math.PI * 2, r = Math.random() * 7.5;
                    dropY[i] = 7 + Math.random() * 3;
                    drops.get(i).entity().teleport(c.clone().add(Math.cos(a) * r, dropY[i], Math.sin(a) * r));
                    // Damage on landing
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(impact) < 4) { p.damage(config.getDamage()); p.setNoDamageTicks(0); p.setFireTicks(40); }
                    }
                }
                Location p = drops.get(i).entity().getLocation();
                drops.get(i).animateTo(
                        new Vector3f((float)(p.getX() - c.getX()) - 0.2f, (float)(p.getY() - c.getY()) - 0.3f, (float)(p.getZ() - c.getZ()) - 0.2f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 1, 1, 0), new Vector3f(0.4f), 2);
            }
            // Trail flame particles
            if (tick % 2 == 0) for (int i = 0; i < drops.size(); i++) {
                Location p = drops.get(i).entity().getLocation();
                w.spawnParticle(Particle.FLAME, p, 1, 0.05, 0.1, 0.05, 0.01);
                w.spawnParticle(Particle.SMALL_FLAME, p, 1, 0.05, 0.1, 0.05, 0);
            }
            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.8f, 0.4f);
        }

        @Override public AbstractAttack newInstance() { return new DevilsRain(plugin); }
    }

    // ================================================================
    // 8. SHIVERING DARK — Cold black void cloud. Slow, dense, oppressive.
    // ================================================================
    public static class ShiveringDark extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> wisps = new ArrayList<>();

        public ShiveringDark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shivering_dark", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0); config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(420); config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.AMBIENT_BASALT_DELTAS_LOOP, 1.4f, 0.3f);
            DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_HIT, 1.0f, 0.4f);
            for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2, r = 1 + Math.random() * 4;
                Location p = c.clone().add(Math.cos(a) * r, 1.5 + Math.random() * 3, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLACK_WOOL));
                h.scale(1.2f + (float)Math.random() * 0.6f, 1.2f, 1.2f).glow(20, 0, 30).interpolation(20, 0);
                wisps.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Wisps drift slowly
            if (tick % 12 == 0) {
                for (int i = 0; i < wisps.size(); i++) {
                    double a = i * 0.78 + tick * 0.012;
                    double r = 2 + Math.sin(tick * 0.02 + i) * 1.5;
                    float ty = 1.5f + (float)Math.sin(tick * 0.015 + i) * 1.5f;
                    float s = 1.4f + (float)Math.sin(tick * 0.025 + i) * 0.3f;
                    wisps.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * r) - s/2f, ty, (float)(Math.sin(a) * r) - s/2f),
                            new AxisAngle4f((float)(tick * 0.01), 0, 1, 0), new Vector3f(s), 12);
                }
            }
            // Heavy black ink + smoke
            if (tick % 2 == 0) for (int i = 0; i < 18; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 5.5, y = 0.5 + Math.random() * 5;
                Location p = c.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                w.spawnParticle(Particle.SQUID_INK, p, 1, 0.4, 0.4, 0.4, 0.005);
                w.spawnParticle(Particle.SMOKE, p, 1, 0.3, 0.3, 0.3, 0.01);
            }
            // Snowflakes (cold)
            if (tick % 3 == 0) for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 6;
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(Math.cos(a) * r, 4 + Math.random() * 2, Math.sin(a) * r), 1, 0.2, 0.1, 0.2, 0.01);
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_FALL, 0.6f, 0.4f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); p.setFreezeTicks(p.getFreezeTicks() + 60); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ShiveringDark(plugin); }
    }

    // ================================================================
    // 9. ASHEN CATHEDRAL VAULTS — Falling ash from invisible vaults.
    // ================================================================
    public static class AshenCathedralVaults extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> embers = new ArrayList<>();

        public AshenCathedralVaults(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ashen_cathedral_vaults", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5); config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(420); config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.BLOCK_CAMPFIRE_CRACKLE, 1.2f, 0.4f);
            DisplayBuilder.playSound(c, Sound.AMBIENT_NETHER_WASTES_LOOP, 1.0f, 0.5f);
            for (int i = 0; i < 10; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 6;
                Location p = c.clone().add(Math.cos(a) * r, 5 + Math.random() * 2, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.MAGMA_CREAM));
                h.scale(0.3f, 0.3f, 0.3f).glow(255, 100, 20).interpolation(2, 0);
                embers.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Embers drift down + sideways
            if (tick % 2 == 0) {
                for (int i = 0; i < embers.size(); i++) {
                    Location p = embers.get(i).entity().getLocation();
                    double dx = Math.sin(tick * 0.05 + i) * 0.05;
                    double dz = Math.cos(tick * 0.04 + i) * 0.05;
                    embers.get(i).animateTo(
                            new Vector3f((float)(p.getX() - c.getX()) - 0.15f + (float)dx,
                                    (float)(p.getY() - c.getY()) - 0.15f,
                                    (float)(p.getZ() - c.getZ()) - 0.15f + (float)dz),
                            new AxisAngle4f((float)(tick * 0.1 + i), 1, 1, 0), new Vector3f(0.3f), 2);
                    if (p.getY() < c.getY() + 0.3) {
                        double a = Math.random() * Math.PI * 2, r = Math.random() * 6;
                        embers.get(i).entity().teleport(c.clone().add(Math.cos(a) * r, 5 + Math.random() * 2, Math.sin(a) * r));
                    }
                }
            }
            // Dense ash rain
            if (tick % 1 == 0) for (int i = 0; i < 22; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 7, y = Math.random() * 6;
                Location p = c.clone().add(Math.cos(a) * r, 0.3 + y, Math.sin(a) * r);
                w.spawnParticle(Particle.WHITE_ASH, p, 1, 0.3, 0.4, 0.3, 0.02);
                if (Math.random() < 0.2) w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.2, 0.2, 0.2, 0.01);
            }
            // Falling ember dust trails
            if (tick % 2 == 0) for (int i = 0; i < embers.size(); i++) {
                Location p = embers.get(i).entity().getLocation();
                w.spawnParticle(Particle.LAVA, p, 1, 0, 0, 0, 0);
                DisplayBuilder.dustParticles(p, 1, 0.05, 255, 100, 20, 0.6f);
            }
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.6f, 0.5f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new AshenCathedralVaults(plugin); }
    }

    // ================================================================
    // 10. BLOOD TIDE — Sweeping wave of blood-red across the ground.
    // ================================================================
    public static class BloodTide extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> waveCrests = new ArrayList<>();
        private double waveAngle = 0;

        public BloodTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_tide", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(360); config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.AMBIENT_UNDERWATER_LOOP, 1.3f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_HOSTILE_HURT, 0.9f, 0.3f);
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                Location p = c.clone().add(Math.cos(a) * 6, 0.3, Math.sin(a) * 6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.REDSTONE_BLOCK));
                h.scale(1.0f, 0.15f, 1.0f).glow(180, 10, 10).interpolation(8, 0);
                waveCrests.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            waveAngle += 0.05;
            // Wave crests sweep around the perimeter
            if (tick % 4 == 0) {
                for (int i = 0; i < waveCrests.size(); i++) {
                    double a = Math.PI * 2 * i / 12 + waveAngle;
                    double r = 6 + Math.sin(tick * 0.08 + i) * 0.6;
                    float wave = (float)Math.abs(Math.sin(tick * 0.1 + i * 0.5)) * 0.4f + 0.15f;
                    waveCrests.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * r) - 0.5f, 0.3f + wave, (float)(Math.sin(a) * r) - 0.5f),
                            new AxisAngle4f((float)a, 0, 1, 0),
                            new Vector3f(1.0f, wave * 2, 1.0f), 4);
                }
            }
            // Blood ground particles + dripping
            if (tick % 2 == 0) for (int i = 0; i < 28; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 8;
                Location p = c.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                DisplayBuilder.dustParticles(p, 1, 0.15, 180, 10, 10, 1.6f);
                if (Math.random() < 0.1) w.spawnParticle(Particle.LANDING_OBSIDIAN_TEAR, p, 1, 0.2, 0.05, 0.2, 0);
            }
            // Tide line — sweeping arc particles
            if (tick % 1 == 0) for (int i = 0; i < 32; i++) {
                double a = waveAngle + (i / 32.0) * 0.6 - 0.3;
                double r = 6.5 + Math.sin(tick * 0.1) * 0.4;
                Location p = c.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                DisplayBuilder.dustParticles(p, 1, 0.05, 255, 30, 30, 2.0f);
            }
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS, 0.9f, 0.4f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new BloodTide(plugin); }
    }
}
