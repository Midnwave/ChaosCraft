package com.blockforge.chaoscraft.modes.corruption.attacks;

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
 * Corruption Mode — CORRUPTION STORMS & SKY ATTACKS (91-105)
 * 15 corruption-themed BlockDisplay attacks featuring dark storms,
 * void phenomena, and shadow sky events.
 *
 * Color palette:
 * - Dark purple: RGB(45, 0, 64)
 * - Crimson: RGB(74, 0, 0)
 * - Void blue: RGB(10, 0, 48)
 *
 * Materials: SCULK, DEEPSLATE, BLACKSTONE, CRYING_OBSIDIAN,
 *            OBSIDIAN, COAL_BLOCK, NETHERRACK, SOUL_SOIL, TINTED_GLASS
 *
 * Sounds: BLOCK_SCULK_SPREAD, BLOCK_SCULK_BREAK, BLOCK_DEEPSLATE_BREAK,
 *         ENTITY_WARDEN_HEARTBEAT, BLOCK_RESPAWN_ANCHOR_DEPLETE
 */
public final class CorruptionStorms {

    private CorruptionStorms() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DarkCloud(plugin));
        registry.register(new CorruptionLightning(plugin));
        registry.register(new VoidMeteor(plugin));
        registry.register(new ShadowCanopy(plugin));
        registry.register(new DarkHalo(plugin));
        registry.register(new CorruptionRain(plugin));
        registry.register(new VoidEclipse(plugin));
        registry.register(new ShadowCeiling(plugin));
        registry.register(new DarkComet(plugin));
        registry.register(new CorruptionAurora(plugin));
        registry.register(new VoidFunnel(plugin));
        registry.register(new ShadowBombardment(plugin));
        registry.register(new DarkStarfall(plugin));
        registry.register(new CorruptionVortex(plugin));
        registry.register(new VoidOmen(plugin));
    }

    // ================================================================
    // 91. DARK CLOUD — 18 blocks flat cloud overhead, rains dark
    //     particles down onto the arena below
    // ================================================================
    public static class DarkCloud extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cloudBlocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 18;
        private static final float CLOUD_Y = 14.0f;
        private static final float CLOUD_RADIUS = 5.0f;

        public DarkCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_cloud", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(800);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.SCULK, Material.DEEPSLATE, Material.COAL_BLOCK, Material.OBSIDIAN };
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (Math.PI * 2 / BLOCK_COUNT) * i + (Math.random() * 0.4 - 0.2);
                double ring = (i % 3 == 0) ? CLOUD_RADIUS * 0.3 : (i % 3 == 1) ? CLOUD_RADIUS * 0.65 : CLOUD_RADIUS;
                double ox = Math.cos(angle) * ring;
                double oz = Math.sin(angle) * ring;
                Location loc = center.clone().add(ox, CLOUD_Y + (Math.random() * 0.4 - 0.2), oz);

                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                block.scale(1.5f + (float)(Math.random() * 0.8f), 0.4f, 1.5f + (float)(Math.random() * 0.8f))
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                float tilt = (float)(Math.random() * 0.15 - 0.075);
                block.rotate(tilt, 1, 0, 0);
                cloudBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, CLOUD_Y, 0), Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Gentle cloud drift
            for (int i = 0; i < cloudBlocks.size(); i++) {
                BlockDisplayHandle block = cloudBlocks.get(i);
                float sway = (float) Math.sin(ticksAlive * 0.03 + i * 0.7) * 0.08f;
                block.rotate(sway, 0, 1, 0);
            }

            // Dark rain particles falling from cloud
            if (ticksAlive % 3 == 0) {
                for (int r = 0; r < 4; r++) {
                    double rx = c.getX() + (Math.random() * CLOUD_RADIUS * 2 - CLOUD_RADIUS);
                    double rz = c.getZ() + (Math.random() * CLOUD_RADIUS * 2 - CLOUD_RADIUS);
                    Location rainStart = new Location(w, rx, c.getY() + CLOUD_Y - 0.5, rz);
                    DisplayBuilder.dustParticles(rainStart, 4, 0.15, 45, 0, 64, 1.4f);
                    // Ground-level drip
                    DisplayBuilder.dustParticles(new Location(w, rx, c.getY() + 0.2, rz), 2, 0.3, 10, 0, 48, 0.8f);
                }
            }

            // Heartbeat pulse
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
            }

            // Sculk ambience
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, CLOUD_Y, 0), Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkCloud(plugin); }
    }

    // ================================================================
    // 92. CORRUPTION LIGHTNING — 14 blocks zigzag bolt strikes down
    //     with blinding flash and heavy damage
    // ================================================================
    public static class CorruptionLightning extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> boltSegments = new ArrayList<>();
        private static final int SEGMENT_COUNT = 14;
        private static final float BOLT_TOP = 18.0f;
        private boolean struck = false;
        private int strikeDelay;

        public CorruptionLightning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_lightning", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(240.0);
            config.setDamageRadius(5.2);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            strikeDelay = 30; // 1.5 seconds warning before strike
            float segHeight = BOLT_TOP / SEGMENT_COUNT;
            float zigzagDrift = 0;

            for (int i = 0; i < SEGMENT_COUNT; i++) {
                float y = BOLT_TOP - (segHeight * i);
                zigzagDrift += (float)(Math.random() * 1.6 - 0.8);
                float xOff = zigzagDrift;
                float zOff = (float)(Math.random() * 0.6 - 0.3);

                Location segLoc = center.clone().add(xOff, y, zOff);
                Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN : (i % 3 == 1) ? Material.BLACKSTONE : Material.SCULK;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, mat);
                seg.scale(0.5f, segHeight * 0.9f, 0.5f)
                   .glow(74, 0, 0)
                   .interpolation(1, 0);

                // Angle segment toward next zigzag
                float angle = (float) Math.atan2(zigzagDrift, segHeight) * 0.5f;
                seg.rotate(angle, 0, 0, 1);

                boltSegments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            // Hide bolt initially (scale to tiny)
            for (BlockDisplayHandle seg : boltSegments) {
                seg.scale(0.05f, 0.05f, 0.05f);
            }

            // Warning rumble
            DisplayBuilder.playSound(center.clone().add(0, BOLT_TOP, 0), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Warning phase: dark particles gather overhead
            if (ticksAlive < strikeDelay) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, BOLT_TOP, 0), 8, 2.0, 45, 0, 64, 1.6f);
                }
                return;
            }

            // Strike moment
            if (!struck) {
                struck = true;
                float segHeight = BOLT_TOP / SEGMENT_COUNT;
                for (int i = 0; i < boltSegments.size(); i++) {
                    boltSegments.get(i).scale(0.5f, segHeight * 0.9f, 0.5f);
                }
                // Flash effect
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.2f);
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.4f, 1.8f);

                // Impact at ground level
                triggerImpactDamage(c);
                DisplayBuilder.particleRing(c, 4.0, Particle.DUST, 32,
                        new Particle.DustOptions(Color.fromRGB(74, 0, 0), 2.0f));
                DisplayBuilder.dustParticles(c, 20, 1.5, 45, 0, 64, 1.8f);
            }

            // Post-strike flicker
            if (ticksAlive > strikeDelay && ticksAlive < strikeDelay + 20) {
                boolean visible = (ticksAlive % 3 != 0);
                float segHeight = BOLT_TOP / SEGMENT_COUNT;
                for (BlockDisplayHandle seg : boltSegments) {
                    if (visible) {
                        seg.scale(0.5f, segHeight * 0.9f, 0.5f);
                    } else {
                        seg.scale(0.05f, 0.05f, 0.05f);
                    }
                }
            }

            // Residual corruption particles at strike point
            if (ticksAlive > strikeDelay && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 6, 1.0, 10, 0, 48, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionLightning(plugin); }
    }

    // ================================================================
    // 93. VOID METEOR — 12 blocks trail dark fire, angled descent,
    //     60 HP shockwave on impact
    // ================================================================
    public static class VoidMeteor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> meteorBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> trailBlocks = new ArrayList<>();
        private static final int CORE_COUNT = 12;
        private float meteorX, meteorY, meteorZ;
        private float velX, velY, velZ;
        private boolean impacted = false;

        public VoidMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_meteor", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(300.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start high and offset, angled descent
            meteorX = (float)(Math.random() * 8 - 4);
            meteorY = 22.0f;
            meteorZ = (float)(Math.random() * 8 - 4);
            velX = -meteorX * 0.04f;
            velY = -0.45f;
            velZ = -meteorZ * 0.04f;

            Material[] mats = { Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BLACKSTONE,
                                Material.NETHERRACK, Material.COAL_BLOCK, Material.DEEPSLATE };

            for (int i = 0; i < CORE_COUNT; i++) {
                double ox = (Math.random() * 1.8 - 0.9);
                double oy = (Math.random() * 1.8 - 0.9);
                double oz = (Math.random() * 1.8 - 0.9);
                Location loc = center.clone().add(meteorX + ox, meteorY + oy, meteorZ + oz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                float s = 0.6f + (float)(Math.random() * 0.6);
                block.scale(s, s, s)
                     .glow(10, 0, 48)
                     .interpolation(2, 0);
                float tumble = (float)(Math.random() * 0.5);
                block.rotate(tumble, (float)Math.random(), (float)Math.random(), 0);
                meteorBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(meteorX, meteorY, meteorZ),
                    Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (impacted) {
                // Post-impact corruption spread
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(c, 10, 3.0, 45, 0, 64, 1.2f);
                }
                return;
            }

            // Move meteor
            meteorX += velX;
            meteorY += velY;
            meteorZ += velZ;
            velY -= 0.008f; // gravity acceleration

            for (int i = 0; i < meteorBlocks.size(); i++) {
                double ox = (Math.random() * 1.8 - 0.9);
                double oy = (Math.random() * 1.8 - 0.9);
                double oz = (Math.random() * 1.8 - 0.9);
                Location newLoc = c.clone().add(meteorX + ox, meteorY + oy, meteorZ + oz);
                meteorBlocks.get(i).entity().teleport(newLoc);

                // Tumble rotation
                float spin = ticksAlive * 0.1f + i * 0.5f;
                meteorBlocks.get(i).rotate(spin, 1, 0.5f, 0);
            }

            // Dark fire trail
            if (ticksAlive % 2 == 0) {
                Location trailLoc = c.clone().add(meteorX, meteorY + 1.5, meteorZ);
                DisplayBuilder.dustParticles(trailLoc, 6, 0.8, 74, 0, 0, 1.6f);
                DisplayBuilder.dustParticles(trailLoc.clone().add(0, 0.5, 0), 4, 0.5, 10, 0, 48, 1.2f);
            }

            // Whoosh sound during descent
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c.clone().add(meteorX, meteorY, meteorZ),
                        Sound.BLOCK_SCULK_SPREAD, 0.7f, 1.5f);
            }

            // Ground impact
            if (meteorY <= 0) {
                impacted = true;
                Location impactLoc = c.clone().add(meteorX, 0, meteorZ);
                triggerImpactDamage(impactLoc);

                // Massive shockwave
                DisplayBuilder.particleRing(impactLoc, 5.0, Particle.DUST, 48,
                        new Particle.DustOptions(Color.fromRGB(45, 0, 64), 2.5f));
                DisplayBuilder.particleRing(impactLoc, 3.0, Particle.DUST, 32,
                        new Particle.DustOptions(Color.fromRGB(74, 0, 0), 2.0f));
                w.spawnParticle(Particle.BLOCK, impactLoc, 50, 2.5, 0.5, 2.5, 0.3,
                        Material.OBSIDIAN.createBlockData());

                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.2f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.4f, 0.4f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidMeteor(plugin); }
    }

    // ================================================================
    // 94. SHADOW CANOPY — 20 blocks spread ceiling slowly descends,
    //     crushing darkness from above
    // ================================================================
    public static class ShadowCanopy extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> canopyBlocks = new ArrayList<>();
        private final List<Double> blockOffsetsX = new ArrayList<>();
        private final List<Double> blockOffsetsZ = new ArrayList<>();
        private static final int BLOCK_COUNT = 20;
        private static final float START_Y = 16.0f;
        private float currentY;

        public ShadowCanopy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_canopy", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(220.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            currentY = START_Y;
            Material[] mats = { Material.SCULK, Material.DEEPSLATE, Material.BLACKSTONE, Material.COAL_BLOCK, Material.TINTED_GLASS };

            // Grid-like canopy with slight randomness
            for (int i = 0; i < BLOCK_COUNT; i++) {
                int row = i / 5;
                int col = i % 5;
                double ox = (col - 2) * 2.0 + (Math.random() * 0.6 - 0.3);
                double oz = (row - 2) * 2.0 + (Math.random() * 0.6 - 0.3);
                blockOffsetsX.add(ox);
                blockOffsetsZ.add(oz);

                Location loc = center.clone().add(ox, START_Y, oz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                block.scale(1.8f, 0.5f, 1.8f)
                     .glow(45, 0, 64)
                     .interpolation(4, 0);
                canopyBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow descent — accelerates slightly over time
            float speed = 0.02f + (ticksAlive * 0.0003f);
            currentY -= speed;
            if (currentY < 2.0f) currentY = 2.0f;

            for (int i = 0; i < canopyBlocks.size(); i++) {
                Location newLoc = c.clone().add(blockOffsetsX.get(i), currentY, blockOffsetsZ.get(i));
                canopyBlocks.get(i).entity().teleport(newLoc);

                // Subtle wobble
                float wobble = (float) Math.sin(ticksAlive * 0.05 + i * 0.4) * 0.03f;
                canopyBlocks.get(i).rotate(wobble, 1, 0, 0);
            }

            // Shadow particles dripping down
            if (ticksAlive % 4 == 0) {
                for (int d = 0; d < 3; d++) {
                    double dx = c.getX() + (Math.random() * 8 - 4);
                    double dz = c.getZ() + (Math.random() * 8 - 4);
                    Location drip = new Location(w, dx, c.getY() + currentY - 0.5, dz);
                    DisplayBuilder.dustParticles(drip, 3, 0.2, 10, 0, 48, 1.0f);
                }
            }

            // Creaking sounds as it descends
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, currentY, 0), Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.3f);
            }

            // Heartbeat intensifies as canopy gets lower
            if (currentY < 8.0f && ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.6f + (8.0f - currentY) * 0.05f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowCanopy(plugin); }
    }

    // ================================================================
    // 95. DARK HALO — 16 blocks ring overhead, rotates, periodic
    //     energy drops that deal damage
    // ================================================================
    public static class DarkHalo extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<Float> baseAngles = new ArrayList<>();
        private static final int RING_COUNT = 16;
        private static final float RING_Y = 10.0f;
        private static final float RING_RADIUS = 4.0f;

        public DarkHalo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_halo", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(210.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.CRYING_OBSIDIAN, Material.SCULK, Material.BLACKSTONE, Material.OBSIDIAN };

            for (int i = 0; i < RING_COUNT; i++) {
                float angle = (float)(Math.PI * 2 / RING_COUNT) * i;
                baseAngles.add(angle);
                double ox = Math.cos(angle) * RING_RADIUS;
                double oz = Math.sin(angle) * RING_RADIUS;

                Location loc = center.clone().add(ox, RING_Y, oz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                block.scale(0.8f, 0.8f, 0.8f)
                     .glow(74, 0, 0)
                     .interpolation(2, 0);
                ringBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, RING_Y, 0), Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float rotation = ticksAlive * 0.04f;

            for (int i = 0; i < RING_COUNT; i++) {
                float angle = baseAngles.get(i) + rotation;
                double ox = Math.cos(angle) * RING_RADIUS;
                double oz = Math.sin(angle) * RING_RADIUS;
                Location newLoc = c.clone().add(ox, RING_Y, oz);
                ringBlocks.get(i).entity().teleport(newLoc);

                // Tilt blocks to face center
                float tiltAngle = angle + (float) Math.PI;
                ringBlocks.get(i).rotate(tiltAngle, 0, 1, 0);
            }

            // Ring glow particles
            if (ticksAlive % 3 == 0) {
                float pAngle = (float)(Math.random() * Math.PI * 2);
                Location glowLoc = c.clone().add(
                        Math.cos(pAngle) * RING_RADIUS, RING_Y, Math.sin(pAngle) * RING_RADIUS);
                DisplayBuilder.dustParticles(glowLoc, 3, 0.3, 74, 0, 0, 1.2f);
            }

            // Periodic energy drops — every 40 ticks, a corruption bolt falls from a random ring block
            if (ticksAlive % 40 == 0 && ticksAlive > 0) {
                int dropIdx = (int)(Math.random() * RING_COUNT);
                float angle = baseAngles.get(dropIdx) + rotation;
                double dropX = Math.cos(angle) * RING_RADIUS;
                double dropZ = Math.sin(angle) * RING_RADIUS;

                // Column of particles from ring to ground
                for (float dy = RING_Y; dy > 0; dy -= 0.5f) {
                    Location dropLoc = c.clone().add(dropX, dy, dropZ);
                    DisplayBuilder.dustParticles(dropLoc, 2, 0.15, 45, 0, 64, 1.5f);
                }

                Location groundHit = c.clone().add(dropX, 0, dropZ);
                DisplayBuilder.particleRing(groundHit, 2.0, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(10, 0, 48), 1.5f));
                DisplayBuilder.playSound(groundHit, Sound.BLOCK_SCULK_BREAK, 0.8f, 1.2f);
            }

            // Ambient rotation hum
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, RING_Y, 0), Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkHalo(plugin); }
    }

    // ================================================================
    // 96. CORRUPTION RAIN — 15 small blocks fall from varying heights,
    //     scattered across the arena
    // ================================================================
    public static class CorruptionRain extends BlockDisplayAttack {

        private static final int DROP_COUNT = 15;
        private final List<BlockDisplayHandle> drops = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Integer> spawnDelays = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();
        private final List<Float> spinRates = new ArrayList<>();

        public CorruptionRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_rain", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(3.8);
            config.setDurationTicks(720);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.SCULK, Material.SOUL_SOIL, Material.DEEPSLATE,
                                Material.COAL_BLOCK, Material.BLACKSTONE };

            for (int i = 0; i < DROP_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 7.0;
                xOffsets.add(Math.cos(angle) * dist);
                zOffsets.add(Math.sin(angle) * dist);
                yPositions.add(14.0f + (float)(Math.random() * 8.0));
                fallSpeeds.add(0.25f + (float)(Math.random() * 0.3));
                spawnDelays.add((int)(Math.random() * 40));
                spinRates.add(0.1f + (float)(Math.random() * 0.2));
                landed.add(false);
                drops.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, 18, 0), Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Material[] mats = { Material.SCULK, Material.SOUL_SOIL, Material.DEEPSLATE,
                                Material.COAL_BLOCK, Material.BLACKSTONE };

            for (int i = 0; i < DROP_COUNT; i++) {
                if (ticksAlive < spawnDelays.get(i)) continue;
                if (landed.get(i)) continue;

                if (drops.get(i) == null) {
                    Location spawnLoc = c.clone().add(xOffsets.get(i), yPositions.get(i), zOffsets.get(i));
                    BlockDisplayHandle drop = displayBuilder.spawnBlock(spawnLoc, mats[i % mats.length]);
                    drop.scale(0.5f, 0.5f, 0.5f)
                        .glow(45, 0, 64)
                        .interpolation(1, 0);
                    drops.set(i, drop);
                    spawnedEntities.add(drop.entity());
                }

                float y = yPositions.get(i) - fallSpeeds.get(i);
                yPositions.set(i, y);

                BlockDisplayHandle drop = drops.get(i);
                Location newLoc = c.clone().add(xOffsets.get(i), y, zOffsets.get(i));
                drop.entity().teleport(newLoc);

                // Spin while falling
                float spin = ticksAlive * spinRates.get(i);
                drop.rotate(spin, 1, 0.3f, 0.7f);

                // Trail particles
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(newLoc.clone().add(0, 0.3, 0), 2, 0.15, 10, 0, 48, 0.8f);
                }

                // Impact
                if (y <= 0) {
                    yPositions.set(i, 0f);
                    landed.set(i, true);

                    Location impactLoc = c.clone().add(xOffsets.get(i), 0, zOffsets.get(i));
                    triggerImpactDamage(impactLoc);

                    DisplayBuilder.particleRing(impactLoc, 2.5, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.2f));
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_SCULK_BREAK, 0.6f, 0.8f);
                }
            }

            // Ambient corruption
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionRain(plugin); }
    }

    // ================================================================
    // 97. VOID ECLIPSE — 14 blocks disc overhead, shadow darkens
    //     the area below with oppressive void energy
    // ================================================================
    public static class VoidEclipse extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> discBlocks = new ArrayList<>();
        private final List<Float> baseAngles = new ArrayList<>();
        private static final int BLOCK_COUNT = 14;
        private static final float ECLIPSE_Y = 12.0f;
        private float pulsePhase = 0;

        public VoidEclipse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_eclipse", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(220.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(800);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.OBSIDIAN, Material.COAL_BLOCK, Material.BLACKSTONE,
                                Material.TINTED_GLASS, Material.DEEPSLATE };

            // Dense disc — inner and outer ring
            for (int i = 0; i < BLOCK_COUNT; i++) {
                float angle = (float)(Math.PI * 2 / BLOCK_COUNT) * i;
                baseAngles.add(angle);
                double radius = (i < 6) ? 1.2 : 3.0;
                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;

                Location loc = center.clone().add(ox, ECLIPSE_Y, oz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                float s = (i < 6) ? 1.8f : 1.2f;
                block.scale(s, 0.3f, s)
                     .glow(10, 0, 48)
                     .interpolation(3, 0);
                discBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, ECLIPSE_Y, 0), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            pulsePhase += 0.05f;

            // Slow rotation of the disc
            float rot = ticksAlive * 0.02f;
            for (int i = 0; i < BLOCK_COUNT; i++) {
                float angle = baseAngles.get(i) + rot;
                double radius = (i < 6) ? 1.2 : 3.0;
                // Pulse the radius
                double pulsedRadius = radius + Math.sin(pulsePhase) * 0.3;
                double ox = Math.cos(angle) * pulsedRadius;
                double oz = Math.sin(angle) * pulsedRadius;
                Location newLoc = c.clone().add(ox, ECLIPSE_Y, oz);
                discBlocks.get(i).entity().teleport(newLoc);
            }

            // Shadow beam — column of dark particles from disc to ground
            if (ticksAlive % 2 == 0) {
                for (float dy = ECLIPSE_Y; dy > 0; dy -= 1.0f) {
                    double spread = (ECLIPSE_Y - dy) * 0.15; // widens as it goes down
                    Location shadowLoc = c.clone().add(
                            (Math.random() - 0.5) * spread,
                            dy,
                            (Math.random() - 0.5) * spread);
                    DisplayBuilder.dustParticles(shadowLoc, 1, 0.1, 10, 0, 48, 1.0f);
                }
            }

            // Edge glow particles
            if (ticksAlive % 5 == 0) {
                float pAngle = (float)(Math.random() * Math.PI * 2);
                Location edgeLoc = c.clone().add(Math.cos(pAngle) * 3.0, ECLIPSE_Y, Math.sin(pAngle) * 3.0);
                DisplayBuilder.dustParticles(edgeLoc, 4, 0.3, 74, 0, 0, 1.4f);
            }

            // Void hum
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, ECLIPSE_Y, 0), Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.2f);
            }

            // Heartbeat under eclipse
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEclipse(plugin); }
    }

    // ================================================================
    // 98. SHADOW CEILING — 18 blocks flat grid, drops individual
    //     blocks periodically onto targets below
    // ================================================================
    public static class ShadowCeiling extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ceilingBlocks = new ArrayList<>();
        private final List<Double> gridX = new ArrayList<>();
        private final List<Double> gridZ = new ArrayList<>();
        private final List<Boolean> dropped = new ArrayList<>();
        private final List<BlockDisplayHandle> fallingBlocks = new ArrayList<>();
        private final List<Float> fallingY = new ArrayList<>();
        private static final int GRID_COUNT = 18;
        private static final float CEILING_Y = 12.0f;
        private int nextDropTick = 30;

        public ShadowCeiling(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_ceiling", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(210.0);
            config.setDamageRadius(3.8);
            config.setDurationTicks(1000);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(42.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.SCULK, Material.DEEPSLATE, Material.BLACKSTONE,
                                Material.COAL_BLOCK, Material.SOUL_SOIL, Material.OBSIDIAN };

            // 6x3 grid layout
            int idx = 0;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 6; col++) {
                    if (idx >= GRID_COUNT) break;
                    double ox = (col - 2.5) * 1.8 + (Math.random() * 0.3 - 0.15);
                    double oz = (row - 1) * 1.8 + (Math.random() * 0.3 - 0.15);
                    gridX.add(ox);
                    gridZ.add(oz);
                    dropped.add(false);

                    Location loc = center.clone().add(ox, CEILING_Y, oz);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[idx % mats.length]);
                    block.scale(1.6f, 0.6f, 1.6f)
                         .glow(45, 0, 64)
                         .interpolation(2, 0);
                    ceilingBlocks.add(block);
                    spawnedEntities.add(block.entity());
                    idx++;
                }
            }

            DisplayBuilder.playSound(center.clone().add(0, CEILING_Y, 0), Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Drop blocks at intervals
            if (ticksAlive >= nextDropTick) {
                // Find a block that hasn't dropped yet
                List<Integer> available = new ArrayList<>();
                for (int i = 0; i < GRID_COUNT; i++) {
                    if (!dropped.get(i)) available.add(i);
                }

                if (!available.isEmpty()) {
                    int pickIdx = available.get((int)(Math.random() * available.size()));
                    dropped.set(pickIdx, true);

                    // The ceiling block becomes a falling block
                    BlockDisplayHandle falling = ceilingBlocks.get(pickIdx);
                    falling.scale(1.0f, 1.0f, 1.0f); // Compact shape for falling
                    fallingBlocks.add(falling);
                    fallingY.add(CEILING_Y);

                    DisplayBuilder.playSound(c.clone().add(gridX.get(pickIdx), CEILING_Y, gridZ.get(pickIdx)),
                            Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.5f);

                    nextDropTick = ticksAlive + 20 + (int)(Math.random() * 15);
                }
            }

            // Animate falling blocks
            for (int f = fallingBlocks.size() - 1; f >= 0; f--) {
                float y = fallingY.get(f) - 0.5f;
                fallingY.set(f, y);

                // Find which grid index this block was
                int gridIdx = ceilingBlocks.indexOf(fallingBlocks.get(f));
                if (gridIdx < 0) continue;

                Location newLoc = c.clone().add(gridX.get(gridIdx), y, gridZ.get(gridIdx));
                fallingBlocks.get(f).entity().teleport(newLoc);

                // Spin while falling
                float spin = (CEILING_Y - y) * 0.15f;
                fallingBlocks.get(f).rotate(spin, 1, 0, 0.5f);

                // Trail
                DisplayBuilder.dustParticles(newLoc.clone().add(0, 0.5, 0), 2, 0.2, 10, 0, 48, 0.8f);

                // Impact
                if (y <= 0) {
                    Location impactLoc = c.clone().add(gridX.get(gridIdx), 0, gridZ.get(gridIdx));
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.particleRing(impactLoc, 2.5, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.3f));
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_SCULK_BREAK, 0.7f, 0.6f);
                    fallingBlocks.remove(f);
                    fallingY.remove(f);
                }
            }

            // Ceiling creak ambience
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, CEILING_Y, 0), Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowCeiling(plugin); }
    }

    // ================================================================
    // 99. DARK COMET — 12 blocks + trail streaks across sky with
    //     whoosh sound, leaves corruption where it passes
    // ================================================================
    public static class DarkComet extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cometBlocks = new ArrayList<>();
        private static final int CORE_COUNT = 12;
        private float cometX, cometY, cometZ;
        private float dirX, dirZ;
        private static final float COMET_SPEED = 0.6f;

        public DarkComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_comet", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(230.0);
            config.setDamageRadius(5.2);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start from one side, streak across to the other
            double startAngle = Math.random() * Math.PI * 2;
            cometX = (float)(Math.cos(startAngle) * 15);
            cometY = 10.0f + (float)(Math.random() * 4);
            cometZ = (float)(Math.sin(startAngle) * 15);
            dirX = -cometX / 25.0f;
            dirZ = -cometZ / 25.0f;

            Material[] mats = { Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.NETHERRACK,
                                Material.COAL_BLOCK, Material.BLACKSTONE, Material.SCULK };

            for (int i = 0; i < CORE_COUNT; i++) {
                double ox = (Math.random() * 1.4 - 0.7);
                double oy = (Math.random() * 1.4 - 0.7);
                double oz = (Math.random() * 1.4 - 0.7);
                Location loc = center.clone().add(cometX + ox, cometY + oy, cometZ + oz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                float s = 0.4f + (float)(Math.random() * 0.5);
                block.scale(s, s, s)
                     .glow(74, 0, 0)
                     .interpolation(1, 0);
                cometBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(cometX, cometY, cometZ),
                    Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Move comet across sky
            cometX += dirX;
            cometZ += dirZ;
            cometY -= 0.01f; // slight descent arc

            for (int i = 0; i < cometBlocks.size(); i++) {
                double ox = (Math.random() * 1.4 - 0.7);
                double oy = (Math.random() * 1.4 - 0.7);
                double oz = (Math.random() * 1.4 - 0.7);
                Location newLoc = c.clone().add(cometX + ox, cometY + oy, cometZ + oz);
                cometBlocks.get(i).entity().teleport(newLoc);

                // Tumble
                float tumble = ticksAlive * 0.15f + i;
                cometBlocks.get(i).rotate(tumble, 0.7f, 1, 0.3f);
            }

            // Corruption trail behind comet
            if (ticksAlive % 2 == 0) {
                Location trailLoc = c.clone().add(cometX - dirX * 3, cometY, cometZ - dirZ * 3);
                DisplayBuilder.dustParticles(trailLoc, 8, 1.0, 45, 0, 64, 1.6f);
                DisplayBuilder.dustParticles(trailLoc.clone().add(0, 0.5, 0), 5, 0.8, 74, 0, 0, 1.2f);
            }

            // Whoosh sound periodically
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c.clone().add(cometX, cometY, cometZ),
                        Sound.BLOCK_SCULK_SPREAD, 0.8f, 1.8f);
            }

            // Drop corruption particles below path
            if (ticksAlive % 5 == 0) {
                Location below = c.clone().add(cometX, 0.5, cometZ);
                DisplayBuilder.dustParticles(below, 4, 1.0, 10, 0, 48, 1.0f);
            }

            // Ambient heartbeat
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkComet(plugin); }
    }

    // ================================================================
    // 100. CORRUPTION AURORA — 20 blocks shimmering curtain,
    //      undulating wave pattern in the sky
    // ================================================================
    public static class CorruptionAurora extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> auroraBlocks = new ArrayList<>();
        private final List<Float> baseX = new ArrayList<>();
        private final List<Float> baseY = new ArrayList<>();
        private static final int BLOCK_COUNT = 20;

        public CorruptionAurora(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_aurora", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(900);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.TINTED_GLASS, Material.SCULK, Material.CRYING_OBSIDIAN,
                                Material.SOUL_SOIL, Material.DEEPSLATE };

            // Horizontal curtain — spread along X axis, stacked vertically
            for (int i = 0; i < BLOCK_COUNT; i++) {
                int col = i % 10;
                int row = i / 10;
                float x = (col - 4.5f) * 1.5f;
                float y = 8.0f + row * 2.5f;
                baseX.add(x);
                baseY.add(y);

                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                block.scale(1.3f, 2.0f, 0.3f)
                     .glow(45, 0, 64)
                     .interpolation(3, 0);
                auroraBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 10, 0), Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float x = baseX.get(i);
                float y = baseY.get(i);

                // Undulating wave motion
                float waveOffset = (float) Math.sin(ticksAlive * 0.06 + x * 0.4) * 1.5f;
                float vertWave = (float) Math.cos(ticksAlive * 0.04 + x * 0.3) * 0.6f;
                float zSway = (float) Math.sin(ticksAlive * 0.03 + i * 0.5) * 1.2f;

                Location newLoc = c.clone().add(x, y + vertWave, zSway);
                auroraBlocks.get(i).entity().teleport(newLoc);

                // Shimmer rotation
                float shimmer = (float) Math.sin(ticksAlive * 0.08 + i * 0.3) * 0.12f;
                auroraBlocks.get(i).rotate(shimmer, 0, 0, 1);
            }

            // Shimmering particles along the aurora
            if (ticksAlive % 3 == 0) {
                int idx = (int)(Math.random() * BLOCK_COUNT);
                float x = baseX.get(idx);
                float y = baseY.get(idx);
                float zSway = (float) Math.sin(ticksAlive * 0.03 + idx * 0.5) * 1.2f;
                Location shimmerLoc = c.clone().add(x, y, zSway);

                // Alternate between the three corruption colors
                int colorChoice = ticksAlive % 9;
                if (colorChoice < 3) {
                    DisplayBuilder.dustParticles(shimmerLoc, 3, 0.4, 45, 0, 64, 1.3f);
                } else if (colorChoice < 6) {
                    DisplayBuilder.dustParticles(shimmerLoc, 3, 0.4, 74, 0, 0, 1.3f);
                } else {
                    DisplayBuilder.dustParticles(shimmerLoc, 3, 0.4, 10, 0, 48, 1.3f);
                }
            }

            // Energy drips falling from aurora
            if (ticksAlive % 8 == 0) {
                int dripIdx = (int)(Math.random() * BLOCK_COUNT);
                float x = baseX.get(dripIdx);
                float y = baseY.get(dripIdx);
                for (float dy = y; dy > 0; dy -= 1.5f) {
                    DisplayBuilder.dustParticles(c.clone().add(x, dy, 0), 1, 0.1, 10, 0, 48, 0.7f);
                }
            }

            // Ethereal hum
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 10, 0), Sound.BLOCK_SCULK_SPREAD, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionAurora(plugin); }
    }

    // ================================================================
    // 101. VOID FUNNEL — 16 blocks inverted tornado from sky
    //      that touches down, pulling darkness from above
    // ================================================================
    public static class VoidFunnel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> funnelBlocks = new ArrayList<>();
        private final List<Float> blockHeights = new ArrayList<>();
        private final List<Float> blockAngles = new ArrayList<>();
        private static final int BLOCK_COUNT = 16;
        private float touchdownProgress = 0;

        public VoidFunnel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_funnel", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(230.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.SCULK, Material.OBSIDIAN, Material.DEEPSLATE,
                                Material.BLACKSTONE, Material.TINTED_GLASS, Material.COAL_BLOCK };

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float height = 4.0f + (i * 12.0f / BLOCK_COUNT); // spread from 4 to 16 blocks high
                float angle = (float)(Math.PI * 2 / BLOCK_COUNT) * i;
                blockHeights.add(height);
                blockAngles.add(angle);

                // Radius widens toward top (inverted tornado)
                float radius = 0.5f + (height / 16.0f) * 5.0f;
                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;

                Location loc = center.clone().add(ox, height, oz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                float s = 0.6f + (height / 16.0f) * 0.8f;
                block.scale(s, s, s)
                     .glow(10, 0, 48)
                     .interpolation(2, 0);
                funnelBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 10, 0), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Gradually tighten the funnel toward ground (touchdown)
            touchdownProgress = Math.min(1.0f, ticksAlive / 120.0f);
            float rotSpeed = 0.08f + touchdownProgress * 0.06f;
            float rotation = ticksAlive * rotSpeed;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float height = blockHeights.get(i);
                float angle = blockAngles.get(i) + rotation;

                // Radius narrows toward bottom, widens at top
                float normalHeight = height / 16.0f;
                float radius = (0.5f + normalHeight * 5.0f) * (1.0f - touchdownProgress * 0.3f * (1.0f - normalHeight));

                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;

                // Lower blocks descend as funnel touches down
                float yDrop = touchdownProgress * (1.0f - normalHeight) * 3.0f;
                Location newLoc = c.clone().add(ox, height - yDrop, oz);
                funnelBlocks.get(i).entity().teleport(newLoc);

                // Spin blocks around their own axis
                float selfSpin = ticksAlive * 0.12f + i;
                funnelBlocks.get(i).rotate(selfSpin, 0, 1, 0);
            }

            // Suction particles spiraling inward at bottom
            if (ticksAlive % 3 == 0) {
                float pAngle = (float)(Math.random() * Math.PI * 2);
                float pRadius = 2.0f + (float)(Math.random() * 3);
                Location sucLoc = c.clone().add(
                        Math.cos(pAngle) * pRadius, 1.0 + Math.random() * 3, Math.sin(pAngle) * pRadius);
                DisplayBuilder.dustParticles(sucLoc, 3, 0.3, 45, 0, 64, 1.2f);
            }

            // Void particles at apex
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 16, 0), 6, 3.0, 10, 0, 48, 1.5f);
            }

            // Vortex whoosh
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.4f + touchdownProgress * 0.8f);
            }

            // Heartbeat on full touchdown
            if (touchdownProgress > 0.8f && ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidFunnel(plugin); }
    }

    // ================================================================
    // 102. SHADOW BOMBARDMENT — 14 blocks fall at angles like
    //      artillery shells, staggered impacts
    // ================================================================
    public static class ShadowBombardment extends BlockDisplayAttack {

        private static final int SHELL_COUNT = 14;
        private final List<BlockDisplayHandle> shells = new ArrayList<>();
        private final List<Float> shellX = new ArrayList<>();
        private final List<Float> shellY = new ArrayList<>();
        private final List<Float> shellZ = new ArrayList<>();
        private final List<Float> velX = new ArrayList<>();
        private final List<Float> velY = new ArrayList<>();
        private final List<Float> velZ = new ArrayList<>();
        private final List<Integer> launchDelays = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();

        public ShadowBombardment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_bombardment", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(220.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(800);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SHELL_COUNT; i++) {
                // Launch from random elevated positions around the perimeter
                double launchAngle = Math.random() * Math.PI * 2;
                float launchDist = 12.0f + (float)(Math.random() * 6);
                float startX = (float)(Math.cos(launchAngle) * launchDist);
                float startZ = (float)(Math.sin(launchAngle) * launchDist);
                float startY = 14.0f + (float)(Math.random() * 6);

                shellX.add(startX);
                shellY.add(startY);
                shellZ.add(startZ);

                // Velocity aimed toward center area with some scatter
                float targetX = (float)(Math.random() * 6 - 3);
                float targetZ = (float)(Math.random() * 6 - 3);
                float flightTicks = 40.0f + (float)(Math.random() * 20);
                velX.add((targetX - startX) / flightTicks);
                velY.add(-startY / flightTicks);
                velZ.add((targetZ - startZ) / flightTicks);

                launchDelays.add(i * 8 + (int)(Math.random() * 5));
                impacted.add(false);
                shells.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, 14, 0), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Material[] mats = { Material.BLACKSTONE, Material.DEEPSLATE, Material.OBSIDIAN,
                                Material.COAL_BLOCK, Material.NETHERRACK };

            for (int i = 0; i < SHELL_COUNT; i++) {
                if (ticksAlive < launchDelays.get(i)) continue;
                if (impacted.get(i)) continue;

                // Spawn shell on its delay
                if (shells.get(i) == null) {
                    Location spawnLoc = c.clone().add(shellX.get(i), shellY.get(i), shellZ.get(i));
                    BlockDisplayHandle shell = displayBuilder.spawnBlock(spawnLoc, mats[i % mats.length]);
                    shell.scale(0.7f, 0.7f, 0.7f)
                         .glow(74, 0, 0)
                         .interpolation(1, 0);
                    shells.set(i, shell);
                    spawnedEntities.add(shell.entity());

                    DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_SCULK_SPREAD, 0.6f, 1.5f);
                }

                // Move shell
                shellX.set(i, shellX.get(i) + velX.get(i));
                shellY.set(i, shellY.get(i) + velY.get(i));
                shellZ.set(i, shellZ.get(i) + velZ.get(i));

                Location newLoc = c.clone().add(shellX.get(i), shellY.get(i), shellZ.get(i));
                shells.get(i).entity().teleport(newLoc);

                // Angle the shell along its trajectory
                float trajectoryAngle = (float) Math.atan2(velY.get(i), Math.sqrt(velX.get(i) * velX.get(i) + velZ.get(i) * velZ.get(i)));
                shells.get(i).rotate(trajectoryAngle, 1, 0, 0);

                // Smoke trail
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(newLoc, 3, 0.3, 45, 0, 64, 1.0f);
                }

                // Impact
                if (shellY.get(i) <= 0) {
                    impacted.set(i, true);
                    Location impactLoc = c.clone().add(shellX.get(i), 0, shellZ.get(i));
                    triggerImpactDamage(impactLoc);

                    DisplayBuilder.particleRing(impactLoc, 3.0, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(74, 0, 0), 1.8f));
                    w.spawnParticle(Particle.BLOCK, impactLoc, 25, 1.5, 0.3, 1.5, 0.2,
                            Material.DEEPSLATE.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_SCULK_BREAK, 0.8f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowBombardment(plugin); }
    }

    // ================================================================
    // 103. DARK STARFALL — 18 small blocks descend at varying speeds,
    //      like falling stars of pure darkness
    // ================================================================
    public static class DarkStarfall extends BlockDisplayAttack {

        private static final int STAR_COUNT = 18;
        private final List<BlockDisplayHandle> stars = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> fallSpeeds = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> burned = new ArrayList<>();
        private final List<Float> trailAngles = new ArrayList<>();

        public DarkStarfall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_starfall", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(200.0);
            config.setDamageRadius(3.8);
            config.setDurationTicks(800);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < STAR_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 8.0;
                xOffsets.add(Math.cos(angle) * dist);
                zOffsets.add(Math.sin(angle) * dist);
                yPositions.add(16.0f + (float)(Math.random() * 10.0));
                fallSpeeds.add(0.15f + (float)(Math.random() * 0.35));
                delays.add((int)(Math.random() * 60));
                trailAngles.add((float)(Math.random() * Math.PI * 2));
                burned.add(false);
                stars.add(null);
            }

            DisplayBuilder.playSound(center.clone().add(0, 20, 0), Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Material[] mats = { Material.SCULK, Material.COAL_BLOCK, Material.SOUL_SOIL,
                                Material.OBSIDIAN, Material.BLACKSTONE, Material.DEEPSLATE };

            for (int i = 0; i < STAR_COUNT; i++) {
                if (ticksAlive < delays.get(i)) continue;
                if (burned.get(i)) continue;

                if (stars.get(i) == null) {
                    Location spawnLoc = c.clone().add(xOffsets.get(i), yPositions.get(i), zOffsets.get(i));
                    BlockDisplayHandle star = displayBuilder.spawnBlock(spawnLoc, mats[i % mats.length]);
                    star.scale(0.35f, 0.35f, 0.35f)
                        .glow(45, 0, 64)
                        .interpolation(1, 0);
                    stars.set(i, star);
                    spawnedEntities.add(star.entity());
                }

                // Fall with slight drift
                float y = yPositions.get(i) - fallSpeeds.get(i);
                yPositions.set(i, y);

                float drift = (float) Math.sin(ticksAlive * 0.05 + i) * 0.02f;
                xOffsets.set(i, xOffsets.get(i) + drift);

                BlockDisplayHandle star = stars.get(i);
                Location newLoc = c.clone().add(xOffsets.get(i), y, zOffsets.get(i));
                star.entity().teleport(newLoc);

                // Twinkle spin
                float spin = ticksAlive * (0.15f + fallSpeeds.get(i) * 0.3f);
                star.rotate(spin, 0.5f, 1, 0.3f);

                // Dark trail
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(newLoc.clone().add(0, 0.2, 0), 2, 0.1, 10, 0, 48, 0.9f);
                    DisplayBuilder.dustParticles(newLoc.clone().add(0, 0.5, 0), 1, 0.05, 74, 0, 0, 0.6f);
                }

                // Impact — dark star burn
                if (y <= 0) {
                    burned.set(i, true);
                    Location impactLoc = c.clone().add(xOffsets.get(i), 0, zOffsets.get(i));
                    triggerImpactDamage(impactLoc);

                    DisplayBuilder.particleRing(impactLoc, 2.5, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.2f));
                    DisplayBuilder.dustParticles(impactLoc, 8, 0.6, 74, 0, 0, 1.0f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_SCULK_BREAK, 0.5f, 1.0f);
                }
            }

            // Ambient void
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 15, 0), Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkStarfall(plugin); }
    }

    // ================================================================
    // 104. CORRUPTION VORTEX — 16 blocks spiral upward in a reverse
    //      tornado, pulling corruption from the ground skyward
    // ================================================================
    public static class CorruptionVortex extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> vortexBlocks = new ArrayList<>();
        private final List<Float> blockPhases = new ArrayList<>();
        private static final int BLOCK_COUNT = 16;
        private static final float VORTEX_HEIGHT = 14.0f;

        public CorruptionVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_vortex", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(220.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.SCULK, Material.CRYING_OBSIDIAN, Material.NETHERRACK,
                                Material.BLACKSTONE, Material.DEEPSLATE, Material.SOUL_SOIL };

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float phase = (float)(Math.PI * 2 / BLOCK_COUNT) * i;
                blockPhases.add(phase);

                float height = (i / (float) BLOCK_COUNT) * VORTEX_HEIGHT;
                float radius = 4.0f - (height / VORTEX_HEIGHT) * 3.0f; // wide at bottom, narrow at top
                double ox = Math.cos(phase) * radius;
                double oz = Math.sin(phase) * radius;

                Location loc = center.clone().add(ox, height + 1.0, oz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                float s = 0.8f - (height / VORTEX_HEIGHT) * 0.3f;
                block.scale(s, s, s)
                     .glow(45, 0, 64)
                     .interpolation(2, 0);
                vortexBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.6f);
            DisplayBuilder.playSound(center.clone().add(0, 7, 0), Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float rotSpeed = 0.1f;
            float rotation = ticksAlive * rotSpeed;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float phase = blockPhases.get(i) + rotation;

                // Blocks spiral upward over time
                float baseHeight = (i / (float) BLOCK_COUNT) * VORTEX_HEIGHT;
                float heightOffset = (float)(Math.sin(ticksAlive * 0.04 + i * 0.3) * 1.0);
                float height = baseHeight + 1.0f + heightOffset;

                float radius = 4.0f - (baseHeight / VORTEX_HEIGHT) * 3.0f;
                // Pulsating radius
                radius += (float) Math.sin(ticksAlive * 0.06 + i * 0.4) * 0.4f;

                double ox = Math.cos(phase) * radius;
                double oz = Math.sin(phase) * radius;

                Location newLoc = c.clone().add(ox, height, oz);
                vortexBlocks.get(i).entity().teleport(newLoc);

                // Self-rotation
                float selfSpin = ticksAlive * 0.15f + i;
                vortexBlocks.get(i).rotate(selfSpin, 0, 1, 0);
            }

            // Upward spiral particles at base
            if (ticksAlive % 2 == 0) {
                float pAngle = rotation + (float)(Math.random() * Math.PI);
                float pRadius = 3.5f + (float)(Math.random() * 1.5);
                Location baseLoc = c.clone().add(
                        Math.cos(pAngle) * pRadius, 0.5 + Math.random(), Math.sin(pAngle) * pRadius);
                DisplayBuilder.dustParticles(baseLoc, 4, 0.3, 74, 0, 0, 1.3f);
            }

            // Top dissipation particles
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, VORTEX_HEIGHT + 1, 0), 5, 1.5, 10, 0, 48, 1.0f);
            }

            // Vortex sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.6f + (float)(Math.sin(ticksAlive * 0.03) * 0.3));
            }

            // Ground corruption
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.particleRing(c, 4.0, Particle.DUST, 24,
                        new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.0f));
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionVortex(plugin); }
    }

    // ================================================================
    // 105. VOID OMEN — 12 blocks form an ominous sigil/symbol in
    //      the sky, pulses with dark glow
    // ================================================================
    public static class VoidOmen extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sigilBlocks = new ArrayList<>();
        private final List<Float> sigilX = new ArrayList<>();
        private final List<Float> sigilY = new ArrayList<>();
        private static final int BLOCK_COUNT = 12;
        private static final float SIGIL_HEIGHT = 12.0f;

        public VoidOmen(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_omen", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(210.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(900);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = { Material.CRYING_OBSIDIAN, Material.SCULK, Material.OBSIDIAN,
                                Material.TINTED_GLASS, Material.BLACKSTONE, Material.DEEPSLATE };

            // Diamond/eye-shaped sigil layout
            // Points of a stylized eye/diamond: top, upper-left, left, lower-left, bottom,
            // lower-right, right, upper-right, plus inner cross
            float[][] sigilPattern = {
                { 0.0f,  3.0f},  // top
                {-1.5f,  2.0f},  // upper-left
                {-3.0f,  0.0f},  // left
                {-1.5f, -2.0f},  // lower-left
                { 0.0f, -3.0f},  // bottom
                { 1.5f, -2.0f},  // lower-right
                { 3.0f,  0.0f},  // right
                { 1.5f,  2.0f},  // upper-right
                { 0.0f,  1.0f},  // inner top
                {-1.0f,  0.0f},  // inner left
                { 0.0f, -1.0f},  // inner bottom
                { 1.0f,  0.0f},  // inner right
            };

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float sx = sigilPattern[i][0];
                float sy = sigilPattern[i][1];
                sigilX.add(sx);
                sigilY.add(sy);

                Location loc = center.clone().add(sx, SIGIL_HEIGHT + sy, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                float s = (i < 8) ? 0.8f : 0.5f; // outer blocks larger
                block.scale(s, s, 0.3f)
                     .glow(10, 0, 48)
                     .interpolation(3, 0);
                sigilBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, SIGIL_HEIGHT, 0),
                    Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.3f);
            DisplayBuilder.playSound(center.clone().add(0, SIGIL_HEIGHT, 0),
                    Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Pulse effect — scale oscillation
            float pulse = (float)(Math.sin(ticksAlive * 0.08) * 0.3 + 1.0);
            float glowPulse = (float)(Math.sin(ticksAlive * 0.08) * 0.5 + 0.5);

            // Slow rotation of entire sigil around its center
            float rot = ticksAlive * 0.015f;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                float sx = sigilX.get(i);
                float sy = sigilY.get(i);

                // Rotate sigil points
                float rotX = (float)(sx * Math.cos(rot) - sy * Math.sin(rot));
                float rotY = (float)(sx * Math.sin(rot) + sy * Math.cos(rot));

                // Pulse scale
                float pulsedX = rotX * pulse;
                float pulsedY = rotY * pulse;

                Location newLoc = c.clone().add(pulsedX, SIGIL_HEIGHT + pulsedY, 0);
                sigilBlocks.get(i).entity().teleport(newLoc);

                // Orient blocks to face outward from center
                float faceAngle = (float) Math.atan2(sy, sx) + rot;
                sigilBlocks.get(i).rotate(faceAngle, 0, 0, 1);
            }

            // Dark glow pulse particles around sigil
            if (ticksAlive % 4 == 0) {
                for (int p = 0; p < 3; p++) {
                    float pAngle = (float)(Math.random() * Math.PI * 2);
                    float pDist = 2.0f + (float)(Math.random() * 2.0) * pulse;
                    Location glowLoc = c.clone().add(
                            Math.cos(pAngle) * pDist,
                            SIGIL_HEIGHT + Math.sin(pAngle) * pDist,
                            (Math.random() - 0.5) * 0.5);

                    // Alternate glow colors with pulse
                    if (glowPulse > 0.5f) {
                        DisplayBuilder.dustParticles(glowLoc, 2, 0.2, 45, 0, 64, 1.5f * pulse);
                    } else {
                        DisplayBuilder.dustParticles(glowLoc, 2, 0.2, 10, 0, 48, 1.5f * pulse);
                    }
                }
            }

            // Ominous beam of light from sigil to ground
            if (ticksAlive % 6 == 0) {
                for (float dy = SIGIL_HEIGHT; dy > 0; dy -= 1.2f) {
                    float beamSpread = (SIGIL_HEIGHT - dy) * 0.1f;
                    Location beamLoc = c.clone().add(
                            (Math.random() - 0.5) * beamSpread, dy, (Math.random() - 0.5) * beamSpread);
                    DisplayBuilder.dustParticles(beamLoc, 1, 0.05, 74, 0, 0, 0.8f);
                }
            }

            // Heartbeat intensifies with pulse
            if (ticksAlive % 20 == 0) {
                float vol = 0.5f + glowPulse * 0.5f;
                DisplayBuilder.playSound(c.clone().add(0, SIGIL_HEIGHT, 0),
                        Sound.ENTITY_WARDEN_HEARTBEAT, vol, 0.4f);
            }

            // Sculk ambience
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, SIGIL_HEIGHT, 0),
                        Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.3f);
            }

            // Anchor deplete for ominous dread
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidOmen(plugin); }
    }
}
