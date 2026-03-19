package com.blockforge.chaoscraft.api.mode;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages all registered game modes. Only one mode can be active at a time.
 */
public class ModeManager implements Listener {

    private final ChaosCraftPlugin plugin;
    private final Map<String, AbstractMode> registeredModes = new LinkedHashMap<>();
    private AbstractMode activeMode = null;
    private BukkitTask tickTask = null;

    public ModeManager(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- Registration ----

    public void registerMode(AbstractMode mode) {
        registeredModes.put(mode.getName().toLowerCase(), mode);
        plugin.getLogger().info("Registered mode: " + mode.getName());
    }

    public AbstractMode getMode(String name) {
        return registeredModes.get(name.toLowerCase());
    }

    public Collection<AbstractMode> getAllModes() {
        return Collections.unmodifiableCollection(registeredModes.values());
    }

    public Set<String> getModeNames() {
        return Collections.unmodifiableSet(registeredModes.keySet());
    }

    // ---- Lifecycle ----

    /**
     * Start a mode by name. Returns false if another mode is already active or mode not found.
     */
    public boolean startMode(String name) {
        if (activeMode != null && activeMode.isActive()) {
            return false;
        }

        AbstractMode mode = registeredModes.get(name.toLowerCase());
        if (mode == null) {
            return false;
        }

        activeMode = mode;
        activeMode.setState(ModeState.STARTING);
        activeMode.resetTracking();
        activeMode.loadExemptPlayers();

        // Track all online players as surviving (they haven't died yet)
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            activeMode.trackPlayer(p);
        }

        // Start the mode timer
        var timer = plugin.getModeTimer();
        timer.start(mode.getModeConfig().getDefaultTimerSeconds());
        timer.setOnExpire(this::onTimerExpire);

        // Run on-start commands (Skript hooks etc.)
        activeMode.runStartCommands();

        // Call mode's onStart
        activeMode.onStart();
        activeMode.setState(ModeState.ACTIVE);

        // Start Mode Points session
        var pointsService = plugin.getModePointsService();
        if (pointsService != null) {
            pointsService.startSession();
        }

        // Start Mode Timer HUD
        var timerHud = plugin.getModeTimerHud();
        if (timerHud != null) {
            timerHud.startHud();
        }

        // Start the tick loop
        startTicking();

        plugin.getLogger().info("Mode started: " + mode.getName());
        return true;
    }

    /**
     * End the currently active mode.
     */
    public void endActiveMode() {
        if (activeMode == null) return;

        activeMode.setState(ModeState.ENDING);

        // Stop tick loop
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        // Stop timer
        plugin.getModeTimer().stop();

        // Give rewards to survivors
        activeMode.giveRewards();

        // Run on-end commands
        activeMode.runEndCommands();

        // End Mode Points session
        var pointsService = plugin.getModePointsService();
        if (pointsService != null) {
            pointsService.endSession();
        }

        // Stop Mode Timer HUD
        var timerHud = plugin.getModeTimerHud();
        if (timerHud != null) {
            timerHud.stopHud();
        }

        // Call mode's onEnd
        activeMode.onEnd();

        activeMode.setState(ModeState.INACTIVE);
        plugin.getLogger().info("Mode ended: " + activeMode.getName());
        activeMode = null;
    }

    public boolean isAnyModeActive() {
        return activeMode != null && activeMode.isActive();
    }

    public AbstractMode getActiveMode() {
        return activeMode;
    }

    public String getActiveModeName() {
        return activeMode != null ? activeMode.getName() : "none";
    }

    // ---- Config reload ----

    public void reloadConfigs() {
        for (AbstractMode mode : registeredModes.values()) {
            mode.getModeConfig().load();
        }
    }

    // ---- Internal ----

    private void startTicking() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeMode == null || !activeMode.isActive()) {
                    cancel();
                    return;
                }
                activeMode.onTick();
                // Tick the timer HUD (flash state, sync counter)
                var hud = plugin.getModeTimerHud();
                if (hud != null) hud.tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void onTimerExpire() {
        if (activeMode != null && activeMode.isActive()) {
            plugin.getLogger().info("Mode timer expired for: " + activeMode.getName());
            endActiveMode();
        }
    }

    // ---- Event forwarding ----

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (activeMode != null && activeMode.isActive()) {
            activeMode.trackPlayer(event.getPlayer());
            activeMode.onPlayerJoin(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (activeMode != null && activeMode.isActive()) {
            activeMode.markDeath(event.getEntity());
            activeMode.onPlayerDeath(event.getEntity());
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (activeMode != null && activeMode.isActive()) {
            activeMode.onPlayerChangeDimension(event.getPlayer());
        }
    }
}
