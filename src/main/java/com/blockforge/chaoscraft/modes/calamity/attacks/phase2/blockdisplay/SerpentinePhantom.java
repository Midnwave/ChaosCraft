package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.blockdisplay;

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
 * Phase 2 (DoG) Block Display -- GROUP 5: SERPENTINE PHANTOM FORMS
 * 10 structures: phantom echoes of the Devourer of Gods' body.
 * Devourer of Gods palette: amethyst, cyan, dark prismarine, polished blackstone.
 *
 * Design rules:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG glow colors: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 */
public final class SerpentinePhantom {

    private SerpentinePhantom() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PhantomBodySegment(plugin));
        registry.register(new SegmentedEchoChain(plugin));
        registry.register(new HeadEcho(plugin));
        registry.register(new TailWhipEcho(plugin));
        registry.register(new SpineCrown(plugin));
        registry.register(new DimensionalScarSerpent(plugin));
        registry.register(new PhaseRiftTrail(plugin));
        registry.register(new CosmicWake(plugin));
        registry.register(new PhantomCoil(plugin));
        registry.register(new SegmentedSpiral(plugin));
    }

    // ================================================================
    // 36. PHANTOM BODY SEGMENT -- Single floating body-echo ring
    // ================================================================
    public static class PhantomBodySegment extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> interiorGlass = new ArrayList<>();
        private final List<BlockDisplayHandle> spineClusters = new ArrayList<>();
        private float driftX = 0;
        private float driftZ = 0;

        public PhantomBodySegment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_body_segment", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Oval ring: 16 amethyst blocks in elliptical profile (3 wide x 2 tall)
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double x = Math.cos(angle) * 1.5;
                double y = Math.sin(angle) * 1.0;
                Location loc = center.clone().add(x, 3 + y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior: 6 cyan stained glass
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * 0.8;
                double y = Math.sin(angle) * 0.5;
                Location loc = center.clone().add(x, 3 + y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                interiorGlass.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 spine clusters pointing outward radially
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 3, Math.sin(angle) * 0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                spineClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Segment drifts across arena: 0.05 blocks per tick in X direction
            driftX += 0.05f;
            float bob = 0.3f * (float) Math.sin(ticksAlive * (2 * Math.PI / 30));

            // Reset drift after reaching 20 blocks
            if (driftX > 20) {
                driftX = -10;
            }

            Location segCenter = center.clone().add(driftX, 3 + bob, driftZ);

            // Scale pulse: 0.95 to 1.05, 40-tick cycle
            float pulse = 1.0f + 0.05f * (float) Math.sin(ticksAlive * (2 * Math.PI / 40));

            // Update ring block positions
            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = segCenter.clone().add(Math.cos(angle) * 1.5 * pulse, Math.sin(angle) * 1.0, 0);
                ringBlocks.get(i).entity().teleport(loc);
            }

            // Interior glass rotates independently at 2 deg/tick
            float glassRot = ticksAlive * 0.035f;
            for (int i = 0; i < interiorGlass.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6 + glassRot;
                Location loc = segCenter.clone().add(Math.cos(angle) * 0.8, Math.sin(angle) * 0.5, 0);
                interiorGlass.get(i).entity().teleport(loc);
            }

            // Violet trail particles behind segment
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.purpleDust(segCenter.clone().add(-0.5, 0, 0), 4, 0.5);
            }

            // Electric spark arcs between spine clusters
            if (ticksAlive % 15 == 0 && spineClusters.size() >= 2) {
                int idx = (ticksAlive / 15) % spineClusters.size();
                int next = (idx + 1) % spineClusters.size();
                Location from = spineClusters.get(idx).entity().getLocation();
                Location to = spineClusters.get(next).entity().getLocation();
                for (int p = 0; p < 4; p++) {
                    double t = p / 3.0;
                    Location particle = from.clone().add(
                            (to.getX() - from.getX()) * t,
                            (to.getY() - from.getY()) * t,
                            (to.getZ() - from.getZ()) * t
                    );
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                }
            }

            // End rod particles from ring surface
            if (ticksAlive % 8 == 0) {
                center.getWorld().spawnParticle(Particle.END_ROD,
                        segCenter, 2, 1.5, 0.5, 0.5, 0.01);
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomBodySegment(plugin); }
    }

    // ================================================================
    // 37. SEGMENTED ECHO CHAIN -- 8 connected body segments
    // ================================================================
    public static class SegmentedEchoChain extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> segments = new ArrayList<>();
        private float chainProgress = 0;

        public SegmentedEchoChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("segmented_echo_chain", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 segments along an S-curve path
            for (int seg = 0; seg < 8; seg++) {
                List<BlockDisplayHandle> segBlocks = new ArrayList<>();
                double t = seg / 7.0;
                double x = (t - 0.5) * 14; // S-curve spread
                double z = Math.sin(t * Math.PI * 2) * 3;
                Location segLoc = center.clone().add(x, 3, z);

                // Each segment: ring of blocks
                Material mat = (seg % 2 == 0) ? Material.AMETHYST_BLOCK : Material.CYAN_STAINED_GLASS;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 * i) / 6;
                    Location blockLoc = segLoc.clone().add(Math.cos(angle) * 0.6, Math.sin(angle) * 0.4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(blockLoc, mat);
                    h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                    segBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Dorsal spine every 4th segment
                if (seg % 4 == 0) {
                    BlockDisplayHandle spine = displayBuilder.spawnBlock(segLoc.clone().add(0, 0.6, 0),
                            Material.AMETHYST_CLUSTER);
                    spine.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                    segBlocks.add(spine);
                    spawnedEntities.add(spine.entity());
                }

                // Connector end rods between segments
                if (seg < 7) {
                    for (int rod = 0; rod < 2; rod++) {
                        double nextT = (seg + 1) / 7.0;
                        double midX = (x + (nextT - 0.5) * 14) / 2;
                        double midZ = (z + Math.sin(nextT * Math.PI * 2) * 3) / 2;
                        Location rodLoc = center.clone().add(midX, 3 + (rod - 0.5) * 0.3, midZ);
                        BlockDisplayHandle rodH = displayBuilder.spawnBlock(rodLoc, Material.END_ROD);
                        rodH.scale(0.1f, 0.1f, 0.5f).glow(240, 240, 255).interpolation(2, 0);
                        spawnedEntities.add(rodH.entity());
                    }
                }

                segments.add(segBlocks);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Chain moves forward slowly: 0.1 block per tick
            chainProgress += 0.1f;
            if (chainProgress > 15) chainProgress = -15;

            // Undulation: each segment oscillates perpendicular to chain direction
            for (int seg = 0; seg < segments.size(); seg++) {
                double t = seg / 7.0;
                double baseX = (t - 0.5) * 14 + chainProgress;
                double baseZ = Math.sin(t * Math.PI * 2) * 3;

                // Wave: 5 degree amplitude, 20-tick cycle from head to tail
                float wave = 0.5f * (float) Math.sin((ticksAlive - seg * 3) * (2 * Math.PI / 20));

                Location segCenter = center.clone().add(baseX, 3, baseZ + wave);

                // Update segment blocks
                List<BlockDisplayHandle> segBlocks = segments.get(seg);
                int ringCount = Math.min(segBlocks.size(), 6);
                for (int i = 0; i < ringCount; i++) {
                    double angle = (Math.PI * 2 * i) / 6;
                    Location loc = segCenter.clone().add(Math.cos(angle) * 0.6, Math.sin(angle) * 0.4, 0);
                    segBlocks.get(i).entity().teleport(loc);
                }
            }

            // Violet trail particles
            if (ticksAlive % 4 == 0) {
                for (int seg = 0; seg < segments.size(); seg++) {
                    if (!segments.get(seg).isEmpty()) {
                        DisplayBuilder.purpleDust(segments.get(seg).get(0).entity().getLocation(), 2, 0.3);
                    }
                }
            }

            // Electric spark connectors between adjacent segments
            if (ticksAlive % 6 == 0) {
                for (int seg = 0; seg < segments.size() - 1; seg++) {
                    if (!segments.get(seg).isEmpty() && !segments.get(seg + 1).isEmpty()) {
                        Location from = segments.get(seg).get(0).entity().getLocation();
                        Location to = segments.get(seg + 1).get(0).entity().getLocation();
                        Location mid = from.clone().add(
                                (to.getX() - from.getX()) * 0.5,
                                (to.getY() - from.getY()) * 0.5,
                                (to.getZ() - from.getZ()) * 0.5
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, mid, 2, 0.1, 0.1, 0.1, 0);
                    }
                }
            }

            // Portal particles from chain
            if (ticksAlive % 8 == 0) {
                int segIdx = (ticksAlive / 8) % segments.size();
                if (!segments.get(segIdx).isEmpty()) {
                    center.getWorld().spawnParticle(Particle.PORTAL,
                            segments.get(segIdx).get(0).entity().getLocation(), 2, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // Sound every 80 ticks
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SegmentedEchoChain(plugin); }
    }

    // ================================================================
    // 38. HEAD ECHO -- Scale model of DoG's head
    // ================================================================
    public static class HeadEcho extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fangClusters = new ArrayList<>();
        private final List<BlockDisplayHandle> crownRods = new ArrayList<>();
        private BlockDisplayHandle leftEye;
        private BlockDisplayHandle rightEye;
        private float pathProgress = 0;

        public HeadEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("head_echo", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Head body: sphere-like form from amethyst blocks in layered rings
            // Base layer (4)
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 4, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Widest (6)
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.3, 5, Math.sin(angle) * 1.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Upper mid (5)
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 6, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Crown (3)
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 7, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Eyes: sea lanterns as Eyes of Ender stand-ins
            leftEye = displayBuilder.spawnBlock(center.clone().add(-0.6, 5.5, 1.2), Material.SEA_LANTERN);
            leftEye.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(leftEye.entity());

            rightEye = displayBuilder.spawnBlock(center.clone().add(0.6, 5.5, 1.2), Material.SEA_LANTERN);
            rightEye.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(rightEye.entity());

            // 8 fang clusters below eyes, pointing forward-downward
            for (int i = 0; i < 8; i++) {
                double x = (i - 3.5) * 0.3;
                Location loc = center.clone().add(x, 4.5, 1.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.2f, 0.5f, 0.2f).glow(128, 0, 255).interpolation(2, 0);
                fangClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown arrangement: 6 end rods pointing upward
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 0.6, 7.5, Math.sin(angle) * 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.15f, 0.6f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                crownRods.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Head moves along circular path at 30% speed
            pathProgress += 0.003f;
            float headX = (float) Math.cos(pathProgress) * 8;
            float headZ = (float) Math.sin(pathProgress) * 8;
            float bob = 0.3f * (float) Math.sin(ticksAlive * 0.08f);

            Location headCenter = center.clone().add(headX, 5 + bob, headZ);

            // Bite animation every 80 ticks: fangs scale from 1.0 to 1.5 over 10 ticks
            boolean biting = (ticksAlive % 80) < 10;
            float fangScale = biting ?
                    1.0f + 0.5f * ((ticksAlive % 80) / 10.0f) : 1.0f;

            for (BlockDisplayHandle fang : fangClusters) {
                fang.scale(0.2f * fangScale, 0.5f * fangScale, 0.2f * fangScale);
                fang.interpolation(2, 0);
            }

            // Eye-to-fang laser beams
            if (ticksAlive % 5 == 0) {
                Location eyeMid = headCenter.clone().add(0, 0.5, 1.2);
                for (int i = 0; i < fangClusters.size(); i += 2) {
                    Location fangLoc = fangClusters.get(i).entity().getLocation();
                    for (int p = 0; p < 3; p++) {
                        double t = p / 2.0;
                        Location particle = eyeMid.clone().add(
                                (fangLoc.getX() - eyeMid.getX()) * t,
                                (fangLoc.getY() - eyeMid.getY()) * t,
                                (fangLoc.getZ() - eyeMid.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Bite portal burst
            if (biting && (ticksAlive % 80) == 5) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        headCenter.clone().add(0, 0, 1.5), 15, 1.5, 1.0, 1.5, 0.2);
            }

            // Cyan dust trailing from motion path
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.cyanDust(headCenter, 4, 1.0);
            }

            // Dragon breath from crown end rods
            if (ticksAlive % 6 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        headCenter.clone().add(0, 2.5, 0), 2, 0.3, 0.2, 0.3, 0.01);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HeadEcho(plugin); }
    }

    // ================================================================
    // 39. TAIL WHIP ECHO -- Tapering whip chain with snap mechanic
    // ================================================================
    public static class TailWhipEcho extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tailSegments = new ArrayList<>();
        private BlockDisplayHandle stingerTip;

        public TailWhipEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tail_whip_echo", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 segments tapering from 2.5 to 0.5 blocks diameter
            for (int i = 0; i < 10; i++) {
                float taper = 2.5f - (i * 0.2f);
                float scaleVal = taper * 0.3f;
                Material mat;
                if (i < 7) {
                    mat = Material.AMETHYST_BLOCK;
                } else {
                    mat = Material.AMETHYST_CLUSTER;
                }
                Location loc = center.clone().add(i * 1.2, 3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(scaleVal, scaleVal, scaleVal).glow(0, 200, 255).interpolation(2, 0);
                tailSegments.add(h);
                spawnedEntities.add(h.entity());

                // End rod connectors between segments
                if (i < 9) {
                    for (int rod = 0; rod < 2; rod++) {
                        Location rodLoc = loc.clone().add(0.6, (rod - 0.5) * 0.2, 0);
                        BlockDisplayHandle rodH = displayBuilder.spawnBlock(rodLoc, Material.END_ROD);
                        rodH.scale(0.1f, 0.1f, 0.4f).glow(240, 240, 255).interpolation(2, 0);
                        spawnedEntities.add(rodH.entity());
                    }
                }
            }

            // Stinger tip at end
            stingerTip = displayBuilder.spawnBlock(center.clone().add(12, 3, 0), Material.AMETHYST_CLUSTER);
            stingerTip.scale(0.3f, 0.6f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(stingerTip.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Whip physics: base moves on 100-tick circular path
            float baseAngle = ticksAlive * (2 * (float) Math.PI / 100);
            float baseX = (float) Math.cos(baseAngle) * 3;
            float baseZ = (float) Math.sin(baseAngle) * 3;

            // Each segment lags by 5 ticks
            for (int i = 0; i < tailSegments.size(); i++) {
                float segAngle = (ticksAlive - i * 5) * (2 * (float) Math.PI / 100);
                float segX = (float)(Math.cos(segAngle) * (3 + i * 0.5));
                float segZ = (float)(Math.sin(segAngle) * (3 + i * 0.5));

                Location segLoc = center.clone().add(segX, 3, segZ);
                tailSegments.get(i).entity().teleport(segLoc);
            }

            // Stinger follows last segment with additional lag
            float stingerAngle = (ticksAlive - 50) * (2 * (float) Math.PI / 100);
            float stingerX = (float) Math.cos(stingerAngle) * 8;
            float stingerZ = (float) Math.sin(stingerAngle) * 8;
            if (stingerTip != null) {
                stingerTip.entity().teleport(center.clone().add(stingerX, 3, stingerZ));
            }

            // Snap detection: when base reverses direction, tip accelerates
            boolean isSnapping = (ticksAlive % 100) > 45 && (ticksAlive % 100) < 55;

            if (isSnapping) {
                // Electric spark burst from tip at snap
                if (stingerTip != null && ticksAlive % 2 == 0) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            stingerTip.entity().getLocation(), 8, 0.5, 0.5, 0.5, 0.05);
                }
            }

            // Cyan dust trailing from tip during motion
            if (ticksAlive % 3 == 0 && stingerTip != null) {
                DisplayBuilder.cyanDust(stingerTip.entity().getLocation(), 3, 0.3);
            }

            // End rod particles from segments
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < tailSegments.size(); i += 3) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            tailSegments.get(i).entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Whip crack sound at snap moment
            if ((ticksAlive % 100) == 50) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TailWhipEcho(plugin); }
    }

    // ================================================================
    // 40. SPINE CROWN -- Floating crown of dorsal spines at Y+8
    // ================================================================
    public static class SpineCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> innerSpines = new ArrayList<>();
        private final List<BlockDisplayHandle> outerSpines = new ArrayList<>();
        private final List<BlockDisplayHandle> connectors = new ArrayList<>();

        public SpineCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spine_crown", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 6 inner spines at 2-block radius
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 2, 8, Math.sin(angle) * 2);
                // Each spine: 2 amethyst shards + large cluster at tip
                for (int seg = 0; seg < 3; seg++) {
                    Material mat = seg < 2 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_CLUSTER;
                    Location segLoc = loc.clone().add(Math.cos(angle) * seg * 0.3, seg * 0.5, Math.sin(angle) * seg * 0.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                    h.scale(0.3f, 0.5f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                    innerSpines.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 6 outer spines at 4-block radius
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + (Math.PI / 6);
                Location loc = center.clone().add(Math.cos(angle) * 4, 8, Math.sin(angle) * 4);
                for (int seg = 0; seg < 3; seg++) {
                    Material mat = seg < 2 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_CLUSTER;
                    Location segLoc = loc.clone().add(Math.cos(angle) * seg * 0.3, seg * 0.5, Math.sin(angle) * seg * 0.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                    h.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                    outerSpines.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 12 end rod connectors between inner and outer
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 3, 8, Math.sin(angle) * 3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.1f, 0.1f, 1.5f).glow(240, 240, 255).interpolation(2, 0);
                connectors.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center: sea lantern representing Elytra wing-plates
            BlockDisplayHandle elytra = displayBuilder.spawnBlock(center.clone().add(0, 8.5, 0), Material.SEA_LANTERN);
            elytra.scale(1.5f, 0.5f, 1.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(elytra.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Inner ring rotates at 1 deg/tick
            float innerRot = ticksAlive * 0.0175f;
            // Outer ring counter-rotates at 0.7 deg/tick
            float outerRot = -ticksAlive * 0.0122f;

            // Assembly height oscillation: descends 1 block over 200 ticks, ascends 2 blocks over 100
            int heightCycle = ticksAlive % 300;
            float yOff;
            if (heightCycle < 200) {
                yOff = -(heightCycle / 200.0f);
            } else {
                yOff = -1.0f + ((heightCycle - 200) / 100.0f) * 2.0f;
            }

            float baseY = 8 + yOff;

            // Update inner spine positions
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + innerRot;
                float radOsc = 0.2f * (float) Math.sin((ticksAlive + i * 8) * (2 * Math.PI / 45));
                for (int seg = 0; seg < 3; seg++) {
                    int idx = i * 3 + seg;
                    if (idx < innerSpines.size()) {
                        Location loc = center.clone().add(
                                Math.cos(angle) * (2 + seg * 0.3 + radOsc),
                                baseY + seg * 0.5,
                                Math.sin(angle) * (2 + seg * 0.3 + radOsc)
                        );
                        innerSpines.get(idx).entity().teleport(loc);
                    }
                }
            }

            // Update outer spine positions
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + (Math.PI / 6) + outerRot;
                float radOsc = 0.2f * (float) Math.sin((ticksAlive + i * 8) * (2 * Math.PI / 45));
                for (int seg = 0; seg < 3; seg++) {
                    int idx = i * 3 + seg;
                    if (idx < outerSpines.size()) {
                        Location loc = center.clone().add(
                                Math.cos(angle) * (4 + seg * 0.3 + radOsc),
                                baseY + seg * 0.5,
                                Math.sin(angle) * (4 + seg * 0.3 + radOsc)
                        );
                        outerSpines.get(idx).entity().teleport(loc);
                    }
                }
            }

            // Cross-ring laser arcs: outer tip to nearest inner tip
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    int outerTipIdx = i * 3 + 2;
                    int innerTipIdx = i * 3 + 2;
                    if (outerTipIdx < outerSpines.size() && innerTipIdx < innerSpines.size()) {
                        Location from = outerSpines.get(outerTipIdx).entity().getLocation();
                        Location to = innerSpines.get(innerTipIdx).entity().getLocation();
                        for (int p = 0; p < 4; p++) {
                            double t = p / 3.0;
                            Location particle = from.clone().add(
                                    (to.getX() - from.getX()) * t,
                                    (to.getY() - from.getY()) * t,
                                    (to.getZ() - from.getZ()) * t
                            );
                            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }

            // Purple dust from center
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, baseY + 0.5, 0), 4, 1.5);
            }

            // End rod particles from spine tips
            if (ticksAlive % 8 == 0) {
                for (int i = 2; i < outerSpines.size(); i += 3) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            outerSpines.get(i).entity().getLocation(), 1, 0.1, 0.2, 0.1, 0.02);
                }
            }

            // Portal particles ring outer spines
            if (ticksAlive % 10 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, baseY, 0), 3, 4.0, 0.2, 4.0, 0.05);
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpineCrown(plugin); }
    }

    // ================================================================
    // 41. DIMENSIONAL SCAR SERPENT -- Full serpent outline around arena
    // ================================================================
    public static class DimensionalScarSerpent extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bodySegments = new ArrayList<>();

        public DimensionalScarSerpent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_scar_serpent", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 30 body segments in 1.5 loops around arena perimeter at Y+6-10
            for (int i = 0; i < 30; i++) {
                double angle = (Math.PI * 3 * i) / 30; // 1.5 loops
                double radius = 12;
                double y = 6 + (i % 5) * 0.8;
                Location loc = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);

                // Ring of 4 amethyst blocks per segment (simplified from 8)
                for (int r = 0; r < 4; r++) {
                    double rAngle = (Math.PI * 2 * r) / 4;
                    Location ringLoc = loc.clone().add(Math.cos(rAngle) * 0.5, Math.sin(rAngle) * 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ringLoc, Material.AMETHYST_BLOCK);
                    h.scale(0.35f, 0.35f, 0.35f).glow(0, 200, 255).interpolation(2, 0);
                    bodySegments.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Cyan glass connector between segments
                if (i < 29) {
                    double nextAngle = (Math.PI * 3 * (i + 1)) / 30;
                    double midX = (Math.cos(angle) + Math.cos(nextAngle)) / 2 * radius;
                    double midZ = (Math.sin(angle) + Math.sin(nextAngle)) / 2 * radius;
                    BlockDisplayHandle conn = displayBuilder.spawnBlock(
                            center.clone().add(midX, y, midZ), Material.CYAN_STAINED_GLASS);
                    conn.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                    spawnedEntities.add(conn.entity());
                }

                // Dorsal spine every 3rd segment
                if (i % 3 == 0) {
                    BlockDisplayHandle spine = displayBuilder.spawnBlock(loc.clone().add(0, 1.0, 0),
                            Material.AMETHYST_CLUSTER);
                    spine.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                    spawnedEntities.add(spine.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Subtle scale pulse wave traveling from head to tail
            for (int i = 0; i < bodySegments.size(); i++) {
                int segIdx = i / 4;
                float pulse = 1.0f + 0.02f * (float) Math.sin((ticksAlive - segIdx * 2) * (2 * Math.PI / 60));
                bodySegments.get(i).scale(0.35f * pulse, 0.35f * pulse, 0.35f * pulse);
                bodySegments.get(i).interpolation(2, 0);
            }

            // Electric spark wave traveling along serpent body
            if (ticksAlive % 3 == 0) {
                int wavePos = (ticksAlive / 3) % 30;
                int blockIdx = wavePos * 4;
                if (blockIdx < bodySegments.size()) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            bodySegments.get(blockIdx).entity().getLocation(), 2, 0.2, 0.2, 0.2, 0);
                }
            }

            // Violet dust bleeding from body
            if (ticksAlive % 6 == 0) {
                int segIdx = (ticksAlive / 6) % 30;
                int blockIdx = segIdx * 4;
                if (blockIdx < bodySegments.size()) {
                    DisplayBuilder.purpleDust(bodySegments.get(blockIdx).entity().getLocation(), 2, 0.5);
                }
            }

            // Portal from head position (segment 0)
            if (ticksAlive % 8 == 0 && !bodySegments.isEmpty()) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        bodySegments.get(0).entity().getLocation(), 2, 0.3, 0.3, 0.3, 0.05);
            }

            // Dragon breath from head
            if (ticksAlive % 10 == 0 && !bodySegments.isEmpty()) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        bodySegments.get(0).entity().getLocation().add(0, 0, 1), 2, 0.3, 0.2, 0.3, 0.01);
            }

            // Sound every 200 ticks (distant worm-like)
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.2f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalScarSerpent(plugin); }
    }

    // ================================================================
    // 42. PHASE RIFT TRAIL -- Sequence of appearing/disappearing tears
    // ================================================================
    public static class PhaseRiftTrail extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tearFrames = new ArrayList<>();
        private final List<BlockDisplayHandle> tearGlass = new ArrayList<>();

        public PhaseRiftTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_rift_trail", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 15 rift tears along a curving path
            for (int i = 0; i < 15; i++) {
                double t = i / 14.0;
                double x = (t - 0.5) * 20 + Math.sin(t * Math.PI * 2) * 3;
                double z = Math.cos(t * Math.PI) * 5;
                double y = 1 + (i % 5);
                float tilt = (i % 3 - 1) * 0.15f;

                // Outer frame: dark blocks (2 tall x 1 wide)
                Location frameLoc = center.clone().add(x, y, z);
                BlockDisplayHandle frame = displayBuilder.spawnBlock(frameLoc, Material.POLISHED_BLACKSTONE);
                frame.scale(0.6f, 1.5f, 0.2f).glow(80, 80, 100).interpolation(2, 0);
                tearFrames.add(frame);
                spawnedEntities.add(frame.entity());

                // Center glass
                BlockDisplayHandle glass = displayBuilder.spawnBlock(frameLoc.clone().add(0, 0, 0.05),
                        Material.LIGHT_BLUE_STAINED_GLASS);
                glass.scale(0.3f, 1.0f, 0.1f).glow(240, 240, 255).interpolation(2, 0);
                tearGlass.add(glass);
                spawnedEntities.add(glass.entity());

                // End rod connectors to adjacent tears
                if (i < 14) {
                    double nextT = (i + 1) / 14.0;
                    double nextX = (nextT - 0.5) * 20 + Math.sin(nextT * Math.PI * 2) * 3;
                    double nextZ = Math.cos(nextT * Math.PI) * 5;
                    double midX = (x + nextX) / 2;
                    double midZ = (z + nextZ) / 2;
                    for (int rod = 0; rod < 2; rod++) {
                        Location rodLoc = center.clone().add(midX, y + (rod - 0.5) * 0.5, midZ);
                        BlockDisplayHandle rodH = displayBuilder.spawnBlock(rodLoc, Material.END_ROD);
                        rodH.scale(0.08f, 0.08f, 0.5f).glow(240, 240, 255).interpolation(2, 0);
                        spawnedEntities.add(rodH.entity());
                    }
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tears pulse visibility: each cycles over 30 ticks, offset 5 ticks
            for (int i = 0; i < tearFrames.size(); i++) {
                int phase = (ticksAlive + i * 5) % 30;
                // Scale from visible to invisible: use scale as proxy
                float visibility = (float) Math.sin(phase * (Math.PI / 30));
                float frameScale = 0.6f * Math.max(visibility, 0.05f);
                float glassScale = 0.3f * Math.max(visibility, 0.05f);

                tearFrames.get(i).scale(frameScale, 1.5f, 0.2f);
                tearFrames.get(i).interpolation(2, 0);

                if (i < tearGlass.size()) {
                    tearGlass.get(i).scale(glassScale, 1.0f, 0.1f);
                    tearGlass.get(i).interpolation(2, 0);
                }

                // Tilted tears rotate slightly
                float tilt = (i % 3 - 1) * 0.15f;
                float tiltOsc = tilt + 0.05f * (float) Math.sin(ticksAlive * 0.2f);
                BlockDisplay bd = tearFrames.get(i).entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-frameScale / 2, -0.75f, -0.1f),
                        new AxisAngle4f(tiltOsc, 0, 0, 1),
                        new Vector3f(frameScale, 1.5f, 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Portal particles from visible tears
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < tearGlass.size(); i++) {
                    int phase = (ticksAlive + i * 5) % 30;
                    if (phase < 20) { // Visible
                        center.getWorld().spawnParticle(Particle.PORTAL,
                                tearGlass.get(i).entity().getLocation(), 1, 0.1, 0.3, 0.1, 0.05);
                    }
                }
            }

            // Electric spark between adjacent visible tears
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < tearFrames.size() - 1; i++) {
                    int phase1 = (ticksAlive + i * 5) % 30;
                    int phase2 = (ticksAlive + (i + 1) * 5) % 30;
                    if (phase1 < 20 && phase2 < 20) {
                        Location from = tearFrames.get(i).entity().getLocation();
                        Location to = tearFrames.get(i + 1).entity().getLocation();
                        Location mid = from.clone().add(
                                (to.getX() - from.getX()) * 0.5,
                                (to.getY() - from.getY()) * 0.5,
                                (to.getZ() - from.getZ()) * 0.5
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, mid, 2, 0.1, 0.1, 0.1, 0);
                    }
                }
            }

            // White dust falling from tear tops
            if (ticksAlive % 8 == 0) {
                int idx = (ticksAlive / 8) % tearFrames.size();
                DisplayBuilder.cyanDust(tearFrames.get(idx).entity().getLocation().add(0, 0.8, 0), 2, 0.3);
            }

            // Sound triggers at each tear during appearance
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseRiftTrail(plugin); }
    }

    // ================================================================
    // 43. COSMIC WAKE -- Drifting space debris trail
    // ================================================================
    public static class CosmicWake extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> debrisPieces = new ArrayList<>();
        private final float[] debrisOffsets;
        private final float[] debrisScales;

        public CosmicWake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_wake", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
            debrisOffsets = new float[20];
            debrisScales = new float[20];
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 debris pieces scattered along a 20-block line
            for (int i = 0; i < 20; i++) {
                float baseX = i - 10;
                float yOff = 2 + (i % 5) * 0.8f;
                float zOff = ((i * 7) % 3 - 1) * 0.5f;
                float scale = 0.3f + (i % 5) * 0.1f;

                debrisOffsets[i] = baseX;
                debrisScales[i] = scale;

                Material mat = (i % 2 == 0) ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK;
                Location loc = center.clone().add(baseX, yOff, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(scale, scale, scale).glow(0, 200, 255).interpolation(2, 0);
                debrisPieces.add(h);
                spawnedEntities.add(h.entity());

                // End rod attached to each piece
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc.clone().add(0.2, 0, 0), Material.END_ROD);
                rod.scale(0.08f, 0.08f, 0.3f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // All pieces drift along line at 0.05 blocks per tick
            float drift = ticksAlive * 0.05f;

            for (int i = 0; i < debrisPieces.size(); i++) {
                float x = debrisOffsets[i] + drift;
                // Reset when reaching end
                if (x > 10) x -= 20;
                float yOff = 2 + (i % 5) * 0.8f;
                float zOff = ((i * 7) % 3 - 1) * 0.5f;

                Location loc = center.clone().add(x, yOff, zOff);
                debrisPieces.get(i).entity().teleport(loc);

                // Local tumble
                float tumbleX = ticksAlive * (0.002f + i * 0.0003f);
                float tumbleY = ticksAlive * (0.003f + i * 0.0002f);
                BlockDisplay bd = debrisPieces.get(i).entity();
                float s = debrisScales[i];
                bd.setTransformation(new Transformation(
                        new Vector3f(-s / 2, -s / 2, -s / 2),
                        new AxisAngle4f(tumbleX, 0.3f + i * 0.05f, 1, 0.2f),
                        new Vector3f(s, s, s),
                        new AxisAngle4f(tumbleY, 0, 0, 1)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Cyan dust trailing from each piece
            if (ticksAlive % 4 == 0) {
                int idx = (ticksAlive / 4) % debrisPieces.size();
                DisplayBuilder.cyanDust(debrisPieces.get(idx).entity().getLocation(), 2, 0.3);
            }

            // End rod particle V-shape from debris
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % debrisPieces.size();
                center.getWorld().spawnParticle(Particle.END_ROD,
                        debrisPieces.get(idx).entity().getLocation(), 2, 0.2, 0.1, 0.2, 0.02);
            }

            // Random electric spark arcs between nearby pieces
            if (ticksAlive % 20 == 0 && debrisPieces.size() >= 2) {
                int a = (ticksAlive / 20) % debrisPieces.size();
                int b = (a + 1) % debrisPieces.size();
                Location from = debrisPieces.get(a).entity().getLocation();
                Location to = debrisPieces.get(b).entity().getLocation();
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        from.clone().add((to.getX() - from.getX()) * 0.5, 0, (to.getZ() - from.getZ()) * 0.5),
                        2, 0.1, 0.1, 0.1, 0);
            }

            // Sound every 160 ticks
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicWake(plugin); }
    }

    // ================================================================
    // 44. PHANTOM COIL -- Helical amethyst coil from ground to Y+8
    // ================================================================
    public static class PhantomCoil extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> helixBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crownClusters = new ArrayList<>();

        public PhantomCoil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_coil", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 amethyst blocks in helix, 2.5 rotations from Y+0 to Y+8
            for (int i = 0; i < 20; i++) {
                double t = i / 19.0;
                double angle = t * Math.PI * 5; // 2.5 rotations
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                double y = t * 8;

                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                helixBlocks.add(h);
                spawnedEntities.add(h.entity());

                // Spine every 4th block
                if (i % 4 == 0) {
                    Location spineLoc = loc.clone().add(Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5);
                    BlockDisplayHandle spine = displayBuilder.spawnBlock(spineLoc, Material.AMETHYST_CLUSTER);
                    spine.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                    spineBlocks.add(spine);
                    spawnedEntities.add(spine.entity());
                }
            }

            // Crown: 5 large amethyst clusters at top
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * 0.8, 8.5, Math.sin(angle) * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.6f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                crownClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3x3 polished blackstone mount
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle mount = displayBuilder.spawnBlock(
                            center.clone().add(x, -0.3, z), Material.POLISHED_BLACKSTONE);
                    mount.scale(0.9f, 0.3f, 0.9f).glow(80, 80, 100).interpolation(2, 0);
                    spawnedEntities.add(mount.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Entire helix rotates at 2 deg/tick around vertical axis
            float rot = ticksAlive * 0.035f;

            for (int i = 0; i < helixBlocks.size(); i++) {
                double t = i / 19.0;
                double baseAngle = t * Math.PI * 5 + rot;
                double x = Math.cos(baseAngle) * 1.5;
                double z = Math.sin(baseAngle) * 1.5;
                double y = t * 8;

                Location loc = center.clone().add(x, y, z);
                helixBlocks.get(i).entity().teleport(loc);
            }

            // Spine clusters oscillate radially: 0.2 blocks
            for (int i = 0; i < spineBlocks.size(); i++) {
                float radOsc = 0.2f * (float) Math.sin(ticksAlive * 0.1f + i * 1.5f);
                int helixIdx = i * 4;
                if (helixIdx < helixBlocks.size()) {
                    Location helixLoc = helixBlocks.get(helixIdx).entity().getLocation();
                    double t = helixIdx / 19.0;
                    double baseAngle = t * Math.PI * 5 + rot;
                    Location spineLoc = helixLoc.clone().add(
                            Math.cos(baseAngle) * (0.5 + radOsc), 0, Math.sin(baseAngle) * (0.5 + radOsc));
                    spineBlocks.get(i).entity().teleport(spineLoc);
                }
            }

            // Crown clusters open outward: scale 1.0 to 1.3, 80-tick cycle
            float crownScale = 1.0f + 0.3f * (float) Math.sin(ticksAlive * (2 * Math.PI / 80));
            for (BlockDisplayHandle h : crownClusters) {
                h.scale(0.4f * crownScale, 0.6f * crownScale, 0.4f * crownScale);
                h.interpolation(2, 0);
            }

            // Electric spark pulse traveling up helix every 60 ticks
            if (ticksAlive % 3 == 0) {
                int pulseIdx = (ticksAlive / 3) % helixBlocks.size();
                Location pulseLoc = helixBlocks.get(pulseIdx).entity().getLocation();
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pulseLoc, 2, 0.1, 0.1, 0.1, 0);
            }

            // Cyan dust spiraling around helix exterior
            if (ticksAlive % 4 == 0) {
                double dustAngle = ticksAlive * 0.15;
                double dustY = (ticksAlive * 0.1) % 8;
                Location dustLoc = center.clone().add(
                        Math.cos(dustAngle) * 2.0, dustY, Math.sin(dustAngle) * 2.0);
                DisplayBuilder.cyanDust(dustLoc, 3, 0.3);
            }

            // End rod particles from crown when extended
            if (ticksAlive % 10 == 0 && crownScale > 1.2f) {
                for (BlockDisplayHandle h : crownClusters) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            h.entity().getLocation(), 1, 0.1, 0.2, 0.1, 0.02);
                }
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomCoil(plugin); }
    }

    // ================================================================
    // 46. SEGMENTED SPIRAL -- Flat Archimedean spiral at Y+3
    // ================================================================
    public static class SegmentedSpiral extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiralSegments = new ArrayList<>();

        public SegmentedSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("segmented_spiral", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 24 segments in Archimedean spiral at Y+3
            for (int i = 0; i < 24; i++) {
                double t = i / 23.0;
                double angle = t * Math.PI * 5; // 2.5 rotations
                double radius = 0.5 + t * 7.5; // 0.5 to 8 block radius
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                // Each segment: dark prismarine + amethyst side by side
                Location loc = center.clone().add(x, 3, z);
                BlockDisplayHandle dp = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                dp.scale(0.4f, 0.3f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                spiralSegments.add(dp);
                spawnedEntities.add(dp.entity());

                Location amLoc = loc.clone().add(0.4, 0, 0);
                BlockDisplayHandle am = displayBuilder.spawnBlock(amLoc, Material.AMETHYST_BLOCK);
                am.scale(0.4f, 0.3f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                spiralSegments.add(am);
                spawnedEntities.add(am.entity());

                // Spine every 6th segment
                if (i % 6 == 0) {
                    BlockDisplayHandle spine = displayBuilder.spawnBlock(loc.clone().add(0, 0.3, 0),
                            Material.AMETHYST_CLUSTER);
                    spine.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                    spawnedEntities.add(spine.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spiral rotates very slowly: 0.15 deg/tick
            float rot = ticksAlive * 0.00262f;

            // Update segment positions with rotation and individual bob
            for (int i = 0; i < 24; i++) {
                double t = i / 23.0;
                double baseAngle = t * Math.PI * 5 + rot;
                double radius = 0.5 + t * 7.5;
                double x = Math.cos(baseAngle) * radius;
                double z = Math.sin(baseAngle) * radius;
                float yBob = 0.1f * (float) Math.sin((ticksAlive + i * 4) * 0.08f);

                int dpIdx = i * 2;
                int amIdx = i * 2 + 1;
                if (dpIdx < spiralSegments.size()) {
                    spiralSegments.get(dpIdx).entity().teleport(center.clone().add(x, 3 + yBob, z));
                }
                if (amIdx < spiralSegments.size()) {
                    spiralSegments.get(amIdx).entity().teleport(center.clone().add(x + 0.4, 3 + yBob, z));
                }
            }

            // Electric spark wave from center outward every 100 ticks
            if (ticksAlive % 5 == 0) {
                int wavePos = (ticksAlive / 5) % 24;
                int blockIdx = wavePos * 2;
                if (blockIdx < spiralSegments.size()) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            spiralSegments.get(blockIdx).entity().getLocation(), 2, 0.1, 0.1, 0.1, 0);
                }
            }

            // Cyan dust drifting outward from segments
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % 24;
                int blockIdx = idx * 2;
                if (blockIdx < spiralSegments.size()) {
                    DisplayBuilder.cyanDust(spiralSegments.get(blockIdx).entity().getLocation(), 2, 0.5);
                }
            }

            // Portal particles from spiral center
            if (ticksAlive % 8 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 3.2, 0), 2, 0.3, 0.1, 0.3, 0.05);
            }

            // End rod particles from spines
            if (ticksAlive % 10 == 0) {
                for (int i = 0; i < 24; i += 6) {
                    int blockIdx = i * 2;
                    if (blockIdx < spiralSegments.size()) {
                        center.getWorld().spawnParticle(Particle.END_ROD,
                                spiralSegments.get(blockIdx).entity().getLocation().add(0, 0.5, 0),
                                1, 0.05, 0.1, 0.05, 0.01);
                    }
                }
            }

            // Sound every 80 ticks
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SegmentedSpiral(plugin); }
    }
}
