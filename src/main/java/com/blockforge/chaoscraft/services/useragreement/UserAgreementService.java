package com.blockforge.chaoscraft.services.useragreement;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks whether each player has accepted the user agreement this session.
 * Acceptance resets on every join so it must be re-confirmed each login.
 */
public class UserAgreementService implements Listener {

    private final ChaosCraftPlugin plugin;
    private final Set<UUID> acceptedPlayers = new HashSet<>();

    public UserAgreementService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("User Agreement Service initialized.");
    }

    public boolean hasAccepted(UUID playerId) {
        return acceptedPlayers.contains(playerId);
    }

    public void acceptAgreement(UUID playerId) {
        acceptedPlayers.add(playerId);
    }

    private void resetAcceptance(UUID playerId) {
        acceptedPlayers.remove(playerId);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        resetAcceptance(playerId);

        if (plugin.getPlayService() != null) {
            plugin.getPlayService().clearPlayMessage(playerId);
        }

        plugin.getLogger().info(event.getPlayer().getName() + " must accept user agreement.");
    }

    public ChaosCraftPlugin getPlugin() { return plugin; }
}
