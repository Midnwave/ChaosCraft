package com.blockforge.chaoscraft.nms.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Seer boss NMS flight AI. Hovers above players at an offset angle,
 * slowly orbits, and uses getLookControl to face the target so
 * ModelEngine h_head bone tracks properly.
 *
 * Uses setDeltaMovement (NMS velocity) instead of Bukkit teleport
 * to preserve ModelEngine model interpolation.
 */
public class SeerFlightGoal extends Goal {

    private final Mob mob;
    private double hoverHeight = 15.0;
    private double orbitRadius = 8.0;      // min distance from player
    private double maxOrbitRadius = 20.0;  // max distance from player
    private double orbitSpeed = 0.015;   // radians per tick
    private double moveSpeed = 0.8;
    private double detectionRange = 500.0;

    private LivingEntity currentTarget;
    private double anchorX, anchorY, anchorZ; // The spot the boss picked to hover at
    private boolean hasAnchor = false;
    private int anchorTicks = 0;
    private int anchorDuration = 200; // Stay at anchor for 10 seconds before picking new spot
    private static final java.util.Random RAND = new java.util.Random();

    public SeerFlightGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return findTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return currentTarget != null && currentTarget.isAlive()
                && mob.distanceToSqr(currentTarget) < detectionRange * detectionRange;
    }

    @Override
    public void start() {
        mob.setNoGravity(true);
        currentTarget = findTarget();
    }

    @Override
    public void stop() {
        // Don't disable gravity — boss should always float
    }

    @Override
    public void tick() {
        if (currentTarget == null || !currentTarget.isAlive()) {
            currentTarget = findTarget();
            if (currentTarget == null) return;
        }

        // 20% chance per second to swap target if multiple players nearby
        if (mob.getRandom().nextFloat() < 0.01f) {
            LivingEntity newTarget = findTarget();
            if (newTarget != null) {
                currentTarget = newTarget;
            }
        }

        mob.getNavigation().stop();

        // Pick an anchor spot near the player and STAY there
        if (!hasAnchor || anchorTicks >= anchorDuration) {
            pickNewAnchor();
        }
        anchorTicks++;

        // If player moved far from anchor, pick a new one sooner
        double playerDistToAnchor = Math.sqrt(
                Math.pow(currentTarget.getX() - anchorX, 2) +
                Math.pow(currentTarget.getZ() - anchorZ, 2));
        if (playerDistToAnchor > 30) {
            pickNewAnchor();
        }

        // Move toward anchor position
        double dx = anchorX - mob.getX();
        double dy = anchorY - mob.getY();
        double dz = anchorZ - mob.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        Vec3 currentVel = mob.getDeltaMovement();

        if (dist > 1.5) {
            // Fly toward anchor at reasonable speed
            double speed = Math.min(moveSpeed * 0.5, dist * 0.04);
            Vec3 desiredVel = new Vec3(
                    (dx / dist) * speed,
                    (dy / dist) * speed,
                    (dz / dist) * speed
            );
            // Smooth: 70% old + 30% new
            mob.setDeltaMovement(new Vec3(
                    currentVel.x * 0.7 + desiredVel.x * 0.3,
                    currentVel.y * 0.7 + desiredVel.y * 0.3,
                    currentVel.z * 0.7 + desiredVel.z * 0.3
            ));
        } else {
            // At anchor — hover still
            mob.setDeltaMovement(Vec3.ZERO);
        }

        // Always look at the player — max possible speed for instant tracking
        // 360f yaw + 360f pitch = no rotation limit per tick
        mob.getLookControl().setLookAt(
                currentTarget.getX(),
                currentTarget.getY() + 1.0,
                currentTarget.getZ(),
                360.0f, 360.0f);
    }

    /**
     * Find nearest player within detection range.
     */
    private LivingEntity findTarget() {
        List<Player> players = mob.level().getEntitiesOfClass(
                Player.class,
                mob.getBoundingBox().inflate(detectionRange),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
        );

        if (players.isEmpty()) return null;

        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : players) {
            double d = mob.distanceToSqr(p);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = p;
            }
        }
        return nearest;
    }

    // --- Config setters ---

    /**
     * Pick a random spot near the player to hover at.
     * Stays 8-15 blocks away horizontally, at hover height above.
     */
    private void pickNewAnchor() {
        if (currentTarget == null) return;
        double angle = RAND.nextDouble() * Math.PI * 2;
        double dist = orbitRadius + RAND.nextDouble() * (maxOrbitRadius - orbitRadius);
        anchorX = currentTarget.getX() + Math.cos(angle) * dist;
        anchorY = currentTarget.getY() + hoverHeight;
        anchorZ = currentTarget.getZ() + Math.sin(angle) * dist;
        hasAnchor = true;
        anchorTicks = 0;
        anchorDuration = 150 + RAND.nextInt(100); // 7.5-12.5 seconds
    }

    public void setHoverHeight(double height) { this.hoverHeight = height; }
    public void setOrbitRadius(double min, double max) { this.orbitRadius = min; this.maxOrbitRadius = max; }
    public void setOrbitRadius(double radius) { this.orbitRadius = radius; this.maxOrbitRadius = radius + 10; }
    public void setOrbitSpeed(double speed) { this.orbitSpeed = speed; }
    public void setMoveSpeed(double speed) { this.moveSpeed = speed; }
    public void setDetectionRange(double range) { this.detectionRange = range; }

    public LivingEntity getCurrentTarget() { return currentTarget; }
}
