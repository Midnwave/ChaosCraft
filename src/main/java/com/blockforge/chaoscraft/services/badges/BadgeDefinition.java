package com.blockforge.chaoscraft.services.badges;

import org.bukkit.Material;

/**
 * Data class representing a single badge definition.
 */
public class BadgeDefinition {

    private final String id;
    private final String displayName;
    private final boolean limited;
    private final long expiryDate;
    private final String description;
    private final String function;
    private final Material iconMaterial;
    private final int customModelData;

    public BadgeDefinition(String id, String displayName, boolean limited, long expiryDate,
                           String description, String function, Material iconMaterial, int customModelData) {
        this.id = id;
        this.displayName = displayName;
        this.limited = limited;
        this.expiryDate = expiryDate;
        this.description = description;
        this.function = function;
        this.iconMaterial = iconMaterial;
        this.customModelData = customModelData;
    }

    public String getId() {
        return id;
    }

    /**
     * Display name with hex color codes (&#RRGGBB prefix style).
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Whether this badge is limited-time (event badges, seasonal, etc.).
     */
    public boolean isLimited() {
        return limited;
    }

    /**
     * Epoch millis when this badge expires. 0 = no expiry.
     */
    public long getExpiryDate() {
        return expiryDate;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Function string parsed by {@link com.blockforge.chaoscraft.services.badges.functions.BadgeFunctionParser}.
     * Examples: "dummy", "command:some_cmd", "on_mode_survive:calamity",
     * "above_placeholderapi:%cc_kills%:100"
     */
    public String getFunction() {
        return function;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    /**
     * Check if this badge has expired (limited-time only).
     */
    public boolean isExpired() {
        return limited && expiryDate > 0 && System.currentTimeMillis() > expiryDate;
    }
}
