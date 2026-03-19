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

        // Ensure ALL keys exist — both base mode keys and calamity-specific keys.
        boolean needsSave = false;

        // ── Base mode keys ───────────────────────────────────────────────────
        if (!config.contains("config-version")) { config.set("config-version", 1); needsSave = true; }
        if (!config.contains("timer.default-seconds")) { config.set("timer.default-seconds", 1200); needsSave = true; }
        if (!config.contains("timer.max-seconds")) { config.set("timer.max-seconds", 2400); needsSave = true; }
        if (!config.contains("music.sound-id")) { config.set("music.sound-id", ""); needsSave = true; }
        if (!config.contains("music.loop")) { config.set("music.loop", true); needsSave = true; }
        if (!config.contains("music.duration-ticks")) { config.set("music.duration-ticks", 6000); needsSave = true; }
        if (!config.contains("on-start-commands")) { config.set("on-start-commands", new java.util.ArrayList<>()); needsSave = true; }
        if (!config.contains("on-end-commands")) { config.set("on-end-commands", new java.util.ArrayList<>()); needsSave = true; }
        if (!config.contains("exempt-players")) { config.set("exempt-players", new java.util.ArrayList<>()); needsSave = true; }
        if (!config.contains("max-events-per-player")) { config.set("max-events-per-player", 5); needsSave = true; }
        if (!config.contains("rewards.commands")) { config.set("rewards.commands", new java.util.ArrayList<>()); needsSave = true; }

        if (needsSave) {
            save();
            config = YamlConfiguration.loadConfiguration(configFile);
        }
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

        // ── Timer ──────────────────────────────────────────────────────────────
        defaults.set("timer.default-seconds", 1200);
        defaults.setComments("timer.default-seconds", List.of(
                "Default duration of Calamity Mode in seconds when started without a time argument.",
                "1200 = 20 min. This is a long boss rush — tune to match your encounter pacing."));
        defaults.set("timer.max-seconds", 3600);
        defaults.setComments("timer.max-seconds", List.of(
                "Maximum timer value (in seconds) allowed via: /cc modes calamity start <seconds>",
                "3600 = 1 hour soft cap for the full 5-boss gauntlet."));

        // ── Music — base track and per-phase overrides ─────────────────────────
        defaults.set("music.sound-id", "chaoscraft:calamity.phase1");
        defaults.setComments("music.sound-id", List.of(
                "Fallback music sound ID played if no phase-specific override is configured.",
                "Use a namespaced ID: \"chaoscraft:music.calamity_base\"   |   Leave empty to disable."));
        defaults.set("music.loop", true);
        defaults.setComments("music.loop", List.of(
                "Whether the fallback music track loops continuously."));
        defaults.set("music.duration-ticks", 6000);
        defaults.setComments("music.duration-ticks", List.of(
                "Duration of the fallback music loop in ticks. 6000 = 5 minutes."));
        defaults.set("music.phases.1.sound-id", "chaoscraft:calamity.phase1");
        defaults.setComments("music.phases.1.sound-id", List.of(
                "Music played during Phase 1 (Voidmaw encounter). Overrides the base music.sound-id."));
        defaults.set("music.phases.1.loop", true);
        defaults.set("music.phases.1.duration-ticks", 6000);
        defaults.setComments("music.phases.1.duration-ticks", List.of(
                "Duration of the Phase 1 music loop in ticks before it repeats."));
        defaults.set("music.phases.2.sound-id", "chaoscraft:calamity.phase2");
        defaults.setComments("music.phases.2.sound-id", List.of(
                "Music played during Phase 2 (DoG encounter)."));
        defaults.set("music.phases.2.loop", true);
        defaults.set("music.phases.2.duration-ticks", 6000);
        defaults.set("music.phases.3.sound-id", "chaoscraft:calamity.phase3");
        defaults.setComments("music.phases.3.sound-id", List.of(
                "Music played during Phase 3 (Dweller encounter)."));
        defaults.set("music.phases.3.loop", true);
        defaults.set("music.phases.3.duration-ticks", 6000);
        defaults.set("music.phases.4.sound-id", "chaoscraft:calamity.phase4");
        defaults.setComments("music.phases.4.sound-id", List.of(
                "Music played during Phase 4 (Dragon / Void Emperor encounter)."));
        defaults.set("music.phases.4.loop", true);
        defaults.set("music.phases.4.duration-ticks", 6000);
        defaults.set("music.phases.5.sound-id", "chaoscraft:calamity.phase5");
        defaults.setComments("music.phases.5.sound-id", List.of(
                "Music played during Phase 5 (Supreme Calamitas — final boss encounter)."));
        defaults.set("music.phases.5.loop", true);
        defaults.set("music.phases.5.duration-ticks", 6000);
        defaults.set("music.dog-phase2.sound-id", "chaoscraft:calamity.dog_phase2");
        defaults.setComments("music.dog-phase2.sound-id", List.of(
                "Special music override that kicks in mid-fight when DoG transitions to Phase 2.",
                "This plays instead of the Phase 2 music while DoG is in its second half."));
        defaults.set("music.dog-phase2.loop", true);
        defaults.set("music.dog-phase2.duration-ticks", 6000);

        // ── Gem System ─────────────────────────────────────────────────────────
        defaults.set("gem.item", null);
        defaults.setComments("gem.item", List.of(
                "Serialized ItemStack bytes for the Calamity Gem drop item.",
                "Set this via the /cc modes calamity setgem command — do NOT edit manually."));
        defaults.set("gem.spawn-rate.no-boss-ticks", 100);
        defaults.setComments("gem.spawn-rate.no-boss-ticks", List.of(
                "Ticks between gem spawn attempts when no boss is currently alive.",
                "100 = attempt every 5 seconds. Gems are the currency used to trigger each boss."));
        defaults.set("gem.spawn-rate.boss-active-ticks", 400);
        defaults.setComments("gem.spawn-rate.boss-active-ticks", List.of(
                "Ticks between gem spawn attempts while a boss is currently active.",
                "400 = every 20 seconds. Reduced frequency during boss fights."));
        defaults.set("gem.spawn-rate.between-bosses-ticks", 60);
        defaults.setComments("gem.spawn-rate.between-bosses-ticks", List.of(
                "Ticks between gem spawns in the brief window after one boss dies and before the next.",
                "60 = every 3 seconds — faster spawning to help players stock up between phases."));
        defaults.set("gem.spawn-height", 40);
        defaults.setComments("gem.spawn-height", List.of(
                "Y-level at which gems spawn (dropped from the sky). Set to match your arena height.",
                "Gems fall from this Y-coordinate above the arena floor."));
        defaults.set("gem.spawn-radius", 30);
        defaults.setComments("gem.spawn-radius", List.of(
                "Radius in blocks around the arena center within which gems can spawn.",
                "Set to match your arena size so gems land within the playable area."));

        // ── Boss: Voidmaw (Phase 1) ────────────────────────────────────────────
        defaults.set("bosses.voidmaw.mythicmobs-id", "Voidmaw");
        defaults.setComments("bosses.voidmaw.mythicmobs-id", List.of(
                "MythicMobs mob ID for the Voidmaw boss (Phase 1). Must match the mob ID in MythicMobs."));
        defaults.set("bosses.voidmaw.gem-requirement", 50);
        defaults.setComments("bosses.voidmaw.gem-requirement", List.of(
                "Number of gems players must collect before the Voidmaw boss is summoned.",
                "This is the Phase 1 trigger threshold. Lower = boss spawns sooner."));
        defaults.set("bosses.voidmaw.spawn-x", 0.0);
        defaults.set("bosses.voidmaw.spawn-y", 75.0);
        defaults.set("bosses.voidmaw.spawn-z", 0.0);
        defaults.setComments("bosses.voidmaw.spawn-z", List.of(
                "XYZ spawn coordinates for the Voidmaw boss. Set these to your arena center."));

        // ── Boss: DoG (Phase 2) ────────────────────────────────────────────────
        defaults.set("bosses.dog.gem-requirement", 75);
        defaults.setComments("bosses.dog.gem-requirement", List.of(
                "Gems required to trigger the DoG (Devourer of Gods) Phase 2 boss encounter."));
        defaults.set("bosses.dog.health", 12_000_000L);
        defaults.setComments("bosses.dog.health", List.of(
                "Total health of the DoG boss (tracked by ChaosCraft, not MythicMobs).",
                "This is a very large number because DoG uses a custom health tracking system."));
        defaults.set("bosses.dog.phase2-health", 6_000_000L);
        defaults.setComments("bosses.dog.phase2-health", List.of(
                "Health threshold at which DoG transitions to Phase 2 (mid-fight music change).",
                "When DoG's health drops below this value, the dog-phase2 music kicks in."));

        // ── Boss: Dweller (Phase 3) ────────────────────────────────────────────
        defaults.set("bosses.dweller.mythicmobs-id", "Dweller");
        defaults.setComments("bosses.dweller.mythicmobs-id", List.of(
                "MythicMobs mob ID for the Dweller boss (Phase 3)."));
        defaults.set("bosses.dweller.gem-requirement", 100);
        defaults.setComments("bosses.dweller.gem-requirement", List.of(
                "Gems required to trigger the Dweller Phase 3 boss encounter."));
        defaults.set("bosses.dweller.spawn-x", 0.0);
        defaults.set("bosses.dweller.spawn-y", 75.0);
        defaults.set("bosses.dweller.spawn-z", 0.0);

        // ── Boss: Dragon / Void Emperor (Phase 4) ─────────────────────────────
        defaults.set("bosses.dragon.gem-requirement", 125);
        defaults.setComments("bosses.dragon.gem-requirement", List.of(
                "Gems required to trigger the Dragon (Void Emperor) Phase 4 encounter."));
        defaults.set("bosses.dragon.mini-boss-count", 4);
        defaults.setComments("bosses.dragon.mini-boss-count", List.of(
                "Number of mini-boss minions spawned alongside the Dragon boss.",
                "Must match the number of IDs listed in mini-boss-ids."));
        defaults.set("bosses.dragon.mini-boss-spawn-radius", 10);
        defaults.setComments("bosses.dragon.mini-boss-spawn-radius", List.of(
                "Radius in blocks around the Dragon's spawn point where mini-bosses appear."));
        defaults.set("bosses.dragon.mini-boss-ids", List.of(
                "VoidEmperorMinion1", "VoidEmperorMinion2", "VoidEmperorMinion3", "VoidEmperorMinion4"));
        defaults.setComments("bosses.dragon.mini-boss-ids", List.of(
                "MythicMobs mob IDs for the Dragon's mini-boss minions. Must match MythicMobs mob IDs.",
                "These are spawned at the start of the Phase 4 encounter."));

        // ── Boss: Supreme Calamitas (Phase 5 — Final Boss) ────────────────────
        defaults.set("bosses.calamitas.gem-requirement", 150);
        defaults.setComments("bosses.calamitas.gem-requirement", List.of(
                "Gems required to trigger Supreme Calamitas — the final boss of Calamity Mode."));
        defaults.set("bosses.calamitas.held-item", null);
        defaults.setComments("bosses.calamitas.held-item", List.of(
                "Serialized ItemStack bytes for the item held by Supreme Calamitas.",
                "Set via /cc modes calamity setcalamitasitem — do NOT edit manually."));
        defaults.set("bosses.calamitas.detection-range", 300);
        defaults.setComments("bosses.calamitas.detection-range", List.of(
                "Detection range in blocks within which Supreme Calamitas targets players.",
                "Set to cover your entire arena. Larger arenas may need a higher value."));
        defaults.set("bosses.calamitas.fly-speed", 0.5);
        defaults.setComments("bosses.calamitas.fly-speed", List.of(
                "Movement speed of Supreme Calamitas while flying/in the air. Higher = more aggressive."));
        defaults.set("bosses.calamitas.ground-speed", 0.3);
        defaults.setComments("bosses.calamitas.ground-speed", List.of(
                "Movement speed of Supreme Calamitas while on the ground (charging phase)."));

        // ── Egg Animation (Phase transition sequence) ──────────────────────────
        defaults.set("egg.attack-count", 30);
        defaults.setComments("egg.attack-count", List.of(
                "Number of attack projectiles fired during the egg charge animation.",
                "This is the spectacular phase-transition sequence between boss fights."));
        defaults.set("egg.charge-duration-ticks", 200);
        defaults.setComments("egg.charge-duration-ticks", List.of(
                "Total duration in ticks of the egg charge animation before it explodes.",
                "200 = 10 seconds of buildup before unleashing the attack burst."));
        defaults.set("egg.orbit-cube-count", 8);
        defaults.setComments("egg.orbit-cube-count", List.of(
                "Number of orbiting block display cubes that swirl around the egg during charging.",
                "Higher = more dramatic visual. Recommended: 6–12."));
        defaults.set("egg.orbit-radius", 2.5);
        defaults.setComments("egg.orbit-radius", List.of(
                "Radius in blocks of the orbit path the cubes travel around the egg."));
        defaults.set("egg.orbit-speed", 0.05);
        defaults.setComments("egg.orbit-speed", List.of(
                "Angular velocity of the orbiting cubes in radians per tick.",
                "0.05 = slow elegant orbit, 0.15 = fast frantic spin."));

        // ── Portal ─────────────────────────────────────────────────────────────
        defaults.set("portal.itemsadder-block-id", "chaoscraft:calamity_portal");
        defaults.setComments("portal.itemsadder-block-id", List.of(
                "ItemsAdder block ID used for the Calamity Mode portal structure.",
                "This block is placed/detected at the arena entrance. Requires ItemsAdder."));

        // ── Island Themes (per-phase arena environment) ────────────────────────
        defaults.set("island.phases.1", "void_fracture");
        defaults.setComments("island.phases.1", List.of(
                "Theme name for the arena island during Phase 1. Controls visual environment.",
                "Available themes: void_fracture, crystalline_plague, brimstone_inversion,",
                "                  fractured_sanctum, calamitous_end."));
        defaults.set("island.phases.2", "crystalline_plague");
        defaults.set("island.phases.3", "brimstone_inversion");
        defaults.set("island.phases.4", "fractured_sanctum");
        defaults.set("island.phases.5", "calamitous_end");

        // ── Lifecycle Commands ─────────────────────────────────────────────────
        defaults.set("on-start-commands", new ArrayList<>());
        defaults.setComments("on-start-commands", List.of(
                "Console commands run when Calamity Mode starts. Use %player% for the starting player.",
                "Example:",
                "  - \"broadcast &4The Calamity begins!\""));
        defaults.set("on-end-commands", new ArrayList<>());
        defaults.setComments("on-end-commands", List.of(
                "Console commands run when Calamity Mode ends (all bosses defeated or timer expires)."));
        defaults.set("exempt-players", new ArrayList<>());
        defaults.setComments("exempt-players", List.of(
                "Player names that receive ZERO damage from all Calamity attacks (staff bypass list)."));
        defaults.set("max-events-per-player", 5);
        defaults.setComments("max-events-per-player", List.of(
                "Maximum simultaneous active attack events targeting one player at once.",
                "Calamity has dense multi-boss phases — keep this at 5–10 for best experience."));
        defaults.set("rewards.commands", new ArrayList<>());
        defaults.setComments("rewards.commands", List.of(
                "Commands run for each surviving player when all 5 bosses are defeated.",
                "Use %player% as a placeholder. Example:",
                "  - \"give %player% netherite_ingot 3\"",
                "  - \"eco give %player% 5000\""));

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
