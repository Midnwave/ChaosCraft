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
 * Main shop GUI — 54 slots (6 rows).
 *
 * Layout:
 *   Row 1: gray dye border
 *   Row 2-4: category icons in center (7 per row, 21 total), claim button at center slot 22
 *   Row 5: gray dye border
 *   Row 6: player head (bottom-left), prev/next page, close (bottom-right)
 */
public class MainShopGUI {

    private static final int SIZE = 54;
    private static final String TITLE = "Shop";
    private static final int CATEGORIES_PER_PAGE = 20; // 21 slots minus 1 for claim button

    // Category grid: rows 2-4, columns 1-7 (excluding borders and claim button)
    private static final int[] CATEGORY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, /* 22 = claim */ 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    // Special slots
    private static final int SLOT_CLAIM = 22;       // Center of the grid — claim your goods
    private static final int SLOT_PLAYER_HEAD = 45;  // Bottom-left corner
    private static final int SLOT_CLOSE = 53;        // Bottom-right corner
    private static final int SLOT_PREV = 47;         // Bottom row, left of center
    private static final int SLOT_NEXT = 51;         // Bottom row, right of center

    private final ChaosCraftPlugin plugin;
    private final ShopService shopService;

    public MainShopGUI(ChaosCraftPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    public void open(Player player, ShopSession session) {
        session.setCurrentType(ShopSession.ShopGUIType.MAIN_CATEGORIES);
        Inventory inv = buildInventory(player, session);
        player.openInventory(inv);
    }

    public Inventory buildInventory(Player player, ShopSession session) {
        Inventory inv = Bukkit.createInventory(
                new ShopGUIListener.ShopInventoryHolder(player.getUniqueId()),
                SIZE,
                Component.text(TITLE, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
        );

        ItemStack border = createDyePane(Material.GRAY_DYE, " ");

        // Fill all with border first
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, border);
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

        // Claim button (center of grid)
        inv.setItem(SLOT_CLAIM, createClaimButton(player));

        // Player head (bottom-left) — shows balance, rank, kills, s-kills, survivals, badges
        inv.setItem(SLOT_PLAYER_HEAD, createPlayerHead(player));

        // Close button (bottom-right)
        inv.setItem(SLOT_CLOSE, createCloseItem());

        // Navigation
        if (page > 0) {
            inv.setItem(SLOT_PREV, createNavItem(Material.ARROW, "Previous Page", page));
        }
        if (page < maxPage) {
            inv.setItem(SLOT_NEXT, createNavItem(Material.ARROW, "Next Page", page + 2));
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

        meta.displayName(Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        // Rank
        String rank = shopService.getRankDisplay(player);
        if (rank != null && !rank.isEmpty()) {
            lore.add(Component.text("Rank: ", NamedTextColor.GRAY)
                    .append(LegacyComponentSerializer.legacySection().deserialize(rank))
                    .decoration(TextDecoration.ITALIC, false));
        }

        // Balance
        lore.add(Component.text("Balance: ", NamedTextColor.GRAY)
                .append(Component.text("$" + String.format("%.2f", shopService.getBalance(player)), NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());

        // Stats
        PlayerStatsService stats = plugin.getPlayerStatsService();
        int kills = stats != null ? stats.getKills(player.getUniqueId()) : 0;
        int sKills = stats != null ? stats.getSKills(player.getUniqueId()) : 0;
        int survivals = stats != null ? stats.getModeSurvivals(player.getUniqueId()) : 0;

        lore.add(Component.text("Kills: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(kills), NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("S-Kills: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(sKills), NamedTextColor.LIGHT_PURPLE))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Mode Survivals: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(survivals), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));

        // Badges
        var badgeService = plugin.getBadgeService();
        if (badgeService != null) {
            int earned = badgeService.getPlayerBadges(player.getUniqueId()).size();
            int total = badgeService.getAllBadgeIds().size();
            lore.add(Component.text("Badges: ", NamedTextColor.GRAY)
                    .append(Component.text(earned + "/" + total, NamedTextColor.DARK_PURPLE))
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createClaimButton(Player player) {
        ItemStack item = new ItemStack(Material.BUNDLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Claim Your Goods", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        // Show pending item count
        var storage = shopService.getVirtualStorage();
        int pending = storage != null ? storage.getItems(player.getUniqueId()).size() : 0;
        if (pending > 0) {
            lore.add(Component.text(pending + " item(s) waiting!", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("No items to claim", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Click to open storage", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
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

    private ItemStack createDyePane(Material material, String name) {
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

    public static int getClaimSlot() {
        return SLOT_CLAIM;
    }

    public static int getCloseSlot() {
        return SLOT_CLOSE;
    }

    public static int getPlayerHeadSlot() {
        return SLOT_PLAYER_HEAD;
    }

    public static int getPrevSlot() {
        return SLOT_PREV;
    }

    public static int getNextSlot() {
        return SLOT_NEXT;
    }
}
