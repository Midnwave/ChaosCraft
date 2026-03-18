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

public class SupernovaGenesisAbility extends Ability {
   private final Random random = new Random();

   public SupernovaGenesisAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "supernova_genesis";
   }

   public String getDisplayName() {
      return "Supernova Genesis";
   }

   public String getDescription() {
      return "Create a miniature star that grows increasingly unstable until it explodes in a magnificent supernova of devastating power.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      this.getExecutionLocation(player);
      Location starLoc = this.getForwardLocation(player, 10.0D).add(0.0D, 5.0D, 0.0D);
      ParticleUtils.playSound(starLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.5F);
      ParticleUtils.playSound(starLoc, Sound.ENTITY_BLAZE_AMBIENT, 2.0F, 0.3F);
      this.createStar(player, starLoc, settings);
   }

   private void createStar(final Player player, final Location loc, final IvoryConfig.AbilitySettings settings) {
      final World world = loc.getWorld();
      if (world != null) {
         final BlockDisplay[] starCore = new BlockDisplay[]{(BlockDisplay)world.spawn(loc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.5F, -0.5F, -0.5F), t.getLeftRotation(), new Vector3f(1.0F, 1.0F, 1.0F), t.getRightRotation()));
         })};
         (new BukkitRunnable() {
            int tick = 0;
            final int birthPhase = 40;
            final int growthPhase = 80;
            final int unstablePhase = 40;
            final int totalTicks = 160;
            double rotation = 0.0D;
            double starSize = 0.5D;
            double pulsePhase = 0.0D;

            public void run() {
               if (this.tick >= 160) {
                  if (starCore[0] != null && starCore[0].isValid()) {
                     starCore[0].remove();
                  }

                  SupernovaGenesisAbility.this.createSupernova(player, loc, settings);
                  this.cancel();
               } else {
                  double overallProgress = (double)this.tick / 160.0D;
                  double unstableProgress;
                  int i;
                  double angle;
                  double coronaRadius;
                  double x;
                  double z;
                  if (this.tick < 40) {
                     unstableProgress = (double)this.tick / 40.0D;
                     this.starSize = 0.5D + unstableProgress * 1.5D;

                     for(i = 0; i < 30; ++i) {
                        angle = SupernovaGenesisAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                        coronaRadius = SupernovaGenesisAbility.this.random.nextDouble() * 3.141592653589793D;
                        x = 8.0D * (1.0D - unstableProgress);
                        z = Math.sin(coronaRadius) * Math.cos(angle) * x;
                        double y = Math.cos(coronaRadius) * x;
                        double zx = Math.sin(coronaRadius) * Math.sin(angle) * x;
                        Location particleLoc = loc.clone().add(z, y, zx);
                        Vector toCenter = loc.toVector().subtract(particleLoc.toVector()).normalize().multiply(0.3D);
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, toCenter.getX(), toCenter.getY(), toCenter.getZ(), 0.1D);
                     }

                     if (this.tick % 10 == 0) {
                        ParticleUtils.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, (float)(0.5D + unstableProgress * 0.30000001192092896D));
                     }
                  } else {
                     double flarePhi;
                     double flareAngle;
                     if (this.tick < 120) {
                        unstableProgress = (double)(this.tick - 40) / 80.0D;
                        this.starSize = 2.0D + unstableProgress * 3.0D;
                        ParticleUtils.sphere(loc, Particle.SOUL_FIRE_FLAME, this.starSize, (int)(this.starSize * 15.0D));
                        ParticleUtils.sphere(loc, Particle.END_ROD, this.starSize * 0.8D, (int)(this.starSize * 10.0D));
                        if (this.tick % 8 == 0) {
                           flareAngle = SupernovaGenesisAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                           flarePhi = SupernovaGenesisAbility.this.random.nextDouble() * 3.141592653589793D;
                           SupernovaGenesisAbility.this.createSolarFlare(loc, this.starSize, flareAngle, flarePhi);
                        }

                        for(i = 0; i < 5; ++i) {
                           angle = this.rotation + (double)i * 3.141592653589793D * 2.0D / 5.0D;
                           coronaRadius = this.starSize * 1.5D;
                           x = Math.cos(angle) * coronaRadius;
                           z = Math.sin(angle) * coronaRadius;
                           Location coronaLoc = loc.clone().add(x, 0.0D, z);
                           ParticleUtils.soulFlame(coronaLoc, 2, 0.2D, 0.03D);
                        }

                        if (this.tick % 20 == 0) {
                           ParticleUtils.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1.0F, 0.5F);
                        }
                     } else {
                        unstableProgress = (double)(this.tick - 40 - 80) / 40.0D;
                        flareAngle = Math.sin(this.pulsePhase * (1.0D + unstableProgress * 3.0D)) * (1.0D + unstableProgress * 2.0D);
                        this.starSize = 5.0D + flareAngle;
                        ParticleUtils.sphere(loc, Particle.SOUL_FIRE_FLAME, this.starSize, (int)(this.starSize * 20.0D));
                        ParticleUtils.sphere(loc, Particle.END_ROD, this.starSize * 0.9D, (int)(this.starSize * 15.0D));
                        ParticleUtils.electricSpark(loc, (int)(unstableProgress * 30.0D), this.starSize * 1.2D, 0.15D);
                        if (this.tick % 3 == 0) {
                           flarePhi = SupernovaGenesisAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                           double flarePhix = SupernovaGenesisAbility.this.random.nextDouble() * 3.141592653589793D;
                           SupernovaGenesisAbility.this.createSolarFlare(loc, this.starSize * 1.5D, flarePhi, flarePhix);
                        }

                        if (SupernovaGenesisAbility.this.random.nextDouble() < unstableProgress * 0.3D) {
                           ParticleUtils.flash(loc);
                        }

                        if (this.tick % 5 == 0) {
                           ParticleUtils.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, (float)(0.5D + unstableProgress), (float)(0.30000001192092896D + unstableProgress * 0.5D));
                        }

                        if (this.tick % 8 == 0) {
                           ParticleUtils.playSound(loc, Sound.ENTITY_BLAZE_HURT, 1.0F, 0.5F);
                        }

                        if (unstableProgress > 0.7D && this.tick % 10 == 0) {
                           ParticleUtils.ring(loc, Particle.END_ROD, this.starSize * 2.0D, 40);
                        }

                        this.pulsePhase += 0.3D + unstableProgress * 0.5D;
                     }
                  }

                  if (starCore[0] != null && starCore[0].isValid()) {
                     float displaySize = (float)this.starSize;
                     Transformation t = starCore[0].getTransformation();
                     starCore[0].setTransformation(new Transformation(new Vector3f(-displaySize / 2.0F, -displaySize / 2.0F, -displaySize / 2.0F), t.getLeftRotation(), new Vector3f(displaySize, displaySize, displaySize), t.getRightRotation()));
                  }

                  ParticleUtils.glow(loc, 15, this.starSize * 0.4D, 0.0D);
                  this.rotation += 0.1D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createSolarFlare(Location starLoc, double starSize, double angle, double phi) {
      World world = starLoc.getWorld();
      if (world != null) {
         double flareLength = starSize * (1.0D + this.random.nextDouble());
         double dx = Math.sin(phi) * Math.cos(angle);
         double dy = Math.cos(phi);
         double dz = Math.sin(phi) * Math.sin(angle);

         for(double d = starSize * 0.8D; d < starSize + flareLength; d += 0.3D) {
            double spread = (d - starSize * 0.8D) * 0.1D;
            Location flareLoc = starLoc.clone().add(dx * d, dy * d, dz * d);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, flareLoc, 2, spread, spread, spread, 0.02D);
            if (this.random.nextDouble() < 0.5D) {
               world.spawnParticle(Particle.END_ROD, flareLoc, 1, spread * 0.5D, spread * 0.5D, spread * 0.5D, 0.01D);
            }
         }

      }
   }

   private void createSupernova(final Player player, final Location loc, final IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.2F);
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
         ParticleUtils.playSound(loc, Sound.ENTITY_WITHER_DEATH, 2.0F, 0.6F);
         ParticleUtils.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 2.0F, 0.3F);

         for(int i = 0; i < 15; ++i) {
            this.runLater(() -> {
               ParticleUtils.flash(loc);

               for(int j = 0; j < 3; ++j) {
                  Location flashLoc = loc.clone().add((this.random.nextDouble() - 0.5D) * 20.0D, (this.random.nextDouble() - 0.5D) * 20.0D, (this.random.nextDouble() - 0.5D) * 20.0D);
                  ParticleUtils.flash(flashLoc);
               }

            }, (long)(i * 2));
         }

         ParticleUtils.soulFlame(loc, 400, 10.0D, 0.8D);
         ParticleUtils.endRod(loc, 300, 8.0D, 0.9D);
         ParticleUtils.firework(loc, 200, 6.0D, 1.0D);
         ParticleUtils.electricSpark(loc, 150, 12.0D, 0.6D);
         ParticleUtils.glow(loc, 100, 8.0D, 0.4D);
         (new BukkitRunnable() {
            int tick = 0;
            final int expansionTime = 60;

            public void run() {
               if (this.tick >= 60) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 60.0D;
                  double radius = (double)(this.tick * 2);
                  double intensity = 1.0D - progress;
                  ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 12.0D) + 40);
                  if (this.tick > 5) {
                     double secondaryRadius = (double)((this.tick - 5) * 2);
                     ParticleUtils.ring(loc, Particle.END_ROD, secondaryRadius, (int)(secondaryRadius * 8.0D) + 20);
                  }

                  double beamAngle;
                  int i;
                  for(i = -3; i <= 3; ++i) {
                     beamAngle = radius * (1.0D - (double)Math.abs(i) * 0.1D);
                     if (beamAngle > 0.0D) {
                        Location ringLoc = loc.clone().add(0.0D, (double)(i * 3), 0.0D);
                        ParticleUtils.ring(ringLoc, i == 0 ? Particle.SOUL_FIRE_FLAME : Particle.END_ROD, beamAngle, (int)(beamAngle * 6.0D) + 15);
                     }
                  }

                  if (this.tick % 2 == 0) {
                     ParticleUtils.sphere(loc, Particle.SOUL_FIRE_FLAME, radius * 0.95D, (int)(radius * 8.0D));
                  }

                  if (this.tick % 4 == 0 && this.tick < 40) {
                     for(i = 0; i < 12; ++i) {
                        beamAngle = 0.5235987755982988D * (double)i + (double)this.tick * 0.1D;
                        double beamPhi = 1.5707963267948966D;

                        for(double d = 0.0D; d < radius; d += 2.0D) {
                           double x = Math.sin(beamPhi) * Math.cos(beamAngle) * d;
                           double y = Math.cos(beamPhi) * d * 0.3D;
                           double z = Math.sin(beamPhi) * Math.sin(beamAngle) * d;
                           Location beamLoc = loc.clone().add(x, y, z);
                           ParticleUtils.endRod(beamLoc, 1, 0.2D, 0.05D);
                        }
                     }
                  }

                  if (intensity > 0.3D) {
                     ParticleUtils.soulFlame(loc, (int)(30.0D * intensity), 3.0D * intensity, 0.1D);
                     ParticleUtils.glow(loc, (int)(20.0D * intensity), 2.0D * intensity, 0.0D);
                  }

                  if (this.tick % 5 == 0 && intensity > 0.3D) {
                     ParticleUtils.flash(loc);
                  }

                  if (this.tick % 15 == 0) {
                     ParticleUtils.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, (float)(0.5D + progress * 0.5D));
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
                  double damageRadius = (double)(this.tick * 2);
                  DamageZone zone = DamageZone.sphere(player, loc, damageRadius, settings.damagePerTick * 2.0D, settings.damageTickInterval, 5);
                  DamageZoneManager.getInstance(SupernovaGenesisAbility.this.plugin).register(zone);
                  this.tick += 5;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 5L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 80) {
                  this.cancel();
               } else {
                  double intensity = 1.0D - (double)this.tick / 80.0D;
                  double radius = settings.radius * intensity;

                  for(int i = 0; i < (int)(20.0D * intensity); ++i) {
                     double theta = SupernovaGenesisAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double phi = SupernovaGenesisAbility.this.random.nextDouble() * 3.141592653589793D;
                     double dist = SupernovaGenesisAbility.this.random.nextDouble() * radius;
                     double x = Math.sin(phi) * Math.cos(theta) * dist;
                     double y = Math.cos(phi) * dist * 0.5D;
                     double z = Math.sin(phi) * Math.sin(theta) * dist;
                     Location particleLoc = loc.clone().add(x, y, z);
                     ParticleUtils.soulFlame(particleLoc, 1, 0.2D, 0.02D);
                     if (SupernovaGenesisAbility.this.random.nextDouble() < 0.2D) {
                        ParticleUtils.endRod(particleLoc, 1, 0.1D, 0.01D);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 60L, 1L);
      }
   }
}
