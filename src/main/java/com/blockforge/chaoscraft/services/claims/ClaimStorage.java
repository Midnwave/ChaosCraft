package com.blockforge.chaoscraft.services.claims;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * SQLite-backed persistent storage for claims, trusts, flags, and claim blocks.
 */
public class ClaimStorage {

    private final ChaosCraftPlugin plugin;
    private Connection connection;
    private int nextClaimId = 1;

    public ClaimStorage(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    // Initialization
    // ========================

    public void initialize() {
        try {
            File dataDir = new File(plugin.getDataFolder(), "data");
            if (!dataDir.exists()) dataDir.mkdirs();

            File dbFile = new File(dataDir, "claims.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // Enable WAL mode for better concurrent performance
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();
            loadNextId();

            plugin.getLogger().info("[Claims] SQLite database initialized at " + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Claims] Failed to initialize SQLite database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS claims (
                    id INTEGER PRIMARY KEY,
                    owner TEXT NOT NULL,
                    world TEXT NOT NULL,
                    min_x INTEGER NOT NULL,
                    min_z INTEGER NOT NULL,
                    max_x INTEGER NOT NULL,
                    max_z INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS trusts (
                    claim_id INTEGER NOT NULL,
                    player TEXT NOT NULL,
                    level TEXT NOT NULL,
                    PRIMARY KEY (claim_id, player),
                    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS claim_flags (
                    claim_id INTEGER NOT NULL,
                    flag TEXT NOT NULL,
                    value INTEGER NOT NULL,
                    PRIMARY KEY (claim_id, flag),
                    FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS claim_blocks (
                    player TEXT PRIMARY KEY,
                    blocks INTEGER NOT NULL DEFAULT 0
                )
            """);

            // Index for spatial lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_claims_world ON claims(world)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_claims_bounds ON claims(world, min_x, max_x, min_z, max_z)");
        }
    }

    private void loadNextId() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM claims")) {
            if (rs.next()) {
                nextClaimId = rs.getInt(1);
            }
        }
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Claims] Error closing database", e);
        }
    }

    // ========================
    // Claim CRUD
    // ========================

    public int getNextClaimId() {
        return nextClaimId++;
    }

    public void saveClaim(Claim claim) {
        String sql = """
            INSERT OR REPLACE INTO claims (id, owner, world, min_x, min_z, max_x, max_z, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, claim.getId());
            ps.setString(2, claim.getOwner().toString());
            ps.setString(3, claim.getWorldName());
            ps.setInt(4, claim.getMinX());
            ps.setInt(5, claim.getMinZ());
            ps.setInt(6, claim.getMaxX());
            ps.setInt(7, claim.getMaxZ());
            ps.setLong(8, claim.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Claims] Failed to save claim " + claim.getId(), e);
        }

        // Save trusts
        saveTrusts(claim);
        // Save flags (only non-default)
        saveFlags(claim);
    }

    public void deleteClaim(int claimId) {
        try {
            // Cascade delete handles trusts and flags
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM claims WHERE id = ?")) {
                ps.setInt(1, claimId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM trusts WHERE claim_id = ?")) {
                ps.setInt(1, claimId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM claim_flags WHERE claim_id = ?")) {
                ps.setInt(1, claimId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Claims] Failed to delete claim " + claimId, e);
        }
    }

    public List<Claim> loadAllClaims() {
        List<Claim> claims = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM claims")) {
            while (rs.next()) {
                Claim claim = new Claim(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("owner")),
                        rs.getString("world"),
                        rs.getInt("min_x"), rs.getInt("min_z"),
                        rs.getInt("max_x"), rs.getInt("max_z"),
                        rs.getLong("created_at")
                );
                loadTrusts(claim);
                loadFlags(claim);
                claims.add(claim);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Claims] Failed to load claims", e);
        }
        return claims;
    }

    // ========================
    // Trusts
    // ========================

    private void saveTrusts(Claim claim) {
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM trusts WHERE claim_id = ?")) {
                ps.setInt(1, claim.getId());
                ps.executeUpdate();
            }
            if (!claim.getTrusts().isEmpty()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO trusts (claim_id, player, level) VALUES (?, ?, ?)")) {
                    for (var entry : claim.getTrusts().entrySet()) {
                        ps.setInt(1, claim.getId());
                        ps.setString(2, entry.getKey().toString());
                        ps.setString(3, entry.getValue().name());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Claims] Failed to save trusts for claim " + claim.getId(), e);
        }
    }

    private void loadTrusts(Claim claim) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT player, level FROM trusts WHERE claim_id = ?")) {
            ps.setInt(1, claim.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID player = UUID.fromString(rs.getString("player"));
                    TrustLevel level = TrustLevel.fromString(rs.getString("level"));
                    if (level != null) {
                        claim.setTrust(player, level);
                    }
                }
            }
        }
    }

    // ========================
    // Flags
    // ========================

    private void saveFlags(Claim claim) {
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM claim_flags WHERE claim_id = ?")) {
                ps.setInt(1, claim.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO claim_flags (claim_id, flag, value) VALUES (?, ?, ?)")) {
                for (var entry : claim.getFlags().entrySet()) {
                    // Only save non-default values
                    if (entry.getValue() != entry.getKey().getDefaultValue()) {
                        ps.setInt(1, claim.getId());
                        ps.setString(2, entry.getKey().name());
                        ps.setInt(3, entry.getValue() ? 1 : 0);
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Claims] Failed to save flags for claim " + claim.getId(), e);
        }
    }

    private void loadFlags(Claim claim) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT flag, value FROM claim_flags WHERE claim_id = ?")) {
            ps.setInt(1, claim.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        ClaimFlag flag = ClaimFlag.valueOf(rs.getString("flag"));
                        claim.setFlag(flag, rs.getInt("value") == 1);
                    } catch (IllegalArgumentException ignored) {
                        // Flag was removed from enum, skip
                    }
                }
            }
        }
    }

    // ========================
    // Claim Blocks
    // ========================

    public int getClaimBlocks(UUID player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT blocks FROM claim_blocks WHERE player = ?")) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("blocks");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Claims] Failed to get claim blocks for " + player, e);
        }
        return 0;
    }

    public void setClaimBlocks(UUID player, int blocks) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO claim_blocks (player, blocks) VALUES (?, ?)")) {
            ps.setString(1, player.toString());
            ps.setInt(2, Math.max(0, blocks));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Claims] Failed to set claim blocks for " + player, e);
        }
    }

    public void addClaimBlocks(UUID player, int amount) {
        int current = getClaimBlocks(player);
        setClaimBlocks(player, current + amount);
    }
}
