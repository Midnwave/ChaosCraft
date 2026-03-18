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

public class CometChargeAbility extends Ability {
   private final Random random = new Random();

   public CometChargeAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "comet_charge";
   }

   public String getDisplayName() {
      return "Comet Charge";
   }

   public String getDescription() {
      return "Transform into a blazing celestial comet and charge forward, leaving a devastating trail of starfire in your wake.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double chargeDistance = settings.getDouble("charge-distance", 30.0D);
      double chargeWidth = settings.getDouble("charge-width", 3.0D);
      ParticleUtils.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5F, 0.8F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 1.5F, 1.0F);
      this.createIgnitionEffect(origin);
      Location endLocation = origin.clone().add(0.0D, 0.0D, -chargeDistance);
      List<BlockDisplay> cometDisplays = this.createCometDisplays(origin);
      this.executeCharge(player, origin, endLocation, chargeWidth, settings, cometDisplays);
   }

   private void createIgnitionEffect(final Location loc) {
      ParticleUtils.flash(loc.clone().add(0.0D, 1.0D, 0.0D));
      ParticleUtils.soulFlame(loc.clone().add(0.0D, 1.0D, 0.0D), 50, 1.5D, 0.15D);
      ParticleUtils.endRod(loc.clone().add(0.0D, 1.5D, 0.0D), 30, 1.0D, 0.2D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 10) {
               this.cancel();
            } else {
               double radius = (double)this.tick * 0.6D;
               ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 10);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private List<BlockDisplay> createCometDisplays(Location origin) {
      List<BlockDisplay> displays = new ArrayList();
      World world = origin.getWorld();
      if (world == null) {
         return displays;
      } else {
         Location cometCenter = origin.clone().add(0.0D, 1.0D, 0.0D);
         BlockDisplay core = (BlockDisplay)world.spawn(cometCenter, BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.5F, -0.5F, -0.5F), t.getLeftRotation(), new Vector3f(1.0F, 1.0F, 1.0F), t.getRightRotation()));
         });
         displays.add(core);

         for(int i = 0; i < 6; ++i) {
            double angle = 1.0471975511965976D * (double)i;
            double x = Math.cos(angle) * 0.7D;
            double y = Math.sin(angle) * 0.7D;
            Location flameLoc = cometCenter.clone().add(x, y, 0.0D);
            BlockDisplay flame = (BlockDisplay)world.spawn(flameLoc, BlockDisplay.class, (d) -> {
               d.setBlock(Material.SEA_LANTERN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.2F, -0.2F, -0.2F), t.getLeftRotation(), new Vector3f(0.4F, 0.4F, 0.4F), t.getRightRotation()));
            });
            displays.add(flame);
         }

         return displays;
      }
   }

   private void executeCharge(final Player player, final Location start, Location end, final double chargeWidth, final IvoryConfig.AbilitySettings settings, final List<BlockDisplay> displays) {
      World world = start.getWorld();
      if (world != null) {
         double totalDistance = start.distance(end);
         final double speed = 2.5D;
         final int totalTicks = (int)(totalDistance / speed);
         (new BukkitRunnable() {
            int tick = 0;
            Location currentLoc = start.clone().add(0.0D, 1.0D, 0.0D);
            double rotation = 0.0D;

            public void run() {
               if (this.tick >= totalTicks) {
                  Iterator var10 = displays.iterator();

                  while(var10.hasNext()) {
                     BlockDisplay d = (BlockDisplay)var10.next();
                     if (d.isValid()) {
                        d.remove();
                     }
                  }

                  CometChargeAbility.this.createImpactEffect(player, this.currentLoc.clone().add(0.0D, -1.0D, 0.0D), chargeWidth, settings);
                  this.cancel();
               } else {
                  this.currentLoc.add(0.0D, 0.0D, -speed);
                  CometChargeAbility.this.updateCometDisplays(displays, this.currentLoc, this.rotation);
                  ParticleUtils.soulFlame(this.currentLoc, 15, 0.6D, 0.05D);
                  ParticleUtils.endRod(this.currentLoc, 10, 0.4D, 0.03D);
                  ParticleUtils.glow(this.currentLoc, 5, 0.3D, 0.0D);

                  for(int i = 0; i < 8; ++i) {
                     double angle = this.rotation + (double)i * 3.141592653589793D / 4.0D;
                     double x = Math.cos(angle) * 1.2D;
                     double y = Math.sin(angle) * 1.2D;
                     Location flameLoc = this.currentLoc.clone().add(x, y, 0.0D);
                     ParticleUtils.soulFlame(flameLoc, 2, 0.1D, 0.02D);
                  }

                  CometChargeAbility.this.createCometTail(this.currentLoc.clone());
                  DamageZone zone = DamageZone.cylinder(player, this.currentLoc.clone().add(0.0D, -1.0D, 0.0D), chargeWidth, 2.0D, settings.damagePerTick, 1, 5);
                  DamageZoneManager.getInstance(CometChargeAbility.this.plugin).register(zone);
                  if (this.tick % 5 == 0) {
                     ParticleUtils.playSound(this.currentLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8F, 1.2F);
                  }

                  this.rotation += 0.4D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 10L, 1L);
      }
   }

   private void updateCometDisplays(List<BlockDisplay> displays, Location center, double rotation) {
      if (!displays.isEmpty()) {
         if (((BlockDisplay)displays.get(0)).isValid()) {
            ((BlockDisplay)displays.get(0)).teleport(center);
         }

         for(int i = 1; i < displays.size(); ++i) {
            BlockDisplay display = (BlockDisplay)displays.get(i);
            if (display.isValid()) {
               double angle = rotation + (double)(i - 1) * 3.141592653589793D * 2.0D / 6.0D;
               double x = Math.cos(angle) * 0.7D;
               double y = Math.sin(angle) * 0.7D;
               display.teleport(center.clone().add(x, y, 0.0D));
            }
         }

      }
   }

   private void createCometTail(final Location headLoc) {
      (new BukkitRunnable() {
         int tick = 0;
         Location tailLoc = headLoc.clone();

         public void run() {
            if (this.tick >= 15) {
               this.cancel();
            } else {
               double fade = 1.0D - (double)this.tick / 15.0D;
               this.tailLoc.add(0.0D, 0.0D, 0.3D);
               int particleCount = (int)(10.0D * fade);
               double spread = 0.2D + (double)this.tick * 0.1D;
               ParticleUtils.soulFlame(this.tailLoc, particleCount, spread, 0.02D);
               if (this.tick % 2 == 0) {
                  ParticleUtils.endRod(this.tailLoc, particleCount / 2, spread * 0.8D, 0.01D);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createImpactEffect(Player player, final Location loc, final double chargeWidth, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.5F, 0.6F);
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.8F);
         ParticleUtils.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5F, 0.5F);
         ParticleUtils.flash(loc.clone().add(0.0D, 1.0D, 0.0D));
         ParticleUtils.soulFlame(loc.clone().add(0.0D, 1.0D, 0.0D), 120, 4.0D, 0.35D);
         ParticleUtils.endRod(loc.clone().add(0.0D, 2.0D, 0.0D), 80, 3.0D, 0.4D);
         ParticleUtils.firework(loc.clone().add(0.0D, 3.0D, 0.0D), 60, 2.5D, 0.5D);
         ParticleUtils.electricSpark(loc.clone().add(0.0D, 1.0D, 0.0D), 50, 5.0D, 0.25D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 15) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 1.0D;
                  ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 10.0D) + 15);
                  double coneLength = (double)this.tick * 1.5D;

                  for(double z = 0.0D; z < coneLength; z += 0.5D) {
                     double coneRadius = (coneLength - z) * 0.3D;
                     Location coneLoc = loc.clone().add(0.0D, 0.5D, -z);

                     for(int i = 0; i < 8; ++i) {
                        double angle = 0.7853981633974483D * (double)i;
                        double x = Math.cos(angle) * coneRadius;
                        double y = Math.sin(angle) * coneRadius;
                        ParticleUtils.soulFlame(coneLoc.clone().add(x, y, 0.0D), 1, 0.1D, 0.02D);
                     }
                  }

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
                  ParticleUtils.soulFlame(loc, (int)(15.0D * intensity), chargeWidth, 0.03D);

                  for(int i = 0; i < (int)(5.0D * intensity); ++i) {
                     double x = (CometChargeAbility.this.random.nextDouble() - 0.5D) * chargeWidth * 2.0D;
                     double z = (CometChargeAbility.this.random.nextDouble() - 0.5D) * chargeWidth * 2.0D;
                     Location emberLoc = loc.clone().add(x, CometChargeAbility.this.random.nextDouble() * 3.0D, z);
                     ParticleUtils.endRod(emberLoc, 1, 0.05D, 0.05D);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 2L);
         DamageZone zone = DamageZone.sphere(player, loc, settings.radius, settings.damagePerTick * 2.5D, settings.damageTickInterval, 40);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }
}
