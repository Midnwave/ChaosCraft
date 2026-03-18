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
import org.joml.Vector3f;

public class StarfallConvergenceAbility extends Ability {
   private final Random random = new Random();

   public StarfallConvergenceAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "starfall_convergence";
   }

   public String getDisplayName() {
      return "Starfall Convergence";
   }

   public String getDescription() {
      return "Command the stars themselves to converge on a single point, building to a catastrophic release of cosmic energy.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      int starCount = settings.getInt("star-count", 30);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.8F);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 1.5F, 1.5F);
      Location convergencePoint = origin.clone().add(0.0D, 15.0D, 0.0D);
      List<StarfallConvergenceAbility.ConvergingStar> stars = this.spawnStars(convergencePoint, radius, starCount);
      this.convergeStars(stars, convergencePoint, () -> {
         this.buildSingularity(convergencePoint, () -> {
            this.catastrophicRelease(player, origin, convergencePoint, radius, settings);
         });
      });
   }

   private List<StarfallConvergenceAbility.ConvergingStar> spawnStars(Location convergencePoint, double radius, int count) {
      List<StarfallConvergenceAbility.ConvergingStar> stars = new ArrayList();
      World world = convergencePoint.getWorld();
      if (world == null) {
         return stars;
      } else {
         for(int i = 0; i < count; ++i) {
            double theta = this.random.nextDouble() * 3.141592653589793D * 2.0D;
            double phi = this.random.nextDouble() * 3.141592653589793D;
            double dist = radius + this.random.nextDouble() * 20.0D;
            double x = Math.sin(phi) * Math.cos(theta) * dist;
            double y = Math.cos(phi) * dist;
            double z = Math.sin(phi) * Math.sin(theta) * dist;
            Location starLoc = convergencePoint.clone().add(x, y, z);
            double starSize = 0.3D + this.random.nextDouble() * 0.3D;
            BlockDisplay display = (BlockDisplay)world.spawn(starLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.SEA_LANTERN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f((float)(-starSize / 2.0D), (float)(-starSize / 2.0D), (float)(-starSize / 2.0D)), t.getLeftRotation(), new Vector3f((float)starSize, (float)starSize, (float)starSize), t.getRightRotation()));
            });
            ParticleUtils.endRod(starLoc, 5, 0.3D, 0.1D);
            stars.add(new StarfallConvergenceAbility.ConvergingStar(starLoc, display, starSize));
         }

         ParticleUtils.playSound(convergencePoint, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5F, 0.6F);
         return stars;
      }
   }

   private void convergeStars(final List<StarfallConvergenceAbility.ConvergingStar> stars, final Location convergencePoint, final Runnable onComplete) {
      (new BukkitRunnable() {
         int tick = 0;
         final int convergeDuration = 60;

         public void run() {
            if (this.tick >= 60) {
               Iterator var8 = stars.iterator();

               while(var8.hasNext()) {
                  StarfallConvergenceAbility.ConvergingStar starx = (StarfallConvergenceAbility.ConvergingStar)var8.next();
                  if (starx.display.isValid()) {
                     starx.display.remove();
                  }
               }

               onComplete.run();
               this.cancel();
            } else {
               double progress = (double)this.tick / 60.0D;
               double easeProgress = progress * progress;
               Iterator var5 = stars.iterator();

               while(var5.hasNext()) {
                  StarfallConvergenceAbility.ConvergingStar star = (StarfallConvergenceAbility.ConvergingStar)var5.next();
                  if (star.display.isValid()) {
                     Location currentLoc = star.startLoc.clone().add(convergencePoint.toVector().subtract(star.startLoc.toVector()).multiply(easeProgress));
                     star.display.teleport(currentLoc);
                     ParticleUtils.endRod(currentLoc, 2, star.size * 0.5D, 0.02D);
                     if (this.tick % 3 == 0) {
                        ParticleUtils.soulFlame(currentLoc, 1, star.size * 0.3D, 0.01D);
                     }
                  }
               }

               int glowParticles = (int)(progress * 20.0D);
               ParticleUtils.soulFlame(convergencePoint, glowParticles, 0.5D + progress * 1.5D, 0.03D);
               ParticleUtils.glow(convergencePoint, glowParticles / 2, 0.3D + progress, 0.01D);
               if (this.tick % 15 == 0) {
                  ParticleUtils.playSound(convergencePoint, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0F, 0.5F + (float)progress * 0.5F);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void buildSingularity(final Location loc, final Runnable onComplete) {
      World world = loc.getWorld();
      if (world != null) {
         ParticleUtils.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 2.0F, 0.3F);
         final BlockDisplay singularity = (BlockDisplay)world.spawn(loc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.25F, -0.25F, -0.25F), t.getLeftRotation(), new Vector3f(0.5F, 0.5F, 0.5F), t.getRightRotation()));
         });
         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;
            final int buildDuration = 40;

            public void run() {
               if (this.tick >= 40) {
                  if (singularity.isValid()) {
                     singularity.remove();
                  }

                  onComplete.run();
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 40.0D;
                  double scale = 0.5D + progress * 2.5D;
                  if (singularity.isValid()) {
                     Transformation t = singularity.getTransformation();
                     singularity.setTransformation(new Transformation(new Vector3f((float)(-scale / 2.0D), (float)(-scale / 2.0D), (float)(-scale / 2.0D)), t.getLeftRotation(), new Vector3f((float)scale, (float)scale, (float)scale), t.getRightRotation()));
                  }

                  for(int ring = 0; ring < 3; ++ring) {
                     double ringRadius = scale * (0.5D + (double)ring * 0.3D);
                     double ringRotation = this.rotation * (1.0D + (double)ring * 0.3D) * (double)(ring % 2 == 0 ? 1 : -1);

                     for(int i = 0; i < 15; ++i) {
                        double angle = 0.41887902047863906D * (double)i + ringRotation;
                        double x = Math.cos(angle) * ringRadius;
                        double z = Math.sin(angle) * ringRadius;
                        Location ringPoint = loc.clone().add(x, (double)ring * 0.3D - 0.3D, z);
                        ParticleUtils.soulFlame(ringPoint, 1, 0.05D, 0.0D);
                     }
                  }

                  if (this.tick % 5 == 0) {
                     ParticleUtils.electricSpark(loc, 15, scale, 0.1D);
                  }

                  if (this.tick % 10 == 0) {
                     ParticleUtils.flash(loc);
                  }

                  ParticleUtils.glow(loc, (int)(progress * 30.0D), scale * 0.5D, 0.02D);
                  if (this.tick % 8 == 0) {
                     ParticleUtils.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1.5F, 0.3F + (float)progress * 0.7F);
                  }

                  this.rotation += 0.15D + progress * 0.2D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void catastrophicRelease(final Player player, final Location origin, final Location convergencePoint, final double radius, final IvoryConfig.AbilitySettings settings) {
      World world = convergencePoint.getWorld();
      if (world != null) {
         ParticleUtils.playSound(convergencePoint, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
         ParticleUtils.playSound(convergencePoint, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
         ParticleUtils.playSound(convergencePoint, Sound.ENTITY_WITHER_DEATH, 1.5F, 0.8F);

         for(int i = 0; i < 5; ++i) {
            ParticleUtils.flash(convergencePoint.clone().add(0.0D, (double)(i * 2), 0.0D));
         }

         ParticleUtils.soulFlame(convergencePoint, 250, 8.0D, 0.5D);
         ParticleUtils.endRod(convergencePoint, 180, 7.0D, 0.6D);
         ParticleUtils.firework(convergencePoint, 120, 6.0D, 0.7D);
         ParticleUtils.electricSpark(convergencePoint, 100, 10.0D, 0.4D);
         (new BukkitRunnable() {
            int tick = 0;
            List<Location> shardLocations = new ArrayList();
            List<Vector> shardDirections = new ArrayList();

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  if (this.tick == 0) {
                     for(int i = 0; i < 50; ++i) {
                        this.shardLocations.add(convergencePoint.clone());
                        double theta = StarfallConvergenceAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                        double phi = StarfallConvergenceAbility.this.random.nextDouble() * 3.141592653589793D;
                        this.shardDirections.add(new Vector(Math.sin(phi) * Math.cos(theta), Math.cos(phi), Math.sin(phi) * Math.sin(theta)));
                     }
                  }

                  double speed = 1.5D + (double)this.tick * 0.1D;

                  for(int ix = 0; ix < this.shardLocations.size(); ++ix) {
                     Location shardLoc = (Location)this.shardLocations.get(ix);
                     shardLoc.add(((Vector)this.shardDirections.get(ix)).clone().multiply(speed));
                     double fade = 1.0D - (double)this.tick / 40.0D;
                     if (StarfallConvergenceAbility.this.random.nextDouble() < fade) {
                        ParticleUtils.endRod(shardLoc, 2, 0.2D, 0.02D);
                        ParticleUtils.soulFlame(shardLoc, 1, 0.15D, 0.01D);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  double shellRadius = (double)this.tick * 1.5D;

                  int i;
                  double angle;
                  double phi;
                  for(i = -3; i <= 3; ++i) {
                     angle = 1.0D - (double)Math.abs(i) * 0.2D;
                     if (angle > 0.0D) {
                        phi = shellRadius * angle;
                        Location ringCenter = convergencePoint.clone().add(0.0D, (double)(i * 2), 0.0D);
                        ParticleUtils.ring(ringCenter, Particle.SOUL_FIRE_FLAME, phi, (int)(phi * 6.0D) + 10);
                     }
                  }

                  for(i = 0; i < 4; ++i) {
                     angle = 1.5707963267948966D * (double)i + (double)this.tick * 0.1D;

                     for(phi = 0.0D; phi < 3.141592653589793D; phi += 0.3D) {
                        double x = Math.cos(angle) * Math.sin(phi) * shellRadius;
                        double y = Math.cos(phi) * shellRadius;
                        double z = Math.sin(angle) * Math.sin(phi) * shellRadius;
                        Location point = convergencePoint.clone().add(x, y, z);
                        ParticleUtils.endRod(point, 1, 0.1D, 0.0D);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 15) {
                  StarfallConvergenceAbility.this.createGroundImpact(player, origin, radius, settings);
                  this.cancel();
               } else {
                  double height = (double)(15 - this.tick);
                  Location impactLevel = origin.clone().add(0.0D, height, 0.0D);
                  ParticleUtils.ring(impactLevel, Particle.SOUL_FIRE_FLAME, radius * 0.8D, (int)(radius * 10.0D) + 20);

                  for(int i = 0; i < 20; ++i) {
                     double angle = StarfallConvergenceAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double dist = StarfallConvergenceAbility.this.random.nextDouble() * radius;
                     Location rainLoc = impactLevel.clone().add(Math.cos(angle) * dist, StarfallConvergenceAbility.this.random.nextDouble() * 3.0D, Math.sin(angle) * dist);
                     ParticleUtils.endRod(rainLoc, 1, 0.2D, 0.15D);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 5L, 1L);
      }
   }

   private void createGroundImpact(Player player, final Location origin, double radius, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.4F);
      ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0F, 0.5F);
      ParticleUtils.flash(origin);
      ParticleUtils.soulFlame(origin, 150, radius, 0.3D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 1.0D, 0.0D), 100, radius * 0.8D, 0.35D);
      ParticleUtils.firework(origin.clone().add(0.0D, 2.0D, 0.0D), 70, radius * 0.6D, 0.4D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 25) {
               this.cancel();
            } else {
               double shockRadius = (double)this.tick * 1.2D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, shockRadius, (int)(shockRadius * 10.0D) + 20);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 50) {
               this.cancel();
            } else {
               double height = (double)Math.min(this.tick, 30);
               double intensity = 1.0D - (double)Math.max(0, this.tick - 30) / 20.0D;

               for(double y = 0.0D; y < height; y += 0.5D) {
                  Location pillarLoc = origin.clone().add(0.0D, y, 0.0D);
                  if (StarfallConvergenceAbility.this.random.nextDouble() < intensity) {
                     ParticleUtils.endRod(pillarLoc, 1, 0.3D, 0.02D);
                  }
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.cylinder(player, origin, radius, 20.0D, settings.damagePerTick * 3.0D, settings.damageTickInterval, 60);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }

   private static class ConvergingStar {
      Location startLoc;
      BlockDisplay display;
      double size;

      ConvergingStar(Location startLoc, BlockDisplay display, double size) {
         this.startLoc = startLoc.clone();
         this.display = display;
         this.size = size;
      }
   }
}
