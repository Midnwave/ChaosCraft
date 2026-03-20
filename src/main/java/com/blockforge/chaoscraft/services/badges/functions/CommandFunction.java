package com.blockforge.chaoscraft.services.badges.functions;

import com.blockforge.chaoscraft.services.badges.BadgeDefinition;
import org.bukkit.entity.Player;

/**
 * Function that associates a badge with a command string.
 * The badge is granted via {@link com.blockforge.chaoscraft.services.badges.BadgeListener}
 * when a player runs the associated command. shouldGrant always returns false
 * because granting is handled externally by the command preprocessor.
 */
public class CommandFunction implements BadgeFunction {

    private final String command;

    public CommandFunction(String command) {
        this.command = command;
    }

    @Override
    public boolean shouldGrant(Player player, BadgeDefinition badge) {
        // Granting is handled by BadgeListener's command preprocessor
        return false;
    }

    @Override
    public String getType() {
        return "command";
    }

    public String getCommand() {
        return command;
    }
}
