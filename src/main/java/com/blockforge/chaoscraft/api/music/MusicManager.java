package com.blockforge.chaoscraft.api.music;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
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
        if (soundId == null || soundId.isEmpty()) return;

        currentSoundId = soundId;
        looping = config.isMusicLooped();
        durationTicks = config.getMusicDurationTicks();

        playForAll();

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
     */
    public void playForPlayer(Player player) {
        if (currentSoundId == null || currentSoundId.isEmpty()) return;
        stopForPlayer(player);
        player.playSound(Sound.sound(
                Key.key(currentSoundId),
                Sound.Source.MUSIC,
                1.0f, 1.0f
        ));
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
        }
        currentSoundId = null;
    }

    public void reload() {
        phaseMusic.clear();
    }

    // ---- Internal ----

    private void playForAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            playForPlayer(p);
        }
    }

    private void stopForPlayer(Player player) {
        if (currentSoundId != null) {
            player.stopSound(SoundStop.named(Key.key(currentSoundId)));
        }
    }

    private void startLoop() {
        loopTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentSoundId == null) {
                    cancel();
                    return;
                }
                playForAll();
            }
        }.runTaskTimer(plugin, durationTicks, durationTicks);
    }

    // ---- Events ----

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (currentSoundId != null) {
            // Slight delay to let client load
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
