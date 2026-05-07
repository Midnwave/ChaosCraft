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
    private long tickClock = 0L;

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
        tickClock++;
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
            case SPECIAL -> tickSpecial(entity, state, target);
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
        // Use a wider leash (+2.0) than the entry threshold so the mob doesn't
        // oscillate APPROACH <-> ATTACK if the player jitters slightly. The
        // small buffer also ensures we always reach the windup tick.
        if (distSq > (attackRange + 2.0) * (attackRange + 2.0)) {
            transition(state, State.APPROACH, entity);
            return;
        }

        // On attack entry, roll for a per-type special
        if (state.stateTicks == 0) {
            state.specialTarget = target;
            String chosen = chooseSpecial(entity, state, target);
            if (chosen != null) {
                state.pendingSpecial = chosen;
                state.specialPhase = "init";
                state.specialPhaseTick = 0;
                transition(state, State.SPECIAL, entity);
                return;
            }
        }

        // Bear windup — only bears wait. All other types bite immediately on
        // contact (magma-cube tempo per user feedback). Windup of 0 means the
        // mob bites the same tick it enters ATTACK state.
        int windup = "bear".equals(state.typeKey) ? config.getBearWindupTicks() : 0;
        if (state.stateTicks >= windup) {
            damageWithMultiplier(entity, target, 1.0);
            transition(state, State.COOLDOWN, entity);
        } else {
            playAnim(entity, state, "attack");
        }
    }

    // ========================
    // SPECIAL state dispatcher
    // ========================

    private void tickSpecial(LivingEntity entity, FluffyMobState state, Player liveTarget) {
        Player target = state.specialTarget != null && state.specialTarget.isValid() && !state.specialTarget.isDead()
                ? state.specialTarget : liveTarget;
        if (target == null || state.pendingSpecial == null) {
            finishSpecial(state, entity);
            return;
        }
        switch (state.typeKey) {
            case "bunny" -> tickSpecial_bunny(entity, state, target);
            case "bear" -> tickSpecial_bear(entity, state, target);
            case "fox" -> tickSpecial_fox(entity, state, target);
            case "dog" -> tickSpecial_dog(entity, state, target);
            case "bird" -> tickSpecial_bird(entity, state, target);
            case "fluffy_cat" -> tickSpecial_cat(entity, state, target);
            case "fluffy_squirrel" -> tickSpecial_squirrel(entity, state, target);
            default -> finishSpecial(state, entity);
        }
    }

    private void finishSpecial(FluffyMobState state, LivingEntity entity) {
        state.lastSpecialTick = tickClock;
        state.pendingSpecial = null;
        state.specialPhase = null;
        state.specialPhaseTick = 0;
        state.specialTarget = null;
        transition(state, State.COOLDOWN, entity);
    }

    // ── Special selection ───────────────────────────────────────────

    private String chooseSpecial(LivingEntity entity, FluffyMobState state, Player target) {
        if (!rollChance(config.getSpecialRollOnAttack())) return null;
        double hpPct = (entity.getHealth() / safeMaxHealth(entity)) * 100.0;
        switch (state.typeKey) {
            case "bunny": {
                boolean tackleReady = specialReady(state, config.getBunnyTackleCooldownTicks());
                boolean multiplyReady = specialReady(state, config.getBunnyMultiplyCooldownTicks())
                        && hpPct <= config.getBunnyMultiplyHpPercent()
                        && rollChance(config.getBunnyMultiplyChance());
                if (multiplyReady && tackleReady) return Math.random() < 0.5 ? "multiply" : "tackle";
                if (multiplyReady) return "multiply";
                if (tackleReady) return "tackle";
                return null;
            }
            case "bear": {
                boolean pawReady = specialReady(state, config.getBearPawCooldownTicks());
                boolean slamReady = specialReady(state, config.getBearSlamCooldownTicks());
                if (pawReady && slamReady) return Math.random() < 0.5 ? "paw_swipe" : "slam";
                if (pawReady) return "paw_swipe";
                if (slamReady) return "slam";
                return null;
            }
            case "fox":
                return specialReady(state, config.getFoxPounceCooldownTicks()) ? "pounce" : null;
            case "dog":
                return specialReady(state, config.getDogPackLungeCooldownTicks()) ? "pack_lunge" : null;
            case "bird": {
                boolean diveReady = specialReady(state, config.getBirdDiveCooldownTicks());
                double dist = entity.getLocation().distance(target.getLocation());
                boolean buffetReady = specialReady(state, config.getBirdBuffetCooldownTicks())
                        && dist <= config.getBirdBuffetRange();
                if (diveReady && buffetReady) return Math.random() < 0.6 ? "dive" : "buffet";
                if (diveReady) return "dive";
                if (buffetReady) return "buffet";
                return null;
            }
            case "fluffy_cat": {
                if (!specialReady(state, config.getCatSpecialCooldownTicks())) return null;
                double wp = config.getCatWigglePounceChance();
                double ts = config.getCatTripleSwipeChance();
                double total = wp + ts;
                double noTheatreShare = Math.max(0.0, 1.0 - total);
                double roll = Math.random();
                if (roll < wp) return "wiggle_pounce";
                if (roll < wp + ts) return "triple_swipe";
                if (noTheatreShare > 0.0) return "no_theatre";
                return null;
            }
            case "fluffy_squirrel": {
                boolean dartReady = specialReady(state, config.getSquirrelDartCooldownTicks())
                        && hpPct <= config.getSquirrelDartHpPercent();
                boolean jumpReady = specialReady(state, config.getSquirrelJumpPounceCooldownTicks());
                if (dartReady) return "dart";
                if (jumpReady) return "jump_pounce";
                return null;
            }
            default:
                return null;
        }
    }

    // ── Per-type special routines ───────────────────────────────────

    private void tickSpecial_bunny(LivingEntity entity, FluffyMobState state, Player target) {
        if ("multiply".equals(state.pendingSpecial)) {
            int count = Math.max(1, config.getBunnyMultiplySpawnCount());
            double childHpMult = config.getBunnyMultiplyChildHpMult();
            for (int i = 0; i < count; i++) {
                try {
                    Entity child = entity.getWorld().spawnEntity(entity.getLocation(), EntityType.RABBIT);
                    if (child instanceof LivingEntity le) {
                        var maxHp = le.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHp != null) {
                            double newMax = Math.max(1.0, maxHp.getBaseValue() * childHpMult);
                            maxHp.setBaseValue(newMax);
                            le.setHealth(newMax);
                        }
                        le.addScoreboardTag("fluffy:managed");
                        le.addScoreboardTag("fluffy:type:bunny");
                        register(le);
                    }
                } catch (Throwable ignored) {}
            }
            finishSpecial(state, entity);
            return;
        }
        // tackle
        if ("init".equals(state.specialPhase)) {
            launchToward(entity, target,
                    config.getBunnyTackleLeapForward(),
                    config.getBunnyTackleLeapY());
            playAnim(entity, state, "attack");
            state.specialPhase = "leap";
            state.specialPhaseTick = 0;
            return;
        }
        state.specialPhaseTick++;
        if (state.specialPhaseTick >= 8) {
            if (entity.getLocation().distance(target.getLocation()) <= config.getMobAiAttackRange() + 1.0) {
                damageWithMultiplier(entity, target, config.getBunnyTackleDamageMult());
            }
            finishSpecial(state, entity);
        }
    }

    private void tickSpecial_bear(LivingEntity entity, FluffyMobState state, Player target) {
        if ("paw_swipe".equals(state.pendingSpecial)) {
            if ("init".equals(state.specialPhase)) {
                state.specialPhase = "windup";
                state.specialPhaseTick = 0;
                playAnim(entity, state, "attack");
                return;
            }
            state.specialPhaseTick++;
            int windup = Math.max(8, config.getBearWindupTicks());
            if ("windup".equals(state.specialPhase) && state.specialPhaseTick >= windup) {
                damageInCone(entity, config.getBearPawConeRadius(),
                        config.getBearPawConeAngleDeg(), config.getBearPawDamageMult());
                finishSpecial(state, entity);
            }
            return;
        }
        // slam
        if ("init".equals(state.specialPhase)) {
            entity.setVelocity(entity.getVelocity().setY(config.getBearSlamKnockupY()));
            playAnim(entity, state, "attack");
            state.specialPhase = "raise";
            state.specialPhaseTick = 0;
            return;
        }
        state.specialPhaseTick++;
        if ("raise".equals(state.specialPhase) && state.specialPhaseTick >= 6) {
            state.specialPhase = "land";
            state.specialPhaseTick = 0;
            return;
        }
        if ("land".equals(state.specialPhase) && state.specialPhaseTick >= 4) {
            damageInRadius(entity, entity.getLocation(),
                    config.getBearSlamRadius(), config.getBearSlamDamageMult());
            // knockup affected players
            for (Player p : entity.getWorld().getPlayers()) {
                if (p.isDead()) continue;
                if (plugin.getModeManager().isAnyModeActive() && plugin.getModeManager().getActiveMode().isExempt(p)) continue;
                if (p.getLocation().distance(entity.getLocation()) <= config.getBearSlamRadius()) {
                    p.setVelocity(p.getVelocity().setY(config.getBearSlamKnockupY()));
                }
            }
            finishSpecial(state, entity);
        }
    }

    private void tickSpecial_fox(LivingEntity entity, FluffyMobState state, Player target) {
        if ("init".equals(state.specialPhase)) {
            launchToward(entity, target,
                    config.getFoxPounceDistance() / 8.0,
                    config.getFoxPounceYVel());
            playAnim(entity, state, "attack");
            state.specialPhase = "leap";
            state.specialPhaseTick = 0;
            return;
        }
        state.specialPhaseTick++;
        if (state.specialPhaseTick >= 10 || (state.specialPhaseTick > 4 && entity.isOnGround())) {
            if (entity.getLocation().distance(target.getLocation()) <= config.getMobAiAttackRange() + 1.5) {
                damageWithMultiplier(entity, target, config.getFoxPounceDamageMult());
            }
            finishSpecial(state, entity);
        }
    }

    private void tickSpecial_dog(LivingEntity entity, FluffyMobState state, Player target) {
        // immediate effect — alert nearby dogs, then fall through to a single bite
        double range = config.getDogPackAlertRange();
        double speedBoost = config.getMobAiApproachSpeedMult() * config.getDogPackLungeSpeedMult();
        for (FluffyMobState other : states.values()) {
            if (other == state) continue;
            if (!"dog".equals(other.typeKey)) continue;
            Entity oe = Bukkit.getEntity(getKeyFor(other));
            if (!(oe instanceof LivingEntity ole) || !ole.isValid()) continue;
            if (ole.getLocation().distance(entity.getLocation()) > range) continue;
            other.targetId = target.getUniqueId();
            other.state = State.APPROACH;
            other.stateTicks = 0;
            try {
                Vector dir = target.getLocation().toVector()
                        .subtract(ole.getLocation().toVector()).setY(0);
                if (dir.lengthSquared() > 0.0001) {
                    dir.normalize().multiply(speedBoost * 0.25);
                    ole.setVelocity(ole.getVelocity().add(dir));
                }
            } catch (Throwable ignored) {}
        }
        // A single bite for the lead dog
        if (entity.getLocation().distance(target.getLocation()) <= config.getMobAiAttackRange() + 0.5) {
            damageWithMultiplier(entity, target, 1.0);
        }
        finishSpecial(state, entity);
    }

    private UUID getKeyFor(FluffyMobState state) {
        for (var entry : states.entrySet()) {
            if (entry.getValue() == state) return entry.getKey();
        }
        return null;
    }

    private void tickSpecial_bird(LivingEntity entity, FluffyMobState state, Player target) {
        if ("buffet".equals(state.pendingSpecial)) {
            damageWithMultiplier(entity, target, config.getBirdBuffetDamageMult());
            try {
                Vector push = target.getLocation().toVector()
                        .subtract(entity.getLocation().toVector()).setY(0);
                if (push.lengthSquared() > 0.0001) {
                    push.normalize().multiply(config.getBirdBuffetKnockback());
                    push.setY(0.3);
                    target.setVelocity(push);
                }
            } catch (Throwable ignored) {}
            finishSpecial(state, entity);
            return;
        }
        // dive
        if ("init".equals(state.specialPhase)) {
            entity.setVelocity(entity.getVelocity().setY(config.getBirdDiveYRise() / 8.0));
            playAnim(entity, state, "attack");
            state.specialPhase = "rise";
            state.specialPhaseTick = 0;
            return;
        }
        state.specialPhaseTick++;
        if ("rise".equals(state.specialPhase) && state.specialPhaseTick >= 8) {
            entity.setVelocity(entity.getVelocity().setY(config.getBirdDiveYDropVel()));
            state.specialPhase = "drop";
            state.specialPhaseTick = 0;
            return;
        }
        if ("drop".equals(state.specialPhase) && state.specialPhaseTick >= 8) {
            damageInRadius(entity, entity.getLocation(),
                    config.getMobAiAttackRange() + 1.0, config.getBirdDiveDamageMult());
            finishSpecial(state, entity);
        }
    }

    private void tickSpecial_cat(LivingEntity entity, FluffyMobState state, Player target) {
        if ("no_theatre".equals(state.pendingSpecial)) {
            playAnim(entity, state, "attack");
            damageWithMultiplier(entity, target, 1.0);
            finishSpecial(state, entity);
            return;
        }
        if ("triple_swipe".equals(state.pendingSpecial)) {
            int interval = Math.max(2, config.getCatTripleSwipeIntervalTicks());
            if ("init".equals(state.specialPhase)) {
                state.specialPhase = "swipe1";
                state.specialPhaseTick = 0;
                playAnim(entity, state, "attack");
                damageWithMultiplier(entity, target, config.getCatTripleSwipeDamageMult());
                return;
            }
            state.specialPhaseTick++;
            if ("swipe1".equals(state.specialPhase) && state.specialPhaseTick >= interval) {
                state.specialPhase = "swipe2";
                state.specialPhaseTick = 0;
                state.currentAnim = "";
                playAnim(entity, state, "attack");
                damageWithMultiplier(entity, target, config.getCatTripleSwipeDamageMult());
                return;
            }
            if ("swipe2".equals(state.specialPhase) && state.specialPhaseTick >= interval) {
                state.specialPhase = "swipe3";
                state.specialPhaseTick = 0;
                state.currentAnim = "";
                playAnim(entity, state, "attack");
                damageWithMultiplier(entity, target, config.getCatTripleSwipeDamageMult());
                return;
            }
            if ("swipe3".equals(state.specialPhase) && state.specialPhaseTick >= interval) {
                finishSpecial(state, entity);
            }
            return;
        }
        // wiggle_pounce
        if ("init".equals(state.specialPhase)) {
            state.specialPhase = "stalk";
            state.specialPhaseTick = 0;
            playAnim(entity, state, "sit_loop");
            return;
        }
        state.specialPhaseTick++;
        switch (state.specialPhase) {
            case "stalk" -> {
                playAnim(entity, state, "sit_loop");
                if (state.specialPhaseTick >= config.getCatStalkTicks()) {
                    state.specialPhase = "wiggle";
                    state.specialPhaseTick = 0;
                    state.currentAnim = "";
                    playAnim(entity, state, "wiggling");
                }
            }
            case "wiggle" -> {
                if (state.specialPhaseTick >= config.getCatWiggleTicks()) {
                    state.specialPhase = "prepare";
                    state.specialPhaseTick = 0;
                    state.currentAnim = "";
                    playAnim(entity, state, "prepare_attack");
                }
            }
            case "prepare" -> {
                if (state.specialPhaseTick >= config.getCatPrepareAttackTicks()) {
                    launchToward(entity, target,
                            config.getCatPounceDistance() / 8.0,
                            config.getCatPounceYVel());
                    state.specialPhase = "pounce";
                    state.specialPhaseTick = 0;
                    state.currentAnim = "";
                    playAnim(entity, state, "attack");
                }
            }
            case "pounce" -> {
                if (state.specialPhaseTick >= 8) {
                    if (entity.getLocation().distance(target.getLocation()) <= config.getMobAiAttackRange() + 1.5) {
                        damageWithMultiplier(entity, target, config.getCatPounceDamageMult());
                    }
                    finishSpecial(state, entity);
                }
            }
            default -> finishSpecial(state, entity);
        }
    }

    private void tickSpecial_squirrel(LivingEntity entity, FluffyMobState state, Player target) {
        if ("jump_pounce".equals(state.pendingSpecial)) {
            if ("init".equals(state.specialPhase)) {
                playAnim(entity, state, "jump");
                launchToward(entity, target,
                        config.getSquirrelJumpPounceDistance() / 8.0,
                        config.getSquirrelJumpPounceYVel());
                state.specialPhase = "jump";
                state.specialPhaseTick = 0;
                return;
            }
            state.specialPhaseTick++;
            if (state.specialPhaseTick >= 8) {
                if (entity.getLocation().distance(target.getLocation()) <= config.getMobAiAttackRange() + 1.5) {
                    damageWithMultiplier(entity, target, config.getSquirrelJumpPounceDamageMult());
                }
                finishSpecial(state, entity);
            }
            return;
        }
        // dart
        int hopCount = Math.max(1, config.getSquirrelDartHopCount());
        int interval = Math.max(2, config.getSquirrelDartHopIntervalTicks());
        if ("init".equals(state.specialPhase)) {
            state.specialPhase = "hop1";
            state.specialPhaseTick = 0;
            doSquirrelHop(entity, state);
            return;
        }
        state.specialPhaseTick++;
        if (state.specialPhase != null && state.specialPhase.startsWith("hop")) {
            if (state.specialPhaseTick >= interval) {
                int next;
                try { next = Integer.parseInt(state.specialPhase.substring(3)) + 1; }
                catch (NumberFormatException e) { finishSpecial(state, entity); return; }
                if (next > hopCount) {
                    if (entity.getLocation().distance(target.getLocation()) <= config.getMobAiAttackRange() + 1.5) {
                        damageWithMultiplier(entity, target, 1.0);
                    }
                    finishSpecial(state, entity);
                    return;
                }
                state.specialPhase = "hop" + next;
                state.specialPhaseTick = 0;
                doSquirrelHop(entity, state);
            }
        }
    }

    private void doSquirrelHop(LivingEntity entity, FluffyMobState state) {
        double d = config.getSquirrelDartHopDistance();
        double dx = (Math.random() * 2 - 1) * d;
        double dz = (Math.random() * 2 - 1) * d;
        Location dest = entity.getLocation().clone().add(dx, 0, dz);
        try { entity.teleport(dest); } catch (Throwable ignored) {}
        state.currentAnim = "";
        playAnim(entity, state, "jump");
    }

    // ========================
    // Special helpers
    // ========================

    private boolean specialReady(FluffyMobState state, int cooldownTicks) {
        return tickClock - state.lastSpecialTick >= cooldownTicks;
    }

    private boolean rollChance(double chance) {
        return Math.random() < chance;
    }

    private void launchToward(LivingEntity entity, Player target, double forward, double y) {
        try {
            Vector dir = target.getLocation().toVector()
                    .subtract(entity.getLocation().toVector()).setY(0);
            if (dir.lengthSquared() < 0.0001) {
                entity.setVelocity(new Vector(0, y, 0));
                return;
            }
            dir.normalize();
            entity.setVelocity(new Vector(dir.getX() * forward, y, dir.getZ() * forward));
        } catch (Throwable ignored) {}
    }

    private void damageWithMultiplier(LivingEntity attacker, Player target, double multiplier) {
        try {
            double base = 2.0;
            var attr = attacker.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attr != null && attr.getValue() > 0.0) base = attr.getValue();
            double diffMult = 1.0;
            try { diffMult = config.getDifficultyMultiplier(); } catch (Throwable ignored) {}
            // Floor of bite-damage-floor hp per bite so every Fluffy mob hit is
            // significant and the player can clearly tell they were attacked,
            // even with armor / protection enchants soaking ~50% of it. Set
            // mob-ai.bite-damage-floor to 0.0 in config to disable the floor.
            double floor = 0.0;
            try { floor = config.getMobAiBiteFloor(); } catch (Throwable ignored) {}
            double finalDamage = Math.max(floor, base * multiplier * diffMult);
            target.damage(finalDamage, attacker);
            plugin.debug("[FluffyAI] " + attacker.getType() + " bit "
                    + target.getName() + " for " + finalDamage + " hp");
        } catch (Throwable t) {
            plugin.debug("[FluffyAI] Damage call FAILED: " + t.getMessage());
        }
    }

    private void damageInRadius(LivingEntity attacker, Location center, double radius, double multiplier) {
        for (Player p : center.getWorld().getPlayers()) {
            if (p.isDead()) continue;
            if (plugin.getModeManager().isAnyModeActive()
                    && plugin.getModeManager().getActiveMode().isExempt(p)) continue;
            if (p.getLocation().distance(center) <= radius) {
                damageWithMultiplier(attacker, p, multiplier);
            }
        }
    }

    private void damageInCone(LivingEntity attacker, double radius, double angleDegrees, double multiplier) {
        Vector facing;
        try {
            facing = attacker.getLocation().getDirection().setY(0);
            if (facing.lengthSquared() < 0.0001) return;
            facing.normalize();
        } catch (Throwable t) { return; }
        double cosThreshold = Math.cos(Math.toRadians(angleDegrees / 2.0));
        for (Player p : attacker.getWorld().getPlayers()) {
            if (p.isDead()) continue;
            if (plugin.getModeManager().isAnyModeActive()
                    && plugin.getModeManager().getActiveMode().isExempt(p)) continue;
            Vector toPlayer = p.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector()).setY(0);
            if (toPlayer.lengthSquared() > radius * radius) continue;
            if (toPlayer.lengthSquared() < 0.0001) {
                damageWithMultiplier(attacker, p, multiplier);
                continue;
            }
            if (toPlayer.normalize().dot(facing) >= cosThreshold) {
                damageWithMultiplier(attacker, p, multiplier);
            }
        }
    }

    /** Force a special on a managed mob (admin trigger). Returns true if applied. */
    public boolean forceTriggerSpecial(LivingEntity entity, String specialName) {
        FluffyMobState state = states.get(entity.getUniqueId());
        if (state == null) return false;
        Player target = resolveTarget(entity, state);
        if (target == null) return false;
        state.specialTarget = target;
        state.pendingSpecial = specialName;
        state.specialPhase = "init";
        state.specialPhaseTick = 0;
        state.stateTicks = 0;
        state.state = State.SPECIAL;
        return true;
    }

    /** Find the nearest managed mob of the given typeKey within radius of origin. */
    public LivingEntity findNearestManaged(Location origin, String typeKey, double radius) {
        LivingEntity best = null;
        double bestSq = radius * radius;
        for (var entry : states.entrySet()) {
            if (!typeKey.equalsIgnoreCase(entry.getValue().typeKey)) continue;
            Entity e = Bukkit.getEntity(entry.getKey());
            if (!(e instanceof LivingEntity le) || !le.isValid()) continue;
            if (!le.getWorld().equals(origin.getWorld())) continue;
            double dSq = le.getLocation().distanceSquared(origin);
            if (dSq <= bestSq) {
                bestSq = dSq;
                best = le;
            }
        }
        return best;
    }

    /** Snapshot of currently registered managed mob count. */
    public int getManagedMobCount() {
        return states.size();
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
            case SPECIAL -> { /* special routine sets its own anim per phase */ }
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
            case "CAT", "OCELOT" -> "fluffy_cat";
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

    public enum State { IDLE, APPROACH, ATTACK, SPECIAL, COOLDOWN, FLEE }

    private static class FluffyMobState {
        State state = State.IDLE;
        int stateTicks = 0;
        UUID targetId = null;
        String typeKey = "default";
        String currentAnim = "";
        long lastSpecialTick = -1000L;
        String pendingSpecial = null;
        String specialPhase = null;
        int specialPhaseTick = 0;
        Player specialTarget = null;
    }
}
