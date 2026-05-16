package com.blockforge.chaoscraft.modes.freezingice.attacks;

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
 * FreezingIce Mode — BLOCK DISPLAY ATTACKS 71-75 (Final Showcase Set)
 *
 * These are climactic centerpieces — jaw-dropping finales of the FreezingIce
 * BlockDisplay roster. Each attack is built from 40-65+ BlockDisplay entities
 * animated via Transformation. Long durations (200-400t), heavy particle
 * volume, frequent sound layering. No follow-AI; all attacks spawn at center
 * and dominate via scale, light, and sustained spectacle.
 *
 * Ice palette + accents:
 *   - PACKED_ICE, BLUE_ICE, ICE, FROSTED_ICE (cold core mass)
 *   - TINTED_GLASS, BLUE_STAINED_GLASS, PURPLE_STAINED_GLASS (translucent walls/halo)
 *   - DIAMOND_BLOCK, AMETHYST_BLOCK (gem highlights / runes)
 *   - CALCITE, QUARTZ_BLOCK, SNOW_BLOCK, LIGHT_BLUE_CONCRETE (bone/structural)
 *   - OBSIDIAN, BLACK_CONCRETE (void/dark hearts)
 *
 * Particles: SOUL_FIRE_FLAME, END_ROD, SNOWFLAKE, ELECTRIC_SPARK,
 *            REVERSE_PORTAL, GLOW, SCULK_SOUL, BUBBLE, CLOUD,
 *            FALLING_DUST(WHITE_CONCRETE), FLAME
 * Sounds: BLOCK_BEACON_ACTIVATE, BLOCK_AMETHYST_BLOCK_CHIME,
 *         ENTITY_WITHER_SPAWN, ENTITY_ENDER_DRAGON_GROWL (low pitch),
 *         ENTITY_GLOW_SQUID_AMBIENT, ITEM_TRIDENT_RIPTIDE,
 *         BLOCK_GLASS_PLACE, BLOCK_GLASS_BREAK, ENTITY_GENERIC_EXPLODE
 *
 * Attacks:
 *  71. EverwinterWorldTree     — 64 blocks, arena-spanning frozen tree
 *  72. ColdHeartedSun          — 45 blocks, frozen black-sun overhead
 *  73. FrozenLeviathanBreach   — 41 blocks, sea-serpent breach + crash
 *  74. AbsoluteZeroChamber     — 41 blocks, sealed hex cryo prison
 *  75. EnderGlacierVoidPortal  — 62 blocks, reality-tearing void portal
 */
public final class FreezingIceBlockDisplay8 {
    private FreezingIceBlockDisplay8() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EverwinterWorldTree(plugin));
        registry.register(new ColdHeartedSun(plugin));
        registry.register(new FrozenLeviathanBreach(plugin));
        registry.register(new AbsoluteZeroChamber(plugin));
        registry.register(new EnderGlacierVoidPortal(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    // ================================================================
    // #71 — EVERWINTER WORLD TREE
    // 64 blocks: 8 PACKED_ICE trunk (Y0..Y12), 6 main branches × 4 BLUE_ICE = 24,
    // 4 BLUE_ICE root-arms at base, 18 TINTED_GLASS frozen-leaves, 4 DIAMOND_BLOCK
    // canopy-crown, 4 ICE hanging icicles. Trunk rises 35t, branches unfurl,
    // leaves grow. 240t active with sway + leaf rustle. Icicles drop every 60t.
    // Constant radius 11.0, 6 hearts dmg/12t. 35t delay.
    // ================================================================
    public static class EverwinterWorldTree extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> trunkBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> branches = new ArrayList<>();
        private final List<BlockDisplayHandle> rootArms = new ArrayList<>();
        private final List<BlockDisplayHandle> leaves = new ArrayList<>();
        private final List<BlockDisplayHandle> crown = new ArrayList<>();
        private final List<BlockDisplayHandle> icicles = new ArrayList<>();
        private float swayPhase = 0f;
        private int lastIcicleDrop = -100;

        public EverwinterWorldTree(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("everwinter_world_tree", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1440.0);
            config.setDamageRadius(16.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(310);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 PACKED_ICE trunk segments, 1.2 x 1.5 x 1.2, stacked Y0..Y10.5
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, -0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(160, 200, 230).interpolation(8, i * 3);
                spawnedEntities.add(h.entity());
                trunkBlocks.add(h);
                // Animate rise to target Y
                final int idx = i;
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(8);
                e.setInterpolationDelay(i * 3);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, idx * 1.45f - 0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.2f, 1.5f, 1.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // 6 main branches × 4 BLUE_ICE 0.5 = 24, splaying outward from upper trunk
            for (int bi = 0; bi < 6; bi++) {
                List<BlockDisplayHandle> branch = new ArrayList<>();
                double bAngle = (2.0 * Math.PI * bi) / 6;
                int delayBase = 22 + bi * 2;
                for (int s = 0; s < 4; s++) {
                    double radial = 1.0 + s * 0.95;
                    double bx = Math.cos(bAngle) * radial;
                    double bz = Math.sin(bAngle) * radial;
                    double by = 9.0 + s * 0.55;
                    Location loc = center.clone().add(bx, by, bz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    h.scale(0.0f, 0.0f, 0.0f).glow(120, 180, 230).interpolation(7, delayBase + s * 2);
                    spawnedEntities.add(h.entity());
                    branch.add(h);
                    growBlock(h, 0.5f, 7, delayBase + s * 2);
                }
                branches.add(branch);
            }

            // 4 BLUE_ICE root arms at base, 0.6 x 0.4 x 1.5, splayed outward
            for (int r = 0; r < 4; r++) {
                double rAngle = (Math.PI * 2 * r) / 4 + Math.PI / 4;
                double rx = Math.cos(rAngle) * 1.6;
                double rz = Math.sin(rAngle) * 1.6;
                Location loc = center.clone().add(rx, 0.1, rz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 180, 230).interpolation(6, r * 2);
                spawnedEntities.add(h.entity());
                // Rotate root to point outward (around Y)
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(6);
                e.setInterpolationDelay(r * 2);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.4f, -0.5f),
                        new AxisAngle4f((float) rAngle, 0f, 1f, 0f),
                        new Vector3f(0.6f, 0.4f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                rootArms.add(h);
            }

            // 18 TINTED_GLASS frozen-leaves scattered on branches
            for (int li = 0; li < 18; li++) {
                int branchIdx = li % 6;
                double bAngle = (2.0 * Math.PI * branchIdx) / 6;
                double radial = 1.5 + (li / 6) * 1.4 + Math.random() * 0.6;
                double lx = Math.cos(bAngle) * radial + (Math.random() - 0.5) * 0.6;
                double lz = Math.sin(bAngle) * radial + (Math.random() - 0.5) * 0.6;
                double ly = 9.6 + (li / 6) * 0.7 + Math.random() * 0.5;
                Location loc = center.clone().add(lx, ly, lz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(170, 220, 255).interpolation(6, 35 + li);
                spawnedEntities.add(h.entity());
                leaves.add(h);
                growBlock(h, 0.4f, 6, 35 + li);
            }

            // 4 DIAMOND_BLOCK canopy crown at top
            for (int c = 0; c < 4; c++) {
                double cAngle = (Math.PI * 2 * c) / 4;
                double cx = Math.cos(cAngle) * 0.7;
                double cz = Math.sin(cAngle) * 0.7;
                Location loc = center.clone().add(cx, 12.5, cz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 240, 255).interpolation(8, 50 + c * 2);
                spawnedEntities.add(h.entity());
                crown.add(h);
                growBlock(h, 0.5f, 8, 50 + c * 2);
            }

            // 4 ICE hanging icicles dangling under branches (0.2 x 1.0 x 0.2)
            for (int i = 0; i < 4; i++) {
                double iAngle = (Math.PI * 2 * i) / 4;
                double ix = Math.cos(iAngle) * 2.4;
                double iz = Math.sin(iAngle) * 2.4;
                Location loc = center.clone().add(ix, 9.3, iz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(180, 220, 255).interpolation(6, 40 + i * 2);
                spawnedEntities.add(h.entity());
                icicles.add(h);
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(6);
                e.setInterpolationDelay(40 + i * 2);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f, 1.0f, 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 6, 0), 80, 4, 6, 4, 0.05);
            w.spawnParticle(Particle.END_ROD, center.clone().add(0, 12, 0), 30, 2, 1, 2, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float target, int duration, int delay) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(delay);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(target, target, target),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // SNOWFLAKE constant
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 10.0;
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(Math.cos(a) * r, Math.random() * 13, Math.sin(a) * r),
                            1, 0.2, 0.2, 0.2, 0.02);
                }
            }
            // GLOW canopy
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 12.5, 0), 8, 1.2, 0.8, 1.2, 0.05);
            }
            // ELECTRIC_SPARK on leaves
            if (tick % 4 == 0) {
                for (BlockDisplayHandle leaf : leaves) {
                    if (Math.random() < 0.3) {
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                leaf.entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }

            // Branches sway gently in wind (±15°), oscillating
            if (tick > 60 && tick < 280 && tick % 5 == 0) {
                swayPhase += 0.08f;
                float swayAngle = (float) Math.toRadians(15.0 * Math.sin(swayPhase));
                for (int bi = 0; bi < branches.size(); bi++) {
                    double bAngle = (2.0 * Math.PI * bi) / 6;
                    // axis perpendicular to branch direction in XZ plane
                    float ax = (float) -Math.sin(bAngle);
                    float az = (float) Math.cos(bAngle);
                    for (BlockDisplayHandle h : branches.get(bi)) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(5);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(swayAngle, ax, 0f, az),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Leaves rustle: small random scale-pulse
            if (tick > 60 && tick < 280 && tick % 6 == 0) {
                for (BlockDisplayHandle h : leaves) {
                    if (Math.random() < 0.4) {
                        float s = 0.35f + (float) Math.random() * 0.1f;
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(6);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(s, s, s),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Icicle drop every 60t (after tick 80)
            if (tick > 80 && tick < 270 && tick - lastIcicleDrop >= 60) {
                lastIcicleDrop = tick;
                // Pick one icicle to "drop"
                int idx = (int) (Math.random() * icicles.size());
                BlockDisplayHandle ic = icicles.get(idx);
                BlockDisplay e = ic.entity();
                Location dropLoc = e.getLocation().clone();
                // animate down
                e.setInterpolationDuration(6);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -9.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f, 1.0f, 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                // Damage at landing
                Location landing = c.clone().add(
                        dropLoc.getX() - c.getX(), 0.0, dropLoc.getZ() - c.getZ());
                applyAreaDamage(landing, 4.0, 120.0);
                DisplayBuilder.playSound(landing, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.6f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, landing, 40, 1.5, 0.3, 1.5, 0.15);
                DisplayBuilder.dustParticles(landing, 30, 2.5, 180, 220, 255, 1.6f);
                // Regrow after 30t — schedule rise back
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (e.isDead()) return;
                    e.setInterpolationDuration(8);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.2f, 1.0f, 0.2f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }, 30L);
            }

            // Ambient sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.8f, 0.5f);
            }
            if (tick == 60) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
            }

            // Dissipate phase 280-310: freeze + shatter top-down
            if (tick >= 280) {
                int shatterStep = (tick - 280);
                // Shatter crown first, then leaves, then upper branches, then trunk
                if (tick == 282) shrinkGroup(crown);
                if (tick == 286) shrinkGroup(leaves);
                if (tick == 292) {
                    for (List<BlockDisplayHandle> b : branches) shrinkGroup(b);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 10, 0), 120, 5, 3, 5, 0.2);
                }
                if (tick == 300) {
                    shrinkGroup(trunkBlocks);
                    shrinkGroup(rootArms);
                    shrinkGroup(icicles);
                }
            }
        }

        private void shrinkGroup(List<BlockDisplayHandle> group) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(8);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
        }

        private void applyAreaDamage(Location loc, double radius, double damage) {
            if (loc.getWorld() == null) return;
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL) continue;
                if (p.getLocation().distanceSquared(loc) <= radius * radius) {
                    p.damage(damage);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new EverwinterWorldTree(plugin); }
    }

    // ================================================================
    // #72 — COLD HEARTED SUN
    // 45 blocks: 1 OBSIDIAN core (Y+8, scale 1.5), 12 BLUE_ICE corona arms
    // radial 0.3x0.3x2.0, 8 TINTED_GLASS outer flares, 4 CALCITE sunspots,
    // 16 BLUE_STAINED_GLASS cold-flame radiating, 4 DIAMOND_BLOCK anchor-beams.
    // Sun rotates Y, corona arms pulse, sunspots drift. Cold-ray every 50t.
    // Constant radius 10.0, 5.5 hearts dmg/12t. Cold-ray radius 6.0, 10 hearts.
    // ================================================================
    public static class ColdHeartedSun extends BlockDisplayAttack {
        private BlockDisplayHandle core;
        private final List<BlockDisplayHandle> coronaArms = new ArrayList<>();
        private final List<BlockDisplayHandle> outerFlares = new ArrayList<>();
        private final List<BlockDisplayHandle> sunspots = new ArrayList<>();
        private final List<BlockDisplayHandle> coldFlames = new ArrayList<>();
        private final List<BlockDisplayHandle> anchorBeams = new ArrayList<>();
        private float sunYaw = 0f;
        private int lastRay = -100;

        public ColdHeartedSun(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_hearted_sun", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1320.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(290);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 1 OBSIDIAN core at Y+8, scale 1.5
            Location coreLoc = center.clone().add(0, 8, 0);
            core = displayBuilder.spawnBlock(coreLoc, Material.OBSIDIAN);
            core.scale(0.0f, 0.0f, 0.0f).glow(20, 10, 30).interpolation(10, 0);
            spawnedEntities.add(core.entity());
            growBlock(core, 1.5f, 10, 0);

            // 12 BLUE_ICE corona arms radiating outward (0.3 x 0.3 x 2.0, rotated to point out)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 * i) / 12;
                double r = 2.0;
                double ax = Math.cos(a) * r;
                double az = Math.sin(a) * r;
                Location loc = center.clone().add(ax, 8, az);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 180, 230).interpolation(6, 10 + i);
                spawnedEntities.add(h.entity());
                coronaArms.add(h);
                // Rotated so length (Z scale 2.0) points radially outward; we use Y-axis rotation
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(6);
                e.setInterpolationDelay(10 + i);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f((float) a, 0f, 1f, 0f),
                        new Vector3f(0.3f, 0.3f, 2.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // 8 TINTED_GLASS outer flares at radius 3.3
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8 + Math.PI / 16;
                double r = 3.3;
                Location loc = center.clone().add(Math.cos(a) * r, 8, Math.sin(a) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(150, 200, 240).interpolation(7, 20 + i);
                spawnedEntities.add(h.entity());
                outerFlares.add(h);
                growBlock(h, 0.5f, 7, 20 + i);
            }

            // 4 CALCITE sunspots on surface (slight offset)
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 * i) / 4 + Math.PI / 8;
                Location loc = center.clone().add(Math.cos(a) * 0.7, 8.0 + (Math.random() - 0.5) * 0.3, Math.sin(a) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 230, 240).interpolation(5, 5 + i);
                spawnedEntities.add(h.entity());
                sunspots.add(h);
                growBlock(h, 0.4f, 5, 5 + i);
            }

            // 16 BLUE_STAINED_GLASS cold-flames in radiating pattern (between corona arms)
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 * i) / 16 + Math.PI / 32;
                double r = 1.4 + Math.random() * 0.5;
                double ay = 8.0 + (Math.random() - 0.5) * 0.6;
                Location loc = center.clone().add(Math.cos(a) * r, ay, Math.sin(a) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 160, 220).interpolation(6, 15 + i);
                spawnedEntities.add(h.entity());
                coldFlames.add(h);
                growBlock(h, 0.2f, 6, 15 + i);
            }

            // 4 DIAMOND_BLOCK anchor beams (stabilizers under sun, pointing down)
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(a) * 0.4, 5, Math.sin(a) * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 240, 255).interpolation(8, 10 + i);
                spawnedEntities.add(h.entity());
                anchorBeams.add(h);
                growBlock(h, 0.3f, 8, 10 + i);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.6f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.4f);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 8, 0), 80, 3, 1.5, 3, 0.05);
            w.spawnParticle(Particle.END_ROD, center.clone().add(0, 8, 0), 40, 2.5, 1, 2.5, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float target, int duration, int delay) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(delay);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(target, target, target),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Dense SOUL_FIRE_FLAME
            if (tick % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 3.0;
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            c.clone().add(Math.cos(a) * r, 7.6 + Math.random() * 1.0, Math.sin(a) * r),
                            1, 0.05, 0.05, 0.05, 0.03);
                }
            }
            // ELECTRIC_SPARK on corona arms
            if (tick % 3 == 0) {
                for (BlockDisplayHandle arm : coronaArms) {
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            arm.entity().getLocation(), 3, 0.15, 0.15, 0.15, 0.04);
                }
            }
            // GLOW on core
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 8, 0), 10, 0.8, 0.8, 0.8, 0.05);
            }
            // Falling SNOWFLAKE under sun
            if (tick % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add((Math.random() - 0.5) * 10, 7.5 - Math.random() * 6, (Math.random() - 0.5) * 10),
                            1, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // Sun rotates Y-axis (3°/tick) — applied to corona, flares, cold flames
            sunYaw += (float) Math.toRadians(3.0);
            if (tick % 2 == 0 && tick > 30) {
                rotateGroup(outerFlares, sunYaw, 0f, 1f, 0f);
                rotateGroup(coldFlames, sunYaw, 0f, 1f, 0f);
                // Corona arms also re-applied (preserving radial direction)
                for (int i = 0; i < coronaArms.size(); i++) {
                    BlockDisplayHandle arm = coronaArms.get(i);
                    double baseAngle = (Math.PI * 2 * i) / 12;
                    float totalAngle = (float) baseAngle + sunYaw;
                    BlockDisplay e = arm.entity();
                    Transformation t = e.getTransformation();
                    Vector3f sc = t.getScale();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(totalAngle, 0f, 1f, 0f),
                            sc,
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Corona arm pulse: scale Z 1.6–2.4 oscillating
            if (tick > 40 && tick < 260 && tick % 6 == 0) {
                float pulseZ = 2.0f + 0.4f * (float) Math.sin(tick * 0.15);
                for (int i = 0; i < coronaArms.size(); i++) {
                    BlockDisplayHandle arm = coronaArms.get(i);
                    double baseAngle = (Math.PI * 2 * i) / 12;
                    float totalAngle = (float) baseAngle + sunYaw;
                    BlockDisplay e = arm.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(totalAngle, 0f, 1f, 0f),
                            new Vector3f(0.3f, 0.3f, pulseZ),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Sunspots drift slightly
            if (tick > 30 && tick < 260 && tick % 10 == 0) {
                for (int i = 0; i < sunspots.size(); i++) {
                    BlockDisplayHandle h = sunspots.get(i);
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    float dx = (float) ((Math.random() - 0.5) * 0.3);
                    float dy = (float) ((Math.random() - 0.5) * 0.3);
                    float dz = (float) ((Math.random() - 0.5) * 0.3);
                    Vector3f tr = t.getTranslation();
                    e.setInterpolationDuration(10);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(tr.x + dx, tr.y + dy, tr.z + dz),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Cold-ray every 50t (after initial spawn)
            if (tick > 50 && tick < 260 && tick - lastRay >= 50) {
                lastRay = tick;
                Player nearest = findNearestPlayer(c, 30.0);
                Location target = nearest != null ? nearest.getLocation() : c.clone();
                // Visual ray: particle line from core to target
                Location rayStart = c.clone().add(0, 8, 0);
                DisplayBuilder.particleLine(rayStart, target, Particle.SCULK_SOUL, 30, null);
                DisplayBuilder.particleLine(rayStart, target, Particle.END_ROD, 20, null);
                // Damage on landing
                applyAreaDamage(target, 6.0, 200.0);
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5f, 0.6f);
                target.getWorld().spawnParticle(Particle.SCULK_SOUL, target, 80, 2.5, 0.5, 2.5, 0.15);
                target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target, 60, 2.5, 0.5, 2.5, 0.1);
                DisplayBuilder.dustParticles(target, 60, 4.0, 120, 180, 240, 1.6f);
            }

            // Ambient drone
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.9f, 0.4f);
            }

            // Dissipate 260-290: corona collapses inward, core dims
            if (tick >= 260) {
                if (tick == 262) {
                    for (BlockDisplayHandle arm : coronaArms) {
                        BlockDisplay e = arm.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(25);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(0f, 0f, 0f),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                    shrinkGroup(coldFlames);
                    shrinkGroup(outerFlares);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.4f);
                }
                if (tick == 275) {
                    shrinkGroup(sunspots);
                    shrinkGroup(anchorBeams);
                    BlockDisplay e = core.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(15);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }
        }

        private void rotateGroup(List<BlockDisplayHandle> group, float angle, float ax, float ay, float az) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, ax, ay, az),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        private void shrinkGroup(List<BlockDisplayHandle> group) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(15);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
        }

        private void applyAreaDamage(Location loc, double radius, double damage) {
            if (loc.getWorld() == null) return;
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL) continue;
                if (p.getLocation().distanceSquared(loc) <= radius * radius) {
                    p.damage(damage);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ColdHeartedSun(plugin); }
    }

    // ================================================================
    // #73 — FROZEN LEVIATHAN BREACH
    // 41 blocks: 8 PACKED_ICE body segments (whale curve), 2 BLUE_ICE head,
    // 1 OBSIDIAN jaw + 6 DIAMOND_BLOCK fangs, 2 BLUE_STAINED_GLASS pectoral
    // fins, 2 PACKED_ICE tail-fluke, 6 CALCITE dorsal spikes, 1 TINTED_GLASS
    // eye, 4 SNOW_BLOCK water-splash, 8 QUARTZ_BLOCK baleen ribs.
    // Spawn: splash, leviathan rises +Y8 over 25t. Active 80t hang at apex.
    // Dissipate: crashes back down 25t. Impact-only damage on rise + crash.
    // ================================================================
    public static class FrozenLeviathanBreach extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private BlockDisplayHandle jaw;
        private final List<BlockDisplayHandle> fangs = new ArrayList<>();
        private final List<BlockDisplayHandle> fins = new ArrayList<>();
        private final List<BlockDisplayHandle> fluke = new ArrayList<>();
        private final List<BlockDisplayHandle> dorsalSpikes = new ArrayList<>();
        private BlockDisplayHandle eye;
        private final List<BlockDisplayHandle> splash = new ArrayList<>();
        private final List<BlockDisplayHandle> ribs = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private boolean breachDamageDone = false;
        private boolean crashDamageDone = false;
        private float jawOpenAngle = 0f;

        public FrozenLeviathanBreach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_leviathan_breach", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(2160.0);
            config.setImpactRadius(13.5);
            config.setDamage(0.0);
            config.setDurationTicks(170);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 PACKED_ICE body segments (whale-curve: arching upward toward middle)
            for (int i = 0; i < 8; i++) {
                double t = i / 7.0;
                double curveY = -2.0 + Math.sin(t * Math.PI) * 1.8; // arched
                double curveZ = -3.0 + t * 6.0; // along Z
                Location loc = center.clone().add(0, curveY, curveZ);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(2.0f, 2.0f, 2.0f).glow(160, 200, 230).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
                allBlocks.add(h);
            }

            // 2 BLUE_ICE head segments (front of body)
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, -1.5, 3.5 + i * 0.9);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(1.5f, 1.5f, 1.5f).glow(120, 180, 230).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
                allBlocks.add(h);
            }

            // 1 OBSIDIAN jaw (front-bottom of head)
            Location jawLoc = center.clone().add(0, -2.4, 4.6);
            jaw = displayBuilder.spawnBlock(jawLoc, Material.OBSIDIAN);
            jaw.scale(1.0f, 1.0f, 1.0f).glow(30, 20, 40).interpolation(4, 0);
            spawnedEntities.add(jaw.entity());
            allBlocks.add(jaw);

            // 6 DIAMOND_BLOCK fangs (3 upper, 3 lower)
            for (int i = 0; i < 6; i++) {
                double fx = (i % 3 - 1) * 0.4;
                double fy = (i < 3) ? -1.8 : -2.5;
                double fz = 5.2;
                Location loc = center.clone().add(fx, fy, fz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(200, 240, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                fangs.add(h);
                allBlocks.add(h);
            }

            // 2 BLUE_STAINED_GLASS pectoral fins (sides of body)
            double[][] finOffsets = {{-1.5, -1.5, 1.5}, {1.5, -1.5, 1.5}};
            for (double[] o : finOffsets) {
                Location loc = center.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.4f, 0.15f, 1.5f).glow(100, 160, 220).interpolation(4, 0);
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(4);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f((float) Math.toRadians(o[0] > 0 ? -25 : 25), 0f, 0f, 1f),
                        new Vector3f(0.4f, 0.15f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                spawnedEntities.add(h.entity());
                fins.add(h);
                allBlocks.add(h);
            }

            // 2 PACKED_ICE tail-fluke (horizontal flipper at rear)
            double[][] flukeOffsets = {{-1.2, -1.5, -3.5}, {1.2, -1.5, -3.5}};
            for (double[] o : flukeOffsets) {
                Location loc = center.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(1.0f, 0.2f, 1.5f).glow(160, 200, 230).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                fluke.add(h);
                allBlocks.add(h);
            }

            // 6 CALCITE dorsal spikes along back
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                double sx = 0;
                double sy = 0.0 + Math.sin(t * Math.PI) * 1.4;
                double sz = -2.5 + t * 5.0;
                Location loc = center.clone().add(sx, sy, sz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.3f, 0.8f, 0.3f).glow(220, 230, 240).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                dorsalSpikes.add(h);
                allBlocks.add(h);
            }

            // 1 TINTED_GLASS eye
            Location eyeLoc = center.clone().add(0.7, -1.3, 4.0);
            eye = displayBuilder.spawnBlock(eyeLoc, Material.TINTED_GLASS);
            eye.scale(0.3f, 0.3f, 0.3f).glow(220, 240, 255).interpolation(4, 0);
            spawnedEntities.add(eye.entity());
            allBlocks.add(eye);

            // 4 SNOW_BLOCK water-splash around base
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(a) * 3.0, -2.5, Math.sin(a) * 3.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SNOW_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(240, 245, 250).interpolation(5, i);
                spawnedEntities.add(h.entity());
                splash.add(h);
                allBlocks.add(h);
                growBlock(h, 1.0f, 5, i);
            }

            // 8 QUARTZ_BLOCK baleen ribs (inside jaw)
            for (int i = 0; i < 8; i++) {
                double rx = (i % 4 - 1.5) * 0.25;
                double ry = -2.2;
                double rz = 4.8 + (i / 4) * 0.3;
                Location loc = center.clone().add(rx, ry, rz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.15f, 0.3f, 0.15f).glow(240, 245, 250).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                ribs.add(h);
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.5f, 0.4f);
            w.spawnParticle(Particle.CLOUD, center.clone().add(0, -2, 0), 60, 4, 0.5, 4, 0.2);
            w.spawnParticle(Particle.BUBBLE, center.clone(), 80, 3, 0.5, 3, 0.3);
        }

        private void growBlock(BlockDisplayHandle h, float target, int duration, int delay) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(delay);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(target, target, target),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // BREACH PHASE: ticks 0-25, body rises +Y8
            if (tick <= 25) {
                float riseY = 8f * (tick / 25f);
                if (tick % 2 == 0) {
                    translateAll(riseY);
                }
                // Heavy splash + bubble + cloud
                if (tick % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.BUBBLE, c.clone().add((Math.random() - 0.5) * 4, riseY - 1, (Math.random() - 0.5) * 4), 8, 1, 0.3, 1, 0.2);
                    c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, riseY - 1, 0), 12, 2.5, 0.5, 2.5, 0.15);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, riseY, 0), 10, 2, 1, 2, 0.1);
                }
                if (tick == 5) DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.6f, 0.4f);
                if (tick == 15) DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.4f, 0.4f);

                if (tick == 24 && !breachDamageDone) {
                    breachDamageDone = true;
                    Location breachLoc = c.clone().add(0, 6, 0);
                    applyAreaDamage(breachLoc, 9.0, 180.0);
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 2.0f, 0.5f);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, breachLoc, 120, 4, 2, 4, 0.3);
                    DisplayBuilder.dustParticles(breachLoc, 80, 5.0, 160, 200, 230, 1.8f);
                }
            }

            // HANG PHASE: ticks 25-105 — leviathan suspended at apex (+Y8), arches, jaw opens/closes, tail slaps
            if (tick > 25 && tick < 105) {
                // Body arch: slight Y bob (sin wave on body Y)
                if (tick % 5 == 0) {
                    float bob = (float) Math.sin((tick - 25) * 0.1) * 0.4f;
                    for (BlockDisplayHandle h : body) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        Vector3f tr = t.getTranslation();
                        e.setInterpolationDuration(5);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(tr.x, 8.0f + bob - 0.5f + tr.y % 2 - tr.y % 2, tr.z),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
                // Jaw opens then closes (cycle ~30t)
                if (tick % 4 == 0) {
                    jawOpenAngle = (float) Math.toRadians(20.0 * (Math.sin((tick - 25) * 0.2) + 1) * 0.5);
                    BlockDisplay je = jaw.entity();
                    Transformation jt = je.getTransformation();
                    je.setInterpolationDuration(4);
                    je.setInterpolationDelay(0);
                    je.setTransformation(new Transformation(
                            jt.getTranslation(),
                            new AxisAngle4f(jawOpenAngle, 1f, 0f, 0f),
                            jt.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                // Tail fluke slap (cycle 20t)
                if (tick % 4 == 0) {
                    float slap = (float) Math.toRadians(20.0 * Math.sin((tick - 25) * 0.3));
                    for (BlockDisplayHandle h : fluke) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(4);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(slap, 1f, 0f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
                if (tick % 15 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.1f, 0.4f);
                }
            }

            // Particles: SNOWFLAKE + BUBBLE drifting from body (heavy)
            if (tick % 2 == 0 && tick > 10) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add((Math.random() - 0.5) * 6, 6 + Math.random() * 4, (Math.random() - 0.5) * 6),
                        6, 0.5, 0.5, 0.5, 0.05);
                c.getWorld().spawnParticle(Particle.BUBBLE,
                        c.clone().add((Math.random() - 0.5) * 5, 5 + Math.random() * 4, (Math.random() - 0.5) * 5),
                        3, 0.3, 0.3, 0.3, 0.05);
            }

            // CRASH PHASE: ticks 105-145, body crashes back down (-Y8)
            if (tick > 105 && tick <= 145) {
                float crashProgress = (tick - 105) / 40f;
                float crashY = 8f - 10f * crashProgress; // 8 -> -2
                if (tick % 2 == 0) {
                    translateAll(crashY);
                }
                if (tick == 110) DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.6f, 0.4f);

                if (tick == 145 && !crashDamageDone) {
                    crashDamageDone = true;
                    applyAreaDamage(c.clone(), 12.0, 260.0);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.4f);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone(), 200, 7, 1, 7, 0.4);
                    c.getWorld().spawnParticle(Particle.CLOUD, c.clone(), 100, 7, 1, 7, 0.3);
                    c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone(),
                            80, 7, 0.3, 7, 0.05, Material.WHITE_CONCRETE.createBlockData());
                    DisplayBuilder.dustParticles(c.clone(), 100, 6.5, 240, 245, 250, 1.8f);
                }
            }

            // DISSIPATE PHASE: 145-170 — shrink everything
            if (tick == 150) {
                shrinkGroup(allBlocks);
            }
        }

        private void translateAll(float dropY) {
            for (BlockDisplayHandle h : allBlocks) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
        }

        private void shrinkGroup(List<BlockDisplayHandle> group) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(15);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
        }

        private void applyAreaDamage(Location loc, double radius, double damage) {
            if (loc.getWorld() == null) return;
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL) continue;
                if (p.getLocation().distanceSquared(loc) <= radius * radius) {
                    p.damage(damage);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenLeviathanBreach(plugin); }
    }

    // ================================================================
    // #74 — ABSOLUTE ZERO CHAMBER
    // 41 blocks: 6 TINTED_GLASS wall panels (hex cell), 6 PACKED_ICE corner
    // struts, 6 BLUE_ICE floor-bases, 6 LIGHT_BLUE_CONCRETE ceiling caps,
    // 5 DIAMOND_BLOCK central pillar (stacked), 12 BLUE_STAINED_GLASS frost-vents.
    // Spawn 25t: floor → walls → ceiling. Active 220t: pillar pulses, vents emit,
    // walls slowly contract inward. Dissipate 25t: walls explode outward.
    // Constant radius 5.5, 7 hearts dmg/10t.
    // ================================================================
    public static class AbsoluteZeroChamber extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallPanels = new ArrayList<>();
        private final List<BlockDisplayHandle> struts = new ArrayList<>();
        private final List<BlockDisplayHandle> floorBases = new ArrayList<>();
        private final List<BlockDisplayHandle> ceilingCaps = new ArrayList<>();
        private final List<BlockDisplayHandle> pillar = new ArrayList<>();
        private final List<BlockDisplayHandle> vents = new ArrayList<>();
        private final double[][] wallAnchorPositions = new double[6][2];
        private float pillarPulse = 0f;
        private float wallContraction = 0f;

        public AbsoluteZeroChamber(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("absolute_zero_chamber", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1680.0);
            config.setDamageRadius(8.25);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 6 floor bases (BLUE_ICE) — appear first (delay 0)
            double floorRadius = 2.4;
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6;
                double fx = Math.cos(a) * floorRadius;
                double fz = Math.sin(a) * floorRadius;
                Location loc = center.clone().add(fx, -0.5, fz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 180, 230).interpolation(6, i);
                spawnedEntities.add(h.entity());
                floorBases.add(h);
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(6);
                e.setInterpolationDelay(i);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f((float) a, 0f, 1f, 0f),
                        new Vector3f(1.5f, 0.2f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // 6 wall panels (TINTED_GLASS) — appear after floor (delay 8-13)
            double wallRadius = 2.6;
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6;
                wallAnchorPositions[i][0] = Math.cos(a) * wallRadius;
                wallAnchorPositions[i][1] = Math.sin(a) * wallRadius;
                Location loc = center.clone().add(wallAnchorPositions[i][0], 1.5, wallAnchorPositions[i][1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(170, 220, 255).interpolation(8, 8 + i);
                spawnedEntities.add(h.entity());
                wallPanels.add(h);
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(8);
                e.setInterpolationDelay(8 + i);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f((float) a, 0f, 1f, 0f),
                        new Vector3f(2.5f, 3.0f, 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // 6 corner struts (PACKED_ICE) — at wall corners (delay 10)
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6 + Math.PI / 6;
                double sx = Math.cos(a) * wallRadius * 1.05;
                double sz = Math.sin(a) * wallRadius * 1.05;
                Location loc = center.clone().add(sx, 1.5, sz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(160, 200, 230).interpolation(8, 10 + i);
                spawnedEntities.add(h.entity());
                struts.add(h);
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(8);
                e.setInterpolationDelay(10 + i);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.5f, 3.5f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // 6 ceiling caps (LIGHT_BLUE_CONCRETE) — appear last (delay 18-23)
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6;
                double cx = Math.cos(a) * floorRadius;
                double cz = Math.sin(a) * floorRadius;
                Location loc = center.clone().add(cx, 3.2, cz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_CONCRETE);
                h.scale(0.0f, 0.0f, 0.0f).glow(180, 220, 255).interpolation(7, 18 + i);
                spawnedEntities.add(h.entity());
                ceilingCaps.add(h);
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(7);
                e.setInterpolationDelay(18 + i);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f((float) a, 0f, 1f, 0f),
                        new Vector3f(1.5f, 0.2f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // 5 DIAMOND_BLOCK central pillar (stacked Y0..Y2.4)
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 0.1 + i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 240, 255).interpolation(6, 5 + i);
                spawnedEntities.add(h.entity());
                pillar.add(h);
                growBlock(h, 0.6f, 6, 5 + i);
            }

            // 12 BLUE_STAINED_GLASS frost-vents (on interior of walls)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 * i) / 12;
                double r = wallRadius - 0.3;
                double vx = Math.cos(a) * r;
                double vz = Math.sin(a) * r;
                double vy = 0.6 + (i % 2) * 1.6;
                Location loc = center.clone().add(vx, vy, vz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 160, 220).interpolation(5, 20 + i);
                spawnedEntities.add(h.entity());
                vents.add(h);
                growBlock(h, 0.3f, 5, 20 + i);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1.5, 0), 60, 2.5, 1.5, 2.5, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float target, int duration, int delay) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(delay);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(target, target, target),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // SOUL_FIRE_FLAME at core (pillar)
            if (tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 1.5, 0), 4, 0.4, 1.2, 0.4, 0.02);
            }
            // Dense SNOWFLAKE interior
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 2.0;
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(Math.cos(a) * r, Math.random() * 3, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // ELECTRIC_SPARK on pillar
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.5, 0), 5, 0.3, 1.0, 0.3, 0.05);
            }
            // FALLING_DUST from vents
            if (tick % 4 == 0) {
                for (BlockDisplayHandle v : vents) {
                    if (Math.random() < 0.4) {
                        c.getWorld().spawnParticle(Particle.FALLING_DUST,
                                v.entity().getLocation(), 3, 0.2, 0.1, 0.2, 0.05,
                                Material.WHITE_CONCRETE.createBlockData());
                    }
                }
            }

            // Central pillar pulses (scale 0.6 ↔ 0.75)
            if (tick > 30 && tick < 260 && tick % 5 == 0) {
                pillarPulse += 0.1f;
                float ps = 0.65f + 0.1f * (float) Math.sin(pillarPulse);
                for (BlockDisplayHandle h : pillar) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(ps, ps, ps),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Frost-vents periodically emit (scale-pulse + particle burst)
            if (tick > 30 && tick < 260 && tick % 25 == 0) {
                for (BlockDisplayHandle v : vents) {
                    BlockDisplay e = v.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.5f, 0.5f, 0.5f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, e.getLocation(), 12, 0.4, 0.4, 0.4, 0.1);
                }
                // Schedule shrink-back
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    for (BlockDisplayHandle v : vents) {
                        BlockDisplay e = v.entity();
                        if (e.isDead()) continue;
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(4);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(0.3f, 0.3f, 0.3f),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }, 8L);
            }

            // Walls contract slowly inward (0.005/tick → max ~1.0 over 220t)
            if (tick > 30 && tick < 250 && tick % 4 == 0) {
                wallContraction += 0.02f;
                if (wallContraction > 0.9f) wallContraction = 0.9f;
                for (int i = 0; i < wallPanels.size(); i++) {
                    BlockDisplayHandle h = wallPanels.get(i);
                    double a = (Math.PI * 2 * i) / 6;
                    float wx = (float) (wallAnchorPositions[i][0] * (1.0 - wallContraction * 0.3));
                    float wz = (float) (wallAnchorPositions[i][1] * (1.0 - wallContraction * 0.3));
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(wx - 0.5f, -0.5f, wz - 0.5f),
                            new AxisAngle4f((float) a, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Chamber hum
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.9f, 0.4f);
            }
            if (tick == 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.6f);
            }

            // Dissipate 250-280: walls explode outward, ceiling falls
            if (tick == 250) {
                // Explode walls outward
                for (int i = 0; i < wallPanels.size(); i++) {
                    BlockDisplayHandle h = wallPanels.get(i);
                    double a = (Math.PI * 2 * i) / 6;
                    float ex = (float) (Math.cos(a) * 6.0);
                    float ez = (float) (Math.sin(a) * 6.0);
                    BlockDisplay e = h.entity();
                    e.setInterpolationDuration(20);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(ex - 0.5f, -0.5f, ez - 0.5f),
                            new AxisAngle4f((float) a, 0f, 1f, 0f),
                            new Vector3f(0.5f, 0.5f, 0.5f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 150, 4, 2, 4, 0.3);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 80, 4.0, 170, 220, 255, 1.6f);
            }
            if (tick == 255) {
                // Ceiling falls
                for (BlockDisplayHandle h : ceilingCaps) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(15);
                    e.setInterpolationDelay(0);
                    Vector3f tr = t.getTranslation();
                    e.setTransformation(new Transformation(
                            new Vector3f(tr.x, tr.y - 3.5f, tr.z),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }
            if (tick == 270) {
                shrinkGroup(struts);
                shrinkGroup(vents);
                shrinkGroup(pillar);
                shrinkGroup(floorBases);
            }
        }

        private void shrinkGroup(List<BlockDisplayHandle> group) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(10);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new AbsoluteZeroChamber(plugin); }
    }

    // ================================================================
    // #75 — ENDER GLACIER VOID PORTAL (CLIMACTIC FINALE)
    // 62 blocks: 1 OBSIDIAN central void-disc (scale 2.0), 12 PACKED_ICE
    // ring-rim (radius 3.5), 8 BLUE_ICE outer-rim (radius 4), 12 DIAMOND_BLOCK
    // ice-shards pointing inward (radial spikes), 4 TINTED_GLASS reality-fractures
    // (floating fragments above), 8 BLUE_STAINED_GLASS emerging-tendrils,
    // 8 CALCITE floor anchors, 4 AMETHYST_BLOCK corner-glow runes,
    // 4 PURPLE_STAINED_GLASS frame stabilizers + 1 OBSIDIAN inner shadow.
    // Spawn 35t: point-of-darkness → ring expands → void opens (warning).
    // Active 260t: portal swirls (8°/tick), shards pulse, tendrils reach.
    // Pull-pulse every 50t: pulls + damage. Dissipate 25t: implodes.
    // Constant radius 11.5, 7 hearts/10t (the most damaging attack).
    // ================================================================
    public static class EnderGlacierVoidPortal extends BlockDisplayAttack {
        private BlockDisplayHandle voidDisc;
        private BlockDisplayHandle innerShadow;
        private final List<BlockDisplayHandle> ringRim = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRim = new ArrayList<>();
        private final List<BlockDisplayHandle> iceShards = new ArrayList<>();
        private final List<BlockDisplayHandle> fractures = new ArrayList<>();
        private final List<BlockDisplayHandle> tendrils = new ArrayList<>();
        private final List<BlockDisplayHandle> floorAnchors = new ArrayList<>();
        private final List<BlockDisplayHandle> runes = new ArrayList<>();
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private float portalYaw = 0f;
        private int lastPullPulse = -100;

        public EnderGlacierVoidPortal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ender_glacier_void_portal", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1680.0);
            config.setDamageRadius(17.25);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(330);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 1 OBSIDIAN central void-disc (scale 2.0) — starts as point
            Location voidLoc = center.clone().add(0, 0.5, 0);
            voidDisc = displayBuilder.spawnBlock(voidLoc, Material.OBSIDIAN);
            voidDisc.scale(0.0f, 0.0f, 0.0f).glow(20, 5, 30).interpolation(20, 0);
            spawnedEntities.add(voidDisc.entity());
            growBlock(voidDisc, 2.0f, 20, 0);

            // 1 OBSIDIAN inner shadow (slightly smaller, layered behind for depth)
            innerShadow = displayBuilder.spawnBlock(center.clone().add(0, 0.4, 0), Material.OBSIDIAN);
            innerShadow.scale(0.0f, 0.0f, 0.0f).glow(40, 20, 60).interpolation(20, 0);
            spawnedEntities.add(innerShadow.entity());
            growBlock(innerShadow, 1.4f, 20, 5);

            // 12 PACKED_ICE ring-rim at radius 3.5
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 * i) / 12;
                double rx = Math.cos(a) * 3.5;
                double rz = Math.sin(a) * 3.5;
                Location loc = center.clone().add(rx, 0.5, rz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(160, 200, 230).interpolation(8, 12 + i);
                spawnedEntities.add(h.entity());
                ringRim.add(h);
                growBlock(h, 0.6f, 8, 12 + i);
            }

            // 8 BLUE_ICE outer-rim at radius 4
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8 + Math.PI / 8;
                double rx = Math.cos(a) * 4.0;
                double rz = Math.sin(a) * 4.0;
                Location loc = center.clone().add(rx, 0.5, rz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 180, 230).interpolation(8, 20 + i);
                spawnedEntities.add(h.entity());
                outerRim.add(h);
                growBlock(h, 0.8f, 8, 20 + i);
            }

            // 12 DIAMOND_BLOCK ice shards pointing inward (radial spikes)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 * i) / 12 + Math.PI / 12;
                double rx = Math.cos(a) * 2.5;
                double rz = Math.sin(a) * 2.5;
                Location loc = center.clone().add(rx, 0.5, rz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 240, 255).interpolation(7, 15 + i);
                spawnedEntities.add(h.entity());
                iceShards.add(h);
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(7);
                e.setInterpolationDelay(15 + i);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f((float) (a + Math.PI / 2), 0f, 1f, 0f),
                        new Vector3f(0.3f, 0.3f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // 4 TINTED_GLASS reality-fractures (floating fragments above)
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 * i) / 4 + Math.PI / 6;
                double fx = Math.cos(a) * 1.5;
                double fz = Math.sin(a) * 1.5;
                double fy = 2.0 + Math.random() * 1.5;
                Location loc = center.clone().add(fx, fy, fz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(170, 220, 255).interpolation(8, 22 + i * 2);
                spawnedEntities.add(h.entity());
                fractures.add(h);
                growBlock(h, 0.3f, 8, 22 + i * 2);
            }

            // 8 BLUE_STAINED_GLASS emerging tendrils reaching out from void
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8;
                double tx = Math.cos(a) * 1.2;
                double tz = Math.sin(a) * 1.2;
                Location loc = center.clone().add(tx, 1.0, tz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 160, 220).interpolation(6, 25 + i);
                spawnedEntities.add(h.entity());
                tendrils.add(h);
                BlockDisplay e = h.entity();
                e.setInterpolationDuration(6);
                e.setInterpolationDelay(25 + i);
                e.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f((float) a, 0f, 1f, 0f),
                        new Vector3f(0.3f, 0.3f, 1.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // 8 CALCITE floor anchors
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8;
                double cx = Math.cos(a) * 4.6;
                double cz = Math.sin(a) * 4.6;
                Location loc = center.clone().add(cx, -0.2, cz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 230, 240).interpolation(6, 10 + i);
                spawnedEntities.add(h.entity());
                floorAnchors.add(h);
                growBlock(h, 0.4f, 6, 10 + i);
            }

            // 4 AMETHYST_BLOCK corner-glow runes
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 * i) / 4 + Math.PI / 4;
                double rx = Math.cos(a) * 5.2;
                double rz = Math.sin(a) * 5.2;
                Location loc = center.clone().add(rx, 0.4, rz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(180, 130, 220).interpolation(7, 26 + i * 2);
                spawnedEntities.add(h.entity());
                runes.add(h);
                growBlock(h, 0.4f, 7, 26 + i * 2);
            }

            // 4 PURPLE_STAINED_GLASS frame stabilizers (cardinal cross marks)
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 * i) / 4;
                double sx = Math.cos(a) * 4.6;
                double sz = Math.sin(a) * 4.6;
                Location loc = center.clone().add(sx, 1.2, sz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 80, 200).interpolation(7, 24 + i * 2);
                spawnedEntities.add(h.entity());
                frame.add(h);
                growBlock(h, 0.3f, 7, 24 + i * 2);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.6f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.6f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.6f, 0.4f);
            w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 0.5, 0), 100, 3, 0.5, 3, 0.1);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 0.5, 0), 60, 2.5, 0.5, 2.5, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float target, int duration, int delay) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(delay);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(target, target, target),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Very dense SCULK_SOUL at void center
            if (tick % 2 == 0) {
                for (int i = 0; i < 14; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 2.5;
                    c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                            c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 0.6, Math.sin(a) * r),
                            1, 0.05, 0.05, 0.05, 0.04);
                }
            }
            // ELECTRIC_SPARK on rim
            if (tick % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            c.clone().add(Math.cos(a) * 3.5, 0.6, Math.sin(a) * 3.5),
                            2, 0.15, 0.15, 0.15, 0.05);
                }
            }
            // SOUL_FIRE_FLAME along tendrils
            if (tick % 3 == 0) {
                for (BlockDisplayHandle ten : tendrils) {
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            ten.entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.03);
                }
            }
            // GLOW on shards
            if (tick % 3 == 0) {
                for (BlockDisplayHandle sh : iceShards) {
                    c.getWorld().spawnParticle(Particle.GLOW,
                            sh.entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                }
            }
            // REVERSE_PORTAL pull effect at center
            if (tick % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 5.0 - Math.random() * 4.5; // converging inward
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                            c.clone().add(Math.cos(a) * r, 0.6 + Math.random() * 2, Math.sin(a) * r),
                            1, 0.05, 0.05, 0.05, 0.08);
                }
            }

            // Portal swirls (rim rotates Y-axis at 8°/tick)
            portalYaw += (float) Math.toRadians(8.0);
            if (tick > 35 && tick % 2 == 0) {
                rotateGroup(ringRim, portalYaw, 0f, 1f, 0f);
                rotateGroup(outerRim, portalYaw * 0.7f, 0f, 1f, 0f);
                rotateGroup(runes, portalYaw * 0.5f, 0f, 1f, 0f);
                rotateGroup(frame, portalYaw * 0.3f, 0f, 1f, 0f);
                // Shards: rotate while preserving their radial pointing direction
                for (int i = 0; i < iceShards.size(); i++) {
                    BlockDisplayHandle sh = iceShards.get(i);
                    double baseAngle = (Math.PI * 2 * i) / 12 + Math.PI / 12;
                    float totalAngle = (float) (baseAngle + Math.PI / 2) + portalYaw;
                    BlockDisplay e = sh.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(totalAngle, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Ice shards pulse-extend/retract (scale Z 1.2 ↔ 1.8)
            if (tick > 45 && tick < 295 && tick % 6 == 0) {
                float zPulse = 1.5f + 0.3f * (float) Math.sin(tick * 0.18);
                for (int i = 0; i < iceShards.size(); i++) {
                    BlockDisplayHandle sh = iceShards.get(i);
                    double baseAngle = (Math.PI * 2 * i) / 12 + Math.PI / 12;
                    float totalAngle = (float) (baseAngle + Math.PI / 2) + portalYaw;
                    BlockDisplay e = sh.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(totalAngle, 0f, 1f, 0f),
                            new Vector3f(0.3f, 0.3f, zPulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Tendrils reach out toward player (extend Z 1.0 → 1.8 cycling)
            if (tick > 45 && tick < 295 && tick % 5 == 0) {
                float tendZ = 1.4f + 0.4f * (float) Math.sin(tick * 0.12);
                for (int i = 0; i < tendrils.size(); i++) {
                    BlockDisplayHandle ten = tendrils.get(i);
                    double a = (Math.PI * 2 * i) / 8;
                    BlockDisplay e = ten.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f((float) a, 0f, 1f, 0f),
                            new Vector3f(0.3f, 0.3f, tendZ),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Reality fractures float and rotate
            if (tick > 45 && tick < 295 && tick % 6 == 0) {
                for (int i = 0; i < fractures.size(); i++) {
                    BlockDisplayHandle h = fractures.get(i);
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    float bob = (float) Math.sin((tick + i * 13) * 0.1) * 0.4f;
                    Vector3f tr = t.getTranslation();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(tr.x, tr.y + bob * 0.05f, tr.z),
                            new AxisAngle4f((float) (tick * 0.04), 1f, 0.5f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Pull-pulse every 50t (after activation): visual pull + bonus damage
            if (tick > 60 && tick < 295 && tick - lastPullPulse >= 50) {
                lastPullPulse = tick;
                // Visual pull: spawn dense converging particles from periphery to center
                for (int i = 0; i < 30; i++) {
                    double a = Math.random() * Math.PI * 2;
                    Location start = c.clone().add(Math.cos(a) * 8.0, 0.5 + Math.random() * 2, Math.sin(a) * 8.0);
                    Location end = c.clone().add(0, 0.5, 0);
                    DisplayBuilder.particleLine(start, end, Particle.REVERSE_PORTAL, 6, null);
                }
                applyAreaDamage(c.clone(), 11.5, 100.0);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.4f);
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 0.5, 0), 80, 3, 0.5, 3, 0.15);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 60, 5.0, 140, 80, 200, 1.6f);
            }

            // Deep cosmic drone
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.4f);
            }

            // Dissipate 295-330: void implodes (suck all inward then vanish)
            if (tick == 295) {
                // All blocks animate toward center then shrink
                for (BlockDisplayHandle h : ringRim) implodeBlock(h);
                for (BlockDisplayHandle h : outerRim) implodeBlock(h);
                for (BlockDisplayHandle h : iceShards) implodeBlock(h);
                for (BlockDisplayHandle h : fractures) implodeBlock(h);
                for (BlockDisplayHandle h : tendrils) implodeBlock(h);
                for (BlockDisplayHandle h : floorAnchors) implodeBlock(h);
                for (BlockDisplayHandle h : runes) implodeBlock(h);
                for (BlockDisplayHandle h : frame) implodeBlock(h);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 0.4f);
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 0.5, 0), 200, 5, 1, 5, 0.4);
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 0.5, 0), 120, 4, 1, 4, 0.2);
            }
            if (tick == 315) {
                // Final implode of central void
                BlockDisplay e1 = voidDisc.entity();
                BlockDisplay e2 = innerShadow.entity();
                Transformation t1 = e1.getTransformation();
                Transformation t2 = e2.getTransformation();
                e1.setInterpolationDuration(15);
                e1.setInterpolationDelay(0);
                e1.setTransformation(new Transformation(
                        t1.getTranslation(),
                        new AxisAngle4f().set(t1.getLeftRotation()),
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f().set(t1.getRightRotation())
                ));
                e2.setInterpolationDuration(15);
                e2.setInterpolationDelay(0);
                e2.setTransformation(new Transformation(
                        t2.getTranslation(),
                        new AxisAngle4f().set(t2.getLeftRotation()),
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f().set(t2.getRightRotation())
                ));
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
            }
        }

        private void implodeBlock(BlockDisplayHandle h) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(20);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void rotateGroup(List<BlockDisplayHandle> group, float angle, float ax, float ay, float az) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, ax, ay, az),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        private void applyAreaDamage(Location loc, double radius, double damage) {
            if (loc.getWorld() == null) return;
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL) continue;
                if (p.getLocation().distanceSquared(loc) <= radius * radius) {
                    p.damage(damage);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new EnderGlacierVoidPortal(plugin); }
    }
}
