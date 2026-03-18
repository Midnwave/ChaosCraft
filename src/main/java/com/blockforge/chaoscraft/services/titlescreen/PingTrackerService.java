package com.blockforge.chaoscraft.services.titlescreen;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically samples each online player's ping and caches it
 * for fast, thread-safe reads (e.g. from placeholder resolution).
 */
public class PingTrackerService {

    private final ChaosCraftPlugin plugin;
    private final Map<UUID, Integer> pingCache = new ConcurrentHashMap<>();
    private BukkitRunnable trackerTask;

    public PingTrackerService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getLogger().info("Starting ping tracker service...");
        trackerTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    pingCache.put(player.getUniqueId(), player.getPing());
                }
            }
        };
        trackerTask.runTaskTimerAsynchronously(plugin, 0L, 20L);
        plugin.getLogger().info("Ping tracker service started (updating every second).");
    }

    public void stop() {
        if (trackerTask != null && !trackerTask.isCancelled()) {
            trackerTask.cancel();
            plugin.getLogger().info("Ping tracker service stopped.");
        }
        pingCache.clear();
    }

    public int getPing(UUID playerId) {
        return pingCache.getOrDefault(playerId, -1);
    }

    public int getPing(Player player) {
        return getPing(player.getUniqueId());
    }

    public void removePlayer(UUID playerId) {
        pingCache.remove(playerId);
    }
}
