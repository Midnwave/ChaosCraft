package com.blockforge.chaoscraft.modes.calamity.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityConfig;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Manages the Ender Dragon during Calamity mode:
 * - Spawns the dragon immediately when Calamity starts
 * - Makes it completely invincible (cancel all damage + Resistance 255)
 * - Dragon orbits the island throughout the entire mode
 * - In Phase 4: remove invincibility so players can fight it
 * - Tracks dragon death for phase completion
 * - Mini boss management for Phase 4 (invincible while mini bosses alive)
 */
public class DragonManager implements Listener {

    private final ChaosCraftPlugin plugin;
    private final CalamityConfig config;

    private EnderDragon dragon;
    private UUID dragonUuid;
    private boolean active = false;
    private boolean invincible = true;
    private boolean phase4Active = false;
    private int aliveMiniBosse = 0;

    // Callback when dragon dies in Phase 4
    private Runnable onDragonDeath;

    public DragonManager(ChaosCraftPlugin plugin, CalamityConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ========================
    // Lifecycle
    // ========================

    /**
     * Spawn the Ender Dragon and make it invincible.
     * Called when Calamity mode starts.
     */
    public void spawnInvincibleDragon() {
        active = true;
        invincible = true;
        phase4Active = false;

        World endWorld = getEndWorld();
        if (endWorld == null) {
            plugin.getLogger().warning("[Calamity] Cannot spawn dragon — The End world not found!");
            return;
        }

        // Remove any existing dragon first
        removeExistingDragons(endWorld);

        // Spawn the dragon
        Location spawnLoc = new Location(endWorld, 0, 80, 0);
        dragon = (EnderDragon) endWorld.spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
        dragonUuid = dragon.getUniqueId();

        // Apply Resistance 255 as backup invincibility
        dragon.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255, false, false, false
        ));

        // Set phase to circling (orbiting the island)
        dragon.setPhase(EnderDragon.Phase.CIRCLING);

        plugin.getLogger().info("[Calamity] Invincible Ender Dragon spawned.");

        // Spawn effects
        endWorld.spawnParticle(Particle.DUST, spawnLoc, 100,
                5, 5, 5, 0,
                new Particle.DustOptions(Color.fromRGB(128, 0, 255), 3.0f));
        endWorld.playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
    }

    /**
     * Stop managing the dragon and clean up.
     */
    public void stop() {
        active = false;
        invincible = true;
        phase4Active = false;
        aliveMiniBosse = 0;
        onDragonDeath = null;

        if (dragon != null && dragon.isValid()) {
            dragon.remove();
        }
        dragon = null;
        dragonUuid = null;
    }

    // ========================
    // Invincibility management
    // ========================

    /**
     * Cancel ALL damage to the dragon while invincible flag is set.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDragonDamage(EntityDamageEvent event) {
        if (!active) return;
        if (!(event.getEntity() instanceof EnderDragon)) return;
        if (dragonUuid == null || !event.getEntity().getUniqueId().equals(dragonUuid)) return;

        if (invincible) {
            event.setCancelled(true);
            return;
        }

        // Phase 4: invincible while mini bosses are still alive
        if (phase4Active && aliveMiniBosse > 0) {
            event.setCancelled(true);

            // Visual feedback that dragon is still shielded
            if (event instanceof EntityDamageByEntityEvent byEntity) {
                if (byEntity.getDamager() instanceof org.bukkit.entity.Player player) {
                    Location dragonLoc = event.getEntity().getLocation();
                    player.getWorld().spawnParticle(Particle.DUST, dragonLoc, 10,
                            1, 1, 1, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 50, 50), 1.5f));
                    player.playSound(dragonLoc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
                }
            }
        }
    }

    /**
     * Track dragon death for Phase 4 completion.
     */
    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (!active) return;
        if (!(event.getEntity() instanceof EnderDragon)) return;
        if (dragonUuid == null || !event.getEntity().getUniqueId().equals(dragonUuid)) return;

        plugin.getLogger().info("[Calamity] Ender Dragon has been slain!");

        // Clean up
        dragon = null;
        dragonUuid = null;

        if (onDragonDeath != null) {
            onDragonDeath.run();
        }
    }

    // ========================
    // Phase 4: Make dragon fightable
    // ========================

    /**
     * Remove invincibility — the dragon can now be damaged.
     * Called at the start of Phase 4 (Boss 4).
     */
    public void makeVulnerable() {
        invincible = false;
        phase4Active = true;

        if (dragon != null && dragon.isValid()) {
            // Remove Resistance
            dragon.removePotionEffect(PotionEffectType.RESISTANCE);

            // Set dragon health to configured value
            AttributeInstance maxHealth = dragon.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double configuredHealth = config.get().getDouble("bosses.dragon.health", 200.0);
                maxHealth.setBaseValue(configuredHealth);
                dragon.setHealth(configuredHealth);
            }

            // Change phase to attack
            dragon.setPhase(EnderDragon.Phase.CHARGE_PLAYER);

            // Announcement effects
            World world = dragon.getWorld();
            world.spawnParticle(Particle.DUST, dragon.getLocation(), 200,
                    10, 10, 10, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 0, 0), 3.0f));
            world.playSound(dragon.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.3f);
            world.playSound(dragon.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);

            plugin.getLogger().info("[Calamity] Ender Dragon is now vulnerable!");
        }
    }

    /**
     * Set the current count of alive mini bosses.
     * Dragon is invincible while any mini boss is alive.
     */
    public void setAliveMiniBosse(int count) {
        this.aliveMiniBosse = count;

        // If all mini bosses dead and Phase 4 active, make sure dragon is truly fightable
        if (phase4Active && count <= 0 && dragon != null && dragon.isValid()) {
            invincible = false;
            dragon.removePotionEffect(PotionEffectType.RESISTANCE);
            plugin.getLogger().info("[Calamity] All mini bosses defeated — Dragon shield removed!");

            World world = dragon.getWorld();
            world.spawnParticle(Particle.DUST, dragon.getLocation(), 100,
                    8, 8, 8, 0,
                    new Particle.DustOptions(Color.fromRGB(0, 255, 100), 2.0f));
            world.playSound(dragon.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.0f);
        }
    }

    public void decrementMiniBoss() {
        aliveMiniBosse = Math.max(0, aliveMiniBosse - 1);
        setAliveMiniBosse(aliveMiniBosse);
    }

    // ========================
    // Dragon state
    // ========================

    public void setOnDragonDeath(Runnable callback) {
        this.onDragonDeath = callback;
    }

    public EnderDragon getDragon() {
        return dragon;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public boolean isPhase4Active() {
        return phase4Active;
    }

    public boolean isAlive() {
        return dragon != null && dragon.isValid() && !dragon.isDead();
    }

    // ========================
    // Helpers
    // ========================

    private void removeExistingDragons(World endWorld) {
        for (Entity entity : endWorld.getEntities()) {
            if (entity instanceof EnderDragon existingDragon) {
                existingDragon.remove();
            }
        }
    }

    private World getEndWorld() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) return world;
        }
        return null;
    }
}
