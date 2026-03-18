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

public class CelestialArmadaAbility extends Ability {
   private final Random random = new Random();

   public CelestialArmadaAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "celestial_armada";
   }

   public String getDisplayName() {
      return "Celestial Armada";
   }

   public String getDescription() {
      return "Summon a magnificent armada of celestial warships that fly overhead and rain devastating beams of concentrated starlight upon your foes.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      int shipCount = settings.getInt("ship-count", 5);
      double flightHeight = settings.getDouble("flight-height", 30.0D);
      int durationSeconds = settings.getInt("duration-seconds", 12);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 2.0F, 1.2F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.6F);
      ParticleUtils.playSound(origin, Sound.ITEM_TRIDENT_THUNDER, 1.5F, 0.8F);
      this.createArrivalPortal(origin, flightHeight, () -> {
         List<CelestialArmadaAbility.ShipData> ships = this.spawnArmada(origin, shipCount, flightHeight);
         this.runLater(() -> {
            this.beginBombardment(player, origin, ships, durationSeconds, settings);
         }, 30L);
      });
   }

   private void createArrivalPortal(Location origin, double height, final Runnable onComplete) {
      final Location portalLoc = origin.clone().add(0.0D, height, 0.0D);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 40) {
               ParticleUtils.flash(portalLoc);
               onComplete.run();
               this.cancel();
            } else {
               double progress = (double)this.tick / 40.0D;
               double portalSize = 10.0D * progress;

               for(int ring = 0; ring < 3; ++ring) {
                  double ringRadius = portalSize * (0.4D + (double)ring * 0.3D);
                  double ringRotation = this.rotation * (1.0D + (double)ring * 0.2D) * (double)(ring % 2 == 0 ? 1 : -1);
                  int points = 30 + ring * 10;
                  double angleStep = 6.283185307179586D / (double)points;

                  for(int i = 0; i < points; ++i) {
                     double angle = (double)i * angleStep + ringRotation;
                     double x = Math.cos(angle) * ringRadius;
                     double z = Math.sin(angle) * ringRadius;
                     Location point = portalLoc.clone().add(x, 0.0D, z);
                     if (ring == 0) {
                        ParticleUtils.soulFlame(point, 1, 0.1D, 0.01D);
                     } else {
                        ParticleUtils.endRod(point, 1, 0.05D, 0.0D);
                     }
                  }
               }

               ParticleUtils.glow(portalLoc, 10, portalSize * 0.2D, 0.0D);
               if (this.tick % 3 == 0) {
                  ParticleUtils.electricSpark(portalLoc, 15, portalSize * 0.3D, 0.1D);
               }

               if (this.tick % 10 == 0) {
                  ParticleUtils.playSound(portalLoc, Sound.BLOCK_PORTAL_AMBIENT, 1.0F, 0.5F);
               }

               this.rotation += 0.15D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private List<CelestialArmadaAbility.ShipData> spawnArmada(Location origin, int count, double height) {
      List<CelestialArmadaAbility.ShipData> ships = new ArrayList();
      World world = origin.getWorld();
      if (world == null) {
         return ships;
      } else {
         for(int i = 0; i < count; ++i) {
            int row = i / 2;
            int side = i % 2 == 0 ? -1 : 1;
            if (i == 0) {
               side = 0;
            }

            double xOffset = (double)(side * (row + 1) * 6);
            double zOffset = (double)(row * 5);
            Location shipLoc = origin.clone().add(xOffset, height, zOffset);
            CelestialArmadaAbility.ShipData ship = this.createShip(world, shipLoc, i);
            ships.add(ship);
            ParticleUtils.playSound(shipLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
            ParticleUtils.flash(shipLoc);
            ParticleUtils.endRod(shipLoc, 40, 3.0D, 0.2D);
         }

         return ships;
      }
   }

   private CelestialArmadaAbility.ShipData createShip(World world, Location loc, int index) {
      CelestialArmadaAbility.ShipData ship = new CelestialArmadaAbility.ShipData(loc.clone(), index);
      ship.hull = (BlockDisplay)world.spawn(loc, BlockDisplay.class, (d) -> {
         d.setBlock(Material.SEA_LANTERN.createBlockData());
         d.setGlowing(true);
         d.setBrightness(new Brightness(15, 15));
         Transformation t = d.getTransformation();
         d.setTransformation(new Transformation(new Vector3f(-1.0F, -0.3F, -2.0F), t.getLeftRotation(), new Vector3f(2.0F, 0.6F, 4.0F), t.getRightRotation()));
      });
      ship.bridge = (BlockDisplay)world.spawn(loc.clone().add(0.0D, 0.5D, 0.5D), BlockDisplay.class, (d) -> {
         d.setBlock(Material.GLOWSTONE.createBlockData());
         d.setGlowing(true);
         d.setBrightness(new Brightness(15, 15));
         Transformation t = d.getTransformation();
         d.setTransformation(new Transformation(new Vector3f(-0.5F, 0.0F, -0.5F), t.getLeftRotation(), new Vector3f(1.0F, 0.4F, 1.0F), t.getRightRotation()));
      });
      ship.engines = (BlockDisplay)world.spawn(loc.clone().add(0.0D, 0.0D, 2.0D), BlockDisplay.class, (d) -> {
         d.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
         d.setGlowing(true);
         d.setBrightness(new Brightness(15, 15));
         Transformation t = d.getTransformation();
         d.setTransformation(new Transformation(new Vector3f(-0.6F, -0.2F, 0.0F), t.getLeftRotation(), new Vector3f(1.2F, 0.4F, 0.3F), t.getRightRotation()));
      });
      return ship;
   }

   private void beginBombardment(final Player player, final Location origin, final List<CelestialArmadaAbility.ShipData> ships, int durationSeconds, final IvoryConfig.AbilitySettings settings) {
      final int durationTicks = durationSeconds * 20;
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 1.5F, 1.2F);
      (new BukkitRunnable() {
         int tick = 0;
         double formationZ = 0.0D;
         int nextFireShip = 0;
         int fireCooldown = 0;

         public void run() {
            CelestialArmadaAbility.ShipData ship;
            if (this.tick < durationTicks) {
               this.formationZ -= 0.15D;

               for(int i = 0; i < ships.size(); ++i) {
                  ship = (CelestialArmadaAbility.ShipData)ships.get(i);
                  int row = i / 2;
                  int side = i % 2 == 0 ? -1 : 1;
                  if (i == 0) {
                     side = 0;
                  }

                  double bob = Math.sin((double)this.tick * 0.1D + (double)i) * 0.5D;
                  Location newLoc = origin.clone().add((double)(side * (row + 1) * 6), ship.location.getY() - origin.getY() + bob, (double)(row * 5) + this.formationZ);
                  ship.location = newLoc;
                  if (ship.hull != null && ship.hull.isValid()) {
                     ship.hull.teleport(newLoc);
                  }

                  if (ship.bridge != null && ship.bridge.isValid()) {
                     ship.bridge.teleport(newLoc.clone().add(0.0D, 0.5D, 0.5D));
                  }

                  if (ship.engines != null && ship.engines.isValid()) {
                     ship.engines.teleport(newLoc.clone().add(0.0D, 0.0D, 2.0D));
                  }

                  Location engineLoc = newLoc.clone().add(0.0D, 0.0D, 2.5D);
                  ParticleUtils.soulFlame(engineLoc, 5, 0.3D, 0.05D);
                  ParticleUtils.endRod(engineLoc, 3, 0.2D, 0.03D);
                  ParticleUtils.endRod(newLoc, 2, 1.0D, 0.01D);
               }

               --this.fireCooldown;
               if (this.fireCooldown <= 0 && !ships.isEmpty()) {
                  CelestialArmadaAbility.ShipData firingShip = (CelestialArmadaAbility.ShipData)ships.get(this.nextFireShip);
                  double targetAngle = CelestialArmadaAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double targetDist = CelestialArmadaAbility.this.random.nextDouble() * settings.radius;
                  Location targetLoc = origin.clone().add(Math.cos(targetAngle) * targetDist, 0.0D, Math.sin(targetAngle) * targetDist + this.formationZ);
                  CelestialArmadaAbility.this.fireBeam(player, firingShip.location.clone().add(0.0D, -0.5D, -1.0D), targetLoc, settings);
                  this.nextFireShip = (this.nextFireShip + 1) % ships.size();
                  this.fireCooldown = 12;
               }

               if (this.tick % 40 == 0) {
                  ParticleUtils.playSound(origin, Sound.BLOCK_CONDUIT_AMBIENT, 1.0F, 0.8F);
               }

               ++this.tick;
            } else {
               Iterator var1 = ships.iterator();

               while(var1.hasNext()) {
                  ship = (CelestialArmadaAbility.ShipData)var1.next();
                  CelestialArmadaAbility.this.destroyShip(ship);
               }

               CelestialArmadaAbility.this.createFinalStrike(player, origin, settings);
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void fireBeam(Player player, Location from, Location to, IvoryConfig.AbilitySettings settings) {
      World world = from.getWorld();
      if (world != null) {
         final Location groundTarget = to.clone();
         groundTarget.setY((double)world.getHighestBlockYAt(to) + 0.5D);
         ParticleUtils.playSound(from, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, 1.5F);
         ParticleUtils.soulFlame(from, 10, 0.5D, 0.05D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  ParticleUtils.ring(groundTarget, Particle.END_ROD, 1.0D + (double)this.tick * 0.1D, 12);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         this.runLater(() -> {
            ParticleUtils.playSound(groundTarget, Sound.ENTITY_GUARDIAN_ATTACK, 1.5F, 0.6F);
            ParticleUtils.playSound(groundTarget, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick >= 12) {
                     this.cancel();
                  } else {
                     double intensity = 1.0D - (double)this.tick / 12.0D;
                     Location current = from.clone();
                     Vector dir = groundTarget.toVector().subtract(from.toVector()).normalize().multiply(2);
                     double dist = from.distance(groundTarget);

                     for(double d = 0.0D; d < dist; d += 2.0D) {
                        ParticleUtils.soulFlame(current, (int)(2.0D * intensity), 0.2D, 0.01D);
                        ParticleUtils.endRod(current, (int)(1.0D * intensity), 0.1D, 0.0D);
                        current.add(dir);
                     }

                     ++this.tick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
            ParticleUtils.flash(groundTarget);
            ParticleUtils.soulFlame(groundTarget, 30, 1.5D, 0.15D);
            ParticleUtils.endRod(groundTarget, 20, 1.2D, 0.2D);
            ParticleUtils.electricSpark(groundTarget, 15, 2.0D, 0.1D);
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick >= 8) {
                     this.cancel();
                  } else {
                     double radius = (double)this.tick * 0.5D;
                     ParticleUtils.ring(groundTarget, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 6);
                     ++this.tick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
            DamageZone zone = DamageZone.cylinder(player, groundTarget, 3.0D, 4.0D, settings.damagePerTick, settings.damageTickInterval, 15);
            DamageZoneManager.getInstance(this.plugin).register(zone);
         }, 8L);
      }
   }

   private void destroyShip(CelestialArmadaAbility.ShipData ship) {
      Location loc = ship.location;
      ParticleUtils.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.2F);
      ParticleUtils.flash(loc);
      ParticleUtils.endRod(loc, 30, 2.0D, 0.3D);
      ParticleUtils.electricSpark(loc, 20, 2.5D, 0.15D);
      if (ship.hull != null && ship.hull.isValid()) {
         ship.hull.remove();
      }

      if (ship.bridge != null && ship.bridge.isValid()) {
         ship.bridge.remove();
      }

      if (ship.engines != null && ship.engines.isValid()) {
         ship.engines.remove();
      }

   }

   private void createFinalStrike(final Player player, final Location origin, final IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.0F, 1.5F);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 20) {
               CelestialArmadaAbility.this.createFinalImpact(player, origin, settings);
               this.cancel();
            } else {
               for(int i = 0; i < 5; ++i) {
                  double angle = 1.2566370614359172D * (double)i + (double)this.tick * 0.1D;
                  double x = Math.cos(angle) * 15.0D;
                  double z = Math.sin(angle) * 15.0D;
                  Location beamStart = origin.clone().add(x, 40.0D, z);
                  ParticleUtils.beam(beamStart, origin, 1.0D);
               }

               ParticleUtils.glow(origin.clone().add(0.0D, 1.0D, 0.0D), 15, (double)this.tick * 0.2D, 0.0D);
               ParticleUtils.electricSpark(origin.clone().add(0.0D, 1.0D, 0.0D), 10, (double)this.tick * 0.15D, 0.05D);
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createFinalImpact(Player player, final Location origin, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.5F);
      ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.5F, 0.7F);
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);

      for(int i = 0; i < 5; ++i) {
         final int fi = i;
         this.runLater(() -> {
            ParticleUtils.flash(origin.clone().add(0.0D, (double)(fi * 3), 0.0D));
         }, (long)(fi * 2));
      }

      ParticleUtils.soulFlame(origin.clone().add(0.0D, 2.0D, 0.0D), 250, 10.0D, 0.4D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 5.0D, 0.0D), 180, 8.0D, 0.5D);
      ParticleUtils.firework(origin.clone().add(0.0D, 8.0D, 0.0D), 120, 6.0D, 0.6D);
      ParticleUtils.electricSpark(origin, 100, 12.0D, 0.3D);
      (new BukkitRunnable() {
         int tick = 0;

         public void run() {
            if (this.tick >= 30) {
               this.cancel();
            } else {
               double radius = (double)this.tick * 1.5D;
               ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 10.0D) + 20);
               if (this.tick % 2 == 0) {
                  ParticleUtils.ring(origin.clone().add(0.0D, (double)this.tick * 0.3D, 0.0D), Particle.END_ROD, radius * 0.8D, (int)(radius * 6.0D) + 10);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
      DamageZone zone = DamageZone.sphere(player, origin, settings.radius * 2.0D, settings.damagePerTick * 5.0D, settings.damageTickInterval, 40);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }

   private static class ShipData {
      Location location;
      int index;
      BlockDisplay hull;
      BlockDisplay bridge;
      BlockDisplay engines;

      ShipData(Location location, int index) {
         this.location = location;
         this.index = index;
      }
   }
}
