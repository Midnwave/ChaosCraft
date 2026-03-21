package com.blockforge.chaoscraft.modes.seer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the Seer boss — a giant flying eyeball with a devastating beam attack.
 * The boss hovers above players, tracks them with smooth AI movement, and fires
 * a continuous purple beam that damages all players in its path.
 *
 * The boss cannot be killed directly. Players must destroy 10 crying obsidian orbs
 * around the arena. Each orb destroyed reduces the boss's max HP by 10M.
 * When all orbs are destroyed (max HP reaches 0), the boss dies instantly.
 *
 * MythicMobs integration uses reflection to avoid a hard dependency.
 * If MythicMobs is unavailable, an invisible Zombie with glowing is used as fallback.
 */
public class SeerBossManager {

    private final ChaosCraftPlugin plugin;
    private final SeerConfig config;

    private Entity bossEntity;
    private UUID bossUUID;
    private boolean bossAlive = false;
    private int orbsRemaining = 10;
    private Player primaryTarget;
    private boolean beamActive = false;
    private boolean beamCharging = false;
    private int beamChargeTick = 0;
    private Vector currentVelocity = new Vector(0, 0, 0);
    private SeerOrbManager orbManager; // set after construction

    public SeerBossManager(ChaosCraftPlugin plugin, SeerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ========================================================================
    // Spawning
    // ========================================================================

    /**
     * Spawns the Seer boss in the given world.
     * Tries MythicMobs via reflection first, falls back to an invisible Zombie.
     */
    public void spawnBoss(World world) {
        if (bossAlive) return;

        // Find center of all online players in this world
        Location center = findPlayerCenter(world);
        if (center == null) {
            center = world.getSpawnLocation();
        }
        center = center.clone().add(0, config.getBossFloatHeight(), 0);

        Entity spawned = trySpawnMythicMob(config.getBossMythicMobId(), center);

        if (spawned == null) {
            // Fallback: spawn invisible Zombie
            spawned = spawnFallbackEntity(center);
        }

        bossEntity = spawned;
        bossUUID = spawned.getUniqueId();
        bossAlive = true;
        orbsRemaining = config.getOrbCount();
        primaryTarget = null;
        beamActive = false;
        beamCharging = false;
        beamChargeTick = 0;
        currentVelocity = new Vector(0, 0, 0);

        // Set max health to orbsRemaining * healthPerOrb
        long totalHealth = orbsRemaining * config.getOrbHealthPerOrb();
        if (bossEntity instanceof LivingEntity living) {
            var attr = living.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(totalHealth);
                living.setHealth(totalHealth);
            }
        }

        // Force-load chunks around boss
        forceLoadChunksAround(bossEntity.getLocation(), 3);

        // Spawn effects
        world.playSound(center, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.3f);
        world.spawnParticle(Particle.END_ROD, center, 200, 5, 5, 5, 0.1);
        world.spawnParticle(Particle.DUST, center, 150, 8, 3, 8, 0.05,
                new Particle.DustOptions(Color.fromRGB(160, 0, 200), 2.5f));

        // Broadcast
        for (Player p : world.getPlayers()) {
            p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "The Seer awakens...");
        }

        plugin.getLogger().info("[Seer] Boss spawned at " + formatLoc(center)
                + " (HP: " + totalHealth + ", orbs: " + orbsRemaining + ")");
    }

    /**
     * Attempt to spawn a MythicMobs mob via reflection.
     * Returns the spawned Entity, or null if MythicMobs is not available.
     */
    private Entity trySpawnMythicMob(String mythicId, Location location) {
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            Object mythicBukkit = instMethod.invoke(null);

            Method getMobManager = mythicBukkit.getClass().getMethod("getMobManager");
            Object mobManager = getMobManager.invoke(mythicBukkit);

            Method spawnMob = mobManager.getClass().getMethod("spawnMob", String.class, Location.class);
            Object activeMob = spawnMob.invoke(mobManager, mythicId, location);

            if (activeMob == null) {
                plugin.getLogger().warning("[Seer] MythicMobs returned null for mob ID: " + mythicId);
                return null;
            }

            Method getEntity = activeMob.getClass().getMethod("getEntity");
            Object abstractEntity = getEntity.invoke(activeMob);

            Method getBukkitEntity = abstractEntity.getClass().getMethod("getBukkitEntity");
            Object bukkitEntity = getBukkitEntity.invoke(abstractEntity);

            if (bukkitEntity instanceof Entity entity) {
                plugin.getLogger().info("[Seer] MythicMobs boss spawned: " + mythicId);
                return entity;
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[Seer] MythicMobs not found, using fallback entity.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Seer] Failed to spawn MythicMobs boss, using fallback.", e);
        }
        return null;
    }

    /**
     * Fallback boss: an invisible, glowing Zombie with no AI and custom name.
     */
    private Entity spawnFallbackEntity(Location location) {
        Zombie zombie = location.getWorld().spawn(location, Zombie.class, z -> {
            z.setGlowing(true);
            z.setInvisible(true);
            z.setAI(false);
            z.setSilent(true);
            z.setPersistent(true);
            z.customName(net.kyori.adventure.text.Component.text("Seer")
                    .color(net.kyori.adventure.text.format.TextColor.color(0xAA00FF)));
            z.setCustomNameVisible(true);
        });

        plugin.getLogger().info("[Seer] Fallback Zombie boss spawned.");
        return zombie;
    }

    // ========================================================================
    // Tick loop
    // ========================================================================

    /**
     * Called every tick by the mode while active.
     */
    public void tick(World world) {
        if (!bossAlive) return;

        // Validate boss entity still exists
        if (bossEntity == null || bossEntity.isDead() || !bossEntity.isValid()) {
            onBossDied();
            return;
        }

        // Re-acquire entity by UUID if reference is stale
        if (!bossEntity.getWorld().equals(world)) {
            Entity found = Bukkit.getEntity(bossUUID);
            if (found == null || found.isDead()) {
                onBossDied();
                return;
            }
            bossEntity = found;
        }

        // 1. AI movement
        tickAI(world);

        // 2. Beam logic
        tickBeam(world);

        // 3. Phase ambient particles
        tickAmbient();

        // 4. Keep chunks loaded (every 20 ticks)
        if (world.getGameTime() % 20 == 0) {
            forceLoadChunksAround(bossEntity.getLocation(), 3);
        }
    }

    // ========================================================================
    // AI Movement
    // ========================================================================

    /**
     * Smooth flying AI: finds nearest player, hovers above them, lerps velocity.
     */
    private void tickAI(World world) {
        // Find all players within detection range
        List<Player> detected = findPlayersInRange(world, config.getBossDetectionRange());
        if (detected.isEmpty()) return;

        // Select/update primary target (nearest, 20% swap chance if multiple nearby)
        updateTarget(detected);

        if (primaryTarget == null) return;

        // Calculate desired position — offset to the side so it looks down at an angle
        // Use the boss's current orbit angle for a dynamic offset
        double offsetDist = 8.0; // blocks to the side
        double orbitAngle = (System.currentTimeMillis() * 0.0005) % (Math.PI * 2); // slow orbit
        double offsetX = Math.cos(orbitAngle) * offsetDist;
        double offsetZ = Math.sin(orbitAngle) * offsetDist;
        Location desired = primaryTarget.getLocation().clone()
                .add(offsetX, config.getBossFloatHeight(), offsetZ);

        // Obstacle avoidance
        desired = avoidObstacles(bossEntity.getLocation(), desired);

        // Smooth movement (lerp velocity)
        Vector toDesired = desired.toVector().subtract(bossEntity.getLocation().toVector());
        double dist = toDesired.length();
        if (dist > 0.5) {
            Vector desiredVel = toDesired.normalize().multiply(Math.min(config.getBossMoveSpeed(), dist * 0.1));
            currentVelocity = currentVelocity.multiply(0.85).add(desiredVel.multiply(0.15)); // smooth lerp
        }

        // Apply movement
        Location newLoc = bossEntity.getLocation().add(currentVelocity);
        // Face the target — set both yaw and pitch so the entity looks down at player
        Vector lookDir = primaryTarget.getLocation().add(0, 1, 0).toVector().subtract(newLoc.toVector());
        if (lookDir.lengthSquared() > 0.01) {
            newLoc.setDirection(lookDir);
            // Also explicitly set pitch to look down (negative = down in MC)
            double horizontalDist = Math.sqrt(lookDir.getX() * lookDir.getX() + lookDir.getZ() * lookDir.getZ());
            float pitch = (float) -Math.toDegrees(Math.atan2(lookDir.getY(), horizontalDist));
            newLoc.setPitch(pitch);
        }
        bossEntity.teleport(newLoc);
    }

    /**
     * Find all players within a given range of the boss.
     */
    private List<Player> findPlayersInRange(World world, double range) {
        List<Player> result = new ArrayList<>();
        if (bossEntity == null) return result;
        Location bossLoc = bossEntity.getLocation();
        double rangeSq = range * range;
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL && !p.isInvulnerable()) {
                if (p.getLocation().distanceSquared(bossLoc) <= rangeSq) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    /**
     * Select or update the primary target. Picks nearest player,
     * with a 20% chance to swap if multiple are nearby.
     */
    private void updateTarget(List<Player> detected) {
        if (detected.isEmpty()) {
            primaryTarget = null;
            return;
        }

        // If current target is invalid, force re-target
        if (primaryTarget == null || !primaryTarget.isOnline()
                || primaryTarget.isDead() || !detected.contains(primaryTarget)) {
            primaryTarget = findNearest(detected);
            return;
        }

        // 20% swap chance when multiple players are nearby
        if (detected.size() > 1 && Math.random() < 0.20) {
            Player nearest = findNearest(detected);
            if (nearest != null && !nearest.equals(primaryTarget)) {
                primaryTarget = nearest;
            }
        }
    }

    /**
     * Find the nearest player in a list to the boss.
     */
    private Player findNearest(List<Player> players) {
        if (bossEntity == null || players.isEmpty()) return null;
        Location bossLoc = bossEntity.getLocation();
        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Player p : players) {
            double distSq = p.getLocation().distanceSquared(bossLoc);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = p;
            }
        }
        return nearest;
    }

    /**
     * Obstacle avoidance: raycast in movement direction, adjust if solid block ahead.
     */
    private Location avoidObstacles(Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector());
        if (dir.lengthSquared() < 0.01) return to;

        Vector normDir = dir.clone().normalize();
        World world = from.getWorld();

        // Raycast 3 blocks ahead
        RayTraceResult ray = world.rayTraceBlocks(from, normDir, 3.0);
        if (ray == null || ray.getHitBlock() == null) {
            return to; // No obstacle
        }

        // Obstacle found — try adjustments
        // Try Y+3 first
        Location adjusted = to.clone().add(0, 3, 0);
        RayTraceResult upRay = world.rayTraceBlocks(from, adjusted.toVector().subtract(from.toVector()).normalize(), 3.0);
        if (upRay == null || upRay.getHitBlock() == null) {
            return adjusted;
        }

        // Try +X
        adjusted = to.clone().add(3, 0, 0);
        RayTraceResult xRay = world.rayTraceBlocks(from, adjusted.toVector().subtract(from.toVector()).normalize(), 3.0);
        if (xRay == null || xRay.getHitBlock() == null) {
            return adjusted;
        }

        // Try -X
        adjusted = to.clone().add(-3, 0, 0);
        RayTraceResult negXRay = world.rayTraceBlocks(from, adjusted.toVector().subtract(from.toVector()).normalize(), 3.0);
        if (negXRay == null || negXRay.getHitBlock() == null) {
            return adjusted;
        }

        // Try +Z
        adjusted = to.clone().add(0, 0, 3);
        RayTraceResult zRay = world.rayTraceBlocks(from, adjusted.toVector().subtract(from.toVector()).normalize(), 3.0);
        if (zRay == null || zRay.getHitBlock() == null) {
            return adjusted;
        }

        // Try -Z
        adjusted = to.clone().add(0, 0, -3);
        RayTraceResult negZRay = world.rayTraceBlocks(from, adjusted.toVector().subtract(from.toVector()).normalize(), 3.0);
        if (negZRay == null || negZRay.getHitBlock() == null) {
            return adjusted;
        }

        // All blocked — return original and hope for the best
        return to;
    }

    // ========================================================================
    // Beam Logic
    // ========================================================================

    /**
     * Tick the beam: charge, fire, and damage cycle.
     */
    private void tickBeam(World world) {
        if (primaryTarget == null || !primaryTarget.isOnline()) {
            stopBeam();
            return;
        }

        double distToTarget = bossEntity.getLocation().distance(primaryTarget.getLocation());

        // Power down if too far
        if (distToTarget > config.getBossBeamRange()) {
            stopBeam();
            return;
        }

        if (!beamCharging && !beamActive) {
            // Start charging
            beamCharging = true;
            beamChargeTick = 0;
            playAnimation("beam_charge");
        }

        if (beamCharging) {
            beamChargeTick++;
            // Charge particles (intensify over time)
            Location eyeLoc = bossEntity.getLocation();
            int particleCount = beamChargeTick / 5;
            DisplayBuilder.dustParticles(eyeLoc, particleCount, 2.0, 160, 0, 200, 1.5f);

            if (beamChargeTick >= config.getBossBeamChargeTicks()) {
                beamCharging = false;
                beamActive = true;
                playAnimation("beam_fire");
            }
            return;
        }

        if (beamActive) {
            // Beam visual: particle line from boss to target
            Location from = bossEntity.getLocation();
            Location to = primaryTarget.getLocation().add(0, 1, 0);

            // Dense purple particle beam
            DisplayBuilder.particleLine(from, to, Particle.DUST, 3,
                    new Particle.DustOptions(Color.fromRGB(160, 0, 200), 2.0f));
            DisplayBuilder.particleLine(from, to, Particle.END_ROD, 2, null);

            // Spiral particles around beam axis
            Vector beamDir = to.toVector().subtract(from.toVector());
            double beamLen = beamDir.length();
            if (beamLen > 0) {
                Vector norm = beamDir.normalize();
                for (double d = 0; d < beamLen; d += 1.0) {
                    double spiralAngle = d * 0.5 + System.currentTimeMillis() * 0.005;
                    // perpendicular offset using spiral
                    Location spiralLoc = from.clone().add(norm.clone().multiply(d));
                    spiralLoc.add(Math.cos(spiralAngle) * 0.8, Math.sin(spiralAngle) * 0.8, 0);
                    world.spawnParticle(Particle.DUST, spiralLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 0, 180), 1.0f));
                }
            }

            // Ground impact particles
            world.spawnParticle(Particle.DUST, to, 10, 1, 0.5, 1, 0.01,
                    new Particle.DustOptions(Color.fromRGB(120, 0, 160), 1.5f));

            // Damage ALL players in beam path (within 2 blocks of beam line)
            double dmgPerTick = config.getBossBeamDamagePerTick();
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                if (distanceToLine(p.getLocation().add(0, 1, 0), from, to) <= 2.0) {
                    p.damage(dmgPerTick);
                    p.setNoDamageTicks(0);
                }
            }

            // Beam sound every 10 ticks
            if (world.getGameTime() % 10 == 0) {
                world.playSound(from, Sound.ENTITY_GUARDIAN_ATTACK, SoundCategory.HOSTILE, 1.5f, 0.3f);
            }
        }
    }

    /**
     * Standard point-to-line-segment distance using cross product.
     */
    private double distanceToLine(Location point, Location lineStart, Location lineEnd) {
        Vector p = point.toVector();
        Vector a = lineStart.toVector();
        Vector b = lineEnd.toVector();

        Vector ab = b.clone().subtract(a);
        Vector ap = p.clone().subtract(a);

        double abLenSq = ab.lengthSquared();
        if (abLenSq < 0.0001) {
            return ap.length();
        }

        // Project point onto line, clamped to segment
        double t = ap.dot(ab) / abLenSq;
        t = Math.max(0, Math.min(1, t));

        // Closest point on segment
        Vector closest = a.clone().add(ab.clone().multiply(t));
        return p.distance(closest);
    }

    /**
     * Stop the beam and reset charge state.
     */
    public void stopBeam() {
        if (beamActive) {
            playAnimation("beam_stop");
        }
        beamActive = false;
        beamCharging = false;
        beamChargeTick = 0;
    }

    /**
     * Force-start the beam (admin).
     */
    public void startBeam() {
        if (!bossAlive || primaryTarget == null) return;
        beamCharging = true;
        beamChargeTick = 0;
    }

    // ========================================================================
    // Ambient effects
    // ========================================================================

    /**
     * Phase ambient particles based on orbs remaining.
     */
    private void tickAmbient() {
        if (bossEntity == null || !bossAlive) return;
        Location loc = bossEntity.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Purple aura particles
        if (world.getGameTime() % 3 == 0) {
            world.spawnParticle(Particle.DUST, loc, 5, 2, 2, 2, 0.01,
                    new Particle.DustOptions(Color.fromRGB(160, 0, 200), 1.5f));
        }

        // Intensify as orbs are destroyed (fewer remaining = more particles)
        int destroyed = config.getOrbCount() - orbsRemaining;
        if (destroyed > 0 && world.getGameTime() % 5 == 0) {
            int extraParticles = destroyed * 2;
            world.spawnParticle(Particle.DUST, loc, extraParticles, 3, 3, 3, 0.02,
                    new Particle.DustOptions(Color.fromRGB(200, 0, 180), 2.0f));
            // End rod sparkle when weakened
            if (destroyed >= 5) {
                world.spawnParticle(Particle.END_ROD, loc, destroyed, 2, 2, 2, 0.05);
            }
        }
    }

    // ========================================================================
    // Orb destruction callback
    // ========================================================================

    /**
     * Called by SeerOrbManager when an orb is destroyed.
     * Reduces orbsRemaining and updates boss max health.
     */
    public void updateMaxHealth(long newMaxHealth) {
        orbsRemaining--;
        if (orbsRemaining < 0) orbsRemaining = 0;

        if (bossEntity instanceof LivingEntity living) {
            var attr = living.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                double currentHealth = living.getHealth();
                attr.setBaseValue(Math.max(1, newMaxHealth));
                living.setHealth(Math.min(currentHealth, Math.max(1, newMaxHealth)));
            }
        }

        plugin.getLogger().info("[Seer] Orb destroyed! Orbs remaining: " + orbsRemaining
                + ", new boss max HP: " + newMaxHealth);

        // Debug-only broadcast
        if (plugin.getConfig().getBoolean("debug", false) && bossEntity != null && bossEntity.getWorld() != null) {
            for (Player p : bossEntity.getWorld().getPlayers()) {
                p.sendMessage(ChatColor.LIGHT_PURPLE + "An orb shatters! The Seer weakens... ("
                        + orbsRemaining + " orbs remain)");
            }
        }

        // Boss screams in pain
        if (bossEntity != null) {
            World world = bossEntity.getWorld();
            Location loc = bossEntity.getLocation();
            world.playSound(loc, Sound.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 2.0f, 0.5f);
            world.spawnParticle(Particle.DUST, loc, 50, 3, 3, 3, 0.1,
                    new Particle.DustOptions(Color.fromRGB(200, 0, 180), 2.5f));
        }

        if (newMaxHealth <= 0) {
            onAllOrbsDestroyed();
        }
    }

    /**
     * All orbs destroyed — the Seer dies instantly.
     */
    private void onAllOrbsDestroyed() {
        if (!bossAlive) return;
        bossAlive = false;

        plugin.getLogger().info("[Seer] All orbs destroyed! The Seer has been vanquished!");

        Location deathLoc = (bossEntity != null && bossEntity.isValid())
                ? bossEntity.getLocation()
                : null;

        if (deathLoc != null) {
            World world = deathLoc.getWorld();

            // Death sounds
            world.playSound(deathLoc, Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 3.0f, 0.4f);
            world.playSound(deathLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.5f, 0.5f);
            world.playSound(deathLoc, Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 2.0f, 0.3f);

            // Massive particle explosion
            world.spawnParticle(Particle.END_ROD, deathLoc, 500, 10, 10, 10, 0.3);
            world.spawnParticle(Particle.FLASH, deathLoc, 5, 0, 0, 0, 0);
            world.spawnParticle(Particle.DUST, deathLoc, 300, 15, 10, 15, 0.1,
                    new Particle.DustOptions(Color.fromRGB(170, 0, 255), 3.0f));
            world.spawnParticle(Particle.DUST, deathLoc, 200, 12, 8, 12, 0.05,
                    new Particle.DustOptions(Color.fromRGB(200, 0, 180), 2.5f));
            world.spawnParticle(Particle.DRAGON_BREATH, deathLoc, 100, 8, 5, 8, 0.08);

            // Broadcast
            for (Player p : world.getPlayers()) {
                p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "The Seer has been vanquished!");
            }
        }

        // Stop beam
        stopBeam();

        // Remove entity
        if (bossEntity != null && bossEntity.isValid() && !bossEntity.isDead()) {
            bossEntity.remove();
        }
    }

    /**
     * Called when the boss entity dies unexpectedly (external kill).
     */
    private void onBossDied() {
        if (!bossAlive) return;
        bossAlive = false;
        stopBeam();

        plugin.getLogger().info("[Seer] Boss entity died unexpectedly.");

        if (bossEntity != null) {
            Location loc = bossEntity.getLocation();
            World world = loc.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.DUST, loc, 100, 5, 5, 5, 0.05,
                        new Particle.DustOptions(Color.fromRGB(160, 0, 200), 2.0f));
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "The Seer has been vanquished!");
                }
            }
        }
    }

    // ========================================================================
    // ModelEngine Animation
    // ========================================================================

    /**
     * Play a ModelEngine animation on the boss entity via reflection.
     * Silently fails if ModelEngine is not available.
     */
    private void playAnimation(String name) {
        if (bossEntity == null) return;
        try {
            // Use Entity-based lookup (not UUID) — matches the working pattern from ModelEngineAttack
            Class<?> modelEngineAPI = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");

            // Try getModeledEntity(Entity) first
            Method getModeledEntity;
            Object modeled;
            try {
                getModeledEntity = modelEngineAPI.getMethod("getModeledEntity", org.bukkit.entity.Entity.class);
                modeled = getModeledEntity.invoke(null, bossEntity);
            } catch (NoSuchMethodException e) {
                // Fallback to UUID-based lookup
                getModeledEntity = modelEngineAPI.getMethod("getModeledEntity", UUID.class);
                modeled = getModeledEntity.invoke(null, bossUUID);
            }
            if (modeled == null) {
                plugin.debug("[Seer] ModeledEntity not found for boss");
                return;
            }

            Method getModels = modeled.getClass().getMethod("getModels");
            @SuppressWarnings("unchecked")
            Map<String, Object> models = (Map<String, Object>) getModels.invoke(modeled);
            if (models == null || models.isEmpty()) {
                plugin.debug("[Seer] No models found on boss entity");
                return;
            }

            Object activeModel = models.values().iterator().next();
            Method getAnimationHandler = activeModel.getClass().getMethod("getAnimationHandler");
            Object animHandler = getAnimationHandler.invoke(activeModel);

            // Force = true to override any current animation
            Method playAnimation = animHandler.getClass().getMethod("playAnimation",
                    String.class, double.class, double.class, double.class, boolean.class);
            playAnimation.invoke(animHandler, name, 0.0, 0.0, 1.0, true);

            plugin.debug("[Seer] Played animation: " + name);
        } catch (ClassNotFoundException ignored) {
            // ModelEngine not installed
        } catch (Exception e) {
            plugin.getLogger().warning("[Seer] Failed to play animation '" + name + "': " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ========================================================================
    // Cleanup
    // ========================================================================

    /**
     * Full cleanup — remove boss entity, clear displays, reset all state.
     */
    public void cleanup() {
        stopBeam();

        if (bossEntity != null) {
            // Unforce chunks
            unforceChunksAround(bossEntity.getLocation(), 3);

            if (bossEntity.isValid() && !bossEntity.isDead()) {
                bossEntity.remove();
            }
        }

        bossEntity = null;
        bossUUID = null;
        bossAlive = false;
        orbsRemaining = 10;
        primaryTarget = null;
        beamActive = false;
        beamCharging = false;
        beamChargeTick = 0;
        currentVelocity = new Vector(0, 0, 0);
    }

    /**
     * Force-kill the boss (admin command).
     */
    public void forceKill() {
        if (!bossAlive) return;
        if (bossEntity instanceof LivingEntity living) {
            living.setHealth(0);
        } else if (bossEntity != null) {
            bossEntity.remove();
        }
        onBossDied();
    }

    // ========================================================================
    // Chunk management
    // ========================================================================

    private void forceLoadChunksAround(Location center, int radius) {
        if (center == null || center.getWorld() == null) return;
        World world = center.getWorld();
        int cx = center.getBlockX() >> 4;
        int cz = center.getBlockZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.setChunkForceLoaded(cx + dx, cz + dz, true);
            }
        }
    }

    private void unforceChunksAround(Location center, int radius) {
        if (center == null || center.getWorld() == null) return;
        World world = center.getWorld();
        int cx = center.getBlockX() >> 4;
        int cz = center.getBlockZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.setChunkForceLoaded(cx + dx, cz + dz, false);
            }
        }
    }

    // ========================================================================
    // Getters
    // ========================================================================

    public boolean isBossAlive() { return bossAlive; }
    public boolean isBeamActive() { return beamActive; }
    public Entity getBossEntity() { return bossEntity; }
    public int getOrbsRemaining() { return orbsRemaining; }

    public void forceSpawn(World world) {
        if (bossAlive) cleanup();
        spawnBoss(world);
    }

    public void forceBeam() {
        if (!bossAlive || beamActive || beamCharging) return;
        beamCharging = true;
        beamChargeTick = 0;
    }

    public void setOrbManager(SeerOrbManager orbManager) {
        this.orbManager = orbManager;
    }

    // ========================================================================
    // Utility
    // ========================================================================

    private Location findPlayerCenter(World world) {
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return null;
        double x = 0, y = 0, z = 0;
        for (Player p : players) {
            Location loc = p.getLocation();
            x += loc.getX();
            y += loc.getY();
            z += loc.getZ();
        }
        int count = players.size();
        return new Location(world, x / count, y / count, z / count);
    }

    private String formatLoc(Location loc) {
        return String.format("(%s, %.1f, %.1f, %.1f)",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}
