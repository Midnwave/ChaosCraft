package com.blockforge.chaoscraft.weapons.ivory.abilities;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryService;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbilityPlaceholders extends PlaceholderExpansion {
   private final ChaosCraftPlugin plugin;
   private final IvoryService ivoryService;

   public AbilityPlaceholders(ChaosCraftPlugin plugin, IvoryService ivoryService) {
      this.plugin = plugin;
      this.ivoryService = ivoryService;
   }

   @NotNull
   public String getIdentifier() {
      return "chaoscraft_ability";
   }

   @NotNull
   public String getAuthor() {
      return "BlockForge Studios";
   }

   @NotNull
   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   @Nullable
   public String onRequest(OfflinePlayer player, @NotNull String params) {
      if (player != null && player.isOnline()) {
         AbilityManager manager = this.ivoryService.getAbilityManager();
         if (manager == null) {
            return "";
         } else {
            String[] parts = params.split("_", 2);
            String suffix;
            Ability ability;
            if (params.equalsIgnoreCase("leftclick")) {
               suffix = manager.getDataManager().getLeftClickAbility(player.getUniqueId());
               if (suffix != null && !suffix.isEmpty()) {
                  ability = manager.getRegistry().getAbility(suffix);
                  return ability != null ? ability.getDisplayName() : suffix;
               } else {
                  return "None";
               }
            } else if (params.equalsIgnoreCase("rightclick")) {
               suffix = manager.getDataManager().getRightClickAbility(player.getUniqueId());
               if (suffix != null && !suffix.isEmpty()) {
                  ability = manager.getRegistry().getAbility(suffix);
                  return ability != null ? ability.getDisplayName() : suffix;
               } else {
                  return "None";
               }
            } else {
               suffix = "";
               String abilityId;
               if (params.endsWith("_cooldown_formatted")) {
                  abilityId = params.substring(0, params.length() - "_cooldown_formatted".length());
                  suffix = "cooldown_formatted";
               } else if (params.endsWith("_cooldown")) {
                  abilityId = params.substring(0, params.length() - "_cooldown".length());
                  suffix = "cooldown";
               } else {
                  if (!params.endsWith("_ready")) {
                     return "";
                  }

                  abilityId = params.substring(0, params.length() - "_ready".length());
                  suffix = "ready";
               }

               AbilityCooldownManager cooldownManager = manager.getCooldownManager();
               byte var9 = -1;
               switch(suffix.hashCode()) {
               case -546109589:
                  if (suffix.equals("cooldown")) {
                     var9 = 0;
                  }
                  break;
               case -77027096:
                  if (suffix.equals("cooldown_formatted")) {
                     var9 = 1;
                  }
                  break;
               case 108386723:
                  if (suffix.equals("ready")) {
                     var9 = 2;
                  }
               }

               int remaining;
               switch(var9) {
               case 0:
                  remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), abilityId);
                  return String.valueOf(remaining);
               case 1:
                  remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), abilityId);
                  return this.formatTime(remaining);
               case 2:
                  boolean ready = !cooldownManager.isOnCooldown(player.getUniqueId(), abilityId);
                  return ready ? "true" : "false";
               default:
                  return "";
               }
            }
         }
      } else {
         return "";
      }
   }

   private String formatTime(int seconds) {
      if (seconds <= 0) {
         return "0:00";
      } else {
         int minutes = seconds / 60;
         int secs = seconds % 60;
         return String.format("%d:%02d", minutes, secs);
      }
   }
}
