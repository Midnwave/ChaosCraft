package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.blockdisplay;

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
import java.util.Random;

/**
 * Phase 1 Block Display — GROUP 5: SERPENTINE AND ORGANIC SHAPES
 * 10 organic/serpentine structures for the Voidmaw boss fight.
 * Attack IDs #37-46.
 * Rules:
 *   - NO status effects (damage only)
 *   - Always spawn straight (yaw=0, pitch=0)
 *   - Calamity palette: purple (128,0,255), cyan (0,200,255), crimson (200,0,50)
 *   - displayBuilder.removeAll() in every onCleanup()
 */
public final class OrganicShapes {

    private OrganicShapes() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidSerpentSpine(plugin));
        registry.register(new TentacleSweep(plugin));
        registry.register(new BranchingMawRoot(plugin));
        registry.register(new TheLash(plugin));
        registry.register(new CoilingVoidWorm(plugin));
        registry.register(new HydraCluster(plugin));
        registry.register(new VoidJellyfish(plugin));
        registry.register(new TheWrithingMass(plugin));
        registry.register(new TendrilForest(plugin));
        registry.register(new AbyssCrawler(plugin));
    }

    // ================================================================
    // 37. VOID SERPENT SPINE — sinuous S-curve spine with ribs and eyes
    // ================================================================
    public static class VoidSerpentSpine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> vertebrae = new ArrayList<>();
        private final List<BlockDisplayHandle> ribs = new ArrayList<>();
        private final List<BlockDisplayHandle> cartilage = new ArrayList<>();
        private final List<BlockDisplayHandle> terminals = new ArrayList<>();
        private final List<BlockDisplayHandle> discs = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();

        public VoidSerpentSpine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_serpent_spine", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 32 obsidian vertebrae in S-curve: 16 blocks long, amplitude 1.5 on Z
            for (int i = 0; i < 32; i++) {
                double t = i / 31.0;
                double x = (t - 0.5) * 12.0; // spans -6 to +6
                double y = 1.5;
                double z = Math.sin(t * Math.PI * 2) * 1.5; // S-curve
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float nodeScale = (i % 4 == 0) ? 1.2f : 1.0f; // vertebral nodes
                h.scale(nodeScale, nodeScale, nodeScale).glow(80, 0, 160).interpolation(2, 0);
                vertebrae.add(h);
                spawnedEntities.add(h.entity());
            }

            // 10 blackstone ribs alternating sides projecting outward and upward
            for (int i = 0; i < 10; i++) {
                double t = (i + 1) / 11.0;
                double x = (t - 0.5) * 12.0;
                double spineZ = Math.sin(t * Math.PI * 2) * 1.5;
                double side = (i % 2 == 0) ? 1.0 : -1.0;
                // Rib projects 1 block outward, 2 blocks upward
                Location ribLoc = center.clone().add(x, 2.5, spineZ + side * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(ribLoc, Material.BLACKSTONE);
                h.scale(0.5f, 2.0f, 0.5f).glow(50, 0, 100).interpolation(2, 0);
                ribs.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 crying obsidian cartilage between rib pairs
            for (int i = 0; i < 6; i++) {
                double t = ((i * 1.5) + 1.5) / 11.0;
                double x = (t - 0.5) * 12.0;
                double z = Math.sin(t * Math.PI * 2) * 1.5;
                Location loc = center.clone().add(x, 1.8, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.7f, 0.7f, 0.7f).glow(100, 0, 200).interpolation(2, 0);
                cartilage.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 netherite terminals: head, tail, and 2 curve inflections
            double[] termX = {-6.0, 6.0, -3.0, 3.0};
            for (double tx : termX) {
                double t = (tx + 6.0) / 12.0;
                double tz = Math.sin(t * Math.PI * 2) * 1.5;
                Location loc = center.clone().add(tx, 1.5, tz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(1.1f, 1.1f, 1.1f).glow(40, 0, 80).interpolation(2, 0);
                terminals.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 polished blackstone discs at highest vertebral points
            int[] discIdx = {8, 16, 24};
            for (int idx : discIdx) {
                double t = idx / 31.0;
                double x = (t - 0.5) * 12.0;
                double z = Math.sin(t * Math.PI * 2) * 1.5;
                Location loc = center.clone().add(x, 3.0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.3f, 1.0f).glow(60, 60, 80).interpolation(2, 0);
                discs.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 amethyst eyes at head terminal
            Location headLoc = center.clone().add(-6.0, 1.8, Math.sin(0) * 1.5);
            BlockDisplayHandle eye1 = displayBuilder.spawnBlock(headLoc.clone().add(0, 0, 0.35), Material.AMETHYST_BLOCK);
            eye1.scale(0.5f, 0.5f, 0.5f).glow(180, 80, 255).interpolation(2, 0);
            eyes.add(eye1);
            spawnedEntities.add(eye1.entity());
            BlockDisplayHandle eye2 = displayBuilder.spawnBlock(headLoc.clone().add(0, 0, -0.35), Material.AMETHYST_BLOCK);
            eye2.scale(0.5f, 0.5f, 0.5f).glow(180, 80, 255).interpolation(2, 0);
            eyes.add(eye2);
            spawnedEntities.add(eye2.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.3f);
            DisplayBuilder.purpleDust(center, 20, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Undulate spine in sine wave propagating head-to-tail: each block 2 ticks behind predecessor
            for (int i = 0; i < vertebrae.size(); i++) {
                double t = i / 31.0;
                double x = (t - 0.5) * 12.0;
                double baseZ = Math.sin(t * Math.PI * 2) * 1.5;
                // Wave: amplitude 1.5, propagating at 0.5 blocks/tick (2 ticks offset per block)
                double wave = Math.sin((ticksAlive - i * 2) * 0.05) * 1.5;
                Location target = c.clone().add(x, 1.5 + wave, baseZ);
                vertebrae.get(i).entity().teleport(target);
            }

            // Ribs stay perpendicular to spine attachment — update position to follow vertebra
            for (int i = 0; i < ribs.size(); i++) {
                int spineIdx = (int) ((i + 1) / 11.0 * 31);
                if (spineIdx >= vertebrae.size()) spineIdx = vertebrae.size() - 1;
                Location spinePos = vertebrae.get(spineIdx).entity().getLocation();
                double side = (i % 2 == 0) ? 1.0 : -1.0;
                Location ribLoc = spinePos.clone().add(0, 1.5, side);
                ribs.get(i).entity().teleport(ribLoc);
            }

            // Eyes counter-rotate (Y-axis) to always face nearest player
            if (ticksAlive % 5 == 0) {
                float eyeRot = ticksAlive * 0.1f;
                for (BlockDisplayHandle eye : eyes) {
                    eye.rotate(-eyeRot, 0, 1, 0);
                    eye.interpolation(5, 0);
                }
                // Sculk_charge_pop from head every 3 seconds (60 ticks)
                if (ticksAlive % 60 == 0) {
                    Location headPos = eyes.isEmpty() ? c : eyes.get(0).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, headPos, 8, 0.3, 0.3, 0.3, 0);
                    DisplayBuilder.playSound(headPos, Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.8f);
                }
            }

            // Dark violet dust along spine
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % vertebrae.size();
                Location vLoc = vertebrae.get(idx).entity().getLocation();
                DisplayBuilder.purpleDust(vLoc, 3, 0.4);
            }

            // Falling obsidian tear between rib pairs
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle cart : cartilage) {
                    Location cLoc = cart.entity().getLocation();
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, cLoc, 2, 0.2, 0.3, 0.2, 0);
                }
            }

            // Warden sonic boom ambient sound every ~15s
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSerpentSpine(plugin); }
    }

    // ================================================================
    // 38. TENTACLE SWEEP — Massive sweeping tentacle with whip-crack tip
    // ================================================================
    public static class TentacleSweep extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> dorsalBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ventralBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> suckers = new ArrayList<>();
        private final List<BlockDisplayHandle> rootPad = new ArrayList<>();
        private BlockDisplayHandle sensingTip;

        // Sweep state
        private float sweepAngle = -90.0f; // degrees, starts left
        private float sweepDir = 1.0f; // +1 = sweeping right, -1 = left
        private int dirChangeCooldown = 0;

        public TentacleSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tentacle_sweep", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(900);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 22 blackstone shaft blocks — 14 long, curving in 3D
            for (int i = 0; i < 14; i++) {
                double t = i / 13.0;
                // Curve: rises 30 degrees, sweeps 60 lateral, base to tip
                double x = Math.sin(sweepAngle * Math.PI / 180.0) * i * 0.8;
                double y = 1.0 + Math.sin(t * Math.PI / 6) * i * 0.35; // rises ~30 deg
                double z = Math.cos(sweepAngle * Math.PI / 180.0) * i * 0.8;
                float taper = 1.0f - (i * 0.05f);
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(taper, taper, taper).glow(50, 0, 100).interpolation(2, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 polished blackstone dorsal skin — upper surface every other block
            for (int i = 1; i < 14; i += 2) {
                BlockDisplay shaft = shaftBlocks.get(i).entity();
                Location loc = shaft.getLocation().clone().add(0, 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.9f, 0.3f, 0.9f).glow(60, 60, 80).interpolation(2, 0);
                dorsalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 obsidian ventral reinforcement — thickest section (first 6 blocks)
            for (int i = 0; i < 6; i++) {
                BlockDisplay shaft = shaftBlocks.get(i).entity();
                Location loc = shaft.getLocation().clone().add(0, -0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.1f, 0.3f, 1.1f).glow(40, 0, 80).interpolation(2, 0);
                ventralBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 crying obsidian suckers on underside
            int[] suckerIdx = {2, 4, 7, 10};
            for (int idx : suckerIdx) {
                Location loc = shaftBlocks.get(idx).entity().getLocation().clone().add(0, -0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(100, 0, 200).interpolation(2, 0);
                suckers.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 netherite base pad root mass
            double[][] rootPos = {{0,0,0},{0.5,0,0.3},{-0.4,0,-0.2}};
            for (double[] rp : rootPos) {
                Location loc = center.clone().add(rp[0], 0.2, rp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(1.0f, 0.6f, 1.0f).glow(30, 0, 60).interpolation(2, 0);
                rootPad.add(h);
                spawnedEntities.add(h.entity());
            }

            // 1 amethyst sensing node at tip
            Location tipLoc = shaftBlocks.get(13).entity().getLocation().clone().add(0, 0.3, 0);
            sensingTip = displayBuilder.spawnBlock(tipLoc, Material.AMETHYST_BLOCK);
            sensingTip.scale(0.6f, 0.6f, 0.6f).glow(180, 80, 255).interpolation(2, 0);
            spawnedEntities.add(sensingTip.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sweep at 20 degrees/second = 1 degree/tick
            sweepAngle += sweepDir * 1.0f;
            dirChangeCooldown--;

            // Reverse at ±90 degrees
            if ((sweepAngle >= 90.0f || sweepAngle <= -90.0f) && dirChangeCooldown <= 0) {
                sweepDir = -sweepDir;
                dirChangeCooldown = 10;
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.4f);
                // Large smoke burst at tip on direction change
                Location tipPos = sensingTip != null ? sensingTip.entity().getLocation() : c;
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, tipPos, 20, 0.5, 0.5, 0.5, 0.05);
            }

            float rad = sweepAngle * (float) Math.PI / 180.0f;

            // Update shaft block positions
            for (int i = 0; i < shaftBlocks.size(); i++) {
                double t = i / 13.0;
                double x = Math.sin(rad) * i * 0.8;
                double y = 1.0 + Math.sin(t * Math.PI / 6) * i * 0.35;
                double z = Math.cos(rad) * i * 0.8;
                shaftBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            // Tip whip-crack oscillation ±15 degrees on sub-axis (last 4 blocks)
            float tipOsc = (float) Math.sin(ticksAlive * 0.3) * 15.0f;
            float tipRad = tipOsc * (float) Math.PI / 180.0f;
            for (int i = 10; i < 14; i++) {
                BlockDisplay bd = shaftBlocks.get(i).entity();
                Location base = bd.getLocation();
                double extraX = Math.sin(rad + tipRad) * (i - 10) * 0.1;
                double extraZ = Math.cos(rad + tipRad) * (i - 10) * 0.1;
                bd.teleport(base.clone().add(extraX, 0, extraZ));
            }

            // Update dorsal, ventral, sucker positions to follow shaft
            for (int i = 0; i < dorsalBlocks.size(); i++) {
                int si = i * 2 + 1;
                if (si < shaftBlocks.size()) {
                    Location base = shaftBlocks.get(si).entity().getLocation();
                    dorsalBlocks.get(i).entity().teleport(base.clone().add(0, 0.5, 0));
                }
            }
            for (int i = 0; i < ventralBlocks.size(); i++) {
                if (i < shaftBlocks.size()) {
                    Location base = shaftBlocks.get(i).entity().getLocation();
                    ventralBlocks.get(i).entity().teleport(base.clone().add(0, -0.4, 0));
                }
            }
            int[] suckerIdx = {2, 4, 7, 10};
            for (int i = 0; i < suckers.size(); i++) {
                if (i < suckerIdx.length && suckerIdx[i] < shaftBlocks.size()) {
                    Location base = shaftBlocks.get(suckerIdx[i]).entity().getLocation();
                    suckers.get(i).entity().teleport(base.clone().add(0, -0.7, 0));
                }
            }

            // Sensing tip follows last shaft block
            if (sensingTip != null && !shaftBlocks.isEmpty()) {
                Location tipBase = shaftBlocks.get(shaftBlocks.size() - 1).entity().getLocation();
                sensingTip.entity().teleport(tipBase.clone().add(0, 0.3, 0));
            }

            // Particles: large_smoke trailing tip
            if (ticksAlive % 3 == 0 && sensingTip != null) {
                Location tipPos = sensingTip.entity().getLocation();
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, tipPos, 4, 0.2, 0.2, 0.2, 0.01);
                DisplayBuilder.purpleDust(tipPos, 2, 0.3);
            }

            // Falling obsidian tear from suckers
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle sk : suckers) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            sk.entity().getLocation(), 2, 0.1, 0.2, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TentacleSweep(plugin); }
    }

    // ================================================================
    // 39. BRANCHING MAW ROOT — Ground root-tree that grows progressively
    // ================================================================
    public static class BranchingMawRoot extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> trunk = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> branches = new ArrayList<>();
        private final List<BlockDisplayHandle> reinforcing = new ArrayList<>();
        private final List<BlockDisplayHandle> bark = new ArrayList<>();
        private final List<BlockDisplayHandle> tipWeep = new ArrayList<>();
        private final List<BlockDisplayHandle> rootBall = new ArrayList<>();
        private final List<BlockDisplayHandle> soulGaps = new ArrayList<>();
        private final List<BlockDisplayHandle> amethystVeins = new ArrayList<>();

        // For peristaltic wave
        private float[] segmentYOffset;
        private int segmentCount = 0;

        public BranchingMawRoot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("branching_maw_root", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(3.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Root ball (4 netherite) at origin
            double[][] rbPos = {{0,0,0},{0.4,0,0.3},{-0.3,0,0.4},{0.2,0,-0.4}};
            for (double[] rp : rbPos) {
                Location loc = center.clone().add(rp[0], 0.1, rp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(1.0f, 0.8f, 1.0f).glow(30, 0, 60).interpolation(2, 0);
                rootBall.add(h);
                spawnedEntities.add(h.entity());
            }

            // Soul soil gaps (3) at origin between branches
            double[][] ssPos = {{0.6,0,-0.6},{-0.6,0,0.5},{0,0,0.7}};
            for (double[] sp : ssPos) {
                Location loc = center.clone().add(sp[0], 0.05, sp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(0.7f, 0.3f, 0.7f).glow(60, 40, 20).interpolation(2, 0);
                soulGaps.add(h);
                spawnedEntities.add(h.entity());
            }

            // Trunk: 6 blackstone blocks along +X axis (will grow)
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(i * 1.0, 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.9f, 0.9f, 0.9f).glow(50, 0, 100).interpolation(2, 0);
                // Visual fire ticks not available on BlockDisplay
                trunk.add(h);
                spawnedEntities.add(h.entity());
            }

            // Amethyst vein nodes (2) in trunk at blocks 1 and 3
            int[] veinIdx = {1, 3};
            for (int idx : veinIdx) {
                Location loc = trunk.get(idx).entity().getLocation().clone().add(0, 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 80, 255).interpolation(2, 0);
                amethystVeins.add(h);
                spawnedEntities.add(h.entity());
            }

            // Reinforcing obsidian on trunk/main branch joints (10 blocks)
            for (int i = 0; i < 6; i += 2) {
                Location loc = trunk.get(i).entity().getLocation().clone().add(0, -0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.1f, 0.4f, 1.1f).glow(40, 0, 80).interpolation(2, 0);
                reinforcing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Main branches: 2 from trunk position 4, at ±40 degrees
            // Each branch: 4 blocks
            List<BlockDisplayHandle> branchA = new ArrayList<>();
            List<BlockDisplayHandle> branchB = new ArrayList<>();
            Location branchOrigin = trunk.get(3).entity().getLocation();
            for (int i = 0; i < 4; i++) {
                double angleA = 40.0 * Math.PI / 180.0;
                double angleB = -40.0 * Math.PI / 180.0;
                Location locA = branchOrigin.clone().add(
                        (i + 1) * Math.cos(angleA), 0.3, (i + 1) * Math.sin(angleA));
                Location locB = branchOrigin.clone().add(
                        (i + 1) * Math.cos(angleB), 0.3, (i + 1) * Math.sin(angleB));
                BlockDisplayHandle ha = displayBuilder.spawnBlock(locA, Material.BLACKSTONE);
                ha.scale(0.8f, 0.8f, 0.8f).glow(50, 0, 100).interpolation(2, 0);
                branchA.add(ha);
                spawnedEntities.add(ha.entity());

                BlockDisplayHandle hb = displayBuilder.spawnBlock(locB, Material.BLACKSTONE);
                hb.scale(0.8f, 0.8f, 0.8f).glow(50, 0, 100).interpolation(2, 0);
                branchB.add(hb);
                spawnedEntities.add(hb.entity());
            }
            branches.add(branchA);
            branches.add(branchB);

            // Sub-branches from each main branch at block 3: 2 each at ±30 deg
            List<List<BlockDisplayHandle>> subBranchGroups = new ArrayList<>();
            double[][] mainAngles = {{40.0, Math.PI/180 * 40}, {-40.0, Math.PI/180 * -40}};
            for (int m = 0; m < 2; m++) {
                double mAngle = mainAngles[m][1];
                Location mainBranchPos = branches.get(m).get(2).entity().getLocation();
                for (int s = -1; s <= 1; s += 2) {
                    double subAngle = mAngle + (s * 30.0 * Math.PI / 180.0);
                    List<BlockDisplayHandle> sub = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        Location loc = mainBranchPos.clone().add(
                                (i + 1) * Math.cos(subAngle), 0.2, (i + 1) * Math.sin(subAngle));
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                        h.scale(0.65f, 0.65f, 0.65f).glow(50, 0, 100).interpolation(2, 0);
                        sub.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    branches.add(sub);
                    subBranchGroups.add(sub);
                }
            }

            // Bark cobbled deepslate on outer branch surfaces (8 blocks)
            for (int i = 0; i < Math.min(8, branchA.size() + branchB.size()); i++) {
                List<BlockDisplayHandle> src = (i < branchA.size()) ? branchA : branchB;
                int si = (i < branchA.size()) ? i : i - branchA.size();
                if (si < src.size()) {
                    Location loc = src.get(si).entity().getLocation().clone().add(0, 0.55, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.COBBLED_DEEPSLATE);
                    h.scale(0.7f, 0.25f, 0.7f).glow(40, 40, 50).interpolation(2, 0);
                    bark.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 6 crying obsidian: 4 sub-branch tips + 2 at first split
            // (placed at tips of sub-branches)
            for (List<BlockDisplayHandle> sub : subBranchGroups) {
                if (!sub.isEmpty()) {
                    BlockDisplayHandle tip = sub.get(sub.size() - 1);
                    Location tLoc = tip.entity().getLocation().clone().add(0, 0.3, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(tLoc, Material.CRYING_OBSIDIAN);
                    h.scale(0.6f, 0.6f, 0.6f).glow(100, 0, 200).interpolation(2, 0);
                    tipWeep.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // 2 more at first split
            Location splitLoc = trunk.get(3).entity().getLocation().clone().add(0, 0.6, 0);
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        splitLoc.clone().add((i == 0 ? 0.4 : -0.4), 0, 0), Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(100, 0, 200).interpolation(2, 0);
                tipWeep.add(h);
                spawnedEntities.add(h.entity());
            }

            // Initialize segment offsets for peristaltic wave
            segmentCount = trunk.size() + branchA.size() + branchB.size();
            segmentYOffset = new float[segmentCount];

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Growth phase: trunk 0-10, mainBranches 10-20, subBranches 20-30, tips 30-40
            // After growth, peristaltic wave propagates
            if (ticksAlive >= 40) {
                // Peristaltic wave: 0.2 blocks up and back in 20-tick cycle, 1 block/3 ticks from origin
                for (int i = 0; i < trunk.size(); i++) {
                    float wavePhase = (ticksAlive - i * 3) * (float) (Math.PI * 2 / 20.0);
                    float yOff = (float) Math.sin(wavePhase) * 0.2f;
                    Location base = c.clone().add(i * 1.0, 0.5 + yOff, 0);
                    trunk.get(i).entity().teleport(base);
                    // Follow with amethyst veins
                    if (i == 1 && amethystVeins.size() > 0)
                        amethystVeins.get(0).entity().teleport(base.clone().add(0, 0.6, 0));
                    if (i == 3 && amethystVeins.size() > 1)
                        amethystVeins.get(1).entity().teleport(base.clone().add(0, 0.6, 0));
                }

                // Sub-branch tips undulate ±10 degrees random axes
                if (ticksAlive % 5 == 0 && !branches.isEmpty()) {
                    for (int bi = 2; bi < branches.size(); bi++) {
                        List<BlockDisplayHandle> sub = branches.get(bi);
                        if (sub.isEmpty()) continue;
                        BlockDisplayHandle tip = sub.get(sub.size() - 1);
                        float rot = (float) Math.sin(ticksAlive * 0.07 + bi) * 0.18f;
                        tip.rotate(rot, 1, 0.5f, 0);
                        tip.interpolation(5, 0);
                    }
                }
            }

            // Growth sound per wave-front
            if (ticksAlive == 0) DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.5f);
            if (ticksAlive == 10) DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.6f);
            if (ticksAlive == 20) DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.7f);
            if (ticksAlive == 30) DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.8f);

            // Block puff per segment during growth
            if (ticksAlive < 40 && ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK,
                        c.clone().add(ticksAlive * 0.15, 0.5, 0),
                        15, 0.3, 0.3, 0.3, 0, Material.BLACKSTONE.createBlockData());
            }

            // Sculk soul from amethyst veins
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle av : amethystVeins) {
                    c.getWorld().spawnParticle(Particle.SCULK_SOUL, av.entity().getLocation(), 3, 0.2, 0.3, 0.2, 0.02);
                }
            }

            // Falling obsidian tear from 4 tips
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < Math.min(4, tipWeep.size()); i++) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            tipWeep.get(i).entity().getLocation(), 3, 0.15, 0.2, 0.15, 0);
                }
            }

            // Soul fire flame from soul soil at origin
            if (ticksAlive % 6 == 0) {
                for (BlockDisplayHandle sg : soulGaps) {
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, sg.entity().getLocation().add(0, 0.3, 0),
                            3, 0.2, 0.2, 0.2, 0.01);
                }
            }

            // Heartbeat sound every ~8 seconds
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BranchingMawRoot(plugin); }
    }

    // ================================================================
    // 40. THE LASH — Chain that crack-the-whips from base to tip
    // ================================================================
    public static class TheLash extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> weepNodes = new ArrayList<>();

        // Snap state
        private int nextSnapTick = 0;
        private boolean snapping = false;
        private int snapProgress = 0;

        public TheLash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_lash", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(2.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 18 obsidian links: 14-block chain in two 3D S-curves
            // Base 1 block above ground, tip 3 blocks height
            for (int i = 0; i < 18; i++) {
                double t = i / 17.0;
                double x = (t - 0.5) * 8.0; // spans -4 to +4
                // Two S-curves stacked
                double z = Math.sin(t * Math.PI * 2) * 0.8;
                double y = 1.0 + t * 2.0 + Math.sin(t * Math.PI) * 0.5; // base 1, tip ~3
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(80, 0, 160).interpolation(2, 0);
                chainLinks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 crying obsidian weeping nodes at positions 3, 6, 10, 13
            int[] weepPos = {3, 6, 10, 13};
            for (int wp : weepPos) {
                if (wp < chainLinks.size()) {
                    Location loc = chainLinks.get(wp).entity().getLocation();
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.6f, 0.6f, 0.6f).glow(100, 0, 200).interpolation(2, 0);
                    weepNodes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            nextSnapTick = 20; // first snap after short settle
            DisplayBuilder.purpleDust(center, 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Check for snap trigger
            if (ticksAlive >= nextSnapTick && !snapping) {
                snapping = true;
                snapProgress = 0;
            }

            if (snapping) {
                snapProgress++;
                // Snap wave: 8 ticks from base to tip — each block offset ±2.5 perpendicular then snap back
                for (int i = 0; i < chainLinks.size(); i++) {
                    // Wave front reaches block i at tick = i * 8 / chainLinks.size()
                    float frontTick = (float) i * 8.0f / chainLinks.size();
                    float localT = snapProgress - frontTick;
                    double t = i / 17.0;
                    double baseX = (t - 0.5) * 8.0;
                    double baseZ = Math.sin(t * Math.PI * 2) * 0.8;
                    double baseY = 1.0 + t * 2.0 + Math.sin(t * Math.PI) * 0.5;

                    double perpOff = 0;
                    if (localT >= 0 && localT < 4) {
                        perpOff = Math.sin((localT / 4.0) * Math.PI) * 2.5;
                    }
                    // Alternate perpendicular direction for adjacent blocks
                    perpOff *= (i % 2 == 0 ? 1 : -1);

                    chainLinks.get(i).entity().teleport(c.clone().add(baseX, baseY + perpOff * 0.3, baseZ + perpOff));
                }

                // Snap finishes after 8 ticks
                if (snapProgress >= 8) {
                    snapping = false;
                    nextSnapTick = ticksAlive + 30;
                    // Boom at tip on snap complete
                    if (!chainLinks.isEmpty()) {
                        Location tipPos = chainLinks.get(chainLinks.size() - 1).entity().getLocation();
                        DisplayBuilder.playSound(tipPos, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.6f);
                        c.getWorld().spawnParticle(Particle.LARGE_SMOKE, tipPos, 15, 0.4, 0.4, 0.4, 0.05);
                        c.getWorld().spawnParticle(Particle.WHITE_ASH, tipPos, 20, 0.5, 0.5, 0.5, 0.05);
                    }
                }
            } else {
                // Ambient undulation ±0.3 blocks between snaps
                for (int i = 0; i < chainLinks.size(); i++) {
                    double t = i / 17.0;
                    double baseX = (t - 0.5) * 8.0;
                    double baseZ = Math.sin(t * Math.PI * 2) * 0.8;
                    double baseY = 1.0 + t * 2.0 + Math.sin(t * Math.PI) * 0.5;
                    double ambient = Math.sin((ticksAlive * 0.04) + i * 0.3) * 0.3;
                    chainLinks.get(i).entity().teleport(c.clone().add(baseX, baseY + ambient, baseZ));
                }
            }

            // Falling obsidian tear from weep nodes
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle wn : weepNodes) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            wn.entity().getLocation(), 3, 0.15, 0.25, 0.15, 0);
                }
            }

            // White dust during snap wave front
            if (snapping && snapProgress % 2 == 0) {
                int frontIdx = Math.min((int)(snapProgress / 8.0f * chainLinks.size()), chainLinks.size() - 1);
                Location frontPos = chainLinks.get(frontIdx).entity().getLocation();
                c.getWorld().spawnParticle(Particle.WHITE_ASH, frontPos, 5, 0.2, 0.2, 0.2, 0.02);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLash(plugin); }
    }

    // ================================================================
    // 41. COILING VOID WORM — Triple-coil worm with striking head
    // ================================================================
    public static class CoilingVoidWorm extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coilBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> striping = new ArrayList<>();
        private final List<BlockDisplayHandle> joints = new ArrayList<>();
        private final List<BlockDisplayHandle> glands = new ArrayList<>();
        private final List<BlockDisplayHandle> headFeatures = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private BlockDisplayHandle mouthFrame1;
        private BlockDisplayHandle mouthFrame2;

        // Animation state
        private float coilRotation = 0f;
        private float headRotation = 0f;
        private int strikePhase = 0; // 0=idle, 1=rear, 2=strike, 3=reset
        private int strikeTimer = 0;
        private int strikeCountdown = 400; // first strike after 400 ticks (~20s)

        public CoilingVoidWorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("coiling_void_worm", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(3.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer coil: 14 blocks, diameter 8, horizontal ring
            for (int i = 0; i < 14; i++) {
                double angle = (Math.PI * 2 * i) / 14;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                Location loc = center.clone().add(x, 0.5, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(80, 0, 160).interpolation(2, 0);
                coilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Middle coil: 10 blocks, diameter 5
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double x = Math.cos(angle) * 2.5;
                double z = Math.sin(angle) * 2.5;
                Location loc = center.clone().add(x, 0.6, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(80, 0, 160).interpolation(2, 0);
                coilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Inner coil: 6 blocks, diameter 3
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                Location loc = center.clone().add(x, 0.7, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(80, 0, 160).interpolation(2, 0);
                coilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 polished blackstone skin striping — every 3rd block upper surface
            for (int i = 0; i < coilBlocks.size(); i += 3) {
                Location loc = coilBlocks.get(i).entity().getLocation().clone().add(0, 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.2f, 0.8f).glow(60, 60, 80).interpolation(2, 0);
                striping.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 netherite joints: loop-end joints and head-to-coil junction
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.0, 0.5, Math.sin(angle) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.7f, 0.7f, 0.7f).glow(30, 0, 60).interpolation(2, 0);
                joints.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 crying obsidian glands on inner coil
            for (int i = 0; i < 6; i++) {
                if (i < coilBlocks.size()) {
                    Location loc = coilBlocks.get(24 + i).entity().getLocation().clone().add(0, -0.4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.5f, 0.5f, 0.5f).glow(100, 0, 200).interpolation(2, 0);
                    glands.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Head: rising 3 blocks above inner center
            // 2 amethyst eyes
            BlockDisplayHandle eyeL = displayBuilder.spawnBlock(center.clone().add(-0.35, 3.2, 0), Material.AMETHYST_BLOCK);
            eyeL.scale(0.5f, 0.5f, 0.5f).glow(180, 80, 255).interpolation(2, 0);
            eyes.add(eyeL);
            headFeatures.add(eyeL);
            spawnedEntities.add(eyeL.entity());

            BlockDisplayHandle eyeR = displayBuilder.spawnBlock(center.clone().add(0.35, 3.2, 0), Material.AMETHYST_BLOCK);
            eyeR.scale(0.5f, 0.5f, 0.5f).glow(180, 80, 255).interpolation(2, 0);
            eyes.add(eyeR);
            headFeatures.add(eyeR);
            spawnedEntities.add(eyeR.entity());

            // 1 crown node amethyst
            BlockDisplayHandle crown = displayBuilder.spawnBlock(center.clone().add(0, 3.6, 0), Material.AMETHYST_BLOCK);
            crown.scale(0.6f, 0.6f, 0.6f).glow(200, 100, 255).interpolation(2, 0);
            headFeatures.add(crown);
            spawnedEntities.add(crown.entity());

            // 1 open mouth amethyst
            BlockDisplayHandle mouth = displayBuilder.spawnBlock(center.clone().add(0, 2.7, 0.5), Material.AMETHYST_BLOCK);
            mouth.scale(0.7f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
            headFeatures.add(mouth);
            spawnedEntities.add(mouth.entity());

            // 2 dark prismarine framing mouth
            mouthFrame1 = displayBuilder.spawnBlock(center.clone().add(-0.4, 2.7, 0.5), Material.DARK_PRISMARINE);
            mouthFrame1.scale(0.3f, 0.5f, 0.4f).glow(0, 150, 180).interpolation(2, 0);
            spawnedEntities.add(mouthFrame1.entity());

            mouthFrame2 = displayBuilder.spawnBlock(center.clone().add(0.4, 2.7, 0.5), Material.DARK_PRISMARINE);
            mouthFrame2.scale(0.3f, 0.5f, 0.4f).glow(0, 150, 180).interpolation(2, 0);
            spawnedEntities.add(mouthFrame2.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Coil rotates on Y-axis at 6 degrees/second = 0.3 deg/tick
            coilRotation += 0.3f;
            float coilRad = coilRotation * (float) Math.PI / 180.0f;

            // Outer coil (14 blocks)
            for (int i = 0; i < 14; i++) {
                double baseAngle = (Math.PI * 2 * i) / 14 + coilRad;
                Location loc = c.clone().add(Math.cos(baseAngle) * 4.0, 0.5, Math.sin(baseAngle) * 4.0);
                coilBlocks.get(i).entity().teleport(loc);
            }
            // Middle coil (10 blocks)
            for (int i = 0; i < 10; i++) {
                double baseAngle = (Math.PI * 2 * i) / 10 + coilRad;
                Location loc = c.clone().add(Math.cos(baseAngle) * 2.5, 0.6, Math.sin(baseAngle) * 2.5);
                coilBlocks.get(14 + i).entity().teleport(loc);
            }
            // Inner coil (6 blocks) — tighten during strike
            float innerRadius = 1.5f;
            if (strikePhase == 2) innerRadius = 1.0f; // contract during strike
            for (int i = 0; i < 6; i++) {
                double baseAngle = (Math.PI * 2 * i) / 6 + coilRad;
                Location loc = c.clone().add(Math.cos(baseAngle) * innerRadius, 0.7, Math.sin(baseAngle) * innerRadius);
                coilBlocks.get(24 + i).entity().teleport(loc);
            }

            // Head counter-rotates — always face nearest player
            headRotation = -coilRotation;
            float headRad = headRotation * (float) Math.PI / 180.0f;

            // Strike cycle
            strikeCountdown--;
            if (strikeCountdown <= 0 && strikePhase == 0) {
                strikePhase = 1; // begin rear back
                strikeTimer = 0;
            }

            float headY = 3.0f;
            float headZ = 0f;

            if (strikePhase == 1) { // rear back: head rises +2 blocks over 15 ticks
                strikeTimer++;
                headY = 3.0f + (strikeTimer / 15.0f) * 2.0f;
                if (strikeTimer >= 15) { strikePhase = 2; strikeTimer = 0; }
            } else if (strikePhase == 2) { // strike: 3 blocks forward in 4 ticks
                strikeTimer++;
                headY = 5.0f - (strikeTimer / 4.0f) * 1.0f;
                headZ = (strikeTimer / 4.0f) * 3.0f;
                if (strikeTimer >= 4) {
                    strikePhase = 3;
                    strikeTimer = 0;
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 4, 3), 20, 0.5, 0.5, 0.5, 0.05);
                }
            } else if (strikePhase == 3) { // reset over 10 ticks
                strikeTimer++;
                float resetT = strikeTimer / 10.0f;
                headY = 4.0f - resetT * 1.0f;
                headZ = (1 - resetT) * 3.0f;
                if (strikeTimer >= 10) {
                    strikePhase = 0;
                    strikeCountdown = 400;
                }
            }

            // Update head feature positions
            Location headCenter = c.clone().add(
                    Math.sin(headRad) * 0.5, headY, Math.cos(headRad) * 0.5 + headZ);
            for (int i = 0; i < headFeatures.size(); i++) {
                double hx = (i == 0 ? -0.35 : (i == 1 ? 0.35 : 0));
                double hy = (i == 2 ? 0.3 : (i == 3 ? -0.3 : 0));
                double hz = (i == 3 ? 0.5 : 0);
                headFeatures.get(i).entity().teleport(headCenter.clone().add(hx, hy, hz));
            }
            if (mouthFrame1 != null)
                mouthFrame1.entity().teleport(headCenter.clone().add(-0.4, -0.3, 0.5));
            if (mouthFrame2 != null)
                mouthFrame2.entity().teleport(headCenter.clone().add(0.4, -0.3, 0.5));

            // Particles: reverse_portal from mouth
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, headCenter.clone().add(0, -0.3, 0.5), 5, 0.2, 0.2, 0.2, 0.02);
            }
            // Purple dust along coil
            if (ticksAlive % 7 == 0) {
                int idx = (ticksAlive / 7) % coilBlocks.size();
                DisplayBuilder.purpleDust(coilBlocks.get(idx).entity().getLocation(), 2, 0.3);
            }
            // Falling obsidian tear from glands
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle g : glands) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, g.entity().getLocation(), 2, 0.1, 0.2, 0.1, 0);
                }
            }
            // Sculk charge pop from eyes every 5 seconds
            if (ticksAlive % 100 == 0) {
                for (BlockDisplayHandle eye : eyes) {
                    c.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, eye.entity().getLocation(), 6, 0.2, 0.2, 0.2, 0);
                }
            }
            // Ambient sound loop
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CoilingVoidWorm(plugin); }
    }

    // ================================================================
    // 42. HYDRA CLUSTER — 3 independent necks from shared base
    // ================================================================
    public static class HydraCluster extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> necks = new ArrayList<>();
        private final List<BlockDisplayHandle> basePlates = new ArrayList<>();
        private final List<BlockDisplayHandle> neckJoints = new ArrayList<>();
        private final List<BlockDisplayHandle> neckTips = new ArrayList<>();
        private final List<BlockDisplayHandle> glands = new ArrayList<>();
        private final List<BlockDisplayHandle> baseAmethyst = new ArrayList<>();
        private final List<BlockDisplayHandle> midPoints = new ArrayList<>();

        // Each neck sways independently on 2 axes
        private final float[] swayPhaseX = {0f, 2.1f, 4.2f};
        private final float[] swayPhaseZ = {1.0f, 3.1f, 5.2f};

        public HydraCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hydra_cluster", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(3.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(1200);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 obsidian reinforced base plates (3x3x2)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = 0; y < 2; y++) {
                        Location loc = center.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                        h.scale(1.0f, 1.0f, 1.0f).glow(50, 0, 100).interpolation(2, 0);
                        basePlates.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            // Extra obsidian reinforcing
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 0.5, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.7f, 0.7f, 0.7f).glow(40, 0, 80).interpolation(2, 0);
                basePlates.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 base amethyst flanking central base
            BlockDisplayHandle baseAm1 = displayBuilder.spawnBlock(center.clone().add(-1.5, 1.5, 0), Material.AMETHYST_BLOCK);
            baseAm1.scale(0.5f, 0.5f, 0.5f).glow(180, 80, 255).interpolation(2, 0);
            baseAmethyst.add(baseAm1);
            spawnedEntities.add(baseAm1.entity());
            BlockDisplayHandle baseAm2 = displayBuilder.spawnBlock(center.clone().add(1.5, 1.5, 0), Material.AMETHYST_BLOCK);
            baseAm2.scale(0.5f, 0.5f, 0.5f).glow(180, 80, 255).interpolation(2, 0);
            baseAmethyst.add(baseAm2);
            spawnedEntities.add(baseAm2.entity());

            // 3 necks diverging 120° apart, 30° outward from vertical
            // Neck structure: 4 blocks tapered 3x3→2x2→1x1
            double[] neckBaseAngles = {0, 2 * Math.PI / 3, 4 * Math.PI / 3}; // 120° apart
            for (int n = 0; n < 3; n++) {
                double baseAngle = neckBaseAngles[n];
                List<BlockDisplayHandle> neck = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    double outward = 0.3 * i; // 30° outward
                    double x = Math.cos(baseAngle) * outward;
                    double z = Math.sin(baseAngle) * outward;
                    double y = 2.0 + i * 1.2;
                    Location loc = center.clone().add(x, y, z);
                    float taper = 1.0f - (i * 0.2f); // 3x3→2x2→1x1 sim
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(taper * 1.0f + 0.3f, 1.2f, taper * 1.0f + 0.3f).glow(50, 0, 100).interpolation(2, 0);
                    neck.add(h);
                    spawnedEntities.add(h.entity());
                }
                necks.add(neck);

                // 8 polished blackstone upper sections
                for (int i = 2; i < 4; i++) {
                    Location loc = neck.get(i).entity().getLocation().clone().add(0, 0.7, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.7f, 0.3f, 0.7f).glow(60, 60, 80).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }

                // Neck-to-base mass joint (netherite) — 6 total = 2/neck
                for (int j = 0; j < 2; j++) {
                    double jAngle = baseAngle + (j == 0 ? 0.2 : -0.2);
                    Location jLoc = center.clone().add(Math.cos(jAngle) * 0.5, 2.0 + j, Math.sin(jAngle) * 0.5);
                    BlockDisplayHandle jh = displayBuilder.spawnBlock(jLoc, Material.NETHERITE_BLOCK);
                    jh.scale(0.6f, 0.6f, 0.6f).glow(30, 0, 60).interpolation(2, 0);
                    neckJoints.add(jh);
                    spawnedEntities.add(jh.entity());
                }

                // Crying obsidian glands on inner neck faces
                Location glandLoc = neck.get(1).entity().getLocation().clone();
                BlockDisplayHandle gl = displayBuilder.spawnBlock(glandLoc.add(0, -0.4, 0), Material.CRYING_OBSIDIAN);
                gl.scale(0.5f, 0.5f, 0.5f).glow(100, 0, 200).interpolation(2, 0);
                glands.add(gl);
                spawnedEntities.add(gl.entity());

                // Dark prismarine on outer 2 necks at mid-point
                if (n != 0) { // only neck 1 and 2
                    Location mpLoc = neck.get(1).entity().getLocation().clone().add(0, 0.5, 0);
                    BlockDisplayHandle mp = displayBuilder.spawnBlock(mpLoc, Material.DARK_PRISMARINE);
                    mp.scale(0.6f, 0.4f, 0.6f).glow(0, 150, 180).interpolation(2, 0);
                    midPoints.add(mp);
                    spawnedEntities.add(mp.entity());
                }

                // 1 amethyst tip per neck
                Location tipLoc = neck.get(3).entity().getLocation().clone().add(0, 0.8, 0);
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.AMETHYST_BLOCK);
                tip.scale(0.55f, 0.55f, 0.55f).glow(200, 100, 255).interpolation(2, 0);
                neckTips.add(tip);
                spawnedEntities.add(tip.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_LISTENING, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[] neckBaseAngles = {0, 2 * Math.PI / 3, 4 * Math.PI / 3};

            // Each neck independently sways ±20 degrees on 2 axes, phased differently
            for (int n = 0; n < necks.size(); n++) {
                List<BlockDisplayHandle> neck = necks.get(n);
                double baseAngle = neckBaseAngles[n];

                float swayX = (float) Math.sin(ticksAlive * 0.06 + swayPhaseX[n]) * 0.35f; // ~20 deg
                float swayZ = (float) Math.sin(ticksAlive * 0.05 + swayPhaseZ[n]) * 0.35f;

                for (int i = 0; i < neck.size(); i++) {
                    double outward = 0.3 * i;
                    double baseX = Math.cos(baseAngle) * outward;
                    double baseZ = Math.sin(baseAngle) * outward;
                    double baseY = 2.0 + i * 1.2;
                    // Apply sway increasing with height
                    double swayMult = (i + 1) * 0.4;
                    Location loc = c.clone().add(baseX + swayX * swayMult, baseY, baseZ + swayZ * swayMult);
                    neck.get(i).entity().teleport(loc);
                }

                // Update tip position to follow top of neck
                if (!neck.isEmpty() && n < neckTips.size()) {
                    Location topLoc = neck.get(neck.size() - 1).entity().getLocation();
                    neckTips.get(n).entity().teleport(topLoc.clone().add(0, 0.8, 0));
                }

                // Update gland position
                if (n < glands.size() && neck.size() > 1) {
                    Location gLoc = neck.get(1).entity().getLocation().clone().add(0, -0.4, 0);
                    glands.get(n).entity().teleport(gLoc);
                }
            }

            // Sculk charge pop from each tip every 4 seconds
            if (ticksAlive % 80 == 0) {
                for (BlockDisplayHandle tip : neckTips) {
                    c.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, tip.entity().getLocation(), 8, 0.2, 0.2, 0.2, 0);
                }
            }

            // Staggered listening sound per neck
            for (int n = 0; n < 3; n++) {
                if (ticksAlive % 120 == n * 40) {
                    if (n < necks.size() && !necks.get(n).isEmpty()) {
                        DisplayBuilder.playSound(necks.get(n).get(0).entity().getLocation(),
                                Sound.ENTITY_WARDEN_LISTENING, 0.5f, 0.9f + n * 0.1f);
                    }
                }
            }

            // Reverse portal pooling from base center
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 2, 0), 5, 1.5, 0.3, 1.5, 0.01);
            }

            // Falling obsidian tear from inner glands
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle gl : glands) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, gl.entity().getLocation(), 2, 0.1, 0.2, 0.1, 0);
                }
            }

            // Deep violet dust outlining each neck
            if (ticksAlive % 6 == 0) {
                for (List<BlockDisplayHandle> neck : necks) {
                    for (BlockDisplayHandle nb : neck) {
                        if (Math.random() < 0.3) {
                            DisplayBuilder.purpleDust(nb.entity().getLocation(), 1, 0.3);
                        }
                    }
                }
            }

            // Enderman ambient from tips when proximity check (approximate: every 3s)
            if (ticksAlive % 60 == 0) {
                for (BlockDisplayHandle tip : neckTips) {
                    DisplayBuilder.playSound(tip.entity().getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.4f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HydraCluster(plugin); }
    }

    // ================================================================
    // 43. VOID JELLYFISH — Pulsing inverted dome bell with tentacles
    // ================================================================
    public static class VoidJellyfish extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bellBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> innerLining = new ArrayList<>();
        private final List<BlockDisplayHandle> tentacles = new ArrayList<>();
        private final List<BlockDisplayHandle> wepRim = new ArrayList<>();
        private final List<BlockDisplayHandle> pulseOrgans = new ArrayList<>();
        private final List<BlockDisplayHandle> apexBlocks = new ArrayList<>();

        // Pulse state
        private int pulsePhase = 0; // 0=expanding (20 ticks), 1=contracting (15 ticks)
        private int pulseTimer = 0;
        private float currentWidth = 5.0f;
        private float bellY = 6.0f;
        // Lateral drift
        private double driftAngle = 0;

        public VoidJellyfish(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_jellyfish", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 28 obsidian — inverted dome bell, 5×5 ring top, hollow 3×3, angling inward, 3 blocks tall
            // Top ring (5×5 outer with hollow center — 16 blocks ring)
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue; // hollow center
                    Location loc = center.clone().add(x, bellY, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.9f, 0.4f, 0.9f).glow(80, 0, 160).interpolation(2, 0);
                    bellBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Middle ring (tapers inward — 3×3 outer offset)
            for (int x = -2; x <= 2; x += 2) {
                for (int z = -2; z <= 2; z += 2) {
                    Location loc = center.clone().add(x * 0.8, bellY - 1.2, z * 0.8);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.9f, 0.8f, 0.9f).glow(80, 0, 160).interpolation(2, 0);
                    bellBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Bottom ring
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    Location loc = center.clone().add(x * 0.6, bellY - 2.4, z * 0.6);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.9f, 0.6f, 0.9f).glow(80, 0, 160).interpolation(2, 0);
                    bellBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 10 black glazed terracotta inner lining
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, bellY - 1.0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_GLAZED_TERRACOTTA);
                h.scale(0.6f, 0.5f, 0.6f).glow(20, 0, 40).interpolation(2, 0);
                innerLining.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 blackstone tentacle-filaments hanging from bell rim, varying lengths
            double[] tentLengths = {1.0, 1.5, 2.0, 1.3, 2.5, 1.8, 1.2, 2.2};
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double tx = Math.cos(angle) * 2.2;
                double tz = Math.sin(angle) * 2.2;
                Location loc = center.clone().add(tx, bellY - tentLengths[i], tz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.5f, (float) tentLengths[i], 0.5f).glow(50, 0, 100).interpolation(2, 0);
                tentacles.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 crying obsidian weeping rim alternating
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 2.3, bellY, Math.sin(angle) * 2.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.4f, 0.6f).glow(100, 0, 200).interpolation(2, 0);
                wepRim.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 amethyst top ring cardinal pulsing radial organs
            double[] cardinalAngles = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
            for (double angle : cardinalAngles) {
                Location loc = center.clone().add(Math.cos(angle) * 2.0, bellY + 0.3, Math.sin(angle) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.6f, 0.5f, 0.6f).glow(180, 80, 255).interpolation(2, 0);
                pulseOrgans.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 dark prismarine dome apex
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? 0.2 : -0.2), bellY + 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.7f, 0.5f, 0.7f).glow(0, 150, 180).interpolation(2, 0);
                apexBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Lateral drift: 80-tick sine ±3 blocks
            driftAngle = ticksAlive * (2 * Math.PI / 80.0);
            double driftX = Math.sin(driftAngle) * 3.0;
            double driftZ = Math.cos(driftAngle * 0.7) * 1.5;
            Location bellCenter = c.clone().add(driftX, 0, driftZ);

            // Pulse cycle
            pulseTimer++;
            if (pulsePhase == 0 && pulseTimer >= 20) { // expanding done, switch to contracting
                pulsePhase = 1;
                pulseTimer = 0;
                // Enchant from pulse organs during expansion
                for (BlockDisplayHandle po : pulseOrgans) {
                    c.getWorld().spawnParticle(Particle.ENCHANT, po.entity().getLocation(), 12, 0.3, 0.3, 0.3, 0.1);
                }
                DisplayBuilder.playSound(bellCenter, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 0.6f);
            } else if (pulsePhase == 1 && pulseTimer >= 15) { // contracting done, switch to expanding
                pulsePhase = 0;
                pulseTimer = 0;
                // Reverse portal through hollow bottom
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, bellCenter.clone().add(0, bellY - 3.5, 0), 20, 0.5, 0.5, 0.5, 0.05);
            }

            // Calculate bell width and Y based on phase
            if (pulsePhase == 0) { // expanding: 3-wide → 5-wide over 20 ticks
                currentWidth = 3.0f + (pulseTimer / 20.0f) * 2.0f;
                bellY = 5.5f + (pulseTimer / 20.0f) * 0.5f; // rises 0.5 during expansion
            } else { // contracting: 5-wide → 3-wide over 15 ticks
                currentWidth = 5.0f - (pulseTimer / 15.0f) * 2.0f;
                bellY = 6.0f - (pulseTimer / 15.0f) * 0.5f; // descends 0.5 during contraction
            }

            // Update bell block positions
            float scale = currentWidth / 5.0f;
            for (int i = 0; i < bellBlocks.size(); i++) {
                BlockDisplay bd = bellBlocks.get(i).entity();
                Location origLoc = bd.getLocation();
                // Scale XZ around bell center
                double relX = Math.signum(origLoc.getX() - c.getX());
                double relZ = Math.signum(origLoc.getZ() - c.getZ());
                double newX = driftX + relX * scale * 2.0;
                double newZ = driftZ + relZ * scale * 2.0;
                double newY = bellY - (i / (double) bellBlocks.size()) * 2.5;
                bd.teleport(c.clone().add(newX, newY, newZ));
            }

            // Update weeping rim
            for (int i = 0; i < wepRim.size(); i++) {
                double angle = (Math.PI * 2 * i) / wepRim.size();
                Location wLoc = bellCenter.clone().add(Math.cos(angle) * scale * 2.3, bellY, Math.sin(angle) * scale * 2.3);
                wepRim.get(i).entity().teleport(wLoc);
            }

            // Update pulse organs
            double[] cardAngles = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
            for (int i = 0; i < pulseOrgans.size(); i++) {
                Location pLoc = bellCenter.clone().add(Math.cos(cardAngles[i]) * scale * 2.0, bellY + 0.3, Math.sin(cardAngles[i]) * scale * 2.0);
                pulseOrgans.get(i).entity().teleport(pLoc);
            }

            // Tentacles trail 8 ticks behind vertical movement, sway outward during expansion
            for (int i = 0; i < tentacles.size(); i++) {
                double angle = (Math.PI * 2 * i) / tentacles.size();
                double tentSway = (pulsePhase == 0) ? scale * 0.5 : 0;
                double tx = Math.cos(angle) * (scale * 2.2 + tentSway);
                double tz = Math.sin(angle) * (scale * 2.2 + tentSway);
                double tentY = (ticksAlive - 8 >= 0) ? bellY - 1.5 : bellY - 1.5;
                tentacles.get(i).entity().teleport(bellCenter.clone().add(tx, tentY - i * 0.05, tz));
            }

            // Inner lining follows bell center
            for (int i = 0; i < innerLining.size(); i++) {
                double angle = (Math.PI * 2 * i) / innerLining.size();
                Location ilLoc = bellCenter.clone().add(Math.cos(angle) * scale * 1.5, bellY - 1.0, Math.sin(angle) * scale * 1.5);
                innerLining.get(i).entity().teleport(ilLoc);
            }

            // Apex follows bell center top
            for (int i = 0; i < apexBlocks.size(); i++) {
                apexBlocks.get(i).entity().teleport(bellCenter.clone().add((i == 0 ? 0.2 : -0.2), bellY + 0.5, 0));
            }

            // Falling obsidian tear from weeping rim
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle wr : wepRim) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, wr.entity().getLocation(), 2, 0.1, 0.3, 0.1, 0);
                }
            }

            // Bubble pop from tentacle tips during sway
            if (pulsePhase == 0 && ticksAlive % 6 == 0) {
                for (BlockDisplayHandle tent : tentacles) {
                    Location tLoc = tent.entity().getLocation().clone().add(0, -0.5, 0);
                    c.getWorld().spawnParticle(Particle.BUBBLE_POP, tLoc, 2, 0.1, 0.1, 0.1, 0);
                }
            }

            // Sound: underwater ambient loop
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(bellCenter, Sound.AMBIENT_UNDERWATER_LOOP, 0.5f, 1.0f);
            }
            // Elder guardian occasionally
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(bellCenter, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidJellyfish(plugin); }
    }

    // ================================================================
    // 44. THE WRITHING MASS — Independent oscillating blob with arms
    // ================================================================
    public static class TheWrithingMass extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> armStumps = new ArrayList<>();
        private final List<BlockDisplayHandle> glandNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> surfacePatches = new ArrayList<>();
        private final List<BlockDisplayHandle> inclusions = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();

        private final float[] oscAmplitude = new float[18];
        private final float[] oscPeriod = new float[18];
        private final float[] oscPhase = new float[18];
        private final int[] oscAxis = new int[18];
        private double driftAngle = 0;
        private float massYaw = 0f;
        private static final Random MRNG = new Random(42L);

        public TheWrithingMass(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_writhing_mass", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double[][] coreOffsets = {
                {0,0,0},{0.5,0,0.3},{-0.4,0,0.5},{0.3,0,-0.4},{-0.5,0,-0.3},
                {0,1,0},{0.4,1,0.4},{-0.3,1,0.3},{0.5,1,-0.4},{-0.4,1,-0.3},
                {0,2,0},{0.3,2,0.5},{-0.5,2,0.2},{0.4,2,-0.3},
                {0,3,0},{0.3,3,0.3},{-0.3,3,0.2},{0,3,-0.4}
            };
            for (int i = 0; i < 18; i++) {
                double[] off = coreOffsets[i];
                Location loc = center.clone().add(off[0], 1 + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float s = 0.85f + MRNG.nextFloat() * 0.3f;
                h.scale(s, s, s).glow(80, 0, 160).interpolation(2, 0);
                coreBlocks.add(h);
                spawnedEntities.add(h.entity());
                oscAmplitude[i] = 0.1f + MRNG.nextFloat() * 0.3f;
                oscPeriod[i] = 6.0f + MRNG.nextFloat() * 12.0f;
                oscPhase[i] = MRNG.nextFloat() * (float)(Math.PI * 2);
                oscAxis[i] = MRNG.nextInt(3);
            }

            double[][] armDirs = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0.7,0,0.7},{-0.7,0,-0.7}};
            for (int a = 0; a < 6; a++) {
                List<BlockDisplayHandle> stump = new ArrayList<>();
                for (int seg = 0; seg < 2; seg++) {
                    double[] dir = armDirs[a];
                    Location loc = center.clone().add(
                            dir[0] * (1.5 + seg), 1.5 + a * 0.3, dir[2] * (1.5 + seg));
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    float ts = 0.8f - seg * 0.2f;
                    h.scale(ts, ts, ts).glow(50, 0, 100).interpolation(2, 0);
                    stump.add(h);
                    spawnedEntities.add(h.entity());
                }
                armStumps.add(stump);
            }

            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                int level = i % 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 1 + level, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(100, 0, 200).interpolation(2, 0);
                glandNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            double[][] patchPos = {{0.8,0.5,0.8},{-0.8,0.5,0.8},{0.8,0.5,-0.8},{0.6,2.5,0.6},{-0.6,2.5,-0.6},{0,3,0.7}};
            for (double[] pp : patchPos) {
                Location loc = center.clone().add(pp[0], pp[1], pp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.COBBLED_DEEPSLATE);
                h.scale(0.7f, 0.4f, 0.7f).glow(40, 40, 50).interpolation(2, 0);
                surfacePatches.add(h);
                spawnedEntities.add(h.entity());
            }

            double[][] inclPos = {{0,1.5,0},{0.2,2.5,0.2},{-0.2,1,0.2},{0.1,3,0}};
            for (double[] ip : inclPos) {
                Location loc = center.clone().add(ip[0], ip[1], ip[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(30, 0, 60).interpolation(2, 0);
                inclusions.add(h);
                spawnedEntities.add(h.entity());
            }

            BlockDisplayHandle eyeA = displayBuilder.spawnBlock(center.clone().add(0.7, 2.8, 0.7), Material.AMETHYST_BLOCK);
            eyeA.scale(0.55f, 0.55f, 0.55f).glow(200, 100, 255).interpolation(2, 0);
            eyes.add(eyeA);
            spawnedEntities.add(eyeA.entity());
            BlockDisplayHandle eyeB = displayBuilder.spawnBlock(center.clone().add(-0.7, 2.8, -0.7), Material.AMETHYST_BLOCK);
            eyeB.scale(0.55f, 0.55f, 0.55f).glow(200, 100, 255).interpolation(2, 0);
            eyes.add(eyeB);
            spawnedEntities.add(eyeB.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            driftAngle = ticksAlive * (2 * Math.PI / 120.0);
            double driftX = Math.sin(driftAngle) * 3.0;
            double driftZ = Math.cos(driftAngle) * 3.0;
            Location massCenter = c.clone().add(driftX, 0, driftZ);

            massYaw += 0.4f;
            float massRad = massYaw * (float) Math.PI / 180.0f;

            double[][] coreOffsets = {
                {0,0,0},{0.5,0,0.3},{-0.4,0,0.5},{0.3,0,-0.4},{-0.5,0,-0.3},
                {0,1,0},{0.4,1,0.4},{-0.3,1,0.3},{0.5,1,-0.4},{-0.4,1,-0.3},
                {0,2,0},{0.3,2,0.5},{-0.5,2,0.2},{0.4,2,-0.3},
                {0,3,0},{0.3,3,0.3},{-0.3,3,0.2},{0,3,-0.4}
            };
            for (int i = 0; i < coreBlocks.size(); i++) {
                double[] baseOff = coreOffsets[i];
                float osc = (float) Math.sin(ticksAlive / oscPeriod[i] * Math.PI * 2 + oscPhase[i]) * oscAmplitude[i];
                double ox = baseOff[0] + (oscAxis[i] == 0 ? osc : 0);
                double oy = 1 + baseOff[1] + (oscAxis[i] == 1 ? osc : 0);
                double oz = baseOff[2] + (oscAxis[i] == 2 ? osc : 0);
                double rotX = ox * Math.cos(massRad) - oz * Math.sin(massRad);
                double rotZ = ox * Math.sin(massRad) + oz * Math.cos(massRad);
                coreBlocks.get(i).entity().teleport(massCenter.clone().add(rotX, oy, rotZ));
            }

            double[][] armDirs = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0.7,0,0.7},{-0.7,0,-0.7}};
            int[] armCycles = {15, 18, 22, 25, 20, 28};
            for (int a = 0; a < armStumps.size(); a++) {
                float extT = (float)(ticksAlive % armCycles[a]) / armCycles[a];
                float ext = (float) Math.sin(extT * Math.PI);
                double[] dir = armDirs[a];
                List<BlockDisplayHandle> stump = armStumps.get(a);
                for (int seg = 0; seg < stump.size(); seg++) {
                    double bx = dir[0] * (1.5 + seg + ext);
                    double bz = dir[2] * (1.5 + seg + ext);
                    double rotX = bx * Math.cos(massRad) - bz * Math.sin(massRad);
                    double rotZ = bx * Math.sin(massRad) + bz * Math.cos(massRad);
                    stump.get(seg).entity().teleport(massCenter.clone().add(rotX, 1.5 + a * 0.3, rotZ));
                }
            }

            for (int i = 0; i < glandNodes.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + massRad;
                int level = i % 4;
                glandNodes.get(i).entity().teleport(massCenter.clone().add(
                        Math.cos(angle) * 1.2, 1 + level, Math.sin(angle) * 1.2));
            }

            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle gl : glandNodes) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, gl.entity().getLocation(), 3, 0.3, 0.3, 0.3, 0);
                }
            }
            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, massCenter.clone().add(0, 2, 0), 5, 0.8, 0.5, 0.8, 0.01);
            }
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, massCenter.clone().add(0, 0.3, 0), 4, 0.5, 0.3, 0.5, 0.02);
            }
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, massCenter.clone().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.05);
            }
            if (ticksAlive % 60 == 0) DisplayBuilder.playSound(massCenter, Sound.ENTITY_WARDEN_ROAR, 0.4f, 0.5f);
            if (ticksAlive % 40 == 0) DisplayBuilder.playSound(massCenter, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.9f);
            if (ticksAlive % 80 == 0) DisplayBuilder.playSound(massCenter, Sound.ENTITY_WARDEN_LISTENING, 0.4f, 0.8f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheWrithingMass(plugin); }
    }

    // ================================================================
    // 45. TENDRIL FOREST — 15 independently swaying stalks
    // ================================================================
    public static class TendrilForest extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> stalks = new ArrayList<>();
        private final List<BlockDisplayHandle> weepingTips = new ArrayList<>();
        private final List<BlockDisplayHandle> rootSwellings = new ArrayList<>();
        private final List<BlockDisplayHandle> smoothNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> groundNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> tallBases = new ArrayList<>();

        private final float[] swayAmplitude = new float[15];
        private final float[] swayPeriod = new float[15];
        private final float[] swayPhaseArr = new float[15];
        private final float[] swayAxisX = new float[15];
        private final float[] swayAxisZ = new float[15];
        private final int[] nodePulsePeriods = {15, 22, 18, 25};
        private static final Random FRNG = new Random(99L);

        public TendrilForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tendril_forest", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(0.5);
            config.setDamageRadius(1.5);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double[][] stalkPos = {
                {0,0},{1.5,1},{-1.5,1},{1,-1.5},{-1,-1.5},
                {2.5,0},{-2.5,0},{0,2.5},{0,-2.5},{2,2},
                {-2,2},{2,-2},{-2,-2},{0.5,0.5},{-0.5,-0.5}
            };
            int[] stalkHeights = {5,4,3,4,2,3,5,4,3,2,4,3,5,2,4};

            for (int s = 0; s < 15; s++) {
                List<BlockDisplayHandle> stalk = new ArrayList<>();
                double sx = stalkPos[s][0];
                double sz = stalkPos[s][1];
                int height = stalkHeights[s];
                for (int y = 0; y < height; y++) {
                    float taper = (y == height - 1) ? 0.5f : (1.0f - y * 0.1f);
                    Location loc = center.clone().add(sx, 0.5 + y, sz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(taper, 1.0f, taper).glow(50, 0, 100).interpolation(2, 0);
                    stalk.add(h);
                    spawnedEntities.add(h.entity());
                }
                stalks.add(stalk);
                swayAmplitude[s] = 0.14f + FRNG.nextFloat() * 0.30f;
                swayPeriod[s] = 12.0f + FRNG.nextFloat() * 28.0f;
                swayPhaseArr[s] = FRNG.nextFloat() * (float)(Math.PI * 2);
                swayAxisX[s] = (FRNG.nextFloat() - 0.5f) * 2;
                swayAxisZ[s] = (FRNG.nextFloat() - 0.5f) * 2;
            }

            // 12 crying obsidian weeping tips (first 12 stalks)
            for (int s = 0; s < 12; s++) {
                List<BlockDisplayHandle> stalk = stalks.get(s);
                if (stalk.isEmpty()) continue;
                Location tipLoc = stalk.get(stalk.size() - 1).entity().getLocation().clone().add(0, 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(tipLoc, Material.CRYING_OBSIDIAN);
                h.scale(0.55f, 0.55f, 0.55f).glow(100, 0, 200).interpolation(2, 0);
                weepingTips.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 obsidian root swellings at base of first 8 stalks
            for (int s = 0; s < 8; s++) {
                List<BlockDisplayHandle> stalk = stalks.get(s);
                if (stalk.isEmpty()) continue;
                Location baseLoc = stalk.get(0).entity().getLocation().clone().add(0, -0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(baseLoc, Material.OBSIDIAN);
                h.scale(1.2f, 0.4f, 1.2f).glow(40, 0, 80).interpolation(2, 0);
                rootSwellings.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 polished blackstone: 3 smooth tips (stalks 12-14) + 3 mid-stalk nodes
            for (int s = 12; s < 15; s++) {
                List<BlockDisplayHandle> stalk = stalks.get(s);
                if (stalk.isEmpty()) continue;
                Location tipLoc = stalk.get(stalk.size() - 1).entity().getLocation().clone().add(0, 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(tipLoc, Material.POLISHED_BLACKSTONE);
                h.scale(0.6f, 0.3f, 0.6f).glow(60, 60, 80).interpolation(2, 0);
                smoothNodes.add(h);
                spawnedEntities.add(h.entity());
            }
            int[] midStalkIds = {3, 7, 10};
            for (int s : midStalkIds) {
                List<BlockDisplayHandle> stalk = stalks.get(s);
                int midIdx = stalk.size() / 2;
                if (midIdx < stalk.size()) {
                    Location mLoc = stalk.get(midIdx).entity().getLocation().clone().add(0, 0.4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(mLoc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.7f, 0.25f, 0.7f).glow(60, 60, 80).interpolation(2, 0);
                    smoothNodes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 4 amethyst ground-level root-glow nodes
            double[][] gNodePos = {{0.8,0,-0.8},{-0.8,0,0.8},{0.5,0,-0.5},{-0.5,0,0.5}};
            for (double[] gp : gNodePos) {
                Location loc = center.clone().add(gp[0], 0.2, gp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.6f, 0.6f, 0.6f).glow(180, 80, 255).interpolation(2, 0);
                groundNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 netherite bases at tallest stalks (indices 0, 6, 12)
            int[] tallIds = {0, 6, 12};
            for (int s : tallIds) {
                List<BlockDisplayHandle> stalk = stalks.get(s);
                if (stalk.isEmpty()) continue;
                Location bLoc = stalk.get(0).entity().getLocation().clone().add(0, -0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(bLoc, Material.NETHERITE_BLOCK);
                h.scale(0.9f, 0.5f, 0.9f).glow(30, 0, 60).interpolation(2, 0);
                tallBases.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ROOTS_STEP, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[][] stalkPos = {
                {0,0},{1.5,1},{-1.5,1},{1,-1.5},{-1,-1.5},
                {2.5,0},{-2.5,0},{0,2.5},{0,-2.5},{2,2},
                {-2,2},{2,-2},{-2,-2},{0.5,0.5},{-0.5,-0.5}
            };

            for (int s = 0; s < stalks.size(); s++) {
                List<BlockDisplayHandle> stalk = stalks.get(s);
                double sx = stalkPos[s][0];
                double sz = stalkPos[s][1];
                float sway = (float) Math.sin(ticksAlive / swayPeriod[s] * Math.PI * 2 + swayPhaseArr[s]) * swayAmplitude[s];
                for (int y = 0; y < stalk.size(); y++) {
                    float heightMult = (y + 1) / (float) stalk.size();
                    double ox = sx + swayAxisX[s] * sway * heightMult;
                    double oz = sz + swayAxisZ[s] * sway * heightMult;
                    stalk.get(y).entity().teleport(c.clone().add(ox, 0.5 + y, oz));
                }
                // Update weeping tip
                if (s < weepingTips.size() && !stalk.isEmpty()) {
                    weepingTips.get(s).entity().teleport(
                            stalk.get(stalk.size() - 1).entity().getLocation().clone().add(0, 0.6, 0));
                }
            }

            // Falling obsidian tear from weeping tips desynchronized
            for (int i = 0; i < weepingTips.size(); i++) {
                if (ticksAlive % 5 == i % 5) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            weepingTips.get(i).entity().getLocation(), 3, 0.1, 0.3, 0.1, 0);
                }
            }

            // Sculk soul from ground nodes
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle gn : groundNodes) {
                    c.getWorld().spawnParticle(Particle.SCULK_SOUL, gn.entity().getLocation().add(0, 0.3, 0), 2, 0.2, 0.3, 0.2, 0.03);
                }
            }

            // Ground node individual pulse
            for (int i = 0; i < groundNodes.size(); i++) {
                if (ticksAlive % nodePulsePeriods[i] == 0) {
                    float pulse = 0.5f + (float) Math.abs(Math.sin(ticksAlive * 0.1 + i)) * 0.3f;
                    groundNodes.get(i).scale(pulse, pulse, pulse);
                    groundNodes.get(i).interpolation(nodePulsePeriods[i] / 2, 0);
                }
            }

            // Dark violet dust at mid-stalk height
            if (ticksAlive % 6 == 0) {
                double midH = 1.5 + Math.sin(ticksAlive * 0.03) * 0.3;
                DisplayBuilder.purpleDust(c.clone().add(0, midH, 0), 3, 3.0);
            }

            if (ticksAlive % 100 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_ROOTS_STEP, 0.4f, 0.4f);
            if (ticksAlive % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.5f);
            if (ticksAlive % 120 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.3f, 1.0f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TendrilForest(plugin); }
    }

    // ================================================================
    // 46. ABYSS CRAWLER — Multi-legged mobile patrol entity
    // ================================================================
    public static class AbyssCrawler extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> dorsalBody = new ArrayList<>();
        private final List<BlockDisplayHandle> bellyPlate = new ArrayList<>();
        private final List<BlockDisplayHandle> legJoints = new ArrayList<>();
        private final List<BlockDisplayHandle> dorsalCorners = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> altLegTips = new ArrayList<>();
        private BlockDisplayHandle eyeL;
        private BlockDisplayHandle eyeR;
        private BlockDisplayHandle spineNode;

        private double crawlerX = 0;
        private double crawlerZ = 0;
        private double crawlerFacing = 0;
        private static final double CRAWLER_SPEED = 0.3 / 20.0;
        private static final double PATROL_BOUNDARY = 8.0;
        private int gaitPhase = 0;
        private int gaitTimer = 0;
        private static final int GAIT_TICKS = 8;

        public AbyssCrawler(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyss_crawler", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(1200);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4×2×2 dorsal body mass (16 blocks)
            for (int x = -2; x <= 1; x++) {
                for (int y = 1; y <= 2; y++) {
                    for (int z = -1; z <= 0; z++) {
                        Location loc = center.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(1.0f, 1.0f, 1.0f).glow(80, 0, 160).interpolation(2, 0);
                        dorsalBody.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // 10 blackstone belly plate
            for (int x = -2; x <= 1; x++) {
                for (int z = -1; z <= 0; z++) {
                    Location loc = center.clone().add(x, 0.6, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(1.0f, 0.4f, 1.0f).glow(50, 0, 100).interpolation(2, 0);
                    bellyPlate.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 6 legs (3 per side), each 3 segments — 45° downward/outward
            double[] legXPos = {-1.5, -0.5, 0.5};
            for (int side = 0; side < 2; side++) {
                double sideZ = (side == 0) ? 1.0 : -1.0;
                for (int l = 0; l < 3; l++) {
                    List<BlockDisplayHandle> leg = new ArrayList<>();
                    for (int seg = 0; seg < 3; seg++) {
                        double lx = legXPos[l];
                        double ly = 1.5 - seg * 0.45;
                        double lz = sideZ * (0.5 + seg * 0.45);
                        Location loc = center.clone().add(lx, ly, lz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        float ts = 0.6f - seg * 0.1f;
                        h.scale(ts, ts, ts).glow(60, 0, 120).interpolation(2, 0);
                        leg.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    legs.add(leg);
                    // Netherite joint at leg root
                    Location jLoc = center.clone().add(legXPos[l], 1.5, sideZ * 0.5);
                    BlockDisplayHandle jh = displayBuilder.spawnBlock(jLoc, Material.NETHERITE_BLOCK);
                    jh.scale(0.5f, 0.5f, 0.5f).glow(30, 0, 60).interpolation(2, 0);
                    legJoints.add(jh);
                    spawnedEntities.add(jh.entity());
                }
            }

            // 4 polished blackstone dorsal corners
            double[][] cornerPos = {{-2,2,-1},{1,2,-1},{-2,2,0},{1,2,0}};
            for (double[] cp : cornerPos) {
                Location loc = center.clone().add(cp[0], cp[1], cp[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.5f, 0.8f).glow(60, 60, 80).interpolation(2, 0);
                dorsalCorners.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 crying obsidian tips on legs 0, 2, 4
            int[] altIds = {0, 2, 4};
            for (int li : altIds) {
                List<BlockDisplayHandle> leg = legs.get(li);
                if (!leg.isEmpty()) {
                    Location tipLoc = leg.get(leg.size() - 1).entity().getLocation().clone().add(0, -0.3, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(tipLoc, Material.CRYING_OBSIDIAN);
                    h.scale(0.5f, 0.5f, 0.5f).glow(100, 0, 200).interpolation(2, 0);
                    altLegTips.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 2 amethyst eyes on front face
            eyeL = displayBuilder.spawnBlock(center.clone().add(-2.1, 2.2, 0.3), Material.AMETHYST_BLOCK);
            eyeL.scale(0.5f, 0.5f, 0.5f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(eyeL.entity());
            eyeR = displayBuilder.spawnBlock(center.clone().add(-2.1, 2.2, -0.3), Material.AMETHYST_BLOCK);
            eyeR.scale(0.5f, 0.5f, 0.5f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(eyeR.entity());

            // 1 dark prismarine center dorsal spine node
            spineNode = displayBuilder.spawnBlock(center.clone().add(-0.5, 2.6, -0.5), Material.DARK_PRISMARINE);
            spineNode.scale(0.6f, 0.5f, 0.6f).glow(0, 150, 180).interpolation(2, 0);
            spawnedEntities.add(spineNode.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Advance patrol position
            crawlerX += Math.cos(crawlerFacing) * CRAWLER_SPEED;
            crawlerZ += Math.sin(crawlerFacing) * CRAWLER_SPEED;
            if (Math.abs(crawlerX) > PATROL_BOUNDARY || Math.abs(crawlerZ) > PATROL_BOUNDARY) {
                crawlerFacing += Math.PI;
                crawlerX = Math.max(-PATROL_BOUNDARY, Math.min(PATROL_BOUNDARY, crawlerX));
                crawlerZ = Math.max(-PATROL_BOUNDARY, Math.min(PATROL_BOUNDARY, crawlerZ));
            }

            // Gait cycle
            gaitTimer++;
            if (gaitTimer >= GAIT_TICKS) {
                gaitPhase = 1 - gaitPhase;
                gaitTimer = 0;
                DisplayBuilder.playSound(c.clone().add(crawlerX, 0, crawlerZ), Sound.BLOCK_STONE_STEP, 0.6f, 0.7f);
            }
            float gaitT = (float) gaitTimer / GAIT_TICKS;
            double bodyBob = Math.sin(gaitT * Math.PI) * 0.15;
            float cos = (float) Math.cos(crawlerFacing);
            float sin = (float) Math.sin(crawlerFacing);

            // Update dorsal body
            int bi = 0;
            for (int x = -2; x <= 1; x++) {
                for (int y = 1; y <= 2; y++) {
                    for (int z = -1; z <= 0; z++) {
                        if (bi >= dorsalBody.size()) break;
                        double wx = cos * x - sin * z;
                        double wz = sin * x + cos * z;
                        dorsalBody.get(bi).entity().teleport(c.clone().add(crawlerX + wx, y + bodyBob, crawlerZ + wz));
                        bi++;
                    }
                }
            }

            // Update belly
            int bpi = 0;
            for (int x = -2; x <= 1; x++) {
                for (int z = -1; z <= 0; z++) {
                    if (bpi >= bellyPlate.size()) break;
                    double wx = cos * x - sin * z;
                    double wz = sin * x + cos * z;
                    bellyPlate.get(bpi).entity().teleport(c.clone().add(crawlerX + wx, 0.6 + bodyBob, crawlerZ + wz));
                    bpi++;
                }
            }

            // Update legs with alternating tripod
            double[] legXPos = {-1.5, -0.5, 0.5};
            for (int legIdx = 0; legIdx < legs.size(); legIdx++) {
                List<BlockDisplayHandle> leg = legs.get(legIdx);
                int side = legIdx / 3;
                int lpos = legIdx % 3;
                double sideZ = (side == 0) ? 1.0 : -1.0;
                boolean isLifted = (legIdx % 2 == gaitPhase);
                float liftY = isLifted ? gaitT * 0.4f : 0;

                for (int seg = 0; seg < leg.size(); seg++) {
                    double lx = legXPos[lpos];
                    double ly = 1.5 - seg * 0.45 + bodyBob + liftY;
                    double lz = sideZ * (0.5 + seg * 0.45);
                    double wx = cos * lx - sin * lz;
                    double wz = sin * lx + cos * lz;
                    leg.get(seg).entity().teleport(c.clone().add(crawlerX + wx, ly, crawlerZ + wz));
                }

                if (legIdx < legJoints.size()) {
                    double wx = cos * legXPos[lpos] - sin * (sideZ * 0.5);
                    double wz = sin * legXPos[lpos] + cos * (sideZ * 0.5);
                    legJoints.get(legIdx).entity().teleport(
                            c.clone().add(crawlerX + wx, 1.5 + bodyBob + liftY * 0.5, crawlerZ + wz));
                }
            }

            // Update alt leg tips
            int[] altIds = {0, 2, 4};
            for (int i = 0; i < altLegTips.size(); i++) {
                int legIdx = altIds[i];
                if (legIdx < legs.size() && !legs.get(legIdx).isEmpty()) {
                    Location tipBase = legs.get(legIdx).get(legs.get(legIdx).size() - 1).entity().getLocation();
                    altLegTips.get(i).entity().teleport(tipBase.clone().add(0, -0.3, 0));
                }
            }

            // Update eyes
            double frontX = Math.cos(crawlerFacing) * 2.1;
            double frontZ = Math.sin(crawlerFacing) * 2.1;
            if (eyeL != null)
                eyeL.entity().teleport(c.clone().add(crawlerX + frontX + sin * 0.3, 2.2 + bodyBob, crawlerZ + frontZ - cos * 0.3));
            if (eyeR != null)
                eyeR.entity().teleport(c.clone().add(crawlerX + frontX - sin * 0.3, 2.2 + bodyBob, crawlerZ + frontZ + cos * 0.3));

            // Update spine node
            if (spineNode != null)
                spineNode.entity().teleport(c.clone().add(crawlerX, 2.6 + bodyBob, crawlerZ));

            // Update dorsal corners
            double[][] cornerOff = {{-2,2,-1},{1,2,-1},{-2,2,0},{1,2,0}};
            for (int i = 0; i < dorsalCorners.size(); i++) {
                double lx = cornerOff[i][0];
                double lz = cornerOff[i][2];
                double wx = cos * lx - sin * lz;
                double wz = sin * lx + cos * lz;
                dorsalCorners.get(i).entity().teleport(
                        c.clone().add(crawlerX + wx, cornerOff[i][1] + bodyBob, crawlerZ + wz));
            }

            // Foot-down dust
            if (gaitTimer == 0) {
                for (int legIdx = 0; legIdx < legs.size(); legIdx++) {
                    if (legIdx % 2 == gaitPhase && !legs.get(legIdx).isEmpty()) {
                        DisplayBuilder.purpleDust(
                                legs.get(legIdx).get(legs.get(legIdx).size() - 1).entity().getLocation(), 3, 0.3);
                    }
                }
            }

            // Large smoke trail behind body
            if (ticksAlive % 4 == 0) {
                double trailX = -Math.cos(crawlerFacing) * 2.5;
                double trailZ = -Math.sin(crawlerFacing) * 2.5;
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(crawlerX + trailX, 1.5, crawlerZ + trailZ), 4, 0.4, 0.3, 0.4, 0.01);
            }

            // Sculk charge pop from eyes
            if (ticksAlive % 60 == 0) {
                if (eyeL != null) c.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, eyeL.entity().getLocation(), 5, 0.2, 0.2, 0.2, 0);
                if (eyeR != null) c.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, eyeR.entity().getLocation(), 5, 0.2, 0.2, 0.2, 0);
            }

            // Heartbeat
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c.clone().add(crawlerX, 0, crawlerZ), Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssCrawler(plugin); }
    }
}
