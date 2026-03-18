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
import org.joml.Vector3f;

public class ZeroPointConvergenceAbility extends Ability {
   private final Random random = new Random();

   public ZeroPointConvergenceAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "zero_point_convergence";
   }

   public String getDisplayName() {
      return "Zero Point Convergence";
   }

   public String getDescription() {
      return "Manifest a point of absolute zero that creates a gravitational singularity, pulling all matter inward before a catastrophic release.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      Location singularityLoc = origin.clone().add(0.0D, 5.0D, 0.0D);
      ParticleUtils.playSound(origin, Sound.BLOCK_END_PORTAL_SPAWN, 2.0F, 0.3F);
      ParticleUtils.playSound(origin, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5F, 0.4F);
      List<BlockDisplay> singularityDisplays = this.createSingularity(singularityLoc);
      this.runLater(() -> {
         this.gravitationalPull(singularityLoc, radius);
      }, 30L);
      this.runLater(() -> {
         this.realityCollapse(player, singularityLoc, origin, radius, settings, singularityDisplays);
      }, 130L);
   }

   private List<BlockDisplay> createSingularity(final Location center) {
      final List<BlockDisplay> displays = new ArrayList();
      World world = center.getWorld();
      if (world == null) {
         return displays;
      } else {
         BlockDisplay core = (BlockDisplay)world.spawn(center, BlockDisplay.class, (d) -> {
            d.setBlock(Material.BLACK_CONCRETE.createBlockData());
            d.setGlowing(false);
            d.setBrightness(new Brightness(0, 0));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.3F, -0.3F, -0.3F), t.getLeftRotation(), new Vector3f(0.6F, 0.6F, 0.6F), t.getRightRotation()));
         });
         displays.add(core);

         for(int i = 0; i < 8; ++i) {
            double angle = 0.7853981633974483D * (double)i;
            double x = Math.cos(angle) * 1.5D;
            double z = Math.sin(angle) * 1.5D;
            Location ringLoc = center.clone().add(x, 0.0D, z);
            BlockDisplay ring = (BlockDisplay)world.spawn(ringLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.CRYING_OBSIDIAN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.15F, -0.15F, -0.15F), t.getLeftRotation(), new Vector3f(0.3F, 0.3F, 0.3F), t.getRightRotation()));
            });
            displays.add(ring);
         }

         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  double formProgress = (double)this.tick / 30.0D;

                  int i;
                  for(i = 1; i < displays.size(); ++i) {
                     BlockDisplay ring = (BlockDisplay)displays.get(i);
                     if (ring.isValid()) {
                        double anglex = 0.7853981633974483D * (double)(i - 1) + this.rotation;
                        double distx = 1.5D * formProgress;
                        double xx = Math.cos(anglex) * distx;
                        double z = Math.sin(anglex) * distx;
                        ring.teleport(center.clone().add(xx, 0.0D, z));
                     }
                  }

                  for(i = 0; i < 20; ++i) {
                     double angle = this.rotation + (double)i * 0.3D;
                     double dist = 5.0D * (1.0D - formProgress) + 1.0D;
                     double x = Math.cos(angle) * dist;
                     double zx = Math.sin(angle) * dist;
                     double y = Math.sin((double)this.tick * 0.2D + (double)i) * 2.0D;
                     Location spiralLoc = center.clone().add(x, y, zx);
                     ParticleUtils.electricSpark(spiralLoc, 1, 0.1D, 0.05D);
                  }

                  ParticleUtils.endRod(center, 5, 0.5D, 0.02D);
                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0F, (float)(0.30000001192092896D + formProgress * 0.30000001192092896D));
                  }

                  this.rotation += 0.15D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         return displays;
      }
   }

   private void gravitationalPull(final Location center, final double radius) {
      ParticleUtils.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5F, 0.3F);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 100) {
               this.cancel();
            } else {
               double intensity = Math.min(1.0D, (double)this.tick / 50.0D);

               int i;
               double diskAngle;
               double diskRadius;
               double x;
               double zx;
               double y;
               for(i = 0; i < (int)(60.0D * intensity); ++i) {
                  diskAngle = ZeroPointConvergenceAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  diskRadius = (ZeroPointConvergenceAbility.this.random.nextDouble() - 0.5D) * 3.141592653589793D;
                  x = radius * (1.0D - ZeroPointConvergenceAbility.this.random.nextDouble() * 0.3D * intensity);
                  zx = Math.cos(diskAngle) * Math.cos(diskRadius) * x;
                  y = Math.sin(diskRadius) * x * 0.5D;
                  double z = Math.sin(diskAngle) * Math.cos(diskRadius) * x;
                  Location particleLoc = center.clone().add(zx, y, z);
                  ParticleUtils.endRod(particleLoc, 1, 0.1D, 0.2D);
               }

               for(i = 0; i < 40; ++i) {
                  diskAngle = this.rotation + (double)i * 3.141592653589793D * 2.0D / 40.0D;
                  diskRadius = 2.0D + ZeroPointConvergenceAbility.this.random.nextDouble() * radius * 0.3D;
                  x = Math.cos(diskAngle) * diskRadius;
                  zx = Math.sin(diskAngle) * diskRadius;
                  y = (ZeroPointConvergenceAbility.this.random.nextDouble() - 0.5D) * 0.5D;
                  Location diskLoc = center.clone().add(x, y, zx);
                  ParticleUtils.soulFlame(diskLoc, 1, 0.15D, 0.01D);
               }

               ParticleUtils.electricSpark(center, (int)(10.0D * intensity), 0.8D, 0.02D);
               double yx;
               if (this.tick % 3 == 0) {
                  yx = 3.0D + Math.sin((double)this.tick * 0.1D) * 0.5D;
                  ParticleUtils.ring(center, Particle.END_ROD, yx, 30);
               }

               if (this.tick > 30) {
                  for(yx = 0.0D; yx < intensity * 8.0D; yx += 0.5D) {
                     ParticleUtils.electricSpark(center.clone().add(0.0D, yx, 0.0D), 1, 0.3D, 0.1D);
                     ParticleUtils.electricSpark(center.clone().add(0.0D, -yx, 0.0D), 1, 0.3D, 0.1D);
                  }
               }

               if (this.tick % 15 == 0) {
                  ParticleUtils.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0F, 0.4F + (float)(intensity * 0.4D));
               }

               if (this.tick % (20 - (int)(intensity * 15.0D)) == 0) {
                  ParticleUtils.flash(center);
               }

               this.rotation += 0.1D + intensity * 0.1D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void realityCollapse(final Player player, final Location singularityLoc, final Location origin, final double radius, final IvoryConfig.AbilitySettings settings, List<BlockDisplay> displays) {
      World world = singularityLoc.getWorld();
      if (world != null) {
         Iterator var9 = displays.iterator();

         while(var9.hasNext()) {
            BlockDisplay d = (BlockDisplay)var9.next();
            if (d.isValid()) {
               d.remove();
            }
         }

         ParticleUtils.playSound(singularityLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0F, 0.2F);
         ParticleUtils.playSound(singularityLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
         ParticleUtils.playSound(singularityLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.4F);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 10) {
                  ZeroPointConvergenceAbility.this.triggerExplosion(player, singularityLoc, origin, radius, settings);
                  this.cancel();
               } else {
                  double implodeRadius = radius * (1.0D - (double)this.tick / 10.0D);

                  for(int i = 0; i < 80; ++i) {
                     double angle = ZeroPointConvergenceAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double elevation = (ZeroPointConvergenceAbility.this.random.nextDouble() - 0.5D) * 3.141592653589793D;
                     double x = Math.cos(angle) * Math.cos(elevation) * implodeRadius;
                     double y = Math.sin(elevation) * implodeRadius * 0.5D;
                     double z = Math.sin(angle) * Math.cos(elevation) * implodeRadius;
                     Location particleLoc = singularityLoc.clone().add(x, y, z);
                     ParticleUtils.endRod(particleLoc, 1, 0.05D, 0.4D);
                  }

                  ParticleUtils.ring(singularityLoc, Particle.SOUL_FIRE_FLAME, implodeRadius, (int)(implodeRadius * 10.0D));
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void triggerExplosion(Player player, final Location singularityLoc, final Location origin, final double radius, IvoryConfig.AbilitySettings settings) {
      for(int i = 0; i < 10; ++i) {
         double angle = this.random.nextDouble() * 3.141592653589793D * 2.0D;
         double dist = this.random.nextDouble() * 5.0D;
         double x = Math.cos(angle) * dist;
         double y = (this.random.nextDouble() - 0.5D) * 4.0D;
         double z = Math.sin(angle) * dist;
         ParticleUtils.flash(singularityLoc.clone().add(x, y, z));
      }

      ParticleUtils.soulFlame(singularityLoc, 500, 10.0D, 0.8D);
      ParticleUtils.endRod(singularityLoc, 400, 8.0D, 0.9D);
      ParticleUtils.electricSpark(singularityLoc, 300, 12.0D, 0.6D);
      ParticleUtils.firework(singularityLoc, 200, 6.0D, 1.0D);
      ParticleUtils.glow(singularityLoc, 150, 7.0D, 0.7D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 50) {
               this.cancel();
            } else {
               double shockRadius = (double)this.tick * 1.8D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, shockRadius, (int)(shockRadius * 12.0D) + 40);
               ParticleUtils.ring(origin.clone().add(0.0D, 1.0D, 0.0D), Particle.END_ROD, shockRadius * 0.95D, (int)(shockRadius * 8.0D) + 25);
               int i;
               double phi;
               double theta;
               double x;
               if (this.tick % 5 == 0) {
                  for(i = 0; i < 6; ++i) {
                     phi = 1.0471975511965976D * (double)i + (double)this.tick * 0.1D;
                     theta = Math.cos(phi) * shockRadius * 0.8D;
                     x = Math.sin(phi) * shockRadius * 0.8D;
                     Location riftBase = origin.clone().add(theta, 0.0D, x);

                     for(double y = 0.0D; y < 10.0D; y += 0.3D) {
                        ParticleUtils.electricSpark(riftBase.clone().add(0.0D, y, 0.0D), 1, 0.2D, 0.02D);
                     }
                  }
               }

               if (this.tick < 30) {
                  for(i = 0; i < 50; ++i) {
                     phi = ZeroPointConvergenceAbility.this.random.nextDouble() * 3.141592653589793D;
                     theta = ZeroPointConvergenceAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     x = shockRadius * Math.sin(phi) * Math.cos(theta);
                     double yx = shockRadius * 0.5D * Math.cos(phi) + 5.0D;
                     double z = shockRadius * Math.sin(phi) * Math.sin(theta);
                     Location shellPoint = origin.clone().add(x, yx, z);
                     ParticleUtils.endRod(shellPoint, 1, 0.3D, 0.0D);
                  }
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 60) {
               this.cancel();
            } else {
               double intensity = 1.0D - (double)this.tick / 60.0D;

               for(int i = 0; i < (int)(30.0D * intensity); ++i) {
                  double angle = ZeroPointConvergenceAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double dist = ZeroPointConvergenceAbility.this.random.nextDouble() * radius * 0.5D;
                  double y = (ZeroPointConvergenceAbility.this.random.nextDouble() - 0.5D) * 6.0D;
                  double x = Math.cos(angle) * dist;
                  double z = Math.sin(angle) * dist;
                  Location voidPoint = singularityLoc.clone().add(x, y, z);
                  ParticleUtils.electricSpark(voidPoint, 1, 0.2D, 0.03D);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 10L, 2L);
      DamageZone zone = DamageZone.sphere(player, singularityLoc, radius * 1.8D, settings.damagePerTick * 5.0D, settings.damageTickInterval, 100);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
