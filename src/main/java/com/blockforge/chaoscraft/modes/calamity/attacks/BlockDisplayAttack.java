package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.Location;

/**
 * Base class for all block display structure attacks.
 * These are the 100 unique block display structures per phase
 * described in the design docs.
 *
 * Block display attacks:
 * - Spawn a structure made of BlockDisplay/ItemDisplay entities
 * - Animate with rotations, translations, scale changes
 * - Have a continuous damage radius (configurable tick interval)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Each looks unique and identifiable
 * - Calamity color palette: purple, cyan, crimson
 */
public abstract class BlockDisplayAttack extends AbstractAttack {

    protected final DisplayBuilder displayBuilder;

    protected BlockDisplayAttack(ChaosCraftPlugin plugin, AttackConfig config) {
        super(plugin, config);
        this.displayBuilder = new DisplayBuilder(plugin);
    }

    @Override
    protected void onCleanup() {
        displayBuilder.removeAll();
    }

    /**
     * Helper: get the DisplayBuilder for creating visual entities.
     * Entities created through DisplayBuilder are auto-tracked for cleanup.
     */
    protected DisplayBuilder getDisplayBuilder() {
        return displayBuilder;
    }

    @Override
    public AttackType getType() {
        return AttackType.BLOCK_DISPLAY;
    }
}
