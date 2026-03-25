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
        var resultsService = plugin.getModeResultsService();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            activeMode.trackPlayer(p);
            if (resultsService != null) {
                resultsService.trackPlayer(p.getUniqueId());
            }
        }

        // Timer is NOT started here — it's controlled by /cc function startmodetimer
        // in the on-start-commands. This allows each mode to set its own duration/flash.
        // Set the expire callback so when the timer eventually runs out, mode ends.
        plugin.getModeTimer().setOnExpire(this::onTimerExpire);

        // Start music immediately (before on-start-commands and done)
        String musicId = mode.getModeConfig().getMusic();
        if (musicId != null && !musicId.isEmpty()) {
            plugin.getMusicManager().playModeMusic(mode);
        }

        // Start the tick loop early so HUD ticks and placeholders update during chargeup
        startTicking();

        // Execute on-start-commands with scripting support (wait, done).
        // "done" in the command list triggers finishModeStart() — this is when
        // attacks/spawning/music actually begin. If no "done", it fires after all commands.
        var commands = mode.getModeConfig().getOnStartCommands();
        new CommandScriptRunner(plugin, commands, this::finishModeStart).execute();

        plugin.getLogger().info("Mode starting: " + mode.getName() + " (running on-start-commands...)");
        return true;
    }

    /**
     * Called by CommandScriptRunner when "done" is reached (or end of commands).
     * This triggers the mode's actual gameplay — attacks, spawning, music, etc.
     */
    private void finishModeStart() {
        if (activeMode == null) return;

        // Call mode's onStart — this starts attacks, schedulers, music
        activeMode.onStart();
        activeMode.setState(ModeState.ACTIVE);

        // Start Mode Points session
        var pointsService = plugin.getModePointsService();
        if (pointsService != null) {
            pointsService.startSession();
        }

        plugin.getLogger().info("Mode fully started: " + activeMode.getName());
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

        // Track mode survivals for surviving players
        var statsService = plugin.getPlayerStatsService();
        if (statsService != null) {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (activeMode.hasSurvived(p)) {
                    statsService.incrementModeSurvivals(p.getUniqueId());
                }
            }
        }

        // Give rewards to survivors
        activeMode.giveRewards();

        // Grant tutorial completion bonus to players who finished all steps
        if (activeMode instanceof com.blockforge.chaoscraft.modes.tutorial.TutorialMode tutorialMode) {
            var modeResults = plugin.getModeResultsService();
            if (modeResults != null) {
                var tutTracker = tutorialMode.getTracker();
                for (java.util.UUID uuid : tutTracker.getCompletedPlayers()) {
                    Player completedPlayer = plugin.getServer().getPlayer(uuid);
                    if (completedPlayer != null && completedPlayer.isOnline()) {
                        modeResults.grantBonus(completedPlayer, "tutorial-complete", activeMode.getModeConfig().get());
                    }
                }
            }
        }

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

        // Schedule results display (10 seconds after mode ends)
        var modeResults = plugin.getModeResultsService();
        if (modeResults != null) {
            modeResults.scheduleResults(activeMode);
        }

        // Restore spectating players to survival mode
        var restrictionListener = plugin.getModeRestrictionListener();
        if (restrictionListener != null) {
            restrictionListener.restoreSpectators();
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
            // Run on-player-ready-commands for this player (world change during mode)
            runPlayerReadyCommands(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        // No action needed on quit — player UUID stays in tracking sets.
        // On rejoin: title screen → verifyexittitlescreen → on-player-ready-commands
        if (activeMode != null && activeMode.isActive()) {
            plugin.debug("[Mode] " + event.getPlayer().getName() + " quit during active mode "
                    + activeMode.getName());
        }
    }

    /**
     * Run on-player-ready-commands for a specific player.
     * Called when: player exits title screen (verifyexittitlescreen), changes world.
     * Uses CommandScriptRunner with full wait/done/PAPI support.
     */
    public void runPlayerReadyCommands(org.bukkit.entity.Player player) {
        if (activeMode == null || !activeMode.isActive()) return;

        var commands = activeMode.getModeConfig().getOnPlayerReadyCommands();
        if (commands.isEmpty()) return;

        // Replace %player% in all commands
        var resolved = new java.util.ArrayList<String>();
        for (String cmd : commands) {
            resolved.add(cmd.replace("%player%", player.getName()));
        }

        new CommandScriptRunner(plugin, resolved, () -> {
            // on-player-ready "done" callback — no special action needed
        }, player).execute();

        // Ensure music is playing
        plugin.getMusicManager().playForPlayer(player);

        plugin.debug("[Mode] Ran on-player-ready-commands for " + player.getName());
    }
}
