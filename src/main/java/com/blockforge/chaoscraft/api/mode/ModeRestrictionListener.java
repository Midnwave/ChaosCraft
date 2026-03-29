package com.blockforge.chaoscraft.api.mode;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.*;

/**
 * Enforces per-mode player restrictions configured in each mode's YAML:
 * - Block world changes
 * - Block specific commands
 * - Block elytra
 * - Spectator mode on death (no respawn)
 *
 * Water-to-glass is handled separately by mode-specific code (too complex for a generic listener).
 */
public class ModeRestrictionListener implements Listener {

    private final ChaosCraftPlugin plugin;

    // Players in spectator mode because they died during a no-respawn mode
    private final Set<UUID> spectatingPlayers = new HashSet<>();
    // Saved locations for spectating players (restored when mode ends)
    private final Map<UUID, Location> savedLocations = new HashMap<>();

    public ModeRestrictionListener(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    // World Change Restriction
    // ========================

    @EventHandler(priority = EventPriority.HIGH)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) return;

        var mode = manager.getActiveMode();
        if (mode.isExempt(event.getPlayer())) return;

        if (!mode.getModeConfig().isWorldChangeAllowed()) {
            // Teleport them back to the world they came from
            var fromWorld = event.getFrom();
            if (fromWorld != null) {
                Location safeSpawn = fromWorld.getSpawnLocation();
                event.getPlayer().teleport(safeSpawn);
                event.getPlayer().sendMessage(
                        Component.text("You cannot change worlds during this mode!", NamedTextColor.RED));
            }
        }
    }

    // ========================
    // Command Blocking
    // ========================

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) return;

        var mode = manager.getActiveMode();
        Player player = event.getPlayer();
        if (mode.isExempt(player)) return;
        if (player.isOp()) return; // Ops bypass

        List<String> blocked = mode.getModeConfig().getBlockedCommands();
        if (blocked.isEmpty()) return;

        String message = event.getMessage().toLowerCase();
        // Strip leading /
        String cmd = message.startsWith("/") ? message.substring(1) : message;
        // Get just the command name (before any args)
        String cmdName = cmd.split(" ")[0];

        for (String blockedCmd : blocked) {
            if (cmdName.equalsIgnoreCase(blockedCmd.toLowerCase())) {
                event.setCancelled(true);
                player.sendMessage(
                        Component.text("You cannot use /" + blockedCmd + " during this mode!", NamedTextColor.RED));
                return;
            }
        }
    }

    // ========================
    // Elytra Restriction
    // ========================

    @EventHandler(priority = EventPriority.HIGH)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) return;

        var mode = manager.getActiveMode();
        if (mode.isExempt(player)) return;

        if (!mode.getModeConfig().isElytraAllowed() && event.isGliding()) {
            event.setCancelled(true);
            player.sendMessage(
                    Component.text("Elytra is disabled during this mode!", NamedTextColor.RED));
        }
    }

    // ========================
    // Death → Spectator (no respawn)
    // ========================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) return;

        var mode = manager.getActiveMode();
        Player player = event.getEntity();
        if (mode.isExempt(player)) return;

        if (!mode.getModeConfig().isRespawnAllowed()) {
            // Save location before death
            savedLocations.put(player.getUniqueId(), player.getLocation().clone());
            spectatingPlayers.add(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) return;

        var mode = manager.getActiveMode();
        Player player = event.getPlayer();
        if (mode.isExempt(player)) return;

        if (!mode.getModeConfig().isRespawnAllowed() && spectatingPlayers.contains(player.getUniqueId())) {
            // Put in spectator mode after respawn (1 tick delay to avoid conflicts)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(
                            Component.text("You died! Spectating until the mode ends.", NamedTextColor.GRAY));
                }
            }, 1L);
        }
    }

    // ========================
    // Damage Knockback Removal
    // ========================

    /**
     * Cancel all knockback during active modes — players still take damage and get
     * the camera tilt, but their movement/velocity is NOT reduced on hit.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onKnockback(EntityKnockbackByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) return;
        if (manager.getActiveMode().isExempt(player)) return;

        event.setCancelled(true);
    }

    /**
     * Remove damage cooldown + velocity reduction during active modes.
     * - Sets noDamageTicks to 0 so players can take rapid damage
     * - Restores velocity on next tick to prevent movement slowdown on hit
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageVelocity(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) return;
        if (manager.getActiveMode().isExempt(player)) return;

        // Remove damage immunity frames — allow rapid damage during modes
        player.setMaximumNoDamageTicks(0);
        player.setNoDamageTicks(0);

        // Capture velocity before damage is applied
        org.bukkit.util.Vector velocity = player.getVelocity().clone();

        // Restore velocity on next tick (after MC applies its damage velocity reduction)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                player.setVelocity(velocity);
                player.setNoDamageTicks(0);
            }
        }, 1L);
    }

    /**
     * Cancel damage to the Blue Moon boss when it's glowing (invincible during laser).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBossDamage(EntityDamageEvent event) {
        var manager = plugin.getModeManager();
        if (!manager.isAnyModeActive()) return;
        if (!(manager.getActiveMode() instanceof com.blockforge.chaoscraft.modes.bluemoon.BlueMoonMode blueMoon)) return;

        Entity entity = event.getEntity();
        if (entity.getScoreboardTags().contains("chaoscraft_bluemoon_boss")) {
            if (entity.isGlowing()) {
                event.setCancelled(true);
                plugin.debug("[BlueMoon] Boss damage BLOCKED (glowing/invincible) — " + event.getDamage());
            } else {
                plugin.debug("[BlueMoon] Boss damage ALLOWED — " + event.getDamage() + " from " + event.getCause());
            }
        }
    }

    // ========================
    // Mode End Cleanup
    // ========================

    /**
     * Called when a mode ends — restore all spectating players to survival mode.
     */
    public void restoreSpectators() {
        for (UUID uuid : spectatingPlayers) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.SURVIVAL);

                // Teleport to saved location or world spawn
                Location saved = savedLocations.get(uuid);
                if (saved != null && saved.getWorld() != null) {
                    player.teleport(saved.getWorld().getSpawnLocation());
                }
            }
        }
        spectatingPlayers.clear();
        savedLocations.clear();

        // Restore default noDamageTicks for all online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.setMaximumNoDamageTicks(20); // Vanilla default
        }
    }

    public boolean isSpectating(UUID uuid) {
        return spectatingPlayers.contains(uuid);
    }
}
