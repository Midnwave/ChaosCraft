package com.blockforge.chaoscraft.modes.bluemoon;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.nms.ai.BlueMoonFlightGoal;
import net.minecraft.world.entity.Mob;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Manages the Blue Moon boss — a massive flying ModelEngine moon entity.
 * The boss orbits above the largest player cluster using NMS flight AI,
 * fires multi-beam lasers, maintains block-display tornados, and progresses
 * through 4 HP-threshold phases.
 *
 * Spawns a vanilla invisible Zombie with ModelEngine model applied via
 * reflection (graceful fallback if ModelEngine is absent). MythicMobs is
 * optionally tried first when {@code use-mythicmobs} is true in config.
 */
public class BlueMoonBossManager {

    private final ChaosCraftPlugin plugin;
    private final BlueMoonConfig config;
    private final DisplayBuilder displayBuilder;

    // ── Boss entity state ──
    private Entity bossEntity;
    private Object bossActiveModel; // ModelEngine ActiveModel reference for animation control
    private UUID bossUUID;
    private boolean bossAlive = false;
    private int currentPhase = 1;

    // ── NMS AI ──
    private BlueMoonFlightGoal flightGoal;

    // ── Multi-beam laser state ──
    private final List<LaserBeam> laserBeams = new ArrayList<>();
    private int laserGlobalCooldown = 0;

    // ── Tornado state ──
    private final List<Tornado> tornados = new ArrayList<>();
    private double tornadoOrbitAngle = 0;

    // ── Boss attacks ──
    private AttackRegistry attackRegistry;
    private int attackCooldown = 0;

    // ── Proximity sound ──
    private int proximitySoundCooldown = 0;

    // ── Early kill callback ──
    private Runnable earlyKillCallback;

    // ── Tornado block materials ──
    private static final Material[] TORNADO_MATERIALS = {
            Material.BLUE_ICE, Material.PACKED_ICE,
            Material.BLUE_STAINED_GLASS, Material.PRISMARINE
    };

    // ── Random ──
    private final Random random = ThreadLocalRandom.current();

    // ========================================================================
    // Inner classes
    // ========================================================================

    /**
     * Tracks a single laser beam targeting one player.
     * States: CHARGING -> FIRING -> COOLDOWN -> (removed)
     */
    private static class LaserBeam {
        enum State { CHARGING, FIRING, COOLDOWN }

        Player targetPlayer;
        State state;
        int chargeTick;
        int fireTick;
        int cooldownTick;
        int damageIntervalCounter;
        final List<Entity> displays = new ArrayList<>();

        LaserBeam(Player target) {
            this.targetPlayer = target;
            this.state = State.CHARGING;
            this.chargeTick = 0;
            this.fireTick = 0;
            this.cooldownTick = 0;
            this.damageIntervalCounter = 0;
        }

        void removeDisplays() {
            for (Entity e : displays) {
                if (e != null && e.isValid()) e.remove();
            }
            displays.clear();
        }
    }

    /**
     * A persistent tornado formation orbiting the boss.
     * Contains block displays arranged in a spiral.
     */
    private static class Tornado {
        double orbitAngle;      // angle around the boss
        double spinAngle;       // internal spin of the tornado
        final List<BlockDisplay> displays = new ArrayList<>();
        int damageIntervalCounter = 0;

        Tornado(double startAngle) {
            this.orbitAngle = startAngle;
            this.spinAngle = 0;
        }

        void removeDisplays() {
            for (BlockDisplay bd : displays) {
                if (bd != null && bd.isValid()) bd.remove();
            }
            displays.clear();
        }
    }

    // ========================================================================
    // Constructor
    // ========================================================================

    public BlueMoonBossManager(ChaosCraftPlugin plugin, BlueMoonConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.displayBuilder = new DisplayBuilder(plugin);
    }

    // ========================================================================
    // External setters
    // ========================================================================

    public void setAttackRegistry(AttackRegistry registry) {
        this.attackRegistry = registry;
    }

    public void setEarlyKillCallback(Runnable callback) {
        this.earlyKillCallback = callback;
    }

    // ========================================================================
    // Spawning
    // ========================================================================

    /**
     * Spawns the Blue Moon boss in the given world.
     * If use-mythicmobs is enabled, tries MythicMobs first with vanilla fallback.
     */
    public void spawnBoss(World world) {
        if (bossAlive) return;

        // Find center of the largest player cluster
        Location center = findPlayerClusterCenter(world);
        if (center == null) {
            center = world.getSpawnLocation();
        }
        center = center.clone().add(0, config.getBossFloatHeight(), 0);

        // Try MythicMobs first if configured
        Entity spawned = null;
        if (config.isUseMythicMobs()) {
            spawned = trySpawnMythicMobs(center);
        }

        // Fallback: our own zombie entity
        if (spawned == null) {
            spawned = spawnVanillaZombie(center);
        }

        bossEntity = spawned;
        bossUUID = spawned.getUniqueId();
        bossAlive = true;
        currentPhase = 1;
        laserGlobalCooldown = config.getSuperLaserCooldownTicks();
        attackCooldown = config.getBossAttackCooldownPhase1();
        proximitySoundCooldown = 0;
        tornadoOrbitAngle = 0;

        // Set attributes via Bukkit API
        applyAttributes();

        // Set up NMS AI — replace vanilla AI with BlueMoonFlightGoal
        setupNmsAI();

        // Spawn tornado formations
        spawnTornados(world);

        // Force-load chunks around boss
        forceLoadChunksAround(center, 3);

        // Spawn effects
        String spawnSound = config.getBossSpawnSound();
        float spawnVol = config.getBossSpawnSoundVolume();
        world.playSound(center, spawnSound, SoundCategory.HOSTILE, spawnVol, 0.5f);
        world.spawnParticle(Particle.END_ROD, center, 300, 8, 8, 8, 0.05);
        world.spawnParticle(Particle.DUST, center, 200, 10, 5, 10, 0.05,
                new Particle.DustOptions(Color.fromRGB(80, 140, 255), 2.5f));

        // Broadcast
        for (Player p : world.getPlayers()) {
            p.sendMessage(ChatColor.BLUE + "" + ChatColor.BOLD + "The Blue Moon rises...");
        }

        plugin.getLogger().info("[BlueMoon] Boss spawned at " + formatLoc(center)
                + " (HP: " + config.getBossHealth() + ", phase: 1)");

        // Confirm health is correct 3 seconds after spawn (MythicMobs can override attributes)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bossEntity != null && bossEntity.isValid() && bossEntity instanceof LivingEntity living) {
                var maxHpAttr = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHpAttr != null && maxHpAttr.getBaseValue() != config.getBossHealth()) {
                    plugin.getLogger().info("[BlueMoon] Correcting boss HP from " + maxHpAttr.getBaseValue()
                            + " to " + config.getBossHealth());
                    maxHpAttr.setBaseValue(config.getBossHealth());
                    living.setHealth(config.getBossHealth());
                }
            }
        }, 60L); // 3 seconds

        // Force-load the chunk the boss is in
        if (center.getWorld() != null) {
            center.getWorld().getChunkAt(center).setForceLoaded(true);
        }
    }

    /**
     * Force-spawn (admin command alias).
     */
    public void forceSpawn(World world) {
        spawnBoss(world);
    }

    /**
     * Spawn a vanilla invisible Zombie as the boss entity.
     * ModelEngine model is applied via reflection if available.
     */
    private Entity spawnVanillaZombie(Location location) {
        Zombie zombie = location.getWorld().spawn(location, Zombie.class, z -> {
            z.setInvisible(true);
            z.setSilent(true);
            z.setPersistent(true);
            z.setRemoveWhenFarAway(false);
            z.setShouldBurnInDay(false);
            z.setBaby(false);
            z.customName(net.kyori.adventure.text.Component.text("Blue Moon")
                    .color(net.kyori.adventure.text.format.TextColor.color(0x5599FF)));
            z.setCustomNameVisible(false);

            z.addScoreboardTag("chaoscraft_bluemoon_boss");
            z.addScoreboardTag("boss_target");
        });

        // Try to apply ModelEngine model via reflection
        applyModelEngineModel(zombie, config.getBossModelEngineId());

        plugin.getLogger().info("[BlueMoon] Vanilla zombie boss entity spawned (+ ModelEngine attempt).");
        return zombie;
    }

    /**
     * Try to spawn via MythicMobs reflection. Returns null if unavailable or failed.
     */
    private Entity trySpawnMythicMobs(Location location) {
        try {
            Class<?> mmApiClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object instance = mmApiClass.getMethod("inst").invoke(null);
            Object mobManager = instance.getClass().getMethod("getMobManager").invoke(instance);
            Method spawnMob = mobManager.getClass().getMethod("spawnMob", String.class, Location.class);
            Object activeMob = spawnMob.invoke(mobManager, config.getMythicMobId(), location);

            if (activeMob != null) {
                Method getEntity = activeMob.getClass().getMethod("getEntity");
                Object bukkitEntity = getEntity.invoke(activeMob);
                Method getBukkitEntity = bukkitEntity.getClass().getMethod("getBukkitEntity");
                Entity entity = (Entity) getBukkitEntity.invoke(bukkitEntity);
                plugin.getLogger().info("[BlueMoon] MythicMobs boss '" + config.getMythicMobId() + "' spawned.");
                return entity;
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[BlueMoon] MythicMobs not installed — falling back to vanilla zombie.");
        } catch (Exception e) {
            plugin.getLogger().warning("[BlueMoon] MythicMobs spawn failed: " + e.getMessage()
                    + " — falling back to vanilla zombie.");
        }
        return null;
    }

    /**
     * Apply a ModelEngine model to an entity via reflection.
     */
    private void applyModelEngineModel(Entity entity, String modelId) {
        try {
            Class<?> meApiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");

            // Create ActiveModel
            Method createModel = meApiClass.getMethod("createActiveModel", String.class);
            Object activeModel = createModel.invoke(null, modelId);
            if (activeModel == null) {
                plugin.getLogger().warning("[BlueMoon] ModelEngine model '" + modelId + "' not found.");
                return;
            }

            // Create ModeledEntity
            Method createModeledEntity = meApiClass.getMethod("createModeledEntity", Entity.class);
            Object modeledEntity = createModeledEntity.invoke(null, entity);

            // Add model
            Class<?> activeModelClass = Class.forName("com.ticxo.modelengine.api.model.ActiveModel");
            Method addModel = modeledEntity.getClass().getMethod("addModel", activeModelClass, boolean.class);
            addModel.invoke(modeledEntity, activeModel, true);

            // Store reference for animation control
            this.bossActiveModel = activeModel;

            // Play idle animation immediately (prevents zombie walk animation)
            playBossAnimation("idle", true);

            plugin.getLogger().info("[BlueMoon] ModelEngine model '" + modelId + "' applied.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[BlueMoon] ModelEngine not installed — boss will appear as invisible zombie.");
        } catch (Exception e) {
            plugin.getLogger().warning("[BlueMoon] Failed to apply ModelEngine model: " + e.getMessage());
        }
    }

    /**
     * Play a named animation on the boss ModelEngine model.
     * @param name animation name (e.g. "idle", "attack", "death")
     * @param loop whether the animation should loop
     */
    private void playBossAnimation(String name, boolean loop) {
        if (bossActiveModel == null) return;
        try {
            Object animHandler = bossActiveModel.getClass().getMethod("getAnimationHandler").invoke(bossActiveModel);
            if (animHandler != null) {
                Method playAnim = animHandler.getClass().getMethod("playAnimation",
                        String.class, double.class, double.class, double.class, boolean.class);
                playAnim.invoke(animHandler, name, 0.25, 0.25, 1.0, true);
                plugin.debug("[BlueMoon] Playing boss animation: " + name + " (loop=" + loop + ")");
            }
        } catch (Exception e) {
            plugin.debug("[BlueMoon] Could not play animation '" + name + "': " + e.getMessage());
        }
    }

    /**
     * Apply all Bukkit attributes to the boss entity.
     */
    private void applyAttributes() {
        if (!(bossEntity instanceof LivingEntity living)) return;

        var maxHp = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(config.getBossHealth());
            living.setHealth(config.getBossHealth());
        }

        var armor = living.getAttribute(Attribute.ARMOR);
        if (armor != null) armor.setBaseValue(config.getBossArmor());

        var armorTough = living.getAttribute(Attribute.ARMOR_TOUGHNESS);
        if (armorTough != null) armorTough.setBaseValue(config.getBossArmorToughness());

        var knockback = living.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) knockback.setBaseValue(config.getBossKnockbackResistance());

        var speed = living.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(config.getBossMoveSpeed());
    }

    // ========================================================================
    // NMS AI Setup
    // ========================================================================

    /**
     * Replace vanilla AI with NMS BlueMoonFlightGoal.
     * Uses setDeltaMovement + getLookControl instead of Bukkit teleport
     * so ModelEngine model renders properly with head tracking.
     */
    private void setupNmsAI() {
        if (!(bossEntity instanceof LivingEntity living)) return;

        try {
            // Get NMS Mob handle
            net.minecraft.world.entity.Entity nmsEntity = ((CraftLivingEntity) living).getHandle();
            if (!(nmsEntity instanceof Mob nmsMob)) {
                plugin.getLogger().warning("[BlueMoon] Boss entity is not a Mob — cannot set NMS AI");
                return;
            }

            // Clear ALL default AI goals
            nmsMob.goalSelector.removeAllGoals(g -> true);
            nmsMob.targetSelector.removeAllGoals(g -> true);

            // Set no gravity for floating
            nmsMob.setNoGravity(true);

            // Set follow range attribute for detection
            var followAttr = nmsMob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
            if (followAttr != null) followAttr.setBaseValue(config.getBossDetectionRange());

            // Create and configure BlueMoonFlightGoal
            flightGoal = new BlueMoonFlightGoal(nmsMob);
            flightGoal.setFloatHeight(config.getBossFloatHeight());
            flightGoal.setOrbitRadius(config.getBossOrbitRadius());
            flightGoal.setOrbitSpeed(config.getBossOrbitSpeed());
            flightGoal.setMoveSpeed(config.getBossMoveSpeed());
            flightGoal.setDetectionRange(config.getBossDetectionRange());
            flightGoal.setGroupDetectionRadius(config.getGroupDetectionRadius());
            flightGoal.setClusterReevaluateInterval(config.getTargetReevaluateTicks());
            flightGoal.setPhase(1);

            // Add goal with priority 1
            nmsMob.goalSelector.addGoal(1, flightGoal);

            // Make zombie silent + disable burn in sun
            if (living instanceof Zombie zombie) {
                zombie.setShouldBurnInDay(false);
            }
            living.setSilent(true);

            // Stop any navigation pathfinding that might conflict
            nmsMob.getNavigation().stop();

            plugin.getLogger().info("[BlueMoon] NMS AI set up: flight goal active, orbit radius "
                    + config.getBossOrbitRadius() + ", detection range " + config.getBossDetectionRange());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[BlueMoon] Failed to set up NMS AI", e);
        }
    }

    // ========================================================================
    // Tick loop
    // ========================================================================

    /**
     * Called every tick by the mode while active.
     */
    public void tick(World world) {
        if (!bossAlive) return;

        validateBossEntity();
        if (!bossAlive) return;

        // Keep boss chunk force-loaded
        if (bossEntity != null && bossEntity.isValid()) {
            org.bukkit.Chunk chunk = bossEntity.getLocation().getChunk();
            if (!chunk.isForceLoaded()) chunk.setForceLoaded(true);
        }

        tickLaserBeams(world);
        tickTornados(world);
        tickBossAttacks(world);
        tickProximitySound(world);
        checkPhaseTransition(world);
    }

    /**
     * Validate the boss entity still exists and is alive.
     */
    private void validateBossEntity() {
        if (bossEntity == null || bossEntity.isDead() || !bossEntity.isValid()) {
            // Try to re-acquire by UUID
            Entity found = Bukkit.getEntity(bossUUID);
            if (found == null || found.isDead()) {
                onBossDied();
                return;
            }
            bossEntity = found;
        }
    }

    // ========================================================================
    // Multi-Beam Laser
    // ========================================================================

    /**
     * Tick all active laser beams and manage the global cooldown.
     */
    private void tickLaserBeams(World world) {
        if (!config.getSuperLaserEnabled()) return;

        // If no beams active, count down global cooldown
        if (laserBeams.isEmpty()) {
            laserGlobalCooldown--;
            if (laserGlobalCooldown <= 0) {
                startLaserBarrage(world);
                laserGlobalCooldown = config.getSuperLaserCooldownTicks();
            }
            return;
        }

        // Boss is INVINCIBLE while any beam is active
        if (bossEntity instanceof LivingEntity living) {
            living.setInvulnerable(true);
        }

        // Tick each beam
        Iterator<LaserBeam> it = laserBeams.iterator();
        while (it.hasNext()) {
            LaserBeam beam = it.next();

            // Validate target
            if (beam.targetPlayer == null || !beam.targetPlayer.isOnline()
                    || beam.targetPlayer.isDead()
                    || beam.targetPlayer.getGameMode() != GameMode.SURVIVAL) {
                beam.removeDisplays();
                it.remove();
                continue;
            }

            switch (beam.state) {
                case CHARGING -> tickLaserCharge(beam, world);
                case FIRING -> tickLaserFire(beam, world);
                case COOLDOWN -> {
                    beam.cooldownTick++;
                    beam.removeDisplays();
                    if (beam.cooldownTick >= 20) {
                        it.remove();
                    }
                }
            }
        }

        // Restore vulnerability when all beams done
        if (laserBeams.isEmpty()) {
            if (bossEntity instanceof LivingEntity living) {
                living.setInvulnerable(false);
            }
        }
    }

    /**
     * Start a multi-beam laser barrage targeting multiple cluster players.
     */
    private void startLaserBarrage(World world) {
        if (flightGoal == null || bossEntity == null) return;

        int maxBeams = config.getSuperLaserMaxBeams();
        List<net.minecraft.world.entity.player.Player> nmsTargets = flightGoal.getClusterPlayers(maxBeams);

        if (nmsTargets.isEmpty()) return;

        for (net.minecraft.world.entity.player.Player nmsPlayer : nmsTargets) {
            // Convert NMS player to Bukkit player
            Player bukkitPlayer = (Player) nmsPlayer.getBukkitEntity();
            if (bukkitPlayer.getGameMode() != GameMode.SURVIVAL || bukkitPlayer.isInvulnerable()) continue;

            LaserBeam beam = new LaserBeam(bukkitPlayer);
            laserBeams.add(beam);
        }

        if (!laserBeams.isEmpty()) {
            plugin.getLogger().info("[BlueMoon] Laser barrage started: " + laserBeams.size() + " beams");
        }
    }

    /**
     * Tick a laser beam in the CHARGING state.
     * Particles gather toward boss from target direction with rising pitch sound.
     */
    private void tickLaserCharge(LaserBeam beam, World world) {
        beam.chargeTick++;
        Location bossLoc = bossEntity.getLocation();
        Location targetLoc = beam.targetPlayer.getLocation().add(0, 1, 0);

        // Gather particles from target toward boss
        double progress = (double) beam.chargeTick / config.getSuperLaserChargeTicks();
        Vector dir = bossLoc.toVector().subtract(targetLoc.toVector()).normalize();
        int particleCount = (int) (5 + progress * 20);

        for (int i = 0; i < particleCount; i++) {
            double dist = random.nextDouble() * targetLoc.distance(bossLoc) * (1.0 - progress * 0.5);
            Location particleLoc = targetLoc.clone().add(dir.clone().multiply(dist));
            particleLoc.add(
                    (random.nextDouble() - 0.5) * 2.0,
                    (random.nextDouble() - 0.5) * 2.0,
                    (random.nextDouble() - 0.5) * 2.0
            );
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.5f));
        }

        // Rising pitch sound every 10 ticks
        if (beam.chargeTick % 10 == 0) {
            float pitch = 0.5f + (float) progress * 1.5f;
            world.playSound(bossLoc, config.getSuperLaserChargeSound(), SoundCategory.HOSTILE,
                    config.getSuperLaserChargeSoundVolume(), pitch);
        }

        // Transition to FIRING
        if (beam.chargeTick >= config.getSuperLaserChargeTicks()) {
            beam.state = LaserBeam.State.FIRING;
            beam.fireTick = 0;
            world.playSound(bossLoc, config.getSuperLaserFireSound(), SoundCategory.HOSTILE,
                    config.getSuperLaserFireSoundVolume(), config.getSuperLaserFireSoundPitch());
        }
    }

    /**
     * Tick a laser beam in the FIRING state.
     * Particle line + block displays along beam, damage within hit radius.
     */
    private void tickLaserFire(LaserBeam beam, World world) {
        beam.fireTick++;
        Location bossLoc = bossEntity.getLocation();
        Location targetLoc = beam.targetPlayer.getLocation().add(0, 1, 0);

        // Remove old displays
        beam.removeDisplays();

        // Spawn block displays along beam line
        Vector beamDir = targetLoc.toVector().subtract(bossLoc.toVector());
        double beamLen = beamDir.length();
        if (beamLen < 0.5) beamLen = 0.5;
        Vector norm = beamDir.normalize();
        int displayCount = Math.min(30, (int) (beamLen / 1.5));

        for (int i = 0; i < displayCount; i++) {
            double t = (double) i / displayCount;
            Location blockLoc = bossLoc.clone().add(norm.clone().multiply(t * beamLen));
            Material mat = (i % 2 == 0) ? Material.BLUE_ICE : Material.PACKED_ICE;
            DisplayBuilder.BlockDisplayHandle handle = displayBuilder.spawnBlock(blockLoc, mat);
            handle.scale(0.6f, 0.6f, 0.6f);
            handle.glow(80, 160, 255);
            beam.displays.add(handle.entity());
        }

        // Particle line (dual spiral like Seer pattern)
        Vector perp1 = norm.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector perp2 = norm.clone().crossProduct(perp1).normalize();
        if (perp1.lengthSquared() < 0.01) {
            perp1 = new Vector(1, 0, 0);
            perp2 = new Vector(0, 0, 1);
        }

        Particle.DustOptions blue = new Particle.DustOptions(Color.fromRGB(80, 160, 255), 2.0f);
        Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(0, 220, 255), 1.5f);
        double timeOffset = world.getGameTime() * 0.2;

        for (double d = 0; d < beamLen; d += 0.5) {
            double t = d / beamLen;
            double bx = bossLoc.getX() + (targetLoc.getX() - bossLoc.getX()) * t;
            double by = bossLoc.getY() + (targetLoc.getY() - bossLoc.getY()) * t;
            double bz = bossLoc.getZ() + (targetLoc.getZ() - bossLoc.getZ()) * t;

            double angle1 = d * 0.8 + timeOffset;
            double radius = 0.8;
            double sx = bx + perp1.getX() * Math.cos(angle1) * radius + perp2.getX() * Math.sin(angle1) * radius;
            double sy = by + perp1.getY() * Math.cos(angle1) * radius + perp2.getY() * Math.sin(angle1) * radius;
            double sz = bz + perp1.getZ() * Math.cos(angle1) * radius + perp2.getZ() * Math.sin(angle1) * radius;
            world.spawnParticle(Particle.DUST, sx, sy, sz, 1, 0, 0, 0, 0, blue);

            double angle2 = d * 0.8 - timeOffset + Math.PI;
            sx = bx + perp1.getX() * Math.cos(angle2) * radius + perp2.getX() * Math.sin(angle2) * radius;
            sy = by + perp1.getY() * Math.cos(angle2) * radius + perp2.getY() * Math.sin(angle2) * radius;
            sz = bz + perp1.getZ() * Math.cos(angle2) * radius + perp2.getZ() * Math.sin(angle2) * radius;
            world.spawnParticle(Particle.DUST, sx, sy, sz, 1, 0, 0, 0, 0, cyan);
        }

        // Impact glow at target
        world.spawnParticle(Particle.DUST, targetLoc, 8, 0.4, 0.4, 0.4, 0.01,
                new Particle.DustOptions(Color.fromRGB(100, 200, 255), 2.5f));

        // Damage players within beam-hit-radius along the beam
        beam.damageIntervalCounter++;
        if (beam.damageIntervalCounter >= config.getSuperLaserDamageInterval()) {
            beam.damageIntervalCounter = 0;
            double hitRadius = config.getSuperLaserBeamHitRadius();
            double hitRadiusSq = hitRadius * hitRadius;
            double damage = config.getSuperLaserDamage() * config.getSuperLaserBeamMultiplier();

            for (Player p : world.getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                Location pLoc = p.getLocation().add(0, 1, 0);

                // Check distance from player to beam line
                double distToBeam = distancePointToLine(pLoc.toVector(), bossLoc.toVector(), targetLoc.toVector());
                if (distToBeam <= hitRadius) {
                    // Also check player is between boss and target (not behind)
                    double projLen = projectOntoLine(pLoc.toVector(), bossLoc.toVector(), targetLoc.toVector());
                    if (projLen >= -1.0 && projLen <= beamLen + 1.0) {
                        p.damage(damage);
                    }
                }
            }

            // Laser heals boss if configured
            if (config.isLaserHealEnabled() && bossEntity instanceof LivingEntity living) {
                double healAmount = config.getLaserHealPerTick();
                var maxHpAttr = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                double maxHp = maxHpAttr != null ? maxHpAttr.getBaseValue() : config.getBossHealth();
                living.setHealth(Math.min(maxHp, living.getHealth() + healAmount));
            }
        }

        // Transition to COOLDOWN
        if (beam.fireTick >= config.getSuperLaserDurationTicks()) {
            beam.state = LaserBeam.State.COOLDOWN;
            beam.cooldownTick = 0;
            beam.removeDisplays();
            world.playSound(bossLoc, config.getSuperLaserEndSound(), SoundCategory.HOSTILE,
                    config.getSuperLaserEndSoundVolume(), config.getSuperLaserEndSoundPitch());
        }
    }

    /**
     * Distance from a point to an infinite line defined by two points.
     */
    private double distancePointToLine(Vector point, Vector lineStart, Vector lineEnd) {
        Vector lineDir = lineEnd.clone().subtract(lineStart);
        Vector toPoint = point.clone().subtract(lineStart);
        double lineLenSq = lineDir.lengthSquared();
        if (lineLenSq < 0.001) return toPoint.length();
        Vector cross = lineDir.clone().crossProduct(toPoint);
        return cross.length() / Math.sqrt(lineLenSq);
    }

    /**
     * Project a point onto a line, returning how far along the line (in blocks) the projection is.
     */
    private double projectOntoLine(Vector point, Vector lineStart, Vector lineEnd) {
        Vector lineDir = lineEnd.clone().subtract(lineStart);
        Vector toPoint = point.clone().subtract(lineStart);
        double lineLenSq = lineDir.lengthSquared();
        if (lineLenSq < 0.001) return 0;
        return lineDir.dot(toPoint) / Math.sqrt(lineLenSq);
    }

    // ========================================================================
    // Block Display Tornados
    // ========================================================================

    /**
     * Spawn initial tornado formations around the boss.
     */
    private void spawnTornados(World world) {
        int count = config.getTornadoCount();
        double angleStep = (2 * Math.PI) / count;

        for (int i = 0; i < count; i++) {
            Tornado tornado = new Tornado(angleStep * i);
            buildTornadoDisplays(tornado, world);
            tornados.add(tornado);
        }

        plugin.getLogger().info("[BlueMoon] Spawned " + count + " tornado formations.");
    }

    /**
     * Build the block displays for a single tornado (15-20 blocks in a spiral).
     */
    private void buildTornadoDisplays(Tornado tornado, World world) {
        if (bossEntity == null) return;
        Location bossLoc = bossEntity.getLocation();

        double orbitRadius = config.getTornadoOrbitRadius();
        double tornadoX = bossLoc.getX() + orbitRadius * Math.cos(tornado.orbitAngle);
        double tornadoZ = bossLoc.getZ() + orbitRadius * Math.sin(tornado.orbitAngle);
        // Tornados at ground level below the boss, not at boss height
        double tornadoBaseY = world.getHighestBlockYAt((int) tornadoX, (int) tornadoZ) + 1;

        int blockCount = 15 + random.nextInt(6); // 15-20 blocks
        for (int j = 0; j < blockCount; j++) {
            double heightFrac = (double) j / blockCount;
            double spiralAngle = tornado.spinAngle + heightFrac * Math.PI * 4; // 2 full rotations
            double spiralRadius = 1.5 * (1.0 - heightFrac * 0.6); // wider at bottom, narrower at top

            double x = tornadoX + Math.cos(spiralAngle) * spiralRadius;
            double y = tornadoBaseY + heightFrac * config.getTornadoHeight(); // 10 blocks tall
            double z = tornadoZ + Math.sin(spiralAngle) * spiralRadius;

            Material mat = TORNADO_MATERIALS[j % TORNADO_MATERIALS.length];
            Location blockLoc = new Location(world, x, y, z);
            DisplayBuilder.BlockDisplayHandle handle = displayBuilder.spawnBlock(blockLoc, mat);
            handle.scale(0.8f, 0.8f, 0.8f);
            handle.glow(80, 160, 255);
            handle.interpolation(3, 0);
            tornado.displays.add(handle.entity());
        }
    }

    /**
     * Tick all tornado formations: update positions, check player catches, deal damage.
     */
    private void tickTornados(World world) {
        if (bossEntity == null || tornados.isEmpty()) return;

        Location bossLoc = bossEntity.getLocation();
        double orbitRadius = config.getTornadoOrbitRadius();
        double catchRadius = config.getTornadoCatchRadius();
        double catchRadiusSq = catchRadius * catchRadius;

        // Phase-based orbit speed multiplier
        double speedMult = switch (currentPhase) {
            case 2 -> 1.3;
            case 3 -> 1.6;
            case 4 -> 2.0;
            default -> 1.0;
        };

        tornadoOrbitAngle += 0.015 * speedMult;
        if (tornadoOrbitAngle > Math.PI * 2) tornadoOrbitAngle -= Math.PI * 2;

        for (Tornado tornado : tornados) {
            // Update orbit angle
            tornado.orbitAngle += 0.015 * speedMult;
            if (tornado.orbitAngle > Math.PI * 2) tornado.orbitAngle -= Math.PI * 2;

            // Update spin
            tornado.spinAngle += 0.1 * speedMult;
            if (tornado.spinAngle > Math.PI * 2) tornado.spinAngle -= Math.PI * 2;

            // Calculate tornado center position
            double tornadoX = bossLoc.getX() + orbitRadius * Math.cos(tornado.orbitAngle);
            double tornadoZ = bossLoc.getZ() + orbitRadius * Math.sin(tornado.orbitAngle);
            // Tornados at ground level below the boss, not at boss height
        double tornadoBaseY = world.getHighestBlockYAt((int) tornadoX, (int) tornadoZ) + 1;

            // Update each block display position
            int blockCount = tornado.displays.size();
            for (int j = 0; j < blockCount; j++) {
                BlockDisplay bd = tornado.displays.get(j);
                if (bd == null || !bd.isValid()) continue;

                double heightFrac = (double) j / blockCount;
                double spiralAngle = tornado.spinAngle + heightFrac * Math.PI * 4;
                double spiralRadius = 1.5 * (1.0 - heightFrac * 0.6);

                double x = tornadoX + Math.cos(spiralAngle) * spiralRadius;
                double y = tornadoBaseY + heightFrac * config.getTornadoHeight();
                double z = tornadoZ + Math.sin(spiralAngle) * spiralRadius;

                Location newLoc = new Location(world, x, y, z);
                newLoc.setYaw(0);
                newLoc.setPitch(0);
                bd.teleport(newLoc);
            }

            // Tornado dust particles
            Location tornadoCenter = new Location(world, tornadoX, tornadoBaseY + 3, tornadoZ);
            world.spawnParticle(Particle.DUST, tornadoCenter, 5, 1.5, 3.0, 1.5, 0.02,
                    new Particle.DustOptions(Color.fromRGB(150, 200, 255), 1.5f));

            // Check for player catches — launch upward
            tornado.damageIntervalCounter++;
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                Location pLoc = p.getLocation();

                // Check horizontal distance to tornado column
                double dx = pLoc.getX() - tornadoX;
                double dz = pLoc.getZ() - tornadoZ;
                double hDistSq = dx * dx + dz * dz;

                // Check if within tornado height range
                double pY = pLoc.getY();
                if (hDistSq <= catchRadiusSq && pY >= tornadoBaseY - 2 && pY <= tornadoBaseY + 12) {
                    // Launch upward
                    Vector vel = p.getVelocity();
                    vel.setY(Math.max(vel.getY(), 0) + 0.8);
                    p.setVelocity(vel);

                    // Deal damage on interval
                    if (tornado.damageIntervalCounter >= config.getTornadoDamageInterval()) {
                        p.damage(config.getTornadoDamage());
                    }
                }
            }

            if (tornado.damageIntervalCounter >= config.getTornadoDamageInterval()) {
                tornado.damageIntervalCounter = 0;
            }
        }
    }

    // ========================================================================
    // Boss Attacks (from AttackRegistry)
    // ========================================================================

    /**
     * Tick boss attack spawning on cooldown.
     */
    private void tickBossAttacks(World world) {
        if (attackRegistry == null) return;

        attackCooldown--;
        if (attackCooldown > 0) return;

        // Reset cooldown based on current phase
        attackCooldown = switch (currentPhase) {
            case 2 -> config.getBossAttackCooldownPhase2();
            case 3 -> config.getBossAttackCooldownPhase3();
            case 4 -> config.getBossAttackCooldownPhase4();
            default -> config.getBossAttackCooldownPhase1();
        };

        // Find a random BOSS-type attack
        List<AbstractAttack> bossAttacks = new ArrayList<>();
        for (AbstractAttack attack : attackRegistry.getAll()) {
            if (attack.getType() == AttackType.BOSS) {
                bossAttacks.add(attack);
            }
        }
        if (bossAttacks.isEmpty()) return;

        AbstractAttack chosen = bossAttacks.get(random.nextInt(bossAttacks.size()));

        // Spawn at boss location targeting nearest cluster player
        Location spawnLoc = bossEntity.getLocation();
        Player target = findNearestSurvivalPlayer(world);
        chosen.spawn(spawnLoc, target);
    }

    // ========================================================================
    // Proximity Ambient Sound
    // ========================================================================

    /**
     * Play ambient sound to all players within proximity radius.
     */
    private void tickProximitySound(World world) {
        proximitySoundCooldown--;
        if (proximitySoundCooldown > 0) return;

        proximitySoundCooldown = config.getProximitySoundInterval();

        if (bossEntity == null) return;
        Location bossLoc = bossEntity.getLocation();
        double radius = config.getProximitySoundRadius();
        double radiusSq = radius * radius;

        String soundId = config.getProximitySoundId();
        float volume = config.getProximitySoundVolume();
        float pitch = config.getProximitySoundPitch();

        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(bossLoc) <= radiusSq) {
                // Play at PLAYER's location so it feels surrounding
                p.playSound(p.getLocation(), soundId, SoundCategory.HOSTILE, volume, pitch);
            }
        }
    }

    // ========================================================================
    // Phase System
    // ========================================================================

    /**
     * Check HP thresholds and transition phases.
     */
    private void checkPhaseTransition(World world) {
        if (!(bossEntity instanceof LivingEntity living)) return;

        double ratio = getHealthRatio();
        int newPhase = currentPhase;

        if (ratio <= config.getPhase4Threshold()) {
            newPhase = 4;
        } else if (ratio <= config.getPhase3Threshold()) {
            newPhase = 3;
        } else if (ratio <= config.getPhase2Threshold()) {
            newPhase = 2;
        }

        if (newPhase != currentPhase) {
            transitionToPhase(newPhase, world);
        }
    }

    /**
     * Perform phase transition with visual effects and AI update.
     */
    private void transitionToPhase(int newPhase, World world) {
        int oldPhase = currentPhase;
        currentPhase = newPhase;

        // Update flight AI phase
        if (flightGoal != null) {
            flightGoal.setPhase(newPhase);
        }

        // Visual effects at boss location
        Location bossLoc = bossEntity.getLocation();

        // Phase-specific particles
        Particle.DustOptions phaseColor = switch (newPhase) {
            case 2 -> new Particle.DustOptions(Color.fromRGB(0, 200, 255), 2.5f);  // cyan
            case 3 -> new Particle.DustOptions(Color.fromRGB(255, 100, 0), 2.5f);   // orange
            case 4 -> new Particle.DustOptions(Color.fromRGB(255, 0, 0), 3.0f);     // red
            default -> new Particle.DustOptions(Color.fromRGB(80, 140, 255), 2.0f);
        };

        world.spawnParticle(Particle.DUST, bossLoc, 200, 8, 8, 8, 0.1, phaseColor);
        world.spawnParticle(Particle.END_ROD, bossLoc, 100, 5, 5, 5, 0.05);
        world.spawnParticle(Particle.FLASH, bossLoc, 3, 0, 0, 0, 0);

        // Sound
        world.playSound(bossLoc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 2.0f, 0.3f);
        world.playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.5f, 0.5f);

        // Phase transition logged but NOT broadcast to players (no cringy messages)

        plugin.getLogger().info("[BlueMoon] Phase transition: " + oldPhase + " -> " + newPhase
                + " (HP: " + String.format("%.1f%%", getHealthRatio() * 100) + ")");
    }

    // ========================================================================
    // Boss Death
    // ========================================================================

    /**
     * Called when the boss entity dies or is removed.
     */
    private void onBossDied() {
        if (!bossAlive) return;
        bossAlive = false;

        plugin.getLogger().info("[BlueMoon] Boss died.");

        // Death effects
        if (bossEntity != null && bossEntity.isValid()) {
            Location deathLoc = bossEntity.getLocation();
            World world = deathLoc.getWorld();
            if (world != null) {
                world.playSound(deathLoc, config.getBossDeathSound(), SoundCategory.HOSTILE,
                        config.getBossDeathSoundVolume(), 0.5f);
                world.spawnParticle(Particle.DUST, deathLoc, 300, 10, 10, 10, 0.1,
                        new Particle.DustOptions(Color.fromRGB(80, 140, 255), 3.0f));
                world.spawnParticle(Particle.END_ROD, deathLoc, 200, 8, 8, 8, 0.1);
                world.spawnParticle(Particle.FLASH, deathLoc, 5, 0, 0, 0, 0);
            }
        }

        // Trigger early kill callback
        if (earlyKillCallback != null) {
            earlyKillCallback.run();
        }

        // Cleanup all visuals
        cleanupVisuals();
    }

    // ========================================================================
    // Admin Methods
    // ========================================================================

    /**
     * Force-kill the boss instantly with death effects.
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

    /**
     * Force-trigger the laser barrage immediately.
     */
    public void forceLaser() {
        if (!bossAlive || bossEntity == null) return;
        World world = bossEntity.getWorld();
        laserGlobalCooldown = 0;
        // Clear any existing beams
        for (LaserBeam beam : laserBeams) {
            beam.removeDisplays();
        }
        laserBeams.clear();
        if (bossEntity instanceof LivingEntity living) {
            living.setInvulnerable(false);
        }
        startLaserBarrage(world);
    }

    /**
     * Force a phase transition.
     */
    public void forcePhase(int phase) {
        if (!bossAlive || bossEntity == null) return;
        phase = Math.max(1, Math.min(4, phase));
        if (phase != currentPhase) {
            transitionToPhase(phase, bossEntity.getWorld());
        }
    }

    // ========================================================================
    // State Queries
    // ========================================================================

    public boolean isBossAlive() {
        return bossAlive;
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Get the boss HP as a ratio (0.0 = dead, 1.0 = full).
     */
    public double getHealthRatio() {
        if (!(bossEntity instanceof LivingEntity living)) return 0;
        var maxHp = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp == null || maxHp.getBaseValue() <= 0) return 0;
        return living.getHealth() / maxHp.getBaseValue();
    }

    /**
     * Whether any laser beam is currently active (charging or firing).
     */
    public boolean isLaserActive() {
        return !laserBeams.isEmpty();
    }

    public Entity getBossEntity() {
        return bossEntity;
    }

    // ========================================================================
    // Cleanup
    // ========================================================================

    /**
     * Full cleanup: remove boss entity, all tornado/laser displays, reset state.
     */
    public void cleanup() {
        // Mark as dead FIRST so onBossDied() won't fire the kill callback
        // (cleanup = mode ended, NOT boss killed by players)
        bossAlive = false;

        // Unforce-load the boss chunk
        if (bossEntity != null && bossEntity.isValid()) {
            try { bossEntity.getLocation().getChunk().setForceLoaded(false); } catch (Exception ignored) {}
            bossEntity.remove();
        }
        bossEntity = null;
        bossUUID = null;

        cleanupVisuals();

        flightGoal = null;
        attackRegistry = null;
        currentPhase = 1;
    }

    /**
     * Remove all visual entities (tornados, lasers) without removing the boss itself.
     */
    private void cleanupVisuals() {
        // Remove all tornado block displays
        for (Tornado tornado : tornados) {
            tornado.removeDisplays();
        }
        tornados.clear();

        // Remove all laser block displays
        for (LaserBeam beam : laserBeams) {
            beam.removeDisplays();
        }
        laserBeams.clear();

        // Remove display builder tracked entities
        displayBuilder.removeAll();

        // Restore invulnerability state
        if (bossEntity instanceof LivingEntity living) {
            living.setInvulnerable(false);
        }
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Find the center of the largest player cluster in the world.
     */
    private Location findPlayerClusterCenter(World world) {
        List<Player> survivalPlayers = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL && !p.isInvulnerable()) {
                survivalPlayers.add(p);
            }
        }

        if (survivalPlayers.isEmpty()) return null;
        if (survivalPlayers.size() == 1) return survivalPlayers.get(0).getLocation();

        // Find player with the most neighbors within group detection radius
        double groupRadius = config.getGroupDetectionRadius();
        double groupRadiusSq = groupRadius * groupRadius;

        Player bestSeed = null;
        int bestCount = -1;

        for (Player p : survivalPlayers) {
            int neighbors = 0;
            for (Player other : survivalPlayers) {
                if (other == p) continue;
                if (p.getLocation().distanceSquared(other.getLocation()) <= groupRadiusSq) {
                    neighbors++;
                }
            }
            if (neighbors > bestCount) {
                bestCount = neighbors;
                bestSeed = p;
            }
        }

        if (bestSeed == null) return survivalPlayers.get(0).getLocation();

        // Calculate centroid of cluster members
        List<Player> cluster = new ArrayList<>();
        cluster.add(bestSeed);
        for (Player p : survivalPlayers) {
            if (p == bestSeed) continue;
            if (bestSeed.getLocation().distanceSquared(p.getLocation()) <= groupRadiusSq) {
                cluster.add(p);
            }
        }

        double sumX = 0, sumY = 0, sumZ = 0;
        for (Player p : cluster) {
            sumX += p.getLocation().getX();
            sumY += p.getLocation().getY();
            sumZ += p.getLocation().getZ();
        }
        return new Location(world,
                sumX / cluster.size(),
                sumY / cluster.size(),
                sumZ / cluster.size());
    }

    /**
     * Find the nearest survival-mode player to the boss.
     */
    private Player findNearestSurvivalPlayer(World world) {
        if (bossEntity == null) return null;
        Location bossLoc = bossEntity.getLocation();
        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Player p : world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
            double distSq = p.getLocation().distanceSquared(bossLoc);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = p;
            }
        }
        return nearest;
    }

    /**
     * Force-load chunks in a radius around a location.
     */
    private void forceLoadChunksAround(Location center, int chunkRadius) {
        World world = center.getWorld();
        if (world == null) return;
        int cx = center.getBlockX() >> 4;
        int cz = center.getBlockZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                world.getChunkAt(cx + dx, cz + dz).setForceLoaded(true);
            }
        }
    }

    /**
     * Format a location for logging.
     */
    private String formatLoc(Location loc) {
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }
}
