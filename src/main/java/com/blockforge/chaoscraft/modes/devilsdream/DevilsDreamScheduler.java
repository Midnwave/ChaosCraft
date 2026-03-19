package com.blockforge.chaoscraft.modes.devilsdream;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attack scheduler for Devil's Dream Mode.
 * Spawns both BLOCK_DISPLAY and ENVIRONMENTAL attacks.
 * Integrates with Dream Adaptation system to bias attack selection
 * toward categories that counter the player's dominant behavior.
 */
public class DevilsDreamScheduler {

    private final ChaosCraftPlugin plugin;
    private final AttackRegistry registry;
    private final DevilsDreamConfig config;
    private final DreamAdaptationTracker adaptationTracker;

    private final List<AbstractAttack> activeAttacks = new ArrayList<>();
    private final Map<UUID, Integer> playerEventCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> attackCooldowns = new HashMap<>();

    private int spawnTickCounter = 0;
    private int baseSpawnInterval;
    private boolean active = false;

    public DevilsDreamScheduler(ChaosCraftPlugin plugin, AttackRegistry registry,
                                 DevilsDreamConfig config, DreamAdaptationTracker adaptationTracker) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = config;
        this.adaptationTracker = adaptationTracker;
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
     * Called every server tick from DevilsDreamMode.onTick().
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
        World world = getDreamWorld();
        if (world == null) return;

        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Player target = players.get(new Random().nextInt(players.size()));

        int maxEvents = config.getMaxEventsPerPlayer();
        int currentEvents = playerEventCounts.getOrDefault(target.getUniqueId(), 0);
        if (currentEvents >= maxEvents) return;

        if (isExempt(target)) return;

        // Select attack type — alternate between block display and environmental
        // Bias based on adaptation scores
        AbstractAttack attack = selectAdaptedAttack(target);
        if (attack == null) return;

        if (attackCooldowns.containsKey(attack.getId())) return;

        Location spawnLoc = target.getLocation().clone();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        if (!attack.getConfig().tracksPlayer()) {
            double offsetRadius = config.getSpawnOffsetRadius();
            double offsetX = (Math.random() * 2 - 1) * offsetRadius;
            double offsetZ = (Math.random() * 2 - 1) * offsetRadius;
            spawnLoc.add(offsetX, 0, offsetZ);
        }

        attack.spawn(spawnLoc, target);
        activeAttacks.add(attack);
        incrementPlayerCount(target);
        attackCooldowns.put(attack.getId(), attack.getConfig().getCooldownTicks());

        plugin.debug("[DevilsDream] Spawned " + attack.getId() + " near " + target.getName()
                + " (active: " + activeAttacks.size() + ")");
    }

    /**
     * Select an attack biased by the player's adaptation scores.
     * 50% chance block display, 50% chance environmental.
     * Within each type, the adaptation system can boost certain attacks.
     */
    private AbstractAttack selectAdaptedAttack(Player target) {
        boolean blockDisplay = Math.random() < 0.5;
        AttackType type = blockDisplay ? AttackType.BLOCK_DISPLAY : AttackType.ENVIRONMENTAL;

        // Devil's Dream uses phase 1 for all attacks
        return registry.selectRandom(1, type);
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

        plugin.debug("[DevilsDream] Force-spawned " + instance.getId() + " on " + player.getName());
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
    // State
    // ========================

    public int getActiveAttackCount() { return activeAttacks.size(); }
    public List<AbstractAttack> getActiveAttacks() { return Collections.unmodifiableList(activeAttacks); }
    public boolean isActive() { return active; }

    public void clearActiveAttacks() {
        for (AbstractAttack attack : new ArrayList<>(activeAttacks)) {
            attack.cleanup();
        }
        activeAttacks.clear();
        playerEventCounts.clear();
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

    private World getDreamWorld() {
        String worldName = config.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
