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
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TitansFistOfJudgmentAbility extends Ability {
   private final Random random = new Random();

   public TitansFistOfJudgmentAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "titans_fist_of_judgment";
   }

   public String getDisplayName() {
      return "Titan's Fist of Judgment";
   }

   public String getDescription() {
      return "Call down the judgment of the divine titans - a colossal fist of crystallized light that descends to obliterate all beneath it.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      double fistSize = settings.getDouble("fist-size", 8.0D);
      Location fistTarget = origin.clone();
      Location fistStart = origin.clone().add(0.0D, 50.0D, 0.0D);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.4F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.5F);
      this.createWarningZone(origin, fistSize);
      List<BlockDisplay> fistDisplays = this.createFist(fistStart, fistSize);
      this.runLater(() -> {
         this.chargeFist(fistStart, fistDisplays);
      }, 40L);
      this.runLater(() -> {
         this.descendFist(player, fistStart, fistTarget, fistSize, settings, fistDisplays);
      }, 80L);
   }

   private void createWarningZone(final Location target, final double size) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 80) {
               this.cancel();
            } else {
               double pulseScale = 1.0D + Math.sin((double)this.tick * 0.2D) * 0.15D;
               double warningRadius = size * 0.5D * pulseScale;
               ParticleUtils.ring(target.clone().add(0.0D, 0.1D, 0.0D), Particle.END_ROD, warningRadius, (int)(warningRadius * 10.0D) + 20);
               ParticleUtils.ring(target.clone().add(0.0D, 0.1D, 0.0D), Particle.SOUL_FIRE_FLAME, warningRadius * 0.6D, (int)(warningRadius * 6.0D) + 10);
               double fistRadius = size * 0.4D;

               for(int i = 0; i < 5; ++i) {
                  double angle = this.rotation + (double)i * 3.141592653589793D / 3.0D - 1.0471975511965976D;
                  double xx = Math.cos(angle) * fistRadius * 0.8D;
                  double z = Math.sin(angle) * fistRadius * 0.6D - fistRadius * 0.3D;
                  ParticleUtils.soulFlame(target.clone().add(xx, 0.1D, z), 2, 0.3D, 0.01D);
               }

               for(double x = -fistRadius; x <= fistRadius; x += 0.5D) {
                  for(double zx = -fistRadius * 0.8D; zx <= fistRadius * 0.5D; zx += 0.5D) {
                     if (TitansFistOfJudgmentAbility.this.random.nextDouble() < 0.05D) {
                        ParticleUtils.endRod(target.clone().add(x, 0.1D, zx), 1, 0.1D, 0.0D);
                     }
                  }
               }

               this.rotation += 0.02D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private List<BlockDisplay> createFist(final Location center, final double size) {
      List<BlockDisplay> displays = new ArrayList();
      World world = center.getWorld();
      if (world == null) {
         return displays;
      } else {
         BlockDisplay palm = (BlockDisplay)world.spawn(center, BlockDisplay.class, (d) -> {
            d.setBlock(Material.QUARTZ_BLOCK.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            float s = (float)size;
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-s * 0.4F, -s * 0.3F, -s * 0.3F), t.getLeftRotation(), new Vector3f(s * 0.8F, s * 0.6F, s * 0.6F), t.getRightRotation()));
         });
         displays.add(palm);

         for(int i = 0; i < 4; ++i) {
            double fingerX = -size * 0.3D + (double)i * size * 0.2D;
            Location fingerLoc = center.clone().add(fingerX, -size * 0.3D, -size * 0.25D);
            BlockDisplay finger = (BlockDisplay)world.spawn(fingerLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.QUARTZ_BLOCK.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               float fingerLen = (float)(size * 0.5D);
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.3F, -fingerLen, -0.3F), (new Quaternionf()).rotateX((float)Math.toRadians(15.0D)), new Vector3f(0.6F, fingerLen, 0.6F), t.getRightRotation()));
            });
            displays.add(finger);
         }

         Location thumbLoc = center.clone().add(size * 0.35D, -size * 0.15D, 0.0D);
         BlockDisplay thumb = (BlockDisplay)world.spawn(thumbLoc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.QUARTZ_BLOCK.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            float thumbLen = (float)(size * 0.35D);
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.3F, -thumbLen * 0.5F, -0.25F), (new Quaternionf()).rotateZ((float)Math.toRadians(-30.0D)), new Vector3f(0.6F, thumbLen, 0.5F), t.getRightRotation()));
         });
         displays.add(thumb);

         for(int i = 0; i < 4; ++i) {
            double knuckleX = -size * 0.3D + (double)i * size * 0.2D;
            Location knuckleLoc = center.clone().add(knuckleX, -size * 0.35D, -size * 0.3D);
            BlockDisplay knuckle = (BlockDisplay)world.spawn(knuckleLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.SEA_LANTERN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.2F, -0.15F, -0.2F), t.getLeftRotation(), new Vector3f(0.4F, 0.3F, 0.4F), t.getRightRotation()));
            });
            displays.add(knuckle);
         }

         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  for(int i = 0; i < 15; ++i) {
                     double angle = TitansFistOfJudgmentAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double dist = 8.0D - (double)this.tick * 0.2D;
                     double y = (TitansFistOfJudgmentAbility.this.random.nextDouble() - 0.5D) * 6.0D;
                     double x = Math.cos(angle) * dist;
                     double z = Math.sin(angle) * dist;
                     Location particleLoc = center.clone().add(x, y, z);
                     ParticleUtils.endRod(particleLoc, 1, 0.15D, 0.15D);
                  }

                  ParticleUtils.soulFlame(center, 10, size * 0.3D, 0.02D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         return displays;
      }
   }

   private void chargeFist(final Location fistLoc, List<BlockDisplay> displays) {
      ParticleUtils.playSound(fistLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0F, 0.4F);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               double intensity = (double)this.tick / 40.0D;

               for(int i = 0; i < (int)(20.0D * intensity); ++i) {
                  double angle = (double)this.tick * 0.15D + (double)i * 0.3D;
                  double dist = 5.0D - intensity * 2.0D;
                  double x = Math.cos(angle) * dist;
                  double y = (TitansFistOfJudgmentAbility.this.random.nextDouble() - 0.5D) * 8.0D;
                  double z = Math.sin(angle) * dist;
                  Location spiralLoc = fistLoc.clone().add(x, y, z);
                  ParticleUtils.electricSpark(spiralLoc, 1, 0.2D, 0.08D);
               }

               ParticleUtils.soulFlame(fistLoc, (int)(15.0D * intensity), 3.0D, 0.02D);
               ParticleUtils.glow(fistLoc, (int)(10.0D * intensity), 2.0D, 0.01D);
               if (this.tick % (10 - (int)(intensity * 7.0D)) == 0) {
                  ParticleUtils.flash(fistLoc);
                  ParticleUtils.playSound(fistLoc, Sound.BLOCK_BEACON_AMBIENT, 1.0F, 0.5F + (float)intensity * 0.5F);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void descendFist(final Player player, final Location start, final Location target, final double size, final IvoryConfig.AbilitySettings settings, final List<BlockDisplay> displays) {
      ParticleUtils.playSound(start, Sound.ITEM_TRIDENT_RIPTIDE_3, 2.5F, 0.4F);
      ParticleUtils.playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F);
      (new BukkitRunnable() {
         int tick = 0;
         Location currentLoc = start.clone();
         double speed = 3.0D;
         boolean impacted = false;

         public void run() {
            World world = target.getWorld();
            if (world != null && !this.impacted) {
               double groundY = (double)(world.getHighestBlockYAt(target) + 1);
               if (this.currentLoc.getY() <= groundY) {
                  this.impacted = true;
                  TitansFistOfJudgmentAbility.this.createImpact(player, target, size, settings, displays);
                  this.cancel();
               } else {
                  this.speed = Math.min(this.speed + 0.3D, 6.0D);
                  this.currentLoc.add(0.0D, -this.speed, 0.0D);
                  double yOffset = this.currentLoc.getY() - start.getY();
                  Iterator var6 = displays.iterator();

                  Location trailLoc;
                  while(var6.hasNext()) {
                     BlockDisplay display = (BlockDisplay)var6.next();
                     if (display.isValid()) {
                        trailLoc = display.getLocation().add(0.0D, -this.speed, 0.0D);
                        display.teleport(trailLoc);
                     }
                  }

                  ParticleUtils.soulFlame(this.currentLoc, 20, size * 0.4D, 0.08D);
                  ParticleUtils.endRod(this.currentLoc.clone().add(0.0D, size * 0.3D, 0.0D), 15, size * 0.3D, 0.05D);

                  for(double y = 0.0D; y < 10.0D; ++y) {
                     trailLoc = this.currentLoc.clone().add(0.0D, y, 0.0D);
                     ParticleUtils.endRod(trailLoc, 3, size * 0.2D, 0.02D);
                  }

                  if (this.tick % 5 == 0) {
                     ParticleUtils.playSound(this.currentLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 0.5F);
                  }

                  ++this.tick;
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createImpact(Player player, Location target, final double size, IvoryConfig.AbilitySettings settings, List<BlockDisplay> displays) {
      World world = target.getWorld();
      if (world != null) {
         final Location groundLoc = target.clone();
         groundLoc.setY((double)world.getHighestBlockYAt(target) + 0.5D);
         this.runLater(() -> {
            Iterator var1 = displays.iterator();

            while(var1.hasNext()) {
               BlockDisplay d = (BlockDisplay)var1.next();
               if (d.isValid()) {
                  d.remove();
               }
            }

         }, 20L);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.2F);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.4F);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0F, 0.5F);
         ParticleUtils.playSound(groundLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.5F, 0.3F);

         for(int i = 0; i < 5; ++i) {
            double x = (this.random.nextDouble() - 0.5D) * size;
            double z = (this.random.nextDouble() - 0.5D) * size;
            ParticleUtils.flash(groundLoc.clone().add(x, (double)(2 + i), z));
         }

         ParticleUtils.soulFlame(groundLoc, 300, size, 0.5D);
         ParticleUtils.endRod(groundLoc.clone().add(0.0D, 2.0D, 0.0D), 250, size * 0.8D, 0.6D);
         ParticleUtils.firework(groundLoc.clone().add(0.0D, 4.0D, 0.0D), 200, size * 0.6D, 0.7D);
         ParticleUtils.electricSpark(groundLoc, 150, size * 1.5D, 0.4D);
         ParticleUtils.glow(groundLoc.clone().add(0.0D, 1.0D, 0.0D), 100, size * 0.5D, 0.5D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  double radius = (double)(this.tick * 2);
                  ParticleUtils.ring(groundLoc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 12.0D) + 40);
                  ParticleUtils.ring(groundLoc.clone().add(0.0D, 0.5D, 0.0D), Particle.END_ROD, radius * 0.95D, (int)(radius * 8.0D) + 25);
                  if (this.tick < 15) {
                     for(int i = 0; i < 20; ++i) {
                        double angle = TitansFistOfJudgmentAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                        double dist = TitansFistOfJudgmentAbility.this.random.nextDouble() * radius;
                        double x = Math.cos(angle) * dist;
                        double z = Math.sin(angle) * dist;
                        double y = TitansFistOfJudgmentAbility.this.random.nextDouble() * (double)(15 - this.tick);
                        Location debrisLoc = groundLoc.clone().add(x, y, z);
                        ParticleUtils.soulFlame(debrisLoc, 1, 0.3D, 0.1D);
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
                  double fistRadius = size * 0.4D;

                  for(int i = 0; i < (int)(20.0D * intensity); ++i) {
                     double x = (TitansFistOfJudgmentAbility.this.random.nextDouble() - 0.5D) * fistRadius * 2.0D;
                     double z = (TitansFistOfJudgmentAbility.this.random.nextDouble() - 0.5D) * fistRadius * 1.5D;
                     Location imprintLoc = groundLoc.clone().add(x, 0.1D, z);
                     ParticleUtils.endRod(imprintLoc, 1, 0.2D, 0.02D);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 10L, 2L);
         DamageZone zone = DamageZone.cylinder(player, groundLoc, settings.radius * 1.5D, 10.0D, settings.damagePerTick * 4.0D, settings.damageTickInterval, 80);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }
}
