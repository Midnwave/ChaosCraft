package com.blockforge.chaoscraft.services.shop;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Configuration loader for the shop system.
 * Loads shop.yml (global settings) and per-category configs from shops/ directory.
 */
public class ShopConfig {

    private static final int CURRENT_CONFIG_VERSION = 1;

    private final ChaosCraftPlugin plugin;
    private final File configFile;
    private final File shopsDir;
    private YamlConfiguration config;

    // Global settings
    private String musicSoundId = "";
    private boolean musicLoop = true;
    private int musicDurationTicks = 6000;
    private String soundPurchase = "minecraft:chaoscraft.purchase";
    private String soundSell = "minecraft:chaoscraft.sell";
    private String soundError = "minecraft:entity.villager.no";
    private String soundOpen = "minecraft:block.chest.open";
    private String rankPlaceholder = "%luckperms_prefix%";

    // Categories loaded from shops/ directory
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();

    public ShopConfig(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "shop.yml");
        this.shopsDir = new File(plugin.getDataFolder(), "shops");
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaults();
        }
        if (!shopsDir.exists()) {
            shopsDir.mkdirs();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Version check and auto-upgrade
        int version = config.getInt("config-version", 0);
        if (version < CURRENT_CONFIG_VERSION) {
            upgrade(version);
            config.set("config-version", CURRENT_CONFIG_VERSION);
            try { config.save(configFile); } catch (IOException ignored) {}
        }

        // Load global settings
        musicSoundId = config.getString("music.sound-id", "");
        musicLoop = config.getBoolean("music.loop", true);
        musicDurationTicks = config.getInt("music.duration-ticks", 6000);
        soundPurchase = config.getString("sounds.purchase", "minecraft:chaoscraft.purchase");
        soundSell = config.getString("sounds.sell", "minecraft:chaoscraft.sell");
        soundError = config.getString("sounds.error", "minecraft:entity.villager.no");
        soundOpen = config.getString("sounds.open", "minecraft:block.chest.open");
        rankPlaceholder = config.getString("rank-placeholder", "%luckperms_prefix%");

        // Load category configs from shops/ directory
        loadCategories();

        plugin.getLogger().info("[Shop] Config loaded. " + categories.size() + " categories.");
    }

    private void upgrade(int fromVersion) {
        // Future version upgrades go here
        // if (fromVersion < 2) { ... add new keys ... }
    }

    private void createDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", CURRENT_CONFIG_VERSION);
        defaults.setComments("config-version", List.of(
                "Internal version number - do NOT edit manually.",
                "The plugin bumps this when new config keys are added."));

        defaults.set("music.sound-id", "");
        defaults.setComments("music.sound-id", List.of(
                "Custom sound ID to play when the shop is opened. Leave empty to disable."));

        defaults.set("music.loop", true);
        defaults.setComments("music.loop", List.of(
                "Whether the shop music should loop while the shop is open."));

        defaults.set("music.duration-ticks", 6000);
        defaults.setComments("music.duration-ticks", List.of(
                "Duration of the music track in ticks (20 ticks = 1 second). Used for looping."));

        defaults.set("sounds.purchase", "minecraft:chaoscraft.purchase");
        defaults.setComments("sounds.purchase", List.of(
                "Sound played when a player successfully purchases an item."));

        defaults.set("sounds.sell", "minecraft:chaoscraft.sell");
        defaults.setComments("sounds.sell", List.of(
                "Sound played when a player successfully sells an item."));

        defaults.set("sounds.error", "minecraft:entity.villager.no");
        defaults.setComments("sounds.error", List.of(
                "Sound played when a transaction fails (not enough money, requirements, etc)."));

        defaults.set("sounds.open", "minecraft:block.chest.open");
        defaults.setComments("sounds.open", List.of(
                "Sound played when the shop GUI is opened."));

        defaults.set("rank-placeholder", "%luckperms_prefix%");
        defaults.setComments("rank-placeholder", List.of(
                "PlaceholderAPI placeholder used to display the player's rank in the shop GUI.",
                "Requires PlaceholderAPI and a permissions plugin (e.g. LuckPerms)."));

        try {
            defaults.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Shop] Failed to save default shop.yml", e);
        }
    }

    private void loadCategories() {
        categories.clear();
        if (!shopsDir.exists() || !shopsDir.isDirectory()) return;

        File[] files = shopsDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            try {
                YamlConfiguration catConfig = YamlConfiguration.loadConfiguration(file);
                String id = file.getName().replace(".yml", "").toLowerCase(Locale.ROOT);

                String displayName = catConfig.getString("display-name", id);
                String iconMaterial = catConfig.getString("icon-material", "CHEST");
                int customModelData = catConfig.getInt("custom-model-data", 0);
                int slot = catConfig.getInt("slot", -1);

                ShopCategory category = new ShopCategory(id, displayName, iconMaterial, customModelData, slot);

                // Load items
                ConfigurationSection itemsSection = catConfig.getConfigurationSection("items");
                if (itemsSection != null) {
                    for (String itemKey : itemsSection.getKeys(false)) {
                        ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                        if (itemSection == null) continue;

                        String material = itemSection.getString("material", "STONE");
                        double buyPrice = itemSection.getDouble("buy-price", -1);
                        double sellPrice = itemSection.getDouble("sell-price", -1);
                        int killsReq = itemSection.getInt("kills-required", 0);
                        int sKillsReq = itemSection.getInt("skills-required", 0);
                        int survivalsReq = itemSection.getInt("survivals-required", 0);
                        String badge = itemSection.getString("badge-required", null);
                        List<String> lore = itemSection.getStringList("shop-lore");
                        int itemSlot = itemSection.getInt("slot", -1);
                        String itemDisplayName = itemSection.getString("display-name", null);

                        ShopItem shopItem = new ShopItem(
                                material, buyPrice, sellPrice,
                                killsReq, sKillsReq, survivalsReq,
                                badge, lore, itemSlot, itemDisplayName
                        );
                        category.addItem(shopItem);
                    }
                }

                categories.put(id, category);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Shop] Failed to load category file: " + file.getName(), e);
            }
        }
    }

    // ========================
    // Category CRUD (save to disk)
    // ========================

    public void saveCategory(ShopCategory category) {
        File file = new File(shopsDir, category.getId() + ".yml");
        YamlConfiguration catConfig = new YamlConfiguration();

        catConfig.set("display-name", category.getDisplayName());
        catConfig.set("icon-material", category.getIconMaterial());
        catConfig.set("custom-model-data", category.getCustomModelData());
        catConfig.set("slot", category.getSlot());

        List<ShopItem> items = category.getItems();
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            String key = "items.item_" + i;
            catConfig.set(key + ".material", item.getMaterialName());
            catConfig.set(key + ".buy-price", item.getBuyPrice());
            catConfig.set(key + ".sell-price", item.getSellPrice());
            catConfig.set(key + ".kills-required", item.getKillsRequired());
            catConfig.set(key + ".skills-required", item.getSKillsRequired());
            catConfig.set(key + ".survivals-required", item.getSurvivalsRequired());
            if (item.getBadgeRequired() != null) {
                catConfig.set(key + ".badge-required", item.getBadgeRequired());
            }
            if (!item.getShopLore().isEmpty()) {
                catConfig.set(key + ".shop-lore", item.getShopLore());
            }
            catConfig.set(key + ".slot", item.getSlot());
            if (item.getDisplayName() != null) {
                catConfig.set(key + ".display-name", item.getDisplayName());
            }
        }

        try {
            catConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Shop] Failed to save category: " + category.getId(), e);
        }

        categories.put(category.getId(), category);
    }

    public void deleteCategory(String id) {
        categories.remove(id);
        File file = new File(shopsDir, id + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    // ========================
    // Getters
    // ========================

    public String getMusicSoundId() { return musicSoundId; }
    public boolean isMusicLoop() { return musicLoop; }
    public int getMusicDurationTicks() { return musicDurationTicks; }
    public String getSoundPurchase() { return soundPurchase; }
    public String getSoundSell() { return soundSell; }
    public String getSoundError() { return soundError; }
    public String getSoundOpen() { return soundOpen; }
    public String getRankPlaceholder() { return rankPlaceholder; }

    public Map<String, ShopCategory> getCategoriesMap() {
        return Collections.unmodifiableMap(categories);
    }

    public ShopCategory getCategory(String id) {
        return categories.get(id.toLowerCase(Locale.ROOT));
    }

    public List<ShopCategory> getCategories() {
        return new ArrayList<>(categories.values());
    }
}
