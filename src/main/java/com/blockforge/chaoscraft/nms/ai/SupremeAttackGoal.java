package com.blockforge.chaoscraft.nms.ai;

import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Custom melee/ranged attack goal for Supreme Calamitas.
 * Handles approach, swing animation, and damage dealing.
 * Works with both ground and flight phases.
 */
public class SupremeAttackGoal extends Goal {

    private final Mob mob;
    private final double speedModifier;
    private final boolean followEvenIfNotSeen;

    private int attackCooldown = 0;
    private int ticksUntilNextAttack = 0;
    private long lastCanUseCheck = 0;
    private static final long CAN_USE_COOLDOWN = 20L;

    // Configurable
    private int attackIntervalTicks = 20;
    private double attackReachSq = 16.0; // 4 blocks squared
    private float attackDamage = 25.0f;

    public SupremeAttackGoal(Mob mob, double speedModifier, boolean followEvenIfNotSeen) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followEvenIfNotSeen = followEvenIfNotSeen;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long gameTime = mob.level().getGameTime();
        if (gameTime - lastCanUseCheck < CAN_USE_COOLDOWN) {
            return false;
        }
        lastCanUseCheck = gameTime;

        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        return followEvenIfNotSeen || mob.getSensing().hasLineOfSight(target);
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        attackCooldown = 0;
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;

        // Look at target
        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);

        double distSq = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

        // Move toward target
        attackCooldown--;
        if (attackCooldown <= 0) {
            attackCooldown = adjustedTickDelay(4); // Re-path every ~4 ticks
            mob.getNavigation().moveTo(target, speedModifier);
        }

        // Attack if in range
        ticksUntilNextAttack = Math.max(ticksUntilNextAttack - 1, 0);
        if (distSq <= attackReachSq && ticksUntilNextAttack <= 0) {
            ticksUntilNextAttack = attackIntervalTicks;
            mob.swing(InteractionHand.MAIN_HAND);
            if (mob.level() instanceof ServerLevel serverLevel) {
                mob.doHurtTarget(serverLevel, target);
            }
        }
    }

    // Configuration setters
    public SupremeAttackGoal setAttackInterval(int ticks) {
        this.attackIntervalTicks = ticks;
        return this;
    }

    public SupremeAttackGoal setAttackReach(double blocks) {
        this.attackReachSq = blocks * blocks;
        return this;
    }

    public SupremeAttackGoal setAttackDamage(float damage) {
        this.attackDamage = damage;
        return this;
    }
}
