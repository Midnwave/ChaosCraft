package com.blockforge.chaoscraft.modes.devilsdream;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Devil's Dream mode admin/test commands.
 * Mirrors the structure of ChainCommand/CorruptionCommand for consistency.
 *
 * Subcommands:
 *   status            — Show mode status + adaptation stats
 *   debug             — Toggle debug mode
 *   test <id>         — Spawn a specific attack on yourself
 *   clearattacks      — Clear all active attacks
 *   spawninterval <t> — Set spawn interval (ticks)
 *   toggleexempt [p]  — Toggle exempt status
 *   list              — List all registered attack IDs
 *   reload            — Reload configs
 *   adaptation        — Show dream adaptation tracker stats
 *   resetadaptation   — Reset all adaptation scores
 */
public class DevilsDreamCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public DevilsDreamCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.devilsdream.admin") && !sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "status" -> handleStatus(sender);
            case "debug" -> handleDebug(sender);
            case "test" -> handleTest(sender, args);
            case "clearattacks" -> handleClearAttacks(sender);
            case "spawninterval" -> handleSpawnInterval(sender, args);
            case "toggleexempt" -> handleToggleExempt(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "adaptation" -> handleAdaptation(sender);
            case "resetadaptation" -> handleResetAdaptation(sender);
            default -> { sendHelp(sender); yield true; }
        };
    }

    // ========================
    // Subcommands
    // ========================

    private boolean handleStatus(CommandSender sender) {
        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive()
                && modeManager.getActiveMode() instanceof DevilsDreamMode;

        sender.sendMessage(Component.text("=== Devil's Dream Status ===", NamedTextColor.DARK_RED));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"), active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(), NamedTextColor.RED));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));

            var world = mode.getDreamWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.AQUA));
            }

            // Adaptation stats
            sender.sendMessage(Component.text("--- Dream Adaptation ---", NamedTextColor.DARK_RED));
            sender.sendMessage(Component.text("Use 'adaptation' subcommand for detailed scores.", NamedTextColor.GOLD));
        }

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int envCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        sender.sendMessage(Component.text("Block Display attacks: " + bdCount, NamedTextColor.RED));
        sender.sendMessage(Component.text("Environmental attacks: " + envCount, NamedTextColor.RED));

        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        boolean current = plugin.getConfig().getBoolean("debug", false);
        plugin.getConfig().set("debug", !current);
        plugin.saveConfig();
        sender.sendMessage(Component.text("Debug mode: " + (!current ? "ON" : "OFF"),
                !current ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }

        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            // List available IDs for both types
            List<String> bdIds = mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY);
            List<String> envIds = mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL);
            sender.sendMessage(Component.text("Usage: /devilsdream test <id>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Block Display attacks (" + bdIds.size() + "):", NamedTextColor.RED));
            sendIdList(sender, bdIds);
            sender.sendMessage(Component.text("Environmental attacks (" + envIds.size() + "):", NamedTextColor.GOLD));
            sendIdList(sender, envIds);
            return true;
        }

        String id = args[1].toLowerCase();
        // Try both attack types
        AbstractAttack attack = mode.getAttackRegistry().get(1, AttackType.BLOCK_DISPLAY, id);
        if (attack == null) {
            attack = mode.getAttackRegistry().get(1, AttackType.ENVIRONMENTAL, id);
        }
        if (attack == null) {
            sender.sendMessage(Component.text("Unknown attack: " + id, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use test without args for a list of IDs.", NamedTextColor.GRAY));
            return true;
        }

        mode.getAttackScheduler().forceSpawn(attack, player);
        sender.sendMessage(Component.text("Spawned: " + id, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Damage: " + attack.getConfig().getDamage()
                + " HP, Radius: " + attack.getConfig().getDamageRadius()
                + ", Duration: " + attack.getConfig().getDurationTicks() + " ticks", NamedTextColor.GRAY));
        return true;
    }

    private boolean handleClearAttacks(CommandSender sender) {
        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        int count = mode.getAttackScheduler().getActiveAttackCount();
        mode.getAttackScheduler().clearActiveAttacks();
        sender.sendMessage(Component.text("Cleared " + count + " active dream attacks.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: spawninterval <ticks>", NamedTextColor.YELLOW));
            return true;
        }

        try {
            int ticks = Integer.parseInt(args[1]);
            if (ticks < 1) ticks = 1;
            mode.getAttackScheduler().setBaseSpawnInterval(ticks);
            sender.sendMessage(Component.text("Spawn interval set to " + ticks + " ticks.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleToggleExempt(CommandSender sender, String[] args) {
        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Usage: toggleexempt <player>", NamedTextColor.YELLOW));
            return true;
        }

        if (mode.isExempt(target)) {
            mode.removeExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is no longer exempt.", NamedTextColor.YELLOW));
        } else {
            mode.addExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is now exempt.", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("=== Devil's Dream Attacks ===", NamedTextColor.DARK_RED));

        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL)) {
            List<String> ids = mode.getAttackRegistry().getIds(1, type);
            sender.sendMessage(Component.text("--- " + type.name() + " (" + ids.size() + ") ---", NamedTextColor.RED));
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                AbstractAttack atk = mode.getAttackRegistry().get(1, type, id);
                String info = atk != null
                        ? String.format(" (dmg=%.0f, r=%.1f, dur=%d, %s)",
                        atk.getConfig().getDamage(), atk.getConfig().getDamageRadius(),
                        atk.getConfig().getDurationTicks(),
                        atk.getConfig().isEnabled() ? "ON" : "OFF")
                        : "";
                sender.sendMessage(Component.text((i + 1) + ". " + id + info, NamedTextColor.GOLD));
            }
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        mode.getDreamConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Devil's Dream configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAdaptation(CommandSender sender) {
        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        var tracker = mode.getAdaptationTracker();
        if (tracker == null) {
            sender.sendMessage(Component.text("Adaptation tracker not initialized.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("=== Dream Adaptation Stats ===", NamedTextColor.DARK_RED));

        // Show per-player scores if sender is a player
        if (sender instanceof Player player) {
            int total = tracker.getTotalScore(player.getUniqueId());
            if (total > 0) {
                sender.sendMessage(Component.text("Your scores (total: " + total + "):", NamedTextColor.RED));
                for (DreamAction action : DreamAction.values()) {
                    int score = tracker.getScore(player.getUniqueId(), action);
                    boolean aboveThreshold = tracker.isAboveThreshold(player.getUniqueId(), action);
                    sender.sendMessage(Component.text("  " + action.name() + ": " + score
                                    + (aboveThreshold ? " [ESCALATED]" : ""),
                            aboveThreshold ? NamedTextColor.YELLOW : NamedTextColor.GOLD));
                }
                DreamAction dominant = tracker.getDominantAction(player.getUniqueId());
                sender.sendMessage(Component.text("Dominant action: " + dominant.name(), NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("No adaptation data for you yet.", NamedTextColor.GRAY));
            }
        }

        return true;
    }

    private boolean handleResetAdaptation(CommandSender sender) {
        DevilsDreamMode mode = getDreamMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Devil's Dream mode not registered.", NamedTextColor.RED));
            return true;
        }

        var tracker = mode.getAdaptationTracker();
        if (tracker != null) {
            tracker.resetAll();
            sender.sendMessage(Component.text("All adaptation scores reset.", NamedTextColor.GREEN));
        }
        return true;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.devilsdream.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartsWith(args[0],
                    "status", "debug", "test", "clearattacks", "spawninterval",
                    "toggleexempt", "list", "reload", "adaptation", "resetadaptation");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            DevilsDreamMode mode = getDreamMode();

            if ("test".equals(sub) && mode != null) {
                List<String> ids = new ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                return filterStartsWith(args[1], ids);
            }

            if ("toggleexempt".equals(sub)) {
                return null; // Bukkit provides player name completion
            }

            if ("spawninterval".equals(sub)) {
                return List.of("20", "40", "60", "100");
            }
        }

        return Collections.emptyList();
    }

    // ========================
    // Helpers
    // ========================

    private DevilsDreamMode getDreamMode() {
        var mode = plugin.getModeManager().getMode("devilsdream");
        return mode instanceof DevilsDreamMode dm ? dm : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Devil's Dream Commands ===", NamedTextColor.DARK_RED));
        sender.sendMessage(Component.text("status — Show mode status + adaptation stats", NamedTextColor.RED));
        sender.sendMessage(Component.text("debug — Toggle debug mode", NamedTextColor.RED));
        sender.sendMessage(Component.text("test <id> — Spawn a dream attack on yourself", NamedTextColor.RED));
        sender.sendMessage(Component.text("list — List all attack IDs with config", NamedTextColor.RED));
        sender.sendMessage(Component.text("clearattacks — Clear all active attacks", NamedTextColor.RED));
        sender.sendMessage(Component.text("spawninterval <ticks> — Set spawn interval", NamedTextColor.RED));
        sender.sendMessage(Component.text("toggleexempt [player] — Toggle exempt", NamedTextColor.RED));
        sender.sendMessage(Component.text("reload — Reload dream configs", NamedTextColor.RED));
        sender.sendMessage(Component.text("adaptation — Show dream adaptation stats", NamedTextColor.RED));
        sender.sendMessage(Component.text("resetadaptation — Reset all adaptation scores", NamedTextColor.RED));
    }

    private void sendIdList(CommandSender sender, List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(", ");
            if ((i + 1) % 5 == 0) {
                sender.sendMessage(Component.text(sb.toString(), NamedTextColor.GRAY));
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) {
            sender.sendMessage(Component.text(sb.toString(), NamedTextColor.GRAY));
        }
    }

    private List<String> filterStartsWith(String input, String... options) {
        return filterStartsWith(input, Arrays.asList(options));
    }

    private List<String> filterStartsWith(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
