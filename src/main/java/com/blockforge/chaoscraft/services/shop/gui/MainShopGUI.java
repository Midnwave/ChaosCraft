package com.blockforge.chaoscraft.services.shop.gui;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.shop.ShopCategory;
import com.blockforge.chaoscraft.services.shop.ShopService;
import com.blockforge.chaoscraft.services.stats.PlayerStatsService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Main shop GUI showing category icons, player info, and navigation.
 * 54-slot (6 rows) layout with categories in the center and player stats on the right column.
 */
public class MainShopGUI {

    private static final int SIZE = 54;
    private static final String TITLE = "Shop";
    private static final int CATEGORIES_PER_PAGE = 21; // 7 per row * 3 rows

    // Category grid slots: rows 2-4, columns 0-6 (slots 9-15, 18-24, 27-33)
    private static final int[] CATEGORY_SLOTS = {
            9, 10, 11, 12, 13, 14, 15,
            18, 19, 20, 21, 22, 23, 24,
            27, 28, 29, 30, 31, 32, 33
    };

    // Player info column slots
    private static final int SLOT_PLAYER_HEAD = 16;
    private static final int SLOT_BALANCE = 17;
    private static final int SLOT_KILLS = 25;
    private static final int SLOT_SKILLS = 26;
    private static final int SLOT_SURVIVALS = 34;
    private static final int SLOT_BADGES = 35;

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;

    public MainShopGUI(ChaosCraftPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    /**
     * Open the main shop GUI for a player.
     */
    public void open(Player player, ShopSession session) {
        session.setCurrentType(ShopSession.ShopGUIType.MAIN_CATEGORIES);
        Inventory inv = buildInventory(player, session);
        player.openInventory(inv);
    }

    /**
     * Build the main shop inventory for the given session state.
     */
    public Inventory buildInventory(Player player, ShopSession session) {
        Inventory inv = Bukkit.createInventory(
                new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                SIZE,
                Component.text(TITLE, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
        );

        ItemStack grayPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");

        // Row 1 (0-8): border
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, grayPane);
        }

        // Row 5 (36-44): border
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, grayPane);
        }

        // Row 6 (45-53): navigation row
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        // Categories
        List<ShopCategory> categories = shopService.getCategories();
        int page = session.getPage();
        int maxPage = Math.max(0, (categories.size() - 1) / CATEGORIES_PER_PAGE);
        page = Math.max(0, Math.min(page, maxPage));
        session.setPage(page);

        int startIndex = page * CATEGORIES_PER_PAGE;
        int endIndex = Math.min(startIndex + CATEGORIES_PER_PAGE, categories.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIdx = i - startIndex;
            if (slotIdx >= CATEGORY_SLOTS.length) break;

            ShopCategory category = categories.get(i);
            ItemStack icon = createCategoryIcon(category);
            inv.setItem(CATEGORY_SLOTS[slotIdx], icon);
        }

        // Player info column
        inv.setItem(SLOT_PLAYER_HEAD, createPlayerHead(player));
        inv.setItem(SLOT_BALANCE, createBalanceItem(player));
        inv.setItem(SLOT_KILLS, createKillsItem(player));
        inv.setItem(SLOT_SKILLS, createSKillsItem(player));
        inv.setItem(SLOT_SURVIVALS, createSurvivalsItem(player));
        inv.setItem(SLOT_BADGES, createBadgesItem(player));

        // Navigation
        if (page > 0) {
            inv.setItem(45, createNavItem(Material.ARROW, "Previous Page", page));
        }
        inv.setItem(49, createCloseItem());
        if (page < maxPage) {
            inv.setItem(53, createNavItem(Material.ARROW, "Next Page", page + 2));
        }

        return inv;
    }

    // ========================
    // Item builders
    // ========================

    private ItemStack createCategoryIcon(ShopCategory category) {
        Material mat = Material.matchMaterial(category.getIconMaterial());
        if (mat == null) mat = Material.CHEST;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        Component displayName = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(category.getDisplayName())
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(displayName);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(category.getItemCount() + " items", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to browse", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        if (category.getCustomModelData() > 0) {
            meta.setCustomModelData(category.getCustomModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);

        String rank = shopService.getRankDisplay(player);
        Component nameComponent = Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(nameComponent);

        List<Component> lore = new ArrayList<>();
        if (rank != null && !rank.isEmpty()) {
            lore.add(Component.text("Rank: ", NamedTextColor.GRAY)
                    .append(LegacyComponentSerializer.legacySection().deserialize(rank))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Balance: ", NamedTextColor.GRAY)
                .append(Component.text("$" + String.format("%.2f", shopService.getBalance(player)), NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createBalanceItem(Player player) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Balance", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("$" + String.format("%.2f", shopService.getBalance(player)), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createKillsItem(Player player) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Kills", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        int kills = 0;
        PlayerStatsService stats = plugin.getPlayerStatsService();
        if (stats != null) kills = stats.getKills(player.getUniqueId());

        meta.lore(List.of(
                Component.text(String.valueOf(kills), NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSKillsItem(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("S-Kills", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        int sKills = 0;
        PlayerStatsService stats = plugin.getPlayerStatsService();
        if (stats != null) sKills = stats.getSKills(player.getUniqueId());

        meta.lore(List.of(
                Component.text(String.valueOf(sKills), NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSurvivalsItem(Player player) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Mode Survivals", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        int survivals = 0;
        PlayerStatsService stats = plugin.getPlayerStatsService();
        if (stats != null) survivals = stats.getModeSurvivals(player.getUniqueId());

        meta.lore(List.of(
                Component.text(String.valueOf(survivals), NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBadgesItem(Player player) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Badges", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("View your earned badges", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavItem(Material material, String name, int targetPage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Close", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    // ========================
    // Slot helpers (for click routing)
    // ========================

    public static int[] getCategorySlots() {
        return CATEGORY_SLOTS;
    }

    public static int getCategoriesPerPage() {
        return CATEGORIES_PER_PAGE;
    }
}
