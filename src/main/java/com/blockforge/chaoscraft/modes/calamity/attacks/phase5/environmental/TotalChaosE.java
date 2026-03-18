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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #91-100
 * DEATH SEQUENCE ENVIRONMENT -- the island healing itself after Calamitas dies.
 * None of these events deal damage. They are narrative/atmospheric.
 */
public final class TotalChaosE {

    private TotalChaosE() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GalleryCollapse(plugin));
        registry.register(new SigilIllumination(plugin));
        registry.register(new VoidZonesClose(plugin));
        registry.register(new BrimstoneCools(plugin));
        registry.register(new CrystalsToDust(plugin));
        registry.register(new GoldGoesDark(plugin));
        registry.register(new MonolithsFallSilent(plugin));
        registry.register(new CrownDescendsLast(plugin));
        registry.register(new SoulTowersStand(plugin));
        registry.register(new TheAftermath(plugin));
    }

    // 91. GALLERY COLLAPSE -- echo gallery arches dissolve upward
    public static class GalleryCollapse extends EnvironmentalAttack {
        public GalleryCollapse(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("gallery_collapse", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(100); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 20) { if (ticksAlive % 4 == 0) for (int i = 0; i < 5; i++) { double ax = center.getX() - 8 + i * 4; w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, ax, center.getY() + 3, center.getZ() + 9), 3, 0.3, 0.5, 0.3, 0.005); } return; }
            if (ticksAlive > 20 && ticksAlive <= 50) {
                double progress = (ticksAlive - 20) / 30.0;
                for (int i = 0; i < 5; i++) { double ax = center.getX() - 8 + i * 4;
                    double riseY = center.getY() + 3 + progress * 12;
                    if (ticksAlive % 3 == 0) { w.spawnParticle(Particle.PORTAL, new Location(w, ax, riseY, center.getZ() + 9), 5, 0.3, 0.5, 0.3, 0.03); w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, ax, riseY, center.getZ() + 9), 3, 0.3, 0.3, 0.3, 0.01); }
                }
                if (ticksAlive == 21) { for (int i = 0; i < 5; i++) DisplayBuilder.playSound(center.clone().add(-8+i*4, 0, 9), Sound.BLOCK_STONE_BREAK, 0.4f, 0.4f); }
            }
            if (ticksAlive > 50 && ticksAlive % 5 == 0) { for (int i = 0; i < 5; i++) { double ax = center.getX() - 8 + i * 4; w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, ax, center.getY() + 15, center.getZ() + 9), 2, 0.2, 0.2, 0.2, 0.003); } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GalleryCollapse(plugin); }
    }

    // 92. SIGIL ILLUMINATION -- sigil glows one final time
    public static class SigilIllumination extends EnvironmentalAttack {
        public SigilIllumination(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("sigil_illumination", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(640); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 40) { double rate = ticksAlive / 40.0; if (ticksAlive % 3 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, 0), (int)(3+rate*12), 1.5, 0.2, 1.5, 0.005 + rate*0.01); for (double d = -2; d <= 2; d += 1) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(d, 0.3, 0), 2, 0.2, 0.1, 0.2, 0.005); w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, d), 2, 0.2, 0.1, 0.2, 0.005); } } return; }
            if (ticksAlive > 40 && ticksAlive <= 240) { if (ticksAlive % 2 == 0) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 8, 1.5, 1, 1.5, 0.01); for (double d = -2; d <= 2; d += 0.5) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(d, 0.3, 0), 2, 0.1, 0.5, 0.1, 0.005); w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, d), 2, 0.1, 0.5, 0.1, 0.005); } DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 3, 0.5, 100, 0, 200, 0.8f); w.spawnParticle(Particle.PORTAL, center.clone().add(0, 0.5, 0), 5, 1, 0.5, 1, 0.03); } if (ticksAlive == 41) for (int i = 0; i < 4; i++) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.8f + i * 0.1f); return; }
            double fade = Math.max(0, 1 - (ticksAlive - 240) / 400.0);
            if (ticksAlive % (int)(3 + (1-fade)*10) == 0 && fade > 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, 0), (int)(fade * 5), 0.5, 0.2, 0.5, 0.003);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SigilIllumination(plugin); }
    }

    // 93. VOID ZONES CLOSE -- consumed arena edges reform
    public static class VoidZonesClose extends EnvironmentalAttack {
        public VoidZonesClose(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("void_zones_close", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(180); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 20) { if (ticksAlive % 3 == 0) for (double d = -10; d <= 10; d += 1) for (int side = 0; side < 4; side++) { Location lp = switch(side) { case 0 -> center.clone().add(d, 0.3, -10); case 1 -> center.clone().add(d, 0.3, 10); case 2 -> center.clone().add(-10, 0.3, d); default -> center.clone().add(10, 0.3, d); }; w.spawnParticle(Particle.PORTAL, lp, 3, 0.2, 0.3, 0.2, 0.05); } return; }
            int reformTick = ticksAlive - 21;
            if (reformTick >= 0 && reformTick <= 160) {
                double progress = reformTick / 160.0;
                int rowsBack = (int)(progress * 3) + 7; // 7->10 over time
                if (reformTick % 5 == 0) for (double d = -rowsBack; d <= rowsBack; d += 1) {
                    DisplayBuilder.dustParticles(center.clone().add(d, 0.3, -rowsBack), 3, 0.2, 200, 200, 180, 0.8f);
                    DisplayBuilder.dustParticles(center.clone().add(d, 0.3, rowsBack), 3, 0.2, 200, 200, 180, 0.8f);
                    DisplayBuilder.dustParticles(center.clone().add(-rowsBack, 0.3, d), 3, 0.2, 200, 200, 180, 0.8f);
                    DisplayBuilder.dustParticles(center.clone().add(rowsBack, 0.3, d), 3, 0.2, 200, 200, 180, 0.8f);
                    w.spawnParticle(Particle.END_ROD, center.clone().add(d, 0.5, -rowsBack), 1, 0.1, 0.1, 0.1, 0.005);
                }
                if (reformTick % 40 == 0) { DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.6f); DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 0.5f); }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VoidZonesClose(plugin); }
    }

    // 94. BRIMSTONE COOLS -- all fire effects cease
    public static class BrimstoneCools extends EnvironmentalAttack {
        public BrimstoneCools(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("brimstone_cools", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(160); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 40) { if (ticksAlive % 3 == 0) for (int i = 0; i < 5; i++) { double fx = center.getX()+(Math.random()-0.5)*18; double fz = center.getZ()+(Math.random()-0.5)*18; DisplayBuilder.dustParticles(new Location(w, fx, center.getY()+0.3, fz), 3, 0.3, 200, 40, 0, 0.6f); } return; }
            double fade = Math.max(0, 1 - (ticksAlive - 40) / 80.0);
            if (ticksAlive % 4 == 0 && fade > 0) for (int i = 0; i < (int)(8*fade); i++) { double fx = center.getX()+(Math.random()-0.5)*18; double fz = center.getZ()+(Math.random()-0.5)*18; w.spawnParticle(Particle.SMOKE, new Location(w, fx, center.getY()+0.5, fz), 3, 0.3, 0.3, 0.3, 0.01); }
            if (ticksAlive > 80 && ticksAlive % 5 == 0) { for (int i = 0; i < 3; i++) { double fx = center.getX()+(Math.random()-0.5)*18; double fz = center.getZ()+(Math.random()-0.5)*18; DisplayBuilder.dustParticles(new Location(w, fx, center.getY()+0.5, fz), 2, 0.3, 80, 80, 80, 0.6f); } }
            if (ticksAlive % 20 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 0.7f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneCools(plugin); }
    }

    // 95. CRYSTALS TURN TO DUST -- crystal elements dissolve into purple dust
    public static class CrystalsToDust extends EnvironmentalAttack {
        public CrystalsToDust(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("crystals_to_dust", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(120); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 20) { if (ticksAlive % 3 == 0) for (int i = 0; i < 5; i++) { double cx = center.getX()+(Math.random()-0.5)*14; double cz = center.getZ()+(Math.random()-0.5)*14; w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, cx, center.getY()+2, cz), 3, 0.3, 0.3, 0.3, 0.01); } return; }
            double fade = Math.max(0, 1 - (ticksAlive - 20) / 60.0);
            if (ticksAlive % 3 == 0 && fade > 0) for (int i = 0; i < (int)(6*fade); i++) { double cx = center.getX()+(Math.random()-0.5)*14; double cz = center.getZ()+(Math.random()-0.5)*14; w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, cx, center.getY()+1+fade*3, cz), 2, 0.5, 0.5, 0.5, 0.01); DisplayBuilder.dustParticles(new Location(w, cx, center.getY()+0.5, cz), 2, 0.5, 180, 100, 255, 0.8f); }
            if (ticksAlive > 80 && ticksAlive % 8 == 0) for (int i = 0; i < 3; i++) { double cx = center.getX()+(Math.random()-0.5)*16; double cz = center.getZ()+(Math.random()-0.5)*16; DisplayBuilder.dustParticles(new Location(w, cx, center.getY()+0.3, cz), 1, 0.2, 180, 100, 255, 0.5f); }
            if (ticksAlive % 20 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.3f, 0.8f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrystalsToDust(plugin); }
    }

    // 96. GOLD GOES DARK -- warm gold tones extinguish
    public static class GoldGoesDark extends EnvironmentalAttack {
        public GoldGoesDark(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("gold_goes_dark", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(120); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (ticksAlive <= 20) { if (ticksAlive % 3 == 0) { w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 2, 0), 10, 5, 1, 5, 0.02); DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 5, 255, 200, 0, 1.0f); } return; }
            double fade = Math.max(0, 1 - (ticksAlive - 20) / 80.0);
            if (ticksAlive % 4 == 0 && fade > 0) { w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1, 0), (int)(fade*8), 5, 0.5, 5, 0.01); DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), (int)(fade*5), 3, 255, 200, 0, 0.8f); }
            if (ticksAlive > 100 && ticksAlive % 6 == 0) DisplayBuilder.dustParticles(center.clone().add((Math.random()-0.5)*10, 0.5, (Math.random()-0.5)*10), 1, 0.2, 100, 80, 0, 0.5f);
            if (ticksAlive == 21) DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.6f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GoldGoesDark(plugin); }
    }

    // 97. MONOLITHS FALL SILENT -- rotation stops, glow extinguishes
    public static class MonolithsFallSilent extends EnvironmentalAttack {
        public MonolithsFallSilent(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("monoliths_fall_silent", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(140); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double[][] corners = {{-9,-9},{9,-9},{9,9},{-9,9}};
            double fade = Math.max(0, 1 - ticksAlive / 60.0);
            if (ticksAlive % 4 == 0 && fade > 0) for (double[] c : corners) DisplayBuilder.dustParticles(center.clone().add(c[0], 3, c[1]), (int)(fade*5), 0.3, 50, 0, 100, 0.8f);
            int darkIndex = ticksAlive / 20;
            if (ticksAlive % 20 == 0 && darkIndex < 4) { Location mono = center.clone().add(corners[darkIndex][0], 3, corners[darkIndex][1]); w.spawnParticle(Particle.END_ROD, mono, 10, 0.3, 0.3, 0.3, 0.01); DisplayBuilder.playSound(mono, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.8f); }
            if (ticksAlive > 60 && ticksAlive % 8 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.2f, 0.3f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MonolithsFallSilent(plugin); }
    }

    // 98. CROWN DESCENDS FOR THE LAST TIME -- gentle descent to floor
    public static class CrownDescendsLast extends EnvironmentalAttack {
        public CrownDescendsLast(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("crown_descends_last", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(240); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            double crownY = center.getY() + 20 - (Math.min(ticksAlive, 240) / 240.0) * 20;
            if (ticksAlive % 4 == 0) { DisplayBuilder.particleRing(center.clone().add(0, crownY - center.getY(), 0), 3.0, Particle.DUST, 15, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.0f)); DisplayBuilder.dustParticles(center.clone().add(0, crownY - center.getY(), 0), 3, 1.5, 180, 0, 0, 0.8f); }
            if (ticksAlive == 230) { DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.5f); DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 20, 2, 180, 0, 0, 1.0f); }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrownDescendsLast(plugin); }
    }

    // 99. SOUL TOWERS STAND -- towers exhale one final drift
    public static class SoulTowersStand extends EnvironmentalAttack {
        public SoulTowersStand(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("soul_towers_stand", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            Location[] towers = { center.clone().add(-8, 0, 6), center.clone().add(8, 0, -6) };
            if (ticksAlive <= 40) { if (ticksAlive % 3 == 0) for (Location t : towers) w.spawnParticle(Particle.SOUL_FIRE_FLAME, t.clone().add(0, 8, 0), 3, 0.2, 0.3, 0.2, 0.005); return; }
            double fade = Math.max(0.15, 1 - (ticksAlive - 40) / 60.0);
            if (ticksAlive % (int)(3 + (1-fade)*8) == 0) for (Location t : towers) w.spawnParticle(Particle.SOUL_FIRE_FLAME, t.clone().add(0, 8, 0), (int)(fade*3), 0.1, 0.2, 0.1, 0.003);
            if (ticksAlive > 40 && ticksAlive <= 140) { double driftY = center.getY() + 8 + ((ticksAlive - 40) / 100.0) * 22; if (ticksAlive % 4 == 0) for (Location t : towers) { w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, t.getX(), driftY, t.getZ()), 2, 0.2, 0.5, 0.2, 0.005); w.spawnParticle(Particle.PORTAL, new Location(w, t.getX(), driftY, t.getZ()), 1, 0.1, 0.3, 0.1, 0.02); } }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulTowersStand(plugin); }
    }

    // 100. THE AFTERMATH -- the permanent post-fight island state
    public static class TheAftermath extends EnvironmentalAttack {
        public TheAftermath(ChaosCraftPlugin plugin) { super(plugin, new AttackConfig("the_aftermath", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDamageRadius(0.0); config.setDurationTicks(3600); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location center) { }
        @Override protected void onTick(int ticksAlive) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Sky residue: faint crimson motes drifting at Y+60 (fading over 3 minutes)
            double skyFade = Math.max(0, 1 - ticksAlive / 3600.0);
            if (ticksAlive % 10 == 0 && skyFade > 0) { double rx = center.getX() + (Math.random()-0.5) * 40; double rz = center.getZ() + (Math.random()-0.5) * 40; DisplayBuilder.dustParticles(new Location(w, rx, center.getY() + 60, rz), 1, 0.5, 180, 0, 0, 0.8f); }
            // Sigil heartbeat: SOUL_FIRE_FLAME 1/tick
            if (ticksAlive % 20 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, 0), 1, 0.1, 0.1, 0.1, 0.002);
            // Tower lanterns
            Location[] towers = { center.clone().add(-8, 8, 6), center.clone().add(8, 8, -6) };
            if (ticksAlive % 20 == 0) for (Location t : towers) w.spawnParticle(Particle.SOUL_FIRE_FLAME, t, 1, 0.05, 0.1, 0.05, 0.002);
            // Crystal shimmer
            if (ticksAlive % 40 == 0) { double sx = center.getX() + (Math.random()-0.5) * 16; double sz = center.getZ() + (Math.random()-0.5) * 16; DisplayBuilder.dustParticles(new Location(w, sx, center.getY() + 0.3, sz), 1, 0.1, 180, 100, 255, 0.4f); }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TheAftermath(plugin); }
    }
}
