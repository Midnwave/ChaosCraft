package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #61-70
 * BRIMSTONE HELLSCAPE (#61-62) + CALAMITAS'S DOMAIN (#63-70)
 *
 * Design: No status effects. Damage 6.0-18.0 HP. AxisAngle4f only.
 */
public final class TotalChaosB {

    private TotalChaosB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SoulFireRainHeavy(plugin));
        registry.register(new BrimstoneConvergencePulse(plugin));
        registry.register(new SigilPulseMinor(plugin));
        registry.register(new SigilPulseMajor(plugin));
        registry.register(new MonolithRotation(plugin));
        registry.register(new MonolithBeamFire(plugin));
        registry.register(new SoulTowerAwakening(plugin));
        registry.register(new EchoGalleryWail(plugin));
        registry.register(new CrimsonCrownDescent(plugin));
        registry.register(new CrimsonCrownCrash(plugin));
    }

    // =========================================================================
    // 61. SOUL FIRE RAIN HEAVY -- triple density rain storm
    // =========================================================================
    public static class SoulFireRainHeavy extends EnvironmentalAttack {
        public SoulFireRainHeavy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_rain_heavy", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(220); config.setCooldownTicks(1200); config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true); config.setImpactDamage(7.0); config.setImpactRadius(5.0);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 20) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 40, 0), 30, 10, 0.5, 10, 0.02); return; }
            int rainTick = ticksAlive - 21;
            int rate = rainTick > 140 ? 20 : 12; // Peak torrent in final 3 seconds
            double splatRadius = 2.5;
            double splatDmg = rainTick > 140 ? 6.0 : 7.0;
            for (int i = 0; i < rate; i++) {
                double rx = center.getX() + (Math.random() - 0.5) * 20; double rz = center.getZ() + (Math.random() - 0.5) * 20;
                double ry = center.getY() + 40 - (rainTick % 30) * 1.33;
                if (ry <= center.getY() + 0.5) {
                    Location splat = new Location(w, rx, center.getY() + 0.2, rz);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, splat, 20, splatRadius, 0.2, splatRadius, 0.02);
                    w.spawnParticle(Particle.SMOKE, splat, 5, splatRadius * 0.5, 0.3, splatRadius * 0.5, 0.01);
                    for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(splat) <= splatRadius * splatRadius * 4) p.damage(splatDmg); }
                } else { w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, rx, ry, rz), 1, 0, 0, 0, 0); }
            }
            if (rainTick % 3 == 0) { DisplayBuilder.dustParticles(center.clone().add((Math.random()-0.5)*18, 35, (Math.random()-0.5)*18), 5, 2, 0, 120, 180, 1.0f); }
            if (rainTick % 20 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_FALL, 0.7f, 0.5f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulFireRainHeavy(plugin); }
    }

    // =========================================================================
    // 62. BRIMSTONE CONVERGENCE PULSE -- all brimstone systems fire at once
    // =========================================================================
    public static class BrimstoneConvergencePulse extends EnvironmentalAttack {
        private boolean shockwaved = false;
        public BrimstoneConvergencePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_convergence_pulse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(240); config.setCooldownTicks(2000); config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) { DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.3f); } }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 120) {
                if (ticksAlive % 4 == 0) { for (int i = 0; i < 8; i++) { double bx = center.getX() + (Math.random()-0.5)*18; double bz = center.getZ() + (Math.random()-0.5)*18; DisplayBuilder.dustParticles(new Location(w, bx, center.getY()+0.3, bz), 5, 0.5, 255, 50, 0, 1.0f); } }
                if (ticksAlive % 40 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.3f);
                return;
            }
            int execTick = ticksAlive - 121;
            if (execTick >= 0 && execTick <= 60) {
                if (execTick % 2 == 0) {
                    for (int i = 0; i < 12; i++) { double fx = center.getX()+(Math.random()-0.5)*18; double fz = center.getZ()+(Math.random()-0.5)*18;
                        w.spawnParticle(Particle.FLAME, new Location(w, fx, center.getY()+0.2, fz), 5, 0.5, 0.2, 0.5, 0.02);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, fx, center.getY()+0.3, fz), 3, 0.5, 0.2, 0.5, 0.02);
                        w.spawnParticle(Particle.LAVA, new Location(w, fx, center.getY()+0.5, fz), 2, 0.3, 1.5, 0.3, 0); }
                }
                if (execTick % 10 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; p.damage(6.0); }
            }
            if (execTick == 60 && !shockwaved) {
                shockwaved = true; DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; p.damage(8.0); }
            }
            if (execTick > 60 && execTick <= 80) {
                double ringR = (execTick - 60) * 2.0;
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), ringR, Particle.DUST, (int)(ringR*5), new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), ringR, Particle.LAVA, (int)(ringR*2), null);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneConvergencePulse(plugin); }
    }

    // =========================================================================
    // 63. SIGIL PULSE MINOR -- single expanding ring from sigil center
    // =========================================================================
    public static class SigilPulseMinor extends EnvironmentalAttack {
        public SigilPulseMinor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sigil_pulse_minor", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(100); config.setCooldownTicks(900); config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true); config.setImpactDamage(7.0); config.setImpactRadius(1.5);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) { w(center).spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, 0), 10, 1, 0.2, 1, 0.01); DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f); } }
        private World w(Location l) { return l.getWorld(); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 30) { if (ticksAlive % 5 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, 0), 5, 0.5, 0.1, 0.5, 0.005); DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 5, 0.3, 100, 0, 200, 1.0f); w.spawnParticle(Particle.PORTAL, center.clone().add(0, 0.5, 0), 5, 0.5, 0.3, 0.5, 0.05); } return; }
            int ringTick = ticksAlive - 31;
            double radius = ringTick * 3.0; // 3 blocks/tick = fast
            if (radius <= 15) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.9, 0), radius, Particle.DUST, (int)(radius*5), new Particle.DustOptions(Color.fromRGB(100, 0, 200), 1.2f));
                DisplayBuilder.particleRing(center.clone().add(0, 0.9, 0), radius, Particle.SOUL_FIRE_FLAME, (int)(radius*3), null);
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double d = p.getLocation().distance(center); if (Math.abs(d - radius) < 1.5) p.damage(7.0); }
            }
            if (ringTick == 0) DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.2f, 0.5f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SigilPulseMinor(plugin); }
    }

    // =========================================================================
    // 64. SIGIL PULSE MAJOR -- three simultaneous rings at different speeds
    // =========================================================================
    public static class SigilPulseMajor extends EnvironmentalAttack {
        public SigilPulseMajor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sigil_pulse_major", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(160); config.setCooldownTicks(6000); config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true); config.setImpactDamage(7.0); config.setImpactRadius(1.5);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) { DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 0.8f, 0.3f); } }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 80) { if (ticksAlive % 4 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 8, 1, 1, 1, 0.01); DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 8, 0.5, 150, 0, 255, 1.2f); } if (ticksAlive % 20 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 0.7f, 0.3f); return; }
            int ringTick = ticksAlive - 81;
            double[] speeds = {4.0, 2.5, 1.5}; double[] dmg = {6.0, 7.0, 6.0};
            for (int r = 0; r < 3; r++) {
                int rStart = r * 6;
                int rTick = ringTick - rStart;
                if (rTick >= 0) {
                    double radius = rTick * speeds[r];
                    if (radius <= 15) {
                        int tint = 100 + r * 25;
                        DisplayBuilder.particleRing(center.clone().add(0, 0.9, 0), radius, Particle.DUST, (int)(radius*4), new Particle.DustOptions(Color.fromRGB(tint, 0, 200), 1.2f));
                        for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (Math.abs(p.getLocation().distance(center) - radius) < 1.5) p.damage(dmg[r]); }
                    }
                }
            }
            if (ringTick == 20) { DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.4f); w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0), 40, 0.5, 4, 0.5, 0.03); DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 40, 1.5, 150, 0, 255, 1.5f);
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(center) <= 9.0) p.damage(7.0); }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SigilPulseMajor(plugin); }
    }

    // =========================================================================
    // 65. MONOLITH ROTATION -- four corner monoliths begin spinning (visual)
    // =========================================================================
    public static class MonolithRotation extends EnvironmentalAttack {
        public MonolithRotation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_rotation", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(400); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.6f, 0.4f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double[][] corners = {{-9,-9},{9,-9},{9,9},{-9,9}};
            if (ticksAlive % 4 == 0) for (double[] c : corners) {
                Location mono = center.clone().add(c[0], 0, c[1]);
                DisplayBuilder.dustParticles(mono.clone().add(0, 3, 0), 5, 0.5, 50, 0, 100, 1.0f);
                if (ticksAlive > 60) w.spawnParticle(Particle.LAVA, mono.clone().add(0, 0.3, 0), 1, 0.3, 0.1, 0.3, 0);
            }
            if (ticksAlive % 40 == 0 && ticksAlive <= 120) DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.5f, 0.4f);
            if (ticksAlive == 120) DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 0.5f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MonolithRotation(plugin); }
    }

    // =========================================================================
    // 66. MONOLITH BEAM FIRE -- one monolith fires a sweeping END_ROD beam
    // =========================================================================
    public static class MonolithBeamFire extends EnvironmentalAttack {
        private int monolithIndex = 0; private double beamAngle;
        public MonolithBeamFire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_beam_fire", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(80); config.setCooldownTicks(700); config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) { monolithIndex = (int)(Math.random() * 4); double[][] c = {{-9,-9},{9,-9},{9,9},{-9,9}}; beamAngle = Math.atan2(-c[monolithIndex][1], -c[monolithIndex][0]); if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.8f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double[][] corners = {{-9,-9},{9,-9},{9,9},{-9,9}};
            Location mono = center.clone().add(corners[monolithIndex][0], 0, corners[monolithIndex][1]);
            if (ticksAlive <= 40) { if (ticksAlive % 3 == 0) { w.spawnParticle(Particle.END_ROD, mono.clone().add(0, 5, 0), 10, 0.3, 0.3, 0.3, 0.01); } return; }
            int sweepTick = ticksAlive - 41;
            beamAngle += 0.08; // ~4.5 deg/tick
            if (sweepTick % 2 == 0) {
                for (double d = 0; d < 20; d += 0.5) {
                    double bx = mono.getX() + Math.cos(beamAngle) * d; double bz = mono.getZ() + Math.sin(beamAngle) * d;
                    w.spawnParticle(Particle.END_ROD, new Location(w, bx, mono.getY() + 0.9, bz), 2, 0.05, 0.05, 0.05, 0.005);
                    DisplayBuilder.dustParticles(new Location(w, bx, mono.getY() + 0.9, bz), 1, 0.05, 200, 200, 255, 0.8f);
                }
            }
            if (sweepTick % 4 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue;
                double dx = p.getLocation().getX() - mono.getX(); double dz = p.getLocation().getZ() - mono.getZ();
                double pAngle = Math.atan2(dz, dx); double diff = Math.abs(pAngle - beamAngle); while (diff > Math.PI) diff = Math.abs(diff - 2*Math.PI);
                if (diff < 0.15 && Math.sqrt(dx*dx+dz*dz) <= 20 && Math.abs(p.getLocation().getY() - mono.getY() - 0.9) < 2) p.damage(6.0);
            }
            if (sweepTick % 10 == 0) DisplayBuilder.playSound(mono, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 0.5f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MonolithBeamFire(plugin); }
    }

    // =========================================================================
    // 67. SOUL TOWER AWAKENING -- towers begin firing soul projectiles
    // =========================================================================
    public static class SoulTowerAwakening extends EnvironmentalAttack {
        public SoulTowerAwakening(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_tower_awakening", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(600); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true); config.setImpactDamage(8.0); config.setImpactRadius(2.0);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) { DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 0.5f); DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.6f, 0.4f); } }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            Location[] towers = { center.clone().add(-8, 0, 6), center.clone().add(8, 0, -6) };
            if (ticksAlive <= 160) { if (ticksAlive % 6 == 0) for (Location t : towers) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, t.clone().add(0, 8, 0), 5, 0.3, 0.5, 0.3, 0.01); w.spawnParticle(Particle.PORTAL, t.clone().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.05); } return; }
            // Fire projectile every 240 ticks (12 seconds)
            if ((ticksAlive - 161) % 240 == 0) {
                for (Location t : towers) {
                    Player nearest = null; double nd = Double.MAX_VALUE;
                    for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double d = p.getLocation().distanceSquared(t); if (d < nd) { nd = d; nearest = p; } }
                    if (nearest != null) {
                        triggerImpactDamage(nearest.getLocation());
                        DisplayBuilder.playSound(t, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.7f);
                        DisplayBuilder.particleLine(t.clone().add(0, 8, 0), nearest.getLocation().clone().add(0, 1, 0), Particle.SOUL_FIRE_FLAME, 3, null);
                        DisplayBuilder.dustParticles(nearest.getLocation(), 20, 1.0, 0, 180, 255, 1.2f);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, nearest.getLocation(), 25, 1, 1, 1, 0.03);
                    }
                }
            }
            if (ticksAlive % 6 == 0) for (Location t : towers) w.spawnParticle(Particle.SOUL_FIRE_FLAME, t.clone().add(0, 8, 0), 3, 0.2, 0.3, 0.2, 0.01);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulTowerAwakening(plugin); }
    }

    // =========================================================================
    // 68. ECHO GALLERY WAIL -- sequential wail fan from gallery arches
    // =========================================================================
    public static class EchoGalleryWail extends EnvironmentalAttack {
        public EchoGalleryWail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_gallery_wail", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(120); config.setCooldownTicks(1100); config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.7f, 0.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 60) { if (ticksAlive % 5 == 0) for (int i = 0; i < 5; i++) { double ax = center.getX() - 8 + i * 4; Location arch = new Location(w, ax, center.getY(), center.getZ() + 9); w.spawnParticle(Particle.SOUL_FIRE_FLAME, arch.clone().add(0, 2, 0), 5, 0.5, 1, 0.5, 0.01); w.spawnParticle(Particle.PORTAL, arch.clone().add(0, 2, 0), 3, 0.5, 1, 0.5, 0.05); } return; }
            int wailTick = ticksAlive - 61;
            int archIndex = wailTick / 8;
            if (archIndex < 5) {
                double ax = center.getX() - 8 + archIndex * 4; Location archPos = new Location(w, ax, center.getY(), center.getZ() + 9);
                for (double d = 0; d < 8; d += 0.5) {
                    double fx = ax + (Math.random()-0.5) * (d * 0.8); double fz = center.getZ() + 9 - d;
                    Location wailPos = new Location(w, fx, center.getY() + 1, fz);
                    DisplayBuilder.dustParticles(wailPos, 3, 0.3, 100, 0, 200, 1.0f);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, wailPos, 2, 0.2, 0.2, 0.2, 0.01);
                }
                if (wailTick % 8 == 0) DisplayBuilder.playSound(archPos, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.5f);
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue;
                    double pz = p.getLocation().getZ() - center.getZ(); double px = Math.abs(p.getLocation().getX() - ax);
                    if (pz >= 1 && pz <= 9 && px <= pz * 0.5) p.damage(6.0);
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new EchoGalleryWail(plugin); }
    }

    // =========================================================================
    // 69. CRIMSON CROWN DESCENT -- floating crown descends with shockwave
    // =========================================================================
    public static class CrimsonCrownDescent extends EnvironmentalAttack {
        private boolean impacted = false;
        public CrimsonCrownDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_crown_descent", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); config.setDamageRadius(5.0); config.setDurationTicks(200); config.setCooldownTicks(1800); config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 0.6f, 0.2f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 100) { double crownY = center.getY() + 20 - (ticksAlive / 100.0) * 17; if (ticksAlive % 4 == 0) { DisplayBuilder.particleRing(center.clone().add(0, crownY - center.getY(), 0), 3.0, Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f)); DisplayBuilder.dustParticles(center.clone().add(0, crownY - center.getY(), 0), 5, 1.5, 180, 0, 0, 1.0f); } return; }
            if (!impacted) { impacted = true; DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.5f);
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), 1.0, Particle.LAVA, 20, null);
                for (double r = 0; r < 10; r += 0.5) DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r, Particle.DUST, (int)(r*5), new Particle.DustOptions(Color.fromRGB(255, 50, 0), 1.2f));
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(center) <= 64) p.damage(8.0); }
            }
            int hoverTick = ticksAlive - 101;
            if (hoverTick >= 0 && hoverTick <= 60) { if (hoverTick % 4 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 2, 0), 8, 2, 1, 2, 0.01); DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 5, 2, 180, 0, 0, 1.0f); } if (hoverTick % 10 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(center) <= 25) p.damage(6.0); } }
            if (hoverTick == 60) { DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 0.3f); DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), 8.0, Particle.DUST, 40, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f)); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(center) <= 100) p.damage(6.0); } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrimsonCrownDescent(plugin); }
    }

    // =========================================================================
    // 70. CRIMSON CROWN CRASH -- fast variant that slams to floor
    // =========================================================================
    public static class CrimsonCrownCrash extends EnvironmentalAttack {
        private boolean crashed = false;
        public CrimsonCrownCrash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_crown_crash", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(160); config.setCooldownTicks(1600); config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true); config.setImpactDamage(12.0); config.setImpactRadius(3.0);
        }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 0.6f, 0.2f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 60) { double y = center.getY() + 20 - (ticksAlive / 60.0) * 20; if (ticksAlive % 3 == 0) { DisplayBuilder.particleRing(center.clone().add(0, y - center.getY(), 0), 3.0, Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f)); DisplayBuilder.dustParticles(center.clone().add(0, y - center.getY() - 1, 0), 5, 0.5, 180, 0, 0, 1.0f); } return; }
            if (!crashed) { crashed = true; triggerImpactDamage(center); DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f); DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.4f);
                for (double r = 0; r < 12; r += 0.3) DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r, Particle.DUST, (int)(r*6), new Particle.DustOptions(Color.fromRGB(255, 80, 0), 1.5f));
                w.spawnParticle(Particle.LAVA, center, 40, 3, 0.5, 3, 0); w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0), 30, 3, 1, 3, 0.03);
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double d = p.getLocation().distance(center); if (d <= 3) p.damage(12.0); else if (d <= 8) p.damage(8.0); }
            }
            int afterTick = ticksAlive - 61;
            if (afterTick >= 0 && afterTick <= 40) { if (afterTick % 3 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 10, 2, 0.3, 2, 0.02); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(center) <= 16) p.damage(6.0); } } }
            if (afterTick == 40) { DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.3f); DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.5f);
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), 10.0, Particle.DUST, 60, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(center) <= 100) p.damage(10.0); }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrimsonCrownCrash(plugin); }
    }
}
