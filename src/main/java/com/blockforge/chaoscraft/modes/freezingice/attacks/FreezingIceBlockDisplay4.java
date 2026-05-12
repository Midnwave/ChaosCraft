package com.blockforge.chaoscraft.modes.freezingice.attacks;

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
 * FreezingIce — BLOCK DISPLAY ATTACKS 31-40 (Geometric / Abstract).
 *
 * Pure-geometry ice horror — every shape uses ACCURATE math (regular
 * polyhedra vertices, Koch fractal, parametric Möbius, logarithmic spiral,
 * tesseract 4D projection, Penrose impossible triangle).
 *
 * Each structure is built with 30+ BlockDisplays interpolated via
 * Transformation. All attacks spawn at fixed center (no follow-AI),
 * constant-radius damage, ice/cold palette.
 *
 * Ice palette:
 *  - PACKED_ICE       light blue 150,210,240
 *  - BLUE_ICE         deeper blue 110,180,230
 *  - ICE              soft cyan 170,220,255
 *  - TINTED_GLASS     muted purple 60,40,80 (translucent)
 *  - BLUE_STAINED_GLASS deep cyan 80,140,220
 *  - AMETHYST_BLOCK   crystal purple 160,120,220
 *  - QUARTZ_BLOCK     pure white 240,240,240
 *  - DIAMOND_BLOCK    icy white-cyan 220,255,255
 *  - WHITE_CONCRETE   bone white 245,245,245
 *  - CALCITE          opal-grey 220,220,230
 *
 * Particles: SNOWFLAKE, ELECTRIC_SPARK, GLOW, END_ROD, SCULK_SOUL.
 * Sounds:    BLOCK_AMETHYST_BLOCK_CHIME, BLOCK_GLASS_BREAK, BLOCK_GLASS_PLACE,
 *            ENTITY_GLOW_SQUID_AMBIENT.
 *
 * Attacks:
 *  31. FrostDodecahedron       — 62 blocks, true 20-vertex regular dodecahedron
 *  32. HexagonalLatticePrism   — 62 blocks, stacked 6-fold prism w/ pulsing columns
 *  33. SnowflakeFractalMandala — 67 blocks, 6-axis Koch-style fractal w/ breathing
 *  34. IceSpiralGalaxy         — 46 blocks, two logarithmic spiral arms + halo
 *  35. PrismaticCube           — 32 blocks, faces + edges + inner mini-cubes
 *  36. MobiusIceStrip          — 36 blocks, parametric Möbius strip (180° twist)
 *  37. TesseractCubeHyper      — 48 blocks, projected 4D cube-in-cube
 *  38. KleinIceBottle          — 38 blocks, self-intersecting topological curve
 *  39. SierpinskiTetrahedron   — 36 blocks, 3-level fractal pyramid
 *  40. PenroseTriangleIllusion — 39 blocks, impossible-triangle 3-beam figure
 */
public final class FreezingIceBlockDisplay4 {
    private FreezingIceBlockDisplay4() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    // Golden ratio for regular dodecahedron / icosahedron vertex coordinates
    private static final double PHI = (1.0 + Math.sqrt(5.0)) / 2.0;
    private static final double INV_PHI = 1.0 / PHI;

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FrostDodecahedron(plugin));
        registry.register(new HexagonalLatticePrism(plugin));
        registry.register(new SnowflakeFractalMandala(plugin));
        registry.register(new IceSpiralGalaxy(plugin));
        registry.register(new PrismaticCube(plugin));
        registry.register(new MobiusIceStrip(plugin));
        registry.register(new TesseractCubeHyper(plugin));
        registry.register(new KleinIceBottle(plugin));
        registry.register(new SierpinskiTetrahedron(plugin));
        registry.register(new PenroseTriangleIllusion(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    /** Translate a single block display (interpolated). */
    private static void translate(BlockDisplay e, float x, float y, float z, int dur) {
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                new AxisAngle4f().set(t.getLeftRotation()),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    /** Rotate one block display around an axis while preserving translation/scale. */
    private static void setRotation(BlockDisplay e, float angle, float ax, float ay, float az, int dur) {
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    /** Set translation + rotation + scale fully (no relative read). */
    private static void setFull(BlockDisplay e,
                                 Vector3f trans, AxisAngle4f rot, Vector3f scale, int dur) {
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(trans, rot, scale, new AxisAngle4f(0, 0, 1, 0)));
    }

    /** Apply scale only. */
    private static void rescale(BlockDisplay e, float sx, float sy, float sz, int dur) {
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    /** Spawn a block aligned to (oriented along) a direction vector by yaw+pitch. */
    private static void orientAlong(BlockDisplay e, double dx, double dy, double dz) {
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.atan2(dz, dx);
        float pitch = (float) Math.atan2(dy, horiz);
        Transformation t = e.getTransformation();
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(yaw, 0f, 1f, 0f),
                t.getScale(),
                new AxisAngle4f(-pitch, (float) Math.sin(yaw), 0f, (float) Math.cos(yaw))
        ));
    }

    // ================================================================
    // #31 — FROST DODECAHEDRON
    // 62 blocks: 20 vertex caps DIAMOND (golden-ratio coords), 30 edge bars
    // BLUE_ICE, 12 pentagonal face panels TINTED_GLASS centered on face normals.
    // Materializes from center outward over 30t. Tumbles on Y + X axes during
    // active. Damage radius 8.5, 120 dmg / 10t (30t delay).
    // ================================================================
    public static class FrostDodecahedron extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> vertices = new ArrayList<>();
        private final List<BlockDisplayHandle> edges = new ArrayList<>();
        private final List<BlockDisplayHandle> faces = new ArrayList<>();
        private final List<double[]> vertexCoords = new ArrayList<>();
        private float rotY = 0f;
        private float rotX = 0f;

        public FrostDodecahedron(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_dodecahedron", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(8.5);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(120);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 20 vertices of a regular dodecahedron, all permutations of (±1, ±1, ±1)
            // plus cyclic permutations of (0, ±1/φ, ±φ). Scaled by 2.6 for arena size.
            double s = 2.6;
            double a = 1.0;
            double b = INV_PHI;
            double c = PHI;
            double[][] base = new double[][]{
                    {a, a, a}, {a, a, -a}, {a, -a, a}, {a, -a, -a},
                    {-a, a, a}, {-a, a, -a}, {-a, -a, a}, {-a, -a, -a},
                    {0, b, c}, {0, b, -c}, {0, -b, c}, {0, -b, -c},
                    {b, c, 0}, {b, -c, 0}, {-b, c, 0}, {-b, -c, 0},
                    {c, 0, b}, {c, 0, -b}, {-c, 0, b}, {-c, 0, -b}
            };
            for (double[] v : base) {
                double[] sc = {v[0] * s, v[1] * s + 3.5, v[2] * s};
                vertexCoords.add(sc);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(sc[0], sc[1], sc[2]), Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(220, 255, 255).interpolation(30, 0);
                spawnedEntities.add(h.entity());
                vertices.add(h);
                rescale(h.entity(), 0.45f, 0.45f, 0.45f, 30);
            }

            // 30 edges of a regular dodecahedron — pairs of vertices at edge length 2/φ
            double edgeLen = 2.0 * INV_PHI * s;
            double edgeTol = edgeLen * 0.05;
            for (int i = 0; i < vertexCoords.size(); i++) {
                for (int j = i + 1; j < vertexCoords.size(); j++) {
                    double[] v1 = vertexCoords.get(i);
                    double[] v2 = vertexCoords.get(j);
                    double dx = v2[0] - v1[0];
                    double dy = v2[1] - v1[1];
                    double dz = v2[2] - v1[2];
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (Math.abs(d - edgeLen) < edgeTol) {
                        double mx = (v1[0] + v2[0]) / 2.0;
                        double my = (v1[1] + v2[1]) / 2.0;
                        double mz = (v1[2] + v2[2]) / 2.0;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(mx, my, mz), Material.BLUE_ICE);
                        h.scale(0.0f, 0.0f, 0.0f).glow(110, 180, 230).interpolation(30, 0);
                        orientAlong(h.entity(), dx, dy, dz);
                        rescale(h.entity(), 0.18f, 0.18f, (float) d, 30);
                        spawnedEntities.add(h.entity());
                        edges.add(h);
                    }
                }
            }

            // 12 pentagonal face panels — face center is average of the 3 adjacent vertices
            // along each (0, ±b, ±c) cyclic axis. We use 12 axis-aligned face normals.
            double r = c * s * 0.78; // face-center distance
            double[][] faceCenters = new double[][]{
                    { 0,  b * r, c * r}, { 0,  b * r, -c * r}, { 0, -b * r,  c * r}, { 0, -b * r, -c * r},
                    { b * r,  c * r, 0}, { b * r, -c * r, 0}, {-b * r,  c * r, 0}, {-b * r, -c * r, 0},
                    { c * r,  0,  b * r}, { c * r, 0, -b * r}, {-c * r,  0,  b * r}, {-c * r, 0, -b * r}
            };
            for (double[] fc : faceCenters) {
                double fx = fc[0];
                double fy = fc[1] + 3.5;
                double fz = fc[2];
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(fx, fy, fz), Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(60, 40, 80).interpolation(30, 0);
                // Orient panel facing outward (normal = fc dir)
                orientAlong(h.entity(), fc[0], fc[1], fc[2]);
                rescale(h.entity(), 1.2f, 1.2f, 0.08f, 30);
                spawnedEntities.add(h.entity());
                faces.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.8f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 3.5, 0), 50, 3, 3, 3, 0.1);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, 3.5, 0), 30, 2, 2, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotY += (float) Math.toRadians(3.0);
            rotX += (float) Math.toRadians(1.5);

            // Composite rotation applied to all blocks by re-deriving their world position
            // from the original vertex coords. Vertices/edges/faces rotated as a rigid body
            // by setting translation = R(rotX,rotY) · originalOffset.
            if (tick % 2 == 0) {
                applyRigidRotation();
            }

            // Particles along edges
            if (tick % 3 == 0) {
                for (int i = 0; i < edges.size(); i += 3) {
                    BlockDisplay e = edges.get(i).entity();
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            e.getLocation(), 1, 0.15, 0.15, 0.15, 0.02);
                }
            }
            if (tick % 4 == 0) {
                for (int i = 0; i < vertices.size(); i += 4) {
                    BlockDisplay e = vertices.get(i).entity();
                    c.getWorld().spawnParticle(Particle.GLOW,
                            e.getLocation(), 2, 0.1, 0.1, 0.1, 0.0);
                }
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 3.5, 0), 8, 4, 3, 4, 0.05);
            }

            // Dissipate: ticks 220-240 collapse inward
            if (tick >= 220 && tick < 240) {
                if (tick == 220) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.7f);
                for (BlockDisplayHandle h : vertices) translate(h.entity(), 0, 3.5f, 0, 20);
                for (BlockDisplayHandle h : edges) rescale(h.entity(), 0f, 0f, 0f, 20);
                for (BlockDisplayHandle h : faces) rescale(h.entity(), 0f, 0f, 0f, 20);
            }

            if (tick % 40 == 0 && tick < 220) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
            }
        }

        private void applyRigidRotation() {
            // Rotate each vertex by current angles and set its translation.
            float cosY = (float) Math.cos(rotY), sinY = (float) Math.sin(rotY);
            float cosX = (float) Math.cos(rotX), sinX = (float) Math.sin(rotX);
            for (int i = 0; i < vertices.size(); i++) {
                double[] v = vertexCoords.get(i);
                double y0 = v[1] - 3.5;
                // Y rotation
                double x1 = v[0] * cosY + v[2] * sinY;
                double z1 = -v[0] * sinY + v[2] * cosY;
                // X rotation
                double y1 = y0 * cosX - z1 * sinX;
                double z2 = y0 * sinX + z1 * cosX;
                BlockDisplay e = vertices.get(i).entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f((float) x1 - 0.5f, (float) y1 + 3.5f - 0.5f, (float) z2 - 0.5f),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
            // For edges/faces we apply a simple shared rotation (cheaper, looks fine)
            AxisAngle4f rot = new AxisAngle4f(rotY, 0f, 1f, 0f);
            for (BlockDisplayHandle h : edges) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(), rot, t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
            for (BlockDisplayHandle h : faces) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(), rot, t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostDodecahedron(plugin); }
    }

    // ================================================================
    // #32 — HEXAGONAL LATTICE PRISM
    // 62 blocks: 5 hex layers × 6 PACKED_ICE nodes (30) + 6 columns × 5 BLUE_ICE
    // segments (30) + 2 TINTED_GLASS hex caps. Bottom-up assembly, rotates Y,
    // columns pulse radially. Constant 8.0r, 110 dmg/12t (30t delay).
    // ================================================================
    public static class HexagonalLatticePrism extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> rings = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> columns = new ArrayList<>();
        private final List<BlockDisplayHandle> caps = new ArrayList<>();
        private float rotY = 0f;

        public HexagonalLatticePrism(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hexagonal_lattice_prism", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(200);
            config.setCooldownTicks(120);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double hexR = 3.4;
            double layerH = 1.4;
            // 6 column slots
            for (int i = 0; i < 6; i++) columns.add(new ArrayList<>());

            // 5 ring layers
            for (int layer = 0; layer < 5; layer++) {
                List<BlockDisplayHandle> ring = new ArrayList<>();
                double y = layer * layerH;
                for (int v = 0; v < 6; v++) {
                    double a = 2 * Math.PI * v / 6.0;
                    double x = Math.cos(a) * hexR;
                    double z = Math.sin(a) * hexR;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, z), Material.PACKED_ICE);
                    h.scale(0f, 0f, 0f).glow(150, 210, 240).interpolation(12, layer * 4);
                    rescale(h.entity(), 0.4f, 0.4f, 0.4f, 12);
                    spawnedEntities.add(h.entity());
                    ring.add(h);
                }
                rings.add(ring);
            }

            // 6 columns × 5 segments — between consecutive layers
            for (int v = 0; v < 6; v++) {
                double a = 2 * Math.PI * v / 6.0;
                double x = Math.cos(a) * hexR;
                double z = Math.sin(a) * hexR;
                for (int seg = 0; seg < 5; seg++) {
                    double y = seg * layerH + layerH * 0.5;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, z), Material.BLUE_ICE);
                    h.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(12, seg * 4 + 2);
                    rescale(h.entity(), 0.2f, (float) layerH * 0.95f, 0.2f, 12);
                    spawnedEntities.add(h.entity());
                    columns.get(v).add(h);
                }
            }

            // Top + bottom hex cap centers
            for (int t = 0; t < 2; t++) {
                double y = (t == 0) ? -0.1 : (4 * layerH + 0.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.TINTED_GLASS);
                h.scale(0f, 0f, 0f).glow(60, 40, 80).interpolation(12, 18);
                rescale(h.entity(), (float) hexR * 0.9f, 0.15f, (float) hexR * 0.9f, 12);
                spawnedEntities.add(h.entity());
                caps.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.3f, 0.9f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 3.5, 0), 35, 3.5, 3, 3.5, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotY += (float) Math.toRadians(5.0);

            // Rotate all blocks around Y
            if (tick % 2 == 0) {
                AxisAngle4f rot = new AxisAngle4f(rotY, 0f, 1f, 0f);
                for (List<BlockDisplayHandle> ring : rings) {
                    for (BlockDisplayHandle h : ring) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(t.getTranslation(), rot, t.getScale(), new AxisAngle4f(0, 0, 1, 0)));
                    }
                }
                for (List<BlockDisplayHandle> col : columns) {
                    for (BlockDisplayHandle h : col) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(t.getTranslation(), rot, t.getScale(), new AxisAngle4f(0, 0, 1, 0)));
                    }
                }
            }

            // Column pulse wave — radial scale 0.2 -> 0.32 -> 0.2 in 30t cycle
            if (tick % 3 == 0 && tick > 24) {
                for (int v = 0; v < columns.size(); v++) {
                    double phase = (tick * 0.15) + v * 1.05;
                    float pulse = 0.2f + 0.12f * (float) Math.sin(phase);
                    for (BlockDisplayHandle h : columns.get(v)) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        Vector3f sc = t.getScale();
                        e.setInterpolationDuration(3);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(pulse, sc.y, pulse),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, 2.8, 0), 8, 3.5, 2.5, 3.5, 0.05);
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 3, 0), 6, 4, 3, 4, 0.04);
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
            }

            // Dissipate top-down
            if (tick >= 180 && tick < 200) {
                int layer = 4 - ((tick - 180) / 4);
                if (layer >= 0 && layer < rings.size() && (tick - 180) % 4 == 0) {
                    for (BlockDisplayHandle h : rings.get(layer)) rescale(h.entity(), 0f, 0f, 0f, 4);
                    if (layer < columns.get(0).size()) {
                        for (List<BlockDisplayHandle> col : columns) {
                            rescale(col.get(layer).entity(), 0f, 0f, 0f, 4);
                        }
                    }
                }
                if (tick == 180) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.8f);
                    for (BlockDisplayHandle h : caps) rescale(h.entity(), 0f, 0f, 0f, 20);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HexagonalLatticePrism(plugin); }
    }

    // ================================================================
    // #33 — SNOWFLAKE FRACTAL MANDALA
    // 67 blocks: 1 DIAMOND hub + 6×4 BLUE_ICE primary arms (24) + 6×3
    // TINTED_GLASS secondary (18) + 6×3 PACKED_ICE tertiary (18). 6-fold
    // radial symmetry, Koch-style branching. Breathing scale + Y rotation.
    // Constant 10.0r, 130 dmg/12t (30t delay).
    // ================================================================
    public static class SnowflakeFractalMandala extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final List<BlockDisplayHandle> primary = new ArrayList<>();
        private final List<BlockDisplayHandle> secondary = new ArrayList<>();
        private final List<BlockDisplayHandle> tertiary = new ArrayList<>();
        private float rotY = 0f;

        public SnowflakeFractalMandala(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snowflake_fractal_mandala", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(130.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(220);
            config.setCooldownTicks(130);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 3.0;

            // Central hub
            BlockDisplayHandle hubH = displayBuilder.spawnBlock(
                    center.clone().add(0, cy, 0), Material.DIAMOND_BLOCK);
            hubH.scale(0f, 0f, 0f).glow(220, 255, 255).interpolation(10, 0);
            rescale(hubH.entity(), 0.5f, 0.5f, 0.5f, 10);
            spawnedEntities.add(hubH.entity());
            hub.add(hubH);

            // 6 primary arms — each 4 BLUE_ICE blocks at radii 1.2/2.0/2.8/3.6
            for (int arm = 0; arm < 6; arm++) {
                double a = 2 * Math.PI * arm / 6.0;
                double dx = Math.cos(a);
                double dz = Math.sin(a);
                for (int s = 0; s < 4; s++) {
                    double r = 1.2 + s * 0.8;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(dx * r, cy, dz * r), Material.BLUE_ICE);
                    h.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(10, 4 + s * 2);
                    orientAlong(h.entity(), dx, 0, dz);
                    rescale(h.entity(), 0.3f, 0.3f, 0.8f, 10);
                    spawnedEntities.add(h.entity());
                    primary.add(h);
                }
            }

            // 6 × 3 secondary — branch off each primary arm at radius 2.4 perpendicular,
            // following the classic Koch snowflake side-branch geometry (±60° off arm).
            for (int arm = 0; arm < 6; arm++) {
                double a = 2 * Math.PI * arm / 6.0;
                double rx = Math.cos(a);
                double rz = Math.sin(a);
                // Perpendicular in XZ plane
                double px = -rz;
                double pz = rx;
                double rootR = 2.4;
                for (int b = 0; b < 3; b++) {
                    double side = (b == 0) ? -1.0 : (b == 1 ? 1.0 : 0.0);
                    double extend = (b == 2) ? 0.9 : 0.7;
                    double bx = rx * rootR + px * side * 0.7 + rx * extend;
                    double bz = rz * rootR + pz * side * 0.7 + rz * extend;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(bx, cy + (b == 2 ? 0.2 : 0), bz), Material.TINTED_GLASS);
                    h.scale(0f, 0f, 0f).glow(60, 40, 80).interpolation(10, 12);
                    orientAlong(h.entity(), px * side + rx * 0.3, 0, pz * side + rz * 0.3);
                    rescale(h.entity(), 0.2f, 0.2f, 0.6f, 10);
                    spawnedEntities.add(h.entity());
                    secondary.add(h);
                }
            }

            // 6 × 3 tertiary — at each arm tip + sub-branches
            for (int arm = 0; arm < 6; arm++) {
                double a = 2 * Math.PI * arm / 6.0;
                double rx = Math.cos(a);
                double rz = Math.sin(a);
                double tipR = 4.2;
                for (int t = 0; t < 3; t++) {
                    double off = (t - 1) * 0.45;
                    double tx = rx * tipR + (-rz) * off;
                    double tz = rz * tipR + (rx) * off;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(tx, cy, tz), Material.PACKED_ICE);
                    h.scale(0f, 0f, 0f).glow(150, 210, 240).interpolation(10, 20);
                    rescale(h.entity(), 0.15f, 0.15f, 0.15f, 10);
                    spawnedEntities.add(h.entity());
                    tertiary.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.2f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, cy, 0), 60, 4, 1, 4, 0.05);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, cy, 0), 30, 0.5, 0.5, 0.5, 0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotY += (float) Math.toRadians(8.0);

            // Whole structure rotates around Y; breathing scale 1.0..1.2 over 30t
            float breathe = 1.0f + 0.1f * (float) Math.sin(tick * 0.21);

            if (tick % 2 == 0) {
                AxisAngle4f rot = new AxisAngle4f(rotY, 0f, 1f, 0f);
                applyShared(hub, rot, 0.5f * breathe);
                applyShared(primary, rot, breathe);
                applyShared(secondary, rot, breathe);
                applyShared(tertiary, rot, 0.15f * breathe);
            }

            // Hub glow burst
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW,
                        c.clone().add(0, 3, 0), 8, 0.4, 0.4, 0.4, 0.0);
            }
            // Branch tips spark
            if (tick % 4 == 0) {
                for (int i = 0; i < tertiary.size(); i += 3) {
                    BlockDisplay e = tertiary.get(i).entity();
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            e.getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // Radial snowflakes
            if (tick % 3 == 0) {
                double a = tick * 0.31;
                double r = 4.5;
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(Math.cos(a) * r, 3, Math.sin(a) * r), 3, 0.1, 0.1, 0.1, 0.02);
            }
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.3f);
            }

            // Dissipate: tertiary → secondary → primary → hub last
            if (tick == 195) for (BlockDisplayHandle h : tertiary) rescale(h.entity(), 0f, 0f, 0f, 8);
            if (tick == 203) for (BlockDisplayHandle h : secondary) rescale(h.entity(), 0f, 0f, 0f, 8);
            if (tick == 211) for (BlockDisplayHandle h : primary) rescale(h.entity(), 0f, 0f, 0f, 8);
            if (tick == 218) {
                for (BlockDisplayHandle h : hub) rescale(h.entity(), 0f, 0f, 0f, 2);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.8f);
            }
        }

        private void applyShared(List<BlockDisplayHandle> list, AxisAngle4f rot, float scaleMul) {
            for (BlockDisplayHandle h : list) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                Vector3f s = t.getScale();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(), rot,
                        new Vector3f(s.x, s.y, s.z),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new SnowflakeFractalMandala(plugin); }
    }

    // ================================================================
    // #34 — ICE SPIRAL GALAXY
    // 46 blocks: 4 DIAMOND core + 18 BLUE_ICE arm-A + 18 PACKED_ICE arm-B
    // (logarithmic spirals r = a·e^(bθ)) + 6 TINTED_GLASS halo dots. Core
    // pulses, arms trail. Constant 12.0r (large), 95 dmg/12t (30t delay).
    // ================================================================
    public static class IceSpiralGalaxy extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private final List<BlockDisplayHandle> armA = new ArrayList<>();
        private final List<BlockDisplayHandle> armB = new ArrayList<>();
        private final List<BlockDisplayHandle> halo = new ArrayList<>();
        private float rotY = 0f;

        public IceSpiralGalaxy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_spiral_galaxy", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(95.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 3.0;

            // 4-block DIAMOND core cluster
            double[][] coreOffsets = {{-0.3, 0, -0.3}, {0.3, 0, -0.3}, {0, 0, 0.3}, {0, 0.4, 0}};
            for (double[] off : coreOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], cy + off[1], off[2]), Material.DIAMOND_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 255, 255).interpolation(10, 0);
                rescale(h.entity(), 0.6f, 0.6f, 0.6f, 10);
                spawnedEntities.add(h.entity());
                core.add(h);
            }

            // Logarithmic spiral arms — r = a · e^(b·θ); θ from 0 to 4π
            double a = 0.35;
            double b = 0.18;
            for (int i = 0; i < 18; i++) {
                double theta = (i / 17.0) * 4 * Math.PI;
                double r = a * Math.exp(b * theta);
                // Arm A
                double xA = Math.cos(theta) * r;
                double zA = Math.sin(theta) * r;
                BlockDisplayHandle hA = displayBuilder.spawnBlock(
                        center.clone().add(xA, cy, zA), Material.BLUE_ICE);
                float sA = 0.5f - (i / 17.0f) * 0.35f;
                hA.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(15, i);
                rescale(hA.entity(), sA, sA, sA, 15);
                spawnedEntities.add(hA.entity());
                armA.add(hA);
                // Arm B mirrored
                double xB = Math.cos(theta + Math.PI) * r;
                double zB = Math.sin(theta + Math.PI) * r;
                BlockDisplayHandle hB = displayBuilder.spawnBlock(
                        center.clone().add(xB, cy, zB), Material.PACKED_ICE);
                hB.scale(0f, 0f, 0f).glow(150, 210, 240).interpolation(15, i);
                rescale(hB.entity(), sA, sA, sA, 15);
                spawnedEntities.add(hB.entity());
                armB.add(hB);
            }

            // 6 halo dots at outer radius
            double haloR = a * Math.exp(b * 4 * Math.PI) + 1.5;
            for (int i = 0; i < 6; i++) {
                double t = 2 * Math.PI * i / 6.0;
                double x = Math.cos(t) * haloR;
                double z = Math.sin(t) * haloR;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, cy, z), Material.TINTED_GLASS);
                h.scale(0f, 0f, 0f).glow(160, 120, 220).interpolation(15, 25);
                rescale(h.entity(), 0.25f, 0.25f, 0.25f, 15);
                spawnedEntities.add(h.entity());
                halo.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.1f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, cy, 0), 50, 0.5, 0.5, 0.5, 0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotY += (float) Math.toRadians(3.0);

            // Core pulse (scale ±10%)
            if (tick % 3 == 0) {
                float pulse = 0.6f + 0.06f * (float) Math.sin(tick * 0.2);
                for (BlockDisplayHandle h : core) rescale(h.entity(), pulse, pulse, pulse, 3);
            }

            // Rotate arms + halo
            if (tick % 2 == 0) {
                AxisAngle4f rot = new AxisAngle4f(rotY, 0f, 1f, 0f);
                for (BlockDisplayHandle h : armA) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(t.getTranslation(), rot, t.getScale(), new AxisAngle4f(0, 0, 1, 0)));
                }
                for (BlockDisplayHandle h : armB) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(t.getTranslation(), rot, t.getScale(), new AxisAngle4f(0, 0, 1, 0)));
                }
                for (BlockDisplayHandle h : halo) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(t.getTranslation(), rot, t.getScale(), new AxisAngle4f(0, 0, 1, 0)));
                }
            }

            // Particles
            if (tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 3, 0), 6, 0.7, 0.5, 0.7, 0);
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, 3, 0), 12, 6.0, 0.5, 6.0, 0.05);
                c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        c.clone().add(0, 3, 0), 4, 7.0, 0.3, 7.0, 0.0);
            }
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.0f);
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.7f, 0.6f);

            // Dissipate — arms fly outward
            if (tick >= 215 && tick < 240) {
                if (tick == 215) {
                    for (int i = 0; i < armA.size(); i++) {
                        BlockDisplay e = armA.get(i).entity();
                        Vector3f tr = e.getTransformation().getTranslation();
                        Vector3f tgt = new Vector3f(tr.x * 1.8f, tr.y, tr.z * 1.8f);
                        e.setInterpolationDuration(25);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(tgt,
                                new AxisAngle4f().set(e.getTransformation().getLeftRotation()),
                                new Vector3f(0, 0, 0),
                                new AxisAngle4f(0, 0, 1, 0)));
                    }
                    for (int i = 0; i < armB.size(); i++) {
                        BlockDisplay e = armB.get(i).entity();
                        Vector3f tr = e.getTransformation().getTranslation();
                        Vector3f tgt = new Vector3f(tr.x * 1.8f, tr.y, tr.z * 1.8f);
                        e.setInterpolationDuration(25);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(tgt,
                                new AxisAngle4f().set(e.getTransformation().getLeftRotation()),
                                new Vector3f(0, 0, 0),
                                new AxisAngle4f(0, 0, 1, 0)));
                    }
                    for (BlockDisplayHandle h : halo) rescale(h.entity(), 0f, 0f, 0f, 20);
                    for (BlockDisplayHandle h : core) rescale(h.entity(), 0f, 0f, 0f, 20);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceSpiralGalaxy(plugin); }
    }

    // ================================================================
    // #35 — PRISMATIC CUBE
    // 32 blocks: 6 TINTED_GLASS faces + 12 BLUE_ICE edges + 8 DIAMOND
    // corner-prisms + 6 BLUE_STAINED_GLASS inner mini-cubes orbiting inside.
    // 3-axis rotation, periodic face-open damage pulse.
    // Constant 7.0r, 110 dmg/10t (30t delay); on open: +10b radius pulse.
    // ================================================================
    public static class PrismaticCube extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> faces = new ArrayList<>();
        private final List<BlockDisplayHandle> edges = new ArrayList<>();
        private final List<BlockDisplayHandle> corners = new ArrayList<>();
        private final List<BlockDisplayHandle> innerCubes = new ArrayList<>();
        private float rotX = 0f, rotY = 0f, rotZ = 0f;

        public PrismaticCube(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prismatic_cube_kaleidoscope", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(200);
            config.setCooldownTicks(120);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 3.0;
            double size = 2.0;

            // 6 face panels — centered on ±X, ±Y, ±Z faces, oriented as 2×2 thin slabs
            double[][] faceData = {
                    { size, 0, 0, 1, 0, 0}, {-size, 0, 0, -1, 0, 0},
                    {0,  size, 0, 0, 1, 0}, {0, -size, 0, 0, -1, 0},
                    {0, 0,  size, 0, 0, 1}, {0, 0, -size, 0, 0, -1}
            };
            for (double[] f : faceData) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(f[0], cy + f[1], f[2]), Material.TINTED_GLASS);
                h.scale(0f, 0f, 0f).glow(80, 140, 220).interpolation(25, 0);
                orientAlong(h.entity(), f[3], f[4], f[5]);
                rescale(h.entity(), 2.0f, 2.0f, 0.1f, 25);
                spawnedEntities.add(h.entity());
                faces.add(h);
            }

            // 12 edges — connect cube corners
            double s = size;
            double[][] corners8 = {
                    { s,  s,  s}, { s,  s, -s}, { s, -s,  s}, { s, -s, -s},
                    {-s,  s,  s}, {-s,  s, -s}, {-s, -s,  s}, {-s, -s, -s}
            };
            int[][] edgePairs = {
                    {0,1},{2,3},{4,5},{6,7}, // along Z axis
                    {0,2},{1,3},{4,6},{5,7}, // along Y axis
                    {0,4},{1,5},{2,6},{3,7}  // along X axis
            };
            for (int[] pair : edgePairs) {
                double[] v1 = corners8[pair[0]];
                double[] v2 = corners8[pair[1]];
                double mx = (v1[0] + v2[0]) / 2.0;
                double my = (v1[1] + v2[1]) / 2.0;
                double mz = (v1[2] + v2[2]) / 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(mx, cy + my, mz), Material.BLUE_ICE);
                h.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(25, 2);
                orientAlong(h.entity(), v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2]);
                rescale(h.entity(), 0.2f, 0.2f, (float) (size * 2), 25);
                spawnedEntities.add(h.entity());
                edges.add(h);
            }

            // 8 corner-prism DIAMOND blocks
            for (double[] v : corners8) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(v[0], cy + v[1], v[2]), Material.DIAMOND_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 255, 255).interpolation(25, 5);
                rescale(h.entity(), 0.4f, 0.4f, 0.4f, 25);
                spawnedEntities.add(h.entity());
                corners.add(h);
            }

            // 6 inner mini-cubes orbiting on a smaller hexagonal ring inside
            for (int i = 0; i < 6; i++) {
                double a = 2 * Math.PI * i / 6.0;
                double r = 1.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(a) * r, cy, Math.sin(a) * r),
                        Material.BLUE_STAINED_GLASS);
                h.scale(0f, 0f, 0f).glow(80, 140, 220).interpolation(25, 10);
                rescale(h.entity(), 0.5f, 0.5f, 0.5f, 25);
                spawnedEntities.add(h.entity());
                innerCubes.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.4f, 1.0f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, cy, 0), 40, 1.5, 1.5, 1.5, 0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotY += (float) Math.toRadians(2.4);
            rotX += (float) Math.toRadians(1.7);
            rotZ += (float) Math.toRadians(1.1);

            // 3-axis rotation of the cube shell
            if (tick % 2 == 0) {
                AxisAngle4f rot = new AxisAngle4f(rotY, 0.4f, 1f, 0.6f);
                for (BlockDisplayHandle h : faces) applyOuterRot(h, rot);
                for (BlockDisplayHandle h : edges) applyOuterRot(h, rot);
                for (BlockDisplayHandle h : corners) applyOuterRot(h, rot);
            }

            // Inner mini-cubes orbit independently around Y at different rate
            if (tick % 2 == 0) {
                float inner = rotZ * 2.5f;
                for (int i = 0; i < innerCubes.size(); i++) {
                    double a = 2 * Math.PI * i / 6.0 + inner;
                    double r = 1.0;
                    BlockDisplay e = innerCubes.get(i).entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f((float) (Math.cos(a) * r) - 0.5f,
                                    -0.5f + 0.3f * (float) Math.sin(tick * 0.1 + i),
                                    (float) (Math.sin(a) * r) - 0.5f),
                            new AxisAngle4f(inner, 1f, 0.5f, 0.3f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Periodic face-open pulse — one face folds out at tick 40/100/160, +10r damage radius
            if (tick == 40 || tick == 100 || tick == 160) {
                BlockDisplayHandle face = faces.get(tick / 40 % faces.size());
                BlockDisplay e = face.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(10);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(t.getTranslation().x * 2f, t.getTranslation().y, t.getTranslation().z * 2f),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(2.5f, 2.5f, 0.1f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.9f);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 3, 0), 60, 7, 3, 7, 0.3);
                // One-shot radial damage burst — schedule a triggerImpactDamage at +10b radius
                Location burst = c.clone().add(0, 3, 0);
                double prevRadius = config.getImpactRadius();
                double prevDmg = config.getImpactDamage();
                config.setImpactRadius(prevRadius + 10.0);
                config.setImpactDamage(100.0);
                triggerImpactDamage(burst);
                config.setImpactRadius(prevRadius);
                config.setImpactDamage(prevDmg);
            }

            if (tick % 4 == 0) {
                for (int i = 0; i < corners.size(); i++) {
                    BlockDisplay e = corners.get(i).entity();
                    c.getWorld().spawnParticle(Particle.GLOW, e.getLocation(), 1, 0.1, 0.1, 0.1, 0);
                }
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 5, 1.5, 1.5, 1.5, 0.03);
            }

            if (tick % 30 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.5f, 1.5f);

            // Dissipate
            if (tick == 185) {
                for (BlockDisplayHandle h : faces) {
                    BlockDisplay e = h.entity();
                    Vector3f tr = e.getTransformation().getTranslation();
                    e.setInterpolationDuration(15);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(tr.x * 2.5f, tr.y, tr.z * 2.5f),
                            new AxisAngle4f().set(e.getTransformation().getLeftRotation()),
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0)));
                }
                for (BlockDisplayHandle h : corners) rescale(h.entity(), 0f, 0f, 0f, 15);
                for (BlockDisplayHandle h : edges) rescale(h.entity(), 0f, 0f, 0f, 15);
                for (BlockDisplayHandle h : innerCubes) rescale(h.entity(), 0f, 0f, 0f, 15);
            }
        }

        private void applyOuterRot(BlockDisplayHandle h, AxisAngle4f rot) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(2);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(), rot, t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PrismaticCube(plugin); }
    }

    // ================================================================
    // #36 — MOBIUS ICE STRIP
    // 36 blocks: 32 BLUE_ICE on parametric Möbius surface + 4 DIAMOND highlights.
    // x(t,v) = (R + v·cos(t/2))·cos(t)
    // y(t,v) = v·sin(t/2)
    // z(t,v) = (R + v·cos(t/2))·sin(t)  for t∈[0,2π), v=0 (centerline)
    // Each block rotates an additional t/2 along its own forward axis (half-twist).
    // Constant 8.0r, 100 dmg/12t (30t delay).
    // ================================================================
    public static class MobiusIceStrip extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> strip = new ArrayList<>();
        private final List<BlockDisplayHandle> highlights = new ArrayList<>();
        private float rotY = 0f;

        public MobiusIceStrip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mobius_ice_strip", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(100.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(120);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 3.0;
            double R = 3.2;
            int N = 32;
            for (int i = 0; i < N; i++) {
                double t = 2 * Math.PI * i / N;
                double v = 0; // centerline
                double x = (R + v * Math.cos(t / 2.0)) * Math.cos(t);
                double y = v * Math.sin(t / 2.0);
                double z = (R + v * Math.cos(t / 2.0)) * Math.sin(t);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, cy + y, z), Material.BLUE_ICE);
                h.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(30, i);
                // Tangent direction = derivative w.r.t. t
                double dx = -(R + v * Math.cos(t / 2)) * Math.sin(t) - 0.5 * v * Math.sin(t / 2) * Math.cos(t);
                double dz =  (R + v * Math.cos(t / 2)) * Math.cos(t) - 0.5 * v * Math.sin(t / 2) * Math.sin(t);
                orientAlong(h.entity(), dx, 0, dz);
                // Add twist around tangent axis = t/2 (the Möbius half-twist)
                BlockDisplay e = h.entity();
                Transformation tr = e.getTransformation();
                double yaw = Math.atan2(dz, dx);
                e.setTransformation(new Transformation(
                        tr.getTranslation(),
                        new AxisAngle4f((float) yaw, 0f, 1f, 0f),
                        tr.getScale(),
                        new AxisAngle4f((float) (t / 2.0), (float) Math.cos(yaw), 0f, (float) Math.sin(yaw))
                ));
                rescale(e, 0.4f, 0.15f, 0.8f, 30);
                spawnedEntities.add(e);
                strip.add(h);
            }

            // 4 DIAMOND highlights evenly spaced at t = 0, π/2, π, 3π/2
            for (int i = 0; i < 4; i++) {
                double t = (Math.PI / 2.0) * i;
                double x = R * Math.cos(t);
                double z = R * Math.sin(t);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, cy, z), Material.DIAMOND_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 255, 255).interpolation(30, 15);
                rescale(h.entity(), 0.25f, 0.25f, 0.25f, 30);
                spawnedEntities.add(h.entity());
                highlights.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, cy, 0), 50, 3, 1, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotY += (float) Math.toRadians(1.0);

            // Whole strip rotates around its central Y axis
            if (tick % 3 == 0) {
                // Re-parametrize each block at offset phase
                double R = 3.2;
                int N = strip.size();
                double phaseOffset = (tick * 0.04);
                for (int i = 0; i < N; i++) {
                    double t = 2 * Math.PI * i / N + phaseOffset;
                    double x = R * Math.cos(t);
                    double z = R * Math.sin(t);
                    double dx = -R * Math.sin(t);
                    double dz =  R * Math.cos(t);
                    double yaw = Math.atan2(dz, dx);
                    // Apply outer Y rotation to position
                    double cs = Math.cos(rotY), sn = Math.sin(rotY);
                    double rx = x * cs + z * sn;
                    double rz = -x * sn + z * cs;
                    BlockDisplay e = strip.get(i).entity();
                    Transformation tr = e.getTransformation();
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f((float) rx - 0.5f, -0.5f, (float) rz - 0.5f),
                            new AxisAngle4f((float) (yaw + rotY), 0f, 1f, 0f),
                            tr.getScale(),
                            new AxisAngle4f((float) (t / 2.0), (float) Math.cos(yaw + rotY), 0f, (float) Math.sin(yaw + rotY))
                    ));
                }
                // Highlights rotate with strip
                for (int i = 0; i < highlights.size(); i++) {
                    double t = (Math.PI / 2.0) * i + phaseOffset;
                    double x = R * Math.cos(t);
                    double z = R * Math.sin(t);
                    double cs = Math.cos(rotY), sn = Math.sin(rotY);
                    double rx = x * cs + z * sn;
                    double rz = -x * sn + z * cs;
                    BlockDisplay e = highlights.get(i).entity();
                    Transformation tr = e.getTransformation();
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f((float) rx - 0.5f, -0.5f, (float) rz - 0.5f),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                }
            }

            if (tick % 3 == 0) {
                for (int i = 0; i < strip.size(); i += 4) {
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            strip.get(i).entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (tick % 4 == 0) {
                for (BlockDisplayHandle h : highlights) {
                    c.getWorld().spawnParticle(Particle.GLOW, h.entity().getLocation(), 2, 0.1, 0.1, 0.1, 0);
                }
            }
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 0.5f);

            if (tick >= 215 && tick < 240) {
                if ((tick - 215) % 2 == 0) {
                    int idx = (tick - 215) / 2;
                    if (idx < strip.size()) rescale(strip.get(idx).entity(), 0f, 0f, 0f, 4);
                }
                if (tick == 215) {
                    for (BlockDisplayHandle h : highlights) rescale(h.entity(), 0f, 0f, 0f, 22);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MobiusIceStrip(plugin); }
    }

    // ================================================================
    // #37 — TESSERACT CUBE HYPER
    // 48 blocks: outer cube (8 DIAMOND verts + 12 BLUE_ICE edges = 20) +
    // inner cube (8 AMETHYST verts + 12 BLUE_STAINED_GLASS edges = 20) +
    // 8 TINTED_GLASS connecting struts. Outer rotates Y, inner rotates X
    // independently; inner periodically "inverts" through outer.
    // Constant 8.0r, 120 dmg/12t (30t delay).
    // ================================================================
    public static class TesseractCubeHyper extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> outerVerts = new ArrayList<>();
        private final List<BlockDisplayHandle> outerEdges = new ArrayList<>();
        private final List<BlockDisplayHandle> innerVerts = new ArrayList<>();
        private final List<BlockDisplayHandle> innerEdges = new ArrayList<>();
        private final List<BlockDisplayHandle> struts = new ArrayList<>();
        private final List<double[]> innerOffsets = new ArrayList<>();
        private float rotOuter = 0f;
        private float rotInner = 0f;
        private float invertPhase = 0f;

        public TesseractCubeHyper(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tesseract_cube_hyper", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(130);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 3.0;
            double outerS = 2.5;
            double innerS = 1.25;

            // 8 outer vertices
            int[][] sign = {
                    {1,1,1},{1,1,-1},{1,-1,1},{1,-1,-1},
                    {-1,1,1},{-1,1,-1},{-1,-1,1},{-1,-1,-1}
            };
            for (int[] s : sign) {
                double x = s[0] * outerS, y = s[1] * outerS, z = s[2] * outerS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, cy + y, z), Material.DIAMOND_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 255, 255).interpolation(30, 0);
                rescale(h.entity(), 0.3f, 0.3f, 0.3f, 30);
                spawnedEntities.add(h.entity());
                outerVerts.add(h);
            }

            // 12 outer edges
            int[][] edgePairs = {
                    {0,1},{2,3},{4,5},{6,7},
                    {0,2},{1,3},{4,6},{5,7},
                    {0,4},{1,5},{2,6},{3,7}
            };
            for (int[] p : edgePairs) {
                int[] v1 = sign[p[0]];
                int[] v2 = sign[p[1]];
                double mx = (v1[0] + v2[0]) * 0.5 * outerS;
                double my = (v1[1] + v2[1]) * 0.5 * outerS;
                double mz = (v1[2] + v2[2]) * 0.5 * outerS;
                double dx = (v2[0] - v1[0]) * outerS;
                double dy = (v2[1] - v1[1]) * outerS;
                double dz = (v2[2] - v1[2]) * outerS;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(mx, cy + my, mz), Material.BLUE_ICE);
                h.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(30, 4);
                orientAlong(h.entity(), dx, dy, dz);
                rescale(h.entity(), 0.15f, 0.15f, (float) len, 30);
                spawnedEntities.add(h.entity());
                outerEdges.add(h);
            }

            // 8 inner vertices
            for (int[] s : sign) {
                double x = s[0] * innerS, y = s[1] * innerS, z = s[2] * innerS;
                innerOffsets.add(new double[]{x, y, z});
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, cy + y, z), Material.AMETHYST_BLOCK);
                h.scale(0f, 0f, 0f).glow(160, 120, 220).interpolation(30, 10);
                rescale(h.entity(), 0.2f, 0.2f, 0.2f, 30);
                spawnedEntities.add(h.entity());
                innerVerts.add(h);
            }

            // 12 inner edges
            for (int[] p : edgePairs) {
                int[] v1 = sign[p[0]];
                int[] v2 = sign[p[1]];
                double mx = (v1[0] + v2[0]) * 0.5 * innerS;
                double my = (v1[1] + v2[1]) * 0.5 * innerS;
                double mz = (v1[2] + v2[2]) * 0.5 * innerS;
                double dx = (v2[0] - v1[0]) * innerS;
                double dy = (v2[1] - v1[1]) * innerS;
                double dz = (v2[2] - v1[2]) * innerS;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(mx, cy + my, mz), Material.BLUE_STAINED_GLASS);
                h.scale(0f, 0f, 0f).glow(80, 140, 220).interpolation(30, 14);
                orientAlong(h.entity(), dx, dy, dz);
                rescale(h.entity(), 0.1f, 0.1f, (float) len, 30);
                spawnedEntities.add(h.entity());
                innerEdges.add(h);
            }

            // 8 connecting struts — outer vertex to corresponding inner vertex
            for (int i = 0; i < sign.length; i++) {
                int[] s = sign[i];
                double ox = s[0] * outerS, oy = s[1] * outerS, oz = s[2] * outerS;
                double ix = s[0] * innerS, iy = s[1] * innerS, iz = s[2] * innerS;
                double mx = (ox + ix) * 0.5;
                double my = (oy + iy) * 0.5;
                double mz = (oz + iz) * 0.5;
                double dx = ox - ix, dy = oy - iy, dz = oz - iz;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(mx, cy + my, mz), Material.TINTED_GLASS);
                h.scale(0f, 0f, 0f).glow(60, 40, 80).interpolation(30, 22);
                orientAlong(h.entity(), dx, dy, dz);
                rescale(h.entity(), 0.1f, 0.1f, (float) len, 30);
                spawnedEntities.add(h.entity());
                struts.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, cy, 0), 50, 2, 2, 2, 0);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, cy, 0), 25, 1.5, 1.5, 1.5, 0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotOuter += (float) Math.toRadians(2.0);
            rotInner += (float) Math.toRadians(3.0);
            invertPhase += 0.05f;

            // Outer rotates Y
            if (tick % 2 == 0) {
                AxisAngle4f rot = new AxisAngle4f(rotOuter, 0f, 1f, 0f);
                for (BlockDisplayHandle h : outerVerts) applyRot(h, rot);
                for (BlockDisplayHandle h : outerEdges) applyRot(h, rot);

                // Inner rotates X with periodic inversion (scale wobble along axis)
                AxisAngle4f rotI = new AxisAngle4f(rotInner, 1f, 0f, 0f);
                float invertScale = 1.0f + 0.6f * (float) Math.sin(invertPhase);
                for (int i = 0; i < innerVerts.size(); i++) {
                    double[] off = innerOffsets.get(i);
                    BlockDisplay e = innerVerts.get(i).entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f((float) (off[0] * invertScale) - 0.5f,
                                    (float) (off[1] * invertScale) - 0.5f,
                                    (float) (off[2] * invertScale) - 0.5f),
                            rotI, t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                for (BlockDisplayHandle h : innerEdges) applyRot(h, rotI);
                for (BlockDisplayHandle h : struts) applyRot(h, rot);
            }

            // Inner "inversion" peak — burst at every full sine cycle
            if (tick % 60 == 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.5f);
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 3, 0), 30, 1.5, 1.5, 1.5, 0.0);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 3, 0), 40, 2, 2, 2, 0.2);
            }

            if (tick % 3 == 0) {
                for (int i = 0; i < outerVerts.size(); i += 2) {
                    c.getWorld().spawnParticle(Particle.GLOW,
                            outerVerts.get(i).entity().getLocation(), 1, 0.1, 0.1, 0.1, 0);
                }
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, 3, 0), 8, 2.5, 2, 2.5, 0.1);
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.7f, 0.4f);

            if (tick == 215) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.7f);
                // Inner explodes outward
                for (int i = 0; i < innerVerts.size(); i++) {
                    double[] off = innerOffsets.get(i);
                    BlockDisplay e = innerVerts.get(i).entity();
                    e.setInterpolationDuration(25);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f((float) (off[0] * 4) - 0.5f, (float) (off[1] * 4) - 0.5f, (float) (off[2] * 4) - 0.5f),
                            new AxisAngle4f().set(e.getTransformation().getLeftRotation()),
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0)));
                }
                for (BlockDisplayHandle h : innerEdges) rescale(h.entity(), 0f, 0f, 0f, 22);
                for (BlockDisplayHandle h : struts) rescale(h.entity(), 0f, 0f, 0f, 22);
                for (BlockDisplayHandle h : outerVerts) rescale(h.entity(), 0f, 0f, 0f, 25);
                for (BlockDisplayHandle h : outerEdges) rescale(h.entity(), 0f, 0f, 0f, 25);
            }
        }

        private void applyRot(BlockDisplayHandle h, AxisAngle4f rot) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(2);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(), rot, t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TesseractCubeHyper(plugin); }
    }

    // ================================================================
    // #38 — KLEIN ICE BOTTLE
    // 38 blocks: 30 BLUE_ICE along a parameterized Klein-bottle figure-8 curve
    // + 4 DIAMOND landmark points + 4 TINTED_GLASS marking self-intersection.
    // Klein bottle "figure-8 immersion":
    //   x = (R + cos(u/2)·sin(v) − sin(u/2)·sin(2v)) · cos(u)
    //   y = sin(u/2)·sin(v) + cos(u/2)·sin(2v)
    //   z = (R + cos(u/2)·sin(v) − sin(u/2)·sin(2v)) · sin(u)
    // We sample the centerline v=0 so it's a clean self-crossing loop.
    // Constant 7.5r, 105 dmg/12t (30t delay).
    // ================================================================
    public static class KleinIceBottle extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> curve = new ArrayList<>();
        private final List<BlockDisplayHandle> landmarks = new ArrayList<>();
        private final List<BlockDisplayHandle> intersections = new ArrayList<>();
        private final List<double[]> curveCoords = new ArrayList<>();
        private float flowPhase = 0f;
        private float rotY = 0f;

        public KleinIceBottle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("klein_ice_bottle", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(105.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(125);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 3.0;
            double R = 2.8;
            int N = 30;
            for (int i = 0; i < N; i++) {
                double u = 2 * Math.PI * i / N;
                // Figure-8 immersion centerline (v=0.6 to give it a clear shape)
                double v = 0.6;
                double x = (R + Math.cos(u / 2) * Math.sin(v) - Math.sin(u / 2) * Math.sin(2 * v)) * Math.cos(u);
                double y = Math.sin(u / 2) * Math.sin(v) + Math.cos(u / 2) * Math.sin(2 * v);
                double z = (R + Math.cos(u / 2) * Math.sin(v) - Math.sin(u / 2) * Math.sin(2 * v)) * Math.sin(u);
                curveCoords.add(new double[]{x, y, z, u});
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, cy + y, z), Material.BLUE_ICE);
                h.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(30, i);
                // Tangent direction (approx)
                double dx = -Math.sin(u) * R;
                double dz =  Math.cos(u) * R;
                orientAlong(h.entity(), dx, 0, dz);
                rescale(h.entity(), 0.3f, 0.3f, 0.5f, 30);
                spawnedEntities.add(h.entity());
                curve.add(h);
            }

            // 4 DIAMOND landmarks at u = 0, π/2, π, 3π/2
            for (int i = 0; i < 4; i++) {
                int idx = i * (N / 4);
                double[] cc = curveCoords.get(idx);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(cc[0], cy + cc[1] + 0.4, cc[2]), Material.DIAMOND_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 255, 255).interpolation(30, 15);
                rescale(h.entity(), 0.25f, 0.25f, 0.25f, 30);
                spawnedEntities.add(h.entity());
                landmarks.add(h);
            }

            // 4 TINTED_GLASS at the self-intersection cluster (small offsets around center)
            double[][] iso = {{0, 0, 0.4}, {0.4, 0, 0}, {0, 0, -0.4}, {-0.4, 0, 0}};
            for (double[] o : iso) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(o[0], cy, o[2]), Material.TINTED_GLASS);
                h.scale(0f, 0f, 0f).glow(60, 40, 80).interpolation(30, 20);
                rescale(h.entity(), 0.25f, 0.25f, 0.25f, 30);
                spawnedEntities.add(h.entity());
                intersections.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, cy, 0), 30, 1.5, 1, 1.5, 0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            flowPhase += 0.15f;
            rotY += (float) Math.toRadians(2.0);

            // Flow blocks along the curve by shifting their phase
            if (tick % 3 == 0) {
                int N = curve.size();
                double R = 2.8;
                double v = 0.6;
                for (int i = 0; i < N; i++) {
                    double u = 2 * Math.PI * i / N + flowPhase;
                    double x = (R + Math.cos(u / 2) * Math.sin(v) - Math.sin(u / 2) * Math.sin(2 * v)) * Math.cos(u);
                    double y = Math.sin(u / 2) * Math.sin(v) + Math.cos(u / 2) * Math.sin(2 * v);
                    double z = (R + Math.cos(u / 2) * Math.sin(v) - Math.sin(u / 2) * Math.sin(2 * v)) * Math.sin(u);
                    // Apply outer Y rotation
                    double cs = Math.cos(rotY), sn = Math.sin(rotY);
                    double rx = x * cs + z * sn;
                    double rz = -x * sn + z * cs;
                    double dx = -Math.sin(u) * R;
                    double dz =  Math.cos(u) * R;
                    double drx = dx * cs + dz * sn;
                    double drz = -dx * sn + dz * cs;
                    BlockDisplay e = curve.get(i).entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    float yaw = (float) Math.atan2(drz, drx);
                    e.setTransformation(new Transformation(
                            new Vector3f((float) rx - 0.5f, (float) y - 0.5f, (float) rz - 0.5f),
                            new AxisAngle4f(yaw, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            if (tick % 3 == 0) {
                for (int i = 0; i < curve.size(); i += 4) {
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            curve.get(i).entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        c.clone().add(0, 3, 0), 4, 0.5, 0.3, 0.5, 0.0);
            }
            if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.9f);
            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 0.5f);

            if (tick >= 215 && tick < 240) {
                if ((tick - 215) % 2 == 0) {
                    int idx = curve.size() / 2 - (tick - 215) / 2;
                    int idx2 = curve.size() / 2 + (tick - 215) / 2;
                    if (idx >= 0 && idx < curve.size()) rescale(curve.get(idx).entity(), 0f, 0f, 0f, 3);
                    if (idx2 >= 0 && idx2 < curve.size()) rescale(curve.get(idx2).entity(), 0f, 0f, 0f, 3);
                }
                if (tick == 215) {
                    for (BlockDisplayHandle h : landmarks) rescale(h.entity(), 0f, 0f, 0f, 22);
                    for (BlockDisplayHandle h : intersections) rescale(h.entity(), 0f, 0f, 0f, 22);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new KleinIceBottle(plugin); }
    }

    // ================================================================
    // #39 — SIERPINSKI TETRAHEDRON
    // 36 blocks: Level 0 = 4 BLUE_ICE big-tetra verts (1.5 scale).
    // Level 1 = 4 sub-tetras × 4 verts PACKED_ICE (16, 0.75 scale).
    // Level 2 = 4 sub-sub-tetras × 4 verts TINTED_GLASS (16, 0.375 scale).
    // Regular tetrahedron vertices: (1,1,1),(1,-1,-1),(-1,1,-1),(-1,-1,1).
    // Constant 9.0r, 110 dmg/12t (30t delay).
    // ================================================================
    public static class SierpinskiTetrahedron extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> level0 = new ArrayList<>();
        private final List<BlockDisplayHandle> level1 = new ArrayList<>();
        private final List<BlockDisplayHandle> level2 = new ArrayList<>();
        private float rotY = 0f;

        public SierpinskiTetrahedron(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sierpinski_tetrahedron", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(220);
            config.setCooldownTicks(125);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 3.2;
            double S = 3.0;
            // Regular tetrahedron: 4 vertices
            double[][] tet = {
                    {1,1,1}, {1,-1,-1}, {-1,1,-1}, {-1,-1,1}
            };

            // Level 0: 4 BLUE_ICE big verts
            for (double[] v : tet) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(v[0] * S, cy + v[1] * S, v[2] * S), Material.BLUE_ICE);
                h.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(12, 0);
                rescale(h.entity(), 1.5f, 1.5f, 1.5f, 12);
                spawnedEntities.add(h.entity());
                level0.add(h);
            }

            // Level 1: 4 sub-tetras at midpoint of each original vertex toward centroid;
            // place sub-tetra at the original vertex itself, scaled to half size.
            double sub1 = S * 0.5;
            for (double[] v : tet) {
                // Sub-tetra is centered at v·S (the parent vertex), with half size
                double cx = v[0] * S;
                double cyy = v[1] * S;
                double cz = v[2] * S;
                for (double[] sv : tet) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(cx + sv[0] * sub1, cy + cyy + sv[1] * sub1, cz + sv[2] * sub1),
                            Material.PACKED_ICE);
                    h.scale(0f, 0f, 0f).glow(150, 210, 240).interpolation(12, 12);
                    rescale(h.entity(), 0.75f, 0.75f, 0.75f, 12);
                    spawnedEntities.add(h.entity());
                    level1.add(h);
                }
            }

            // Level 2: 4 sub-sub-tetras at one of the level-1 sub-tetras' verts (highest-Y vertex)
            // We'll add 4 at each of the level-1 nodes whose v[1]=1 in the parent tetrahedron's sub-config.
            double sub2 = sub1 * 0.5;
            // Pick the top-Y vertex of each level-1 sub-tetra as the level-2 anchor
            for (double[] v : tet) {
                double cx = v[0] * S + 1 * sub1; // sv = (1,1,1) sub-vertex
                double cyy = v[1] * S + 1 * sub1;
                double cz = v[2] * S + 1 * sub1;
                for (double[] sv : tet) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(cx + sv[0] * sub2, cy + cyy + sv[1] * sub2, cz + sv[2] * sub2),
                            Material.TINTED_GLASS);
                    h.scale(0f, 0f, 0f).glow(60, 40, 80).interpolation(12, 22);
                    rescale(h.entity(), 0.375f, 0.375f, 0.375f, 12);
                    spawnedEntities.add(h.entity());
                    level2.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f, 0.9f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, cy, 0), 50, 3, 3, 3, 0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotY += (float) Math.toRadians(4.0);

            // Whole rotates Y; levels pulse independently
            if (tick % 2 == 0) {
                AxisAngle4f rot = new AxisAngle4f(rotY, 0f, 1f, 0f);
                for (BlockDisplayHandle h : level0) applyRotKeep(h, rot, 1.5f + 0.1f * (float) Math.sin(tick * 0.15));
                for (BlockDisplayHandle h : level1) applyRotKeep(h, rot, 0.75f + 0.06f * (float) Math.sin(tick * 0.2));
                for (BlockDisplayHandle h : level2) applyRotKeep(h, rot, 0.375f + 0.05f * (float) Math.sin(tick * 0.25));
            }

            if (tick % 4 == 0) {
                for (int i = 0; i < level0.size(); i++) {
                    BlockDisplay e = level0.get(i).entity();
                    c.getWorld().spawnParticle(Particle.GLOW, e.getLocation(), 1, 0.2, 0.2, 0.2, 0);
                }
            }
            if (tick % 5 == 0) {
                for (int i = 0; i < level1.size(); i += 3) {
                    BlockDisplay e = level1.get(i).entity();
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, e.getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (tick == 30) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.1f);
            if (tick == 60) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.3f);
            if (tick == 90) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.5f);

            if (tick == 200) for (BlockDisplayHandle h : level2) rescale(h.entity(), 0f, 0f, 0f, 8);
            if (tick == 210) for (BlockDisplayHandle h : level1) rescale(h.entity(), 0f, 0f, 0f, 8);
            if (tick == 218) {
                for (BlockDisplayHandle h : level0) rescale(h.entity(), 0f, 0f, 0f, 2);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.7f);
            }
        }

        private void applyRotKeep(BlockDisplayHandle h, AxisAngle4f rot, float scale) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(2);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(), rot,
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new SierpinskiTetrahedron(plugin); }
    }

    // ================================================================
    // #40 — PENROSE TRIANGLE ILLUSION
    // 39 blocks: 3 beams × 10 BLUE_ICE segments (30) + 3 DIAMOND corner-junctions
    // + 6 CALCITE "perspective overlap" trick blocks. The 3 beams form an
    // equilateral triangle in plan view, but each beam is given a different Y
    // offset at its endpoints (creating the impossible-perspective illusion).
    // Constant 8.0r, 110 dmg/12t (30t delay).
    // ================================================================
    public static class PenroseTriangleIllusion extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> beams = new ArrayList<>();
        private final List<BlockDisplayHandle> junctions = new ArrayList<>();
        private final List<BlockDisplayHandle> overlaps = new ArrayList<>();
        private float rotY = 0f;

        public PenroseTriangleIllusion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("penrose_triangle_illusion", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(220);
            config.setCooldownTicks(125);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 3.5;
            double R = 4.0; // outer radius

            // Three corner positions of equilateral triangle (in XZ plane)
            double[][] corners = new double[3][3];
            for (int i = 0; i < 3; i++) {
                double a = (Math.PI / 2.0) + 2 * Math.PI * i / 3.0;
                corners[i] = new double[]{Math.cos(a) * R, 0, Math.sin(a) * R};
            }

            // 3 beams — each from corner[i] to corner[(i+1)%3], with a CHEATED Y
            // offset at one end so they don't truly connect, creating the illusion.
            // Beam endpoints get progressively offset Y so each beam appears to
            // pass IN FRONT of the next when viewed from the original angle.
            double[] yOffStart = {0,    0.8, 1.6};
            double[] yOffEnd   = {0.8,  1.6, 0.0}; // wraps around — but the gap is hidden by overlap blocks
            for (int b = 0; b < 3; b++) {
                double[] p1 = corners[b];
                double[] p2 = corners[(b + 1) % 3];
                double yStart = yOffStart[b];
                double yEnd = yOffEnd[b];
                // 10 segments along the beam
                for (int s = 0; s < 10; s++) {
                    double t = (s + 0.5) / 10.0;
                    double px = p1[0] + (p2[0] - p1[0]) * t;
                    double pz = p1[2] + (p2[2] - p1[2]) * t;
                    double py = yStart + (yEnd - yStart) * t;
                    double dx = (p2[0] - p1[0]);
                    double dy = (yEnd - yStart);
                    double dz = (p2[2] - p1[2]);
                    double segLen = Math.sqrt(dx*dx + dy*dy + dz*dz) / 10.0;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(px, cy + py, pz), Material.BLUE_ICE);
                    h.scale(0f, 0f, 0f).glow(110, 180, 230).interpolation(24, s * 2 + b * 4);
                    orientAlong(h.entity(), dx, dy, dz);
                    rescale(h.entity(), 0.4f, 0.4f, (float) segLen * 1.05f, 24);
                    spawnedEntities.add(h.entity());
                    beams.add(h);
                }
            }

            // 3 DIAMOND corner-junctions
            for (int i = 0; i < 3; i++) {
                double yOff = yOffStart[i]; // start of beam-i
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(corners[i][0], cy + yOff, corners[i][2]), Material.DIAMOND_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 255, 255).interpolation(24, 18);
                rescale(h.entity(), 0.6f, 0.6f, 0.6f, 24);
                spawnedEntities.add(h.entity());
                junctions.add(h);
            }

            // 6 CALCITE overlap-trick blocks — placed near each junction at slight
            // offsets where beams "pass through" each other, hiding the gap.
            double[][] overlapOff = {
                    {0.6, 0.0, 0.0}, {-0.6, 0.0, 0.0},
                    {0.0, 0.6, 0.6}, {0.0, -0.6, -0.6},
                    {0.0, 0.0, 0.6}, {0.0, 0.6, -0.6}
            };
            for (int i = 0; i < 6; i++) {
                int junction = i / 2;
                double[] c = corners[junction];
                double[] off = overlapOff[i];
                double yOff = yOffStart[junction];
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(c[0] + off[0], cy + yOff + off[1], c[2] + off[2]),
                        Material.CALCITE);
                h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(24, 22);
                rescale(h.entity(), 0.3f, 0.3f, 0.3f, 24);
                spawnedEntities.add(h.entity());
                overlaps.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 0.9f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, cy, 0), 30, 3, 1, 3, 0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            rotY += (float) Math.toRadians(2.0);

            // Rotate as rigid body
            if (tick % 2 == 0) {
                AxisAngle4f rot = new AxisAngle4f(rotY, 0f, 1f, 0f);
                for (BlockDisplayHandle h : beams) applyKeepScale(h, rot);
                for (BlockDisplayHandle h : junctions) applyKeepScale(h, rot);
                for (BlockDisplayHandle h : overlaps) applyKeepScale(h, rot);
            }

            // Junction pulse-glow
            if (tick % 6 == 0) {
                float pulse = 0.6f + 0.1f * (float) Math.sin(tick * 0.2);
                for (BlockDisplayHandle h : junctions) rescale(h.entity(), pulse, pulse, pulse, 6);
                for (BlockDisplayHandle h : junctions) {
                    c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                            h.entity().getLocation(), 3, 0.2, 0.2, 0.2, 0.0);
                }
            }

            if (tick % 3 == 0) {
                for (int i = 0; i < beams.size(); i += 4) {
                    BlockDisplay e = beams.get(i).entity();
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            e.getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.7f, 0.4f);
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.0f);

            // Dissipate: beams snap at junctions
            if (tick >= 198 && tick < 220) {
                if ((tick - 198) % 2 == 0) {
                    int idx = (tick - 198) / 2;
                    if (idx < beams.size()) rescale(beams.get(idx).entity(), 0f, 0f, 0f, 4);
                }
                if (tick == 198) {
                    for (BlockDisplayHandle h : junctions) rescale(h.entity(), 0f, 0f, 0f, 22);
                    for (BlockDisplayHandle h : overlaps) rescale(h.entity(), 0f, 0f, 0f, 22);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.7f);
                }
            }
        }

        private void applyKeepScale(BlockDisplayHandle h, AxisAngle4f rot) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(2);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(), rot, t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PenroseTriangleIllusion(plugin); }
    }
}
