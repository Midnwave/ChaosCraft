package com.blockforge.chaoscraft.weapons.radiant;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.itemtags.ItemTagsAPI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

public final class RadiantCoreService implements Listener {
   private final ChaosCraftPlugin plugin;
   private final Map<UUID, RadiantCoreService.RadiantSession> active = new ConcurrentHashMap();
   private final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap();

   public RadiantCoreService(ChaosCraftPlugin plugin) {
      this.plugin = plugin;
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onLeftClick(PlayerInteractEvent e) {
      if (e.getHand() == EquipmentSlot.HAND) {
         Action a = e.getAction();
         if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
            Player p = e.getPlayer();
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (inHand != null && inHand.getType() != Material.AIR) {
               YamlConfiguration yc = this.plugin.yaml("weapons/radiant_core.yml");
               if (yc != null && yc.getBoolean("enabled", true)) {
                  String requiredTag = yc.getString("required-item-tag", "radiant_core");
                  if (ItemTagsAPI.hasTag(inHand, requiredTag)) {
                     long now = Instant.now().getEpochSecond();
                     long until = (Long)this.cooldownUntil.getOrDefault(p.getUniqueId(), 0L);
                     int cd = Math.max(0, yc.getInt("per-player-cooldown-seconds", 30));
                     if (now < until) {
                        long left = until - now;
                        String var10001 = String.valueOf(ChatColor.RED);
                        p.sendActionBar(Component.text(var10001 + "Radiant Core cooldown: " + left + "s"));
                     } else {
                        inHand.setAmount(inHand.getAmount() - 1);
                        p.getInventory().setItemInMainHand(inHand.getAmount() > 0 ? inHand : null);
                        double radius = Math.max(0.5D, yc.getDouble("radius", 8.0D));
                        double dmgPerTick = Math.max(0.0D, yc.getDouble("damage-per-tick", 0.1D));
                        double healPerTick = Math.max(0.0D, yc.getDouble("heal-per-tick", 0.05D));
                        boolean ignoreArmor = yc.getBoolean("ignore-armor", true);
                        int durationSeconds = Math.max(1, yc.getInt("duration-seconds", 10));
                        String soundKey = yc.getString("activation-sound", "chaoscraft:weapons.radiant_core.activate");
                        boolean splitScaling = yc.getBoolean("split-scaling", true);
                        boolean lightningOnKill = yc.getBoolean("lightning-on-kill", true);
                        boolean extraParticles = yc.getBoolean("extra-particles", true);
                        RadiantCoreService.RadiantSession session = new RadiantCoreService.RadiantSession(p, radius, dmgPerTick, healPerTick, ignoreArmor, durationSeconds, soundKey, splitScaling, lightningOnKill, extraParticles);
                        if (this.active.putIfAbsent(p.getUniqueId(), session) != null) {
                           p.sendActionBar(Component.text(String.valueOf(ChatColor.YELLOW) + "Radiant Core already active."));
                        } else {
                           this.cooldownUntil.put(p.getUniqueId(), now + (long)cd);
                           session.start();
                           if (soundKey != null && !soundKey.isBlank()) {
                              try {
                                 Sound sound = Sound.valueOf(soundKey.toUpperCase(Locale.ROOT));
                                 p.getWorld().playSound(p.getLocation(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
                              } catch (IllegalArgumentException var26) {
                                 p.getWorld().playSound(p.getLocation(), soundKey, SoundCategory.PLAYERS, 1.0F, 1.0F);
                              }
                           }

                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onQuit(PlayerQuitEvent e) {
      this.stopIfActive(e.getPlayer().getUniqueId());
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onDeath(PlayerDeathEvent e) {
      Player p = e.getEntity();
      this.stopIfActive(p.getUniqueId());
   }

   public boolean isActive(UUID playerId) {
      return this.active.containsKey(playerId);
   }

   public Optional<Long> cooldownLeft(UUID playerId) {
      long now = Instant.now().getEpochSecond();
      long until = (Long)this.cooldownUntil.getOrDefault(playerId, 0L);
      return until <= now ? Optional.empty() : Optional.of(until - now);
   }

   public void stopIfActive(UUID playerId) {
      RadiantCoreService.RadiantSession s = (RadiantCoreService.RadiantSession)this.active.remove(playerId);
      if (s != null) {
         s.stop();
      }

   }

   private final class RadiantSession {
      private final Player player;
      private final double radius;
      private final double dmgPerTick;
      private final double healPerTick;
      private final boolean ignoreArmor;
      private final int durationSeconds;
      private final String soundKey;
      private final boolean splitScaling;
      private final boolean lightningOnKill;
      private final boolean extraParticles;
      private final World world;
      private int ticksLived = 0;
      private BukkitRunnable loop;
      private Display haloDisplay;

      RadiantSession(Player player, double radius, double dmgPerTick, double healPerTick, boolean ignoreArmor, int durationSeconds, String soundKey, boolean splitScaling, boolean lightningOnKill, boolean extraParticles) {
         this.player = player;
         this.radius = radius;
         this.dmgPerTick = dmgPerTick;
         this.healPerTick = healPerTick;
         this.ignoreArmor = ignoreArmor;
         this.durationSeconds = durationSeconds;
         this.soundKey = soundKey;
         this.splitScaling = splitScaling;
         this.lightningOnKill = lightningOnKill;
         this.extraParticles = extraParticles;
         this.world = player.getWorld();
      }

      void start() {
         this.spawnHalo();
         this.loop = new BukkitRunnable() {
            public void run() {
               if (RadiantSession.this.player.isOnline() && !RadiantSession.this.player.isDead()) {
                  ++RadiantSession.this.ticksLived;
                  if (RadiantSession.this.ticksLived >= RadiantSession.this.durationSeconds * 20) {
                     RadiantSession.this.stop();
                  } else {
                     RadiantSession.this.updateHalo();
                     List<LivingEntity> hostiles = RadiantSession.this.findHostiles(RadiantSession.this.player.getLocation(), RadiantSession.this.radius);
                     int count = hostiles.size();
                     if (count > 0) {
                        double perMobDamage = RadiantSession.this.dmgPerTick;
                        double healThisTickIncrement = 0.0D;
                        if (RadiantSession.this.splitScaling) {
                           perMobDamage = RadiantSession.this.dmgPerTick / (double)count;
                           healThisTickIncrement = RadiantSession.this.healPerTick / (double)count;
                        }

                        double totalHeal = 0.0D;
                        Iterator var9 = hostiles.iterator();

                        while(var9.hasNext()) {
                           LivingEntity mob = (LivingEntity)var9.next();
                           if (mob.isValid() && !mob.isDead() && !(mob.getLocation().distanceSquared(RadiantSession.this.player.getLocation()) > RadiantSession.this.radius * RadiantSession.this.radius)) {
                              boolean died = RadiantSession.this.applyDamage(mob, perMobDamage);
                              RadiantSession.this.drawBeam(mob.getLocation().add(0.0D, mob.getHeight() * 0.5D, 0.0D), RadiantSession.this.haloLocation(), 0.35D, RadiantSession.this.extraParticles);
                              if (RadiantSession.this.splitScaling) {
                                 totalHeal += healThisTickIncrement;
                              } else {
                                 totalHeal += RadiantSession.this.healPerTick;
                              }

                              if (died && RadiantSession.this.lightningOnKill) {
                                 RadiantSession.this.world.strikeLightningEffect(mob.getLocation());
                              }
                           }
                        }

                        if (totalHeal > 0.0D) {
                           double max = RadiantSession.this.player.getAttribute(Attribute.MAX_HEALTH).getValue();
                           double newHp = Math.min(max, RadiantSession.this.player.getHealth() + totalHeal);
                           RadiantSession.this.player.setHealth(newHp);
                        }
                     }

                     if (RadiantSession.this.ticksLived % 3 == 0) {
                        Location hl = RadiantSession.this.haloLocation();
                        RadiantSession.this.world.spawnParticle(Particle.END_ROD, hl, 18, 0.35D, 0.18D, 0.35D, 0.01D);
                        RadiantSession.this.world.spawnParticle(Particle.GLOW, hl, 6, 0.08D, 0.08D, 0.08D, 0.0D);
                        if (RadiantSession.this.extraParticles) {
                           RadiantSession.this.world.spawnParticle(Particle.ENCHANT, hl, 12, 0.6D, 0.1D, 0.6D, 0.0D);
                        }
                     }

                     if (RadiantSession.this.extraParticles && RadiantSession.this.ticksLived % 10 == 0) {
                        RadiantSession.this.ringBurst(RadiantSession.this.player.getLocation().add(0.0D, 0.2D, 0.0D), 1.6D, 24);
                     }

                  }
               } else {
                  RadiantSession.this.stop();
               }
            }
         };
         this.loop.runTaskTimer(RadiantCoreService.this.plugin, 1L, 1L);
      }

      void stop() {
         if (this.loop != null) {
            this.loop.cancel();
            this.loop = null;
         }

         this.removeHalo();
         RadiantCoreService.this.active.remove(this.player.getUniqueId());
      }

      private boolean applyDamage(LivingEntity mob, double amount) {
         if (amount <= 0.0D) {
            return false;
         } else {
            double newHp;
            if (this.ignoreArmor) {
               newHp = Math.max(0.0D, mob.getHealth() - amount);
               mob.setHealth(newHp);
               if (newHp <= 0.0D) {
                  mob.damage(0.01D, this.player);
                  return true;
               } else {
                  return false;
               }
            } else {
               newHp = mob.getHealth();
               mob.damage(amount, this.player);
               return !mob.isValid() || mob.isDead() || mob.getHealth() <= 0.0D || mob.getHealth() >= newHp && newHp <= 0.0D;
            }
         }
      }

      private void spawnHalo() {
         Location loc = this.player.getLocation().add(0.0D, this.player.getHeight() + 0.8D, 0.0D);
         BlockDisplay bd = (BlockDisplay)this.world.spawn(loc, BlockDisplay.class, (d) -> {
            d.setBlock(Material.LIGHT.createBlockData());
            d.setBillboard(Billboard.CENTER);
            d.setBrightness(new Brightness(15, 15));
            Transformation t = d.getTransformation();
            t.getScale().set(1.6F, 0.1F, 1.6F);
            d.setTransformation(t);
            d.setGlowing(true);
            d.setGlowColorOverride(Color.fromRGB(255, 210, 80));
            d.setShadowRadius(0.0F);
            d.setViewRange(64.0F);
            d.setPersistent(false);
            d.setGravity(false);
            d.setInvulnerable(true);
         });
         this.haloDisplay = bd;
      }

      private void updateHalo() {
         if (this.haloDisplay != null && !this.haloDisplay.isDead()) {
            Location base = this.player.getLocation().add(0.0D, this.player.getHeight() + 0.8D, 0.0D);
            float angle = (float)(this.ticksLived % 360) * 5.0F;
            Transformation t = this.haloDisplay.getTransformation();
            t.getLeftRotation().set(0.0F, (float)Math.toRadians((double)angle), 0.0F, 1.0F);
            this.haloDisplay.setTransformation(t);
            this.haloDisplay.teleport(base);
         } else {
            this.spawnHalo();
         }
      }

      private void removeHalo() {
         if (this.haloDisplay != null && this.haloDisplay.isValid()) {
            this.haloDisplay.remove();
         }

         this.haloDisplay = null;
      }

      private Location haloLocation() {
         return this.haloDisplay != null ? this.haloDisplay.getLocation().clone() : this.player.getLocation().add(0.0D, this.player.getHeight() + 0.8D, 0.0D);
      }

      private void drawBeam(Location from, Location to, double step, boolean richer) {
         Vector dir = to.toVector().subtract(from.toVector());
         double length = dir.length();
         if (!(length <= 0.05D)) {
            dir.normalize().multiply(step);
            int points = (int)Math.ceil(length / step);
            Location cur = from.clone();

            for(int i = 0; i < points; ++i) {
               this.world.spawnParticle(Particle.CRIT, cur, 1, 0.0D, 0.0D, 0.0D, 0.0D);
               this.world.spawnParticle(Particle.END_ROD, cur, 1, 0.0D, 0.0D, 0.0D, 0.0D);
               if (richer && i % 3 == 0) {
                  this.world.spawnParticle(Particle.ENCHANT, cur, 2, 0.02D, 0.02D, 0.02D, 0.0D);
               }

               cur.add(dir);
            }

         }
      }

      private void ringBurst(Location center, double radius, int points) {
         double step = 6.283185307179586D / (double)points;

         for(int i = 0; i < points; ++i) {
            double a = (double)i * step;
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;
            Location spot = center.clone().add(x, 0.0D, z);
            this.world.spawnParticle(Particle.END_ROD, spot, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            this.world.spawnParticle(Particle.CRIT, spot, 1, 0.0D, 0.0D, 0.0D, 0.0D);
         }

      }

      private List<LivingEntity> findHostiles(Location center, double radius) {
         double r2 = radius * radius;
         List<LivingEntity> result = new ArrayList();
         Iterator var7 = center.getWorld().getNearbyEntities(center, radius, radius, radius).iterator();

         while(var7.hasNext()) {
            Entity e = (Entity)var7.next();
            if (e instanceof LivingEntity) {
               LivingEntity le = (LivingEntity)e;
               if (!e.getUniqueId().equals(this.player.getUniqueId()) && this.isHostile(le) && !(e.getLocation().distanceSquared(center) > r2)) {
                  result.add(le);
               }
            }
         }

         return result;
      }

      private boolean isHostile(LivingEntity le) {
         return le instanceof Monster || le.getType() == EntityType.SLIME || le.getType() == EntityType.MAGMA_CUBE || le.getType() == EntityType.PHANTOM || le.getType() == EntityType.SHULKER || le.getType() == EntityType.GUARDIAN || le.getType() == EntityType.ELDER_GUARDIAN || le.getType() == EntityType.PIGLIN || le.getType() == EntityType.HOGLIN || le.getType() == EntityType.ZOGLIN;
      }
   }
}
