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

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation GUI (27 slots / 3 rows).
 * Shows a receipt with item details, total price, and balance after transaction.
 * Slot 11 = CONFIRM (green wool), Slot 13 = item preview, Slot 15 = CANCEL (red wool).
 */
public class ConfirmGUI {

    private static final int SIZE = 27;

    // Fixed slot assignments
    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_PREVIEW = 13;
    public static final int SLOT_CANCEL = 15;

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;

    public ConfirmGUI(ChaosCraftPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    /**
     * Open the confirmation GUI for the current session's selected item/quantity.
     */
    public void open(Player player, ShopSession session) {
        boolean buying = session.isBuyMode();
        session.setCurrentType(buying
                ? ShopSession.ShopGUIType.CONFIRM_PURCHASE
                : ShopSession.ShopGUIType.CONFIRM_SELL);

        Inventory inv = buildInventory(player, session);
        player.openInventory(inv);
    }

    public Inventory buildInventory(Player player, ShopSession session) {
        ShopItem shopItem = session.getSelectedItem();
        boolean buying = session.isBuyMode();
        int quantity = session.getSelectedQuantity();
        double unitPrice = buying ? shopItem.getBuyPrice() : shopItem.getSellPrice();
        double totalPrice = unitPrice * quantity;
        double balance = shopService.getBalance(player);
        double balanceAfter = buying ? (balance - totalPrice) : (balance + totalPrice);

        String titleText = buying ? "Confirm Purchase" : "Confirm Sale";

        Inventory inv = Bukkit.createInventory(
                new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                SIZE,
                Component.text(titleText, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
        );

        // Fill with glass
        ItemStack grayPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, grayPane);
        }

        // Confirm button with receipt
        inv.setItem(SLOT_CONFIRM, createConfirmButton(shopItem, quantity, totalPrice, balanceAfter, buying));

        // Item preview
        inv.setItem(SLOT_PREVIEW, createPreview(shopItem, quantity));

        // Cancel button
        inv.setItem(SLOT_CANCEL, createCancelButton());

        return inv;
    }

    // ========================
    // Item builders
    // ========================

    private ItemStack createConfirmButton(ShopItem shopItem, int quantity, double totalPrice,
                                          double balanceAfter, boolean buying) {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("CONFIRM", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        String materialDisplay = shopItem.getDisplayName() != null
                ? shopItem.getDisplayName()
                : formatMaterialName(shopItem.getMaterialName());

        lore.add(Component.text("Item: ", NamedTextColor.GRAY)
                .append(Component.text(materialDisplay, NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Quantity: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(quantity), NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Total " + (buying ? "Cost" : "Earnings") + ": ", NamedTextColor.GRAY)
                .append(Component.text("$" + String.format("%.2f", totalPrice), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Balance After: ", NamedTextColor.GRAY)
                .append(Component.text("$" + String.format("%.2f", balanceAfter),
                        balanceAfter >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to confirm", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPreview(ShopItem shopItem, int quantity) {
        Material material = Material.matchMaterial(shopItem.getMaterialName());
        if (material == null) material = Material.BARRIER;

        ItemStack item = new ItemStack(material, Math.min(Math.max(quantity, 1), 64));
        ItemMeta meta = item.getItemMeta();
        if (shopItem.getDisplayName() != null) {
            meta.displayName(Component.text(shopItem.getDisplayName(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("CANCEL", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click to go back", NamedTextColor.GRAY)
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
}
