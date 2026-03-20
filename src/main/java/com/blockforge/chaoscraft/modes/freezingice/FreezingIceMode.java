package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.freezingice.attacks.*;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Freezing Ice Mode — An ancient frozen entity beneath the world is waking up.
 * The cold is ALIVE, aggressive, and predatory.
 *
 * Features:
 * - 208 unique ice-themed attacks (104 block display + 104 environmental)
 * - Temperature tracker (players get colder over time, affecting speed/damage)
 * - Powder snow freeze mechanic (setFreezeTicks for visual frost overlay)
 * - Heat source interaction (torches, campfires restore warmth)
 * - Living ice creatures, glacial structures, frost weapons, avalanches
 */
public class FreezingIceMode extends AbstractMode {

    private final FreezingIceConfig iceConfig;
    private final AttackRegistry attackRegistry;
    private final FreezingIceScheduler attackScheduler;
    private final TemperatureTracker temperatureTracker;
    private long tickCounter = 0;

    public FreezingIceMode(ChaosCraftPlugin plugin) {
        super(plugin, "freezingice");
        this.iceConfig = new FreezingIceConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new FreezingIceScheduler(plugin, attackRegistry, iceConfig);
        this.temperatureTracker = new TemperatureTracker(plugin, iceConfig);
        registerAllAttacks();
    }

    // ========================
    // Attack Registration
    // ========================

    private void registerAllAttacks() {
        // Block Display attacks (104)
        LivingIce.registerAll(plugin, attackRegistry);
        GlacialStructures.registerAll(plugin, attackRegistry);
        FrostWeaponry.registerAll(plugin, attackRegistry);
        AvalancheSlides.registerAll(plugin, attackRegistry);
        CrystallineTraps.registerAll(plugin, attackRegistry);
        BlizzardProjectiles.registerAll(plugin, attackRegistry);
        PermafrostEruptions.registerAll(plugin, attackRegistry);
        FrozenArchitecture.registerAll(plugin, attackRegistry);

        // Environmental attacks (104)
        TemperatureDrop.registerAll(plugin, attackRegistry);
        BlizzardWeather.registerAll(plugin, attackRegistry);
        FrostCreepEffects.registerAll(plugin, attackRegistry);
        IceQuakes.registerAll(plugin, attackRegistry);
        FrozenMobEffects.registerAll(plugin, attackRegistry);
        WhiteoutEvents.registerAll(plugin, attackRegistry);
        HypothermiaPulse.registerAll(plugin, attackRegistry);
        ThawAndRefreeze.registerAll(plugin, attackRegistry);

        // ModelEngine VFX attacks (25)
        IceModelEngine.registerAll(plugin, attackRegistry);

        // Generate/load per-attack YAML config files
        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[FreezingIce] Registered " + attackRegistry.size() + " attacks, configs loaded.");
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
            plugin.getLogger().severe("[FreezingIce] Cannot start — configured world not found!");
            return;
        }

        int playerCount = world.getPlayers().size();
        plugin.getLogger().info("[FreezingIce] Running in world: " + world.getName()
                + " (" + playerCount + " players)");

        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }

        loadExemptPlayers();

        // Start timer
        long timerSeconds = iceConfig.getDefaultTimerSeconds();
        plugin.getModeTimer().start((int) timerSeconds);
        plugin.getModeTimer().setOnExpire(() -> {
            plugin.getLogger().info("[FreezingIce] Timer expired — players survived the cold!");
            plugin.getModeManager().endActiveMode();
        });

        // Reload configs
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        plugin.getLogger().info("[FreezingIce] " + bd + " BLOCK_DISPLAY attacks registered. " + attackCount + " total.");

        // Music
        String musicId = iceConfig.getMusicSoundId();
        if (!musicId.isEmpty()) {
            plugin.getMusicManager().playModeMusic(this);
        }

        // Start schedulers
        attackScheduler.start();

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().createSession("freezingice",
                    iceConfig.getMobSpawnConfig(), world);
        }

        plugin.getLogger().info("[FreezingIce] Mode fully started. Survive " + timerSeconds + " seconds!"
                + " (" + attackCount + " attacks)");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        // Temperature tracking for all players in the world
        World world = getIceWorld();
        if (world != null) {
            temperatureTracker.tick(world.getPlayers());
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().tick("freezingice", getExemptPlayers());
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[FreezingIce] Mode ending — cleaning up...");

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();

        // Reset temperature and freeze effects for all players
        World world = getIceWorld();
        if (world != null) {
            temperatureTracker.resetAll(world.getPlayers());
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
        temperatureTracker.resetPlayer(player.getUniqueId());
        plugin.debug("[FreezingIce] " + player.getName() + " died.");
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // Music replayed via MusicManager listener
    }

    // ========================
    // Freezing Ice API
    // ========================

    public FreezingIceConfig getIceConfig() { return iceConfig; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public FreezingIceScheduler getAttackScheduler() { return attackScheduler; }
    public TemperatureTracker getTemperatureTracker() { return temperatureTracker; }
    public long getTickCounter() { return tickCounter; }

    public World getIceWorld() {
        String worldName = iceConfig.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
