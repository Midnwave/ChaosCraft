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
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DivineOmegaCannonAbility extends Ability {
   private final Random random = new Random();

   public DivineOmegaCannonAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "divine_omega_cannon";
   }

   public String getDisplayName() {
      return "Divine Omega Cannon";
   }

   public String getDescription() {
      return "Transform the Celestial Ivory into its ultimate form - a colossal spinning weapon that fires a devastating 5-block laser beam, sweeping destruction across all who oppose you.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double beamLength = settings.getDouble("beam-length", 60.0D);
      double beamWidth = settings.getDouble("beam-width", 5.0D);
      double sweepAngle = Math.toRadians(settings.getDouble("sweep-angle", 120.0D));
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.5F, 0.4F);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.6F);
      List<BlockDisplay> weaponDisplays = this.createTransformedWeapon(origin);
      this.runLater(() -> {
         this.chargeOmegaCannon(origin, weaponDisplays);
      }, 40L);
      this.runLater(() -> {
         this.fireSweepingLaser(player, origin, beamLength, beamWidth, sweepAngle, settings, weaponDisplays);
      }, 100L);
      this.runLater(() -> {
         this.revertWeapon(weaponDisplays);
      }, 220L);
   }

   private List<BlockDisplay> createTransformedWeapon(Location origin) {
      final List<BlockDisplay> displays = new ArrayList();
      World world = origin.getWorld();
      if (world == null) {
         return displays;
      } else {
         final Location weaponCenter = origin.clone().add(0.0D, 2.0D, 0.0D);
         BlockDisplay blade = (BlockDisplay)world.spawn(weaponCenter, BlockDisplay.class, (d) -> {
            d.setBlock(Material.DIAMOND_BLOCK.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.5F, -0.5F, -4.0F), t.getLeftRotation(), new Vector3f(1.0F, 1.0F, 8.0F), t.getRightRotation()));
         });
         displays.add(blade);
         BlockDisplay core = (BlockDisplay)world.spawn(weaponCenter, BlockDisplay.class, (d) -> {
            d.setBlock(Material.SEA_LANTERN.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.75F, -0.75F, -0.75F), t.getLeftRotation(), new Vector3f(1.5F, 1.5F, 1.5F), t.getRightRotation()));
         });
         displays.add(core);

         for(int side = -1; side <= 1; side += 2) {
            BlockDisplay wing = (BlockDisplay)world.spawn(weaponCenter.clone().add((double)side * 1.5D, 0.0D, -2.0D), BlockDisplay.class, (d) -> {
               d.setBlock(Material.QUARTZ_BLOCK.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.3F, -0.3F, -1.5F), t.getLeftRotation(), new Vector3f(0.6F, 0.6F, 3.0F), t.getRightRotation()));
            });
            displays.add(wing);
         }

         BlockDisplay muzzle = (BlockDisplay)world.spawn(weaponCenter.clone().add(0.0D, 0.0D, -4.5D), BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.6F, -0.6F, -0.3F), t.getLeftRotation(), new Vector3f(1.2F, 1.2F, 0.6F), t.getRightRotation()));
         });
         displays.add(muzzle);
         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;
            float scale = 0.3F;

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 40.0D;
                  this.scale = 0.3F + (float)(progress * 2.700000047683716D);
                  this.rotation += 0.05D + progress * 0.15D;

                  for(int i = 0; i < displays.size(); ++i) {
                     BlockDisplay display = (BlockDisplay)displays.get(i);
                     if (display.isValid()) {
                        Transformation t = display.getTransformation();
                        Vector3f currentScale = t.getScale();
                        display.setTransformation(new Transformation(t.getTranslation(), (new Quaternionf()).rotateY((float)this.rotation), new Vector3f(currentScale.x * (1.0F + (this.scale - 1.0F) * 0.02F), currentScale.y * (1.0F + (this.scale - 1.0F) * 0.02F), currentScale.z * (1.0F + (this.scale - 1.0F) * 0.02F)), t.getRightRotation()));
                     }
                  }

                  ParticleUtils.soulFlame(weaponCenter, (int)(10.0D * progress), 1.5D * (double)this.scale, 0.03D);
                  ParticleUtils.electricSpark(weaponCenter, (int)(5.0D * progress), (double)(2.0F * this.scale), 0.02D);
                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(weaponCenter, Sound.BLOCK_BEACON_AMBIENT, 1.0F, 0.5F + (float)progress * 0.5F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         return displays;
      }
   }

   private void chargeOmegaCannon(Location origin, final List<BlockDisplay> displays) {
      final Location weaponCenter = origin.clone().add(0.0D, 2.0D, 0.0D);
      final Location muzzle = weaponCenter.clone().add(0.0D, 0.0D, -4.5D);
      ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.5F, 0.4F);
      ParticleUtils.playSound(origin, Sound.ENTITY_GUARDIAN_ATTACK, 2.0F, 0.3F);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 60) {
               this.cancel();
            } else {
               double intensity = (double)this.tick / 60.0D;
               this.rotation += 0.2D;
               Iterator var3 = displays.iterator();

               while(var3.hasNext()) {
                  BlockDisplay display = (BlockDisplay)var3.next();
                  if (display.isValid()) {
                     Transformation t = display.getTransformation();
                     display.setTransformation(new Transformation(t.getTranslation(), (new Quaternionf()).rotateY((float)this.rotation), t.getScale(), t.getRightRotation()));
                  }
               }

               for(int i = 0; i < (int)(20.0D * intensity); ++i) {
                  double angle = DivineOmegaCannonAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double dist = 8.0D * (1.0D - intensity) + 1.0D;
                  double y = (DivineOmegaCannonAbility.this.random.nextDouble() - 0.5D) * 4.0D;
                  double x = Math.cos(angle) * dist;
                  double z = Math.sin(angle) * dist - 4.5D;
                  Location convergePoint = weaponCenter.clone().add(x, y, z);
                  ParticleUtils.endRod(convergePoint, 1, 0.1D, 0.15D);
               }

               ParticleUtils.soulFlame(muzzle, (int)(15.0D * intensity), 1.0D + intensity, 0.02D);
               ParticleUtils.glow(muzzle, (int)(10.0D * intensity), 0.8D + intensity * 0.5D, 0.01D);
               ParticleUtils.electricSpark(muzzle, (int)(8.0D * intensity), 1.5D, 0.03D);
               if (this.tick % 5 == 0) {
                  ParticleUtils.flash(muzzle);
               }

               if (this.tick % (15 - (int)(intensity * 10.0D)) == 0) {
                  ParticleUtils.playSound(muzzle, Sound.ENTITY_GUARDIAN_ATTACK, 1.0F, 0.5F + (float)intensity * 0.5F);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void fireSweepingLaser(final Player player, final Location origin, final double beamLength, final double beamWidth, final double sweepAngle, final IvoryConfig.AbilitySettings settings, final List<BlockDisplay> weaponDisplays) {
      final World world = origin.getWorld();
      if (world != null) {
         final Location weaponCenter = origin.clone().add(0.0D, 2.0D, 0.0D);
         ParticleUtils.playSound(origin, Sound.ENTITY_GUARDIAN_ATTACK, 3.0F, 0.3F);
         ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SHOOT, 2.5F, 0.4F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.6F);
         final Location muzzle = weaponCenter.clone().add(0.0D, 0.0D, -4.5D);
         ParticleUtils.flash(muzzle);
         final List<BlockDisplay> beamDisplays = new ArrayList();
         (new BukkitRunnable() {
            int tick = 0;
            final int sweepDuration = 100;
            double weaponRotation = 0.0D;
            double beamRotation = -sweepAngle / 2.0D;

            public void run() {
               if (this.tick >= 100) {
                  Iterator var20 = beamDisplays.iterator();

                  while(var20.hasNext()) {
                     BlockDisplay d = (BlockDisplay)var20.next();
                     if (d.isValid()) {
                        d.remove();
                     }
                  }

                  this.cancel();
               } else {
                  double progress = (double)this.tick / 100.0D;
                  this.beamRotation = -sweepAngle / 2.0D + sweepAngle * progress;
                  this.weaponRotation += 0.25D;
                  Iterator var3 = weaponDisplays.iterator();

                  while(var3.hasNext()) {
                     BlockDisplay display = (BlockDisplay)var3.next();
                     if (display.isValid()) {
                        Transformation t = display.getTransformation();
                        display.setTransformation(new Transformation(t.getTranslation(), (new Quaternionf()).rotateY((float)(this.weaponRotation + this.beamRotation)), t.getScale(), t.getRightRotation()));
                     }
                  }

                  double beamDirX = Math.sin(this.beamRotation);
                  double beamDirZ = -Math.cos(this.beamRotation);
                  Vector beamDir = new Vector(beamDirX, 0.0D, beamDirZ);
                  Location beamEnd;
                  if (this.tick == 0) {
                     for(double distx = 0.0D; distx < beamLength; distx += 3.0D) {
                        beamEnd = muzzle.clone().add(beamDir.clone().multiply(distx));
                        BlockDisplay segment = (BlockDisplay)world.spawn(beamEnd, BlockDisplay.class, (dx) -> {
                           dx.setBlock(Material.SEA_LANTERN.createBlockData());
                           dx.setGlowing(true);
                           dx.setBrightness(new Brightness(15, 15));
                           Transformation t = dx.getTransformation();
                           dx.setTransformation(new Transformation(new Vector3f((float)(-beamWidth / 2.0D), (float)(-beamWidth / 2.0D), -1.5F), t.getLeftRotation(), new Vector3f((float)beamWidth, (float)beamWidth, 3.0F), t.getRightRotation()));
                        });
                        beamDisplays.add(segment);
                     }
                  }

                  int displayIdx = 0;

                  Location damagePoint;
                  for(double distxx = 0.0D; distxx < beamLength && displayIdx < beamDisplays.size(); distxx += 3.0D) {
                     Location newMuzzle = weaponCenter.clone().add(Math.sin(this.beamRotation) * 4.5D, 0.0D, -Math.cos(this.beamRotation) * 4.5D);
                     damagePoint = newMuzzle.clone().add(beamDir.clone().multiply(distxx));
                     BlockDisplay segmentx = (BlockDisplay)beamDisplays.get(displayIdx);
                     if (segmentx.isValid()) {
                        segmentx.teleport(damagePoint);
                        Transformation tx = segmentx.getTransformation();
                        segmentx.setTransformation(new Transformation(tx.getTranslation(), (new Quaternionf()).rotateY((float)this.beamRotation), tx.getScale(), tx.getRightRotation()));
                     }

                     ++displayIdx;
                  }

                  Location currentMuzzle = weaponCenter.clone().add(Math.sin(this.beamRotation) * 4.5D, 0.0D, -Math.cos(this.beamRotation) * 4.5D);

                  double dist;
                  for(dist = 0.0D; dist < beamLength; dist += 0.5D) {
                     damagePoint = currentMuzzle.clone().add(beamDir.clone().multiply(dist));
                     ParticleUtils.soulFlame(damagePoint, 5, beamWidth * 0.3D, 0.01D);
                     ParticleUtils.endRod(damagePoint, 3, beamWidth * 0.4D, 0.02D);

                     for(double edge = -beamWidth / 2.0D; edge <= beamWidth / 2.0D; edge += beamWidth) {
                        double perpX = -beamDirZ * edge;
                        double perpZ = beamDirX * edge;
                        Location edgePoint = damagePoint.clone().add(perpX, 0.0D, perpZ);
                        ParticleUtils.electricSpark(edgePoint, 1, 0.3D, 0.02D);
                     }
                  }

                  ParticleUtils.soulFlame(currentMuzzle, 20, beamWidth * 0.5D, 0.05D);
                  ParticleUtils.flash(currentMuzzle);
                  if (this.tick % 5 == 0) {
                     for(dist = 5.0D; dist < beamLength; dist += 8.0D) {
                        damagePoint = currentMuzzle.clone().add(beamDir.clone().multiply(dist));
                        DamageZone zone = DamageZone.cylinder(player, damagePoint, beamWidth, 4.0D, settings.damagePerTick * 2.0D, settings.damageTickInterval, 8);
                        DamageZoneManager.getInstance(DivineOmegaCannonAbility.this.plugin).register(zone);
                     }
                  }

                  beamEnd = currentMuzzle.clone().add(beamDir.clone().multiply(beamLength));
                  ParticleUtils.soulFlame(beamEnd, 30, beamWidth, 0.15D);
                  ParticleUtils.firework(beamEnd, 15, beamWidth * 0.8D, 0.2D);
                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(currentMuzzle, Sound.ENTITY_GUARDIAN_ATTACK, 1.5F, 0.4F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;
            double beamRotation = -sweepAngle / 2.0D;

            public void run() {
               if (this.tick >= 100) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 100.0D;
                  this.beamRotation = -sweepAngle / 2.0D + sweepAngle * progress;
                  double beamDirX = Math.sin(this.beamRotation);
                  double beamDirZ = -Math.cos(this.beamRotation);

                  for(double dist = 5.0D; dist < beamLength; dist += 3.0D) {
                     Location scorchPoint = origin.clone().add(beamDirX * dist, 0.1D, beamDirZ * dist);
                     ParticleUtils.soulFlame(scorchPoint, 2, 1.5D, 0.02D);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 5L, 2L);
      }
   }

   private void revertWeapon(final List<BlockDisplay> displays) {
      ParticleUtils.playSound(displays.isEmpty() ? null : ((BlockDisplay)displays.get(0)).getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.8F);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 30) {
               Iterator var7 = displays.iterator();

               while(var7.hasNext()) {
                  BlockDisplay d = (BlockDisplay)var7.next();
                  if (d.isValid()) {
                     d.remove();
                  }
               }

               this.cancel();
            } else {
               double shrink = 1.0D - (double)this.tick / 30.0D;
               Iterator var3 = displays.iterator();

               while(var3.hasNext()) {
                  BlockDisplay display = (BlockDisplay)var3.next();
                  if (display.isValid()) {
                     Transformation t = display.getTransformation();
                     Vector3f currentScale = t.getScale();
                     display.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(), new Vector3f(currentScale.x * (float)(0.95D + shrink * 0.05D), currentScale.y * (float)(0.95D + shrink * 0.05D), currentScale.z * (float)(0.95D + shrink * 0.05D)), t.getRightRotation()));
                     ParticleUtils.endRod(display.getLocation(), 2, 0.5D, 0.05D);
                  }
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }
}
