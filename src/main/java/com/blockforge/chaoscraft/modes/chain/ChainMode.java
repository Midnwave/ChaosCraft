package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.chain.attacks.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

/**
 * Chain Mode — Chain-themed overworld survival mode.
 *
 * No boss, no escalation — pure random weighted attack survival.
 * Foundation phase: BD attack files (75 attacks across 8 files), ENV
 * attack files (75 attacks across 5 files), and a ModelEngine placeholder
 * (empty for now — user-provided ME specs come later).
 *
 * 8 BD attack categories:
 *   1-10  Pendulums / Wrecking balls
 *   11-20 Falling chains / Sky drops
 *   21-30 Ground eruptions
 *   31-40 Orbiting / Spinning (part 1)
 *   41-50 Orbiting / Projectile
 *   51-60 Projectile / Cages
 *   61-70 Signature attacks (part 1)
 *   71-75 Signature attacks (part 2)
 *
 * 5 ENV attack categories (15 each):
 *   1-15  Hanging ambient
 *   16-30 Atmospheric particles
 *   31-45 Falling/Drifting effects
 *   46-60 Structural ambience
 *   61-75 Combat feedback
 *
 * Mirrors FreezingIceMode structurally but strips the freezingice-specific
 * gimmicks (frostbite, bonfire network, ice physics) — chain gimmicks are
 * a later phase.
 */
public class ChainMode extends AbstractMode {

    private final ChainConfig chainConfig;
    private final AttackRegistry attackRegistry;
    private final ChainScheduler attackScheduler;

    private int tickCounter = 0;
    private int ambientSoundNextTick = 0;
    private final Random random = new Random();

    private int endTaskId = -1;
    private Location arenaCenter;

    // Gimmick subsystems
    private ChainAttackSystem chainAttackSystem;
    private ChainPlaceholders placeholders;

    public ChainMode(ChaosCraftPlugin plugin) {
        super(plugin, "chain");
        this.chainConfig = new ChainConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new ChainScheduler(plugin, attackRegistry, chainConfig);

        registerAllAttacks();
    }

    private void registerAllAttacks() {
        ChainBlockDisplay.registerAll(plugin, attackRegistry);
        ChainBlockDisplay2.registerAll(plugin, attackRegistry);
        ChainBlockDisplay3.registerAll(plugin, attackRegistry);
        ChainBlockDisplay4.registerAll(plugin, attackRegistry);
        ChainBlockDisplay5.registerAll(plugin, attackRegistry);
        ChainBlockDisplay6.registerAll(plugin, attackRegistry);
        ChainBlockDisplay7.registerAll(plugin, attackRegistry);
        ChainBlockDisplay8.registerAll(plugin, attackRegistry);
        ChainEnvironmental.registerAll(plugin, attackRegistry);
        ChainEnvironmental2.registerAll(plugin, attackRegistry);
        ChainEnvironmental3.registerAll(plugin, attackRegistry);
        ChainEnvironmental4.registerAll(plugin, attackRegistry);
        ChainEnvironmental5.registerAll(plugin, attackRegistry);
        ChainModelEngine.registerAll(plugin, attackRegistry);
        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[Chain] Registered " + attackRegistry.size()
                + " attacks, configs loaded.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;
        plugin.getLogger().info("[Chain] Mode starting — initializing systems...");

        World world = getChainWorld();
        if (world == null) {
            plugin.getLogger().severe("[Chain] Cannot start — no world found!");
            return;
        }
        plugin.getLogger().info("[Chain] Running in world: " + world.getName()
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
        plugin.getLogger().info("[Chain] " + bd + " BLOCK_DISPLAY, " + env + " ENVIRONMENTAL, "
                + me + " MODEL_ENGINE attacks registered (" + attackCount + " total).");

        // Start subsystems
        attackScheduler.start();

        // ── Gimmick: Chain Attack (mobs leash players) ─────────────────
        if (chainConfig.isGimmickEnabled() && chainConfig.isChainAttackEnabled()) {
            chainAttackSystem = new ChainAttackSystem(plugin, chainConfig);
            chainAttackSystem.start();

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    placeholders = new ChainPlaceholders(plugin);
                    placeholders.register();
                    plugin.getLogger().info("[Chain] Registered PlaceholderAPI expansion: chaoscraft_chain");
                } catch (Throwable t) {
                    plugin.getLogger().warning("[Chain] Failed to register PAPI expansion: " + t.getMessage());
                }
            } else {
                plugin.getLogger().info("[Chain] PlaceholderAPI not installed — skipping expansion registration.");
            }
        }

        // On-start commands
        for (String cmd : chainConfig.getOnStartCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().createSession("chain",
                    chainConfig.getMobSpawnConfig(), world);
        }

        // Schedule mode-end after duration
        long duration = (long) chainConfig.getDurationSeconds() * 20L;
        endTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getModeManager().endActiveMode();
        }, duration).getTaskId();

        // Initialize ambient sound timer
        ambientSoundNextTick = nextAmbientTick();

        plugin.getLogger().info("[Chain] Mode fully started. Duration: "
                + chainConfig.getDurationSeconds() + "s, arena radius: " + chainConfig.getArenaRadius());
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        // Ambient sounds
        if (chainConfig.isAmbientSoundsEnabled() && tickCounter >= ambientSoundNextTick) {
            playRandomAmbient();
            ambientSoundNextTick = tickCounter + nextAmbientInterval();
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().tick("chain", getExemptPlayers());
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Chain] Mode ending — cleaning up...");

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();

        // Stop gimmick subsystems
        if (chainAttackSystem != null) {
            try { chainAttackSystem.stop(); } catch (Throwable ignored) {}
            chainAttackSystem = null;
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
        for (String cmd : chainConfig.getOnEndCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }

        // Universal mob spawning cleanup
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().destroySession("chain");
        }

        tickCounter = 0;
        plugin.getLogger().info("[Chain] Mode ended. All systems cleaned up.");
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
        // No dimension-change handling for Chain mode.
    }

    // ========================
    // Helpers
    // ========================

    private int nextAmbientTick() {
        return tickCounter + nextAmbientInterval();
    }

    private int nextAmbientInterval() {
        int min = Math.max(1, chainConfig.getAmbientSoundMinInterval());
        int max = Math.max(min + 1, chainConfig.getAmbientSoundMaxInterval());
        return min + random.nextInt(max - min);
    }

    private void playRandomAmbient() {
        World world = getChainWorld();
        if (world == null) return;
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;
        List<String> sounds = chainConfig.getAmbientSounds();
        if (sounds == null || sounds.isEmpty()) return;
        Player p = players.get(random.nextInt(players.size()));
        String pick = sounds.get(random.nextInt(sounds.size()));
        try {
            Sound sound = Sound.valueOf(pick);
            p.playSound(p.getLocation(), sound, (float) chainConfig.getAmbientSoundVolume(),
                    0.8f + random.nextFloat() * 0.6f);
        } catch (IllegalArgumentException e) {
            // Try as namespaced custom sound key
            try {
                p.playSound(p.getLocation(), pick.toLowerCase(),
                        (float) chainConfig.getAmbientSoundVolume(), 1.0f);
            } catch (Throwable ignored) {}
        }
    }

    public World getChainWorld() {
        String configWorld = chainConfig.getWorldName();
        if (configWorld != null && !configWorld.isEmpty()) {
            World w = Bukkit.getWorld(configWorld);
            if (w != null) return w;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    // ========================
    // Accessors
    // ========================

    public ChainConfig getChainConfig() { return chainConfig; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public ChainScheduler getAttackScheduler() { return attackScheduler; }
    public int getTickCounter() { return tickCounter; }
    public Location getArenaCenter() { return arenaCenter; }
    public ChainAttackSystem getChainAttackSystem() { return chainAttackSystem; }
}
