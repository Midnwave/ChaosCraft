package com.blockforge.chaoscraft.nms.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Flight AI goal for Supreme Calamitas. Handles hovering, strafing,
 * circling around targets, and smooth altitude transitions.
 *
 * When flying: hovers at configurable height above target,
 * circles/strafes, dives for attack runs, then re-ascends.
 *
 * When grounded: defers to SupremeAttackGoal for melee.
 */
public class SupremeFlightGoal extends Goal {

    private final Mob mob;
    private boolean flying = false;

    // Flight config
    private double hoverHeight = 15.0;
    private double maxHoverHeight = 25.0;
    private double flySpeed = 0.8;
    private double circleRadius = 12.0;
    private double circleSpeed = 0.03; // radians per tick

    // State
    private double circleAngle = 0;
    private FlightState state = FlightState.HOVERING;
    private int stateTimer = 0;
    private int diveRecoveryTicks = 0;

    // Dive attack
    private Vec3 diveTarget = null;
    private static final int DIVE_COOLDOWN = 100; // 5 seconds between dives
    private int diveCooldown = 0;

    public enum FlightState {
        HOVERING,       // Circling above target
        STRAFING,       // Moving laterally
        DIVING,         // Dive attack toward target
        ASCENDING,      // Returning to hover altitude
        GROUNDED        // On the ground (flight disabled)
    }

    public SupremeFlightGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return flying && mob.getTarget() != null && mob.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return flying && mob.getTarget() != null && mob.getTarget().isAlive();
    }

    @Override
    public void start() {
        state = FlightState.ASCENDING;
        stateTimer = 0;
        mob.setNoGravity(true);
    }

    @Override
    public void stop() {
        if (!flying) {
            mob.setNoGravity(false);
            state = FlightState.GROUNDED;
        }
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        stateTimer++;
        diveCooldown = Math.max(diveCooldown - 1, 0);

        switch (state) {
            case HOVERING -> tickHover(target);
            case STRAFING -> tickStrafe(target);
            case DIVING -> tickDive(target);
            case ASCENDING -> tickAscend(target);
            case GROUNDED -> {} // Do nothing in grounded state
        }
    }

    private void tickHover(LivingEntity target) {
        // Circle around target at hover height
        circleAngle += circleSpeed;
        double targetY = target.getY() + hoverHeight;
        double targetX = target.getX() + Math.cos(circleAngle) * circleRadius;
        double targetZ = target.getZ() + Math.sin(circleAngle) * circleRadius;

        moveToSmooth(targetX, targetY, targetZ);

        // Look at target
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

        // Randomly transition to strafing or diving
        if (stateTimer > 60 && mob.getRandom().nextFloat() < 0.02f) {
            state = FlightState.STRAFING;
            stateTimer = 0;
        }
        if (diveCooldown <= 0 && stateTimer > 40 && mob.getRandom().nextFloat() < 0.015f) {
            state = FlightState.DIVING;
            diveTarget = target.position();
            stateTimer = 0;
        }
    }

    private void tickStrafe(LivingEntity target) {
        // Fast lateral movement
        double strafeAngle = circleAngle + (stateTimer * 0.08);
        double strafeRadius = circleRadius * 0.6;
        double targetX = target.getX() + Math.cos(strafeAngle) * strafeRadius;
        double targetZ = target.getZ() + Math.sin(strafeAngle) * strafeRadius;
        double targetY = target.getY() + hoverHeight * 0.7;

        moveToSmooth(targetX, targetY, targetZ);
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

        // Return to hovering after a few seconds
        if (stateTimer > 60) {
            state = FlightState.HOVERING;
            stateTimer = 0;
        }
    }

    private void tickDive(LivingEntity target) {
        if (diveTarget == null) {
            state = FlightState.ASCENDING;
            stateTimer = 0;
            return;
        }

        // Dive toward the target's position (frozen at start of dive)
        double dx = diveTarget.x - mob.getX();
        double dy = (diveTarget.y + 1.0) - mob.getY(); // Aim at body height
        double dz = diveTarget.z - mob.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 2.0 || stateTimer > 40) {
            // Dive complete — ascend
            state = FlightState.ASCENDING;
            stateTimer = 0;
            diveCooldown = DIVE_COOLDOWN;
            diveTarget = null;
            return;
        }

        // Fast dive movement
        double diveSpeed = flySpeed * 1.8;
        double nx = dx / dist;
        double ny = dy / dist;
        double nz = dz / dist;
        mob.setDeltaMovement(new Vec3(nx * diveSpeed * 0.05, ny * diveSpeed * 0.05, nz * diveSpeed * 0.05));

        mob.getLookControl().setLookAt(target, 60.0f, 60.0f);
    }

    private void tickAscend(LivingEntity target) {
        // Rise back to hover height
        double targetY = target.getY() + hoverHeight;
        double currentY = mob.getY();

        if (currentY >= targetY - 1.0 || stateTimer > 60) {
            state = FlightState.HOVERING;
            stateTimer = 0;
            return;
        }

        double targetX = target.getX() + Math.cos(circleAngle) * circleRadius;
        double targetZ = target.getZ() + Math.sin(circleAngle) * circleRadius;

        moveToSmooth(targetX, targetY, targetZ);
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
    }

    private void moveToSmooth(double x, double y, double z) {
        double dx = x - mob.getX();
        double dy = y - mob.getY();
        double dz = z - mob.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.5) return;

        double speed = Math.min(flySpeed * 0.05, dist * 0.1);
        mob.setDeltaMovement(new Vec3(
                (dx / dist) * speed,
                (dy / dist) * speed,
                (dz / dist) * speed
        ));
    }

    // --- Public API ---

    public void setFlying(boolean flying) {
        this.flying = flying;
        if (flying) {
            mob.setNoGravity(true);
            state = FlightState.ASCENDING;
        } else {
            mob.setNoGravity(false);
            state = FlightState.GROUNDED;
        }
        stateTimer = 0;
    }

    public boolean isFlying() {
        return flying;
    }

    public FlightState getState() {
        return state;
    }

    public void setHoverHeight(double min, double max) {
        this.hoverHeight = min;
        this.maxHoverHeight = max;
    }

    public void setFlySpeed(double speed) {
        this.flySpeed = speed;
    }

    public void setCircleRadius(double radius) {
        this.circleRadius = radius;
    }

    public void setCircleSpeed(double speed) {
        this.circleSpeed = speed;
    }
}
