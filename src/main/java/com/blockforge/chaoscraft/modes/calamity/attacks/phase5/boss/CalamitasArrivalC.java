package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

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
 * Supreme Calamitas — Phase 4E Boss (Boss 5, Final Boss)
 * Phase 1: "The Arrival" — HP 100% to 75%
 * Attacks #21-30
 *
 * Trap-layering begins. Mines, splitting skulls, spirals, sigils.
 * The arena becomes a minefield of overlapping threats.
 * NO status effects — damage only.
 */
public final class CalamitasArrivalC {

    private CalamitasArrivalC() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new Patience(plugin));
        registry.register(new TwinSeekers(plugin));
        registry.register(new BrimstoneCurtain(plugin));
        registry.register(new TheCross(plugin));
        registry.register(new AltitudeLock(plugin));
        registry.register(new SplittingSkull(plugin));
        registry.register(new TheSpiral(plugin));
        registry.register(new FocusedBurn(plugin));
        registry.register(new Scattershot(plugin));
        registry.register(new Sigil(plugin));
    }

    // ================================================================
    // 21. PATIENCE — Delayed ground mines, 8-second dormancy
    // ================================================================
    public static class Patience extends BossAttack {
        private final List<BlockDisplayHandle> mineHandles = new ArrayList<>();
        private boolean minesPlaced = false;
        private int placeTick = 0;

        public Patience(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_patience", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 lava circles appear on ground
            double[][] positions = {{2, 0, 6}, {-3, 0, 4}, {0, 0, 10}};
            for (double[] pos : positions) {
                Location mineLoc = center.clone().add(pos[0], 0.05, pos[2]);
                BlockDisplayHandle mine = displayBuilder.spawnBlock(mineLoc, Material.MAGMA_BLOCK);
                mine.scale(1.5f, 0.06f, 1.5f).glow(255, 80, 0).interpolation(3, 0);
                mineHandles.add(mine);
                spawnedEntities.add(mine.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph pulse (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !minesPlaced) {
                float pulse = 1.5f + (float) Math.sin(ticksAlive * 0.3) * 0.3f;
                for (int i = 0; i < 3 && i < mineHandles.size(); i++) {
                    mineHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-pulse / 2, -0.03f, -pulse / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, 0.06f, pulse),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
            }
            // Mines go dormant (tick 40) — faint glow
            else if (ticksAlive == 40 && !minesPlaced) {
                minesPlaced = true;
                placeTick = ticksAlive;
                for (int i = 0; i < 3 && i < mineHandles.size(); i++) {
                    mineHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.4f, -0.03f, -0.4f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.8f, 0.06f, 0.8f),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
            }
            // Dormant phase (40-200 ticks = 8 seconds)
            else if (minesPlaced && ticksAlive - placeTick < 160) {
                if (ticksAlive % 20 == 0) {
                    double[][] positions = {{2, 0, 6}, {-3, 0, 4}, {0, 0, 10}};
                    for (double[] pos : positions) {
                        DisplayBuilder.crimsonDust(center.clone().add(pos[0], 0.1, pos[2]), 3, 0.5);
                    }
                }
            }
            // Auto-detonation (tick 200)
            else if (minesPlaced && ticksAlive - placeTick == 160) {
                double[][] positions = {{2, 0, 6}, {-3, 0, 4}, {0, 0, 10}};
                for (double[] pos : positions) {
                    Location detLoc = center.clone().add(pos[0], 0.1, pos[2]);
                    // Explosion visuals
                    BlockDisplayHandle blast = displayBuilder.spawnBlock(detLoc, Material.ORANGE_STAINED_GLASS);
                    blast.scale(3.0f, 3.0f, 3.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(blast.entity());
                    DisplayBuilder.crimsonDust(detLoc, 25, 3.0);
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 7));
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Patience(plugin); }
    }

    // ================================================================
    // 22. TWIN SEEKERS — Dual seeking tridents targeting two players
    // ================================================================
    public static class TwinSeekers extends BossAttack {
        private final List<BlockDisplayHandle> seekerHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public TwinSeekers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_twin_seekers", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dual hand spirals + halos on two targets
            for (int i = 0; i < 2; i++) {
                double xOff = (i == 0) ? -1.0 : 1.0;
                BlockDisplayHandle hand = displayBuilder.spawnBlock(
                    center.clone().add(xOff, 20, 0), Material.IRON_BLOCK);
                hand.scale(0.25f, 0.25f, 0.25f).glow(200, 200, 255).interpolation(3, 0);
                seekerHandles.add(hand);
                spawnedEntities.add(hand.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fired) {
                double spiral = ticksAlive * 0.4;
                for (int i = 0; i < 2 && i < seekerHandles.size(); i++) {
                    double xOff = (i == 0) ? -1.0 : 1.0;
                    float radius = 0.6f - (ticksAlive / 40.0f) * 0.3f;
                    seekerHandles.get(i).entity().teleport(
                        center.clone().add(xOff + Math.cos(spiral + Math.PI * i) * radius,
                            20 + Math.sin(spiral) * radius, 0));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 6, 1.0);
                }
            }
            // Fire dual seekers (tick 40)
            else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 2; i++) {
                    double xOff = (i == 0) ? -2.0 : 2.0;
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                    seekerHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.1f);
            }
            // Seekers track with corrections (40-140 ticks = 5 seconds)
            else if (fired && ticksAlive - fireTick < 100) {
                int seekTick = ticksAlive - fireTick;
                for (int i = 0; i < 2; i++) {
                    int idx = 2 + i;
                    if (idx < seekerHandles.size()) {
                        double xOff = (i == 0) ? -2.0 : 2.0;
                        float dist = seekTick * 1.6f;
                        double seekDrift = Math.sin(seekTick * 0.15) * 3.0;
                        seekerHandles.get(idx).entity().teleport(
                            center.clone().add(xOff + seekDrift, 20 - dist * 0.1, dist));
                    }
                }
                if (seekTick % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_METAL_HIT, 0.3f, 1.7f);
                }
                if (seekTick % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 18, seekTick * 1.6f), 5, 1.0);
                }
            }
            // Impact (tick 140)
            else if (fired && ticksAlive - fireTick == 100) {
                triggerImpactDamage(center.clone().add(0, 0.5, 100));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 100), 15, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TwinSeekers(plugin); }
    }

    // ================================================================
    // 23. BRIMSTONE CURTAIN — 6 skulls in vertical curtain wall
    // ================================================================
    public static class BrimstoneCurtain extends BossAttack {
        private final List<BlockDisplayHandle> curtainHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public BrimstoneCurtain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_curtain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Horizontal line of soul fire at Y+20 spanning arena width
            BlockDisplayHandle line = displayBuilder.spawnBlock(
                center.clone().add(0, 20, 0), Material.ORANGE_STAINED_GLASS);
            line.scale(20.0f, 0.1f, 0.3f).glow(255, 80, 0).interpolation(3, 0);
            curtainHandles.add(line);
            spawnedEntities.add(line.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !fired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 12, 10.0);
                }
            }
            // Fire 6 skulls in curtain (tick 36, staggered 6 ticks apart)
            else if (ticksAlive == 36 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 6; i++) {
                    double y = 20 - i * 2; // Y+20, 18, 16, 14, 12, 10
                    Location spawnLoc = center.clone().add(0, y, 0);
                    BlockDisplayHandle skull = displayBuilder.spawnBlock(spawnLoc, Material.SOUL_SAND);
                    skull.scale(0.6f, 0.6f, 0.6f).glow(180, 80, 0).interpolation(1, 0);
                    curtainHandles.add(skull);
                    spawnedEntities.add(skull.entity());
                }
            }
            // Skulls advance perpendicular (staggered launch, same direction)
            else if (fired && ticksAlive - fireTick < 80) {
                int curtainTick = ticksAlive - fireTick;
                for (int i = 0; i < 6; i++) {
                    int idx = 1 + i;
                    int delay = i * 6;
                    if (curtainTick > delay && idx < curtainHandles.size()) {
                        float dist = (curtainTick - delay) * 1.0f;
                        double y = 20 - i * 2;
                        curtainHandles.get(idx).entity().teleport(
                            center.clone().add(0, y, dist));
                    }
                }
                if (curtainTick % 8 == 0) {
                    float pitch = 0.6f + curtainTick * 0.003f;
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, pitch);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 15, curtainTick * 0.8f), 8, 3.0);
                }
            }
            // Impact on far wall (tick 116)
            else if (fired && ticksAlive - fireTick == 80) {
                triggerImpactDamage(center.clone().add(0, 10, 60));
                DisplayBuilder.crimsonDust(center.clone().add(0, 10, 60), 20, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCurtain(plugin); }
    }

    // ================================================================
    // 24. THE CROSS — 4 tridents in cardinal directions
    // ================================================================
    public static class TheCross extends BossAttack {
        private final List<BlockDisplayHandle> crossHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public TheCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_cross", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 4 spark lines extending N/S/E/W
            int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] dir : dirs) {
                Location linePt = center.clone().add(dir[0] * 5, 22, dir[1] * 5);
                BlockDisplayHandle line = displayBuilder.spawnBlock(linePt, Material.IRON_BLOCK);
                line.scale(0.1f, 0.1f, 5.0f).glow(200, 200, 255).interpolation(3, 0);
                crossHandles.add(line);
                spawnedEntities.add(line.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 8 == 0) {
                    int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                    for (int[] dir : dirs) {
                        DisplayBuilder.crimsonDust(
                            center.clone().add(dir[0] * 8, 22, dir[1] * 8), 5, 1.0);
                    }
                }
            }
            // Fire 4 tridents (tick 40)
            else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                for (int[] dir : dirs) {
                    Location spawnLoc = center.clone().add(dir[0] * 2, 22, dir[1] * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    crossHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 1.0f);
            }
            // Tridents fly in cardinal directions (40-80 ticks)
            else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 1.8f;
                int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                for (int i = 0; i < 4; i++) {
                    int idx = 4 + i;
                    if (idx < crossHandles.size()) {
                        crossHandles.get(idx).entity().teleport(
                            center.clone().add(dirs[i][0] * (2 + dist), 22, dirs[i][1] * (2 + dist)));
                    }
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 6, dist * 0.3);
                }
            }
            // Embed in walls (tick 80)
            else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 22, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 10, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheCross(plugin); }
    }

    // ================================================================
    // 25. ALTITUDE LOCK — Proximity lock at Y+15, 5 seconds
    // ================================================================
    public static class AltitudeLock extends BossAttack {
        private final List<BlockDisplayHandle> lockHandles = new ArrayList<>();
        private boolean lockActive = false;
        private int lockStartTick = 0;

        public AltitudeLock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_altitude_lock", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Totem particles cascade downward from Y+15
            BlockDisplayHandle column = displayBuilder.spawnBlock(
                center.clone().add(0, 15, 0), Material.YELLOW_STAINED_GLASS);
            column.scale(6.0f, 0.2f, 6.0f).glow(255, 220, 100).interpolation(3, 0);
            lockHandles.add(column);
            spawnedEntities.add(column.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !lockActive) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 12, 0), 8, 3.0);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 18, 0), 8, 3.0);
                }
            }
            // Lock active (tick 40-140 = 5 seconds)
            else if (ticksAlive == 40 && !lockActive) {
                lockActive = true;
                lockStartTick = ticksAlive;
                // Danger zone visual rings at Y+12 and Y+18
                for (int ring = 0; ring < 2; ring++) {
                    double y = (ring == 0) ? 12 : 18;
                    for (int seg = 0; seg < 8; seg++) {
                        double angle = (Math.PI * 2 / 8) * seg;
                        Location ringPt = center.clone().add(Math.cos(angle) * 4, y, Math.sin(angle) * 4);
                        BlockDisplayHandle ringBlock = displayBuilder.spawnBlock(ringPt, Material.ORANGE_STAINED_GLASS);
                        ringBlock.scale(0.8f, 0.1f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
                        lockHandles.add(ringBlock);
                        spawnedEntities.add(ringBlock.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.6f);
            }
            // Active lock damage (40-140 ticks)
            else if (lockActive && ticksAlive - lockStartTick < 100) {
                if ((ticksAlive - lockStartTick) % 20 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 15, 0), 10, 4.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.2f);
                }
            }
            // Lock ends (tick 140)
            else if (lockActive && ticksAlive - lockStartTick == 100) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AltitudeLock(plugin); }
    }

    // ================================================================
    // 26. SPLITTING SKULL — Oversized skull splits into 3 fragments
    // ================================================================
    public static class SplittingSkull extends BossAttack {
        private final List<BlockDisplayHandle> splitHandles = new ArrayList<>();
        private boolean launched = false;
        private boolean split = false;
        private int launchTick = 0;

        public SplittingSkull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_splitting_skull", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Large skull 1.5x scale at hand
            BlockDisplayHandle bigSkull = displayBuilder.spawnBlock(
                center.clone().add(0.5, 20, 0), Material.SOUL_SAND);
            bigSkull.scale(1.0f, 1.0f, 1.0f).glow(180, 80, 0).interpolation(3, 0);
            splitHandles.add(bigSkull);
            spawnedEntities.add(bigSkull.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — skull pulses (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !launched) {
                float pulse = 1.0f + (float) Math.sin(ticksAlive * 0.5) * 0.25f;
                if (!splitHandles.isEmpty()) {
                    splitHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-pulse / 2, 20 - pulse / 2, -pulse / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, pulse, pulse),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0.5, 20, 0), 8, 1.0);
                }
            }
            // Launch (tick 40)
            else if (ticksAlive == 40 && !launched) {
                launched = true;
                launchTick = ticksAlive;
            }
            // Skull travels slowly (40-90 ticks = 2.5 seconds at vel 0.7)
            else if (launched && !split && ticksAlive - launchTick < 50) {
                float dist = (ticksAlive - launchTick) * 0.7f;
                if (!splitHandles.isEmpty()) {
                    splitHandles.get(0).entity().teleport(
                        center.clone().add(0.5, 20 - dist * 0.02, dist));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0.5, 20 - dist * 0.02, dist), 6, 0.8);
                }
            }
            // Split into 3 (tick 90)
            else if (launched && !split && ticksAlive - launchTick == 50) {
                split = true;
                float splitZ = 50 * 0.7f;
                Location splitLoc = center.clone().add(0.5, 19, splitZ);
                // 3 fragments: straight, 45 left, 45 right
                for (int i = -1; i <= 1; i++) {
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(splitLoc, Material.SOUL_SAND);
                    frag.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0).interpolation(1, 0);
                    splitHandles.add(frag);
                    spawnedEntities.add(frag.entity());
                }
                DisplayBuilder.crimsonDust(splitLoc, 15, 2.0);
                DisplayBuilder.playSound(splitLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);
                DisplayBuilder.playSound(splitLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.0f);
            }
            // Fragments travel (90-130 ticks = 2 seconds)
            else if (split && ticksAlive - launchTick >= 50 && ticksAlive - launchTick < 90) {
                int fragTick = ticksAlive - launchTick - 50;
                float fragDist = fragTick * 1.4f;
                float splitZ = 50 * 0.7f;
                for (int i = -1; i <= 1; i++) {
                    int idx = 1 + (i + 1);
                    if (idx < splitHandles.size()) {
                        double angle = Math.toRadians(45 * i);
                        splitHandles.get(idx).entity().teleport(
                            center.clone().add(0.5 + Math.sin(angle) * fragDist,
                                19 - fragDist * 0.05, splitZ + Math.cos(angle) * fragDist));
                    }
                }
                if (fragTick % 6 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0.5, 18, splitZ + fragDist), 6, 2.0);
                }
            }
            // Fragment impacts (tick 130)
            else if (split && ticksAlive - launchTick == 90) {
                triggerImpactDamage(center.clone().add(0, 0.5, 60));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 60), 12, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SplittingSkull(plugin); }
    }

    // ================================================================
    // 27. THE SPIRAL — 6 tridents in clockwise expanding spiral
    // ================================================================
    public static class TheSpiral extends BossAttack {
        private final List<BlockDisplayHandle> spiralHandles = new ArrayList<>();
        private boolean spiralActive = false;
        private int spiralStartTick = 0;

        public TheSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_spiral", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // She rises to Y+25, swirl particles tightening
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 25, 0), Material.RED_STAINED_GLASS);
            core.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 0).interpolation(3, 0);
            spiralHandles.add(core);
            spawnedEntities.add(core.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !spiralActive) {
                if (ticksAlive % 6 == 0) {
                    double angle = ticksAlive * 0.3;
                    float radius = 3.0f - (ticksAlive / 40.0f) * 2.0f;
                    DisplayBuilder.crimsonDust(
                        center.clone().add(Math.cos(angle) * radius, 25, Math.sin(angle) * radius), 6, 1.0);
                }
            }
            // Fire 6 tridents in rapid succession (tick 40-65, one per 5 ticks)
            else if (ticksAlive == 40 && !spiralActive) {
                spiralActive = true;
                spiralStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
            }
            // Sequential firing + travel
            else if (spiralActive && ticksAlive - spiralStartTick < 90) {
                int sTick = ticksAlive - spiralStartTick;
                // Fire one trident every 5 ticks for 6 total
                if (sTick < 30 && sTick % 5 == 0) {
                    int tridentIdx = sTick / 5;
                    double angle = Math.toRadians(60 * tridentIdx);
                    Location spawnLoc = center.clone().add(Math.cos(angle) * 2, 25, Math.sin(angle) * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    spiralHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
                }
                // Move all fired tridents outward
                for (int i = 1; i < spiralHandles.size(); i++) {
                    int tridentFireTick = (i - 1) * 5;
                    if (sTick > tridentFireTick) {
                        float dist = (sTick - tridentFireTick) * 1.3f;
                        double angle = Math.toRadians(60 * (i - 1));
                        spiralHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * (2 + dist), 25, Math.sin(angle) * (2 + dist)));
                    }
                }
                if (sTick % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 8, 4.0);
                }
            }
            // Impact (tick 130)
            else if (spiralActive && ticksAlive - spiralStartTick == 90) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 15, 10.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheSpiral(plugin); }
    }

    // ================================================================
    // 28. FOCUSED BURN — Pinpoint dual-beam laser, massive damage
    // ================================================================
    public static class FocusedBurn extends BossAttack {
        private final List<BlockDisplayHandle> burnHandles = new ArrayList<>();
        private boolean beamActive = false;
        private int beamStartTick = 0;

        public FocusedBurn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_focused_burn", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dual hand charge beams converging on target
            for (int i = 0; i < 2; i++) {
                double xOff = (i == 0) ? -1.5 : 1.5;
                BlockDisplayHandle hand = displayBuilder.spawnBlock(
                    center.clone().add(xOff, 20, 0), Material.MAGMA_BLOCK);
                hand.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                burnHandles.add(hand);
                spawnedEntities.add(hand.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !beamActive) {
                float intensity = ticksAlive / 40.0f;
                for (int i = 0; i < 2 && i < burnHandles.size(); i++) {
                    float scale = 0.5f + intensity * 0.8f;
                    burnHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, 20 - scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 5), (int)(6 + intensity * 12), 1.5);
                }
                if (ticksAlive == 38) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.6f);
                }
            }
            // Beam fires — fixed position (tick 40-90 = 2.5 seconds)
            else if (ticksAlive == 40 && !beamActive) {
                beamActive = true;
                beamStartTick = ticksAlive;
                // Converging dual beams
                for (int seg = 0; seg < 8; seg++) {
                    Location segLoc = center.clone().add(0, 20, 2 + seg * 3);
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(segLoc, Material.ORANGE_STAINED_GLASS);
                    beam.scale(1.0f, 1.0f, 3.0f).glow(255, 60, 0).interpolation(1, 0);
                    burnHandles.add(beam);
                    spawnedEntities.add(beam.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 2.0f, 0.5f);
            }
            // Beam sustained (40-90 ticks)
            else if (beamActive && ticksAlive - beamStartTick < 50) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 12), 15, 2.5);
                }
            }
            // Beam ends — ground impact spot (tick 90)
            else if (beamActive && ticksAlive - beamStartTick == 50) {
                Location burnSpot = center.clone().add(0, 0.1, 24);
                // Persistent burn zone
                BlockDisplayHandle zone = displayBuilder.spawnBlock(burnSpot, Material.MAGMA_BLOCK);
                zone.scale(4.0f, 0.1f, 4.0f).glow(255, 80, 0).interpolation(2, 0);
                spawnedEntities.add(zone.entity());
                triggerImpactDamage(burnSpot);
                DisplayBuilder.crimsonDust(burnSpot, 25, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_DEATH, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FocusedBurn(plugin); }
    }

    // ================================================================
    // 29. SCATTERSHOT — 8 tridents in random scatter below her
    // ================================================================
    public static class Scattershot extends BossAttack {
        private final List<BlockDisplayHandle> scatterHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;
        private final double[] scatterAnglesX = new double[8];
        private final double[] scatterAnglesZ = new double[8];

        public Scattershot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_scattershot", AttackType.BOSS, 5), "calamitas");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
            // Pre-calculate random scatter angles
            for (int i = 0; i < 8; i++) {
                scatterAnglesX[i] = (Math.random() - 0.5) * 20;
                scatterAnglesZ[i] = (Math.random() - 0.5) * 20;
            }
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Chaotic particle burst from palms
            BlockDisplayHandle burst = displayBuilder.spawnBlock(
                center.clone().add(0, 20, 0), Material.GLOWSTONE);
            burst.scale(1.5f, 1.5f, 1.5f).glow(200, 200, 255).interpolation(2, 0);
            scatterHandles.add(burst);
            spawnedEntities.add(burst.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.8f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — chaotic burst (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !fired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 12, 3.0);
                }
            }
            // Fire 8 tridents in random scatter (tick 36)
            else if (ticksAlive == 36 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 8; i++) {
                    Location spawnLoc = center.clone().add(0, 20, 0);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.1f, 0.1f, 0.8f).glow(220, 220, 255).interpolation(1, 0);
                    scatterHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
            }
            // Tridents scatter downward (36-76 ticks = 2 seconds)
            else if (fired && ticksAlive - fireTick < 40) {
                float progress = (ticksAlive - fireTick) / 40.0f;
                for (int i = 0; i < 8; i++) {
                    int idx = 1 + i;
                    if (idx < scatterHandles.size()) {
                        double x = scatterAnglesX[i] * progress;
                        double z = scatterAnglesZ[i] * progress;
                        double y = 20 - progress * 20;
                        scatterHandles.get(idx).entity().teleport(
                            center.clone().add(x, Math.max(0.1, y), z));
                    }
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20 - progress * 15, 0), 8, 5.0);
                }
            }
            // Impact — 8 embed points (tick 76)
            else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                for (int i = 0; i < 8; i++) {
                    Location impactPt = center.clone().add(scatterAnglesX[i], 0.1, scatterAnglesZ[i]);
                    DisplayBuilder.crimsonDust(impactPt, 5, 0.8);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Scattershot(plugin); }
    }

    // ================================================================
    // 30. SIGIL — Ground sigil detonation with 4 segments + center burst
    // ================================================================
    public static class Sigil extends BossAttack {
        private final List<BlockDisplayHandle> sigilHandles = new ArrayList<>();
        private boolean sigilActive = false;
        private int sigilStartTick = 0;

        public Sigil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_sigil", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Circle on ground radius 6 + cross inside
            // Outer ring
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 6, 0.05, Math.sin(angle) * 6);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.MAGMA_BLOCK);
                ring.scale(1.0f, 0.04f, 1.0f).glow(255, 80, 0).interpolation(3, 0);
                sigilHandles.add(ring);
                spawnedEntities.add(ring.entity());
            }
            // Cross lines
            int[][] crossDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] dir : crossDirs) {
                for (int seg = 1; seg <= 5; seg++) {
                    Location crossPt = center.clone().add(dir[0] * seg * 1.2, 0.05, dir[1] * seg * 1.2);
                    BlockDisplayHandle cross = displayBuilder.spawnBlock(crossPt, Material.MAGMA_BLOCK);
                    cross.scale(0.4f, 0.04f, 0.4f).glow(255, 60, 0).interpolation(3, 0);
                    sigilHandles.add(cross);
                    spawnedEntities.add(cross.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !sigilActive) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.3, 0), 12, 6.0);
                }
            }
            // Sigil activates — segments erupt clockwise (tick 40)
            else if (ticksAlive == 40 && !sigilActive) {
                sigilActive = true;
                sigilStartTick = ticksAlive;
            }
            // Segment eruptions clockwise (40-55 ticks, one per 5 ticks)
            else if (sigilActive && ticksAlive - sigilStartTick < 20) {
                int sTick = ticksAlive - sigilStartTick;
                if (sTick % 5 == 0) {
                    int segment = sTick / 5;
                    double[][] segDirs = {{3, 3}, {3, -3}, {-3, -3}, {-3, 3}};
                    if (segment < 4) {
                        Location segLoc = center.clone().add(segDirs[segment][0], 0.1, segDirs[segment][1]);
                        for (int y = 0; y < 4; y++) {
                            BlockDisplayHandle eruption = displayBuilder.spawnBlock(
                                segLoc.clone().add(0, y, 0), Material.ORANGE_STAINED_GLASS);
                            eruption.scale(3.0f, 1.0f, 3.0f).glow(255, 60, 0).interpolation(1, 0);
                            spawnedEntities.add(eruption.entity());
                        }
                        DisplayBuilder.crimsonDust(segLoc, 20, 3.0);
                        DisplayBuilder.playSound(segLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                    }
                }
            }
            // Center detonation (tick 60)
            else if (sigilActive && ticksAlive - sigilStartTick == 20) {
                // Massive center burst
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle centerBlast = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                    centerBlast.scale(4.0f, 1.0f, 4.0f).glow(255, 40, 0).interpolation(1, 0);
                    spawnedEntities.add(centerBlast.entity());
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 30, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.7f);
            }
            // Aftermath burn (60-120 ticks = 3 seconds)
            else if (sigilActive && ticksAlive - sigilStartTick > 20 && ticksAlive - sigilStartTick < 80) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.3, 0), 8, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Sigil(plugin); }
    }
}
