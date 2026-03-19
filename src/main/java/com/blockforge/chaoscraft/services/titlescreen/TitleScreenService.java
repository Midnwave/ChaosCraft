package com.blockforge.chaoscraft.services.titlescreen;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the title-screen experience: sessions, event blocking,
 * script execution, and left-click interactions.
 */
public class TitleScreenService implements Listener {

    private final ChaosCraftPlugin plugin;
    private TitleScreenConfig config;
    private final PingTrackerService pingTracker;
    private final Map<UUID, TitleScreenSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> activeScripts = new ConcurrentHashMap<>();

    public TitleScreenService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.pingTracker = new PingTrackerService(plugin);
        this.pingTracker.start();
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        this.config = new TitleScreenConfig(plugin.yaml("titlescreen.yml"));
    }

    // ---- Session management ----

    public void startSession(Player player) {
        if (!sessions.containsKey(player.getUniqueId())) {
            TitleScreenSession session = new TitleScreenSession(this, player);
            sessions.put(player.getUniqueId(), session);
            session.start();
        }
    }

    public Optional<TitleScreenSession> getSession(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public void endSession(UUID playerId) {
        TitleScreenSession session = sessions.remove(playerId);
        if (session != null) {
            session.exitTitleScreen();
        }
        BukkitRunnable script = activeScripts.remove(playerId);
        if (script != null && !script.isCancelled()) {
            script.cancel();
        }
    }

    public boolean isInTitleScreen(UUID playerId) {
        return getSession(playerId).map(TitleScreenSession::isInTitleScreen).orElse(false);
    }

    public boolean isLoading(UUID playerId) {
        return getSession(playerId).map(TitleScreenSession::isLoading).orElse(false);
    }

    // ---- Event handlers ----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> startSession(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        endSession(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!config.isMovementDisabled()) return;

        Player player = event.getPlayer();
        if (!isInTitleScreen(player.getUniqueId())) return;

        // Block position changes (allow head rotation)
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID playerId = sender.getUniqueId();

        // Check verification first
        getSession(playerId).ifPresent(session -> {
            if (session.handleVerificationAttempt(event.getMessage())) {
                event.setCancelled(true);
            }
        });
        if (event.isCancelled()) return;

        // Chat blocking for title-screen players
        if (config.isChatDisabled()) {
            if (isInTitleScreen(playerId)) {
                event.setCancelled(true);
                return;
            }
            event.getRecipients().removeIf(recipient -> isInTitleScreen(recipient.getUniqueId()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (config.isUntargetable()) {
            Entity target = event.getTarget();
            if (target instanceof Player player) {
                if (isInTitleScreen(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (config.isCommandsDisabled()) {
            Player player = event.getPlayer();
            if (isInTitleScreen(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot use commands while in the title screen.", NamedTextColor.RED));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeftClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        getSession(player.getUniqueId()).ifPresent(session -> {
            if (session.isInTitleScreen() && !session.isLoading()) {
                event.setCancelled(true);
                handleLeftClickInteractions(player);
            }
        });
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        getSession(player.getUniqueId()).ifPresent(session -> {
            switch (event.getStatus()) {
                case ACCEPTED -> {
                    session.setResourcePackLoading(true);
                    session.setResourcePackProgress(0);
                }
                case SUCCESSFULLY_LOADED -> {
                    session.setResourcePackProgress(100);
                    session.setResourcePackLoading(false);
                }
                case FAILED_DOWNLOAD, DECLINED, INVALID_URL, FAILED_RELOAD, DISCARDED -> {
                    session.setResourcePackLoading(false);
                    session.setResourcePackProgress(0);
                }
            }
        });
    }

    // ---- Left-click interactions ----

    private void handleLeftClickInteractions(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return;

        for (TitleScreenConfig.LeftClickInteraction interaction : config.getLeftClickInteractions()) {
            boolean allConditionsMet = true;
            for (var condition : interaction.getConditions().entrySet()) {
                String placeholder = condition.getKey();
                String expectedValue = condition.getValue();
                String actualValue = PlaceholderAPI.setPlaceholders(player, "%" + placeholder + "%");
                if (!actualValue.equals(expectedValue)) {
                    allConditionsMet = false;
                    break;
                }
            }
            if (allConditionsMet) {
                executeScriptActions(interaction.getActions(), player, null);
                break;
            }
        }
    }

    // ---- Script execution ----

    public void executeScript(String scriptName, Player player) {
        executeScript(scriptName, player, null);
    }

    public void executeScript(String scriptName, Player player, Runnable onComplete) {
        List<TitleScreenConfig.ScriptAction> actions = config.getScript(scriptName);
        if (actions.isEmpty()) {
            plugin.getLogger().warning("Script '" + scriptName + "' is empty or does not exist for " + player.getName());
            if (onComplete != null) onComplete.run();
            return;
        }
        plugin.getLogger().info("Executing script '" + scriptName + "' for " + player.getName() + " (" + actions.size() + " actions)");
        executeScriptActions(actions, player, onComplete);
    }

    private void executeScriptActions(List<TitleScreenConfig.ScriptAction> actions, Player player, Runnable onComplete) {
        UUID playerId = player.getUniqueId();

        // Cancel any existing script for this player
        BukkitRunnable existing = activeScripts.remove(playerId);
        if (existing != null && !existing.isCancelled()) {
            existing.cancel();
        }

        BukkitRunnable executor = new BukkitRunnable() {
            private int actionIndex = 0;

            @Override
            public void run() {
                while (player.isOnline()) {
                    if (actionIndex >= actions.size()) {
                        cancel();
                        activeScripts.remove(playerId);
                        plugin.getLogger().info("Script completed for " + player.getName());
                        if (onComplete != null) {
                            plugin.getLogger().info("Calling completion callback for " + player.getName());
                            onComplete.run();
                        }
                        return;
                    }

                    TitleScreenConfig.ScriptAction action = actions.get(actionIndex);
                    actionIndex++;

                    switch (action.getType()) {
                        case EXECUTE_COMMAND -> {
                            String cmd = action.getValue().replace("%player%", player.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        }
                        case WAIT -> {
                            try {
                                String waitValue = action.getValue();
                                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                                    waitValue = PlaceholderAPI.setPlaceholders(player, waitValue);
                                }
                                int ticks = Integer.parseInt(waitValue);
                                actionIndex--; // Will re-read same action but skip past it next time
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    actionIndex++;
                                    this.run();
                                }, ticks);
                                return;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        case NEXTPART -> {
                            getSession(playerId).ifPresent(TitleScreenSession::openNextPartGate);
                        }
                        case EXITTITLESCREEN -> {
                            getSession(playerId).ifPresent(session -> {
                                session.exitTitleScreen();
                                endSession(playerId);
                            });
                        }
                    }
                }

                // Player went offline
                cancel();
                activeScripts.remove(playerId);
                if (onComplete != null) onComplete.run();
            }
        };

        activeScripts.put(playerId, executor);
        executor.runTask(plugin);
    }

    // ---- Accessors ----

    public ChaosCraftPlugin getPlugin() { return plugin; }
    public TitleScreenConfig getConfig() { return config; }
    public PingTrackerService getPingTracker() { return pingTracker; }
}
