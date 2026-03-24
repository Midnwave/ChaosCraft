package com.blockforge.chaoscraft.modes.devilsdream;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.devilsdream.attacks.*;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Devil's Dream Mode — Lucid Nightmare survival.
 *
 * The dream reacts to player behavior through the Dream Adaptation system.
 * 208 total attacks: 104 BlockDisplay + 104 Environmental.
 * First mode with MythicMobs integration for nightmare creature spawning.
 *
 * Unique mechanics:
 * - Dream Adaptation: tracks sprinting, mining, building, fighting, sneaking,
 *   jumping, standing still, and looking around. Escalates targeted nightmare responses.
 * - MythicMobs: spawns nightmare creatures whose type matches the player's dominant behavior.
 * - Dual attack types: both block display structures AND environmental particle effects.
 *
 * Theme: Surreal nightmare, demonic, psychological horror, hellfire, dream logic.
 * Materials: Nether bricks, crimson wood, magma, blackstone, obsidian, soul sand, bone, sculk.
 * Distinct from: Chain (metallic), Corruption (void/glitch), Calamity (cosmic/dimensional).
 */
public class DevilsDreamMode extends AbstractMode implements Listener {

    private final DevilsDreamConfig dreamConfig;
    private final AttackRegistry attackRegistry;
    private final DevilsDreamScheduler attackScheduler;
    private final DreamAdaptationTracker adaptationTracker;
    private final DreamMobSpawner mobSpawner;
    private long tickCounter = 0;

    public DevilsDreamMode(ChaosCraftPlugin plugin) {
        super(plugin, "devilsdream");
        this.dreamConfig = new DevilsDreamConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.adaptationTracker = new DreamAdaptationTracker(plugin, dreamConfig);
        this.attackScheduler = new DevilsDreamScheduler(plugin, attackRegistry, dreamConfig, adaptationTracker);
        this.mobSpawner = new DreamMobSpawner(plugin, dreamConfig, adaptationTracker);
        registerAllAttacks();
    }

    // ========================
    // Attack Registration
    // ========================

    private void registerAllAttacks() {
        // Block Display attacks (8 categories x 13 = 104)
        NightmareConstructs.registerAll(plugin, attackRegistry);
        HellfireFormations.registerAll(plugin, attackRegistry);
        ShadowBeasts.registerAll(plugin, attackRegistry);
        DreamDistortions.registerAll(plugin, attackRegistry);
        DevilsArsenal.registerAll(plugin, attackRegistry);
        FallingNightmares.registerAll(plugin, attackRegistry);
        GroundTerrors.registerAll(plugin, attackRegistry);
        EtherealHauntings.registerAll(plugin, attackRegistry);

        // Environmental attacks (8 categories x 13 = 104)
        DreamShifts.registerAll(plugin, attackRegistry);
        NightmareWeather.registerAll(plugin, attackRegistry);
        FloorHazards.registerAll(plugin, attackRegistry);
        AdaptationResponses.registerAll(plugin, attackRegistry);
        AmbientDread.registerAll(plugin, attackRegistry);
        SoulHarvesting.registerAll(plugin, attackRegistry);
        HellscapeSurges.registerAll(plugin, attackRegistry);
        VoidIntrusions.registerAll(plugin, attackRegistry);

        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[DevilsDream] Registered " + attackRegistry.size() + " attacks, configs loaded.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;

        plugin.getLogger().info("[DevilsDream] Mode starting — the nightmare begins...");

        World world = getDreamWorld();
        if (world == null) {
            plugin.getLogger().severe("[DevilsDream] Cannot start — configured world not found!");
            return;
        }

        int playerCount = world.getPlayers().size();
        plugin.getLogger().info("[DevilsDream] Running in world: " + world.getName()
                + " (" + playerCount + " players)");

        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }

        loadExemptPlayers();

        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int env = attackRegistry.getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        plugin.getLogger().info("[DevilsDream] " + bd + " BLOCK_DISPLAY + " + env + " ENVIRONMENTAL = "
                + attackCount + " total attacks.");

        // NOTE: runStartCommands() is called by ModeManager — do NOT call here

        // Register event listener for adaptation tracking
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start systems
        adaptationTracker.resetAll();
        attackScheduler.start();

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().createSession("devilsdream",
                    dreamConfig.getMobSpawnConfig(), world);
        }

        plugin.getLogger().info("[DevilsDream] Mode fully started. "
                + "Dream Adaptation active. MythicMobs: " + (mobSpawner.isMythicMobsAvailable() ? "ENABLED" : "DISABLED"));
    }

    @Override
    public void onTick() {
        tickCounter++;

        World world = getDreamWorld();
        if (world == null) return;

        // Tick adaptation tracker — analyzes all player behavior
        adaptationTracker.tick(world.getPlayers());

        // Tick attack scheduler
        attackScheduler.tick();

        // Tick mob spawner
        mobSpawner.tick(world);

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().tick("devilsdream", getExemptPlayers());
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[DevilsDream] Mode ending — waking up...");

        attackScheduler.stop();
        mobSpawner.cleanup();
        adaptationTracker.resetAll();
        plugin.getMusicManager().stopAll();
        plugin.getModeTimer().stop();

        // Universal mob spawning cleanup
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().destroySession("devilsdream");
        }

        // Unregister event listeners
        BlockBreakEvent.getHandlerList().unregister(this);
        BlockPlaceEvent.getHandlerList().unregister(this);
        EntityDamageByEntityEvent.getHandlerList().unregister(this);

        // NOTE: runEndCommands() and giveRewards() are called by ModeManager — do NOT call here

        tickCounter = 0;
        plugin.getLogger().info("[DevilsDream] Mode ended. The nightmare is over.");
    }

    @Override
    public void onPlayerJoin(Player player) {
        trackPlayer(player);
        plugin.getMusicManager().playForPlayer(player);
    }

    @Override
    public void onPlayerDeath(Player player) {
        markDeath(player);
        plugin.debug("[DevilsDream] " + player.getName() + " perished in the nightmare.");
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // Music replayed via MusicManager listener
    }

    // ========================
    // Adaptation Event Listeners
    // ========================

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isDreamActive()) return;
        adaptationTracker.recordAction(event.getPlayer().getUniqueId(), DreamAction.DESTRUCTION);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isDreamActive()) return;
        adaptationTracker.recordAction(event.getPlayer().getUniqueId(), DreamAction.CREATION);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isDreamActive()) return;
        if (event.getDamager() instanceof Player player) {
            adaptationTracker.recordAction(player.getUniqueId(), DreamAction.AGGRESSION);
        }
    }

    private boolean isDreamActive() {
        return plugin.getModeManager().isAnyModeActive()
                && plugin.getModeManager().getActiveMode() == this;
    }

    // ========================
    // Devil's Dream API
    // ========================

    public DevilsDreamConfig getDreamConfig() { return dreamConfig; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public DevilsDreamScheduler getAttackScheduler() { return attackScheduler; }
    public DreamAdaptationTracker getAdaptationTracker() { return adaptationTracker; }
    public DreamMobSpawner getMobSpawner() { return mobSpawner; }
    public long getTickCounter() { return tickCounter; }

    public World getDreamWorld() {
        String worldName = dreamConfig.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
