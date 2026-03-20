package com.blockforge.chaoscraft.services.stats;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listens for entity deaths and awards kills to the killing player.
 * Supports vanilla mobs, MythicMobs, and ChaosCraft mode entities.
 */
public class KillTrackingListener implements Listener {

    private final ChaosCraftPlugin plugin;
    private final PlayerStatsService statsService;

    public KillTrackingListener(ChaosCraftPlugin plugin, PlayerStatsService statsService) {
        this.plugin = plugin;
        this.statsService = statsService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        // Don't count players
        if (entity.getType() == EntityType.PLAYER) return;

        int killValue = resolveKillValue(entity);
        if (killValue > 0) {
            statsService.addKills(killer.getUniqueId(), killValue);
        }
    }

    private int resolveKillValue(LivingEntity entity) {
        // Check MythicMobs first
        int mythicValue = checkMythicMobs(entity);
        if (mythicValue > 0) return mythicValue;

        // Fall back to entity type config
        return statsService.getKillValue(entity.getType().name());
    }

    private int checkMythicMobs(LivingEntity entity) {
        try {
            // MythicMobs 5.x API
            var mythicInst = io.lumine.mythic.bukkit.MythicBukkit.inst();
            if (mythicInst == null) return -1;

            var activeMob = mythicInst.getMobManager().getActiveMob(entity.getUniqueId());
            if (activeMob.isPresent()) {
                String mobType = activeMob.get().getMobType();
                int configValue = statsService.getMythicMobKillValue(mobType);
                if (configValue > 0) return configValue;
                // MythicMob exists but no config — use chaoscraft-entity default
                return statsService.getConfig().getKillValue("MYTHIC_DEFAULT");
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // MythicMobs not installed — silently ignore
        }
        return -1;
    }
}
