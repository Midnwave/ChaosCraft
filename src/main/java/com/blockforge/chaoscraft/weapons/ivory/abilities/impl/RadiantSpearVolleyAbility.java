package com.blockforge.chaoscraft.weapons.ivory.abilities.impl;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryEffectsManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZone;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZoneManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.ParticleUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RadiantSpearVolleyAbility extends Ability {
   private final Random random = new Random();

   public RadiantSpearVolleyAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "radiant_spear_volley";
   }

   public String getDisplayName() {
      return "Radiant Spear Volley";
   }

   public String getDescription() {
      return "Conjure a volley of radiant spears behind you that launch forward in a devastating barrage, piercing through all enemies.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      int spearCount = settings.getInt("spear-count", 15);
      double travelDistance = settings.getDouble("travel-distance", 60.0D);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 1.2F);
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.5F, 0.8F);
      List<RadiantSpearVolleyAbility.SpearData> spears = this.generateSpearFormation(origin, spearCount);
      this.createSpearMaterialization(spears, () -> {
         this.launchSpearVolley(player, spears, travelDistance, settings);
      });
   }

   private List<RadiantSpearVolleyAbility.SpearData> generateSpearFormation(Location origin, int count) {
      List<RadiantSpearVolleyAbility.SpearData> spears = new ArrayList();
      int rows = 3;
      int spearsPerRow = count / rows;

      for(int row = 0; row < rows; ++row) {
         double zOffset = (double)(3 + row * 2);
         double yOffset = 1.0D + (double)row * 1.5D;
         double rowWidth = (double)(6 + row * 2);

         for(int i = 0; i < spearsPerRow; ++i) {
            double xOffset = ((double)i - (double)spearsPerRow / 2.0D) * (rowWidth / (double)spearsPerRow);
            xOffset += (this.random.nextDouble() - 0.5D) * 0.5D;
            Location spearLoc = origin.clone().add(xOffset, yOffset, zOffset);
            spears.add(new RadiantSpearVolleyAbility.SpearData(spearLoc, row));
         }
      }

      return spears;
   }

   private void createSpearMaterialization(final List<RadiantSpearVolleyAbility.SpearData> spears, final Runnable onComplete) {
      (new BukkitRunnable() {
         int tick = 0;
         final int formationTime = 30;
         final List<BlockDisplay> displays = new ArrayList();

         public void run() {
            if (this.tick >= 30) {
               onComplete.run();

               for(int i = 0; i < Math.min(this.displays.size(), spears.size()); ++i) {
                  ((RadiantSpearVolleyAbility.SpearData)spears.get(i)).display = (BlockDisplay)this.displays.get(i);
               }

               this.cancel();
            } else {
               double progress = (double)this.tick / 30.0D;

               for(int ix = 0; ix < spears.size(); ++ix) {
                  RadiantSpearVolleyAbility.SpearData spear = (RadiantSpearVolleyAbility.SpearData)spears.get(ix);
                  Location loc = spear.location;
                  World world = loc.getWorld();
                  if (world != null) {
                     double spearProgress = Math.max(0.0D, progress * 1.5D - (double)spear.row * 0.15D);
                     if (!(spearProgress <= 0.0D)) {
                        spearProgress = Math.min(1.0D, spearProgress);
                        if (spearProgress > 0.1D && (ix >= this.displays.size() || this.displays.get(ix) == null)) {
                           while(this.displays.size() <= ix) {
                              this.displays.add((BlockDisplay)null);
                           }

                           BlockDisplay display = (BlockDisplay)world.spawn(loc, BlockDisplay.class, (d) -> {
                              d.setBlock(Material.SEA_LANTERN.createBlockData());
                              d.setGlowing(true);
                              d.setBrightness(new Brightness(15, 15));
                              Transformation t = d.getTransformation();
                              Quaternionf rotation = (new Quaternionf()).rotateX((float)Math.toRadians(90.0D));
                              d.setTransformation(new Transformation(new Vector3f(-0.1F, -0.1F, -0.75F), rotation, new Vector3f(0.2F, 0.2F, 1.5F), t.getRightRotation()));
                           });
                           this.displays.set(ix, display);
                        }

                        if (spearProgress < 1.0D) {
                           for(int p = 0; p < 5; ++p) {
                              double angle = RadiantSpearVolleyAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                              double dist = (1.0D - spearProgress) * 2.0D;
                              double x = Math.cos(angle) * dist;
                              double y = (RadiantSpearVolleyAbility.this.random.nextDouble() - 0.5D) * dist;
                              double z = Math.sin(angle) * dist;
                              Location particleLoc = loc.clone().add(x, y, z);
                              Vector toSpear = loc.toVector().subtract(particleLoc.toVector()).normalize().multiply(0.2D);
                              world.spawnParticle(Particle.END_ROD, particleLoc, 1, toSpear.getX(), toSpear.getY(), toSpear.getZ(), 0.05D);
                           }
                        }

                        ParticleUtils.soulFlame(loc, 2, 0.1D, 0.0D);
                        ParticleUtils.endRod(loc, 1, 0.05D, 0.0D);
                        if (RadiantSpearVolleyAbility.this.random.nextDouble() < 0.1D) {
                           ParticleUtils.electricSpark(loc, 3, 0.2D, 0.02D);
                        }
                     }
                  }
               }

               if (this.tick % 10 == 0) {
                  ParticleUtils.playSound(((RadiantSpearVolleyAbility.SpearData)spears.get(0)).location, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5F, (float)(1.0D + progress * 0.5D));
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void launchSpearVolley(Player player, List<RadiantSpearVolleyAbility.SpearData> spears, double travelDistance, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(((RadiantSpearVolleyAbility.SpearData)spears.get(0)).location, Sound.ITEM_TRIDENT_THROW, 2.0F, 0.6F);
      ParticleUtils.playSound(((RadiantSpearVolleyAbility.SpearData)spears.get(0)).location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0F, 0.5F);
      ParticleUtils.playSound(((RadiantSpearVolleyAbility.SpearData)spears.get(0)).location, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5F, 1.0F);
      Iterator var6 = spears.iterator();

      while(var6.hasNext()) {
         RadiantSpearVolleyAbility.SpearData spear = (RadiantSpearVolleyAbility.SpearData)var6.next();
         int delay = spear.row * 3;
         this.runLater(() -> {
            this.launchSingleSpear(player, spear, travelDistance, settings);
         }, (long)delay);
      }

   }

   private void launchSingleSpear(final Player player, final RadiantSpearVolleyAbility.SpearData spear, final double travelDistance, final IvoryConfig.AbilitySettings settings) {
      World world = spear.location.getWorld();
      if (world != null) {
         final Vector direction = new Vector(0, 0, -1);
         final double speed = 3.0D;
         ParticleUtils.playSound(spear.location, Sound.ENTITY_ARROW_SHOOT, 0.8F, 0.5F);
         (new BukkitRunnable() {
            Location currentLoc;
            int tick;
            final int maxTicks;

            {
               this.currentLoc = spear.location.clone();
               this.tick = 0;
               this.maxTicks = (int)(travelDistance / speed);
            }

            public void run() {
               if (this.tick >= this.maxTicks) {
                  if (spear.display != null && spear.display.isValid()) {
                     spear.display.remove();
                  }

                  RadiantSpearVolleyAbility.this.createSpearImpact(player, this.currentLoc, settings);
                  this.cancel();
               } else {
                  this.currentLoc.add(direction.clone().multiply(speed));
                  if (spear.display != null && spear.display.isValid()) {
                     spear.display.teleport(this.currentLoc);
                  }

                  ParticleUtils.soulFlame(this.currentLoc, 3, 0.1D, 0.01D);
                  ParticleUtils.endRod(this.currentLoc, 2, 0.05D, 0.0D);
                  Location trailLoc = this.currentLoc.clone();

                  for(int i = 0; i < 6; ++i) {
                     trailLoc.add(0.0D, 0.0D, speed * 0.3D);
                     double fade = 1.0D - (double)i / 6.0D;
                     if (fade > 0.3D) {
                        ParticleUtils.endRod(trailLoc, 1, 0.02D, 0.0D);
                     }
                  }

                  if (RadiantSpearVolleyAbility.this.random.nextDouble() < 0.15D) {
                     ParticleUtils.electricSpark(this.currentLoc, 2, 0.15D, 0.02D);
                  }

                  if (this.tick % 3 == 0) {
                     DamageZone zone = DamageZone.sphere(player, this.currentLoc.clone(), 1.5D, settings.damagePerTick, settings.damageTickInterval, 5);
                     DamageZoneManager.getInstance(RadiantSpearVolleyAbility.this.plugin).register(zone);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createSpearImpact(Player player, final Location loc, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         ParticleUtils.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0F, 0.8F);
         ParticleUtils.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8F, 1.2F);
         ParticleUtils.flash(loc);
         ParticleUtils.soulFlame(loc, 30, 1.0D, 0.15D);
         ParticleUtils.endRod(loc, 20, 0.8D, 0.2D);
         ParticleUtils.electricSpark(loc, 15, 1.2D, 0.1D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 0.4D;
                  ParticleUtils.ring(loc, Particle.END_ROD, radius, (int)(radius * 6.0D) + 6);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.sphere(player, loc, 2.5D, settings.damagePerTick * 1.5D, settings.damageTickInterval, 15);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private static class SpearData {
      Location location;
      int row;
      BlockDisplay display;

      SpearData(Location location, int row) {
         this.location = location;
         this.row = row;
      }
   }
}
