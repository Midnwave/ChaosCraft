package com.blockforge.chaoscraft.weapons.ivory.abilities.impl;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryEffectsManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZone;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZoneManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.ParticleUtils;
import java.util.Iterator;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GravityWellLuminaAbility extends Ability {
   private final Random random = new Random();

   public GravityWellLuminaAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "gravity_well_lumina";
   }

   public String getDisplayName() {
      return "Gravity Well Lumina";
   }

   public String getDescription() {
      return "Create a singularity of brilliant light that draws all enemies toward its crushing center before detonating in a blinding explosion.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      this.getExecutionLocation(player);
      double pullRadius = settings.getDouble("pull-radius", 15.0D);
      double pullStrength = settings.getDouble("pull-strength", 0.5D);
      int durationSeconds = settings.getInt("duration-seconds", 8);
      Location wellLocation = this.getForwardLocation(player, 8.0D);
      ParticleUtils.playSound(wellLocation, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.5F);
      ParticleUtils.playSound(wellLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0F, 0.3F);
      ParticleUtils.playSound(wellLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5F, 0.5F);
      this.createWellFormation(wellLocation, () -> {
         this.createActiveWell(player, wellLocation, pullRadius, pullStrength, durationSeconds, settings);
      });
   }

   private void createWellFormation(final Location loc, final Runnable onComplete) {
      (new BukkitRunnable() {
         int tick = 0;
         final int formTime = 30;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 30) {
               ParticleUtils.flash(loc);
               onComplete.run();
               this.cancel();
            } else {
               World world = loc.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 30.0D;

                  double ringRadius;
                  double ringRotation;
                  double tilt;
                  for(int i = 0; i < 30; ++i) {
                     double theta = GravityWellLuminaAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     ringRadius = GravityWellLuminaAbility.this.random.nextDouble() * 3.141592653589793D;
                     ringRotation = 8.0D * (1.0D - progress);
                     tilt = Math.sin(ringRadius) * Math.cos(theta) * ringRotation;
                     double y = Math.cos(ringRadius) * ringRotation;
                     double z = Math.sin(ringRadius) * Math.sin(theta) * ringRotation;
                     Location particleLoc = loc.clone().add(tilt, y, z);
                     Vector toCenter = loc.toVector().subtract(particleLoc.toVector()).normalize().multiply(0.3D);
                     world.spawnParticle(Particle.END_ROD, particleLoc, 1, toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.1D);
                  }

                  double coreSize = progress * 2.0D;
                  ParticleUtils.sphere(loc, Particle.SOUL_FIRE_FLAME, coreSize, (int)(20.0D * progress) + 5);
                  ParticleUtils.glow(loc, (int)(10.0D * progress), coreSize * 0.5D, 0.0D);

                  for(int ring = 0; ring < 3; ++ring) {
                     ringRadius = 2.0D + (double)ring * 1.5D;
                     ringRotation = this.rotation * (1.0D + (double)ring * 0.3D) * (double)(ring % 2 == 0 ? 1 : -1);
                     tilt = (double)ring * 3.141592653589793D / 6.0D;
                     GravityWellLuminaAbility.this.drawTiltedRing(loc, ringRadius, ringRotation, tilt, Particle.END_ROD, 20 - ring * 4);
                  }

                  if (this.tick % 6 == 0) {
                     ParticleUtils.flash(loc);
                  }

                  if (this.tick % 8 == 0) {
                     ParticleUtils.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5F, (float)(0.5D + progress * 0.5D));
                  }

                  this.rotation += 0.15D;
                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void drawTiltedRing(Location center, double radius, double rotation, double tilt, Particle particle, int points) {
      World world = center.getWorld();
      if (world != null) {
         double angleStep = 6.283185307179586D / (double)points;

         for(int i = 0; i < points; ++i) {
            double angle = (double)i * angleStep + rotation;
            double x = Math.cos(angle) * radius;
            double y = Math.sin(angle) * radius * Math.sin(tilt);
            double z = Math.sin(angle) * radius * Math.cos(tilt);
            Location point = center.clone().add(x, y, z);
            world.spawnParticle(particle, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
         }

      }
   }

   private void createActiveWell(final Player player, final Location loc, final double pullRadius, final double pullStrength, int durationSeconds, final IvoryConfig.AbilitySettings settings) {
      final int durationTicks = durationSeconds * 20;
      ParticleUtils.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 2.0F, 0.3F);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;
         double pulsePhase = 0.0D;

         public void run() {
            if (this.tick >= durationTicks) {
               GravityWellLuminaAbility.this.createWellExplosion(player, loc, pullRadius, settings);
               this.cancel();
            } else {
               World world = loc.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / (double)durationTicks;
                  double intensity = 1.0D + progress * 0.5D;
                  double coreSize = 2.0D + Math.sin(this.pulsePhase) * 0.5D;
                  ParticleUtils.sphere(loc, Particle.SOUL_FIRE_FLAME, coreSize, 30);
                  ParticleUtils.sphere(loc, Particle.END_ROD, coreSize * 0.8D, 20);
                  ParticleUtils.glow(loc, 15, coreSize * 0.6D, 0.0D);
                  GravityWellLuminaAbility.this.drawAccretionDisc(loc, 4.0D, this.rotation);

                  int i;
                  double theta;
                  double distance;
                  for(i = 0; i < 4; ++i) {
                     theta = (double)(3 + i * 2);
                     distance = this.rotation * (0.5D + (double)i * 0.1D) * (double)(i % 2 == 0 ? 1 : -1);
                     ParticleUtils.ring(loc, Particle.END_ROD, theta, 15 + i * 5);
                  }

                  double pullForce;
                  for(i = 0; i < 20; ++i) {
                     theta = GravityWellLuminaAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     distance = GravityWellLuminaAbility.this.random.nextDouble() * 3.141592653589793D;
                     pullForce = pullRadius * (0.5D + GravityWellLuminaAbility.this.random.nextDouble() * 0.5D);
                     double x = Math.sin(distance) * Math.cos(theta) * pullForce;
                     double y = Math.cos(distance) * pullForce * 0.3D;
                     double z = Math.sin(distance) * Math.sin(theta) * pullForce;
                     Location particleLoc = loc.clone().add(x, y, z);
                     Vector toCenter = loc.toVector().subtract(particleLoc.toVector()).normalize().multiply(0.5D);
                     world.spawnParticle(Particle.END_ROD, particleLoc, 1, toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.15D);
                  }

                  Iterator var23 = world.getNearbyEntities(loc, pullRadius, pullRadius, pullRadius).iterator();

                  while(var23.hasNext()) {
                     Entity entity = (Entity)var23.next();
                     if (!(entity instanceof Player) && !(entity instanceof ArmorStand) && entity instanceof LivingEntity) {
                        Vector toWell = loc.toVector().subtract(entity.getLocation().toVector());
                        distance = toWell.length();
                        if (distance > 0.5D) {
                           pullForce = pullStrength * intensity * (1.0D + 3.0D / (distance + 1.0D));
                           pullForce = Math.min(pullForce, 1.5D);
                           toWell.normalize().multiply(pullForce);
                           Vector currentVel = entity.getVelocity();
                           entity.setVelocity(currentVel.add(toWell).multiply(0.5D));
                        }
                     }
                  }

                  if (this.tick % 10 == 0) {
                     DamageZone zone = DamageZone.sphere(player, loc, coreSize + 1.0D, settings.damagePerTick * intensity, settings.damageTickInterval, 12);
                     DamageZoneManager.getInstance(GravityWellLuminaAbility.this.plugin).register(zone);
                  }

                  if (this.tick % 10 == 0) {
                     ParticleUtils.flash(loc);
                  }

                  if (this.tick % 15 == 0) {
                     ParticleUtils.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, 1.0F, 0.5F);
                  }

                  if (this.tick % 30 == 0) {
                     ParticleUtils.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1.0F, 0.3F);
                  }

                  this.rotation += 0.1D;
                  this.pulsePhase += 0.2D;
                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void drawAccretionDisc(Location center, double radius, double rotation) {
      World world = center.getWorld();
      if (world != null) {
         for(int layer = 0; layer < 3; ++layer) {
            double layerRadius = radius * (0.6D + (double)layer * 0.2D);
            double layerRotation = rotation * (1.5D - (double)layer * 0.3D);
            int points = 25 + layer * 10;
            double angleStep = 6.283185307179586D / (double)points;

            for(int i = 0; i < points; ++i) {
               double angle = (double)i * angleStep + layerRotation;
               double x = Math.cos(angle) * layerRadius;
               double z = Math.sin(angle) * layerRadius;
               double y = Math.sin(angle * 3.0D + layerRotation * 2.0D) * 0.3D;
               Location point = center.clone().add(x, y, z);
               if (layer == 0) {
                  world.spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.05D, 0.02D, 0.05D, 0.01D);
               } else {
                  world.spawnParticle(Particle.END_ROD, point, 1, 0.02D, 0.01D, 0.02D, 0.0D);
               }
            }
         }

      }
   }

   private void createWellExplosion(final Player player, final Location loc, final double radius, final IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         ParticleUtils.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0F, 0.3F);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 15) {
                  GravityWellLuminaAbility.this.createMassiveExplosion(player, loc, radius, settings);
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 15.0D;
                  double shrinkRadius = radius * (1.0D - progress);
                  ParticleUtils.sphere(loc, Particle.END_ROD, shrinkRadius, (int)(shrinkRadius * 10.0D));
                  ParticleUtils.sphere(loc, Particle.SOUL_FIRE_FLAME, shrinkRadius * 0.8D, (int)(shrinkRadius * 8.0D));
                  ParticleUtils.glow(loc, 20, 1.0D + progress, 0.0D);
                  ParticleUtils.flash(loc);
                  ParticleUtils.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0F, (float)(0.5D + progress));
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createMassiveExplosion(Player player, final Location loc, double radius, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.4F);
      ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0.8F);
      ParticleUtils.playSound(loc, Sound.ENTITY_WITHER_DEATH, 1.0F, 1.2F);

      for(int i = 0; i < 5; ++i) {
         this.runLater(() -> {
            ParticleUtils.flash(loc);
         }, (long)(i * 2));
      }

      ParticleUtils.soulFlame(loc, 300, radius * 0.8D, 0.5D);
      ParticleUtils.endRod(loc.clone().add(0.0D, 2.0D, 0.0D), 200, radius * 0.6D, 0.6D);
      ParticleUtils.electricSpark(loc, 150, radius, 0.4D);
      ParticleUtils.firework(loc.clone().add(0.0D, 3.0D, 0.0D), 100, radius * 0.5D, 0.7D);
      ParticleUtils.glow(loc.clone().add(0.0D, 1.0D, 0.0D), 80, radius * 0.4D, 0.3D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 25) {
               this.cancel();
            } else {
               double waveRadius = (double)this.tick * 1.2D;
               double intensity = 1.0D - (double)this.tick / 25.0D;
               ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, waveRadius, (int)(waveRadius * 10.0D) + 20);

               for(int h = 0; h < 4; ++h) {
                  double heightRadius = waveRadius * (1.0D - (double)h * 0.15D);
                  if (heightRadius > 0.0D) {
                     Location ringLoc = loc.clone().add(0.0D, (double)(h * 3) * intensity, 0.0D);
                     ParticleUtils.ring(ringLoc, Particle.END_ROD, heightRadius, (int)(heightRadius * 6.0D) + 10);
                  }
               }

               if (this.tick % 3 == 0 && intensity > 0.3D) {
                  ParticleUtils.flash(loc.clone().add(0.0D, (double)this.tick * 0.5D, 0.0D));
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      World world = loc.getWorld();
      if (world != null) {
         Iterator var7 = world.getNearbyEntities(loc, radius * 2.0D, radius * 2.0D, radius * 2.0D).iterator();

         while(var7.hasNext()) {
            Entity entity = (Entity)var7.next();
            if (!(entity instanceof Player) && entity instanceof LivingEntity) {
               Vector knockback = entity.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2.5D);
               knockback.setY(0.8D);
               entity.setVelocity(knockback);
            }
         }
      }

      DamageZone zone = DamageZone.sphere(player, loc, radius * 1.5D, settings.damagePerTick * 4.0D, settings.damageTickInterval, 30);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
