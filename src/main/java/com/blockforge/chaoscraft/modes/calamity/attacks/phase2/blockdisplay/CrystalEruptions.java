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
 * Phase 2 (DoG) Block Display -- GROUP 1: CRYSTAL ERUPTIONS
 * 10 structures: crystalline growths erupting from the arena floor.
 * Devourer of Gods palette: amethyst, cyan, dark prismarine, polished blackstone.
 *
 * Design rules:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG glow colors: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 * - All values configurable via AttackConfig
 */
public final class CrystalEruptions {

    private CrystalEruptions() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrownSpikeArray(plugin));
        registry.register(new GeodeVent(plugin));
        registry.register(new ShatterFan(plugin));
        registry.register(new DeepBore(plugin));
        registry.register(new CrystalSpineRidge(plugin));
        registry.register(new BloomNode(plugin));
        registry.register(new ShatteredPillar(plugin));
        registry.register(new BioluminescentPatch(plugin));
        registry.register(new CrystalFountain(plugin));
        registry.register(new RootTangle(plugin));
    }

    // ================================================================
    // 1. CROWN SPIKE ARRAY -- Radial starburst of amethyst spikes
    // ================================================================
    public static class CrownSpikeArray extends BlockDisplayAttack {

        private BlockDisplayHandle centerBlock;
        private final List<BlockDisplayHandle> horizontalSpikes = new ArrayList<>();
        private final List<BlockDisplayHandle> diagonalSpikes = new ArrayList<>();
        private final List<BlockDisplayHandle> baseRing = new ArrayList<>();
        private BlockDisplayHandle lightSource;

        public CrownSpikeArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crown_spike_array", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central amethyst block
            centerBlock = displayBuilder.spawnBlock(center, Material.AMETHYST_BLOCK);
            centerBlock.scale(1f, 1f, 1f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(centerBlock.entity());

            // 8 horizontal spikes radiating outward
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                for (int seg = 1; seg <= 3; seg++) {
                    double dist = seg * 0.8;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    Material mat = seg < 3 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_CLUSTER;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    float taper = 1.0f - (seg * 0.15f);
                    h.scale(taper, 0.6f, taper).glow(0, 200, 255).interpolation(2, 0);
                    horizontalSpikes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 8 diagonal spikes rising between horizontal ones
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 + (Math.PI / 8);
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 1.0, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.7f, 0.8f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
                diagonalSpikes.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 budding amethyst base ring
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 0, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BUDDING_AMETHYST);
                h.scale(0.8f, 0.5f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
                baseRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Sea lantern below center
            lightSource = displayBuilder.spawnBlock(center.clone().add(0, -1, 0), Material.SEA_LANTERN);
            lightSource.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(lightSource.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
            DisplayBuilder.cyanDust(center, 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Entire structure rotates slowly: 1 degree per tick
            float rotAngle = ticksAlive * 0.0175f;

            // Breathing pulse: scale 1.0 to 1.15 over 40 ticks every 80 ticks
            float breathCycle = (ticksAlive % 80) / 80.0f;
            float breathScale = 1.0f;
            if (breathCycle < 0.5f) {
                breathScale = 1.0f + 0.15f * (float) Math.sin(breathCycle * Math.PI * 2);
            }

            // Rotate center block
            if (centerBlock != null) {
                BlockDisplay bd = centerBlock.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(rotAngle, 0, 1, 0),
                        new Vector3f(breathScale, breathScale, breathScale),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Diagonal spike tips oscillate up/down on sine wave
            for (int i = 0; i < diagonalSpikes.size(); i++) {
                float yOff = 0.3f * (float) Math.sin((ticksAlive + i * 7) * 0.105f);
                BlockDisplay bd = diagonalSpikes.get(i).entity();
                double angle = (Math.PI * 2 * i) / 8 + (Math.PI / 8) + rotAngle;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 1.0 + yOff, Math.sin(angle) * 1.2);
                bd.teleport(loc);
            }

            // Cycling electric spark arcs between base ring blocks
            if (ticksAlive % 15 == 0) {
                int idx = (ticksAlive / 15) % baseRing.size();
                int next = (idx + 1) % baseRing.size();
                Location from = baseRing.get(idx).entity().getLocation().add(0, 0.5, 0);
                Location to = baseRing.get(next).entity().getLocation().add(0, 0.5, 0);
                for (int p = 0; p < 6; p++) {
                    double t = p / 5.0;
                    Location particle = from.clone().add(
                            (to.getX() - from.getX()) * t,
                            (to.getY() - from.getY()) * t + 0.2,
                            (to.getZ() - from.getZ()) * t
                    );
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                }
            }

            // Cyan dust rising from center
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 3, 0.5);
            }

            // Sound loop every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownSpikeArray(plugin); }
    }

    // ================================================================
    // 2. GEODE VENT -- Vertical amethyst cylinder erupting from base
    // ================================================================
    public static class GeodeVent extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tubeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crownClusters = new ArrayList<>();
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private float sinkProgress = 0;

        public GeodeVent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("geode_vent", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3x3 hollow tube, 4 blocks tall (12 amethyst blocks in ring pattern)
            for (int y = 0; y < 4; y++) {
                for (int i = 0; i < 3; i++) {
                    double angle = (Math.PI * 2 * i) / 3;
                    Location loc = center.clone().add(Math.cos(angle) * 1.2, y, Math.sin(angle) * 1.2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                    h.scale(0.9f, 1.0f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                    tubeBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Sea lanterns inside tube at Y+0 and Y+4
            for (int y : new int[]{0, 4}) {
                BlockDisplayHandle sl = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.SEA_LANTERN);
                sl.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(sl.entity());
            }

            // Crown: 8 large amethyst clusters at top, varying heights
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                float height = 4.0f + ((i % 3) * 0.5f);
                Location loc = center.clone().add(Math.cos(angle) * 1.0, height, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.6f, 0.7f + (i % 3) * 0.2f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                crownClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // 5x5 base of alternating budding amethyst and dark prismarine
            int baseIdx = 0;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue; // Skip inner
                    Material mat = (baseIdx % 2 == 0) ? Material.BUDDING_AMETHYST : Material.DARK_PRISMARINE;
                    Location loc = center.clone().add(x, -0.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.9f, 0.5f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                    baseBlocks.add(h);
                    spawnedEntities.add(h.entity());
                    baseIdx++;
                }
            }

            // 4 end rods at cardinal points on base perimeter
            double[][] cardinals = {{2.5, 0, 0}, {-2.5, 0, 0}, {0, 0, 2.5}, {0, 0, -2.5}};
            for (double[] off : cardinals) {
                BlockDisplayHandle rod = displayBuilder.spawnBlock(center.clone().add(off[0], 0, off[2]), Material.END_ROD);
                rod.scale(0.3f, 1.0f, 0.3f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.5f);
            DisplayBuilder.purpleDust(center, 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sink and snap: sinks 0.2 blocks over 100 ticks, snaps back in 10
            int sinkCycle = ticksAlive % 110;
            float sinkOffset = 0;
            if (sinkCycle < 100) {
                sinkOffset = -0.2f * (sinkCycle / 100.0f);
            }

            // Crown clusters oscillate independently on staggered 45-tick cycles
            for (int i = 0; i < crownClusters.size(); i++) {
                float yOff = 0.15f * (float) Math.sin((ticksAlive + i * 6) * (2 * Math.PI / 45));
                BlockDisplay bd = crownClusters.get(i).entity();
                double angle = (Math.PI * 2 * i) / 8;
                float height = 4.0f + ((i % 3) * 0.5f) + yOff + sinkOffset;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, height, Math.sin(angle) * 1.0);
                bd.teleport(loc);
            }

            // Portal particles erupting from open top
            if (ticksAlive % 2 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(0, 4.5, 0),
                        5, 0.3, 0.2, 0.3, 0.3);
            }

            // Cyan dust transition at base perimeter
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0, 0), 4, 2.5);
            }

            // Sound loop every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GeodeVent(plugin); }
    }

    // ================================================================
    // 3. SHATTER FAN -- Semicircular fan of crystal shards
    // ================================================================
    public static class ShatterFan extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fanBlades = new ArrayList<>();
        private final List<BlockDisplayHandle> glassBacking = new ArrayList<>();
        private BlockDisplayHandle basePlinth;

        public ShatterFan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shatter_fan", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base plinth: 3x1 polished blackstone
            basePlinth = displayBuilder.spawnBlock(center, Material.POLISHED_BLACKSTONE);
            basePlinth.scale(3.0f, 0.5f, 1.0f).glow(80, 80, 100).interpolation(2, 0);
            spawnedEntities.add(basePlinth.entity());

            // 15 crystal fan blades in semicircle
            for (int i = 0; i < 15; i++) {
                double angle = (Math.PI * i) / 14 - (Math.PI / 2);
                double x = Math.cos(angle) * 2.0;
                double y = 0.5 + Math.sin(angle) * 2.0;
                Location loc = center.clone().add(x, y, 0);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_CLUSTER : Material.AMETHYST_CLUSTER;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.5f, 0.6f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                fanBlades.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cyan glass backing -- 6 blocks behind the fan
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * (i * 2 + 1)) / 14 - (Math.PI / 2);
                double x = Math.cos(angle) * 2.0;
                double y = 0.5 + Math.sin(angle) * 2.0;
                Location loc = center.clone().add(x, y, -0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.6f, 0.6f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                glassBacking.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fan oscillation: 15 degrees left/right on 70-tick cycle
            float swing = 15.0f * (float) Math.sin(ticksAlive * (2 * Math.PI / 70));
            float swingRad = (float) Math.toRadians(swing);

            // Scale pulse: 1.0 to 1.08 on 120-tick cycle
            float scalePulse = 1.0f + 0.08f * (float) Math.sin(ticksAlive * (2 * Math.PI / 120));

            // Animate fan blades (oscillate, glass stays put)
            for (int i = 0; i < fanBlades.size(); i++) {
                BlockDisplay bd = fanBlades.get(i).entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.25f, -0.3f, -0.25f),
                        new AxisAngle4f(swingRad, 0, 0, 1),
                        new Vector3f(0.5f * scalePulse, 0.6f * scalePulse, 0.5f * scalePulse),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Electric spark arcs along outer edge of fan
            if (ticksAlive % 20 == 0 && fanBlades.size() >= 2) {
                int idx = (ticksAlive / 20) % (fanBlades.size() - 1);
                Location from = fanBlades.get(idx).entity().getLocation();
                Location to = fanBlades.get(idx + 1).entity().getLocation();
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

            // Purple dust falling from upper edge
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 2.5, 0), 3, 1.5);
            }

            // Sound every 80 ticks
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShatterFan(plugin); }
    }

    // ================================================================
    // 4. DEEP BORE -- Sunken depression ringed with crystal
    // ================================================================
    public static class DeepBore extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> bottomClusters = new ArrayList<>();
        private BlockDisplayHandle bottomLantern;
        private final List<BlockDisplayHandle> endRods = new ArrayList<>();

        public DeepBore(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deep_bore", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 7x7 outer ring of dark prismarine at ground level
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(1.0f, 0.5f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner ring: 8 budding amethyst at Y-1
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.0, -1, Math.sin(angle) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BUDDING_AMETHYST);
                h.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 large amethyst clusters at Y-2 pointing inward
            double[][] cardinals = {{1.2, 0}, {-1.2, 0}, {0, 1.2}, {0, -1.2}};
            for (double[] off : cardinals) {
                Location loc = center.clone().add(off[0], -2, off[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.7f, 0.7f, 0.7f).glow(0, 200, 255).interpolation(2, 0);
                bottomClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // Sea lantern at Y-3 (bottom)
            bottomLantern = displayBuilder.spawnBlock(center.clone().add(0, -3, 0), Material.SEA_LANTERN);
            bottomLantern.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(bottomLantern.entity());

            // 4 large amethyst clusters at Y-3 pointing up
            for (double[] off : cardinals) {
                Location loc = center.clone().add(off[0], -3, off[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.8f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // 8 end rods at outer perimeter pointing skyward
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 0.5, Math.sin(angle) * 3.5);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.2f, 1.5f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                endRods.add(rod);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.8f);
            DisplayBuilder.cyanDust(center, 25, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Inner ring of budding amethyst rotates at 0.5 deg/tick
            float rotAngle = ticksAlive * 0.00875f;
            for (int i = 0; i < innerRing.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 8 + rotAngle;
                Location loc = center.clone().add(Math.cos(baseAngle) * 2.0, -1, Math.sin(baseAngle) * 2.0);
                innerRing.get(i).entity().teleport(loc);
            }

            // Bottom clusters scale pulse: 0.9 to 1.1 on 60-tick cycle
            float clusterScale = 0.7f + 0.14f * (float) Math.sin(ticksAlive * (2 * Math.PI / 60));
            for (BlockDisplayHandle h : bottomClusters) {
                h.scale(clusterScale, clusterScale, clusterScale);
                h.interpolation(2, 0);
            }

            // Dragon breath particles rising from bottom lantern
            if (ticksAlive % 3 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        center.clone().add(0, -3, 0), 3, 0.2, 0.5, 0.2, 0.01);
            }

            // Portal particles spiraling around end rods
            if (ticksAlive % 4 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 1.0, 0), 4, 3.0, 0.5, 3.0, 0);
            }

            // Cloud mist at Y-1
            if (ticksAlive % 6 == 0) {
                center.getWorld().spawnParticle(Particle.CLOUD,
                        center.clone().add(0, -1, 0), 2, 1.0, 0.1, 1.0, 0.01);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeepBore(plugin); }
    }

    // ================================================================
    // 5. CRYSTAL SPINE RIDGE -- Linear ridge of erupting crystal spines
    // ================================================================
    public static class CrystalSpineRidge extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spineClusters = new ArrayList<>();
        private final List<BlockDisplayHandle> flankingShards = new ArrayList<>();
        private BlockDisplayHandle centerTall;

        public CrystalSpineRidge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_spine_ridge", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 6 base amethyst blocks flush with ground
            int[] heights = {1, 2, 1, 3, 2, 1};
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(i - 2.5, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.9f, 0.5f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                baseBlocks.add(h);
                spawnedEntities.add(h.entity());

                // Large amethyst cluster spines at staggered heights
                Location spineLoc = center.clone().add(i - 2.5, 0.5, 0);
                BlockDisplayHandle spine = displayBuilder.spawnBlock(spineLoc, Material.AMETHYST_CLUSTER);
                spine.scale(0.6f, heights[i] * 0.6f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                spineClusters.add(spine);
                spawnedEntities.add(spine.entity());
            }

            // Center tall cluster (3 blocks)
            centerTall = spineClusters.get(3); // The tallest one

            // 2 end rods on the center tall spine pointing horizontally
            for (int side = -1; side <= 1; side += 2) {
                Location rodLoc = center.clone().add(0.5, 1.5, side * 0.5);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(rodLoc, Material.END_ROD);
                rod.scale(0.2f, 0.2f, 1.0f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            // 12 flanking shards at ground level
            for (int i = 0; i < 6; i++) {
                for (int side = -1; side <= 1; side += 2) {
                    Location shardLoc = center.clone().add(i - 2.5, 0, side * 0.6);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.AMETHYST_CLUSTER);
                    shard.scale(0.4f, 0.3f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                    flankingShards.add(shard);
                    spawnedEntities.add(shard.entity());
                }
            }

            // Prismarine anchor blocks at ends
            for (int end = -1; end <= 1; end += 2) {
                Location anchorLoc = center.clone().add(end * 3.5, 0, 0);
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(anchorLoc, Material.PRISMARINE);
                anchor.scale(1.0f, 0.8f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(anchor.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Center tall cluster slow rotation: 0.3 deg/tick
            if (centerTall != null) {
                BlockDisplay bd = centerTall.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.3f, -0.3f, -0.3f),
                        new AxisAngle4f(ticksAlive * 0.00524f, 0, 1, 0),
                        new Vector3f(0.6f, 1.8f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Flanking shards wave: each offset 10 ticks creating ripple
            for (int i = 0; i < flankingShards.size(); i++) {
                float yOff = 0.1f * (float) Math.sin((ticksAlive - i * 5) * 0.15f);
                BlockDisplay bd = flankingShards.get(i).entity();
                Location base = bd.getLocation();
                bd.teleport(base.clone().add(0, yOff, 0));
            }

            // Cyan dust flowing along ridge
            if (ticksAlive % 4 == 0) {
                int idx = (ticksAlive / 4) % 6;
                Location dustLoc = center.clone().add(idx - 2.5, 0.3, 0);
                DisplayBuilder.cyanDust(dustLoc, 3, 0.3);
            }

            // Electric spark arc between end rods every 8 ticks
            if (ticksAlive % 8 == 0) {
                Location left = center.clone().add(0.5, 1.5, -0.5);
                Location right = center.clone().add(0.5, 1.5, 0.5);
                for (int p = 0; p < 5; p++) {
                    double t = p / 4.0;
                    Location particle = left.clone().add(0, 0.1, t);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0.1, 0, 0);
                }
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalSpineRidge(plugin); }
    }

    // ================================================================
    // 6. BLOOM NODE -- Central eye surrounded by crystalline petals
    // ================================================================
    public static class BloomNode extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> outerClusters = new ArrayList<>();
        private final List<BlockDisplayHandle> capBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> capRods = new ArrayList<>();
        private final List<BlockDisplayHandle> lanternRing = new ArrayList<>();

        public BloomNode(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bloom_node", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3x3 base of budding amethyst
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BUDDING_AMETHYST);
                    h.scale(0.9f, 0.5f, 0.9f).glow(128, 0, 255).interpolation(2, 0);
                    baseBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 8 outer large amethyst clusters at cardinal/intercardinal points
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 0.3, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.6f, 0.8f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                outerClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central "eye" region: amethyst block at Y+1.5 (representing the Eye of Ender)
            BlockDisplayHandle eyeCore = displayBuilder.spawnBlock(center.clone().add(0, 1.5, 0), Material.SEA_LANTERN);
            eyeCore.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(eyeCore.entity());

            // 4 tetrahedral cap amethyst blocks above the eye
            double[][] capOffsets = {{0.5, 3.0, 0}, {-0.5, 3.0, 0}, {0, 3.0, 0.5}, {0, 3.0, -0.5}};
            for (double[] off : capOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                capBlocks.add(h);
                spawnedEntities.add(h.entity());

                // End rod from cap block pointing toward eye
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc.clone().add(0, -0.5, 0), Material.END_ROD);
                rod.scale(0.2f, 1.0f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                capRods.add(rod);
                spawnedEntities.add(rod.entity());
            }

            // 5x5 sea lantern ring (12 visible, corners missing)
            int[][] ringPositions = {{-2,-2},{-2,-1},{-2,0},{-2,1},{-2,2},{-1,-2},{-1,2},{0,-2},{0,2},{1,-2},{1,2},{2,-2}};
            for (int[] pos : ringPositions) {
                Location loc = center.clone().add(pos[0], 0, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.8f, 0.3f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                lanternRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
            DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye orbits at 2 deg/tick -- sea lantern at center orbits
            float eyeAngle = ticksAlive * 0.035f;

            // Cap blocks counter-rotate at 1 deg/tick
            float capAngle = -ticksAlive * 0.0175f;
            for (int i = 0; i < capBlocks.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 4 + capAngle;
                float bob = 0.4f * (float) Math.sin(ticksAlive * (2 * Math.PI / 90));
                Location loc = center.clone().add(
                        Math.cos(baseAngle) * 0.5, 3.0 + bob, Math.sin(baseAngle) * 0.5
                );
                capBlocks.get(i).entity().teleport(loc);
                if (i < capRods.size()) {
                    capRods.get(i).entity().teleport(loc.clone().add(0, -0.5, 0));
                }
            }

            // Laser pulse from eye to each end rod tip in sequence every 30 ticks
            if (ticksAlive % 30 == 0) {
                int beamIdx = (ticksAlive / 30) % 4;
                if (beamIdx < capBlocks.size()) {
                    Location eyeLoc = center.clone().add(0, 1.5, 0);
                    Location rodLoc = capBlocks.get(beamIdx).entity().getLocation();
                    for (int p = 0; p < 8; p++) {
                        double t = p / 7.0;
                        Location particle = eyeLoc.clone().add(
                                (rodLoc.getX() - eyeLoc.getX()) * t,
                                (rodLoc.getY() - eyeLoc.getY()) * t,
                                (rodLoc.getZ() - eyeLoc.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 2, 0.05, 0.05, 0.05, 0);
                    }
                }
            }

            // Cyan dust emitting from eye
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 5, 1.0);
            }

            // End rod particles from sea lantern ring
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle h : lanternRing) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            h.entity().getLocation().add(0, 0.5, 0), 1, 0.1, 0.3, 0.1, 0.01);
                }
            }

            // Sound every 160 ticks
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BloomNode(plugin); }
    }

    // ================================================================
    // 7. SHATTERED PILLAR -- Broken crystal column with floating shards
    // ================================================================
    public static class ShatteredPillar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> lowerColumn = new ArrayList<>();
        private final List<BlockDisplayHandle> shardLeft = new ArrayList<>();
        private final List<BlockDisplayHandle> shardCenter = new ArrayList<>();
        private final List<BlockDisplayHandle> shardRight = new ArrayList<>();
        private final List<BlockDisplayHandle> rubble = new ArrayList<>();

        public ShatteredPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shattered_pillar", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 2x2 polished blackstone base
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x - 0.5, 0, z - 0.5), Material.POLISHED_BLACKSTONE);
                    h.scale(0.9f, 1.0f, 0.9f).glow(80, 80, 100).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // Lower column: 3x3 amethyst, 3 blocks tall (one corner missing)
            for (int y = 1; y <= 3; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 1 && z == 1) continue; // Missing corner
                        if (x == 0 && z == 0) continue; // Hollow center
                        Location loc = center.clone().add(x * 0.5, y, z * 0.5);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                        h.scale(0.5f, 0.9f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                        lowerColumn.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Left shard: 3 amethyst shard blocks going up-left
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-0.5 - i * 0.4, 4 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.6f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                shardLeft.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center shard: 2 amethyst + 1 large cluster straight up
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, 4 + i, 0);
                Material mat = i < 2 ? Material.AMETHYST_BLOCK : Material.AMETHYST_CLUSTER;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.8f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                shardCenter.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right shard: 2 amethyst shard blocks going up-right
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0.5 + i * 0.4, 4 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.6f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                shardRight.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cyan glass backing behind each shard fragment
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-0.5 - i * 0.4, 4 + i * 0.8, -0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.4f, 0.5f, 0.1f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Dark prismarine rubble at break point
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add((i - 1.5) * 0.3, 3.5 + (i % 2) * 0.2, (i % 2) * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                rubble.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Center shard bobs vertically: 0.25 blocks, 55-tick cycle
            float centerBob = 0.25f * (float) Math.sin(ticksAlive * (2 * Math.PI / 55));
            for (int i = 0; i < shardCenter.size(); i++) {
                Location loc = center.clone().add(0, 4 + i + centerBob, 0);
                shardCenter.get(i).entity().teleport(loc);
            }

            // Left shard rotates 5 degrees and back: 70-tick cycle
            float leftTilt = 0.087f * (float) Math.sin(ticksAlive * (2 * Math.PI / 70));
            for (BlockDisplayHandle h : shardLeft) {
                BlockDisplay bd = h.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.25f, -0.3f, -0.25f),
                        new AxisAngle4f(leftTilt, 0, 0, 1),
                        new Vector3f(0.5f, 0.6f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Right shard scale pulse: 0.95 to 1.05, 85-tick cycle
            float rightScale = 1.0f + 0.05f * (float) Math.sin(ticksAlive * (2 * Math.PI / 85));
            for (BlockDisplayHandle h : shardRight) {
                h.scale(0.5f * rightScale, 0.6f * rightScale, 0.5f * rightScale);
                h.interpolation(2, 0);
            }

            // Portal particles from break point
            if (ticksAlive % 4 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 3.5, 0), 3, 0.5, 0.2, 0.5, 0.05);
            }

            // Electric spark arcs between shard tips in triangle every 25 ticks
            if (ticksAlive % 25 == 0) {
                int arcIdx = (ticksAlive / 25) % 3;
                List<List<BlockDisplayHandle>> shards = List.of(shardLeft, shardCenter, shardRight);
                int nextIdx = (arcIdx + 1) % 3;
                if (!shards.get(arcIdx).isEmpty() && !shards.get(nextIdx).isEmpty()) {
                    Location from = shards.get(arcIdx).get(shards.get(arcIdx).size() - 1).entity().getLocation();
                    Location to = shards.get(nextIdx).get(shards.get(nextIdx).size() - 1).entity().getLocation();
                    for (int p = 0; p < 5; p++) {
                        double t = p / 4.0;
                        Location particle = from.clone().add(
                                (to.getX() - from.getX()) * t,
                                (to.getY() - from.getY()) * t,
                                (to.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Purple dust falling from shard tips
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 6, 0), 3, 1.0);
            }

            // Sound every 140 ticks
            if (ticksAlive % 140 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShatteredPillar(plugin); }
    }

    // ================================================================
    // 8. BIOLUMINESCENT PATCH -- Ground-level crystal growth spread
    // ================================================================
    public static class BioluminescentPatch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> middleRing = new ArrayList<>();
        private final List<BlockDisplayHandle> centerClusters = new ArrayList<>();
        private final List<BlockDisplayHandle> centerLanterns = new ArrayList<>();

        public BioluminescentPatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bioluminescent_patch", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 10 blocks alternating budding amethyst / amethyst shard
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double radius = 3.0 + (i % 2) * 0.3; // Irregular spacing
                Location loc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                Material mat = (i % 2 == 0) ? Material.BUDDING_AMETHYST : Material.AMETHYST_CLUSTER;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.3f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring: 8 amethyst blocks at staggered heights
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                float height = 0.5f + (i % 3) * 0.25f;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, height, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.6f, 0.5f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                middleRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center: 3 large amethyst clusters at 1.5-2.0 height
            float[] clusterHeights = {1.5f, 1.8f, 2.0f};
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, clusterHeights[i], Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.7f, 0.9f, 0.7f).glow(0, 200, 255).interpolation(2, 0);
                centerClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 sea lanterns at ground between clusters
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3 + (Math.PI / 3);
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.5f, 0.3f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                centerLanterns.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 end rods between outer and middle rings
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 2.4, 0, Math.sin(angle) * 2.4);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.2f, 1.5f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Center clusters pulse scale: 1.0 to 1.2, 50-tick cycle staggered 17 ticks
            for (int i = 0; i < centerClusters.size(); i++) {
                float scale = 0.7f + 0.14f * (float) Math.sin((ticksAlive + i * 17) * (2 * Math.PI / 50));
                centerClusters.get(i).scale(scale, scale * 1.3f, scale);
                centerClusters.get(i).interpolation(2, 0);
            }

            // Outer ring gentle vertical oscillation: 0.1 block, 40-tick cycle
            for (int i = 0; i < outerRing.size(); i++) {
                float yOff = 0.1f * (float) Math.sin((ticksAlive + i * 4) * (2 * Math.PI / 40));
                BlockDisplay bd = outerRing.get(i).entity();
                Location base = bd.getLocation();
                bd.teleport(base.clone().add(0, yOff, 0));
            }

            // Spore blossom particles drifting upward from all blocks
            if (ticksAlive % 3 == 0) {
                center.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                        center.clone().add(0, 1.0, 0), 8, 3.0, 0.5, 3.0, 0.01);
            }

            // Cyan dust pulse from center at each cluster scale peak
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 12, 2.0);
            }

            // Sound every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BioluminescentPatch(plugin); }
    }

    // ================================================================
    // 9. CRYSTAL FOUNTAIN -- Vertical eruption with rotating crown
    // ================================================================
    public static class CrystalFountain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crownClusters = new ArrayList<>();
        private BlockDisplayHandle peakLantern;
        private final List<BlockDisplayHandle> midArms = new ArrayList<>();
        private final List<BlockDisplayHandle> buttresses = new ArrayList<>();

        public CrystalFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_fountain", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central column: 5 blocks alternating amethyst block and large amethyst cluster
            for (int y = 0; y < 5; y++) {
                Material mat = (y % 2 == 0) ? Material.AMETHYST_BLOCK : Material.AMETHYST_CLUSTER;
                Location loc = center.clone().add(0, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.9f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                columnBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown: 6 large amethyst clusters at Y+5 angled outward
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 0.8, 5, Math.sin(angle) * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.7f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                crownClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // Sea lantern at peak
            peakLantern = displayBuilder.spawnBlock(center.clone().add(0, 5.5, 0), Material.SEA_LANTERN);
            peakLantern.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(peakLantern.entity());

            // 4 mid-level amethyst shard arms at Y+3
            double[][] armOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (double[] off : armOffsets) {
                Location loc = center.clone().add(off[0], 3, off[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.8f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                midArms.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 dark prismarine buttresses at Y+1
            for (double[] off : armOffsets) {
                Location loc = center.clone().add(off[0], 1, off[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.7f, 0.7f, 0.7f).glow(0, 200, 255).interpolation(2, 0);
                buttresses.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3x3 prismarine base with cyan glass corners
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    Material mat = (Math.abs(x) == 1 && Math.abs(z) == 1) ?
                            Material.CYAN_STAINED_GLASS : Material.PRISMARINE;
                    Location loc = center.clone().add(x, -0.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.9f, 0.5f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
            DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Column (not base) rotates at 1.5 deg/tick
            float rotAngle = ticksAlive * 0.0262f;
            for (int y = 0; y < columnBlocks.size(); y++) {
                Location loc = center.clone().add(0, y, 0);
                BlockDisplay bd = columnBlocks.get(y).entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.3f, -0.45f, -0.3f),
                        new AxisAngle4f(rotAngle, 0, 1, 0),
                        new Vector3f(0.6f, 0.9f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Crown clusters open outward: scale 1.0 to 1.3, 100-tick cycle
            float crownScale = 1.0f + 0.3f * (float) Math.sin(ticksAlive * (2 * Math.PI / 100));
            for (int i = 0; i < crownClusters.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6 + rotAngle;
                Location loc = center.clone().add(Math.cos(angle) * 0.8, 5, Math.sin(angle) * 0.8);
                crownClusters.get(i).entity().teleport(loc);
                crownClusters.get(i).scale(0.5f * crownScale, 0.7f * crownScale, 0.5f * crownScale);
                crownClusters.get(i).interpolation(2, 0);
            }

            // Mid arms oscillate up/down 0.2 blocks
            float armBob = 0.2f * (float) Math.sin(ticksAlive * 0.08f);
            for (BlockDisplayHandle arm : midArms) {
                Location base = arm.entity().getLocation();
                arm.entity().teleport(base.clone().add(0, armBob, 0));
            }

            // Electric spark cascade from peak lantern downward along column
            if (ticksAlive % 4 == 0) {
                int cascadeY = (ticksAlive / 4) % 6;
                Location sparkLoc = center.clone().add(0, 5.5 - cascadeY, 0);
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 3, 0.2, 0.1, 0.2, 0.02);
            }

            // Cyan dust arcs from crown clusters outward
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < crownClusters.size(); i++) {
                    DisplayBuilder.cyanDust(crownClusters.get(i).entity().getLocation(), 2, 0.5);
                }
            }

            // Portal particles from base cyan glass
            if (ticksAlive % 10 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 0, 0), 3, 1.0, 0.1, 1.0, 0.02);
            }

            // Sound every 80 ticks
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalFountain(plugin); }
    }

    // ================================================================
    // 10. ROOT TANGLE -- Sprawling crystal root network
    // ================================================================
    public static class RootTangle extends BlockDisplayAttack {

        private BlockDisplayHandle centerNode;
        private final List<List<BlockDisplayHandle>> rootArms = new ArrayList<>();
        private final List<BlockDisplayHandle> fillBlocks = new ArrayList<>();

        public RootTangle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("root_tangle", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central node: amethyst block + large cluster at 1 block height
            centerNode = displayBuilder.spawnBlock(center, Material.AMETHYST_BLOCK);
            centerNode.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(centerNode.entity());

            BlockDisplayHandle centerCluster = displayBuilder.spawnBlock(center.clone().add(0, 1, 0), Material.AMETHYST_CLUSTER);
            centerCluster.scale(0.6f, 0.7f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(centerCluster.entity());

            // 8 root arms radiating outward (4 horizontal, 4 diagonal)
            Material[] armMats = {Material.AMETHYST_CLUSTER, Material.AMETHYST_BLOCK, Material.DARK_PRISMARINE};
            for (int arm = 0; arm < 8; arm++) {
                double angle = (Math.PI * 2 * arm) / 8;
                int armLen = 3 + (arm % 2); // 3 or 4 blocks
                List<BlockDisplayHandle> armBlocks = new ArrayList<>();

                for (int seg = 1; seg <= armLen; seg++) {
                    double dist = seg * 0.9;
                    float depth = -0.1f * seg; // Slightly below ground
                    Location loc = center.clone().add(Math.cos(angle) * dist, depth, Math.sin(angle) * dist);
                    Material mat = armMats[seg % armMats.length];
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.5f, 0.3f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                    armBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Tip clusters at arm end
                double tipDist = (armLen + 1) * 0.9;
                for (int tip = 0; tip < 2; tip++) {
                    Location tipLoc = center.clone().add(
                            Math.cos(angle) * (tipDist + tip * 0.3), -0.1, Math.sin(angle) * (tipDist + tip * 0.3));
                    BlockDisplayHandle tipH = displayBuilder.spawnBlock(tipLoc, Material.AMETHYST_CLUSTER);
                    tipH.scale(0.4f, 0.5f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                    armBlocks.add(tipH);
                    spawnedEntities.add(tipH.entity());
                }

                rootArms.add(armBlocks);
            }

            // 16 budding amethyst fill blocks between root arms
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16 + 0.1;
                double dist = 1.0 + (i % 3) * 0.6;
                Location loc = center.clone().add(Math.cos(angle) * dist, -0.1, Math.sin(angle) * dist);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BUDDING_AMETHYST);
                h.scale(0.5f, 0.25f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                fillBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.7f);
            DisplayBuilder.purpleDust(center, 15, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Central node extremely slow rotation: 360 deg / 200 ticks
            if (centerNode != null) {
                BlockDisplay bd = centerNode.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.4f, -0.4f, -0.4f),
                        new AxisAngle4f(ticksAlive * 0.0314f, 0, 1, 0),
                        new Vector3f(0.8f, 0.8f, 0.8f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Root arms pivot very subtly: 3 degrees up/down, different cycles per arm
            for (int arm = 0; arm < rootArms.size(); arm++) {
                int cycleTicks = 60 + arm * 5; // 60-95 tick cycles
                float pivot = 0.05f * (float) Math.sin(ticksAlive * (2 * Math.PI / cycleTicks));
                List<BlockDisplayHandle> armBlocks = rootArms.get(arm);
                for (BlockDisplayHandle h : armBlocks) {
                    BlockDisplay bd = h.entity();
                    Location base = bd.getLocation();
                    bd.teleport(base.clone().add(0, pivot, 0));
                }
            }

            // Electric spark runs along one arm at a time, cycling
            if (ticksAlive % 8 == 0) {
                int armIdx = (ticksAlive / 8) % rootArms.size();
                List<BlockDisplayHandle> arm = rootArms.get(armIdx);
                int segIdx = (ticksAlive / 8) % arm.size();
                if (segIdx < arm.size()) {
                    Location sparkLoc = arm.get(segIdx).entity().getLocation().add(0, 0.3, 0);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 2, 0.1, 0.1, 0.1, 0);
                }
            }

            // Purple dust pulse from center outward every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 0.3, 0), 8, 4.0);
            }

            // Witch particles from budding amethyst fill blocks
            if (ticksAlive % 10 == 0) {
                int idx = (ticksAlive / 10) % fillBlocks.size();
                center.getWorld().spawnParticle(Particle.WITCH,
                        fillBlocks.get(idx).entity().getLocation().add(0, 0.3, 0), 1, 0.1, 0.2, 0.1, 0);
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RootTangle(plugin); }
    }
}
