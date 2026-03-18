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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #81-90
 * VOID TIDES (#81-84) + THE CONVERGENCE (#85) + PERMANENT DAMAGE ZONES (#86-88) + DEATH SEQUENCE (#89-90)
 */
public final class TotalChaosD {

    private TotalChaosD() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidTideFirst(plugin));
        registry.register(new VoidTideSecond(plugin));
        registry.register(new VoidTideThird(plugin));
        registry.register(new VoidTideFinal(plugin));
        registry.register(new TheConvergence(plugin));
        registry.register(new SigilScar(plugin));
        registry.register(new BrimstoneScar(plugin));
        registry.register(new FinalTideScar(plugin));
        registry.register(new TheSilence(plugin));
        registry.register(new LastCrimsonStream(plugin));
    }

    // 81. VOID TIDE FIRST -- one row consumed from north edge
    public static class VoidTideFirst extends EnvironmentalAttack {
        public VoidTideFirst(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("void_tide_first", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.4f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 120) { if (ticksAlive % 5 == 0) { for (double x = -10; x <= 10; x += 1) { DisplayBuilder.dustParticles(center.clone().add(x, 0.3, -10), 3, 0.3, 20, 0, 40, 1.0f); w.spawnParticle(Particle.PORTAL, center.clone().add(x, 0.5, -10.5), 2, 0.2, 0.3, 0.2, 0.05); } } return; }
            if (ticksAlive > 120 && ticksAlive <= 150) {
                double progress = (ticksAlive - 120) / 30.0;
                if (ticksAlive % 3 == 0) for (double x = -10; x <= 10; x += 1) { Location tp = center.clone().add(x, 0.3, -10); DisplayBuilder.dustParticles(tp, (int)(10*(1-progress)), 0.3, 20, 0, 40, 1.2f); w.spawnParticle(Particle.PORTAL, tp, 5, 0.2, 0.5, 0.2, 0.1); }
                if (ticksAlive == 121) for (int i = 0; i < 20; i++) DisplayBuilder.playSound(center.clone().add(i-10, 0, -10), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.5f);
            }
            if (ticksAlive > 150) { if (ticksAlive % 4 == 0) for (double x = -10; x <= 10; x += 2) w.spawnParticle(Particle.PORTAL, center.clone().add(x, 0.3, -9), 2, 0.1, 0.2, 0.1, 0.02);
                if (ticksAlive % 20 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().getZ() < center.getZ() - 9) p.damage(6.0); }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VoidTideFirst(plugin); }
    }

    // 82. VOID TIDE SECOND -- two rows consumed simultaneously
    public static class VoidTideSecond extends EnvironmentalAttack {
        public VoidTideSecond(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("void_tide_second", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(160); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) { DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.7f, 0.4f); DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.6f); } }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 80) { if (ticksAlive % 5 == 0) { for (double d = -10; d <= 10; d += 1) { DisplayBuilder.dustParticles(center.clone().add(d, 0.3, 10), 3, 0.3, 20, 0, 40, 1.0f); DisplayBuilder.dustParticles(center.clone().add(-10, 0.3, d), 3, 0.3, 20, 0, 40, 1.0f); } } return; }
            if (ticksAlive > 80 && ticksAlive <= 110) {
                if (ticksAlive % 3 == 0) { for (double d = -10; d <= 10; d += 1) { w.spawnParticle(Particle.PORTAL, center.clone().add(d, 0.5, 10), 5, 0.2, 0.5, 0.2, 0.1); w.spawnParticle(Particle.PORTAL, center.clone().add(-10, 0.5, d), 5, 0.2, 0.5, 0.2, 0.1); } }
            }
            if (ticksAlive > 110) { if (ticksAlive % 20 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().getZ() > center.getZ()+9 || p.getLocation().getX() < center.getX()-9) p.damage(6.0); } if (ticksAlive % 5 == 0) { for (double d = -10; d <= 10; d += 2) { w.spawnParticle(Particle.PORTAL, center.clone().add(d, 0.3, 9), 1, 0.1, 0.1, 0.1, 0.02); w.spawnParticle(Particle.PORTAL, center.clone().add(-9, 0.3, d), 1, 0.1, 0.1, 0.1, 0.02); } } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VoidTideSecond(plugin); }
    }

    // 83. VOID TIDE THIRD -- three edges consumed simultaneously
    public static class VoidTideThird extends EnvironmentalAttack {
        public VoidTideThird(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("void_tide_third", AttackType.ENVIRONMENTAL, 5)); config.setDamage(8.0); config.setDamageRadius(0.0); config.setDurationTicks(140); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 60) { if (ticksAlive % 4 == 0) for (double d = -8; d <= 8; d += 1) { w.spawnParticle(Particle.PORTAL, center.clone().add(d, 0.5, -8), 3, 0.2, 0.3, 0.2, 0.1); w.spawnParticle(Particle.PORTAL, center.clone().add(d, 0.5, 8), 3, 0.2, 0.3, 0.2, 0.1); w.spawnParticle(Particle.PORTAL, center.clone().add(8, 0.5, d), 3, 0.2, 0.3, 0.2, 0.1); } return; }
            if (ticksAlive > 60 && ticksAlive <= 90) { if (ticksAlive % 2 == 0) for (double d = -8; d <= 8; d += 1) { DisplayBuilder.dustParticles(center.clone().add(d, 0.3, -8), 5, 0.3, 20, 0, 40, 1.5f); DisplayBuilder.dustParticles(center.clone().add(d, 0.3, 8), 5, 0.3, 20, 0, 40, 1.5f); DisplayBuilder.dustParticles(center.clone().add(8, 0.3, d), 5, 0.3, 20, 0, 40, 1.5f); }
                if (ticksAlive == 61) DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 0.4f); }
            if (ticksAlive > 90) { if (ticksAlive % 20 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double dx = Math.abs(p.getLocation().getX()-center.getX()); double dz = Math.abs(p.getLocation().getZ()-center.getZ()); if (dx > 8 || dz > 8) p.damage(8.0); } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VoidTideThird(plugin); }
    }

    // 84. VOID TIDE FINAL -- all four edges; arena shrinks to ~14x14
    public static class VoidTideFinal extends EnvironmentalAttack {
        public VoidTideFinal(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("void_tide_final", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(10); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_DEATH, 0.6f, 0.4f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 80) { if (ticksAlive % 3 == 0) for (double d = -7; d <= 7; d += 1) { for (int side = 0; side < 4; side++) { Location lp = switch(side) { case 0 -> center.clone().add(d, 0.3, -7); case 1 -> center.clone().add(d, 0.3, 7); case 2 -> center.clone().add(-7, 0.3, d); default -> center.clone().add(7, 0.3, d); }; w.spawnParticle(Particle.PORTAL, lp, 3, 0.2, 0.3, 0.2, 0.1); DisplayBuilder.dustParticles(lp, 2, 0.3, 20, 0, 40, 1.5f); } } return; }
            if (ticksAlive > 80 && ticksAlive <= 120) {
                if (ticksAlive % 2 == 0) for (double d = -7; d <= 7; d += 1) for (int side = 0; side < 4; side++) { Location lp = switch(side) { case 0 -> center.clone().add(d, 0.3, -7); case 1 -> center.clone().add(d, 0.3, 7); case 2 -> center.clone().add(-7, 0.3, d); default -> center.clone().add(7, 0.3, d); }; w.spawnParticle(Particle.PORTAL, lp.clone().add(0, 2.5, 0), 3, 0.2, 2.5, 0.2, 0.05); DisplayBuilder.dustParticles(lp, 3, 0.3, 20, 0, 40, 1.8f); }
                if (ticksAlive == 81) { DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_DEATH, 0.8f, 0.4f); DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.5f); }
            }
            // Void walls pulse
            if (ticksAlive > 120 && ticksAlive <= 200) {
                if (ticksAlive == 140) { // 2 second stillness then walls
                    for (int side = 0; side < 4; side++) for (double d = -7; d <= 7; d += 0.5) { Location lp = switch(side) { case 0 -> center.clone().add(d, 0.3, -7); case 1 -> center.clone().add(d, 0.3, 7); case 2 -> center.clone().add(-7, 0.3, d); default -> center.clone().add(7, 0.3, d); }; for (double y = 0; y < 8; y += 1) w.spawnParticle(Particle.PORTAL, lp.clone().add(0, y, 0), 3, 0.2, 0.2, 0.2, 0.05); }
                    DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
                }
                if (ticksAlive % 10 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double dx = Math.abs(p.getLocation().getX()-center.getX()); double dz = Math.abs(p.getLocation().getZ()-center.getZ()); if (dx > 7 || dz > 7) { p.damage(6.0); org.bukkit.util.Vector push = center.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.5); push.setY(0.3); p.setVelocity(push); } }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VoidTideFinal(plugin); }
    }

    // 85. THE CONVERGENCE -- everything fires at 300% for 5 seconds
    public static class TheConvergence extends EnvironmentalAttack {
        public TheConvergence(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("the_convergence", AttackType.ENVIRONMENTAL, 5)); config.setDamage(8.0); config.setDamageRadius(10.0); config.setDurationTicks(260); config.setCooldownTicks(99999); config.setTicksBetweenDamage(10); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) { DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.1f); } }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 160) { if (ticksAlive % 2 == 0) for (int i = 0; i < 15; i++) { double px = center.getX()+(Math.random()-0.5)*20; double py = center.getY()+Math.random()*15; double pz = center.getZ()+(Math.random()-0.5)*20; switch(i%5) { case 0 -> w.spawnParticle(Particle.WITCH, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0); case 1 -> w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.02); case 2 -> w.spawnParticle(Particle.FLAME, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.02); case 3 -> w.spawnParticle(Particle.PORTAL, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.05); case 4 -> w.spawnParticle(Particle.END_ROD, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.02); } }
                if (ticksAlive == 80) { for (Sound s : new Sound[]{Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_WARDEN_AMBIENT, Sound.ENTITY_ELDER_GUARDIAN_CURSE}) DisplayBuilder.playSound(center, s, 1.5f, 0.3f); }
                // Title card
                if (ticksAlive == 120) for (Player p : w.getPlayers()) p.sendTitle("", "\u00a74Everything at once.", 10, 30, 10);
                return;
            }
            int fireTick = ticksAlive - 161;
            if (fireTick >= 0 && fireTick <= 100) {
                if (fireTick % 2 == 0) for (int i = 0; i < 30; i++) { double px = center.getX()+(Math.random()-0.5)*18; double py = center.getY()+Math.random()*12; double pz = center.getZ()+(Math.random()-0.5)*18; switch((int)(Math.random()*10)) { case 0 -> w.spawnParticle(Particle.WITCH, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0); case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.02); case 2 -> w.spawnParticle(Particle.END_ROD, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.02); case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.01); case 4 -> DisplayBuilder.dustParticles(new Location(w,px,py,pz), 3, 0.5, 180, 0, 0, 1.5f); case 5 -> w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.02); case 6 -> w.spawnParticle(Particle.FLAME, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.02); case 7 -> w.spawnParticle(Particle.LAVA, new Location(w,px,py,pz), 2, 0.3, 0.3, 0.3, 0); case 8 -> w.spawnParticle(Particle.PORTAL, new Location(w,px,py,pz), 3, 0.5, 0.5, 0.5, 0.05); case 9 -> DisplayBuilder.dustParticles(new Location(w,px,py,pz), 3, 0.5, 100, 0, 200, 1.5f); } }
            }
            if (fireTick == 100) { DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.5f); w.spawnParticle(Particle.WITCH, center, 400, 10, 5, 10, 0.1); w.spawnParticle(Particle.END_ROD, center, 400, 10, 5, 10, 0.1); }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TheConvergence(plugin); }
    }

    // 86. SIGIL SCAR -- permanent 1-heart/sec center tile damage
    public static class SigilScar extends EnvironmentalAttack {
        public SigilScar(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("sigil_scar", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(1.0); config.setDurationTicks(6000); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive % 4 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, 0), 3, 0.2, 0.2, 0.2, 0.005);
            if (ticksAlive % 60 == 0) DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 10, 0.5, 150, 0, 255, 1.0f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SigilScar(plugin); }
    }

    // 87. BRIMSTONE SCAR -- three permanent hot spots
    public static class BrimstoneScar extends EnvironmentalAttack {
        private final List<Location> hotSpots = new ArrayList<>();
        public BrimstoneScar(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("brimstone_scar", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(6000); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location center) { for (int i = 0; i < 3; i++) hotSpots.add(center.clone().add((Math.random()-0.5)*14, 0, (Math.random()-0.5)*14)); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive % 4 == 0) for (Location hs : hotSpots) { w.spawnParticle(Particle.LAVA, hs.clone().add(0, 0.3, 0), 1, 0.2, 0.1, 0.2, 0); DisplayBuilder.dustParticles(hs.clone().add(0, 0.5, 0), 3, 0.3, 200, 40, 0, 0.8f); }
            if (ticksAlive % 20 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; for (Location hs : hotSpots) if (p.getLocation().distanceSquared(hs) <= 1.0) { p.damage(6.0); break; } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneScar(plugin); }
    }

    // 88. FINAL TIDE SCAR -- outermost ring of arena is slow-damage zone
    public static class FinalTideScar extends EnvironmentalAttack {
        public FinalTideScar(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("final_tide_scar", AttackType.ENVIRONMENTAL, 5)); config.setDamage(6.0); config.setDamageRadius(0.0); config.setDurationTicks(6000); config.setCooldownTicks(99999); config.setTicksBetweenDamage(20); }
        @Override protected void onSpawn(Location center) { if (center.getWorld() != null) DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.4f, 0.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive % 5 == 0) for (double d = -7; d <= 7; d += 2) { w.spawnParticle(Particle.PORTAL, center.clone().add(d, 0.3, -7), 1, 0.1, 0.1, 0.1, 0.02); w.spawnParticle(Particle.PORTAL, center.clone().add(d, 0.3, 7), 1, 0.1, 0.1, 0.1, 0.02); w.spawnParticle(Particle.PORTAL, center.clone().add(-7, 0.3, d), 1, 0.1, 0.1, 0.1, 0.02); w.spawnParticle(Particle.PORTAL, center.clone().add(7, 0.3, d), 1, 0.1, 0.1, 0.1, 0.02); DisplayBuilder.dustParticles(center.clone().add(d, 0.2, -7), 1, 0.1, 20, 0, 40, 0.8f); }
            if (ticksAlive % 20 == 0) for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double dx = Math.abs(p.getLocation().getX()-center.getX()); double dz = Math.abs(p.getLocation().getZ()-center.getZ()); if (dx >= 6 || dz >= 6) p.damage(6.0); }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FinalTideScar(plugin); }
    }

    // 89. THE SILENCE -- all particles stop; 3 seconds of nothing
    public static class TheSilence extends EnvironmentalAttack {
        public TheSilence(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("the_silence", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(60); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { /* Complete silence -- no spawn effects */ }
        @Override protected void onTick(int ticksAlive) { /* Intentionally empty -- the silence IS the event */ }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TheSilence(plugin); }
    }

    // 90. LAST CRIMSON STREAM -- single slow stream crosses empty sky
    public static class LastCrimsonStream extends EnvironmentalAttack {
        public LastCrimsonStream(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("last_crimson_stream", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(100); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { /* Fires immediately after silence */ }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double progress = ticksAlive / 100.0;
            double streamX = center.getX() - 20 + progress * 40;
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(new Location(w, streamX, center.getY() + 60, center.getZ()), 8, 0.5, 180, 0, 0, 2.0f);
                DisplayBuilder.dustParticles(new Location(w, streamX - 1, center.getY() + 60, center.getZ()), 5, 0.3, 220, 20, 0, 1.5f);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new LastCrimsonStream(plugin); }
    }
}
