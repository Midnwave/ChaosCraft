package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * GROUP 7: RIFT STORM EVENTS (Attacks 61-70)
 * Phase 2 (50% HP) — DoG's phasing ability bleeds into the environment.
 * Rifts that teleport, divide, cascade, compress, rain from the sky,
 * and eventually form a dimensional storm and a sweeping God Rift beam.
 *
 * Palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * Materials: AMETHYST_BLOCK, DARK_PRISMARINE, SEA_LANTERN, END_ROD, CYAN_STAINED_GLASS
 */
public final class RiftStorm {

    private RiftStorm() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new RiftStormOnset(plugin));
        registry.register(new PhaseRift(plugin));
        registry.register(new RiftShotgun(plugin));
        registry.register(new DoGEchoRift(plugin));
        registry.register(new RiftWall(plugin));
        registry.register(new CascadingRifts(plugin));
        registry.register(new RiftCompression(plugin));
        registry.register(new RiftRain(plugin));
        registry.register(new DimensionalStorm(plugin));
        registry.register(new GodRift(plugin));
    }

    // =========================================================================
    // 61. RIFT STORM ONSET
    // Twelve rifts open simultaneously — six at ground level, six at Y+5.
    // Portal particle ovals with white fringing. Touching a rift teleports
    // the player to a random other rift position. Pure positional chaos.
    // No direct damage, 60s cooldown.
    // =========================================================================
    public static class RiftStormOnset extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> riftPositions = new ArrayList<>();
        private boolean riftsActive = false;

        public RiftStormOnset(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_storm_onset", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 ground rifts + 6 elevated rifts
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double dist = 6.0 + Math.random() * 10.0;
                double yOff = (i < 6) ? 0 : 5.0;
                Location riftLoc = center.clone().add(
                        Math.cos(angle) * dist, yOff, Math.sin(angle) * dist);
                riftPositions.add(riftLoc);
                // Rift portal block — tall thin cyan glass
                BlockDisplayHandle h = displayBuilder.spawnBlock(riftLoc, Material.CYAN_STAINED_GLASS);
                h.scale(1.5f, 2.5f, 0.15f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(0, 200, 255)
                 .interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: cascade of teleport sounds over 20 ticks
            if (ticksAlive < 20) {
                if (ticksAlive % 2 == 0) {
                    int idx = ticksAlive / 2;
                    if (idx < riftPositions.size()) {
                        DisplayBuilder.playSound(riftPositions.get(idx),
                                Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
                    }
                }
                return;
            }

            riftsActive = true;

            // Portal particle effects on each rift
            if (ticksAlive % 5 == 0) {
                for (Location rift : riftPositions) {
                    World w = rift.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.PORTAL, rift.clone().add(0, 1.2, 0),
                                8, 0.4, 1.0, 0.4, 0.1);
                    }
                    DisplayBuilder.dustParticles(rift.clone().add(0, 1.2, 0),
                            4, 0.6, 240, 240, 255, 1.0f);
                }
            }

            // Teleport players who touch rifts
            if (ticksAlive % 10 == 0 && riftsActive) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (int i = 0; i < riftPositions.size(); i++) {
                        Location rift = riftPositions.get(i);
                        if (p.getLocation().distanceSquared(rift) <= 4.0) {
                            // Teleport to a random different rift
                            int target = (int)(Math.random() * riftPositions.size());
                            while (target == i && riftPositions.size() > 1) {
                                target = (int)(Math.random() * riftPositions.size());
                            }
                            Location dest = riftPositions.get(target).clone().add(0, 0.5, 0);
                            p.teleport(dest);
                            DisplayBuilder.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                            DisplayBuilder.cyanDust(dest, 10, 1.0);
                            break;
                        }
                    }
                }
            }

            // Ambient rift hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftStormOnset(plugin); }
    }

    // =========================================================================
    // 62. PHASE RIFT
    // A single massive rift — 4 blocks tall, 2 wide. Dense white END_ROD
    // curtain. Acts as a DoG exit marker: when DoG phases through the island,
    // it exits from here. Players within 3 blocks take damage on exit.
    // 12 HP when DoG exits, 50s cooldown.
    // =========================================================================
    public static class PhaseRift extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location riftLocation;
        private boolean exitPulsed = false;

        public PhaseRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_rift", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * 2 * Math.PI;
            double dist = 8.0 + Math.random() * 6.0;
            riftLocation = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            // Massive rift frame — stacked dark prismarine
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 2; x++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            riftLocation.clone().add(x - 0.5, y, 0), Material.DARK_PRISMARINE);
                    h.scale(1.0f, 1.0f, 0.2f).glow(0, 200, 255).interpolation(3, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Inner glow — sea lantern
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle inner = displayBuilder.spawnBlock(
                        riftLocation.clone().add(0, y + 0.5, 0), Material.SEA_LANTERN);
                inner.scale(1.2f, 0.8f, 0.1f).glow(240, 240, 255).interpolation(3, 0);
                handles.add(inner);
                spawnedEntities.add(inner.entity());
            }
            DisplayBuilder.playSound(riftLocation, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning phase: 60 ticks
            if (ticksAlive < 60) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(riftLocation.clone().add(0, 2, 0),
                            12, 1.5, 240, 240, 255, 1.4f);
                    DisplayBuilder.playSound(riftLocation, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.3f);
                }
                return;
            }

            // Dense END_ROD curtain particles
            if (ticksAlive % 3 == 0) {
                World w = riftLocation.getWorld();
                if (w != null) {
                    for (int i = 0; i < 6; i++) {
                        double y = Math.random() * 4.0;
                        double x = (Math.random() - 0.5) * 2.0;
                        w.spawnParticle(Particle.END_ROD,
                                riftLocation.clone().add(x, y, 0), 2, 0.2, 0.3, 0.1, 0.01);
                    }
                }
            }

            // Periodic exit pulse — simulates DoG emerging
            if (ticksAlive % 100 == 0 && ticksAlive > 60) {
                DisplayBuilder.playSound(riftLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                DisplayBuilder.dustParticles(riftLocation.clone().add(0, 2, 0),
                        30, 3.0, 240, 240, 255, 2.0f);
                DisplayBuilder.cyanDust(riftLocation.clone().add(0, 2, 0), 20, 2.5);
                // Damage nearby players
                World w = riftLocation.getWorld();
                if (w != null) {
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(riftLocation) <= 9.0) {
                            p.damage(config.getDamage());
                        }
                    }
                }
            }

            // Ambient portal hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(riftLocation, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseRift(plugin); }
    }

    // =========================================================================
    // 63. RIFT SHOTGUN
    // Five rifts open in a tight spread pattern aimed at the nearest player
    // cluster — covering a 10-block arc. Each rift teleports intersecting
    // players 15-20 blocks away. Nearly impossible for groups to avoid all 5.
    // No direct damage (repositioning danger), 35s cooldown.
    // =========================================================================
    public static class RiftShotgun extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> riftPositions = new ArrayList<>();
        private boolean fired = false;

        public RiftShotgun(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_shotgun", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(700);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spread 5 rifts in a 10-block arc toward center
            double baseAngle = Math.random() * 2 * Math.PI;
            double dist = 6.0;
            for (int i = -2; i <= 2; i++) {
                double angle = baseAngle + i * 0.25; // ~15 degree spacing
                Location riftLoc = center.clone().add(
                        Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                riftPositions.add(riftLoc);
                BlockDisplayHandle h = displayBuilder.spawnBlock(riftLoc, Material.CYAN_STAINED_GLASS);
                h.scale(1.0f, 2.0f, 0.12f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(0, 200, 255)
                 .interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Brief ambient warning
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 16 ticks (~0.8 seconds)
            if (ticksAlive < 16) {
                if (ticksAlive % 4 == 0) {
                    for (Location rift : riftPositions) {
                        DisplayBuilder.cyanDust(rift.clone().add(0, 1, 0), 6, 0.5);
                    }
                }
                return;
            }

            // Active for 20 ticks (1 second window)
            if (!fired) {
                fired = true;
                // Portal particles on each rift
                for (Location rift : riftPositions) {
                    World w = rift.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.PORTAL, rift.clone().add(0, 1, 0),
                                15, 0.3, 0.8, 0.3, 0.2);
                    }
                    DisplayBuilder.dustParticles(rift.clone().add(0, 1, 0),
                            8, 0.8, 240, 240, 255, 1.2f);
                }
            }

            // Teleport check
            if (ticksAlive >= 16 && ticksAlive < 36 && ticksAlive % 5 == 0) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (Location rift : riftPositions) {
                        if (p.getLocation().distanceSquared(rift) <= 4.0) {
                            // Teleport 15-20 blocks away in random direction
                            double tpAngle = Math.random() * 2 * Math.PI;
                            double tpDist = 15.0 + Math.random() * 5.0;
                            Location dest = p.getLocation().clone().add(
                                    Math.cos(tpAngle) * tpDist, 0, Math.sin(tpAngle) * tpDist);
                            // Clamp to island bounds
                            dest.setY(center.getY());
                            p.teleport(dest);
                            DisplayBuilder.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                            DisplayBuilder.cyanDust(dest, 12, 1.0);
                            break;
                        }
                    }
                }
            }

            // Rift particle fade
            if (ticksAlive > 36 && ticksAlive % 6 == 0) {
                for (Location rift : riftPositions) {
                    DisplayBuilder.cyanDust(rift.clone().add(0, 1, 0), 3, 0.4);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftShotgun(plugin); }
    }

    // =========================================================================
    // 64. DoG ECHO RIFT
    // A rift opens revealing a ghost image of DoG — white, translucent, wrong
    // proportions. After 3 seconds the ghost fires a copy of the current boss
    // attack from the rift position as a secondary attack vector.
    // 12 HP minimum (mirrors current boss attack), 55s cooldown.
    // =========================================================================
    public static class DoGEchoRift extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location riftLocation;
        private boolean attackFired = false;

        public DoGEchoRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_echo_rift", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(1100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * 2 * Math.PI;
            double dist = 8.0 + Math.random() * 8.0;
            riftLocation = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            // Rift frame
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftLocation.clone().add(0, y, 0), Material.DARK_PRISMARINE);
                h.scale(2.0f, 1.0f, 0.15f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Ghost DoG silhouette — segmented amethyst blocks suggesting a worm shape
            for (int i = 0; i < 5; i++) {
                Location segLoc = riftLocation.clone().add(
                        Math.sin(i * 0.6) * 0.4, 1.0 + Math.cos(i * 0.8) * 0.5, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                seg.scale(0.5f, 0.5f, 0.5f).glow(240, 240, 255).interpolation(3, 0);
                handles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.playSound(riftLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ghost visibility phase: 60 ticks (3 seconds) — ghost visible through rift
            if (ticksAlive < 60) {
                // Ghost pulsing
                if (ticksAlive % 4 == 0) {
                    World w = riftLocation.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.END_ROD,
                                riftLocation.clone().add(0, 1.5, 0), 4, 0.5, 0.8, 0.5, 0.01);
                    }
                    DisplayBuilder.dustParticles(riftLocation.clone().add(0, 1.5, 0),
                            6, 1.0, 240, 240, 255, 1.2f);
                }
                // Ghost sway animation
                if (ticksAlive % 8 == 0) {
                    for (int i = 5; i < handles.size() && i < 10; i++) {
                        BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                        float sway = (float)Math.sin(ticksAlive * 0.15) * 0.3f;
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.25f + sway, -0.25f, -0.25f),
                                new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                                new Vector3f(0.5f, 0.5f, 0.5f),
                                new AxisAngle4f(0, 0, 1, 0)));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(4);
                    }
                }
                return;
            }

            // Fire the echo attack at tick 60
            if (!attackFired) {
                attackFired = true;
                DisplayBuilder.playSound(riftLocation, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.8f);
                DisplayBuilder.dustParticles(riftLocation.clone().add(0, 1.5, 0),
                        40, 4.0, 240, 240, 255, 1.8f);
                DisplayBuilder.cyanDust(riftLocation.clone().add(0, 1.5, 0), 20, 3.5);
                // Damage burst from rift
                World w = riftLocation.getWorld();
                if (w != null) {
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(riftLocation) <= 25.0) {
                            p.damage(config.getDamage());
                        }
                    }
                }
            }

            // Post-attack rift closing
            if (ticksAlive > 60 && ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(riftLocation.clone().add(0, 1, 0), 4, 0.8);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoGEchoRift(plugin); }
    }

    // =========================================================================
    // 65. RIFT WALL
    // A line of 8 rifts divides the island in half. Continuous curtain of
    // portal and END_ROD particles. Passing through teleports you to the
    // other side. Splits teams and isolates players.
    // No direct damage (teleport danger), 65s cooldown.
    // =========================================================================
    public static class RiftWall extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> wallPositions = new ArrayList<>();
        private double wallAngle;

        public RiftWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_wall", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(1300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            wallAngle = Math.random() * Math.PI;
            // 8 rift columns in a line across the island
            for (int i = -4; i < 4; i++) {
                double offset = i * 3.5;
                Location loc = center.clone().add(
                        Math.cos(wallAngle) * offset, 0,
                        Math.sin(wallAngle) * offset);
                wallPositions.add(loc);
                // Tall rift column
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            loc.clone().add(0, y, 0), Material.CYAN_STAINED_GLASS);
                    h.scale(0.8f, 1.0f, 0.15f)
                     .rotate((float)(wallAngle + Math.PI / 2), 0, 1, 0)
                     .glow(0, 200, 255)
                     .interpolation(3, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks — dust trace on ground
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    for (Location pos : wallPositions) {
                        DisplayBuilder.dustParticles(pos, 5, 0.5, 240, 240, 255, 1.0f);
                    }
                }
                return;
            }

            // Wall particle curtain
            if (ticksAlive % 4 == 0) {
                for (Location pos : wallPositions) {
                    World w = pos.getWorld();
                    if (w != null) {
                        for (int y = 0; y < 3; y++) {
                            w.spawnParticle(Particle.PORTAL,
                                    pos.clone().add(0, y + 0.5, 0), 4, 0.2, 0.4, 0.2, 0.1);
                            w.spawnParticle(Particle.END_ROD,
                                    pos.clone().add(0, y + 0.5, 0), 2, 0.2, 0.4, 0.2, 0.01);
                        }
                    }
                }
            }

            // Teleport players who cross the wall
            if (ticksAlive % 8 == 0 && ticksAlive >= 40) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (Location pos : wallPositions) {
                        if (p.getLocation().distanceSquared(pos) <= 2.25) {
                            // Teleport to opposite side of wall
                            double perpAngle = wallAngle + Math.PI / 2;
                            Location dest = p.getLocation().clone().add(
                                    Math.cos(perpAngle) * 6, 0,
                                    Math.sin(perpAngle) * 6);
                            dest.setY(center.getY());
                            p.teleport(dest);
                            DisplayBuilder.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                            DisplayBuilder.cyanDust(dest, 10, 1.0);
                            break;
                        }
                    }
                }
            }

            // Ambient wall hum
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftWall(plugin); }
    }

    // =========================================================================
    // 66. CASCADING RIFTS
    // A rift opens, and from its exit another rift opens 2 seconds later,
    // chaining across the island. Creates 6 rifts over 12 seconds.
    // Players at rift exit points when they open take dimensional pressure.
    // 4 HP if standing at exit point, 60s cooldown.
    // =========================================================================
    public static class CascadingRifts extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> riftChain = new ArrayList<>();
        private int riftsOpened = 0;

        public CascadingRifts(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cascading_rifts", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // First rift position
            double angle = Math.random() * 2 * Math.PI;
            double dist = 5.0 + Math.random() * 6.0;
            Location first = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            riftChain.add(first);
            openRift(first);
            DisplayBuilder.playSound(first, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
        }

        private void openRift(Location loc) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
            h.scale(1.2f, 2.0f, 0.12f).glow(0, 200, 255).interpolation(3, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
            // Top frame block
            BlockDisplayHandle top = displayBuilder.spawnBlock(loc.clone().add(0, 2, 0),
                    Material.DARK_PRISMARINE);
            top.scale(1.4f, 0.2f, 0.2f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(top);
            spawnedEntities.add(top.entity());
            riftsOpened++;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Open new rift every 40 ticks (2 seconds) at a random offset from the previous
            if (ticksAlive % 40 == 0 && riftsOpened < 6) {
                Location prev = riftChain.get(riftChain.size() - 1);
                double angle = Math.random() * 2 * Math.PI;
                double dist = 6.0 + Math.random() * 5.0;
                Location next = prev.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                riftChain.add(next);
                openRift(next);
                DisplayBuilder.playSound(next, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
                DisplayBuilder.cyanDust(next, 12, 1.5);

                // Dimensional pressure damage to players at the new rift location
                World w = next.getWorld();
                if (w != null) {
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(next) <= 6.25) {
                            p.damage(config.getDamage());
                        }
                    }
                }
            }

            // Cascade particles connecting rifts
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < riftChain.size() - 1; i++) {
                    DisplayBuilder.particleLine(riftChain.get(i).clone().add(0, 1, 0),
                            riftChain.get(i + 1).clone().add(0, 1, 0),
                            Particle.ELECTRIC_SPARK, 1, null);
                }
            }

            // Portal particles on active rifts
            if (ticksAlive % 5 == 0) {
                for (Location rift : riftChain) {
                    World w = rift.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.PORTAL, rift.clone().add(0, 1, 0),
                                6, 0.3, 0.8, 0.3, 0.1);
                    }
                }
            }

            // Ambient rift sounds
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CascadingRifts(plugin); }
    }

    // =========================================================================
    // 67. RIFT COMPRESSION
    // Two rifts on opposite sides slowly move inward toward each other at
    // 1 block per 2 seconds. When they meet, both detonate in a white burst.
    // Players in the path are teleported through. Detonation deals damage.
    // 14 HP from detonation in 4-block radius, 65s cooldown.
    // =========================================================================
    public static class RiftCompression extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location riftA;
        private Location riftB;
        private final List<BlockDisplayHandle> riftAHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> riftBHandles = new ArrayList<>();
        private boolean detonated = false;

        public RiftCompression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_compression", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(14.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(1300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI;
            double dist = 14.0;
            riftA = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            riftB = center.clone().add(Math.cos(angle + Math.PI) * dist, 0,
                    Math.sin(angle + Math.PI) * dist);

            spawnRiftPair(riftA, riftAHandles, angle);
            spawnRiftPair(riftB, riftBHandles, angle + Math.PI);
            DisplayBuilder.playSound(riftA, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);
            DisplayBuilder.playSound(riftB, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);
        }

        private void spawnRiftPair(Location loc, List<BlockDisplayHandle> riftHandles, double angle) {
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        loc.clone().add(0, y, 0), Material.CYAN_STAINED_GLASS);
                h.scale(1.5f, 1.0f, 0.12f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(0, 200, 255)
                 .interpolation(2, 0);
                riftHandles.add(h);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (detonated) {
                // Post-detonation sparks
                if (ticksAlive % 10 == 0) {
                    Location mid = riftA.clone().add(riftB).multiply(0.5);
                    DisplayBuilder.dustParticles(mid, 6, 2.0, 240, 240, 255, 1.0f);
                }
                return;
            }

            // Move rifts toward each other: 1 block per 40 ticks (1 block per 2 seconds)
            double dx = riftB.getX() - riftA.getX();
            double dz = riftB.getZ() - riftA.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > 2.0) {
                double moveStep = 0.025; // per tick
                double nx = dx / dist;
                double nz = dz / dist;
                riftA.add(nx * moveStep, 0, nz * moveStep);
                riftB.add(-nx * moveStep, 0, -nz * moveStep);

                // Teleport rift display entities
                for (int y = 0; y < riftAHandles.size(); y++) {
                    riftAHandles.get(y).entity().teleport(riftA.clone().add(0, y, 0));
                }
                for (int y = 0; y < riftBHandles.size(); y++) {
                    riftBHandles.get(y).entity().teleport(riftB.clone().add(0, y, 0));
                }
            } else {
                // Rifts have met — detonate
                detonated = true;
                Location meetPoint = riftA.clone().add(riftB).multiply(0.5);
                DisplayBuilder.playSound(meetPoint, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                DisplayBuilder.dustParticles(meetPoint, 50, 4.0, 240, 240, 255, 2.0f);
                DisplayBuilder.cyanDust(meetPoint, 30, 3.5);
                World w = meetPoint.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, meetPoint.clone().add(0, 1.5, 0),
                            40, 3, 2, 3, 0.05);
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(meetPoint) <= 16.0) {
                            p.damage(config.getDamage());
                        }
                    }
                }
            }

            // Portal particle trails
            if (ticksAlive % 5 == 0) {
                World w = riftA.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.PORTAL, riftA.clone().add(0, 1.5, 0),
                            6, 0.4, 0.8, 0.4, 0.1);
                    w.spawnParticle(Particle.PORTAL, riftB.clone().add(0, 1.5, 0),
                            6, 0.4, 0.8, 0.4, 0.1);
                }
            }

            // Teleport players in path
            if (ticksAlive % 10 == 0) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(riftA) <= 4.0) {
                        p.teleport(riftB.clone().add(0, 0.5, 0));
                        DisplayBuilder.playSound(riftB, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
                        break;
                    }
                    if (p.getLocation().distanceSquared(riftB) <= 4.0) {
                        p.teleport(riftA.clone().add(0, 0.5, 0));
                        DisplayBuilder.playSound(riftA, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
                        break;
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftCompression(plugin); }
    }

    // =========================================================================
    // 68. RIFT RAIN
    // Small horizontal rift ovals fall from Y+60, drifting downward. 15-20
    // rifts descend over 8 seconds. Players hit mid-air by a falling rift
    // are teleported to a random position — fall damage risk.
    // No direct damage (fall damage risk), 50s cooldown.
    // =========================================================================
    public static class RiftRain extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> fallingRifts = new ArrayList<>();
        private int spawned = 0;

        public RiftRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_rain", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 60 ticks
            if (ticksAlive < 60) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 30, 0),
                            10, 8.0, 0, 200, 255, 1.0f);
                }
                return;
            }

            // Spawn falling rift discs — 1 every 8 ticks for 160 ticks
            if (ticksAlive % 8 == 0 && spawned < 18) {
                spawned++;
                double ox = (Math.random() - 0.5) * 24;
                double oz = (Math.random() - 0.5) * 24;
                Location riftLoc = center.clone().add(ox, 60, oz);
                fallingRifts.add(riftLoc);
                BlockDisplayHandle h = displayBuilder.spawnBlock(riftLoc, Material.CYAN_STAINED_GLASS);
                h.scale(0.8f, 0.1f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Move all rifts downward
            for (int i = fallingRifts.size() - 1; i >= 0; i--) {
                Location rift = fallingRifts.get(i);
                rift.subtract(0, 0.4, 0); // Fall speed
                if (i < handles.size()) {
                    handles.get(i).entity().teleport(rift);
                }

                // Portal particles on each falling rift
                if (ticksAlive % 6 == 0) {
                    World w = rift.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.PORTAL, rift, 3, 0.3, 0.1, 0.3, 0.05);
                    }
                }

                // Remove if below ground
                if (rift.getY() < center.getY() - 2) {
                    if (i < handles.size()) {
                        handles.get(i).entity().remove();
                    }
                    fallingRifts.remove(i);
                    if (i < handles.size()) handles.remove(i);
                }
            }

            // Teleport check — players near any falling rift
            if (ticksAlive % 6 == 0) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (Location rift : fallingRifts) {
                        if (p.getLocation().distanceSquared(rift) <= 2.25) {
                            double tpAngle = Math.random() * 2 * Math.PI;
                            double tpDist = 8.0 + Math.random() * 10.0;
                            Location dest = center.clone().add(
                                    Math.cos(tpAngle) * tpDist, 5 + Math.random() * 10,
                                    Math.sin(tpAngle) * tpDist);
                            p.teleport(dest);
                            DisplayBuilder.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
                            DisplayBuilder.cyanDust(dest, 8, 0.8);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftRain(plugin); }
    }

    // =========================================================================
    // 69. DIMENSIONAL STORM
    // Peak rift density: 8 ground rifts, 4 mid-air, cascading particle trails
    // cover 60% of the island. All rift teleports loop — every rift sends to
    // the next in sequence. DoG targets rift positions instead of players for
    // 4 seconds (brief boss relief during chaos).
    // 4 HP per rift transit, 90s cooldown, fires at most once per Phase 2.
    // =========================================================================
    public static class DimensionalStorm extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> stormRifts = new ArrayList<>();

        public DimensionalStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_storm", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 8 ground rifts
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double dist = 5.0 + Math.random() * 10.0;
                Location loc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                stormRifts.add(loc);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(1.0f, 2.0f, 0.12f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(0, 200, 255)
                 .interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // 4 mid-air rifts
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4 + 0.4;
                double dist = 6.0 + Math.random() * 8.0;
                Location loc = center.clone().add(
                        Math.cos(angle) * dist, 4 + Math.random() * 3, Math.sin(angle) * dist);
                stormRifts.add(loc);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.8f, 1.5f, 0.1f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(128, 0, 255)
                 .interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dense portal particle coverage
            if (ticksAlive % 3 == 0) {
                for (Location rift : stormRifts) {
                    World w = rift.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.PORTAL, rift.clone().add(0, 1, 0),
                                8, 0.5, 1.0, 0.5, 0.15);
                        w.spawnParticle(Particle.REVERSE_PORTAL, rift.clone().add(0, 1, 0),
                                4, 0.3, 0.6, 0.3, 0.1);
                    }
                }
            }

            // Cascade particle trails between rifts
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < stormRifts.size() - 1; i++) {
                    DisplayBuilder.particleLine(stormRifts.get(i).clone().add(0, 1, 0),
                            stormRifts.get(i + 1).clone().add(0, 1, 0),
                            Particle.ELECTRIC_SPARK, 1, null);
                }
            }

            // Teleport loop — each rift sends to next in sequence
            if (ticksAlive % 8 == 0) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (int i = 0; i < stormRifts.size(); i++) {
                        if (p.getLocation().distanceSquared(stormRifts.get(i)) <= 3.0) {
                            int next = (i + 1) % stormRifts.size();
                            Location dest = stormRifts.get(next).clone().add(0, 0.5, 0);
                            p.teleport(dest);
                            p.damage(config.getDamage());
                            DisplayBuilder.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
                            DisplayBuilder.cyanDust(dest, 10, 0.8);
                            break;
                        }
                    }
                }
            }

            // Storm ambient
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);
                DisplayBuilder.dustParticles(center, 15, 10.0, 0, 200, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalStorm(plugin); }
    }

    // =========================================================================
    // 70. GOD RIFT
    // A single massive rift — 6 blocks tall, 4 wide — emitting pure white
    // light with END_ROD streams flowing outward. A nether star rotates at
    // its center. Fires a white beam laser that sweeps 45 degrees over 6
    // seconds, dealing devastating damage on contact.
    // 10 HP per second of beam contact, 80s cooldown.
    // =========================================================================
    public static class GodRift extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location riftCenter;
        private double beamAngle;
        private double beamStartAngle;

        public GodRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("god_rift", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(10.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(1600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * 2 * Math.PI;
            double dist = 4.0 + Math.random() * 6.0;
            riftCenter = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            beamStartAngle = Math.random() * 2 * Math.PI;
            beamAngle = beamStartAngle;

            // Massive rift frame — 6 tall, 4 wide
            for (int y = 0; y < 6; y++) {
                for (int x = -1; x <= 1; x++) {
                    Material mat = (y >= 1 && y <= 4 && x == 0)
                            ? Material.SEA_LANTERN : Material.DARK_PRISMARINE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            riftCenter.clone().add(x * 1.3, y, 0), mat);
                    h.scale(1.3f, 1.0f, 0.2f).glow(240, 240, 255).interpolation(3, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Nether star at center (represented by glowing amethyst block)
            BlockDisplayHandle star = displayBuilder.spawnBlock(
                    riftCenter.clone().add(0, 3, 0), Material.AMETHYST_BLOCK);
            star.scale(0.6f, 0.6f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
            handles.add(star);
            spawnedEntities.add(star.entity());

            DisplayBuilder.playSound(riftCenter, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 60 ticks (3 seconds)
            if (ticksAlive < 60) {
                if (ticksAlive % 5 == 0) {
                    World w = riftCenter.getWorld();
                    if (w != null) {
                        for (int y = 0; y < 6; y++) {
                            w.spawnParticle(Particle.END_ROD,
                                    riftCenter.clone().add(0, y, 0), 4, 1.0, 0.3, 1.0, 0.02);
                        }
                    }
                    DisplayBuilder.dustParticles(riftCenter.clone().add(0, 3, 0),
                            10, 2.0, 240, 240, 255, 1.6f);
                }
                // Rotate nether star
                if (!handles.isEmpty()) {
                    BlockDisplayHandle star = handles.get(handles.size() - 1);
                    BlockDisplay bd = (BlockDisplay) star.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.3f, -0.3f, -0.3f),
                            new AxisAngle4f(ticksAlive * 0.1f, 0, 1, 0),
                            new Vector3f(0.6f, 0.6f, 0.6f),
                            new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                return;
            }

            // Beam sweep phase: 120 ticks (6 seconds)
            // Sweep 45 degrees = PI/4 radians over 120 ticks
            int beamTick = ticksAlive - 60;
            if (beamTick >= 0 && beamTick < 120) {
                beamAngle = beamStartAngle + (beamTick / 120.0) * (Math.PI / 4);

                // Draw beam across island — 30 blocks long
                Location beamStart = riftCenter.clone().add(0, 3, 0);
                if (ticksAlive % 2 == 0) {
                    for (int step = 0; step <= 30; step++) {
                        Location beamPoint = beamStart.clone().add(
                                Math.cos(beamAngle) * step, 0, Math.sin(beamAngle) * step);
                        World w = beamPoint.getWorld();
                        if (w != null) {
                            w.spawnParticle(Particle.END_ROD, beamPoint, 2, 0.1, 0.1, 0.1, 0.01);
                        }
                        DisplayBuilder.dustParticles(beamPoint, 3, 0.2, 240, 240, 255, 1.5f);
                    }
                }

                // Beam damage — check players along beam line
                if (ticksAlive % 20 == 0) {
                    World w = riftCenter.getWorld();
                    if (w == null) return;
                    Location beamEnd = beamStart.clone().add(
                            Math.cos(beamAngle) * 30, 0, Math.sin(beamAngle) * 30);
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (isNearBeamLine(p.getLocation(), beamStart, beamEnd, 2.5)) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(p.getLocation(), 8, 0.5,
                                    240, 240, 255, 1.5f);
                        }
                    }
                }
            }

            // Continue rotating star
            if (!handles.isEmpty()) {
                BlockDisplayHandle star = handles.get(handles.size() - 1);
                BlockDisplay bd = (BlockDisplay) star.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.3f, -0.3f, -0.3f),
                        new AxisAngle4f(ticksAlive * 0.15f, 0, 1, 0),
                        new Vector3f(0.6f, 0.6f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }

            // Ambient outward END_ROD streams
            if (ticksAlive % 6 == 0) {
                World w = riftCenter.getWorld();
                if (w != null) {
                    for (int i = 0; i < 8; i++) {
                        double a = (2 * Math.PI * i) / 8;
                        Location outward = riftCenter.clone().add(Math.cos(a) * 4, 3, Math.sin(a) * 4);
                        w.spawnParticle(Particle.END_ROD, outward, 2, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }
        }

        private boolean isNearBeamLine(Location p, Location start, Location end, double threshold) {
            double dx = end.getX() - start.getX();
            double dz = end.getZ() - start.getZ();
            double len2 = dx * dx + dz * dz;
            if (len2 < 0.01) return p.distanceSquared(start) <= threshold * threshold;
            double t = Math.max(0, Math.min(1,
                    ((p.getX() - start.getX()) * dx + (p.getZ() - start.getZ()) * dz) / len2));
            double cx = start.getX() + t * dx;
            double cz = start.getZ() + t * dz;
            double dist2 = (p.getX() - cx) * (p.getX() - cx) + (p.getZ() - cz) * (p.getZ() - cz);
            // Also check Y within 3 blocks
            double dy = Math.abs(p.getY() - start.getY());
            return dist2 <= threshold * threshold && dy <= 3.0;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GodRift(plugin); }
    }
}
