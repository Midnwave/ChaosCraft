package com.blockforge.chaoscraft.api.points;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Listens for gameplay events that award Mode Points.
 * Handles: corrupted block breaking, fragment/shard collection, milestones.
 */
public class ModePointsListener implements Listener {

    private final ChaosCraftPlugin plugin;
    private final ModePointsService pointsService;
    private final NamespacedKey fragmentKey;

    public ModePointsListener(ChaosCraftPlugin plugin, ModePointsService pointsService) {
        this.plugin = plugin;
        this.pointsService = pointsService;
        this.fragmentKey = new NamespacedKey(plugin, "mode_fragment");
    }

    // ========================
    // Corrupted block breaking (Corruption Mode)
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!pointsService.isSessionActive()) return;

        Player player = event.getPlayer();
        Material broken = event.getBlock().getType();

        // Check if this is a corruption-placed block
        if (isCorruptionBlock(broken)) {
            String modeName = plugin.getModeManager().getActiveModeName();
            if ("corruption".equals(modeName)) {
                // Award points for breaking corrupted blocks
                pointsService.award(player, PointAction.CORRUPTION_BREAK_CORRUPTED);

                // Check milestone
                int count = pointsService.getTracker().incrementMilestone(
                        player.getUniqueId(), "corruption_breaks");
                int milestone = pointsService.getConfig().getCorruptionBreakMilestone();
                if (count > 0 && count % milestone == 0) {
                    pointsService.award(player, PointAction.CORRUPTION_BREAK_25);
                }

                // Check chunk purify
                int chunkBreaks = pointsService.getTracker().incrementMilestone(
                        player.getUniqueId(), "chunk_" + event.getBlock().getChunk().getX() + "_" + event.getBlock().getChunk().getZ());
                if (chunkBreaks >= pointsService.getConfig().getCorruptionPurifyThreshold()) {
                    pointsService.award(player, PointAction.CORRUPTION_PURIFY_CHUNK);
                }
            }
        }
    }

    // ========================
    // Fragment/Shard pickup
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        if (!pointsService.isSessionActive()) return;

        Item item = event.getItem();
        ItemStack stack = item.getItemStack();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        // Check for fragment/shard tag
        String fragmentType = meta.getPersistentDataContainer()
                .get(fragmentKey, PersistentDataType.STRING);
        if (fragmentType == null) return;

        Player player = event.getPlayer();

        switch (fragmentType) {
            case "chain_fragment" -> pointsService.award(player, PointAction.CHAIN_COLLECT_FRAGMENT);
            case "corruption_shard" -> pointsService.award(player, PointAction.CORRUPTION_COLLECT_SHARD);
        }
    }

    // ========================
    // Fragment item creation (called by attack cleanup)
    // ========================

    /**
     * Create a collectible chain fragment item.
     */
    public ItemStack createChainFragment() {
        ItemStack item = new ItemStack(Material.CHAIN, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Chain Fragment", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("A fragment from a shattered chain attack.", NamedTextColor.GRAY),
                Component.text("Pick up for Mode Points!", NamedTextColor.YELLOW)
        ));
        meta.getPersistentDataContainer().set(fragmentKey, PersistentDataType.STRING, "chain_fragment");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a collectible corruption shard item.
     */
    public ItemStack createCorruptionShard() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Corruption Shard", NamedTextColor.DARK_PURPLE));
        meta.lore(List.of(
                Component.text("A shard of pure corruption energy.", NamedTextColor.GRAY),
                Component.text("Pick up for Mode Points!", NamedTextColor.YELLOW)
        ));
        meta.getPersistentDataContainer().set(fragmentKey, PersistentDataType.STRING, "corruption_shard");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a breakable weak point item (placed as a block, mined to destroy attack).
     * This is a SCULK_CATALYST with a special tag.
     */
    public ItemStack createWeakPointBlock() {
        ItemStack item = new ItemStack(Material.SCULK_CATALYST, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Weak Point", NamedTextColor.RED));
        meta.getPersistentDataContainer().set(fragmentKey, PersistentDataType.STRING, "weak_point");
        item.setItemMeta(meta);
        return item;
    }

    // ========================
    // Helpers
    // ========================

    private boolean isCorruptionBlock(Material material) {
        return material == Material.CRYING_OBSIDIAN
                || material == Material.BLACKSTONE
                || material == Material.DEEPSLATE
                || material == Material.SCULK
                || material == Material.COAL_BLOCK;
    }

    public NamespacedKey getFragmentKey() { return fragmentKey; }
}
