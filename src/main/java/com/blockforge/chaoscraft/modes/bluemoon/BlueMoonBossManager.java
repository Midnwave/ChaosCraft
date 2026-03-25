package com.blockforge.chaoscraft.modes.bluemoon;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the Blue Moon boss — a massive floating ModelEngine moon entity
 * backed by MythicMobs. The boss always floats above players, orbits the
 * battlefield, has 4 HP-threshold phases, and fires a signature Lunar Super Laser.
 *
 * MythicMobs integration uses reflection to avoid a hard dependency.
 * If MythicMobs is unavailable, a resized glowing Phantom is used as fallback.
 */
public class BlueMoonBossManager {

    private final ChaosCraftPlugin plugin;
    private final BlueMoonConfig config;
    private Entity bossEntity;
    private UUID bossUUID;
    private int currentPhase = 1;
    private boolean bossAlive = false;
    private boolean laserActive = false;
    private int laserCooldown = 0;
    private int laserTick = 0;
    private int phase4EnrageTicks = 0;
    private double orbitAngle = 0;
    private long savedBossMaxHealth = 0;

    // Super laser state
    private Location laserTarget;
    private double laserSweepAngle = 0;
    private List<Entity> laserDisplays = new ArrayList<>();
    private Entity laserModelEntity = null; // ModelEngine laser model inside the boss

    // Display builder for laser beam visuals
    private final DisplayBuilder displayBuilder;

    // Callback for early kill rewards
    private Runnable earlyKillCallback;

    // Boss attack cycle — fires BOSS-type attacks from the registry
    private com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry attackRegistry;
    private int bossAttackCooldown = 0;

    public BlueMoonBossManager(ChaosCraftPlugin plugin, BlueMoonConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.displayBuilder = new DisplayBuilder(plugin);
    }

    /** Set the attack registry so the boss can fire BOSS-type attacks. */
    public void setAttackRegistry(com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry registry) {
        this.attackRegistry = registry;
    }

    // ========================================================================
    // Spawning
    // ========================================================================

    /**
     * Spawns the Blue Moon boss in the given world.
     * Tries MythicMobs via reflection first, falls back to a Phantom.
     */
    public void spawnBoss(World world) {
        if (bossAlive) return;

        // Pick spawn location near the densest cluster of players
        Location center = findDensestPlayerCluster(world);
        if (center == null) {
            center = world.getSpawnLocation();
        }
        center = center.clone().add(0, config.getBossFloatHeight(), 0);

        Entity spawned = trySpawnMythicMob(config.getBossMythicMobId(), center);

        if (spawned == null) {
            // Fallback: spawn a Phantom
            spawned = spawnFallbackPhantom(center);
        }

        bossEntity = spawned;
        bossUUID = spawned.getUniqueId();
        bossAlive = true;
        currentPhase = 1;
        phase4EnrageTicks = 0;
        orbitAngle = 0;
        laserCooldown = 0;
        laserActive = false;

        // Record max health for phase threshold calculations
        if (bossEntity instanceof LivingEntity living) {
            savedBossMaxHealth = (long) living.getMaxHealth();
        } else {
            savedBossMaxHealth = (long) config.getBossHealth();
        }

        // Spawn effects
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2.0f, 0.5f);
        world.spawnParticle(Particle.END_ROD, center, 200, 5, 5, 5, 0.1);
        world.spawnParticle(Particle.SNOWFLAKE, center, 150, 8, 3, 8, 0.05);

        plugin.getLogger().info("[BlueMoon] Boss spawned at " + formatLoc(center));
    }

    /**
     * Force-spawn the boss (admin command).
     */
    public void forceSpawn(World world) {
        if (bossAlive) {
            cleanup();
        }
        spawnBoss(world);
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

            // MythicMobs API: spawnMob(String mobType, Location location)
            Method spawnMob = mobManager.getClass().getMethod("spawnMob", String.class, Location.class);
            Object activeMob = spawnMob.invoke(mobManager, mythicId, location);

            if (activeMob == null) {
                plugin.getLogger().warning("[BlueMoon] MythicMobs returned null for mob ID: " + mythicId);
                return null;
            }

            // Get the Bukkit entity from the ActiveMob
            Method getEntity = activeMob.getClass().getMethod("getEntity");
            Object abstractEntity = getEntity.invoke(activeMob);

            Method getBukkitEntity = abstractEntity.getClass().getMethod("getBukkitEntity");
            Object bukkitEntity = getBukkitEntity.invoke(abstractEntity);

            if (bukkitEntity instanceof Entity entity) {
                plugin.getLogger().info("[BlueMoon] MythicMobs boss spawned: " + mythicId);
                return entity;
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[BlueMoon] MythicMobs not found, using fallback Phantom.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[BlueMoon] Failed to spawn MythicMobs boss, using fallback.", e);
        }
        return null;
    }

    /**
     * Fallback boss: a resized, glowing Phantom with custom name.
     */
    private Entity spawnFallbackPhantom(Location location) {
        Phantom phantom = location.getWorld().spawn(location, Phantom.class, p -> {
            p.setSize(20); // Large phantom
            p.setGlowing(true);
            p.customName(net.kyori.adventure.text.Component.text("Blue Moon")
                    .color(net.kyori.adventure.text.format.TextColor.color(0x88CCFF)));
            p.setCustomNameVisible(true);
            p.setAI(false); // We control movement manually
            p.setSilent(true);
            p.setPersistent(true);
        });

        // Set health via attribute
        if (phantom.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
            phantom.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)
                    .setBaseValue(config.getBossHealth());
            phantom.setHealth(config.getBossHealth());
        }

        plugin.getLogger().info("[BlueMoon] Fallback Phantom boss spawned.");
        return phantom;
    }

    // ========================================================================
    // Tick loop
    // ========================================================================

    /**
     * Called every tick by the mode scheduler while the mode is active.
     */
    public void tick(World world) {
        if (!bossAlive) return;

        // Validate boss entity still exists
        if (bossEntity == null || bossEntity.isDead() || !bossEntity.isValid()) {
            onBossDeath();
            return;
        }

        // Re-acquire entity by UUID if reference is stale
        if (!bossEntity.getWorld().equals(world)) {
            Entity found = Bukkit.getEntity(bossUUID);
            if (found == null || found.isDead()) {
                onBossDeath();
                return;
            }
            bossEntity = found;
        }

        // Update orbit position
        updateOrbit(world);

        // Check HP-based phase transitions
        checkPhaseTransition();

        // Tick laser cooldown
        if (laserCooldown > 0) laserCooldown--;

        // Tick super laser if active
        tickSuperLaser(world);

        // Phase-specific ambient effects
        tickPhaseAmbient(world);

        // Boss attack cycle — fire BOSS-type attacks at the boss's position
        tickBossAttacks(world);
    }

    // ========================================================================
    // Orbit movement
    // ========================================================================

    /**
     * Move the boss in a circular orbit around the nearest player.
     * Orbit speed and radius scale with phase.
     */
    private void updateOrbit(World world) {
        Player nearest = findNearestPlayer(world);

        double speedMultiplier = getPhaseSpeedMultiplier();
        orbitAngle += config.getBossOrbitSpeed() * speedMultiplier;
        if (orbitAngle > Math.PI * 2) orbitAngle -= Math.PI * 2;

        double radius = config.getBossOrbitRadius();
        double floatHeight = config.getBossFloatHeight();

        // Phase 4: boss descends
        if (currentPhase == 4) {
            floatHeight -= 10;
        }

        Location target;
        if (nearest != null) {
            double cx = nearest.getLocation().getX() + Math.cos(orbitAngle) * radius;
            double cz = nearest.getLocation().getZ() + Math.sin(orbitAngle) * radius;
            double cy = nearest.getLocation().getY() + floatHeight;

            // Phase 2+: Y oscillation (wobble)
            if (currentPhase >= 2) {
                cy += Math.sin(orbitAngle * 3) * 1.5;
            }

            target = new Location(world, cx, cy, cz);
        } else {
            // No players — hover at current position
            target = bossEntity.getLocation();
        }

        // Face the nearest player (or center)
        if (nearest != null) {
            Vector direction = nearest.getLocation().toVector().subtract(target.toVector());
            if (direction.lengthSquared() > 0.01) {
                target.setDirection(direction);
            }
        }

        bossEntity.teleport(target);
    }

    /**
     * Returns orbit speed multiplier for current phase.
     */
    private double getPhaseSpeedMultiplier() {
        return switch (currentPhase) {
            case 2 -> 1.5;
            case 3 -> 2.0;
            case 4 -> 2.5;
            default -> 1.0;
        };
    }

    // ========================================================================
    // Phase transitions
    // ========================================================================

    /**
     * Check current health ratio and trigger phase transitions at thresholds.
     */
    private void checkPhaseTransition() {
        double ratio = getHealthRatio();
        if (ratio <= config.getBossPhase4Threshold() && currentPhase < 4) {
            enterPhase(4);
        } else if (ratio <= config.getBossPhase3Threshold() && currentPhase < 3) {
            enterPhase(3);
        } else if (ratio <= config.getBossPhase2Threshold() && currentPhase < 2) {
            enterPhase(2);
        }
    }

    /**
     * Transition to a new phase with effects and optional super laser.
     */
    private void enterPhase(int phase) {
        int oldPhase = currentPhase;
        currentPhase = phase;
        plugin.getLogger().info("[BlueMoon] Boss phase transition: " + oldPhase + " -> " + phase);

        Location loc = bossEntity.getLocation();
        World world = loc.getWorld();

        // Phase transition sound
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2.0f, 1.5f);

        // Expanding particle ring
        for (double angle = 0; angle < Math.PI * 2; angle += 0.1) {
            double ringRadius = 8.0;
            double rx = loc.getX() + Math.cos(angle) * ringRadius;
            double rz = loc.getZ() + Math.sin(angle) * ringRadius;
            world.spawnParticle(Particle.END_ROD, rx, loc.getY(), rz, 3, 0.2, 0.2, 0.2, 0.02);
        }

        // Fire super laser on phase transition
        if (config.isSuperLaserEnabled()) {
            fireSuperLaser();
        }

        // Phase 4: start enrage timer
        if (phase == 4) {
            phase4EnrageTicks = 0;
        }
    }

    /**
     * Force a phase transition (admin command).
     */
    public void forcePhase(int phase) {
        if (!bossAlive || phase < 1 || phase > 4) return;
        enterPhase(phase);
    }

    // ========================================================================
    // Phase-specific ambient effects
    // ========================================================================

    private void tickPhaseAmbient(World world) {
        Location loc = bossEntity.getLocation();

        switch (currentPhase) {
            case 1 -> {
                // Slow orbit, gentle snowflake particles
                if (world.getGameTime() % 5 == 0) {
                    world.spawnParticle(Particle.SNOWFLAKE, loc, 5, 3, 2, 3, 0.01);
                }
            }
            case 2 -> {
                // Cyan dust particles
                if (world.getGameTime() % 3 == 0) {
                    world.spawnParticle(Particle.DUST,
                            loc, 8, 4, 2, 4, 0.01,
                            new Particle.DustOptions(Color.fromRGB(0, 200, 255), 1.5f));
                }
            }
            case 3 -> {
                // Particle cracks + frost storm
                if (world.getGameTime() % 2 == 0) {
                    world.spawnParticle(Particle.CRIT, loc, 10, 5, 3, 5, 0.1);
                    world.spawnParticle(Particle.SNOWFLAKE, loc, 12, 8, 5, 8, 0.05);
                }
                // Frost storm particles around players
                if (world.getGameTime() % 10 == 0) {
                    for (Player p : world.getPlayers()) {
                        world.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 3, 0),
                                8, 3, 2, 3, 0.02);
                    }
                }
            }
            case 4 -> {
                // Red-tinted crimson dust, descending
                world.spawnParticle(Particle.DUST,
                        loc, 15, 5, 3, 5, 0.02,
                        new Particle.DustOptions(Color.fromRGB(200, 50, 50), 2.0f));
                // Intense particle storm
                if (world.getGameTime() % 2 == 0) {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 8, 4, 2, 4, 0.03);
                }
            }
        }
    }

    // ========================================================================
    // Super Laser
    // ========================================================================

    /**
     * Boss attack cycle — periodically picks a BOSS-type attack from the registry
     * and spawns it at the boss's location, targeting a nearby player.
     * Attack frequency scales with phase: P1=every 6s, P2=5s, P3=4s, P4=3s.
     */
    private void tickBossAttacks(World world) {
        if (attackRegistry == null || !bossAlive || bossEntity == null) return;
        if (laserActive) return; // Don't attack during laser

        if (bossAttackCooldown > 0) {
            bossAttackCooldown--;
            return;
        }

        // Select a random BOSS-type attack
        var attack = attackRegistry.selectRandom(1, com.blockforge.chaoscraft.modes.calamity.attacks.AttackType.BOSS);
        if (attack == null) return;

        // Spawn at boss location, aimed at ground level below boss
        Location spawnLoc = bossEntity.getLocation().clone();
        // Find nearest player to use as target location
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : world.getPlayers()) {
            double d = p.getLocation().distanceSquared(spawnLoc);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = p;
            }
        }
        if (nearest != null) {
            spawnLoc = nearest.getLocation().clone();
        }
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        attack.spawn(spawnLoc, nearest);
        plugin.debug("[BlueMoon] Boss fired attack: " + attack.getId());

        // Cooldown scales with phase: P1=120t(6s), P2=100t(5s), P3=80t(4s), P4=60t(3s)
        bossAttackCooldown = switch (currentPhase) {
            case 1 -> 120;
            case 2 -> 100;
            case 3 -> 80;
            case 4 -> 60;
            default -> 120;
        };
    }

    /**
     * Fire the Lunar Super Laser. Picks a random player as the initial target
     * and begins the charge-fire-end sequence.
     */
    public void fireSuperLaser() {
        if (!config.isSuperLaserEnabled() || laserCooldown > 0 || laserActive) return;
        if (!bossAlive || bossEntity == null) return;

        World world = bossEntity.getWorld();
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        laserActive = true;
        laserTick = 0;
        laserCooldown = config.getSuperLaserCooldownTicks();
        laserSweepAngle = 0;

        // Pick random player as initial target
        laserTarget = players.get(new Random().nextInt(players.size())).getLocation();

        // Broadcast charge warning
        for (Player p : players) {
            p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "The Blue Moon is charging its laser...");
        }

        // Charge sound (configurable)
        String chargeSound = config.getLaserChargeSound();
        float chargeVol = config.getLaserChargeSoundVolume();
        float chargePitch = config.getLaserChargeSoundPitch();
        world.playSound(bossEntity.getLocation(), chargeSound, SoundCategory.HOSTILE, chargeVol, chargePitch);

        // Spawn ModelEngine laser model at boss location (inside the moon)
        spawnLaserModel(bossEntity.getLocation());
    }

    /**
     * Force-fire the super laser (admin command).
     */
    public void forceLaser() {
        laserCooldown = 0;
        fireSuperLaser();
    }

    /**
     * Tick the super laser through charge, fire, and end phases.
     */
    private void tickSuperLaser(World world) {
        if (!laserActive) return;
        laserTick++;

        int chargeTicks = config.getSuperLaserChargeTicks();
        int durationTicks = config.getSuperLaserDurationTicks();
        Location bossLoc = bossEntity.getLocation();

        if (laserTick <= chargeTicks) {
            // ── Charge phase ──────────────────────────────────────────────
            int particleCount = laserTick * 3;
            world.spawnParticle(Particle.END_ROD, bossLoc, particleCount,
                    2.0, 2.0, 2.0, 0.05);

            // Rising pitch sound every 10 ticks
            if (laserTick % 10 == 0) {
                float pitch = 0.5f + ((float) laserTick / chargeTicks) * 1.5f;
                String chargeSound = config.getLaserChargeSound();
                world.playSound(bossLoc, chargeSound, SoundCategory.HOSTILE,
                        config.getLaserChargeSoundVolume(), pitch);
            }

            // Move laser model to stay at boss position
            if (laserModelEntity != null && laserModelEntity.isValid()) {
                laserModelEntity.teleport(bossLoc.clone().add(0, -2, 0));
            }

        } else if (laserTick <= chargeTicks + durationTicks) {
            // ── Fire phase ────────────────────────────────────────────────
            // Trigger fire animation on first fire tick
            if (laserTick == chargeTicks + 1) {
                playLaserFireAnimation();
            }
            laserSweepAngle += 0.05;

            // Beam position: sweeping around the boss
            double beamX = bossLoc.getX() + Math.cos(laserSweepAngle) * 5.0;
            double beamZ = bossLoc.getZ() + Math.sin(laserSweepAngle) * 5.0;
            double beamTopY = bossLoc.getY();
            double beamBottomY = world.getHighestBlockYAt((int) beamX, (int) beamZ);

            // Remove old beam displays
            clearLaserDisplays();

            // Spawn beam column: SEA_LANTERN + DIAMOND_BLOCK display blocks
            double columnHeight = beamTopY - beamBottomY;
            int blockCount = Math.min(20, (int) (columnHeight / 2) + 1);
            for (int i = 0; i < blockCount; i++) {
                double y = beamBottomY + (columnHeight * i / blockCount);
                Location blockLoc = new Location(world, beamX, y, beamZ);
                Material mat = (i % 2 == 0) ? Material.SEA_LANTERN : Material.DIAMOND_BLOCK;
                BlockDisplayHandle handle = displayBuilder.spawnBlock(blockLoc, mat);
                if (handle != null && handle.entity() != null) {
                    laserDisplays.add(handle.entity());
                }
            }

            // Dense spiraling particles down the beam
            for (double y = beamTopY; y > beamBottomY; y -= 0.5) {
                double spiralAngle = (beamTopY - y) * 0.5 + laserSweepAngle * 2;
                double px = beamX + Math.cos(spiralAngle) * 0.5;
                double pz = beamZ + Math.sin(spiralAngle) * 0.5;
                world.spawnParticle(Particle.DUST,
                        px, y, pz, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 230, 255), 2.0f));
                world.spawnParticle(Particle.END_ROD, px, y, pz, 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Ground impact: frost trail where beam touches
            Location groundImpact = new Location(world, beamX, beamBottomY + 1, beamZ);
            world.spawnParticle(Particle.SNOWFLAKE, groundImpact, 20, 2, 0.5, 2, 0.02);
            world.spawnParticle(Particle.DUST,
                    groundImpact, 10, 1.5, 0.2, 1.5, 0.01,
                    new Particle.DustOptions(Color.fromRGB(150, 220, 255), 1.5f));

            // Damage all players every 20 ticks
            int fireOffset = laserTick - chargeTicks;
            if (fireOffset % 20 == 0) {
                double baseDamage = config.getSuperLaserDamage();
                double beamMultiplier = config.getSuperLaserBeamMultiplier();

                for (Player p : world.getPlayers()) {
                    // Check proximity to beam
                    double distToBeam = horizontalDistance(p.getLocation(), beamX, beamZ);
                    if (distToBeam <= 3.0) {
                        // Direct beam hit — multiplied damage
                        p.damage(baseDamage * beamMultiplier);
                    } else {
                        // Unavoidable ambient damage
                        p.damage(baseDamage);
                    }
                }
            }

            // Continuous fire sound (configurable)
            if (fireOffset % 15 == 0) {
                String fireSound = config.getLaserFireSound();
                world.playSound(bossLoc, fireSound, SoundCategory.HOSTILE,
                        config.getLaserFireSoundVolume(), config.getLaserFireSoundPitch());
            }

            // Move laser model to stay at boss
            if (laserModelEntity != null && laserModelEntity.isValid()) {
                laserModelEntity.teleport(bossLoc.clone().add(0, -2, 0));
            }

        } else {
            // ── End phase ─────────────────────────────────────────────────
            laserActive = false;
            laserTick = 0;
            // Play end animation on model before cleanup
            playLaserAnimation("end");
            // Delay cleanup by 10 ticks so end animation plays
            Bukkit.getScheduler().runTaskLater(plugin, this::clearLaserDisplays, 10L);
            String endSound = config.getLaserEndSound();
            world.playSound(bossLoc, endSound, SoundCategory.HOSTILE,
                    config.getLaserEndSoundVolume(), config.getLaserEndSoundPitch());
        }
    }

    /**
     * Remove all laser beam display entities and the ModelEngine laser model.
     */
    private void clearLaserDisplays() {
        for (Entity e : laserDisplays) {
            if (e != null && e.isValid() && !e.isDead()) {
                e.remove();
            }
        }
        laserDisplays.clear();
        despawnLaserModel();
    }

    /**
     * Spawn the ModelEngine laser model at the boss location.
     * This creates a separate entity inside/below the boss that plays the laser animation.
     */
    private void spawnLaserModel(Location loc) {
        despawnLaserModel(); // Clean up any existing
        String modelId = config.getLaserModelEngineId();
        if (modelId == null || modelId.isEmpty()) return;

        try {
            // Try ModelEngine API via reflection
            Class<?> meClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            var createMethod = meClass.getMethod("createModeledEntity", org.bukkit.entity.Entity.class);

            // Spawn a marker armor stand as the model host
            laserModelEntity = loc.getWorld().spawn(loc, org.bukkit.entity.ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setMarker(true);
                stand.setInvulnerable(true);
                stand.setSilent(true);
            });

            var modeledEntity = createMethod.invoke(null, laserModelEntity);
            var getModelMethod = meClass.getMethod("createActiveModel", String.class);
            var activeModel = getModelMethod.invoke(null, modelId);

            if (activeModel != null && modeledEntity != null) {
                var addModelMethod = modeledEntity.getClass().getMethod("addModel", activeModel.getClass().getInterfaces()[0]);
                addModelMethod.invoke(modeledEntity, activeModel);

                // Play charge animation
                try {
                    var getAnimHandler = activeModel.getClass().getMethod("getAnimationHandler");
                    var animHandler = getAnimHandler.invoke(activeModel);
                    var playMethod = animHandler.getClass().getMethod("playAnimation", String.class, double.class, double.class, double.class, boolean.class);
                    playMethod.invoke(animHandler, "charge", 0.0, 0.0, 1.0, false);
                } catch (Exception ignored) {}
            }

            plugin.debug("[BlueMoon] Spawned laser ModelEngine model: " + modelId);
        } catch (ClassNotFoundException e) {
            // ModelEngine not installed — skip model, block displays will still show
            plugin.debug("[BlueMoon] ModelEngine not found, laser uses block displays only.");
        } catch (Exception e) {
            plugin.getLogger().warning("[BlueMoon] Failed to spawn laser model: " + e.getMessage());
        }
    }

    /**
     * Play a named animation on the laser model (charge, fire, idle, end).
     */
    private void playLaserAnimation(String animationName) {
        if (laserModelEntity == null || laserModelEntity.isDead()) return;
        String modelId = config.getLaserModelEngineId();
        if (modelId == null || modelId.isEmpty()) return;

        try {
            Class<?> meClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            var getModeledEntity = meClass.getMethod("getModeledEntity", java.util.UUID.class);
            var modeledEntity = getModeledEntity.invoke(null, laserModelEntity.getUniqueId());
            if (modeledEntity == null) return;

            var getModels = modeledEntity.getClass().getMethod("getModels");
            @SuppressWarnings("unchecked")
            var models = (java.util.Map<String, ?>) getModels.invoke(modeledEntity);
            var activeModel = models.get(modelId);
            if (activeModel == null) return;

            var getAnimHandler = activeModel.getClass().getMethod("getAnimationHandler");
            var animHandler = getAnimHandler.invoke(activeModel);
            var playMethod = animHandler.getClass().getMethod("playAnimation", String.class, double.class, double.class, double.class, boolean.class);
            playMethod.invoke(animHandler, animationName, 0.0, 0.0, 1.0, false);
            plugin.debug("[BlueMoon] Laser animation: " + animationName);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            plugin.debug("[BlueMoon] Failed to play laser animation '" + animationName + "': " + e.getMessage());
        }
    }

    private void playLaserFireAnimation() {
        playLaserAnimation("fire");
    }

    /**
     * Remove the ModelEngine laser model entity.
     */
    private void despawnLaserModel() {
        if (laserModelEntity != null) {
            if (laserModelEntity.isValid() && !laserModelEntity.isDead()) {
                laserModelEntity.remove();
            }
            laserModelEntity = null;
        }
    }

    // ========================================================================
    // Boss death
    // ========================================================================

    /**
     * Called when the boss entity dies or is removed.
     */
    private void onBossDeath() {
        if (!bossAlive) return;
        bossAlive = false;

        plugin.getLogger().info("[BlueMoon] Boss defeated!");

        Location deathLoc = (bossEntity != null && bossEntity.isValid())
                ? bossEntity.getLocation()
                : null;

        if (deathLoc != null) {
            World world = deathLoc.getWorld();

            // Death sounds
            world.playSound(deathLoc, Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 3.0f, 0.6f);
            world.playSound(deathLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 0.5f);

            // Massive particle explosion
            world.spawnParticle(Particle.END_ROD, deathLoc, 500, 10, 10, 10, 0.2);
            world.spawnParticle(Particle.SNOWFLAKE, deathLoc, 300, 15, 8, 15, 0.1);
            world.spawnParticle(Particle.FLASH, deathLoc, 5, 0, 0, 0, 0);
            world.spawnParticle(Particle.DUST,
                    deathLoc, 200, 12, 8, 12, 0.05,
                    new Particle.DustOptions(Color.fromRGB(136, 204, 255), 3.0f));

            // Broadcast
            for (Player p : world.getPlayers()) {
                p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "The Blue Moon has been shattered!");
            }
        }

        // Clean up laser if active
        if (laserActive) {
            laserActive = false;
            clearLaserDisplays();
        }

        // Trigger early kill rewards
        if (earlyKillCallback != null) {
            earlyKillCallback.run();
        }
    }

    // ========================================================================
    // Enrage (Phase 4)
    // ========================================================================

    /**
     * Phase 4 enrage: boss heals 10% of max HP and re-enters phase 3.
     */
    // Enrage mechanic removed — boss stays in Phase 4 until death.

    // ========================================================================
    // Cleanup
    // ========================================================================

    /**
     * Full cleanup — remove boss entity, clear displays, reset all state.
     */
    public void cleanup() {
        if (bossEntity != null && bossEntity.isValid() && !bossEntity.isDead()) {
            bossEntity.remove();
        }
        bossEntity = null;
        bossUUID = null;
        bossAlive = false;
        currentPhase = 1;
        phase4EnrageTicks = 0;
        laserActive = false;
        laserTick = 0;
        laserCooldown = 0;
        orbitAngle = 0;
        savedBossMaxHealth = 0;
        laserTarget = null;
        laserSweepAngle = 0;
        clearLaserDisplays();
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
        onBossDeath();
    }

    // ========================================================================
    // Getters
    // ========================================================================

    /**
     * Returns the boss health ratio as 0.0-1.0 (current / max).
     */
    public double getHealthRatio() {
        if (!bossAlive || bossEntity == null) return 0.0;
        if (bossEntity instanceof LivingEntity living) {
            double max = living.getMaxHealth();
            if (max <= 0) return 0.0;
            return living.getHealth() / max;
        }
        // Non-living entity: if still valid, treat as full health
        return bossEntity.isValid() ? 1.0 : 0.0;
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public boolean isBossAlive() {
        return bossAlive;
    }

    public boolean isLaserActive() {
        return laserActive;
    }

    public Entity getBossEntity() {
        return bossEntity;
    }

    /**
     * Set a callback that fires when the boss is killed (for early-kill rewards).
     */
    public void setEarlyKillCallback(Runnable callback) {
        this.earlyKillCallback = callback;
    }

    // ========================================================================
    // Utility
    // ========================================================================

    /**
     * Find the average position of all players in the given world.
     */
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

    /**
     * Find the player in the densest cluster and spawn above them.
     * For each player, count how many other players are within 30 blocks.
     * Pick the player with the most neighbors — this ensures the boss
     * spawns where the most action is happening.
     * If tied, picks randomly among the top candidates.
     */
    private Location findDensestPlayerCluster(World world) {
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return null;
        if (players.size() == 1) return players.get(0).getLocation();

        double clusterRadius = 30.0;
        double clusterRadiusSq = clusterRadius * clusterRadius;

        Player bestPlayer = null;
        int bestNeighbors = -1;
        List<Player> topCandidates = new ArrayList<>();

        for (Player p : players) {
            int neighbors = 0;
            for (Player other : players) {
                if (other == p) continue;
                if (p.getLocation().distanceSquared(other.getLocation()) <= clusterRadiusSq) {
                    neighbors++;
                }
            }
            if (neighbors > bestNeighbors) {
                bestNeighbors = neighbors;
                topCandidates.clear();
                topCandidates.add(p);
            } else if (neighbors == bestNeighbors) {
                topCandidates.add(p);
            }
        }

        // Pick randomly among top candidates
        bestPlayer = topCandidates.get(new Random().nextInt(topCandidates.size()));
        return bestPlayer.getLocation();
    }

    /**
     * Find the nearest player to the boss entity in the given world.
     */
    private Player findNearestPlayer(World world) {
        if (bossEntity == null) return null;
        Location bossLoc = bossEntity.getLocation();
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Player p : world.getPlayers()) {
            double dist = p.getLocation().distanceSquared(bossLoc);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    /**
     * Horizontal distance from a location to a point (X/Z only).
     */
    private double horizontalDistance(Location loc, double x, double z) {
        double dx = loc.getX() - x;
        double dz = loc.getZ() - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Format a location for logging.
     */
    private String formatLoc(Location loc) {
        return String.format("(%s, %.1f, %.1f, %.1f)",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}
