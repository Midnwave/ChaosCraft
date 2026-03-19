package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.chain.attacks.*;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Chain Mode — Overworld survival mode with 115 chain-themed BlockDisplay attacks.
 *
 * Design:
 * - Pure survival timer — survive X minutes (configurable) to win
 * - No bosses, no phases, no gem system
 * - All attacks are BLOCK_DISPLAY type using 1.21.4 chains
 * - Runs in the Overworld (configurable world)
 * - Minimum 40 HP damage per hit (configurable per-attack)
 * - 10+ BlockDisplays per structure with full animations
 * - Same attack framework as Calamity (AttackRegistry, AttackScheduler, AttackConfig)
 */
public class ChainMode extends AbstractMode {

    private final ChainConfig chainConfig;
    private final AttackRegistry attackRegistry;
    private final ChainAttackScheduler attackScheduler;
    private final ChainMobManager mobManager;
    private long tickCounter = 0;

    public ChainMode(ChaosCraftPlugin plugin) {
        super(plugin, "chain");
        this.chainConfig = new ChainConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new ChainAttackScheduler(plugin, attackRegistry, chainConfig);
        this.mobManager = new ChainMobManager(plugin, chainConfig);
        registerAllAttacks();
    }

    // ========================
    // Attack Registration
    // ========================

    private void registerAllAttacks() {
        ChainRains.registerAll(plugin, attackRegistry);
        SwingingPendulum.registerAll(plugin, attackRegistry);
        EnclosuresCages.registerAll(plugin, attackRegistry);
        GroundEruptions.registerAll(plugin, attackRegistry);
        SpinningRotational.registerAll(plugin, attackRegistry);
        OverheadSky.registerAll(plugin, attackRegistry);
        WeaponsTools.registerAll(plugin, attackRegistry);
        EnvironmentalAmbient.registerAll(plugin, attackRegistry);

        // Generate/load per-attack YAML config files
        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[Chain] Registered " + attackRegistry.size() + " attacks, configs loaded.");
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
            plugin.getLogger().severe("[Chain] Cannot start — configured world not found! "
                    + "Check chain.yml world setting.");
            return;
        }

        int playerCount = world.getPlayers().size();
        plugin.getLogger().info("[Chain] Running in world: " + world.getName()
                + " (" + playerCount + " players)");

        if (playerCount == 0) {
            plugin.getLogger().warning("[Chain] No players in chain world — events will not spawn until someone enters.");
        }

        // Track all players currently in the world
        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }

        // Load exempt players from config
        loadExemptPlayers();

        // Start timer
        long timerSeconds = chainConfig.getDefaultTimerSeconds();
        plugin.getModeTimer().start((int) timerSeconds);
        plugin.getModeTimer().setOnExpire(() -> {
            plugin.getLogger().info("[Chain] Timer expired — mode complete! Players survived!");
            plugin.getModeManager().endActiveMode();
        });

        // Load attack configs and validate
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        if (attackCount == 0) {
            plugin.getLogger().severe("[Chain] WARNING: 0 attacks registered! Check attack registration.");
        } else {
            int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
            plugin.getLogger().info("[Chain] " + bd + " BLOCK_DISPLAY attacks registered.");
        }

        // Start music (reads from ChainConfig which shares the same yml file as ModeConfig)
        String musicId = chainConfig.getMusicSoundId();
        if (!musicId.isEmpty()) {
            plugin.getMusicManager().playModeMusic(this);
        }

        // NOTE: runStartCommands() is called by ModeManager — do NOT call it here to avoid double execution

        // Start the attack scheduler
        attackScheduler.start();

        plugin.getLogger().info("[Chain] Mode fully started. Survive " + timerSeconds + " seconds! "
                + "(" + attackCount + " attacks registered)");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        // Tick chain mob AI
        World world = getChainWorld();
        if (world != null) {
            mobManager.tick(world);
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Chain] Mode ending — cleaning up...");

        attackScheduler.stop();
        mobManager.cleanup();
        plugin.getMusicManager().stopAll();

        // NOTE: runEndCommands() and giveRewards() are called by ModeManager — do NOT call here

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
        plugin.debug("[Chain] " + player.getName() + " died.");
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // Music replayed via MusicManager listener
    }

    // ========================
    // Chain-specific API
    // ========================

    public ChainConfig getChainConfig() {
        return chainConfig;
    }

    public AttackRegistry getAttackRegistry() {
        return attackRegistry;
    }

    public ChainAttackScheduler getAttackScheduler() {
        return attackScheduler;
    }

    public long getTickCounter() {
        return tickCounter;
    }

    public World getChainWorld() {
        String worldName = chainConfig.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        // Default: first loaded world (Overworld)
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
