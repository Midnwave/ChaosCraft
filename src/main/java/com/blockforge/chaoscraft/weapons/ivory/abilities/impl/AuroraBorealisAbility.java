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
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AuroraBorealisAbility extends Ability {
   private final Random random = new Random();

   public AuroraBorealisAbility(ChaosCraftPlugin plugin, IvoryConfig config, IvoryEffectsManager effectsManager) {
      super(plugin, config, effectsManager);
   }

   public String getId() {
      return "aurora_borealis";
   }

   public String getDisplayName() {
      return "Aurora Borealis";
   }

   public String getDescription() {
      return "Summon magnificent curtains of celestial aurora that sweep across the battlefield with devastating beauty.";
   }

   public void execute(Player player) {
      IvoryConfig.AbilitySettings settings = this.getSettings();
      Location origin = this.getExecutionLocation(player);
      double radius = settings.radius;
      int duration = settings.getInt("duration-ticks", 160);
      int waveCount = settings.getInt("wave-count", 5);
      ParticleUtils.playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 1.5F);
      ParticleUtils.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5F, 0.8F);
      this.createAurora(player, origin, radius, duration, waveCount, settings);
   }

   private void createAurora(Player player, final Location origin, final double radius, final int duration, final int waveCount, IvoryConfig.AbilitySettings settings) {
      World world = origin.getWorld();
      if (world != null) {
         DamageZone zone = DamageZone.cylinder(player, origin, radius, 25.0D, settings.damagePerTick, settings.damageTickInterval, duration);
         DamageZoneManager.getInstance(this.plugin).register(zone);
         (new BukkitRunnable() {
            int tick = 0;
            double[] waveOffsets = new double[waveCount];
            double[] wavePhases = new double[waveCount];
            double[] waveSpeeds = new double[waveCount];

            public void run() {
               if (this.tick >= duration) {
                  this.cancel();
               } else {
                  int i;
                  if (this.tick == 0) {
                     for(i = 0; i < waveCount; ++i) {
                        this.waveOffsets[i] = AuroraBorealisAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                        this.wavePhases[i] = AuroraBorealisAbility.this.random.nextDouble() * 3.141592653589793D * 2.0D;
                        this.waveSpeeds[i] = 0.05D + AuroraBorealisAbility.this.random.nextDouble() * 0.05D;
                     }
                  }

                  for(i = 0; i < waveCount; ++i) {
                     AuroraBorealisAbility.this.drawAuroraCurtain(origin, radius, i, waveCount, this.tick, this.waveOffsets[i], this.wavePhases[i], this.waveSpeeds[i]);
                  }

                  double x;
                  double z;
                  double y;
                  Location sparkleLoc;
                  if (this.tick % 3 == 0) {
                     for(i = 0; i < 50; ++i) {
                        x = 0.12566370614359174D * (double)i;
                        z = Math.cos(x) * radius;
                        y = Math.sin(x) * radius;
                        sparkleLoc = origin.clone().add(z, 0.1D, y);
                        if (AuroraBorealisAbility.this.random.nextDouble() < 0.3D) {
                           ParticleUtils.endRod(sparkleLoc, 1, 0.2D, 0.02D);
                        }
                     }
                  }

                  if (this.tick % 2 == 0) {
                     for(i = 0; i < 10; ++i) {
                        x = (AuroraBorealisAbility.this.random.nextDouble() - 0.5D) * radius * 2.0D;
                        z = (AuroraBorealisAbility.this.random.nextDouble() - 0.5D) * radius * 2.0D;
                        y = 5.0D + AuroraBorealisAbility.this.random.nextDouble() * 15.0D;
                        sparkleLoc = origin.clone().add(x, y, z);
                        ParticleUtils.glow(sparkleLoc, 1, 0.1D, 0.05D);
                     }
                  }

                  if (this.tick % 30 == 0) {
                     ParticleUtils.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.0F + AuroraBorealisAbility.this.random.nextFloat() * 0.3F);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(this.plugin, 0L, 1L);
      }
   }

   private void drawAuroraCurtain(Location origin, double radius, int waveIndex, int totalWaves, int tick, double offset, double phase, double speed) {
      World world = origin.getWorld();
      if (world != null) {
         double baseAngle = 6.283185307179586D / (double)totalWaves * (double)waveIndex + offset;
         double waveMotion = (double)tick * speed;
         int stripCount = 20;

         for(int strip = 0; strip < stripCount; ++strip) {
            double stripAngle = baseAngle + ((double)strip - (double)stripCount / 2.0D) * 0.1D;
            double stripRadius = radius * (0.3D + Math.sin(phase + (double)strip * 0.3D + waveMotion) * 0.3D);
            double x = Math.cos(stripAngle) * stripRadius;
            double z = Math.sin(stripAngle) * stripRadius;
            double baseHeight = 5.0D + Math.sin(phase + (double)strip * 0.5D + waveMotion * 0.5D) * 3.0D;
            double topHeight = baseHeight + 12.0D + Math.sin(phase + (double)strip * 0.3D + waveMotion * 0.8D) * 5.0D;

            for(double y = baseHeight; y < topHeight; y += 0.4D) {
               double waveX = x + Math.sin(y * 0.3D + waveMotion) * 0.8D;
               double waveZ = z + Math.cos(y * 0.3D + waveMotion + phase) * 0.8D;
               Location stripPoint = origin.clone().add(waveX, y, waveZ);
               double heightProgress = (y - baseHeight) / (topHeight - baseHeight);
               if (heightProgress < 0.4D) {
                  if (this.random.nextDouble() < 0.6D) {
                     ParticleUtils.soulFlame(stripPoint, 1, 0.1D, 0.0D);
                  }
               } else if (heightProgress < 0.7D) {
                  if (this.random.nextDouble() < 0.5D) {
                     ParticleUtils.electricSpark(stripPoint, 1, 0.1D, 0.0D);
                  }
               } else if (this.random.nextDouble() < 0.4D) {
                  ParticleUtils.endRod(stripPoint, 1, 0.15D, 0.02D);
               }
            }
         }

         if (tick % 20 == waveIndex * 4) {
            double flashRadius = radius * 0.5D;
            double flashX = Math.cos(baseAngle) * flashRadius;
            double flashZ = Math.sin(baseAngle) * flashRadius;
            Location flashLoc = origin.clone().add(flashX, 10.0D, flashZ);
            ParticleUtils.flash(flashLoc);
            ParticleUtils.glow(flashLoc, 20, 2.0D, 0.1D);
         }

      }
   }
}
