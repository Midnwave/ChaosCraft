package com.blockforge.chaoscraft.modes.seer;

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
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Seer mode admin commands.
 * Permission: chaoscraft.seer.admin
 * Colors: DARK_PURPLE headers, LIGHT_PURPLE items.
 */
public class SeerCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public SeerCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.seer.admin") && !sender.hasPermission("chaoscraft.admin")) {
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
            case "orbs" -> handleOrbs(sender, args);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleStatus(CommandSender sender) {
        SeerMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Seer mode not registered.", NamedTextColor.RED));
            return true;
        }

        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive()
                && modeManager.getActiveMode() instanceof SeerMode;

        sender.sendMessage(Component.text("=== Seer Status ===", NamedTextColor.DARK_PURPLE));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"), active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(), NamedTextColor.LIGHT_PURPLE));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));

            var world = mode.getSeerWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.LIGHT_PURPLE));
            }

            // Boss info
            var boss = mode.getBossManager();
            sender.sendMessage(Component.text("--- Boss ---", NamedTextColor.DARK_PURPLE));
            sender.sendMessage(Component.text("Alive: " + boss.isBossAlive(),
                    boss.isBossAlive() ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (boss.isBossAlive()) {
                sender.sendMessage(Component.text("Beam: " + (boss.isBeamActive() ? "FIRING" : "OFF"),
                        boss.isBeamActive() ? NamedTextColor.RED : NamedTextColor.GRAY));
                long maxHp = (long) boss.getOrbsRemaining() * mode.getSeerConfig().getOrbHealthPerOrb();
                sender.sendMessage(Component.text("Max HP: " + String.format("%,d", maxHp), NamedTextColor.YELLOW));
                Entity bossEntity = boss.getBossEntity();
                if (bossEntity != null) {
                    Location bLoc = bossEntity.getLocation();
                    sender.sendMessage(Component.text("Position: " + String.format("%.0f, %.0f, %.0f",
                            bLoc.getX(), bLoc.getY(), bLoc.getZ()), NamedTextColor.GRAY));
                    if (bossEntity instanceof org.bukkit.entity.LivingEntity living) {
                        sender.sendMessage(Component.text("Entity HP: " + String.format("%.0f/%.0f",
                                living.getHealth(), living.getMaxHealth()), NamedTextColor.YELLOW));
                    }
                }
            }

            // Orb info
            var orbs = mode.getOrbManager();
            int orbCount = mode.getSeerConfig().getOrbCount();
            int destroyed = orbs.getDestroyedCount();
            sender.sendMessage(Component.text("--- Orbs ---", NamedTextColor.DARK_PURPLE));
            sender.sendMessage(Component.text("Remaining: " + (orbCount - destroyed) + "/" + orbCount,
                    destroyed > 0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
            for (int i = 1; i <= orbCount; i++) {
                boolean isDest = orbs.isOrbDestroyed(i);
                var pos = mode.getSeerConfig().getOrbPosition(i);
                String posStr = pos != null
                        ? String.format("(%.0f, %.0f, %.0f)", pos.getX(), pos.getY(), pos.getZ())
                        : "(not set)";
                sender.sendMessage(Component.text("  Orb " + i + ": " + (isDest ? "DESTROYED" : "INTACT") + " " + posStr,
                        isDest ? NamedTextColor.RED : NamedTextColor.GREEN));
            }
        }

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int envCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int bossCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BOSS).size();
        sender.sendMessage(Component.text("Block Display: " + bdCount + " | Environmental: " + envCount + " | Boss: " + bossCount, NamedTextColor.LIGHT_PURPLE));

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

        SeerMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Seer mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.BOSS)) {
                List<String> ids = mode.getAttackRegistry().getIds(1, type);
                if (!ids.isEmpty()) {
                    sender.sendMessage(Component.text(type.name() + " (" + ids.size() + "):", NamedTextColor.LIGHT_PURPLE));
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
        SeerMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        int count = mode.getAttackScheduler().getActiveAttackCount();
        mode.getAttackScheduler().clearActiveAttacks();
        sender.sendMessage(Component.text("Cleared " + count + " active attacks.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        SeerMode mode = getMode();
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
        SeerMode mode = getMode();
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
        SeerMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }

        sender.sendMessage(Component.text("=== Seer Attacks ===", NamedTextColor.DARK_PURPLE));
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.BOSS)) {
            List<String> ids = mode.getAttackRegistry().getIds(1, type);
            if (ids.isEmpty()) continue;
            sender.sendMessage(Component.text("--- " + type.name() + " (" + ids.size() + ") ---", NamedTextColor.LIGHT_PURPLE));
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
        SeerMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        mode.getSeerConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Seer configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleBoss(CommandSender sender, String[] args) {
        SeerMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        var boss = mode.getBossManager();

        if (args.length < 2) {
            sender.sendMessage(Component.text("Boss commands: spawn, kill, beam", NamedTextColor.LIGHT_PURPLE));
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "spawn" -> {
                var world = mode.getSeerWorld();
                if (world == null) { sender.sendMessage(Component.text("World not found.", NamedTextColor.RED)); return true; }
                boss.forceSpawn(world);
                sender.sendMessage(Component.text("Boss force-spawned.", NamedTextColor.GREEN));
            }
            case "kill" -> {
                boss.forceKill();
                sender.sendMessage(Component.text("Boss force-killed.", NamedTextColor.GREEN));
            }
            case "beam" -> {
                boss.forceBeam();
                sender.sendMessage(Component.text("Seer beam triggered!", NamedTextColor.LIGHT_PURPLE));
            }
            default -> sender.sendMessage(Component.text("Unknown boss command. Use: spawn, kill, beam", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleOrbs(CommandSender sender, String[] args) {
        SeerMode mode = getMode();
        if (mode == null) { sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED)); return true; }
        var orbs = mode.getOrbManager();

        if (args.length < 2) {
            sender.sendMessage(Component.text("Orb commands: status, reset, break <1-10>", NamedTextColor.LIGHT_PURPLE));
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "status" -> {
                sender.sendMessage(Component.text("=== Orb Status ===", NamedTextColor.DARK_PURPLE));
                int orbCount = mode.getSeerConfig().getOrbCount();
                for (int i = 1; i <= orbCount; i++) {
                    boolean destroyed = orbs.isOrbDestroyed(i);
                    var pos = mode.getSeerConfig().getOrbPosition(i);
                    String posStr = pos != null
                            ? String.format("(%.0f, %.0f, %.0f)", pos.getX(), pos.getY(), pos.getZ())
                            : "(not set)";
                    sender.sendMessage(Component.text("  Orb " + i + ": " + (destroyed ? "DESTROYED" : "INTACT") + " " + posStr,
                            destroyed ? NamedTextColor.RED : NamedTextColor.GREEN));
                }
                sender.sendMessage(Component.text("Destroyed: " + orbs.getDestroyedCount() + "/" + orbCount, NamedTextColor.LIGHT_PURPLE));
            }
            case "reset" -> {
                orbs.resetAllOrbs();
                sender.sendMessage(Component.text("All orbs restored.", NamedTextColor.GREEN));
            }
            case "break" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: orbs break <1-10>", NamedTextColor.YELLOW)); return true; }
                try {
                    int index = Integer.parseInt(args[2]);
                    int maxOrbs = mode.getSeerConfig().getOrbCount();
                    if (index < 1 || index > maxOrbs) {
                        sender.sendMessage(Component.text("Orb index must be 1-" + maxOrbs + ".", NamedTextColor.RED));
                        return true;
                    }
                    orbs.forceBreak(index);
                    sender.sendMessage(Component.text("Orb " + index + " force-broken.", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(Component.text("Unknown orb command. Use: status, reset, break <1-10>", NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.seer.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartsWith(args[0], "status", "debug", "test", "clearattacks",
                    "spawninterval", "toggleexempt", "list", "reload", "boss", "orbs");
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            SeerMode mode = getMode();

            if ("test".equals(sub) && mode != null) {
                List<String> ids = new ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BOSS));
                return filterStartsWith(args[1], ids);
            }
            if ("toggleexempt".equals(sub)) return null;
            if ("spawninterval".equals(sub)) return List.of("20", "40", "60", "100");
            if ("boss".equals(sub)) return filterStartsWith(args[1], "spawn", "kill", "beam");
            if ("orbs".equals(sub)) return filterStartsWith(args[1], "status", "reset", "break");
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            SeerMode mode = getMode();
            if ("orbs".equals(sub) && "break".equals(args[1].toLowerCase())) {
                int maxOrbs = mode != null ? mode.getSeerConfig().getOrbCount() : 10;
                List<String> nums = new ArrayList<>();
                for (int i = 1; i <= maxOrbs; i++) nums.add(String.valueOf(i));
                return filterStartsWith(args[2], nums);
            }
        }

        return Collections.emptyList();
    }

    // Helpers
    private SeerMode getMode() {
        var mode = plugin.getModeManager().getMode("seer");
        return mode instanceof SeerMode sm ? sm : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Seer Commands ===", NamedTextColor.DARK_PURPLE));
        sender.sendMessage(Component.text("status — Mode status", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("debug — Toggle debug mode", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("test <id> — Spawn attack on yourself", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("list — List all attack IDs", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("clearattacks — Clear active attacks", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("spawninterval <ticks> — Set spawn interval", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("toggleexempt [player] — Toggle exempt", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("reload — Reload configs", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("boss spawn/kill/beam — Boss controls", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("orbs status/reset/break <n> — Orb controls", NamedTextColor.LIGHT_PURPLE));
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
