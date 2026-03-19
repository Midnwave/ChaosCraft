package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackScheduler;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * /calamity command for Calamity mode-specific operations.
 *
 * Test commands:
 *   testblockdisplay, testenvironmentalattack, testbossattack
 *
 * Dev commands:
 *   setphase, spawnboss, killboss, addgems, setgems, status,
 *   debug, clearattacks, spawninterval, toggleexempt
 */
public class CalamityCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;
    private static final List<String> ROOT_SUBS = List.of(
            "testblockdisplay", "testenvironmentalattack", "testbossattack",
            "setphase", "spawnboss", "killboss",
            "addgems", "setgems",
            "status", "debug", "clearattacks", "spawninterval", "toggleexempt"
    );
    private static final List<String> PHASES = List.of("1", "2", "3", "4", "5");
    private static final List<String> BOSSES = List.of(
            "voidmaw", "dog", "dweller", "dragon", "calamitas"
    );

    public CalamityCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("chaoscraft.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            // Test commands
            case "testblockdisplay" -> handleTestBlockDisplay(player, args);
            case "testenvironmentalattack", "testenv" -> handleTestEnvironmental(player, args);
            case "testbossattack", "testboss" -> handleTestBossAttack(player, args);
            // Dev commands
            case "setphase" -> handleSetPhase(player, args);
            case "spawnboss" -> handleSpawnBoss(player, args);
            case "killboss" -> handleKillBoss(player, args);
            case "addgems" -> handleAddGems(player, args);
            case "setgems" -> handleSetGems(player, args);
            case "status" -> handleStatus(player);
            case "debug" -> handleDebug(player);
            case "clearattacks" -> handleClearAttacks(player);
            case "spawninterval" -> handleSpawnInterval(player, args);
            case "toggleexempt" -> handleToggleExempt(player, args);
            default -> player.sendMessage(Component.text("Unknown subcommand: " + args[0] + ". Use /calamity for help.", NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Help
    // ========================

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("--- Calamity Dev Commands ---", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        player.sendMessage(Component.text("/calamity status", NamedTextColor.AQUA).append(Component.text(" — Show mode state, phase, gems, attacks", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity debug", NamedTextColor.AQUA).append(Component.text(" — Toggle debug mode (radius outlines)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity setphase <1-5>", NamedTextColor.AQUA).append(Component.text(" — Force change phase", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity spawnboss <name>", NamedTextColor.AQUA).append(Component.text(" — Manually spawn a boss", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity killboss", NamedTextColor.AQUA).append(Component.text(" — Kill current boss / trigger defeat", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity addgems <amount>", NamedTextColor.AQUA).append(Component.text(" — Add deposited gems", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity setgems <amount>", NamedTextColor.AQUA).append(Component.text(" — Set deposited gems", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity clearattacks", NamedTextColor.AQUA).append(Component.text(" — Remove all active attacks", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity spawninterval <ticks>", NamedTextColor.AQUA).append(Component.text(" — Set spawn interval", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/calamity toggleexempt [player]", NamedTextColor.AQUA).append(Component.text(" — Toggle exempt status", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("--- Test Commands ---", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        player.sendMessage(Component.text("/calamity testblockdisplay <phase> <id>", NamedTextColor.AQUA));
        player.sendMessage(Component.text("/calamity testenv <phase> <id>", NamedTextColor.AQUA));
        player.sendMessage(Component.text("/calamity testboss <boss> <id>", NamedTextColor.AQUA));
        player.sendMessage(Component.empty());
    }

    // ========================
    // Dev commands
    // ========================

    private void handleSetPhase(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /calamity setphase <1-5>", NamedTextColor.RED));
            return;
        }
        CalamityMode mode = getCalamityMode();
        if (mode == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }
        int phase = parsePhase(args[1]);
        if (phase < 1 || phase > 5) {
            player.sendMessage(Component.text("Phase must be 1-5.", NamedTextColor.RED));
            return;
        }

        mode.setCurrentPhase(phase);
        mode.getAttackScheduler().start(phase);
        player.sendMessage(Component.text("Phase set to " + phase + ". Attack scheduler restarted.", NamedTextColor.GREEN));
        plugin.getLogger().info("[Calamity] Dev command: phase forced to " + phase + " by " + player.getName());
    }

    private void handleSpawnBoss(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /calamity spawnboss <" + String.join("|", BOSSES) + ">", NamedTextColor.RED));
            return;
        }
        String bossName = args[1].toLowerCase();
        if (!BOSSES.contains(bossName)) {
            player.sendMessage(Component.text("Unknown boss: " + bossName + ". Options: " + String.join(", ", BOSSES), NamedTextColor.RED));
            return;
        }
        CalamityMode mode = getCalamityMode();
        if (mode == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }

        int bossPhase = bossToPhase(bossName);
        mode.setCurrentPhase(bossPhase);
        mode.getAttackScheduler().start(bossPhase);
        mode.getAttackScheduler().setBossActive(true, bossName);

        player.sendMessage(Component.text("Spawning boss: " + bossName + " (phase " + bossPhase + ")", NamedTextColor.GREEN));
        plugin.getLogger().info("[Calamity] Dev command: spawning " + bossName + " by " + player.getName());

        // Dispatch the actual spawn via the mode's own spawning logic
        // This runs the MythicMobs/datapack spawn commands
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                "triggermode calamity");
    }

    private void handleKillBoss(Player player, String[] args) {
        CalamityMode mode = getCalamityMode();
        if (mode == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }
        if (!mode.isBossAlive()) {
            player.sendMessage(Component.text("No boss is currently alive.", NamedTextColor.YELLOW));
            return;
        }

        mode.onBossDefeated();
        player.sendMessage(Component.text("Boss defeat triggered. Advancing to next phase.", NamedTextColor.GREEN));
        plugin.getLogger().info("[Calamity] Dev command: boss killed by " + player.getName());
    }

    private void handleAddGems(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /calamity addgems <amount>", NamedTextColor.RED));
            return;
        }
        CalamityMode mode = getCalamityMode();
        if (mode == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }
        try {
            int amount = Integer.parseInt(args[1]);
            mode.addGems(amount);
            player.sendMessage(Component.text("Added " + amount + " gems. Total: " + mode.getGemCount(), NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }
    }

    private void handleSetGems(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /calamity setgems <amount>", NamedTextColor.RED));
            return;
        }
        CalamityMode mode = getCalamityMode();
        if (mode == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }
        try {
            int amount = Integer.parseInt(args[1]);
            mode.setGemCount(amount);
            player.sendMessage(Component.text("Gems set to " + amount, NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }
    }

    private void handleStatus(Player player) {
        CalamityMode mode = getCalamityMode();
        if (mode == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }

        boolean modeActive = plugin.getModeManager().isAnyModeActive()
                && "calamity".equals(plugin.getModeManager().getActiveModeName());

        AttackScheduler scheduler = mode.getAttackScheduler();
        AttackRegistry registry = mode.getAttackRegistry();

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("--- Calamity Status ---", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        player.sendMessage(Component.text("Mode Active: ", NamedTextColor.GRAY)
                .append(Component.text(modeActive ? "YES" : "NO", modeActive ? NamedTextColor.GREEN : NamedTextColor.RED)));
        player.sendMessage(Component.text("Phase: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(mode.getCurrentPhase()), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Boss Alive: ", NamedTextColor.GRAY)
                .append(Component.text(mode.isBossAlive() ? "YES" : "NO", mode.isBossAlive() ? NamedTextColor.RED : NamedTextColor.GREEN)));
        player.sendMessage(Component.text("Gems Deposited: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(mode.getGemCount()), NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Active Attacks: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(scheduler.getActiveAttackCount()), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Registered Attacks: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(registry.size()), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Your Events: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(scheduler.getPlayerEventCount(player)), NamedTextColor.YELLOW)));

        boolean debug = plugin.getConfig().getBoolean("debug", false);
        player.sendMessage(Component.text("Debug Mode: ", NamedTextColor.GRAY)
                .append(Component.text(debug ? "ON" : "OFF", debug ? NamedTextColor.GREEN : NamedTextColor.RED)));

        boolean exempt = false;
        if (modeActive) {
            exempt = plugin.getModeManager().getActiveMode().isExempt(player);
        }
        player.sendMessage(Component.text("You Are Exempt: ", NamedTextColor.GRAY)
                .append(Component.text(exempt ? "YES (no damage)" : "NO", exempt ? NamedTextColor.YELLOW : NamedTextColor.GREEN)));

        // Show attack counts per phase
        for (int phase = 1; phase <= 5; phase++) {
            int bd = registry.getByPhaseAndType(phase, AttackType.BLOCK_DISPLAY).size();
            int env = registry.getByPhaseAndType(phase, AttackType.ENVIRONMENTAL).size();
            int boss = registry.getByPhaseAndType(phase, AttackType.BOSS).size();
            player.sendMessage(Component.text("  Phase " + phase + ": ", NamedTextColor.GRAY)
                    .append(Component.text(bd + " BD", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(env + " ENV", NamedTextColor.GREEN))
                    .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(boss + " BOSS", NamedTextColor.RED)));
        }

        // Show active attack list
        if (!scheduler.getActiveAttacks().isEmpty()) {
            player.sendMessage(Component.text("Active: ", NamedTextColor.GRAY));
            for (AbstractAttack attack : scheduler.getActiveAttacks()) {
                player.sendMessage(Component.text("  - " + attack.getId(), NamedTextColor.YELLOW)
                        .append(Component.text(" [" + attack.getType().name() + " P" + attack.getPhase() + "]", NamedTextColor.DARK_GRAY))
                        .append(Component.text(" tick " + attack.getTicksAlive() + "/" + attack.getConfig().getDurationTicks(), NamedTextColor.GRAY))
                        .append(Component.text(" dmg=" + attack.getConfig().getDamage() + " r=" + attack.getConfig().getDamageRadius(), NamedTextColor.DARK_GRAY)));
            }
        }

        player.sendMessage(Component.empty());
    }

    private void handleDebug(Player player) {
        boolean current = plugin.getConfig().getBoolean("debug", false);
        plugin.getConfig().set("debug", !current);
        plugin.saveConfig();
        String state = !current ? "ON" : "OFF";
        player.sendMessage(Component.text("Debug mode: " + state, !current ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (!current) {
            player.sendMessage(Component.text("  RED circles = continuous damage radius", NamedTextColor.GRAY));
            player.sendMessage(Component.text("  YELLOW circles = impact damage radius", NamedTextColor.GRAY));
        }
        plugin.getLogger().info("[Calamity] Debug mode " + state + " by " + player.getName());
    }

    private void handleClearAttacks(Player player) {
        CalamityMode mode = getCalamityMode();
        if (mode == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }
        AttackScheduler scheduler = mode.getAttackScheduler();
        int count = scheduler.getActiveAttackCount();
        // Clean up all active attacks
        for (AbstractAttack attack : new ArrayList<>(scheduler.getActiveAttacks())) {
            attack.cleanup();
        }
        player.sendMessage(Component.text("Cleared " + count + " active attacks.", NamedTextColor.GREEN));
    }

    private void handleSpawnInterval(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /calamity spawninterval <ticks>  (default: 60 = 3 seconds)", NamedTextColor.RED));
            return;
        }
        CalamityMode mode = getCalamityMode();
        if (mode == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }
        try {
            int ticks = Integer.parseInt(args[1]);
            if (ticks < 1) ticks = 1;
            mode.getAttackScheduler().setBaseSpawnInterval(ticks);
            player.sendMessage(Component.text("Spawn interval set to " + ticks + " ticks (" + String.format("%.1f", ticks / 20.0) + "s)", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }
    }

    private void handleToggleExempt(Player player, String[] args) {
        if (!plugin.getModeManager().isAnyModeActive()) {
            player.sendMessage(Component.text("No mode is active.", NamedTextColor.RED));
            return;
        }
        var activeMode = plugin.getModeManager().getActiveMode();

        Player target = player;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return;
            }
        }

        if (activeMode.isExempt(target)) {
            activeMode.removeExempt(target);
            player.sendMessage(Component.text(target.getName() + " is no longer exempt (will take damage).", NamedTextColor.GREEN));
        } else {
            activeMode.addExempt(target);
            player.sendMessage(Component.text(target.getName() + " is now exempt (no damage).", NamedTextColor.YELLOW));
        }
    }

    // ========================
    // Test commands (force-spawn attacks)
    // ========================

    private void handleTestBlockDisplay(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /calamity testblockdisplay <phase> <id>", NamedTextColor.RED));
            return;
        }
        int phase = parsePhase(args[1]);
        if (phase < 1 || phase > 5) {
            player.sendMessage(Component.text("Phase must be 1-5.", NamedTextColor.RED));
            return;
        }
        String attackId = args[2].toLowerCase();

        AttackRegistry registry = getRegistry();
        if (registry == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }

        AbstractAttack template = registry.get(phase, AttackType.BLOCK_DISPLAY, attackId);
        if (template == null) {
            player.sendMessage(Component.text("Block display attack not found: phase " + phase + ", id: " + attackId, NamedTextColor.RED));
            listAvailable(player, registry, phase, AttackType.BLOCK_DISPLAY);
            return;
        }

        getScheduler().forceSpawn(template, player);
        player.sendMessage(Component.text("Spawned block display: " + attackId + " (phase " + phase + ")"
                + " [dmg=" + template.getConfig().getDamage() + " r=" + template.getConfig().getDamageRadius() + "]", NamedTextColor.LIGHT_PURPLE));
    }

    private void handleTestEnvironmental(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /calamity testenv <phase> <id>", NamedTextColor.RED));
            return;
        }
        int phase = parsePhase(args[1]);
        if (phase < 1 || phase > 5) {
            player.sendMessage(Component.text("Phase must be 1-5.", NamedTextColor.RED));
            return;
        }
        String attackId = args[2].toLowerCase();

        AttackRegistry registry = getRegistry();
        if (registry == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }

        AbstractAttack template = registry.get(phase, AttackType.ENVIRONMENTAL, attackId);
        if (template == null) {
            player.sendMessage(Component.text("Environmental attack not found: phase " + phase + ", id: " + attackId, NamedTextColor.RED));
            listAvailable(player, registry, phase, AttackType.ENVIRONMENTAL);
            return;
        }

        getScheduler().forceSpawn(template, player);
        player.sendMessage(Component.text("Spawned environmental: " + attackId + " (phase " + phase + ")"
                + " [dmg=" + template.getConfig().getDamage() + " r=" + template.getConfig().getDamageRadius() + "]", NamedTextColor.LIGHT_PURPLE));
    }

    private void handleTestBossAttack(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /calamity testboss <boss> <id>", NamedTextColor.RED));
            return;
        }
        String boss = args[1].toLowerCase();
        if (!BOSSES.contains(boss)) {
            player.sendMessage(Component.text("Unknown boss: " + boss + ". Options: " + String.join(", ", BOSSES), NamedTextColor.RED));
            return;
        }
        String attackId = args[2].toLowerCase();

        AttackRegistry registry = getRegistry();
        if (registry == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }

        int bossPhase = bossToPhase(boss);
        AbstractAttack template = registry.get(bossPhase, AttackType.BOSS, attackId);
        if (template == null) {
            player.sendMessage(Component.text("Boss attack not found: " + boss + ", id: " + attackId, NamedTextColor.RED));
            // Show available boss attacks for this boss
            List<String> available = registry.getBossAttackIds(boss);
            if (!available.isEmpty()) {
                player.sendMessage(Component.text("Available: " + String.join(", ", available.subList(0, Math.min(10, available.size())))
                        + (available.size() > 10 ? " (+" + (available.size() - 10) + " more)" : ""), NamedTextColor.GRAY));
            }
            return;
        }

        getScheduler().forceSpawn(template, player);
        player.sendMessage(Component.text("Spawned boss attack: " + attackId + " (" + boss + ")"
                + " [dmg=" + template.getConfig().getDamage() + " r=" + template.getConfig().getDamageRadius() + "]", NamedTextColor.LIGHT_PURPLE));
    }

    /**
     * Show available attack IDs when an invalid ID is entered.
     */
    private void listAvailable(Player player, AttackRegistry registry, int phase, AttackType type) {
        List<String> available = registry.getIds(phase, type);
        if (!available.isEmpty()) {
            player.sendMessage(Component.text("Available: " + String.join(", ", available.subList(0, Math.min(10, available.size())))
                    + (available.size() > 10 ? " (+" + (available.size() - 10) + " more)" : ""), NamedTextColor.GRAY));
        }
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStartsWith(ROOT_SUBS, args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "testbossattack", "testboss", "spawnboss" -> {
                    return filterStartsWith(BOSSES, args[1]);
                }
                case "testblockdisplay", "testenvironmentalattack", "testenv", "setphase" -> {
                    return filterStartsWith(PHASES, args[1]);
                }
                case "toggleexempt" -> {
                    // Return online player names
                    List<String> names = new ArrayList<>();
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        names.add(p.getName());
                    }
                    return filterStartsWith(names, args[1]);
                }
            }
        }
        if (args.length == 3) {
            AttackRegistry registry = getRegistry();
            if (registry == null) return List.of();

            switch (args[0].toLowerCase()) {
                case "testblockdisplay" -> {
                    int phase = parsePhase(args[1]);
                    if (phase >= 1 && phase <= 5) {
                        return filterStartsWith(registry.getIds(phase, AttackType.BLOCK_DISPLAY), args[2]);
                    }
                }
                case "testenvironmentalattack", "testenv" -> {
                    int phase = parsePhase(args[1]);
                    if (phase >= 1 && phase <= 5) {
                        return filterStartsWith(registry.getIds(phase, AttackType.ENVIRONMENTAL), args[2]);
                    }
                }
                case "testbossattack", "testboss" -> {
                    String boss = args[1].toLowerCase();
                    return filterStartsWith(registry.getBossAttackIds(boss), args[2]);
                }
            }
        }
        return List.of();
    }

    // ========================
    // Helpers
    // ========================

    private CalamityMode getCalamityMode() {
        var mode = plugin.getModeManager().getMode("calamity");
        if (mode instanceof CalamityMode calamityMode) {
            return calamityMode;
        }
        return null;
    }

    private AttackRegistry getRegistry() {
        CalamityMode mode = getCalamityMode();
        return mode != null ? mode.getAttackRegistry() : null;
    }

    private AttackScheduler getScheduler() {
        CalamityMode mode = getCalamityMode();
        return mode != null ? mode.getAttackScheduler() : null;
    }

    private int bossToPhase(String boss) {
        return switch (boss) {
            case "voidmaw" -> 1;
            case "dog" -> 2;
            case "dweller" -> 3;
            case "dragon" -> 4;
            case "calamitas" -> 5;
            default -> 1;
        };
    }

    private int parsePhase(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) result.add(opt);
        }
        return result;
    }
}
