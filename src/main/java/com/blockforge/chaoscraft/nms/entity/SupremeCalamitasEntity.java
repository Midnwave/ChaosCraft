package com.blockforge.chaoscraft.nms.entity;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.nms.ai.MultiPlayerTargetGoal;
import com.blockforge.chaoscraft.nms.ai.SupremeAttackGoal;
import com.blockforge.chaoscraft.nms.ai.SupremeFlightGoal;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.entity.CraftVindicator;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

/**
 * Supreme Calamitas — Custom NMS AI boss entity.
 *
 * Built on a Vindicator base with all default AI stripped and replaced
 * with custom goals for multi-target detection, flight, and smart combat.
 *
 * Features:
 * - 300-block detection range (configurable)
 * - Flying + ground phase transitions
 * - Multi-player targeting (tracks all players, not just attacker)
 * - Configurable health, speed, held item
 * - Flame particle orbits
 * - Dive attacks from above
 * - Smart pathfinding with obstacle avoidance
 */
public class SupremeCalamitasEntity {

    private final ChaosCraftPlugin plugin;
    private final Vindicator bukkitEntity;
    private final PathfinderMob nmsMob;
    private final UUID entityUuid;

    // AI goals (stored for debug access)
    private final MultiPlayerTargetGoal targetGoal;
    private final SupremeAttackGoal attackGoal;
    private final SupremeFlightGoal flightGoal;

    // Particle orbit state
    private double flameOrbitAngle = 0;
    private double flameOrbitAngle2 = Math.PI; // Second orbit offset by 180 degrees
    private BukkitTask tickTask;

    // Config
    private double maxHealth;
    private double detectionRange;
    private double groundSpeed;
    private double flySpeed;
    private double attackDamage;
    private int attackInterval;
    private double orbitRadius = 2.5;
    private double orbitSpeed = 0.08;

    // State
    private boolean alive = false;
    private boolean inFlightPhase = false;

    public SupremeCalamitasEntity(ChaosCraftPlugin plugin, Location spawnLoc,
                                   double maxHealth, double detectionRange,
                                   double groundSpeed, double flySpeed,
                                   double attackDamage, int attackInterval,
                                   ItemStack heldItem) {
        this.plugin = plugin;
        this.maxHealth = maxHealth;
        this.detectionRange = detectionRange;
        this.groundSpeed = groundSpeed;
        this.flySpeed = flySpeed;
        this.attackDamage = attackDamage;
        this.attackInterval = attackInterval;

        // 1. Spawn Vindicator base
        this.bukkitEntity = (Vindicator) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VINDICATOR);
        bukkitEntity.customName(Component.text("Supreme Calamitas", NamedTextColor.RED, TextDecoration.BOLD));
        bukkitEntity.setCustomNameVisible(true);
        bukkitEntity.setCanJoinRaid(false);
        bukkitEntity.setRemoveWhenFarAway(false);
        bukkitEntity.setPersistent(true);

        // Set held item
        if (heldItem != null && !heldItem.getType().isAir()) {
            bukkitEntity.getEquipment().setItemInMainHand(heldItem);
            bukkitEntity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        // 2. Get NMS handle
        this.nmsMob = ((CraftVindicator) bukkitEntity).getHandle();
        this.entityUuid = bukkitEntity.getUniqueId();

        // 3. Set attributes
        var healthAttr = nmsMob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(maxHealth);
            nmsMob.setHealth((float) maxHealth);
        }

        var speedAttr = nmsMob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(groundSpeed);

        var damageAttr = nmsMob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) damageAttr.setBaseValue(attackDamage);

        var followAttr = nmsMob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followAttr != null) followAttr.setBaseValue(detectionRange);

        // 4. Clear all default AI
        nmsMob.goalSelector.removeAllGoals(g -> true);
        nmsMob.targetSelector.removeAllGoals(g -> true);

        // 5. Register custom goals
        // Priority 0 = highest. Lower number = checked first.
        nmsMob.goalSelector.addGoal(0, new FloatGoal(nmsMob));

        this.flightGoal = new SupremeFlightGoal(nmsMob);
        flightGoal.setFlySpeed(flySpeed);
        flightGoal.setHoverHeight(15.0, 25.0);
        flightGoal.setCircleRadius(12.0);
        nmsMob.goalSelector.addGoal(2, flightGoal);

        this.attackGoal = new SupremeAttackGoal(nmsMob, groundSpeed * 2.5, true);
        attackGoal.setAttackInterval(attackInterval);
        attackGoal.setAttackReach(4.0);
        nmsMob.goalSelector.addGoal(4, attackGoal);

        nmsMob.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(nmsMob, groundSpeed));
        nmsMob.goalSelector.addGoal(8, new RandomLookAroundGoal(nmsMob));

        this.targetGoal = new MultiPlayerTargetGoal(nmsMob, detectionRange);
        nmsMob.targetSelector.addGoal(1, targetGoal);

        // 6. Start tick loop for particles and state management
        this.alive = true;
        startTickLoop();
    }

    private void startTickLoop() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!alive || bukkitEntity.isDead()) {
                    alive = false;
                    cancel();
                    return;
                }

                // Flame particle orbits
                tickFlameOrbits();

                // Additional soul fire orbit (offset)
                tickSoulFireOrbits();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void tickFlameOrbits() {
        flameOrbitAngle += orbitSpeed;
        Location base = bukkitEntity.getLocation().add(0, 1.2, 0);

        // Primary flame orbit (horizontal circle)
        for (int i = 0; i < 3; i++) {
            double angle = flameOrbitAngle + (i * (2 * Math.PI / 3));
            double x = Math.cos(angle) * orbitRadius;
            double z = Math.sin(angle) * orbitRadius;
            Location particleLoc = base.clone().add(x, 0, z);
            base.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private void tickSoulFireOrbits() {
        flameOrbitAngle2 += orbitSpeed * 0.7;
        Location base = bukkitEntity.getLocation().add(0, 1.8, 0);

        // Secondary soul fire orbit (tilted ellipse — varies Y axis)
        for (int i = 0; i < 2; i++) {
            double angle = flameOrbitAngle2 + (i * Math.PI);
            double x = Math.cos(angle) * (orbitRadius * 0.6);
            double y = Math.sin(angle) * 1.0; // Vertical oscillation
            double z = Math.sin(angle) * (orbitRadius * 0.6);
            Location particleLoc = base.clone().add(x, y, z);
            base.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    // --- Phase transitions ---

    /**
     * Enable flight phase — switches from ground AI to flying AI.
     */
    public void enableFlight() {
        inFlightPhase = true;
        flightGoal.setFlying(true);

        // Switch to FlyingPathNavigation for 3D pathfinding
        // Paper exposes navigation via getNavigation(), set via Bukkit pathfinder API
        // Use Bukkit entity to switch navigation mode
        bukkitEntity.setAI(false);
        nmsMob.setNoGravity(true);
        bukkitEntity.setAI(true);

        plugin.getLogger().info("[SC] Supreme Calamitas entering flight phase.");
    }

    /**
     * Disable flight — return to ground combat.
     */
    public void disableFlight() {
        inFlightPhase = false;
        flightGoal.setFlying(false);

        // Switch back to GroundPathNavigation
        // Return to ground navigation
        nmsMob.setNoGravity(false);

        plugin.getLogger().info("[SC] Supreme Calamitas returning to ground phase.");
    }

    // --- Public API ---

    public void remove() {
        alive = false;
        if (tickTask != null) tickTask.cancel();
        if (!bukkitEntity.isDead()) bukkitEntity.remove();
    }

    public boolean isAlive() {
        return alive && !bukkitEntity.isDead();
    }

    public boolean isInFlightPhase() {
        return inFlightPhase;
    }

    public Vindicator getBukkitEntity() {
        return bukkitEntity;
    }

    public Mob getNmsMob() {
        return nmsMob;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public double getCurrentHealth() {
        return bukkitEntity.getHealth();
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getHealthPercent() {
        return maxHealth > 0 ? (getCurrentHealth() / maxHealth) * 100.0 : 0;
    }

    public Location getLocation() {
        return bukkitEntity.getLocation();
    }

    public List<Player> getTrackedPlayers() {
        return targetGoal.getTrackedPlayers().stream()
                .map(nmsPlayer -> Bukkit.getPlayer(nmsPlayer.getUUID()))
                .filter(p -> p != null)
                .toList();
    }

    public int getTrackedPlayerCount() {
        return targetGoal.getTrackedCount();
    }

    public SupremeFlightGoal.FlightState getFlightState() {
        return flightGoal.getState();
    }

    public double getDetectionRange() {
        return detectionRange;
    }

    // --- Debug info ---

    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("§5--- Supreme Calamitas Debug ---\n");
        sb.append("§7Alive: §f").append(isAlive()).append("\n");
        sb.append("§7HP: §f").append(String.format("%.0f / %.0f (%.1f%%)",
                getCurrentHealth(), maxHealth, getHealthPercent())).append("\n");
        sb.append("§7Location: §f").append(String.format("%.1f, %.1f, %.1f",
                getLocation().getX(), getLocation().getY(), getLocation().getZ())).append("\n");
        sb.append("§7Flight Phase: §f").append(inFlightPhase).append("\n");
        sb.append("§7Flight State: §f").append(flightGoal.getState()).append("\n");
        sb.append("§7Tracked Players: §f").append(getTrackedPlayerCount()).append("\n");
        sb.append("§7Detection Range: §f").append(detectionRange).append("\n");
        sb.append("§7Ground Speed: §f").append(groundSpeed).append("\n");
        sb.append("§7Fly Speed: §f").append(flySpeed).append("\n");
        sb.append("§7Attack Damage: §f").append(attackDamage).append("\n");
        sb.append("§7Attack Interval: §f").append(attackInterval).append("t\n");

        var target = nmsMob.getTarget();
        if (target != null) {
            sb.append("§7Primary Target: §f").append(target.getName().getString()).append("\n");
        } else {
            sb.append("§7Primary Target: §fnone\n");
        }

        return sb.toString();
    }
}
