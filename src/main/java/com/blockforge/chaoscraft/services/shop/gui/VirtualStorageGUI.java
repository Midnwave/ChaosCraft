package com.blockforge.chaoscraft.services.shop.gui;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.shop.ShopService;
import com.blockforge.chaoscraft.services.shop.VirtualStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Virtual storage GUI (54 slots / 6 rows).
 * Displays items stored via virtual storage when a purchase overflowed the player's inventory.
 * Players can claim items back via left-click (one) or shift-click (all of type).
 */
public class VirtualStorageGUI {

    private static final int SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45; // slots 0-44
    private static final String TITLE = "Shop Storage";

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;

    public VirtualStorageGUI(ChaosCraftPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    /**
     * Open the virtual storage GUI for a player.
     */
    public void open(Player player, ShopSession session) {
        session.setCurrentType(ShopSession.ShopGUIType.VIRTUAL_STORAGE);
        Inventory inv = buildInventory(player, session);
        player.openInventory(inv);
    }

    public Inventory buildInventory(Player player, ShopSession session) {
        Inventory inv = Bukkit.createInventory(
                new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                SIZE,
                Component.text(TITLE, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
        );

        VirtualStorage storage = shopService.getVirtualStorage();
        List<VirtualStorage.VirtualStorageEntry> entries = storage.getItems(player.getUniqueId());

        int page = session.getPage();
        int maxPage = Math.max(0, (entries.size() - 1) / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, maxPage));
        session.setPage(page);

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());

        // Place items in slots 0-44
        for (int i = startIndex; i < endIndex; i++) {
            VirtualStorage.VirtualStorageEntry entry = entries.get(i);
            int slot = i - startIndex;
            ItemStack display = entry.item().clone();
            ItemMeta meta = display.getItemMeta();

            // Add claim instructions to lore
            List<Component> lore = meta.lore() != null ? new java.util.ArrayList<>(meta.lore()) : new java.util.ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Left-click to claim", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Shift-click to claim all of type", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("ID: #" + entry.id(), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            display.setItemMeta(meta);

            inv.setItem(slot, display);
        }

        // Bottom row (45-53): navigation
        ItemStack grayPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        if (page > 0) {
            inv.setItem(45, createNavItem("Previous Page"));
        }

        inv.setItem(49, createItemCountDisplay(entries.size()));

        inv.setItem(52, createCloseButton());

        if (page < maxPage) {
            inv.setItem(53, createNavItem("Next Page"));
        }

        return inv;
    }

    // ========================
    // Item builders
    // ========================

    private ItemStack createNavItem(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItemCountDisplay(int count) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Stored Items: " + count, NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click items above to claim them", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Close", NamedTextColor.RED, TextDecoration.BOLD)
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

    public static int getItemsPerPage() { return ITEMS_PER_PAGE; }
}
