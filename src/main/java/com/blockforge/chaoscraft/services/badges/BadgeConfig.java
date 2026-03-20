package com.blockforge.chaoscraft.services.badges;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads and manages badge definitions from badges.yml.
 * Supports config-version auto-upgrade.
 */
public class BadgeConfig {

    private static final int CURRENT_CONFIG_VERSION = 1;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private YamlConfiguration config;

    private final Map<String, BadgeDefinition> badges = new LinkedHashMap<>();
    private String grantSound = "minecraft:chaoscraft.badgegrant";

    public BadgeConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "badges.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Version check and auto-upgrade
        int version = config.getInt("config-version", 0);
        if (version < CURRENT_CONFIG_VERSION) {
            config.set("config-version", CURRENT_CONFIG_VERSION);
            try { config.save(configFile); } catch (IOException ignored) {}
        }

        // Load grant sound
        grantSound = config.getString("sound-on-grant", "minecraft:chaoscraft.badgegrant");

        // Load badge definitions
        badges.clear();
        ConfigurationSection badgesSection = config.getConfigurationSection("badges");
        if (badgesSection != null) {
            for (String id : badgesSection.getKeys(false)) {
                ConfigurationSection sec = badgesSection.getConfigurationSection(id);
                if (sec == null) continue;

                String displayName = sec.getString("display-name", id);
                boolean limited = sec.getBoolean("limited", false);
                long expiryDate = sec.getLong("expiry-date", 0L);
                String description = sec.getString("description", "");
                String function = sec.getString("function", "dummy");
                String materialName = sec.getString("icon-material", "PAPER");
                int customModelData = sec.getInt("custom-model-data", 0);

                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    material = Material.PAPER;
                    plugin.getLogger().warning("[Badges] Invalid material '" + materialName + "' for badge " + id + ", defaulting to PAPER.");
                }

                badges.put(id, new BadgeDefinition(id, displayName, limited, expiryDate, description, function, material, customModelData));
            }
        }

        plugin.getLogger().info("[Badges] Loaded " + badges.size() + " badge definitions.");
    }

    /**
     * Save current in-memory config back to disk.
     */
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Badges] Failed to save badges.yml", e);
        }
    }

    /**
     * Add or replace a badge definition in the config and save.
     */
    public void saveBadgeDefinition(BadgeDefinition badge) {
        String path = "badges." + badge.getId();
        config.set(path + ".display-name", badge.getDisplayName());
        config.set(path + ".limited", badge.isLimited());
        config.set(path + ".expiry-date", badge.getExpiryDate());
        config.set(path + ".description", badge.getDescription());
        config.set(path + ".function", badge.getFunction());
        config.set(path + ".icon-material", badge.getIconMaterial().name());
        config.set(path + ".custom-model-data", badge.getCustomModelData());
        badges.put(badge.getId(), badge);
        save();
    }

    /**
     * Remove a badge definition from the config and save.
     */
    public void removeBadgeDefinition(String id) {
        config.set("badges." + id, null);
        badges.remove(id);
        save();
    }

    public Map<String, BadgeDefinition> getBadges() {
        return Collections.unmodifiableMap(badges);
    }

    public BadgeDefinition getBadge(String id) {
        return badges.get(id);
    }

    public String getGrantSound() {
        return grantSound;
    }

    // ========================
    // Default Config Generation
    // ========================

    private void createDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();

        defaults.set("config-version", CURRENT_CONFIG_VERSION);
        defaults.setComments("config-version", List.of(
                "ChaosCraft Badge System Configuration",
                "Internal version number - do NOT edit manually.",
                "The plugin bumps this when new config keys are added."));

        defaults.set("sound-on-grant", "minecraft:chaoscraft.badgegrant");
        defaults.setComments("sound-on-grant", List.of(
                "Sound played when a player earns a badge.",
                "Set to empty string to disable."));

        defaults.setComments("badges", List.of(
                "Badge definitions. Each key is the badge ID.",
                "display-name: Name shown in GUI. Supports hex colors with &# prefix (e.g. &#FF5555).",
                "limited: If true, this badge is time-limited (event/seasonal).",
                "expiry-date: Epoch millis when the badge expires. 0 = no expiry.",
                "description: Lore text shown in the badge GUI.",
                "function: How this badge is earned. See docs for function types.",
                "icon-material: Vanilla material for the badge icon in the GUI.",
                "custom-model-data: Custom model data for resource pack icons. 0 = none."));

        // Default badges
        addDefault(defaults, "calamity_event_1", "&#FF0000Calamity Event I", false, 0,
                "Earned during the first Calamity event.", "dummy", "WITHER_SKELETON_SKULL", 0);
        addDefault(defaults, "calamity_event_2", "&#FF3300Calamity Event II", false, 0,
                "Earned during the second Calamity event.", "dummy", "DRAGON_HEAD", 0);
        addDefault(defaults, "1x1x1x1", "&#AA00FF1x1x1x1", false, 0,
                "A mysterious badge from an unknown entity.", "dummy", "COMMAND_BLOCK", 0);
        addDefault(defaults, "blue_moon_event_2023", "&#5555FFBlue Moon 2023", true, 0,
                "Earned during the Blue Moon event of 2023.", "dummy", "BLUE_ICE", 0);
        addDefault(defaults, "hallows_2023", "&#FF8800Hallows 2023", true, 0,
                "Earned during the Halloween event of 2023.", "dummy", "CARVED_PUMPKIN", 0);
        addDefault(defaults, "hallows_2024", "&#FF9900Hallows 2024", true, 0,
                "Earned during the Halloween event of 2024.", "dummy", "JACK_O_LANTERN", 0);
        addDefault(defaults, "anniversary_2024", "&#FFD700Anniversary 2024", true, 0,
                "Earned during the server anniversary event of 2024.", "dummy", "CAKE", 0);
        addDefault(defaults, "april_fools_2025", "&#FF55FFApril Fools 2025", true, 0,
                "Earned during April Fools 2025.", "dummy", "TNT", 0);
        addDefault(defaults, "tutorial", "&#55FF55Tutorial", false, 0,
                "Completed the server tutorial.", "dummy", "BOOK", 0);
        addDefault(defaults, "freezing_ice", "&#AAEEFFFreezing Ice", false, 0,
                "Survived the Freezing Ice mode.", "dummy", "PACKED_ICE", 0);
        addDefault(defaults, "blue_moon", "&#5577FFBlue Moon", false, 0,
                "Survived the Blue Moon mode.", "dummy", "LAPIS_LAZULI", 0);
        addDefault(defaults, "fish", "&#00AAFFSomething Fishy", false, 0,
                "There's something fishy going on...", "dummy", "TROPICAL_FISH", 0);
        addDefault(defaults, "sonic", "&#0055FFSonic", false, 0,
                "Gotta go fast!", "dummy", "FEATHER", 0);
        addDefault(defaults, "infested", "&#556600Infested", false, 0,
                "Survived an infestation.", "dummy", "SPIDER_EYE", 0);
        addDefault(defaults, "chef", "&#FFAA00Master Chef", false, 0,
                "A culinary master of chaos.", "dummy", "GOLDEN_APPLE", 0);
        addDefault(defaults, "devils_dream", "&#CC0000Devil's Dream", false, 0,
                "Survived the Devil's Dream mode.", "dummy", "BLAZE_POWDER", 0);
        addDefault(defaults, "corrupted_corruption", "&#880088Corrupted Corruption", false, 0,
                "Survived the Corrupted Corruption mode.", "dummy", "CRYING_OBSIDIAN", 0);
        addDefault(defaults, "doom", "&#880000Doom", false, 0,
                "Faced the doom and lived.", "dummy", "NETHERITE_SWORD", 0);
        addDefault(defaults, "chain", "&#AAAAAAChain", false, 0,
                "Survived the Chain mode.", "dummy", "CHAIN", 0);
        addDefault(defaults, "creeper_infestation", "&#00CC00Creeper Infestation", false, 0,
                "Survived a creeper infestation.", "dummy", "CREEPER_HEAD", 0);
        addDefault(defaults, "unpredictable_randomness", "&#FFFF00Unpredictable Randomness", false, 0,
                "Survived pure randomness.", "dummy", "CHORUS_FRUIT", 0);
        addDefault(defaults, "crazy_minecrafter", "&#FF5555Crazy Minecrafter", false, 0,
                "A truly unhinged individual.", "dummy", "DIAMOND_PICKAXE", 0);
        addDefault(defaults, "seer", "&#AA00AASeer", false, 0,
                "Saw beyond the veil.", "dummy", "ENDER_EYE", 0);
        addDefault(defaults, "neko", "&#FFAAFFNeko", false, 0,
                "Nyaa~", "dummy", "STRING", 0);
        addDefault(defaults, "fallen", "&#555555Fallen", false, 0,
                "Touched by the Fallen.", "dummy", "WITHER_ROSE", 0);
        addDefault(defaults, "bruh", "&#AAAAAABruh", false, 0,
                "Bruh moment.", "dummy", "DEAD_BUSH", 0);
        addDefault(defaults, "ghost", "&#DDDDDDGhost", false, 0,
                "Became one with the spirits.", "dummy", "PHANTOM_MEMBRANE", 0);
        addDefault(defaults, "insanity_rampage", "&#FF0055Insanity Rampage", false, 0,
                "Lost all sanity and survived.", "dummy", "REDSTONE", 0);
        addDefault(defaults, "total_chaos", "&#FF0000Total Chaos", false, 0,
                "Survived total chaos.", "dummy", "FIRE_CHARGE", 0);
        addDefault(defaults, "chaotic_determination", "&#FF3300Chaotic Determination", false, 0,
                "Survived Chaotic Determination.", "dummy", "NETHER_STAR", 0);
        addDefault(defaults, "nightmare", "&#330033Nightmare", false, 0,
                "Survived the Nightmare.", "dummy", "BLACK_BED", 0);
        addDefault(defaults, "lost_moon", "&#8888FFLost Moon", false, 0,
                "Wandered the Lost Moon.", "dummy", "END_STONE", 0);
        addDefault(defaults, "bloody_eclipse", "&#990000Bloody Eclipse", false, 0,
                "Survived the Bloody Eclipse.", "dummy", "REDSTONE_BLOCK", 0);
        addDefault(defaults, "armageddon", "&#FF4400Armageddon", false, 0,
                "Witnessed Armageddon.", "dummy", "MAGMA_BLOCK", 0);
        addDefault(defaults, "oblivion", "&#220022Oblivion", false, 0,
                "Faced Oblivion and returned.", "dummy", "OBSIDIAN", 0);
        addDefault(defaults, "god_eater", "&#FFD700God Eater", false, 0,
                "Devoured a god.", "dummy", "GOLDEN_APPLE", 0);
        addDefault(defaults, "lava_rise", "&#FF5500Lava Rise", false, 0,
                "Survived the rising lava.", "dummy", "LAVA_BUCKET", 0);

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Badges] Failed to save default badges.yml", e);
        }
    }

    private void addDefault(YamlConfiguration cfg, String id, String displayName, boolean limited,
                            long expiryDate, String description, String function, String material, int cmd) {
        String path = "badges." + id;
        cfg.set(path + ".display-name", displayName);
        cfg.set(path + ".limited", limited);
        cfg.set(path + ".expiry-date", expiryDate);
        cfg.set(path + ".description", description);
        cfg.set(path + ".function", function);
        cfg.set(path + ".icon-material", material);
        cfg.set(path + ".custom-model-data", cmd);
        cfg.setComments(path, List.of("Badge: " + id));
    }
}
