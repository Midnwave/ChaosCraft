package com.blockforge.chaoscraft.modes.calamity.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ravager;
import org.bukkit.scheduler.BukkitRunnable;

public class DoGManager {
    private final ChaosCraftPlugin plugin;
    private boolean alive = false;
    private boolean phase2 = false;
    private double currentHealth = 0;
    private double maxHealth = 12000000;

    public DoGManager(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawn(World endWorld) {
        // Run datapack spawn function
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
            "execute in minecraft:the_end run function mc_calamity:debug/spawn/devourer_of_gods");
        alive = true;
        phase2 = false;
        startHealthPolling();
    }

    public void kill() {
        // Kill all DoG entities
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
            "kill @e[tag=mc_calamity.entity.devourer_of_gods]");
        alive = false;
        phase2 = false;
    }

    private void startHealthPolling() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!alive) { cancel(); return; }

                World end = Bukkit.getWorld("world_the_end");
                if (end == null) { end = Bukkit.getWorld("the_end"); }
                if (end == null) { cancel(); return; }

                // Find any DoG hitbox ravager
                Ravager hitbox = null;
                for (Entity e : end.getEntities()) {
                    if (e instanceof Ravager r && e.getScoreboardTags().contains("mc_calamity.entity.devourer_of_gods.hitbox")) {
                        hitbox = r;
                        break;
                    }
                }

                if (hitbox == null) {
                    // DoG is dead
                    alive = false;
                    plugin.getLogger().info("[DoG] Devourer of Gods has been defeated!");
                    cancel();
                    return;
                }

                currentHealth = hitbox.getHealth();
                maxHealth = hitbox.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();

                // Check phase transition
                if (!phase2 && currentHealth <= maxHealth * 0.5) {
                    phase2 = true;
                    plugin.getLogger().info("[DoG] Universal Collapse triggered! Phase 2 active.");
                }
            }
        }.runTaskTimer(plugin, 20L, 10L); // Poll every 0.5 seconds
    }

    // Getters for placeholder/bossbar integration
    public boolean isAlive() { return alive; }
    public boolean isPhase2() { return phase2; }
    public double getCurrentHealth() { return currentHealth; }
    public double getMaxHealth() { return maxHealth; }
    public double getHealthPercent() { return maxHealth > 0 ? (currentHealth / maxHealth) * 100.0 : 0; }
}
