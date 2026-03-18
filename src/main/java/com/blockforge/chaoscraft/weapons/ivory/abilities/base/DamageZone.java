package com.blockforge.chaoscraft.weapons.ivory.abilities.base;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;

public class DamageZone {

    public static final String ABILITY_DAMAGE_KEY = "chaoscraft_ability_damage";
    private static Plugin pluginInstance;

    private final UUID id = UUID.randomUUID();
    private final Player owner;
    private Location center;
    private Location endPoint;
    private Shape shape;
    private double radius, height, width, damage;
    private int tickInterval, durationTicks, remainingTicks;
    private boolean active;
    private final Set<UUID> damagedThisTick = new HashSet<>();
    private Predicate<LivingEntity> entityFilter;
    private boolean followOwner;

    public static void setPlugin(Plugin plugin) { pluginInstance = plugin; }

    public static DamageZone sphere(Player owner, Location center, double radius, double damage, int tickInterval, int durationTicks) {
        var zone = new DamageZone(owner);
        zone.center = center.clone(); zone.shape = Shape.SPHERE; zone.radius = radius;
        zone.damage = damage; zone.tickInterval = tickInterval;
        zone.durationTicks = durationTicks; zone.remainingTicks = durationTicks;
        return zone;
    }

    public static DamageZone cylinder(Player owner, Location center, double radius, double height, double damage, int tickInterval, int durationTicks) {
        var zone = new DamageZone(owner);
        zone.center = center.clone(); zone.shape = Shape.CYLINDER; zone.radius = radius; zone.height = height;
        zone.damage = damage; zone.tickInterval = tickInterval;
        zone.durationTicks = durationTicks; zone.remainingTicks = durationTicks;
        return zone;
    }

    public static DamageZone box(Player owner, Location center, double width, double height, double depth, double damage, int tickInterval, int durationTicks) {
        var zone = new DamageZone(owner);
        zone.center = center.clone(); zone.shape = Shape.BOX; zone.width = width; zone.height = height; zone.radius = depth;
        zone.damage = damage; zone.tickInterval = tickInterval;
        zone.durationTicks = durationTicks; zone.remainingTicks = durationTicks;
        return zone;
    }

    public static DamageZone line(Player owner, Location start, Location end, double width, double damage, int tickInterval, int durationTicks) {
        var zone = new DamageZone(owner);
        zone.center = start.clone(); zone.endPoint = end.clone(); zone.shape = Shape.LINE; zone.width = width;
        zone.damage = damage; zone.tickInterval = tickInterval;
        zone.durationTicks = durationTicks; zone.remainingTicks = durationTicks;
        return zone;
    }

    public static DamageZone cone(Player owner, Location apex, double length, double angle, double damage, int tickInterval, int durationTicks) {
        var zone = new DamageZone(owner);
        zone.center = apex.clone(); zone.shape = Shape.CONE; zone.radius = length; zone.width = angle;
        zone.damage = damage; zone.tickInterval = tickInterval;
        zone.durationTicks = durationTicks; zone.remainingTicks = durationTicks;
        return zone;
    }

    private DamageZone(Player owner) {
        this.owner = owner;
        this.active = true;
        this.entityFilter = this::isValidTarget;
    }

    private boolean isValidTarget(LivingEntity entity) {
        return !(entity instanceof Player) && !(entity instanceof ArmorStand);
    }

    public boolean tick(int currentTick) {
        if (!active) return false;
        remainingTicks--;
        if (remainingTicks <= 0) { active = false; return false; }
        damagedThisTick.clear();
        if (currentTick % tickInterval == 0) applyDamage();
        if (followOwner && owner != null && owner.isOnline()) center = owner.getLocation().clone();
        return true;
    }

    private void applyDamage() {
        if (center == null || center.getWorld() == null) return;
        for (Entity entity : getNearbyEntities()) {
            if (entity instanceof LivingEntity living
                    && !damagedThisTick.contains(entity.getUniqueId())
                    && entityFilter.test(living)
                    && isInZone(entity.getLocation())) {
                if (pluginInstance != null)
                    living.setMetadata(ABILITY_DAMAGE_KEY, new FixedMetadataValue(pluginInstance, true));
                living.damage(damage, owner);
                damagedThisTick.add(entity.getUniqueId());
                if (pluginInstance != null)
                    Bukkit.getScheduler().runTaskLater(pluginInstance, () -> {
                        if (living.isValid()) living.removeMetadata(ABILITY_DAMAGE_KEY, pluginInstance);
                    }, 1L);
            }
        }
    }

    private Collection<Entity> getNearbyEntities() {
        double searchRadius = switch (shape) {
            case SPHERE, CONE -> radius;
            case CYLINDER -> Math.max(radius, height);
            case BOX -> Math.max(Math.max(width, height), radius);
            case LINE -> endPoint != null ? center.distance(endPoint) + width : radius;
        };
        return center.getWorld().getNearbyEntities(center, searchRadius, searchRadius, searchRadius);
    }

    public boolean isInZone(Location loc) {
        if (loc.getWorld() != center.getWorld()) return false;
        return switch (shape) {
            case SPHERE -> loc.distanceSquared(center) <= radius * radius;
            case CYLINDER -> {
                double dx = loc.getX() - center.getX();
                double dz = loc.getZ() - center.getZ();
                double dist2d = dx * dx + dz * dz;
                double dy = loc.getY() - center.getY();
                yield dist2d <= radius * radius && dy >= 0 && dy <= height;
            }
            case BOX -> {
                double dx = Math.abs(loc.getX() - center.getX());
                double dy = loc.getY() - center.getY();
                double dz = Math.abs(loc.getZ() - center.getZ());
                yield dx <= width / 2.0 && dy >= 0 && dy <= height && dz <= radius / 2.0;
            }
            case LINE -> endPoint != null && distanceToLine(loc, center, endPoint) <= width / 2.0;
            case CONE -> {
                Vector toPoint = loc.toVector().subtract(center.toVector());
                double distance = toPoint.length();
                if (distance > radius) yield false;
                double angle = Math.atan2(Math.sqrt(toPoint.getX() * toPoint.getX() + toPoint.getZ() * toPoint.getZ()), toPoint.getY());
                yield angle <= width / 2.0;
            }
        };
    }

    private double distanceToLine(Location point, Location lineStart, Location lineEnd) {
        Vector line = lineEnd.toVector().subtract(lineStart.toVector());
        Vector toPoint = point.toVector().subtract(lineStart.toVector());
        double lineLength = line.length();
        if (lineLength == 0) return toPoint.length();
        double t = Math.max(0, Math.min(1, toPoint.dot(line) / (lineLength * lineLength)));
        Vector projection = lineStart.toVector().add(line.multiply(t));
        return point.toVector().distance(projection);
    }

    // --- Getters/Setters ---
    public UUID getId() { return id; }
    public Player getOwner() { return owner; }
    public Location getCenter() { return center.clone(); }
    public void setCenter(Location center) { this.center = center.clone(); }
    public Location getEndPoint() { return endPoint != null ? endPoint.clone() : null; }
    public void setEndPoint(Location endPoint) { this.endPoint = endPoint != null ? endPoint.clone() : null; }
    public Shape getShape() { return shape; }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getRemainingTicks() { return remainingTicks; }
    public void setFollowOwner(boolean followOwner) { this.followOwner = followOwner; }
    public void setEntityFilter(Predicate<LivingEntity> filter) { this.entityFilter = filter; }
    public void cancel() { active = false; remainingTicks = 0; }

    public enum Shape { SPHERE, CYLINDER, BOX, LINE, CONE }
}
