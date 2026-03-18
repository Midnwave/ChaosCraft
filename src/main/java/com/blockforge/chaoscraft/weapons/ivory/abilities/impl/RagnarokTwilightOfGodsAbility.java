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
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RagnarokTwilightOfGodsAbility extends Ability {
   private final Random random = new Random();

   public RagnarokTwilightOfGodsAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "ragnarok_twilight_of_gods";
   }

   public String getDisplayName() {
      return "Ragnarok: Twilight of Gods";
   }

   public String getDescription() {
      return "Invoke the end of all things - the divine apocalypse where reality itself shatters under the weight of ultimate judgment.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_SPAWN, 3.0F, 0.3F);
      ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 2.5F, 0.4F);
      ParticleUtils.playSound(origin, Sound.BLOCK_END_PORTAL_SPAWN, 2.0F, 0.5F);
      List<BlockDisplay> allDisplays = new ArrayList();
      this.createSkyRift(origin, radius, allDisplays);
      this.runLater(() -> {
         this.descendDivineChains(origin, radius, allDisplays);
      }, 40L);
      this.runLater(() -> {
         this.rainFallenStars(player, origin, radius, settings);
      }, 80L);
      this.runLater(() -> {
         this.shatterReality(player, origin, radius, settings, allDisplays);
      }, 160L);
      this.runLater(() -> {
         Iterator var1 = allDisplays.iterator();

         while(var1.hasNext()) {
            BlockDisplay d = (BlockDisplay)var1.next();
            if (d.isValid()) {
               d.remove();
            }
         }

      }, 280L);
   }

   private void createSkyRift(final Location origin, final double radius, final List<BlockDisplay> displays) {
      final World world = origin.getWorld();
      if (world != null) {
         final Location riftCenter = origin.clone().add(0.0D, 35.0D, 0.0D);
         (new BukkitRunnable() {
            int tick = 0;
            double rotation = 0.0D;
            List<BlockDisplay> riftDisplays = new ArrayList();

            public void run() {
               if (this.tick >= 40) {
                  this.cancel();
               } else {
                  double progress = (double)this.tick / 40.0D;
                  double riftSize = progress * radius * 0.8D;
                  int i;
                  double angle;
                  if (this.tick == 10) {
                     for(i = 0; i < 12; ++i) {
                        angle = 0.5235987755982988D * (double)i;
                        Location edgeLoc = riftCenter.clone().add(Math.cos(angle) * radius * 0.4D, 0.0D, Math.sin(angle) * radius * 0.4D);
                        BlockDisplay edgex = (BlockDisplay)world.spawn(edgeLoc, BlockDisplay.class, (d) -> {
                           d.setBlock(Material.CRYING_OBSIDIAN.createBlockData());
                           d.setGlowing(true);
                           d.setBrightness(new Brightness(15, 15));
                           Transformation t = d.getTransformation();
                           d.setTransformation(new Transformation(new Vector3f(-0.3F, -0.3F, -0.3F), t.getLeftRotation(), new Vector3f(0.6F, 0.6F, 0.6F), t.getRightRotation()));
                        });
                        this.riftDisplays.add(edgex);
                        displays.add(edgex);
                     }
                  }

                  double x;
                  for(i = 0; i < this.riftDisplays.size(); ++i) {
                     BlockDisplay edge = (BlockDisplay)this.riftDisplays.get(i);
                     if (edge.isValid()) {
                        x = 0.5235987755982988D * (double)i + this.rotation;
                        Location newLoc = riftCenter.clone().add(Math.cos(x) * riftSize, Math.sin((double)this.tick * 0.2D) * 2.0D, Math.sin(x) * riftSize);
                        edge.teleport(newLoc);
                     }
                  }

                  for(i = 0; i < (int)(30.0D * progress); ++i) {
                     angle = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     double dist = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * riftSize;
                     double xx = Math.cos(angle) * dist;
                     double z = Math.sin(angle) * dist;
                     double y = (RagnarokTwilightOfGodsAbility.this.random.nextDouble() - 0.5D) * 4.0D;
                     Location voidPoint = riftCenter.clone().add(xx, y, z);
                     ParticleUtils.electricSpark(voidPoint, 1, 0.3D, 0.05D);
                  }

                  for(double anglex = 0.0D; anglex < 6.283185307179586D; anglex += 0.2D) {
                     x = Math.cos(anglex + this.rotation) * riftSize;
                     double zx = Math.sin(anglex + this.rotation) * riftSize;
                     ParticleUtils.soulFlame(riftCenter.clone().add(x, 0.0D, zx), 2, 0.2D, 0.02D);
                  }

                  ParticleUtils.ring(origin, Particle.END_ROD, riftSize, (int)(riftSize * 10.0D) + 20);
                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(riftCenter, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5F, (float)(0.4000000059604645D + progress * 0.30000001192092896D));
                  }

                  this.rotation += 0.08D;
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void descendDivineChains(Location origin, double radius, List<BlockDisplay> displays) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.BLOCK_CHAIN_PLACE, 2.0F, 0.3F);
         ParticleUtils.playSound(origin, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5F, 0.5F);

         for(int chainIndex = 0; chainIndex < 6; ++chainIndex) {
            double angle = 1.0471975511965976D * (double)chainIndex;
            double chainX = Math.cos(angle) * radius * 0.5D;
            double chainZ = Math.sin(angle) * radius * 0.5D;
            Location chainTarget = origin.clone().add(chainX, 0.0D, chainZ);
            int delay = chainIndex * 5;
            this.runLater(() -> {
               (new BukkitRunnable() {
                  int tick = 0;
                  List<BlockDisplay> chainLinks = new ArrayList();
                  double chainY = 35.0D;

                  public void run() {
                     if (this.tick >= 40) {
                        this.cancel();
                     } else {
                        if (this.tick % 3 == 0 && this.chainY > 0.0D) {
                           Location linkLoc = chainTarget.clone().add(0.0D, this.chainY, 0.0D);
                           BlockDisplay link = (BlockDisplay)world.spawn(linkLoc, BlockDisplay.class, (d) -> {
                              d.setBlock(Material.GOLD_BLOCK.createBlockData());
                              d.setGlowing(true);
                              d.setBrightness(new Brightness(15, 15));
                              Transformation t = d.getTransformation();
                              d.setTransformation(new Transformation(new Vector3f(-0.2F, -0.4F, -0.2F), (new Quaternionf()).rotateY((float)((double)this.tick * 0.2D)), new Vector3f(0.4F, 0.8F, 0.4F), t.getRightRotation()));
                           });
                           this.chainLinks.add(link);
                           displays.add(link);
                           this.chainY -= 2.5D;
                           ParticleUtils.playSound(linkLoc, Sound.BLOCK_CHAIN_PLACE, 0.5F, 0.6F);
                        }

                        for(double y = this.chainY; y < 35.0D; ++y) {
                           Location chainPoint = chainTarget.clone().add(0.0D, y, 0.0D);
                           if (RagnarokTwilightOfGodsAbility.this.random.nextDouble() < 0.2D) {
                              ParticleUtils.endRod(chainPoint, 1, 0.2D, 0.02D);
                           }
                        }

                        if (this.chainY <= 0.0D && this.tick % 5 == 0) {
                           ParticleUtils.soulFlame(chainTarget, 10, 1.0D, 0.05D);
                           ParticleUtils.electricSpark(chainTarget, 5, 0.8D, 0.03D);
                        }

                        ++this.tick;
                     }
                  }
               }).runTaskTimer(this.plugin, 0L, 1L);
            }, (long)delay);
         }

      }
   }

   private void rainFallenStars(final Player player, final Location origin, final double radius, final IvoryConfig.AbilitySettings settings) {
      ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.5F, 0.4F);
      (new BukkitRunnable() {
         int tick = 0;
         int starsSpawned = 0;
         final int maxStars = 20;

         public void run() {
            if (this.tick >= 80) {
               this.cancel();
            } else {
               double angle;
               if (this.tick % 4 == 0 && this.starsSpawned < 20) {
                  double spawnAngle = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  angle = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * radius * 0.7D;
                  Location targetLoc = origin.clone().add(Math.cos(spawnAngle) * angle, 0.0D, Math.sin(spawnAngle) * angle);
                  RagnarokTwilightOfGodsAbility.this.spawnFallenStar(player, targetLoc, settings);
                  ++this.starsSpawned;
               }

               Location riftCenter = origin.clone().add(0.0D, 35.0D, 0.0D);

               for(int i = 0; i < 10; ++i) {
                  angle = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                  double dist = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * radius * 0.6D;
                  ParticleUtils.electricSpark(riftCenter.clone().add(Math.cos(angle) * dist, (RagnarokTwilightOfGodsAbility.this.random.nextDouble() - 0.5D) * 4.0D, Math.sin(angle) * dist), 1, 0.2D, 0.03D);
               }

               if (this.tick % 20 == 0) {
                  ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5F, 0.5F + RagnarokTwilightOfGodsAbility.this.random.nextFloat() * 0.3F);
               }

               ++this.tick;
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spawnFallenStar(final Player player, final Location target, final IvoryConfig.AbilitySettings settings) {
      final World world = target.getWorld();
      if (world != null) {
         final Location startLoc = target.clone().add((this.random.nextDouble() - 0.5D) * 10.0D, 40.0D + this.random.nextDouble() * 10.0D, (this.random.nextDouble() - 0.5D) * 10.0D);
         ParticleUtils.playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 0.5F);
         final BlockDisplay star = (BlockDisplay)world.spawn(startLoc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.GLOWSTONE.createBlockData());
            d.setGlowing(true);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            d.setTransformation(new Transformation(new Vector3f(-0.4F, -0.4F, -0.4F), t.getLeftRotation(), new Vector3f(0.8F, 0.8F, 0.8F), t.getRightRotation()));
         });
         (new BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            Vector velocity = target.toVector().subtract(startLoc.toVector()).normalize().multiply(2.5D);
            int tick = 0;

            public void run() {
               double groundY = (double)(world.getHighestBlockYAt(target) + 1);
               if (!(this.currentLoc.getY() <= groundY) && this.tick < 40) {
                  this.currentLoc.add(this.velocity);
                  this.velocity.multiply(1.05D);
                  if (star.isValid()) {
                     star.teleport(this.currentLoc);
                  }

                  ParticleUtils.soulFlame(this.currentLoc, 10, 0.5D, 0.03D);
                  ParticleUtils.endRod(this.currentLoc, 6, 0.3D, 0.02D);
                  Location tailLoc = this.currentLoc.clone();

                  for(int i = 0; i < 6; ++i) {
                     tailLoc.subtract(this.velocity.clone().normalize().multiply(0.8D));
                     ParticleUtils.soulFlame(tailLoc, 3, 0.3D, 0.02D);
                  }

                  ++this.tick;
               } else {
                  if (star.isValid()) {
                     star.remove();
                  }

                  RagnarokTwilightOfGodsAbility.this.createStarImpact(player, target, settings);
                  this.cancel();
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void createStarImpact(Player player, Location loc, IvoryConfig.AbilitySettings settings) {
      World world = loc.getWorld();
      if (world != null) {
         final Location groundLoc = loc.clone();
         groundLoc.setY((double)world.getHighestBlockYAt(loc) + 0.5D);
         ParticleUtils.playSound(groundLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.8F);
         ParticleUtils.flash(groundLoc);
         ParticleUtils.soulFlame(groundLoc, 40, 2.0D, 0.2D);
         ParticleUtils.endRod(groundLoc, 25, 1.5D, 0.25D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 8) {
                  this.cancel();
               } else {
                  ParticleUtils.ring(groundLoc, Particle.SOUL_FIRE_FLAME, (double)this.tick * 0.5D, this.tick * 5 + 5);
                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         DamageZone zone = DamageZone.sphere(player, groundLoc, 3.0D, settings.damagePerTick, settings.damageTickInterval, 15);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }

   private void shatterReality(Player player, final Location origin, final double radius, IvoryConfig.AbilitySettings settings, List<BlockDisplay> displays) {
      World world = origin.getWorld();
      if (world != null) {
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.3F);
         ParticleUtils.playSound(origin, Sound.ENTITY_WITHER_DEATH, 2.5F, 0.4F);
         ParticleUtils.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 3.0F, 0.2F);
         ParticleUtils.playSound(origin, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 3.0F, 0.3F);

         for(int i = 0; i < 10; ++i) {
            double x = (this.random.nextDouble() - 0.5D) * radius;
            double y = this.random.nextDouble() * 20.0D;
            double z = (this.random.nextDouble() - 0.5D) * radius;
            ParticleUtils.flash(origin.clone().add(x, y, z));
         }

         final List<BlockDisplay> crackDisplays = new ArrayList();

         for(int i = 0; i < 8; ++i) {
            double angle = 0.7853981633974483D * (double)i;
            Location crackStart = origin.clone().add(Math.cos(angle) * 2.0D, 5.0D, Math.sin(angle) * 2.0D);
            BlockDisplay crack = (BlockDisplay)world.spawn(crackStart, BlockDisplay.class, (d) -> {
               d.setBlock(Material.CRYING_OBSIDIAN.createBlockData());
               d.setGlowing(true);
               d.setBrightness(new Brightness(15, 15));
               Transformation t = d.getTransformation();
               d.setTransformation(new Transformation(new Vector3f(-0.1F, -2.0F, -0.1F), (new Quaternionf()).rotateY((float)angle), new Vector3f(0.2F, 4.0F, 0.2F), t.getRightRotation()));
            });
            crackDisplays.add(crack);
            displays.add(crack);
         }

         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 20) {
                  this.cancel();
               } else {
                  double expansion = (double)this.tick * 1.5D;

                  for(int i = 0; i < crackDisplays.size(); ++i) {
                     BlockDisplay crack = (BlockDisplay)crackDisplays.get(i);
                     if (crack.isValid()) {
                        double angle = 0.7853981633974483D * (double)i;
                        Location newLoc = origin.clone().add(Math.cos(angle) * (2.0D + expansion), 5.0D, Math.sin(angle) * (2.0D + expansion));
                        crack.teleport(newLoc);
                        ParticleUtils.electricSpark(newLoc, 5, 0.5D, 0.05D);
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         ParticleUtils.soulFlame(origin, 500, radius, 0.6D);
         ParticleUtils.endRod(origin.clone().add(0.0D, 5.0D, 0.0D), 400, radius * 0.8D, 0.7D);
         ParticleUtils.firework(origin.clone().add(0.0D, 10.0D, 0.0D), 300, radius * 0.6D, 0.8D);
         ParticleUtils.electricSpark(origin, 250, radius * 1.2D, 0.5D);
         ParticleUtils.glow(origin.clone().add(0.0D, 3.0D, 0.0D), 200, radius * 0.5D, 0.6D);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 50) {
                  this.cancel();
               } else {
                  double shockRadius = (double)(this.tick * 2);
                  ParticleUtils.ring(origin, Particle.SOUL_FIRE_FLAME, shockRadius, (int)(shockRadius * 15.0D) + 50);
                  ParticleUtils.ring(origin.clone().add(0.0D, 1.0D, 0.0D), Particle.END_ROD, shockRadius * 0.95D, (int)(shockRadius * 10.0D) + 30);
                  int i;
                  double phi;
                  double theta;
                  double x;
                  if (this.tick % 3 == 0) {
                     for(i = 0; i < 8; ++i) {
                        phi = 0.7853981633974483D * (double)i + (double)this.tick * 0.1D;
                        theta = Math.cos(phi) * shockRadius;
                        x = Math.sin(phi) * shockRadius;
                        Location pillarBase = origin.clone().add(theta, 0.0D, x);

                        for(double y = 0.0D; y < 15.0D; y += 0.5D) {
                           ParticleUtils.soulFlame(pillarBase.clone().add(0.0D, y, 0.0D), 1, 0.3D, 0.05D);
                        }
                     }
                  }

                  if (this.tick < 30) {
                     for(i = 0; i < 100; ++i) {
                        phi = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * 3.141592653589793D;
                        theta = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                        x = shockRadius * 0.8D * Math.sin(phi) * Math.cos(theta);
                        double yx = shockRadius * 0.4D * Math.cos(phi) + 5.0D;
                        double z = shockRadius * 0.8D * Math.sin(phi) * Math.sin(theta);
                        ParticleUtils.endRod(origin.clone().add(x, yx, z), 1, 0.3D, 0.0D);
                     }
                  }

                  if (this.tick % 10 == 0) {
                     ParticleUtils.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 0.4F + (float)this.tick * 0.01F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick >= 60) {
                  this.cancel();
               } else {
                  double intensity = 1.0D - (double)this.tick / 60.0D;

                  int i;
                  double angle;
                  double dist;
                  for(i = 0; i < (int)(40.0D * intensity); ++i) {
                     angle = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                     dist = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * radius;
                     double y = RagnarokTwilightOfGodsAbility.this.random.nextDouble() * 20.0D;
                     Location voidPoint = origin.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                     ParticleUtils.electricSpark(voidPoint, 1, 0.3D, 0.03D);
                  }

                  if (this.tick % 5 == 0) {
                     for(i = 0; i < 6; ++i) {
                        angle = 1.0471975511965976D * (double)i;

                        for(dist = 0.0D; dist < radius * 0.8D * intensity; ++dist) {
                           Location scarPoint = origin.clone().add(Math.cos(angle) * dist, 0.1D, Math.sin(angle) * dist);
                           ParticleUtils.endRod(scarPoint, 1, 0.2D, 0.0D);
                        }
                     }
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 30L, 2L);
         DamageZone zone = DamageZone.cylinder(player, origin, radius * 2.0D, 20.0D, settings.damagePerTick * 5.0D, settings.damageTickInterval, 120);
         DamageZoneManager.getInstance(this.plugin).register(zone);
      }
   }
}
