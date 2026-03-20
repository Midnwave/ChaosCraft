package com.blockforge.chaoscraft.modes.bluemoon;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay.*;
import com.blockforge.chaoscraft.modes.bluemoon.attacks.environmental.*;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Blue Moon Mode — Cosmic frost/moonlight horror survival with a floating boss.
 *
 * Design:
 * - Survival timer (default 15 min) OR kill the massive floating Blue Moon boss
 * - 104 BLOCK_DISPLAY + 104 ENVIRONMENTAL + 11 BOSS attacks = 219 total
 * - Boss: MythicMobs + ModelEngine massive floating moon (15-20 blocks)
 * - Forced nighttime in Overworld
 * - 52 configurable gimmick mechanics
 * - Lunar Super Laser: inescapable signature boss attack
 * - Theme: Silver, ice blue, frost cyan, deep indigo, moonlight
 */
public class BlueMoonMode extends AbstractMode {

    private final BlueMoonConfig moonConfig;
    private final AttackRegistry attackRegistry;
    private final BlueMoonScheduler attackScheduler;
    private long tickCounter = 0;
    private long savedTime = -1; // Original world time to restore on end

    public BlueMoonMode(ChaosCraftPlugin plugin) {
        super(plugin, "bluemoon");
        this.moonConfig = new BlueMoonConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new BlueMoonScheduler(plugin, attackRegistry, moonConfig);
        registerAllAttacks();
    }

    private void registerAllAttacks() {
        // Block Display attacks (8 categories × 13 = 104)
        CelestialStructures.registerAll(plugin, attackRegistry);
        TidalFormations.registerAll(plugin, attackRegistry);
        FrostIceConstructs.registerAll(plugin, attackRegistry);
        MoonbeamLight.registerAll(plugin, attackRegistry);
        WerewolfBeast.registerAll(plugin, attackRegistry);
        EclipseShadow.registerAll(plugin, attackRegistry);
        StarfallMeteors.registerAll(plugin, attackRegistry);
        LunarArchitecture.registerAll(plugin, attackRegistry);

        // Environmental attacks (8 categories × 13 = 104)
        TidalForces.registerAll(plugin, attackRegistry);
        MoonphaseEffects.registerAll(plugin, attackRegistry);
        FrostStorms.registerAll(plugin, attackRegistry);
        HowlingWinds.registerAll(plugin, attackRegistry);
        StarShowers.registerAll(plugin, attackRegistry);
        LunarQuakes.registerAll(plugin, attackRegistry);
        EclipseDarkness.registerAll(plugin, attackRegistry);
        CosmicRadiation.registerAll(plugin, attackRegistry);

        // Boss attacks (11) — TODO: implement
        // BlueMoonBossAttacks.registerAll(plugin, attackRegistry);

        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[BlueMoon] Registered " + attackRegistry.size() + " attacks, configs loaded.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;
        plugin.getLogger().info("[BlueMoon] Mode starting — initializing systems...");

        World world = getBlueMoonWorld();
        if (world == null) {
            plugin.getLogger().severe("[BlueMoon] Cannot start — configured world not found!");
            return;
        }

        int playerCount = world.getPlayers().size();
        plugin.getLogger().info("[BlueMoon] Running in world: " + world.getName()
                + " (" + playerCount + " players)");

        // Track all players
        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }
        loadExemptPlayers();

        // Force night
        if (moonConfig.isForceNight()) {
            savedTime = world.getTime();
            world.setTime(18000); // Midnight
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            plugin.getLogger().info("[BlueMoon] Forced nighttime (saved time: " + savedTime + ")");
        }

        // Start timer
        long timerSeconds = moonConfig.getDefaultTimerSeconds();
        plugin.getModeTimer().start((int) timerSeconds);
        plugin.getModeTimer().setOnExpire(() -> {
            plugin.getLogger().info("[BlueMoon] Timer expired — players survived the Blue Moon!");
            plugin.getModeManager().endActiveMode();
        });

        // Load attack configs
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int env = attackRegistry.getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int boss = attackRegistry.getByPhaseAndType(1, AttackType.BOSS).size();
        plugin.getLogger().info("[BlueMoon] " + bd + " BLOCK_DISPLAY, " + env + " ENVIRONMENTAL, "
                + boss + " BOSS attacks registered (" + attackCount + " total).");

        // Music
        String musicId = moonConfig.getMusicSoundId();
        if (!musicId.isEmpty()) {
            plugin.getMusicManager().playModeMusic(this);
        }

        // Start attack scheduler
        attackScheduler.start();

        // TODO: Start boss manager after spawn delay
        // TODO: Start gimmick manager

        plugin.getLogger().info("[BlueMoon] Mode fully started. Survive " + timerSeconds + " seconds or slay the moon! "
                + "(" + attackCount + " attacks registered)");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        // TODO: Tick boss manager
        // TODO: Tick gimmick manager
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[BlueMoon] Mode ending — cleaning up...");

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();

        // Restore world time
        if (moonConfig.isForceNight()) {
            World world = getBlueMoonWorld();
            if (world != null) {
                if (savedTime >= 0) {
                    world.setTime(savedTime);
                }
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                plugin.getLogger().info("[BlueMoon] Restored world time.");
            }
        }

        // TODO: Cleanup boss manager
        // TODO: Cleanup gimmick manager

        tickCounter = 0;
        plugin.getLogger().info("[BlueMoon] Mode ended. All systems cleaned up.");
    }

    @Override
    public void onPlayerJoin(Player player) {
        trackPlayer(player);
        plugin.getMusicManager().playForPlayer(player);
    }

    @Override
    public void onPlayerDeath(Player player) {
        markDeath(player);
        plugin.debug("[BlueMoon] " + player.getName() + " died.");
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // Music replayed via MusicManager listener
    }

    // ========================
    // Blue Moon API
    // ========================

    public BlueMoonConfig getMoonConfig() { return moonConfig; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public BlueMoonScheduler getAttackScheduler() { return attackScheduler; }
    public long getTickCounter() { return tickCounter; }

    public World getBlueMoonWorld() {
        String worldName = moonConfig.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
