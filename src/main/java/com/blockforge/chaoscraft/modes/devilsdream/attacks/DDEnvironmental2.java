package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream Mode — ENVIRONMENTAL ATTACKS (set 2 — entries 61–70).
 * Each attack is a multi-display nightmare set-piece: gallows, hand-pits,
 * shattered hourglass, broken pillars, ashen throne, void mirror lake,
 * carousel, boneyard orchard, cracked cosmos, and the well of screams.
 */
public final class DDEnvironmental2 {
    private DDEnvironmental2() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WitheredGallows(plugin));
        registry.register(new PitOfHands(plugin));
        registry.register(new ShatteredHourglass(plugin));
        registry.register(new GraveyardOfPillars(plugin));
        registry.register(new AshenThroneRoom(plugin));
        registry.register(new VoidMirrorLake(plugin));
        registry.register(new NightmareCarousel(plugin));
        registry.register(new BoneyardOrchard(plugin));
        registry.register(new CrackedCosmos(plugin));
        registry.register(new WellOfScreams(plugin));
    }

    // ================================================================
    // 61. WITHERED GALLOWS — atmospheric. Pendulum ghost on noose.
    //     Frame post + horizontal arm + bone noose + calcite ghost
    //     + deepslate platform. ~28 displays.
    // ================================================================
    public static class WitheredGallows extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private final List<BlockDisplayHandle> platform = new ArrayList<>();
        private final List<BlockDisplayHandle> noose = new ArrayList<>();
        private final List<BlockDisplayHandle> ghost = new ArrayList<>();

        public WitheredGallows(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("withered_gallows", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(35);
            config.setDurationTicks(360);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.4f);

            // Vertical post — 8 tall blackstone
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-3, i, 0), Material.BLACKSTONE);
                h.scale(1.0f, 1.0f, 1.0f).interpolation(20, 0);
                frame.add(h);
            }
            // Horizontal arm — 5 long blackstone at top
            for (int i = 0; i < 5; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-3 + i, 7, 0), Material.BLACKSTONE);
                h.scale(1.0f, 1.0f, 1.0f).interpolation(20, 0);
                frame.add(h);
            }
            // Platform — 4x2 deepslate tiles below
            for (int dx = -4; dx <= -1; dx++) {
                for (int dz = -1; dz <= 0; dz++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(dx, -0.1, dz), Material.DEEPSLATE_TILES);
                    h.scale(1.0f, 0.2f, 1.0f).interpolation(20, 0);
                    platform.add(h);
                }
            }
            // Noose knot — bone block cluster at end of arm (3)
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(1, 6.5 - i * 0.4, 0), Material.BONE_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(240, 230, 200).interpolation(20, 0);
                noose.add(h);
            }
            // Calcite ghost figure (humanoid silhouette: head, body, 2 arms, 2 legs = 5)
            BlockDisplayHandle head = displayBuilder.spawnBlock(
                    center.clone().add(1, 5.0, 0), Material.CALCITE);
            head.scale(0.5f, 0.5f, 0.5f).glow(240, 230, 200).brightness(15, 15).interpolation(20, 0);
            ghost.add(head);
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                    center.clone().add(1, 4.0, 0), Material.CALCITE);
            body.scale(0.6f, 1.0f, 0.4f).glow(240, 230, 200).brightness(15, 15).interpolation(20, 0);
            ghost.add(body);
            BlockDisplayHandle armL = displayBuilder.spawnBlock(
                    center.clone().add(0.7, 4.2, 0), Material.CALCITE);
            armL.scale(0.25f, 0.8f, 0.25f).glow(240, 230, 200).interpolation(20, 0);
            ghost.add(armL);
            BlockDisplayHandle armR = displayBuilder.spawnBlock(
                    center.clone().add(1.3, 4.2, 0), Material.CALCITE);
            armR.scale(0.25f, 0.8f, 0.25f).glow(240, 230, 200).interpolation(20, 0);
            ghost.add(armR);
            BlockDisplayHandle legs = displayBuilder.spawnBlock(
                    center.clone().add(1, 3.0, 0), Material.CALCITE);
            legs.scale(0.5f, 1.0f, 0.3f).glow(240, 230, 200).brightness(15, 15).interpolation(20, 0);
            ghost.add(legs);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Ghost pendulum sway every 30 ticks
            if (tick % 30 == 0) {
                float sway = (float) Math.sin(tick * 0.05) * 0.6f;
                for (int i = 0; i < ghost.size(); i++) {
                    BlockDisplayHandle h = ghost.get(i);
                    Vector3f base;
                    Vector3f scl;
                    switch (i) {
                        case 0: base = new Vector3f(-0.25f + sway, 5.0f, -0.25f); scl = new Vector3f(0.5f,0.5f,0.5f); break;
                        case 1: base = new Vector3f(-0.30f + sway, 4.0f, -0.20f); scl = new Vector3f(0.6f,1.0f,0.4f); break;
                        case 2: base = new Vector3f(-0.425f + sway, 4.2f, -0.125f); scl = new Vector3f(0.25f,0.8f,0.25f); break;
                        case 3: base = new Vector3f(0.175f + sway, 4.2f, -0.125f); scl = new Vector3f(0.25f,0.8f,0.25f); break;
                        default: base = new Vector3f(-0.25f + sway, 3.0f, -0.15f); scl = new Vector3f(0.5f,1.0f,0.3f); break;
                    }
                    // offset to gallows arm position (x=+1)
                    base.add(1f, 0f, 0f);
                    h.animateTo(base,
                            new AxisAngle4f((float) (Math.sin(tick * 0.04) * 0.25), 0, 0, 1),
                            scl, 30);
                }
            }

            // Whole gallows creak every 100 ticks (rock 3 deg on Z)
            if (tick % 100 == 0) {
                float ang = (float) Math.toRadians(3.0 * Math.sin(tick * 0.01));
                for (BlockDisplayHandle h : frame) {
                    h.entity().setRotation(0, 0); // ensure flat
                }
                for (BlockDisplayHandle h : platform) {
                    h.entity().setRotation(0, 0);
                }
                // we use rotate via animateTo for ghost noose only since frames are already placed
                for (BlockDisplayHandle h : noose) {
                    h.animateTo(null, new AxisAngle4f(ang, 0, 0, 1),
                            new Vector3f(0.4f, 0.4f, 0.4f), 40);
                }
            }

            // Particles
            if (tick % 6 == 0) {
                Location nooseLoc = getCenter().clone().add(1, 6, 0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, nooseLoc, 1, 0.2, 0.2, 0.2, 0.005);
                DisplayBuilder.dustParticles(nooseLoc, 2, 0.4, 240, 230, 200, 1.0f);
            }
            if (tick % 10 == 0) {
                Location ghostLoc = getCenter().clone().add(1, 4, 0);
                w.spawnParticle(Particle.SOUL, ghostLoc, 1, 0.4, 0.6, 0.4, 0.01);
            }
            if (tick % 20 == 0) {
                Location plat = getCenter().clone().add(-2.5, 0.2, -0.5);
                DisplayBuilder.dustParticles(plat, 1, 0.5, 100, 80, 60, 1.0f);
            }
            if (tick % 70 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_WOOD_STEP, 0.4f, 0.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new WitheredGallows(plugin); }
    }

    // ================================================================
    // 62. PIT OF HANDS — active hazard. 8 hands reach/grasp/retract from
    //     a deepslate-rim pit. ~32 displays.
    // ================================================================
    public static class PitOfHands extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<HandUnit> hands = new ArrayList<>();

        private static class HandUnit {
            BlockDisplayHandle palm;
            List<BlockDisplayHandle> fingers = new ArrayList<>();
            double angle;
            int phase; // 0 hidden, 1 rising, 2 grasping, 3 retracting
            int phaseStart;
        }

        public PitOfHands(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pit_of_hands", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(360);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.3f);

            // Rim ring — 12 deepslate tiles in a circle
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                Location p = center.clone().add(Math.cos(angle) * 3.5, 0.05, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_TILES);
                h.scale(0.9f, 0.2f, 0.9f).interpolation(15, 0);
                rim.add(h);
            }

            // 8 hands, each: palm + 3 finger segments = 4 displays * 8 = 32
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                double r = 1.5 + Math.random() * 1.0;
                HandUnit hu = new HandUnit();
                hu.angle = angle;
                hu.phase = 0;
                hu.phaseStart = i * 6;

                Location palmLoc = center.clone().add(Math.cos(angle) * r, -1.0, Math.sin(angle) * r);
                hu.palm = displayBuilder.spawnBlock(palmLoc, Material.BONE_BLOCK);
                hu.palm.scale(0.5f, 0.5f, 0.5f).glow(240, 230, 200).interpolation(15, 0);

                for (int f = 0; f < 3; f++) {
                    Location fingerLoc = palmLoc.clone().add(0, 0.4 + f * 0.3, 0);
                    BlockDisplayHandle finger = displayBuilder.spawnBlock(fingerLoc,
                            f == 2 ? Material.BLACKSTONE : Material.BONE_BLOCK);
                    finger.scale(0.18f, 0.35f, 0.18f).interpolation(15, 0);
                    if (f == 2) finger.glow(40, 40, 40);
                    hu.fingers.add(finger);
                }
                hands.add(hu);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Cycle hands through phases — staggered
            for (int i = 0; i < hands.size(); i++) {
                HandUnit hu = hands.get(i);
                int local = tick - hu.phaseStart;
                if (local < 0) continue;
                int cycle = local % 80;
                int newPhase;
                if (cycle < 5) newPhase = 1;        // rising start
                else if (cycle < 40) newPhase = 2;  // active grasp
                else if (cycle < 50) newPhase = 3;  // retract
                else newPhase = 0;                  // hidden

                if (newPhase != hu.phase) {
                    hu.phase = newPhase;
                    double r = 1.5 + 0.5 * Math.sin(hu.angle * 3);
                    float baseX = (float) (Math.cos(hu.angle) * r);
                    float baseZ = (float) (Math.sin(hu.angle) * r);
                    if (newPhase == 1 || newPhase == 2) {
                        // Reach upward
                        hu.palm.animateTo(
                                new Vector3f(baseX - 0.25f, 1.2f, baseZ - 0.25f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(0.5f, 0.5f, 0.5f), 12);
                        for (int f = 0; f < hu.fingers.size(); f++) {
                            float curl = (newPhase == 2) ? -0.4f : 0f;
                            hu.fingers.get(f).animateTo(
                                    new Vector3f(baseX - 0.09f, 1.5f + f * 0.3f, baseZ - 0.09f),
                                    new AxisAngle4f(curl, 1, 0, 0),
                                    new Vector3f(0.18f, 0.35f, 0.18f), 12);
                        }
                    } else if (newPhase == 3) {
                        // Retract
                        hu.palm.animateTo(
                                new Vector3f(baseX - 0.25f, -1.0f, baseZ - 0.25f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(0.5f, 0.5f, 0.5f), 18);
                        for (int f = 0; f < hu.fingers.size(); f++) {
                            hu.fingers.get(f).animateTo(
                                    new Vector3f(baseX - 0.09f, -0.6f + f * 0.2f, baseZ - 0.09f),
                                    new AxisAngle4f(0, 1, 0, 0),
                                    new Vector3f(0.18f, 0.35f, 0.18f), 18);
                        }
                    }
                }
            }

            // Particles
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, 0.1, 0),
                        4, 2.0, 0.3, 2.0, 0.01);
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 0.5, 0),
                        3, 2.5, 240, 230, 200, 1.1f);
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new PitOfHands(plugin); }
    }

    // ================================================================
    // 63. SHATTERED HOURGLASS — atmosphere centerpiece. Two glass halves
    //     + frame + basalt debris pile. ~28 displays.
    // ================================================================
    public static class ShatteredHourglass extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> topHalf = new ArrayList<>();
        private final List<BlockDisplayHandle> botHalf = new ArrayList<>();
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private final List<BlockDisplayHandle> debris = new ArrayList<>();

        public ShatteredHourglass(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shattered_hourglass", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(360);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.3f);

            // Frame rims — 4 blackstone disc segments at top and bottom (8)
            for (int rim = 0; rim < 2; rim++) {
                double y = (rim == 0) ? 6.0 : 1.0;
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI * 2 * i / 4;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(a) * 1.8, y, Math.sin(a) * 1.8),
                            Material.BLACKSTONE);
                    h.scale(0.6f, 0.3f, 0.6f).interpolation(20, 0);
                    frame.add(h);
                }
            }

            // Top half — 8 red stained glass blocks in ovoid shell
            double[][] topShell = {
                    {1.4, 5.0, 0}, {-1.4, 5.0, 0}, {0, 5.0, 1.4}, {0, 5.0, -1.4},
                    {1.0, 4.2, 1.0}, {-1.0, 4.2, -1.0}, {1.0, 4.2, -1.0}, {-1.0, 4.2, 1.0}
            };
            for (double[] o : topShell) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(o[0], o[1], o[2]), Material.RED_STAINED_GLASS);
                h.scale(0.7f, 0.7f, 0.7f).glow(180, 30, 30).interpolation(25, 0);
                topHalf.add(h);
            }

            // Bottom half — 8 red stained glass blocks
            double[][] botShell = {
                    {1.4, 2.0, 0}, {-1.4, 2.0, 0}, {0, 2.0, 1.4}, {0, 2.0, -1.4},
                    {1.0, 2.8, 1.0}, {-1.0, 2.8, -1.0}, {1.0, 2.8, -1.0}, {-1.0, 2.8, 1.0}
            };
            for (double[] o : botShell) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(o[0], o[1], o[2]), Material.RED_STAINED_GLASS);
                h.scale(0.7f, 0.7f, 0.7f).glow(180, 30, 30).interpolation(25, 0);
                botHalf.add(h);
            }

            // Basalt debris pile — 6 blocks at floor
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r),
                        Material.BASALT);
                h.scale(0.4f + (float) Math.random() * 0.3f, 0.3f, 0.4f + (float) Math.random() * 0.3f)
                        .interpolation(20, 0);
                debris.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Halves rotate opposite directions slowly
            if (tick % 15 == 0) {
                float topAng = tick * 0.015f;
                float botAng = -tick * 0.015f;
                for (int i = 0; i < topHalf.size(); i++) {
                    topHalf.get(i).animateTo(null,
                            new AxisAngle4f(topAng, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 15);
                }
                for (int i = 0; i < botHalf.size(); i++) {
                    botHalf.get(i).animateTo(null,
                            new AxisAngle4f(botAng, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 15);
                }
            }

            // Debris drift every 50 ticks
            if (tick % 50 == 0) {
                for (BlockDisplayHandle h : debris) {
                    float dx = (float) ((Math.random() - 0.5) * 0.3);
                    float dz = (float) ((Math.random() - 0.5) * 0.3);
                    h.animateTo(new Vector3f(dx, 0.1f, dz),
                            new AxisAngle4f((float) (Math.random() * 0.5), 0, 1, 0),
                            new Vector3f(0.5f, 0.3f, 0.5f), 50);
                }
            }

            // Eternal red sand stream from top half through gap to floor
            if (tick % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    double oy = 4.0 - (Math.random() * 3.5);
                    Location loc = getCenter().clone().add(
                            (Math.random() - 0.5) * 0.4, oy, (Math.random() - 0.5) * 0.4);
                    DisplayBuilder.dustParticles(loc, 1, 0.05, 180, 30, 30, 1.0f);
                }
            }

            // Glass crack dust occasionally
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(0, 4, 0),
                        Sound.BLOCK_GLASS_HIT, 0.5f, 0.3f);
                w.spawnParticle(Particle.BLOCK, getCenter().clone().add(0, 4, 0),
                        15, 1.5, 0.5, 1.5, 0.05, Material.RED_STAINED_GLASS.createBlockData());
            }
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SAND_FALL, 0.4f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ShatteredHourglass(plugin); }
    }

    // ================================================================
    // 64. GRAVEYARD OF PILLARS — atmosphere/hazard. 6 broken pillars
    //     slowly sink. ~28 displays.
    // ================================================================
    public static class GraveyardOfPillars extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tops = new ArrayList<>();
        private final List<BlockDisplayHandle> decay = new ArrayList<>();

        public GraveyardOfPillars(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("graveyard_of_pillars", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(600);
            config.setCooldownTicks(700);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BRICKS_BREAK, 1.0f, 0.3f);

            int[] heights = {8, 7, 5, 4, 6, 3};
            for (int p = 0; p < 6; p++) {
                double angle = Math.PI * 2 * p / 6;
                double r = 5 + (p % 2) * 1.5;
                double bx = Math.cos(angle) * r;
                double bz = Math.sin(angle) * r;
                int h = heights[p];

                for (int y = 0; y < h; y++) {
                    BlockDisplayHandle b = displayBuilder.spawnBlock(
                            center.clone().add(bx, y, bz), Material.BLACKSTONE);
                    b.scale(0.95f, 1.0f, 0.95f).interpolation(20, 0);
                    pillarBlocks.add(b);
                }
                // Broken top — deepslate tile shear face
                BlockDisplayHandle top = displayBuilder.spawnBlock(
                        center.clone().add(bx, h - 0.05, bz), Material.DEEPSLATE_TILES);
                top.scale(1.0f, 0.2f, 1.0f).interpolation(20, 0);
                tops.add(top);

                // Decay — crying obsidian creeping up the side
                BlockDisplayHandle d = displayBuilder.spawnBlock(
                        center.clone().add(bx + 0.3, h * 0.4, bz), Material.CRYING_OBSIDIAN);
                d.scale(0.4f, 1.5f, 0.4f).glow(90, 0, 130).interpolation(20, 0);
                decay.add(d);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Slow sink — 1 block over 600 ticks
            float sink = -(tick / 600f);
            if (tick % 60 == 0) {
                for (BlockDisplayHandle b : pillarBlocks) {
                    b.animateTo(new Vector3f(0, sink, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 1.0f, 0.95f), 60);
                }
                for (BlockDisplayHandle t : tops) {
                    t.animateTo(new Vector3f(0, sink, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.0f, 0.2f, 1.0f), 60);
                }
            }

            // Decay slowly extends scale up
            if (tick % 80 == 0) {
                float grow = 1.5f + (tick / 800f);
                for (BlockDisplayHandle d : decay) {
                    d.animateTo(new Vector3f(0, sink, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f, grow, 0.4f), 80);
                }
            }

            // Tops occasionally shed fragments — every 100 ticks one top scales briefly down
            if (tick % 100 == 0 && !tops.isEmpty()) {
                int idx = (int) (Math.random() * tops.size());
                BlockDisplayHandle t = tops.get(idx);
                t.animateTo(new Vector3f(0, sink + 0.2f, 0),
                        new AxisAngle4f((float) Math.random(), 1, 0, 1),
                        new Vector3f(0.05f, 0.05f, 0.05f), 6);
                // Then return
                Location at = getCenter().clone().add(0, 1, 0);
                w.spawnParticle(Particle.BLOCK, at, 10, 1, 1, 1, 0.05,
                        Material.DEEPSLATE_TILES.createBlockData());
            }

            if (tick % 8 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter(), 3, 5.0, 1.0, 5.0, 0.005);
            }
            if (tick % 12 == 0) {
                for (int i = 0; i < 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    Location loc = getCenter().clone().add(Math.cos(a) * 5, Math.random() * 4, Math.sin(a) * 5);
                    DisplayBuilder.dustParticles(loc, 1, 0.3, 90, 0, 130, 1.2f);
                }
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BRICKS_FALL, 0.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new GraveyardOfPillars(plugin); }
    }

    // ================================================================
    // 65. THE ASHEN THRONE ROOM — atmosphere backdrop. 4 wall panels +
    //     throne + sconces + ash piles. ~34 displays.
    // ================================================================
    public static class AshenThroneRoom extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> throne = new ArrayList<>();
        private final List<BlockDisplayHandle> sconces = new ArrayList<>();
        private final List<BlockDisplayHandle> ash = new ArrayList<>();

        public AshenThroneRoom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ashen_throne_room", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(500);
            config.setCooldownTicks(560);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.7f, 0.3f);

            // 4 wall panels behind center, each 3x5 tile = 15 displays * 4 = too many.
            // Compress: each wall is 3 wide x 2 tall scaled tile = 6 displays. 4 walls = 24.
            // Then throne 4, sconces 4, ash 4 = 36 total.
            for (int wIdx = 0; wIdx < 4; wIdx++) {
                double a = Math.PI + (wIdx - 1.5) * 0.4; // arc behind center
                double bx = Math.cos(a) * 8;
                double bz = Math.sin(a) * 8;
                for (int dx = 0; dx < 3; dx++) {
                    for (int dy = 0; dy < 2; dy++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                new Location(center.getWorld(), center.getX() + bx + (dx - 1) * 1.0,
                                        center.getY() + dy * 2.5, center.getZ() + bz),
                                Material.BLACKSTONE);
                        h.scale(1.0f, 2.5f, 0.4f).interpolation(20, 0);
                        walls.add(h);
                    }
                }
            }

            // Throne — 4 carved blackstone (seat, backrest, 2 armrests)
            Location tCenter = center.clone().add(Math.cos(Math.PI) * 6, 0, Math.sin(Math.PI) * 6);
            BlockDisplayHandle seat = displayBuilder.spawnBlock(tCenter, Material.POLISHED_BLACKSTONE);
            seat.scale(1.4f, 0.8f, 1.4f).interpolation(20, 0);
            throne.add(seat);
            BlockDisplayHandle back = displayBuilder.spawnBlock(
                    tCenter.clone().add(-0.5, 1.5, 0), Material.POLISHED_BLACKSTONE);
            back.scale(0.4f, 3.0f, 1.4f).interpolation(20, 0);
            throne.add(back);
            BlockDisplayHandle armL = displayBuilder.spawnBlock(
                    tCenter.clone().add(0.0, 0.8, 0.7), Material.POLISHED_BLACKSTONE);
            armL.scale(1.0f, 0.4f, 0.3f).interpolation(20, 0);
            throne.add(armL);
            BlockDisplayHandle armR = displayBuilder.spawnBlock(
                    tCenter.clone().add(0.0, 0.8, -0.7), Material.POLISHED_BLACKSTONE);
            armR.scale(1.0f, 0.4f, 0.3f).interpolation(20, 0);
            throne.add(armR);

            // Sconces — 4 shroomlight on wall panels
            for (int i = 0; i < 4; i++) {
                double a = Math.PI + (i - 1.5) * 0.4;
                Location p = center.clone().add(Math.cos(a) * 7.5, 3.5, Math.sin(a) * 7.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 180, 80).brightness(15, 15).interpolation(10, 0);
                sconces.add(h);
            }

            // Ash piles — 4 tuff at base of walls
            for (int i = 0; i < 4; i++) {
                double a = Math.PI + (i - 1.5) * 0.4;
                Location p = center.clone().add(Math.cos(a) * 7.0, 0.05, Math.sin(a) * 7.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.TUFF);
                h.scale(1.2f, 0.25f, 1.2f).interpolation(15, 0);
                ash.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Wall panels sway slightly
            if (tick % 30 == 0) {
                float ang = (float) Math.toRadians(2.0 * Math.sin(tick * 0.02));
                for (BlockDisplayHandle h : walls) {
                    h.animateTo(null, new AxisAngle4f(ang, 1, 0, 0),
                            new Vector3f(1.0f, 2.5f, 0.4f), 30);
                }
            }

            // Sconce flicker — scale pulse
            if (tick % 8 == 0) {
                float s = 0.4f + (float) Math.random() * 0.15f;
                for (BlockDisplayHandle h : sconces) {
                    h.animateTo(null, new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, s, s), 6);
                }
            }

            // Ash drift sideways and back
            if (tick % 60 == 0) {
                float drift = (float) Math.sin(tick * 0.02) * 0.5f;
                for (BlockDisplayHandle h : ash) {
                    h.animateTo(new Vector3f(drift, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.2f, 0.25f, 1.2f), 60);
                }
            }

            // Throne tilt on one armrest then right itself
            if (tick % 120 == 0) {
                float tilt = (tick / 120) % 2 == 0 ? 0.15f : 0f;
                throne.get(0).animateTo(null, new AxisAngle4f(tilt, 1, 0, 0),
                        new Vector3f(1.4f, 0.8f, 1.4f), 60);
                throne.get(1).animateTo(null, new AxisAngle4f(tilt, 1, 0, 0),
                        new Vector3f(0.4f, 3.0f, 1.4f), 60);
            }

            // Particles
            if (tick % 4 == 0) {
                for (BlockDisplayHandle h : ash) {
                    Location loc = h.entity().getLocation().clone().add(0, 0.4, 0);
                    DisplayBuilder.dustParticles(loc, 1, 0.5, 130, 130, 130, 1.1f);
                }
            }
            if (tick % 6 == 0) {
                for (BlockDisplayHandle h : sconces) {
                    Location loc = h.entity().getLocation().clone().add(0, 0.5, 0);
                    w.spawnParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.005);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.15, 0.15, 0.15, 0.005);
                }
            }
            if (tick % 10 == 0) {
                w.spawnParticle(Particle.SOUL, getCenter().clone().add(0, 1.5, 0),
                        1, 4.0, 1.5, 4.0, 0.005);
            }
            if (tick % 100 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_NETHER_WASTES_MOOD, 0.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new AshenThroneRoom(plugin); }
    }

    // ================================================================
    // 66. VOID MIRROR LAKE — active hazard. ~25 obsidian displays form
    //     irregular lake on floor with crying obsidian shore.
    // ================================================================
    public static class VoidMirrorLake extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> surface = new ArrayList<>();
        private final List<BlockDisplayHandle> shore = new ArrayList<>();

        public VoidMirrorLake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_mirror_lake", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(400);
            config.setCooldownTicks(480);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.4f);

            // Irregular organic blob — 18 obsidian surface tiles
            double[][] blob = {
                    {0,0}, {1,0}, {-1,0}, {0,1}, {0,-1},
                    {2,0.5}, {-2,0.5}, {1,1.5}, {-1,1.5}, {1,-1.5}, {-1,-1.5},
                    {2.5,-0.5}, {-2.5,-0.5}, {0,2}, {0,-2}, {3,0.2},
                    {-3,-0.2}, {0.5,2.5}
            };
            for (double[] o : blob) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(o[0], 0.05, o[1]), Material.OBSIDIAN);
                h.scale(1.0f, 0.1f, 1.0f).glow(20, 0, 40).interpolation(20, 0);
                surface.add(h);
            }
            // Deeper bed — 3 blackstone in deeper center
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add((Math.random()-0.5)*2, 0.02, (Math.random()-0.5)*2),
                        Material.BLACKSTONE);
                h.scale(0.8f, 0.05f, 0.8f).interpolation(20, 0);
                surface.add(h);
            }

            // Shoreline — 8 crying obsidian border around blob edge
            double[][] shoreEdge = {
                    {3.5, 0}, {-3.5, 0}, {0, 3.0}, {0, -3.0},
                    {2.8, 2.0}, {-2.8, 2.0}, {2.8, -2.0}, {-2.8, -2.0}
            };
            for (double[] o : shoreEdge) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(o[0], 0.1, o[1]), Material.CRYING_OBSIDIAN);
                h.scale(0.7f, 0.3f, 0.7f).glow(90, 0, 130).interpolation(20, 0);
                shore.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Surface ripples — concentric scale pulses from a random center periodically
            if (tick % 30 == 0) {
                int rippleIdx = (int) (Math.random() * surface.size());
                Location rippleCenter = surface.get(rippleIdx).entity().getLocation();
                for (BlockDisplayHandle h : surface) {
                    double d = h.entity().getLocation().distance(rippleCenter);
                    int delay = (int) (d * 4);
                    float s = 1.0f + (float) Math.sin(tick * 0.1 - d) * 0.2f;
                    h.interpolation(15, delay);
                    h.animateTo(null, new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, 0.1f, s), 15);
                }
            }

            // Shore drips inward
            if (tick % 25 == 0) {
                for (BlockDisplayHandle h : shore) {
                    float pulse = 0.7f + (float) Math.sin(tick * 0.08) * 0.1f;
                    h.animateTo(null, new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, 0.3f, pulse), 25);
                }
            }

            // End void particles rising
            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 6;
                    double oz = (Math.random() - 0.5) * 5;
                    Location loc = getCenter().clone().add(ox, 0.3 + Math.random() * 1.5, oz);
                    w.spawnParticle(Particle.REVERSE_PORTAL, loc, 1, 0.2, 0.4, 0.2, 0.01);
                }
            }
            if (tick % 8 == 0) {
                for (BlockDisplayHandle h : shore) {
                    Location loc = h.entity().getLocation().clone().add(0.3, 0.2, 0.3);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, loc, 2, 0.2, 0.3, 0.2, 0);
                    DisplayBuilder.dustParticles(loc, 1, 0.3, 90, 0, 130, 1.2f);
                }
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new VoidMirrorLake(plugin); }
    }

    // ================================================================
    // 67. NIGHTMARE CAROUSEL — active hazard. Carved base + pole + 6
    //     canopy arms + panels + glass trim. ~30 displays. Rotates.
    // ================================================================
    public static class NightmareCarousel extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<BlockDisplayHandle> pole = new ArrayList<>();
        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> panels = new ArrayList<>();
        private final List<BlockDisplayHandle> trim = new ArrayList<>();
        private int direction = 1;

        public NightmareCarousel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_carousel", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(420);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 0.5f);

            // Base — 4x4x2 carved blackstone (compressed: 8 displays in a 4x4 ring at y=0 and y=1)
            for (int dx = -2; dx <= 1; dx++) {
                for (int dz = -2; dz <= 1; dz++) {
                    if (dx == -2 || dx == 1 || dz == -2 || dz == 1) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(dx + 0.5, 0, dz + 0.5),
                                Material.POLISHED_BLACKSTONE_BRICKS);
                        h.scale(1.0f, 2.0f, 1.0f).interpolation(15, 0);
                        base.add(h);
                    }
                }
            }

            // Pole — 8 tall blackstone center (1x1)
            for (int y = 2; y < 10; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.BLACKSTONE);
                h.scale(0.4f, 1.0f, 0.4f).interpolation(15, 0);
                pole.add(h);
            }

            // 6 canopy arms (bone block) radiating from top of pole
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location armLoc = center.clone().add(Math.cos(a) * 2.5, 9.5, Math.sin(a) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(armLoc, Material.BONE_BLOCK);
                h.scale(2.0f, 0.3f, 0.4f).glow(240, 230, 200).interpolation(8, 0);
                arms.add(h);
            }

            // 6 calcite panels hanging at arm tips
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location pLoc = center.clone().add(Math.cos(a) * 4.5, 8.0, Math.sin(a) * 4.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(pLoc, Material.CALCITE);
                h.scale(0.8f, 1.5f, 0.2f).glow(240, 230, 200).interpolation(8, 0);
                panels.add(h);
            }

            // 6 red glass accents on panels
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location pLoc = center.clone().add(Math.cos(a) * 4.5, 7.2, Math.sin(a) * 4.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(pLoc, Material.RED_STAINED_GLASS);
                h.scale(0.4f, 0.4f, 0.2f).glow(180, 30, 30).brightness(15, 15).interpolation(8, 0);
                trim.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Reverse direction abruptly every ~150 ticks
            if (tick > 0 && tick % 150 == 0) {
                direction = -direction;
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_ANVIL_LAND, 0.6f, 0.3f);
            }

            // Top rotates one way, base opposite
            if (tick % 4 == 0) {
                float topAng = direction * tick * 0.06f;
                float botAng = -direction * tick * 0.04f;

                // Arms rotate around carousel
                for (int i = 0; i < arms.size(); i++) {
                    double a = Math.PI * 2 * i / 6 + topAng;
                    float tx = (float) (Math.cos(a) * 2.5);
                    float tz = (float) (Math.sin(a) * 2.5);
                    arms.get(i).animateTo(
                            new Vector3f(tx - 1.0f, 9.5f, tz - 0.2f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(2.0f, 0.3f, 0.4f), 4);
                }
                // Panels follow with their own independent fast spin
                for (int i = 0; i < panels.size(); i++) {
                    double a = Math.PI * 2 * i / 6 + topAng;
                    float tx = (float) (Math.cos(a) * 4.5);
                    float tz = (float) (Math.sin(a) * 4.5);
                    panels.get(i).animateTo(
                            new Vector3f(tx - 0.4f, 8.0f, tz - 0.1f),
                            new AxisAngle4f(tick * 0.3f * direction, 0, 1, 0),
                            new Vector3f(0.8f, 1.5f, 0.2f), 4);
                }
                for (int i = 0; i < trim.size(); i++) {
                    double a = Math.PI * 2 * i / 6 + topAng;
                    float tx = (float) (Math.cos(a) * 4.5);
                    float tz = (float) (Math.sin(a) * 4.5);
                    trim.get(i).animateTo(
                            new Vector3f(tx - 0.2f, 7.2f, tz - 0.1f),
                            new AxisAngle4f(tick * 0.3f * direction, 0, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.2f), 4);
                }
                // Base rotates opposite
                for (int i = 0; i < base.size(); i++) {
                    base.get(i).animateTo(null,
                            new AxisAngle4f(botAng, 0, 1, 0),
                            new Vector3f(1.0f, 2.0f, 1.0f), 4);
                }
            }

            // Sparks from spinning panels
            if (tick % 3 == 0) {
                for (BlockDisplayHandle p : panels) {
                    Location loc = p.entity().getLocation();
                    w.spawnParticle(Particle.CRIT, loc, 2, 0.2, 0.2, 0.2, 0.05);
                    DisplayBuilder.dustParticles(loc, 1, 0.2, 180, 30, 30, 1.0f);
                }
            }
            // Eerie glow under canopy
            if (tick % 6 == 0) {
                Location glow = getCenter().clone().add(0, 8, 0);
                DisplayBuilder.dustParticles(glow, 4, 3.0, 180, 30, 30, 1.5f);
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f,
                        0.5f + (float) Math.sin(tick * 0.02) * 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new NightmareCarousel(plugin); }
    }

    // ================================================================
    // 68. BONEYARD ORCHARD — atmosphere/hazard. 4 dead trees with bone
    //     branches & calcite fruit. Fruit drops occasionally. ~32 displays.
    // ================================================================
    public static class BoneyardOrchard extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> trunks = new ArrayList<>();
        private final List<BlockDisplayHandle> branches = new ArrayList<>();
        private final List<BlockDisplayHandle> fruit = new ArrayList<>();

        public BoneyardOrchard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("boneyard_orchard", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(420);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BONE_BLOCK_BREAK, 0.8f, 0.4f);

            double[][] treePositions = {
                    {5, 0, 4}, {-5, 0, 4}, {5, 0, -4}, {-5, 0, -4}
            };
            for (double[] pos : treePositions) {
                Location base = center.clone().add(pos[0], pos[1], pos[2]);
                // Trunk — 5 blocks tall
                for (int y = 0; y < 5; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            base.clone().add(0, y, 0), Material.BLACKSTONE);
                    h.scale(0.6f, 1.0f, 0.6f).interpolation(20, 0);
                    trunks.add(h);
                }
                // Branches — 3 arms per tree
                for (int b = 0; b < 3; b++) {
                    double a = Math.PI * 2 * b / 3;
                    Location bLoc = base.clone().add(Math.cos(a) * 1.0, 4.5, Math.sin(a) * 1.0);
                    BlockDisplayHandle bh = displayBuilder.spawnBlock(bLoc, Material.BONE_BLOCK);
                    bh.scale(1.5f, 0.3f, 0.3f).glow(240, 230, 200).interpolation(15, 0);
                    branches.add(bh);
                    // Fruit at branch end
                    Location fLoc = base.clone().add(Math.cos(a) * 1.8, 4.2, Math.sin(a) * 1.8);
                    BlockDisplayHandle fh = displayBuilder.spawnBlock(fLoc, Material.CALCITE);
                    fh.scale(0.3f, 0.3f, 0.3f).glow(240, 230, 200).interpolation(15, 0);
                    fruit.add(fh);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Tree sway — slow X rotation on trunks
            if (tick % 25 == 0) {
                float ang = (float) Math.toRadians(3.0 * Math.sin(tick * 0.03));
                for (BlockDisplayHandle t : trunks) {
                    t.animateTo(null, new AxisAngle4f(ang, 1, 0, 0),
                            new Vector3f(0.6f, 1.0f, 0.6f), 25);
                }
            }
            // Branch arms move independently (delayed)
            if (tick % 30 == 0) {
                for (int i = 0; i < branches.size(); i++) {
                    float ang = (float) Math.toRadians(4.0 * Math.sin(tick * 0.04 + i * 0.3));
                    branches.get(i).animateTo(null, new AxisAngle4f(ang, 0, 0, 1),
                            new Vector3f(1.5f, 0.3f, 0.3f), 30);
                }
            }
            // Fruit slowly spins
            if (tick % 6 == 0) {
                for (int i = 0; i < fruit.size(); i++) {
                    fruit.get(i).animateTo(null,
                            new AxisAngle4f(tick * 0.05f + i * 0.2f, 0, 1, 0),
                            new Vector3f(0.3f, 0.3f, 0.3f), 6);
                }
            }

            // Drop fruit occasionally
            if (tick % 70 == 0 && !fruit.isEmpty()) {
                int idx = (int) (Math.random() * fruit.size());
                BlockDisplayHandle f = fruit.get(idx);
                Location at = f.entity().getLocation();
                f.animateTo(new Vector3f(0, -4f, 0),
                        new AxisAngle4f((float) Math.random() * 6, 1, 0, 1),
                        new Vector3f(0.3f, 0.3f, 0.3f), 20);
                // Schedule small AoE impact via triggerImpactDamage at landing — use scheduled effect
                Location landing = at.clone().add(0, -4, 0);
                w.spawnParticle(Particle.BLOCK, landing, 12, 0.5, 0.1, 0.5, 0.05,
                        Material.CALCITE.createBlockData());
                DisplayBuilder.playSound(landing, Sound.BLOCK_BONE_BLOCK_BREAK, 0.7f, 0.5f);
                DisplayBuilder.dustParticles(landing, 4, 0.6, 240, 230, 200, 1.3f);
                triggerImpactDamage(landing);
            }

            // Particles
            if (tick % 6 == 0) {
                for (BlockDisplayHandle t : trunks) {
                    if (Math.random() > 0.85) {
                        Location loc = t.entity().getLocation().clone().add(0, 0.2, 0);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.3, 0.1, 0.3, 0.005);
                    }
                }
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BAMBOO_HIT, 0.4f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new BoneyardOrchard(plugin); }
    }

    // ================================================================
    // 69. THE CRACKED COSMOS — atmosphere ceiling. 32+ displays of
    //     obsidian panels, end stone star chunks, calcite stars, crying
    //     obsidian crack lines. Drift apart over time.
    // ================================================================
    public static class CrackedCosmos extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> panels = new ArrayList<>();
        private final List<BlockDisplayHandle> chunks = new ArrayList<>();
        private final List<BlockDisplayHandle> stars = new ArrayList<>();
        private final List<BlockDisplayHandle> cracks = new ArrayList<>();

        public CrackedCosmos(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cracked_cosmos", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(500);
            config.setCooldownTicks(560);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 0.4f);

            // Obsidian panels — 12 in 3x4 grid overhead
            for (int dx = -2; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(dx * 2.5, 14, dz * 2.5),
                            Material.OBSIDIAN);
                    h.scale(2.0f, 0.3f, 2.0f).glow(20, 0, 40).interpolation(20, 0);
                    panels.add(h);
                }
            }
            // End stone star chunks — 8
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = center.clone().add(Math.cos(a) * 4, 13.5, Math.sin(a) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.END_STONE_BRICKS);
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 200).interpolation(15, 0);
                chunks.add(h);
            }
            // Calcite bright stars — 8 scattered
            for (int i = 0; i < 8; i++) {
                double ox = (Math.random() - 0.5) * 8;
                double oz = (Math.random() - 0.5) * 8;
                Location p = center.clone().add(ox, 13.8, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CALCITE);
                h.scale(0.2f, 0.2f, 0.2f).glow(255, 255, 255).brightness(15, 15).interpolation(10, 0);
                stars.add(h);
            }
            // Crying obsidian crack lines — 5
            for (int i = 0; i < 5; i++) {
                double a = Math.PI * 2 * i / 5;
                Location p = center.clone().add(Math.cos(a) * 2, 13.6, Math.sin(a) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(2.5f, 0.15f, 0.15f).glow(90, 0, 130).interpolation(20, 0);
                h.animateTo(null, new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(2.5f, 0.15f, 0.15f), 0);
                cracks.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Panels drift apart slowly
            if (tick % 60 == 0) {
                float spread = 1.0f + (tick / 600f);
                int idx = 0;
                for (int dx = -2; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (idx >= panels.size()) break;
                        panels.get(idx).animateTo(
                                new Vector3f(dx * 2.5f * (spread - 1f), 0,
                                        dz * 2.5f * (spread - 1f)),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(2.0f, 0.3f, 2.0f), 60);
                        idx++;
                    }
                }
            }

            // Star chunks rotate independently
            if (tick % 8 == 0) {
                for (int i = 0; i < chunks.size(); i++) {
                    chunks.get(i).animateTo(null,
                            new AxisAngle4f(tick * 0.1f + i * 0.4f, 1, 1, 0),
                            new Vector3f(0.5f, 0.5f, 0.5f), 8);
                }
            }
            // Cracks slowly widen
            if (tick % 80 == 0) {
                float widen = 0.15f + (tick / 1500f);
                for (int i = 0; i < cracks.size(); i++) {
                    double a = Math.PI * 2 * i / 5;
                    cracks.get(i).animateTo(null,
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(2.5f, widen, widen), 80);
                }
            }
            // Stars blink
            if (tick % 15 == 0 && !stars.isEmpty()) {
                int idx = (int) (Math.random() * stars.size());
                BlockDisplayHandle s = stars.get(idx);
                s.animateTo(null, new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.01f, 0.01f, 0.01f), 4);
                // Rebound back
                final BlockDisplayHandle rebound = s;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (rebound.entity() != null && !rebound.entity().isDead()) {
                        rebound.animateTo(null, new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.2f, 0.2f, 0.2f), 6);
                    }
                }, 5L);
            }
            // One large crack pulses bright then opens wider every 200 ticks
            if (tick % 200 == 100 && !cracks.isEmpty()) {
                BlockDisplayHandle big = cracks.get(0);
                big.animateTo(null, new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(3.5f, 0.4f, 0.4f), 30);
            }

            // Particles
            if (tick % 4 == 0) {
                for (int i = 0; i < 5; i++) {
                    double ox = (Math.random() - 0.5) * 10;
                    double oz = (Math.random() - 0.5) * 10;
                    Location loc = getCenter().clone().add(ox, 12 + Math.random() * 2, oz);
                    w.spawnParticle(Particle.REVERSE_PORTAL, loc, 1, 0.3, 0.5, 0.3, 0.005);
                }
            }
            if (tick % 6 == 0) {
                for (BlockDisplayHandle s : stars) {
                    Location loc = s.entity().getLocation();
                    DisplayBuilder.dustParticles(loc, 1, 0.2, 255, 255, 255, 1.5f);
                }
            }
            if (tick % 10 == 0) {
                for (BlockDisplayHandle c : cracks) {
                    Location loc = c.entity().getLocation();
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, loc, 1, 0.5, 0.2, 0.5, 0);
                }
            }
            if (tick % 100 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CrackedCosmos(plugin); }
    }

    // ================================================================
    // 70. THE WELL OF SCREAMS — active hazard. Bucket lowers, then
    //     yanks up rapidly. ~24 displays.
    // ================================================================
    public static class WellOfScreams extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> wallRing = new ArrayList<>();
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<BlockDisplayHandle> rope = new ArrayList<>();
        private BlockDisplayHandle bucket;
        private int phase = 0; // 0 lowering, 1 yanking, 2 dripping
        private int phaseStart = 0;

        public WellOfScreams(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("well_of_screams", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.5);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(420);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 0.9f, 0.4f);

            // Ring wall — 12 wall displays in a circle, 3 tall = 12*3 displays. Use 1 stretched
            // display per "column" for budget: 12 columns scaled tall.
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                Location p = center.clone().add(Math.cos(a) * 2.0, 1.5, Math.sin(a) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(0.7f, 3.0f, 0.7f).interpolation(15, 0);
                h.animateTo(null, new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.7f, 3.0f, 0.7f), 0);
                wallRing.add(h);
            }
            // Rim cap — 8 deepslate tile pieces around top
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = center.clone().add(Math.cos(a) * 2.2, 3.1, Math.sin(a) * 2.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_TILES);
                h.scale(0.7f, 0.25f, 0.7f).interpolation(15, 0);
                rim.add(h);
            }
            // Rope — 5 crying obsidian segments dangling from above center
            for (int i = 0; i < 5; i++) {
                Location p = center.clone().add(0, 5 - i * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.12f, 0.5f, 0.12f).glow(90, 0, 130).interpolation(8, 0);
                rope.add(h);
            }
            // Bucket — 1 small blackstone at rope end
            bucket = displayBuilder.spawnBlock(
                    center.clone().add(0, 2.5, 0), Material.BLACKSTONE);
            bucket.scale(0.4f, 0.4f, 0.4f).interpolation(8, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            int local = tick - phaseStart;
            if (phase == 0 && local >= 80) {
                // Bucket lowers from y=2.5 to y=-3.0 over 80 ticks → reached bottom, yank
                phase = 1;
                phaseStart = tick;
                // Yank up rapidly — bucket goes from -3 back to 5 over 12 ticks
                bucket.animateTo(new Vector3f(-0.2f, 5f - 2.5f + 5f, -0.2f),
                        new AxisAngle4f((float) (Math.PI), 1, 0, 1),
                        new Vector3f(0.4f, 0.4f, 0.4f), 12);
                for (int i = 0; i < rope.size(); i++) {
                    rope.get(i).animateTo(
                            new Vector3f(-0.06f, 5f + i * 0.5f - (5f - i * 0.5f), -0.06f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.12f, 0.5f, 0.12f), 12);
                }
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_HURT, 1.2f, 0.3f);
                triggerImpactDamage(getCenter().clone().add(0, 2, 0));
                w.spawnParticle(Particle.SCULK_SOUL, getCenter().clone().add(0, 1, 0),
                        20, 1.2, 1.5, 1.2, 0.05);
            } else if (phase == 1 && local >= 14) {
                // Bucket shakes and drips for 30 ticks
                phase = 2;
                phaseStart = tick;
            } else if (phase == 2 && local >= 30) {
                // Reset — lower again
                phase = 0;
                phaseStart = tick;
                bucket.animateTo(new Vector3f(-0.2f, -3f, -0.2f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(0.4f, 0.4f, 0.4f), 80);
                for (int i = 0; i < rope.size(); i++) {
                    rope.get(i).animateTo(
                            new Vector3f(-0.06f, -3f + i * 0.5f, -0.06f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.12f, 0.8f, 0.12f), 80);
                }
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.3f);
            } else if (phase == 0 && local == 0) {
                // Initial lower
                bucket.animateTo(new Vector3f(-0.2f, -3f, -0.2f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(0.4f, 0.4f, 0.4f), 80);
                for (int i = 0; i < rope.size(); i++) {
                    rope.get(i).animateTo(
                            new Vector3f(-0.06f, -3f + i * 0.5f, -0.06f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.12f, 0.8f, 0.12f), 80);
                }
            }

            // Phase 2 — bucket shakes and drips
            if (phase == 2 && tick % 2 == 0) {
                float jx = (float) ((Math.random() - 0.5) * 0.2);
                float jz = (float) ((Math.random() - 0.5) * 0.2);
                bucket.animateTo(new Vector3f(jx - 0.2f, 5f - 2.5f + 5f + jx, jz - 0.2f),
                        new AxisAngle4f((float) (Math.random() * Math.PI * 2), 1, 0, 1),
                        new Vector3f(0.4f, 0.4f, 0.4f), 2);
                Location bLoc = bucket.entity().getLocation();
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, bLoc, 2, 0.3, 0.2, 0.3, 0);
                DisplayBuilder.dustParticles(bLoc, 1, 0.3, 90, 0, 130, 1.2f);
            }

            // Particles — dark soul rising from well opening
            if (tick % 4 == 0) {
                Location open = getCenter().clone().add(0, 3, 0);
                w.spawnParticle(Particle.SCULK_SOUL, open, 2, 1.2, 0.3, 1.2, 0.01);
                w.spawnParticle(Particle.SOUL, open, 1, 1.0, 0.5, 1.0, 0.005);
            }
            if (tick % 6 == 0) {
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 2, 0),
                        2, 1.5, 50, 220, 200, 1.4f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.6f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new WellOfScreams(plugin); }
    }
}
