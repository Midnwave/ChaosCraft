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
    private double orbitRadius = 8.0;
    private double orbitSpeed = 0.015;   // radians per tick
    private double moveSpeed = 0.8;
    private double detectionRange = 500.0;

    private double orbitAngle = 0;
    private LivingEntity currentTarget;

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

        orbitAngle += orbitSpeed;
        if (orbitAngle > Math.PI * 2) orbitAngle -= Math.PI * 2;

        // Desired position: offset from target, hovering above at an angle
        double targetX = currentTarget.getX() + Math.cos(orbitAngle) * orbitRadius;
        double targetY = currentTarget.getY() + hoverHeight;
        double targetZ = currentTarget.getZ() + Math.sin(orbitAngle) * orbitRadius;

        // Smooth movement via NMS velocity
        double dx = targetX - mob.getX();
        double dy = targetY - mob.getY();
        double dz = targetZ - mob.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 0.5) {
            double speed = Math.min(moveSpeed * 0.05, dist * 0.08);
            Vec3 current = mob.getDeltaMovement();
            Vec3 desired = new Vec3(
                    (dx / dist) * speed,
                    (dy / dist) * speed,
                    (dz / dist) * speed
            );
            // Smooth lerp velocity (85% old + 15% new)
            mob.setDeltaMovement(
                    current.x * 0.85 + desired.x * 0.15,
                    current.y * 0.85 + desired.y * 0.15,
                    current.z * 0.85 + desired.z * 0.15
            );
        } else {
            // Close enough — hover in place
            mob.setDeltaMovement(Vec3.ZERO);
        }

        // Look at target — this makes ModelEngine h_head bone track the player
        mob.getLookControl().setLookAt(currentTarget, 60.0f, 60.0f);
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

    public void setHoverHeight(double height) { this.hoverHeight = height; }
    public void setOrbitRadius(double radius) { this.orbitRadius = radius; }
    public void setOrbitSpeed(double speed) { this.orbitSpeed = speed; }
    public void setMoveSpeed(double speed) { this.moveSpeed = speed; }
    public void setDetectionRange(double range) { this.detectionRange = range; }

    public LivingEntity getCurrentTarget() { return currentTarget; }
}
