package com.blockforge.chaoscraft.nms.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Seer boss NMS flight AI. Hovers directly above the nearest player
 * at a configurable height, always facing them with unlimited rotation.
 *
 * Uses setDeltaMovement (NMS velocity) instead of Bukkit teleport
 * to preserve ModelEngine model interpolation.
 */
public class SeerFlightGoal extends Goal {

    private final Mob mob;
    private double hoverHeight = 15.0;
    private double moveSpeed = 0.3;
    private double detectionRange = 500.0;

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

        mob.getNavigation().stop();

        // PRIORITY 1: Always face the player — unlimited rotation
        double lookDx = currentTarget.getX() - mob.getX();
        double lookDz = currentTarget.getZ() - mob.getZ();
        double lookDy = (currentTarget.getY() + 1.0) - mob.getY();
        float yaw = (float) (Math.toDegrees(Math.atan2(-lookDx, lookDz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(lookDy, Math.sqrt(lookDx * lookDx + lookDz * lookDz))));

        // Force body + head rotation directly (NMS)
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setXRot(pitch);
        mob.xRotO = pitch;

        // Backup for ModelEngine h_head bone tracking
        mob.getLookControl().setLookAt(
                currentTarget.getX(), currentTarget.getY() + 1.0, currentTarget.getZ(),
                360.0f, 360.0f);

        // PRIORITY 2: Fly toward position directly above the player
        double targetX = currentTarget.getX();
        double targetY = currentTarget.getY() + hoverHeight;
        double targetZ = currentTarget.getZ();

        double dx = targetX - mob.getX();
        double dy = targetY - mob.getY();
        double dz = targetZ - mob.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.5) {
            // Basically at target — hard stop
            mob.setDeltaMovement(Vec3.ZERO);
        } else if (dist < 2.0) {
            // Close — scale speed down proportionally to avoid oscillation
            double scale = dist / 2.0;
            double speed = moveSpeed * scale * 0.4;
            mob.setDeltaMovement(new Vec3(
                    (dx / dist) * speed,
                    (dy / dist) * speed,
                    (dz / dist) * speed
            ));
        } else {
            // Far — fly at full configured speed
            double speed = Math.min(moveSpeed, dist * 0.05);
            mob.setDeltaMovement(new Vec3(
                    (dx / dist) * speed,
                    (dy / dist) * speed,
                    (dz / dist) * speed
            ));
        }
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
    public void setMoveSpeed(double speed) { this.moveSpeed = speed; }
    public void setDetectionRange(double range) { this.detectionRange = range; }

    public LivingEntity getCurrentTarget() { return currentTarget; }
}
