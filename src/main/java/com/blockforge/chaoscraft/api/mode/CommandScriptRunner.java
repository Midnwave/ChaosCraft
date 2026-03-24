package com.blockforge.chaoscraft.api.mode;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Executes a list of commands sequentially with scripting support:
 *   - Regular commands: dispatched immediately as console
 *   - "wait <ticks>": pauses execution for X ticks before continuing
 *   - "done": signals that the mode should fully start (attacks, spawning)
 *   - PlaceholderAPI placeholders (%placeholder%) are resolved in all commands
 *
 * If no "done" is in the command list, onDone is called after all commands finish.
 */
public class CommandScriptRunner {

    private final Plugin plugin;
    private final List<String> commands;
    private final Runnable onDone;
    private final Player triggerPlayer; // Player who started the mode (for PAPI context)
    private int index = 0;
    private boolean doneTriggered = false;
    private final boolean hasPapi;

    public CommandScriptRunner(Plugin plugin, List<String> commands, Runnable onDone, Player triggerPlayer) {
        this.plugin = plugin;
        this.commands = commands;
        this.onDone = onDone;
        this.triggerPlayer = triggerPlayer;
        this.hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public CommandScriptRunner(Plugin plugin, List<String> commands, Runnable onDone) {
        this(plugin, commands, onDone, null);
    }

    /**
     * Begin executing the command list from the start.
     */
    public void execute() {
        processNext();
    }

    private void processNext() {
        while (index < commands.size()) {
            String cmd = commands.get(index).trim();
            index++;

            if (cmd.isEmpty()) continue;

            // Resolve PlaceholderAPI placeholders (%...%)
            cmd = resolvePlaceholders(cmd);

            // "done" — trigger mode full start
            if (cmd.equalsIgnoreCase("done")) {
                doneTriggered = true;
                onDone.run();
                // Continue processing remaining commands after done
                continue;
            }

            // "wait <ticks>" — pause execution
            if (cmd.toLowerCase().startsWith("wait ")) {
                String ticksStr = cmd.substring(5).trim();
                long ticks;
                try {
                    ticks = Long.parseLong(ticksStr);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[Script] Invalid wait value: " + ticksStr);
                    continue;
                }
                if (ticks <= 0) continue;

                // Schedule the rest after the wait
                Bukkit.getScheduler().runTaskLater(plugin, this::processNext, ticks);
                return; // Stop the loop — processNext will be called after the delay
            }

            // Regular command — dispatch as console
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (Exception e) {
                plugin.getLogger().warning("[Script] Error executing command: " + cmd + " — " + e.getMessage());
            }
        }

        // Reached end of command list — if "done" was never called, trigger it now
        if (!doneTriggered) {
            onDone.run();
        }
    }

    /**
     * Resolve PlaceholderAPI placeholders in a string.
     * Uses the trigger player as context, or first online player if no trigger player.
     */
    private String resolvePlaceholders(String input) {
        if (!hasPapi) return input;
        if (!input.contains("%")) return input; // Fast path — no placeholders

        Player context = triggerPlayer;
        if (context == null || !context.isOnline()) {
            var online = Bukkit.getOnlinePlayers();
            if (!online.isEmpty()) {
                context = online.iterator().next();
            }
        }

        try {
            return PlaceholderAPI.setPlaceholders(context, input);
        } catch (Exception e) {
            return input; // Return unresolved if PAPI fails
        }
    }
}
