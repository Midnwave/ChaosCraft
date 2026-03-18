package com.blockforge.chaoscraft.weapons.ivory.abilities.impl;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.IvoryConfig;
import com.blockforge.chaoscraft.weapons.ivory.IvoryEffectsManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZone;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.DamageZoneManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.ParticleUtils;
import java.util.ArrayList;
import java.util.List;
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

public class DivineCascadeAbility extends Ability {
   public DivineCascadeAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "divine_cascade";
   }

   public String getDisplayName() {
      return "Divine Cascade";
   }

   public String getDescription() {
      return "Summon cascading pillars of divine light that rain down from the heavens, creating expanding rings of holy fire that purge all enemies.";
   }

   public void execute(final Player player) {
      final IvoryConfig.AbilitySettings settings = this.getSettings();
      final Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 1.0F, 1.5F);
      ParticleUtils.flash(origin.clone().add(0.0D, 1.0D, 0.0D));
      ParticleUtils.soulFlame(origin.clone().add(0.0D, 1.0D, 0.0D), 50, 1.0D, 0.1D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 2.0D, 0.0D), 30, 0.5D, 0.2D);
      final List<Location> cascadePoints = this.generateCascadePattern(origin, radius, 12);
      (new BukkitRunnable() {
         int index = 0;
         int tick = 0;

         public void run() {
            if (this.index >= cascadePoints.size()) {
               DivineCascadeAbility.this.runLater(() -> {
                  DivineCascadeAbility.this.createFinalExplosion(origin, settings);
               }, 10L);
               this.cancel();
            } else {
               if (this.tick % 8 == 0 && this.index < cascadePoints.size()) {
                  Location pillarLoc = (Location)cascadePoints.get(this.index);
                  DivineCascadeAbility.this.createDivinePillar(player, pillarLoc, settings);
                  ++this.index;
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 20L, 1L);
      this.createChargingEffect(origin);
   }

   private List<Location> generateCascadePattern(Location origin, double maxRadius, int count) {
      List<Location> points = new ArrayList();

      for(int i = 0; i < count; ++i) {
         double progress = (double)i / (double)count;
         double angle = progress * 3.141592653589793D * 4.0D;
         double radius = progress * maxRadius;
         double x = Math.cos(angle) * radius;
         double z = Math.sin(angle) * radius;
         points.add(origin.clone().add(x, 0.0D, z));
      }

      return points;
   }

   private void createChargingEffect(final Location origin) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 20) {
               this.cancel();
            } else {
               double height = (double)this.tick * 0.5D;
               Location ringCenter = origin.clone().add(0.0D, height, 0.0D);
               DivineCascadeAbility.this.drawRotatingRing(ringCenter, Particle.SOUL_FIRE_FLAME, 1.0D, this.rotation, 20);
               DivineCascadeAbility.this.drawRotatingRing(ringCenter, Particle.END_ROD, 2.0D, -this.rotation, 30);

               for(int i = 0; i < 4; ++i) {
                  double angle = this.rotation + (double)i * 3.141592653589793D / 2.0D;
                  double x = Math.cos(angle) * 1.5D;
                  double z = Math.sin(angle) * 1.5D;
                  Location beamBase = origin.clone().add(x, 0.0D, z);

                  for(double y = 0.0D; y < height; y += 0.3D) {
                     Location beamPoint = beamBase.clone().add(0.0D, y, 0.0D);
                     ParticleUtils.spawnSingle(beamPoint, Particle.END_ROD);
                  }
               }

               ParticleUtils.electricSpark(origin.clone().add(0.0D, height, 0.0D), 10, 1.5D, 0.05D);
               this.rotation += 0.3D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void drawRotatingRing(Location center, Particle particle, double radius, double rotation, int points) {
      World world = center.getWorld();
      if (world != null) {
         double angleStep = 6.283185307179586D / (double)points;

         for(int i = 0; i < points; ++i) {
            double angle = (double)i * angleStep + rotation;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location point = center.clone().add(x, 0.0D, z);
            world.spawnParticle(particle, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
         }

      }
   }

   private void createDivinePillar(final Player player, final Location groundLoc, final IvoryConfig.AbilitySettings settings) {
      final World world = groundLoc.getWorld();
      if (world != null) {
         final double pillarHeight = 30.0D;
         Location skyLoc = groundLoc.clone().add(0.0D, pillarHeight, 0.0D);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 1.2F);
         ParticleUtils.playSound(groundLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.5F);
         this.createGroundMarker(groundLoc);
         (new BukkitRunnable() {
            double currentY = pillarHeight;
            int tick = 0;
            BlockDisplay pillarCore = null;

            public void run() {
               if (this.tick >= 15) {
                  if (this.pillarCore != null) {
                     this.pillarCore.remove();
                  }

                  DivineCascadeAbility.this.createPillarImpact(player, groundLoc, settings);
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 15.0D;
                  this.currentY = pillarHeight * (1.0D - progress);
                  Location pillarLoc = groundLoc.clone().add(0.0D, this.currentY, 0.0D);
                  if (this.tick == 0) {
                     this.pillarCore = (BlockDisplay)world.spawn(pillarLoc, BlockDisplay.class, (display) -> {
                        display.setBlock(Material.SEA_LANTERN.createBlockData());
                        display.setGlowing(true);
                        display.setBrightness(new Brightness(15, 15));
                        Transformation transform = display.getTransformation();
                        display.setTransformation(new Transformation(new Vector3f(-0.5F, 0.0F, -0.5F), transform.getLeftRotation(), new Vector3f(1.0F, 3.0F, 1.0F), transform.getRightRotation()));
                     });
                  } else if (this.pillarCore != null && this.pillarCore.isValid()) {
                     this.pillarCore.teleport(pillarLoc);
                  }

                  double spiralAngle;
                  for(spiralAngle = 0.0D; spiralAngle < 5.0D; spiralAngle += 0.5D) {
                     Location trailLoc = pillarLoc.clone().add(0.0D, spiralAngle, 0.0D);
                     ParticleUtils.soulFlame(trailLoc, 3, 0.3D, 0.02D);
                     ParticleUtils.endRod(trailLoc, 2, 0.2D, 0.01D);
                  }

                  spiralAngle = (double)this.tick * 0.5D;

                  for(int i = 0; i < 3; ++i) {
                     double angle = spiralAngle + (double)i * 3.141592653589793D * 2.0D / 3.0D;
                     double x = Math.cos(angle) * 0.8D;
                     double z = Math.sin(angle) * 0.8D;
                     Location spiralLoc = pillarLoc.clone().add(x, 1.0D, z);
                     ParticleUtils.spawnSingle(spiralLoc, Particle.ELECTRIC_SPARK);
                  }

                  if (this.tick % 3 == 0) {
                     ParticleUtils.flash(pillarLoc);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 5L, 1L);
      }
   }

   private void createGroundMarker(final Location loc) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 20) {
               this.cancel();
            } else {
               double pulseScale = 1.0D + Math.sin((double)this.tick * 0.5D) * 0.3D;
               ParticleUtils.ring(loc, Particle.END_ROD, 1.5D * pulseScale, 20);

               for(int i = 0; i < 6; ++i) {
                  double angle = this.rotation + (double)i * 3.141592653589793D / 3.0D;
                  double x = Math.cos(angle) * 0.8D;
                  double z = Math.sin(angle) * 0.8D;
                  Location point = loc.clone().add(x, 0.1D, z);
                  ParticleUtils.spawnSingle(point, Particle.SOUL_FIRE_FLAME);
               }

               this.rotation += 0.2D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createPillarImpact(Player player, Location loc, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.8F);
         ParticleUtils.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5F, 1.5F);
         ParticleUtils.flash(loc.clone().add(0.0D, 1.0D, 0.0D));
         ParticleUtils.soulFlame(loc.clone().add(0.0D, 0.5D, 0.0D), 80, 2.0D, 0.15D);
         ParticleUtils.endRod(loc.clone().add(0.0D, 1.0D, 0.0D), 50, 1.5D, 0.2D);
         ParticleUtils.electricSpark(loc.clone().add(0.0D, 0.5D, 0.0D), 40, 2.5D, 0.1D);
         ParticleUtils.firework(loc.clone().add(0.0D, 1.5D, 0.0D), 30, 1.0D, 0.2D);
         this.createExpandingRings(loc);
         DamageZone zone = DamageZone.cylinder(player, loc, settings.radius / 3.0D, 5.0D, settings.damagePerTick, settings.damageTickInterval, 40);
         DamageZoneManager.getInstance(this.plugin).register(zone);
         this.createLingeringPillar(loc);
      }
   }

   private void createExpandingRings(final Location loc) {
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 20) {
               this.cancel();
            } else {
               double radius = (double)this.tick * 0.5D;

               for(double y = 0.0D; y <= 2.0D; y += 0.5D) {
                  Location ringLoc = loc.clone().add(0.0D, y, 0.0D);
                  ParticleUtils.ring(ringLoc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 10);
                  if (this.tick % 2 == 0) {
                     ParticleUtils.ring(ringLoc, Particle.END_ROD, radius * 0.8D, (int)(radius * 5.0D) + 5);
                  }
               }

               if (this.tick % 3 == 0) {
                  ParticleUtils.ring(loc, Particle.WHITE_ASH, radius, (int)(radius * 6.0D));
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createLingeringPillar(final Location loc) {
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               double intensity = 1.0D - (double)this.tick / 40.0D;
               int particleCount = (int)(10.0D * intensity);

               for(double y = 0.0D; y < 15.0D * intensity; y += 0.5D) {
                  Location beamLoc = loc.clone().add(0.0D, y, 0.0D);
                  if (Math.random() < intensity) {
                     ParticleUtils.spawnSingle(beamLoc, Particle.END_ROD);
                  }

                  if (Math.random() < intensity * 0.5D) {
                     ParticleUtils.spawnSingle(beamLoc, Particle.SOUL_FIRE_FLAME);
                  }
               }

               if (this.tick % 5 == 0) {
                  ParticleUtils.electricSpark(loc.clone().add(0.0D, 2.0D, 0.0D), particleCount, 0.5D, 0.05D);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createFinalExplosion(final Location origin, IvoryConfig.AbilitySettings settings) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.5F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.5F);
         ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.8F);
         ParticleUtils.flash(origin.clone().add(0.0D, 5.0D, 0.0D));
         ParticleUtils.soulFlame(origin.clone().add(0.0D, 3.0D, 0.0D), 200, 5.0D, 0.3D);
         ParticleUtils.endRod(origin.clone().add(0.0D, 5.0D, 0.0D), 150, 4.0D, 0.4D);
         ParticleUtils.electricSpark(origin.clone().add(0.0D, 2.0D, 0.0D), 100, 6.0D, 0.2D);
         ParticleUtils.firework(origin.clone().add(0.0D, 8.0D, 0.0D), 80, 3.0D, 0.5D);
         ParticleUtils.glow(origin.clone().add(0.0D, 4.0D, 0.0D), 60, 4.0D, 0.1D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  double height = (double)(this.tick * 2);

                  double spiralAngle;
                  for(spiralAngle = 0.0D; spiralAngle < height; spiralAngle += 0.5D) {
                     Location beamLoc = origin.clone().add(0.0D, spiralAngle, 0.0D);
                     ParticleUtils.soulFlame(beamLoc, 2, 0.3D, 0.01D);
                     ParticleUtils.endRod(beamLoc, 1, 0.2D, 0.01D);
                  }

                  spiralAngle = (double)this.tick * 0.4D;

                  for(int i = 0; i < 4; ++i) {
                     double angle = spiralAngle + (double)i * 3.141592653589793D / 2.0D;

                     for(double y = 0.0D; y < height; ++y) {
                        double spiralRadius = 2.0D + y * 0.1D;
                        double x = Math.cos(angle + y * 0.2D) * spiralRadius;
                        double z = Math.sin(angle + y * 0.2D) * spiralRadius;
                        Location spiralLoc = origin.clone().add(x, y, z);
                        ParticleUtils.spawnSingle(spiralLoc, Particle.END_ROD);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }
}
