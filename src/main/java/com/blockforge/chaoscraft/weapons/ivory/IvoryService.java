package com.blockforge.chaoscraft.weapons.ivory;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.itemtags.ItemTagsAPI;
import com.blockforge.chaoscraft.weapons.ivory.abilities.AbilityManager;
import com.github.retrooper.packetevents.PacketEvents;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class IvoryService implements Listener {

    private final ChaosCraftPlugin plugin;
    private IvoryConfig config;
    private IvoryStateManager stateManager;
    private IvoryAttackHandler attackHandler;
    private IvorySoundManager soundManager;
    private IvoryEffectsManager effectsManager;
    private IvoryLightManager lightManager;
    private AbilityManager abilityManager;
    private IvoryPacketListener packetListener;
    private boolean packetEventsAvailable = false;

    public IvoryService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadConfig();
        if (!config.isEnabled()) {
            plugin.getLogger().info("Celestial Ivory is disabled in configuration.");
            return;
        }

        stateManager = new IvoryStateManager(plugin, config);
        soundManager = new IvorySoundManager(plugin, config);
        effectsManager = new IvoryEffectsManager(plugin, config);
        attackHandler = new IvoryAttackHandler(plugin, config, stateManager, soundManager, effectsManager);
        lightManager = new IvoryLightManager(plugin, config);
        abilityManager = new AbilityManager(plugin, config, stateManager, effectsManager);
        lightManager.start();

        Bukkit.getPluginManager().registerEvents(stateManager, plugin);
        Bukkit.getPluginManager().registerEvents(attackHandler, plugin);
        Bukkit.getPluginManager().registerEvents(abilityManager, plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerPacketEvents();

        Logger log = plugin.getLogger();
        log.info("Celestial Ivory service initialized.");
        log.info("  - States: OFF, CHARGING, ACTIVE, RAGE");
        log.info("  - Abilities: " + config.getAbilityOrder().size() + " configured");
        log.info("  - PacketEvents: " + (packetEventsAvailable ? "ACTIVE" : "NOT AVAILABLE"));
    }

    private void registerPacketEvents() {
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            packetListener = new IvoryPacketListener(plugin, config, stateManager);
            PacketEvents.getAPI().getEventManager().registerListener(packetListener);
            packetEventsAvailable = true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("PacketEvents not found - using fallback swing prevention (Mining Fatigue)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register PacketEvents listener: " + e.getMessage());
        }
    }

    public void loadConfig() {
        var yaml = plugin.yaml("weapons/celestial_ivory.yml");
        if (yaml == null) {
            plugin.saveResource("weapons/celestial_ivory.yml", false);
            plugin.reloadAllYaml();
            yaml = plugin.yaml("weapons/celestial_ivory.yml");
        }
        if (yaml == null) {
            var configFile = new File(plugin.getDataFolder(), "weapons/celestial_ivory.yml");
            if (configFile.exists()) {
                yaml = YamlConfiguration.loadConfiguration(configFile);
                plugin.getLogger().info("Loaded celestial_ivory.yml directly from file");
            } else {
                plugin.getLogger().warning("celestial_ivory.yml not found! Using defaults.");
                yaml = new YamlConfiguration();
            }
        }
        if (config == null) {
            config = new IvoryConfig(yaml);
        } else {
            config.load(yaml);
        }
        plugin.getLogger().info("Ivory tags loaded: off=" + config.getTagOff()
                + ", charging=" + config.getTagCharging()
                + ", active=" + config.getTagActive()
                + ", rage=" + config.getTagRage());
    }

    public void reload() {
        loadConfig();
        plugin.getLogger().info("Ivory tags: OFF=" + config.getTagOff()
                + ", CHARGING=" + config.getTagCharging()
                + ", ACTIVE=" + config.getTagActive()
                + ", RAGE=" + config.getTagRage());
        if (stateManager != null) stateManager.updateConfig(config);
        if (attackHandler != null) attackHandler.updateConfig(config);
        if (soundManager != null) soundManager.updateConfig(config);
        if (effectsManager != null) effectsManager.updateConfig(config);
        if (lightManager != null) lightManager.updateConfig(config);
        if (abilityManager != null) {
            abilityManager.updateConfig(config);
            abilityManager.reload();
        }
        plugin.getLogger().info("Celestial Ivory configuration reloaded.");
    }

    public void shutdown() {
        if (stateManager != null) stateManager.cleanup();
        if (attackHandler != null) attackHandler.cleanup();
        if (lightManager != null) lightManager.stop();
        if (abilityManager != null) abilityManager.shutdown();
        if (packetEventsAvailable && packetListener != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            } catch (Exception ignored) {}
        }
    }

    public boolean isPacketEventsAvailable() {
        return packetEventsAvailable;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!config.isEnabled()) return;
        var droppedItem = event.getItemDrop();
        var item = droppedItem.getItemStack();
        if (isActiveIvory(item)) {
            stateManager.transitionToOff(item);
            droppedItem.setItemStack(item);
            stateManager.cancelCharging(event.getPlayer());
            stateManager.cancelRage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        if (!config.isEnabled()) return;
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        int previousSlot = event.getPreviousSlot();
        ItemStack previousItem = inv.getItem(previousSlot);
        if (previousItem != null && isActiveIvory(previousItem)) {
            stateManager.cancelCharging(player);
            stateManager.cancelRage(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack currentItem = inv.getItem(previousSlot);
                if (currentItem != null && isActiveIvory(currentItem)) {
                    stateManager.transitionToOff(currentItem);
                    inv.setItem(previousSlot, currentItem);
                    player.updateInventory();
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        if (!config.isEnabled()) return;
        var action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        boolean isActive = ItemTagsAPI.hasTag(item, config.getTagActive());
        if (!isActive) return;

        if (player.isSneaking()) {
            if (abilityManager != null) {
                event.setCancelled(true);
                abilityManager.useSelectedAbility(player);
            }
        } else {
            String activationMode = config.getStellarRageActivationMode();
            if ("right_click".equalsIgnoreCase(activationMode) && abilityManager != null) {
                event.setCancelled(true);
                abilityManager.triggerStellarRage(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (stateManager != null) {
            stateManager.cancelCharging(player);
            stateManager.cancelRage(player);
        }
        if (lightManager != null) {
            lightManager.removeLight(player.getUniqueId());
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && isActiveIvory(mainHand)) {
            stateManager.transitionToOff(mainHand);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!config.isEnabled()) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.CRAFTING) return;
        if (!"inventory_key".equalsIgnoreCase(config.getStellarRageActivationMode())) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && ItemTagsAPI.hasTag(mainHand, config.getTagActive())) {
            event.setCancelled(true);
            if (abilityManager != null) {
                abilityManager.triggerStellarRage(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!config.isEnabled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (isActiveIvory(item)) {
            stateManager.transitionToOff(item);
            event.getItem().setItemStack(item);
        }
    }

    public boolean isIvoryWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return ItemTagsAPI.hasAny(item, config.getTagOff(), config.getTagCharging(), config.getTagActive(), config.getTagRage());
    }

    public boolean isActiveIvory(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return ItemTagsAPI.hasAny(item, config.getTagCharging(), config.getTagActive(), config.getTagRage());
    }

    public ItemStack createIvoryWeapon() {
        return stateManager.createIvoryItem(IvoryState.OFF);
    }

    public IvoryConfig getConfig() { return config; }
    public IvoryStateManager getStateManager() { return stateManager; }
    public IvoryAttackHandler getAttackHandler() { return attackHandler; }
    public IvorySoundManager getSoundManager() { return soundManager; }
    public IvoryEffectsManager getEffectsManager() { return effectsManager; }
    public AbilityManager getAbilityManager() { return abilityManager; }

    public enum IvoryState {
        OFF, CHARGING, ACTIVE, RAGE
    }
}
