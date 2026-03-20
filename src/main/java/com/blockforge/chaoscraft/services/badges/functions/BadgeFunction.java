package com.blockforge.chaoscraft.services.badges.functions;

import com.blockforge.chaoscraft.services.badges.BadgeDefinition;
import org.bukkit.entity.Player;

/**
 * Interface for badge earning conditions.
 * Each function type determines how/when a player earns a badge.
 */
public interface BadgeFunction {

    /**
     * Check if the given player should be granted this badge.
     *
     * @param player the online player to check
     * @param badge  the badge definition being evaluated
     * @return true if the player meets the criteria for this badge
     */
    boolean shouldGrant(Player player, BadgeDefinition badge);

    /**
     * The function type identifier (e.g. "dummy", "command", "on_mode_survive").
     */
    String getType();
}
