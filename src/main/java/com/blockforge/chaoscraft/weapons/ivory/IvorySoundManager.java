package com.blockforge.chaoscraft.weapons.ivory;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.Locale;

public class IvorySoundManager {

    private final ChaosCraftPlugin plugin;
    private IvoryConfig config;

    public IvorySoundManager(ChaosCraftPlugin plugin, IvoryConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateConfig(IvoryConfig config) { this.config = config; }

    public void playSwingSound(Player player) {
        playSoundToPlayer(player, config.getSoundSwing(), config.getSoundSwingVolume(), config.getSoundSwingPitch());
    }

    public void playHitSound(Location location) {
        playSound(location, config.getSoundHit(), config.getSoundHitVolume(), config.getSoundHitPitch());
    }

    public void playSound(Location location, String soundKey, float volume, float pitch) {
        if (location == null || soundKey == null || soundKey.isEmpty() || location.getWorld() == null) return;
        volume = Math.clamp(volume, 0.0f, 10.0f);
        pitch = Math.clamp(pitch, 0.5f, 2.0f);
        try {
            var vanillaSound = Sound.valueOf(soundKey.toUpperCase(Locale.ROOT).replace(".", "_").replace(":", "_"));
            location.getWorld().playSound(location, vanillaSound, SoundCategory.PLAYERS, volume, pitch);
        } catch (IllegalArgumentException e) {
            location.getWorld().playSound(location, soundKey, SoundCategory.PLAYERS, volume, pitch);
        }
    }

    public void playSoundToPlayer(Player player, String soundKey, float volume, float pitch) {
        if (player == null || soundKey == null || soundKey.isEmpty()) return;
        Location location = player.getLocation();
        volume = Math.clamp(volume, 0.0f, 10.0f);
        pitch = Math.clamp(pitch, 0.5f, 2.0f);
        try {
            var vanillaSound = Sound.valueOf(soundKey.toUpperCase(Locale.ROOT).replace(".", "_").replace(":", "_"));
            player.playSound(location, vanillaSound, SoundCategory.PLAYERS, volume, pitch);
        } catch (IllegalArgumentException e) {
            player.playSound(location, soundKey, SoundCategory.PLAYERS, volume, pitch);
        }
    }

    public void playSoundNearby(Location location, String soundKey, float volume, float pitch, double radius) {
        if (location == null || soundKey == null || soundKey.isEmpty() || location.getWorld() == null) return;
        double radiusSquared = radius * radius;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSquared) {
                playSoundToPlayer(player, soundKey, volume, pitch);
            }
        }
    }

    public void stopSound(Player player, String soundKey) {
        if (player == null || soundKey == null || soundKey.isEmpty()) return;
        try {
            var vanillaSound = Sound.valueOf(soundKey.toUpperCase(Locale.ROOT).replace(".", "_"));
            player.stopSound(vanillaSound, SoundCategory.PLAYERS);
        } catch (IllegalArgumentException e) {
            player.stopSound(soundKey, SoundCategory.PLAYERS);
        }
    }

    public void playSoundSequence(Location location, Object[][] sounds) {
        if (location == null || sounds == null || sounds.length == 0) return;
        long totalDelay = 0L;
        for (Object[] sound : sounds) {
            if (sound.length < 3) continue;
            String soundKey = (String) sound[0];
            float volume = ((Number) sound[1]).floatValue();
            float pitch = ((Number) sound[2]).floatValue();
            long delay = sound.length > 3 ? ((Number) sound[3]).longValue() : 0L;
            totalDelay += delay;
            Location loc = location.clone();
            if (totalDelay == 0L) {
                playSound(loc, soundKey, volume, pitch);
            } else {
                long capturedDelay = totalDelay;
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> playSound(loc, soundKey, volume, pitch), capturedDelay);
            }
        }
    }

    public void playAbilityActivation(Player player, String soundKey, float volume, float pitch) {
        playSound(player.getLocation(), soundKey, volume, pitch);
    }

    public int playAmbientLoop(Location location, String soundKey, float volume, float pitch, int intervalTicks) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (location.getWorld() != null) playSound(location, soundKey, volume, pitch);
        }, 0L, intervalTicks).getTaskId();
    }

    public void stopAmbientLoop(int taskId) { plugin.getServer().getScheduler().cancelTask(taskId); }

    public void playExplosion(Location location, float volume, float pitch) {
        playSound(location, "ENTITY_GENERIC_EXPLODE", volume, pitch);
    }
    public void playThunder(Location location, float volume, float pitch) {
        playSound(location, "ENTITY_LIGHTNING_BOLT_THUNDER", volume, pitch);
    }
    public void playBeaconActivate(Location location, float volume, float pitch) {
        playSound(location, "BLOCK_BEACON_ACTIVATE", volume, pitch);
    }
    public void playAmethystChime(Location location, float volume, float pitch) {
        playSound(location, "BLOCK_AMETHYST_BLOCK_CHIME", volume, pitch);
    }
    public void playTridentThrow(Location location, float volume, float pitch) {
        playSound(location, "ITEM_TRIDENT_THROW", volume, pitch);
    }
    public void playFireworkBlast(Location location, float volume, float pitch) {
        playSound(location, "ENTITY_FIREWORK_ROCKET_BLAST", volume, pitch);
    }
    public void playConduitAmbient(Location location, float volume, float pitch) {
        playSound(location, "BLOCK_CONDUIT_AMBIENT", volume, pitch);
    }
    public void playRespawnAnchorCharge(Location location, float volume, float pitch) {
        playSound(location, "BLOCK_RESPAWN_ANCHOR_CHARGE", volume, pitch);
    }
}
