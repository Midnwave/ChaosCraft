package com.blockforge.chaoscraft.services.codes;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks incorrect code-entry attempts per player and manages
 * time-based blacklisting when too many wrong codes are entered.
 */
public class BlacklistData {

    private final File file;
    private YamlConfiguration yaml;
    private final Map<UUID, BlacklistEntry> blacklist = new HashMap<>();

    public BlacklistData(File file) {
        this.file = file;
        load();
    }

    // ---- Persistence ----

    public void load() {
        if (!file.exists()) {
            try { file.createNewFile(); }
            catch (IOException e) { e.printStackTrace(); }
        }

        yaml = YamlConfiguration.loadConfiguration(file);
        blacklist.clear();

        for (String uuidStr : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                int incorrectAttempts = yaml.getInt(uuidStr + ".incorrectAttempts");
                long blacklistedUntil = yaml.getLong(uuidStr + ".blacklistedUntil");
                blacklist.put(uuid, new BlacklistEntry(incorrectAttempts, blacklistedUntil));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        yaml = new YamlConfiguration();
        for (var entry : blacklist.entrySet()) {
            String uuid = entry.getKey().toString();
            yaml.set(uuid + ".incorrectAttempts", entry.getValue().incorrectAttempts);
            yaml.set(uuid + ".blacklistedUntil", entry.getValue().blacklistedUntil);
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- Query ----

    public boolean isBlacklisted(UUID playerId) {
        BlacklistEntry entry = blacklist.get(playerId);
        if (entry == null) return false;

        if (entry.blacklistedUntil > 0L && System.currentTimeMillis() >= entry.blacklistedUntil) {
            blacklist.remove(playerId);
            save();
            return false;
        }
        return entry.blacklistedUntil > 0L;
    }

    public long getBlacklistTimeRemaining(UUID playerId) {
        BlacklistEntry entry = blacklist.get(playerId);
        if (entry != null && entry.blacklistedUntil > 0L) {
            return Math.max(0L, entry.blacklistedUntil - System.currentTimeMillis());
        }
        return 0L;
    }

    public int getIncorrectAttempts(UUID playerId) {
        BlacklistEntry entry = blacklist.get(playerId);
        return entry != null ? entry.incorrectAttempts : 0;
    }

    // ---- Mutation ----

    public void recordIncorrectAttempt(UUID playerId, int maxAttempts, long blacklistDuration) {
        BlacklistEntry entry = blacklist.getOrDefault(playerId, new BlacklistEntry(0, 0L));
        entry.incorrectAttempts++;
        if (entry.incorrectAttempts >= maxAttempts) {
            entry.blacklistedUntil = System.currentTimeMillis() + blacklistDuration;
        }
        blacklist.put(playerId, entry);
        save();
    }

    public void resetAttempts(UUID playerId) {
        blacklist.remove(playerId);
        save();
    }

    // ---- Inner type ----

    private static class BlacklistEntry {
        int incorrectAttempts;
        long blacklistedUntil;

        BlacklistEntry(int incorrectAttempts, long blacklistedUntil) {
            this.incorrectAttempts = incorrectAttempts;
            this.blacklistedUntil = blacklistedUntil;
        }
    }
}
