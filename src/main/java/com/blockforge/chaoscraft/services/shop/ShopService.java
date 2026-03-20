package com.blockforge.chaoscraft.services.shop;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.stats.PlayerStatsService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.logging.Level;

/**
 * Main service for the ChaosCraft shop system.
 * Handles buying, selling, requirement checks, and Vault economy integration.
 */
public class ShopService {

    private final ChaosCraftPlugin plugin;
    private ShopConfig config;
    private VirtualStorage virtualStorage;
    private Economy economy;

    public ShopService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    // Lifecycle
    // ========================

    public void initialize() {
        // Setup Vault economy
        if (!setupEconomy()) {
            plugin.getLogger().warning("[Shop] Vault economy not found! Shop system will be limited.");
        }

        config = new ShopConfig(plugin);
        config.load();

        virtualStorage = new VirtualStorage(plugin);
        virtualStorage.initialize();

        plugin.getLogger().info("[Shop] Shop service initialized.");
    }

    public void reload() {
        if (config != null) config.load();
    }

    public void shutdown() {
        if (virtualStorage != null) virtualStorage.shutdown();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return true;
    }

    // ========================
    // Categories
    // ========================

    public List<ShopCategory> getCategories() {
        return config.getCategories();
    }

    public ShopCategory getCategory(String id) {
        return config.getCategory(id);
    }

    // ========================
    // Buy
    // ========================

    /**
     * Attempt to purchase an item for a player.
     * Checks balance, requirements, deducts money, gives items (or stores in virtual storage).
     * Returns a result message component.
     */
    public Component buyItem(Player player, ShopItem shopItem, int quantity) {
        if (!shopItem.isBuyable()) {
            return Component.text("This item is not available for purchase.", NamedTextColor.RED);
        }

        if (economy == null) {
            return Component.text("Economy system is not available.", NamedTextColor.RED);
        }

        // Check requirements
        if (!canPlayerBuy(player, shopItem)) {
            return Component.text("You do not meet the requirements to buy this item.", NamedTextColor.RED);
        }

        double totalCost = shopItem.getBuyPrice() * quantity;
        double balance = economy.getBalance(player);

        if (balance < totalCost) {
            return Component.text("Insufficient funds. You need $" + String.format("%.2f", totalCost)
                    + " but only have $" + String.format("%.2f", balance) + ".", NamedTextColor.RED);
        }

        // Deduct money
        EconomyResponse response = economy.withdrawPlayer(player, totalCost);
        if (!response.transactionSuccess()) {
            return Component.text("Transaction failed: " + response.errorMessage, NamedTextColor.RED);
        }

        // Create the item(s)
        Material material = Material.matchMaterial(shopItem.getMaterialName());
        if (material == null) {
            // Refund on bad material
            economy.depositPlayer(player, totalCost);
            plugin.getLogger().warning("[Shop] Invalid material: " + shopItem.getMaterialName());
            return Component.text("Invalid item configuration. You have been refunded.", NamedTextColor.RED);
        }

        // Give items in stacks
        int remaining = quantity;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, material.getMaxStackSize());
            ItemStack item = new ItemStack(material, stackSize);

            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                // Store overflow in virtual storage
                for (ItemStack leftover : overflow.values()) {
                    virtualStorage.addItem(player.getUniqueId(), leftover);
                }
                player.sendMessage(Component.text("Some items were sent to your virtual storage (/shop storage).",
                        NamedTextColor.YELLOW));
            }
            remaining -= stackSize;
        }

        return Component.text("Purchased " + quantity + "x " + formatMaterialName(material.name())
                + " for $" + String.format("%.2f", totalCost) + "!", NamedTextColor.GREEN);
    }

    // ========================
    // Sell
    // ========================

    /**
     * Attempt to sell items from the player's inventory.
     * Returns a result message component.
     */
    public Component sellItem(Player player, Material material, int quantity) {
        if (economy == null) {
            return Component.text("Economy system is not available.", NamedTextColor.RED);
        }

        // Find the sell price from any category
        ShopItem shopItem = findSellableItem(material.name());
        if (shopItem == null || !shopItem.isSellable()) {
            return Component.text("This item cannot be sold.", NamedTextColor.RED);
        }

        // Count how many the player actually has
        int playerHas = countMaterial(player, material);
        if (playerHas <= 0) {
            return Component.text("You don't have any of this item.", NamedTextColor.RED);
        }

        int toSell = Math.min(quantity, playerHas);
        double totalEarnings = shopItem.getSellPrice() * toSell;

        // Remove items from inventory
        removeMaterial(player, material, toSell);

        // Add money
        economy.depositPlayer(player, totalEarnings);

        return Component.text("Sold " + toSell + "x " + formatMaterialName(material.name())
                + " for $" + String.format("%.2f", totalEarnings) + "!", NamedTextColor.GREEN);
    }

    // ========================
    // Requirement checks
    // ========================

    /**
     * Check if a player meets all requirements to buy a shop item.
     */
    public boolean canPlayerBuy(Player player, ShopItem shopItem) {
        PlayerStatsService statsService = plugin.getPlayerStatsService();
        if (statsService != null) {
            UUID uuid = player.getUniqueId();
            if (shopItem.getKillsRequired() > 0 && statsService.getKills(uuid) < shopItem.getKillsRequired()) {
                return false;
            }
            if (shopItem.getSKillsRequired() > 0 && statsService.getSKills(uuid) < shopItem.getSKillsRequired()) {
                return false;
            }
            if (shopItem.getSurvivalsRequired() > 0 && statsService.getModeSurvivals(uuid) < shopItem.getSurvivalsRequired()) {
                return false;
            }
        }

        // Badge check — uses a badge service if available
        if (shopItem.getBadgeRequired() != null && !shopItem.getBadgeRequired().isEmpty()) {
            // Badge service integration point — for now, check via permission node
            if (!player.hasPermission("chaoscraft.badge." + shopItem.getBadgeRequired())) {
                return false;
            }
        }

        return true;
    }

    // ========================
    // Economy helpers
    // ========================

    public double getBalance(Player player) {
        if (economy == null) return 0;
        return economy.getBalance(player);
    }

    /**
     * Parse the rank placeholder for display in the shop GUI.
     */
    public String getRankDisplay(Player player) {
        String placeholder = config.getRankPlaceholder();
        if (placeholder == null || placeholder.isEmpty()) return "";

        // Attempt PAPI parsing
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    // ========================
    // Virtual storage access
    // ========================

    public VirtualStorage getVirtualStorage() {
        return virtualStorage;
    }

    // ========================
    // Config access
    // ========================

    public ShopConfig getConfig() {
        return config;
    }

    public Economy getEconomy() {
        return economy;
    }

    // ========================
    // Utility
    // ========================

    private ShopItem findSellableItem(String materialName) {
        for (ShopCategory category : config.getCategories()) {
            for (ShopItem item : category.getItems()) {
                if (item.getMaterialName().equalsIgnoreCase(materialName) && item.isSellable()) {
                    return item;
                }
            }
        }
        return null;
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void removeMaterial(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == material) {
                int take = Math.min(remaining, stack.getAmount());
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
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
