package com.blockforge.chaoscraft.services.claims.events;

import com.blockforge.chaoscraft.services.claims.Claim;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ClaimCreateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Claim claim;
    private final Player player;
    private boolean cancelled;

    public ClaimCreateEvent(Claim claim, Player player) {
        this.claim = claim;
        this.player = player;
    }

    public Claim getClaim() { return claim; }
    public Player getPlayer() { return player; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
