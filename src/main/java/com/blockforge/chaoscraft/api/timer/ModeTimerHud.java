package com.blockforge.chaoscraft.api.timer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

/**
 * Manages the BetterHud mode timer display state.
 *
 * Started via: /cc function starttimer <duration MM:SS> <flash_at MM:SS>
 * Stopped via: /cc function stopmodetimer  (or automatically when timer expires)
 *
 * The timer counts down from the given duration. When the remaining time hits
 * the flash threshold, {@link #isFlashing()} alternates: true for 10 ticks,
 * false for 10 ticks. When the timer reaches 0:00, flash stays permanently true.
 *
 * PlaceholderAPI placeholders:
 *   %chaoscraft_mode_timer_active%   — "true"/"false" — controls BetterHud visibility
 *   %chaoscraft_mode_timer_flash%    — "true"/"false" — 10-tick alternation when low
 *   %chaoscraft_timer_m_ss%          — countdown text like "2:30" (from ModeTimer)
 */
public class ModeTimerHud {

    private final ChaosCraftPlugin plugin;

    private boolean active = false;
    private boolean flashing = false;     // Current flash state (alternates 10 ticks on/off)
    private int flashCounter = 0;         // Ticks within current flash half-cycle
    private long flashThresholdTicks = 0; // Remaining ticks at which flashing begins
    private boolean expired = false;      // Timer hit 0:00 — flash stays true permanently

    // Per-mode display info
    private String displayName = "";
    private String color = "white";

    public ModeTimerHud(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the timer HUD with explicit duration and flash threshold.
     * Called from /cc function starttimer <duration> <flash_at>.
     *
     * @param durationSeconds  total timer duration in seconds (e.g. 150 for 2:30)
     * @param flashAtSeconds   seconds remaining when flashing begins (e.g. 30 for 0:30)
     */
    public void startHud(long durationSeconds, long flashAtSeconds) {
        active = true;
        flashing = false;
        flashCounter = 0;
        expired = false;
        flashThresholdTicks = flashAtSeconds * 20L;

        // Start the actual countdown timer
        var timer = plugin.getModeTimer();
        timer.start(durationSeconds);
        timer.setOnExpire(() -> {
            expired = true;
            flashing = true; // Stays red permanently at 0:00
            plugin.debug("[TimerHud] Timer expired — flash locked to true.");
        });

        // Load display info from active mode config if available
        var manager = plugin.getModeManager();
        if (manager.isAnyModeActive()) {
            var modeConfig = manager.getActiveMode().getModeConfig();
            var config = modeConfig.get();
            if (config != null) {
                displayName = config.getString("timer-hud.display-name",
                        manager.getActiveModeName().toUpperCase());
                color = config.getString("timer-hud.color", "white");
            } else {
                displayName = manager.getActiveModeName().toUpperCase();
            }
        }

        plugin.debug("[TimerHud] Started. Duration: " + durationSeconds + "s, Flash at: "
                + flashAtSeconds + "s remaining, Display: " + displayName);
    }

    /**
     * Start HUD using active mode's config values (backwards compat for modes
     * that call startHud() without explicit args).
     */
    public void startHud() {
        var manager = plugin.getModeManager();
        long duration = 600;
        long flashAt = 60;

        if (manager.isAnyModeActive()) {
            var modeConfig = manager.getActiveMode().getModeConfig();
            var config = modeConfig.get();
            if (config != null) {
                duration = config.getLong("timer.default-seconds", 600);
                flashAt = config.getInt("timer-hud.flash-threshold-seconds", 60);
            }
        }

        startHud(duration, flashAt);
    }

    /**
     * Stop the timer HUD and reset all state.
     */
    public void stopHud() {
        active = false;
        flashing = false;
        flashCounter = 0;
        expired = false;
        flashThresholdTicks = 0;
        displayName = "";
        color = "white";

        plugin.getModeTimer().stop();
        plugin.debug("[TimerHud] Stopped.");
    }

    /**
     * Called every tick from the mode tick loop or the main plugin tick.
     * Handles the 10-tick-on / 10-tick-off flash alternation.
     */
    public void tick() {
        if (!active) return;
        if (expired) return; // Flash locked to true at 0:00

        var timer = plugin.getModeTimer();
        long remainingTicks = timer.getRemainingTicks();

        if (remainingTicks <= flashThresholdTicks && remainingTicks > 0) {
            // In flash zone — alternate every 10 ticks
            flashCounter++;
            if (flashCounter >= 10) {
                flashing = !flashing;
                flashCounter = 0;
            }
        } else if (remainingTicks <= 0) {
            // Timer expired — handled by onExpire callback
            expired = true;
            flashing = true;
        } else {
            // Not in flash zone yet
            flashing = false;
            flashCounter = 0;
        }
    }

    // ========================
    // Placeholder getters
    // ========================

    /** Whether the timer HUD is currently active/visible. */
    public boolean isActive() { return active; }

    /** Whether the timer text should currently show as red. */
    public boolean isFlashing() { return flashing; }

    /** The display name for the current mode (e.g. "CHAIN MODE"). */
    public String getDisplayName() { return displayName; }

    /** The color name for the current mode (e.g. "gray", "dark_purple"). */
    public String getColor() { return color; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setColor(String color) { this.color = color; }
}
