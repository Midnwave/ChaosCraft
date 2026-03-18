package com.blockforge.chaoscraft.weapons.ivory.abilities;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.itemtags.ItemTagsAPI;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryEffectsManager;
import com.blockforge.chaoscraft.weapons.ivory.IvoryStateManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

public class AbilityManager implements Listener {
   private final ChaosCraftPlugin plugin;
   private IvoryConfig config;
   private final IvoryStateManager stateManager;
   private final IvoryEffectsManager effectsManager;
   private final AbilityRegistry registry;
   private final AbilityCooldownManager cooldownManager;
   private final AbilityDataManager dataManager;
   private AbilityGUI gui;
   private final Map<UUID, Boolean> crouchingPlayers = new HashMap();
   private final Map<UUID, Long> lastScrollTime = new HashMap();

   public AbilityManager(ChaosCraftPlugin plugin, IvoryConfig config, IvoryStateManager stateManager, IvoryEffectsManager effectsManager) {
      this.plugin = plugin;
      this.config = config;
      this.stateManager = stateManager;
      this.effectsManager = effectsManager;
      this.registry = new AbilityRegistry(plugin, config, effectsManager);
      this.cooldownManager = new AbilityCooldownManager(plugin, config);
      this.dataManager = new AbilityDataManager(plugin);
      this.registry.registerAllAbilities();
      this.gui = new AbilityGUI(plugin, config, this, this.registry);
      plugin.getLogger().info("Ability Manager initialized with " + this.registry.getAllAbilityIds().size() + " abilities.");
   }

   public void reload() {
      this.registry.reloadConfigs();
      this.cooldownManager.updateConfig(this.config);
      this.dataManager.reload();
      this.plugin.getLogger().info("Ability system reloaded.");
   }

   public void updateConfig(IvoryConfig config) {
      this.config = config;
      this.registry.updateConfig(config);
      this.cooldownManager.updateConfig(config);
      this.gui = new AbilityGUI(this.plugin, config, this, this.registry);
   }

   public void shutdown() {
      this.cooldownManager.clearAll();
      this.crouchingPlayers.clear();
      this.lastScrollTime.clear();
      this.dataManager.saveAll();
   }

   private boolean isHoldingActiveIvory(Player player) {
      ItemStack item = player.getInventory().getItemInMainHand();
      if (item != null && item.getType() != Material.AIR) {
         String activeTag = this.config.getTagActive();
         boolean hasTag = ItemTagsAPI.hasTag(item, activeTag);
         if (!hasTag) {
            Set<String> tags = ItemTagsAPI.getTags(item);
            Logger var10000 = this.plugin.getLogger();
            String var10001 = String.valueOf(tags);
            var10000.fine("Ability check failed - Item tags: " + var10001 + ", Expected: " + activeTag);
         }

         return hasTag;
      } else {
         return false;
      }
   }

   public boolean executeAbility(Player player, String abilityId) {
      if (!this.isHoldingActiveIvory(player)) {
         player.sendActionBar(Component.text("Abilities can only be used while ivory is ACTIVE!", NamedTextColor.RED));
         return false;
      } else {
         Ability ability = this.registry.getAbility(abilityId);
         if (ability == null) {
            player.sendMessage(Component.text("Unknown ability: " + abilityId, NamedTextColor.RED));
            return false;
         } else {
            IvoryConfig.AbilitySettings settings = this.config.getAbilitySettings(abilityId);
            if (!settings.enabled) {
               player.sendMessage(Component.text("This ability is disabled.", NamedTextColor.RED));
               return false;
            } else if (this.cooldownManager.isOnCooldown(player.getUniqueId(), abilityId)) {
               if (this.config.isShowCooldownWarning()) {
                  int remaining = this.cooldownManager.getRemainingSeconds(player.getUniqueId(), abilityId);
                  player.sendActionBar(Component.text("Ability on cooldown: " + remaining + "s", NamedTextColor.RED));
               }

               return false;
            } else {
               ability.execute(player);
               this.cooldownManager.setCooldown(player.getUniqueId(), abilityId, settings.cooldownSeconds);
               return true;
            }
         }
      }
   }

   public void executeLeftClickAbility(Player player) {
      String abilityId = this.dataManager.getLeftClickAbility(player.getUniqueId());
      if (abilityId != null && !abilityId.isEmpty()) {
         this.executeAbility(player, abilityId);
      }

   }

   public void executeRightClickAbility(Player player) {
      String abilityId = this.dataManager.getRightClickAbility(player.getUniqueId());
      if (abilityId != null && !abilityId.isEmpty()) {
         this.executeAbility(player, abilityId);
      }

   }

   public void useSelectedAbility(Player player) {
      String abilityId = this.dataManager.getLeftClickAbility(player.getUniqueId());
      if (abilityId != null && !abilityId.isEmpty()) {
         this.executeAbility(player, abilityId);
      } else {
         player.sendActionBar(Component.text("No ability selected! Use /abilities to choose one.", NamedTextColor.RED));
      }

   }

   public void triggerStellarRage(Player player) {
      if (this.stateManager.isInRage(player)) {
         player.sendActionBar(Component.text("Already in Rage mode!", NamedTextColor.YELLOW));
      } else if (this.cooldownManager.isOnCooldown(player.getUniqueId(), "stellar_rage")) {
         if (this.config.isShowCooldownWarning()) {
            int remaining = this.cooldownManager.getRemainingSeconds(player.getUniqueId(), "stellar_rage");
            player.sendActionBar(Component.text("Stellar Rage on cooldown: " + remaining + "s", NamedTextColor.RED));
         }

      } else {
         ItemStack held = player.getInventory().getItemInMainHand();
         if (held != null && ItemTagsAPI.hasTag(held, this.config.getTagActive())) {
            this.stateManager.activateRage(player, held);
            this.cooldownManager.setCooldown(player.getUniqueId(), "stellar_rage", this.config.getStellarRageCooldown());
         }
      }
   }

   public void setLeftClickAbility(Player player, String abilityId) {
      this.dataManager.setLeftClickAbility(player.getUniqueId(), abilityId);
      Ability ability = this.registry.getAbility(abilityId);
      String name = ability != null ? ability.getDisplayName() : abilityId;
      player.sendMessage(Component.text("Left-click ability set to: ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.GOLD)));
   }

   public void setRightClickAbility(Player player, String abilityId) {
      this.dataManager.setRightClickAbility(player.getUniqueId(), abilityId);
      Ability ability = this.registry.getAbility(abilityId);
      String name = ability != null ? ability.getDisplayName() : abilityId;
      player.sendMessage(Component.text("Right-click ability set to: ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.GOLD)));
   }

   public void cycleNextAbility(Player player) {
      String current = this.dataManager.getLeftClickAbility(player.getUniqueId());
      String next = this.getNextAbilityId(current);
      if (next != null) {
         this.dataManager.setLeftClickAbility(player.getUniqueId(), next);
         Ability ability = this.registry.getAbility(next);
         String name = ability != null ? ability.getDisplayName() : next;
         player.sendActionBar(Component.text("Ability: ", NamedTextColor.GRAY).append(Component.text(name, NamedTextColor.GOLD)));
      }

   }

   public void cyclePreviousAbility(Player player) {
      String current = this.dataManager.getLeftClickAbility(player.getUniqueId());
      String prev = this.getPreviousAbilityId(current);
      if (prev != null) {
         this.dataManager.setLeftClickAbility(player.getUniqueId(), prev);
         Ability ability = this.registry.getAbility(prev);
         String name = ability != null ? ability.getDisplayName() : prev;
         player.sendActionBar(Component.text("Ability: ", NamedTextColor.GRAY).append(Component.text(name, NamedTextColor.GOLD)));
      }

   }

   private String getNextAbilityId(String current) {
      List<String> order = this.config.getAbilityOrder();
      if (order.isEmpty()) {
         return null;
      } else if (current != null && !current.isEmpty()) {
         int index = order.indexOf(current);
         if (index < 0) {
            return (String)order.get(0);
         } else {
            int nextIndex = (index + 1) % order.size();
            return (String)order.get(nextIndex);
         }
      } else {
         return (String)order.get(0);
      }
   }

   private String getPreviousAbilityId(String current) {
      List<String> order = this.config.getAbilityOrder();
      if (order.isEmpty()) {
         return null;
      } else if (current != null && !current.isEmpty()) {
         int index = order.indexOf(current);
         if (index < 0) {
            return (String)order.get(order.size() - 1);
         } else {
            int prevIndex = (index - 1 + order.size()) % order.size();
            return (String)order.get(prevIndex);
         }
      } else {
         return (String)order.get(order.size() - 1);
      }
   }

   public void openAbilityGUI(Player player) {
      this.gui.open(player);
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onToggleSneak(PlayerToggleSneakEvent event) {
      Player player = event.getPlayer();
      this.crouchingPlayers.put(player.getUniqueId(), event.isSneaking());
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onHeldItemChange(PlayerItemHeldEvent event) {
      if (this.config.getAbilityCycleMethod().equalsIgnoreCase("crouch_scroll")) {
         Player player = event.getPlayer();
         UUID playerId = player.getUniqueId();
         if (Boolean.TRUE.equals(this.crouchingPlayers.get(playerId))) {
            ItemStack item = player.getInventory().getItem(event.getPreviousSlot());
            if (item != null && item.getType() != Material.AIR) {
               if (ItemTagsAPI.hasAny(item, this.config.getTagActive(), this.config.getTagRage())) {
                  event.setCancelled(true);
                  long now = System.currentTimeMillis();
                  Long lastScroll = (Long)this.lastScrollTime.get(playerId);
                  if (lastScroll == null || now - lastScroll >= 100L) {
                     this.lastScrollTime.put(playerId, now);
                     int prev = event.getPreviousSlot();
                     int next = event.getNewSlot();
                     int diff = next - prev;
                     if (diff > 4) {
                        diff -= 9;
                     }

                     if (diff < -4) {
                        diff += 9;
                     }

                     if (diff > 0) {
                        this.cycleNextAbility(player);
                     } else if (diff < 0) {
                        this.cyclePreviousAbility(player);
                     }

                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onShiftClick(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      if (player.isSneaking()) {
         ItemStack item = player.getInventory().getItemInMainHand();
         if (item != null && item.getType() != Material.AIR) {
            if (ItemTagsAPI.hasAny(item, this.config.getTagActive(), this.config.getTagRage())) {
               if (!ItemTagsAPI.hasTag(item, this.config.getTagRage())) {
                  Action action = event.getAction();
                  if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
                     if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                        event.setCancelled(true);
                        this.executeRightClickAbility(player);
                     }
                  } else {
                     event.setCancelled(true);
                     this.executeLeftClickAbility(player);
                  }

               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerQuit(PlayerQuitEvent event) {
      UUID playerId = event.getPlayer().getUniqueId();
      this.crouchingPlayers.remove(playerId);
      this.lastScrollTime.remove(playerId);
      this.dataManager.savePlayer(playerId);
   }

   public AbilityRegistry getRegistry() {
      return this.registry;
   }

   public AbilityCooldownManager getCooldownManager() {
      return this.cooldownManager;
   }

   public AbilityDataManager getDataManager() {
      return this.dataManager;
   }

   public AbilityGUI getGUI() {
      return this.gui;
   }

   public IvoryConfig getConfig() {
      return this.config;
   }
}
