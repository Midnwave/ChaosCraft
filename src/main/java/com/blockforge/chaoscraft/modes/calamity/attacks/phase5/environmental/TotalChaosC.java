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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #71-80
 * CALAMITAS'S DOMAIN (#71-74) + TOTAL ENVIRONMENTAL CHAOS (#75-80)
 */
public final class TotalChaosC {

    private TotalChaosC() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadowChase(plugin));
        registry.register(new SigilCascade(plugin));
        registry.register(new ArenaRemembersHer(plugin));
        registry.register(new DomainLock(plugin));
        registry.register(new ChainSkullMaze(plugin));
        registry.register(new ChainCrownMonolith(plugin));
        registry.register(new ChainRainVeins(plugin));
        registry.register(new OverlapMazeRain(plugin));
        registry.register(new OverlapCrownSkulls(plugin));
        registry.register(new OverlapSigilMonolith(plugin));
    }

    // 71. SHADOW CHASE
    public static class ShadowChase extends EnvironmentalAttack {
        private Location shadowPos; private boolean triggered = false;
        public ShadowChase(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("shadow_chase", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(160); config.setCooldownTicks(1400); config.setTicksBetweenDamage(999); config.setDamageOnImpactOnly(true); config.setImpactDamage(10.0); config.setImpactRadius(2.0); }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return; List<Player> ps = new ArrayList<>(w.getPlayers()); ps.removeIf(this::isExempt); if (!ps.isEmpty()) shadowPos = ps.get(0).getLocation().clone(); else shadowPos = center.clone(); DisplayBuilder.playSound(shadowPos, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.6f); DisplayBuilder.dustParticles(shadowPos, 20, 1.0, 30, 0, 60, 1.0f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null || shadowPos == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 60) { if (ticksAlive % 3 == 0) DisplayBuilder.dustParticles(shadowPos.clone().add(0, 0.1, 0), 5, 1.0, 30, 0, 60, 0.8f); return; }
            List<Player> ps = new ArrayList<>(w.getPlayers()); ps.removeIf(this::isExempt);
            Player target = null; double nd = Double.MAX_VALUE; for (Player p : ps) { if (p.getLocation().distanceSquared(shadowPos) > 1) { double d = p.getLocation().distanceSquared(shadowPos); if (d < nd) { nd = d; target = p; } } }
            if (target != null && !triggered) {
                org.bukkit.util.Vector dir = target.getLocation().toVector().subtract(shadowPos.toVector()).normalize().multiply(0.2);
                shadowPos.add(dir.getX(), 0, dir.getZ());
                if (ticksAlive % 3 == 0) { DisplayBuilder.dustParticles(shadowPos.clone().add(0, 0.1, 0), 3, 0.5, 30, 0, 60, 0.8f); w.spawnParticle(Particle.PORTAL, shadowPos.clone().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0.02); }
                if (target.getLocation().distanceSquared(shadowPos) <= 1.0) {
                    triggered = true; triggerImpactDamage(shadowPos);
                    DisplayBuilder.dustParticles(shadowPos, 30, 1.5, 80, 0, 160, 1.5f);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, shadowPos.clone().add(0, 1, 0), 30, 1, 1.5, 1, 0.03);
                    DisplayBuilder.playSound(shadowPos, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.8f);
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ShadowChase(plugin); }
    }

    // 72. SIGIL CASCADE -- 10 cascading rings from sigil
    public static class SigilCascade extends EnvironmentalAttack {
        public SigilCascade(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("sigil_cascade", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(1200); config.setTicksBetweenDamage(12); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.7f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 80) { if (ticksAlive % 4 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 8, 1, 0.5, 1, 0.01); DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 8, 0.5, 150, 0, 255, 1.2f); } if (ticksAlive % 14 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.7f); return; }
            int cascTick = ticksAlive - 81;
            for (int ring = 0; ring < 10; ring++) {
                int rStart = ring * 12;
                int rTick = cascTick - rStart;
                if (rTick >= 0 && rTick <= 20) {
                    double radius = rTick * 1.5 + ring * 0.3;
                    if (radius <= 15) {
                        DisplayBuilder.particleRing(center.clone().add(0, 0.9, 0), radius, Particle.DUST, (int)(radius*4), new Particle.DustOptions(Color.fromRGB(100, 0, 200), 1.2f));
                        DisplayBuilder.particleRing(center.clone().add(0, 0.9, 0), radius, Particle.SOUL_FIRE_FLAME, (int)(radius*2), null);
                        for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (Math.abs(p.getLocation().distance(center) - radius) < 1.5) p.damage(6.0); }
                    }
                    if (rTick == 0) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f + ring * 0.05f);
                }
            }
            if (cascTick == 120) DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 0.7f, 0.4f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SigilCascade(plugin); }
    }

    // 73. ARENA REMEMBERS HER -- scripted narrative event (no damage)
    public static class ArenaRemembersHer extends EnvironmentalAttack {
        public ArenaRemembersHer(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("arena_remembers_her", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(220); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 160) {
                if (ticksAlive % 5 == 0) { for (int i = 0; i < 5; i++) { double sx = center.getX() + (Math.random()-0.5)*18; double sz = center.getZ() + (Math.random()-0.5)*18; DisplayBuilder.dustParticles(new Location(w, sx, center.getY()+3, sz), 3, 0.5, 100, 0, 150, 0.8f); } }
                if (ticksAlive == 60) DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.3f);
                if (ticksAlive == 120) DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.3f);
                return;
            }
            if (ticksAlive == 161) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 0.6f, 0.2f);
                for (Player p : w.getPlayers()) p.sendTitle("", "\u00a75This place was always hers.", 10, 40, 10);
            }
            if (ticksAlive == 165) {
                for (double x = -10; x <= 10; x += 1) for (double z = -10; z <= 10; z += 1) DisplayBuilder.dustParticles(center.clone().add(x, 0.5, z), 1, 0.05, 180, 0, 180, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0.7f);
            }
            if (ticksAlive > 165 && ticksAlive % 8 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 5, 1, 0.2, 1, 0.005); }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ArenaRemembersHer(plugin); }
    }

    // 74. DOMAIN LOCK -- Phase 4 entry; everything at 140%
    public static class DomainLock extends EnvironmentalAttack {
        private boolean locked = false;
        public DomainLock(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("domain_lock", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(100); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); config.setDamageOnImpactOnly(true); config.setImpactDamage(10.0); config.setImpactRadius(10.0); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.1f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 60) { if (ticksAlive % 6 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 15, 1.5, 0.3, 1.5, 0.02); return; }
            if (!locked) {
                locked = true; triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f); DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.4f);
                for (double r = 0; r < 12; r += 0.3) DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r, Particle.DUST, (int)(r*8), new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 80, 5, 2, 5, 0.05);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DomainLock(plugin); }
    }

    // 75. CHAIN: SKULLS TRIGGER MAZE
    public static class ChainSkullMaze extends EnvironmentalAttack {
        public ChainSkullMaze(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("chain_skull_maze", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(15); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) { DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.6f); DisplayBuilder.particleRing(center.clone().add(0, 0.9, 0), 3, Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(100, 0, 200), 1.2f)); } }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 10) { double r = ticksAlive * 3.0; DisplayBuilder.particleRing(center.clone().add(0, 0.9, 0), r, Particle.DUST, (int)(r*5), new Particle.DustOptions(Color.fromRGB(100, 0, 200), 1.2f)); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (Math.abs(p.getLocation().distance(center) - r) < 1.5) p.damage(7.0); } return; }
            if (ticksAlive > 10 && ticksAlive <= 70) { if (ticksAlive % 4 == 0) { double[][] segs = {{-5,-6,-5,2},{5,-6,5,2},{-5,2,5,2}}; for (double[] seg : segs) { double steps = Math.max(Math.abs(seg[2]-seg[0]), Math.abs(seg[3]-seg[1])); for (double t = 0; t <= 1; t += 1.0/Math.max(1,steps)) { double x = seg[0]+(seg[2]-seg[0])*t; double z = seg[1]+(seg[3]-seg[1])*t; w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, 0.2, z), 2, 0.1, 0.1, 0.1, 0.005); } } } }
            if (ticksAlive > 70 && ticksAlive <= 200) { if (ticksAlive % 3 == 0) { double[][] segs = {{-5,-6,-5,2},{5,-6,5,2},{-5,2,5,2}}; for (double[] seg : segs) { double steps = Math.max(Math.abs(seg[2]-seg[0]), Math.abs(seg[3]-seg[1])); for (double t = 0; t <= 1; t += 1.0/Math.max(1,steps)) { double x = seg[0]+(seg[2]-seg[0])*t; double z = seg[1]+(seg[3]-seg[1])*t; for (double y = 0; y < 3; y += 1.0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, y, z), 2, 0.1, 0.2, 0.1, 0.005); } }
                if (ticksAlive % 15 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; for (double[] seg : segs) { double px = p.getLocation().getX()-center.getX(); double pz = p.getLocation().getZ()-center.getZ(); double sdx = seg[2]-seg[0]; double sdz = seg[3]-seg[1]; double sl = Math.sqrt(sdx*sdx+sdz*sdz); if (sl < 0.1) continue; double tt = Math.max(0, Math.min(1, ((px-seg[0])*sdx+(pz-seg[1])*sdz)/(sl*sl))); double cx = seg[0]+sdx*tt; double cz = seg[1]+sdz*tt; if (Math.sqrt((px-cx)*(px-cx)+(pz-cz)*(pz-cz)) <= 1.0) { p.damage(6.0); break; } } }
            } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ChainSkullMaze(plugin); }
    }

    // 76. CHAIN: CROWN TRIGGERS ALL MONOLITH BEAMS
    public static class ChainCrownMonolith extends EnvironmentalAttack {
        private final double[] beamAngles = new double[4];
        public ChainCrownMonolith(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("chain_crown_monolith", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(60); config.setCooldownTicks(99999); config.setTicksBetweenDamage(10); }
        @Override protected void onSpawn(Location center) { double[][] c = {{-9,-9},{9,-9},{9,9},{-9,9}}; for (int i = 0; i < 4; i++) beamAngles[i] = Math.atan2(-c[i][1], -c[i][0]); if (center.getWorld() != null) { for (int i = 0; i < 4; i++) DisplayBuilder.playSound(center.clone().add(c[i][0],0,c[i][1]), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.8f); } }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double[][] corners = {{-9,-9},{9,-9},{9,9},{-9,9}};
            for (int m = 0; m < 4; m++) { beamAngles[m] += 0.06; Location mono = center.clone().add(corners[m][0], 0, corners[m][1]);
                if (ticksAlive % 2 == 0) for (double d = 0; d < 20; d += 0.8) { double bx = mono.getX()+Math.cos(beamAngles[m])*d; double bz = mono.getZ()+Math.sin(beamAngles[m])*d; w.spawnParticle(Particle.END_ROD, new Location(w, bx, mono.getY()+0.9, bz), 2, 0.05, 0.05, 0.05, 0.005); }
                if (ticksAlive % 4 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double dx = p.getLocation().getX()-mono.getX(); double dz = p.getLocation().getZ()-mono.getZ(); double pa = Math.atan2(dz, dx); double diff = Math.abs(pa - beamAngles[m]); while (diff > Math.PI) diff = Math.abs(diff - 2*Math.PI); if (diff < 0.15 && Math.sqrt(dx*dx+dz*dz) <= 20) p.damage(6.0); }
            }
            if (ticksAlive % 10 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 0.5f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ChainCrownMonolith(plugin); }
    }

    // 77. CHAIN: RAIN FEEDS VEINS
    public static class ChainRainVeins extends EnvironmentalAttack {
        public ChainRainVeins(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("chain_rain_veins", AttackType.ENVIRONMENTAL, 5)); config.setDamage(7.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(10); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            for (int i = 0; i < 8; i++) { double rx = center.getX()+(Math.random()-0.5)*20; double rz = center.getZ()+(Math.random()-0.5)*20; double ry = center.getY()+40-(ticksAlive%30)*1.33;
                if (ry <= center.getY()+0.5) { Location splat = new Location(w, rx, center.getY()+0.2, rz); boolean onVein = Math.random() < 0.3;
                    if (onVein) { for (double y = 0; y < 5; y += 0.5) w.spawnParticle(Particle.LAVA, splat.clone().add(0, y, 0), 3, 0.2, 0.1, 0.2, 0); DisplayBuilder.dustParticles(splat, 15, 0.5, 255, 50, 0, 1.2f); DisplayBuilder.playSound(splat, Sound.BLOCK_LAVA_POP, 0.4f, 0.6f); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(splat) <= 2.25) p.damage(7.0); }
                    } else { w.spawnParticle(Particle.SOUL_FIRE_FLAME, splat, 15, 1.5, 0.2, 1.5, 0.02); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(splat) <= 9.0) p.damage(6.0); } }
                } else w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, rx, ry, rz), 1, 0, 0, 0, 0);
            }
            if (ticksAlive % 20 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_FALL, 0.5f, 0.5f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ChainRainVeins(plugin); }
    }

    // 78. OVERLAP: MAZE AND RAIN
    public static class OverlapMazeRain extends EnvironmentalAttack {
        public OverlapMazeRain(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("overlap_maze_rain", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(10); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.7f, 0.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Maze walls
            if (ticksAlive > 100 && ticksAlive % 3 == 0) { double[][] segs = {{-5,-6,-5,2},{5,-6,5,2},{-5,2,5,2},{0,5,5,5}}; for (double[] seg : segs) { double steps = Math.max(Math.abs(seg[2]-seg[0]), Math.abs(seg[3]-seg[1])); for (double t = 0; t <= 1; t += 1.0/Math.max(1,steps)) { double x = seg[0]+(seg[2]-seg[0])*t; double z = seg[1]+(seg[3]-seg[1])*t; for (double y = 0; y < 3; y += 1.0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, y, z), 2, 0.1, 0.2, 0.1, 0.005); } } }
            // Rain
            if (ticksAlive > 60) for (int i = 0; i < 12; i++) { double rx = center.getX()+(Math.random()-0.5)*20; double rz = center.getZ()+(Math.random()-0.5)*20; double ry = center.getY()+40-(ticksAlive%30)*1.33; if (ry <= center.getY()+0.5) { Location splat = new Location(w, rx, center.getY()+0.2, rz); w.spawnParticle(Particle.SOUL_FIRE_FLAME, splat, 15, 2.5, 0.2, 2.5, 0.02); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(splat) <= 6.25) p.damage(7.0); } } else w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, rx, ry, rz), 1, 0, 0, 0, 0); }
            if (ticksAlive % 10 == 0 && ticksAlive > 100) { double[][] segs = {{-5,-6,-5,2},{5,-6,5,2},{-5,2,5,2},{0,5,5,5}}; for (Player p : w.getPlayers()) { if (isExempt(p)) continue; for (double[] seg : segs) { double px = p.getLocation().getX()-center.getX(); double pz = p.getLocation().getZ()-center.getZ(); double sdx = seg[2]-seg[0]; double sdz = seg[3]-seg[1]; double sl = Math.sqrt(sdx*sdx+sdz*sdz); if (sl < 0.1) continue; double tt = Math.max(0, Math.min(1, ((px-seg[0])*sdx+(pz-seg[1])*sdz)/(sl*sl))); double cx = seg[0]+sdx*tt; double cz = seg[1]+sdz*tt; if (Math.sqrt((px-cx)*(px-cx)+(pz-cz)*(pz-cz)) <= 1.0) { p.damage(6.0); break; } } } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new OverlapMazeRain(plugin); }
    }

    // 79. OVERLAP: CROWN AND SKULLS
    public static class OverlapCrownSkulls extends EnvironmentalAttack {
        private boolean impacted = false;
        public OverlapCrownSkulls(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("overlap_crown_skulls", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(160); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); config.setDamageOnImpactOnly(true); config.setImpactDamage(12.0); config.setImpactRadius(5.0); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 0.6f, 0.2f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 50) { double cy = center.getY() + 20 - (ticksAlive/50.0)*20; if (ticksAlive % 3 == 0) DisplayBuilder.particleRing(center.clone().add(0, cy-center.getY(), 0), 3.0, Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f)); for (int i = 0; i < 5; i++) { double sx = center.getX()+(Math.random()-0.5)*14; double sz = center.getZ()+(Math.random()-0.5)*14; if (ticksAlive % 5 == 0) DisplayBuilder.dustParticles(new Location(w, sx, center.getY()+(ticksAlive/50.0)*2, sz), 5, 0.3, 200, 0, 0, 1.0f); } return; }
            if (!impacted) { impacted = true; triggerImpactDamage(center); DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);
                for (double r = 0; r < 10; r += 0.3) DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r, Particle.DUST, (int)(r*6), new Particle.DustOptions(Color.fromRGB(255, 80, 0), 1.5f));
                for (int i = 0; i < 5; i++) { double angle = (2*Math.PI*i)/5; for (double d = 0; d < 10; d += 0.5) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(Math.cos(angle)*d, 1, Math.sin(angle)*d), 3, 0.2, 0.3, 0.2, 0.02); }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.6f);
                List<Player> ps = new ArrayList<>(w.getPlayers()); ps.removeIf(this::isExempt); Player furthest = null; double fd = 0;
                for (Player p : ps) { double d = p.getLocation().distanceSquared(center); if (d > fd) { fd = d; furthest = p; } }
                if (furthest != null) { for (int i = 0; i < 5; i++) { triggerImpactDamage(furthest.getLocation()); DisplayBuilder.playSound(furthest.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.7f); w.spawnParticle(Particle.EXPLOSION, furthest.getLocation(), 1, 0, 0, 0, 0); } }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new OverlapCrownSkulls(plugin); }
    }

    // 80. OVERLAP: SIGIL CASCADE AND MONOLITH BEAMS
    public static class OverlapSigilMonolith extends EnvironmentalAttack {
        private final double[] beamAngles = new double[4];
        public OverlapSigilMonolith(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("overlap_sigil_monolith", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(10); }
        @Override protected void onSpawn(Location center) { double[][] c = {{-9,-9},{9,-9},{9,9},{-9,9}}; for (int i = 0; i < 4; i++) beamAngles[i] = Math.atan2(-c[i][1], -c[i][0]); if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.7f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 80) { if (ticksAlive % 4 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 8, 1, 0.5, 1, 0.01); double[][] corners = {{-9,-9},{9,-9},{9,9},{-9,9}}; for (double[] c : corners) w.spawnParticle(Particle.END_ROD, center.clone().add(c[0], 5, c[1]), 5, 0.3, 0.3, 0.3, 0.01); } return; }
            int execTick = ticksAlive - 81;
            // Cascade rings
            for (int ring = 0; ring < 10; ring++) { int rStart = ring * 12; int rTick = execTick - rStart; if (rTick >= 0 && rTick <= 20) { double radius = rTick * 1.5 + ring * 0.3; if (radius <= 15) { DisplayBuilder.particleRing(center.clone().add(0, 0.9, 0), radius, Particle.DUST, (int)(radius*4), new Particle.DustOptions(Color.fromRGB(100, 0, 200), 1.2f)); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (Math.abs(p.getLocation().distance(center) - radius) < 1.5) p.damage(6.0); } } } }
            // Monolith beams
            double[][] corners = {{-9,-9},{9,-9},{9,9},{-9,9}};
            for (int m = 0; m < 4; m++) { beamAngles[m] += 0.05; Location mono = center.clone().add(corners[m][0], 0, corners[m][1]); if (execTick % 2 == 0) for (double d = 0; d < 18; d += 0.8) { double bx = mono.getX()+Math.cos(beamAngles[m])*d; double bz = mono.getZ()+Math.sin(beamAngles[m])*d; w.spawnParticle(Particle.END_ROD, new Location(w, bx, mono.getY()+0.9, bz), 1, 0.05, 0.05, 0.05, 0.005); }
                if (execTick % 4 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double dx = p.getLocation().getX()-mono.getX(); double dz = p.getLocation().getZ()-mono.getZ(); double pa = Math.atan2(dz, dx); double diff = Math.abs(pa - beamAngles[m]); while (diff > Math.PI) diff = Math.abs(diff - 2*Math.PI); if (diff < 0.15 && Math.sqrt(dx*dx+dz*dz) <= 18) p.damage(6.0); }
            }
            if (execTick % 12 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new OverlapSigilMonolith(plugin); }
    }
}
