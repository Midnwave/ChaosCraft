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
 * Attacks #31-40
 *
 * Full 360 sweeps. Descent bombs. Brimstone walls.
 * Arena-wide threat coverage. She owns the space.
 * NO status effects — damage only.
 */
public final class CalamitasArrivalD {

    private CalamitasArrivalD() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TridentStormFirst(plugin));
        registry.register(new DescentBomb(plugin));
        registry.register(new TripleTridentRicochet(plugin));
        registry.register(new BrimstoneColumnWalk(plugin));
        registry.register(new DoubleArc(plugin));
        registry.register(new EndRodBeam(plugin));
        registry.register(new ThePendulum(plugin));
        registry.register(new BrimstoneHalo(plugin));
        registry.register(new DiagonalCut(plugin));
        registry.register(new BrimstoneWall(plugin));
    }

    // ================================================================
    // 31. TRIDENT STORM (FIRST) — 360-degree slow rotating sweep, 20 tridents
    // ================================================================
    public static class TridentStormFirst extends BossAttack {
        private final List<BlockDisplayHandle> stormHandles = new ArrayList<>();
        private boolean sweeping = false;
        private int sweepStartTick = 0;

        public TridentStormFirst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_storm_first", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ring of spark particles at Y+22
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 3, 22, Math.sin(angle) * 3);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.IRON_BLOCK);
                ring.scale(0.2f, 0.2f, 0.2f).glow(200, 200, 255).interpolation(3, 0);
                stormHandles.add(ring);
                spawnedEntities.add(ring.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !sweeping) {
                double rot = ticksAlive * 0.15;
                for (int i = 0; i < 8 && i < stormHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i + rot;
                    stormHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 3, 22, Math.sin(angle) * 3));
                }
            }
            // Begin 360-degree sweep (tick 40 — 160 ticks = 8 seconds)
            else if (ticksAlive == 40 && !sweeping) {
                sweeping = true;
                sweepStartTick = ticksAlive;
            }
            // Fire one trident every 8 ticks, rotating 18 degrees each
            else if (sweeping && ticksAlive - sweepStartTick < 160) {
                int sTick = ticksAlive - sweepStartTick;
                if (sTick % 8 == 0) {
                    int tridentNum = sTick / 8;
                    double angle = Math.toRadians(18 * tridentNum);
                    Location spawnLoc = center.clone().add(Math.cos(angle) * 2, 22, Math.sin(angle) * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.1f, 0.1f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    stormHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.0f);
                }
                // Move all fired tridents outward
                for (int i = 8; i < stormHandles.size(); i++) {
                    int tridentNum = i - 8;
                    int tridentFireTick = tridentNum * 8;
                    if (sTick > tridentFireTick) {
                        float dist = (sTick - tridentFireTick) * 1.6f;
                        double angle = Math.toRadians(18 * tridentNum);
                        stormHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * (2 + dist), 22, Math.sin(angle) * (2 + dist)));
                    }
                }
            }
            // Sweep complete (tick 200)
            else if (sweeping && ticksAlive - sweepStartTick == 160) {
                triggerImpactDamage(center.clone().add(0, 22, 0));
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentStormFirst(plugin); }
    }

    // ================================================================
    // 32. DESCENT BOMB — Vertical dive + ground shockwave
    // ================================================================
    public static class DescentBomb extends BossAttack {
        private final List<BlockDisplayHandle> bombHandles = new ArrayList<>();
        private boolean diving = false;
        private int diveStartTick = 0;

        public DescentBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_descent_bomb", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // She rises to Y+30, crimson column streams downward
            BlockDisplayHandle column = displayBuilder.spawnBlock(
                center.clone().add(0, 30, 0), Material.RED_STAINED_GLASS);
            column.scale(1.5f, 30.0f, 1.5f).glow(180, 0, 0).interpolation(3, 0);
            bombHandles.add(column);
            spawnedEntities.add(column.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !diving) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 15, 0), 10, 1.5);
                }
            }
            // Dive (tick 40)
            else if (ticksAlive == 40 && !diving) {
                diving = true;
                diveStartTick = ticksAlive;
                // Dive model
                BlockDisplayHandle diveModel = displayBuilder.spawnBlock(
                    center.clone().add(0, 30, 0), Material.RED_STAINED_GLASS);
                diveModel.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(1, 0);
                bombHandles.add(diveModel);
                spawnedEntities.add(diveModel.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.4f);
            }
            // Diving descent (40-60 ticks = 1 second fast dive)
            else if (diving && ticksAlive - diveStartTick < 20) {
                float progress = (ticksAlive - diveStartTick) / 20.0f;
                double y = 30 - progress * 27; // Y+30 to Y+3
                if (bombHandles.size() > 1) {
                    bombHandles.get(1).entity().teleport(center.clone().add(0, y, 0));
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, y, 0), 10, 2.0);
                }
            }
            // Impact — shockwave at Y+3 (tick 60)
            else if (diving && ticksAlive - diveStartTick == 20) {
                // Shockwave ring expanding outward
                for (int ring = 0; ring < 10; ring++) {
                    double angle = (Math.PI * 2 / 10) * ring;
                    Location ringPt = center.clone().add(Math.cos(angle) * 5, 0.1, Math.sin(angle) * 5);
                    BlockDisplayHandle shock = displayBuilder.spawnBlock(ringPt, Material.ORANGE_STAINED_GLASS);
                    shock.scale(1.5f, 0.3f, 1.5f).glow(255, 60, 0).interpolation(2, 0);
                    spawnedEntities.add(shock.entity());
                }
                triggerImpactDamage(center.clone().add(0, 1, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 30, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.7f);
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DescentBomb(plugin); }
    }

    // ================================================================
    // 33. TRIPLE TRIDENT RICOCHET — 3 simultaneous wall bounces converging center
    // ================================================================
    public static class TripleTridentRicochet extends BossAttack {
        private final List<BlockDisplayHandle> tripleHandles = new ArrayList<>();
        private boolean fired = false;
        private boolean bounced = false;
        private int fireTick = 0;

        public TripleTridentRicochet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_triple_trident_ricochet", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 wall telegraph marks
            double[][] wallPoints = {{-15, 20, 0}, {0, 20, -15}, {15, 20, 0}};
            for (double[] pt : wallPoints) {
                BlockDisplayHandle mark = displayBuilder.spawnBlock(
                    center.clone().add(pt[0], pt[1], pt[2]), Material.IRON_BLOCK);
                mark.scale(0.3f, 0.3f, 0.3f).glow(200, 200, 255).interpolation(3, 0);
                tripleHandles.add(mark);
                spawnedEntities.add(mark.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 8 == 0) {
                    double[][] wallPoints = {{-15, 20, 0}, {0, 20, -15}, {15, 20, 0}};
                    for (double[] pt : wallPoints) {
                        DisplayBuilder.crimsonDust(center.clone().add(pt[0], pt[1], pt[2]), 5, 0.5);
                    }
                }
            }
            // Fire 3 tridents at walls (tick 40)
            else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 3; i++) {
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(
                        center.clone().add(0, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    tripleHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
            }
            // Tridents travel to walls (40-70 ticks)
            else if (fired && !bounced && ticksAlive - fireTick < 30) {
                float progress = (ticksAlive - fireTick) / 30.0f;
                double[][] wallPoints = {{-15, 20, 0}, {0, 20, -15}, {15, 20, 0}};
                for (int i = 0; i < 3; i++) {
                    int idx = 3 + i;
                    if (idx < tripleHandles.size()) {
                        double x = wallPoints[i][0] * progress;
                        double z = wallPoints[i][2] * progress;
                        tripleHandles.get(idx).entity().teleport(
                            center.clone().add(x, 20, z));
                    }
                }
            }
            // Bounce at walls (tick 70)
            else if (fired && !bounced && ticksAlive - fireTick == 30) {
                bounced = true;
                double[][] wallPoints = {{-15, 20, 0}, {0, 20, -15}, {15, 20, 0}};
                for (double[] pt : wallPoints) {
                    DisplayBuilder.crimsonDust(center.clone().add(pt[0], pt[1], pt[2]), 10, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_METAL_HIT, 1.0f, 1.3f);
            }
            // Reflected travel toward center (70-120 ticks = 2.5 seconds)
            else if (bounced && ticksAlive - fireTick < 80) {
                float progress = (ticksAlive - fireTick - 30) / 50.0f;
                double[][] wallPoints = {{-15, 20, 0}, {0, 20, -15}, {15, 20, 0}};
                for (int i = 0; i < 3; i++) {
                    int idx = 3 + i;
                    if (idx < tripleHandles.size()) {
                        double x = wallPoints[i][0] * (1 - progress);
                        double z = wallPoints[i][2] * (1 - progress);
                        tripleHandles.get(idx).entity().teleport(
                            center.clone().add(x, 20, z));
                    }
                }
            }
            // Converge at center (tick 120)
            else if (bounced && ticksAlive - fireTick == 80) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 15, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TripleTridentRicochet(plugin); }
    }

    // ================================================================
    // 34. BRIMSTONE COLUMN WALK — Sequential ground columns left-to-right
    // ================================================================
    public static class BrimstoneColumnWalk extends BossAttack {
        private final List<BlockDisplayHandle> walkHandles = new ArrayList<>();
        private boolean walkActive = false;
        private int walkStartTick = 0;

        public BrimstoneColumnWalk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_column_walk", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Lava line traced on ground from left to right
            for (int seg = 0; seg < 10; seg++) {
                double x = -10 + seg * 2.2;
                Location linePt = center.clone().add(x, 0.05, 8);
                BlockDisplayHandle line = displayBuilder.spawnBlock(linePt, Material.MAGMA_BLOCK);
                line.scale(2.0f, 0.04f, 0.3f).glow(255, 80, 0).interpolation(3, 0);
                walkHandles.add(line);
                spawnedEntities.add(line.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !walkActive) {
                if (ticksAlive % 8 == 0) {
                    for (int seg = 0; seg < 10; seg++) {
                        double x = -10 + seg * 2.2;
                        DisplayBuilder.crimsonDust(center.clone().add(x, 0.2, 8), 3, 0.5);
                    }
                }
            }
            // Walk begins (tick 40 — columns erupt one per 6 ticks)
            else if (ticksAlive == 40 && !walkActive) {
                walkActive = true;
                walkStartTick = ticksAlive;
            }
            // Sequential column eruptions (40-100 ticks = 3 seconds)
            else if (walkActive && ticksAlive - walkStartTick < 60) {
                int wTick = ticksAlive - walkStartTick;
                if (wTick % 6 == 0) {
                    int colIdx = wTick / 6;
                    if (colIdx < 10) {
                        double x = -10 + colIdx * 2.2;
                        Location colLoc = center.clone().add(x, 0.1, 8);
                        for (int y = 0; y < 4; y++) {
                            BlockDisplayHandle col = displayBuilder.spawnBlock(
                                colLoc.clone().add(0, y, 0), Material.ORANGE_STAINED_GLASS);
                            col.scale(1.5f, 1.0f, 1.5f).glow(255, 60, 0).interpolation(1, 0);
                            spawnedEntities.add(col.entity());
                        }
                        triggerImpactDamage(colLoc);
                        DisplayBuilder.crimsonDust(colLoc, 15, 1.5);
                        DisplayBuilder.playSound(colLoc, Sound.BLOCK_LAVA_POP, 1.0f, 0.9f);
                    }
                }
            }
            // Final column (tick 100)
            else if (walkActive && ticksAlive - walkStartTick == 60) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneColumnWalk(plugin); }
    }

    // ================================================================
    // 35. DOUBLE ARC — Two simultaneous 3-spread fans at different players
    // ================================================================
    public static class DoubleArc extends BossAttack {
        private final List<BlockDisplayHandle> dualHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public DoubleArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_double_arc", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dual fan telegraphs from each hand
            for (int hand = 0; hand < 2; hand++) {
                double xOff = (hand == 0) ? -2 : 2;
                for (int i = -1; i <= 1; i++) {
                    double angle = Math.toRadians(15 * i + (hand == 0 ? -20 : 20));
                    Location pt = center.clone().add(xOff + Math.sin(angle) * 3, 20, Math.cos(angle) * 3);
                    BlockDisplayHandle line = displayBuilder.spawnBlock(pt, Material.IRON_BLOCK);
                    line.scale(0.1f, 0.1f, 2.0f).glow(200, 200, 255).interpolation(3, 0);
                    dualHandles.add(line);
                    spawnedEntities.add(line.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-2, 20, 4), 5, 1.0);
                    DisplayBuilder.crimsonDust(center.clone().add(2, 20, 4), 5, 1.0);
                }
            }
            // Fire dual 3-fans (tick 40) — 6 tridents total
            else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int hand = 0; hand < 2; hand++) {
                    double xOff = (hand == 0) ? -2 : 2;
                    for (int i = -1; i <= 1; i++) {
                        double angle = Math.toRadians(15 * i + (hand == 0 ? -20 : 20));
                        Location spawnLoc = center.clone().add(xOff + Math.sin(angle), 20, Math.cos(angle));
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                        trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                        dualHandles.add(trident);
                        spawnedEntities.add(trident.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.8f, 1.0f);
            }
            // 6 tridents travel (40-80 ticks)
            else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 1.8f;
                int idx = 6;
                for (int hand = 0; hand < 2; hand++) {
                    double xOff = (hand == 0) ? -2 : 2;
                    for (int i = -1; i <= 1; i++) {
                        if (idx < dualHandles.size()) {
                            double angle = Math.toRadians(15 * i + (hand == 0 ? -20 : 20));
                            dualHandles.get(idx).entity().teleport(
                                center.clone().add(xOff + Math.sin(angle) * (1 + dist),
                                    20, Math.cos(angle) * (1 + dist)));
                        }
                        idx++;
                    }
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, dist), 8, 3.0);
                }
            }
            // Impact (tick 80)
            else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 72));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 72), 12, 4.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoubleArc(plugin); }
    }

    // ================================================================
    // 36. END_ROD BEAM — Sky stream pulled into vertical spike at target
    // ================================================================
    public static class EndRodBeam extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamFired = false;
        private int beamFireTick = 0;

        public EndRodBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_end_rod_beam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // END_ROD stream bends toward her hand
            BlockDisplayHandle streamPull = displayBuilder.spawnBlock(
                center.clone().add(0, 30, 5), Material.END_ROD);
            streamPull.scale(1.0f, 30.0f, 1.0f).glow(255, 255, 230).interpolation(3, 0);
            beamHandles.add(streamPull);
            spawnedEntities.add(streamPull.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !beamFired) {
                float compress = ticksAlive / 40.0f;
                if (!beamHandles.isEmpty()) {
                    float width = 1.0f - compress * 0.7f;
                    beamHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-width / 2, 0, 5 - width / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(width, 30.0f, width),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 15, 5), 8, 2.0,
                        255, 255, 230, 1.0f);
                }
            }
            // Beam fires vertically (tick 40)
            else if (ticksAlive == 40 && !beamFired) {
                beamFired = true;
                beamFireTick = ticksAlive;
                // Concentrated beam column
                for (int y = 0; y < 20; y += 2) {
                    BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 5), Material.SEA_LANTERN);
                    beamSeg.scale(1.0f, 2.0f, 1.0f).glow(255, 255, 255).interpolation(1, 0);
                    beamHandles.add(beamSeg);
                    spawnedEntities.add(beamSeg.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 1.2f);
            }
            // Beam active (40-60 ticks = 1 second)
            else if (beamFired && ticksAlive - beamFireTick < 20) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 10, 5), 15, 3.0,
                        255, 255, 230, 1.5f);
                }
            }
            // Beam ends (tick 60)
            else if (beamFired && ticksAlive - beamFireTick == 20) {
                triggerImpactDamage(center.clone().add(0, 0.5, 5));
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EndRodBeam(plugin); }
    }

    // ================================================================
    // 37. THE PENDULUM — Alternating direct/predictive throws, 8 total
    // ================================================================
    public static class ThePendulum extends BossAttack {
        private final List<BlockDisplayHandle> pendHandles = new ArrayList<>();
        private boolean active = false;
        private int activeStartTick = 0;

        public ThePendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_pendulum", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // T-pose, left arm sparks first
            BlockDisplayHandle leftArm = displayBuilder.spawnBlock(
                center.clone().add(-2, 20, 0), Material.IRON_BLOCK);
            leftArm.scale(0.3f, 0.3f, 0.3f).glow(200, 200, 255).interpolation(3, 0);
            pendHandles.add(leftArm);
            spawnedEntities.add(leftArm.entity());

            BlockDisplayHandle rightArm = displayBuilder.spawnBlock(
                center.clone().add(2, 20, 0), Material.IRON_BLOCK);
            rightArm.scale(0.3f, 0.3f, 0.3f).glow(180, 180, 200).interpolation(3, 0);
            pendHandles.add(rightArm);
            spawnedEntities.add(rightArm.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph pendulum visual (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !active) {
                // Alternate glow between hands
                boolean leftActive = (ticksAlive % 20) < 10;
                if (pendHandles.size() >= 2) {
                    pendHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-0.15f, 19.85f, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(leftActive ? 0.5f : 0.2f, leftActive ? 0.5f : 0.2f, leftActive ? 0.5f : 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    pendHandles.get(1).entity().setTransformation(new Transformation(
                        new Vector3f(-0.15f, 19.85f, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(leftActive ? 0.2f : 0.5f, leftActive ? 0.2f : 0.5f, leftActive ? 0.2f : 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
            }
            // Begin pendulum sequence (tick 40 — 8 throws over 12 seconds)
            else if (ticksAlive == 40 && !active) {
                active = true;
                activeStartTick = ticksAlive;
            }
            // Fire tridents alternating every 30 ticks (1.5 seconds)
            else if (active && ticksAlive - activeStartTick < 240) {
                int pTick = ticksAlive - activeStartTick;
                if (pTick % 30 == 0) {
                    int throwNum = pTick / 30;
                    if (throwNum < 8) {
                        boolean isLeft = throwNum % 2 == 0;
                        double xOff = isLeft ? -2 : 2;
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(
                            center.clone().add(xOff, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                        trident.scale(0.12f, 0.12f, 1.2f)
                            .glow(isLeft ? 220 : 255, isLeft ? 220 : 200, 255).interpolation(1, 0);
                        pendHandles.add(trident);
                        spawnedEntities.add(trident.entity());
                        float pitch = isLeft ? 1.2f : 0.9f;
                        DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, pitch);
                    }
                }
                // Move all fired tridents
                for (int i = 2; i < pendHandles.size(); i++) {
                    int throwIdx = i - 2;
                    int throwFireTick = throwIdx * 30;
                    if (pTick > throwFireTick) {
                        float dist = (pTick - throwFireTick) * 2.0f;
                        boolean isLeft = throwIdx % 2 == 0;
                        double xOff = isLeft ? -2 : 2.5;
                        pendHandles.get(i).entity().teleport(
                            center.clone().add(xOff, 20, dist));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThePendulum(plugin); }
    }

    // ================================================================
    // 38. BRIMSTONE HALO — 8 skulls in contracting ring from arena edge
    // ================================================================
    public static class BrimstoneHalo extends BossAttack {
        private final List<BlockDisplayHandle> haloHandles = new ArrayList<>();
        private boolean contracting = false;
        private int contractStartTick = 0;

        public BrimstoneHalo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_halo", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Outer ring at Y+2, maximum arena radius
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location skullLoc = center.clone().add(Math.cos(angle) * 20, 2, Math.sin(angle) * 20);
                BlockDisplayHandle skull = displayBuilder.spawnBlock(skullLoc, Material.SOUL_SAND);
                skull.scale(0.8f, 0.8f, 0.8f).glow(180, 80, 0).interpolation(2, 0);
                haloHandles.add(skull);
                spawnedEntities.add(skull.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !contracting) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 / 8) * i;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.cos(angle) * 20, 2, Math.sin(angle) * 20), 5, 0.8);
                    }
                }
            }
            // Contraction begins (tick 40 — skulls slide inward over 100 ticks = 5 seconds)
            else if (ticksAlive == 40 && !contracting) {
                contracting = true;
                contractStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
            }
            else if (contracting && ticksAlive - contractStartTick < 100) {
                float progress = (ticksAlive - contractStartTick) / 100.0f;
                double radius = 20 - progress * 17; // 20 -> 3 blocks
                for (int i = 0; i < 8 && i < haloHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    haloHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 2, Math.sin(angle) * radius));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 10, radius);
                }
            }
            // Center detonation (tick 140) — 8 simultaneous explosions
            else if (contracting && ticksAlive - contractStartTick == 100) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location detLoc = center.clone().add(Math.cos(angle) * 3, 0.1, Math.sin(angle) * 3);
                    BlockDisplayHandle blast = displayBuilder.spawnBlock(detLoc, Material.ORANGE_STAINED_GLASS);
                    blast.scale(2.0f, 2.0f, 2.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(blast.entity());
                }
                triggerImpactDamage(center.clone().add(0, 1, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 30, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneHalo(plugin); }
    }

    // ================================================================
    // 39. DIAGONAL CUT — 3 tridents in diagonal stagger
    // ================================================================
    public static class DiagonalCut extends BossAttack {
        private final List<BlockDisplayHandle> diagHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public DiagonalCut(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_diagonal_cut", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 spark points in diagonal 45-degree line
            for (int i = -1; i <= 1; i++) {
                Location pt = center.clone().add(i * 2, 20, i * 2);
                BlockDisplayHandle point = displayBuilder.spawnBlock(pt, Material.IRON_BLOCK);
                point.scale(0.2f, 0.2f, 0.2f).glow(200, 200, 255).interpolation(3, 0);
                diagHandles.add(point);
                spawnedEntities.add(point.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !fired) {
                if (ticksAlive % 8 == 0) {
                    for (int i = -1; i <= 1; i++) {
                        DisplayBuilder.crimsonDust(center.clone().add(i * 2, 20, i * 2), 4, 0.5);
                    }
                }
            }
            // Fire 3 tridents staggered (tick 36, 44, 52)
            else if (ticksAlive >= 36 && ticksAlive <= 52 && !fired) {
                if (ticksAlive == 36) {
                    fired = true;
                    fireTick = ticksAlive;
                }
                int fTick = ticksAlive - 36;
                if (fTick % 8 == 0 && fTick / 8 < 3) {
                    int tIdx = fTick / 8;
                    int offset = tIdx - 1;
                    Location spawnLoc = center.clone().add(offset * 2, 20, offset * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    diagHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.1f);
                }
            }
            // Tridents travel diagonally (36-76 ticks)
            else if (fired && ticksAlive - fireTick < 40) {
                for (int i = 3; i < diagHandles.size(); i++) {
                    int tIdx = i - 3;
                    int tFireTick = tIdx * 8;
                    int tTick = (ticksAlive - fireTick) - tFireTick;
                    if (tTick > 0) {
                        float dist = tTick * 2.0f;
                        int offset = tIdx - 1;
                        diagHandles.get(i).entity().teleport(
                            center.clone().add(offset * 2, 20, offset * 2 + dist));
                    }
                }
            }
            // Impact (tick 76)
            else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 60));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 60), 10, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DiagonalCut(plugin); }
    }

    // ================================================================
    // 40. BRIMSTONE WALL — Slow full-width wall of 7 skulls advancing
    // ================================================================
    public static class BrimstoneWall extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean wallActive = false;
        private int wallStartTick = 0;

        public BrimstoneWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_wall", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wall of fire at Y+24 spanning arena width
            for (int i = 0; i < 7; i++) {
                double x = -9 + i * 3;
                double y = 21 + i % 2 * 3; // Alternate heights
                Location skullLoc = center.clone().add(x, y, -5);
                BlockDisplayHandle skull = displayBuilder.spawnBlock(skullLoc, Material.SOUL_SAND);
                skull.scale(0.8f, 0.8f, 0.8f).glow(180, 80, 0).interpolation(2, 0);
                wallHandles.add(skull);
                spawnedEntities.add(skull.entity());
            }
            // Visual wall fill between skulls
            for (int seg = 0; seg < 5; seg++) {
                double x = -7.5 + seg * 3.5;
                Location fillLoc = center.clone().add(x, 23, -5);
                BlockDisplayHandle fill = displayBuilder.spawnBlock(fillLoc, Material.ORANGE_STAINED_GLASS);
                fill.scale(3.0f, 6.0f, 0.3f).glow(255, 60, 0).interpolation(2, 0);
                wallHandles.add(fill);
                spawnedEntities.add(fill.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !wallActive) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, -5), 15, 10.0);
                }
            }
            // Wall advances (tick 40 — 0.5 blocks/sec over 200 ticks = 10 seconds)
            else if (ticksAlive == 40 && !wallActive) {
                wallActive = true;
                wallStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.8f, 0.6f);
            }
            else if (wallActive && ticksAlive - wallStartTick < 200) {
                float advance = (ticksAlive - wallStartTick) * 0.15f;
                // Move skulls
                for (int i = 0; i < 7 && i < wallHandles.size(); i++) {
                    double x = -9 + i * 3;
                    double y = 21 + i % 2 * 3;
                    wallHandles.get(i).entity().teleport(
                        center.clone().add(x, y, -5 + advance));
                }
                // Move fill panels
                for (int seg = 0; seg < 5; seg++) {
                    int idx = 7 + seg;
                    if (idx < wallHandles.size()) {
                        double x = -7.5 + seg * 3.5;
                        wallHandles.get(idx).entity().teleport(
                            center.clone().add(x, 23, -5 + advance));
                    }
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, -5 + advance), 12, 10.0);
                }
            }
            // Far wall detonation (tick 240)
            else if (wallActive && ticksAlive - wallStartTick == 200) {
                for (int i = 0; i < 7; i++) {
                    double x = -9 + i * 3;
                    Location detLoc = center.clone().add(x, 0.5, 25);
                    DisplayBuilder.crimsonDust(detLoc, 10, 2.0);
                    DisplayBuilder.playSound(detLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                }
                triggerImpactDamage(center.clone().add(0, 1, 20));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneWall(plugin); }
    }
}
