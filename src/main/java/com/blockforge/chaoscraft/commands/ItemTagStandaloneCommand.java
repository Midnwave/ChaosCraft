package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.itemtags.ItemTagsAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Standalone /itemtag command -- add, remove, and list tags on the held item.
 */
public class ItemTagStandaloneCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public ItemTagStandaloneCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("chaoscraft.itemtag.use")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /itemtag add <tag> | remove <tag> | list", NamedTextColor.YELLOW));
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            sender.sendMessage(Component.text("Hold an item in your main hand.", NamedTextColor.RED));
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "add" -> {
                if (!sender.hasPermission("chaoscraft.itemtag.add")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    yield true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /itemtag add <tag>", NamedTextColor.RED));
                    yield true;
                }
                boolean added = ItemTagsAPI.addTag(hand, args[1]);
                sender.sendMessage(added
                        ? Component.text("Added tag '" + args[1] + "' to the item.", NamedTextColor.GREEN)
                        : Component.text("Item already has tag '" + args[1] + "'.", NamedTextColor.YELLOW));
                yield true;
            }
            case "remove" -> {
                if (!sender.hasPermission("chaoscraft.itemtag.remove")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    yield true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /itemtag remove <tag>", NamedTextColor.RED));
                    yield true;
                }
                boolean removed = ItemTagsAPI.removeTag(hand, args[1]);
                sender.sendMessage(removed
                        ? Component.text("Removed tag '" + args[1] + "' from the item.", NamedTextColor.GREEN)
                        : Component.text("Item did not have tag '" + args[1] + "'.", NamedTextColor.YELLOW));
                yield true;
            }
            case "list" -> {
                if (!sender.hasPermission("chaoscraft.itemtag.list")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    yield true;
                }
                Set<String> tags = ItemTagsAPI.getTags(hand);
                sender.sendMessage(tags.isEmpty()
                        ? Component.text("This item has no tags.", NamedTextColor.GRAY)
                        : Component.text("Tags: " + String.join(", ", tags), NamedTextColor.AQUA));
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("Usage: /itemtag add <tag> | remove <tag> | list", NamedTextColor.YELLOW));
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && sender instanceof Player player) {
            if ("remove".equalsIgnoreCase(args[0])) {
                Set<String> tags = ItemTagsAPI.getTags(player.getInventory().getItemInMainHand());
                String partial = args[1].toLowerCase();
                return tags.stream().filter(t -> t.toLowerCase().startsWith(partial)).toList();
            }
        }
        return Collections.emptyList();
    }
}
