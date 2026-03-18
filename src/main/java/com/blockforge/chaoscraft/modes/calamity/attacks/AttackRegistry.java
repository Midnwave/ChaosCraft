package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

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
                return attack.newInstance();
            }
        }

        // Fallback: last enabled
        return enabled.get(enabled.size() - 1).newInstance();
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
     */
    public void reloadConfigs() {
        for (AbstractAttack attack : templates.values()) {
            attack.getConfig().loadFromFile(plugin);
        }
        plugin.getLogger().info("[AttackRegistry] Reloaded configs for " + templates.size() + " attacks.");
    }

    /**
     * Save all attack configs to YAML files (creates defaults).
     */
    public void saveConfigs() {
        for (AbstractAttack attack : templates.values()) {
            attack.getConfig().saveToFile(plugin);
        }
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
