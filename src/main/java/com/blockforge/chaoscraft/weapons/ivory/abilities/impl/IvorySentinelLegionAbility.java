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

public class IvorySentinelLegionAbility extends Ability {
   private final Random random = new Random();

   public IvorySentinelLegionAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "ivory_sentinel_legion";
   }

   public String getDisplayName() {
      return "Ivory Sentinel Legion";
   }

   public String getDescription() {
      return "Summon a legion of sentinel warriors made of crystallized starlight that march forward and destroy all enemies in their path.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      int sentinelCount = settings.getInt("sentinel-count", 8);
      double marchDistance = settings.getDouble("march-distance", 40.0D);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.0F, 1.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.7F);
      ParticleUtils.playSound(origin, Sound.ITEM_TOTEM_USE, 1.0F, 0.8F);
      this.createSummoningCircle(origin, () -> {
         List<IvorySentinelLegionAbility.SentinelData> sentinels = this.summonSentinels(origin, sentinelCount);
         this.runLater(() -> {
            this.beginMarch(player, origin, sentinels, marchDistance, settings);
         }, 20L);
      });
   }

   private void createSummoningCircle(final Location origin, final Runnable onComplete) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 40) {
               ParticleUtils.flash(origin.clone().add(0.0D, 1.0D, 0.0D));
               onComplete.run();
               this.cancel();
            } else {
               World world = origin.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 40.0D;
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.1D, 0.0D), Particle.SOUL_FIRE_FLAME, 6.0D, 40);
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.1D, 0.0D), Particle.END_ROD, 4.0D, 30);
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.1D, 0.0D), Particle.ELECTRIC_SPARK, 2.0D, 20);

                  double anglex;
                  double x;
                  for(int i = 0; i < 8; ++i) {
                     double angle = this.rotation + (double)i * 3.141592653589793D / 4.0D;
                     anglex = Math.cos(angle) * 5.0D;
                     x = Math.sin(angle) * 5.0D;
                     Location runeLoc = origin.clone().add(anglex, 0.2D, x);
                     ParticleUtils.soulFlame(runeLoc, 3, 0.2D, 0.01D);
                     ParticleUtils.glow(runeLoc, 2, 0.15D, 0.0D);
                  }

                  if (progress > 0.5D) {
                     double pillarProgress = (progress - 0.5D) * 2.0D;

                     for(int ix = 0; ix < 8; ++ix) {
                        anglex = 0.7853981633974483D * (double)ix;
                        x = Math.cos(anglex) * 4.0D;
                        double z = Math.sin(anglex) * 4.0D;
                        Location pillarLoc = origin.clone().add(x, 0.0D, z);
                        double pillarHeight = pillarProgress * 3.0D;

                        for(double y = 0.0D; y < pillarHeight; y += 0.3D) {
                           Location particleLoc = pillarLoc.clone().add(0.0D, y, 0.0D);
                           world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.1D, 0.0D, 0.1D, 0.0D);
                        }
                     }
                  }

                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, (float)(0.699999988079071D + progress * 0.5D));
                  }

                  this.rotation += 0.1D;
                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private List<IvorySentinelLegionAbility.SentinelData> summonSentinels(Location origin, int count) {
      List<IvorySentinelLegionAbility.SentinelData> sentinels = new ArrayList();
      World world = origin.getWorld();
      if (world == null) {
         return sentinels;
      } else {
         for(int i = 0; i < count; ++i) {
            double angle = 6.283185307179586D / (double)count * (double)i;
            double x = Math.cos(angle) * 4.0D;
            double z = Math.sin(angle) * 4.0D;
            Location spawnLoc = origin.clone().add(x, 0.0D, z);
            IvorySentinelLegionAbility.SentinelData sentinel = this.createSentinel(world, spawnLoc, i);
            sentinels.add(sentinel);
            ParticleUtils.playSound(spawnLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0F, 1.5F);
            ParticleUtils.flash(spawnLoc.clone().add(0.0D, 1.0D, 0.0D));
            ParticleUtils.endRod(spawnLoc.clone().add(0.0D, 1.0D, 0.0D), 30, 1.0D, 0.15D);
            ParticleUtils.soulFlame(spawnLoc.clone().add(0.0D, 0.5D, 0.0D), 20, 0.5D, 0.1D);
         }

         return sentinels;
      }
   }

   private IvorySentinelLegionAbility.SentinelData createSentinel(World world, Location loc, int index) {
      IvorySentinelLegionAbility.SentinelData sentinel = new IvorySentinelLegionAbility.SentinelData(loc.clone(), index);
      sentinel.body = (BlockDisplay)world.spawn(loc.clone().add(0.0D, 1.0D, 0.0D), BlockDisplay.class, (d) -> {
         d.setBlock(Material.SEA_LANTERN.createBlockData());
         d.setGlowing(true);
         d.setBrightness(new Brightness(15, 15));
         Transformation t = d.getTransformation();
         d.setTransformation(new Transformation(new Vector3f(-0.4F, -0.5F, -0.2F), t.getLeftRotation(), new Vector3f(0.8F, 1.0F, 0.4F), t.getRightRotation()));
      });
      sentinel.head = (BlockDisplay)world.spawn(loc.clone().add(0.0D, 2.2D, 0.0D), BlockDisplay.class, (d) -> {
         d.setBlock(Material.GLOWSTONE.createBlockData());
         d.setGlowing(true);
         d.setBrightness(new Brightness(15, 15));
         Transformation t = d.getTransformation();
         d.setTransformation(new Transformation(new Vector3f(-0.25F, -0.25F, -0.25F), t.getLeftRotation(), new Vector3f(0.5F, 0.5F, 0.5F), t.getRightRotation()));
      });
      sentinel.sword = (BlockDisplay)world.spawn(loc.clone().add(0.5D, 1.5D, 0.0D), BlockDisplay.class, (d) -> {
         d.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
         d.setGlowing(true);
         d.setBrightness(new Brightness(15, 15));
         Transformation t = d.getTransformation();
         Quaternionf rotation = (new Quaternionf()).rotateZ((float)Math.toRadians(-30.0D));
         d.setTransformation(new Transformation(new Vector3f(-0.1F, -0.1F, -0.5F), rotation, new Vector3f(0.15F, 0.15F, 1.2F), t.getRightRotation()));
      });
      return sentinel;
   }

   private void beginMarch(final Player player, final Location origin, final List<IvorySentinelLegionAbility.SentinelData> sentinels, final double marchDistance, final IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_RAVAGER_STEP, 1.5F, 0.8F);
      (new BukkitRunnable() {
         int tick = 0;
         final double speed = 0.3D;
         final int maxTicks = (int)(marchDistance / 0.3D);
         double animationPhase = 0.0D;

         public void run() {
            Iterator var1;
            IvorySentinelLegionAbility.SentinelData sentinel;
            if (this.tick < this.maxTicks && !sentinels.isEmpty()) {
               this.animationPhase += 0.3D;
               var1 = sentinels.iterator();

               while(var1.hasNext()) {
                  sentinel = (IvorySentinelLegionAbility.SentinelData)var1.next();
                  sentinel.location.add(0.0D, 0.0D, -0.3D);
                  double bob = Math.sin(this.animationPhase + (double)sentinel.index) * 0.1D;
                  if (sentinel.body != null && sentinel.body.isValid()) {
                     sentinel.body.teleport(sentinel.location.clone().add(0.0D, 1.0D + bob, 0.0D));
                  }

                  if (sentinel.head != null && sentinel.head.isValid()) {
                     sentinel.head.teleport(sentinel.location.clone().add(0.0D, 2.2D + bob, 0.0D));
                  }

                  if (sentinel.sword != null && sentinel.sword.isValid()) {
                     double swingOffset = Math.sin(this.animationPhase * 2.0D + (double)sentinel.index) * 0.3D;
                     sentinel.sword.teleport(sentinel.location.clone().add(0.5D, 1.5D + bob, swingOffset));
                  }

                  ParticleUtils.soulFlame(sentinel.location.clone().add(0.0D, 0.5D, 0.0D), 2, 0.3D, 0.02D);
                  ParticleUtils.endRod(sentinel.location.clone().add(0.0D, 1.5D, 0.0D), 1, 0.2D, 0.0D);
                  if (this.tick % 10 == 0) {
                     ParticleUtils.whiteAsh(sentinel.location, 5, 0.3D, 0.02D);
                  }
               }

               if (this.tick % 5 == 0) {
                  var1 = sentinels.iterator();

                  while(var1.hasNext()) {
                     sentinel = (IvorySentinelLegionAbility.SentinelData)var1.next();
                     DamageZone zone = DamageZone.sphere(player, sentinel.location.clone().add(0.0D, 1.0D, 0.0D), 2.0D, settings.damagePerTick, settings.damageTickInterval, 8);
                     DamageZoneManager.getInstance(IvorySentinelLegionAbility.this.plugin).register(zone);
                  }
               }

               if (this.tick % 20 == 0) {
                  var1 = sentinels.iterator();

                  while(var1.hasNext()) {
                     sentinel = (IvorySentinelLegionAbility.SentinelData)var1.next();
                     IvorySentinelLegionAbility.this.createAttackSlash(sentinel);
                  }

                  ParticleUtils.playSound(origin.clone().add(0.0D, 0.0D, -((double)this.tick * 0.3D)), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.8F);
               }

               if (this.tick % 15 == 0) {
                  ParticleUtils.playSound(origin.clone().add(0.0D, 0.0D, -((double)this.tick * 0.3D)), Sound.ENTITY_RAVAGER_STEP, 1.0F, 1.0F);
               }

               ++this.tick;
            } else {
               var1 = sentinels.iterator();

               while(var1.hasNext()) {
                  sentinel = (IvorySentinelLegionAbility.SentinelData)var1.next();
                  IvorySentinelLegionAbility.this.destroySentinel(sentinel);
               }

               Location finalLoc = origin.clone().add(0.0D, 0.0D, -marchDistance);
               IvorySentinelLegionAbility.this.createFinalExplosion(player, finalLoc, sentinels.size(), settings);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createAttackSlash(IvorySentinelLegionAbility.SentinelData sentinel) {
      Location slashLoc = sentinel.location.clone().add(0.0D, 1.5D, -1.0D);

      for(int i = 0; i < 15; ++i) {
         double angle = -1.0471975511965976D + 2.0943951023931953D * ((double)i / 15.0D);
         double x = Math.cos(angle) * 2.0D;
         double y = Math.sin(angle) * 1.5D;
         Location particleLoc = slashLoc.clone().add(x, y, 0.0D);
         ParticleUtils.soulFlame(particleLoc, 2, 0.1D, 0.01D);
         ParticleUtils.endRod(particleLoc, 1, 0.05D, 0.0D);
      }

      ParticleUtils.spawn(slashLoc, Particle.SWEEP_ATTACK, 1, 0.0D, 0.0D, 0.0D, 0.0D);
   }

   private void destroySentinel(IvorySentinelLegionAbility.SentinelData sentinel) {
      Location loc = sentinel.location.clone().add(0.0D, 1.0D, 0.0D);
      ParticleUtils.flash(loc);
      ParticleUtils.endRod(loc, 25, 1.0D, 0.2D);
      ParticleUtils.soulFlame(loc, 20, 0.8D, 0.15D);
      ParticleUtils.electricSpark(loc, 15, 1.2D, 0.1D);
      ParticleUtils.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0F, 0.5F);
      if (sentinel.body != null && sentinel.body.isValid()) {
         sentinel.body.remove();
      }

      if (sentinel.head != null && sentinel.head.isValid()) {
         sentinel.head.remove();
      }

      if (sentinel.sword != null && sentinel.sword.isValid()) {
         sentinel.sword.remove();
      }

   }

   private void createFinalExplosion(Player player, Location loc, int sentinelCount, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.6F);
      ParticleUtils.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, 1.5F, 1.2F);
      ParticleUtils.flash(loc.clone().add(0.0D, 2.0D, 0.0D));
      ParticleUtils.soulFlame(loc.clone().add(0.0D, 1.0D, 0.0D), 100, 4.0D, 0.25D);
      ParticleUtils.endRod(loc.clone().add(0.0D, 2.0D, 0.0D), 80, 3.0D, 0.3D);
      ParticleUtils.firework(loc.clone().add(0.0D, 3.0D, 0.0D), 50, 2.5D, 0.4D);
      DamageZone zone = DamageZone.sphere(player, loc, settings.radius, settings.damagePerTick * (double)sentinelCount * 0.5D, settings.damageTickInterval, 30);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }

   private static class SentinelData {
      Location location;
      int index;
      BlockDisplay body;
      BlockDisplay head;
      BlockDisplay sword;

      SentinelData(Location location, int index) {
         this.location = location;
         this.index = index;
      }
   }
}
