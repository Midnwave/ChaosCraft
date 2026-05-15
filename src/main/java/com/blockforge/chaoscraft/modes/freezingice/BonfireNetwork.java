package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * FreezingIce Bonfire Network — warmth sources the players must hunt down.
 *
 * <p>A minimum number of bonfires are always active in the arena. Each
 * bonfire is a small visual structure (NETHERRACK pit + MAGMA_BLOCK heart
 * + LANTERN canopy + warm DUST ring at the heat boundary + vertical
 * END_ROD beam for long-range visibility + crackle SFX). Players standing
 * within the bonfire's {@code heat-radius} have their frostbite reduced.
 *
 * <p>Bonfires expire after a configurable lifetime — they despawn and
 * respawn elsewhere, so players cannot simply camp one safe spot. Attacks
 * still spawn freely inside a bonfire's heat radius — the bonfire only
 * protects against frostbite, not against attacks.
 */
public class BonfireNetwork {

    private final ChaosCraftPlugin plugin;
    private final FreezingIceMode mode;
    private final FreezingIceConfig config;
    private final DisplayBuilder displayBuilder;
    private final Random random = new Random();

    private final List<Bonfire> active = new ArrayList<>();
    private long tickCounter = 0L;
    private long nextSpawnTick = 0L;
    private BukkitTask task;

    public BonfireNetwork(ChaosCraftPlugin plugin, FreezingIceMode mode, FreezingIceConfig config) {
        this.plugin = plugin;
        this.mode = mode;
        this.config = config;
        this.displayBuilder = new DisplayBuilder(plugin, "freezingice");
    }

    // ========================
    // Lifecycle
    // ========================

    public void start() {
        if (task != null) return;
        tickCounter = 0L;
        nextSpawnTick = 0L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 4L, 4L);
        plugin.getLogger().info("[FreezingIce] BonfireNetwork started.");
    }

    public void stop() {
        if (task != null) {
            try { task.cancel(); } catch (Throwable ignored) {}
            task = null;
        }
        // Remove every spawned display
        for (Bonfire b : active) {
            cleanupBonfire(b);
        }
        active.clear();
        plugin.getLogger().info("[FreezingIce] BonfireNetwork stopped.");
    }

    // ========================
    // Tick driver (runs every 4 game ticks)
    // ========================

    private void tick() {
        tickCounter += 4;

        // 1. Expire bonfires past their lifetime
        Iterator<Bonfire> it = active.iterator();
        while (it.hasNext()) {
            Bonfire b = it.next();
            if (tickCounter >= b.expireAtTick) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[FreezingIce] Bonfire expired at "
                            + b.center.getBlockX() + "," + b.center.getBlockY()
                            + "," + b.center.getBlockZ());
                }
                cleanupBonfire(b);
                it.remove();
            }
        }

        // 2. Spawn new bonfires up to minimum count (respecting respawn delay)
        int minCount = Math.max(0, config.getBonfireMinCount());
        int maxCount = Math.max(minCount, config.getBonfireMaxCount());
        if (active.size() < minCount && tickCounter >= nextSpawnTick) {
            Bonfire spawned = spawnBonfireRandom();
            if (spawned != null) {
                nextSpawnTick = tickCounter + Math.max(0, config.getBonfireRespawnMinDelayTicks());
            }
        }
        // Cap: never exceed max-active (admin /spawnbonfire could push over min,
        // but the auto-spawner shouldn't blow past max).
        // (No-op here — auto path only fires when below min, which is <= max.)
        // The maxCount is enforced indirectly via min logic; harden if needed:
        if (active.size() > maxCount) {
            // Trim oldest (front of list) until at cap
            while (active.size() > maxCount) {
                Bonfire b = active.remove(0);
                cleanupBonfire(b);
            }
        }

        // 3. Render particle/sound signature for each active bonfire
        for (Bonfire b : active) {
            renderBonfireFx(b);
        }
    }

    // ========================
    // Spawn API
    // ========================

    /**
     * Spawn a bonfire at the given location (admin/debug entry point).
     * Returns the new Bonfire, or null if the world is unavailable.
     */
    public Bonfire spawnBonfire(Location at) {
        if (at == null || at.getWorld() == null) return null;
        World world = at.getWorld();
        int gy = world.getHighestBlockYAt(at.getBlockX(), at.getBlockZ());
        Location center = new Location(world,
                at.getBlockX() + 0.5,
                gy + 0.5,
                at.getBlockZ() + 0.5,
                0.0f, 0.0f);

        Bonfire b = new Bonfire();
        b.center = center;
        b.expireAtTick = tickCounter + Math.max(20L, (long) config.getBonfireLifetimeTicks());

        // ── Visual structure ──
        // 1. NETHERRACK base — firepit floor (wide & flat)
        try {
            BlockDisplayHandle base = displayBuilder.spawnBlock(
                    center.clone().add(0, 0, 0), Material.NETHERRACK);
            base.scale(1.8f, 0.3f, 1.8f).translate(-0.9f, -0.5f, -0.9f).glow(120, 60, 30);
            b.displays.add(base.entity());
        } catch (Throwable ignored) {}

        // 2. MAGMA_BLOCK center — heart of the fire
        try {
            BlockDisplayHandle heart = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.2, 0), Material.MAGMA_BLOCK);
            heart.scale(1.0f, 0.5f, 1.0f).translate(-0.5f, -0.5f, -0.5f).glow(255, 100, 30);
            b.displays.add(heart.entity());
        } catch (Throwable ignored) {}

        // 3. Four NETHERRACK ring stones at cardinals (radius 0.7)
        double[][] cardinal = {{0.7, 0.0, 0.0}, {-0.7, 0.0, 0.0}, {0.0, 0.0, 0.7}, {0.0, 0.0, -0.7}};
        for (double[] off : cardinal) {
            try {
                BlockDisplayHandle stone = displayBuilder.spawnBlock(
                        center.clone().add(off[0], 0.0, off[2]), Material.NETHERRACK);
                stone.scale(0.5f, 0.5f, 0.5f).translate(-0.25f, -0.5f, -0.25f).glow(160, 70, 30);
                b.displays.add(stone.entity());
            } catch (Throwable ignored) {}
        }

        // 4. LANTERN canopy above the pit
        try {
            Material canopy = Material.LANTERN;
            BlockDisplayHandle lantern = displayBuilder.spawnBlock(
                    center.clone().add(0, 1.5, 0), canopy);
            lantern.scale(0.8f, 0.8f, 0.8f).translate(-0.4f, -0.4f, -0.4f).glow(255, 180, 80);
            b.displays.add(lantern.entity());
        } catch (Throwable ignored) {}

        active.add(b);
        return b;
    }

    /**
     * Pick a random point within the arena (mode arena center + arena radius)
     * and spawn a bonfire there. Returns null if no arena center is available.
     */
    public Bonfire spawnBonfireRandom() {
        Location arenaCenter = mode != null ? mode.getArenaCenter() : null;
        if (arenaCenter == null || arenaCenter.getWorld() == null) {
            // Fall back to the mode's world spawn if no arena center
            World w = mode != null ? mode.getIceWorld() : null;
            if (w == null) return null;
            arenaCenter = w.getSpawnLocation();
        }
        double radius = Math.max(1.0, config.getArenaRadius());
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(random.nextDouble()) * radius;
        double dx = Math.cos(angle) * dist;
        double dz = Math.sin(angle) * dist;
        Location at = arenaCenter.clone().add(dx, 0, dz);
        return spawnBonfire(at);
    }

    // ========================
    // Heat radius check
    // ========================

    /**
     * Returns true if {@code p} is currently inside any active bonfire's
     * heat radius (same-world only).
     */
    public boolean isPlayerNearBonfire(Player p) {
        if (p == null) return false;
        Location pl = p.getLocation();
        World pw = pl.getWorld();
        if (pw == null) return false;
        double heat = Math.max(0.1, config.getBonfireHeatRadius());
        double heatSq = heat * heat;
        for (Bonfire b : active) {
            if (b.center == null || b.center.getWorld() == null) continue;
            if (!b.center.getWorld().equals(pw)) continue;
            if (pl.distanceSquared(b.center) <= heatSq) return true;
        }
        return false;
    }

    /** Read-only snapshot of active bonfires. */
    public List<Bonfire> getActive() {
        return Collections.unmodifiableList(new ArrayList<>(active));
    }

    // ========================
    // Per-bonfire FX rendering
    // ========================

    /** Visual signature called every 4 ticks per bonfire. */
    private void renderBonfireFx(Bonfire b) {
        if (b.center == null || b.center.getWorld() == null) return;
        World w = b.center.getWorld();
        Location core = b.center.clone().add(0, 0.4, 0);
        double density = Math.max(0.1, config.getBonfireParticleDensity());

        // 8 FLAME particles in upward cone above the pit
        try {
            w.spawnParticle(Particle.FLAME, core,
                    (int) Math.round(8 * density), 0.3, 0.3, 0.3, 0.02);
        } catch (Throwable ignored) {}

        // 4 SOUL_FIRE_FLAME for that bluer accent
        try {
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, core,
                    (int) Math.round(4 * density), 0.25, 0.3, 0.25, 0.01);
        } catch (Throwable ignored) {}

        // 6 CRIT particles in core position
        try {
            w.spawnParticle(Particle.CRIT, core,
                    (int) Math.round(6 * density), 0.2, 0.1, 0.2, 0.0);
        } catch (Throwable ignored) {}

        // Vertical END_ROD beam for long-range visibility
        int beamHeight = Math.max(0, config.getBonfireBeamHeight());
        if (beamHeight > 0) {
            int beamPoints = 12;
            double step = beamHeight / (double) beamPoints;
            for (int i = 0; i < beamPoints; i++) {
                try {
                    Location pt = b.center.clone().add(0, i * step, 0);
                    w.spawnParticle(Particle.END_ROD, pt, 1, 0.02, 0.0, 0.02, 0.0);
                } catch (Throwable ignored) {}
            }
        }

        // Warm-orange DUST ring at the heat-radius perimeter — "this is the safe zone"
        double heatR = Math.max(0.1, config.getBonfireHeatRadius());
        try {
            Particle.DustOptions dust = new Particle.DustOptions(
                    Color.fromRGB(0xFF, 0xA0, 0x30), 1.4f);
            int ringPoints = 12;
            for (int i = 0; i < ringPoints; i++) {
                double a = (Math.PI * 2.0 * i) / ringPoints;
                Location pt = b.center.clone().add(
                        Math.cos(a) * heatR, 0.15, Math.sin(a) * heatR);
                w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, dust);
            }
        } catch (Throwable ignored) {}

        // Crackle every 16 ticks (4 fx-ticks)
        b.particleTickCounter += 4;
        if (b.particleTickCounter >= 16) {
            b.particleTickCounter = 0;
            try {
                float pitch = 0.8f + random.nextFloat() * 0.4f;
                w.playSound(b.center, Sound.BLOCK_CAMPFIRE_CRACKLE, 1.0f, pitch);
            } catch (Throwable ignored) {}
        }
    }

    // ========================
    // Cleanup
    // ========================

    private void cleanupBonfire(Bonfire b) {
        if (b == null) return;
        if (b.center != null && b.center.getWorld() != null) {
            try {
                b.center.getWorld().playSound(b.center, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.9f);
            } catch (Throwable ignored) {}
        }
        for (Entity e : b.displays) {
            if (e != null && e.isValid()) {
                try { e.remove(); } catch (Throwable ignored) {}
            }
        }
        b.displays.clear();
    }

    // ========================
    // Bonfire holder
    // ========================

    /** A single active bonfire instance — visual displays plus expiration tick. */
    public static class Bonfire {
        public Location center;
        public long expireAtTick;
        public final List<Entity> displays = new ArrayList<>();
        int particleTickCounter = 0;
    }
}
