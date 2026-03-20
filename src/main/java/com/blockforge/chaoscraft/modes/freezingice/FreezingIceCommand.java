package com.blockforge.chaoscraft.modes.freezingice;

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
 * Freezing Ice mode admin commands.
 * Mirrors chain/corruption/DD/bluemoon command structure.
 */
public class FreezingIceCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public FreezingIceCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.freezingice.admin") && !sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
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
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleStatus(CommandSender sender) {
        FreezingIceMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Freezing Ice mode not registered.", NamedTextColor.RED));
            return true;
        }

        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive()
                && modeManager.getActiveMode() instanceof FreezingIceMode;

        sender.sendMessage(Component.text("=== Freezing Ice Status ===", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"), active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(), NamedTextColor.WHITE));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));

            var world = mode.getIceWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.WHITE));
            }
        }

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int envCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int meCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
        sender.sendMessage(Component.text("Block Display: " + bdCount + " | Environmental: " + envCount + " | Model Engine: " + meCount, NamedTextColor.WHITE));

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

        FreezingIceMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Freezing Ice mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
                List<String> ids = mode.getAttackRegistry().getIds(1, type);
                if (!ids.isEmpty()) {
                    sender.sendMessage(Component.text(type.name() + " (" + ids.size() + "):", NamedTextColor.AQUA));
                    sendIdList(sender, ids);
                }
            }
            return true;
        }

        String id = args[1].toLowerCase();
        AbstractAttack attack = null;
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
            attack = mode.getAttackRegistry().get(1, type, id);
            if (attack != null) break;
        }

        if (attack == null) {
            sender.sendMessage(Component.text("Unknown attack: " + id, NamedTextColor.RED));
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
        FreezingIceMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        int count = mode.getAttackScheduler().getActiveAttackCount();
        mode.getAttackScheduler().clearActiveAttacks();
        sender.sendMessage(Component.text("Cleared " + count + " active attacks.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        FreezingIceMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        if (args.length < 2) { sender.sendMessage(Component.text("Usage: spawninterval <ticks>", NamedTextColor.YELLOW)); return true; }
        try {
            int ticks = Math.max(1, Integer.parseInt(args[1]));
            mode.getAttackScheduler().setBaseSpawnInterval(ticks);
            sender.sendMessage(Component.text("Spawn interval set to " + ticks + " ticks.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleToggleExempt(CommandSender sender, String[] args) {
        FreezingIceMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED)); return true; }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Usage: toggleexempt <player>", NamedTextColor.YELLOW));
            return true;
        }
        if (mode.isExempt(target)) {
            mode.removeExempt(target);
            sender.sendMessage(Component.text(target.getName() + " no longer exempt.", NamedTextColor.YELLOW));
        } else {
            mode.addExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is now exempt.", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        FreezingIceMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }

        sender.sendMessage(Component.text("=== Freezing Ice Attacks ===", NamedTextColor.AQUA));
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
            List<String> ids = mode.getAttackRegistry().getIds(1, type);
            if (ids.isEmpty()) continue;
            sender.sendMessage(Component.text("--- " + type.name() + " (" + ids.size() + ") ---", NamedTextColor.WHITE));
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                AbstractAttack atk = mode.getAttackRegistry().get(1, type, id);
                String info = atk != null
                        ? String.format(" (dmg=%.0f, r=%.1f, dur=%d, %s)",
                        atk.getConfig().getDamage(), atk.getConfig().getDamageRadius(),
                        atk.getConfig().getDurationTicks(), atk.getConfig().isEnabled() ? "ON" : "OFF")
                        : "";
                sender.sendMessage(Component.text((i + 1) + ". " + id + info, NamedTextColor.GRAY));
            }
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        FreezingIceMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        mode.getIceConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Freezing Ice configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.freezingice.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartsWith(args[0], "status", "debug", "test", "clearattacks",
                    "spawninterval", "toggleexempt", "list", "reload");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            FreezingIceMode mode = getMode();

            if ("test".equals(sub) && mode != null) {
                List<String> ids = new ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE));
                return filterStartsWith(args[1], ids);
            }
            if ("toggleexempt".equals(sub)) return null;
            if ("spawninterval".equals(sub)) return List.of("20", "40", "60", "100");
        }

        return Collections.emptyList();
    }

    // Helpers
    private FreezingIceMode getMode() {
        var mode = plugin.getModeManager().getMode("freezingice");
        return mode instanceof FreezingIceMode fi ? fi : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Freezing Ice Commands ===", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("status — Mode status", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("debug — Toggle debug mode", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("test <id> — Spawn attack on yourself", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("list — List all attack IDs", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("clearattacks — Clear active attacks", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("spawninterval <ticks> — Set spawn interval", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("toggleexempt [player] — Toggle exempt", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("reload — Reload configs", NamedTextColor.WHITE));
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
        if (sb.length() > 0) sender.sendMessage(Component.text(sb.toString(), NamedTextColor.GRAY));
    }

    private List<String> filterStartsWith(String input, String... options) {
        return filterStartsWith(input, Arrays.asList(options));
    }

    private List<String> filterStartsWith(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
