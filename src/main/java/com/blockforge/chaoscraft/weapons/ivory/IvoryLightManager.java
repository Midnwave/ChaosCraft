package com.blockforge.chaoscraft.weapons.ivory;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.itemtags.ItemTagsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IvoryLightManager {

    private final ChaosCraftPlugin plugin;
    private IvoryConfig config;
    private final Map<UUID, Location> playerLightLocations = new HashMap<>();
    private BukkitTask updateTask;

    public IvoryLightManager(ChaosCraftPlugin plugin, IvoryConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (updateTask != null && !updateTask.isCancelled()) updateTask.cancel();
        updateTask = new BukkitRunnable() {
            @Override public void run() { updateAllPlayers(); }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void stop() {
        if (updateTask != null && !updateTask.isCancelled()) updateTask.cancel();
        playerLightLocations.keySet().forEach(this::removeLight);
        playerLightLocations.clear();
    }

    public void updateConfig(IvoryConfig config) { this.config = config; }

    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) updatePlayer(player);
        playerLightLocations.keySet().removeIf(playerId -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) return false;
            Location oldLoc = playerLightLocations.get(playerId);
            if (oldLoc != null) removeLightBlock(oldLoc);
            return true;
        });
    }

    private void updatePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        boolean shouldHaveLight = mainHand != null && mainHand.getType() != Material.AIR
                && (ItemTagsAPI.hasTag(mainHand, config.getTagActive()) || ItemTagsAPI.hasTag(mainHand, config.getTagRage()));
        Location currentLightLoc = playerLightLocations.get(playerId);
        if (shouldHaveLight) {
            Location newLightLoc = findBestLightLocation(player);
            if (newLightLoc != null && (currentLightLoc == null || !isSameBlock(currentLightLoc, newLightLoc))) {
                if (currentLightLoc != null) removeLightBlock(currentLightLoc);
                placeLightBlock(newLightLoc);
                playerLightLocations.put(playerId, newLightLoc);
            }
        } else if (currentLightLoc != null) {
            removeLightBlock(currentLightLoc);
            playerLightLocations.remove(playerId);
        }
    }

    private Location findBestLightLocation(Player player) {
        Location playerLoc = player.getLocation();
        Location eyeLoc = player.getEyeLocation().getBlock().getLocation();
        if (canPlaceLight(eyeLoc)) return eyeLoc;
        Location aboveHead = playerLoc.clone().add(0, 2, 0).getBlock().getLocation();
        if (canPlaceLight(aboveHead)) return aboveHead;
        Location feetLoc = playerLoc.getBlock().getLocation();
        if (canPlaceLight(feetLoc)) return feetLoc;
        Location aboveFeet = playerLoc.clone().add(0, 1, 0).getBlock().getLocation();
        return canPlaceLight(aboveFeet) ? aboveFeet : null;
    }

    private boolean canPlaceLight(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        Material type = loc.getBlock().getType();
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR || type == Material.LIGHT;
    }

    private void placeLightBlock(Location loc) {
        if (loc == null || loc.getWorld() == null || !canPlaceLight(loc)) return;
        Block block = loc.getBlock();
        block.setType(Material.LIGHT, false);
        var data = block.getBlockData();
        if (data instanceof Levelled levelled) {
            levelled.setLevel(15);
            block.setBlockData(data, false);
        }
    }

    private void removeLightBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        if (loc.getBlock().getType() == Material.LIGHT) {
            loc.getBlock().setType(Material.AIR, false);
        }
    }

    private boolean isSameBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || loc1.getWorld() != loc2.getWorld()) return false;
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    public void removeLight(UUID playerId) {
        Location loc = playerLightLocations.remove(playerId);
        if (loc != null) removeLightBlock(loc);
    }

    public void forceUpdate(Player player) { updatePlayer(player); }
}
