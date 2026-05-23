package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.services.mobspawn.MobSpawnEntry;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chain Mode — Chain Attack gimmick system.
 *
 * <p>Every mob spawned by Chain mode (via the universal {@code MobSpawnService}
 * session) can fire a "chain attack" at a nearby player. The attack draws a
 * line of CHAIN BlockDisplays from the mob's head to the player ("leashing"),
 * then plays out a configurable effect (PULL / SWING / SLAM / LAUNCH / ANCHOR /
 * DAMAGE_ONLY).
 *
 * <p>Cooldown rules:
 * <ul>
 *   <li><b>Per-player global cooldown</b> — after a player is chained, no
 *       other mob can chain them for {@link ChainConfig#getChainAttackGlobalCooldownTicks()}
 *       ticks. Prevents chain-stun-lock.</li>
 *   <li><b>Per-mob cooldown</b> — a mob that fires its chain cannot fire again
 *       for {@link ChainConfig#getChainAttackPerMobCooldownTicks()} ticks.
 *       Prevents one mob from monopolizing a player.</li>
 * </ul>
 *
 * <p>Per-mob YAML overrides (read from {@code MobSpawnEntry#getChainAttack()})
 * trump the config defaults. Keys: {@code enabled, reach-radius, effect,
 * damage, effect-duration-ticks}.
 *
 * <p>Ticks every 4 server ticks. Each tick:
 * <ol>
 *   <li>Scan Chain-mode-spawned mobs (via {@code MobSpawnService.getActiveMobs("chain")}).</li>
 *   <li>For each mob with cooldowns clear, pick nearest non-exempt player in
 *       reach radius; on a successful proximity roll, fire chain.</li>
 *   <li>Advance every active chain — leash spawn / effect apply / retract.</li>
 * </ol>
 */
public class ChainAttackSystem {

    /** Scoreboard tag added to every chain BlockDisplay for cleanup safety net. */
    private static final String CHAIN_DISPLAY_TAG = "chaoscraft_chain_attack";

    /** Ticks the leash-attach (spawn-in) phase lasts. */
    private static final int LEASH_ATTACH_TICKS = 8;
    /** Ticks the retract phase lasts before despawning. */
    private static final int RETRACT_TICKS = 8;

    private final ChaosCraftPlugin plugin;
    private final ChainConfig config;

    // ── State (tick-clock-based cooldowns) ────────────────────────────
    /** tickClock value when a player's global cooldown expires. */
    private final Map<UUID, Long> playerGlobalCooldownExpiry = new ConcurrentHashMap<>();
    /** tickClock value when a mob's per-mob cooldown expires. */
    private final Map<UUID, Long> mobCooldownExpiry = new ConcurrentHashMap<>();
    /** Last time (ms) a player was chained — used for PAPI lastchain. */
    private final Map<UUID, Long> lastChainMillis = new ConcurrentHashMap<>();
    /** Currently active chain effects. */
    private final List<ActiveChain> activeChains = new ArrayList<>();
    /** Increments every 4 ticks (our scan period). */
    private long tickClock = 0L;
    /** Cumulative chains fired this session — debug/stats. */
    private long totalFired = 0L;

    private BukkitTask task;

    public ChainAttackSystem(ChaosCraftPlugin plugin, ChainConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ====================================================================
    // Lifecycle
    // ====================================================================

    /** Register a 4-tick BukkitRunnable that drives the chain system. */
    public void start() {
        if (task != null) return;
        task = new BukkitRunnable() {
            @Override public void run() { tick(); }
        }.runTaskTimer(plugin, 4L, 4L);
        plugin.getLogger().info("[Chain] ChainAttackSystem started (tick every 4t).");
    }

    /** Cancel the runnable and despawn every active chain BlockDisplay. */
    public void stop() {
        if (task != null) {
            try { task.cancel(); } catch (Throwable ignored) {}
            task = null;
        }
        for (ActiveChain c : new ArrayList<>(activeChains)) {
            despawnChainDisplays(c);
        }
        activeChains.clear();
        playerGlobalCooldownExpiry.clear();
        mobCooldownExpiry.clear();
        plugin.getLogger().info("[Chain] ChainAttackSystem stopped, " + totalFired + " chains fired this session.");
    }

    // ====================================================================
    // Public accessors (admin / PAPI hooks)
    // ====================================================================

    public int getActiveChainCount() { return activeChains.size(); }
    public long getTotalFired() { return totalFired; }

    /** Seconds remaining until {@code p} can be chained again. 0 if not on cooldown. */
    public int getGlobalCooldownRemaining(Player p) {
        Long expiry = playerGlobalCooldownExpiry.get(p.getUniqueId());
        if (expiry == null) return 0;
        long ticksRemaining = (expiry - tickClock) * 4L;
        return ticksRemaining <= 0 ? 0 : (int) Math.ceil(ticksRemaining / 20.0);
    }

    /** Debug/admin view of every active per-mob cooldown (mob UUID -> expiry tickClock). */
    public Map<UUID, Long> getPerMobCooldowns() {
        return Collections.unmodifiableMap(mobCooldownExpiry);
    }

    /** Snapshot of every active per-player global cooldown. */
    public Map<UUID, Long> getPlayerGlobalCooldowns() {
        return Collections.unmodifiableMap(playerGlobalCooldownExpiry);
    }

    /** Seconds since the player was last chained, or 0 if never. */
    public int getSecondsSinceLastChain(Player p) {
        Long lastMs = lastChainMillis.get(p.getUniqueId());
        if (lastMs == null) return 0;
        return (int) ((System.currentTimeMillis() - lastMs) / 1000L);
    }

    /**
     * Admin hook — fire a chain from {@code mob} at {@code target} bypassing
     * every cooldown check and the proximity roll. Returns false if the mob
     * is invalid or the world doesn't match.
     */
    public boolean forceFireChain(LivingEntity mob, Player target) {
        if (mob == null || target == null || !mob.isValid() || target.isDead()) return false;
        if (!mob.getWorld().equals(target.getWorld())) return false;
        MobSpawnEntry entry = plugin.getMobSpawnService() != null
                ? plugin.getMobSpawnService().getEntryFor(mob.getUniqueId())
                : null;
        fireChain(mob, target, entry);
        return true;
    }

    /** Admin reset — clear every cooldown for a player. */
    public void resetCooldowns(Player p) {
        playerGlobalCooldownExpiry.remove(p.getUniqueId());
    }

    // ====================================================================
    // Tick loop (every 4 server ticks)
    // ====================================================================

    private void tick() {
        tickClock++;

        // Drop expired cooldown entries to keep the maps small.
        playerGlobalCooldownExpiry.values().removeIf(v -> v <= tickClock);
        mobCooldownExpiry.values().removeIf(v -> v <= tickClock);

        // ── 1) Maybe fire new chains ────────────────────────────────
        scanAndFire();

        // ── 2) Advance active chains ────────────────────────────────
        Iterator<ActiveChain> it = activeChains.iterator();
        while (it.hasNext()) {
            ActiveChain c = it.next();
            if (!advance(c)) {
                despawnChainDisplays(c);
                it.remove();
            }
        }
    }

    private void scanAndFire() {
        if (plugin.getMobSpawnService() == null) return;
        Set<UUID> mobs = plugin.getMobSpawnService().getActiveMobs("chain");
        if (mobs.isEmpty()) return;

        // Locate the active chain mode (for exempt-player checks).
        AbstractMode modeObj = plugin.getModeManager().getMode("chain");
        if (!(modeObj instanceof ChainMode chainMode)) return;
        World chainWorld = chainMode.getChainWorld();
        if (chainWorld == null) return;

        double defaultReach = config.getChainAttackDefaultReachRadius();
        double rollChance = config.getChainProximityRollChance();
        int globalCdTicks = config.getChainAttackGlobalCooldownTicks();
        int perMobCdTicks = config.getChainAttackPerMobCooldownTicks();

        for (UUID mobId : mobs) {
            Long mobExpiry = mobCooldownExpiry.get(mobId);
            if (mobExpiry != null && mobExpiry > tickClock) continue;

            Entity e = Bukkit.getEntity(mobId);
            if (!(e instanceof LivingEntity mob) || !e.isValid() || e.isDead()) continue;
            if (!e.getWorld().equals(chainWorld)) continue;

            MobSpawnEntry entry = plugin.getMobSpawnService().getEntryFor(mobId);

            // Per-mob enabled gate: missing override -> system default; explicit false -> skip.
            if (entry != null) {
                Object enabledOverride = entry.getChainAttack().get("enabled");
                if (enabledOverride instanceof Boolean b && !b) continue;
            }

            double reach = entry != null
                    ? toDouble(entry.getChainAttack().get("reach-radius"), defaultReach)
                    : defaultReach;

            Player target = nearestEligiblePlayer(mob, chainMode, reach);
            if (target == null) continue;

            Long playerExpiry = playerGlobalCooldownExpiry.get(target.getUniqueId());
            if (playerExpiry != null && playerExpiry > tickClock) continue;

            // Proximity roll
            if (ThreadLocalRandom.current().nextDouble() >= rollChance) continue;

            fireChain(mob, target, entry);

            // Apply cooldowns: convert "ticks" config value into our 4t units.
            long globalUnits = Math.max(1L, globalCdTicks / 4L);
            long mobUnits = Math.max(1L, perMobCdTicks / 4L);
            playerGlobalCooldownExpiry.put(target.getUniqueId(), tickClock + globalUnits);
            mobCooldownExpiry.put(mobId, tickClock + mobUnits);
        }
    }

    private Player nearestEligiblePlayer(LivingEntity mob, ChainMode chainMode, double reach) {
        double bestSq = reach * reach;
        Player best = null;
        for (Player p : mob.getWorld().getPlayers()) {
            if (p.isDead() || !p.isOnline()) continue;
            if (chainMode.isExempt(p)) continue;
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            double d2 = p.getLocation().distanceSquared(mob.getLocation());
            if (d2 <= bestSq) { bestSq = d2; best = p; }
        }
        return best;
    }

    // ====================================================================
    // Fire a chain
    // ====================================================================

    private void fireChain(LivingEntity mob, Player target, MobSpawnEntry entry) {
        // Resolve effective parameters (per-mob override > config default).
        String effectName = config.getChainAttackDefaultEffect();
        double damage = config.getChainAttackDefaultDamage();
        int duration = config.getChainAttackDefaultEffectDurationTicks();
        if (entry != null) {
            Map<String, Object> ov = entry.getChainAttack();
            Object eff = ov.get("effect");
            if (eff != null) effectName = String.valueOf(eff).toUpperCase(Locale.ROOT);
            damage = toDouble(ov.get("damage"), damage);
            duration = (int) toDouble(ov.get("effect-duration-ticks"), duration);
        }
        ChainEffect effect = ChainEffect.fromString(effectName);

        ActiveChain ac = new ActiveChain();
        ac.mobId = mob.getUniqueId();
        ac.playerId = target.getUniqueId();
        ac.startPos = mob.getLocation().clone().add(0, mob.getHeight() * 0.8, 0); // head-ish
        ac.endPos = target.getLocation().clone().add(0, 1.0, 0);                  // chest
        ac.duration = Math.max(LEASH_ATTACH_TICKS + RETRACT_TICKS + 1, duration);
        ac.age = 0;
        ac.effect = effect;
        ac.damage = damage;
        ac.damageApplied = false;
        ac.launchApplied = false;
        ac.swingAngle = 0.0;

        spawnChainDisplays(ac, mob, target);

        // Sound cues — chain place at mob, chain hit at player.
        try {
            mob.getWorld().playSound(mob.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.8f);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 1.0f);
        } catch (Throwable ignored) {}

        activeChains.add(ac);
        totalFired++;
        lastChainMillis.put(target.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Spawn the line of CHAIN BlockDisplays and the IRON_BARS hook cap.
     * Links are spawned all at once but each is interpolation-scaled in over
     * LEASH_ATTACH_TICKS so the leash appears to extend out of the mob.
     */
    private void spawnChainDisplays(ActiveChain ac, LivingEntity mob, Player target) {
        World world = mob.getWorld();
        double spacing = Math.max(0.25, config.getChainLinkSpacing());
        double scale = Math.max(0.05, config.getChainLinkScale());

        Vector delta = ac.endPos.clone().subtract(ac.startPos).toVector();
        double distance = delta.length();
        int links = Math.max(2, (int) Math.floor(distance / spacing));
        ac.linkCount = links;

        float yawRad = (float) Math.atan2(-delta.getX(), delta.getZ());
        float pitchRad = (float) Math.atan2(delta.getY(), Math.hypot(delta.getX(), delta.getZ()));

        for (int i = 0; i < links; i++) {
            double t = (double) i / (double) Math.max(1, links - 1);
            Location loc = ac.startPos.clone().add(delta.clone().multiply(t));
            loc.setYaw(0); loc.setPitch(0);

            BlockDisplay bd = world.spawn(loc, BlockDisplay.class, d -> {
                d.setBlock(Material.CHAIN.createBlockData());
                d.setBrightness(new Display.Brightness(15, 15));
                d.addScoreboardTag("chaoscraft_display");
                d.addScoreboardTag(CHAIN_DISPLAY_TAG);
                d.addScoreboardTag("cc:chain");
                d.setGlowing(true);
                d.setGlowColorOverride(Color.fromRGB(190, 100, 40)); // orange-rust
                d.setInterpolationDuration(LEASH_ATTACH_TICKS);
                d.setInterpolationDelay(0);
                d.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        rotationFor(yawRad, pitchRad),
                        new Vector3f(0.05f, 0.05f, 0.05f), // start small, grows in
                        new AxisAngle4f(0, 0, 1, 0)));
            });
            // Apply target transformation (full scale) after spawn to drive interp.
            applyLinkTransform(bd, yawRad, pitchRad, (float) scale, (float) spacing);
            ac.chainLinks.add(bd);
        }

        // Hook cap (IRON_BARS) at player end
        Location capLoc = ac.endPos.clone();
        capLoc.setYaw(0); capLoc.setPitch(0);
        BlockDisplay cap = world.spawn(capLoc, BlockDisplay.class, d -> {
            d.setBlock(Material.IRON_BARS.createBlockData());
            d.setBrightness(new Display.Brightness(15, 15));
            d.addScoreboardTag("chaoscraft_display");
            d.addScoreboardTag(CHAIN_DISPLAY_TAG);
            d.addScoreboardTag("cc:chain");
            d.setGlowing(true);
            d.setGlowColorOverride(Color.fromRGB(220, 130, 50));
            d.setInterpolationDuration(LEASH_ATTACH_TICKS);
            d.setInterpolationDelay(0);
            d.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.05f, 0.05f, 0.05f),
                    new AxisAngle4f(0, 0, 1, 0)));
        });
        applyCapTransform(cap, (float) (scale * 1.4f));
        ac.hookCap = cap;
    }

    private static AxisAngle4f rotationFor(float yawRad, float pitchRad) {
        // We rotate around Y (yaw) then X (pitch); approximate with a single
        // axis-angle by combining vectors. Good enough for a chain visual.
        // Use yaw as primary axis; chain is symmetric, so pitch comes via
        // translation-driven follow on each subsequent retarget.
        return new AxisAngle4f(yawRad, 0, 1, 0);
    }

    private static void applyLinkTransform(BlockDisplay d, float yawRad, float pitchRad,
                                           float xz, float yLen) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(LEASH_ATTACH_TICKS);
        Transformation t = d.getTransformation();
        d.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(yawRad, 0, 1, 0),
                new Vector3f(xz, xz, yLen),
                new AxisAngle4f(pitchRad, 1, 0, 0)));
    }

    private static void applyCapTransform(BlockDisplay d, float scale) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(LEASH_ATTACH_TICKS);
        Transformation t = d.getTransformation();
        d.setTransformation(new Transformation(
                t.getTranslation(),
                t.getLeftRotation(),
                new Vector3f(scale, scale, scale),
                t.getRightRotation()));
    }

    private void despawnChainDisplays(ActiveChain ac) {
        for (BlockDisplay bd : ac.chainLinks) {
            if (bd != null && bd.isValid()) bd.remove();
        }
        ac.chainLinks.clear();
        if (ac.hookCap != null && ac.hookCap.isValid()) ac.hookCap.remove();
        ac.hookCap = null;
    }

    // ====================================================================
    // Per-tick advance for an active chain
    // ====================================================================

    /** Returns false to signal the chain is finished (caller despawns). */
    private boolean advance(ActiveChain ac) {
        ac.age += 4; // tick step

        Entity mobE = Bukkit.getEntity(ac.mobId);
        Player player = Bukkit.getPlayer(ac.playerId);

        // Bail if either side is gone — clean up.
        if (player == null || player.isDead() || !player.isOnline()
                || mobE == null || !mobE.isValid() || mobE.isDead()) {
            return false;
        }
        LivingEntity mob = (mobE instanceof LivingEntity le) ? le : null;
        if (mob == null) return false;

        // Always re-target the visual to match current positions.
        ac.startPos = mob.getLocation().clone().add(0, mob.getHeight() * 0.8, 0);
        ac.endPos = player.getLocation().clone().add(0, 1.0, 0);
        updateChainVisual(ac);

        // Phase determination
        int leashEnd = LEASH_ATTACH_TICKS;
        int retractStart = ac.duration - RETRACT_TICKS;

        if (ac.age <= leashEnd) {
            // Leash-attach phase — visual is interpolating in, no effect yet.
            return true;
        }

        if (ac.age >= ac.duration) {
            return false; // done
        }

        if (ac.age >= retractStart) {
            // Retract phase — shrink links to 0
            shrinkAll(ac);
            return true;
        }

        // ── Effect phase ────────────────────────────────────────────
        // Apply one-shot damage on first effect tick
        if (!ac.damageApplied) {
            try { player.damage(ac.damage, mob); } catch (Throwable ignored) {}
            ac.damageApplied = true;
        }

        applyEffect(ac, mob, player);
        return true;
    }

    private void updateChainVisual(ActiveChain ac) {
        if (ac.chainLinks.isEmpty()) return;
        Vector delta = ac.endPos.clone().subtract(ac.startPos).toVector();
        float yawRad = (float) Math.atan2(-delta.getX(), delta.getZ());
        float pitchRad = (float) Math.atan2(delta.getY(), Math.hypot(delta.getX(), delta.getZ()));
        double spacing = Math.max(0.25, config.getChainLinkSpacing());

        int n = ac.chainLinks.size();
        for (int i = 0; i < n; i++) {
            BlockDisplay bd = ac.chainLinks.get(i);
            if (bd == null || !bd.isValid()) continue;
            double t = (double) i / (double) Math.max(1, n - 1);
            Location loc = ac.startPos.clone().add(delta.clone().multiply(t));
            loc.setYaw(0); loc.setPitch(0);
            bd.setInterpolationDelay(0);
            bd.setInterpolationDuration(2);
            bd.teleport(loc);
            Transformation tr = bd.getTransformation();
            bd.setTransformation(new Transformation(
                    tr.getTranslation(),
                    new AxisAngle4f(yawRad, 0, 1, 0),
                    new Vector3f(tr.getScale().x, tr.getScale().y, (float) spacing),
                    new AxisAngle4f(pitchRad, 1, 0, 0)));
        }
        if (ac.hookCap != null && ac.hookCap.isValid()) {
            Location capLoc = ac.endPos.clone();
            capLoc.setYaw(0); capLoc.setPitch(0);
            ac.hookCap.setInterpolationDelay(0);
            ac.hookCap.setInterpolationDuration(2);
            ac.hookCap.teleport(capLoc);
        }
    }

    private void shrinkAll(ActiveChain ac) {
        for (BlockDisplay bd : ac.chainLinks) {
            if (bd == null || !bd.isValid()) continue;
            Transformation tr = bd.getTransformation();
            bd.setInterpolationDelay(0);
            bd.setInterpolationDuration(RETRACT_TICKS);
            bd.setTransformation(new Transformation(
                    tr.getTranslation(),
                    tr.getLeftRotation(),
                    new Vector3f(0.01f, 0.01f, 0.01f),
                    tr.getRightRotation()));
        }
        if (ac.hookCap != null && ac.hookCap.isValid()) {
            Transformation tr = ac.hookCap.getTransformation();
            ac.hookCap.setInterpolationDelay(0);
            ac.hookCap.setInterpolationDuration(RETRACT_TICKS);
            ac.hookCap.setTransformation(new Transformation(
                    tr.getTranslation(),
                    tr.getLeftRotation(),
                    new Vector3f(0.01f, 0.01f, 0.01f),
                    tr.getRightRotation()));
        }
    }

    private void applyEffect(ActiveChain ac, LivingEntity mob, Player player) {
        switch (ac.effect) {
            case PULL -> {
                Vector dir = mob.getLocation().toVector().subtract(player.getLocation().toVector());
                if (dir.lengthSquared() > 0.0001) {
                    dir.normalize().multiply(config.getChainPullStrength());
                    // Add a tiny upward bias so the player isn't ground-stuck.
                    dir.setY(Math.max(dir.getY(), 0.1));
                    player.setVelocity(dir);
                }
            }
            case SWING -> {
                ac.swingAngle += config.getChainSwingRateRadPerTick();
                double r = config.getChainSwingRadius();
                Location anchor = mob.getLocation();
                double x = anchor.getX() + Math.cos(ac.swingAngle) * r;
                double z = anchor.getZ() + Math.sin(ac.swingAngle) * r;
                Location orbit = new Location(player.getWorld(), x,
                        player.getLocation().getY(), z,
                        player.getLocation().getYaw(), player.getLocation().getPitch());
                try { player.teleport(orbit); } catch (Throwable ignored) {}
            }
            case SLAM -> {
                int half = (ac.duration - LEASH_ATTACH_TICKS - RETRACT_TICKS) / 2;
                int relAge = ac.age - LEASH_ATTACH_TICKS;
                if (relAge < half) {
                    Vector dir = mob.getLocation().toVector().subtract(player.getLocation().toVector());
                    if (dir.lengthSquared() > 0.0001) {
                        dir.normalize().multiply(config.getChainPullStrength());
                        dir.setY(Math.max(dir.getY(), 0.4)); // upward arc
                        player.setVelocity(dir);
                    }
                } else {
                    Vector v = player.getVelocity();
                    v.setY(config.getChainSlamVelocityY());
                    player.setVelocity(v);
                }
            }
            case LAUNCH -> {
                if (!ac.launchApplied) {
                    Vector dir = player.getLocation().toVector().subtract(mob.getLocation().toVector());
                    if (dir.lengthSquared() > 0.0001) {
                        dir.normalize().multiply(config.getChainLaunchStrength());
                        dir.setY(Math.max(dir.getY(), 0.6));
                        player.setVelocity(dir);
                    }
                    ac.launchApplied = true;
                }
            }
            case ANCHOR -> player.setVelocity(new Vector(0, 0, 0));
            case DAMAGE_ONLY -> { /* damage was already applied; nothing else */ }
        }
    }

    // ====================================================================
    // Helpers
    // ====================================================================

    private static double toDouble(Object o, double fallback) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return fallback; }
        }
        return fallback;
    }

    // ====================================================================
    // Inner types
    // ====================================================================

    /** Six configurable chain effects. */
    public enum ChainEffect {
        PULL, SWING, SLAM, LAUNCH, ANCHOR, DAMAGE_ONLY;

        public static ChainEffect fromString(String s) {
            if (s == null) return PULL;
            try { return ChainEffect.valueOf(s.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { return PULL; }
        }
    }

    /** Snapshot of one in-flight chain. */
    public static class ActiveChain {
        public UUID mobId;
        public UUID playerId;
        public Location startPos;   // cached mob head position
        public Location endPos;     // cached player chest position
        public int duration;        // total ticks the chain is alive
        public int age;             // ticks since fire
        public ChainEffect effect;
        public double damage;
        public boolean damageApplied;
        public boolean launchApplied;
        public double swingAngle;
        public int linkCount;
        public final List<BlockDisplay> chainLinks = new ArrayList<>();
        public BlockDisplay hookCap;
    }
}
