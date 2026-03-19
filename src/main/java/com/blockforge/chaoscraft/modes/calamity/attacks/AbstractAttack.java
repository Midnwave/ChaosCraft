package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityMode;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Base class for all Calamity attacks (block displays, environmental, boss).
 *
 * Design rules enforced here:
 * - NO status effects (no blindness, nausea, etc.)
 * - Always spawn straight (fixed yaw/pitch, never based on player facing)
 * - Configurable: damage, radius, tick interval, cooldown, duration, chance, enabled
 * - Exempt player check built in
 * - Max active events per player respected
 * - Damage on impact only for falling/meteor attacks (special case)
 * - Regular displays: continuous damage radius with tick interval
 * - Only ~5% of attacks track players
 *
 * Subclasses implement:
 * - onSpawn(Location) — create visual entities, particles, sounds
 * - onTick(int ticksAlive) — animate, move, update particles
 * - onCleanup() — remove all entities
 * - onImpact(Location) — for impact-only damage attacks
 */
public abstract class AbstractAttack {

    protected final ChaosCraftPlugin plugin;
    protected final AttackConfig config;

    // Runtime state
    private boolean active = false;
    private int ticksAlive = 0;
    private int damageCooldownTicks = 0;
    private Location center;
    private Player targetPlayer; // Only set for tracking attacks (~5%)
    private boolean impactTriggered = false;

    // Track spawned display entities for cleanup
    protected final List<Entity> spawnedEntities = new ArrayList<>();

    protected AbstractAttack(ChaosCraftPlugin plugin, AttackConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ========================
    // Lifecycle (called by AttackScheduler)
    // ========================

    /**
     * Spawn this attack at the given location.
     * Location is always straight (yaw=0, pitch=0) regardless of player facing.
     *
     * @param location The center point to spawn at (near a player typically)
     * @param target   The target player (for tracking attacks, null for non-tracking)
     */
    public final void spawn(Location location, Player target) {
        // Always spawn straight — normalize yaw/pitch
        this.center = location.clone();
        this.center.setYaw(0);
        this.center.setPitch(0);
        this.targetPlayer = target;
        this.active = true;
        this.ticksAlive = 0;
        this.damageCooldownTicks = 0;
        this.impactTriggered = false;

        onSpawn(this.center);
    }

    /**
     * Tick this attack. Called every server tick while active.
     * Handles duration expiry, damage application, and subclass animation.
     */
    public final void tick() {
        if (!active) return;

        ticksAlive++;

        // Check duration expiry
        if (ticksAlive >= config.getDurationTicks()) {
            cleanup();
            return;
        }

        // Update center if tracking player
        if (config.tracksPlayer() && targetPlayer != null && targetPlayer.isOnline()) {
            Location playerLoc = targetPlayer.getLocation().clone();
            playerLoc.setYaw(0);
            playerLoc.setPitch(0);
            this.center = playerLoc;
        }

        // Subclass animation
        onTick(ticksAlive);

        // Apply damage (respects damage-delay-ticks — no damage until delay expires)
        if (!config.isDamageOnImpactOnly() && ticksAlive >= config.getDamageDelayTicks()) {
            applyRadiusDamage();
        }

        // Debug: show damage radius outline every 3 ticks
        if (plugin.getConfig().getBoolean("debug", false) && ticksAlive % 3 == 0) {
            renderDebugRadius();
        }
    }

    /**
     * Clean up this attack — remove all entities, stop effects.
     */
    public final void cleanup() {
        if (!active) return;
        active = false;

        onCleanup();

        // Remove all spawned entities
        for (Entity entity : spawnedEntities) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        spawnedEntities.clear();
    }

    // ========================
    // Damage system
    // ========================

    /**
     * Apply continuous radius damage (for non-impact attacks).
     * Respects tick interval between damage, exempt players.
     */
    private void applyRadiusDamage() {
        if (center == null || center.getWorld() == null) return;

        damageCooldownTicks++;
        if (damageCooldownTicks < config.getTicksBetweenDamage()) return;
        damageCooldownTicks = 0;

        double radius = config.getDamageRadius();
        double damage = config.getDamage();
        if (damage <= 0 || radius <= 0) return;

        World world = center.getWorld();
        for (Player player : world.getPlayers()) {
            if (isExempt(player)) continue;
            if (player.getLocation().distanceSquared(center) <= radius * radius) {
                player.damage(damage);
            }
        }
    }

    // ========================
    // Debug radius rendering
    // ========================

    /**
     * Renders a particle circle outline at the attack center showing the damage radius.
     * BLUE = continuous damage radius, CYAN = impact damage radius.
     * Only shown when debug: true in config.yml.
     */
    private void renderDebugRadius() {
        if (center == null || center.getWorld() == null) return;
        World world = center.getWorld();

        // Continuous damage radius (blue circle)
        double radius = config.getDamageRadius();
        if (radius > 0 && config.getDamage() > 0) {
            renderCircle(world, center, radius, Color.fromRGB(50, 100, 255), 1.0f);
        }

        // Impact damage radius (cyan circle, slightly above)
        if (config.isDamageOnImpactOnly()) {
            double impactRadius = config.getImpactRadius();
            if (impactRadius > 0 && config.getImpactDamage() > 0) {
                Location raised = center.clone().add(0, 0.1, 0);
                renderCircle(world, raised, impactRadius, Color.fromRGB(50, 200, 255), 1.2f);
            }
        }
    }

    /**
     * Renders a circle of dust particles at the given location and radius.
     */
    private void renderCircle(World world, Location loc, double radius, Color color, float size) {
        int points = Math.max(12, (int) (radius * 8)); // More points for larger circles
        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = loc.getX() + Math.cos(angle) * radius;
            double z = loc.getZ() + Math.sin(angle) * radius;
            world.spawnParticle(Particle.DUST, x, loc.getY() + 0.1, z, 1, 0, 0, 0, 0, dust);
        }
    }

    /**
     * Trigger impact damage (for meteor/falling attacks).
     * Call this from onTick() when the attack hits the ground.
     */
    protected void triggerImpactDamage(Location impactLocation) {
        if (impactTriggered) return;
        impactTriggered = true;

        double radius = config.getImpactRadius();
        double damage = config.getImpactDamage();
        if (damage <= 0 || radius <= 0) return;

        World world = impactLocation.getWorld();
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            if (isExempt(player)) continue;
            if (player.getLocation().distanceSquared(impactLocation) <= radius * radius) {
                player.damage(damage);
            }
        }

        onImpact(impactLocation);
    }

    /**
     * Check if a player is exempt from this attack's damage.
     */
    protected boolean isExempt(Player player) {
        // Check mode-level exempt list
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive()) {
            var activeMode = modeManager.getActiveMode();
            if (activeMode.isExempt(player)) return true;
        }
        // Permission-based exempt
        return player.hasPermission("chaoscraft.mode.exempt");
    }

    // ========================
    // Abstract methods for subclasses
    // ========================

    /**
     * Create visual entities, particles, initial sounds.
     * Always receives a location with yaw=0, pitch=0 (straight spawn).
     */
    protected abstract void onSpawn(Location center);

    /**
     * Animate, move, update particles each tick.
     * @param ticksAlive How many ticks since spawn
     */
    protected abstract void onTick(int ticksAlive);

    /**
     * Remove all custom visual effects (entities handled automatically).
     */
    protected abstract void onCleanup();

    /**
     * Called when an impact-only attack hits the ground.
     * Override to add impact particles/sounds.
     */
    protected void onImpact(Location impactLocation) {
        // Default: no-op. Override for impact effects.
    }

    // ========================
    // State access
    // ========================

    public boolean isActive() { return active; }
    public int getTicksAlive() { return ticksAlive; }
    public Location getCenter() { return center; }
    public Player getTargetPlayer() { return targetPlayer; }
    public AttackConfig getConfig() { return config; }

    public String getId() { return config.getAttackId(); }
    public AttackType getType() { return config.getType(); }
    public int getPhase() { return config.getPhase(); }

    /**
     * Create a new instance of this attack for spawning.
     * Each spawn needs a fresh instance since attacks track state.
     */
    public abstract AbstractAttack newInstance();

    /**
     * Update center location (for external movement control).
     */
    protected void setCenter(Location center) {
        this.center = center;
    }
}
