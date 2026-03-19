package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attack scheduler for Chain Mode.
 * Similar to Calamity's AttackScheduler but works in a configurable world
 * (Overworld by default) instead of hardcoded The End.
 *
 * No boss attacks — only BLOCK_DISPLAY type for Chain Mode.
 */
public class ChainAttackScheduler {

    private final ChaosCraftPlugin plugin;
    private final AttackRegistry registry;
    private final ChainConfig config;

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

    public ChainAttackScheduler(ChaosCraftPlugin plugin, AttackRegistry registry, ChainConfig config) {
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
     * Called every server tick from ChainMode.onTick().
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
        World world = getChainWorld();
        if (world == null) {
            plugin.debug("[ChainScheduler] Chain world not found — cannot spawn attacks.");
            return;
        }

        List<Player> players = world.getPlayers();
        if (players.isEmpty()) {
            plugin.debug("[ChainScheduler] No players in chain world — skipping spawn.");
            return;
        }

        Player target = players.get(new Random().nextInt(players.size()));

        int maxEvents = config.getMaxEventsPerPlayer();
        int currentEvents = playerEventCounts.getOrDefault(target.getUniqueId(), 0);
        if (currentEvents >= maxEvents) {
            plugin.debug("[ChainScheduler] " + target.getName() + " has max events (" + currentEvents + "/" + maxEvents + ") — skipping.");
            return;
        }

        if (isExempt(target)) {
            plugin.debug("[ChainScheduler] " + target.getName() + " is exempt — skipping.");
            return;
        }

        // Chain Mode only has BLOCK_DISPLAY attacks, all phase 1
        AbstractAttack attack = registry.selectRandom(1, AttackType.BLOCK_DISPLAY);
        if (attack == null) {
            plugin.debug("[ChainScheduler] No enabled attack found. Check attack configs.");
            return;
        }

        if (attackCooldowns.containsKey(attack.getId())) {
            plugin.debug("[ChainScheduler] Attack " + attack.getId() + " on cooldown — skipping.");
            return;
        }

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

        attackCooldowns.put(attack.getId(), attack.getConfig().getCooldownTicks());

        plugin.debug("[ChainScheduler] Spawned " + attack.getId() + " near " + target.getName()
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
        instance.spawn(spawnLoc, player);
        activeAttacks.add(instance);

        if (!active) {
            startTemporaryTicking(instance);
        }

        plugin.debug("[ChainScheduler] Force-spawned " + instance.getId() + " on " + player.getName()
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

    private World getChainWorld() {
        String worldName = config.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        // Default: first loaded world (Overworld)
        List<World> worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
