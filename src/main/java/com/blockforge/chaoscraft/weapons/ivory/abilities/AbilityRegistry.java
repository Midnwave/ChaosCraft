package com.blockforge.chaoscraft.weapons.ivory.abilities;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryEffectsManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.ApocalypseDivineJudgmentAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.AstralPhoenixRebirthAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.AstralStepDanceAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.AuroraBorealisAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CascadeDetonationAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CelestialArcBladeAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CelestialArmadaAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CelestialMeteorShowerAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CelestialSatelliteNetworkAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CelestialStormFrontAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CometBarrageAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CometChargeAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.ConstellationManifestationAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.CosmicObliterationCannonAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.DivineCascadeAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.DivineGuillotineRainAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.DivineOmegaCannonAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.GravityWellLuminaAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.HeavensDownpourAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.HeavensWrathFullReleaseAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.IvorySentinelLegionAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.PrismaticRefractionAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.RadiantSpearVolleyAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.RagnarokOfLightAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.RagnarokTwilightOfGodsAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.SacredGeometryMetatronsCubeAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.SanctumOfFallenStarsAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.SolarFlareEruptionAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.StarfallConvergenceAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.StellarGuillotineAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.StormCallersWrathAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.SupernovaGenesisAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.SupernovaImplosionAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.TempestOfHeavensAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.TitansFistOfJudgmentAbility;
import com.blockforge.chaoscraft.weapons.ivory.abilities.impl.ZeroPointConvergenceAbility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbilityRegistry {
   private final ChaosCraftPlugin plugin;
   private IvoryConfig config;
   private final IvoryEffectsManager effectsManager;
   private final Map<String, Ability> abilities = new LinkedHashMap();

   public AbilityRegistry(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      this.plugin = plugin;
      this.config = config;
      this.effectsManager = effectsManager;
   }

   public void updateConfig(IvoryConfig config) {
      this.config = config;
   }

   public void registerAllAbilities() {
      this.abilities.clear();
      this.register(new DivineCascadeAbility(this.plugin, this.config, this.effectsManager));
      this.register(new StellarGuillotineAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CometBarrageAbility(this.plugin, this.config, this.effectsManager));
      this.register(new HeavensDownpourAbility(this.plugin, this.config, this.effectsManager));
      this.register(new RadiantSpearVolleyAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CelestialSatelliteNetworkAbility(this.plugin, this.config, this.effectsManager));
      this.register(new IvorySentinelLegionAbility(this.plugin, this.config, this.effectsManager));
      this.register(new AstralPhoenixRebirthAbility(this.plugin, this.config, this.effectsManager));
      this.register(new GravityWellLuminaAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CelestialArmadaAbility(this.plugin, this.config, this.effectsManager));
      this.register(new ApocalypseDivineJudgmentAbility(this.plugin, this.config, this.effectsManager));
      this.register(new SupernovaGenesisAbility(this.plugin, this.config, this.effectsManager));
      this.register(new RagnarokOfLightAbility(this.plugin, this.config, this.effectsManager));
      this.register(new HeavensWrathFullReleaseAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CosmicObliterationCannonAbility(this.plugin, this.config, this.effectsManager));
      this.register(new AstralStepDanceAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CometChargeAbility(this.plugin, this.config, this.effectsManager));
      this.register(new SanctumOfFallenStarsAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CelestialMeteorShowerAbility(this.plugin, this.config, this.effectsManager));
      this.register(new StarfallConvergenceAbility(this.plugin, this.config, this.effectsManager));
      this.register(new AuroraBorealisAbility(this.plugin, this.config, this.effectsManager));
      this.register(new PrismaticRefractionAbility(this.plugin, this.config, this.effectsManager));
      this.register(new DivineGuillotineRainAbility(this.plugin, this.config, this.effectsManager));
      this.register(new TempestOfHeavensAbility(this.plugin, this.config, this.effectsManager));
      this.register(new StormCallersWrathAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CelestialStormFrontAbility(this.plugin, this.config, this.effectsManager));
      this.register(new SupernovaImplosionAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CascadeDetonationAbility(this.plugin, this.config, this.effectsManager));
      this.register(new DivineOmegaCannonAbility(this.plugin, this.config, this.effectsManager));
      this.register(new ConstellationManifestationAbility(this.plugin, this.config, this.effectsManager));
      this.register(new CelestialArcBladeAbility(this.plugin, this.config, this.effectsManager));
      this.register(new SolarFlareEruptionAbility(this.plugin, this.config, this.effectsManager));
      this.register(new TitansFistOfJudgmentAbility(this.plugin, this.config, this.effectsManager));
      this.register(new SacredGeometryMetatronsCubeAbility(this.plugin, this.config, this.effectsManager));
      this.register(new ZeroPointConvergenceAbility(this.plugin, this.config, this.effectsManager));
      this.register(new RagnarokTwilightOfGodsAbility(this.plugin, this.config, this.effectsManager));
      this.plugin.getLogger().info("Registered " + this.abilities.size() + " Celestial Ivory abilities.");
   }

   public void register(Ability ability) {
      this.abilities.put(ability.getId(), ability);
   }

   public void unregister(String id) {
      this.abilities.remove(id);
   }

   public Ability getAbility(String id) {
      return (Ability)this.abilities.get(id);
   }

   public boolean hasAbility(String id) {
      return this.abilities.containsKey(id);
   }

   public Set<String> getAllAbilityIds() {
      return new LinkedHashSet(this.abilities.keySet());
   }

   public Collection<Ability> getAllAbilities() {
      return new ArrayList(this.abilities.values());
   }

   public List<Ability> getAbilitiesInOrder() {
      List<Ability> ordered = new ArrayList();
      Iterator var2 = this.config.getAbilityOrder().iterator();

      while(var2.hasNext()) {
         String id = (String)var2.next();
         Ability ability = (Ability)this.abilities.get(id);
         if (ability != null) {
            ordered.add(ability);
         }
      }

      return ordered;
   }

   public List<Ability> getEnabledAbilitiesInOrder() {
      List<Ability> ordered = new ArrayList();
      Iterator var2 = this.config.getAbilityOrder().iterator();

      while(var2.hasNext()) {
         String id = (String)var2.next();
         Ability ability = (Ability)this.abilities.get(id);
         if (ability != null) {
            IvoryConfig.AbilitySettings settings = this.config.getAbilitySettings(id);
            if (settings.enabled) {
               ordered.add(ability);
            }
         }
      }

      return ordered;
   }

   public void reloadConfigs() {
   }

   public int size() {
      return this.abilities.size();
   }

   public Ability getAbilityByIndex(int index) {
      List<String> order = this.config.getAbilityOrder();
      return index >= 0 && index < order.size() ? (Ability)this.abilities.get(order.get(index)) : null;
   }

   public int getAbilityIndex(String id) {
      return this.config.getAbilityOrder().indexOf(id);
   }
}
