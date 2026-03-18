package com.blockforge.chaoscraft.nms.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Custom AI goal that detects and targets ALL players within range,
 * not just the one who attacked. Selects the closest as primary target
 * but exposes the full list for multi-target attacks.
 */
public class MultiPlayerTargetGoal extends Goal {

    private final Mob mob;
    private final double detectionRange;
    private final double detectionRangeSq;
    private final List<Player> trackedPlayers = new ArrayList<>();
    private int scanCooldown = 0;
    private static final int SCAN_INTERVAL = 10; // Scan every 10 ticks

    public MultiPlayerTargetGoal(Mob mob, double detectionRange) {
        this.mob = mob;
        this.detectionRange = detectionRange;
        this.detectionRangeSq = detectionRange * detectionRange;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        scanForPlayers();
        return !trackedPlayers.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        scanCooldown++;
        if (scanCooldown >= SCAN_INTERVAL) {
            scanCooldown = 0;
            scanForPlayers();
        }
        return !trackedPlayers.isEmpty();
    }

    @Override
    public void start() {
        selectPrimaryTarget();
    }

    @Override
    public void tick() {
        // Re-evaluate primary target each tick
        if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
            selectPrimaryTarget();
        }

        // If primary target moved out of range, pick a new one
        if (mob.getTarget() != null && mob.distanceToSqr(mob.getTarget()) > detectionRangeSq) {
            selectPrimaryTarget();
        }
    }

    @Override
    public void stop() {
        mob.setTarget(null);
        trackedPlayers.clear();
    }

    private void scanForPlayers() {
        trackedPlayers.clear();
        for (Player player : mob.level().players()) {
            if (player.isAlive()
                    && !player.isSpectator()
                    && !player.isCreative()
                    && mob.distanceToSqr(player) <= detectionRangeSq) {
                trackedPlayers.add(player);
            }
        }
    }

    private void selectPrimaryTarget() {
        if (trackedPlayers.isEmpty()) return;

        // Pick closest living player
        Player closest = trackedPlayers.stream()
                .filter(Player::isAlive)
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);

        if (closest != null) {
            mob.setTarget(closest);
        }
    }

    /**
     * Returns all players currently being tracked (for multi-target attacks).
     */
    public List<Player> getTrackedPlayers() {
        return new ArrayList<>(trackedPlayers);
    }

    /**
     * Returns the number of tracked players.
     */
    public int getTrackedCount() {
        return trackedPlayers.size();
    }

    public double getDetectionRange() {
        return detectionRange;
    }
}
