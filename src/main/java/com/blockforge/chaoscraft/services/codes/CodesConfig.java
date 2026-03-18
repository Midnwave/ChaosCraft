package com.blockforge.chaoscraft.services.codes;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Loads the codes-system configuration: attempt limits, blacklist duration, and sound keys.
 */
public class CodesConfig {

    private int maxIncorrectAttempts;
    private long blacklistDurationMillis;
    private String successSound;
    private String invalidSound;
    private String expiredSound;
    private String cooldownSound;
    private String alreadyRedeemedSound;
    private String blacklistedSound;

    public void load(YamlConfiguration yaml) {
        maxIncorrectAttempts = yaml.getInt("MaxIncorrectAttempts", 3);
        blacklistDurationMillis = parseTimeString(yaml.getString("BlacklistDuration", "1h"));
        successSound = yaml.getString("Sounds.Success", "minecraft:entity.player.levelup");
        invalidSound = yaml.getString("Sounds.Invalid", "minecraft:entity.villager.no");
        expiredSound = yaml.getString("Sounds.Expired", "minecraft:entity.wither.hurt");
        cooldownSound = yaml.getString("Sounds.Cooldown", "minecraft:block.note_block.bass");
        alreadyRedeemedSound = yaml.getString("Sounds.AlreadyRedeemed", "minecraft:entity.villager.no");
        blacklistedSound = yaml.getString("Sounds.Blacklisted", "minecraft:entity.ender_dragon.growl");
    }

    // ---- Time parsing ----

    public static long parseTimeString(String timeStr) {
        if (timeStr == null || timeStr.isEmpty() || timeStr.equalsIgnoreCase("never")) {
            return -1L;
        }

        long totalMillis = 0L;
        StringBuilder currentNumber = new StringBuilder();
        for (char c : timeStr.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else if (!currentNumber.isEmpty()) {
                int value = Integer.parseInt(currentNumber.toString());
                totalMillis += switch (c) {
                    case 'd' -> value * 86_400_000L;
                    case 'h' -> value * 3_600_000L;
                    case 'm' -> value * 60_000L;
                    case 's' -> value * 1_000L;
                    default -> 0L;
                };
                currentNumber = new StringBuilder();
            }
        }
        return totalMillis;
    }

    public static String formatTimeRemaining(long millis) {
        if (millis <= 0L) return "0 seconds";

        long days = millis / 86_400_000L;
        long hours = millis % 86_400_000L / 3_600_000L;
        long minutes = millis % 3_600_000L / 60_000L;
        long seconds = millis % 60_000L / 1_000L;

        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append(" day").append(days > 1 ? "s" : "").append(", ");
        if (hours > 0) result.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(", ");
        if (minutes > 0) result.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(", ");
        if (seconds > 0 || result.isEmpty()) result.append(seconds).append(" second").append(seconds > 1 ? "s" : "");

        String str = result.toString();
        if (str.endsWith(", ")) str = str.substring(0, str.length() - 2);
        return str;
    }

    // ---- Accessors ----

    public int getMaxIncorrectAttempts() { return maxIncorrectAttempts; }
    public long getBlacklistDurationMillis() { return blacklistDurationMillis; }
    public String getSuccessSound() { return successSound; }
    public String getInvalidSound() { return invalidSound; }
    public String getExpiredSound() { return expiredSound; }
    public String getCooldownSound() { return cooldownSound; }
    public String getAlreadyRedeemedSound() { return alreadyRedeemedSound; }
    public String getBlacklistedSound() { return blacklistedSound; }
}
