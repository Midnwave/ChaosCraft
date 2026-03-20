package com.blockforge.chaoscraft.modes.corruption;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attack scheduler for Corrupted Corruption Mode.
 * Similar to ChainAttackScheduler but tailored for corruption-themed attacks.
 *
 * Key differences from Chain:
 * - Configurable world from CorruptionConfig (default: "world")
 * - Respects land claims if configured (placeholder — will wire up later)
 * - Only BLOCK_DISPLAY type attacks, all phase 1
 * - Integrated with corruption engine for coordinated environmental effects
 */
public class CorruptionAttackScheduler {

    private final ChaosCraftPlugin plugin;
    private final AttackRegistry registry;
    private final CorruptionConfig config;

    // Active attack instances
    private final List<AbstractAttack> activeAttacks = new ArrayList<>();

    // Per-player active event count
    private final Map<UUID, Integer> playerEventCounts = new ConcurrentHashMap<>();

    // Cooldown tracking per attack ID
    private final Map<String, Integer> attackCooldowns = new HashMap<>();

    // Spawn timer
    private int spawnTickCounter = 0;
    private int baseSpawnInterval;

    // State
    private boolean active = false;

    public CorruptionAttackScheduler(ChaosCraftPlugin plugin, AttackRegistry registry, CorruptionConfig config) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = config;
        this.baseSpawnInterval = config.getBaseSpawnInterval();
    }

    // ========================
    // Lifecycle
    // ========================

    public void start() {
        active = true;
        spawnTickCounter = 0;
        activeAttacks.clear();
        playerEventCounts.clear();
        attackCooldowns.clear();
        baseSpawnInterval = config.getBaseSpawnInterval();
    }

    public void stop() {
        active = false;
        for (AbstractAttack attack : new ArrayList<>(activeAttacks)) {
            attack.cleanup();
        }
        activeAttacks.clear();
        playerEventCounts.clear();
        attackCooldowns.clear();
    }

    /**
     * Called every server tick from CorruptionMode.onTick().
     */
    public void tick() {
        if (!active) return;

        tickActiveAttacks();
        tickCooldowns();

        spawnTickCounter++;
        if (spawnTickCounter >= baseSpawnInterval) {
            spawnTickCounter = 0;
            attemptSpawn();
        }
    }

    // ========================
    // Active attack management
    // ========================

    private void tickActiveAttacks() {
        Iterator<AbstractAttack> iter = activeAttacks.iterator();
        while (iter.hasNext()) {
            AbstractAttack attack = iter.next();
            if (!attack.isActive()) {
                decrementPlayerCount(attack.getTargetPlayer());
                iter.remove();
                continue;
            }
            attack.tick();
        }
    }

    private void tickCooldowns() {
        attackCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }

    // ========================
    // Spawning
    // ========================

    private void attemptSpawn() {
        World world = getCorruptionWorld();
        if (world == null) {
            plugin.debug("[CorruptionScheduler] Corruption world not found — cannot spawn attacks.");
            return;
        }

        List<Player> players = world.getPlayers();
        if (players.isEmpty()) {
            plugin.debug("[CorruptionScheduler] No players in corruption world — skipping spawn.");
            return;
        }

        // Pick a random target player from the corruption world
        Player target = players.get(new Random().nextInt(players.size()));

        // Check max events for this player
        int maxEvents = config.getMaxEventsPerPlayer();
        int currentEvents = playerEventCounts.getOrDefault(target.getUniqueId(), 0);
        if (currentEvents >= maxEvents) {
            plugin.debug("[CorruptionScheduler] " + target.getName() + " has max events (" + currentEvents + "/" + maxEvents + ") — skipping.");
            return;
        }

        // Check exempt
        if (isExempt(target)) {
            plugin.debug("[CorruptionScheduler] " + target.getName() + " is exempt — skipping.");
            return;
        }

        // Respect claims check (placeholder — will wire up with claim plugin API later)
        if (config.isRespectClaims() && isInProtectedClaim(target)) {
            plugin.debug("[CorruptionScheduler] " + target.getName() + " is in a protected claim — skipping.");
            return;
        }

        // Alternate between BLOCK_DISPLAY and ENVIRONMENTAL attacks
        AttackType type = Math.random() < 0.5 ? AttackType.BLOCK_DISPLAY : AttackType.ENVIRONMENTAL;
        AbstractAttack attack = registry.selectRandom(1, type);
        if (attack == null) {
            // Fallback to other type
            type = (type == AttackType.BLOCK_DISPLAY) ? AttackType.ENVIRONMENTAL : AttackType.BLOCK_DISPLAY;
            attack = registry.selectRandom(1, type);
        }
        if (attack == null) {
            plugin.debug("[CorruptionScheduler] No enabled attack found. Check attack configs.");
            return;
        }

        // Check cooldown
        if (attackCooldowns.containsKey(attack.getId())) {
            plugin.debug("[CorruptionScheduler] Attack " + attack.getId() + " on cooldown — skipping.");
            return;
        }

        // Spawn location near target
        Location spawnLoc = target.getLocation().clone();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        // Offset for non-tracking attacks
        if (!attack.getConfig().tracksPlayer()) {
            double offsetRadius = config.getSpawnOffsetRadius();
            double offsetX = (Math.random() * 2 - 1) * offsetRadius;
            double offsetZ = (Math.random() * 2 - 1) * offsetRadius;
            spawnLoc.add(offsetX, 0, offsetZ);
        }

        attack.spawn(spawnLoc, target);
        activeAttacks.add(attack);
        incrementPlayerCount(target);

        // Set cooldown
        attackCooldowns.put(attack.getId(), attack.getConfig().getCooldownTicks());

        plugin.debug("[CorruptionScheduler] Spawned " + attack.getId() + " near " + target.getName()
                + " (active: " + activeAttacks.size() + ")");
    }

    // ========================
    // Forced spawns (test commands)
    // ========================

    public void forceSpawn(AbstractAttack attack, Player player) {
        Location spawnLoc = player.getLocation().clone();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        AbstractAttack instance = attack.newInstance();
        instance.getConfig().copyFrom(attack.getConfig()); // Apply YAML config to spawned instance
        instance.spawn(spawnLoc, player);
        activeAttacks.add(instance);

        if (!active) {
            startTemporaryTicking(instance);
        }

        plugin.debug("[CorruptionScheduler] Force-spawned " + instance.getId() + " on " + player.getName()
                + " (scheduler active: " + active + ")");
    }

    private void startTemporaryTicking(AbstractAttack attack) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!attack.isActive()) {
                    activeAttacks.remove(attack);
                    cancel();
                    return;
                }
                attack.tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ========================
    // State management
    // ========================

    public void setBaseSpawnInterval(int ticks) {
        this.baseSpawnInterval = ticks;
    }

    public int getActiveAttackCount() {
        return activeAttacks.size();
    }

    public List<AbstractAttack> getActiveAttacks() {
        return Collections.unmodifiableList(activeAttacks);
    }

    public void clearActiveAttacks() {
        for (AbstractAttack attack : new ArrayList<>(activeAttacks)) {
            attack.cleanup();
        }
        activeAttacks.clear();
        playerEventCounts.clear();
    }

    public boolean isActive() {
        return active;
    }

    // ========================
    // Player event tracking
    // ========================

    private void incrementPlayerCount(Player player) {
        playerEventCounts.merge(player.getUniqueId(), 1, Integer::sum);
    }

    private void decrementPlayerCount(Player player) {
        if (player == null) return;
        playerEventCounts.computeIfPresent(player.getUniqueId(), (k, v) -> v > 1 ? v - 1 : null);
    }

    public int getPlayerEventCount(Player player) {
        return playerEventCounts.getOrDefault(player.getUniqueId(), 0);
    }

    // ========================
    // Helpers
    // ========================

    private boolean isExempt(Player player) {
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive()) {
            return modeManager.getActiveMode().isExempt(player);
        }
        return player.hasPermission("chaoscraft.mode.exempt");
    }

    /**
     * Get the Bukkit World where Corruption Mode is running.
     * Uses the world name from config, falls back to first loaded world.
     */
    private World getCorruptionWorld() {
        String worldName = config.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        // Default: first loaded world (Overworld)
        List<World> worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    /**
     * Placeholder claim protection check.
     * Will be wired up with the claim plugin API (e.g., GriefPrevention, Lands, etc.) later.
     *
     * @param player the player to check
     * @return true if the player is in a protected claim area where corruption should not spawn
     */
    private boolean isInProtectedClaim(Player player) {
        // TODO: Wire up with claim plugin API
        // Example integration points:
        //   - GriefPrevention: GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), ...)
        //   - Lands: LandsIntegration.of(plugin).getArea(player.getLocation())
        //   - WorldGuard: RegionQuery.testState(player.getLocation(), ...)
        return false;
    }
}
