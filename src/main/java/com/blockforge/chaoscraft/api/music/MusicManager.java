package com.blockforge.chaoscraft.api.music;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages per-mode music playback. Supports looping and per-phase music overrides.
 * Music replays on: join, dimension change, phase change.
 *
 * Uses Bukkit's native playSound(Location, String, SoundCategory, volume, pitch)
 * with the player's current location and world for reliable custom resource pack sounds.
 */
public class MusicManager implements Listener {

    private final ChaosCraftPlugin plugin;

    // Currently playing sound ID (from resource pack)
    private String currentSoundId = null;
    private boolean looping = false;
    private long durationTicks = 0;
    private BukkitTask loopTask = null;

    // Per-phase music overrides (phase number -> sound ID)
    private final Map<Integer, MusicEntry> phaseMusic = new HashMap<>();

    public MusicManager(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start playing mode music for all online players.
     */
    public void playModeMusic(AbstractMode mode) {
        stopAll();
        var config = mode.getModeConfig();
        String soundId = config.getMusic();
        if (soundId == null || soundId.isEmpty()) {
            plugin.debug("[Music] No sound-id configured for mode " + mode.getName() + " — skipping.");
            return;
        }

        currentSoundId = soundId;
        looping = config.isMusicLooped();
        durationTicks = config.getMusicDurationTicks();

        plugin.getLogger().info("[Music] Playing '" + soundId + "' for mode " + mode.getName()
                + " (loop=" + looping + ", duration=" + durationTicks + " ticks)");

        // Small delay to ensure mode is fully initialized before sound packets
        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentSoundId != null) {
                    playForAll();
                }
            }
        }.runTaskLater(plugin, 10L);

        if (looping && durationTicks > 0) {
            startLoop();
        }
    }

    /**
     * Switch to a specific phase's music. If no phase override exists, keeps current.
     */
    public void playPhaseMusic(int phase) {
        MusicEntry entry = phaseMusic.get(phase);
        if (entry == null) return;

        stopAll();
        currentSoundId = entry.soundId;
        looping = entry.loop;
        durationTicks = entry.durationTicks;

        playForAll();

        if (looping && durationTicks > 0) {
            startLoop();
        }
    }

    /**
     * Register a per-phase music override.
     */
    public void setPhaseMusic(int phase, String soundId, boolean loop, long durationTicks) {
        phaseMusic.put(phase, new MusicEntry(soundId, loop, durationTicks));
    }

    /**
     * Clear all phase music registrations.
     */
    public void clearPhaseMusic() {
        phaseMusic.clear();
    }

    /**
     * Play current music for a specific player (used on join/dimension change).
     * Uses Bukkit's native playSound with explicit Location for reliable playback.
     */
    public void playForPlayer(Player player) {
        if (currentSoundId == null || currentSoundId.isEmpty()) return;
        if (!player.isOnline()) return;

        stopForPlayer(player);

        // Use Bukkit native API with explicit location in the player's world
        // This is more reliable than Adventure API for custom resource pack sounds
        player.playSound(player.getLocation(), currentSoundId, SoundCategory.MASTER, 1.0f, 1.0f);
        plugin.debug("[Music] Played '" + currentSoundId + "' for " + player.getName()
                + " at " + player.getLocation().getWorld().getName()
                + " (" + (int) player.getLocation().getX() + ", "
                + (int) player.getLocation().getY() + ", "
                + (int) player.getLocation().getZ() + ")");
    }

    /**
     * Stop all music for all players.
     */
    public void stopAll() {
        if (loopTask != null) {
            loopTask.cancel();
            loopTask = null;
        }
        if (currentSoundId != null) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                stopForPlayer(p);
            }
            plugin.debug("[Music] Stopped '" + currentSoundId + "' for all players.");
        }
        currentSoundId = null;
    }

    public void reload() {
        phaseMusic.clear();
    }

    public String getCurrentSoundId() {
        return currentSoundId;
    }

    // ---- Internal ----

    private void playForAll() {
        int count = 0;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            playForPlayer(p);
            count++;
        }
        plugin.debug("[Music] playForAll — sent to " + count + " players.");
    }

    private void stopForPlayer(Player player) {
        if (currentSoundId != null) {
            // Stop by both name+category to ensure it actually stops
            player.stopSound(currentSoundId, SoundCategory.MASTER);
        }
    }

    private void startLoop() {
        // Offset loop start by the initial delay (10 ticks) so first loop fires at the right time
        loopTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentSoundId == null) {
                    cancel();
                    return;
                }
                plugin.debug("[Music] Loop tick — replaying '" + currentSoundId + "'");
                playForAll();
            }
        }.runTaskTimer(plugin, durationTicks, durationTicks);
    }

    // ---- Events ----

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (currentSoundId != null) {
            // Delay to let client fully load and resource pack apply
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (currentSoundId != null && event.getPlayer().isOnline()) {
                        playForPlayer(event.getPlayer());
                    }
                }
            }.runTaskLater(plugin, 40L);
        }
    }

    @EventHandler
    public void onDimensionChange(PlayerChangedWorldEvent event) {
        if (currentSoundId != null) {
            // Replay music after dimension change
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (currentSoundId != null && event.getPlayer().isOnline()) {
                        playForPlayer(event.getPlayer());
                    }
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    // ---- Data class ----

    private record MusicEntry(String soundId, boolean loop, long durationTicks) {}
}
