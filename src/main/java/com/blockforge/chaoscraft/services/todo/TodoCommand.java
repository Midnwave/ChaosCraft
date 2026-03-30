package com.blockforge.chaoscraft.services.todo;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interactive chat-based to-do list command.
 *
 * Usage:
 *   /todo                     — Show your to-do list with clickable actions
 *   /todo add <text>          — Add a new item
 *   /todo done <id>           — Mark an item as completed
 *   /todo undone <id>         — Mark an item as not completed
 *   /todo edit <id> <text>    — Edit an item's text
 *   /todo remove <id>         — Delete an item
 *   /todo clear               — Remove all completed items
 *
 * All actions are clickable in chat — players can click to toggle done, edit, or remove.
 */
public class TodoCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;
    private final TodoStorage storage;

    public TodoCommand(ChaosCraftPlugin plugin, TodoStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use to-do lists.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showTodoList(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "add" -> handleAdd(player, args);
            case "done" -> handleDone(player, args);
            case "undone" -> handleUndone(player, args);
            case "edit" -> handleEdit(player, args);
            case "remove", "delete", "rm" -> handleRemove(player, args);
            case "clear" -> handleClear(player);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand. Use /todo for help.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    // ========================
    // Show list
    // ========================

    private void showTodoList(Player player) {
        List<TodoItem> items = storage.getTodos(player.getUniqueId());

        // Header
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  ").append(
                Component.text("\u2550\u2550\u2550 ", NamedTextColor.DARK_PURPLE))
                .append(Component.text("Your To-Do List", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" \u2550\u2550\u2550", NamedTextColor.DARK_PURPLE)));

        if (items.isEmpty()) {
            player.sendMessage(Component.text("  No items! Use ", NamedTextColor.GRAY)
                    .append(Component.text("/todo add <text>", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.suggestCommand("/todo add "))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to add a new item", NamedTextColor.GREEN))))
                    .append(Component.text(" to get started.", NamedTextColor.GRAY)));
        } else {
            long pending = items.stream().filter(i -> !i.isCompleted()).count();
            long done = items.stream().filter(TodoItem::isCompleted).count();

            player.sendMessage(Component.text("  " + pending + " pending", NamedTextColor.YELLOW)
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(done + " completed", NamedTextColor.GREEN)));
            player.sendMessage(Component.empty());

            for (TodoItem item : items) {
                player.sendMessage(buildItemLine(item));
            }
        }

        // Footer with actions
        player.sendMessage(Component.empty());
        TextComponent footer = Component.text("  ")
                .append(Component.text("[+ Add]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.suggestCommand("/todo add "))
                        .hoverEvent(HoverEvent.showText(Component.text("Add a new to-do item"))))
                .append(Component.text("  "))
                .append(Component.text("[Clear Done]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/todo clear"))
                        .hoverEvent(HoverEvent.showText(Component.text("Remove all completed items"))));
        player.sendMessage(footer);
        player.sendMessage(Component.empty());
    }

    /**
     * Build a clickable chat line for a single to-do item.
     */
    private Component buildItemLine(TodoItem item) {
        TextComponent.Builder line = Component.text().content("  ");

        if (item.isCompleted()) {
            // Completed: [✓] strikethrough text [undo] [x]
            line.append(Component.text("[\u2713] ", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/todo undone " + item.getId()))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to mark as not done", NamedTextColor.YELLOW))));

            line.append(Component.text(item.getText(), NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
        } else {
            // Pending: [ ] text [✓] [edit] [x]
            line.append(Component.text("[ ] ", NamedTextColor.GRAY)
                    .clickEvent(ClickEvent.runCommand("/todo done " + item.getId()))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to mark as done", NamedTextColor.GREEN))));

            line.append(Component.text(item.getText(), NamedTextColor.WHITE));
        }

        // Action buttons
        line.append(Component.text(" "));

        // Done/undone toggle
        if (!item.isCompleted()) {
            line.append(Component.text("[\u2713]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/todo done " + item.getId()))
                    .hoverEvent(HoverEvent.showText(Component.text("Mark as done"))));
        } else {
            line.append(Component.text("[\u21A9]", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/todo undone " + item.getId()))
                    .hoverEvent(HoverEvent.showText(Component.text("Mark as not done"))));
        }

        line.append(Component.text(" "));

        // Edit button
        line.append(Component.text("[\u270E]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.suggestCommand("/todo edit " + item.getId() + " "))
                .hoverEvent(HoverEvent.showText(Component.text("Edit this item"))));

        line.append(Component.text(" "));

        // Delete button
        line.append(Component.text("[\u2716]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/todo remove " + item.getId()))
                .hoverEvent(HoverEvent.showText(Component.text("Delete this item"))));

        return line.build();
    }

    // ========================
    // Subcommands
    // ========================

    private boolean handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /todo add <text>", NamedTextColor.RED));
            return true;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (text.length() > 200) {
            player.sendMessage(Component.text("To-do text is too long (max 200 characters).", NamedTextColor.RED));
            return true;
        }

        int id = storage.addTodo(player.getUniqueId(), text);
        if (id > 0) {
            player.sendMessage(Component.text("  Added: ", NamedTextColor.GREEN)
                    .append(Component.text(text, NamedTextColor.WHITE)));
            showTodoList(player);
        } else {
            player.sendMessage(Component.text("Failed to add to-do item.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleDone(Player player, String[] args) {
        int id = parseId(player, args);
        if (id < 0) return true;

        if (storage.setCompleted(id, player.getUniqueId(), true)) {
            TodoItem item = storage.getTodo(id, player.getUniqueId());
            player.sendMessage(Component.text("  Completed: ", NamedTextColor.GREEN)
                    .append(Component.text(item != null ? item.getText() : "#" + id, NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH)));
            showTodoList(player);
        } else {
            player.sendMessage(Component.text("Item not found or not yours.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleUndone(Player player, String[] args) {
        int id = parseId(player, args);
        if (id < 0) return true;

        if (storage.setCompleted(id, player.getUniqueId(), false)) {
            player.sendMessage(Component.text("  Marked as not done.", NamedTextColor.YELLOW));
            showTodoList(player);
        } else {
            player.sendMessage(Component.text("Item not found or not yours.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleEdit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /todo edit <id> <new text>", NamedTextColor.RED));
            return true;
        }
        int id = parseId(player, args);
        if (id < 0) return true;

        String newText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (newText.length() > 200) {
            player.sendMessage(Component.text("To-do text is too long (max 200 characters).", NamedTextColor.RED));
            return true;
        }

        if (storage.editTodo(id, player.getUniqueId(), newText)) {
            player.sendMessage(Component.text("  Updated item #" + id, NamedTextColor.AQUA));
            showTodoList(player);
        } else {
            player.sendMessage(Component.text("Item not found or not yours.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        int id = parseId(player, args);
        if (id < 0) return true;

        TodoItem item = storage.getTodo(id, player.getUniqueId());
        if (storage.deleteTodo(id, player.getUniqueId())) {
            player.sendMessage(Component.text("  Removed: ", NamedTextColor.RED)
                    .append(Component.text(item != null ? item.getText() : "#" + id, NamedTextColor.GRAY)));
            showTodoList(player);
        } else {
            player.sendMessage(Component.text("Item not found or not yours.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleClear(Player player) {
        int cleared = storage.clearCompleted(player.getUniqueId());
        player.sendMessage(Component.text("  Cleared " + cleared + " completed item(s).", NamedTextColor.GREEN));
        showTodoList(player);
        return true;
    }

    // ========================
    // Helpers
    // ========================

    private int parseId(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /todo " + args[0] + " <id>", NamedTextColor.RED));
            return -1;
        }
        try {
            return Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid ID: " + args[1], NamedTextColor.RED));
            return -1;
        }
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            return filterStartsWith(args[0], List.of("add", "done", "undone", "edit", "remove", "clear"));
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("done".equals(sub) || "undone".equals(sub) || "edit".equals(sub) || "remove".equals(sub)) {
                // Suggest item IDs
                List<TodoItem> items = storage.getTodos(player.getUniqueId());
                List<String> ids = items.stream()
                        .map(i -> String.valueOf(i.getId()))
                        .collect(Collectors.toList());
                return filterStartsWith(args[1], ids);
            }
        }

        return List.of();
    }

    private List<String> filterStartsWith(String prefix, List<String> options) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
