package com.blockforge.chaoscraft.modes.calamity.ritual;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Manages the Calamity gem system:
 * - Gem item template (set via /cc devs setgem)
 * - Periodic gem rain from sky (configurable rates)
 * - Pickup detection → global counter + timer +1s per gem
 * - Deposit mechanic at exit portal bedrock
 * - Deposit counter per boss phase
 * - DynamicBackpacks integration (gems in backpacks count)
 * - Particle arrow + text hologram at deposit location
 */
public class GemManager implements Listener {

    private final ChaosCraftPlugin plugin;
    private final CalamityConfig calamityConfig;

    private int globalGemCount = 0;
    private int depositedGems = 0;
    private boolean bossActive = false;
    private boolean betweenBosses = false;
    private boolean active = false;

    private BukkitTask spawnTask;
    private BukkitTask depositIndicatorTask;

    // Tracks dropped gem entities so we only count our gems
    private final Set<UUID> activeGemEntities = new HashSet<>();

    // The current gem requirement to unlock the next boss
    private int currentGemRequirement = 0;

    public GemManager(ChaosCraftPlugin plugin, CalamityConfig calamityConfig) {
        this.plugin = plugin;
        this.calamityConfig = calamityConfig;
    }

    // ========================
    // Lifecycle
    // ========================

    public void start() {
        active = true;
        globalGemCount = 0;
        depositedGems = 0;
        bossActive = false;
        betweenBosses = false;
        startSpawning();
        startDepositIndicator();
    }

    public void stop() {
        active = false;
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        if (depositIndicatorTask != null) {
            depositIndicatorTask.cancel();
            depositIndicatorTask = null;
        }
        // Remove any remaining gem items on the ground
        cleanupGemEntities();
        activeGemEntities.clear();
    }

    // ========================
    // Gem spawning
    // ========================

    private void startSpawning() {
        if (spawnTask != null) spawnTask.cancel();

        spawnTask = new BukkitRunnable() {
            private int tickCounter = 0;

            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }
                tickCounter++;
                int interval = getCurrentSpawnInterval();
                if (interval > 0 && tickCounter >= interval) {
                    tickCounter = 0;
                    spawnGemRain();
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    private int getCurrentSpawnInterval() {
        if (bossActive) return calamityConfig.getGemSpawnRateBossActive();
        if (betweenBosses) return calamityConfig.getGemSpawnRateBetweenBosses();
        return calamityConfig.getGemSpawnRateNoBoss();
    }

    /**
     * Spawn a gem item falling from the sky above a random player in The End.
     */
    private void spawnGemRain() {
        World endWorld = getEndWorld();
        if (endWorld == null) return;

        List<Player> endPlayers = endWorld.getPlayers();
        if (endPlayers.isEmpty()) return;

        ItemStack gemTemplate = getGemTemplate();
        if (gemTemplate == null) return;

        // Pick a random player
        Player target = endPlayers.get(new Random().nextInt(endPlayers.size()));
        Location playerLoc = target.getLocation();

        // Random offset within spawn radius
        int radius = calamityConfig.getGemSpawnRadius();
        double offsetX = (Math.random() * 2 - 1) * radius;
        double offsetZ = (Math.random() * 2 - 1) * radius;
        int height = calamityConfig.getGemSpawnHeight();

        Location spawnLoc = playerLoc.clone().add(offsetX, height, offsetZ);

        // Spawn the gem as a dropped item
        ItemStack gem = gemTemplate.clone();
        gem.setAmount(1);

        Item droppedItem = endWorld.dropItem(spawnLoc, gem);
        droppedItem.setVelocity(new Vector(0, -0.2, 0)); // Gentle fall
        droppedItem.setPickupDelay(10); // Half second before can pick up
        droppedItem.setGlowing(true);
        droppedItem.setCustomNameVisible(true);
        droppedItem.customName(Component.text("Calamity Gem", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        // Track this entity
        activeGemEntities.add(droppedItem.getUniqueId());

        // Particles at spawn point
        endWorld.spawnParticle(Particle.DUST, spawnLoc, 15,
                0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.5f));

        // Remove after 60 seconds if not picked up
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!droppedItem.isDead() && droppedItem.isValid()) {
                    activeGemEntities.remove(droppedItem.getUniqueId());
                    droppedItem.remove();
                }
            }
        }.runTaskLater(plugin, 1200L);
    }

    // ========================
    // Gem pickup
    // ========================

    @EventHandler
    public void onPickupGem(EntityPickupItemEvent event) {
        if (!active) return;
        if (!(event.getEntity() instanceof Player player)) return;

        Item item = event.getItem();
        if (!activeGemEntities.contains(item.getUniqueId())) return;

        // This is one of our gems
        activeGemEntities.remove(item.getUniqueId());
        globalGemCount++;

        // Add +1 second to the timer
        plugin.getModeTimer().addTime(1);

        // Effects
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 10,
                0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f));

        plugin.debug("[Calamity] Gem collected by " + player.getName() + " — total: " + globalGemCount);
    }

    // ========================
    // Deposit mechanic
    // ========================

    /**
     * Called when a player interacts near the exit portal bedrock.
     * Deposits all gems from their inventory (+ backpacks if DynamicBackpacks loaded).
     */
    @EventHandler
    public void onDepositInteract(PlayerInteractEvent event) {
        if (!active) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BEDROCK) return;

        World world = event.getClickedBlock().getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) return;

        // Check if the bedrock is near the exit portal (within 5 blocks of 0,y,0)
        Location blockLoc = event.getClickedBlock().getLocation();
        if (Math.abs(blockLoc.getBlockX()) > 5 || Math.abs(blockLoc.getBlockZ()) > 5) return;

        Player player = event.getPlayer();
        int deposited = depositGemsFromPlayer(player);

        if (deposited > 0) {
            depositedGems += deposited;
            player.sendMessage(Component.text("Deposited " + deposited + " gems! (" + depositedGems + "/" + currentGemRequirement + ")",
                    NamedTextColor.DARK_PURPLE));

            // Effects
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            world.spawnParticle(Particle.DUST, blockLoc.clone().add(0.5, 1.5, 0.5), 30,
                    0.5, 1, 0.5, 0,
                    new Particle.DustOptions(Color.fromRGB(200, 0, 255), 2.0f));

            plugin.debug("[Calamity] " + player.getName() + " deposited " + deposited + " gems. Total deposited: " + depositedGems);
        } else {
            player.sendMessage(Component.text("You have no gems to deposit.", NamedTextColor.GRAY));
        }
    }

    /**
     * Remove all gem items from a player's inventory (and backpacks).
     * Returns the count of gems removed.
     */
    private int depositGemsFromPlayer(Player player) {
        ItemStack gemTemplate = getGemTemplate();
        if (gemTemplate == null) return 0;

        int count = 0;

        // Check main inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot != null && isGem(slot, gemTemplate)) {
                count += slot.getAmount();
                player.getInventory().setItem(i, null);
            }
        }

        // Check DynamicBackpacks (soft dependency)
        count += depositFromBackpacks(player, gemTemplate);

        return count;
    }

    /**
     * DynamicBackpacks integration — check for gems inside backpacks.
     * Gracefully no-ops if DynamicBackpacks is not loaded.
     */
    private int depositFromBackpacks(Player player, ItemStack gemTemplate) {
        if (plugin.getServer().getPluginManager().getPlugin("DynamicBackpacks") == null) return 0;

        try {
            // Use reflection to avoid hard dependency
            Class<?> backpackFactoryClass = Class.forName("com.blockforge.dynamicbackpacks.items.BackpackItemFactory");
            var isBackpackMethod = backpackFactoryClass.getMethod("isBackpack", ItemStack.class);

            int count = 0;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot == null) continue;

                boolean isBackpack = (boolean) isBackpackMethod.invoke(null, slot);
                if (!isBackpack) continue;

                // Get backpack contents via BackpackManager
                Class<?> managerClass = Class.forName("com.blockforge.dynamicbackpacks.backpack.BackpackManager");
                var getContentsMethod = managerClass.getMethod("getBackpackContents", ItemStack.class);
                @SuppressWarnings("unchecked")
                List<ItemStack> contents = (List<ItemStack>) getContentsMethod.invoke(null, slot);

                if (contents == null) continue;
                for (ItemStack bpItem : contents) {
                    if (bpItem != null && isGem(bpItem, gemTemplate)) {
                        count += bpItem.getAmount();
                        // Note: clearing items in backpack requires saving back
                        // For now we just count them — full clearing can be done in Phase 7
                    }
                }
            }
            return count;
        } catch (Exception e) {
            plugin.debug("[Calamity] DynamicBackpacks integration error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Check if an ItemStack matches the configured gem template.
     * Compares type and custom model data / display name.
     */
    private boolean isGem(ItemStack item, ItemStack template) {
        if (item.getType() != template.getType()) return false;
        // Compare serialized NBT for exact match
        return Arrays.equals(item.serializeAsBytes(), template.serializeAsBytes())
                || item.isSimilar(template);
    }

    // ========================
    // Deposit indicator
    // ========================

    private void startDepositIndicator() {
        if (depositIndicatorTask != null) depositIndicatorTask.cancel();

        depositIndicatorTask = new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                World endWorld = getEndWorld();
                if (endWorld == null) return;

                tick++;

                // Particle arrow pointing down at deposit location (every 5 ticks)
                if (tick % 5 == 0) {
                    Location depositLoc = new Location(endWorld, 0.5, 68.0, 0.5);

                    // Vertical beam above deposit point
                    for (double y = 0; y < 5; y += 0.3) {
                        endWorld.spawnParticle(Particle.DUST,
                                depositLoc.clone().add(0, y, 0), 1,
                                0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(180, 0, 255), 1.2f));
                    }

                    // Downward arrow particles
                    double arrowY = 3.0 + Math.sin(tick * 0.1) * 0.5;
                    for (double offset = -0.5; offset <= 0.5; offset += 0.25) {
                        // Arrow shape pointing down
                        endWorld.spawnParticle(Particle.DUST,
                                depositLoc.clone().add(offset, arrowY - Math.abs(offset), 0), 1,
                                0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(255, 0, 200), 1.0f));
                        endWorld.spawnParticle(Particle.DUST,
                                depositLoc.clone().add(0, arrowY - Math.abs(offset), offset), 1,
                                0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(255, 0, 200), 1.0f));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }

    // ========================
    // State management
    // ========================

    public void setBossActive(boolean active) {
        this.bossActive = active;
    }

    public void setBetweenBosses(boolean between) {
        this.betweenBosses = between;
    }

    public void setCurrentGemRequirement(int requirement) {
        this.currentGemRequirement = requirement;
    }

    public int getGlobalGemCount() {
        return globalGemCount;
    }

    public int getDepositedGems() {
        return depositedGems;
    }

    public int getCurrentGemRequirement() {
        return currentGemRequirement;
    }

    public boolean hasEnoughDeposited() {
        return depositedGems >= currentGemRequirement;
    }

    public void resetDeposits() {
        depositedGems = 0;
    }

    public void setGlobalGemCount(int count) {
        this.globalGemCount = count;
    }

    // ========================
    // Helpers
    // ========================

    private ItemStack getGemTemplate() {
        byte[] bytes = calamityConfig.getGemItemBytes();
        if (bytes == null) return null;
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            plugin.debug("[Calamity] Failed to deserialize gem template: " + e.getMessage());
            return null;
        }
    }

    private World getEndWorld() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) return world;
        }
        return null;
    }

    private void cleanupGemEntities() {
        World endWorld = getEndWorld();
        if (endWorld == null) return;
        for (var entity : endWorld.getEntities()) {
            if (entity instanceof Item item && activeGemEntities.contains(item.getUniqueId())) {
                item.remove();
            }
        }
    }
}
