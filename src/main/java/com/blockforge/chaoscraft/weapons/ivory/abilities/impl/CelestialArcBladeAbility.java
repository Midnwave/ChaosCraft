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
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CelestialArcBladeAbility extends Ability {
   private final Random random = new Random();

   public CelestialArcBladeAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "celestial_arc_blade";
   }

   public String getDisplayName() {
      return "Celestial Arc Blade";
   }

   public String getDescription() {
      return "Manifest a colossal blade of crystallized starlight that sweeps in a devastating arc, cutting through all who stand before you.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double bladeLength = settings.getDouble("blade-length", 15.0D);
      double arcAngle = Math.toRadians(settings.getDouble("arc-angle", 180.0D));
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THROW, 2.0F, 0.6F);
      ParticleUtils.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.0F);
      List<BlockDisplay> bladeDisplays = this.createBlade(origin, bladeLength);
      this.runLater(() -> {
         this.executeSweep(player, origin, bladeLength, arcAngle, settings, bladeDisplays);
      }, 20L);
   }

   private List<BlockDisplay> createBlade(Location origin, final double bladeLength) {
      List<BlockDisplay> displays = new ArrayList();
      World world = origin.getWorld();
      if (world == null) {
         return displays;
      } else {
         final Location bladeBase = origin.clone().add(0.0D, 1.5D, 0.0D);

         double dist;
         for(dist = 0.0D; dist < bladeLength; ++dist) {
            double segmentWidth = 0.8D - dist / bladeLength * 0.4D;
            double segmentHeight = 3.0D - dist / bladeLength * 1.5D;
            Location segmentLoc = bladeBase.clone().add(0.0D, 0.0D, -dist);
            BlockDisplay segment = (BlockDisplay)world.spawn(segmentLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.DIAMOND_BLOCK.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f((float)(-segmentWidth / 2.0D), (float)(-segmentHeight / 2.0D), -0.75F), t.getLeftRotation(), new Vector3f((float)segmentWidth, (float)segmentHeight, 1.5F), t.getRightRotation()));
            });
            displays.add(segment);
         }

         for(dist = 0.0D; dist < bladeLength; dist += 2.0D) {
            Location edgeLoc = bladeBase.clone().add(0.0D, 0.0D, -dist);
            BlockDisplay edge = (BlockDisplay)world.spawn(edgeLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.SEA_LANTERN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.15F, -1.6F, -1.0F), t.getLeftRotation(), new Vector3f(0.3F, 0.3F, 2.0F), t.getRightRotation()));
            });
            displays.add(edge);
         }

         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 20) {
                  this.cancel();
               } else {
                  for(double dist = 0.0D; dist < bladeLength; dist += 0.5D) {
                     Location bladeLoc = bladeBase.clone().add(0.0D, 0.0D, -dist);
                     if (CelestialArcBladeAbility.this.random.nextDouble() < 0.3D) {
                        ParticleUtils.endRod(bladeLoc, 2, 0.3D, 0.02D);
                     }

                     ParticleUtils.electricSpark(bladeLoc, 1, 0.4D, 0.01D);
                  }

                  ParticleUtils.soulFlame(bladeBase, 10, 0.5D, 0.03D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         return displays;
      }
   }

   private void executeSweep(final Player player, final Location origin, final double bladeLength, double arcAngle, final IvoryConfig.AbilitySettings settings, final List<BlockDisplay> bladeDisplays) {
      World world = origin.getWorld();
      if (world != null) {
         final Location sweepCenter = origin.clone().add(0.0D, 1.5D, 0.0D);
         ParticleUtils.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2.5F, 0.4F);
         ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_3, 2.0F, 0.8F);
         final double startAngle = -arcAngle / 2.0D;
         final double endAngle = arcAngle / 2.0D;
         (new BukkitRunnable() {
            int tick = 0;
            final int sweepDuration = 30;
            double currentAngle = startAngle;

            public void run() {
               if (this.tick >= 30) {
                  Iterator var19 = bladeDisplays.iterator();

                  while(var19.hasNext()) {
                     BlockDisplay d = (BlockDisplay)var19.next();
                     if (d.isValid()) {
                        d.remove();
                     }
                  }

                  CelestialArcBladeAbility.this.createSweepFinale(player, origin, bladeLength, settings);
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 30.0D;
                  double easeProgress = Math.sin(progress * 3.141592653589793D * 0.5D);
                  this.currentAngle = startAngle + (endAngle - startAngle) * easeProgress;
                  double dirX = Math.sin(this.currentAngle);
                  double dirZ = -Math.cos(this.currentAngle);
                  int displayIndex = 0;
                  Iterator var10 = bladeDisplays.iterator();

                  double dist;
                  Location damageLoc;
                  while(var10.hasNext()) {
                     BlockDisplay display = (BlockDisplay)var10.next();
                     if (display.isValid() && displayIndex < bladeDisplays.size()) {
                        dist = (double)(displayIndex % 10) * 1.5D;
                        if (displayIndex >= 10) {
                           dist = (double)((displayIndex - 10) * 2);
                        }

                        damageLoc = sweepCenter.clone().add(dirX * dist, 0.0D, dirZ * dist);
                        display.teleport(damageLoc);
                        Transformation t = display.getTransformation();
                        display.setTransformation(new Transformation(t.getTranslation(), (new Quaternionf()).rotateY((float)(-this.currentAngle)), t.getScale(), t.getRightRotation()));
                        ++displayIndex;
                     }
                  }

                  double trailAngle;
                  for(trailAngle = 0.0D; trailAngle < bladeLength; trailAngle += 0.3D) {
                     Location edgeLoc = sweepCenter.clone().add(dirX * trailAngle, 0.0D, dirZ * trailAngle);
                     ParticleUtils.soulFlame(edgeLoc, 3, 0.2D, 0.02D);
                     ParticleUtils.endRod(edgeLoc.clone().add(0.0D, -1.5D, 0.0D), 2, 0.15D, 0.01D);

                     for(double y = -1.5D; y <= 1.5D; y += 0.5D) {
                        if (CelestialArcBladeAbility.this.random.nextDouble() < 0.2D) {
                           ParticleUtils.electricSpark(edgeLoc.clone().add(0.0D, y, 0.0D), 1, 0.1D, 0.01D);
                        }
                     }
                  }

                  trailAngle = this.currentAngle - 0.3D;

                  for(dist = 0.0D; dist < bladeLength; ++dist) {
                     double trailX = Math.sin(trailAngle) * dist;
                     double trailZ = -Math.cos(trailAngle) * dist;
                     Location trailLoc = sweepCenter.clone().add(trailX, 0.0D, trailZ);
                     ParticleUtils.soulFlame(trailLoc, 2, 0.4D, 0.03D);
                  }

                  for(dist = 2.0D; dist < bladeLength; dist += 3.0D) {
                     damageLoc = sweepCenter.clone().add(dirX * dist, 0.0D, dirZ * dist);
                     DamageZone zone = DamageZone.box(player, damageLoc, 1.5D, 3.0D, 1.5D, settings.damagePerTick, settings.damageTickInterval, 3);
                     DamageZoneManager.getInstance(CelestialArcBladeAbility.this.plugin).register(zone);
                  }

                  if (this.tick % 5 == 0) {
                     ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THROW, 1.0F, 1.2F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createSweepFinale(Player player, final Location origin, final double bladeLength, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
      ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5F, 0.8F);
      ParticleUtils.flash(origin.clone().add(0.0D, 1.5D, 0.0D));
      double arcAngle = 3.141592653589793D;

      for(double angle = -arcAngle / 2.0D; angle <= arcAngle / 2.0D; angle += 0.3D) {
         double x = Math.sin(angle) * bladeLength;
         double z = -Math.cos(angle) * bladeLength;
         Location explosionLoc = origin.clone().add(x, 0.5D, z);
         int delay = (int)((angle + arcAngle / 2.0D) / arcAngle * 15.0D);
         this.runLater(() -> {
            ParticleUtils.soulFlame(explosionLoc, 25, 1.5D, 0.15D);
            ParticleUtils.endRod(explosionLoc, 15, 1.2D, 0.2D);
            ParticleUtils.playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.3F);
         }, (long)delay);
      }

      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 15) {
               this.cancel();
            } else {
               double arcRadius = bladeLength + (double)this.tick * 0.8D;

               for(double angle = -1.5707963267948966D; angle <= 1.5707963267948966D; angle += 0.1D) {
                  double x = Math.sin(angle) * arcRadius;
                  double z = -Math.cos(angle) * arcRadius;
                  Location arcLoc = origin.clone().add(x, 0.5D, z);
                  ParticleUtils.soulFlame(arcLoc, 1, 0.2D, 0.0D);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               double intensity = 1.0D - (double)this.tick / 40.0D;

               for(double angle = -1.5707963267948966D; angle <= 1.5707963267948966D; angle += 0.5D) {
                  double x = Math.sin(angle) * bladeLength * 0.8D;
                  double z = -Math.cos(angle) * bladeLength * 0.8D;
                  Location lingerLoc = origin.clone().add(x, 0.5D, z);
                  if (CelestialArcBladeAbility.this.random.nextDouble() < intensity * 0.3D) {
                     ParticleUtils.endRod(lingerLoc, 1, 0.3D, 0.02D);
                  }
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 5L, 2L);
      DamageZone zone = DamageZone.cone(player, origin, bladeLength, 3.141592653589793D, settings.damagePerTick * 2.0D, settings.damageTickInterval, 30);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
