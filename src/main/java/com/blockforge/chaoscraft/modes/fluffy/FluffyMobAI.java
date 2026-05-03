package com.blockforge.chaoscraft.modes.fluffy;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom Java state-machine AI for FluffyMode mobs.
 *
 * Any LivingEntity tagged with the scoreboard tag {@code fluffy:managed}
 * is registered into a per-tick state machine. The type of behavior
 * (bunny / bear / fox / dog / bird / default) is read from the tag
 * {@code fluffy:type:<name>} or inferred from EntityType.
 *
 * State graph:
 * <pre>
 *   IDLE ── player in range ──> APPROACH
 *   APPROACH ── within attack-range ──> ATTACK
 *   ATTACK ── damage applied ──> COOLDOWN
 *   COOLDOWN ── elapsed ──> IDLE
 *   any ── HP below flee-threshold ──> FLEE
 * </pre>
 *
 * ModelEngine animations are dispatched via reflection — failures are
 * silently swallowed so vanilla mobs (no ME model) work seamlessly.
 */
public class FluffyMobAI implements Listener {

    private final ChaosCraftPlugin plugin;
    private final FluffyConfig config;
    private final Map<UUID, FluffyMobState> states = new HashMap<>();
    private BukkitRunnable runnable;
    private boolean running = false;

    public FluffyMobAI(ChaosCraftPlugin plugin, FluffyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (running) return;
        running = true;
        states.clear();

        // Register any pre-existing managed mobs (already in world before mode start).
        for (var world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e instanceof LivingEntity le && e.getScoreboardTags().contains("fluffy:managed")) {
                    register(le);
                }
            }
        }

        int interval = Math.max(1, config.getMobAiTickInterval());
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }
                tickAll();
            }
        };
        runnable.runTaskTimer(plugin, interval, interval);
    }

    public void stop() {
        running = false;
        if (runnable != null) {
            try { runnable.cancel(); } catch (IllegalStateException ignored) {}
            runnable = null;
        }
        states.clear();
    }

    // ========================
    // Listener
    // ========================

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (!running) return;
        Entity e = event.getEntity();
        if (!(e instanceof LivingEntity le)) return;
        // Tag may be applied by spawner code AFTER spawn; schedule a deferred check too.
        if (e.getScoreboardTags().contains("fluffy:managed")) {
            register(le);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (running && le.isValid() && le.getScoreboardTags().contains("fluffy:managed")) {
                    register(le);
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        states.remove(event.getEntity().getUniqueId());
    }

    public void register(LivingEntity entity) {
        if (entity == null || !entity.isValid()) return;
        if (states.containsKey(entity.getUniqueId())) return;
        FluffyMobState state = new FluffyMobState();
        state.typeKey = resolveTypeKey(entity);
        states.put(entity.getUniqueId(), state);
    }

    // ========================
    // Per-tick AI evaluation
    // ========================

    private void tickAll() {
        if (states.isEmpty()) return;

        var iter = states.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            UUID id = entry.getKey();
            FluffyMobState state = entry.getValue();

            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof LivingEntity le) || !le.isValid() || le.isDead()) {
                iter.remove();
                continue;
            }

            tickEntity(le, state);
        }
    }

    private void tickEntity(LivingEntity entity, FluffyMobState state) {
        state.stateTicks++;

        // Flee check
        double hpPct = (entity.getHealth() / safeMaxHealth(entity)) * 100.0;
        if (state.state != State.FLEE && hpPct <= config.getMobAiFleeHpPercent()) {
            transition(state, State.FLEE, entity);
        }

        Player target = resolveTarget(entity, state);

        switch (state.state) {
            case IDLE -> tickIdle(entity, state, target);
            case APPROACH -> tickApproach(entity, state, target);
            case ATTACK -> tickAttack(entity, state, target);
            case COOLDOWN -> tickCooldown(entity, state);
            case FLEE -> tickFlee(entity, state, target);
        }
    }

    private void tickIdle(LivingEntity entity, FluffyMobState state, Player target) {
        if (target != null) {
            transition(state, State.APPROACH, entity);
        } else {
            playAnim(entity, state, "idle");
        }
    }

    private void tickApproach(LivingEntity entity, FluffyMobState state, Player target) {
        if (target == null) { transition(state, State.IDLE, entity); return; }

        double distSq = entity.getLocation().distanceSquared(target.getLocation());
        double attackRange = config.getMobAiAttackRange();

        if (distSq <= attackRange * attackRange) {
            transition(state, State.ATTACK, entity);
            return;
        }

        // Dog pack-alert: if any other dog is in ATTACK within pack-alert-range, accelerate
        double speedMult = config.getMobAiApproachSpeedMult();
        if ("bear".equals(state.typeKey)) speedMult = config.getBearSpeedMult();
        if ("fox".equals(state.typeKey) && state.stateTicks < config.getFoxStrafeTicks()) {
            // Fox circle-strafe before approach commits
            doFoxStrafe(entity, state, target);
            return;
        }

        if (entity instanceof Mob mob) {
            try {
                mob.getPathfinder().moveTo(target, speedMult);
            } catch (Throwable ignored) {
                // Some entity types throw; ignore and use velocity nudge as fallback
                Vector dir = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.15);
                entity.setVelocity(entity.getVelocity().add(dir));
            }
        }

        // Bunny hop
        if ("bunny".equals(state.typeKey)
                && state.stateTicks % Math.max(1, config.getBunnyHopInterval()) == 0
                && entity.isOnGround()) {
            entity.setVelocity(entity.getVelocity().setY(config.getBunnyHopY()));
        }

        // Bird bob
        if ("bird".equals(state.typeKey)) {
            double bob = Math.sin(state.stateTicks * 0.4) * config.getBirdBobAmplitude();
            entity.setVelocity(entity.getVelocity().setY(bob));
        }

        playAnim(entity, state, "walk");
    }

    private void doFoxStrafe(LivingEntity entity, FluffyMobState state, Player target) {
        double radius = config.getFoxStrafeRadius();
        double angle = state.stateTicks * 0.2;
        Location anchor = target.getLocation();
        double tx = anchor.getX() + Math.cos(angle) * radius;
        double tz = anchor.getZ() + Math.sin(angle) * radius;
        Location strafePoint = new Location(anchor.getWorld(), tx, anchor.getY(), tz);
        if (entity instanceof Mob mob) {
            try {
                mob.getPathfinder().moveTo(strafePoint, config.getMobAiApproachSpeedMult());
            } catch (Throwable ignored) {}
        }
        playAnim(entity, state, "walk");
    }

    private void tickAttack(LivingEntity entity, FluffyMobState state, Player target) {
        if (target == null) { transition(state, State.IDLE, entity); return; }

        double distSq = entity.getLocation().distanceSquared(target.getLocation());
        double attackRange = config.getMobAiAttackRange();
        if (distSq > (attackRange + 1.0) * (attackRange + 1.0)) {
            transition(state, State.APPROACH, entity);
            return;
        }

        // Bear windup — wait windup-ticks before swinging
        int windup = "bear".equals(state.typeKey) ? config.getBearWindupTicks() : 8;
        if (state.stateTicks >= windup) {
            // Apply damage via attribute
            try {
                double dmg = 2.0;
                if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                    dmg = entity.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
                }
                target.damage(Math.max(1.0, dmg), entity);
            } catch (Throwable ignored) {}
            transition(state, State.COOLDOWN, entity);
        } else {
            playAnim(entity, state, "attack");
        }
    }

    private void tickCooldown(LivingEntity entity, FluffyMobState state) {
        playAnim(entity, state, "idle");
        if (state.stateTicks >= config.getMobAiAttackCooldownTicks()) {
            transition(state, State.IDLE, entity);
        }
    }

    private void tickFlee(LivingEntity entity, FluffyMobState state, Player target) {
        if (target == null) { transition(state, State.IDLE, entity); return; }
        Location away = entity.getLocation().clone().add(
                entity.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(6.0));
        if (entity instanceof Mob mob) {
            try {
                mob.getPathfinder().moveTo(away, config.getMobAiApproachSpeedMult() * 1.2);
            } catch (Throwable ignored) {}
        }
        playAnim(entity, state, "flee");
    }

    // ========================
    // Helpers
    // ========================

    private void transition(FluffyMobState state, State next, LivingEntity entity) {
        if (state.state == next) return;
        state.state = next;
        state.stateTicks = 0;
        // Trigger animation change appropriate to new state
        switch (next) {
            case IDLE -> playAnim(entity, state, "idle");
            case APPROACH -> playAnim(entity, state, "walk");
            case ATTACK -> playAnim(entity, state, "attack");
            case COOLDOWN -> playAnim(entity, state, "idle");
            case FLEE -> playAnim(entity, state, "flee");
        }
    }

    private Player resolveTarget(LivingEntity entity, FluffyMobState state) {
        double range = config.getMobAiDetectionRange();
        Player nearest = null;
        double bestSq = range * range;
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.isDead() || !p.isOnline()) continue;
            // Respect mode exempt
            if (plugin.getModeManager().isAnyModeActive()
                    && plugin.getModeManager().getActiveMode().isExempt(p)) {
                continue;
            }
            double dSq = entity.getLocation().distanceSquared(p.getLocation());
            if (dSq < bestSq) {
                bestSq = dSq;
                nearest = p;
            }
        }
        if (nearest != null) state.targetId = nearest.getUniqueId();
        return nearest;
    }

    private String resolveTypeKey(LivingEntity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith("fluffy:type:")) {
                return tag.substring("fluffy:type:".length()).toLowerCase();
            }
        }
        EntityType t = entity.getType();
        return switch (t.name()) {
            case "RABBIT" -> "bunny";
            case "FOX" -> "fox";
            case "WOLF" -> "dog";
            case "PANDA", "POLAR_BEAR" -> "bear";
            case "PARROT", "CHICKEN", "BAT" -> "bird";
            default -> "default";
        };
    }

    private double safeMaxHealth(LivingEntity entity) {
        try {
            var attr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) return Math.max(1.0, attr.getValue());
        } catch (Throwable ignored) {}
        return 20.0;
    }

    /**
     * Dispatch a ModelEngine animation via reflection.
     * Fails silently — vanilla entities have no ME model.
     */
    private void playAnim(LivingEntity entity, FluffyMobState state, String animName) {
        if (animName == null || animName.equals(state.currentAnim)) return;
        state.currentAnim = animName;

        try {
            // ModelEngineAPI.getModeledEntity(entity).getModels().values() → ActiveModel
            Class<?> apiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Method getModeled = apiClass.getMethod("getModeledEntity",
                    Class.forName("org.bukkit.entity.Entity"));
            Object modeled = getModeled.invoke(null, entity);
            if (modeled == null) return;

            Method getModels = modeled.getClass().getMethod("getModels");
            Object models = getModels.invoke(modeled);
            if (!(models instanceof java.util.Map<?, ?> modelMap) || modelMap.isEmpty()) return;

            for (Object active : modelMap.values()) {
                if (active == null) continue;
                try {
                    Method getAnimHandler = active.getClass().getMethod("getAnimationHandler");
                    Object animHandler = getAnimHandler.invoke(active);
                    if (animHandler == null) continue;
                    Method playAnimation = animHandler.getClass().getMethod("playAnimation",
                            String.class, double.class, double.class, double.class, boolean.class);
                    playAnimation.invoke(animHandler, animName, 0.2, 0.2, config.getMobAiAnimationSpeed(), true);
                } catch (Throwable ignored) {
                    // Try alternative signature on this active model
                }
            }
        } catch (Throwable ignored) {
            // ME not present, or entity not modeled — silent
        }
    }

    // ========================
    // State holder
    // ========================

    public enum State { IDLE, APPROACH, ATTACK, COOLDOWN, FLEE }

    private static class FluffyMobState {
        State state = State.IDLE;
        int stateTicks = 0;
        UUID targetId = null;
        String typeKey = "default";
        String currentAnim = "";
    }
}
