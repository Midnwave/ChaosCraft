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

public class AstralPhoenixRebirthAbility extends Ability {
   private final Random random = new Random();

   public AstralPhoenixRebirthAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "astral_phoenix_rebirth";
   }

   public String getDisplayName() {
      return "Astral Phoenix Rebirth";
   }

   public String getDescription() {
      return "Summon a magnificent phoenix of celestial fire that rises from the ground and dives forward, leaving trails of purifying flames.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double flightDistance = settings.getDouble("flight-distance", 50.0D);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 2.0F, 1.5F);
      ParticleUtils.playSound(origin, Sound.ITEM_TOTEM_USE, 2.0F, 1.0F);
      ParticleUtils.playSound(origin, Sound.ENTITY_BLAZE_AMBIENT, 2.0F, 0.5F);
      this.createPhoenixBirth(origin, () -> {
         this.createPhoenixRise(player, origin, settings, () -> {
            this.createPhoenixDive(player, origin, flightDistance, settings);
         });
      });
   }

   private void createPhoenixBirth(final Location origin, final Runnable onComplete) {
      (new BukkitRunnable() {
         int tick = 0;
         final int birthTime = 30;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 30) {
               ParticleUtils.flash(origin.clone().add(0.0D, 3.0D, 0.0D));
               onComplete.run();
               this.cancel();
            } else {
               World world = origin.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 30.0D;
                  double height = progress * 4.0D;

                  for(int i = 0; i < 20; ++i) {
                     double angle = this.rotation + AstralPhoenixRebirthAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double radius = 2.0D * (1.0D - progress * 0.5D);
                     double y = AstralPhoenixRebirthAbility.this.random.nextDouble() * height;
                     double x = Math.cos(angle) * radius * (1.0D - y / height * 0.5D);
                     double z = Math.sin(angle) * radius * (1.0D - y / height * 0.5D);
                     Location particleLoc = origin.clone().add(x, y, z);
                     world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.1D, 0.2D, 0.1D, 0.02D);
                     if (AstralPhoenixRebirthAbility.this.random.nextDouble() < 0.3D) {
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05D, 0.1D, 0.05D, 0.01D);
                     }
                  }

                  Location coreLoc = origin.clone().add(0.0D, height * 0.7D, 0.0D);
                  ParticleUtils.soulFlame(coreLoc, 10, 0.5D * progress, 0.02D);
                  ParticleUtils.glow(coreLoc, 5, 0.3D, 0.0D);
                  ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, 3.0D * (1.0D - progress * 0.3D), 30);
                  if (this.tick % 5 == 0) {
                     ParticleUtils.flash(coreLoc);
                  }

                  if (this.tick % 8 == 0) {
                     ParticleUtils.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 0.8F, (float)(0.5D + progress * 0.5D));
                  }

                  this.rotation += 0.2D;
                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createPhoenixRise(Player player, final Location origin, IvoryConfig.AbilitySettings settings, final Runnable onComplete) {
      World world = origin.getWorld();
      if (world != null) {
         Location phoenixLoc = origin.clone().add(0.0D, 3.0D, 0.0D);
         final List<BlockDisplay> phoenixParts = new ArrayList();
         final BlockDisplay body = (BlockDisplay)world.spawn(phoenixLoc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.4F, -0.3F, -0.6F), t.getLeftRotation(), new Vector3f(0.8F, 0.6F, 1.2F), t.getRightRotation()));
         });
         phoenixParts.add(body);
         final BlockDisplay head = (BlockDisplay)world.spawn(phoenixLoc.clone().add(0.0D, 0.5D, -0.8D), BlockDisplay.class, (d) -> {
            d.setBlock(Material.SEA_LANTERN.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.2F, -0.2F, -0.3F), t.getLeftRotation(), new Vector3f(0.4F, 0.4F, 0.6F), t.getRightRotation()));
         });
         phoenixParts.add(head);
         (new BukkitRunnable() {
            int tick = 0;
            final int riseTime = 40;
            double wingSpread = 0.0D;

            public void run() {
               if (this.tick >= 40) {
                  onComplete.run();
                  Iterator var12 = phoenixParts.iterator();

                  while(var12.hasNext()) {
                     BlockDisplay d = (BlockDisplay)var12.next();
                     if (d.isValid()) {
                        d.remove();
                     }
                  }

                  this.cancel();
               } else {
                  double progress = (double)this.tick / 40.0D;
                  double riseHeight = 3.0D + progress * 8.0D;
                  this.wingSpread = Math.sin(progress * 3.141592653589793D) * 8.0D;
                  Location currentLoc = origin.clone().add(0.0D, riseHeight, 0.0D);
                  if (body.isValid()) {
                     body.teleport(currentLoc);
                  }

                  if (head.isValid()) {
                     head.teleport(currentLoc.clone().add(0.0D, 0.5D, -0.8D));
                  }

                  AstralPhoenixRebirthAbility.this.drawWings(currentLoc, this.wingSpread);
                  AstralPhoenixRebirthAbility.this.drawTail(currentLoc, 5.0D);
                  ParticleUtils.soulFlame(currentLoc, 15, 0.8D, 0.03D);
                  ParticleUtils.endRod(currentLoc, 8, 0.5D, 0.02D);
                  ParticleUtils.glow(currentLoc, 5, 0.4D, 0.0D);

                  for(int i = 0; i < 10; ++i) {
                     double x = (AstralPhoenixRebirthAbility.this.random.nextDouble() - 0.5D) * this.wingSpread * 0.5D;
                     double z = (AstralPhoenixRebirthAbility.this.random.nextDouble() - 0.5D) * 2.0D;
                     Location sparkLoc = currentLoc.clone().add(x, -AstralPhoenixRebirthAbility.this.random.nextDouble() * 3.0D, z);
                     ParticleUtils.soulFlame(sparkLoc, 1, 0.1D, 0.05D);
                  }

                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(currentLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.2F);
                  }

                  if (this.tick == 20) {
                     ParticleUtils.flash(currentLoc);
                     ParticleUtils.playSound(currentLoc, Sound.ITEM_ELYTRA_FLYING, 2.0F, 0.8F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void drawWings(Location center, double wingSpan) {
      World world = center.getWorld();
      if (world != null) {
         for(int side = -1; side <= 1; side += 2) {
            for(double x = 0.0D; x < wingSpan / 2.0D; x += 0.3D) {
               double wingX = x * (double)side;
               double wingY = Math.sin(x / (wingSpan / 2.0D) * 3.141592653589793D) * 2.0D;
               Location wingLoc = center.clone().add(wingX, wingY, 0.0D);
               world.spawnParticle(Particle.SOUL_FIRE_FLAME, wingLoc, 2, 0.1D, 0.1D, 0.1D, 0.01D);
               world.spawnParticle(Particle.END_ROD, wingLoc, 1, 0.05D, 0.05D, 0.05D, 0.0D);
               if ((int)(x * 3.0D) % 2 == 0) {
                  for(double f = 0.0D; f < 1.5D; f += 0.2D) {
                     Location featherLoc = wingLoc.clone().add(0.0D, -f, f * 0.3D);
                     world.spawnParticle(Particle.END_ROD, featherLoc, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                  }
               }
            }
         }

      }
   }

   private void drawTail(Location center, double length) {
      World world = center.getWorld();
      if (world != null) {
         for(double z = 0.0D; z < length; z += 0.3D) {
            double spread = z * 0.3D;
            Location tailLoc = center.clone().add(0.0D, -z * 0.2D, z);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, tailLoc, 2, spread * 0.3D, 0.1D, 0.1D, 0.02D);
            if (z > 1.0D) {
               for(int side = -1; side <= 1; side += 2) {
                  Location plumeLoc = tailLoc.clone().add((double)side * spread * 0.5D, 0.0D, 0.0D);
                  world.spawnParticle(Particle.END_ROD, plumeLoc, 1, 0.05D, 0.05D, 0.05D, 0.01D);
               }
            }
         }

      }
   }

   private void createPhoenixDive(final Player player, final Location origin, final double distance, final IvoryConfig.AbilitySettings settings) {
      final World world = origin.getWorld();
      if (world != null) {
         final Location startLoc = origin.clone().add(0.0D, 11.0D, 0.0D);
         ParticleUtils.playSound(startLoc, Sound.ITEM_ELYTRA_FLYING, 2.0F, 0.5F);
         ParticleUtils.playSound(startLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 2.0F, 0.8F);
         (new BukkitRunnable() {
            int tick = 0;
            final double speed = 2.5D;
            final int maxTicks = (int)(distance / 2.5D);
            Location currentLoc = startLoc.clone();
            double wingFlap = 0.0D;

            public void run() {
               if (this.tick >= this.maxTicks) {
                  AstralPhoenixRebirthAbility.this.createFinalExplosion(player, this.currentLoc, settings);
                  this.cancel();
               } else {
                  this.currentLoc.add(0.0D, -0.1D, -2.5D);
                  if (this.currentLoc.getY() < origin.getY() + 3.0D) {
                     this.currentLoc.setY(origin.getY() + 3.0D);
                  }

                  this.wingFlap += 0.5D;
                  double wingSpread = 6.0D + Math.sin(this.wingFlap) * 2.0D;
                  AstralPhoenixRebirthAbility.this.drawWings(this.currentLoc, wingSpread);
                  AstralPhoenixRebirthAbility.this.drawTail(this.currentLoc, 8.0D);
                  ParticleUtils.soulFlame(this.currentLoc, 20, 1.0D, 0.05D);
                  ParticleUtils.endRod(this.currentLoc, 12, 0.6D, 0.03D);
                  ParticleUtils.glow(this.currentLoc, 8, 0.5D, 0.0D);
                  Location groundLoc = this.currentLoc.clone();
                  groundLoc.setY((double)world.getHighestBlockYAt(this.currentLoc) + 0.1D);
                  ParticleUtils.soulFlame(groundLoc, 10, 1.5D, 0.02D);
                  if (this.tick % 8 == 0) {
                     ParticleUtils.flash(this.currentLoc);
                  }

                  if (this.tick % 12 == 0) {
                     ParticleUtils.playSound(this.currentLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);
                  }

                  if (this.tick % 4 == 0) {
                     DamageZone zone = DamageZone.cylinder(player, this.currentLoc.clone(), 4.0D, 3.0D, settings.damagePerTick, settings.damageTickInterval, 10);
                     DamageZoneManager.getInstance(AstralPhoenixRebirthAbility.this.plugin).register(zone);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createFinalExplosion(Player player, final Location loc, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.5F);
      ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.5F);
      ParticleUtils.playSound(loc, Sound.ENTITY_BLAZE_DEATH, 2.0F, 0.8F);

      for(int i = 0; i < 3; ++i) {
         ParticleUtils.flash(loc.clone().add((this.random.nextDouble() - 0.5D) * 5.0D, (double)(i * 2), (this.random.nextDouble() - 0.5D) * 5.0D));
      }

      ParticleUtils.soulFlame(loc, 250, 8.0D, 0.35D);
      ParticleUtils.endRod(loc.clone().add(0.0D, 2.0D, 0.0D), 150, 6.0D, 0.4D);
      ParticleUtils.firework(loc.clone().add(0.0D, 4.0D, 0.0D), 100, 5.0D, 0.5D);
      ParticleUtils.electricSpark(loc, 80, 7.0D, 0.25D);
      ParticleUtils.glow(loc.clone().add(0.0D, 3.0D, 0.0D), 60, 5.0D, 0.2D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 40) {
               this.cancel();
            } else {
               double intensity = 1.0D - (double)this.tick / 40.0D;

               for(int i = 0; i < (int)(30.0D * intensity); ++i) {
                  double angle = AstralPhoenixRebirthAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double dist = AstralPhoenixRebirthAbility.this.random.nextDouble() * 5.0D;
                  double x = Math.cos(angle) * dist;
                  double z = Math.sin(angle) * dist;
                  double y = (double)this.tick * 0.5D + AstralPhoenixRebirthAbility.this.random.nextDouble() * 3.0D;
                  Location particleLoc = loc.clone().add(x, y, z);
                  ParticleUtils.soulFlame(particleLoc, 1, 0.1D, 0.05D);
                  if (AstralPhoenixRebirthAbility.this.random.nextDouble() < 0.3D) {
                     ParticleUtils.endRod(particleLoc, 1, 0.05D, 0.03D);
                  }
               }

               if (this.tick < 20) {
                  double radius = (double)this.tick * 0.8D;
                  ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 10);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.sphere(player, loc, settings.radius * 1.5D, settings.damagePerTick * 3.0D, settings.damageTickInterval, 40);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }
}
