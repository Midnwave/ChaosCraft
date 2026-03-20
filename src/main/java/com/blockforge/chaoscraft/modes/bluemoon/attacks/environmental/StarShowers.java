package com.blockforge.chaoscraft.modes.bluemoon.attacks.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Blue Moon Mode — STAR SHOWERS
 * 13 star/particle rain environmental attacks.
 * Palette: pale blue (180,210,255), silver (200,200,220), warm gold (255,200,100)
 */
public final class StarShowers {

    private StarShowers() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MeteorShower(plugin));
        registry.register(new StarfireRain(plugin));
        registry.register(new ShootingStarTrail(plugin));
        registry.register(new StarClusterDrop(plugin));
        registry.register(new StardustCloud(plugin));
        registry.register(new FallingConstellation(plugin));
        registry.register(new SolarWind(plugin));
        registry.register(new CosmicRay(plugin));
        registry.register(new NebulaFog(plugin));
        registry.register(new StarBomb(plugin));
        registry.register(new PulsarFlash(plugin));
        registry.register(new GalaxySpiral(plugin));
        registry.register(new AuroraStrike(plugin));
    }

    // ================================================================
    // 1. METEOR SHOWER — 20 small END_ROD particles falling over wide area
    // ================================================================
    public static class MeteorShower extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public MeteorShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meteor_shower", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 0.5f);
            // Warning shimmer
            for (int i = 0; i < 10; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 24;
                double oz = (RNG.nextDouble() - 0.5) * 24;
                DisplayBuilder.dustParticles(center.clone().add(ox, 20, oz), 3, 0.5, 180, 210, 255, 1.0f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Spawn falling END_ROD particles across the area
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 20; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 24;
                    double oz = (RNG.nextDouble() - 0.5) * 24;
                    double height = 15 + RNG.nextDouble() * 10;
                    Location dropLoc = center.clone().add(ox, height, oz);
                    w.spawnParticle(Particle.END_ROD, dropLoc, 1, 0, -0.5, 0, 0.1);
                }
            }

            // Falling streaks at lower height for visual density
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 24;
                    double oz = (RNG.nextDouble() - 0.5) * 24;
                    double y = 5 + RNG.nextDouble() * 10;
                    w.spawnParticle(Particle.END_ROD, center.clone().add(ox, y, oz), 2, 0.2, -0.3, 0.2, 0.02);
                }
            }

            // Ground impact sparkles
            if (ticksAlive % 4 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, 0.3, oz), 4, 0.5, 200, 200, 220, 1.0f);
                DisplayBuilder.playSound(center.clone().add(ox, 0, oz), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new MeteorShower(plugin);
        }
    }

    // ================================================================
    // 2. STARFIRE RAIN — FLAME + SOUL_FIRE_FLAME particles raining down
    // ================================================================
    public static class StarfireRain extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public StarfireRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("starfire_rain", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.5f);
            // Warning glow overhead
            for (int i = 0; i < 8; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 16;
                double oz = (RNG.nextDouble() - 0.5) * 16;
                DisplayBuilder.dustParticles(center.clone().add(ox, 18, oz), 3, 1.0, 255, 200, 100, 1.5f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Rain of flame particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 16;
                    double oz = (RNG.nextDouble() - 0.5) * 16;
                    double height = 12 + RNG.nextDouble() * 6;
                    Location loc = center.clone().add(ox, height, oz);
                    w.spawnParticle(Particle.FLAME, loc, 1, 0.1, -0.3, 0.1, 0.02);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, -1, 0), 1, 0.1, -0.2, 0.1, 0.01);
                }
            }

            // Ground sizzle
            if (ticksAlive % 5 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 14;
                double oz = (RNG.nextDouble() - 0.5) * 14;
                w.spawnParticle(Particle.FLAME, center.clone().add(ox, 0.2, oz), 3, 0.3, 0.1, 0.3, 0.01);
                DisplayBuilder.playSound(center.clone().add(ox, 0, oz), Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.2f);
            }

            // Ambient sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new StarfireRain(plugin);
        }
    }

    // ================================================================
    // 3. SHOOTING STAR TRAIL — Diagonal particle streak, damage after 10-tick delay
    // ================================================================
    public static class ShootingStarTrail extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double dirX, dirZ;
        private final List<Location> trailPoints = new ArrayList<>();
        private boolean trailComplete = false;

        public ShootingStarTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shooting_star_trail", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            if (ticksAlive <= 10 && !trailComplete) {
                // Draw the diagonal streak across the sky and ground
                double progress = ticksAlive / 10.0;
                double startX = -dirX * 15;
                double startZ = -dirZ * 15;
                double endX = dirX * 15 * progress;
                double endZ = dirZ * 15 * progress;

                // Head of the streak
                Location head = center.clone().add(endX, 8 - 7 * progress, endZ);
                w.spawnParticle(Particle.END_ROD, head, 8, 0.2, 0.2, 0.2, 0.05);
                DisplayBuilder.dustParticles(head, 5, 0.3, 255, 255, 200, 2.0f);

                // Store ground impact points along trail
                Location groundPoint = center.clone().add(endX, 0, endZ);
                trailPoints.add(groundPoint);

                if (ticksAlive == 10) trailComplete = true;
            }

            // After trail completes, damage along the trail with 10-tick delay
            if (trailComplete && ticksAlive >= 20 && ticksAlive == 20) {
                for (Location point : trailPoints) {
                    DisplayBuilder.dustParticles(point, 8, 1.0, 255, 200, 100, 1.5f);
                    w.spawnParticle(Particle.END_ROD, point.clone().add(0, 0.5, 0), 5, 0.5, 0.5, 0.5, 0.05);
                    // Damage players near each trail point
                    for (Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        if (player.getLocation().distanceSquared(point) <= 16) {
                            player.damage(config.getDamage());
                            player.setNoDamageTicks(0);
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.5f);
            }

            // Lingering sparkle
            if (ticksAlive > 20 && ticksAlive % 5 == 0) {
                for (Location point : trailPoints) {
                    w.spawnParticle(Particle.END_ROD, point.clone().add(0, 0.3, 0), 1, 0.3, 0.2, 0.3, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() {
            trailPoints.clear();
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ShootingStarTrail(plugin);
        }
    }

    // ================================================================
    // 4. STAR CLUSTER DROP — 8 star clusters fall in tight group, impact burst
    // ================================================================
    public static class StarClusterDrop extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<Location> clusterTargets = new ArrayList<>();
        private boolean impacted = false;

        public StarClusterDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_cluster_drop", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate 8 cluster landing positions in tight group
            for (int i = 0; i < 8; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 6;
                double oz = (RNG.nextDouble() - 0.5) * 6;
                clusterTargets.add(center.clone().add(ox, 0, oz));
            }
            // Warning glow
            DisplayBuilder.dustParticles(center.clone().add(0, 20, 0), 15, 3.0, 180, 210, 255, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            if (!impacted && ticksAlive < 20) {
                // Clusters descending
                double height = 20 - (20.0 * ticksAlive / 20.0);
                for (Location target : clusterTargets) {
                    Location falling = target.clone().add(0, height, 0);
                    w.spawnParticle(Particle.END_ROD, falling, 3, 0.2, 0.2, 0.2, 0.02);
                    DisplayBuilder.dustParticles(falling, 2, 0.3, 200, 200, 220, 1.2f);
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.5f + ticksAlive * 0.05f);
                }
            }

            if (!impacted && ticksAlive >= 20) {
                impacted = true;
                // Impact burst at each cluster location
                for (Location target : clusterTargets) {
                    triggerImpactDamage(target);
                    DisplayBuilder.dustParticles(target, 15, 2.0, 255, 255, 200, 2.0f);
                    w.spawnParticle(Particle.END_ROD, target.clone().add(0, 1, 0), 10, 1.0, 1.0, 1.0, 0.1);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            }

            // Lingering sparkles
            if (impacted && ticksAlive % 3 == 0) {
                for (Location target : clusterTargets) {
                    w.spawnParticle(Particle.END_ROD, target.clone().add(0, 0.5, 0), 1, 0.5, 0.3, 0.5, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() {
            clusterTargets.clear();
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new StarClusterDrop(plugin);
        }
    }

    // ================================================================
    // 5. STARDUST CLOUD — Lingering DUST cloud at ground, slowly expands
    // ================================================================
    public static class StardustCloud extends EnvironmentalAttack {
        private double currentRadius = 5.0;

        public StardustCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stardust_cloud", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentRadius = 5.0;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 20, 2.0, 180, 210, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Expand radius slowly over duration
            currentRadius = 5.0 + (ticksAlive / 160.0) * 5.0;
            config.setDamageRadius(currentRadius);

            // Render cloud particles
            if (ticksAlive % 3 == 0) {
                Random rng = new Random();
                for (int i = 0; i < 12; i++) {
                    double angle = rng.nextDouble() * 2 * Math.PI;
                    double dist = rng.nextDouble() * currentRadius;
                    double ox = Math.cos(angle) * dist;
                    double oz = Math.sin(angle) * dist;
                    double y = rng.nextDouble() * 2.5;
                    Location pLoc = center.clone().add(ox, y, oz);
                    // Stardust colors: silver and pale blue
                    if (rng.nextBoolean()) {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.4, 200, 200, 220, 1.2f);
                    } else {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.4, 180, 210, 255, 1.0f);
                    }
                }
            }

            // Occasional sparkle
            if (ticksAlive % 10 == 0) {
                double angle = new Random().nextDouble() * 2 * Math.PI;
                double dist = new Random().nextDouble() * currentRadius;
                w.spawnParticle(Particle.END_ROD, center.clone().add(
                        Math.cos(angle) * dist, 1.5, Math.sin(angle) * dist), 2, 0.2, 0.3, 0.2, 0.01);
            }

            // Ambient sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new StardustCloud(plugin);
        }
    }

    // ================================================================
    // 6. FALLING CONSTELLATION — Stars fall in pattern, damage where they land
    // ================================================================
    public static class FallingConstellation extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<Location> starPositions = new ArrayList<>();
        private final List<Boolean> starLanded = new ArrayList<>();

        public FallingConstellation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_constellation", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(60);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Generate constellation pattern (8 stars in recognizable shape)
            double[][] pattern = {
                {-3, 0, -3}, {-1, 0, -4}, {1, 0, -3}, {3, 0, -1},
                {2, 0, 2}, {0, 0, 3}, {-2, 0, 2}, {-4, 0, 0}
            };
            for (double[] offset : pattern) {
                starPositions.add(center.clone().add(offset[0], 0, offset[2]));
                starLanded.add(false);
            }

            // Warning: draw constellation lines in sky
            for (int i = 0; i < starPositions.size(); i++) {
                Location skyPos = starPositions.get(i).clone().add(0, 20, 0);
                DisplayBuilder.dustParticles(skyPos, 5, 0.3, 255, 255, 200, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Stars fall one by one every 5 ticks
            int starIndex = ticksAlive / 5;
            if (starIndex < starPositions.size()) {
                int currentFalling = Math.min(starIndex, starPositions.size() - 1);

                // Animate currently falling star
                for (int i = 0; i <= currentFalling; i++) {
                    if (starLanded.get(i)) continue;
                    Location target = starPositions.get(i);
                    int ticksSinceStart = ticksAlive - (i * 5);

                    if (ticksSinceStart >= 0 && ticksSinceStart < 10) {
                        double height = 20 - (20.0 * ticksSinceStart / 10.0);
                        Location falling = target.clone().add(0, height, 0);
                        w.spawnParticle(Particle.END_ROD, falling, 3, 0.1, 0.1, 0.1, 0.03);
                        DisplayBuilder.dustParticles(falling, 2, 0.2, 255, 255, 200, 1.5f);
                    }

                    if (ticksSinceStart >= 10 && !starLanded.get(i)) {
                        starLanded.set(i, true);
                        triggerImpactDamage(target);
                        DisplayBuilder.dustParticles(target, 10, 1.5, 180, 210, 255, 1.5f);
                        w.spawnParticle(Particle.END_ROD, target.clone().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.05);
                        DisplayBuilder.playSound(target, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f + i * 0.1f);
                    }
                }
            }

            // Constellation glow on landed stars
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < starPositions.size(); i++) {
                    if (starLanded.get(i)) {
                        w.spawnParticle(Particle.END_ROD, starPositions.get(i).clone().add(0, 0.5, 0),
                                1, 0.1, 0.2, 0.1, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            starPositions.clear();
            starLanded.clear();
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new FallingConstellation(plugin);
        }
    }

    // ================================================================
    // 7. SOLAR WIND — Horizontal particle stream, push + damage
    // ================================================================
    public static class SolarWind extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double windDirX, windDirZ;

        public SolarWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("solar_wind", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Random horizontal direction
            double angle = RNG.nextDouble() * 2 * Math.PI;
            windDirX = Math.cos(angle);
            windDirZ = Math.sin(angle);
            // Warning particles on incoming side
            for (int i = 0; i < 10; i++) {
                double spread = (RNG.nextDouble() - 0.5) * 12;
                Location edge = center.clone().add(-windDirX * 14, 2 + RNG.nextDouble() * 3, -windDirZ * 14 + spread);
                DisplayBuilder.dustParticles(edge, 4, 0.5, 255, 200, 100, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Stream of particles flowing in wind direction
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 15; i++) {
                    double offset = (RNG.nextDouble() - 0.5) * 20;
                    double perpX = -windDirZ * offset;
                    double perpZ = windDirX * offset;
                    double streamPos = (RNG.nextDouble() - 0.5) * 28;
                    double y = 0.5 + RNG.nextDouble() * 4;
                    Location pLoc = center.clone().add(
                            windDirX * streamPos + perpX, y, windDirZ * streamPos + perpZ);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.3, 255, 200, 100, 1.2f);
                    w.spawnParticle(Particle.END_ROD, pLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Push players in wind direction
            if (ticksAlive % 5 == 0) {
                Vector pushDir = new Vector(windDirX * 0.25, 0.02, windDirZ * 0.25);
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 144) {
                        player.setVelocity(player.getVelocity().add(pushDir));
                    }
                }
            }

            // Whooshing sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SolarWind(plugin);
        }
    }

    // ================================================================
    // 8. COSMIC RAY — Single intense narrow beam sweeping from sky
    // ================================================================
    public static class CosmicRay extends EnvironmentalAttack {
        private double sweepAngle = 0;
        private final double sweepSpeed = Math.PI / 60.0; // Full half-circle over duration

        public CosmicRay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_ray", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            sweepAngle = 0;
            // Warning flash at sky
            DisplayBuilder.dustParticles(center.clone().add(0, 25, 0), 20, 2.0, 255, 255, 255, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Sweep the beam across the area
            sweepAngle += sweepSpeed;
            double beamX = Math.cos(sweepAngle) * 8;
            double beamZ = Math.sin(sweepAngle) * 8;
            Location beamGround = center.clone().add(beamX, 0, beamZ);

            // Update damage center to beam ground position
            setCenter(beamGround);

            // Draw beam from sky to ground
            for (int y = 0; y <= 25; y++) {
                Location beamPoint = beamGround.clone().add(0, y, 0);
                DisplayBuilder.dustParticles(beamPoint, 2, 0.15, 255, 255, 255, 2.0f);
                if (y % 3 == 0) {
                    w.spawnParticle(Particle.END_ROD, beamPoint, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Ground impact circle
            DisplayBuilder.dustParticles(beamGround.clone().add(0, 0.3, 0), 8, 1.0, 180, 210, 255, 1.5f);

            // Intense sound at beam position
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.playSound(beamGround, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 2.0f);
            }

            // Manual damage since we move the center
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                if (player.getLocation().distanceSquared(beamGround) <= 4) {
                    player.damage(config.getDamage());
                    player.setNoDamageTicks(0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new CosmicRay(plugin);
        }
    }

    // ================================================================
    // 9. NEBULA FOG — Colorful particles obscure area, damage inside
    // ================================================================
    public static class NebulaFog extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public NebulaFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nebula_fog", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial fog bloom
            for (int i = 0; i < 30; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 12;
                double oz = (RNG.nextDouble() - 0.5) * 12;
                double y = RNG.nextDouble() * 4;
                int colorChoice = RNG.nextInt(3);
                if (colorChoice == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 3, 0.8, 180, 100, 255, 1.5f);
                } else if (colorChoice == 1) {
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 3, 0.8, 100, 180, 255, 1.5f);
                } else {
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 3, 0.8, 255, 150, 200, 1.5f);
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Dense colorful fog particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 8;
                    double ox = Math.cos(angle) * dist;
                    double oz = Math.sin(angle) * dist;
                    double y = RNG.nextDouble() * 5;
                    Location pLoc = center.clone().add(ox, y, oz);

                    int colorChoice = RNG.nextInt(4);
                    switch (colorChoice) {
                        case 0 -> DisplayBuilder.dustParticles(pLoc, 2, 0.6, 180, 100, 255, 1.8f);
                        case 1 -> DisplayBuilder.dustParticles(pLoc, 2, 0.6, 100, 180, 255, 1.8f);
                        case 2 -> DisplayBuilder.dustParticles(pLoc, 2, 0.6, 255, 150, 200, 1.8f);
                        case 3 -> DisplayBuilder.dustParticles(pLoc, 2, 0.6, 200, 100, 180, 1.8f);
                    }
                }
            }

            // Occasional star glint inside the fog
            if (ticksAlive % 8 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 14;
                double oz = (RNG.nextDouble() - 0.5) * 14;
                w.spawnParticle(Particle.END_ROD, center.clone().add(ox, 2, oz), 2, 0.1, 0.1, 0.1, 0.02);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new NebulaFog(plugin);
        }
    }

    // ================================================================
    // 10. STAR BOMB — Single large descending particle, explodes into 12 impacts
    // ================================================================
    public static class StarBomb extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private boolean exploded = false;

        public StarBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_bomb", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Large star visible overhead
            DisplayBuilder.dustParticles(center.clone().add(0, 25, 0), 25, 2.0, 255, 255, 200, 3.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            if (!exploded && ticksAlive < 20) {
                // Large star descending
                double height = 25 - (25.0 * ticksAlive / 20.0);
                Location starLoc = center.clone().add(0, height, 0);
                DisplayBuilder.dustParticles(starLoc, 10, 1.5, 255, 255, 200, 3.0f);
                w.spawnParticle(Particle.END_ROD, starLoc, 5, 0.5, 0.5, 0.5, 0.05);
                // Growing glow
                DisplayBuilder.dustParticles(starLoc, 5, 0.8, 255, 200, 100, 2.0f);

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.playSound(starLoc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.5f + ticksAlive * 0.05f);
                }
            }

            if (!exploded && ticksAlive >= 20) {
                exploded = true;
                // Primary impact
                triggerImpactDamage(center);
                DisplayBuilder.dustParticles(center, 30, 3.0, 255, 255, 200, 2.5f);
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 20, 2.0, 2.0, 2.0, 0.1);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                // 12 smaller impacts radiating outward
                for (int i = 0; i < 12; i++) {
                    double angle = (i / 12.0) * 2 * Math.PI;
                    double dist = 3 + RNG.nextDouble() * 3;
                    Location subImpact = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    triggerImpactDamage(subImpact);
                    DisplayBuilder.dustParticles(subImpact, 8, 1.0, 180, 210, 255, 1.5f);
                    w.spawnParticle(Particle.END_ROD, subImpact.clone().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.03);
                }
            }

            // Lingering sparkles
            if (exploded && ticksAlive % 5 == 0) {
                for (int i = 0; i < 5; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 10;
                    double oz = (RNG.nextDouble() - 0.5) * 10;
                    w.spawnParticle(Particle.END_ROD, center.clone().add(ox, 0.5, oz), 1, 0.2, 0.3, 0.2, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new StarBomb(plugin);
        }
    }

    // ================================================================
    // 11. PULSAR FLASH — Rapid flashing END_ROD, damage pulses every 5 ticks
    // ================================================================
    public static class PulsarFlash extends EnvironmentalAttack {

        public PulsarFlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pulsar_flash", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial bright flash
            w.spawnParticle(Particle.END_ROD, center.clone().add(0, 3, 0), 30, 3.0, 3.0, 3.0, 0.1);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Pulse every 5 ticks: bright flash of END_ROD
            if (ticksAlive % 5 == 0) {
                // Intense burst
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 20, 3.0, 2.0, 3.0, 0.08);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 3.0, 255, 255, 255, 2.0f);
                DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 2.0f);
            }

            // Between pulses: dim glow
            if (ticksAlive % 5 == 2) {
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 1.5, 0), 3, 1.0, 1.0, 1.0, 0.02);
            }

            // Expanding ring on each pulse
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), 6.0, Particle.END_ROD, 16, null);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new PulsarFlash(plugin);
        }
    }

    // ================================================================
    // 12. GALAXY SPIRAL — Particle spiral on ground rotates, damage in arms
    // ================================================================
    public static class GalaxySpiral extends EnvironmentalAttack {
        private double rotationAngle = 0;

        public GalaxySpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("galaxy_spiral", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            rotationAngle = 0;
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 15, 4.0, 180, 210, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            rotationAngle += 0.08;

            // Draw 3 spiral arms
            for (int arm = 0; arm < 3; arm++) {
                double armOffset = (arm / 3.0) * 2 * Math.PI;
                for (int point = 0; point < 16; point++) {
                    double t = point / 16.0;
                    double spiralAngle = rotationAngle + armOffset + t * 2.0;
                    double radius = 1.0 + t * 7.0;
                    double ox = Math.cos(spiralAngle) * radius;
                    double oz = Math.sin(spiralAngle) * radius;
                    Location pLoc = center.clone().add(ox, 0.3 + t * 0.5, oz);

                    if (point % 2 == 0) {
                        DisplayBuilder.dustParticles(pLoc, 1, 0.2, 180, 210, 255, 1.2f);
                    }
                    if (point % 4 == 0) {
                        w.spawnParticle(Particle.END_ROD, pLoc, 1, 0.1, 0.1, 0.1, 0.005);
                    }
                }
            }

            // Damage players in spiral arms (not center)
            if (ticksAlive % 10 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double distSq = ploc.distanceSquared(center);
                    double dist = Math.sqrt(distSq);

                    // Safe in center (< 2 blocks), damage in arms (2-8 blocks)
                    if (dist >= 2.0 && dist <= 8.0) {
                        // Check if player is on a spiral arm
                        double playerAngle = Math.atan2(ploc.getZ() - center.getZ(), ploc.getX() - center.getX());
                        for (int arm = 0; arm < 3; arm++) {
                            double armAngle = rotationAngle + (arm / 3.0) * 2 * Math.PI + (dist / 8.0) * 2.0;
                            double angleDiff = Math.abs(normalizeAngle(playerAngle - armAngle));
                            if (angleDiff < 0.5) {
                                player.damage(config.getDamage());
                                player.setNoDamageTicks(0);
                                break;
                            }
                        }
                    }
                }
            }

            // Center glow (safe zone indicator)
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 3, 0.5, 200, 200, 220, 1.0f);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 1.5f);
            }
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new GalaxySpiral(plugin);
        }
    }

    // ================================================================
    // 13. AURORA STRIKE — Curtain of colored dust descends, wide area
    // ================================================================
    public static class AuroraStrike extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double curtainY = 20;

        public AuroraStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("aurora_strike", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            curtainY = 20;
            // Aurora shimmer overhead
            for (int i = 0; i < 20; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                int color = RNG.nextInt(3);
                if (color == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(ox, 20, oz), 3, 0.5, 100, 255, 150, 1.5f);
                } else if (color == 1) {
                    DisplayBuilder.dustParticles(center.clone().add(ox, 20, oz), 3, 0.5, 150, 100, 255, 1.5f);
                } else {
                    DisplayBuilder.dustParticles(center.clone().add(ox, 20, oz), 3, 0.5, 100, 200, 255, 1.5f);
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Curtain descends
            curtainY = 20 - (20.0 * ticksAlive / 40.0);
            if (curtainY < 0) curtainY = 0;

            // Draw aurora curtain
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 15; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 20;
                    double oz = (RNG.nextDouble() - 0.5) * 20;
                    double y = curtainY + RNG.nextDouble() * 5;
                    Location pLoc = center.clone().add(ox, y, oz);

                    // Aurora colors: green, purple, cyan, shifting
                    double phase = (ticksAlive + ox + oz) * 0.1;
                    int r = (int) (100 + 100 * Math.sin(phase));
                    int g = (int) (150 + 100 * Math.sin(phase + 2.1));
                    int b = (int) (200 + 55 * Math.sin(phase + 4.2));
                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));
                    DisplayBuilder.dustParticles(pLoc, 2, 0.5, r, g, b, 1.5f);
                }
            }

            // Vertical streaks
            if (ticksAlive % 4 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 18;
                double oz = (RNG.nextDouble() - 0.5) * 18;
                for (int y = 0; y < 6; y++) {
                    w.spawnParticle(Particle.END_ROD, center.clone().add(ox, curtainY + y, oz),
                            1, 0.05, 0.1, 0.05, 0.01);
                }
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new AuroraStrike(plugin);
        }
    }
}
