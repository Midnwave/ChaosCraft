package com.blockforge.chaoscraft.modes.fluffy;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.fluffy.attacks.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

/**
 * Fluffy Mode — Cute-but-deadly survival mode.
 *
 * No boss, no escalation — pure random weighted attack survival.
 * 50 BlockDisplay + 50 Environmental attacks; ModelEngine attacks
 * planned but not registered yet (weight 0).
 *
 * Unique mechanics:
 * <ul>
 *   <li>{@link FluffyMobAI} — custom Java state-machine AI for managed mobs</li>
 *   <li>{@link FluffyRainSpawner} — continuous rain of falling fluffy mobs</li>
 *   <li>{@link FluffyWorldEffects} — 8 ambient effects + flora bloom system</li>
 *   <li>Herd Pulse — periodic synchronized freeze of all managed mobs (in {@link FluffyScheduler})</li>
 *   <li>{@link FluffyVersionUtil} — version-aware flora list (1.21.4 vs 1.21.5+)</li>
 * </ul>
 */
public class FluffyMode extends AbstractMode {

    private final FluffyConfig fluffyConfig;
    private final AttackRegistry attackRegistry;
    private final FluffyScheduler attackScheduler;
    private final FluffyMobAI mobAI;
    private final FluffyRainSpawner rainSpawner;
    private final FluffyWorldEffects worldEffects;

    private int tickCounter = 0;
    private int ambientSoundNextTick = 0;
    private final Random random = new Random();

    private int endTaskId = -1;
    private Location arenaCenter;

    public FluffyMode(ChaosCraftPlugin plugin) {
        super(plugin, "fluffy");
        this.fluffyConfig = new FluffyConfig(plugin);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new FluffyScheduler(plugin, attackRegistry, fluffyConfig);
        this.mobAI = new FluffyMobAI(plugin, fluffyConfig);
        this.rainSpawner = new FluffyRainSpawner(plugin, fluffyConfig);
        this.worldEffects = new FluffyWorldEffects(plugin, fluffyConfig);

        plugin.getServer().getPluginManager().registerEvents(mobAI, plugin);
        plugin.getServer().getPluginManager().registerEvents(worldEffects, plugin);

        registerAllAttacks();
    }

    private void registerAllAttacks() {
        FluffyBlockDisplay.registerAll(plugin, attackRegistry);
        FluffyBlockDisplay2.registerAll(plugin, attackRegistry);
        FluffyBlockDisplay3.registerAll(plugin, attackRegistry);
        FluffyBlockDisplay4.registerAll(plugin, attackRegistry);
        FluffyBlockDisplay5.registerAll(plugin, attackRegistry);
        FluffyEnvironmental.registerAll(plugin, attackRegistry);
        FluffyEnvironmental2.registerAll(plugin, attackRegistry);
        FluffyEnvironmental3.registerAll(plugin, attackRegistry);
        FluffyEnvironmental4.registerAll(plugin, attackRegistry);
        FluffyEnvironmental5.registerAll(plugin, attackRegistry);
        FluffyModelEngine.registerAll(plugin, attackRegistry);
        attackRegistry.reloadConfigs();
        applyDifficultyToAttacks();
        plugin.getLogger().info("[Fluffy] Registered " + attackRegistry.size() + " attacks, configs loaded.");
    }

    /**
     * Multiplies every registered Fluffy attack's damage by the global
     * difficulty-multiplier from FluffyConfig (default 15.0). Applied AFTER
     * attackRegistry.reloadConfigs() so user-supplied per-attack damage
     * values are still scaled. Fails open (multiplier = 1.0) on any error.
     */
    private void applyDifficultyToAttacks() {
        double diffMult;
        try {
            diffMult = fluffyConfig.getDifficultyMultiplier();
        } catch (Throwable t) {
            diffMult = 1.0;
        }
        if (diffMult == 1.0) return;
        int scaled = 0;
        for (var atk : attackRegistry.getAll()) {
            try {
                var cfg = atk.getConfig();
                cfg.setDamage(cfg.getDamage() * diffMult);
                scaled++;
            } catch (Throwable ignored) {}
        }
        plugin.getLogger().info("[Fluffy] Applied difficulty x" + diffMult + " to "
                + scaled + " attacks.");
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        tickCounter = 0;
        plugin.getLogger().info("[Fluffy] Mode starting — initializing systems...");

        World world = getFluffyWorld();
        if (world == null) {
            plugin.getLogger().severe("[Fluffy] Cannot start — no world found!");
            return;
        }
        plugin.getLogger().info("[Fluffy] Running in world: " + world.getName()
                + " (" + world.getPlayers().size() + " players)");

        for (Player player : world.getPlayers()) {
            trackPlayer(player);
        }
        loadExemptPlayers();

        // Determine arena center — first online player, or world spawn
        if (!world.getPlayers().isEmpty()) {
            arenaCenter = world.getPlayers().get(0).getLocation().clone();
        } else {
            arenaCenter = world.getSpawnLocation().clone();
        }

        // Reload attack configs and re-apply difficulty multiplier
        attackRegistry.reloadConfigs();
        applyDifficultyToAttacks();

        int attackCount = attackRegistry.size();
        int bd = attackRegistry.getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int env = attackRegistry.getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int me = attackRegistry.getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
        plugin.getLogger().info("[Fluffy] " + bd + " BLOCK_DISPLAY, " + env + " ENVIRONMENTAL, "
                + me + " MODEL_ENGINE attacks registered (" + attackCount + " total).");

        // Start subsystems
        attackScheduler.start();
        if (fluffyConfig.isMobAiEnabled()) mobAI.start();
        rainSpawner.start();
        worldEffects.start(world, arenaCenter);

        // On-start commands
        for (String cmd : fluffyConfig.getOnStartCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().createSession("fluffy",
                    fluffyConfig.getMobSpawnConfig(), world);
        }

        // Schedule mode-end after duration
        long duration = (long) fluffyConfig.getDurationSeconds() * 20L;
        endTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getModeManager().endActiveMode();
        }, duration).getTaskId();

        // Initialize ambient sound timer
        ambientSoundNextTick = nextAmbientTick();

        plugin.getLogger().info("[Fluffy] Mode fully started. Duration: "
                + fluffyConfig.getDurationSeconds() + "s, arena radius: " + fluffyConfig.getArenaRadius());
    }

    @Override
    public void onTick() {
        tickCounter++;
        attackScheduler.tick();
        rainSpawner.tick();
        worldEffects.tick();

        // Ambient sounds
        if (fluffyConfig.isAmbientSoundsEnabled() && tickCounter >= ambientSoundNextTick) {
            playRandomAmbient();
            ambientSoundNextTick = tickCounter + nextAmbientInterval();
        }

        // Universal mob spawning
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().tick("fluffy", getExemptPlayers());
        }
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Fluffy] Mode ending — cleaning up...");

        attackScheduler.stop();
        mobAI.stop();
        rainSpawner.stop();
        worldEffects.stop();
        plugin.getMusicManager().stopAll();

        if (endTaskId != -1) {
            try { Bukkit.getScheduler().cancelTask(endTaskId); } catch (Throwable ignored) {}
            endTaskId = -1;
        }

        // On-end commands
        for (String cmd : fluffyConfig.getOnEndCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }

        // Universal mob spawning cleanup
        if (plugin.getMobSpawnService() != null) {
            plugin.getMobSpawnService().destroySession("fluffy");
        }

        tickCounter = 0;
        plugin.getLogger().info("[Fluffy] Mode ended. All systems cleaned up.");
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
        // No dimension-change handling for Fluffy.
    }

    // ========================
    // Helpers
    // ========================

    private int nextAmbientTick() {
        return tickCounter + nextAmbientInterval();
    }

    private int nextAmbientInterval() {
        int min = Math.max(1, fluffyConfig.getAmbientSoundMinInterval());
        int max = Math.max(min + 1, fluffyConfig.getAmbientSoundMaxInterval());
        return min + random.nextInt(max - min);
    }

    private void playRandomAmbient() {
        World world = getFluffyWorld();
        if (world == null) return;
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;
        List<String> sounds = fluffyConfig.getAmbientSounds();
        if (sounds == null || sounds.isEmpty()) return;
        Player p = players.get(random.nextInt(players.size()));
        String pick = sounds.get(random.nextInt(sounds.size()));
        try {
            Sound sound = Sound.valueOf(pick);
            p.playSound(p.getLocation(), sound, (float) fluffyConfig.getAmbientSoundVolume(),
                    0.8f + random.nextFloat() * 0.6f);
        } catch (IllegalArgumentException e) {
            // Try as namespaced custom sound key
            try {
                p.playSound(p.getLocation(), pick.toLowerCase(),
                        (float) fluffyConfig.getAmbientSoundVolume(), 1.0f);
            } catch (Throwable ignored) {}
        }
    }

    public World getFluffyWorld() {
        String configWorld = fluffyConfig.getWorldName();
        if (configWorld != null && !configWorld.isEmpty()) {
            World w = Bukkit.getWorld(configWorld);
            if (w != null) return w;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    // ========================
    // Accessors
    // ========================

    public FluffyConfig getFluffyConfig() { return fluffyConfig; }
    public AttackRegistry getAttackRegistry() { return attackRegistry; }
    public FluffyScheduler getAttackScheduler() { return attackScheduler; }
    public FluffyMobAI getMobAI() { return mobAI; }
    public FluffyRainSpawner getRainSpawner() { return rainSpawner; }
    public FluffyWorldEffects getWorldEffects() { return worldEffects; }
    public int getTickCounter() { return tickCounter; }
}
