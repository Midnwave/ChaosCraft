package com.blockforge.chaoscraft.services.todo;

import com.blockforge.chaoscraft.ChaosCraftPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite storage for player to-do lists.
 * Database: plugins/ChaosCraft/data/todos.db
 */
public class TodoStorage {

    private final ChaosCraftPlugin plugin;
    private Connection connection;

    public TodoStorage(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    private void connect() {
        try {
            File dataDir = new File(plugin.getDataFolder(), "data");
            if (!dataDir.exists()) dataDir.mkdirs();
            String url = "jdbc:sqlite:" + new File(dataDir, "todos.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // WAL mode for better concurrent read/write
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to connect to SQLite: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS todos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner TEXT NOT NULL,
                    text TEXT NOT NULL,
                    completed INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    completed_at INTEGER DEFAULT 0
                )
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_todos_owner ON todos(owner)");
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to create tables: " + e.getMessage());
        }
    }

    /**
     * Add a new to-do item for a player.
     * @return the generated ID, or -1 on failure
     */
    public int addTodo(UUID owner, String text) {
        String sql = "INSERT INTO todos (owner, text, completed, created_at, completed_at) VALUES (?, ?, 0, ?, 0)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, owner.toString());
            ps.setString(2, text);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to add todo: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Get all to-do items for a player.
     */
    public List<TodoItem> getTodos(UUID owner) {
        List<TodoItem> items = new ArrayList<>();
        String sql = "SELECT id, owner, text, completed, created_at, completed_at FROM todos WHERE owner = ? ORDER BY completed ASC, id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new TodoItem(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("owner")),
                        rs.getString("text"),
                        rs.getInt("completed") == 1,
                        rs.getLong("created_at"),
                        rs.getLong("completed_at")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to get todos: " + e.getMessage());
        }
        return items;
    }

    /**
     * Mark a to-do as completed or uncompleted.
     */
    public boolean setCompleted(int id, UUID owner, boolean completed) {
        String sql = "UPDATE todos SET completed = ?, completed_at = ? WHERE id = ? AND owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, completed ? 1 : 0);
            ps.setLong(2, completed ? System.currentTimeMillis() : 0);
            ps.setInt(3, id);
            ps.setString(4, owner.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to update todo: " + e.getMessage());
        }
        return false;
    }

    /**
     * Edit the text of a to-do item.
     */
    public boolean editTodo(int id, UUID owner, String newText) {
        String sql = "UPDATE todos SET text = ? WHERE id = ? AND owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newText);
            ps.setInt(2, id);
            ps.setString(3, owner.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to edit todo: " + e.getMessage());
        }
        return false;
    }

    /**
     * Delete a to-do item.
     */
    public boolean deleteTodo(int id, UUID owner) {
        String sql = "DELETE FROM todos WHERE id = ? AND owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, owner.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to delete todo: " + e.getMessage());
        }
        return false;
    }

    /**
     * Clear all completed to-do items for a player.
     */
    public int clearCompleted(UUID owner) {
        String sql = "DELETE FROM todos WHERE owner = ? AND completed = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to clear completed: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get a specific to-do item by ID.
     */
    public TodoItem getTodo(int id, UUID owner) {
        String sql = "SELECT id, owner, text, completed, created_at, completed_at FROM todos WHERE id = ? AND owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, owner.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new TodoItem(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("owner")),
                        rs.getString("text"),
                        rs.getInt("completed") == 1,
                        rs.getLong("created_at"),
                        rs.getLong("completed_at")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Todo] Failed to get todo: " + e.getMessage());
        }
        return null;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // ignore
        }
    }
}
