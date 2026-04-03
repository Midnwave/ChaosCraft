package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Base class for ModelEngine 4 animated VFX attacks.
 *
 * <p>These attacks spawn a ModelEngine blueprint (a .bbmodel) as an invisible
 * armor stand or marker entity with the model applied. The model handles its
 * own animations (spawn, idle, dissipate) and the attack handles damage,
 * duration, and cleanup.
 *
 * <p>ModelEngine integration via reflection — no hard compile dependency.
 * If ModelEngine is not installed, the attack falls back to a particle-only
 * effect so the mode can still function.
 *
 * <p>Each subclass specifies:
 * <ul>
 *   <li>{@link #getModelId()} — the ModelEngine blueprint ID (matches the .bbmodel filename)</li>
 *   <li>{@link #getModelScale()} — scale multiplier for the model (default 1.0)</li>
 *   <li>{@link #onSpawn(Location)} — spawn logic (model + particles/sounds)</li>
 *   <li>{@link #onTick(int)} — tick logic (damage, animation triggers)</li>
 *   <li>{@link #onModelCleanup()} — extra cleanup beyond model removal</li>
 * </ul>
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>spawn() → creates invisible marker entity → applies ModelEngine blueprint → plays "spawn" animation</li>
 *   <li>tick() → damage checks, "idle" animation loops automatically</li>
 *   <li>cleanup() → plays "dissipate" animation → waits → removes entity and model</li>
 * </ol>
 */
public abstract class ModelEngineAttack extends AbstractAttack {

    protected final DisplayBuilder displayBuilder;

    /** The marker entity that hosts the ModelEngine model. */
    protected Entity modelHost;

    /** The ModelEngine ActiveModel reference (Object to avoid compile dep). */
    protected Object activeModel;

    /** Whether ModelEngine is available on this server. */
    private static Boolean meAvailable;

    protected ModelEngineAttack(ChaosCraftPlugin plugin, AttackConfig config) {
        super(plugin, config);
        this.displayBuilder = new DisplayBuilder(plugin);
    }

    // ========================
    // Subclass API
    // ========================

    /**
     * The ModelEngine blueprint ID. Must match the .bbmodel filename
     * registered in ModelEngine (e.g. "glacial_throne" → glacial_throne.bbmodel).
     */
    protected abstract String getModelId();

    /**
     * Scale multiplier for the model.
     * Reads from attack config key "modelengine-scale":
     *   - A number (e.g. "2.0") = fixed scale multiplier
     *   - "auto" = scale proportional to damage radius (radius / 5.0, min 0.5)
     *   - Default "1.0" = normal size
     *
     * Set in each attack's YAML config section, e.g.:
     *   tide_breaker:
     *     modelengine-scale: auto
     *   selenite_spear:
     *     modelengine-scale: 1.5
     */
    protected double getModelScale() {
        String scaleStr = config.getModelengineScale();
        if ("auto".equalsIgnoreCase(scaleStr.trim())) {
            double radius = config.getDamageRadius();
            return Math.max(0.5, radius / 5.0);
        }
        try {
            return Double.parseDouble(scaleStr.trim());
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    /**
     * Called after the model entity is cleaned up.
     * Override to clean up any extra state (particles, scheduled tasks, etc.).
     */
    protected void onModelCleanup() {}

    // ========================
    // ModelEngine API (reflection)
    // ========================

    /**
     * Check if ModelEngine is installed and available.
     */
    protected static boolean isModelEngineAvailable() {
        if (meAvailable == null) {
            meAvailable = Bukkit.getPluginManager().getPlugin("ModelEngine") != null;
        }
        return meAvailable;
    }

    /**
     * Spawn a marker entity and apply the ModelEngine blueprint to it.
     * Plays the "spawn" animation if it exists.
     *
     * @param location where to spawn the model
     * @return true if the model was successfully applied, false if ME is unavailable
     */
    protected boolean spawnModel(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        // Spawn on the surface — get highest block at X/Z, then offset Y+1.5
        // so models don't clip into ground or spawn underground
        int surfaceY = world.getHighestBlockYAt(location.getBlockX(), location.getBlockZ());
        Location spawnLoc = location.clone();
        spawnLoc.setY(surfaceY + 1.5);

        // Spawn invisible zombie as the model host — marker ArmorStands zero out
        // dimensions which prevents ME4 from scaling models. Zombie matches the
        // working Seer boss implementation.
        org.bukkit.entity.Zombie zombie = world.spawn(spawnLoc, org.bukkit.entity.Zombie.class, z -> {
            z.setInvisible(true);
            z.setSilent(true);
            z.setGravity(false);
            z.setInvulnerable(true);
            z.setAI(false);
            z.setPersistent(false);
            z.setRemoveWhenFarAway(false);
            z.setShouldBurnInDay(false);
            z.setBaby(false);
            z.setCollidable(false);
            z.setCustomNameVisible(false);
            z.addScoreboardTag("chaoscraft_display");

            // Prevent zombie from spawning with or picking up equipment
            z.setCanPickupItems(false);
            z.getEquipment().clear();
            z.getEquipment().setHelmetDropChance(0);
            z.getEquipment().setChestplateDropChance(0);
            z.getEquipment().setLeggingsDropChance(0);
            z.getEquipment().setBootsDropChance(0);
            z.getEquipment().setItemInMainHandDropChance(0);
            z.getEquipment().setItemInOffHandDropChance(0);
        });
        modelHost = zombie;
        spawnedEntities.add(modelHost);

        if (!isModelEngineAvailable()) {
            plugin.debug("[ModelEngine] ModelEngine not available — using particle fallback for " + getModelId());
            return false;
        }

        try {
            // ModelEngine 4.x API via reflection:
            // ModelEngineAPI.createActiveModel(modelId) → ActiveModel
            Class<?> apiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Method createModel = apiClass.getMethod("createActiveModel", String.class);
            activeModel = createModel.invoke(null, getModelId());

            if (activeModel == null) {
                plugin.getLogger().warning("[ModelEngine] Blueprint '" + getModelId()
                        + "' not found. Check that the .bbmodel is in ModelEngine/blueprints/.");
                return false;
            }

            double scale = getModelScale();
            plugin.debug("[ModelEngine] " + getModelId() + " — raw config value: '" + config.getModelengineScale()
                    + "', computed scale: " + scale + ", damage-radius: " + config.getDamageRadius());

            // Dump all available methods on ActiveModel for debugging
            plugin.debug("[ModelEngine] ActiveModel class: " + activeModel.getClass().getName());
            for (Method m : activeModel.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("scale")) {
                    plugin.debug("[ModelEngine]   scale method: " + m.getName() + "(" +
                            java.util.Arrays.toString(m.getParameterTypes()) + ") -> " + m.getReturnType().getSimpleName());
                }
            }

            // Try setScale BEFORE addModel
            if (scale != 1.0) {
                boolean scaled = false;
                // Try double
                try {
                    Method setScale = activeModel.getClass().getMethod("setScale", double.class);
                    setScale.invoke(activeModel, scale);
                    plugin.debug("[ModelEngine] SUCCESS: setScale(double " + scale + ") on " + getModelId());
                    scaled = true;
                } catch (Exception e1) {
                    plugin.debug("[ModelEngine] setScale(double) failed: " + e1.getClass().getSimpleName() + ": " + e1.getMessage());
                }
                // Try float
                if (!scaled) {
                    try {
                        Method setScale = activeModel.getClass().getMethod("setScale", float.class);
                        setScale.invoke(activeModel, (float) scale);
                        plugin.debug("[ModelEngine] SUCCESS: setScale(float " + scale + ") on " + getModelId());
                        scaled = true;
                    } catch (Exception e2) {
                        plugin.debug("[ModelEngine] setScale(float) failed: " + e2.getClass().getSimpleName() + ": " + e2.getMessage());
                    }
                }
                if (!scaled) {
                    plugin.debug("[ModelEngine] WARNING: No setScale worked before addModel for " + getModelId());
                }
            }

            // Create ModeledEntity and add the ActiveModel
            Class<?> meApiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Method createModeledEntity = meApiClass.getMethod("createModeledEntity", Entity.class);
            Object modeledEntity = createModeledEntity.invoke(null, modelHost);

            if (modeledEntity != null) {
                Method addModel = modeledEntity.getClass().getMethod("addModel",
                        Class.forName("com.ticxo.modelengine.api.model.ActiveModel"), boolean.class);
                addModel.invoke(modeledEntity, activeModel, true);

                // Try setScale AFTER addModel too — some ME4 versions need it post-attach
                if (scale != 1.0) {
                    try {
                        Method setScale = activeModel.getClass().getMethod("setScale", double.class);
                        setScale.invoke(activeModel, scale);
                        // Verify it actually stuck
                        Method getScale = activeModel.getClass().getMethod("getScale");
                        Object scaleVec = getScale.invoke(activeModel);
                        plugin.debug("[ModelEngine] Post-addModel setScale(" + scale + ") — getScale() returns: " + scaleVec);
                    } catch (Exception e) {
                        plugin.debug("[ModelEngine] Post-addModel setScale failed: " + e.getMessage());
                    }
                }
            }

            // Play "spawn" animation if it exists
            playAnimation("spawn", 0.0, false);

            plugin.debug("[ModelEngine] Spawned model '" + getModelId() + "' at "
                    + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("[ModelEngine] ModelEngine API classes not found. Requires ModelEngine 4.x+.");
            meAvailable = false;
            return false;
        } catch (Exception e) {
            plugin.debug("[ModelEngine] Failed to spawn model '" + getModelId() + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Play a named animation on the active model.
     *
     * @param animationName the animation name (e.g. "spawn", "idle", "dissipate")
     * @param lerpIn        blend-in time in seconds (0.0 for instant)
     * @param loop          whether the animation should loop
     */
    protected void playAnimation(String animationName, double lerpIn, boolean loop) {
        if (activeModel == null) return;
        try {
            // activeModel.getAnimationHandler().playAnimation(name, lerpIn, lerpOut, speed, force)
            Object animHandler = activeModel.getClass().getMethod("getAnimationHandler").invoke(activeModel);
            if (animHandler != null) {
                // Try the simple playAnimation(String, double, double, double, boolean)
                Method playAnim = animHandler.getClass().getMethod("playAnimation",
                        String.class, double.class, double.class, double.class, boolean.class);
                playAnim.invoke(animHandler, animationName, lerpIn, lerpIn, 1.0, true);
            }
        } catch (Exception e) {
            plugin.debug("[ModelEngine] Could not play animation '" + animationName
                    + "' on " + getModelId() + ": " + e.getMessage());
        }
    }

    /**
     * Stop a named animation on the active model.
     */
    protected void stopAnimation(String animationName) {
        if (activeModel == null) return;
        try {
            Object animHandler = activeModel.getClass().getMethod("getAnimationHandler").invoke(activeModel);
            if (animHandler != null) {
                Method stopAnim = animHandler.getClass().getMethod("stopAnimation", String.class);
                stopAnim.invoke(animHandler, animationName);
            }
        } catch (Exception e) {
            // Silently ignore — animation may not be playing
        }
    }

    // ========================
    // Cleanup
    // ========================

    @Override
    protected void onCleanup() {
        // Play dissipate animation then remove after delay
        if (activeModel != null) {
            playAnimation("dissipate", 0.0, false);

            // Schedule actual removal after dissipate animation (~14 ticks = 0.7s)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeModelEntity();
                onModelCleanup();
            }, 14L);
        } else {
            // No model — just remove entities immediately
            removeModelEntity();
            onModelCleanup();
        }

        displayBuilder.removeAll();
    }

    private void removeModelEntity() {
        // Remove the ModelEngine model from the entity
        if (activeModel != null && modelHost != null && modelHost.isValid()) {
            try {
                Class<?> meApiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
                Method getModeledEntity = meApiClass.getMethod("getModeledEntity", Entity.class);
                Object modeledEntity = getModeledEntity.invoke(null, modelHost);
                if (modeledEntity != null) {
                    Method removeModel = modeledEntity.getClass().getMethod("removeModel", String.class);
                    removeModel.invoke(modeledEntity, getModelId());
                }
            } catch (Exception e) {
                // Best effort cleanup
            }
        }

        // Remove the host entity
        if (modelHost != null && modelHost.isValid()) {
            modelHost.remove();
        }
        activeModel = null;
        modelHost = null;
    }

    @Override
    public AttackType getType() {
        return AttackType.MODEL_ENGINE;
    }
}
