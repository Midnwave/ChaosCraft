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

public class CascadeDetonationAbility extends Ability {
   private final Random random = new Random();

   public CascadeDetonationAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "cascade_detonation";
   }

   public String getDisplayName() {
      return "Cascade Detonation";
   }

   public String getDescription() {
      return "Fire a projectile that embeds in the ground and triggers a devastating chain reaction of 30+ explosions expanding outward.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      int chainCount = settings.getInt("chain-count", 30);
      int delayBetweenChains = settings.getInt("delay-between-chains-ticks", 3);
      ParticleUtils.playSound(origin, Sound.ENTITY_TNT_PRIMED, 2.0F, 1.2F);
      ParticleUtils.playSound(origin, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5F, 0.8F);
      this.fireInitialProjectile(player, origin, chainCount, delayBetweenChains, settings);
   }

   private void fireInitialProjectile(final Player player, final Location origin, final int chainCount, final int delayBetweenChains, final IvoryConfig.AbilitySettings settings) {
      final World world = origin.getWorld();
      if (world != null) {
         final BlockDisplay projectile = (BlockDisplay)world.spawn(origin.clone().add(0.0D, 1.0D, 0.0D), BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.2F, -0.2F, -0.2F), t.getLeftRotation(), new Vector3f(0.4F, 0.4F, 0.4F), t.getRightRotation()));
         });
         (new BukkitRunnable() {
            Location currentLoc = origin.clone().add(0.0D, 1.0D, 0.0D);
            int tick = 0;
            double speed = 2.0D;

            public void run() {
               double groundY = (double)(world.getHighestBlockYAt(this.currentLoc) + 1);
               if (!(this.currentLoc.getY() <= groundY) && this.tick < 30) {
                  this.currentLoc.add(0.0D, -0.3D, -this.speed);
                  if (projectile.isValid()) {
                     projectile.teleport(this.currentLoc);
                  }

                  ParticleUtils.soulFlame(this.currentLoc, 8, 0.3D, 0.02D);
                  ParticleUtils.endRod(this.currentLoc, 5, 0.2D, 0.01D);
                  ++this.tick;
               } else {
                  if (projectile.isValid()) {
                     projectile.remove();
                  }

                  Location impactLoc = this.currentLoc.clone();
                  impactLoc.setY(groundY);
                  CascadeDetonationAbility.this.createEmbeddedMarker(impactLoc);
                  CascadeDetonationAbility.this.runLater(() -> {
                     CascadeDetonationAbility.this.startChainReaction(player, impactLoc, chainCount, delayBetweenChains, settings);
                  }, 40L);
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createEmbeddedMarker(final Location loc) {
      World world = loc.getWorld();
      if (world != null) {
         ParticleUtils.playSound(loc, Sound.BLOCK_STONE_HIT, 1.5F, 0.8F);
         final BlockDisplay marker = (BlockDisplay)world.spawn(loc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.CRYING_OBSIDIAN.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.25F, -0.1F, -0.25F), t.getLeftRotation(), new Vector3f(0.5F, 0.2F, 0.5F), t.getRightRotation()));
         });
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 40) {
                  if (marker.isValid()) {
                     marker.remove();
                  }

                  this.cancel();
               } else {
                  ParticleUtils.soulFlame(loc, 5, 0.4D, 0.02D);
                  if (this.tick % 5 == 0) {
                     ParticleUtils.ring(loc, Particle.END_ROD, 1.0D, 15);
                     ParticleUtils.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 1.5F + (float)this.tick * 0.02F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void startChainReaction(final Player player, final Location initialLoc, final int totalChains, int delayBetweenChains, final IvoryConfig.AbilitySettings settings) {
      final World world = initialLoc.getWorld();
      if (world != null) {
         this.createExplosion(player, initialLoc, 1.0D, settings, true);
         final List<Location> explosionQueue = new ArrayList();
         explosionQueue.add(initialLoc);
         (new BukkitRunnable() {
            int generation = 0;
            int explosionsCreated = 1;
            List<Location> currentGen = new ArrayList(explosionQueue);
            List<Location> nextGen = new ArrayList();

            public void run() {
               if (this.explosionsCreated < totalChains && !this.currentGen.isEmpty()) {
                  Iterator var1 = this.currentGen.iterator();

                  while(var1.hasNext()) {
                     Location loc = (Location)var1.next();
                     if (this.explosionsCreated >= totalChains) {
                        break;
                     }

                     int children = this.generation == 0 ? 6 : 4;
                     double spread = 3.0D + (double)this.generation * 0.5D;

                     for(int i = 0; i < children && this.explosionsCreated < totalChains; ++i) {
                        double angle = 6.283185307179586D / (double)children * (double)i + CascadeDetonationAbility.this.random.nextDouble() * 0.5D;
                        double dist = spread + CascadeDetonationAbility.this.random.nextDouble() * 2.0D;
                        double x = Math.cos(angle) * dist;
                        double z = Math.sin(angle) * dist;
                        Location childLoc = loc.clone().add(x, 0.0D, z);
                        childLoc.setY((double)world.getHighestBlockYAt(childLoc) + 0.5D);
                        int delay = i * 2;
                        CascadeDetonationAbility.this.runLater(() -> {
                           CascadeDetonationAbility.this.createExplosion(player, childLoc, 0.8D - (double)this.generation * 0.1D, settings, false);
                        }, (long)delay);
                        this.nextGen.add(childLoc);
                        ++this.explosionsCreated;
                     }
                  }

                  this.currentGen = new ArrayList(this.nextGen);
                  this.nextGen.clear();
                  ++this.generation;
               } else {
                  CascadeDetonationAbility.this.createFinale(player, initialLoc, settings);
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, (long)delayBetweenChains);
      }
   }

   private void createExplosion(Player player, final Location loc, final double scale, IvoryConfig.AbilitySettings settings, boolean isInitial) {
      World world = loc.getWorld();
      if (world != null) {
         float pitch = 0.7F + this.random.nextFloat() * 0.6F;
         ParticleUtils.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, isInitial ? 2.0F : 1.2F, pitch);
         ParticleUtils.flash(loc);
         int baseParticles = isInitial ? 50 : 30;
         double baseRadius = isInitial ? 3.0D : 2.0D;
         ParticleUtils.soulFlame(loc, (int)((double)baseParticles * scale), baseRadius * scale, 0.2D);
         ParticleUtils.endRod(loc, (int)((double)baseParticles * 0.6D * scale), baseRadius * 0.8D * scale, 0.25D);
         ParticleUtils.firework(loc, (int)((double)baseParticles * 0.4D * scale), baseRadius * 0.6D * scale, 0.3D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  double ringRadius = (double)this.tick * 0.6D * scale;
                  ParticleUtils.ring(loc, Particle.SOUL_FIRE_FLAME, ringRadius, (int)(ringRadius * 6.0D) + 8);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 20) {
                  this.cancel();
               } else {
                  double intensity = 1.0D - (double)this.tick / 20.0D;
                  ParticleUtils.soulFlame(loc, (int)(5.0D * intensity * scale), 1.0D * scale, 0.02D);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 3L, 2L);
         double damageRadius = (isInitial ? 3.0D : 2.0D) * scale;
         DamageZone zone = DamageZone.sphere(player, loc, damageRadius, settings.damagePerTick * (isInitial ? 1.5D : 1.0D), settings.damageTickInterval, 15);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void createFinale(Player player, Location center, IvoryConfig.AbilitySettings settings) {
      this.runLater(() -> {
         World world = center.getWorld();
         if (world != null) {
            ParticleUtils.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
            ParticleUtils.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F);

            for(int i = 0; i < 5; ++i) {
               double x = (this.random.nextDouble() - 0.5D) * 10.0D;
               double z = (this.random.nextDouble() - 0.5D) * 10.0D;
               ParticleUtils.flash(center.clone().add(x, 2.0D, z));
            }

            ParticleUtils.soulFlame(center, 150, 8.0D, 0.4D);
            ParticleUtils.endRod(center.clone().add(0.0D, 2.0D, 0.0D), 100, 6.0D, 0.5D);
            ParticleUtils.firework(center.clone().add(0.0D, 4.0D, 0.0D), 80, 5.0D, 0.6D);
            ParticleUtils.electricSpark(center, 60, 10.0D, 0.3D);
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick >= 25) {
                     this.cancel();
                  } else {
                     double radius = (double)this.tick * 1.5D;
                     ParticleUtils.ring(center, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 10.0D) + 30);
                     ParticleUtils.ring(center.clone().add(0.0D, 0.5D, 0.0D), Particle.END_ROD, radius * 0.9D, (int)(radius * 6.0D) + 15);
                     ++this.tick;
                  }
               }
            }).runTaskTimer(this.plugin, 0L, 1L);
            DamageZone zone = DamageZone.sphere(player, center, settings.radius * 1.5D, settings.damagePerTick * 2.5D, settings.damageTickInterval, 40);
            DamageZoneManager.getInstance(this.plugin).register(zone);
         }
      }, 10L);
   }
}
