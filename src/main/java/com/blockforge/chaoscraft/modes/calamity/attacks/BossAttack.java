package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.Location;

/**
 * Base class for all boss-specific attacks.
 * These are attacks that only occur while a specific boss is active:
 * - Tied to a specific boss (voidmaw, dog, dweller, dragon, calamitas)
 * - May have unique mechanics (multi-hit, phased, etc.)
 * - Boss attacks can be more dangerous than environmental
 * - Still follow all core rules: no status effects, straight spawn,
 *   configurable everything
 */
public abstract class BossAttack extends AbstractAttack {

    protected final DisplayBuilder displayBuilder;
    protected final String bossName;

    protected BossAttack(ChaosCraftPlugin plugin, AttackConfig config, String bossName) {
        super(plugin, config);
        this.displayBuilder = new DisplayBuilder(plugin, config.getModeName());
        this.bossName = bossName;
    }

    @Override
    protected void onCleanup() {
        displayBuilder.removeAll();
    }

    protected DisplayBuilder getDisplayBuilder() {
        return displayBuilder;
    }

    public String getBossName() {
        return bossName;
    }

    @Override
    public AttackType getType() {
        return AttackType.BOSS;
    }
}
