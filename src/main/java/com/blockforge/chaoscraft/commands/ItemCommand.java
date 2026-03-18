package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
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

import java.util.HashMap;
import java.util.List;

/**
 * /cc item give &lt;itemName&gt; -- Give special items to the player.
 */
public class ItemCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public ItemCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("ChaosCraft Item Commands:", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /cc item give <itemName> - Give yourself an item", NamedTextColor.GRAY));
            return true;
        }

        if ("give".equalsIgnoreCase(args[0])) {
            return handleGive(sender, args);
        }

        sender.sendMessage(Component.text("Unknown item subcommand: " + args[0], NamedTextColor.RED));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only for item give.", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("chaoscraft.item.give")) {
            sender.sendMessage(Component.text("You lack permission: chaoscraft.item.give", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc item give <itemName>", NamedTextColor.YELLOW));
            return true;
        }

        String itemName = args[1].toLowerCase();
        if ("radiant_core".equals(itemName) || "radiant-core".equals(itemName)) {
            ItemStack rc = plugin.createRadiantCoreItem();
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(rc);
            if (!leftover.isEmpty()) {
                sender.sendMessage(Component.text("Inventory full; dropped item at your feet.", NamedTextColor.YELLOW));
                player.getWorld().dropItemNaturally(player.getLocation(), rc);
            } else {
                sender.sendMessage(Component.text("Gave you the Radiant Core.", NamedTextColor.GREEN));
            }
        } else {
            sender.sendMessage(Component.text("Unknown item: " + itemName, NamedTextColor.RED));
            sender.sendMessage(Component.text("Available items: radiant_core", NamedTextColor.GRAY));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            return List.of("radiant_core").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
