package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D Environmental -- GROUP 3: BRIMSTONE SCAR EVENTS
 * Attacks #21-30: Dweller's brimstone legacy -- lava fountains, shockwaves,
 * heat zones, scar ignitions, soul fire storms, and void fracture mechanics.
 *
 * Design notes:
 * - No status effects
 * - Brimstone palette: crimson (200,0,50), orange glow (255,100,0), deep purple (60,0,100)
 * - Materials: NETHERRACK, MAGMA_BLOCK, OBSIDIAN, CRYING_OBSIDIAN, POLISHED_BLACKSTONE
 * - Damage range: 6.0-14.0 HP
 */
public final class BrimstoneScars {

    private BrimstoneScars() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new LavaFountainRing(plugin));
        registry.register(new BrimstoneShockwave(plugin));
        registry.register(new HeatSurgeZone(plugin));
        registry.register(new DwellerScarIgnition(plugin));
        registry.register(new BrimstoneCreep(plugin));
        registry.register(new SoulFireStorm(plugin));
        registry.register(new VoidFractureReactivation(plugin));
        registry.register(new GravityInversionPocket(plugin));
        registry.register(new DimensionalTearGroundRupture(plugin));
        registry.register(new VoidPullVortex(plugin));
    }

    // =========================================================================
    // 21. LAVA FOUNTAIN RING -- 12 fountain vents erupt in a ring
    // =========================================================================
    public static class LavaFountainRing extends EnvironmentalAttack {

        private final List<Location> ventPositions = new ArrayList<>();
        private final List<BlockDisplayHandle> ventDisplays = new ArrayList<>();
        private boolean fountainsActive = false;

        public LavaFountainRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_fountain_ring", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 vent points at 18-block radius
            for (int i = 0; i < 12; i++) {
                double a = (2 * Math.PI * i) / 12;
                ventPositions.add(center.clone().add(Math.cos(a) * 18, 0, Math.sin(a) * 18));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- vents heat up
            if (ticksAlive <= 80) {
                double ventHeight = Math.min(ticksAlive * 0.05, 4);
                if (ticksAlive % 5 == 0) {
                    for (Location vent : ventPositions) {
                        w.spawnParticle(Particle.LAVA, vent.clone().add(0, ventHeight / 2, 0),
                                3, 0.2, ventHeight / 2, 0.2, 0);
                        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, vent.clone().add(0, ventHeight, 0),
                                4, 0.3, 0.5, 0.3, 0.01);
                    }
                }
                if (ticksAlive == 70) {
                    // Flash burst warning
                    for (Location vent : ventPositions) {
                        w.spawnParticle(Particle.LAVA, vent, 10, 0.3, 0.5, 0.3, 0);
                        w.spawnParticle(Particle.FLAME, vent, 10, 0.3, 0.5, 0.3, 0.05);
                    }
                }
                return;
            }

            // Full eruption
            if (!fountainsActive) {
                fountainsActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.4f);

                for (Location vent : ventPositions) {
                    DisplayBuilder.playSound(vent, Sound.ENTITY_GHAST_SHOOT, 0.7f, 0.5f);

                    // Vent base displays
                    BlockDisplayHandle base = displayBuilder.spawnBlock(vent, Material.NETHERRACK);
                    base.scale(1.5f, 0.5f, 1.5f).glow(200, 0, 50).interpolation(3, 0);
                    ventDisplays.add(base);
                    spawnedEntities.add(base.entity());

                    BlockDisplayHandle magma = displayBuilder.spawnBlock(
                            vent.clone().add(0, 0.5, 0), Material.MAGMA_BLOCK);
                    magma.scale(2.0f, 0.5f, 2.0f).glow(255, 100, 0).interpolation(3, 0);
                    spawnedEntities.add(magma.entity());
                }
            }

            int fountainTick = ticksAlive - 80;

            // Fountains active: 100 ticks (5 seconds)
            if (fountainTick <= 100) {
                if (fountainTick % 2 == 0) {
                    for (Location vent : ventPositions) {
                        // Full height fountain particles
                        for (int y = 0; y <= 16; y += 3) {
                            Location colLoc = vent.clone().add(0, y, 0);
                            w.spawnParticle(Particle.LAVA, colLoc, 4, 0.3, 0.5, 0.3, 0);
                            w.spawnParticle(Particle.FLAME, colLoc, 3, 0.3, 0.5, 0.3, 0.02);
                            w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, colLoc, 2, 0.3, 0.5, 0.3, 0.01);
                        }
                        // Crown at Y+16
                        w.spawnParticle(Particle.LAVA, vent.clone().add(0, 16, 0), 6, 1.5, 0.5, 1.5, 0);
                    }
                }

                // Ambient loop
                if (fountainTick % 40 == 0) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_NETHER_WASTES_ADDITIONS, 0.7f, 0.6f);
                }

                // Damage players in columns
                if (fountainTick % 20 == 0) {
                    for (Location vent : ventPositions) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - vent.getX();
                            double dz = p.getLocation().getZ() - vent.getZ();
                            if (dx * dx + dz * dz <= 2.25 && p.getLocation().getY() <= vent.getY() + 16) {
                                p.damage(10.0);
                            }
                        }
                    }
                }
            }

            // Subside one at a time: 40 ticks
            if (fountainTick > 100 && fountainTick <= 140) {
                int ventIndex = (fountainTick - 100) / 4;
                if (ventIndex < ventPositions.size() && (fountainTick - 100) % 4 == 0) {
                    DisplayBuilder.playSound(ventPositions.get(ventIndex),
                            Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaFountainRing(plugin); }
    }

    // =========================================================================
    // 22. BRIMSTONE SHOCKWAVE -- radial ground wave from center
    // =========================================================================
    public static class BrimstoneShockwave extends EnvironmentalAttack {

        private double waveRadius = 0;
        private boolean waveFired = false;
        private boolean waveReflecting = false;
        private double reflectRadius = 25;

        public BrimstoneShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_shockwave", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Lava streams from all cracks
            for (int i = 0; i < 12; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 3 + Math.random() * 18;
                Location crack = center.clone().add(Math.cos(a) * d, 0.1, Math.sin(a) * d);
                w.spawnParticle(Particle.LAVA, crack, 2, 0.1, 0.3, 0.1, 0);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 50 ticks -- floor vibrates, lava streams rise
            if (ticksAlive <= 50) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        Location crack = center.clone().add(Math.cos(a) * d, 0.1, Math.sin(a) * d);
                        w.spawnParticle(Particle.LAVA, crack, 1, 0.1, 0.2, 0.1, 0);
                    }
                }
                if (ticksAlive == 45) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 1.0f, 0.3f);
                }
                return;
            }

            // Fire wave
            if (!waveFired) {
                waveFired = true;
                waveRadius = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
            }

            // Outward wave: 5 blocks/tick
            if (!waveReflecting) {
                waveRadius += 5;
                if (waveRadius >= 25) {
                    waveReflecting = true;
                    reflectRadius = 25;
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_FALL, 0.9f, 0.4f);
                }

                // Wave ring particles
                if (ticksAlive % 1 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), waveRadius,
                            Particle.LAVA, (int) (waveRadius * 2), null);
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), waveRadius,
                            Particle.FLAME, (int) (waveRadius * 1.5), null);
                    DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), waveRadius,
                            Particle.DUST, (int) waveRadius,
                            new Particle.DustOptions(Color.fromRGB(255, 100, 0), 2.5f));
                }

                // Damage: 10 HP + knockback outward
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (Math.abs(dist - waveRadius) <= 3.0 && p.getLocation().getY() <= center.getY() + 2.5) {
                        p.damage(10.0);
                        double angle = Math.atan2(p.getLocation().getZ() - center.getZ(),
                                p.getLocation().getX() - center.getX());
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(Math.cos(angle) * 0.5, 0.2, Math.sin(angle) * 0.5)));
                    }
                }
            } else {
                // Reflected wave inward at 60% speed
                reflectRadius -= 3;
                if (reflectRadius <= 0) return;

                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), reflectRadius,
                        Particle.LAVA, (int) (reflectRadius), null);
                DisplayBuilder.particleRing(center.clone().add(0, 1, 0), reflectRadius,
                        Particle.DUST, (int) (reflectRadius * 0.8),
                        new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.5f));

                // Reflected damage: 6 HP + knockback inward
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (Math.abs(dist - reflectRadius) <= 3.0 && p.getLocation().getY() <= center.getY() + 2.5) {
                        p.damage(6.0);
                        double angle = Math.atan2(center.getZ() - p.getLocation().getZ(),
                                center.getX() - p.getLocation().getX());
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(Math.cos(angle) * 0.4, 0.15, Math.sin(angle) * 0.4)));
                    }
                }

                if (reflectRadius <= 1) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 0.7f, 0.5f);
                }
            }

            // Trailing smoke
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0, 0.5, 0),
                        8, 10, 0.5, 10, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneShockwave(plugin); }
    }

    // =========================================================================
    // 23. HEAT SURGE ZONE -- invisible heat zone with proximity-based damage
    // =========================================================================
    public static class HeatSurgeZone extends EnvironmentalAttack {

        private Location zoneCenter;
        private boolean zoneActive = false;

        public HeatSurgeZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heat_surge_zone", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 3 + Math.random() * 15;
            zoneCenter = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);

            // Tiny center marker
            BlockDisplayHandle marker = displayBuilder.spawnBlock(zoneCenter.clone().add(0, 0.1, 0),
                    Material.MAGMA_BLOCK);
            marker.scale(0.3f, 0.1f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(marker.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 25 ticks -- smoke builds
            if (ticksAlive <= 25) {
                if (ticksAlive % 3 == 0) {
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, zoneCenter.clone().add(0, 0.5, 0),
                            ticksAlive / 3, 3.0, 0.3, 3.0, 0.01);
                }
                return;
            }

            // Zone active: 60 ticks
            if (!zoneActive) {
                zoneActive = true;
            }

            int zoneTick = ticksAlive - 25;
            if (zoneTick > 60) return;

            // Subtle particles
            if (zoneTick % 3 == 0) {
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, zoneCenter.clone().add(0, 0.5, 0),
                        8, 3.0, 0.5, 3.0, 0.01);
                w.spawnParticle(Particle.FLAME, zoneCenter.clone().add(0, 0.5, 0),
                        1, 2.0, 0.3, 2.0, 0.01);
                DisplayBuilder.dustParticles(zoneCenter.clone().add(0, 0.5, 0), 3, 2.0, 255, 120, 30, 0.8f);

                // Faint perimeter marker
                DisplayBuilder.particleRing(zoneCenter.clone().add(0, 0.5, 0), 6.0,
                        Particle.LAVA, 2, null);
            }

            // Fire ambient
            if (zoneTick % 80 == 0) {
                DisplayBuilder.playSound(zoneCenter, Sound.BLOCK_FIRE_AMBIENT, 0.2f, 0.8f);
            }

            // Proximity-based damage
            if (zoneTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - zoneCenter.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - zoneCenter.getZ(), 2));
                    if (dist <= 1.0) {
                        p.damage(8.0); // 4 hearts/sec center
                    } else if (dist <= 3.0) {
                        p.damage(4.0); // 2 hearts/sec mid
                    } else if (dist <= 6.0) {
                        p.damage(2.0); // 1 heart/sec edge
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HeatSurgeZone(plugin); }
    }

    // =========================================================================
    // 24. DWELLER SCAR IGNITION -- humanoid brimstone silhouette ignites
    // =========================================================================
    public static class DwellerScarIgnition extends EnvironmentalAttack {

        private Location silhouetteCenter;
        private final List<Location> silhouettePoints = new ArrayList<>();
        private final Location[] extremities = new Location[4]; // hands + feet
        private boolean ignited = false;

        public DwellerScarIgnition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_scar_ignition", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double a = Math.random() * 2 * Math.PI;
            double d = 5 + Math.random() * 10;
            silhouetteCenter = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);

            // Generate humanoid silhouette footprint (rough T-shape)
            for (int x = -1; x <= 1; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(z) <= 1 || Math.abs(x) <= 0) { // T-shape body
                        silhouettePoints.add(silhouetteCenter.clone().add(x, 0, z));
                    }
                }
            }
            // Arms (wider at z=2)
            silhouettePoints.add(silhouetteCenter.clone().add(-2, 0, 2));
            silhouettePoints.add(silhouetteCenter.clone().add(2, 0, 2));

            // Extremities: hands and feet
            extremities[0] = silhouetteCenter.clone().add(-2, 0, 2); // left hand
            extremities[1] = silhouetteCenter.clone().add(2, 0, 2);  // right hand
            extremities[2] = silhouetteCenter.clone().add(0, 0, -3); // left foot
            extremities[3] = silhouetteCenter.clone().add(0, 0, 3);  // right foot
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- silhouette glows
            if (ticksAlive <= 60) {
                if (ticksAlive % 5 == 0) {
                    for (Location pt : silhouettePoints) {
                        w.spawnParticle(Particle.LAVA, pt.clone().add(0, 0.3, 0), 1, 0.1, 0.2, 0.1, 0);
                    }
                }
                if (ticksAlive >= 30 && ticksAlive % 3 == 0) {
                    for (Location pt : silhouettePoints) {
                        w.spawnParticle(Particle.FLAME, pt.clone().add(0, 0.3, 0), 2, 0.2, 0.3, 0.2, 0.01);
                    }
                }
                if (ticksAlive == 50) {
                    // Extremity crit bursts
                    for (Location ext : extremities) {
                        if (ext == null) continue;
                        w.spawnParticle(Particle.CRIT, ext.clone().add(0, 0.5, 0), 15, 0.3, 0.5, 0.3, 0.1);
                    }
                }
                return;
            }

            // Ignition
            if (!ignited) {
                ignited = true;
                DisplayBuilder.playSound(silhouetteCenter, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.4f);
            }

            int igniteTick = ticksAlive - 60;

            // Active: 80 ticks
            if (igniteTick <= 80) {
                // Silhouette fire
                if (igniteTick % 2 == 0) {
                    for (Location pt : silhouettePoints) {
                        w.spawnParticle(Particle.LAVA, pt.clone().add(0, 0.5, 0), 3, 0.2, 0.5, 0.2, 0);
                        w.spawnParticle(Particle.FLAME, pt.clone().add(0, 0.5, 0), 3, 0.2, 0.5, 0.2, 0.02);
                        DisplayBuilder.dustParticles(pt, 2, 0.3, 255, 60, 0, 2.0f);
                    }
                }

                // Eruption columns from extremities
                if (igniteTick % 3 == 0) {
                    for (Location ext : extremities) {
                        if (ext == null) continue;
                        for (int y = 0; y <= 10; y += 2) {
                            w.spawnParticle(Particle.LAVA, ext.clone().add(0, y, 0), 3, 0.3, 0.5, 0.3, 0);
                            w.spawnParticle(Particle.FLAME, ext.clone().add(0, y, 0), 3, 0.3, 0.5, 0.3, 0.02);
                        }
                        // Lateral spread at column tops
                        w.spawnParticle(Particle.LAVA, ext.clone().add(0, 10, 0), 5, 2.5, 0.5, 2.5, 0);
                    }
                }

                // Fire ambient
                if (igniteTick % 40 == 0) {
                    DisplayBuilder.playSound(silhouetteCenter, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.3f);
                }

                // Silhouette zone damage: 8 HP per second
                if (igniteTick % 20 == 0) {
                    for (Location pt : silhouettePoints) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(pt) <= 2.25) {
                                p.damage(8.0);
                            }
                        }
                    }
                    // Eruption column damage: 12 HP
                    for (Location ext : extremities) {
                        if (ext == null) continue;
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - ext.getX();
                            double dz = p.getLocation().getZ() - ext.getZ();
                            if (dx * dx + dz * dz <= 2.25 && p.getLocation().getY() <= ext.getY() + 10) {
                                p.damage(12.0);
                                p.setVelocity(p.getVelocity().setY(0.6));
                            }
                        }
                    }
                }
            }

            // Extinguish
            if (igniteTick == 81) {
                DisplayBuilder.playSound(silhouetteCenter, Sound.BLOCK_LAVA_EXTINGUISH, 0.9f, 0.5f);
                // Smoke settling
                for (Location pt : silhouettePoints) {
                    w.spawnParticle(Particle.SMOKE, pt.clone().add(0, 1, 0), 5, 0.3, 1.0, 0.3, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellerScarIgnition(plugin); }
    }

    // =========================================================================
    // 25. BRIMSTONE CREEP -- passive brimstone tile spread along veins
    // =========================================================================
    public static class BrimstoneCreep extends EnvironmentalAttack {

        public BrimstoneCreep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase4_brimstone_creep", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find position along existing brimstone boundary
            double a = Math.random() * 2 * Math.PI;
            double d = 5 + Math.random() * 18;
            Location spreadLoc = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);

            // Brief lava flare warning
            w.spawnParticle(Particle.LAVA, spreadLoc.clone().add(0, 0.3, 0), 5, 0.2, 0.2, 0.2, 0);

            // Spawn new tile
            Material mat = Math.random() < 0.7 ? Material.NETHERRACK : Material.MAGMA_BLOCK;
            BlockDisplayHandle tile = displayBuilder.spawnBlock(spreadLoc.clone().add(0, 0.01, 0), mat);
            tile.scale(0.1f, 0.1f, 0.1f).glow(200, 0, 50).interpolation(20, 0);
            spawnedEntities.add(tile.entity());

            DisplayBuilder.playSound(spreadLoc, Sound.BLOCK_NETHERRACK_PLACE, 0.5f, 0.7f);

            // Animate growth
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                tile.scale(1.0f, 0.15f, 1.0f);
                tile.interpolation(15, 0);
            }, 3L);
        }

        @Override
        protected void onTick(int ticksAlive) {
            // Passive ambient lava particles
            if (ticksAlive == 20) {
                Location center = getCenter();
                if (center != null && center.getWorld() != null) {
                    center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(0, 0.3, 0),
                            2, 0.3, 0.1, 0.3, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { /* Tiles persist -- do not removeAll */ }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCreep(plugin); }
    }

    // =========================================================================
    // 26. SOUL FIRE STORM -- arena-wide soul fire rain + 6 targeted strikes
    // =========================================================================
    public static class SoulFireStorm extends EnvironmentalAttack {

        private boolean stormActive = false;
        private int stormTick = 0;
        private int strikesRemaining = 6;

        public SoulFireStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_storm", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Triple soul fire ambient density
            for (int i = 0; i < 15; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = Math.random() * 20;
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(Math.cos(a) * d, 1, Math.sin(a) * d),
                        5, 0.5, 0.5, 0.5, 0.01);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 50 ticks -- soul fire columns at all deposits + sky darkens
            if (ticksAlive <= 50) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = 5 + Math.random() * 12;
                        Location col = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, col, 10, 0.3, 4, 0.3, 0.01);
                    }
                }
                if (ticksAlive == 35) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);
                }
                if (ticksAlive == 45) {
                    // Sky smoke cloud
                    w.spawnParticle(Particle.SMOKE, center.clone().add(0, 40, 0), 80, 15, 3, 15, 0.02);
                }
                return;
            }

            // Storm active: 120 ticks
            if (!stormActive) {
                stormActive = true;
                stormTick = 0;
                DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, 1.0f, 0.6f);
            }

            stormTick++;
            if (stormTick > 120) return;

            // Dense soul fire rain across arena
            if (stormTick % 2 == 0) {
                for (int i = 0; i < 15; i++) {
                    double rx = (Math.random() - 0.5) * 40;
                    double rz = (Math.random() - 0.5) * 40;
                    double ry = 20 + Math.random() * 30;
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(rx, ry, rz),
                            2, 0.5, 1.0, 0.5, 0.01);
                }
            }

            // Targeted strikes: one every 20 ticks
            if (stormTick % 20 == 0 && strikesRemaining > 0) {
                strikesRemaining--;

                // Pick random player
                List<Player> players = new ArrayList<>(w.getPlayers());
                players.removeIf(this::isExempt);
                if (!players.isEmpty()) {
                    Player target = players.get((int) (Math.random() * players.size()));
                    Location strikePos = target.getLocation().clone();

                    DisplayBuilder.playSound(strikePos, Sound.ENTITY_WITHER_SKELETON_AMBIENT, 0.5f, 0.7f);

                    // Strike column descends
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        for (int y = 0; y <= 50; y += 3) {
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME, strikePos.clone().add(0, y, 0),
                                    15, 0.3, 0.5, 0.3, 0.01);
                            w.spawnParticle(Particle.SCULK_SOUL, strikePos.clone().add(0, y, 0),
                                    5, 0.2, 0.3, 0.2, 0.01);
                            DisplayBuilder.dustParticles(strikePos.clone().add(0, y, 0), 4, 0.3,
                                    100, 200, 255, 1.5f);
                        }

                        // Ground impact
                        w.spawnParticle(Particle.EXPLOSION, strikePos, 8, 1.0, 0.5, 1.0, 0);
                        w.spawnParticle(Particle.SCULK_SOUL, strikePos, 15, 1.0, 0.5, 1.0, 0.05);
                        DisplayBuilder.playSound(strikePos, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.5f);

                        // Damage: 14 HP direct, 6 HP splash
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dist = p.getLocation().distanceSquared(strikePos);
                            if (dist <= 1.0) {
                                p.damage(14.0);
                            } else if (dist <= 4.0) {
                                p.damage(6.0);
                            }
                        }
                    }, 6L);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFireStorm(plugin); }
    }

    // =========================================================================
    // 27. VOID FRACTURE REACTIVATION -- dimensional tear opens in floor channel
    // =========================================================================
    public static class VoidFractureReactivation extends EnvironmentalAttack {

        private Location fractureStart;
        private double fractureAngle;
        private boolean fractureActive = false;

        public VoidFractureReactivation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_fracture_reactivation", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(6.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            fractureAngle = (int) (Math.random() * 4) * (Math.PI / 2); // Cardinal direction
            fractureStart = center.clone();

            // Reverse portal particles from fracture
            for (int i = -8; i <= 8; i++) {
                Location pt = center.clone().add(Math.cos(fractureAngle) * i, 0.2, Math.sin(fractureAngle) * i);
                w.spawnParticle(Particle.REVERSE_PORTAL, pt, 3, 0.1, 0.3, 0.1, 0.01);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- fracture glows, widens
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    for (int i = -8; i <= 8; i++) {
                        Location pt = fractureStart.clone().add(
                                Math.cos(fractureAngle) * i, 0.2, Math.sin(fractureAngle) * i);
                        w.spawnParticle(Particle.REVERSE_PORTAL, pt, 4, 0.2, 0.5, 0.2, 0.01);
                    }
                }
                if (ticksAlive == 35) {
                    DisplayBuilder.playSound(fractureStart, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.4f);
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, fractureStart.clone().add(0, 1, 0),
                            20, 4, 1, 4, 0);
                }
                return;
            }

            // Fracture active: 60 ticks
            if (!fractureActive) {
                fractureActive = true;
                DisplayBuilder.playSound(fractureStart, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.4f);

                // Spawn glass displays in fracture
                for (int i = -6; i <= 6; i += 2) {
                    Location glassLoc = fractureStart.clone().add(
                            Math.cos(fractureAngle) * i, 3, Math.sin(fractureAngle) * i);
                    BlockDisplayHandle glass = displayBuilder.spawnBlock(glassLoc, Material.MAGENTA_STAINED_GLASS);
                    glass.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(5, 0);
                    float rot = (float) (Math.random() * Math.PI);
                    glass.rotate(rot, 0, 1, 0);
                    spawnedEntities.add(glass.entity());
                }
            }

            int fracTick = ticksAlive - 40;
            if (fracTick > 60) return;

            // Active fracture: particles + gravity pull
            if (fracTick % 2 == 0) {
                for (int i = -8; i <= 8; i++) {
                    Location pt = fractureStart.clone().add(
                            Math.cos(fractureAngle) * i, 0.5, Math.sin(fractureAngle) * i);
                    w.spawnParticle(Particle.REVERSE_PORTAL, pt, 8, 0.3, 1.0, 0.3, 0.01);
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, pt, 2, 0.2, 0, 0.2, 0);
                    DisplayBuilder.dustParticles(pt, 3, 0.5, 50, 0, 80, 2.5f);
                }
            }

            // Pull zone indicator particles
            if (fracTick % 4 == 0) {
                double perpX = -Math.sin(fractureAngle);
                double perpZ = Math.cos(fractureAngle);
                for (int i = -8; i <= 8; i += 3) {
                    for (int side = -1; side <= 1; side += 2) {
                        Location pullPt = fractureStart.clone().add(
                                Math.cos(fractureAngle) * i + perpX * side * 4, 0.3,
                                Math.sin(fractureAngle) * i + perpZ * side * 4);
                        DisplayBuilder.dustParticles(pullPt, 2, 0.3, 30, 0, 50, 0.5f);
                    }
                }
            }

            // Gravitational pull toward fracture
            if (fracTick % 5 == 0) {
                double perpX = -Math.sin(fractureAngle);
                double perpZ = Math.cos(fractureAngle);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    // Distance to fracture line
                    double dx = pl.getX() - fractureStart.getX();
                    double dz = pl.getZ() - fractureStart.getZ();
                    double perpDist = Math.abs(dx * perpX + dz * perpZ);
                    double parDist = Math.abs(dx * Math.cos(fractureAngle) + dz * Math.sin(fractureAngle));

                    if (perpDist <= 8.0 && parDist <= 10.0) {
                        // Pull toward fracture
                        double pullStrength = 0.06;
                        double pullDir = (dx * perpX + dz * perpZ >= 0) ? -1.0 : 1.0;
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(perpX * pullDir * pullStrength, 0,
                                        perpZ * pullDir * pullStrength)));
                    }

                    // Fall into fracture: eject upward
                    if (perpDist <= 1.0 && parDist <= 8.0) {
                        p.damage(6.0);
                        p.setVelocity(p.getVelocity().setY(1.2)); // Eject to Y+10
                        DisplayBuilder.playSound(pl, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.6f);
                    }
                }
            }

            // Ambient void drone
            if (fracTick % 40 == 0) {
                DisplayBuilder.playSound(fractureStart, Sound.AMBIENT_NETHER_WASTES_LOOP, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
            if (fractureStart != null) {
                DisplayBuilder.playSound(fractureStart, Sound.BLOCK_STONE_PLACE, 0.9f, 0.5f);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new VoidFractureReactivation(plugin); }
    }

    // =========================================================================
    // 28. GRAVITY INVERSION POCKET -- spherical zone inverts gravity
    // =========================================================================
    public static class GravityInversionPocket extends EnvironmentalAttack {

        private Location pocketCenter;
        private boolean pocketActive = false;

        public GravityInversionPocket(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_inversion_pocket", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0); // Fall damage only
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 5 + Math.random() * 12;
            pocketCenter = center.clone().add(Math.cos(a) * d, 3, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- spiral inward to pocket
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    w.spawnParticle(Particle.REVERSE_PORTAL, pocketCenter, 10, 3, 3, 3, 0.01);
                }
                if (ticksAlive == 30) {
                    // Sphere boundary outline
                    DisplayBuilder.particleRing(pocketCenter, 5.0, Particle.DUST, 15,
                            new Particle.DustOptions(Color.fromRGB(0, 0, 30), 2.0f));
                }
                return;
            }

            // Pocket activates: 80 ticks
            if (!pocketActive) {
                pocketActive = true;
                DisplayBuilder.playSound(pocketCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 0.3f);

                // Central void shard
                BlockDisplayHandle shard = displayBuilder.spawnBlock(pocketCenter, Material.OBSIDIAN);
                shard.scale(0.2f, 0.2f, 0.2f).glow(0, 0, 30).interpolation(3, 0);
                spawnedEntities.add(shard.entity());
            }

            int pocketTick = ticksAlive - 40;
            if (pocketTick > 80) return;

            // Sphere particles
            if (pocketTick % 2 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL, pocketCenter, 20, 2.5, 2.5, 2.5, 0.01);
                DisplayBuilder.dustParticles(pocketCenter, 10, 2.5, 20, 0, 60, 1.5f);
                w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, pocketCenter, 5, 2.5, 2.5, 2.5, 0);

                // Boundary ring
                DisplayBuilder.particleRing(pocketCenter, 5.0, Particle.DUST, 10,
                        new Particle.DustOptions(Color.fromRGB(20, 0, 60), 1.5f));
            }

            // Ambient enderman loop
            if (pocketTick % 60 == 0) {
                DisplayBuilder.playSound(pocketCenter, Sound.ENTITY_ENDERMAN_AMBIENT, 0.5f, 0.4f);
            }

            // Gravity inversion: pull players upward
            if (pocketTick % 3 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(pocketCenter) <= 25.0) { // 5 block radius
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(0, 0.08, 0))); // Upward pull
                    }
                }
            }

            // Deactivation burst
            if (pocketTick == 80) {
                w.spawnParticle(Particle.EXPLOSION, pocketCenter, 5, 1, 1, 1, 0);
                DisplayBuilder.dustParticles(pocketCenter, 20, 2.0, 0, 0, 0, 3.0f);
                DisplayBuilder.playSound(pocketCenter, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityInversionPocket(plugin); }
    }

    // =========================================================================
    // 29. DIMENSIONAL TEAR GROUND RUPTURE -- floor cracks open, void gap
    // =========================================================================
    public static class DimensionalTearGroundRupture extends EnvironmentalAttack {

        private Location crackCenter;
        private double crackAngle;
        private boolean crackOpen = false;

        public DimensionalTearGroundRupture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_tear_ground_rupture", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(8.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            crackAngle = Math.random() * Math.PI;
            double a = Math.random() * 2 * Math.PI;
            double d = 3 + Math.random() * 12;
            crackCenter = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks -- crack line flickers
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    for (int i = -5; i <= 5; i++) {
                        Location pt = crackCenter.clone().add(
                                Math.cos(crackAngle) * i, 0.1, Math.sin(crackAngle) * i);
                        DisplayBuilder.dustParticles(pt, 2, 0.2, 70, 0, 130, 0.8f);
                    }
                }
                if (ticksAlive >= 20 && ticksAlive % 4 == 0) {
                    for (int i = -5; i <= 5; i++) {
                        Location pt = crackCenter.clone().add(
                                Math.cos(crackAngle) * i, 0.3, Math.sin(crackAngle) * i);
                        w.spawnParticle(Particle.REVERSE_PORTAL, pt, 2, 0.1, 0.3, 0.1, 0.01);
                    }
                }
                return;
            }

            // Crack tears open
            if (!crackOpen) {
                crackOpen = true;
                DisplayBuilder.playSound(crackCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);

                // Void depth displays
                for (int i = -5; i <= 5; i++) {
                    for (int depth = 1; depth <= 3; depth++) {
                        Location depthLoc = crackCenter.clone().add(
                                Math.cos(crackAngle) * i, -depth, Math.sin(crackAngle) * i);
                        BlockDisplayHandle glass = displayBuilder.spawnBlock(depthLoc,
                                Material.MAGENTA_STAINED_GLASS);
                        glass.scale(1.0f, 1.0f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                        spawnedEntities.add(glass.entity());
                    }
                }
            }

            int tearTick = ticksAlive - 40;
            if (tearTick > 60) {
                // Closing
                if (tearTick == 61) {
                    DisplayBuilder.playSound(crackCenter, Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f);
                    for (int i = -5; i <= 5; i++) {
                        Location pt = crackCenter.clone().add(
                                Math.cos(crackAngle) * i, 0.5, Math.sin(crackAngle) * i);
                        w.spawnParticle(Particle.BLOCK, pt, 10, 0.3, 0.5, 0.3, 0,
                                Material.PURPUR_BLOCK.createBlockData());
                    }
                }
                return;
            }

            // Active tear: particles
            if (tearTick % 2 == 0) {
                for (int i = -5; i <= 5; i++) {
                    Location pt = crackCenter.clone().add(
                            Math.cos(crackAngle) * i, 0.5, Math.sin(crackAngle) * i);
                    w.spawnParticle(Particle.REVERSE_PORTAL, pt, 6, 0.3, 1.0, 0.3, 0.01);
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, pt, 2, 0.2, 0, 0.2, 0);
                    DisplayBuilder.dustParticles(pt, 3, 0.5, 120, 0, 200, 2.5f);
                    w.spawnParticle(Particle.SCULK_SOUL, pt, 2, 0.3, 0.5, 0.3, 0.01);
                }
            }

            // Ambient void stare
            if (tearTick % 80 == 0) {
                DisplayBuilder.playSound(crackCenter, Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.3f);
            }

            // Damage: fall into tear + edge exposure
            if (tearTick % 10 == 0) {
                double perpX = -Math.sin(crackAngle);
                double perpZ = Math.cos(crackAngle);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dx = pl.getX() - crackCenter.getX();
                    double dz = pl.getZ() - crackCenter.getZ();
                    double perpDist = Math.abs(dx * perpX + dz * perpZ);
                    double parDist = Math.abs(dx * Math.cos(crackAngle) + dz * Math.sin(crackAngle));

                    if (perpDist <= 0.5 && parDist <= 5.0) {
                        // Fell into tear
                        p.damage(8.0);
                        p.setVelocity(p.getVelocity().setY(1.5)); // Eject to Y+12
                        DisplayBuilder.playSound(pl, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.6f);
                    } else if (perpDist <= 1.5 && parDist <= 5.0) {
                        // Edge exposure
                        p.damage(2.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalTearGroundRupture(plugin); }
    }

    // =========================================================================
    // 30. VOID PULL VORTEX -- 10-block-radius pulling force with collapse zone
    // =========================================================================
    public static class VoidPullVortex extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> orbitDisplays = new ArrayList<>();
        private Location vortexCenter;
        private boolean vortexActive = false;

        public VoidPullVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_pull_vortex", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(6.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(2000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 5 + Math.random() * 10;
            vortexCenter = center.clone().add(Math.cos(a) * d, -1, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- reverse portal drift inward
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = 8 + Math.random() * 12;
                        Location driftPt = vortexCenter.clone().add(Math.cos(a) * d, 1, Math.sin(a) * d);
                        w.spawnParticle(Particle.REVERSE_PORTAL, driftPt, 3, 0.5, 0.5, 0.5, 0.01);
                    }
                }
                if (ticksAlive >= 30 && ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(vortexCenter.clone().add(0, 1, 0), 8, 4.0,
                            30, 0, 60, 1.0f);
                }
                return;
            }

            // Vortex activates: 100 ticks
            if (!vortexActive) {
                vortexActive = true;
                DisplayBuilder.playSound(vortexCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.2f);

                // Inner orbit displays
                for (int i = 0; i < 6; i++) {
                    double a = (2 * Math.PI * i) / 6;
                    Location orbitLoc = vortexCenter.clone().add(Math.cos(a) * 2, 0.5, Math.sin(a) * 2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(orbitLoc, Material.OBSIDIAN);
                    h.scale(0.4f, 0.4f, 0.4f).glow(60, 0, 100).interpolation(3, 0);
                    orbitDisplays.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Outer orbit -- crying obsidian
                for (int i = 0; i < 12; i++) {
                    double a = (2 * Math.PI * i) / 12;
                    Location orbitLoc = vortexCenter.clone().add(Math.cos(a) * 8, 0.3, Math.sin(a) * 8);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(orbitLoc, Material.CRYING_OBSIDIAN);
                    h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                    orbitDisplays.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            int vortexTick = ticksAlive - 60;
            if (vortexTick > 100) return;

            // Vortex particles
            if (vortexTick % 2 == 0) {
                // Tightening spiral from 20 blocks
                for (int i = 0; i < 12; i++) {
                    double spiralAngle = vortexTick * 0.2 + (2 * Math.PI * i) / 12;
                    double spiralRadius = 20 - (vortexTick * 0.15);
                    if (spiralRadius < 2) spiralRadius = 2;
                    Location spiralPt = vortexCenter.clone().add(
                            Math.cos(spiralAngle) * spiralRadius, 1, Math.sin(spiralAngle) * spiralRadius);
                    w.spawnParticle(Particle.REVERSE_PORTAL, spiralPt, 3, 0.3, 0.3, 0.3, 0.01);
                }

                // Collapse zone boundary
                DisplayBuilder.dustParticles(vortexCenter.clone().add(0, 1, 0), 15, 3.0,
                        10, 0, 40, 2.5f);
                w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, vortexCenter.clone().add(0, 2, 0),
                        8, 3.0, 1.0, 3.0, 0);
            }

            // Vacuum drone
            if (vortexTick % 40 == 0) {
                DisplayBuilder.playSound(vortexCenter, Sound.AMBIENT_NETHER_WASTES_LOOP, 0.8f, 0.2f);
            }

            // Pull all players toward vortex
            if (vortexTick % 3 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dist = pl.distance(vortexCenter);
                    if (dist <= 20 && dist > 0.5) {
                        double pullStrength = 0.08;
                        double dx = (vortexCenter.getX() - pl.getX()) / dist * pullStrength;
                        double dz = (vortexCenter.getZ() - pl.getZ()) / dist * pullStrength;
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(dx, 0, dz)));
                    }
                }
            }

            // Collapse zone damage: 6 HP per second
            if (vortexTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(vortexCenter) <= 36.0) { // 6 block radius
                        p.damage(6.0);
                    }
                }
            }

            // Center ejection
            if (vortexTick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(vortexCenter) <= 1.0) {
                        p.setVelocity(p.getVelocity().setY(1.5)); // Eject upward
                        DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);
                    }
                }
            }

            // Orbit display rotation
            if (vortexTick % 3 == 0) {
                double innerAngle = vortexTick * 0.26; // 15 deg/tick
                for (int i = 0; i < Math.min(6, orbitDisplays.size()); i++) {
                    double a = innerAngle + (2 * Math.PI * i) / 6;
                    Location newPos = vortexCenter.clone().add(Math.cos(a) * 2, 0.5, Math.sin(a) * 2);
                    orbitDisplays.get(i).entity().teleport(newPos);
                }
                double outerAngle = vortexTick * 0.09; // 5 deg/tick
                double currentOuterRadius = 8 - (vortexTick * 0.05); // Spiraling inward
                if (currentOuterRadius < 3) currentOuterRadius = 3;
                for (int i = 6; i < orbitDisplays.size(); i++) {
                    double a = outerAngle + (2 * Math.PI * (i - 6)) / 12;
                    Location newPos = vortexCenter.clone().add(
                            Math.cos(a) * currentOuterRadius, 0.3, Math.sin(a) * currentOuterRadius);
                    orbitDisplays.get(i).entity().teleport(newPos);
                }
            }

            // Collapse
            if (vortexTick == 100) {
                DisplayBuilder.playSound(vortexCenter, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 0.6f);

                // Permanent crying obsidian remnants
                for (int i = 0; i < 8; i++) {
                    double a = (2 * Math.PI * i) / 8;
                    Location remnant = vortexCenter.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(remnant, Material.CRYING_OBSIDIAN);
                    h.scale(0.5f, 0.2f, 0.5f).glow(128, 0, 255).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPullVortex(plugin); }
    }
}
