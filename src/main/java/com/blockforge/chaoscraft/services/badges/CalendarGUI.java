package com.blockforge.chaoscraft.services.badges;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;

/**
 * Admin date picker GUI for setting badge expiry dates.
 * Three-step flow: Year -> Month -> Day.
 * On selection, calls a callback Consumer<Long> with epoch millis.
 */
public class CalendarGUI implements Listener {

    private final ChaosCraftPlugin plugin;
    private final Map<UUID, CalendarSession> sessions = new HashMap<>();

    private static final int SIZE = 54;
    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };
    private static final int[] DAYS_IN_MONTH = {
            31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    public CalendarGUI(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open the year selection screen for a player.
     *
     * @param player   the admin player
     * @param callback called with the selected date as epoch millis
     */
    public void open(Player player, Consumer<Long> callback) {
        CalendarSession session = new CalendarSession(player.getUniqueId(), callback);
        sessions.put(player.getUniqueId(), session);
        openYearScreen(player, session);
    }

    // ========================
    // Year Screen
    // ========================

    private void openYearScreen(Player player, CalendarSession session) {
        session.stage = CalendarStage.YEAR;

        Inventory inv = Bukkit.createInventory(new CalendarGUIHolder(player.getUniqueId()), SIZE,
                Component.text("Select Year", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        // Background
        ItemStack background = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, background);
        }

        // Years 2022-2030 in the middle area
        int[] years = {2022, 2023, 2024, 2025, 2026, 2027, 2028, 2029, 2030};
        int slot = 19; // Start row 3, column 2
        for (int year : years) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(String.valueOf(year), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Click to select " + year, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            session.slotToYear.put(slot, year);
            slot++;
            if (slot == 26) slot = 28; // Skip to next row
        }

        // Cancel button at slot 49
        inv.setItem(49, createCancelItem());

        player.openInventory(inv);
    }

    // ========================
    // Month Screen
    // ========================

    private void openMonthScreen(Player player, CalendarSession session) {
        session.stage = CalendarStage.MONTH;

        Inventory inv = Bukkit.createInventory(new CalendarGUIHolder(player.getUniqueId()), SIZE,
                Component.text("Select Month (" + session.selectedYear + ")", NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));

        // Background
        ItemStack background = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, background);
        }

        // Months displayed as CLOCK items in the middle rows
        int slot = 19; // Start row 3, column 2
        for (int month = 0; month < 12; month++) {
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(MONTH_NAMES[month], NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Click to select " + MONTH_NAMES[month], NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            session.slotToMonth.put(slot, month);
            slot++;
            // After 7 items in a row, skip border columns
            if ((slot % 9) == 8) slot += 2;
        }

        // Back button at slot 45
        inv.setItem(45, createBackItem());

        // Cancel button at slot 49
        inv.setItem(49, createCancelItem());

        player.openInventory(inv);
    }

    // ========================
    // Day Screen
    // ========================

    private void openDayScreen(Player player, CalendarSession session) {
        session.stage = CalendarStage.DAY;

        int daysInMonth = getDaysInMonth(session.selectedYear, session.selectedMonth);

        Inventory inv = Bukkit.createInventory(new CalendarGUIHolder(player.getUniqueId()), SIZE,
                Component.text(MONTH_NAMES[session.selectedMonth] + " " + session.selectedYear, NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));

        // Background
        ItemStack background = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, background);
        }

        // Days as PAPER items with amount = day number
        int slot = 9; // Start at row 2
        for (int day = 1; day <= daysInMonth; day++) {
            ItemStack item = new ItemStack(Material.PAPER, day);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Day " + day, NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text(MONTH_NAMES[session.selectedMonth] + " " + day + ", " + session.selectedYear, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            session.slotToDay.put(slot, day);
            slot++;
            // Skip last column for border
            if ((slot % 9) == 8) slot += 1;
        }

        // Back button at slot 45
        inv.setItem(45, createBackItem());

        // Cancel button at slot 49
        inv.setItem(49, createCancelItem());

        player.openInventory(inv);
    }

    // ========================
    // Utility
    // ========================

    private int getDaysInMonth(int year, int month) {
        if (month == 1) {
            // February — check leap year
            if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                return 29;
            }
            return 28;
        }
        return DAYS_IN_MONTH[month];
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCancelItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Cancel", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Back", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    // ========================
    // Event Handlers
    // ========================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof CalendarGUIHolder)) return;

        event.setCancelled(true);

        CalendarSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (event.getClickedInventory() != topInv) return;

        int slot = event.getSlot();

        // Cancel button
        if (slot == 49) {
            sessions.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            return;
        }

        switch (session.stage) {
            case YEAR -> {
                Integer year = session.slotToYear.get(slot);
                if (year != null) {
                    session.selectedYear = year;
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> openMonthScreen(player, session));
                }
            }
            case MONTH -> {
                // Back button
                if (slot == 45) {
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> openYearScreen(player, session));
                    return;
                }
                Integer month = session.slotToMonth.get(slot);
                if (month != null) {
                    session.selectedMonth = month;
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> openDayScreen(player, session));
                }
            }
            case DAY -> {
                // Back button
                if (slot == 45) {
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> openMonthScreen(player, session));
                    return;
                }
                Integer day = session.slotToDay.get(slot);
                if (day != null) {
                    session.selectedDay = day;

                    // Build epoch millis from selected date (end of day, 23:59:59)
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    cal.set(session.selectedYear, session.selectedMonth, session.selectedDay, 23, 59, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    long epochMillis = cal.getTimeInMillis();

                    // Invoke callback
                    Consumer<Long> callback = session.callback;
                    sessions.remove(player.getUniqueId());
                    Bukkit.getScheduler().runTask(plugin, (Runnable) () -> {
                        player.closeInventory();
                        callback.accept(epochMillis);
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getTopInventory().getHolder() instanceof CalendarGUIHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            sessions.remove(player.getUniqueId());
        }
    }

    // ========================
    // Inner Classes
    // ========================

    private enum CalendarStage {
        YEAR, MONTH, DAY
    }

    private static class CalendarSession {
        final UUID playerId;
        final Consumer<Long> callback;
        CalendarStage stage = CalendarStage.YEAR;
        int selectedYear;
        int selectedMonth;
        int selectedDay;
        final Map<Integer, Integer> slotToYear = new HashMap<>();
        final Map<Integer, Integer> slotToMonth = new HashMap<>();
        final Map<Integer, Integer> slotToDay = new HashMap<>();

        CalendarSession(UUID playerId, Consumer<Long> callback) {
            this.playerId = playerId;
            this.callback = callback;
        }
    }

    private static class CalendarGUIHolder implements InventoryHolder {
        private final UUID playerId;

        CalendarGUIHolder(UUID playerId) {
            this.playerId = playerId;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
