package com.blockforge.chaoscraft.services.badges.functions;

import com.blockforge.chaoscraft.services.badges.BadgeDefinition;
import org.bukkit.entity.Player;

/**
 * A placeholder function that never auto-grants.
 * Used for badges that are granted manually or by external systems.
 */
public class DummyFunction implements BadgeFunction {

    @Override
    public boolean shouldGrant(Player player, BadgeDefinition badge) {
        return false;
    }

    @Override
    public String getType() {
        return "dummy";
    }
}
