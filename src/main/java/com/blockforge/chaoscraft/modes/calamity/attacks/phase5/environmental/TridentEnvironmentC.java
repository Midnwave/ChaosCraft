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
 * Phase 4E Environmental -- TRIDENT ENVIRONMENT C
 * Attacks #121-130: Reality decay continues -- floor instability, sky stream
 * fragmentation, void echo crawlers, crimson floor tide, lattice grid eruption,
 * Dweller ember trail echoes, inheritance trail merge, boss echo indicators,
 * void fear pulse, and DoG lattice eruption.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 10.0-20.0 HP (escalating)
 * - Boss echo / reality decay themed
 */
public final class TridentEnvironmentC {

    private TridentEnvironmentC() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FloorInstabilityWave(plugin));
        registry.register(new SkyStreamFragmentation(plugin));
        registry.register(new VoidEchoCrawlers(plugin));
        registry.register(new CrimsonFloorTide(plugin));
        registry.register(new LatticeGridEruption(plugin));
        registry.register(new DwellerEmberEchoes(plugin));
        registry.register(new InheritanceTrailMerge(plugin));
        registry.register(new BossEchoIndicators(plugin));
        registry.register(new VoidFearPulse(plugin));
        registry.register(new DoGLatticeEruption(plugin));
    }

    // =========================================================================
    // 121. FLOOR INSTABILITY WAVE -- ripple of tiles shifting vertically
    // =========================================================================
    public static class FloorInstabilityWave extends EnvironmentalAttack {

        private double waveRadius = 0;
        private boolean waveStarted = false;

        public FloorInstabilityWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_instability_wave", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(1100);
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

            // Buildup: 50 ticks -- center rumbles
            if (ticksAlive <= 50) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0),
                            12, 2.0, 120, 80, 50, 1.5f);
                    w.spawnParticle(Particle.BLOCK, center.clone().add(0, 0.2, 0),
                            8, 1.5, 0.2, 1.5, 0, Material.STONE.createBlockData());
                }
                if (ticksAlive == 45) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 0.8f, 0.4f);
                }
                return;
            }

            // Wave propagation
            if (!waveStarted) {
                waveStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
            }

            waveRadius += 0.6;

            if (waveRadius <= 12) {
                // Wave ring particles
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), waveRadius,
                        Particle.BLOCK, (int) (waveRadius * 4),
                        Material.END_STONE.createBlockData());
                DisplayBuilder.particleRing(center.clone().add(0, 0.8, 0), waveRadius,
                        Particle.SMOKE, (int) (waveRadius * 2), null);

                // Damage players in wave ring
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (Math.abs(dist - waveRadius) <= 2.0 && p.getLocation().getY() <= center.getY() + 2) {
                        p.damage(10.0);
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 0.3, 0)));
                    }
                }
            }

            // Aftermath: unsettled floor particles
            int waveTick = ticksAlive - 50;
            if (waveTick > 20 && waveTick % 10 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = Math.random() * 10;
                    w.spawnParticle(Particle.SMOKE, center.clone().add(Math.cos(a) * d, 0.3, Math.sin(a) * d),
                            2, 0.3, 0.2, 0.3, 0.005);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloorInstabilityWave(plugin); }
    }

    // =========================================================================
    // 122. SKY STREAM FRAGMENTATION -- streams break into chaotic pieces
    // =========================================================================
    public static class SkyStreamFragmentation extends EnvironmentalAttack {

        private boolean fragmentActive = false;

        public SkyStreamFragmentation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_stream_fragmentation", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1400);
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

            // Buildup: 40 ticks -- streams stutter
            if (ticksAlive <= 40) {
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < 3; i++) {
                        Location skyPos = center.clone().add(
                                (Math.random() - 0.5) * 20, 15 + Math.random() * 10, (Math.random() - 0.5) * 20);
                        w.spawnParticle(Particle.END_ROD, skyPos, 8, 1.0, 0.5, 1.0, 0.02);
                        w.spawnParticle(Particle.WITCH, skyPos, 5, 0.5, 0.5, 0.5, 0);
                    }
                }
                return;
            }

            // Fragmentation: stream debris falls
            if (!fragmentActive) {
                fragmentActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.6f);
            }

            int fragTick = ticksAlive - 40;

            if (fragTick <= 120) {
                // Random stream fragment debris descending
                if (fragTick % 3 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double ox = (Math.random() - 0.5) * 18;
                        double oz = (Math.random() - 0.5) * 18;
                        double fallY = 12 - (fragTick % 24) * 0.5;
                        Location fragPos = center.clone().add(ox, fallY, oz);

                        // Random stream type
                        int type = (int) (Math.random() * 5);
                        switch (type) {
                            case 0: w.spawnParticle(Particle.WITCH, fragPos, 6, 0.3, 0.3, 0.3, 0); break;
                            case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, fragPos, 4, 0.3, 0.3, 0.3, 0.02); break;
                            case 2: w.spawnParticle(Particle.END_ROD, fragPos, 5, 0.3, 0.3, 0.3, 0.01); break;
                            case 3: w.spawnParticle(Particle.DRAGON_BREATH, fragPos, 6, 0.3, 0.3, 0.3, 0.01); break;
                            default: DisplayBuilder.dustParticles(fragPos, 6, 0.3, 200, 0, 50, 1.5f); break;
                        }
                    }
                }

                // Damage from falling fragments
                if (fragTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distanceSquared(center);
                        if (dist <= 81.0 && Math.random() < 0.3) {
                            p.damage(10.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyStreamFragmentation(plugin); }
    }

    // =========================================================================
    // 123. VOID ECHO CRAWLERS -- dark tendrils creep from arena edges
    // =========================================================================
    public static class VoidEchoCrawlers extends EnvironmentalAttack {

        private final List<double[]> crawlerDirections = new ArrayList<>();
        private final List<Double> crawlerProgress = new ArrayList<>();
        private boolean crawlersActive = false;

        public VoidEchoCrawlers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_echo_crawlers", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(240);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // 6 crawlers from random edge positions
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * 2 * Math.PI;
                crawlerDirections.add(new double[]{Math.cos(a), Math.sin(a)});
                crawlerProgress.add(10.0);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- void shimmer at edges
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < crawlerDirections.size(); i++) {
                        double[] dir = crawlerDirections.get(i);
                        Location edgePos = center.clone().add(dir[0] * 10, 0.5, dir[1] * 10);
                        DisplayBuilder.dustParticles(edgePos, 8, 0.5, 20, 0, 40, 1.5f);
                        w.spawnParticle(Particle.PORTAL, edgePos, 5, 0.3, 0.5, 0.3, 0.01);
                    }
                }
                return;
            }

            // Crawlers advance inward
            if (!crawlersActive) {
                crawlersActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.8f, 0.4f);
            }

            int crawlTick = ticksAlive - 60;

            for (int i = 0; i < crawlerDirections.size(); i++) {
                double progress = crawlerProgress.get(i);
                progress -= 0.08;
                if (progress < 0) progress = 0;
                crawlerProgress.set(i, progress);

                double[] dir = crawlerDirections.get(i);
                Location crawlerPos = center.clone().add(dir[0] * progress, 0.5, dir[1] * progress);

                if (crawlTick % 2 == 0) {
                    // Dark tendril particles
                    DisplayBuilder.dustParticles(crawlerPos, 12, 0.5, 10, 0, 30, 2.0f);
                    w.spawnParticle(Particle.PORTAL, crawlerPos, 8, 0.3, 0.3, 0.3, 0.01);

                    // Trail behind crawler
                    for (int t = 0; t < 3; t++) {
                        double trailDist = progress + t * 0.5;
                        if (trailDist <= 10) {
                            Location trail = center.clone().add(dir[0] * trailDist, 0.3, dir[1] * trailDist);
                            DisplayBuilder.dustParticles(trail, 4, 0.2, 5, 0, 15, 1.0f);
                        }
                    }
                }

                // Damage on contact
                if (crawlTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(crawlerPos) <= 2.25) {
                            p.damage(14.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEchoCrawlers(plugin); }
    }

    // =========================================================================
    // 124. CRIMSON FLOOR TIDE -- rising crimson particle flood
    // =========================================================================
    public static class CrimsonFloorTide extends EnvironmentalAttack {

        private double tideHeight = 0;
        private boolean tideRising = true;

        public CrimsonFloorTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_floor_tide", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1600);
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

            // Buildup: 60 ticks -- crimson seeps from floor
            if (ticksAlive <= 60) {
                double intensity = ticksAlive / 60.0;
                if (ticksAlive % 3 == 0) {
                    for (int x = -8; x <= 8; x += 4) {
                        for (int z = -8; z <= 8; z += 4) {
                            Location tilePos = center.clone().add(x, 0.2, z);
                            DisplayBuilder.dustParticles(tilePos, (int) (3 * intensity), 1.0,
                                    180, 0, 0, 1.5f);
                        }
                    }
                }
                if (ticksAlive == 50) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_AMBIENT, 0.9f, 0.3f);
                }
                return;
            }

            int tideTick = ticksAlive - 60;

            // Rising phase: 150 ticks
            if (tideRising && tideTick <= 150) {
                tideHeight = Math.min(tideTick * 0.08, 12);

                if (tideTick % 2 == 0) {
                    for (int x = -8; x <= 8; x += 3) {
                        for (int z = -8; z <= 8; z += 3) {
                            Location tilePos = center.clone().add(x, tideHeight * 0.5, z);
                            DisplayBuilder.dustParticles(tilePos, 8, 1.5, 180, 0, 0, 2.0f);
                        }
                    }
                }

                // Stationary player damage (standing still for 2+ seconds)
                if (tideTick % 40 == 0 && tideTick > 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distanceSquared(center);
                        if (dist <= 100.0 && p.getLocation().getY() <= center.getY() + tideHeight) {
                            // Check if player velocity is near zero (approximate stationary check)
                            if (p.getVelocity().lengthSquared() < 0.01) {
                                p.damage(16.0);
                            }
                        }
                    }
                }

                if (tideTick == 150) {
                    tideRising = false;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1.2f, 0.25f);
                }
            }

            // Receding phase
            if (!tideRising && tideTick > 150) {
                tideHeight = Math.max(12 - (tideTick - 150) * 0.15, 0);
                if (tideTick % 3 == 0 && tideHeight > 0) {
                    for (int x = -8; x <= 8; x += 4) {
                        for (int z = -8; z <= 8; z += 4) {
                            DisplayBuilder.dustParticles(center.clone().add(x, tideHeight * 0.3, z),
                                    4, 1.0, 180, 0, 0, 1.2f);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonFloorTide(plugin); }
    }

    // =========================================================================
    // 125. LATTICE GRID ERUPTION -- DoG crystal lattice pattern on floor
    // =========================================================================
    public static class LatticeGridEruption extends EnvironmentalAttack {

        private final List<Location> spikePositions = new ArrayList<>();
        private boolean spikesActive = false;

        public LatticeGridEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lattice_grid_eruption", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // 16-spike grid in northeast quadrant
            for (int x = 0; x <= 7; x += 2) {
                for (int z = 0; z <= 7; z += 2) {
                    spikePositions.add(center.clone().add(x + 1, 0, z + 1));
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- crystalline grid particles
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    for (Location spike : spikePositions) {
                        DisplayBuilder.dustParticles(spike.clone().add(0, 0.3, 0),
                                6, 0.3, 0, 200, 255, 1.5f);
                        w.spawnParticle(Particle.ENCHANTED_HIT, spike.clone().add(0, 0.5, 0),
                                3, 0.2, 0.3, 0.2, 0.02);
                    }
                }
                if (ticksAlive == 55) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.6f);
                }
                return;
            }

            // Spike eruption
            if (!spikesActive) {
                spikesActive = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 0.5f);
            }

            int spikeTick = ticksAlive - 60;

            if (spikeTick <= 60) {
                double height = Math.min(spikeTick * 0.3, 8);

                if (spikeTick % 2 == 0) {
                    for (Location spike : spikePositions) {
                        for (int y = 0; y <= (int) height; y += 2) {
                            DisplayBuilder.dustParticles(spike.clone().add(0, y, 0),
                                    8, 0.3, 0, 200, 255, 2.0f);
                            w.spawnParticle(Particle.ENCHANTED_HIT, spike.clone().add(0, y, 0),
                                    4, 0.2, 0.3, 0.2, 0.03);
                        }
                    }
                }

                // Damage
                if (spikeTick % 10 == 0) {
                    for (Location spike : spikePositions) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - spike.getX();
                            double dz = p.getLocation().getZ() - spike.getZ();
                            if (dx * dx + dz * dz <= 2.25 && p.getLocation().getY() <= center.getY() + height) {
                                p.damage(16.0);
                            }
                        }
                    }
                }
            }

            // Dissolve phase
            if (spikeTick > 60 && spikeTick % 5 == 0) {
                for (Location spike : spikePositions) {
                    DisplayBuilder.dustParticles(spike.clone().add(0, 3, 0),
                            4, 0.5, 0, 150, 200, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LatticeGridEruption(plugin); }
    }

    // =========================================================================
    // 126. DWELLER EMBER ECHOES -- twin fire trails crawl across arena
    // =========================================================================
    public static class DwellerEmberEchoes extends EnvironmentalAttack {

        private Location trailAStart;
        private Location trailBStart;
        private double trailProgress = 0;

        public DwellerEmberEchoes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_ember_echoes", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(1500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // Twin embers from north and south walls
            trailAStart = center.clone().add(0, 0.5, -10);
            trailBStart = center.clone().add(0, 0.5, 10);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- walls glow orange
            if (ticksAlive <= 40) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(trailAStart, 10, 0.5, 200, 50, 0, 1.5f);
                    DisplayBuilder.dustParticles(trailBStart, 10, 0.5, 200, 50, 0, 1.5f);
                    w.spawnParticle(Particle.FLAME, trailAStart, 5, 0.3, 0.3, 0.3, 0.01);
                    w.spawnParticle(Particle.FLAME, trailBStart, 5, 0.3, 0.3, 0.3, 0.01);
                }
                if (ticksAlive == 35) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.4f);
                }
                return;
            }

            // Trail crawl: 1.0 block/tick toward each other
            trailProgress += 0.15;

            int trailTick = ticksAlive - 40;

            Location trailAPos = trailAStart.clone().add(0, 0, trailProgress);
            Location trailBPos = trailBStart.clone().add(0, 0, -trailProgress);

            if (trailProgress <= 20) {
                if (trailTick % 2 == 0) {
                    // Trail A particles (crawling south)
                    DisplayBuilder.dustParticles(trailAPos, 15, 0.5, 200, 50, 0, 2.0f);
                    w.spawnParticle(Particle.FLAME, trailAPos, 8, 0.3, 0.3, 0.3, 0.02);
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, trailAPos, 4, 0.3, 0.3, 0.3, 0.005);

                    // Trail B particles (crawling north)
                    DisplayBuilder.dustParticles(trailBPos, 15, 0.5, 200, 50, 0, 2.0f);
                    w.spawnParticle(Particle.FLAME, trailBPos, 8, 0.3, 0.3, 0.3, 0.02);
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, trailBPos, 4, 0.3, 0.3, 0.3, 0.005);
                }

                // Trail damage
                if (trailTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(trailAPos) <= 2.25 ||
                                p.getLocation().distanceSquared(trailBPos) <= 2.25) {
                            p.damage(14.0);
                        }
                    }
                }
            }

            // Intersection detonation when trails meet
            if (trailProgress >= 10 && trailProgress <= 10.5) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 60, 2.0, 1.0, 2.0, 0.05);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 40, 3.0, 255, 100, 0, 2.5f);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 9.0) {
                        p.damage(20.0);
                    }
                }
            }

            // Persistent trail marks
            if (trailTick > 0 && trailTick % 8 == 0 && trailProgress <= 20) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, trailAPos.clone().add(0, -0.3, 0),
                        3, 0.3, 0.1, 0.3, 0.005);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, trailBPos.clone().add(0, -0.3, 0),
                        3, 0.3, 0.1, 0.3, 0.005);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellerEmberEchoes(plugin); }
    }

    // =========================================================================
    // 127. INHERITANCE TRAIL MERGE -- two echo trails intersect
    // =========================================================================
    public static class InheritanceTrailMerge extends EnvironmentalAttack {

        private double trailProgressA = 0;
        private double trailProgressB = 0;

        public InheritanceTrailMerge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inheritance_trail_merge", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1600);
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

            // Buildup: 40 ticks
            if (ticksAlive <= 40) {
                if (ticksAlive % 4 == 0) {
                    // Void trail (east edge)
                    DisplayBuilder.dustParticles(center.clone().add(10, 0.5, 0),
                            8, 0.5, 20, 0, 40, 1.5f);
                    // Crystal trail (west edge)
                    DisplayBuilder.dustParticles(center.clone().add(-10, 0.5, 0),
                            8, 0.5, 0, 200, 255, 1.5f);
                }
                return;
            }

            trailProgressA += 0.12;
            trailProgressB += 0.12;

            int trailTick = ticksAlive - 40;

            // Trail A: void (from east, heading west)
            Location trailAPos = center.clone().add(10 - trailProgressA, 0.5, 0);
            // Trail B: crystal (from west, heading east)
            Location trailBPos = center.clone().add(-10 + trailProgressB, 0.5, 0);

            if (trailProgressA <= 20) {
                if (trailTick % 2 == 0) {
                    DisplayBuilder.dustParticles(trailAPos, 10, 0.4, 20, 0, 40, 1.5f);
                    w.spawnParticle(Particle.PORTAL, trailAPos, 5, 0.2, 0.3, 0.2, 0.01);

                    DisplayBuilder.dustParticles(trailBPos, 10, 0.4, 0, 200, 255, 1.5f);
                    w.spawnParticle(Particle.ENCHANTED_HIT, trailBPos, 5, 0.2, 0.3, 0.2, 0.02);
                }

                // Damage
                if (trailTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(trailAPos) <= 4.0 ||
                                p.getLocation().distanceSquared(trailBPos) <= 4.0) {
                            p.damage(16.0);
                        }
                    }
                }
            }

            // Intersection explosion
            if (trailProgressA >= 10 && trailProgressA <= 10.5) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);

                // Mixed void + crystal explosion
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 30, 3.0, 20, 0, 40, 2.5f);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 30, 3.0, 0, 200, 255, 2.5f);
                w.spawnParticle(Particle.PORTAL, center.clone().add(0, 1, 0), 40, 2.0, 1.0, 2.0, 0.1);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 16.0) {
                        p.damage(20.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InheritanceTrailMerge(plugin); }
    }

    // =========================================================================
    // 128. BOSS ECHO INDICATORS -- faint visual echoes of prior bosses
    // =========================================================================
    public static class BossEchoIndicators extends EnvironmentalAttack {

        private int echoType = 0; // 0=Voidmaw, 1=DoG, 2=Dweller, 3=Emperor

        public BossEchoIndicators(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("boss_echo_indicators", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(2000);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            echoType = (int) (Math.random() * 4);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive % 4 != 0) return;

            // Psychological zero-damage echoes
            switch (echoType) {
                case 0: // Voidmaw -- void circle
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0),
                            12, 4.0, 20, 0, 40, 1.5f);
                    if (ticksAlive == 4) {
                        DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_AMBIENT, 0.4f, 0.3f);
                    }
                    break;
                case 1: // DoG -- crystal lattice
                    for (int i = 0; i < 4; i++) {
                        double a = (2 * Math.PI * i) / 4 + ticksAlive * 0.02;
                        Location lPos = center.clone().add(Math.cos(a) * 5, 1.5, Math.sin(a) * 5);
                        DisplayBuilder.dustParticles(lPos, 6, 0.3, 0, 200, 255, 1.2f);
                    }
                    if (ticksAlive == 4) {
                        DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.3f, 0.2f);
                    }
                    break;
                case 2: // Dweller -- twin embers
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, -5),
                            8, 0.5, 200, 50, 0, 1.5f);
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 5),
                            8, 0.5, 200, 50, 0, 1.5f);
                    if (ticksAlive == 4) {
                        DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_DEATH, 0.3f, 0.3f);
                    }
                    break;
                case 3: // Void Emperor -- blade ring
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), 5.0,
                            Particle.DUST, 12,
                            new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.5f));
                    if (ticksAlive == 4) {
                        DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.7f);
                    }
                    break;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BossEchoIndicators(plugin); }
    }

    // =========================================================================
    // 129. VOID FEAR PULSE -- psychological near-invisible silhouette
    // =========================================================================
    public static class VoidFearPulse extends EnvironmentalAttack {

        private Location silhouettePos;
        private boolean burst = false;

        public VoidFearPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_fear_pulse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 3 + Math.random() * 6;
            silhouettePos = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (silhouettePos == null) return;
            World w = silhouettePos.getWorld();
            if (w == null) return;

            // NO SOUND -- silence is the mechanic

            // Ghostly silhouette forms slowly: 100 ticks
            if (ticksAlive <= 100) {
                if (ticksAlive % 5 == 0) {
                    // Very sparse white dust -- near invisible
                    DisplayBuilder.dustParticles(silhouettePos.clone().add(0, 1, 0),
                            2, 0.3, 200, 200, 200, 1.0f);
                    if (ticksAlive > 50) {
                        DisplayBuilder.dustParticles(silhouettePos.clone().add(0, 0.5, 0),
                                1, 0.2, 200, 200, 200, 0.8f);
                    }
                }
                return;
            }

            // Burst at 100 ticks
            if (!burst) {
                burst = true;
                w.spawnParticle(Particle.WITCH, silhouettePos.clone().add(0, 1, 0),
                        80, 1.5, 1.5, 1.5, 0);

                // Damage players in range
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(silhouettePos) <= 9.0) {
                        p.damage(10.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidFearPulse(plugin); }
    }

    // =========================================================================
    // 130. DOG LATTICE ERUPTION -- crystal spike eruption from DoG echo
    // =========================================================================
    public static class DoGLatticeEruption extends EnvironmentalAttack {

        private final List<Location> latticePoints = new ArrayList<>();
        private boolean erupted = false;

        public DoGLatticeEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_lattice_eruption", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // Northeast quadrant lattice grid
            for (int x = 1; x <= 8; x += 2) {
                for (int z = 1; z <= 8; z += 2) {
                    latticePoints.add(center.clone().add(x, 0, z));
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- lattice grid glows cyan
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    for (Location pt : latticePoints) {
                        DisplayBuilder.dustParticles(pt.clone().add(0, 0.3, 0),
                                6, 0.3, 0, 200, 255, 1.5f);
                    }
                    // Connecting lines between adjacent lattice points
                    for (int i = 0; i < latticePoints.size() - 1; i++) {
                        Location a = latticePoints.get(i);
                        Location b = latticePoints.get(i + 1);
                        if (a.distanceSquared(b) <= 5.0) {
                            Location mid = a.clone().add(
                                    (b.getX() - a.getX()) * 0.5, 0.5,
                                    (b.getZ() - a.getZ()) * 0.5);
                            DisplayBuilder.dustParticles(mid, 3, 0.5, 0, 200, 255, 0.8f);
                        }
                    }
                }
                if (ticksAlive == 55) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.4f);
                }
                return;
            }

            // Full eruption
            if (!erupted) {
                erupted = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.3f);
            }

            int eruptTick = ticksAlive - 60;

            // Crystal spikes erupt upward
            if (eruptTick <= 40) {
                double height = Math.min(eruptTick * 0.4, 10);
                if (eruptTick % 2 == 0) {
                    for (Location pt : latticePoints) {
                        for (int y = 0; y <= (int) height; y += 2) {
                            DisplayBuilder.dustParticles(pt.clone().add(0, y, 0),
                                    10, 0.3, 0, 200, 255, 2.5f);
                        }
                        w.spawnParticle(Particle.ENCHANTED_HIT, pt.clone().add(0, height, 0),
                                6, 0.3, 0.5, 0.3, 0.05);
                    }
                }

                // Detonation ring at each spike
                if (eruptTick % 10 == 0) {
                    for (Location pt : latticePoints) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - pt.getX();
                            double dz = p.getLocation().getZ() - pt.getZ();
                            if (dx * dx + dz * dz <= 4.0 && p.getLocation().getY() <= center.getY() + height) {
                                p.damage(18.0);
                            }
                        }
                    }
                }
            }

            // Fragmentation dissolve
            if (eruptTick > 40 && eruptTick % 5 == 0) {
                for (Location pt : latticePoints) {
                    DisplayBuilder.dustParticles(pt.clone().add(0, 5, 0),
                            4, 1.0, 0, 150, 200, 0.8f);
                    w.spawnParticle(Particle.END_ROD, pt.clone().add(0, 6, 0),
                            2, 0.5, 0.5, 0.5, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoGLatticeEruption(plugin); }
    }
}
