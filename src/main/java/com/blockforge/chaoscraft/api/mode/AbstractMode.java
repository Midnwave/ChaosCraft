package com.blockforge.chaoscraft.api.mode;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Base class all game modes extend. Provides lifecycle hooks, state tracking,
 * exempt player management, and survival tracking.
 */
public abstract class AbstractMode {

    protected final ChaosCraftPlugin plugin;
    protected final String name;
    protected final ModeConfig modeConfig;
    protected ModeState state = ModeState.INACTIVE;

    // Tracks which players have survived without dying
    private final Set<UUID> survivedPlayers = new HashSet<>();
    // Tracks which players have died at least once
    private final Set<UUID> diedPlayers = new HashSet<>();
    // Exempt players (no attack damage/spawning)
    private final Set<UUID> exemptPlayers = new HashSet<>();
    // Whether the boss was killed during this mode session (for boss-killed rewards)
    private boolean bossKilled = false;

    protected AbstractMode(ChaosCraftPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.modeConfig = new ModeConfig(plugin, name);
    }

    // ---- Lifecycle hooks ----

    /** Called when the mode starts. Set up arena, spawn entities, etc. */
    public abstract void onStart();

    /** Called every tick while the mode is ACTIVE. */
    public abstract void onTick();

    /** Called when the mode ends (timer expired, manual end, or win). */
    public abstract void onEnd();

    /** Called when a player joins the server while this mode is active. */
    public abstract void onPlayerJoin(Player player);

    /** Called when a player dies while this mode is active. */
    public abstract void onPlayerDeath(Player player);

    /** Called when a player changes dimension while this mode is active. */
    public abstract void onPlayerChangeDimension(Player player);

    // ---- State management ----

    public String getName() {
        return name;
    }

    public ModeState getState() {
        return state;
    }

    public void setState(ModeState state) {
        this.state = state;
    }

    public boolean isActive() {
        return state == ModeState.ACTIVE || state == ModeState.STARTING;
    }

    public ModeConfig getModeConfig() {
        return modeConfig;
    }

    // ---- Survival tracking ----

    public void trackPlayer(Player player) {
        survivedPlayers.add(player.getUniqueId());
    }

    public void markDeath(Player player) {
        diedPlayers.add(player.getUniqueId());
        survivedPlayers.remove(player.getUniqueId());
    }

    public boolean hasSurvived(Player player) {
        return survivedPlayers.contains(player.getUniqueId()) && !diedPlayers.contains(player.getUniqueId());
    }

    public void resetTracking() {
        survivedPlayers.clear();
        diedPlayers.clear();
        bossKilled = false;
    }

    // ---- Boss killed tracking ----

    /** Mark that the boss was killed during this session. Call from BossManager.onBossDeath(). */
    public void setBossKilled(boolean killed) { this.bossKilled = killed; }

    /** Whether the boss was killed during this mode session. */
    public boolean isBossKilled() { return bossKilled; }

    // ---- Exempt players ----

    public void loadExemptPlayers() {
        exemptPlayers.clear();
        for (String name : modeConfig.getExemptPlayers()) {
            var player = plugin.getServer().getPlayerExact(name);
            if (player != null) {
                exemptPlayers.add(player.getUniqueId());
            }
        }
    }

    public void addExempt(Player player) {
        exemptPlayers.add(player.getUniqueId());
    }

    public void removeExempt(Player player) {
        exemptPlayers.remove(player.getUniqueId());
    }

    public boolean isExempt(Player player) {
        return exemptPlayers.contains(player.getUniqueId())
                || player.hasPermission("chaoscraft.mode.exempt");
    }

    public Set<UUID> getExemptPlayers() {
        return Collections.unmodifiableSet(exemptPlayers);
    }

    // ---- Utility ----

    /** Execute configured on-start commands. */
    protected void runStartCommands() {
        for (String cmd : modeConfig.getOnStartCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }
    }

    /** Execute configured on-end commands. */
    protected void runEndCommands() {
        for (String cmd : modeConfig.getOnEndCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }
    }

    /** Execute reward commands for players who survived. */
    protected void giveRewards() {
        for (UUID uuid : survivedPlayers) {
            var player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline() && !diedPlayers.contains(uuid)) {
                for (String cmd : modeConfig.getRewardCommands()) {
                    String resolved = cmd.replace("%player%", player.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), resolved);
                }
            }
        }
    }
}
