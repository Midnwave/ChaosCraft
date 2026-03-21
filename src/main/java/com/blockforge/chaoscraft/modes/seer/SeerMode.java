package com.blockforge.chaoscraft.modes.seer;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Seer Mode — A boss fight where a giant flying eyeball fires beams at players.
 * Players must destroy 10 crying obsidian orbs around the arena to kill the boss.
 * Each orb destroyed reduces boss max HP by 10M (100M total).
 * Breaking an orb halves the breaker's current health.
 *
 * Design:
 * - Survival timer (default 20 min) OR destroy all 10 orbs to kill the boss
 * - Boss: MythicMobs + ModelEngine giant flying eyeball with beam attack
 * - Arena-based fight with crying obsidian orb mechanic
 * - Theme: Deep purple, violet, dark magenta, eldritch eye
 */
public class SeerMode extends AbstractMode {

    private final SeerConfig seerConfig;
    private final AttackRegistry attackRegistry;
    private final SeerScheduler attackScheduler;
    private final SeerBossManager bossManager;
    private final SeerOrbManager orbManager;
    private long tickCounter = 0;
    private int bossSpawnDelay = 0;
    private boolean bossSpawnScheduled = false;

    public SeerMode(ChaosCraftPlugin plugin) {
        super(plugin, "seer");
        this.seerConfig = new SeerConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new SeerScheduler(plugin, attackRegistry, seerConfig);
        this.bossManager = new SeerBossManager(plugin, seerConfig);
        this.orbManager = new SeerOrbManager(plugin, seerConfig);
        // Cross-reference: orb manager needs boss manager for health updates
        orbManager.setBossManager(bossManager);
        bossManager.setOrbManager(orbManager);
        registerAllAttacks();
    }

    private void registerAllAttacks() {
        // TODO: Block Display attacks (8 categories)
        // SeerBlockDisplayCategory1.registerAll(plugin, attackRegistry);
        // SeerBlockDisplayCategory2.registerAll(plugin, attackRegistry);
        // SeerBlockDisplayCategory3.registerAll(plugin, attackRegistry);
        // SeerBlockDisplayCategory4.registerAll(plugin, attackRegistry);

        // TODO: Environmental attacks (8 categories)
        // SeerEnvironmentalCategory1.registerAll(plugin, attackRegistry);
        // SeerEnvironmentalCategory2.registerAll(plugin, attackRegistry);
        // SeerEnvironmentalCategory3.registerAll(plugin, attackRegistry);
        // SeerEnvironmentalCategory4.registerAll(plugin, attackRegistry);

        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[Seer] Registered " + attackRegistry.size() + " attacks, configs loaded.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;
        plugin.getLogger().info("[Seer] Mode starting — initializing systems...");

        World world = getSeerWorld();
        if (world == null) {
            plugin.getLogger().severe("[Seer] Cannot start — configured world '" + seerConfig.getWorldName() + "' not found!");
            return;
        }

        int playerCount = world.getPlayers().size();
        plugin.getLogger().info("[Seer] Running in world: " + world.getName()
                + " (" + playerCount + " players)");

        // Track all players
        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }
        loadExemptPlayers();

        // Start timer
        long timerSeconds = seerConfig.getDefaultTimerSeconds();
        plugin.getModeTimer().start((int) timerSeconds);
        plugin.getModeTimer().setOnExpire(() -> {
            plugin.getLogger().info("[Seer] Timer expired — the Seer endures!");
            plugin.getModeManager().endActiveMode();
        });

        // Load attack configs
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int env = attackRegistry.getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        plugin.getLogger().info("[Seer] " + bd + " BLOCK_DISPLAY, " + env + " ENVIRONMENTAL attacks registered ("
                + attackCount + " total).");

        // Music
        String musicId = seerConfig.getMusicSoundId();
        if (!musicId.isEmpty()) {
            plugin.getMusicManager().playModeMusic(this);
        }

        // Start attack scheduler
        attackScheduler.start();

        // Place orbs in the arena
        orbManager.placeOrbs(world);
        plugin.getLogger().info("[Seer] Placed " + seerConfig.getOrbCount() + " crying obsidian orbs.");

        // Register orb listener
        Bukkit.getPluginManager().registerEvents(orbManager, plugin);

        // Schedule boss spawn after a short delay (100 ticks = 5 seconds)
        bossSpawnDelay = 100;
        bossSpawnScheduled = true;
        plugin.getLogger().info("[Seer] Boss will spawn in " + bossSpawnDelay + " ticks.");

        plugin.getLogger().info("[Seer] Mode fully started. Destroy all orbs to vanquish the Seer! "
                + "(" + attackCount + " attacks registered, timer: " + timerSeconds + "s)");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        World world = getSeerWorld();
        if (world == null) return;

        // Boss spawn delay
        if (bossSpawnScheduled) {
            bossSpawnDelay--;
            if (bossSpawnDelay <= 0) {
                bossSpawnScheduled = false;
                bossManager.spawnBoss(world);
            }
        }

        // Tick boss
        if (bossManager.isBossAlive()) {
            bossManager.tick(world);
        }

        // Tick orb effects
        orbManager.tick(world);
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Seer] Mode ending — cleaning up...");

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();

        // Cleanup boss + orbs
        bossManager.cleanup();
        orbManager.cleanup();

        tickCounter = 0;
        plugin.getLogger().info("[Seer] Mode ended. All systems cleaned up.");
    }

    @Override
    public void onPlayerJoin(Player player) {
        trackPlayer(player);
        plugin.getMusicManager().playForPlayer(player);
    }

    @Override
    public void onPlayerDeath(Player player) {
        markDeath(player);
        plugin.debug("[Seer] " + player.getName() + " died.");
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // Music replayed via MusicManager listener
    }

    // ========================
    // Seer API
    // ========================

    public SeerConfig getSeerConfig() { return seerConfig; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public SeerScheduler getAttackScheduler() { return attackScheduler; }
    public SeerBossManager getBossManager() { return bossManager; }
    public SeerOrbManager getOrbManager() { return orbManager; }
    public long getTickCounter() { return tickCounter; }

    public World getSeerWorld() {
        String worldName = seerConfig.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
