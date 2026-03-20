package com.blockforge.chaoscraft.services.shop;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * SQLite-backed virtual storage for items that couldn't fit in a player's inventory
 * during a shop purchase. Items are serialized as bytes via ItemStack.serializeAsBytes().
 */
public class VirtualStorage {

    private final ChaosCraftPlugin plugin;
    private Connection connection;

    public VirtualStorage(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    // Initialization
    // ========================

    public void initialize() {
        try {
            File dataDir = new File(plugin.getDataFolder(), "data");
            if (!dataDir.exists()) dataDir.mkdirs();

            File dbFile = new File(dataDir, "shop.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
            }

            createTables();
            plugin.getLogger().info("[Shop] Virtual storage database initialized at " + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[Shop] Failed to initialize virtual storage database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS virtual_storage (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    item_data BLOB NOT NULL,
                    amount INTEGER NOT NULL DEFAULT 1
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_vs_player ON virtual_storage(player_uuid)");
        }
    }

    // ========================
    // Storage operations
    // ========================

    /**
     * Add an item to a player's virtual storage.
     */
    public void addItem(UUID playerUuid, ItemStack item) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO virtual_storage (player_uuid, item_data, amount) VALUES (?, ?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setBytes(2, item.serializeAsBytes());
            ps.setInt(3, item.getAmount());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Shop] Failed to add item to virtual storage for " + playerUuid, e);
        }
    }

    /**
     * Get all items in a player's virtual storage.
     */
    public List<VirtualStorageEntry> getItems(UUID playerUuid) {
        List<VirtualStorageEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, item_data, amount FROM virtual_storage WHERE player_uuid = ? ORDER BY id")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    byte[] data = rs.getBytes("item_data");
                    int amount = rs.getInt("amount");
                    try {
                        ItemStack item = ItemStack.deserializeBytes(data);
                        item.setAmount(amount);
                        entries.add(new VirtualStorageEntry(id, item, amount));
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "[Shop] Failed to deserialize virtual storage item id=" + id + " for " + playerUuid, e);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Shop] Failed to load virtual storage for " + playerUuid, e);
        }
        return entries;
    }

    /**
     * Claim a specific item from virtual storage and give it to the player.
     * Returns true if the item was successfully claimed.
     */
    public boolean claimItem(UUID playerUuid, int entryId, Player player) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT item_data, amount FROM virtual_storage WHERE id = ? AND player_uuid = ?")) {
            ps.setInt(1, entryId);
            ps.setString(2, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] data = rs.getBytes("item_data");
                    int amount = rs.getInt("amount");
                    ItemStack item = ItemStack.deserializeBytes(data);
                    item.setAmount(amount);

                    // Check if player has room
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                    if (overflow.isEmpty()) {
                        // Fully claimed, remove from storage
                        removeEntry(entryId);
                        return true;
                    } else {
                        // Partial claim — update remaining amount
                        int remaining = 0;
                        for (ItemStack leftover : overflow.values()) {
                            remaining += leftover.getAmount();
                        }
                        updateAmount(entryId, remaining);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Shop] Failed to claim virtual storage item for " + playerUuid, e);
        }
        return false;
    }

    /**
     * Get the total number of items in a player's virtual storage.
     */
    public int getItemCount(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM virtual_storage WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Shop] Failed to count virtual storage items for " + playerUuid, e);
        }
        return 0;
    }

    private void removeEntry(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM virtual_storage WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Shop] Failed to remove virtual storage entry id=" + id, e);
        }
    }

    private void updateAmount(int id, int amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE virtual_storage SET amount = ? WHERE id = ?")) {
            ps.setInt(1, amount);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Shop] Failed to update virtual storage entry id=" + id, e);
        }
    }

    // ========================
    // Shutdown
    // ========================

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("[Shop] Virtual storage database closed.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[Shop] Failed to close virtual storage database", e);
            }
        }
    }

    // ========================
    // Entry record
    // ========================

    public record VirtualStorageEntry(int id, ItemStack item, int amount) {}
}
