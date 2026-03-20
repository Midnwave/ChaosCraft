package com.blockforge.chaoscraft.services.shop;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.shop.gui.ShopGUIListener;
import com.blockforge.chaoscraft.services.shop.gui.ShopSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command handler for /shop, /market, and /cc shop subcommands.
 *
 * Player commands:
 *   /shop, /market — Open the main shop GUI
 *   /cc shop storage — Open virtual storage
 *
 * Admin commands (chaoscraft.shop.admin):
 *   /cc shop additem <buy/none> <sell/none> <kills/none> <skills/none> <badge/none>
 *   /cc shop addhanditem <buy/none> <sell/none> <kills/none> <skills/none> <badge/none>
 *   /cc shop createsection <id> <display name...>
 *   /cc shop deletesection <id>
 *   /cc shop renamesection <id> <new name...>
 *   /cc shop editshop
 *   /cc shop reload
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;
    private final ShopGUIListener guiListener;

    private static final List<String> ADMIN_SUBS = List.of(
            "additem", "addhanditem", "createsection", "deletesection",
            "renamesection", "editshop", "reload", "storage"
    );

    public ShopCommand(ChaosCraftPlugin plugin, ShopService shopService, ShopGUIListener guiListener) {
        this.plugin = plugin;
        this.shopService = shopService;
        this.guiListener = guiListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // /shop and /market with no args — open shop
        if (label.equalsIgnoreCase("shop") || label.equalsIgnoreCase("market")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use the shop.", NamedTextColor.RED));
                return true;
            }
            guiListener.openMainShop(player);
            return true;
        }

        // /cc shop ... (args come pre-stripped by ChaosCraftCommand: args[0] is first sub-arg)
        if (args.length == 0) {
            if (sender instanceof Player player) {
                guiListener.openMainShop(player);
            } else {
                sender.sendMessage(Component.text("Only players can use the shop.", NamedTextColor.RED));
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "storage" -> handleStorage(sender);
            case "additem" -> handleAddItem(sender, args);
            case "addhanditem" -> handleAddHandItem(sender, args);
            case "createsection" -> handleCreateSection(sender, args);
            case "deletesection" -> handleDeleteSection(sender, args);
            case "renamesection" -> handleRenameSection(sender, args);
            case "editshop" -> handleEditShop(sender);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown shop subcommand: " + sub, NamedTextColor.RED));
                yield true;
            }
        };
    }

    // ========================
    // Storage
    // ========================

    private boolean handleStorage(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use virtual storage.", NamedTextColor.RED));
            return true;
        }
        guiListener.openVirtualStorage(player);
        return true;
    }

    // ========================
    // Add Item (opens GUI to drag item in)
    // ========================

    private boolean handleAddItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.shop.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        // Parse: additem <buy/none> <sell/none> <kills/none> <skills/none> <badge/none>
        if (args.length < 6) {
            sender.sendMessage(Component.text("Usage: /cc shop additem <buy/none> <sell/none> <kills/none> <skills/none> <badge/none>", NamedTextColor.RED));
            return true;
        }

        double buyPrice = parsePrice(args[1]);
        double sellPrice = parsePrice(args[2]);
        int kills = parseInt(args[3]);
        int sKills = parseInt(args[4]);
        String badge = args[5].equalsIgnoreCase("none") ? null : args[5];

        player.sendMessage(Component.text("Add item GUI coming soon. For now, use /cc shop addhanditem with the item in your hand.", NamedTextColor.YELLOW));
        return true;
    }

    // ========================
    // Add Hand Item
    // ========================

    private boolean handleAddHandItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.shop.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 6) {
            sender.sendMessage(Component.text("Usage: /cc shop addhanditem <buy/none> <sell/none> <kills/none> <skills/none> <badge/none>", NamedTextColor.RED));
            return true;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            sender.sendMessage(Component.text("You must be holding an item.", NamedTextColor.RED));
            return true;
        }

        double buyPrice = parsePrice(args[1]);
        double sellPrice = parsePrice(args[2]);
        int kills = parseInt(args[3]);
        int sKills = parseInt(args[4]);
        String badge = args[5].equalsIgnoreCase("none") ? null : args[5];

        // Check if we have at least one category
        List<ShopCategory> categories = shopService.getCategories();
        if (categories.isEmpty()) {
            sender.sendMessage(Component.text("No shop sections exist. Create one first with /cc shop createsection.", NamedTextColor.RED));
            return true;
        }

        // Add to the first category (admin can reorganize later)
        ShopCategory firstCat = categories.get(0);
        ShopItem newItem = new ShopItem(
                handItem.getType().name(), buyPrice, sellPrice,
                kills, sKills, 0, badge,
                List.of(), -1, null
        );
        firstCat.addItem(newItem);
        shopService.getConfig().saveCategory(firstCat);

        sender.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                .append(Component.text(handItem.getType().name(), NamedTextColor.WHITE))
                .append(Component.text(" to section '", NamedTextColor.GREEN))
                .append(Component.text(firstCat.getId(), NamedTextColor.YELLOW))
                .append(Component.text("'.", NamedTextColor.GREEN)));
        return true;
    }

    // ========================
    // Create Section
    // ========================

    private boolean handleCreateSection(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.shop.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /cc shop createsection <id> <display name...>", NamedTextColor.RED));
            return true;
        }

        String id = args[1].toLowerCase();
        String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (shopService.getCategory(id) != null) {
            sender.sendMessage(Component.text("Section '" + id + "' already exists.", NamedTextColor.RED));
            return true;
        }

        ShopCategory category = new ShopCategory(id, displayName, "CHEST", 0, -1);
        shopService.getConfig().saveCategory(category);

        sender.sendMessage(Component.text("Created shop section '", NamedTextColor.GREEN)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName))
                .append(Component.text("' (id: " + id + ").", NamedTextColor.GREEN)));
        return true;
    }

    // ========================
    // Delete Section
    // ========================

    private boolean handleDeleteSection(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.shop.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /cc shop deletesection <id>", NamedTextColor.RED));
            return true;
        }

        String id = args[1].toLowerCase();
        if (shopService.getCategory(id) == null) {
            sender.sendMessage(Component.text("Section '" + id + "' does not exist.", NamedTextColor.RED));
            return true;
        }

        shopService.getConfig().deleteCategory(id);
        sender.sendMessage(Component.text("Deleted shop section '" + id + "'.", NamedTextColor.RED));
        return true;
    }

    // ========================
    // Rename Section
    // ========================

    private boolean handleRenameSection(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chaoscraft.shop.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /cc shop renamesection <id> <new name...>", NamedTextColor.RED));
            return true;
        }

        String id = args[1].toLowerCase();
        ShopCategory category = shopService.getCategory(id);
        if (category == null) {
            sender.sendMessage(Component.text("Section '" + id + "' does not exist.", NamedTextColor.RED));
            return true;
        }

        String newName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        category.setDisplayName(newName);
        shopService.getConfig().saveCategory(category);

        sender.sendMessage(Component.text("Renamed section '" + id + "' to ", NamedTextColor.GREEN)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(newName))
                .append(Component.text(".", NamedTextColor.GREEN)));
        return true;
    }

    // ========================
    // Edit Shop
    // ========================

    private boolean handleEditShop(CommandSender sender) {
        if (!sender.hasPermission("chaoscraft.shop.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can edit the shop.", NamedTextColor.RED));
            return true;
        }
        guiListener.openMainShopEditMode(player);
        return true;
    }

    // ========================
    // Reload
    // ========================

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("chaoscraft.shop.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        shopService.reload();
        sender.sendMessage(Component.text("Shop configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }

    // ========================
    // Tab Complete
    // ========================

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        // /shop or /market — no completions
        if (label.equalsIgnoreCase("shop") || label.equalsIgnoreCase("market")) {
            return List.of();
        }

        // /cc shop <tab>
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("storage");
            if (sender.hasPermission("chaoscraft.shop.admin")) {
                subs.addAll(ADMIN_SUBS);
            }
            return filterCompletions(subs, args[0]);
        }

        // /cc shop <sub> <tab>
        if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "deletesection", "renamesection" -> {
                    if (args.length == 2) {
                        return filterCompletions(getCategoryIds(), args[1]);
                    }
                }
                case "additem", "addhanditem" -> {
                    return switch (args.length) {
                        case 2 -> filterCompletions(List.of("none", "10", "50", "100", "500"), args[1]); // buy price
                        case 3 -> filterCompletions(List.of("none", "5", "25", "50", "250"), args[2]);   // sell price
                        case 4 -> filterCompletions(List.of("none", "0", "10", "50", "100"), args[3]);   // kills
                        case 5 -> filterCompletions(List.of("none", "0", "5", "25"), args[4]);            // skills
                        case 6 -> filterCompletions(List.of("none"), args[5]);                             // badge
                        default -> List.of();
                    };
                }
            }
        }

        return List.of();
    }

    // ========================
    // Utility
    // ========================

    private double parsePrice(String input) {
        if (input.equalsIgnoreCase("none")) return -1;
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseInt(String input) {
        if (input.equalsIgnoreCase("none")) return 0;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<String> getCategoryIds() {
        return shopService.getCategories().stream()
                .map(ShopCategory::getId)
                .collect(Collectors.toList());
    }

    private List<String> filterCompletions(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
