package com.blockforge.chaoscraft.modes.doom;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom lava damage handler for Doom Mode.
 *
 * <p>Overrides vanilla lava damage with a configurable amount and interval.
 * Vanilla lava deals 4 hearts/sec — this lets server admins tune it for their
 * player base and difficulty level.
 *
 * <p>Listens to {@link EntityDamageEvent} with {@code DamageCause.LAVA},
 * cancels the vanilla damage, and applies the configured amount on a cooldown.
 */
public class DoomDamageHandler implements Listener {

    private final ChaosCraftPlugin plugin;
    private final DoomConfig config;

    private boolean active = false;
    // Listeners stay armed for 60 seconds AFTER mode stop, so any residual
    // lava-cooling events fired during cleanup still get cancelled + reverted.
    private long listenerUntilMillis = 0;
    private static final long GRACE_PERIOD_MILLIS = 60_000L;
    private final Map<UUID, Integer> lavaDamageCooldowns = new HashMap<>();

    public DoomDamageHandler(ChaosCraftPlugin plugin, DoomConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Activate the damage handler. Call on mode start.
     */
    public void start() {
        active = true;
        listenerUntilMillis = Long.MAX_VALUE;
        lavaDamageCooldowns.clear();
    }

    /**
     * Deactivate the damage handler. Lava-cancel listeners stay armed for
     * {@link #GRACE_PERIOD_MILLIS} after this call so they catch any late
     * block-form events during/after cleanup.
     */
    public void stop() {
        active = false;
        listenerUntilMillis = System.currentTimeMillis() + GRACE_PERIOD_MILLIS;
        lavaDamageCooldowns.clear();
    }

    /**
     * True while the mode is active OR we're within the post-stop grace period.
     * Lava-cancel listeners use this instead of {@code active} so they stay
     * armed during and right after cleanup.
     */
    private boolean listenerArmed() {
        return active || System.currentTimeMillis() < listenerUntilMillis;
    }

    /**
     * Returns true if the material is a "lava cooling product" block that
     * should never appear in a doom-arena where we want pure lava.
     */
    private static boolean isCoolingProduct(Material m) {
        return m == Material.STONE
                || m == Material.COBBLESTONE
                || m == Material.OBSIDIAN
                || m == Material.BASALT;
    }

    /**
     * Called every tick from DoomMode.onTick() to decrement cooldowns.
     */
    public void tick() {
        if (!active) return;
        lavaDamageCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!active) return;
        if (!config.isLavaDamageEnabled()) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.LAVA) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if player is exempt
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive()) {
            var activeMode = modeManager.getActiveMode();
            if (activeMode.isExempt(player)) {
                event.setCancelled(true);
                return;
            }
        }

        // Cancel vanilla lava damage
        event.setCancelled(true);

        // Check cooldown
        UUID uuid = player.getUniqueId();
        if (lavaDamageCooldowns.containsKey(uuid)) return;

        // Apply custom damage
        double damage = config.getLavaDamagePerTick();
        player.damage(damage);
        player.setNoDamageTicks(0); // Allow rapid damage from other sources

        // Set cooldown
        lavaDamageCooldowns.put(uuid, config.getLavaDamageIntervalTicks());
    }

    /**
     * Cancel lava→stone / lava→cobblestone / lava→obsidian formations that
     * happen when rising lava meets water. During Doom Mode the arena should
     * stay full of pure lava — solid floating stone/obsidian chunks break the
     * visual and let players stand safely.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (!listenerArmed()) return;
        if (isCoolingProduct(event.getNewState().getType())) {
            event.setCancelled(true);
        }
    }

    /**
     * Safety net — MONITOR runs after all cancels. If a cooling block somehow
     * still made it into the world (plugin bypass, world-gen, pre-existing
     * lava-water interaction at mode start), revert it to LAVA on the next tick.
     * No-ops if our HIGH-priority cancel already fired, since the block
     * wouldn't be the cooling material anymore.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockFormRevert(BlockFormEvent event) {
        if (!listenerArmed()) return;
        if (event.isCancelled()) return;
        if (!isCoolingProduct(event.getNewState().getType())) return;
        Block b = event.getBlock();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isCoolingProduct(b.getType())) {
                b.setType(Material.LAVA, false);
            }
        }, 1L);
    }

    /**
     * Cancel lava flowing (source-distance spread). The mode's own fill logic
     * places lava blocks directly where we want them — we don't need vanilla's
     * flow simulation propagating lava outside the arena bounds, which would
     * also cause surface FPS spikes as the flow front expanded block by block.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!listenerArmed()) return;
        Material src = event.getBlock().getType();
        if (src == Material.LAVA) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancel physics ticks on lava blocks so the fluid-flow rescheduling loop
     * stops entirely — when a neighbor changes, LiquidBlock.neighborChanged
     * normally re-schedules a fluid tick to try to spread again, so even with
     * BlockFromToEvent cancelled we'd still be burning CPU / packets on the
     * perpetual reschedule. Cancelling physics for lava stops that dead.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!listenerArmed()) return;
        if (event.getBlock().getType() == Material.LAVA) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancel lava "spread" events (BlockSpreadEvent covers some fluid edge
     * cases that BlockFromTo doesn't on Paper).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!listenerArmed()) return;
        if (event.getSource().getType() == Material.LAVA
                || event.getNewState().getType() == Material.LAVA) {
            event.setCancelled(true);
        }
    }
}
