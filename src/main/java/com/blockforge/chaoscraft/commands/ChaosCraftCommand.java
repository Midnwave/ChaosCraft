package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.timer.ModeTimer;
import com.blockforge.chaoscraft.modes.calamity.CalamityMode;
import com.blockforge.chaoscraft.modes.calamity.boss.DoGManager;
import com.blockforge.chaoscraft.updater.UpdateChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Main /chaoscraft (alias: /cc) command.
 *
 * Subcommands:
 *   timer        — Mode timer management (set/add/remove/pause/resume)
 *   devs         — Developer tools (setgem, setscitem)
 *   dog          — DoG datapack entity (spawn/kill/status)
 *   debug        — Debug info (calamitas/dog/attacks)
 *   reload       — Reload all configs
 *   exempt       — Attack exempt list (add/remove/list)
 *   entertitlescreen / exittitlescreen — Title screen control
 *   item         — Give special items
 *   itemtag      — Item tag management
 *   settings     — Admin settings management
 *   codes        — Admin code management
 *   useragreement — User agreement admin
 *   play         — Execute play scripts
 *   update       — Check for and download updates from GitHub
 */
public class ChaosCraftCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    private static final List<String> ROOT_SUBS = List.of(
            "timer", "devs", "reload", "exempt", "dog", "debug", "update",
            "entertitlescreen", "exittitlescreen", "item", "itemtag",
            "settings", "codes", "useragreement", "play"
    );
    private static final List<String> TIMER_SUBS = List.of("set", "add", "remove", "pause", "resume");
    private static final List<String> DEVS_SUBS = List.of("setgem", "setscitem");
    private static final List<String> EXEMPT_SUBS = List.of("add", "remove", "list");
    private static final List<String> DOG_SUBS = List.of("spawn", "kill", "status");
    private static final List<String> DEBUG_SUBS = List.of("calamitas", "dog", "attacks");
    private static final List<String> UPDATE_SUBS = List.of("check", "download");

    // Delegates for ported subcommands
    private final EnterTitleScreenCommand enterTitleScreenCmd;
    private final ExitTitleScreenCommand exitTitleScreenCmd;
    private final ItemCommand itemCmd;
    private final ItemTagCommand itemTagCmd;
    private final AdminSettingsCommand adminSettingsCmd;
    private final AdminCodesCommand adminCodesCmd;
    private final UserAgreementCommand userAgreementCmd;
    private final PlayCommand playCmd;

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
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("ChaosCraft v" + plugin.getDescription().getVersion(), NamedTextColor.DARK_PURPLE));
            sender.sendMessage(Component.text("Subcommands:", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  timer, devs, dog, debug, reload, exempt", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  entertitlescreen, exittitlescreen, item, itemtag", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  settings, codes, useragreement, play", NamedTextColor.GRAY));
            return true;
        }

        // Strip the subcommand name and pass remaining args to delegates
        String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);

        return switch (args[0].toLowerCase()) {
            case "timer" -> handleTimer(sender, args);
            case "devs" -> handleDevs(sender, args);
            case "dog" -> handleDog(sender, args);
            case "debug" -> handleDebug(sender, args);
            case "update" -> handleUpdate(sender, args);
            case "reload" -> handleReload(sender);
            case "exempt" -> handleExempt(sender, args);
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
                sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
                yield true;
            }
        };
    }

    // ---- Timer subcommand ----

    private boolean handleTimer(CommandSender sender, String[] args) {
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

    // ---- Devs subcommand ----

    private boolean handleDevs(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.devs")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc devs <setgem|setscitem>", NamedTextColor.RED));
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

    // ---- DoG subcommand ----

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

    // ---- Debug subcommand ----

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
                // Send debug info (uses legacy § color codes)
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

    // ---- Reload ----

    // ---- Update subcommand ----

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

    // ---- Reload ----

    private boolean handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(Component.text("ChaosCraft configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }

    // ---- Exempt ----

    private boolean handleExempt(CommandSender sender, String[] args) {
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

    // ---- Tab completion ----

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStartsWith(ROOT_SUBS, args[0]);
        }
        // For ported subcommands, delegate tab completion with shifted args
        String sub = args[0].toLowerCase();
        if (List.of("entertitlescreen", "exittitlescreen", "item", "itemtag",
                "settings", "codes", "useragreement", "play").contains(sub)) {
            String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
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

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.startsWith(lower)) result.add(opt);
        }
        return result;
    }
}
