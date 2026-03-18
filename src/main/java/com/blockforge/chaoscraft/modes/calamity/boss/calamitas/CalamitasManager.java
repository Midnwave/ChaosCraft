package com.blockforge.chaoscraft.modes.calamity.boss.calamitas;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityConfig;
import com.blockforge.chaoscraft.nms.entity.SupremeCalamitasEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Manages Supreme Calamitas lifecycle — spawn, phase transitions, death,
 * and integration with CalamityMode.
 */
public class CalamitasManager {

    private final ChaosCraftPlugin plugin;
    private final CalamityConfig config;
    private SupremeCalamitasEntity entity;

    public CalamitasManager(ChaosCraftPlugin plugin, CalamityConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Spawn Supreme Calamitas at the given location.
     */
    public void spawn(Location spawnLoc) {
        if (entity != null && entity.isAlive()) {
            plugin.getLogger().warning("[SC] Supreme Calamitas is already alive!");
            return;
        }

        // Load config values
        double health = config.getCalamitasHealth();
        double detectionRange = config.getCalamitasDetectionRange();
        double groundSpeed = config.getCalamitasGroundSpeed();
        double flySpeed = config.getCalamitasFlySpeed();
        double attackDamage = config.getCalamitasAttackDamage();
        int attackInterval = config.getCalamitasAttackInterval();
        ItemStack heldItem = config.getCalamitasHeldItem();

        entity = new SupremeCalamitasEntity(
                plugin, spawnLoc,
                health, detectionRange,
                groundSpeed, flySpeed,
                attackDamage, attackInterval,
                heldItem
        );

        plugin.getLogger().info("[SC] Supreme Calamitas spawned at " +
                String.format("%.1f, %.1f, %.1f", spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
    }

    /**
     * Kill/remove Supreme Calamitas.
     */
    public void kill() {
        if (entity != null) {
            entity.remove();
            entity = null;
            plugin.getLogger().info("[SC] Supreme Calamitas removed.");
        }
    }

    /**
     * Switch to flight phase.
     */
    public void enableFlight() {
        if (entity != null && entity.isAlive()) {
            entity.enableFlight();
        }
    }

    /**
     * Return to ground phase.
     */
    public void disableFlight() {
        if (entity != null && entity.isAlive()) {
            entity.disableFlight();
        }
    }

    // --- Getters ---

    public boolean isAlive() {
        return entity != null && entity.isAlive();
    }

    public double getCurrentHealth() {
        return entity != null ? entity.getCurrentHealth() : 0;
    }

    public double getMaxHealth() {
        return entity != null ? entity.getMaxHealth() : 0;
    }

    public double getHealthPercent() {
        return entity != null ? entity.getHealthPercent() : 0;
    }

    public boolean isInFlightPhase() {
        return entity != null && entity.isInFlightPhase();
    }

    public List<Player> getTrackedPlayers() {
        return entity != null ? entity.getTrackedPlayers() : List.of();
    }

    public String getDebugInfo() {
        if (entity == null) return "§cSupreme Calamitas is not spawned.";
        return entity.getDebugInfo();
    }

    public SupremeCalamitasEntity getEntity() {
        return entity;
    }
}
