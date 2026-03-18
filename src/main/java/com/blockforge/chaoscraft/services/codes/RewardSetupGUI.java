package com.blockforge.chaoscraft.services.codes;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Admin GUI for configuring code rewards (items, Vault money, commands).
 */
public class RewardSetupGUI implements Listener {

    private final ChaosCraftPlugin plugin;
    private final Map<UUID, GUISession> sessions = new HashMap<>();

    private static final String MAIN_TITLE_PREFIX = "\u00A76Reward Setup: \u00A7e";
    private static final String ITEMS_TITLE_PREFIX = "\u00A76Item Rewards: \u00A7e";
    private static final String CMDS_TITLE_PREFIX = "\u00A76Commands: \u00A7e";

    public RewardSetupGUI(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ---- Open screens ----

    public void openMainGUI(Player player, CodeData codeData) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE_PREFIX + codeData.getName());

        inv.setItem(10, buildButton(Material.CHEST, "\u00A76Item Rewards",
                List.of("\u00A77Click to add items", "\u00A77Current: \u00A7f" + codeData.getItemRewards().size() + " items")));
        inv.setItem(12, buildButton(Material.GOLD_INGOT, "\u00A76Vault Money",
                List.of("\u00A77Click to set amount", "\u00A77Current: \u00A7f$" + codeData.getVaultMoney())));
        inv.setItem(14, buildButton(Material.COMMAND_BLOCK, "\u00A76Commands",
                List.of("\u00A77Click to add commands", "\u00A77Current: \u00A7f" + codeData.getCommands().size() + " commands")));
        inv.setItem(16, buildButton(Material.EMERALD, "\u00A7a\u00A7lSAVE & EXIT",
                List.of("\u00A77Click to save rewards")));

        player.openInventory(inv);
        sessions.put(player.getUniqueId(), new GUISession(codeData, GUIType.MAIN));
    }

    private void openItemsGUI(Player player, CodeData codeData) {
        Inventory inv = Bukkit.createInventory(null, 54, ITEMS_TITLE_PREFIX + codeData.getName());
        List<ItemStack> items = codeData.getItemRewards();
        for (int i = 0; i < items.size() && i < 45; i++) {
            inv.setItem(i, items.get(i));
        }
        inv.setItem(53, buildButton(Material.ARROW, "\u00A7c\u2190 Back", List.of()));
        player.openInventory(inv);
        sessions.put(player.getUniqueId(), new GUISession(codeData, GUIType.ITEMS));
    }

    private void openCommandsGUI(Player player, CodeData codeData) {
        Inventory inv = Bukkit.createInventory(null, 27, CMDS_TITLE_PREFIX + codeData.getName());

        inv.setItem(10, buildButton(Material.COMMAND_BLOCK, "\u00A76Add Console Command",
                List.of("\u00A77Click and type in chat")));
        inv.setItem(12, buildButton(Material.COMMAND_BLOCK_MINECART, "\u00A76Add Player Command",
                List.of("\u00A77Click and type in chat")));

        List<CodeData.CodeCommand> cmds = codeData.getCommands();
        for (int i = 0; i < cmds.size() && i < 5; i++) {
            CodeData.CodeCommand cmd = cmds.get(i);
            Material mat = cmd.asPlayer() ? Material.PLAYER_HEAD : Material.REPEATING_COMMAND_BLOCK;
            inv.setItem(18 + i, buildButton(mat, "\u00A7e" + cmd.command(),
                    List.of(cmd.asPlayer() ? "\u00A77Runs as: \u00A7fPlayer" : "\u00A77Runs as: \u00A7fConsole",
                            "\u00A7cRight-click to remove")));
        }

        inv.setItem(26, buildButton(Material.ARROW, "\u00A7c\u2190 Back", List.of()));
        player.openInventory(inv);
        sessions.put(player.getUniqueId(), new GUISession(codeData, GUIType.COMMANDS));
    }

    // ---- Event handlers ----

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GUISession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        String title = event.getView().getTitle();
        if (!title.startsWith("\u00A76Reward Setup:") && !title.startsWith("\u00A76Item Rewards:") && !title.startsWith("\u00A76Commands:"))
            return;

        switch (session.type) {
            case MAIN -> {
                event.setCancelled(true);
                handleMainGUIClick(player, session.codeData, event.getSlot());
            }
            case ITEMS -> handleItemsGUIClick(player, session.codeData, event);
            case COMMANDS -> {
                event.setCancelled(true);
                handleCommandsGUIClick(player, session.codeData, event.getSlot(), event.isRightClick());
            }
        }
    }

    private void handleMainGUIClick(Player player, CodeData codeData, int slot) {
        switch (slot) {
            case 10 -> openItemsGUI(player, codeData);
            case 12 -> {
                player.closeInventory();
                player.sendMessage(Component.text("Type the money amount in chat (or 'cancel' to abort):", NamedTextColor.GREEN));
                sessions.put(player.getUniqueId(), new GUISession(codeData, GUIType.MONEY_INPUT));
            }
            case 14 -> openCommandsGUI(player, codeData);
            case 16 -> {
                codeData.save();
                player.closeInventory();
                player.sendMessage(Component.text("Rewards saved!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                sessions.remove(player.getUniqueId());
            }
        }
    }

    private void handleItemsGUIClick(Player player, CodeData codeData, InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot == 53) {
            event.setCancelled(true);
            saveItemsAndGoBack(player, codeData, event.getInventory());
        } else if (slot >= 45) {
            event.setCancelled(true);
        }
    }

    private void saveItemsAndGoBack(Player player, CodeData codeData, Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        codeData.setItemRewards(items);
        openMainGUI(player, codeData);
    }

    private void handleCommandsGUIClick(Player player, CodeData codeData, int slot, boolean rightClick) {
        if (slot == 10) {
            player.closeInventory();
            player.sendMessage(Component.text("Type the console command in chat (or 'cancel' to abort):", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Example: give %player% diamond 5", NamedTextColor.GRAY));
            sessions.put(player.getUniqueId(), new GUISession(codeData, GUIType.COMMAND_INPUT_CONSOLE));
        } else if (slot == 12) {
            player.closeInventory();
            player.sendMessage(Component.text("Type the player command in chat (or 'cancel' to abort):", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Example: spawn", NamedTextColor.GRAY));
            sessions.put(player.getUniqueId(), new GUISession(codeData, GUIType.COMMAND_INPUT_PLAYER));
        } else if (slot >= 18 && slot <= 22 && rightClick) {
            int index = slot - 18;
            List<CodeData.CodeCommand> commands = codeData.getCommands();
            if (index < commands.size()) {
                commands.remove(index);
                codeData.setCommands(commands);
                openCommandsGUI(player, codeData);
            }
        } else if (slot == 26) {
            openMainGUI(player, codeData);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GUISession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (session.type != GUIType.MONEY_INPUT &&
                session.type != GUIType.COMMAND_INPUT_CONSOLE &&
                session.type != GUIType.COMMAND_INPUT_PLAYER) return;

        event.setCancelled(true);
        String input = event.getMessage();

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Cancelled.", NamedTextColor.RED));
            Bukkit.getScheduler().runTask(plugin, () -> openMainGUI(player, session.codeData));
            return;
        }

        if (session.type == GUIType.MONEY_INPUT) {
            try {
                double amount = Double.parseDouble(input);
                session.codeData.setVaultMoney(amount);
                player.sendMessage(Component.text("Set money to: $" + amount, NamedTextColor.GREEN));
                Bukkit.getScheduler().runTask(plugin, () -> openMainGUI(player, session.codeData));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid number! Try again:", NamedTextColor.RED));
            }
        } else if (session.type == GUIType.COMMAND_INPUT_CONSOLE) {
            session.codeData.getCommands().add(new CodeData.CodeCommand(input, false));
            player.sendMessage(Component.text("Added console command: " + input, NamedTextColor.GREEN));
            Bukkit.getScheduler().runTask(plugin, () -> openCommandsGUI(player, session.codeData));
        } else if (session.type == GUIType.COMMAND_INPUT_PLAYER) {
            session.codeData.getCommands().add(new CodeData.CodeCommand(input, true));
            player.sendMessage(Component.text("Added player command: " + input, NamedTextColor.GREEN));
            Bukkit.getScheduler().runTask(plugin, () -> openCommandsGUI(player, session.codeData));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        GUISession session = sessions.get(player.getUniqueId());
        if (session != null && session.type == GUIType.ITEMS) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!event.getView().getTitle().startsWith(MAIN_TITLE_PREFIX)) {
                    saveItemsAndGoBack(player, session.codeData, event.getInventory());
                }
            });
        }
    }

    // ---- Helpers ----

    private static ItemStack buildButton(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ---- Inner types ----

    private static class GUISession {
        final CodeData codeData;
        GUIType type;

        GUISession(CodeData codeData, GUIType type) {
            this.codeData = codeData;
            this.type = type;
        }
    }

    private enum GUIType {
        MAIN, ITEMS, COMMANDS, MONEY_INPUT, COMMAND_INPUT_CONSOLE, COMMAND_INPUT_PLAYER
    }
}
