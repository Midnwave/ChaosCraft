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

public class HeavensWrathFullReleaseAbility extends Ability {
   private final Random random = new Random();

   public HeavensWrathFullReleaseAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "heavens_wrath_full_release";
   }

   public String getDisplayName() {
      return "Heaven's Wrath: Full Release";
   }

   public String getDescription() {
      return "Summon an army of divine swords in the heavens that descend in perfect unison, obliterating everything in a synchronized divine strike.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      int swordCount = settings.getInt("sword-count", 12);
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THUNDER, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.6F);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 1.5F, 1.5F);
      List<HeavensWrathFullReleaseAbility.SwordConstruct> swords = this.materializeSwords(origin, radius, swordCount);
      this.runLater(() -> {
         this.chargeSwords(swords, origin);
      }, 40L);
      this.runLater(() -> {
         this.descendSwords(player, swords, origin, settings);
      }, 80L);
   }

   private List<HeavensWrathFullReleaseAbility.SwordConstruct> materializeSwords(Location origin, double radius, int count) {
      List<HeavensWrathFullReleaseAbility.SwordConstruct> swords = new ArrayList();
      World world = origin.getWorld();
      if (world == null) {
         return swords;
      } else {
         for(int i = 0; i < count; ++i) {
            double angle = 6.283185307179586D / (double)count * (double)i;
            double dist = radius * 0.6D;
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            double y = 30.0D + this.random.nextDouble() * 10.0D;
            final Location swordLoc = origin.clone().add(x, y, z);
            BlockDisplay blade = (BlockDisplay)world.spawn(swordLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.DIAMOND_BLOCK.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.15F, 0.0F, -0.15F), t.getLeftRotation(), new Vector3f(0.3F, 5.0F, 0.3F), t.getRightRotation()));
            });
            BlockDisplay hilt = (BlockDisplay)world.spawn(swordLoc.clone().add(0.0D, 5.5D, 0.0D), BlockDisplay.class, (d) -> {
               d.setBlock(Material.GOLD_BLOCK.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.3F, -0.1F, -0.3F), t.getLeftRotation(), new Vector3f(0.6F, 0.2F, 0.6F), t.getRightRotation()));
            });
            HeavensWrathFullReleaseAbility.SwordConstruct sword = new HeavensWrathFullReleaseAbility.SwordConstruct(swordLoc, angle, blade, hilt);
            swords.add(sword);
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick >= 40) {
                     this.cancel();
                  } else {
                     for(int j = 0; j < 5; ++j) {
                        double offsetX = (HeavensWrathFullReleaseAbility.this.random.nextDouble() - 0.5D) * 3.0D;
                        double offsetY = (HeavensWrathFullReleaseAbility.this.random.nextDouble() - 0.5D) * 6.0D;
                        double offsetZ = (HeavensWrathFullReleaseAbility.this.random.nextDouble() - 0.5D) * 3.0D;
                        Location particleLoc = swordLoc.clone().add(offsetX, offsetY, offsetZ);
                        double progress = (double)this.tick / 40.0D;
                        Location targetLoc = swordLoc.clone().add(0.0D, 2.5D, 0.0D);
                        particleLoc.add(targetLoc.toVector().subtract(particleLoc.toVector()).multiply(progress));
                        ParticleUtils.endRod(particleLoc, 1, 0.1D, 0.0D);
                     }

                     ParticleUtils.soulFlame(swordLoc.clone().add(0.0D, 2.5D, 0.0D), 3, 0.2D, 0.01D);
                     ++this.tick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
         }

         return swords;
      }
   }

   private void chargeSwords(final List<HeavensWrathFullReleaseAbility.SwordConstruct> swords, final Location origin) {
      ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0F, 0.5F);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               Iterator var1 = swords.iterator();

               while(true) {
                  HeavensWrathFullReleaseAbility.SwordConstruct sword;
                  do {
                     if (!var1.hasNext()) {
                        if (this.tick > 20) {
                           Location center = origin.clone().add(0.0D, 40.0D, 0.0D);
                           ParticleUtils.soulFlame(center, 15, 1.5D, 0.05D);
                           ParticleUtils.glow(center, 10, 1.0D, 0.02D);
                           if (this.tick % 5 == 0) {
                              ParticleUtils.flash(center);
                           }
                        }

                        ++this.tick;
                        return;
                     }

                     sword = (HeavensWrathFullReleaseAbility.SwordConstruct)var1.next();
                  } while(!sword.blade.isValid());

                  double spiralAngle = (double)this.tick * 0.3D + sword.angle;

                  for(int i = 0; i < 3; ++i) {
                     double angle = spiralAngle + (double)i * 3.141592653589793D * 2.0D / 3.0D;
                     double spiralRadius = 0.8D;
                     double x = Math.cos(angle) * spiralRadius;
                     double z = Math.sin(angle) * spiralRadius;
                     Location spiralLoc = sword.location.clone().add(x, 2.5D + Math.sin((double)this.tick * 0.2D) * 2.0D, z);
                     ParticleUtils.electricSpark(spiralLoc, 2, 0.1D, 0.02D);
                  }

                  ParticleUtils.soulFlame(sword.location.clone().add(0.0D, 2.5D, 0.0D), 5, 0.3D, 0.02D);
                  if (this.tick > 20) {
                     Location centerAbove = origin.clone().add(0.0D, 40.0D, 0.0D);
                     ParticleUtils.line(sword.location.clone().add(0.0D, 5.0D, 0.0D), centerAbove, Particle.END_ROD, 1.0D);
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void descendSwords(final Player player, final List<HeavensWrathFullReleaseAbility.SwordConstruct> swords, final Location origin, final IvoryConfig.AbilitySettings settings) {
      final World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0F, 0.4F);
         ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_3, 2.0F, 0.6F);
         Iterator var6 = swords.iterator();

         while(var6.hasNext()) {
            HeavensWrathFullReleaseAbility.SwordConstruct sword = (HeavensWrathFullReleaseAbility.SwordConstruct)var6.next();
            ParticleUtils.flash(sword.location);
         }

         (new BukkitRunnable() {
            int tick = 0;
            final double speed = 3.0D;
            boolean impacted = false;

            public void run() {
               Iterator var1;
               HeavensWrathFullReleaseAbility.SwordConstruct sword;
               if (this.tick < 30 && !this.impacted) {
                  var1 = swords.iterator();

                  while(var1.hasNext()) {
                     sword = (HeavensWrathFullReleaseAbility.SwordConstruct)var1.next();
                     sword.location.add(0.0D, -3.0D, 0.0D);
                     if (sword.blade.isValid()) {
                        sword.blade.teleport(sword.location);
                     }

                     if (sword.hilt.isValid()) {
                        sword.hilt.teleport(sword.location.clone().add(0.0D, 5.5D, 0.0D));
                     }

                     ParticleUtils.soulFlame(sword.location.clone().add(0.0D, 6.0D, 0.0D), 5, 0.2D, 0.05D);
                     ParticleUtils.endRod(sword.location.clone().add(0.0D, 7.0D, 0.0D), 3, 0.15D, 0.02D);
                     double groundY = (double)world.getHighestBlockYAt(sword.location);
                     if (sword.location.getY() <= groundY + 1.0D && !this.impacted) {
                        this.impacted = true;
                        HeavensWrathFullReleaseAbility.this.createMassImpact(player, origin, swords, settings);
                     }
                  }

                  if (this.tick % 5 == 0) {
                     ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THROW, 1.5F, 0.8F);
                  }

                  ++this.tick;
               } else {
                  var1 = swords.iterator();

                  while(var1.hasNext()) {
                     sword = (HeavensWrathFullReleaseAbility.SwordConstruct)var1.next();
                     if (sword.blade.isValid()) {
                        sword.blade.remove();
                     }

                     if (sword.hilt.isValid()) {
                        sword.hilt.remove();
                     }
                  }

                  if (!this.impacted) {
                     HeavensWrathFullReleaseAbility.this.createMassImpact(player, origin, swords, settings);
                  }

                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createMassImpact(Player player, final Location origin, final List<HeavensWrathFullReleaseAbility.SwordConstruct> swords, IvoryConfig.AbilitySettings settings) {
      final World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.6F);
         ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.5F, 0.4F);
         Iterator var6 = swords.iterator();

         while(var6.hasNext()) {
            HeavensWrathFullReleaseAbility.SwordConstruct sword = (HeavensWrathFullReleaseAbility.SwordConstruct)var6.next();
            final Location impactLoc = sword.location.clone();
            impactLoc.setY((double)world.getHighestBlockYAt(impactLoc) + 0.5D);
            ParticleUtils.flash(impactLoc);
            ParticleUtils.soulFlame(impactLoc, 40, 2.0D, 0.2D);
            ParticleUtils.endRod(impactLoc, 25, 1.5D, 0.25D);
            ParticleUtils.electricSpark(impactLoc, 20, 2.5D, 0.15D);
            (new BukkitRunnable() {
               int waveTick = 0;

               public void run() {
                  if (this.waveTick >= 10) {
                     this.cancel();
                  } else {
                     ParticleUtils.ring(impactLoc, Particle.SOUL_FIRE_FLAME, (double)this.waveTick * 0.5D, this.waveTick * 6 + 8);
                     ++this.waveTick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
         }

         ParticleUtils.flash(origin);
         ParticleUtils.soulFlame(origin, 200, 8.0D, 0.4D);
         ParticleUtils.endRod(origin.clone().add(0.0D, 3.0D, 0.0D), 150, 6.0D, 0.5D);
         ParticleUtils.firework(origin.clone().add(0.0D, 5.0D, 0.0D), 100, 5.0D, 0.6D);
         ParticleUtils.electricSpark(origin, 80, 10.0D, 0.3D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 1.5D;
                  ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 10.0D) + 30);
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.5D, 0.0D), Particle.END_ROD, radius * 0.9D, (int)(radius * 6.0D) + 15);

                  for(int h = 1; h <= 3; ++h) {
                     double heightRadius = radius * (1.0D - (double)h * 0.2D);
                     if (heightRadius > 0.0D) {
                        ParticleUtils.ring(origin.clone().add(0.0D, (double)(h * 3), 0.0D), Particle.END_ROD, heightRadius, (int)(heightRadius * 5.0D));
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
                  Iterator var3 = swords.iterator();

                  while(true) {
                     Location echoLoc;
                     do {
                        if (!var3.hasNext()) {
                           ++this.tick;
                           return;
                        }

                        HeavensWrathFullReleaseAbility.SwordConstruct sword = (HeavensWrathFullReleaseAbility.SwordConstruct)var3.next();
                        echoLoc = sword.location.clone();
                        echoLoc.setY((double)world.getHighestBlockYAt(echoLoc) + 0.5D);
                     } while(!(HeavensWrathFullReleaseAbility.this.random.nextDouble() < intensity));

                     for(double y = 0.0D; y < 5.0D * intensity; y += 0.5D) {
                        ParticleUtils.endRod(echoLoc.clone().add(0.0D, y, 0.0D), 1, 0.1D, 0.0D);
                     }
                  }
               }
            }
         }).runTaskTimer(this.plugin, 5L, 2L);
         DamageZone zone = DamageZone.cylinder(player, origin, settings.radius, 15.0D, settings.damagePerTick * 2.5D, settings.damageTickInterval, 60);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private static class SwordConstruct {
      Location location;
      double angle;
      BlockDisplay blade;
      BlockDisplay hilt;

      SwordConstruct(Location location, double angle, BlockDisplay blade, BlockDisplay hilt) {
         this.location = location;
         this.angle = angle;
         this.blade = blade;
         this.hilt = hilt;
      }
   }
}
