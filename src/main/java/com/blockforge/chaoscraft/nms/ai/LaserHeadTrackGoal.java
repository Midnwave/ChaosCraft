package com.blockforge.chaoscraft.nms.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * NMS AI goal that makes an entity's head (and body) track a specific player
 * at extreme angles — the entity never moves, only rotates to face the target.
 * <p>
 * Used for the Blue Moon laser ModelEngine entities: an invisible armor stand
 * with a laser beam model that visually tracks the target player. The head
 * can tilt at ridiculous angles (looking straight up/down) since the laser
 * fires from the boss in the sky to the player on the ground.
 * <p>
 * The entity is completely stationary — all vanilla AI goals are cleared
 * before this goal is added.
 */
public class LaserHeadTrackGoal extends Goal {

    private final Mob mob;
    private UUID targetUUID;

    public LaserHeadTrackGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    /**
     * Set the target player UUID to track. Null to stop tracking.
     */
    public void setTarget(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    @Override
    public boolean canUse() {
        return targetUUID != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetUUID != null;
    }

    @Override
    public void tick() {
        if (targetUUID == null) return;

        // Find the target player in the same world
        Player target = null;
        for (Player p : mob.level().players()) {
            if (p.getUUID().equals(targetUUID) && p.isAlive()) {
                target = p;
                break;
            }
        }
        if (target == null) return;

        // Calculate direction from entity to target
        Vec3 entityPos = mob.position();
        Vec3 targetPos = target.position().add(0, 1.0, 0); // Aim at chest height
        Vec3 dir = targetPos.subtract(entityPos);
        double horizontalDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);

        // Calculate yaw (horizontal rotation) — standard MC yaw
        float yaw = (float) (Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI));

        // Calculate pitch (vertical rotation) — allow extreme angles (-90 to +90)
        float pitch = (float) -(Math.atan2(dir.y, horizontalDist) * (180.0 / Math.PI));

        // Force-set both body and head rotation for maximum visual tracking
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setYBodyRot(yaw);
        mob.yBodyRotO = yaw;
        mob.setYHeadRot(yaw);

        mob.setXRot(pitch);
        mob.xRotO = pitch;

        // Kill all movement — entity is purely a visual anchor
        mob.setDeltaMovement(Vec3.ZERO);
        mob.getNavigation().stop();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);
    }
}
