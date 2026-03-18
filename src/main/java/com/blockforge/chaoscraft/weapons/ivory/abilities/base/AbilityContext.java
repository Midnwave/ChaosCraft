package com.blockforge.chaoscraft.weapons.ivory.abilities.base;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AbilityContext {

    private final Player player;
    private final Location origin;
    private final long startTime;

    public AbilityContext(Player player) {
        this.player = player;
        this.origin = player.getLocation().clone();
        origin.setYaw(0.0F);
        origin.setPitch(0.0F);
        this.startTime = System.currentTimeMillis();
    }

    public Player getPlayer() { return player; }
    public Location getOrigin() { return origin.clone(); }
    public Location getOriginAtY(double y) { var loc = origin.clone(); loc.setY(y); return loc; }
    public Location getOffset(double x, double y, double z) { return origin.clone().add(x, y, z); }
    public Location getForward(double distance) { return origin.clone().add(0, 0, -distance); }
    public Location getBehind(double distance) { return origin.clone().add(0, 0, distance); }
    public Location getRight(double distance) { return origin.clone().add(distance, 0, 0); }
    public Location getLeft(double distance) { return origin.clone().add(-distance, 0, 0); }
    public Location getAbove(double distance) { return origin.clone().add(0, distance, 0); }
    public Location getBelow(double distance) { return origin.clone().add(0, -distance, 0); }
    public long getElapsedMs() { return System.currentTimeMillis() - startTime; }
    public long getElapsedTicks() { return getElapsedMs() / 50L; }
    public Vector getForwardDirection() { return new Vector(0, 0, -1); }
    public Vector getRightDirection() { return new Vector(1, 0, 0); }
    public Vector getUpDirection() { return new Vector(0, 1, 0); }

    public Location getCirclePoint(double radius, double angle, double yOffset) {
        return origin.clone().add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
    }

    public Location getSpherePoint(double radius, double theta, double phi) {
        double x = radius * Math.sin(phi) * Math.cos(theta);
        double y = radius * Math.cos(phi);
        double z = radius * Math.sin(phi) * Math.sin(theta);
        return origin.clone().add(x, y, z);
    }
}
