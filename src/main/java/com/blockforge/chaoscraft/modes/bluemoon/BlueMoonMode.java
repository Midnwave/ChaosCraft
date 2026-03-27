package com.blockforge.chaoscraft.modes.bluemoon;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay.*;
import com.blockforge.chaoscraft.modes.bluemoon.attacks.environmental.*;
import com.blockforge.chaoscraft.modes.bluemoon.attacks.boss.BlueMoonBossAttacks;
import com.blockforge.chaoscraft.modes.bluemoon.attacks.BlueMoonModelEngine;
import org.bukkit.Bukkit;
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
    private final BlueMoonBossManager bossManager;
    private final LunarGimmickManager gimmickManager;
    private final BlueMoonSkyEffect skyEffect;
    private long tickCounter = 0;
    private long savedTime = -1; // Original world time to restore on end
    private int bossSpawnDelay = 0;
    private boolean bossSpawnScheduled = false;

    public BlueMoonMode(ChaosCraftPlugin plugin) {
        super(plugin, "bluemoon");
        this.moonConfig = new BlueMoonConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new BlueMoonScheduler(plugin, attackRegistry, moonConfig);
        this.bossManager = new BlueMoonBossManager(plugin, moonConfig);
        this.bossManager.setAttackRegistry(attackRegistry);
        this.gimmickManager = new LunarGimmickManager(plugin);
        this.skyEffect = new BlueMoonSkyEffect(plugin);
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

        // Boss attacks (11)
        BlueMoonBossAttacks.registerAll(plugin, attackRegistry);

        // ModelEngine VFX attacks (25)
        BlueMoonModelEngine.registerAll(plugin, attackRegistry);

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

        // Load attack configs
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int env = attackRegistry.getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int boss = attackRegistry.getByPhaseAndType(1, AttackType.BOSS).size();
        plugin.getLogger().info("[BlueMoon] " + bd + " BLOCK_DISPLAY, " + env + " ENVIRONMENTAL, "
                + boss + " BOSS attacks registered (" + attackCount + " total).");

        // Start attack scheduler
        attackScheduler.start();

        // Schedule boss spawn after delay
        if (moonConfig.isBossEnabled()) {
            bossSpawnDelay = moonConfig.getBossSpawnDelayTicks();
            bossSpawnScheduled = true;
            plugin.getLogger().info("[BlueMoon] Boss will spawn in " + bossSpawnDelay + " ticks.");
            bossManager.setEarlyKillCallback(() -> {
                plugin.getLogger().info("[BlueMoon] Boss killed early! Bonus rewards triggered.");
                // Run early kill bonus commands
                for (String cmd : moonConfig.getEarlyKillBonusCommands()) {
                    for (Player p : world.getPlayers()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                cmd.replace("%player%", p.getName()));
                    }
                }
            });
        }

        // Start gimmick manager
        gimmickManager.loadConfig(moonConfig);

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().createSession("bluemoon",
                    moonConfig.getMobSpawnConfig(), world);
        }

        plugin.getLogger().info("[BlueMoon] Mode fully started. Survive or slay the moon! "
                + "(" + attackCount + " attacks registered)");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        World world = getBlueMoonWorld();
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

        // Tick gimmicks
        gimmickManager.tick(world);

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().tick("bluemoon", getExemptPlayers());
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[BlueMoon] Mode ending — cleaning up...");

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();

        // Cleanup boss + gimmicks
        bossManager.cleanup();
        gimmickManager.cleanup();

        // Universal mob spawning cleanup
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().destroySession("bluemoon");
        }

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
    public BlueMoonBossManager getBossManager() { return bossManager; }
    public LunarGimmickManager getGimmickManager() { return gimmickManager; }
    public BlueMoonSkyEffect getSkyEffect() { return skyEffect; }
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
