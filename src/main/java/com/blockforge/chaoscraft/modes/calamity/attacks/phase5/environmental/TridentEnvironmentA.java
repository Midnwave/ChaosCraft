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
 * Phase 4E Environmental -- TRIDENT ENVIRONMENT A
 * Attacks #101-110: Electrified floor tridents, geysers, monolith wall launch,
 * trident garden, trident rain, orbit field, pillar eruption, cage convergence,
 * horizontal sweep, and pinwheel.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 10.0-20.0 HP (escalating)
 * - No status effects applied directly
 * - Trident-themed environmental hazards using arena-embedded trident positions
 */
public final class TridentEnvironmentA {

    private TridentEnvironmentA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ElectrifiedFloorTridents(plugin));
        registry.register(new TridentGeysers(plugin));
        registry.register(new MonolithWallLaunch(plugin));
        registry.register(new TridentGarden(plugin));
        registry.register(new TridentRain(plugin));
        registry.register(new TridentOrbitField(plugin));
        registry.register(new TridentPillarEruption(plugin));
        registry.register(new TridentCageConvergence(plugin));
        registry.register(new HorizontalTridentSweep(plugin));
        registry.register(new TridentPinwheel(plugin));
    }

    // =========================================================================
    // 101. ELECTRIFIED FLOOR TRIDENTS -- embedded tridents discharge electric pulses
    // =========================================================================
    public static class ElectrifiedFloorTridents extends EnvironmentalAttack {

        private final List<Location> tridentPositions = new ArrayList<>();
        private boolean discharged = false;

        public ElectrifiedFloorTridents(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("electrified_floor_tridents", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4-8 embedded trident positions scattered across the arena
            int count = 4 + (int) (Math.random() * 5);
            for (int i = 0; i < count; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 3 + Math.random() * 7;
                tridentPositions.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- sparks arc between trident tips and ground
            if (ticksAlive <= 60) {
                double intensity = Math.min(ticksAlive / 40.0, 1.0);
                if (ticksAlive % 3 == 0) {
                    for (Location trident : tridentPositions) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, trident.clone().add(0, 1.2, 0),
                                (int) (8 * intensity), 0.5, 0.5, 0.5, 0.05);
                        w.spawnParticle(Particle.CRIT, trident.clone().add(0, 0.8, 0),
                                (int) (5 * intensity), 0.3, 0.3, 0.3, 0.02);
                    }
                }
                if (ticksAlive == 50) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 0.9f);
                }
                return;
            }

            // Discharge
            if (!discharged) {
                discharged = true;
                for (Location trident : tridentPositions) {
                    DisplayBuilder.playSound(trident, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.8f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2.0f);
            }

            int dischargeTick = ticksAlive - 60;

            // Electric pulse: 10 ticks per trident
            if (dischargeTick <= 10) {
                for (Location trident : tridentPositions) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, trident.clone().add(0, 0.5, 0),
                            30, 1.5, 1.0, 1.5, 0.1);
                    w.spawnParticle(Particle.CRIT, trident.clone().add(0, 0.8, 0),
                            15, 0.25, 0.5, 0.25, 0.03);
                    DisplayBuilder.dustParticles(trident.clone().add(0, 0.3, 0),
                            20, 1.5, 100, 200, 255, 2.0f);
                }
            }

            // Damage during discharge window
            if (dischargeTick <= 10 && dischargeTick % 5 == 0) {
                for (Location trident : tridentPositions) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - trident.getX();
                        double dz = p.getLocation().getZ() - trident.getZ();
                        double distSq = dx * dx + dz * dz;
                        if (distSq <= 9.0) {
                            p.damage(14.0);
                        }
                    }
                }
            }

            // Residual sparks post-discharge
            if (dischargeTick > 10 && dischargeTick % 10 == 0) {
                for (Location trident : tridentPositions) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, trident.clone().add(0, 0.5, 0),
                            4, 0.5, 0.5, 0.5, 0.02);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ElectrifiedFloorTridents(plugin); }
    }

    // =========================================================================
    // 102. TRIDENT GEYSERS -- random floor tiles erupt with trident blasts
    // =========================================================================
    public static class TridentGeysers extends EnvironmentalAttack {

        private final List<Location> geyserTiles = new ArrayList<>();
        private boolean erupted = false;

        public TridentGeysers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_geysers", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            int count = 3 + (int) (Math.random() * 4);
            for (int i = 0; i < count; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 2 + Math.random() * 8;
                geyserTiles.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 50 ticks -- ground boils at geyser tiles
            if (ticksAlive <= 50) {
                if (ticksAlive % 4 == 0) {
                    for (Location tile : geyserTiles) {
                        w.spawnParticle(Particle.SMOKE, tile.clone().add(0, 0.3, 0),
                                3, 0.2, 0.3, 0.2, 0.01);
                        w.spawnParticle(Particle.LAVA, tile.clone().add(0, 0.2, 0),
                                2, 0.3, 0.1, 0.3, 0);
                    }
                }
                if (ticksAlive == 40) {
                    for (Location tile : geyserTiles) {
                        DisplayBuilder.playSound(tile, Sound.BLOCK_LAVA_POP, 0.8f, 1.4f);
                    }
                }
                return;
            }

            // Eruption
            if (!erupted) {
                erupted = true;
                int stagger = 0;
                for (Location tile : geyserTiles) {
                    final int delay = stagger;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.playSound(tile, Sound.ENTITY_ARROW_SHOOT, 1.2f, 0.6f);
                        w.spawnParticle(Particle.LAVA, tile, 25, 0.3, 0.5, 0.3, 0);
                    }, delay);
                    stagger += 3;
                }
            }

            int eruptTick = ticksAlive - 50;

            // Ascending trident particles
            if (eruptTick <= 35) {
                double height = Math.min(eruptTick * 0.5, 18);
                if (eruptTick % 2 == 0) {
                    for (Location tile : geyserTiles) {
                        w.spawnParticle(Particle.CRIT, tile.clone().add(0, height, 0),
                                8, 0.2, 0.5, 0.2, 0.05);
                        DisplayBuilder.dustParticles(tile.clone().add(0, height / 2, 0),
                                6, 0.5, 255, 80, 0, 1.5f);
                    }
                }
            }

            // Landing impacts
            if (eruptTick >= 30 && eruptTick <= 40) {
                if (eruptTick % 3 == 0) {
                    for (Location tile : geyserTiles) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, tile.clone().add(0, 0.5, 0),
                                40, 1.0, 0.5, 1.0, 0.1);
                        w.spawnParticle(Particle.CLOUD, tile.clone().add(0, 0.5, 0),
                                8, 0.5, 0.5, 0.5, 0.02);
                    }
                }
            }

            // Damage during eruption window
            if (eruptTick >= 0 && eruptTick <= 40 && eruptTick % 10 == 0) {
                for (Location tile : geyserTiles) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - tile.getX();
                        double dz = p.getLocation().getZ() - tile.getZ();
                        if (dx * dx + dz * dz <= 4.0) {
                            p.damage(eruptTick <= 5 ? 18.0 : 12.0);
                            DisplayBuilder.playSound(tile, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 2.0f);
                        }
                    }
                }
            }

            // Aftermath smoke
            if (eruptTick > 40 && eruptTick % 10 == 0) {
                for (Location tile : geyserTiles) {
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, tile.clone().add(0, 0.3, 0),
                            3, 0.3, 0.3, 0.3, 0.005);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentGeysers(plugin); }
    }

    // =========================================================================
    // 103. MONOLITH WALL LAUNCH -- arena edge tridents fire inward simultaneously
    // =========================================================================
    public static class MonolithWallLaunch extends EnvironmentalAttack {

        private final List<Location> launchPoints = new ArrayList<>();
        private final List<Location> crossPoints = new ArrayList<>();
        private boolean firstWaveFired = false;
        private boolean secondWaveFired = false;

        public MonolithWallLaunch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_wall_launch", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 launch points at arena edges (cardinal + intercardinal)
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8;
                launchPoints.add(center.clone().add(Math.cos(a) * 10, 1.0, Math.sin(a) * 10));
            }
            // Cross-sweep inner ring
            for (int i = 0; i < 4; i++) {
                double a = (2 * Math.PI * i) / 4 + Math.PI / 4;
                crossPoints.add(center.clone().add(Math.cos(a) * 5, 1.0, Math.sin(a) * 5));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- monoliths vibrate, tridents slide outward
            if (ticksAlive <= 80) {
                if (ticksAlive % 5 == 0) {
                    for (Location pt : launchPoints) {
                        DisplayBuilder.dustParticles(pt, 8, 0.5, 0, 0, 0, 1.5f);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, pt, 5, 0.3, 0.5, 0.3, 0.02);
                    }
                }
                if (ticksAlive == 70) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 0.5f);
                }
                return;
            }

            // First wave: all 8 fire inward
            if (!firstWaveFired) {
                firstWaveFired = true;
                for (Location pt : launchPoints) {
                    DisplayBuilder.playSound(pt, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 0.7f, 1.1f);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pt, 25, 0.3, 0.3, 0.3, 0.1);
                }
            }

            int waveTick = ticksAlive - 80;

            // First wave travel: 13 ticks at 1.5 blocks/tick
            if (waveTick <= 13) {
                double progress = waveTick * 1.5;
                for (Location pt : launchPoints) {
                    double dx = center.getX() - pt.getX();
                    double dz = center.getZ() - pt.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist == 0) continue;
                    double nx = dx / dist;
                    double nz = dz / dist;
                    Location tridentPos = pt.clone().add(nx * progress, 0, nz * progress);
                    DisplayBuilder.dustParticles(tridentPos, 12, 0.3, 200, 0, 50, 1.5f);
                    w.spawnParticle(Particle.CRIT, tridentPos, 6, 0.2, 0.2, 0.2, 0.05);

                    // Damage check
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(tridentPos) <= 2.25) {
                            p.damage(10.0);
                        }
                    }
                }
            }

            // Second wave from inner ring at tick 8
            if (waveTick == 16 && !secondWaveFired) {
                secondWaveFired = true;
                for (Location pt : crossPoints) {
                    DisplayBuilder.playSound(pt, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 0.5f, 1.3f);
                }
            }

            if (waveTick >= 16 && waveTick <= 26) {
                double progress = (waveTick - 16) * 1.5;
                for (Location pt : crossPoints) {
                    double dx = center.getX() - pt.getX();
                    double dz = center.getZ() - pt.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist == 0) continue;
                    // Fire toward far edge (opposite direction)
                    double nx = -dx / dist;
                    double nz = -dz / dist;
                    Location tridentPos = pt.clone().add(nx * progress, 0, nz * progress);
                    DisplayBuilder.dustParticles(tridentPos, 10, 0.3, 200, 0, 50, 1.5f);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(tridentPos) <= 2.25) {
                            p.damage(15.0);
                        }
                    }
                }
            }

            // Impact smoke at center
            if (waveTick == 14) {
                w.spawnParticle(Particle.SMOKE, center, 20, 1.0, 0.5, 1.0, 0.03);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MonolithWallLaunch(plugin); }
    }

    // =========================================================================
    // 104. TRIDENT GARDEN -- 16 tridents rise from floor in 4x4 grid, hover, fire
    // =========================================================================
    public static class TridentGarden extends EnvironmentalAttack {

        private final List<Location> gardenTiles = new ArrayList<>();
        private boolean volley = false;

        public TridentGarden(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_garden", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(13.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // 4x4 grid centered on arena
            for (int x = -2; x <= 1; x++) {
                for (int z = -2; z <= 1; z++) {
                    gardenTiles.add(center.clone().add(x * 3 + 1.5, 0, z * 3 + 1.5));
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks -- soul fire columns grow
            if (ticksAlive <= 100) {
                double height = Math.min(ticksAlive * 0.08, 8);
                if (ticksAlive % 4 == 0) {
                    for (Location tile : gardenTiles) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, tile.clone().add(0, height / 2, 0),
                                20, 0.2, height / 2, 0.2, 0.01);
                    }
                }
                if (ticksAlive == 90) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 0.8f);
                    // Gold ring expanding
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), 10.0,
                            Particle.TOTEM_OF_UNDYING, 50, null);
                }
                return;
            }

            // Hover phase: ticks 100-200 -- tridents spin at chest height
            int hoverTick = ticksAlive - 100;
            if (hoverTick <= 100) {
                if (hoverTick % 5 == 0) {
                    for (Location tile : gardenTiles) {
                        w.spawnParticle(Particle.ENCHANTED_HIT, tile.clone().add(0, 1.3, 0),
                                10, 0.3, 0.3, 0.3, 0.05);
                    }
                }
                if (hoverTick == 50) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 1.0f);
                }
                return;
            }

            // Fire volley
            if (!volley) {
                volley = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.9f);
                for (Location tile : gardenTiles) {
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, tile.clone().add(0, 1.3, 0),
                            50, 0.5, 0.5, 0.5, 0.1);
                    w.spawnParticle(Particle.END_ROD, tile.clone().add(0, 1.3, 0),
                            8, 0.2, 0.2, 0.2, 0.05);
                }
            }

            // Trident travel toward nearest player
            int fireTick = ticksAlive - 200;
            if (fireTick <= 20 && fireTick % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (Location tile : gardenTiles) {
                        if (p.getLocation().distanceSquared(tile) <= 4.0) {
                            p.damage(13.0);
                        }
                    }
                }
            }

            // Aftermath: sculk soul particles from holes
            if (fireTick > 20 && fireTick % 8 == 0) {
                for (Location tile : gardenTiles) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, tile.clone().add(0, 0.3, 0),
                            3, 0.2, 0.3, 0.2, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentGarden(plugin); }
    }

    // =========================================================================
    // 105. TRIDENT RAIN -- 12 tridents fall from overhead barrage
    // =========================================================================
    public static class TridentRain extends EnvironmentalAttack {

        private final List<Location> impactSites = new ArrayList<>();
        private boolean barrageFired = false;

        public TridentRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_rain", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // 12 random impact positions within 20x20 arena
            for (int i = 0; i < 12; i++) {
                double ox = (Math.random() - 0.5) * 18;
                double oz = (Math.random() - 0.5) * 18;
                impactSites.add(center.clone().add(ox, 0, oz));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- descending crit lines from sky
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    for (Location site : impactSites) {
                        double fallY = 25.0 - (ticksAlive * 0.3);
                        w.spawnParticle(Particle.CRIT, site.clone().add(0, fallY, 0),
                                6, 0.1, 1.0, 0.1, 0.02);
                    }
                }
                if (ticksAlive == 50) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.3f, 2.0f);
                }
                return;
            }

            // Barrage: staggered 2-tick intervals for 12 tridents
            if (!barrageFired) {
                barrageFired = true;
            }

            int barrageTick = ticksAlive - 60;

            // Each trident impacts in sequence
            for (int i = 0; i < impactSites.size(); i++) {
                int impactTime = i * 2;
                Location site = impactSites.get(i);

                // Falling trail
                if (barrageTick >= impactTime && barrageTick <= impactTime + 10) {
                    double fallProgress = (barrageTick - impactTime) / 10.0;
                    double tridentY = 25.0 * (1.0 - fallProgress);
                    w.spawnParticle(Particle.CRIT, site.clone().add(0, tridentY, 0),
                            10, 0.1, 0.5, 0.1, 0.03);
                    DisplayBuilder.dustParticles(site.clone().add(0, tridentY, 0),
                            8, 0.3, 200, 200, 255, 1.5f);
                }

                // Impact
                if (barrageTick == impactTime + 10) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, site.clone().add(0, 0.5, 0),
                            35, 0.8, 0.5, 0.8, 0.1);
                    w.spawnParticle(Particle.CLOUD, site.clone().add(0, 0.5, 0),
                            8, 0.5, 0.3, 0.5, 0.02);
                    DisplayBuilder.playSound(site, Sound.ITEM_TRIDENT_HIT_GROUND, 1.2f, 1.0f);

                    // Damage in splash radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double distSq = p.getLocation().distanceSquared(site);
                        if (distSq <= 1.0) {
                            p.damage(16.0);
                        } else if (distSq <= 2.25) {
                            p.damage(10.0);
                        }
                    }
                }
            }

            // Aftermath: charged hazards at impact sites
            if (barrageTick > 30 && barrageTick % 8 == 0) {
                for (Location site : impactSites) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, site.clone().add(0, 0.5, 0),
                            4, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentRain(plugin); }
    }

    // =========================================================================
    // 106. TRIDENT ORBIT FIELD -- 6 tridents orbit then ricochet
    // =========================================================================
    public static class TridentOrbitField extends EnvironmentalAttack {

        private double orbitAngle = 0;
        private double orbitSpeed = 15;
        private boolean broken = false;

        public TridentOrbitField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_orbit_field", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(11.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(240);
            config.setCooldownTicks(1700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup + orbit phase: 80 ticks -- 6 tridents orbit at 9-block radius
            if (ticksAlive <= 80) {
                orbitSpeed = 15 + ticksAlive * 0.2;
                orbitAngle += Math.toRadians(orbitSpeed);

                for (int i = 0; i < 6; i++) {
                    double a = orbitAngle + (2 * Math.PI * i) / 6;
                    Location tridentPos = center.clone().add(Math.cos(a) * 9, 1.5, Math.sin(a) * 9);

                    if (ticksAlive % 2 == 0) {
                        w.spawnParticle(Particle.DRAGON_BREATH, tridentPos, 15, 0.2, 0.2, 0.2, 0.01);
                        w.spawnParticle(Particle.WITCH, tridentPos, 8, 0.3, 0.3, 0.3, 0);
                    }

                    // Graze damage
                    if (ticksAlive % 20 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(tridentPos) <= 1.0) {
                                p.damage(6.0);
                            }
                        }
                    }
                }

                if (ticksAlive == 70) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND, 0.5f, 1.2f);
                }
                return;
            }

            // Break orbit and fire outward
            if (!broken) {
                broken = true;
                DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.1f);

                for (int i = 0; i < 6; i++) {
                    double a = orbitAngle + (2 * Math.PI * i) / 6;
                    Location breakPos = center.clone().add(Math.cos(a) * 9, 1.5, Math.sin(a) * 9);
                    w.spawnParticle(Particle.WITCH, breakPos, 30, 0.5, 0.5, 0.5, 0.1);
                }
            }

            int breakTick = ticksAlive - 80;

            // Ricochet: 18 secondary projectiles
            if (breakTick <= 20) {
                for (int i = 0; i < 18; i++) {
                    double a = (2 * Math.PI * i) / 18;
                    double progress = breakTick * 1.2;
                    Location ricoPos = center.clone().add(Math.cos(a) * progress, 1.0, Math.sin(a) * progress);

                    if (breakTick % 2 == 0) {
                        w.spawnParticle(Particle.CRIT, ricoPos, 5, 0.1, 0.1, 0.1, 0.03);
                        DisplayBuilder.dustParticles(ricoPos, 4, 0.2, 200, 0, 50, 1.0f);
                    }

                    if (breakTick % 5 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(ricoPos) <= 2.25) {
                                p.damage(11.0);
                            }
                        }
                    }
                }
            }

            // Wall embed sounds
            if (breakTick == 20) {
                DisplayBuilder.playSound(center, Sound.BLOCK_METAL_HIT, 0.8f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentOrbitField(plugin); }
    }

    // =========================================================================
    // 107. TRIDENT PILLAR ERUPTION -- 3 tiles erupt 8-trident columns
    // =========================================================================
    public static class TridentPillarEruption extends EnvironmentalAttack {

        private final List<Location> eruptionTiles = new ArrayList<>();
        private final List<BlockDisplayHandle> crackDisplays = new ArrayList<>();
        private boolean erupted = false;

        public TridentPillarEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_pillar_eruption", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(18.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(1300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 3; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 3 + Math.random() * 7;
                eruptionTiles.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- tiles crack
            if (ticksAlive <= 60) {
                double intensity = ticksAlive / 60.0;
                if (ticksAlive % 3 == 0) {
                    for (Location tile : eruptionTiles) {
                        w.spawnParticle(Particle.BLOCK, tile.clone().add(0, 0.3, 0),
                                (int) (15 * intensity), 0.4, 0.3, 0.4, 0,
                                Material.OBSIDIAN.createBlockData());
                        w.spawnParticle(Particle.LAVA, tile.clone().add(0, 0.2, 0),
                                (int) (3 * intensity), 0.3, 0.1, 0.3, 0);
                    }
                }
                if (ticksAlive == 30 || ticksAlive == 50) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.4f, 2.0f);
                }
                return;
            }

            // Eruption
            if (!erupted) {
                erupted = true;
                for (Location tile : eruptionTiles) {
                    DisplayBuilder.playSound(tile, Sound.BLOCK_BASALT_BREAK, 0.9f, 0.7f);
                    w.spawnParticle(Particle.BLOCK, tile, 40, 0.5, 1.0, 0.5, 0,
                            Material.OBSIDIAN.createBlockData());

                    // Crack display
                    BlockDisplayHandle crack = displayBuilder.spawnBlock(
                            tile.clone().add(0, 0.1, 0), Material.CRYING_OBSIDIAN);
                    crack.scale(1.2f, 0.1f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
                    crackDisplays.add(crack);
                    spawnedEntities.add(crack.entity());
                }
            }

            int eruptTick = ticksAlive - 60;

            // 8 tridents per pillar, 4-tick stagger
            if (eruptTick <= 32) {
                int tridentIndex = eruptTick / 4;
                if (eruptTick % 4 == 0 && tridentIndex < 8) {
                    for (Location tile : eruptionTiles) {
                        double height = tridentIndex * 2;
                        double spreadAngle = (Math.random() - 0.5) * Math.toRadians(30);
                        Location tridentPos = tile.clone().add(
                                Math.sin(spreadAngle) * height * 0.2, height, Math.cos(spreadAngle) * height * 0.2);

                        w.spawnParticle(Particle.LAVA, tridentPos, 6, 0.2, 0.3, 0.2, 0);
                        DisplayBuilder.dustParticles(tridentPos, 8, 0.4, 200, 0, 50, 1.5f);
                        w.spawnParticle(Particle.ENCHANTED_HIT, tridentPos, 5, 0.3, 0.3, 0.3, 0.05);

                        DisplayBuilder.playSound(tile, Sound.ITEM_TRIDENT_HIT_GROUND, 0.6f, 1.3f);
                    }
                }
            }

            // Ground zone damage
            if (eruptTick >= 0 && eruptTick <= 32 && eruptTick % 10 == 0) {
                for (Location tile : eruptionTiles) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - tile.getX();
                        double dz = p.getLocation().getZ() - tile.getZ();
                        if (dx * dx + dz * dz <= 2.25 && p.getLocation().getY() <= tile.getY() + 16) {
                            p.damage(18.0);
                        }
                    }
                }
            }

            // Aftermath: crying obsidian drips
            if (eruptTick > 40 && eruptTick % 6 == 0) {
                for (Location tile : eruptionTiles) {
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, tile.clone().add(0, 0.5, 0),
                            3, 0.3, 0.3, 0.3, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentPillarEruption(plugin); }
    }

    // =========================================================================
    // 108. TRIDENT CAGE CONVERGENCE -- 8 tridents converge on center
    // =========================================================================
    public static class TridentCageConvergence extends EnvironmentalAttack {

        private final List<Location> launchPositions = new ArrayList<>();
        private boolean firstPass = false;
        private boolean secondPass = false;

        public TridentCageConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_cage_convergence", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(2400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            // 8 positions at arena edge (cardinal + intercardinal)
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8;
                launchPositions.add(center.clone().add(Math.cos(a) * 10, 1.2, Math.sin(a) * 10));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks -- tridents pulse with magenta halos, end_rod chains
            if (ticksAlive <= 100) {
                if (ticksAlive % 5 == 0) {
                    for (Location pos : launchPositions) {
                        w.spawnParticle(Particle.WITCH, pos, 8, 0.3, 0.3, 0.3, 0);
                    }
                    // END_ROD connecting lines
                    for (int i = 0; i < launchPositions.size(); i++) {
                        Location a = launchPositions.get(i);
                        Location b = launchPositions.get((i + 1) % launchPositions.size());
                        int steps = 12;
                        for (int s = 0; s < steps; s++) {
                            double t = s / (double) steps;
                            Location line = a.clone().add(
                                    (b.getX() - a.getX()) * t, (b.getY() - a.getY()) * t, (b.getZ() - a.getZ()) * t);
                            w.spawnParticle(Particle.END_ROD, line, 1, 0, 0, 0, 0);
                        }
                    }
                }
                if (ticksAlive == 90) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.3f);
                }
                return;
            }

            int convergeTick = ticksAlive - 100;

            // First pass: converge to center in 13 ticks
            if (!firstPass && convergeTick <= 13) {
                double progress = convergeTick / 13.0;
                for (Location pos : launchPositions) {
                    Location tridentPos = pos.clone().add(
                            (center.getX() - pos.getX()) * progress,
                            (center.getY() + 1.2 - pos.getY()) * progress,
                            (center.getZ() - pos.getZ()) * progress);
                    DisplayBuilder.dustParticles(tridentPos, 8, 0.2, 100, 0, 200, 1.5f);
                }

                if (convergeTick == 13) {
                    firstPass = true;
                    DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1.2, 0),
                            80, 1.0, 1.0, 1.0, 0.05);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1.2, 0),
                            30, 2.0, 1.0, 2.0, 0.1);

                    // Damage at center
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distanceSquared(center);
                        if (dist <= 4.0) {
                            p.damage(10.0);
                        }
                    }
                }
            }

            // Second pass: re-fire through center at tick 20
            if (convergeTick >= 20 && convergeTick <= 33 && !secondPass) {
                double progress = (convergeTick - 20) / 13.0;
                for (Location pos : launchPositions) {
                    // Fire toward opposite wall
                    Location opposite = center.clone().add(
                            -(pos.getX() - center.getX()) * progress,
                            1.2,
                            -(pos.getZ() - center.getZ()) * progress);
                    DisplayBuilder.dustParticles(opposite, 6, 0.2, 100, 0, 200, 1.2f);
                }

                if (convergeTick == 26) {
                    DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 0.9f);
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1.2, 0),
                            60, 0.8, 0.8, 0.8, 0.04);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 4.0) {
                            p.damage(14.0);
                        }
                    }
                }

                if (convergeTick == 33) {
                    secondPass = true;
                }
            }

            // Aftermath: sculk soul at center
            if (convergeTick > 33 && convergeTick % 10 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0),
                        6, 0.5, 0.3, 0.5, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentCageConvergence(plugin); }
    }

    // =========================================================================
    // 109. HORIZONTAL TRIDENT SWEEP -- full-width sweep at waist height
    // =========================================================================
    public static class HorizontalTridentSweep extends EnvironmentalAttack {

        private int sweepDirection = 0; // 0 = X-axis, 1 = Z-axis
        private double sweepProgress = -10;
        private boolean firstSweep = true;
        private boolean secondSweepStarted = false;

        public HorizontalTridentSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("horizontal_trident_sweep", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            sweepDirection = Math.random() < 0.5 ? 0 : 1;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- electric spark line at sweep edge
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    for (int i = -10; i <= 10; i += 2) {
                        Location edgePos;
                        if (sweepDirection == 0) {
                            edgePos = center.clone().add(-10, 1.0, i);
                        } else {
                            edgePos = center.clone().add(i, 1.0, -10);
                        }
                        w.spawnParticle(Particle.ELECTRIC_SPARK, edgePos, 4, 0.1, 0.3, 0.1, 0.02);
                    }
                }
                if (ticksAlive == 35) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.5f);
                }
                return;
            }

            int sweepTick = ticksAlive - 40;

            // First sweep: 5 tridents at 2.0 blocks/tick across 20 blocks = 10 ticks
            if (firstSweep && sweepTick <= 10) {
                sweepProgress = -10 + sweepTick * 2.0;

                for (int i = -8; i <= 8; i += 4) {
                    Location tridentPos;
                    if (sweepDirection == 0) {
                        tridentPos = center.clone().add(sweepProgress, 1.2, i);
                    } else {
                        tridentPos = center.clone().add(i, 1.2, sweepProgress);
                    }

                    w.spawnParticle(Particle.CRIT, tridentPos, 10, 0.1, 0.2, 0.1, 0.05);
                    DisplayBuilder.dustParticles(tridentPos, 8, 0.3, 200, 0, 50, 1.2f);

                    // Damage players in sweep path
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double pPos = sweepDirection == 0 ?
                                p.getLocation().getX() - center.getX() :
                                p.getLocation().getZ() - center.getZ();
                        if (Math.abs(pPos - sweepProgress) <= 1.5 &&
                                p.getLocation().getY() >= center.getY() &&
                                p.getLocation().getY() <= center.getY() + 2.0) {
                            p.damage(16.0);
                        }
                    }
                }

                if (sweepTick == 10) {
                    firstSweep = false;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT, 0.6f, 0.8f);
                }
            }

            // Second sweep: offset by 2 blocks, fires from opposite direction
            if (!firstSweep && sweepTick >= 18 && sweepTick <= 28) {
                if (!secondSweepStarted) {
                    secondSweepStarted = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.4f, 1.2f);
                }

                double returnProgress = 10 - (sweepTick - 18) * 2.0;
                for (int i = -6; i <= 6; i += 4) {
                    Location tridentPos;
                    if (sweepDirection == 0) {
                        tridentPos = center.clone().add(returnProgress, 1.2, i + 2);
                    } else {
                        tridentPos = center.clone().add(i + 2, 1.2, returnProgress);
                    }
                    w.spawnParticle(Particle.CRIT, tridentPos, 8, 0.1, 0.2, 0.1, 0.04);
                    DisplayBuilder.dustParticles(tridentPos, 6, 0.3, 200, 0, 50, 1.0f);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double pPos = sweepDirection == 0 ?
                                p.getLocation().getX() - center.getX() :
                                p.getLocation().getZ() - center.getZ();
                        if (Math.abs(pPos - returnProgress) <= 1.5 &&
                                p.getLocation().getY() >= center.getY() &&
                                p.getLocation().getY() <= center.getY() + 2.0) {
                            p.damage(16.0);
                        }
                    }
                }
            }

            // Floor path highlight
            if (sweepTick > 28 && sweepTick % 5 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0),
                        8, 5.0, 255, 100, 0, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HorizontalTridentSweep(plugin); }
    }

    // =========================================================================
    // 110. TRIDENT PINWHEEL -- center spin splits into expanding pattern
    // =========================================================================
    public static class TridentPinwheel extends EnvironmentalAttack {

        private double spinAngle = 0;
        private boolean firstSplit = false;
        private boolean secondSplit = false;

        public TridentPinwheel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_pinwheel", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(18.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(220);
            config.setCooldownTicks(1500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 70 ticks -- center spin accelerates
            if (ticksAlive <= 70) {
                double speed = 6 + ticksAlive * 0.5;
                spinAngle += Math.toRadians(speed);

                if (ticksAlive % 2 == 0) {
                    Location spinPos = center.clone().add(0, 3, 0);
                    for (int i = 0; i < 4; i++) {
                        double a = spinAngle + (Math.PI / 2 * i);
                        Location armEnd = spinPos.clone().add(Math.cos(a) * 2, 0, Math.sin(a) * 2);
                        w.spawnParticle(Particle.DRAGON_BREATH, armEnd, 8, 0.1, 0.1, 0.1, 0.01);
                        w.spawnParticle(Particle.WITCH, armEnd, 5, 0.2, 0.2, 0.2, 0);
                    }
                }

                // Proximity damage during spin
                if (ticksAlive % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - center.getX();
                        double dz = p.getLocation().getZ() - center.getZ();
                        if (dx * dx + dz * dz <= 2.25 &&
                                Math.abs(p.getLocation().getY() - center.getY() - 3) <= 1.5) {
                            p.damage(6.0);
                        }
                    }
                }

                if (ticksAlive == 65) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 1.6f);
                }
                return;
            }

            // First split: 4 primary tridents fire outward at 90-degree intervals
            if (!firstSplit) {
                firstSplit = true;
                DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.1f, 1.0f);
            }

            int splitTick = ticksAlive - 70;

            // Primary 4 travel outward 8 blocks
            if (splitTick <= 16) {
                double dist = splitTick * 0.5;
                for (int i = 0; i < 4; i++) {
                    double a = spinAngle + (Math.PI / 2 * i);
                    Location tridentPos = center.clone().add(Math.cos(a) * dist, 1.0, Math.sin(a) * dist);
                    w.spawnParticle(Particle.CRIT, tridentPos, 8, 0.1, 0.1, 0.1, 0.05);
                    w.spawnParticle(Particle.ENCHANTED_HIT, tridentPos, 5, 0.2, 0.2, 0.2, 0.03);

                    if (splitTick % 5 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(tridentPos) <= 2.25) {
                                p.damage(18.0);
                            }
                        }
                    }
                }
            }

            // Hover at 8 blocks for 20 ticks, then second split
            if (splitTick == 36) {
                // End rod expanding ring at hover positions
                for (int i = 0; i < 4; i++) {
                    double a = spinAngle + (Math.PI / 2 * i);
                    Location hoverPos = center.clone().add(Math.cos(a) * 8, 1.0, Math.sin(a) * 8);
                    w.spawnParticle(Particle.END_ROD, hoverPos, 15, 1.0, 0.5, 1.0, 0.02);
                }
            }

            // Secondary split: 8 tridents at 45-degree offsets
            if (splitTick >= 36 && splitTick <= 50) {
                if (!secondSplit) {
                    secondSplit = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.1f);
                }

                double secondDist = 8 + (splitTick - 36) * 0.7;
                for (int i = 0; i < 8; i++) {
                    double a = spinAngle + (Math.PI / 4 * i) + Math.PI / 8;
                    Location secPos = center.clone().add(Math.cos(a) * secondDist, 1.0, Math.sin(a) * secondDist);
                    w.spawnParticle(Particle.CRIT, secPos, 4, 0.1, 0.1, 0.1, 0.03);

                    if (splitTick % 5 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(secPos) <= 2.25) {
                                p.damage(14.0);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentPinwheel(plugin); }
    }
}
