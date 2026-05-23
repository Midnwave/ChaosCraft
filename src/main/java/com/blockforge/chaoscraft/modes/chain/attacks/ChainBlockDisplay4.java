package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — BLOCK DISPLAY ATTACKS 31-40 (Orbiting / Spinning)
 *
 * Heavy industrial chain/iron themed orbital and spinning structures.
 * Each attack:
 *  - Spawn formation phase
 *  - Active rotation/orbit phase
 *  - Dissipate phase
 *  - Min 30 block displays
 *  - Always spawn straight (yaw=0, pitch=0)
 *  - Tracks spawnedEntities for cleanup
 *  - Smooth interpolated rotation via teleport + setInterpolationDuration
 *
 * Industrial palette: IRON_BLOCK, NETHERITE_BLOCK, CHAIN, POLISHED_BLACKSTONE,
 *                     ANVIL, HEAVY_WEIGHTED_PRESSURE_PLATE
 *
 * Particles: BLOCK (iron/netherite), CRIT, ELECTRIC_SPARK, SMOKE, LARGE_SMOKE,
 *            SOUL_FIRE_FLAME, EXPLOSION (sparingly), LAVA
 * Sounds:    BLOCK_CHAIN_FALL, BLOCK_CHAIN_HIT, BLOCK_GRINDSTONE_USE,
 *            BLOCK_NETHERITE_BLOCK_HIT, ENTITY_IRON_GOLEM_ATTACK,
 *            BLOCK_PISTON_EXTEND, BLOCK_BEACON_AMBIENT
 *
 * Attacks:
 *  31. MAGNETIC PILLAR          — Pull-toward + orbit (fight pull, find orbit gap)
 *  32. ANCHOR BURST             — 5-point burst (read pentagon, find safe sector)
 *  33. IRON GYROSCOPE           — Triple-axis rotation (read 3D safe corridor)
 *  34. CHAIN MAELSTROM          — Spiral pull (fight inward, sprint outward)
 *  35. CHAIN PINWHEEL           — Rotating blades (timing dodge between blade gaps)
 *  36. ORBITAL WRECKING BALLS   — Multi-orbit constellation (read orbits, thread)
 *  37. IRON HALO DESCENT        — Descending ring (escape before halo lands)
 *  38. CHAIN HELIX TOWER        — Spiraling helix (timing through helix gaps)
 *  39. SPINNING CHAIN WALL      — Coin-flip wall (read which face is incoming)
 *  40. IRON TORNADO             — Vortex zone (sprint perpendicular, find eye)
 */
public final class ChainBlockDisplay4 {
    private ChainBlockDisplay4() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MagneticPillar(plugin));
        registry.register(new AnchorBurst(plugin));
        registry.register(new IronGyroscope(plugin));
        registry.register(new ChainMaelstrom(plugin));
        registry.register(new ChainPinwheel(plugin));
        registry.register(new OrbitalWreckingBalls(plugin));
        registry.register(new IronHaloDescent(plugin));
        registry.register(new ChainHelixTower(plugin));
        registry.register(new SpinningChainWall(plugin));
        registry.register(new IronTornado(plugin));
    }

    // ================================================================
    // Helpers
    // ================================================================

    private static void teleportSmooth(BlockDisplayHandle h, Location to, int interpTicks) {
        BlockDisplay e = h.entity();
        if (e == null || !e.isValid()) return;
        e.setInterpolationDuration(interpTicks);
        e.setInterpolationDelay(0);
        e.teleport(to);
    }

    private static void setRotation(BlockDisplayHandle h, float angle, float ax, float ay, float az, int interpTicks) {
        BlockDisplay e = h.entity();
        if (e == null || !e.isValid()) return;
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(interpTicks);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    private static void setScale(BlockDisplayHandle h, float sx, float sy, float sz, int interpTicks) {
        BlockDisplay e = h.entity();
        if (e == null || !e.isValid()) return;
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(interpTicks);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void shrinkToZero(BlockDisplayHandle h, int duration) {
        setScale(h, 0f, 0f, 0f, duration);
    }

    // ================================================================
    // #31 — MAGNETIC PILLAR ("The Draw")
    // 32 displays: 5 IRON_BLOCK pillar + 4 NETHERITE_BLOCK caps + 6 CHAIN
    //   field beams + 2 IRON_BLOCK flanges + 15 orbital CHAIN rings
    // Pulls nearby players slightly toward center while orbital chains
    // sweep around — must fight pull and find gap.
    // ================================================================
    public static class MagneticPillar extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillar = new ArrayList<>();
        private final List<BlockDisplayHandle> caps = new ArrayList<>();
        private final List<BlockDisplayHandle> beams = new ArrayList<>();
        private final List<BlockDisplayHandle> flanges = new ArrayList<>();
        private final List<BlockDisplayHandle> orbitRing = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public MagneticPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magnetic_pillar", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setChance(6.0);
            config.setEnabled(true);
            config.setDesignType("Pull-toward + orbit (fight pull, find orbit gap)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 5 pillar segments, tapering top
            float[] pillarScales = {0.8f, 0.75f, 0.65f, 0.55f, 0.45f};
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 0.4 + i * 0.9, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(15, i * 2);
                spawnedEntities.add(h.entity());
                pillar.add(h); allBlocks.add(h);
                setScale(h, pillarScales[i], 0.9f, pillarScales[i], 15);
            }

            // 4 NETHERITE caps at pillar top
            double[][] capOffsets = {{0.4, 5.0, 0}, {-0.4, 5.0, 0}, {0, 5.0, 0.4}, {0, 5.0, -0.4}};
            for (double[] off : capOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(60, 60, 80).interpolation(15, 12);
                spawnedEntities.add(h.entity());
                caps.add(h); allBlocks.add(h);
                setScale(h, 0.3f, 0.3f, 0.3f, 15);
            }

            // 6 CHAIN beam slabs (radial)
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2 * i / 6;
                double x = Math.cos(angle) * 1.0;
                double z = Math.sin(angle) * 1.0;
                Location loc = center.clone().add(x, 5.3, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(120, 120, 160).interpolation(15, 15);
                spawnedEntities.add(h.entity());
                beams.add(h); allBlocks.add(h);
                setScale(h, 0.08f, 0.04f, 2.0f, 15);
                setRotation(h, (float) angle, 0f, 1f, 0f, 15);
            }

            // 2 IRON base flanges
            for (int i = 0; i < 2; i++) {
                double xOff = (i == 0) ? -0.8 : 0.8;
                Location loc = center.clone().add(xOff, 0.1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 210).interpolation(15, 5);
                spawnedEntities.add(h.entity());
                flanges.add(h); allBlocks.add(h);
                setScale(h, 1.2f, 0.12f, 0.35f, 15);
            }

            // 15 orbiting CHAIN ring pieces at varying radii
            for (int i = 0; i < 15; i++) {
                double angle = Math.PI * 2 * i / 15;
                double r = 3.2;
                double y = 2.5;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(160, 160, 200).interpolation(15, 18);
                spawnedEntities.add(h.entity());
                orbitRing.add(h); allBlocks.add(h);
                setScale(h, 0.3f, 0.3f, 0.3f, 15);
                setRotation(h, (float) angle, 0f, 1f, 0f, 15);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.8f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.3f, 0.7f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 5, 0), 40, 1.5, 1, 1.5, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Update orbit positions each tick
            double baseAngle = tick * 0.06;
            for (int i = 0; i < orbitRing.size(); i++) {
                double a = baseAngle + Math.PI * 2 * i / orbitRing.size();
                double x = Math.cos(a) * 3.2;
                double z = Math.sin(a) * 3.2;
                double y = 2.5 + Math.sin(tick * 0.05 + i * 0.4) * 0.3;
                Location target = c.clone().add(x, y, z);
                target.setYaw(0); target.setPitch(0);
                teleportSmooth(orbitRing.get(i), target, 2);
                setRotation(orbitRing.get(i), (float) a, 0f, 1f, 0f, 2);
            }

            // Beam direction update toward nearest player every 20 ticks
            if (tick > 0 && tick % 20 == 0) {
                Player nearest = null;
                double best = 12.0 * 12.0;
                for (Player p : c.getWorld().getPlayers()) {
                    if (isExempt(p)) continue;
                    double d = p.getLocation().distanceSquared(c);
                    if (d < best) { best = d; nearest = p; }
                }
                if (nearest != null) {
                    Vector toP = nearest.getLocation().toVector().subtract(c.toVector());
                    double targetAngle = Math.atan2(toP.getZ(), toP.getX());
                    for (int i = 0; i < beams.size(); i++) {
                        double a = targetAngle + Math.PI * 2 * i / 6;
                        setRotation(beams.get(i), (float) a, 0f, 1f, 0f, 6);
                    }
                }
            }

            // Pull-toward velocity on players inside damage radius (after delay)
            if (tick > 25 && tick % 5 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dx = c.getX() - pl.getX();
                    double dz = c.getZ() - pl.getZ();
                    double hd = Math.sqrt(dx * dx + dz * dz);
                    if (hd > 1.5 && hd < 7.0) {
                        double scale = 0.08;
                        Vector pull = new Vector(dx / hd * scale, 0, dz / hd * scale);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }

            // Particles
            if (tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 5.0, 0), 4, 0.3, 0.2, 0.3, 0.1);
            }
            if (tick % 5 == 0) {
                for (BlockDisplayHandle h : orbitRing) {
                    c.getWorld().spawnParticle(Particle.CRIT, h.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.1f, 0.6f);
            }
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.9f, 1.2f);
            }

            // Dissipate
            if (tick >= 220) {
                int dt = tick - 220;
                if (dt == 0) {
                    for (BlockDisplayHandle h : beams) shrinkToZero(h, 8);
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.5f);
                }
                if (dt == 6) for (BlockDisplayHandle h : orbitRing) shrinkToZero(h, 10);
                if (dt == 14) {
                    for (BlockDisplayHandle h : caps) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : pillar) shrinkToZero(h, 8);
                    for (BlockDisplayHandle h : flanges) shrinkToZero(h, 8);
                    DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.6f, 0.4f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MagneticPillar(plugin); }
    }

    // ================================================================
    // #32 — ANCHOR BURST ("The Five Points")
    // 35 displays — 7 per anchor × 5 anchors in pentagon formation.
    // Impact-only — 5 simultaneous bursts in pentagon, find safe sector.
    // ================================================================
    public static class AnchorBurst extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> anchors = new ArrayList<>();
        private final List<Location> anchorCenters = new ArrayList<>();
        private boolean impactDone = false;

        public AnchorBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("anchor_burst", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(220.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(100);
            config.setCooldownTicks(220);
            config.setChance(7.0);
            config.setEnabled(true);
            config.setDesignType("5-point burst (read pentagon, find safe sector)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            double R = 2.2;

            for (int i = 0; i < 5; i++) {
                double a = Math.PI * 2 * i / 5;
                double x = Math.cos(a) * R;
                double z = Math.sin(a) * R;
                Location anchorLoc = center.clone().add(x, 0, z);
                anchorCenters.add(anchorLoc);

                List<BlockDisplayHandle> parts = new ArrayList<>();
                // Ring (2 IRON_BLOCK)
                for (int j = 0; j < 2; j++) {
                    Location loc = anchorLoc.clone().add(0, 1.6 + j * 0.15, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(10, 0);
                    spawnedEntities.add(h.entity()); parts.add(h);
                }
                // Shank (1 IRON_BLOCK)
                {
                    Location loc = anchorLoc.clone().add(0, 0.8, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(10, 0);
                    spawnedEntities.add(h.entity()); parts.add(h);
                }
                // Stock (2 IRON_BLOCK crossbars)
                for (int j = 0; j < 2; j++) {
                    double dx = (j == 0) ? -0.5 : 0.5;
                    Location loc = anchorLoc.clone().add(dx, 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(170, 170, 190).interpolation(10, 0);
                    spawnedEntities.add(h.entity()); parts.add(h);
                }
                // Fluke arms (2)
                for (int j = 0; j < 2; j++) {
                    double dx = (j == 0) ? -0.4 : 0.4;
                    Location loc = anchorLoc.clone().add(dx, 0.15, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(160, 160, 180).interpolation(10, 0);
                    spawnedEntities.add(h.entity()); parts.add(h);
                }
                anchors.add(parts);
            }
            // Initial telegraph
            for (Location ac : anchorCenters) {
                w.spawnParticle(Particle.LARGE_SMOKE, ac.clone().add(0, 0, 0), 12, 0.4, 0.1, 0.4, 0.05);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Eruption: ticks 5-15, anchors rise + scale up
            if (tick == 5) {
                for (int i = 0; i < anchors.size(); i++) {
                    Location anchorLoc = anchorCenters.get(i);
                    List<BlockDisplayHandle> parts = anchors.get(i);
                    // Ring
                    setScale(parts.get(0), 0.35f, 0.18f, 0.35f, 10);
                    setScale(parts.get(1), 0.32f, 0.16f, 0.32f, 10);
                    // Shank
                    setScale(parts.get(2), 0.22f, 0.9f, 0.22f, 10);
                    // Stock crossbars
                    setScale(parts.get(3), 0.55f, 0.18f, 0.18f, 10);
                    setScale(parts.get(4), 0.55f, 0.18f, 0.18f, 10);
                    // Flukes
                    setScale(parts.get(5), 0.32f, 0.18f, 0.42f, 10);
                    setScale(parts.get(6), 0.32f, 0.18f, 0.42f, 10);
                    anchorLoc.getWorld().spawnParticle(Particle.BLOCK, anchorLoc, 30, 0.6, 0.3, 0.6, 0.2, Material.IRON_BLOCK.createBlockData());
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.6f, 0.7f);
            }

            // Impact damage at tick 15 — full extension
            if (tick == 15 && !impactDone) {
                impactDone = true;
                for (Location ac : anchorCenters) {
                    triggerImpactDamage(ac.clone());
                    ac.getWorld().spawnParticle(Particle.LARGE_SMOKE, ac, 25, 0.8, 0.5, 0.8, 0.1);
                    ac.getWorld().spawnParticle(Particle.BLOCK, ac, 25, 0.6, 0.4, 0.6, 0.2, Material.IRON_BLOCK.createBlockData());
                    DisplayBuilder.playSound(ac, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.8f, 0.6f);
                }
            }

            // Floating bob during active phase
            if (tick > 25 && tick % 4 == 0) {
                float bob = (float) (Math.sin(tick * 0.15) * 0.05);
                for (int i = 0; i < anchors.size(); i++) {
                    Location ac = anchorCenters.get(i);
                    Location target = ac.clone().add(0, bob, 0);
                    target.setYaw(0); target.setPitch(0);
                    for (BlockDisplayHandle h : anchors.get(i)) {
                        // We only bob ring and shank for performance
                        BlockDisplay e = h.entity();
                        if (e == null || !e.isValid()) continue;
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(4);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(-0.5f, bob - 0.5f, -0.5f),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Ambient particles + sounds
            if (tick % 6 == 0) {
                for (Location ac : anchorCenters) {
                    ac.getWorld().spawnParticle(Particle.SMOKE, ac.clone().add(0, 0.5, 0), 2, 0.2, 0.1, 0.2, 0.02);
                }
            }
            if (tick > 0 && tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.9f, 0.8f);
            }

            // Dissipate ticks 85-95
            if (tick == 85) {
                for (List<BlockDisplayHandle> parts : anchors) {
                    for (BlockDisplayHandle h : parts) shrinkToZero(h, 10);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.2f, 0.6f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AnchorBurst(plugin); }
    }

    // ================================================================
    // #33 — IRON GYROSCOPE ("The Mechanism")
    // 36 displays: 12 outer Y-ring + 10 mid X-ring + 8 inner Z-ring +
    //              1 hub + 4 chain spokes + 1 extra polish piece
    // 3 independent rotation axes — read 3D safe corridor.
    // ================================================================
    public static class IronGyroscope extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringY = new ArrayList<>();
        private final List<BlockDisplayHandle> ringX = new ArrayList<>();
        private final List<BlockDisplayHandle> ringZ = new ArrayList<>();
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final List<BlockDisplayHandle> spokes = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public IronGyroscope(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_gyroscope", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(300.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setChance(5.0);
            config.setEnabled(true);
            config.setDesignType("Triple-axis rotation (read 3D safe corridor)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            Location core = center.clone().add(0, 2.5, 0);

            // Y-ring outer (12 IRON_BLOCK at r=1.8)
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                double x = Math.cos(a) * 1.8;
                double z = Math.sin(a) * 1.8;
                Location loc = core.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(15, 0);
                spawnedEntities.add(h.entity()); ringY.add(h); allBlocks.add(h);
                setScale(h, 0.55f, 0.1f, 0.18f, 15);
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }
            // X-ring middle (10 at r=1.3, perpendicular - in X-Y plane)
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 10;
                double y = Math.cos(a) * 1.3;
                double z = Math.sin(a) * 1.3;
                Location loc = core.clone().add(0, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(15, 5);
                spawnedEntities.add(h.entity()); ringX.add(h); allBlocks.add(h);
                setScale(h, 0.48f, 0.1f, 0.16f, 15);
                setRotation(h, (float) a, 1f, 0f, 0f, 15);
            }
            // Z-ring inner (8 at r=0.8, X-Y plane other orientation)
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double x = Math.cos(a) * 0.8;
                double y = Math.sin(a) * 0.8;
                Location loc = core.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(160, 160, 180).interpolation(15, 10);
                spawnedEntities.add(h.entity()); ringZ.add(h); allBlocks.add(h);
                setScale(h, 0.38f, 0.1f, 0.14f, 15);
                setRotation(h, (float) a, 0f, 0f, 1f, 15);
            }
            // Hub (NETHERITE)
            BlockDisplayHandle hubCore = displayBuilder.spawnBlock(core.clone(), Material.NETHERITE_BLOCK);
            hubCore.scale(0f, 0f, 0f).glow(40, 40, 60).interpolation(15, 5);
            spawnedEntities.add(hubCore.entity()); hub.add(hubCore); allBlocks.add(hubCore);
            setScale(hubCore, 0.45f, 0.45f, 0.45f, 15);

            // Polish piece (extra NETHERITE small)
            BlockDisplayHandle polish = displayBuilder.spawnBlock(core.clone().add(0, 0.5, 0), Material.NETHERITE_BLOCK);
            polish.scale(0f, 0f, 0f).glow(80, 80, 110).interpolation(15, 6);
            spawnedEntities.add(polish.entity()); hub.add(polish); allBlocks.add(polish);
            setScale(polish, 0.2f, 0.2f, 0.2f, 15);

            // 4 chain spokes radiating from hub
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                double x = Math.cos(a) * 0.4;
                double z = Math.sin(a) * 0.4;
                Location loc = core.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(140, 140, 180).interpolation(15, 8);
                spawnedEntities.add(h.entity()); spokes.add(h); allBlocks.add(h);
                setScale(h, 0.1f, 0.1f, 0.8f, 15);
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.4f, 0.8f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, core, 40, 1.2, 1.2, 1.2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location core = c.clone().add(0, 2.5, 0);

            // Y-ring rotates around Y (slow)
            if (tick % 2 == 0) {
                double baseY = tick * 0.05;
                for (int i = 0; i < ringY.size(); i++) {
                    double a = baseY + Math.PI * 2 * i / 12;
                    double x = Math.cos(a) * 1.8;
                    double z = Math.sin(a) * 1.8;
                    Location target = core.clone().add(x, 0, z);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(ringY.get(i), target, 2);
                    setRotation(ringY.get(i), (float) a, 0f, 1f, 0f, 2);
                }
                // X-ring rotates around X axis
                double baseX = tick * 0.07;
                for (int i = 0; i < ringX.size(); i++) {
                    double a = baseX + Math.PI * 2 * i / 10;
                    double y = Math.cos(a) * 1.3;
                    double z = Math.sin(a) * 1.3;
                    Location target = core.clone().add(0, y, z);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(ringX.get(i), target, 2);
                    setRotation(ringX.get(i), (float) a, 1f, 0f, 0f, 2);
                }
                // Z-ring (fastest) rotates around Z
                double baseZ = tick * 0.10;
                for (int i = 0; i < ringZ.size(); i++) {
                    double a = baseZ + Math.PI * 2 * i / 8;
                    double x = Math.cos(a) * 0.8;
                    double y = Math.sin(a) * 0.8;
                    Location target = core.clone().add(x, y, 0);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(ringZ.get(i), target, 2);
                    setRotation(ringZ.get(i), (float) a, 0f, 0f, 1f, 2);
                }
            }

            // Hub pulse
            if (tick % 8 == 0) {
                float p = 0.45f + 0.05f * (float) Math.sin(tick * 0.2);
                setScale(hub.get(0), p, p, p, 8);
            }

            // Spokes rotate slowly counter to Y ring
            if (tick % 4 == 0) {
                double sa = -tick * 0.04;
                for (int i = 0; i < spokes.size(); i++) {
                    double a = sa + Math.PI * 2 * i / 4;
                    double x = Math.cos(a) * 0.4;
                    double z = Math.sin(a) * 0.4;
                    Location target = core.clone().add(x, 0, z);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(spokes.get(i), target, 4);
                    setRotation(spokes.get(i), (float) a, 0f, 1f, 0f, 4);
                }
            }

            // Particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, core, 3, 1.6, 1.6, 1.6, 0.05);
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, core, 4, 1.4, 1.4, 1.4, 0.1);
            }

            // Sounds
            if (tick % 15 == 0) DisplayBuilder.playSound(core, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 0.8f);
            if (tick % 22 == 0) DisplayBuilder.playSound(core, Sound.BLOCK_CHAIN_HIT, 0.8f, 1.2f);
            if (tick % 40 == 0) DisplayBuilder.playSound(core, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.4f);

            // Dissipate
            if (tick >= 260) {
                int dt = tick - 260;
                if (dt == 0) for (BlockDisplayHandle h : ringZ) shrinkToZero(h, 8);
                if (dt == 5) for (BlockDisplayHandle h : ringX) shrinkToZero(h, 10);
                if (dt == 10) for (BlockDisplayHandle h : ringY) shrinkToZero(h, 12);
                if (dt == 14) {
                    for (BlockDisplayHandle h : spokes) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : hub) shrinkToZero(h, 6);
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.6f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronGyroscope(plugin); }
    }

    // ================================================================
    // #34 — CHAIN MAELSTROM ("The Funnel")
    // 36 displays: 4 stacked rings (12/9/6/3) + 8 vertical ribs + 4 swirls
    // Spiral pull — fight inward, sprint outward.
    // ================================================================
    public static class ChainMaelstrom extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> r1 = new ArrayList<>();
        private final List<BlockDisplayHandle> r2 = new ArrayList<>();
        private final List<BlockDisplayHandle> r3 = new ArrayList<>();
        private final List<BlockDisplayHandle> r4 = new ArrayList<>();
        private final List<BlockDisplayHandle> ribs = new ArrayList<>();
        private final List<BlockDisplayHandle> swirls = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public ChainMaelstrom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_maelstrom", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(260.0);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(6.0);
            config.setEnabled(true);
            config.setDesignType("Spiral pull (fight inward, sprint outward)");
        }

        private void spawnRing(Location center, double y, double radius, int count, List<BlockDisplayHandle> dest) {
            for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count;
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(150, 150, 190).interpolation(15, 5);
                spawnedEntities.add(h.entity()); dest.add(h); allBlocks.add(h);
                setScale(h, 0.35f, 0.4f, 0.18f, 15);
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            spawnRing(center, 0.5, 2.0, 12, r1);
            spawnRing(center, 1.7, 1.4, 9, r2);
            spawnRing(center, 2.9, 0.8, 6, r3);
            spawnRing(center, 4.0, 0.25, 3, r4);

            // 8 vertical ribs connecting rings
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double x = Math.cos(a) * 1.0;
                double z = Math.sin(a) * 1.0;
                Location loc = center.clone().add(x, 2.0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(120, 120, 170).interpolation(15, 10);
                spawnedEntities.add(h.entity()); ribs.add(h); allBlocks.add(h);
                setScale(h, 0.1f, 3.5f, 0.1f, 15);
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }

            // 4 inner CHAIN swirl accents
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                double x = Math.cos(a) * 1.5;
                double z = Math.sin(a) * 1.5;
                Location loc = center.clone().add(x, 1.0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(70, 70, 100).interpolation(15, 12);
                spawnedEntities.add(h.entity()); swirls.add(h); allBlocks.add(h);
                setScale(h, 0.18f, 0.18f, 0.18f, 15);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.1f, 0.5f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 2, 0), 40, 2, 1.5, 2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // All rings same Y rotation — appear as one continuous spinning wall
            if (tick % 2 == 0) {
                double baseAngle = tick * 0.06;
                updateRing(r1, c, 0.5, 2.0, baseAngle);
                updateRing(r2, c, 1.7, 1.4, baseAngle);
                updateRing(r3, c, 2.9, 0.8, baseAngle);
                updateRing(r4, c, 4.0, 0.25, baseAngle);

                // swirls counter-rotate inside
                double swirlA = -tick * 0.08;
                for (int i = 0; i < swirls.size(); i++) {
                    double a = swirlA + Math.PI * 2 * i / 4;
                    double x = Math.cos(a) * 1.5;
                    double z = Math.sin(a) * 1.5;
                    Location target = c.clone().add(x, 1.0 + Math.sin(tick * 0.06 + i) * 0.3, z);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(swirls.get(i), target, 2);
                }
            }

            // Sprial pull on players
            if (tick > 25 && tick % 6 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dx = c.getX() - pl.getX();
                    double dz = c.getZ() - pl.getZ();
                    double hd = Math.sqrt(dx * dx + dz * dz);
                    if (hd > 2.0 && hd < 9.0) {
                        // tangential + inward
                        double inwardX = (dx / hd) * 0.06;
                        double inwardZ = (dz / hd) * 0.06;
                        double tangX = -(dz / hd) * 0.04;
                        double tangZ = (dx / hd) * 0.04;
                        p.setVelocity(p.getVelocity().add(new Vector(inwardX + tangX, 0, inwardZ + tangZ)));
                    }
                }
            }

            // Particles
            if (tick % 2 == 0) {
                for (int i = 0; i < r1.size(); i += 2) {
                    Location pl = r1.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pl, 1, 0.15, 0.15, 0.15, 0.04);
                }
            }
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1.5, 0), 4, 1.6, 1.0, 1.6, 0.3, Material.CHAIN.createBlockData());
            }
            if (tick % 18 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.6f);
            if (tick % 35 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 0.8f, 0.5f);

            // Dissipate
            if (tick >= 220) {
                int dt = tick - 220;
                if (dt == 0) for (BlockDisplayHandle h : r4) shrinkToZero(h, 8);
                if (dt == 4) for (BlockDisplayHandle h : r3) shrinkToZero(h, 8);
                if (dt == 8) for (BlockDisplayHandle h : r2) shrinkToZero(h, 8);
                if (dt == 12) for (BlockDisplayHandle h : r1) shrinkToZero(h, 8);
                if (dt == 14) {
                    for (BlockDisplayHandle h : ribs) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : swirls) shrinkToZero(h, 6);
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.2f, 0.5f);
                }
            }
        }

        private void updateRing(List<BlockDisplayHandle> ring, Location c, double y, double radius, double baseAngle) {
            int n = ring.size();
            for (int i = 0; i < n; i++) {
                double a = baseAngle + Math.PI * 2 * i / n;
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                Location target = c.clone().add(x, y, z);
                target.setYaw(0); target.setPitch(0);
                teleportSmooth(ring.get(i), target, 2);
                setRotation(ring.get(i), (float) a, 0f, 1f, 0f, 2);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainMaelstrom(plugin); }
    }

    // ================================================================
    // #35 — CHAIN PINWHEEL ("The Blades")
    // 31 displays: 1 hub + 4 anchor links + 6 arms × (3 chain + 1 iron tip) = 24
    //   + 2 base anchor flanges = 31 — Timing between blade gaps.
    // ================================================================
    public static class ChainPinwheel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final List<BlockDisplayHandle> hubAnchor = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> tips = new ArrayList<>();
        private final List<BlockDisplayHandle> flanges = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public ChainPinwheel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_pinwheel", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(360.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(220);
            config.setCooldownTicks(260);
            config.setChance(6.0);
            config.setEnabled(true);
            config.setDesignType("Rotating blades (timing dodge between blade gaps)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            Location core = center.clone().add(0, 1.5, 0);

            // Hub
            BlockDisplayHandle hb = displayBuilder.spawnBlock(core.clone(), Material.NETHERITE_BLOCK);
            hb.scale(0f, 0f, 0f).glow(60, 60, 80).interpolation(15, 0);
            spawnedEntities.add(hb.entity()); hub.add(hb); allBlocks.add(hb);
            setScale(hb, 0.55f, 0.45f, 0.55f, 15);

            // Hub vertical anchor (4 chain links above)
            for (int i = 0; i < 4; i++) {
                Location loc = core.clone().add(0, 0.4 + i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(140, 140, 180).interpolation(15, 5);
                spawnedEntities.add(h.entity()); hubAnchor.add(h); allBlocks.add(h);
                setScale(h, 0.16f, 0.26f, 0.16f, 15);
            }

            // 6 arms
            for (int i = 0; i < 6; i++) {
                List<BlockDisplayHandle> arm = new ArrayList<>();
                double a = Math.PI * 2 * i / 6;
                for (int j = 0; j < 3; j++) {
                    double r = 0.6 + j * 0.9;
                    double x = Math.cos(a) * r;
                    double z = Math.sin(a) * r;
                    Location loc = core.clone().add(x, 0, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(140, 140, 180).interpolation(15, 8 + j * 2);
                    spawnedEntities.add(h.entity()); arm.add(h); allBlocks.add(h);
                    setScale(h, 0.18f, 0.18f, 0.5f, 15);
                    setRotation(h, (float) a, 0f, 1f, 0f, 15);
                }
                // Iron tip
                double r = 3.2;
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                Location loc = core.clone().add(x, 0, z);
                BlockDisplayHandle tip = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                tip.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(15, 15);
                spawnedEntities.add(tip.entity()); arm.add(tip); tips.add(tip); allBlocks.add(tip);
                setScale(tip, 0.4f, 0.4f, 0.4f, 15);

                arms.add(arm);
            }

            // 2 base flanges
            for (int i = 0; i < 2; i++) {
                double dx = (i == 0) ? -0.6 : 0.6;
                Location loc = center.clone().add(dx, 0.1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(15, 4);
                spawnedEntities.add(h.entity()); flanges.add(h); allBlocks.add(h);
                setScale(h, 1.0f, 0.12f, 0.3f, 15);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.4f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, core, 50, 2, 0.5, 2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location core = c.clone().add(0, 1.5, 0);

            // Arms spin 10°/tick = 0.1745 rad/tick
            double baseAngle = tick * 0.175;
            for (int i = 0; i < arms.size(); i++) {
                double a = baseAngle + Math.PI * 2 * i / 6;
                List<BlockDisplayHandle> arm = arms.get(i);
                for (int j = 0; j < 3; j++) {
                    double r = 0.6 + j * 0.9;
                    double x = Math.cos(a) * r;
                    double z = Math.sin(a) * r;
                    Location target = core.clone().add(x, 0, z);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(arm.get(j), target, 2);
                    setRotation(arm.get(j), (float) a, 0f, 1f, 0f, 2);
                }
                // Tip
                double rTip = 3.2;
                double xt = Math.cos(a) * rTip;
                double zt = Math.sin(a) * rTip;
                double bob = Math.sin(tick * 0.4 + i) * 0.06;
                Location tipTarget = core.clone().add(xt, bob, zt);
                tipTarget.setYaw(0); tipTarget.setPitch(0);
                teleportSmooth(arm.get(3), tipTarget, 2);
                setRotation(arm.get(3), (float) a, 0f, 1f, 0f, 2);
            }

            // Hub spins counter
            if (tick % 4 == 0) {
                float hubA = (float) (-tick * 0.07);
                setRotation(hub.get(0), hubA, 0f, 1f, 0f, 4);
            }

            // Particle trails behind tips
            if (tick % 2 == 0) {
                for (BlockDisplayHandle tip : tips) {
                    c.getWorld().spawnParticle(Particle.CRIT, tip.entity().getLocation(), 2, 0.15, 0.15, 0.15, 0.05);
                }
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, core, 5, 0.4, 0.3, 0.4, 0.05);
            }
            if (tick % 14 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.9f, 1.3f);
            if (tick % 25 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 0.7f);

            // Dissipate
            if (tick >= 200) {
                int dt = tick - 200;
                if (dt == 0) {
                    for (BlockDisplayHandle h : tips) shrinkToZero(h, 10);
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.3f, 0.6f);
                }
                if (dt == 8) {
                    for (List<BlockDisplayHandle> arm : arms) {
                        for (int j = 0; j < 3; j++) shrinkToZero(arm.get(j), 8);
                    }
                }
                if (dt == 14) {
                    for (BlockDisplayHandle h : hub) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : hubAnchor) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : flanges) shrinkToZero(h, 6);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainPinwheel(plugin); }
    }

    // ================================================================
    // #36 — ORBITAL WRECKING BALLS ("The Constellation")
    // 35 displays: 5 balls × 7 parts + 1 hub. Wait — 5×7=35, +hub = 36.
    // We'll use 5×6 (core+4poles+1trail) + 5 hub layers = 35.
    // Multi-orbit constellation — thread through orbits.
    // ================================================================
    public static class OrbitalWreckingBalls extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> balls = new ArrayList<>();
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final double[] radii = {1.8, 2.5, 3.2, 4.0, 4.8};
        private final double[] periodTicks = {28, 36, 44, 52, 60};
        private final double[] startAngles = {0, 1.2, 2.4, 3.6, 4.8};

        public OrbitalWreckingBalls(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_wrecking_balls", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setChance(5.0);
            config.setEnabled(true);
            config.setDesignType("Multi-orbit constellation (read orbits, thread through)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            Location core = center.clone().add(0, 2.0, 0);

            // Central hub (5 layered pieces)
            for (int i = 0; i < 5; i++) {
                Location loc = core.clone().add(0, -0.2 + i * 0.1, 0);
                Material mat = (i % 2 == 0) ? Material.POLISHED_BLACKSTONE : Material.NETHERITE_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0f, 0f, 0f).glow(80, 80, 100).interpolation(15, i);
                spawnedEntities.add(h.entity()); hub.add(h);
                setScale(h, 0.4f - i * 0.05f, 0.1f, 0.4f - i * 0.05f, 15);
            }

            // 5 balls × 6 parts
            for (int b = 0; b < 5; b++) {
                List<BlockDisplayHandle> ball = new ArrayList<>();
                double a = startAngles[b];
                double r = radii[b];
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                Location bLoc = core.clone().add(x, 0, z);

                // Core
                BlockDisplayHandle c0 = displayBuilder.spawnBlock(bLoc, Material.NETHERITE_BLOCK);
                c0.scale(0f, 0f, 0f).glow(70, 70, 100).interpolation(15, 5);
                spawnedEntities.add(c0.entity()); ball.add(c0);
                setScale(c0, 0.55f, 0.55f, 0.55f, 15);

                // 4 IRON poles
                double[][] poleOff = {{0.25, 0, 0}, {-0.25, 0, 0}, {0, 0, 0.25}, {0, 0, -0.25}};
                for (double[] po : poleOff) {
                    BlockDisplayHandle p = displayBuilder.spawnBlock(bLoc.clone().add(po[0], po[1], po[2]), Material.IRON_BLOCK);
                    p.scale(0f, 0f, 0f).glow(190, 190, 210).interpolation(15, 7);
                    spawnedEntities.add(p.entity()); ball.add(p);
                    setScale(p, 0.24f, 0.24f, 0.24f, 15);
                }

                // 1 trailing chain link
                BlockDisplayHandle tail = displayBuilder.spawnBlock(bLoc, Material.CHAIN);
                tail.scale(0f, 0f, 0f).glow(140, 140, 180).interpolation(15, 9);
                spawnedEntities.add(tail.entity()); ball.add(tail);
                setScale(tail, 0.16f, 0.16f, 0.6f, 15);

                balls.add(ball);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.6f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.3f, 0.6f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, core, 40, 1.5, 1, 1.5, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location core = c.clone().add(0, 2.0, 0);

            // Update each ball orbit
            for (int b = 0; b < balls.size(); b++) {
                double a = startAngles[b] + (Math.PI * 2 * tick) / periodTicks[b];
                double r = radii[b];
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                double y = Math.sin(a * 1.3) * 0.5;
                Location bLoc = core.clone().add(x, y, z);
                bLoc.setYaw(0); bLoc.setPitch(0);

                List<BlockDisplayHandle> ball = balls.get(b);
                teleportSmooth(ball.get(0), bLoc, 2);
                // poles
                teleportSmooth(ball.get(1), bLoc.clone().add(0.25, 0, 0), 2);
                teleportSmooth(ball.get(2), bLoc.clone().add(-0.25, 0, 0), 2);
                teleportSmooth(ball.get(3), bLoc.clone().add(0, 0, 0.25), 2);
                teleportSmooth(ball.get(4), bLoc.clone().add(0, 0, -0.25), 2);
                // tail behind by 0.3 rad
                double tailA = a - 0.3;
                Location tailLoc = core.clone().add(Math.cos(tailA) * r, y, Math.sin(tailA) * r);
                tailLoc.setYaw(0); tailLoc.setPitch(0);
                teleportSmooth(ball.get(5), tailLoc, 2);
                setRotation(ball.get(5), (float) (a + Math.PI / 2), 0f, 1f, 0f, 2);
            }

            // Particles
            if (tick % 3 == 0) {
                for (List<BlockDisplayHandle> ball : balls) {
                    Location l = ball.get(0).entity().getLocation();
                    DisplayBuilder.dustParticles(l, 2, 0.2, 120, 120, 120, 1.0f);
                }
            }
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, core, 4, 0.5, 0.3, 0.5, 0.05);
            }
            if (tick % 6 == 0) DisplayBuilder.playSound(core, Sound.BLOCK_CHAIN_HIT, 0.6f, 0.9f);
            if (tick % 25 == 0) DisplayBuilder.playSound(core, Sound.BLOCK_BEACON_AMBIENT, 0.9f, 1.0f);
            if (tick % 40 == 0) DisplayBuilder.playSound(core, Sound.BLOCK_PISTON_EXTEND, 0.7f, 0.6f);

            // Hub pulse
            if (tick % 10 == 0) {
                for (int i = 0; i < hub.size(); i++) {
                    float s = 0.4f - i * 0.05f + 0.04f * (float) Math.sin(tick * 0.12 + i);
                    setScale(hub.get(i), s, 0.1f, s, 10);
                }
            }

            // Dissipate
            if (tick >= 260) {
                int dt = tick - 260;
                if (dt == 0) {
                    // balls fly outward
                    for (int b = 0; b < balls.size(); b++) {
                        for (BlockDisplayHandle h : balls.get(b)) shrinkToZero(h, 10);
                    }
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.6f);
                }
                if (dt == 10) {
                    for (BlockDisplayHandle h : hub) shrinkToZero(h, 8);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new OrbitalWreckingBalls(plugin); }
    }

    // ================================================================
    // #37 — IRON HALO DESCENT ("The Nimbus")
    // 32 displays: 16 IRON slabs + 8 CHAIN accents + 2 NETHERITE weights +
    //              6 inner support rim — descends slowly, escape ring perimeter.
    // ================================================================
    public static class IronHaloDescent extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ring = new ArrayList<>();
        private final List<BlockDisplayHandle> accents = new ArrayList<>();
        private final List<BlockDisplayHandle> weights = new ArrayList<>();
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private static final double DESCEND_FROM = 4.0;
        private static final double DESCEND_TO = 0.4;
        private static final int DESCEND_TICKS = 120;
        private int landedTick = -1;

        public IronHaloDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_halo_descent", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(220);
            config.setCooldownTicks(260);
            config.setChance(5.0);
            config.setEnabled(true);
            config.setDesignType("Descending ring (escape before halo lands)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                double x = Math.cos(a) * 2.5;
                double z = Math.sin(a) * 2.5;
                Location loc = center.clone().add(x, DESCEND_FROM, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(15, i / 2);
                spawnedEntities.add(h.entity()); ring.add(h); allBlocks.add(h);
                setScale(h, 0.6f, 0.1f, 0.18f, 15);
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }

            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + Math.PI / 8;
                double x = Math.cos(a) * 2.5;
                double z = Math.sin(a) * 2.5;
                Location loc = center.clone().add(x, DESCEND_FROM, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(140, 140, 180).interpolation(15, 8);
                spawnedEntities.add(h.entity()); accents.add(h); allBlocks.add(h);
                setScale(h, 0.12f, 0.12f, 0.3f, 15);
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }

            // 2 NETHERITE weights N/S
            for (int i = 0; i < 2; i++) {
                double a = (i == 0) ? 0 : Math.PI;
                double x = Math.cos(a) * 2.5;
                double z = Math.sin(a) * 2.5;
                Location loc = center.clone().add(x, DESCEND_FROM - 0.2, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(50, 50, 70).interpolation(15, 12);
                spawnedEntities.add(h.entity()); weights.add(h); allBlocks.add(h);
                setScale(h, 0.2f, 0.35f, 0.2f, 15);
            }

            // 6 inner rim supports
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 12;
                double x = Math.cos(a) * 1.8;
                double z = Math.sin(a) * 1.8;
                Location loc = center.clone().add(x, DESCEND_FROM + 0.2, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(170, 170, 190).interpolation(15, 14);
                spawnedEntities.add(h.entity()); rim.add(h); allBlocks.add(h);
                setScale(h, 0.15f, 0.08f, 0.5f, 15);
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 1.0f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, DESCEND_FROM, 0), 40, 3, 0.3, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descent from tick 20 to tick 20+DESCEND_TICKS
            int dt = tick - 20;
            double y;
            if (dt <= 0) {
                y = DESCEND_FROM;
            } else if (dt >= DESCEND_TICKS) {
                y = DESCEND_TO;
                if (landedTick < 0) {
                    landedTick = tick;
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 2.0f, 0.5f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.6f, 0.4f);
                    c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, DESCEND_TO, 0), 30, 3, 0.3, 3, 0.1);
                    c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, DESCEND_TO, 0), 40, 3, 0.3, 3, 0.3, Material.IRON_BLOCK.createBlockData());
                }
            } else {
                y = DESCEND_FROM + (DESCEND_TO - DESCEND_FROM) * ((double) dt / DESCEND_TICKS);
            }

            // Spin halo on Y, 3°/tick
            double spin = tick * 0.052;

            // Update ring + accents + weights + rim positions
            if (tick % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double a = spin + Math.PI * 2 * i / 16;
                    Location target = c.clone().add(Math.cos(a) * 2.5, y, Math.sin(a) * 2.5);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(ring.get(i), target, 2);
                    setRotation(ring.get(i), (float) a, 0f, 1f, 0f, 2);
                }
                for (int i = 0; i < 8; i++) {
                    double a = spin + Math.PI * 2 * i / 8 + Math.PI / 8;
                    Location target = c.clone().add(Math.cos(a) * 2.5, y, Math.sin(a) * 2.5);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(accents.get(i), target, 2);
                    setRotation(accents.get(i), (float) a, 0f, 1f, 0f, 2);
                }
                for (int i = 0; i < weights.size(); i++) {
                    double a = spin + ((i == 0) ? 0 : Math.PI);
                    Location target = c.clone().add(Math.cos(a) * 2.5, y - 0.2, Math.sin(a) * 2.5);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(weights.get(i), target, 2);
                }
                for (int i = 0; i < rim.size(); i++) {
                    double a = spin + Math.PI * 2 * i / 6 + Math.PI / 12;
                    Location target = c.clone().add(Math.cos(a) * 1.8, y + 0.2, Math.sin(a) * 1.8);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(rim.get(i), target, 2);
                    setRotation(rim.get(i), (float) a, 0f, 1f, 0f, 2);
                }
            }

            // Particles on perimeter
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = spin + Math.PI * 2 * i / 8;
                    Location pl = c.clone().add(Math.cos(a) * 2.5, y, Math.sin(a) * 2.5);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pl, 1, 0.1, 0.1, 0.1, 0.03);
                }
            }
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, y - 0.2, 0), 4, 2.4, 0.1, 2.4, 0.02);
            }
            if (tick % 12 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.8f, 1.0f);
            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 1.3f);

            // Dissipate
            if (tick >= 200) {
                int x = tick - 200;
                if (x == 0) DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.2f, 0.5f);
                if (x == 4) {
                    for (BlockDisplayHandle h : ring) shrinkToZero(h, 8);
                    for (BlockDisplayHandle h : accents) shrinkToZero(h, 8);
                }
                if (x == 10) {
                    for (BlockDisplayHandle h : weights) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : rim) shrinkToZero(h, 6);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronHaloDescent(plugin); }
    }

    // ================================================================
    // #38 — CHAIN HELIX TOWER ("The Drill")
    // 34 displays: 16 CHAIN strand A + 16 IRON_BLOCK strand B + 2 axis cores.
    // Double helix tower — time through helix gaps.
    // ================================================================
    public static class ChainHelixTower extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> strandA = new ArrayList<>();
        private final List<BlockDisplayHandle> strandB = new ArrayList<>();
        private final List<BlockDisplayHandle> axis = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private static final int N = 16;
        private static final double R = 0.8;
        private static final double TURNS = 2.0;
        private static final double HEIGHT = 4.5;

        public ChainHelixTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_helix_tower", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(340.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(5.0);
            config.setEnabled(true);
            config.setDesignType("Spiraling helix (timing through helix gaps)");
        }

        private double helixY(int i) {
            return (i / (double) (N - 1)) * HEIGHT;
        }

        private double helixT(int i) {
            return (i / (double) (N - 1)) * Math.PI * 2 * TURNS;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Strand A — CHAIN
            for (int i = 0; i < N; i++) {
                double t = helixT(i);
                double x = Math.cos(t) * R;
                double z = Math.sin(t) * R;
                double y = helixY(i);
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(140, 140, 180).interpolation(15, i);
                spawnedEntities.add(h.entity()); strandA.add(h); allBlocks.add(h);
                setScale(h, 0.3f, 0.3f, 0.3f, 15);
            }
            // Strand B — IRON, offset by pi
            for (int i = 0; i < N; i++) {
                double t = helixT(i) + Math.PI;
                double x = Math.cos(t) * R;
                double z = Math.sin(t) * R;
                double y = helixY(i);
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(15, i);
                spawnedEntities.add(h.entity()); strandB.add(h); allBlocks.add(h);
                setScale(h, 0.3f, 0.3f, 0.3f, 15);
            }
            // 2 thin central axis columns
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, HEIGHT / 2 + (i == 0 ? -0.5 : 0.5), 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(180, 180, 210).interpolation(15, 5);
                spawnedEntities.add(h.entity()); axis.add(h); allBlocks.add(h);
                setScale(h, 0.06f, (float) (HEIGHT / 2.0), 0.06f, 15);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.5f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.3f, 1.0f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, HEIGHT / 2, 0), 40, 1, 2, 1, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spin entire helix
            if (tick % 2 == 0) {
                double base = tick * 0.08;
                for (int i = 0; i < N; i++) {
                    double t = base + helixT(i);
                    double x = Math.cos(t) * R;
                    double z = Math.sin(t) * R;
                    double y = helixY(i) + Math.sin(tick * 0.1 + i * 0.3) * 0.05;
                    Location target = c.clone().add(x, y, z);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(strandA.get(i), target, 2);

                    double tb = base + helixT(i) + Math.PI;
                    double xb = Math.cos(tb) * R;
                    double zb = Math.sin(tb) * R;
                    Location targetB = c.clone().add(xb, y, zb);
                    targetB.setYaw(0); targetB.setPitch(0);
                    teleportSmooth(strandB.get(i), targetB, 2);
                }
            }

            // Particle spiral up the helix
            if (tick % 2 == 0) {
                double base = tick * 0.08;
                for (int i = 0; i < N; i += 2) {
                    double t = base + helixT(i);
                    double x = Math.cos(t) * R;
                    double z = Math.sin(t) * R;
                    double y = helixY(i);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(x, y, z), 1, 0.1, 0.05, 0.1, 0.03);
                }
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, HEIGHT / 2, 0), 5, 0.5, 1.8, 0.5, 0.1);
            }
            if (tick % 14 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.8f, 1.3f);
            if (tick % 22 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 1.2f);
            if (tick % 35 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.9f, 1.0f);

            // Dissipate
            if (tick >= 220) {
                int dt = tick - 220;
                if (dt == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.6f);
                    for (BlockDisplayHandle h : axis) shrinkToZero(h, 8);
                }
                // Cascading shrink from top down
                int idx = N - 1 - dt;
                if (idx >= 0 && idx < N) {
                    shrinkToZero(strandA.get(idx), 4);
                    shrinkToZero(strandB.get(idx), 4);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainHelixTower(plugin); }
    }

    // ================================================================
    // #39 — SPINNING CHAIN WALL ("The Coin")
    // 32 displays: 5×6 = 30 CHAIN slabs + 2 IRON borders.
    // Wall coin-flips on Y axis — read which face is incoming.
    // ================================================================
    public static class SpinningChainWall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wall = new ArrayList<>();
        private final List<BlockDisplayHandle> borders = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private final List<double[]> localOffsets = new ArrayList<>(); // local (x,y) per slab

        public SpinningChainWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spinning_chain_wall", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(380.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(220);
            config.setCooldownTicks(260);
            config.setChance(6.0);
            config.setEnabled(true);
            config.setDesignType("Coin-flip wall (read which face is incoming)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 5 rows × 6 cols grid (panel size 3.0 wide × 2.5 tall)
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 6; col++) {
                    double lx = -1.25 + col * 0.5;
                    double ly = 0.4 + row * 0.5;
                    localOffsets.add(new double[]{lx, ly});

                    Location loc = center.clone().add(lx, ly, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(150, 150, 190).interpolation(15, row + col);
                    spawnedEntities.add(h.entity()); wall.add(h); allBlocks.add(h);
                    setScale(h, 0.5f, 0.45f, 0.08f, 15);
                }
            }

            // 2 iron borders top/bottom
            for (int i = 0; i < 2; i++) {
                double ly = (i == 0) ? 0.1 : 2.8;
                localOffsets.add(new double[]{0, ly});
                Location loc = center.clone().add(0, ly, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(15, 8);
                spawnedEntities.add(h.entity()); borders.add(h); allBlocks.add(h);
                setScale(h, 3.0f, 0.18f, 0.16f, 15);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.6f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 1.0f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 1.5, 0), 30, 1.5, 1.2, 0.2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wall rotates around Y axis — full revolution per 36 ticks
            double base = tick * 0.175;
            double cos = Math.cos(base);
            double sin = Math.sin(base);

            if (tick % 2 == 0) {
                // wall slabs
                for (int k = 0; k < wall.size(); k++) {
                    double[] off = localOffsets.get(k);
                    double lx = off[0];
                    double ly = off[1];
                    // Local X rotates into XZ plane
                    double wx = lx * cos;
                    double wz = lx * sin;
                    Location target = c.clone().add(wx, ly, wz);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(wall.get(k), target, 2);
                    setRotation(wall.get(k), (float) base, 0f, 1f, 0f, 2);
                }
                // borders (zero local X — stay on axis but rotate visually)
                for (int k = 0; k < borders.size(); k++) {
                    double[] off = localOffsets.get(wall.size() + k);
                    double ly = off[1];
                    Location target = c.clone().add(0, ly, 0);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(borders.get(k), target, 2);
                    setRotation(borders.get(k), (float) base, 0f, 1f, 0f, 2);
                }
            }

            // Trail smoke at edges
            if (tick % 2 == 0) {
                double edgeX = 1.5 * cos;
                double edgeZ = 1.5 * sin;
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(edgeX, 1.5, edgeZ), 1, 0.1, 0.5, 0.1, 0.05);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(-edgeX, 1.5, -edgeZ), 1, 0.1, 0.5, 0.1, 0.05);
            }
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 1.5, 0), 3, 1.5, 1.0, 1.5, 0.1);
            }
            if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 1.0f, 1.4f);
            if (tick % 18 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 1.3f);
            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 0.9f);

            // Dissipate
            if (tick >= 200) {
                int dt = tick - 200;
                if (dt == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.3f, 0.5f);
                }
                // Outer columns shrink first, work inward
                int colOrder = dt;
                for (int row = 0; row < 5; row++) {
                    if (colOrder < 6) {
                        int idx1 = row * 6 + (5 - colOrder);
                        int idx2 = row * 6 + colOrder;
                        if (idx1 >= 0 && idx1 < wall.size()) shrinkToZero(wall.get(idx1), 4);
                        if (idx2 >= 0 && idx2 < wall.size()) shrinkToZero(wall.get(idx2), 4);
                    }
                }
                if (dt == 12) {
                    for (BlockDisplayHandle h : borders) shrinkToZero(h, 6);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SpinningChainWall(plugin); }
    }

    // ================================================================
    // #40 — IRON TORNADO ("The Cyclone")
    // 34 displays: 4 rings alternating CHAIN/IRON_BLOCK (12+9+6+3=30) +
    //              3 vertical CHAIN rib columns + 1 NETHERITE eye core.
    // Vortex zone — sprint perpendicular, find eye.
    // ================================================================
    public static class IronTornado extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> r1 = new ArrayList<>();
        private final List<BlockDisplayHandle> r2 = new ArrayList<>();
        private final List<BlockDisplayHandle> r3 = new ArrayList<>();
        private final List<BlockDisplayHandle> r4 = new ArrayList<>();
        private final List<BlockDisplayHandle> ribs = new ArrayList<>();
        private final List<BlockDisplayHandle> eye = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public IronTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_tornado", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(6.5);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
            config.setChance(4.0);
            config.setEnabled(true);
            config.setDesignType("Vortex zone (sprint perpendicular, find eye)");
        }

        private void spawnTornadoRing(Location center, double y, double radius, int count, List<BlockDisplayHandle> dest) {
            for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count;
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                Location loc = center.clone().add(x, y, z);
                Material mat = (i % 2 == 0) ? Material.CHAIN : Material.IRON_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0f, 0f, 0f).glow(160, 160, 190).interpolation(15, 5);
                spawnedEntities.add(h.entity()); dest.add(h); allBlocks.add(h);
                if (mat == Material.CHAIN) {
                    setScale(h, 0.28f, 0.4f, 0.16f, 15);
                } else {
                    setScale(h, 0.35f, 0.18f, 0.22f, 15);
                }
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            spawnTornadoRing(center, 0.4, 2.0, 12, r1);
            spawnTornadoRing(center, 1.6, 1.4, 9, r2);
            spawnTornadoRing(center, 2.8, 0.8, 6, r3);
            spawnTornadoRing(center, 3.8, 0.25, 3, r4);

            // 3 vertical chain rib columns
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 * i / 3;
                double x = Math.cos(a) * 1.2;
                double z = Math.sin(a) * 1.2;
                Location loc = center.clone().add(x, 2.0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(130, 130, 170).interpolation(15, 8);
                spawnedEntities.add(h.entity()); ribs.add(h); allBlocks.add(h);
                setScale(h, 0.1f, 3.6f, 0.1f, 15);
                setRotation(h, (float) a, 0f, 1f, 0f, 15);
            }

            // Eye core
            BlockDisplayHandle eyeCore = displayBuilder.spawnBlock(center.clone().add(0, 2.0, 0), Material.NETHERITE_BLOCK);
            eyeCore.scale(0f, 0f, 0f).glow(80, 80, 110).interpolation(15, 5);
            spawnedEntities.add(eyeCore.entity()); eye.add(eyeCore); allBlocks.add(eyeCore);
            setScale(eyeCore, 0.28f, 0.28f, 0.28f, 15);

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.5f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 2, 0), 50, 2.5, 1.5, 2.5, 0.15);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rings spin at slightly different speeds for vortex effect
            if (tick % 2 == 0) {
                updateRingT(r1, c, 0.4, 2.0, tick * 0.05);
                updateRingT(r2, c, 1.6, 1.4, tick * 0.065);
                updateRingT(r3, c, 2.8, 0.8, tick * 0.08);
                updateRingT(r4, c, 3.8, 0.25, tick * 0.10);
            }

            // Ribs spin slowly with the average
            if (tick % 3 == 0) {
                double ribBase = tick * 0.06;
                for (int i = 0; i < ribs.size(); i++) {
                    double a = ribBase + Math.PI * 2 * i / 3;
                    double x = Math.cos(a) * 1.2;
                    double z = Math.sin(a) * 1.2;
                    Location target = c.clone().add(x, 2.0, z);
                    target.setYaw(0); target.setPitch(0);
                    teleportSmooth(ribs.get(i), target, 3);
                    setRotation(ribs.get(i), (float) a, 0f, 1f, 0f, 3);
                }
            }

            // Eye pulses
            if (tick % 6 == 0) {
                float p = 0.25f + 0.05f * (float) Math.sin(tick * 0.18);
                setScale(eye.get(0), p, p, p, 6);
            }

            // CRIT debris flying outward at perimeter
            if (tick % 2 == 0) {
                double rOut = 2.5;
                for (int i = 0; i < 4; i++) {
                    double a = tick * 0.12 + Math.PI * 2 * i / 4;
                    double x = Math.cos(a) * rOut;
                    double z = Math.sin(a) * rOut;
                    c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(x, 1.0, z), 2, 0.1, 0.3, 0.1, 0.08);
                }
            }
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1.5, 0), 4, 0.3, 1.5, 0.3, 0.08);
            }
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 2.5, 0), 5, 1.5, 1.0, 1.5, 0.05);
            }

            // Sounds
            if (tick % 14 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.9f, 0.4f);
            if (tick % 9 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.9f, 0.5f);
            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 0.8f, 0.4f);

            // Mild outward push at periphery (just outside damage zone) — encourage "sprint perpendicular"
            if (tick > 35 && tick % 8 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dx = pl.getX() - c.getX();
                    double dz = pl.getZ() - c.getZ();
                    double hd = Math.sqrt(dx * dx + dz * dz);
                    if (hd > 5.5 && hd < 8.0) {
                        // tangential nudge to feel like wind
                        double tangX = -(dz / hd) * 0.05;
                        double tangZ = (dx / hd) * 0.05;
                        p.setVelocity(p.getVelocity().add(new Vector(tangX, 0, tangZ)));
                    }
                }
            }

            // Dissipate
            if (tick >= 280) {
                int dt = tick - 280;
                if (dt == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.6f, 0.4f);
                }
                if (dt == 2) for (BlockDisplayHandle h : r4) shrinkToZero(h, 6);
                if (dt == 4) for (BlockDisplayHandle h : r3) shrinkToZero(h, 6);
                if (dt == 6) for (BlockDisplayHandle h : r2) shrinkToZero(h, 6);
                if (dt == 8) for (BlockDisplayHandle h : r1) shrinkToZero(h, 6);
                if (dt == 12) {
                    for (BlockDisplayHandle h : ribs) shrinkToZero(h, 4);
                    for (BlockDisplayHandle h : eye) shrinkToZero(h, 4);
                }
            }
        }

        private void updateRingT(List<BlockDisplayHandle> ring, Location c, double y, double radius, double baseAngle) {
            int n = ring.size();
            for (int i = 0; i < n; i++) {
                double a = baseAngle + Math.PI * 2 * i / n;
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                Location target = c.clone().add(x, y, z);
                target.setYaw(0); target.setPitch(0);
                teleportSmooth(ring.get(i), target, 2);
                setRotation(ring.get(i), (float) a, 0f, 1f, 0f, 2);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronTornado(plugin); }
    }
}
