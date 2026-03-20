package com.blockforge.chaoscraft.services.shop;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class representing an item available in the shop.
 * Stores pricing, requirement thresholds, and display metadata.
 */
public class ShopItem {

    private String materialName;
    private double buyPrice;
    private double sellPrice;
    private int killsRequired;
    private int sKillsRequired;
    private int survivalsRequired;
    private String badgeRequired;
    private List<String> shopLore;
    private int slot;
    private String displayName;

    public ShopItem(String materialName, double buyPrice, double sellPrice,
                    int killsRequired, int sKillsRequired, int survivalsRequired,
                    String badgeRequired, List<String> shopLore, int slot, String displayName) {
        this.materialName = materialName;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.killsRequired = killsRequired;
        this.sKillsRequired = sKillsRequired;
        this.survivalsRequired = survivalsRequired;
        this.badgeRequired = badgeRequired;
        this.shopLore = shopLore != null ? new ArrayList<>(shopLore) : new ArrayList<>();
        this.slot = slot;
        this.displayName = displayName;
    }

    // ========================
    // Getters
    // ========================

    public String getMaterialName() { return materialName; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public int getKillsRequired() { return killsRequired; }
    public int getSKillsRequired() { return sKillsRequired; }
    public int getSurvivalsRequired() { return survivalsRequired; }
    public String getBadgeRequired() { return badgeRequired; }
    public List<String> getShopLore() { return shopLore; }
    public int getSlot() { return slot; }
    public String getDisplayName() { return displayName; }

    public boolean isBuyable() { return buyPrice >= 0; }
    public boolean isSellable() { return sellPrice >= 0; }

    // ========================
    // Setters
    // ========================

    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public void setKillsRequired(int killsRequired) { this.killsRequired = killsRequired; }
    public void setSKillsRequired(int sKillsRequired) { this.sKillsRequired = sKillsRequired; }
    public void setSurvivalsRequired(int survivalsRequired) { this.survivalsRequired = survivalsRequired; }
    public void setBadgeRequired(String badgeRequired) { this.badgeRequired = badgeRequired; }
    public void setShopLore(List<String> shopLore) { this.shopLore = shopLore != null ? new ArrayList<>(shopLore) : new ArrayList<>(); }
    public void setSlot(int slot) { this.slot = slot; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
