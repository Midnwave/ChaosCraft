package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.boss;

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

/**
 * GROUP 3: PULL / GRAVITY
 * Drags players toward danger zones, edges, or into the void.
 * The Voidmaw treats gravity as a suggestion.
 * Attacks 21-30 from the Voidmaw boss design document.
 */
public final class PullGravity {

    private PullGravity() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new AbyssBeckoning(plugin));
        registry.register(new GravityInversion(plugin));
        registry.register(new UndertowCurrent(plugin));
        registry.register(new SingularityPoint(plugin));
        registry.register(new EdgeHunger(plugin));
        registry.register(new VoidLeash(plugin));
        registry.register(new AbyssalTide(plugin));
        registry.register(new HungerPull(plugin));
        registry.register(new SpiralDrain(plugin));
        registry.register(new TheMawOpens(plugin));
    }

    // -------------------------------------------------------------------------
    // 21. Abyss Beckoning
    // A gravitational pull toward the nearest island edge. Pull strength scales
    // with proximity to the edge.
    // -------------------------------------------------------------------------
    public static class AbyssBeckoning extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public AbyssBeckoning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyss_beckoning", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(0.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Target: north edge
            Location edgeLoc = center.clone().add(0, 0, -20);
            DisplayBuilder.playSound(edgeLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);

            // Swirling dark purple particles at the target edge
            for (int i = 0; i < 16; i++) {
                double angle = Math.toRadians(i * 22.5);
                Location spiralLoc = edgeLoc.clone().add(Math.cos(angle) * 3.0, 0.5, Math.sin(angle) * 3.0);
                BlockDisplayHandle node = displayBuilder.spawnBlock(spiralLoc, Material.PURPLE_STAINED_GLASS);
                node.scale(0.4f, 0.6f, 0.4f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(node);
                spawnedEntities.add(node.entity());
            }

            // Gravitational distortion lens — visual warping
            BlockDisplayHandle lens = displayBuilder.spawnBlock(edgeLoc.clone().add(0, 2, 0),
                    Material.PURPLE_STAINED_GLASS);
            lens.scale(4.0f, 3.0f, 0.1f).glow(128, 0, 255).interpolation(3, 0);
            handles.add(lens);
            spawnedEntities.add(lens.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location edgeLoc = center.clone().add(0, 0, -20);

            // Pull phase (0-60): spiral tightens and accelerates toward edge
            if (ticksAlive <= 60) {
                float t = ticksAlive / 60f;
                for (int i = 0; i < handles.size() - 1; i++) {
                    double angle = Math.toRadians(i * 22.5 - ticksAlive * 4.0);
                    double r = 3.0 * (1f - t * 0.4f);
                    Location spiralLoc = edgeLoc.clone().add(Math.cos(angle) * r, 0.5 + t * 1.5, Math.sin(angle) * r);
                    handles.get(i).entity().teleport(spiralLoc);

                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float scaleY = 0.6f + t * 1.5f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.2f, -scaleY * 0.5f, -0.2f),
                        new AxisAngle4f((float)Math.toRadians(-ticksAlive * 4.0 + i * 22.5), 0, 1, 0),
                        new Vector3f(0.4f, scaleY, 0.4f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(edgeLoc.clone().add(0, 1, 0), 15, 4.0);
                }
            }

            // Dissipate (60-100)
            if (ticksAlive > 60) {
                float decay = (ticksAlive - 60) / 40f;
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.purpleDust(edgeLoc.clone().add(0, 1 + decay * 2, 0), (int)(10 * (1f - decay)) + 1, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new AbyssBeckoning(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 22. Gravity Inversion
    // A localized inversion field — a shimmering black circle on the ground
    // that pulls players upward. Fall damage on deactivation.
    // -------------------------------------------------------------------------
    public static class GravityInversion extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean collapsed = false;

        public GravityInversion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_inversion", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(0.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location fieldCenter = center.clone().add(2, 0, -3);

            DisplayBuilder.playSound(fieldCenter, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);

            // Inversion field circle on ground — grows from a point
            for (int ring = 0; ring < 4; ring++) {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    double r = (ring + 1) * 1.0;
                    Location ringLoc = fieldCenter.clone().add(Math.cos(angle) * r, -0.05, Math.sin(angle) * r);
                    BlockDisplayHandle node = displayBuilder.spawnBlock(ringLoc, Material.BLACK_STAINED_GLASS);
                    node.scale(0.4f, 0.05f, 0.4f).glow(80, 0, 160).interpolation(3, 0);
                    handles.add(node);
                    spawnedEntities.add(node.entity());
                }
            }

            // Sky darkening column above
            BlockDisplayHandle darkCol = displayBuilder.spawnBlock(fieldCenter.clone().add(0, 5, 0),
                    Material.PURPLE_STAINED_GLASS);
            darkCol.scale(8f, 20f, 8f).glow(50, 0, 100).interpolation(4, 0);
            handles.add(darkCol);
            spawnedEntities.add(darkCol.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location fieldCenter = center.clone().add(2, 0, -3);

            // Phase 1 (0-40): field materializes — circles grow
            if (ticksAlive <= 40) {
                float t = ticksAlive / 40f;
                int circleCount = handles.size() - 1; // last is sky column

                for (int i = 0; i < circleCount; i++) {
                    int ring = i / 16;
                    int nodeIdx = i % 16;
                    // Expand ring radius
                    double angle = Math.toRadians(nodeIdx * 22.5);
                    double r = (ring + 1) * 1.0 * t * 2.0;
                    Location ringLoc = fieldCenter.clone().add(Math.cos(angle) * r, -0.05, Math.sin(angle) * r);
                    handles.get(i).entity().teleport(ringLoc);
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(fieldCenter.clone().add(0, 1, 0), 10, 3.0);
                }
            }

            // Phase 2 (40-120): field active — visual inversion effect
            if (ticksAlive > 40 && ticksAlive <= 120) {
                // Rings pulse and rotate
                float pulse = (float)Math.sin(ticksAlive * 0.2) * 0.2f;
                for (int i = 0; i < handles.size() - 1; i++) {
                    int ring = i / 16;
                    int nodeIdx = i % 16;
                    double angle = Math.toRadians(nodeIdx * 22.5 + ticksAlive * 2.0);
                    double r = (ring + 1) * 2.0 + pulse;
                    Location ringLoc = fieldCenter.clone().add(Math.cos(angle) * r, -0.05, Math.sin(angle) * r);
                    handles.get(i).entity().teleport(ringLoc);

                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float scale = 0.4f + pulse;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-scale * 0.5f, 0f, -scale * 0.5f),
                        new AxisAngle4f((float)Math.toRadians(ticksAlive * 2.0 + nodeIdx * 22.5), 0, 1, 0),
                        new Vector3f(scale, 0.05f, scale),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(fieldCenter.clone().add(0, 8, 0), 15, 5.0);
                }
            }

            // Phase 3 (120): field collapses — fall damage implied
            if (!collapsed && ticksAlive == 120) {
                collapsed = true;
                DisplayBuilder.purpleDust(fieldCenter.clone().add(0, 5, 0), 40, 6.0);
                DisplayBuilder.playSound(fieldCenter, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.2f);
                triggerImpactDamage(fieldCenter);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new GravityInversion(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 23. Undertow Current
    // Black fluid-like tendrils flow across the ground converging toward an
    // island edge — persistent current lasting 6 seconds.
    // -------------------------------------------------------------------------
    public static class UndertowCurrent extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public UndertowCurrent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("undertow_current", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(0.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(80);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location edgeLoc = center.clone().add(0, 0, -20);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.6f);
            DisplayBuilder.purpleDust(edgeLoc, 20, 4.0);

            // Crack indicators pointing toward the edge
            for (int i = 0; i < 8; i++) {
                double xOffset = (Math.random() - 0.5) * 20;
                Location crackLoc = center.clone().add(xOffset, -0.05, (i - 4) * 2.5);
                BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.OBSIDIAN);
                crack.scale(0.3f, 0.05f, 2.0f).glow(80, 0, 160).interpolation(3, 0);
                handles.add(crack);
                spawnedEntities.add(crack.entity());
            }

            // Tendril flow segments
            for (int row = 0; row < 5; row++) {
                for (int seg = 0; seg < 8; seg++) {
                    double x = (row - 2) * 4.0;
                    double z = center.getZ() + seg * 2.5 - 10;
                    Location tendrilLoc = new Location(center.getWorld(), center.getX() + x, center.getY() + 0.1, z);
                    BlockDisplayHandle tendril = displayBuilder.spawnBlock(tendrilLoc, Material.PURPLE_STAINED_GLASS);
                    tendril.scale(0.8f, 0.15f, 1.5f).glow(128, 0, 255).interpolation(2, 0);
                    handles.add(tendril);
                    spawnedEntities.add(tendril.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location edgeLoc = center.clone().add(0, 0, -20);

            // Phase 1 (0-30): cracks form
            if (ticksAlive < 30) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.3, 0), 10, 6.0);
                }
                return;
            }

            // Phase 2 (30-150): tendrils flow toward edge with accelerating speed
            if (ticksAlive >= 30 && ticksAlive <= 150) {
                int crackCount = 8;
                float flowAge = (ticksAlive - 30) / 120f;
                float speed = 0.2f + flowAge * 1.5f; // accelerates

                int tendrilBase = crackCount;
                for (int t = 0; t < 5; t++) {
                    for (int s = 0; s < 8; s++) {
                        int idx = tendrilBase + t * 8 + s;
                        if (idx >= handles.size()) break;
                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        Location curr = handles.get(idx).entity().getLocation();
                        double newZ = curr.getZ() - speed;
                        // Reset if past edge
                        if (newZ < edgeLoc.getZ() - 2) {
                            newZ = center.getZ() + 10;
                        }
                        handles.get(idx).entity().teleport(new Location(center.getWorld(), curr.getX(), curr.getY(), newZ));

                        float scaleZ = 1.5f + speed * 0.5f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-0.4f, 0f, -scaleZ * 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.8f, 0.15f, scaleZ),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(2);
                    }
                }

                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.purpleDust(edgeLoc.clone().add(0, 0.5, 0), 15, 3.0);
                }
            }

            // Phase 3 (150+): current fades
            if (ticksAlive > 150) {
                DisplayBuilder.cyanDust(center, 5, 2.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new UndertowCurrent(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 24. Singularity Point
    // A perfect black sphere hovers above the island center, pulling
    // everything toward it, then collapses with an outward shockwave.
    // -------------------------------------------------------------------------
    public static class SingularityPoint extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean collapsed = false;

        public SingularityPoint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("singularity_point", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(800);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location singularityLoc = center.clone().add(0, 15, 0);
            DisplayBuilder.playSound(singularityLoc, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.3f);

            // Main singularity sphere — materializes slowly
            BlockDisplayHandle core = displayBuilder.spawnBlock(singularityLoc, Material.OBSIDIAN);
            core.scale(0.01f, 0.01f, 0.01f).glow(128, 0, 255).interpolation(4, 0);
            handles.add(core);
            spawnedEntities.add(core.entity());

            // Accretion disk — flat ring
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18);
                Location diskLoc = singularityLoc.clone().add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
                BlockDisplayHandle diskNode = displayBuilder.spawnBlock(diskLoc, Material.PURPLE_STAINED_GLASS);
                diskNode.scale(0.4f, 0.15f, 0.4f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(diskNode);
                spawnedEntities.add(diskNode.entity());
            }

            // Particle debris pulled inward
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45);
                double r = 8.0 + Math.random() * 5.0;
                Location debrisLoc = center.clone().add(Math.cos(angle) * r, 5, Math.sin(angle) * r);
                BlockDisplayHandle debris = displayBuilder.spawnBlock(debrisLoc, Material.END_STONE);
                debris.scale(0.3f, 0.3f, 0.3f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(debris);
                spawnedEntities.add(debris.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location singularityLoc = center.clone().add(0, 15, 0);

            // Phase 1 (0-30): singularity materializes
            if (ticksAlive <= 30) {
                float t = ticksAlive / 30f;
                BlockDisplay core = (BlockDisplay) handles.get(0).entity();
                core.setTransformation(new Transformation(
                    new Vector3f(-0.5f * t, -0.5f * t, -0.5f * t),
                    new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                    new Vector3f(t, t, t),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                core.setInterpolationDelay(0);
                core.setInterpolationDuration(4);
                return;
            }

            // Phase 2 (30-130): pull phase — disk spins, debris spirals inward
            if (ticksAlive > 30 && ticksAlive <= 130 && !collapsed) {
                float pullAge = (ticksAlive - 30) / 100f;

                // Core pulses
                BlockDisplay core = (BlockDisplay) handles.get(0).entity();
                float coreScale = 1.0f + (float)Math.sin(ticksAlive * 0.2) * 0.2f;
                core.setTransformation(new Transformation(
                    new Vector3f(-coreScale * 0.5f, -coreScale * 0.5f, -coreScale * 0.5f),
                    new AxisAngle4f(ticksAlive * 0.1f, 0, 1, 0),
                    new Vector3f(coreScale, coreScale, coreScale),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                core.setInterpolationDelay(0);
                core.setInterpolationDuration(2);

                // Disk spins faster over time
                float diskSpeed = 3.0f + pullAge * 8.0f;
                for (int i = 0; i < 20; i++) {
                    int idx = 1 + i;
                    if (idx >= handles.size()) break;
                    double angle = Math.toRadians(i * 18 + ticksAlive * diskSpeed);
                    double r = 2.0 * (1f - pullAge * 0.5f);
                    Location diskLoc = singularityLoc.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                    handles.get(idx).entity().teleport(diskLoc);
                }

                // Debris spirals inward
                for (int i = 0; i < 8; i++) {
                    int idx = 21 + i;
                    if (idx >= handles.size()) break;
                    double angle = Math.toRadians(i * 45 + ticksAlive * 2.5);
                    double r = (8.0 + Math.random() * 5.0) * (1f - pullAge);
                    double y = 5.0 + (10.0 * pullAge);
                    handles.get(idx).entity().teleport(center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r));
                }

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(singularityLoc, 12, 3.0);
                }
            }

            // Phase 3 (130): COLLAPSE — outward shockwave
            if (!collapsed && ticksAlive == 130) {
                collapsed = true;
                DisplayBuilder.purpleDust(singularityLoc, 60, 8.0);
                DisplayBuilder.playSound(singularityLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.2f);
                triggerImpactDamage(singularityLoc);
            }

            // Phase 4 (130-200): shockwave ring expands outward
            if (collapsed && ticksAlive > 130) {
                float shockAge = (ticksAlive - 130) / 70f;
                float r = shockAge * 20f;

                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.toRadians(i * 45 + ticksAlive * 5);
                        Location waveLoc = center.clone().add(Math.cos(angle) * r, 2, Math.sin(angle) * r);
                        DisplayBuilder.purpleDust(waveLoc, 6, 1.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SingularityPoint(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 25. Edge Hunger
    // All four edges pulse then three go dark — the remaining edge surges
    // with a magnetic pull. Deliberate misdirection.
    // -------------------------------------------------------------------------
    public static class EdgeHunger extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int activeEdge = 0; // 0=north, 1=east, 2=south, 3=west

        public EdgeHunger(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("edge_hunger", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(0.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(80);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random active edge
            activeEdge = (int)(Math.random() * 4);

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.7f);

            // Four edge indicator columns — one per edge
            double[][] edgeOffsets = {{0, 0, -20}, {20, 0, 0}, {0, 0, 20}, {-20, 0, 0}};
            for (int e = 0; e < 4; e++) {
                for (int i = 0; i < 8; i++) {
                    double lateral = (i - 3.5) * 2.5;
                    Location edgeLoc;
                    if (e == 0 || e == 2) {
                        edgeLoc = center.clone().add(lateral, 0.5, edgeOffsets[e][2]);
                    } else {
                        edgeLoc = center.clone().add(edgeOffsets[e][0], 0.5, lateral);
                    }
                    BlockDisplayHandle node = displayBuilder.spawnBlock(edgeLoc, Material.PURPLE_STAINED_GLASS);
                    node.scale(0.5f, 1.5f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                    handles.add(node);
                    spawnedEntities.add(node.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < 32) return;

            double[][] edgeOffsets = {{0, 0, -20}, {20, 0, 0}, {0, 0, 20}, {-20, 0, 0}};

            // Phase 1 (0-60): all four edges pulse — misdirection
            if (ticksAlive <= 60) {
                for (int e = 0; e < 4; e++) {
                    float pulse = (float)Math.sin(ticksAlive * 0.3 + e * 1.5) * 0.5f + 0.8f;
                    for (int i = 0; i < 8; i++) {
                        int idx = e * 8 + i;
                        if (idx >= handles.size()) break;
                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        bd.setTransformation(new Transformation(
                            new Vector3f(-0.25f, -pulse * 0.75f, -0.25f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.5f, pulse * 1.5f, 0.5f),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(3);
                    }
                }

                if (ticksAlive % 8 == 0) {
                    for (int e = 0; e < 4; e++) {
                        Location edgeLoc = center.clone().add(edgeOffsets[e][0], 1, edgeOffsets[e][2]);
                        DisplayBuilder.purpleDust(edgeLoc, 10, 3.0);
                    }
                }
            }

            // Phase 2 (60-80): three edges go dark, active flares
            if (ticksAlive > 60 && ticksAlive <= 80) {
                for (int e = 0; e < 4; e++) {
                    for (int i = 0; i < 8; i++) {
                        int idx = e * 8 + i;
                        if (idx >= handles.size()) break;
                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();

                        if (e != activeEdge) {
                            // Shrink inactive edges
                            bd.setTransformation(new Transformation(
                                new Vector3f(-0.01f, -0.01f, -0.01f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.02f, 0.02f, 0.02f),
                                new AxisAngle4f(0, 0, 1, 0)
                            ));
                            bd.setInterpolationDelay(0);
                            bd.setInterpolationDuration(8);
                        } else {
                            // Flare active edge
                            float flare = 1.5f + (float)Math.sin(ticksAlive * 0.5) * 0.5f;
                            bd.setTransformation(new Transformation(
                                new Vector3f(-0.25f, -flare, -0.25f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.5f, flare * 2f, 0.5f),
                                new AxisAngle4f(0, 0, 1, 0)
                            ));
                            bd.setInterpolationDelay(0);
                            bd.setInterpolationDuration(3);
                        }
                    }
                }
            }

            // Phase 3 (80-160): active edge pull
            if (ticksAlive > 80 && ticksAlive <= 160) {
                Location activeEdgeLoc = center.clone().add(edgeOffsets[activeEdge][0], 1, edgeOffsets[activeEdge][2]);
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(activeEdgeLoc, 20, 5.0);
                }

                // Active edge spirals intensify
                for (int i = 0; i < 8; i++) {
                    int idx = activeEdge * 8 + i;
                    if (idx >= handles.size()) break;
                    float surge = 2.0f + (float)Math.sin(ticksAlive * 0.4 + i) * 0.8f;
                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.25f, -surge, -0.25f),
                        new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                        new Vector3f(0.5f, surge * 2f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new EdgeHunger(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 26. Void Leash
    // A thin black tendril shoots out and roots a single player, then begins
    // dragging them toward the nearest island edge.
    // -------------------------------------------------------------------------
    public static class VoidLeash extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public VoidLeash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_leash", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(0.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location leashTarget = center.clone().add(3, 0, 8); // near an edge

            DisplayBuilder.playSound(leashTarget, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);

            // Main leash cord — thin rope-like segments from ground to target
            for (int i = 0; i < 8; i++) {
                float t = i / 8f;
                Location cordLoc = leashTarget.clone().add(0, i * 0.3, t * 4.0);
                BlockDisplayHandle cord = displayBuilder.spawnBlock(cordLoc, Material.OBSIDIAN);
                cord.scale(0.2f, 0.2f, 0.8f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(cord);
                spawnedEntities.add(cord.entity());
            }

            // Root binding rings at target feet
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45);
                Location bindLoc = leashTarget.clone().add(Math.cos(angle) * 0.6, 0.2, Math.sin(angle) * 0.6);
                BlockDisplayHandle bind = displayBuilder.spawnBlock(bindLoc, Material.CRYING_OBSIDIAN);
                bind.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                handles.add(bind);
                spawnedEntities.add(bind.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location leashTarget = center.clone().add(3, 0, 8);
            Location edgeLoc = center.clone().add(0, 0, 20); // south edge

            // Phase 1 (0-40): root — binding rings tighten
            if (ticksAlive <= 40) {
                float pulse = (float)Math.sin(ticksAlive * 0.3) * 0.1f;
                int cordCount = 8;
                for (int i = cordCount; i < handles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.15f - pulse, -0.25f, -0.15f - pulse),
                        new AxisAngle4f(ticksAlive * 0.08f, 0, 1, 0),
                        new Vector3f(0.3f + pulse * 2, 0.5f, 0.3f + pulse * 2),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(leashTarget.clone().add(0, 0.5, 0), 10, 1.5);
                }
            }

            // Phase 2 (40-160): drag toward edge — cord pulls taut
            if (ticksAlive > 40 && ticksAlive <= 160) {
                float dragProgress = (ticksAlive - 40) / 120f;

                // Cord end moves toward edge
                Location draggedLoc = new Location(
                    center.getWorld(),
                    leashTarget.getX() + (edgeLoc.getX() - leashTarget.getX()) * dragProgress,
                    leashTarget.getY(),
                    leashTarget.getZ() + (edgeLoc.getZ() - leashTarget.getZ()) * dragProgress
                );

                // Animate cord segments stretching
                for (int i = 0; i < 8; i++) {
                    float t = i / 8f;
                    Location cordLoc = draggedLoc.clone().add(0, i * 0.2, -dragProgress * 4 + t * 4);
                    handles.get(i).entity().teleport(cordLoc);
                }

                // Move binding rings with the "target"
                for (int i = 8; i < handles.size(); i++) {
                    int nodeIdx = i - 8;
                    double angle = Math.toRadians(nodeIdx * 45);
                    Location bindLoc = draggedLoc.clone().add(Math.cos(angle) * 0.6, 0.2, Math.sin(angle) * 0.6);
                    handles.get(i).entity().teleport(bindLoc);
                }

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(draggedLoc.clone().add(0, 0.5, 0), 8, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidLeash(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 27. Abyssal Tide
    // A visible gravitational distortion wave sweeps across the island — first
    // pressing players down, then launching them up as it passes.
    // -------------------------------------------------------------------------
    public static class AbyssalTide extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean impactFired = false;

        public AbyssalTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_tide", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(640);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Sky darkening effect
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.4f);

            // Wave front — thin vertical wall of distortion
            for (int x = -12; x <= 12; x += 2) {
                for (int y = 0; y < 6; y++) {
                    Location waveLoc = center.clone().add(x, y * 0.5, -22);
                    BlockDisplayHandle waveNode = displayBuilder.spawnBlock(waveLoc, Material.PURPLE_STAINED_GLASS);
                    waveNode.scale(1.8f, 0.5f, 0.1f).glow(80, 0, 160).interpolation(2, 0);
                    handles.add(waveNode);
                    spawnedEntities.add(waveNode.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-20): sky darkens
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 15, -20), 15, 6.0);
                }
                return;
            }

            // Phase 2 (20-80): wave sweeps from north to south across island
            if (ticksAlive >= 20 && ticksAlive <= 80) {
                float waveProgress = (ticksAlive - 20) / 60f;
                double waveZ = -22 + waveProgress * 44; // -22 to +22

                // Advance wave front
                int nodeIdx = 0;
                for (int xi = -12; xi <= 12; xi += 2) {
                    for (int y = 0; y < 6; y++) {
                        if (nodeIdx >= handles.size()) break;
                        Location waveLoc = center.clone().add(xi, y * 0.5, waveZ);
                        handles.get(nodeIdx).entity().teleport(waveLoc);

                        // Wave shape — taller at crest
                        BlockDisplay bd = (BlockDisplay) handles.get(nodeIdx).entity();
                        float heightMod = 0.5f + (float)Math.sin(waveProgress * Math.PI) * 0.5f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-0.9f, -heightMod * 0.5f, 0f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.8f, heightMod, 0.1f),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(2);
                        nodeIdx++;
                    }
                }

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 2, waveZ), 15, 4.0);
                    triggerImpactDamage(center.clone().add(0, 2, waveZ));
                }
            }

            // Phase 3 (80+): wave dissipates
            if (ticksAlive > 80) {
                float decay = (ticksAlive - 80) / 60f;
                if (ticksAlive % 8 == 0 && decay < 1f) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 2, 22), (int)(10 * (1f - decay)) + 1, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new AbyssalTide(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 28. Hunger Pull
    // Sustained suction pulls players toward the island center, with cracks
    // as the actual danger zones.
    // -------------------------------------------------------------------------
    public static class HungerPull extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int CRACK_COUNT = 8;

        public HungerPull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunger_pull", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(700);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);

            // Cracks distributed across the island — 8 1x1 blocks
            double[] crackX = {-5, 3, 7, -8, 0, 5, -3, 9};
            double[] crackZ = {4, -6, 2, 1, -9, 8, -4, -2};

            for (int i = 0; i < CRACK_COUNT; i++) {
                Location crackLoc = center.clone().add(crackX[i], -0.05, crackZ[i]);
                BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.OBSIDIAN);
                crack.scale(1.0f, 0.05f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(crack);
                spawnedEntities.add(crack.entity());
            }

            // Dark air currents spiraling downward — visual suction
            for (int i = 0; i < 16; i++) {
                double angle = Math.toRadians(i * 22.5);
                double r = 5.0 + Math.random() * 8.0;
                Location suctionLoc = center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                BlockDisplayHandle suction = displayBuilder.spawnBlock(suctionLoc, Material.PURPLE_STAINED_GLASS);
                suction.scale(0.3f, 0.8f, 0.3f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(suction);
                spawnedEntities.add(suction.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            double[] crackX = {-5, 3, 7, -8, 0, 5, -3, 9};
            double[] crackZ = {4, -6, 2, 1, -9, 8, -4, -2};

            // Phase 1 (0-20): inhale buildup
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center, 10, 3.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.4f);
                }
                return;
            }

            // Phase 2 (20-120): suction active
            if (ticksAlive >= 20 && ticksAlive <= 120) {
                float pullAge = (ticksAlive - 20) / 100f;

                // Spiral suction nodes converge toward center
                for (int i = 0; i < 16; i++) {
                    int idx = CRACK_COUNT + i;
                    if (idx >= handles.size()) break;
                    double angle = Math.toRadians(i * 22.5 - ticksAlive * 3.0);
                    double r = (5.0 + (i % 3) * 3.0) * (1f - pullAge * 0.5f);
                    double y = 0.5 * (1f - pullAge * 0.8f);
                    Location suctionLoc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                    handles.get(idx).entity().teleport(suctionLoc);
                }

                // Cracks glow
                for (int i = 0; i < CRACK_COUNT; i++) {
                    if (i >= handles.size()) break;
                    float glow = (float)Math.sin(ticksAlive * 0.2 + i) * 0.3f + 0.7f;
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-glow * 0.5f, 0f, -glow * 0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(glow, 0.05f, glow),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);

                    // Crack damage zones
                    if (ticksAlive % 25 == 0) {
                        Location crackLoc = center.clone().add(crackX[i], 0, crackZ[i]);
                        triggerImpactDamage(crackLoc);
                        DisplayBuilder.purpleDust(crackLoc.clone().add(0, 0.5, 0), 12, 1.5);
                    }
                }

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center, 12, 5.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new HungerPull(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 29. Spiral Drain
    // A massive gravitational vortex — End stone dust and dark particles spin
    // clockwise, pulling players toward the center.
    // -------------------------------------------------------------------------
    public static class SpiralDrain extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean ejected = false;

        public SpiralDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spiral_drain", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(2.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location vortexCenter = center.clone().add(0, 0.3, 0);
            DisplayBuilder.playSound(vortexCenter, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.8f);

            // Spiral rings — 3 rings of 20 nodes each, at different radii
            for (int ring = 0; ring < 3; ring++) {
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18 + ring * 60);
                    double r = 2.0 + ring * 3.0;
                    Location spiralLoc = vortexCenter.clone().add(Math.cos(angle) * r, ring * 0.2, Math.sin(angle) * r);
                    BlockDisplayHandle node = displayBuilder.spawnBlock(spiralLoc, Material.PURPLE_STAINED_GLASS);
                    node.scale(0.5f, 0.3f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                    handles.add(node);
                    spawnedEntities.add(node.entity());
                }
            }

            // Debris chunks being pulled in
            for (int i = 0; i < 10; i++) {
                double angle = Math.toRadians(i * 36);
                double r = 5.0 + Math.random() * 4.0;
                Location debrisLoc = vortexCenter.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                BlockDisplayHandle debris = displayBuilder.spawnBlock(debrisLoc, Material.END_STONE);
                debris.scale(0.4f, 0.4f, 0.4f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(debris);
                spawnedEntities.add(debris.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location vortexCenter = center.clone().add(0, 0.3, 0);

            // Phase 1 (0-20): telegraph — dust starts to spiral
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.purpleDust(vortexCenter, 12, 5.0);
                }
            }

            // Phase 2 (20-120): spiral active
            if (ticksAlive >= 20 && ticksAlive <= 120) {
                float spiralAge = (ticksAlive - 20) / 100f;
                float rotSpeed = 3.0f + spiralAge * 6.0f; // spins faster

                // Animate ring nodes
                for (int ring = 0; ring < 3; ring++) {
                    float ringRadius = (2.0f + ring * 3.0f) * (1f - spiralAge * 0.5f);
                    for (int i = 0; i < 20; i++) {
                        int idx = ring * 20 + i;
                        if (idx >= handles.size()) break;
                        double angle = Math.toRadians(i * 18 + ring * 60 + ticksAlive * rotSpeed);
                        Location spiralLoc = vortexCenter.clone().add(
                            Math.cos(angle) * ringRadius, ring * 0.2, Math.sin(angle) * ringRadius
                        );
                        handles.get(idx).entity().teleport(spiralLoc);
                    }
                }

                // Debris spirals inward
                for (int i = 0; i < 10; i++) {
                    int idx = 60 + i;
                    if (idx >= handles.size()) break;
                    double angle = Math.toRadians(i * 36 + ticksAlive * 2.5);
                    double r = (5.0 + Math.random() * 4.0) * (1f - spiralAge * 0.7f);
                    handles.get(idx).entity().teleport(
                        vortexCenter.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r)
                    );
                }

                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.purpleDust(vortexCenter, 10, 4.0);
                    triggerImpactDamage(vortexCenter);
                }
            }

            // Phase 3 (120): violent ejection
            if (!ejected && ticksAlive == 120) {
                ejected = true;
                DisplayBuilder.purpleDust(vortexCenter, 50, 8.0);
                DisplayBuilder.playSound(vortexCenter, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.6f);
                triggerImpactDamage(vortexCenter);
            }

            // Phase 4 (120-160): dissipate
            if (ticksAlive > 120) {
                float decay = (ticksAlive - 120) / 40f;
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(vortexCenter, (int)(8 * (1f - decay)) + 1, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SpiralDrain(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 30. The Maw Opens
    // The actual maw becomes visible below a translucent section of the island.
    // All players are pulled toward center — then violently expelled.
    // -------------------------------------------------------------------------
    public static class TheMawOpens extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean mawFullyOpen = false;
        private boolean expelled = false;

        public TheMawOpens(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_maw_opens", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(900);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(15.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Translucent island surface — central area glows
            for (int ring = 0; ring < 4; ring++) {
                for (int i = 0; i < 24; i++) {
                    double angle = Math.toRadians(i * 15);
                    double r = (ring + 1) * 2.0;
                    Location islandLoc = center.clone().add(Math.cos(angle) * r, -0.1, Math.sin(angle) * r);
                    BlockDisplayHandle panel = displayBuilder.spawnBlock(islandLoc, Material.PURPLE_STAINED_GLASS);
                    panel.scale(1.5f, 0.1f, 1.5f).glow(128, 0, 255).interpolation(4, 0);
                    handles.add(panel);
                    spawnedEntities.add(panel.entity());
                }
            }

            // The maw — concentric rings of teeth below
            for (int row = 0; row < 3; row++) {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    double r = (row + 1) * 1.5;
                    Location toothLoc = center.clone().add(Math.cos(angle) * r, -3.0 - row * 0.8, Math.sin(angle) * r);
                    BlockDisplayHandle tooth = displayBuilder.spawnBlock(toothLoc, Material.OBSIDIAN);
                    tooth.scale(0.5f, 1.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                    handles.add(tooth);
                    spawnedEntities.add(tooth.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.2f);
            DisplayBuilder.purpleDust(center, 30, 6.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int surfaceNodeCount = 4 * 24;

            // Phase 1 (0-120): maw slowly becomes visible — surface glows more intensely
            if (ticksAlive <= 120) {
                float openProgress = ticksAlive / 120f;

                // Surface panels become more transparent (visual glow brightens)
                for (int i = 0; i < surfaceNodeCount && i < handles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float scaleXZ = 1.5f + openProgress * 0.5f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-scaleXZ * 0.5f, -0.05f, -scaleXZ * 0.5f),
                        new AxisAngle4f(ticksAlive * 0.02f, 0, 1, 0),
                        new Vector3f(scaleXZ, 0.1f, scaleXZ),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }

                // Maw teeth rise into visibility
                for (int i = surfaceNodeCount; i < handles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    int relIdx = i - surfaceNodeCount;
                    int row = relIdx / 16;
                    float toothY = -3.0f - row * 0.8f + openProgress * 2.5f;
                    int nodeIdx = relIdx % 16;
                    double angle = Math.toRadians(nodeIdx * 22.5 + ticksAlive * 1.5);
                    double r = (row + 1) * 1.5;
                    Location toothLoc = center.clone().add(Math.cos(angle) * r, toothY, Math.sin(angle) * r);
                    handles.get(i).entity().teleport(toothLoc);
                }

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, -1, 0), 20, 5.0);
                }
            }

            // Phase 2 (120): maw fully open — maximum pull
            if (!mawFullyOpen && ticksAlive == 120) {
                mawFullyOpen = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.3f);
                DisplayBuilder.purpleDust(center, 50, 8.0);
            }

            // Phase 3 (120-160): pull active, center is the danger zone
            if (mawFullyOpen && !expelled && ticksAlive > 120 && ticksAlive <= 160) {
                if (ticksAlive % 10 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.purpleDust(center, 15, 3.0);
                }
            }

            // Phase 4 (160): IMPACT — center players are expelled
            if (!expelled && ticksAlive == 160) {
                expelled = true;
                DisplayBuilder.purpleDust(center, 70, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.2f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheMawOpens(plugin);
        }
    }
}
