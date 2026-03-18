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

public class StellarGuillotineAbility extends Ability {
   public StellarGuillotineAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "stellar_guillotine";
   }

   public String getDisplayName() {
      return "Stellar Guillotine";
   }

   public String getDescription() {
      return "Launch a massive spinning disc of crystallized starlight that slices through all enemies in its path, leaving trails of celestial fire.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double discWidth = settings.getDouble("disc-width", 10.0D);
      double travelDistance = settings.getDouble("travel-distance", 50.0D);
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THROW, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.2F);
      ParticleUtils.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2.0F, 0.7F);
      this.createDiscFormation(origin, discWidth, () -> {
         this.launchDisc(player, origin, discWidth, travelDistance, settings);
      });
   }

   private void createDiscFormation(final Location origin, final double discWidth, final Runnable onComplete) {
      (new BukkitRunnable() {
         int tick = 0;
         final int formationTime = 20;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 20) {
               onComplete.run();
               this.cancel();
            } else {
               double progress = (double)this.tick / 20.0D;
               double currentRadius = discWidth / 2.0D * progress;
               Location discCenter = origin.clone().add(0.0D, 1.5D, 0.0D);

               for(int i = 0; i < 30; ++i) {
                  double angle = Math.random() * 3.141592653589793D * 2.0D;
                  double dist = currentRadius + (discWidth - currentRadius) * (1.0D - progress) * Math.random();
                  double x = Math.cos(angle) * dist;
                  double z = Math.sin(angle) * dist;
                  Location particleLoc = discCenter.clone().add(x, (Math.random() - 0.5D) * 0.3D, z);
                  ParticleUtils.soulFlame(particleLoc, 1, 0.1D, 0.02D);
               }

               StellarGuillotineAbility.this.drawDiscOutline(discCenter, currentRadius, this.rotation, Particle.END_ROD, 40);
               StellarGuillotineAbility.this.drawDiscOutline(discCenter, currentRadius * 0.7D, -this.rotation * 1.5D, Particle.SOUL_FIRE_FLAME, 30);
               StellarGuillotineAbility.this.drawDiscOutline(discCenter, currentRadius * 0.4D, this.rotation * 2.0D, Particle.ELECTRIC_SPARK, 20);
               if (this.tick % 3 == 0) {
                  ParticleUtils.flash(discCenter);
               }

               ParticleUtils.glow(discCenter, 5, 0.3D, 0.01D);
               if (this.tick % 5 == 0) {
                  ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5F, (float)(0.800000011920929D + progress * 0.4000000059604645D));
               }

               this.rotation += 0.3D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void drawDiscOutline(Location center, double radius, double rotation, Particle particle, int points) {
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

   private void launchDisc(final Player player, final Location origin, final double discWidth, final double travelDistance, final IvoryConfig.AbilitySettings settings) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SHOOT, 2.0F, 0.5F);
         ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5F, 0.8F);
         (new BukkitRunnable() {
            double traveled = 0.0D;
            final double speed = 2.0D;
            double rotation = 0.0D;
            final double spinSpeed = 0.8D;
            Location currentLoc = origin.clone().add(0.0D, 1.5D, 0.0D);
            final List<BlockDisplay> discDisplays = new ArrayList();
            boolean displaysCreated = false;

            public void run() {
               if (this.traveled >= travelDistance) {
                  StellarGuillotineAbility.this.createImpactExplosion(this.currentLoc, discWidth, settings, player);
                  Iterator var3 = this.discDisplays.iterator();

                  while(var3.hasNext()) {
                     BlockDisplay display = (BlockDisplay)var3.next();
                     if (display.isValid()) {
                        display.remove();
                     }
                  }

                  this.cancel();
               } else {
                  this.currentLoc.add(0.0D, 0.0D, -2.0D);
                  this.traveled += 2.0D;
                  if (!this.displaysCreated) {
                     StellarGuillotineAbility.this.createDiscStructure(this.currentLoc, discWidth, this.discDisplays);
                     this.displaysCreated = true;
                  }

                  StellarGuillotineAbility.this.updateDiscDisplays(this.currentLoc, this.rotation, this.discDisplays);
                  StellarGuillotineAbility.this.drawSpinningDisc(this.currentLoc, discWidth / 2.0D, this.rotation);
                  StellarGuillotineAbility.this.createTrailParticles(this.currentLoc.clone().add(0.0D, 0.0D, 2.0D), discWidth);
                  if ((int)this.traveled % 5 == 0) {
                     DamageZone zone = DamageZone.cylinder(player, this.currentLoc.clone(), discWidth / 2.0D, 1.0D, settings.damagePerTick, settings.damageTickInterval, 10);
                     DamageZoneManager.getInstance(StellarGuillotineAbility.this.plugin).register(zone);
                  }

                  if ((int)this.traveled % 10 == 0) {
                     ParticleUtils.playSound(this.currentLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.2F);
                  }

                  this.rotation += 0.8D;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createDiscStructure(Location center, double discWidth, List<BlockDisplay> displays) {
      World world = center.getWorld();
      if (world != null) {
         int blockCount = 8;
         double radius = discWidth / 3.0D;

         for(int i = 0; i < blockCount; ++i) {
            double angle = 6.283185307179586D / (double)blockCount * (double)i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location blockLoc = center.clone().add(x, 0.0D, z);
            BlockDisplay display = (BlockDisplay)world.spawn(blockLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.SEA_LANTERN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation transform = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.25F, -0.1F, -0.25F), transform.getLeftRotation(), new Vector3f(0.5F, 0.2F, 0.5F), transform.getRightRotation()));
            });
            displays.add(display);
         }

         BlockDisplay core = (BlockDisplay)world.spawn(center, BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation transform = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.3F, -0.15F, -0.3F), transform.getLeftRotation(), new Vector3f(0.6F, 0.3F, 0.6F), transform.getRightRotation()));
         });
         displays.add(core);
      }
   }

   private void updateDiscDisplays(Location center, double rotation, List<BlockDisplay> displays) {
      if (!displays.isEmpty()) {
         int blockCount = displays.size() - 1;
         double radius = 3.0D;

         for(int i = 0; i < blockCount; ++i) {
            BlockDisplay display = (BlockDisplay)displays.get(i);
            if (display.isValid()) {
               double angle = 6.283185307179586D / (double)blockCount * (double)i + rotation;
               double x = Math.cos(angle) * radius;
               double z = Math.sin(angle) * radius;
               Location blockLoc = center.clone().add(x, 0.0D, z);
               display.teleport(blockLoc);
            }
         }

         BlockDisplay core = (BlockDisplay)displays.get(displays.size() - 1);
         if (core.isValid()) {
            core.teleport(center);
         }

      }
   }

   private void drawSpinningDisc(Location center, double radius, double rotation) {
      World world = center.getWorld();
      if (world != null) {
         int blade;
         double bladeAngle;
         double r;
         double x;
         Location point;
         for(blade = 0; blade < 60; ++blade) {
            bladeAngle = 0.10471975511965977D * (double)blade + rotation;
            r = Math.cos(bladeAngle) * radius;
            x = Math.sin(bladeAngle) * radius;
            point = center.clone().add(r, 0.0D, x);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.05D, 0.02D, 0.05D, 0.0D);
            world.spawnParticle(Particle.END_ROD, point, 1, 0.02D, 0.01D, 0.02D, 0.0D);
         }

         for(blade = 0; blade < 40; ++blade) {
            bladeAngle = 0.15707963267948966D * (double)blade - rotation * 1.5D;
            r = Math.cos(bladeAngle) * radius * 0.6D;
            x = Math.sin(bladeAngle) * radius * 0.6D;
            point = center.clone().add(r, 0.0D, x);
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.03D, 0.01D, 0.03D, 0.0D);
         }

         for(blade = 0; blade < 4; ++blade) {
            bladeAngle = rotation + (double)blade * 3.141592653589793D / 2.0D;

            for(r = 0.0D; r < radius; r += 0.3D) {
               x = Math.cos(bladeAngle) * r;
               double z = Math.sin(bladeAngle) * r;
               point = center.clone().add(x, 0.0D, z);
               world.spawnParticle(Particle.END_ROD, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
         }

         ParticleUtils.glow(center, 3, 0.2D, 0.0D);
         if (Math.random() < 0.1D) {
            ParticleUtils.flash(center);
         }

      }
   }

   private void createTrailParticles(Location trailLoc, double width) {
      World world = trailLoc.getWorld();
      if (world != null) {
         ParticleUtils.soulFlame(trailLoc, 15, width / 4.0D, 0.02D);
         ParticleUtils.endRod(trailLoc, 8, width / 5.0D, 0.01D);
         ParticleUtils.whiteAsh(trailLoc, 20, width / 3.0D, 0.03D);
         Location groundLoc = trailLoc.clone();
         groundLoc.setY((double)trailLoc.getWorld().getHighestBlockYAt(trailLoc) + 0.1D);
         ParticleUtils.soulFlame(groundLoc, 5, width / 4.0D, 0.01D);
      }
   }

   private void createImpactExplosion(final Location loc, final double discWidth, IvoryConfig.AbilitySettings settings, Player player) {
      World world = loc.getWorld();
      if (world != null) {
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.6F);
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.8F);
         ParticleUtils.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0F, 0.5F);
         ParticleUtils.flash(loc);
         ParticleUtils.soulFlame(loc, 150, discWidth / 2.0D, 0.3D);
         ParticleUtils.endRod(loc, 100, discWidth / 2.0D, 0.4D);
         ParticleUtils.electricSpark(loc, 80, discWidth / 2.0D, 0.25D);
         ParticleUtils.firework(loc, 60, discWidth / 3.0D, 0.5D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 15) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 1.5D;
                  ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 6.0D) + 10);
                  ParticleUtils.ring(loc.clone().add(0.0D, 0.5D, 0.0D), Particle.END_ROD, radius * 0.8D, (int)(radius * 4.0D) + 5);
                  if (this.tick % 2 == 0) {
                     ParticleUtils.flash(loc);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.sphere(player, loc, discWidth, settings.damagePerTick * 2.0D, settings.damageTickInterval, 30);
         DamageZoneManager.getInstance(this.plugin).register(zone);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  double intensity = 1.0D - (double)this.tick / 40.0D;
                  ParticleUtils.soulFlame(loc, (int)(20.0D * intensity), discWidth / 3.0D, 0.02D);
                  ParticleUtils.whiteAsh(loc, (int)(15.0D * intensity), discWidth / 2.0D, 0.01D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 2L);
      }
   }
}
