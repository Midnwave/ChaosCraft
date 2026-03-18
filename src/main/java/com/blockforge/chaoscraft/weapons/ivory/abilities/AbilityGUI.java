package com.blockforge.chaoscraft.weapons.ivory.abilities;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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

public class AbilityGUI implements Listener {
   private final ChaosCraftPlugin plugin;
   private final IvoryConfig config;
   private final AbilityManager abilityManager;
   private final AbilityRegistry registry;
   private final Map<UUID, AbilityGUI.GUISession> openSessions = new HashMap();
   private static final int ROWS = 6;
   private static final int SIZE = 54;
   private static final String GUI_TITLE = "§6§lCelestial Ivory Abilities";
   private static final int ABILITIES_PER_PAGE = 36;
   private static final int ABILITY_START_SLOT = 9;

   public AbilityGUI(ChaosCraftPlugin plugin, IvoryConfig config, AbilityManager abilityManager, AbilityRegistry registry) {
      this.plugin = plugin;
      this.config = config;
      this.abilityManager = abilityManager;
      this.registry = registry;
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   public void open(Player player) {
      this.open(player, 0);
   }

   public void open(Player player, int page) {
      List<Ability> abilities = this.registry.getEnabledAbilitiesInOrder();
      int maxPage = Math.max(0, (abilities.size() - 1) / 36);
      page = Math.max(0, Math.min(page, maxPage));
      AbilityGUI.GUISession session = new AbilityGUI.GUISession(player.getUniqueId(), page, maxPage);
      this.openSessions.put(player.getUniqueId(), session);
      Inventory inv = Bukkit.createInventory(new AbilityGUI.AbilityGUIHolder(player.getUniqueId()), 54, Component.text("§6§lCelestial Ivory Abilities"));
      ItemStack background = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");

      int startIndex;
      for(startIndex = 0; startIndex < 9; ++startIndex) {
         inv.setItem(startIndex, background);
      }

      for(startIndex = 45; startIndex < 54; ++startIndex) {
         inv.setItem(startIndex, background);
      }

      inv.setItem(4, this.createInfoItem(player));
      startIndex = page * 36;
      int endIndex = Math.min(startIndex + 36, abilities.size());
      String currentLeftClick = this.abilityManager.getDataManager().getLeftClickAbility(player.getUniqueId());
      String currentRightClick = this.abilityManager.getDataManager().getRightClickAbility(player.getUniqueId());
      int slot = 9;

      for(int i = startIndex; i < endIndex; ++i) {
         Ability ability = (Ability)abilities.get(i);
         boolean isLeftClick = ability.getId().equals(currentLeftClick);
         boolean isRightClick = ability.getId().equals(currentRightClick);
         ItemStack item = this.createAbilityItem(ability, isLeftClick, isRightClick);
         inv.setItem(slot, item);
         session.slotToAbility.put(slot, ability.getId());
         ++slot;
         if ((slot + 1) % 9 == 0) {
            slot += 2;
         }
      }

      if (page > 0) {
         inv.setItem(45, this.createNavigationItem(Material.ARROW, "§ePrevious Page", page));
      }

      if (page < maxPage) {
         inv.setItem(53, this.createNavigationItem(Material.ARROW, "§eNext Page", page + 2));
      }

      inv.setItem(49, this.createPageIndicator(page + 1, maxPage + 1));
      player.openInventory(inv);
   }

   private ItemStack createInfoItem(Player player) {
      ItemStack item = new ItemStack(Material.NETHER_STAR);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(((TextComponent)Component.text("Ability Selection", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)).decoration(TextDecoration.BOLD, true));
      List<Component> lore = new ArrayList();
      lore.add(Component.empty());
      String leftClick = this.abilityManager.getDataManager().getLeftClickAbility(player.getUniqueId());
      String rightClick = this.abilityManager.getDataManager().getRightClickAbility(player.getUniqueId());
      Ability leftAbility = leftClick != null ? this.registry.getAbility(leftClick) : null;
      Ability rightAbility = rightClick != null ? this.registry.getAbility(rightClick) : null;
      lore.add(((TextComponent)Component.text("Shift + Left Click: ", NamedTextColor.YELLOW).append(Component.text(leftAbility != null ? leftAbility.getDisplayName() : "None", NamedTextColor.WHITE))).decoration(TextDecoration.ITALIC, false));
      lore.add(((TextComponent)Component.text("Shift + Right Click: ", NamedTextColor.YELLOW).append(Component.text(rightAbility != null ? rightAbility.getDisplayName() : "None", NamedTextColor.WHITE))).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.empty());
      lore.add(Component.text("Left-click an ability to set", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.text("as your Shift+Left Click.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.empty());
      lore.add(Component.text("Right-click an ability to set", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.text("as your Shift+Right Click.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      meta.lore(lore);
      meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createAbilityItem(Ability ability, boolean isLeftClick, boolean isRightClick) {
      Material material = Material.BLAZE_POWDER;
      if (isLeftClick && isRightClick) {
         material = Material.NETHER_STAR;
      } else if (isLeftClick) {
         material = Material.DIAMOND;
      } else if (isRightClick) {
         material = Material.EMERALD;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      Component name = ((TextComponent)Component.text(ability.getDisplayName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)).decoration(TextDecoration.BOLD, true);
      if (isLeftClick) {
         name = name.append(Component.text(" [L]", NamedTextColor.AQUA));
      }

      if (isRightClick) {
         name = name.append(Component.text(" [R]", NamedTextColor.GREEN));
      }

      meta.displayName(name);
      List<Component> lore = new ArrayList();
      lore.add(Component.empty());
      String description = ability.getDescription();
      List<String> descLines = this.wrapText(description, 25);
      Iterator var11 = descLines.iterator();

      while(var11.hasNext()) {
         String line = (String)var11.next();
         lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      }

      lore.add(Component.empty());
      IvoryConfig.AbilitySettings settings = this.config.getAbilitySettings(ability.getId());
      lore.add(((TextComponent)Component.text("Cooldown: ", NamedTextColor.YELLOW).append(Component.text(settings.cooldownSeconds + "s", NamedTextColor.WHITE))).decoration(TextDecoration.ITALIC, false));
      lore.add(Component.empty());
      if (!isLeftClick) {
         lore.add(Component.text("Left-click to set as L-Click", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
      }

      if (!isRightClick) {
         lore.add(Component.text("Right-click to set as R-Click", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
      }

      meta.lore(lore);
      meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
      item.setItemMeta(meta);
      return item;
   }

   private List<String> wrapText(String text, int maxCharsPerLine) {
      List<String> lines = new ArrayList();
      String[] words = text.split(" ");
      StringBuilder currentLine = new StringBuilder();
      String[] var6 = words;
      int var7 = words.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         String word = var6[var8];
         if (currentLine.length() + word.length() + 1 > maxCharsPerLine && currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
            currentLine = new StringBuilder();
         }

         currentLine.append(word).append(" ");
      }

      if (currentLine.length() > 0) {
         lines.add(currentLine.toString().trim());
      }

      return lines;
   }

   private ItemStack createNavigationItem(Material material, String name, int targetPage) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createPageIndicator(int currentPage, int totalPages) {
      ItemStack item = new ItemStack(Material.PAPER);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text("Page " + currentPage + "/" + totalPages, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createItem(Material material, String name) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
      item.setItemMeta(meta);
      return item;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onInventoryClick(InventoryClickEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player) {
         Player player = (Player)var3;
         Inventory topInv = event.getView().getTopInventory();
         InventoryHolder var5 = topInv.getHolder();
         if (var5 instanceof AbilityGUI.AbilityGUIHolder) {
            AbilityGUI.AbilityGUIHolder holder = (AbilityGUI.AbilityGUIHolder)var5;
            event.setCancelled(true);
            AbilityGUI.GUISession session = (AbilityGUI.GUISession)this.openSessions.get(player.getUniqueId());
            if (session != null) {
               if (event.getClickedInventory() == topInv) {
                  int slot = event.getSlot();
                  if (slot == 45 && session.page > 0) {
                     Bukkit.getScheduler().runTask(this.plugin, () -> {
                        this.open(player, session.page - 1);
                     });
                  } else if (slot == 53 && session.page < session.maxPage) {
                     Bukkit.getScheduler().runTask(this.plugin, () -> {
                        this.open(player, session.page + 1);
                     });
                  } else {
                     String abilityId = (String)session.slotToAbility.get(slot);
                     if (abilityId != null) {
                        if (event.isLeftClick()) {
                           this.abilityManager.setLeftClickAbility(player, abilityId);
                        } else if (event.isRightClick()) {
                           this.abilityManager.setRightClickAbility(player, abilityId);
                        }

                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                           player.closeInventory();
                        });
                     }

                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onInventoryDrag(InventoryDragEvent event) {
      HumanEntity var3 = event.getWhoClicked();
      if (var3 instanceof Player) {
         Player player = (Player)var3;
         Inventory var4 = event.getView().getTopInventory();
         if (var4.getHolder() instanceof AbilityGUI.AbilityGUIHolder) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onInventoryClose(InventoryCloseEvent event) {
      HumanEntity var3 = event.getPlayer();
      if (var3 instanceof Player) {
         Player player = (Player)var3;
         this.openSessions.remove(player.getUniqueId());
      }

   }

   private static class GUISession {
      final UUID playerId;
      final int page;
      final int maxPage;
      final Map<Integer, String> slotToAbility = new HashMap();

      GUISession(UUID playerId, int page, int maxPage) {
         this.playerId = playerId;
         this.page = page;
         this.maxPage = maxPage;
      }
   }

   private static class AbilityGUIHolder implements InventoryHolder {
      private final UUID playerId;

      AbilityGUIHolder(UUID playerId) {
         this.playerId = playerId;
      }

      public UUID getPlayerId() {
         return this.playerId;
      }

      public Inventory getInventory() {
         return null;
      }
   }
}
