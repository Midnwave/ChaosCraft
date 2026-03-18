package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

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
 * Phase 5E Block Display -- GROUP 1: THE CALAMITAS ALTAR (#1-8)
 * 8 structures forming the central ritual altar for Supreme Calamitas.
 * The altar is placed during her descent arrival sequence and persists
 * throughout the fight, evolving with phase transitions.
 *
 * Structures:
 *  #1  CentralSigil               -- 8-arm crying obsidian sigil with soul soil fill
 *  #2  SoulFirePillarColumns       -- 8 short blackstone/soul soil marker columns at radius 21
 *  #3  NorthObsidianMonolith       -- Primary 16-block monolith, north, dark prismarine cap
 *  #4  SouthObsidianMonolith       -- Secondary 14-block monolith, south, netherrack cap
 *  #5  EastWestMonolithPair        -- Two shorter 10-block monoliths on the movement axis
 *  #6  DiagonalAltarPylons         -- 4 blackstone/crying obsidian pylons at diagonals
 *  #7  DeathResonancePillarEnhance -- Crimson wraps and cap on Boss 4's pillar
 *  #8  AltarGroundCandles          -- 24 micro-scale ritual candles scattered in middle ring
 *
 * Rules applied:
 * - NO status effects
 * - Calamitas palette: crimson(200,0,50), brimstone(255,100,0), soul blue(0,150,255),
 *   purple(128,0,255), white(240,240,255)
 * - Materials: NETHERRACK, MAGMA_BLOCK, BLACKSTONE, POLISHED_BLACKSTONE, OBSIDIAN,
 *   CRYING_OBSIDIAN, NETHER_WART_BLOCK, CRIMSON_NYLIUM, SOUL_SOIL, RED_STAINED_GLASS,
 *   ORANGE_STAINED_GLASS, AMETHYST_BLOCK, AMETHYST_CLUSTER, END_ROD, SEA_LANTERN
 * - Damage HP 4.0-14.0
 * - AxisAngle4f ONLY (never Quaternionf)
 * - Static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Location center = getCenter(); if (center == null) return; EVERY onTick
 */
public final class CalamitasAltar {

    private CalamitasAltar() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CentralSigil(plugin));
        registry.register(new SoulFirePillarColumns(plugin));
        registry.register(new NorthObsidianMonolith(plugin));
        registry.register(new SouthObsidianMonolith(plugin));
        registry.register(new EastWestMonolithPair(plugin));
        registry.register(new DiagonalAltarPylons(plugin));
        registry.register(new DeathResonancePillarEnhance(plugin));
        registry.register(new AltarGroundCandles(plugin));
    }

    // ================================================================
    // #1 -- CENTRAL SIGIL
    // 8 radiating arms of crying obsidian (flat inlay), soul soil fill,
    // octagonal border, upright center node. Arms grow outward over 15s.
    // ================================================================
    public static class CentralSigil extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> borderSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> soulFill = new ArrayList<>();
        private BlockDisplayHandle centerNode;

        public CentralSigil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_central_sigil", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center node: upright crying obsidian, appears first
            Location nodeLoc = center.clone().add(0, 0.05, 0);
            centerNode = displayBuilder.spawnBlock(nodeLoc, Material.CRYING_OBSIDIAN);
            centerNode.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 50).interpolation(10, 0);
            spawnedEntities.add(centerNode.entity());

            // 8 radiating arms at 45-degree intervals, flat inlay
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(45.0 * i);
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                // Each arm: 8 segments of crying obsidian at 1-block intervals
                for (int seg = 1; seg <= 8; seg++) {
                    Location armLoc = center.clone().add(dx * seg, 0.05, dz * seg);
                    BlockDisplayHandle arm = displayBuilder.spawnBlock(armLoc, Material.CRYING_OBSIDIAN);
                    arm.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50);
                    // Start with zero Y scale for growth animation
                    arm.interpolation(40, seg * 40);
                    arms.add(arm);
                    spawnedEntities.add(arm.entity());
                }
                // Teardrop tip expansion at arm end
                Location tipLoc = center.clone().add(dx * 8.5, 0.05, dz * 8.5);
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.CRYING_OBSIDIAN);
                tip.scale(2.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(20, 240);
                arms.add(tip);
                spawnedEntities.add(tip.entity());
            }

            // Octagonal border: 8 curved segments at radius 9 connecting arm tips
            for (int i = 0; i < 8; i++) {
                double a1 = Math.toRadians(45.0 * i);
                double a2 = Math.toRadians(45.0 * (i + 1));
                double midAngle = (a1 + a2) / 2.0;
                double bx = Math.cos(midAngle) * 9.0;
                double bz = Math.sin(midAngle) * 9.0;
                Location borderLoc = center.clone().add(bx, 0.05, bz);
                BlockDisplayHandle border = displayBuilder.spawnBlock(borderLoc, Material.CRYING_OBSIDIAN);
                float rotY = (float) midAngle;
                border.scale(3.0f, 0.1f, 0.5f).rotate(rotY, 0, 1, 0).glow(200, 0, 50);
                border.interpolation(20, 240);
                borderSegments.add(border);
                spawnedEntities.add(border.entity());
            }

            // Soul soil fill inside octagonal border
            for (int x = -8; x <= 8; x += 2) {
                for (int z = -8; z <= 8; z += 2) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist > 1.5 && dist < 8.5) {
                        Location fillLoc = center.clone().add(x, 0.04, z);
                        BlockDisplayHandle fill = displayBuilder.spawnBlock(fillLoc, Material.SOUL_SOIL);
                        fill.scale(1.0f, 0.1f, 1.0f).glow(0, 150, 255);
                        fill.brightness(0, 0).interpolation(60, 240);
                        soulFill.add(fill);
                        spawnedEntities.add(fill.entity());
                    }
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.15f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Soul soil fill brightness fade-in (ticks 240-300)
            if (ticksAlive >= 240 && ticksAlive <= 300) {
                int brightness = Math.min(15, (ticksAlive - 240));
                for (BlockDisplayHandle fill : soulFill) {
                    fill.brightness(brightness, brightness);
                }
            }

            // SOUL_FIRE_FLAME from center node
            if (ticksAlive % 10 == 0) {
                Location nodeLoc = center.clone().add(0, 0.3, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, nodeLoc, 1, 0.05, 0.1, 0.05, 0.01);
            }

            // Crimson dust drifting outward along arm axes
            if (ticksAlive % 40 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(45.0 * i);
                    double dist = 2.0 + (ticksAlive * 0.01 % 6.0);
                    Location dustLoc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);
                    DisplayBuilder.crimsonDust(dustLoc, 2, 0.2);
                }
            }

            // Sculk clicking per arm completion
            if (ticksAlive > 0 && ticksAlive <= 320 && ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.15f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CentralSigil(plugin); }
    }

    // ================================================================
    // #2 -- SOUL FIRE PILLAR COLUMNS (x8)
    // Short blackstone base + soul soil block columns at radius 21.
    // SOUL_FIRE_FLAME particle column to Y+4, sinusoidal breathing.
    // ================================================================
    public static class SoulFirePillarColumns extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> soulBlocks = new ArrayList<>();
        private final double[] columnAngles = new double[8];

        public SoulFirePillarColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_soul_fire_columns", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(45.0 * i);
                columnAngles[i] = angle;
                double px = Math.cos(angle) * 21.0;
                double pz = Math.sin(angle) * 21.0;

                // Blackstone base at Y+0
                Location baseLoc = center.clone().add(px, 0, pz);
                BlockDisplayHandle base = displayBuilder.spawnBlock(baseLoc, Material.BLACKSTONE);
                base.scale(1.0f, 0.5f, 1.0f).glow(200, 0, 50);
                baseBlocks.add(base);
                spawnedEntities.add(base.entity());

                // Soul soil on top at Y+0.5
                Location soulLoc = center.clone().add(px, 0.5, pz);
                BlockDisplayHandle soul = displayBuilder.spawnBlock(soulLoc, Material.SOUL_SOIL);
                soul.scale(1.0f, 0.5f, 1.0f).glow(0, 150, 255);
                soulBlocks.add(soul);
                spawnedEntities.add(soul.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            for (int i = 0; i < 8; i++) {
                double angle = columnAngles[i];
                double px = Math.cos(angle) * 21.0;
                double pz = Math.sin(angle) * 21.0;
                Location colLoc = center.clone().add(px, 1.0, pz);

                // Phase-offset breathing: each column breathes in sequence
                double phaseOffset = (2.0 * Math.PI * i) / 8.0;
                float yOscillation = (float) (0.2 * Math.sin(ticksAlive * Math.PI / 20.0 + phaseOffset));
                Location flameLoc = colLoc.clone().add(0, yOscillation, 0);

                // SOUL_FIRE_FLAME column from Y+1 to Y+4
                if (ticksAlive % 2 == 0) {
                    for (double y = 0; y < 3.0; y += 0.4) {
                        Location particleLoc = flameLoc.clone().add(0, y, 0);
                        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.15, 0.1, 0.15, 0.005);
                    }
                }

                // SMOKE accent
                if (ticksAlive % 4 == 0) {
                    center.getWorld().spawnParticle(Particle.SMOKE, flameLoc.clone().add(0, 1.5, 0), 1, 0.1, 0.2, 0.1, 0.005);
                }
            }

            // Sound: soul sand step every 60 ticks per column (stagger by 8 ticks)
            int soundColumn = (ticksAlive / 60) % 8;
            if (ticksAlive % 60 == soundColumn * 8 % 60) {
                double angle = columnAngles[soundColumn];
                Location soundLoc = center.clone().add(Math.cos(angle) * 21.0, 1.0, Math.sin(angle) * 21.0);
                DisplayBuilder.playSound(soundLoc, Sound.BLOCK_STONE_PLACE, 0.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFirePillarColumns(plugin); }
    }

    // ================================================================
    // #3 -- NORTH OBSIDIAN MONOLITH
    // 16-block tall composite: blackstone base, obsidian shaft, crying
    // obsidian mid-band, tapered upper shaft, dark prismarine cap.
    // 34 blocks north. Phase 1->2 cracks via red stained glass.
    // ================================================================
    public static class NorthObsidianMonolith extends BlockDisplayAttack {

        private BlockDisplayHandle basePlinth;
        private BlockDisplayHandle lowerShaft;
        private BlockDisplayHandle midBand;
        private BlockDisplayHandle upperShaft;
        private BlockDisplayHandle cap;
        private final List<BlockDisplayHandle> crackPanels = new ArrayList<>();

        public NorthObsidianMonolith(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_north_monolith", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, -34);

            // Base plinth: Y+0 to Y+2, wider blackstone
            basePlinth = displayBuilder.spawnBlock(base, Material.BLACKSTONE);
            basePlinth.scale(3.5f, 2.0f, 2.5f).glow(200, 0, 50);
            spawnedEntities.add(basePlinth.entity());

            // Lower shaft: Y+2 to Y+10, obsidian
            Location lowerLoc = base.clone().add(0, 2, 0);
            lowerShaft = displayBuilder.spawnBlock(lowerLoc, Material.OBSIDIAN);
            lowerShaft.scale(3.0f, 8.0f, 2.0f).glow(128, 0, 255);
            spawnedEntities.add(lowerShaft.entity());

            // Mid band: Y+9.5 to Y+10.5, crying obsidian (wider)
            Location midLoc = base.clone().add(0, 9.5, 0);
            midBand = displayBuilder.spawnBlock(midLoc, Material.CRYING_OBSIDIAN);
            midBand.scale(3.2f, 1.0f, 2.2f).glow(200, 0, 50);
            spawnedEntities.add(midBand.entity());

            // Upper shaft: Y+10 to Y+14, tapered obsidian
            Location upperLoc = base.clone().add(0, 10, 0);
            upperShaft = displayBuilder.spawnBlock(upperLoc, Material.OBSIDIAN);
            upperShaft.scale(2.6f, 4.0f, 1.8f).glow(128, 0, 255);
            spawnedEntities.add(upperShaft.entity());

            // Cap: Y+14 to Y+16, polished blackstone (replacing dark prismarine per material rules)
            Location capLoc = base.clone().add(0, 14, 0);
            cap = displayBuilder.spawnBlock(capLoc, Material.POLISHED_BLACKSTONE);
            cap.scale(3.0f, 2.0f, 2.2f).glow(0, 150, 255);
            spawnedEntities.add(cap.entity());

            DisplayBuilder.playSound(base, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.06f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location base = center.clone().add(0, 0, -34);

            // Crying obsidian drip particles from mid-band
            if (ticksAlive % 5 == 0) {
                Location dripLoc = base.clone().add(0, 10, 0);
                center.getWorld().spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, dripLoc, 1, 0.5, 0.1, 0.3, 0);
            }

            // Cap soul particles
            if (ticksAlive % 10 == 0) {
                Location capTop = base.clone().add(0, 16, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, capTop, 1, 0.3, 0.1, 0.3, 0.01);
            }

            // Phase transition: crack panels appear at tick 600 (simulating phase transition)
            if (ticksAlive == 600 && crackPanels.isEmpty()) {
                for (int i = 0; i < 3; i++) {
                    float yOffset = 3.0f + i * 2.5f;
                    float rotAngle = (float) Math.toRadians(15 + i * 10);
                    Location crackLoc = base.clone().add(0.1, yOffset, 0);
                    BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.RED_STAINED_GLASS);
                    crack.scale(0.05f, 3.0f + i * 0.5f, 0.05f).rotate(rotAngle, 0, 0, 1);
                    crack.glow(200, 0, 50).interpolation(12, 0);
                    crackPanels.add(crack);
                    spawnedEntities.add(crack.entity());
                }
                DisplayBuilder.playSound(base, Sound.BLOCK_GLASS_BREAK, 0.25f, 0.7f);
            }

            // Crimson dust from cracks after they appear
            if (ticksAlive > 600 && !crackPanels.isEmpty() && ticksAlive % 10 == 0) {
                for (int i = 0; i < crackPanels.size(); i++) {
                    Location crackDust = base.clone().add(0.1, 3.0 + i * 2.5, 0);
                    DisplayBuilder.crimsonDust(crackDust, 2, 0.2);
                }
            }

            // Ambient hum
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(base, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.06f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NorthObsidianMonolith(plugin); }
    }

    // ================================================================
    // #4 -- SOUTH OBSIDIAN MONOLITH
    // 14-block variant, mirrored on Z. Netherrack cap with FLAME.
    // Crimson crystal growth on phase 2->3 transition.
    // ================================================================
    public static class SouthObsidianMonolith extends BlockDisplayAttack {

        private BlockDisplayHandle basePlinth;
        private BlockDisplayHandle lowerShaft;
        private BlockDisplayHandle midBand;
        private BlockDisplayHandle upperShaft;
        private BlockDisplayHandle cap;
        private BlockDisplayHandle crystalGrowth;

        public SouthObsidianMonolith(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_south_monolith", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, 34);

            // Base plinth
            basePlinth = displayBuilder.spawnBlock(base, Material.BLACKSTONE);
            basePlinth.scale(3.5f, 2.0f, 2.5f).glow(200, 0, 50);
            spawnedEntities.add(basePlinth.entity());

            // Lower shaft: Y+2 to Y+8
            Location lowerLoc = base.clone().add(0, 2, 0);
            lowerShaft = displayBuilder.spawnBlock(lowerLoc, Material.OBSIDIAN);
            lowerShaft.scale(3.0f, 6.0f, 2.0f).glow(128, 0, 255);
            spawnedEntities.add(lowerShaft.entity());

            // Mid band: Y+7.5
            Location midLoc = base.clone().add(0, 7.5, 0);
            midBand = displayBuilder.spawnBlock(midLoc, Material.CRYING_OBSIDIAN);
            midBand.scale(3.2f, 1.0f, 2.2f).glow(200, 0, 50);
            spawnedEntities.add(midBand.entity());

            // Upper shaft: Y+8 to Y+12
            Location upperLoc = base.clone().add(0, 8, 0);
            upperShaft = displayBuilder.spawnBlock(upperLoc, Material.OBSIDIAN);
            upperShaft.scale(2.6f, 4.0f, 1.8f).glow(128, 0, 255);
            spawnedEntities.add(upperShaft.entity());

            // Netherrack cap: Y+12 to Y+14
            Location capLoc = base.clone().add(0, 12, 0);
            cap = displayBuilder.spawnBlock(capLoc, Material.NETHERRACK);
            cap.scale(3.0f, 2.0f, 2.2f).glow(255, 100, 0);
            spawnedEntities.add(cap.entity());

            DisplayBuilder.playSound(base, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.06f, 0.25f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location base = center.clone().add(0, 0, 34);

            // FLAME from netherrack cap top
            if (ticksAlive % 10 == 0) {
                Location capTop = base.clone().add(0, 14, 0);
                center.getWorld().spawnParticle(Particle.FLAME, capTop, 1, 0.3, 0.05, 0.3, 0.01);
            }

            // Crying obsidian drip from mid-band
            if (ticksAlive % 5 == 0) {
                Location dripLoc = base.clone().add(0, 8, 0);
                center.getWorld().spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, dripLoc, 1, 0.5, 0.1, 0.3, 0);
            }

            // Phase 2->3 crystal growth at tick 1200
            if (ticksAlive == 1200 && crystalGrowth == null) {
                Location crystalLoc = base.clone().add(1.5, 0, 0);
                crystalGrowth = displayBuilder.spawnBlock(crystalLoc, Material.RED_STAINED_GLASS);
                float leanAngle = (float) Math.toRadians(20);
                crystalGrowth.scale(0.6f, 3.0f, 0.6f).rotate(leanAngle, 0, 0, 1);
                crystalGrowth.glow(200, 0, 50).interpolation(80, 0);
                spawnedEntities.add(crystalGrowth.entity());
                DisplayBuilder.playSound(base, Sound.BLOCK_GLASS_BREAK, 0.3f, 0.6f);
            }

            // Ambient hum
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(base, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.06f, 0.25f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SouthObsidianMonolith(plugin); }
    }

    // ================================================================
    // #5 -- EAST AND WEST MONOLITH PAIR
    // Two shorter 10-block monoliths at X+34 and X-34.
    // Soul fire emitters at Y+8 facing inward, alternating brightness.
    // ================================================================
    public static class EastWestMonolithPair extends BlockDisplayAttack {

        private BlockDisplayHandle eastBase;
        private BlockDisplayHandle eastShaft;
        private BlockDisplayHandle eastCap;
        private BlockDisplayHandle eastFlameBlock;
        private BlockDisplayHandle westBase;
        private BlockDisplayHandle westShaft;
        private BlockDisplayHandle westCap;
        private BlockDisplayHandle westFlameBlock;

        public EastWestMonolithPair(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_east_west_monoliths", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // East monolith at X+34
            Location eLoc = center.clone().add(34, 0, 0);
            eastBase = displayBuilder.spawnBlock(eLoc, Material.BLACKSTONE);
            eastBase.scale(2.5f, 1.0f, 2.0f).glow(200, 0, 50);
            spawnedEntities.add(eastBase.entity());

            eastShaft = displayBuilder.spawnBlock(eLoc.clone().add(0, 1, 0), Material.OBSIDIAN);
            eastShaft.scale(2.0f, 7.0f, 1.5f).glow(128, 0, 255);
            spawnedEntities.add(eastShaft.entity());

            eastCap = displayBuilder.spawnBlock(eLoc.clone().add(0, 8, 0), Material.OBSIDIAN);
            eastCap.scale(2.2f, 2.0f, 1.7f).glow(128, 0, 255);
            spawnedEntities.add(eastCap.entity());

            // Flame emitter block facing west (inward)
            eastFlameBlock = displayBuilder.spawnBlock(eLoc.clone().add(-0.8, 8, 0), Material.SOUL_SOIL);
            eastFlameBlock.scale(0.3f, 0.3f, 0.3f).glow(0, 150, 255);
            spawnedEntities.add(eastFlameBlock.entity());

            // West monolith at X-34
            Location wLoc = center.clone().add(-34, 0, 0);
            westBase = displayBuilder.spawnBlock(wLoc, Material.BLACKSTONE);
            westBase.scale(2.5f, 1.0f, 2.0f).glow(200, 0, 50);
            spawnedEntities.add(westBase.entity());

            westShaft = displayBuilder.spawnBlock(wLoc.clone().add(0, 1, 0), Material.OBSIDIAN);
            westShaft.scale(2.0f, 7.0f, 1.5f).glow(128, 0, 255);
            spawnedEntities.add(westShaft.entity());

            westCap = displayBuilder.spawnBlock(wLoc.clone().add(0, 8, 0), Material.OBSIDIAN);
            westCap.scale(2.2f, 2.0f, 1.7f).glow(128, 0, 255);
            spawnedEntities.add(westCap.entity());

            // Flame emitter block facing east (inward)
            westFlameBlock = displayBuilder.spawnBlock(wLoc.clone().add(0.8, 8, 0), Material.SOUL_SOIL);
            westFlameBlock.scale(0.3f, 0.3f, 0.3f).glow(0, 150, 255);
            spawnedEntities.add(westFlameBlock.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // SOUL_FIRE_FLAME from east emitter
            if (ticksAlive % 3 == 0) {
                Location eastEmit = center.clone().add(33.2, 8.2, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, eastEmit, 1, 0.1, 0.1, 0.1, 0.01);
            }

            // SOUL_FIRE_FLAME from west emitter
            if (ticksAlive % 3 == 0) {
                Location westEmit = center.clone().add(-33.2, 8.2, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, westEmit, 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Alternating brightness oscillation: east and west 30-tick offset
            float eastBrightness = 12.0f + 3.0f * (float) Math.sin(ticksAlive * Math.PI / 30.0);
            float westBrightness = 12.0f + 3.0f * (float) Math.sin(ticksAlive * Math.PI / 30.0 + Math.PI);
            int eb = Math.min(15, Math.max(0, (int) eastBrightness));
            int wb = Math.min(15, Math.max(0, (int) westBrightness));
            if (eastFlameBlock != null) eastFlameBlock.brightness(eb, eb);
            if (westFlameBlock != null) westFlameBlock.brightness(wb, wb);

            // Phase 4: faster flickering (period shortens to 20 ticks)
            // This would be controlled by phase state in the actual game
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EastWestMonolithPair(plugin); }
    }

    // ================================================================
    // #6 -- DIAGONAL ALTAR PYLONS (x4)
    // 6-block tall, 1.5 wide pylons at NE/NW/SE/SW at radius 28.
    // Blackstone lower 4, crying obsidian upper 2. Netherrack fire base.
    // Very slow sway on Y axis (+/-0.5 deg, 120-tick period).
    // ================================================================
    public static class DiagonalAltarPylons extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pylonBodies = new ArrayList<>();
        private final List<BlockDisplayHandle> pylonTops = new ArrayList<>();
        private final List<BlockDisplayHandle> baseSlabs = new ArrayList<>();
        private final double[][] positions = {
            {28, 0, -28}, {-28, 0, -28}, {28, 0, 28}, {-28, 0, 28}
        };

        public DiagonalAltarPylons(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_diagonal_pylons", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (double[] pos : positions) {
                Location pylonBase = center.clone().add(pos[0], 0, pos[2]);

                // Lower body: blackstone, 4 blocks tall
                BlockDisplayHandle body = displayBuilder.spawnBlock(pylonBase, Material.BLACKSTONE);
                body.scale(1.5f, 4.0f, 1.0f).glow(200, 0, 50).interpolation(5, 0);
                pylonBodies.add(body);
                spawnedEntities.add(body.entity());

                // Upper section: crying obsidian, 2 blocks
                BlockDisplayHandle top = displayBuilder.spawnBlock(pylonBase.clone().add(0, 4, 0), Material.CRYING_OBSIDIAN);
                top.scale(1.5f, 2.0f, 1.0f).glow(200, 0, 50).interpolation(5, 0);
                pylonTops.add(top);
                spawnedEntities.add(top.entity());

                // Netherrack fire ring base (4 slabs around pylon)
                for (int d = 0; d < 4; d++) {
                    double slabAngle = Math.toRadians(90.0 * d + 45);
                    double sx = Math.cos(slabAngle) * 1.5;
                    double sz = Math.sin(slabAngle) * 1.5;
                    Location slabLoc = pylonBase.clone().add(sx, 0, sz);
                    BlockDisplayHandle slab = displayBuilder.spawnBlock(slabLoc, Material.NETHERRACK);
                    slab.scale(1.0f, 0.3f, 1.0f).glow(255, 100, 0);
                    baseSlabs.add(slab);
                    spawnedEntities.add(slab.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sway animation: NE/SW in phase, NW/SE opposing
            float swayNESW = (float) Math.toRadians(0.5 * Math.sin(ticksAlive * Math.PI / 60.0));
            float swayNWSE = (float) Math.toRadians(0.5 * Math.sin(ticksAlive * Math.PI / 60.0 + Math.PI));

            for (int i = 0; i < pylonBodies.size(); i++) {
                float sway = (i == 0 || i == 3) ? swayNESW : swayNWSE;
                pylonBodies.get(i).rotate(sway, 0, 1, 0);
                pylonBodies.get(i).interpolation(5, 0);
                pylonTops.get(i).rotate(sway, 0, 1, 0);
                pylonTops.get(i).interpolation(5, 0);
            }

            // Crying obsidian drip from upper sections
            if (ticksAlive % 10 == 0) {
                for (double[] pos : positions) {
                    Location dripLoc = center.clone().add(pos[0], 5, pos[2]);
                    center.getWorld().spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, dripLoc, 1, 0.2, 0.1, 0.2, 0);
                }
            }

            // FLAME from netherrack base slabs
            if (ticksAlive % 40 == 0) {
                for (int i = 0; i < baseSlabs.size(); i++) {
                    int pylonIdx = i / 4;
                    double[] pos = positions[pylonIdx];
                    int slabIdx = i % 4;
                    double slabAngle = Math.toRadians(90.0 * slabIdx + 45);
                    Location flameLoc = center.clone().add(
                        pos[0] + Math.cos(slabAngle) * 1.5,
                        0.3,
                        pos[2] + Math.sin(slabAngle) * 1.5
                    );
                    center.getWorld().spawnParticle(Particle.FLAME, flameLoc, 1, 0.05, 0.05, 0.05, 0.005);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DiagonalAltarPylons(plugin); }
    }

    // ================================================================
    // #7 -- DEATH RESONANCE PILLAR ENHANCEMENT
    // Adds crimson wraps, new blackstone cap, and netherrack base ring
    // to the existing Boss 4 pillar. 30 blocks NE of center.
    // Glass wraps pulse in brightness on 80-tick cycle.
    // ================================================================
    public static class DeathResonancePillarEnhance extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crimsonWraps = new ArrayList<>();
        private BlockDisplayHandle newCap;
        private final List<BlockDisplayHandle> baseRing = new ArrayList<>();

        public DeathResonancePillarEnhance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_death_resonance_enhance", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pillar position: ~30 blocks NE
            Location pillar = center.clone().add(21, 0, -21);

            // Crimson wraps: 4 diagonal red stained glass panels on each face
            float[] wrapYPositions = {2.0f, 5.0f, 8.0f, 11.0f};
            float[][] faceOffsets = {{0.6f, 0, 0}, {-0.6f, 0, 0}, {0, 0, 0.6f}, {0, 0, -0.6f}};

            for (int face = 0; face < 4; face++) {
                Location wrapLoc = pillar.clone().add(faceOffsets[face][0], wrapYPositions[face], faceOffsets[face][2]);
                BlockDisplayHandle wrap = displayBuilder.spawnBlock(wrapLoc, Material.RED_STAINED_GLASS);
                float rotAngle = (float) Math.toRadians(15 + face * 5);
                wrap.scale(0.1f, 4.0f, 0.1f).rotate(rotAngle, 0, 0, 1);
                wrap.glow(200, 0, 50).brightness(12, 12);
                crimsonWraps.add(wrap);
                spawnedEntities.add(wrap.entity());
            }

            // New top cap: blackstone slab at slight tilt
            Location capLoc = pillar.clone().add(0, 14, 0);
            newCap = displayBuilder.spawnBlock(capLoc, Material.BLACKSTONE);
            float tiltAngle = (float) Math.toRadians(5);
            newCap.scale(1.5f, 0.5f, 1.5f).rotate(tiltAngle, 1, 0, 0).glow(200, 0, 50);
            spawnedEntities.add(newCap.entity());

            // Base ring: 8 netherrack blocks at radius 1.5
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(45.0 * i);
                Location ringLoc = pillar.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.NETHERRACK);
                ring.scale(0.5f, 0.3f, 0.5f).glow(255, 100, 0);
                baseRing.add(ring);
                spawnedEntities.add(ring.entity());
            }

            // Soul fire at cap center
            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, capLoc.clone().add(0, 0.5, 0), 5, 0.2, 0.1, 0.2, 0.02);

            DisplayBuilder.playSound(pillar, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.2f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location pillar = center.clone().add(21, 0, -21);

            // Crimson wrap brightness pulse: 80-tick cycle
            float brightnessVal = 8.0f + 7.0f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 80.0);
            int bv = Math.min(15, Math.max(0, (int) brightnessVal));
            for (BlockDisplayHandle wrap : crimsonWraps) {
                wrap.brightness(bv, bv);
            }

            // SOUL_FIRE_FLAME from cap center
            if (ticksAlive % 3 == 0) {
                Location capFlame = pillar.clone().add(0, 14.5, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, capFlame, 1, 0.1, 0.1, 0.1, 0.02);
            }

            // FLAME from base ring
            if (ticksAlive % 20 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(45.0 * i);
                    Location ringFlame = pillar.clone().add(Math.cos(angle) * 1.5, 0.3, Math.sin(angle) * 1.5);
                    center.getWorld().spawnParticle(Particle.FLAME, ringFlame, 1, 0.05, 0.05, 0.05, 0.005);
                }
            }

            // Sound at pulse peak (every 80 ticks)
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(pillar, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathResonancePillarEnhance(plugin); }
    }

    // ================================================================
    // #8 -- ALTAR GROUND CANDLES (x24)
    // Micro-scale netherrack candles in the 10-21 block radius zone.
    // FLAME or SOUL_FIRE_FLAME at tips, 2:1 orange to blue ratio.
    // Individual flicker at randomized 18-30 tick periods.
    // ================================================================
    public static class AltarGroundCandles extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> candles = new ArrayList<>();
        private final List<Location> candleLocations = new ArrayList<>();
        private final List<Boolean> useSoulFire = new ArrayList<>();
        private final List<Integer> flickerPeriods = new ArrayList<>();

        public AltarGroundCandles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_altar_candles", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Place 24 candles in the 10-21 block radius zone with irregular spacing
            java.util.Random rng = new java.util.Random(center.hashCode());
            int placed = 0;
            int attempts = 0;
            while (placed < 24 && attempts < 200) {
                attempts++;
                double angle = rng.nextDouble() * 2.0 * Math.PI;
                double radius = 10.0 + rng.nextDouble() * 11.0;
                double cx = Math.cos(angle) * radius;
                double cz = Math.sin(angle) * radius;
                Location candleLoc = center.clone().add(cx, 0, cz);

                // Check minimum spacing from existing candles (>2 blocks)
                boolean tooClose = false;
                for (Location existing : candleLocations) {
                    if (existing.distanceSquared(candleLoc) < 3.0) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                BlockDisplayHandle candle = displayBuilder.spawnBlock(candleLoc, Material.NETHERRACK);
                candle.scale(0.15f, 0.3f, 0.15f).glow(255, 100, 0);
                candles.add(candle);
                spawnedEntities.add(candle.entity());
                candleLocations.add(candleLoc);

                // 2:1 ratio orange to blue
                boolean isSoul = placed % 3 == 2;
                useSoulFire.add(isSoul);

                // Randomized flicker period 18-30 ticks
                flickerPeriods.add(18 + rng.nextInt(13));

                placed++;
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            for (int i = 0; i < candleLocations.size(); i++) {
                Location loc = candleLocations.get(i);
                int period = flickerPeriods.get(i);
                boolean soul = useSoulFire.get(i);

                // Flickering emission rate: oscillate between 2-6 per second
                float phase = (float) Math.sin(ticksAlive * 2.0 * Math.PI / period);
                int rate = (int) (4.0 + 2.0 * phase); // 2 to 6

                if (ticksAlive % Math.max(1, 20 / rate) == i % Math.max(1, 20 / rate)) {
                    Location flameLoc = loc.clone().add(0, 0.35, 0);
                    if (soul) {
                        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, flameLoc, 1, 0.05, 0.05, 0.05, 0.002);
                    } else {
                        center.getWorld().spawnParticle(Particle.FLAME, flameLoc, 1, 0.05, 0.05, 0.05, 0.002);
                    }
                }
            }

            // Aggregate candle ambience — very quiet crackle
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.05f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AltarGroundCandles(plugin); }
    }
}
