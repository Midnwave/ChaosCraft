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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class StormCallersWrathAbility extends Ability {
   private final Random random = new Random();

   public StormCallersWrathAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "storm_callers_wrath";
   }

   public String getDisplayName() {
      return "Storm Caller's Wrath";
   }

   public String getDescription() {
      return "Summon eight divine storm clouds that hover overhead, continuously firing forked lightning bolts that track and devastate enemies.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      int cloudCount = settings.getInt("cloud-count", 8);
      double cloudHeight = settings.getDouble("cloud-height", 25.0D);
      int durationSeconds = settings.getInt("duration-seconds", 15);
      ParticleUtils.playSound(origin, Sound.WEATHER_RAIN, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.8F);
      ParticleUtils.playSound(origin, Sound.BLOCK_CONDUIT_AMBIENT, 2.0F, 0.5F);
      List<StormCallersWrathAbility.StormCloud> clouds = this.createStormClouds(origin, cloudCount, cloudHeight, settings.radius);
      this.executeStorm(player, origin, clouds, durationSeconds, settings);
   }

   private List<StormCallersWrathAbility.StormCloud> createStormClouds(Location origin, int count, double height, double radius) {
      List<StormCallersWrathAbility.StormCloud> clouds = new ArrayList();

      for(int i = 0; i < count; ++i) {
         double angle = 6.283185307179586D / (double)count * (double)i;
         double dist = radius * 0.6D;
         double x = Math.cos(angle) * dist;
         double z = Math.sin(angle) * dist;
         Location cloudLoc = origin.clone().add(x, height, z);
         StormCallersWrathAbility.StormCloud cloud = new StormCallersWrathAbility.StormCloud(cloudLoc, angle, 3.0D + this.random.nextDouble() * 2.0D);
         clouds.add(cloud);
         ParticleUtils.flash(cloudLoc);
         ParticleUtils.whiteAsh(cloudLoc, 50, 3.0D, 0.1D);
         ParticleUtils.glow(cloudLoc, 30, 2.5D, 0.05D);
         ParticleUtils.playSound(cloudLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);
      }

      return clouds;
   }

   private void executeStorm(final Player player, final Location origin, final List<StormCallersWrathAbility.StormCloud> clouds, int durationSeconds, final IvoryConfig.AbilitySettings settings) {
      final int durationTicks = durationSeconds * 20;
      (new BukkitRunnable() {
         int tick = 0;
         double orbitRotation = 0.0D;
         int lightningCooldown = 0;
         int currentCloudIndex = 0;

         public void run() {
            if (this.tick >= durationTicks) {
               StormCallersWrathAbility.this.createFinalStormBurst(player, origin, clouds, settings);
               this.cancel();
            } else {
               this.orbitRotation += 0.01D;

               for(int i = 0; i < clouds.size(); ++i) {
                  StormCallersWrathAbility.StormCloud cloudx = (StormCallersWrathAbility.StormCloud)clouds.get(i);
                  double baseAngle = 6.283185307179586D / (double)clouds.size() * (double)i;
                  cloudx.angle = baseAngle + this.orbitRotation;
                  double dist = settings.radius * 0.6D;
                  double x = Math.cos(cloudx.angle) * dist;
                  double z = Math.sin(cloudx.angle) * dist;
                  cloudx.location = origin.clone().add(x, cloudx.location.getY() - origin.getY(), z);
                  cloudx.rotation += 0.05D;
                  StormCallersWrathAbility.this.renderCloud(cloudx);
               }

               --this.lightningCooldown;
               if (this.lightningCooldown <= 0) {
                  StormCallersWrathAbility.StormCloud cloud = (StormCallersWrathAbility.StormCloud)clouds.get(this.currentCloudIndex);
                  double targetAngle = cloud.angle + (StormCallersWrathAbility.this.random.nextDouble() - 0.5D) * 3.141592653589793D / 2.0D;
                  double targetDist = StormCallersWrathAbility.this.random.nextDouble() * settings.radius * 0.8D;
                  Location targetLoc = origin.clone().add(Math.cos(targetAngle) * targetDist, 0.0D, Math.sin(targetAngle) * targetDist);
                  StormCallersWrathAbility.this.fireForkedLightning(player, cloud.location, targetLoc, settings);
                  this.currentCloudIndex = (this.currentCloudIndex + 1) % clouds.size();
                  this.lightningCooldown = 15;
               }

               if (this.tick % 40 == 0) {
                  ParticleUtils.playSound(origin, Sound.WEATHER_RAIN, 1.0F, 0.5F);
                  ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.2F);
               }

               if (this.tick % 60 == 0) {
                  ParticleUtils.playSound(origin, Sound.BLOCK_CONDUIT_AMBIENT, 1.5F, 0.8F);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void renderCloud(StormCallersWrathAbility.StormCloud cloud) {
      Location loc = cloud.location;
      World world = loc.getWorld();
      if (world != null) {
         double size = cloud.size;

         Location particleLoc;
         int i;
         double angle;
         double dist;
         double x;
         double y;
         double z;
         for(i = 0; i < 15; ++i) {
            angle = cloud.rotation + this.random.nextDouble() * 3.141592653589793D * 2.0D;
            dist = this.random.nextDouble() * size;
            x = Math.cos(angle) * dist;
            y = (this.random.nextDouble() - 0.5D) * size * 0.4D;
            z = Math.sin(angle) * dist;
            particleLoc = loc.clone().add(x, y, z);
            world.spawnParticle(Particle.WHITE_ASH, particleLoc, 1, 0.2D, 0.1D, 0.2D, 0.0D);
         }

         for(i = 0; i < 8; ++i) {
            angle = cloud.rotation * 1.5D + this.random.nextDouble() * 3.141592653589793D * 2.0D;
            dist = this.random.nextDouble() * size * 0.8D;
            x = Math.cos(angle) * dist;
            y = (this.random.nextDouble() - 0.5D) * size * 0.3D;
            z = Math.sin(angle) * dist;
            particleLoc = loc.clone().add(x, y, z);
            world.spawnParticle(Particle.GLOW, particleLoc, 1, 0.1D, 0.05D, 0.1D, 0.0D);
         }

         if (this.random.nextDouble() < 0.1D) {
            ParticleUtils.electricSpark(loc, 5, size * 0.5D, 0.05D);
         }

         if (this.random.nextDouble() < 0.03D) {
            ParticleUtils.flash(loc);
         }

      }
   }

   private void fireForkedLightning(Player player, final Location from, Location to, IvoryConfig.AbilitySettings settings) {
      final World world = from.getWorld();
      if (world != null) {
         final Location groundTarget = to.clone();
         groundTarget.setY((double)world.getHighestBlockYAt(to) + 0.5D);
         ParticleUtils.playSound(groundTarget, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 1.0F + this.random.nextFloat() * 0.3F);
         (new BukkitRunnable() {
            int tick = 0;
            final List<Location> forkPoints = new ArrayList();

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  double intensity = 1.0D - (double)this.tick / 8.0D;
                  Location current = from.clone();
                  Vector mainDir = groundTarget.toVector().subtract(from.toVector()).normalize();
                  double mainDist = from.distance(groundTarget);
                  double traveled = 0.0D;
                  this.forkPoints.clear();

                  while(traveled < mainDist) {
                     double jag = (StormCallersWrathAbility.this.random.nextDouble() - 0.5D) * 2.0D;
                     current.add(jag * 0.3D, 0.0D, jag * 0.3D);
                     current.add(mainDir.clone().multiply(2));
                     traveled += 2.0D;
                     world.spawnParticle(Particle.ELECTRIC_SPARK, current, (int)(3.0D * intensity), 0.15D, 0.15D, 0.15D, 0.02D);
                     world.spawnParticle(Particle.END_ROD, current, (int)(2.0D * intensity), 0.1D, 0.1D, 0.1D, 0.0D);
                     if (StormCallersWrathAbility.this.random.nextDouble() < 0.15D && traveled < mainDist * 0.7D) {
                        this.forkPoints.add(current.clone());
                     }
                  }

                  Iterator var20 = this.forkPoints.iterator();

                  while(var20.hasNext()) {
                     Location forkStart = (Location)var20.next();
                     double forkAngle = StormCallersWrathAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double forkLength = 3.0D + StormCallersWrathAbility.this.random.nextDouble() * 5.0D;
                     Location forkCurrent = forkStart.clone();

                     for(double f = 0.0D; f < forkLength; f += 0.5D) {
                        double forkJag = StormCallersWrathAbility.this.random.nextDouble() - 0.5D;
                        forkCurrent.add(Math.cos(forkAngle) * 0.5D + forkJag * 0.3D, -0.3D - StormCallersWrathAbility.this.random.nextDouble() * 0.3D, Math.sin(forkAngle) * 0.5D + forkJag * 0.3D);
                        world.spawnParticle(Particle.ELECTRIC_SPARK, forkCurrent, (int)(2.0D * intensity), 0.1D, 0.1D, 0.1D, 0.01D);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         ParticleUtils.flash(groundTarget);
         ParticleUtils.electricSpark(groundTarget, 30, 1.5D, 0.1D);
         ParticleUtils.soulFlame(groundTarget, 20, 1.2D, 0.08D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 0.4D;
                  ParticleUtils.ring(groundTarget, Particle.ELECTRIC_SPARK, radius, (int)(radius * 6.0D) + 5);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.cylinder(player, groundTarget, 2.5D, 3.0D, settings.damagePerTick, settings.damageTickInterval, 10);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void createFinalStormBurst(Player player, Location origin, List<StormCallersWrathAbility.StormCloud> clouds, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.8F);
      Iterator var5 = clouds.iterator();

      while(var5.hasNext()) {
         StormCallersWrathAbility.StormCloud cloud = (StormCallersWrathAbility.StormCloud)var5.next();
         this.fireForkedLightning(player, cloud.location, origin, settings);
         ParticleUtils.whiteAsh(cloud.location, 80, 5.0D, 0.2D);
         ParticleUtils.flash(cloud.location);
      }

      this.runLater(() -> {
         ParticleUtils.flash(origin.clone().add(0.0D, 2.0D, 0.0D));
         ParticleUtils.electricSpark(origin, 120, 8.0D, 0.3D);
         ParticleUtils.soulFlame(origin.clone().add(0.0D, 1.0D, 0.0D), 100, 6.0D, 0.25D);
         ParticleUtils.endRod(origin.clone().add(0.0D, 3.0D, 0.0D), 80, 5.0D, 0.3D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 20) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 1.0D;
                  ParticleUtils.ring(origin, Particle.ELECTRIC_SPARK, radius, (int)(radius * 8.0D) + 15);
                  ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, radius * 0.8D, (int)(radius * 5.0D) + 10);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.sphere(player, origin, settings.radius, settings.damagePerTick * 2.0D, settings.damageTickInterval, 30);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }, 10L);
   }

   private static class StormCloud {
      Location location;
      double angle;
      double size;
      double rotation;

      StormCloud(Location location, double angle, double size) {
         this.location = location;
         this.angle = angle;
         this.size = size;
         this.rotation = 0.0D;
      }
   }
}
