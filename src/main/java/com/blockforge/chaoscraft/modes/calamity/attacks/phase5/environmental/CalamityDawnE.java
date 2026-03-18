package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #41-50
 * DIMENSIONAL INSTABILITY (#41-42) + PHASE 4 SPECIFIC (#43-50)
 *
 * Design: No status effects. Damage 6.0-18.0 HP. AxisAngle4f only.
 */
public final class CalamityDawnE {

    private CalamityDawnE() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SpatialFractureColumns(plugin));
        registry.register(new EndTide(plugin));
        registry.register(new ArenaFloorFracture(plugin));
        registry.register(new VoidCreep(plugin));
        registry.register(new TripleDensitySky(plugin));
        registry.register(new CalamityPulse(plugin));
        registry.register(new FallingSky(plugin));
        registry.register(new ResonanceCascade(plugin));
        registry.register(new TheLastGround(plugin));
        registry.register(new EndOfCalamity(plugin));
    }

    // =========================================================================
    // 41. SPATIAL FRACTURE COLUMNS -- three portal columns that teleport players
    // =========================================================================
    public static class SpatialFractureColumns extends EnvironmentalAttack {

        private final List<Location> columnPositions = new ArrayList<>();
        private boolean imploded = false;

        public SpatialFractureColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spatial_fracture_columns", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 3; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 3 + Math.random() * 6;
                columnPositions.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    for (Location cp : columnPositions) {
                        w.spawnParticle(Particle.PORTAL, cp.clone().add(0, ticksAlive / 60.0 * 5, 0),
                                10, 0.3, 0.3, 0.3, 0.05);
                        w.spawnParticle(Particle.END_ROD, cp.clone().add(0, ticksAlive / 60.0 * 5, 0),
                                5, 0.2, 0.2, 0.2, 0.01);
                    }
                }
                return;
            }

            int activeTick = ticksAlive - 61;
            if (activeTick >= 0 && activeTick <= 160) {
                if (activeTick % 3 == 0) {
                    for (Location cp : columnPositions) {
                        for (double y = 0; y < 15; y += 2.0) {
                            w.spawnParticle(Particle.PORTAL, cp.clone().add(0, y, 0),
                                    5, 0.5, 0.3, 0.5, 0.05);
                            w.spawnParticle(Particle.END_ROD, cp.clone().add(0, y, 0),
                                    3, 0.3, 0.2, 0.3, 0.01);
                        }
                    }
                }

                if (activeTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location cp : columnPositions) {
                            if (p.getLocation().distanceSquared(cp) <= 2.25) {
                                p.damage(6.0);
                                double teleAngle = Math.random() * 2 * Math.PI;
                                double teleDist = 1 + Math.random() * 2;
                                Location teleTarget = p.getLocation().clone().add(
                                        Math.cos(teleAngle) * teleDist, 0, Math.sin(teleAngle) * teleDist);
                                p.teleport(teleTarget);
                                w.spawnParticle(Particle.PORTAL, p.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                                break;
                            }
                        }
                    }
                }

                if (activeTick % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.4f, 0.9f);
                }
            }

            if (activeTick > 160 && !imploded) {
                imploded = true;
                for (Location cp : columnPositions) {
                    triggerImpactDamage(cp);
                    w.spawnParticle(Particle.PORTAL, cp, 80, 2, 2, 2, 0.1);
                    w.spawnParticle(Particle.END_ROD, cp, 80, 2, 2, 2, 0.1);
                    DisplayBuilder.playSound(cp, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.8f, 1.0f);
                    DisplayBuilder.playSound(cp, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.0f);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(cp) <= 16.0) {
                            p.damage(12.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpatialFractureColumns(plugin); }
    }

    // =========================================================================
    // 42. END TIDE -- dimensional ceiling presses down onto arena
    // =========================================================================
    public static class EndTide extends EnvironmentalAttack {

        public EndTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("end_tide", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(2000);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 100) {
                double skyY = center.getY() + 30 - (ticksAlive / 100.0) * 15;
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double px = center.getX() + (Math.random() - 0.5) * 20;
                        double pz = center.getZ() + (Math.random() - 0.5) * 20;
                        w.spawnParticle(Particle.PORTAL, new Location(w, px, skyY, pz),
                                5, 1, 1, 1, 0.05);
                    }
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.4f);
                }
                return;
            }

            if (ticksAlive > 100 && ticksAlive <= 220) {
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 25; i++) {
                        double px = center.getX() + (Math.random() - 0.5) * 20;
                        double py = center.getY() + 10 + Math.random() * 5;
                        double pz = center.getZ() + (Math.random() - 0.5) * 20;
                        w.spawnParticle(Particle.PORTAL, new Location(w, px, py, pz),
                                3, 0.5, 0.5, 0.5, 0.03);
                    }
                }

                if (ticksAlive % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().getY() > center.getY() + 12) {
                            p.damage(6.0);
                        }
                    }
                }
            }

            if (ticksAlive > 220) {
                double riseY = center.getY() + 15 + ((ticksAlive - 220) / 40.0) * 30;
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double px = center.getX() + (Math.random() - 0.5) * 20;
                        double pz = center.getZ() + (Math.random() - 0.5) * 20;
                        w.spawnParticle(Particle.PORTAL, new Location(w, px, riseY, pz),
                                3, 1, 1, 1, 0.05);
                    }
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EndTide(plugin); }
    }

    // =========================================================================
    // 43. ARENA FLOOR FRACTURE -- visual crack pattern expanding across floor
    // =========================================================================
    public static class ArenaFloorFracture extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();

        public ArenaFloorFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arena_floor_fracture", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.3f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 80) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0), 10, 5.0, 80, 80, 80, 0.8f);
                }
                return;
            }

            if (ticksAlive > 80 && ticksAlive <= 480) {
                if ((ticksAlive - 80) % 20 == 0) {
                    int crackIndex = (ticksAlive - 80) / 20;
                    if (crackIndex < 15) {
                        double angle = (2 * Math.PI * crackIndex) / 15;
                        double length = 2 + crackIndex * 0.5;
                        for (double d = 0; d < length; d += 1.0) {
                            double cx = center.getX() + Math.cos(angle) * d;
                            double cz = center.getZ() + Math.sin(angle) * d;
                            Location crackPos = new Location(w, cx, center.getY() + 0.05, cz);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(crackPos, Material.BLACK_CONCRETE);
                            h.scale(0.1f, 0.1f, (float) Math.min(3, length - d))
                                    .glow(40, 20, 0).interpolation(5, 0);
                            h.rotate((float) angle, 0, 1, 0);
                            crackHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                        DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.3f, 0.4f);
                    }
                }

                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double rx = center.getX() + (Math.random() - 0.5) * 14;
                        double rz = center.getZ() + (Math.random() - 0.5) * 14;
                        DisplayBuilder.dustParticles(new Location(w, rx, center.getY() + 0.2, rz),
                                3, 0.3, 80, 40, 0, 0.6f);
                    }
                }

                if (ticksAlive % 100 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_STEP, 0.2f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArenaFloorFracture(plugin); }
    }

    // =========================================================================
    // 44. VOID CREEP -- continuous arena shrinkage from edges
    // =========================================================================
    public static class VoidCreep extends EnvironmentalAttack {

        private double safeRadius = 10.0;
        private final List<BlockDisplayHandle> boundaryHandles = new ArrayList<>();

        public VoidCreep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_creep", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(2400);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            safeRadius = 10.0;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive % 100 == 0 && safeRadius > 4.0) {
                safeRadius -= 0.5;
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 0.5f);
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), safeRadius,
                        Particle.PORTAL, (int) (safeRadius * 6), null);
                DisplayBuilder.particleRing(center.clone().add(0, 2, 0), safeRadius,
                        Particle.DUST, (int) (safeRadius * 4),
                        new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.0f));
                DisplayBuilder.particleRing(center.clone().add(0, 3.5, 0), safeRadius,
                        Particle.PORTAL, (int) (safeRadius * 3), null);
            }

            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = Math.abs(p.getLocation().getX() - center.getX());
                    double dz = Math.abs(p.getLocation().getZ() - center.getZ());
                    if (dx > safeRadius || dz > safeRadius) {
                        p.damage(6.0);
                        org.bukkit.util.Vector push = center.toVector()
                                .subtract(p.getLocation().toVector()).normalize().multiply(1.5);
                        push.setY(0.3);
                        p.setVelocity(push);
                    }
                }
            }

            if (ticksAlive % 10 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = safeRadius + 1 + Math.random() * 3;
                    Location voidPos = center.clone().add(Math.cos(angle) * dist, Math.random() * 3, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.PORTAL, voidPos, 3, 0.3, 0.5, 0.3, 0.05);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCreep(plugin); }
    }

    // =========================================================================
    // 45. TRIPLE DENSITY SKY -- all 5 streams at 3x rate for 30 seconds
    // =========================================================================
    public static class TripleDensitySky extends EnvironmentalAttack {

        public TripleDensitySky(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("triple_density_sky", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(660);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    double density = 1 + (ticksAlive / 60.0) * 2;
                    for (int i = 0; i < (int) (5 * density); i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sy = center.getY() + 20 + Math.random() * 30;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        Location skyPos = new Location(w, sx, sy, sz);
                        switch (i % 5) {
                            case 0 -> w.spawnParticle(Particle.WITCH, skyPos, 3, 0.5, 0.5, 0.5, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 3, 0.5, 0.5, 0.5, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, skyPos, 3, 0.5, 0.5, 0.5, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, skyPos, 3, 0.5, 0.5, 0.5, 0.01);
                            case 4 -> DisplayBuilder.dustParticles(skyPos, 3, 0.5, 180, 0, 0, 1.5f);
                        }
                    }
                }
                return;
            }

            if (ticksAlive > 60 && ticksAlive <= 660) {
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sy = center.getY() + 15 + Math.random() * 35;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        Location skyPos = new Location(w, sx, sy, sz);
                        switch (i % 5) {
                            case 0 -> w.spawnParticle(Particle.WITCH, skyPos, 5, 1, 1, 1, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 5, 1, 1, 1, 0.03);
                            case 2 -> w.spawnParticle(Particle.END_ROD, skyPos, 5, 1, 1, 1, 0.03);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, skyPos, 5, 1, 1, 1, 0.02);
                            case 4 -> DisplayBuilder.dustParticles(skyPos, 5, 1.0, 180, 0, 0, 1.5f);
                        }
                    }
                }

                if (ticksAlive % 15 == 0) {
                    for (int s = 0; s < 5; s++) {
                        double dx = center.getX() + (Math.random() - 0.5) * 12;
                        double dz = center.getZ() + (Math.random() - 0.5) * 12;
                        Location drip = new Location(w, dx, center.getY() + 30, dz);
                        switch (s) {
                            case 0 -> w.spawnParticle(Particle.WITCH, drip, 10, 0.5, 10, 0.5, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, drip, 10, 0.5, 10, 0.5, 0.03);
                            case 2 -> w.spawnParticle(Particle.END_ROD, drip, 10, 0.5, 10, 0.5, 0.03);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, drip, 10, 0.5, 10, 0.5, 0.02);
                            case 4 -> DisplayBuilder.dustParticles(drip, 10, 5.0, 180, 0, 0, 1.2f);
                        }
                    }
                }

                if (ticksAlive % 60 == 0) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.5f, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TripleDensitySky(plugin); }
    }

    // =========================================================================
    // 46. CALAMITY PULSE -- massive shockwave from Calamitas position
    // =========================================================================
    public static class CalamityPulse extends EnvironmentalAttack {

        private boolean pulsed = false;

        public CalamityPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamity_pulse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 60) {
                if (ticksAlive % 5 == 0) {
                    double pulseRadius = 1 + (ticksAlive % 10 == 0 ? 0.5 : 0);
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), pulseRadius,
                            Particle.DUST, 15,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.4f);
                }
                return;
            }

            if (!pulsed) {
                pulsed = true;
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.4f);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 5) {
                        p.damage(18.0);
                        DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1.2f, 0.6f);
                    } else if (dist <= 15) {
                        p.damage(14.0);
                    }
                    if (dist <= 15 && dist > 0) {
                        org.bukkit.util.Vector kb = p.getLocation().toVector()
                                .subtract(center.toVector()).normalize().multiply(2.0);
                        kb.setY(0.5);
                        p.setVelocity(kb);
                    }
                }
            }

            int pulseTick = ticksAlive - 61;
            if (pulseTick >= 0 && pulseTick <= 150) {
                double ringRadius = pulseTick * 2.0;
                if (ringRadius <= 30 && pulseTick % 2 == 0) {
                    Particle.DustOptions crimsonDust = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f);
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), ringRadius, Particle.WITCH, 20, null);
                    DisplayBuilder.particleRing(center.clone().add(0, 2, 0), ringRadius, Particle.TOTEM_OF_UNDYING, 15, null);
                    DisplayBuilder.particleRing(center.clone().add(0, 3, 0), ringRadius, Particle.END_ROD, 15, null);
                    DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), ringRadius, Particle.DUST,
                            (int) (ringRadius * 3), crimsonDust);
                }
                if (pulseTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CalamityPulse(plugin); }
    }

    // =========================================================================
    // 47. FALLING SKY -- all streams fire downward then bounce back up
    // =========================================================================
    public static class FallingSky extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> scorchHandles = new ArrayList<>();
        private boolean impacted = false;

        public FallingSky(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_sky", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(8.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 160) {
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sy = center.getY() + 40 + Math.random() * 20;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, sx, sy, sz), 3, 1, 1, 1, 0.01);
                        DisplayBuilder.dustParticles(new Location(w, sx, sy, sz), 3, 1.0, 0, 0, 0, 2.0f);
                    }
                }
                if (ticksAlive == 80) DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 1.5f, 0.3f);
                if (ticksAlive == 140) DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.2f);
                return;
            }

            if (ticksAlive > 160 && ticksAlive <= 200) {
                double progress = (ticksAlive - 160) / 40.0;
                double columnY = center.getY() + 50 * (1 - progress);
                if (ticksAlive % 2 == 0) {
                    for (int c = 0; c < 5; c++) {
                        double cx = center.getX() + (c - 2) * 4;
                        double cz = center.getZ() + ((c % 2 == 0) ? -3 : 3);
                        Particle type = switch (c) {
                            case 0 -> Particle.WITCH;
                            case 1 -> Particle.TOTEM_OF_UNDYING;
                            case 2 -> Particle.END_ROD;
                            case 3 -> Particle.DRAGON_BREATH;
                            default -> Particle.FLAME;
                        };
                        for (double y = columnY; y < columnY + 15; y += 2) {
                            w.spawnParticle(type, new Location(w, cx, y, cz), 8, 1, 0.5, 1, 0.02);
                        }
                    }
                }

                if (ticksAlive % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().getY() >= columnY - 2 && p.getLocation().getY() <= columnY + 15) {
                            p.damage(8.0);
                        }
                    }
                }
            }

            if (ticksAlive == 201 && !impacted) {
                impacted = true;
                for (int c = 0; c < 5; c++) {
                    double cx = center.getX() + (c - 2) * 4;
                    double cz = center.getZ() + ((c % 2 == 0) ? -3 : 3);
                    Location impactPos = new Location(w, cx, center.getY(), cz);
                    DisplayBuilder.playSound(impactPos, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);

                    Material mat = Material.MAGMA_BLOCK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(impactPos, mat);
                    h.scale(1.0f, 1.0f, 1.0f).glow(200, 80, 0).interpolation(5, 0);
                    scorchHandles.add(h);
                    spawnedEntities.add(h.entity());

                    for (double r = 0; r < 8; r += 0.5) {
                        for (int dir = 0; dir < 8; dir++) {
                            double angle = (Math.PI * 2 * dir) / 8;
                            Location spread = impactPos.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                            Particle type = switch (c) {
                                case 0 -> Particle.WITCH;
                                case 1 -> Particle.TOTEM_OF_UNDYING;
                                case 2 -> Particle.END_ROD;
                                case 3 -> Particle.DRAGON_BREATH;
                                default -> Particle.FLAME;
                            };
                            w.spawnParticle(type, spread, 2, 0.2, 0.2, 0.2, 0.02);
                        }
                    }

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(impactPos) <= 25.0) {
                            p.damage(10.0);
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.7f);
            }

            if (ticksAlive > 200 && ticksAlive <= 240) {
                double bounceY = center.getY() + ((ticksAlive - 200) / 40.0) * 50;
                if (ticksAlive % 3 == 0) {
                    for (int c = 0; c < 5; c++) {
                        double cx = center.getX() + (c - 2) * 4;
                        double cz = center.getZ() + ((c % 2 == 0) ? -3 : 3);
                        w.spawnParticle(Particle.END_ROD, new Location(w, cx, bounceY, cz),
                                10, 1, 2, 1, 0.05);
                    }
                }
            }

            if (ticksAlive == 240) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FallingSky(plugin); }
    }

    // =========================================================================
    // 48. RESONANCE CASCADE -- all environmental systems fire simultaneously
    // =========================================================================
    public static class ResonanceCascade extends EnvironmentalAttack {

        public ResonanceCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("resonance_cascade", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(420);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 2.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 120) {
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double px = center.getX() + (Math.random() - 0.5) * 20;
                        double py = center.getY() + Math.random() * 20;
                        double pz = center.getZ() + (Math.random() - 0.5) * 20;
                        switch (i % 5) {
                            case 0 -> w.spawnParticle(Particle.WITCH, new Location(w, px, py, pz), 5, 1, 1, 1, 0);
                            case 1 -> w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, px, py, pz), 5, 1, 1, 1, 0.02);
                            case 2 -> w.spawnParticle(Particle.FLAME, new Location(w, px, py, pz), 5, 1, 1, 1, 0.02);
                            case 3 -> w.spawnParticle(Particle.PORTAL, new Location(w, px, py, pz), 5, 1, 1, 1, 0.05);
                            case 4 -> w.spawnParticle(Particle.END_ROD, new Location(w, px, py, pz), 5, 1, 1, 1, 0.02);
                        }
                    }
                }

                if (ticksAlive == 80) {
                    // 2 second silence
                }
                return;
            }

            if (ticksAlive > 120 && ticksAlive <= 420) {
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 30; i++) {
                        double px = center.getX() + (Math.random() - 0.5) * 20;
                        double py = center.getY() + Math.random() * 15;
                        double pz = center.getZ() + (Math.random() - 0.5) * 20;
                        switch ((int) (Math.random() * 10)) {
                            case 0 -> w.spawnParticle(Particle.WITCH, new Location(w, px, py, pz), 3, 0.5, 0.5, 0.5, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, new Location(w, px, py, pz), 3, 0.5, 0.5, 0.5, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, new Location(w, px, py, pz), 3, 0.5, 0.5, 0.5, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, px, py, pz), 3, 0.5, 0.5, 0.5, 0.01);
                            case 4 -> DisplayBuilder.dustParticles(new Location(w, px, py, pz), 3, 0.5, 180, 0, 0, 1.5f);
                            case 5 -> w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, px, py, pz), 3, 0.5, 0.5, 0.5, 0.02);
                            case 6 -> w.spawnParticle(Particle.FLAME, new Location(w, px, py, pz), 3, 0.5, 0.5, 0.5, 0.02);
                            case 7 -> DisplayBuilder.dustParticles(new Location(w, px, py, pz), 3, 0.5, 100, 0, 200, 1.5f);
                            case 8 -> w.spawnParticle(Particle.PORTAL, new Location(w, px, py, pz), 3, 0.5, 0.5, 0.5, 0.05);
                            case 9 -> DisplayBuilder.dustParticles(new Location(w, px, py, pz), 3, 0.5, 30, 0, 60, 1.2f);
                        }
                    }
                }

                if (ticksAlive % 20 == 0) {
                    double ringRadius = ((ticksAlive - 120) % 60) * 0.5;
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), ringRadius,
                            Particle.DUST, 30, new Particle.DustOptions(Color.fromRGB(100, 0, 200), 1.5f));
                }

                if (ticksAlive == 420) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.5f);
                    w.spawnParticle(Particle.WITCH, center, 400, 10, 5, 10, 0.1);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 400, 10, 5, 10, 0.1);
                    w.spawnParticle(Particle.END_ROD, center, 400, 10, 5, 10, 0.1);
                    w.spawnParticle(Particle.DRAGON_BREATH, center, 400, 10, 5, 10, 0.05);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ResonanceCascade(plugin); }
    }

    // =========================================================================
    // 49. THE LAST GROUND -- 6 void holes open in arena floor
    // =========================================================================
    public static class TheLastGround extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> holeHandles = new ArrayList<>();
        private final List<Location> holePositions = new ArrayList<>();

        public TheLastGround(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_last_ground", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(8.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double rx = center.getX() + (Math.random() - 0.5) * 16;
                        double rz = center.getZ() + (Math.random() - 0.5) * 16;
                        DisplayBuilder.dustParticles(new Location(w, rx, center.getY() + 0.3, rz),
                                5, 0.5, 255, 100, 0, 1.2f);
                    }
                }
                return;
            }

            if (ticksAlive == 61) {
                for (int h = 0; h < 6; h++) {
                    double hx, hz;
                    do {
                        hx = center.getX() + (Math.random() - 0.5) * 14;
                        hz = center.getZ() + (Math.random() - 0.5) * 14;
                    } while (Math.abs(hx - center.getX()) < 2 && Math.abs(hz - center.getZ()) < 2);

                    Location holePos = new Location(w, hx, center.getY(), hz);
                    holePositions.add(holePos);

                    BlockDisplayHandle falling = displayBuilder.spawnBlock(holePos, Material.OBSIDIAN);
                    falling.scale(3.0f, 0.2f, 3.0f).glow(20, 0, 40).interpolation(60, 0);
                    holeHandles.add(falling);
                    spawnedEntities.add(falling.entity());

                    BlockDisplayHandle opening = displayBuilder.spawnBlock(
                            holePos.clone().add(0, -0.5, 0), Material.BLACK_STAINED_GLASS);
                    opening.scale(3.0f, 0.05f, 3.0f).glow(0, 0, 0).interpolation(5, 0);
                    spawnedEntities.add(opening.entity());

                    DisplayBuilder.playSound(holePos, Sound.BLOCK_STONE_BREAK, 1.2f, 0.3f);
                    DisplayBuilder.playSound(holePos, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.4f);
                }
            }

            if (ticksAlive > 61) {
                if (ticksAlive % 4 == 0) {
                    for (Location hp : holePositions) {
                        for (double y = -5; y <= 0; y += 1.0) {
                            w.spawnParticle(Particle.PORTAL, hp.clone().add(0, y, 0),
                                    8, 1.5, 0.3, 1.5, 0.05);
                        }
                        DisplayBuilder.dustParticles(hp, 5, 1.5, 0, 0, 0, 1.5f);
                    }
                }

                if (ticksAlive % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location hp : holePositions) {
                            double dx = Math.abs(p.getLocation().getX() - hp.getX());
                            double dz = Math.abs(p.getLocation().getZ() - hp.getZ());
                            if (dx <= 1.5 && dz <= 1.5) {
                                p.damage(8.0);
                                p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 0.5, 0)));
                                break;
                            }
                        }
                    }
                }

                if (ticksAlive % 40 == 0) {
                    Location randomHole = holePositions.get((int) (Math.random() * holePositions.size()));
                    DisplayBuilder.playSound(randomHole,
                            Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, 0.3f, 0.3f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastGround(plugin); }
    }

    // =========================================================================
    // 50. END OF CALAMITY -- pre-death environmental finale (no damage)
    // =========================================================================
    public static class EndOfCalamity extends EnvironmentalAttack {

        public EndOfCalamity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("end_of_calamity", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // Fires instantly, no buildup
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // TICKS 0-40: Streams retract one by one
            if (ticksAlive <= 40) {
                int streamIndex = ticksAlive / 8;
                if (ticksAlive % 8 == 0 && streamIndex < 5) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f,
                            0.7f + streamIndex * 0.1f);
                    for (double y = 0; y < 40; y += 2) {
                        Particle type = switch (streamIndex) {
                            case 0 -> Particle.WITCH;
                            case 1 -> Particle.TOTEM_OF_UNDYING;
                            case 2 -> Particle.END_ROD;
                            case 3 -> Particle.DRAGON_BREATH;
                            default -> Particle.FLAME;
                        };
                        double sx = center.getX() + (streamIndex - 2) * 4;
                        w.spawnParticle(type, new Location(w, sx, center.getY() + y, center.getZ()),
                                3, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }

            // TICKS 40-100: Death Resonance Pillar shatters
            if (ticksAlive == 50) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DEATH, 1.0f, 0.5f);
                DisplayBuilder.dustParticles(center.clone().add(2, 3, -1), 150, 3.0, 80, 0, 80, 1.5f);
            }

            // TICKS 100-160: Void zones collapse, spires shatter, veins extinguish
            if (ticksAlive == 110) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
                w.spawnParticle(Particle.PORTAL, center, 100, 8, 2, 8, 0.1);
                DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 80, 5.0, 150, 100, 255, 1.2f);
            }

            // TICKS 160-240: Floor cracks seal, void holes close
            if (ticksAlive >= 160 && ticksAlive <= 240) {
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.6f);
                }
            }

            // TICKS 240-320: Complete darkness and silence
            // Nothing renders -- this is intentional

            // TICKS 320-400: Final white pulse and stream echoes
            if (ticksAlive > 320 && ticksAlive <= 400) {
                int finalTick = ticksAlive - 320;

                if (finalTick <= 40) {
                    double radius = finalTick * 0.5;
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), radius,
                            Particle.END_ROD, (int) (radius * 4), null);
                }

                if (finalTick > 40 && finalTick <= 80) {
                    int echoIndex = (finalTick - 40) / 8;
                    if ((finalTick - 40) % 8 == 0 && echoIndex < 5) {
                        Particle type = switch (echoIndex) {
                            case 0 -> Particle.WITCH;
                            case 1 -> Particle.TOTEM_OF_UNDYING;
                            case 2 -> Particle.END_ROD;
                            case 3 -> Particle.DRAGON_BREATH;
                            default -> Particle.FLAME;
                        };
                        DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), 0.5, type, 5, null);
                    }
                }

                if (finalTick == 60) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EndOfCalamity(plugin); }
    }
}
