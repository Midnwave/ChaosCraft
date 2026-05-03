package com.blockforge.chaoscraft.modes.fluffy;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attack scheduler for Fluffy Mode.
 * Mirrors DoomScheduler — weighted random selection across BD / ENV / ME
 * attack types — but does NOT include any phase / escalation logic
 * (Fluffy is a flat-difficulty mode).
 *
 * Also drives the herd-pulse mechanic: every herd-pulse.interval-ticks
 * all entities tagged with {@code fluffy:managed} freeze (setAI(false))
 * for pulse-duration-ticks, then resume.
 */
public class FluffyScheduler {

    private final ChaosCraftPlugin plugin;
    private final AttackRegistry registry;
    private final FluffyConfig config;

    private final List<AbstractAttack> activeAttacks = new ArrayList<>();
    private final Map<UUID, Integer> playerEventCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> attackCooldowns = new HashMap<>();

    private int spawnTickCounter = 0;
    private int herdPulseTicks = 0;
    private int baseSpawnInterval;
    private boolean active = false;

    public FluffyScheduler(ChaosCraftPlugin plugin, AttackRegistry registry, FluffyConfig config) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = config;
        this.baseSpawnInterval = config.getBaseSpawnInterval();
    }

    public void start() {
        active = true;
        spawnTickCounter = 0;
        herdPulseTicks = 0;
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
        tickHerdPulse();

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

    private void tickHerdPulse() {
        if (!config.isHerdPulseEnabled()) return;
        herdPulseTicks++;
        if (herdPulseTicks < config.getHerdPulseInterval()) return;
        herdPulseTicks = 0;

        World world = getWorld();
        if (world == null) return;

        final List<UUID> frozenEntities = new ArrayList<>();
        for (Entity e : world.getEntities()) {
            if (e instanceof LivingEntity le && e.getScoreboardTags().contains("fluffy:managed")) {
                if (le.hasAI()) {
                    le.setAI(false);
                    frozenEntities.add(le.getUniqueId());
                }
            }
        }
        if (frozenEntities.isEmpty()) return;

        plugin.debug("[FluffyScheduler] Herd pulse — froze " + frozenEntities.size() + " managed mob(s).");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID id : frozenEntities) {
                Entity e = Bukkit.getEntity(id);
                if (e instanceof LivingEntity le) {
                    le.setAI(true);
                }
            }
        }, config.getHerdPulseDuration());
    }

    private void attemptSpawn() {
        World world = getWorld();
        if (world == null) return;

        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Player target = players.get(new Random().nextInt(players.size()));

        int maxEvents = config.getMaxEventsPerPlayer();
        int currentEvents = playerEventCounts.getOrDefault(target.getUniqueId(), 0);
        if (currentEvents >= maxEvents) return;

        if (isExempt(target)) return;

        // Weighted random attack type selection
        AttackType selectedType = selectWeightedType();
        AbstractAttack attack = registry.selectRandom(1, selectedType);

        // Fallback: if selected type has no attacks, try the others
        if (attack == null) {
            for (AttackType fallback : AttackType.values()) {
                if (fallback == AttackType.BOSS) continue;
                attack = registry.selectRandom(1, fallback);
                if (attack != null) break;
            }
        }
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

        plugin.debug("[FluffyScheduler] Spawned " + attack.getId() + " (" + attack.getType()
                + ") near " + target.getName() + " (active: " + activeAttacks.size() + ")");
    }

    private AttackType selectWeightedType() {
        double bdWeight = config.getTypeWeightBlockDisplay();
        double envWeight = config.getTypeWeightEnvironmental();
        double meWeight = config.getTypeWeightModelEngine();
        double total = bdWeight + envWeight + meWeight;

        if (total <= 0) return AttackType.BLOCK_DISPLAY;

        double roll = Math.random() * total;
        if (roll < bdWeight) return AttackType.BLOCK_DISPLAY;
        if (roll < bdWeight + envWeight) return AttackType.ENVIRONMENTAL;
        return AttackType.MODEL_ENGINE;
    }

    // ========================
    // Forced spawns (test commands)
    // ========================

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

        plugin.debug("[FluffyScheduler] Force-spawned " + instance.getId() + " on " + player.getName());
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
    public void setBaseSpawnInterval(int ticks) { this.baseSpawnInterval = ticks; }

    public void clearActiveAttacks() {
        for (AbstractAttack attack : new ArrayList<>(activeAttacks)) {
            attack.cleanup();
        }
        activeAttacks.clear();
        playerEventCounts.clear();
    }

    private void incrementPlayerCount(Player player) {
        playerEventCounts.merge(player.getUniqueId(), 1, Integer::sum);
    }

    private void decrementPlayerCount(Player player) {
        if (player == null) return;
        playerEventCounts.computeIfPresent(player.getUniqueId(), (k, v) -> v > 1 ? v - 1 : null);
    }

    private boolean isExempt(Player player) {
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive()) {
            return modeManager.getActiveMode().isExempt(player);
        }
        return player.hasPermission("chaoscraft.mode.exempt");
    }

    private World getWorld() {
        String worldName = config.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        List<World> worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
