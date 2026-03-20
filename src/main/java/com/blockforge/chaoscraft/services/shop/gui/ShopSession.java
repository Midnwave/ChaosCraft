package com.blockforge.chaoscraft.services.shop.gui;

import com.blockforge.chaoscraft.services.shop.ShopItem;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Per-player session state for the shop GUI system.
 * Tracks which GUI screen is open, pagination, selection state, and edit mode.
 */
public class ShopSession {

    public enum ShopGUIType {
        MAIN_CATEGORIES,
        CATEGORY_ITEMS,
        QUANTITY_SELECTOR,
        CONFIRM_PURCHASE,
        CONFIRM_SELL,
        VIRTUAL_STORAGE,
        EDIT_MAIN,
        EDIT_CATEGORY,
        EDIT_SECTION_SETTINGS,
        FILTER_SORT,
        ADD_ITEM
    }

    private final UUID playerId;
    private ShopGUIType currentType;
    private int page;
    private String currentCategoryId;
    private ShopItem selectedItem;
    private int selectedQuantity;
    private boolean editMode;
    private ItemStack cursorItem;
    private String sortMode;
    private boolean buyMode; // true = buying, false = selling

    public ShopSession(UUID playerId) {
        this.playerId = playerId;
        this.currentType = ShopGUIType.MAIN_CATEGORIES;
        this.page = 0;
        this.currentCategoryId = null;
        this.selectedItem = null;
        this.selectedQuantity = 1;
        this.editMode = false;
        this.cursorItem = null;
        this.sortMode = "default";
        this.buyMode = true;
    }

    // ========================
    // Getters
    // ========================

    public UUID getPlayerId() { return playerId; }
    public ShopGUIType getCurrentType() { return currentType; }
    public int getPage() { return page; }
    public String getCurrentCategoryId() { return currentCategoryId; }
    public ShopItem getSelectedItem() { return selectedItem; }
    public int getSelectedQuantity() { return selectedQuantity; }
    public boolean isEditMode() { return editMode; }
    public ItemStack getCursorItem() { return cursorItem; }
    public String getSortMode() { return sortMode; }
    public boolean isBuyMode() { return buyMode; }

    // ========================
    // Setters
    // ========================

    public void setCurrentType(ShopGUIType currentType) { this.currentType = currentType; }
    public void setPage(int page) { this.page = page; }
    public void setCurrentCategoryId(String currentCategoryId) { this.currentCategoryId = currentCategoryId; }
    public void setSelectedItem(ShopItem selectedItem) { this.selectedItem = selectedItem; }
    public void setSelectedQuantity(int selectedQuantity) { this.selectedQuantity = Math.max(1, selectedQuantity); }
    public void setEditMode(boolean editMode) { this.editMode = editMode; }
    public void setCursorItem(ItemStack cursorItem) { this.cursorItem = cursorItem; }
    public void setSortMode(String sortMode) { this.sortMode = sortMode; }
    public void setBuyMode(boolean buyMode) { this.buyMode = buyMode; }
}
