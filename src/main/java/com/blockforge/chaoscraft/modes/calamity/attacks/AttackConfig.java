package com.blockforge.chaoscraft.modes.calamity.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public static final int CURRENT_CONFIG_VERSION = 4;

    private final String attackId;
    private final AttackType type;
    private final int phase;
    private final String modePath; // e.g. "modes/calamity/attacks" or "modes/chain/attacks"

    // Configurable values with defaults
    private double damage = 6.0;
    private double damageRadius = 6.0;
    private int ticksBetweenDamage = 20;
    private int cooldownTicks = 200;
    private int durationTicks = 100;
    private double chance = 1.0; // Equal chance = 1.0 for all by default
    private boolean enabled = true;
    private boolean tracksPlayer = false;

    // Delay before constant damage radius activates (ticks after spawn)
    private int damageDelayTicks = 0;

    // Optional overrides
    private boolean damageOnImpactOnly = false; // For meteors/falling attacks
    private double impactDamage = 16.0;
    private double impactRadius = 7.0;

    // ModelEngine scale: "auto" = scale proportional to damage radius, or a fixed number
    private String modelengineScale = "auto";

    // Follow-AI: drift the attack center slowly toward the nearest player.
    // Distinct from tracksPlayer (which snap-teleports). Opt-in per attack.
    private boolean followAiEnabled = false;
    private double followAiWalkSpeed = 0.06;

    // Skill-based dodge style label, e.g. "Pendulum sweep" / "Drop-from-sky" /
    // "Parry window". Purely informational — written into each per-attack YAML
    // file's top-level header comment block so designers / config editors can
    // see at a glance what the intended dodge interaction is.
    private String designType = "";

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
        damageDelayTicks = section.getInt("damage-delay-ticks", damageDelayTicks);
        damageOnImpactOnly = section.getBoolean("damage-on-impact-only", damageOnImpactOnly);
        impactDamage = section.getDouble("impact-damage", impactDamage);
        impactRadius = section.getDouble("impact-radius", impactRadius);
        modelengineScale = section.getString("modelengine-scale", modelengineScale);
        followAiEnabled = section.getBoolean("follow-ai-enabled", followAiEnabled);
        followAiWalkSpeed = section.getDouble("follow-ai-walk-speed", followAiWalkSpeed);
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
        section.set("damage-delay-ticks", damageDelayTicks);
        section.set("damage-on-impact-only", damageOnImpactOnly);
        section.set("impact-damage", impactDamage);
        section.set("impact-radius", impactRadius);
        section.set("modelengine-scale", modelengineScale);
        section.set("follow-ai-enabled", followAiEnabled);
        section.set("follow-ai-walk-speed", followAiWalkSpeed);
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
            applyAttackComments(config, attackId);
            applyFileHeader(config);
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
        config.setComments("config-version", List.of(
                "Internal version number — do NOT edit manually.",
                "The plugin bumps this when new config keys are added and will auto-upgrade the file."));
        ConfigurationSection section = config.createSection(attackId);
        saveTo(section);
        applyAttackComments(config, attackId);
        applyFileHeader(config);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save attack config: " + attackId);
        }
    }

    /**
     * Apply a top-of-file YAML header that identifies the most-recently-saved
     * attack and its design type. Uses {@link org.bukkit.configuration.file.YamlConfigurationOptions#setHeader(List)}
     * on Paper 1.18+; if that method is unavailable on the running server,
     * falls back to {@link YamlConfiguration#setComments(String, List)} on the
     * top-level {@code config-version} key with the same content.
     *
     * Since multiple attacks share one file, this header may get overwritten
     * each time a sibling attack saves itself. That's fine — the authoritative
     * per-attack identification comment block lives on each attack's section
     * (see {@link #applyAttackComments(YamlConfiguration, String)}).
     */
    private void applyFileHeader(YamlConfiguration config) {
        List<String> header = new ArrayList<>();
        header.add("============================================================");
        header.add("Attack: " + attackId);
        if (designType != null && !designType.isEmpty()) {
            header.add("Design Type: " + designType);
        }
        header.add("============================================================");
        try {
            config.options().setHeader(header);
        } catch (Throwable noSetHeader) {
            // Older API — fall back to first-key comments
            try {
                config.setComments("config-version", header);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * All attacks of the same type share one YAML file:
     *   plugins/ChaosCraft/modes/chain/attacks/blockdisplays.yml
     *   plugins/ChaosCraft/modes/calamity/attacks/environmental.yml
     *   plugins/ChaosCraft/modes/corruption/attacks/boss.yml
     */
    /**
     * Returns the shared config file for this attack's type.
     * Package-visible so AttackRegistry can batch-load files efficiently.
     */
    File getConfigFile(ChaosCraftPlugin plugin) {
        return new File(plugin.getDataFolder(),
                modePath + "/" + getTypePath() + ".yml");
    }

    /**
     * Sets descriptive block comments on every key in this attack's config section.
     * Called whenever the section is written so comments are always present in the file.
     *
     * When this attack has a non-empty design type, that label is included in the
     * section header block so config editors can see the skill-based dodge style
     * (e.g. "Pendulum sweep" / "Drop-from-sky" / "Parry window") at a glance.
     */
    void applyAttackComments(YamlConfiguration config, String id) {
        String p = id + ".";
        List<String> header = new ArrayList<>();
        header.add("============================================================");
        header.add("Attack: " + id);
        if (designType != null && !designType.isEmpty()) {
            header.add("Design Type: " + designType);
        }
        header.add("Type: " + type.name() + "  |  Phase: " + phase);
        header.add("============================================================");
        config.setComments(id, header);
        config.setComments(p + "damage", List.of(
                "Base damage dealt to players within the damage radius each damage tick.",
                "Measured in half-hearts (2.0 = 1 full heart). 20.0 = 10 hearts (instant kill on default HP)."));
        config.setComments(p + "damage-radius", List.of(
                "Radius in blocks around the attack's center where players take damage each tick.",
                "Circular area check — any player within this many blocks of the attack origin is hurt."));
        config.setComments(p + "ticks-between-damage", List.of(
                "How often (in ticks) this attack deals damage while it is active. 20 ticks = 1 second.",
                "Example: 20 = damage once per second, 10 = twice per second, 40 = every 2 seconds."));
        config.setComments(p + "cooldown-ticks", List.of(
                "Minimum ticks that must pass before this attack can spawn again for the same player.",
                "Prevents the same attack from immediately re-spawning after it ends. 200 = 10 seconds."));
        config.setComments(p + "duration-ticks", List.of(
                "How long (in ticks) this attack stays active before it expires and cleans itself up.",
                "100 = 5 seconds, 200 = 10 seconds. Short = quick burst, long = sustained pressure."));
        config.setComments(p + "chance", List.of(
                "Relative weight used when randomly selecting which attack to spawn next for a player.",
                "All attacks default to 1.0 (equal chance). 2.0 = twice as likely, 0.5 = half as likely.",
                "Setting this to 0.0 effectively disables the attack without using the 'enabled' flag."));
        config.setComments(p + "enabled", List.of(
                "Set to false to completely disable this attack — it will never be selected to spawn.",
                "Useful for disabling individual attacks during testing without deleting their config."));
        config.setComments(p + "tracks-player", List.of(
                "If true, this attack continuously moves toward the targeted player's current position.",
                "Creates homing/tracking attacks. If false, the attack spawns at a fixed location."));
        config.setComments(p + "damage-delay-ticks", List.of(
                "Number of ticks after spawning before the continuous damage radius activates.",
                "Gives players time to see the attack and react before it starts hurting them.",
                "0 = damage starts immediately, 20 = 1 second delay, 40 = 2 second delay."));
        config.setComments(p + "damage-on-impact-only", List.of(
                "For falling or projectile-style attacks: if true, damage is only dealt on the initial",
                "impact rather than continuously over the duration. Best for meteor/explosion attacks."));
        config.setComments(p + "impact-damage", List.of(
                "One-time damage dealt on first impact when damage-on-impact-only is true.",
                "This is separate from the ongoing 'damage' field — only applied at the moment of impact."));
        config.setComments(p + "impact-radius", List.of(
                "Radius in blocks of the impact explosion area when damage-on-impact-only is true.",
                "All players within this radius of the impact point receive impact-damage instantly."));
        config.setComments(p + "modelengine-scale", List.of(
                "Scale of the ModelEngine model for this attack. Only applies to MODEL_ENGINE type attacks.",
                "Set to a number (e.g. 1.5) for a fixed scale, or \"auto\" to scale proportionally",
                "to the damage-radius (radius / 5.0, minimum 0.5). Default: 1.0"));
        config.setComments(p + "follow-ai-enabled", List.of(
                "If true, this attack drifts slowly toward the nearest player at follow-ai-walk-speed.",
                "NOT the same as tracks-player (which snap-teleports). Use for chase mechanics that",
                "should feel scary but dodgeable."));
        config.setComments(p + "follow-ai-walk-speed", List.of(
                "Blocks per tick the attack drifts toward the nearest player when follow-ai-enabled.",
                "Player walk speed is ~0.21, so values 0.05-0.15 are typical sub-walk speeds."));
    }

    private String getTypePath() {
        return switch (type) {
            case BLOCK_DISPLAY -> "blockdisplays";
            case ENVIRONMENTAL -> "environmental";
            case BOSS -> "boss";
            case MODEL_ENGINE -> "modelengine";
        };
    }

    /**
     * Copy all configurable values from another AttackConfig into this one.
     * Used when newInstance() creates a fresh attack — the new instance
     * copies the template's YAML-loaded values so config actually takes effect.
     */
    public void copyFrom(AttackConfig other) {
        this.damage = other.damage;
        this.damageRadius = other.damageRadius;
        this.ticksBetweenDamage = other.ticksBetweenDamage;
        this.cooldownTicks = other.cooldownTicks;
        this.durationTicks = other.durationTicks;
        this.chance = other.chance;
        this.enabled = other.enabled;
        this.tracksPlayer = other.tracksPlayer;
        this.damageDelayTicks = other.damageDelayTicks;
        this.damageOnImpactOnly = other.damageOnImpactOnly;
        this.impactDamage = other.impactDamage;
        this.impactRadius = other.impactRadius;
        this.modelengineScale = other.modelengineScale;
        this.followAiEnabled = other.followAiEnabled;
        this.followAiWalkSpeed = other.followAiWalkSpeed;
    }

    // ---- Getters/Setters ----

    public String getAttackId() { return attackId; }

    /**
     * Mode name extracted from the modePath. E.g. "modes/devilsdream/attacks" → "devilsdream".
     * Used to tag spawned entities with "cc:&lt;mode&gt;" for cross-mode entity tracking.
     */
    public String getModeName() {
        if (modePath == null || modePath.isEmpty()) return "unknown";
        String[] parts = modePath.split("/");
        return parts.length >= 2 ? parts[1] : modePath;
    }
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

    public int getDamageDelayTicks() { return damageDelayTicks; }
    public void setDamageDelayTicks(int ticks) { this.damageDelayTicks = ticks; }

    public boolean isDamageOnImpactOnly() { return damageOnImpactOnly; }
    public void setDamageOnImpactOnly(boolean impact) { this.damageOnImpactOnly = impact; }

    public double getImpactDamage() { return impactDamage; }
    public void setImpactDamage(double damage) { this.impactDamage = damage; }

    public double getImpactRadius() { return impactRadius; }

    /** ModelEngine scale: "auto" = proportional to damage radius, or a fixed number string. */
    public String getModelengineScale() { return modelengineScale; }
    public void setModelengineScale(String s) { this.modelengineScale = s; }
    public void setImpactRadius(double radius) { this.impactRadius = radius; }

    public boolean isFollowAiEnabled() { return followAiEnabled; }
    public void setFollowAiEnabled(boolean v) { followAiEnabled = v; }
    public double getFollowAiWalkSpeed() { return followAiWalkSpeed; }
    public void setFollowAiWalkSpeed(double v) { followAiWalkSpeed = v; }

    public String getDesignType() { return designType; }
    public void setDesignType(String type) { this.designType = type == null ? "" : type; }
}
