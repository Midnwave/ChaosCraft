package com.blockforge.chaoscraft.modes.calamity;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Calamity-specific configuration loader.
 * Reads from plugins/ChaosCraft/modes/calamity/calamity.yml with all
 * boss, gem, phase music, island, and egg settings.
 */
public class CalamityConfig {

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public CalamityConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        File modeDir = new File(plugin.getDataFolder(), "modes/calamity");
        if (!modeDir.exists()) modeDir.mkdirs();
        this.configFile = new File(modeDir, "calamity.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save calamity config: " + e.getMessage());
        }
    }

    public FileConfiguration get() {
        return config;
    }

    // ========================
    // Timer
    // ========================

    public long getDefaultTimerSeconds() {
        return config.getLong("timer.default-seconds", 1200);
    }

    public long getMaxTimerSeconds() {
        return config.getLong("timer.max-seconds", 3600);
    }

    // ========================
    // Music — per-phase overrides
    // ========================

    public String getBaseMusicId() {
        return config.getString("music.sound-id", "");
    }

    public boolean isBaseMusicLooped() {
        return config.getBoolean("music.loop", true);
    }

    public long getBaseMusicDurationTicks() {
        return config.getLong("music.duration-ticks", 6000);
    }

    /**
     * Returns phase music entries: phase number → {sound-id, loop, duration-ticks}
     */
    public Map<Integer, PhaseMusicEntry> getPhaseMusicOverrides() {
        Map<Integer, PhaseMusicEntry> map = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("music.phases");
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            try {
                int phase = Integer.parseInt(key);
                String soundId = section.getString(key + ".sound-id", "");
                boolean loop = section.getBoolean(key + ".loop", true);
                long duration = section.getLong(key + ".duration-ticks", 6000);
                map.put(phase, new PhaseMusicEntry(soundId, loop, duration));
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    /**
     * DoG Phase 2 music override (separate entry since it's mid-boss).
     */
    public PhaseMusicEntry getDogPhase2Music() {
        String soundId = config.getString("music.dog-phase2.sound-id", "");
        boolean loop = config.getBoolean("music.dog-phase2.loop", true);
        long duration = config.getLong("music.dog-phase2.duration-ticks", 6000);
        return new PhaseMusicEntry(soundId, loop, duration);
    }

    // ========================
    // Gem system
    // ========================

    /**
     * Get the gem item template (serialized bytes). Null if not set.
     */
    public byte[] getGemItemBytes() {
        Object raw = config.get("gem.item");
        if (raw instanceof byte[] bytes) return bytes;
        return null;
    }

    public void setGemItemBytes(byte[] bytes) {
        config.set("gem.item", bytes);
    }

    public int getGemSpawnRateNoBoss() {
        return config.getInt("gem.spawn-rate.no-boss-ticks", 100);
    }

    public int getGemSpawnRateBossActive() {
        return config.getInt("gem.spawn-rate.boss-active-ticks", 400);
    }

    public int getGemSpawnRateBetweenBosses() {
        return config.getInt("gem.spawn-rate.between-bosses-ticks", 60);
    }

    public int getGemSpawnHeight() {
        return config.getInt("gem.spawn-height", 40);
    }

    public int getGemSpawnRadius() {
        return config.getInt("gem.spawn-radius", 30);
    }

    // ========================
    // Bosses
    // ========================

    public int getBossGemRequirement(String bossName) {
        return config.getInt("bosses." + bossName + ".gem-requirement", 50);
    }

    public String getBossMythicId(String bossName) {
        return config.getString("bosses." + bossName + ".mythicmobs-id", "");
    }

    public double getBossSpawnX(String bossName) {
        return config.getDouble("bosses." + bossName + ".spawn-x", 0);
    }

    public double getBossSpawnY(String bossName) {
        return config.getDouble("bosses." + bossName + ".spawn-y", 75);
    }

    public double getBossSpawnZ(String bossName) {
        return config.getDouble("bosses." + bossName + ".spawn-z", 0);
    }

    public Location getBossSpawnLocation(String bossName, World world) {
        return new Location(world,
                getBossSpawnX(bossName),
                getBossSpawnY(bossName),
                getBossSpawnZ(bossName));
    }

    // DoG specific
    public long getDogTotalHealth() {
        return config.getLong("bosses.dog.health", 12_000_000L);
    }

    public long getDogPhase2Health() {
        return config.getLong("bosses.dog.phase2-health", 6_000_000L);
    }

    // Dragon specific
    public int getDragonMiniBossCount() {
        return config.getInt("bosses.dragon.mini-boss-count", 4);
    }

    public int getDragonMiniBossSpawnRadius() {
        return config.getInt("bosses.dragon.mini-boss-spawn-radius", 10);
    }

    public List<String> getDragonMiniBossIds() {
        return config.getStringList("bosses.dragon.mini-boss-ids");
    }

    // Supreme Calamitas specific
    public byte[] getCalamitasHeldItemBytes() {
        Object raw = config.get("bosses.calamitas.held-item");
        if (raw instanceof byte[] bytes) return bytes;
        return null;
    }

    public void setCalamitasHeldItemBytes(byte[] bytes) {
        config.set("bosses.calamitas.held-item", bytes);
    }

    public int getCalamitasDetectionRange() {
        return config.getInt("bosses.calamitas.detection-range", 300);
    }

    public double getCalamitasFlySpeed() {
        return config.getDouble("bosses.calamitas.fly-speed", 0.5);
    }

    public double getCalamitasGroundSpeed() {
        return config.getDouble("bosses.calamitas.ground-speed", 0.3);
    }

    public double getCalamitasHealth() {
        return config.getDouble("bosses.calamitas.health", 500.0);
    }

    public double getCalamitasAttackDamage() {
        return config.getDouble("bosses.calamitas.attack-damage", 25.0);
    }

    public int getCalamitasAttackInterval() {
        return config.getInt("bosses.calamitas.attack-interval", 20);
    }

    public org.bukkit.inventory.ItemStack getCalamitasHeldItem() {
        byte[] bytes = getCalamitasHeldItemBytes();
        if (bytes != null) {
            return org.bukkit.inventory.ItemStack.deserializeBytes(bytes);
        }
        return new org.bukkit.inventory.ItemStack(org.bukkit.Material.TRIDENT);
    }

    // ========================
    // Egg animation
    // ========================

    public int getEggAttackCount() {
        return config.getInt("egg.attack-count", 30);
    }

    public long getEggChargeDurationTicks() {
        return config.getLong("egg.charge-duration-ticks", 200);
    }

    public int getEggOrbitCubeCount() {
        return config.getInt("egg.orbit-cube-count", 8);
    }

    public double getEggOrbitRadius() {
        return config.getDouble("egg.orbit-radius", 2.5);
    }

    public double getEggOrbitSpeed() {
        return config.getDouble("egg.orbit-speed", 0.05);
    }

    // ========================
    // Portal
    // ========================

    public String getPortalItemsAdderId() {
        return config.getString("portal.itemsadder-block-id", "chaoscraft:calamity_portal");
    }

    // ========================
    // Island themes
    // ========================

    public String getIslandTheme(int phase) {
        return config.getString("island.phases." + phase, "default");
    }

    // ========================
    // On-start / on-end commands
    // ========================

    public List<String> getOnStartCommands() {
        return config.getStringList("on-start-commands");
    }

    public List<String> getOnEndCommands() {
        return config.getStringList("on-end-commands");
    }

    public List<String> getExemptPlayers() {
        return config.getStringList("exempt-players");
    }

    public int getMaxEventsPerPlayer() {
        return config.getInt("max-events-per-player", 5);
    }

    public List<String> getRewardCommands() {
        return config.getStringList("rewards.commands");
    }

    // ========================
    // Default config creation
    // ========================

    private void createDefaults() {
        FileConfiguration defaults = new YamlConfiguration();

        // Timer
        defaults.set("timer.default-seconds", 1200);
        defaults.set("timer.max-seconds", 3600);

        // Music
        defaults.set("music.sound-id", "chaoscraft:calamity.phase1");
        defaults.set("music.loop", true);
        defaults.set("music.duration-ticks", 6000);
        defaults.set("music.phases.1.sound-id", "chaoscraft:calamity.phase1");
        defaults.set("music.phases.1.loop", true);
        defaults.set("music.phases.1.duration-ticks", 6000);
        defaults.set("music.phases.2.sound-id", "chaoscraft:calamity.phase2");
        defaults.set("music.phases.2.loop", true);
        defaults.set("music.phases.2.duration-ticks", 6000);
        defaults.set("music.phases.3.sound-id", "chaoscraft:calamity.phase3");
        defaults.set("music.phases.3.loop", true);
        defaults.set("music.phases.3.duration-ticks", 6000);
        defaults.set("music.phases.4.sound-id", "chaoscraft:calamity.phase4");
        defaults.set("music.phases.4.loop", true);
        defaults.set("music.phases.4.duration-ticks", 6000);
        defaults.set("music.phases.5.sound-id", "chaoscraft:calamity.phase5");
        defaults.set("music.phases.5.loop", true);
        defaults.set("music.phases.5.duration-ticks", 6000);
        defaults.set("music.dog-phase2.sound-id", "chaoscraft:calamity.dog_phase2");
        defaults.set("music.dog-phase2.loop", true);
        defaults.set("music.dog-phase2.duration-ticks", 6000);

        // Gem system
        defaults.set("gem.item", null);
        defaults.set("gem.spawn-rate.no-boss-ticks", 100);
        defaults.set("gem.spawn-rate.boss-active-ticks", 400);
        defaults.set("gem.spawn-rate.between-bosses-ticks", 60);
        defaults.set("gem.spawn-height", 40);
        defaults.set("gem.spawn-radius", 30);

        // Boss: Voidmaw
        defaults.set("bosses.voidmaw.mythicmobs-id", "Voidmaw");
        defaults.set("bosses.voidmaw.gem-requirement", 50);
        defaults.set("bosses.voidmaw.spawn-x", 0.0);
        defaults.set("bosses.voidmaw.spawn-y", 75.0);
        defaults.set("bosses.voidmaw.spawn-z", 0.0);

        // Boss: DoG
        defaults.set("bosses.dog.gem-requirement", 75);
        defaults.set("bosses.dog.health", 12_000_000L);
        defaults.set("bosses.dog.phase2-health", 6_000_000L);

        // Boss: Dweller
        defaults.set("bosses.dweller.mythicmobs-id", "Dweller");
        defaults.set("bosses.dweller.gem-requirement", 100);
        defaults.set("bosses.dweller.spawn-x", 0.0);
        defaults.set("bosses.dweller.spawn-y", 75.0);
        defaults.set("bosses.dweller.spawn-z", 0.0);

        // Boss: Dragon
        defaults.set("bosses.dragon.gem-requirement", 125);
        defaults.set("bosses.dragon.mini-boss-count", 4);
        defaults.set("bosses.dragon.mini-boss-spawn-radius", 10);
        defaults.set("bosses.dragon.mini-boss-ids", List.of("VoidEmperorMinion1", "VoidEmperorMinion2", "VoidEmperorMinion3", "VoidEmperorMinion4"));

        // Boss: Supreme Calamitas
        defaults.set("bosses.calamitas.gem-requirement", 150);
        defaults.set("bosses.calamitas.held-item", null);
        defaults.set("bosses.calamitas.detection-range", 300);
        defaults.set("bosses.calamitas.fly-speed", 0.5);
        defaults.set("bosses.calamitas.ground-speed", 0.3);

        // Egg animation
        defaults.set("egg.attack-count", 30);
        defaults.set("egg.charge-duration-ticks", 200);
        defaults.set("egg.orbit-cube-count", 8);
        defaults.set("egg.orbit-radius", 2.5);
        defaults.set("egg.orbit-speed", 0.05);

        // Portal
        defaults.set("portal.itemsadder-block-id", "chaoscraft:calamity_portal");

        // Island themes
        defaults.set("island.phases.1", "void_fracture");
        defaults.set("island.phases.2", "crystalline_plague");
        defaults.set("island.phases.3", "brimstone_inversion");
        defaults.set("island.phases.4", "fractured_sanctum");
        defaults.set("island.phases.5", "calamitous_end");

        // Mode lifecycle
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.set("exempt-players", new ArrayList<>());
        defaults.set("max-events-per-player", 5);
        defaults.set("rewards.commands", new ArrayList<>());

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default calamity config: " + e.getMessage());
        }
    }

    // ========================
    // Data classes
    // ========================

    public record PhaseMusicEntry(String soundId, boolean loop, long durationTicks) {}
}
