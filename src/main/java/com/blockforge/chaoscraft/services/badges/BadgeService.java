package com.blockforge.chaoscraft.services.badges;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Public API for the ChaosCraft Badge System.
 * Manages badge definitions, storage, granting, and revoking.
 */
public class BadgeService {

    private final ChaosCraftPlugin plugin;
    private BadgeStorage storage;
    private BadgeConfig config;

    public BadgeService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        storage = new BadgeStorage(plugin);
        storage.initialize();

        config = new BadgeConfig(plugin);
        config.load();

        plugin.getLogger().info("[Badges] Badge service initialized.");
    }

    public void reload() {
        if (config != null) config.load();
    }

    public void shutdown() {
        if (storage != null) storage.shutdown();
    }

    // ========================
    // Grant / Revoke
    // ========================

    /**
     * Grant a badge to a player with sound and message.
     *
     * @param player  the online player
     * @param badgeId the badge ID to grant
     * @return true if the badge was newly granted
     */
    public boolean grantBadge(Player player, String badgeId) {
        BadgeDefinition badge = config.getBadge(badgeId);
        if (badge == null) return false;

        // Don't grant expired limited badges
        if (badge.isExpired()) return false;

        boolean granted = storage.grantBadge(player.getUniqueId(), badgeId);
        if (granted) {
            // Play configurable sound
            String sound = config.getGrantSound();
            if (sound != null && !sound.isEmpty()) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }

            // Send grant message
            Component message = Component.text("Badge Earned! ", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .append(Component.text("", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                    .append(parseDisplayName(badge.getDisplayName()));
            player.sendMessage(message);
        }
        return granted;
    }

    /**
     * Revoke a badge from a player by UUID.
     *
     * @param playerUuid the player UUID
     * @param badgeId    the badge ID to revoke
     * @return true if the badge was removed
     */
    public boolean revokeBadge(UUID playerUuid, String badgeId) {
        return storage.revokeBadge(playerUuid, badgeId);
    }

    // ========================
    // Queries
    // ========================

    /**
     * Check if a player has a specific badge.
     */
    public boolean hasBadge(UUID playerUuid, String badgeId) {
        return storage.hasBadge(playerUuid, badgeId);
    }

    /**
     * Get all badges owned by a player, mapped as badge_id -> earned_at epoch millis.
     */
    public Map<String, Long> getPlayerBadges(UUID playerUuid) {
        return storage.getPlayerBadges(playerUuid);
    }

    /**
     * Get a badge definition by ID.
     */
    public BadgeDefinition getBadgeDefinition(String id) {
        return config.getBadge(id);
    }

    /**
     * Get all defined badge IDs.
     */
    public Set<String> getAllBadgeIds() {
        return config.getBadges().keySet();
    }

    /**
     * Get all badge definitions.
     */
    public Map<String, BadgeDefinition> getAllBadges() {
        return config.getBadges();
    }

    // ========================
    // Badge Management
    // ========================

    /**
     * Create or update a badge definition in the config.
     */
    public void createBadge(BadgeDefinition badge) {
        config.saveBadgeDefinition(badge);
    }

    /**
     * Delete a badge definition from the config.
     */
    public void deleteBadge(String id) {
        config.removeBadgeDefinition(id);
    }

    // ========================
    // Config Access
    // ========================

    public BadgeConfig getConfig() {
        return config;
    }

    // ========================
    // Display Name Parsing
    // ========================

    /**
     * Parse a display name string with hex color codes (&#RRGGBB) into an Adventure Component.
     */
    public static Component parseDisplayName(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        int i = 0;
        TextColor currentColor = null;
        StringBuilder buffer = new StringBuilder();

        while (i < input.length()) {
            // Check for hex color code: &#RRGGBB
            if (i + 8 <= input.length() && input.charAt(i) == '&' && input.charAt(i + 1) == '#') {
                // Flush current buffer
                if (buffer.length() > 0) {
                    Component part = Component.text(buffer.toString())
                            .decoration(TextDecoration.ITALIC, false);
                    if (currentColor != null) {
                        part = part.color(currentColor);
                    }
                    result = result.append(part);
                    buffer = new StringBuilder();
                }

                String hex = input.substring(i + 2, i + 8);
                try {
                    currentColor = TextColor.fromHexString("#" + hex);
                } catch (Exception e) {
                    currentColor = NamedTextColor.WHITE;
                }
                i += 8;
            }
            // Check for section symbol color codes (legacy &X)
            else if (i + 1 < input.length() && input.charAt(i) == '&') {
                // Flush current buffer
                if (buffer.length() > 0) {
                    Component part = Component.text(buffer.toString())
                            .decoration(TextDecoration.ITALIC, false);
                    if (currentColor != null) {
                        part = part.color(currentColor);
                    }
                    result = result.append(part);
                    buffer = new StringBuilder();
                }

                char code = input.charAt(i + 1);
                currentColor = legacyColor(code);
                i += 2;
            } else {
                buffer.append(input.charAt(i));
                i++;
            }
        }

        // Flush remaining buffer
        if (buffer.length() > 0) {
            Component part = Component.text(buffer.toString())
                    .decoration(TextDecoration.ITALIC, false);
            if (currentColor != null) {
                part = part.color(currentColor);
            }
            result = result.append(part);
        }

        return result;
    }

    private static TextColor legacyColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> NamedTextColor.BLACK;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '2' -> NamedTextColor.DARK_GREEN;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '4' -> NamedTextColor.DARK_RED;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case '6' -> NamedTextColor.GOLD;
            case '7' -> NamedTextColor.GRAY;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '9' -> NamedTextColor.BLUE;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case 'c' -> NamedTextColor.RED;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case 'e' -> NamedTextColor.YELLOW;
            case 'f' -> NamedTextColor.WHITE;
            default -> NamedTextColor.WHITE;
        };
    }
}
