package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay;

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
 * Phase 3 Block Display — GROUP 3: PORTAL AND RIFT FRAMES (Structures 18-25)
 * Dimensional tears appearing in the arena — windows into wrong.
 * Where the Nether's overwrite of the End has punched through.
 *
 * Dweller theme: Brimstone Inversion — Hell consuming The End.
 * Palette: obsidian, crying obsidian, blackstone, magma, soul soil/sand.
 * NO status effects. Damage in HP (not hearts).
 */
public final class PortalRiftFrames {

    private PortalRiftFrames() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DimensionalRiftA(plugin));
        registry.register(new DimensionalRiftB(plugin));
        registry.register(new LavaVeinColumn(plugin));
        registry.register(new BasaltGeode(plugin));
        registry.register(new SoulPillarTrio(plugin));
        registry.register(new NetherCeilingFragment(plugin));
        registry.register(new BrimstoneSpireRing(plugin));
        registry.register(new DwellersMark(plugin));
    }

    // ================================================================
    // 18. DIMENSIONAL RIFT FRAME A — Northwest, obsidian/crying obsidian
    //     frame 4x8, dark membrane interior, tear-in spawn animation
    // ================================================================
    public static class DimensionalRiftA extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneBlocks = new ArrayList<>();
        private boolean fullyOpened = false;

        public DimensionalRiftA(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_rift_a", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location riftCenter = center.clone().add(-14, 4, -14);

            // Frame: alternating obsidian/crying obsidian rectangle 4 wide x 8 tall
            // Bottom edge
            for (int x = 0; x < 4; x++) {
                Material mat = (x % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftCenter.clone().add(x - 2, -4, 0), mat);
                h.glow(128, 0, 255).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Top edge
            for (int x = 0; x < 4; x++) {
                Material mat = (x % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftCenter.clone().add(x - 2, 4, 0), mat);
                h.glow(128, 0, 255).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Left edge
            for (int y = -3; y <= 3; y++) {
                Material mat = (y % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftCenter.clone().add(-2, y, 0), mat);
                h.glow(128, 0, 255).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right edge
            for (int y = -3; y <= 3; y++) {
                Material mat = (y % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftCenter.clone().add(2, y, 0), mat);
                h.glow(128, 0, 255).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior membrane: BLACK_CONCRETE blocks scaled flat (dark membrane)
            for (int x = -1; x <= 1; x++) {
                for (int y = -3; y <= 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            riftCenter.clone().add(x, y, 0), Material.BLACK_CONCRETE);
                    h.scale(1.0f, 1.0f, 0.05f).glow(30, 0, 60).interpolation(3, 0);
                    membraneBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Spawn sounds: tear-in effect
            DisplayBuilder.playSound(riftCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location riftCenter = center.clone().add(-14, 4, -14);

            // Tear-in animation: first 20 ticks, frame materializes from center outward
            if (ticksAlive <= 20) {
                // Electric spark line at rift center
                for (int y = -4; y <= 4; y++) {
                    Location sparkLoc = riftCenter.clone().add(0, y, 0);
                    float spread = ticksAlive * 0.1f;
                    w.spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 3,
                            spread, 0.1, 0.1, 0);
                }
                // Sound stagger
                if (ticksAlive == 5 || ticksAlive == 10 || ticksAlive == 15) {
                    DisplayBuilder.playSound(riftCenter,
                            Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.7f);
                }
            }

            // Membrane materializes at tick 20 with portal burst
            if (ticksAlive == 20) {
                fullyOpened = true;
                w.spawnParticle(Particle.PORTAL, riftCenter, 60, 1.5, 3, 0.5, 0.5);
            }

            // Membrane ripple: Z-position wave propagating downward
            if (ticksAlive > 20) {
                for (int i = 0; i < membraneBlocks.size(); i++) {
                    int row = i % 7; // 7 rows (y = -3 to 3)
                    float wave = (float) Math.sin((ticksAlive * 0.1) - (row * 0.5)) * 0.1f;
                    float prevWave = (float) Math.sin(((ticksAlive - 1) * 0.1) - (row * 0.5)) * 0.1f;
                    BlockDisplay bd = membraneBlocks.get(i).entity();
                    bd.teleport(bd.getLocation().add(0, 0, wave - prevWave));
                }
            }

            // Frame blocks drift inward/outward on normal axis
            if (ticksAlive > 20) {
                float frameDrift = (float) Math.sin(ticksAlive * Math.PI / 40.0) * 0.15f;
                for (BlockDisplayHandle h : frameBlocks) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().add(0, 0, (frameDrift - (float) Math.sin((ticksAlive - 1) * Math.PI / 40.0) * 0.15f) * 0.5));
                }
            }

            // Portal particles from membrane surface
            if (ticksAlive % 2 == 0 && fullyOpened) {
                w.spawnParticle(Particle.PORTAL, riftCenter, 10, 1.5, 3, 0.2, 0.3);
            }

            // Dragon breath orbiting frame perimeter
            if (ticksAlive % 5 == 0 && fullyOpened) {
                double orbitAngle = ticksAlive * 0.1;
                double ox = Math.cos(orbitAngle) * 2.5;
                double oy = Math.sin(orbitAngle) * 4.5;
                Location orbitLoc = riftCenter.clone().add(ox, oy, 0);
                w.spawnParticle(Particle.DRAGON_BREATH, orbitLoc, 3, 0.1, 0.1, 0.1, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalRiftA(plugin); }
    }

    // ================================================================
    // 19. DIMENSIONAL RIFT FRAME B — Southeast, blackstone/polished blackstone,
    //     red interior, upward wave propagation, soul fire perimeter
    // ================================================================
    public static class DimensionalRiftB extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneBlocks = new ArrayList<>();
        private boolean fullyOpened = false;

        public DimensionalRiftB(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_rift_b", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location riftCenter = center.clone().add(14, 4, 14);

            // Frame: blackstone / polished blackstone
            for (int x = 0; x < 4; x++) {
                Material mat = (x % 2 == 0) ? Material.BLACKSTONE : Material.POLISHED_BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftCenter.clone().add(x - 2, -4, 0), mat);
                h.glow(0, 150, 255).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int x = 0; x < 4; x++) {
                Material mat = (x % 2 == 0) ? Material.POLISHED_BLACKSTONE : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftCenter.clone().add(x - 2, 4, 0), mat);
                h.glow(0, 150, 255).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int y = -3; y <= 3; y++) {
                Material mat = (y % 2 == 0) ? Material.BLACKSTONE : Material.POLISHED_BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftCenter.clone().add(-2, y, 0), mat);
                h.glow(0, 150, 255).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int y = -3; y <= 3; y++) {
                Material mat = (y % 2 == 0) ? Material.POLISHED_BLACKSTONE : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftCenter.clone().add(2, y, 0), mat);
                h.glow(0, 150, 255).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior: red stained glass membrane (deep red)
            for (int x = -1; x <= 1; x++) {
                for (int y = -3; y <= 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            riftCenter.clone().add(x, y, 0), Material.RED_STAINED_GLASS);
                    h.scale(1.0f, 1.0f, 0.05f).glow(200, 0, 50).interpolation(3, 0);
                    membraneBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Delayed spawn (20 ticks after Rift A — handled by scheduler)
            DisplayBuilder.playSound(riftCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location riftCenter = center.clone().add(14, 4, 14);

            // Tear-in animation (same as Rift A)
            if (ticksAlive <= 20) {
                for (int y = -4; y <= 4; y++) {
                    Location sparkLoc = riftCenter.clone().add(0, y, 0);
                    float spread = ticksAlive * 0.1f;
                    w.spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 3,
                            spread, 0.1, 0.1, 0);
                }
                if (ticksAlive == 5 || ticksAlive == 10 || ticksAlive == 15) {
                    DisplayBuilder.playSound(riftCenter,
                            Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.7f);
                }
            }

            if (ticksAlive == 20) {
                fullyOpened = true;
                w.spawnParticle(Particle.REVERSE_PORTAL, riftCenter, 60, 1.5, 3, 0.5, 0.5);
            }

            // Membrane ripple: wave propagates UPWARD (opposite of Rift A)
            if (ticksAlive > 20) {
                for (int i = 0; i < membraneBlocks.size(); i++) {
                    int row = i % 7;
                    float wave = (float) Math.sin((ticksAlive * 0.1) + (row * 0.5)) * 0.1f;
                    float prevWave = (float) Math.sin(((ticksAlive - 1) * 0.1) + (row * 0.5)) * 0.1f;
                    BlockDisplay bd = membraneBlocks.get(i).entity();
                    bd.teleport(bd.getLocation().add(0, 0, wave - prevWave));
                }
            }

            // Frame drift
            if (ticksAlive > 20) {
                float frameDrift = (float) Math.sin(ticksAlive * Math.PI / 40.0) * 0.15f;
                float prevDrift = (float) Math.sin((ticksAlive - 1) * Math.PI / 40.0) * 0.15f;
                for (BlockDisplayHandle h : frameBlocks) {
                    h.entity().teleport(h.entity().getLocation().add(0, 0, (frameDrift - prevDrift) * 0.5));
                }
            }

            // Reverse portal particles from interior
            if (ticksAlive % 2 == 0 && fullyOpened) {
                w.spawnParticle(Particle.REVERSE_PORTAL, riftCenter, 10, 1.5, 3, 0.2, 0.3);
            }

            // Soul fire flame orbiting frame perimeter
            if (ticksAlive % 5 == 0 && fullyOpened) {
                double orbitAngle = ticksAlive * 0.1;
                double ox = Math.cos(orbitAngle) * 2.5;
                double oy = Math.sin(orbitAngle) * 4.5;
                Location orbitLoc = riftCenter.clone().add(ox, oy, 0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, orbitLoc, 3, 0.1, 0.1, 0.1, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalRiftB(plugin); }
    }

    // ================================================================
    // 20. LAVA VEIN COLUMN — Cracked-ground vent, Y+0 to Y+14,
    //     magma base narrowing to particle lava spout with glass planes
    // ================================================================
    public static class LavaVeinColumn extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> glassPlanes = new ArrayList<>();

        public LavaVeinColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_vein_column", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(6, 0, -6);

            // Base: triangular footprint of 3 magma blocks at Y+0
            double[][] baseOffsets = {{0, 0, 0}, {1, 0, 0}, {0.5, 0, 0.87}};
            for (double[] off : baseOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        base.clone().add(off[0], off[1], off[2]), Material.MAGMA_BLOCK);
                h.glow(255, 100, 0).interpolation(3, 0);
                columnBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Narrowing column: 2 magma at Y+2, then alternating up to Y+10
            for (int y = 2; y <= 10; y += 2) {
                int blocksAtLevel = (y <= 4) ? 2 : 1;
                for (int b = 0; b < blocksAtLevel; b++) {
                    double xOff = (blocksAtLevel > 1) ? (b - 0.5) * 0.5 : 0;
                    Material mat = (y % 4 == 0) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            base.clone().add(0.5 + xOff, y, 0.5), mat);
                    float taper = 1.0f - (y * 0.05f);
                    h.scale(taper, 1.0f, taper).glow(255, 100, 0).interpolation(3, 0);
                    columnBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Orange stained glass planes at Y+11 and Y+13
            for (int glassY : new int[]{11, 13}) {
                BlockDisplayHandle gp = displayBuilder.spawnBlock(
                        base.clone().add(0, glassY, 0), Material.ORANGE_STAINED_GLASS);
                gp.scale(2.0f, 0.1f, 2.0f).glow(255, 100, 0).interpolation(3, 0);
                glassPlanes.add(gp);
                spawnedEntities.add(gp.entity());
            }

            DisplayBuilder.playSound(base, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(6, 0, -6);

            // Lava particle column from Y+10 to Y+14 (30/sec -> every tick, ~1-2)
            if (ticksAlive % 1 == 0) {
                double particleY = 10 + Math.random() * 4.0;
                // Surge: extend to Y+18 for 5 ticks every 60 ticks
                int tickInCycle = ticksAlive % 60;
                if (tickInCycle >= 55) {
                    particleY = 10 + Math.random() * 8.0; // Surge to Y+18
                }
                Location particleLoc = base.clone().add(0.5, particleY, 0.5);
                w.spawnParticle(Particle.LAVA, particleLoc, 2, 0.2, 0.5, 0.2, 0);
            }

            // Glass plane Y-axis rotation: 1 deg/tick
            float glassRot = (float) Math.toRadians(ticksAlive);
            for (BlockDisplayHandle gp : glassPlanes) {
                gp.rotate(glassRot, 0, 1, 0);
                gp.interpolation(3, 0);
            }

            // Base magma blocks brightness pulse bottom-up
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < columnBlocks.size(); i++) {
                    int pulsePhase = (ticksAlive / 5 + i) % 6;
                    int brightness = (pulsePhase < 3) ? 15 : 8;
                    columnBlocks.get(i).brightness(brightness, brightness);
                }
            }

            // Crimson dust at base
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.crimsonDust(base.clone().add(0.5, 1, 0.5), 5, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaVeinColumn(plugin); }
    }

    // ================================================================
    // 21. BASALT GEODE — Broken sphere of basalt at ground level,
    //     cracked open revealing crystal interior, shell fragments
    // ================================================================
    public static class BasaltGeode extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shellBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crystalBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fragmentPlates = new ArrayList<>();

        public BasaltGeode(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("basalt_geode", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location geodeCenter = center.clone().add(-10, -3, 10);

            // Rough sphere shell: basalt blocks arranged in lower hemisphere
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 16; i++) {
                double y = -1.0 + (1.0 * i / 15.0); // Only lower half
                if (y > 0.3) continue; // Skip top (cracked open)
                double radiusAtY = Math.sqrt(Math.max(0, 1 - y * y));
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * radiusAtY * 2.5;
                double z = Math.sin(theta) * radiusAtY * 2.5;

                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        geodeCenter.clone().add(x, y * 2.5, z), Material.BASALT);
                h.glow(200, 0, 50).interpolation(3, 0);
                shellBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior crystals: amethyst cluster blocks with lava glow
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * 0.8;
                double z = Math.sin(angle) * 0.8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        geodeCenter.clone().add(x, 0.5, z), Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.6f, 0.4f).glow(255, 100, 0).interpolation(3, 0);
                float randRot = (float) (Math.random() * Math.PI);
                h.rotate(randRot, (float) Math.random(), (float) Math.random(), (float) Math.random());
                crystalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Polished basalt interior lining
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4 + 0.4;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        geodeCenter.clone().add(x, -0.5, z), Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.5f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
                shellBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Shell fragment plates: 4 basalt plates leaning outward
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        geodeCenter.clone().add(x, 1.0, z), Material.BASALT);
                h.scale(2.0f, 0.2f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                fragmentPlates.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(geodeCenter, Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location geodeCenter = center.clone().add(-10, 0, 10);

            // Eruption: rise from Y-3 over 40 ticks
            if (ticksAlive <= 40) {
                float rise = ticksAlive / 40.0f;
                for (BlockDisplayHandle h : shellBlocks) {
                    h.entity().teleport(h.entity().getLocation().add(0, 3.0 / 40.0, 0));
                }
                for (BlockDisplayHandle h : crystalBlocks) {
                    h.entity().teleport(h.entity().getLocation().add(0, 3.0 / 40.0, 0));
                }
                for (BlockDisplayHandle h : fragmentPlates) {
                    h.entity().teleport(h.entity().getLocation().add(0, 3.0 / 40.0, 0));
                }
            }

            // Fragment plates open outward: lean from 0 to 45 degrees over 30 ticks after eruption
            if (ticksAlive > 40 && ticksAlive <= 70) {
                float openT = (ticksAlive - 40) / 30.0f;
                float openAngle = openT * 0.785f; // 45 degrees
                for (int i = 0; i < fragmentPlates.size(); i++) {
                    double angle = (2 * Math.PI * i) / 4;
                    float axisX = (float) Math.cos(angle);
                    float axisZ = (float) Math.sin(angle);
                    fragmentPlates.get(i).rotate(openAngle, axisX, 0, axisZ);
                    fragmentPlates.get(i).interpolation(3, 0);
                }
            }

            // Impact at eruption completion
            if (ticksAlive == 40) {
                triggerImpactDamage(geodeCenter);
                DisplayBuilder.playSound(geodeCenter, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
                DisplayBuilder.crimsonDust(geodeCenter, 30, 2.0);
            }

            // Crystal rotation: all axes at 0.5 deg/tick
            if (ticksAlive > 70) {
                float crystalRot = (float) Math.toRadians((ticksAlive - 70) * 0.5);
                for (BlockDisplayHandle h : crystalBlocks) {
                    h.rotate(crystalRot, 0.5f, 0.5f, 0.5f);
                    h.interpolation(3, 0);
                }
            }

            // Lava + flame particles around crystals
            if (ticksAlive % 8 == 0 && ticksAlive > 40) {
                w.spawnParticle(Particle.LAVA, geodeCenter.clone().add(0, 1, 0), 2, 0.5, 0.3, 0.5, 0);
                w.spawnParticle(Particle.FLAME, geodeCenter.clone().add(0, 1.5, 0), 3, 0.5, 0.2, 0.5, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BasaltGeode(plugin); }
    }

    // ================================================================
    // 22. SOUL PILLAR TRIO — 3 soul soil columns in triangle, 8 blocks
    //     from center, soul fire tops, lean toward Dweller
    // ================================================================
    public static class SoulPillarTrio extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> pillars = new ArrayList<>();

        public SoulPillarTrio(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_pillar_trio", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3 columns in triangle: NNE, SSW, ESE
            double[][] pillarPositions = {
                {3, -7},    // NNE
                {-5, 6},    // SSW
                {7, 3}      // ESE
            };

            for (int p = 0; p < 3; p++) {
                List<BlockDisplayHandle> pillar = new ArrayList<>();
                Location base = center.clone().add(pillarPositions[p][0], -1, pillarPositions[p][1]);

                for (int y = 0; y < 9; y++) {
                    Material mat;
                    if (y == 4) {
                        mat = Material.SOUL_SAND; // Face block at Y+4
                    } else {
                        mat = Material.SOUL_SOIL;
                    }
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            base.clone().add(0, y, 0), mat);
                    h.glow(0, 150, 255).interpolation(3, 0);
                    pillar.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Soul fire top: small spinning soul soil block
                BlockDisplayHandle fireTop = displayBuilder.spawnBlock(
                        base.clone().add(0, 9, 0), Material.SOUL_SOIL);
                fireTop.scale(0.3f, 0.6f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
                pillar.add(fireTop);
                spawnedEntities.add(fireTop.entity());

                pillars.add(pillar);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            double[][] pillarPositions = {
                {3, -7}, {-5, 6}, {7, 3}
            };

            // Rise from Y-1 over 50 ticks, staggered 10 ticks apart
            if (ticksAlive <= 70) {
                for (int p = 0; p < pillars.size(); p++) {
                    int startTick = p * 10;
                    if (ticksAlive < startTick) continue;
                    int elapsed = Math.min(ticksAlive - startTick, 50);
                    float rise = elapsed / 50.0f;
                    for (BlockDisplayHandle h : pillars.get(p)) {
                        h.entity().teleport(h.entity().getLocation().add(0, rise * 0.02, 0));
                    }
                }
            }

            // Soul fire top spinning: 5 deg/tick on Y-axis
            for (List<BlockDisplayHandle> pillar : pillars) {
                if (pillar.size() > 9) {
                    BlockDisplayHandle fireTop = pillar.get(9);
                    float spinRot = (float) Math.toRadians(ticksAlive * 5);
                    fireTop.rotate(spinRot, 0, 1, 0);
                    fireTop.interpolation(2, 0);
                }
            }

            // Soul fire flame eruption from tops (10/sec per pillar = every 2 ticks)
            if (ticksAlive % 2 == 0 && ticksAlive > 70) {
                for (int p = 0; p < pillars.size(); p++) {
                    Location topLoc = center.clone().add(
                            pillarPositions[p][0], 9.5, pillarPositions[p][1]);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, topLoc, 5, 0.1, 0.3, 0.1, 0.02);
                }
            }

            // Lean toward center: subtle tilt (up to 10 degrees)
            if (ticksAlive > 70) {
                for (int p = 0; p < pillars.size(); p++) {
                    double dx = -pillarPositions[p][0];
                    double dz = -pillarPositions[p][1];
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    float leanAngle = (float) Math.toRadians(Math.min(10,
                            (ticksAlive - 70) * 0.01));
                    float axisX = (float) (dz / dist); // Perpendicular to lean direction
                    float axisZ = (float) (-dx / dist);
                    for (BlockDisplayHandle h : pillars.get(p)) {
                        h.rotate(leanAngle, axisX, 0, axisZ);
                        h.interpolation(5, 0);
                    }
                }
            }

            // Ambient sound every 80 ticks
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulPillarTrio(plugin); }
    }

    // ================================================================
    // 23. NETHER CEILING FRAGMENT — Ragged 5x5 netherrack chunk
    //     suspended at Y+24, dropped from above with impact effects
    // ================================================================
    public static class NetherCeilingFragment extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> slabBlocks = new ArrayList<>();
        private boolean impacted = false;

        public NetherCeilingFragment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_ceiling_fragment", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(5000);
            config.setCooldownTicks(200);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location slabCenter = center.clone().add(0, 60, 6); // Start at Y+60

            // 5x5 chunk with varied materials and Y offsets
            Material[] slabMats = {
                Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK,
                Material.MAGMA_BLOCK, Material.NETHERRACK
            };

            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    // Skip corners for ragged look
                    if (Math.abs(x) == 2 && Math.abs(z) == 2 && Math.random() > 0.5) continue;

                    double yVariance = (Math.random() - 0.5);
                    Material mat = Material.NETHERRACK;
                    // Scatter some glowstone and magma
                    double roll = Math.random();
                    if (roll < 0.15) mat = Material.MAGMA_BLOCK;
                    else if (roll < 0.25) mat = Material.CRACKED_STONE_BRICKS;

                    Location loc = slabCenter.clone().add(x, yVariance, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.glow(255, 100, 0).interpolation(3, 0);
                    slabBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(0, 24, 6),
                    Sound.ENTITY_RAVAGER_ROAR, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location targetCenter = center.clone().add(0, 24, 6);

            // Gravity drop: Y+60 to Y+24 over 120 ticks (cubic ease-in, accelerating)
            if (ticksAlive <= 120) {
                float t = ticksAlive / 120.0f;
                float eased = t * t * t; // Gravity acceleration
                float currentY = 60.0f + (24.0f - 60.0f) * eased;
                for (BlockDisplayHandle h : slabBlocks) {
                    BlockDisplay bd = h.entity();
                    Location loc = bd.getLocation();
                    double targetBlockY = currentY + (loc.getY() - center.getY() - 60);
                    bd.teleport(loc.clone().add(0, (currentY - 60.0f) * (1.0f / 120.0f) * 3 * t * t, 0));
                }
            }

            // Impact at tick 120
            if (ticksAlive == 120 && !impacted) {
                impacted = true;
                triggerImpactDamage(targetCenter);

                // Impact sound burst
                for (int i = 0; i < 5; i++) {
                    DisplayBuilder.playSound(targetCenter, Sound.BLOCK_STONE_PLACE, 0.8f, 0.4f);
                }
                DisplayBuilder.playSound(targetCenter, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.5f);

                // Dust cloud
                w.spawnParticle(Particle.BLOCK, targetCenter, 200, 3, 1, 3, 0.5,
                        Material.NETHERRACK.createBlockData());
                DisplayBuilder.crimsonDust(targetCenter, 40, 3.0);
            }

            // Post-impact: slow oscillation on all axes (+-1 deg, 150 ticks)
            if (ticksAlive > 120) {
                float oscX = (float) Math.sin(ticksAlive * Math.PI / 75.0) * 0.017f;
                float oscZ = (float) Math.sin(ticksAlive * Math.PI / 75.0 + 1.0) * 0.017f;
                for (BlockDisplayHandle h : slabBlocks) {
                    h.rotate(oscX + oscZ, 1, 0, 1);
                    h.interpolation(5, 0);
                }
            }

            // Dripping lava from underside (8 positions)
            if (ticksAlive % 5 == 0 && ticksAlive > 120) {
                for (int i = 0; i < Math.min(slabBlocks.size(), 8); i += 1) {
                    if (i % 3 != 0) continue;
                    Location drip = slabBlocks.get(i).entity().getLocation().add(0, -0.5, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, drip, 2, 0.3, 0, 0.3, 0);
                }
            }

            // Lava particles drifting upward from slab top
            if (ticksAlive % 4 == 0 && ticksAlive > 120) {
                Location topLoc = targetCenter.clone().add(
                        (Math.random() - 0.5) * 4, 1, (Math.random() - 0.5) * 4);
                w.spawnParticle(Particle.LAVA, topLoc, 1, 0.5, 0.2, 0.5, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetherCeilingFragment(plugin); }
    }

    // ================================================================
    // 24. BRIMSTONE SPIRE RING — 10 spires at radius 20, arena outer wall,
    //     lava curtains between them, swaying teeth
    // ================================================================
    public static class BrimstoneSpireRing extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> spires = new ArrayList<>();
        private final int SPIRE_COUNT = 10;

        public BrimstoneSpireRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_spire_ring", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SPIRE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / SPIRE_COUNT;
                double x = Math.cos(angle) * 20.0;
                double z = Math.sin(angle) * 20.0;

                List<BlockDisplayHandle> spire = new ArrayList<>();

                // 4-block spire: alternating netherrack/blackstone
                for (int y = 0; y < 4; y++) {
                    Material mat = (y % 2 == 0) ? Material.NETHERRACK : Material.BLACKSTONE;
                    Location loc = center.clone().add(x, -2 + y, z); // Start underground

                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    // Top block has compressed Y for pointed tip
                    if (y == 3) {
                        h.scale(1.0f, 0.5f, 1.0f);
                    }
                    h.glow(200, 0, 50).interpolation(3, 0);
                    spire.add(h);
                    spawnedEntities.add(h.entity());
                }

                spires.add(spire);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.4f);
            DisplayBuilder.crimsonDust(center, 30, 20.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Eruption: rise from Y-2 to Y+4 over 30 ticks (simultaneous)
            if (ticksAlive <= 30) {
                float rise = ticksAlive / 30.0f;
                for (List<BlockDisplayHandle> spire : spires) {
                    for (BlockDisplayHandle h : spire) {
                        h.entity().teleport(h.entity().getLocation().add(0, 6.0 / 30.0 * 0.33, 0));
                    }
                }
            }

            // Spire sway: +-1.5 degrees, 200-tick period, offset per spire
            if (ticksAlive > 30) {
                for (int i = 0; i < spires.size(); i++) {
                    float swayOffset = i * 20;
                    float sway = (float) Math.sin((ticksAlive + swayOffset) * Math.PI / 100.0) * 0.026f;
                    for (BlockDisplayHandle h : spires.get(i)) {
                        h.rotate(sway, 1, 0, 0);
                        h.interpolation(5, 0);
                    }
                }
            }

            // Lava curtains between adjacent spires (every 5 ticks, 20 particles per curtain)
            if (ticksAlive % 5 == 0 && ticksAlive > 30) {
                for (int i = 0; i < SPIRE_COUNT; i++) {
                    int next = (i + 1) % SPIRE_COUNT;
                    double angle1 = (2 * Math.PI * i) / SPIRE_COUNT;
                    double angle2 = (2 * Math.PI * next) / SPIRE_COUNT;
                    double x1 = Math.cos(angle1) * 20.0;
                    double z1 = Math.sin(angle1) * 20.0;
                    double x2 = Math.cos(angle2) * 20.0;
                    double z2 = Math.sin(angle2) * 20.0;

                    // Pulse density: full -> half, 60-tick cycle
                    int density = ((ticksAlive / 30) % 2 == 0) ? 10 : 5;
                    for (int p = 0; p < density; p++) {
                        double t = p / (double) density;
                        double px = x1 + (x2 - x1) * t;
                        double pz = z1 + (z2 - z1) * t;
                        double py = 4.0 - Math.random() * 4.0; // Falling from Y+4 to Y+0
                        Location particleLoc = center.clone().add(px, py, pz);
                        w.spawnParticle(Particle.LAVA, particleLoc, 1, 0.1, 0.2, 0.1, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSpireRing(plugin); }
    }

    // ================================================================
    // 25. THE DWELLER'S MARK (GROUND SIGIL) — 20-block diameter ground
    //     sigil, eight-pointed star, rotating Y-axis, soul fire + lava
    // ================================================================
    public static class DwellersMark extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> starArms = new ArrayList<>();
        private final List<BlockDisplayHandle> centerBlocks = new ArrayList<>();

        public DwellersMark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dwellers_mark", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(100);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: netherrack panels flush with ground, radius 10
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double x = Math.cos(angle) * 10.0;
                double z = Math.sin(angle) * 10.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0.05, z), Material.NETHERRACK);
                h.scale(1.0f, 0.05f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                ringSegments.add(h);
                spawnedEntities.add(h.entity());
            }

            // Eight-pointed star arms: blackstone panels radiating from center
            for (int arm = 0; arm < 8; arm++) {
                double armAngle = (2 * Math.PI * arm) / 8;
                // Each arm is 3 segments from radius 3 to radius 9
                for (int seg = 0; seg < 3; seg++) {
                    double radius = 3.0 + seg * 2.5;
                    double x = Math.cos(armAngle) * radius;
                    double z = Math.sin(armAngle) * radius;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.05, z), Material.BLACKSTONE);
                    h.scale(1.0f, 0.05f, 0.5f).glow(200, 0, 50)
                     .rotate((float) armAngle, 0, 1, 0)
                     .interpolation(3, 0);
                    starArms.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Center 3x3 magma block square
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.05, z), Material.MAGMA_BLOCK);
                    h.scale(1.0f, 0.05f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                    centerBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sigil rotation: 0.3 deg/tick on Y-axis
            float rotAngle = (float) Math.toRadians(ticksAlive * 0.3);

            // Rotate ring segments around center
            for (int i = 0; i < ringSegments.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 12 + rotAngle;
                double x = Math.cos(baseAngle) * 10.0;
                double z = Math.sin(baseAngle) * 10.0;
                ringSegments.get(i).entity().teleport(center.clone().add(x, 0.05, z));
            }

            // Rotate star arms around center
            for (int arm = 0; arm < 8; arm++) {
                double armAngle = (2 * Math.PI * arm) / 8 + rotAngle;
                for (int seg = 0; seg < 3; seg++) {
                    int idx = arm * 3 + seg;
                    if (idx >= starArms.size()) break;
                    double radius = 3.0 + seg * 2.5;
                    double x = Math.cos(armAngle) * radius;
                    double z = Math.sin(armAngle) * radius;
                    starArms.get(idx).entity().teleport(center.clone().add(x, 0.05, z));
                    starArms.get(idx).rotate((float) armAngle, 0, 1, 0);
                    starArms.get(idx).interpolation(3, 0);
                }
            }

            // Soul fire from star-arm tips (2/sec per tip = 16 total/sec, every ~1.25 ticks)
            if (ticksAlive % 2 == 0) {
                for (int arm = 0; arm < 8; arm++) {
                    double tipAngle = (2 * Math.PI * arm) / 8 + rotAngle;
                    double tipX = Math.cos(tipAngle) * 8.5;
                    double tipZ = Math.sin(tipAngle) * 8.5;
                    Location tipLoc = center.clone().add(tipX, 0.3, tipZ);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, tipLoc, 2, 0.1, 0.2, 0.1, 0);
                }
            }

            // Lava from magma center (10/sec -> every 2 ticks)
            if (ticksAlive % 2 == 0) {
                Location centerLoc = center.clone().add(0, 0.3, 0);
                w.spawnParticle(Particle.LAVA, centerLoc, 5, 1.0, 0.1, 1.0, 0);
            }

            // Crimson dust ambient
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 10, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellersMark(plugin); }
    }
}
