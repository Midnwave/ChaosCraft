package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E Environmental -- FINAL CALAMITY C
 * Attacks #171-180: South/East/West monolith falls, final compression,
 * sigil activation, ground heave, mass soul eruption, echo gallery rush,
 * soul vortex, soul memory flashes.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 14.0-28.0 HP (escalating finale)
 * - Arena dissolution + Soul Reckoning -- the dead assert themselves
 */
public final class FinalCalamityC {

    private FinalCalamityC() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SouthMonolithFall(plugin));
        registry.register(new EastMonolithFall(plugin));
        registry.register(new WestMonolithFall(plugin));
        registry.register(new FinalCompression(plugin));
        registry.register(new SigilGroundActivation(plugin));
        registry.register(new GroundHeave(plugin));
        registry.register(new MassSoulEruption(plugin));
        registry.register(new EchoGalleryRush(plugin));
        registry.register(new SoulVortex(plugin));
        registry.register(new SoulMemoryFlashes(plugin));
    }

    // =========================================================================
    // 171-173. MONOLITH FALLS (South, East, West) -- identical to North (#170)
    // =========================================================================
    public static class SouthMonolithFall extends EnvironmentalAttack {
        private Location base; private boolean fallen = false;
        public SouthMonolithFall(ChaosCraftPlugin p) { super(p, new AttackConfig("south_monolith_fall", AttackType.ENVIRONMENTAL, 5)); config.setDamage(16.0); config.setDamageRadius(3.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) { base = c.clone().add(0,0,10); }
        @Override protected void onTick(int t) { monolithFallTick(t, base, 0, -7, fallen, f -> fallen = f); }
        private void monolithFallTick(int t, Location b, double dX, double dZ, boolean f, java.util.function.Consumer<Boolean> setF) {
            if (b == null) return; World w = b.getWorld(); if (w == null) return;
            if (t <= 80) { if (t%3==0) { DisplayBuilder.dustParticles(b.clone().add(0,7,0),20,0.5,200,150,80,1.5f); w.spawnParticle(Particle.BLOCK,b.clone().add(0,5,0),8,0.3,2.0,0.3,0,Material.STONE.createBlockData()); } if (t==70) DisplayBuilder.playSound(b,Sound.BLOCK_STONE_BREAK,0.7f,0.3f); return; }
            if (!f && t > 120) { setF.accept(true); Location imp = b.clone().add(dX,0,dZ); DisplayBuilder.playSound(imp,Sound.ENTITY_GENERIC_EXPLODE,1.0f,0.4f); w.spawnParticle(Particle.BLOCK,imp,100,2.0,1.0,2.0,0,Material.STONE.createBlockData()); w.spawnParticle(Particle.SMOKE,imp.clone().add(0,1,0),50,2.0,1.0,2.0,0.03); for (Player p:w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(imp)<=9.0) p.damage(16.0); } }
            if (t>120 && t%10==0) w.spawnParticle(Particle.SMOKE, b.clone().add(dX,1,dZ), 5, 3.0,0.5,1.0, 0.01);
        }
        @Override protected void onCleanup() {} @Override public AbstractAttack newInstance() { return new SouthMonolithFall(plugin); }
    }

    public static class EastMonolithFall extends EnvironmentalAttack {
        private Location base; private boolean fallen = false;
        public EastMonolithFall(ChaosCraftPlugin p) { super(p, new AttackConfig("east_monolith_fall", AttackType.ENVIRONMENTAL, 5)); config.setDamage(16.0); config.setDamageRadius(3.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) { base = c.clone().add(10,0,0); }
        @Override protected void onTick(int t) {
            if (base == null) return; World w = base.getWorld(); if (w == null) return;
            if (t <= 80) { if (t%3==0) { DisplayBuilder.dustParticles(base.clone().add(0,7,0),20,0.5,200,150,80,1.5f); } if (t==70) DisplayBuilder.playSound(base,Sound.BLOCK_STONE_BREAK,0.7f,0.3f); return; }
            if (!fallen && t > 120) { fallen = true; Location imp = base.clone().add(-7,0,0); DisplayBuilder.playSound(imp,Sound.ENTITY_GENERIC_EXPLODE,1.0f,0.4f); w.spawnParticle(Particle.BLOCK,imp,100,2.0,1.0,2.0,0,Material.STONE.createBlockData()); for (Player p:w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(imp)<=9.0) p.damage(16.0); } }
        }
        @Override protected void onCleanup() {} @Override public AbstractAttack newInstance() { return new EastMonolithFall(plugin); }
    }

    public static class WestMonolithFall extends EnvironmentalAttack {
        private Location base; private boolean fallen = false;
        public WestMonolithFall(ChaosCraftPlugin p) { super(p, new AttackConfig("west_monolith_fall", AttackType.ENVIRONMENTAL, 5)); config.setDamage(16.0); config.setDamageRadius(3.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) { base = c.clone().add(-10,0,0); }
        @Override protected void onTick(int t) {
            if (base == null) return; World w = base.getWorld(); if (w == null) return;
            if (t <= 80) { if (t%3==0) DisplayBuilder.dustParticles(base.clone().add(0,7,0),20,0.5,200,150,80,1.5f); if (t==70) DisplayBuilder.playSound(base,Sound.BLOCK_STONE_BREAK,0.7f,0.3f); return; }
            if (!fallen && t > 120) { fallen = true; Location imp = base.clone().add(7,0,0); DisplayBuilder.playSound(imp,Sound.ENTITY_GENERIC_EXPLODE,1.0f,0.4f); w.spawnParticle(Particle.BLOCK,imp,100,2.0,1.0,2.0,0,Material.STONE.createBlockData()); for (Player p:w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(imp)<=9.0) p.damage(16.0); } }
        }
        @Override protected void onCleanup() {} @Override public AbstractAttack newInstance() { return new WestMonolithFall(plugin); }
    }

    // =========================================================================
    // 174. FINAL COMPRESSION -- arena shrinks from 20x20 to 16x16
    // =========================================================================
    public static class FinalCompression extends EnvironmentalAttack {
        private boolean compressed = false;
        public FinalCompression(ChaosCraftPlugin p) { super(p, new AttackConfig("final_compression", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(180); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (t <= 120) {
                double shrink = t * (2.0/120.0);
                if (t % 3 == 0) {
                    for (int i = -10; i <= 10; i += 2) {
                        DisplayBuilder.dustParticles(center.clone().add(-10+shrink, 0.3, i), 4, 0.3, 0, 0, 0, 2.0f);
                        DisplayBuilder.dustParticles(center.clone().add(10-shrink, 0.3, i), 4, 0.3, 0, 0, 0, 2.0f);
                        DisplayBuilder.dustParticles(center.clone().add(i, 0.3, -10+shrink), 4, 0.3, 0, 0, 0, 2.0f);
                        DisplayBuilder.dustParticles(center.clone().add(i, 0.3, 10-shrink), 4, 0.3, 0, 0, 0, 2.0f);
                    }
                }
                if (t % 4 == 0) DisplayBuilder.playSound(center.clone().add(0,0,(Math.random()-0.5)*20), Sound.BLOCK_STONE_BREAK, 0.5f, 0.5f);
                return;
            }
            if (!compressed) {
                compressed = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.2f);
                // New void edge portal particles
                for (int i = -8; i <= 8; i++) {
                    w.spawnParticle(Particle.PORTAL, center.clone().add(-8, 0.5, i), 15, 0.3, 0.5, 0.3, 0.01);
                    w.spawnParticle(Particle.PORTAL, center.clone().add(8, 0.5, i), 15, 0.3, 0.5, 0.3, 0.01);
                    w.spawnParticle(Particle.PORTAL, center.clone().add(i, 0.5, -8), 15, 0.3, 0.5, 0.3, 0.01);
                    w.spawnParticle(Particle.PORTAL, center.clone().add(i, 0.5, 8), 15, 0.3, 0.5, 0.3, 0.01);
                }
            }
            // Persistent new edge particles
            if (t % 6 == 0) {
                for (int i = -8; i <= 8; i += 3) {
                    w.spawnParticle(Particle.PORTAL, center.clone().add(-8, 0.5, i), 4, 0.3, 0.3, 0.3, 0.005);
                    w.spawnParticle(Particle.PORTAL, center.clone().add(8, 0.5, i), 4, 0.3, 0.3, 0.3, 0.005);
                }
            }
        }
        @Override protected void onCleanup() {} @Override public AbstractAttack newInstance() { return new FinalCompression(plugin); }
    }

    // =========================================================================
    // 175. SIGIL GROUND ACTIVATION -- crying obsidian circuit lights up
    // =========================================================================
    public static class SigilGroundActivation extends EnvironmentalAttack {
        public SigilGroundActivation(ChaosCraftPlugin p) { super(p, new AttackConfig("sigil_ground_activation", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(400); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Sigil pattern pulses
            if (t % 4 == 0) {
                double pulseProgress = (t % 80) / 80.0;
                for (int i = 0; i < 12; i++) {
                    double a = (2*Math.PI*i)/12;
                    double d = 3 + pulseProgress * 5;
                    Location nodePos = center.clone().add(Math.cos(a)*d, 0.2, Math.sin(a)*d);
                    w.spawnParticle(Particle.DUST, nodePos, 6, 0.2, 0.1, 0.2, 0, new Particle.DustOptions(Color.fromRGB(255, 0, 200), 3.0f));
                    w.spawnParticle(Particle.REVERSE_PORTAL, nodePos, 4, 0.2, 0.3, 0.2, 0.01);
                }
            }
            if (t == 5) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.4f, 0.5f);
            if (t % 160 == 0 && t > 0) DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.2f, 0.3f);
        }
        @Override protected void onCleanup() {} @Override public AbstractAttack newInstance() { return new SigilGroundActivation(plugin); }
    }

    // =========================================================================
    // 176. GROUND HEAVE -- arena floor buckles permanently
    // =========================================================================
    public static class GroundHeave extends EnvironmentalAttack {
        private boolean heaved = false;
        public GroundHeave(ChaosCraftPlugin p) { super(p, new AttackConfig("ground_heave", AttackType.ENVIRONMENTAL, 5)); config.setDamage(14.0); config.setDurationTicks(160); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Vibration buildup: 80 ticks
            if (t <= 80) {
                double amp = t <= 40 ? 0.05 : 0.15;
                if (t % 3 == 0) {
                    for (int x = -7; x <= 7; x += 3) for (int z = -7; z <= 7; z += 3) {
                        w.spawnParticle(Particle.SMOKE, center.clone().add(x, 0.3 + Math.random()*amp, z), 2, 0.2, 0.1, 0.2, 0.005);
                    }
                }
                return;
            }
            if (!heaved) {
                heaved = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);
                // Massive floor particle explosion
                for (int x = -8; x <= 8; x += 2) for (int z = -8; z <= 8; z += 2) {
                    w.spawnParticle(Particle.BLOCK, center.clone().add(x, 0.5, z), 20, 0.5, 0.5, 0.5, 0, Material.END_STONE.createBlockData());
                    w.spawnParticle(Particle.SMOKE, center.clone().add(x, 0.5, z), 5, 0.3, 0.3, 0.3, 0.02);
                }
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 100.0 && p.getLocation().getY() <= center.getY()+2) p.damage(14.0);
                }
            }
            // Settling sounds
            int heaveTick = t - 80;
            if (heaveTick % 20 == 0 && heaveTick <= 200) DisplayBuilder.playSound(center.clone().add((Math.random()-0.5)*12, 0, (Math.random()-0.5)*12), Sound.BLOCK_STONE_STEP, 0.4f, 0.2f);
        }
        @Override protected void onCleanup() {} @Override public AbstractAttack newInstance() { return new GroundHeave(plugin); }
    }

    // =========================================================================
    // 177. MASS SOUL ERUPTION -- 50 soul fire columns
    // =========================================================================
    public static class MassSoulEruption extends EnvironmentalAttack {
        private final List<Location> columns = new ArrayList<>(); private boolean erupted = false;
        public MassSoulEruption(ChaosCraftPlugin p) { super(p, new AttackConfig("mass_soul_eruption", AttackType.ENVIRONMENTAL, 5)); config.setDamage(16.0); config.setDamageRadius(0.5); config.setDurationTicks(200); config.setCooldownTicks(1500); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location c) { for (int i = 0; i < 50; i++) columns.add(c.clone().add((Math.random()-0.5)*16, 0, (Math.random()-0.5)*16)); }
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Buildup: 60 ticks -- floor cools to purple
            if (t <= 60) {
                if (t % 4 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add((Math.random()-0.5)*16, 0.3, (Math.random()-0.5)*16), 5, 1.0, 0.1, 1.0, 0.005);
                // 10 marked positions
                if (t == 50) { for (int i = 0; i < 10; i++) DisplayBuilder.particleRing(columns.get(i).clone().add(0,0.3,0), 0.5, Particle.DUST, 6, new Particle.DustOptions(Color.fromRGB(50,200,200), 1.5f)); }
                return;
            }
            if (!erupted) {
                erupted = true;
                // 50 simultaneous sounds
                for (int i = 0; i < 5; i++) DisplayBuilder.playSound(center.clone().add((Math.random()-0.5)*10,0,(Math.random()-0.5)*10), Sound.BLOCK_SOUL_SAND_STEP, 1.0f, 0.2f);
            }
            int eruptTick = t - 60;
            // Columns active for 100 ticks then vanish in 1 tick
            if (eruptTick <= 100) {
                if (eruptTick % 3 == 0) {
                    for (Location col : columns) {
                        for (int y = 0; y <= 12; y += 3) w.spawnParticle(Particle.SOUL_FIRE_FLAME, col.clone().add(0, y, 0), 12, 0.3, 0.5, 0.3, 0.01);
                        w.spawnParticle(Particle.REVERSE_PORTAL, col.clone().add(0, 12, 0), 3, 0.2, 0.3, 0.2, 0.005);
                    }
                }
                if (eruptTick % 20 == 0) {
                    for (Location col : columns) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double dx=p.getLocation().getX()-col.getX(); double dz=p.getLocation().getZ()-col.getZ(); if (dx*dx+dz*dz<=0.25) p.damage(16.0); }
                }
            }
            // Aftermath: lingering soul fire
            if (eruptTick > 100 && eruptTick % 6 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add((Math.random()-0.5)*16, 0.3, (Math.random()-0.5)*16), 5, 2.0, 0.1, 2.0, 0.005);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new MassSoulEruption(plugin); }
    }

    // =========================================================================
    // 178. ECHO GALLERY RUSH -- 7 spectral figures charge players
    // =========================================================================
    public static class EchoGalleryRush extends EnvironmentalAttack {
        private final List<Location> figurePositions = new ArrayList<>(); private boolean rushing = false;
        public EchoGalleryRush(ChaosCraftPlugin p) { super(p, new AttackConfig("echo_gallery_rush", AttackType.ENVIRONMENTAL, 5)); config.setDamage(20.0); config.setDamageRadius(1.5); config.setDurationTicks(240); config.setCooldownTicks(1800); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) { for (int i = 0; i < 7; i++) { double a = (2*Math.PI*i)/7; figurePositions.add(c.clone().add(Math.cos(a)*10, 0, Math.sin(a)*10)); } }
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Advance: 80 ticks -- figures step inward
            if (t <= 80) {
                double advance = t <= 40 ? t * 0.025 : (t <= 60 ? 1.0 + (t-40)*0.1 : 3.0);
                if (t % 4 == 0) for (Location fig : figurePositions) {
                    double dx = center.getX()-fig.getX(); double dz = center.getZ()-fig.getZ(); double dist = Math.sqrt(dx*dx+dz*dz);
                    if (dist > 0) { Location figPos = fig.clone().add((dx/dist)*advance, 1.0, (dz/dist)*advance); DisplayBuilder.dustParticles(figPos, 6, 0.3, 180, 180, 255, 1.5f); w.spawnParticle(Particle.END_ROD, figPos.clone().add(0, 0.5, 0), 3, 0.1, 0.1, 0.1, 0.005); }
                }
                if (t == 70) { /* 10-tick stillness */ }
                return;
            }
            // Rush: 20 ticks -- figures charge nearest player
            if (!rushing) { rushing = true; DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.8f, 0.6f); }
            int rushTick = t - 80;
            if (rushTick <= 20) {
                double rushDist = rushTick * 1.5;
                for (Location fig : figurePositions) {
                    double dx = center.getX()-fig.getX(); double dz = center.getZ()-fig.getZ(); double dist = Math.sqrt(dx*dx+dz*dz);
                    if (dist > 0) {
                        Location rushPos = fig.clone().add((dx/dist)*(3+rushDist), 1.0, (dz/dist)*(3+rushDist));
                        if (rushTick % 2 == 0) { DisplayBuilder.dustParticles(rushPos, 20, 0.3, 180, 180, 255, 1.5f); w.spawnParticle(Particle.END_ROD, rushPos.clone().add(0, 0.5, 0), 3, 0.1, 0.1, 0.1, 0.01); }
                        if (rushTick % 5 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(rushPos) <= 2.25) { p.damage(20.0); DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 1.2f); } }
                    }
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new EchoGalleryRush(plugin); }
    }

    // =========================================================================
    // 179. SOUL VORTEX -- 8-block pulling center
    // =========================================================================
    public static class SoulVortex extends EnvironmentalAttack {
        private boolean vortexActive = false;
        public SoulVortex(ChaosCraftPlugin p) { super(p, new AttackConfig("soul_vortex", AttackType.ENVIRONMENTAL, 5)); config.setDamage(14.0); config.setDamageRadius(2.0); config.setDurationTicks(600); config.setCooldownTicks(2000); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Buildup: 80 ticks -- dark spiral
            if (t <= 80) {
                if (t % 3 == 0) {
                    w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 2, 0), 20, 3.0, 1.0, 3.0, 0.02);
                    if (t > 40) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 2, 0), 30, 3.0, 1.0, 3.0, 0.01);
                    if (t > 60) w.spawnParticle(Particle.PORTAL, center.clone().add(0, 2, 0), 40, 2.0, 1.0, 2.0, 0.03);
                }
                return;
            }
            if (!vortexActive) { vortexActive = true; DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 0.4f, 0.2f); }
            // Vortex active: continuous pull for 520 ticks
            int vTick = t - 80;
            // Pull force
            if (vTick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(Math.pow(p.getLocation().getX()-center.getX(),2)+Math.pow(p.getLocation().getZ()-center.getZ(),2));
                    if (dist <= 8.0 && dist > 0.5) {
                        double dx = center.getX()-p.getLocation().getX(); double dz = center.getZ()-p.getLocation().getZ();
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(dx/dist*0.15, 0, dz/dist*0.15)));
                    }
                    if (dist <= 2.0) p.damage(14.0);
                }
            }
            // Visual
            if (vTick % 2 == 0) {
                w.spawnParticle(Particle.PORTAL, center.clone().add(0, 2, 0), 60, 4.0, 1.0, 4.0, 0.03);
                for (int r = 1; r <= 8; r += 2) DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r, Particle.SOUL_FIRE_FLAME, r*2, null);
            }
            if (vTick % 60 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 0.1f, 0.2f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SoulVortex(plugin); }
    }

    // =========================================================================
    // 180. SOUL MEMORY FLASHES -- prior boss death ghosts appear briefly
    // =========================================================================
    public static class SoulMemoryFlashes extends EnvironmentalAttack {
        private int flashType = 0;
        public SoulMemoryFlashes(ChaosCraftPlugin p) { super(p, new AttackConfig("soul_memory_flashes", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(120); config.setCooldownTicks(400); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) { flashType = (int)(Math.random()*4); }
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            Location flashPos = center.clone().add((Math.random()-0.5)*12, 0, (Math.random()-0.5)*12);
            if (t == 1) DisplayBuilder.playSound(flashPos, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.5f);
            if (t <= 60 && t % 4 == 0) {
                switch (flashType) {
                    case 0: DisplayBuilder.dustParticles(flashPos.clone().add(0,1,0), 15, 1.0, 0, 0, 80, 1.5f); w.spawnParticle(Particle.CRIT, flashPos.clone().add(0,1,0), 8, 0.5, 0.5, 0.5, 0.03); break;
                    case 1: DisplayBuilder.dustParticles(flashPos.clone().add(0,1,0), 15, 1.0, 180, 100, 255, 1.5f); w.spawnParticle(Particle.ENCHANTED_HIT, flashPos.clone().add(0,1,0), 8, 0.5, 0.5, 0.5, 0.03); break;
                    case 2: DisplayBuilder.dustParticles(flashPos.clone().add(0,1,0), 15, 1.0, 255, 80, 0, 1.5f); w.spawnParticle(Particle.SOUL_FIRE_FLAME, flashPos.clone().add(0,1,0), 5, 0.3, 0.3, 0.3, 0.01); w.spawnParticle(Particle.LAVA, flashPos.clone().add(0,1,0), 3, 0.3, 0.3, 0.3, 0); break;
                    default: for (int i = 0; i < 3; i++) { Location sPos = flashPos.clone().add((Math.random()-0.5)*2, 0.5+Math.random()*2, (Math.random()-0.5)*2); w.spawnParticle(Particle.END_ROD, sPos, 3, 0.1, 0.1, 0.1, 0.002); w.spawnParticle(Particle.WITCH, sPos, 2, 0.1, 0.1, 0.1, 0); } break;
                }
            }
            if (t == 1) {
                switch (flashType) {
                    case 0: DisplayBuilder.playSound(flashPos, Sound.ENTITY_GUARDIAN_DEATH, 0.2f, 0.3f); break;
                    case 1: DisplayBuilder.playSound(flashPos, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.2f, 0.2f); break;
                    case 2: DisplayBuilder.playSound(flashPos, Sound.ENTITY_BLAZE_DEATH, 0.2f, 0.3f); break;
                    default: DisplayBuilder.playSound(flashPos, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.2f, 0.4f); break;
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SoulMemoryFlashes(plugin); }
    }
}
