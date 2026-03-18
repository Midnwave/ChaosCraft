package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GROUP 4: SWEEPING ATTACKS
 * Ten attacks that move horizontally across the arena.
 */
public final class SweepingAttacks {

    private SweepingAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TheScythe(plugin));
        registry.register(new VoidTide(plugin));
        registry.register(new ShadowLance(plugin));
        registry.register(new RollingThunder(plugin));
        registry.register(new InkFlood(plugin));
        registry.register(new PhantomStampede(plugin));
        registry.register(new VoidBroom(plugin));
        registry.register(new OscillatingBlades(plugin));
        registry.register(new TheHungerWall(plugin));
        registry.register(new FractureLine(plugin));
    }

    // =========================================================================
    // ATTACK 31 — The Scythe
    // 20-block crescent void-energy blade sweeps across arena at waist height (2 blocks).
    // =========================================================================
    public static class TheScythe extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double bladeX;
        private double sweepDir;
        private double groundY;
        private boolean spawned = false;

        public TheScythe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_scythe", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(700); // 35s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            bladeX = center.getX() - sweepDir * 22;

            // Warning: edge flash
            for (int z = -10; z <= 10; z++) {
                DisplayBuilder.purpleDust(new Location(w, bladeX + sweepDir * 2, groundY + 1, center.getZ() + z), 3, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 40) return; // Warning

            // Build crescent on first active tick
            if (!spawned) {
                spawned = true;
                // Crescent shape: curved row of blocks, 2 heights
                for (int z = -10; z <= 10; z++) {
                    double curve = Math.cos(z / 10.0 * Math.PI / 2) * 1.5; // Slight forward arc
                    for (int y = 0; y < 2; y++) {
                        Location blockLoc = new Location(w, bladeX + curve * sweepDir, groundY + y, center.getZ() + z);
                        Material mat = (Math.abs(z) < 5) ? Material.PURPLE_CONCRETE : Material.OBSIDIAN;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(blockLoc, mat);
                        h.scale(0.7f, 1.0f, 0.7f).glow(128, 0, 255).interpolation(1, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Sweep: 4-second crossing = 44 blocks / 80 ticks ≈ 0.55 blocks/tick
            double speed = 0.55;
            bladeX += sweepDir * speed;

            for (int i = 0; i < handles.size(); i++) {
                if (!handles.get(i).entity().isValid()) continue;
                Location loc = handles.get(i).entity().getLocation();
                handles.get(i).entity().teleport(loc.clone().add(sweepDir * speed, 0, 0));

                // Spin blade elements
                BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.35f, -0.5f, -0.35f),
                        new AxisAngle4f((float)(ticksAlive * 0.1 * sweepDir), 0, 1, 0),
                        new Vector3f(0.7f, 1.0f, 0.7f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Damage + knockback for players in blade path
            if (ticksAlive % 2 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = Math.abs(ploc.getX() - bladeX);
                    if (dx < 2.0 && Math.abs(ploc.getZ() - center.getZ()) <= 12 && ploc.getY() < groundY + 3) {
                        triggerImpactDamage(ploc);
                        // Lateral knockback
                        player.setVelocity(player.getVelocity().add(
                                new org.bukkit.util.Vector(sweepDir * 2.5, 0.5, 0)));
                    }
                }
            }

            // Smear particles on ground where blade passed
            if (ticksAlive % 3 == 0) {
                for (int z = -10; z <= 10; z += 4) {
                    DisplayBuilder.purpleDust(new Location(w, bladeX, groundY + 0.2, center.getZ() + z), 2, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheScythe(plugin);
        }
    }

    // =========================================================================
    // ATTACK 32 — Void Tide
    // Invisible 1-block-tall void fog wave — only faint shimmer at leading edge.
    // Damages players walking through it; jumping clears it.
    // =========================================================================
    public static class VoidTide extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double tideX;
        private double sweepDir;
        private double groundY;
        private boolean built = false;

        public VoidTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tide", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts per second
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(400);
            config.setCooldownTicks(900); // 45s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            tideX = center.getX() - sweepDir * 22;

            // Barely-visible warning: one shimmer line
            for (int z = -10; z <= 10; z++) {
                DisplayBuilder.purpleDust(new Location(w, tideX, groundY + 0.5, center.getZ() + z), 1, 0.2);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 30) return;

            if (!built) {
                built = true;
                // Thin flat fog plane (1 block tall, nearly invisible)
                for (int z = -12; z <= 12; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, tideX, groundY + 0.1, center.getZ() + z),
                            Material.BLACK_CONCRETE);
                    h.scale(1.0f, 0.3f, 1.0f).glow(30, 0, 60).interpolation(1, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Sweep
            double speed = 0.35;
            tideX += sweepDir * speed;

            for (BlockDisplayHandle h : handles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(sweepDir * speed, 0, 0));
            }

            // Damage ground-level players (not airborne)
            if (ticksAlive % 20 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    // Only if not airborne (Y close to ground)
                    if (Math.abs(ploc.getY() - groundY) < 1.3) {
                        double dx = Math.abs(ploc.getX() - tideX);
                        if (dx < 1.5 && Math.abs(ploc.getZ() - center.getZ()) <= 12) {
                            player.damage(4.0);
                        }
                    }
                }
            }

            // Very subtle shimmer at leading edge only
            if (ticksAlive % 5 == 0) {
                for (int z = -12; z <= 12; z += 6) {
                    DisplayBuilder.purpleDust(new Location(w, tideX + sweepDir, groundY + 0.5, center.getZ() + z), 1, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidTide(plugin);
        }
    }

    // =========================================================================
    // ATTACK 33 — Shadow Lance
    // Needle-thin 30-block lance fires at chest height, moves faster than sprint.
    // Very short warning (1s). Pierces through structures.
    // =========================================================================
    public static class ShadowLance extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double lanceX;
        private double sweepDir;
        private double groundY;
        private boolean built = false;

        public ShadowLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_lance", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts
            config.setImpactRadius(1.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(500); // 25s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            lanceX = center.getX() - sweepDir * 22;

            // 1-second warning: flash and tone
            DisplayBuilder.purpleDust(new Location(w, lanceX, groundY + 1.0, center.getZ()), 10, 4.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 20) return; // 1-second warning

            if (!built) {
                built = true;
                // Lance: 30 segments in a line, 1 block each
                for (int i = 0; i < 30; i++) {
                    double segX = lanceX - sweepDir * i * 0.5;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, segX, groundY + 1.0, center.getZ()),
                            i % 5 == 0 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
                    h.scale(0.5f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // High speed: 1.8 blocks/tick
            double speed = 1.8;
            lanceX += sweepDir * speed;

            for (BlockDisplayHandle h : handles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(sweepDir * speed, 0, 0));
            }

            // Afterimage trail
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.purpleDust(new Location(w, lanceX - sweepDir * 2, groundY + 1.0, center.getZ()), 4, 0.4);
            }

            // Damage players in lance path (very tight Z = 0 corridor)
            if (ticksAlive % 1 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = Math.abs(ploc.getX() - lanceX);
                    double dz = Math.abs(ploc.getZ() - center.getZ());
                    // Lance passes through all Z in a 3-block band
                    if (dx < 2.0 && dz <= 14 && Math.abs(ploc.getY() - groundY - 1.0) < 1.5) {
                        triggerImpactDamage(ploc);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ShadowLance(plugin);
        }
    }

    // =========================================================================
    // ATTACK 34 — Rolling Thunder
    // Pressure wave at knee height — strong knockback, no direct damage.
    // Players crouching take reduced knockback.
    // =========================================================================
    public static class RollingThunder extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double waveX;
        private double sweepDir;
        private double groundY;
        private boolean built = false;

        public RollingThunder(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rolling_thunder", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0); // No direct damage — knockback only
            config.setDamageRadius(0.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(800); // 40s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            waveX = center.getX() - sweepDir * 22;

            // 3s warning: sub-bass boom proxy (low pitch)
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.2f);
            for (int z = -12; z <= 12; z += 4) {
                DisplayBuilder.purpleDust(new Location(w, waveX + sweepDir * 3, groundY + 0.5, center.getZ() + z), 2, 0.4);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 60) {
                // Screen-shake proxy: periodic ground-ripple particles
                if (ticksAlive % 8 == 0) {
                    double rX = waveX + sweepDir * (ticksAlive / 60.0) * 8;
                    for (int z = -10; z <= 10; z += 3) {
                        DisplayBuilder.crimsonDust(new Location(w, rX, groundY + 0.2, center.getZ() + z), 3, 0.5);
                    }
                }
                return;
            }

            if (!built) {
                built = true;
                // Wave: broad flat shockwave ring (1-block-tall pressure front)
                for (int z = -12; z <= 12; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, waveX, groundY + 0.15, center.getZ() + z), Material.PURPLE_CONCRETE);
                    h.scale(2.0f, 0.4f, 1.0f).glow(100, 0, 180).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 5-second sweep = 44 blocks / 100 ticks ≈ 0.44 blocks/tick
            double speed = 0.44;
            waveX += sweepDir * speed;

            for (BlockDisplayHandle h : handles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(sweepDir * speed, 0, 0));
            }

            // Knockback on contact — no damage
            if (ticksAlive % 3 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = Math.abs(ploc.getX() - waveX);
                    if (dx < 2.5 && Math.abs(ploc.getZ() - center.getZ()) <= 12 && ploc.getY() < groundY + 2) {
                        double kbStrength = player.isSneaking() ? 1.0 : 2.5;
                        player.setVelocity(player.getVelocity().add(
                                new org.bukkit.util.Vector(sweepDir * kbStrength, 0.25, 0)));
                    }
                }
            }

            // Ground ripple particles
            if (ticksAlive % 4 == 0) {
                for (int z = -12; z <= 12; z += 5) {
                    DisplayBuilder.purpleDust(new Location(w, waveX - sweepDir, groundY + 0.1, center.getZ() + z), 3, 0.6);
                }
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new RollingThunder(plugin);
        }
    }

    // =========================================================================
    // ATTACK 35 — Ink Flood
    // Ankle-height black void-ink floods across 60% of arena for 15s.
    // Applies Slowness+Blindness while covered; can stand on pillar bases.
    // =========================================================================
    public static class InkFlood extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double inkReach; // how far from source edge ink has spread
        private double sweepDir;
        private double groundY;
        private boolean receding = false;

        public InkFlood(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ink_flood", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0); // No direct damage
            config.setDamageRadius(0.0);
            config.setDurationTicks(1400); // ~70s total
            config.setCooldownTicks(1800); // 90s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = 1.0;
            inkReach = 0;

            // Warning: edge tiles wet/black
            for (int z = -12; z <= 12; z++) {
                DisplayBuilder.purpleDust(new Location(w,
                        center.getX() - 18, groundY + 0.1, center.getZ() + z), 2, 0.4);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 4-second warning before flood (80 ticks)
            if (ticksAlive < 80) {
                if (ticksAlive % 10 == 0) {
                    double px = center.getX() - 18 + ticksAlive * 0.15;
                    for (int z = -12; z <= 12; z += 4) {
                        DisplayBuilder.purpleDust(new Location(w, px, groundY + 0.2, center.getZ() + z), 3, 0.6);
                    }
                }
                return;
            }

            // Active flood (advance 1 block per 5 ticks, up to 24 blocks = 60% of 40-block arena)
            if (!receding && ticksAlive % 5 == 0 && inkReach < 24) {
                inkReach += 1;
                // Add a new column of ink blocks at the flood front
                double frontX = center.getX() - 18 + inkReach;
                for (int z = -12; z <= 12; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, frontX, groundY + 0.06, center.getZ() + z), Material.BLACK_CONCRETE);
                    h.scale(1.0f, 0.06f, 1.0f).glow(20, 0, 40).interpolation(2, 8);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Apply slow/darkness to players in ink zone every 20 ticks
            if (ticksAlive % 20 == 0 && !receding) {
                double inkedMinX = center.getX() - 18;
                double inkedMaxX = inkedMinX + inkReach;
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (ploc.getX() >= inkedMinX && ploc.getX() <= inkedMaxX
                            && Math.abs(ploc.getZ() - center.getZ()) <= 12
                            && Math.abs(ploc.getY() - groundY) < 1.5) {
                        // No damage, but ink-spray particle effect
                        DisplayBuilder.purpleDust(ploc, 4, 0.4);
                        DisplayBuilder.crimsonDust(ploc, 2, 0.3);
                    }
                }
            }

            // Begin recession at tick 900 (15s flood)
            if (ticksAlive == 900) {
                receding = true;
            }

            if (receding && ticksAlive % 4 == 0 && !handles.isEmpty()) {
                // Remove blocks from the far end
                int removeCount = Math.min(12, handles.size());
                for (int i = 0; i < removeCount; i++) {
                    BlockDisplayHandle h = handles.remove(handles.size() - 1);
                    if (h.entity().isValid()) {
                        DisplayBuilder.purpleDust(h.entity().getLocation(), 2, 0.4);
                        h.entity().remove();
                    }
                }
            }

            if (ticksAlive % 30 == 0 && !receding) {
                DisplayBuilder.purpleDust(new Location(w, center.getX() - 18 + inkReach / 2, groundY + 0.2, center.getZ()), 8, inkReach / 3.0);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new InkFlood(plugin);
        }
    }

    // =========================================================================
    // ATTACK 36 — Phantom Stampede
    // Herd of ghostly void-animal silhouettes stampede across a 20-block lane.
    // =========================================================================
    public static class PhantomStampede extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        private static class Phantom {
            BlockDisplayHandle body;
            BlockDisplayHandle head;
            double x, y, z;
            double speed;
        }

        private final List<Phantom> phantoms = new ArrayList<>();
        private double sweepDir;
        private double startX;
        private double groundY;
        private boolean built = false;

        public PhantomStampede(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_stampede", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0); // 2 hearts per phantom strike
            config.setImpactRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(1100); // 55s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            startX = center.getX() - sweepDir * 22;

            // Baying sound warning
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 80) {
                // Distant sounds building
                if (ticksAlive % 30 == 0) {
                    DisplayBuilder.purpleDust(new Location(w, startX, groundY + 1, center.getZ()), 8, 5.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.8f + ticksAlive * 0.005f);
                }
                return;
            }

            // Build herd on first active tick
            if (!built) {
                built = true;
                int count = 5 + RNG.nextInt(3);
                for (int i = 0; i < count; i++) {
                    Phantom p = new Phantom();
                    p.x = startX;
                    p.z = center.getZ() + (RNG.nextDouble() - 0.5) * 20;
                    p.y = groundY;
                    p.speed = 0.5 + RNG.nextDouble() * 0.3;

                    // Body
                    p.body = displayBuilder.spawnBlock(new Location(w, p.x, p.y + 0.5, p.z), Material.OBSIDIAN);
                    p.body.scale(1.4f, 0.9f, 0.7f).glow(60, 0, 120).interpolation(1, 0);
                    spawnedEntities.add(p.body.entity());

                    // Head
                    p.head = displayBuilder.spawnBlock(new Location(w, p.x + sweepDir * 0.7, p.y + 1.0, p.z), Material.CRYING_OBSIDIAN);
                    p.head.scale(0.8f, 0.8f, 0.8f).glow(100, 0, 200).interpolation(1, 0);
                    spawnedEntities.add(p.head.entity());

                    phantoms.add(p);
                }
            }

            // Move phantoms
            for (Phantom p : phantoms) {
                if (!p.body.entity().isValid()) continue;
                p.x += sweepDir * p.speed;

                // Bobbing animation
                double bob = Math.sin(ticksAlive * 0.4 + p.z * 0.1) * 0.2;
                Location bodyLoc = new Location(w, p.x, p.y + 0.5 + bob, p.z);
                Location headLoc = new Location(w, p.x + sweepDir * 0.7, p.y + 1.0 + bob, p.z);
                p.body.entity().teleport(bodyLoc);
                p.head.entity().teleport(headLoc);

                // Animate body lean
                BlockDisplay bd = (BlockDisplay) p.body.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.7f, -0.45f, -0.35f),
                        new AxisAngle4f((float)(sweepDir * 0.15f), 0, 0, 1),
                        new Vector3f(1.4f, 0.9f, 0.7f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);

                // Trailing particles
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.purpleDust(bodyLoc, 3, 0.8);
                }

                // Damage players
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(bodyLoc) <= 4) {
                        triggerImpactDamage(player.getLocation());
                        // Push in sweep direction
                        player.setVelocity(player.getVelocity().add(
                                new org.bukkit.util.Vector(sweepDir * 1.8, 0.3, (RNG.nextDouble() - 0.5) * 1.2)));
                    }
                }
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            phantoms.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new PhantomStampede(plugin);
        }
    }

    // =========================================================================
    // ATTACK 37 — Void Broom
    // Invisible sweeping force pushes everything across arena from one side.
    // Only visible as particle debris it kicks up.
    // =========================================================================
    public static class VoidBroom extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double broomX;
        private double sweepDir;
        private double groundY;

        public VoidBroom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_broom", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(1000); // 50s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            broomX = center.getX() - sweepDir * 22;

            // Rising wind particles from origin direction
            for (int z = -12; z <= 12; z += 3) {
                DisplayBuilder.purpleDust(new Location(w, broomX, groundY + 1, center.getZ() + z), 4, 1.0);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 60) return;

            // 6-second sweep (120 ticks over 44 blocks)
            double speed = 0.37;
            broomX += sweepDir * speed;

            // Debris particles kicked up ahead of the force
            if (ticksAlive % 2 == 0) {
                for (int z = -12; z <= 12; z += 4) {
                    double debrisY = groundY + 0.3 + Math.random() * 1.5;
                    DisplayBuilder.purpleDust(new Location(w, broomX + sweepDir, debrisY, center.getZ() + z), 3, 0.5);
                }
            }

            // Cracked ground trail
            if (ticksAlive % 6 == 0) {
                for (int z = -12; z <= 12; z += 6) {
                    DisplayBuilder.crimsonDust(new Location(w, broomX - sweepDir, groundY + 0.15, center.getZ() + z), 2, 0.4);
                }
            }

            // Push all players at the broom's X position
            if (ticksAlive % 3 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = (ploc.getX() - broomX) * sweepDir;
                    if (dx >= 0 && dx < 3 && Math.abs(ploc.getZ() - center.getZ()) <= 12) {
                        player.setVelocity(player.getVelocity().add(
                                new org.bukkit.util.Vector(sweepDir * 0.4, 0.05, 0)));
                    }
                }
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidBroom(plugin);
        }
    }

    // =========================================================================
    // ATTACK 38 — Oscillating Blades
    // Two crescent blades sweep from opposite edges, crossing at center.
    // Players at center when both arrive take double damage.
    // =========================================================================
    public static class OscillatingBlades extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> blade1 = new ArrayList<>();
        private final List<BlockDisplayHandle> blade2 = new ArrayList<>();
        private double blade1X;
        private double blade2X;
        private double groundY;
        private boolean built = false;

        public OscillatingBlades(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("oscillating_blades", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts per blade
            config.setImpactRadius(1.8);
            config.setDurationTicks(280);
            config.setCooldownTicks(1200); // 60s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            blade1X = center.getX() - 20;
            blade2X = center.getX() + 20;

            // Both edges flash simultaneously
            for (int z = -10; z <= 10; z++) {
                DisplayBuilder.purpleDust(new Location(w, blade1X, groundY + 1, center.getZ() + z), 2, 0.4);
                DisplayBuilder.purpleDust(new Location(w, blade2X, groundY + 1, center.getZ() + z), 2, 0.4);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 40) return;

            if (!built) {
                built = true;
                // Blade 1 (from left): purple
                for (int z = -10; z <= 10; z++) {
                    double curve = Math.cos(z / 10.0 * Math.PI / 2) * 2.0;
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                new Location(w, blade1X + curve, groundY + y, center.getZ() + z),
                                Material.PURPLE_CONCRETE);
                        h.scale(0.6f, 1.0f, 0.6f).glow(128, 0, 255).interpolation(1, 0);
                        blade1.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // Blade 2 (from right): cyan
                for (int z = -10; z <= 10; z++) {
                    double curve = -Math.cos(z / 10.0 * Math.PI / 2) * 2.0;
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                new Location(w, blade2X + curve, groundY + y, center.getZ() + z),
                                Material.CYAN_CONCRETE);
                        h.scale(0.6f, 1.0f, 0.6f).glow(0, 200, 255).interpolation(1, 0);
                        blade2.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Moderate speed: ~0.45 blocks/tick
            double speed = 0.45;
            blade1X += speed;
            blade2X -= speed;

            for (BlockDisplayHandle h : blade1) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(speed, 0, 0));
                BlockDisplay bd = (BlockDisplay) h.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.3f, -0.5f, -0.3f),
                        new AxisAngle4f((float)(ticksAlive * 0.08), 0, 1, 0),
                        new Vector3f(0.6f, 1.0f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }
            for (BlockDisplayHandle h : blade2) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().subtract(speed, 0, 0));
                BlockDisplay bd = (BlockDisplay) h.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.3f, -0.5f, -0.3f),
                        new AxisAngle4f((float)(-ticksAlive * 0.08), 0, 1, 0),
                        new Vector3f(0.6f, 1.0f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Particle trails
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.purpleDust(new Location(w, blade1X, groundY + 1, center.getZ()), 4, 5.0);
                DisplayBuilder.cyanDust(new Location(w, blade2X, groundY + 1, center.getZ()), 4, 5.0);
            }

            // Damage players
            if (ticksAlive % 2 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    boolean hitBlade1 = Math.abs(ploc.getX() - blade1X) < 2.0
                            && Math.abs(ploc.getZ() - center.getZ()) <= 10
                            && ploc.getY() < groundY + 3;
                    boolean hitBlade2 = Math.abs(ploc.getX() - blade2X) < 2.0
                            && Math.abs(ploc.getZ() - center.getZ()) <= 10
                            && ploc.getY() < groundY + 3;

                    if (hitBlade1 || hitBlade2) {
                        triggerImpactDamage(ploc);
                        if (hitBlade1 && hitBlade2) {
                            // Double hit
                            player.damage(8.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            blade1.clear();
            blade2.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new OscillatingBlades(plugin);
        }
    }

    // =========================================================================
    // ATTACK 39 — The Hunger Wall
    // 8-block-tall wall of dense black void fog advances slowly across arena.
    // Phantoms inside strike every 2s. Must be outrun — takes 20s to cross.
    // =========================================================================
    public static class TheHungerWall extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double wallX;
        private double sweepDir;
        private double groundY;
        private boolean built = false;

        public TheHungerWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_hunger_wall", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts per phantom strike
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(40); // Every 2s
            config.setDurationTicks(1600); // ~80s (20s to cross + dissipation)
            config.setCooldownTicks(2400); // 120s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            wallX = center.getX() - sweepDir * 22;

            // Whisper warning 8s before arriving
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.2f);
            for (int y = 0; y < 8; y++) {
                for (int z = -12; z <= 12; z += 6) {
                    DisplayBuilder.purpleDust(new Location(w, wallX, groundY + y, center.getZ() + z), 3, 1.0);
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 160) {
                // Warning: whispers intensify
                if (ticksAlive % 20 == 0) {
                    for (int y = 0; y < 8; y += 2) {
                        DisplayBuilder.purpleDust(new Location(w, wallX, groundY + y, center.getZ()), 6, 8.0);
                    }
                }
                return;
            }

            if (!built) {
                built = true;
                // Solid wall: 8 blocks tall, full arena width (25 blocks)
                for (int y = 0; y < 8; y++) {
                    for (int z = -12; z <= 12; z++) {
                        Material mat = (y < 2) ? Material.OBSIDIAN :
                                (y < 5) ? Material.PURPLE_CONCRETE : Material.BLACK_CONCRETE;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                new Location(w, wallX, groundY + y, center.getZ() + z), mat);
                        float alpha = (y < 2) ? 0.9f : (y < 5 ? 0.7f : 0.5f);
                        h.scale(1.0f, 1.0f, 1.0f).glow(30 + y * 8, 0, 60 + y * 10).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Half walk speed: ~0.22 blocks/tick = 20s to cross 44 blocks
            double speed = 0.22;
            wallX += sweepDir * speed;

            for (BlockDisplayHandle h : handles) {
                if (!h.entity().isValid()) continue;
                h.entity().teleport(h.entity().getLocation().clone().add(sweepDir * speed, 0, 0));
            }

            // Phantom shapes inside wall
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 3; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 2;
                    double oz = (RNG.nextDouble() - 0.5) * 24;
                    double oy = 1 + RNG.nextDouble() * 5;
                    DisplayBuilder.purpleDust(new Location(w, wallX + ox, groundY + oy, center.getZ() + oz), 5, 0.8);
                }
            }

            // Damage players inside wall (handled by base class interval damage)
            // But we also apply phantom strike visual when they're engulfed
            if (ticksAlive % 40 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = (ploc.getX() - wallX) * sweepDir;
                    if (dx >= 0 && dx < 4 && Math.abs(ploc.getZ() - center.getZ()) <= 12) {
                        DisplayBuilder.crimsonDust(ploc, 6, 1.0);
                    }
                }
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheHungerWall(plugin);
        }
    }

    // =========================================================================
    // ATTACK 40 — Fracture Line
    // Nearly invisible void-energy line sweeps silently. Leaves glowing scar cracks.
    // =========================================================================
    public static class FractureLine extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double lineX;
        private double sweepDir;
        private double groundY;
        private boolean built = false;
        private final List<Location> scarLocations = new ArrayList<>();

        public FractureLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fracture_line", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts direct hit
            config.setImpactRadius(1.0);
            config.setDurationTicks(700); // ~35s (10s scars)
            config.setCooldownTicks(1400); // 70s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            lineX = center.getX() - sweepDir * 22;

            // Barely-there warning: one whisper flash
            DisplayBuilder.purpleDust(new Location(w, lineX, groundY + 1, center.getZ()), 3, 8.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 20) return; // 1 second warning only

            if (!built) {
                built = true;
                // The distortion line: functionally invisible — thin nearly-transparent blocks
                for (int z = -12; z <= 12; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, lineX, groundY + 1, center.getZ() + z), Material.TINTED_GLASS);
                    h.scale(0.05f, 1.0f, 0.9f).glow(80, 0, 160).interpolation(1, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // High speed sweep: 1.2 blocks/tick (instantaneous-feeling)
            double speed = 1.2;
            lineX += sweepDir * speed;

            for (BlockDisplayHandle h : handles) {
                if (!h.entity().isValid()) continue;
                h.entity().teleport(h.entity().getLocation().clone().add(sweepDir * speed, 0, 0));
            }

            // Leave fracture scars — glowing crack blocks at ground
            if (ticksAlive % 2 == 0) {
                for (int z = -12; z <= 12; z += 4) {
                    Location scarLoc = new Location(w, lineX - sweepDir * speed, groundY + 0.05, center.getZ() + z);
                    BlockDisplayHandle scar = displayBuilder.spawnBlock(scarLoc, Material.CRYING_OBSIDIAN);
                    scar.scale(0.9f, 0.08f, 0.15f).glow(128, 0, 255).interpolation(1, 0);
                    handles.add(scar);
                    spawnedEntities.add(scar.entity());
                    scarLocations.add(scarLoc);
                }
            }

            // Damage players on direct hit
            if (ticksAlive % 1 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = Math.abs(ploc.getX() - lineX);
                    if (dx < 1.5 && Math.abs(ploc.getZ() - center.getZ()) <= 12) {
                        triggerImpactDamage(ploc);
                    }
                }
            }

            // Scar damage: 1 heart/s on fracture marks
            if (ticksAlive % 20 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    for (Location scar : scarLocations) {
                        if (ploc.distanceSquared(scar) <= 2.25) {
                            player.damage(2.0);
                            DisplayBuilder.purpleDust(scar, 3, 0.5);
                            break;
                        }
                    }
                }
            }

            // Scars fade over time
            if (ticksAlive > 200 && ticksAlive % 6 == 0 && !scarLocations.isEmpty()) {
                scarLocations.remove(0);
            }

            // Line pulse
            if (ticksAlive % 3 == 0 && !handles.isEmpty()) {
                DisplayBuilder.purpleDust(new Location(w, lineX, groundY + 0.5, center.getZ()), 2, 12.0);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            scarLocations.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new FractureLine(plugin);
        }
    }
}
