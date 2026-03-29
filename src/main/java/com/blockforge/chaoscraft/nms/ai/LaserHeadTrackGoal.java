package com.blockforge.chaoscraft.nms.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * NMS AI goal that forces an entity to face a target player with UNLIMITED
 * head rotation — bypasses Minecraft's default ±45° pitch clamp on mobs.
 * <p>
 * Sets body rotation, head rotation, AND pitch every tick via direct NMS
 * field assignment. The entity never moves — it is purely a visual anchor
 * for the ModelEngine laser beam model with h_head bone.
 */
public class LaserHeadTrackGoal extends Goal {

    private final Mob mob;
    private UUID targetUUID;

    public LaserHeadTrackGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

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

        Player target = null;
        for (Player p : mob.level().players()) {
            if (p.getUUID().equals(targetUUID) && p.isAlive()) {
                target = p;
                break;
            }
        }
        if (target == null) return;

        Vec3 entityPos = mob.position();
        Vec3 targetPos = target.position().add(0, 1.0, 0);
        Vec3 dir = targetPos.subtract(entityPos);
        double horizontalDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);

        float yaw = (float) (Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI));
        float pitch = (float) -(Math.atan2(dir.y, horizontalDist) * (180.0 / Math.PI));

        // Force ALL rotation fields — bypasses Minecraft's head rotation clamp
        // Body rotation (yaw)
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setYBodyRot(yaw);
        mob.yBodyRotO = yaw;
        mob.setYHeadRot(yaw);

        // Pitch — set BOTH current and previous to prevent interpolation fighting
        mob.setXRot(pitch);
        mob.xRotO = pitch;

        // Also force via lookControl to prevent vanilla AI from resetting
        mob.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z);

        // Override the mob's max head rotation limits
        // These fields control how far the head can turn per tick
        // By calling lookAt directly AND setting fields, we bypass all clamping
        mob.lookAt(target, 360.0F, 360.0F);

        // Kill all movement
        mob.setDeltaMovement(Vec3.ZERO);
        mob.getNavigation().stop();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);
    }
}
