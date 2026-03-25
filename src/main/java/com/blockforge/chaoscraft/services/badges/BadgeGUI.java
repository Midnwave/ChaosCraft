package com.blockforge.chaoscraft.services.badges;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 54-slot paginated chest GUI showing all defined badges.
 * Owned badges show their icon material with green name + description + earned date.
 * Unearned badges show GRAY_STAINED_GLASS_PANE with "???" name.
 * Limited expired badges show RED_STAINED_GLASS_PANE.
 */
public class BadgeGUI implements Listener {

    private final ChaosCraftPlugin plugin;
    private final BadgeService badgeService;
    private final Map<UUID, GUISession> openSessions = new HashMap<>();

    private static final int SIZE = 54;
    private static final int BADGES_PER_PAGE = 36;
    private static final int BADGE_START_SLOT = 9;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    public BadgeGUI(ChaosCraftPlugin plugin, BadgeService badgeService) {
        this.plugin = plugin;
        this.badgeService = badgeService;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<BadgeDefinition> allBadges = new ArrayList<>(badgeService.getAllBadges().values());
        int maxPage = Math.max(0, (allBadges.size() - 1) / BADGES_PER_PAGE);
        page = Math.max(0, Math.min(page, maxPage));

        GUISession session = new GUISession(player.getUniqueId(), page, maxPage);
        openSessions.put(player.getUniqueId(), session);

        Inventory inv = Bukkit.createInventory(new BadgeGUIHolder(player.getUniqueId()), SIZE,
                Component.text("Badges", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true));

        // Top and bottom row background
        ItemStack background = createItem(Material.BLACK_DYE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, background);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, background);
        }

        // Info item at slot 4
        inv.setItem(4, createInfoItem(player));

        // Player's owned badges
        Map<String, Long> ownedBadges = badgeService.getPlayerBadges(player.getUniqueId());

        int startIndex = page * BADGES_PER_PAGE;
        int endIndex = Math.min(startIndex + BADGES_PER_PAGE, allBadges.size());

        int slot = BADGE_START_SLOT;
        for (int i = startIndex; i < endIndex; i++) {
            BadgeDefinition badge = allBadges.get(i);
            Long earnedAt = ownedBadges.get(badge.getId());

            ItemStack item;
            if (earnedAt != null) {
                // Owned badge
                item = createOwnedBadgeItem(badge, earnedAt);
            } else if (badge.isExpired()) {
                // Limited + expired
                item = createExpiredBadgeItem(badge);
            } else {
                // Unearned
                item = createUnearnedBadgeItem(badge);
            }

            inv.setItem(slot, item);
            slot++;
            // Skip border columns if using a 9-wide layout with 7 items per row
            // Actually, we fill slots 9-44 contiguously (36 slots)
        }

        // Pagination: previous at slot 45
        if (page > 0) {
            inv.setItem(45, createNavigationItem(Material.ARROW, "Previous Page", page));
        }

        // Pagination: next at slot 53
        if (page < maxPage) {
            inv.setItem(53, createNavigationItem(Material.ARROW, "Next Page", page + 2));
        }

        // Close button at slot 49
        inv.setItem(49, createCloseItem());

        player.openInventory(inv);
    }

    // ========================
    // Item Builders
    // ========================

    private ItemStack createInfoItem(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Your Badges", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        Map<String, Long> owned = badgeService.getPlayerBadges(player.getUniqueId());
        int total = badgeService.getAllBadgeIds().size();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Collected: ", NamedTextColor.YELLOW)
                .append(Component.text(owned.size() + "/" + total, NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Earn badges by completing", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("challenges and surviving modes!", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOwnedBadgeItem(BadgeDefinition badge, long earnedAt) {
        ItemStack item = new ItemStack(badge.getIconMaterial());
        ItemMeta meta = item.getItemMeta();

        // Green colored display name parsed from config
        Component displayName = BadgeService.parseDisplayName(badge.getDisplayName());
        meta.displayName(Component.text("")
                .append(displayName)
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Description
        if (badge.getDescription() != null && !badge.getDescription().isEmpty()) {
            for (String line : wrapText(badge.getDescription(), 30)) {
                lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
        }

        // Earned date
        String dateStr = DATE_FORMAT.format(new Date(earnedAt));
        lore.add(Component.text("Earned: ", NamedTextColor.YELLOW)
                .append(Component.text(dateStr, NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));

        // Limited badge indicator
        if (badge.isLimited()) {
            lore.add(Component.text("Limited Edition", NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, true));
        }

        // Custom model data
        if (badge.getCustomModelData() > 0) {
            meta.setCustomModelData(badge.getCustomModelData());
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUnearnedBadgeItem(BadgeDefinition badge) {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        // Show the actual badge name but in gray with a lock indicator
        Component displayName = BadgeService.parseDisplayName(badge.getDisplayName());
        meta.displayName(Component.text("\u274C ", NamedTextColor.DARK_GRAY) // ❌
                .append(displayName.color(NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Show full description so players know what to work toward
        if (badge.getDescription() != null && !badge.getDescription().isEmpty()) {
            for (String line : wrapText(badge.getDescription(), 30)) {
                lore.add(Component.text(line, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
        }

        lore.add(Component.text("Status: ", NamedTextColor.YELLOW)
                .append(Component.text("Not Obtained", NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));

        if (badge.isLimited()) {
            String expiryStr = DATE_FORMAT.format(new Date(badge.getExpiryDate()));
            lore.add(Component.text("Limited Edition — Expires: ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(expiryStr, NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (badge.getCustomModelData() > 0) {
            meta.setCustomModelData(badge.getCustomModelData());
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createExpiredBadgeItem(BadgeDefinition badge) {
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();

        // Show the actual badge name in dark red with strikethrough
        Component displayName = BadgeService.parseDisplayName(badge.getDisplayName());
        meta.displayName(Component.text("\u26D4 ", NamedTextColor.DARK_RED) // ⛔
                .append(displayName.color(NamedTextColor.DARK_RED).decoration(TextDecoration.STRIKETHROUGH, true))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Show full description
        if (badge.getDescription() != null && !badge.getDescription().isEmpty()) {
            for (String line : wrapText(badge.getDescription(), 30)) {
                lore.add(Component.text(line, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
        }

        lore.add(Component.text("Status: ", NamedTextColor.YELLOW)
                .append(Component.text("Unobtainable", NamedTextColor.DARK_RED))
                .decoration(TextDecoration.ITALIC, false));

        String expiryStr = DATE_FORMAT.format(new Date(badge.getExpiryDate()));
        lore.add(Component.text("Expired: ", NamedTextColor.RED)
                .append(Component.text(expiryStr, NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("This badge is no longer available.", NamedTextColor.DARK_RED)
                .decoration(TextDecoration.ITALIC, true));

        if (badge.getCustomModelData() > 0) {
            meta.setCustomModelData(badge.getCustomModelData());
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationItem(Material material, String name, int targetPage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Close", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    // ========================
    // Text Wrapping
    // ========================

    private List<String> wrapText(String text, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxCharsPerLine && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder();
            }
            currentLine.append(word).append(" ");
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        return lines;
    }

    // ========================
    // Event Handlers
    // ========================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof BadgeGUIHolder)) return;

        event.setCancelled(true);

        GUISession session = openSessions.get(player.getUniqueId());
        if (session == null) return;

        if (event.getClickedInventory() != topInv) return;

        int slot = event.getSlot();

        // Previous page
        if (slot == 45 && session.page > 0) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> open(player, session.page - 1));
            return;
        }

        // Next page
        if (slot == 53 && session.page < session.maxPage) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> open(player, session.page + 1));
            return;
        }

        // Close button
        if (slot == 49) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getTopInventory().getHolder() instanceof BadgeGUIHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // Delay removal by 1 tick to avoid race condition with page changes
            // (new page opens before old close event removes the session)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Only remove if the player doesn't have the GUI open anymore
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof BadgeGUIHolder) return;
                openSessions.remove(player.getUniqueId());
            }, 1L);
        }
    }

    // ========================
    // Inner Classes
    // ========================

    private static class GUISession {
        final UUID playerId;
        final int page;
        final int maxPage;

        GUISession(UUID playerId, int page, int maxPage) {
            this.playerId = playerId;
            this.page = page;
            this.maxPage = maxPage;
        }
    }

    private static class BadgeGUIHolder implements InventoryHolder {
        private final UUID playerId;

        BadgeGUIHolder(UUID playerId) {
            this.playerId = playerId;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
