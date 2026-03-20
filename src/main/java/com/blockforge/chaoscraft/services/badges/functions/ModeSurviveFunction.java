package com.blockforge.chaoscraft.services.badges.functions;

import com.blockforge.chaoscraft.services.badges.BadgeDefinition;
import org.bukkit.entity.Player;

/**
 * Function that associates a badge with surviving a specific game mode.
 * The badge is granted externally when the mode ends and the player survived.
 * shouldGrant always returns false because granting is handled by the mode system.
 */
public class ModeSurviveFunction implements BadgeFunction {

    private final String modeName;

    public ModeSurviveFunction(String modeName) {
        this.modeName = modeName;
    }

    @Override
    public boolean shouldGrant(Player player, BadgeDefinition badge) {
        // Granting is handled externally by mode completion hooks
        return false;
    }

    @Override
    public String getType() {
        return "on_mode_survive";
    }

    public String getModeName() {
        return modeName;
    }
}
