package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.chain.ChainMode;
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
 * Chain Mode admin/test commands.
 * Mirrors the structure of CalamityCommand for consistency.
 *
 * /chain status        — Show mode status
 * /chain debug         — Toggle debug mode
 * /chain test <id>     — Spawn a specific chain attack on yourself
 * /chain clearattacks  — Clear all active attacks
 * /chain spawninterval <ticks> — Set spawn interval
 * /chain toggleexempt [player] — Toggle exempt status
 * /chain list          — List all registered attack IDs
 * /chain reload        — Reload chain configs
 */
public class ChainCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public ChainCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
            default -> { sendHelp(sender); yield true; }
        };
    }

    // ========================
    // Subcommands
    // ========================

    private boolean handleStatus(CommandSender sender) {
        ChainMode mode = getChainMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
            return true;
        }

        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive()
                && modeManager.getActiveMode() instanceof ChainMode;

        sender.sendMessage(Component.text("=== Chain Mode Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"), active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(), NamedTextColor.AQUA));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));

            var world = mode.getChainWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.AQUA));
            }
        }

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        sender.sendMessage(Component.text("Block Display attacks: " + bdCount, NamedTextColor.AQUA));

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

        ChainMode mode = getChainMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            // List available IDs
            List<String> ids = mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY);
            sender.sendMessage(Component.text("Usage: /chain test <id>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Available IDs (" + ids.size() + "):", NamedTextColor.AQUA));
            // Show in columns
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
            return true;
        }

        String id = args[1].toLowerCase();
        AbstractAttack attack = mode.getAttackRegistry().get(1, AttackType.BLOCK_DISPLAY, id);
        if (attack == null) {
            sender.sendMessage(Component.text("Unknown attack: " + id, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /chain test for a list of available IDs.", NamedTextColor.GRAY));
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
        ChainMode mode = getChainMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
            return true;
        }

        int count = mode.getAttackScheduler().getActiveAttackCount();
        mode.getAttackScheduler().clearActiveAttacks();
        sender.sendMessage(Component.text("Cleared " + count + " active attacks.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        ChainMode mode = getChainMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /chain spawninterval <ticks>", NamedTextColor.YELLOW));
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
        ChainMode mode = getChainMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Usage: /chain toggleexempt <player>", NamedTextColor.YELLOW));
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
        ChainMode mode = getChainMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
            return true;
        }

        List<String> ids = mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY);
        sender.sendMessage(Component.text("=== Chain Attacks (" + ids.size() + ") ===", NamedTextColor.GOLD));
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            AbstractAttack atk = mode.getAttackRegistry().get(1, AttackType.BLOCK_DISPLAY, id);
            String info = atk != null
                    ? String.format(" (dmg=%.0f, r=%.1f, dur=%d, %s)",
                    atk.getConfig().getDamage(), atk.getConfig().getDamageRadius(),
                    atk.getConfig().getDurationTicks(),
                    atk.getConfig().isEnabled() ? "ON" : "OFF")
                    : "";
            sender.sendMessage(Component.text((i + 1) + ". " + id + info, NamedTextColor.AQUA));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        ChainMode mode = getChainMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
            return true;
        }

        mode.getChainConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Chain configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // Hide all completions if player lacks permission
        if (!sender.hasPermission("chaoscraft.chain.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filterStartsWith(args[0],
                    "status", "debug", "test", "clearattacks", "spawninterval",
                    "toggleexempt", "list", "reload");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            ChainMode mode = getChainMode();

            if ("test".equals(sub) && mode != null) {
                List<String> ids = mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY);
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

    private ChainMode getChainMode() {
        var mode = plugin.getModeManager().getMode("chain");
        return mode instanceof ChainMode cm ? cm : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Chain Mode Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/chain status — Show mode status", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/chain debug — Toggle debug mode", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/chain test <id> — Spawn a chain attack on yourself", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/chain list — List all attack IDs with config", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/chain clearattacks — Clear all active attacks", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/chain spawninterval <ticks> — Set spawn interval", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/chain toggleexempt [player] — Toggle exempt", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/chain reload — Reload chain configs", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Start/stop: /triggermode chain | /endmode", NamedTextColor.GRAY));
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
