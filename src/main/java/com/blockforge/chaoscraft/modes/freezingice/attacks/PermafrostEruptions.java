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
 * Freezing Ice Mode — PERMAFROST ERUPTIONS
 * 13 ground-eruption block display attacks: ice bursting from below.
 *
 * Rules applied:
 * - 10+ block displays per attack, ICE materials (BLUE_ICE, PACKED_ICE, ICE, SNOW_BLOCK, WHITE_CONCRETE, PRISMARINE)
 * - Spawn STRAIGHT (yaw=0, pitch=0)
 * - Particles: SNOWFLAKE, END_ROD, DUST RGB(100,180,255)
 * - Damage radiuses 5-8 blocks, NO potion effects
 * - All values configurable via AttackConfig
 */
public final class PermafrostEruptions {
    private PermafrostEruptions() {}

    // Shared ice dust options
    private static final Particle.DustOptions ICE_DUST = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.2f);
    private static final Particle.DustOptions ICE_DUST_SMALL = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 0.8f);

    private static void iceDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 100, 180, 255, 1.2f);
    }

    private static void snowflakes(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, count, spread, spread, spread, 0.02);
    }

    private static void endRods(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, count, spread, spread, spread, 0.01);
    }

    private static final Material[] ICE_MATS = {
        Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE, Material.SNOW_BLOCK,
        Material.WHITE_CONCRETE, Material.PRISMARINE
    };

    private static Material iceMat(int index) {
        return ICE_MATS[index % ICE_MATS.length];
    }

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceGeyser(plugin));
        registry.register(new FrostSpike(plugin));
        registry.register(new PermafrostCrack(plugin));
        registry.register(new IceFountain(plugin));
        registry.register(new FrostFlowerBurst(plugin));
        registry.register(new GlacialFissure(plugin));
        registry.register(new IceVolcano(plugin));
        registry.register(new FrostMushroom(plugin));
        registry.register(new CrystalCluster(plugin));
        registry.register(new IcePillarLine(plugin));
        registry.register(new PermafrostPlate(plugin));
        registry.register(new FrostWell(plugin));
        registry.register(new IceStalagmite(plugin));
    }

    // ================================================================
    // 1. ICE GEYSER — 12 blocks shooting upward from ground like geyser
    // ================================================================
    public static class IceGeyser extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_geyser", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 4 base ring blocks at ground level
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, -4, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(1.2f, 0.8f, 1.2f).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }
            // 8 geyser column blocks stacked vertically (start underground)
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, -4 + i * 0.8, 0);
                Material mat = (i % 2 == 0) ? Material.BLUE_ICE : Material.ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float taper = 1.0f - (i * 0.08f);
                h.scale(taper, 0.8f, taper).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            iceDust(center, 30, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Erupt upward over 30 ticks
            if (tick <= 30) {
                float progress = tick / 30.0f;
                float yOffset = progress * 4.0f;
                for (int i = 0; i < all.size(); i++) {
                    BlockDisplay bd = all.get(i).entity();
                    Location base = bd.getLocation();
                    bd.teleport(base.clone().add(0, yOffset / 30.0f, 0));
                }
            }
            // Phase 2: Spray particles like water geyser
            else if (tick <= 200) {
                if (tick % 3 == 0) {
                    Location top = c.clone().add(0, 5, 0);
                    snowflakes(top, 15, 1.5);
                    endRods(top, 5, 1.0);
                    iceDust(top, 8, 2.0);
                }
                // Tremble
                float tremble = (float) Math.sin(tick * 1.5) * 0.1f;
                for (BlockDisplayHandle h : all) {
                    BlockDisplay bd = h.entity();
                    Location loc = bd.getLocation();
                    bd.teleport(loc.clone().add(tremble * 0.5, 0, tremble * 0.5));
                }
            }

            // Ongoing particle spray
            if (tick % 5 == 0 && tick > 30) {
                DisplayBuilder.particleRing(c.clone().add(0, 1, 0), 1.5, Particle.SNOWFLAKE, 12, null);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceGeyser(plugin); }
    }

    // ================================================================
    // 2. FROST SPIKE — 10 blocks forming massive pointed spike (5 tall)
    // ================================================================
    public static class FrostSpike extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_spike", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Base: 4 wide blocks at ground level
            double[][] baseOffsets = {{0.6, 0, 0.6}, {-0.6, 0, 0.6}, {0.6, 0, -0.6}, {-0.6, 0, -0.6}};
            for (double[] off : baseOffsets) {
                Location loc = center.clone().add(off[0], -5, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(1.4f, 1.0f, 1.4f).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }
            // Spine: 5 tapering blocks (the spike) starting underground
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, -5 + i * 1.0, 0);
                Material mat = (i < 3) ? Material.BLUE_ICE : Material.ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float taper = 1.2f - (i * 0.22f);
                h.scale(taper, 1.0f, taper).glow(100, 200, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }
            // Tip: 1 sharp narrow block
            Location tipLoc = center.clone().add(0, -5 + 5, 0);
            BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.WHITE_CONCRETE);
            tip.scale(0.3f, 1.2f, 0.3f).glow(180, 220, 255).interpolation(3, 0);
            all.add(tip);
            spawnedEntities.add(tip.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            iceDust(center, 25, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Erupt upward over 25 ticks
            if (tick <= 25) {
                float progress = tick / 25.0f;
                float yOffset = progress * 5.0f / 25.0f;
                for (BlockDisplayHandle h : all) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, yOffset, 0));
                }
                // Ground crack particles during eruption
                if (tick % 3 == 0) {
                    snowflakes(c.clone().add(0, 0.3, 0), 20, 2.0);
                    iceDust(c, 10, 1.5);
                }
            }
            // Idle phase: shimmer and particles
            else {
                if (tick % 4 == 0) {
                    Location tipLoc = c.clone().add(0, 5.5, 0);
                    endRods(tipLoc, 4, 0.3);
                    snowflakes(tipLoc, 6, 0.5);
                }
                // Micro-vibration
                float tremble = (float) Math.sin(tick * 2.0) * 0.05f;
                for (BlockDisplayHandle h : all) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(tremble, 0, -tremble));
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostSpike(plugin); }
    }

    // ================================================================
    // 3. PERMAFROST CRACK — 14 blocks erupting from crack extending 10 blocks toward player
    // ================================================================
    public static class PermafrostCrack extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private double dirX, dirZ;

        public PermafrostCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_crack", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Pick a random direction for the crack line
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // 14 blocks along the crack line, alternating sides, all underground
            for (int i = 0; i < 14; i++) {
                double dist = (i - 7) * 0.75; // spread across ~10 blocks
                double sideOffset = ((i % 2 == 0) ? 0.3 : -0.3);
                Location loc = center.clone().add(
                    dirX * dist + dirZ * sideOffset,
                    -4,
                    dirZ * dist - dirX * sideOffset
                );
                Material mat = (i % 3 == 0) ? Material.BLUE_ICE : (i % 3 == 1) ? Material.PACKED_ICE : Material.PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scaleY = 0.4f + (float)(Math.random() * 0.6);
                h.scale(0.6f, scaleY, 0.6f).glow(100, 180, 255).interpolation(3, 0);
                // Slight random tilt
                h.rotate((float)(Math.random() * 0.3 - 0.15), 0, 0, 1);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.4f);
            iceDust(center, 30, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sequential eruption: each block rises at staggered intervals over 50 ticks
            if (tick <= 50) {
                for (int i = 0; i < all.size(); i++) {
                    int startTick = i * 3; // stagger 3 ticks apart
                    if (tick >= startTick && tick < startTick + 15) {
                        float progress = (tick - startTick) / 15.0f;
                        float yOffset = progress * 4.0f / 15.0f;
                        BlockDisplay bd = all.get(i).entity();
                        bd.teleport(bd.getLocation().clone().add(0, yOffset, 0));
                    }
                }
                // Crack particles along the line
                if (tick % 2 == 0) {
                    int activeIndex = Math.min(tick / 3, all.size() - 1);
                    Location crackPoint = all.get(activeIndex).entity().getLocation().add(0, 0.5, 0);
                    snowflakes(crackPoint, 10, 0.5);
                    iceDust(crackPoint, 5, 0.3);
                }
            }
            // Idle: frost mist along the crack
            else if (tick % 6 == 0) {
                int idx = (tick / 6) % all.size();
                Location loc = all.get(idx).entity().getLocation().add(0, 1.0, 0);
                snowflakes(loc, 6, 0.4);
                endRods(loc, 2, 0.2);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PermafrostCrack(plugin); }
    }

    // ================================================================
    // 4. ICE FOUNTAIN — 12 blocks spraying upward then arcing outward
    // ================================================================
    public static class IceFountain extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] angles;
        private final double[] arcSpeeds;

        public IceFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_fountain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            angles = new double[12];
            arcSpeeds = new double[12];
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 12 blocks: all start at center underground
            for (int i = 0; i < 12; i++) {
                angles[i] = (Math.PI * 2 * i) / 12;
                arcSpeeds[i] = 0.8 + Math.random() * 0.4;
                Material mat = (i % 4 == 0) ? Material.BLUE_ICE : (i % 4 == 1) ? Material.ICE
                        : (i % 4 == 2) ? Material.PACKED_ICE : Material.SNOW_BLOCK;
                Location loc = center.clone().add(0, -3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.7f, 0.7f).glow(100, 180, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.6f);
            iceDust(center, 25, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (tick 1-40): shoot upward then arc outward (parabolic)
            if (tick <= 40) {
                float t = tick / 40.0f;
                for (int i = 0; i < all.size(); i++) {
                    // Parabolic arc: y = -4t^2 + 4t (peaks at t=0.5 → y=1 → scaled to 5 blocks)
                    double yArc = (-4 * t * t + 4 * t) * 5.0;
                    double outward = t * arcSpeeds[i] * 3.0;
                    double x = Math.cos(angles[i]) * outward;
                    double z = Math.sin(angles[i]) * outward;
                    Location target = c.clone().add(x, yArc, z);
                    all.get(i).entity().teleport(target);
                }
                // Spray particles from center upward
                if (tick % 2 == 0) {
                    snowflakes(c.clone().add(0, 3, 0), 15, 1.0);
                    endRods(c.clone().add(0, 4, 0), 5, 0.5);
                }
            }
            // Phase 2: blocks settled, continuous mist
            else if (tick % 5 == 0) {
                for (int i = 0; i < all.size(); i += 3) {
                    Location loc = all.get(i).entity().getLocation().add(0, 0.5, 0);
                    snowflakes(loc, 4, 0.3);
                }
                iceDust(c.clone().add(0, 0.5, 0), 8, 2.5);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceFountain(plugin); }
    }

    // ================================================================
    // 5. FROST FLOWER BURST — 13 blocks, center rises first then petals unfold
    // ================================================================
    public static class FrostFlowerBurst extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle centerBlock;
        private final List<BlockDisplayHandle> petals = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostFlowerBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_flower_burst", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Center stem: 1 tall block (starts underground)
            centerBlock = displayBuilder.spawnBlock(center.clone().add(0, -4, 0), Material.BLUE_ICE);
            centerBlock.scale(0.8f, 2.0f, 0.8f).glow(100, 200, 255).interpolation(3, 0);
            all.add(centerBlock);
            spawnedEntities.add(centerBlock.entity());

            // 12 petal blocks arranged in 2 rings of 6, all at center underground
            for (int ring = 0; ring < 2; ring++) {
                for (int i = 0; i < 6; i++) {
                    Location loc = center.clone().add(0, -4, 0);
                    Material mat = (ring == 0) ? Material.ICE : Material.SNOW_BLOCK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    float scale = (ring == 0) ? 0.9f : 0.7f;
                    h.scale(scale, 0.4f, scale).glow(100, 180, 255).interpolation(3, 0);
                    petals.add(h);
                    all.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
            iceDust(center, 20, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (tick 1-20): Center stem rises
            if (tick <= 20) {
                float progress = tick / 20.0f;
                centerBlock.entity().teleport(c.clone().add(0, -4 + progress * 4.0, 0));
                if (tick % 4 == 0) {
                    snowflakes(c.clone().add(0, progress * 2, 0), 10, 0.5);
                }
            }
            // Phase 2 (tick 21-45): Petals unfold outward from center
            else if (tick <= 45) {
                float petalProgress = (tick - 20) / 25.0f;
                for (int i = 0; i < petals.size(); i++) {
                    int ring = i / 6;
                    int idx = i % 6;
                    double angle = (Math.PI * 2 * idx) / 6 + (ring * Math.PI / 6); // offset rings
                    double radius = (ring == 0 ? 2.0 : 3.2) * petalProgress;
                    double yOff = (ring == 0 ? 0.5 : 0.0);
                    Location target = c.clone().add(
                        Math.cos(angle) * radius,
                        yOff,
                        Math.sin(angle) * radius
                    );
                    petals.get(i).entity().teleport(target);
                    // Tilt petals outward as they unfold
                    float tilt = petalProgress * 0.5f;
                    petals.get(i).rotate(tilt, (float) Math.cos(angle), 0, (float) Math.sin(angle));
                    petals.get(i).interpolation(3, 0);
                }
                if (tick % 3 == 0) {
                    endRods(c.clone().add(0, 1.5, 0), 6, 1.5);
                }
            }
            // Phase 3: Bloom shimmer
            else if (tick % 5 == 0) {
                for (int i = 0; i < petals.size(); i += 4) {
                    Location pLoc = petals.get(i).entity().getLocation().add(0, 0.3, 0);
                    snowflakes(pLoc, 3, 0.3);
                }
                iceDust(c.clone().add(0, 1, 0), 5, 2.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostFlowerBurst(plugin); }
    }

    // ================================================================
    // 6. GLACIAL FISSURE — 16 blocks rising alternating left-right from wide crack
    // ================================================================
    public static class GlacialFissure extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private double dirX, dirZ;

        public GlacialFissure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_fissure", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // 16 blocks: 8 pairs, alternating left/right of the fissure line
            for (int i = 0; i < 16; i++) {
                int pair = i / 2;
                boolean leftSide = (i % 2 == 0);
                double along = (pair - 4) * 1.3; // spread along crack
                double side = leftSide ? 1.0 : -1.0;

                Location loc = center.clone().add(
                    dirX * along + dirZ * side,
                    -5,
                    dirZ * along - dirX * side
                );
                Material mat;
                if (i % 4 == 0) mat = Material.BLUE_ICE;
                else if (i % 4 == 1) mat = Material.PACKED_ICE;
                else if (i % 4 == 2) mat = Material.PRISMARINE;
                else mat = Material.ICE;

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float height = 1.0f + (float)(Math.random() * 1.5);
                h.scale(0.7f, height, 0.7f).glow(100, 180, 255).interpolation(3, 0);
                // Lean outward from fissure center
                float lean = leftSide ? 0.2f : -0.2f;
                h.rotate(lean, (float) dirZ, 0, (float) -dirX);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.5f);
            iceDust(center, 40, 4.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Staggered alternating rise: left block, then right, repeating per pair
            if (tick <= 60) {
                for (int i = 0; i < all.size(); i++) {
                    int pair = i / 2;
                    boolean leftSide = (i % 2 == 0);
                    // Left blocks rise on even pairs first, right on odd
                    int startTick = pair * 5 + (leftSide ? 0 : 3);
                    if (tick >= startTick && tick < startTick + 20) {
                        float progress = (tick - startTick) / 20.0f;
                        BlockDisplay bd = all.get(i).entity();
                        bd.teleport(bd.getLocation().clone().add(0, progress * 5.0 / 20.0, 0));
                    }
                }
                if (tick % 3 == 0) {
                    int activeIdx = Math.min((tick / 5) * 2, all.size() - 1);
                    Location pt = all.get(activeIdx).entity().getLocation().add(0, 0.5, 0);
                    snowflakes(pt, 12, 0.5);
                    iceDust(pt, 6, 0.4);
                }
            }
            // Idle: frost mist drifting along the fissure
            else if (tick % 6 == 0) {
                int idx = (tick / 6) % all.size();
                Location loc = all.get(idx).entity().getLocation().add(0, 1.2, 0);
                snowflakes(loc, 5, 0.4);
                endRods(loc, 2, 0.2);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GlacialFissure(plugin); }
    }

    // ================================================================
    // 7. ICE VOLCANO — 12 cone blocks + 4 projectile blocks erupting from top
    // ================================================================
    public static class IceVolcano extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> cone = new ArrayList<>();
        private final List<BlockDisplayHandle> projectiles = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] projAngles = new double[4];
        private boolean erupted = false;

        public IceVolcano(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_volcano", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Cone: 12 blocks in 4 layers (4 base, 3, 3, 2 top) tapering upward, underground
            int blockIdx = 0;
            int[] layerCounts = {4, 3, 3, 2};
            double[] layerRadii = {2.0, 1.4, 0.8, 0.3};
            Material[] layerMats = {Material.PACKED_ICE, Material.BLUE_ICE, Material.ICE, Material.WHITE_CONCRETE};

            for (int layer = 0; layer < 4; layer++) {
                for (int i = 0; i < layerCounts[layer]; i++) {
                    double angle = (Math.PI * 2 * i) / layerCounts[layer];
                    Location loc = center.clone().add(
                        Math.cos(angle) * layerRadii[layer],
                        -6 + layer * 1.2,
                        Math.sin(angle) * layerRadii[layer]
                    );
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, layerMats[layer]);
                    float s = 1.2f - layer * 0.2f;
                    h.scale(s, 1.2f, s).glow(100, 180, 255).interpolation(3, 0);
                    cone.add(h);
                    all.add(h);
                    spawnedEntities.add(h.entity());
                    blockIdx++;
                }
            }

            // 4 projectile blocks at the top (will erupt later)
            for (int i = 0; i < 4; i++) {
                projAngles[i] = (Math.PI * 2 * i) / 4 + Math.random() * 0.5;
                Location loc = center.clone().add(0, -6 + 4.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.5f, 0.5f, 0.5f).glow(150, 220, 255).interpolation(2, 0);
                projectiles.add(h);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.4f);
            iceDust(center, 35, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (1-35): Cone rises from ground
            if (tick <= 35) {
                float progress = tick / 35.0f;
                for (BlockDisplayHandle h : cone) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, progress * 6.0 / 35.0, 0));
                }
                for (BlockDisplayHandle h : projectiles) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, progress * 6.0 / 35.0, 0));
                }
                if (tick % 4 == 0) {
                    snowflakes(c.clone().add(0, progress * 3, 0), 15, 1.0);
                }
            }
            // Phase 2 (36-60): Projectile blocks erupt outward in arcs
            else if (tick <= 60 && !erupted) {
                float t = (tick - 35) / 25.0f;
                for (int i = 0; i < projectiles.size(); i++) {
                    double outward = t * 4.0;
                    double yArc = (-4 * t * t + 4 * t) * 4.0; // parabolic
                    Location target = c.clone().add(
                        Math.cos(projAngles[i]) * outward,
                        4.8 + yArc,
                        Math.sin(projAngles[i]) * outward
                    );
                    projectiles.get(i).entity().teleport(target);
                }
                if (tick % 2 == 0) {
                    Location top = c.clone().add(0, 5, 0);
                    snowflakes(top, 20, 1.5);
                    endRods(top, 8, 1.0);
                    iceDust(top, 10, 1.5);
                }
                if (tick == 60) erupted = true;
            }
            // Phase 3: Vent mist from crater
            else if (tick % 5 == 0) {
                Location vent = c.clone().add(0, 4.5, 0);
                snowflakes(vent, 8, 0.8);
                iceDust(vent, 4, 0.5);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceVolcano(plugin); }
    }

    // ================================================================
    // 8. FROST MUSHROOM — 6 stem + 10 cap blocks, stem rises then cap expands
    // ================================================================
    public static class FrostMushroom extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> stem = new ArrayList<>();
        private final List<BlockDisplayHandle> cap = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostMushroom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_mushroom", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Stem: 6 blocks vertically (underground)
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, -6 + i, 0);
                Material mat = (i % 2 == 0) ? Material.PACKED_ICE : Material.BLUE_ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 1.0f, 0.8f).glow(100, 180, 255).interpolation(3, 0);
                stem.add(h);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cap: 10 blocks — center disc + 8 edge blocks + 1 top (all at center underground)
            // Center cap block
            BlockDisplayHandle capCenter = displayBuilder.spawnBlock(center.clone().add(0, -6, 0), Material.ICE);
            capCenter.scale(2.0f, 0.5f, 2.0f).glow(120, 200, 255).interpolation(3, 0);
            cap.add(capCenter);
            all.add(capCenter);
            spawnedEntities.add(capCenter.entity());

            // 8 edge cap blocks in a ring
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(0, -6, 0);
                Material mat = (i % 2 == 0) ? Material.SNOW_BLOCK : Material.WHITE_CONCRETE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.0f, 0.4f, 1.0f).glow(100, 180, 255).interpolation(3, 0);
                cap.add(h);
                all.add(h);
                spawnedEntities.add(h.entity());
            }
            // Top cap accent
            BlockDisplayHandle topCap = displayBuilder.spawnBlock(center.clone().add(0, -6, 0), Material.BLUE_ICE);
            topCap.scale(1.2f, 0.3f, 1.2f).glow(150, 220, 255).interpolation(3, 0);
            cap.add(topCap);
            all.add(topCap);
            spawnedEntities.add(topCap.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);
            iceDust(center, 20, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (1-30): Stem rises from ground
            if (tick <= 30) {
                float progress = tick / 30.0f;
                for (int i = 0; i < stem.size(); i++) {
                    BlockDisplay bd = stem.get(i).entity();
                    bd.teleport(bd.getLocation().clone().add(0, progress * 6.0 / 30.0, 0));
                }
                // Cap follows stem top
                for (BlockDisplayHandle h : cap) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, progress * 6.0 / 30.0, 0));
                }
                if (tick % 4 == 0) {
                    snowflakes(c.clone().add(0, progress * 3, 0), 10, 0.5);
                }
            }
            // Phase 2 (31-55): Cap expands outward at top of stem
            else if (tick <= 55) {
                float capProgress = (tick - 30) / 25.0f;
                // Cap center moves to top of stem
                Location stemTop = c.clone().add(0, 5.5, 0);
                cap.get(0).entity().teleport(stemTop);

                // Edge blocks expand outward
                for (int i = 1; i <= 8; i++) {
                    double angle = (Math.PI * 2 * (i - 1)) / 8;
                    double radius = capProgress * 2.5;
                    Location target = stemTop.clone().add(
                        Math.cos(angle) * radius,
                        -0.2,
                        Math.sin(angle) * radius
                    );
                    cap.get(i).entity().teleport(target);
                }
                // Top cap block
                cap.get(9).entity().teleport(stemTop.clone().add(0, 0.4, 0));

                if (tick % 3 == 0) {
                    endRods(stemTop, 6, 2.0);
                    snowflakes(stemTop, 8, 2.0);
                }
            }
            // Phase 3: Drip particles from cap edges
            else if (tick % 5 == 0) {
                for (int i = 1; i <= 8; i += 2) {
                    Location edgeLoc = cap.get(i).entity().getLocation().add(0, -0.3, 0);
                    snowflakes(edgeLoc, 3, 0.2);
                }
                iceDust(c.clone().add(0, 5, 0), 4, 2.5);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostMushroom(plugin); }
    }

    // ================================================================
    // 9. CRYSTAL CLUSTER — 12 blocks erupting at random angles (hedgehog)
    // ================================================================
    public static class CrystalCluster extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] spikeAnglesH;
        private final double[] spikeAnglesV;
        private final float[] spikeLengths;

        public CrystalCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_cluster", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            spikeAnglesH = new double[12];
            spikeAnglesV = new double[12];
            spikeLengths = new float[12];
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 12 crystal shards radiating from center at random angles
            for (int i = 0; i < 12; i++) {
                spikeAnglesH[i] = Math.random() * Math.PI * 2;
                spikeAnglesV[i] = Math.random() * Math.PI * 0.6 - 0.1; // mostly upward
                spikeLengths[i] = 1.5f + (float)(Math.random() * 2.5);

                Location loc = center.clone().add(0, -3, 0);
                Material mat;
                if (i % 4 == 0) mat = Material.BLUE_ICE;
                else if (i % 4 == 1) mat = Material.PRISMARINE;
                else if (i % 4 == 2) mat = Material.ICE;
                else mat = Material.PACKED_ICE;

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 0.4f, 0.4f).glow(100, 180, 255).interpolation(3, 0);
                // Rotate each shard to point in its random direction
                h.rotate((float) spikeAnglesV[i], (float) Math.cos(spikeAnglesH[i]), 0, (float) Math.sin(spikeAnglesH[i]));
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.5f);
            iceDust(center, 30, 1.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (1-35): Shards shoot outward from center
            if (tick <= 35) {
                float progress = tick / 35.0f;
                for (int i = 0; i < all.size(); i++) {
                    double dist = spikeLengths[i] * progress;
                    double x = Math.cos(spikeAnglesH[i]) * Math.cos(spikeAnglesV[i]) * dist;
                    double y = Math.sin(spikeAnglesV[i]) * dist;
                    double z = Math.sin(spikeAnglesH[i]) * Math.cos(spikeAnglesV[i]) * dist;
                    Location target = c.clone().add(x, y, z);
                    all.get(i).entity().teleport(target);

                    // Scale up as they extend
                    float scaleW = 0.3f + progress * 0.3f;
                    float scaleL = 0.4f + progress * (spikeLengths[i] * 0.3f);
                    all.get(i).scale(scaleW, scaleL, scaleW);
                    all.get(i).interpolation(3, 0);
                }
                if (tick % 3 == 0) {
                    snowflakes(c, 15, 1.5);
                    iceDust(c, 8, 1.0);
                }
            }
            // Phase 2: Crystal shimmer at tips
            else if (tick % 4 == 0) {
                for (int i = 0; i < all.size(); i += 3) {
                    Location tipLoc = all.get(i).entity().getLocation().add(0, 0.3, 0);
                    endRods(tipLoc, 2, 0.2);
                }
                snowflakes(c, 5, 2.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrystalCluster(plugin); }
    }

    // ================================================================
    // 10. ICE PILLAR LINE — 18 blocks (6 pillars of 3) erupting in sequence toward player
    // ================================================================
    public static class IcePillarLine extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private double dirX, dirZ;

        public IcePillarLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_pillar_line", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Direction toward nearest player or random
            double angle = Math.random() * Math.PI * 2;
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }
            if (nearest != null) {
                Location pLoc = nearest.getLocation();
                double dx = pLoc.getX() - center.getX();
                double dz = pLoc.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) {
                    dirX = dx / len;
                    dirZ = dz / len;
                } else {
                    dirX = Math.cos(angle);
                    dirZ = Math.sin(angle);
                }
            } else {
                dirX = Math.cos(angle);
                dirZ = Math.sin(angle);
            }

            // 6 pillars, each 3 blocks tall, spaced 1.8 apart along direction
            Material[] pillarMats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE};
            for (int pillar = 0; pillar < 6; pillar++) {
                double dist = pillar * 1.8;
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(
                        dirX * dist,
                        -5 + y,
                        dirZ * dist
                    );
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, pillarMats[y]);
                    float taper = 1.0f - (y * 0.15f);
                    h.scale(taper, 1.0f, taper).glow(100, 180, 255).interpolation(3, 0);
                    all.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            iceDust(center, 25, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sequential eruption: each pillar (3 blocks) erupts 8 ticks apart
            if (tick <= 70) {
                for (int pillar = 0; pillar < 6; pillar++) {
                    int startTick = pillar * 8;
                    if (tick >= startTick && tick < startTick + 20) {
                        float progress = (tick - startTick) / 20.0f;
                        for (int y = 0; y < 3; y++) {
                            int idx = pillar * 3 + y;
                            BlockDisplay bd = all.get(idx).entity();
                            bd.teleport(bd.getLocation().clone().add(0, progress * 5.0 / 20.0, 0));
                        }
                    }
                    // Burst particles when each pillar starts rising
                    if (tick == startTick) {
                        double dist = pillar * 1.8;
                        Location burstLoc = c.clone().add(dirX * dist, 0.5, dirZ * dist);
                        snowflakes(burstLoc, 15, 0.8);
                        iceDust(burstLoc, 8, 0.5);
                        DisplayBuilder.playSound(burstLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.8f + pillar * 0.1f);
                    }
                }
            }
            // Idle: frost trail particles along the line
            else if (tick % 5 == 0) {
                int pillar = (tick / 5) % 6;
                double dist = pillar * 1.8;
                Location trailLoc = c.clone().add(dirX * dist, 1.5, dirZ * dist);
                snowflakes(trailLoc, 5, 0.4);
                endRods(trailLoc, 2, 0.3);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IcePillarLine(plugin); }
    }

    // ================================================================
    // 11. PERMAFROST PLATE — 16 flat blocks rising flush with ground, then shards pop up
    // ================================================================
    public static class PermafrostPlate extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> plates = new ArrayList<>();
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public PermafrostPlate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_plate", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 10 flat plate blocks in a 3x3+ grid pattern (underground, will rise flush)
            double[][] plateOffsets = {
                {0, 0}, {1.2, 0}, {-1.2, 0}, {0, 1.2}, {0, -1.2},
                {1.2, 1.2}, {-1.2, 1.2}, {1.2, -1.2}, {-1.2, -1.2}, {2.4, 0}
            };
            for (int i = 0; i < plateOffsets.length; i++) {
                Location loc = center.clone().add(plateOffsets[i][0], -2, plateOffsets[i][1]);
                Material mat = (i % 3 == 0) ? Material.BLUE_ICE : (i % 3 == 1) ? Material.PACKED_ICE : Material.SNOW_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.2f, 0.2f, 1.2f).glow(100, 180, 255).interpolation(3, 0);
                plates.add(h);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 shard blocks (will pop up from plates later)
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, -3, Math.sin(angle) * 1.5);
                Material mat = (i % 2 == 0) ? Material.ICE : Material.PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.3f, 0.3f, 0.3f).glow(150, 220, 255).interpolation(2, 0);
                // Tilt shards at angles
                h.rotate((float)(0.3 + Math.random() * 0.4), (float) Math.cos(angle), 0, (float) Math.sin(angle));
                shards.add(h);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
            iceDust(center, 25, 2.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (1-20): Plates rise flush with ground surface
            if (tick <= 20) {
                float progress = tick / 20.0f;
                for (BlockDisplayHandle h : plates) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, progress * 2.0 / 20.0, 0));
                }
                if (tick % 3 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), 2.5, Particle.SNOWFLAKE, 16, null);
                }
            }
            // Phase 2 (30-50): Shards pop up from plates
            else if (tick >= 30 && tick <= 50) {
                float shardProgress = (tick - 30) / 20.0f;
                for (int i = 0; i < shards.size(); i++) {
                    BlockDisplay bd = shards.get(i).entity();
                    bd.teleport(bd.getLocation().clone().add(0, shardProgress * 3.0 / 20.0, 0));
                    // Scale up as they emerge
                    float s = 0.3f + shardProgress * 0.5f;
                    shards.get(i).scale(s, s * 2.5f, s);
                    shards.get(i).interpolation(3, 0);
                }
                if (tick % 3 == 0) {
                    snowflakes(c.clone().add(0, 1.0, 0), 12, 2.0);
                    iceDust(c.clone().add(0, 0.5, 0), 6, 1.5);
                }
            }
            // Phase 3: Frost mist across plate surface
            else if (tick % 6 == 0) {
                double rx = (Math.random() - 0.5) * 4;
                double rz = (Math.random() - 0.5) * 4;
                snowflakes(c.clone().add(rx, 0.3, rz), 5, 0.3);
                endRods(c.clone().add(0, 1.5, 0), 3, 1.5);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PermafrostPlate(plugin); }
    }

    // ================================================================
    // 12. FROST WELL — 12 rim blocks rising in circle, periodic central eruption
    // ================================================================
    public static class FrostWell extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private BlockDisplayHandle centralGeyser;

        public FrostWell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_well", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 12 rim blocks in a circle (underground)
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, -4, Math.sin(angle) * 2.5);
                Material mat;
                if (i % 4 == 0) mat = Material.BLUE_ICE;
                else if (i % 4 == 1) mat = Material.PACKED_ICE;
                else if (i % 4 == 2) mat = Material.PRISMARINE;
                else mat = Material.ICE;

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.9f, 1.2f, 0.9f).glow(100, 180, 255).interpolation(3, 0);
                rim.add(h);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central geyser block (will pulse up and down)
            centralGeyser = displayBuilder.spawnBlock(center.clone().add(0, -4, 0), Material.WHITE_CONCRETE);
            centralGeyser.scale(0.6f, 0.6f, 0.6f).glow(180, 220, 255).interpolation(2, 0);
            all.add(centralGeyser);
            spawnedEntities.add(centralGeyser.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            iceDust(center, 25, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (1-30): Rim blocks rise from ground
            if (tick <= 30) {
                float progress = tick / 30.0f;
                for (BlockDisplayHandle h : rim) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, progress * 4.0 / 30.0, 0));
                }
                centralGeyser.entity().teleport(
                    centralGeyser.entity().getLocation().clone().add(0, progress * 4.0 / 30.0, 0)
                );
                if (tick % 4 == 0) {
                    DisplayBuilder.particleRing(c.clone().add(0, progress * 2, 0), 2.5, Particle.SNOWFLAKE, 16, null);
                }
            }
            // Phase 2: Periodic central eruptions every 40 ticks
            else {
                // Central geyser pulses up and down
                int eruptionPhase = (tick - 30) % 40;
                if (eruptionPhase < 15) {
                    // Rising
                    float eruptProgress = eruptionPhase / 15.0f;
                    Location geyserLoc = c.clone().add(0, eruptProgress * 4.0, 0);
                    centralGeyser.entity().teleport(geyserLoc);
                    centralGeyser.scale(0.6f + eruptProgress * 0.4f, 0.6f + eruptProgress * 2.0f, 0.6f + eruptProgress * 0.4f);
                    centralGeyser.interpolation(2, 0);

                    if (eruptionPhase % 2 == 0) {
                        snowflakes(geyserLoc.clone().add(0, 1, 0), 15, 0.5);
                        endRods(geyserLoc.clone().add(0, 2, 0), 5, 0.3);
                        iceDust(geyserLoc, 8, 0.5);
                    }
                } else if (eruptionPhase < 25) {
                    // Falling back
                    float fallProgress = (eruptionPhase - 15) / 10.0f;
                    Location geyserLoc = c.clone().add(0, (1.0f - fallProgress) * 4.0, 0);
                    centralGeyser.entity().teleport(geyserLoc);
                    centralGeyser.scale(0.6f, 0.6f, 0.6f);
                    centralGeyser.interpolation(2, 0);
                } else {
                    // Rest at base
                    centralGeyser.entity().teleport(c.clone());
                }

                // Sound on eruption start
                if (eruptionPhase == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_SPLASH, 0.7f, 0.5f);
                }

                // Ambient rim mist
                if (tick % 6 == 0) {
                    int idx = (tick / 6) % 12;
                    Location rimLoc = rim.get(idx).entity().getLocation().add(0, 1.0, 0);
                    snowflakes(rimLoc, 3, 0.3);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostWell(plugin); }
    }

    // ================================================================
    // 13. ICE STALAGMITE — 15 pointed blocks in cluster, staggered rise, tallest in center
    // ================================================================
    public static class IceStalagmite extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] offsetsX;
        private final double[] offsetsZ;
        private final float[] heights;

        public IceStalagmite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_stalagmite", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            offsetsX = new double[15];
            offsetsZ = new double[15];
            heights = new float[15];
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 15 stalagmites: center tallest, radiating outward with decreasing height
            for (int i = 0; i < 15; i++) {
                if (i == 0) {
                    // Center stalagmite — tallest
                    offsetsX[i] = 0;
                    offsetsZ[i] = 0;
                    heights[i] = 5.0f;
                } else if (i <= 5) {
                    // Inner ring (5)
                    double angle = (Math.PI * 2 * (i - 1)) / 5;
                    offsetsX[i] = Math.cos(angle) * 1.5;
                    offsetsZ[i] = Math.sin(angle) * 1.5;
                    heights[i] = 3.0f + (float)(Math.random() * 1.0);
                } else {
                    // Outer ring (9)
                    double angle = (Math.PI * 2 * (i - 6)) / 9;
                    offsetsX[i] = Math.cos(angle) * 3.0;
                    offsetsZ[i] = Math.sin(angle) * 3.0;
                    heights[i] = 1.5f + (float)(Math.random() * 1.5);
                }

                Location loc = center.clone().add(offsetsX[i], -5, offsetsZ[i]);
                Material mat;
                if (i == 0) mat = Material.BLUE_ICE;
                else if (i <= 5) mat = (i % 2 == 0) ? Material.PACKED_ICE : Material.ICE;
                else mat = (i % 3 == 0) ? Material.PRISMARINE : (i % 3 == 1) ? Material.SNOW_BLOCK : Material.WHITE_CONCRETE;

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                // Pointed shape: narrow width, variable height
                float width = 0.5f - (heights[i] / 20.0f); // taller = narrower
                if (width < 0.2f) width = 0.2f;
                h.scale(width, heights[i] * 0.3f, width).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.6f);
            iceDust(center, 30, 2.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Staggered rise: center first, inner ring next, outer ring last
            if (tick <= 60) {
                for (int i = 0; i < all.size(); i++) {
                    int startTick;
                    if (i == 0) startTick = 0;
                    else if (i <= 5) startTick = 10;
                    else startTick = 25;

                    int riseDuration = 25;
                    if (tick >= startTick && tick < startTick + riseDuration) {
                        float progress = (tick - startTick) / (float) riseDuration;
                        BlockDisplay bd = all.get(i).entity();
                        bd.teleport(bd.getLocation().clone().add(0, progress * 5.0 / riseDuration, 0));

                        // Scale height grows as it rises
                        float width = 0.5f - (heights[i] / 20.0f);
                        if (width < 0.2f) width = 0.2f;
                        float scaleH = heights[i] * 0.3f * (0.3f + progress * 0.7f);
                        all.get(i).scale(width, scaleH, width);
                        all.get(i).interpolation(3, 0);
                    }
                }
                // Eruption particles
                if (tick % 3 == 0) {
                    snowflakes(c.clone().add(0, 1.0, 0), 15, 2.0);
                    iceDust(c, 8, 1.5);
                }
                // Center hits ground sound
                if (tick == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
                }
                if (tick == 10) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.8f);
                }
                if (tick == 25) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.0f);
                }
            }
            // Idle: Crystal shimmer at tips
            else if (tick % 5 == 0) {
                // Center tip
                Location tipCenter = c.clone().add(0, heights[0], 0);
                endRods(tipCenter, 4, 0.3);
                snowflakes(tipCenter, 3, 0.2);

                // Random tip sparkle
                int idx = 1 + (tick / 5) % (all.size() - 1);
                Location tipLoc = all.get(idx).entity().getLocation().add(0, heights[idx] * 0.3f, 0);
                endRods(tipLoc, 2, 0.2);
            }

            // Ambient frost haze across the cluster
            if (tick > 60 && tick % 8 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), 3.0, Particle.SNOWFLAKE, 12, null);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceStalagmite(plugin); }
    }
}
