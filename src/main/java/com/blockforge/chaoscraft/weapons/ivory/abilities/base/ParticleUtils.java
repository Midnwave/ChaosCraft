package com.blockforge.chaoscraft.weapons.ivory.abilities.base;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ParticleUtils {

    private ParticleUtils() {}

    public static void spawn(Location loc, Particle particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
        if (loc.getWorld() != null) loc.getWorld().spawnParticle(particle, loc, count, spreadX, spreadY, spreadZ, speed);
    }

    public static void spawnSingle(Location loc, Particle particle) {
        if (loc.getWorld() != null) loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0);
    }

    public static void soulFlame(Location loc, int count, double spread, double speed) { spawn(loc, Particle.SOUL_FIRE_FLAME, count, spread, spread, spread, speed); }
    public static void endRod(Location loc, int count, double spread, double speed) { spawn(loc, Particle.END_ROD, count, spread, spread, spread, speed); }
    public static void electricSpark(Location loc, int count, double spread, double speed) { spawn(loc, Particle.ELECTRIC_SPARK, count, spread, spread, spread, speed); }
    public static void flash(Location loc) { spawn(loc, Particle.FLASH, 1, 0, 0, 0, 0); }
    public static void whiteAsh(Location loc, int count, double spread, double speed) { spawn(loc, Particle.WHITE_ASH, count, spread, spread, spread, speed); }
    public static void glow(Location loc, int count, double spread, double speed) { spawn(loc, Particle.GLOW, count, spread, spread, spread, speed); }
    public static void firework(Location loc, int count, double spread, double speed) { spawn(loc, Particle.FIREWORK, count, spread, spread, spread, speed); }

    public static void ring(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        if (world == null) return;
        double angleStep = Math.TAU / points;
        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            world.spawnParticle(particle, center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
        }
    }

    public static void verticalRing(Location center, Particle particle, double radius, int points) {
        World world = center.getWorld();
        if (world == null) return;
        double angleStep = Math.TAU / points;
        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            world.spawnParticle(particle, center.clone().add(Math.cos(angle) * radius, Math.sin(angle) * radius, 0), 1, 0, 0, 0, 0);
        }
    }

    public static void sphere(Location center, Particle particle, double radius, int density) {
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

    public static void filledSphere(Location center, Particle particle, double radius, int density) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < density; i++) {
            double r = Math.random() * radius;
            double theta = Math.random() * Math.TAU;
            double phi = Math.acos(2.0 * Math.random() - 1.0);
            world.spawnParticle(particle, center.clone().add(r * Math.sin(phi) * Math.cos(theta), r * Math.sin(phi) * Math.sin(theta), r * Math.cos(phi)), 1, 0, 0, 0, 0);
        }
    }

    public static void line(Location from, Location to, Particle particle, double spacing) {
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

    public static void beam(Location from, Location to, double spacing) {
        World world = from.getWorld();
        if (world == null) return;
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

    public static void spiral(Location base, Particle particle, double radius, double height, int rotations, int particlesPerRotation) {
        World world = base.getWorld();
        if (world == null) return;
        int totalParticles = rotations * particlesPerRotation;
        double angleStep = Math.TAU * rotations / totalParticles;
        double heightStep = height / totalParticles;
        for (int i = 0; i < totalParticles; i++) {
            double angle = i * angleStep;
            double y = i * heightStep;
            world.spawnParticle(particle, base.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
        }
    }

    public static void doubleHelix(Location base, Particle p1, Particle p2, double radius, double height, int rotations, int points) {
        World world = base.getWorld();
        if (world == null) return;
        double angleStep = Math.TAU * rotations / points;
        double heightStep = height / points;
        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            double y = i * heightStep;
            world.spawnParticle(p1, base.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
            world.spawnParticle(p2, base.clone().add(Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius), 1, 0, 0, 0, 0);
        }
    }

    public static void dome(Location center, Particle particle, double radius, int density) {
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

    public static void crescent(Location center, Particle particle, double radius, double startAngle, double endAngle, int points) {
        World world = center.getWorld();
        if (world == null) return;
        double step = (endAngle - startAngle) / points;
        for (int i = 0; i <= points; i++) {
            double angle = startAngle + i * step;
            world.spawnParticle(particle, center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
        }
    }

    public static void expandingRing(ChaosCraftPlugin plugin, Location center, Particle particle, double startRadius, double endRadius, int durationTicks, int points) {
        new BukkitRunnable() {
            double currentRadius = startRadius;
            final double step = (endRadius - startRadius) / durationTicks;
            int tick = 0;
            @Override public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                ring(center, particle, currentRadius, points);
                currentRadius += step; tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void shrinkingSphere(ChaosCraftPlugin plugin, Location center, Particle particle, double startRadius, double endRadius, int durationTicks, int density) {
        new BukkitRunnable() {
            double currentRadius = startRadius;
            final double step = (startRadius - endRadius) / durationTicks;
            int tick = 0;
            @Override public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                sphere(center, particle, currentRadius, density);
                currentRadius -= step; tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void rotatingRing(ChaosCraftPlugin plugin, Location center, Particle particle, double radius, int points, int durationTicks) {
        new BukkitRunnable() {
            double rotation = 0;
            int tick = 0;
            @Override public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                World world = center.getWorld();
                if (world == null) { cancel(); return; }
                double angleStep = Math.TAU / points;
                for (int i = 0; i < points; i++) {
                    double angle = i * angleStep + rotation;
                    world.spawnParticle(particle, center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
                }
                rotation += 0.1; tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static BlockDisplay createBlockDisplay(Location loc, Material material) {
        if (loc.getWorld() == null) return null;
        return loc.getWorld().spawn(loc, BlockDisplay.class, d -> d.setBlock(material.createBlockData()));
    }

    public static BlockDisplay createGlowingBlockDisplay(Location loc, Material material) {
        var display = createBlockDisplay(loc, material);
        if (display != null) { display.setGlowing(true); display.setBrightness(new Brightness(15, 15)); }
        return display;
    }

    public static void setScale(Display display, float scale) {
        var t = display.getTransformation();
        display.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(), new Vector3f(scale, scale, scale), t.getRightRotation()));
    }

    public static void setRotation(Display display, float pitch, float yaw, float roll) {
        var t = display.getTransformation();
        var rotation = new Quaternionf().rotateXYZ((float) Math.toRadians(pitch), (float) Math.toRadians(yaw), (float) Math.toRadians(roll));
        display.setTransformation(new Transformation(t.getTranslation(), rotation, t.getScale(), t.getRightRotation()));
    }

    public static void animateScale(ChaosCraftPlugin plugin, Display display, float startScale, float endScale, int durationTicks) {
        new BukkitRunnable() {
            float current = startScale;
            final float step = (endScale - startScale) / durationTicks;
            int tick = 0;
            @Override public void run() {
                if (tick >= durationTicks || !display.isValid()) { cancel(); return; }
                setScale(display, current); current += step; tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static void animateRotation(ChaosCraftPlugin plugin, Display display, int durationTicks, float degreesPerTick) {
        new BukkitRunnable() {
            float rotation = 0;
            int tick = 0;
            @Override public void run() {
                if (tick >= durationTicks || !display.isValid()) { cancel(); return; }
                setRotation(display, 0, rotation, 0); rotation += degreesPerTick; tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static ItemDisplay createItemDisplay(Location loc, ItemStack item) {
        if (loc.getWorld() == null) return null;
        return loc.getWorld().spawn(loc, ItemDisplay.class, d -> d.setItemStack(item));
    }

    public static ItemDisplay createGlowingItemDisplay(Location loc, ItemStack item) {
        var display = createItemDisplay(loc, item);
        if (display != null) { display.setGlowing(true); display.setBrightness(new Brightness(15, 15)); }
        return display;
    }

    public static void playSound(Location loc, Sound sound, float volume, float pitch) {
        if (loc.getWorld() != null) loc.getWorld().playSound(loc, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    public static void playCustomSound(Location loc, String soundKey, float volume, float pitch) {
        if (loc.getWorld() != null) loc.getWorld().playSound(loc, soundKey, SoundCategory.PLAYERS, volume, pitch);
    }
}
