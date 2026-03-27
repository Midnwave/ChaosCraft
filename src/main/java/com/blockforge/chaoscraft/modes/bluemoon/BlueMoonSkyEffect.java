package com.blockforge.chaoscraft.modes.bluemoon;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Smooth sky transition for Blue Moon mode.
 * <p>
 * {@code startTransitionToNight()} — smoothly advances time to midnight (18000)
 * within a configurable max duration (default 20 seconds / 400 ticks).
 * <p>
 * {@code startTransitionToSunrise()} — smoothly advances time from midnight
 * to sunrise (23000) within the same max duration.
 * <p>
 * The transition speed adapts: if the distance is large, multiple ticks are
 * advanced per game tick (2-3x speed). If close, 1 tick per game tick.
 * Time always moves FORWARD (never backward).
 */
public class BlueMoonSkyEffect {

    private final ChaosCraftPlugin plugin;
    private BukkitTask transitionTask;
    private long savedTime = -1;
    private boolean savedDaylightCycle = true;
    private boolean nightActive = false;

    // Configurable (set from BlueMoonConfig)
    private int maxTransitionTicks = 400;  // 20 seconds
    private long targetNightTick = 18000;
    private long targetSunriseTick = 23000;

    public BlueMoonSkyEffect(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Configure transition parameters from BlueMoonConfig.
     */
    public void configure(int maxTransitionSeconds, long nightTick, long sunriseTick) {
        this.maxTransitionTicks = maxTransitionSeconds * 20;
        this.targetNightTick = nightTick;
        this.targetSunriseTick = sunriseTick;
    }

    /**
     * Start smooth transition from current time to midnight (moon zenith).
     * Disables daylight cycle and freezes at target when reached.
     */
    public void startTransitionToNight(World world) {
        if (world == null) return;
        cancelTransition();

        // Save original state
        savedTime = world.getTime();
        Boolean doCycle = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
        savedDaylightCycle = doCycle != null ? doCycle : true;

        long currentTime = world.getTime() % 24000;
        long target = targetNightTick;

        // Calculate forward distance (time always moves forward)
        long distance;
        if (currentTime <= target) {
            distance = target - currentTime;
        } else {
            // Must wrap around: go through rest of day + to target
            distance = (24000 - currentTime) + target;
        }

        // Already at target?
        if (distance == 0 || distance > 23999) {
            world.setTime(target);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            nightActive = true;
            plugin.debug("[SkyEffect] Already at target time " + target + " — frozen.");
            return;
        }

        // Calculate ticks-per-game-tick to finish within maxTransitionTicks
        final int ticksPerStep = Math.max(1, (int) Math.ceil((double) distance / maxTransitionTicks));

        plugin.debug("[SkyEffect] Transitioning to night: current=" + currentTime
                + " target=" + target + " distance=" + distance + " speed=" + ticksPerStep + "t/tick");

        // Disable daylight cycle so only we control time
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

        transitionTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = world.getTime() % 24000;

                // Check if we've reached or passed the target
                long remaining;
                if (now <= target) {
                    remaining = target - now;
                } else {
                    remaining = (24000 - now) + target;
                }

                if (remaining <= ticksPerStep || remaining > 23000) {
                    // Close enough — snap to exact target and stop
                    world.setTime(target);
                    nightActive = true;
                    plugin.debug("[SkyEffect] Night reached. Time frozen at " + target + ".");
                    cancel();
                    return;
                }

                // Advance time forward
                world.setTime(world.getTime() + ticksPerStep);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Start smooth transition from midnight back to sunrise.
     * Re-enables daylight cycle when complete.
     */
    public void startTransitionToSunrise(World world) {
        if (world == null) return;
        cancelTransition();

        long currentTime = world.getTime() % 24000;
        long target = targetSunriseTick;

        // Forward distance from current (should be 18000) to sunrise (23000)
        long distance;
        if (currentTime <= target) {
            distance = target - currentTime;
        } else {
            distance = (24000 - currentTime) + target;
        }

        if (distance == 0 || distance > 23999) {
            world.setTime(target);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            nightActive = false;
            plugin.debug("[SkyEffect] Already at sunrise — cycle restored.");
            return;
        }

        final int ticksPerStep = Math.max(1, (int) Math.ceil((double) distance / maxTransitionTicks));

        plugin.debug("[SkyEffect] Transitioning to sunrise: current=" + currentTime
                + " target=" + target + " distance=" + distance + " speed=" + ticksPerStep + "t/tick");

        transitionTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = world.getTime() % 24000;

                long remaining;
                if (now <= target) {
                    remaining = target - now;
                } else {
                    remaining = (24000 - now) + target;
                }

                if (remaining <= ticksPerStep || remaining > 23000) {
                    world.setTime(target);
                    // Restore daylight cycle
                    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, savedDaylightCycle);
                    nightActive = false;
                    plugin.debug("[SkyEffect] Sunrise reached. Daylight cycle restored.");
                    cancel();
                    return;
                }

                world.setTime(world.getTime() + ticksPerStep);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Force-stop any active transition and restore original state.
     * Called on mode end / cleanup.
     */
    public void forceRestore(World world) {
        cancelTransition();
        if (world != null) {
            if (savedTime >= 0) {
                world.setTime(savedTime);
            }
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, savedDaylightCycle);
        }
        nightActive = false;
        savedTime = -1;
        plugin.debug("[SkyEffect] Force restored time and daylight cycle.");
    }

    /**
     * Cancel any running transition task.
     */
    public void cancelTransition() {
        if (transitionTask != null && !transitionTask.isCancelled()) {
            transitionTask.cancel();
            transitionTask = null;
        }
    }

    public boolean isNightActive() {
        return nightActive;
    }

    public boolean isTransitioning() {
        return transitionTask != null && !transitionTask.isCancelled();
    }

    public long getSavedTime() {
        return savedTime;
    }
}
