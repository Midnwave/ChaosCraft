package com.blockforge.chaoscraft.weapons.ivory;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IvoryConfig {

    private boolean enabled;
    private String tagOff, tagCharging, tagActive, tagRage;
    private Material baseMaterial;
    private int modelDataOff, modelDataCharging, modelDataActive, modelDataRage;
    private String nameOff;
    private double attackSpeedOff;
    private boolean rightClickToCharge;
    private String nameCharging;
    private double attackSpeedCharging;
    private int chargeDurationTicks;
    private List<String> chargeScript;
    private String nameActive;
    private double attackCooldownActive, baseDamageActive;
    private double knockbackHorizontalActive, knockbackVerticalActive;
    private String soundSwing, soundHit;
    private float soundSwingVolume, soundSwingPitch, soundHitVolume, soundHitPitch;
    private boolean screenShakeEnabled, lightningEnabled, invisibleLightningBolt;
    private boolean whiteFlashEnabled, hitParticlesEnabled;
    private int whiteFlashDuration;
    private List<String> onHitScript;
    private String nameRage;
    private double attackCooldownRage, baseDamageRage;
    private double knockbackHorizontalRage, knockbackVerticalRage;
    private int rageDurationSeconds;
    private double speedMultiplier;
    private List<String> onRageActivateScript, onRageDeactivateScript;
    private int stellarRageCooldown;
    private String stellarRageActivationMode;
    private boolean allowModeToggle;
    private String abilityCycleMethod;
    private boolean showCooldownWarning, abilitiesMobsOnly;
    private List<String> abilityOrder;
    private final Map<String, AbilitySettings> abilityConfigs = new HashMap<>();

    public IvoryConfig(YamlConfiguration yaml) {
        load(yaml);
    }

    public void load(YamlConfiguration yaml) {
        enabled = yaml.getBoolean("enabled", true);

        String offTag = yaml.getString("item-tags.\"off\"");
        if (offTag == null) offTag = yaml.getString("item-tags.off");
        String chargingTag = yaml.getString("item-tags.charging");
        String activeTag = yaml.getString("item-tags.active");
        String rageTag = yaml.getString("item-tags.rage");
        tagOff = offTag != null ? offTag : "celestial_ivory_off";
        tagCharging = chargingTag != null ? chargingTag : "celestial_ivory_charging";
        tagActive = activeTag != null ? activeTag : "celestial_ivory_active";
        tagRage = rageTag != null ? rageTag : "celestial_ivory_rage";

        var models = yaml.getConfigurationSection("custom-model-data");
        if (models != null) {
            modelDataOff = models.getInt("off", 39001);
            modelDataCharging = models.getInt("charging", 39002);
            modelDataActive = models.getInt("active", 39003);
            modelDataRage = models.getInt("rage", 39004);
        } else {
            modelDataOff = 39001; modelDataCharging = 39002;
            modelDataActive = 39003; modelDataRage = 39004;
        }

        String matName = yaml.getString("base-material", "DIAMOND_SWORD");
        try { baseMaterial = Material.valueOf(matName.toUpperCase()); }
        catch (IllegalArgumentException e) { baseMaterial = Material.DIAMOND_SWORD; }

        var off = yaml.getConfigurationSection("off");
        if (off != null) {
            nameOff = color(off.getString("name", "&7Celestial Ivory &8(Dormant)"));
            attackSpeedOff = off.getDouble("attack-speed", 48.0);
            rightClickToCharge = off.getBoolean("right-click-to-charge", true);
        } else {
            nameOff = color("&7Celestial Ivory &8(Dormant)");
            attackSpeedOff = 48.0; rightClickToCharge = true;
        }

        var charging = yaml.getConfigurationSection("charging");
        if (charging != null) {
            nameCharging = color(charging.getString("name", "&eCharging..."));
            attackSpeedCharging = charging.getDouble("attack-speed", 48.0);
            chargeDurationTicks = charging.getInt("charge-duration-ticks", 160);
            chargeScript = charging.getStringList("charge-script");
        } else {
            nameCharging = color("&eCharging...");
            attackSpeedCharging = 48.0; chargeDurationTicks = 160;
            chargeScript = new ArrayList<>();
        }

        var active = yaml.getConfigurationSection("active");
        if (active != null) {
            nameActive = color(active.getString("name", "&fCelestial Ivory &b\u2726"));
            attackCooldownActive = active.getDouble("attack-cooldown-seconds", 3.0);
            baseDamageActive = active.getDouble("base-damage", 15.0);
            var kb = active.getConfigurationSection("knockback");
            knockbackHorizontalActive = kb != null ? kb.getDouble("horizontal", 1.5) : 1.5;
            knockbackVerticalActive = kb != null ? kb.getDouble("vertical", 0.5) : 0.5;
            var sounds = active.getConfigurationSection("sounds");
            if (sounds != null) {
                soundSwing = sounds.getString("swing", "chaoscraft:ivory.swing");
                soundSwingVolume = (float) sounds.getDouble("swing-volume", 1.0);
                soundSwingPitch = (float) sounds.getDouble("swing-pitch", 1.0);
                soundHit = sounds.getString("hit", "chaoscraft:ivory.hit");
                soundHitVolume = (float) sounds.getDouble("hit-volume", 1.5);
                soundHitPitch = (float) sounds.getDouble("hit-pitch", 1.0);
            } else {
                soundSwing = "chaoscraft:ivory.swing"; soundSwingVolume = 1.0f; soundSwingPitch = 1.0f;
                soundHit = "chaoscraft:ivory.hit"; soundHitVolume = 1.5f; soundHitPitch = 1.0f;
            }
            var fx = active.getConfigurationSection("hit-effects");
            if (fx != null) {
                screenShakeEnabled = fx.getBoolean("screen-shake.enabled", true);
                lightningEnabled = fx.getBoolean("lightning.enabled", true);
                invisibleLightningBolt = fx.getBoolean("lightning.invisible-bolt", true);
                whiteFlashEnabled = fx.getBoolean("white-flash.enabled", true);
                whiteFlashDuration = fx.getInt("white-flash.duration-ticks", 10);
                hitParticlesEnabled = fx.getBoolean("particles.enabled", true);
            } else {
                screenShakeEnabled = lightningEnabled = invisibleLightningBolt = true;
                whiteFlashEnabled = hitParticlesEnabled = true; whiteFlashDuration = 10;
            }
            onHitScript = active.getStringList("on-hit-script");
        } else {
            nameActive = color("&fCelestial Ivory &b\u2726");
            attackCooldownActive = 3.0; baseDamageActive = 15.0;
            knockbackHorizontalActive = 1.5; knockbackVerticalActive = 0.5;
            soundSwing = "chaoscraft:ivory.swing"; soundSwingVolume = 1.0f; soundSwingPitch = 1.0f;
            soundHit = "chaoscraft:ivory.hit"; soundHitVolume = 1.5f; soundHitPitch = 1.0f;
            screenShakeEnabled = lightningEnabled = invisibleLightningBolt = true;
            whiteFlashEnabled = hitParticlesEnabled = true; whiteFlashDuration = 10;
            onHitScript = new ArrayList<>();
        }

        var rage = yaml.getConfigurationSection("rage");
        if (rage != null) {
            nameRage = color(rage.getString("name", "&c&lCELESTIAL IVORY &4\u26A1 RAGE"));
            attackCooldownRage = rage.getDouble("attack-cooldown-seconds", 0.5);
            baseDamageRage = rage.getDouble("base-damage", 25.0);
            var rageKb = rage.getConfigurationSection("knockback");
            knockbackHorizontalRage = rageKb != null ? rageKb.getDouble("horizontal", 2.5) : 2.5;
            knockbackVerticalRage = rageKb != null ? rageKb.getDouble("vertical", 0.8) : 0.8;
            rageDurationSeconds = rage.getInt("duration-seconds", 15);
            speedMultiplier = rage.getDouble("speed-multiplier", 0.7);
            onRageActivateScript = rage.getStringList("on-activate-script");
            onRageDeactivateScript = rage.getStringList("on-deactivate-script");
        } else {
            nameRage = color("&c&lCELESTIAL IVORY &4\u26A1 RAGE");
            attackCooldownRage = 0.5; baseDamageRage = 25.0;
            knockbackHorizontalRage = 2.5; knockbackVerticalRage = 0.8;
            rageDurationSeconds = 15; speedMultiplier = 0.7;
            onRageActivateScript = new ArrayList<>(); onRageDeactivateScript = new ArrayList<>();
        }

        var stellar = yaml.getConfigurationSection("stellar-rage");
        if (stellar != null) {
            stellarRageCooldown = stellar.getInt("cooldown-seconds", 60);
            stellarRageActivationMode = stellar.getString("activation-mode", "right_click");
            allowModeToggle = stellar.getBoolean("allow-mode-toggle", true);
        } else {
            stellarRageCooldown = 60; stellarRageActivationMode = "right_click"; allowModeToggle = true;
        }

        var abilities = yaml.getConfigurationSection("abilities");
        if (abilities != null) {
            abilityCycleMethod = abilities.getString("cycle-method", "crouch_scroll");
            showCooldownWarning = abilities.getBoolean("show-cooldown-warning", true);
            abilitiesMobsOnly = abilities.getBoolean("mobs-only", true);
            abilityOrder = abilities.getStringList("order");
        } else {
            abilityCycleMethod = "crouch_scroll"; showCooldownWarning = true;
            abilitiesMobsOnly = true; abilityOrder = new ArrayList<>();
        }

        abilityConfigs.clear();
        var abilityConfig = yaml.getConfigurationSection("ability-config");
        if (abilityConfig != null) {
            for (String key : abilityConfig.getKeys(false)) {
                var ab = abilityConfig.getConfigurationSection(key);
                if (ab == null) continue;
                var settings = new AbilitySettings();
                settings.enabled = ab.getBoolean("enabled", true);
                settings.cooldownSeconds = ab.getInt("cooldown-seconds", 30);
                settings.damagePerTick = ab.getDouble("damage-per-tick", 2.0);
                settings.damageTickInterval = ab.getInt("damage-tick-interval", 5);
                settings.radius = ab.getDouble("radius", 15.0);
                for (String subKey : ab.getKeys(false)) {
                    if (!subKey.equals("enabled") && !subKey.equals("cooldown-seconds")
                            && !subKey.equals("damage-per-tick") && !subKey.equals("damage-tick-interval")
                            && !subKey.equals("radius")) {
                        settings.extraSettings.put(subKey, ab.get(subKey));
                    }
                }
                abilityConfigs.put(key, settings);
            }
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // --- Getters ---
    public boolean isEnabled() { return enabled; }
    public String getTagOff() { return tagOff; }
    public String getTagCharging() { return tagCharging; }
    public String getTagActive() { return tagActive; }
    public String getTagRage() { return tagRage; }
    public boolean isIvoryTag(String tag) {
        return tag.equals(tagOff) || tag.equals(tagCharging) || tag.equals(tagActive) || tag.equals(tagRage);
    }
    public Material getBaseMaterial() { return baseMaterial; }
    public int getModelDataOff() { return modelDataOff; }
    public int getModelDataCharging() { return modelDataCharging; }
    public int getModelDataActive() { return modelDataActive; }
    public int getModelDataRage() { return modelDataRage; }
    public int getModelDataForTag(String tag) {
        if (tag.equals(tagOff)) return modelDataOff;
        if (tag.equals(tagCharging)) return modelDataCharging;
        if (tag.equals(tagActive)) return modelDataActive;
        if (tag.equals(tagRage)) return modelDataRage;
        return modelDataOff;
    }
    public String getNameOff() { return nameOff; }
    public double getAttackSpeedOff() { return attackSpeedOff; }
    public boolean isRightClickToCharge() { return rightClickToCharge; }
    public String getNameCharging() { return nameCharging; }
    public double getAttackSpeedCharging() { return attackSpeedCharging; }
    public int getChargeDurationTicks() { return chargeDurationTicks; }
    public List<String> getChargeScript() { return chargeScript; }
    public String getNameActive() { return nameActive; }
    public double getAttackCooldownActive() { return attackCooldownActive; }
    public double getBaseDamageActive() { return baseDamageActive; }
    public double getKnockbackHorizontalActive() { return knockbackHorizontalActive; }
    public double getKnockbackVerticalActive() { return knockbackVerticalActive; }
    public String getSoundSwing() { return soundSwing; }
    public float getSoundSwingVolume() { return soundSwingVolume; }
    public float getSoundSwingPitch() { return soundSwingPitch; }
    public String getSoundHit() { return soundHit; }
    public float getSoundHitVolume() { return soundHitVolume; }
    public float getSoundHitPitch() { return soundHitPitch; }
    public boolean isScreenShakeEnabled() { return screenShakeEnabled; }
    public boolean isLightningEnabled() { return lightningEnabled; }
    public boolean isInvisibleLightningBolt() { return invisibleLightningBolt; }
    public boolean isWhiteFlashEnabled() { return whiteFlashEnabled; }
    public int getWhiteFlashDuration() { return whiteFlashDuration; }
    public boolean isHitParticlesEnabled() { return hitParticlesEnabled; }
    public List<String> getOnHitScript() { return onHitScript; }
    public String getNameRage() { return nameRage; }
    public double getAttackCooldownRage() { return attackCooldownRage; }
    public double getBaseDamageRage() { return baseDamageRage; }
    public double getKnockbackHorizontalRage() { return knockbackHorizontalRage; }
    public double getKnockbackVerticalRage() { return knockbackVerticalRage; }
    public int getRageDurationSeconds() { return rageDurationSeconds; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public List<String> getOnRageActivateScript() { return onRageActivateScript; }
    public List<String> getOnRageDeactivateScript() { return onRageDeactivateScript; }
    public int getStellarRageCooldown() { return stellarRageCooldown; }
    public String getStellarRageActivationMode() { return stellarRageActivationMode; }
    public boolean isAllowModeToggle() { return allowModeToggle; }
    public String getAbilityCycleMethod() { return abilityCycleMethod; }
    public boolean isShowCooldownWarning() { return showCooldownWarning; }
    public boolean isAbilitiesMobsOnly() { return abilitiesMobsOnly; }
    public List<String> getAbilityOrder() { return abilityOrder; }
    public AbilitySettings getAbilitySettings(String abilityId) {
        return abilityConfigs.getOrDefault(abilityId, new AbilitySettings());
    }

    public static class AbilitySettings {
        public boolean enabled = true;
        public int cooldownSeconds = 30;
        public double damagePerTick = 2.0;
        public int damageTickInterval = 5;
        public double radius = 15.0;
        public final Map<String, Object> extraSettings = new HashMap<>();

        public int getInt(String key, int def) {
            var val = extraSettings.get(key);
            return val instanceof Number n ? n.intValue() : def;
        }
        public double getDouble(String key, double def) {
            var val = extraSettings.get(key);
            return val instanceof Number n ? n.doubleValue() : def;
        }
        public String getString(String key, String def) {
            var val = extraSettings.get(key);
            return val instanceof String s ? s : def;
        }
        public boolean getBoolean(String key, boolean def) {
            var val = extraSettings.get(key);
            return val instanceof Boolean b ? b : def;
        }
    }
}
