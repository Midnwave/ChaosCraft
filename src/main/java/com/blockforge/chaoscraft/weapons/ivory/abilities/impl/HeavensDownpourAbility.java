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

public class HeavensDownpourAbility extends Ability {
   private final Random random = new Random();

   public HeavensDownpourAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "heavens_downpour";
   }

   public String getDisplayName() {
      return "Heaven's Downpour";
   }

   public String getDescription() {
      return "Call upon the heavens to unleash a devastating rain of one hundred arrows of pure divine light upon your enemies.";
   }

   public void execute(final Player player) {
      final IvoryConfig.AbilitySettings settings = this.getSettings();
      final Location origin = this.getExecutionLocation(player);
      final int arrowCount = settings.getInt("arrow-count", 100);
      int durationSeconds = settings.getInt("duration-seconds", 10);
      final double radius = settings.radius;
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 2.0F, 1.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.6F);
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THUNDER, 1.5F, 1.2F);
      this.createSkyCharge(origin, radius);
      final int durationTicks = durationSeconds * 20;
      final int arrowsPerWave = Math.max(1, arrowCount / (durationTicks / 3));
      (new BukkitRunnable() {
         int tick = 0;
         int arrowsSpawned = 0;

         public void run() {
            if (this.tick < durationTicks && this.arrowsSpawned < arrowCount) {
               if (this.tick % 3 == 0) {
                  for(int i = 0; i < arrowsPerWave && this.arrowsSpawned < arrowCount; ++i) {
                     double angle = HeavensDownpourAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double dist = HeavensDownpourAbility.this.random.nextDouble() * radius;
                     double x = Math.cos(angle) * dist;
                     double z = Math.sin(angle) * dist;
                     Location targetLoc = origin.clone().add(x, 0.0D, z);
                     HeavensDownpourAbility.this.spawnLightArrow(player, targetLoc, settings);
                     ++this.arrowsSpawned;
                  }
               }

               if (this.tick % 10 == 0) {
                  HeavensDownpourAbility.this.createSkyFlash(origin, radius);
               }

               if (this.tick % 20 == 0) {
                  ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0F, 0.8F);
               }

               ++this.tick;
            } else {
               HeavensDownpourAbility.this.createFinalBarrage(player, origin, radius, settings);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 40L, 1L);
   }

   private void createSkyCharge(final Location origin, final double radius) {
      final Location skyCenter = origin.clone().add(0.0D, 35.0D, 0.0D);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 40) {
               ParticleUtils.flash(skyCenter);
               ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 1.5F);
               this.cancel();
            } else {
               World world = skyCenter.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 40.0D;
                  double currentRadius = radius * 1.5D * (1.0D - progress * 0.5D);

                  int ring;
                  double ringRadius;
                  double ringRotation;
                  double angle;
                  for(ring = 0; ring < 3; ++ring) {
                     ringRadius = currentRadius * (1.0D - (double)ring * 0.25D);
                     ringRotation = this.rotation * (1.0D + (double)ring * 0.3D);
                     int points = 50 - ring * 10;

                     for(int i = 0; i < points; ++i) {
                        angle = 6.283185307179586D / (double)points * (double)i + ringRotation;
                        double x = Math.cos(angle) * ringRadius;
                        double z = Math.sin(angle) * ringRadius;
                        Location point = skyCenter.clone().add(x, (double)(ring * 2), z);
                        if (ring == 0) {
                           world.spawnParticle(Particle.END_ROD, point, 1, 0.1D, 0.1D, 0.1D, 0.0D);
                        } else {
                           world.spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.1D, 0.1D, 0.1D, 0.0D);
                        }
                     }
                  }

                  for(ring = 0; ring < 20; ++ring) {
                     ringRadius = HeavensDownpourAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     ringRotation = currentRadius * (1.0D + HeavensDownpourAbility.this.random.nextDouble() * 0.5D);
                     double xx = Math.cos(ringRadius) * ringRotation;
                     angle = Math.sin(ringRadius) * ringRotation;
                     Location startLoc = skyCenter.clone().add(xx, HeavensDownpourAbility.this.random.nextDouble() * 5.0D, angle);
                     Vector toCenter = skyCenter.toVector().subtract(startLoc.toVector()).normalize().multiply(0.5D);
                     world.spawnParticle(Particle.END_ROD, startLoc, 1, toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.1D);
                  }

                  if (this.tick % 5 == 0) {
                     ParticleUtils.electricSpark(skyCenter, 15, currentRadius * 0.3D, 0.1D);
                  }

                  if (this.tick % 8 == 0) {
                     ParticleUtils.flash(skyCenter);
                  }

                  this.rotation += 0.15D;
                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spawnLightArrow(final Player player, final Location targetLoc, final IvoryConfig.AbilitySettings settings) {
      World world = targetLoc.getWorld();
      if (world != null) {
         double offsetX = (this.random.nextDouble() - 0.5D) * 5.0D;
         double offsetZ = (this.random.nextDouble() - 0.5D) * 5.0D;
         final Location spawnLoc = targetLoc.clone().add(offsetX, 40.0D + this.random.nextDouble() * 10.0D, offsetZ);
         final Vector direction = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();
         final double speed = 2.5D + this.random.nextDouble() * 1.5D;
         if (this.random.nextDouble() < 0.3D) {
            ParticleUtils.playSound(spawnLoc, Sound.ENTITY_ARROW_SHOOT, 0.5F, 1.5F);
         }

         (new BukkitRunnable() {
            Location currentLoc = spawnLoc.clone();
            int tick = 0;
            final int maxTicks = 30;

            public void run() {
               if (this.tick < 30 && !(this.currentLoc.getY() <= targetLoc.getY() + 1.0D)) {
                  this.currentLoc.add(direction.clone().multiply(speed));
                  ParticleUtils.endRod(this.currentLoc, 3, 0.1D, 0.0D);
                  ParticleUtils.glow(this.currentLoc, 2, 0.05D, 0.0D);
                  Location trailLoc = this.currentLoc.clone();
                  Vector trailDir = direction.clone().multiply(-1);

                  for(int i = 0; i < 5; ++i) {
                     trailLoc.add(trailDir.clone().multiply(0.4D));
                     double fade = 1.0D - (double)i / 5.0D;
                     if (fade > 0.5D) {
                        ParticleUtils.soulFlame(trailLoc, 1, 0.02D, 0.0D);
                     }

                     ParticleUtils.endRod(trailLoc, 1, 0.01D, 0.0D);
                  }

                  ++this.tick;
               } else {
                  HeavensDownpourAbility.this.createArrowImpact(player, this.currentLoc, settings);
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createArrowImpact(Player player, Location loc, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         Location groundLoc = loc.clone();
         groundLoc.setY(Math.max((double)world.getHighestBlockYAt(loc) + 0.1D, loc.getY() - 5.0D));
         if (this.random.nextDouble() < 0.2D) {
            ParticleUtils.playSound(groundLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5F, 1.5F);
         }

         ParticleUtils.soulFlame(groundLoc, 8, 0.3D, 0.05D);
         ParticleUtils.endRod(groundLoc, 5, 0.2D, 0.08D);
         ParticleUtils.electricSpark(groundLoc, 3, 0.4D, 0.02D);
         if (this.random.nextDouble() < 0.1D) {
            ParticleUtils.flash(groundLoc);
         }

         double ringRadius = 0.5D;
         ParticleUtils.ring(groundLoc, Particle.END_ROD, ringRadius, 8);
         DamageZone zone = DamageZone.sphere(player, groundLoc, 1.5D, settings.damagePerTick, settings.damageTickInterval, 10);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void createSkyFlash(Location origin, double radius) {
      Location skyLoc = origin.clone().add(0.0D, 30.0D, 0.0D);
      ParticleUtils.flash(skyLoc);
      World world = origin.getWorld();
      if (world != null) {
         for(int i = 0; i < 5; ++i) {
            double angle = this.random.nextDouble() * 3.141592653589793D * 2.0D;
            double dist = this.random.nextDouble() * radius;
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            Location streakStart = skyLoc.clone().add(x, this.random.nextDouble() * 5.0D, z);
            Location streakEnd = streakStart.clone().add(0.0D, -10.0D - this.random.nextDouble() * 10.0D, 0.0D);
            ParticleUtils.line(streakStart, streakEnd, Particle.END_ROD, 0.5D);
         }

      }
   }

   private void createFinalBarrage(final Player player, final Location origin, final double radius, final IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.0F, 1.5F);
      (new BukkitRunnable() {
         int wave = 0;

         public void run() {
            if (this.wave >= 3) {
               HeavensDownpourAbility.this.createFinalImpact(player, origin, radius, settings);
               this.cancel();
            } else {
               int arrowsInRing = 15;
               double ringRadius = radius * (0.3D + (double)this.wave * 0.3D);

               for(int i = 0; i < arrowsInRing; ++i) {
                  double angle = 6.283185307179586D / (double)arrowsInRing * (double)i;
                  double x = Math.cos(angle) * ringRadius;
                  double z = Math.sin(angle) * ringRadius;
                  Location targetLoc = origin.clone().add(x, 0.0D, z);
                  HeavensDownpourAbility.this.spawnLightArrow(player, targetLoc, settings);
               }

               ParticleUtils.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5F, 0.5F);
               ++this.wave;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 10L);
   }

   private void createFinalImpact(Player player, final Location origin, double radius, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.5F);
      ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.8F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.6F);

      for(int i = 0; i < 5; ++i) {
         double x = (this.random.nextDouble() - 0.5D) * radius;
         double z = (this.random.nextDouble() - 0.5D) * radius;
         ParticleUtils.flash(origin.clone().add(x, 2.0D, z));
      }

      ParticleUtils.soulFlame(origin.clone().add(0.0D, 2.0D, 0.0D), 200, radius * 0.5D, 0.3D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 3.0D, 0.0D), 150, radius * 0.4D, 0.4D);
      ParticleUtils.electricSpark(origin.clone().add(0.0D, 1.0D, 0.0D), 100, radius * 0.6D, 0.2D);
      ParticleUtils.firework(origin.clone().add(0.0D, 5.0D, 0.0D), 80, radius * 0.3D, 0.5D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 20) {
               this.cancel();
            } else {
               double waveRadius = (double)this.tick * 1.5D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, waveRadius, (int)(waveRadius * 8.0D) + 15);
               ParticleUtils.ring(origin.clone().add(0.0D, 0.5D, 0.0D), Particle.END_ROD, waveRadius * 0.9D, (int)(waveRadius * 5.0D) + 10);
               ParticleUtils.whiteAsh(origin, 30, waveRadius * 0.5D, 0.1D);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.cylinder(player, origin, radius, 10.0D, settings.damagePerTick * 2.0D, settings.damageTickInterval, 40);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
