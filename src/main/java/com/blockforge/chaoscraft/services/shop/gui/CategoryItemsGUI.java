package com.blockforge.chaoscraft.services.shop.gui;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.shop.ShopCategory;
import com.blockforge.chaoscraft.services.shop.ShopItem;
import com.blockforge.chaoscraft.services.shop.ShopService;
import com.blockforge.chaoscraft.services.stats.PlayerStatsService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Category items GUI showing all items in a selected shop category.
 * 54-slot paginated with item details, buy/sell prices, and requirement indicators.
 */
public class CategoryItemsGUI {

    private static final int SIZE = 54;
    private static final int ITEMS_PER_PAGE = 36; // slots 9-44
    private static final int ITEM_START_SLOT = 9;
    private static final int ITEM_END_SLOT = 44;

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;

    public CategoryItemsGUI(ChaosCraftPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    /**
     * Open the category items GUI for a player.
     */
    public void open(Player player, ShopSession session) {
        session.setCurrentType(ShopSession.ShopGUIType.CATEGORY_ITEMS);
        Inventory inv = buildInventory(player, session);
        player.openInventory(inv);
    }

    public Inventory buildInventory(Player player, ShopSession session) {
        ShopCategory category = shopService.getCategory(session.getCurrentCategoryId());
        if (category == null) {
            return Bukkit.createInventory(
                    new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                    SIZE,
                    Component.text("Unknown Category", NamedTextColor.RED)
            );
        }

        Component title = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(category.getDisplayName())
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false);

        Inventory inv = Bukkit.createInventory(
                new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                SIZE, title
        );

        ItemStack grayPane = createDyePane(Material.GRAY_DYE, " ");

        // Top row
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, grayPane);
        }
        // Slot 0: back button
        inv.setItem(0, createBackButton());
        // Slot 4: category name display
        inv.setItem(4, createCategoryDisplay(category));
        // Slot 8: filter button
        inv.setItem(8, createFilterButton());

        // Bottom row
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        // Items (sorted if applicable)
        List<ShopItem> items = new ArrayList<>(category.getItems());
        applySorting(items, session.getSortMode());

        int page = session.getPage();
        int maxPage = Math.max(0, (items.size() - 1) / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, maxPage));
        session.setPage(page);

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        int slot = ITEM_START_SLOT;
        for (int i = startIndex; i < endIndex && slot <= ITEM_END_SLOT; i++) {
            ShopItem shopItem = items.get(i);
            inv.setItem(slot, createItemDisplay(player, shopItem));
            slot++;
        }

        // Navigation
        if (page > 0) {
            inv.setItem(45, createNavItem("Previous Page"));
        }
        inv.setItem(49, createPageIndicator(page + 1, maxPage + 1));
        if (page < maxPage) {
            inv.setItem(53, createNavItem("Next Page"));
        }

        return inv;
    }

    // ========================
    // Item display builder
    // ========================

    private ItemStack createItemDisplay(Player player, ShopItem shopItem) {
        Material material = Material.matchMaterial(shopItem.getMaterialName());
        if (material == null) material = Material.BARRIER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Display name
        if (shopItem.getDisplayName() != null && !shopItem.getDisplayName().isEmpty()) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(shopItem.getDisplayName())
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(Component.text(formatMaterialName(material.name()), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
        }

        List<Component> lore = new ArrayList<>();

        // Shop-specific lore (not applied to the given item)
        for (String line : shopItem.getShopLore()) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());

        // Buy price
        if (shopItem.isBuyable()) {
            lore.add(Component.text("Buy: ", NamedTextColor.GRAY)
                    .append(Component.text("$" + String.format("%.2f", shopItem.getBuyPrice()), NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Buy: ", NamedTextColor.GRAY)
                    .append(Component.text("Not available", NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        }

        // Sell price
        if (shopItem.isSellable()) {
            lore.add(Component.text("Sell: ", NamedTextColor.GRAY)
                    .append(Component.text("$" + String.format("%.2f", shopItem.getSellPrice()), NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Sell: ", NamedTextColor.GRAY)
                    .append(Component.text("Not available", NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        }

        // Requirements
        boolean hasRequirements = false;
        PlayerStatsService stats = plugin.getPlayerStatsService();

        if (shopItem.getKillsRequired() > 0) {
            hasRequirements = true;
            int playerKills = stats != null ? stats.getKills(player.getUniqueId()) : 0;
            boolean met = playerKills >= shopItem.getKillsRequired();
            lore.add(Component.empty());
            Component req = met
                    ? Component.text("Kills: " + shopItem.getKillsRequired(), NamedTextColor.GREEN)
                    : Component.text("Kills: " + shopItem.getKillsRequired(), NamedTextColor.RED, TextDecoration.STRIKETHROUGH);
            lore.add(req.decoration(TextDecoration.ITALIC, false));
        }

        if (shopItem.getSKillsRequired() > 0) {
            if (!hasRequirements) lore.add(Component.empty());
            hasRequirements = true;
            int playerSKills = stats != null ? stats.getSKills(player.getUniqueId()) : 0;
            boolean met = playerSKills >= shopItem.getSKillsRequired();
            Component req = met
                    ? Component.text("S-Kills: " + shopItem.getSKillsRequired(), NamedTextColor.GREEN)
                    : Component.text("S-Kills: " + shopItem.getSKillsRequired(), NamedTextColor.RED, TextDecoration.STRIKETHROUGH);
            lore.add(req.decoration(TextDecoration.ITALIC, false));
        }

        if (shopItem.getSurvivalsRequired() > 0) {
            if (!hasRequirements) lore.add(Component.empty());
            hasRequirements = true;
            int playerSurvivals = stats != null ? stats.getModeSurvivals(player.getUniqueId()) : 0;
            boolean met = playerSurvivals >= shopItem.getSurvivalsRequired();
            Component req = met
                    ? Component.text("Survivals: " + shopItem.getSurvivalsRequired(), NamedTextColor.GREEN)
                    : Component.text("Survivals: " + shopItem.getSurvivalsRequired(), NamedTextColor.RED, TextDecoration.STRIKETHROUGH);
            lore.add(req.decoration(TextDecoration.ITALIC, false));
        }

        if (shopItem.getBadgeRequired() != null && !shopItem.getBadgeRequired().isEmpty()) {
            if (!hasRequirements) lore.add(Component.empty());
            boolean met = player.hasPermission("chaoscraft.badge." + shopItem.getBadgeRequired());
            Component req = met
                    ? Component.text("Badge: " + shopItem.getBadgeRequired(), NamedTextColor.GREEN)
                    : Component.text("Badge: " + shopItem.getBadgeRequired(), NamedTextColor.RED, TextDecoration.STRIKETHROUGH);
            lore.add(req.decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Left-click to buy", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right-click to sell", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ========================
    // Sorting
    // ========================

    private void applySorting(List<ShopItem> items, String sortMode) {
        if (sortMode == null) return;
        switch (sortMode.toLowerCase()) {
            case "alphabetical" -> items.sort(Comparator.comparing(ShopItem::getMaterialName));
            case "price_asc" -> items.sort(Comparator.comparingDouble(ShopItem::getBuyPrice));
            case "price_desc" -> items.sort(Comparator.comparingDouble(ShopItem::getBuyPrice).reversed());
            // "default" and "reset" keep original order
        }
    }

    // ========================
    // Helper item builders
    // ========================

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back to Categories", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCategoryDisplay(ShopCategory category) {
        Material mat = Material.matchMaterial(category.getIconMaterial());
        if (mat == null) mat = Material.NAME_TAG;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(category.getDisplayName())
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(List.of(
                Component.text(category.getItemCount() + " items", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFilterButton() {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Sort & Filter", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click to change sort order", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavItem(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPageIndicator(int current, int total) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Page " + current + "/" + total, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDyePane(Material material, String name) {
        return createGlassPane(material, name);
    }

    private String formatMaterialName(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    // ========================
    // Constants for click routing
    // ========================

    public static int getItemStartSlot() { return ITEM_START_SLOT; }
    public static int getItemEndSlot() { return ITEM_END_SLOT; }
    public static int getItemsPerPage() { return ITEMS_PER_PAGE; }
}
