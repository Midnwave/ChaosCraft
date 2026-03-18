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
 * GROUP 1: SKY FALLS
 * Ten attacks that descend or fall from above.
 */
public final class SkyFalls {

    private SkyFalls() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidSpireDrop(plugin));
        registry.register(new BlackRain(plugin));
        registry.register(new FallingEye(plugin));
        registry.register(new ObsidianHail(plugin));
        registry.register(new VoidAnchorDrop(plugin));
        registry.register(new StarCollapse(plugin));
        registry.register(new TheDrape(plugin));
        registry.register(new ShatteredMirror(plugin));
        registry.register(new GravityWell(plugin));
        registry.register(new VoidMeteorShower(plugin));
    }

    // =========================================================================
    // ATTACK 1 — Void Spire Drop
    // A jagged obsidian spike materializes 30 blocks up, plummets, shatters on impact.
    // Warning beam of particles 2s before impact. 4-heart direct hit, 3-block splash.
    // =========================================================================
    public static class VoidSpireDrop extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double spireY;
        private boolean impacted = false;
        private double groundY;
        private int warningTicksLeft = 40; // 2s warning

        public VoidSpireDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_spire_drop", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);  // 4 hearts
            config.setImpactRadius(3.0);
            config.setDurationTicks(300); // 15s (10s linger)
            config.setCooldownTicks(400); // ~20s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            spireY = groundY + 30.0;

            // Spawn the spire 30 blocks up: 5 stacked obsidian blocks
            for (int i = 0; i < 5; i++) {
                Location blockLoc = center.clone().add(0, spireY + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(blockLoc, Material.OBSIDIAN);
                h.scale(0.8f, 1.0f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 30, 0),
                    Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning phase: emit purple beam particles below
            if (ticksAlive < warningTicksLeft) {
                if (ticksAlive % 3 == 0) {
                    for (int y = 0; y <= 6; y++) {
                        DisplayBuilder.purpleDust(center.clone().add(0, y, 0), 2, 0.3);
                    }
                }
                return;
            }

            if (impacted) {
                // Post-impact: linger effect
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center, 4, 1.5);
                    DisplayBuilder.crimsonDust(center, 2, 1.2);
                }
                return;
            }

            // Fall phase: drop spire downward
            double fallRate = 1.2;
            spireY -= fallRate;

            for (int i = 0; i < handles.size(); i++) {
                Location newLoc = center.clone().add(0, spireY + i, 0);
                handles.get(i).entity().teleport(newLoc);
                // Spin while falling
                BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.4f, -0.5f, -0.4f),
                        new AxisAngle4f(ticksAlive * 0.12f, 0, 1, 0),
                        new Vector3f(0.8f, 1.0f, 0.8f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Emit falling trail
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, spireY, 0), 6, 0.8);
            }

            // Impact check
            if (spireY <= groundY + 0.5) {
                impacted = true;
                triggerImpactDamage(center);
                // Shatter burst
                DisplayBuilder.purpleDust(center, 25, 3.5);
                DisplayBuilder.crimsonDust(center, 15, 2.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.5f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
                // Hide spire pieces
                for (BlockDisplayHandle h : handles) {
                    h.scale(0f, 0f, 0f).interpolation(0, 3);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidSpireDrop(plugin);
        }
    }

    // =========================================================================
    // ATTACK 2 — Black Rain
    // Sky darkens; black void droplets fall across arena for 8s.
    // Periodic damage every 30 ticks (1.5s) while under the rain.
    // =========================================================================
    public static class BlackRain extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public BlackRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_rain", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(1.0); // 0.5 hearts per tick interval
            config.setDamageRadius(20.0); // Arena-wide
            config.setTicksBetweenDamage(30); // Every 1.5s
            config.setDurationTicks(220); // ~11s (3s warning + 8s rain)
            config.setCooldownTicks(1200); // 60s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning phase (0-60 ticks = 3s): dim sky effect via dark particles at height
            if (ticksAlive < 60) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double ox = (RNG.nextDouble() - 0.5) * 20;
                        double oz = (RNG.nextDouble() - 0.5) * 20;
                        DisplayBuilder.purpleDust(center.clone().add(ox, 15, oz), 3, 1.0);
                    }
                }
                return;
            }

            // Rain phase: spawn falling droplets every 3 ticks
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 18;
                    double oz = (RNG.nextDouble() - 0.5) * 18;
                    Location dropStart = center.clone().add(ox, 12, oz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(dropStart, Material.BLACK_CONCRETE);
                    h.scale(0.2f, 0.4f, 0.2f).glow(100, 0, 200).interpolation(1, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Move all droplets downward and remove when below ground
            handles.removeIf(h -> {
                if (!h.entity().isValid()) return true;
                Location loc = h.entity().getLocation();
                if (loc.getY() < center.getY() - 1) {
                    DisplayBuilder.purpleDust(loc, 3, 0.5);
                    h.entity().remove();
                    return true;
                }
                h.entity().teleport(loc.subtract(0, 0.6, 0));
                return false;
            });

            // Ambient sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new BlackRain(plugin);
        }
    }

    // =========================================================================
    // ATTACK 3 — Falling Eye
    // A massive disembodied eye materializes overhead, stares, then fires 8 beams.
    // Tracks a player (the "target"). One of the ~5% tracking attacks.
    // =========================================================================
    public static class FallingEye extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int beamsFired = 0;

        public FallingEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_eye", AttackType.ENVIRONMENTAL, 1));
            config.setTracksPlayer(true);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(2.5);
            config.setDurationTicks(200); // ~10s
            config.setCooldownTicks(900); // 45s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Eye core: large end_stone sphere using concentric blocks
            Location eyeLoc = center.clone().add(0, 22, 0);

            // Eye center (white crystal for "iris")
            BlockDisplayHandle iris = displayBuilder.spawnBlock(eyeLoc, Material.END_STONE);
            iris.scale(1.8f, 1.8f, 1.8f).glow(200, 200, 255).interpolation(2, 10);
            handles.add(iris);
            spawnedEntities.add(iris.entity());

            // Pupil (obsidian, smaller)
            BlockDisplayHandle pupil = displayBuilder.spawnBlock(eyeLoc, Material.OBSIDIAN);
            pupil.scale(0.9f, 0.9f, 0.9f).glow(30, 0, 80).interpolation(2, 10);
            handles.add(pupil);
            spawnedEntities.add(pupil.entity());

            // Converging warning particles
            for (int i = 0; i < 12; i++) {
                double ox = (Math.random() - 0.5) * 10;
                double oz = (Math.random() - 0.5) * 10;
                DisplayBuilder.purpleDust(center.clone().add(ox, 2 + Math.random() * 6, oz), 4, 0.8);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location eyeLoc = center.clone().add(0, 22, 0);

            // Stare phase (0-40 ticks = 2s): pulse the eye
            if (ticksAlive < 40) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.purpleDust(eyeLoc, 8, 2.0);
                }
                // Animate iris pulse
                if (!handles.isEmpty()) {
                    BlockDisplay bd = (BlockDisplay) handles.get(0).entity();
                    float pulse = 1.8f + (float) Math.sin(ticksAlive * 0.3) * 0.3f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -pulse / 2, -pulse / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, pulse, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                return;
            }

            // Beam phase: fire 8 radial beams downward (one every 3 ticks)
            if (ticksAlive >= 40 && beamsFired < 8) {
                if ((ticksAlive - 40) % 3 == 0) {
                    double angle = (beamsFired / 8.0) * 2 * Math.PI;
                    double bx = Math.cos(angle) * 8;
                    double bz = Math.sin(angle) * 8;
                    Location beamTarget = center.clone().add(bx, 0, bz);
                    // Beam of particles from eye to ground
                    for (int step = 0; step <= 20; step++) {
                        double t = step / 20.0;
                        Location beamPoint = eyeLoc.clone().add(bx * t, -22 * t, bz * t);
                        DisplayBuilder.purpleDust(beamPoint, 3, 0.3);
                    }
                    triggerImpactDamage(beamTarget);
                    DisplayBuilder.playSound(beamTarget, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.4f);
                    beamsFired++;
                }
            }

            // Eye collapse after all beams fired
            if (ticksAlive > 64 && ticksAlive % 5 == 0) {
                DisplayBuilder.purpleDust(eyeLoc, 10, 3.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new FallingEye(plugin);
        }
    }

    // =========================================================================
    // ATTACK 4 — Obsidian Hail
    // Dense obsidian shards rain down in a 10-block lane that sweeps across arena.
    // =========================================================================
    public static class ObsidianHail extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double laneOffset = -12.0; // Starts at one edge, sweeps to other

        public ObsidianHail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_hail", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(1.0); // 0.5 hearts per interval
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(240); // ~12s (2s warning + ~6s sweep + linger)
            config.setCooldownTicks(800); // 40s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: sparks at the incoming edge
            for (int z = -5; z <= 5; z++) {
                DisplayBuilder.purpleDust(center.clone().add(-14, 2, z), 3, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40) {
                // Warning phase: advance sparks
                if (ticksAlive % 5 == 0) {
                    for (int z = -5; z <= 5; z++) {
                        DisplayBuilder.purpleDust(center.clone().add(-14 + ticksAlive * 0.3, 3, z), 2, 0.4);
                    }
                }
                return;
            }

            // Sweep the lane across the arena (speed: ~0.5 blocks per tick over 48 ticks)
            double sweepProgress = (ticksAlive - 40) / 4.8;
            laneOffset = -12.0 + sweepProgress;

            if (laneOffset > 14.0) return; // Done sweeping

            // Spawn hail shards in the lane
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double oz = (RNG.nextDouble() - 0.5) * 10;
                    double fallHeight = 10 + RNG.nextDouble() * 5;
                    Location shardLoc = center.clone().add(laneOffset + (RNG.nextDouble() - 0.5) * 2, fallHeight, oz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(shardLoc, Material.OBSIDIAN);
                    h.scale(0.25f, 0.5f, 0.25f).glow(128, 0, 255).interpolation(1, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Move shards down, remove on ground
            handles.removeIf(h -> {
                if (!h.entity().isValid()) return true;
                Location loc = h.entity().getLocation();
                if (loc.getY() <= center.getY() + 0.5) {
                    DisplayBuilder.purpleDust(loc, 2, 0.4);
                    h.entity().remove();
                    return true;
                }
                // Spin while falling
                BlockDisplay bd = (BlockDisplay) h.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.125f, -0.25f, -0.125f),
                        new AxisAngle4f((float)(ticksAlive * 0.18f), 1, 1, 0),
                        new Vector3f(0.25f, 0.5f, 0.25f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(1);
                h.entity().teleport(loc.subtract(0, 0.55, 0));
                return false;
            });

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center.clone().add(laneOffset, 0, 0),
                        Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ObsidianHail(plugin);
        }
    }

    // =========================================================================
    // ATTACK 5 — Void Anchor Drop
    // Massive black anchor-shape drops from above onto arena center.
    // Forces players to flee; heavy impact damage.
    // =========================================================================
    public static class VoidAnchorDrop extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double anchorY;
        private boolean impacted = false;
        private double groundY;

        public VoidAnchorDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_anchor_drop", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0); // 8 hearts
            config.setImpactRadius(5.0);
            config.setDurationTicks(600); // 30s total
            config.setCooldownTicks(1800); // 90s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            anchorY = groundY + 35.0;

            // Anchor crown (top bar) — 5 wide
            for (int x = -2; x <= 2; x++) {
                Location l = center.clone().add(x, anchorY + 4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(l, Material.CRYING_OBSIDIAN);
                h.scale(1f, 1f, 1f).glow(80, 0, 180).interpolation(2, 10);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Anchor shaft — 4 tall
            for (int y = 0; y < 4; y++) {
                Location l = center.clone().add(0, anchorY + y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(l, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 1f, 0.8f).glow(80, 0, 180).interpolation(2, 10);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Anchor flukes (bottom spread)
            for (int x : new int[]{-2, -1, 1, 2}) {
                Location l = center.clone().add(x, anchorY - 1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(l, Material.CRYING_OBSIDIAN);
                h.scale(0.9f, 1.3f, 0.9f).glow(80, 0, 180).interpolation(2, 10);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Warning fissure particles at center
            for (int r = 0; r < 20; r++) {
                double angle = r / 20.0 * 2 * Math.PI;
                DisplayBuilder.crimsonDust(center.clone().add(Math.cos(angle) * 2, 0.1, Math.sin(angle) * 2), 3, 0.4);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (impacted) {
                // Dissolve effect: pulse purple/crimson, then shrink
                if (ticksAlive < 360) {
                    if (ticksAlive % 8 == 0) {
                        DisplayBuilder.purpleDust(center, 8, 3.0);
                        DisplayBuilder.crimsonDust(center, 4, 2.0);
                    }
                    // Post-impact slowness-zone visual
                    if (ticksAlive % 20 == 0) {
                        for (int r = 0; r < 12; r++) {
                            double angle = r / 12.0 * 2 * Math.PI;
                            DisplayBuilder.purpleDust(center.clone().add(Math.cos(angle) * 5, 0.2, Math.sin(angle) * 5), 2, 0.3);
                        }
                    }
                }
                return;
            }

            // Warning phase (0-60 ticks = 3s)
            if (ticksAlive < 60) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.2, 0), 6, 2.0);
                }
                return;
            }

            // Fall: drop 0.8 blocks per tick
            anchorY -= 0.8;

            for (int i = 0; i < handles.size(); i++) {
                BlockDisplayHandle h = handles.get(i);
                if (!h.entity().isValid()) continue;
                Location orig = h.entity().getLocation();
                double dy = anchorY - groundY - 35.0;
                h.entity().teleport(orig.clone().add(0, -0.8, 0));
            }

            // Fall particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, anchorY, 0), 8, 2.0);
                DisplayBuilder.crimsonDust(center.clone().add(0, anchorY, 0), 4, 1.5);
            }

            // Impact
            if (anchorY <= groundY + 3) {
                impacted = true;
                triggerImpactDamage(center);
                DisplayBuilder.purpleDust(center, 40, 6.0);
                DisplayBuilder.crimsonDust(center, 25, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidAnchorDrop(plugin);
        }
    }

    // =========================================================================
    // ATTACK 6 — Star Collapse
    // A sky star inverts and rains down 20-30 end-crystal-like shards randomly.
    // =========================================================================
    public static class StarCollapse extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int shardsSpawned = 0;

        public StarCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_collapse", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts
            config.setImpactRadius(1.5);
            config.setDurationTicks(280); // ~14s
            config.setCooldownTicks(1100); // 55s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Flash warning: converging upward particles
            for (int i = 0; i < 20; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 16;
                double oz = (RNG.nextDouble() - 0.5) * 16;
                DisplayBuilder.cyanDust(center.clone().add(ox, 0.5, oz), 3, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Barrage window: ticks 40 - 140 (5 seconds, every 4 ticks spawn a shard)
            if (ticksAlive >= 40 && ticksAlive <= 140 && ticksAlive % 4 == 0 && shardsSpawned < 25) {
                double ox = (RNG.nextDouble() - 0.5) * 18;
                double oz = (RNG.nextDouble() - 0.5) * 18;
                double startY = 25 + RNG.nextDouble() * 8;
                Location shardLoc = center.clone().add(ox, startY, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(shardLoc, Material.AMETHYST_BLOCK);
                h.scale(0.6f, 1.0f, 0.6f).glow(180, 100, 255).interpolation(1, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
                shardsSpawned++;
            }

            // Move shards, impact on landing
            handles.removeIf(h -> {
                if (!h.entity().isValid()) return true;
                Location loc = h.entity().getLocation();
                if (loc.getY() <= center.getY() + 0.5) {
                    triggerImpactDamage(loc);
                    DisplayBuilder.purpleDust(loc, 10, 1.5);
                    DisplayBuilder.cyanDust(loc, 5, 1.0);
                    DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.5f);
                    h.entity().remove();
                    return true;
                }
                BlockDisplay bd = (BlockDisplay) h.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.3f, -0.5f, -0.3f),
                        new AxisAngle4f(ticksAlive * 0.2f, 1, 1, 1),
                        new Vector3f(0.6f, 1.0f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(1);
                h.entity().teleport(loc.subtract(0, 0.9, 0));
                return false;
            });

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 12, 4.0);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new StarCollapse(plugin);
        }
    }

    // =========================================================================
    // ATTACK 7 — The Drape
    // A massive black void membrane drifts down slowly, covering half the arena.
    // Players under it when it lands take damage.
    // =========================================================================
    public static class TheDrape extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double drapeY;
        private boolean landed = false;
        private double groundY;

        public TheDrape(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_drape", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts
            config.setImpactRadius(8.0); // Half-arena coverage
            config.setDurationTicks(440); // ~22s
            config.setCooldownTicks(1400); // 70s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            drapeY = groundY + 30.0;

            // Create a wide flat sheet of black_concrete blocks representing the membrane
            for (int x = -6; x <= 6; x++) {
                for (int z = -4; z <= 4; z++) {
                    Location l = center.clone().add(x, drapeY, z);
                    Material mat = (Math.abs(x) + Math.abs(z)) % 3 == 0 ? Material.OBSIDIAN : Material.BLACK_CONCRETE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(l, mat);
                    h.scale(1.0f, 0.1f, 1.0f).glow(60, 0, 120).interpolation(2, 20);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 80) {
                // Shadow warning: particles below, growing
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, groundY + 0.2, 0), 8, 5.0);
                }
                return;
            }

            if (landed) {
                // Ground contact phase: pulse dark particles
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center, 12, 6.0);
                }
                return;
            }

            // Descend: 8 seconds to fall 30 blocks = ~0.19 blocks/tick
            drapeY -= 0.19;

            for (BlockDisplayHandle h : handles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(0, -0.19, 0));
                // Gentle undulation
                BlockDisplay bd = (BlockDisplay) h.entity();
                float wave = 0.1f + (float) Math.abs(Math.sin(ticksAlive * 0.05 + loc.getX() * 0.3)) * 0.05f;
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -wave / 2, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.0f, wave, 1.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(5);
            }

            if (drapeY <= groundY + 1.0) {
                landed = true;
                triggerImpactDamage(center);
                DisplayBuilder.purpleDust(center, 30, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheDrape(plugin);
        }
    }

    // =========================================================================
    // ATTACK 8 — Shattered Mirror
    // Sky cracks like glass; 8-12 large shards land upright as obstacles.
    // Players who walk into them take contact damage.
    // =========================================================================
    public static class ShatteredMirror extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int shardsLanded = 0;
        private static final int TOTAL_SHARDS = 10;

        public ShatteredMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shattered_mirror", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // 1 heart per interval (shard contact)
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(420); // ~21s (3s warning + 15s obstacle persist + dissolve)
            config.setCooldownTicks(1000); // 50s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crack-line warning particles
            for (int i = -8; i <= 8; i++) {
                DisplayBuilder.purpleDust(center.clone().add(i, 25, 0), 2, 0.5);
                DisplayBuilder.purpleDust(center.clone().add(0, 25, i), 2, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spawn shards during ticks 60-120 (one every 6 ticks)
            if (ticksAlive >= 60 && shardsLanded < TOTAL_SHARDS && ticksAlive % 6 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 16;
                double oz = (RNG.nextDouble() - 0.5) * 16;
                double startY = center.getY() + 20;
                Location shardBase = center.clone().add(ox, startY, oz);

                // Shard: tall thin obsidian pillar (1x4)
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(shardBase.clone().add(0, y, 0), Material.BLACK_STAINED_GLASS);
                    float tilt = (RNG.nextFloat() - 0.5f) * 0.4f;
                    h.scale(0.35f, 1.0f, 0.2f).glow(150, 50, 255).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
                shardsLanded++;
                DisplayBuilder.playSound(shardBase, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 1.5f);
            }

            // Move falling shards down until they hit ground
            // (Only move shards that are still above ground — those in upper Y range)
            for (BlockDisplayHandle h : handles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                if (loc.getY() > center.getY() + 4) {
                    h.entity().teleport(loc.subtract(0, 0.7, 0));
                }
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 22, 0), 10, 5.0);
            }

            // Dissolve phase near end
            if (ticksAlive > 380) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center, 6, 5.0);
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
            return new ShatteredMirror(plugin);
        }
    }

    // =========================================================================
    // ATTACK 9 — Gravity Well
    // Small sphere of darkness 20 blocks up pulls players inward, then flings outward.
    // =========================================================================
    public static class GravityWell extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean collapsed = false;

        public GravityWell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_well", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts on fling
            config.setImpactRadius(12.0); // All players within 12 blocks
            config.setDurationTicks(260); // ~13s
            config.setCooldownTicks(1300); // 65s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location wellLoc = center.clone().add(0, 20, 0);

            // Core sphere: obsidian center with end stone shell
            BlockDisplayHandle core = displayBuilder.spawnBlock(wellLoc, Material.OBSIDIAN);
            core.scale(1.4f, 1.4f, 1.4f).glow(30, 0, 80).interpolation(2, 10);
            handles.add(core);
            spawnedEntities.add(core.entity());

            // Outer glow shell
            BlockDisplayHandle shell = displayBuilder.spawnBlock(wellLoc, Material.CRYING_OBSIDIAN);
            shell.scale(2.2f, 2.2f, 2.2f).glow(128, 0, 255).interpolation(2, 15);
            handles.add(shell);
            spawnedEntities.add(shell.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location wellLoc = center.clone().add(0, 20, 0);

            if (collapsed) {
                // Post-collapse: outward burst particles
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center, 12, 6.0);
                    DisplayBuilder.cyanDust(center, 6, 4.0);
                }
                return;
            }

            // Pull phase (0-60 ticks = 3s): visually suck particles inward
            if (ticksAlive < 60) {
                if (ticksAlive % 3 == 0) {
                    // Spiral particles converging toward well
                    double angle = ticksAlive * 0.5;
                    double r = 10 - ticksAlive * 0.15;
                    DisplayBuilder.purpleDust(wellLoc.clone().add(
                            Math.cos(angle) * r, -ticksAlive * 0.1, Math.sin(angle) * r), 4, 0.6);
                }

                // Animate well sphere growing (pull building up)
                if (!handles.isEmpty()) {
                    BlockDisplay bd = (BlockDisplay) handles.get(1).entity();
                    float growScale = 2.2f + (ticksAlive / 60f) * 1.2f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-growScale / 2, -growScale / 2, -growScale / 2),
                            new AxisAngle4f(ticksAlive * 0.08f, 0, 1, 0),
                            new Vector3f(growScale, growScale, growScale),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }

                // Apply pull to nearby players
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        Location ploc = player.getLocation();
                        if (ploc.distanceSquared(center) <= 144) { // 12 blocks
                            org.bukkit.util.Vector pullDir = center.toVector().subtract(ploc.toVector()).normalize().multiply(0.15);
                            player.setVelocity(player.getVelocity().add(pullDir));
                        }
                    }
                }
                return;
            }

            // Collapse + fling (tick 60)
            if (!collapsed) {
                collapsed = true;
                // Fling all players outward
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        Location ploc = player.getLocation();
                        if (ploc.distanceSquared(center) <= 144) {
                            org.bukkit.util.Vector flingDir = ploc.toVector().subtract(center.toVector()).normalize().multiply(2.0).add(new org.bukkit.util.Vector(0, 0.6, 0));
                            player.setVelocity(flingDir);
                        }
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.purpleDust(wellLoc, 30, 5.0);
                DisplayBuilder.cyanDust(wellLoc, 15, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.3f);

                // Hide well sphere
                for (BlockDisplayHandle h : handles) {
                    h.scale(0f, 0f, 0f).interpolation(0, 5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new GravityWell(plugin);
        }
    }

    // =========================================================================
    // ATTACK 10 — Void Meteor Shower
    // Cluster of 5 void meteors falls at a steep angle, each bouncing once on impact.
    // =========================================================================
    public static class VoidMeteorShower extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        private static class Meteor {
            BlockDisplayHandle handle;
            double x, y, z;
            double vx, vy, vz;
            boolean bounced = false;
            boolean done = false;
        }

        private final List<Meteor> meteors = new ArrayList<>();
        private double groundY;

        public VoidMeteorShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_meteor_shower", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts first impact
            config.setImpactRadius(3.0);
            config.setDurationTicks(500); // ~25s (scorch lingers)
            config.setCooldownTicks(1600); // 80s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();

            // Warning: streak trails from north-east high above
            for (int i = 0; i < 5; i++) {
                double angle = Math.PI / 4 + (RNG.nextDouble() - 0.5) * 0.6;
                DisplayBuilder.purpleDust(center.clone().add(
                        Math.cos(angle) * 20, 30, Math.sin(angle) * 20), 5, 1.0);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 1.6f);

            // Spawn 5 meteors
            for (int i = 0; i < 5; i++) {
                double spreadX = (RNG.nextDouble() - 0.5) * 10;
                double spreadZ = (RNG.nextDouble() - 0.5) * 10;

                Meteor m = new Meteor();
                m.x = center.getX() + spreadX + 20;
                m.y = center.getY() + 28;
                m.z = center.getZ() + spreadZ + 20;
                m.vx = -1.1 + (RNG.nextDouble() - 0.5) * 0.3;
                m.vy = -0.9;
                m.vz = -1.1 + (RNG.nextDouble() - 0.5) * 0.3;

                Location startLoc = new Location(w, m.x, m.y, m.z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(startLoc, Material.CRYING_OBSIDIAN);
                h.scale(1.2f, 1.0f, 1.2f).glow(150, 0, 80).interpolation(1, 0);
                m.handle = h;
                meteors.add(m);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            boolean anyActive = false;

            for (Meteor m : meteors) {
                if (m.done || !m.handle.entity().isValid()) continue;
                anyActive = true;

                m.x += m.vx;
                m.y += m.vy;
                m.z += m.vz;

                Location meteorLoc = new Location(w, m.x, m.y, m.z);
                m.handle.entity().teleport(meteorLoc);

                // Spin the meteor
                BlockDisplay bd = (BlockDisplay) m.handle.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.6f, -0.5f, -0.6f),
                        new AxisAngle4f(ticksAlive * 0.25f, 1, 1, 0),
                        new Vector3f(1.2f, 1.0f, 1.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(1);

                // Trailing fire particles
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.crimsonDust(meteorLoc, 5, 0.8);
                    DisplayBuilder.purpleDust(meteorLoc.clone().add(-m.vx * 2, -m.vy * 2, -m.vz * 2), 3, 0.5);
                }

                // First impact
                if (!m.bounced && m.y <= groundY + 0.8) {
                    m.bounced = true;
                    triggerImpactDamage(meteorLoc);
                    DisplayBuilder.purpleDust(meteorLoc, 20, 4.0);
                    DisplayBuilder.crimsonDust(meteorLoc, 12, 3.0);
                    DisplayBuilder.playSound(meteorLoc, Sound.BLOCK_STONE_PLACE, 2.0f, 0.3f);

                    // Bounce: reverse Y, reduce speed
                    m.vy = 0.5;
                    m.vx *= 0.4;
                    m.vz *= 0.4;

                    // Scorch mark: small dark block at impact
                    BlockDisplayHandle scorch = displayBuilder.spawnBlock(meteorLoc, Material.BLACKSTONE);
                    scorch.scale(2.0f, 0.1f, 2.0f).glow(80, 0, 0).interpolation(1, 0);
                    spawnedEntities.add(scorch.entity());
                }

                // Second (bounce) impact
                if (m.bounced && m.vy > 0) {
                    m.vy -= 0.06; // gravity
                }
                if (m.bounced && m.y <= groundY + 0.8 && m.vy <= 0) {
                    // Landed after bounce — small secondary hit
                    if (w != null) {
                        for (org.bukkit.entity.Player player : w.getPlayers()) {
                            if (isExempt(player)) continue;
                            if (player.getLocation().distanceSquared(meteorLoc) <= 9) {
                                player.damage(4.0); // 2 hearts bounce hit
                            }
                        }
                    }
                    DisplayBuilder.purpleDust(meteorLoc, 8, 2.0);
                    m.handle.entity().remove();
                    m.done = true;
                }
            }
        }

        @Override
        protected void onCleanup() {
            meteors.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidMeteorShower(plugin);
        }
    }
}
