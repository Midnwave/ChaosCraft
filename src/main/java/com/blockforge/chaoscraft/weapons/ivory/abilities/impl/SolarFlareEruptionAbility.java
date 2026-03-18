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
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class SolarFlareEruptionAbility extends Ability {
   private final Random random = new Random();

   public SolarFlareEruptionAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "solar_flare_eruption";
   }

   public String getDisplayName() {
      return "Solar Flare Eruption";
   }

   public String getDescription() {
      return "Manifest a miniature sun that erupts with devastating solar flares, scorching all enemies with stellar fire.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      int flareCount = settings.getInt("flare-count", 6);
      Location sunLoc = origin.clone().add(0.0D, 8.0D, 0.0D);
      ParticleUtils.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.8F);
      List<BlockDisplay> sunDisplays = this.createSun(sunLoc);
      this.runLater(() -> {
         this.buildSolarPressure(sunLoc, sunDisplays);
      }, 30L);
      this.runLater(() -> {
         this.eruptFlares(player, sunLoc, radius, flareCount, settings);
      }, 70L);
      this.runLater(() -> {
         Iterator var1 = sunDisplays.iterator();

         while(var1.hasNext()) {
            BlockDisplay d = (BlockDisplay)var1.next();
            if (d.isValid()) {
               d.remove();
            }
         }

      }, 180L);
   }

   private List<BlockDisplay> createSun(final Location center) {
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
            d.setTransformation(new Transformation(new Vector3f(-1.0F, -1.0F, -1.0F), t.getLeftRotation(), new Vector3f(2.0F, 2.0F, 2.0F), t.getRightRotation()));
         });
         displays.add(core);

         for(int i = 0; i < 8; ++i) {
            double phi = Math.acos(1.0D - 2.0D * ((double)i + 0.5D) / 8.0D);
            double theta = 3.141592653589793D * (1.0D + Math.sqrt(5.0D)) * (double)i;
            double x = 1.5D * Math.sin(phi) * Math.cos(theta);
            double y = 1.5D * Math.cos(phi);
            double z = 1.5D * Math.sin(phi) * Math.sin(theta);
            Location coronaLoc = center.clone().add(x, y, z);
            BlockDisplay corona = (BlockDisplay)world.spawn(coronaLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.ORANGE_STAINED_GLASS.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.3F, -0.3F, -0.3F), t.getLeftRotation(), new Vector3f(0.6F, 0.6F, 0.6F), t.getRightRotation()));
            });
            displays.add(corona);
         }

         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  int i;
                  double angle;
                  double x;
                  double z;
                  for(i = 0; i < 25; ++i) {
                     angle = SolarFlareEruptionAbility.this.random.nextDouble() * 3.141592653589793D;
                     x = SolarFlareEruptionAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     z = 2.0D * Math.sin(angle) * Math.cos(x);
                     double y = 2.0D * Math.cos(angle);
                     double zx = 2.0D * Math.sin(angle) * Math.sin(x);
                     Location surfacePoint = center.clone().add(z, y, zx);
                     ParticleUtils.soulFlame(surfacePoint, 1, 0.2D, 0.02D);
                  }

                  for(i = 0; i < 20; ++i) {
                     angle = this.rotation + (double)i * 3.141592653589793D * 2.0D / 20.0D;
                     x = Math.cos(angle) * 2.5D;
                     z = Math.sin(angle) * 2.5D;
                     Location coronaPoint = center.clone().add(x, 0.0D, z);
                     ParticleUtils.endRod(coronaPoint, 1, 0.1D, 0.01D);
                  }

                  ParticleUtils.glow(center, 10, 0.8D, 0.02D);
                  this.rotation += 0.1D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         return displays;
      }
   }

   private void buildSolarPressure(final Location center, final List<BlockDisplay> displays) {
      ParticleUtils.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0F, 0.5F);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               double pressure = (double)this.tick / 40.0D;

               Location fieldPoint;
               int i;
               double angle;
               double t;
               double fieldDist;
               double x;
               double y;
               double z;
               for(i = 0; i < (int)(20.0D + pressure * 40.0D); ++i) {
                  angle = SolarFlareEruptionAbility.this.random.nextDouble() * 3.141592653589793D;
                  t = SolarFlareEruptionAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  fieldDist = 2.0D + SolarFlareEruptionAbility.this.random.nextDouble() * pressure * 2.0D;
                  x = fieldDist * Math.sin(angle) * Math.cos(t);
                  y = fieldDist * Math.cos(angle);
                  z = fieldDist * Math.sin(angle) * Math.sin(t);
                  fieldPoint = center.clone().add(x, y, z);
                  ParticleUtils.soulFlame(fieldPoint, 1, 0.3D, 0.04D);
               }

               if (!displays.isEmpty() && ((BlockDisplay)displays.get(0)).isValid()) {
                  float pulseScale = (float)(2.0D + Math.sin((double)this.tick * 0.3D) * 0.3D + pressure * 0.5D);
                  Transformation tx = ((BlockDisplay)displays.get(0)).getTransformation();
                  ((BlockDisplay)displays.get(0)).setTransformation(new Transformation(new Vector3f(-pulseScale / 2.0F, -pulseScale / 2.0F, -pulseScale / 2.0F), tx.getLeftRotation(), new Vector3f(pulseScale, pulseScale, pulseScale), tx.getRightRotation()));
               }

               for(i = 0; i < 4; ++i) {
                  angle = this.rotation + (double)i * 3.141592653589793D / 2.0D;

                  for(t = 0.0D; t < 3.141592653589793D; t += 0.2D) {
                     fieldDist = 3.0D + pressure * 2.0D;
                     x = Math.cos(angle) * Math.sin(t) * fieldDist;
                     y = Math.cos(t) * fieldDist - fieldDist * 0.5D;
                     z = Math.sin(angle) * Math.sin(t) * fieldDist;
                     fieldPoint = center.clone().add(x, y, z);
                     ParticleUtils.electricSpark(fieldPoint, 1, 0.1D, 0.01D);
                  }
               }

               if (this.tick > 20 && this.tick % (8 - (int)(pressure * 5.0D)) == 0) {
                  ParticleUtils.flash(center);
                  ParticleUtils.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0F, 0.6F + (float)pressure * 0.5F);
               }

               this.rotation += 0.08D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void eruptFlares(Player player, final Location sunLoc, double radius, int flareCount, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(sunLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.4F);
      ParticleUtils.playSound(sunLoc, Sound.ENTITY_BLAZE_SHOOT, 2.0F, 0.3F);
      ParticleUtils.flash(sunLoc);

      for(int i = 0; i < flareCount; ++i) {
         double angle = 6.283185307179586D / (double)flareCount * (double)i;
         double elevation = (this.random.nextDouble() - 0.5D) * 3.141592653589793D * 0.5D;
         double dirX = Math.cos(angle) * Math.cos(elevation);
         double dirY = Math.sin(elevation);
         double dirZ = Math.sin(angle) * Math.cos(elevation);
         Vector flareDir = new Vector(dirX, dirY, dirZ);
         int delay = i * 5;
         this.runLater(() -> {
            this.launchFlare(player, sunLoc, flareDir, radius, settings);
         }, (long)delay);
      }

      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 30) {
               this.cancel();
            } else {
               ParticleUtils.soulFlame(sunLoc, 40, 3.0D, 0.25D);
               ParticleUtils.endRod(sunLoc, 25, 2.5D, 0.3D);
               ParticleUtils.firework(sunLoc, 15, 2.0D, 0.35D);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 2L);
   }

   private void launchFlare(final Player player, final Location start, final Vector direction, final double maxDist, final IvoryConfig.AbilitySettings settings) {
      World world = start.getWorld();
      if (world != null) {
         ParticleUtils.playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5F, 0.6F);
         final BlockDisplay flareCore = (BlockDisplay)world.spawn(start, BlockDisplay.class, (d) -> {
            d.setBlock(Material.ORANGE_STAINED_GLASS.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.5F, -0.5F, -0.5F), t.getLeftRotation(), new Vector3f(1.0F, 1.0F, 1.0F), t.getRightRotation()));
         });
         (new BukkitRunnable() {
            Location currentLoc = start.clone();
            int tick = 0;
            double speed = 2.0D;
            double traveled = 0.0D;

            public void run() {
               if (!(this.traveled >= maxDist) && this.tick < 60) {
                  this.currentLoc.add(direction.clone().multiply(this.speed));
                  this.traveled += this.speed;
                  if (flareCore.isValid()) {
                     flareCore.teleport(this.currentLoc);
                  }

                  ParticleUtils.soulFlame(this.currentLoc, 15, 0.8D, 0.05D);
                  ParticleUtils.endRod(this.currentLoc, 8, 0.6D, 0.03D);
                  ParticleUtils.firework(this.currentLoc, 5, 0.4D, 0.02D);
                  Location tailLoc = this.currentLoc.clone();

                  int i;
                  double angle;
                  for(i = 0; i < 8; ++i) {
                     tailLoc.subtract(direction.clone().multiply(0.8D));
                     angle = 1.0D - (double)i / 8.0D;
                     ParticleUtils.soulFlame(tailLoc, (int)(5.0D * angle), 0.4D, 0.02D);
                  }

                  for(i = 0; i < 4; ++i) {
                     angle = (double)this.tick * 0.3D + (double)i * 3.141592653589793D / 2.0D;
                     double x = Math.cos(angle) * 1.5D;
                     double y = Math.sin(angle) * 1.5D;
                     Location distortLoc = this.currentLoc.clone().add(x, y, 0.0D);
                     ParticleUtils.electricSpark(distortLoc, 1, 0.2D, 0.02D);
                  }

                  if (this.tick % 5 == 0) {
                     DamageZone zone = DamageZone.sphere(player, this.currentLoc, 2.0D, settings.damagePerTick, settings.damageTickInterval, 8);
                     DamageZoneManager.getInstance(SolarFlareEruptionAbility.this.plugin).register(zone);
                  }

                  ++this.tick;
               } else {
                  if (flareCore.isValid()) {
                     flareCore.remove();
                  }

                  SolarFlareEruptionAbility.this.createFlareImpact(player, this.currentLoc, settings);
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createFlareImpact(Player player, final Location loc, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0.8F);
      ParticleUtils.flash(loc);
      ParticleUtils.soulFlame(loc, 60, 3.0D, 0.25D);
      ParticleUtils.endRod(loc, 40, 2.5D, 0.3D);
      ParticleUtils.firework(loc, 30, 2.0D, 0.35D);
      ParticleUtils.electricSpark(loc, 20, 4.0D, 0.2D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 12) {
               this.cancel();
            } else {
               double ringRadius = (double)this.tick * 0.8D;
               ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, ringRadius, (int)(ringRadius * 8.0D) + 10);
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
               double intensity = 1.0D - (double)this.tick / 30.0D;
               ParticleUtils.soulFlame(loc, (int)(10.0D * intensity), 1.5D, 0.03D);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 5L, 2L);
      DamageZone zone = DamageZone.sphere(player, loc, settings.radius / 3.0D, settings.damagePerTick * 1.5D, settings.damageTickInterval, 25);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
