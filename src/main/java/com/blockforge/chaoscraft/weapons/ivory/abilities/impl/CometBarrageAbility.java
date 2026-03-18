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

public class CometBarrageAbility extends Ability {
   private final Random random = new Random();

   public CometBarrageAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "comet_barrage";
   }

   public String getDisplayName() {
      return "Comet Barrage";
   }

   public String getDescription() {
      return "Summon a devastating barrage of celestial comets that rain down from the heavens, each exploding on impact with brilliant starfire.";
   }

   public void execute(final Player player) {
      final IvoryConfig.AbilitySettings settings = this.getSettings();
      final Location origin = this.getExecutionLocation(player);
      final double radius = settings.radius;
      final int cometCount = settings.getInt("comet-count", 20);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.0F, 1.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.8F);
      this.createSkyPortal(origin, radius);
      (new BukkitRunnable() {
         int cometsSpawned = 0;
         int tick = 0;

         public void run() {
            if (this.cometsSpawned >= cometCount) {
               CometBarrageAbility.this.runLater(() -> {
                  CometBarrageAbility.this.spawnMegaComet(player, origin, settings);
               }, 20L);
               this.cancel();
            } else {
               if (this.tick % 3 == 0) {
                  double angle = CometBarrageAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double dist = CometBarrageAbility.this.random.nextDouble() * radius;
                  double x = Math.cos(angle) * dist;
                  double z = Math.sin(angle) * dist;
                  Location targetLoc = origin.clone().add(x, 0.0D, z);
                  CometBarrageAbility.this.spawnComet(player, targetLoc, settings);
                  ++this.cometsSpawned;
                  if (CometBarrageAbility.this.random.nextDouble() < 0.3D && this.cometsSpawned < cometCount) {
                     angle = CometBarrageAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     dist = CometBarrageAbility.this.random.nextDouble() * radius;
                     x = Math.cos(angle) * dist;
                     z = Math.sin(angle) * dist;
                     targetLoc = origin.clone().add(x, 0.0D, z);
                     CometBarrageAbility.this.spawnComet(player, targetLoc, settings);
                     ++this.cometsSpawned;
                  }
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 30L, 1L);
   }

   private void createSkyPortal(Location origin, final double radius) {
      final Location portalCenter = origin.clone().add(0.0D, 40.0D, 0.0D);
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 30) {
               this.cancel();
            } else {
               World world = portalCenter.getWorld();
               if (world == null) {
                  this.cancel();
               } else {
                  double currentRadius = radius * ((double)this.tick / 30.0D);

                  int i;
                  double angle;
                  double x;
                  double z;
                  Location beamStart;
                  for(i = 0; i < 40; ++i) {
                     angle = 0.15707963267948966D * (double)i + this.rotation;
                     x = Math.cos(angle) * currentRadius;
                     z = Math.sin(angle) * currentRadius;
                     beamStart = portalCenter.clone().add(x, 0.0D, z);
                     world.spawnParticle(Particle.SOUL_FIRE_FLAME, beamStart, 1, 0.1D, 0.1D, 0.1D, 0.0D);
                  }

                  for(i = 0; i < 30; ++i) {
                     angle = 0.20943951023931953D * (double)i - this.rotation * 1.5D;
                     x = Math.cos(angle) * currentRadius * 0.6D;
                     z = Math.sin(angle) * currentRadius * 0.6D;
                     beamStart = portalCenter.clone().add(x, 0.0D, z);
                     world.spawnParticle(Particle.END_ROD, beamStart, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                  }

                  ParticleUtils.electricSpark(portalCenter, 10, currentRadius * 0.3D, 0.05D);
                  if (this.tick % 5 == 0) {
                     for(i = 0; i < 4; ++i) {
                        angle = this.rotation + (double)i * 3.141592653589793D / 2.0D;
                        x = Math.cos(angle) * currentRadius * 0.8D;
                        z = Math.sin(angle) * currentRadius * 0.8D;
                        beamStart = portalCenter.clone().add(x, 0.0D, z);

                        for(double y = 0.0D; y > -10.0D; y -= 0.5D) {
                           Location beamPoint = beamStart.clone().add(0.0D, y, 0.0D);
                           world.spawnParticle(Particle.END_ROD, beamPoint, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        }
                     }
                  }

                  if (this.tick % 8 == 0) {
                     ParticleUtils.flash(portalCenter);
                  }

                  this.rotation += 0.2D;
                  ++this.tick;
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spawnComet(final Player player, final Location targetLoc, final IvoryConfig.AbilitySettings settings) {
      final World world = targetLoc.getWorld();
      if (world != null) {
         double offsetX = (this.random.nextDouble() - 0.5D) * 20.0D;
         double offsetZ = (this.random.nextDouble() - 0.5D) * 20.0D;
         final Location spawnLoc = targetLoc.clone().add(offsetX, 50.0D, offsetZ);
         final Vector direction = targetLoc.toVector().subtract(spawnLoc.toVector()).normalize();
         double distance = spawnLoc.distance(targetLoc);
         final double speed = 3.0D + this.random.nextDouble() * 2.0D;
         final int travelTicks = (int)(distance / speed);
         this.createCometMarker(targetLoc);
         ParticleUtils.playSound(spawnLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 0.5F);
         (new BukkitRunnable() {
            Location currentLoc = spawnLoc.clone();
            int tick = 0;
            BlockDisplay cometCore = null;
            double rotation;

            {
               this.rotation = CometBarrageAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
            }

            public void run() {
               if (this.tick < travelTicks && !(this.currentLoc.getY() <= targetLoc.getY())) {
                  this.currentLoc.add(direction.clone().multiply(speed));
                  if (this.tick == 0) {
                     this.cometCore = (BlockDisplay)world.spawn(this.currentLoc, BlockDisplay.class, (d) -> {
                        d.setBlock(Material.GLOWSTONE.createBlockData());
                        d.setGlowing(true);
                        d.setBrightness(new Brightness(15, 15));
                        float size = 0.4F + CometBarrageAbility.this.random.nextFloat() * 0.3F;
                        Transformation transform = d.getTransformation();
                        d.setTransformation(new Transformation(new Vector3f(-size / 2.0F, -size / 2.0F, -size / 2.0F), transform.getLeftRotation(), new Vector3f(size, size, size), transform.getRightRotation()));
                     });
                  } else if (this.cometCore != null && this.cometCore.isValid()) {
                     this.cometCore.teleport(this.currentLoc);
                  }

                  ParticleUtils.soulFlame(this.currentLoc, 8, 0.3D, 0.02D);
                  ParticleUtils.endRod(this.currentLoc, 5, 0.2D, 0.01D);
                  ParticleUtils.glow(this.currentLoc, 3, 0.2D, 0.0D);
                  Location tailLoc = this.currentLoc.clone();
                  Vector tailDir = direction.clone().multiply(-1);

                  int i;
                  double angle;
                  for(i = 0; i < 8; ++i) {
                     tailLoc.add(tailDir.clone().multiply(0.5D));
                     angle = 1.0D - (double)i / 8.0D;
                     int count = (int)(5.0D * angle);
                     ParticleUtils.soulFlame(tailLoc, count, 0.15D + (double)i * 0.05D, 0.02D);
                     if (i % 2 == 0) {
                        ParticleUtils.endRod(tailLoc, (int)(3.0D * angle), 0.1D + (double)i * 0.03D, 0.01D);
                     }
                  }

                  for(i = 0; i < 3; ++i) {
                     angle = this.rotation + (double)i * 3.141592653589793D * 2.0D / 3.0D;
                     double spiralRadius = 0.5D;
                     double x = Math.cos(angle) * spiralRadius;
                     double y = Math.sin(angle) * spiralRadius;
                     Location sparkLoc = this.currentLoc.clone().add(x, y, 0.0D);
                     ParticleUtils.electricSpark(sparkLoc, 1, 0.05D, 0.0D);
                  }

                  this.rotation += 0.4D;
                  ++this.tick;
               } else {
                  if (this.cometCore != null && this.cometCore.isValid()) {
                     this.cometCore.remove();
                  }

                  CometBarrageAbility.this.createCometImpact(player, this.currentLoc, settings);
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createCometMarker(final Location loc) {
      (new BukkitRunnable() {
         int tick = 0;
         double rotation = 0.0D;

         public void run() {
            if (this.tick >= 25) {
               this.cancel();
            } else {
               double pulseScale = 0.5D + Math.sin((double)this.tick * 0.4D) * 0.2D;
               ParticleUtils.ring(loc.clone().add(0.0D, 0.1D, 0.0D), Particle.END_ROD, 1.5D * pulseScale, 16);

               for(int i = 0; i < 4; ++i) {
                  double angle = this.rotation + (double)i * 3.141592653589793D / 2.0D;
                  double x = Math.cos(angle) * pulseScale;
                  double z = Math.sin(angle) * pulseScale;
                  ParticleUtils.spawnSingle(loc.clone().add(x, 0.1D, z), Particle.SOUL_FIRE_FLAME);
               }

               this.rotation += 0.15D;
               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void createCometImpact(Player player, Location loc, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         final Location groundLoc = loc.clone();
         groundLoc.setY((double)world.getHighestBlockYAt(loc) + 0.5D);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0.9F);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 0.7F);
         ParticleUtils.flash(groundLoc);
         ParticleUtils.soulFlame(groundLoc, 60, 1.5D, 0.2D);
         ParticleUtils.endRod(groundLoc, 40, 1.2D, 0.3D);
         ParticleUtils.firework(groundLoc, 25, 1.0D, 0.25D);
         ParticleUtils.electricSpark(groundLoc, 30, 2.0D, 0.15D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 10) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 0.5D;
                  ParticleUtils.ring(groundLoc, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 8.0D) + 8);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.sphere(player, groundLoc, settings.radius / 5.0D, settings.damagePerTick, settings.damageTickInterval, 20);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void spawnMegaComet(final Player player, final Location origin, final IvoryConfig.AbilitySettings settings) {
      final World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
         ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, 2.0F, 0.5F);
         final Location spawnLoc = origin.clone().add(0.0D, 60.0D, 0.0D);
         final Vector direction = new Vector(0, -1, 0);
         final double speed = 2.0D;
         final int travelTicks = 30;
         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  double scale = 1.0D + (double)this.tick * 0.1D;
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.1D, 0.0D), Particle.END_ROD, 3.0D * scale, 40);
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.1D, 0.0D), Particle.SOUL_FIRE_FLAME, 2.0D * scale, 30);
                  ParticleUtils.ring(origin.clone().add(0.0D, 0.1D, 0.0D), Particle.ELECTRIC_SPARK, 4.0D * scale, 50);

                  for(int i = 0; i < 8; ++i) {
                     double angle = this.rotation + (double)i * 3.141592653589793D / 4.0D;

                     for(double r = 0.0D; r < 4.0D * scale; r += 0.3D) {
                        double x = Math.cos(angle) * r;
                        double z = Math.sin(angle) * r;
                        ParticleUtils.spawnSingle(origin.clone().add(x, 0.1D, z), Particle.END_ROD);
                     }
                  }

                  this.rotation += 0.1D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            Location currentLoc = spawnLoc.clone();
            int tick = 0;
            List<BlockDisplay> cometParts = new ArrayList();
            double rotation = 0.0D;

            public void run() {
               BlockDisplay part;
               if (this.tick >= travelTicks) {
                  Iterator var13 = this.cometParts.iterator();

                  while(var13.hasNext()) {
                     part = (BlockDisplay)var13.next();
                     if (part.isValid()) {
                        part.remove();
                     }
                  }

                  CometBarrageAbility.this.createMegaImpact(player, origin, settings);
                  this.cancel();
               } else {
                  this.currentLoc.add(direction.clone().multiply(speed));
                  int ix;
                  double fade;
                  double x;
                  double z;
                  if (this.tick == 0) {
                     BlockDisplay core = (BlockDisplay)world.spawn(this.currentLoc, BlockDisplay.class, (d) -> {
                        d.setBlock(Material.GLOWSTONE.createBlockData());
                        d.setGlowing(true);
                        d.setBrightness(new Brightness(15, 15));
                        Transformation t = d.getTransformation();
                        d.setTransformation(new Transformation(new Vector3f(-0.75F, -0.75F, -0.75F), t.getLeftRotation(), new Vector3f(1.5F, 1.5F, 1.5F), t.getRightRotation()));
                     });
                     this.cometParts.add(core);

                     for(ix = 0; ix < 6; ++ix) {
                        fade = 1.0471975511965976D * (double)ix;
                        x = Math.cos(fade) * 2.0D;
                        z = Math.sin(fade) * 2.0D;
                        Location partLoc = this.currentLoc.clone().add(x, 0.0D, z);
                        BlockDisplay partx = (BlockDisplay)world.spawn(partLoc, BlockDisplay.class, (d) -> {
                           d.setBlock(Material.SEA_LANTERN.createBlockData());
                           d.setGlowing(true);
                           d.setBrightness(new Brightness(15, 15));
                           Transformation t = d.getTransformation();
                           d.setTransformation(new Transformation(new Vector3f(-0.3F, -0.3F, -0.3F), t.getLeftRotation(), new Vector3f(0.6F, 0.6F, 0.6F), t.getRightRotation()));
                        });
                        this.cometParts.add(partx);
                     }
                  } else {
                     if (!this.cometParts.isEmpty() && ((BlockDisplay)this.cometParts.get(0)).isValid()) {
                        ((BlockDisplay)this.cometParts.get(0)).teleport(this.currentLoc);
                     }

                     for(int i = 1; i < this.cometParts.size(); ++i) {
                        part = (BlockDisplay)this.cometParts.get(i);
                        if (part.isValid()) {
                           fade = this.rotation + 1.0471975511965976D * (double)(i - 1);
                           x = Math.cos(fade) * 2.0D;
                           z = Math.sin(fade) * 2.0D;
                           part.teleport(this.currentLoc.clone().add(x, Math.sin(this.rotation * 2.0D + (double)i) * 0.5D, z));
                        }
                     }
                  }

                  ParticleUtils.soulFlame(this.currentLoc, 30, 1.5D, 0.05D);
                  ParticleUtils.endRod(this.currentLoc, 20, 1.2D, 0.03D);
                  ParticleUtils.electricSpark(this.currentLoc, 15, 2.0D, 0.04D);
                  ParticleUtils.glow(this.currentLoc, 10, 1.0D, 0.0D);
                  Location tailLoc = this.currentLoc.clone();

                  for(ix = 0; ix < 20; ++ix) {
                     tailLoc.add(0.0D, 1.5D, 0.0D);
                     fade = 1.0D - (double)ix / 20.0D;
                     ParticleUtils.soulFlame(tailLoc, (int)(15.0D * fade), 0.5D + (double)ix * 0.1D, 0.03D);
                     if (ix % 2 == 0) {
                        ParticleUtils.endRod(tailLoc, (int)(8.0D * fade), 0.3D + (double)ix * 0.08D, 0.02D);
                     }
                  }

                  if (this.tick % 5 == 0) {
                     ParticleUtils.playSound(this.currentLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5F, 0.3F);
                  }

                  this.rotation += 0.3D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 30L, 1L);
      }
   }

   private void createMegaImpact(Player player, final Location origin, IvoryConfig.AbilitySettings settings) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.4F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.5F, 0.8F);
         ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 2.0F, 0.5F);
         ParticleUtils.flash(origin);
         ParticleUtils.flash(origin.clone().add(0.0D, 5.0D, 0.0D));
         ParticleUtils.flash(origin.clone().add(0.0D, 10.0D, 0.0D));
         ParticleUtils.soulFlame(origin, 300, 8.0D, 0.4D);
         ParticleUtils.endRod(origin.clone().add(0.0D, 2.0D, 0.0D), 200, 6.0D, 0.5D);
         ParticleUtils.firework(origin.clone().add(0.0D, 3.0D, 0.0D), 150, 5.0D, 0.6D);
         ParticleUtils.electricSpark(origin, 120, 10.0D, 0.3D);
         ParticleUtils.glow(origin.clone().add(0.0D, 5.0D, 0.0D), 80, 6.0D, 0.2D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 30) {
                  this.cancel();
               } else {
                  double radius = (double)this.tick * 1.0D;
                  ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, radius, (int)(radius * 10.0D) + 20);

                  for(int h = 0; h < 5; ++h) {
                     double heightRadius = radius * (1.0D - (double)h * 0.15D);
                     if (heightRadius > 0.0D) {
                        Location ringLoc = origin.clone().add(0.0D, (double)(h * 2), 0.0D);
                        ParticleUtils.ring(ringLoc, Particle.END_ROD, heightRadius, (int)(heightRadius * 6.0D) + 10);
                     }
                  }

                  if (this.tick % 3 == 0) {
                     ParticleUtils.flash(origin.clone().add(0.0D, (double)this.tick * 0.5D, 0.0D));
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  double height = (double)this.tick * 1.5D;
                  double intensity = 1.0D - (double)this.tick / 40.0D;

                  for(double y = 0.0D; y < height; y += 0.5D) {
                     Location pillarLoc = origin.clone().add(0.0D, y, 0.0D);
                     double widthAtHeight = (1.0D + y * 0.1D) * intensity;
                     ParticleUtils.soulFlame(pillarLoc, (int)(8.0D * intensity), widthAtHeight, 0.02D);
                     if (y % 2.0D < 0.5D) {
                        ParticleUtils.endRod(pillarLoc, (int)(4.0D * intensity), widthAtHeight * 0.8D, 0.01D);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.sphere(player, origin, settings.radius, settings.damagePerTick * 3.0D, settings.damageTickInterval, 60);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }
}
