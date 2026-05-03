package com.blockforge.chaoscraft.modes.fluffy;

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
 * Fluffy mode admin commands. Dispatched via {@code /cc modes fluffy <sub>}.
 *
 * Subcommands:
 *  - status                  — runtime info
 *  - debug                   — toggle plugin-wide debug
 *  - test [id]               — list attack ids OR force-spawn one on yourself
 *  - list                    — list every attack with damage/radius/duration
 *  - clearattacks            — remove all currently active attacks
 *  - spawninterval <ticks>   — change attack spawn interval at runtime
 *  - rainspawn               — fire a single rain-from-sky drop now
 *  - rainstatus              — number of falling mobs + rain config
 *  - flora <small|medium|large> — fire a flora bloom event manually
 *  - toggleexempt [player]   — toggle attack exemption for a player
 *  - reload                  — reload fluffy config + attack configs
 */
public class FluffyCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public FluffyCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.fluffy.admin") && !sender.hasPermission("chaoscraft.admin")) {
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
            case "list" -> handleList(sender);
            case "clearattacks" -> handleClearAttacks(sender);
            case "spawninterval" -> handleSpawnInterval(sender, args);
            case "rainspawn" -> handleRainSpawn(sender);
            case "rainstatus" -> handleRainStatus(sender);
            case "flora" -> handleFlora(sender, args);
            case "toggleexempt" -> handleToggleExempt(sender, args);
            case "reload" -> handleReload(sender);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleStatus(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Fluffy mode not registered.", NamedTextColor.RED));
            return true;
        }
        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive() && modeManager.getActiveMode() instanceof FluffyMode;

        sender.sendMessage(Component.text("=== Fluffy Status ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"),
                active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(),
                NamedTextColor.LIGHT_PURPLE));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(),
                    NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));
            var world = mode.getFluffyWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.LIGHT_PURPLE));
            }
        }

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int envCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int meCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
        sender.sendMessage(Component.text("Block Display: " + bdCount + " | Environmental: " + envCount
                + " | ModelEngine: " + meCount, NamedTextColor.LIGHT_PURPLE));

        FluffyConfig cfg = mode.getFluffyConfig();
        sender.sendMessage(Component.text("Duration: " + cfg.getDurationSeconds() + "s | Spawn interval: "
                + cfg.getBaseSpawnInterval() + " ticks", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Rain: " + cfg.getRainSpawnsPerWindow() + " per "
                + cfg.getRainWindowSeconds() + "s | Anim speed: " + cfg.getMobAiAnimationSpeed() + "x",
                NamedTextColor.GRAY));
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
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Fluffy mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
                List<String> ids = mode.getAttackRegistry().getIds(1, type);
                if (!ids.isEmpty()) {
                    sender.sendMessage(Component.text(type.name() + " (" + ids.size() + "):",
                            NamedTextColor.LIGHT_PURPLE));
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
                + " | Radius: " + attack.getConfig().getDamageRadius()
                + " | Duration: " + attack.getConfig().getDurationTicks() + "t", NamedTextColor.GRAY));
        return true;
    }

    private boolean handleClearAttacks(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        int count = mode.getAttackScheduler().getActiveAttackCount();
        mode.getAttackScheduler().clearActiveAttacks();
        sender.sendMessage(Component.text("Cleared " + count + " active attacks.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: spawninterval <ticks>", NamedTextColor.YELLOW));
            return true;
        }
        try {
            int ticks = Math.max(1, Integer.parseInt(args[1]));
            mode.getAttackScheduler().setBaseSpawnInterval(ticks);
            sender.sendMessage(Component.text("Spawn interval: " + ticks + " ticks.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleRainSpawn(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null || mode.getRainSpawner() == null) {
            sender.sendMessage(Component.text("Rain spawner unavailable.", NamedTextColor.RED));
            return true;
        }
        mode.getRainSpawner().forceDrop();
        sender.sendMessage(Component.text("Forced rain drop fired.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleRainStatus(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null || mode.getRainSpawner() == null) {
            sender.sendMessage(Component.text("Rain spawner unavailable.", NamedTextColor.RED));
            return true;
        }
        FluffyConfig cfg = mode.getFluffyConfig();
        sender.sendMessage(Component.text("=== Rain From Sky ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("Falling mobs: " + mode.getRainSpawner().getFallingMobCount()
                + " / " + cfg.getRainMaxFallingMobs(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Rate: " + cfg.getRainSpawnsPerWindow() + " per "
                + cfg.getRainWindowSeconds() + "s", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Drop height: " + cfg.getRainDropHeight()
                + " | Target: " + cfg.getRainTargetMode()
                + " | Scatter: " + cfg.getRainScatterRadius(), NamedTextColor.GRAY));
        return true;
    }

    private boolean handleFlora(CommandSender sender, String[] args) {
        FluffyMode mode = getMode();
        if (mode == null || mode.getWorldEffects() == null) {
            sender.sendMessage(Component.text("World effects unavailable.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: flora <small|medium|large>", NamedTextColor.YELLOW));
            return true;
        }
        String tier = args[1].toLowerCase();
        if (!tier.equals("small") && !tier.equals("medium") && !tier.equals("large")) {
            sender.sendMessage(Component.text("Tier must be small, medium, or large.", NamedTextColor.RED));
            return true;
        }
        boolean fired = mode.getWorldEffects().forceBloom(tier);
        if (fired) {
            sender.sendMessage(Component.text("Triggered " + tier + " flora bloom.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Could not fire bloom (mode not active or no players).",
                    NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleToggleExempt(CommandSender sender, String[] args) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text(target.getName() + " no longer exempt.", NamedTextColor.YELLOW));
        } else {
            mode.addExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is now exempt.", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("=== Fluffy Attacks ===", NamedTextColor.LIGHT_PURPLE));
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
            List<String> ids = mode.getAttackRegistry().getIds(1, type);
            if (ids.isEmpty()) continue;
            sender.sendMessage(Component.text("--- " + type.name() + " (" + ids.size() + ") ---",
                    NamedTextColor.LIGHT_PURPLE));
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
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        mode.getFluffyConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Fluffy configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.fluffy.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filterStartsWith(args[0], "status", "debug", "test", "list", "clearattacks",
                    "spawninterval", "rainspawn", "rainstatus", "flora", "toggleexempt", "reload");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            FluffyMode mode = getMode();
            if ("test".equals(sub) && mode != null) {
                List<String> ids = new ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE));
                return filterStartsWith(args[1], ids);
            }
            if ("flora".equals(sub)) return filterStartsWith(args[1], "small", "medium", "large");
            if ("toggleexempt".equals(sub)) return null;
            if ("spawninterval".equals(sub)) return List.of("20", "40", "60", "100");
        }
        return Collections.emptyList();
    }

    // ========================
    // Helpers
    // ========================

    private FluffyMode getMode() {
        var mode = plugin.getModeManager().getMode("fluffy");
        return mode instanceof FluffyMode fm ? fm : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Fluffy Commands ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("status — Mode status", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("debug — Toggle debug mode", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("test [id] — List attacks or spawn one on yourself",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("list — List all attack IDs with stats", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("clearattacks — Clear active attacks", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("spawninterval <ticks> — Set spawn interval",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("rainspawn — Force one rain-from-sky drop",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("rainstatus — Show rain config + falling mob count",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("flora <small|medium|large> — Trigger a flora bloom",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("toggleexempt [player] — Toggle exempt", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("reload — Reload configs", NamedTextColor.LIGHT_PURPLE));
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
