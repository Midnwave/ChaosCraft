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

public class ApocalypseDivineJudgmentAbility extends Ability {
   private final Random random = new Random();

   public ApocalypseDivineJudgmentAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "apocalypse_divine_judgment";
   }

   public String getDisplayName() {
      return "Apocalypse: Divine Judgment";
   }

   public String getDescription() {
      return "Open the gates of heaven and call down devastating beams of divine judgment that obliterate all who stand before you.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      int beamCount = settings.getInt("beam-count", 30);
      int durationSeconds = settings.getInt("duration-seconds", 15);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_END_PORTAL_SPAWN, 2.0F, 0.3F);
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
      this.createSkyOpening(origin, () -> {
         this.executeJudgment(player, origin, beamCount, durationSeconds, settings);
      });
   }

   private void createSkyOpening(final Location origin, final Runnable onComplete) {
      final Location skyCenter = origin.clone().add(0.0D, 50.0D, 0.0D);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;
         final int openTime = 60;
         List<BlockDisplay> portalParts = new ArrayList();

         public void run() {
            if (this.tick >= 60) {
               Iterator var16 = this.portalParts.iterator();

               while(var16.hasNext()) {
                  BlockDisplay d = (BlockDisplay)var16.next();
                  if (d.isValid()) {
                     d.remove();
                  }
               }

               for(int ix = 0; ix < 10; ++ix) {
                  Location flashLoc = skyCenter.clone().add((ApocalypseDivineJudgmentAbility.this.random.nextDouble() - 0.5D) * 30.0D, (ApocalypseDivineJudgmentAbility.this.random.nextDouble() - 0.5D) * 10.0D, (ApocalypseDivineJudgmentAbility.this.random.nextDouble() - 0.5D) * 30.0D);
                  ParticleUtils.flash(flashLoc);
               }

               ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0F, 0.3F);
               onComplete.run();
               this.cancel();
            } else {
               World world = skyCenter.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 60.0D;
                  double portalRadius = 20.0D * progress;
                  int i;
                  double angle;
                  double dist;
                  double x;
                  Location beamStart;
                  if (this.tick == 0) {
                     for(i = 0; i < 8; ++i) {
                        angle = 0.7853981633974483D * (double)i;
                        dist = Math.cos(angle) * 15.0D;
                        x = Math.sin(angle) * 15.0D;
                        beamStart = skyCenter.clone().add(dist, 0.0D, x);
                        BlockDisplay display = (BlockDisplay)world.spawn(beamStart, BlockDisplay.class, (dx) -> {
                           dx.setBlock(Material.SEA_LANTERN.createBlockData());
                           dx.setGlowing(true);
                           dx.setBrightness(new Brightness(15, 15));
                           Transformation t = dx.getTransformation();
                           dx.setTransformation(new Transformation(new Vector3f(-1.0F, -1.0F, -1.0F), t.getLeftRotation(), new Vector3f(2.0F, 2.0F, 2.0F), t.getRightRotation()));
                        });
                        this.portalParts.add(display);
                     }
                  }

                  for(i = 0; i < this.portalParts.size(); ++i) {
                     BlockDisplay displayx = (BlockDisplay)this.portalParts.get(i);
                     if (displayx.isValid()) {
                        double anglex = 6.283185307179586D / (double)this.portalParts.size() * (double)i + this.rotation;
                        double radius = portalRadius * 0.8D;
                        double xx = Math.cos(anglex) * radius;
                        double z = Math.sin(anglex) * radius;
                        displayx.teleport(skyCenter.clone().add(xx, 0.0D, z));
                     }
                  }

                  for(i = 0; i < 5; ++i) {
                     angle = portalRadius * (0.3D + (double)i * 0.15D);
                     dist = this.rotation * (1.0D + (double)i * 0.2D) * (double)(i % 2 == 0 ? 1 : -1);
                     ParticleUtils.ring(skyCenter, i == 0 ? Particle.SOUL_FIRE_FLAME : Particle.END_ROD, angle, 30 + i * 10);
                  }

                  if (this.tick % 3 == 0) {
                     for(i = 0; i < 8; ++i) {
                        angle = this.rotation + 0.7853981633974483D * (double)i;
                        dist = Math.cos(angle) * portalRadius;
                        x = Math.sin(angle) * portalRadius;
                        beamStart = skyCenter.clone().add(dist, 0.0D, x);
                        Location beamEnd = beamStart.clone().add(0.0D, -20.0D, 0.0D);
                        ParticleUtils.line(beamStart, beamEnd, Particle.END_ROD, 1.0D);
                     }
                  }

                  ParticleUtils.glow(skyCenter, (int)(30.0D * progress), portalRadius * 0.3D, 0.0D);
                  ParticleUtils.electricSpark(skyCenter, (int)(20.0D * progress), portalRadius * 0.4D, 0.1D);
                  if (this.tick % 8 == 0) {
                     ParticleUtils.flash(skyCenter);
                  }

                  if (this.tick % 15 == 0) {
                     ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, (float)(0.30000001192092896D + progress * 0.5D));
                  }

                  if (this.tick % 20 == 0) {
                     ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 1.5F);
                  }

                  for(i = 0; i < (int)(30.0D * progress); ++i) {
                     angle = ApocalypseDivineJudgmentAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     dist = ApocalypseDivineJudgmentAbility.this.random.nextDouble() * portalRadius;
                     x = Math.cos(angle) * dist;
                     double zx = Math.sin(angle) * dist;
                     Location particleLoc = skyCenter.clone().add(x, -ApocalypseDivineJudgmentAbility.this.random.nextDouble() * 10.0D, zx);
                     ParticleUtils.endRod(particleLoc, 1, 0.1D, 0.1D);
                  }

                  this.rotation += 0.08D;
                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void executeJudgment(final Player player, final Location origin, final int beamCount, int durationSeconds, final IvoryConfig.AbilitySettings settings) {
      final int durationTicks = durationSeconds * 20;
      final int beamsPerWave = Math.max(1, beamCount / (durationTicks / 10));
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
      (new BukkitRunnable() {
         int tick = 0;
         int beamsFired = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick < durationTicks && this.beamsFired < beamCount) {
               if (this.tick % 10 == 0) {
                  for(int i = 0; i < beamsPerWave && this.beamsFired < beamCount; ++i) {
                     double targetAngle;
                     double targetDist;
                     if (this.beamsFired < beamCount / 3) {
                        targetAngle = this.rotation + (double)this.beamsFired * 0.5D;
                        targetDist = settings.radius * 0.8D;
                     } else if (this.beamsFired < beamCount * 2 / 3) {
                        targetAngle = this.rotation + (double)this.beamsFired * 0.7D;
                        targetDist = settings.radius * 0.5D;
                     } else {
                        targetAngle = ApocalypseDivineJudgmentAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                        targetDist = ApocalypseDivineJudgmentAbility.this.random.nextDouble() * settings.radius;
                     }

                     double x = Math.cos(targetAngle) * targetDist;
                     double z = Math.sin(targetAngle) * targetDist;
                     Location targetLoc = origin.clone().add(x, 0.0D, z);
                     ApocalypseDivineJudgmentAbility.this.fireJudgmentBeam(player, targetLoc, settings);
                     ++this.beamsFired;
                  }
               }

               Location skyLoc = origin.clone().add(0.0D, 50.0D, 0.0D);
               ParticleUtils.ring(skyLoc, Particle.SOUL_FIRE_FLAME, 20.0D, 60);
               ParticleUtils.ring(skyLoc, Particle.END_ROD, 15.0D, 40);

               for(int ix = 0; ix < 20; ++ix) {
                  double angle = ApocalypseDivineJudgmentAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double dist = ApocalypseDivineJudgmentAbility.this.random.nextDouble() * 25.0D;
                  double xx = Math.cos(angle) * dist;
                  double zx = Math.sin(angle) * dist;
                  double y = 50.0D - ApocalypseDivineJudgmentAbility.this.random.nextDouble() * 30.0D;
                  Location particleLoc = origin.clone().add(xx, y, zx);
                  ParticleUtils.endRod(particleLoc, 1, 0.1D, 0.15D);
               }

               if (this.tick % 15 == 0) {
                  ParticleUtils.flash(skyLoc);
                  ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 1.2F);
               }

               if (this.tick % 30 == 0) {
                  ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 2.0F, 0.5F);
               }

               this.rotation += 0.1D;
               ++this.tick;
            } else {
               ApocalypseDivineJudgmentAbility.this.createFinalApocalypse(player, origin, settings);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void fireJudgmentBeam(Player player, Location targetLoc, IvoryConfig.AbilitySettings settings) {
      World world = targetLoc.getWorld();
      if (world != null) {
         final Location groundTarget = targetLoc.clone();
         groundTarget.setY((double)world.getHighestBlockYAt(targetLoc) + 0.5D);
         Location skyStart = groundTarget.clone().add(0.0D, 60.0D, 0.0D);
         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;

            public void run() {
               if (this.tick >= 15) {
                  this.cancel();
               } else {
                  double scale = 1.0D + Math.sin((double)this.tick * 0.5D) * 0.3D;
                  ParticleUtils.ring(groundTarget.clone().add(0.0D, 0.1D, 0.0D), Particle.END_ROD, 2.0D * scale, 16);

                  for(int i = 0; i < 4; ++i) {
                     double angle = this.rotation + (double)i * 3.141592653589793D / 2.0D;

                     for(double d = 0.0D; d < 2.0D; d += 0.3D) {
                        double x = Math.cos(angle) * d;
                        double z = Math.sin(angle) * d;
                        ParticleUtils.spawnSingle(groundTarget.clone().add(x, 0.1D, z), Particle.SOUL_FIRE_FLAME);
                     }
                  }

                  this.rotation += 0.2D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         this.runLater(() -> {
            ParticleUtils.playSound(groundTarget, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 1.0F);
            ParticleUtils.playSound(groundTarget, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0F, 1.2F);
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick >= 20) {
                     this.cancel();
                  } else {
                     double intensity = 1.0D - (double)this.tick / 20.0D;

                     for(double y = 60.0D; y > 0.0D; y -= 2.0D) {
                        Location beamLoc = groundTarget.clone().add(0.0D, y, 0.0D);
                        if (this.tick < 10) {
                           ParticleUtils.soulFlame(beamLoc, (int)(3.0D * intensity), 0.5D, 0.01D);
                        }

                        ParticleUtils.endRod(beamLoc, (int)(2.0D * intensity), 0.3D, 0.0D);
                     }

                     if (this.tick % 4 == 0 && this.tick < 12) {
                        ParticleUtils.flash(groundTarget.clone().add(0.0D, 30.0D, 0.0D));
                     }

                     ++this.tick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
            ParticleUtils.flash(groundTarget);
            ParticleUtils.soulFlame(groundTarget, 50, 2.0D, 0.2D);
            ParticleUtils.endRod(groundTarget.clone().add(0.0D, 1.0D, 0.0D), 35, 1.5D, 0.25D);
            ParticleUtils.electricSpark(groundTarget, 25, 2.5D, 0.15D);
            ParticleUtils.firework(groundTarget.clone().add(0.0D, 2.0D, 0.0D), 20, 1.5D, 0.3D);
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick >= 12) {
                     this.cancel();
                  } else {
                     double radius = (double)this.tick * 0.6D;
                     ParticleUtils.ring(groundTarget, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 10);
                     if (this.tick % 2 == 0) {
                        ParticleUtils.ring(groundTarget.clone().add(0.0D, 0.3D, 0.0D), Particle.END_ROD, radius * 0.8D, (int)(radius * 5.0D) + 5);
                     }

                     ++this.tick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
            DamageZone zone = DamageZone.cylinder(player, groundTarget, 3.5D, 4.0D, settings.damagePerTick, settings.damageTickInterval, 15);
            DamageZoneManager.getInstance(this.plugin).register(zone);
         }, 15L);
      }
   }

   private void createFinalApocalypse(final Player player, final Location origin, final IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_DEATH, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.6F);
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               ApocalypseDivineJudgmentAbility.this.createUltimateExplosion(player, origin, settings);
               this.cancel();
            } else {
               double beamWidth = 5.0D + (double)this.tick * 0.2D;

               double spiralAngle;
               for(spiralAngle = 0.0D; spiralAngle < 60.0D; ++spiralAngle) {
                  Location beamLoc = origin.clone().add(0.0D, spiralAngle, 0.0D);
                  ParticleUtils.soulFlame(beamLoc, 8, beamWidth * 0.3D, 0.02D);
                  ParticleUtils.endRod(beamLoc, 5, beamWidth * 0.2D, 0.01D);
               }

               spiralAngle = (double)this.tick * 0.3D;

               double groundRadius;
               for(groundRadius = 0.0D; groundRadius < 60.0D; groundRadius += 2.0D) {
                  double spiralRadius = beamWidth * 0.8D;
                  double x = Math.cos(spiralAngle + groundRadius * 0.2D) * spiralRadius;
                  double z = Math.sin(spiralAngle + groundRadius * 0.2D) * spiralRadius;
                  Location spiralLoc = origin.clone().add(x, groundRadius, z);
                  ParticleUtils.electricSpark(spiralLoc, 2, 0.2D, 0.03D);
               }

               groundRadius = (double)this.tick * 0.8D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, groundRadius, (int)(groundRadius * 6.0D) + 20);
               if (this.tick % 3 == 0) {
                  ParticleUtils.flash(origin.clone().add(0.0D, ApocalypseDivineJudgmentAbility.this.random.nextDouble() * 30.0D, 0.0D));
               }

               if (this.tick % 10 == 0) {
                  ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.8F);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createUltimateExplosion(Player player, final Location origin, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.2F);

      for(int i = 0; i < 20; ++i) {
         this.runLater(() -> {
            Location flashLoc = origin.clone().add((this.random.nextDouble() - 0.5D) * 20.0D, this.random.nextDouble() * 20.0D, (this.random.nextDouble() - 0.5D) * 20.0D);
            ParticleUtils.flash(flashLoc);
         }, (long)i);
      }

      ParticleUtils.soulFlame(origin.clone().add(0.0D, 3.0D, 0.0D), 500, 15.0D, 0.6D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 5.0D, 0.0D), 350, 12.0D, 0.7D);
      ParticleUtils.firework(origin.clone().add(0.0D, 8.0D, 0.0D), 250, 10.0D, 0.8D);
      ParticleUtils.electricSpark(origin, 200, 18.0D, 0.5D);
      ParticleUtils.glow(origin.clone().add(0.0D, 5.0D, 0.0D), 150, 10.0D, 0.4D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               double radius = (double)(this.tick * 2);
               double intensity = 1.0D - (double)this.tick / 40.0D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 12.0D) + 30);

               int i;
               double angle;
               for(i = 0; i < 6; ++i) {
                  angle = radius * (1.0D - (double)i * 0.12D);
                  if (angle > 0.0D) {
                     Location ringLoc = origin.clone().add(0.0D, (double)(i * 4) * intensity, 0.0D);
                     ParticleUtils.ring(ringLoc, Particle.END_ROD, angle, (int)(angle * 8.0D) + 15);
                  }
               }

               if (this.tick % 3 == 0) {
                  for(i = 0; i < 8; ++i) {
                     angle = 0.7853981633974483D * (double)i + (double)this.tick * 0.1D;
                     double x = Math.cos(angle) * radius * 0.5D;
                     double z = Math.sin(angle) * radius * 0.5D;

                     for(double y = 0.0D; y < 20.0D * intensity; ++y) {
                        Location pillarLoc = origin.clone().add(x, y, z);
                        ParticleUtils.endRod(pillarLoc, 1, 0.2D, 0.05D);
                     }
                  }
               }

               if (this.tick % 4 == 0 && intensity > 0.3D) {
                  ParticleUtils.flash(origin.clone().add(0.0D, (double)this.tick * 0.5D, 0.0D));
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.sphere(player, origin, settings.radius * 2.5D, settings.damagePerTick * 8.0D, settings.damageTickInterval, 60);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
