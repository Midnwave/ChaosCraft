package com.blockforge.chaoscraft.services.claims;

/**
 * Trust levels for claims. Higher levels inherit all permissions from lower levels.
 * Hierarchy: ACCESS < CONTAINER < BUILD < PERMISSION
 */
public enum TrustLevel {
    ACCESS(0, "Access", "Open doors, buttons, levers, pressure plates"),
    CONTAINER(1, "Container", "Access + chests, furnaces, hoppers, barrels"),
    BUILD(2, "Build", "Container + place/break blocks, use buckets"),
    PERMISSION(3, "Permission", "Build + manage trusts, resize claim, set flags");

    private final int level;
    private final String displayName;
    private final String description;

    TrustLevel(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /**
     * Check if this trust level includes the given level.
     * E.g., BUILD includes CONTAINER and ACCESS.
     */
    public boolean includes(TrustLevel other) {
        return this.level >= other.level;
    }

    public static TrustLevel fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
