package com.blockforge.chaoscraft.modes.bluemoon;

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
 * Blue Moon mode admin commands.
 * Mirrors chain/corruption/DD command structure.
 */
public class BlueMoonCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public BlueMoonCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.bluemoon.admin") && !sender.hasPermission("chaoscraft.admin")) {
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
            case "boss" -> handleBoss(sender, args);
            case "gimmick" -> handleGimmick(sender, args);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleStatus(CommandSender sender) {
        BlueMoonMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Blue Moon mode not registered.", NamedTextColor.RED));
            return true;
        }

        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive()
                && modeManager.getActiveMode() instanceof BlueMoonMode;

        sender.sendMessage(Component.text("=== Blue Moon Status ===", NamedTextColor.BLUE));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"), active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(), NamedTextColor.AQUA));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));

            var world = mode.getBlueMoonWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.AQUA));
            }
        }

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int envCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int bossCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BOSS).size();
        sender.sendMessage(Component.text("Block Display: " + bdCount + " | Environmental: " + envCount + " | Boss: " + bossCount, NamedTextColor.AQUA));

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

        BlueMoonMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Blue Moon mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.BOSS)) {
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
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.BOSS)) {
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
        BlueMoonMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        int count = mode.getAttackScheduler().getActiveAttackCount();
        mode.getAttackScheduler().clearActiveAttacks();
        sender.sendMessage(Component.text("Cleared " + count + " active attacks.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        BlueMoonMode mode = getMode();
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
        BlueMoonMode mode = getMode();
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
        BlueMoonMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }

        sender.sendMessage(Component.text("=== Blue Moon Attacks ===", NamedTextColor.BLUE));
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.BOSS)) {
            List<String> ids = mode.getAttackRegistry().getIds(1, type);
            if (ids.isEmpty()) continue;
            sender.sendMessage(Component.text("--- " + type.name() + " (" + ids.size() + ") ---", NamedTextColor.AQUA));
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
        BlueMoonMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        mode.getMoonConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Blue Moon configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleBoss(CommandSender sender, String[] args) {
        BlueMoonMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        var boss = mode.getBossManager();

        if (args.length < 2) {
            sender.sendMessage(Component.text("Boss commands: spawn, kill, phase <1-4>, laser, status", NamedTextColor.AQUA));
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "spawn" -> {
                var world = mode.getBlueMoonWorld();
                if (world == null) { sender.sendMessage(Component.text("World not found.", NamedTextColor.RED)); return true; }
                boss.forceSpawn(world);
                sender.sendMessage(Component.text("Boss force-spawned.", NamedTextColor.GREEN));
            }
            case "kill" -> {
                boss.forceKill();
                sender.sendMessage(Component.text("Boss force-killed.", NamedTextColor.GREEN));
            }
            case "phase" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: boss phase <1-4>", NamedTextColor.YELLOW)); return true; }
                try {
                    int phase = Integer.parseInt(args[2]);
                    if (phase < 1 || phase > 4) { sender.sendMessage(Component.text("Phase must be 1-4.", NamedTextColor.RED)); return true; }
                    boss.forcePhase(phase);
                    sender.sendMessage(Component.text("Boss forced to phase " + phase + ".", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
                }
            }
            case "laser" -> {
                boss.forceLaser();
                sender.sendMessage(Component.text("Lunar Super Laser triggered!", NamedTextColor.LIGHT_PURPLE));
            }
            case "status" -> {
                sender.sendMessage(Component.text("--- Boss Status ---", NamedTextColor.BLUE));
                sender.sendMessage(Component.text("Alive: " + boss.isBossAlive(), boss.isBossAlive() ? NamedTextColor.GREEN : NamedTextColor.RED));
                sender.sendMessage(Component.text("Phase: " + boss.getCurrentPhase(), NamedTextColor.AQUA));
                sender.sendMessage(Component.text("HP: " + String.format("%.0f%%", boss.getHealthRatio() * 100), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("Laser active: " + boss.isLaserActive(), NamedTextColor.LIGHT_PURPLE));
            }
            default -> sender.sendMessage(Component.text("Unknown boss command. Use: spawn, kill, phase, laser, status", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleGimmick(CommandSender sender, String[] args) {
        BlueMoonMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        var gimmicks = mode.getGimmickManager();

        if (args.length < 2) {
            sender.sendMessage(Component.text("Gimmick commands: list, toggle <name>", NamedTextColor.AQUA));
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "list" -> {
                sender.sendMessage(Component.text("=== Gimmicks (" + gimmicks.getEnabledGimmicks().size() + "/" + gimmicks.getGimmickNames().size() + " enabled) ===", NamedTextColor.BLUE));
                for (String name : gimmicks.getGimmickNames()) {
                    boolean enabled = gimmicks.isGimmickEnabled(name);
                    sender.sendMessage(Component.text("  " + name + ": " + (enabled ? "ON" : "OFF"),
                            enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY));
                }
            }
            case "toggle" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: gimmick toggle <name>", NamedTextColor.YELLOW)); return true; }
                String name = args[2].toLowerCase();
                if (!gimmicks.getGimmickNames().contains(name)) {
                    sender.sendMessage(Component.text("Unknown gimmick: " + name, NamedTextColor.RED));
                    return true;
                }
                gimmicks.toggleGimmick(name);
                boolean nowEnabled = gimmicks.isGimmickEnabled(name);
                sender.sendMessage(Component.text(name + " is now " + (nowEnabled ? "ON" : "OFF"),
                        nowEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            default -> sender.sendMessage(Component.text("Unknown gimmick command. Use: list, toggle <name>", NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.bluemoon.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartsWith(args[0], "status", "debug", "test", "clearattacks",
                    "spawninterval", "toggleexempt", "list", "reload", "boss", "gimmick");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            BlueMoonMode mode = getMode();

            if ("test".equals(sub) && mode != null) {
                List<String> ids = new ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BOSS));
                return filterStartsWith(args[1], ids);
            }
            if ("toggleexempt".equals(sub)) return null;
            if ("spawninterval".equals(sub)) return List.of("20", "40", "60", "100");
            if ("boss".equals(sub)) return filterStartsWith(args[1], "spawn", "kill", "phase", "laser", "status");
            if ("gimmick".equals(sub)) return filterStartsWith(args[1], "list", "toggle");
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            BlueMoonMode mode = getMode();
            if ("boss".equals(sub) && "phase".equals(args[1].toLowerCase())) {
                return List.of("1", "2", "3", "4");
            }
            if ("gimmick".equals(sub) && "toggle".equals(args[1].toLowerCase()) && mode != null) {
                return filterStartsWith(args[2], mode.getGimmickManager().getGimmickNames());
            }
        }

        return Collections.emptyList();
    }

    // Helpers
    private BlueMoonMode getMode() {
        var mode = plugin.getModeManager().getMode("bluemoon");
        return mode instanceof BlueMoonMode bm ? bm : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Blue Moon Commands ===", NamedTextColor.BLUE));
        sender.sendMessage(Component.text("status — Mode status", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("debug — Toggle debug mode", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("test <id> — Spawn attack on yourself", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("list — List all attack IDs", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("clearattacks — Clear active attacks", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("spawninterval <ticks> — Set spawn interval", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("toggleexempt [player] — Toggle exempt", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("reload — Reload configs", NamedTextColor.AQUA));
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
