package com.blockforge.chaoscraft.api.timer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

/**
 * Manages the BetterHud mode timer display state.
 *
 * Activated automatically by ModeManager when a mode starts. Reads the
 * countdown from the existing ModeTimer (which modes start themselves).
 *
 * Can also be started manually via: /cc function startmodetimer <MM:SS> <flash_at>
 * which overrides the mode's timer with explicit values.
 *
 * Flash logic: when remaining time <= flash threshold, alternates true/false
 * every 10 ticks. At 0:00, flash stays permanently true (red).
 *
 * PlaceholderAPI:
 *   %chaoscraft_mode_timer_active%   — "true"/"false" — BetterHud visibility
 *   %chaoscraft_mode_timer_flash%    — "true"/"false" — 10-tick alternation
 *   %chaoscraft_timer_m_ss%          — countdown text (from ModeTimer)
 */
public class ModeTimerHud {

    private final ChaosCraftPlugin plugin;

    private boolean active = false;
    private boolean flashing = false;
    private int flashCounter = 0;
    private long flashThresholdTicks = 60 * 20L; // default 60 seconds
    private boolean expired = false;

    // Slide animation — counts 0→20 ticks after HUD activates, then stays at 20
    private int slideTick = 0;
    private static final int SLIDE_DURATION = 20; // 20 ticks = 1 second at 20fps

    // Per-mode display info
    private String displayName = "";
    private String color = "white";

    public ModeTimerHud(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the HUD with explicit duration and flash threshold.
     * Called from /cc function startmodetimer <duration> <flash_at>.
     * This ALSO starts the ModeTimer countdown.
     */
    public void startHud(long durationSeconds, long flashAtSeconds) {
        active = true;
        flashing = false;
        flashCounter = 0;
        expired = false;
        slideTick = 0;
        flashThresholdTicks = flashAtSeconds * 20L;

        // Start the actual countdown timer
        var timer = plugin.getModeTimer();
        timer.start(durationSeconds);
        timer.setOnExpire(() -> {
            expired = true;
            flashing = true;
            plugin.debug("[TimerHud] Timer expired — flash locked to true.");
        });

        loadModeDisplayInfo();
        plugin.debug("[TimerHud] Started with explicit timer: " + durationSeconds + "s, flash at: "
                + flashAtSeconds + "s");
    }

    /**
     * Activate the HUD display without starting a new timer.
     * The mode has already started its own ModeTimer — we just
     * piggyback on it for the BetterHud display.
     *
     * Called by ModeManager when a mode starts.
     */
    public void startHud() {
        active = true;
        flashing = false;
        flashCounter = 0;
        expired = false;
        slideTick = 0;

        // Read flash threshold from mode config
        var manager = plugin.getModeManager();
        if (manager.isAnyModeActive()) {
            var config = manager.getActiveMode().getModeConfig().get();
            if (config != null) {
                flashThresholdTicks = config.getInt("timer-hud.flash-threshold-seconds", 60) * 20L;
            }
        }

        // Listen for timer expiry
        var timer = plugin.getModeTimer();
        // Only set expire callback if the mode didn't already set one that ends the mode
        // We add our flash behavior via tick() instead

        loadModeDisplayInfo();
        plugin.debug("[TimerHud] Activated (piggyback on mode's timer). Flash threshold: "
                + (flashThresholdTicks / 20) + "s");
    }

    /**
     * Stop the timer HUD and reset all state.
     */
    public void stopHud() {
        active = false;
        flashing = false;
        flashCounter = 0;
        expired = false;
        slideTick = 0;
        flashThresholdTicks = 60 * 20L;
        displayName = "";
        color = "white";

        plugin.debug("[TimerHud] Stopped.");
    }

    /**
     * Called every tick from ModeManager's tick loop.
     * Handles the 10-tick-on / 10-tick-off flash alternation.
     */
    public void tick() {
        if (!active) return;

        // Slide animation counter — counts 0→20 then stops
        if (slideTick < SLIDE_DURATION) {
            slideTick++;
        }

        if (expired) return; // Flash locked to true at 0:00

        var timer = plugin.getModeTimer();
        if (!timer.isRunning()) {
            // Timer finished — lock flash to true
            expired = true;
            flashing = true;
            return;
        }

        long remainingTicks = timer.getRemainingTicks();

        if (remainingTicks <= flashThresholdTicks && remainingTicks > 0) {
            // In flash zone — alternate every 10 ticks
            flashCounter++;
            if (flashCounter >= 10) {
                flashing = !flashing;
                flashCounter = 0;
            }
        } else if (remainingTicks <= 0) {
            expired = true;
            flashing = true;
        } else {
            flashing = false;
            flashCounter = 0;
        }
    }

    private void loadModeDisplayInfo() {
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
    }

    // ========================
    // Placeholder getters
    // ========================

    public boolean isActive() { return active; }
    public boolean isFlashing() { return flashing; }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }

    /** Slide tick counter: 0→20 after HUD starts, then stays at 20. */
    public int getSlideTick() { return slideTick; }

    /** True when the slide-down animation is complete (tick >= 20). */
    public boolean isSlideComplete() { return slideTick >= SLIDE_DURATION; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setColor(String color) { this.color = color; }
}
