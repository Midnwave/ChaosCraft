package com.blockforge.chaoscraft.api.timer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

/**
 * Manages the BetterHud mode timer display state.
 * Provides tick synchronization, flash state, and active tracking
 * for PlaceholderAPI placeholders consumed by BetterHud.
 *
 * Placeholders provided:
 *   %chaoscraft_mode_timer_ticks%    — 1-20 cycle for animation sync
 *   %chaoscraft_mode_timer_active%   — "true"/"false"
 *   %chaoscraft_mode_timer_flash%    — "true"/"false" alternates every 10 ticks when low
 *   %chaoscraft_mode_timer_low%      — "true"/"false" when below flash threshold
 *   %chaoscraft_mode_color%          — Color name for current mode
 *   %chaoscraft_mode_display_name%   — Display name for current mode
 */
public class ModeTimerHud {

    private final ChaosCraftPlugin plugin;

    private boolean active = false;
    private int syncTick = 1;           // 1-20 cycle for BetterHud animation sync
    private boolean flashing = false;   // Alternates every 10 ticks when timer is low
    private boolean low = false;        // Timer below flash threshold
    private int flashCounter = 0;       // Counts ticks for flash alternation

    // Per-mode display info (set when mode starts)
    private String displayName = "";
    private String color = "white";
    private String flashColor = "red";
    private int flashThresholdSeconds = 60;

    public ModeTimerHud(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the timer HUD. Called when a mode starts or via /cc function startmodetimer.
     */
    public void startHud() {
        active = true;
        syncTick = 1;
        flashing = false;
        low = false;
        flashCounter = 0;

        // Load display info from active mode config
        var manager = plugin.getModeManager();
        if (manager.isAnyModeActive()) {
            var modeConfig = manager.getActiveMode().getModeConfig();
            var config = modeConfig.get();
            if (config != null) {
                displayName = config.getString("timer-hud.display-name",
                        manager.getActiveModeName().toUpperCase());
                color = config.getString("timer-hud.color", "white");
                flashColor = config.getString("timer-hud.flash-color", "red");
                flashThresholdSeconds = config.getInt("timer-hud.flash-threshold-seconds", 60);
            } else {
                displayName = manager.getActiveModeName().toUpperCase();
            }
        }

        plugin.debug("[TimerHud] Started. Display: " + displayName + ", Color: " + color
                + ", Flash at: " + flashThresholdSeconds + "s");
    }

    /**
     * Stop the timer HUD. Called when mode ends or via /cc function stopmodetimer.
     */
    public void stopHud() {
        active = false;
        syncTick = 1;
        flashing = false;
        low = false;
        flashCounter = 0;
        displayName = "";
        color = "white";

        plugin.debug("[TimerHud] Stopped.");
    }

    /**
     * Called every tick from the mode tick loop.
     * Updates sync counter, flash state, and low state.
     */
    public void tick() {
        if (!active) return;

        // Update sync tick (1-20 cycle)
        syncTick++;
        if (syncTick > 20) syncTick = 1;

        // Check if timer is below flash threshold
        var timer = plugin.getModeTimer();
        long remaining = timer.getRemainingSeconds();
        low = remaining > 0 && remaining <= flashThresholdSeconds;

        // Flash alternation (every 10 ticks)
        if (low) {
            flashCounter++;
            if (flashCounter >= 10) {
                flashing = !flashing;
                flashCounter = 0;
            }
        } else {
            flashing = false;
            flashCounter = 0;
        }
    }

    // ========================
    // Placeholder getters
    // ========================

    /** 1-20 sync tick for BetterHud animation synchronization. */
    public int getSyncTick() { return syncTick; }

    /** Whether the timer HUD is currently active/visible. */
    public boolean isActive() { return active; }

    /** Whether the timer text should be showing as red (flashing state). */
    public boolean isFlashing() { return flashing; }

    /** Whether the timer is below the flash threshold. */
    public boolean isLow() { return low; }

    /** The display name for the current mode (e.g. "CHAIN MODE"). */
    public String getDisplayName() { return displayName; }

    /** The color name for the current mode (e.g. "gray", "dark_purple"). */
    public String getColor() { return color; }

    /** The flash color name (e.g. "red"). */
    public String getFlashColor() { return flashColor; }

    /** The threshold in seconds when flashing starts. */
    public int getFlashThresholdSeconds() { return flashThresholdSeconds; }
}
