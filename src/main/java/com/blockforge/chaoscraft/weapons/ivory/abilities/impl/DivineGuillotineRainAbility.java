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
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DivineGuillotineRainAbility extends Ability {
   private final Random random = new Random();

   public DivineGuillotineRainAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "divine_guillotine_rain";
   }

   public String getDisplayName() {
      return "Divine Guillotine Rain";
   }

   public String getDescription() {
      return "Summon a devastating rain of massive divine guillotine blades that slice through everything below.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      int waveCount = settings.getInt("wave-count", 5);
      int bladesPerWave = settings.getInt("blades-per-wave", 8);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.5F, 1.2F);
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THUNDER, 1.5F, 0.8F);
      this.createSkyWarning(origin, radius);

      for(int wave = 0; wave < waveCount; ++wave) {
         final int fWave = wave;
         int waveDelay = 30 + wave * 25;
         this.runLater(() -> {
            this.spawnBladeWave(player, origin, radius, bladesPerWave, fWave, settings);
         }, (long)waveDelay);
      }

      this.runLater(() -> {
         this.createFinale(player, origin, radius, settings);
      }, (long)(30 + waveCount * 25 + 40));
   }

   private void createSkyWarning(final Location origin, final double radius) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 30) {
               this.cancel();
            } else {
               Location skyLoc = origin.clone().add(0.0D, 35.0D, 0.0D);
               double portalRadius = radius * ((double)this.tick / 30.0D);

               int i;
               double angle;
               double x;
               double z;
               Location point;
               for(i = 0; i < 40; ++i) {
                  angle = 0.15707963267948966D * (double)i + this.rotation;
                  x = Math.cos(angle) * portalRadius;
                  z = Math.sin(angle) * portalRadius;
                  point = skyLoc.clone().add(x, 0.0D, z);
                  ParticleUtils.soulFlame(point, 1, 0.2D, 0.0D);
               }

               for(i = 0; i < 20; ++i) {
                  angle = 0.3141592653589793D * (double)i - this.rotation * 1.5D;
                  x = Math.cos(angle) * portalRadius * 0.5D;
                  z = Math.sin(angle) * portalRadius * 0.5D;
                  point = skyLoc.clone().add(x, 0.0D, z);
                  ParticleUtils.endRod(point, 1, 0.15D, 0.0D);
               }

               this.rotation += 0.1D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spawnBladeWave(Player player, Location origin, double radius, int bladeCount, int waveNum, IvoryConfig.AbilitySettings settings) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THROW, 2.0F, 0.6F);

         for(int i = 0; i < bladeCount; ++i) {
            double angle = 6.283185307179586D / (double)bladeCount * (double)i + (double)waveNum * 0.3D;
            double dist = radius * (0.3D + this.random.nextDouble() * 0.7D);
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            Location targetLoc = origin.clone().add(x, 0.0D, z);
            int bladeDelay = i * 2;
            this.runLater(() -> {
               this.spawnBlade(player, targetLoc, settings);
            }, (long)bladeDelay);
         }

      }
   }

   private void spawnBlade(final Player player, final Location targetLoc, final IvoryConfig.AbilitySettings settings) {
      final World world = targetLoc.getWorld();
      if (world != null) {
         double offsetX = (this.random.nextDouble() - 0.5D) * 5.0D;
         double offsetZ = (this.random.nextDouble() - 0.5D) * 5.0D;
         final Location spawnLoc = targetLoc.clone().add(offsetX, 40.0D, offsetZ);
         this.createBladeWarning(targetLoc);
         final double bladeSize = 2.0D + this.random.nextDouble() * 1.0D;
         final double rotationAngle = this.random.nextDouble() * 3.141592653589793D * 2.0D;
         final BlockDisplay blade = (BlockDisplay)world.spawn(spawnLoc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.DIAMOND_BLOCK.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Quaternionf rotation = new Quaternionf();
            rotation.rotateY((float)rotationAngle);
            rotation.rotateX((float)Math.toRadians(15.0D));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f((float)(-bladeSize * 0.1D), 0.0F, (float)(-bladeSize * 0.5D)), rotation, new Vector3f((float)(bladeSize * 0.2D), (float)bladeSize, (float)(bladeSize * 3.0D)), t.getRightRotation()));
         });
         (new BukkitRunnable() {
            Location currentLoc = spawnLoc.clone();
            int tick = 0;
            final double speed = 4.0D;
            double spinSpeed = 0.15D;
            double currentSpin = rotationAngle;

            public void run() {
               double groundY = (double)(world.getHighestBlockYAt(targetLoc) + 1);
               if (this.currentLoc.getY() <= groundY) {
                  if (blade.isValid()) {
                     blade.remove();
                  }

                  DivineGuillotineRainAbility.this.createBladeImpact(player, this.currentLoc, bladeSize, settings);
                  this.cancel();
               } else {
                  this.currentLoc.add(0.0D, -4.0D, 0.0D);
                  if (blade.isValid()) {
                     blade.teleport(this.currentLoc);
                     this.currentSpin += this.spinSpeed;
                     Quaternionf rotation = new Quaternionf();
                     rotation.rotateY((float)this.currentSpin);
                     rotation.rotateX((float)Math.toRadians(15.0D));
                     Transformation t = blade.getTransformation();
                     blade.setTransformation(new Transformation(t.getTranslation(), rotation, t.getScale(), t.getRightRotation()));
                  }

                  ParticleUtils.soulFlame(this.currentLoc.clone().add(0.0D, bladeSize, 0.0D), 5, 0.3D, 0.05D);
                  ParticleUtils.endRod(this.currentLoc.clone().add(0.0D, bladeSize * 0.5D, 0.0D), 3, 0.2D, 0.02D);
                  if (this.tick % 5 == 0) {
                     ParticleUtils.playSound(this.currentLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5F, 1.5F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 5L, 1L);
      }
   }

   private void createBladeWarning(final Location loc) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 15) {
               this.cancel();
            } else {
               double pulseScale = 1.0D + Math.sin((double)this.tick * 0.5D) * 0.2D;

               for(int arm = 0; arm < 4; ++arm) {
                  double angle = this.rotation + (double)arm * 3.141592653589793D / 2.0D;

                  for(double r = 0.0D; r < 1.5D * pulseScale; r += 0.3D) {
                     double x = Math.cos(angle) * r;
                     double z = Math.sin(angle) * r;
                     ParticleUtils.spawnSingle(loc.clone().add(x, 0.1D, z), Particle.END_ROD);
                  }
               }

               this.rotation += 0.15D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createBladeImpact(Player player, Location loc, double bladeSize, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         final Location groundLoc = loc.clone();
         groundLoc.setY((double)world.getHighestBlockYAt(loc) + 0.5D);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5F, 0.8F);
         ParticleUtils.playSound(groundLoc, Sound.BLOCK_ANVIL_LAND, 1.0F, 1.5F);
         ParticleUtils.flash(groundLoc);
         ParticleUtils.soulFlame(groundLoc, 25, bladeSize, 0.15D);
         ParticleUtils.endRod(groundLoc.clone().add(0.0D, 0.5D, 0.0D), 15, bladeSize * 0.8D, 0.2D);
         ParticleUtils.electricSpark(groundLoc, 12, bladeSize * 1.2D, 0.1D);
         final double slashAngle = this.random.nextDouble() * 3.141592653589793D * 2.0D;
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  double slashLength = (double)this.tick * 0.5D;

                  for(double d = -slashLength; d <= slashLength; d += 0.3D) {
                     double x = Math.cos(slashAngle) * d;
                     double z = Math.sin(slashAngle) * d;
                     Location slashPoint = groundLoc.clone().add(x, 0.2D, z);
                     ParticleUtils.soulFlame(slashPoint, 1, 0.1D, 0.02D);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.cylinder(player, groundLoc, bladeSize, 3.0D, settings.damagePerTick, settings.damageTickInterval, 15);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void createFinale(Player player, final Location origin, double radius, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5F, 0.6F);
      ParticleUtils.flash(origin);
      ParticleUtils.flash(origin.clone().add(0.0D, 5.0D, 0.0D));
      ParticleUtils.soulFlame(origin, 100, radius * 0.6D, 0.3D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 2.0D, 0.0D), 80, radius * 0.5D, 0.35D);
      ParticleUtils.firework(origin.clone().add(0.0D, 3.0D, 0.0D), 50, radius * 0.4D, 0.4D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 20) {
               this.cancel();
            } else {
               double shockRadius = (double)this.tick * 1.2D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, shockRadius, (int)(shockRadius * 8.0D) + 15);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.cylinder(player, origin, radius, 10.0D, settings.damagePerTick * 2.0D, settings.damageTickInterval, 30);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
