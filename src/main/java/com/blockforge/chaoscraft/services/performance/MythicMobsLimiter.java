package com.blockforge.chaoscraft.services.performance;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Listens for MythicMobs spawn events and cancels them when configured limits
 * (radius / server-wide / per-player) are exceeded.
 * Also provides helper methods for mob counting and ping-based cleanup.
 */
public class MythicMobsLimiter implements Listener {

    private final ChaosCraftPlugin plugin;
    private PerformanceConfig config;
    private boolean registered = false;
    private BukkitAPIHelper mythicHelper;

    public MythicMobsLimiter(ChaosCraftPlugin plugin, PerformanceConfig config) {
        this.plugin = plugin;
        this.config = config;

        try {
            this.mythicHelper = MythicBukkit.inst().getAPIHelper();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get MythicMobs API helper: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- registration

    public void register() {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
            if (config.isDebug()) {
                plugin.getLogger().info("[Performance] MythicMobs limiter registered.");
            }
        }
    }

    public void unregister() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
            if (config.isDebug()) {
                plugin.getLogger().info("[Performance] MythicMobs limiter unregistered.");
            }
        }
    }

    public void updateConfig(PerformanceConfig config) {
        this.config = config;
    }

    // ---------------------------------------------------------------- spawn listener

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        if (!config.isEnabled()) return;

        MythicMob mobType = event.getMobType();
        var mobId = mobType.getInternalName();
        var spawnLoc = event.getLocation();

        // Bypass check
        if (config.isBypassMob(mobId)) {
            if (config.isDebug()) {
                plugin.getLogger().info("[Performance] Bypass mob spawning: " + mobId);
            }
            return;
        }

        // Radius limit
        if (config.isRadiusLimitEnabled()) {
            int count = getMobCountInRadius(spawnLoc, config.getRadiusBlocks());
            if (count >= config.getRadiusMaxMobs()) {
                event.setCancelled(true);
                if (config.isDebug()) {
                    plugin.getLogger().info("[Performance] Spawn blocked (radius): " + mobId
                            + " - " + count + "/" + config.getRadiusMaxMobs()
                            + " in " + config.getRadiusBlocks() + " blocks");
                }
                return;
            }
        }

        // Server limit
        if (config.isServerLimitEnabled()) {
            int count = getServerMobCount();
            if (count >= config.getServerMaxMobs()) {
                event.setCancelled(true);
                if (config.isDebug()) {
                    plugin.getLogger().info("[Performance] Spawn blocked (server): " + mobId
                            + " - " + count + "/" + config.getServerMaxMobs());
                }
                return;
            }
        }

        // Per-player limit
        if (config.isPerPlayerLimitEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("chaoscraft.performance.bypass")) continue;

                var playerLoc = player.getLocation();
                if (playerLoc.getWorld() != spawnLoc.getWorld()) continue;

                double distance = playerLoc.distance(spawnLoc);
                if (distance <= config.getPerPlayerRadiusBlocks()) {
                    int count = getMobCountInRadius(playerLoc, config.getPerPlayerRadiusBlocks());
                    if (count >= config.getPerPlayerMaxMobs()) {
                        event.setCancelled(true);
                        if (config.isDebug()) {
                            plugin.getLogger().info("[Performance] Spawn blocked (per-player): " + mobId
                                    + " - " + count + "/" + config.getPerPlayerMaxMobs()
                                    + " near " + player.getName());
                        }
                        return;
                    }
                }
            }
        }

        if (config.isDebug()) {
            plugin.getLogger().info("[Performance] Spawn allowed: " + mobId);
        }
    }

    // ---------------------------------------------------------------- mob counting

    /** Returns the total number of active MythicMobs on the server. */
    public int getServerMobCount() {
        try {
            return MythicBukkit.inst().getMobManager().getActiveMobs().size();
        } catch (Exception e) {
            if (config.isDebug()) {
                plugin.getLogger().warning("[Performance] Error getting server mob count: " + e.getMessage());
            }
            return 0;
        }
    }

    /** Counts active MythicMobs within {@code radius} blocks of {@code center}. */
    public int getMobCountInRadius(Location center, int radius) {
        try {
            Collection<ActiveMob> activeMobs = MythicBukkit.inst().getMobManager().getActiveMobs();
            double radiusSquared = (double) radius * radius;
            int count = 0;

            for (var mob : activeMobs) {
                Entity entity = mob.getEntity().getBukkitEntity();
                if (entity != null) {
                    var mobLoc = entity.getLocation();
                    if (mobLoc.getWorld() == center.getWorld()
                            && mobLoc.distanceSquared(center) <= radiusSquared) {
                        count++;
                    }
                }
            }
            return count;
        } catch (Exception e) {
            if (config.isDebug()) {
                plugin.getLogger().warning("[Performance] Error getting mob count in radius: " + e.getMessage());
            }
            return 0;
        }
    }

    /** Returns all active MythicMobs within {@code radius} blocks of {@code center}. */
    public List<ActiveMob> getMobsInRadius(Location center, int radius) {
        var result = new ArrayList<ActiveMob>();
        try {
            Collection<ActiveMob> activeMobs = MythicBukkit.inst().getMobManager().getActiveMobs();
            double radiusSquared = (double) radius * radius;

            for (var mob : activeMobs) {
                Entity entity = mob.getEntity().getBukkitEntity();
                if (entity != null) {
                    var mobLoc = entity.getLocation();
                    if (mobLoc.getWorld() == center.getWorld()
                            && mobLoc.distanceSquared(center) <= radiusSquared) {
                        result.add(mob);
                    }
                }
            }
        } catch (Exception e) {
            if (config.isDebug()) {
                plugin.getLogger().warning("[Performance] Error getting mobs in radius: " + e.getMessage());
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- cleanup

    /**
     * Removes up to {@code maxRemove} MythicMobs within {@code radius} blocks of the player.
     * Respects bypass and ping-protected mob lists.
     *
     * @return the number of mobs actually removed
     */
    public int cleanupMobsAroundPlayer(Player player, int radius, int maxRemove) {
        var mobs = getMobsInRadius(player.getLocation(), radius);
        int removed = 0;

        for (var mob : mobs) {
            if (removed >= maxRemove) break;

            var mobId = mob.getMobType();
            if (config.isPingProtectedMob(mobId) || config.isBypassMob(mobId)) continue;

            List<String> targetMobs = config.getPingTargetMobs();
            if (!targetMobs.isEmpty() && !targetMobs.contains(mobId)) continue;

            try {
                Entity entity = mob.getEntity().getBukkitEntity();
                if (entity != null && entity.isValid()) {
                    mob.remove();
                    removed++;
                    if (config.isDebug()) {
                        plugin.getLogger().info("[Performance] Removed mob: " + mobId
                                + " near " + player.getName());
                    }
                }
            } catch (Exception e) {
                if (config.isDebug()) {
                    plugin.getLogger().warning("[Performance] Error removing mob: " + e.getMessage());
                }
            }
        }
        return removed;
    }

    // ---------------------------------------------------------------- state

    public boolean isApiAvailable() {
        return mythicHelper != null;
    }
}
