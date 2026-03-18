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

public class ConstellationManifestationAbility extends Ability {
   private final Random random = new Random();

   public ConstellationManifestationAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "constellation_manifestation";
   }

   public String getDisplayName() {
      return "Constellation Manifestation";
   }

   public String getDescription() {
      return "Manifest ancient celestial constellations that ignite with divine power and rain coordinated destruction upon your enemies.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 1.2F);
      ParticleUtils.playSound(origin, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1.5F, 0.6F);
      List<ConstellationManifestationAbility.Constellation> constellations = new ArrayList();
      constellations.add(this.createTriangleConstellation(origin.clone().add(0.0D, 25.0D, -10.0D)));
      constellations.add(this.createSquareConstellation(origin.clone().add(-15.0D, 25.0D, 0.0D)));
      constellations.add(this.createCrossConstellation(origin.clone().add(15.0D, 25.0D, 0.0D)));
      this.formConstellations(constellations);
      this.runLater(() -> {
         this.connectConstellations(constellations);
      }, 40L);
      this.runLater(() -> {
         this.fireFromConstellations(player, constellations, origin, settings);
      }, 80L);
      this.runLater(() -> {
         this.cleanupConstellations(constellations);
      }, 200L);
   }

   private ConstellationManifestationAbility.Constellation createTriangleConstellation(Location center) {
      List<Location> stars = new ArrayList();
      double size = 5.0D;

      for(int i = 0; i < 3; ++i) {
         double angle = 2.0943951023931953D * (double)i - 1.5707963267948966D;
         double x = Math.cos(angle) * size;
         double z = Math.sin(angle) * size;
         stars.add(center.clone().add(x, 0.0D, z));
      }

      int[][] connections = new int[][]{{0, 1}, {1, 2}, {2, 0}};
      return new ConstellationManifestationAbility.Constellation("Triangle", center, stars, connections);
   }

   private ConstellationManifestationAbility.Constellation createSquareConstellation(Location center) {
      List<Location> stars = new ArrayList();
      double size = 4.0D;
      stars.add(center.clone().add(-size, 0.0D, -size));
      stars.add(center.clone().add(size, 0.0D, -size));
      stars.add(center.clone().add(size, 0.0D, size));
      stars.add(center.clone().add(-size, 0.0D, size));
      int[][] connections = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}};
      return new ConstellationManifestationAbility.Constellation("Square", center, stars, connections);
   }

   private ConstellationManifestationAbility.Constellation createCrossConstellation(Location center) {
      List<Location> stars = new ArrayList();
      double size = 5.0D;
      stars.add(center.clone());
      stars.add(center.clone().add(0.0D, 0.0D, -size));
      stars.add(center.clone().add(size, 0.0D, 0.0D));
      stars.add(center.clone().add(0.0D, 0.0D, size));
      stars.add(center.clone().add(-size, 0.0D, 0.0D));
      int[][] connections = new int[][]{{0, 1}, {0, 2}, {0, 3}, {0, 4}};
      return new ConstellationManifestationAbility.Constellation("Cross", center, stars, connections);
   }

   private void formConstellations(final List<ConstellationManifestationAbility.Constellation> constellations) {
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               double formProgress = (double)this.tick / 40.0D;
               Iterator var3 = constellations.iterator();

               while(true) {
                  ConstellationManifestationAbility.Constellation constellation;
                  World world;
                  do {
                     if (!var3.hasNext()) {
                        if (this.tick % 10 == 0) {
                           ParticleUtils.playSound(((ConstellationManifestationAbility.Constellation)constellations.get(0)).center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, (float)(0.800000011920929D + formProgress * 0.4000000059604645D));
                        }

                        ++this.tick;
                        return;
                     }

                     constellation = (ConstellationManifestationAbility.Constellation)var3.next();
                     world = constellation.center.getWorld();
                  } while(world == null);

                  BlockDisplay display;
                  if (this.tick == 0) {
                     Iterator var6 = constellation.stars.iterator();

                     while(var6.hasNext()) {
                        Location starLocx = (Location)var6.next();
                        display = (BlockDisplay)world.spawn(starLocx, BlockDisplay.class, (d) -> {
                           d.setBlock(Material.SEA_LANTERN.createBlockData());
                           d.setGlowing(true);
                           d.setBrightness(new Brightness(15, 15));
                           Transformation t = d.getTransformation();
                           d.setTransformation(new Transformation(new Vector3f(-0.25F, -0.25F, -0.25F), t.getLeftRotation(), new Vector3f(0.5F, 0.5F, 0.5F), t.getRightRotation()));
                        });
                        constellation.starDisplays.add(display);
                     }
                  }

                  float scale = (float)(0.5D + formProgress * 0.5D);
                  Iterator var11 = constellation.starDisplays.iterator();

                  while(var11.hasNext()) {
                     display = (BlockDisplay)var11.next();
                     if (display.isValid()) {
                        Transformation t = display.getTransformation();
                        display.setTransformation(new Transformation(new Vector3f(-scale / 2.0F, -scale / 2.0F, -scale / 2.0F), t.getLeftRotation(), new Vector3f(scale, scale, scale), t.getRightRotation()));
                     }
                  }

                  var11 = constellation.stars.iterator();

                  while(var11.hasNext()) {
                     Location starLoc = (Location)var11.next();
                     ParticleUtils.endRod(starLoc, 3, 0.3D, 0.02D);
                     if (ConstellationManifestationAbility.this.random.nextDouble() < 0.3D) {
                        ParticleUtils.glow(starLoc, 1, 0.2D, 0.0D);
                     }
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void connectConstellations(final List<ConstellationManifestationAbility.Constellation> constellations) {
      ParticleUtils.playSound(((ConstellationManifestationAbility.Constellation)constellations.get(0)).center, Sound.BLOCK_BEACON_AMBIENT, 1.5F, 1.2F);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               Iterator var1 = constellations.iterator();

               while(var1.hasNext()) {
                  ConstellationManifestationAbility.Constellation constellation = (ConstellationManifestationAbility.Constellation)var1.next();
                  int[][] var3 = constellation.connections;
                  int var4 = var3.length;

                  for(int var5 = 0; var5 < var4; ++var5) {
                     int[] connection = var3[var5];
                     Location start = (Location)constellation.stars.get(connection[0]);
                     Location end = (Location)constellation.stars.get(connection[1]);
                     ParticleUtils.line(start, end, Particle.END_ROD, 0.3D);
                     double pulseProgress = (double)(this.tick % 20) / 20.0D;
                     Location pulseLoc = start.clone().add(end.toVector().subtract(start.toVector()).multiply(pulseProgress));
                     ParticleUtils.soulFlame(pulseLoc, 3, 0.2D, 0.02D);
                  }

                  Iterator var12 = constellation.stars.iterator();

                  while(var12.hasNext()) {
                     Location starLoc = (Location)var12.next();
                     ParticleUtils.glow(starLoc, 5, 0.4D, 0.01D);
                  }
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void fireFromConstellations(Player player, List<ConstellationManifestationAbility.Constellation> constellations, Location origin, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 1.2F);
      Iterator var5 = constellations.iterator();

      while(var5.hasNext()) {
         ConstellationManifestationAbility.Constellation constellation = (ConstellationManifestationAbility.Constellation)var5.next();

         for(int i = 0; i < constellation.stars.size(); ++i) {
            Location starLoc = (Location)constellation.stars.get(i);
            int delay = i * 5;
            this.runLater(() -> {
               this.fireStarBeam(player, starLoc, origin, settings);
            }, (long)delay);
         }
      }

      this.runLater(() -> {
         this.fireGrandFinale(player, constellations, origin, settings);
      }, 60L);
   }

   private void fireStarBeam(final Player player, final Location starLoc, Location origin, final IvoryConfig.AbilitySettings settings) {
      World world = starLoc.getWorld();
      if (world != null) {
         double angle = this.random.nextDouble() * 3.141592653589793D * 2.0D;
         double dist = this.random.nextDouble() * settings.radius * 0.8D;
         final Location targetLoc = origin.clone().add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist);
         ParticleUtils.playSound(starLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.5F);
         (new BukkitRunnable() {
            int tick = 0;
            Location currentLoc = starLoc.clone();
            Vector direction = targetLoc.toVector().subtract(starLoc.toVector()).normalize();
            double speed = 3.0D;

            public void run() {
               double distToTarget = this.currentLoc.distance(targetLoc);
               if (!(distToTarget < this.speed) && this.tick < 50) {
                  this.currentLoc.add(this.direction.clone().multiply(this.speed));
                  ParticleUtils.soulFlame(this.currentLoc, 8, 0.3D, 0.02D);
                  ParticleUtils.endRod(this.currentLoc, 5, 0.2D, 0.01D);
                  ++this.tick;
               } else {
                  ConstellationManifestationAbility.this.createStarImpact(player, targetLoc, settings);
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createStarImpact(Player player, Location loc, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         final Location groundLoc = loc.clone();
         groundLoc.setY((double)world.getHighestBlockYAt(loc) + 0.5D);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);
         ParticleUtils.flash(groundLoc);
         ParticleUtils.soulFlame(groundLoc, 30, 2.0D, 0.15D);
         ParticleUtils.endRod(groundLoc, 20, 1.5D, 0.2D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  ParticleUtils.ring(groundLoc, Particle.END_ROD, (double)this.tick * 0.4D, this.tick * 5 + 6);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.sphere(player, groundLoc, 2.5D, settings.damagePerTick, settings.damageTickInterval, 15);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void fireGrandFinale(final Player player, final List<ConstellationManifestationAbility.Constellation> constellations, final Location origin, final IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
      Iterator var5 = constellations.iterator();

      while(var5.hasNext()) {
         ConstellationManifestationAbility.Constellation constellation = (ConstellationManifestationAbility.Constellation)var5.next();
         Iterator var7 = constellation.stars.iterator();

         while(var7.hasNext()) {
            Location starLoc = (Location)var7.next();
            ParticleUtils.flash(starLoc);
         }
      }

      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 20) {
               ConstellationManifestationAbility.this.createFinaleExplosion(player, origin, settings);
               this.cancel();
            } else {
               Iterator var1 = constellations.iterator();

               while(var1.hasNext()) {
                  ConstellationManifestationAbility.Constellation constellation = (ConstellationManifestationAbility.Constellation)var1.next();
                  Iterator var3 = constellation.stars.iterator();

                  while(var3.hasNext()) {
                     Location starLoc = (Location)var3.next();
                     double progress = (double)this.tick / 20.0D;
                     Location beamPoint = starLoc.clone().add(origin.clone().add(0.0D, 2.0D, 0.0D).toVector().subtract(starLoc.toVector()).multiply(progress));
                     ParticleUtils.soulFlame(beamPoint, 5, 0.3D, 0.02D);
                     ParticleUtils.endRod(beamPoint, 3, 0.2D, 0.01D);
                  }
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createFinaleExplosion(Player player, final Location origin, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.5F);
      ParticleUtils.flash(origin.clone().add(0.0D, 2.0D, 0.0D));
      ParticleUtils.soulFlame(origin.clone().add(0.0D, 2.0D, 0.0D), 150, 6.0D, 0.4D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 3.0D, 0.0D), 100, 5.0D, 0.5D);
      ParticleUtils.firework(origin.clone().add(0.0D, 4.0D, 0.0D), 80, 4.0D, 0.6D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 20) {
               this.cancel();
            } else {
               double radius = (double)this.tick * 1.2D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 10.0D) + 20);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.sphere(player, origin, settings.radius, settings.damagePerTick * 2.5D, settings.damageTickInterval, 50);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }

   private void cleanupConstellations(List<ConstellationManifestationAbility.Constellation> constellations) {
      Iterator var2 = constellations.iterator();

      while(var2.hasNext()) {
         ConstellationManifestationAbility.Constellation constellation = (ConstellationManifestationAbility.Constellation)var2.next();
         Iterator var4 = constellation.starDisplays.iterator();

         while(var4.hasNext()) {
            BlockDisplay display = (BlockDisplay)var4.next();
            if (display.isValid()) {
               display.remove();
            }
         }
      }

   }

   private static class Constellation {
      String name;
      Location center;
      List<Location> stars;
      int[][] connections;
      List<BlockDisplay> starDisplays = new ArrayList();

      Constellation(String name, Location center, List<Location> stars, int[][] connections) {
         this.name = name;
         this.center = center;
         this.stars = stars;
         this.connections = connections;
      }
   }
}
