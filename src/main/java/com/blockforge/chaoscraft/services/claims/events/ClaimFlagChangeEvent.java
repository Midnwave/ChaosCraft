package com.blockforge.chaoscraft.services.claims.events;

import com.blockforge.chaoscraft.services.claims.Claim;
import com.blockforge.chaoscraft.services.claims.ClaimFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ClaimFlagChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Claim claim;
    private final Player player;
    private final ClaimFlag flag;
    private final boolean newValue;
    private boolean cancelled;

    public ClaimFlagChangeEvent(Claim claim, Player player, ClaimFlag flag, boolean newValue) {
        this.claim = claim;
        this.player = player;
        this.flag = flag;
        this.newValue = newValue;
    }

    public Claim getClaim() { return claim; }
    public Player getPlayer() { return player; }
    public ClaimFlag getFlag() { return flag; }
    public boolean getNewValue() { return newValue; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
