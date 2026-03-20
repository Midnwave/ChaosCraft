package com.blockforge.chaoscraft.services.badges.functions;

import com.blockforge.chaoscraft.services.badges.BadgeDefinition;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Function that grants a badge when a PlaceholderAPI placeholder value
 * is strictly greater than a configured threshold.
 * Type: "above_placeholderapi"
 */
public class PlaceholderAboveFunction implements BadgeFunction {

    private final String placeholder;
    private final double value;

    public PlaceholderAboveFunction(String placeholder, double value) {
        this.placeholder = placeholder;
        this.value = value;
    }

    @Override
    public boolean shouldGrant(Player player, BadgeDefinition badge) {
        try {
            String parsed = PlaceholderAPI.setPlaceholders(player, placeholder);
            double playerValue = Double.parseDouble(parsed);
            return playerValue > value;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    @Override
    public String getType() {
        return "above_placeholderapi";
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public double getValue() {
        return value;
    }
}
