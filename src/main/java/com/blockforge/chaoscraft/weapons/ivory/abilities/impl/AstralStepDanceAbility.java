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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AstralStepDanceAbility extends Ability {
   private final Random random = new Random();

   public AstralStepDanceAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "astral_step_dance";
   }

   public String getDisplayName() {
      return "Astral Step Dance";
   }

   public String getDescription() {
      return "Become one with the stars, teleporting rapidly and leaving devastating afterimages that strike all enemies in your wake.";
   }

   public void execute(final Player player) {
      final IvoryConfig.AbilitySettings settings = this.getSettings();
      final Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      int steps = settings.getInt("step-count", 8);
      ParticleUtils.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0F, 1.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.2F);
      final List<Location> teleportPoints = this.generateTeleportPattern(origin, radius, steps);
      ParticleUtils.flash(origin);
      this.createDepartureEffect(origin);
      (new BukkitRunnable() {
         int currentStep = 0;
         Location lastLocation = origin.clone();

         public void run() {
            if (this.currentStep >= teleportPoints.size()) {
               AstralStepDanceAbility.this.createReturnEffect(player, origin, settings);
               this.cancel();
            } else {
               Location targetLoc = (Location)teleportPoints.get(this.currentStep);
               AstralStepDanceAbility.this.createLightTrail(this.lastLocation, targetLoc);
               AstralStepDanceAbility.this.createAfterimage(player, this.lastLocation, settings);
               AstralStepDanceAbility.this.createArrivalEffect(targetLoc);
               ParticleUtils.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.2F + AstralStepDanceAbility.this.random.nextFloat() * 0.3F);
               this.lastLocation = targetLoc.clone();
               ++this.currentStep;
            }
         }
      }).runTaskTimer(this.plugin, 5L, 6L);
   }

   private List<Location> generateTeleportPattern(Location origin, double radius, int count) {
      List<Location> points = new ArrayList();
      World world = origin.getWorld();
      if (world == null) {
         return points;
      } else {
         for(int i = 0; i < count; ++i) {
            double angle = 6.283185307179586D / (double)count * (double)i;
            double dist = radius * (0.5D + this.random.nextDouble() * 0.5D);
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            Location point = origin.clone().add(x, 0.0D, z);
            point.setY((double)(world.getHighestBlockYAt(point) + 1));
            points.add(point);
         }

         return points;
      }
   }

   private void createDepartureEffect(final Location loc) {
      ParticleUtils.soulFlame(loc.clone().add(0.0D, 1.0D, 0.0D), 30, 0.5D, 0.1D);
      ParticleUtils.endRod(loc.clone().add(0.0D, 1.0D, 0.0D), 20, 0.4D, 0.15D);
      ParticleUtils.electricSpark(loc.clone().add(0.0D, 1.0D, 0.0D), 15, 0.6D, 0.08D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 8) {
               this.cancel();
            } else {
               ParticleUtils.ring(loc, Particle.END_ROD, (double)this.tick * 0.4D, this.tick * 4 + 8);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createArrivalEffect(final Location loc) {
      ParticleUtils.flash(loc.clone().add(0.0D, 1.0D, 0.0D));
      ParticleUtils.soulFlame(loc.clone().add(0.0D, 1.0D, 0.0D), 25, 0.4D, 0.08D);
      ParticleUtils.endRod(loc.clone().add(0.0D, 1.5D, 0.0D), 15, 0.3D, 0.1D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 5) {
               this.cancel();
            } else {
               double inwardRadius = 2.0D - (double)this.tick * 0.4D;
               if (inwardRadius > 0.0D) {
                  ParticleUtils.ring(loc.clone().add(0.0D, 1.0D, 0.0D), Particle.SOUL_FIRE_FLAME, inwardRadius, 12);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createLightTrail(Location from, Location to) {
      World world = from.getWorld();
      if (world != null) {
         final Location start = from.clone().add(0.0D, 1.0D, 0.0D);
         final Location end = to.clone().add(0.0D, 1.0D, 0.0D);
         ParticleUtils.line(start, end, Particle.END_ROD, 0.3D);
         ParticleUtils.line(start, end, Particle.SOUL_FIRE_FLAME, 0.5D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 10) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 10.0D;
                  Location trailPoint = start.clone().add(end.toVector().subtract(start.toVector()).multiply(progress));
                  ParticleUtils.endRod(trailPoint, 2, 0.2D, 0.02D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 2L);
      }
   }

   private void createAfterimage(final Player player, final Location loc, final IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         final ArmorStand afterimage = (ArmorStand)world.spawn(loc, ArmorStand.class, (stand) -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.setGlowing(true);
            stand.setCustomNameVisible(false);
         });
         (new BukkitRunnable() {
            int tick = 0;
            double rotation;

            {
               this.rotation = AstralStepDanceAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
            }

            public void run() {
               if (this.tick < 30 && afterimage.isValid()) {
                  double fade = 1.0D - (double)this.tick / 30.0D;
                  Location imageLoc = loc.clone().add(0.0D, 1.0D, 0.0D);

                  for(double y = 0.0D; y < 2.0D * fade; y += 0.2D) {
                     double bodyWidth = 0.4D * fade;
                     if (y > 1.5D) {
                        bodyWidth *= 0.5D;
                     }

                     for(int i = 0; i < 8; ++i) {
                        double angle = 0.7853981633974483D * (double)i + this.rotation;
                        double x = Math.cos(angle) * bodyWidth;
                        double z = Math.sin(angle) * bodyWidth;
                        Location particleLoc = imageLoc.clone().add(x, y, z);
                        if (AstralStepDanceAbility.this.random.nextDouble() < fade) {
                           ParticleUtils.endRod(particleLoc, 1, 0.05D, 0.0D);
                        }
                     }
                  }

                  if (this.tick == 10) {
                     AstralStepDanceAbility.this.createAfterimageAttack(player, imageLoc, settings);
                  }

                  this.rotation += 0.1D;
                  ++this.tick;
               } else {
                  if (afterimage.isValid()) {
                     afterimage.remove();
                  }

                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createAfterimageAttack(Player player, final Location loc, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 1.2F);
      (new BukkitRunnable() {
         int tick = 0;
         double slashAngle;

         {
            this.slashAngle = AstralStepDanceAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
         }

         public void run() {
            if (this.tick >= 5) {
               this.cancel();
            } else {
               double arcExtent = (double)this.tick * 0.6D;

               for(double a = -arcExtent; a <= arcExtent; a += 0.2D) {
                  double angle = this.slashAngle + a;
                  double dist = 2.0D + (double)this.tick * 0.3D;
                  double x = Math.cos(angle) * dist;
                  double z = Math.sin(angle) * dist;
                  Location slashPoint = loc.clone().add(x, 0.5D, z);
                  ParticleUtils.soulFlame(slashPoint, 1, 0.1D, 0.02D);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.sphere(player, loc, settings.radius / 4.0D, settings.damagePerTick, settings.damageTickInterval, 15);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }

   private void createReturnEffect(final Player player, final Location origin, final IvoryConfig.AbilitySettings settings) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0F, 0.8F);
         ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_DEACTIVATE, 1.5F, 1.0F);
         ParticleUtils.flash(origin.clone().add(0.0D, 1.0D, 0.0D));
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 20) {
                  AstralStepDanceAbility.this.createFinalBurst(player, origin, settings);
                  this.cancel();
               } else {
                  double convergingRadius = 10.0D - (double)this.tick * 0.5D;
                  Location center = origin.clone().add(0.0D, 1.0D, 0.0D);
                  if (convergingRadius > 0.0D) {
                     ParticleUtils.ring(center, Particle.END_ROD, convergingRadius, (int)(convergingRadius * 6.0D) + 10);
                     ParticleUtils.ring(center.clone().add(0.0D, 1.0D, 0.0D), Particle.SOUL_FIRE_FLAME, convergingRadius * 0.8D, (int)(convergingRadius * 4.0D) + 5);
                  }

                  ParticleUtils.soulFlame(center, 10, 0.5D, 0.15D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createFinalBurst(Player player, final Location origin, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.2F);
      final Location center = origin.clone().add(0.0D, 1.0D, 0.0D);
      ParticleUtils.flash(center);
      ParticleUtils.soulFlame(center, 100, 4.0D, 0.3D);
      ParticleUtils.endRod(center, 80, 3.0D, 0.4D);
      ParticleUtils.electricSpark(center, 50, 5.0D, 0.2D);
      ParticleUtils.firework(center, 40, 2.5D, 0.35D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 15) {
               this.cancel();
            } else {
               double radius = (double)this.tick * 0.8D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 12);
               ParticleUtils.ring(center, Particle.END_ROD, radius * 0.9D, (int)(radius * 6.0D) + 8);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.sphere(player, origin, settings.radius, settings.damagePerTick * 2.0D, settings.damageTickInterval, 30);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
