package com.blockforge.chaoscraft.weapons.ivory;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class IvoryEffectsManager {

    private final ChaosCraftPlugin plugin;
    private IvoryConfig config;

    public IvoryEffectsManager(ChaosCraftPlugin plugin, IvoryConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateConfig(IvoryConfig config) { this.config = config; }

    public void applyHitEffects(Player attacker, LivingEntity target) {
        Location hitLocation = target.getLocation();
        if (config.isScreenShakeEnabled()) applyScreenShake(target);
        if (config.isLightningEnabled()) spawnLightningEffect(hitLocation);
        if (config.isWhiteFlashEnabled()) applyWhiteFlash(target, config.getWhiteFlashDuration());
        if (config.isHitParticlesEnabled()) spawnHitParticles(hitLocation);
    }

    private void applyScreenShake(LivingEntity target) {
        var shake = new Vector((Math.random() - 0.5) * 0.1, 0.05, (Math.random() - 0.5) * 0.1);
        target.setVelocity(target.getVelocity().add(shake));
    }

    private void spawnLightningEffect(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        world.strikeLightningEffect(location.clone().add(0, 100, 0));
        spawnLightningParticles(location);
    }

    private void spawnLightningParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(0, 1, 0), 30, 0.5, 1.0, 0.5, 0.1);
        world.spawnParticle(Particle.END_ROD, location.clone().add(0, 0.5, 0), 15, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticle(Particle.FLASH, location.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
    }

    private void applyWhiteFlash(LivingEntity target, int durationTicks) {
        boolean wasGlowing = target.isGlowing();
        target.setGlowing(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isValid() && !wasGlowing) target.setGlowing(false);
        }, durationTicks);
        Location loc = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        World world = loc.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.END_ROD, loc, 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void spawnHitParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        var loc = location.clone().add(0, 1, 0);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 25, 0.4, 0.4, 0.4, 0.05);
        world.spawnParticle(Particle.END_ROD, loc, 15, 0.3, 0.3, 0.3, 0.08);
        world.spawnParticle(Particle.CRIT, loc, 20, 0.4, 0.4, 0.4, 0.3);
        world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
    }

    public void spawnParticleBurst(Location location, Particle particle, int count,
                                   double spreadX, double spreadY, double spreadZ, double speed) {
        World world = location.getWorld();
        if (world != null) world.spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, speed);
    }

    public void spawnParticleRing(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        if (world == null) return;
        double angleStep = Math.TAU / points;
        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            var point = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    public void spawnParticleSpiral(Location base, Particle particle, double radius,
                                    double height, int rotations, int particlesPerRotation) {
        World world = base.getWorld();
        if (world == null) return;
        int totalParticles = rotations * particlesPerRotation;
        double angleStep = Math.TAU * rotations / totalParticles;
        double heightStep = height / totalParticles;
        for (int i = 0; i < totalParticles; i++) {
            double angle = i * angleStep;
            double y = i * heightStep;
            var point = base.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }

    public void spawnParticleLine(Location from, Location to, Particle particle, double spacing) {
        World world = from.getWorld();
        if (world == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize().multiply(spacing);
        Location current = from.clone();
        for (double traveled = 0; traveled < distance; traveled += spacing) {
            world.spawnParticle(particle, current, 1, 0, 0, 0, 0);
            current.add(direction);
        }
    }

    public void spawnParticleBeam(Location from, Location to, double spacing) {
        World world = from.getWorld();
        if (world == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize().multiply(spacing);
        Location current = from.clone();
        for (double traveled = 0; traveled < distance; traveled += spacing) {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, current, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.END_ROD, current, 1, 0.05, 0.05, 0.05, 0);
            current.add(direction);
        }
    }

    public void spawnParticleSphere(Location center, Particle particle, double radius, int density) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < density; i++) {
            double theta = Math.random() * Math.TAU;
            double phi = Math.acos(2.0 * Math.random() - 1.0);
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
    }

    public void spawnShockwave(Location center, Particle particle, double maxRadius,
                               int durationTicks, int pointsPerRing) {
        new BukkitRunnable() {
            double currentRadius = 0.5;
            final double radiusStep = (maxRadius - 0.5) / durationTicks;
            int tick = 0;
            @Override public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                spawnParticleRing(center, particle, currentRadius, pointsPerRing);
                currentRadius += radiusStep;
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void spawnParticleDome(Location center, Particle particle, double radius, int density) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < density; i++) {
            double theta = Math.random() * Math.TAU;
            double phi = Math.random() * Math.PI / 2.0;
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(phi) * Math.sin(theta);
            world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
    }

    public int createSustainedEffect(Location location, Particle particle, int count,
                                     double spread, double speed, int intervalTicks, int durationTicks) {
        final int[] ticksRemaining = {durationTicks};
        return new BukkitRunnable() {
            @Override public void run() {
                if (ticksRemaining[0] <= 0) { cancel(); return; }
                World world = location.getWorld();
                if (world != null) world.spawnParticle(particle, location, count, spread, spread, spread, speed);
                ticksRemaining[0] -= intervalTicks;
            }
        }.runTaskTimer(plugin, 0L, intervalTicks).getTaskId();
    }

    public void cancelSustainedEffect(int taskId) { Bukkit.getScheduler().cancelTask(taskId); }

    public void spawnMeteorTrail(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.FLAME, location, 5, 0.1, 0.1, 0.1, 0.02);
        world.spawnParticle(Particle.SMOKE, location, 3, 0.1, 0.1, 0.1, 0.01);
        world.spawnParticle(Particle.END_ROD, location, 2, 0.05, 0.05, 0.05, 0);
    }

    public void spawnExplosionEffect(Location location, double size) {
        World world = location.getWorld();
        if (world == null) return;
        int count = (int) (size * 10);
        double spread = size * 0.5;
        world.spawnParticle(Particle.EXPLOSION, location, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.FLASH, location, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, count, spread, spread, spread, 0.1);
        world.spawnParticle(Particle.END_ROD, location, count / 2, spread, spread, spread, 0.15);
        world.spawnParticle(Particle.FIREWORK, location, count / 2, spread, spread, spread, 0.1);
    }
}
