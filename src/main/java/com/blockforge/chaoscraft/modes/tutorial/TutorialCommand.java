package com.blockforge.chaoscraft.modes.tutorial;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin commands for Tutorial Mode.
 * Accessed via /cc modes tutorial <subcommand>
 */
public class TutorialCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public TutorialCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.tutorial.admin") && !sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) { sendHelp(sender); return true; }

        return switch (args[0].toLowerCase()) {
            case "status" -> handleStatus(sender);
            case "debug" -> handleDebug(sender);
            case "test" -> handleTest(sender, args);
            case "clearattacks" -> handleClearAttacks(sender);
            case "spawninterval" -> handleSpawnInterval(sender, args);
            case "toggleexempt" -> handleToggleExempt(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "designs" -> handleDesigns(sender);
            case "forcedesign" -> handleForceDesign(sender, args);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleStatus(CommandSender sender) {
        TutorialMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Tutorial mode not registered.", NamedTextColor.RED)); return true; }

        sender.sendMessage(Component.text("=== Tutorial Mode Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  State: " + mode.getState(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Attacks registered: " + mode.getAttackRegistry().size(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Active displays: " + mode.getAttackScheduler().getActiveCount(), NamedTextColor.GRAY));

        if (mode.isActive()) {
            sender.sendMessage(Component.text("  Active design: " + mode.getActiveDesignName(), NamedTextColor.YELLOW));
            // Show player progress
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                var pp = mode.getTracker().getProgress(p.getUniqueId());
                if (pp != null) {
                    String status = pp.isCompleted() ? "COMPLETE" : "Step " + (pp.getCurrentStepIndex() + 1) + "/" + pp.getDesign().getStepCount();
                    sender.sendMessage(Component.text("  " + p.getName() + ": " + status, NamedTextColor.AQUA));
                }
            }
        }
        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        boolean current = plugin.getConfig().getBoolean("debug", false);
        plugin.getConfig().set("debug", !current);
        plugin.saveConfig();
        sender.sendMessage(Component.text("Debug: " + (!current ? "ON" : "OFF"), NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED)); return true; }
        TutorialMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Tutorial mode not registered.", NamedTextColor.RED)); return true; }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: test <attack_id>", NamedTextColor.RED));
            return true;
        }

        String id = args[1].toLowerCase();
        AbstractAttack attack = mode.getAttackRegistry().getAll().stream()
                .filter(a -> a.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (attack == null) {
            sender.sendMessage(Component.text("Attack not found: " + id, NamedTextColor.RED));
            return true;
        }

        mode.getAttackScheduler().forceSpawn(attack, player);
        sender.sendMessage(Component.text("Spawned: " + id, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleClearAttacks(CommandSender sender) {
        TutorialMode mode = getMode();
        if (mode == null) return true;
        mode.getAttackScheduler().clearAll();
        sender.sendMessage(Component.text("Cleared all active displays.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        TutorialMode mode = getMode();
        if (mode == null) return true;
        if (args.length < 2) { sender.sendMessage(Component.text("Usage: spawninterval <ticks>", NamedTextColor.RED)); return true; }
        try {
            int ticks = Integer.parseInt(args[1]);
            mode.getAttackScheduler().setSpawnInterval(ticks);
            sender.sendMessage(Component.text("Spawn interval set to " + ticks + " ticks.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleToggleExempt(CommandSender sender, String[] args) {
        TutorialMode mode = getMode();
        if (mode == null) return true;
        if (args.length < 2) { sender.sendMessage(Component.text("Usage: toggleexempt <player>", NamedTextColor.RED)); return true; }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) { sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED)); return true; }
        if (mode.isExempt(target)) {
            mode.removeExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is no longer exempt.", NamedTextColor.GREEN));
        } else {
            mode.addExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is now exempt.", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        TutorialMode mode = getMode();
        if (mode == null) return true;
        AttackRegistry reg = mode.getAttackRegistry();
        sender.sendMessage(Component.text("=== Tutorial Attacks (" + reg.size() + ") ===", NamedTextColor.GOLD));
        for (AbstractAttack a : reg.getAll()) {
            String status = a.getConfig().isEnabled() ? "✓" : "✗";
            sender.sendMessage(Component.text("  " + status + " " + a.getId(), NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        TutorialMode mode = getMode();
        if (mode == null) return true;
        mode.getTutorialConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Tutorial config reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleDesigns(CommandSender sender) {
        sender.sendMessage(Component.text("=== Tutorial Designs (20) ===", NamedTextColor.GOLD));
        for (TutorialDesign design : TutorialDesign.getAllDesigns().values()) {
            sender.sendMessage(Component.text("  " + design.getDesignId() + " — "
                    + design.getDisplayName() + " (" + design.getStepCount() + " steps)", NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean handleForceDesign(CommandSender sender, String[] args) {
        TutorialMode mode = getMode();
        if (mode == null) return true;
        if (args.length < 2) { sender.sendMessage(Component.text("Usage: forcedesign <name>", NamedTextColor.RED)); return true; }
        String name = args[1].toLowerCase();
        TutorialDesign design = TutorialDesign.getByName(name);
        if (design == null) {
            sender.sendMessage(Component.text("Unknown design: " + name, NamedTextColor.RED));
            return true;
        }
        mode.setForcedDesign(name);
        sender.sendMessage(Component.text("Next run will use: " + design.getDisplayName(), NamedTextColor.GREEN));
        return true;
    }

    private TutorialMode getMode() {
        var mode = plugin.getModeManager().getMode("tutorial");
        return (mode instanceof TutorialMode t) ? t : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Tutorial Mode Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  status — Show mode status + player progress", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  debug — Toggle debug logging", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  test <id> — Test a block display", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  clearattacks — Clear active displays", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  spawninterval <ticks> — Set spawn interval", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  toggleexempt <player> — Toggle exempt", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  list — List all attacks", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  reload — Reload config", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  designs — List all 20 designs", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  forcedesign <name> — Force design next run", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "debug", "test", "clearattacks", "spawninterval",
                    "toggleexempt", "list", "reload", "designs", "forcedesign");
        }
        if (args.length == 2) {
            if ("test".equals(args[0])) {
                TutorialMode mode = getMode();
                if (mode != null) {
                    return mode.getAttackRegistry().getAll().stream()
                            .map(AbstractAttack::getId)
                            .filter(id -> id.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if ("forcedesign".equals(args[0])) {
                return TutorialDesign.getAllDesigns().keySet().stream()
                        .filter(k -> k.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if ("toggleexempt".equals(args[0])) {
                return null; // default player completion
            }
        }
        return List.of();
    }
}
