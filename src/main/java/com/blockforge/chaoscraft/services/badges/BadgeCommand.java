package com.blockforge.chaoscraft.services.badges;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles badge subcommands dispatched from the main /cc command.
 *
 * Subcommands:
 *   badges                                      — Opens the BadgeGUI for the sender
 *   createbadge <id> <display_name> <limited> <function> — Create a badge
 *   deletebadge <id>                            — Delete a badge
 *   assignbadge <id> <player>                   — Grant a badge to a player
 *   removebadge <id> <player>                   — Revoke a badge from a player
 *   setbadgelimited <id> <true/false>           — Toggle limited; if true opens CalendarGUI
 *   setbadgedescription <id> <description...>   — Set badge description
 */
public class BadgeCommand {

    private final ChaosCraftPlugin plugin;
    private final BadgeService badgeService;
    private final BadgeGUI badgeGUI;
    private final CalendarGUI calendarGUI;

    public BadgeCommand(ChaosCraftPlugin plugin, BadgeService badgeService,
                        BadgeGUI badgeGUI, CalendarGUI calendarGUI) {
        this.plugin = plugin;
        this.badgeService = badgeService;
        this.badgeGUI = badgeGUI;
        this.calendarGUI = calendarGUI;
    }

    /**
     * Handle a badge subcommand.
     *
     * @param sender the command sender
     * @param sub    the subcommand name (e.g. "badges", "createbadge", etc.)
     * @param args   the remaining arguments after the subcommand
     * @return true if the command was handled
     */
    public boolean handle(CommandSender sender, String sub, String[] args) {
        switch (sub.toLowerCase()) {
            case "badges" -> {
                return handleBadges(sender);
            }
            case "createbadge" -> {
                return handleCreateBadge(sender, args);
            }
            case "deletebadge" -> {
                return handleDeleteBadge(sender, args);
            }
            case "assignbadge" -> {
                return handleAssignBadge(sender, args);
            }
            case "removebadge" -> {
                return handleRemoveBadge(sender, args);
            }
            case "setbadgelimited" -> {
                return handleSetBadgeLimited(sender, args);
            }
            case "setbadgedescription" -> {
                return handleSetBadgeDescription(sender, args);
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Provide tab completions for badge subcommands.
     */
    public List<String> tabComplete(String sub, String[] args) {
        switch (sub.toLowerCase()) {
            case "createbadge" -> {
                // createbadge <id> <display_name...> <limited> <function>
                if (args.length == 0) return List.of("<id>");
                if (args.length >= 3) {
                    // Last two args are limited and function
                    String lastArg = args[args.length - 1].toLowerCase();
                    // Could be limited or function
                    List<String> suggestions = new ArrayList<>();
                    suggestions.addAll(List.of("true", "false"));
                    suggestions.addAll(List.of("dummy", "command:", "on_mode_survive:", "above_placeholderapi:", "greaterthanequal_placeholderapi:"));
                    return suggestions.stream().filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
            case "deletebadge" -> {
                if (args.length == 1) {
                    return filterBadgeIds(args[0]);
                }
                return Collections.emptyList();
            }
            case "assignbadge" -> {
                if (args.length == 1) return filterBadgeIds(args[0]);
                if (args.length == 2) return filterPlayerNames(args[1]);
                return Collections.emptyList();
            }
            case "removebadge" -> {
                if (args.length == 1) return filterBadgeIds(args[0]);
                if (args.length == 2) return filterPlayerNames(args[1]);
                return Collections.emptyList();
            }
            case "setbadgelimited" -> {
                if (args.length == 1) return filterBadgeIds(args[0]);
                if (args.length == 2) {
                    return List.of("true", "false").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
            case "setbadgedescription" -> {
                if (args.length == 1) return filterBadgeIds(args[0]);
                return Collections.emptyList();
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    /**
     * Get all badge subcommand names for root tab completion.
     */
    public static List<String> getSubcommands() {
        return List.of("badges", "createbadge", "deletebadge", "assignbadge",
                "removebadge", "setbadgelimited", "setbadgedescription");
    }

    // ========================
    // Subcommand Handlers
    // ========================

    private boolean handleBadges(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        badgeGUI.open(player);
        return true;
    }

    /**
     * /cc createbadge <id> <display_name_with_spaces> <limited> <function>
     * Display name is everything between id and limited (supports hex &#RRGGBB and spaces).
     * The last two args are always limited (true/false) and function.
     */
    private boolean handleCreateBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.badges.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        // Minimum: id, at least 1 word of display name, limited, function = 4 args
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /cc createbadge <id> <display_name...> <true/false> <function>", NamedTextColor.RED));
            return true;
        }

        String id = args[0].toLowerCase();
        String function = args[args.length - 1];
        String limitedStr = args[args.length - 2];

        boolean limited;
        if (limitedStr.equalsIgnoreCase("true")) {
            limited = true;
        } else if (limitedStr.equalsIgnoreCase("false")) {
            limited = false;
        } else {
            sender.sendMessage(Component.text("Limited must be 'true' or 'false'.", NamedTextColor.RED));
            return true;
        }

        // Display name is everything between args[1] and args[length-2]
        StringBuilder displayNameBuilder = new StringBuilder();
        for (int i = 1; i < args.length - 2; i++) {
            if (displayNameBuilder.length() > 0) displayNameBuilder.append(" ");
            displayNameBuilder.append(args[i]);
        }
        String displayName = displayNameBuilder.toString();

        if (displayName.isEmpty()) {
            sender.sendMessage(Component.text("Display name cannot be empty.", NamedTextColor.RED));
            return true;
        }

        BadgeDefinition badge = new BadgeDefinition(id, displayName, limited, 0L,
                "", function, Material.PAPER, 0);
        badgeService.createBadge(badge);

        sender.sendMessage(Component.text("Badge created: ", NamedTextColor.GREEN)
                .append(BadgeService.parseDisplayName(displayName))
                .append(Component.text(" (" + id + ")", NamedTextColor.GRAY)));
        return true;
    }

    private boolean handleDeleteBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.badges.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /cc deletebadge <id>", NamedTextColor.RED));
            return true;
        }

        String id = args[0].toLowerCase();
        if (badgeService.getBadgeDefinition(id) == null) {
            sender.sendMessage(Component.text("Badge not found: " + id, NamedTextColor.RED));
            return true;
        }

        badgeService.deleteBadge(id);
        sender.sendMessage(Component.text("Badge deleted: " + id, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAssignBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.badges.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc assignbadge <id> <player>", NamedTextColor.RED));
            return true;
        }

        String badgeId = args[0].toLowerCase();
        String playerName = args[1];

        BadgeDefinition badge = badgeService.getBadgeDefinition(badgeId);
        if (badge == null) {
            sender.sendMessage(Component.text("Badge not found: " + badgeId, NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        boolean granted = badgeService.grantBadge(target, badgeId);
        if (granted) {
            sender.sendMessage(Component.text("Assigned badge ", NamedTextColor.GREEN)
                    .append(BadgeService.parseDisplayName(badge.getDisplayName()))
                    .append(Component.text(" to " + target.getName(), NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text(target.getName() + " already has badge " + badgeId, NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleRemoveBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.badges.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc removebadge <id> <player>", NamedTextColor.RED));
            return true;
        }

        String badgeId = args[0].toLowerCase();
        String playerName = args[1];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        boolean revoked = badgeService.revokeBadge(target.getUniqueId(), badgeId);
        if (revoked) {
            sender.sendMessage(Component.text("Removed badge " + badgeId + " from " + target.getName(), NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text(target.getName() + " does not have badge " + badgeId, NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleSetBadgeLimited(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.badges.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc setbadgelimited <id> <true/false>", NamedTextColor.RED));
            return true;
        }

        String badgeId = args[0].toLowerCase();
        BadgeDefinition existing = badgeService.getBadgeDefinition(badgeId);
        if (existing == null) {
            sender.sendMessage(Component.text("Badge not found: " + badgeId, NamedTextColor.RED));
            return true;
        }

        boolean limited = args[1].equalsIgnoreCase("true");

        if (limited && sender instanceof Player player) {
            // Open CalendarGUI to pick expiry date
            sender.sendMessage(Component.text("Select the expiry date for badge " + badgeId + "...", NamedTextColor.YELLOW));
            calendarGUI.open(player, (epochMillis) -> {
                BadgeDefinition updated = new BadgeDefinition(
                        existing.getId(), existing.getDisplayName(), true, epochMillis,
                        existing.getDescription(), existing.getFunction(),
                        existing.getIconMaterial(), existing.getCustomModelData()
                );
                badgeService.createBadge(updated);
                player.sendMessage(Component.text("Badge " + badgeId + " set to limited with expiry date.", NamedTextColor.GREEN));
            });
        } else {
            // Set to not limited (clear expiry)
            BadgeDefinition updated = new BadgeDefinition(
                    existing.getId(), existing.getDisplayName(), limited, 0L,
                    existing.getDescription(), existing.getFunction(),
                    existing.getIconMaterial(), existing.getCustomModelData()
            );
            badgeService.createBadge(updated);
            sender.sendMessage(Component.text("Badge " + badgeId + " limited set to " + limited, NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleSetBadgeDescription(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.badges.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc setbadgedescription <id> <description...>", NamedTextColor.RED));
            return true;
        }

        String badgeId = args[0].toLowerCase();
        BadgeDefinition existing = badgeService.getBadgeDefinition(badgeId);
        if (existing == null) {
            sender.sendMessage(Component.text("Badge not found: " + badgeId, NamedTextColor.RED));
            return true;
        }

        StringBuilder descBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (descBuilder.length() > 0) descBuilder.append(" ");
            descBuilder.append(args[i]);
        }

        BadgeDefinition updated = new BadgeDefinition(
                existing.getId(), existing.getDisplayName(), existing.isLimited(), existing.getExpiryDate(),
                descBuilder.toString(), existing.getFunction(),
                existing.getIconMaterial(), existing.getCustomModelData()
        );
        badgeService.createBadge(updated);
        sender.sendMessage(Component.text("Badge " + badgeId + " description updated.", NamedTextColor.GREEN));
        return true;
    }

    // ========================
    // Helpers
    // ========================

    private List<String> filterBadgeIds(String prefix) {
        String lower = prefix.toLowerCase();
        return badgeService.getAllBadgeIds().stream()
                .filter(id -> id.startsWith(lower))
                .collect(Collectors.toList());
    }

    private List<String> filterPlayerNames(String prefix) {
        String lower = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
