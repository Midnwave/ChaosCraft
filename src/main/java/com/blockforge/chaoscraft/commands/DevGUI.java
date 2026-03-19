package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Developer GUI showing all registered commands, permissions, and placeholders.
 * Opened via /cc devs gui
 */
public class DevGUI implements Listener {

    private final ChaosCraftPlugin plugin;
    private static final Component TITLE_COMMANDS = Component.text("ChaosCraft — Commands & Permissions", NamedTextColor.DARK_PURPLE);
    private static final Component TITLE_PLACEHOLDERS = Component.text("ChaosCraft — Placeholders", NamedTextColor.DARK_AQUA);

    public DevGUI(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the commands/permissions page.
     */
    public void openCommandsPage(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_COMMANDS);

        // Header
        inv.setItem(4, createItem(Material.COMMAND_BLOCK, "&5&lCommand Reference",
                "&7All registered commands,", "&7permissions, and descriptions.",
                "&8Click items for details."));

        // Core commands
        int slot = 9;
        slot = addCommand(inv, slot, "/cc help", "chaoscraft.use", "Show the full help menu with all available commands");
        slot = addCommand(inv, slot, "/cc modes <mode> <cmd>", "chaoscraft.admin", "Unified mode management — start, stop, test, status, debug");
        slot = addCommand(inv, slot, "/cc modes <mode> start", "chaoscraft.mode.trigger", "Start a game mode");
        slot = addCommand(inv, slot, "/cc modes <mode> stop", "chaoscraft.mode.end", "Stop the active game mode");
        slot = addCommand(inv, slot, "/cc timer <action> [val]", "chaoscraft.admin", "Timer management: set, add, remove, pause, resume");
        slot = addCommand(inv, slot, "/cc reload", "chaoscraft.admin", "Reload all ChaosCraft configs");
        slot = addCommand(inv, slot, "/cc exempt <action> [player]", "chaoscraft.admin", "Manage attack-exempt players: add, remove, list");
        slot = addCommand(inv, slot, "/cc update <check|download>", "chaoscraft.admin", "Check for or download plugin updates from GitHub");
        slot = addCommand(inv, slot, "/cc devs gui", "chaoscraft.devs", "Open this developer GUI");
        slot = addCommand(inv, slot, "/cc devs setgem", "chaoscraft.devs", "Set the held item as the Calamity gem item");
        slot = addCommand(inv, slot, "/cc devs setscitem", "chaoscraft.devs", "Set the held item as the Supreme Calamitas item");
        slot = addCommand(inv, slot, "/cc dog <spawn|kill|status>", "chaoscraft.devs", "Devourer of Gods datapack entity management");
        slot = addCommand(inv, slot, "/cc debug <target>", "chaoscraft.devs", "Debug info: calamitas, dog, attacks");

        // Mode-specific commands
        slot = addCommand(inv, slot, "/cc modes calamity status", "chaoscraft.calamity.admin", "Show Calamity mode state, phase, gems, attacks");
        slot = addCommand(inv, slot, "/cc modes calamity test* <phase> <id>", "chaoscraft.calamity.admin", "Force-spawn block display, environmental, or boss attack");
        slot = addCommand(inv, slot, "/cc modes calamity setphase <1-5>", "chaoscraft.calamity.admin", "Force change Calamity phase");
        slot = addCommand(inv, slot, "/cc modes calamity spawnboss <name>", "chaoscraft.calamity.admin", "Manually spawn a Calamity boss");
        slot = addCommand(inv, slot, "/cc modes chain status", "chaoscraft.chain.admin", "Show Chain mode state and attack info");
        slot = addCommand(inv, slot, "/cc modes chain test <id>", "chaoscraft.chain.admin", "Force-spawn a chain attack on yourself");
        slot = addCommand(inv, slot, "/cc modes chain list", "chaoscraft.chain.admin", "List all 115 chain attack IDs with config info");

        // Service commands
        slot = addCommand(inv, slot, "/cc entertitlescreen <player>", "chaoscraft.titlescreen", "Force a player into the title screen");
        slot = addCommand(inv, slot, "/cc exittitlescreen <player>", "chaoscraft.titlescreen", "Force a player out of the title screen");
        slot = addCommand(inv, slot, "/settings", "chaoscraft.settings.use", "Open the player settings menu");
        slot = addCommand(inv, slot, "/codes", "(none)", "Enter a promotional code");
        slot = addCommand(inv, slot, "/itemtag <add|remove|list>", "chaoscraft.itemtag.use", "View and manage item tags");
        slot = addCommand(inv, slot, "/ccperf <action>", "chaoscraft.performance.admin", "MythicMobs performance limiter management");

        // Navigation to placeholders page
        inv.setItem(53, createItem(Material.ENDER_EYE, "&a&lPlaceholders >>",
                "&7Click to view all", "&7registered placeholders."));

        player.openInventory(inv);
    }

    /**
     * Open the placeholders page.
     */
    public void openPlaceholdersPage(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PLACEHOLDERS);

        // Header
        inv.setItem(4, createItem(Material.NAME_TAG, "&b&lPlaceholder Reference",
                "&7All registered PlaceholderAPI", "&7expansions and identifiers."));

        int slot = 9;

        // Game-mode placeholders (%chaoscraft_*%)
        slot = addPlaceholder(inv, slot, "%chaoscraft_timer_hh_mm_ss%", "Timer in HH:MM:SS format");
        slot = addPlaceholder(inv, slot, "%chaoscraft_timer_mm_ss%", "Timer in MM:SS format");
        slot = addPlaceholder(inv, slot, "%chaoscraft_timer_m_ss%", "Timer in M:SS format");
        slot = addPlaceholder(inv, slot, "%chaoscraft_timer_ss_ms%", "Timer in SS.ms format");
        slot = addPlaceholder(inv, slot, "%chaoscraft_timer_raw%", "Timer raw seconds");
        slot = addPlaceholder(inv, slot, "%chaoscraft_mode%", "Active mode name or 'none'");
        slot = addPlaceholder(inv, slot, "%chaoscraft_mode_active%", "true/false if any mode is active");
        slot = addPlaceholder(inv, slot, "%chaoscraft_survived%", "true/false if player survived current mode");
        slot = addPlaceholder(inv, slot, "%chaoscraft_gems%", "Global gem count (Calamity)");
        slot = addPlaceholder(inv, slot, "%chaoscraft_gems_deposited%", "Deposited gem count (Calamity)");
        slot = addPlaceholder(inv, slot, "%chaoscraft_gems_required%", "Current boss gem requirement");
        slot = addPlaceholder(inv, slot, "%chaoscraft_phase%", "Current Calamity phase (1-5)");
        slot = addPlaceholder(inv, slot, "%chaoscraft_dog_alive%", "DoG alive true/false");
        slot = addPlaceholder(inv, slot, "%chaoscraft_dog_health%", "DoG current HP");
        slot = addPlaceholder(inv, slot, "%chaoscraft_dog_health_pct%", "DoG health percentage");
        slot = addPlaceholder(inv, slot, "%chaoscraft_dog_phase2%", "DoG Universal Collapse active");

        // Title screen placeholders
        slot = addPlaceholder(inv, slot, "%chaoscraft_loadingstatus%", "Title screen loading stage name");
        slot = addPlaceholder(inv, slot, "%chaoscraft_loadingprogress%", "Title screen loading progress value");
        slot = addPlaceholder(inv, slot, "%chaoscraft_isloading%", "true/false if player is loading");
        slot = addPlaceholder(inv, slot, "%chaoscraft_verificationcode%", "Player's verification code");
        slot = addPlaceholder(inv, slot, "%chaoscraft_awaitingverification%", "true/false if awaiting verification");
        slot = addPlaceholder(inv, slot, "%chaoscraft_tickstill20%", "Ticks until next 20-tick cycle");
        slot = addPlaceholder(inv, slot, "%chaoscraft_ping%", "Player ping in ms");

        // Cross-service placeholders (%cc_*%)
        slot = addPlaceholder(inv, slot, "%cc_isintitlescreen%", "true/false if in title screen");
        slot = addPlaceholder(inv, slot, "%cc_verificationcode%", "Verification code (cross-service)");
        slot = addPlaceholder(inv, slot, "%cc_codestatus%", "Current promotional code status text");
        slot = addPlaceholder(inv, slot, "%cc_settings_status%", "Settings service status");
        slot = addPlaceholder(inv, slot, "%cc_play_status%", "Play/agreement status message");

        // Navigation back to commands page
        inv.setItem(45, createItem(Material.ENDER_EYE, "&a&l<< Commands",
                "&7Click to view all", "&7registered commands."));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Component title = event.getView().title();

        boolean isCommandsPage = TITLE_COMMANDS.equals(title);
        boolean isPlaceholdersPage = TITLE_PLACEHOLDERS.equals(title);

        if (!isCommandsPage && !isPlaceholdersPage) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Navigate between pages
        if (isCommandsPage && event.getSlot() == 53 && clicked.getType() == Material.ENDER_EYE) {
            openPlaceholdersPage(player);
        } else if (isPlaceholdersPage && event.getSlot() == 45 && clicked.getType() == Material.ENDER_EYE) {
            openCommandsPage(player);
        }
    }

    // ---- Helpers ----

    private int addCommand(Inventory inv, int slot, String command, String permission, String description) {
        if (slot >= 53) return slot; // Don't overflow into nav slot
        inv.setItem(slot, createItem(Material.COMMAND_BLOCK,
                "&e" + command,
                "&7Permission: &f" + permission,
                "&7" + description));
        return slot + 1;
    }

    private int addPlaceholder(Inventory inv, int slot, String placeholder, String description) {
        if (slot >= 53) return slot;
        inv.setItem(slot, createItem(Material.NAME_TAG,
                "&b" + placeholder,
                "&7" + description));
        return slot + 1;
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(name));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(colorize(line));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private Component colorize(String text) {
        // Simple & color code support
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(text).decoration(TextDecoration.ITALIC, false);
    }
}
