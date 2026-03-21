package com.blockforge.chaoscraft.modes.seer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attack scheduler for Seer Mode.
 * Supports both BLOCK_DISPLAY and ENVIRONMENTAL attack types.
 */
public class SeerScheduler {

    private final ChaosCraftPlugin plugin;
    private final AttackRegistry registry;
    private final SeerConfig config;

    private final List<AbstractAttack> activeAttacks = new ArrayList<>();
    private final Map<UUID, Integer> playerEventCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> attackCooldowns = new HashMap<>();

    private int spawnTickCounter = 0;
    private int baseSpawnInterval;
    private boolean active = false;

    public SeerScheduler(ChaosCraftPlugin plugin, AttackRegistry registry, SeerConfig config) {
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

        tickActiveAttacks();
        tickCooldowns();

        spawnTickCounter++;
        if (spawnTickCounter >= baseSpawnInterval) {
            spawnTickCounter = 0;
            attemptSpawn();
        }
    }

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

    private void attemptSpawn() {
        World world = getSeerWorld();
        if (world == null) return;

        // Filter to valid survival targets only
        List<Player> validPlayers = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL && !p.isInvulnerable() && !isExempt(p)) {
                int currentEvents = playerEventCounts.getOrDefault(p.getUniqueId(), 0);
                if (currentEvents < config.getSpawnMaxEventsPerPlayer()) {
                    validPlayers.add(p);
                }
            }
        }
        if (validPlayers.isEmpty()) return;

        Player target = validPlayers.get(new Random().nextInt(validPlayers.size()));

        // Alternate between BLOCK_DISPLAY and ENVIRONMENTAL
        AttackType type = Math.random() < 0.5 ? AttackType.BLOCK_DISPLAY : AttackType.ENVIRONMENTAL;
        AbstractAttack attack = registry.selectRandom(1, type);

        // Fallback to other type if none available
        if (attack == null) {
            type = (type == AttackType.BLOCK_DISPLAY) ? AttackType.ENVIRONMENTAL : AttackType.BLOCK_DISPLAY;
            attack = registry.selectRandom(1, type);
        }
        if (attack == null) return;

        // Try up to 3 times if the selected attack is on cooldown
        for (int attempt = 0; attempt < 3 && attackCooldowns.containsKey(attack.getId()); attempt++) {
            attack = registry.selectRandom(1, type);
            if (attack == null) return;
        }
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

        plugin.debug("[Seer] Spawned " + attack.getId() + " near " + target.getName()
                + " (active: " + activeAttacks.size() + ")");
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

    // State
    public void setBaseSpawnInterval(int ticks) { this.baseSpawnInterval = ticks; }
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

    // Player tracking
    private void incrementPlayerCount(Player player) {
        playerEventCounts.merge(player.getUniqueId(), 1, Integer::sum);
    }

    private void decrementPlayerCount(Player player) {
        if (player == null) return;
        playerEventCounts.computeIfPresent(player.getUniqueId(), (k, v) -> v > 1 ? v - 1 : null);
    }

    // Helpers
    private boolean isExempt(Player player) {
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive()) {
            return modeManager.getActiveMode().isExempt(player);
        }
        return player.hasPermission("chaoscraft.mode.exempt");
    }

    private World getSeerWorld() {
        String worldName = config.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        List<World> worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
