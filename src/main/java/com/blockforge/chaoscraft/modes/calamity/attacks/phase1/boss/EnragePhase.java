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
 * Phase 1 Boss Attacks — GROUP 10: ENRAGE / PHASE
 * 10 desperate, devastating, multi-component attacks available only below 25% HP.
 * Massive structures, highest damage values, long durations, intense visuals.
 * Attacks 91-100 from boss1-voidmaw.md.
 * NO status effects — all replaced with damage and spectacular visual escalation.
 */
public final class EnragePhase {

    private EnragePhase() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new AbyssalScream(plugin));
        registry.register(new ThousandTeeth(plugin));
        registry.register(new DimensionalCollapse(plugin));
        registry.register(new HungeringSwarm(plugin));
        registry.register(new TheVoidSpeaks(plugin));
        registry.register(new TideOfTheAbyss(plugin));
        registry.register(new VoidRuptureCascade(plugin));
        registry.register(new TheLastEye(plugin));
        registry.register(new VoidApotheosis(plugin));
        registry.register(new MawConsumesAll(plugin));
    }

    // ================================================================
    // 91. ABYSSAL SCREAM — Full island + 30-block unavoidable pulse
    // ================================================================
    public static class AbyssalScream extends BossAttack {
        private final List<BlockDisplayHandle> mawHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();
        private boolean screamed = false;

        public AbyssalScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_scream", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(24.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Maw opens — massive void opening below island
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2 / 32) * i;
                Location loc = center.clone().add(Math.cos(a) * 12, -2, Math.sin(a) * 12);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(2.0f, 0.5f, 2.0f).glow(128, 0, 255).interpolation(4, 0);
                mawHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Maw opening telegraph (0-60 ticks = 3 seconds)
            if (ticksAlive < 60 && !screamed) {
                float angle = ticksAlive * 0.05f;
                for (int i = 0; i < mawHandles.size(); i++) {
                    double a = angle + (Math.PI * 2 / mawHandles.size()) * i;
                    // Maw expands outward
                    float r = 12 + ticksAlive * 0.15f;
                    mawHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, -2, Math.sin(a) * r));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center, 15, 8.0);
                }
            }
            // SCREAM (tick 60)
            else if (ticksAlive == 60 && !screamed) {
                screamed = true;
                // Sky fractures — crack handles spreading upward
                for (int i = 0; i < 12; i++) {
                    double a = (Math.PI * 2 / 12) * i;
                    for (int y = 0; y <= 20; y += 3) {
                        Location crackLoc = center.clone().add(Math.cos(a) * (y * 0.8), y, Math.sin(a) * (y * 0.8));
                        BlockDisplayHandle h = displayBuilder.spawnBlock(crackLoc, Material.CRYING_OBSIDIAN);
                        h.scale(0.15f, 2.5f, 0.15f).glow(128, 0, 255).interpolation(2, 0);
                        crackHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // Full-island compression ring
                for (int r = 1; r <= 30; r += 2) {
                    for (int i = 0; i < 24; i++) {
                        double a = (Math.PI * 2 / 24) * i;
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * r, 1.5, Math.sin(a) * r), 6, 1.2);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.4f);
            }
            // Sky cracks linger with pulses
            else if (screamed && ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 5, 0), 12, 15.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssalScream(plugin); }
    }

    // ================================================================
    // 92. THOUSAND TEETH — Every surface erupts with void tooth spikes
    // ================================================================
    public static class ThousandTeeth extends BossAttack {
        private final List<BlockDisplayHandle> toothHandles = new ArrayList<>();
        private boolean teethOut = false;

        public ThousandTeeth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thousand_teeth", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Grinding sound telegraph
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.3f);
            // Teeth start below surface
            for (int x = -14; x <= 14; x += 2) {
                for (int z = -14; z <= 14; z += 2) {
                    if (x * x + z * z <= 200) {
                        Location loc = center.clone().add(x, -1.5, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        // Tapered spike shape
                        float taper = 0.3f + (float)(Math.random() * 0.3);
                        h.scale(taper, 2.0f + (float)(Math.random() * 1.5), taper).glow(128, 0, 255).interpolation(4, 0);
                        toothHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Teeth emerge over 50 ticks (2.5 seconds)
            if (ticksAlive <= 50 && !teethOut) {
                float rise = (ticksAlive / 50.0f) * 3.5f;
                for (BlockDisplayHandle h : toothHandles) {
                    Location base = h.entity().getLocation();
                    double targetY = center.getY() - 1.5 + rise;
                    if (base.getY() < targetY) {
                        h.entity().teleport(base.clone().add(0, 0.07, 0));
                    }
                }
                if (ticksAlive == 50) {
                    teethOut = true;
                    DisplayBuilder.purpleDust(center, 40, 14.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.4f);
                }
            }

            // Teeth active: pulsing + tip glow
            if (teethOut) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.purpleDust(center, 10, 12.0);
                }
                // Animate teeth — slight sway
                if (ticksAlive % 20 == 0) {
                    float sway = (float)(Math.sin(ticksAlive * 0.15) * 0.05);
                    for (int i = 0; i < toothHandles.size(); i += 8) {
                        BlockDisplay bd = (BlockDisplay) toothHandles.get(i).entity();
                        float h = 2.0f + (float)(Math.random() * 1.5);
                        bd.setTransformation(new Transformation(
                            new Vector3f(-0.15f, 0, -0.15f),
                            new AxisAngle4f(sway, 0, 0, 1),
                            new Vector3f(0.3f, h, 0.3f),
                            new AxisAngle4f(0, 0, 1, 0)));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(10);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThousandTeeth(plugin); }
    }

    // ================================================================
    // 93. DIMENSIONAL COLLAPSE — 4 simultaneous effects: rifts + gravity
    // ================================================================
    public static class DimensionalCollapse extends BossAttack {
        private final List<BlockDisplayHandle> distortionHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private boolean collapsed = false;
        private int riftCount = 0;

        public DimensionalCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_collapse", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(12.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Visual distortion: sky folds inward — converging blocks
            for (int i = 0; i < 20; i++) {
                double a = (Math.PI * 2 / 20) * i;
                Location loc = center.clone().add(Math.cos(a) * 22, 8, Math.sin(a) * 22);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.5f, 3.0f, 1.5f).glow(128, 0, 255).interpolation(5, 0);
                distortionHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 4-second distortion convergence telegraph (0-80 ticks)
            if (ticksAlive < 80 && !collapsed) {
                float convR = 22 - (ticksAlive / 80.0f) * 20;
                for (int i = 0; i < distortionHandles.size(); i++) {
                    double a = (Math.PI * 2 / distortionHandles.size()) * i;
                    distortionHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * convR, 8 - ticksAlive * 0.04, Math.sin(a) * convR));
                }
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 8, 0), 10, convR);
                }
            }
            // Collapse triggers (tick 80) — 5 void rifts + gravity waves
            else if (ticksAlive == 80 && !collapsed) {
                collapsed = true;
                // Initial 6-heart blast
                DisplayBuilder.purpleDust(center, 50, 12.0);
                DisplayBuilder.crimsonDust(center, 30, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
            }
            // Void rifts open one per 30 ticks (5 total)
            if (collapsed && riftCount < 5 && ticksAlive >= 80 && (ticksAlive - 80) % 30 == 0) {
                riftCount++;
                double ra = Math.random() * Math.PI * 2;
                double rr = Math.random() * 10;
                Location riftLoc = center.clone().add(Math.cos(ra) * rr, 0, Math.sin(ra) * rr);
                // Rift visual: vertical tear
                for (int y = 0; y <= 8; y++) {
                    BlockDisplayHandle rift = displayBuilder.spawnBlock(
                        riftLoc.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                    rift.scale(0.1f, 1.5f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
                    riftHandles.add(rift);
                    spawnedEntities.add(rift.entity());
                }
                DisplayBuilder.purpleDust(riftLoc, 15, 3.0);
                DisplayBuilder.playSound(riftLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }

            // Gravity waves alternating (every 40 ticks while collapsed)
            if (collapsed && (ticksAlive - 80) % 40 == 0 && ticksAlive > 80) {
                boolean highGravity = ((ticksAlive - 80) / 40) % 2 == 0;
                for (int r = 1; r <= 15; r += 2) {
                    for (int i = 0; i < 16; i++) {
                        double a = (Math.PI * 2 / 16) * i;
                        double yOff = highGravity ? 0.3 : 2.0;
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * r, yOff, Math.sin(a) * r), 4, 0.8);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalCollapse(plugin); }
    }

    // ================================================================
    // 94. HUNGERING SWARM — 80% island coverage DoT swarm with shifting safe zone
    // ================================================================
    public static class HungeringSwarm extends BossAttack {
        private final List<BlockDisplayHandle> swarmHandles = new ArrayList<>();
        private double safeAngle = 0;
        private int safeZoneShifts = 0;

        public HungeringSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hungering_swarm", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(12.0);
            config.setDamageRadius(14.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            safeAngle = Math.random() * Math.PI * 2;
            // Dense swarm: fill 80% of island with dark motes (leave safe wedge)
            for (int x = -14; x <= 14; x += 1) {
                for (int z = -14; z <= 14; z += 1) {
                    if (x * x + z * z > 200) continue;
                    // Check if in safe wedge (30-degree arc from safeAngle)
                    double pointAngle = Math.atan2(z, x);
                    double angleDiff = Math.abs(pointAngle - safeAngle);
                    if (angleDiff > Math.PI) angleDiff = Math.PI * 2 - angleDiff;
                    if (angleDiff < 0.26) continue; // ~15 degree safe wedge per side = 30 total

                    if (Math.random() < 0.4) {
                        int yy = (int)(Math.random() * 3);
                        Location loc = center.clone().add(x, yy, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        float s = 0.2f + (float)(Math.random() * 0.25);
                        h.scale(s, s, s).glow(80, 0, 160).interpolation(3, 0);
                        swarmHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 2.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Swarm flows toward player positions — individual drift
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle h : swarmHandles) {
                    Location loc = h.entity().getLocation();
                    double dx = center.getX() - loc.getX();
                    double dz = center.getZ() - loc.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0 && dist < 15) {
                        double nx = dx / dist * 0.08;
                        double nz = dz / dist * 0.08;
                        double jitter = (Math.random() - 0.5) * 0.1;
                        h.entity().teleport(loc.clone().add(nx + jitter, 0, nz + jitter));
                    }
                }
            }

            // Safe zone shifts every 100 ticks (5 seconds)
            if (ticksAlive % 100 == 0 && ticksAlive > 0 && safeZoneShifts < 3) {
                safeZoneShifts++;
                safeAngle = Math.random() * Math.PI * 2;
                // Flash announce new safe direction
                Location safeDir = center.clone().add(
                    Math.cos(safeAngle) * 8, 1.5, Math.sin(safeAngle) * 8);
                DisplayBuilder.cyanDust(safeDir, 25, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
            }

            // Swarm density pulse
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 10, 10.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HungeringSwarm(plugin); }
    }

    // ================================================================
    // 95. THE VOID SPEAKS — 5-second countdown then full-island damage
    // ================================================================
    public static class TheVoidSpeaks extends BossAttack {
        private final List<BlockDisplayHandle> countdownHandles = new ArrayList<>();
        private int lastCountdownNum = 6;
        private boolean impactFired = false;

        public TheVoidSpeaks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_void_speaks", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(40.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Everything stops — eerie silence begins
            // Numbers will appear above island as large block display clusters
            // Nothing spawned yet; countdown starts at tick 0
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Countdown: 5...4...3...2...1...0 — one number per 20 ticks
            int countdownNum = 5 - (ticksAlive / 20);
            if (countdownNum >= 0 && countdownNum < lastCountdownNum) {
                lastCountdownNum = countdownNum;

                // Remove previous number display
                for (BlockDisplayHandle h : countdownHandles) h.entity().remove();
                countdownHandles.clear();

                // Display current number as large block cluster above center
                int num = countdownNum;
                spawnNumberDisplay(center.clone().add(0, 15, 0), num);

                // Sound per count
                float pitch = 0.5f + ((5 - num) * 0.1f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, pitch);
                if (num == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
                }
            }

            // IMPACT at tick 100 (count reaches 0)
            if (ticksAlive == 100 && !impactFired) {
                impactFired = true;
                for (BlockDisplayHandle h : countdownHandles) h.entity().remove();
                countdownHandles.clear();

                // Full island slam — concentric rings expanding out from every point
                for (int ring = 1; ring <= 20; ring += 2) {
                    for (int i = 0; i < 24; i++) {
                        double a = (Math.PI * 2 / 24) * i;
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * ring, 0.5, Math.sin(a) * ring), 8, 1.5);
                    }
                }
                // Massive column rising
                for (int y = 0; y <= 20; y++) {
                    BlockDisplayHandle col = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.OBSIDIAN);
                    col.scale(10.0f - (y * 0.4f), 1.0f, 10.0f - (y * 0.4f)).glow(128, 0, 255).interpolation(2, 0);
                    countdownHandles.add(col);
                    spawnedEntities.add(col.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.4f);
            }
        }

        private void spawnNumberDisplay(Location loc, int number) {
            // Simple block-pattern number display (5x3 block grid for each digit 0-5)
            // Using a simple encoding: bit pattern rows for digits
            boolean[][] patterns = {
                {true, true, true, true, false, true, true, false, true, true, false, true, true, true, true}, // 5
                {true, true, true, true, false, true, true, false, true, true, false, true, true, false, true}, // 4
                {true, true, true, false, false, true, true, true, true, true, false, false, true, true, true}, // 3
                {true, false, true, true, false, true, true, true, true, false, false, true, false, false, true}, // 2
                {false, true, false, false, true, false, false, true, false, false, true, false, false, true, false}, // 1
                {true, true, true, true, false, true, true, false, true, true, false, true, true, true, true}  // 0
            };
            if (number < 0 || number >= patterns.length) return;
            boolean[] pattern = patterns[5 - number]; // index 0=5, 5=0
            for (int i = 0; i < pattern.length; i++) {
                if (pattern[i]) {
                    int col = i % 3;
                    int row = i / 3;
                    Location blockLoc = loc.clone().add(col - 1, -(row), 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(blockLoc, Material.OBSIDIAN);
                    h.scale(0.8f, 0.8f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                    countdownHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheVoidSpeaks(plugin); }
    }

    // ================================================================
    // 96. TIDE OF THE ABYSS — Rising void flood in 3 phases
    // ================================================================
    public static class TideOfTheAbyss extends BossAttack {
        private final List<BlockDisplayHandle> tideHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> floodHandles = new ArrayList<>();
        private boolean breached = false;
        private boolean flooded = false;

        public TideOfTheAbyss(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tide_of_the_abyss", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(16.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Phase 1: visible void rising below island — 5-second terror
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2 / 32) * i;
                Location loc = center.clone().add(Math.cos(a) * 14, -6, Math.sin(a) * 14);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(2.0f, 1.0f, 2.0f).glow(128, 0, 255).interpolation(5, 0);
                tideHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 2.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Phase 1 (0-100): void rises toward island underside
            if (ticksAlive <= 100 && !breached) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle h : tideHandles) {
                        h.entity().teleport(h.entity().getLocation().clone().add(0, 0.12, 0));
                    }
                    if (ticksAlive % 20 == 0) {
                        DisplayBuilder.purpleDust(center.clone().add(0, -4, 0), 10, 14.0);
                    }
                }
            }
            // Phase 2 (tick 100): void breaches — rivers flow across island surface (60 ticks)
            else if (ticksAlive == 100 && !breached) {
                breached = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
                // River streams across 70% of island
                for (int x = -12; x <= 12; x += 3) {
                    for (int z = -12; z <= 12; z++) {
                        if (x * x + z * z <= 175 && Math.random() > 0.3) {
                            Location riverLoc = center.clone().add(x, 0.15, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(riverLoc, Material.OBSIDIAN);
                            h.scale(1.5f, 0.3f, 1.0f).glow(80, 0, 160).interpolation(4, 0);
                            floodHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
            }
            // Phase 3 (tick 160): full flood — entire island 1 block deep (160 ticks duration)
            else if (ticksAlive == 160 && !flooded) {
                flooded = true;
                // Replace rivers with solid flood layer
                for (BlockDisplayHandle h : floodHandles) h.entity().remove();
                floodHandles.clear();
                for (int x = -14; x <= 14; x++) {
                    for (int z = -14; z <= 14; z++) {
                        if (x * x + z * z <= 200) {
                            Location floodLoc = center.clone().add(x, 0.1, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(floodLoc, Material.CRYING_OBSIDIAN);
                            h.scale(0.98f, 0.6f, 0.98f).glow(128, 0, 255).interpolation(3, 0);
                            floodHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
                DisplayBuilder.purpleDust(center, 50, 16.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.3f);
            }

            // Flood particles
            if (flooded && ticksAlive % 15 == 0) {
                DisplayBuilder.purpleDust(center, 8, 12.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TideOfTheAbyss(plugin); }
    }

    // ================================================================
    // 97. VOID RUPTURE CASCADE — 10 chains of 16-direction explosions
    // ================================================================
    public static class VoidRuptureCascade extends BossAttack {
        private final List<BlockDisplayHandle> cascadeHandles = new ArrayList<>();
        private int lastCascadeTick = 0;
        private int cascadeWave = 0;
        private boolean started = false;

        public VoidRuptureCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_rupture_cascade", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Center flash telegraph (1.5 seconds)
            BlockDisplayHandle centerFlash = displayBuilder.spawnBlock(
                center.clone().add(0, 1, 0), Material.OBSIDIAN);
            centerFlash.scale(2.0f, 2.0f, 2.0f).glow(128, 0, 255).interpolation(2, 0);
            cascadeHandles.add(centerFlash);
            spawnedEntities.add(centerFlash.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph flashes (0-30 ticks)
            if (ticksAlive < 30) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 1, 0), 10, 2.0);
                }
            }
            else if (!started) {
                started = true;
                lastCascadeTick = ticksAlive;
                // Remove center flash
                for (BlockDisplayHandle h : cascadeHandles) h.entity().remove();
                cascadeHandles.clear();
            }

            // Fire one cascade wave every 10 ticks (10 waves)
            if (started && cascadeWave < 10 && ticksAlive - lastCascadeTick >= 10) {
                lastCascadeTick = ticksAlive;
                cascadeWave++;
                float waveR = cascadeWave * 2.0f;

                // 16 directions simultaneously
                for (int dir = 0; dir < 16; dir++) {
                    double a = (Math.PI * 2 / 16) * dir;
                    Location ruptureLoc = center.clone().add(
                        Math.cos(a) * waveR, 0.8, Math.sin(a) * waveR);
                    // Rupture burst visual
                    for (int rb = 0; rb < 8; rb++) {
                        double ra2 = (Math.PI * 2 / 8) * rb;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            ruptureLoc.clone().add(Math.cos(ra2) * 1.5, 0, Math.sin(ra2) * 1.5),
                            Material.OBSIDIAN);
                        h.scale(0.5f, 0.8f, 0.5f).glow(128, 0, 255).interpolation(1, 0);
                        cascadeHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    DisplayBuilder.purpleDust(ruptureLoc, 8, 1.5);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f + cascadeWave * 0.05f);
                // Flash at center
                DisplayBuilder.purpleDust(center, 5, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidRuptureCascade(plugin); }
    }

    // ================================================================
    // 98. THE LAST EYE — 15-block void eye opens and fires massive beam
    // ================================================================
    public static class TheLastEye extends BossAttack {
        private final List<BlockDisplayHandle> eyeHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamFired = false;

        public TheLastEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_last_eye", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(24.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eye opens slowly in island surface — iris ring
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2 / 32) * i;
                Location loc = center.clone().add(Math.cos(a) * 7.5, 0.05, Math.sin(a) * 7.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.5f, 0.15f, 1.5f).glow(128, 0, 255).interpolation(4, 0);
                eyeHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Pupil (inner dark circle)
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (x * x + z * z <= 9) {
                        BlockDisplayHandle pupil = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.08, z), Material.CRYING_OBSIDIAN);
                        pupil.scale(0.95f, 0.1f, 0.95f).glow(200, 0, 50).interpolation(4, 0);
                        eyeHandles.add(pupil);
                        spawnedEntities.add(pupil.entity());
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye opening — pupil dilates (0-100 ticks = 5 seconds)
            if (ticksAlive < 100 && !beamFired) {
                float dilation = ticksAlive / 100.0f;
                // Iris contracts (outer ring shrinks toward edge of 7.5-block radius)
                float irisR = 7.5f * (1.0f - dilation * 0.3f);
                for (int i = 0; i < Math.min(32, eyeHandles.size()); i++) {
                    double a = (Math.PI * 2 / 32) * i;
                    eyeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * irisR, 0.05, Math.sin(a) * irisR));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center, 8, 3.0 + dilation * 5.0);
                }
                // Final dilation at tick 95
                if (ticksAlive == 95) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
                }
            }
            // BEAM FIRES (tick 100)
            else if (ticksAlive == 100 && !beamFired) {
                beamFired = true;
                // Massive upward beam — 10-block diameter, 60 blocks tall
                for (int y = 1; y <= 60; y++) {
                    for (int bx = -4; bx <= 4; bx += 2) {
                        for (int bz = -4; bz <= 4; bz += 2) {
                            if (bx * bx + bz * bz <= 16) {
                                Location beamLoc = center.clone().add(bx, y, bz);
                                BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    beamLoc, Material.OBSIDIAN);
                                h.scale(1.8f, 1.0f, 1.8f).glow(128, 0, 255).interpolation(1, 0);
                                beamHandles.add(h);
                                spawnedEntities.add(h.entity());
                            }
                        }
                    }
                }
                DisplayBuilder.purpleDust(center.clone().add(0, 30, 0), 50, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.3f);
            }

            // Beam active: pulsing (100-200 ticks = 5 seconds)
            if (beamFired && ticksAlive < 200 && ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 15, 0), 15, 5.0);
                DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 10, 3.0);
            }
            // Beam ends at tick 200 — remove beam blocks
            if (beamFired && ticksAlive == 200) {
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();
                // Residual damage flash
                DisplayBuilder.purpleDust(center, 20, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastEye(plugin); }
    }

    // ================================================================
    // 99. VOID APOTHEOSIS — Fight-state transformation, massive visual event
    //     (Design doc: permanent damage multiplier + no direct damage
    //      → implemented as sustained high-DPS aura + massive structure spawn)
    // ================================================================
    public static class VoidApotheosis extends BossAttack {
        private final List<BlockDisplayHandle> apotheosisHandles = new ArrayList<>();
        private boolean transformed = false;

        public VoidApotheosis(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_apotheosis", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(0); // Single use at ~15% HP
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 8-second transformation telegraph — ALL attacks suspended visually
            // Voidmaw grows: massive expanding dark mass rising below island
            for (int i = 0; i < 40; i++) {
                double a = (Math.PI * 2 / 40) * i;
                double r = 10 + (i % 5) * 2.0;
                for (int y = -5; y <= 0; y += 2) {
                    Location loc = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(2.5f, 2.5f, 2.5f).glow(40, 0, 80).interpolation(6, 0);
                    apotheosisHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // No sound on spawn — ominous silence
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Transformation grows over 160 ticks (8 seconds)
            if (ticksAlive <= 160 && !transformed) {
                float growth = ticksAlive / 160.0f;
                float angle = ticksAlive * 0.03f;
                for (int i = 0; i < apotheosisHandles.size(); i++) {
                    double a = angle + (Math.PI * 2 / 40) * (i / 3);
                    double r = (10 + (i % 5) * 2.0) * (1.0 + growth);
                    int y = -5 + ((i % 3) * 2);
                    apotheosisHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r));
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.purpleDust(center, (int)(10 * growth + 5), 12.0 * growth + 5);
                }
                if (ticksAlive == 80) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.3f);
                }
            }
            // Transformation complete (tick 160)
            else if (ticksAlive == 160 && !transformed) {
                transformed = true;
                // Pillar structures rise around island — three reactivated zone effects
                for (int i = 0; i < 12; i++) {
                    double a = (Math.PI * 2 / 12) * i;
                    for (int y = 0; y <= 15; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(a) * 18, y, Math.sin(a) * 18),
                            Material.OBSIDIAN);
                        h.scale(1.5f, 1.0f, 1.5f).glow(128, 0, 255).interpolation(2, 0);
                        apotheosisHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.purpleDust(center, 80, 20.0);
                DisplayBuilder.crimsonDust(center, 50, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
            }
            // Sustained aura post-transformation
            if (transformed && ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 12, 18.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidApotheosis(plugin); }
    }

    // ================================================================
    // 100. THE MAW CONSUMES ALL — Final attack, coil + swallow + expulsion
    // ================================================================
    public static class MawConsumesAll extends BossAttack {
        private final List<BlockDisplayHandle> coilHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> swallowHandles = new ArrayList<>();
        private boolean coilingComplete = false;
        private boolean swallowStarted = false;
        private boolean expulsed = false;
        private float islandSinkY = 0;

        public MawConsumesAll(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("maw_consumes_all", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(10.0);
            config.setDamageRadius(22.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(0); // Once at 10% HP
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Phase 1: massive coils rising around island perimeter (6 seconds = 120 ticks)
            for (int i = 0; i < 40; i++) {
                double a = (Math.PI * 2 / 40) * i;
                double spiralY = (i / 40.0) * 20;
                double r = 16 + Math.sin(i * 0.5) * 2;
                Location loc = center.clone().add(Math.cos(a) * r, -spiralY, Math.sin(a) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(3.0f, 3.0f, 3.0f).glow(128, 0, 255).interpolation(5, 0);
                coilHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Phase 1: COILING (0-120 ticks) — coils rise up around island
            if (ticksAlive <= 120 && !coilingComplete) {
                float riseProgress = ticksAlive / 120.0f;
                for (int i = 0; i < coilHandles.size(); i++) {
                    double a = (Math.PI * 2 / 40) * i + (riseProgress * Math.PI);
                    double r = 16 + Math.sin(i * 0.5) * 2;
                    double wallY = riseProgress * 20 - (i / 40.0) * 20;
                    coilHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, wallY, Math.sin(a) * r));
                }
                // Proximity damage from coil flesh
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.purpleDust(center, 10, 16.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
                }
                if (ticksAlive == 120) {
                    coilingComplete = true;
                    // Instant knockback visual — edge wall complete
                    DisplayBuilder.purpleDust(center, 40, 16.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
                }
            }

            // Phase 2: THE SWALLOW (120-320 ticks = 10 seconds) — island sinks
            if (ticksAlive >= 120 && ticksAlive <= 320 && !expulsed) {
                if (!swallowStarted) {
                    swallowStarted = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.3f);
                }
                // Spawn sinking island visual blocks
                if (ticksAlive == 121) {
                    for (int x = -14; x <= 14; x += 2) {
                        for (int z = -14; z <= 14; z += 2) {
                            if (x * x + z * z <= 200) {
                                BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    center.clone().add(x, 0, z), Material.OBSIDIAN);
                                h.scale(1.8f, 0.8f, 1.8f).glow(80, 0, 160).interpolation(3, 0);
                                swallowHandles.add(h);
                                spawnedEntities.add(h.entity());
                            }
                        }
                    }
                }
                // Island sinks at 2 blocks per second (0.1 per tick)
                if (ticksAlive > 121 && ticksAlive % 5 == 0) {
                    islandSinkY -= 0.5f;
                    for (BlockDisplayHandle h : swallowHandles) {
                        Location loc = h.entity().getLocation();
                        h.entity().teleport(loc.clone().add(0, -0.1, 0));
                    }
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, islandSinkY, 0), 15, 14.0);
                }
            }

            // Phase 3: EXPULSION (tick 320) — island blasted back up
            if (ticksAlive == 320 && !expulsed) {
                expulsed = true;
                for (BlockDisplayHandle h : swallowHandles) h.entity().remove();
                swallowHandles.clear();
                // Massive explosion + 15-heart expulsion impact
                for (int r = 1; r <= 20; r += 2) {
                    for (int i = 0; i < 24; i++) {
                        double a = (Math.PI * 2 / 24) * i;
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * r, 1, Math.sin(a) * r), 8, 1.8);
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.cos(a) * r, 3, Math.sin(a) * r), 5, 1.5);
                    }
                }
                // Coils retreating
                for (BlockDisplayHandle h : coilHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(loc.clone().add(0, -5, 0));
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
            }

            // Post-expulsion recovery glow (320+)
            if (expulsed && ticksAlive % 30 == 0 && ticksAlive < 500) {
                DisplayBuilder.cyanDust(center, 8, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MawConsumesAll(plugin); }
    }
}
