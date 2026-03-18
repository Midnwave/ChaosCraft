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

public class CelestialSatelliteNetworkAbility extends Ability {
   private final Random random = new Random();

   public CelestialSatelliteNetworkAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "celestial_satellite_network";
   }

   public String getDisplayName() {
      return "Celestial Satellite Network";
   }

   public String getDescription() {
      return "Deploy a network of celestial satellites that orbit above and rain down devastating beams of concentrated starlight.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      int satelliteCount = settings.getInt("satellite-count", 6);
      double orbitRadius = settings.getDouble("orbit-radius", 12.0D);
      double orbitHeight = settings.getDouble("orbit-height", 25.0D);
      int durationSeconds = settings.getInt("duration-seconds", 15);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.8F);
      ParticleUtils.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5F, 0.5F);
      List<CelestialSatelliteNetworkAbility.SatelliteData> satellites = new ArrayList();

      for(int i = 0; i < satelliteCount; ++i) {
         double angle = 6.283185307179586D / (double)satelliteCount * (double)i;
         CelestialSatelliteNetworkAbility.SatelliteData satellite = new CelestialSatelliteNetworkAbility.SatelliteData(angle, orbitRadius, orbitHeight);
         satellites.add(satellite);
         int delay = i * 10;
         this.runLater(() -> {
            this.deploySatellite(origin, satellite);
         }, (long)delay);
      }

      this.runLater(() -> {
         this.startOrbitalOperation(player, origin, satellites, durationSeconds, settings);
      }, (long)(satelliteCount * 10 + 20));
   }

   private void deploySatellite(final Location origin, final CelestialSatelliteNetworkAbility.SatelliteData satellite) {
      World world = origin.getWorld();
      if (world != null) {
         Location targetLoc = this.calculateSatellitePosition(origin, satellite);
         ParticleUtils.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.5F);
         ParticleUtils.flash(targetLoc);
         ParticleUtils.endRod(targetLoc, 30, 1.5D, 0.2D);
         ParticleUtils.electricSpark(targetLoc, 20, 2.0D, 0.1D);
         satellite.display = (BlockDisplay)world.spawn(targetLoc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.SEA_LANTERN.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.5F, -0.25F, -0.5F), t.getLeftRotation(), new Vector3f(1.0F, 0.5F, 1.0F), t.getRightRotation()));
         });
         satellite.antenna = (BlockDisplay)world.spawn(targetLoc.clone().add(0.0D, 0.5D, 0.0D), BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.15F, 0.0F, -0.15F), t.getLeftRotation(), new Vector3f(0.3F, 0.8F, 0.3F), t.getRightRotation()));
         });
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 20) {
                  this.cancel();
               } else {
                  Location loc = CelestialSatelliteNetworkAbility.this.calculateSatellitePosition(origin, satellite);
                  double particleAngle = (double)this.tick * 0.5D;

                  for(int i = 0; i < 4; ++i) {
                     double angle = particleAngle + (double)i * 3.141592653589793D / 2.0D;
                     double x = Math.cos(angle) * 1.2D;
                     double z = Math.sin(angle) * 1.2D;
                     Location particleLoc = loc.clone().add(x, 0.0D, z);
                     ParticleUtils.spawnSingle(particleLoc, Particle.END_ROD);
                  }

                  ParticleUtils.soulFlame(loc, 2, 0.3D, 0.01D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private Location calculateSatellitePosition(Location origin, CelestialSatelliteNetworkAbility.SatelliteData satellite) {
      double x = Math.cos(satellite.angle) * satellite.orbitRadius;
      double z = Math.sin(satellite.angle) * satellite.orbitRadius;
      return origin.clone().add(x, satellite.orbitHeight, z);
   }

   private void startOrbitalOperation(final Player player, final Location origin, final List<CelestialSatelliteNetworkAbility.SatelliteData> satellites, int durationSeconds, final IvoryConfig.AbilitySettings settings) {
      final int durationTicks = durationSeconds * 20;
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, 2.0F, 1.0F);
      (new BukkitRunnable() {
         int tick = 0;
         double orbitRotation = 0.0D;
         int beamCooldown = 0;
         int currentSatelliteIndex = 0;

         public void run() {
            CelestialSatelliteNetworkAbility.SatelliteData satellite;
            if (this.tick < durationTicks) {
               this.orbitRotation += 0.02D;

               double targetAngle;
               for(int i = 0; i < satellites.size(); ++i) {
                  satellite = (CelestialSatelliteNetworkAbility.SatelliteData)satellites.get(i);
                  targetAngle = 6.283185307179586D / (double)satellites.size() * (double)i;
                  satellite.angle = targetAngle + this.orbitRotation;
                  Location newPos = CelestialSatelliteNetworkAbility.this.calculateSatellitePosition(origin, satellite);
                  if (satellite.display != null && satellite.display.isValid()) {
                     satellite.display.teleport(newPos);
                  }

                  if (satellite.antenna != null && satellite.antenna.isValid()) {
                     satellite.antenna.teleport(newPos.clone().add(0.0D, 0.5D, 0.0D));
                  }

                  ParticleUtils.endRod(newPos, 1, 0.3D, 0.0D);
                  if (this.tick % 5 == 0) {
                     ParticleUtils.electricSpark(newPos, 2, 0.5D, 0.02D);
                  }
               }

               --this.beamCooldown;
               if (this.beamCooldown <= 0) {
                  CelestialSatelliteNetworkAbility.SatelliteData satellitex = (CelestialSatelliteNetworkAbility.SatelliteData)satellites.get(this.currentSatelliteIndex);
                  Location satPos = CelestialSatelliteNetworkAbility.this.calculateSatellitePosition(origin, satellitex);
                  targetAngle = CelestialSatelliteNetworkAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double targetDist = CelestialSatelliteNetworkAbility.this.random.nextDouble() * settings.radius;
                  Location targetLoc = origin.clone().add(Math.cos(targetAngle) * targetDist, 0.0D, Math.sin(targetAngle) * targetDist);
                  CelestialSatelliteNetworkAbility.this.fireBeam(player, satPos, targetLoc, settings);
                  this.currentSatelliteIndex = (this.currentSatelliteIndex + 1) % satellites.size();
                  this.beamCooldown = 15;
               }

               if (this.tick % 40 == 0) {
                  ParticleUtils.playSound(origin, Sound.BLOCK_CONDUIT_AMBIENT, 1.0F, 1.2F);
               }

               ++this.tick;
            } else {
               Iterator var1 = satellites.iterator();

               while(var1.hasNext()) {
                  satellite = (CelestialSatelliteNetworkAbility.SatelliteData)var1.next();
                  CelestialSatelliteNetworkAbility.this.cleanupSatellite(satellite);
               }

               CelestialSatelliteNetworkAbility.this.createFinalBurst(player, origin, satellites.size(), settings);
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
         ParticleUtils.playSound(from, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0F, 1.5F);
         ParticleUtils.flash(from);
         ParticleUtils.soulFlame(from, 15, 0.5D, 0.05D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 10) {
                  this.cancel();
               } else {
                  double scale = 0.5D + (double)this.tick * 0.1D;
                  ParticleUtils.ring(groundTarget, Particle.END_ROD, scale, 12);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         this.runLater(() -> {
            ParticleUtils.playSound(groundTarget, Sound.ENTITY_GUARDIAN_ATTACK, 1.5F, 0.5F);
            ParticleUtils.playSound(groundTarget, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick >= 15) {
                     this.cancel();
                  } else {
                     double intensity = 1.0D - (double)this.tick / 15.0D;
                     ParticleUtils.beam(from, groundTarget, 0.5D);
                     if (this.tick < 8) {
                        Location current = from.clone();
                        Vector dir = groundTarget.toVector().subtract(from.toVector()).normalize().multiply(1.5D);
                        double dist = from.distance(groundTarget);

                        for(double d = 0.0D; d < dist; ++d) {
                           ParticleUtils.soulFlame(current, (int)(3.0D * intensity), 0.2D, 0.01D);
                           ParticleUtils.endRod(current, (int)(2.0D * intensity), 0.1D, 0.0D);
                           current.add(dir);
                        }
                     }

                     ++this.tick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
            ParticleUtils.flash(groundTarget);
            ParticleUtils.soulFlame(groundTarget, 40, 1.5D, 0.15D);
            ParticleUtils.endRod(groundTarget, 25, 1.2D, 0.2D);
            ParticleUtils.electricSpark(groundTarget, 20, 2.0D, 0.1D);
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick >= 10) {
                     this.cancel();
                  } else {
                     double radius = (double)this.tick * 0.5D;
                     ParticleUtils.ring(groundTarget, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 8);
                     ++this.tick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
            DamageZone zone = DamageZone.cylinder(player, groundTarget, 3.0D, 5.0D, settings.damagePerTick, settings.damageTickInterval, 20);
            DamageZoneManager.getInstance(this.plugin).register(zone);
         }, 10L);
      }
   }

   private void cleanupSatellite(CelestialSatelliteNetworkAbility.SatelliteData satellite) {
      if (satellite.display != null && satellite.display.isValid()) {
         Location loc = satellite.display.getLocation();
         ParticleUtils.flash(loc);
         ParticleUtils.endRod(loc, 20, 1.0D, 0.2D);
         ParticleUtils.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.5F);
         satellite.display.remove();
      }

      if (satellite.antenna != null && satellite.antenna.isValid()) {
         satellite.antenna.remove();
      }

   }

   private void createFinalBurst(Player player, Location origin, int satelliteCount, IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 0.6F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.8F);
      ParticleUtils.flash(origin.clone().add(0.0D, 2.0D, 0.0D));
      ParticleUtils.soulFlame(origin.clone().add(0.0D, 1.0D, 0.0D), 150, 4.0D, 0.25D);
      ParticleUtils.endRod(origin.clone().add(0.0D, 2.0D, 0.0D), 100, 3.0D, 0.3D);
      ParticleUtils.firework(origin.clone().add(0.0D, 3.0D, 0.0D), 60, 2.5D, 0.4D);
      DamageZone zone = DamageZone.sphere(player, origin, settings.radius, settings.damagePerTick * 2.0D, settings.damageTickInterval, 30);
      DamageZoneManager.getInstance(this.plugin).register(zone);
   }

   private static class SatelliteData {
      double angle;
      double orbitRadius;
      double orbitHeight;
      BlockDisplay display;
      BlockDisplay antenna;

      SatelliteData(double angle, double orbitRadius, double orbitHeight) {
         this.angle = angle;
         this.orbitRadius = orbitRadius;
         this.orbitHeight = orbitHeight;
      }
   }
}
