package com.blockforge.chaoscraft.modes.tutorial;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attack scheduler for Tutorial Mode.
 * Slower pace than other modes — only BLOCK_DISPLAY type.
 */
public class TutorialScheduler {

    private final ChaosCraftPlugin plugin;
    private final AttackRegistry registry;
    private final TutorialConfig config;
    private final List<AbstractAttack> activeAttacks = new ArrayList<>();
    private final Map<UUID, Integer> playerEventCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> attackCooldowns = new HashMap<>();
    private int spawnTickCounter = 0;
    private int baseSpawnInterval;
    private boolean active = false;

    public TutorialScheduler(ChaosCraftPlugin plugin, AttackRegistry registry, TutorialConfig config) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = config;
        this.baseSpawnInterval = config.getBaseSpawnInterval();
    }

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

    public void tick() {
        if (!active) return;

        // Tick active attacks
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

        // Tick cooldowns
        attackCooldowns.entrySet().removeIf(e -> { e.setValue(e.getValue() - 1); return e.getValue() <= 0; });

        // Spawn check
        spawnTickCounter++;
        if (spawnTickCounter >= baseSpawnInterval) {
            spawnTickCounter = 0;
            attemptSpawn();
        }
    }

    private void attemptSpawn() {
        World world = getTutorialWorld();
        if (world == null) return;

        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Player target = players.get(new Random().nextInt(players.size()));

        int maxEvents = config.getMaxEventsPerPlayer();
        int currentEvents = playerEventCounts.getOrDefault(target.getUniqueId(), 0);
        if (currentEvents >= maxEvents) return;

        if (isExempt(target)) return;

        // Only BLOCK_DISPLAY for tutorial
        AbstractAttack attack = registry.selectRandom(1, AttackType.BLOCK_DISPLAY);
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

        plugin.debug("[TutorialScheduler] Spawned " + attack.getId() + " near " + target.getName());
    }

    public void forceSpawn(AbstractAttack attack, Player player) {
        Location spawnLoc = player.getLocation().clone();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(0);

        AbstractAttack instance = attack.newInstance();
        instance.getConfig().copyFrom(attack.getConfig());
        instance.spawn(spawnLoc, player);
        activeAttacks.add(instance);

        if (!active) {
            startTemporaryTicking(instance);
        }
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

    public void clearAll() {
        for (AbstractAttack a : new ArrayList<>(activeAttacks)) a.cleanup();
        activeAttacks.clear();
        playerEventCounts.clear();
    }

    private void incrementPlayerCount(Player player) {
        playerEventCounts.merge(player.getUniqueId(), 1, Integer::sum);
    }

    private void decrementPlayerCount(Player player) {
        if (player == null) return;
        playerEventCounts.computeIfPresent(player.getUniqueId(), (k, v) -> v <= 1 ? null : v - 1);
    }

    private boolean isExempt(Player player) {
        var mm = plugin.getModeManager();
        if (mm.isAnyModeActive()) {
            var active = mm.getActiveMode();
            if (active.isExempt(player)) return true;
        }
        return player.hasPermission("chaoscraft.mode.exempt");
    }

    private World getTutorialWorld() {
        String worldName = config.getWorldName();
        if (worldName != null && !worldName.isEmpty()) return plugin.getServer().getWorld(worldName);
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    public int getActiveCount() { return activeAttacks.size(); }
    public void setSpawnInterval(int ticks) { this.baseSpawnInterval = ticks; }
}
