package com.blockforge.chaoscraft.weapons.ivory.abilities;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityCooldownManager {
   private final ChaosCraftPlugin plugin;
   private IvoryConfig config;
   private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap();

   public AbilityCooldownManager(ChaosCraftPlugin plugin, IvoryConfig config) {
      this.plugin = plugin;
      this.config = config;
   }

   public void updateConfig(IvoryConfig config) {
      this.config = config;
   }

   public void setCooldown(UUID playerId, String abilityId, int seconds) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.computeIfAbsent(playerId, (k) -> {
         return new ConcurrentHashMap();
      });
      long endTime = System.currentTimeMillis() + (long)seconds * 1000L;
      playerCooldowns.put(abilityId, endTime);
   }

   public boolean isOnCooldown(UUID playerId, String abilityId) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns == null) {
         return false;
      } else {
         Long endTime = (Long)playerCooldowns.get(abilityId);
         if (endTime == null) {
            return false;
         } else if (System.currentTimeMillis() >= endTime) {
            playerCooldowns.remove(abilityId);
            return false;
         } else {
            return true;
         }
      }
   }

   public int getRemainingSeconds(UUID playerId, String abilityId) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns == null) {
         return 0;
      } else {
         Long endTime = (Long)playerCooldowns.get(abilityId);
         if (endTime == null) {
            return 0;
         } else {
            long remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0L) {
               playerCooldowns.remove(abilityId);
               return 0;
            } else {
               return (int)Math.ceil((double)remaining / 1000.0D);
            }
         }
      }
   }

   public long getRemainingMillis(UUID playerId, String abilityId) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns == null) {
         return 0L;
      } else {
         Long endTime = (Long)playerCooldowns.get(abilityId);
         if (endTime == null) {
            return 0L;
         } else {
            long remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0L) {
               playerCooldowns.remove(abilityId);
               return 0L;
            } else {
               return remaining;
            }
         }
      }
   }

   public double getCooldownProgress(UUID playerId, String abilityId) {
      IvoryConfig.AbilitySettings settings = this.config.getAbilitySettings(abilityId);
      int totalSeconds = settings.cooldownSeconds;
      if (totalSeconds <= 0) {
         return 1.0D;
      } else {
         int remaining = this.getRemainingSeconds(playerId, abilityId);
         return remaining <= 0 ? 1.0D : 1.0D - (double)remaining / (double)totalSeconds;
      }
   }

   public void clearCooldown(UUID playerId, String abilityId) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns != null) {
         playerCooldowns.remove(abilityId);
      }

   }

   public void clearAllCooldowns(UUID playerId) {
      this.cooldowns.remove(playerId);
   }

   public void clearAll() {
      this.cooldowns.clear();
   }

   public void reduceCooldown(UUID playerId, String abilityId, int seconds) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns != null) {
         Long endTime = (Long)playerCooldowns.get(abilityId);
         if (endTime != null) {
            long newEndTime = endTime - (long)seconds * 1000L;
            if (newEndTime <= System.currentTimeMillis()) {
               playerCooldowns.remove(abilityId);
            } else {
               playerCooldowns.put(abilityId, newEndTime);
            }

         }
      }
   }

   public Map<String, Integer> getAllCooldowns(UUID playerId) {
      Map<String, Integer> result = new ConcurrentHashMap();
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns != null) {
         long now = System.currentTimeMillis();
         Iterator var6 = playerCooldowns.entrySet().iterator();

         while(var6.hasNext()) {
            Entry<String, Long> entry = (Entry)var6.next();
            long remaining = (Long)entry.getValue() - now;
            if (remaining > 0L) {
               result.put((String)entry.getKey(), (int)Math.ceil((double)remaining / 1000.0D));
            }
         }
      }

      return result;
   }

   public boolean hasAnyCooldown(UUID playerId) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns != null && !playerCooldowns.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator var5 = playerCooldowns.values().iterator();

         Long endTime;
         do {
            if (!var5.hasNext()) {
               return false;
            }

            endTime = (Long)var5.next();
         } while(endTime <= now);

         return true;
      } else {
         return false;
      }
   }

   public String getShortestCooldownAbility(UUID playerId) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns != null && !playerCooldowns.isEmpty()) {
         long now = System.currentTimeMillis();
         String shortest = null;
         long shortestRemaining = Long.MAX_VALUE;
         Iterator var8 = playerCooldowns.entrySet().iterator();

         while(var8.hasNext()) {
            Entry<String, Long> entry = (Entry)var8.next();
            long remaining = (Long)entry.getValue() - now;
            if (remaining > 0L && remaining < shortestRemaining) {
               shortestRemaining = remaining;
               shortest = (String)entry.getKey();
            }
         }

         return shortest;
      } else {
         return null;
      }
   }

   public String getLongestCooldownAbility(UUID playerId) {
      Map<String, Long> playerCooldowns = (Map)this.cooldowns.get(playerId);
      if (playerCooldowns != null && !playerCooldowns.isEmpty()) {
         long now = System.currentTimeMillis();
         String longest = null;
         long longestRemaining = 0L;
         Iterator var8 = playerCooldowns.entrySet().iterator();

         while(var8.hasNext()) {
            Entry<String, Long> entry = (Entry)var8.next();
            long remaining = (Long)entry.getValue() - now;
            if (remaining > longestRemaining) {
               longestRemaining = remaining;
               longest = (String)entry.getKey();
            }
         }

         return longest;
      } else {
         return null;
      }
   }
}
