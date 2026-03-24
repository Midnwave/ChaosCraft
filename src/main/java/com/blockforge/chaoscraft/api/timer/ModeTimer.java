package com.blockforge.chaoscraft.api.timer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Central mode timer API. Tracks remaining time in ticks, provides formatted
 * placeholder output, and fires callbacks when the timer expires.
 */
public class ModeTimer {

    private final ChaosCraftPlugin plugin;
    private BukkitTask tickTask;

    private long remainingTicks = 0;
    private boolean running = false;
    private boolean paused = false;
    private Runnable onExpire;

    public ModeTimer(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the timer with the given duration in seconds.
     */
    public void start(long seconds) {
        stop();
        this.remainingTicks = seconds * 20L;
        this.running = true;
        this.paused = false;
        startTickTask();
    }

    /**
     * Start the timer with a formatted string: "hh:mm:ss", "mm:ss", "m:ss", or just seconds.
     */
    public void start(String formatted) {
        start(parseTime(formatted));
    }

    /**
     * Stop and reset the timer.
     */
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        remainingTicks = 0;
        running = false;
        paused = false;
    }

    /**
     * Pause the timer without resetting it.
     */
    public void pause() {
        if (running && !paused) {
            paused = true;
        }
    }

    /**
     * Resume a paused timer.
     */
    public void resume() {
        if (running && paused) {
            paused = false;
        }
    }

    /**
     * Add seconds to the running timer.
     */
    public void addTime(long seconds) {
        if (running) {
            remainingTicks += seconds * 20L;
        }
    }

    /**
     * Remove seconds from the running timer. Floors at 0.
     */
    public void removeTime(long seconds) {
        if (running) {
            remainingTicks = Math.max(0, remainingTicks - (seconds * 20L));
        }
    }

    /**
     * Set the callback that fires when the timer reaches zero.
     */
    public void setOnExpire(Runnable onExpire) {
        this.onExpire = onExpire;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public long getRemainingTicks() {
        return remainingTicks;
    }

    public long getRemainingSeconds() {
        return remainingTicks / 20L;
    }

    // ---- Placeholder format methods ----

    /**
     * Format as hh:mm:ss (e.g. "01:30:45")
     */
    public String formatHhMmSs() {
        if (!running) return "0";
        long totalSeconds = getRemainingSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Format as mm:ss (e.g. "90:45" — minutes can exceed 59)
     */
    public String formatMmSs() {
        if (!running) return "0";
        long totalSeconds = getRemainingSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Format as m:ss (e.g. "5:03" — no leading zero on minutes)
     */
    public String formatMSs() {
        if (!running || remainingTicks <= 0) return "0:00";
        long totalSeconds = getRemainingSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Format as ss.ms — total seconds with millisecond fraction from ticks
     */
    public String formatSsMs() {
        if (!running) return "0";
        long totalSeconds = remainingTicks / 20L;
        long remainderTicks = remainingTicks % 20L;
        long millis = remainderTicks * 50; // each tick = 50ms
        return String.format("%d.%03d", totalSeconds, millis);
    }

    /**
     * Raw seconds remaining.
     */
    public String formatRaw() {
        if (!running) return "0";
        return String.valueOf(getRemainingSeconds());
    }

    // ---- Internal ----

    private void startTickTask() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }
                if (paused) {
                    return;
                }
                remainingTicks--;
                if (remainingTicks <= 0) {
                    remainingTicks = 0;
                    running = false;
                    cancel();
                    if (onExpire != null) {
                        onExpire.run();
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /**
     * Parse time strings: "hh:mm:ss", "mm:ss", "m:ss", or plain seconds.
     */
    public static long parseTime(String input) {
        if (input == null || input.isEmpty()) return 0;
        input = input.trim();

        String[] parts = input.split(":");
        try {
            if (parts.length == 3) {
                long h = Long.parseLong(parts[0]);
                long m = Long.parseLong(parts[1]);
                long s = Long.parseLong(parts[2]);
                return h * 3600 + m * 60 + s;
            } else if (parts.length == 2) {
                long m = Long.parseLong(parts[0]);
                long s = Long.parseLong(parts[1]);
                return m * 60 + s;
            } else {
                return Long.parseLong(parts[0]);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
