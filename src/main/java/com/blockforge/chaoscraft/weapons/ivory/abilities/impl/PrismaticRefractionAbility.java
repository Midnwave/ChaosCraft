package com.blockforge.chaoscraft.weapons.ivory.abilities.impl;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryEffectsManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZone;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZoneManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.ParticleUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PrismaticRefractionAbility extends Ability {
   private final Random random = new Random();

   public PrismaticRefractionAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "prismatic_refraction";
   }

   public String getDisplayName() {
      return "Prismatic Refraction";
   }

   public String getDescription() {
      return "Fire a concentrated beam of light that refracts into multiple devastating prismatic beams spreading across the battlefield.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double beamLength = settings.getDouble("beam-length", 40.0D);
      int splitCount = settings.getInt("split-count", 7);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 1.5F);
      ParticleUtils.playSound(origin, Sound.ENTITY_GUARDIAN_ATTACK, 1.5F, 1.2F);
      this.createChargeEffect(origin, () -> {
         this.fireMainBeam(player, origin, beamLength, splitCount, settings);
      });
   }

   private void createChargeEffect(final Location loc, final Runnable onComplete) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 20) {
               onComplete.run();
               this.cancel();
            } else {
               double progress = (double)this.tick / 20.0D;
               Location chargeLoc = loc.clone().add(0.0D, 1.5D, 0.0D);
               double convergeRadius = 3.0D * (1.0D - progress);

               for(int i = 0; i < 15; ++i) {
                  double angle = 0.41887902047863906D * (double)i + this.rotation;
                  double x = Math.cos(angle) * convergeRadius;
                  double z = Math.sin(angle) * convergeRadius;
                  Location particleLoc = chargeLoc.clone().add(x, 0.0D, z);
                  ParticleUtils.endRod(particleLoc, 1, 0.1D, 0.05D);
               }

               ParticleUtils.glow(chargeLoc, (int)(progress * 15.0D), 0.3D + progress * 0.5D, 0.02D);
               ParticleUtils.soulFlame(chargeLoc, (int)(progress * 10.0D), 0.2D + progress * 0.3D, 0.01D);
               if (this.tick % 5 == 0) {
                  ParticleUtils.flash(chargeLoc);
               }

               this.rotation += 0.2D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void fireMainBeam(final Player player, Location origin, final double beamLength, final int splitCount, final IvoryConfig.AbilitySettings settings) {
      World world = origin.getWorld();
      if (world != null) {
         final Location startLoc = origin.clone().add(0.0D, 1.5D, 0.0D);
         final Location splitPoint = startLoc.clone().add(0.0D, 0.0D, -beamLength * 0.5D);
         ParticleUtils.playSound(origin, Sound.ENTITY_GUARDIAN_ATTACK, 2.0F, 1.5F);
         ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.5F, 1.2F);
         (new BukkitRunnable() {
            int tick = 0;
            Location currentEnd = startLoc.clone();
            final double speed = 3.0D;
            boolean hasSplit = false;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  this.currentEnd.add(0.0D, 0.0D, -3.0D);
                  double beamProgress = Math.min(1.0D, startLoc.distance(this.currentEnd) / (beamLength * 0.5D));
                  Location beamEnd = startLoc.clone().add(this.currentEnd.toVector().subtract(startLoc.toVector()).multiply(beamProgress));
                  ParticleUtils.line(startLoc, beamEnd, Particle.END_ROD, 0.2D);
                  ParticleUtils.line(startLoc, beamEnd, Particle.SOUL_FIRE_FLAME, 0.3D);

                  for(double d = 0.0D; d < startLoc.distance(beamEnd); ++d) {
                     Location glowLoc = startLoc.clone().add(beamEnd.toVector().subtract(startLoc.toVector()).normalize().multiply(d));
                     ParticleUtils.glow(glowLoc, 2, 0.3D, 0.0D);
                  }

                  if (!this.hasSplit && this.currentEnd.distance(startLoc) >= beamLength * 0.5D) {
                     this.hasSplit = true;
                     PrismaticRefractionAbility.this.createSplitEffect(player, splitPoint, splitCount, beamLength * 0.5D, settings);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createSplitEffect(Player player, Location splitPoint, int splitCount, double beamLength, IvoryConfig.AbilitySettings settings) {
      World world = splitPoint.getWorld();
      if (world != null) {
         ParticleUtils.playSound(splitPoint, Sound.BLOCK_GLASS_BREAK, 1.5F, 1.5F);
         ParticleUtils.playSound(splitPoint, Sound.BLOCK_BEACON_POWER_SELECT, 1.5F, 1.8F);
         ParticleUtils.flash(splitPoint);
         ParticleUtils.soulFlame(splitPoint, 30, 1.5D, 0.15D);
         ParticleUtils.endRod(splitPoint, 25, 1.2D, 0.2D);
         ParticleUtils.electricSpark(splitPoint, 20, 2.0D, 0.1D);

         for(int i = 0; i < splitCount; ++i) {
            double spreadAngle = (double)(i - splitCount / 2) / (double)splitCount * 3.141592653589793D * 0.6D;
            Vector direction = (new Vector(Math.sin(spreadAngle), (this.random.nextDouble() - 0.5D) * 0.2D, -Math.cos(spreadAngle))).normalize();
            int delay = i * 2;
            this.runLater(() -> {
               this.fireSplitBeam(player, splitPoint.clone(), direction, beamLength, settings);
            }, (long)delay);
         }

      }
   }

   private void fireSplitBeam(final Player player, final Location start, final Vector direction, final double length, final IvoryConfig.AbilitySettings settings) {
      World world = start.getWorld();
      if (world != null) {
         ParticleUtils.playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8F, 1.8F);
         (new BukkitRunnable() {
            int tick = 0;
            Location currentLoc = start.clone();
            final double speed = 2.5D;
            List<Location> trailPoints = new ArrayList();

            public void run() {
               if (this.tick >= (int)(length / 2.5D) + 10) {
                  this.cancel();
               } else {
                  if (this.tick < (int)(length / 2.5D)) {
                     this.currentLoc.add(direction.clone().multiply(2.5D));
                     this.trailPoints.add(this.currentLoc.clone());
                     ParticleUtils.endRod(this.currentLoc, 5, 0.3D, 0.05D);
                     ParticleUtils.soulFlame(this.currentLoc, 3, 0.2D, 0.03D);
                     ParticleUtils.glow(this.currentLoc, 2, 0.2D, 0.0D);
                     DamageZone zone = DamageZone.sphere(player, this.currentLoc, 1.5D, settings.damagePerTick * 0.5D, 1, 3);
                     DamageZoneManager.getInstance(PrismaticRefractionAbility.this.plugin).register(zone);
                  }

                  double fade = 1.0D;
                  if (this.tick >= (int)(length / 2.5D)) {
                     fade = 1.0D - (double)(this.tick - (int)(length / 2.5D)) / 10.0D;
                  }

                  for(int i = Math.max(0, this.trailPoints.size() - 15); i < this.trailPoints.size() && fade > 0.0D; ++i) {
                     Location trailLoc = (Location)this.trailPoints.get(i);
                     double trailFade = fade * (1.0D - (double)(this.trailPoints.size() - i) / 15.0D);
                     if (trailFade > 0.1D) {
                        ParticleUtils.endRod(trailLoc, (int)(3.0D * trailFade), 0.15D, 0.0D);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }
}
