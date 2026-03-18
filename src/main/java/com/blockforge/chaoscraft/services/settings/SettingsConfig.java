package com.blockforge.chaoscraft.services.settings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Defines available settings, their options, and associated scripts.
 * Loaded from SettingsConfig.yml.
 */
public class SettingsConfig {

    private final Map<String, Setting> settings = new LinkedHashMap<>();

    public void load(YamlConfiguration yaml) {
        settings.clear();
        ConfigurationSection settingsSection = yaml.getConfigurationSection("Settings");
        if (settingsSection == null) return;

        for (String settingName : settingsSection.getKeys(false)) {
            ConfigurationSection settingSection = settingsSection.getConfigurationSection(settingName);
            if (settingSection == null) continue;

            Setting setting = new Setting(settingName);
            setting.setDefaultOption(settingSection.getString("Default", "disabled"));

            ConfigurationSection optionsSection = settingSection.getConfigurationSection("Options");
            if (optionsSection != null) {
                for (String optionName : optionsSection.getKeys(false)) {
                    ConfigurationSection optionSection = optionsSection.getConfigurationSection(optionName);
                    if (optionSection != null) {
                        SettingOption option = new SettingOption(optionName);
                        option.setScript(optionSection.getStringList("Script"));
                        setting.addOption(option);
                    }
                }
            }

            settings.put(settingName.toLowerCase(), setting);
        }
    }

    public Setting getSetting(String name) {
        return settings.get(name.toLowerCase());
    }

    public Set<String> getSettingNames() {
        return settings.keySet();
    }

    public Collection<Setting> getSettings() {
        return settings.values();
    }

    public boolean hasSetting(String name) {
        return settings.containsKey(name.toLowerCase());
    }

    // ---- Inner types ----

    public static class Setting {
        private final String name;
        private final Map<String, SettingOption> options = new LinkedHashMap<>();
        private String defaultOption = "disabled";

        public Setting(String name) { this.name = name; }

        public String getName() { return name; }
        public String getDefaultOption() { return defaultOption; }
        public void setDefaultOption(String defaultOption) { this.defaultOption = defaultOption; }

        public void addOption(SettingOption option) {
            options.put(option.getName().toLowerCase(), option);
        }

        public SettingOption getOption(String name) {
            return options.get(name.toLowerCase());
        }

        public Collection<SettingOption> getOptions() { return options.values(); }
        public Set<String> getOptionNames() { return options.keySet(); }
        public boolean hasOption(String name) { return options.containsKey(name.toLowerCase()); }
    }

    public static class SettingOption {
        private final String name;
        private List<String> script = new ArrayList<>();

        public SettingOption(String name) { this.name = name; }

        public String getName() { return name; }
        public List<String> getScript() { return script; }
        public void setScript(List<String> script) {
            this.script = script != null ? script : new ArrayList<>();
        }
    }
}
