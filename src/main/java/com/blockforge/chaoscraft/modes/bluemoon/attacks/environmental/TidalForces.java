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
 * Blue Moon Mode — TIDAL FORCES
 * 13 gravity/force manipulation environmental attacks.
 * Primarily use velocity manipulation, particles, and sounds.
 * Blue Moon palette: pale blue (180,210,255) / silver (200,200,220) / frost cyan (150,230,255)
 */
public final class TidalForces {

    private TidalForces() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GravityPulse(plugin));
        registry.register(new LunarPull(plugin));
        registry.register(new WeightSurge(plugin));
        registry.register(new LevitationBurst(plugin));
        registry.register(new TidalOscillation(plugin));
        registry.register(new ZeroGZone(plugin));
        registry.register(new CrushWave(plugin));
        registry.register(new MagneticNorth(plugin));
        registry.register(new OrbitLock(plugin));
        registry.register(new TidalBore(plugin));
        registry.register(new GravityStorm(plugin));
        registry.register(new MassIncrease(plugin));
        registry.register(new Slingshot(plugin));
    }

    // ================================================================
    // 1. GRAVITY PULSE — Push all players outward from center
    // ================================================================
    public static class GravityPulse extends EnvironmentalAttack {

        public GravityPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_pulse", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
            DisplayBuilder.dustParticles(center, 60, 2.0, 180, 210, 255, 1.5f);
            DisplayBuilder.particleRing(center, 3.0, Particle.END_ROD, 20, null);

            // Push all survival players outward
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                double dist = player.getLocation().distance(center);
                if (dist > 15.0 || dist < 0.5) continue;

                Vector direction = player.getLocation().toVector().subtract(center.toVector()).normalize();
                player.setVelocity(player.getVelocity().add(direction.multiply(0.8)));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Expanding ring visual
            double radius = 2.0 + (ticksAlive / 40.0) * 13.0;
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.END_ROD, 16, null);
                DisplayBuilder.dustParticles(c, 8, radius, 180, 210, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GravityPulse(plugin); }
    }

    // ================================================================
    // 2. LUNAR PULL — Pull players toward center over 3 seconds
    // ================================================================
    public static class LunarPull extends EnvironmentalAttack {

        public LunarPull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_pull", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.6f);
            DisplayBuilder.particleRing(center, 12.0, Particle.END_ROD, 30, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Pull players gently inward each tick
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                double dist = player.getLocation().distance(c);
                if (dist > 12.0 || dist < 0.5) continue;

                Vector pull = c.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.15);
                player.setVelocity(player.getVelocity().add(pull));
            }

            // Contracting ring visual
            if (ticksAlive % 3 == 0) {
                double radius = 12.0 - (ticksAlive / 60.0) * 10.0;
                DisplayBuilder.particleRing(c, Math.max(radius, 2.0), Particle.END_ROD, 20, null);
                DisplayBuilder.dustParticles(c, 10, 3.0, 200, 200, 220, 1.2f);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LunarPull(plugin); }
    }

    // ================================================================
    // 3. WEIGHT SURGE — Slow players via downward velocity for 80 ticks
    // ================================================================
    public static class WeightSurge extends EnvironmentalAttack {

        public WeightSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("weight_surge", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.3f);
            DisplayBuilder.dustParticles(center, 40, 10.0, 150, 230, 255, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Apply downward drag and horizontal slowdown every tick
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                if (player.getLocation().distance(c) > 10.0) continue;

                Vector vel = player.getVelocity();
                // Reduce horizontal velocity and push down slightly
                player.setVelocity(new Vector(vel.getX() * 0.7, vel.getY() - 0.04, vel.getZ() * 0.7));
            }

            // Heavy atmosphere particles
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c, 15, 10.0, 150, 230, 255, 1.5f);
                DisplayBuilder.particleRing(c, 10.0, Particle.CLOUD, 8, null);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.4f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WeightSurge(plugin); }
    }

    // ================================================================
    // 4. LEVITATION BURST — Launch players up 6 blocks
    // ================================================================
    public static class LevitationBurst extends EnvironmentalAttack {

        public LevitationBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("levitation_burst", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 1.5f);
            DisplayBuilder.dustParticles(center, 50, 4.0, 180, 210, 255, 2.0f);

            // Launch all survival players upward
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                if (player.getLocation().distance(center) > 8.0) continue;

                player.setVelocity(player.getVelocity().add(new Vector(0, 1.0, 0)));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Upward particle column
            if (ticksAlive % 2 == 0) {
                for (int y = 0; y < 8; y++) {
                    Location pLoc = c.clone().add(0, y, 0);
                    DisplayBuilder.dustParticles(pLoc, 4, 2.0, 180, 210, 255, 1.2f);
                }
                DisplayBuilder.particleRing(c, 8.0, Particle.END_ROD, 12, null);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LevitationBurst(plugin); }
    }

    // ================================================================
    // 5. TIDAL OSCILLATION — Push/pull alternating every 20 ticks
    // ================================================================
    public static class TidalOscillation extends EnvironmentalAttack {

        public TidalOscillation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tidal_oscillation", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.0f);
            DisplayBuilder.particleRing(center, 6.0, Particle.END_ROD, 24, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Determine push or pull phase (20-tick cycles)
            boolean pushing = (ticksAlive / 20) % 2 == 0;
            double force = pushing ? 0.3 : -0.3;

            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                double dist = player.getLocation().distance(c);
                if (dist > 12.0 || dist < 0.5) continue;

                Vector direction = player.getLocation().toVector().subtract(c.toVector()).normalize();
                player.setVelocity(player.getVelocity().add(direction.multiply(force)));
            }

            // Oscillation visuals
            if (ticksAlive % 4 == 0) {
                double radius = pushing ? 4.0 + (ticksAlive % 20) * 0.4 : 12.0 - (ticksAlive % 20) * 0.4;
                DisplayBuilder.particleRing(c, radius, Particle.END_ROD, 16, null);
                int r = pushing ? 180 : 200;
                int g = pushing ? 210 : 200;
                int b = pushing ? 255 : 220;
                DisplayBuilder.dustParticles(c, 10, radius, r, g, b, 1.0f);
            }

            // Sound at phase transitions
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.6f, pushing ? 1.2f : 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TidalOscillation(plugin); }
    }

    // ================================================================
    // 6. ZERO-G ZONE — Slow fall with upward drift for 100 ticks
    // ================================================================
    public static class ZeroGZone extends EnvironmentalAttack {

        public ZeroGZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("zero_g_zone", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(50);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.5f);
            DisplayBuilder.particleRing(center, 8.0, Particle.END_ROD, 24, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Apply gentle upward drift and negate gravity
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                if (player.getLocation().distance(c) > 8.0) continue;

                Vector vel = player.getVelocity();
                // Counteract gravity and add slight upward drift
                player.setVelocity(new Vector(vel.getX(), Math.max(vel.getY(), 0) + 0.05, vel.getZ()));
            }

            // Floating particles within zone
            if (ticksAlive % 3 == 0) {
                Random rand = new Random();
                for (int i = 0; i < 8; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 16.0;
                    double oy = rand.nextDouble() * 6.0;
                    double oz = (rand.nextDouble() - 0.5) * 16.0;
                    Location pLoc = c.clone().add(ox, oy, oz);
                    if (pLoc.distance(c) <= 8.0) {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.3, 200, 200, 220, 0.8f);
                    }
                }
                DisplayBuilder.particleRing(c.clone().add(0, ticksAlive % 6, 0), 8.0, Particle.CLOUD, 8, null);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ZeroGZone(plugin); }
    }

    // ================================================================
    // 7. CRUSH WAVE — Downward force slamming players into ground
    // ================================================================
    public static class CrushWave extends EnvironmentalAttack {

        public CrushWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crush_wave", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.2f);

            // Slam all players downward
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                if (player.getLocation().distance(center) > 6.0) continue;

                player.setVelocity(player.getVelocity().add(new Vector(0, -1.0, 0)));
            }

            // Impact visual
            DisplayBuilder.dustParticles(center, 80, 6.0, 150, 230, 255, 2.5f);
            DisplayBuilder.particleRing(center, 6.0, Particle.CLOUD, 20, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Ground shockwave particles expanding outward
            if (ticksAlive % 2 == 0) {
                double radius = ticksAlive * 0.5;
                DisplayBuilder.particleRing(c, radius, Particle.CLOUD, 12, null);
                DisplayBuilder.dustParticles(c, 6, radius, 180, 210, 255, 1.5f);
            }

            // Continued downward force for a few ticks
            if (ticksAlive <= 5) {
                World w = c.getWorld();
                for (Player player : w.getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                    if (player.getLocation().distance(c) > 6.0) continue;
                    player.setVelocity(player.getVelocity().add(new Vector(0, -0.5, 0)));
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CrushWave(plugin); }
    }

    // ================================================================
    // 8. MAGNETIC NORTH — Pull all players in one compass direction
    // ================================================================
    public static class MagneticNorth extends EnvironmentalAttack {

        private Vector driftDirection;

        public MagneticNorth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magnetic_north", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pick a random horizontal compass direction
            double angle = new Random().nextDouble() * 2 * Math.PI;
            driftDirection = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.4f);
            DisplayBuilder.dustParticles(center, 40, 8.0, 200, 200, 220, 1.5f);

            // Draw particle line showing drift direction
            Location from = center.clone();
            Location to = center.clone().add(driftDirection.clone().multiply(20));
            DisplayBuilder.particleLine(from, to, Particle.END_ROD, 2, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || driftDirection == null) return;
            World w = c.getWorld();

            // Continuously push all players in the drift direction
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                if (player.getLocation().distance(c) > 20.0) continue;

                player.setVelocity(player.getVelocity().add(driftDirection.clone().multiply(0.1)));
            }

            // Directional wind particles
            if (ticksAlive % 3 == 0) {
                Random rand = new Random();
                for (int i = 0; i < 6; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 20.0;
                    double oy = rand.nextDouble() * 4.0;
                    double oz = (rand.nextDouble() - 0.5) * 20.0;
                    Location from = c.clone().add(ox, oy, oz);
                    Location to = from.clone().add(driftDirection.clone().multiply(3));
                    DisplayBuilder.particleLine(from, to, Particle.CLOUD, 2, null);
                }
                DisplayBuilder.dustParticles(c, 8, 10.0, 180, 210, 255, 1.0f);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MagneticNorth(plugin); }
    }

    // ================================================================
    // 9. ORBIT LOCK — Force one target player in a circular path
    // ================================================================
    public static class OrbitLock extends EnvironmentalAttack {

        private Player lockedTarget;
        private double orbitAngle;

        public OrbitLock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbit_lock", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find nearest survival player as orbit target
            double closestDist = Double.MAX_VALUE;
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                double dist = player.getLocation().distance(center);
                if (dist < closestDist && dist <= 15.0) {
                    closestDist = dist;
                    lockedTarget = player;
                }
            }

            if (lockedTarget != null) {
                // Calculate initial angle from center to player
                double dx = lockedTarget.getLocation().getX() - center.getX();
                double dz = lockedTarget.getLocation().getZ() - center.getZ();
                orbitAngle = Math.atan2(dz, dx);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.4f);
            DisplayBuilder.particleRing(center, 4.0, Particle.END_ROD, 20, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || lockedTarget == null) return;
            if (!lockedTarget.isOnline() || lockedTarget.isDead()) return;

            // Advance orbit angle
            orbitAngle += Math.toRadians(6.0); // 6 degrees per tick

            // Compute tangent velocity to push player along circular path
            double radius = 4.0;
            double targetX = c.getX() + Math.cos(orbitAngle) * radius;
            double targetZ = c.getZ() + Math.sin(orbitAngle) * radius;

            Location playerLoc = lockedTarget.getLocation();
            double dx = targetX - playerLoc.getX();
            double dz = targetZ - playerLoc.getZ();

            if (lockedTarget.getGameMode() == GameMode.SURVIVAL && !lockedTarget.isInvulnerable()) {
                lockedTarget.setVelocity(new Vector(dx * 0.3, 0, dz * 0.3));
            }

            // Orbit ring particles
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.particleRing(c, radius, Particle.END_ROD, 12, null);
                DisplayBuilder.dustParticles(lockedTarget.getLocation(), 4, 0.5, 200, 200, 220, 1.0f);
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitLock(plugin); }
    }

    // ================================================================
    // 10. TIDAL BORE — Line of force sweeping in one direction
    // ================================================================
    public static class TidalBore extends EnvironmentalAttack {

        private Vector sweepDirection;
        private double sweepProgress;

        public TidalBore(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tidal_bore", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random sweep direction
            double angle = new Random().nextDouble() * 2 * Math.PI;
            sweepDirection = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();
            sweepProgress = -15.0; // Start behind center

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || sweepDirection == null) return;
            World w = c.getWorld();

            // Advance the wave front
            sweepProgress = -15.0 + (ticksAlive / 40.0) * 30.0;

            // Wave front location
            Location waveFront = c.clone().add(sweepDirection.clone().multiply(sweepProgress));

            // Perpendicular direction for the wave width
            Vector perp = new Vector(-sweepDirection.getZ(), 0, sweepDirection.getX());

            // Push players near the wave front
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;

                // Check if player is near the wave front line
                Vector toPlayer = player.getLocation().toVector().subtract(waveFront.toVector());
                double alongSweep = toPlayer.dot(sweepDirection);
                double perpDist = Math.abs(toPlayer.dot(perp));

                if (Math.abs(alongSweep) < 3.0 && perpDist < 8.0) {
                    player.setVelocity(player.getVelocity().add(sweepDirection.clone().multiply(0.5)));
                }
            }

            // Wave front particle line
            if (ticksAlive % 2 == 0) {
                for (int i = -8; i <= 8; i++) {
                    Location linePt = waveFront.clone().add(perp.clone().multiply(i));
                    DisplayBuilder.dustParticles(linePt, 3, 0.5, 180, 210, 255, 1.5f);
                }
                DisplayBuilder.particleRing(waveFront, 2.0, Particle.CLOUD, 8, null);
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(waveFront, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TidalBore(plugin); }
    }

    // ================================================================
    // 11. GRAVITY STORM — Random direction shifts every 10 ticks
    // ================================================================
    public static class GravityStorm extends EnvironmentalAttack {

        private Vector currentForce;
        private final Random random = new Random();

        public GravityStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_storm", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            currentForce = randomHorizontalForce();
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
            DisplayBuilder.dustParticles(center, 50, 8.0, 180, 210, 255, 1.8f);
        }

        private Vector randomHorizontalForce() {
            double angle = random.nextDouble() * 2 * Math.PI;
            double yComp = (random.nextDouble() - 0.5) * 0.4;
            return new Vector(Math.cos(angle) * 0.25, yComp, Math.sin(angle) * 0.25);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Shift direction every 10 ticks
            if (ticksAlive % 10 == 0) {
                currentForce = randomHorizontalForce();
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.5f + random.nextFloat());
            }

            // Apply current force to all players in range
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                if (player.getLocation().distance(c) > 12.0) continue;

                player.setVelocity(player.getVelocity().add(currentForce));
            }

            // Chaotic swirling particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    double ox = (random.nextDouble() - 0.5) * 24.0;
                    double oy = random.nextDouble() * 5.0;
                    double oz = (random.nextDouble() - 0.5) * 24.0;
                    Location pLoc = c.clone().add(ox, oy, oz);
                    if (pLoc.distance(c) <= 12.0) {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.5, 150, 230, 255, 1.2f);
                    }
                }
                DisplayBuilder.particleRing(c, 12.0, Particle.END_ROD, 8, null);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GravityStorm(plugin); }
    }

    // ================================================================
    // 12. MASS INCREASE — Cap player jump height, pin to ground
    // ================================================================
    public static class MassIncrease extends EnvironmentalAttack {

        public MassIncrease(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mass_increase", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.2f);
            DisplayBuilder.dustParticles(center, 60, 10.0, 200, 200, 220, 2.0f);
            DisplayBuilder.particleRing(center, 10.0, Particle.CLOUD, 16, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Suppress any upward velocity — cap Y at 0
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                if (player.getLocation().distance(c) > 10.0) continue;

                Vector vel = player.getVelocity();
                if (vel.getY() > 0) {
                    player.setVelocity(new Vector(vel.getX(), 0, vel.getZ()));
                }
            }

            // Heavy gravity particles pressing downward
            if (ticksAlive % 4 == 0) {
                Random rand = new Random();
                for (int i = 0; i < 8; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 20.0;
                    double oz = (rand.nextDouble() - 0.5) * 20.0;
                    Location top = c.clone().add(ox, 6, oz);
                    Location bottom = c.clone().add(ox, 0, oz);
                    if (top.distance(c) <= 12.0) {
                        DisplayBuilder.particleLine(top, bottom, Particle.CLOUD, 1, null);
                    }
                }
                DisplayBuilder.dustParticles(c, 10, 10.0, 200, 200, 220, 1.5f);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.15f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MassIncrease(plugin); }
    }

    // ================================================================
    // 13. SLINGSHOT — Pull then fling at high velocity (two phases)
    // ================================================================
    public static class Slingshot extends EnvironmentalAttack {

        private boolean flung = false;

        public Slingshot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slingshot", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
            DisplayBuilder.particleRing(center, 8.0, Particle.END_ROD, 20, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive <= 30) {
                // Phase 1: Pull players inward (30 ticks)
                for (Player player : w.getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                    double dist = player.getLocation().distance(c);
                    if (dist > 8.0 || dist < 0.3) continue;

                    Vector pull = c.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.2);
                    player.setVelocity(player.getVelocity().add(pull));
                }

                // Contracting ring
                if (ticksAlive % 3 == 0) {
                    double radius = 8.0 - (ticksAlive / 30.0) * 6.0;
                    DisplayBuilder.particleRing(c, Math.max(radius, 2.0), Particle.END_ROD, 16, null);
                    DisplayBuilder.dustParticles(c, 8, radius, 180, 210, 255, 1.2f);
                }

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.5f + (ticksAlive / 30.0f));
                }
            } else if (!flung) {
                // Phase 2: Fling all nearby players outward at high velocity
                flung = true;

                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.5f);
                DisplayBuilder.dustParticles(c, 80, 3.0, 150, 230, 255, 2.5f);
                DisplayBuilder.particleRing(c, 4.0, Particle.END_ROD, 30, null);

                for (Player player : w.getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                    double dist = player.getLocation().distance(c);
                    if (dist > 8.0) continue;

                    Vector fling = player.getLocation().toVector().subtract(c.toVector()).normalize().multiply(1.5);
                    fling.setY(0.6);
                    player.setVelocity(fling);
                }
            }

            // Post-fling dissipation particles
            if (ticksAlive > 30 && ticksAlive % 4 == 0) {
                double radius = 4.0 + ((ticksAlive - 30) / 30.0) * 10.0;
                DisplayBuilder.particleRing(c, radius, Particle.CLOUD, 10, null);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Slingshot(plugin); }
    }
}
