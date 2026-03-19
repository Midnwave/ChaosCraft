package com.blockforge.chaoscraft.services.claims;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Main claims service. Initializes storage, manager, and provides the public API.
 */
public class ClaimsService implements ClaimsAPI {

    private final ChaosCraftPlugin plugin;
    private final ClaimStorage storage;
    private final ClaimManager manager;
    private final ClaimBlockTracker blockTracker;
    private boolean enabled;

    public ClaimsService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.storage = new ClaimStorage(plugin);
        this.manager = new ClaimManager(plugin, storage);
        this.blockTracker = new ClaimBlockTracker(plugin, storage);
    }

    public void initialize() {
        enabled = plugin.getConfig().getBoolean("claims.enabled", true);
        if (!enabled) {
            plugin.getLogger().info("[Claims] Claims system disabled in config.");
            return;
        }

        storage.initialize();
        manager.initialize();
        blockTracker.initialize();

        plugin.getLogger().info("[Claims] Claims system initialized. " + manager.getClaimCount() + " claims loaded.");
    }

    public void shutdown() {
        if (blockTracker != null) blockTracker.shutdown();
        if (storage != null) storage.shutdown();
    }

    public void reload() {
        manager.loadConfig();
        blockTracker.loadConfig();
    }

    public boolean isEnabled() { return enabled; }
    public ClaimsAPI getAPI() { return this; }
    public ClaimManager getManager() { return manager; }
    public ClaimStorage getStorage() { return storage; }
    public ClaimBlockTracker getBlockTracker() { return blockTracker; }

    // ========================
    // ClaimsAPI implementation
    // ========================

    @Override
    public boolean isClaimAt(Location loc) {
        return manager.getClaimAt(loc) != null;
    }

    @Override
    public Claim getClaimAt(Location loc) {
        return manager.getClaimAt(loc);
    }

    @Override
    public boolean canBuild(Player player, Location loc) {
        return manager.canBuild(player, loc);
    }

    @Override
    public boolean canInteract(Player player, Location loc) {
        return manager.canAccess(player, loc);
    }

    @Override
    public boolean canOpenContainer(Player player, Location loc) {
        return manager.canOpenContainer(player, loc);
    }

    @Override
    public boolean getFlag(Location loc, ClaimFlag flag) {
        Claim claim = manager.getClaimAt(loc);
        if (claim == null) return flag.getDefaultValue();
        return claim.getFlag(flag);
    }

    @Override
    public Claim createClaim(Player owner, Location corner1, Location corner2) {
        return manager.createClaim(owner, corner1, corner2);
    }

    @Override
    public boolean deleteClaim(Claim claim, Player actor) {
        return manager.deleteClaim(claim, actor);
    }

    @Override
    public boolean resizeClaim(Claim claim, Player actor, int newX1, int newZ1, int newX2, int newZ2) {
        return manager.resizeClaim(claim, actor, newX1, newZ1, newX2, newZ2);
    }

    @Override
    public int getClaimBlocks(UUID player) {
        return storage.getClaimBlocks(player);
    }

    @Override
    public void addClaimBlocks(UUID player, int amount) {
        storage.addClaimBlocks(player, amount);
    }

    @Override
    public void setClaimBlocks(UUID player, int amount) {
        storage.setClaimBlocks(player, amount);
    }

    @Override
    public int getUsedClaimBlocks(UUID player) {
        return manager.getUsedClaimBlocks(player);
    }

    @Override
    public boolean addTrust(Claim claim, Player executor, UUID target, TrustLevel level) {
        return manager.setTrust(claim, executor, target, level);
    }

    @Override
    public boolean removeTrust(Claim claim, Player executor, UUID target) {
        return manager.removeTrust(claim, executor, target);
    }

    @Override
    public List<Claim> getPlayerClaims(UUID owner) {
        return manager.getPlayerClaims(owner);
    }

    @Override
    public Collection<Claim> getAllClaims() {
        return manager.getAllClaims();
    }

    @Override
    public Claim getClaimById(int id) {
        return manager.getClaimById(id);
    }

    @Override
    public boolean shouldBlockDisplayDamageAt(Location loc) {
        return manager.shouldBlockDisplayDamageAt(loc);
    }

    @Override
    public boolean shouldCorruptionSpreadAt(Location loc) {
        return manager.shouldCorruptionSpreadAt(loc);
    }

    @Override
    public boolean shouldModeEventSpawnAt(Location loc) {
        return manager.shouldModeEventSpawnAt(loc);
    }
}
