package com.blockforge.chaoscraft.api.timer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;

/**
 * Manages the BetterHud mode timer display state.
 *
 * Started via: /cc function startmodetimer <MM:SS> <flash_at> [delay_seconds]
 *
 * The timer counts down from the given duration. When remaining time hits
 * the flash threshold, isFlashing() alternates 10 ticks on / 10 ticks off.
 * At 0:00, flash stays permanently true (red).
 *
 * Optional delay: bar appears immediately but timer text is blank until
 * the delay expires, then the countdown begins.
 *
 * PlaceholderAPI:
 *   %chaoscraft_mode_timer_active%   — "true"/"false" — BetterHud visibility
 *   %chaoscraft_mode_timer_flash%    — "true"/"false" — red flash alternation
 *   %chaoscraft_mode_timer_text%     — timer text (blank during delay, "m:ss" during countdown, "0:00" at end)
 *   %chaoscraft_join_ticks%          — 0→20 per-player for slide animation
 */
public class ModeTimerHud {

    private final ChaosCraftPlugin plugin;

    private boolean active = false;
    private boolean flashing = false;
    private int flashCounter = 0;
    private long flashThresholdTicks = 60 * 20L;
    private boolean expired = false;

    // Delay: bar visible but timer text blank until delay expires
    private long delayTicksRemaining = 0;
    private boolean delaying = false;
    private long pendingDurationSeconds = 0; // Timer starts after delay

    // Per-mode display info
    private String displayName = "";
    private String color = "white";

    public ModeTimerHud(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the HUD with explicit duration, flash threshold, and optional delay.
     * Called from /cc function startmodetimer <duration> <flash_at> [delay].
     *
     * @param durationSeconds  countdown duration
     * @param flashAtSeconds   seconds remaining when flashing begins
     * @param delaySeconds     seconds before countdown starts (bar visible, text blank)
     */
    public void startHud(long durationSeconds, long flashAtSeconds, long delaySeconds) {
        active = true;
        flashing = false;
        flashCounter = 0;
        expired = false;
        flashThresholdTicks = flashAtSeconds * 20L;

        loadModeDisplayInfo();

        if (delaySeconds > 0) {
            // Delay mode: bar visible but text blank
            delaying = true;
            delayTicksRemaining = delaySeconds * 20L;
            pendingDurationSeconds = durationSeconds;
            plugin.debug("[TimerHud] Started with " + delaySeconds + "s delay, then " + durationSeconds + "s timer");
        } else {
            // No delay: start countdown immediately
            delaying = false;
            delayTicksRemaining = 0;
            startCountdown(durationSeconds);
            plugin.debug("[TimerHud] Started: " + durationSeconds + "s, flash at " + flashAtSeconds + "s");
        }
    }

    /** Convenience: no delay. */
    public void startHud(long durationSeconds, long flashAtSeconds) {
        startHud(durationSeconds, flashAtSeconds, 0);
    }

    /** Convenience: use mode config values. */
    public void startHud() {
        var manager = plugin.getModeManager();
        long duration = 600;
        long flashAt = 60;

        if (manager.isAnyModeActive()) {
            var config = manager.getActiveMode().getModeConfig().get();
            if (config != null) {
                duration = config.getLong("timer.default-seconds", 600);
                flashAt = config.getInt("timer-hud.flash-threshold-seconds", 60);
            }
        }

        startHud(duration, flashAt, 0);
    }

    private void startCountdown(long durationSeconds) {
        var timer = plugin.getModeTimer();
        timer.start(durationSeconds);
        timer.setOnExpire(() -> {
            expired = true;
            flashing = true;
            plugin.debug("[TimerHud] Timer expired — flash locked to true.");
        });
    }

    public void stopHud() {
        active = false;
        flashing = false;
        flashCounter = 0;
        expired = false;
        delaying = false;
        delayTicksRemaining = 0;
        displayName = "";
        color = "white";

        plugin.debug("[TimerHud] Stopped.");
    }

    /**
     * Called every tick from ModeManager's tick loop.
     */
    public void tick() {
        if (!active) return;

        // Delay countdown: bar visible, text blank
        if (delaying) {
            delayTicksRemaining--;
            if (delayTicksRemaining <= 0) {
                delaying = false;
                startCountdown(pendingDurationSeconds);
                plugin.debug("[TimerHud] Delay over — countdown started: " + pendingDurationSeconds + "s");
            }
            return;
        }

        if (expired) return;

        var timer = plugin.getModeTimer();
        if (!timer.isRunning()) {
            expired = true;
            flashing = true;
            return;
        }

        long remainingTicks = timer.getRemainingTicks();

        if (remainingTicks <= flashThresholdTicks && remainingTicks > 0) {
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
            var config = manager.getActiveMode().getModeConfig().get();
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
    public boolean isDelaying() { return delaying; }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }

    /**
     * Timer text for the HUD. Returns "" during delay, "m:ss" during countdown, "0:00" at end.
     */
    public String getTimerText() {
        if (delaying) return "";
        return plugin.getModeTimer().formatMSs();
    }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setColor(String color) { this.color = color; }
}
