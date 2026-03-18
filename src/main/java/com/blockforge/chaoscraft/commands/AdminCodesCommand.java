package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.codes.CodeData;
import com.blockforge.chaoscraft.services.codes.CodesConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
 * /cc codes &lt;create|delete|expire|setreward|setpermissionrequirement|unblacklist|enter|exit&gt;
 * Administrative code management subcommand.
 */
public class AdminCodesCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    private static final List<String> SUB_COMMANDS = List.of(
            "create", "delete", "expire", "setreward", "setpermissionrequirement",
            "unblacklist", "enter", "exit"
    );

    public AdminCodesCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.codes.create")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (plugin.getCodesService() == null) {
            sender.sendMessage(Component.text("Codes service is not enabled!", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "expire" -> handleExpire(sender, args);
            case "setreward" -> handleSetReward(sender, args);
            case "setpermissionrequirement" -> handleSetPermission(sender, args);
            case "unblacklist" -> handleUnblacklist(sender, args);
            case "enter" -> handleEnter(sender, args);
            case "exit" -> handleExit(sender, args);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /cc codes create \"<code name>\" <expires> <maxUses> [cooldown]", NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /cc codes create \"WELCOME2024\" 7d 1", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Example: /cc codes create \"DAILY\" never unlimited 1d", NamedTextColor.GRAY));
            return true;
        }

        String codeName = args[1];
        if (codeName.startsWith("\"") && codeName.endsWith("\"")) {
            codeName = codeName.substring(1, codeName.length() - 1);
        }

        long expiresAt;
        if (args[2].equalsIgnoreCase("never")) {
            expiresAt = -1L;
        } else {
            expiresAt = System.currentTimeMillis() + CodesConfig.parseTimeString(args[2]);
        }

        int maxUses;
        if (args[3].equalsIgnoreCase("unlimited")) {
            maxUses = -1;
        } else {
            try { maxUses = Integer.parseInt(args[3]); }
            catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid max uses: " + args[3], NamedTextColor.RED));
                return true;
            }
        }

        long cooldownMillis = 0L;
        if (args.length >= 5 && maxUses == -1) {
            cooldownMillis = CodesConfig.parseTimeString(args[4]);
        }

        plugin.getCodesService().createCode(codeName, expiresAt, maxUses, cooldownMillis);
        sender.sendMessage(Component.text("Created code: " + codeName, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Expires: " + (expiresAt == -1L ? "Never" : args[2]), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Max uses: " + (maxUses == -1 ? "Unlimited" : maxUses), NamedTextColor.GRAY));
        if (cooldownMillis > 0L) {
            sender.sendMessage(Component.text("Cooldown: " + args[4], NamedTextColor.GRAY));
        }

        if (sender instanceof Player player) {
            sender.sendMessage(Component.text("Opening reward setup GUI...", NamedTextColor.GRAY));
            plugin.getCodesService().openRewardGUI(player, codeName);
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc codes delete <code name>", NamedTextColor.RED));
            return true;
        }
        boolean success = plugin.getCodesService().deleteCode(args[1]);
        sender.sendMessage(success
                ? Component.text("Deleted code: " + args[1], NamedTextColor.GREEN)
                : Component.text("Code not found: " + args[1], NamedTextColor.RED));
        return true;
    }

    private boolean handleExpire(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc codes expire <code name>", NamedTextColor.RED));
            return true;
        }
        CodeData codeData = plugin.getCodesService().getCode(args[1]);
        if (codeData == null) {
            sender.sendMessage(Component.text("Code not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        codeData.setExpired();
        sender.sendMessage(Component.text("Expired code: " + args[1], NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetReward(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only for this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc codes setreward <code name>", NamedTextColor.RED));
            return true;
        }
        CodeData codeData = plugin.getCodesService().getCode(args[1]);
        if (codeData == null) {
            sender.sendMessage(Component.text("Code not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("Opening reward setup GUI...", NamedTextColor.GRAY));
        plugin.getCodesService().openRewardGUI(player, args[1]);
        return true;
    }

    private boolean handleSetPermission(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc codes setpermissionrequirement <code name>", NamedTextColor.RED));
            return true;
        }
        CodeData codeData = plugin.getCodesService().getCode(args[1]);
        if (codeData == null) {
            sender.sendMessage(Component.text("Code not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("Type the permission requirement in chat, or 'none' to remove:", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleUnblacklist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc codes unblacklist <player>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        if (!plugin.getCodesService().isBlacklisted(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is not currently blacklisted.", NamedTextColor.YELLOW));
            return true;
        }
        plugin.getCodesService().clearBlacklist(target.getUniqueId());
        target.sendMessage(Component.text("You have been removed from the codes blacklist!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Removed " + target.getName() + " from the codes blacklist.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleEnter(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc codes enter <player>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        if (plugin.getCodesService().isInSession(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is already in a code session.", NamedTextColor.YELLOW));
            return true;
        }
        plugin.getCodesService().startSession(target.getUniqueId());
        sender.sendMessage(Component.text("Forced " + target.getName() + " into code entry mode.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleExit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc codes exit <player>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        if (!plugin.getCodesService().isInSession(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is not in a code session.", NamedTextColor.YELLOW));
            return true;
        }
        plugin.getCodesService().endSession(target.getUniqueId());
        sender.sendMessage(Component.text("Removed " + target.getName() + " from code entry mode.", NamedTextColor.GREEN));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Code Management Commands ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("/cc codes create \"<name>\" <expires> <maxUses> [cooldown]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cc codes delete <code>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cc codes expire <code>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cc codes setreward <code>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cc codes setpermissionrequirement <code>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cc codes unblacklist <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cc codes enter <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/cc codes exit <player>", NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("delete", "expire", "setreward", "setpermissionrequirement").contains(sub)) {
                return new ArrayList<>(plugin.getCodesService().getCodeNames()).stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
            }
            if (List.of("exit", "unblacklist", "enter").contains(sub)) {
                return null; // online player names
            }
        }
        return List.of();
    }
}
