package com.blockforge.chaoscraft.modes.doom;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.doom.attacks.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Doom Mode — Lava Rise Survival
 *
 * Players are trapped in a defined arena while lava rises from below.
 * Survive the rising lava, block display attacks, environmental effects,
 * and ModelEngine VFX attacks until the timer expires.
 *
 * Core systems:
 * - Arena boundary enforcement (pos1/pos2 cuboid)
 * - Lagless rising lava (batched setType with physics disabled)
 * - Custom configurable lava damage
 * - Weighted multi-type attack scheduler (block display + environmental + model engine)
 * - MobSpawnService integration
 *
 * Arena setup: /cc function setdoommodepos1 + /cc function setdoommodepos2
 */
public class DoomMode extends AbstractMode {

    private final DoomConfig doomConfig;
    private final DoomArenaManager arenaManager;
    private final DoomLavaRise lavaRise;
    private final DoomDamageHandler damageHandler;
    private final AttackRegistry attackRegistry;
    private final DoomScheduler attackScheduler;

    private int tickCounter = 0;

    public DoomMode(ChaosCraftPlugin plugin) {
        super(plugin, "doom");
        this.doomConfig = new DoomConfig(plugin);
        this.arenaManager = new DoomArenaManager(plugin, doomConfig);
        this.lavaRise = new DoomLavaRise(plugin, doomConfig, arenaManager);
        this.damageHandler = new DoomDamageHandler(plugin, doomConfig);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new DoomScheduler(plugin, attackRegistry, doomConfig);

        // Register damage handler as listener
        plugin.getServer().getPluginManager().registerEvents(damageHandler, plugin);

        registerAllAttacks();
    }

    private void registerAllAttacks() {
        // Block Display attacks (52)
        DoomBlockDisplay.registerAll(plugin, attackRegistry);
        DoomBlockDisplay2.registerAll(plugin, attackRegistry);
        DoomBlockDisplay3.registerAll(plugin, attackRegistry);

        // Environmental attacks
        DoomEnvironmental.registerAll(plugin, attackRegistry);

        // ModelEngine VFX attacks (10)
        DoomModelEngine.registerAll(plugin, attackRegistry);

        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[Doom] Registered " + attackRegistry.size() + " attacks, configs loaded.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;
        plugin.getLogger().info("[Doom] Mode starting — initializing systems...");

        // Check arena
        arenaManager.loadFromConfig();
        if (!arenaManager.isConfigured()) {
            plugin.getLogger().severe("[Doom] Cannot start — arena not configured! "
                    + "Use /cc function setdoommodepos1 and /cc function setdoommodepos2 to define the arena.");
            return;
        }

        World world = getDoomWorld();
        if (world == null) {
            plugin.getLogger().severe("[Doom] Cannot start — arena world not found!");
            return;
        }

        int playerCount = world.getPlayers().size();
        plugin.getLogger().info("[Doom] Running in world: " + world.getName()
                + " (" + playerCount + " players)");

        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }
        loadExemptPlayers();

        // Reload attack configs
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int env = attackRegistry.getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int me = attackRegistry.getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
        plugin.getLogger().info("[Doom] " + bd + " BLOCK_DISPLAY, " + env + " ENVIRONMENTAL, "
                + me + " MODEL_ENGINE attacks registered (" + attackCount + " total).");

        // Start lava rise
        lavaRise.start();

        // Start damage handler
        damageHandler.start();

        // Start attack scheduler
        attackScheduler.start();

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().createSession("doom",
                    doomConfig.getMobSpawnConfig(), world);
        }

        plugin.getLogger().info("[Doom] Mode fully started. "
                + "Arena: (" + arenaManager.getMinX() + "," + arenaManager.getMinY() + "," + arenaManager.getMinZ()
                + ") to (" + arenaManager.getMaxX() + "," + arenaManager.getMaxY() + "," + arenaManager.getMaxZ() + ")"
                + " | Lava start Y=" + doomConfig.getLavaRiseStartY()
                + " | Rise every " + doomConfig.getRiseIntervalTicks() + " ticks");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();
        lavaRise.tick();
        damageHandler.tick();

        // Arena boundary enforcement
        World world = getDoomWorld();
        if (world != null && doomConfig.isEnforceBoundary()) {
            for (Player player : world.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL) continue;
                if (isExempt(player)) continue;
                arenaManager.enforceArenaBoundary(player);
            }
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().tick("doom", getExemptPlayers());
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Doom] Mode ending — cleaning up...");

        attackScheduler.stop();
        lavaRise.stop();
        lavaRise.cleanup();
        damageHandler.stop();
        plugin.getMusicManager().stopAll();

        // Universal mob spawning cleanup
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().destroySession("doom");
        }

        tickCounter = 0;
        plugin.getLogger().info("[Doom] Mode ended. All systems cleaned up.");
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
        // No dimension changes in Doom Mode
    }

    // ========================
    // Accessors
    // ========================

    public DoomConfig getDoomConfig() { return doomConfig; }
    public DoomArenaManager getArenaManager() { return arenaManager; }
    public DoomLavaRise getLavaRise() { return lavaRise; }
    public DoomScheduler getAttackScheduler() { return attackScheduler; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public int getTickCounter() { return tickCounter; }

    public World getDoomWorld() {
        String arenaWorld = arenaManager.getWorldName();
        if (arenaWorld != null && !arenaWorld.isEmpty()) {
            return Bukkit.getWorld(arenaWorld);
        }
        String configWorld = doomConfig.getWorldName();
        if (configWorld != null && !configWorld.isEmpty()) {
            return Bukkit.getWorld(configWorld);
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }
}
