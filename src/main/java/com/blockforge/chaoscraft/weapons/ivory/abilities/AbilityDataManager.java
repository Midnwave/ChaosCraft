package com.blockforge.chaoscraft.weapons.ivory.abilities;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;

public class AbilityDataManager {
   private final ChaosCraftPlugin plugin;
   private final File dataFile;
   private YamlConfiguration data;
   private final Map<UUID, AbilityDataManager.PlayerAbilityData> cache = new ConcurrentHashMap();
   private final Map<UUID, Boolean> dirty = new ConcurrentHashMap();

   public AbilityDataManager(ChaosCraftPlugin plugin) {
      this.plugin = plugin;
      this.dataFile = new File(plugin.getDataFolder(), "ability_selections.yml");
      this.loadData();
   }

   private void loadData() {
      if (!this.dataFile.exists()) {
         try {
            this.dataFile.getParentFile().mkdirs();
            this.dataFile.createNewFile();
         } catch (IOException var2) {
            this.plugin.getLogger().warning("Failed to create ability_selections.yml: " + var2.getMessage());
         }
      }

      this.data = YamlConfiguration.loadConfiguration(this.dataFile);
   }

   public void reload() {
      this.saveAll();
      this.cache.clear();
      this.dirty.clear();
      this.loadData();
      this.plugin.getLogger().info("Ability selections reloaded from disk.");
   }

   public void saveAll() {
      Iterator var1 = this.dirty.keySet().iterator();

      while(var1.hasNext()) {
         UUID playerId = (UUID)var1.next();
         this.saveToYaml(playerId);
      }

      this.dirty.clear();

      try {
         this.data.save(this.dataFile);
      } catch (IOException var3) {
         this.plugin.getLogger().warning("Failed to save ability_selections.yml: " + var3.getMessage());
      }

   }

   public void savePlayer(UUID playerId) {
      if (this.dirty.containsKey(playerId)) {
         this.saveToYaml(playerId);
         this.dirty.remove(playerId);

         try {
            this.data.save(this.dataFile);
         } catch (IOException var3) {
            this.plugin.getLogger().warning("Failed to save ability_selections.yml: " + var3.getMessage());
         }
      }

   }

   private void saveToYaml(UUID playerId) {
      AbilityDataManager.PlayerAbilityData playerData = (AbilityDataManager.PlayerAbilityData)this.cache.get(playerId);
      if (playerData != null) {
         String path = "players." + playerId.toString();
         this.data.set(path + ".left-click", playerData.leftClickAbility);
         this.data.set(path + ".right-click", playerData.rightClickAbility);
      }
   }

   private AbilityDataManager.PlayerAbilityData getPlayerData(UUID playerId) {
      return (AbilityDataManager.PlayerAbilityData)this.cache.computeIfAbsent(playerId, (id) -> {
         AbilityDataManager.PlayerAbilityData playerData = new AbilityDataManager.PlayerAbilityData();
         String path = "players." + id.toString();
         if (this.data.contains(path)) {
            playerData.leftClickAbility = this.data.getString(path + ".left-click");
            playerData.rightClickAbility = this.data.getString(path + ".right-click");
         }

         return playerData;
      });
   }

   public String getLeftClickAbility(UUID playerId) {
      return this.getPlayerData(playerId).leftClickAbility;
   }

   public void setLeftClickAbility(UUID playerId, String abilityId) {
      AbilityDataManager.PlayerAbilityData playerData = this.getPlayerData(playerId);
      playerData.leftClickAbility = abilityId;
      this.dirty.put(playerId, true);
      this.savePlayer(playerId);
   }

   public String getRightClickAbility(UUID playerId) {
      return this.getPlayerData(playerId).rightClickAbility;
   }

   public void setRightClickAbility(UUID playerId, String abilityId) {
      AbilityDataManager.PlayerAbilityData playerData = this.getPlayerData(playerId);
      playerData.rightClickAbility = abilityId;
      this.dirty.put(playerId, true);
      this.savePlayer(playerId);
   }

   public void clearPlayer(UUID playerId) {
      this.cache.remove(playerId);
      this.data.set("players." + playerId.toString(), (Object)null);
      this.dirty.remove(playerId);

      try {
         this.data.save(this.dataFile);
      } catch (IOException var3) {
         this.plugin.getLogger().warning("Failed to save ability_selections.yml: " + var3.getMessage());
      }

   }

   public boolean hasAnyAbility(UUID playerId) {
      AbilityDataManager.PlayerAbilityData playerData = this.getPlayerData(playerId);
      return playerData.leftClickAbility != null && !playerData.leftClickAbility.isEmpty() || playerData.rightClickAbility != null && !playerData.rightClickAbility.isEmpty();
   }

   public Map<UUID, AbilityDataManager.PlayerAbilityData> getAllPlayerData() {
      Map<UUID, AbilityDataManager.PlayerAbilityData> all = new HashMap();
      if (this.data.contains("players")) {
         Iterator var2 = this.data.getConfigurationSection("players").getKeys(false).iterator();

         while(var2.hasNext()) {
            String key = (String)var2.next();

            try {
               UUID playerId = UUID.fromString(key);
               all.put(playerId, this.getPlayerData(playerId));
            } catch (IllegalArgumentException var5) {
            }
         }
      }

      all.putAll(this.cache);
      return all;
   }

   public static class PlayerAbilityData {
      public String leftClickAbility;
      public String rightClickAbility;

      public PlayerAbilityData() {
      }

      public PlayerAbilityData(String leftClick, String rightClick) {
         this.leftClickAbility = leftClick;
         this.rightClickAbility = rightClick;
      }
   }
}
