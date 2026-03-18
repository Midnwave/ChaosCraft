package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active attacks during Calamity mode.
 *
 * Responsibilities:
 * - Periodically selects and spawns random attacks from the registry
 * - Enforces max events per player
 * - Ticks all active attacks every server tick
 * - Cleans up expired attacks
 * - Handles forced spawns from test commands
 * - Tracks cooldowns to prevent rapid re-spawning of same attacks
 */
public class AttackScheduler {

    private final ChaosCraftPlugin plugin;
    private final AttackRegistry registry;
    private final CalamityConfig config;

    // Active attack instances
    private final List<AbstractAttack> activeAttacks = new ArrayList<>();

    // Per-player active event count (player UUID → count)
    private final Map<UUID, Integer> playerEventCounts = new ConcurrentHashMap<>();

    // Cooldown tracking per attack ID (attack ID → remaining cooldown ticks)
    private final Map<String, Integer> attackCooldowns = new HashMap<>();

    // Spawn timer
    private int spawnTickCounter = 0;
    private int baseSpawnInterval = 60; // Ticks between spawn attempts (configurable)

    // State
    private boolean active = false;
    private int currentPhase = 1;
    private boolean bossActive = false;
    private String currentBossName = null;

    public AttackScheduler(ChaosCraftPlugin plugin, AttackRegistry registry, CalamityConfig config) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = config;
    }

    // ========================
    // Lifecycle
    // ========================

    public void start(int phase) {
        active = true;
        currentPhase = phase;
        spawnTickCounter = 0;
        activeAttacks.clear();
        playerEventCounts.clear();
        attackCooldowns.clear();
    }

    public void stop() {
        active = false;
        // Cleanup all active attacks
        for (AbstractAttack attack : new ArrayList<>(activeAttacks)) {
            attack.cleanup();
        }
        activeAttacks.clear();
        playerEventCounts.clear();
        attackCooldowns.clear();
    }

    /**
     * Called every server tick from CalamityMode.onTick().
     */
    public void tick() {
        if (!active) return;

        // Tick all active attacks
        tickActiveAttacks();

        // Tick cooldowns
        tickCooldowns();

        // Attempt to spawn new attacks
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
                // Attack expired — clean up and remove
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
        World endWorld = getEndWorld();
        if (endWorld == null) return;

        List<Player> players = endWorld.getPlayers();
        if (players.isEmpty()) return;

        // Pick a random target player
        Player target = players.get(new Random().nextInt(players.size()));

        // Check max events for this player
        int maxEvents = config.getMaxEventsPerPlayer();
        int currentEvents = playerEventCounts.getOrDefault(target.getUniqueId(), 0);
        if (currentEvents >= maxEvents) return;

        // Check exempt
        if (isExempt(target)) return;

        // Select a random attack to spawn
        AbstractAttack attack = selectAttackForSpawn();
        if (attack == null) return;

        // Check if this attack is on cooldown
        if (attackCooldowns.containsKey(attack.getId())) return;

        // Spawn it
        Location spawnLoc = target.getLocation().clone();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        // Offset for non-tracking attacks (spawn near player, not on them)
        if (!attack.getConfig().tracksPlayer()) {
            double offsetX = (Math.random() * 2 - 1) * 8;
            double offsetZ = (Math.random() * 2 - 1) * 8;
            spawnLoc.add(offsetX, 0, offsetZ);
        }

        attack.spawn(spawnLoc, target);
        activeAttacks.add(attack);
        incrementPlayerCount(target);

        // Set cooldown
        attackCooldowns.put(attack.getId(), attack.getConfig().getCooldownTicks());

        plugin.debug("[AttackScheduler] Spawned " + attack.getId() + " near " + target.getName()
                + " (active: " + activeAttacks.size() + ")");
    }

    /**
     * Select the right type of attack based on current state.
     */
    private AbstractAttack selectAttackForSpawn() {
        // Decide what type to spawn based on weights
        // Block displays and environmental always active
        // Boss attacks only when boss is active
        double roll = Math.random();

        if (bossActive && currentBossName != null && roll < 0.33) {
            // 33% chance to spawn a boss attack when boss is active
            AbstractAttack bossAttack = registry.selectRandomBossAttack(currentBossName);
            if (bossAttack != null) return bossAttack;
        }

        if (roll < 0.5) {
            // Block display attack
            return registry.selectRandom(currentPhase, AttackType.BLOCK_DISPLAY);
        } else {
            // Environmental attack
            return registry.selectRandom(currentPhase, AttackType.ENVIRONMENTAL);
        }
    }

    // ========================
    // Forced spawns (test commands)
    // ========================

    /**
     * Force-spawn a specific attack on a player (for test commands).
     * Bypasses cooldowns, max events, and exempt checks.
     */
    public void forceSpawn(AbstractAttack attack, Player player) {
        Location spawnLoc = player.getLocation().clone();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        AbstractAttack instance = attack.newInstance();
        instance.spawn(spawnLoc, player);
        activeAttacks.add(instance);

        plugin.debug("[AttackScheduler] Force-spawned " + instance.getId() + " on " + player.getName());
    }

    // ========================
    // State management
    // ========================

    public void setPhase(int phase) {
        this.currentPhase = phase;
        // Clean up attacks from previous phase
        for (AbstractAttack attack : new ArrayList<>(activeAttacks)) {
            if (attack.getPhase() != phase) {
                attack.cleanup();
            }
        }
        activeAttacks.removeIf(a -> !a.isActive());
        plugin.debug("[AttackScheduler] Phase changed to " + phase);
    }

    public void setBossActive(boolean active, String bossName) {
        this.bossActive = active;
        this.currentBossName = bossName;
    }

    public void setBaseSpawnInterval(int ticks) {
        this.baseSpawnInterval = ticks;
    }

    public int getActiveAttackCount() {
        return activeAttacks.size();
    }

    public List<AbstractAttack> getActiveAttacks() {
        return Collections.unmodifiableList(activeAttacks);
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

    private World getEndWorld() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) return world;
        }
        return null;
    }
}
