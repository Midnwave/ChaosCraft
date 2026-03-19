package com.blockforge.chaoscraft.modes.devilsdream.attacks;

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
 * Devil's Dream — NIGHTMARE CONSTRUCT ATTACKS
 * 13 impossible architecture BlockDisplay attacks that defy geometry and logic.
 * Escher-inspired structures, melting buildings, and reality-breaking constructs.
 *
 * Color palette (nightmare/surreal):
 * - Deepslate gray: deepslate, polished_deepslate
 * - Warped teal: warped_planks, warped_stem
 * - Crying purple: crying_obsidian
 * - Blackstone dark: blackstone, polished_blackstone
 * - End pale: end_stone_bricks, purpur_block
 */
public final class NightmareConstructs {

    private NightmareConstructs() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EscherStaircase(plugin));
        registry.register(new InfiniteHallway(plugin));
        registry.register(new FloatingRoom(plugin));
        registry.register(new BrokenClock(plugin));
        registry.register(new DoorToNowhere(plugin));
        registry.register(new MeltingTower(plugin));
        registry.register(new GravityStairs(plugin));
        registry.register(new MirrorCorridor(plugin));
        registry.register(new PendulumRoom(plugin));
        registry.register(new ShrinkingBox(plugin));
        registry.register(new TwistedSpire(plugin));
        registry.register(new NightmareCarousel(plugin));
        registry.register(new ImpossibleArch(plugin));
    }

    // ================================================================
    // 1. ESCHER STAIRCASE — 16 stair blocks that form a loop going
    //    up on all 4 sides, rotating slowly. Impossible geometry.
    //    Stairs climb up then seamlessly connect back to the bottom.
    // ================================================================
    public static class EscherStaircase extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stairs = new ArrayList<>();
        private static final int STAIR_COUNT = 16;
        private static final double RADIUS = 4.0;

        public EscherStaircase(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("escher_staircase", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build a spiral staircase that loops impossibly
            for (int i = 0; i < STAIR_COUNT; i++) {
                double angle = (2 * Math.PI * i) / STAIR_COUNT;
                double x = Math.cos(angle) * RADIUS;
                double z = Math.sin(angle) * RADIUS;
                // Height goes up then wraps back — creates illusion of infinite ascent
                double y = (i % (STAIR_COUNT / 2)) * 0.7;

                Location loc = center.clone().add(x, y, z);
                Material mat = (i % 3 == 0) ? Material.DEEPSLATE_BRICK_STAIRS
                        : (i % 3 == 1) ? Material.POLISHED_BLACKSTONE_BRICK_STAIRS
                        : Material.DARK_OAK_STAIRS;
                BlockDisplayHandle stair = displayBuilder.spawnBlock(loc, mat);
                stair.scale(1.8f, 0.6f, 1.8f)
                     .glow(100, 60, 160)
                     .interpolation(3, 0);
                stairs.add(stair);
                spawnedEntities.add(stair.entity());
            }

            // Central pillar of crying obsidian
            for (int y = 0; y < 6; y++) {
                BlockDisplayHandle pillar = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                pillar.scale(1.2f, 1.2f, 1.2f).glow(120, 50, 200);
                spawnedEntities.add(pillar.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate the entire staircase slowly
            double rotationOffset = ticksAlive * 0.02;
            for (int i = 0; i < stairs.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / STAIR_COUNT;
                double angle = baseAngle + rotationOffset;
                double x = Math.cos(angle) * RADIUS;
                double z = Math.sin(angle) * RADIUS;
                double y = (i % (STAIR_COUNT / 2)) * 0.7;

                stairs.get(i).entity().teleport(c.clone().add(x, y, z));
                stairs.get(i).rotate((float)(ticksAlive * 0.03), 0, 1, 0);
            }

            // Eerie particles along the staircase
            if (ticksAlive % 5 == 0) {
                double angle = Math.random() * Math.PI * 2;
                Location particleLoc = c.clone().add(
                        Math.cos(angle) * RADIUS, Math.random() * 5, Math.sin(angle) * RADIUS);
                DisplayBuilder.dustParticles(particleLoc, 5, 0.5, 100, 60, 160, 1.5f);
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 3, 0.3, 0.3, 0.3, 0.02);
            }

            // Ambient sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new EscherStaircase(plugin); }
    }

    // ================================================================
    // 2. INFINITE HALLWAY — Two rows of 6 walls on each side with a
    //    ceiling, forming a corridor. Walls slowly close inward.
    //    Floor has warped planks creating uneasy feeling.
    // ================================================================
    public static class InfiniteHallway extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftWall = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWall = new ArrayList<>();
        private final List<BlockDisplayHandle> ceiling = new ArrayList<>();
        private final List<BlockDisplayHandle> floor = new ArrayList<>();
        private float wallDistance = 5.0f;

        public InfiniteHallway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infinite_hallway", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 6; i++) {
                double z = -3 + i * 1.2;

                // Left wall
                BlockDisplayHandle left = displayBuilder.spawnBlock(
                        center.clone().add(-wallDistance, 1.5, z), Material.DEEPSLATE_BRICKS);
                left.scale(0.6f, 4.0f, 1.2f).glow(80, 60, 120).interpolation(3, 0);
                leftWall.add(left);
                spawnedEntities.add(left.entity());

                // Right wall
                BlockDisplayHandle right = displayBuilder.spawnBlock(
                        center.clone().add(wallDistance, 1.5, z), Material.DEEPSLATE_BRICKS);
                right.scale(0.6f, 4.0f, 1.2f).glow(80, 60, 120).interpolation(3, 0);
                rightWall.add(right);
                spawnedEntities.add(right.entity());

                // Ceiling
                BlockDisplayHandle ceil = displayBuilder.spawnBlock(
                        center.clone().add(0, 5, z), Material.POLISHED_BLACKSTONE);
                ceil.scale(wallDistance * 2 + 1, 0.5f, 1.2f).glow(60, 40, 100).interpolation(3, 0);
                ceiling.add(ceil);
                spawnedEntities.add(ceil.entity());

                // Floor
                BlockDisplayHandle flr = displayBuilder.spawnBlock(
                        center.clone().add(0, 0, z), Material.WARPED_PLANKS);
                flr.scale(wallDistance * 2 + 1, 0.3f, 1.2f).glow(40, 100, 100);
                floor.add(flr);
                spawnedEntities.add(flr.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Walls close in slowly
            if (ticksAlive < 200) {
                wallDistance = 5.0f - (ticksAlive * 0.015f);
                if (wallDistance < 1.5f) wallDistance = 1.5f;
            }

            for (int i = 0; i < 6; i++) {
                double z = -3 + i * 1.2;
                leftWall.get(i).entity().teleport(c.clone().add(-wallDistance, 1.5, z));
                rightWall.get(i).entity().teleport(c.clone().add(wallDistance, 1.5, z));
                ceiling.get(i).entity().teleport(c.clone().add(0, 5, z));
                ceiling.get(i).scale(wallDistance * 2 + 1, 0.5f, 1.2f);
            }

            // Flickering light particles
            if (ticksAlive % 8 == 0) {
                Location lightLoc = c.clone().add(0, 4, Math.random() * 6 - 3);
                c.getWorld().spawnParticle(Particle.END_ROD, lightLoc, 2, 0.5, 0.2, 0.5, 0.01);
            }

            // Whisper sounds as walls close
            if (ticksAlive % 30 == 0 && wallDistance < 3.5f) {
                DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.5f, 0.3f);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_STEP, 0.8f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new InfiniteHallway(plugin); }
    }

    // ================================================================
    // 3. FLOATING ROOM — A 4-wall room (10 blocks) that materializes
    //    in the air, tilts slowly, then collapses downward.
    //    Room hovers 8 blocks above player.
    // ================================================================
    public static class FloatingRoom extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private BlockDisplayHandle roofBlock;
        private float tilt = 0;
        private float height = 8.0f;
        private boolean collapsing = false;

        public FloatingRoom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floating_room", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(80.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            float size = 3.0f;
            // 4 walls, 2 blocks each = 8, plus floor and roof = 10
            Material wallMat = Material.POLISHED_BLACKSTONE_BRICKS;
            Material roofMat = Material.CRYING_OBSIDIAN;

            // Front wall (2 blocks)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle wall = displayBuilder.spawnBlock(
                        center.clone().add(-size / 2 + i * size, height, -size / 2), wallMat);
                wall.scale(size / 2 + 0.5f, 3.0f, 0.5f).glow(90, 50, 140).interpolation(4, 0);
                walls.add(wall);
                spawnedEntities.add(wall.entity());
            }
            // Back wall (2 blocks)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle wall = displayBuilder.spawnBlock(
                        center.clone().add(-size / 2 + i * size, height, size / 2), wallMat);
                wall.scale(size / 2 + 0.5f, 3.0f, 0.5f).glow(90, 50, 140).interpolation(4, 0);
                walls.add(wall);
                spawnedEntities.add(wall.entity());
            }
            // Left wall (2 blocks)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle wall = displayBuilder.spawnBlock(
                        center.clone().add(-size / 2, height + i * 1.5, 0), wallMat);
                wall.scale(0.5f, 1.5f, size + 0.5f).glow(90, 50, 140).interpolation(4, 0);
                walls.add(wall);
                spawnedEntities.add(wall.entity());
            }
            // Right wall (2 blocks)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle wall = displayBuilder.spawnBlock(
                        center.clone().add(size / 2, height + i * 1.5, 0), wallMat);
                wall.scale(0.5f, 1.5f, size + 0.5f).glow(90, 50, 140).interpolation(4, 0);
                walls.add(wall);
                spawnedEntities.add(wall.entity());
            }
            // Roof
            roofBlock = displayBuilder.spawnBlock(
                    center.clone().add(0, height + 3, 0), roofMat);
            roofBlock.scale(size + 1, 0.5f, size + 1).glow(120, 50, 200).interpolation(4, 0);
            spawnedEntities.add(roofBlock.entity());

            DisplayBuilder.playSound(center.clone().add(0, height, 0), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: hover and tilt (0-200 ticks)
            if (ticksAlive < 200) {
                tilt = (float) Math.sin(ticksAlive * 0.04) * 0.15f;
                float bob = (float) Math.sin(ticksAlive * 0.06) * 0.3f;
                height = 8.0f + bob;
            }
            // Phase 2: collapse (200+ ticks)
            else if (!collapsing) {
                collapsing = true;
                DisplayBuilder.playSound(c.clone().add(0, height, 0),
                        Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.5f);
            }

            if (collapsing) {
                height -= 0.6f;
                if (height <= 0.5f) {
                    height = 0.5f;
                    if (ticksAlive == 201 || (collapsing && height <= 0.6f)) {
                        triggerImpactDamage(c);
                        c.getWorld().spawnParticle(Particle.EXPLOSION, c, 3, 2, 1, 2, 0);
                        DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                    }
                }
            }

            // Update wall positions with tilt
            for (BlockDisplayHandle wall : walls) {
                wall.rotate(tilt, 0, 0, 1);
            }

            // Ambient particles
            if (ticksAlive % 6 == 0) {
                Location pLoc = c.clone().add(
                        Math.random() * 6 - 3, height + Math.random() * 3, Math.random() * 6 - 3);
                DisplayBuilder.dustParticles(pLoc, 3, 0.3, 90, 50, 140, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FloatingRoom(plugin); }
    }

    // ================================================================
    // 4. BROKEN CLOCK — Large clock face (12 hour markers in a ring)
    //    with 2 spinning hands. Hands speed up and slow down randomly.
    //    Clock face made of gilded blackstone, hands of copper.
    // ================================================================
    public static class BrokenClock extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hourMarkers = new ArrayList<>();
        private BlockDisplayHandle minuteHand;
        private BlockDisplayHandle hourHand;
        private BlockDisplayHandle centerHub;
        private double minuteSpeed = 0.05;
        private double hourSpeed = 0.02;

        public BrokenClock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("broken_clock", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location clockCenter = center.clone().add(0, 4, 0);

            // 12 hour markers in a vertical ring (facing player)
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double x = Math.cos(angle) * 4.0;
                double y = Math.sin(angle) * 4.0;
                BlockDisplayHandle marker = displayBuilder.spawnBlock(
                        clockCenter.clone().add(x, y, 0), Material.GILDED_BLACKSTONE);
                marker.scale(0.8f, 0.8f, 0.4f).glow(180, 150, 50).interpolation(2, 0);
                hourMarkers.add(marker);
                spawnedEntities.add(marker.entity());
            }

            // Minute hand (long, thin)
            minuteHand = displayBuilder.spawnBlock(clockCenter, Material.OXIDIZED_COPPER);
            minuteHand.scale(0.3f, 3.5f, 0.3f).glow(100, 180, 140).interpolation(2, 0);
            spawnedEntities.add(minuteHand.entity());

            // Hour hand (shorter, wider)
            hourHand = displayBuilder.spawnBlock(clockCenter, Material.WEATHERED_COPPER);
            hourHand.scale(0.5f, 2.2f, 0.3f).glow(120, 160, 100).interpolation(2, 0);
            spawnedEntities.add(hourHand.entity());

            // Center hub
            centerHub = displayBuilder.spawnBlock(clockCenter, Material.CRYING_OBSIDIAN);
            centerHub.scale(1.0f, 1.0f, 0.6f).glow(150, 80, 220);
            spawnedEntities.add(centerHub.entity());

            DisplayBuilder.playSound(clockCenter, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location clockCenter = c.clone().add(0, 4, 0);

            // Randomly change speeds (broken clock effect)
            if (ticksAlive % 40 == 0) {
                minuteSpeed = 0.02 + Math.random() * 0.15;
                hourSpeed = 0.01 + Math.random() * 0.08;
                if (Math.random() < 0.3) minuteSpeed = -minuteSpeed; // Reverse!
            }

            double minuteAngle = ticksAlive * minuteSpeed;
            double hourAngle = ticksAlive * hourSpeed;

            // Rotate hands by moving them in the vertical plane (XY)
            minuteHand.rotate((float) minuteAngle, 0, 0, 1);
            hourHand.rotate((float) hourAngle, 0, 0, 1);

            // Tick-tock sound with erratic timing
            if (ticksAlive % (10 + (int)(Math.random() * 20)) == 0) {
                DisplayBuilder.playSound(clockCenter, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f,
                        0.5f + (float)(Math.random() * 1.0));
            }

            // Sparks from the center when direction changes
            if (ticksAlive % 40 == 1) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, clockCenter, 8, 0.3, 0.3, 0.3, 0.05);
            }

            // Dust trail behind hands
            if (ticksAlive % 4 == 0) {
                double mx = Math.cos(minuteAngle) * 2.5;
                double my = Math.sin(minuteAngle) * 2.5;
                DisplayBuilder.dustParticles(
                        clockCenter.clone().add(mx, my, 0), 3, 0.2, 100, 180, 140, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrokenClock(plugin); }
    }

    // ================================================================
    // 5. DOOR TO NOWHERE — A door frame (10 blocks) appears, opens
    //    to reveal void/nothing. Pulls player toward it.
    //    Frame: polished blackstone. Inside: black concrete + smoke.
    // ================================================================
    public static class DoorToNowhere extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private final List<BlockDisplayHandle> voidBlocks = new ArrayList<>();
        private boolean opened = false;

        public DoorToNowhere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("door_to_nowhere", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location doorBase = center.clone().add(0, 0, -4);
            Material frameMat = Material.POLISHED_BLACKSTONE_BRICKS;

            // Door frame — left pillar (3)
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        doorBase.clone().add(-1.5, y * 1.5, 0), frameMat);
                block.scale(0.8f, 1.5f, 0.8f).glow(70, 50, 100).interpolation(3, 0);
                frame.add(block);
                spawnedEntities.add(block.entity());
            }
            // Right pillar (3)
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        doorBase.clone().add(1.5, y * 1.5, 0), frameMat);
                block.scale(0.8f, 1.5f, 0.8f).glow(70, 50, 100).interpolation(3, 0);
                frame.add(block);
                spawnedEntities.add(block.entity());
            }
            // Top beam (2)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        doorBase.clone().add(-0.5 + i, 4.5, 0), frameMat);
                block.scale(1.5f, 0.8f, 0.8f).glow(70, 50, 100).interpolation(3, 0);
                frame.add(block);
                spawnedEntities.add(block.entity());
            }
            // Decorative top piece
            BlockDisplayHandle crown = displayBuilder.spawnBlock(
                    doorBase.clone().add(0, 5.3, 0), Material.CRYING_OBSIDIAN);
            crown.scale(2.5f, 0.6f, 0.6f).glow(150, 60, 220);
            frame.add(crown);
            spawnedEntities.add(crown.entity());

            // Void inside — starts hidden (scale 0)
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle voidBlock = displayBuilder.spawnBlock(
                        doorBase.clone().add(0, y * 1.5, 0), Material.BLACK_CONCRETE);
                voidBlock.scale(0.01f, 0.01f, 0.01f).glow(10, 0, 20).interpolation(10, 0);
                voidBlocks.add(voidBlock);
                spawnedEntities.add(voidBlock.entity());
            }

            DisplayBuilder.playSound(doorBase, Sound.BLOCK_WOODEN_DOOR_CLOSE, 1.0f, 0.3f);
            DisplayBuilder.playSound(doorBase, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location doorCenter = c.clone().add(0, 2, -4);

            // Open door at tick 40
            if (ticksAlive == 40 && !opened) {
                opened = true;
                for (BlockDisplayHandle vb : voidBlocks) {
                    vb.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(2.0f, 1.5f, 0.3f), 15);
                }
                DisplayBuilder.playSound(doorCenter, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 0.4f);
            }

            // Void particles and suction effect when open
            if (opened && ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, doorCenter, 8, 0.8, 1.5, 0.3, 0.03);
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, doorCenter, 5, 0.5, 1.0, 0.5, 0.05);
                DisplayBuilder.dustParticles(doorCenter, 4, 1.0, 10, 0, 20, 2.0f);
            }

            if (opened && ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(doorCenter, Sound.ENTITY_ENDERMAN_SCREAM, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DoorToNowhere(plugin); }
    }

    // ================================================================
    // 6. MELTING TOWER — 12-block tall tower that spawns then slowly
    //    "melts" — blocks widen and flatten, dripping particles fall.
    //    Made of deepslate that turns to magma as it melts.
    // ================================================================
    public static class MeltingTower extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();
        private final List<Float> meltProgress = new ArrayList<>();

        public MeltingTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("melting_tower", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int y = 0; y < 12; y++) {
                Material mat = y < 8 ? Material.DEEPSLATE_BRICKS : Material.POLISHED_DEEPSLATE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), mat);
                block.scale(2.0f, 1.0f, 2.0f).glow(80, 60, 100).interpolation(5, 0);
                towerBlocks.add(block);
                spawnedEntities.add(block.entity());
                meltProgress.add(0.0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Melting starts from top, cascades down
            for (int i = towerBlocks.size() - 1; i >= 0; i--) {
                int meltDelay = (towerBlocks.size() - 1 - i) * 15; // Top melts first
                if (ticksAlive > meltDelay) {
                    float progress = Math.min(1.0f, meltProgress.get(i) + 0.008f);
                    meltProgress.set(i, progress);

                    // Widen and flatten as it melts
                    float width = 2.0f + progress * 3.0f;
                    float height = 1.0f - progress * 0.7f;
                    float yOffset = i * (1.0f - progress * 0.3f);

                    towerBlocks.get(i).animateTo(
                            new Vector3f(-width / 2, -0.5f, -width / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(width, height, width), 5);
                    towerBlocks.get(i).entity().teleport(c.clone().add(0, yOffset, 0));

                    // Dripping particles
                    if (progress > 0.3f && ticksAlive % 6 == 0 && Math.random() < 0.5) {
                        Location drip = c.clone().add(
                                Math.random() * width - width / 2, yOffset,
                                Math.random() * width - width / 2);
                        c.getWorld().spawnParticle(Particle.DRIPPING_LAVA, drip, 2, 0.3, 0.1, 0.3, 0);
                        DisplayBuilder.dustParticles(drip, 2, 0.2, 200, 100, 40, 1.2f);
                    }
                }
            }

            // Sizzling sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MeltingTower(plugin); }
    }

    // ================================================================
    // 7. GRAVITY STAIRS — 12 stair blocks that alternate going up
    //    and upside-down, defying gravity. Some float upward, some
    //    fall down, creating impossible vertical movement.
    // ================================================================
    public static class GravityStairs extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stairs = new ArrayList<>();
        private final List<Float> velocities = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Boolean> invertedGravity = new ArrayList<>();

        public GravityStairs(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_stairs", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 12; i++) {
                double x = (i % 4 - 1.5) * 2.0;
                double z = (i / 4 - 1) * 2.0;
                float y = 1.0f + (float)(Math.random() * 6);
                boolean inverted = i % 2 == 0;

                Material mat = inverted ? Material.WARPED_STAIRS : Material.BLACKSTONE_STAIRS;
                BlockDisplayHandle stair = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), mat);
                float rotation = inverted ? (float) Math.PI : 0;
                stair.scale(1.5f, 0.6f, 1.5f).glow(60, 120, 120).interpolation(3, 0);
                stair.rotate(rotation, 1, 0, 0);
                stairs.add(stair);
                spawnedEntities.add(stair.entity());
                velocities.add(0.0f);
                yPositions.add(y);
                invertedGravity.add(inverted);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < stairs.size(); i++) {
                float gravity = invertedGravity.get(i) ? 0.03f : -0.03f;
                float vel = velocities.get(i) + gravity;
                float y = yPositions.get(i) + vel;

                // Bounce at boundaries
                if (y < 0.5f) { y = 0.5f; vel = Math.abs(vel) * 0.8f; }
                if (y > 12.0f) { y = 12.0f; vel = -Math.abs(vel) * 0.8f; }

                velocities.set(i, vel);
                yPositions.set(i, y);

                double x = (i % 4 - 1.5) * 2.0;
                double z = (i / 4 - 1) * 2.0;
                stairs.get(i).entity().teleport(c.clone().add(x, y, z));

                // Gravity-flip particles
                if (ticksAlive % 8 == 0 && Math.random() < 0.4) {
                    Particle p = invertedGravity.get(i) ? Particle.REVERSE_PORTAL : Particle.PORTAL;
                    c.getWorld().spawnParticle(p, c.clone().add(x, y, z), 3, 0.3, 0.3, 0.3, 0.02);
                }
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SHULKER_TELEPORT, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GravityStairs(plugin); }
    }

    // ================================================================
    // 8. MIRROR CORRIDOR — Two mirrored rows of 5 tinted glass panels
    //    that slowly converge. Reflective particles bounce between them.
    //    Creates an unsettling infinite reflection effect.
    // ================================================================
    public static class MirrorCorridor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftMirrors = new ArrayList<>();
        private final List<BlockDisplayHandle> rightMirrors = new ArrayList<>();
        private float mirrorGap = 8.0f;

        public MirrorCorridor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mirror_corridor", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 5; i++) {
                double z = -4 + i * 2;

                BlockDisplayHandle leftMirror = displayBuilder.spawnBlock(
                        center.clone().add(-mirrorGap / 2, 1.5, z), Material.TINTED_GLASS);
                leftMirror.scale(0.3f, 4.0f, 2.0f).glow(150, 150, 200).interpolation(4, 0);
                leftMirrors.add(leftMirror);
                spawnedEntities.add(leftMirror.entity());

                BlockDisplayHandle rightMirror = displayBuilder.spawnBlock(
                        center.clone().add(mirrorGap / 2, 1.5, z), Material.TINTED_GLASS);
                rightMirror.scale(0.3f, 4.0f, 2.0f).glow(150, 150, 200).interpolation(4, 0);
                rightMirrors.add(rightMirror);
                spawnedEntities.add(rightMirror.entity());
            }

            // Floor strip
            BlockDisplayHandle floorStrip = displayBuilder.spawnBlock(
                    center.clone().add(0, 0, 0), Material.POLISHED_DEEPSLATE);
            floorStrip.scale(mirrorGap, 0.2f, 10.0f).glow(60, 60, 80);
            spawnedEntities.add(floorStrip.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Mirrors converge
            if (mirrorGap > 2.5f) {
                mirrorGap -= 0.02f;
            }

            for (int i = 0; i < 5; i++) {
                double z = -4 + i * 2;
                leftMirrors.get(i).entity().teleport(c.clone().add(-mirrorGap / 2, 1.5, z));
                rightMirrors.get(i).entity().teleport(c.clone().add(mirrorGap / 2, 1.5, z));
            }

            // Bouncing light particles between mirrors
            if (ticksAlive % 4 == 0) {
                double z = -4 + Math.random() * 8;
                double bounceX = (ticksAlive % 8 < 4) ? -mirrorGap / 2 + 0.5 : mirrorGap / 2 - 0.5;
                Location sparkLoc = c.clone().add(bounceX, 1 + Math.random() * 3, z);
                c.getWorld().spawnParticle(Particle.END_ROD, sparkLoc, 2, 0.1, 0.1, 0.1, 0.03);
            }

            // Eerie chime
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.3f + (float)(Math.random() * 0.4));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MirrorCorridor(plugin); }
    }

    // ================================================================
    // 9. PENDULUM ROOM — A large pendulum (blade + arm, 12 blocks)
    //    swings back and forth in a room frame. Blade sweeps through
    //    player area. Classic horror trap.
    // ================================================================
    public static class PendulumRoom extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> roomFrame = new ArrayList<>();
        private BlockDisplayHandle pendulumArm;
        private BlockDisplayHandle pendulumBlade;
        private BlockDisplayHandle pendulumWeight;
        private double swingAngle = 0;

        public PendulumRoom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pendulum_room", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(65.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Room frame — top beam
            BlockDisplayHandle topBeam = displayBuilder.spawnBlock(
                    center.clone().add(0, 8, 0), Material.POLISHED_BLACKSTONE);
            topBeam.scale(8.0f, 0.8f, 0.8f).glow(60, 50, 80);
            roomFrame.add(topBeam);
            spawnedEntities.add(topBeam.entity());

            // Left and right pillars
            for (int x = -1; x <= 1; x += 2) {
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle pillar = displayBuilder.spawnBlock(
                            center.clone().add(x * 4, y * 2, 0), Material.DEEPSLATE_BRICKS);
                    pillar.scale(0.8f, 2.0f, 0.8f).glow(60, 50, 80);
                    roomFrame.add(pillar);
                    spawnedEntities.add(pillar.entity());
                }
            }

            // Pendulum arm (long vertical bar hanging from top)
            pendulumArm = displayBuilder.spawnBlock(
                    center.clone().add(0, 5, 0), Material.CHAIN);
            pendulumArm.scale(0.4f, 5.0f, 0.4f).glow(180, 180, 200).interpolation(2, 0);
            spawnedEntities.add(pendulumArm.entity());

            // Blade at bottom
            pendulumBlade = displayBuilder.spawnBlock(
                    center.clone().add(0, 1, 0), Material.IRON_BLOCK);
            pendulumBlade.scale(3.0f, 0.3f, 0.8f).glow(200, 200, 220).interpolation(2, 0);
            spawnedEntities.add(pendulumBlade.entity());

            // Weight
            pendulumWeight = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.5, 0), Material.NETHERITE_BLOCK);
            pendulumWeight.scale(1.2f, 1.2f, 1.2f).glow(60, 50, 60).interpolation(2, 0);
            spawnedEntities.add(pendulumWeight.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pendulum swing — accelerating
            double maxAngle = Math.min(0.8, 0.3 + ticksAlive * 0.002);
            swingAngle = Math.sin(ticksAlive * 0.06) * maxAngle;

            double pivotY = 8.0;
            double armLength = 5.0;
            double tipX = Math.sin(swingAngle) * armLength;
            double tipY = pivotY - Math.cos(swingAngle) * armLength;

            // Move pendulum components
            Location armLoc = c.clone().add(tipX * 0.5, (pivotY + tipY) / 2, 0);
            pendulumArm.entity().teleport(armLoc);
            pendulumArm.rotate((float) swingAngle, 0, 0, 1);

            Location bladeLoc = c.clone().add(tipX, tipY - 0.5, 0);
            pendulumBlade.entity().teleport(bladeLoc);
            pendulumBlade.rotate((float) swingAngle, 0, 0, 1);

            Location weightLoc = c.clone().add(tipX, tipY - 1.0, 0);
            pendulumWeight.entity().teleport(weightLoc);

            // Swoosh sound at swing extremes
            if (Math.abs(swingAngle) > maxAngle * 0.9 && ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(bladeLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.5f);
            }

            // Spark particles at blade tip
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, bladeLoc, 3, 0.5, 0.1, 0.3, 0.05);
            }

            // Ominous ticking
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 8, 0), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PendulumRoom(plugin); }
    }

    // ================================================================
    // 10. SHRINKING BOX — 6 walls of a box (12 blocks) that slowly
    //     close in around the player. Walls are blackstone with
    //     crying obsidian accents. Gets smaller over time.
    // ================================================================
    public static class ShrinkingBox extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private float boxSize = 6.0f;

        public ShrinkingBox(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shrinking_box", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material main = Material.POLISHED_BLACKSTONE;
            Material accent = Material.CRYING_OBSIDIAN;

            // 6 faces × 2 blocks each = 12 block displays
            // +X face
            walls.add(spawnWall(center, boxSize / 2, 2, 0, 0.5f, 4.0f, boxSize, main));
            walls.add(spawnWall(center, boxSize / 2, 2, 0, 0.5f, 1.0f, 1.0f, accent));
            // -X face
            walls.add(spawnWall(center, -boxSize / 2, 2, 0, 0.5f, 4.0f, boxSize, main));
            walls.add(spawnWall(center, -boxSize / 2, 2, 0, 0.5f, 1.0f, 1.0f, accent));
            // +Z face
            walls.add(spawnWall(center, 0, 2, boxSize / 2, boxSize, 4.0f, 0.5f, main));
            walls.add(spawnWall(center, 0, 2, boxSize / 2, 1.0f, 1.0f, 0.5f, accent));
            // -Z face
            walls.add(spawnWall(center, 0, 2, -boxSize / 2, boxSize, 4.0f, 0.5f, main));
            walls.add(spawnWall(center, 0, 2, -boxSize / 2, 1.0f, 1.0f, 0.5f, accent));
            // Top
            walls.add(spawnWall(center, 0, 4.5, 0, boxSize, 0.5f, boxSize, main));
            walls.add(spawnWall(center, 0, 4.5, 0, 1.0f, 0.5f, 1.0f, accent));
            // Bottom
            walls.add(spawnWall(center, 0, 0, 0, boxSize, 0.5f, boxSize, main));
            walls.add(spawnWall(center, 0, 0, 0, 1.0f, 0.5f, 1.0f, accent));

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.4f);
        }

        private BlockDisplayHandle spawnWall(Location center, double ox, double oy, double oz,
                                             float sx, float sy, float sz, Material mat) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(80, 50, 130).interpolation(4, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Shrink
            if (boxSize > 1.5f) {
                boxSize -= 0.02f;
            }

            // Reposition walls
            float half = boxSize / 2;
            // +X, -X, +Z, -Z, Top, Bottom (each has main + accent)
            updateWallPos(0, c, half, 2, 0, 0.5f, 4.0f, boxSize);
            updateWallPos(1, c, half, 2, 0, 0.5f, 1.0f, 1.0f);
            updateWallPos(2, c, -half, 2, 0, 0.5f, 4.0f, boxSize);
            updateWallPos(3, c, -half, 2, 0, 0.5f, 1.0f, 1.0f);
            updateWallPos(4, c, 0, 2, half, boxSize, 4.0f, 0.5f);
            updateWallPos(5, c, 0, 2, half, 1.0f, 1.0f, 0.5f);
            updateWallPos(6, c, 0, 2, -half, boxSize, 4.0f, 0.5f);
            updateWallPos(7, c, 0, 2, -half, 1.0f, 1.0f, 0.5f);
            updateWallPos(8, c, 0, 4.5, 0, boxSize, 0.5f, boxSize);
            updateWallPos(9, c, 0, 4.5, 0, 1.0f, 0.5f, 1.0f);
            updateWallPos(10, c, 0, 0, 0, boxSize, 0.5f, boxSize);
            updateWallPos(11, c, 0, 0, 0, 1.0f, 0.5f, 1.0f);

            // Crushing sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 0.6f, 0.4f);
            }

            // Panic particles
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 5, half, 80, 50, 130, 1.2f);
            }
        }

        private void updateWallPos(int idx, Location c, double ox, double oy, double oz,
                                   float sx, float sy, float sz) {
            if (idx < walls.size()) {
                walls.get(idx).entity().teleport(c.clone().add(ox, oy, oz));
                walls.get(idx).scale(sx, sy, sz);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ShrinkingBox(plugin); }
    }

    // ================================================================
    // 11. TWISTED SPIRE — 14 blocks spiraling upward in a helix,
    //     each block slightly rotated. The whole spire slowly twists
    //     more over time, distorting. Warped nether materials.
    // ================================================================
    public static class TwistedSpire extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spireBlocks = new ArrayList<>();
        private double twistMultiplier = 1.0;

        public TwistedSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("twisted_spire", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 14; i++) {
                double angle = (i * 0.5) * twistMultiplier;
                double radius = 1.5 - (i * 0.05); // Tapers toward top
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                float y = i * 0.8f;

                Material mat = (i % 3 == 0) ? Material.WARPED_STEM
                        : (i % 3 == 1) ? Material.WARPED_HYPHAE
                        : Material.WARPED_PLANKS;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), mat);
                float scale = 1.8f - (i * 0.08f);
                block.scale(scale, 0.8f, scale).glow(40, 140, 130).interpolation(3, 0);
                block.rotate((float) angle, 0, 1, 0);
                spireBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Increase twist over time
            twistMultiplier = 1.0 + ticksAlive * 0.005;

            for (int i = 0; i < spireBlocks.size(); i++) {
                double angle = (i * 0.5) * twistMultiplier;
                double radius = 1.5 - (i * 0.05);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                float y = i * 0.8f;

                spireBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
                spireBlocks.get(i).rotate((float) angle, 0, 1, 0);
            }

            // Warped particles spiraling up
            if (ticksAlive % 4 == 0) {
                double angle = ticksAlive * 0.2;
                Location pLoc = c.clone().add(
                        Math.cos(angle) * 1.5, (ticksAlive % 50) * 0.2, Math.sin(angle) * 1.5);
                DisplayBuilder.dustParticles(pLoc, 3, 0.2, 40, 140, 130, 1.2f);
                c.getWorld().spawnParticle(Particle.WARPED_SPORE, pLoc, 2, 0.3, 0.3, 0.3, 0.01);
            }

            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_VEIN_BREAK, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TwistedSpire(plugin); }
    }

    // ================================================================
    // 12. NIGHTMARE CAROUSEL — 10 blocks arranged in a ring at
    //     varying heights, spinning like a broken carousel.
    //     Speed fluctuates erratically. Dark oak + soul lanterns.
    // ================================================================
    public static class NightmareCarousel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> horses = new ArrayList<>();
        private final List<Float> baseHeights = new ArrayList<>();
        private double spinSpeed = 0.04;
        private BlockDisplayHandle centerPole;

        public NightmareCarousel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_carousel", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center pole
            centerPole = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.DARK_OAK_LOG);
            centerPole.scale(0.8f, 8.0f, 0.8f).glow(80, 50, 40);
            spawnedEntities.add(centerPole.entity());

            // Top disc
            BlockDisplayHandle disc = displayBuilder.spawnBlock(
                    center.clone().add(0, 7, 0), Material.DARK_OAK_PLANKS);
            disc.scale(10.0f, 0.4f, 10.0f).glow(80, 50, 40);
            spawnedEntities.add(disc.entity());

            // 10 "horses" (dark structures on poles)
            for (int i = 0; i < 10; i++) {
                float baseH = 1.5f + (float)(Math.sin(i * 1.2) * 1.5);
                baseHeights.add(baseH);

                double angle = (2 * Math.PI * i) / 10;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;

                Material mat = (i % 2 == 0) ? Material.DARK_OAK_PLANKS : Material.SOUL_LANTERN;
                BlockDisplayHandle horse = displayBuilder.spawnBlock(
                        center.clone().add(x, baseH, z), mat);
                horse.scale(1.2f, 2.0f, 0.6f).glow(100, 80, 50).interpolation(2, 0);
                horses.add(horse);
                spawnedEntities.add(horse.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_HARP, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Erratic speed changes
            if (ticksAlive % 30 == 0) {
                spinSpeed = 0.02 + Math.random() * 0.1;
                if (Math.random() < 0.2) spinSpeed = -spinSpeed; // Reverse!
            }

            for (int i = 0; i < horses.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 10;
                double angle = baseAngle + ticksAlive * spinSpeed;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                // Bob up and down
                float bob = (float) Math.sin(ticksAlive * 0.08 + i * 0.6) * 1.0f;
                float y = baseHeights.get(i) + bob;

                horses.get(i).entity().teleport(c.clone().add(x, y, z));
                horses.get(i).rotate((float) angle, 0, 1, 0);
            }

            // Creepy music box sound
            if (ticksAlive % 15 == 0) {
                float pitch = 0.3f + (float)(Math.random() * 0.5);
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, pitch);
            }

            // Soul particles
            if (ticksAlive % 6 == 0) {
                double angle = Math.random() * Math.PI * 2;
                Location pLoc = c.clone().add(Math.cos(angle) * 4, 2, Math.sin(angle) * 4);
                c.getWorld().spawnParticle(Particle.SOUL, pLoc, 2, 0.3, 0.5, 0.3, 0.01);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareCarousel(plugin); }
    }

    // ================================================================
    // 13. IMPOSSIBLE ARCH — A massive arch (14 blocks) that curves
    //     impossibly — both ends touch the ground but the arch goes
    //     UP on both sides (Penrose-style). Slowly rotates.
    // ================================================================
    public static class ImpossibleArch extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();

        public ImpossibleArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("impossible_arch", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build arch using parametric curve
            for (int i = 0; i < 14; i++) {
                double t = (double) i / 13; // 0 to 1
                // Arch shape: parabolic with impossible twist
                double x = (t - 0.5) * 10.0;
                double y = 8.0 * (1 - 4 * (t - 0.5) * (t - 0.5)); // Parabola
                // Add slight Z twist for impossible geometry
                double z = Math.sin(t * Math.PI * 2) * 1.5;

                Material mat = (i % 4 == 0) ? Material.CRYING_OBSIDIAN
                        : (i % 2 == 0) ? Material.POLISHED_BLACKSTONE
                        : Material.DEEPSLATE_TILES;
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), mat);
                float tiltAngle = (float)(t * Math.PI * 0.5); // Gradual tilt
                block.scale(1.5f, 1.5f, 1.5f).glow(100, 70, 150).interpolation(3, 0);
                block.rotate(tiltAngle, 0, 0, 1);
                archBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double rotOffset = ticksAlive * 0.015;

            for (int i = 0; i < archBlocks.size(); i++) {
                double t = (double) i / 13;
                double baseX = (t - 0.5) * 10.0;
                double baseZ = Math.sin(t * Math.PI * 2) * 1.5;
                double y = 8.0 * (1 - 4 * (t - 0.5) * (t - 0.5));

                // Rotate entire arch around Y axis
                double cos = Math.cos(rotOffset);
                double sin = Math.sin(rotOffset);
                double x = baseX * cos - baseZ * sin;
                double z = baseX * sin + baseZ * cos;

                archBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
                archBlocks.get(i).rotate((float)(t * Math.PI * 0.5 + rotOffset), 0, 0, 1);
            }

            // Enigmatic particles along the arch
            if (ticksAlive % 5 == 0) {
                double t = Math.random();
                double baseX = (t - 0.5) * 10.0;
                double baseZ = Math.sin(t * Math.PI * 2) * 1.5;
                double y = 8.0 * (1 - 4 * (t - 0.5) * (t - 0.5));
                double cos = Math.cos(rotOffset);
                double sin = Math.sin(rotOffset);
                Location pLoc = c.clone().add(baseX * cos - baseZ * sin, y, baseX * sin + baseZ * cos);
                DisplayBuilder.dustParticles(pLoc, 4, 0.3, 100, 70, 150, 1.3f);
                c.getWorld().spawnParticle(Particle.ENCHANT, pLoc, 3, 0.5, 0.5, 0.5, 0.05);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ImpossibleArch(plugin); }
    }
}
