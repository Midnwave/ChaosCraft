package com.blockforge.chaoscraft.modes.tutorial;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.*;

/**
 * Listens for Bukkit events to track tutorial step completion.
 * Registered when Tutorial Mode starts, unregistered on end.
 */
public class TutorialListener implements Listener {

    private final ChaosCraftPlugin plugin;
    private final TutorialTracker tracker;

    public TutorialListener(ChaosCraftPlugin plugin, TutorialTracker tracker) {
        this.plugin = plugin;
        this.tracker = tracker;
    }

    // ========================
    // Block Break
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        Material broken = event.getBlock().getType();
        // For ores, the drop is different from the block — check block type directly
        tracker.incrementProgress(player, TutorialStepType.BLOCK_BREAK, broken);
    }

    // ========================
    // Craft Item
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        Material crafted = event.getRecipe().getResult().getType();
        int amount = event.getRecipe().getResult().getAmount();

        // Pass actual crafted amount (e.g. planks give 4 per craft)
        tracker.incrementProgress(player, TutorialStepType.CRAFT_ITEM, crafted, amount);
    }

    // ========================
    // Block Place
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        Material placed = event.getBlock().getType();
        tracker.incrementProgress(player, TutorialStepType.BLOCK_PLACE, placed);
    }

    // ========================
    // Furnace Extract
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        Material smelted = event.getItemType();
        // FurnaceExtractEvent fires per stack extracted — count as amount extracted
        int amount = event.getItemAmount();
        for (int i = 0; i < amount; i++) {
            if (tracker.incrementProgress(player, TutorialStepType.FURNACE_EXTRACT, smelted)) {
                break; // Step was completed, stop counting
            }
        }
    }

    // ========================
    // Player Interact (hoe tilling, shearing, using items)
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        TutorialStep step = tracker.getCurrentStep(player);
        if (step == null || step.getType() != TutorialStepType.PLAYER_INTERACT) return;

        // Tilling dirt (hoe on dirt/grass)
        if (event.getClickedBlock() != null) {
            Material blockType = event.getClickedBlock().getType();
            Material itemInHand = event.getItem() != null ? event.getItem().getType() : Material.AIR;

            // Hoe tilling
            if ((blockType == Material.DIRT || blockType == Material.GRASS_BLOCK)
                    && (itemInHand == Material.WOODEN_HOE || itemInHand == Material.STONE_HOE
                    || itemInHand == Material.IRON_HOE || itemInHand == Material.GOLDEN_HOE
                    || itemInHand == Material.DIAMOND_HOE || itemInHand == Material.NETHERITE_HOE)) {
                tracker.incrementProgress(player, TutorialStepType.PLAYER_INTERACT, Material.FARMLAND);
            }
        }
    }

    // ========================
    // Shearing (separate event)
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        // Shearing counts as PLAYER_INTERACT with no specific material
        tracker.incrementProgress(player, TutorialStepType.PLAYER_INTERACT, Material.AIR);
    }

    // ========================
    // Entity Kill
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        tracker.incrementProgress(player, TutorialStepType.ENTITY_KILL, Material.AIR);
    }

    // ========================
    // Player Move (distance / Y threshold)
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        TutorialStep step = tracker.getCurrentStep(player);
        if (step == null || step.getType() != TutorialStepType.PLAYER_MOVE) return;

        if (event.getFrom().getWorld() != null && event.getTo() != null) {
            tracker.incrementMoveProgress(player, event.getFrom(), event.getTo());
        }
    }

    // ========================
    // Fishing
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        tracker.incrementProgress(player, TutorialStepType.PLAYER_FISH, Material.AIR);
    }

    // ========================
    // Consume Food
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsumeFood(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!tracker.isTracking(player)) return;
        if (isExempt(player)) return;

        Material consumed = event.getItem().getType();
        tracker.incrementProgress(player, TutorialStepType.CONSUME_FOOD, consumed);
    }

    // ========================
    // Helper
    // ========================

    private boolean isExempt(Player player) {
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive()) {
            var activeMode = modeManager.getActiveMode();
            if (activeMode.isExempt(player)) return true;
        }
        return player.hasPermission("chaoscraft.mode.exempt");
    }
}
