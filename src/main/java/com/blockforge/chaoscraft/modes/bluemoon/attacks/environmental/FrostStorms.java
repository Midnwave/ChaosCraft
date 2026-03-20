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
 * Blue Moon Environmental -- GROUP: FROST STORMS
 * 13 ice/frost-themed environmental attacks.
 * Blue Moon palette: pale blue (150,230,255), silver (200,200,220), moonlight white (240,240,255).
 * Particles: SNOWFLAKE, DUST(150,230,255), BLOCK_CRACK(ICE), CLOUD
 * Sounds: BLOCK_GLASS_BREAK, ENTITY_SNOW_GOLEM_AMBIENT, WEATHER_RAIN
 * NO status effects. Velocity for knockback/push only.
 */
public final class FrostStorms {

    private FrostStorms() {}

    private static final Particle.DustOptions FROST_DUST =
            new Particle.DustOptions(Color.fromRGB(150, 230, 255), 1.2f);
    private static final Particle.DustOptions FROST_DUST_SMALL =
            new Particle.DustOptions(Color.fromRGB(150, 230, 255), 0.8f);
    private static final Particle.DustOptions SILVER_DUST =
            new Particle.DustOptions(Color.fromRGB(200, 200, 220), 1.0f);

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BlizzardZone(plugin));
        registry.register(new FlashFreeze(plugin));
        registry.register(new IceSpikeEruption(plugin));
        registry.register(new FrostBreath(plugin));
        registry.register(new PermafrostPatch(plugin));
        registry.register(new ShatterBurst(plugin));
        registry.register(new HypothermiaWave(plugin));
        registry.register(new RimeCoating(plugin));
        registry.register(new FrozenGeyser(plugin));
        registry.register(new Hailstorm(plugin));
        registry.register(new ColdSnap(plugin));
        registry.register(new GlacialDrift(plugin));
        registry.register(new CryogenicBurst(plugin));
    }

    // ================================================================
    // 1. BLIZZARD ZONE -- 10-block radius snow particles + frost dust
    //    Damage 3/tick inside. Duration: 160, radius: 10.
    // ================================================================
    public static class BlizzardZone extends EnvironmentalAttack {

        public BlizzardZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blizzard_zone", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 1.0f, 0.5f);
            DisplayBuilder.dustParticles(center, 60, 10.0, 150, 230, 255, 1.2f);
            w.spawnParticle(Particle.SNOWFLAKE, center, 40, 10, 3, 10, 0.02);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Continuous blizzard particles
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 2, 0), 30, 10, 3, 10, 0.03);
                DisplayBuilder.dustParticles(center, 20, 10.0, 150, 230, 255, 1.0f);
                w.spawnParticle(Particle.CLOUD, center.clone().add(0, 1, 0), 8, 8, 2, 8, 0.01);
            }

            // Periodic wind sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.8f, 0.4f);
            }

            // Push players slightly with velocity
            if (ticksAlive % 20 == 0) {
                double angle = Math.random() * Math.PI * 2;
                Vector push = new Vector(Math.cos(angle) * 0.15, 0, Math.sin(angle) * 0.15);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 100) {
                        p.setVelocity(p.getVelocity().add(push));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BlizzardZone(plugin); }
    }

    // ================================================================
    // 2. FLASH FREEZE -- Instant expanding frost ring (particles).
    //    Players hit slowed (velocity reduction). Damage: 8, radius: 8, duration: 20.
    // ================================================================
    public static class FlashFreeze extends EnvironmentalAttack {

        private boolean ringFired = false;

        public FlashFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("flash_freeze", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Expanding frost ring over first 10 ticks
            if (ticksAlive <= 10) {
                double radius = ticksAlive * 0.8;
                int points = (int) (radius * 8);
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    Location p = center.clone().add(Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, FROST_DUST);
                }
            }

            // At tick 5: damage + velocity kill (simulate freeze)
            if (ticksAlive == 5 && !ringFired) {
                ringFired = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.8f);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 64) {
                        // Reduce velocity to simulate freeze
                        Vector vel = p.getVelocity();
                        p.setVelocity(new Vector(vel.getX() * 0.1, vel.getY(), vel.getZ() * 0.1));
                    }
                }
                // Ice particle explosion
                w.spawnParticle(Particle.BLOCK, center, 80, 8, 1, 8, 0,
                        Material.ICE.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FlashFreeze(plugin); }
    }

    // ================================================================
    // 3. ICE SPIKE ERUPTION -- Line of frost particle bursts along ground.
    //    Damage: 7 along line, radius: 4, duration: 40.
    // ================================================================
    public static class IceSpikeEruption extends EnvironmentalAttack {

        private double dirX, dirZ;

        public IceSpikeEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_spike_eruption", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Eruption progresses along line -- one spike every 2 ticks
            if (ticksAlive % 2 == 0) {
                int spikeIndex = ticksAlive / 2;
                double dist = spikeIndex * 1.5;
                Location spikeLoc = center.clone().add(dirX * dist, 0, dirZ * dist);

                // Spike particles burst upward
                w.spawnParticle(Particle.SNOWFLAKE, spikeLoc, 15, 0.3, 1.5, 0.3, 0.05);
                w.spawnParticle(Particle.DUST, spikeLoc, 10, 0.3, 1.0, 0.3, 0, FROST_DUST);
                w.spawnParticle(Particle.BLOCK, spikeLoc, 8, 0.3, 0.5, 0.3, 0,
                        Material.ICE.createBlockData());
                DisplayBuilder.playSound(spikeLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.2f + (float)(Math.random() * 0.3));

                // Damage players near each spike
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(spikeLoc) <= 9) {
                        p.damage(config.getDamage());
                        p.setVelocity(p.getVelocity().add(new Vector(0, 0.4, 0)));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceSpikeEruption(plugin); }
    }

    // ================================================================
    // 4. FROST BREATH -- Cone of frost particles from random direction.
    //    Sweeps slowly. Damage: 6, radius: 8, duration: 80.
    // ================================================================
    public static class FrostBreath extends EnvironmentalAttack {

        private double baseAngle;
        private Location source;

        public FrostBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_breath", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            baseAngle = Math.random() * Math.PI * 2;
            // Source 10 blocks away from center
            source = center.clone().add(Math.cos(baseAngle) * 10, 1, Math.sin(baseAngle) * 10);
            DisplayBuilder.playSound(source, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sweep angle over duration: sweeps 120 degrees (PI/3 each side)
            double sweepProgress = (double) ticksAlive / config.getDurationTicks();
            double currentAngle = baseAngle + Math.PI + (sweepProgress - 0.5) * (Math.PI * 2.0 / 3.0);

            if (ticksAlive % 2 == 0) {
                // Cone of frost particles
                double coneHalfAngle = Math.PI / 8; // 22.5 degree half-width
                for (int i = 0; i < 12; i++) {
                    double particleAngle = currentAngle + (Math.random() - 0.5) * coneHalfAngle * 2;
                    double dist = 1.0 + Math.random() * 8.0;
                    Location pLoc = source.clone().add(
                            Math.cos(particleAngle) * dist,
                            Math.random() * 2.0,
                            Math.sin(particleAngle) * dist
                    );
                    w.spawnParticle(Particle.SNOWFLAKE, pLoc, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, FROST_DUST_SMALL);
                }

                // Damage players in the cone
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double dx = pLoc.getX() - source.getX();
                    double dz = pLoc.getZ() - source.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 8 || dist < 1) continue;
                    double pAngle = Math.atan2(dz, dx);
                    double angleDiff = Math.abs(normalizeAngle(pAngle - currentAngle));
                    if (angleDiff <= coneHalfAngle) {
                        p.damage(config.getDamage() * 0.5);
                        // Push away from source
                        Vector push = pLoc.toVector().subtract(source.toVector()).normalize().multiply(0.3);
                        push.setY(0.1);
                        p.setVelocity(p.getVelocity().add(push));
                    }
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(source, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.7f, 0.5f);
            }
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostBreath(plugin); }
    }

    // ================================================================
    // 5. PERMAFROST PATCH -- Ground area frozen (frost particles).
    //    Standing on it = damage. Damage: 3/tick, radius: 5, duration: 200.
    // ================================================================
    public static class PermafrostPatch extends EnvironmentalAttack {

        public PermafrostPatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_patch", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Frost ground ring
            DisplayBuilder.particleRing(center, 5.0, Particle.SNOWFLAKE, 30, null);
            DisplayBuilder.dustParticles(center, 40, 5.0, 150, 230, 255, 1.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ground frost particle layer
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = Math.random() * 5.0;
                    Location pLoc = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.SNOWFLAKE, pLoc, 1, 0, 0.1, 0, 0.01);
                    w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, FROST_DUST_SMALL);
                }
            }

            // Ambient crackle sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.5f, 0.8f);
            }

            // Fade effect near the end
            if (ticksAlive > 170) {
                if (ticksAlive % 4 == 0) {
                    w.spawnParticle(Particle.CLOUD, center, 5, 5, 0.5, 5, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PermafrostPatch(plugin); }
    }

    // ================================================================
    // 6. SHATTER BURST -- Glass break sound + ice particle explosion.
    //    Instant damage. Damage: 10, radius: 6, duration: 10.
    // ================================================================
    public static class ShatterBurst extends EnvironmentalAttack {

        private boolean shattered = false;

        public ShatterBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shatter_burst", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(10);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Brief warning shimmer
            w.spawnParticle(Particle.DUST, center, 20, 3, 1, 3, 0, SILVER_DUST);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Shatter at tick 3
            if (ticksAlive == 3 && !shattered) {
                shattered = true;

                // Glass break sounds layered
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.0f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);

                // Massive ice particle explosion
                w.spawnParticle(Particle.BLOCK, center.clone().add(0, 1, 0), 120, 6, 2, 6, 0.1,
                        Material.ICE.createBlockData());
                w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1, 0), 60, 6, 3, 6, 0.05);
                DisplayBuilder.dustParticles(center, 50, 6.0, 150, 230, 255, 1.5f);

                // Knockback outward
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 36) {
                        Vector knockback = p.getLocation().toVector().subtract(center.toVector());
                        if (knockback.lengthSquared() > 0) {
                            knockback.normalize().multiply(0.6).setY(0.3);
                        } else {
                            knockback = new Vector(0, 0.3, 0);
                        }
                        p.setVelocity(p.getVelocity().add(knockback));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ShatterBurst(plugin); }
    }

    // ================================================================
    // 7. HYPOTHERMIA WAVE -- Slow-moving wall of frost particles.
    //    Must dodge sideways. Damage: 8, radius: 12, duration: 80.
    // ================================================================
    public static class HypothermiaWave extends EnvironmentalAttack {

        private double dirX, dirZ;
        private double perpX, perpZ;

        public HypothermiaWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_wave", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            perpX = -dirZ;
            perpZ = dirX;
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wall moves forward at 0.3 blocks/tick from -12 blocks behind center
            double offset = -12.0 + ticksAlive * 0.3;
            Location wallCenter = center.clone().add(dirX * offset, 0, dirZ * offset);

            if (ticksAlive % 2 == 0) {
                // Wall is 12 blocks wide, 3 blocks tall
                for (int i = -6; i <= 6; i++) {
                    for (int y = 0; y < 3; y++) {
                        Location pLoc = wallCenter.clone().add(
                                perpX * i, y + 0.5, perpZ * i);
                        w.spawnParticle(Particle.SNOWFLAKE, pLoc, 1, 0.2, 0.2, 0.2, 0);
                        if (i % 2 == 0) {
                            w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, FROST_DUST);
                        }
                    }
                }
                w.spawnParticle(Particle.CLOUD, wallCenter.clone().add(0, 1, 0), 5, 6, 1, 0.5, 0.01);
            }

            // Damage + push players hit by wall
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    // Check if player is within wall thickness (2 blocks deep, 12 wide)
                    double forwardDist = (pLoc.getX() - wallCenter.getX()) * dirX
                            + (pLoc.getZ() - wallCenter.getZ()) * dirZ;
                    double sideDist = (pLoc.getX() - wallCenter.getX()) * perpX
                            + (pLoc.getZ() - wallCenter.getZ()) * perpZ;
                    if (Math.abs(forwardDist) <= 2.0 && Math.abs(sideDist) <= 6.0
                            && pLoc.getY() - wallCenter.getY() < 3.5 && pLoc.getY() - wallCenter.getY() > -1) {
                        p.damage(config.getDamage());
                        p.setVelocity(p.getVelocity().add(new Vector(dirX * 0.4, 0.1, dirZ * 0.4)));
                    }
                }
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(wallCenter, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HypothermiaWave(plugin); }
    }

    // ================================================================
    // 8. RIME COATING -- Players get frost particle trail.
    //    1 HP per 20 ticks for 80 ticks. Sprint 10+ blocks to cleanse.
    //    Damage: 2, radius: 10, duration: 80.
    // ================================================================
    public static class RimeCoating extends EnvironmentalAttack {

        private final Map<UUID, Location> lastPositions = new HashMap<>();
        private final Map<UUID, Double> distanceTraveled = new HashMap<>();
        private final Set<UUID> cleansed = new HashSet<>();

        public RimeCoating(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rime_coating", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(999); // Manual damage
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.8f, 1.0f);
            DisplayBuilder.dustParticles(center, 30, 10.0, 150, 230, 255, 1.0f);
            lastPositions.clear();
            distanceTraveled.clear();
            cleansed.clear();
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                if (p.getLocation().distanceSquared(center) > 100) continue;
                UUID uuid = p.getUniqueId();

                if (cleansed.contains(uuid)) continue;

                // Track distance traveled
                Location last = lastPositions.get(uuid);
                if (last != null && last.getWorld() == p.getWorld()) {
                    double dist = last.distance(p.getLocation());
                    distanceTraveled.merge(uuid, dist, Double::sum);
                }
                lastPositions.put(uuid, p.getLocation().clone());

                // Check cleanse: 10+ blocks sprinted
                double totalDist = distanceTraveled.getOrDefault(uuid, 0.0);
                if (totalDist >= 10.0) {
                    cleansed.add(uuid);
                    // Cleanse effect
                    w.spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
                    DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.6f, 1.8f);
                    continue;
                }

                // Frost trail particles on affected player
                if (ticksAlive % 4 == 0) {
                    w.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.01);
                    w.spawnParticle(Particle.DUST, p.getLocation(), 2, 0.3, 0.3, 0.3, 0, FROST_DUST_SMALL);
                }

                // Damage every 20 ticks
                if (ticksAlive % 20 == 0) {
                    p.damage(1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new RimeCoating(plugin); }
    }

    // ================================================================
    // 9. FROZEN GEYSER -- Column of frost particles erupting from ground.
    //    Upward velocity + damage. Damage: 8, radius: 3, duration: 40.
    // ================================================================
    public static class FrozenGeyser extends EnvironmentalAttack {

        private boolean erupted = false;

        public FrozenGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_geyser", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ground rumble warning
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.4f);
            w.spawnParticle(Particle.DUST, center, 10, 2, 0.1, 2, 0, SILVER_DUST);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning cracks for first 10 ticks
            if (ticksAlive <= 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.particleRing(center, 3.0, Particle.SNOWFLAKE, 12, null);
                    w.spawnParticle(Particle.DUST, center, 5, 2, 0.1, 2, 0, FROST_DUST_SMALL);
                }
                return;
            }

            // Eruption at tick 10
            if (ticksAlive == 11 && !erupted) {
                erupted = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 1.0f, 0.3f);

                // Launch players upward
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 9) {
                        p.setVelocity(p.getVelocity().add(new Vector(0, 1.2, 0)));
                    }
                }
            }

            // Column particles for remaining duration
            if (ticksAlive > 10 && ticksAlive % 2 == 0) {
                double height = Math.min((ticksAlive - 10) * 0.5, 8.0);
                for (double y = 0; y < height; y += 0.5) {
                    Location pLoc = center.clone().add(
                            (Math.random() - 0.5) * 1.5, y,
                            (Math.random() - 0.5) * 1.5);
                    w.spawnParticle(Particle.SNOWFLAKE, pLoc, 2, 0.3, 0, 0.3, 0.05);
                    w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, FROST_DUST);
                }
                w.spawnParticle(Particle.CLOUD, center.clone().add(0, height, 0), 3, 1, 0.5, 1, 0.02);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenGeyser(plugin); }
    }

    // ================================================================
    // 10. HAILSTORM -- Random small hits across 15-block area.
    //     Individual small hits. Damage: 2 per hit, radius: 15, duration: 120.
    // ================================================================
    public static class Hailstorm extends EnvironmentalAttack {

        public Hailstorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hailstorm", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(999); // Manual damage
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 2-3 hailstones per tick at random positions
            int hailCount = 2 + (ticksAlive % 3 == 0 ? 1 : 0);
            for (int h = 0; h < hailCount; h++) {
                double hx = center.getX() + (Math.random() - 0.5) * 30;
                double hz = center.getZ() + (Math.random() - 0.5) * 30;
                Location hitLoc = new Location(w, hx, center.getY(), hz);

                // Falling particle trail
                for (int y = 0; y < 5; y++) {
                    Location trail = hitLoc.clone().add(0, y, 0);
                    w.spawnParticle(Particle.SNOWFLAKE, trail, 1, 0, 0, 0, 0);
                }

                // Impact particles
                w.spawnParticle(Particle.BLOCK, hitLoc, 4, 0.3, 0.1, 0.3, 0,
                        Material.ICE.createBlockData());
                w.spawnParticle(Particle.DUST, hitLoc, 2, 0.2, 0.1, 0.2, 0, FROST_DUST_SMALL);

                // Individual hail hit damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(hitLoc) <= 4) {
                        p.damage(config.getDamage());
                    }
                }
            }

            // Ambient rain/hail sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 1.0f);
            }
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Hailstorm(plugin); }
    }

    // ================================================================
    // 11. COLD SNAP -- Instant AoE all players within 20 blocks take 3 HP.
    //     Ice crack sound. Damage: 6, radius: 20, duration: 5.
    // ================================================================
    public static class ColdSnap extends EnvironmentalAttack {

        private boolean snapped = false;

        public ColdSnap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_snap", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(5);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Instant snap -- no warning
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive == 1 && !snapped) {
                snapped = true;

                // Expanding frost ring
                for (double r = 2; r <= 20; r += 3) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r,
                            Particle.SNOWFLAKE, (int)(r * 4), null);
                }
                DisplayBuilder.dustParticles(center, 80, 20.0, 150, 230, 255, 1.5f);
                w.spawnParticle(Particle.BLOCK, center, 60, 15, 1, 15, 0,
                        Material.ICE.createBlockData());

                // Damage all players in range
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 400) {
                        p.damage(config.getDamage());
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ColdSnap(plugin); }
    }

    // ================================================================
    // 12. GLACIAL DRIFT -- Slow-moving frost particle cloud.
    //     Damage inside. Moves with wind. Damage: 4, radius: 5, duration: 160.
    // ================================================================
    public static class GlacialDrift extends EnvironmentalAttack {

        private double driftX, driftZ;
        private double cloudX, cloudZ;

        public GlacialDrift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_drift", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI * 2;
            driftX = Math.cos(angle) * 0.15;
            driftZ = Math.sin(angle) * 0.15;
            cloudX = center.getX();
            cloudZ = center.getZ();
            DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Drift the cloud
            cloudX += driftX;
            cloudZ += driftZ;

            // Slight wind direction change over time
            if (ticksAlive % 40 == 0) {
                driftX += (Math.random() - 0.5) * 0.05;
                driftZ += (Math.random() - 0.5) * 0.05;
                double speed = Math.sqrt(driftX * driftX + driftZ * driftZ);
                if (speed > 0.2) {
                    driftX = driftX / speed * 0.2;
                    driftZ = driftZ / speed * 0.2;
                }
            }

            Location cloudCenter = new Location(w, cloudX, center.getY() + 1, cloudZ);

            // Cloud particles
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.CLOUD, cloudCenter, 8, 4, 1.5, 4, 0.01);
                w.spawnParticle(Particle.SNOWFLAKE, cloudCenter, 10, 5, 2, 5, 0.02);
                DisplayBuilder.dustParticles(cloudCenter, 6, 4.0, 150, 230, 255, 0.8f);
            }

            // Damage players inside cloud
            if (ticksAlive % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(cloudCenter) <= 25) {
                        p.damage(config.getDamage());
                    }
                }
                DisplayBuilder.playSound(cloudCenter, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialDrift(plugin); }
    }

    // ================================================================
    // 13. CRYOGENIC BURST -- 6 rotating frost particle beams from center.
    //     Damage where beams touch. Damage: 7, radius: 8, duration: 100.
    // ================================================================
    public static class CryogenicBurst extends EnvironmentalAttack {

        private static final int BEAM_COUNT = 6;

        public CryogenicBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryogenic_burst", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(999); // Manual damage
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.8f, 0.4f);
            DisplayBuilder.dustParticles(center, 30, 3.0, 150, 230, 255, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rotation speed: one full rotation over duration
            double rotationOffset = (ticksAlive / (double) config.getDurationTicks()) * Math.PI * 2;

            if (ticksAlive % 2 == 0) {
                for (int beam = 0; beam < BEAM_COUNT; beam++) {
                    double beamAngle = rotationOffset + (Math.PI * 2 * beam) / BEAM_COUNT;

                    // Draw beam as particle line from center outward
                    for (double dist = 1; dist <= 8; dist += 0.5) {
                        Location bLoc = center.clone().add(
                                Math.cos(beamAngle) * dist,
                                0.5 + Math.sin(dist * 0.5) * 0.3,
                                Math.sin(beamAngle) * dist
                        );
                        w.spawnParticle(Particle.SNOWFLAKE, bLoc, 1, 0, 0, 0, 0);
                        if (dist % 1.0 < 0.5) {
                            w.spawnParticle(Particle.DUST, bLoc, 1, 0, 0, 0, 0, FROST_DUST);
                        }
                    }
                }

                // Center glow
                w.spawnParticle(Particle.DUST, center.clone().add(0, 0.5, 0), 5, 0.5, 0.5, 0.5, 0, FROST_DUST);
            }

            // Damage players touching beams (check every 10 ticks)
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double dx = pLoc.getX() - center.getX();
                    double dz = pLoc.getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 8 || dist < 1) continue;

                    double pAngle = Math.atan2(dz, dx);
                    for (int beam = 0; beam < BEAM_COUNT; beam++) {
                        double beamAngle = rotationOffset + (Math.PI * 2 * beam) / BEAM_COUNT;
                        double angleDiff = Math.abs(normalizeAngle(pAngle - beamAngle));
                        if (angleDiff < 0.25) { // ~14 degrees tolerance
                            p.damage(config.getDamage());
                            // Push outward
                            Vector push = new Vector(Math.cos(pAngle) * 0.4, 0.2, Math.sin(pAngle) * 0.4);
                            p.setVelocity(p.getVelocity().add(push));
                            break;
                        }
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.5f, 0.6f + (float)(ticksAlive * 0.005));
            }
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CryogenicBurst(plugin); }
    }
}
