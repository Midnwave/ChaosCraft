package com.blockforge.chaoscraft.weapons.ivory.abilities.impl;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryEffectsManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZone;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZoneManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.ParticleUtils;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CelestialStormFrontAbility extends Ability {
   private final Random random = new Random();

   public CelestialStormFrontAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "celestial_storm_front";
   }

   public String getDisplayName() {
      return "Celestial Storm Front";
   }

   public String getDescription() {
      return "Summon a massive advancing wall of divine lightning 40 blocks wide that slowly advances, obliterating everything in its path.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double wallWidth = settings.getDouble("wall-width", 40.0D);
      double wallHeight = settings.getDouble("wall-height", 30.0D);
      double travelDistance = settings.getDouble("travel-distance", 50.0D);
      double speed = settings.getDouble("speed", 1.0D);
      ParticleUtils.playSound(origin, Sound.WEATHER_RAIN, 2.0F, 0.3F);
      ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_FIRE_AMBIENT, 2.0F, 0.5F);
      this.createWallFormation(origin, wallWidth, wallHeight, () -> {
         this.advanceWall(player, origin, wallWidth, wallHeight, travelDistance, speed, settings);
      });
   }

   private void createWallFormation(final Location origin, final double width, final double height, final Runnable onComplete) {
      final Location wallCenter = origin.clone().add(0.0D, height / 2.0D, -5.0D);
      (new BukkitRunnable() {
         int tick = 0;
         final int formTime = 40;

         public void run() {
            if (this.tick >= 40) {
               ParticleUtils.flash(wallCenter);
               ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.5F, 0.3F);
               onComplete.run();
               this.cancel();
            } else {
               double progress = (double)this.tick / 40.0D;
               double currentWidth = width * progress;
               double currentHeight = height * progress;

               for(double x = -currentWidth / 2.0D; x <= currentWidth / 2.0D; x += 2.0D) {
                  for(double y = 0.0D; y < currentHeight; y += 2.0D) {
                     if (CelestialStormFrontAbility.this.random.nextDouble() < 0.3D) {
                        Location particleLoc = wallCenter.clone().add(x, y - height / 2.0D, 0.0D);
                        ParticleUtils.electricSpark(particleLoc, 2, 0.5D, 0.05D);
                        if (CelestialStormFrontAbility.this.random.nextDouble() < 0.2D) {
                           ParticleUtils.soulFlame(particleLoc, 1, 0.3D, 0.02D);
                        }
                     }
                  }
               }

               for(int i = 0; i < 20; ++i) {
                  double convergeX = (CelestialStormFrontAbility.this.random.nextDouble() - 0.5D) * width * 1.5D;
                  double convergeY = CelestialStormFrontAbility.this.random.nextDouble() * height * 1.5D;
                  double targetX = Math.max(-currentWidth / 2.0D, Math.min(currentWidth / 2.0D, convergeX * progress));
                  double targetY = Math.max(0.0D, Math.min(currentHeight, convergeY * progress));
                  Location from = wallCenter.clone().add(convergeX, convergeY - height / 2.0D, 0.0D);
                  Location to = wallCenter.clone().add(targetX, targetY - height / 2.0D, 0.0D);
                  Vector dir = to.toVector().subtract(from.toVector()).normalize().multiply(0.5D);
                  ParticleUtils.spawn(from, Particle.END_ROD, 1, dir.getX(), dir.getY(), dir.getZ(), 0.1D);
               }

               if (this.tick % 10 == 0) {
                  ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, (float)(0.5D + progress * 0.30000001192092896D));
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void advanceWall(final Player player, final Location origin, final double width, final double height, final double travelDistance, double speed, final IvoryConfig.AbilitySettings settings) {
      final double tickSpeed = speed / 20.0D;
      final int totalTicks = (int)(travelDistance / tickSpeed);
      (new BukkitRunnable() {
         int tick = 0;
         double traveled = 0.0D;

         public void run() {
            if (this.tick >= totalTicks) {
               CelestialStormFrontAbility.this.createFinalImpact(player, origin.clone().add(0.0D, 0.0D, -travelDistance), width, height, settings);
               this.cancel();
            } else {
               this.traveled += tickSpeed;
               Location wallCenter = origin.clone().add(0.0D, height / 2.0D, -(5.0D + this.traveled));
               CelestialStormFrontAbility.this.renderWall(wallCenter, width, height);
               if (this.tick % 5 == 0) {
                  for(double x = -width / 2.0D; x <= width / 2.0D; x += width / 4.0D) {
                     Location zoneLoc = wallCenter.clone().add(x, -height / 2.0D, 0.0D);
                     DamageZone zone = DamageZone.box(player, zoneLoc, width / 4.0D, height, 2.0D, settings.damagePerTick, settings.damageTickInterval, 8);
                     DamageZoneManager.getInstance(CelestialStormFrontAbility.this.plugin).register(zone);
                  }
               }

               if (this.tick % 3 == 0) {
                  CelestialStormFrontAbility.this.createScorchedPath(wallCenter.clone().add(0.0D, -height / 2.0D, 1.0D), width);
               }

               if (this.tick % 20 == 0) {
                  ParticleUtils.playSound(wallCenter, Sound.WEATHER_RAIN, 2.0F, 0.3F);
               }

               if (this.tick % 30 == 0) {
                  ParticleUtils.playSound(wallCenter, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 0.8F);
               }

               if (this.tick % 10 == 0) {
                  ParticleUtils.playSound(wallCenter, Sound.BLOCK_FIRE_AMBIENT, 1.5F, 0.5F);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void renderWall(Location center, double width, double height) {
      World world = center.getWorld();
      if (world != null) {
         double x;
         double y;
         for(x = -width / 2.0D; x <= width / 2.0D; ++x) {
            for(y = -height / 2.0D; y < height / 2.0D; ++y) {
               Location rodLoc;
               if (this.random.nextDouble() < 0.4D) {
                  rodLoc = center.clone().add(x + (this.random.nextDouble() - 0.5D), y + (this.random.nextDouble() - 0.5D), (this.random.nextDouble() - 0.5D) * 2.0D);
                  world.spawnParticle(Particle.ELECTRIC_SPARK, rodLoc, 2, 0.3D, 0.3D, 0.2D, 0.03D);
               }

               if (this.random.nextDouble() < 0.2D) {
                  rodLoc = center.clone().add(x, y, this.random.nextDouble() - 0.5D);
                  world.spawnParticle(Particle.SOUL_FIRE_FLAME, rodLoc, 1, 0.2D, 0.2D, 0.1D, 0.02D);
               }

               if (this.random.nextDouble() < 0.15D) {
                  rodLoc = center.clone().add(x, y, 0.0D);
                  world.spawnParticle(Particle.END_ROD, rodLoc, 1, 0.1D, 0.1D, 0.1D, 0.01D);
               }
            }
         }

         if (this.random.nextDouble() < 0.15D) {
            x = (this.random.nextDouble() - 0.5D) * width;
            y = (this.random.nextDouble() - 0.5D) * height;
            ParticleUtils.flash(center.clone().add(x, y, 0.0D));
         }

         if (this.random.nextDouble() < 0.1D) {
            x = (this.random.nextDouble() - 0.5D) * width;
            y = height / 2.0D;
            double endX = x + (this.random.nextDouble() - 0.5D) * 10.0D;
            double endY = -height / 2.0D;
            Location start = center.clone().add(x, y, 0.0D);
            Location end = center.clone().add(endX, endY, 0.0D);
            this.drawLightningStreak(start, end);
         }

         Location topEdge;
         for(x = -height / 2.0D; x < height / 2.0D; x += 2.0D) {
            topEdge = center.clone().add(-width / 2.0D, x, 0.0D);
            world.spawnParticle(Particle.GLOW, topEdge, 2, 0.3D, 0.3D, 0.2D, 0.0D);
            Location rightEdge = center.clone().add(width / 2.0D, x, 0.0D);
            world.spawnParticle(Particle.GLOW, rightEdge, 2, 0.3D, 0.3D, 0.2D, 0.0D);
         }

         for(x = -width / 2.0D; x <= width / 2.0D; x += 2.0D) {
            topEdge = center.clone().add(x, height / 2.0D, 0.0D);
            world.spawnParticle(Particle.END_ROD, topEdge, 1, 0.2D, 0.1D, 0.2D, 0.02D);
         }

      }
   }

   private void drawLightningStreak(Location start, Location end) {
      World world = start.getWorld();
      if (world != null) {
         Location current = start.clone();
         Vector dir = end.toVector().subtract(start.toVector()).normalize();
         double distance = start.distance(end);
         double traveled = 0.0D;

         while(traveled < distance) {
            double jag = (this.random.nextDouble() - 0.5D) * 2.0D;
            current.add(jag * 0.3D, 0.0D, 0.0D);
            current.add(dir.clone().multiply(1.5D));
            ++traveled;
            world.spawnParticle(Particle.ELECTRIC_SPARK, current, 3, 0.1D, 0.1D, 0.1D, 0.02D);
            world.spawnParticle(Particle.END_ROD, current, 1, 0.05D, 0.05D, 0.05D, 0.0D);
         }

      }
   }

   private void createScorchedPath(Location center, double width) {
      World world = center.getWorld();
      if (world != null) {
         for(double x = -width / 2.0D; x <= width / 2.0D; x += 3.0D) {
            Location pathLoc = center.clone().add(x, 0.0D, 0.0D);
            pathLoc.setY((double)world.getHighestBlockYAt(pathLoc) + 0.1D);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, pathLoc, 3, 0.5D, 0.1D, 0.5D, 0.01D);
            if (this.random.nextDouble() < 0.3D) {
               world.spawnParticle(Particle.ELECTRIC_SPARK, pathLoc, 2, 0.3D, 0.2D, 0.3D, 0.02D);
            }
         }

      }
   }

   private void createFinalImpact(Player player, final Location center, double width, double height, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.5F);
      ParticleUtils.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0F, 0.4F);
      ParticleUtils.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);

      for(double x = -width / 2.0D; x <= width / 2.0D; x += 5.0D) {
         Location impactLoc = center.clone().add(x, 0.0D, 0.0D);
         ParticleUtils.flash(impactLoc.clone().add(0.0D, height / 4.0D, 0.0D));
         ParticleUtils.electricSpark(impactLoc, 50, 5.0D, 0.2D);
         ParticleUtils.soulFlame(impactLoc.clone().add(0.0D, 2.0D, 0.0D), 40, 4.0D, 0.15D);
         ParticleUtils.endRod(impactLoc.clone().add(0.0D, 4.0D, 0.0D), 30, 3.0D, 0.2D);
      }

      ParticleUtils.soulFlame(center, 150, 10.0D, 0.3D);
      ParticleUtils.electricSpark(center, 120, 12.0D, 0.25D);
      ParticleUtils.firework(center.clone().add(0.0D, 5.0D, 0.0D), 80, 6.0D, 0.4D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 25) {
               this.cancel();
            } else {
               double radius = (double)this.tick * 1.5D;

               for(int h = 0; h < 3; ++h) {
                  Location ringLoc = center.clone().add(0.0D, (double)(h * 5), 0.0D);
                  ParticleUtils.ring(ringLoc, Particle.ELECTRIC_SPARK, radius, (int)(radius * 6.0D) + 10);
               }

               ParticleUtils.ring(center, Particle.SOUL_FIRE_FLAME, radius * 0.8D, (int)(radius * 4.0D) + 8);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.box(player, center.clone().add(-width / 2.0D, 0.0D, -5.0D), width, height, 10.0D, settings.damagePerTick * 3.0D, settings.damageTickInterval, 40);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
