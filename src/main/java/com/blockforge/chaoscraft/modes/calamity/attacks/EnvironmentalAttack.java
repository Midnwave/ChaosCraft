package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.Location;

/**
 * Base class for all environmental attacks.
 * These are area-of-effect events that affect the arena:
 * - Ground effects, particle storms, falling objects, etc.
 * - May or may not use BlockDisplay entities
 * - Can have impact-only damage (meteors, falling things)
 * - Always spawn straight
 * - Calamity color palette
 */
public abstract class EnvironmentalAttack extends AbstractAttack {

    protected final DisplayBuilder displayBuilder;

    protected EnvironmentalAttack(ChaosCraftPlugin plugin, AttackConfig config) {
        super(plugin, config);
        this.displayBuilder = new DisplayBuilder(plugin);
    }

    @Override
    protected void onCleanup() {
        displayBuilder.removeAll();
    }

    protected DisplayBuilder getDisplayBuilder() {
        return displayBuilder;
    }

    @Override
    public AttackType getType() {
        return AttackType.ENVIRONMENTAL;
    }
}
