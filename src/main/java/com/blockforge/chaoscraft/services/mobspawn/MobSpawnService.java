package com.blockforge.chaoscraft.services.mobspawn;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal mob spawning service used by all ChaosCraft game modes.
 *
 * <p>Each mode creates a {@link MobSpawnSession} via {@link #createSession(String, MobSpawnConfig, World)}
 * when it starts, ticks it every server tick, and destroys it when the mode ends.
 *
 * <p>Supports both MythicMobs (via reflection — no hard dependency) and vanilla mobs.
 * Applies health/damage multipliers, respects per-player and total mob caps,
 * and cleans up all spawned entities on session end.
 *
 * <p>Key features:
 * <ul>
 *   <li>Weighted random mob selection from the config's mob list</li>
 *   <li>Per-player mob cap (within 30 blocks)</li>
 *   <li>Global mob cap per mode session</li>
 *   <li>Surface / player-level / random Y placement</li>
 *   <li>Automatic health &amp; damage attribute scaling</li>
 *   <li>Full cleanup on session end (removes all spawned entities)</li>
 *   <li>Death event tracking (removes from internal tracking when mobs die)</li>
 * </ul>
 */
public class MobSpawnService implements Listener {

    private final ChaosCraftPlugin plugin;

    /** Active sessions keyed by mode name. */
    private final Map<String, MobSpawnSession> sessions = new ConcurrentHashMap<>();

    /** All entity UUIDs spawned by this service, mapped to their session's mode name. */
    private final Map<UUID, String> entityToSession = new ConcurrentHashMap<>();

    /**
     * All entity UUIDs spawned by this service, mapped to the {@link MobSpawnEntry}
     * that produced them. Used by mode-specific gimmicks (e.g. ChainAttackSystem)
     * to resolve per-mob overrides like reach radius / chain effect from the YAML.
     */
    private final Map<UUID, MobSpawnEntry> entityToEntry = new ConcurrentHashMap<>();

    private boolean mythicMobsAvailable = false;

    public MobSpawnService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.mythicMobsAvailable = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[MobSpawn] Service initialized. MythicMobs: "
                + (mythicMobsAvailable ? "AVAILABLE" : "NOT FOUND (vanilla mobs only)"));
    }

    // ========================
    // Session management
    // ========================

    /**
     * Create a new mob spawning session for a mode.
     * Call this when the mode starts. Only one session per mode name at a time.
     *
     * @param modeName the unique mode name (e.g. "freezingice", "bluemoon")
     * @param config   the mob spawn config read from the mode's YAML
     * @param world    the world to spawn mobs in
     * @return the created session, or null if mob spawning is disabled
     */
    public MobSpawnSession createSession(String modeName, MobSpawnConfig config, World world) {
        if (!config.isEnabled() || config.getMobs().isEmpty()) {
            plugin.debug("[MobSpawn] Session '" + modeName + "' not created — disabled or no mobs configured.");
            return null;
        }

        // Clean up any existing session for this mode
        destroySession(modeName);

        MobSpawnSession session = new MobSpawnSession(modeName, config, world);
        sessions.put(modeName, session);
        plugin.debug("[MobSpawn] Session '" + modeName + "' created with " + config.getMobs().size()
                + " mob types, interval=" + config.getSpawnIntervalTicks() + "t, max=" + config.getMaxTotalMobs());
        return session;
    }

    /**
     * Destroy a session and optionally clean up all its mobs.
     * Call this when the mode ends.
     */
    public void destroySession(String modeName) {
        MobSpawnSession session = sessions.remove(modeName);
        if (session == null) return;

        if (session.config.isCleanupOnEnd()) {
            int removed = 0;
            for (UUID uuid : new ArrayList<>(session.activeMobs)) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                    removed++;
                }
                entityToSession.remove(uuid);
                entityToEntry.remove(uuid);
            }
            plugin.debug("[MobSpawn] Session '" + modeName + "' destroyed, removed " + removed + " mobs.");
        } else {
            // Just untrack, don't remove entities
            for (UUID uuid : session.activeMobs) {
                entityToSession.remove(uuid);
                entityToEntry.remove(uuid);
            }
            plugin.debug("[MobSpawn] Session '" + modeName + "' destroyed (mobs left alive).");
        }
        session.activeMobs.clear();
    }

    /**
     * Destroy all active sessions. Called on plugin disable.
     */
    public void shutdown() {
        for (String modeName : new ArrayList<>(sessions.keySet())) {
            destroySession(modeName);
        }
    }

    /**
     * Get a session by mode name, or null if none exists.
     */
    public MobSpawnSession getSession(String modeName) {
        return sessions.get(modeName);
    }

    // ========================
    // Tick — call from mode's onTick()
    // ========================

    /**
     * Tick a specific session. Call this from the mode's onTick() method.
     * Handles spawn timing, mob selection, and dead mob cleanup.
     *
     * @param modeName      the mode name
     * @param exemptPlayers set of exempt player UUIDs (won't be targeted for spawning near)
     */
    public void tick(String modeName, Set<UUID> exemptPlayers) {
        MobSpawnSession session = sessions.get(modeName);
        if (session == null) return;

        // Clean up dead/removed mobs
        session.activeMobs.removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity == null || entity.isDead() || !entity.isValid()) {
                entityToSession.remove(uuid);
                entityToEntry.remove(uuid);
                return true;
            }
            return false;
        });

        // Tick spawn counter
        session.spawnCounter++;
        if (session.spawnCounter < session.config.getSpawnIntervalTicks()) return;
        session.spawnCounter = 0;

        // Check global cap
        if (session.activeMobs.size() >= session.config.getMaxTotalMobs()) return;

        // Get valid target players
        List<Player> validPlayers = new ArrayList<>();
        for (Player p : session.world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (p.isInvulnerable()) continue;
            if (exemptPlayers.contains(p.getUniqueId())) continue;
            if (p.hasPermission("chaoscraft.mode.exempt")) continue;
            validPlayers.add(p);
        }
        if (validPlayers.isEmpty()) return;

        // Pick random target
        Player target = validPlayers.get(new Random().nextInt(validPlayers.size()));

        // Check per-player cap
        int nearbyCount = countMobsNear(session, target.getLocation(), 30.0);
        if (nearbyCount >= session.config.getMaxMobsPerPlayer()) return;

        // Select mob entry by weighted random
        MobSpawnEntry entry = selectWeightedRandom(session.config);
        if (entry == null) return;

        // Determine count
        int count = entry.getMinCount();
        if (entry.getMaxCount() > entry.getMinCount()) {
            count += new Random().nextInt(entry.getMaxCount() - entry.getMinCount() + 1);
        }

        // Spawn each mob
        for (int i = 0; i < count; i++) {
            // Check cap again mid-wave
            if (session.activeMobs.size() >= session.config.getMaxTotalMobs()) break;

            Location spawnLoc = calculateSpawnLocation(session, target);
            if (spawnLoc == null) continue;

            Entity spawned = spawnMob(entry, spawnLoc);
            if (spawned != null) {
                session.activeMobs.add(spawned.getUniqueId());
                entityToSession.put(spawned.getUniqueId(), modeName);
                entityToEntry.put(spawned.getUniqueId(), entry);
                applyMultipliers(spawned, entry);

                plugin.debug("[MobSpawn] " + modeName + ": Spawned " + entry.getId()
                        + " near " + target.getName() + " (total: " + session.activeMobs.size() + ")");
            }
        }
    }

    // ========================
    // Death tracking
    // ========================

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        String modeName = entityToSession.remove(uuid);
        entityToEntry.remove(uuid);
        if (modeName != null) {
            MobSpawnSession session = sessions.get(modeName);
            if (session != null) {
                session.activeMobs.remove(uuid);
            }
        }
    }

    // ========================
    // Spawning logic
    // ========================

    /**
     * Public spawn entry point for callers outside the universal session system
     * (e.g. FluffyRainSpawner). Branches on entry type and spawns accordingly.
     * Returns the spawned Bukkit entity, or null on failure.
     */
    public Entity spawnFromEntry(MobSpawnEntry entry, Location location) {
        return spawnMob(entry, location);
    }

    private Entity spawnMob(MobSpawnEntry entry, Location location) {
        switch (entry.getType()) {
            case MYTHICMOBS -> {
                return spawnMythicMob(entry.getId(), location);
            }
            case VANILLA -> {
                return spawnVanillaMob(entry, location);
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * Spawn a MythicMobs mob via reflection (soft dependency).
     * Returns the spawned Bukkit Entity, or null if MythicMobs is unavailable or spawn failed.
     */
    private Entity spawnMythicMob(String mythicId, Location location) {
        if (!mythicMobsAvailable) {
            plugin.getLogger().warning("[MobSpawn] Cannot spawn MythicMob '" + mythicId
                    + "' — MythicMobs plugin not installed.");
            return null;
        }

        try {
            // MythicMobs 5.x API via reflection to avoid hard dependency
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method instMethod = mythicBukkitClass.getMethod("inst");
            Object mythicInst = instMethod.invoke(null);
            Object mobManager = mythicInst.getClass().getMethod("getMobManager").invoke(mythicInst);
            Method spawnMethod = mobManager.getClass().getMethod("spawnMob", String.class, Location.class);
            Object activeMob = spawnMethod.invoke(mobManager, mythicId, location);

            if (activeMob == null) {
                plugin.getLogger().warning("[MobSpawn] MythicMobs returned null for mob ID '" + mythicId
                        + "'. Check that this ID exists in your MythicMobs/Mobs/ folder.");
                return null;
            }

            // Get the Bukkit entity from the ActiveMob
            Object abstractEntity = activeMob.getClass().getMethod("getEntity").invoke(activeMob);
            Object bukkitEntity = abstractEntity.getClass().getMethod("getBukkitEntity").invoke(abstractEntity);
            if (bukkitEntity instanceof Entity entity) {
                return entity;
            }
            return null;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("[MobSpawn] MythicMobs API class not found. Requires MythicMobs 5.x+.");
            mythicMobsAvailable = false; // Don't keep retrying
            return null;
        } catch (Exception e) {
            plugin.debug("[MobSpawn] Failed to spawn MythicMob '" + mythicId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Spawn a vanilla Minecraft mob using the EntityType from the entry's id.
     */
    private Entity spawnVanillaMob(MobSpawnEntry entry, Location location) {
        EntityType entityType = entry.resolveEntityType();
        if (entityType == null || !entityType.isAlive()) {
            plugin.getLogger().warning("[MobSpawn] Invalid vanilla mob type: '" + entry.getId()
                    + "'. Must be a valid living EntityType (e.g. ZOMBIE, SKELETON, PHANTOM).");
            return null;
        }

        try {
            Entity entity = location.getWorld().spawnEntity(location, entityType);
            if (entity instanceof Mob mob) {
                // Prevent natural despawning so our cleanup handles it
                mob.setRemoveWhenFarAway(false);
                // Prevent picking up items (visual clutter)
                if (mob instanceof Monster monster) {
                    monster.setCanPickupItems(false);
                }
            }
            return entity;
        } catch (Exception e) {
            plugin.debug("[MobSpawn] Failed to spawn vanilla mob " + entry.getId() + ": " + e.getMessage());
            return null;
        }
    }

    // ========================
    // Attribute scaling
    // ========================

    /**
     * Apply health and damage multipliers to a spawned entity.
     * Modifies the entity's MAX_HEALTH and ATTACK_DAMAGE attributes.
     */
    private void applyMultipliers(Entity entity, MobSpawnEntry entry) {
        if (!(entity instanceof LivingEntity living)) return;

        // Health multiplier
        if (entry.getHealthMultiplier() != 1.0) {
            var healthAttr = living.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) {
                double newMax = healthAttr.getBaseValue() * entry.getHealthMultiplier();
                healthAttr.setBaseValue(newMax);
                living.setHealth(newMax); // Heal to new max
            }
        }

        // Damage multiplier
        if (entry.getDamageMultiplier() != 1.0) {
            var damageAttr = living.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damageAttr != null) {
                damageAttr.setBaseValue(damageAttr.getBaseValue() * entry.getDamageMultiplier());
            }
        }
    }

    // ========================
    // Location calculation
    // ========================

    /**
     * Calculate where to spawn a mob relative to the target player.
     * Uses the session's spawn-distance and spawn-y-mode settings.
     */
    private Location calculateSpawnLocation(MobSpawnSession session, Player target) {
        double dist = session.config.getSpawnDistance();
        double angle = Math.random() * Math.PI * 2;

        Location loc = target.getLocation().clone().add(
                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

        World world = session.world;
        String yMode = session.config.getSpawnYMode().toLowerCase();

        switch (yMode) {
            case "player" -> loc.setY(target.getLocation().getY());
            case "random" -> loc.setY(target.getLocation().getY() - 5 + new Random().nextInt(16));
            default -> {
                // "surface" — find highest solid block
                int highestY = world.getHighestBlockYAt(loc);
                if (highestY < world.getMinHeight()) return null; // Void
                loc.setY(highestY + 1);
            }
        }

        // Safety: don't spawn in void
        if (loc.getY() < world.getMinHeight() + 1) return null;

        return loc;
    }

    // ========================
    // Helpers
    // ========================

    /**
     * Count how many tracked mobs from a session are within radius of a location.
     */
    private int countMobsNear(MobSpawnSession session, Location center, double radius) {
        double radiusSq = radius * radius;
        int count = 0;
        for (UUID uuid : session.activeMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()
                    && entity.getWorld().equals(center.getWorld())
                    && entity.getLocation().distanceSquared(center) <= radiusSq) {
                count++;
            }
        }
        return count;
    }

    /**
     * Select a random MobSpawnEntry from the config using weighted random selection.
     * Mobs with higher weight are proportionally more likely to be chosen.
     */
    private MobSpawnEntry selectWeightedRandom(MobSpawnConfig config) {
        int totalWeight = config.getTotalWeight();
        if (totalWeight <= 0) return null;

        int roll = new Random().nextInt(totalWeight);
        int cumulative = 0;
        for (MobSpawnEntry entry : config.getMobs()) {
            cumulative += entry.getWeight();
            if (roll < cumulative) return entry;
        }
        return config.getMobs().get(config.getMobs().size() - 1); // Fallback
    }

    /** Whether MythicMobs is installed and available. */
    public boolean isMythicMobsAvailable() { return mythicMobsAvailable; }

    /**
     * Get the total number of active mobs across all sessions.
     */
    public int getTotalActiveMobs() {
        return entityToSession.size();
    }

    /**
     * Get the number of active mobs for a specific mode session.
     */
    public int getActiveMobCount(String modeName) {
        MobSpawnSession session = sessions.get(modeName);
        return session != null ? session.activeMobs.size() : 0;
    }

    /**
     * Get a snapshot view of the active mob UUIDs for a mode session.
     * Returns an empty set if no session exists.
     *
     * <p>Used by gimmick subsystems (e.g. ChainAttackSystem) to iterate the
     * mobs the universal spawner has placed for a given mode and apply
     * mode-specific behavior to them.
     */
    public Set<UUID> getActiveMobs(String modeName) {
        MobSpawnSession session = sessions.get(modeName);
        if (session == null) return Collections.emptySet();
        return new HashSet<>(session.activeMobs);
    }

    /**
     * Look up the {@link MobSpawnEntry} (config row) used to spawn a tracked
     * entity. Returns null if the entity wasn't spawned by this service, or
     * if the entry that produced it can no longer be resolved (e.g. mob list
     * was reloaded mid-session).
     */
    public MobSpawnEntry getEntryFor(UUID entityId) {
        return entityToEntry.get(entityId);
    }

    // ========================
    // Session data class
    // ========================

    /**
     * Tracks the state of mob spawning for a single active mode.
     * Each mode gets its own session with its own mob list, counters, and caps.
     */
    public static class MobSpawnSession {
        public final String modeName;
        public final MobSpawnConfig config;
        public final World world;
        public final List<UUID> activeMobs = new ArrayList<>();
        public int spawnCounter = 0;

        MobSpawnSession(String modeName, MobSpawnConfig config, World world) {
            this.modeName = modeName;
            this.config = config;
            this.world = world;
        }
    }
}
