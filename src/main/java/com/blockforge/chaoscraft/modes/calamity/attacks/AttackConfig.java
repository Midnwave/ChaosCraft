package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Loads and manages per-attack YAML configuration.
 * Each attack has a config section with configurable values:
 * damage, damageRadius, ticksBetweenDamage, cooldown, duration,
 * chance, enabled, tracksPlayer.
 *
 * Config stored at: plugins/ChaosCraft/modes/calamity/attacks/{type}/{id}.yml
 * Or as a section in a phase-level file: plugins/ChaosCraft/modes/calamity/phases/phase{n}_{type}.yml
 */
public class AttackConfig {

    /**
     * Bump this whenever a new config key is added or defaults change.
     * On load, if the file version is lower, the file is re-saved with new keys
     * while preserving user-edited values.
     */
    public static final int CURRENT_CONFIG_VERSION = 1;

    private final String attackId;
    private final AttackType type;
    private final int phase;
    private final String modePath; // e.g. "modes/calamity/attacks" or "modes/chain/attacks"

    // Configurable values with defaults
    private double damage = 4.0;
    private double damageRadius = 3.0;
    private int ticksBetweenDamage = 20;
    private int cooldownTicks = 200;
    private int durationTicks = 100;
    private double chance = 1.0; // Equal chance = 1.0 for all by default
    private boolean enabled = true;
    private boolean tracksPlayer = false;

    // Optional overrides
    private boolean damageOnImpactOnly = false; // For meteors/falling attacks
    private double impactDamage = 8.0;
    private double impactRadius = 4.0;

    public AttackConfig(String attackId, AttackType type, int phase) {
        this(attackId, type, phase, "modes/calamity/attacks");
    }

    public AttackConfig(String attackId, AttackType type, int phase, String modePath) {
        this.attackId = attackId;
        this.type = type;
        this.phase = phase;
        this.modePath = modePath;
    }

    /**
     * Load values from a ConfigurationSection.
     */
    public void loadFrom(ConfigurationSection section) {
        if (section == null) return;
        damage = section.getDouble("damage", damage);
        damageRadius = section.getDouble("damage-radius", damageRadius);
        ticksBetweenDamage = section.getInt("ticks-between-damage", ticksBetweenDamage);
        cooldownTicks = section.getInt("cooldown-ticks", cooldownTicks);
        durationTicks = section.getInt("duration-ticks", durationTicks);
        chance = section.getDouble("chance", chance);
        enabled = section.getBoolean("enabled", enabled);
        tracksPlayer = section.getBoolean("tracks-player", tracksPlayer);
        damageOnImpactOnly = section.getBoolean("damage-on-impact-only", damageOnImpactOnly);
        impactDamage = section.getDouble("impact-damage", impactDamage);
        impactRadius = section.getDouble("impact-radius", impactRadius);
    }

    /**
     * Save values to a ConfigurationSection.
     */
    public void saveTo(ConfigurationSection section) {
        section.set("damage", damage);
        section.set("damage-radius", damageRadius);
        section.set("ticks-between-damage", ticksBetweenDamage);
        section.set("cooldown-ticks", cooldownTicks);
        section.set("duration-ticks", durationTicks);
        section.set("chance", chance);
        section.set("enabled", enabled);
        section.set("tracks-player", tracksPlayer);
        section.set("damage-on-impact-only", damageOnImpactOnly);
        section.set("impact-damage", impactDamage);
        section.set("impact-radius", impactRadius);
    }

    /**
     * Load this attack's config from the shared type file.
     * All attacks of the same type share one YAML file, each as a section:
     *   plugins/ChaosCraft/{modePath}/blockdisplays.yml
     *   plugins/ChaosCraft/{modePath}/environmental.yml
     *   plugins/ChaosCraft/{modePath}/boss.yml
     *
     * If the attack's section doesn't exist yet, it's created with defaults.
     * If config-version is outdated, the file is re-saved with new keys.
     */
    public void loadFromFile(ChaosCraftPlugin plugin) {
        File file = getConfigFile(plugin);
        file.getParentFile().mkdirs();

        YamlConfiguration config;
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            config = new YamlConfiguration();
            config.set("config-version", CURRENT_CONFIG_VERSION);
        }

        ConfigurationSection section = config.getConfigurationSection(attackId);
        if (section != null) {
            loadFrom(section);
        } else {
            // Attack not in file yet — add it with defaults
            saveToFile(plugin);
            return;
        }

        // Config version check
        int fileVersion = config.getInt("config-version", 0);
        if (fileVersion < CURRENT_CONFIG_VERSION) {
            plugin.debug("[AttackConfig] Upgrading " + getTypePath() + ".yml from v" + fileVersion + " to v" + CURRENT_CONFIG_VERSION);
            config.set("config-version", CURRENT_CONFIG_VERSION);
            // Re-save this attack's section with any new keys
            ConfigurationSection updated = config.createSection(attackId);
            saveTo(updated);
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save attack config file: " + file.getName());
            }
        }
    }

    /**
     * Save this attack's config section to the shared type file.
     */
    public void saveToFile(ChaosCraftPlugin plugin) {
        File file = getConfigFile(plugin);
        file.getParentFile().mkdirs();

        YamlConfiguration config;
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            config = new YamlConfiguration();
        }

        config.set("config-version", CURRENT_CONFIG_VERSION);
        ConfigurationSection section = config.createSection(attackId);
        saveTo(section);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save attack config: " + attackId);
        }
    }

    /**
     * All attacks of the same type share one YAML file:
     *   plugins/ChaosCraft/modes/chain/attacks/blockdisplays.yml
     *   plugins/ChaosCraft/modes/calamity/attacks/environmental.yml
     *   plugins/ChaosCraft/modes/corruption/attacks/boss.yml
     */
    private File getConfigFile(ChaosCraftPlugin plugin) {
        return new File(plugin.getDataFolder(),
                modePath + "/" + getTypePath() + ".yml");
    }

    private String getTypePath() {
        return switch (type) {
            case BLOCK_DISPLAY -> "blockdisplays";
            case ENVIRONMENTAL -> "environmental";
            case BOSS -> "boss";
        };
    }

    // ---- Getters/Setters ----

    public String getAttackId() { return attackId; }
    public AttackType getType() { return type; }
    public int getPhase() { return phase; }

    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }

    public double getDamageRadius() { return damageRadius; }
    public void setDamageRadius(double damageRadius) { this.damageRadius = damageRadius; }

    public int getTicksBetweenDamage() { return ticksBetweenDamage; }
    public void setTicksBetweenDamage(int ticks) { this.ticksBetweenDamage = ticks; }

    public int getCooldownTicks() { return cooldownTicks; }
    public void setCooldownTicks(int ticks) { this.cooldownTicks = ticks; }

    public int getDurationTicks() { return durationTicks; }
    public void setDurationTicks(int ticks) { this.durationTicks = ticks; }

    public double getChance() { return chance; }
    public void setChance(double chance) { this.chance = chance; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean tracksPlayer() { return tracksPlayer; }
    public void setTracksPlayer(boolean tracks) { this.tracksPlayer = tracks; }

    public boolean isDamageOnImpactOnly() { return damageOnImpactOnly; }
    public void setDamageOnImpactOnly(boolean impact) { this.damageOnImpactOnly = impact; }

    public double getImpactDamage() { return impactDamage; }
    public void setImpactDamage(double damage) { this.impactDamage = damage; }

    public double getImpactRadius() { return impactRadius; }
    public void setImpactRadius(double radius) { this.impactRadius = radius; }
}
