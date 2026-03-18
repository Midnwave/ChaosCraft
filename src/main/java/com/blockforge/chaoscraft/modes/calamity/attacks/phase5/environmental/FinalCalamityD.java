package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;

/**
 * Phase 4E Environmental -- FINAL CALAMITY D
 * Attacks #181-190: Sigil beams, soul choir, vortex intensification, echo figure
 * ring formation, soul ground, island trembles, sky dims, soul fire extinguish,
 * single rising rod, everything slows.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 14.0-28.0 HP (escalating finale)
 * - Pre-death sequence events (#187-190 deal NO damage)
 */
public final class FinalCalamityD {

    private FinalCalamityD() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SigilBeams(plugin));
        registry.register(new SoulChoir(plugin));
        registry.register(new SoulVortexIntensifies(plugin));
        registry.register(new EchoFigureFinalPosition(plugin));
        registry.register(new SoulGround(plugin));
        registry.register(new IslandTrembles(plugin));
        registry.register(new SkyDims(plugin));
        registry.register(new SoulFireGoesOut(plugin));
        registry.register(new SingleRisingRod(plugin));
        registry.register(new EverythingSlows(plugin));
    }

    // =========================================================================
    // 181. SIGIL BEAMS -- vertical light columns from sigil nodes
    // =========================================================================
    public static class SigilBeams extends EnvironmentalAttack {
        private boolean beamsFired = false;
        public SigilBeams(ChaosCraftPlugin p) { super(p, new AttackConfig("sigil_beams", AttackType.ENVIRONMENTAL, 5)); config.setDamage(16.0); config.setDamageRadius(0.5); config.setDurationTicks(240); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Buildup: 20 ticks -- nodes sprout END_ROD columns
            if (t <= 20) {
                if (t % 2 == 0) for (int i = 0; i < 12; i++) { double a = (2*Math.PI*i)/12; Location node = center.clone().add(Math.cos(a)*5, 0.3, Math.sin(a)*5); w.spawnParticle(Particle.END_ROD, node, 5, 0.1, 0.3, 0.1, 0.005); }
                return;
            }
            // Beams fire upward to Y+80 in 20 ticks
            if (!beamsFired) { beamsFired = true; for (int i = 0; i < 12; i++) { double a = (2*Math.PI*i)/12; DisplayBuilder.playSound(center.clone().add(Math.cos(a)*5, 0, Math.sin(a)*5), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f); } }
            int beamTick = t - 20;
            // Rising beams
            if (beamTick <= 20) {
                double height = beamTick * 4.0;
                if (beamTick % 2 == 0) for (int i = 0; i < 12; i++) { double a = (2*Math.PI*i)/12; Location node = center.clone().add(Math.cos(a)*5, 0, Math.sin(a)*5); for (int y = 0; y <= (int)height; y += 4) { w.spawnParticle(Particle.END_ROD, node.clone().add(0, y, 0), 15, 0.15, 0.5, 0.15, 0.005); DisplayBuilder.dustParticles(node.clone().add(0, y, 0), 8, 0.15, 220, 180, 255, 1.5f); } }
                // Rising beam damage
                if (beamTick % 5 == 0) for (int i = 0; i < 12; i++) { double a = (2*Math.PI*i)/12; Location node = center.clone().add(Math.cos(a)*5, 0, Math.sin(a)*5); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double dx=p.getLocation().getX()-node.getX(); double dz=p.getLocation().getZ()-node.getZ(); if (dx*dx+dz*dz<=0.25) p.damage(16.0); } }
            }
            // Hold: 80 ticks
            if (beamTick > 20 && beamTick <= 100 && beamTick % 4 == 0) {
                for (int i = 0; i < 12; i++) { double a = (2*Math.PI*i)/12; Location node = center.clone().add(Math.cos(a)*5, 0, Math.sin(a)*5); for (int y = 0; y <= 80; y += 8) w.spawnParticle(Particle.END_ROD, node.clone().add(0, y, 0), 10, 0.15, 0.5, 0.15, 0.003); }
                if (beamTick % 20 == 0) for (int i = 0; i < 12; i++) { double a = (2*Math.PI*i)/12; Location node = center.clone().add(Math.cos(a)*5, 0, Math.sin(a)*5); for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double dx=p.getLocation().getX()-node.getX(); double dz=p.getLocation().getZ()-node.getZ(); if (dx*dx+dz*dz<=0.25) p.damage(14.0); } }
            }
            // Retract
            if (beamTick > 100 && beamTick <= 110 && beamTick % 2 == 0) { double retractHeight = 80 - (beamTick-100)*8; for (int i = 0; i < 12; i++) { double a = (2*Math.PI*i)/12; Location node = center.clone().add(Math.cos(a)*5, retractHeight, Math.sin(a)*5); w.spawnParticle(Particle.END_ROD, node, 8, 0.2, 0.5, 0.2, 0.005); } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SigilBeams(plugin); }
    }

    // =========================================================================
    // 182. SOUL CHOIR -- mass audio event, no warning
    // =========================================================================
    public static class SoulChoir extends EnvironmentalAttack {
        private boolean choirFired = false;
        public SoulChoir(ChaosCraftPlugin p) { super(p, new AttackConfig("soul_choir", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(120); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // 4 ticks silence, then choir
            if (t == 5 && !choirFired) {
                choirFired = true;
                for (int i = 0; i < 20; i++) { double a = (2*Math.PI*i)/20; double r = 10; Location choirPos = center.clone().add(Math.cos(a)*r, 5+Math.random()*5, Math.sin(a)*r); DisplayBuilder.playSound(choirPos, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1.0f, 0.3f); }
            }
            if (t >= 5 && t <= 65 && t % 3 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add((Math.random()-0.5)*16, 0.3, (Math.random()-0.5)*16), 20, 3.0, 0.1, 3.0, 0.005);
                w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add((Math.random()-0.5)*10, 1, (Math.random()-0.5)*10), 10, 2.0, 0.5, 2.0, 0.01);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SoulChoir(plugin); }
    }

    // =========================================================================
    // 183. SOUL VORTEX INTENSIFIES -- pull force doubled
    // =========================================================================
    public static class SoulVortexIntensifies extends EnvironmentalAttack {
        public SoulVortexIntensifies(ChaosCraftPlugin p) { super(p, new AttackConfig("soul_vortex_intensifies", AttackType.ENVIRONMENTAL, 5)); config.setDamage(14.0); config.setDamageRadius(2.0); config.setDurationTicks(600); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Tripled core density in first 20 ticks
            if (t <= 20 && t % 2 == 0) w.spawnParticle(Particle.PORTAL, center.clone().add(0, 2, 0), 120, 3.0, 1.0, 3.0, 0.05);
            // Doubled pull force
            if (t % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(Math.pow(p.getLocation().getX()-center.getX(),2)+Math.pow(p.getLocation().getZ()-center.getZ(),2));
                    if (dist <= 8.0 && dist > 0.5) { double dx=center.getX()-p.getLocation().getX(); double dz=center.getZ()-p.getLocation().getZ(); p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(dx/dist*0.30, 0, dz/dist*0.30))); }
                    if (dist <= 2.0) p.damage(14.0);
                }
            }
            if (t % 3 == 0) { w.spawnParticle(Particle.PORTAL, center.clone().add(0,2,0), 80, 4.0, 1.0, 4.0, 0.04); for (int r=1; r<=8; r+=2) DisplayBuilder.particleRing(center.clone().add(0,0.5,0), r, Particle.SOUL_FIRE_FLAME, r*3, null); }
            if (t == 5) DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 0.4f, 0.1f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SoulVortexIntensifies(plugin); }
    }

    // =========================================================================
    // 184. ECHO FIGURE FINAL POSITION -- 7 figures form ring at 6-block radius
    // =========================================================================
    public static class EchoFigureFinalPosition extends EnvironmentalAttack {
        public EchoFigureFinalPosition(ChaosCraftPlugin p) { super(p, new AttackConfig("echo_figure_final_position", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(100); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double progress = Math.min(t / 20.0, 1.0);
            if (t % 3 == 0) {
                for (int i = 0; i < 7; i++) { double a = (2*Math.PI*i)/7; double startR = 10; double endR = 6; double r = startR + (endR-startR)*progress; Location figPos = center.clone().add(Math.cos(a)*r, 1.0, Math.sin(a)*r); w.spawnParticle(Particle.SOUL_FIRE_FLAME, figPos, 10, 0.2, 0.5, 0.2, 0.005); DisplayBuilder.dustParticles(figPos, 5, 0.2, 200, 200, 220, 1.2f); }
            }
            if (t == 20) for (int i = 0; i < 7; i++) { double a = (2*Math.PI*i)/7; DisplayBuilder.playSound(center.clone().add(Math.cos(a)*6, 0, Math.sin(a)*6), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.4f); }
            // In ring: reverse_portal outward
            if (t > 20 && t % 5 == 0) for (int i = 0; i < 7; i++) { double a = (2*Math.PI*i)/7; Location figPos = center.clone().add(Math.cos(a)*6, 1.0, Math.sin(a)*6); w.spawnParticle(Particle.REVERSE_PORTAL, figPos, 5, 0.2, 0.3, 0.2, 0.005); }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new EchoFigureFinalPosition(plugin); }
    }

    // =========================================================================
    // 185. SOUL GROUND -- floor absorbs soul energy, permanent blue tint
    // =========================================================================
    public static class SoulGround extends EnvironmentalAttack {
        public SoulGround(ChaosCraftPlugin p) { super(p, new AttackConfig("soul_ground", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(120); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Floor tint shift over 40 ticks
            if (t % 3 == 0) {
                for (int x = -7; x <= 7; x += 2) for (int z = -7; z <= 7; z += 2) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(x, 0.3, z), 3, 0.3, 0.1, 0.3, 0.005);
                }
            }
            if (t == 5) DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_STEP, 0.2f, 0.1f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SoulGround(plugin); }
    }

    // =========================================================================
    // 186. ISLAND TREMBLES -- all systems at maximum for 5 seconds
    // =========================================================================
    public static class IslandTrembles extends EnvironmentalAttack {
        public IslandTrembles(ChaosCraftPlugin p) { super(p, new AttackConfig("island_trembles", AttackType.ENVIRONMENTAL, 5)); config.setDamage(20.0); config.setDurationTicks(100); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (t == 1) for (int i = 0; i < 5; i++) DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.2f + i*0.1f);
            // Everything at maximum: all particle types
            if (t % 2 == 0) {
                for (int i = 0; i < 30; i++) { Location pos = center.clone().add((Math.random()-0.5)*18, Math.random()*15, (Math.random()-0.5)*18); switch ((int)(Math.random()*6)) { case 0: w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 8, 0.5, 0.5, 0.5, 0.01); break; case 1: w.spawnParticle(Particle.WITCH, pos, 6, 0.5, 0.5, 0.5, 0); break; case 2: w.spawnParticle(Particle.TOTEM_OF_UNDYING, pos, 4, 0.5, 0.5, 0.5, 0.02); break; case 3: w.spawnParticle(Particle.END_ROD, pos, 6, 0.5, 0.5, 0.5, 0.01); break; case 4: w.spawnParticle(Particle.DRAGON_BREATH, pos, 4, 0.5, 0.5, 0.5, 0.01); break; default: DisplayBuilder.dustParticles(pos, 4, 0.5, 180, 0, 0, 1.5f); break; } }
            }
            if (t % 20 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; p.damage(20.0); }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new IslandTrembles(plugin); }
    }

    // =========================================================================
    // 187. SKY DIMS -- streams fade from 300% to 10% (NO DAMAGE)
    // =========================================================================
    public static class SkyDims extends EnvironmentalAttack {
        public SkyDims(ChaosCraftPlugin p) { super(p, new AttackConfig("sky_dims", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(140); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double density = Math.max(1.0 - t / 120.0, 0.1);
            if (t % 4 == 0) {
                int count = Math.max((int)(density * 15), 1);
                for (int i = 0; i < count; i++) { Location pos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*15, (Math.random()-0.5)*20); switch ((int)(Math.random()*5)) { case 0: w.spawnParticle(Particle.WITCH, pos, 3, 0.5, 0.5, 0.5, 0); break; case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, pos, 2, 0.5, 0.5, 0.5, 0.005); break; case 2: w.spawnParticle(Particle.END_ROD, pos, 3, 0.5, 0.5, 0.5, 0.003); break; case 3: w.spawnParticle(Particle.DRAGON_BREATH, pos, 2, 0.5, 0.5, 0.5, 0.003); break; default: DisplayBuilder.dustParticles(pos, 2, 0.5, 180, 0, 0, 0.8f); break; } }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SkyDims(plugin); }
    }

    // =========================================================================
    // 188. SOUL FIRE GOES OUT -- all soul fire extinguished simultaneously
    // =========================================================================
    public static class SoulFireGoesOut extends EnvironmentalAttack {
        private boolean extinguished = false;
        public SoulFireGoesOut(ChaosCraftPlugin p) { super(p, new AttackConfig("soul_fire_goes_out", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(60); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (!extinguished) {
                extinguished = true;
                // Poof across entire floor
                for (int x = -7; x <= 7; x += 2) for (int z = -7; z <= 7; z += 2) w.spawnParticle(Particle.POOF, center.clone().add(x, 0.3, z), 5, 0.3, 0.1, 0.3, 0.01);
                // 20 fire-out sounds
                for (int i = 0; i < 20; i++) DisplayBuilder.playSound(center.clone().add((Math.random()-0.5)*14, 0, (Math.random()-0.5)*14), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 0.5f);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SoulFireGoesOut(plugin); }
    }

    // =========================================================================
    // 189. SINGLE RISING ROD -- one end_rod particle rises from center to Y+100
    // =========================================================================
    public static class SingleRisingRod extends EnvironmentalAttack {
        public SingleRisingRod(ChaosCraftPlugin p) { super(p, new AttackConfig("single_rising_rod", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double height = t * 0.5;
            if (t == 1) DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 2.0f);
            if (t % 2 == 0) w.spawnParticle(Particle.END_ROD, center.clone().add(0, height, 0), 1, 0, 0, 0, 0);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new SingleRisingRod(plugin); }
    }

    // =========================================================================
    // 190. EVERYTHING SLOWS -- world animation speeds reduce to 25%
    // =========================================================================
    public static class EverythingSlows extends EnvironmentalAttack {
        public EverythingSlows(ChaosCraftPlugin p) { super(p, new AttackConfig("everything_slows", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (t == 1) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 0.1f);
            // Sky at 25% travel speed -- fewer, slower particles
            if (t % 8 == 0) {
                for (int i = 0; i < 3; i++) { Location pos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*15, (Math.random()-0.5)*20); w.spawnParticle(Particle.END_ROD, pos, 2, 0.3, 0.3, 0.3, 0.001); w.spawnParticle(Particle.WITCH, pos, 1, 0.3, 0.3, 0.3, 0); }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); } @Override public AbstractAttack newInstance() { return new EverythingSlows(plugin); }
    }
}
