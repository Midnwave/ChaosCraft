package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.timer.ModeTimer;
import com.blockforge.chaoscraft.modes.calamity.CalamityMode;
import com.blockforge.chaoscraft.modes.calamity.boss.DoGManager;
import com.blockforge.chaoscraft.updater.UpdateChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Main /chaoscraft (alias: /cc) command.
 *
 * Subcommands:
 *   help         — Full help menu (permission-filtered)
 *   modes        — Unified mode management (/cc modes <mode> <cmd>)
 *   timer        — Mode timer management (set/add/remove/pause/resume)
 *   devs         — Developer tools (setgem, setscitem, gui)
 *   dog          — DoG datapack entity (spawn/kill/status)
 *   debug        — Debug info (calamitas/dog/attacks)
 *   reload       — Reload all configs
 *   update       — Check for and download updates from GitHub
 *   exempt       — Attack exempt list (add/remove/list)
 *   entertitlescreen / exittitlescreen — Title screen control
 *   item         — Give special items
 *   itemtag      — Item tag management
 *   settings     — Admin settings management
 *   codes        — Admin code management
 *   useragreement — User agreement admin
 *   play         — Execute play scripts
 */
public class ChaosCraftCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    private static final List<String> ROOT_SUBS = List.of(
            "help", "modes", "timer", "devs", "reload", "exempt", "dog", "debug", "update",
            "function", "entertitlescreen", "exittitlescreen", "item", "itemtag",
            "settings", "codes", "useragreement", "play"
    );

    // Permission required for each subcommand (for tab-complete filtering and help display)
    private static final Map<String, String> SUB_PERMISSIONS = Map.ofEntries(
            Map.entry("help", "chaoscraft.use"),
            Map.entry("modes", "chaoscraft.use"),
            Map.entry("timer", "chaoscraft.admin"),
            Map.entry("devs", "chaoscraft.devs"),
            Map.entry("reload", "chaoscraft.admin"),
            Map.entry("exempt", "chaoscraft.admin"),
            Map.entry("dog", "chaoscraft.devs"),
            Map.entry("debug", "chaoscraft.devs"),
            Map.entry("update", "chaoscraft.admin"),
            Map.entry("entertitlescreen", "chaoscraft.titlescreen"),
            Map.entry("exittitlescreen", "chaoscraft.titlescreen"),
            Map.entry("item", "chaoscraft.item.give"),
            Map.entry("itemtag", "chaoscraft.itemtag.use"),
            Map.entry("settings", "chaoscraft.settings.admin"),
            Map.entry("codes", "chaoscraft.codes.create"),
            Map.entry("useragreement", "chaoscraft.useragreement.admin"),
            Map.entry("play", "chaoscraft.play.admin"),
            Map.entry("function", "chaoscraft.admin")
    );

    private static final List<String> TIMER_SUBS = List.of("set", "add", "remove", "pause", "resume");
    private static final List<String> DEVS_SUBS = List.of("setgem", "setscitem", "gui");
    private static final List<String> EXEMPT_SUBS = List.of("add", "remove", "list");
    private static final List<String> DOG_SUBS = List.of("spawn", "kill", "status");
    private static final List<String> DEBUG_SUBS = List.of("calamitas", "dog", "attacks");
    private static final List<String> UPDATE_SUBS = List.of("check", "download");
    private static final List<String> MODE_ACTIONS = List.of("start", "stop");
    private static final List<String> FUNCTION_SUBS = List.of("startmodetimer", "stopmodetimer");

    // Delegates for ported subcommands
    private final EnterTitleScreenCommand enterTitleScreenCmd;
    private final ExitTitleScreenCommand exitTitleScreenCmd;
    private final ItemCommand itemCmd;
    private final ItemTagCommand itemTagCmd;
    private final AdminSettingsCommand adminSettingsCmd;
    private final AdminCodesCommand adminCodesCmd;
    private final UserAgreementCommand userAgreementCmd;
    private final PlayCommand playCmd;

    // Mode-specific command handlers (used for /cc modes <mode> delegation)
    private final CalamityCommand calamityHandler;
    private final ChainCommand chainHandler;
    private final com.blockforge.chaoscraft.modes.corruption.CorruptionCommand corruptionHandler;
    private final com.blockforge.chaoscraft.modes.devilsdream.DevilsDreamCommand devilsDreamHandler;
    private final com.blockforge.chaoscraft.modes.bluemoon.BlueMoonCommand blueMoonHandler;

    // Dev GUI
    private final DevGUI devGUI;

    public ChaosCraftCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.enterTitleScreenCmd = new EnterTitleScreenCommand(plugin);
        this.exitTitleScreenCmd = new ExitTitleScreenCommand(plugin);
        this.itemCmd = new ItemCommand(plugin);
        this.itemTagCmd = new ItemTagCommand();
        this.adminSettingsCmd = new AdminSettingsCommand(plugin);
        this.adminCodesCmd = new AdminCodesCommand(plugin);
        this.userAgreementCmd = new UserAgreementCommand(plugin);
        this.playCmd = new PlayCommand(plugin);
        this.calamityHandler = new CalamityCommand(plugin);
        this.chainHandler = new ChainCommand(plugin);
        this.corruptionHandler = new com.blockforge.chaoscraft.modes.corruption.CorruptionCommand(plugin);
        this.devilsDreamHandler = new com.blockforge.chaoscraft.modes.devilsdream.DevilsDreamCommand(plugin);
        this.blueMoonHandler = new com.blockforge.chaoscraft.modes.bluemoon.BlueMoonCommand(plugin);
        this.devGUI = new DevGUI(plugin);

        // Register the DevGUI listener
        plugin.getServer().getPluginManager().registerEvents(devGUI, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendBrief(sender);
            return true;
        }

        // Strip the subcommand name and pass remaining args to delegates
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (args[0].toLowerCase()) {
            case "help" -> handleHelp(sender);
            case "modes" -> handleModes(sender, command, label, args);
            case "timer" -> handleTimer(sender, args);
            case "devs" -> handleDevs(sender, args);
            case "dog" -> handleDog(sender, args);
            case "debug" -> handleDebug(sender, args);
            case "update" -> handleUpdate(sender, args);
            case "reload" -> handleReload(sender);
            case "exempt" -> handleExempt(sender, args);
            case "function" -> handleFunction(sender, args);
            // Ported subcommands -- delegate to their own command classes
            case "entertitlescreen" -> enterTitleScreenCmd.onCommand(sender, command, label, subArgs);
            case "exittitlescreen" -> exitTitleScreenCmd.onCommand(sender, command, label, subArgs);
            case "item" -> itemCmd.onCommand(sender, command, label, subArgs);
            case "itemtag" -> itemTagCmd.onCommand(sender, command, label, subArgs);
            case "settings" -> adminSettingsCmd.onCommand(sender, command, label, subArgs);
            case "codes" -> adminCodesCmd.onCommand(sender, command, label, subArgs);
            case "useragreement" -> userAgreementCmd.onCommand(sender, command, label, subArgs);
            case "play" -> playCmd.onCommand(sender, command, label, subArgs);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + args[0] + ". Use /cc help for a full list.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    // ========================
    // Brief info (no args)
    // ========================

    private void sendBrief(CommandSender sender) {
        sender.sendMessage(Component.text("ChaosCraft v" + plugin.getDescription().getVersion(), NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Use /cc help for a full list of commands.", NamedTextColor.GRAY));
    }

    // ========================
    // Help (permission-filtered)
    // ========================

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("=== ChaosCraft Help ===", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Commands you have access to:", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());

        // Core
        helpSection(sender, "Core", NamedTextColor.GOLD);
        helpLine(sender, "/cc help", "chaoscraft.use", "Show this help menu");
        helpLine(sender, "/cc reload", "chaoscraft.admin", "Reload all configs");
        helpLine(sender, "/cc timer <set|add|remove|pause|resume> [val]", "chaoscraft.admin", "Mode timer management");
        helpLine(sender, "/cc exempt <add|remove|list> [player]", "chaoscraft.admin", "Attack-exempt player management");
        helpLine(sender, "/cc update <check|download>", "chaoscraft.admin", "Plugin updates from GitHub");

        // Modes
        helpSection(sender, "Modes", NamedTextColor.LIGHT_PURPLE);
        helpLine(sender, "/cc modes <mode> start", "chaoscraft.mode.trigger", "Start a game mode");
        helpLine(sender, "/cc modes <mode> stop", "chaoscraft.mode.end", "Stop the active game mode");

        Set<String> modeNames = plugin.getModeManager().getModeNames();
        for (String mode : modeNames) {
            String perm = "chaoscraft." + mode + ".admin";
            if (sender.hasPermission(perm)) {
                sender.sendMessage(Component.text("  /cc modes " + mode + " <cmd>", NamedTextColor.AQUA)
                        .append(Component.text(" — " + capitalize(mode) + " mode admin tools", NamedTextColor.GRAY)));
            }
        }

        // Dev
        helpSection(sender, "Developer", NamedTextColor.RED);
        helpLine(sender, "/cc devs gui", "chaoscraft.devs", "Open the developer GUI (commands, permissions, placeholders)");
        helpLine(sender, "/cc devs setgem", "chaoscraft.devs", "Set held item as the Calamity gem item");
        helpLine(sender, "/cc devs setscitem", "chaoscraft.devs", "Set held item as the Supreme Calamitas item");
        helpLine(sender, "/cc dog <spawn|kill|status>", "chaoscraft.devs", "Devourer of Gods entity management");
        helpLine(sender, "/cc debug <calamitas|dog|attacks>", "chaoscraft.devs", "Debug info for subsystems");

        // Services
        helpSection(sender, "Services", NamedTextColor.GREEN);
        helpLine(sender, "/cc entertitlescreen <player> [reason]", "chaoscraft.titlescreen", "Force player into title screen");
        helpLine(sender, "/cc exittitlescreen <player> [reason]", "chaoscraft.titlescreen", "Force player out of title screen");
        helpLine(sender, "/settings", "chaoscraft.settings.use", "Open the player settings menu");
        helpLine(sender, "/codes", null, "Enter a promotional code");
        helpLine(sender, "/itemtag <add|remove|list> [tag]", "chaoscraft.itemtag.use", "View/manage item tags");
        helpLine(sender, "/ccperf <action>", "chaoscraft.performance.admin", "MythicMobs performance limiter");

        // Legacy shortcuts
        helpSection(sender, "Shortcuts", NamedTextColor.DARK_GRAY);
        helpLine(sender, "/calamity <cmd>", "chaoscraft.calamity.admin", "Shortcut for /cc modes calamity <cmd>");
        helpLine(sender, "/chain <cmd>", "chaoscraft.chain.admin", "Shortcut for /cc modes chain <cmd>");
        helpLine(sender, "/triggermode <mode>", "chaoscraft.mode.trigger", "Legacy mode start command");
        helpLine(sender, "/endmode", "chaoscraft.mode.end", "Legacy mode stop command");

        sender.sendMessage(Component.empty());
        return true;
    }

    private void helpSection(CommandSender sender, String title, NamedTextColor color) {
        sender.sendMessage(Component.text("--- " + title + " ---", color, TextDecoration.BOLD));
    }

    private void helpLine(CommandSender sender, String command, String permission, String description) {
        // Only show commands the player has permission for (null permission = always show)
        if (permission != null && !sender.hasPermission(permission)) return;
        sender.sendMessage(Component.text("  " + command, NamedTextColor.AQUA)
                .append(Component.text(" — " + description, NamedTextColor.GRAY)));
    }

    // ========================
    // Modes subcommand
    // ========================

    private boolean handleModes(CommandSender sender, Command command, String label, String[] args) {
        // args[0] = "modes"
        Set<String> modeNames = plugin.getModeManager().getModeNames();

        if (args.length < 2) {
            sender.sendMessage(Component.text("=== Available Modes ===", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
            for (String mode : modeNames) {
                boolean active = plugin.getModeManager().isAnyModeActive()
                        && mode.equals(plugin.getModeManager().getActiveModeName());
                sender.sendMessage(Component.text("  " + mode, NamedTextColor.AQUA)
                        .append(Component.text(active ? " (ACTIVE)" : "", NamedTextColor.GREEN)));
            }
            sender.sendMessage(Component.text("Usage: /cc modes <mode> <start|stop|...>", NamedTextColor.GRAY));
            return true;
        }

        String modeName = args[1].toLowerCase();
        if (!modeNames.contains(modeName)) {
            sender.sendMessage(Component.text("Unknown mode: " + args[1] + ". Available: " + String.join(", ", modeNames), NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /cc modes " + modeName + " <start|stop|status|...>", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Use /cc modes " + modeName + " help for mode-specific commands.", NamedTextColor.GRAY));
            return true;
        }

        String action = args[2].toLowerCase();

        // Handle start/stop universally for all modes
        if ("start".equals(action)) {
            if (!sender.hasPermission("chaoscraft.mode.trigger")) {
                sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return true;
            }
            if (plugin.getModeManager().isAnyModeActive()) {
                sender.sendMessage(Component.text("A mode is already active: " + plugin.getModeManager().getActiveModeName()
                        + ". Use /cc modes " + modeName + " stop first.", NamedTextColor.RED));
                return true;
            }
            boolean started = plugin.getModeManager().startMode(modeName);
            if (started) {
                sender.sendMessage(Component.text("Started mode: " + modeName, NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Failed to start mode: " + modeName, NamedTextColor.RED));
            }
            return true;
        }

        if ("stop".equals(action)) {
            if (!sender.hasPermission("chaoscraft.mode.end")) {
                sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return true;
            }
            if (!plugin.getModeManager().isAnyModeActive()) {
                sender.sendMessage(Component.text("No mode is currently active.", NamedTextColor.RED));
                return true;
            }
            if (!modeName.equals(plugin.getModeManager().getActiveModeName())) {
                sender.sendMessage(Component.text(capitalize(modeName) + " is not the active mode. Active: "
                        + plugin.getModeManager().getActiveModeName(), NamedTextColor.RED));
                return true;
            }
            plugin.getModeManager().endActiveMode();
            sender.sendMessage(Component.text("Stopped mode: " + modeName, NamedTextColor.GREEN));
            return true;
        }

        // Delegate remaining subcommands to the mode-specific handler
        // Shift args: ["modes", "chain", "test", "id"] -> ["test", "id"]
        String[] modeArgs = Arrays.copyOfRange(args, 2, args.length);

        return switch (modeName) {
            case "calamity" -> {
                if (!sender.hasPermission("chaoscraft.calamity.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    yield true;
                }
                yield calamityHandler.onCommand(sender, command, label, modeArgs);
            }
            case "chain" -> {
                if (!sender.hasPermission("chaoscraft.chain.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    yield true;
                }
                yield chainHandler.onCommand(sender, command, label, modeArgs);
            }
            case "corruption" -> {
                if (!sender.hasPermission("chaoscraft.corruption.admin") && !sender.hasPermission("chaoscraft.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    yield true;
                }
                yield corruptionHandler.onCommand(sender, command, label, modeArgs);
            }
            case "devilsdream" -> {
                if (!sender.hasPermission("chaoscraft.devilsdream.admin") && !sender.hasPermission("chaoscraft.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    yield true;
                }
                yield devilsDreamHandler.onCommand(sender, command, label, modeArgs);
            }
            case "bluemoon" -> {
                if (!sender.hasPermission("chaoscraft.bluemoon.admin") && !sender.hasPermission("chaoscraft.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    yield true;
                }
                yield blueMoonHandler.onCommand(sender, command, label, modeArgs);
            }
            default -> {
                sender.sendMessage(Component.text("Mode '" + modeName + "' does not have admin commands yet.", NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    // ========================
    // Timer subcommand
    // ========================

    private boolean handleTimer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc timer <set|add|remove|pause|resume> [value]", NamedTextColor.RED));
            return true;
        }

        ModeTimer timer = plugin.getModeTimer();
        switch (args[1].toLowerCase()) {
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /cc timer set <time>  (e.g. 5:00, 1:30:00, 300)", NamedTextColor.RED));
                    return true;
                }
                long seconds = ModeTimer.parseTime(args[2]);
                timer.start(seconds);
                sender.sendMessage(Component.text("Timer set to " + timer.formatMmSs(), NamedTextColor.GREEN));
            }
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /cc timer add <seconds>", NamedTextColor.RED));
                    return true;
                }
                try {
                    long sec = Long.parseLong(args[2]);
                    timer.addTime(sec);
                    sender.sendMessage(Component.text("Added " + sec + "s. Timer: " + timer.formatMmSs(), NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number: " + args[2], NamedTextColor.RED));
                }
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /cc timer remove <seconds>", NamedTextColor.RED));
                    return true;
                }
                try {
                    long sec = Long.parseLong(args[2]);
                    timer.removeTime(sec);
                    sender.sendMessage(Component.text("Removed " + sec + "s. Timer: " + timer.formatMmSs(), NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number: " + args[2], NamedTextColor.RED));
                }
            }
            case "pause" -> {
                timer.pause();
                sender.sendMessage(Component.text("Timer paused.", NamedTextColor.YELLOW));
            }
            case "resume" -> {
                timer.resume();
                sender.sendMessage(Component.text("Timer resumed.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown timer action: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Devs subcommand (now includes GUI)
    // ========================

    private boolean handleDevs(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.devs")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc devs <setgem|setscitem|gui>", NamedTextColor.RED));
            return true;
        }

        if (args[1].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
                return true;
            }
            devGUI.openCommandsPage(player);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType().isAir()) {
            sender.sendMessage(Component.text("Hold an item in your main hand.", NamedTextColor.RED));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "setgem" -> {
                var mode = plugin.getModeManager().getMode("calamity");
                if (!(mode instanceof CalamityMode calamityMode)) {
                    sender.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
                    return true;
                }
                calamityMode.getCalamityConfig().setGemItemBytes(heldItem.serializeAsBytes());
                calamityMode.getCalamityConfig().save();
                sender.sendMessage(Component.text("Gem item set to: " + heldItem.getType().name(), NamedTextColor.GREEN));
            }
            case "setscitem" -> {
                var mode = plugin.getModeManager().getMode("calamity");
                if (!(mode instanceof CalamityMode calamityMode)) {
                    sender.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
                    return true;
                }
                calamityMode.getCalamityConfig().setCalamitasHeldItemBytes(heldItem.serializeAsBytes());
                calamityMode.getCalamityConfig().save();
                sender.sendMessage(Component.text("Supreme Calamitas item set to: " + heldItem.getType().name(), NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown devs command: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // DoG subcommand
    // ========================

    private boolean handleDog(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.devs")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc dog <spawn|kill|status>", NamedTextColor.RED));
            return true;
        }

        var mode = plugin.getModeManager().getMode("calamity");
        if (!(mode instanceof CalamityMode calamityMode)) {
            sender.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return true;
        }
        DoGManager dog = calamityMode.getDoGManager();

        switch (args[1].toLowerCase()) {
            case "spawn" -> {
                if (dog.isAlive()) {
                    sender.sendMessage(Component.text("DoG is already alive!", NamedTextColor.RED));
                    return true;
                }
                var endWorld = plugin.getServer().getWorld("world_the_end");
                if (endWorld == null) endWorld = plugin.getServer().getWorld("the_end");
                if (endWorld == null) {
                    sender.sendMessage(Component.text("End world not found.", NamedTextColor.RED));
                    return true;
                }
                dog.spawn(endWorld);
                sender.sendMessage(Component.text("Devourer of Gods spawned!", NamedTextColor.DARK_PURPLE));
            }
            case "kill" -> {
                if (!dog.isAlive()) {
                    sender.sendMessage(Component.text("DoG is not alive.", NamedTextColor.RED));
                    return true;
                }
                dog.kill();
                sender.sendMessage(Component.text("Devourer of Gods killed.", NamedTextColor.GREEN));
            }
            case "status" -> {
                sender.sendMessage(Component.text("--- DoG Status ---", NamedTextColor.DARK_PURPLE));
                sender.sendMessage(Component.text("Alive: " + dog.isAlive(), NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Phase 2: " + dog.isPhase2(), NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Health: " + String.format("%.0f / %.0f (%.1f%%)",
                        dog.getCurrentHealth(), dog.getMaxHealth(), dog.getHealthPercent()), NamedTextColor.GRAY));
            }
            default -> sender.sendMessage(Component.text("Unknown dog action: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Debug subcommand
    // ========================

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.devs")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc debug <calamitas|dog|attacks>", NamedTextColor.RED));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "dog" -> {
                var mode = plugin.getModeManager().getMode("calamity");
                if (!(mode instanceof CalamityMode calamityMode)) {
                    sender.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
                    return true;
                }
                DoGManager dog = calamityMode.getDoGManager();
                sender.sendMessage(Component.text("--- DoG Debug ---", NamedTextColor.DARK_PURPLE));
                sender.sendMessage(Component.text("Alive: " + dog.isAlive(), NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Phase 2 (Universal Collapse): " + dog.isPhase2(), NamedTextColor.GRAY));
                sender.sendMessage(Component.text("HP: " + String.format("%.0f / %.0f", dog.getCurrentHealth(), dog.getMaxHealth()), NamedTextColor.GRAY));
                sender.sendMessage(Component.text("HP%: " + String.format("%.2f%%", dog.getHealthPercent()), NamedTextColor.GRAY));
            }
            case "calamitas" -> {
                var cMode = plugin.getModeManager().getMode("calamity");
                if (!(cMode instanceof CalamityMode cm)) {
                    sender.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
                    return true;
                }
                var sc = cm.getCalamitasManager();
                if (!sc.isAlive()) {
                    sender.sendMessage(Component.text("Supreme Calamitas is not spawned.", NamedTextColor.RED));
                    return true;
                }
                for (String line : sc.getDebugInfo().split("\n")) {
                    sender.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacySection().deserialize(line));
                }
            }
            case "attacks" -> {
                var manager = plugin.getModeManager();
                if (!manager.isAnyModeActive()) {
                    sender.sendMessage(Component.text("No active mode.", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Active attack debug — not yet implemented.", NamedTextColor.YELLOW));
            }
            default -> sender.sendMessage(Component.text("Unknown debug target: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Update subcommand
    // ========================

    private boolean handleUpdate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc update <check|download>", NamedTextColor.RED));
            return true;
        }

        UpdateChecker updater = plugin.getUpdateChecker();
        if (updater == null) {
            sender.sendMessage(Component.text("Updater is disabled.", NamedTextColor.RED));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "check" -> updater.checkForUpdate(sender);
            case "download" -> updater.downloadUpdate(sender);
            default -> sender.sendMessage(Component.text("Usage: /cc update <check|download>", NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Reload
    // ========================

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        plugin.reload();
        sender.sendMessage(Component.text("ChaosCraft configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }

    // ========================
    // Function (startmodetimer, stopmodetimer)
    // ========================

    private boolean handleFunction(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc function <startmodetimer|stopmodetimer>", NamedTextColor.RED));
            return true;
        }

        var timerHud = plugin.getModeTimerHud();
        if (timerHud == null) {
            sender.sendMessage(Component.text("Timer HUD not initialized.", NamedTextColor.RED));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "startmodetimer" -> {
                timerHud.startHud();
                sender.sendMessage(Component.text("Mode timer HUD started.", NamedTextColor.GREEN));
            }
            case "stopmodetimer" -> {
                timerHud.stopHud();
                sender.sendMessage(Component.text("Mode timer HUD stopped.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown function: " + args[1]
                    + ". Available: startmodetimer, stopmodetimer", NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Exempt
    // ========================

    private boolean handleExempt(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc exempt <add|remove|list> [player]", NamedTextColor.RED));
            return true;
        }

        var manager = plugin.getModeManager();
        var activeMode = manager.getActiveMode();

        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /cc exempt add <player>", NamedTextColor.RED));
                    return true;
                }
                var target = plugin.getServer().getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                    return true;
                }
                if (activeMode != null) {
                    activeMode.addExempt(target);
                }
                sender.sendMessage(Component.text("Added " + target.getName() + " to exempt list.", NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /cc exempt remove <player>", NamedTextColor.RED));
                    return true;
                }
                var target = plugin.getServer().getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                    return true;
                }
                if (activeMode != null) {
                    activeMode.removeExempt(target);
                }
                sender.sendMessage(Component.text("Removed " + target.getName() + " from exempt list.", NamedTextColor.GREEN));
            }
            case "list" -> {
                if (activeMode == null) {
                    sender.sendMessage(Component.text("No active mode.", NamedTextColor.RED));
                    return true;
                }
                var exemptIds = activeMode.getExemptPlayers();
                if (exemptIds.isEmpty()) {
                    sender.sendMessage(Component.text("No exempt players.", NamedTextColor.GRAY));
                } else {
                    sender.sendMessage(Component.text("Exempt players:", NamedTextColor.GOLD));
                    for (var uuid : exemptIds) {
                        var p = plugin.getServer().getPlayer(uuid);
                        String name = p != null ? p.getName() : uuid.toString();
                        sender.sendMessage(Component.text(" - " + name, NamedTextColor.GRAY));
                    }
                }
            }
            default -> sender.sendMessage(Component.text("Unknown exempt action: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    // ========================
    // Tab completion (permission-filtered)
    // ========================

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // Filter root subcommands by permission
            List<String> filtered = new ArrayList<>();
            for (String sub : ROOT_SUBS) {
                String perm = SUB_PERMISSIONS.get(sub);
                if (perm == null || sender.hasPermission(perm)) {
                    filtered.add(sub);
                }
            }
            return filterStartsWith(filtered, args[0]);
        }

        String sub = args[0].toLowerCase();

        // Check permission before showing any deeper completions
        String requiredPerm = SUB_PERMISSIONS.get(sub);
        if (requiredPerm != null && !sender.hasPermission(requiredPerm)) {
            return List.of();
        }

        // Modes subcommand tab completion
        if ("modes".equals(sub)) {
            return tabCompleteModes(sender, args);
        }

        // For ported subcommands, delegate tab completion with shifted args
        if (List.of("entertitlescreen", "exittitlescreen", "item", "itemtag",
                "settings", "codes", "useragreement", "play").contains(sub)) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            TabCompleter delegate = switch (sub) {
                case "entertitlescreen" -> enterTitleScreenCmd;
                case "exittitlescreen" -> exitTitleScreenCmd;
                case "item" -> itemCmd;
                case "itemtag" -> itemTagCmd;
                case "settings" -> adminSettingsCmd;
                case "codes" -> adminCodesCmd;
                case "useragreement" -> userAgreementCmd;
                case "play" -> playCmd;
                default -> null;
            };
            if (delegate != null) {
                return delegate.onTabComplete(sender, command, alias, subArgs);
            }
        }

        if (args.length == 2) {
            return switch (sub) {
                case "timer" -> filterStartsWith(TIMER_SUBS, args[1]);
                case "devs" -> filterStartsWith(DEVS_SUBS, args[1]);
                case "dog" -> filterStartsWith(DOG_SUBS, args[1]);
                case "debug" -> filterStartsWith(DEBUG_SUBS, args[1]);
                case "update" -> filterStartsWith(UPDATE_SUBS, args[1]);
                case "exempt" -> filterStartsWith(EXEMPT_SUBS, args[1]);
                case "function" -> filterStartsWith(FUNCTION_SUBS, args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("exempt") &&
                    (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
                return null; // Default to online player names
            }
        }
        return List.of();
    }

    /**
     * Tab complete for /cc modes <mode> <action> [args...]
     */
    private List<String> tabCompleteModes(CommandSender sender, String[] args) {
        Set<String> modeNames = plugin.getModeManager().getModeNames();

        // /cc modes <mode>
        if (args.length == 2) {
            return filterStartsWith(new ArrayList<>(modeNames), args[1]);
        }

        String modeName = args[1].toLowerCase();
        if (!modeNames.contains(modeName)) return List.of();

        // /cc modes <mode> <action>
        if (args.length == 3) {
            List<String> actions = new ArrayList<>(MODE_ACTIONS); // start, stop

            // Add mode-specific actions based on which mode
            switch (modeName) {
                case "calamity" -> {
                    if (sender.hasPermission("chaoscraft.calamity.admin")) {
                        actions.addAll(List.of("testblockdisplay", "testenvironmentalattack", "testbossattack",
                                "setphase", "spawnboss", "killboss", "addgems", "setgems",
                                "status", "debug", "clearattacks", "spawninterval", "toggleexempt"));
                    }
                }
                case "chain" -> {
                    if (sender.hasPermission("chaoscraft.chain.admin")) {
                        actions.addAll(List.of("status", "debug", "test", "clearattacks",
                                "spawninterval", "toggleexempt", "list", "reload"));
                    }
                }
                case "corruption" -> {
                    if (sender.hasPermission("chaoscraft.corruption.admin") || sender.hasPermission("chaoscraft.admin")) {
                        actions.addAll(List.of("status", "debug", "test", "clearattacks",
                                "spawninterval", "toggleexempt", "list", "reload", "corruption", "restore"));
                    }
                }
                case "devilsdream" -> {
                    if (sender.hasPermission("chaoscraft.devilsdream.admin") || sender.hasPermission("chaoscraft.admin")) {
                        actions.addAll(List.of("status", "debug", "test", "clearattacks",
                                "spawninterval", "toggleexempt", "list", "reload", "adaptation", "resetadaptation"));
                    }
                }
                case "bluemoon" -> {
                    if (sender.hasPermission("chaoscraft.bluemoon.admin") || sender.hasPermission("chaoscraft.admin")) {
                        actions.addAll(List.of("status", "debug", "test", "clearattacks",
                                "spawninterval", "toggleexempt", "list", "reload", "boss", "gimmick"));
                    }
                }
            }

            // Filter start/stop by permission
            if (!sender.hasPermission("chaoscraft.mode.trigger")) actions.remove("start");
            if (!sender.hasPermission("chaoscraft.mode.end")) actions.remove("stop");

            return filterStartsWith(actions, args[2]);
        }

        // Deeper tab completion — delegate to mode-specific handlers
        // Shift args: ["modes", "chain", "test", "..."] -> ["test", "..."]
        String[] modeArgs = Arrays.copyOfRange(args, 2, args.length);

        return switch (modeName) {
            case "calamity" -> {
                if (!sender.hasPermission("chaoscraft.calamity.admin")) yield List.of();
                List<String> result = calamityHandler.onTabComplete(sender, null, "", modeArgs);
                yield result != null ? result : List.of();
            }
            case "chain" -> {
                if (!sender.hasPermission("chaoscraft.chain.admin")) yield List.of();
                List<String> result = chainHandler.onTabComplete(sender, null, "", modeArgs);
                yield result != null ? result : List.of();
            }
            case "corruption" -> {
                if (!sender.hasPermission("chaoscraft.corruption.admin") && !sender.hasPermission("chaoscraft.admin")) yield List.of();
                List<String> result = corruptionHandler.onTabComplete(sender, null, "", modeArgs);
                yield result != null ? result : List.of();
            }
            case "devilsdream" -> {
                if (!sender.hasPermission("chaoscraft.devilsdream.admin") && !sender.hasPermission("chaoscraft.admin")) yield List.of();
                List<String> result = devilsDreamHandler.onTabComplete(sender, null, "", modeArgs);
                yield result != null ? result : List.of();
            }
            case "bluemoon" -> {
                if (!sender.hasPermission("chaoscraft.bluemoon.admin") && !sender.hasPermission("chaoscraft.admin")) yield List.of();
                List<String> result = blueMoonHandler.onTabComplete(sender, null, "", modeArgs);
                yield result != null ? result : List.of();
            }
            default -> List.of();
        };
    }

    // ========================
    // Helpers
    // ========================

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) result.add(opt);
        }
        return result;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
