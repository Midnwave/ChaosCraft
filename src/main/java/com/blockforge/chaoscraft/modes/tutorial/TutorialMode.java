package com.blockforge.chaoscraft.modes.tutorial;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.tutorial.attacks.*;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Random;

/**
 * Tutorial Mode — teaches Minecraft basics in 3:30 while decorative + mild
 * block displays spawn around the player. 20 different tutorial paths,
 * 53 block display attacks.
 */
public class TutorialMode extends AbstractMode {

    private final TutorialConfig tutorialConfig;
    private final AttackRegistry attackRegistry;
    private final TutorialScheduler attackScheduler;
    private final TutorialTracker tracker;
    private final TutorialListener listener;
    private long tickCounter = 0;
    private String forcedDesignOverride = null;
    private TutorialDesign activeDesign = null;

    // (Tutorial Diamond recipe removed — all steps are vanilla now)

    // Vanilla hostile mob spawning
    private static final EntityType[] TUTORIAL_MOBS = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER
    };
    private static final int MOB_SPAWN_INTERVAL = 200; // Every 10 seconds
    private static final int MAX_MOBS_PER_PLAYER = 3;
    private static final double MOB_SPAWN_DISTANCE = 20.0;
    private final Random mobRandom = new Random();

    public TutorialMode(ChaosCraftPlugin plugin) {
        super(plugin, "tutorial");
        this.tutorialConfig = new TutorialConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new TutorialScheduler(plugin, attackRegistry, tutorialConfig);
        this.tracker = new TutorialTracker(plugin, tutorialConfig);
        this.listener = new TutorialListener(plugin, tracker);
        // No custom recipes needed — all tutorial steps use vanilla crafting
        registerAllAttacks();
    }

    private void registerAllAttacks() {
        GuidingDisplays.registerAll(plugin, attackRegistry);
        ToolItemShapes.registerAll(plugin, attackRegistry);
        ProgressAchievement.registerAll(plugin, attackRegistry);
        CelebrationReward.registerAll(plugin, attackRegistry);
        HazardChallenge.registerAll(plugin, attackRegistry);

        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[Tutorial] Registered " + attackRegistry.size() + " attacks, configs loaded.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;
        plugin.getLogger().info("[Tutorial] Mode starting...");

        World world = getTutorialWorld();
        if (world == null) {
            plugin.getLogger().severe("[Tutorial] Cannot start — world not found!");
            return;
        }

        // Track all players
        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }
        loadExemptPlayers();

        // Select tutorial design
        activeDesign = selectDesign();
        plugin.getLogger().info("[Tutorial] Selected design: " + activeDesign.getDisplayName()
                + " (" + activeDesign.getStepCount() + " steps)");

        // Register tutorial diamond recipe
        // All tutorial steps use vanilla recipes — no custom registration needed

        // Start tutorial tracking for each player
        for (Player player : world.getPlayers()) {
            if (!isExempt(player)) {
                tracker.startTutorial(player, activeDesign);
            }
        }

        // Load attack configs
        attackRegistry.reloadConfigs();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        plugin.getLogger().info("[Tutorial] " + bd + " BLOCK_DISPLAY attacks available.");

        // Start scheduler
        attackScheduler.start();

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        plugin.getLogger().info("[Tutorial] Mode fully started.");
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();

        // Refresh actionbar every 20 ticks (1 second)
        if (tickCounter % 20 == 0) {
            tracker.tickActionbar();
        }

        // Send chat reminder every 200 ticks (10 seconds)
        if (tickCounter % 200 == 0) {
            tracker.tickChatReminder();
        }

        // Spawn vanilla hostile mobs around players
        if (tickCounter % MOB_SPAWN_INTERVAL == 0) {
            spawnTutorialMobs();
        }
    }

    private void spawnTutorialMobs() {
        World world = getTutorialWorld();
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            if (isExempt(player)) continue;

            // Count nearby hostile mobs
            long nearbyMobs = player.getLocation().getNearbyEntities(30, 30, 30).stream()
                    .filter(e -> e instanceof org.bukkit.entity.Monster).count();
            if (nearbyMobs >= MAX_MOBS_PER_PLAYER) continue;

            // Spawn at random position around player
            double angle = mobRandom.nextDouble() * Math.PI * 2;
            Location spawnLoc = player.getLocation().clone().add(
                    Math.cos(angle) * MOB_SPAWN_DISTANCE, 0, Math.sin(angle) * MOB_SPAWN_DISTANCE);
            spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

            EntityType mobType = TUTORIAL_MOBS[mobRandom.nextInt(TUTORIAL_MOBS.length)];
            world.spawnEntity(spawnLoc, mobType);
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Tutorial] Mode ending...");

        attackScheduler.stop();
        plugin.getMusicManager().stopAll();

        // Unregister listener
        HandlerList.unregisterAll(listener);

        // Remove tutorial diamond recipe
        // No custom recipes to clean up

        // Cleanup tracking
        tracker.cleanup();
        activeDesign = null;
        forcedDesignOverride = null;
        tickCounter = 0;

        plugin.getLogger().info("[Tutorial] Mode ended.");
    }

    @Override
    public void onPlayerJoin(Player player) {
        trackPlayer(player);
        plugin.getMusicManager().playForPlayer(player);

        // Start tutorial for joining player
        if (activeDesign != null && !isExempt(player)) {
            tracker.startTutorial(player, activeDesign);
        }
    }

    @Override
    public void onPlayerDeath(Player player) {
        markDeath(player);
        plugin.debug("[Tutorial] " + player.getName() + " died.");
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // Music handled by MusicManager listener
    }

    // ========================
    // Design Selection
    // ========================

    private TutorialDesign selectDesign() {
        // Check forced override (from command)
        if (forcedDesignOverride != null && !forcedDesignOverride.isEmpty()) {
            TutorialDesign forced = TutorialDesign.getByName(forcedDesignOverride);
            if (forced != null) return forced;
        }

        // Check config forced design
        String configForced = tutorialConfig.getForcedDesign();
        if (!configForced.isEmpty() && !tutorialConfig.isRandomDesign()) {
            TutorialDesign forced = TutorialDesign.getByName(configForced);
            if (forced != null) return forced;
        }

        // Random selection
        return TutorialDesign.getRandom(new Random());
    }

    // (Tutorial Diamond recipe removed — all steps are vanilla now)

    // ========================
    // API
    // ========================

    public TutorialConfig getTutorialConfig() { return tutorialConfig; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public TutorialScheduler getAttackScheduler() { return attackScheduler; }
    public TutorialTracker getTracker() { return tracker; }
    public long getTickCounter() { return tickCounter; }

    public void setForcedDesign(String designId) {
        this.forcedDesignOverride = designId;
    }

    public String getActiveDesignName() {
        return activeDesign != null ? activeDesign.getDisplayName() : "none";
    }

    public World getTutorialWorld() {
        String worldName = tutorialConfig.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            return plugin.getServer().getWorld(worldName);
        }
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }
}
