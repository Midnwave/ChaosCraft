package com.blockforge.chaoscraft.services.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data class representing a shop category (section).
 * Each category has an icon, display name, and a list of purchasable items.
 */
public class ShopCategory {

    private final String id;
    private String displayName;
    private String iconMaterial;
    private int customModelData;
    private int slot;
    private final List<ShopItem> items;

    public ShopCategory(String id, String displayName, String iconMaterial, int customModelData, int slot) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.customModelData = customModelData;
        this.slot = slot;
        this.items = new ArrayList<>();
    }

    // ========================
    // Getters
    // ========================

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getIconMaterial() { return iconMaterial; }
    public int getCustomModelData() { return customModelData; }
    public int getSlot() { return slot; }
    public List<ShopItem> getItems() { return Collections.unmodifiableList(items); }

    // ========================
    // Setters
    // ========================

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setIconMaterial(String iconMaterial) { this.iconMaterial = iconMaterial; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
    public void setSlot(int slot) { this.slot = slot; }

    // ========================
    // Item management
    // ========================

    public void addItem(ShopItem item) {
        items.add(item);
    }

    public void removeItem(ShopItem item) {
        items.remove(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void clearItems() {
        items.clear();
    }

    public int getItemCount() {
        return items.size();
    }
}
