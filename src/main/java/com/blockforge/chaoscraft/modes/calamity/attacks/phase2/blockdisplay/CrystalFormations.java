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
 * Phase 2 (4B) Block Display -- GROUP 6: CRYSTAL FORMATION CLUSTERS
 * 10 attacks featuring crystalline growths erupting from the Crystalline Plague island.
 * DoG theme: electric cyan, amethyst, dark prismarine, polished blackstone.
 * These formations represent the island's crystal infection intensifying
 * as DoG enters its ethereal Phase 2 state.
 *
 * Rules applied:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG palette: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 * - Damage in HP (4.0-12.0 range)
 */
public final class CrystalFormations {

    private CrystalFormations() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PlagueBloom(plugin));
        registry.register(new CrystallineGeyser(plugin));
        registry.register(new AmethystMaw(plugin));
        registry.register(new ShardStorm(plugin));
        registry.register(new GeodeCrater(plugin));
        registry.register(new PrismarineTumor(plugin));
        registry.register(new CrystalVeinEruption(plugin));
        registry.register(new FracturedGeode(plugin));
        registry.register(new BloomingInfection(plugin));
        registry.register(new CrystallineTremor(plugin));
    }

    // ================================================================
    // 1. PLAGUE BLOOM -- Radial burst of amethyst clusters that pulse
    //    outward from a budding amethyst core like a diseased flower
    // ================================================================
    public static class PlagueBloom extends BlockDisplayAttack {

        private BlockDisplayHandle coreBlock;
        private final List<BlockDisplayHandle> petalRing = new ArrayList<>();
        private final List<BlockDisplayHandle> outerSpines = new ArrayList<>();
        private final List<BlockDisplayHandle> baseNodes = new ArrayList<>();

        public PlagueBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plague_bloom", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Core: budding amethyst center node
            coreBlock = displayBuilder.spawnBlock(center, Material.BUDDING_AMETHYST);
            coreBlock.scale(1.2f, 1.2f, 1.2f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(coreBlock.entity());

            // Petal ring: 8 large amethyst clusters angled outward at 30 degrees
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 0.3, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.8f, 1.2f, 0.8f).glow(128, 0, 255)
                        .rotate((float)(angle + Math.PI / 6), 0, 1, 0).interpolation(3, 0);
                petalRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer spines: 6 amethyst blocks at 3-block radius
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + Math.PI / 6;
                Location loc = center.clone().add(Math.cos(angle) * 3.0, 0, Math.sin(angle) * 3.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.7f, 0.5f).glow(0, 200, 255).interpolation(3, 0);
                outerSpines.add(h);
                spawnedEntities.add(h.entity());
            }

            // Base: 4 dark prismarine anchor blocks
            double[][] baseOff = {{1, -0.5, 0}, {-1, -0.5, 0}, {0, -0.5, 1}, {0, -0.5, -1}};
            for (double[] off : baseOff) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.9f, 0.4f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                baseNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
            DisplayBuilder.cyanDust(center, 25, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Petal breathing: scale pulse on 60-tick cycle
            float breathScale = 1.0f + 0.2f * (float) Math.sin(ticksAlive * Math.PI / 30);
            for (int i = 0; i < petalRing.size(); i++) {
                float offset = i * 0.15f;
                float localScale = 1.0f + 0.2f * (float) Math.sin((ticksAlive + i * 8) * Math.PI / 30);
                petalRing.get(i).scale(0.8f * localScale, 1.2f * localScale, 0.8f * localScale);
                petalRing.get(i).interpolation(3, 0);
            }

            // Core rotation: slow spin
            if (coreBlock != null) {
                coreBlock.rotate(ticksAlive * 0.02f, 0, 1, 0);
                coreBlock.interpolation(3, 0);
            }

            // Outer spines bob vertically with staggered cycles
            for (int i = 0; i < outerSpines.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6 + Math.PI / 6;
                float yOff = 0.3f * (float) Math.sin((ticksAlive + i * 12) * Math.PI / 40);
                Location target = center.clone().add(Math.cos(angle) * 3.0, yOff, Math.sin(angle) * 3.0);
                outerSpines.get(i).entity().teleport(target);
            }

            // Particles: cyan dust pulse from center every 20 ticks
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 12, 2.0);
            }

            // Ambient particles from petals
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : petalRing) {
                    Location pLoc = h.entity().getLocation().add(0, 0.5, 0);
                    w.spawnParticle(Particle.END_ROD, pLoc, 1, 0.1, 0.2, 0.1, 0.01);
                }
            }

            // Sound pulse every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PlagueBloom(plugin); }
    }

    // ================================================================
    // 2. CRYSTALLINE GEYSER -- Vertical column of crystal shards that
    //    erupts upward, scattering amethyst debris at its peak
    // ================================================================
    public static class CrystallineGeyser extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crownShards = new ArrayList<>();
        private BlockDisplayHandle lanternBase;

        public CrystallineGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_geyser", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base sea lantern
            lanternBase = displayBuilder.spawnBlock(center.clone().add(0, -1, 0), Material.SEA_LANTERN);
            lanternBase.scale(1.5f, 0.5f, 1.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(lanternBase.entity());

            // Column: 6 alternating amethyst/dark prismarine blocks
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, -6 + i, 0);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_BLOCK : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float taper = 1.0f - (i * 0.08f);
                h.scale(taper, 1.0f, taper).glow(128, 0, 255).interpolation(3, 0);
                columnBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown: 5 large amethyst clusters radiating outward at peak
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * 0.8, -1, Math.sin(angle) * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.6f, 0.9f, 0.6f).glow(240, 240, 255).interpolation(3, 0);
                crownShards.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
            DisplayBuilder.cyanDust(center, 30, 1.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Emerge upward over 35 ticks
            if (ticksAlive <= 35) {
                float progress = ticksAlive / 35.0f;
                float yOffset = progress * 6.0f;
                for (int i = 0; i < columnBlocks.size(); i++) {
                    Location target = center.clone().add(0, -6 + i + yOffset, 0);
                    columnBlocks.get(i).entity().teleport(target);
                    float scale = 0.1f + progress * 0.9f;
                    float taper = scale * (1.0f - (i * 0.08f));
                    columnBlocks.get(i).scale(taper, scale, taper);
                    columnBlocks.get(i).interpolation(3, 0);
                }
                for (int i = 0; i < crownShards.size(); i++) {
                    double angle = (Math.PI * 2 * i) / 5;
                    Location target = center.clone().add(Math.cos(angle) * 0.8, -1 + yOffset + 5, Math.sin(angle) * 0.8);
                    crownShards.get(i).entity().teleport(target);
                }
            }
            // Phase 2: Crown oscillates, column trembles
            else {
                float tremble = (float) Math.sin(ticksAlive * 1.5) * 0.1f;
                for (BlockDisplayHandle h : columnBlocks) {
                    BlockDisplay bd = h.entity();
                    Location base = bd.getLocation();
                    bd.teleport(base.clone().add(tremble * 0.5, 0, tremble * 0.3));
                }

                // Crown shards pulse outward
                for (int i = 0; i < crownShards.size(); i++) {
                    double angle = (Math.PI * 2 * i) / 5;
                    float radPulse = 0.8f + 0.4f * (float) Math.sin((ticksAlive + i * 10) * Math.PI / 25);
                    float yPulse = 0.2f * (float) Math.sin((ticksAlive + i * 15) * Math.PI / 30);
                    Location target = center.clone().add(Math.cos(angle) * radPulse, 5 + yPulse, Math.sin(angle) * radPulse);
                    crownShards.get(i).entity().teleport(target);
                }
            }

            // Particles: upward crystal spray
            if (ticksAlive % 4 == 0 && ticksAlive > 35) {
                DisplayBuilder.dustParticles(center.clone().add(0, 6, 0), 8, 0.5, 0, 200, 255, 1.0f);
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 6.5, 0), 3, 0.3, 0.5, 0.3, 0.02);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineGeyser(plugin); }
    }

    // ================================================================
    // 3. AMETHYST MAW -- Open jaw formation of crystal teeth rising
    //    from the ground, snapping shut periodically
    // ================================================================
    public static class AmethystMaw extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> upperTeeth = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerTeeth = new ArrayList<>();
        private final List<BlockDisplayHandle> jawFrame = new ArrayList<>();

        public AmethystMaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("amethyst_maw", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(10.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Upper jaw: 6 large amethyst clusters pointing downward in an arc
            for (int i = 0; i < 6; i++) {
                double xOff = -2.5 + i;
                Location loc = center.clone().add(xOff, 3.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.6f, 1.0f, 0.6f).glow(128, 0, 255)
                        .rotate((float) Math.PI, 1, 0, 0).interpolation(3, 0);
                upperTeeth.add(h);
                spawnedEntities.add(h.entity());
            }

            // Lower jaw: 6 large amethyst clusters pointing upward
            for (int i = 0; i < 6; i++) {
                double xOff = -2.5 + i;
                Location loc = center.clone().add(xOff, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.6f, 1.0f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                lowerTeeth.add(h);
                spawnedEntities.add(h.entity());
            }

            // Frame: polished blackstone jaw hinge blocks
            Location left = center.clone().add(-3, 1.5, 0);
            Location right = center.clone().add(3, 1.5, 0);
            BlockDisplayHandle lFrame = displayBuilder.spawnBlock(left, Material.POLISHED_BLACKSTONE);
            lFrame.scale(0.8f, 2.0f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            jawFrame.add(lFrame);
            spawnedEntities.add(lFrame.entity());

            BlockDisplayHandle rFrame = displayBuilder.spawnBlock(right, Material.POLISHED_BLACKSTONE);
            rFrame.scale(0.8f, 2.0f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            jawFrame.add(rFrame);
            spawnedEntities.add(rFrame.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.7f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Jaw snap cycle: open for 50 ticks, snap shut over 5 ticks, hold 15, reopen 30
            int cycle = ticksAlive % 100;
            float upperY, lowerY;

            if (cycle < 50) {
                // Open
                upperY = 3.0f;
                lowerY = 0.0f;
            } else if (cycle < 55) {
                // Snapping shut
                float snap = (cycle - 50) / 5.0f;
                upperY = 3.0f - snap * 1.5f;
                lowerY = 0.0f + snap * 1.5f;
            } else if (cycle < 70) {
                // Closed hold
                upperY = 1.5f;
                lowerY = 1.5f;
            } else {
                // Reopening
                float open = (cycle - 70) / 30.0f;
                upperY = 1.5f + open * 1.5f;
                lowerY = 1.5f - open * 1.5f;
            }

            for (int i = 0; i < upperTeeth.size(); i++) {
                double xOff = -2.5 + i;
                upperTeeth.get(i).entity().teleport(center.clone().add(xOff, upperY, 0));
                upperTeeth.get(i).interpolation(2, 0);
            }
            for (int i = 0; i < lowerTeeth.size(); i++) {
                double xOff = -2.5 + i;
                lowerTeeth.get(i).entity().teleport(center.clone().add(xOff, lowerY, 0));
                lowerTeeth.get(i).interpolation(2, 0);
            }

            // Snap sound at moment of closure
            if (cycle == 54) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.9f, 0.8f);
                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 20, 1.5);
            }

            // Ambient particles between teeth
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 5, 1.5, 128, 0, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AmethystMaw(plugin); }
    }

    // ================================================================
    // 4. SHARD STORM -- Orbiting ring of sharp crystal fragments that
    //    spin faster over time before releasing outward
    // ================================================================
    public static class ShardStorm extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shardRing = new ArrayList<>();
        private BlockDisplayHandle eyeBlock;

        public ShardStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shard_storm", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central eye: sea lantern
            eyeBlock = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.SEA_LANTERN);
            eyeBlock.scale(0.6f, 0.6f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(eyeBlock.entity());

            // 12 orbiting shards at varying heights
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double yVar = (i % 3 - 1) * 0.4;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 2 + yVar, Math.sin(angle) * 2.5);
                Material mat = (i % 3 == 0) ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 0.5f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                shardRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Orbital speed increases over time
            float speedMult = 1.0f + (ticksAlive / (float) config.getDurationTicks()) * 3.0f;
            float baseAngle = ticksAlive * 0.03f * speedMult;

            // Orbit radius tightens over time
            float radius = 2.5f - (ticksAlive / (float) config.getDurationTicks()) * 1.5f;
            radius = Math.max(0.8f, radius);

            for (int i = 0; i < shardRing.size(); i++) {
                double angle = baseAngle + (Math.PI * 2 * i) / 12;
                double yVar = (i % 3 - 1) * 0.4 * (float) Math.sin(ticksAlive * 0.05 + i);
                Location target = center.clone().add(
                        Math.cos(angle) * radius, 2 + yVar, Math.sin(angle) * radius);
                shardRing.get(i).entity().teleport(target);
                shardRing.get(i).rotate(ticksAlive * 0.1f + i, 0.5f, 1, 0.3f);
                shardRing.get(i).interpolation(2, 0);
            }

            // Eye pulse
            float eyeScale = 0.6f + 0.15f * (float) Math.sin(ticksAlive * Math.PI / 20);
            if (eyeBlock != null) {
                eyeBlock.scale(eyeScale, eyeScale, eyeScale);
                eyeBlock.interpolation(2, 0);
            }

            // Particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 5, radius);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.5f + speedMult * 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShardStorm(plugin); }
    }

    // ================================================================
    // 5. GEODE CRATER -- Hollow sphere of crystal that opens from the
    //    ground revealing a glowing interior, dealing area damage
    // ================================================================
    public static class GeodeCrater extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shellBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> interiorLanterns = new ArrayList<>();
        private BlockDisplayHandle coreGlow;

        public GeodeCrater(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("geode_crater", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shell: 14 blocks in a hemisphere pattern
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 14; i++) {
                double y = (double) i / 14;
                double radiusAtY = Math.sqrt(1 - y * y) * 2.5;
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * radiusAtY;
                double z = Math.sin(theta) * radiusAtY;
                Material mat = (i % 3 == 0) ? Material.DARK_PRISMARINE : Material.AMETHYST_BLOCK;
                Location loc = center.clone().add(x, y * 2.5, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
                shellBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior lanterns: 3 sea lanterns inside the geode
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, 0.5 + i * 0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                interiorLanterns.add(h);
                spawnedEntities.add(h.entity());
            }

            // Core glow: cyan stained glass at center
            coreGlow = displayBuilder.spawnBlock(center.clone().add(0, 1, 0), Material.CYAN_STAINED_GLASS);
            coreGlow.scale(0.6f, 0.6f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(coreGlow.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Shell opens over first 40 ticks then pulses
            float openFactor;
            if (ticksAlive <= 40) {
                openFactor = ticksAlive / 40.0f;
            } else {
                openFactor = 1.0f + 0.1f * (float) Math.sin(ticksAlive * Math.PI / 30);
            }

            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < shellBlocks.size(); i++) {
                double y = (double) i / 14;
                double radiusAtY = Math.sqrt(1 - y * y) * (2.5 + openFactor * 0.5);
                double theta = goldenAngle * i + ticksAlive * 0.005;
                double x = Math.cos(theta) * radiusAtY;
                double z = Math.sin(theta) * radiusAtY;
                shellBlocks.get(i).entity().teleport(center.clone().add(x, y * 2.5, z));
                shellBlocks.get(i).interpolation(3, 0);
            }

            // Core glow pulse
            if (coreGlow != null) {
                float scale = 0.6f + 0.3f * (float) Math.sin(ticksAlive * Math.PI / 20);
                coreGlow.scale(scale, scale, scale);
                coreGlow.rotate(ticksAlive * 0.02f, 0, 1, 0);
                coreGlow.interpolation(2, 0);
            }

            // Interior lanterns bob
            for (int i = 0; i < interiorLanterns.size(); i++) {
                float yBob = 0.15f * (float) Math.sin((ticksAlive + i * 20) * Math.PI / 25);
                Location loc = center.clone().add(0, 0.5 + i * 0.7 + yBob, 0);
                interiorLanterns.get(i).entity().teleport(loc);
            }

            // Particles: light leaking from the geode
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1.2, 0), 8, 1.5);
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 2.5, 0), 2, 1.0, 0.5, 1.0, 0.01);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GeodeCrater(plugin); }
    }

    // ================================================================
    // 6. PRISMARINE TUMOR -- Bulging mass of dark prismarine and
    //    amethyst that grows outward from a central infection point
    // ================================================================
    public static class PrismarineTumor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tumorBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> veinBlocks = new ArrayList<>();

        public PrismarineTumor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prismarine_tumor", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(550);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Core tumor mass: 8 dark prismarine in irregular cluster
            double[][] tumorOffsets = {
                    {0, 0, 0}, {0.6, 0.3, 0.4}, {-0.5, 0.5, 0.3}, {0.3, 0.8, -0.3},
                    {-0.4, 0.2, -0.6}, {0.2, 1.0, 0.1}, {-0.3, 0.6, 0.5}, {0.5, 0.4, -0.4}
            };
            for (double[] off : tumorOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                Material mat = (Math.random() > 0.4) ? Material.DARK_PRISMARINE : Material.PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float s = 0.5f + (float)(Math.random() * 0.4);
                h.scale(s, s, s).glow(0, 200, 255).interpolation(3, 0);
                tumorBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crystal veins: 5 amethyst clusters radiating outward
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * 2.0, 0.2, Math.sin(angle) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                veinBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Tumor growth: blocks slowly expand over 60 ticks
            float growFactor = Math.min(1.0f, ticksAlive / 60.0f);
            for (int i = 0; i < tumorBlocks.size(); i++) {
                float baseScale = 0.5f + (i % 3) * 0.15f;
                float pulse = 0.05f * (float) Math.sin((ticksAlive + i * 12) * Math.PI / 35);
                float s = (baseScale + pulse) * growFactor;
                tumorBlocks.get(i).scale(s, s, s);
                tumorBlocks.get(i).interpolation(3, 0);
            }

            // Vein extension: grow outward over 80 ticks
            float veinGrow = Math.min(1.0f, ticksAlive / 80.0f);
            for (int i = 0; i < veinBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 5;
                float rad = 0.5f + veinGrow * 1.5f;
                Location target = center.clone().add(Math.cos(angle) * rad, 0.2, Math.sin(angle) * rad);
                veinBlocks.get(i).entity().teleport(target);
                veinBlocks.get(i).scale(0.3f * veinGrow, 0.5f * veinGrow, 0.3f * veinGrow);
                veinBlocks.get(i).interpolation(3, 0);
            }

            // Particles: infection spreading
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 6, 1.0, 0, 200, 255, 1.2f);
            }
            if (ticksAlive % 6 == 0) {
                for (BlockDisplayHandle v : veinBlocks) {
                    Location vLoc = v.entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.END_ROD, vLoc, 1, 0.1, 0.1, 0.1, 0.005);
                }
            }

            if (ticksAlive % 70 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrismarineTumor(plugin); }
    }

    // ================================================================
    // 7. CRYSTAL VEIN ERUPTION -- Line of amethyst shards bursting
    //    from a fissure in sequence like dominoes
    // ================================================================
    public static class CrystalVeinEruption extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> veinSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> fissureGlass = new ArrayList<>();

        public CrystalVeinEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_vein_eruption", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Fissure line: 10 cyan stained glass blocks at ground level
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(i - 5, -0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.8f, 0.2f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                fissureGlass.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crystal eruptions: 10 shards that will emerge sequentially
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(i - 5, -3, 0);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_CLUSTER : Material.LARGE_AMETHYST_BUD;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float height = 1.0f + (float)(Math.random() * 1.5);
                h.scale(0.5f, height, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                veinSegments.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.9f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sequential eruption: each segment erupts 6 ticks apart
            for (int i = 0; i < veinSegments.size(); i++) {
                int eruptStart = i * 6;
                if (ticksAlive >= eruptStart && ticksAlive < eruptStart + 20) {
                    float progress = (ticksAlive - eruptStart) / 20.0f;
                    float yOffset = progress * 3.0f;
                    Location target = center.clone().add(i - 5, -3 + yOffset, 0);
                    veinSegments.get(i).entity().teleport(target);
                    veinSegments.get(i).interpolation(3, 0);

                    // Eruption particle at this segment
                    if (ticksAlive == eruptStart) {
                        Location pLoc = center.clone().add(i - 5, 0, 0);
                        DisplayBuilder.cyanDust(pLoc, 8, 0.5);
                        DisplayBuilder.playSound(pLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.8f + i * 0.1f);
                    }
                } else if (ticksAlive >= eruptStart + 20) {
                    // Emerged: tremble
                    float tremble = 0.05f * (float) Math.sin((ticksAlive + i * 5) * 1.5);
                    Location rest = center.clone().add(i - 5, 0 + tremble, 0);
                    veinSegments.get(i).entity().teleport(rest);
                }
            }

            // Fissure glow pulse
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : fissureGlass) {
                    Location fLoc = h.entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.END_ROD, fLoc, 1, 0.2, 0.1, 0.2, 0.005);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalVeinEruption(plugin); }
    }

    // ================================================================
    // 8. FRACTURED GEODE -- Split-open crystal sphere with rotating
    //    halves that expose a damaging interior
    // ================================================================
    public static class FracturedGeode extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> topHalf = new ArrayList<>();
        private final List<BlockDisplayHandle> bottomHalf = new ArrayList<>();
        private BlockDisplayHandle coreLight;

        public FracturedGeode(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fractured_geode", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(9.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Top half: 8 blocks in upper hemisphere
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double yOff = 0.5 + (i % 3) * 0.5;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, yOff, Math.sin(angle) * 1.8);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_BLOCK : Material.POLISHED_BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.7f, 0.7f).glow(128, 0, 255).interpolation(3, 0);
                topHalf.add(h);
                spawnedEntities.add(h.entity());
            }

            // Bottom half: 8 blocks in lower hemisphere
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 + Math.PI / 8;
                double yOff = -0.5 - (i % 3) * 0.5;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, yOff, Math.sin(angle) * 1.8);
                Material mat = (i % 2 == 0) ? Material.DARK_PRISMARINE : Material.AMETHYST_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.7f, 0.7f).glow(0, 200, 255).interpolation(3, 0);
                bottomHalf.add(h);
                spawnedEntities.add(h.entity());
            }

            // Core light
            coreLight = displayBuilder.spawnBlock(center, Material.SEA_LANTERN);
            coreLight.scale(0.8f, 0.8f, 0.8f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(coreLight.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            DisplayBuilder.cyanDust(center, 25, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Split open over first 30 ticks
            float splitFactor = Math.min(1.0f, ticksAlive / 30.0f);
            float splitY = splitFactor * 1.0f;

            // Top half rotates clockwise and rises
            float topRot = ticksAlive * 0.015f;
            for (int i = 0; i < topHalf.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + topRot;
                double yOff = 0.5 + (i % 3) * 0.5 + splitY;
                Location target = center.clone().add(Math.cos(angle) * 1.8, yOff, Math.sin(angle) * 1.8);
                topHalf.get(i).entity().teleport(target);
                topHalf.get(i).interpolation(3, 0);
            }

            // Bottom half counter-rotates and descends
            float botRot = -ticksAlive * 0.012f;
            for (int i = 0; i < bottomHalf.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + Math.PI / 8 + botRot;
                double yOff = -0.5 - (i % 3) * 0.5 - splitY;
                Location target = center.clone().add(Math.cos(angle) * 1.8, yOff, Math.sin(angle) * 1.8);
                bottomHalf.get(i).entity().teleport(target);
                bottomHalf.get(i).interpolation(3, 0);
            }

            // Core glow pulses
            if (coreLight != null) {
                float s = 0.8f + 0.3f * (float) Math.sin(ticksAlive * Math.PI / 15);
                coreLight.scale(s, s, s);
                coreLight.interpolation(2, 0);
            }

            // Particles between halves
            if (ticksAlive % 4 == 0 && ticksAlive > 30) {
                DisplayBuilder.dustParticles(center, 10, 1.5, 240, 240, 255, 1.0f);
                w.spawnParticle(Particle.END_ROD, center, 3, 1.0, 0.3, 1.0, 0.02);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FracturedGeode(plugin); }
    }

    // ================================================================
    // 9. BLOOMING INFECTION -- Concentric rings of crystal growth that
    //    spread outward from a central budding amethyst node
    // ================================================================
    public static class BloomingInfection extends BlockDisplayAttack {

        private BlockDisplayHandle coreNode;
        private final List<BlockDisplayHandle> ring1 = new ArrayList<>();
        private final List<BlockDisplayHandle> ring2 = new ArrayList<>();
        private final List<BlockDisplayHandle> ring3 = new ArrayList<>();

        public BloomingInfection(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blooming_infection", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(5.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Core budding amethyst
            coreNode = displayBuilder.spawnBlock(center, Material.BUDDING_AMETHYST);
            coreNode.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(coreNode.entity());

            // Ring 1: 6 amethyst clusters at 1.5 radius
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.01f, 0.01f, 0.01f).glow(0, 200, 255).interpolation(3, 0);
                ring1.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring 2: 10 amethyst blocks at 3.0 radius
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                Location loc = center.clone().add(Math.cos(angle) * 3.0, 0, Math.sin(angle) * 3.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.01f, 0.01f, 0.01f).glow(128, 0, 255).interpolation(3, 0);
                ring2.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring 3: 14 dark prismarine at 4.5 radius
            for (int i = 0; i < 14; i++) {
                double angle = (Math.PI * 2 * i) / 14;
                Location loc = center.clone().add(Math.cos(angle) * 4.5, 0, Math.sin(angle) * 4.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.01f, 0.01f, 0.01f).glow(0, 200, 255).interpolation(3, 0);
                ring3.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Core pulses
            if (coreNode != null) {
                float pulse = 1.0f + 0.15f * (float) Math.sin(ticksAlive * Math.PI / 20);
                coreNode.scale(pulse, pulse, pulse);
                coreNode.interpolation(3, 0);
            }

            // Ring 1 grows in from tick 0-30
            float r1Grow = Math.min(1.0f, ticksAlive / 30.0f);
            for (BlockDisplayHandle h : ring1) {
                float s = 0.5f * r1Grow;
                h.scale(s, s * 1.5f, s);
                h.interpolation(3, 0);
            }

            // Ring 2 grows in from tick 30-60
            float r2Grow = Math.min(1.0f, Math.max(0, (ticksAlive - 30) / 30.0f));
            for (int i = 0; i < ring2.size(); i++) {
                float s = 0.4f * r2Grow;
                float bob = 0.1f * r2Grow * (float) Math.sin((ticksAlive + i * 8) * Math.PI / 25);
                ring2.get(i).scale(s, s + bob, s);
                ring2.get(i).interpolation(3, 0);
            }

            // Ring 3 grows in from tick 60-90
            float r3Grow = Math.min(1.0f, Math.max(0, (ticksAlive - 60) / 30.0f));
            for (int i = 0; i < ring3.size(); i++) {
                float s = 0.35f * r3Grow;
                ring3.get(i).scale(s, s, s);
                ring3.get(i).interpolation(3, 0);
            }

            // Particles: spreading infection wave
            if (ticksAlive % 8 == 0) {
                float particleRad = Math.min(4.5f, ticksAlive * 0.05f);
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), particleRad,
                        Particle.DUST, 12, new Particle.DustOptions(Color.fromRGB(0, 200, 255), 0.8f));
            }

            if (ticksAlive % 30 == 0 && ticksAlive <= 90) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f + ticksAlive * 0.01f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BloomingInfection(plugin); }
    }

    // ================================================================
    // 10. CRYSTALLINE TREMOR -- Ground-level shockwave of crystal
    //     spikes that ripple outward in a ring from the center
    // ================================================================
    public static class CrystallineTremor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> waveBlocks = new ArrayList<>();
        private BlockDisplayHandle epicenter;

        public CrystallineTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_tremor", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Epicenter: crying obsidian impact point
            epicenter = displayBuilder.spawnBlock(center, Material.CRYING_OBSIDIAN);
            epicenter.scale(1.0f, 0.3f, 1.0f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(epicenter.entity());

            // Wave: 16 crystal spikes in a ring, start underground
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 3.0, -2, Math.sin(angle) * 3.0);
                Material mat = (i % 3 == 0) ? Material.LARGE_AMETHYST_BUD : Material.AMETHYST_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 0.8f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
                waveBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
            DisplayBuilder.cyanDust(center, 30, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wave expansion: ring erupts upward sequentially by angle
            float waveRadius = 1.0f + ticksAlive * 0.08f;
            waveRadius = Math.min(waveRadius, 6.0f);

            for (int i = 0; i < waveBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16;

                // Each spike erupts based on wave reaching it
                float spikeRadius = 3.0f;
                float delay = spikeRadius / 0.08f; // ticks until wave reaches this spike
                int eruptTick = (int)(i * 2); // stagger by index

                float yPos;
                if (ticksAlive < eruptTick) {
                    yPos = -2.0f;
                } else if (ticksAlive < eruptTick + 15) {
                    float progress = (ticksAlive - eruptTick) / 15.0f;
                    yPos = -2.0f + progress * 3.0f;
                } else {
                    // Emerged: tremble and slowly sink
                    float sinkProgress = Math.min(1.0f, (ticksAlive - eruptTick - 15) / 200.0f);
                    float tremble = 0.08f * (float) Math.sin((ticksAlive + i * 7) * 1.2);
                    yPos = 1.0f - sinkProgress * 1.5f + tremble;
                }

                Location target = center.clone().add(Math.cos(angle) * spikeRadius, yPos, Math.sin(angle) * spikeRadius);
                waveBlocks.get(i).entity().teleport(target);
                waveBlocks.get(i).interpolation(2, 0);

                // Eruption particle
                if (ticksAlive == eruptTick) {
                    Location pLoc = center.clone().add(Math.cos(angle) * spikeRadius, 0, Math.sin(angle) * spikeRadius);
                    DisplayBuilder.cyanDust(pLoc, 5, 0.3);
                }
            }

            // Ground shake particles
            if (ticksAlive % 5 == 0 && ticksAlive < 50) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), waveRadius,
                        Particle.DUST, 16, new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f));
            }

            if (ticksAlive % 30 == 0 && ticksAlive < 100) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineTremor(plugin); }
    }
}
