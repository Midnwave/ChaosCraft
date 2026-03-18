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

public class SacredGeometryMetatronsCubeAbility extends Ability {
   private final Random random = new Random();

   public SacredGeometryMetatronsCubeAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "sacred_geometry_metatrons_cube";
   }

   public String getDisplayName() {
      return "Sacred Geometry: Metatron's Cube";
   }

   public String getDescription() {
      return "Manifest the divine Metatron's Cube - a sacred geometric pattern that channels the fundamental forces of creation into devastating power.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      double cubeSize = settings.getDouble("cube-size", 10.0D);
      Location cubeCenter = origin.clone().add(0.0D, 6.0D, 0.0D);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 1.0F);
      ParticleUtils.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
      List<BlockDisplay> geometryDisplays = new ArrayList();
      this.drawSacredGeometry(cubeCenter, cubeSize, geometryDisplays);
      this.runLater(() -> {
         this.activateGeometry(cubeCenter, cubeSize);
      }, 60L);
      this.runLater(() -> {
         this.releaseGeometricPower(player, cubeCenter, origin, cubeSize, settings);
      }, 120L);
      this.runLater(() -> {
         Iterator var1 = geometryDisplays.iterator();

         while(var1.hasNext()) {
            BlockDisplay d = (BlockDisplay)var1.next();
            if (d.isValid()) {
               d.remove();
            }
         }

      }, 200L);
   }

   private void drawSacredGeometry(final Location center, final double size, List<BlockDisplay> displays) {
      World world = center.getWorld();
      if (world != null) {
         final List<Location> circlecenters = new ArrayList();
         circlecenters.add(center.clone());
         double innerRadius = size * 0.3D;

         double angle;
         double x;
         for(int i = 0; i < 6; ++i) {
            angle = 1.0471975511965976D * (double)i;
            x = Math.cos(angle) * innerRadius;
            double z = Math.sin(angle) * innerRadius;
            circlecenters.add(center.clone().add(x, 0.0D, z));
         }

         double outerRadius = size * 0.6D;

         int i;
         for(i = 0; i < 6; ++i) {
            angle = 1.0471975511965976D * (double)i + 0.5235987755982988D;
            x = Math.cos(angle) * outerRadius;
            double z = Math.sin(angle) * outerRadius;
            circlecenters.add(center.clone().add(x, 0.0D, z));
         }

         for(i = 0; i < circlecenters.size(); ++i) {
            final int fi = i;
            Location circleLoc = (Location)circlecenters.get(i);
            BlockDisplay node = (BlockDisplay)world.spawn(circleLoc, BlockDisplay.class, (d) -> {
               d.setBlock(fi == 0 ? Material.SEA_LANTERN.createBlockData() : Material.DIAMOND_BLOCK.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               float nodeSize = fi == 0 ? 0.8F : 0.5F;
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-nodeSize / 2.0F, -nodeSize / 2.0F, -nodeSize / 2.0F), t.getLeftRotation(), new Vector3f(nodeSize, nodeSize, nodeSize), t.getRightRotation()));
            });
            displays.add(node);
         }

         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;
            int circleIndex = 0;
            int lineIndex = 0;

            public void run() {
               if (this.tick >= 60) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 60.0D;
                  double angle;
                  double x;
                  double z;
                  if (this.tick % 5 == 0 && this.circleIndex < circlecenters.size()) {
                     Location circleLoc = (Location)circlecenters.get(this.circleIndex);
                     angle = this.circleIndex == 0 ? size * 0.15D : size * 0.1D;

                     for(x = 0.0D; x < 6.283185307179586D; x += 0.15D) {
                        z = Math.cos(x) * angle;
                        double zx = Math.sin(x) * angle;
                        ParticleUtils.endRod(circleLoc.clone().add(z, 0.0D, zx), 1, 0.05D, 0.0D);
                     }

                     ParticleUtils.playSound(circleLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5F, 1.0F + (float)this.circleIndex * 0.1F);
                     ++this.circleIndex;
                  }

                  int i;
                  if (this.tick > 20 && this.tick % 2 == 0) {
                     i = circlecenters.size() * (circlecenters.size() - 1) / 2;
                     if (this.lineIndex < i) {
                        int from = 0;
                        int to = 0;
                        int lineCount = 0;

                        label51:
                        for(int ix = 0; ix < circlecenters.size(); ++ix) {
                           for(int j = ix + 1; j < circlecenters.size(); ++j) {
                              if (lineCount == this.lineIndex) {
                                 from = ix;
                                 to = j;
                                 break label51;
                              }

                              ++lineCount;
                           }
                        }

                        Location start = (Location)circlecenters.get(from);
                        Location end = (Location)circlecenters.get(to);
                        ParticleUtils.line(start, end, Particle.END_ROD, 0.3D);
                        ++this.lineIndex;
                     }
                  }

                  for(i = 0; i < 12; ++i) {
                     angle = this.rotation + (double)i * 3.141592653589793D * 2.0D / 12.0D;
                     x = Math.cos(angle) * size * 0.7D;
                     z = Math.sin(angle) * size * 0.7D;
                     ParticleUtils.soulFlame(center.clone().add(x, 0.0D, z), 1, 0.1D, 0.01D);
                  }

                  ParticleUtils.glow(center, 5, 0.5D, 0.02D);
                  this.rotation += 0.05D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void activateGeometry(final Location center, final double size) {
      ParticleUtils.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0F, 0.6F);
      ParticleUtils.playSound(center, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1.5F, 0.5F);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 60) {
               this.cancel();
            } else {
               double intensity = (double)this.tick / 60.0D;

               for(int layer = 0; layer < 3; ++layer) {
                  double layerRadius = size * 0.2D * (double)(layer + 1);

                  for(int i = 0; i < 6; ++i) {
                     double angle = 1.0471975511965976D * (double)i + this.rotation * (double)(layer % 2 == 0 ? 1 : -1);
                     double xx = Math.cos(angle) * layerRadius;
                     double z = Math.sin(angle) * layerRadius;
                     ParticleUtils.electricSpark(center.clone().add(xx, 0.0D, z), (int)(3.0D * intensity), 0.2D, 0.02D);
                  }
               }

               double innerRadius = size * 0.3D;
               double outerRadius = size * 0.6D;

               double vortexAngle;
               double vortexRadius;
               double x;
               int ix;
               for(ix = 0; ix < 6; ++ix) {
                  vortexAngle = 1.0471975511965976D * (double)ix;
                  vortexRadius = 1.0471975511965976D * (double)ix + 0.5235987755982988D;
                  Location inner = center.clone().add(Math.cos(vortexAngle) * innerRadius, 0.0D, Math.sin(vortexAngle) * innerRadius);
                  Location outer = center.clone().add(Math.cos(vortexRadius) * outerRadius, 0.0D, Math.sin(vortexRadius) * outerRadius);
                  x = (double)this.tick * 0.1D % 1.0D;
                  Location pulseLoc = inner.clone().add(outer.toVector().subtract(inner.toVector()).multiply(x));
                  ParticleUtils.soulFlame(pulseLoc, 3, 0.15D, 0.02D);
               }

               for(ix = 0; ix < (int)(10.0D * intensity); ++ix) {
                  vortexAngle = this.rotation * 2.0D + (double)ix * 0.3D;
                  vortexRadius = (1.0D - (double)ix / 10.0D) * size * 0.3D;
                  double vortexY = (double)ix * 0.2D;
                  x = Math.cos(vortexAngle) * vortexRadius;
                  double zx = Math.sin(vortexAngle) * vortexRadius;
                  ParticleUtils.endRod(center.clone().add(x, vortexY, zx), 1, 0.1D, 0.01D);
               }

               if (this.tick % (15 - (int)(intensity * 10.0D)) == 0) {
                  ParticleUtils.flash(center);
                  ParticleUtils.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0F, 0.8F + (float)intensity * 0.5F);
               }

               this.rotation += 0.08D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void releaseGeometricPower(Player player, final Location center, final Location origin, final double size, IvoryConfig.AbilitySettings settings) {
      World world = center.getWorld();
      if (world != null) {
         ParticleUtils.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.8F);
         ParticleUtils.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
         ParticleUtils.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.5F);
         ParticleUtils.flash(center);
         final double innerRadius = size * 0.3D;
         final double outerRadius = size * 0.6D;

         for(int i = 0; i < 6; ++i) {
            double innerAngle = 1.0471975511965976D * (double)i;
            double outerAngle = innerAngle + 0.5235987755982988D;
            ParticleUtils.flash(center.clone().add(Math.cos(innerAngle) * innerRadius, 0.0D, Math.sin(innerAngle) * innerRadius));
            ParticleUtils.flash(center.clone().add(Math.cos(outerAngle) * outerRadius, 0.0D, Math.sin(outerAngle) * outerRadius));
         }

         ParticleUtils.soulFlame(center, 200, size * 0.5D, 0.4D);
         ParticleUtils.endRod(center, 150, size * 0.4D, 0.5D);
         ParticleUtils.electricSpark(center, 100, size * 0.8D, 0.3D);
         ParticleUtils.firework(center, 80, size * 0.3D, 0.6D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  double angle;
                  for(int i = 0; i < 6; ++i) {
                     double innerAngle = 1.0471975511965976D * (double)i;
                     angle = innerAngle + 0.5235987755982988D;
                     Location innerNode = center.clone().add(Math.cos(innerAngle) * innerRadius, 0.0D, Math.sin(innerAngle) * innerRadius);
                     Location outerNode = center.clone().add(Math.cos(angle) * outerRadius, 0.0D, Math.sin(angle) * outerRadius);

                     for(double y = 0.0D; y > -6.0D; y -= 0.5D) {
                        ParticleUtils.soulFlame(innerNode.clone().add(0.0D, y, 0.0D), 2, 0.2D, 0.02D);
                        ParticleUtils.endRod(outerNode.clone().add(0.0D, y, 0.0D), 2, 0.15D, 0.02D);
                     }
                  }

                  double shockRadius = (double)this.tick * 1.5D;

                  for(int ix = 0; ix < 6; ++ix) {
                     angle = 1.0471975511965976D * (double)ix + (double)this.tick * 0.1D;
                     double nextAngle = 1.0471975511965976D * (double)(ix + 1) + (double)this.tick * 0.1D;
                     Location start = origin.clone().add(Math.cos(angle) * shockRadius, 0.5D, Math.sin(angle) * shockRadius);
                     Location end = origin.clone().add(Math.cos(nextAngle) * shockRadius, 0.5D, Math.sin(nextAngle) * shockRadius);
                     ParticleUtils.line(start, end, Particle.SOUL_FIRE_FLAME, 0.3D);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 80) {
                  this.cancel();
               } else {
                  double intensity = 1.0D - (double)this.tick / 80.0D;
                  ParticleUtils.glow(origin, (int)(5.0D * intensity), 0.3D, 0.0D);

                  int ix;
                  double anglex;
                  for(ix = 1; ix <= 3; ++ix) {
                     anglex = size * 0.15D * (double)ix;

                     for(int i = 0; i < 6; ++i) {
                        double angle = 1.0471975511965976D * (double)i;
                        double x = Math.cos(angle) * anglex;
                        double z = Math.sin(angle) * anglex;
                        if (SacredGeometryMetatronsCubeAbility.this.random.nextDouble() < intensity * 0.3D) {
                           ParticleUtils.endRod(origin.clone().add(x, 0.1D, z), 1, 0.1D, 0.0D);
                        }
                     }
                  }

                  if (this.tick % 5 == 0) {
                     for(ix = 0; ix < 6; ++ix) {
                        anglex = 1.0471975511965976D * (double)ix;
                        double nextAngle = 1.0471975511965976D * (double)((ix + 1) % 6);
                        Location start = origin.clone().add(Math.cos(anglex) * size * 0.3D, 0.1D, Math.sin(anglex) * size * 0.3D);
                        Location end = origin.clone().add(Math.cos(nextAngle) * size * 0.3D, 0.1D, Math.sin(nextAngle) * size * 0.3D);
                        ParticleUtils.line(start, end, Particle.END_ROD, 0.5D);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 10L, 2L);
         DamageZone zone = DamageZone.cylinder(player, origin, settings.radius * 1.2D, 12.0D, settings.damagePerTick * 3.0D, settings.damageTickInterval, 80);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }
}
