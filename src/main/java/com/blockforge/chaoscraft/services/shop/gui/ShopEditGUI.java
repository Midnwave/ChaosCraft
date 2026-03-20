package com.blockforge.chaoscraft.services.shop.gui;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.shop.ShopCategory;
import com.blockforge.chaoscraft.services.shop.ShopService;
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

import java.util.List;
import java.util.Set;

/**
 * Edit mode overlay for shop GUIs.
 * When editMode=true in the session, the main shop and category GUIs gain drag-and-drop
 * reordering, right-click section editing, and a sort sub-GUI.
 */
public class ShopEditGUI {

    private static final int SECTION_SETTINGS_SIZE = 27;
    private static final int FILTER_SORT_SIZE = 27;

    // Reserved slots that cannot be used for placement
    private static final Set<Integer> MAIN_RESERVED = Set.of(
            0, 1, 2, 3, 4, 5, 6, 7, 8,     // top border
            16, 17, 25, 26, 34, 35,           // info column
            36, 37, 38, 39, 40, 41, 42, 43, 44, // row 5 border
            45, 46, 47, 48, 49, 50, 51, 52, 53  // bottom nav
    );

    private static final Set<Integer> CATEGORY_RESERVED = Set.of(
            0, 1, 2, 3, 4, 5, 6, 7, 8,       // top row
            45, 46, 47, 48, 49, 50, 51, 52, 53 // bottom row
    );

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;

    public ShopEditGUI(ChaosCraftPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    /**
     * Check if a slot is reserved (glass, navigation, info) in the main shop GUI.
     */
    public boolean isMainSlotReserved(int slot) {
        return MAIN_RESERVED.contains(slot);
    }

    /**
     * Check if a slot is reserved in the category items GUI.
     */
    public boolean isCategorySlotReserved(int slot) {
        return CATEGORY_RESERVED.contains(slot);
    }

    // ========================
    // Section Settings sub-GUI (right-click category in edit mode)
    // ========================

    /**
     * Open a small sub-GUI for editing a category's properties (rename, delete, change icon).
     */
    public void openSectionSettings(Player player, ShopSession session, ShopCategory category) {
        session.setCurrentType(ShopSession.ShopGUIType.EDIT_SECTION_SETTINGS);

        Inventory inv = Bukkit.createInventory(
                new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                SECTION_SETTINGS_SIZE,
                Component.text("Edit: " + category.getId(), NamedTextColor.GOLD, TextDecoration.BOLD)
        );

        ItemStack grayPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SECTION_SETTINGS_SIZE; i++) {
            inv.setItem(i, grayPane);
        }

        // Slot 10: Rename
        inv.setItem(10, createOptionItem(Material.NAME_TAG, "Rename Section",
                "Click to rename this section", NamedTextColor.YELLOW));

        // Slot 12: Change Icon
        inv.setItem(12, createOptionItem(Material.PAINTING, "Change Icon",
                "Click with an item to set as icon", NamedTextColor.AQUA));

        // Slot 14: Delete
        inv.setItem(14, createOptionItem(Material.TNT, "Delete Section",
                "Permanently delete this section and all items", NamedTextColor.RED));

        // Slot 16: Back
        inv.setItem(16, createOptionItem(Material.ARROW, "Back",
                "Return to edit mode", NamedTextColor.GRAY));

        // Slot 4: Current category display
        Material iconMat = Material.matchMaterial(category.getIconMaterial());
        if (iconMat == null) iconMat = Material.CHEST;
        ItemStack display = new ItemStack(iconMat);
        ItemMeta displayMeta = display.getItemMeta();
        displayMeta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(category.getDisplayName())
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        displayMeta.lore(List.of(
                Component.text("ID: " + category.getId(), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(category.getItemCount() + " items", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        display.setItemMeta(displayMeta);
        inv.setItem(4, display);

        player.openInventory(inv);
    }

    // ========================
    // Filter/Sort sub-GUI (filter button in edit mode or normal mode)
    // ========================

    /**
     * Open the sort/filter sub-GUI.
     */
    public void openFilterSort(Player player, ShopSession session) {
        session.setCurrentType(ShopSession.ShopGUIType.FILTER_SORT);

        Inventory inv = Bukkit.createInventory(
                new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                FILTER_SORT_SIZE,
                Component.text("Sort & Filter", NamedTextColor.GOLD, TextDecoration.BOLD)
        );

        ItemStack grayPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < FILTER_SORT_SIZE; i++) {
            inv.setItem(i, grayPane);
        }

        String current = session.getSortMode();

        // Slot 10: Alphabetical
        inv.setItem(10, createSortOption(Material.BOOK, "Alphabetical",
                "Sort items A-Z", "alphabetical", current));

        // Slot 12: Price Ascending
        inv.setItem(12, createSortOption(Material.GOLD_NUGGET, "Price: Low to High",
                "Sort by price ascending", "price_asc", current));

        // Slot 14: Price Descending
        inv.setItem(14, createSortOption(Material.GOLD_BLOCK, "Price: High to Low",
                "Sort by price descending", "price_desc", current));

        // Slot 16: Reset
        inv.setItem(16, createSortOption(Material.BARRIER, "Reset",
                "Reset to default order", "default", current));

        // Slot 22: Back
        inv.setItem(22, createOptionItem(Material.ARROW, "Back",
                "Return to category", NamedTextColor.GRAY));

        player.openInventory(inv);
    }

    // ========================
    // Item builders
    // ========================

    private ItemStack createOptionItem(Material material, String name, String description, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(description, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSortOption(Material material, String name, String description,
                                       String sortId, String currentSort) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        boolean active = sortId.equalsIgnoreCase(currentSort);
        NamedTextColor nameColor = active ? NamedTextColor.GREEN : NamedTextColor.YELLOW;

        meta.displayName(Component.text((active ? "> " : "") + name, nameColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(description, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                active ? Component.text("Currently active", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
                        : Component.text("Click to select", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));
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
}
