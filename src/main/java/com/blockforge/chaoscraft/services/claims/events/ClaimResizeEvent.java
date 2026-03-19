package com.blockforge.chaoscraft.services.claims.events;

import com.blockforge.chaoscraft.services.claims.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ClaimResizeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Claim claim;
    private final Player player;
    private final int newMinX, newMinZ, newMaxX, newMaxZ;
    private boolean cancelled;

    public ClaimResizeEvent(Claim claim, Player player, int newMinX, int newMinZ, int newMaxX, int newMaxZ) {
        this.claim = claim;
        this.player = player;
        this.newMinX = newMinX;
        this.newMinZ = newMinZ;
        this.newMaxX = newMaxX;
        this.newMaxZ = newMaxZ;
    }

    public Claim getClaim() { return claim; }
    public Player getPlayer() { return player; }
    public int getNewMinX() { return newMinX; }
    public int getNewMinZ() { return newMinZ; }
    public int getNewMaxX() { return newMaxX; }
    public int getNewMaxZ() { return newMaxZ; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
