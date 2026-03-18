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
import java.util.Objects;
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

public class CelestialMeteorShowerAbility extends Ability {
   private final Random random = new Random();

   public CelestialMeteorShowerAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "celestial_meteor_shower";
   }

   public String getDisplayName() {
      return "Celestial Meteor Shower";
   }

   public String getDescription() {
      return "Call down a devastating shower of massive celestial meteors that blanket the area in divine destruction.";
   }

   public void execute(final Player player) {
      final IvoryConfig.AbilitySettings settings = this.getSettings();
      final Location origin = this.getExecutionLocation(player);
      final double radius = settings.radius;
      final int meteorCount = settings.getInt("meteor-count", 15);
      final int duration = settings.getInt("duration-ticks", 150);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.5F, 0.6F);
      ParticleUtils.playSound(origin, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.2F);
      this.createSkyWarning(origin, radius);
      (new BukkitRunnable() {
         int meteorsSpawned = 0;
         int tick = 0;

         public void run() {
            if (this.tick < duration && this.meteorsSpawned < meteorCount) {
               if (this.tick % 10 == 0 && this.meteorsSpawned < meteorCount) {
                  double angle = CelestialMeteorShowerAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double dist = CelestialMeteorShowerAbility.this.random.nextDouble() * radius;
                  double xx = Math.cos(angle) * dist;
                  double zx = Math.sin(angle) * dist;
                  Location targetLoc = origin.clone().add(xx, 0.0D, zx);
                  double roll = CelestialMeteorShowerAbility.this.random.nextDouble();
                  CelestialMeteorShowerAbility.MeteorSize size;
                  if (roll < 0.1D) {
                     size = CelestialMeteorShowerAbility.MeteorSize.MASSIVE;
                  } else if (roll < 0.35D) {
                     size = CelestialMeteorShowerAbility.MeteorSize.LARGE;
                  } else {
                     size = CelestialMeteorShowerAbility.MeteorSize.MEDIUM;
                  }

                  CelestialMeteorShowerAbility.this.spawnMeteor(player, targetLoc, size, settings);
                  ++this.meteorsSpawned;
               }

               if (this.tick % 3 == 0) {
                  for(int i = 0; i < 5; ++i) {
                     double x = (CelestialMeteorShowerAbility.this.random.nextDouble() - 0.5D) * radius * 2.0D;
                     double z = (CelestialMeteorShowerAbility.this.random.nextDouble() - 0.5D) * radius * 2.0D;
                     double y = 30.0D + CelestialMeteorShowerAbility.this.random.nextDouble() * 20.0D;
                     Location skyLoc = origin.clone().add(x, y, z);
                     ParticleUtils.endRod(skyLoc, 1, 0.5D, 0.3D);
                  }
               }

               ++this.tick;
            } else {
               CelestialMeteorShowerAbility.this.runLater(() -> {
                  CelestialMeteorShowerAbility.this.createFinale(player, origin, radius, settings);
               }, 20L);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 30L, 1L);
   }

   private void createSkyWarning(final Location origin, final double radius) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 30) {
               this.cancel();
            } else {
               Location skyCenter = origin.clone().add(0.0D, 40.0D, 0.0D);
               double currentRadius = radius * ((double)this.tick / 30.0D);

               int i;
               double angle;
               double x;
               double z;
               Location point;
               for(i = 0; i < 50; ++i) {
                  angle = 0.12566370614359174D * (double)i + this.rotation;
                  x = Math.cos(angle) * currentRadius;
                  z = Math.sin(angle) * currentRadius;
                  point = skyCenter.clone().add(x, 0.0D, z);
                  ParticleUtils.soulFlame(point, 1, 0.2D, 0.0D);
               }

               for(i = 0; i < 30; ++i) {
                  angle = 0.20943951023931953D * (double)i - this.rotation * 1.5D;
                  x = Math.cos(angle) * currentRadius * 0.5D;
                  z = Math.sin(angle) * currentRadius * 0.5D;
                  point = skyCenter.clone().add(x, 0.0D, z);
                  ParticleUtils.endRod(point, 1, 0.1D, 0.0D);
               }

               if (this.tick % 10 == 0) {
                  ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 1.5F, 0.5F);
               }

               this.rotation += 0.1D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spawnMeteor(final Player player, final Location targetLoc, final CelestialMeteorShowerAbility.MeteorSize size, final IvoryConfig.AbilitySettings settings) {
      final World world = targetLoc.getWorld();
      if (world != null) {
         double offsetX = (this.random.nextDouble() - 0.5D) * 30.0D;
         double offsetZ = (this.random.nextDouble() - 0.5D) * 30.0D;
         Location var10000 = targetLoc.clone();
         Objects.requireNonNull(size);
         final Location spawnLoc = var10000.add(offsetX, (double)(50 + 10), offsetZ);
         final Vector direction = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();
         double distance = spawnLoc.distance(targetLoc);
         final double speed = 2.5D + (double)size.ordinal() * 0.5D;
         final int travelTicks = (int)(distance / speed);
         this.createMeteorWarning(targetLoc, size);
         ParticleUtils.playSound(spawnLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5F, 0.4F);
         (new BukkitRunnable() {
            Location currentLoc = spawnLoc.clone();
            int tick = 0;
            List<BlockDisplay> meteorDisplays = new ArrayList();
            double rotation;

            {
               this.rotation = CelestialMeteorShowerAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
            }

            public void run() {
               double groundY = (double)(world.getHighestBlockYAt(targetLoc) + 1);
               BlockDisplay chunkx;
               Location tailLoc;
               if (this.tick < travelTicks && !(this.currentLoc.getY() <= groundY)) {
                  this.currentLoc.add(direction.clone().multiply(speed));
                  double angle;
                  double chunkDist;
                  double x;
                  double y;
                  if (this.tick == 0) {
                     BlockDisplay core = (BlockDisplay)world.spawn(this.currentLoc, BlockDisplay.class, (d) -> {
                        d.setBlock(Material.MAGMA_BLOCK.createBlockData());
                        d.setGlowing(true);
                        d.setBrightness(new Brightness(15, 15));
                        float coreSize = size.displaySize;
                        Transformation t = d.getTransformation();
                        d.setTransformation(new Transformation(new Vector3f(-coreSize / 2.0F, -coreSize / 2.0F, -coreSize / 2.0F), t.getLeftRotation(), new Vector3f(coreSize, coreSize, coreSize), t.getRightRotation()));
                     });
                     this.meteorDisplays.add(core);

                     for(int ix = 0; ix < size.chunkCount; ++ix) {
                        angle = 6.283185307179586D / (double)size.chunkCount * (double)ix;
                        chunkDist = (double)size.displaySize * 0.8D;
                        x = Math.cos(angle) * chunkDist;
                        y = Math.sin(angle) * chunkDist;
                        Location chunkLoc = this.currentLoc.clone().add(x, y, 0.0D);
                        BlockDisplay chunk = (BlockDisplay)world.spawn(chunkLoc, BlockDisplay.class, (d) -> {
                           d.setBlock(Material.GLOWSTONE.createBlockData());
                           d.setGlowing(true);
                           d.setBrightness(new Brightness(15, 15));
                           float chunkSize = size.displaySize * 0.4F;
                           Transformation t = d.getTransformation();
                           d.setTransformation(new Transformation(new Vector3f(-chunkSize / 2.0F, -chunkSize / 2.0F, -chunkSize / 2.0F), t.getLeftRotation(), new Vector3f(chunkSize, chunkSize, chunkSize), t.getRightRotation()));
                        });
                        this.meteorDisplays.add(chunk);
                     }
                  } else {
                     if (!this.meteorDisplays.isEmpty() && ((BlockDisplay)this.meteorDisplays.get(0)).isValid()) {
                        ((BlockDisplay)this.meteorDisplays.get(0)).teleport(this.currentLoc);
                     }

                     for(int i = 1; i < this.meteorDisplays.size(); ++i) {
                        chunkx = (BlockDisplay)this.meteorDisplays.get(i);
                        if (chunkx.isValid()) {
                           angle = this.rotation + (double)(i - 1) * 3.141592653589793D * 2.0D / (double)size.chunkCount;
                           chunkDist = (double)size.displaySize * 0.8D;
                           x = Math.cos(angle) * chunkDist;
                           y = Math.sin(angle) * chunkDist;
                           chunkx.teleport(this.currentLoc.clone().add(x, y, 0.0D));
                        }
                     }
                  }

                  ParticleUtils.soulFlame(this.currentLoc, size.particleCount, (double)size.displaySize * 0.5D, 0.05D);
                  ParticleUtils.endRod(this.currentLoc, size.particleCount / 2, (double)size.displaySize * 0.4D, 0.03D);
                  tailLoc = this.currentLoc.clone();
                  Vector tailDir = direction.clone().multiply(-1);

                  for(int ixx = 0; ixx < size.tailLength; ++ixx) {
                     tailLoc.add(tailDir.clone().multiply(0.8D));
                     double fade = 1.0D - (double)ixx / (double)size.tailLength;
                     ParticleUtils.soulFlame(tailLoc, (int)((double)size.particleCount * fade * 0.3D), (double)size.displaySize * 0.3D + (double)ixx * 0.1D, 0.03D);
                     if (ixx % 2 == 0) {
                        ParticleUtils.endRod(tailLoc, (int)((double)size.particleCount * fade * 0.2D), (double)size.displaySize * 0.2D + (double)ixx * 0.05D, 0.02D);
                     }
                  }

                  this.rotation += 0.3D;
                  ++this.tick;
               } else {
                  Iterator var3 = this.meteorDisplays.iterator();

                  while(var3.hasNext()) {
                     chunkx = (BlockDisplay)var3.next();
                     if (chunkx.isValid()) {
                        chunkx.remove();
                     }
                  }

                  tailLoc = this.currentLoc.clone();
                  tailLoc.setY(groundY);
                  CelestialMeteorShowerAbility.this.createMeteorImpact(player, tailLoc, size, settings);
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 10L, 1L);
      }
   }

   private void createMeteorWarning(final Location loc, final CelestialMeteorShowerAbility.MeteorSize size) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 30) {
               this.cancel();
            } else {
               double pulseScale = 1.0D + Math.sin((double)this.tick * 0.5D) * 0.2D;
               double warningRadius = size.impactRadius * 0.5D * pulseScale;
               ParticleUtils.ring(loc.clone().add(0.0D, 0.1D, 0.0D), Particle.END_ROD, warningRadius, (int)(warningRadius * 8.0D) + 10);

               for(int i = 0; i < 4; ++i) {
                  double angle = this.rotation + (double)i * 3.141592653589793D / 2.0D;

                  for(double r = 0.0D; r < warningRadius; r += 0.3D) {
                     double x = Math.cos(angle) * r;
                     double z = Math.sin(angle) * r;
                     ParticleUtils.spawnSingle(loc.clone().add(x, 0.1D, z), Particle.SOUL_FIRE_FLAME);
                  }
               }

               this.rotation += 0.1D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createMeteorImpact(Player player, final Location loc, final CelestialMeteorShowerAbility.MeteorSize size, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         float volumeScale = 1.0F + (float)size.ordinal() * 0.5F;
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F * volumeScale, 0.5F);
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5F * volumeScale, 0.7F);
         ParticleUtils.flash(loc);
         if (size == CelestialMeteorShowerAbility.MeteorSize.MASSIVE) {
            ParticleUtils.flash(loc.clone().add(0.0D, 3.0D, 0.0D));
            ParticleUtils.flash(loc.clone().add(0.0D, 6.0D, 0.0D));
         }

         int particleMultiplier = 1 + size.ordinal();
         ParticleUtils.soulFlame(loc, 50 * particleMultiplier, size.impactRadius, 0.25D);
         ParticleUtils.endRod(loc.clone().add(0.0D, 1.0D, 0.0D), 35 * particleMultiplier, size.impactRadius * 0.8D, 0.3D);
         ParticleUtils.firework(loc.clone().add(0.0D, 2.0D, 0.0D), 25 * particleMultiplier, size.impactRadius * 0.6D, 0.4D);
         ParticleUtils.electricSpark(loc, 20 * particleMultiplier, size.impactRadius * 1.2D, 0.2D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 15) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * (size.impactRadius / 10.0D);
                  ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 15);
                  if (this.tick % 2 == 0) {
                     ParticleUtils.ring(loc.clone().add(0.0D, 0.5D, 0.0D), Particle.END_ROD, radius * 0.8D, (int)(radius * 5.0D) + 8);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  double intensity = 1.0D - (double)this.tick / 40.0D;
                  ParticleUtils.soulFlame(loc, (int)(15.0D * intensity), size.impactRadius * 0.5D, 0.03D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 2L);
         DamageZone zone = DamageZone.sphere(player, loc, size.impactRadius, settings.damagePerTick * size.damageMultiplier, settings.damageTickInterval, 30);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void createFinale(Player player, final Location origin, double radius, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0.8F);

      for(int i = 0; i < 5; ++i) {
         double x = (this.random.nextDouble() - 0.5D) * radius;
         double z = (this.random.nextDouble() - 0.5D) * radius;
         ParticleUtils.flash(origin.clone().add(x, (double)(5 + i * 2), z));
      }

      ParticleUtils.soulFlame(origin.clone().add(0.0D, 2.0D, 0.0D), 200, radius * 0.6D, 0.35D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 4.0D, 0.0D), 150, radius * 0.5D, 0.4D);
      ParticleUtils.firework(origin.clone().add(0.0D, 6.0D, 0.0D), 100, radius * 0.4D, 0.5D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 25) {
               this.cancel();
            } else {
               double shockRadius = (double)this.tick * 1.5D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, shockRadius, (int)(shockRadius * 10.0D) + 25);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private static enum MeteorSize {
      MEDIUM(1.0F, 4, 8, 10, 3.0D, 1.0D),
      LARGE(1.8F, 6, 15, 15, 5.0D, 1.5D),
      MASSIVE(3.0F, 8, 25, 20, 8.0D, 2.5D);

      final float displaySize;
      final int chunkCount;
      final int particleCount;
      final int tailLength;
      final double impactRadius;
      final double damageMultiplier;
      final int height = 10;

      private MeteorSize(float displaySize, int chunkCount, int particleCount, int tailLength, double impactRadius, double damageMultiplier) {
         this.displaySize = displaySize;
         this.chunkCount = chunkCount;
         this.particleCount = particleCount;
         this.tailLength = tailLength;
         this.impactRadius = impactRadius;
         this.damageMultiplier = damageMultiplier;
      }

      
      private static CelestialMeteorShowerAbility.MeteorSize[] $values() {
         return new CelestialMeteorShowerAbility.MeteorSize[]{MEDIUM, LARGE, MASSIVE};
      }
   }
}
