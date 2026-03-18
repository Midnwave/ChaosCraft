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

public class RagnarokOfLightAbility extends Ability {
   private final Random random = new Random();

   public RagnarokOfLightAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "ragnarok_of_light";
   }

   public String getDisplayName() {
      return "Ragnarok of Light";
   }

   public String getDescription() {
      return "Invoke the divine apocalypse, summoning ancient runic symbols that erupt into a world-ending pillar of pure celestial annihilation.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_END_PORTAL_SPAWN, 1.5F, 0.8F);
      this.createRunicCircle(origin, radius, () -> {
         this.igniteSymbols(origin, radius, () -> {
            this.createApocalypsePillar(player, origin, radius, settings);
         });
      });
   }

   private void createRunicCircle(final Location origin, final double radius, final Runnable onComplete) {
      final List<BlockDisplay> runeDisplays = new ArrayList();
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;
         double currentRadius = 0.0D;

         public void run() {
            if (this.tick >= 40) {
               onComplete.run();
               this.cancel();
            } else {
               World world = origin.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  this.currentRadius = radius * ((double)this.tick / 40.0D);

                  int i;
                  double angle1;
                  double angle2;
                  double zx;
                  Location pillarLoc;
                  for(i = 0; i < 80; ++i) {
                     angle1 = 0.07853981633974483D * (double)i + this.rotation;
                     angle2 = Math.cos(angle1) * this.currentRadius;
                     zx = Math.sin(angle1) * this.currentRadius;
                     pillarLoc = origin.clone().add(angle2, 0.1D, zx);
                     world.spawnParticle(Particle.SOUL_FIRE_FLAME, pillarLoc, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                  }

                  for(double r = this.currentRadius * 0.3D; r < this.currentRadius; r += this.currentRadius * 0.2D) {
                     for(int ix = 0; ix < (int)(r * 5.0D); ++ix) {
                        angle2 = 6.283185307179586D / (r * 5.0D) * (double)ix - this.rotation * 0.5D;
                        zx = Math.cos(angle2) * r;
                        double z = Math.sin(angle2) * r;
                        Location point = origin.clone().add(zx, 0.1D, z);
                        world.spawnParticle(Particle.END_ROD, point, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                     }
                  }

                  if (this.tick == 20) {
                     for(i = 0; i < 8; ++i) {
                        angle1 = 0.7853981633974483D * (double)i;
                        angle2 = Math.cos(angle1) * radius * 0.8D;
                        zx = Math.sin(angle1) * radius * 0.8D;
                        pillarLoc = origin.clone().add(angle2, 0.0D, zx);
                        BlockDisplay pillar = (BlockDisplay)world.spawn(pillarLoc, BlockDisplay.class, (d) -> {
                           d.setBlock(Material.CRYING_OBSIDIAN.createBlockData());
                           d.setGlowing(true);
                           d.setBrightness(new Brightness(15, 15));
                           Transformation t = d.getTransformation();
                           d.setTransformation(new Transformation(new Vector3f(-0.25F, 0.0F, -0.25F), t.getLeftRotation(), new Vector3f(0.5F, 3.0F, 0.5F), t.getRightRotation()));
                        });
                        runeDisplays.add(pillar);
                     }
                  }

                  if (this.tick > 20) {
                     for(i = 0; i < 8; ++i) {
                        angle1 = 0.7853981633974483D * (double)i;
                        angle2 = 0.7853981633974483D * (double)((i + 3) % 8);
                        Location start = origin.clone().add(Math.cos(angle1) * radius * 0.8D, 0.1D, Math.sin(angle1) * radius * 0.8D);
                        Location end = origin.clone().add(Math.cos(angle2) * radius * 0.8D, 0.1D, Math.sin(angle2) * radius * 0.8D);
                        ParticleUtils.line(start, end, Particle.ELECTRIC_SPARK, 0.5D);
                     }
                  }

                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 1.0F, 0.5F + (float)this.tick * 0.02F);
                  }

                  this.rotation += 0.05D;
                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void igniteSymbols(final Location origin, final double radius, final Runnable onComplete) {
      ParticleUtils.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 2.0F, 0.5F);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 30) {
               onComplete.run();
               this.cancel();
            } else {
               World world = origin.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  int i;
                  double angle;
                  double r;
                  double x;
                  for(i = 0; i < 8; ++i) {
                     angle = 0.7853981633974483D * (double)i;
                     r = Math.cos(angle) * radius * 0.8D;
                     x = Math.sin(angle) * radius * 0.8D;
                     Location pillarTop = origin.clone().add(r, 3.0D + (double)this.tick * 0.3D, x);
                     ParticleUtils.soulFlame(pillarTop, 8, 0.2D, 0.1D);
                     ParticleUtils.endRod(pillarTop, 4, 0.15D, 0.05D);
                     if (this.tick > 15) {
                        Location centerAbove = origin.clone().add(0.0D, 3.0D + (double)this.tick * 0.3D, 0.0D);
                        ParticleUtils.line(pillarTop, centerAbove, Particle.SOUL_FIRE_FLAME, 0.3D);
                     }
                  }

                  for(i = 0; i < 100; ++i) {
                     angle = 0.06283185307179587D * (double)i;
                     r = radius * (0.3D + RagnarokOfLightAbility.this.random.nextDouble() * 0.7D);
                     x = Math.cos(angle) * r;
                     double z = Math.sin(angle) * r;
                     Location point = origin.clone().add(x, 0.1D, z);
                     if (RagnarokOfLightAbility.this.random.nextDouble() < 0.3D) {
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.0D, 0.3D, 0.0D, 0.02D);
                     }
                  }

                  if (this.tick % 5 == 0) {
                     ParticleUtils.flash(origin.clone().add(0.0D, (double)this.tick * 0.3D, 0.0D));
                  }

                  if (this.tick % 8 == 0) {
                     ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5F, 0.8F + (float)this.tick * 0.03F);
                  }

                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createApocalypsePillar(Player player, final Location origin, final double radius, IvoryConfig.AbilitySettings settings) {
      final World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
         ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_DEATH, 1.5F, 0.8F);

         for(int i = 0; i < 5; ++i) {
            ParticleUtils.flash(origin.clone().add(0.0D, (double)(i * 10), 0.0D));
         }

         DamageZone zone = DamageZone.cylinder(player, origin, radius, 100.0D, settings.damagePerTick * 3.0D, settings.damageTickInterval, 100);
         DamageZoneManager.getInstance(this.plugin).register(zone);
         (new BukkitRunnable() {
            int tick = 0;
            double pillarHeight = 0.0D;
            final double maxHeight = 80.0D;
            List<BlockDisplay> pillarDisplays = new ArrayList();

            public void run() {
               BlockDisplay display;
               if (this.tick >= 100) {
                  Iterator var17 = this.pillarDisplays.iterator();

                  while(var17.hasNext()) {
                     display = (BlockDisplay)var17.next();
                     if (display.isValid()) {
                        display.remove();
                     }
                  }

                  this.cancel();
               } else {
                  if (this.tick < 20) {
                     this.pillarHeight = 80.0D * ((double)this.tick / 20.0D);
                  } else if (this.tick > 80) {
                     this.pillarHeight = 80.0D * (1.0D - (double)(this.tick - 80) / 20.0D);
                  }

                  int currentTick;
                  double y;
                  double yx;
                  double spiralAngle;
                  if (this.tick == 0) {
                     for(currentTick = 0; currentTick < 4; ++currentTick) {
                        y = 1.5707963267948966D * (double)currentTick;
                        yx = Math.cos(y) * radius * 0.3D;
                        spiralAngle = Math.sin(y) * radius * 0.3D;
                        Location displayLoc = origin.clone().add(yx, 0.0D, spiralAngle);
                        BlockDisplay displayx = (BlockDisplay)world.spawn(displayLoc, BlockDisplay.class, (d) -> {
                           d.setBlock(Material.SEA_LANTERN.createBlockData());
                           d.setGlowing(true);
                           d.setBrightness(new Brightness(15, 15));
                        });
                        this.pillarDisplays.add(displayx);
                     }
                  }

                  for(currentTick = 0; currentTick < this.pillarDisplays.size(); ++currentTick) {
                     display = (BlockDisplay)this.pillarDisplays.get(currentTick);
                     if (display.isValid()) {
                        Transformation t = display.getTransformation();
                        display.setTransformation(new Transformation(new Vector3f(-1.0F, 0.0F, -1.0F), t.getLeftRotation(), new Vector3f(2.0F, (float)this.pillarHeight, 2.0F), t.getRightRotation()));
                     }
                  }

                  double spiralRadius;
                  for(currentTick = 0; currentTick < 100; ++currentTick) {
                     y = 0.06283185307179587D * (double)currentTick + (double)this.tick * 0.1D;
                     yx = Math.cos(y) * radius;
                     spiralAngle = Math.sin(y) * radius;
                     spiralRadius = RagnarokOfLightAbility.this.random.nextDouble() * this.pillarHeight;
                     Location point = origin.clone().add(yx, spiralRadius, spiralAngle);
                     world.spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.1D, 0.0D, 0.1D, 0.0D);
                  }

                  for(currentTick = 0; currentTick < 4; ++currentTick) {
                     y = (double)this.tick * 0.2D + (double)currentTick * 3.141592653589793D / 2.0D;

                     for(yx = 0.0D; yx < this.pillarHeight; ++yx) {
                        spiralAngle = y + yx * 0.15D;
                        spiralRadius = radius * 0.5D * (1.0D - yx / 80.0D * 0.3D);
                        double x = Math.cos(spiralAngle) * spiralRadius;
                        double z = Math.sin(spiralAngle) * spiralRadius;
                        Location spiralPoint = origin.clone().add(x, yx, z);
                        world.spawnParticle(Particle.END_ROD, spiralPoint, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                     }
                  }

                  if (this.tick % 3 == 0) {
                     for(currentTick = 0; currentTick < 5; ++currentTick) {
                        y = RagnarokOfLightAbility.this.random.nextDouble() * this.pillarHeight;
                        Location dischargeLoc = origin.clone().add(0.0D, y, 0.0D);
                        ParticleUtils.electricSpark(dischargeLoc, 20, radius * 0.8D, 0.1D);
                     }
                  }

                  if (this.tick % 10 == 0 && this.tick < 80) {
                     currentTick = this.tick;
                     (new BukkitRunnable() {
                        int waveTick = 0;

                        public void run() {
                           if (this.waveTick >= 15) {
                              this.cancel();
                           } else {
                              double waveRadius = radius + (double)this.waveTick * 1.5D;
                              ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, waveRadius, (int)(waveRadius * 6.0D));
                              ++this.waveTick;
                           }
                        }
                     }).runTaskTimer(RagnarokOfLightAbility.this.plugin, 0L, 1L);
                  }

                  Location topLoc = origin.clone().add(0.0D, this.pillarHeight, 0.0D);
                  ParticleUtils.soulFlame(topLoc, 30, radius * 0.5D, 0.2D);
                  ParticleUtils.firework(topLoc, 15, radius * 0.3D, 0.3D);
                  if (this.tick % 15 == 0) {
                     ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.6F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }
}
