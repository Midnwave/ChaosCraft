package com.blockforge.chaoscraft.modes.calamity.ritual;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

/**
 * Manages portal mechanics for Calamity mode:
 * - Teleport all players to The End when Calamity starts
 * - Prevent players from leaving The End during active mode
 * - Force respawn in The End (not overworld) on death
 * - ItemsAdder portal block integration (soft dependency)
 * - Cool animation/transition when being teleported
 */
public class PortalManager implements Listener {

    private final ChaosCraftPlugin plugin;
    private final CalamityConfig calamityConfig;
    private boolean active = false;

    // Location where players spawn in The End
    private Location endSpawnLocation;

    public PortalManager(ChaosCraftPlugin plugin, CalamityConfig calamityConfig) {
        this.plugin = plugin;
        this.calamityConfig = calamityConfig;
    }

    // ========================
    // Lifecycle
    // ========================

    public void start() {
        active = true;
        World endWorld = getEndWorld();
        if (endWorld != null) {
            // Default spawn on the End island at (0, 65, 0)
            endSpawnLocation = new Location(endWorld, 0.5, 65.0, 0.5);
            // Find safe spawn point
            findSafeSpawnPoint(endWorld);
        }
    }

    public void stop() {
        active = false;
        endSpawnLocation = null;
    }

    // ========================
    // Teleport all players to End
    // ========================

    /**
     * Teleport all online players to The End with a cool transition.
     * Players not in The End get the full animation.
     * Players already in The End just see the title.
     */
    public void teleportAllToEnd() {
        World endWorld = getEndWorld();
        if (endWorld == null) {
            plugin.getLogger().warning("[Calamity] The End world not found! Cannot teleport players.");
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                // Already in The End — just show title
                showCalamityTitle(player);
                continue;
            }

            // Teleport with animation
            teleportWithAnimation(player, endWorld);
        }
    }

    /**
     * Teleport a single player to The End with a cinematic transition.
     */
    private void teleportWithAnimation(Player player, World endWorld) {
        // Phase 1: Dark screen + sound
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
        player.showTitle(Title.title(
                Component.empty(),
                Component.text("You are being summoned...", NamedTextColor.DARK_PURPLE),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1500), Duration.ofMillis(500))
        ));

        // Phase 2: Actual teleport after short delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !active) return;

                Location destination = endSpawnLocation != null ? endSpawnLocation : new Location(endWorld, 0.5, 65.0, 0.5);

                player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 1.2f);
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.8f);

                // Spawn arrival particles
                endWorld.spawnParticle(Particle.REVERSE_PORTAL,
                        player.getLocation().add(0, 1, 0),
                        50, 1, 1, 1, 0.1);
                endWorld.spawnParticle(Particle.DUST,
                        player.getLocation().add(0, 1, 0),
                        20, 0.5, 0.5, 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.5f));

                showCalamityTitle(player);
            }
        }.runTaskLater(plugin, 40L); // 2 second delay
    }

    /**
     * Teleport a single player who just joined to The End.
     */
    public void teleportJoiningPlayer(Player player) {
        if (!active) return;
        if (player.getWorld().getEnvironment() == World.Environment.THE_END) return;

        World endWorld = getEndWorld();
        if (endWorld == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !active) return;
                Location destination = endSpawnLocation != null ? endSpawnLocation : new Location(endWorld, 0.5, 65.0, 0.5);
                player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
                showCalamityTitle(player);
            }
        }.runTaskLater(plugin, 60L); // 3 second delay for joining player
    }

    private void showCalamityTitle(Player player) {
        player.showTitle(Title.title(
                Component.text("CALAMITY", NamedTextColor.DARK_RED),
                Component.text("The End awaits your ruin.", NamedTextColor.DARK_PURPLE),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
        ));
    }

    // ========================
    // Prevent leaving The End
    // ========================

    /**
     * Block portal usage that would take a player OUT of The End while Calamity is active.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!active) return;

        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;

        // Block any portal that would move them out of The End
        if (event.getTo() != null && event.getTo().getWorld() != null
                && event.getTo().getWorld().getEnvironment() != World.Environment.THE_END) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot leave The End during Calamity!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 1.0f, 0.5f);
        }
    }

    /**
     * Block teleport commands/events that would move a player out of The End.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!active) return;

        // Don't interfere with our own PLUGIN teleports
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;

        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;

        // Allow if staying in The End
        if (event.getTo() != null && event.getTo().getWorld() != null
                && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) return;

        // Block leaving The End via other means (ender pearl into portal, etc.)
        // Exempt players with admin permission can bypass
        if (player.hasPermission("chaoscraft.admin")) return;

        event.setCancelled(true);
        player.sendMessage(Component.text("You cannot leave The End during Calamity!", NamedTextColor.RED));
    }

    // ========================
    // Force End respawn on death
    // ========================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!active) return;

        Player player = event.getPlayer();

        // Force respawn in The End
        World endWorld = getEndWorld();
        if (endWorld != null && endSpawnLocation != null) {
            event.setRespawnLocation(endSpawnLocation);

            // Delayed effects after respawn
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        player.sendMessage(Component.text("You respawn in The End. There is no escape.", NamedTextColor.DARK_PURPLE));
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    // ========================
    // ItemsAdder portal integration
    // ========================

    /**
     * Attempts to place ItemsAdder portal blocks around the exit portal.
     * This is a soft dependency — gracefully no-ops if ItemsAdder isn't loaded.
     */
    public void spawnItemsAdderPortals() {
        if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") == null) {
            plugin.debug("[Calamity] ItemsAdder not loaded — skipping portal block placement.");
            return;
        }

        String blockId = calamityConfig.getPortalItemsAdderId();
        if (blockId.isEmpty()) return;

        try {
            // Use reflection to place ItemsAdder custom blocks
            Class<?> customBlockClass = Class.forName("dev.lone.itemsadder.api.CustomBlock");
            var placeMethod = customBlockClass.getMethod("place", String.class, Location.class);

            World endWorld = getEndWorld();
            if (endWorld == null) return;

            // Place portal blocks around the exit portal (ring pattern)
            Location center = new Location(endWorld, 0, 65, 0);
            int[][] portalOffsets = {
                    {3, 0}, {-3, 0}, {0, 3}, {0, -3},
                    {2, 2}, {-2, 2}, {2, -2}, {-2, -2}
            };

            for (int[] offset : portalOffsets) {
                Location loc = center.clone().add(offset[0], 0, offset[1]);
                if (loc.getBlock().getType() == Material.AIR) {
                    placeMethod.invoke(null, blockId, loc);
                }
            }

            plugin.debug("[Calamity] ItemsAdder portal blocks placed.");
        } catch (Exception e) {
            plugin.debug("[Calamity] ItemsAdder portal placement failed: " + e.getMessage());
        }
    }

    /**
     * Remove ItemsAdder portal blocks on cleanup.
     */
    public void removeItemsAdderPortals() {
        World endWorld = getEndWorld();
        if (endWorld == null) return;

        // Just set the positions back to air
        Location center = new Location(endWorld, 0, 65, 0);
        int[][] portalOffsets = {
                {3, 0}, {-3, 0}, {0, 3}, {0, -3},
                {2, 2}, {-2, 2}, {2, -2}, {-2, -2}
        };

        for (int[] offset : portalOffsets) {
            Location loc = center.clone().add(offset[0], 0, offset[1]);
            // Only clear if it's not natural terrain
            if (loc.getBlock().getType() != Material.END_STONE && loc.getBlock().getType() != Material.BEDROCK) {
                loc.getBlock().setType(Material.AIR);
            }
        }
    }

    // ========================
    // Helpers
    // ========================

    private World getEndWorld() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) return world;
        }
        return null;
    }

    private void findSafeSpawnPoint(World endWorld) {
        // Find the highest solid block at 0,0 for safe spawn
        for (int y = 100; y > 50; y--) {
            if (endWorld.getBlockAt(0, y, 0).getType().isSolid()
                    && endWorld.getBlockAt(0, y + 1, 0).getType().isAir()
                    && endWorld.getBlockAt(0, y + 2, 0).getType().isAir()) {
                endSpawnLocation = new Location(endWorld, 0.5, y + 1.0, 0.5);
                return;
            }
        }
    }

    public boolean isActive() {
        return active;
    }

    public Location getEndSpawnLocation() {
        return endSpawnLocation;
    }
}
