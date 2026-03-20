package com.blockforge.chaoscraft.services.badges;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * SQLite-backed persistent storage for player badge ownership.
 * Database file: data/badges.db
 */
public class BadgeStorage {

    private final ChaosCraftPlugin plugin;
    private Connection connection;

    public BadgeStorage(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    // Initialization
    // ========================

    public void initialize() {
        try {
            File dataDir = new File(plugin.getDataFolder(), "data");
            if (!dataDir.exists()) dataDir.mkdirs();

            File dbFile = new File(dataDir, "badges.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // Enable WAL mode for better concurrent performance
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();

            plugin.getLogger().info("[Badges] SQLite database initialized at " + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Badges] Failed to initialize SQLite database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_badges (
                    player_uuid TEXT NOT NULL,
                    badge_id TEXT NOT NULL,
                    earned_at INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, badge_id)
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_badges_player ON player_badges(player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_badges_badge ON player_badges(badge_id)");
        }
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Badges] Error closing database", e);
        }
    }

    // ========================
    // Badge Operations
    // ========================

    /**
     * Check if a player has a specific badge.
     */
    public boolean hasBadge(UUID playerUuid, String badgeId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM player_badges WHERE player_uuid = ? AND badge_id = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, badgeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Badges] Failed to check badge for " + playerUuid, e);
        }
        return false;
    }

    /**
     * Grant a badge to a player. Returns true if the badge was newly granted.
     */
    public boolean grantBadge(UUID playerUuid, String badgeId) {
        if (hasBadge(playerUuid, badgeId)) return false;

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_badges (player_uuid, badge_id, earned_at) VALUES (?, ?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, badgeId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Badges] Failed to grant badge " + badgeId + " to " + playerUuid, e);
        }
        return false;
    }

    /**
     * Revoke a badge from a player. Returns true if the badge was removed.
     */
    public boolean revokeBadge(UUID playerUuid, String badgeId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM player_badges WHERE player_uuid = ? AND badge_id = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, badgeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Badges] Failed to revoke badge " + badgeId + " from " + playerUuid, e);
        }
        return false;
    }

    /**
     * Get all badges owned by a player, mapped as badge_id -> earned_at epoch millis.
     */
    public Map<String, Long> getPlayerBadges(UUID playerUuid) {
        Map<String, Long> badges = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT badge_id, earned_at FROM player_badges WHERE player_uuid = ? ORDER BY earned_at ASC")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    badges.put(rs.getString("badge_id"), rs.getLong("earned_at"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Badges] Failed to load badges for " + playerUuid, e);
        }
        return badges;
    }
}
