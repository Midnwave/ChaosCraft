package com.blockforge.chaoscraft.services.todo;

import java.util.UUID;

/**
 * Represents a single to-do list item for a player.
 */
public class TodoItem {

    private final int id;
    private final UUID owner;
    private String text;
    private boolean completed;
    private final long createdAt;
    private long completedAt;

    public TodoItem(int id, UUID owner, String text, boolean completed, long createdAt, long completedAt) {
        this.id = id;
        this.owner = owner;
        this.text = text;
        this.completed = completed;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public int getId() { return id; }
    public UUID getOwner() { return owner; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public long getCreatedAt() { return createdAt; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
}
