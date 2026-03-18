package com.blockforge.chaoscraft.services.performance;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Holds all performance-limiter settings loaded from performance.yml.
 * Supports hot-reloading via {@link #load(YamlConfiguration)}.
 */
public class PerformanceConfig {

    // --- master switch ---
    private boolean enabled;

    // --- radius limit ---
    private boolean radiusLimitEnabled;
    private int radiusMaxMobs;
    private int radiusBlocks;

    // --- server limit ---
    private boolean serverLimitEnabled;
    private int serverMaxMobs;

    // --- per-player limit ---
    private boolean perPlayerLimitEnabled;
    private int perPlayerMaxMobs;
    private int perPlayerRadiusBlocks;

    // --- bypass list ---
    private final Set<String> bypassMobs = new HashSet<>();

    // --- ping cleanup ---
    private boolean pingCleanupEnabled;
    private int pingThresholdMs;
    private int pingCheckIntervalTicks;
    private int pingCleanupRadius;
    private int pingMaxRemovePerCycle;
    private int pingCooldownSeconds;
    private final List<String> pingTargetMobs = new ArrayList<>();
    private final Set<String> pingProtectedMobs = new HashSet<>();

    // --- messages (raw MiniMessage / legacy §-string) ---
    private String messagePrefix;
    private String msgSpawnBlockedRadius;
    private String msgSpawnBlockedServer;
    private String msgSpawnBlockedPlayer;
    private String msgPingCleanupWarning;
    private String msgPingCleanupComplete;
    private String msgStatusEnabled;
    private String msgStatusDisabled;
    private String msgConfigReloaded;
    private String msgInvalidValue;
    private String msgSettingUpdated;

    // --- debug ---
    private boolean debug;

    // ----------------------------------------------------- construction

    public PerformanceConfig(YamlConfiguration yaml) {
        load(yaml);
    }

    // ----------------------------------------------------- loading

    public void load(YamlConfiguration yaml) {
        enabled = yaml.getBoolean("enabled", true);

        // --- radius limit ---
        var radiusSection = yaml.getConfigurationSection("limits.radius");
        if (radiusSection != null) {
            radiusLimitEnabled = radiusSection.getBoolean("enabled", true);
            radiusMaxMobs = radiusSection.getInt("max-mobs", 100);
            radiusBlocks = radiusSection.getInt("radius-blocks", 100);
        } else {
            radiusLimitEnabled = true;
            radiusMaxMobs = 100;
            radiusBlocks = 100;
        }

        // --- server limit ---
        var serverSection = yaml.getConfigurationSection("limits.server");
        if (serverSection != null) {
            serverLimitEnabled = serverSection.getBoolean("enabled", true);
            serverMaxMobs = serverSection.getInt("max-mobs", 500);
        } else {
            serverLimitEnabled = true;
            serverMaxMobs = 500;
        }

        // --- per-player limit ---
        var playerSection = yaml.getConfigurationSection("limits.per-player");
        if (playerSection != null) {
            perPlayerLimitEnabled = playerSection.getBoolean("enabled", true);
            perPlayerMaxMobs = playerSection.getInt("max-mobs", 50);
            perPlayerRadiusBlocks = playerSection.getInt("radius-blocks", 64);
        } else {
            perPlayerLimitEnabled = true;
            perPlayerMaxMobs = 50;
            perPlayerRadiusBlocks = 64;
        }

        // --- bypass mobs ---
        bypassMobs.clear();
        bypassMobs.addAll(yaml.getStringList("bypass-mobs"));

        // --- ping cleanup ---
        var pingSection = yaml.getConfigurationSection("ping-cleanup");
        if (pingSection != null) {
            pingCleanupEnabled = pingSection.getBoolean("enabled", true);
            pingThresholdMs = pingSection.getInt("threshold-ms", 200);
            pingCheckIntervalTicks = pingSection.getInt("check-interval-ticks", 100);
            pingCleanupRadius = pingSection.getInt("cleanup-radius", 32);
            pingMaxRemovePerCycle = pingSection.getInt("max-remove-per-cycle", 10);
            pingCooldownSeconds = pingSection.getInt("cooldown-seconds", 30);

            pingTargetMobs.clear();
            pingTargetMobs.addAll(pingSection.getStringList("target-mobs"));

            pingProtectedMobs.clear();
            pingProtectedMobs.addAll(pingSection.getStringList("protected-mobs"));
        } else {
            pingCleanupEnabled = true;
            pingThresholdMs = 200;
            pingCheckIntervalTicks = 100;
            pingCleanupRadius = 32;
            pingMaxRemovePerCycle = 10;
            pingCooldownSeconds = 30;
        }

        // --- messages (stored as plain strings with & colour codes) ---
        var msgSection = yaml.getConfigurationSection("messages");
        messagePrefix          = msg(msgSection, "prefix",                "&8[&6ChaosCraft&8] &7");
        msgSpawnBlockedRadius  = msg(msgSection, "spawn-blocked-radius",  "&cSpawn blocked: Too many MythicMobs in radius ({count}/{max})");
        msgSpawnBlockedServer  = msg(msgSection, "spawn-blocked-server",  "&cSpawn blocked: Server MythicMob limit reached ({count}/{max})");
        msgSpawnBlockedPlayer  = msg(msgSection, "spawn-blocked-player",  "&cSpawn blocked: Too many MythicMobs near players");
        msgPingCleanupWarning  = msg(msgSection, "ping-cleanup-warning",  "&eHigh ping detected ({ping}ms). Reducing nearby mob count...");
        msgPingCleanupComplete = msg(msgSection, "ping-cleanup-complete", "&aCleared {count} mobs to improve performance.");
        msgStatusEnabled       = msg(msgSection, "status-enabled",        "&aPerformance limiter is &2ENABLED");
        msgStatusDisabled      = msg(msgSection, "status-disabled",       "&cPerformance limiter is &4DISABLED");
        msgConfigReloaded      = msg(msgSection, "config-reloaded",       "&aPerformance configuration reloaded!");
        msgInvalidValue        = msg(msgSection, "invalid-value",         "&cInvalid value. Please enter a valid number.");
        msgSettingUpdated      = msg(msgSection, "setting-updated",       "&aSetting &e{setting} &aupdated to &e{value}");

        debug = yaml.getBoolean("debug", false);
    }

    /** Read a string from the section (or fall back to the default) and translate '&' colour codes. */
    private static String msg(ConfigurationSection section, String key, String def) {
        var raw = section != null ? section.getString(key, def) : def;
        return translateColors(raw);
    }

    /** Translate '&' colour codes to section-sign colour codes. */
    private static String translateColors(String text) {
        if (text == null) return "";
        var chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(chars[i + 1]) != -1) {
                chars[i] = '\u00A7';         // §
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    // ----------------------------------------------------- accessors

    public boolean isEnabled()                { return enabled; }
    public void setEnabled(boolean enabled)   { this.enabled = enabled; }

    public boolean isRadiusLimitEnabled()                    { return radiusLimitEnabled; }
    public void setRadiusLimitEnabled(boolean enabled)       { this.radiusLimitEnabled = enabled; }
    public int getRadiusMaxMobs()                            { return radiusMaxMobs; }
    public void setRadiusMaxMobs(int max)                    { this.radiusMaxMobs = max; }
    public int getRadiusBlocks()                             { return radiusBlocks; }
    public void setRadiusBlocks(int radius)                  { this.radiusBlocks = radius; }

    public boolean isServerLimitEnabled()                    { return serverLimitEnabled; }
    public void setServerLimitEnabled(boolean enabled)       { this.serverLimitEnabled = enabled; }
    public int getServerMaxMobs()                            { return serverMaxMobs; }
    public void setServerMaxMobs(int max)                    { this.serverMaxMobs = max; }

    public boolean isPerPlayerLimitEnabled()                 { return perPlayerLimitEnabled; }
    public void setPerPlayerLimitEnabled(boolean enabled)    { this.perPlayerLimitEnabled = enabled; }
    public int getPerPlayerMaxMobs()                         { return perPlayerMaxMobs; }
    public void setPerPlayerMaxMobs(int max)                 { this.perPlayerMaxMobs = max; }
    public int getPerPlayerRadiusBlocks()                    { return perPlayerRadiusBlocks; }
    public void setPerPlayerRadiusBlocks(int radius)         { this.perPlayerRadiusBlocks = radius; }

    public Set<String> getBypassMobs()                       { return bypassMobs; }
    public boolean isBypassMob(String mobId)                 { return bypassMobs.contains(mobId); }

    public boolean isPingCleanupEnabled()                    { return pingCleanupEnabled; }
    public void setPingCleanupEnabled(boolean enabled)       { this.pingCleanupEnabled = enabled; }
    public int getPingThresholdMs()                          { return pingThresholdMs; }
    public void setPingThresholdMs(int threshold)            { this.pingThresholdMs = threshold; }
    public int getPingCheckIntervalTicks()                   { return pingCheckIntervalTicks; }
    public int getPingCleanupRadius()                        { return pingCleanupRadius; }
    public void setPingCleanupRadius(int radius)             { this.pingCleanupRadius = radius; }
    public int getPingMaxRemovePerCycle()                    { return pingMaxRemovePerCycle; }
    public void setPingMaxRemovePerCycle(int max)            { this.pingMaxRemovePerCycle = max; }
    public int getPingCooldownSeconds()                      { return pingCooldownSeconds; }
    public void setPingCooldownSeconds(int cooldown)         { this.pingCooldownSeconds = cooldown; }

    public List<String> getPingTargetMobs()                  { return pingTargetMobs; }
    public Set<String> getPingProtectedMobs()                { return pingProtectedMobs; }
    public boolean isPingProtectedMob(String mobId)          { return pingProtectedMobs.contains(mobId); }

    public String getMessagePrefix()           { return messagePrefix; }
    public String getMsgSpawnBlockedRadius()    { return msgSpawnBlockedRadius; }
    public String getMsgSpawnBlockedServer()    { return msgSpawnBlockedServer; }
    public String getMsgSpawnBlockedPlayer()    { return msgSpawnBlockedPlayer; }
    public String getMsgPingCleanupWarning()    { return msgPingCleanupWarning; }
    public String getMsgPingCleanupComplete()   { return msgPingCleanupComplete; }
    public String getMsgStatusEnabled()         { return msgStatusEnabled; }
    public String getMsgStatusDisabled()        { return msgStatusDisabled; }
    public String getMsgConfigReloaded()        { return msgConfigReloaded; }
    public String getMsgInvalidValue()          { return msgInvalidValue; }
    public String getMsgSettingUpdated()        { return msgSettingUpdated; }

    public boolean isDebug()                   { return debug; }
    public void setDebug(boolean debug)        { this.debug = debug; }
}
