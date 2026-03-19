package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream — GROUND TERROR ATTACKS
 * 13 ground-erupting BlockDisplay attacks featuring demon hands,
 * hellfire cracks, soul geysers, and erupting pentagrams.
 *
 * All emerge from below ground level. Materials: soul_sand, soul_soil,
 * bone_block, netherrack, magma_block, blackstone, deepslate
 */
public final class GroundTerrors {

    private GroundTerrors() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DemonHandEruption(plugin));
        registry.register(new HellfireCrack(plugin));
        registry.register(new BoneField(plugin));
        registry.register(new SoulGeyser(plugin));
        registry.register(new NightmareRoots(plugin));
        registry.register(new MagmaPool(plugin));
        registry.register(new TombstoneRise(plugin));
        registry.register(new HellfireVent(plugin));
        registry.register(new AbyssalMaw(plugin));
        registry.register(new CrimsonThorn(plugin));
        registry.register(new NightmareGrave(plugin));
        registry.register(new SoulVortex(plugin));
        registry.register(new EruptingPentagram(plugin));
    }

    // ================================================================
    // 1. DEMON HAND ERUPTION — Giant hand (12 blocks): palm + 5
    //    fingers burst from the ground, grasping upward. Fingers
    //    splay then slowly close.
    // ================================================================
    public static class DemonHandEruption extends BlockDisplayAttack {
        private BlockDisplayHandle palm1, palm2;
        private final List<BlockDisplayHandle> fingers = new ArrayList<>();
        private float riseProgress = 0;
        private float gripProgress = 0;

        public DemonHandEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demon_hand_eruption", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(60.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            palm1 = spawnP(center, -0.8, -3, 0, Material.BLACKSTONE, 2.0f, 1.0f, 2.5f);
            palm2 = spawnP(center, 0.8, -3, 0, Material.BLACKSTONE, 2.0f, 1.0f, 2.5f);

            double[] fingerX = {-1.5, -0.75, 0, 0.75, 1.5};
            for (int f = 0; f < 5; f++) {
                fingers.add(spawnP(center, fingerX[f], -3, 1.0, Material.COAL_BLOCK, 0.5f, 2.5f, 0.5f));
                fingers.add(spawnP(center, fingerX[f], -3, 1.0, Material.OBSIDIAN, 0.35f, 1.5f, 0.35f));
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 0.4f);
            c(center).spawnParticle(Particle.BLOCK, center, 20, 1.5, 0.5, 1.5, 0.1,
                    Material.SOUL_SOIL.createBlockData());
        }

        private World c(Location l) { return l.getWorld(); }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(40, 20, 40).interpolation(4, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise phase
            if (riseProgress < 1.0f) {
                riseProgress += 0.012f;
            } else if (gripProgress < 1.0f) {
                gripProgress += 0.005f;
            }

            float yOff = -3.0f + riseProgress * 4.0f;
            palm1.entity().teleport(c.clone().add(-0.8, yOff, 0));
            palm2.entity().teleport(c.clone().add(0.8, yOff, 0));

            double[] fingerX = {-1.5, -0.75, 0, 0.75, 1.5};
            for (int f = 0; f < 5; f++) {
                float curl = gripProgress * 1.0f;
                double fz = 1.0 - Math.sin(curl) * 2.0;
                double fy = yOff + 1.5 + Math.cos(curl) * 2.0;
                double tipFz = fz - Math.sin(curl * 1.3) * 1.2;
                double tipFy = fy + Math.cos(curl * 1.3) * 1.5 - 0.3;
                double xShift = fingerX[f] * (1.0 - gripProgress * 0.4);

                fingers.get(f * 2).entity().teleport(c.clone().add(xShift, fy, fz));
                fingers.get(f * 2).rotate(-curl, 1, 0, 0);
                fingers.get(f * 2 + 1).entity().teleport(c.clone().add(xShift, tipFy, tipFz));
                fingers.get(f * 2 + 1).rotate(-curl * 1.3f, 1, 0, 0);
            }

            // Rising particles
            if (riseProgress < 1.0f && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK, c, 8, 1.5, 0.3, 1.5, 0.05,
                        Material.SOUL_SOIL.createBlockData());
            }

            // Dark energy when gripping
            if (gripProgress > 0.3f && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, yOff + 2, 0), 5, 1.0, 40, 20, 40, 1.5f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DemonHandEruption(plugin); }
    }

    // ================================================================
    // 2. HELLFIRE CRACK — Line of 12 magma blocks splits open in the
    //    ground, widening over time with fire erupting from the gap.
    // ================================================================
    public static class HellfireCrack extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftEdge = new ArrayList<>();
        private final List<BlockDisplayHandle> rightEdge = new ArrayList<>();
        private float crackWidth = 0.1f;

        public HellfireCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_crack", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 6; i++) {
                double z = (i - 2.5) * 2.0;
                leftEdge.add(spawnP(center, -crackWidth, 0, z, Material.NETHERRACK, 0.5f, 0.8f, 2.0f));
                rightEdge.add(spawnP(center, crackWidth, 0, z, Material.NETHERRACK, 0.5f, 0.8f, 2.0f));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(200, 80, 20).interpolation(3, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (crackWidth < 2.0f) crackWidth += 0.01f;

            for (int i = 0; i < 6; i++) {
                double z = (i - 2.5) * 2.0;
                leftEdge.get(i).entity().teleport(c.clone().add(-crackWidth, 0, z));
                rightEdge.get(i).entity().teleport(c.clone().add(crackWidth, 0, z));
            }

            // Fire from crack
            if (ticksAlive % 3 == 0) {
                double z = Math.random() * 10 - 5;
                double x = (Math.random() - 0.5) * crackWidth;
                c.getWorld().spawnParticle(Particle.FLAME,
                        c.clone().add(x, 0.5, z), 5, 0.2, 0.5, 0.2, 0.03);
                c.getWorld().spawnParticle(Particle.LAVA,
                        c.clone().add(x, 0.3, z), 2, 0.1, 0.1, 0.1, 0);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new HellfireCrack(plugin); }
    }

    // ================================================================
    // 3. BONE FIELD — 14 bone blocks erupt as spikes from the ground
    //    at random positions around the player. Rise at staggered
    //    intervals like a minefield going off.
    // ================================================================
    public static class BoneField extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<Float> heights = new ArrayList<>();
        private final List<Float> targetHeights = new ArrayList<>();
        private final List<Double> xOff = new ArrayList<>();
        private final List<Double> zOff = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();

        public BoneField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bone_field", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 14; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.0 + Math.random() * 6.0;
                xOff.add(Math.cos(angle) * dist);
                zOff.add(Math.sin(angle) * dist);
                heights.add(-1.5f);
                targetHeights.add(2.0f + (float)(Math.random() * 3.0));
                delays.add((int)(Math.random() * 60));

                BlockDisplayHandle spike = displayBuilder.spawnBlock(
                        center.clone().add(xOff.get(i), -1.5, zOff.get(i)), Material.BONE_BLOCK);
                float width = 0.4f + (float)(Math.random() * 0.4);
                spike.scale(width, 3.0f, width).glow(200, 190, 170).interpolation(3, 0);
                // Slight random tilt for natural look
                spike.rotate((float)(Math.random() * 0.2 - 0.1), 0, 0, 1);
                spikes.add(spike);
                spawnedEntities.add(spike.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SKELETON_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < spikes.size(); i++) {
                if (ticksAlive < delays.get(i)) continue;

                float h = heights.get(i);
                float target = targetHeights.get(i);
                if (h < target) {
                    h += 0.15f;
                    if (h > target) h = target;
                    heights.set(i, h);

                    // Eruption effect on first rise
                    if (ticksAlive == delays.get(i) + 1) {
                        Location spikeLoc = c.clone().add(xOff.get(i), 0, zOff.get(i));
                        c.getWorld().spawnParticle(Particle.BLOCK, spikeLoc, 8, 0.3, 0.3, 0.3, 0.05,
                                Material.BONE_BLOCK.createBlockData());
                        DisplayBuilder.playSound(spikeLoc, Sound.BLOCK_BONE_BLOCK_BREAK, 0.5f, 0.6f);
                    }
                }

                spikes.get(i).entity().teleport(c.clone().add(xOff.get(i), h - 1.5, zOff.get(i)));
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SKELETON_STEP, 0.4f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BoneField(plugin); }
    }

    // ================================================================
    // 4. SOUL GEYSER — Central vent (3 blocks) erupts soul sand
    //    fragments (10) that shoot upward in a column then float down.
    // ================================================================
    public static class SoulGeyser extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> vent = new ArrayList<>();
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private final List<Float> fragY = new ArrayList<>();
        private final List<Float> fragSpeed = new ArrayList<>();
        private final List<Double> fragX = new ArrayList<>();
        private final List<Double> fragZ = new ArrayList<>();
        private boolean erupting = false;

        public SoulGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_geyser", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Vent base
            vent.add(spawnP(center, 0, 0, 0, Material.SOUL_SOIL, 2.0f, 0.5f, 2.0f));
            vent.add(spawnP(center, 0, 0.5, 0, Material.SOUL_SAND, 1.5f, 0.5f, 1.5f));
            vent.add(spawnP(center, 0, 1.0, 0, Material.SOUL_SAND, 1.0f, 0.5f, 1.0f));

            for (int i = 0; i < 10; i++) {
                BlockDisplayHandle frag = displayBuilder.spawnBlock(center, Material.SOUL_SAND);
                frag.scale(0.01f, 0.01f, 0.01f).glow(80, 120, 140).interpolation(2, 0);
                fragments.add(frag);
                spawnedEntities.add(frag.entity());
                fragY.add(0.0f);
                fragSpeed.add(0.0f);
                fragX.add(0.0);
                fragZ.add(0.0);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(80, 120, 140);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (ticksAlive == 30) erupting = true;

            if (erupting) {
                // Launch fragments periodically
                for (int i = 0; i < fragments.size(); i++) {
                    int launchTick = 30 + i * 8;
                    if (ticksAlive == launchTick) {
                        float s = 0.5f + (float)(Math.random() * 0.5);
                        fragments.get(i).scale(s, s, s);
                        fragSpeed.set(i, 0.4f + (float)(Math.random() * 0.3));
                        fragX.set(i, (Math.random() - 0.5) * 0.1);
                        fragZ.set(i, (Math.random() - 0.5) * 0.1);
                    }

                    if (ticksAlive > launchTick) {
                        float speed = fragSpeed.get(i) - 0.015f; // Gravity
                        fragSpeed.set(i, speed);
                        float y = fragY.get(i) + speed;
                        if (y < 0) y = 0;
                        fragY.set(i, y);

                        double spreadX = fragX.get(i) * (ticksAlive - launchTick);
                        double spreadZ = fragZ.get(i) * (ticksAlive - launchTick);
                        fragments.get(i).entity().teleport(c.clone().add(spreadX, y + 1.5, spreadZ));
                    }
                }

                // Geyser particles
                if (ticksAlive % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.SOUL, c.clone().add(0, 1.5, 0),
                            5, 0.3, 1, 0.3, 0.05);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 2, 0),
                            3, 0.2, 0.5, 0.2, 0.03);
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SOUL_SAND_PLACE, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulGeyser(plugin); }
    }

    // ================================================================
    // 5. NIGHTMARE ROOTS — 12 crimson/warped root-like blocks snake
    //    out from a center point along the ground, spreading outward.
    // ================================================================
    public static class NightmareRoots extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> roots = new ArrayList<>();
        private final List<Double> angles = new ArrayList<>();
        private final List<Float> lengths = new ArrayList<>();

        public NightmareRoots(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_roots", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12 + (Math.random() * 0.3 - 0.15);
                angles.add(angle);
                lengths.add(0.0f);

                Material mat = (i % 3 == 0) ? Material.CRIMSON_STEM
                        : (i % 3 == 1) ? Material.WARPED_STEM
                        : Material.CRIMSON_HYPHAE;
                BlockDisplayHandle root = displayBuilder.spawnBlock(center, mat);
                root.scale(0.4f, 0.3f, 0.01f).glow(100, 40, 40).interpolation(3, 0);
                roots.add(root);
                spawnedEntities.add(root.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_VINE_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < roots.size(); i++) {
                float len = lengths.get(i);
                float maxLen = 3.0f + i * 0.5f;
                int startDelay = i * 5;

                if (ticksAlive > startDelay && len < maxLen) {
                    len += 0.08f;
                    lengths.set(i, len);
                }

                double angle = angles.get(i);
                double x = Math.cos(angle) * len;
                double z = Math.sin(angle) * len;
                // Slight wave
                double y = 0.2 + Math.sin(len * 1.5) * 0.2;

                roots.get(i).entity().teleport(c.clone().add(x, y, z));
                roots.get(i).scale(0.4f, 0.3f, len * 0.5f + 0.1f);
                roots.get(i).rotate((float) angle, 0, 1, 0);

                // Growth particles at tips
                if (ticksAlive % 6 == 0 && len < maxLen && len > 0) {
                    Location tipLoc = c.clone().add(x, y, z);
                    c.getWorld().spawnParticle(Particle.CRIMSON_SPORE, tipLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_VINE_STEP, 0.4f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareRoots(plugin); }
    }

    // ================================================================
    // 6. MAGMA POOL — 10 magma blocks spread outward from center at
    //    ground level, forming an expanding lava pool.
    // ================================================================
    public static class MagmaPool extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> poolBlocks = new ArrayList<>();
        private float poolRadius = 0.5f;

        public MagmaPool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_pool", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                Material mat = (i % 2 == 0) ? Material.MAGMA_BLOCK : Material.ORANGE_CONCRETE;
                BlockDisplayHandle b = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0), mat);
                b.scale(0.01f, 0.2f, 0.01f).glow(255, 120, 20).interpolation(5, 0);
                poolBlocks.add(b);
                spawnedEntities.add(b.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (poolRadius < 6.0f) poolRadius += 0.03f;

            for (int i = 0; i < poolBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / poolBlocks.size();
                double r = poolRadius * (0.5 + 0.5 * Math.sin(i * 1.3 + ticksAlive * 0.02));
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                poolBlocks.get(i).entity().teleport(c.clone().add(x, 0.1, z));
                poolBlocks.get(i).scale(poolRadius * 0.5f, 0.2f, poolRadius * 0.5f);
            }

            // Bubbling
            if (ticksAlive % 4 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * poolRadius;
                Location bubbleLoc = c.clone().add(Math.cos(angle) * dist, 0.3, Math.sin(angle) * dist);
                c.getWorld().spawnParticle(Particle.LAVA, bubbleLoc, 2, 0.3, 0.1, 0.3, 0);
                c.getWorld().spawnParticle(Particle.FLAME, bubbleLoc, 1, 0.2, 0.2, 0.2, 0.02);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MagmaPool(plugin); }
    }

    // ================================================================
    // 7. TOMBSTONE RISE — 10 tombstone-shaped blocks (stone + sign
    //    shape) rise from ground at staggered intervals around player.
    // ================================================================
    public static class TombstoneRise extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stones = new ArrayList<>();
        private final List<Float> riseHeights = new ArrayList<>();
        private final List<Double> xOff = new ArrayList<>();
        private final List<Double> zOff = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();

        public TombstoneRise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tombstone_rise", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10 + (Math.random() * 0.3);
                double dist = 2.0 + Math.random() * 4.0;
                xOff.add(Math.cos(angle) * dist);
                zOff.add(Math.sin(angle) * dist);
                riseHeights.add(-1.5f);
                delays.add(i * 10 + (int)(Math.random() * 10));

                Material mat = (i % 3 == 0) ? Material.COBBLESTONE
                        : (i % 3 == 1) ? Material.MOSSY_COBBLESTONE
                        : Material.STONE_BRICKS;
                BlockDisplayHandle stone = displayBuilder.spawnBlock(
                        center.clone().add(xOff.get(i), -1.5, zOff.get(i)), mat);
                stone.scale(0.8f, 1.5f, 0.3f).glow(80, 80, 80).interpolation(4, 0);
                stones.add(stone);
                spawnedEntities.add(stone.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < stones.size(); i++) {
                if (ticksAlive < delays.get(i)) continue;

                float h = riseHeights.get(i);
                if (h < 0.5f) {
                    h += 0.05f;
                    riseHeights.set(i, h);

                    if (ticksAlive == delays.get(i) + 1) {
                        Location loc = c.clone().add(xOff.get(i), 0, zOff.get(i));
                        c.getWorld().spawnParticle(Particle.BLOCK, loc, 5, 0.3, 0.2, 0.3, 0.02,
                                Material.DIRT.createBlockData());
                        DisplayBuilder.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 0.3f, 0.5f);
                    }
                }

                stones.get(i).entity().teleport(c.clone().add(xOff.get(i), h, zOff.get(i)));
            }

            // Eerie mist
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,
                        c.clone().add(Math.random() * 8 - 4, 0.3, Math.random() * 8 - 4),
                        1, 0.3, 0.1, 0.3, 0.005);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TombstoneRise(plugin); }
    }

    // ================================================================
    // 8. HELLFIRE VENT — 4 vent holes (3 blocks each = 12) that
    //    periodically blast fire upward in columns.
    // ================================================================
    public static class HellfireVent extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ventBlocks = new ArrayList<>();
        private final List<double[]> ventPositions = new ArrayList<>();

        public HellfireVent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_vent", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double[][] positions = {{3, 3}, {-3, 3}, {3, -3}, {-3, -3}};

            for (double[] pos : positions) {
                ventPositions.add(pos);
                // Ring + center + grate
                ventBlocks.add(spawnP(center, pos[0], 0, pos[1], Material.BLACKSTONE, 2.0f, 0.5f, 2.0f));
                ventBlocks.add(spawnP(center, pos[0], 0.3, pos[1], Material.NETHERRACK, 1.5f, 0.3f, 1.5f));
                ventBlocks.add(spawnP(center, pos[0], 0.1, pos[1], Material.MAGMA_BLOCK, 1.0f, 0.2f, 1.0f));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.4f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(200, 80, 20);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each vent fires in sequence
            for (int v = 0; v < 4; v++) {
                int firePhase = (ticksAlive + v * 20) % 80;
                double[] pos = ventPositions.get(v);
                Location ventLoc = c.clone().add(pos[0], 0.5, pos[1]);

                // Fire blast (20 ticks out of 80)
                if (firePhase < 20) {
                    float intensity = (float) Math.sin((firePhase / 20.0) * Math.PI);
                    if (ticksAlive % 2 == 0) {
                        c.getWorld().spawnParticle(Particle.FLAME, ventLoc,
                                (int)(10 * intensity), 0.4, intensity * 3, 0.4, 0.05);
                        c.getWorld().spawnParticle(Particle.SMOKE, ventLoc,
                                3, 0.3, intensity * 2, 0.3, 0.03);
                    }
                    if (firePhase == 1) {
                        DisplayBuilder.playSound(ventLoc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.5f);
                    }
                }

                // Magma glow pulse
                int blockIdx = v * 3 + 2;
                float glow = (firePhase < 20) ? 255 : 180;
                ventBlocks.get(blockIdx).glow((int) glow, 80, 20);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new HellfireVent(plugin); }
    }

    // ================================================================
    // 9. ABYSSAL MAW — Circular ring of 12 blocks opens like a mouth
    //    in the ground, getting wider, revealing darkness below.
    // ================================================================
    public static class AbyssalMaw extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> jawBlocks = new ArrayList<>();
        private float mawRadius = 0.5f;

        public AbyssalMaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_maw", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(60.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 12; i++) {
                Material mat = (i % 3 == 0) ? Material.BLACKSTONE
                        : (i % 3 == 1) ? Material.DEEPSLATE
                        : Material.OBSIDIAN;
                BlockDisplayHandle tooth = displayBuilder.spawnBlock(center, mat);
                tooth.scale(0.8f, 1.2f, 0.8f).glow(30, 20, 40).interpolation(3, 0);
                jawBlocks.add(tooth);
                spawnedEntities.add(tooth.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (mawRadius < 5.0f) mawRadius += 0.02f;

            for (int i = 0; i < jawBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 12;
                double x = Math.cos(angle) * mawRadius;
                double z = Math.sin(angle) * mawRadius;
                // Teeth point inward
                jawBlocks.get(i).entity().teleport(c.clone().add(x, 0, z));
                jawBlocks.get(i).rotate((float) angle + (float) Math.PI, 0, 1, 0);
            }

            // Dark void in the center
            if (ticksAlive % 4 == 0 && mawRadius > 1.5f) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, -0.5, 0),
                        5, mawRadius * 0.5, 0.2, mawRadius * 0.5, 0.02);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.2, 0),
                        4, mawRadius * 0.4, 10, 5, 20, 2.0f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_ROAR, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new AbyssalMaw(plugin); }
    }

    // ================================================================
    // 10. CRIMSON THORN — 10 crimson stem "thorns" grow upward at
    //     angles around the player, creating a barbed ring.
    // ================================================================
    public static class CrimsonThorn extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> thorns = new ArrayList<>();
        private final List<Float> growths = new ArrayList<>();

        public CrimsonThorn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_thorn", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double dist = 3.0;
                Material mat = (i % 2 == 0) ? Material.CRIMSON_STEM : Material.CRIMSON_HYPHAE;
                BlockDisplayHandle thorn = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist), mat);
                float tiltAngle = 0.5f + (float)(Math.random() * 0.3);
                thorn.scale(0.3f, 0.01f, 0.3f).glow(150, 30, 30).interpolation(4, 0);
                thorn.rotate(tiltAngle, (float) Math.sin(angle), 0, -(float) Math.cos(angle));
                thorns.add(thorn);
                spawnedEntities.add(thorn.entity());
                growths.add(0.0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NETHER_SPROUTS_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < thorns.size(); i++) {
                int delay = i * 6;
                if (ticksAlive < delay) continue;

                float growth = growths.get(i);
                if (growth < 4.0f) {
                    growth += 0.06f;
                    growths.set(i, growth);
                }

                thorns.get(i).scale(0.3f, growth, 0.3f);

                // Barb particles at tip
                if (growth > 2.0f && ticksAlive % 8 == 0) {
                    double angle = (2 * Math.PI * i) / 10;
                    double dist = 3.0 - growth * 0.2;
                    Location tipLoc = c.clone().add(Math.cos(angle) * dist, growth * 0.7, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(tipLoc, 2, 0.2, 150, 30, 30, 1.0f);
                }
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrimsonThorn(plugin); }
    }

    // ================================================================
    // 11. NIGHTMARE GRAVE — Coffin shape (10 blocks) rises from
    //     underground, lid slowly opens. Dark particles pour out.
    // ================================================================
    public static class NightmareGrave extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> coffin = new ArrayList<>();
        private BlockDisplayHandle lid;
        private float riseY = -3;
        private float lidAngle = 0;

        public NightmareGrave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_grave", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, riseY, 0);

            // Box base (8 blocks)
            coffin.add(spawnP(pos, 0, 0, 0, Material.DARK_OAK_PLANKS, 1.5f, 0.3f, 3.0f)); // Bottom
            coffin.add(spawnP(pos, -0.75, 0.5, 0, Material.DARK_OAK_PLANKS, 0.15f, 1.0f, 3.0f)); // Left
            coffin.add(spawnP(pos, 0.75, 0.5, 0, Material.DARK_OAK_PLANKS, 0.15f, 1.0f, 3.0f)); // Right
            coffin.add(spawnP(pos, 0, 0.5, 1.5, Material.DARK_OAK_PLANKS, 1.5f, 1.0f, 0.15f)); // Front
            coffin.add(spawnP(pos, 0, 0.5, -1.5, Material.DARK_OAK_PLANKS, 1.5f, 1.0f, 0.15f)); // Back
            // Interior void
            coffin.add(spawnP(pos, 0, 0.2, 0, Material.BLACK_CONCRETE, 1.2f, 0.1f, 2.6f));
            // Cross decorations
            coffin.add(spawnP(pos, 0, 0.8, 0.5, Material.GOLD_BLOCK, 0.15f, 0.5f, 0.15f));
            coffin.add(spawnP(pos, 0, 0.7, 0.5, Material.GOLD_BLOCK, 0.4f, 0.15f, 0.15f));

            // Lid (separate for opening animation)
            lid = spawnP(pos, 0, 1.2, 0, Material.DARK_OAK_PLANKS, 1.5f, 0.2f, 3.0f);

            DisplayBuilder.playSound(pos, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(60, 40, 30).interpolation(4, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise from ground
            if (riseY < 0) {
                riseY += 0.04f;
                if (riseY > 0) riseY = 0;
            }

            // Open lid after risen
            if (riseY >= 0 && lidAngle < 1.2f) {
                lidAngle += 0.01f;
            }

            lid.entity().teleport(c.clone().add(0, riseY + 1.2, -1.5 + Math.sin(lidAngle) * 1.5));
            lid.rotate(lidAngle, 1, 0, 0);

            // Dark particles from open coffin
            if (lidAngle > 0.3f && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE,
                        c.clone().add(0, riseY + 0.5, 0), 5, 0.5, 0.3, 1.0, 0.02);
                c.getWorld().spawnParticle(Particle.SOUL,
                        c.clone().add(0, riseY + 0.8, 0), 2, 0.3, 0.2, 0.5, 0.02);
            }

            // Rising particles
            if (riseY < 0 && ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK, c, 5, 0.8, 0.2, 1.5, 0.02,
                        Material.DIRT.createBlockData());
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WOODEN_DOOR_OPEN, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareGrave(plugin); }
    }

    // ================================================================
    // 12. SOUL VORTEX — 10 soul sand blocks spin in an accelerating
    //     ground-level vortex, pulling inward like a whirlpool.
    // ================================================================
    public static class SoulVortex extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> vortexBlocks = new ArrayList<>();
        private double spinSpeed = 0.03;
        private float vortexRadius = 6.0f;

        public SoulVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_vortex", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 10; i++) {
                Material mat = (i % 2 == 0) ? Material.SOUL_SAND : Material.SOUL_SOIL;
                BlockDisplayHandle b = displayBuilder.spawnBlock(center, mat);
                b.scale(1.0f, 0.5f, 1.0f).glow(80, 120, 140).interpolation(2, 0);
                vortexBlocks.add(b);
                spawnedEntities.add(b.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            spinSpeed += 0.0003; // Accelerate
            if (vortexRadius > 1.5f) vortexRadius -= 0.01f;

            for (int i = 0; i < vortexBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / vortexBlocks.size();
                double angle = baseAngle + ticksAlive * spinSpeed;
                // Spiral inward
                double r = vortexRadius - (i * 0.3);
                if (r < 0.5) r = 0.5;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;

                vortexBlocks.get(i).entity().teleport(c.clone().add(x, 0.3, z));
                vortexBlocks.get(i).rotate((float) angle, 0, 1, 0);
            }

            // Soul particles spiraling
            if (ticksAlive % 3 == 0) {
                double angle = ticksAlive * spinSpeed * 3;
                Location pLoc = c.clone().add(
                        Math.cos(angle) * vortexRadius * 0.7, 0.5, Math.sin(angle) * vortexRadius * 0.7);
                c.getWorld().spawnParticle(Particle.SOUL, pLoc, 2, 0.2, 0.2, 0.2, 0.02);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulVortex(plugin); }
    }

    // ================================================================
    // 13. ERUPTING PENTAGRAM — 5 arms of 2 blocks each (10) + center
    //     (2) = 12 form a pentagram on the ground that erupts fire.
    // ================================================================
    public static class EruptingPentagram extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();
        private BlockDisplayHandle center1, center2;
        private float glowIntensity = 0;
        private boolean erupting = false;

        public EruptingPentagram(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("erupting_pentagram", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(65.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center
            center1 = spawnP(center, 0, 0.1, 0, Material.NETHERRACK, 1.5f, 0.2f, 1.5f);
            center2 = spawnP(center, 0, 0.2, 0, Material.MAGMA_BLOCK, 1.0f, 0.15f, 1.0f);

            // 5 arms (pentagram points)
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5 - Math.PI / 2;
                for (int seg = 1; seg <= 2; seg++) {
                    double dist = seg * 2.5;
                    double x = Math.cos(angle) * dist;
                    double z = Math.sin(angle) * dist;
                    Material mat = (seg == 1) ? Material.RED_NETHER_BRICKS : Material.NETHERRACK;
                    BlockDisplayHandle b = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.1, z), mat);
                    b.scale(1.2f, 0.2f, 1.2f).glow(50, 10, 10).interpolation(4, 0);
                    starBlocks.add(b);
                    spawnedEntities.add(b.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(50, 10, 10);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gradually glow brighter
            if (ticksAlive < 80) {
                glowIntensity = ticksAlive / 80.0f;
            } else {
                erupting = true;
            }

            int r = (int)(50 + glowIntensity * 205);
            int g = (int)(10 + glowIntensity * 40);
            for (BlockDisplayHandle b : starBlocks) {
                b.glow(r, g, 10);
            }
            center1.glow(r, g, 10);
            center2.glow(Math.min(255, r + 50), g + 20, 10);

            // Draw pentagram lines with particles
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 5; i++) {
                    double a1 = (2 * Math.PI * i) / 5 - Math.PI / 2;
                    double a2 = (2 * Math.PI * ((i + 2) % 5)) / 5 - Math.PI / 2;
                    Location p1 = c.clone().add(Math.cos(a1) * 5, 0.3, Math.sin(a1) * 5);
                    Location p2 = c.clone().add(Math.cos(a2) * 5, 0.3, Math.sin(a2) * 5);
                    DisplayBuilder.particleLine(p1, p2, Particle.DUST, 2,
                            new Particle.DustOptions(Color.fromRGB(r, g, 10), 1.0f));
                }
            }

            // Eruption phase — fire columns from each point
            if (erupting && ticksAlive % 4 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = (2 * Math.PI * i) / 5 - Math.PI / 2;
                    Location pointLoc = c.clone().add(Math.cos(angle) * 5, 0.5, Math.sin(angle) * 5);
                    c.getWorld().spawnParticle(Particle.FLAME, pointLoc, 8, 0.3, 1.5, 0.3, 0.05);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pointLoc, 3, 0.2, 1.0, 0.2, 0.03);
                }
                // Center eruption
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 0.5, 0),
                        5, 0.5, 2.0, 0.5, 0.05);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new EruptingPentagram(plugin); }
    }
}
