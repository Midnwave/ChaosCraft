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

public class SupernovaImplosionAbility extends Ability {
   private final Random random = new Random();

   public SupernovaImplosionAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "supernova_implosion";
   }

   public String getDisplayName() {
      return "Supernova Implosion";
   }

   public String getDescription() {
      return "Manifest a dying star that collapses under its own gravity before rebounding in a catastrophic supernova explosion.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 2.0F, 0.4F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 0.6F);
      Location starCenter = origin.clone().add(0.0D, 8.0D, 0.0D);
      List<BlockDisplay> starDisplays = this.createStar(starCenter, radius * 0.4D);
      this.runLater(() -> {
         this.collapsePhase(starCenter, radius, starDisplays);
      }, 40L);
      this.runLater(() -> {
         this.supernovaExplosion(player, starCenter, origin, radius, settings, starDisplays);
      }, 100L);
   }

   private List<BlockDisplay> createStar(final Location center, final double starRadius) {
      List<BlockDisplay> displays = new ArrayList();
      World world = center.getWorld();
      if (world == null) {
         return displays;
      } else {
         BlockDisplay core = (BlockDisplay)world.spawn(center, BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-1.5F, -1.5F, -1.5F), t.getLeftRotation(), new Vector3f(3.0F, 3.0F, 3.0F), t.getRightRotation()));
         });
         displays.add(core);

         for(int i = 0; i < 12; ++i) {
            double phi = Math.acos(1.0D - 2.0D * ((double)i + 0.5D) / 12.0D);
            double theta = 3.141592653589793D * (1.0D + Math.sqrt(5.0D)) * (double)i;
            double x = starRadius * Math.sin(phi) * Math.cos(theta);
            double y = starRadius * Math.cos(phi);
            double z = starRadius * Math.sin(phi) * Math.sin(theta);
            Location shellLoc = center.clone().add(x, y, z);
            BlockDisplay shell = (BlockDisplay)world.spawn(shellLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.SEA_LANTERN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.4F, -0.4F, -0.4F), t.getLeftRotation(), new Vector3f(0.8F, 0.8F, 0.8F), t.getRightRotation()));
            });
            displays.add(shell);
         }

         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  for(int i = 0; i < 30; ++i) {
                     double phi = SupernovaImplosionAbility.this.random.nextDouble() * 3.141592653589793D;
                     double theta = SupernovaImplosionAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double xx = (starRadius + 0.5D) * Math.sin(phi) * Math.cos(theta);
                     double y = (starRadius + 0.5D) * Math.cos(phi);
                     double zx = (starRadius + 0.5D) * Math.sin(phi) * Math.sin(theta);
                     Location surfacePoint = center.clone().add(xx, y, zx);
                     if (SupernovaImplosionAbility.this.random.nextDouble() < 0.6D) {
                        ParticleUtils.soulFlame(surfacePoint, 1, 0.2D, 0.03D);
                     } else {
                        ParticleUtils.endRod(surfacePoint, 1, 0.15D, 0.02D);
                     }
                  }

                  if (this.tick % 5 == 0) {
                     double flareAngle = this.rotation;

                     for(double r = starRadius; r < starRadius + 2.0D; r += 0.3D) {
                        double x = Math.cos(flareAngle) * r;
                        double z = Math.sin(flareAngle) * r;
                        ParticleUtils.soulFlame(center.clone().add(x, 0.0D, z), 2, 0.1D, 0.05D);
                     }
                  }

                  ParticleUtils.glow(center, 10, 1.0D, 0.02D);
                  this.rotation += 0.2D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         return displays;
      }
   }

   private void collapsePhase(final Location center, final double radius, final List<BlockDisplay> displays) {
      ParticleUtils.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0F, 0.3F);
      ParticleUtils.playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5F, 0.5F);
      (new BukkitRunnable() {
         int tick = 0;
         final int collapseDuration = 60;
         double currentRadius = radius * 0.4D;

         public void run() {
            if (this.tick >= 60) {
               this.cancel();
            } else {
               double progress = (double)this.tick / 60.0D;
               double collapseProgress = Math.pow(progress, 2.0D);
               this.currentRadius = radius * 0.4D * (1.0D - collapseProgress * 0.9D);
               double phi;
               double theta;
               double x;
               double y;
               double z;
               if (!displays.isEmpty()) {
                  BlockDisplay core = (BlockDisplay)displays.get(0);
                  if (core.isValid()) {
                     float coreScale = (float)(3.0D * (1.0D - collapseProgress * 0.8D));
                     Transformation t = core.getTransformation();
                     core.setTransformation(new Transformation(new Vector3f(-coreScale / 2.0F, -coreScale / 2.0F, -coreScale / 2.0F), t.getLeftRotation(), new Vector3f(coreScale, coreScale, coreScale), t.getRightRotation()));
                  }

                  for(int i = 1; i < displays.size(); ++i) {
                     BlockDisplay shell = (BlockDisplay)displays.get(i);
                     if (shell.isValid()) {
                        phi = Math.acos(1.0D - 2.0D * ((double)(i - 1) + 0.5D) / 12.0D);
                        theta = 3.141592653589793D * (1.0D + Math.sqrt(5.0D)) * (double)(i - 1);
                        x = this.currentRadius * Math.sin(phi) * Math.cos(theta);
                        y = this.currentRadius * Math.cos(phi);
                        z = this.currentRadius * Math.sin(phi) * Math.sin(theta);
                        shell.teleport(center.clone().add(x, y, z));
                        float shellScale = (float)(0.8D * (1.0D - collapseProgress * 0.5D));
                        Transformation tx = shell.getTransformation();
                        shell.setTransformation(new Transformation(new Vector3f(-shellScale / 2.0F, -shellScale / 2.0F, -shellScale / 2.0F), tx.getLeftRotation(), new Vector3f(shellScale, shellScale, shellScale), tx.getRightRotation()));
                     }
                  }
               }

               int coreParticles;
               double ringRadius;
               for(coreParticles = 0; coreParticles < 40; ++coreParticles) {
                  ringRadius = SupernovaImplosionAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  phi = (SupernovaImplosionAbility.this.random.nextDouble() - 0.5D) * 3.141592653589793D;
                  theta = radius * 1.5D * (1.0D - progress);
                  x = Math.cos(ringRadius) * Math.cos(phi) * theta;
                  y = Math.sin(phi) * theta;
                  z = Math.sin(ringRadius) * Math.cos(phi) * theta;
                  Location particleLoc = center.clone().add(x, y, z);
                  ParticleUtils.endRod(particleLoc, 1, 0.1D, 0.15D);
               }

               coreParticles = (int)(5.0D + progress * 30.0D);
               ParticleUtils.soulFlame(center, coreParticles, this.currentRadius + 0.5D, 0.02D);
               ParticleUtils.electricSpark(center, (int)(progress * 15.0D), this.currentRadius + 1.0D, 0.05D);
               if (this.tick % Math.max(1, (int)(10.0D - progress * 8.0D)) == 0) {
                  ParticleUtils.flash(center);
                  ParticleUtils.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.5F, 0.5F + (float)progress);
               }

               if (this.tick % 5 == 0) {
                  ringRadius = this.currentRadius + 2.0D - progress;
                  ParticleUtils.ring(center, Particle.END_ROD, ringRadius, (int)(ringRadius * 8.0D));
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void supernovaExplosion(Player player, final Location starCenter, final Location origin, final double radius, IvoryConfig.AbilitySettings settings, List<BlockDisplay> displays) {
      World world = starCenter.getWorld();
      if (world != null) {
         Iterator var9 = displays.iterator();

         while(var9.hasNext()) {
            BlockDisplay d = (BlockDisplay)var9.next();
            if (d.isValid()) {
               d.remove();
            }
         }

         ParticleUtils.playSound(starCenter, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.2F);
         ParticleUtils.playSound(starCenter, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.5F);
         ParticleUtils.playSound(starCenter, Sound.ENTITY_WITHER_DEATH, 2.0F, 0.6F);
         ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0F, 0.3F);

         for(int i = 0; i < 8; ++i) {
            double angle = 0.7853981633974483D * (double)i;
            double x = Math.cos(angle) * 4.0D;
            double z = Math.sin(angle) * 4.0D;
            ParticleUtils.flash(starCenter.clone().add(x, 0.0D, z));
         }

         ParticleUtils.flash(starCenter);
         ParticleUtils.flash(origin);
         ParticleUtils.soulFlame(starCenter, 400, 8.0D, 0.6D);
         ParticleUtils.endRod(starCenter, 300, 7.0D, 0.7D);
         ParticleUtils.firework(starCenter, 200, 6.0D, 0.8D);
         ParticleUtils.electricSpark(starCenter, 150, 10.0D, 0.5D);
         ParticleUtils.glow(starCenter, 100, 5.0D, 0.6D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  double shockRadius = (double)this.tick * 1.5D;

                  for(int i = 0; i < 100; ++i) {
                     double phi = SupernovaImplosionAbility.this.random.nextDouble() * 3.141592653589793D;
                     double theta = SupernovaImplosionAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double x = shockRadius * Math.sin(phi) * Math.cos(theta);
                     double y = shockRadius * Math.cos(phi);
                     double z = shockRadius * Math.sin(phi) * Math.sin(theta);
                     Location shockPoint = starCenter.clone().add(x, y, z);
                     if (SupernovaImplosionAbility.this.random.nextDouble() < 0.7D) {
                        ParticleUtils.soulFlame(shockPoint, 1, 0.3D, 0.0D);
                     } else {
                        ParticleUtils.endRod(shockPoint, 1, 0.2D, 0.0D);
                     }
                  }

                  ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, shockRadius, (int)(shockRadius * 12.0D) + 30);
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.5D, 0.0D), Particle.END_ROD, shockRadius * 0.95D, (int)(shockRadius * 8.0D) + 20);
                  if (this.tick % 8 == 0) {
                     ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F + (float)this.tick * 0.02F);
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

                  for(int i = 0; i < (int)(50.0D * intensity); ++i) {
                     double angle = SupernovaImplosionAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D + (double)this.tick * 0.1D;
                     double dist = SupernovaImplosionAbility.this.random.nextDouble() * radius * 0.5D * intensity;
                     double y = (SupernovaImplosionAbility.this.random.nextDouble() - 0.5D) * 4.0D * intensity;
                     double x = Math.cos(angle) * dist;
                     double z = Math.sin(angle) * dist;
                     Location remnantPoint = starCenter.clone().add(x, y, z);
                     if (SupernovaImplosionAbility.this.random.nextDouble() < 0.5D) {
                        ParticleUtils.endRod(remnantPoint, 1, 0.2D, 0.02D);
                     } else {
                        ParticleUtils.soulFlame(remnantPoint, 1, 0.15D, 0.01D);
                     }
                  }

                  ParticleUtils.glow(starCenter, (int)(20.0D * intensity), intensity * 2.0D, 0.02D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 10L, 2L);
         DamageZone zone = DamageZone.sphere(player, starCenter, radius * 1.5D, settings.damagePerTick * 4.0D, settings.damageTickInterval, 80);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }
}
