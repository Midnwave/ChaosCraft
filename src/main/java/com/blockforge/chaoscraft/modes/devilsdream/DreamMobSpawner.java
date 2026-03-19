package com.blockforge.chaoscraft.modes.devilsdream;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Handles MythicMobs nightmare creature spawning during Devil's Dream.
 * Selects mob type based on Dream Adaptation scores — the mob that
 * spawns matches the player's dominant behavior.
 *
 * Mob type → Adaptation mapping:
 * - NightmareHound → MOVEMENT (fast melee, punishes sprinters)
 * - DreamWraith → FEAR (ranged, punishes sneakers)
 * - ShadowStalker → PARANOIA (invisible until close, punishes lookers)
 * - InfernalImp → FLIGHT (swarm, punishes jumpers)
 * - NightmareBrute → AGGRESSION (tanky, punishes fighters)
 * - SoulHarvester → STILLNESS (ranged drain, punishes campers)
 * - DreamPhantom → CREATION (flying, punishes builders)
 * - BoneRevenant → DESTRUCTION (skeleton, punishes miners)
 */
public class DreamMobSpawner {

    private final ChaosCraftPlugin plugin;
    private final DevilsDreamConfig config;
    private final DreamAdaptationTracker adaptationTracker;

    private final List<UUID> activeMobs = new ArrayList<>();
    private int spawnTickCounter = 0;
    private boolean mythicMobsAvailable = false;

    // Mapping from DreamAction to preferred MythicMobs mob ID index
    private static final Map<DreamAction, String> ACTION_MOB_MAP = new EnumMap<>(DreamAction.class);
    static {
        ACTION_MOB_MAP.put(DreamAction.MOVEMENT, "NightmareHound");
        ACTION_MOB_MAP.put(DreamAction.FEAR, "DreamWraith");
        ACTION_MOB_MAP.put(DreamAction.PARANOIA, "ShadowStalker");
        ACTION_MOB_MAP.put(DreamAction.FLIGHT, "InfernalImp");
        ACTION_MOB_MAP.put(DreamAction.AGGRESSION, "NightmareBrute");
        ACTION_MOB_MAP.put(DreamAction.STILLNESS, "SoulHarvester");
        ACTION_MOB_MAP.put(DreamAction.CREATION, "DreamPhantom");
        ACTION_MOB_MAP.put(DreamAction.DESTRUCTION, "BoneRevenant");
    }

    public DreamMobSpawner(ChaosCraftPlugin plugin, DevilsDreamConfig config,
                            DreamAdaptationTracker adaptationTracker) {
        this.plugin = plugin;
        this.config = config;
        this.adaptationTracker = adaptationTracker;
        this.mythicMobsAvailable = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
    }

    /**
     * Called every tick from DevilsDreamMode.onTick().
     */
    public void tick(World world) {
        if (!config.isMythicMobsEnabled() || !mythicMobsAvailable) return;

        // Clean up dead/removed mobs
        activeMobs.removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            return entity == null || entity.isDead() || !entity.isValid();
        });

        spawnTickCounter++;
        if (spawnTickCounter < config.getMobSpawnInterval()) return;
        spawnTickCounter = 0;

        if (activeMobs.size() >= config.getMaxMobsAlive()) return;

        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        Player target = players.get(new Random().nextInt(players.size()));

        // Check exempt
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive() && modeManager.getActiveMode().isExempt(target)) return;

        // Select mob based on dominant adaptation action
        DreamAction dominant = adaptationTracker.getDominantAction(target.getUniqueId());
        String preferredMob = ACTION_MOB_MAP.getOrDefault(dominant, "NightmareHound");

        // Verify the mob ID is in the configured list
        List<String> configuredMobs = config.getMobIds();
        if (!configuredMobs.contains(preferredMob)) {
            // Fallback to random configured mob
            if (configuredMobs.isEmpty()) return;
            preferredMob = configuredMobs.get(new Random().nextInt(configuredMobs.size()));
        }

        // Spawn location: 8-15 blocks from player in random direction
        Location spawnLoc = target.getLocation().clone();
        double angle = Math.random() * Math.PI * 2;
        double dist = 8 + Math.random() * 7;
        spawnLoc.add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        // Snap to ground level
        spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

        // Use MythicMobs API to spawn
        try {
            var mythicMobs = (io.lumine.mythic.bukkit.MythicBukkit) Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (mythicMobs == null) return;

            var mobManager = mythicMobs.getMobManager();
            var mob = mobManager.spawnMob(preferredMob, spawnLoc);

            if (mob != null) {
                Entity entity = mob.getEntity().getBukkitEntity();
                activeMobs.add(entity.getUniqueId());
                plugin.debug("[DevilsDream] Spawned nightmare mob: " + preferredMob
                        + " near " + target.getName() + " (alive: " + activeMobs.size() + ")");
            }
        } catch (Exception e) {
            plugin.debug("[DevilsDream] Failed to spawn MythicMob '" + preferredMob + "': " + e.getMessage());
        }
    }

    /**
     * Clean up all active nightmare mobs.
     */
    public void cleanup() {
        for (UUID uuid : activeMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activeMobs.clear();
        spawnTickCounter = 0;
    }

    public int getActiveMobCount() { return activeMobs.size(); }
    public boolean isMythicMobsAvailable() { return mythicMobsAvailable; }
}
