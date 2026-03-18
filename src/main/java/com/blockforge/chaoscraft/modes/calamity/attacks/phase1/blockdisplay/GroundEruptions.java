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

/**
 * Phase 1 Block Display — GROUP 1: GROUND ERUPTIONS
 * 10 structures that burst upward from the island floor.
 * Adapted from boss1-voidmaw.md with Calamity attack rules applied:
 * - NO status effects (Blindness, Slowness, etc. removed → damage only)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Calamity particle palette (purple, cyan, crimson)
 * - All values configurable via AttackConfig
 */
public final class GroundEruptions {

    private GroundEruptions() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidFang(plugin));
        registry.register(new ShatterPlate(plugin));
        registry.register(new ObsidianRib(plugin));
        registry.register(new VoidGeyserColumn(plugin));
        registry.register(new BlackstoneThorns(plugin));
        registry.register(new SoulVentMound(plugin));
        registry.register(new NetheriteMonolith(plugin));
        registry.register(new CrackedVoidEye(plugin));
        registry.register(new AbyssStake(plugin));
        registry.register(new FracturedMawTooth(plugin));
    }

    // ================================================================
    // 1. VOID FANG — Tapering obsidian column that erupts from below
    // ================================================================
    public static class VoidFang extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private BlockDisplayHandle tipBlock;
        private float emergeProgress = 0;

        public VoidFang(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_fang", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spine: 5 obsidian blocks in tapering column (start underground)
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, -6 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float taper = 1.0f - (i * 0.12f);
                h.scale(taper, 1.0f, taper).glow(80, 0, 160).interpolation(2, 0);
                spineBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Base flare: 6 crying obsidian splayed outward
            double[][] baseOffsets = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0.7,0,0.7},{-0.7,0,-0.7}};
            for (double[] off : baseOffsets) {
                Location loc = center.clone().add(off[0], -6, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 0.6f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
                baseBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Tip: netherite block
            Location tipLoc = center.clone().add(0, -1, 0);
            tipBlock = displayBuilder.spawnBlock(tipLoc, Material.NETHERITE_BLOCK);
            tipBlock.scale(0.4f, 0.6f, 0.4f).glow(40, 0, 80).interpolation(2, 0);
            spawnedEntities.add(tipBlock.entity());

            // Spawn sound
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.6f);
            // Spawn burst particles
            DisplayBuilder.purpleDust(center, 30, 1.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Emerge upward over 40 ticks
            if (ticksAlive <= 40) {
                emergeProgress = ticksAlive / 40.0f;
                float yOffset = emergeProgress * 6.0f; // Rise 6 blocks
                for (int i = 0; i < spineBlocks.size(); i++) {
                    BlockDisplay bd = spineBlocks.get(i).entity();
                    Location target = c.clone().add(0, -6 + i + yOffset, 0);
                    bd.teleport(target);
                }
                for (int i = 0; i < baseBlocks.size(); i++) {
                    BlockDisplay bd = baseBlocks.get(i).entity();
                    double[][] offsets = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0.7,0,0.7},{-0.7,0,-0.7}};
                    Location target = c.clone().add(offsets[i][0], -6 + yOffset, offsets[i][2]);
                    bd.teleport(target);
                }
                if (tipBlock != null) {
                    tipBlock.entity().teleport(c.clone().add(0, -1 + yOffset + 5, 0));
                }
                // Scale animation: 0.1 → 1.0
                float scale = 0.1f + emergeProgress * 0.9f;
                for (BlockDisplayHandle h : spineBlocks) {
                    float taper = scale * (1.0f - (spineBlocks.indexOf(h) * 0.12f));
                    h.scale(taper, scale, taper);
                    h.interpolation(3, 0);
                }
            }
            // Phase 2: Tremble micro-oscillation
            else {
                float tremble = (float) Math.sin(ticksAlive * 1.2) * 0.15f;
                for (BlockDisplayHandle h : spineBlocks) {
                    BlockDisplay bd = h.entity();
                    Location base = bd.getLocation();
                    bd.teleport(base.clone().add(0, tremble - (float) Math.sin((ticksAlive - 1) * 1.2) * 0.15f, 0));
                }
            }

            // Ongoing particles: crying obsidian tears from base
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle h : baseBlocks) {
                    Location bLoc = h.entity().getLocation().add(0, 0.5, 0);
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, bLoc, 2, 0.1, 0.2, 0.1, 0);
                }
                // Purple dust at base
                DisplayBuilder.purpleDust(c.clone().add(0, 0.5, 0), 3, 0.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidFang(plugin); }
    }

    // ================================================================
    // 2. SHATTER PLATE — Tilted deepslate disc with orbiting shrapnel
    // ================================================================
    public static class ShatterPlate extends BlockDisplayAttack {

        private BlockDisplayHandle mainDisc;
        private final List<BlockDisplayHandle> shrapnel = new ArrayList<>();
        private float emergeProgress = 0;

        public ShatterPlate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shatter_plate", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(40); // Only damage on spawn
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main disc: 5x5 cobbled deepslate, tilted 15°
            mainDisc = displayBuilder.spawnBlock(center.clone().add(0, -3, 0), Material.COBBLED_DEEPSLATE);
            mainDisc.scale(5.0f, 0.4f, 5.0f).glow(60, 60, 80)
                    .rotate(0.26f, 1, 0, 0) // 15° tilt
                    .interpolation(3, 0);
            spawnedEntities.add(mainDisc.entity());

            // 4 shrapnel orbiters
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location sLoc = center.clone().add(Math.cos(angle) * 2.5, -3, Math.sin(angle) * 2.5);
                BlockDisplayHandle s = displayBuilder.spawnBlock(sLoc, Material.DEEPSLATE);
                s.scale(0.5f, 0.5f, 0.5f).glow(70, 70, 90).interpolation(2, 0);
                shrapnel.add(s);
                spawnedEntities.add(s.entity());
            }

            // Amethyst node at tilt apex
            BlockDisplayHandle amethyst = displayBuilder.spawnBlock(center.clone().add(1.5, -2.5, 0), Material.AMETHYST_BLOCK);
            amethyst.scale(0.6f, 0.6f, 0.6f).glow(180, 80, 255).interpolation(2, 0);
            spawnedEntities.add(amethyst.entity());

            // Spawn effects
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.8f);
            w.spawnParticle(Particle.BLOCK, center, 60, 2, 1, 2, 0, Material.DEEPSLATE.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Emerge with wobble (first 30 ticks)
            if (ticksAlive <= 30) {
                emergeProgress = ticksAlive / 30.0f;
                float yOffset = emergeProgress * 3.0f;
                float wobble = (float) Math.sin(ticksAlive * 0.8) * (1.0f - emergeProgress) * 0.35f;
                float scale = 0.1f + emergeProgress * 0.9f;

                mainDisc.entity().teleport(c.clone().add(0, -3 + yOffset, 0));
                mainDisc.scale(5.0f * scale, 0.4f * scale, 5.0f * scale);
                mainDisc.rotate(0.26f + wobble, 1, 0, 0);
                mainDisc.interpolation(2, 0);
            }

            // Disc Y-axis rotation: 18°/sec = 0.314 rad/sec
            float discRot = ticksAlive * 0.0157f; // ~18°/sec at 20tps
            if (ticksAlive > 30 && mainDisc != null) {
                mainDisc.rotate(discRot, 0, 1, 0);
                mainDisc.interpolation(2, 0);
            }

            // Shrapnel orbit: 8°/sec
            float shrapnelRot = ticksAlive * 0.007f;
            for (int i = 0; i < shrapnel.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 4 + shrapnelRot;
                Location sLoc = c.clone().add(Math.cos(baseAngle) * 2.5, 0.5, Math.sin(baseAngle) * 2.5);
                shrapnel.get(i).entity().teleport(sLoc);
                shrapnel.get(i).rotate(ticksAlive * 0.05f, 0.5f, 1, 0.3f);
                shrapnel.get(i).interpolation(2, 0);
            }

            // Ash particles drifting upward
            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.ASH, c.clone().add(0, 1, 0), 8, 2.5, 0.5, 2.5, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShatterPlate(plugin); }
    }

    // ================================================================
    // 3. OBSIDIAN RIB — Curved arc like a tusk rising from below
    // ================================================================
    public static class ObsidianRib extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arcBlocks = new ArrayList<>();
        private float emergeProgress = 0;

        public ObsidianRib(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_rib", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(700);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Curved arc: obsidian spine with crying obsidian inner face
            for (int i = 0; i < 8; i++) {
                // Arc curve: x = sin(i/7 * PI) * 2, y = i
                double curveX = Math.sin((i / 7.0) * Math.PI) * 2.5;
                Location loc = center.clone().add(curveX, -8 + i, 0);

                Material mat = (i % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.0f, 1.0f, 0.8f).glow(100, 0, 200).interpolation(3, 0);
                arcBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Dark prismarine crown at apex
            Location crownLoc = center.clone().add(0, -1, 0);
            BlockDisplayHandle crown = displayBuilder.spawnBlock(crownLoc, Material.DARK_PRISMARINE);
            crown.scale(0.8f, 0.8f, 0.8f).glow(0, 150, 180).interpolation(3, 0);
            arcBlocks.add(crown);
            spawnedEntities.add(crown.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 35 ticks
            if (ticksAlive <= 35) {
                emergeProgress = ticksAlive / 35.0f;
                float yOffset = emergeProgress * 8.0f;
                for (int i = 0; i < Math.min(arcBlocks.size(), 8); i++) {
                    double curveX = Math.sin((i / 7.0) * Math.PI) * 2.5;
                    Location target = c.clone().add(curveX, -8 + i + yOffset, 0);
                    arcBlocks.get(i).entity().teleport(target);
                }
                if (arcBlocks.size() > 8) {
                    arcBlocks.get(8).entity().teleport(c.clone().add(0, -1 + yOffset + 7, 0));
                }
            }
            // Gentle Z-axis tilt oscillation after rise
            else {
                float tilt = (float) Math.sin(ticksAlive * 0.03) * 0.05f; // ~3° oscillation
                for (BlockDisplayHandle h : arcBlocks) {
                    h.rotate(tilt, 0, 0, 1);
                    h.interpolation(5, 0);
                }
            }

            // Crying obsidian tears cascading down inner face
            if (ticksAlive % 3 == 0 && ticksAlive > 35) {
                for (int i = 1; i < arcBlocks.size() - 1; i += 2) {
                    Location tearLoc = arcBlocks.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tearLoc, 3, 0.2, 0.5, 0.2, 0);
                }
            }
            // Sculk particles at apex
            if (ticksAlive % 10 == 0 && arcBlocks.size() > 8) {
                Location apex = arcBlocks.get(8).entity().getLocation();
                DisplayBuilder.cyanDust(apex, 4, 0.3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianRib(plugin); }
    }

    // ================================================================
    // 4. VOID GEYSER COLUMN — Erupting jet with pulsing cap
    // ================================================================
    public static class VoidGeyserColumn extends BlockDisplayAttack {

        private BlockDisplayHandle baseMouth;
        private final List<BlockDisplayHandle> helixBlocks = new ArrayList<>();
        private BlockDisplayHandle capPlate;
        private int jetPhase = 0;
        private int jetCount = 0;

        public VoidGeyserColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_geyser_column", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base mouth: 3x3 black concrete
            baseMouth = displayBuilder.spawnBlock(center, Material.BLACK_CONCRETE);
            baseMouth.scale(3.0f, 0.5f, 3.0f).glow(30, 0, 60).interpolation(2, 0);
            spawnedEntities.add(baseMouth.entity());

            // Helix column: obsidian blocks in broken spiral
            for (int level = 0; level < 5; level++) {
                for (int side = 0; side < 2; side++) {
                    double angle = (level * 0.6) + (side * Math.PI);
                    double x = Math.cos(angle) * 0.6;
                    double z = Math.sin(angle) * 0.6;
                    Location loc = center.clone().add(x, 1 + level, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.6f, 0.9f, 0.6f).glow(60, 0, 120).interpolation(2, 0);
                    helixBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Cap plate: polished blackstone
            capPlate = displayBuilder.spawnBlock(center.clone().add(0, 6, 0), Material.POLISHED_BLACKSTONE);
            capPlate.scale(2.0f, 0.4f, 2.0f).glow(50, 50, 70).interpolation(2, 0);
            spawnedEntities.add(capPlate.entity());

            // Amethyst glow sources inside helix
            int[] glowLevels = {1, 3, 5};
            for (int y : glowLevels) {
                BlockDisplayHandle glow = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.AMETHYST_BLOCK);
                glow.scale(0.4f, 0.4f, 0.4f).glow(180, 80, 255).interpolation(2, 0);
                spawnedEntities.add(glow.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Build phase: static for 15 ticks
            if (ticksAlive < 15) return;

            // Shudder phase: ticks 15-25
            if (ticksAlive >= 15 && ticksAlive < 25) {
                float shudder = ((ticksAlive % 2 == 0) ? 0.3f : -0.3f);
                for (BlockDisplayHandle h : helixBlocks) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().add(0, shudder, 0));
                }
            }

            // Jet phase: cap shoots up, reverse portal particles
            if (ticksAlive >= 25 && jetCount < 3) {
                jetPhase++;
                int cyclePos = jetPhase % 28; // 8 ticks up, 20 ticks down
                if (cyclePos == 0) {
                    jetCount++;
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.5f);
                    // Trigger impact damage during jet
                    triggerImpactDamage(c.clone().add(0, 3, 0));
                }
                float capY;
                if (cyclePos < 8) {
                    capY = 6.0f + (cyclePos / 8.0f) * 5.0f;
                } else {
                    capY = 11.0f - ((cyclePos - 8) / 20.0f) * 5.0f;
                }
                capPlate.entity().teleport(c.clone().add(0, capY, 0));

                // Dense upward particle jet
                if (cyclePos < 8) {
                    for (int p = 0; p < 20; p++) {
                        double py = 1 + Math.random() * 8;
                        c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(
                                (Math.random() - 0.5) * 0.8, py, (Math.random() - 0.5) * 0.8
                        ), 1, 0, 0, 0, 0);
                    }
                }
            }

            // Soul fire at base continuously
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.particleRing(c, 1.5, Particle.SOUL_FIRE_FLAME, 8, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidGeyserColumn(plugin); }
    }

    // ================================================================
    // 5. BLACKSTONE THORN CLUSTER — 5 spikes rising staggered
    // ================================================================
    public static class BlackstoneThorns extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> rubble = new ArrayList<>();
        private final int[] spikeHeights = {6, 5, 4, 4, 3};
        private final int[] spikeStartTicks = {0, 6, 12, 18, 24};

        public BlackstoneThorns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blackstone_thorns", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 spikes at radial positions
            double[][] spikePositions = {{0,0},{1.5,0},{-1,1},{-1,-1},{0.5,-1.5}};
            for (int s = 0; s < 5; s++) {
                List<BlockDisplayHandle> spike = new ArrayList<>();
                for (int y = 0; y < spikeHeights[s]; y++) {
                    float taper = 1.0f - (y * 0.15f);
                    Location loc = center.clone().add(spikePositions[s][0], -6 + y, spikePositions[s][1]);
                    Material mat = (y == spikeHeights[s] - 1) ? Material.POLISHED_BLACKSTONE : Material.BLACKSTONE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(taper, 1.0f, taper).glow(50, 50, 60).interpolation(2, 0);
                    spike.add(h);
                    spawnedEntities.add(h.entity());
                }
                spikes.add(spike);
            }

            // 6 rubble pieces orbiting base
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location rLoc = center.clone().add(Math.cos(angle) * 3, 0.2, Math.sin(angle) * 3);
                BlockDisplayHandle r = displayBuilder.spawnBlock(rLoc, Material.COBBLED_DEEPSLATE);
                r.scale(0.4f, 0.3f, 0.4f).glow(40, 40, 50).interpolation(2, 0);
                rubble.add(r);
                spawnedEntities.add(r.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[][] spikePositions = {{0,0},{1.5,0},{-1,1},{-1,-1},{0.5,-1.5}};

            // Staggered spike rise
            for (int s = 0; s < 5; s++) {
                int spikeAge = ticksAlive - spikeStartTicks[s];
                if (spikeAge < 0 || spikeAge > 20) continue;

                float progress = spikeAge / 20.0f;
                float yOffset = progress * 6.0f;
                List<BlockDisplayHandle> spike = spikes.get(s);
                for (int y = 0; y < spike.size(); y++) {
                    Location target = c.clone().add(spikePositions[s][0], -6 + y + yOffset, spikePositions[s][1]);
                    spike.get(y).entity().teleport(target);
                }

                // Rise particles
                if (spikeAge % 4 == 0) {
                    Location base = c.clone().add(spikePositions[s][0], yOffset, spikePositions[s][1]);
                    c.getWorld().spawnParticle(Particle.BLOCK, base, 20, 0.3, 0.3, 0.3, 0,
                            Material.BLACKSTONE.createBlockData());
                }

                // Sound per spike
                if (spikeAge == 0) {
                    DisplayBuilder.playSound(c.clone().add(spikePositions[s][0], 0, spikePositions[s][1]),
                            Sound.BLOCK_STONE_BREAK, 1.0f, 0.9f);
                }
            }

            // Group Y rotation after all spikes up (tick 44+)
            if (ticksAlive > 44) {
                float groupRot = (ticksAlive - 44) * 0.00524f; // 6°/sec
                // Rubble orbit: 4°/sec
                float rubbleRot = (ticksAlive - 44) * 0.00349f;
                for (int i = 0; i < rubble.size(); i++) {
                    double baseAngle = (Math.PI * 2 * i) / 6 + rubbleRot;
                    Location rLoc = c.clone().add(Math.cos(baseAngle) * 3, 0.2, Math.sin(baseAngle) * 3);
                    rubble.get(i).entity().teleport(rLoc);
                }
            }

            // Purple dust tracing spikes
            if (ticksAlive % 8 == 0 && ticksAlive > 44) {
                for (int s = 0; s < spikes.size(); s++) {
                    if (!spikes.get(s).isEmpty()) {
                        Location top = spikes.get(s).get(spikes.get(s).size() - 1).entity().getLocation();
                        DisplayBuilder.darkPurpleDust(top, 3, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlackstoneThorns(plugin); }
    }

    // ================================================================
    // 6. SOUL VENT MOUND — Dome with pulsing vent pipes
    // ================================================================
    public static class SoulVentMound extends BlockDisplayAttack {

        private BlockDisplayHandle dome;
        private final List<BlockDisplayHandle> ventPipes = new ArrayList<>();
        private BlockDisplayHandle apexCluster;

        public SoulVentMound(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_vent_mound", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main dome: soul soil
            dome = displayBuilder.spawnBlock(center.clone().add(0, -3, 0), Material.SOUL_SOIL);
            dome.scale(5.0f, 3.0f, 5.0f).glow(60, 40, 30).interpolation(2, 0);
            spawnedEntities.add(dome.entity());

            // Blackstone border ring
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.8, -3, Math.sin(angle) * 2.8);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                b.scale(0.8f, 0.6f, 0.8f).interpolation(2, 0);
                spawnedEntities.add(b.entity());
            }

            // 6 obsidian vent pipes
            double[] ventAngles = {0, Math.PI/3, 2*Math.PI/3, Math.PI, 4*Math.PI/3, 5*Math.PI/3};
            for (double angle : ventAngles) {
                Location loc = center.clone().add(Math.cos(angle) * 1.5, -2, Math.sin(angle) * 1.5);
                BlockDisplayHandle v = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                v.scale(0.4f, 1.2f, 0.4f).glow(40, 0, 80).interpolation(2, 0);
                ventPipes.add(v);
                spawnedEntities.add(v.entity());
            }

            // Amethyst apex
            apexCluster = displayBuilder.spawnBlock(center.clone().add(0, -0.5, 0), Material.AMETHYST_BLOCK);
            apexCluster.scale(1.0f, 0.6f, 1.0f).glow(180, 80, 255).interpolation(2, 0);
            spawnedEntities.add(apexCluster.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Dome rise over 20 ticks
            if (ticksAlive <= 20) {
                float progress = ticksAlive / 20.0f;
                float yOffset = progress * 3.0f;
                dome.entity().teleport(c.clone().add(0, -3 + yOffset, 0));
            }

            // Vent pipe pulsing: staggered ±0.1 block upward
            for (int i = 0; i < ventPipes.size(); i++) {
                float pulse = (float) Math.sin((ticksAlive + i * 3) * 0.6) * 0.1f;
                BlockDisplay bd = ventPipes.get(i).entity();
                Location base = bd.getLocation();
                // Small Y translation pulse
                ventPipes.get(i).translate(-0.2f, pulse, -0.2f);
                ventPipes.get(i).interpolation(2, 0);
            }

            // Apex bob: sine wave
            float apexBob = (float) Math.sin(ticksAlive * 0.157) * 0.2f; // 40-tick cycle
            apexCluster.entity().teleport(c.clone().add(0, 0.5 + apexBob, 0));

            // Soul fire from each vent
            if (ticksAlive % 4 == 0 && ticksAlive > 20) {
                double[] ventAngles = {0, Math.PI/3, 2*Math.PI/3, Math.PI, 4*Math.PI/3, 5*Math.PI/3};
                for (double angle : ventAngles) {
                    Location vLoc = c.clone().add(Math.cos(angle) * 1.5, 1.5, Math.sin(angle) * 1.5);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, vLoc, 4, 0.05, 0.3, 0.05, 0.02);
                }
            }

            // Sculk soul drifting outward
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(c.clone().add(0, 1, 0), 5, 2.5);
            }

            // Ambient sound
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulVentMound(plugin); }
    }

    // ================================================================
    // 7. NETHERITE MONOLITH — Dense slow-rising rectangular slab
    // ================================================================
    public static class NetheriteMonolith extends BlockDisplayAttack {

        private BlockDisplayHandle core;
        private BlockDisplayHandle cladding;
        private BlockDisplayHandle crown;
        private BlockDisplayHandle weepingBelt;
        private final List<BlockDisplayHandle> plinths = new ArrayList<>();

        public NetheriteMonolith(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("netherite_monolith", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Core: 2x6x2 netherite
            core = displayBuilder.spawnBlock(center.clone().add(0, -7, 0), Material.NETHERITE_BLOCK);
            core.scale(2.0f, 6.0f, 2.0f).glow(20, 20, 30).interpolation(5, 0);
            spawnedEntities.add(core.entity());

            // Obsidian cladding sides
            cladding = displayBuilder.spawnBlock(center.clone().add(1.1, -7, 0), Material.OBSIDIAN);
            cladding.scale(0.3f, 5.5f, 2.2f).glow(40, 0, 60).interpolation(5, 0);
            spawnedEntities.add(cladding.entity());

            // Crown: black concrete cap
            crown = displayBuilder.spawnBlock(center.clone().add(0, -1.5, 0), Material.BLACK_CONCRETE);
            crown.scale(2.2f, 0.8f, 2.2f).glow(20, 20, 25).interpolation(5, 0);
            spawnedEntities.add(crown.entity());

            // Crying obsidian belt at mid-height
            weepingBelt = displayBuilder.spawnBlock(center.clone().add(0, -4, 0), Material.CRYING_OBSIDIAN);
            weepingBelt.scale(2.4f, 0.5f, 2.4f).glow(128, 0, 255).interpolation(5, 0);
            spawnedEntities.add(weepingBelt.entity());

            // Corner plinths
            double[][] corners = {{1,1},{1,-1},{-1,1},{-1,-1}};
            for (double[] corner : corners) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(
                        center.clone().add(corner[0], -7, corner[1]), Material.POLISHED_BLACKSTONE);
                p.scale(0.6f, 0.5f, 0.6f).interpolation(5, 0);
                plinths.add(p);
                spawnedEntities.add(p.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.0f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow rise: 60 ticks, 7 blocks
            if (ticksAlive <= 60) {
                float progress = ticksAlive / 60.0f;
                float yOffset = progress * 7.0f;
                core.entity().teleport(c.clone().add(0, -7 + yOffset, 0));
                cladding.entity().teleport(c.clone().add(1.1, -7 + yOffset, 0));
                crown.entity().teleport(c.clone().add(0, -1.5 + yOffset, 0));
                weepingBelt.entity().teleport(c.clone().add(0, -4 + yOffset, 0));
                double[][] corners = {{1,1},{1,-1},{-1,1},{-1,-1}};
                for (int i = 0; i < plinths.size(); i++) {
                    plinths.get(i).entity().teleport(c.clone().add(corners[i][0], -7 + yOffset, corners[i][1]));
                }
            }

            // Breathing scale: 1.0 → 1.06, 100-tick cycle
            if (ticksAlive > 60) {
                float breathe = 1.0f + (float) Math.sin(ticksAlive * 0.0628) * 0.03f;
                core.scale(2.0f * breathe, 6.0f, 2.0f * breathe);
                core.interpolation(5, 0);
            }

            // Crying obsidian tears from belt
            if (ticksAlive % 3 == 0 && ticksAlive > 60) {
                Location beltLoc = weepingBelt.entity().getLocation().add(0, 0.3, 0);
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, beltLoc, 4, 1.2, 0, 1.2, 0);
            }

            // Reverse portal at base
            if (ticksAlive % 5 == 0 && ticksAlive > 60) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 0.3, 0), 5, 1, 0.2, 1, 0.01);
            }

            // Large smoke from crown
            if (ticksAlive % 8 == 0 && ticksAlive > 60) {
                Location crownLoc = crown.entity().getLocation().add(0, 1, 0);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, crownLoc, 2, 0.3, 0.2, 0.3, 0.01);
            }

            // Infrasound pulse every 12 seconds
            if (ticksAlive % 240 == 120) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.3f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetheriteMonolith(plugin); }
    }

    // ================================================================
    // 8. CRACKED VOID EYE — Flat oval disc with amethyst iris
    // ================================================================
    public static class CrackedVoidEye extends BlockDisplayAttack {

        private BlockDisplayHandle sclera;
        private BlockDisplayHandle iris;
        private BlockDisplayHandle pupil;
        private final List<BlockDisplayHandle> tearDucts = new ArrayList<>();

        public CrackedVoidEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cracked_void_eye", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Sclera: obsidian oval disc
            sclera = displayBuilder.spawnBlock(center.clone().add(0, -5, 0), Material.OBSIDIAN);
            sclera.scale(5.0f, 0.5f, 3.0f).glow(40, 0, 60).interpolation(3, 0);
            spawnedEntities.add(sclera.entity());

            // Iris: amethyst center
            iris = displayBuilder.spawnBlock(center.clone().add(0, -4.7, 0), Material.AMETHYST_BLOCK);
            iris.scale(2.0f, 0.6f, 1.5f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(iris.entity());

            // Pupil: purple stained glass hovering above iris
            pupil = displayBuilder.spawnBlock(center.clone().add(0, -4.4, 0), Material.PURPLE_STAINED_GLASS);
            pupil.scale(1.0f, 0.3f, 0.8f).glow(160, 0, 200).interpolation(3, 0);
            spawnedEntities.add(pupil.entity());

            // Tear ducts: 4 crying obsidian at cardinal edges
            double[][] ductPos = {{2.5,0},{-2.5,0},{0,1.5},{0,-1.5}};
            for (double[] pos : ductPos) {
                BlockDisplayHandle d = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], -5, pos[1]), Material.CRYING_OBSIDIAN);
                d.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                tearDucts.add(d);
                spawnedEntities.add(d.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 30 ticks
            if (ticksAlive <= 30) {
                float progress = ticksAlive / 30.0f;
                float yOffset = progress * 7.0f;
                sclera.entity().teleport(c.clone().add(0, -5 + yOffset, 0));
                iris.entity().teleport(c.clone().add(0, -4.7 + yOffset, 0));
                pupil.entity().teleport(c.clone().add(0, -4.4 + yOffset, 0));
                double[][] ductPos = {{2.5,0},{-2.5,0},{0,1.5},{0,-1.5}};
                for (int i = 0; i < tearDucts.size(); i++) {
                    tearDucts.get(i).entity().teleport(c.clone().add(ductPos[i][0], -5 + yOffset, ductPos[i][1]));
                }
            }

            // Slow Y rotation: 3°/sec
            float eyeRot = ticksAlive * 0.00262f;
            sclera.rotate(eyeRot, 0, 1, 0);
            sclera.interpolation(3, 0);

            // Iris bob: ±0.15 blocks, 25-tick cycle
            if (ticksAlive > 30) {
                float irisBob = (float) Math.sin(ticksAlive * 0.251) * 0.15f;
                iris.entity().teleport(c.clone().add(0, 2.3 + irisBob, 0));
            }

            // Pupil counter-rotation: 5°/sec
            float pupilRot = -ticksAlive * 0.00436f;
            pupil.rotate(pupilRot, 0, 1, 0);
            pupil.interpolation(3, 0);

            // Enchant particles from iris
            if (ticksAlive % 3 == 0 && ticksAlive > 30) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2.5, 0), 8, 0.8, 0.3, 0.8, 0.5);
            }

            // Tears from ducts
            if (ticksAlive % 4 == 0 && ticksAlive > 30) {
                for (BlockDisplayHandle duct : tearDucts) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, duct.entity().getLocation(), 2, 0.1, 0.3, 0.1, 0);
                }
            }

            // Bright purple halo at perimeter
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 2, 0), 3.0, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(155, 48, 255), 1.5f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrackedVoidEye(plugin); }
    }

    // ================================================================
    // 9. ABYSS STAKE — Fast hexagonal spike that tilts toward player
    // ================================================================
    public static class AbyssStake extends BlockDisplayAttack {

        private BlockDisplayHandle mainStake;
        private final List<BlockDisplayHandle> flanges = new ArrayList<>();
        private BlockDisplayHandle weepingTip;

        public AbyssStake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyss_stake", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main stake: tall narrow blackstone pillar
            mainStake = displayBuilder.spawnBlock(center.clone().add(0, -8, 0), Material.BLACKSTONE);
            mainStake.scale(1.2f, 8.0f, 1.2f).glow(50, 50, 60).interpolation(1, 0);
            spawnedEntities.add(mainStake.entity());

            // Polished blackstone base band
            BlockDisplayHandle baseBand = displayBuilder.spawnBlock(center.clone().add(0, -8, 0), Material.POLISHED_BLACKSTONE);
            baseBand.scale(1.5f, 3.0f, 1.5f).glow(60, 60, 70).interpolation(1, 0);
            spawnedEntities.add(baseBand.entity());

            // Mid-height flanges: cobbled deepslate pointing outward
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                Location fLoc = center.clone().add(Math.cos(angle) * 0.8, -4, Math.sin(angle) * 0.8);
                BlockDisplayHandle f = displayBuilder.spawnBlock(fLoc, Material.COBBLED_DEEPSLATE);
                f.scale(0.5f, 0.5f, 0.3f).rotate((float)(angle), 0, 1, 0).interpolation(1, 0);
                flanges.add(f);
                spawnedEntities.add(f.entity());
            }

            // Crying obsidian tip
            weepingTip = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.CRYING_OBSIDIAN);
            weepingTip.scale(0.4f, 0.6f, 0.4f).glow(200, 0, 255).interpolation(1, 0);
            spawnedEntities.add(weepingTip.entity());

            // FAST emergence sound
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // FAST emergence: 15 ticks
            if (ticksAlive <= 15) {
                float progress = ticksAlive / 15.0f;
                float yOffset = progress * 8.0f;
                mainStake.entity().teleport(c.clone().add(0, -8 + yOffset, 0));
                weepingTip.entity().teleport(c.clone().add(0, yOffset, 0));

                // Shockwave ring on complete
                if (ticksAlive == 15) {
                    c.getWorld().spawnParticle(Particle.BLOCK, c, 80, 3, 0.2, 3, 0,
                            Material.BLACKSTONE.createBlockData());
                    triggerImpactDamage(c);
                }
            }

            // Tilt toward spawn point: 8° on X or Z
            if (ticksAlive > 15) {
                float tilt = 0.14f; // ~8°
                float oscillate = (float) Math.sin(ticksAlive * 0.419) * 0.0175f; // ±1° at 15-tick cycle
                mainStake.rotate(tilt + oscillate, 1, 0, 0);
                mainStake.interpolation(5, 0);
            }

            // Weeping tip particles
            if (ticksAlive % 3 == 0 && ticksAlive > 15) {
                Location tipLoc = weepingTip.entity().getLocation();
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tipLoc, 2, 0.1, 0.2, 0.1, 0);
                DisplayBuilder.purpleDust(tipLoc, 2, 0.2);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssStake(plugin); }
    }

    // ================================================================
    // 10. FRACTURED MAW TOOTH — Massive curved fang with secondary tooth
    // ================================================================
    public static class FracturedMawTooth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mainFang = new ArrayList<>();
        private final List<BlockDisplayHandle> secondaryFang = new ArrayList<>();
        private BlockDisplayHandle rootBase;
        private BlockDisplayHandle fractureFill;

        public FracturedMawTooth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fractured_maw_tooth", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Root base: 4x2 obsidian splayed wide
            rootBase = displayBuilder.spawnBlock(center.clone().add(0, -12, 0), Material.OBSIDIAN);
            rootBase.scale(4.0f, 2.0f, 2.0f).glow(40, 0, 60).interpolation(3, 0);
            spawnedEntities.add(rootBase.entity());

            // Primary fang: curved arc of obsidian + crying obsidian
            for (int i = 0; i < 12; i++) {
                // Curve: forward lean with height
                double curveForward = Math.sin((i / 11.0) * Math.PI * 0.6) * 3.0;
                Location loc = center.clone().add(curveForward, -12 + i, 0);
                Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                float taper = 1.0f - (i * 0.06f);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(taper, 1.0f, taper * 0.8f).glow(80, 0, 160).interpolation(3, 0);
                mainFang.add(h);
                spawnedEntities.add(h.entity());
            }

            // Secondary fang: diverging at 30° from base, 5 blocks
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(30);
                double x = Math.sin(angle) * i * 0.8;
                Location loc = center.clone().add(-x, -12 + i, x * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float taper = 0.8f - (i * 0.12f);
                h.scale(taper, 1.0f, taper).glow(60, 0, 120).interpolation(3, 0);
                secondaryFang.add(h);
                spawnedEntities.add(h.entity());
            }

            // Amethyst fracture fill
            fractureFill = displayBuilder.spawnBlock(center.clone().add(1, -6, 0), Material.AMETHYST_BLOCK);
            fractureFill.scale(0.6f, 1.5f, 0.4f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(fractureFill.entity());

            // Purple glass behind fracture
            BlockDisplayHandle glass = displayBuilder.spawnBlock(center.clone().add(1.2, -6, 0), Material.PURPLE_STAINED_GLASS);
            glass.scale(0.3f, 1.2f, 0.3f).glow(160, 0, 200).interpolation(3, 0);
            spawnedEntities.add(glass.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Twisting emergence: root first (0-20), then spine (20-50)
            if (ticksAlive <= 50) {
                float rootProgress = Math.min(1.0f, ticksAlive / 20.0f);
                float spineProgress = Math.max(0, (ticksAlive - 20) / 30.0f);
                float yRoot = rootProgress * 12.0f;
                float ySpine = spineProgress * 12.0f;

                rootBase.entity().teleport(c.clone().add(0, -12 + yRoot, 0));

                for (int i = 0; i < mainFang.size(); i++) {
                    float blockProgress = Math.min(1.0f, spineProgress * mainFang.size() / (i + 1));
                    double curveForward = Math.sin((i / 11.0) * Math.PI * 0.6) * 3.0;
                    float y = -12 + i + (yRoot * 0.3f) + (ySpine * 0.7f * blockProgress);
                    mainFang.get(i).entity().teleport(c.clone().add(curveForward, y, 0));
                }

                // Secondary fang branches off during spine rise
                float secProgress = Math.max(0, (ticksAlive - 30) / 20.0f);
                for (int i = 0; i < secondaryFang.size(); i++) {
                    double angle = Math.toRadians(30);
                    double x = Math.sin(angle) * i * 0.8;
                    float y = -12 + i + (yRoot * 0.3f) + (secProgress * 5.0f * Math.min(1, (float)i/4));
                    secondaryFang.get(i).entity().teleport(c.clone().add(-x, y, x * 0.5));
                }
            }

            // Y-axis rotation: 4°/sec after full extension
            if (ticksAlive > 50) {
                float rot = (ticksAlive - 50) * 0.00349f;
                for (BlockDisplayHandle h : mainFang) {
                    h.rotate(rot, 0, 1, 0);
                    h.interpolation(5, 0);
                }

                // Secondary fang oscillation: ±5° on branch axis
                float secOscillate = (float) Math.sin(ticksAlive * 0.209) * 0.0873f; // ±5°, 30-tick cycle
                for (BlockDisplayHandle h : secondaryFang) {
                    h.rotate(secOscillate + rot, 0, 1, 0);
                    h.interpolation(5, 0);
                }
            }

            // Particles: tears down concave face
            if (ticksAlive % 3 == 0 && ticksAlive > 50) {
                for (int i = 0; i < mainFang.size(); i += 3) {
                    Location fLoc = mainFang.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, fLoc, 3, 0.3, 0.5, 0.3, 0);
                }
            }

            // Reverse portal at breach point
            if (ticksAlive % 5 == 0 && ticksAlive > 20) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 0.5, 0), 10, 1, 0.3, 1, 0.02);
            }

            // Black dust cloud at base
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.darkPurpleDust(c.clone().add(0, 0.3, 0), 5, 2.0);
            }

            // Soul fire tips every 5 seconds
            if (ticksAlive % 100 == 0 && ticksAlive > 50) {
                if (!mainFang.isEmpty()) {
                    Location tipLoc = mainFang.get(mainFang.size() - 1).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, tipLoc, 6, 0.2, 0.2, 0.2, 0.02);
                }
                if (!secondaryFang.isEmpty()) {
                    Location secTip = secondaryFang.get(secondaryFang.size() - 1).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, secTip, 4, 0.15, 0.15, 0.15, 0.02);
                }
            }

            // Sculk spread sound every 3 seconds
            if (ticksAlive % 60 == 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FracturedMawTooth(plugin); }
    }
}
