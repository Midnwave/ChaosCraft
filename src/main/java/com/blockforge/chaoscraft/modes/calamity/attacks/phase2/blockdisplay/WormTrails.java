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
 * Phase 2 (4B) Block Display -- GROUP 7: WORM TRAIL STRUCTURES
 * 10 attacks representing the physical traces of DoG's passage through the arena.
 * Segmented worm body echoes, crystal scars, dimensional residue left behind
 * as the Devourer of Gods phases in and out of reality.
 *
 * Rules applied:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG palette: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 * - Damage in HP (4.0-12.0 range)
 */
public final class WormTrails {

    private WormTrails() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SegmentedScar(plugin));
        registry.register(new BurrowTrail(plugin));
        registry.register(new SlitherPath(plugin));
        registry.register(new CoiledRemnant(plugin));
        registry.register(new ScaleDebrisLine(plugin));
        registry.register(new TunnelMouth(plugin));
        registry.register(new SpineRidge(plugin));
        registry.register(new CrystalWake(plugin));
        registry.register(new EchoSegmentArc(plugin));
        registry.register(new DevourerTrackway(plugin));
    }

    // ================================================================
    // 1. SEGMENTED SCAR -- Line of worm body-segment imprints burnt
    //    into the ground, each glowing with residual energy
    // ================================================================
    public static class SegmentedScar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> scarSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> glowVeins = new ArrayList<>();

        public SegmentedScar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("segmented_scar", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 scar imprints in a curved line
            for (int i = 0; i < 8; i++) {
                double xOff = (i - 4) * 1.8;
                double zOff = Math.sin(i * 0.4) * 1.5;
                Location loc = center.clone().add(xOff, -0.3, zOff);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_BLOCK : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.2f, 0.2f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                scarSegments.add(h);
                spawnedEntities.add(h.entity());
            }

            // Glow veins: cyan stained glass between segments
            for (int i = 0; i < 7; i++) {
                double xOff = (i - 3.5) * 1.8;
                double zOff = Math.sin((i + 0.5) * 0.4) * 1.5;
                Location loc = center.clone().add(xOff, -0.4, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.8f, 0.1f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                glowVeins.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Traveling energy pulse along the scar
            int pulseIndex = (ticksAlive / 8) % scarSegments.size();
            for (int i = 0; i < scarSegments.size(); i++) {
                float glowScale = (i == pulseIndex) ? 1.5f : 1.2f;
                scarSegments.get(i).scale(glowScale, 0.2f, 1.0f);
                scarSegments.get(i).interpolation(3, 0);
            }

            // Scar segments smolder with ambient rise
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % scarSegments.size();
                Location sLoc = scarSegments.get(idx).entity().getLocation().add(0, 0.3, 0);
                DisplayBuilder.dustParticles(sLoc, 4, 0.4, 0, 200, 255, 0.8f);
            }

            // Particles: energy traveling along the path
            if (ticksAlive % 4 == 0) {
                int pIdx = (ticksAlive / 4) % 8;
                double xOff = (pIdx - 4) * 1.8;
                double zOff = Math.sin(pIdx * 0.4) * 1.5;
                Location pLoc = center.clone().add(xOff, 0.3, zOff);
                w.spawnParticle(Particle.END_ROD, pLoc, 2, 0.1, 0.2, 0.1, 0.01);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SegmentedScar(plugin); }
    }

    // ================================================================
    // 2. BURROW TRAIL -- Rising mounds of crystal-infested ground
    //    tracing DoG's underground path
    // ================================================================
    public static class BurrowTrail extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> moundBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crystalTops = new ArrayList<>();

        public BurrowTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("burrow_trail", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 ground mounds in a winding trail
            for (int i = 0; i < 10; i++) {
                double xOff = (i - 5) * 1.5;
                double zOff = Math.sin(i * 0.6) * 2.0;
                Location loc = center.clone().add(xOff, -0.5, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.3f, 0.3f, 1.3f).glow(0, 200, 255).interpolation(3, 0);
                moundBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crystal growths on top of each mound
            for (int i = 0; i < 10; i++) {
                double xOff = (i - 5) * 1.5;
                double zOff = Math.sin(i * 0.6) * 2.0;
                Location loc = center.clone().add(xOff, -0.2, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.01f, 0.01f, 0.01f).glow(128, 0, 255).interpolation(3, 0);
                crystalTops.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sequential mound rise: each rises 5 ticks apart
            for (int i = 0; i < moundBlocks.size(); i++) {
                int riseStart = i * 5;
                double xOff = (i - 5) * 1.5;
                double zOff = Math.sin(i * 0.6) * 2.0;

                if (ticksAlive >= riseStart && ticksAlive < riseStart + 20) {
                    float progress = (ticksAlive - riseStart) / 20.0f;
                    float yRise = progress * 0.8f;
                    Location target = center.clone().add(xOff, -0.5 + yRise, zOff);
                    moundBlocks.get(i).entity().teleport(target);
                    moundBlocks.get(i).scale(1.3f, 0.3f + yRise * 0.5f, 1.3f);
                    moundBlocks.get(i).interpolation(3, 0);

                    // Crystal top grows
                    float crystalScale = progress * 0.5f;
                    crystalTops.get(i).scale(crystalScale, crystalScale * 1.5f, crystalScale);
                    crystalTops.get(i).interpolation(3, 0);
                    Location cLoc = center.clone().add(xOff, -0.2 + yRise + 0.3, zOff);
                    crystalTops.get(i).entity().teleport(cLoc);
                } else if (ticksAlive >= riseStart + 20) {
                    // Idle tremble
                    float tremble = 0.03f * (float) Math.sin((ticksAlive + i * 10) * 1.0);
                    Location rest = center.clone().add(xOff, 0.3 + tremble, zOff);
                    moundBlocks.get(i).entity().teleport(rest);
                }

                // Rise particles
                if (ticksAlive == riseStart) {
                    Location pLoc = center.clone().add(xOff, 0, zOff);
                    DisplayBuilder.cyanDust(pLoc, 6, 0.5);
                    DisplayBuilder.playSound(pLoc, Sound.BLOCK_STONE_PLACE, 0.3f, 0.6f + i * 0.05f);
                }
            }

            // Ambient trail particles
            if (ticksAlive % 8 == 0 && ticksAlive > 50) {
                int idx = (ticksAlive / 8) % 10;
                double xOff = (idx - 5) * 1.5;
                double zOff = Math.sin(idx * 0.6) * 2.0;
                w.spawnParticle(Particle.END_ROD, center.clone().add(xOff, 0.8, zOff), 1, 0.1, 0.2, 0.1, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BurrowTrail(plugin); }
    }

    // ================================================================
    // 3. SLITHER PATH -- S-curve of polished blackstone and amethyst
    //    tracing a serpentine movement pattern
    // ================================================================
    public static class SlitherPath extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pathBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> edgeSpines = new ArrayList<>();

        public SlitherPath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slither_path", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // S-curve path: 12 polished blackstone segments
            for (int i = 0; i < 12; i++) {
                double t = (i / 11.0) * Math.PI * 2;
                double xOff = (i - 6) * 1.2;
                double zOff = Math.sin(t) * 2.5;
                Location loc = center.clone().add(xOff, -0.3, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.15f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                pathBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Edge spines: amethyst spikes along the path edges
            for (int i = 0; i < 12; i++) {
                double t = (i / 11.0) * Math.PI * 2;
                double xOff = (i - 6) * 1.2;
                double zOff = Math.sin(t) * 2.5 + ((i % 2 == 0) ? 0.6 : -0.6);
                Location loc = center.clone().add(xOff, 0, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                float height = 0.3f + (float)(Math.random() * 0.6);
                h.scale(0.3f, height, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                edgeSpines.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Traveling glow along the path
            int glowIndex = (ticksAlive / 6) % pathBlocks.size();
            for (int i = 0; i < pathBlocks.size(); i++) {
                if (i == glowIndex) {
                    pathBlocks.get(i).glow(240, 240, 255);
                } else {
                    pathBlocks.get(i).glow(0, 200, 255);
                }
            }

            // Edge spines oscillate
            for (int i = 0; i < edgeSpines.size(); i++) {
                float bob = 0.15f * (float) Math.sin((ticksAlive + i * 10) * Math.PI / 30);
                double t = (i / 11.0) * Math.PI * 2;
                double xOff = (i - 6) * 1.2;
                double zOff = Math.sin(t) * 2.5 + ((i % 2 == 0) ? 0.6 : -0.6);
                Location target = center.clone().add(xOff, bob, zOff);
                edgeSpines.get(i).entity().teleport(target);
            }

            // Traveling particle along path
            if (ticksAlive % 3 == 0) {
                int pIdx = (ticksAlive / 3) % 12;
                double t = (pIdx / 11.0) * Math.PI * 2;
                double xOff = (pIdx - 6) * 1.2;
                double zOff = Math.sin(t) * 2.5;
                Location pLoc = center.clone().add(xOff, 0.3, zOff);
                DisplayBuilder.dustParticles(pLoc, 3, 0.2, 240, 240, 255, 0.8f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SlitherPath(plugin); }
    }

    // ================================================================
    // 4. COILED REMNANT -- Spiral of crystal blocks forming a coiled
    //    worm body section frozen mid-rotation
    // ================================================================
    public static class CoiledRemnant extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coilBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spineNodes = new ArrayList<>();

        public CoiledRemnant(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("coiled_remnant", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Helical coil: 16 blocks in a helix, 2 full rotations
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 8; // 2 full rotations
                double radius = 2.0;
                double yOff = i * 0.4;
                Location loc = center.clone().add(Math.cos(angle) * radius, yOff, Math.sin(angle) * radius);
                Material mat = (i % 3 == 0) ? Material.DARK_PRISMARINE : Material.AMETHYST_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.6f, 0.7f).glow(0, 200, 255).interpolation(3, 0);
                coilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Dorsal spines: every 4th segment gets a spine
            for (int i = 0; i < 4; i++) {
                int idx = i * 4;
                double angle = (Math.PI * 2 * idx) / 8;
                double yOff = idx * 0.4 + 0.5;
                Location loc = center.clone().add(
                        Math.cos(angle) * 2.3, yOff, Math.sin(angle) * 2.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.4f, 0.8f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                spineNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Slow rotation of entire coil
            float rotOffset = ticksAlive * 0.01f;

            for (int i = 0; i < coilBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + rotOffset;
                double yOff = i * 0.4;
                // Subtle undulation perpendicular to coil
                float undulate = 0.15f * (float) Math.sin((ticksAlive + i * 5) * Math.PI / 20);
                double radius = 2.0 + undulate;
                Location target = center.clone().add(Math.cos(angle) * radius, yOff, Math.sin(angle) * radius);
                coilBlocks.get(i).entity().teleport(target);
                coilBlocks.get(i).interpolation(3, 0);
            }

            // Spines track their parent segments
            for (int i = 0; i < spineNodes.size(); i++) {
                int idx = i * 4;
                double angle = (Math.PI * 2 * idx) / 8 + rotOffset;
                double yOff = idx * 0.4 + 0.5;
                float bob = 0.1f * (float) Math.sin((ticksAlive + i * 15) * Math.PI / 25);
                Location target = center.clone().add(
                        Math.cos(angle) * 2.3, yOff + bob, Math.sin(angle) * 2.3);
                spineNodes.get(i).entity().teleport(target);
            }

            // Energy pulse traveling up the coil
            if (ticksAlive % 4 == 0) {
                int pulseIdx = (ticksAlive / 4) % coilBlocks.size();
                Location pLoc = coilBlocks.get(pulseIdx).entity().getLocation().add(0, 0.3, 0);
                DisplayBuilder.cyanDust(pLoc, 3, 0.3);
            }

            if (ticksAlive % 70 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CoiledRemnant(plugin); }
    }

    // ================================================================
    // 5. SCALE DEBRIS LINE -- Scattered crystal scale fragments
    //    drifting slowly outward from DoG's passage corridor
    // ================================================================
    public static class ScaleDebrisLine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> debris = new ArrayList<>();
        private final double[] initialAngles = new double[15];
        private final float[] driftSpeeds = new float[15];

        public ScaleDebrisLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scale_debris_line", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 15; i++) {
                double xOff = (Math.random() - 0.5) * 6;
                double yOff = Math.random() * 2;
                double zOff = (Math.random() - 0.5) * 3;
                Location loc = center.clone().add(xOff, yOff, zOff);
                Material mat;
                int choice = i % 4;
                if (choice == 0) mat = Material.AMETHYST_CLUSTER;
                else if (choice == 1) mat = Material.AMETHYST_BLOCK;
                else if (choice == 2) mat = Material.DARK_PRISMARINE;
                else mat = Material.PRISMARINE;
                float s = 0.2f + (float)(Math.random() * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(s, s, s).glow(0, 200, 255).interpolation(2, 0);
                debris.add(h);
                spawnedEntities.add(h.entity());

                initialAngles[i] = Math.random() * Math.PI * 2;
                driftSpeeds[i] = 0.01f + (float)(Math.random() * 0.02);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Each debris piece tumbles and drifts
            for (int i = 0; i < debris.size(); i++) {
                BlockDisplay bd = debris.get(i).entity();
                Location loc = bd.getLocation();

                // Drift outward slowly
                double dx = Math.cos(initialAngles[i]) * driftSpeeds[i];
                double dz = Math.sin(initialAngles[i]) * driftSpeeds[i];
                double dy = Math.sin(ticksAlive * 0.03 + i) * 0.005;
                bd.teleport(loc.clone().add(dx, dy, dz));

                // Tumble rotation
                debris.get(i).rotate(ticksAlive * (0.02f + i * 0.005f), 0.5f, 1, 0.3f);
                debris.get(i).interpolation(2, 0);
            }

            // Occasional particles from debris
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % debris.size();
                Location dLoc = debris.get(idx).entity().getLocation();
                w.spawnParticle(Particle.END_ROD, dLoc, 1, 0.1, 0.1, 0.1, 0.005);
            }

            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScaleDebrisLine(plugin); }
    }

    // ================================================================
    // 6. TUNNEL MOUTH -- Open portal-like cavity ringed with crystal
    //    teeth, representing where DoG burrowed through dimensions
    // ================================================================
    public static class TunnelMouth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> teethBlocks = new ArrayList<>();
        private BlockDisplayHandle throatGlow;

        public TunnelMouth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tunnel_mouth", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ring frame: 10 polished blackstone in an oval
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double x = Math.cos(angle) * 2.0;
                double y = Math.sin(angle) * 2.5 + 2.5;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.8f, 0.5f).glow(0, 200, 255).interpolation(3, 0);
                ringFrame.add(h);
                spawnedEntities.add(h.entity());
            }

            // Teeth: 8 amethyst clusters on inner ring pointing inward
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double x = Math.cos(angle) * 1.4;
                double y = Math.sin(angle) * 1.8 + 2.5;
                Location loc = center.clone().add(x, y, 0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.4f, 0.6f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                teethBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Throat glow: sea lantern at center
            throatGlow = displayBuilder.spawnBlock(center.clone().add(0, 2.5, -0.5), Material.SEA_LANTERN);
            throatGlow.scale(1.0f, 1.0f, 0.3f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(throatGlow.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Teeth flex inward periodically (chewing motion)
            float flex = 0.15f * (float) Math.sin(ticksAlive * Math.PI / 30);
            for (int i = 0; i < teethBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double x = Math.cos(angle) * (1.4 - flex);
                double y = Math.sin(angle) * (1.8 - flex) + 2.5;
                Location target = center.clone().add(x, y, 0.2);
                teethBlocks.get(i).entity().teleport(target);
                teethBlocks.get(i).interpolation(2, 0);
            }

            // Throat glow pulse
            if (throatGlow != null) {
                float s = 1.0f + 0.2f * (float) Math.sin(ticksAlive * Math.PI / 15);
                throatGlow.scale(s, s, 0.3f);
                throatGlow.interpolation(2, 0);
            }

            // Particles: dimensional breath from the tunnel
            if (ticksAlive % 4 == 0) {
                Location tLoc = center.clone().add(0, 2.5, 0.5);
                w.spawnParticle(Particle.PORTAL, tLoc, 5, 0.5, 0.8, 0.2, 0.1);
            }
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 2.5, 0.3), 5, 1.0,
                        128, 0, 255, 1.0f);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TunnelMouth(plugin); }
    }

    // ================================================================
    // 7. SPINE RIDGE -- Linear ridge of dorsal spine formations
    //    resembling the top of DoG's body frozen in crystal
    // ================================================================
    public static class SpineRidge extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ridgeBase = new ArrayList<>();
        private final List<BlockDisplayHandle> spines = new ArrayList<>();

        public SpineRidge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spine_ridge", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(550);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ridge base: 8 dark prismarine blocks in a line
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(i - 4, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.9f, 0.5f, 0.7f).glow(0, 200, 255).interpolation(2, 0);
                ridgeBase.add(h);
                spawnedEntities.add(h.entity());
            }

            // Spines: alternating heights of amethyst clusters
            float[] heights = {1.0f, 1.5f, 2.2f, 1.8f, 2.5f, 1.3f, 2.0f, 1.1f};
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(i - 4, 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.35f, heights[i] * 0.5f, 0.35f).glow(128, 0, 255).interpolation(3, 0);
                spines.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spine wave: ripple traveling along the ridge
            for (int i = 0; i < spines.size(); i++) {
                float wave = 0.2f * (float) Math.sin((ticksAlive - i * 8) * Math.PI / 20);
                float baseHeight = new float[]{1.0f, 1.5f, 2.2f, 1.8f, 2.5f, 1.3f, 2.0f, 1.1f}[i];
                spines.get(i).scale(0.35f, (baseHeight + wave) * 0.5f, 0.35f);
                spines.get(i).interpolation(3, 0);
            }

            // Energy traveling along ridge
            if (ticksAlive % 3 == 0) {
                int idx = (ticksAlive / 3) % 8;
                Location pLoc = center.clone().add(idx - 4, 1.5, 0);
                DisplayBuilder.cyanDust(pLoc, 3, 0.2);
            }

            // Ambient sparks between spine tips
            if (ticksAlive % 10 == 0) {
                int from = (ticksAlive / 10) % 7;
                Location start = spines.get(from).entity().getLocation().add(0, 0.5, 0);
                Location end = spines.get(from + 1).entity().getLocation().add(0, 0.5, 0);
                DisplayBuilder.particleLine(start, end, Particle.ELECTRIC_SPARK, 3, null);
            }

            if (ticksAlive % 55 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpineRidge(plugin); }
    }

    // ================================================================
    // 8. CRYSTAL WAKE -- V-shaped wave of crystal fragments trailing
    //    behind DoG's recent passage like a boat wake
    // ================================================================
    public static class CrystalWake extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private BlockDisplayHandle apexBlock;

        public CrystalWake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_wake", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Apex: amethyst block at the V-point
            apexBlock = displayBuilder.spawnBlock(center, Material.AMETHYST_BLOCK);
            apexBlock.scale(0.8f, 0.8f, 0.8f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(apexBlock.entity());

            // Left arm: 6 blocks spreading left and back
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(-(i + 1) * 0.9, 0, (i + 1) * 0.7);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_CLUSTER : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float s = 0.6f - (i * 0.05f);
                h.scale(s, s, s).glow(0, 200, 255).interpolation(3, 0);
                leftArm.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right arm: 6 blocks spreading right and back
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add((i + 1) * 0.9, 0, (i + 1) * 0.7);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_CLUSTER : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float s = 0.6f - (i * 0.05f);
                h.scale(s, s, s).glow(0, 200, 255).interpolation(3, 0);
                rightArm.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Arms spread outward over time
            float spread = Math.min(1.5f, ticksAlive * 0.01f);

            for (int i = 0; i < leftArm.size(); i++) {
                float bob = 0.1f * (float) Math.sin((ticksAlive + i * 8) * Math.PI / 25);
                Location target = center.clone().add(
                        -(i + 1) * (0.9 + spread * 0.3), bob, (i + 1) * 0.7);
                leftArm.get(i).entity().teleport(target);
                leftArm.get(i).rotate(ticksAlive * 0.03f + i * 0.5f, 0, 1, 0);
                leftArm.get(i).interpolation(2, 0);
            }

            for (int i = 0; i < rightArm.size(); i++) {
                float bob = 0.1f * (float) Math.sin((ticksAlive + i * 8 + 15) * Math.PI / 25);
                Location target = center.clone().add(
                        (i + 1) * (0.9 + spread * 0.3), bob, (i + 1) * 0.7);
                rightArm.get(i).entity().teleport(target);
                rightArm.get(i).rotate(-ticksAlive * 0.03f + i * 0.5f, 0, 1, 0);
                rightArm.get(i).interpolation(2, 0);
            }

            // Apex pulse
            if (apexBlock != null) {
                float s = 0.8f + 0.15f * (float) Math.sin(ticksAlive * Math.PI / 15);
                apexBlock.scale(s, s, s);
                apexBlock.interpolation(2, 0);
            }

            // Wake particles trailing behind
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 2), 5, 1.5);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalWake(plugin); }
    }

    // ================================================================
    // 9. ECHO SEGMENT ARC -- Curved arc of phantom body segments
    //    hanging in the air where DoG recently phased
    // ================================================================
    public static class EchoSegmentArc extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arcSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> connectorRods = new ArrayList<>();

        public EchoSegmentArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_segment_arc", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(450);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Arc of 10 segments in a parabolic curve
            for (int i = 0; i < 10; i++) {
                double t = (i - 5) * 1.5;
                double yOff = 4.0 - (t * t) * 0.12; // Parabola
                Location loc = center.clone().add(t, yOff, 0);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_BLOCK : Material.CYAN_STAINED_GLASS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 0.6f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
                arcSegments.add(h);
                spawnedEntities.add(h.entity());
            }

            // Connectors between segments
            for (int i = 0; i < 9; i++) {
                double t1 = (i - 5) * 1.5;
                double t2 = (i - 4) * 1.5;
                double y1 = 4.0 - (t1 * t1) * 0.12;
                double y2 = 4.0 - (t2 * t2) * 0.12;
                Location loc = center.clone().add((t1 + t2) / 2, (y1 + y2) / 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.15f, 0.15f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
                connectorRods.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Arc segments bob individually with wave propagation
            for (int i = 0; i < arcSegments.size(); i++) {
                double t = (i - 5) * 1.5;
                double baseY = 4.0 - (t * t) * 0.12;
                float wave = 0.25f * (float) Math.sin((ticksAlive - i * 6) * Math.PI / 20);
                Location target = center.clone().add(t, baseY + wave, 0);
                arcSegments.get(i).entity().teleport(target);
                arcSegments.get(i).interpolation(3, 0);
            }

            // Fade effect: segments pulse transparency via scale
            for (int i = 0; i < arcSegments.size(); i++) {
                float fade = 0.6f + 0.2f * (float) Math.sin((ticksAlive + i * 10) * Math.PI / 40);
                arcSegments.get(i).scale(0.8f * fade, 0.6f * fade, 0.8f * fade);
            }

            // Energy pulse traveling along the arc
            if (ticksAlive % 3 == 0) {
                int idx = (ticksAlive / 3) % 10;
                Location pLoc = arcSegments.get(idx).entity().getLocation();
                DisplayBuilder.dustParticles(pLoc, 3, 0.2, 240, 240, 255, 0.8f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoSegmentArc(plugin); }
    }

    // ================================================================
    // 10. DEVOURER TRACKWAY -- Massive parallel grooves carved into
    //     the ground with crystal residue, showing DoG's full-body
    //     passage like fossil trackways
    // ================================================================
    public static class DevourerTrackway extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftGroove = new ArrayList<>();
        private final List<BlockDisplayHandle> rightGroove = new ArrayList<>();
        private final List<BlockDisplayHandle> residueCrystals = new ArrayList<>();

        public DevourerTrackway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devourer_trackway", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(550);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left groove: 8 dark prismarine depression blocks
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(i - 4, -0.4, -1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.9f, 0.2f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                leftGroove.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right groove
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(i - 4, -0.4, 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.9f, 0.2f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                rightGroove.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crystal residue between grooves
            for (int i = 0; i < 6; i++) {
                double xOff = (i - 3) * 1.3 + 0.5;
                double zOff = (Math.random() - 0.5) * 2.0;
                Location loc = center.clone().add(xOff, 0, zOff);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_CLUSTER : Material.BUDDING_AMETHYST;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.3f, 0.4f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                residueCrystals.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Grooves pulse with energy
            int pulseIdx = (ticksAlive / 5) % 8;
            for (int i = 0; i < 8; i++) {
                float glow = (i == pulseIdx) ? 0.35f : 0.2f;
                leftGroove.get(i).scale(0.9f, glow, 0.6f);
                leftGroove.get(i).interpolation(2, 0);
                rightGroove.get(i).scale(0.9f, glow, 0.6f);
                rightGroove.get(i).interpolation(2, 0);
            }

            // Residue crystals grow slowly over 50 ticks then tremble
            float growFactor = Math.min(1.0f, ticksAlive / 50.0f);
            for (int i = 0; i < residueCrystals.size(); i++) {
                float tremble = growFactor * 0.05f * (float) Math.sin((ticksAlive + i * 12) * 1.3);
                residueCrystals.get(i).scale(0.3f * growFactor, (0.4f + tremble) * growFactor, 0.3f * growFactor);
                residueCrystals.get(i).interpolation(3, 0);
            }

            // Traveling energy along grooves
            if (ticksAlive % 5 == 0) {
                Location lLoc = center.clone().add(pulseIdx - 4, 0, -1.5);
                Location rLoc = center.clone().add(pulseIdx - 4, 0, 1.5);
                DisplayBuilder.cyanDust(lLoc, 3, 0.2);
                DisplayBuilder.cyanDust(rLoc, 3, 0.2);
            }

            if (ticksAlive % 65 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DevourerTrackway(plugin); }
    }
}
