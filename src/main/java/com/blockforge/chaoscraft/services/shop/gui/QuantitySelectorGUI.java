package com.blockforge.chaoscraft.services.shop.gui;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.shop.ShopItem;
import com.blockforge.chaoscraft.services.shop.ShopService;
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
 * Quantity selector GUI (27 slots / 3 rows).
 * Allows players to adjust the quantity before confirming a purchase or sale.
 * Layout: -64/-16/-1 | preview | +1/+16/+64 | toggle | price | confirm/cancel
 */
public class QuantitySelectorGUI {

    private static final int SIZE = 27;

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;

    public QuantitySelectorGUI(ChaosCraftPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    /**
     * Open the quantity selector for the player's current session.
     */
    public void open(Player player, ShopSession session) {
        session.setCurrentType(ShopSession.ShopGUIType.QUANTITY_SELECTOR);
        Inventory inv = buildInventory(player, session);
        player.openInventory(inv);
    }

    public Inventory buildInventory(Player player, ShopSession session) {
        ShopItem shopItem = session.getSelectedItem();
        boolean buying = session.isBuyMode();
        String titleText = buying ? "Select Quantity - Buy" : "Select Quantity - Sell";

        Inventory inv = Bukkit.createInventory(
                new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                SIZE,
                Component.text(titleText, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
        );

        ItemStack grayPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");

        // Fill with glass
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, grayPane);
        }

        int quantity = session.getSelectedQuantity();
        double unitPrice = buying ? shopItem.getBuyPrice() : shopItem.getSellPrice();
        double totalPrice = unitPrice * quantity;

        // Row 1: quantity adjusters + preview
        inv.setItem(0, createAdjustButton(-64, Material.RED_STAINED_GLASS_PANE));
        inv.setItem(1, createAdjustButton(-16, Material.RED_STAINED_GLASS_PANE));
        inv.setItem(2, createAdjustButton(-1, Material.RED_STAINED_GLASS_PANE));

        // Slot 4: item preview with quantity
        inv.setItem(4, createPreview(shopItem, quantity));

        inv.setItem(6, createAdjustButton(1, Material.GREEN_STAINED_GLASS_PANE));
        inv.setItem(7, createAdjustButton(16, Material.GREEN_STAINED_GLASS_PANE));
        inv.setItem(8, createAdjustButton(64, Material.GREEN_STAINED_GLASS_PANE));

        // Row 2: toggle + price display
        inv.setItem(10, createToggleButton());
        inv.setItem(13, createPriceDisplay(totalPrice, buying));

        // Row 3: cancel + confirm
        inv.setItem(18, createCancelButton());
        inv.setItem(22, createConfirmButton(buying, quantity, totalPrice));

        return inv;
    }

    // ========================
    // Item builders
    // ========================

    private ItemStack createAdjustButton(int amount, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String prefix = amount > 0 ? "+" : "";
        meta.displayName(Component.text(prefix + amount, amount > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPreview(ShopItem shopItem, int quantity) {
        Material material = Material.matchMaterial(shopItem.getMaterialName());
        if (material == null) material = Material.BARRIER;

        ItemStack item = new ItemStack(material, Math.min(Math.max(quantity, 1), 64));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Quantity: " + quantity, NamedTextColor.WHITE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggleButton() {
        ItemStack item = new ItemStack(Material.LEVER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Items / Stacks", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Toggle between adjusting by", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("individual items or full stacks", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPriceDisplay(double totalPrice, boolean buying) {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Total " + (buying ? "Cost" : "Earnings") + ": $" + String.format("%.2f", totalPrice),
                        NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Cancel", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConfirmButton(boolean buying, int quantity, double totalPrice) {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = item.getItemMeta();
        String action = buying ? "Confirm Purchase" : "Confirm Sale";
        meta.displayName(Component.text(action, NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(quantity + "x for $" + String.format("%.2f", totalPrice), NamedTextColor.YELLOW)
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
