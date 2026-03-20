package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Freezing Ice Mode — LIVING ICE CREATURE ATTACKS
 * 13 ice-themed BlockDisplay attacks that form recognizable living creatures.
 * Each creature is built from ice-palette block displays and animates uniquely.
 *
 * Color palette:
 * - Ice blue: RGB(100, 180, 255)
 * - Frost white: RGB(220, 240, 255)
 * - Deep ice: RGB(30, 80, 160)
 * - Glacial glow: RGB(150, 210, 255)
 *
 * Materials: BLUE_ICE, PACKED_ICE, ICE, SNOW_BLOCK, PRISMARINE,
 *            LIGHT_BLUE_STAINED_GLASS, WHITE_CONCRETE, BONE_BLOCK, QUARTZ_BLOCK
 *
 * Particles: SNOWFLAKE, END_ROD, DUST with ice colors
 * Sounds: BLOCK_GLASS_BREAK, BLOCK_POWDER_SNOW_STEP, BLOCK_AMETHYST_BLOCK_CHIME, ENTITY_PLAYER_HURT_FREEZE
 */
public final class LivingIce {
    private LivingIce() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FrostSerpent(plugin));
        registry.register(new IceSpider(plugin));
        registry.register(new CrystallineStalker(plugin));
        registry.register(new FrostWorm(plugin));
        registry.register(new IceSwarm(plugin));
        registry.register(new GlacialCrab(plugin));
        registry.register(new FrostBat(plugin));
        registry.register(new CrystalJellyfish(plugin));
        registry.register(new IceMimic(plugin));
        registry.register(new PermafrostBeetle(plugin));
        registry.register(new FrostHydra(plugin));
        registry.register(new IceLeech(plugin));
        registry.register(new GlacialBehemoth(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    // ================================================================
    // 1. FROST SERPENT — Snake of 14 blue ice segments + packed ice head.
    //    Sine-wave slither on ground, head tracks nearest player.
    //    14 body segments (BLUE_ICE) + 1 head (PACKED_ICE, larger) = 15 blocks
    // ================================================================
    public static class FrostSerpent extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> bodySegments = new ArrayList<>();
        private BlockDisplayHandle head;
        private double moveAngle = 0;
        private double pathProgress = 0;

        public FrostSerpent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_serpent", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
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

            // Spawn head — larger packed ice block
            Location headLoc = center.clone().add(0, 0.3, 0);
            head = displayBuilder.spawnBlock(headLoc, Material.PACKED_ICE);
            head.scale(1.4f, 1.0f, 1.4f)
                .glow(100, 180, 255)
                .interpolation(3, 0);
            spawnedEntities.add(head.entity());

            // Spawn 14 body segments — decreasing size toward tail
            for (int i = 0; i < 14; i++) {
                double offset = -(i + 1) * 0.8;
                Location segLoc = center.clone().add(0, 0.2, offset);
                float segScale = 1.2f - (i * 0.05f);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.BLUE_ICE);
                seg.scale(segScale, 0.7f, 0.8f)
                   .glow(30, 80, 160)
                   .interpolation(3, 0);
                spawnedEntities.add(seg.entity());
                bodySegments.add(seg);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            pathProgress += 0.06;
            moveAngle += 0.04;

            // Head position — circles and tracks toward nearest player
            double headX = Math.cos(pathProgress) * 4.0;
            double headZ = Math.sin(pathProgress) * 4.0;

            Player nearest = findNearestPlayer(c, 20);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - (c.getX() + headX);
                double dz = nearest.getLocation().getZ() - (c.getZ() + headZ);
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.5) {
                    headX += (dx / dist) * 0.3;
                    headZ += (dz / dist) * 0.3;
                }
            }

            Location headLoc = c.clone().add(headX, 0.3, headZ);
            head.entity().teleport(headLoc);
            setCenter(headLoc);

            // Body segments follow with sine-wave slither
            for (int i = 0; i < bodySegments.size(); i++) {
                double delay = (i + 1) * 0.3;
                double segAngle = pathProgress - delay;
                double segX = Math.cos(segAngle) * 4.0;
                double segZ = Math.sin(segAngle) * 4.0;
                // Add lateral sine wave for slithering effect
                double sineOffset = Math.sin(tick * 0.15 + i * 0.6) * 0.5;
                double perpX = -Math.sin(segAngle) * sineOffset;
                double perpZ = Math.cos(segAngle) * sineOffset;

                Location segLoc = c.clone().add(segX + perpX, 0.2, segZ + perpZ);
                bodySegments.get(i).entity().teleport(segLoc);
            }

            // Frost trail particles
            if (tick % 2 == 0) {
                headLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, headLoc, 3, 0.3, 0.2, 0.3, 0.01);
                DisplayBuilder.dustParticles(headLoc, 2, 0.3, 100, 180, 255, 1.0f);
            }

            // Hissing sound every 40 ticks
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(headLoc, Sound.BLOCK_POWDER_SNOW_STEP, 0.7f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostSerpent(plugin); }
    }

    // ================================================================
    // 2. ICE SPIDER — 8 legs of packed ice + 6 body blocks of blue ice.
    //    Walking animation with leg cycling, lunges at players.
    //    6 body (BLUE_ICE) + 8 legs (PACKED_ICE) = 14 blocks
    // ================================================================
    public static class IceSpider extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private double posX, posZ;
        private double targetX, targetZ;
        private boolean lunging = false;
        private int lungeTick = 0;

        public IceSpider(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_spider", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.posX = center.getX();
            this.posZ = center.getZ();
            this.targetX = posX;
            this.targetZ = posZ;
            World w = center.getWorld();
            if (w == null) return;

            // Body: 2 wide x 1 tall x 3 long thorax+abdomen (6 blocks of BLUE_ICE)
            double[][] bodyOffsets = {
                {-0.5, 0.8, -0.5}, {0.5, 0.8, -0.5},  // rear abdomen
                {-0.5, 0.8, 0.0},  {0.5, 0.8, 0.0},    // mid thorax
                {-0.3, 1.0, 0.5},  {0.3, 1.0, 0.5}      // head (slightly raised)
            };
            for (double[] off : bodyOffsets) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BLUE_ICE);
                float sc = (off[1] > 0.9) ? 0.7f : 0.9f;
                b.scale(sc, 0.6f, sc).glow(100, 180, 255).interpolation(3, 0);
                spawnedEntities.add(b.entity());
                bodyBlocks.add(b);
            }

            // Legs: 8 legs, 4 on each side, angled outward (PACKED_ICE, thin)
            double[][] legBases = {
                {-1.2, 0.3, -0.6}, {1.2, 0.3, -0.6},   // rear pair
                {-1.3, 0.3, -0.2}, {1.3, 0.3, -0.2},   // mid-rear pair
                {-1.3, 0.3, 0.2},  {1.3, 0.3, 0.2},    // mid-front pair
                {-1.1, 0.3, 0.5},  {1.1, 0.3, 0.5}     // front pair
            };
            for (double[] off : legBases) {
                BlockDisplayHandle leg = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.PACKED_ICE);
                leg.scale(0.3f, 0.8f, 0.3f).glow(30, 80, 160).interpolation(3, 0);
                spawnedEntities.add(leg.entity());
                legs.add(leg);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Find nearest player and walk toward them
            Player nearest = findNearestPlayer(c, 25);
            if (nearest != null) {
                targetX = nearest.getLocation().getX();
                targetZ = nearest.getLocation().getZ();
            }

            double dx = targetX - posX;
            double dz = targetZ - posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);

            // Lunge if close enough
            if (dist < 5.0 && !lunging && tick % 60 < 2) {
                lunging = true;
                lungeTick = 0;
            }

            double speed = lunging ? 0.35 : 0.12;
            if (lunging) {
                lungeTick++;
                if (lungeTick > 15) lunging = false;
            }

            if (dist > 0.3) {
                posX += (dx / dist) * speed;
                posZ += (dz / dist) * speed;
            }

            Location spiderCenter = new Location(c.getWorld(), posX, c.getY(), posZ);
            spiderCenter.setYaw(0);
            spiderCenter.setPitch(0);
            setCenter(spiderCenter);

            // Move body blocks
            double[][] bodyOffsets = {
                {-0.5, 0.8, -0.5}, {0.5, 0.8, -0.5},
                {-0.5, 0.8, 0.0},  {0.5, 0.8, 0.0},
                {-0.3, 1.0, 0.5},  {0.3, 1.0, 0.5}
            };
            for (int i = 0; i < bodyBlocks.size(); i++) {
                Location bLoc = spiderCenter.clone().add(bodyOffsets[i][0], bodyOffsets[i][1], bodyOffsets[i][2]);
                bodyBlocks.get(i).entity().teleport(bLoc);
            }

            // Animate legs — alternating leg raise for walking gait
            double[][] legBases = {
                {-1.2, 0.3, -0.6}, {1.2, 0.3, -0.6},
                {-1.3, 0.3, -0.2}, {1.3, 0.3, -0.2},
                {-1.3, 0.3, 0.2},  {1.3, 0.3, 0.2},
                {-1.1, 0.3, 0.5},  {1.1, 0.3, 0.5}
            };
            for (int i = 0; i < legs.size(); i++) {
                double legLift = Math.sin(tick * 0.3 + i * Math.PI / 4) * 0.25;
                if (legLift < 0) legLift = 0;
                double forwardStep = Math.sin(tick * 0.3 + i * Math.PI / 4) * 0.2;
                Location legLoc = spiderCenter.clone().add(
                    legBases[i][0], legBases[i][1] + legLift, legBases[i][2] + forwardStep
                );
                legs.get(i).entity().teleport(legLoc);
            }

            // Frost particles from body
            if (tick % 3 == 0) {
                spiderCenter.getWorld().spawnParticle(Particle.SNOWFLAKE, spiderCenter.clone().add(0, 0.9, 0), 4, 0.5, 0.2, 0.5, 0.01);
            }

            // Lunge impact sound
            if (lunging && lungeTick == 8) {
                DisplayBuilder.playSound(spiderCenter, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
            }

            // Ambient sound
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(spiderCenter, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceSpider(plugin); }
    }

    // ================================================================
    // 3. CRYSTALLINE STALKER — Tall humanoid of 12 ice blocks.
    //    Jerky stop-motion teleport pursuit. Freezes in place, then teleports closer.
    //    12 blocks: 2 legs, 4 torso, 2 arms, 1 neck, 1 head, 2 shoulders = 12 blocks
    // ================================================================
    public static class CrystallineStalker extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private double posX, posY, posZ;
        private int freezeTimer = 0;
        private boolean frozen = true;
        private double nextX, nextZ;

        public CrystallineStalker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_stalker", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.posX = center.getX();
            this.posY = center.getY();
            this.posZ = center.getZ();
            this.nextX = posX;
            this.nextZ = posZ;
            World w = center.getWorld();
            if (w == null) return;

            // Build humanoid figure: feet to head
            // Legs (2 blocks)
            spawnPart(center, 0.3, 0.0, 0.0, Material.PACKED_ICE, 0.5f, 1.0f, 0.5f);   // left leg
            spawnPart(center, -0.3, 0.0, 0.0, Material.PACKED_ICE, 0.5f, 1.0f, 0.5f);  // right leg
            // Torso (4 blocks, 2x2 tall)
            spawnPart(center, -0.3, 1.0, 0.0, Material.BLUE_ICE, 0.8f, 0.8f, 0.6f);
            spawnPart(center, 0.3, 1.0, 0.0, Material.BLUE_ICE, 0.8f, 0.8f, 0.6f);
            spawnPart(center, -0.3, 1.8, 0.0, Material.BLUE_ICE, 0.8f, 0.8f, 0.6f);
            spawnPart(center, 0.3, 1.8, 0.0, Material.BLUE_ICE, 0.8f, 0.8f, 0.6f);
            // Shoulders (2 blocks)
            spawnPart(center, -0.9, 2.2, 0.0, Material.PRISMARINE, 0.6f, 0.5f, 0.5f);
            spawnPart(center, 0.9, 2.2, 0.0, Material.PRISMARINE, 0.6f, 0.5f, 0.5f);
            // Arms (2 blocks, hanging from shoulders)
            spawnPart(center, -1.0, 1.4, 0.0, Material.ICE, 0.35f, 1.0f, 0.35f);
            spawnPart(center, 1.0, 1.4, 0.0, Material.ICE, 0.35f, 1.0f, 0.35f);
            // Neck (1 block)
            spawnPart(center, 0.0, 2.6, 0.0, Material.BONE_BLOCK, 0.4f, 0.4f, 0.4f);
            // Head (1 block, larger)
            spawnPart(center, 0.0, 3.0, 0.0, Material.LIGHT_BLUE_STAINED_GLASS, 0.9f, 0.9f, 0.9f);

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.4f);
        }

        private void spawnPart(Location base, double ox, double oy, double oz, Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(220, 240, 255).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            parts.add(h);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            freezeTimer++;

            // Alternate: freeze for 20 ticks, then teleport instantly
            if (frozen) {
                if (freezeTimer >= 20) {
                    frozen = false;
                    freezeTimer = 0;

                    // Calculate teleport destination toward nearest player
                    Player nearest = findNearestPlayer(new Location(c.getWorld(), posX, posY, posZ), 30);
                    if (nearest != null) {
                        double dx = nearest.getLocation().getX() - posX;
                        double dz = nearest.getLocation().getZ() - posZ;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 2.0) {
                            double jump = Math.min(dist * 0.6, 5.0);
                            nextX = posX + (dx / dist) * jump;
                            nextZ = posZ + (dz / dist) * jump;
                        }
                    }
                }
                // While frozen, emit subtle particles
                if (tick % 5 == 0) {
                    Location headLoc = new Location(c.getWorld(), posX, posY + 3.0, posZ);
                    headLoc.getWorld().spawnParticle(Particle.END_ROD, headLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            } else {
                // Teleport instantly (stop-motion jerk)
                posX = nextX;
                posZ = nextZ;

                Location stalkerBase = new Location(c.getWorld(), posX, posY, posZ);
                stalkerBase.setYaw(0);
                stalkerBase.setPitch(0);
                setCenter(stalkerBase.clone().add(0, 1.5, 0));

                // Reposition all parts
                double[][] offsets = {
                    {0.3, 0.0, 0.0}, {-0.3, 0.0, 0.0},
                    {-0.3, 1.0, 0.0}, {0.3, 1.0, 0.0},
                    {-0.3, 1.8, 0.0}, {0.3, 1.8, 0.0},
                    {-0.9, 2.2, 0.0}, {0.9, 2.2, 0.0},
                    {-1.0, 1.4, 0.0}, {1.0, 1.4, 0.0},
                    {0.0, 2.6, 0.0}, {0.0, 3.0, 0.0}
                };
                for (int i = 0; i < parts.size() && i < offsets.length; i++) {
                    Location pLoc = stalkerBase.clone().add(offsets[i][0], offsets[i][1], offsets[i][2]);
                    parts.get(i).entity().teleport(pLoc);
                }

                // Teleport effects
                DisplayBuilder.playSound(stalkerBase, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.8f);
                DisplayBuilder.dustParticles(stalkerBase, 15, 1.0, 220, 240, 255, 1.5f);
                stalkerBase.getWorld().spawnParticle(Particle.SNOWFLAKE, stalkerBase.clone().add(0, 1.5, 0), 10, 0.5, 1.0, 0.5, 0.02);

                frozen = true;
                freezeTimer = 0;
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineStalker(plugin); }
    }

    // ================================================================
    // 4. FROST WORM — 16 segments arcing through the air like a worm.
    //    Sine-wave vertical arc, moving across the arena.
    //    16 body segments (BLUE_ICE / PACKED_ICE alternating) = 16 blocks
    // ================================================================
    public static class FrostWorm extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private double pathAngle = 0;
        private static final int SEGMENT_COUNT = 16;

        public FrostWorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_worm", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
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

            for (int i = 0; i < SEGMENT_COUNT; i++) {
                Material mat = (i % 2 == 0) ? Material.BLUE_ICE : Material.PACKED_ICE;
                Location segLoc = center.clone().add(i * 0.7, 2.0, 0);
                float scale = (i == 0) ? 1.3f : (i < 3 || i > 13) ? 0.7f : 0.9f;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, mat);
                seg.scale(scale, scale, scale)
                   .glow(100, 180, 255)
                   .interpolation(3, 0);
                spawnedEntities.add(seg.entity());
                segments.add(seg);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            pathAngle += 0.05;

            // Worm moves forward along a circular path
            double centerX = c.getX() + Math.cos(pathAngle) * 6.0;
            double centerZ = c.getZ() + Math.sin(pathAngle) * 6.0;

            for (int i = 0; i < segments.size(); i++) {
                double segDelay = i * 0.25;
                double segPathAngle = pathAngle - segDelay * 0.05;
                double sx = c.getX() + Math.cos(segPathAngle) * 6.0 - (i * 0.5 * Math.cos(pathAngle));
                double sz = c.getZ() + Math.sin(segPathAngle) * 6.0 - (i * 0.5 * Math.sin(pathAngle));
                // Vertical arc: sine wave makes worm "dive" in and out
                double sy = c.getY() + Math.sin(tick * 0.08 + i * 0.5) * 3.5 + 2.0;

                Location segLoc = new Location(c.getWorld(), sx, sy, sz);
                segLoc.setYaw(0);
                segLoc.setPitch(0);
                segments.get(i).entity().teleport(segLoc);

                // Snow trail from each segment
                if (tick % 4 == 0 && i % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, segLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }

            // Update damage center to head position
            Location headLoc = segments.get(0).entity().getLocation();
            setCenter(headLoc);

            // Sound as worm surfaces
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(headLoc, Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 0.5f);
            }

            // Frost dust at head
            if (tick % 2 == 0) {
                DisplayBuilder.dustParticles(headLoc, 3, 0.4, 30, 80, 160, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostWorm(plugin); }
    }

    // ================================================================
    // 5. ICE SWARM — 12 small (scale 0.4) ice cubes orbiting a moving center.
    //    Center drifts toward players, cubes orbit at varying heights and speeds.
    //    12 cubes (mixed ICE, PACKED_ICE, BLUE_ICE) = 12 blocks
    // ================================================================
    public static class IceSwarm extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> cubes = new ArrayList<>();
        private final double[] orbitSpeeds = new double[12];
        private final double[] orbitRadii = new double[12];
        private final double[] orbitHeights = new double[12];
        private final double[] orbitPhases = new double[12];
        private double driftX, driftZ;

        public IceSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_swarm", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.driftX = center.getX();
            this.driftZ = center.getZ();
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE};
            for (int i = 0; i < 12; i++) {
                orbitSpeeds[i] = 0.06 + Math.random() * 0.08;
                orbitRadii[i] = 1.5 + Math.random() * 2.5;
                orbitHeights[i] = 0.5 + Math.random() * 3.0;
                orbitPhases[i] = Math.random() * Math.PI * 2;

                Material mat = mats[i % 3];
                BlockDisplayHandle cube = displayBuilder.spawnBlock(center.clone().add(0, orbitHeights[i], 0), mat);
                cube.scale(0.4f, 0.4f, 0.4f)
                    .glow(150, 210, 255)
                    .interpolation(2, 0);
                spawnedEntities.add(cube.entity());
                cubes.add(cube);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drift center toward nearest player
            Player nearest = findNearestPlayer(new Location(c.getWorld(), driftX, c.getY(), driftZ), 25);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - driftX;
                double dz = nearest.getLocation().getZ() - driftZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 1.0) {
                    driftX += (dx / dist) * 0.1;
                    driftZ += (dz / dist) * 0.1;
                }
            }

            Location swarmCenter = new Location(c.getWorld(), driftX, c.getY(), driftZ);
            swarmCenter.setYaw(0);
            swarmCenter.setPitch(0);
            setCenter(swarmCenter.clone().add(0, 1.5, 0));

            // Orbit each cube
            for (int i = 0; i < cubes.size(); i++) {
                double angle = tick * orbitSpeeds[i] + orbitPhases[i];
                double ox = Math.cos(angle) * orbitRadii[i];
                double oz = Math.sin(angle) * orbitRadii[i];
                double oy = orbitHeights[i] + Math.sin(tick * 0.1 + i) * 0.5;

                Location cubeLoc = swarmCenter.clone().add(ox, oy, oz);
                cubes.get(i).entity().teleport(cubeLoc);
            }

            // Ambient particles at swarm center
            if (tick % 3 == 0) {
                swarmCenter.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    swarmCenter.clone().add(0, 1.5, 0), 5, 1.5, 1.0, 1.5, 0.02);
            }

            // Buzzing ice sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(swarmCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceSwarm(plugin); }
    }

    // ================================================================
    // 6. GLACIAL CRAB — Flat body + 6 legs + 4 claw blocks. Sideways scuttle.
    //    Body: 4 flat BLUE_ICE, Legs: 6 PACKED_ICE, Claws: 2 big + 2 small PRISMARINE = 14 blocks
    // ================================================================
    public static class GlacialCrab extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> bodyParts = new ArrayList<>();
        private final List<BlockDisplayHandle> legParts = new ArrayList<>();
        private final List<BlockDisplayHandle> clawParts = new ArrayList<>();
        private double posX, posZ;
        private double moveDir = 0; // radians, crab moves sideways
        private int scuttleCycle = 0;

        public GlacialCrab(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_crab", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.posX = center.getX();
            this.posZ = center.getZ();
            World w = center.getWorld();
            if (w == null) return;

            // Body: 4 flat wide blocks (2x1x2 grid, low profile)
            double[][] bodyOff = {{-0.4, 0.3, -0.4}, {0.4, 0.3, -0.4}, {-0.4, 0.3, 0.4}, {0.4, 0.3, 0.4}};
            for (double[] off : bodyOff) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BLUE_ICE);
                b.scale(0.9f, 0.35f, 0.9f).glow(30, 80, 160).interpolation(3, 0);
                spawnedEntities.add(b.entity());
                bodyParts.add(b);
            }

            // Legs: 6 thin legs, 3 per side
            double[][] legOff = {
                {-1.1, 0.1, -0.5}, {-1.2, 0.1, 0.0}, {-1.1, 0.1, 0.5},
                {1.1, 0.1, -0.5},  {1.2, 0.1, 0.0},  {1.1, 0.1, 0.5}
            };
            for (double[] off : legOff) {
                BlockDisplayHandle leg = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.PACKED_ICE);
                leg.scale(0.3f, 0.5f, 0.25f).glow(100, 180, 255).interpolation(3, 0);
                spawnedEntities.add(leg.entity());
                legParts.add(leg);
            }

            // Claws: 2 large + 2 small (front)
            BlockDisplayHandle clawL = displayBuilder.spawnBlock(center.clone().add(-0.8, 0.4, 1.2), Material.PRISMARINE);
            clawL.scale(0.7f, 0.5f, 0.9f).glow(100, 180, 255).interpolation(3, 0);
            spawnedEntities.add(clawL.entity());
            clawParts.add(clawL);

            BlockDisplayHandle clawLTip = displayBuilder.spawnBlock(center.clone().add(-0.8, 0.6, 1.8), Material.PRISMARINE);
            clawLTip.scale(0.4f, 0.3f, 0.5f).glow(150, 210, 255).interpolation(3, 0);
            spawnedEntities.add(clawLTip.entity());
            clawParts.add(clawLTip);

            BlockDisplayHandle clawR = displayBuilder.spawnBlock(center.clone().add(0.8, 0.4, 1.2), Material.PRISMARINE);
            clawR.scale(0.7f, 0.5f, 0.9f).glow(100, 180, 255).interpolation(3, 0);
            spawnedEntities.add(clawR.entity());
            clawParts.add(clawR);

            BlockDisplayHandle clawRTip = displayBuilder.spawnBlock(center.clone().add(0.8, 0.6, 1.8), Material.PRISMARINE);
            clawRTip.scale(0.4f, 0.3f, 0.5f).glow(150, 210, 255).interpolation(3, 0);
            spawnedEntities.add(clawRTip.entity());
            clawParts.add(clawRTip);

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            scuttleCycle++;

            // Face nearest player, then scuttle sideways
            Player nearest = findNearestPlayer(new Location(c.getWorld(), posX, c.getY(), posZ), 25);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - posX;
                double dz = nearest.getLocation().getZ() - posZ;
                moveDir = Math.atan2(dz, dx);
            }

            // Sideways scuttle — move perpendicular to facing, alternating left/right
            double sideAngle = moveDir + Math.PI / 2;
            double sideDir = (scuttleCycle % 60 < 30) ? 1.0 : -1.0;
            // Also approach player slowly
            posX += Math.cos(moveDir) * 0.06 + Math.cos(sideAngle) * 0.1 * sideDir;
            posZ += Math.sin(moveDir) * 0.06 + Math.sin(sideAngle) * 0.1 * sideDir;

            Location crabBase = new Location(c.getWorld(), posX, c.getY(), posZ);
            crabBase.setYaw(0);
            crabBase.setPitch(0);
            setCenter(crabBase.clone().add(0, 0.4, 0));

            // Reposition body
            double[][] bodyOff = {{-0.4, 0.3, -0.4}, {0.4, 0.3, -0.4}, {-0.4, 0.3, 0.4}, {0.4, 0.3, 0.4}};
            for (int i = 0; i < bodyParts.size(); i++) {
                bodyParts.get(i).entity().teleport(crabBase.clone().add(bodyOff[i][0], bodyOff[i][1], bodyOff[i][2]));
            }

            // Animate legs with scuttle bob
            double[][] legOff = {
                {-1.1, 0.1, -0.5}, {-1.2, 0.1, 0.0}, {-1.1, 0.1, 0.5},
                {1.1, 0.1, -0.5},  {1.2, 0.1, 0.0},  {1.1, 0.1, 0.5}
            };
            for (int i = 0; i < legParts.size(); i++) {
                double bob = Math.abs(Math.sin(tick * 0.4 + i * 1.0)) * 0.15;
                legParts.get(i).entity().teleport(crabBase.clone().add(legOff[i][0], legOff[i][1] + bob, legOff[i][2]));
            }

            // Animate claws — pinching motion
            double pinch = Math.sin(tick * 0.15) * 0.3;
            double[][] clawOff = {
                {-0.8, 0.4, 1.2}, {-0.8, 0.6, 1.8 + pinch},
                {0.8, 0.4, 1.2},  {0.8, 0.6, 1.8 + pinch}
            };
            for (int i = 0; i < clawParts.size(); i++) {
                clawParts.get(i).entity().teleport(crabBase.clone().add(clawOff[i][0], clawOff[i][1], clawOff[i][2]));
            }

            // Scuttle particles
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(crabBase, 4, 0.8, 100, 180, 255, 0.8f);
            }

            // Scuttle sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(crabBase, Sound.BLOCK_POWDER_SNOW_STEP, 0.6f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialCrab(plugin); }
    }

    // ================================================================
    // 7. FROST BAT — 6 body blocks + 8 wing blocks (white glass). Circling flight, dive-bomb.
    //    Body: 4 BLUE_ICE + 2 PACKED_ICE head. Wings: 8 LIGHT_BLUE_STAINED_GLASS = 14 blocks
    // ================================================================
    public static class FrostBat extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> wings = new ArrayList<>();
        private double flightAngle = 0;
        private double flightHeight;
        private boolean diving = false;
        private int diveTick = 0;
        private double diveStartY;

        public FrostBat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_bat", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.flightHeight = center.getY() + 8.0;
            this.diveStartY = flightHeight;
            World w = center.getWorld();
            if (w == null) return;

            Location batCenter = center.clone().add(0, 8, 0);

            // Body: elongated bat torso (4 BLUE_ICE, 2 PACKED_ICE head)
            BlockDisplayHandle head1 = displayBuilder.spawnBlock(batCenter.clone().add(0, 0, 0.6), Material.PACKED_ICE);
            head1.scale(0.6f, 0.5f, 0.5f).glow(220, 240, 255).interpolation(3, 0);
            spawnedEntities.add(head1.entity()); body.add(head1);

            BlockDisplayHandle head2 = displayBuilder.spawnBlock(batCenter.clone().add(0, 0.3, 0.6), Material.PACKED_ICE);
            head2.scale(0.5f, 0.4f, 0.4f).glow(220, 240, 255).interpolation(3, 0);
            spawnedEntities.add(head2.entity()); body.add(head2);

            // Main torso
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle torso = displayBuilder.spawnBlock(batCenter.clone().add(0, 0, -i * 0.4), Material.BLUE_ICE);
                float sx = (i == 0 || i == 3) ? 0.5f : 0.7f;
                torso.scale(sx, 0.4f, 0.5f).glow(30, 80, 160).interpolation(3, 0);
                spawnedEntities.add(torso.entity()); body.add(torso);
            }

            // Wings: 4 segments each side (LIGHT_BLUE_STAINED_GLASS, thin and wide)
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 4; j++) {
                    double wx = side * (0.6 + j * 0.8);
                    double wz = -j * 0.15;
                    float wingWidth = (j < 2) ? 0.8f : 0.6f;
                    BlockDisplayHandle wing = displayBuilder.spawnBlock(batCenter.clone().add(wx, 0, wz), Material.LIGHT_BLUE_STAINED_GLASS);
                    wing.scale(wingWidth, 0.12f, 0.6f).glow(150, 210, 255).interpolation(3, 0);
                    spawnedEntities.add(wing.entity());
                    wings.add(wing);
                }
            }

            DisplayBuilder.playSound(batCenter, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
            DisplayBuilder.playSound(batCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            flightAngle += 0.04;

            // Dive-bomb every 80 ticks
            if (!diving && tick % 80 == 60) {
                diving = true;
                diveTick = 0;
                diveStartY = flightHeight;
            }

            double currentY;
            if (diving) {
                diveTick++;
                if (diveTick < 15) {
                    // Dive down
                    currentY = diveStartY - (diveTick * 0.5);
                } else if (diveTick < 30) {
                    // Pull back up
                    currentY = diveStartY - 7.5 + ((diveTick - 15) * 0.5);
                } else {
                    diving = false;
                    currentY = flightHeight;
                }
            } else {
                currentY = flightHeight + Math.sin(tick * 0.08) * 1.0;
            }

            double bx = c.getX() + Math.cos(flightAngle) * 5.0;
            double bz = c.getZ() + Math.sin(flightAngle) * 5.0;

            Location batCenter = new Location(c.getWorld(), bx, currentY, bz);
            batCenter.setYaw(0);
            batCenter.setPitch(0);
            setCenter(batCenter);

            // Move body
            double[][] bodyOff = {
                {0, 0, 0.6}, {0, 0.3, 0.6},
                {0, 0, 0}, {0, 0, -0.4}, {0, 0, -0.8}, {0, 0, -1.2}
            };
            for (int i = 0; i < body.size(); i++) {
                body.get(i).entity().teleport(batCenter.clone().add(bodyOff[i][0], bodyOff[i][1], bodyOff[i][2]));
            }

            // Animate wings — flapping
            double flapAngle = Math.sin(tick * 0.3) * 0.6;
            int wingIdx = 0;
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 4; j++) {
                    double wx = side * (0.6 + j * 0.8);
                    double wz = -j * 0.15;
                    double wy = flapAngle * (j + 1) * 0.3 * side;
                    if (wingIdx < wings.size()) {
                        wings.get(wingIdx).entity().teleport(batCenter.clone().add(wx, wy, wz));
                    }
                    wingIdx++;
                }
            }

            // Particles
            if (tick % 3 == 0) {
                batCenter.getWorld().spawnParticle(Particle.SNOWFLAKE, batCenter, 3, 1.0, 0.3, 1.0, 0.01);
            }

            // Dive sound
            if (diving && diveTick == 1) {
                DisplayBuilder.playSound(batCenter, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
            }

            // Wing flap sound
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(batCenter, Sound.BLOCK_POWDER_SNOW_STEP, 0.4f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostBat(plugin); }
    }

    // ================================================================
    // 8. CRYSTAL JELLYFISH — 8 glass dome blocks + 8 ice tentacles. Pulsing bell overhead.
    //    Dome: 8 LIGHT_BLUE_STAINED_GLASS. Tentacles: 8 ICE (thin, hanging).
    //    Floats above players, bell pulses (scale changes), tentacles sway = 16 blocks
    // ================================================================
    public static class CrystalJellyfish extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> dome = new ArrayList<>();
        private final List<BlockDisplayHandle> tentacles = new ArrayList<>();
        private double floatX, floatZ;
        private static final double FLOAT_HEIGHT = 7.0;

        public CrystalJellyfish(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_jellyfish", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.floatX = center.getX();
            this.floatZ = center.getZ();
            World w = center.getWorld();
            if (w == null) return;

            Location jellyCenter = center.clone().add(0, FLOAT_HEIGHT, 0);

            // Dome: 8 blocks arranged in a dome shape (hemisphere)
            // Ring of 6 around + 1 top + 1 center
            BlockDisplayHandle top = displayBuilder.spawnBlock(jellyCenter.clone().add(0, 0.8, 0), Material.LIGHT_BLUE_STAINED_GLASS);
            top.scale(0.8f, 0.5f, 0.8f).glow(150, 210, 255).interpolation(4, 0);
            spawnedEntities.add(top.entity()); dome.add(top);

            BlockDisplayHandle mid = displayBuilder.spawnBlock(jellyCenter, Material.LIGHT_BLUE_STAINED_GLASS);
            mid.scale(1.0f, 0.4f, 1.0f).glow(150, 210, 255).interpolation(4, 0);
            spawnedEntities.add(mid.entity()); dome.add(mid);

            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double dx = Math.cos(angle) * 0.7;
                double dz = Math.sin(angle) * 0.7;
                BlockDisplayHandle domeBlock = displayBuilder.spawnBlock(jellyCenter.clone().add(dx, 0.2, dz), Material.LIGHT_BLUE_STAINED_GLASS);
                domeBlock.scale(0.6f, 0.5f, 0.6f).glow(100, 180, 255).interpolation(4, 0);
                spawnedEntities.add(domeBlock.entity()); dome.add(domeBlock);
            }

            // Tentacles: 8 thin hanging ice blocks
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double dx = Math.cos(angle) * 0.6;
                double dz = Math.sin(angle) * 0.6;
                BlockDisplayHandle tent = displayBuilder.spawnBlock(jellyCenter.clone().add(dx, -1.2, dz), Material.ICE);
                tent.scale(0.15f, 1.5f, 0.15f).glow(220, 240, 255).interpolation(4, 0);
                spawnedEntities.add(tent.entity()); tentacles.add(tent);
            }

            DisplayBuilder.playSound(jellyCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
            DisplayBuilder.playSound(jellyCenter, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drift toward nearest player
            Player nearest = findNearestPlayer(new Location(c.getWorld(), floatX, c.getY(), floatZ), 25);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - floatX;
                double dz = nearest.getLocation().getZ() - floatZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 1.0) {
                    floatX += (dx / dist) * 0.07;
                    floatZ += (dz / dist) * 0.07;
                }
            }

            // Bob up and down gently
            double bobY = c.getY() + FLOAT_HEIGHT + Math.sin(tick * 0.06) * 0.8;
            Location jellyCenter = new Location(c.getWorld(), floatX, bobY, floatZ);
            jellyCenter.setYaw(0);
            jellyCenter.setPitch(0);
            setCenter(jellyCenter);

            // Pulse: scale the dome blocks with a breathing effect
            float pulseScale = 1.0f + (float) Math.sin(tick * 0.1) * 0.15f;

            // Reposition dome top + center
            dome.get(0).entity().teleport(jellyCenter.clone().add(0, 0.8 * pulseScale, 0));
            dome.get(0).scale(0.8f * pulseScale, 0.5f, 0.8f * pulseScale);

            dome.get(1).entity().teleport(jellyCenter);
            dome.get(1).scale(1.0f * pulseScale, 0.4f, 1.0f * pulseScale);

            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double dx = Math.cos(angle) * 0.7 * pulseScale;
                double dz = Math.sin(angle) * 0.7 * pulseScale;
                dome.get(i + 2).entity().teleport(jellyCenter.clone().add(dx, 0.2, dz));
                dome.get(i + 2).scale(0.6f * pulseScale, 0.5f, 0.6f * pulseScale);
            }

            // Tentacles: sway and stretch
            for (int i = 0; i < tentacles.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double swayX = Math.sin(tick * 0.08 + i * 0.8) * 0.4;
                double swayZ = Math.cos(tick * 0.08 + i * 0.8) * 0.4;
                double dx = Math.cos(angle) * 0.6 + swayX;
                double dz = Math.sin(angle) * 0.6 + swayZ;
                double tentLen = 1.2 + Math.sin(tick * 0.1) * 0.4;
                tentacles.get(i).entity().teleport(jellyCenter.clone().add(dx, -tentLen, dz));
                tentacles.get(i).scale(0.15f, 1.5f + (float)(tentLen - 1.2) * 0.5f, 0.15f);
            }

            // Ambient glow particles
            if (tick % 4 == 0) {
                jellyCenter.getWorld().spawnParticle(Particle.END_ROD, jellyCenter, 3, 0.8, 0.5, 0.8, 0.01);
                DisplayBuilder.dustParticles(jellyCenter.clone().add(0, -1.5, 0), 2, 0.5, 150, 210, 255, 0.8f);
            }

            // Pulse sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(jellyCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalJellyfish(plugin); }
    }

    // ================================================================
    // 9. ICE MIMIC — Starts as 1 block, unfolds into 14-block creature after 30 ticks.
    //    Phase 1 (ticks 0-30): single PACKED_ICE block, stationary, innocent.
    //    Phase 2 (ticks 30+): unfolds into 14-block spider-like horror.
    //    1 initial + 13 unfolding = 14 blocks
    // ================================================================
    public static class IceMimic extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle initialBlock;
        private final List<BlockDisplayHandle> unfoldedParts = new ArrayList<>();
        private boolean unfolded = false;
        private double posX, posZ;

        public IceMimic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mimic", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.posX = center.getX();
            this.posZ = center.getZ();
            World w = center.getWorld();
            if (w == null) return;

            // Start as innocent-looking single block
            initialBlock = displayBuilder.spawnBlock(center, Material.PACKED_ICE);
            initialBlock.scale(1.0f, 1.0f, 1.0f).interpolation(2, 0);
            spawnedEntities.add(initialBlock.entity());
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Sit still, look like a normal block
            if (tick < 30) {
                // Subtle wobble to hint it's alive
                if (tick > 20 && tick % 5 == 0) {
                    initialBlock.scale(1.0f + (float)Math.sin(tick * 0.5) * 0.03f, 1.0f, 1.0f);
                }
                return;
            }

            // Phase 2: UNFOLD (one-time setup)
            if (!unfolded) {
                unfolded = true;
                initialBlock.glow(100, 180, 255);

                Location base = new Location(c.getWorld(), posX, c.getY(), posZ);
                base.setYaw(0);
                base.setPitch(0);

                // Core body (3 blocks, vertical stack)
                for (int i = 0; i < 3; i++) {
                    BlockDisplayHandle core = displayBuilder.spawnBlock(base.clone().add(0, 0.5 + i * 0.6, 0), Material.BLUE_ICE);
                    core.scale(0.8f, 0.6f, 0.8f).glow(30, 80, 160).interpolation(5, 0);
                    spawnedEntities.add(core.entity());
                    unfoldedParts.add(core);
                }

                // Head (1 block, top, glowing)
                BlockDisplayHandle head = displayBuilder.spawnBlock(base.clone().add(0, 2.3, 0), Material.LIGHT_BLUE_STAINED_GLASS);
                head.scale(0.7f, 0.7f, 0.7f).glow(220, 240, 255).interpolation(5, 0);
                spawnedEntities.add(head.entity());
                unfoldedParts.add(head);

                // Arms/appendages (4 blocks, extending outward)
                double[][] armOff = {{-1.0, 1.2, 0.3}, {1.0, 1.2, 0.3}, {-0.8, 0.8, -0.5}, {0.8, 0.8, -0.5}};
                for (double[] off : armOff) {
                    BlockDisplayHandle arm = displayBuilder.spawnBlock(base.clone().add(off[0], off[1], off[2]), Material.ICE);
                    arm.scale(0.4f, 0.3f, 0.8f).glow(100, 180, 255).interpolation(5, 0);
                    spawnedEntities.add(arm.entity());
                    unfoldedParts.add(arm);
                }

                // Legs/supports (5 blocks)
                double[][] legOff = {
                    {-0.6, 0.0, 0.6}, {0.6, 0.0, 0.6}, {-0.6, 0.0, -0.6},
                    {0.6, 0.0, -0.6}, {0.0, 0.0, -0.8}
                };
                for (double[] off : legOff) {
                    BlockDisplayHandle leg = displayBuilder.spawnBlock(base.clone().add(off[0], off[1], off[2]), Material.PACKED_ICE);
                    leg.scale(0.3f, 0.5f, 0.3f).glow(150, 210, 255).interpolation(5, 0);
                    spawnedEntities.add(leg.entity());
                    unfoldedParts.add(leg);
                }

                // Dramatic unfold sounds
                DisplayBuilder.playSound(base, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
                DisplayBuilder.playSound(base, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.6f);
                DisplayBuilder.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.4f);

                // Explosion of particles
                base.getWorld().spawnParticle(Particle.SNOWFLAKE, base.clone().add(0, 1, 0), 30, 1.5, 1.0, 1.5, 0.05);
                DisplayBuilder.dustParticles(base.clone().add(0, 1, 0), 20, 1.5, 100, 180, 255, 1.5f);
            }

            // Chase nearest player
            Player nearest = findNearestPlayer(new Location(c.getWorld(), posX, c.getY(), posZ), 25);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - posX;
                double dz = nearest.getLocation().getZ() - posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 1.0) {
                    posX += (dx / dist) * 0.15;
                    posZ += (dz / dist) * 0.15;
                }
            }

            Location base = new Location(c.getWorld(), posX, c.getY(), posZ);
            base.setYaw(0);
            base.setPitch(0);
            setCenter(base.clone().add(0, 1.0, 0));

            // Move initial block to base
            initialBlock.entity().teleport(base);

            // Move all unfolded parts with shambling animation
            double shamble = Math.sin(tick * 0.2) * 0.15;
            double[][] allOff = {
                {0, 0.5, 0}, {0, 1.1, 0}, {0, 1.7, 0},   // core
                {0, 2.3, 0},                                  // head
                {-1.0, 1.2, 0.3}, {1.0, 1.2, 0.3}, {-0.8, 0.8, -0.5}, {0.8, 0.8, -0.5}, // arms
                {-0.6, 0.0, 0.6}, {0.6, 0.0, 0.6}, {-0.6, 0.0, -0.6}, {0.6, 0.0, -0.6}, {0.0, 0.0, -0.8} // legs
            };
            for (int i = 0; i < unfoldedParts.size() && i < allOff.length; i++) {
                double animY = (i < 4) ? shamble : 0; // Body shambles, legs stay grounded
                double legAnim = (i >= 8) ? Math.abs(Math.sin(tick * 0.3 + i)) * 0.1 : 0;
                unfoldedParts.get(i).entity().teleport(
                    base.clone().add(allOff[i][0], allOff[i][1] + animY + legAnim, allOff[i][2])
                );
            }

            // Ambient particles
            if (tick % 3 == 0) {
                base.getWorld().spawnParticle(Particle.SNOWFLAKE, base.clone().add(0, 1.5, 0), 3, 0.5, 0.5, 0.5, 0.01);
            }

            if (tick % 45 == 0) {
                DisplayBuilder.playSound(base, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceMimic(plugin); }
    }

    // ================================================================
    // 10. PERMAFROST BEETLE — 8 shell blocks + 6 legs. Charges forward, rolls into ball.
    //     Shell: 6 BLUE_ICE + 2 PRISMARINE. Legs: 6 PACKED_ICE. = 14 blocks
    //     Alternates: walk -> charge -> roll into ball -> unroll
    // ================================================================
    public static class PermafrostBeetle extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> shell = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private double posX, posZ;
        private double facingAngle = 0;
        private int phase = 0; // 0=walk, 1=charge, 2=roll
        private int phaseTimer = 0;

        public PermafrostBeetle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_beetle", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.posX = center.getX();
            this.posZ = center.getZ();
            World w = center.getWorld();
            if (w == null) return;

            // Shell: dome-like top (6 BLUE_ICE + 2 PRISMARINE accent)
            double[][] shellOff = {
                {-0.4, 0.5, -0.4}, {0.4, 0.5, -0.4},  // rear shell
                {-0.4, 0.5, 0.0},  {0.4, 0.5, 0.0},    // mid shell
                {-0.3, 0.7, -0.2}, {0.3, 0.7, -0.2},   // top ridge (raised)
                {0.0, 0.5, 0.5},                          // front (head area, PRISMARINE)
                {0.0, 0.7, 0.0}                           // top center (PRISMARINE)
            };
            Material[] shellMats = {
                Material.BLUE_ICE, Material.BLUE_ICE, Material.BLUE_ICE, Material.BLUE_ICE,
                Material.BLUE_ICE, Material.BLUE_ICE, Material.PRISMARINE, Material.PRISMARINE
            };
            for (int i = 0; i < shellOff.length; i++) {
                BlockDisplayHandle s = displayBuilder.spawnBlock(center.clone().add(shellOff[i][0], shellOff[i][1], shellOff[i][2]), shellMats[i]);
                float sc = (shellOff[i][1] > 0.6) ? 0.6f : 0.8f;
                s.scale(sc, 0.4f, sc).glow(100, 180, 255).interpolation(3, 0);
                spawnedEntities.add(s.entity());
                shell.add(s);
            }

            // Legs: 6 short legs, 3 per side
            double[][] legOff = {
                {-0.8, 0.1, -0.3}, {-0.9, 0.1, 0.0}, {-0.8, 0.1, 0.3},
                {0.8, 0.1, -0.3},  {0.9, 0.1, 0.0},  {0.8, 0.1, 0.3}
            };
            for (double[] off : legOff) {
                BlockDisplayHandle leg = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.PACKED_ICE);
                leg.scale(0.25f, 0.4f, 0.25f).glow(30, 80, 160).interpolation(3, 0);
                spawnedEntities.add(leg.entity());
                legs.add(leg);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            phaseTimer++;

            Player nearest = findNearestPlayer(new Location(c.getWorld(), posX, c.getY(), posZ), 25);

            // Phase cycling: walk 50 -> charge 25 -> roll 35 -> repeat
            if (phase == 0 && phaseTimer > 50) { phase = 1; phaseTimer = 0; }
            else if (phase == 1 && phaseTimer > 25) { phase = 2; phaseTimer = 0; }
            else if (phase == 2 && phaseTimer > 35) { phase = 0; phaseTimer = 0; }

            // Face player
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - posX;
                double dz = nearest.getLocation().getZ() - posZ;
                facingAngle = Math.atan2(dz, dx);
            }

            double speed;
            switch (phase) {
                case 1: speed = 0.3; break;   // charge
                case 2: speed = 0.4; break;   // roll (fastest)
                default: speed = 0.08; break;  // walk
            }

            posX += Math.cos(facingAngle) * speed;
            posZ += Math.sin(facingAngle) * speed;

            Location beetleBase = new Location(c.getWorld(), posX, c.getY(), posZ);
            beetleBase.setYaw(0);
            beetleBase.setPitch(0);
            setCenter(beetleBase.clone().add(0, 0.5, 0));

            if (phase == 2) {
                // ROLLING: compress all blocks into a ball shape, spin
                double rollAngle = tick * 0.3;
                for (int i = 0; i < shell.size(); i++) {
                    double a = rollAngle + (Math.PI * 2 * i) / shell.size();
                    double rx = Math.cos(a) * 0.4;
                    double ry = Math.sin(a) * 0.4 + 0.6;
                    shell.get(i).entity().teleport(beetleBase.clone().add(rx, ry, 0));
                    shell.get(i).scale(0.5f, 0.5f, 0.5f);
                }
                // Hide legs inside ball
                for (BlockDisplayHandle leg : legs) {
                    leg.entity().teleport(beetleBase.clone().add(0, 0.5, 0));
                    leg.scale(0.1f, 0.1f, 0.1f);
                }

                // Rolling particles
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(beetleBase, 5, 0.6, 100, 180, 255, 1.0f);
                }
                if (phaseTimer == 1) {
                    DisplayBuilder.playSound(beetleBase, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.2f);
                }
            } else {
                // WALKING/CHARGING: normal body layout
                double[][] shellOff = {
                    {-0.4, 0.5, -0.4}, {0.4, 0.5, -0.4},
                    {-0.4, 0.5, 0.0},  {0.4, 0.5, 0.0},
                    {-0.3, 0.7, -0.2}, {0.3, 0.7, -0.2},
                    {0.0, 0.5, 0.5},   {0.0, 0.7, 0.0}
                };
                for (int i = 0; i < shell.size() && i < shellOff.length; i++) {
                    shell.get(i).entity().teleport(beetleBase.clone().add(shellOff[i][0], shellOff[i][1], shellOff[i][2]));
                    float sc = (shellOff[i][1] > 0.6) ? 0.6f : 0.8f;
                    shell.get(i).scale(sc, 0.4f, sc);
                }

                double[][] legOff = {
                    {-0.8, 0.1, -0.3}, {-0.9, 0.1, 0.0}, {-0.8, 0.1, 0.3},
                    {0.8, 0.1, -0.3},  {0.9, 0.1, 0.0},  {0.8, 0.1, 0.3}
                };
                for (int i = 0; i < legs.size(); i++) {
                    double bob = Math.abs(Math.sin(tick * 0.4 + i * 1.0)) * 0.12;
                    legs.get(i).entity().teleport(beetleBase.clone().add(legOff[i][0], legOff[i][1] + bob, legOff[i][2]));
                    legs.get(i).scale(0.25f, 0.4f, 0.25f);
                }

                // Charge telegraph particles
                if (phase == 1 && tick % 2 == 0) {
                    beetleBase.getWorld().spawnParticle(Particle.SNOWFLAKE, beetleBase.clone().add(0, 0.5, 0), 5, 0.3, 0.2, 0.3, 0.02);
                }
            }

            // Walk/charge sound
            if (tick % 15 == 0) {
                float pitch = (phase == 1) ? 1.5f : 1.0f;
                DisplayBuilder.playSound(beetleBase, Sound.BLOCK_POWDER_SNOW_STEP, 0.6f, pitch);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PermafrostBeetle(plugin); }
    }

    // ================================================================
    // 11. FROST HYDRA — 12 neck segments + 6 head blocks. 3 heads strike independently.
    //     Base body: 4 BLUE_ICE torso. 3 necks x 4 PACKED_ICE segments each.
    //     3 heads: 2 ICE blocks each. = 4 + 12 + 6 = 22 blocks total
    // ================================================================
    public static class FrostHydra extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> torso = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> necks = new ArrayList<>(); // 3 necks
        private final List<List<BlockDisplayHandle>> heads = new ArrayList<>(); // 3 heads
        private final double[] headStrikeTimer = {0, 0, 0};
        private final boolean[] headStriking = {false, false, false};
        private final double[] headStrikeX = {0, 0, 0};
        private final double[] headStrikeZ = {0, 0, 0};

        public FrostHydra(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_hydra", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
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

            // Torso: 4 large blocks
            double[][] torsoOff = {{-0.5, 0.0, -0.5}, {0.5, 0.0, -0.5}, {-0.5, 0.0, 0.5}, {0.5, 0.0, 0.5}};
            for (double[] off : torsoOff) {
                BlockDisplayHandle t = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.BLUE_ICE);
                t.scale(1.0f, 1.0f, 1.0f).glow(30, 80, 160).interpolation(3, 0);
                spawnedEntities.add(t.entity());
                torso.add(t);
            }

            // 3 necks, each 4 segments, branching upward in different directions
            double[][] neckDirs = {{0.0, 0.0}, {-0.4, 0.0}, {0.4, 0.0}};
            for (int n = 0; n < 3; n++) {
                List<BlockDisplayHandle> neck = new ArrayList<>();
                for (int s = 0; s < 4; s++) {
                    double nx = neckDirs[n][0] * (s + 1);
                    double ny = 1.0 + s * 0.8;
                    double nz = neckDirs[n][1] * (s + 1) + 0.2 * s;
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(center.clone().add(nx, ny, nz), Material.PACKED_ICE);
                    seg.scale(0.5f, 0.7f, 0.5f).glow(100, 180, 255).interpolation(3, 0);
                    spawnedEntities.add(seg.entity());
                    neck.add(seg);
                }
                necks.add(neck);

                // Head: 2 blocks per head
                List<BlockDisplayHandle> head = new ArrayList<>();
                double hx = neckDirs[n][0] * 5;
                double hy = 4.2;
                BlockDisplayHandle skull = displayBuilder.spawnBlock(center.clone().add(hx, hy, 0.8), Material.ICE);
                skull.scale(0.7f, 0.6f, 0.8f).glow(220, 240, 255).interpolation(3, 0);
                spawnedEntities.add(skull.entity());
                head.add(skull);

                BlockDisplayHandle jaw = displayBuilder.spawnBlock(center.clone().add(hx, hy - 0.3, 1.0), Material.ICE);
                jaw.scale(0.6f, 0.3f, 0.5f).glow(150, 210, 255).interpolation(3, 0);
                spawnedEntities.add(jaw.entity());
                head.add(jaw);

                heads.add(head);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Torso sways slightly
            double sway = Math.sin(tick * 0.05) * 0.15;
            double[][] torsoOff = {{-0.5, 0.0, -0.5}, {0.5, 0.0, -0.5}, {-0.5, 0.0, 0.5}, {0.5, 0.0, 0.5}};
            for (int i = 0; i < torso.size(); i++) {
                torso.get(i).entity().teleport(c.clone().add(torsoOff[i][0] + sway, torsoOff[i][1], torsoOff[i][2]));
            }

            // Each head operates independently
            Player nearest = findNearestPlayer(c, 25);
            double[][] neckDirs = {{0.0, 0.0}, {-0.4, 0.0}, {0.4, 0.0}};

            for (int n = 0; n < 3; n++) {
                // Random strike trigger (staggered)
                if (!headStriking[n] && tick % (60 + n * 20) == 30 + n * 10) {
                    headStriking[n] = true;
                    headStrikeTimer[n] = 0;
                    if (nearest != null) {
                        headStrikeX[n] = nearest.getLocation().getX();
                        headStrikeZ[n] = nearest.getLocation().getZ();
                    }
                }

                if (headStriking[n]) {
                    headStrikeTimer[n]++;
                    if (headStrikeTimer[n] > 20) headStriking[n] = false;
                }

                // Animate neck segments
                for (int s = 0; s < 4; s++) {
                    double baseNx = neckDirs[n][0] * (s + 1);
                    double baseNy = 1.0 + s * 0.8;
                    double baseNz = 0.2 * s;

                    // If striking, extend toward target
                    if (headStriking[n]) {
                        double t = headStrikeTimer[n] / 20.0;
                        double strikeX = headStrikeX[n] - c.getX();
                        double strikeZ = headStrikeZ[n] - c.getZ();
                        double strikeDist = Math.sqrt(strikeX * strikeX + strikeZ * strikeZ);
                        if (strikeDist > 0) {
                            double reach = Math.min(t * 2.0, 1.0) * (s + 1) * 0.5;
                            baseNx += (strikeX / strikeDist) * reach;
                            baseNz += (strikeZ / strikeDist) * reach;
                        }
                    } else {
                        // Idle sway
                        baseNx += Math.sin(tick * 0.04 + n * 2 + s * 0.5) * 0.2;
                        baseNz += Math.cos(tick * 0.05 + n * 2 + s * 0.5) * 0.2;
                    }

                    Location segLoc = c.clone().add(baseNx, baseNy, baseNz);
                    necks.get(n).get(s).entity().teleport(segLoc);
                }

                // Animate head
                Location lastSeg = necks.get(n).get(3).entity().getLocation();
                double headOffY = 0.7;
                double headOffZ = 0.3;

                if (headStriking[n] && headStrikeTimer[n] > 10 && headStrikeTimer[n] < 15) {
                    // Snap forward
                    headOffZ += 0.5;
                    headOffY -= 0.3;
                }

                heads.get(n).get(0).entity().teleport(lastSeg.clone().add(0, headOffY, headOffZ));
                heads.get(n).get(1).entity().teleport(lastSeg.clone().add(0, headOffY - 0.3, headOffZ + 0.2));

                // Strike sound
                if (headStriking[n] && headStrikeTimer[n] == 12) {
                    DisplayBuilder.playSound(lastSeg, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.3f);
                    lastSeg.getWorld().spawnParticle(Particle.SNOWFLAKE, lastSeg, 8, 0.5, 0.3, 0.5, 0.03);
                }
            }

            // Ambient frost breath from all heads
            if (tick % 5 == 0) {
                for (int n = 0; n < 3; n++) {
                    Location headLoc = heads.get(n).get(0).entity().getLocation();
                    DisplayBuilder.dustParticles(headLoc, 2, 0.3, 220, 240, 255, 0.8f);
                }
            }

            // Roar sound
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostHydra(plugin); }
    }

    // ================================================================
    // 12. ICE LEECH — 10 flat segments that extend vertically when player near.
    //     Lies flat on ground, raises up like a snake when player approaches.
    //     10 segments: alternating PACKED_ICE / SNOW_BLOCK = 10 blocks
    // ================================================================
    public static class IceLeech extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private static final int SEG_COUNT = 10;
        private boolean alerted = false;
        private int alertTick = 0;

        public IceLeech(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_leech", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
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

            // Spawn flat on ground in a line
            for (int i = 0; i < SEG_COUNT; i++) {
                Material mat = (i % 2 == 0) ? Material.PACKED_ICE : Material.SNOW_BLOCK;
                Location segLoc = center.clone().add(0, 0.05, i * 0.6 - 2.7);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, mat);
                // Flat profile when lying down
                seg.scale(0.5f, 0.1f, 0.6f).glow(100, 180, 255).interpolation(4, 0);
                spawnedEntities.add(seg.entity());
                segments.add(seg);
            }

            // Very quiet spawn — stealthy
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.3f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Player nearest = findNearestPlayer(c, 20);
            double playerDist = (nearest != null) ? nearest.getLocation().distance(c) : 999;

            // Alert when player within 8 blocks
            if (!alerted && playerDist < 8.0) {
                alerted = true;
                alertTick = tick;
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.2f);
            }

            if (!alerted) {
                // Lying flat, slight breathing
                for (int i = 0; i < segments.size(); i++) {
                    double breathe = Math.sin(tick * 0.1 + i * 0.3) * 0.02;
                    Location segLoc = c.clone().add(0, 0.05 + breathe, i * 0.6 - 2.7);
                    segments.get(i).entity().teleport(segLoc);
                }
            } else {
                // Rearing up! Segments rise from tail to head progressively
                int ticksSinceAlert = tick - alertTick;
                double riseProgress = Math.min(ticksSinceAlert / 30.0, 1.0);

                for (int i = 0; i < segments.size(); i++) {
                    double segProgress = Math.max(0, Math.min(1.0, riseProgress * 2.0 - (double) i / SEG_COUNT));

                    // When risen: curved upward arc
                    double flatZ = i * 0.6 - 2.7;
                    double risenY = segProgress * (0.3 + i * 0.35); // Each segment higher than the last
                    double risenZ = flatZ * (1.0 - segProgress * 0.3); // Compress horizontally as it rises

                    // After fully risen, add hunting sway toward player
                    double swayX = 0, swayZ = 0;
                    if (riseProgress >= 1.0 && nearest != null && i >= SEG_COUNT - 3) {
                        double dx = nearest.getLocation().getX() - c.getX();
                        double dz = nearest.getLocation().getZ() - c.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 0.5) {
                            double trackStr = (i - (SEG_COUNT - 3)) * 0.3;
                            swayX = (dx / dist) * trackStr * Math.sin(tick * 0.1);
                            swayZ = (dz / dist) * trackStr * Math.sin(tick * 0.1);
                        }
                    }

                    // Wave motion when fully risen
                    double wave = (riseProgress >= 1.0) ? Math.sin(tick * 0.12 + i * 0.5) * 0.2 : 0;

                    Location segLoc = c.clone().add(swayX + wave, risenY, risenZ + swayZ);
                    segments.get(i).entity().teleport(segLoc);

                    // Transition scale from flat to vertical
                    float scaleX = 0.5f;
                    float scaleY = (float)(0.1 + segProgress * 0.5);
                    float scaleZ = (float)(0.6 - segProgress * 0.2);
                    segments.get(i).scale(scaleX, scaleY, scaleZ);
                }

                // Ambient frost
                if (tick % 3 == 0) {
                    Location topSeg = segments.get(SEG_COUNT - 1).entity().getLocation();
                    topSeg.getWorld().spawnParticle(Particle.SNOWFLAKE, topSeg, 3, 0.3, 0.2, 0.3, 0.01);
                    DisplayBuilder.dustParticles(topSeg, 2, 0.2, 220, 240, 255, 1.0f);
                }

                // Hissing
                if (tick % 30 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceLeech(plugin); }
    }

    // ================================================================
    // 13. GLACIAL BEHEMOTH — 20+ blocks, 4 legs. Slow walk, devastating stomp shockwave.
    //     Body: 8 BLUE_ICE, Head: 3 PACKED_ICE, Legs: 4x2 QUARTZ_BLOCK,
    //     Spine: 2 BONE_BLOCK, Tail: 2 PRISMARINE = 23 blocks total
    // ================================================================
    public static class GlacialBehemoth extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> bodyParts = new ArrayList<>();
        private final List<BlockDisplayHandle> headParts = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> legParts = new ArrayList<>(); // 4 legs, 2 blocks each
        private final List<BlockDisplayHandle> spineParts = new ArrayList<>();
        private final List<BlockDisplayHandle> tailParts = new ArrayList<>();
        private double posX, posZ;
        private int stompCooldown = 0;
        private boolean stomping = false;
        private int stompTick = 0;

        public GlacialBehemoth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_behemoth", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.posX = center.getX();
            this.posZ = center.getZ();
            World w = center.getWorld();
            if (w == null) return;

            // Body: massive 2x2x2 core (8 BLUE_ICE blocks)
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    for (int z = 0; z < 2; z++) {
                        Location bLoc = center.clone().add(x * 1.0 - 0.5, 2.5 + y * 1.0, z * 1.0 - 0.5);
                        BlockDisplayHandle b = displayBuilder.spawnBlock(bLoc, Material.BLUE_ICE);
                        b.scale(1.1f, 1.1f, 1.1f).glow(30, 80, 160).interpolation(3, 0);
                        spawnedEntities.add(b.entity());
                        bodyParts.add(b);
                    }
                }
            }

            // Head: 3 blocks (PACKED_ICE), extending forward
            double[][] headOff = {{0, 3.8, 1.5}, {-0.4, 3.5, 2.0}, {0.4, 3.5, 2.0}};
            for (double[] off : headOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.PACKED_ICE);
                h.scale(0.9f, 0.8f, 0.9f).glow(220, 240, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                headParts.add(h);
            }

            // Spine ridge: 2 blocks along top (BONE_BLOCK)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle sp = displayBuilder.spawnBlock(center.clone().add(0, 4.6, -i * 0.8), Material.BONE_BLOCK);
                sp.scale(0.5f, 0.6f, 0.7f).glow(220, 240, 255).interpolation(3, 0);
                spawnedEntities.add(sp.entity());
                spineParts.add(sp);
            }

            // 4 legs: each is 2 blocks (upper + lower, QUARTZ_BLOCK)
            double[][] legPositions = {{-1.2, 0, -0.8}, {1.2, 0, -0.8}, {-1.2, 0, 0.8}, {1.2, 0, 0.8}};
            for (double[] lp : legPositions) {
                List<BlockDisplayHandle> leg = new ArrayList<>();
                // Upper leg
                BlockDisplayHandle upper = displayBuilder.spawnBlock(center.clone().add(lp[0], 1.5, lp[2]), Material.QUARTZ_BLOCK);
                upper.scale(0.6f, 1.2f, 0.6f).glow(220, 240, 255).interpolation(3, 0);
                spawnedEntities.add(upper.entity());
                leg.add(upper);
                // Lower leg (foot)
                BlockDisplayHandle lower = displayBuilder.spawnBlock(center.clone().add(lp[0], 0.2, lp[2]), Material.QUARTZ_BLOCK);
                lower.scale(0.7f, 1.0f, 0.7f).glow(150, 210, 255).interpolation(3, 0);
                spawnedEntities.add(lower.entity());
                leg.add(lower);
                legParts.add(leg);
            }

            // Tail: 2 PRISMARINE blocks
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle tail = displayBuilder.spawnBlock(center.clone().add(0, 2.5, -1.5 - i * 0.8), Material.PRISMARINE);
                tail.scale(0.6f, 0.5f, 0.7f).glow(100, 180, 255).interpolation(3, 0);
                spawnedEntities.add(tail.entity());
                tailParts.add(tail);
            }

            // Epic spawn sound
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.4f);
            center.getWorld().spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 2, 0), 30, 2.0, 1.5, 2.0, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            stompCooldown--;

            // Slow walk toward nearest player
            Player nearest = findNearestPlayer(new Location(c.getWorld(), posX, c.getY(), posZ), 30);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - posX;
                double dz = nearest.getLocation().getZ() - posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 2.0) {
                    posX += (dx / dist) * 0.06;
                    posZ += (dz / dist) * 0.06;
                }

                // Stomp when close enough
                if (dist < 8.0 && stompCooldown <= 0 && !stomping) {
                    stomping = true;
                    stompTick = 0;
                    stompCooldown = 70;
                }
            }

            if (stomping) {
                stompTick++;
                if (stompTick > 20) stomping = false;
            }

            Location behemothBase = new Location(c.getWorld(), posX, c.getY(), posZ);
            behemothBase.setYaw(0);
            behemothBase.setPitch(0);
            setCenter(behemothBase.clone().add(0, 3.0, 0));

            // Walking bob
            double walkBob = Math.sin(tick * 0.1) * 0.15;
            double stompDrop = 0;
            if (stomping && stompTick >= 8 && stompTick <= 12) {
                stompDrop = -0.4; // Slam down
            }

            // Move body
            int bi = 0;
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    for (int z = 0; z < 2; z++) {
                        bodyParts.get(bi).entity().teleport(
                            behemothBase.clone().add(x * 1.0 - 0.5, 2.5 + y * 1.0 + walkBob + stompDrop, z * 1.0 - 0.5)
                        );
                        bi++;
                    }
                }
            }

            // Move head
            double[][] headOff = {{0, 3.8, 1.5}, {-0.4, 3.5, 2.0}, {0.4, 3.5, 2.0}};
            for (int i = 0; i < headParts.size(); i++) {
                headParts.get(i).entity().teleport(
                    behemothBase.clone().add(headOff[i][0], headOff[i][1] + walkBob + stompDrop, headOff[i][2])
                );
            }

            // Move spine
            for (int i = 0; i < spineParts.size(); i++) {
                spineParts.get(i).entity().teleport(
                    behemothBase.clone().add(0, 4.6 + walkBob + stompDrop, -i * 0.8)
                );
            }

            // Move tail with sway
            double tailSway = Math.sin(tick * 0.08) * 0.3;
            for (int i = 0; i < tailParts.size(); i++) {
                tailParts.get(i).entity().teleport(
                    behemothBase.clone().add(tailSway * (i + 1), 2.5 + walkBob + stompDrop, -1.5 - i * 0.8)
                );
            }

            // Animate legs: alternating gait
            double[][] legPositions = {{-1.2, 0, -0.8}, {1.2, 0, -0.8}, {-1.2, 0, 0.8}, {1.2, 0, 0.8}};
            for (int i = 0; i < legParts.size(); i++) {
                double legPhase = tick * 0.15 + i * Math.PI / 2;
                double legLift = Math.max(0, Math.sin(legPhase)) * 0.4;
                double legStep = Math.sin(legPhase) * 0.3;

                // During stomp, front legs slam down
                double legStomp = 0;
                if (stomping && stompTick >= 8 && stompTick <= 12 && i >= 2) {
                    legStomp = -0.3;
                    legLift = 0;
                }

                // Upper leg
                legParts.get(i).get(0).entity().teleport(
                    behemothBase.clone().add(legPositions[i][0], 1.5 + legLift + legStomp, legPositions[i][2] + legStep)
                );
                // Lower leg
                legParts.get(i).get(1).entity().teleport(
                    behemothBase.clone().add(legPositions[i][0], 0.2 + legStomp, legPositions[i][2] + legStep)
                );
            }

            // STOMP SHOCKWAVE
            if (stomping && stompTick == 10) {
                DisplayBuilder.playSound(behemothBase, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
                DisplayBuilder.playSound(behemothBase, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.5f);

                // Expanding ring of frost particles
                DisplayBuilder.particleRing(behemothBase, 3.0, Particle.DUST, 30,
                    new Particle.DustOptions(Color.fromRGB(100, 180, 255), 2.0f));
                DisplayBuilder.particleRing(behemothBase, 5.0, Particle.DUST, 40,
                    new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.5f));
                DisplayBuilder.particleRing(behemothBase, 7.0, Particle.SNOWFLAKE, 50, null);

                // Screen-shake snowflakes
                behemothBase.getWorld().spawnParticle(Particle.SNOWFLAKE, behemothBase, 40, 4.0, 0.5, 4.0, 0.05);
            }

            // Ground shake particles while walking
            if (tick % 10 == 0) {
                DisplayBuilder.dustParticles(behemothBase, 5, 1.5, 30, 80, 160, 1.2f);
                DisplayBuilder.playSound(behemothBase, Sound.BLOCK_POWDER_SNOW_STEP, 0.7f, 0.3f);
            }

            // Frost breath from head
            if (tick % 6 == 0) {
                Location headTip = behemothBase.clone().add(0, 3.6, 2.2);
                headTip.getWorld().spawnParticle(Particle.SNOWFLAKE, headTip, 4, 0.3, 0.2, 0.3, 0.02);
                DisplayBuilder.dustParticles(headTip, 3, 0.3, 220, 240, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialBehemoth(plugin); }
    }
}
