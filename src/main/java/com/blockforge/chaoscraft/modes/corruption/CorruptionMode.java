package com.blockforge.chaoscraft.modes.corruption;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.corruption.attacks.*;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Corrupted Corruption Mode — Event-driven environmental horror mode.
 *
 * Design:
 * - 48+ corruption-themed events across 8 categories
 * - Environmental corruption: floating blocks, block replacement spread, mob glitching
 * - Pure survival timer — survive X minutes (configurable, default 15 min) to win
 * - No bosses, no phases, no gem system
 * - All attacks are BLOCK_DISPLAY type (phase 1)
 * - Runs in the Overworld (configurable world)
 * - Block replacement engine corrupts terrain around players, restored on mode end
 * - Ambient horror: dark particles, corruption fog, ambient sounds
 * - Same attack framework as Calamity (AttackRegistry, AttackScheduler, AttackConfig)
 *
 * Attack Categories (8):
 * 1. Corruption Spread   — terrain-corrupting block displays
 * 2. Void Tendrils        — tentacle-like chain/block structures rising from ground
 * 3. Glitch Anomalies     — reality-warping visual distortion attacks
 * 4. Dark Eruptions       — ground-bursting corruption geysers
 * 5. Shadow Enclosures    — cage/trap structures that encase players
 * 6. Decay Rains          — falling corrupted block projectiles
 * 7. Corruption Storms    — spinning/rotational corruption vortexes
 * 8. Abyssal Constructs   — large overhead/sky corruption structures
 */
public class CorruptionMode extends AbstractMode {

    private final CorruptionConfig corruptionConfig;
    private final AttackRegistry attackRegistry;
    private final CorruptionAttackScheduler attackScheduler;
    private long tickCounter = 0;

    public CorruptionMode(ChaosCraftPlugin plugin) {
        super(plugin, "corruption");
        this.corruptionConfig = new CorruptionConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new CorruptionAttackScheduler(plugin, attackRegistry, corruptionConfig);
        registerAllAttacks();
    }

    // ========================
    // Attack Registration
    // ========================

    /**
     * Register all 120 corruption-themed attacks across 8 categories.
     * Each category class registers ~15 attacks as phase 1, BLOCK_DISPLAY type.
     */
    private void registerAllAttacks() {
        CorruptionSpread.registerAll(plugin, attackRegistry);
        VoidTendrils.registerAll(plugin, attackRegistry);
        GlitchAnomalies.registerAll(plugin, attackRegistry);
        DarkEruptions.registerAll(plugin, attackRegistry);
        ShadowEnclosures.registerAll(plugin, attackRegistry);
        DecayRains.registerAll(plugin, attackRegistry);
        CorruptionStorms.registerAll(plugin, attackRegistry);
        AbyssalConstructs.registerAll(plugin, attackRegistry);

        // Generate/load per-attack YAML config files
        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[Corruption] Registered " + attackRegistry.size() + " attacks, configs loaded.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;

        plugin.getLogger().info("[Corruption] Mode starting — initializing corruption systems...");

        World world = getCorruptionWorld();
        if (world == null) {
            plugin.getLogger().severe("[Corruption] Cannot start — configured world not found! "
                    + "Check corruption.yml world setting.");
            return;
        }

        int playerCount = world.getPlayers().size();
        plugin.getLogger().info("[Corruption] Running in world: " + world.getName()
                + " (" + playerCount + " players)");

        if (playerCount == 0) {
            plugin.getLogger().warning("[Corruption] No players in corruption world — events will not spawn until someone enters.");
        }

        // Track all players currently in the world
        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }

        // Load exempt players from config
        loadExemptPlayers();

        // Start timer
        int timerSeconds = corruptionConfig.getTimerSeconds();
        plugin.getModeTimer().start(timerSeconds);
        plugin.getModeTimer().setOnExpire(() -> {
            plugin.getLogger().info("[Corruption] Timer expired — mode complete! Players survived the corruption!");
            plugin.getModeManager().endActiveMode();
        });

        // Load attack configs and validate
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        if (attackCount == 0) {
            plugin.getLogger().severe("[Corruption] WARNING: 0 attacks registered! Check attack registration.");
        } else {
            int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
            plugin.getLogger().info("[Corruption] " + bd + " BLOCK_DISPLAY attacks registered.");
        }

        // Start music
        String musicId = corruptionConfig.getMusicSoundId();
        if (!musicId.isEmpty()) {
            plugin.getMusicManager().playModeMusic(this);
        }

        // Run start commands
        runStartCommands();

        // Start the attack scheduler
        attackScheduler.start();

        // Log corruption-specific config
        plugin.getLogger().info("[Corruption] Floating blocks: " + (corruptionConfig.isFloatingBlocksEnabled() ? "ON" : "OFF")
                + ", Block replacement: " + (corruptionConfig.isBlockReplacementEnabled() ? "ON" : "OFF")
                + ", Mob glitch: " + (corruptionConfig.isMobGlitchEnabled() ? "ON" : "OFF"));

        plugin.getLogger().info("[Corruption] Mode fully started. Survive " + timerSeconds + " seconds! "
                + "(" + attackCount + " attacks registered)");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();
        // TODO: corruptionEngine.tick() — engine handles floating blocks, block replacement,
        //       mob glitch, and ambient effects. Created separately.
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Corruption] Mode ending — cleaning up corruption...");

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();
        plugin.getModeTimer().stop();

        // TODO: corruptionEngine.stop() — stops floating blocks, mob glitch, ambient effects
        // TODO: corruptionEngine.startRestoration() — begins block restoration process

        // Run end commands
        runEndCommands();

        // Give rewards to survivors
        giveRewards();

        tickCounter = 0;
        plugin.getLogger().info("[Corruption] Mode ended. All corruption systems cleaned up.");
    }

    @Override
    public void onPlayerJoin(Player player) {
        trackPlayer(player);
        plugin.getMusicManager().playForPlayer(player);
    }

    @Override
    public void onPlayerDeath(Player player) {
        markDeath(player);
        plugin.debug("[Corruption] " + player.getName() + " died to corruption.");
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // Music replayed via MusicManager listener
    }

    // ========================
    // Corruption-specific API
    // ========================

    public CorruptionConfig getCorruptionConfig() {
        return corruptionConfig;
    }

    public AttackRegistry getAttackRegistry() {
        return attackRegistry;
    }

    public CorruptionAttackScheduler getAttackScheduler() {
        return attackScheduler;
    }

    public long getTickCounter() {
        return tickCounter;
    }

    /**
     * Get the Bukkit World where Corruption Mode is running.
     * Falls back to the first loaded world if the configured world name is empty or not found.
     */
    public World getCorruptionWorld() {
        String worldName = corruptionConfig.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) return world;
            plugin.getLogger().warning("[Corruption] Configured world '" + worldName + "' not found, falling back to default.");
        }
        // Default: first loaded world (Overworld)
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
