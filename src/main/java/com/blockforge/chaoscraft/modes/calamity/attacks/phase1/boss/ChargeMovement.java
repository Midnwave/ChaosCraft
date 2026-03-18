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
 * Phase 1 Boss Attacks — GROUP 9: CHARGE / MOVEMENT
 * 10 aggressive charge and movement attacks featuring fast-moving display structures
 * that sweep, surge, and slam across the arena.
 * Attacks 81-90 from boss1-voidmaw.md.
 * NO status effects — damage only.
 */
public final class ChargeMovement {

    private ChargeMovement() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BreachCharge(plugin));
        registry.register(new RimSprint(plugin));
        registry.register(new MawRush(plugin));
        registry.register(new DeepDive(plugin));
        registry.register(new SerpentRoll(plugin));
        registry.register(new HungerLunge(plugin));
        registry.register(new SpiralDescent(plugin));
        registry.register(new VoidRampage(plugin));
        registry.register(new TheSurge(plugin));
        registry.register(new EarthBreaker(plugin));
    }

    // ================================================================
    // 81. BREACH CHARGE — Voidmaw erupts through island in a straight line
    // ================================================================
    public static class BreachCharge extends BossAttack {
        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private boolean breachFired = false;
        private int sweepTick = 0;

        public BreachCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("breach_charge", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(36.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crack line forming over 3 seconds (edge toward center)
            for (int i = 0; i < 20; i++) {
                Location loc = center.clone().add(-15 + i * 1.5, 0.08, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.2f, 0.12f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
                crackHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: cracks form progressively from edge to center (0-60 ticks)
            if (ticksAlive < 60) {
                int cracksVisible = (int)(ticksAlive / 60.0f * crackHandles.size());
                for (int i = 0; i < crackHandles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) crackHandles.get(i).entity();
                    float opacity = i <= cracksVisible ? 1.0f : 0.0f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.1f, 0, -0.75f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f, opacity < 0.5f ? 0.01f : 0.12f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(-10, 0.5, 0), 5, 2.0);
                }
            }
            // Breach fires at tick 60
            else if (ticksAlive == 60 && !breachFired) {
                breachFired = true;
                sweepTick = 0;
                // Remove crack indicators
                for (BlockDisplayHandle h : crackHandles) h.entity().remove();
                crackHandles.clear();
                // Spawn tall breach body structure at starting edge
                for (int y = 0; y <= 25; y += 2) {
                    for (int z = -2; z <= 2; z++) {
                        Location loc = center.clone().add(-16, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(1.8f, 1.8f, 1.0f).glow(128, 0, 255).interpolation(1, 0);
                        bodyHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.3f);
            }
            // Sweep body across island at high speed (60-160 ticks)
            else if (breachFired && sweepTick < 100) {
                sweepTick++;
                float sweepX = -16 + (sweepTick / 100.0f) * 32;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    Location base = bodyHandles.get(i).entity().getLocation();
                    int z = (i % 5) - 2;
                    int y = (i / 5) * 2;
                    bodyHandles.get(i).entity().teleport(center.clone().add(sweepX, y, z));
                }
                if (sweepTick % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(sweepX, 1, 0), 8, 3.5);
                }
                // Crash-down effect when sweep completes
                if (sweepTick == 100) {
                    DisplayBuilder.purpleDust(center.clone().add(16, 0, 0), 40, 8.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BreachCharge(plugin); }
    }

    // ================================================================
    // 82. RIM SPRINT — Tentacle strikes around full 360-degree island edge
    // ================================================================
    public static class RimSprint extends BossAttack {
        private final List<BlockDisplayHandle> tentacleHandles = new ArrayList<>();
        private int lastStrikeAngle = 0;

        public RimSprint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rim_sprint", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(18.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rumble indicator on first side
            BlockDisplayHandle rimIndicator = displayBuilder.spawnBlock(
                center.clone().add(-18, 0.1, 0), Material.CRYING_OBSIDIAN);
            rimIndicator.scale(3.0f, 0.2f, 3.0f).glow(200, 0, 50).interpolation(4, 0);
            tentacleHandles.add(rimIndicator);
            spawnedEntities.add(rimIndicator.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sprint begins at tick 40, full 360 over 100 ticks
            if (ticksAlive >= 40) {
                int sprintTick = ticksAlive - 40;
                float angle = (float)(sprintTick / 100.0 * Math.PI * 2);
                float strikeAngleDeg = (int)(Math.toDegrees(angle));

                // Strike every 18 degrees (20 strikes total around rim)
                if (strikeAngleDeg - lastStrikeAngle >= 18 && sprintTick <= 100) {
                    lastStrikeAngle = (int)(strikeAngleDeg / 18) * 18;
                    double sa = Math.toRadians(lastStrikeAngle);
                    Location strikeLoc = center.clone().add(Math.cos(sa) * 16, 0, Math.sin(sa) * 16);

                    // Tentacle strike visual: tall spike from below
                    for (int y = -3; y <= 6; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            strikeLoc.clone().add(0, y, 0), Material.OBSIDIAN);
                        float scl = 1.2f - (y + 3) * 0.08f;
                        h.scale(scl, 1.0f, scl).glow(200, 0, 50).interpolation(1, 0);
                        tentacleHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    DisplayBuilder.crimsonDust(strikeLoc, 15, 4.0);
                    DisplayBuilder.playSound(strikeLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
                }
                // Sprint wake particles
                if (sprintTick <= 100 && sprintTick % 3 == 0) {
                    Location wakeLoc = center.clone().add(Math.cos(angle) * 18, 0.3, Math.sin(angle) * 18);
                    DisplayBuilder.crimsonDust(wakeLoc, 4, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RimSprint(plugin); }
    }

    // ================================================================
    // 83. MAW RUSH — Quadrant bulge tosses players into the air
    // ================================================================
    public static class MawRush extends BossAttack {
        private final List<BlockDisplayHandle> bulgeHandles = new ArrayList<>();
        private boolean bulged = false;
        private boolean subsided = false;

        public MawRush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("maw_rush", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(14.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Quadrant glow from below (NE quadrant)
            for (int x = 0; x <= 12; x += 2) {
                for (int z = 0; z <= 12; z += 2) {
                    Location loc = center.clone().add(x, -0.2, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.9f, 0.1f, 0.9f).glow(128, 0, 255).interpolation(4, 0);
                    bulgeHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pre-bulge warning glow (0-50 ticks)
            if (ticksAlive < 50 && !bulged) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(6, 0, 6), 10, 7.0);
                }
            }
            // Bulge event (tick 50)
            else if (ticksAlive == 50 && !bulged) {
                bulged = true;
                // Raise all quadrant blocks upward 3 blocks
                for (BlockDisplayHandle h : bulgeHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(loc.clone().add(0, 3, 0));
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.45f, -0.05f, -0.45f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.9f, 0.5f, 0.9f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                DisplayBuilder.purpleDust(center.clone().add(6, 3, 6), 25, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.5f);
            }
            // Subside shockwave (tick 90) — adjacent quadrants hit
            else if (ticksAlive == 90 && bulged && !subsided) {
                subsided = true;
                // Drop quadrant back
                for (BlockDisplayHandle h : bulgeHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(loc.clone().add(0, -3, 0));
                }
                // Shockwave rings to adjacent quadrants
                for (int i = 0; i < 20; i++) {
                    double a = (Math.PI * 2 / 20) * i;
                    for (int r = 10; r <= 14; r++) {
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r), 3, 0.8);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MawRush(plugin); }
    }

    // ================================================================
    // 84. DEEP DIVE — Silence then massive half-island slam on return
    // ================================================================
    public static class DeepDive extends BossAttack {
        private final List<BlockDisplayHandle> slamHandles = new ArrayList<>();
        private boolean slamFired = false;

        public DeepDive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deep_dive", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(26.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eerie silence visual: everything dims, void goes dark
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location loc = center.clone().add(Math.cos(a) * 20, 0.1, Math.sin(a) * 20);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 0.05f, 1.0f).glow(40, 0, 80).interpolation(5, 0);
                slamHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Silence: no sound on spawn
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Silence period (0-120 ticks = 6 seconds)
            if (ticksAlive < 120 && !slamFired) {
                // No sound, no particles — psychological dread
                if (ticksAlive == 60) {
                    DisplayBuilder.purpleDust(center, 3, 2.0);
                }
            }
            // Rumble warning (tick 100-120)
            else if (ticksAlive >= 100 && ticksAlive < 120 && !slamFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(10, 0, 0), 8, 5.0);
                }
            }
            // Slam on re-emergence (tick 120)
            else if (ticksAlive == 120 && !slamFired) {
                slamFired = true;
                // Massive half-island slam from far side
                for (int x = 2; x <= 20; x += 2) {
                    for (int z = -12; z <= 12; z += 2) {
                        Location loc = center.clone().add(x, 0, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(1.8f, 2.5f, 1.8f).glow(128, 0, 255).interpolation(2, 0);
                        slamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.purpleDust(center.clone().add(10, 0, 0), 60, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeepDive(plugin); }
    }

    // ================================================================
    // 85. SERPENT ROLL — 10 random column strikes over 5 seconds
    // ================================================================
    public static class SerpentRoll extends BossAttack {
        private final List<BlockDisplayHandle> columnHandles = new ArrayList<>();
        private int lastStrikeTick = 0;
        private int strikeCount = 0;

        public SerpentRoll(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("serpent_roll", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(760);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rolling motion indicator at rim
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 / 12) * i;
                Location loc = center.clone().add(Math.cos(a) * 17, 1, Math.sin(a) * 17);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                columnHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rolling rim indicator spins
            if (ticksAlive < 40) {
                float angle = ticksAlive * 0.1f;
                for (int i = 0; i < columnHandles.size() && i < 12; i++) {
                    double a = angle + (Math.PI * 2 / 12) * i;
                    columnHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * 17, 1, Math.sin(a) * 17));
                }
            }

            // Strike every 10 ticks starting at tick 40 (10 strikes over 100 ticks)
            if (ticksAlive >= 40 && strikeCount < 10 && ticksAlive - lastStrikeTick >= 10) {
                lastStrikeTick = ticksAlive;
                strikeCount++;
                // Random strike location within island
                double sa = Math.random() * Math.PI * 2;
                double sr = Math.random() * 12;
                Location strikeLoc = center.clone().add(Math.cos(sa) * sr, 0, Math.sin(sa) * sr);
                // Column impact visual
                for (int y = 0; y <= 5; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        strikeLoc.clone().add(0, y, 0), Material.OBSIDIAN);
                    h.scale(1.2f, 1.0f, 1.2f).glow(128, 0, 255).interpolation(1, 0);
                    columnHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.purpleDust(strikeLoc, 12, 2.0);
                DisplayBuilder.playSound(strikeLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SerpentRoll(plugin); }
    }

    // ================================================================
    // 86. HUNGER LUNGE — Full-island push from one side
    // ================================================================
    public static class HungerLunge extends BossAttack {
        private final List<BlockDisplayHandle> lungeHandles = new ArrayList<>();
        private boolean lungeFired = false;

        public HungerLunge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunger_lunge", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(12.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Particle swirl at left side (X negative) as warning
            for (int y = 0; y <= 8; y++) {
                for (int z = -8; z <= 8; z += 2) {
                    Location loc = center.clone().add(-18, y, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.3f, 0.8f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
                    lungeHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning rumble (0-40 ticks)
            if (ticksAlive < 40 && !lungeFired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-16, 2, 0), 10, 5.0);
                }
            }
            // Lunge impact (tick 40)
            else if (ticksAlive == 40 && !lungeFired) {
                lungeFired = true;
                // Wall of force sweeping across island
                for (BlockDisplayHandle h : lungeHandles) {
                    h.entity().teleport(center.clone().add(h.entity().getLocation().getX() - center.getX() + 4,
                        h.entity().getLocation().getY(), h.entity().getLocation().getZ()));
                }
                DisplayBuilder.purpleDust(center, 40, 18.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
            }
            // Shockwave after lunge
            else if (ticksAlive == 50 && lungeFired) {
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2 / 24) * i;
                    for (int r = 5; r <= 18; r += 3) {
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * r, 1.0, Math.sin(a) * r), 3, 0.8);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HungerLunge(plugin); }
    }

    // ================================================================
    // 87. SPIRAL DESCENT — Sequential cardinal edge impacts in rotation
    // ================================================================
    public static class SpiralDescent extends BossAttack {
        private final List<BlockDisplayHandle> spiralHandles = new ArrayList<>();
        private int edgesHit = 0;
        private final double[][] edgeDirections = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

        public SpiralDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spiral_descent", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(20.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rising spiral particle column around island
            for (int i = 0; i < 24; i++) {
                double a = (Math.PI * 2 / 24) * i;
                double spiralY = (i / 24.0) * 12;
                Location loc = center.clone().add(Math.cos(a) * 16, spiralY, Math.sin(a) * 16);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                spiralHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spiral rotates (0-60 ticks)
            if (ticksAlive < 60) {
                float angle = ticksAlive * 0.08f;
                for (int i = 0; i < spiralHandles.size(); i++) {
                    double a = angle + (Math.PI * 2 / spiralHandles.size()) * i;
                    double spiralY = (i / (double) spiralHandles.size()) * 12;
                    spiralHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * 16, spiralY, Math.sin(a) * 16));
                }
            }

            // Cardinal edge impacts: one every 20 ticks starting at tick 60
            int edgeTick = ticksAlive - 60;
            int nextEdge = edgeTick / 20;
            if (nextEdge > edgesHit && nextEdge <= 4) {
                edgesHit = nextEdge;
                int edgeIdx = (edgesHit - 1) % 4;
                double ex = edgeDirections[edgeIdx][0];
                double ez = edgeDirections[edgeIdx][1];

                // Edge impact wall
                for (int i = -6; i <= 6; i++) {
                    for (int y = 0; y <= 8; y += 2) {
                        // Perpendicular to edge direction
                        double px = ez * i;
                        double pz = ex * i;
                        Location edgeLoc = center.clone().add(ex * 14 + px, y, ez * 14 + pz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(edgeLoc, Material.OBSIDIAN);
                        h.scale(1.5f, 1.5f, 1.5f).glow(128, 0, 255).interpolation(2, 0);
                        spiralHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.purpleDust(center.clone().add(ex * 14, 1, ez * 14), 25, 7.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.5f);

                // Center shockwave pulse
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.purpleDust(center, 5, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpiralDescent(plugin); }
    }

    // ================================================================
    // 88. VOID RAMPAGE — Rapid succession of smaller strike columns
    // ================================================================
    public static class VoidRampage extends BossAttack {
        private final List<BlockDisplayHandle> rampageHandles = new ArrayList<>();
        private int lastRampageTick = 0;
        private int rampageHits = 0;
        private boolean rampageStarted = false;

        public VoidRampage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_rampage", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Roar telegraph visual
            for (int i = 0; i < 20; i++) {
                double a = (Math.PI * 2 / 20) * i;
                Location loc = center.clone().add(Math.cos(a) * (2 + i * 0.5), 1.5, Math.sin(a) * (2 + i * 0.5));
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                rampageHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Roar telegraph (0-40 ticks)
            if (ticksAlive < 40) {
                if (ticksAlive % 5 == 0) {
                    float expandR = 2 + ticksAlive * 0.4f;
                    for (int i = 0; i < rampageHandles.size() && i < 20; i++) {
                        double a = (Math.PI * 2 / 20) * i;
                        rampageHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(a) * expandR, 1.5, Math.sin(a) * expandR));
                    }
                }
                if (ticksAlive == 39) {
                    rampageStarted = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
                }
            }

            // Rampage: strike every 30 ticks for 8 seconds (ticks 40-200)
            if (rampageStarted && ticksAlive >= 40 && rampageHits < 6 && ticksAlive - lastRampageTick >= 30) {
                lastRampageTick = ticksAlive;
                rampageHits++;
                // Random aggressive strike
                double sa = Math.random() * Math.PI * 2;
                double sr = Math.random() * 10;
                Location strikeLoc = center.clone().add(Math.cos(sa) * sr, 0, Math.sin(sa) * sr);
                for (int y = 0; y <= 8; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        strikeLoc.clone().add(0, y, 0), Material.OBSIDIAN);
                    h.scale(1.5f, 1.0f, 1.5f).glow(128, 0, 255).interpolation(1, 0);
                    rampageHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.purpleDust(strikeLoc, 15, 3.5);
                DisplayBuilder.crimsonDust(strikeLoc, 10, 2.0);
                DisplayBuilder.playSound(strikeLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidRampage(plugin); }
    }

    // ================================================================
    // 89. THE SURGE — Void floods island perimeter then crashes inward
    // ================================================================
    public static class TheSurge extends BossAttack {
        private final List<BlockDisplayHandle> floodHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private boolean peakFired = false;
        private boolean waveFired = false;
        private float waveRadius = 18.0f;

        public TheSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_surge", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(18.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Outer 5-block perimeter void flood rising
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2 / 32) * i;
                double r = 14;
                for (int yr = 0; yr <= 4; yr++) {
                    Location loc = center.clone().add(Math.cos(a) * r, -1 + yr, Math.sin(a) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(1.5f, 0.8f, 1.5f).glow(128, 0, 255).interpolation(4, 0);
                    floodHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Void rises (0-60 ticks)
            if (ticksAlive <= 60 && !peakFired) {
                if (ticksAlive % 6 == 0) {
                    for (BlockDisplayHandle h : floodHandles) {
                        h.entity().teleport(h.entity().getLocation().clone().add(0, 0.12, 0));
                    }
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.5, 0), 6, 14.0);
                }
            }
            // Flood peaks (tick 60)
            else if (ticksAlive == 60 && !peakFired) {
                peakFired = true;
                DisplayBuilder.purpleDust(center, 30, 16.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.4f);
            }
            // Recession wave crashes inward (tick 80)
            else if (ticksAlive == 80 && !waveFired) {
                waveFired = true;
                waveRadius = 18.0f;
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2 / 24) * i;
                    Location waveLoc = center.clone().add(Math.cos(a) * waveRadius, 0.8, Math.sin(a) * waveRadius);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(waveLoc, Material.CRYING_OBSIDIAN);
                    h.scale(1.2f, 2.5f, 1.2f).glow(128, 0, 255).interpolation(1, 0);
                    waveHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
            }
            // Wave rushes inward
            else if (waveFired && waveRadius > 0) {
                waveRadius -= 0.5f;
                for (int i = 0; i < waveHandles.size(); i++) {
                    double a = (Math.PI * 2 / waveHandles.size()) * i;
                    waveHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * waveRadius, 0.8, Math.sin(a) * waveRadius));
                }
                if ((int) waveRadius % 3 == 0) {
                    DisplayBuilder.purpleDust(center, 8, waveRadius);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheSurge(plugin); }
    }

    // ================================================================
    // 90. EARTH BREAKER — Massive full breach, permanent void hole visual
    // ================================================================
    public static class EarthBreaker extends BossAttack {
        private final List<BlockDisplayHandle> debrisHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> holeHandles = new ArrayList<>();
        private boolean breachFired = false;

        public EarthBreaker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("earth_breaker", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(28.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Complete silence — no spawn sound, no particles
            // (3-second silence period is the telegraph)
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Silence (0-60 ticks), rumble (60-80)
            if (ticksAlive == 60) {
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.2f);
            }
            if (ticksAlive >= 60 && ticksAlive < 80 && ticksAlive % 5 == 0) {
                DisplayBuilder.purpleDust(center, 5, 3.0);
            }

            // BREACH (tick 80) — massive 20-block-diameter eruption
            if (ticksAlive == 80 && !breachFired) {
                breachFired = true;

                // Permanent void hole ring at center
                for (int i = 0; i < 32; i++) {
                    double a = (Math.PI * 2 / 32) * i;
                    Location edgeLoc = center.clone().add(Math.cos(a) * 10, 0.1, Math.sin(a) * 10);
                    BlockDisplayHandle holeEdge = displayBuilder.spawnBlock(edgeLoc, Material.OBSIDIAN);
                    holeEdge.scale(1.2f, 0.15f, 1.2f).glow(128, 0, 255).interpolation(3, 0);
                    holeHandles.add(holeEdge);
                    spawnedEntities.add(holeEdge.entity());
                }

                // Void pit darkness blocks at center
                for (int x = -8; x <= 8; x += 2) {
                    for (int z = -8; z <= 8; z += 2) {
                        if (x * x + z * z <= 64) {
                            Location pitLoc = center.clone().add(x, -1.5, z);
                            BlockDisplayHandle pit = displayBuilder.spawnBlock(pitLoc, Material.CRYING_OBSIDIAN);
                            pit.scale(1.9f, 0.5f, 1.9f).glow(40, 0, 80).interpolation(4, 0);
                            holeHandles.add(pit);
                            spawnedEntities.add(pit.entity());
                        }
                    }
                }

                // Debris wave expanding outward
                for (int d = 0; d < 24; d++) {
                    double da = (Math.PI * 2 / 24) * d;
                    for (int r = 10; r <= 18; r++) {
                        Location debrisLoc = center.clone().add(Math.cos(da) * r, 0.5, Math.sin(da) * r);
                        BlockDisplayHandle debris = displayBuilder.spawnBlock(debrisLoc, Material.OBSIDIAN);
                        debris.scale(0.7f, 0.7f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
                        debrisHandles.add(debris);
                        spawnedEntities.add(debris.entity());
                    }
                }

                // Breach column shoots upward
                for (int y = 0; y <= 30; y++) {
                    BlockDisplayHandle col = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.OBSIDIAN);
                    col.scale(8.0f - (y * 0.2f), 1.0f, 8.0f - (y * 0.2f)).glow(128, 0, 255).interpolation(2, 0);
                    debrisHandles.add(col);
                    spawnedEntities.add(col.entity());
                }

                DisplayBuilder.purpleDust(center, 80, 12.0);
                DisplayBuilder.crimsonDust(center, 50, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
            }

            // Settle debris falling (80-160 ticks)
            if (breachFired && ticksAlive > 80 && ticksAlive <= 160 && ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : debrisHandles) {
                    Location loc = h.entity().getLocation();
                    if (loc.getY() > center.getY() + 0.5) {
                        h.entity().teleport(loc.clone().add(0, -0.15, 0));
                    }
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.purpleDust(center, 15, 8.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EarthBreaker(plugin); }
    }
}
