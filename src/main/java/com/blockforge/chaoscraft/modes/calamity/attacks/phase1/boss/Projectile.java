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
 * GROUP 2: PROJECTILE
 * Things fired or launched at players — void bolts, tooth shards, ichor blasts,
 * and abyssal debris hurled with lethal accuracy.
 * Attacks 11-20 from the Voidmaw boss design document.
 */
public final class Projectile {

    private Projectile() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidSpit(plugin));
        registry.register(new FangVolley(plugin));
        registry.register(new AbyssalBarrage(plugin));
        registry.register(new BlackTendrilJavelin(plugin));
        registry.register(new VoidMortar(plugin));
        registry.register(new EyeClusterLaunch(plugin));
        registry.register(new IchorTorpedo(plugin));
        registry.register(new ScreamingSkull(plugin));
        registry.register(new VoidScattershot(plugin));
        registry.register(new TheLongTongue(plugin));
    }

    // -------------------------------------------------------------------------
    // 11. Void Spit
    // A pulsating black ichor glob launches upward from below, arcing toward
    // the player. Splashes on impact and leaves a lingering puddle.
    // -------------------------------------------------------------------------
    public static class VoidSpit extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location projectilePos;
        private Location targetPos;
        private boolean hit = false;

        public VoidSpit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_spit", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(3.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(240);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(7.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            projectilePos = center.clone().add(0, -3, 0);
            targetPos = center.clone().add(3, 0, 3); // offset landing zone

            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);
            DisplayBuilder.purpleDust(center.clone().add(0, -1, 0), 10, 2.0);

            // Main ichor sphere
            BlockDisplayHandle sphere = displayBuilder.spawnBlock(projectilePos, Material.CRYING_OBSIDIAN);
            sphere.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(sphere);
            spawnedEntities.add(sphere.entity());

            // Trailing dark particles
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle trail = displayBuilder.spawnBlock(projectilePos.clone().add(0, -i * 0.4, 0),
                        Material.PURPLE_STAINED_GLASS);
                trail.scale(0.4f, 0.4f, 0.4f).glow(80, 0, 160).interpolation(1, 0);
                handles.add(trail);
                spawnedEntities.add(trail.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty() || hit) return;

            // Arc the projectile from spawn point to target (20 tick flight)
            if (ticksAlive <= 20) {
                float t = ticksAlive / 20f;
                double x = projectilePos.getX() + (targetPos.getX() - projectilePos.getX()) * t;
                double z = projectilePos.getZ() + (targetPos.getZ() - projectilePos.getZ()) * t;
                // Parabolic arc height
                double y = projectilePos.getY() + (targetPos.getY() - projectilePos.getY()) * t
                        + Math.sin(t * Math.PI) * 8.0;

                Location currentPos = new Location(center.getWorld(), x, y, z);
                handles.get(0).entity().teleport(currentPos);

                // Animate spin
                BlockDisplay sphere = (BlockDisplay) handles.get(0).entity();
                sphere.setTransformation(new Transformation(
                    new Vector3f(-0.4f, -0.4f, -0.4f),
                    new AxisAngle4f(ticksAlive * 0.2f, 1, 1, 0),
                    new Vector3f(0.8f, 0.8f, 0.8f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                sphere.setInterpolationDelay(0);
                sphere.setInterpolationDuration(2);

                // Trail follows
                for (int i = 1; i < handles.size(); i++) {
                    handles.get(i).entity().teleport(currentPos.clone().add(0, -i * 0.4, 0));
                }

                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.purpleDust(currentPos, 6, 1.0);
                }
            }

            // Impact at tick 20
            if (ticksAlive == 21 && !hit) {
                hit = true;
                DisplayBuilder.purpleDust(targetPos, 25, 3.0);
                DisplayBuilder.playSound(targetPos, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.7f);
                triggerImpactDamage(targetPos);

                // Puddle display
                for (int i = 0; i < handles.size(); i++) {
                    handles.get(i).entity().teleport(targetPos.clone().add(0, 0.05, 0));
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-1.0f, 0f, -1.0f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(2.0f, 0.05f, 2.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
            }

            // Puddle lingers, pulsing (21-120)
            if (hit && ticksAlive % 20 == 0 && ticksAlive <= 120) {
                triggerImpactDamage(targetPos);
                DisplayBuilder.purpleDust(targetPos.clone().add(0, 0.3, 0), 8, 2.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidSpit(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 12. Fang Volley
    // Five enormous black fangs launch upward simultaneously from the void in
    // a spread pattern, landing embedded in the End stone.
    // -------------------------------------------------------------------------
    public static class FangVolley extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int FANG_COUNT = 5;
        private final Location[] landingPositions = new Location[FANG_COUNT];
        private boolean launched = false;

        public FangVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fang_volley", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(9.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(9.0);
            config.setImpactRadius(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.8f);

            // Telegraph: spread pattern particles below
            double[] angles = {0, 72, 144, 216, 288};
            for (int i = 0; i < FANG_COUNT; i++) {
                double angle = Math.toRadians(angles[i]);
                double r = 5.0 + Math.random() * 4.0;
                landingPositions[i] = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                DisplayBuilder.purpleDust(landingPositions[i], 8, 1.0);
            }

            // Spawn fangs below the surface
            for (int i = 0; i < FANG_COUNT; i++) {
                Location fangStart = center.clone().add(0, -5, 0);
                BlockDisplayHandle fang = displayBuilder.spawnBlock(fangStart, Material.OBSIDIAN);
                fang.scale(0.5f, 2.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                handles.add(fang);
                spawnedEntities.add(fang.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < FANG_COUNT) return;

            // Phase 1 (0-20): telegraph with particles at landing zones
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    for (Location landing : landingPositions) {
                        if (landing != null) DisplayBuilder.purpleDust(landing, 5, 1.5);
                    }
                }
                return;
            }

            // Phase 2 (20-40): fangs arc upward and crash down
            if (ticksAlive <= 40) {
                float t = (ticksAlive - 20) / 20f;
                for (int i = 0; i < FANG_COUNT; i++) {
                    if (landingPositions[i] == null) continue;
                    Location start = center.clone().add(0, -5, 0);
                    Location land = landingPositions[i];
                    double x = start.getX() + (land.getX() - start.getX()) * t;
                    double z = start.getZ() + (land.getZ() - start.getZ()) * t;
                    double y = start.getY() + (land.getY() - start.getY()) * t
                            + Math.sin(t * Math.PI) * 10.0;

                    Location fangLoc = new Location(center.getWorld(), x, y, z);
                    handles.get(i).entity().teleport(fangLoc);

                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float angle = t * 3.14f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.25f, -1.25f, -0.25f),
                        new AxisAngle4f(angle, 1, 0, 0),
                        new Vector3f(0.5f, 2.5f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);

                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.purpleDust(fangLoc, 5, 0.8);
                    }
                }
                return;
            }

            // Phase 3 (41): impact — all fangs hit simultaneously
            if (!launched && ticksAlive == 41) {
                launched = true;
                for (int i = 0; i < FANG_COUNT; i++) {
                    if (landingPositions[i] == null) continue;
                    handles.get(i).entity().teleport(landingPositions[i].clone().add(0, 0.5, 0));
                    DisplayBuilder.purpleDust(landingPositions[i].clone().add(0, 1, 0), 20, 2.0);
                    DisplayBuilder.playSound(landingPositions[i], Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f);
                    triggerImpactDamage(landingPositions[i]);
                }
            }

            // Phase 4 (41-80): fangs vibrate then dissolve
            if (launched && ticksAlive > 41) {
                float dissolveProgress = (ticksAlive - 41) / 39f;
                for (int i = 0; i < FANG_COUNT; i++) {
                    if (landingPositions[i] == null || i >= handles.size()) continue;
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float jitter = (float)(Math.sin(ticksAlive * 0.5 + i) * 0.1 * (1f - dissolveProgress));
                    float scaleY = 2.5f * (1f - dissolveProgress * 0.8f);
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.25f + jitter, 0f, -0.25f + jitter),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.5f, scaleY, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new FangVolley(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 13. Abyssal Barrage
    // Rapid-fire void pellets from multiple edge points — a storm of small,
    // fast-moving projectiles spread over 4 seconds.
    // -------------------------------------------------------------------------
    public static class AbyssalBarrage extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> pelletTargets = new ArrayList<>();
        private int nextPellet = 0;
        private int totalPellets = 25;

        public AbyssalBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_barrage", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(2.0);
            config.setDamageRadius(0.8);
            config.setDurationTicks(100);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(2.0);
            config.setImpactRadius(0.8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 2.0f);

            // Telegraph: edge sparks
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(i * 90);
                Location edge = center.clone().add(Math.cos(angle) * 20, 0.5, Math.sin(angle) * 20);
                DisplayBuilder.purpleDust(edge, 15, 3.0);
            }

            // Pre-generate random pellet landing positions
            for (int i = 0; i < totalPellets; i++) {
                double rx = (Math.random() - 0.5) * 24;
                double rz = (Math.random() - 0.5) * 24;
                pelletTargets.add(center.clone().add(rx, 0, rz));
            }

            // Pre-spawn pellet displays hidden below
            for (int i = 0; i < totalPellets; i++) {
                Location startLoc = center.clone().add(
                    (Math.random() - 0.5) * 30, -4, (Math.random() - 0.5) * 30
                );
                BlockDisplayHandle pellet = displayBuilder.spawnBlock(startLoc, Material.CRYING_OBSIDIAN);
                pellet.scale(0.25f, 0.25f, 0.25f).glow(128, 0, 255).interpolation(1, 0);
                handles.add(pellet);
                spawnedEntities.add(pellet.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Launch one pellet every 3 ticks for 75 ticks
            if (ticksAlive > 5 && ticksAlive % 3 == 0 && nextPellet < totalPellets) {
                int idx = nextPellet;
                nextPellet++;
                if (idx < handles.size() && idx < pelletTargets.size()) {
                    Location target = pelletTargets.get(idx);
                    Location start = center.clone().add(
                        (Math.random() - 0.5) * 30, -3, (Math.random() - 0.5) * 30
                    );
                    // Teleport to start, then animate toward target
                    handles.get(idx).entity().teleport(start);

                    // Schedule fast arc — these are single-tick launches
                    DisplayBuilder.purpleDust(start.clone().add(0, 1, 0), 4, 0.5);
                }
            }

            // Animate in-flight pellets (rough arc simulation)
            if (ticksAlive > 5) {
                for (int i = 0; i < nextPellet && i < handles.size(); i++) {
                    int pelletAge = ticksAlive - 5 - (i * 3);
                    if (pelletAge < 0 || pelletAge > 12) continue;

                    Location target = pelletTargets.get(i);
                    float t = pelletAge / 12f;
                    Location start = center.clone().add(
                        target.getX() - center.getX() > 0 ? -20 : 20, -3,
                        target.getZ() - center.getZ() > 0 ? -20 : 20
                    );

                    double x = start.getX() + (target.getX() - start.getX()) * t;
                    double z = start.getZ() + (target.getZ() - start.getZ()) * t;
                    double y = start.getY() + (target.getY() - start.getY()) * t
                            + Math.sin(t * Math.PI) * 5.0;

                    handles.get(i).entity().teleport(new Location(center.getWorld(), x, y, z));

                    // Impact when reaching target
                    if (pelletAge == 11) {
                        DisplayBuilder.purpleDust(target, 8, 1.0);
                        triggerImpactDamage(target);
                    }
                }
            }

            // Edge spark effect during barrage
            if (ticksAlive < 75 && ticksAlive % 8 == 0) {
                double angle = Math.toRadians(ticksAlive * 40);
                Location sparkEdge = center.clone().add(Math.cos(angle) * 20, 0.5, Math.sin(angle) * 20);
                DisplayBuilder.purpleDust(sparkEdge, 8, 2.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new AbyssalBarrage(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 14. Black Tendril Javelin
    // A single massive void tendril tears free and is hurled like a javelin —
    // embeds at an angle and pulses damage while standing.
    // -------------------------------------------------------------------------
    public static class BlackTendrilJavelin extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean embedded = false;
        private Location embeddedLoc;

        public BlackTendrilJavelin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_tendril_javelin", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(440);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            embeddedLoc = center.clone().add(4, 0, 2);

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);

            // Main javelin body — long, thin
            Location launchPos = center.clone().add(-15, 8, -8);
            BlockDisplayHandle shaft = displayBuilder.spawnBlock(launchPos, Material.OBSIDIAN);
            shaft.scale(0.5f, 5.0f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(shaft);
            spawnedEntities.add(shaft.entity());

            // Tendril texture segments
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle seg = displayBuilder.spawnBlock(launchPos.clone().add(0, i * 1.2, 0),
                        Material.CRYING_OBSIDIAN);
                seg.scale(0.35f, 1.0f, 0.35f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(seg);
                spawnedEntities.add(seg.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-25): javelin in flight
            if (ticksAlive <= 25 && !embedded) {
                float t = ticksAlive / 25f;
                Location startPos = center.clone().add(-15, 8, -8);

                double x = startPos.getX() + (embeddedLoc.getX() - startPos.getX()) * t;
                double z = startPos.getZ() + (embeddedLoc.getZ() - startPos.getZ()) * t;
                double y = startPos.getY() + (embeddedLoc.getY() - startPos.getY()) * t
                        - t * t * 6; // gravity arc

                Location flightPos = new Location(center.getWorld(), x, y, z);
                handles.get(0).entity().teleport(flightPos);

                // Spin during flight
                BlockDisplay shaft = (BlockDisplay) handles.get(0).entity();
                shaft.setTransformation(new Transformation(
                    new Vector3f(-0.25f, -2.5f, -0.25f),
                    new AxisAngle4f(t * 4.0f, 1, 0.3f, 0),
                    new Vector3f(0.5f, 5.0f, 0.5f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                shaft.setInterpolationDelay(0);
                shaft.setInterpolationDuration(2);

                // Trail segments
                for (int i = 1; i < Math.min(handles.size(), 5); i++) {
                    handles.get(i).entity().teleport(flightPos.clone().add(0, i * 1.2, 0));
                }

                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.purpleDust(flightPos, 6, 0.8);
                }
            }

            // Phase 2 (26): embed on impact
            if (!embedded && ticksAlive == 26) {
                embedded = true;
                handles.get(0).entity().teleport(embeddedLoc.clone().add(0, 1.5, 0));

                BlockDisplay shaft = (BlockDisplay) handles.get(0).entity();
                // Angled embed — tilted like a javelin stuck in ground
                shaft.setTransformation(new Transformation(
                    new Vector3f(-0.25f, 0f, -0.25f),
                    new AxisAngle4f(0.4f, 0, 0, 1),
                    new Vector3f(0.5f, 5.0f, 0.5f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                shaft.setInterpolationDelay(0);
                shaft.setInterpolationDuration(3);

                DisplayBuilder.purpleDust(embeddedLoc.clone().add(0, 2, 0), 30, 4.0);
                DisplayBuilder.playSound(embeddedLoc, Sound.BLOCK_STONE_PLACE, 1.0f, 0.4f);
                triggerImpactDamage(embeddedLoc);

                // Reposition trail segments around embed point
                for (int i = 1; i < Math.min(handles.size(), 5); i++) {
                    handles.get(i).entity().teleport(embeddedLoc.clone().add(0, 0.3 + i * 0.4, 0));
                }
            }

            // Phase 3 (26-120): pulse damage while embedded
            if (embedded && ticksAlive % 20 == 0 && ticksAlive > 26) {
                triggerImpactDamage(embeddedLoc);
                DisplayBuilder.purpleDust(embeddedLoc.clone().add(0, 2.5, 0), 15, 5.0);

                // Pulse visual
                BlockDisplay shaft = (BlockDisplay) handles.get(0).entity();
                shaft.setTransformation(new Transformation(
                    new Vector3f(-0.3f, 0f, -0.3f),
                    new AxisAngle4f(0.4f, 0, 0, 1),
                    new Vector3f(0.6f, 5.0f, 0.6f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                shaft.setInterpolationDelay(0);
                shaft.setInterpolationDuration(4);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new BlackTendrilJavelin(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 15. Void Mortar
    // A slow, arcing void energy orb with delayed detonation. Lands then
    // explodes upward in a pillar of void fire.
    // -------------------------------------------------------------------------
    public static class VoidMortar extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean detonated = false;
        private Location landLoc;

        public VoidMortar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_mortar", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(5.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            landLoc = center.clone().add(2, 0, 2);

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.7f);
            DisplayBuilder.purpleDust(center.clone().add(0, -2, 0), 15, 2.0);

            // Main orb — black-white void core
            Location launchPos = center.clone().add(-8, -6, -8);
            BlockDisplayHandle orb = displayBuilder.spawnBlock(launchPos, Material.OBSIDIAN);
            orb.scale(1.2f, 1.2f, 1.2f).glow(200, 200, 255).interpolation(2, 0);
            handles.add(orb);
            spawnedEntities.add(orb.entity());

            // Outer shell
            BlockDisplayHandle shell = displayBuilder.spawnBlock(launchPos.clone().add(0, 0.2, 0),
                    Material.PURPLE_STAINED_GLASS);
            shell.scale(1.6f, 1.6f, 1.6f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(shell);
            spawnedEntities.add(shell.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-40): slow high arc toward center
            if (ticksAlive <= 40) {
                float t = ticksAlive / 40f;
                Location startPos = center.clone().add(-8, -6, -8);
                double x = startPos.getX() + (landLoc.getX() - startPos.getX()) * t;
                double z = startPos.getZ() + (landLoc.getZ() - startPos.getZ()) * t;
                double y = startPos.getY() + (landLoc.getY() - startPos.getY()) * t
                        + Math.sin(t * Math.PI) * 15.0;

                Location orbPos = new Location(center.getWorld(), x, y, z);
                handles.get(0).entity().teleport(orbPos);
                handles.get(1).entity().teleport(orbPos.clone().add(0, 0.2, 0));

                // Slow rotation
                BlockDisplay orb = (BlockDisplay) handles.get(0).entity();
                orb.setTransformation(new Transformation(
                    new Vector3f(-0.6f, -0.6f, -0.6f),
                    new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                orb.setInterpolationDelay(0);
                orb.setInterpolationDuration(3);

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.purpleDust(orbPos, 8, 1.5);
                }
            }

            // Phase 2 (40-40): orb sinks slightly into ground
            if (ticksAlive == 40) {
                handles.get(0).entity().teleport(landLoc.clone().add(0, 0.3, 0));
                handles.get(1).entity().teleport(landLoc.clone().add(0, 0.5, 0));
                DisplayBuilder.playSound(landLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.3f);
            }

            // Phase 3 (40-55): sinking + delay before detonation
            if (ticksAlive > 40 && ticksAlive < 55) {
                float sinkT = (ticksAlive - 40) / 15f;
                handles.get(0).entity().teleport(landLoc.clone().add(0, 0.3 - sinkT * 0.5, 0));
                DisplayBuilder.purpleDust(landLoc.clone().add(0, 0.3, 0), 5, 1.5);
            }

            // Phase 4 (55): DETONATE upward pillar
            if (!detonated && ticksAlive == 55) {
                detonated = true;
                DisplayBuilder.playSound(landLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.3f);
                DisplayBuilder.purpleDust(landLoc.clone().add(0, 5, 0), 60, 4.0);
                triggerImpactDamage(landLoc);

                // Visual pillar
                for (int h = 0; h < 10; h++) {
                    handles.get(0).entity().teleport(landLoc.clone().add(0, h * 1.0, 0));
                    BlockDisplay orb = (BlockDisplay) handles.get(0).entity();
                    orb.setTransformation(new Transformation(
                        new Vector3f(-1.0f, -0.5f, -1.0f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(2.0f, 1.0f, 2.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    orb.setInterpolationDelay(0);
                    orb.setInterpolationDuration(4);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidMortar(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 16. Eye Cluster Launch
    // 12 organic eye-spheres rain down across the island. Each emits a gaze
    // radius that pulses danger.
    // -------------------------------------------------------------------------
    public static class EyeClusterLaunch extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int EYE_COUNT = 12;
        private final Location[] eyePositions = new Location[EYE_COUNT];
        private boolean landed = false;

        public EyeClusterLaunch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eye_cluster_launch", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(3.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(560);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 1.0f, 0.5f);

            // Generate eye landing positions scattered across island
            for (int i = 0; i < EYE_COUNT; i++) {
                double angle = Math.toRadians(i * (360.0 / EYE_COUNT));
                double r = 3.0 + Math.random() * 12.0;
                eyePositions[i] = center.clone().add(
                    Math.cos(angle) * r, 0.3, Math.sin(angle) * r
                );
            }

            // Spawn eyes starting from below
            for (int i = 0; i < EYE_COUNT; i++) {
                Location startLoc = center.clone().add(0, -5, 0);
                BlockDisplayHandle eye = displayBuilder.spawnBlock(startLoc, Material.CRIMSON_NYLIUM);
                eye.scale(0.6f, 0.6f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                handles.add(eye);
                spawnedEntities.add(eye.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < EYE_COUNT) return;

            // Phase 1 (0-10): telegraph flash
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 15, 5.0);
                }
                return;
            }

            // Phase 2 (10-30): eyes arc through air toward positions
            if (ticksAlive <= 30) {
                float t = (ticksAlive - 10) / 20f;
                for (int i = 0; i < EYE_COUNT; i++) {
                    if (eyePositions[i] == null || i >= handles.size()) continue;
                    Location start = center.clone().add(0, -5, 0);
                    Location end = eyePositions[i];
                    double x = start.getX() + (end.getX() - start.getX()) * t;
                    double z = start.getZ() + (end.getZ() - start.getZ()) * t;
                    double y = start.getY() + (end.getY() - start.getY()) * t
                            + Math.sin(t * Math.PI) * 8.0;
                    handles.get(i).entity().teleport(new Location(center.getWorld(), x, y, z));

                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(new Location(center.getWorld(), x, y, z), 4, 0.5);
                    }
                }
            }

            // Phase 3 (31): eyes land
            if (!landed && ticksAlive == 31) {
                landed = true;
                for (int i = 0; i < EYE_COUNT; i++) {
                    if (eyePositions[i] == null || i >= handles.size()) continue;
                    handles.get(i).entity().teleport(eyePositions[i]);
                    DisplayBuilder.crimsonDust(eyePositions[i], 10, 2.0);
                    DisplayBuilder.playSound(eyePositions[i], Sound.ENTITY_SHULKER_OPEN, 0.5f, 0.8f);
                    triggerImpactDamage(eyePositions[i]);
                }
            }

            // Phase 4 (31-200): eyes pulse gaze — slow rotation and damage pulses
            if (landed && ticksAlive > 31) {
                for (int i = 0; i < EYE_COUNT; i++) {
                    if (eyePositions[i] == null || i >= handles.size()) continue;
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float rot = ticksAlive * 0.04f + i * 0.5f;
                    float pulse = 0.5f + (float)Math.sin(ticksAlive * 0.15 + i) * 0.15f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-pulse * 0.5f, -pulse * 0.5f, -pulse * 0.5f),
                        new AxisAngle4f(rot, 0, 1, 0),
                        new Vector3f(pulse, pulse, pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }

                // Damage pulse every 30 ticks
                if (ticksAlive % 30 == 0) {
                    for (int i = 0; i < EYE_COUNT; i++) {
                        if (eyePositions[i] != null) {
                            triggerImpactDamage(eyePositions[i]);
                            DisplayBuilder.crimsonDust(eyePositions[i].clone().add(0, 0.5, 0), 6, 2.0);
                        }
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
            return new EyeClusterLaunch(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 17. Ichor Torpedo
    // A fast void energy bolt launched horizontally at waist height across
    // the full island diameter. Must jump to avoid.
    // -------------------------------------------------------------------------
    public static class IchorTorpedo extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean fired = false;

        public IchorTorpedo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ichor_torpedo", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(360);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph: horizontal streak of particles at waist height
            for (int i = -12; i <= 12; i++) {
                Location streakLoc = center.clone().add(i, 1.5, -20);
                DisplayBuilder.purpleDust(streakLoc, 3, 0.3);
            }
            DisplayBuilder.playSound(center.clone().add(0, 0, -20), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f);

            // Torpedo — elongated bolt
            Location startPos = center.clone().add(0, 1.5, -22);
            BlockDisplayHandle bolt = displayBuilder.spawnBlock(startPos, Material.OBSIDIAN);
            bolt.scale(1.0f, 0.6f, 3.0f).glow(128, 0, 255).interpolation(1, 0);
            handles.add(bolt);
            spawnedEntities.add(bolt.entity());

            // Streak trail
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle trail = displayBuilder.spawnBlock(startPos.clone().add(0, 0, -i * 1.5),
                        Material.PURPLE_STAINED_GLASS);
                trail.scale(0.6f, 0.4f, 1.5f).glow(80, 0, 160).interpolation(1, 0);
                handles.add(trail);
                spawnedEntities.add(trail.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-10): telegraph
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.5, -20), 10, 1.5);
                }
                return;
            }

            // Phase 2 (10-50): torpedo travels at high speed across island
            if (ticksAlive <= 50) {
                float t = (ticksAlive - 10) / 40f;
                double zPos = -22 + t * 44; // -22 to +22

                Location torpedoLoc = center.clone().add(0, 1.5, zPos);
                handles.get(0).entity().teleport(torpedoLoc);

                // Trail segments
                for (int i = 1; i < handles.size(); i++) {
                    handles.get(i).entity().teleport(torpedoLoc.clone().add(0, 0, -i * 1.5));
                }

                // Continuous damage in path
                if (ticksAlive % 5 == 0) {
                    triggerImpactDamage(torpedoLoc);
                    DisplayBuilder.purpleDust(torpedoLoc, 6, 1.5);
                }

                // Streak particles
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.purpleDust(torpedoLoc.clone().add(0, 0, -2), 4, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new IchorTorpedo(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 18. Screaming Skull
    // A massive void-formed skull floats slowly toward the player cluster,
    // emitting a wail. Implodes after 6 seconds.
    // -------------------------------------------------------------------------
    public static class ScreamingSkull extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean imploded = false;

        public ScreamingSkull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("screaming_skull", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(700);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(11.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);

            // Skull starts at island edge, 3 blocks above surface
            Location skullStart = center.clone().add(-18, 3, -10);

            // Main skull head
            BlockDisplayHandle skull = displayBuilder.spawnBlock(skullStart, Material.OBSIDIAN);
            skull.scale(2.5f, 2.5f, 2.5f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(skull);
            spawnedEntities.add(skull.entity());

            // Eye sockets — crimson glow
            for (int eye = 0; eye < 2; eye++) {
                Location eyeLoc = skullStart.clone().add(-0.5 + eye * 1.2, 0.5, -1.3);
                BlockDisplayHandle eyeBlock = displayBuilder.spawnBlock(eyeLoc, Material.CRIMSON_NYLIUM);
                eyeBlock.scale(0.5f, 0.5f, 0.2f).glow(200, 0, 50).interpolation(2, 0);
                handles.add(eyeBlock);
                spawnedEntities.add(eyeBlock.entity());
            }

            // Jaw void gap
            BlockDisplayHandle jaw = displayBuilder.spawnBlock(skullStart.clone().add(0, -0.8, -1.3),
                    Material.CRYING_OBSIDIAN);
            jaw.scale(1.5f, 0.4f, 0.2f).glow(80, 0, 160).interpolation(2, 0);
            handles.add(jaw);
            spawnedEntities.add(jaw.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location skullStart = center.clone().add(-18, 3, -10);

            // Phase 1 (0-120): skull floats slowly toward center
            if (ticksAlive <= 120 && !imploded) {
                float t = ticksAlive / 120f;
                Location targetLoc = center.clone().add(0, 3, 0);
                double x = skullStart.getX() + (targetLoc.getX() - skullStart.getX()) * t;
                double z = skullStart.getZ() + (targetLoc.getZ() - skullStart.getZ()) * t;
                double y = 3.0 + Math.sin(t * Math.PI * 2) * 0.5; // gentle bob

                Location skullPos = new Location(center.getWorld(), x, center.getY() + y, z);
                handles.get(0).entity().teleport(skullPos);

                // Update eye and jaw positions relative to skull
                handles.get(1).entity().teleport(skullPos.clone().add(-0.5, 0.5, -1.3));
                handles.get(2).entity().teleport(skullPos.clone().add(0.7, 0.5, -1.3));
                if (handles.size() > 3) handles.get(3).entity().teleport(skullPos.clone().add(0, -0.8, -1.3));

                // Wailing sound and particle trail
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(skullPos, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.6f);
                    DisplayBuilder.purpleDust(skullPos, 10, 2.0);
                }

                // Rotate slowly
                BlockDisplay bd = (BlockDisplay) handles.get(0).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-1.25f, -1.25f, -1.25f),
                    new AxisAngle4f(ticksAlive * 0.02f, 0, 1, 0),
                    new Vector3f(2.5f, 2.5f, 2.5f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }

            // Phase 2 (120): IMPLODE
            if (!imploded && ticksAlive == 120) {
                imploded = true;
                Location implodeLoc = handles.get(0).entity().getLocation();
                DisplayBuilder.purpleDust(implodeLoc, 50, 6.0);
                DisplayBuilder.playSound(implodeLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.3f);
                triggerImpactDamage(implodeLoc);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ScreamingSkull(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 19. Void Scattershot
    // 16 bolts land simultaneously in a 4x4 grid pattern centered on the
    // island. Players must find the gaps.
    // -------------------------------------------------------------------------
    public static class VoidScattershot extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean fired = false;
        private final Location[] gridPositions = new Location[16];

        public VoidScattershot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_scattershot", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(520);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build 4x4 grid positions centered on island
            int gridIdx = 0;
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    double x = -6.0 + col * 4.0;
                    double z = -6.0 + row * 4.0;
                    gridPositions[gridIdx] = center.clone().add(x, 0, z);
                    gridIdx++;
                }
            }

            // Telegraph: glowing orifices appear at grid positions
            for (Location gp : gridPositions) {
                if (gp != null) DisplayBuilder.purpleDust(gp, 8, 1.0);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.2f);

            // Pre-spawn 16 bolts above
            for (int i = 0; i < 16; i++) {
                Location boltStart = center.clone().add(0, 18, 0);
                BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltStart, Material.OBSIDIAN);
                bolt.scale(0.5f, 2.0f, 0.5f).glow(128, 0, 255).interpolation(1, 0);
                handles.add(bolt);
                spawnedEntities.add(bolt.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < 16) return;

            // Phase 1 (0-40): telegraph — bolts glow above grid positions
            if (ticksAlive < 40) {
                for (int i = 0; i < 16; i++) {
                    if (gridPositions[i] == null) continue;
                    Location aboveLoc = gridPositions[i].clone().add(0, 18 - ticksAlive * 0.1, 0);
                    handles.get(i).entity().teleport(aboveLoc);

                    if (ticksAlive % 5 == 0) {
                        DisplayBuilder.purpleDust(gridPositions[i], 5, 1.0);
                    }
                }
                return;
            }

            // Phase 2 (40): all 16 bolts crash simultaneously
            if (!fired && ticksAlive == 40) {
                fired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.8f);
                for (int i = 0; i < 16; i++) {
                    if (gridPositions[i] == null || i >= handles.size()) continue;
                    handles.get(i).entity().teleport(gridPositions[i].clone().add(0, 1.0, 0));
                    DisplayBuilder.purpleDust(gridPositions[i].clone().add(0, 1, 0), 15, 1.5);
                    triggerImpactDamage(gridPositions[i]);
                }
            }

            // Phase 3 (40-80): bolts shrink and dissolve
            if (fired && ticksAlive > 40) {
                float decay = (ticksAlive - 40) / 40f;
                float scaleY = 2.0f * (1f - decay);
                if (scaleY < 0.01f) scaleY = 0.01f;

                for (int i = 0; i < 16 && i < handles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.25f, 0f, -0.25f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.5f, scaleY, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidScattershot(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 20. The Long Tongue
    // An impossibly fast whip-thin tongue lashes across the island at ground
    // level. Nearly instant — must jump during the telegraph.
    // -------------------------------------------------------------------------
    public static class TheLongTongue extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean swept = false;

        public TheLongTongue(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_long_tongue", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(13.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(13.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph: particles gather at one island edge in a thin horizontal line
            for (int i = -12; i <= 12; i += 2) {
                Location telegraphLoc = center.clone().add(i, 1.5, -20);
                DisplayBuilder.purpleDust(telegraphLoc, 5, 0.4);
            }

            DisplayBuilder.playSound(center.clone().add(0, 0, -20), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.8f);
            DisplayBuilder.playSound(center.clone().add(0, 0, -20), Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 2.0f);

            // Pre-spawn tongue segments (initially off to the side)
            for (int i = 0; i < 20; i++) {
                Location offscreen = center.clone().add(i * 2.5 - 25, 1.3, -25);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(offscreen, Material.OBSIDIAN);
                seg.scale(2.5f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                handles.add(seg);
                spawnedEntities.add(seg.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-40): telegraph window — particles pulse
            if (ticksAlive < 40) {
                if (ticksAlive % 5 == 0) {
                    for (int i = -12; i <= 12; i += 4) {
                        Location telegraphLoc = center.clone().add(i, 1.5, -20);
                        DisplayBuilder.purpleDust(telegraphLoc, 4, 0.5);
                    }
                }
                return;
            }

            // Phase 2 (40-46): ultra-fast sweep — near instant
            if (!swept && ticksAlive == 40) {
                swept = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 2.0f);

                // Position tongue spanning full island at ground level
                for (int i = 0; i < handles.size(); i++) {
                    Location tongueLoc = center.clone().add(i * 2.5 - 24, 1.3, 0);
                    handles.get(i).entity().teleport(tongueLoc);

                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-1.25f, -0.15f, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(2.5f, 0.3f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(1);
                }

                // Single sweeping damage across full width
                for (int x = -20; x <= 20; x += 4) {
                    Location hitLoc = center.clone().add(x, 1.2, 0);
                    triggerImpactDamage(hitLoc);
                    DisplayBuilder.purpleDust(hitLoc, 8, 1.0);
                }
            }

            // Phase 3 (40-60): tongue retracts rapidly
            if (swept && ticksAlive > 40) {
                float retr = (ticksAlive - 40) / 20f;
                for (int i = 0; i < handles.size(); i++) {
                    double xPos = (i * 2.5 - 24) + retr * (25 + i * 2.5);
                    Location retractLoc = center.clone().add(xPos, 1.3, 0);
                    handles.get(i).entity().teleport(retractLoc);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheLongTongue(plugin);
        }
    }
}
