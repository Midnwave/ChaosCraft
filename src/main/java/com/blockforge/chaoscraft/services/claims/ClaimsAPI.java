package com.blockforge.chaoscraft.services.claims;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Public API for the claims system. Used by modes, services, and external integrations.
 * Obtain via {@code ChaosCraftPlugin.getInstance().getClaimsService().getAPI()}.
 */
public interface ClaimsAPI {

    // ---- Check operations ----

    /** Check if there is a claim at the given location. */
    boolean isClaimAt(Location loc);

    /** Get the claim at the given location, or null. */
    Claim getClaimAt(Location loc);

    /** Check if a player can build (place/break blocks) at a location. */
    boolean canBuild(Player player, Location loc);

    /** Check if a player can interact (doors, buttons, levers) at a location. */
    boolean canInteract(Player player, Location loc);

    /** Check if a player can open containers at a location. */
    boolean canOpenContainer(Player player, Location loc);

    /** Get a specific flag value for the claim at a location. Returns default if no claim. */
    boolean getFlag(Location loc, ClaimFlag flag);

    // ---- Modification (admin/mode use) ----

    /** Create a new claim. Returns null if validation fails. */
    Claim createClaim(Player owner, Location corner1, Location corner2);

    /** Delete a claim. Returns true if successful. */
    boolean deleteClaim(Claim claim, Player actor);

    /** Resize an existing claim. Returns true if successful. */
    boolean resizeClaim(Claim claim, Player actor, int newX1, int newZ1, int newX2, int newZ2);

    // ---- Claim blocks ----

    /** Get available claim blocks for a player. */
    int getClaimBlocks(UUID player);

    /** Add claim blocks to a player. */
    void addClaimBlocks(UUID player, int amount);

    /** Set claim blocks for a player. */
    void setClaimBlocks(UUID player, int amount);

    /** Get used claim blocks for a player (sum of all claim areas). */
    int getUsedClaimBlocks(UUID player);

    // ---- Trust ----

    /** Add trust for a player on a claim. */
    boolean addTrust(Claim claim, Player executor, UUID target, TrustLevel level);

    /** Remove trust for a player on a claim. */
    boolean removeTrust(Claim claim, Player executor, UUID target);

    // ---- Queries ----

    /** Get all claims owned by a player. */
    List<Claim> getPlayerClaims(UUID owner);

    /** Get all claims in the system. */
    Collection<Claim> getAllClaims();

    /** Get a claim by ID. */
    Claim getClaimById(int id);

    // ---- Mode integration ----

    /** Should block display attacks deal damage at this location? */
    boolean shouldBlockDisplayDamageAt(Location loc);

    /** Should corruption spread to this location? */
    boolean shouldCorruptionSpreadAt(Location loc);

    /** Should mode events spawn at this location? */
    boolean shouldModeEventSpawnAt(Location loc);
}
