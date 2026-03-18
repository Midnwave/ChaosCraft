package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E Environmental -- FINAL CALAMITY A
 * Attacks #151-160: Maximum Sky Saturation events.
 * Triple density, intersection flares, sky storm, sky convergence explosion,
 * crimson rainfall columns, layer separation, void thread amplification,
 * TOTEM gold surge, END_ROD white lattice explosion, purple cloud descent.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 14.0-28.0 HP (escalating finale)
 * - Phase 4 sky weaponization -- the sky IS the fight
 */
public final class FinalCalamityA {

    private FinalCalamityA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SimultaneousTripleDensity(plugin));
        registry.register(new StreamIntersectionFlares(plugin));
        registry.register(new SkyStormChaos(plugin));
        registry.register(new TheSkyOpens(plugin));
        registry.register(new CrimsonRainfallColumns(plugin));
        registry.register(new StreamLayerSeparation(plugin));
        registry.register(new VoidThreadAmplification(plugin));
        registry.register(new SanctumGoldSurge(plugin));
        registry.register(new WhiteLatticeExplosion(plugin));
        registry.register(new PurpleCloudDescent(plugin));
    }

    // =========================================================================
    // 151. SIMULTANEOUS TRIPLE DENSITY -- all streams triple at Phase 4 entry
    // =========================================================================
    public static class SimultaneousTripleDensity extends EnvironmentalAttack {

        private boolean transitioned = false;

        public SimultaneousTripleDensity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("simultaneous_triple_density", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 4-tick flicker sequence: flicker, blackout, flash, settle
            if (ticksAlive == 1) {
                // Flicker -- streams stutter
                for (int i = 0; i < 10; i++) {
                    Location skyPos = center.clone().add((Math.random()-0.5)*20, 20+Math.random()*10, (Math.random()-0.5)*20);
                    w.spawnParticle(Particle.END_ROD, skyPos, 8, 2.0, 1.0, 2.0, 0.02);
                }
            }
            if (ticksAlive == 2) {
                // Blackout -- nothing
            }
            if (ticksAlive == 3) {
                // Flash at 500%
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
                for (int i = 0; i < 60; i++) {
                    Location skyPos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*15, (Math.random()-0.5)*20);
                    switch ((int)(Math.random()*5)) {
                        case 0: w.spawnParticle(Particle.WITCH, skyPos, 10, 1.0, 1.0, 1.0, 0); break;
                        case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 8, 1.0, 1.0, 1.0, 0.02); break;
                        case 2: w.spawnParticle(Particle.END_ROD, skyPos, 10, 1.0, 1.0, 1.0, 0.01); break;
                        case 3: w.spawnParticle(Particle.DRAGON_BREATH, skyPos, 8, 1.0, 1.0, 1.0, 0.01); break;
                        default: DisplayBuilder.dustParticles(skyPos, 8, 1.0, 180, 0, 0, 2.0f); break;
                    }
                }
            }
            if (ticksAlive == 4 && !transitioned) {
                transitioned = true;
                // Phase 4 ambient loop
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.4f, 0.8f);
            }

            // Settle at 300% -- ground-level light bleed from overhead
            if (ticksAlive >= 4 && ticksAlive % 4 == 0) {
                for (int i = 0; i < 15; i++) {
                    Location skyPos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*15, (Math.random()-0.5)*20);
                    switch ((int)(Math.random()*5)) {
                        case 0: w.spawnParticle(Particle.WITCH, skyPos, 6, 1.0, 0.5, 1.0, 0); break;
                        case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 4, 1.0, 0.5, 1.0, 0.01); break;
                        case 2: w.spawnParticle(Particle.END_ROD, skyPos, 6, 1.0, 0.5, 1.0, 0.005); break;
                        case 3: w.spawnParticle(Particle.DRAGON_BREATH, skyPos, 4, 1.0, 0.5, 1.0, 0.005); break;
                        default: DisplayBuilder.dustParticles(skyPos, 4, 1.0, 180, 0, 0, 1.0f); break;
                    }
                }
                // Ground bleed from above
                for (int i = 0; i < 5; i++) {
                    Location groundPos = center.clone().add((Math.random()-0.5)*16, 0.5, (Math.random()-0.5)*16);
                    DisplayBuilder.dustParticles(groundPos, 3, 1.0, 180, 0, 0, 0.8f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SimultaneousTripleDensity(plugin); }
    }

    // =========================================================================
    // 152. STREAM INTERSECTION FLARES -- multi-color star bursts in sky
    // =========================================================================
    public static class StreamIntersectionFlares extends EnvironmentalAttack {

        private final List<Location> flarePoints = new ArrayList<>();

        public StreamIntersectionFlares(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_intersection_flares", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            int count = 3 + (int)(Math.random() * 3);
            for (int i = 0; i < count; i++) {
                flarePoints.add(center.clone().add((Math.random()-0.5)*16, 20+Math.random()*10, (Math.random()-0.5)*16));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- streams drift toward intersection points
            if (ticksAlive <= 40) {
                if (ticksAlive % 4 == 0) {
                    for (Location fp : flarePoints) {
                        w.spawnParticle(Particle.END_ROD, fp, 6, 1.0, 0.5, 1.0, 0.01);
                        w.spawnParticle(Particle.WITCH, fp, 4, 0.5, 0.5, 0.5, 0);
                    }
                }
                return;
            }

            int flareTick = ticksAlive - 40;

            // Flare bursts for 80 ticks
            if (flareTick <= 80 && flareTick % 4 == 0) {
                for (Location fp : flarePoints) {
                    // Multi-color star burst
                    w.spawnParticle(Particle.WITCH, fp, 30, 1.0, 1.0, 1.0, 0);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, fp, 25, 1.0, 1.0, 1.0, 0.02);
                    w.spawnParticle(Particle.END_ROD, fp, 30, 1.0, 1.0, 1.0, 0.01);
                    DisplayBuilder.playSound(fp, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 1.2f);
                }
            }

            // Shadow zone damage (ground below flare points)
            if (flareTick <= 80 && flareTick % 20 == 0) {
                for (Location fp : flarePoints) {
                    Location groundShadow = center.clone().add(fp.getX()-center.getX(), 0.5, fp.getZ()-center.getZ());
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(groundShadow) <= 9.0) {
                            p.damage(14.0);
                        }
                    }
                }
            }

            // Afterglow
            if (flareTick > 80 && flareTick % 4 == 0) {
                for (Location fp : flarePoints) {
                    w.spawnParticle(Particle.END_ROD, fp, 10, 1.5, 1.5, 1.5, 0.005);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new StreamIntersectionFlares(plugin); }
    }

    // =========================================================================
    // 153. SKY STORM: 20-SECOND RANDOM CHAOS
    // =========================================================================
    public static class SkyStormChaos extends EnvironmentalAttack {

        private boolean chaosActive = false;

        public SkyStormChaos(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_storm_chaos", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(460);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Freeze: 30 ticks -- streams go static
            if (ticksAlive <= 30) {
                if (ticksAlive == 1) DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.2f, 2.0f);
                return;
            }
            // Vibrate: 10 ticks
            if (ticksAlive <= 40) return;

            // 400 ticks of chaos
            if (!chaosActive) {
                chaosActive = true;
                DisplayBuilder.playSound(center, Sound.AMBIENT_BASALT_DELTAS_LOOP, 0.5f, 0.6f);
            }

            int chaosTick = ticksAlive - 40;
            if (chaosTick > 400) return;

            // Random directional streams and columns
            if (chaosTick % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random()-0.5)*20;
                    double oy = 5+Math.random()*20;
                    double oz = (Math.random()-0.5)*20;
                    Location streamPos = center.clone().add(ox, oy, oz);
                    switch ((int)(Math.random()*5)) {
                        case 0: w.spawnParticle(Particle.WITCH, streamPos, 4, 0.5, 0.5, 0.5, 0); break;
                        case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 3, 0.5, 0.5, 0.5, 0.01); break;
                        case 2: w.spawnParticle(Particle.END_ROD, streamPos, 4, 0.5, 0.5, 0.5, 0.005); break;
                        case 3: w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 3, 0.5, 0.5, 0.5, 0.005); break;
                        default: DisplayBuilder.dustParticles(streamPos, 3, 0.5, 180, 0, 0, 1.0f); break;
                    }
                }
                // 15% chance downward columns
                if (Math.random() < 0.15) {
                    double ox = (Math.random()-0.5)*18;
                    double oz = (Math.random()-0.5)*18;
                    for (int y = 0; y <= 20; y += 3) {
                        Location colPos = center.clone().add(ox, y, oz);
                        w.spawnParticle(Particle.END_ROD, colPos, 6, 0.3, 0.5, 0.3, 0.01);
                    }
                }
            }

            // Column ground strike damage
            if (chaosTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (Math.random() < 0.15 && p.getLocation().distanceSquared(center) <= 100.0) {
                        p.damage(14.0);
                        DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_STONE_FALL, 0.3f, 1.4f);
                    }
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SkyStormChaos(plugin); }
    }

    // =========================================================================
    // 154. THE SKY OPENS -- convergence explosion + shockwave
    // =========================================================================
    public static class TheSkyOpens extends EnvironmentalAttack {

        private boolean exploded = false;
        private double shockwaveRadius = 0;

        public TheSkyOpens(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_sky_opens", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1600);
            config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Convergence buildup: 160 ticks -- streams bend toward Y+80 center
            if (ticksAlive <= 160) {
                double convergence = ticksAlive / 160.0;
                if (ticksAlive % 3 == 0) {
                    int particleCount = (int)(5 + convergence * 30);
                    Location convergePt = center.clone().add(0, 40, 0);
                    for (int i = 0; i < particleCount; i++) {
                        double spread = (1.0 - convergence) * 15;
                        Location srcPos = convergePt.clone().add((Math.random()-0.5)*spread*2, (Math.random()-0.5)*spread, (Math.random()-0.5)*spread*2);
                        switch ((int)(Math.random()*5)) {
                            case 0: w.spawnParticle(Particle.WITCH, srcPos, 3, 0.3, 0.3, 0.3, 0); break;
                            case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, srcPos, 2, 0.3, 0.3, 0.3, 0.01); break;
                            case 2: w.spawnParticle(Particle.END_ROD, srcPos, 3, 0.3, 0.3, 0.3, 0.005); break;
                            case 3: w.spawnParticle(Particle.DRAGON_BREATH, srcPos, 2, 0.3, 0.3, 0.3, 0.005); break;
                            default: DisplayBuilder.dustParticles(srcPos, 2, 0.3, 180, 0, 0, 1.0f); break;
                        }
                    }
                }
                // Warning rings
                if (ticksAlive == 140) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.4f);
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), 5.0,
                            Particle.DUST, 30, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 2.0f));
                }
                if (ticksAlive == 150) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.6f);
                }
                return;
            }

            // 3-second hold at convergence point
            int holdTick = ticksAlive - 160;

            if (holdTick <= 60) {
                if (holdTick % 2 == 0) {
                    Location convergePt = center.clone().add(0, 40, 0);
                    w.spawnParticle(Particle.WITCH, convergePt, 60, 2.0, 2.0, 2.0, 0);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, convergePt, 45, 2.0, 2.0, 2.0, 0.02);
                    w.spawnParticle(Particle.END_ROD, convergePt, 75, 2.0, 2.0, 2.0, 0.01);
                    w.spawnParticle(Particle.DRAGON_BREATH, convergePt, 30, 2.0, 2.0, 2.0, 0.01);
                    DisplayBuilder.dustParticles(convergePt, 40, 2.0, 180, 0, 0, 3.0f);
                }
                return;
            }

            // Explosion
            if (!exploded) {
                exploded = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.2f);
                Location convergePt = center.clone().add(0, 40, 0);
                // Massive burst
                w.spawnParticle(Particle.END_ROD, convergePt, 200, 15.0, 15.0, 15.0, 0.05);
                w.spawnParticle(Particle.WITCH, convergePt, 150, 10.0, 10.0, 10.0, 0);
            }

            // Ground shockwave at tick 70 after hold
            int shockTick = holdTick - 70;
            if (shockTick >= 0 && shockTick <= 10) {
                shockwaveRadius = shockTick * 2.0;
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), shockwaveRadius,
                        Particle.DUST, (int)(shockwaveRadius * 8),
                        new Particle.DustOptions(Color.fromRGB(255, 255, 255), 3.0f));

                if (shockTick == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.6f);
                }

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(Math.pow(p.getLocation().getX()-center.getX(),2)+Math.pow(p.getLocation().getZ()-center.getZ(),2));
                    if (Math.abs(dist - shockwaveRadius) <= 2.0 && p.getLocation().getY() <= center.getY()+2) {
                        p.damage(14.0);
                    }
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TheSkyOpens(plugin); }
    }

    // =========================================================================
    // 155. CRIMSON RAINFALL -- four columns of crimson DUST descend
    // =========================================================================
    public static class CrimsonRainfallColumns extends EnvironmentalAttack {

        private final List<Location> columnPositions = new ArrayList<>();
        private boolean columnsFiring = false;

        public CrimsonRainfallColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_rainfall_columns", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            columnPositions.add(center.clone().add(-6, 0, -6));
            columnPositions.add(center.clone().add(6, 0, -6));
            columnPositions.add(center.clone().add(6, 0, 6));
            columnPositions.add(center.clone().add(-6, 0, 6));
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- crimson streams slow and rotate toward vertical
            if (ticksAlive <= 60) {
                double angle = 90.0 * (ticksAlive / 60.0);
                if (ticksAlive % 3 == 0) {
                    for (Location col : columnPositions) {
                        Location skyPos = col.clone().add(0, 30 - angle * 0.2, 0);
                        DisplayBuilder.dustParticles(skyPos, 8, 1.0, 180, 0, 0, 2.0f);
                    }
                }
                return;
            }

            // Columns vertical: 60 ticks (3 seconds)
            if (!columnsFiring) {
                columnsFiring = true;
                for (Location col : columnPositions) {
                    DisplayBuilder.playSound(col, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.5f);
                }
            }

            int colTick = ticksAlive - 60;
            if (colTick <= 60) {
                if (colTick % 2 == 0) {
                    for (Location col : columnPositions) {
                        for (int y = 0; y <= 30; y += 3) {
                            DisplayBuilder.dustParticles(col.clone().add(0, y, 0), 15, 1.5, 180, 0, 0, 2.5f);
                        }
                    }
                }

                // Damage inside columns
                if (colTick % 20 == 0) {
                    for (Location col : columnPositions) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - col.getX();
                            double dz = p.getLocation().getZ() - col.getZ();
                            if (dx*dx + dz*dz <= 2.25) {
                                p.damage(16.0);
                            }
                        }
                    }
                }
            }

            // Permanent scorched marks
            if (colTick > 60 && colTick % 8 == 0) {
                for (Location col : columnPositions) {
                    DisplayBuilder.dustParticles(col.clone().add(0, 0.3, 0), 5, 1.0, 100, 0, 0, 0.5f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrimsonRainfallColumns(plugin); }
    }

    // =========================================================================
    // 156. STREAM LAYER SEPARATION -- altitude bands weaponized
    // =========================================================================
    public static class StreamLayerSeparation extends EnvironmentalAttack {

        private boolean descending = false;

        public StreamLayerSeparation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_layer_separation", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sorting: 60 ticks -- streams separate into altitude bands
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    // 5 distinct layers forming
                    double[] baseHeights = {5, 16, 27, 41, 56};
                    Particle[] types = {Particle.DRAGON_BREATH, null, Particle.WITCH, Particle.TOTEM_OF_UNDYING, Particle.END_ROD};
                    for (int i = 0; i < 5; i++) {
                        for (int j = 0; j < 3; j++) {
                            Location layerPos = center.clone().add((Math.random()-0.5)*16, baseHeights[i]+Math.random()*5, (Math.random()-0.5)*16);
                            if (types[i] != null) {
                                w.spawnParticle(types[i], layerPos, 4, 1.0, 0.5, 1.0, 0.005);
                            } else {
                                DisplayBuilder.dustParticles(layerPos, 4, 1.0, 180, 0, 0, 1.5f);
                            }
                        }
                    }
                }
                if (ticksAlive == 55) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.8f);
                }
                return;
            }

            // Descent: 80 ticks -- all bands lower
            int descTick = ticksAlive - 60;
            if (descTick <= 80) {
                double descent = descTick * 0.15;
                if (descTick % 3 == 0) {
                    // Dragon breath at floor level
                    for (int i = 0; i < 6; i++) {
                        Location dbPos = center.clone().add((Math.random()-0.5)*16, 5-descent*3, (Math.random()-0.5)*16);
                        w.spawnParticle(Particle.DRAGON_BREATH, dbPos, 8, 2.0, 1.0, 2.0, 0.01);
                    }
                }

                // Dragon breath floor damage
                if (descTick > 40 && descTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().getY() <= center.getY() + 3) {
                            p.damage(14.0);
                        }
                    }
                }
            }

            // Hold 80 ticks, then ascend
            if (descTick > 80 && descTick <= 160) {
                if (descTick % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        Location dbPos = center.clone().add((Math.random()-0.5)*16, 1 + Math.random()*3, (Math.random()-0.5)*16);
                        w.spawnParticle(Particle.DRAGON_BREATH, dbPos, 6, 2.0, 0.5, 2.0, 0.005);
                    }
                }
            }

            // Post-event fog residual
            if (descTick > 160 && descTick % 8 == 0) {
                for (int i = 0; i < 3; i++) {
                    Location fogPos = center.clone().add((Math.random()-0.5)*16, 0.5, (Math.random()-0.5)*16);
                    w.spawnParticle(Particle.DRAGON_BREATH, fogPos, 3, 1.0, 0.3, 1.0, 0.002);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new StreamLayerSeparation(plugin); }
    }

    // =========================================================================
    // 157. VOID THREAD AMPLIFICATION -- SPELL_WITCH from 15 to 45 streams
    // =========================================================================
    public static class VoidThreadAmplification extends EnvironmentalAttack {

        public VoidThreadAmplification(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_thread_amplification", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(380); config.setCooldownTicks(2000); config.setTicksBetweenDamage(999);
        }

        @Override protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // New SPELL_WITCH lines appear over 80 ticks
            int activeStreams = Math.min(15 + (ticksAlive / 3), 45);
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < Math.min(activeStreams / 3, 15); i++) {
                    Location pos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*15, (Math.random()-0.5)*20);
                    w.spawnParticle(Particle.WITCH, pos, 6, 1.0, 0.5, 1.0, 0);
                }
            }
            if (ticksAlive == 5) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new VoidThreadAmplification(plugin); }
    }

    // =========================================================================
    // 158. SANCTUM GOLD SURGE -- TOTEM streams flare fat and bright
    // =========================================================================
    public static class SanctumGoldSurge extends EnvironmentalAttack {

        public SanctumGoldSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_gold_surge", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(280); config.setCooldownTicks(1600); config.setTicksBetweenDamage(999);
        }

        @Override protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    Location pos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*15, (Math.random()-0.5)*20);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, pos, 8, 1.0, 0.5, 1.0, 0.02);
                }
                // Ground gold reflection
                for (int i = 0; i < 4; i++) {
                    Location gPos = center.clone().add((Math.random()-0.5)*16, 0.5, (Math.random()-0.5)*16);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, gPos, 4, 1.0, 0.3, 1.0, 0.005);
                }
            }
            if (ticksAlive == 5) DisplayBuilder.playSound(center, Sound.BLOCK_GILDED_BLACKSTONE_PLACE, 0.5f, 0.4f);
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SanctumGoldSurge(plugin); }
    }

    // =========================================================================
    // 159. WHITE LATTICE EXPLOSION -- END_ROD to 110 streams, white-out
    // =========================================================================
    public static class WhiteLatticeExplosion extends EnvironmentalAttack {

        public WhiteLatticeExplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("white_lattice_explosion", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(340); config.setCooldownTicks(2400); config.setTicksBetweenDamage(999);
        }

        @Override protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Massive END_ROD saturation
            if (ticksAlive % 2 == 0) {
                int count = Math.min(10 + ticksAlive / 2, 50);
                for (int i = 0; i < count; i++) {
                    Location pos = center.clone().add((Math.random()-0.5)*20, 10+Math.random()*20, (Math.random()-0.5)*20);
                    w.spawnParticle(Particle.END_ROD, pos, 6, 1.0, 0.5, 1.0, 0.01);
                }
            }
            if (ticksAlive == 5) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.0f);
            }
            // Ground flash at peak
            if (ticksAlive == 120) {
                for (int x = -8; x <= 8; x += 2) {
                    for (int z = -8; z <= 8; z += 2) {
                        w.spawnParticle(Particle.END_ROD, center.clone().add(x, 0.5, z), 4, 0.5, 0.3, 0.5, 0.01);
                    }
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new WhiteLatticeExplosion(plugin); }
    }

    // =========================================================================
    // 160. PURPLE CLOUD DESCENT -- DRAGON_BREATH fog descends to floor
    // =========================================================================
    public static class PurpleCloudDescent extends EnvironmentalAttack {

        private double cloudHeight = 35;

        public PurpleCloudDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("purple_cloud_descent", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDurationTicks(320); config.setCooldownTicks(1400); config.setTicksBetweenDamage(999);
        }

        @Override protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent: 1 block per 4 ticks
            if (ticksAlive <= 120) cloudHeight = 35 - ticksAlive * 0.25;
            // Hold at floor: 40 ticks
            else if (ticksAlive <= 160) cloudHeight = 2;
            // Ascend
            else cloudHeight = 2 + (ticksAlive - 160) * 0.4;

            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    Location cloudPos = center.clone().add((Math.random()-0.5)*20, cloudHeight + Math.random()*5, (Math.random()-0.5)*20);
                    w.spawnParticle(Particle.DRAGON_BREATH, cloudPos, 10, 3.0, 2.0, 3.0, 0.005);
                }
            }

            if (ticksAlive == 5) DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.7f, 0.3f);
            if (ticksAlive == 120) DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.3f, 0.2f);
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PurpleCloudDescent(plugin); }
    }
}
