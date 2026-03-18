package com.blockforge.chaoscraft.weapons.ivory.abilities.base;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryEffectsManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public abstract class Ability {

    protected final ChaosCraftPlugin plugin;
    protected final IvoryConfig config;
    protected final IvoryEffectsManager effectsManager;

    public Ability(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
        this.plugin = plugin;
        this.config = config;
        this.effectsManager = effectsManager;
    }

    public abstract String getId();
    public abstract String getDisplayName();
    public abstract String getDescription();
    public abstract void execute(Player player);

    protected IvoryConfig.AbilitySettings getSettings() {
        return config.getAbilitySettings(getId());
    }

    protected Location getExecutionLocation(Player player) {
        Location loc = player.getLocation().clone();
        loc.setYaw(0.0F);
        loc.setPitch(0.0F);
        return loc;
    }

    protected Location getForwardLocation(Player player, double distance) {
        return getExecutionLocation(player).add(0, 0, -distance);
    }

    protected void runLater(Runnable task, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    protected int runRepeating(Runnable task, long delayTicks, long periodTicks) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();
    }

    protected void cancelTask(int taskId) {
        plugin.getServer().getScheduler().cancelTask(taskId);
    }
}
