package com.blockforge.chaoscraft.weapons.ivory.abilities.base;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DamageZoneManager {

    private static DamageZoneManager instance;
    private final ChaosCraftPlugin plugin;
    private final Map<UUID, DamageZone> activeZones = new ConcurrentHashMap<>();
    private BukkitTask tickTask;
    private int tickCounter = 0;

    private DamageZoneManager(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        DamageZone.setPlugin(plugin);
        startTicking();
    }

    public static DamageZoneManager getInstance(ChaosCraftPlugin plugin) {
        if (instance == null) instance = new DamageZoneManager(plugin);
        return instance;
    }

    public static DamageZoneManager getInstance() { return instance; }

    private void startTicking() {
        if (tickTask != null) return;
        tickTask = new BukkitRunnable() {
            @Override public void run() { tick(); }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void shutdown() {
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        activeZones.clear();
    }

    private void tick() {
        tickCounter++;
        var toRemove = new ArrayList<UUID>();
        for (var entry : activeZones.entrySet()) {
            if (!entry.getValue().tick(tickCounter)) toRemove.add(entry.getKey());
        }
        toRemove.forEach(activeZones::remove);
    }

    public void register(DamageZone zone) { if (zone != null) activeZones.put(zone.getId(), zone); }
    public void unregister(UUID zoneId) {
        var zone = activeZones.remove(zoneId);
        if (zone != null) zone.cancel();
    }
    public DamageZone getZone(UUID zoneId) { return activeZones.get(zoneId); }
    public Collection<DamageZone> getAllZones() { return new ArrayList<>(activeZones.values()); }

    public List<DamageZone> getZonesByOwner(UUID ownerId) {
        return activeZones.values().stream()
                .filter(z -> z.getOwner() != null && z.getOwner().getUniqueId().equals(ownerId))
                .toList();
    }

    public void cancelAllByOwner(UUID ownerId) {
        var toRemove = new ArrayList<UUID>();
        for (var entry : activeZones.entrySet()) {
            var zone = entry.getValue();
            if (zone.getOwner() != null && zone.getOwner().getUniqueId().equals(ownerId)) {
                zone.cancel(); toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(activeZones::remove);
    }

    public void cancelAll() {
        activeZones.values().forEach(DamageZone::cancel);
        activeZones.clear();
    }

    public int getActiveZoneCount() { return activeZones.size(); }
    public boolean isActive(UUID zoneId) {
        var zone = activeZones.get(zoneId);
        return zone != null && zone.isActive();
    }
}
