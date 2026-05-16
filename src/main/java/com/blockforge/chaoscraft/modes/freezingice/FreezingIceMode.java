package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.freezingice.attacks.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

/**
 * FreezingIce Mode — Cold horror survival mode.
 *
 * No boss, no escalation — pure random weighted attack survival.
 * Currently a fresh-foundation Phase 1 build. Phase 2 will populate the
 * eight BlockDisplay attack files (75 attacks total) and the five
 * Environmental files (50 attacks total). ModelEngine attacks pending.
 *
 * Mirrors FluffyMode structurally but strips out the fluffy-specific
 * subsystems (world effects, rain-from-sky, custom mob AI) — those are
 * not part of the FreezingIce vision yet.
 */
public class FreezingIceMode extends AbstractMode {

    private final FreezingIceConfig iceConfig;
    private final AttackRegistry attackRegistry;
    private final FreezingIceScheduler attackScheduler;

    private int tickCounter = 0;
    private int ambientSoundNextTick = 0;
    private final Random random = new Random();

    private int endTaskId = -1;
    private Location arenaCenter;

    // Gimmick subsystems
    private FrostbiteTracker frostbiteTracker;
    private BonfireNetwork bonfireNetwork;
    private FreezingIcePlaceholders placeholders;

    public FreezingIceMode(ChaosCraftPlugin plugin) {
        super(plugin, "freezingice");
        this.iceConfig = new FreezingIceConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new FreezingIceScheduler(plugin, attackRegistry, iceConfig);

        registerAllAttacks();
    }

    private void registerAllAttacks() {
        FreezingIceBlockDisplay.registerAll(plugin, attackRegistry);
        FreezingIceBlockDisplay2.registerAll(plugin, attackRegistry);
        FreezingIceBlockDisplay3.registerAll(plugin, attackRegistry);
        FreezingIceBlockDisplay4.registerAll(plugin, attackRegistry);
        FreezingIceBlockDisplay5.registerAll(plugin, attackRegistry);
        FreezingIceBlockDisplay6.registerAll(plugin, attackRegistry);
        FreezingIceBlockDisplay7.registerAll(plugin, attackRegistry);
        FreezingIceBlockDisplay8.registerAll(plugin, attackRegistry);
        FreezingIceEnvironmental.registerAll(plugin, attackRegistry);
        FreezingIceEnvironmental2.registerAll(plugin, attackRegistry);
        FreezingIceEnvironmental3.registerAll(plugin, attackRegistry);
        FreezingIceEnvironmental4.registerAll(plugin, attackRegistry);
        FreezingIceEnvironmental5.registerAll(plugin, attackRegistry);
        FreezingIceModelEngine.registerAll(plugin, attackRegistry);
        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[FreezingIce] Registered " + attackRegistry.size()
                + " attacks, configs loaded.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;
        plugin.getLogger().info("[FreezingIce] Mode starting — initializing systems...");

        World world = getIceWorld();
        if (world == null) {
            plugin.getLogger().severe("[FreezingIce] Cannot start — no world found!");
            return;
        }
        plugin.getLogger().info("[FreezingIce] Running in world: " + world.getName()
                + " (" + world.getPlayers().size() + " players)");

        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }
        loadExemptPlayers();

        // Determine arena center — first online player, or world spawn
        if (!world.getPlayers().isEmpty()) {
            arenaCenter = world.getPlayers().get(0).getLocation().clone();
        } else {
            arenaCenter = world.getSpawnLocation().clone();
        }

        // Reload attack configs
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int env = attackRegistry.getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int me = attackRegistry.getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
        plugin.getLogger().info("[FreezingIce] " + bd + " BLOCK_DISPLAY, " + env + " ENVIRONMENTAL, "
                + me + " MODEL_ENGINE attacks registered (" + attackCount + " total).");

        // Start subsystems
        attackScheduler.start();

        // ── Gimmick: Frostbite + Bonfire Network ───────────────────────
        if (iceConfig.isGimmickEnabled()) {
            bonfireNetwork = new BonfireNetwork(plugin, this, iceConfig);
            bonfireNetwork.start();
            frostbiteTracker = new FrostbiteTracker(plugin, iceConfig, bonfireNetwork);
            frostbiteTracker.start();

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    placeholders = new FreezingIcePlaceholders(plugin);
                    placeholders.register();
                    plugin.getLogger().info("[FreezingIce] Registered PlaceholderAPI expansion: chaoscraft_freezingice");
                } catch (Throwable t) {
                    plugin.getLogger().warning("[FreezingIce] Failed to register PAPI expansion: " + t.getMessage());
                }
            } else {
                plugin.getLogger().info("[FreezingIce] PlaceholderAPI not installed — skipping expansion registration.");
            }

            // ── Gimmick: Global Ice Physics (block-friction override) ─
            if (iceConfig.isIcePhysicsEnabled()) {
                try {
                    FreezingIceFrictionOverride.apply(plugin,
                            (float) iceConfig.getIcePhysicsFriction(),
                            iceConfig.getIcePhysicsExemptBlocks());
                } catch (Throwable t) {
                    plugin.getLogger().warning("[FreezingIce] Ice-physics override failed: " + t.getMessage());
                }
            }
        }

        // On-start commands
        for (String cmd : iceConfig.getOnStartCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().createSession("freezingice",
                    iceConfig.getMobSpawnConfig(), world);
        }

        // Schedule mode-end after duration
        long duration = (long) iceConfig.getDurationSeconds() * 20L;
        endTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getModeManager().endActiveMode();
        }, duration).getTaskId();

        // Initialize ambient sound timer
        ambientSoundNextTick = nextAmbientTick();

        plugin.getLogger().info("[FreezingIce] Mode fully started. Duration: "
                + iceConfig.getDurationSeconds() + "s, arena radius: " + iceConfig.getArenaRadius());
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        // Ambient sounds
        if (iceConfig.isAmbientSoundsEnabled() && tickCounter >= ambientSoundNextTick) {
            playRandomAmbient();
            ambientSoundNextTick = tickCounter + nextAmbientInterval();
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().tick("freezingice", getExemptPlayers());
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[FreezingIce] Mode ending — cleaning up...");

        // Revert global friction override first — must happen even if other
        // subsystems are missing / null, so blocks never leak modifications.
        try { FreezingIceFrictionOverride.revert(plugin); } catch (Throwable ignored) {}

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();

        // Stop gimmick subsystems
        if (frostbiteTracker != null) {
            try { frostbiteTracker.stop(); } catch (Throwable ignored) {}
            frostbiteTracker = null;
        }
        if (bonfireNetwork != null) {
            try { bonfireNetwork.stop(); } catch (Throwable ignored) {}
            bonfireNetwork = null;
        }
        if (placeholders != null) {
            try { placeholders.unregister(); } catch (Throwable ignored) {}
            placeholders = null;
        }

        if (endTaskId != -1) {
            try { Bukkit.getScheduler().cancelTask(endTaskId); } catch (Throwable ignored) {}
            endTaskId = -1;
        }

        // On-end commands
        for (String cmd : iceConfig.getOnEndCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }

        // Universal mob spawning cleanup
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().destroySession("freezingice");
        }

        tickCounter = 0;
        plugin.getLogger().info("[FreezingIce] Mode ended. All systems cleaned up.");
    }

    @Override
    public void onPlayerJoin(Player player) {
        trackPlayer(player);
        plugin.getMusicManager().playForPlayer(player);
    }

    @Override
    public void onPlayerDeath(Player player) {
        markDeath(player);
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // No dimension-change handling for FreezingIce.
    }

    // ========================
    // Helpers
    // ========================

    private int nextAmbientTick() {
        return tickCounter + nextAmbientInterval();
    }

    private int nextAmbientInterval() {
        int min = Math.max(1, iceConfig.getAmbientSoundMinInterval());
        int max = Math.max(min + 1, iceConfig.getAmbientSoundMaxInterval());
        return min + random.nextInt(max - min);
    }

    private void playRandomAmbient() {
        World world = getIceWorld();
        if (world == null) return;
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;
        List<String> sounds = iceConfig.getAmbientSounds();
        if (sounds == null || sounds.isEmpty()) return;
        Player p = players.get(random.nextInt(players.size()));
        String pick = sounds.get(random.nextInt(sounds.size()));
        try {
            Sound sound = Sound.valueOf(pick);
            p.playSound(p.getLocation(), sound, (float) iceConfig.getAmbientSoundVolume(),
                    0.8f + random.nextFloat() * 0.6f);
        } catch (IllegalArgumentException e) {
            // Try as namespaced custom sound key
            try {
                p.playSound(p.getLocation(), pick.toLowerCase(),
                        (float) iceConfig.getAmbientSoundVolume(), 1.0f);
            } catch (Throwable ignored) {}
        }
    }

    public World getIceWorld() {
        String configWorld = iceConfig.getWorldName();
        if (configWorld != null && !configWorld.isEmpty()) {
            World w = Bukkit.getWorld(configWorld);
            if (w != null) return w;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    // ========================
    // Accessors
    // ========================

    public FreezingIceConfig getIceConfig() { return iceConfig; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public FreezingIceScheduler getAttackScheduler() { return attackScheduler; }
    public int getTickCounter() { return tickCounter; }
    public Location getArenaCenter() { return arenaCenter; }
    public FrostbiteTracker getFrostbiteTracker() { return frostbiteTracker; }
    public BonfireNetwork getBonfireNetwork() { return bonfireNetwork; }
}
