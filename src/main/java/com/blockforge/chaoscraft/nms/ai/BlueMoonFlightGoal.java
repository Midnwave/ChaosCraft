package com.blockforge.chaoscraft.nms.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Blue Moon boss NMS flight AI. Orbits above the largest player cluster
 * at a configurable radius and height, with phase-based speed scaling.
 * <p>
 * Uses setDeltaMovement (NMS velocity) instead of Bukkit teleport
 * to preserve ModelEngine model interpolation.
 * <p>
 * Key differences from SeerFlightGoal:
 * - Orbital movement (circles around target) instead of hover-above
 * - Cluster-based targeting (finds the largest group of players)
 * - Phase-based speed multiplier
 * - Y-axis wobble in higher phases
 */
public class BlueMoonFlightGoal extends Goal {

    private final Mob mob;

    // Configurable parameters (set via setters from BossManager)
    private double floatHeight = 25.0;
    private double orbitRadius = 15.0;
    private double orbitSpeed = 0.02;
    private double moveSpeed = 0.3;
    private double detectionRange = 100.0;
    private double groupDetectionRadius = 20.0;

    // State
    private double orbitAngle = 0;
    private int currentPhase = 1;
    private Vec3 clusterCentroid = null;
    private int clusterReevaluateCooldown = 0;
    private int clusterReevaluateInterval = 60; // re-evaluate every 3 seconds

    public BlueMoonFlightGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── Setters for configuration ──

    public void setFloatHeight(double h) { this.floatHeight = h; }
    public void setOrbitRadius(double r) { this.orbitRadius = r; }
    public void setOrbitSpeed(double s) { this.orbitSpeed = s; }
    public void setMoveSpeed(double s) { this.moveSpeed = s; }
    public void setDetectionRange(double r) { this.detectionRange = r; }
    public void setGroupDetectionRadius(double r) { this.groupDetectionRadius = r; }
    public void setClusterReevaluateInterval(int ticks) { this.clusterReevaluateInterval = ticks; }
    public void setPhase(int phase) { this.currentPhase = Math.max(1, Math.min(4, phase)); }

    /** Get the current cluster centroid (for external use by BossManager). */
    public Vec3 getClusterCentroid() { return clusterCentroid; }

    // ── Goal lifecycle ──

    @Override
    public boolean canUse() {
        return findPlayers().size() > 0;
    }

    @Override
    public boolean canContinueToUse() {
        return findPlayers().size() > 0;
    }

    @Override
    public void start() {
        mob.setNoGravity(true);
        evaluateCluster();
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();

        // Re-evaluate player cluster periodically
        clusterReevaluateCooldown--;
        if (clusterReevaluateCooldown <= 0) {
            evaluateCluster();
            clusterReevaluateCooldown = clusterReevaluateInterval;
        }

        if (clusterCentroid == null) return;

        // ── Phase-based speed multiplier ──
        double speedMult = switch (currentPhase) {
            case 2 -> 1.5;
            case 3 -> 2.0;
            case 4 -> 2.5;
            default -> 1.0;
        };

        // ── Advance orbit angle ──
        orbitAngle += orbitSpeed * speedMult;
        if (orbitAngle > Math.PI * 2) orbitAngle -= Math.PI * 2;

        // ── Calculate target orbit position ──
        double targetX = clusterCentroid.x + orbitRadius * Math.cos(orbitAngle);
        double targetZ = clusterCentroid.z + orbitRadius * Math.sin(orbitAngle);
        double targetY = clusterCentroid.y + floatHeight;

        // Phase 2+: Y-axis wobble (sine wave oscillation)
        if (currentPhase >= 2) {
            targetY += Math.sin(orbitAngle * 3) * 1.5;
        }

        // Phase 4: descend 10 blocks lower
        if (currentPhase == 4) {
            targetY -= 10;
        }

        // ── Move toward target position using velocity ──
        double dx = targetX - mob.getX();
        double dy = targetY - mob.getY();
        double dz = targetZ - mob.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.5) {
            mob.setDeltaMovement(Vec3.ZERO);
        } else if (dist < 3.0) {
            // Scale down near target to prevent oscillation
            double scale = dist / 3.0;
            double speed = moveSpeed * speedMult * scale * 0.4;
            mob.setDeltaMovement(new Vec3(
                    (dx / dist) * speed,
                    (dy / dist) * speed,
                    (dz / dist) * speed
            ));
        } else {
            // Full speed toward target
            double speed = Math.min(moveSpeed * speedMult, dist * 0.05);
            mob.setDeltaMovement(new Vec3(
                    (dx / dist) * speed,
                    (dy / dist) * speed,
                    (dz / dist) * speed
            ));
        }

        // ── Face the cluster centroid (unlimited rotation via NMS direct assignment) ──
        double lookDx = clusterCentroid.x - mob.getX();
        double lookDz = clusterCentroid.z - mob.getZ();
        double lookDy = (clusterCentroid.y + 1.0) - mob.getY();

        float yaw = (float) Math.toDegrees(Math.atan2(-lookDx, lookDz));
        float pitch = (float) -Math.toDegrees(Math.atan2(lookDy,
                Math.sqrt(lookDx * lookDx + lookDz * lookDz)));

        // Direct NMS rotation assignment (bypasses Bukkit, works with ModelEngine)
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setXRot(pitch);
        mob.xRotO = pitch;

        // Backup for ModelEngine h_head bone tracking
        mob.getLookControl().setLookAt(
                clusterCentroid.x, clusterCentroid.y + 1.0, clusterCentroid.z,
                360.0f, 360.0f);
    }

    // ── Cluster detection ──

    /**
     * Find the largest cluster of survival players within detection range.
     * A cluster is defined as a group of players where each player has at least
     * one other player within groupDetectionRadius blocks.
     * Sets clusterCentroid to the average position of the largest cluster.
     */
    private void evaluateCluster() {
        List<Player> players = findPlayers();
        if (players.isEmpty()) {
            clusterCentroid = null;
            return;
        }

        // Single player — just target them
        if (players.size() == 1) {
            Player p = players.get(0);
            clusterCentroid = new Vec3(p.getX(), p.getY(), p.getZ());
            return;
        }

        // For each player, count neighbors within groupDetectionRadius
        // Then find the player with the most neighbors = cluster seed
        Player bestSeed = null;
        int bestCount = -1;

        for (Player p : players) {
            int neighbors = 0;
            for (Player other : players) {
                if (other == p) continue;
                if (p.distanceToSqr(other) <= groupDetectionRadius * groupDetectionRadius) {
                    neighbors++;
                }
            }
            if (neighbors > bestCount) {
                bestCount = neighbors;
                bestSeed = p;
            }
        }

        if (bestSeed == null) {
            // Fallback: just use first player
            Player p = players.get(0);
            clusterCentroid = new Vec3(p.getX(), p.getY(), p.getZ());
            return;
        }

        // Collect all players in the winning cluster (within radius of seed)
        List<Player> cluster = new ArrayList<>();
        cluster.add(bestSeed);
        for (Player p : players) {
            if (p == bestSeed) continue;
            if (bestSeed.distanceToSqr(p) <= groupDetectionRadius * groupDetectionRadius) {
                cluster.add(p);
            }
        }

        // Calculate centroid (average position)
        double sumX = 0, sumY = 0, sumZ = 0;
        for (Player p : cluster) {
            sumX += p.getX();
            sumY += p.getY();
            sumZ += p.getZ();
        }
        clusterCentroid = new Vec3(sumX / cluster.size(), sumY / cluster.size(), sumZ / cluster.size());
    }

    /**
     * Find all survival-mode players within detection range.
     */
    private List<Player> findPlayers() {
        return mob.level().getEntitiesOfClass(
                Player.class,
                mob.getBoundingBox().inflate(detectionRange),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
        );
    }

    /**
     * Get the list of players in the current cluster (for multi-beam targeting).
     * Returns up to maxTargets players sorted by distance to boss.
     */
    public List<Player> getClusterPlayers(int maxTargets) {
        List<Player> players = findPlayers();
        if (players.isEmpty() || clusterCentroid == null) return List.of();

        // Filter to cluster members (within groupDetectionRadius of centroid)
        List<Player> cluster = new ArrayList<>();
        for (Player p : players) {
            double dx = p.getX() - clusterCentroid.x;
            double dy = p.getY() - clusterCentroid.y;
            double dz = p.getZ() - clusterCentroid.z;
            if (dx * dx + dy * dy + dz * dz <= groupDetectionRadius * groupDetectionRadius * 4) {
                cluster.add(p);
            }
        }

        // Sort by distance to boss (closest first)
        cluster.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mob)));

        // Return up to maxTargets
        return cluster.subList(0, Math.min(maxTargets, cluster.size()));
    }
}
