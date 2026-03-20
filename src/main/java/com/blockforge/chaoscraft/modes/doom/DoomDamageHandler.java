package com.blockforge.chaoscraft.modes.doom;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom lava damage handler for Doom Mode.
 *
 * <p>Overrides vanilla lava damage with a configurable amount and interval.
 * Vanilla lava deals 4 hearts/sec — this lets server admins tune it for their
 * player base and difficulty level.
 *
 * <p>Listens to {@link EntityDamageEvent} with {@code DamageCause.LAVA},
 * cancels the vanilla damage, and applies the configured amount on a cooldown.
 */
public class DoomDamageHandler implements Listener {

    private final ChaosCraftPlugin plugin;
    private final DoomConfig config;

    private boolean active = false;
    private final Map<UUID, Integer> lavaDamageCooldowns = new HashMap<>();

    public DoomDamageHandler(ChaosCraftPlugin plugin, DoomConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Activate the damage handler. Call on mode start.
     */
    public void start() {
        active = true;
        lavaDamageCooldowns.clear();
    }

    /**
     * Deactivate the damage handler. Call on mode end.
     */
    public void stop() {
        active = false;
        lavaDamageCooldowns.clear();
    }

    /**
     * Called every tick from DoomMode.onTick() to decrement cooldowns.
     */
    public void tick() {
        if (!active) return;
        lavaDamageCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!active) return;
        if (!config.isLavaDamageEnabled()) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.LAVA) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if player is exempt
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive()) {
            var activeMode = modeManager.getActiveMode();
            if (activeMode.isExempt(player)) {
                event.setCancelled(true);
                return;
            }
        }

        // Cancel vanilla lava damage
        event.setCancelled(true);

        // Check cooldown
        UUID uuid = player.getUniqueId();
        if (lavaDamageCooldowns.containsKey(uuid)) return;

        // Apply custom damage
        double damage = config.getLavaDamagePerTick();
        player.damage(damage);
        player.setNoDamageTicks(0); // Allow rapid damage from other sources

        // Set cooldown
        lavaDamageCooldowns.put(uuid, config.getLavaDamageIntervalTicks());
    }
}
