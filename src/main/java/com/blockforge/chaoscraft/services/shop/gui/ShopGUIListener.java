package com.blockforge.chaoscraft.services.shop.gui;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.shop.ShopCategory;
import com.blockforge.chaoscraft.services.shop.ShopItem;
import com.blockforge.chaoscraft.services.shop.ShopService;
import com.blockforge.chaoscraft.services.shop.VirtualStorage;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Master listener for all shop GUI interactions.
 * Routes click events based on the player's current ShopGUIType to the appropriate handler.
 * Uses a custom InventoryHolder to identify shop inventories.
 */
public class ShopGUIListener implements Listener {

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;
    private final Map<UUID, ShopSession> sessions = new HashMap<>();

    // GUI handlers
    private final MainShopGUI mainShopGUI;
    private final CategoryItemsGUI categoryItemsGUI;
    private final QuantitySelectorGUI quantitySelectorGUI;
    private final ConfirmGUI confirmGUI;
    private final VirtualStorageGUI virtualStorageGUI;
    private final ShopEditGUI shopEditGUI;

    public ShopGUIListener(ChaosCraftPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
        this.mainShopGUI = new MainShopGUI(plugin, shopService);
        this.categoryItemsGUI = new CategoryItemsGUI(plugin, shopService);
        this.quantitySelectorGUI = new QuantitySelectorGUI(plugin, shopService);
        this.confirmGUI = new ConfirmGUI(plugin, shopService);
        this.virtualStorageGUI = new VirtualStorageGUI(plugin, shopService);
        this.shopEditGUI = new ShopEditGUI(plugin, shopService);
    }

    // ========================
    // Public API (open GUIs)
    // ========================

    public void openMainShop(Player player) {
        ShopSession session = getOrCreateSession(player);
        session.setPage(0);
        session.setEditMode(false);
        mainShopGUI.open(player, session);
        playSound(player, shopService.getConfig().getSoundOpen());
    }

    public void openMainShopEditMode(Player player) {
        ShopSession session = getOrCreateSession(player);
        session.setPage(0);
        session.setEditMode(true);
        session.setCurrentType(ShopSession.ShopGUIType.EDIT_MAIN);
        mainShopGUI.open(player, session);
    }

    public void openVirtualStorage(Player player) {
        ShopSession session = getOrCreateSession(player);
        session.setPage(0);
        virtualStorageGUI.open(player, session);
    }

    public ShopSession getOrCreateSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), uuid -> new ShopSession(uuid));
    }

    public ShopSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public void removeSession(UUID uuid) {
        sessions.remove(uuid);
    }

    // ========================
    // GUI handlers
    // ========================

    public MainShopGUI getMainShopGUI() { return mainShopGUI; }
    public CategoryItemsGUI getCategoryItemsGUI() { return categoryItemsGUI; }
    public ShopEditGUI getShopEditGUI() { return shopEditGUI; }

    // ========================
    // Inventory Click
    // ========================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity whoClicked = event.getWhoClicked();
        if (!(whoClicked instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof ShopInventoryHolder)) return;

        event.setCancelled(true);

        ShopSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        // Only process clicks on the top inventory
        if (event.getClickedInventory() != topInv) return;

        int slot = event.getSlot();

        switch (session.getCurrentType()) {
            case MAIN_CATEGORIES -> handleMainCategoriesClick(player, session, slot, event);
            case CATEGORY_ITEMS -> handleCategoryItemsClick(player, session, slot, event);
            case QUANTITY_SELECTOR -> handleQuantitySelectorClick(player, session, slot, event);
            case CONFIRM_PURCHASE, CONFIRM_SELL -> handleConfirmClick(player, session, slot, event);
            case VIRTUAL_STORAGE -> handleVirtualStorageClick(player, session, slot, event);
            case EDIT_MAIN -> handleEditMainClick(player, session, slot, event);
            case EDIT_CATEGORY -> handleEditCategoryClick(player, session, slot, event);
            case EDIT_SECTION_SETTINGS -> handleSectionSettingsClick(player, session, slot, event);
            case FILTER_SORT -> handleFilterSortClick(player, session, slot, event);
        }
    }

    // ========================
    // Inventory Drag (cancel in shop)
    // ========================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getHolder() instanceof ShopInventoryHolder) {
            event.setCancelled(true);
        }
    }

    // ========================
    // Inventory Close (cleanup)
    // ========================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Only remove session if not reopening another shop GUI next tick
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Inventory open = player.getOpenInventory().getTopInventory();
            if (!(open.getHolder() instanceof ShopInventoryHolder)) {
                sessions.remove(uuid);
            }
        }, 1L);
    }

    // ========================
    // Main Categories Click Handler
    // ========================

    private void handleMainCategoriesClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        // Close button
        if (slot == 49) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            return;
        }

        // Prev page
        if (slot == 45 && session.getPage() > 0) {
            session.setPage(session.getPage() - 1);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> mainShopGUI.open(player, session));
            return;
        }

        // Next page
        if (slot == 53) {
            session.setPage(session.getPage() + 1);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> mainShopGUI.open(player, session));
            return;
        }

        // Category slots
        int[] catSlots = MainShopGUI.getCategorySlots();
        for (int i = 0; i < catSlots.length; i++) {
            if (catSlots[i] == slot) {
                int categoryIndex = session.getPage() * MainShopGUI.getCategoriesPerPage() + i;
                List<ShopCategory> categories = shopService.getCategories();
                if (categoryIndex < categories.size()) {
                    ShopCategory category = categories.get(categoryIndex);
                    session.setCurrentCategoryId(category.getId());
                    session.setPage(0);
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
                }
                return;
            }
        }
    }

    // ========================
    // Category Items Click Handler
    // ========================

    private void handleCategoryItemsClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        // Back button (slot 0)
        if (slot == 0) {
            session.setPage(0);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> mainShopGUI.open(player, session));
            return;
        }

        // Filter button (slot 8)
        if (slot == 8) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> shopEditGUI.openFilterSort(player, session));
            return;
        }

        // Prev page
        if (slot == 45 && session.getPage() > 0) {
            session.setPage(session.getPage() - 1);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
            return;
        }

        // Next page
        if (slot == 53) {
            session.setPage(session.getPage() + 1);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
            return;
        }

        // Item slots (9-44)
        if (slot >= CategoryItemsGUI.getItemStartSlot() && slot <= CategoryItemsGUI.getItemEndSlot()) {
            ShopCategory category = shopService.getCategory(session.getCurrentCategoryId());
            if (category == null) return;

            int itemIndex = session.getPage() * CategoryItemsGUI.getItemsPerPage() + (slot - CategoryItemsGUI.getItemStartSlot());
            List<ShopItem> items = category.getItems();
            if (itemIndex >= items.size()) return;

            ShopItem shopItem = items.get(itemIndex);
            session.setSelectedItem(shopItem);
            session.setSelectedQuantity(1);

            if (event.isLeftClick()) {
                // Buy flow
                if (!shopItem.isBuyable()) {
                    player.sendMessage(Component.text("This item is not available for purchase.", NamedTextColor.RED));
                    playSound(player, shopService.getConfig().getSoundError());
                    return;
                }
                session.setBuyMode(true);
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> quantitySelectorGUI.open(player, session));
            } else if (event.isRightClick()) {
                // Sell flow
                if (!shopItem.isSellable()) {
                    player.sendMessage(Component.text("This item cannot be sold.", NamedTextColor.RED));
                    playSound(player, shopService.getConfig().getSoundError());
                    return;
                }
                session.setBuyMode(false);
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> quantitySelectorGUI.open(player, session));
            }
        }
    }

    // ========================
    // Quantity Selector Click Handler
    // ========================

    private void handleQuantitySelectorClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        switch (slot) {
            case 0 -> adjustQuantity(session, -64);
            case 1 -> adjustQuantity(session, -16);
            case 2 -> adjustQuantity(session, -1);
            case 6 -> adjustQuantity(session, 1);
            case 7 -> adjustQuantity(session, 16);
            case 8 -> adjustQuantity(session, 64);
            case 18 -> {
                // Cancel — go back to category
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
                return;
            }
            case 22 -> {
                // Confirm — open confirm GUI
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> confirmGUI.open(player, session));
                return;
            }
            default -> { return; }
        }

        // Refresh the quantity selector after adjustment
        Bukkit.getScheduler().runTask(plugin, (Runnable) () -> quantitySelectorGUI.open(player, session));
    }

    private void adjustQuantity(ShopSession session, int delta) {
        int newQty = session.getSelectedQuantity() + delta;
        session.setSelectedQuantity(Math.max(1, newQty));
    }

    // ========================
    // Confirm Click Handler
    // ========================

    private void handleConfirmClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        if (slot == ConfirmGUI.SLOT_CONFIRM) {
            ShopItem shopItem = session.getSelectedItem();
            if (shopItem == null) return;

            Component result;
            String soundKey;

            if (session.isBuyMode()) {
                result = shopService.buyItem(player, shopItem, session.getSelectedQuantity());
                soundKey = shopService.getConfig().getSoundPurchase();
            } else {
                Material mat = Material.matchMaterial(shopItem.getMaterialName());
                if (mat == null) return;
                result = shopService.sellItem(player, mat, session.getSelectedQuantity());
                soundKey = shopService.getConfig().getSoundSell();
            }

            player.sendMessage(result);
            playSound(player, soundKey);

            // Reopen category items
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
        } else if (slot == ConfirmGUI.SLOT_CANCEL) {
            // Go back to quantity selector
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> quantitySelectorGUI.open(player, session));
        }
    }

    // ========================
    // Virtual Storage Click Handler
    // ========================

    private void handleVirtualStorageClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        // Close button
        if (slot == 52) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            return;
        }

        // Prev page
        if (slot == 45 && session.getPage() > 0) {
            session.setPage(session.getPage() - 1);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> virtualStorageGUI.open(player, session));
            return;
        }

        // Next page
        if (slot == 53) {
            session.setPage(session.getPage() + 1);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> virtualStorageGUI.open(player, session));
            return;
        }

        // Item claim (slots 0-44)
        if (slot >= 0 && slot < 45) {
            VirtualStorage storage = shopService.getVirtualStorage();
            List<VirtualStorage.VirtualStorageEntry> entries = storage.getItems(player.getUniqueId());

            int entryIndex = session.getPage() * VirtualStorageGUI.getItemsPerPage() + slot;
            if (entryIndex >= entries.size()) return;

            VirtualStorage.VirtualStorageEntry entry = entries.get(entryIndex);

            if (event.isShiftClick()) {
                // Claim all of this type
                Material type = entry.item().getType();
                for (VirtualStorage.VirtualStorageEntry e : entries) {
                    if (e.item().getType() == type) {
                        storage.claimItem(player.getUniqueId(), e.id(), player);
                    }
                }
            } else {
                // Claim single
                storage.claimItem(player.getUniqueId(), entry.id(), player);
            }

            // Refresh
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> virtualStorageGUI.open(player, session));
        }
    }

    // ========================
    // Edit Mode Click Handlers
    // ========================

    private void handleEditMainClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        if (shopEditGUI.isMainSlotReserved(slot)) {
            // Nav buttons still work
            if (slot == 49) {
                Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            } else if (slot == 45 && session.getPage() > 0) {
                session.setPage(session.getPage() - 1);
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> mainShopGUI.open(player, session));
            } else if (slot == 53) {
                session.setPage(session.getPage() + 1);
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> mainShopGUI.open(player, session));
            }
            return;
        }

        // Category slots
        int[] catSlots = MainShopGUI.getCategorySlots();
        for (int i = 0; i < catSlots.length; i++) {
            if (catSlots[i] == slot) {
                int categoryIndex = session.getPage() * MainShopGUI.getCategoriesPerPage() + i;
                List<ShopCategory> categories = shopService.getCategories();
                if (categoryIndex >= categories.size()) return;

                ShopCategory category = categories.get(categoryIndex);

                if (event.isRightClick()) {
                    // Open section settings sub-GUI
                    session.setCurrentCategoryId(category.getId());
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () ->
                            shopEditGUI.openSectionSettings(player, session, category));
                } else if (event.isShiftClick() && event.isLeftClick()) {
                    // Enter category in edit mode
                    session.setCurrentCategoryId(category.getId());
                    session.setPage(0);
                    session.setCurrentType(ShopSession.ShopGUIType.EDIT_CATEGORY);
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
                } else if (event.isLeftClick()) {
                    // Pick up / place / swap (edit drag behavior)
                    if (session.getCursorItem() == null) {
                        // Pick up
                        ItemStack inSlot = event.getView().getTopInventory().getItem(slot);
                        if (inSlot != null && inSlot.getType() != Material.AIR) {
                            session.setCursorItem(inSlot.clone());
                            event.getView().getTopInventory().setItem(slot, null);
                        }
                    } else {
                        // Place or swap
                        ItemStack inSlot = event.getView().getTopInventory().getItem(slot);
                        event.getView().getTopInventory().setItem(slot, session.getCursorItem());
                        session.setCursorItem(inSlot != null && inSlot.getType() != Material.AIR ? inSlot.clone() : null);
                    }
                }
                return;
            }
        }
    }

    private void handleEditCategoryClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        // Back button (slot 0)
        if (slot == 0) {
            session.setPage(0);
            session.setCurrentType(ShopSession.ShopGUIType.EDIT_MAIN);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> mainShopGUI.open(player, session));
            return;
        }

        // Filter (slot 8)
        if (slot == 8) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> shopEditGUI.openFilterSort(player, session));
            return;
        }

        // Nav
        if (slot == 45 && session.getPage() > 0) {
            session.setPage(session.getPage() - 1);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
            return;
        }
        if (slot == 53) {
            session.setPage(session.getPage() + 1);
            Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
            return;
        }

        if (shopEditGUI.isCategorySlotReserved(slot)) return;

        // Edit drag in item area
        if (slot >= CategoryItemsGUI.getItemStartSlot() && slot <= CategoryItemsGUI.getItemEndSlot()) {
            if (event.isLeftClick()) {
                if (session.getCursorItem() == null) {
                    ItemStack inSlot = event.getView().getTopInventory().getItem(slot);
                    if (inSlot != null && inSlot.getType() != Material.AIR) {
                        session.setCursorItem(inSlot.clone());
                        event.getView().getTopInventory().setItem(slot, null);
                    }
                } else {
                    ItemStack inSlot = event.getView().getTopInventory().getItem(slot);
                    event.getView().getTopInventory().setItem(slot, session.getCursorItem());
                    session.setCursorItem(inSlot != null && inSlot.getType() != Material.AIR ? inSlot.clone() : null);
                }
            }
        }
    }

    // ========================
    // Section Settings Click Handler
    // ========================

    private void handleSectionSettingsClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        switch (slot) {
            case 10 -> {
                // Rename — send message instruction
                player.closeInventory();
                player.sendMessage(Component.text("Type the new name in chat (use & for color codes):", NamedTextColor.YELLOW));
                // Rename handling would require a chat listener; for now, inform the user
                player.sendMessage(Component.text("Use /cc shop renamesection <id> <name> instead.", NamedTextColor.GRAY));
            }
            case 14 -> {
                // Delete
                ShopCategory category = shopService.getCategory(session.getCurrentCategoryId());
                if (category != null) {
                    shopService.getConfig().deleteCategory(category.getId());
                    player.sendMessage(Component.text("Section '" + category.getId() + "' deleted.", NamedTextColor.RED));
                }
                session.setCurrentType(ShopSession.ShopGUIType.EDIT_MAIN);
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> mainShopGUI.open(player, session));
            }
            case 16 -> {
                // Back
                session.setCurrentType(ShopSession.ShopGUIType.EDIT_MAIN);
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> mainShopGUI.open(player, session));
            }
        }
    }

    // ========================
    // Filter/Sort Click Handler
    // ========================

    private void handleFilterSortClick(Player player, ShopSession session, int slot, InventoryClickEvent event) {
        switch (slot) {
            case 10 -> session.setSortMode("alphabetical");
            case 12 -> session.setSortMode("price_asc");
            case 14 -> session.setSortMode("price_desc");
            case 16 -> session.setSortMode("default");
            case 22 -> {
                // Back to category
                if (session.isEditMode()) {
                    session.setCurrentType(ShopSession.ShopGUIType.EDIT_CATEGORY);
                } else {
                    session.setCurrentType(ShopSession.ShopGUIType.CATEGORY_ITEMS);
                }
                Bukkit.getScheduler().runTask(plugin, (Runnable) () -> categoryItemsGUI.open(player, session));
                return;
            }
            default -> { return; }
        }

        // Refresh sort GUI to show active indicator
        Bukkit.getScheduler().runTask(plugin, (Runnable) () -> shopEditGUI.openFilterSort(player, session));
    }

    // ========================
    // Sound helper
    // ========================

    private void playSound(Player player, String soundId) {
        if (soundId == null || soundId.isEmpty()) return;
        try {
            player.playSound(Sound.sound(Key.key(soundId), Sound.Source.MASTER, 1.0f, 1.0f));
        } catch (Exception e) {
            plugin.debug("[Shop] Failed to play sound: " + soundId);
        }
    }

    // ========================
    // Custom InventoryHolder
    // ========================

    public static class ShopInventoryHolder implements InventoryHolder {
        private final UUID playerId;

        public ShopInventoryHolder(UUID playerId) {
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
