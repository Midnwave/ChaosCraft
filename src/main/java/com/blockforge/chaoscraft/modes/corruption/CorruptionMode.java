package com.blockforge.chaoscraft.modes.corruption;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.corruption.attacks.*;
import com.blockforge.chaoscraft.modes.corruption.engine.CorruptionEngine;
import com.blockforge.chaoscraft.modes.corruption.environmental.AmbientEffects;
import com.blockforge.chaoscraft.modes.corruption.environmental.MobGlitchHandler;
import com.blockforge.chaoscraft.modes.corruption.environmental.WorldCorruptor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Corrupted Corruption Mode — Event-driven environmental horror mode.
 *
 * Design:
 * - 190 corruption-themed attacks (120 block display, 40 environmental, 30 ModelEngine)
 * - Environmental corruption: floating blocks, block replacement spread, mob glitching
 * - Pure survival timer — survive X minutes (configurable, default 15 min) to win
 * - No bosses, no phases, no gem system
 * - Attacks: BLOCK_DISPLAY (45%), ENVIRONMENTAL (35%), MODEL_ENGINE (20%)
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
    private CorruptionEngine corruptionEngine;
    private AmbientEffects ambientEffects;
    private MobGlitchHandler mobGlitchHandler;
    private WorldCorruptor worldCorruptor;
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
     * Register all 190 corruption-themed attacks across 10 categories.
     * 120 BLOCK_DISPLAY (8 categories × 15), 40 ENVIRONMENTAL, 30 MODEL_ENGINE.
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

        // Environmental attacks (40)
        CorruptionEnvironmental.registerAll(plugin, attackRegistry);

        // ModelEngine VFX attacks (30)
        CorruptionModelEngine.registerAll(plugin, attackRegistry);

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

        // Load attack configs and validate
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        if (attackCount == 0) {
            plugin.getLogger().severe("[Corruption] WARNING: 0 attacks registered! Check attack registration.");
        } else {
            int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
            int env = attackRegistry.getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
            int me = attackRegistry.getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
            plugin.getLogger().info("[Corruption] " + bd + " BLOCK_DISPLAY, " + env + " ENVIRONMENTAL, " + me + " MODEL_ENGINE attacks registered.");
        }

        // NOTE: runStartCommands() is called by ModeManager — do NOT call here

        // Start the attack scheduler
        attackScheduler.start();

        // Log corruption-specific config
        plugin.getLogger().info("[Corruption] Floating blocks: " + (corruptionConfig.isFloatingBlocksEnabled() ? "ON" : "OFF")
                + ", Block replacement: " + (corruptionConfig.isBlockReplacementEnabled() ? "ON" : "OFF")
                + ", Mob glitch: " + (corruptionConfig.isMobGlitchEnabled() ? "ON" : "OFF"));

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().createSession("corruption",
                    corruptionConfig.getMobSpawnConfig(), world);
        }

        // Start corruption engine (floating blocks, proximity damage)
        if (corruptionConfig.isFloatingBlocksEnabled()) {
            this.corruptionEngine = new CorruptionEngine(plugin, corruptionConfig);
            corruptionEngine.start(world);
        }

        // Start ambient effects (particles, fog, sounds)
        if (corruptionConfig.isDarkParticlesEnabled()
                || corruptionConfig.isCorruptionFogEnabled()
                || corruptionConfig.isAmbientSoundsEnabled()) {
            this.ambientEffects = new AmbientEffects(plugin);
            int ambCx = world.getSpawnLocation().getBlockX();
            int ambCz = world.getSpawnLocation().getBlockZ();
            ambientEffects.start(world, ambCx, ambCz,
                    corruptionConfig.getMobGlitchRadiusChunks(),
                    corruptionConfig.isDarkParticlesEnabled(),
                    corruptionConfig.isCorruptionFogEnabled(),
                    corruptionConfig.isAmbientSoundsEnabled(),
                    corruptionConfig.getAmbientSoundIntervalTicks());
        }

        // Start mob glitch handler
        if (corruptionConfig.isMobGlitchEnabled()) {
            this.mobGlitchHandler = new MobGlitchHandler(plugin);
            int glitchCx = world.getSpawnLocation().getBlockX();
            int glitchCz = world.getSpawnLocation().getBlockZ();
            if (!world.getPlayers().isEmpty()) {
                Location pLoc = world.getPlayers().get(0).getLocation();
                glitchCx = pLoc.getBlockX();
                glitchCz = pLoc.getBlockZ();
            }
            mobGlitchHandler.start(world, glitchCx, glitchCz,
                    corruptionConfig.getMobGlitchRadiusChunks(),
                    corruptionConfig.getMobGlitchIntensity(),
                    corruptionConfig.isMobGlitchHostileOnly());
        }

        // Start world corruptor (block replacement spread)
        if (corruptionConfig.isBlockReplacementEnabled()) {
            this.worldCorruptor = new WorldCorruptor(plugin,
                    corruptionEngine != null ? corruptionEngine.getRestorer()
                            : new com.blockforge.chaoscraft.modes.corruption.engine.BlockRestorer(plugin));
            int wcCx = world.getSpawnLocation().getBlockX();
            int wcCz = world.getSpawnLocation().getBlockZ();
            worldCorruptor.start(world, wcCx, wcCz,
                    corruptionConfig.getBlockReplacementMaxRadiusChunks(),
                    corruptionConfig.getBlockReplacementBlocksPerTick(),
                    corruptionConfig.getBlockReplacementVanillaBlocks(),
                    corruptionConfig.getBlockReplacementItemsAdderBlocks(),
                    corruptionConfig.getBlockReplacementCraftEngineBlocks());
        }

        plugin.getLogger().info("[Corruption] Mode fully started. "
                + "(" + attackCount + " attacks registered)");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        // Tick the corruption engine (floating blocks, proximity damage, ambient particles)
        if (corruptionEngine != null) {
            corruptionEngine.tick();
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().tick("corruption", getExemptPlayers());
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Corruption] Mode ending — cleaning up corruption...");

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();
        plugin.getModeTimer().stop();

        // Universal mob spawning cleanup
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().destroySession("corruption");
        }

        // Stop corruption engine + begin block restoration
        if (corruptionEngine != null) {
            corruptionEngine.stop();
            corruptionEngine = null;
        }

        // Stop ambient effects
        if (ambientEffects != null) {
            ambientEffects.stop();
            ambientEffects = null;
        }

        // Stop mob glitch handler
        if (mobGlitchHandler != null) {
            mobGlitchHandler.stop();
            mobGlitchHandler = null;
        }

        // Stop world corruptor
        if (worldCorruptor != null) {
            worldCorruptor.stop();
            worldCorruptor = null;
        }

        // NOTE: runEndCommands() and giveRewards() are called by ModeManager — do NOT call here

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
