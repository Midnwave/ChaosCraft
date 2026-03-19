package com.blockforge.chaoscraft.services.claims.events;

import com.blockforge.chaoscraft.services.claims.Claim;
import com.blockforge.chaoscraft.services.claims.TrustLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClaimTrustChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Claim claim;
    private final Player executor;
    private final UUID target;
    private final TrustLevel newLevel; // null = trust removed
    private boolean cancelled;

    public ClaimTrustChangeEvent(Claim claim, Player executor, UUID target, @Nullable TrustLevel newLevel) {
        this.claim = claim;
        this.executor = executor;
        this.target = target;
        this.newLevel = newLevel;
    }

    public Claim getClaim() { return claim; }
    public Player getExecutor() { return executor; }
    public UUID getTarget() { return target; }
    @Nullable public TrustLevel getNewLevel() { return newLevel; }
    public boolean isRemoval() { return newLevel == null; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
