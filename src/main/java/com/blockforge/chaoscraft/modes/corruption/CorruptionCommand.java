package com.blockforge.chaoscraft.modes.corruption;

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
 * Corrupted Corruption mode admin/test commands.
 * Mirrors the structure of ChainCommand for consistency.
 *
 * /corruption status            — Show mode status + corruption engine stats
 * /corruption debug             — Toggle debug mode
 * /corruption test <id>         — Spawn a specific corruption attack on yourself
 * /corruption clearattacks      — Clear all active attacks
 * /corruption spawninterval <t> — Set spawn interval (ticks)
 * /corruption toggleexempt [p]  — Toggle exempt status
 * /corruption list              — List all registered attack IDs
 * /corruption reload            — Reload corruption configs
 * /corruption corruption        — Show corruption engine stats (placeholder)
 * /corruption restore           — Force-trigger block restoration (placeholder)
 */
public class CorruptionCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public CorruptionCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.corruption.admin") && !sender.hasPermission("chaoscraft.admin")) {
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
            case "corruption" -> handleCorruptionStats(sender);
            case "restore" -> handleRestore(sender);
            default -> { sendHelp(sender); yield true; }
        };
    }

    // ========================
    // Subcommands
    // ========================

    private boolean handleStatus(CommandSender sender) {
        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
            return true;
        }

        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive()
                && modeManager.getActiveMode() instanceof CorruptionMode;

        sender.sendMessage(Component.text("=== Corruption Mode Status ===", NamedTextColor.DARK_PURPLE));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"), active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(), NamedTextColor.AQUA));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));

            var world = mode.getCorruptionWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.AQUA));
            }

            // Corruption engine stats placeholder
            sender.sendMessage(Component.text("--- Corruption Engine ---", NamedTextColor.DARK_PURPLE));
            sender.sendMessage(Component.text("Floating blocks: " + (mode.getCorruptionConfig().isFloatingBlocksEnabled() ? "ON" : "OFF"), NamedTextColor.LIGHT_PURPLE));
            sender.sendMessage(Component.text("Block replacement: " + (mode.getCorruptionConfig().isBlockReplacementEnabled() ? "ON" : "OFF"), NamedTextColor.LIGHT_PURPLE));
            sender.sendMessage(Component.text("Mob glitch: " + (mode.getCorruptionConfig().isMobGlitchEnabled() ? "ON" : "OFF"), NamedTextColor.LIGHT_PURPLE));
            var engine = mode.getCorruptionEngine();
            if (engine != null && engine.isRunning()) {
                sender.sendMessage(Component.text("Active floating blocks: " + engine.getActiveFloatingCount(), NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Corrupted blocks: " + engine.getRestorer().getStoredCount(), NamedTextColor.GRAY));
                sender.sendMessage(Component.text(engine.getStats(), NamedTextColor.GRAY));
            } else {
                sender.sendMessage(Component.text("Engine: not active", NamedTextColor.GRAY));
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

        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            // List available IDs
            List<String> ids = new java.util.ArrayList<>();
            ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
            ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
            ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE));
            sender.sendMessage(Component.text("Usage: /corruption test <id>", NamedTextColor.YELLOW));
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
        if (attack == null) attack = mode.getAttackRegistry().get(1, AttackType.ENVIRONMENTAL, id);
        if (attack == null) attack = mode.getAttackRegistry().get(1, AttackType.MODEL_ENGINE, id);
        if (attack == null) {
            sender.sendMessage(Component.text("Unknown attack: " + id, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /corruption test for a list of available IDs.", NamedTextColor.GRAY));
            return true;
        }

        mode.getAttackScheduler().forceSpawn(attack, player);
        sender.sendMessage(Component.text("Spawned: " + id, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Damage: " + attack.getConfig().getDamage()
                + " HP, Radius: " + attack.getConfig().getDamageRadius()
                + ", Duration: " + attack.getConfig().getDurationTicks() + " ticks"
                + ", ME Scale: " + attack.getConfig().getModelengineScale(), NamedTextColor.GRAY));
        return true;
    }

    private boolean handleClearAttacks(CommandSender sender) {
        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
            return true;
        }

        int count = mode.getAttackScheduler().getActiveAttackCount();
        mode.getAttackScheduler().clearActiveAttacks();
        sender.sendMessage(Component.text("Cleared " + count + " active corruption attacks.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /corruption spawninterval <ticks>", NamedTextColor.YELLOW));
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
        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Usage: /corruption toggleexempt <player>", NamedTextColor.YELLOW));
            return true;
        }

        if (mode.isExempt(target)) {
            mode.removeExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is no longer exempt from corruption.", NamedTextColor.YELLOW));
        } else {
            mode.addExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is now exempt from corruption.", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
            return true;
        }

        // Collect all attack types
        for (AttackType attackType : new AttackType[]{AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE}) {
            List<String> ids = mode.getAttackRegistry().getIds(1, attackType);
            if (ids.isEmpty()) continue;
            sender.sendMessage(Component.text("=== " + attackType.name() + " (" + ids.size() + ") ===", NamedTextColor.DARK_PURPLE));
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                AbstractAttack atk = mode.getAttackRegistry().get(1, attackType, id);
                String info = atk != null
                        ? String.format(" (dmg=%.0f, r=%.1f, dur=%d, %s)",
                        atk.getConfig().getDamage(), atk.getConfig().getDamageRadius(),
                        atk.getConfig().getDurationTicks(),
                        atk.getConfig().isEnabled() ? "ON" : "OFF")
                        : "";
                sender.sendMessage(Component.text((i + 1) + ". " + id + info, NamedTextColor.LIGHT_PURPLE));
            }
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
            return true;
        }

        mode.getCorruptionConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Corruption configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleCorruptionStats(CommandSender sender) {
        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("=== Corruption Engine Stats ===", NamedTextColor.DARK_PURPLE));
        sender.sendMessage(Component.text("Floating blocks enabled: " + mode.getCorruptionConfig().isFloatingBlocksEnabled(), NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("  Max per chunk: " + mode.getCorruptionConfig().getFloatingBlocksMaxPerChunk(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Spread rate: " + mode.getCorruptionConfig().getFloatingBlocksSpreadRateTicks() + " ticks", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Damage: " + mode.getCorruptionConfig().getFloatingBlocksDamage() + " HP", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Block replacement enabled: " + mode.getCorruptionConfig().isBlockReplacementEnabled(), NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("  Blocks/tick: " + mode.getCorruptionConfig().getBlockReplacementBlocksPerTick(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Max radius: " + mode.getCorruptionConfig().getBlockReplacementMaxRadiusChunks() + " chunks", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Mob glitch enabled: " + mode.getCorruptionConfig().isMobGlitchEnabled(), NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("  Hostile only: " + mode.getCorruptionConfig().isMobGlitchHostileOnly(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Intensity: " + mode.getCorruptionConfig().getMobGlitchIntensity(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Respect claims: " + mode.getCorruptionConfig().isRespectClaims(), NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("Restoration blocks/tick: " + mode.getCorruptionConfig().getRestorationBlocksPerTick(), NamedTextColor.LIGHT_PURPLE));
        var engine = mode.getCorruptionEngine();
        if (engine != null && engine.isRunning()) {
            sender.sendMessage(Component.text("--- Live Engine Stats ---", NamedTextColor.DARK_PURPLE));
            sender.sendMessage(Component.text("Active floating blocks: " + engine.getActiveFloatingCount(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Corrupted blocks stored: " + engine.getRestorer().getStoredCount(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text(engine.getStats(), NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("[Engine not active — showing config defaults only]", NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean handleRestore(CommandSender sender) {
        CorruptionMode mode = getCorruptionMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Corruption mode not registered.", NamedTextColor.RED));
            return true;
        }

        var engine = mode.getCorruptionEngine();
        if (engine != null && engine.isRunning()) {
            engine.forceRestore();
            sender.sendMessage(Component.text("Block restoration triggered — restoring all corrupted blocks.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Corruption engine not active. Nothing to restore.", NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // Hide all completions if player lacks permission
        if (!sender.hasPermission("chaoscraft.corruption.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartsWith(args[0],
                    "status", "debug", "test", "clearattacks", "spawninterval",
                    "toggleexempt", "list", "reload", "corruption", "restore");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            CorruptionMode mode = getCorruptionMode();

            if ("test".equals(sub) && mode != null) {
                List<String> ids = new java.util.ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE));
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

    private CorruptionMode getCorruptionMode() {
        var mode = plugin.getModeManager().getMode("corruption");
        return mode instanceof CorruptionMode cm ? cm : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Corruption Mode Commands ===", NamedTextColor.DARK_PURPLE));
        sender.sendMessage(Component.text("/corruption status — Show mode status + engine stats", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption debug — Toggle debug mode", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption test <id> — Spawn a corruption attack on yourself", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption list — List all attack IDs with config", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption clearattacks — Clear all active attacks", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption spawninterval <ticks> — Set spawn interval", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption toggleexempt [player] — Toggle exempt", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption reload — Reload corruption configs", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption corruption — Show corruption engine stats", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("/corruption restore — Force block restoration", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Start/stop: /triggermode corruption | /endmode", NamedTextColor.GRAY));
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
