package com.blockforge.chaoscraft.weapons.ivory.abilities;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.itemtags.ItemTagsAPI;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryService;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbilityCommand implements CommandExecutor, TabCompleter {
   private final ChaosCraftPlugin plugin;
   private final IvoryService ivoryService;

   public AbilityCommand(ChaosCraftPlugin plugin, IvoryService ivoryService) {
      this.plugin = plugin;
      this.ivoryService = ivoryService;
   }

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (sender instanceof Player) {
         Player player = (Player)sender;
         if (!player.hasPermission("chaoscraft.ivory.abilities")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command.");
            return true;
         } else if (this.ivoryService != null && this.ivoryService.getAbilityManager() != null) {
            IvoryConfig config = this.ivoryService.getConfig();
            if (config != null && config.isEnabled()) {
               ItemStack held = player.getInventory().getItemInMainHand();
               if (held != null && held.getType() != Material.AIR) {
                  boolean isActive = ItemTagsAPI.hasTag(held, config.getTagActive());
                  boolean isRage = ItemTagsAPI.hasTag(held, config.getTagRage());
                  if (!isActive && !isRage) {
                     boolean isOff = ItemTagsAPI.hasTag(held, config.getTagOff());
                     boolean isCharging = ItemTagsAPI.hasTag(held, config.getTagCharging());
                     if (isOff) {
                        player.sendMessage(String.valueOf(ChatColor.RED) + "Your Celestial Ivory is dormant. Activate it first with right-click.");
                     } else if (isCharging) {
                        player.sendMessage(String.valueOf(ChatColor.RED) + "Your Celestial Ivory is still charging...");
                     } else {
                        player.sendMessage(String.valueOf(ChatColor.RED) + "You must be holding a Celestial Ivory weapon to open the abilities menu.");
                     }

                     return true;
                  } else if (isRage) {
                     player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Cannot change abilities while in Rage mode!");
                     return true;
                  } else {
                     this.ivoryService.getAbilityManager().openAbilityGUI(player);
                     return true;
                  }
               } else {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "You must be holding a Celestial Ivory weapon to open the abilities menu.");
                  return true;
               }
            } else {
               player.sendMessage(String.valueOf(ChatColor.RED) + "Celestial Ivory is currently disabled.");
               return true;
            }
         } else {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Celestial Ivory system is not available.");
            return true;
         }
      } else {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "This command can only be used by players.");
         return true;
      }
   }

   @Nullable
   public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
      return List.of();
   }
}
