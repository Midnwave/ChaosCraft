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
 * Phase 1 Block Display — GROUP 2: SKY / OVERHEAD STRUCTURES
 * 10 floating or descending structures (attacks #11-20).
 * Adapted from boss1-voidmaw.md with Calamity attack rules applied:
 * - NO status effects (Blindness, Slowness, Darkness, etc. removed -> damage only)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Calamity particle palette (purple, cyan, crimson)
 * - All values configurable via AttackConfig
 */
public final class SkyOverhead {

    private SkyOverhead() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidStalactite(plugin));
        registry.register(new HangingSpineArray(plugin));
        registry.register(new VoidCanopy(plugin));
        registry.register(new FractureKite(plugin));
        registry.register(new AbyssBell(plugin));
        registry.register(new InvertedSpire(plugin));
        registry.register(new DescendingCrown(plugin));
        registry.register(new VoidComet(plugin));
        registry.register(new HangingVoidLattice(plugin));
        registry.register(new OrbitalSiegeRing(plugin));
    }

    // ================================================================
    // 11. VOID STALACTITE — Descending obsidian spike that hovers then plunges
    // ================================================================
    public static class VoidStalactite extends BlockDisplayAttack {

        private BlockDisplayHandle mainBody;
        private final List<BlockDisplayHandle> fringeBlocks = new ArrayList<>();
        private BlockDisplayHandle ceilingCap;
        private BlockDisplayHandle glowPocket;
        private boolean impacted = false;
        private float currentY;
        private static final float START_Y = 12.0f;
        private static final float HOVER_Y = 3.0f;
        private static final float DESCENT_SPEED = 0.025f; // blocks per tick (0.5/sec)
        private int hoverTicks = 0;

        public VoidStalactite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_stalactite", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentY = START_Y;

            // Main body: tapered obsidian column pointing downward
            mainBody = displayBuilder.spawnBlock(center.clone().add(0, START_Y, 0), Material.OBSIDIAN);
            mainBody.scale(2.5f, 5.0f, 2.5f).glow(80, 0, 160).interpolation(3, 0);
            spawnedEntities.add(mainBody.entity());

            // Crying obsidian dripping fringe at lower half
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double x = Math.cos(angle) * 0.9;
                double z = Math.sin(angle) * 0.9;
                Location fLoc = center.clone().add(x, START_Y - 2.5 - (i % 3) * 0.4, z);
                BlockDisplayHandle f = displayBuilder.spawnBlock(fLoc, Material.CRYING_OBSIDIAN);
                float icicleScale = 0.3f - (i % 3) * 0.05f;
                f.scale(icicleScale, 0.8f + (i % 2) * 0.3f, icicleScale).glow(128, 0, 255).interpolation(3, 0);
                fringeBlocks.add(f);
                spawnedEntities.add(f.entity());
            }

            // Blackstone ceiling cap
            ceilingCap = displayBuilder.spawnBlock(center.clone().add(0, START_Y + 4.5, 0), Material.BLACKSTONE);
            ceilingCap.scale(3.0f, 0.8f, 3.0f).glow(40, 40, 50).interpolation(3, 0);
            spawnedEntities.add(ceilingCap.entity());

            // Amethyst glow pocket near point
            glowPocket = displayBuilder.spawnBlock(center.clone().add(0, START_Y - 1.5, 0), Material.AMETHYST_BLOCK);
            glowPocket.scale(0.8f, 0.8f, 0.8f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(glowPocket.entity());

            // Dark prismarine texture patches on upper third
            BlockDisplayHandle dp1 = displayBuilder.spawnBlock(center.clone().add(1, START_Y + 2, 0), Material.DARK_PRISMARINE);
            dp1.scale(0.5f, 0.6f, 0.5f).glow(0, 150, 180).interpolation(3, 0);
            spawnedEntities.add(dp1.entity());
            BlockDisplayHandle dp2 = displayBuilder.spawnBlock(center.clone().add(-1, START_Y + 3, 0), Material.DARK_PRISMARINE);
            dp2.scale(0.5f, 0.6f, 0.5f).glow(0, 150, 180).interpolation(3, 0);
            spawnedEntities.add(dp2.entity());

            DisplayBuilder.playSound(center.clone().add(0, START_Y, 0), Sound.BLOCK_NETHERITE_BLOCK_FALL, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            float prevY = currentY;

            // Phase 1: Slow descent at 0.5 blocks/sec until 3 blocks above ground
            if (currentY > HOVER_Y) {
                currentY -= DESCENT_SPEED;
                if (currentY <= HOVER_Y) {
                    currentY = HOVER_Y;
                    hoverTicks = 0;
                }
            }
            // Phase 2: Threatening hover for 20 ticks
            else if (hoverTicks < 20) {
                hoverTicks++;
                // Subtle menacing vibration during hover
                float shake = (float) Math.sin(hoverTicks * 2.5) * 0.05f;
                currentY = HOVER_Y + shake;
            }
            // Phase 3: Plunge at 8 blocks/sec = 0.4 blocks/tick
            else {
                currentY -= 0.4f;
                if (currentY <= 0) {
                    // IMPACT
                    impacted = true;
                    currentY = 0;
                    triggerImpactDamage(c);
                    return;
                }
            }

            // Move all components
            float delta = currentY - START_Y;
            mainBody.entity().teleport(c.clone().add(0, currentY, 0));
            ceilingCap.entity().teleport(c.clone().add(0, currentY + 4.5, 0));
            glowPocket.entity().teleport(c.clone().add(0, currentY - 1.5, 0));

            for (int i = 0; i < fringeBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double x = Math.cos(angle) * 0.9;
                double z = Math.sin(angle) * 0.9;
                fringeBlocks.get(i).entity().teleport(c.clone().add(x, currentY - 2.5 - (i % 3) * 0.4, z));
            }

            // Particles: obsidian tears trailing upward behind descent
            if (ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                        c.clone().add(0, currentY - 3, 0), 5, 0.4, 1.0, 0.4, 0);
            }
            // Ash trailing from tip
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.ASH,
                        c.clone().add(0, currentY - 3.5, 0), 4, 0.2, 0.3, 0.2, 0);
            }
            // Purple dust aura
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.purpleDust(c.clone().add(0, currentY, 0), 6, 1.5);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            World w = impactLocation.getWorld();
            if (w == null) return;

            // Massive obsidian particle burst
            w.spawnParticle(Particle.BLOCK, impactLocation, 120, 2, 1, 2, 0.5,
                    Material.OBSIDIAN.createBlockData());
            // Reverse portal ground ring
            DisplayBuilder.particleRing(impactLocation, 3.0, Particle.REVERSE_PORTAL, 30, null);
            // Crimson dust shockwave
            DisplayBuilder.crimsonDust(impactLocation, 20, 4.0);
            // Impact sound
            DisplayBuilder.playSound(impactLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.0f);

            // Remove all displays on impact
            displayBuilder.removeAll();
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidStalactite(plugin); }
    }

    // ================================================================
    // 12. HANGING SPINE ARRAY — 6 independent spines drifting at staggered heights
    // ================================================================
    public static class HangingSpineArray extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> spines = new ArrayList<>();
        private final List<BlockDisplayHandle> looseShard = new ArrayList<>();
        private final float[] spineHeights = {8, 10, 12, 14, 16, 11};
        private final float[] spineRotSpeeds = {0.00262f, 0.00436f, 0.00524f, 0.00349f, 0.00611f, 0.00175f};
        private final float[] spineDriftPhase = {0, 1.2f, 2.4f, 3.6f, 4.8f, 0.6f};

        public HangingSpineArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_spine_array", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 6 spines arranged in 2x3 grid, 4 blocks apart
            double[][] gridPos = {{-2,0,-2},{-2,0,2},{2,0,-2},{2,0,2},{0,0,-4},{0,0,4}};

            for (int s = 0; s < 6; s++) {
                List<BlockDisplayHandle> spine = new ArrayList<>();
                float baseY = spineHeights[s];
                boolean hasCryingTip = (s < 4); // 4 of 6 have crying obsidian tips
                boolean hasAmethyst = (s == 1 || s == 4); // 2 have amethyst glow nodes

                // Spine: 5-block column tapering from 2x2 to 1x1
                for (int y = 0; y < 5; y++) {
                    float taper = 1.0f - (y * 0.15f);
                    Location loc = center.clone().add(gridPos[s][0], baseY - y, gridPos[s][2]);
                    Material mat;
                    if (y < 2) {
                        // Upper half: obsidian jacket + optional netherite anchor
                        mat = (s % 2 == 0 && y == 0) ? Material.NETHERITE_BLOCK : Material.OBSIDIAN;
                    } else if (y == 4 && hasCryingTip) {
                        mat = Material.CRYING_OBSIDIAN;
                    } else {
                        mat = Material.BLACKSTONE;
                    }
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(taper * 0.8f, 1.0f, taper * 0.8f).glow(60, 0, 120).interpolation(3, 0);
                    spine.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Amethyst glow node at mid-height
                if (hasAmethyst) {
                    Location aLoc = center.clone().add(gridPos[s][0], baseY - 2.5, gridPos[s][2]);
                    BlockDisplayHandle a = displayBuilder.spawnBlock(aLoc, Material.AMETHYST_BLOCK);
                    a.scale(0.4f, 0.4f, 0.4f).glow(200, 100, 255).interpolation(3, 0);
                    spine.add(a);
                    spawnedEntities.add(a.entity());
                }

                spines.add(spine);
            }

            // 5 loose floating shards between spines
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                Location sLoc = center.clone().add(Math.cos(angle) * 3, 11 + (i % 3), Math.sin(angle) * 3);
                BlockDisplayHandle sh = displayBuilder.spawnBlock(sLoc, Material.COBBLED_DEEPSLATE);
                sh.scale(0.35f, 0.35f, 0.35f).glow(50, 50, 60).interpolation(2, 0);
                looseShard.add(sh);
                spawnedEntities.add(sh.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 12, 0), Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[][] gridPos = {{-2,0,-2},{-2,0,2},{2,0,-2},{2,0,2},{0,0,-4},{0,0,4}};

            // Spine drift and rotation
            for (int s = 0; s < 6; s++) {
                float rot = ticksAlive * spineRotSpeeds[s];
                // Lateral sine drift: 1.5 blocks, 60-tick cycle, phased differently
                float driftX = (float) Math.sin(ticksAlive * 0.105f + spineDriftPhase[s]) * 1.5f;
                float driftZ = (float) Math.cos(ticksAlive * 0.08f + spineDriftPhase[s] * 1.3f) * 1.5f;

                List<BlockDisplayHandle> spine = spines.get(s);
                for (int y = 0; y < Math.min(spine.size(), 5); y++) {
                    Location target = c.clone().add(
                            gridPos[s][0] + driftX,
                            spineHeights[s] - y,
                            gridPos[s][2] + driftZ
                    );
                    spine.get(y).entity().teleport(target);
                    spine.get(y).rotate(rot, 0, 1, 0);
                    spine.get(y).interpolation(3, 0);
                }
            }

            // Loose shards: spin on arbitrary axes at 15 deg/sec
            for (int i = 0; i < looseShard.size(); i++) {
                float shardRot = ticksAlive * 0.01309f; // ~15 deg/sec
                double angle = (Math.PI * 2 * i) / 5 + ticksAlive * 0.003f;
                Location sLoc = c.clone().add(Math.cos(angle) * 3, 11 + (i % 3), Math.sin(angle) * 3);
                looseShard.get(i).entity().teleport(sLoc);
                looseShard.get(i).rotate(shardRot, 0.5f, 0.7f, 0.3f);
                looseShard.get(i).interpolation(2, 0);
            }

            // Crying obsidian tears dripping down from tips
            if (ticksAlive % 4 == 0) {
                for (int s = 0; s < 4; s++) {
                    List<BlockDisplayHandle> spine = spines.get(s);
                    if (spine.size() >= 5) {
                        Location tipLoc = spine.get(4).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tipLoc.add(0, -0.5, 0),
                                3, 0.1, 0.5, 0.1, 0);
                    }
                }
            }

            // Violet dust orbiting each spine
            if (ticksAlive % 8 == 0) {
                for (int s = 0; s < 6; s++) {
                    if (!spines.get(s).isEmpty()) {
                        Location spineCenter = spines.get(s).get(2).entity().getLocation();
                        DisplayBuilder.purpleDust(spineCenter, 4, 0.8);
                    }
                }
            }

            // Chain creak sound every 4 seconds
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 12, 0), Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.5f);
            }

            // Enderman ambient occasionally
            if (ticksAlive % 200 == 100) {
                DisplayBuilder.playSound(c.clone().add(0, 12, 0), Sound.ENTITY_ENDERMAN_AMBIENT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HangingSpineArray(plugin); }
    }

    // ================================================================
    // 13. VOID CANOPY — Inverted dome descending with breathing rim
    // ================================================================
    public static class VoidCanopy extends BlockDisplayAttack {

        private BlockDisplayHandle domeConcrete;
        private BlockDisplayHandle obsidianRim;
        private BlockDisplayHandle cryingInnerRim;
        private final List<BlockDisplayHandle> rimTeeth = new ArrayList<>();
        private BlockDisplayHandle apexMass;
        private BlockDisplayHandle glazedPanels;
        private BlockDisplayHandle glassWindows;
        private float canopyY = 10.0f;
        private float apexExtend = 0;

        public VoidCanopy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_canopy", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main dome body: wide inverted dome of black concrete
            domeConcrete = displayBuilder.spawnBlock(center.clone().add(0, canopyY, 0), Material.BLACK_CONCRETE);
            domeConcrete.scale(9.0f, 3.0f, 9.0f).glow(20, 20, 30).interpolation(5, 0);
            spawnedEntities.add(domeConcrete.entity());

            // Obsidian rim teeth (12 blocks hanging from outer edge)
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location tLoc = center.clone().add(Math.cos(angle) * 4.2, canopyY - 1.5, Math.sin(angle) * 4.2);
                BlockDisplayHandle tooth = displayBuilder.spawnBlock(tLoc, Material.OBSIDIAN);
                tooth.scale(0.4f, 1.2f, 0.4f).glow(60, 0, 120).interpolation(3, 0);
                rimTeeth.add(tooth);
                spawnedEntities.add(tooth.entity());
            }

            // Crying obsidian inner rim ring
            cryingInnerRim = displayBuilder.spawnBlock(center.clone().add(0, canopyY - 0.5, 0), Material.CRYING_OBSIDIAN);
            cryingInnerRim.scale(6.0f, 0.4f, 6.0f).glow(128, 0, 255).interpolation(5, 0);
            spawnedEntities.add(cryingInnerRim.entity());

            // Glazed terracotta panels on top surface
            glazedPanels = displayBuilder.spawnBlock(center.clone().add(0, canopyY + 2.5, 0), Material.BLACK_GLAZED_TERRACOTTA);
            glazedPanels.scale(5.0f, 0.3f, 5.0f).glow(30, 30, 40).interpolation(5, 0);
            spawnedEntities.add(glazedPanels.entity());

            // Purple stained glass windows at cardinal points
            double[][] windowPos = {{4, 0}, {-4, 0}, {0, 4}, {0, -4}};
            for (double[] pos : windowPos) {
                BlockDisplayHandle glass = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], canopyY - 1, pos[1]), Material.PURPLE_STAINED_GLASS);
                glass.scale(0.5f, 0.8f, 0.5f).glow(160, 0, 200).interpolation(3, 0);
                spawnedEntities.add(glass.entity());
            }

            // Netherite apex mass (hanging center)
            apexMass = displayBuilder.spawnBlock(center.clone().add(0, canopyY - 2, 0), Material.NETHERITE_BLOCK);
            apexMass.scale(1.0f, 1.0f, 1.0f).glow(20, 20, 30).interpolation(5, 0);
            spawnedEntities.add(apexMass.entity());

            // Dark prismarine flanking the apex
            BlockDisplayHandle flank1 = displayBuilder.spawnBlock(center.clone().add(0.6, canopyY - 2, 0), Material.DARK_PRISMARINE);
            flank1.scale(0.4f, 0.6f, 0.4f).glow(0, 150, 180).interpolation(5, 0);
            spawnedEntities.add(flank1.entity());
            BlockDisplayHandle flank2 = displayBuilder.spawnBlock(center.clone().add(-0.6, canopyY - 2, 0), Material.DARK_PRISMARINE);
            flank2.scale(0.4f, 0.6f, 0.4f).glow(0, 150, 180).interpolation(5, 0);
            spawnedEntities.add(flank2.entity());

            DisplayBuilder.playSound(center.clone().add(0, canopyY, 0), Sound.AMBIENT_CAVE, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow descent: 0.3 blocks/sec = 0.015 blocks/tick + Y-rotation 8 deg/sec
            canopyY -= 0.015f;
            float rot = ticksAlive * 0.007f; // ~8 deg/sec

            // Move dome
            domeConcrete.entity().teleport(c.clone().add(0, canopyY, 0));
            domeConcrete.rotate(rot, 0, 1, 0);
            domeConcrete.interpolation(5, 0);

            cryingInnerRim.entity().teleport(c.clone().add(0, canopyY - 0.5, 0));
            glazedPanels.entity().teleport(c.clone().add(0, canopyY + 2.5, 0));

            // Rim teeth: oscillate inward +-0.3 blocks, staggered 15-tick cycle (breathing)
            for (int i = 0; i < rimTeeth.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12 + rot;
                float breathe = (float) Math.sin((ticksAlive + i * 1.25f) * 0.419f) * 0.3f;
                double radius = 4.2 - breathe;
                Location tLoc = c.clone().add(Math.cos(angle) * radius, canopyY - 1.5, Math.sin(angle) * radius);
                rimTeeth.get(i).entity().teleport(tLoc);
            }

            // Apex mass: descend extra 1.5 blocks at 0.1 blocks/sec
            apexExtend = Math.min(1.5f, apexExtend + 0.005f);
            apexMass.entity().teleport(c.clone().add(0, canopyY - 2 - apexExtend, 0));

            // Reverse portal rain from underside
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double rx = (Math.random() - 0.5) * 8;
                    double rz = (Math.random() - 0.5) * 8;
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                            c.clone().add(rx, canopyY - 1, rz), 2, 0.2, 0.5, 0.2, 0.01);
                }
            }

            // Crying obsidian curtain from inner rim
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, canopyY - 1, 0), 3.0,
                        Particle.FALLING_OBSIDIAN_TEAR, 8, null);
            }

            // Large smoke columns from apex
            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(0, canopyY - 2 - apexExtend, 0), 3, 0.2, 0.5, 0.2, 0.01);
            }

            // Cyan dust haze beneath canopy
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(c.clone().add(0, canopyY - 3, 0), 6, 4.0);
            }

            // Heartbeat sound every 3 seconds
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, canopyY, 0), Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCanopy(plugin); }
    }

    // ================================================================
    // 14. FRACTURE KITE — Diamond-shaped structure that hovers then dive-bombs
    // ================================================================
    public static class FractureKite extends BlockDisplayAttack {

        private BlockDisplayHandle kiteBody;
        private BlockDisplayHandle crackCenter;
        private BlockDisplayHandle glassOverlay;
        private BlockDisplayHandle penetratingTip;
        private final List<BlockDisplayHandle> borderPieces = new ArrayList<>();
        private boolean diving = false;
        private boolean impacted = false;
        private float kiteY = 15.0f;
        private float kiteX = 0;
        private float diveSpeed = 0.005f; // Accelerating
        private int hoverTicks = 0;

        public FractureKite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fracture_kite", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(20.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Kite body: cobbled deepslate diamond tilted 25 degrees
            kiteBody = displayBuilder.spawnBlock(center.clone().add(0, kiteY, 0), Material.COBBLED_DEEPSLATE);
            kiteBody.scale(3.5f, 7.0f, 1.2f).glow(60, 60, 80)
                    .rotate(0.436f, 1, 0, 0) // 25 degree tilt
                    .interpolation(3, 0);
            spawnedEntities.add(kiteBody.entity());

            // Amethyst fracture center
            crackCenter = displayBuilder.spawnBlock(center.clone().add(0, kiteY, 0.2), Material.AMETHYST_BLOCK);
            crackCenter.scale(1.5f, 2.0f, 0.5f).glow(220, 120, 255).interpolation(3, 0);
            spawnedEntities.add(crackCenter.entity());

            // Magenta stained glass behind the fracture
            glassOverlay = displayBuilder.spawnBlock(center.clone().add(0, kiteY, 0.5), Material.MAGENTA_STAINED_GLASS);
            glassOverlay.scale(1.2f, 1.5f, 0.3f).glow(255, 0, 255).interpolation(3, 0);
            spawnedEntities.add(glassOverlay.entity());

            // Obsidian crack pattern radiating from center
            double[][] crackOffsets = {{0.8, 1.5}, {-0.8, 1.5}, {0.8, -1.5}, {-0.8, -1.5},
                                       {1.2, 0}, {-1.2, 0}, {0, 2.5}, {0, -2.0}};
            for (double[] off : crackOffsets) {
                Location cLoc = center.clone().add(off[0], kiteY + off[1], 0);
                BlockDisplayHandle crack = displayBuilder.spawnBlock(cLoc, Material.OBSIDIAN);
                crack.scale(0.3f, 0.6f, 0.2f).glow(60, 0, 120).interpolation(3, 0);
                spawnedEntities.add(crack.entity());
            }

            // Black concrete border
            for (int i = 0; i < 6; i++) {
                float borderY = -3.0f + i * 1.2f;
                float borderX = (i < 3) ? (-1.2f + i * 0.4f) : (1.2f - (i - 3) * 0.4f);
                Location bLoc = center.clone().add(borderX, kiteY + borderY, -0.3);
                BlockDisplayHandle b = displayBuilder.spawnBlock(bLoc, Material.BLACK_CONCRETE);
                b.scale(0.6f, 0.8f, 0.3f).glow(30, 30, 40).interpolation(3, 0);
                borderPieces.add(b);
                spawnedEntities.add(b.entity());
            }

            // Dark prismarine penetrating tip at bottom
            penetratingTip = displayBuilder.spawnBlock(center.clone().add(0, kiteY - 3.5, 0), Material.DARK_PRISMARINE);
            penetratingTip.scale(0.5f, 1.0f, 0.5f).glow(0, 180, 200).interpolation(3, 0);
            spawnedEntities.add(penetratingTip.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            // Phase 1: Hover for 10 seconds (200 ticks)
            if (!diving) {
                hoverTicks++;
                if (hoverTicks >= 200) {
                    diving = true;
                    DisplayBuilder.playSound(c.clone().add(0, kiteY, 0),
                            Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 0.4f);
                }

                // Hover: enchant particles from amethyst center
                if (hoverTicks % 4 == 0) {
                    c.getWorld().spawnParticle(Particle.ENCHANT,
                            c.clone().add(0, kiteY, 0), 10, 0.8, 1.0, 0.8, 0.5);
                }
                // Magenta dust from crack lines
                if (hoverTicks % 6 == 0) {
                    DisplayBuilder.crimsonDust(c.clone().add(0, kiteY, 0), 5, 1.5);
                }
            }
            // Phase 2: Accelerating dive-bomb arc
            else {
                diveSpeed += 0.0025f; // Accelerate from 0.005 toward ~0.05 blocks/tick
                kiteY -= diveSpeed * 20; // Scale up to make it feel fast
                kiteX += 0.02f; // Slight lateral movement toward center

                if (kiteY <= 0) {
                    // IMPACT
                    impacted = true;
                    triggerImpactDamage(c);
                    return;
                }

                // Large smoke trail during dive
                if (ticksAlive % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                            c.clone().add(kiteX, kiteY + 3, 0), 6, 0.3, 0.5, 0.3, 0.05);
                }
            }

            // Update positions
            kiteBody.entity().teleport(c.clone().add(kiteX, kiteY, 0));
            crackCenter.entity().teleport(c.clone().add(kiteX, kiteY, 0.2));
            glassOverlay.entity().teleport(c.clone().add(kiteX, kiteY, 0.5));
            penetratingTip.entity().teleport(c.clone().add(kiteX, kiteY - 3.5, 0));
        }

        @Override
        protected void onImpact(Location impactLocation) {
            World w = impactLocation.getWorld();
            if (w == null) return;

            // Massive deepslate burst
            w.spawnParticle(Particle.BLOCK, impactLocation, 200, 3, 2, 3, 1.0,
                    Material.COBBLED_DEEPSLATE.createBlockData());
            // Purple explosion ring
            DisplayBuilder.purpleDust(impactLocation, 30, 5.0);
            DisplayBuilder.cyanDust(impactLocation, 15, 3.0);
            // Sonic boom
            DisplayBuilder.playSound(impactLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.9f);

            displayBuilder.removeAll();
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FractureKite(plugin); }
    }

    // ================================================================
    // 15. ABYSS BELL — Massive hanging bell that swings with shockwave rings
    // ================================================================
    public static class AbyssBell extends BlockDisplayAttack {

        private BlockDisplayHandle bellBody;
        private final List<BlockDisplayHandle> fringeBlocks = new ArrayList<>();
        private BlockDisplayHandle crownCluster;
        private BlockDisplayHandle crossBrace;
        private final List<BlockDisplayHandle> innerCrying = new ArrayList<>();
        private int swingTimer = 0;
        private float currentSwingAngle = 0;

        public AbyssBell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyss_bell", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(60); // Damage on swing (every ~3 seconds)
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            float bellY = 14.0f;

            // Bell body: netherite bell shape (wide rim narrowing to crown)
            bellBody = displayBuilder.spawnBlock(center.clone().add(0, bellY, 0), Material.NETHERITE_BLOCK);
            bellBody.scale(6.0f, 6.0f, 6.0f).glow(20, 20, 30).interpolation(3, 0);
            spawnedEntities.add(bellBody.entity());

            // Obsidian skirt fringe hanging below rim
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                Location fLoc = center.clone().add(Math.cos(angle) * 2.8, bellY - 3.5, Math.sin(angle) * 2.8);
                BlockDisplayHandle f = displayBuilder.spawnBlock(fLoc, Material.OBSIDIAN);
                f.scale(0.4f, 1.5f, 0.4f).glow(60, 0, 120).interpolation(2, 0);
                fringeBlocks.add(f);
                spawnedEntities.add(f.entity());
            }

            // Black glazed terracotta inner lining
            BlockDisplayHandle lining = displayBuilder.spawnBlock(center.clone().add(0, bellY - 1, 0),
                    Material.BLACK_GLAZED_TERRACOTTA);
            lining.scale(4.5f, 4.0f, 4.5f).glow(30, 30, 40).interpolation(3, 0);
            spawnedEntities.add(lining.entity());

            // 5 crying obsidian reversed-weeping inside the rim
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                Location cLoc = center.clone().add(Math.cos(angle) * 2.2, bellY - 2.5, Math.sin(angle) * 2.2);
                BlockDisplayHandle cry = displayBuilder.spawnBlock(cLoc, Material.CRYING_OBSIDIAN);
                cry.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                innerCrying.add(cry);
                spawnedEntities.add(cry.entity());
            }

            // Cross brace inside bell mouth
            crossBrace = displayBuilder.spawnBlock(center.clone().add(0, bellY - 3, 0), Material.POLISHED_BLACKSTONE);
            crossBrace.scale(5.0f, 0.3f, 0.3f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(crossBrace.entity());

            // Amethyst crown cluster
            crownCluster = displayBuilder.spawnBlock(center.clone().add(0, bellY + 5.5, 0), Material.AMETHYST_BLOCK);
            crownCluster.scale(1.2f, 1.0f, 1.2f).glow(220, 120, 255).interpolation(3, 0);
            spawnedEntities.add(crownCluster.entity());

            DisplayBuilder.playSound(center.clone().add(0, bellY, 0),
                    Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            float bellY = 14.0f;

            swingTimer++;

            // Bell ring cycle: every 300 ticks (15 seconds)
            // Swing: 10 ticks one side, 10 ticks other, 20 ticks damp back
            int ringCyclePos = swingTimer % 300;
            float swingAngle = 0;
            boolean isRinging = false;

            if (ringCyclePos < 10) {
                // Swing right: 0 -> 20 degrees
                swingAngle = (ringCyclePos / 10.0f) * 0.349f; // 20 degrees in radians
                isRinging = true;
            } else if (ringCyclePos < 20) {
                // Swing left: 20 -> -20 degrees
                swingAngle = 0.349f - ((ringCyclePos - 10) / 10.0f) * 0.698f;
                isRinging = true;
            } else if (ringCyclePos < 40) {
                // Damp back to center
                float damping = 1.0f - ((ringCyclePos - 20) / 20.0f);
                swingAngle = -0.349f * damping;
                isRinging = true;
            }

            currentSwingAngle = swingAngle;

            // Apply swing rotation to bell body
            bellBody.rotate(swingAngle, 0, 0, 1);
            bellBody.interpolation(2, 0);

            // Fringe lags behind with momentum: extra 10 degrees, 4-tick delay
            float fringeLag = (ringCyclePos < 40) ? swingAngle * 1.5f : swingAngle;
            for (int i = 0; i < fringeBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 10;
                float fX = (float) Math.cos(angle) * 2.8f + (float) Math.sin(fringeLag) * 1.5f;
                Location fLoc = c.clone().add(fX, bellY - 3.5, Math.sin(angle) * 2.8);
                fringeBlocks.get(i).entity().teleport(fLoc);
            }

            // On swing apex: soul fire burst from crown, ground-level violet rings
            if (ringCyclePos == 10 || ringCyclePos == 20) {
                Location crownLoc = c.clone().add(0, bellY + 5.5, 0);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, crownLoc, 12, 0.3, 0.3, 0.3, 0.05);

                // Ground-level purple ring expanding outward
                DisplayBuilder.particleRing(c, 6.0, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(128, 0, 255), 2.0f));

                // Bell ring sound
                DisplayBuilder.playSound(c.clone().add(0, bellY, 0),
                        Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.3f);
            }

            // Continuous: reverse portal streaming upward inside bell
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        c.clone().add(0, bellY - 2, 0), 6, 1.5, 1.0, 1.5, 0.02);
            }

            // Crimson dust outward during ring
            if (isRinging && ticksAlive % 4 == 0) {
                DisplayBuilder.crimsonDust(c.clone().add(0, 1, 0), 8, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssBell(plugin); }
    }

    // ================================================================
    // 16. INVERTED SPIRE — Spinning inverted pyramid descending to ground
    // ================================================================
    public static class InvertedSpire extends BlockDisplayAttack {

        private BlockDisplayHandle mainSpire;
        private BlockDisplayHandle blackstonePanels;
        private BlockDisplayHandle topBorder;
        private BlockDisplayHandle cryingTip;
        private BlockDisplayHandle amethystEyes;
        private float spireY = 18.0f;
        private float rotSpeed = 0.01047f; // 12 deg/sec
        private boolean embedded = false;

        public InvertedSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inverted_spire", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(2400); // ~120 seconds
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main spire: obsidian inverted pyramid (4x4 top tapering to point)
            mainSpire = displayBuilder.spawnBlock(center.clone().add(0, spireY, 0), Material.OBSIDIAN);
            mainSpire.scale(4.0f, 7.0f, 4.0f).glow(60, 0, 120).interpolation(5, 0);
            spawnedEntities.add(mainSpire.entity());

            // Blackstone exterior panels on 4 faces
            blackstonePanels = displayBuilder.spawnBlock(center.clone().add(0, spireY + 1, 0), Material.BLACKSTONE);
            blackstonePanels.scale(4.2f, 5.0f, 4.2f).glow(40, 40, 50).interpolation(5, 0);
            spawnedEntities.add(blackstonePanels.entity());

            // Polished blackstone top border ring
            topBorder = displayBuilder.spawnBlock(center.clone().add(0, spireY + 6.5, 0), Material.POLISHED_BLACKSTONE);
            topBorder.scale(4.5f, 0.5f, 4.5f).glow(50, 50, 60).interpolation(5, 0);
            spawnedEntities.add(topBorder.entity());

            // Netherite corner anchors at top
            double[][] corners = {{1.8, 1.8}, {1.8, -1.8}, {-1.8, 1.8}, {-1.8, -1.8}};
            for (double[] corner : corners) {
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(
                        center.clone().add(corner[0], spireY + 6, corner[1]), Material.NETHERITE_BLOCK);
                anchor.scale(0.6f, 0.6f, 0.6f).glow(20, 20, 30).interpolation(5, 0);
                spawnedEntities.add(anchor.entity());
            }

            // Crying obsidian tip (bottom 3 blocks pointing down)
            cryingTip = displayBuilder.spawnBlock(center.clone().add(0, spireY - 3, 0), Material.CRYING_OBSIDIAN);
            cryingTip.scale(0.8f, 2.5f, 0.8f).glow(128, 0, 255).interpolation(5, 0);
            spawnedEntities.add(cryingTip.entity());

            // Amethyst eyes at second taper level
            amethystEyes = displayBuilder.spawnBlock(center.clone().add(0, spireY + 2, 0), Material.AMETHYST_BLOCK);
            amethystEyes.scale(0.5f, 0.5f, 3.5f).glow(200, 100, 255).interpolation(5, 0);
            spawnedEntities.add(amethystEyes.entity());

            DisplayBuilder.playSound(center.clone().add(0, spireY, 0), Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || embedded) return;

            // First 8 seconds (160 ticks): hang still
            if (ticksAlive <= 160) {
                // Subtle ominous idle
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.darkPurpleDust(c.clone().add(0, spireY - 3, 0), 3, 0.5);
                }
                return;
            }

            // Descend at 0.08 blocks/sec = 0.004 blocks/tick
            spireY -= 0.004f;

            // Rotation accelerates as it descends: 12 deg/sec -> 25 deg/sec
            float descentProgress = Math.min(1.0f, (18.0f - spireY) / 18.0f);
            rotSpeed = 0.01047f + descentProgress * 0.01135f; // 12 -> 25 deg/sec
            float rot = ticksAlive * rotSpeed;

            // Ground contact check
            if (spireY <= 0) {
                embedded = true;
                triggerImpactDamage(c);
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.7f);
                DisplayBuilder.crimsonDust(c, 25, 3.0);
                return;
            }

            // Move all components
            mainSpire.entity().teleport(c.clone().add(0, spireY, 0));
            mainSpire.rotate(rot, 0, 1, 0);
            mainSpire.interpolation(3, 0);

            blackstonePanels.entity().teleport(c.clone().add(0, spireY + 1, 0));
            blackstonePanels.rotate(rot, 0, 1, 0);
            blackstonePanels.interpolation(3, 0);

            topBorder.entity().teleport(c.clone().add(0, spireY + 6.5, 0));
            cryingTip.entity().teleport(c.clone().add(0, spireY - 3, 0));
            amethystEyes.entity().teleport(c.clone().add(0, spireY + 2, 0));
            amethystEyes.rotate(rot, 0, 1, 0);
            amethystEyes.interpolation(3, 0);

            // Particles: reversed tears streaming up from tip
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                        c.clone().add(0, spireY - 4, 0), 3, 0.2, 0.8, 0.2, 0);
            }

            // Ash spiraling with rotation
            if (ticksAlive % 4 == 0) {
                double ashAngle = rot * 3;
                c.getWorld().spawnParticle(Particle.ASH,
                        c.clone().add(Math.cos(ashAngle) * 2.5, spireY, Math.sin(ashAngle) * 2.5),
                        4, 0.3, 2, 0.3, 0);
            }

            // Sculk charge at tip every 2 seconds
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.cyanDust(c.clone().add(0, spireY - 3.5, 0), 4, 0.4);
            }

            // Groaning rotation sound every 2 seconds
            if (ticksAlive % 40 == 20) {
                DisplayBuilder.playSound(c.clone().add(0, spireY, 0), Sound.BLOCK_NETHERITE_BLOCK_STEP, 0.5f, 0.5f);
            }

            // Enderman stare every 10 seconds
            if (ticksAlive % 200 == 100) {
                DisplayBuilder.playSound(c.clone().add(0, spireY, 0), Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InvertedSpire(plugin); }
    }

    // ================================================================
    // 17. THE DESCENDING CROWN — Inverted crown with oscillating fangs
    // ================================================================
    public static class DescendingCrown extends BlockDisplayAttack {

        private BlockDisplayHandle crownRing;
        private final List<BlockDisplayHandle> fangBattlements = new ArrayList<>();
        private BlockDisplayHandle glassLid;
        private float crownY = 16.0f;
        private static final float HOVER_Y = 3.0f;

        public DescendingCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("descending_crown", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(1600); // Persistent hover
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Crown ring: polished blackstone base ring (inverted)
            crownRing = displayBuilder.spawnBlock(center.clone().add(0, crownY, 0), Material.POLISHED_BLACKSTONE);
            crownRing.scale(5.0f, 2.0f, 5.0f).glow(50, 50, 60).interpolation(5, 0);
            spawnedEntities.add(crownRing.entity());

            // Obsidian reinforcement at battlement connections
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location rLoc = center.clone().add(Math.cos(angle) * 2.3, crownY - 0.5, Math.sin(angle) * 2.3);
                BlockDisplayHandle reinforcement = displayBuilder.spawnBlock(rLoc, Material.OBSIDIAN);
                reinforcement.scale(0.4f, 0.4f, 0.4f).glow(60, 0, 120).interpolation(5, 0);
                spawnedEntities.add(reinforcement.entity());
            }

            // 8 downward-pointing fang battlements
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location fLoc = center.clone().add(Math.cos(angle) * 2.3, crownY - 3, Math.sin(angle) * 2.3);
                Material mat;
                if (i % 2 == 0) {
                    mat = Material.CRYING_OBSIDIAN; // 4 weeping fangs
                } else {
                    mat = Material.NETHERITE_BLOCK; // 4 netherite capped
                }
                BlockDisplayHandle fang = displayBuilder.spawnBlock(fLoc, mat);
                fang.scale(0.5f, 3.0f, 0.5f).glow(i % 2 == 0 ? 128 : 20, 0, i % 2 == 0 ? 255 : 30)
                        .interpolation(3, 0);
                fangBattlements.add(fang);
                spawnedEntities.add(fang.entity());
            }

            // Purple stained glass lid inside crown center
            glassLid = displayBuilder.spawnBlock(center.clone().add(0, crownY + 1.5, 0), Material.PURPLE_STAINED_GLASS);
            glassLid.scale(3.0f, 0.3f, 3.0f).glow(160, 0, 200).interpolation(5, 0);
            spawnedEntities.add(glassLid.entity());

            DisplayBuilder.playSound(center.clone().add(0, crownY, 0),
                    Sound.ENTITY_WARDEN_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descend at 0.25 blocks/sec = 0.0125 blocks/tick until 3 blocks above ground
            if (crownY > HOVER_Y) {
                crownY -= 0.0125f;
                if (crownY < HOVER_Y) crownY = HOVER_Y;
            }

            // Y-rotation: 10 deg/sec
            float rot = ticksAlive * 0.00873f;

            // Move crown ring
            crownRing.entity().teleport(c.clone().add(0, crownY, 0));
            crownRing.rotate(rot, 0, 1, 0);
            crownRing.interpolation(3, 0);

            glassLid.entity().teleport(c.clone().add(0, crownY + 1.5, 0));

            // Fang battlements: oscillate inward +-0.4 blocks with staggered 12-tick sine
            for (int i = 0; i < 8; i++) {
                double baseAngle = (Math.PI * 2 * i) / 8 + rot;
                float inward = (float) Math.sin((ticksAlive + i * 1.5f) * 0.524f) * 0.4f; // 12-tick sine
                double radius = 2.3 - inward;
                Location fLoc = c.clone().add(
                        Math.cos(baseAngle) * radius,
                        crownY - 3,
                        Math.sin(baseAngle) * radius
                );
                fangBattlements.get(i).entity().teleport(fLoc);
            }

            // Tears from weeping fang tips (every other fang)
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 8; i += 2) {
                    Location tearLoc = fangBattlements.get(i).entity().getLocation().add(0, -1.5, 0);
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tearLoc, 3, 0.05, 0.3, 0.05, 0);
                }
            }

            // Deep violet haze drifting outward from crown ring
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.darkPurpleDust(c.clone().add(0, crownY, 0), 6, 3.0);
            }

            // Reverse portal inside crown ring, pooling downward
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        c.clone().add(0, crownY - 1, 0), 5, 1.5, 0.5, 1.5, 0.01);
            }

            // Amethyst cluster break sound when fangs reach inward apex
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, crownY, 0),
                        Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DescendingCrown(plugin); }
    }

    // ================================================================
    // 18. VOID COMET — Fast ballistic projectile with chaotic tumble
    // ================================================================
    public static class VoidComet extends BlockDisplayAttack {

        private BlockDisplayHandle cometCore;
        private BlockDisplayHandle amethystProw;
        private BlockDisplayHandle energyLens;
        private final List<BlockDisplayHandle> debrisTail = new ArrayList<>();
        private final List<BlockDisplayHandle> wingChips = new ArrayList<>();
        private BlockDisplayHandle trailingCluster;
        private Location targetPos;
        private double posX, posY, posZ;
        private double velX, velY, velZ;
        private boolean impacted = false;
        private final int[] tailDelays = {3, 5, 6, 4, 7, 8, 5, 3};

        public VoidComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_comet", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(100); // Fast projectile ~5 seconds
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(4.0);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Starting position: high oblique angle (20 blocks up, 30 blocks out)
            double angle = Math.random() * Math.PI * 2;
            posX = center.getX() + Math.cos(angle) * 30;
            posY = center.getY() + 20;
            posZ = center.getZ() + Math.sin(angle) * 30;

            // Target: center location (player's last known position)
            targetPos = center.clone();

            // Compute velocity vector toward target at 1.2 blocks/tick
            double dx = targetPos.getX() - posX;
            double dy = targetPos.getY() - posY;
            double dz = targetPos.getZ() - posZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            velX = (dx / dist) * 1.2;
            velY = (dy / dist) * 1.2;
            velZ = (dz / dist) * 1.2;

            Location spawnLoc = new Location(w, posX, posY, posZ);

            // Comet core: 3x3x2 netherite ovoid
            cometCore = displayBuilder.spawnBlock(spawnLoc, Material.NETHERITE_BLOCK);
            cometCore.scale(2.5f, 2.0f, 2.5f).glow(20, 20, 30).interpolation(1, 0);
            spawnedEntities.add(cometCore.entity());

            // Amethyst prow on forward face
            amethystProw = displayBuilder.spawnBlock(spawnLoc.clone().add(velX, velY, velZ), Material.AMETHYST_BLOCK);
            amethystProw.scale(1.8f, 1.2f, 1.8f).glow(220, 120, 255).interpolation(1, 0);
            spawnedEntities.add(amethystProw.entity());

            // Magenta energy lens
            energyLens = displayBuilder.spawnBlock(spawnLoc.clone().add(velX * 1.5, velY * 1.5, velZ * 1.5),
                    Material.MAGENTA_STAINED_GLASS);
            energyLens.scale(0.8f, 0.6f, 0.8f).glow(255, 0, 255).interpolation(1, 0);
            spawnedEntities.add(energyLens.entity());

            // 8 debris tail fragments
            for (int i = 0; i < 8; i++) {
                double offset = -(0.5 + i * 0.25);
                Location tLoc = spawnLoc.clone().add(velX * offset, velY * offset, velZ * offset);
                BlockDisplayHandle debris = displayBuilder.spawnBlock(tLoc, Material.OBSIDIAN);
                debris.scale(0.4f + (float)(Math.random() * 0.3f), 0.4f, 0.4f).glow(60, 0, 120).interpolation(1, 0);
                debrisTail.add(debris);
                spawnedEntities.add(debris.entity());
            }

            // Wing chips: 4 diagonal off core
            for (int i = 0; i < 4; i++) {
                double wAngle = (Math.PI / 2) * i;
                Location wLoc = spawnLoc.clone().add(Math.cos(wAngle) * 1.5, 0, Math.sin(wAngle) * 1.5);
                BlockDisplayHandle wing = displayBuilder.spawnBlock(wLoc, Material.BLACK_CONCRETE);
                wing.scale(0.5f, 0.3f, 0.5f).glow(30, 30, 40).interpolation(1, 0);
                wingChips.add(wing);
                spawnedEntities.add(wing.entity());
            }

            // Crying obsidian trailing cluster
            trailingCluster = displayBuilder.spawnBlock(spawnLoc.clone().add(-velX * 2, -velY * 2, -velZ * 2),
                    Material.CRYING_OBSIDIAN);
            trailingCluster.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(1, 0);
            spawnedEntities.add(trailingCluster.entity());

            DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            // Move comet along velocity
            posX += velX;
            posY += velY;
            posZ += velZ;

            World w = c.getWorld();
            Location currentLoc = new Location(w, posX, posY, posZ);

            // Ground contact or proximity to target
            if (posY <= c.getY() || currentLoc.distanceSquared(targetPos) < 4.0) {
                impacted = true;
                triggerImpactDamage(currentLoc);
                return;
            }

            // Chaotic tumble: X 5°/tick, Y 3°/tick, Z 7°/tick
            float tumbleX = ticksAlive * 0.0873f;  // 5 deg/tick
            float tumbleY = ticksAlive * 0.05236f; // 3 deg/tick
            float tumbleZ = ticksAlive * 0.1222f;  // 7 deg/tick

            // Move core
            cometCore.entity().teleport(currentLoc);
            cometCore.rotate(tumbleX, 1, 0, 0);
            cometCore.interpolation(1, 0);

            // Move prow
            amethystProw.entity().teleport(currentLoc.clone().add(velX * 0.8, velY * 0.8, velZ * 0.8));
            energyLens.entity().teleport(currentLoc.clone().add(velX * 1.2, velY * 1.2, velZ * 1.2));

            // Debris tail follows with lag
            for (int i = 0; i < debrisTail.size(); i++) {
                double lagFactor = -(1.0 + i * 0.5);
                Location tLoc = currentLoc.clone().add(
                        -velX * lagFactor + (Math.random() - 0.5) * 0.3,
                        -velY * lagFactor + (Math.random() - 0.5) * 0.3,
                        -velZ * lagFactor + (Math.random() - 0.5) * 0.3
                );
                debrisTail.get(i).entity().teleport(tLoc);
                debrisTail.get(i).rotate(tumbleZ + i * 0.5f, 0.3f, 0.7f, 0.5f);
                debrisTail.get(i).interpolation(1, 0);
            }

            // Wing chips orbit with tumble
            for (int i = 0; i < wingChips.size(); i++) {
                double wAngle = (Math.PI / 2) * i + ticksAlive * 0.15;
                wingChips.get(i).entity().teleport(currentLoc.clone().add(
                        Math.cos(wAngle) * 1.5, Math.sin(wAngle) * 0.8, Math.sin(wAngle) * 1.5));
            }

            trailingCluster.entity().teleport(currentLoc.clone().add(-velX * 3, -velY * 3, -velZ * 3));

            // Dense reverse portal comet tail
            if (ticksAlive % 1 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL,
                        currentLoc.clone().add(-velX * 2, -velY * 2, -velZ * 2),
                        15, 0.5, 0.5, 0.5, 0.02);
            }

            // Large smoke in the far wake
            if (ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.LARGE_SMOKE,
                        currentLoc.clone().add(-velX * 4, -velY * 4, -velZ * 4),
                        5, 0.5, 0.5, 0.5, 0.02);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            World w = impactLocation.getWorld();
            if (w == null) return;

            // Explosion particles
            w.spawnParticle(Particle.EXPLOSION, impactLocation, 5, 1.5, 1, 1.5, 0);
            w.spawnParticle(Particle.BLOCK, impactLocation, 150, 3, 2, 3, 1.0,
                    Material.NETHERITE_BLOCK.createBlockData());
            // Crimson shockwave
            DisplayBuilder.crimsonDust(impactLocation, 30, 5.0);
            DisplayBuilder.purpleDust(impactLocation, 20, 3.0);
            // Iron golem hurt (ground shake)
            DisplayBuilder.playSound(impactLocation, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);

            displayBuilder.removeAll();
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidComet(plugin); }
    }

    // ================================================================
    // 19. HANGING VOID LATTICE — Complex 3D lattice with counter-rotating rings
    // ================================================================
    public static class HangingVoidLattice extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> ringFrames = new ArrayList<>();
        private final List<BlockDisplayHandle> verticalColumns = new ArrayList<>();
        private final List<BlockDisplayHandle> orbitingDrops = new ArrayList<>();
        private BlockDisplayHandle centralAmethyst;
        private BlockDisplayHandle darkPrismarineBase;

        public HangingVoidLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_void_lattice", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(1800); // 90 seconds
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            float latticeY = 10.0f;

            // 8 horizontal ring frames, stacked 1 block apart
            for (int ring = 0; ring < 8; ring++) {
                List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
                Material ringMat = (ring % 2 == 0) ? Material.BLACKSTONE : Material.OBSIDIAN;

                // Each ring: 5x5 hollow frame (4 sides, no corners filled)
                // Simplified: 4 edge beams per ring
                for (int side = 0; side < 4; side++) {
                    double angle = (Math.PI / 2) * side;
                    Location rLoc = center.clone().add(
                            Math.cos(angle) * 2.3,
                            latticeY + ring,
                            Math.sin(angle) * 2.3
                    );
                    BlockDisplayHandle b = displayBuilder.spawnBlock(rLoc, ringMat);
                    float width = (side % 2 == 0) ? 4.5f : 0.4f;
                    float depth = (side % 2 == 0) ? 0.4f : 4.5f;
                    b.scale(width, 0.3f, depth).glow(50, 50, 60).interpolation(3, 0);
                    ringBlocks.add(b);
                    spawnedEntities.add(b.entity());
                }

                ringFrames.add(ringBlocks);
            }

            // 8 vertical columns connecting rings
            double[] colAngles = {0, Math.PI / 4, Math.PI / 2, 3 * Math.PI / 4,
                    Math.PI, 5 * Math.PI / 4, 3 * Math.PI / 2, 7 * Math.PI / 4};
            for (int col = 0; col < 8; col++) {
                Location cLoc = center.clone().add(
                        Math.cos(colAngles[col]) * 2.3,
                        latticeY,
                        Math.sin(colAngles[col]) * 2.3
                );
                Material colMat = (col < 4) ? Material.BLACKSTONE : Material.OBSIDIAN;
                BlockDisplayHandle column = displayBuilder.spawnBlock(cLoc, colMat);
                column.scale(0.3f, 8.0f, 0.3f).glow(50, 50, 60).interpolation(3, 0);
                verticalColumns.add(column);
                spawnedEntities.add(column.entity());
            }

            // Dark prismarine at 8 column bases
            for (int col = 0; col < 8; col++) {
                Location bLoc = center.clone().add(
                        Math.cos(colAngles[col]) * 2.3,
                        latticeY - 0.3,
                        Math.sin(colAngles[col]) * 2.3
                );
                if (col < 4) continue; // Only bottom 4 get prismarine
                BlockDisplayHandle dp = displayBuilder.spawnBlock(bLoc, Material.DARK_PRISMARINE);
                dp.scale(0.5f, 0.5f, 0.5f).glow(0, 150, 180).interpolation(3, 0);
                spawnedEntities.add(dp.entity());
            }

            // 6 crying obsidian drops orbiting inside
            for (int i = 0; i < 6; i++) {
                double orbAngle = (Math.PI * 2 * i) / 6;
                float orbRadius = 0.8f + (i % 3) * 0.3f;
                Location dLoc = center.clone().add(
                        Math.cos(orbAngle) * orbRadius,
                        latticeY + 3 + (i % 3),
                        Math.sin(orbAngle) * orbRadius
                );
                BlockDisplayHandle drop = displayBuilder.spawnBlock(dLoc, Material.CRYING_OBSIDIAN);
                drop.scale(0.3f, 0.4f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                orbitingDrops.add(drop);
                spawnedEntities.add(drop.entity());
            }

            // Purple stained glass at top ring facing down
            for (int i = 0; i < 4; i++) {
                double gAngle = (Math.PI / 2) * i;
                Location gLoc = center.clone().add(Math.cos(gAngle) * 1.5, latticeY + 7.5, Math.sin(gAngle) * 1.5);
                BlockDisplayHandle glass = displayBuilder.spawnBlock(gLoc, Material.PURPLE_STAINED_GLASS);
                glass.scale(0.5f, 0.3f, 0.5f).glow(160, 0, 200).interpolation(3, 0);
                spawnedEntities.add(glass.entity());
            }

            // Central amethyst
            centralAmethyst = displayBuilder.spawnBlock(center.clone().add(0, latticeY + 4, 0), Material.AMETHYST_BLOCK);
            centralAmethyst.scale(0.6f, 0.6f, 0.6f).glow(220, 120, 255).interpolation(3, 0);
            spawnedEntities.add(centralAmethyst.entity());

            DisplayBuilder.playSound(center.clone().add(0, latticeY + 4, 0), Sound.BLOCK_CHAIN_FALL, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            float latticeY = 10.0f;

            // Whole lattice rotates at 5 deg/sec
            float mainRot = ticksAlive * 0.00436f;
            // Every other ring counter-rotates at 3 deg/sec
            float counterRot = -ticksAlive * 0.00262f;

            // Rotate ring frames
            for (int ring = 0; ring < ringFrames.size(); ring++) {
                float ringRot = (ring % 2 == 0) ? mainRot : counterRot;
                for (BlockDisplayHandle b : ringFrames.get(ring)) {
                    b.rotate(ringRot, 0, 1, 0);
                    b.interpolation(3, 0);
                }
            }

            // Rotate vertical columns with main rotation
            double[] colAngles = {0, Math.PI / 4, Math.PI / 2, 3 * Math.PI / 4,
                    Math.PI, 5 * Math.PI / 4, 3 * Math.PI / 2, 7 * Math.PI / 4};
            for (int col = 0; col < verticalColumns.size(); col++) {
                double rotAngle = colAngles[col] + mainRot;
                Location cLoc = c.clone().add(
                        Math.cos(rotAngle) * 2.3,
                        latticeY,
                        Math.sin(rotAngle) * 2.3
                );
                verticalColumns.get(col).entity().teleport(cLoc);
            }

            // Orrery drops: orbit amethyst center at different speeds
            float[] dropSpeeds = {0.00175f, 0.00262f, 0.00349f, 0.00436f, 0.00524f, 0.00175f};
            float[] dropRadii = {0.8f, 1.0f, 1.2f, 0.9f, 1.1f, 0.7f};
            for (int i = 0; i < orbitingDrops.size(); i++) {
                double orbAngle = ticksAlive * dropSpeeds[i] + (Math.PI * 2 * i) / 6;
                Location dLoc = c.clone().add(
                        Math.cos(orbAngle) * dropRadii[i],
                        latticeY + 3 + (i % 3) + Math.sin(ticksAlive * 0.05 + i) * 0.3,
                        Math.sin(orbAngle) * dropRadii[i]
                );
                orbitingDrops.get(i).entity().teleport(dLoc);
            }

            // Enchant particles streaming from amethyst center outward
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT,
                        c.clone().add(0, latticeY + 4, 0), 10, 2, 3, 2, 0.5);
            }

            // Tears from crying obsidian drops
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle drop : orbitingDrops) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            drop.entity().getLocation(), 2, 0.1, 0.3, 0.1, 0);
                }
            }

            // Dark blue-violet dust drifting through lattice gaps
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.darkPurpleDust(c.clone().add(0, latticeY + 4, 0), 5, 2.5);
            }

            // Chain sound every 5 seconds
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, latticeY + 4, 0), Sound.BLOCK_CHAIN_FALL, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HangingVoidLattice(plugin); }
    }

    // ================================================================
    // 20. ORBITAL SIEGE RING — Massive torus orbiting island center
    // ================================================================
    public static class OrbitalSiegeRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> glowNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> innerWindows = new ArrayList<>();
        private BlockDisplayHandle polishedStripe;
        private float orbitalAngle = 0;
        private float selfRotAngle = 0;
        private float orbitalRadius = 15.0f;
        private boolean contracting = false;
        private int contractionTimer = 0;

        public OrbitalSiegeRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_siege_ring", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(6000); // Persistent for entire fight phase
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Build torus from segments: outer ring of obsidian + netherite
            // 16 segments around the ring
            for (int seg = 0; seg < 16; seg++) {
                double segAngle = (Math.PI * 2 * seg) / 16;
                double segX = Math.cos(segAngle) * 4.0; // 8-block outer diameter
                double segZ = Math.sin(segAngle) * 4.0;

                Material mat = (seg % 3 == 0) ? Material.NETHERITE_BLOCK : Material.OBSIDIAN;
                Location sLoc = center.clone().add(orbitalRadius + segX, 12, segZ);
                BlockDisplayHandle segment = displayBuilder.spawnBlock(sLoc, mat);
                segment.scale(1.2f, 2.0f, 1.2f).glow(mat == Material.NETHERITE_BLOCK ? 20 : 60, 0,
                        mat == Material.NETHERITE_BLOCK ? 30 : 120).interpolation(3, 0);
                ringSegments.add(segment);
                spawnedEntities.add(segment.entity());
            }

            // Inner face: 12 crying obsidian weeping inward
            for (int i = 0; i < 12; i++) {
                double iAngle = (Math.PI * 2 * i) / 12;
                double iX = Math.cos(iAngle) * 2.0; // inner diameter
                double iZ = Math.sin(iAngle) * 2.0;
                Location iLoc = center.clone().add(orbitalRadius + iX, 12, iZ);
                BlockDisplayHandle inner = displayBuilder.spawnBlock(iLoc, Material.CRYING_OBSIDIAN);
                inner.scale(0.6f, 1.5f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                ringSegments.add(inner);
                spawnedEntities.add(inner.entity());
            }

            // Polished blackstone racing stripes on top face
            polishedStripe = displayBuilder.spawnBlock(center.clone().add(orbitalRadius, 13.5, 0),
                    Material.POLISHED_BLACKSTONE);
            polishedStripe.scale(8.0f, 0.3f, 8.0f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(polishedStripe.entity());

            // 6 amethyst glow nodes at equidistant positions on outer ring
            for (int i = 0; i < 6; i++) {
                double nAngle = (Math.PI * 2 * i) / 6;
                Location nLoc = center.clone().add(
                        orbitalRadius + Math.cos(nAngle) * 4.2, 12, Math.sin(nAngle) * 4.2);
                BlockDisplayHandle node = displayBuilder.spawnBlock(nLoc, Material.AMETHYST_BLOCK);
                node.scale(0.5f, 0.5f, 0.5f).glow(220, 120, 255).interpolation(3, 0);
                glowNodes.add(node);
                spawnedEntities.add(node.entity());
            }

            // 4 magenta stained glass windows on inner face
            for (int i = 0; i < 4; i++) {
                double wAngle = (Math.PI / 2) * i;
                Location wLoc = center.clone().add(
                        orbitalRadius + Math.cos(wAngle) * 2.0, 12, Math.sin(wAngle) * 2.0);
                BlockDisplayHandle window = displayBuilder.spawnBlock(wLoc, Material.MAGENTA_STAINED_GLASS);
                window.scale(0.4f, 0.8f, 0.4f).glow(255, 0, 255).interpolation(3, 0);
                innerWindows.add(window);
                spawnedEntities.add(window.entity());
            }

            DisplayBuilder.playSound(center.clone().add(orbitalRadius, 12, 0),
                    Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Orbital movement: 4 deg/sec around island center
            orbitalAngle += 0.00349f; // 4 deg/sec at 20 tps

            // Self-rotation: ring spins on its own axis at 8 deg/sec
            selfRotAngle += 0.007f;

            // Contraction mechanic: every 30 seconds (600 ticks), contract from 15 to 6 blocks
            contractionTimer++;
            if (contractionTimer >= 600) {
                contracting = true;
            }

            if (contracting) {
                int contractionAge = contractionTimer - 600;
                if (contractionAge < 40) {
                    // Contract: 15 -> 6 over 40 ticks
                    orbitalRadius = 15.0f - (contractionAge / 40.0f) * 9.0f;
                } else if (contractionAge < 80) {
                    // Expand back: 6 -> 15 over 40 ticks
                    orbitalRadius = 6.0f + ((contractionAge - 40) / 40.0f) * 9.0f;
                } else {
                    orbitalRadius = 15.0f;
                    contracting = false;
                    contractionTimer = 0;
                }
            }

            // Calculate ring center position on orbital path
            double orbCenterX = c.getX() + Math.cos(orbitalAngle) * orbitalRadius;
            double orbCenterZ = c.getZ() + Math.sin(orbitalAngle) * orbitalRadius;
            Location ringCenter = new Location(c.getWorld(), orbCenterX, c.getY() + 12, orbCenterZ);

            // Update ring segments positions
            for (int seg = 0; seg < 16; seg++) {
                double segAngle = (Math.PI * 2 * seg) / 16 + selfRotAngle;
                double segX = Math.cos(segAngle) * 4.0;
                double segY = Math.sin(segAngle) * 1.0; // Rolling wheel effect
                double segZ = Math.sin(segAngle) * 4.0;
                Location sLoc = ringCenter.clone().add(segX, segY, segZ);
                ringSegments.get(seg).entity().teleport(sLoc);
            }

            // Inner crying obsidian
            for (int i = 0; i < 12; i++) {
                double iAngle = (Math.PI * 2 * i) / 12 + selfRotAngle;
                double iX = Math.cos(iAngle) * 2.0;
                double iY = Math.sin(iAngle) * 0.5;
                double iZ = Math.sin(iAngle) * 2.0;
                int idx = 16 + i;
                if (idx < ringSegments.size()) {
                    ringSegments.get(idx).entity().teleport(ringCenter.clone().add(iX, iY, iZ));
                }
            }

            // Polished stripe follows
            polishedStripe.entity().teleport(ringCenter.clone().add(0, 1.5, 0));
            polishedStripe.rotate(selfRotAngle, 0, 1, 0);
            polishedStripe.interpolation(3, 0);

            // Glow nodes on outer equator
            for (int i = 0; i < glowNodes.size(); i++) {
                double nAngle = (Math.PI * 2 * i) / 6 + selfRotAngle;
                Location nLoc = ringCenter.clone().add(Math.cos(nAngle) * 4.2, 0, Math.sin(nAngle) * 4.2);
                glowNodes.get(i).entity().teleport(nLoc);
            }

            // Inner windows
            for (int i = 0; i < innerWindows.size(); i++) {
                double wAngle = (Math.PI / 2) * i + selfRotAngle;
                Location wLoc = ringCenter.clone().add(Math.cos(wAngle) * 2.0, 0, Math.sin(wAngle) * 2.0);
                innerWindows.get(i).entity().teleport(wLoc);
            }

            // Update attack center for damage calculations
            setCenter(ringCenter);

            // Reverse portal streaming from inner torus face
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        ringCenter, 10, 2.0, 0.5, 2.0, 0.02);
            }

            // Amethyst purple dust orbiting outer equator
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.purpleDust(ringCenter, 8, 4.5);
            }

            // Large smoke trail behind orbital path
            if (ticksAlive % 4 == 0) {
                double trailAngle = orbitalAngle - 0.1;
                Location trailLoc = new Location(c.getWorld(),
                        c.getX() + Math.cos(trailAngle) * orbitalRadius,
                        c.getY() + 12,
                        c.getZ() + Math.sin(trailAngle) * orbitalRadius);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, trailLoc, 4, 0.5, 0.3, 0.5, 0.01);
            }

            // Cyan dust burst during contraction phase
            if (contracting && ticksAlive % 3 == 0) {
                DisplayBuilder.cyanDust(ringCenter, 10, 3.0);
            }

            // Elder guardian ambient every 8 seconds
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(ringCenter, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.5f, 0.5f);
            }

            // Heartbeat synchronized with contraction
            if (contracting && ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(ringCenter, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalSiegeRing(plugin); }
    }
}
