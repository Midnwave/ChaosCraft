package com.blockforge.chaoscraft.services.settings;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages per-player settings with persistence and scripted side-effects.
 * Settings are defined in SettingsConfig.yml and values stored in settings.yml.
 */
public class SettingsService {

    private final ChaosCraftPlugin plugin;
    private SettingsConfig config;
    private final Map<UUID, Map<String, String>> playerSettings = new HashMap<>();
    private final Map<UUID, StatusMessage> statusMessages = new HashMap<>();
    private File settingsFile;
    private YamlConfiguration settingsData;

    public SettingsService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadConfig();
        loadPlayerSettings();
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "SettingsConfig.yml");
        if (!configFile.exists()) {
            plugin.saveResource("SettingsConfig.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        config = new SettingsConfig();
        config.load(yaml);
        plugin.getLogger().info("Loaded settings configuration with " + config.getSettingNames().size() + " settings.");
    }

    private void loadPlayerSettings() {
        settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        if (!settingsFile.exists()) {
            try { settingsFile.createNewFile(); }
            catch (IOException e) {
                plugin.getLogger().severe("Failed to create settings.yml:");
                e.printStackTrace();
            }
        }

        settingsData = YamlConfiguration.loadConfiguration(settingsFile);
        playerSettings.clear();

        for (String key : settingsData.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Map<String, String> settings = new HashMap<>();
                var section = settingsData.getConfigurationSection(key);
                if (section != null) {
                    for (String settingName : section.getKeys(false)) {
                        String value = settingsData.getString(key + "." + settingName);
                        settings.put(settingName.toLowerCase(), value);
                    }
                }
                playerSettings.put(uuid, settings);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in settings.yml: " + key);
            }
        }

        plugin.getLogger().info("Loaded settings for " + playerSettings.size() + " players.");
    }

    private void savePlayerSettings() {
        settingsData = new YamlConfiguration();
        for (var entry : playerSettings.entrySet()) {
            String uuid = entry.getKey().toString();
            for (var setting : entry.getValue().entrySet()) {
                settingsData.set(uuid + "." + setting.getKey(), setting.getValue());
            }
        }
        try {
            settingsData.save(settingsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save settings.yml:");
            e.printStackTrace();
        }
    }

    public String getPlayerSetting(UUID playerId, String settingName) {
        Map<String, String> settings = playerSettings.get(playerId);
        if (settings != null && settings.containsKey(settingName.toLowerCase())) {
            return settings.get(settingName.toLowerCase());
        }
        SettingsConfig.Setting setting = config.getSetting(settingName);
        return setting != null ? setting.getDefaultOption() : "disabled";
    }

    public boolean setPlayerSetting(Player player, String settingName, String optionName) {
        UUID playerId = player.getUniqueId();
        SettingsConfig.Setting setting = config.getSetting(settingName);
        if (setting == null) return false;

        SettingsConfig.SettingOption option = setting.getOption(optionName);
        if (option == null) return false;

        playerSettings.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(settingName.toLowerCase(), optionName.toLowerCase());
        savePlayerSettings();

        String statusText = "You have " + optionName.toLowerCase() + " the " + settingName.toLowerCase() + ".";
        statusMessages.put(playerId, new StatusMessage(statusText, System.currentTimeMillis()));

        executeScript(player, option);
        plugin.getLogger().info("Set " + player.getName() + "'s " + settingName + " to " + optionName);
        return true;
    }

    private void executeScript(Player player, SettingsConfig.SettingOption option) {
        List<String> script = option.getScript();
        if (script == null || script.isEmpty()) return;

        plugin.getLogger().info("Executing " + option.getName() + " script for " + player.getName() + " (" + script.size() + " actions)");
        for (String line : script) {
            executeScriptLine(player, line);
        }
    }

    private void executeScriptLine(Player player, String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) return;

        line = line.replace("%player%", player.getName());

        if (line.startsWith("execute console command ")) {
            String command = stripQuotes(line.substring("execute console command ".length()).trim());
            if (plugin.isEnabled()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        } else if (line.startsWith("execute player command ")) {
            String command = stripQuotes(line.substring("execute player command ".length()).trim());
            if (plugin.isEnabled()) {
                player.performCommand(command);
            }
        } else if (line.startsWith("message ")) {
            String msg = line.substring("message ".length()).trim();
            if (msg.startsWith("%player% ")) {
                msg = msg.substring("%player% ".length());
            }
            msg = processColorCodes(msg);
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg));
        }
    }

    public Map<String, String> getPlayerSettings(UUID playerId) {
        Map<String, String> settings = new HashMap<>();
        for (String settingName : config.getSettingNames()) {
            settings.put(settingName, getPlayerSetting(playerId, settingName));
        }
        return settings;
    }

    public String getSettingsStatus(UUID playerId) {
        StatusMessage status = statusMessages.get(playerId);
        if (status == null) return "";
        long elapsed = System.currentTimeMillis() - status.timestamp;
        if (elapsed > 10_000L) {
            statusMessages.remove(playerId);
            return "";
        }
        return stripColorCodes(status.message);
    }

    // ---- Helpers ----

    private static String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String processColorCodes(String text) {
        if (text == null) return "";
        text = processHexColors(text);
        text = text.replace('&', '\u00A7');
        return text;
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private String processHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00A7x");
            for (char c : hex.toCharArray()) {
                replacement.append('\u00A7').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String stripColorCodes(String text) {
        return text == null ? "" : text.replaceAll("\u00A7[0-9a-fk-or]", "");
    }

    public SettingsConfig getConfig() { return config; }
    public ChaosCraftPlugin getPlugin() { return plugin; }

    // ---- Inner types ----

    private record StatusMessage(String message, long timestamp) {}
}
