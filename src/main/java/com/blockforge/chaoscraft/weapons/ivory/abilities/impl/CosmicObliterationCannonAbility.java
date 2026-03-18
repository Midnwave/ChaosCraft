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

public class CosmicObliterationCannonAbility extends Ability {
   private final Random random = new Random();

   public CosmicObliterationCannonAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "cosmic_obliteration_cannon";
   }

   public String getDisplayName() {
      return "Cosmic Obliteration Cannon";
   }

   public String getDescription() {
      return "Manifest a massive cosmic cannon that charges with stellar energy before firing a beam of absolute annihilation.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double beamLength = settings.getDouble("beam-length", 60.0D);
      double beamWidth = settings.getDouble("beam-width", 4.0D);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.5F, 1.2F);
      List<BlockDisplay> cannonDisplays = this.createCannonConstruct(origin);
      this.runLater(() -> {
         this.chargeCannonEnergy(origin, cannonDisplays);
      }, 30L);
      this.runLater(() -> {
         this.fireObliterationBeam(player, origin, beamLength, beamWidth, settings, cannonDisplays);
      }, 90L);
   }

   private List<BlockDisplay> createCannonConstruct(Location origin) {
      List<BlockDisplay> displays = new ArrayList();
      World world = origin.getWorld();
      if (world == null) {
         return displays;
      } else {
         final Location cannonBase = origin.clone().add(0.0D, 1.5D, 0.0D);
         BlockDisplay barrel = (BlockDisplay)world.spawn(cannonBase, BlockDisplay.class, (d) -> {
            d.setBlock(Material.IRON_BLOCK.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.75F, -0.75F, -4.0F), t.getLeftRotation(), new Vector3f(1.5F, 1.5F, 4.0F), t.getRightRotation()));
         });
         displays.add(barrel);
         BlockDisplay core = (BlockDisplay)world.spawn(cannonBase.clone().add(0.0D, 0.0D, 1.0D), BlockDisplay.class, (d) -> {
            d.setBlock(Material.SEA_LANTERN.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-1.0F, -1.0F, -1.0F), t.getLeftRotation(), new Vector3f(2.0F, 2.0F, 2.0F), t.getRightRotation()));
         });
         displays.add(core);

         int vert;
         BlockDisplay fin;
         for(vert = -1; vert <= 1; vert += 2) {
            fin = (BlockDisplay)world.spawn(cannonBase.clone().add((double)vert * 1.5D, 0.0D, 0.0D), BlockDisplay.class, (d) -> {
               d.setBlock(Material.DIAMOND_BLOCK.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.3F, -0.5F, -2.0F), t.getLeftRotation(), new Vector3f(0.6F, 1.0F, 2.0F), t.getRightRotation()));
            });
            displays.add(fin);
         }

         for(vert = -1; vert <= 1; vert += 2) {
            fin = (BlockDisplay)world.spawn(cannonBase.clone().add(0.0D, (double)vert * 1.2D, 0.0D), BlockDisplay.class, (d) -> {
               d.setBlock(Material.GOLD_BLOCK.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.5F, -0.2F, -1.5F), t.getLeftRotation(), new Vector3f(1.0F, 0.4F, 1.5F), t.getRightRotation()));
            });
            displays.add(fin);
         }

         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  for(int i = 0; i < 15; ++i) {
                     double angle = CosmicObliterationCannonAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double dist = 5.0D - (double)this.tick * 0.15D;
                     double x = Math.cos(angle) * dist;
                     double y = (CosmicObliterationCannonAbility.this.random.nextDouble() - 0.5D) * dist;
                     double z = Math.sin(angle) * dist;
                     Location particleLoc = cannonBase.clone().add(x, y, z);
                     ParticleUtils.endRod(particleLoc, 1, 0.1D, 0.1D);
                  }

                  ParticleUtils.soulFlame(cannonBase.clone().add(0.0D, 0.0D, 1.0D), (int)((double)this.tick * 0.5D), 0.5D, 0.02D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         return displays;
      }
   }

   private void chargeCannonEnergy(final Location origin, List<BlockDisplay> displays) {
      final Location cannonBase = origin.clone().add(0.0D, 1.5D, 0.0D);
      final Location muzzle = cannonBase.clone().add(0.0D, 0.0D, -4.0D);
      ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0F, 0.3F);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 60) {
               this.cancel();
            } else {
               double chargeIntensity = (double)this.tick / 60.0D;

               int coreParticles;
               double z;
               for(coreParticles = 0; coreParticles < 4; ++coreParticles) {
                  z = this.rotation + (double)coreParticles * 3.141592653589793D / 2.0D;
                  double spiralRadius = 3.0D - chargeIntensity * 2.5D;
                  double x = Math.cos(z) * spiralRadius;
                  double y = Math.sin(z) * spiralRadius;
                  Location spiralLoc = muzzle.clone().add(x, y, 0.0D);
                  ParticleUtils.soulFlame(spiralLoc, 2, 0.1D, 0.05D);
                  ParticleUtils.electricSpark(spiralLoc, 1, 0.1D, 0.02D);
               }

               coreParticles = (int)(chargeIntensity * 20.0D);
               ParticleUtils.soulFlame(cannonBase.clone().add(0.0D, 0.0D, 1.0D), coreParticles, 0.8D, 0.03D);
               ParticleUtils.glow(cannonBase.clone().add(0.0D, 0.0D, 1.0D), coreParticles / 2, 0.6D, 0.01D);

               for(z = 0.0D; z > -4.0D; z -= 0.5D) {
                  Location barrelPoint = cannonBase.clone().add(0.0D, 0.0D, z);
                  if (CosmicObliterationCannonAbility.this.random.nextDouble() < chargeIntensity) {
                     ParticleUtils.endRod(barrelPoint, 1, 0.3D, 0.0D);
                  }
               }

               ParticleUtils.soulFlame(muzzle, (int)(chargeIntensity * 15.0D), 0.5D + chargeIntensity * 0.5D, 0.02D);
               if (this.tick % (15 - (int)(chargeIntensity * 10.0D)) == 0) {
                  ParticleUtils.flash(muzzle);
                  ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 1.0F, 0.5F + (float)chargeIntensity);
               }

               if (this.tick > 30 && this.tick % 5 == 0) {
                  ParticleUtils.electricSpark(muzzle, 15, 1.5D, 0.1D);
               }

               if (this.tick % 10 == 0) {
                  ParticleUtils.playSound(origin, Sound.ENTITY_GUARDIAN_ATTACK, 1.0F, 0.5F + (float)(chargeIntensity * 0.5D));
               }

               this.rotation += 0.2D + chargeIntensity * 0.3D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void fireObliterationBeam(final Player player, final Location origin, final double beamLength, final double beamWidth, final IvoryConfig.AbilitySettings settings, final List<BlockDisplay> cannonDisplays) {
      World world = origin.getWorld();
      if (world != null) {
         final Location muzzle = origin.clone().add(0.0D, 1.5D, -4.0D);
         ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SHOOT, 3.0F, 0.3F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.5F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GUARDIAN_ATTACK, 2.0F, 0.5F);
         ParticleUtils.flash(muzzle);
         ParticleUtils.soulFlame(muzzle, 80, 2.0D, 0.3D);
         final List<BlockDisplay> beamDisplays = new ArrayList();

         for(double z = 0.0D; z < beamLength; z += 3.0D) {
            Location beamSegmentLoc = muzzle.clone().add(0.0D, 0.0D, -z);
            BlockDisplay segment = (BlockDisplay)world.spawn(beamSegmentLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.SEA_LANTERN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f((float)(-beamWidth / 2.0D), (float)(-beamWidth / 2.0D), -1.5F), t.getLeftRotation(), new Vector3f((float)beamWidth, (float)beamWidth, 3.0F), t.getRightRotation()));
            });
            beamDisplays.add(segment);
         }

         (new BukkitRunnable() {
            int tick = 0;
            final int beamDuration = 80;
            double rotation = 0.0D;

            public void run() {
               if (this.tick >= 80) {
                  Iterator var14 = beamDisplays.iterator();

                  BlockDisplay d;
                  while(var14.hasNext()) {
                     d = (BlockDisplay)var14.next();
                     if (d.isValid()) {
                        d.remove();
                     }
                  }

                  var14 = cannonDisplays.iterator();

                  while(var14.hasNext()) {
                     d = (BlockDisplay)var14.next();
                     if (d.isValid()) {
                        d.remove();
                     }
                  }

                  Location beamEnd = muzzle.clone().add(0.0D, 0.0D, -beamLength);
                  CosmicObliterationCannonAbility.this.createBeamEndExplosion(player, beamEnd, settings);
                  this.cancel();
               } else {
                  double z;
                  Location damagePoint;
                  for(z = 0.0D; z < beamLength; z += 0.5D) {
                     damagePoint = muzzle.clone().add(0.0D, 0.0D, -z);
                     ParticleUtils.soulFlame(damagePoint, 3, beamWidth * 0.3D, 0.02D);
                     double spiralAngle = this.rotation + z * 0.2D;

                     for(int i = 0; i < 4; ++i) {
                        double angle = spiralAngle + (double)i * 3.141592653589793D / 2.0D;
                        double x = Math.cos(angle) * beamWidth * 0.4D;
                        double y = Math.sin(angle) * beamWidth * 0.4D;
                        Location edgeLoc = damagePoint.clone().add(x, y, 0.0D);
                        ParticleUtils.endRod(edgeLoc, 1, 0.05D, 0.0D);
                     }

                     if (CosmicObliterationCannonAbility.this.random.nextDouble() < 0.1D) {
                        ParticleUtils.electricSpark(damagePoint, 3, beamWidth * 0.5D, 0.05D);
                     }
                  }

                  ParticleUtils.soulFlame(muzzle, 20, 1.5D, 0.15D);
                  ParticleUtils.firework(muzzle, 5, 1.0D, 0.2D);
                  if (this.tick % 5 == 0) {
                     z = (double)(this.tick % 20 * 3);
                     if (z < beamLength) {
                        ParticleUtils.flash(muzzle.clone().add(0.0D, 0.0D, -z));
                     }
                  }

                  if (this.tick % settings.damageTickInterval == 0) {
                     for(z = 0.0D; z < beamLength; z += 5.0D) {
                        damagePoint = muzzle.clone().add(0.0D, 0.0D, -z);
                        DamageZone zone = DamageZone.sphere(player, damagePoint, beamWidth, settings.damagePerTick, 1, 5);
                        DamageZoneManager.getInstance(CosmicObliterationCannonAbility.this.plugin).register(zone);
                     }
                  }

                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(origin, Sound.ENTITY_GUARDIAN_ATTACK, 1.5F, 0.4F);
                  }

                  this.rotation += 0.15D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 10) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 0.8D;
                  ParticleUtils.ring(muzzle, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 10);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createBeamEndExplosion(Player player, final Location loc, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
      ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
      ParticleUtils.flash(loc);
      ParticleUtils.soulFlame(loc, 150, 6.0D, 0.4D);
      ParticleUtils.endRod(loc, 100, 5.0D, 0.5D);
      ParticleUtils.firework(loc, 80, 4.0D, 0.6D);
      ParticleUtils.electricSpark(loc, 60, 8.0D, 0.3D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 20) {
               this.cancel();
            } else {
               double radius = (double)this.tick * 1.2D;
               ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 10.0D) + 20);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.sphere(player, loc, settings.radius * 1.5D, settings.damagePerTick * 3.0D, settings.damageTickInterval, 40);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
