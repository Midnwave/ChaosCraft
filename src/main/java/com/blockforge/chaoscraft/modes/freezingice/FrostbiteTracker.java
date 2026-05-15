package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FreezingIce Frostbite tracker — per-player meter 0-100%.
 *
 * <p>Each player in the FreezingIce world has a frostbite percentage. The
 * percentage climbs over time when the player is NOT near a warmth source,
 * and drops when the player IS warm (near a {@link BonfireNetwork.Bonfire}
 * heat radius, OR on fire — {@code player.getFireTicks() > 0}).
 *
 * <p>Visual feedback is delivered exclusively through the vanilla
 * powder-snow freeze overlay via {@link Player#setFreezeTicks(int)}.
 * Per user direction, no titles, bossbars, or potion effects are used.
 * The {@code %chaoscraft_freezingice_frostbite%} placeholder exposes the
 * current integer value 0-100 (see {@link FreezingIcePlaceholders}).
 *
 * <p>Cold-rate curve: self-accelerating. The higher the current % already
 * is, the FASTER it climbs to the next %:
 * <pre>
 *   ticks_to_next = start_ticks - (start_ticks - end_ticks) * (current_pct / 99.0)
 * </pre>
 *
 * <p>At 100%, freeze damage starts ticking on a configurable interval. The
 * Paper 1.20.5+ {@code DamageSource} API is used reflectively to deal
 * {@code DamageType.FREEZE} damage (correct sound effect, correct death
 * message); falls back to plain {@code player.damage(double)} on older API.
 */
public class FrostbiteTracker implements Listener {

    private final ChaosCraftPlugin plugin;
    private final FreezingIceConfig config;
    private final BonfireNetwork bonfires;

    private final Map<UUID, Double> frostbitePct = new HashMap<>();
    private final Map<UUID, Integer> damageTickCounter = new HashMap<>();

    private BukkitTask task;
    private boolean listenerRegistered = false;

    public FrostbiteTracker(ChaosCraftPlugin plugin, FreezingIceConfig config, BonfireNetwork bonfires) {
        this.plugin = plugin;
        this.config = config;
        this.bonfires = bonfires;
    }

    // ========================
    // Lifecycle
    // ========================

    public void start() {
        if (task != null) return;
        if (!listenerRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        plugin.getLogger().info("[FreezingIce] FrostbiteTracker started.");
    }

    public void stop() {
        if (task != null) {
            try { task.cancel(); } catch (Throwable ignored) {}
            task = null;
        }
        // Clear freeze overlay for every tracked player
        for (UUID id : frostbitePct.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                try { p.setFreezeTicks(0); } catch (Throwable ignored) {}
            }
        }
        frostbitePct.clear();
        damageTickCounter.clear();
        // Note: don't unregister listener — Bukkit will clean it up when plugin disables.
        plugin.getLogger().info("[FreezingIce] FrostbiteTracker stopped.");
    }

    // ========================
    // Tick loop
    // ========================

    private void tick() {
        FreezingIceMode mode = getMode();
        if (mode == null) return;
        World world = mode.getIceWorld();
        if (world == null) return;

        double coldStart = Math.max(1, config.getFrostbiteColdStartTicksPerPercent());
        double coldEnd = Math.max(1, config.getFrostbiteColdEndTicksPerPercent());
        double warmTicks = Math.max(1, config.getWarmthRecoveryTicksPerPercent());
        int damageInterval = Math.max(1, config.getFrostbiteDamageIntervalTicks());

        for (Player player : world.getPlayers()) {
            if (player == null || !player.isOnline()) continue;
            if (mode.isExempt(player)) {
                // Clear any residual state for exempt players
                if (frostbitePct.containsKey(player.getUniqueId())) {
                    frostbitePct.remove(player.getUniqueId());
                    damageTickCounter.remove(player.getUniqueId());
                    try { player.setFreezeTicks(0); } catch (Throwable ignored) {}
                }
                continue;
            }

            boolean warm = (bonfires != null && bonfires.isPlayerNearBonfire(player))
                    || player.getFireTicks() > 0;

            double p = frostbitePct.getOrDefault(player.getUniqueId(), 0.0);

            double delta;
            if (warm) {
                delta = -(1.0 / warmTicks);
            } else {
                // Self-accelerating cold curve
                double ticks = coldStart - (coldStart - coldEnd) * (p / 99.0);
                if (ticks < 1.0) ticks = 1.0;
                delta = 1.0 / ticks;
            }

            p += delta;
            if (p < 0.0) p = 0.0;
            if (p > 100.0) p = 100.0;
            frostbitePct.put(player.getUniqueId(), p);

            // Update vanilla freeze overlay
            try {
                int maxFt = player.getMaxFreezeTicks();
                int ft = (int) Math.round((p / 100.0) * maxFt);
                player.setFreezeTicks(ft);
            } catch (Throwable ignored) {}

            // Damage at 100%
            if (p >= 100.0) {
                int c = damageTickCounter.getOrDefault(player.getUniqueId(), 0) + 1;
                if (c >= damageInterval) {
                    applyFreezeDamage(player);
                    c = 0;
                }
                damageTickCounter.put(player.getUniqueId(), c);
            } else {
                if (damageTickCounter.getOrDefault(player.getUniqueId(), 0) != 0) {
                    damageTickCounter.put(player.getUniqueId(), 0);
                }
            }
        }
    }

    /**
     * Deal frostbite damage. Tries the Paper 1.20.5+ DamageSource/DamageType.FREEZE
     * API reflectively so we get the correct sound and death message. Falls back
     * to a plain {@link Player#damage(double)} call on older API.
     */
    private void applyFreezeDamage(Player player) {
        double dmg = config.getFrostbiteDamageAt100();
        boolean ignoresArmor = config.isFrostbiteDamageIgnoresArmor();
        try {
            Class<?> dsBuilder = Class.forName("org.bukkit.damage.DamageSource");
            Class<?> dt = Class.forName("org.bukkit.damage.DamageType");
            Object freezeType = dt.getField("FREEZE").get(null);
            Object builder = dsBuilder.getMethod("builder", dt).invoke(null, freezeType);
            Object source = builder.getClass().getMethod("build").invoke(builder);

            if (ignoresArmor) {
                // Apply a near-zero damage event (plays sound + tags damage type) then
                // adjust health directly to bypass armor / enchantment reductions.
                player.getClass().getMethod("damage", double.class, dsBuilder).invoke(player, 0.001, source);
                double newHp = Math.max(0.0, player.getHealth() - dmg);
                player.setHealth(newHp);
            } else {
                player.getClass().getMethod("damage", double.class, dsBuilder).invoke(player, dmg, source);
            }
        } catch (Throwable ignored) {
            // Fallback for older Paper builds without DamageSource builder API
            try { player.damage(dmg); } catch (Throwable ignored2) {}
        }
    }

    // ========================
    // Public API
    // ========================

    /** Returns the player's current frostbite as an integer 0-100. */
    public int getFrostbite(Player p) {
        if (p == null) return 0;
        return (int) Math.floor(frostbitePct.getOrDefault(p.getUniqueId(), 0.0));
    }

    /** Set a player's frostbite directly (clamped 0-100). For admin commands. */
    public void setFrostbite(Player p, int pct) {
        if (p == null) return;
        double clamped = Math.max(0.0, Math.min(100.0, pct));
        frostbitePct.put(p.getUniqueId(), clamped);
        damageTickCounter.put(p.getUniqueId(), 0);
        try {
            int maxFt = p.getMaxFreezeTicks();
            p.setFreezeTicks((int) Math.round((clamped / 100.0) * maxFt));
        } catch (Throwable ignored) {}
    }

    /** Reset a player's frostbite state (also clears the freeze overlay). */
    public void reset(Player p) {
        if (p == null) return;
        frostbitePct.remove(p.getUniqueId());
        damageTickCounter.remove(p.getUniqueId());
        try { p.setFreezeTicks(0); } catch (Throwable ignored) {}
    }

    // ========================
    // Listeners
    // ========================

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        reset(event.getEntity());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer());
    }

    // ========================
    // Helpers
    // ========================

    private FreezingIceMode getMode() {
        var m = plugin.getModeManager().getMode("freezingice");
        return m instanceof FreezingIceMode fi ? fi : null;
    }
}
