package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Per-player temperature and freeze tracking for Freezing Ice mode.
 *
 * Temperature starts at 100 (warm) and decays over time.
 * At thresholds: movement slows, freeze overlay appears, damage increases.
 * Heat sources (torches, campfires, lava, fire) restore temperature.
 * Powder snow freeze ticks via Player.setFreezeTicks() for visual overlay.
 */
public class TemperatureTracker {

    private final ChaosCraftPlugin plugin;
    private final FreezingIceConfig config;

    private final Map<UUID, Integer> temperatures = new HashMap<>();
    private final Map<UUID, Integer> freezeTicks = new HashMap<>();
    private int decayCounter = 0;
    private int freezeCounter = 0;

    // Attribute modifier key for movement speed
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String SPEED_MODIFIER_NAME = "chaoscraft.freezingice.cold";

    // Heat source blocks
    private static final Set<Material> HEAT_SOURCES = Set.of(
            Material.TORCH, Material.WALL_TORCH,
            Material.SOUL_TORCH, Material.SOUL_WALL_TORCH,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.LAVA, Material.FIRE, Material.SOUL_FIRE,
            Material.MAGMA_BLOCK, Material.FURNACE, Material.BLAST_FURNACE,
            Material.SMOKER
    );

    public TemperatureTracker(ChaosCraftPlugin plugin, FreezingIceConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Called every tick from FreezingIceMode.onTick().
     */
    public void tick(Collection<? extends Player> players) {
        if (!config.isTemperatureEnabled() && !config.isFreezeEnabled()) return;

        for (Player player : players) {
            if (!player.isOnline()) continue;
            UUID id = player.getUniqueId();

            // Initialize if new
            temperatures.putIfAbsent(id, config.getTemperatureStartValue());
            freezeTicks.putIfAbsent(id, 0);

            // Check heat sources
            boolean nearHeat = isNearHeatSource(player);

            // Update freeze ticks
            if (config.isFreezeEnabled()) {
                updateFreezeTicks(player, id, nearHeat);
            }

            // Restore temperature near heat
            if (nearHeat && config.isTemperatureEnabled()) {
                int temp = temperatures.get(id);
                int restored = Math.min(temp + config.getHeatSourceRestore(), config.getTemperatureStartValue());
                temperatures.put(id, restored);
            }
        }

        // Temperature decay
        if (config.isTemperatureEnabled()) {
            decayCounter++;
            if (decayCounter >= config.getTemperatureDecayInterval()) {
                decayCounter = 0;
                decayTemperatures(players);
            }
        }

        // Apply speed modifiers every 20 ticks (1 second)
        freezeCounter++;
        if (freezeCounter % 20 == 0 && config.isTemperatureEnabled()) {
            for (Player player : players) {
                if (!player.isOnline()) continue;
                applySpeedModifier(player);
            }
        }
    }

    /**
     * Decay all player temperatures.
     */
    private void decayTemperatures(Collection<? extends Player> players) {
        int decayAmount = config.getTemperatureDecayRate();
        for (Player player : players) {
            if (!player.isOnline()) continue;
            UUID id = player.getUniqueId();
            int temp = temperatures.getOrDefault(id, config.getTemperatureStartValue());
            temperatures.put(id, Math.max(0, temp - decayAmount));
        }
    }

    /**
     * Update freeze ticks for a player (powder snow overlay).
     */
    private void updateFreezeTicks(Player player, UUID id, boolean nearHeat) {
        int current = freezeTicks.getOrDefault(id, 0);
        int temp = temperatures.getOrDefault(id, config.getTemperatureStartValue());
        int max = config.getMaxFreezeTicks();

        if (nearHeat) {
            // Thaw
            current = Math.max(0, current - config.getThawRate());
        } else if (temp <= config.getFreezeOverlayThreshold()) {
            // Cold enough for freeze overlay
            current = Math.min(max, current + config.getBaseFreezeRate());
        }

        freezeTicks.put(id, current);
        player.setFreezeTicks(current);
    }

    /**
     * Apply movement speed modifier based on temperature.
     */
    private void applySpeedModifier(Player player) {
        UUID id = player.getUniqueId();
        int temp = temperatures.getOrDefault(id, config.getTemperatureStartValue());

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        // Remove existing modifier
        for (AttributeModifier mod : speedAttr.getModifiers()) {
            if (SPEED_MODIFIER_NAME.equals(mod.getName())) {
                speedAttr.removeModifier(mod);
                break;
            }
        }

        // Apply new modifier based on temperature threshold
        double speedMod = 0.0;
        if (temp <= 25) {
            speedMod = config.getSpeedThreshold25();
        } else if (temp <= 50) {
            speedMod = config.getSpeedThreshold50();
        } else if (temp <= 75) {
            speedMod = config.getSpeedThreshold75();
        }

        if (speedMod != 0.0) {
            speedAttr.addModifier(new AttributeModifier(
                    SPEED_MODIFIER_UUID, SPEED_MODIFIER_NAME, speedMod,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    /**
     * Check if a player is near a heat source block.
     */
    private boolean isNearHeatSource(Player player) {
        if (!config.isThawNearHeat()) return false;
        Location loc = player.getLocation();
        int radius = (int) Math.ceil(config.getHeatSourceRadius());

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = loc.getWorld().getBlockAt(
                            loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (HEAT_SOURCES.contains(block.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ========================
    // Public API
    // ========================

    /** Get a player's current temperature (0-100). */
    public int getTemperature(UUID playerId) {
        return temperatures.getOrDefault(playerId, config.getTemperatureStartValue());
    }

    /** Get a player's current freeze ticks. */
    public int getFreezeTicks(UUID playerId) {
        return freezeTicks.getOrDefault(playerId, 0);
    }

    /** Reduce a player's temperature (called by attacks). */
    public void reduceTemperature(UUID playerId, int amount) {
        int current = temperatures.getOrDefault(playerId, config.getTemperatureStartValue());
        temperatures.put(playerId, Math.max(0, current - amount));
    }

    /** Add freeze ticks to a player (called by attacks). */
    public void addFreezeTicks(UUID playerId, int ticks) {
        int current = freezeTicks.getOrDefault(playerId, 0);
        int max = config.getMaxFreezeTicks();
        freezeTicks.put(playerId, Math.min(max, current + ticks));
    }

    /** Set a player's temperature directly. */
    public void setTemperature(UUID playerId, int value) {
        temperatures.put(playerId, Math.max(0, Math.min(config.getTemperatureStartValue(), value)));
    }

    /** Reset a single player. */
    public void resetPlayer(UUID playerId) {
        temperatures.remove(playerId);
        freezeTicks.remove(playerId);
    }

    /** Reset all tracking and remove speed modifiers from all players. */
    public void resetAll(Collection<? extends Player> players) {
        // Remove speed modifiers from all tracked players
        for (Player player : players) {
            if (!player.isOnline()) continue;
            AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) {
                for (AttributeModifier mod : new ArrayList<>(speedAttr.getModifiers())) {
                    if (SPEED_MODIFIER_NAME.equals(mod.getName())) {
                        speedAttr.removeModifier(mod);
                    }
                }
            }
            // Reset freeze ticks to 0
            player.setFreezeTicks(0);
        }
        temperatures.clear();
        freezeTicks.clear();
        decayCounter = 0;
        freezeCounter = 0;
    }
}
