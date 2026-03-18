package com.blockforge.chaoscraft.services.codes;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Intercepts chat messages from players in a code-entry session
 * and routes them to the CodesService for redemption processing.
 */
public class CodesChatListener implements Listener {

    private final ChaosCraftPlugin plugin;
    private final CodesService codesService;

    public CodesChatListener(ChaosCraftPlugin plugin, CodesService codesService) {
        this.plugin = plugin;
        this.codesService = codesService;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!codesService.isInSession(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CodesService.CodeRedemptionResult result = codesService.attemptRedemption(player, message);
            player.sendMessage(Component.text(result.message()));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        codesService.endSession(event.getPlayer().getUniqueId());
    }
}
