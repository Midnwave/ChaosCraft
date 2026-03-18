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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #11-20
 * THE LIVING SKY (#11-12) + ISLAND MEMORY ERUPTIONS (#13-20)
 *
 * Design: No status effects. Damage 6.0-18.0 HP. AxisAngle4f only.
 */
public final class CalamityDawnB {

    private CalamityDawnB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FiveFoldDescent(plugin));
        registry.register(new SkyCollapse(plugin));
        registry.register(new VoidZonePulse(plugin));
        registry.register(new CrystalSpireSurge(plugin));
        registry.register(new BrimstoneVeinIgnition(plugin));
        registry.register(new DeathResonanceShockwave(plugin));
        registry.register(new VoidZoneCollapse(plugin));
        registry.register(new TheInheritance(plugin));
        registry.register(new GhostVeinTrace(plugin));
        registry.register(new CrystalLatticeTrap(plugin));
    }

    // =========================================================================
    // 11. FIVE-FOLD DESCENT -- all 5 streams fire as columns to ground then spread
    // =========================================================================
    public static class FiveFoldDescent extends EnvironmentalAttack {

        private boolean fired = false;

        public FiveFoldDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("five_fold_descent", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(6000); // HP threshold
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-100 ticks (5 seconds) -- streams thickening
            if (ticksAlive <= 100) {
                if (ticksAlive % 5 == 0) {
                    // Countdown rings descending
                    int ringCount = 5 - (ticksAlive / 20);
                    if (ringCount > 0) {
                        double ringY = center.getY() + 45 - (ticksAlive % 20) * 1.0;
                        for (int r = 0; r < Math.min(ringCount, 5); r++) {
                            Particle type = switch (r) {
                                case 0 -> Particle.WITCH;
                                case 1 -> Particle.TOTEM_OF_UNDYING;
                                case 2 -> Particle.END_ROD;
                                case 3 -> Particle.DRAGON_BREATH;
                                default -> Particle.FLAME;
                            };
                            DisplayBuilder.particleRing(center.clone().add(0, ringY - center.getY() - r * 4, 0),
                                    3.0, type, 20, null);
                        }
                    }
                }
                return;
            }

            // Fire at tick 101
            if (!fired) {
                fired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.3f, 0.6f);

                // 5 columns hit ground
                double[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}, {0.7, 0.7}};
                for (int s = 0; s < 5; s++) {
                    double colX = center.getX() + (Math.random() - 0.5) * 8;
                    double colZ = center.getZ() + (Math.random() - 0.5) * 8;
                    Location colBase = new Location(w, colX, center.getY(), colZ);

                    triggerImpactDamage(colBase);
                    DisplayBuilder.playSound(colBase, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);

                    // Column particles sky to ground
                    Particle type = switch (s) {
                        case 0 -> Particle.WITCH;
                        case 1 -> Particle.TOTEM_OF_UNDYING;
                        case 2 -> Particle.END_ROD;
                        case 3 -> Particle.DRAGON_BREATH;
                        default -> Particle.FLAME;
                    };
                    for (double y = 0; y < 40; y += 2) {
                        w.spawnParticle(type, colBase.clone().add(0, y, 0), 10, 1, 0.5, 1, 0.02);
                    }

                    // Spread in cardinal/diagonal direction
                    double spreadDx = directions[s][0];
                    double spreadDz = directions[s][1];
                    for (double d = 0; d < 10; d += 0.5) {
                        Location spreadPos = colBase.clone().add(spreadDx * d, 0.5, spreadDz * d);
                        w.spawnParticle(type, spreadPos, 5, 0.5, 1.5, 0.5, 0.02);
                    }
                }

                // Spread damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 10) {
                        p.damage(10.0); // 5 hearts
                        org.bukkit.util.Vector kb = p.getLocation().toVector()
                                .subtract(center.toVector()).normalize().multiply(1.0);
                        kb.setY(0.3);
                        p.setVelocity(kb);
                    }
                }
            }

            // Aftermath: streams retracting 101-200
            int afterTick = ticksAlive - 101;
            if (afterTick > 0 && afterTick <= 60 && afterTick % 6 == 0) {
                double retractY = afterTick * 0.67;
                for (int s = 0; s < 5; s++) {
                    double sx = center.getX() + (Math.random() - 0.5) * 10;
                    double sz = center.getZ() + (Math.random() - 0.5) * 10;
                    w.spawnParticle(Particle.END_ROD, new Location(w, sx, center.getY() + retractY, sz),
                            5, 0.5, 0.5, 0.5, 0.02);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FiveFoldDescent(plugin); }
    }

    // =========================================================================
    // 12. SKY COLLAPSE -- all streams descend as massive unified impact
    // =========================================================================
    public static class SkyCollapse extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> rubbleHandles = new ArrayList<>();
        private boolean collapsed = false;

        public SkyCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_collapse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400); // 20 seconds
            config.setCooldownTicks(12000); // HP threshold at 25%
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0); // 9 hearts -- unavoidable if not jumping
            config.setImpactRadius(10.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-100 ticks (5 seconds) -- sky filling with color
            if (ticksAlive <= 100) {
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        double sy = center.getY() + 50 + Math.random() * 20;
                        w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, sx, sy, sz), 3, 1, 1, 1, 0.01);
                        DisplayBuilder.dustParticles(new Location(w, sx, sy, sz), 3, 1.0, 180, 0, 0, 1.5f);
                    }
                }
                if (ticksAlive == 60) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.5f);
                }
                return;
            }

            // Descent: 101-140 ticks -- 5 columns descend
            if (ticksAlive > 100 && ticksAlive <= 140) {
                double progress = (ticksAlive - 100) / 40.0;
                double colY = center.getY() + 50 * (1 - progress);
                if (ticksAlive % 2 == 0) {
                    for (int c = 0; c < 5; c++) {
                        double cx = center.getX() + (c - 2) * 5;
                        double cz = center.getZ() + ((c % 2 == 0) ? -3 : 3);
                        for (double y = colY; y < colY + 10; y += 2) {
                            Location colPos = new Location(w, cx, y, cz);
                            switch (c) {
                                case 0 -> w.spawnParticle(Particle.WITCH, colPos, 10, 1, 1, 1, 0);
                                case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, colPos, 10, 1, 1, 1, 0.03);
                                case 2 -> w.spawnParticle(Particle.END_ROD, colPos, 10, 1, 1, 1, 0.03);
                                case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, colPos, 10, 1, 1, 1, 0.02);
                                case 4 -> DisplayBuilder.dustParticles(colPos, 10, 1.0, 180, 0, 0, 1.5f);
                            }
                        }
                    }
                }
            }

            // Ground impact at tick 141
            if (ticksAlive > 140 && !collapsed) {
                collapsed = true;
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_HURT, 2.0f, 0.5f);

                // Massive ground blast
                w.spawnParticle(Particle.WITCH, center, 200, 10, 1, 10, 0.1);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 150, 10, 1, 10, 0.1);
                w.spawnParticle(Particle.END_ROD, center, 150, 10, 1, 10, 0.1);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 150, 10, 1, 10, 0.05);
                w.spawnParticle(Particle.FLASH, center, 3, 0, 0, 0, 0);

                // Rubble displays
                for (int i = 0; i < 10; i++) {
                    double rx = center.getX() + (Math.random() - 0.5) * 18;
                    double rz = center.getZ() + (Math.random() - 0.5) * 18;
                    Location rubblePos = new Location(w, rx, center.getY() + 0.5, rz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(rubblePos, Material.CRYING_OBSIDIAN);
                    h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 200).interpolation(40, 0);
                    rubbleHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Post-impact visibility fill: 141-200 ticks
            if (ticksAlive > 140 && ticksAlive <= 200) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double fx = center.getX() + (Math.random() - 0.5) * 20;
                        double fz = center.getZ() + (Math.random() - 0.5) * 20;
                        double fy = center.getY() + Math.random() * 8;
                        Location fillPos = new Location(w, fx, fy, fz);
                        switch ((int) (Math.random() * 5)) {
                            case 0 -> w.spawnParticle(Particle.WITCH, fillPos, 3, 0.5, 0.5, 0.5, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, fillPos, 3, 0.5, 0.5, 0.5, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, fillPos, 3, 0.5, 0.5, 0.5, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, fillPos, 3, 0.5, 0.5, 0.5, 0.01);
                            case 4 -> DisplayBuilder.dustParticles(fillPos, 3, 0.5, 180, 0, 0, 1.2f);
                        }
                    }
                }
            }

            // Stream reformation: 201-400 ticks
            if (ticksAlive > 200 && ticksAlive <= 400 && ticksAlive % 10 == 0) {
                double reformY = center.getY() + ((ticksAlive - 200) / 200.0) * 50;
                for (int s = 0; s < 5; s++) {
                    double sx = center.getX() + (s - 2) * 5;
                    w.spawnParticle(Particle.END_ROD, new Location(w, sx, reformY, center.getZ()),
                            5, 0.3, 0.3, 0.3, 0.02);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyCollapse(plugin); }
    }

    // =========================================================================
    // 13. VOID ZONE PULSE -- void zones expand outward then contract
    // =========================================================================
    public static class VoidZonePulse extends EnvironmentalAttack {

        private final List<Location> voidPositions = new ArrayList<>();
        private double expansionRadius = 0;

        public VoidZonePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_zone_pulse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per cycle in expanded zone
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate 3-5 void zone positions
            int count = 3 + (int) (Math.random() * 3);
            for (int i = 0; i < count; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 4 + Math.random() * 6;
                voidPositions.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-40 ticks
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    for (Location vp : voidPositions) {
                        w.spawnParticle(Particle.PORTAL, vp.clone().add(0, 1, 0), 15, 1, 1, 1, 0.1);
                        w.spawnParticle(Particle.END_ROD, vp.clone().add(0, 0.5, 0), 5, 1.5, 0.1, 1.5, 0.01);
                    }
                }
                return;
            }

            // Expansion: 41-60 ticks
            if (ticksAlive > 40 && ticksAlive <= 60) {
                expansionRadius = ((ticksAlive - 40) / 20.0) * 3.0; // Expand to 3 blocks
            }

            // Hold: 61-140 ticks
            if (ticksAlive > 60 && ticksAlive <= 140) {
                expansionRadius = 3.0;
            }

            // Contraction: 141-160 ticks
            if (ticksAlive > 140 && ticksAlive <= 160) {
                expansionRadius = 3.0 * (1 - (ticksAlive - 140) / 20.0);
            }

            // Active phase particles and damage: 41-160
            if (ticksAlive > 40 && ticksAlive <= 160) {
                if (ticksAlive % 3 == 0) {
                    for (Location vp : voidPositions) {
                        DisplayBuilder.particleRing(vp.clone().add(0, 0.5, 0), expansionRadius,
                                Particle.PORTAL, 15, null);
                        w.spawnParticle(Particle.PORTAL, vp.clone().add(0, 1, 0), 8,
                                expansionRadius * 0.5, 0.5, expansionRadius * 0.5, 0.05);
                    }
                }

                // Damage in expanded zones
                if (ticksAlive % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location vp : voidPositions) {
                            if (p.getLocation().distanceSquared(vp) <= expansionRadius * expansionRadius + 4) {
                                p.damage(6.0);
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidZonePulse(plugin); }
    }

    // =========================================================================
    // 14. CRYSTAL SPIRE SURGE -- crystal spires grow tall then shatter
    // =========================================================================
    public static class CrystalSpireSurge extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> spireHandles = new ArrayList<>();
        private final List<Location> spirePositions = new ArrayList<>();
        private boolean shattered = false;

        public CrystalSpireSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_spire_surge", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts burst
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            int count = 4 + (int) (Math.random() * 4); // 4-7 spires
            for (int i = 0; i < count; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 3 + Math.random() * 7;
                spirePositions.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks -- vibrating
            if (ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.8f, 0.9f);
                }
                return;
            }

            // Growth: 61-90 ticks -- spires grow 5 blocks taller
            if (ticksAlive > 60 && ticksAlive <= 90) {
                if (ticksAlive == 61) {
                    for (Location sp : spirePositions) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(sp.clone().add(0, 2, 0),
                                Material.AMETHYST_BLOCK);
                        h.scale(0.6f, 8.0f, 0.6f).glow(150, 100, 255).interpolation(30, 0);
                        spireHandles.add(h);
                        spawnedEntities.add(h.entity());
                        DisplayBuilder.playSound(sp, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.9f, 0.8f);
                    }
                }
                if (ticksAlive % 4 == 0) {
                    for (Location sp : spirePositions) {
                        DisplayBuilder.dustParticles(sp.clone().add(0, 8, 0), 8, 0.5, 150, 100, 255, 0.8f);
                    }
                }
                return;
            }

            // Hold: 91-150 ticks (3 seconds)
            if (ticksAlive > 90 && ticksAlive <= 150) {
                if (ticksAlive % 6 == 0) {
                    for (Location sp : spirePositions) {
                        DisplayBuilder.dustParticles(sp.clone().add(0, 6, 0), 5, 0.3, 150, 100, 255, 0.6f);
                    }
                }
                return;
            }

            // Shatter at tick 151
            if (!shattered) {
                shattered = true;
                for (Location sp : spirePositions) {
                    triggerImpactDamage(sp);
                    DisplayBuilder.playSound(sp, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 0.7f);
                    DisplayBuilder.playSound(sp, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.1f);

                    // Shatter burst
                    DisplayBuilder.dustParticles(sp.clone().add(0, 4, 0), 40, 2.5, 150, 100, 255, 1.2f);

                    // Crystal shards flying outward
                    for (int shard = 0; shard < 4; shard++) {
                        double shardAngle = Math.random() * 2 * Math.PI;
                        for (double d = 0; d < 8; d += 0.5) {
                            Location shardPos = sp.clone().add(
                                    Math.cos(shardAngle) * d, 3 + Math.random(), Math.sin(shardAngle) * d);
                            DisplayBuilder.dustParticles(shardPos, 2, 0.2, 180, 140, 255, 1.8f);
                        }
                    }

                    // Shard damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(sp);
                        if (dist <= 8 && dist > 2) {
                            p.damage(7.0); // 3.5 hearts shard hit
                        }
                    }

                    // Debris displays
                    for (int d = 0; d < 5; d++) {
                        Location debrisPos = sp.clone().add(
                                (Math.random() - 0.5) * 3, Math.random() * 0.5, (Math.random() - 0.5) * 3);
                        BlockDisplayHandle dh = displayBuilder.spawnBlock(debrisPos, Material.BLACKSTONE);
                        dh.scale(0.2f, 0.2f, 0.2f).glow(50, 0, 100).interpolation(20, 0);
                        spawnedEntities.add(dh.entity());
                    }
                }
            }

            // Post-shatter dust cloud: 151-200
            if (ticksAlive > 150 && ticksAlive <= 200 && ticksAlive % 8 == 0) {
                for (Location sp : spirePositions) {
                    DisplayBuilder.dustParticles(sp.clone().add(0, 3 + Math.random() * 3, 0),
                            10, 1.5, 150, 100, 255, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalSpireSurge(plugin); }
    }

    // =========================================================================
    // 15. BRIMSTONE VEIN IGNITION -- floor veins erupt with flame
    // =========================================================================
    public static class BrimstoneVeinIgnition extends EnvironmentalAttack {

        private final List<Location> veinPositions = new ArrayList<>();

        public BrimstoneVeinIgnition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_vein_ignition", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per 6 ticks
            config.setDamageRadius(0.0);
            config.setDurationTicks(220); // 11 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate vein positions across arena
            for (int i = 0; i < 12; i++) {
                double vx = center.getX() + (Math.random() - 0.5) * 18;
                double vz = center.getZ() + (Math.random() - 0.5) * 18;
                veinPositions.add(new Location(w, vx, center.getY(), vz));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks -- veins seeping
            if (ticksAlive <= 60) {
                if (ticksAlive % 6 == 0) {
                    for (Location vp : veinPositions) {
                        DisplayBuilder.dustParticles(vp.clone().add(0, 0.5, 0), 5, 0.5, 255, 80, 0, 1.0f);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.7f);
                }
                return;
            }

            // Ignition: 61-220 ticks (8 seconds)
            int igniteTick = ticksAlive - 61;
            if (igniteTick >= 0) {
                // Final 40 ticks = intensified
                boolean intense = igniteTick > 120;
                int flameRate = intense ? 8 : 4;

                if (igniteTick % flameRate == 0) {
                    for (Location vp : veinPositions) {
                        w.spawnParticle(Particle.FLAME, vp.clone().add(0, 0.3, 0), 8, 0.3, 0.3, 0.3, 0.02);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, vp.clone().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.02);
                    }
                }

                // Damage players on veins
                int damageInterval = intense ? 4 : 6;
                if (igniteTick % damageInterval == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location vp : veinPositions) {
                            if (p.getLocation().distanceSquared(vp) <= 2.25) { // 1.5 block radius
                                p.damage(6.0);
                                break;
                            }
                        }
                    }
                }

                if (igniteTick % 40 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.5f);
                }

                // Extinguish burst at end
                if (igniteTick == 159) {
                    for (Location vp : veinPositions) {
                        w.spawnParticle(Particle.SMOKE, vp.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.03);
                        DisplayBuilder.playSound(vp, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneVeinIgnition(plugin); }
    }

    // =========================================================================
    // 16. DEATH RESONANCE SHOCKWAVE -- pillar fires triple shockwave rings
    // =========================================================================
    public static class DeathResonanceShockwave extends EnvironmentalAttack {

        private Location pillarPos;

        public DeathResonanceShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_resonance_shockwave", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(6000); // HP threshold
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts per ring
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pillar near center, slightly offset
            pillarPos = center.clone().add(2, 0, -1);
            DisplayBuilder.playSound(pillarPos, Sound.ENTITY_WARDEN_AMBIENT, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || pillarPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-80 ticks (4 seconds)
            if (ticksAlive <= 80) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(pillarPos.clone().add(0, 3, 0), 15, 1.5, 80, 0, 80, 2.0f);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, pillarPos.clone().add(0, 6, 0), 5, 0.3, 0.5, 0.3, 0.02);
                    // Preview rings
                    for (double r : new double[]{5, 10, 15}) {
                        DisplayBuilder.particleRing(pillarPos.clone().add(0, 1, 0), r, Particle.DUST,
                                (int) (r * 3), new Particle.DustOptions(Color.fromRGB(80, 0, 80), 1.0f));
                    }
                }
                return;
            }

            // Shockwave rings: tick 81 (ring1), 96 (ring2), 111 (ring3)
            for (int ring = 0; ring < 3; ring++) {
                int ringStart = 81 + ring * 15;
                int ringTick = ticksAlive - ringStart;
                if (ringTick >= 0 && ringTick <= 30) {
                    double ringRadius = ringTick * 2.0;
                    if (ringRadius <= 20) {
                        DisplayBuilder.particleRing(pillarPos.clone().add(0, 1.5, 0), ringRadius, Particle.DUST,
                                (int) (ringRadius * 5),
                                new Particle.DustOptions(Color.fromRGB(80, 0, 80), 2.0f));
                        DisplayBuilder.particleRing(pillarPos.clone().add(0, 2.5, 0), ringRadius, Particle.DUST,
                                (int) (ringRadius * 3),
                                new Particle.DustOptions(Color.fromRGB(80, 0, 80), 1.5f));

                        // Ring damage
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double pDist = p.getLocation().distance(pillarPos);
                            if (Math.abs(pDist - ringRadius) < 2.0) {
                                p.damage(10.0); // 5 hearts per ring
                            }
                        }
                    }
                }

                if (ticksAlive == ringStart) {
                    DisplayBuilder.playSound(pillarPos, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f,
                            0.6f + ring * 0.1f);
                }
            }

            // Triple hit bonus check at tick 130
            if (ticksAlive == 130) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // If player is very close (didn't dodge), they got all 3
                    if (p.getLocation().distanceSquared(pillarPos) <= 16.0) {
                        p.damage(8.0); // 4 hearts bonus
                        DisplayBuilder.dustParticles(p.getLocation(), 30, 1.0, 200, 0, 200, 2.5f);
                        DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_WARDEN_DEATH, 0.6f, 0.8f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathResonanceShockwave(plugin); }
    }

    // =========================================================================
    // 17. VOID ZONE COLLAPSE -- void zones implode then detonate
    // =========================================================================
    public static class VoidZoneCollapse extends EnvironmentalAttack {

        private final List<Location> collapsePositions = new ArrayList<>();
        private boolean detonated = false;

        public VoidZoneCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_zone_collapse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0); // 8 hearts within 2 blocks
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            int count = 3 + (int) (Math.random() * 3);
            for (int i = 0; i < count; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 4 + Math.random() * 6;
                collapsePositions.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 0.9f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Implosion: 0-100 ticks (shrinking)
            if (ticksAlive <= 100) {
                double progress = ticksAlive / 100.0;
                double radius = 3.0 * (1 - progress);
                if (ticksAlive % 3 == 0) {
                    for (Location cp : collapsePositions) {
                        // Spiraling inward
                        for (int p = 0; p < 8; p++) {
                            double angle = (2 * Math.PI * p) / 8 + ticksAlive * 0.1;
                            double px = cp.getX() + Math.cos(angle) * radius;
                            double pz = cp.getZ() + Math.sin(angle) * radius;
                            w.spawnParticle(Particle.PORTAL, new Location(w, px, cp.getY() + 1, pz),
                                    3, 0.1, 0.5, 0.1, 0.05);
                        }
                    }
                }
                return;
            }

            // Singularity: 101-160 ticks (3 seconds)
            if (ticksAlive > 100 && ticksAlive <= 160) {
                if (ticksAlive % 3 == 0) {
                    for (Location cp : collapsePositions) {
                        w.spawnParticle(Particle.PORTAL, cp.clone().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.05);
                        w.spawnParticle(Particle.END_ROD, cp.clone().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.01);
                    }
                }
                // Pull players within 5 blocks
                if (ticksAlive % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location cp : collapsePositions) {
                            double dist = p.getLocation().distance(cp);
                            if (dist <= 5 && dist > 0.5) {
                                org.bukkit.util.Vector pull = cp.toVector()
                                        .subtract(p.getLocation().toVector()).normalize().multiply(0.15);
                                p.setVelocity(p.getVelocity().add(pull));
                            }
                        }
                    }
                }
            }

            // Detonation at tick 161
            if (ticksAlive > 160 && !detonated) {
                detonated = true;
                for (Location cp : collapsePositions) {
                    triggerImpactDamage(cp);
                    DisplayBuilder.playSound(cp, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                    DisplayBuilder.playSound(cp, Sound.ENTITY_ENDER_DRAGON_HURT, 0.9f, 0.8f);
                    w.spawnParticle(Particle.PORTAL, cp, 200, 4, 4, 4, 0.1);
                    w.spawnParticle(Particle.END_ROD, cp, 80, 4, 4, 4, 0.1);

                    // Knockback from singularity
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(cp) <= 16.0) {
                            org.bukkit.util.Vector kb = p.getLocation().toVector()
                                    .subtract(cp.toVector()).normalize().multiply(2.0);
                            kb.setY(0.5);
                            p.setVelocity(kb);
                        }
                    }
                }
            }

            // Reformation: 161-240 ticks
            if (ticksAlive > 160 && ticksAlive <= 240 && ticksAlive % 6 == 0) {
                double reformProgress = (ticksAlive - 160) / 80.0;
                for (Location cp : collapsePositions) {
                    DisplayBuilder.particleRing(cp.clone().add(0, 0.5, 0), reformProgress * 3.0,
                            Particle.PORTAL, 10, null);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidZoneCollapse(plugin); }
    }

    // =========================================================================
    // 18. THE INHERITANCE -- massive 50% HP event, all residue systems activate
    // =========================================================================
    public static class TheInheritance extends EnvironmentalAttack {

        public TheInheritance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_inheritance", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per system per cycle
            config.setDamageRadius(0.0);
            config.setDurationTicks(500); // 25 seconds
            config.setCooldownTicks(99999); // Once at 50% HP
            config.setTicksBetweenDamage(10);
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

            // Buildup: 0-160 ticks (8 seconds) -- arena goes grey, buildup sounds
            if (ticksAlive <= 160) {
                if (ticksAlive % 4 == 0) {
                    // Greyscale particles
                    DisplayBuilder.dustParticles(center.clone().add(0, 20, 0), 30, 10.0, 128, 128, 128, 1.5f);
                }
                if (ticksAlive == 40) DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.4f);
                if (ticksAlive == 80) DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.5f);
                if (ticksAlive == 120) DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
                return;
            }

            // EXECUTION: 160-560 (20 seconds)
            int execTick = ticksAlive - 160;

            // SECOND 0-2: Void zone expansion (0-40 ticks)
            if (execTick <= 40) {
                if (execTick % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 3 + Math.random() * 7;
                        Location vp = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        w.spawnParticle(Particle.PORTAL, vp, 30, 2, 1, 2, 0.1);
                    }
                }
            }

            // SECOND 2-5: Crystal shards (40-100 ticks)
            if (execTick > 40 && execTick <= 100) {
                if (execTick % 5 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 16;
                        double sz = center.getZ() + (Math.random() - 0.5) * 16;
                        Location sp = new Location(w, sx, center.getY() + 5, sz);
                        DisplayBuilder.dustParticles(sp, 20, 2.0, 150, 100, 255, 1.2f);
                    }
                }
            }

            // SECOND 5-9: Brimstone ignition (100-180 ticks)
            if (execTick > 100 && execTick <= 180) {
                if (execTick % 3 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double bx = center.getX() + (Math.random() - 0.5) * 18;
                        double bz = center.getZ() + (Math.random() - 0.5) * 18;
                        Location bp = new Location(w, bx, center.getY() + 0.3, bz);
                        w.spawnParticle(Particle.FLAME, bp, 5, 0.3, 0.2, 0.3, 0.02);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, bp, 3, 0.3, 0.2, 0.3, 0.02);
                    }
                }
                // Brimstone damage
                if (execTick % 6 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        p.damage(6.0); // 3 hearts, near-unavoidable during this window
                    }
                }
            }

            // SECOND 9-13: Death Resonance rapid rings (180-260 ticks)
            if (execTick > 180 && execTick <= 260) {
                if (execTick % 20 == 0) {
                    double ringRadius = ((execTick - 180) % 40) * 3.0;
                    DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), ringRadius, Particle.DUST,
                            40, new Particle.DustOptions(Color.fromRGB(80, 0, 80), 2.0f));
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
                }
            }

            // SECOND 13-17: All streams at 500% (260-340 ticks)
            if (execTick > 260 && execTick <= 340) {
                if (execTick % 2 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        double sy = center.getY() + 10 + Math.random() * 30;
                        Location streamPos = new Location(w, sx, sy, sz);
                        switch (i % 5) {
                            case 0 -> w.spawnParticle(Particle.WITCH, streamPos, 5, 1, 1, 1, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 5, 1, 1, 1, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, streamPos, 5, 1, 1, 1, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 5, 1, 1, 1, 0.01);
                            case 4 -> DisplayBuilder.dustParticles(streamPos, 5, 1.0, 180, 0, 0, 1.5f);
                        }
                    }
                }
            }

            // SECOND 17-20: Everything simultaneous (340-400 ticks)
            if (execTick > 340 && execTick <= 400) {
                if (execTick % 2 == 0) {
                    // All particle types simultaneously
                    w.spawnParticle(Particle.WITCH, center.clone().add(0, 5, 0), 20, 10, 5, 10, 0.05);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 3, 0), 15, 10, 3, 10, 0.05);
                    w.spawnParticle(Particle.FLAME, center.clone().add(0, 0.5, 0), 20, 10, 0.5, 10, 0.03);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0), 15, 10, 1, 10, 0.03);
                    w.spawnParticle(Particle.PORTAL, center.clone().add(0, 2, 0), 15, 10, 2, 10, 0.1);
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, 8, 0), 10, 10, 4, 10, 0.05);
                }
                // Stacked damage per system in contact
                if (execTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        p.damage(6.0); // Per-system overlap damage
                    }
                }
                if (execTick == 341) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.4f);
                }
            }

            // Return to rest: 400-500 ticks
            if (execTick > 400) {
                if (execTick == 401) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.6f);
                }
                // Inheritance Ring particles
                if (execTick % 4 == 0) {
                    double ringAngle = (ticksAlive * 0.02);
                    for (int i = 0; i < 5; i++) {
                        double a = ringAngle + (2 * Math.PI * i) / 5;
                        double rx = center.getX() + Math.cos(a) * 9;
                        double rz = center.getZ() + Math.sin(a) * 9;
                        Location ringPos = new Location(w, rx, center.getY() + 1.5, rz);
                        switch (i) {
                            case 0 -> w.spawnParticle(Particle.WITCH, ringPos, 2, 0.1, 0.1, 0.1, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, ringPos, 2, 0.1, 0.1, 0.1, 0.01);
                            case 2 -> w.spawnParticle(Particle.END_ROD, ringPos, 2, 0.1, 0.1, 0.1, 0.01);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, ringPos, 2, 0.1, 0.1, 0.1, 0.01);
                            case 4 -> DisplayBuilder.dustParticles(ringPos, 2, 0.1, 180, 0, 0, 1.0f);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheInheritance(plugin); }
    }

    // =========================================================================
    // 19. GHOST VEIN TRACE -- phantom brimstone extensions ignite
    // =========================================================================
    public static class GhostVeinTrace extends EnvironmentalAttack {

        private final List<Location> tracePositions = new ArrayList<>();

        public GhostVeinTrace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghost_vein_trace", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per 8 ticks
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate ghost trace extensions
            for (int i = 0; i < 20; i++) {
                double tx = center.getX() + (Math.random() - 0.5) * 18;
                double tz = center.getZ() + (Math.random() - 0.5) * 18;
                tracePositions.add(new Location(w, tx, center.getY(), tz));
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.3f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-40 ticks -- faint traces appearing
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    for (Location tp : tracePositions) {
                        DisplayBuilder.dustParticles(tp.clone().add(0, 0.3, 0), 3, 0.3, 255, 80, 0, 0.7f);
                    }
                }
                return;
            }

            // Active phase: 41-160 ticks -- ghost traces burning
            if (ticksAlive > 40) {
                if (ticksAlive % 3 == 0) {
                    for (Location tp : tracePositions) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, tp.clone().add(0, 0.3, 0),
                                3, 0.3, 0.2, 0.3, 0.01);
                        DisplayBuilder.dustParticles(tp.clone().add(0, 0.2, 0), 2, 0.3, 255, 130, 50, 0.8f);
                    }
                }

                // Damage on traces
                if (ticksAlive % 8 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location tp : tracePositions) {
                            if (p.getLocation().distanceSquared(tp) <= 1.5) {
                                p.damage(6.0);
                                break;
                            }
                        }
                    }
                }

                // Fade-out smoke in final 40 ticks
                if (ticksAlive > 120 && ticksAlive % 4 == 0) {
                    for (Location tp : tracePositions) {
                        w.spawnParticle(Particle.SMOKE, tp.clone().add(0, 0.5, 0), 3, 0.2, 0.3, 0.2, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GhostVeinTrace(plugin); }
    }

    // =========================================================================
    // 20. CRYSTAL LATTICE TRAP -- crystal blocks spawn on floor in lattice pattern
    // =========================================================================
    public static class CrystalLatticeTrap extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crystalHandles = new ArrayList<>();
        private final List<Location> crystalPositions = new ArrayList<>();
        private boolean shatterStarted = false;
        private int shatterIndex = 0;

        public CrystalLatticeTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_lattice_trap", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts on contact
            config.setDamageRadius(0.0);
            config.setDurationTicks(340); // 17 seconds
            config.setCooldownTicks(1600); // 80 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-80 ticks (4 seconds) -- particles drifting to floor
            if (ticksAlive <= 80) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double px = center.getX() + (Math.random() - 0.5) * 16;
                        double pz = center.getZ() + (Math.random() - 0.5) * 16;
                        double py = center.getY() + 5 - (ticksAlive / 80.0) * 5;
                        DisplayBuilder.dustParticles(new Location(w, px, py, pz), 3, 0.3, 150, 100, 255, 0.7f);
                    }
                }

                // Crystallize at tick 80
                if (ticksAlive == 80) {
                    int crystalCount = 12 + (int) (Math.random() * 9); // 12-20
                    for (int i = 0; i < crystalCount; i++) {
                        double cx = center.getX() + (Math.random() - 0.5) * 16;
                        double cz = center.getZ() + (Math.random() - 0.5) * 16;
                        // Avoid exact center
                        if (Math.abs(cx - center.getX()) < 2 && Math.abs(cz - center.getZ()) < 2) {
                            cx += 3;
                        }
                        Location cPos = new Location(w, cx, center.getY() + 0.5, cz);
                        crystalPositions.add(cPos);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(cPos, Material.AMETHYST_BLOCK);
                        h.scale(0.8f, 0.8f, 0.8f).glow(150, 100, 255).interpolation(5, 0);
                        crystalHandles.add(h);
                        spawnedEntities.add(h.entity());
                        DisplayBuilder.playSound(cPos, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.8f, 0.9f);
                    }
                }
                return;
            }

            // Crystal active phase: 81-320 ticks (12 seconds)
            int crystalTick = ticksAlive - 81;
            if (crystalTick >= 0 && crystalTick <= 240 && !shatterStarted) {
                // Crystal emissions
                if (crystalTick % 6 == 0) {
                    for (Location cp : crystalPositions) {
                        DisplayBuilder.dustParticles(cp, 3, 0.5, 150, 100, 255, 1.0f);
                    }
                }

                // Contact damage
                if (crystalTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location cp : crystalPositions) {
                            if (p.getLocation().distanceSquared(cp) <= 1.5) {
                                p.damage(6.0);
                                break;
                            }
                        }
                    }
                }

                // Resonance sound
                if (crystalTick % 60 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 1.0f);
                }
            }

            // Chain shatter: starts at tick 321
            if (crystalTick >= 240) {
                shatterStarted = true;
                int shatterTick = crystalTick - 240;
                int targetIndex = shatterTick / 5;
                if (targetIndex < crystalPositions.size() && targetIndex > shatterIndex - 1) {
                    shatterIndex = targetIndex + 1;
                    Location shatterPos = crystalPositions.get(targetIndex);
                    DisplayBuilder.dustParticles(shatterPos, 30, 1.0, 150, 100, 255, 1.3f);
                    DisplayBuilder.playSound(shatterPos, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.9f);

                    // Shatter damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(shatterPos) <= 2.25) { // 1.5 blocks
                            p.damage(6.0); // 3 hearts shatter damage
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalLatticeTrap(plugin); }
    }
}
