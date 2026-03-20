package com.blockforge.chaoscraft.services.stats;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite-backed persistent storage for player kills, s-kills, and mode survivals.
 */
public class PlayerStatsStorage {

    private final ChaosCraftPlugin plugin;
    private Connection connection;

    public PlayerStatsStorage(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dataDir = new File(plugin.getDataFolder(), "data");
            if (!dataDir.exists()) dataDir.mkdirs();

            File dbFile = new File(dataDir, "playerstats.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();
            plugin.getLogger().info("[Stats] SQLite database initialized at " + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Stats] Failed to initialize SQLite database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    player_uuid TEXT PRIMARY KEY,
                    kills INTEGER NOT NULL DEFAULT 0,
                    s_kills INTEGER NOT NULL DEFAULT 0,
                    mode_survivals INTEGER NOT NULL DEFAULT 0
                )
            """);
        }
    }

    public PlayerStats getStats(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT kills, s_kills, mode_survivals FROM player_stats WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PlayerStats(uuid, rs.getInt("kills"), rs.getInt("s_kills"), rs.getInt("mode_survivals"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to load stats for " + uuid, e);
        }
        return PlayerStats.empty(uuid);
    }

    public void ensurePlayer(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_stats (player_uuid) VALUES (?)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to ensure player " + uuid, e);
        }
    }

    public void addKills(UUID uuid, int amount) {
        ensurePlayer(uuid);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET kills = kills + ? WHERE player_uuid = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to add kills for " + uuid, e);
        }
    }

    public void setKills(UUID uuid, int amount) {
        ensurePlayer(uuid);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET kills = ? WHERE player_uuid = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to set kills for " + uuid, e);
        }
    }

    public void addSKills(UUID uuid, int amount) {
        ensurePlayer(uuid);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET s_kills = s_kills + ? WHERE player_uuid = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to add s-kills for " + uuid, e);
        }
    }

    public void setSKills(UUID uuid, int amount) {
        ensurePlayer(uuid);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET s_kills = ? WHERE player_uuid = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to set s-kills for " + uuid, e);
        }
    }

    public void addModeSurvivals(UUID uuid, int amount) {
        ensurePlayer(uuid);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET mode_survivals = mode_survivals + ? WHERE player_uuid = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to add survivals for " + uuid, e);
        }
    }

    public void setModeSurvivals(UUID uuid, int amount) {
        ensurePlayer(uuid);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_stats SET mode_survivals = ? WHERE player_uuid = ?")) {
            ps.setInt(1, amount);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Stats] Failed to set survivals for " + uuid, e);
        }
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("[Stats] SQLite database closed.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[Stats] Failed to close database", e);
            }
        }
    }
}
