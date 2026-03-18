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

public class TempestOfHeavensAbility extends Ability {
   private final Random random = new Random();

   public TempestOfHeavensAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "tempest_of_heavens";
   }

   public String getDisplayName() {
      return "Tempest of the Heavens";
   }

   public String getDescription() {
      return "Raise your weapon skyward as the sky cracks with divine lightning, raining 25+ devastating strikes that chain between impact points.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      int strikeCount = settings.getInt("strike-count", 25);
      int durationSeconds = settings.getInt("duration-seconds", 12);
      double radius = settings.radius;
      ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.8F);
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THUNDER, 2.0F, 0.6F);
      this.createWeaponRaiseEffect(origin, () -> {
         this.executeTempest(player, origin, strikeCount, durationSeconds, radius, settings);
      });
   }

   private void createWeaponRaiseEffect(final Location origin, final Runnable onComplete) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 30) {
               ParticleUtils.flash(origin.clone().add(0.0D, 30.0D, 0.0D));
               ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0F, 0.3F);
               onComplete.run();
               this.cancel();
            } else {
               double progress = (double)this.tick / 30.0D;
               double height = progress * 20.0D;

               for(double y = 0.0D; y < height; y += 0.5D) {
                  Location beamLoc = origin.clone().add(0.0D, y, 0.0D);
                  ParticleUtils.endRod(beamLoc, 2, 0.2D, 0.02D);
                  ParticleUtils.electricSpark(beamLoc, 1, 0.3D, 0.01D);
               }

               for(int ring = 0; ring < 3; ++ring) {
                  double ringY = height * (double)(ring + 1) / 4.0D;
                  Location ringLoc = origin.clone().add(0.0D, ringY, 0.0D);
                  double ringRadius = 1.0D + (double)ring * 0.5D;
                  TempestOfHeavensAbility.this.drawRotatingRing(ringLoc, ringRadius, this.rotation * (1.0D + (double)ring * 0.3D), 15 + ring * 5);
               }

               if (progress > 0.5D) {
                  Location topLoc = origin.clone().add(0.0D, height, 0.0D);
                  ParticleUtils.electricSpark(topLoc, (int)(20.0D * (progress - 0.5D) * 2.0D), 2.0D, 0.1D);
               }

               if (this.tick % 8 == 0) {
                  ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, (float)(0.5D + progress * 0.5D));
               }

               this.rotation += 0.2D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void drawRotatingRing(Location center, double radius, double rotation, int points) {
      World world = center.getWorld();
      if (world != null) {
         double angleStep = 6.283185307179586D / (double)points;

         for(int i = 0; i < points; ++i) {
            double angle = (double)i * angleStep + rotation;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location point = center.clone().add(x, 0.0D, z);
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
         }

      }
   }

   private void executeTempest(final Player player, final Location origin, final int strikeCount, int durationSeconds, final double radius, final IvoryConfig.AbilitySettings settings) {
      final int durationTicks = durationSeconds * 20;
      final List<Location> strikeLocations = new ArrayList();
      (new BukkitRunnable() {
         int tick = 0;
         int strikesLaunched = 0;
         int strikeInterval = Math.max(1, durationTicks / strikeCount);

         public void run() {
            if (this.tick < durationTicks && this.strikesLaunched < strikeCount) {
               if (this.tick % this.strikeInterval == 0 && this.strikesLaunched < strikeCount) {
                  double angle = TempestOfHeavensAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double dist = TempestOfHeavensAbility.this.random.nextDouble() * radius;
                  double x = Math.cos(angle) * dist;
                  double z = Math.sin(angle) * dist;
                  Location strikeLoc = origin.clone().add(x, 0.0D, z);
                  TempestOfHeavensAbility.this.createLightningStrike(player, strikeLoc, strikeLocations, settings);
                  strikeLocations.add(strikeLoc);
                  ++this.strikesLaunched;
                  if (TempestOfHeavensAbility.this.random.nextDouble() < 0.3D && this.strikesLaunched < strikeCount) {
                     angle = TempestOfHeavensAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     dist = TempestOfHeavensAbility.this.random.nextDouble() * radius;
                     x = Math.cos(angle) * dist;
                     z = Math.sin(angle) * dist;
                     Location strikeLoc2 = origin.clone().add(x, 0.0D, z);
                     TempestOfHeavensAbility.this.runLater(() -> {
                        TempestOfHeavensAbility.this.createLightningStrike(player, strikeLoc2, strikeLocations, settings);
                     }, 3L);
                     strikeLocations.add(strikeLoc2);
                     ++this.strikesLaunched;
                  }
               }

               if (this.tick % 5 == 0) {
                  TempestOfHeavensAbility.this.createStormAmbience(origin, radius);
               }

               if (this.tick % 25 == 0) {
                  ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 0.8F + TempestOfHeavensAbility.this.random.nextFloat() * 0.4F);
               }

               ++this.tick;
            } else {
               TempestOfHeavensAbility.this.createFinalStrike(player, origin, strikeLocations, settings);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createLightningStrike(Player player, Location loc, List<Location> previousStrikes, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         final Location groundLoc = loc.clone();
         groundLoc.setY((double)world.getHighestBlockYAt(loc) + 0.1D);
         world.strikeLightningEffect(groundLoc.clone().add(0.0D, 100.0D, 0.0D));
         this.createCustomLightning(groundLoc);
         ParticleUtils.flash(groundLoc);
         ParticleUtils.electricSpark(groundLoc, 40, 2.0D, 0.15D);
         ParticleUtils.soulFlame(groundLoc, 30, 1.5D, 0.1D);
         ParticleUtils.endRod(groundLoc.clone().add(0.0D, 1.0D, 0.0D), 20, 1.2D, 0.15D);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 1.0F + this.random.nextFloat() * 0.5F);
         ParticleUtils.playSound(groundLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.5F);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_CREEPER_HURT, 0.8F, 0.5F);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 12) {
                  this.cancel();
               } else {
                  double ringRadius = (double)this.tick * 0.6D;
                  ParticleUtils.ring(groundLoc, Particle.SOUL_FIRE_FLAME, ringRadius, (int)(ringRadius * 8.0D) + 8);
                  ParticleUtils.ring(groundLoc.clone().add(0.0D, 0.2D, 0.0D), Particle.ELECTRIC_SPARK, ringRadius * 0.8D, (int)(ringRadius * 6.0D) + 5);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         this.createGroundBurn(groundLoc);
         Iterator var8 = previousStrikes.iterator();

         while(var8.hasNext()) {
            Location prevLoc = (Location)var8.next();
            if (prevLoc.distance(groundLoc) < 15.0D && this.random.nextDouble() < 0.4D) {
               this.createChainLightning(groundLoc, prevLoc);
            }
         }

         DamageZone zone = DamageZone.cylinder(player, groundLoc, 3.0D, 4.0D, settings.damagePerTick, settings.damageTickInterval, 15);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void createCustomLightning(final Location groundLoc) {
      World world = groundLoc.getWorld();
      if (world != null) {
         final Location skyLoc = groundLoc.clone().add(0.0D, 50.0D, 0.0D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  double intensity = 1.0D - (double)this.tick / 8.0D;
                  Location current = skyLoc.clone();
                  double targetY = groundLoc.getY();

                  while(current.getY() > targetY) {
                     double jag = (TempestOfHeavensAbility.this.random.nextDouble() - 0.5D) * 2.0D * intensity;
                     current.add(jag, -3.0D, jag);
                     ParticleUtils.electricSpark(current, (int)(3.0D * intensity), 0.2D, 0.02D);
                     ParticleUtils.endRod(current, (int)(2.0D * intensity), 0.1D, 0.01D);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createGroundBurn(final Location loc) {
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               double intensity = 1.0D - (double)this.tick / 40.0D;
               ParticleUtils.soulFlame(loc, (int)(8.0D * intensity), 1.5D * intensity, 0.02D);
               if (this.tick % 3 == 0) {
                  ParticleUtils.electricSpark(loc, (int)(5.0D * intensity), 1.0D * intensity, 0.03D);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createChainLightning(final Location from, final Location to) {
      final World world = from.getWorld();
      if (world != null) {
         ParticleUtils.playSound(from, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8F, 1.5F);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 5) {
                  this.cancel();
               } else {
                  Vector direction = to.toVector().subtract(from.toVector());
                  double distance = direction.length();
                  direction.normalize();
                  Location current = from.clone().add(0.0D, 1.0D, 0.0D);
                  double traveled = 0.0D;

                  while(traveled < distance) {
                     double jag = (TempestOfHeavensAbility.this.random.nextDouble() - 0.5D) * 1.5D;
                     current.add(jag * 0.3D, jag * 0.1D, jag * 0.3D);
                     current.add(direction.clone().multiply(1.5D));
                     ++traveled;
                     world.spawnParticle(Particle.ELECTRIC_SPARK, current, 2, 0.1D, 0.1D, 0.1D, 0.02D);
                     world.spawnParticle(Particle.END_ROD, current, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createStormAmbience(Location origin, double radius) {
      World world = origin.getWorld();
      if (world != null) {
         Location skyLoc = origin.clone().add((this.random.nextDouble() - 0.5D) * radius * 2.0D, 30.0D + this.random.nextDouble() * 20.0D, (this.random.nextDouble() - 0.5D) * radius * 2.0D);
         ParticleUtils.electricSpark(skyLoc, 10, 3.0D, 0.1D);
         if (this.random.nextDouble() < 0.2D) {
            ParticleUtils.flash(skyLoc);
         }

      }
   }

   private void createFinalStrike(Player player, Location origin, List<Location> strikeLocations, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.0F, 1.5F);
      Iterator var5 = strikeLocations.iterator();

      while(var5.hasNext()) {
         Location loc = (Location)var5.next();
         ParticleUtils.electricSpark(loc, 20, 1.0D, 0.1D);
      }

      this.runLater(() -> {
         ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0F, 0.3F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.5F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);
         World world = origin.getWorld();
         if (world != null) {
            world.strikeLightningEffect(origin.clone().add(0.0D, 100.0D, 0.0D));
         }

         for(int i = 0; i < 5; ++i) {
            ParticleUtils.flash(origin.clone().add(0.0D, (double)(i * 5), 0.0D));
         }

         ParticleUtils.electricSpark(origin, 150, 8.0D, 0.3D);
         ParticleUtils.soulFlame(origin.clone().add(0.0D, 1.0D, 0.0D), 120, 6.0D, 0.25D);
         ParticleUtils.endRod(origin.clone().add(0.0D, 3.0D, 0.0D), 100, 5.0D, 0.3D);
         ParticleUtils.firework(origin.clone().add(0.0D, 5.0D, 0.0D), 80, 4.0D, 0.4D);
         Iterator var8 = strikeLocations.iterator();

         while(var8.hasNext()) {
            Location loc = (Location)var8.next();
            this.createChainLightning(origin, loc);
         }

         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 25) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 1.5D;
                  ParticleUtils.ring(origin, Particle.ELECTRIC_SPARK, radius, (int)(radius * 10.0D) + 20);
                  ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, radius * 0.8D, (int)(radius * 6.0D) + 15);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.sphere(player, origin, settings.radius * 1.5D, settings.damagePerTick * 3.0D, settings.damageTickInterval, 40);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }, 20L);
   }
}
