package com.blockforge.chaoscraft.modes.seer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

/**
 * Manages the 10 crying obsidian orbs in the Seer arena.
 * Each orb destroyed reduces boss max HP by health-per-orb.
 * Breaking an orb halves the breaker's current health and knocks them back.
 *
 * Orbs are marked by vertical beacon-like particle beams and ambient effects.
 */
public class SeerOrbManager implements Listener {

    private final ChaosCraftPlugin plugin;
    private final SeerConfig config;

    private final boolean[] orbDestroyed;
    private final Location[] orbLocations;
    private SeerBossManager bossManager;

    public SeerOrbManager(ChaosCraftPlugin plugin, SeerConfig config) {
        this.plugin = plugin;
        this.config = config;
        int count = config.getOrbCount();
        this.orbDestroyed = new boolean[count];
        this.orbLocations = new Location[count];
    }

    // ========================================================================
    // Setup
    // ========================================================================

    /**
     * Place all orbs in the arena from config positions.
     */
    public void placeOrbs(World world) {
        int count = config.getOrbCount();
        Material orbMaterial;
        try {
            orbMaterial = Material.valueOf(config.getOrbMaterial());
        } catch (IllegalArgumentException e) {
            orbMaterial = Material.CRYING_OBSIDIAN;
        }

        for (int i = 0; i < count; i++) {
            orbDestroyed[i] = false;
            Location loc = config.getOrbPosition(i + 1); // 1-based in config

            if (loc == null) {
                loc = world.getSpawnLocation().clone().add(i * 10, 0, 0);
                plugin.getLogger().warning("[Seer] Orb " + (i + 1) + " has no position set, using fallback.");
            }

            orbLocations[i] = loc.clone();

            // Build obsidian tower base (3 blocks tall pillar)
            Block base = loc.getBlock();
            base.getRelative(0, -3, 0).setType(Material.OBSIDIAN);
            base.getRelative(0, -2, 0).setType(Material.OBSIDIAN);
            base.getRelative(0, -1, 0).setType(Material.OBSIDIAN);

            // Place the crying obsidian orb on top
            base.setType(orbMaterial);

            // Spawn placement effects
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0.5, 0.5, 0.5), 30, 0.5, 1, 0.5, 0.05);
            world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.5f, 0.5f);

            plugin.debug("[Seer] Placed orb " + (i + 1) + " at " + formatLoc(loc));
        }
    }

    // ========================================================================
    // Tick
    // ========================================================================

    /**
     * Called every tick by the mode. Spawns beacon beam and ambient particles.
     */
    public void tick(World world) {
        long gameTime = world.getGameTime();

        for (int i = 0; i < orbLocations.length; i++) {
            if (orbDestroyed[i] || orbLocations[i] == null) continue;

            Location orbCenter = orbLocations[i].clone().add(0.5, 0.5, 0.5);

            // Every 3 ticks: vertical beacon beam (purple dust line from orb to Y+60)
            if (gameTime % 3 == 0) {
                for (double y = orbCenter.getY(); y < orbCenter.getY() + 60; y += 1.5) {
                    world.spawnParticle(Particle.DUST,
                            orbCenter.getX(), y, orbCenter.getZ(),
                            1, 0.15, 0, 0.15, 0.01,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 255), 1.8f));
                }
                // END_ROD scattered along beam for sparkle
                for (double y = orbCenter.getY(); y < orbCenter.getY() + 60; y += 4.0) {
                    world.spawnParticle(Particle.END_ROD,
                            orbCenter.getX(), y, orbCenter.getZ(),
                            1, 0.2, 0, 0.2, 0.01);
                }
            }

            // Every tick: rotating purple dust ring around the orb
            double angle = (gameTime * 0.1) + (i * 0.7); // offset per orb
            double ringRadius = 1.5;
            for (int p = 0; p < 4; p++) {
                double a = angle + (p * Math.PI / 2);
                double px = orbCenter.getX() + Math.cos(a) * ringRadius;
                double pz = orbCenter.getZ() + Math.sin(a) * ringRadius;
                double py = orbCenter.getY() + Math.sin(a * 2) * 0.5;
                world.spawnParticle(Particle.DUST, px, py, pz, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 50, 255), 1.2f));
            }

            // Every 10 ticks: ambient purple burst around orb
            if (gameTime % 10 == 0) {
                world.spawnParticle(Particle.DUST, orbCenter, 10, 1.2, 1.2, 1.2, 0.02,
                        new Particle.DustOptions(Color.fromRGB(160, 0, 200), 1.5f));
                world.spawnParticle(Particle.ENCHANT, orbCenter, 8, 1, 1, 1, 0.5);
            }
        }
    }

    // ========================================================================
    // Block Break Listener
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        Block broken = e.getBlock();
        Location brokenLoc = broken.getLocation();
        Player breaker = e.getPlayer();

        // Check if the broken block matches any undestroyed orb
        int orbIndex = findOrbAt(brokenLoc);
        if (orbIndex < 0) return; // Not an orb

        // Let the vanilla break happen (block disappears naturally)
        // But apply the punishment and boss update

        orbDestroyed[orbIndex] = true;

        // Half the breaker's current health (min 0.5 hearts = 1.0 health)
        double currentHealth = breaker.getHealth();
        double newHealth = Math.max(1.0, currentHealth / config.getOrbBreakHealthDivisor());
        breaker.setHealth(newHealth);

        // Knockback the breaker backward
        Vector knockback = breaker.getLocation().getDirection().multiply(-1).normalize()
                .multiply(config.getOrbBreakKnockback());
        knockback.setY(0.5); // slight upward
        breaker.setVelocity(knockback);

        // Effects at orb location
        World world = brokenLoc.getWorld();
        Location center = brokenLoc.clone().add(0.5, 0.5, 0.5);

        // Sound
        world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.BLOCKS, 2.0f, 0.5f);
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.BLOCKS, 1.5f, 0.3f);

        // Explosion particles
        world.spawnParticle(Particle.DUST, center, 50, 1.5, 1.5, 1.5, 0.1,
                new Particle.DustOptions(Color.fromRGB(170, 0, 255), 2.5f));
        world.spawnParticle(Particle.END_ROD, center, 30, 1, 1, 1, 0.15);
        world.spawnParticle(Particle.DRAGON_BREATH, center, 20, 1, 1, 1, 0.08);
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);

        // Breaker effects
        breaker.playSound(breaker.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 0.5f);

        // Notify boss manager
        int remaining = getOrbsRemaining();
        long newMaxHealth = remaining * config.getOrbHealthPerOrb();

        plugin.getLogger().info("[Seer] Orb " + (orbIndex + 1) + " destroyed by " + breaker.getName()
                + " — " + remaining + " remaining, new max HP: " + newMaxHealth);

        if (bossManager != null) {
            bossManager.updateMaxHealth(newMaxHealth);
        }
    }

    // ========================================================================
    // Queries
    // ========================================================================

    /**
     * Find the orb index at a given location, or -1 if no undestroyed orb is there.
     */
    private int findOrbAt(Location loc) {
        for (int i = 0; i < orbLocations.length; i++) {
            if (orbDestroyed[i] || orbLocations[i] == null) continue;
            if (orbLocations[i].getBlockX() == loc.getBlockX()
                    && orbLocations[i].getBlockY() == loc.getBlockY()
                    && orbLocations[i].getBlockZ() == loc.getBlockZ()
                    && orbLocations[i].getWorld() != null
                    && orbLocations[i].getWorld().equals(loc.getWorld())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if a location matches any undestroyed orb.
     */
    public boolean isOrbAt(Location loc) {
        return findOrbAt(loc) >= 0;
    }

    /**
     * Count remaining undestroyed orbs.
     */
    public int getOrbsRemaining() {
        int count = 0;
        for (boolean destroyed : orbDestroyed) {
            if (!destroyed) count++;
        }
        return count;
    }

    /**
     * Admin command: force-break a specific orb by index (0-based).
     */
    public void forceBreak(int index) {
        if (index < 0 || index >= orbLocations.length) return;
        if (orbDestroyed[index]) return;

        orbDestroyed[index] = true;

        // Remove block
        if (orbLocations[index] != null) {
            Block block = orbLocations[index].getBlock();
            World world = orbLocations[index].getWorld();
            Location center = orbLocations[index].clone().add(0.5, 0.5, 0.5);

            block.setType(Material.AIR);

            // Effects
            if (world != null) {
                world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.BLOCKS, 2.0f, 0.5f);
                world.spawnParticle(Particle.DUST, center, 50, 1.5, 1.5, 1.5, 0.1,
                        new Particle.DustOptions(Color.fromRGB(170, 0, 255), 2.5f));
                world.spawnParticle(Particle.END_ROD, center, 30, 1, 1, 1, 0.15);
            }
        }

        // Notify boss
        int remaining = getOrbsRemaining();
        long newMaxHealth = remaining * config.getOrbHealthPerOrb();

        plugin.getLogger().info("[Seer] Orb " + (index + 1) + " force-broken — "
                + remaining + " remaining.");

        if (bossManager != null) {
            bossManager.updateMaxHealth(newMaxHealth);
        }
    }

    /**
     * Reset all orbs: restore blocks and marks.
     */
    public void resetOrbs() {
        Material orbMaterial;
        try {
            orbMaterial = Material.valueOf(config.getOrbMaterial());
        } catch (IllegalArgumentException e) {
            orbMaterial = Material.CRYING_OBSIDIAN;
        }

        for (int i = 0; i < orbLocations.length; i++) {
            orbDestroyed[i] = false;
            if (orbLocations[i] != null) {
                orbLocations[i].getBlock().setType(orbMaterial);
            }
        }

        plugin.getLogger().info("[Seer] All orbs reset.");
    }

    public boolean isOrbDestroyed(int index) {
        if (index < 1 || index > orbDestroyed.length) return false;
        return orbDestroyed[index - 1];
    }

    public int getDestroyedCount() {
        int count = 0;
        for (boolean d : orbDestroyed) if (d) count++;
        return count;
    }

    public void resetAllOrbs() {
        resetOrbs();
    }

    // ========================================================================
    // Cleanup
    // ========================================================================

    /**
     * Full cleanup — restore orb blocks to AIR, unregister listener.
     */
    public void cleanup() {
        for (int i = 0; i < orbLocations.length; i++) {
            if (orbLocations[i] != null) {
                // Remove orb block + obsidian tower
                Block base = orbLocations[i].getBlock();
                base.setType(Material.AIR);
                base.getRelative(0, -1, 0).setType(Material.AIR);
                base.getRelative(0, -2, 0).setType(Material.AIR);
                base.getRelative(0, -3, 0).setType(Material.AIR);
            }
            orbLocations[i] = null;
        }

        // Unregister this listener
        HandlerList.unregisterAll(this);

        plugin.getLogger().info("[Seer] Orb manager cleaned up.");
    }

    // ========================================================================
    // Setters
    // ========================================================================

    public void setBossManager(SeerBossManager bossManager) {
        this.bossManager = bossManager;
    }

    // ========================================================================
    // Utility
    // ========================================================================

    private String formatLoc(Location loc) {
        return String.format("(%s, %.1f, %.1f, %.1f)",
                loc.getWorld() != null ? loc.getWorld().getName() : "?",
                loc.getX(), loc.getY(), loc.getZ());
    }
}
