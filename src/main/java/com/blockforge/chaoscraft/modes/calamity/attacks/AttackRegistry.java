package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry for all Calamity attacks.
 * Attacks are registered at plugin startup and looked up by
 * phase, type, id, and boss name.
 *
 * Provides:
 * - Registration of attack templates (prototype pattern)
 * - Lookup by phase + type + id
 * - Random selection for spawning (equal chance default, configurable)
 * - Tab completion lists for commands
 * - Config reload for all registered attacks
 */
public class AttackRegistry {

    private final ChaosCraftPlugin plugin;

    // All registered attack templates: key = "phase:type:id"
    private final Map<String, AbstractAttack> templates = new LinkedHashMap<>();

    // Index by phase for quick lookup
    private final Map<Integer, List<AbstractAttack>> byPhase = new HashMap<>();

    // Index by phase + type
    private final Map<String, List<AbstractAttack>> byPhaseType = new HashMap<>();

    // Index by boss name (for boss attacks)
    private final Map<String, List<AbstractAttack>> byBoss = new HashMap<>();

    public AttackRegistry(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    // Registration
    // ========================

    /**
     * Register an attack template. The template is used as a prototype —
     * newInstance() is called each time the attack is spawned.
     */
    public void register(AbstractAttack attack) {
        String key = buildKey(attack.getPhase(), attack.getType(), attack.getId());
        templates.put(key, attack);

        // Index by phase
        byPhase.computeIfAbsent(attack.getPhase(), k -> new ArrayList<>()).add(attack);

        // Index by phase + type
        String ptKey = attack.getPhase() + ":" + attack.getType().name();
        byPhaseType.computeIfAbsent(ptKey, k -> new ArrayList<>()).add(attack);

        // Index by boss (for boss attacks)
        if (attack instanceof BossAttack bossAttack) {
            byBoss.computeIfAbsent(bossAttack.getBossName().toLowerCase(), k -> new ArrayList<>()).add(attack);
        }

        plugin.debug("[AttackRegistry] Registered: " + key);
    }

    /**
     * Register multiple attacks at once.
     */
    public void registerAll(AbstractAttack... attacks) {
        for (AbstractAttack attack : attacks) {
            register(attack);
        }
    }

    // ========================
    // Lookup
    // ========================

    /**
     * Get a specific attack template by phase, type, and id.
     */
    public AbstractAttack get(int phase, AttackType type, String id) {
        return templates.get(buildKey(phase, type, id));
    }

    /**
     * Get all attacks for a given phase.
     */
    public List<AbstractAttack> getByPhase(int phase) {
        return byPhase.getOrDefault(phase, Collections.emptyList());
    }

    /**
     * Get all attacks of a specific type in a phase.
     */
    public List<AbstractAttack> getByPhaseAndType(int phase, AttackType type) {
        String key = phase + ":" + type.name();
        return byPhaseType.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Get all attacks for a specific boss.
     */
    public List<AbstractAttack> getByBoss(String bossName) {
        return byBoss.getOrDefault(bossName.toLowerCase(), Collections.emptyList());
    }

    /**
     * Get all registered attack templates.
     */
    public Collection<AbstractAttack> getAll() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public int size() {
        return templates.size();
    }

    // ========================
    // Random selection (for spawning)
    // ========================

    /**
     * Select a random enabled attack from a phase and type.
     * Uses configured chance weights (default: equal chance = 1.0 for all).
     *
     * @return A new instance of the selected attack, or null if none available
     */
    public AbstractAttack selectRandom(int phase, AttackType type) {
        List<AbstractAttack> candidates = getByPhaseAndType(phase, type);
        return selectWeighted(candidates);
    }

    /**
     * Select a random enabled attack from a phase (any type).
     */
    public AbstractAttack selectRandomFromPhase(int phase) {
        List<AbstractAttack> candidates = getByPhase(phase);
        return selectWeighted(candidates);
    }

    /**
     * Select a random enabled boss attack for a specific boss.
     */
    public AbstractAttack selectRandomBossAttack(String bossName) {
        List<AbstractAttack> candidates = getByBoss(bossName);
        return selectWeighted(candidates);
    }

    private AbstractAttack selectWeighted(List<AbstractAttack> candidates) {
        if (candidates.isEmpty()) return null;

        // Filter to enabled only
        List<AbstractAttack> enabled = candidates.stream()
                .filter(a -> a.getConfig().isEnabled())
                .toList();
        if (enabled.isEmpty()) return null;

        // Calculate total weight
        double totalWeight = enabled.stream()
                .mapToDouble(a -> a.getConfig().getChance())
                .sum();
        if (totalWeight <= 0) return null;

        // Weighted random selection
        double roll = Math.random() * totalWeight;
        double cumulative = 0;
        for (AbstractAttack attack : enabled) {
            cumulative += attack.getConfig().getChance();
            if (roll < cumulative) {
                AbstractAttack instance = attack.newInstance();
                instance.getConfig().copyFrom(attack.getConfig());
                return instance;
            }
        }

        // Fallback: last enabled
        AbstractAttack last = enabled.get(enabled.size() - 1);
        AbstractAttack instance = last.newInstance();
        instance.getConfig().copyFrom(last.getConfig());
        return instance;
    }

    // ========================
    // Tab completion helpers
    // ========================

    /**
     * Get all attack IDs for a phase and type (for tab completion).
     */
    public List<String> getIds(int phase, AttackType type) {
        return getByPhaseAndType(phase, type).stream()
                .map(AbstractAttack::getId)
                .collect(Collectors.toList());
    }

    /**
     * Get all attack IDs for a boss (for tab completion).
     */
    public List<String> getBossAttackIds(String bossName) {
        return getByBoss(bossName).stream()
                .map(AbstractAttack::getId)
                .collect(Collectors.toList());
    }

    /**
     * Get all phases that have registered attacks.
     */
    public List<String> getPhases() {
        return byPhase.keySet().stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Get all boss names that have registered attacks.
     */
    public List<String> getBossNames() {
        return new ArrayList<>(byBoss.keySet());
    }

    // ========================
    // Config reload
    // ========================

    /**
     * Reload all attack configs from YAML files.
     * Batches by shared file so each YAML file is parsed from disk only ONCE
     * instead of once per attack (critical for 1000+ attacks).
     */
    public void reloadConfigs() {
        long totalStart = System.currentTimeMillis();

        // Group attacks by their shared config file (e.g., blockdisplays.yml)
        Map<File, List<AbstractAttack>> byFile = new LinkedHashMap<>();
        for (AbstractAttack attack : templates.values()) {
            File file = attack.getConfig().getConfigFile(plugin);
            byFile.computeIfAbsent(file, k -> new ArrayList<>()).add(attack);
        }

        int totalLoaded = 0;
        for (Map.Entry<File, List<AbstractAttack>> entry : byFile.entrySet()) {
            long fileStart = System.currentTimeMillis();
            File file = entry.getKey();
            List<AbstractAttack> attacks = entry.getValue();

            file.getParentFile().mkdirs();

            YamlConfiguration yaml;
            boolean needsSave = false;
            if (file.exists()) {
                yaml = YamlConfiguration.loadConfiguration(file);
            } else {
                yaml = new YamlConfiguration();
                yaml.set("config-version", AttackConfig.CURRENT_CONFIG_VERSION);
                yaml.setComments("config-version", List.of(
                        "Internal version number — do NOT edit manually.",
                        "The plugin bumps this when new config keys are added and will auto-upgrade the file."));
                needsSave = true;
            }

            // Version check — once per file, not per attack
            int fileVersion = yaml.getInt("config-version", 0);
            boolean versionUpgrade = fileVersion < AttackConfig.CURRENT_CONFIG_VERSION;
            if (versionUpgrade) {
                yaml.set("config-version", AttackConfig.CURRENT_CONFIG_VERSION);
                needsSave = true;
            }

            for (AbstractAttack attack : attacks) {
                AttackConfig cfg = attack.getConfig();
                String id = cfg.getAttackId();
                ConfigurationSection section = yaml.getConfigurationSection(id);
                if (section != null) {
                    cfg.loadFrom(section);
                    if (versionUpgrade) {
                        // Re-save section to pick up any new keys
                        ConfigurationSection updated = yaml.createSection(id);
                        cfg.saveTo(updated);
                        cfg.applyAttackComments(yaml, id);
                    }
                } else {
                    // Attack not in file yet — create section with defaults
                    ConfigurationSection newSection = yaml.createSection(id);
                    cfg.saveTo(newSection);
                    cfg.applyAttackComments(yaml, id);
                    needsSave = true;
                }
            }

            if (needsSave) {
                try {
                    yaml.save(file);
                } catch (IOException e) {
                    plugin.getLogger().severe("[AttackRegistry] Failed to save: " + file.getName() + " — " + e.getMessage());
                }
            }

            long fileMs = System.currentTimeMillis() - fileStart;
            totalLoaded += attacks.size();
            plugin.getLogger().info("[AttackRegistry] Loaded " + file.getName()
                    + " (" + attacks.size() + " attacks) in " + fileMs + "ms");
        }

        long totalMs = System.currentTimeMillis() - totalStart;
        plugin.getLogger().info("[AttackRegistry] Reloaded " + totalLoaded + " attacks from "
                + byFile.size() + " files in " + totalMs + "ms");
    }

    /**
     * Save all attack configs to YAML files (creates defaults).
     * Batched by file to avoid re-reading the same file per attack.
     */
    public void saveConfigs() {
        long totalStart = System.currentTimeMillis();

        Map<File, List<AbstractAttack>> byFile = new LinkedHashMap<>();
        for (AbstractAttack attack : templates.values()) {
            File file = attack.getConfig().getConfigFile(plugin);
            byFile.computeIfAbsent(file, k -> new ArrayList<>()).add(attack);
        }

        for (Map.Entry<File, List<AbstractAttack>> entry : byFile.entrySet()) {
            File file = entry.getKey();
            file.getParentFile().mkdirs();

            YamlConfiguration yaml;
            if (file.exists()) {
                yaml = YamlConfiguration.loadConfiguration(file);
            } else {
                yaml = new YamlConfiguration();
            }
            yaml.set("config-version", AttackConfig.CURRENT_CONFIG_VERSION);
            yaml.setComments("config-version", List.of(
                    "Internal version number — do NOT edit manually.",
                    "The plugin bumps this when new config keys are added and will auto-upgrade the file."));

            for (AbstractAttack attack : entry.getValue()) {
                AttackConfig cfg = attack.getConfig();
                ConfigurationSection section = yaml.createSection(cfg.getAttackId());
                cfg.saveTo(section);
                cfg.applyAttackComments(yaml, cfg.getAttackId());
            }

            try {
                yaml.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("[AttackRegistry] Failed to save: " + file.getName());
            }
        }

        long totalMs = System.currentTimeMillis() - totalStart;
        plugin.getLogger().info("[AttackRegistry] Saved " + templates.size() + " attacks to "
                + byFile.size() + " files in " + totalMs + "ms");
    }

    // ========================
    // Internal
    // ========================

    private String buildKey(int phase, AttackType type, String id) {
        return phase + ":" + type.name() + ":" + id;
    }

    public void clear() {
        templates.clear();
        byPhase.clear();
        byPhaseType.clear();
        byBoss.clear();
    }
}
