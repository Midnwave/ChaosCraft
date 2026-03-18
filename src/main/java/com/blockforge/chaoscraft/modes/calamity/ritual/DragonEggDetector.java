package com.blockforge.chaoscraft.modes.calamity.ritual;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Detects the dragon egg on the exit portal bedrock pillar in The End,
 * plus end crystal placement. When both conditions are met, triggers
 * the Calamity summon sequence instead of a normal dragon respawn.
 *
 * Flow:
 * 1. Player places dragon egg on top of the exit portal bedrock (0, top, 0)
 * 2. Player places end crystals on the obsidian pillars
 * 3. When an EnderCrystal spawns while egg is detected → start Calamity
 * 4. If egg is NOT on the portal → normal dragon respawn (don't interfere)
 */
public class DragonEggDetector implements Listener {

    private final ChaosCraftPlugin plugin;

    public DragonEggDetector(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * When an Ender Crystal spawns in The End, check if the dragon egg
     * is sitting on the exit portal bedrock. If so, hijack the dragon
     * respawn and start Calamity mode instead.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCrystalSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() != EntityType.END_CRYSTAL) return;

        World world = event.getLocation().getWorld();
        if (world == null || world.getEnvironment() != World.Environment.THE_END) return;

        // Don't interfere if a mode is already active
        if (plugin.getModeManager().isAnyModeActive()) return;

        // Check if dragon egg is on top of exit portal bedrock pillar
        if (!isEggOnPortal(world)) return;

        // Cancel the vanilla dragon respawn by removing the crystal
        // and start Calamity mode instead
        plugin.getLogger().info("[Calamity] Dragon egg detected on portal + crystal placed — initiating Calamity summon!");

        // Start Calamity mode
        boolean started = plugin.getModeManager().startMode("calamity");
        if (started) {
            plugin.debug("[Calamity] Mode started via dragon egg ritual.");
        } else {
            plugin.getLogger().warning("[Calamity] Failed to start Calamity mode via ritual.");
        }
    }

    /**
     * Check if the dragon egg block exists on top of the exit portal's
     * bedrock pillar in The End. The exit portal center is at (0, y, 0)
     * where the bedrock pillar rises to ~y=65 (varies). We scan the
     * bedrock column from y=60 to y=75 looking for a dragon egg on top.
     */
    public boolean isEggOnPortal(World world) {
        if (world.getEnvironment() != World.Environment.THE_END) return false;

        // The exit portal is centered at x=0, z=0 in The End
        // The bedrock pillar goes from ~y=0 to ~y=65
        // Check for dragon egg on top of the pillar
        for (int y = 75; y >= 60; y--) {
            Block block = world.getBlockAt(0, y, 0);
            if (block.getType() == Material.DRAGON_EGG) {
                return true;
            }
            // Stop scanning if we hit bedrock — egg should be above this
            if (block.getType() == Material.BEDROCK) {
                // Check the block directly above the bedrock
                Block above = world.getBlockAt(0, y + 1, 0);
                return above.getType() == Material.DRAGON_EGG;
            }
        }
        return false;
    }

    /**
     * Remove the dragon egg from the portal when Calamity starts
     * (the egg becomes an animated BlockDisplay instead).
     */
    public Location removeEggFromPortal(World world) {
        for (int y = 75; y >= 60; y--) {
            Block block = world.getBlockAt(0, y, 0);
            if (block.getType() == Material.DRAGON_EGG) {
                Location eggLocation = block.getLocation().add(0.5, 0.5, 0.5);
                block.setType(Material.AIR);
                return eggLocation;
            }
        }
        // Fallback position above portal
        return new Location(world, 0.5, 68.0, 0.5);
    }
}
