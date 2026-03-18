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

public class SanctumOfFallenStarsAbility extends Ability {
   private final Random random = new Random();

   public SanctumOfFallenStarsAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "sanctum_of_fallen_stars";
   }

   public String getDisplayName() {
      return "Sanctum of Fallen Stars";
   }

   public String getDescription() {
      return "Erect a magnificent dome of crystallized starlight that shields your position while continuously damaging enemies within.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      int duration = settings.getInt("duration-ticks", 200);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 1.2F);
      ParticleUtils.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5F, 0.8F);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 1.0F, 1.8F);
      List<BlockDisplay> domeDisplays = new ArrayList();
      this.createDomeFormation(origin, radius, domeDisplays, () -> {
         this.activateDome(player, origin, radius, duration, settings, domeDisplays);
      });
   }

   private void createDomeFormation(final Location origin, final double radius, final List<BlockDisplay> displays, final Runnable onComplete) {
      final World world = origin.getWorld();
      if (world != null) {
         (new BukkitRunnable() {
            int tick = 0;
            double currentHeight = 0.0D;
            final double maxHeight = radius;

            public void run() {
               if (this.tick >= 30) {
                  onComplete.run();
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 30.0D;
                  this.currentHeight = this.maxHeight * progress;
                  double groundRadius = radius * progress;
                  ParticleUtils.ring(origin, Particle.END_ROD, groundRadius, (int)(groundRadius * 8.0D) + 20);
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.1D, 0.0D), Particle.SOUL_FIRE_FLAME, groundRadius * 0.9D, (int)(groundRadius * 6.0D) + 15);
                  int domeProgress;
                  double domeAngle;
                  if (this.tick == 10) {
                     for(domeProgress = 0; domeProgress < 8; ++domeProgress) {
                        domeAngle = 0.7853981633974483D * (double)domeProgress;
                        double xx = Math.cos(domeAngle) * radius;
                        double zx = Math.sin(domeAngle) * radius;
                        Location pillarLoc = origin.clone().add(xx, 0.0D, zx);
                        BlockDisplay pillar = (BlockDisplay)world.spawn(pillarLoc, BlockDisplay.class, (d) -> {
                           d.setBlock(Material.AMETHYST_BLOCK.createBlockData());
                           d.setGlowing(true);
                           d.setBrightness(new Brightness(15, 15));
                           Transformation t = d.getTransformation();
                           d.setTransformation(new Transformation(new Vector3f(-0.3F, 0.0F, -0.3F), t.getLeftRotation(), new Vector3f(0.6F, 0.1F, 0.6F), t.getRightRotation()));
                        });
                        displays.add(pillar);
                     }
                  }

                  if (this.tick > 10) {
                     float pillarHeight = (float)((double)(this.tick - 10) * 0.3D);
                     Iterator var21 = displays.iterator();

                     while(var21.hasNext()) {
                        BlockDisplay display = (BlockDisplay)var21.next();
                        if (display.isValid()) {
                           Transformation t = display.getTransformation();
                           display.setTransformation(new Transformation(new Vector3f(-0.3F, 0.0F, -0.3F), t.getLeftRotation(), new Vector3f(0.6F, pillarHeight, 0.6F), t.getRightRotation()));
                        }
                     }
                  }

                  if (this.tick > 15) {
                     domeProgress = this.tick - 15;
                     domeAngle = 1.5707963267948966D * ((double)domeProgress / 15.0D);

                     for(int i = 0; i < 30; ++i) {
                        double theta = 0.20943951023931953D * (double)i;
                        double x = radius * Math.sin(domeAngle) * Math.cos(theta);
                        double y = radius * Math.cos(domeAngle);
                        double z = radius * Math.sin(domeAngle) * Math.sin(theta);
                        if (y > 0.0D) {
                           Location domePoint = origin.clone().add(x, y, z);
                           ParticleUtils.endRod(domePoint, 1, 0.1D, 0.0D);
                        }
                     }
                  }

                  if (this.tick % 5 == 0) {
                     ParticleUtils.playSound(origin, Sound.BLOCK_AMETHYST_CLUSTER_STEP, 1.0F, (float)(0.800000011920929D + progress * 0.4000000059604645D));
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void activateDome(Player player, final Location origin, final double radius, final int duration, IvoryConfig.AbilitySettings settings, final List<BlockDisplay> displays) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, 2.0F, 1.0F);
         ParticleUtils.flash(origin.clone().add(0.0D, radius / 2.0D, 0.0D));
         final List<BlockDisplay> domeStructure = new ArrayList();

         for(int ring = 1; ring <= 3; ++ring) {
            double ringAngle = 1.5707963267948966D * ((double)ring / 4.0D);
            double ringRadius = radius * Math.sin(ringAngle);
            double ringHeight = radius * Math.cos(ringAngle);

            for(int i = 0; i < 8; ++i) {
               double angle = 0.7853981633974483D * (double)i;
               double x = Math.cos(angle) * ringRadius;
               double z = Math.sin(angle) * ringRadius;
               Location nodeLoc = origin.clone().add(x, ringHeight, z);
               BlockDisplay node = (BlockDisplay)world.spawn(nodeLoc, BlockDisplay.class, (d) -> {
                  d.setBlock(Material.SEA_LANTERN.createBlockData());
                  d.setGlowing(true);
                  d.setBrightness(new Brightness(15, 15));
                  Transformation t = d.getTransformation();
                  d.setTransformation(new Transformation(new Vector3f(-0.25F, -0.25F, -0.25F), t.getLeftRotation(), new Vector3f(0.5F, 0.5F, 0.5F), t.getRightRotation()));
               });
               domeStructure.add(node);
            }
         }

         DamageZone zone = DamageZone.sphere(player, origin, radius, settings.damagePerTick, settings.damageTickInterval, duration);
         DamageZoneManager.getInstance(this.plugin).register(zone);
         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;

            public void run() {
               if (this.tick >= duration) {
                  SanctumOfFallenStarsAbility.this.collapseDome(origin, radius, displays, domeStructure);
                  this.cancel();
               } else {
                  int i;
                  double starAngle;
                  double starDist;
                  double startHeight;
                  double ringRotation;
                  for(i = 0; i < 40; ++i) {
                     starAngle = SanctumOfFallenStarsAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     starDist = SanctumOfFallenStarsAbility.this.random.nextDouble() * 3.141592653589793D / 2.0D;
                     startHeight = radius * Math.sin(starDist) * Math.cos(starAngle);
                     ringRotation = radius * Math.cos(starDist);
                     double zx = radius * Math.sin(starDist) * Math.sin(starAngle);
                     if (ringRotation > 0.0D) {
                        Location surfacePoint = origin.clone().add(startHeight, ringRotation, zx);
                        if (SanctumOfFallenStarsAbility.this.random.nextDouble() < 0.5D) {
                           ParticleUtils.endRod(surfacePoint, 1, 0.05D, 0.0D);
                        } else {
                           ParticleUtils.soulFlame(surfacePoint, 1, 0.05D, 0.0D);
                        }
                     }
                  }

                  for(i = 0; i < 3; ++i) {
                     starAngle = 1.5707963267948966D * ((double)(i + 1) / 4.0D);
                     starDist = radius * Math.sin(starAngle);
                     startHeight = radius * Math.cos(starAngle);
                     ringRotation = this.rotation * (1.0D + (double)i * 0.3D) * (double)(i % 2 == 0 ? 1 : -1);

                     for(int ix = 0; ix < 20; ++ix) {
                        double angle = 0.3141592653589793D * (double)ix + ringRotation;
                        double x = Math.cos(angle) * starDist;
                        double z = Math.sin(angle) * starDist;
                        Location ringPoint = origin.clone().add(x, startHeight, z);
                        ParticleUtils.electricSpark(ringPoint, 1, 0.05D, 0.0D);
                     }
                  }

                  ParticleUtils.ring(origin.clone().add(0.0D, 0.1D, 0.0D), Particle.END_ROD, radius, 40);
                  if (this.tick % 10 == 0) {
                     for(i = 0; i < 3; ++i) {
                        starAngle = SanctumOfFallenStarsAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                        starDist = SanctumOfFallenStarsAbility.this.random.nextDouble() * radius * 0.8D;
                        startHeight = radius * 0.8D;
                        Location starStart = origin.clone().add(Math.cos(starAngle) * starDist, startHeight, Math.sin(starAngle) * starDist);
                        SanctumOfFallenStarsAbility.this.createFallingStar(starStart);
                     }
                  }

                  if (this.tick % 40 == 0) {
                     ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 1.0F, 1.2F);
                     ParticleUtils.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.0F);
                  }

                  if (this.tick % 30 == 0) {
                     ParticleUtils.flash(origin.clone().add(0.0D, radius * 0.5D, 0.0D));
                  }

                  this.rotation += 0.05D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createFallingStar(final Location start) {
      (new BukkitRunnable() {
         Location current = start.clone();
         int tick = 0;

         public void run() {
            if (this.tick < 20 && !(this.current.getY() <= start.getY() - 10.0D)) {
               this.current.add(0.0D, -0.5D, 0.0D);
               ParticleUtils.endRod(this.current, 2, 0.1D, 0.01D);
               ParticleUtils.soulFlame(this.current, 1, 0.05D, 0.02D);
               ++this.tick;
            } else {
               ParticleUtils.soulFlame(this.current, 10, 0.3D, 0.05D);
               ParticleUtils.playSound(this.current, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5F, 1.5F);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void collapseDome(final Location origin, final double radius, final List<BlockDisplay> pillars, final List<BlockDisplay> structure) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.BLOCK_GLASS_BREAK, 2.0F, 0.8F);
         ParticleUtils.playSound(origin, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5F, 0.6F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 1.2F);
         ParticleUtils.flash(origin.clone().add(0.0D, radius / 2.0D, 0.0D));
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 30) {
                  Iterator var14 = pillars.iterator();

                  BlockDisplay dx;
                  while(var14.hasNext()) {
                     dx = (BlockDisplay)var14.next();
                     if (dx.isValid()) {
                        dx.remove();
                     }
                  }

                  var14 = structure.iterator();

                  while(var14.hasNext()) {
                     dx = (BlockDisplay)var14.next();
                     if (dx.isValid()) {
                        dx.remove();
                     }
                  }

                  this.cancel();
               } else {
                  int i;
                  double x;
                  double z;
                  double y;
                  for(i = 0; i < 20; ++i) {
                     x = SanctumOfFallenStarsAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     z = SanctumOfFallenStarsAbility.this.random.nextDouble() * 3.141592653589793D / 2.0D;
                     y = radius * Math.sin(z) * Math.cos(x);
                     double yx = radius * Math.cos(z) - (double)this.tick * 0.5D;
                     double zx = radius * Math.sin(z) * Math.sin(x);
                     if (yx > 0.0D) {
                        Location shardLoc = origin.clone().add(y, yx, zx);
                        ParticleUtils.endRod(shardLoc, 1, 0.1D, 0.1D);
                     }
                  }

                  for(i = 0; i < 10; ++i) {
                     x = (SanctumOfFallenStarsAbility.this.random.nextDouble() - 0.5D) * radius * 2.0D;
                     z = (SanctumOfFallenStarsAbility.this.random.nextDouble() - 0.5D) * radius * 2.0D;
                     y = radius - (double)this.tick * 0.3D + SanctumOfFallenStarsAbility.this.random.nextDouble() * 2.0D;
                     if (y > 0.0D) {
                        Location sparkleLoc = origin.clone().add(x, y, z);
                        ParticleUtils.electricSpark(sparkleLoc, 1, 0.1D, 0.15D);
                     }
                  }

                  float scale = Math.max(0.1F, 1.0F - (float)this.tick * 0.03F);
                  Iterator var15 = structure.iterator();

                  while(var15.hasNext()) {
                     BlockDisplay d = (BlockDisplay)var15.next();
                     if (d.isValid()) {
                        Transformation t = d.getTransformation();
                        d.setTransformation(new Transformation(new Vector3f(-scale * 0.25F, -scale * 0.25F, -scale * 0.25F), t.getLeftRotation(), new Vector3f(scale * 0.5F, scale * 0.5F, scale * 0.5F), t.getRightRotation()));
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }
}
